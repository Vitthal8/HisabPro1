package com.hisabpro.app.data.sync

import android.content.Context
import androidx.room.withTransaction
import com.hisabpro.app.data.local.AppDatabase
import com.hisabpro.app.data.local.entity.AccountEntity
import com.hisabpro.app.data.local.entity.BusinessEntity
import com.hisabpro.app.data.local.entity.ExpenseEntity
import com.hisabpro.app.data.local.entity.InvoiceEntity
import com.hisabpro.app.data.local.entity.InvoiceItemEntity
import com.hisabpro.app.data.local.entity.ItemEntity
import com.hisabpro.app.data.local.entity.JournalEntryEntity
import com.hisabpro.app.data.local.entity.JournalEntryLineEntity
import com.hisabpro.app.data.local.entity.KhataEntryEntity
import com.hisabpro.app.data.local.entity.PartyEntity
import com.hisabpro.app.data.local.entity.PaymentEntity
import com.hisabpro.app.data.local.entity.SyncMetadataEntity
import com.hisabpro.app.data.local.entity.SyncQueueEntity
import com.hisabpro.app.data.model.BankDetails
import com.hisabpro.app.data.model.BusinessProfile
import com.hisabpro.app.data.repository.BusinessManager
import com.hisabpro.app.data.repository.InvoiceRepository
import com.hisabpro.app.data.repository.ItemRepository
import com.hisabpro.app.data.repository.PartyRepository
import com.hisabpro.app.data.repository.SettingsRepository
import com.hisabpro.app.data.repository.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class CloudSyncUiState(
    val isSyncing: Boolean = false,
    val lastSyncTimeMillis: Long = 0L,
    val pendingQueueCount: Int = 0,
    val lastError: String? = null,
    val statusMessage: String = "Ready",
    val isAuthenticated: Boolean = false,
    val userEmail: String? = null,
    val userPhone: String? = null,
    val totalSyncedRecords: Int = 0
)

data class PullResultSummary(
    val totalPulled: Int,
    val successCount: Int,
    val failureCount: Int,
    val tableStatuses: Map<String, String>
)

class CloudSyncManager private constructor(private val appContext: Context) {

    private val db = AppDatabase.getInstance(appContext)
    private val authManager = SupabaseAuthManager.getInstance(appContext)
    private val apiClient = SupabaseApiClient(appContext)
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val _syncState = MutableStateFlow(CloudSyncUiState())
    val syncState: StateFlow<CloudSyncUiState> = _syncState.asStateFlow()

    private var autoSyncJob: Job? = null

    init {
        scope.launch {
            authManager.authState.collect { auth ->
                val session = (auth as? AuthState.Authenticated)?.session
                _syncState.value = _syncState.value.copy(
                    isAuthenticated = session != null,
                    userEmail = session?.email,
                    userPhone = session?.phone
                )
                if (session != null) {
                    try {
                        val bizCount = db.businessDao().getAllBusinessesSync().size
                        if (bizCount == 0) {
                            performSync(isManual = false)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        scope.launch {
            db.syncQueueDao().getPendingCount().collect { count ->
                _syncState.value = _syncState.value.copy(pendingQueueCount = count)
            }
        }

        startAutoSyncLoop()
    }

    companion object {
        @Volatile
        private var INSTANCE: CloudSyncManager? = null

        fun getInstance(context: Context): CloudSyncManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CloudSyncManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private fun startAutoSyncLoop() {
        autoSyncJob?.cancel()
        autoSyncJob = scope.launch {
            while (isActive) {
                delay(60_000L) // Check every 60 seconds
                if (authManager.isAuthenticated()) {
                    val pendingCount = db.syncQueueDao().getPendingCountSync()
                    if (pendingCount > 0) {
                        performSync(isManual = false)
                    }
                }
            }
        }
    }

    suspend fun enqueueChange(
        entityType: String,
        entityId: String,
        operation: String,
        payloadJson: String
    ) = withContext(Dispatchers.IO) {
        db.syncQueueDao().deletePendingForEntity(entityType, entityId)
        val entry = SyncQueueEntity(
            entityType = entityType,
            entityId = entityId,
            operation = operation,
            payloadJson = payloadJson,
            createdAt = System.currentTimeMillis()
        )
        db.syncQueueDao().insert(entry)
    }

    suspend fun triggerSync(isManual: Boolean = true): Result<String> = withContext(Dispatchers.IO) {
        performSync(isManual)
    }

    private suspend fun performSync(isManual: Boolean): Result<String> = withContext(Dispatchers.IO) {
        val session = authManager.getCurrentSession()
        if (session == null) {
            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                lastError = "Sign in to enable Cloud Synchronization"
            )
            return@withContext Result.failure(Exception("Not authenticated"))
        }

        _syncState.value = _syncState.value.copy(
            isSyncing = true,
            statusMessage = "Synchronizing local records...",
            lastError = null
        )

        var pullResult = PullResultSummary(0, 0, 0, emptyMap())
        try {
            val isFreshLocal = db.invoiceDao().getAllInvoicesGlobalSync().isEmpty() &&
                               db.partyDao().getAllPartiesGlobalSync().isEmpty()

            if (!isFreshLocal) {
                // Step 1: Pre-sync active business and repository states to database
                BusinessManager.getInstance(appContext).syncToDatabase()
                InvoiceRepository.getInstance(appContext).syncToDatabase()
                PartyRepository.getInstance(appContext).syncToDatabase()
                ItemRepository.getInstance(appContext).syncToDatabase()
                TransactionRepository.getInstance(appContext).syncToDatabase()

                // Step 2: Push pending local changes to Supabase (Topological Order)
                _syncState.value = _syncState.value.copy(statusMessage = "Uploading changes to Cloud...")
                executePush(session)
            }

            // Step 3: Pull remote updates from Supabase (Delta Sync)
            _syncState.value = _syncState.value.copy(statusMessage = "Downloading updates from Cloud...")
            pullResult = executePull(session)

            if (isFreshLocal) {
                executePush(session)
            }

            val statusMsg = when {
                pullResult.failureCount == 0 -> "Cloud Sync Complete"
                pullResult.successCount > 0 -> "Cloud Sync Partial Failure (${pullResult.failureCount} tables failed)"
                else -> "Cloud Sync Failed"
            }
            val errStr = if (pullResult.failureCount > 0) "Errors in ${pullResult.failureCount} tables: ${pullResult.tableStatuses.filter { it.value.startsWith("ERROR") }}" else null

            val now = System.currentTimeMillis()
            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                lastSyncTimeMillis = now,
                statusMessage = statusMsg,
                lastError = errStr,
                totalSyncedRecords = pullResult.totalPulled
            )

            if (pullResult.failureCount > 0) {
                Result.failure(Exception(errStr))
            } else {
                Result.success("Sync completed successfully")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                lastError = e.localizedMessage ?: "Sync error",
                statusMessage = "Sync failed"
            )
            Result.failure(e)
        } finally {
            // Step 4: Refresh active in-memory repositories (Always executed in finally block)
            InvoiceRepository.getInstance(appContext).reloadFromDatabase()
            PartyRepository.getInstance(appContext).reloadFromDatabase()
            ItemRepository.getInstance(appContext).reloadFromDatabase()
            TransactionRepository.getInstance(appContext).reloadFromDatabase()
        }
    }

    private suspend fun executePush(session: UserSession): Int {
        val pending = db.syncQueueDao().getPendingItems(limit = 200)
        if (pending.isEmpty()) return 0

        val idsToMark = pending.map { it.id }
        db.syncQueueDao().markInProgress(idsToMark)

        // Topological ordering: parent tables first
        val orderMap = mapOf(
            "business" to 1,
            "party" to 2,
            "account" to 3,
            "item" to 4,
            "invoice" to 5,
            "invoice_item" to 6,
            "payment" to 7,
            "expense" to 8,
            "khata_entry" to 9,
            "journal_entry" to 10,
            "journal_line" to 11
        )

        val sorted = pending.sortedBy { orderMap[it.entityType] ?: 99 }
        val syncedIds = mutableListOf<Long>()

        for (item in sorted) {
            try {
                val table = when (item.entityType) {
                    "business" -> "businesses"
                    "party" -> "parties"
                    "item" -> "items"
                    "invoice" -> "invoices"
                    "invoice_item" -> "invoice_items"
                    "payment" -> "payments"
                    "expense" -> "expenses"
                    "account" -> "accounts"
                    "khata_entry" -> "khata_entries"
                    "journal_entry" -> "journal_entries"
                    "journal_line" -> "journal_entry_lines"
                    else -> item.entityType
                }

                if (item.operation == "DELETE") {
                    apiClient.softDeleteBatch(table, session.accessToken, listOf(item.entityId)).getOrThrow()
                } else {
                    val jsonObj = JSONObject(item.payloadJson)
                    jsonObj.put("user_id", session.userId)
                    val array = JSONArray().put(jsonObj)
                    apiClient.upsertBatch(table, session.accessToken, array, onConflict = "id").getOrThrow()
                }
                syncedIds.add(item.id)
            } catch (e: Exception) {
                val retry = item.retryCount + 1
                val backoff = (Math.pow(2.0, retry.toDouble()).toLong() * 1000L).coerceAtMost(3600000L)
                db.syncQueueDao().updateStatus(
                    id = item.id,
                    status = if (retry > 5) "FAILED" else "PENDING",
                    error = e.localizedMessage,
                    retryCount = retry,
                    nextRetryAt = System.currentTimeMillis() + backoff
                )
            }
        }

        if (syncedIds.isNotEmpty()) {
            db.syncQueueDao().deleteByIds(syncedIds)
        }
        return syncedIds.size
    }

    private suspend fun executePull(session: UserSession): PullResultSummary {
        val tables = listOf(
            "businesses", "parties", "accounts", "items", "invoices",
            "invoice_items", "payments", "expenses", "khata_entries",
            "journal_entries", "journal_entry_lines"
        )

        var totalPulled = 0
        var successCount = 0
        var failureCount = 0
        val tableStatuses = mutableMapOf<String, String>()

        for (table in tables) {
            val metadata = db.syncMetadataDao().getMetadata(table)
            val lastPulled = metadata?.lastPulledAt ?: 0L

            val result = apiClient.fetchDelta(table, session.accessToken, lastPulled)
            if (result.isSuccess) {
                val records = result.getOrThrow()
                android.util.Log.i("CloudSyncManager", "PULL SUCCESS table=$table records=${records.length()}")
                try {
                    if (records.length() > 0) {
                        db.withTransaction {
                            applyRemoteRecords(table, records)
                        }
                        totalPulled += records.length()
                    }
                    successCount++
                    tableStatuses[table] = "SUCCESS (${records.length()} records)"

                    db.syncMetadataDao().insertOrUpdate(
                        SyncMetadataEntity(
                            tableName = table,
                            lastPulledAt = System.currentTimeMillis(),
                            lastSyncStatus = "SUCCESS"
                        )
                    )
                } catch (e: Exception) {
                    failureCount++
                    val errMsg = "Error applying remote records for $table: ${e.localizedMessage}"
                    tableStatuses[table] = "ERROR: $errMsg"
                    android.util.Log.e("CloudSyncManager", "PULL APPLY ERROR table=$table", e)
                    // Keep previous lastPulledAt so retry fetches them again
                    db.syncMetadataDao().insertOrUpdate(
                        SyncMetadataEntity(
                            tableName = table,
                            lastPulledAt = lastPulled,
                            lastSyncStatus = "FAILED"
                        )
                    )
                }
            } else {
                failureCount++
                val ex = result.exceptionOrNull()
                val errMsg = ex?.localizedMessage ?: "Unknown error"
                tableStatuses[table] = "ERROR: $errMsg"
                android.util.Log.e("CloudSyncManager", "PULL FAILURE table=$table error=$errMsg")
                db.syncMetadataDao().insertOrUpdate(
                    SyncMetadataEntity(
                        tableName = table,
                        lastPulledAt = lastPulled,
                        lastSyncStatus = "FAILED"
                    )
                )
            }
        }

        return PullResultSummary(totalPulled, successCount, failureCount, tableStatuses)
    }

    suspend fun ensureBusinessExists(businessId: String): Boolean {
        if (businessId.isBlank()) return false
        val existing = db.businessDao().getBusinessSync(businessId)
        if (existing != null) {
            return true
        }

        // Check if BusinessManager has this profile in memory/preferences
        val profiles = BusinessManager.getInstance(appContext).businesses.value
        val localProfile = profiles.find { p ->
            val bId = BusinessManager.getInstance(appContext).getBusinessDatabaseId(p)
            bId == businessId || p.shopName == businessId || p.gstin == businessId
        }

        if (localProfile != null) {
            val firstBank = localProfile.effectiveBankAccounts.firstOrNull() ?: BankDetails()
            val entity = BusinessEntity(
                id = businessId,
                name = localProfile.shopName,
                ownerName = localProfile.ownerName,
                address = localProfile.address,
                phone = localProfile.phone,
                email = localProfile.email,
                gstin = localProfile.gstin,
                pan = localProfile.pan,
                logoPath = localProfile.logoPath,
                gstEnabled = localProfile.isGstRegistered,
                financialYearStart = "01-04",
                upiId = localProfile.upiId,
                bankName = firstBank.bankName,
                accountNumber = firstBank.accountNumber,
                ifscCode = firstBank.ifscCode,
                termsAndConditions = localProfile.termsAndConditions,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            db.businessDao().insertOrUpdate(entity)
            android.util.Log.i("CloudSyncManager", "ensureBusinessExists: Registered local profile entity for businessId=$businessId")
            return true
        }

        // Missing cloud business: Create a clearly marked recovery placeholder to satisfy foreign key integrity
        val recoveryEntity = BusinessEntity(
            id = businessId,
            name = "Business ($businessId) [Pending Recovery]",
            ownerName = "",
            address = "",
            phone = "",
            email = "",
            gstin = "",
            pan = "",
            logoPath = "",
            gstEnabled = false,
            financialYearStart = "01-04",
            termsAndConditions = "Cloud restored entity pending profile update",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        db.businessDao().insertOrUpdate(recoveryEntity)
        android.util.Log.w("CloudSyncManager", "ensureBusinessExists: Inserted recovery placeholder for missing parent businessId=$businessId to satisfy foreign-key constraints")
        return true
    }

    private fun parseTimestamp(obj: JSONObject, key: String): Long {
        val optLong = obj.optLong(key, -1L)
        if (optLong != -1L) return optLong
        val str = obj.optString(key, "")
        if (str.isNotBlank()) {
            try {
                return java.time.Instant.parse(str).toEpochMilli()
            } catch (_: Exception) {
                try {
                    return str.toLong()
                } catch (_: Exception) {}
            }
        }
        return System.currentTimeMillis()
    }

    private suspend fun applyRemoteRecords(table: String, records: JSONArray) {
        when (table) {
            "businesses" -> {
                val list = mutableListOf<BusinessEntity>()
                val businessProfiles = mutableListOf<BusinessProfile>()
                for (i in 0 until records.length()) {
                    val obj = records.getJSONObject(i)
                    val remote = BusinessEntity(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        ownerName = obj.optString("owner_name", ""),
                        address = obj.optString("address", ""),
                        phone = obj.optString("phone", ""),
                        email = obj.optString("email", ""),
                        gstin = obj.optString("gstin", ""),
                        pan = obj.optString("pan", ""),
                        logoPath = obj.optString("logo_path", ""),
                        gstEnabled = obj.optBoolean("gst_enabled", false),
                        financialYearStart = obj.optString("financial_year_start", "01-04"),
                        upiId = obj.optString("upi_id", ""),
                        bankName = obj.optString("bank_name", ""),
                        accountNumber = obj.optString("account_number", ""),
                        ifscCode = obj.optString("ifsc_code", ""),
                        termsAndConditions = obj.optString("terms_and_conditions", ""),
                        createdAt = parseTimestamp(obj, "created_at"),
                        updatedAt = parseTimestamp(obj, "updated_at")
                    )
                    val local = db.businessDao().getBusinessSync(remote.id)
                    val resolved = ConflictResolver.resolveBusiness(local, remote)
                    list.add(resolved)

                    businessProfiles.add(
                        BusinessProfile(
                            id = resolved.id,
                            shopName = resolved.name,
                            ownerName = resolved.ownerName,
                            phone = resolved.phone,
                            email = resolved.email,
                            address = resolved.address,
                            isGstRegistered = resolved.gstEnabled,
                            gstin = resolved.gstin,
                            pan = resolved.pan,
                            upiId = resolved.upiId,
                            bankName = resolved.bankName,
                            accountNumber = resolved.accountNumber,
                            ifscCode = resolved.ifscCode,
                            termsAndConditions = resolved.termsAndConditions,
                            logoPath = resolved.logoPath,
                            hasCompletedOnboarding = true
                        )
                    )
                }
                if (list.isNotEmpty()) {
                    db.businessDao().insertAllBusinesses(list)
                    if (businessProfiles.isNotEmpty()) {
                        BusinessManager.getInstance(appContext).restoreBusinessesFromCloud(businessProfiles)
                    }
                }
            }
            "parties" -> {
                val list = mutableListOf<PartyEntity>()
                for (i in 0 until records.length()) {
                    val obj = records.getJSONObject(i)
                    val bId = obj.optString("business_id", "default_business")
                    ensureBusinessExists(bId)
                    list.add(
                        PartyEntity(
                            id = obj.getString("id"),
                            businessId = bId,
                            name = obj.getString("name"),
                            phone = obj.optString("phone", ""),
                            email = obj.optString("email", ""),
                            address = obj.optString("address", ""),
                            gstin = obj.optString("gstin", ""),
                            type = obj.optString("type", "CUSTOMER"),
                            tag = obj.optString("tag", "REGULAR"),
                            openingBalance = obj.optLong("opening_balance", 0L),
                            createdAt = parseTimestamp(obj, "created_at"),
                            updatedAt = parseTimestamp(obj, "updated_at"),
                            deletedAt = if (obj.isNull("deleted_at")) null else parseTimestamp(obj, "deleted_at")
                        )
                    )
                }
                if (list.isNotEmpty()) db.partyDao().insertAllParties(list)
            }
            "accounts" -> {
                val list = mutableListOf<AccountEntity>()
                for (i in 0 until records.length()) {
                    val obj = records.getJSONObject(i)
                    val bId = obj.optString("business_id", "default_business")
                    ensureBusinessExists(bId)
                    list.add(
                        AccountEntity(
                            id = obj.getString("id"),
                            businessId = bId,
                            name = obj.getString("name"),
                            type = obj.optString("type", "CASH"),
                            openingBalance = obj.optLong("opening_balance", 0L),
                            accountNumber = obj.optString("account_number", ""),
                            ifscCode = obj.optString("ifsc_code", ""),
                            createdAt = parseTimestamp(obj, "created_at"),
                            updatedAt = parseTimestamp(obj, "updated_at"),
                            deletedAt = if (obj.isNull("deleted_at")) null else parseTimestamp(obj, "deleted_at")
                        )
                    )
                }
                if (list.isNotEmpty()) db.accountDao().insertAllAccounts(list)
            }
            "items" -> {
                val list = mutableListOf<ItemEntity>()
                for (i in 0 until records.length()) {
                    val obj = records.getJSONObject(i)
                    val bId = obj.optString("business_id", "default_business")
                    ensureBusinessExists(bId)
                    list.add(
                        ItemEntity(
                            id = obj.getString("id"),
                            businessId = bId,
                            name = obj.getString("name"),
                            itemCode = obj.optString("item_code", ""),
                            unit = obj.optString("unit", "Pcs"),
                            hsnCode = obj.optString("hsn_code", ""),
                            purchasePrice = obj.optLong("purchase_price", 0L),
                            sellPrice = obj.optLong("sell_price", 0L),
                            gstRate = obj.optDouble("gst_rate", 0.0),
                            category = obj.optString("category", "General"),
                            stockQty = obj.optDouble("stock_qty", 0.0),
                            lowStockThreshold = obj.optDouble("low_stock_threshold", 5.0),
                            createdAt = parseTimestamp(obj, "created_at"),
                            updatedAt = parseTimestamp(obj, "updated_at"),
                            deletedAt = if (obj.isNull("deleted_at")) null else parseTimestamp(obj, "deleted_at")
                        )
                    )
                }
                if (list.isNotEmpty()) db.itemDao().insertAllItems(list)
            }
            "invoices" -> {
                val list = mutableListOf<InvoiceEntity>()
                for (i in 0 until records.length()) {
                    val obj = records.getJSONObject(i)
                    val bId = obj.optString("business_id", "default_business")
                    ensureBusinessExists(bId)

                    val rawPartyId = obj.optString("party_id", "").ifBlank { null }
                    val safePartyId = if (rawPartyId != null) {
                        val partyExists = db.partyDao().getPartyByIdSync(rawPartyId) != null
                        if (partyExists) {
                            rawPartyId
                        } else {
                            val invNo = obj.optString("invoice_no", obj.getString("id"))
                            val custName = obj.optString("customer_name", "")
                            android.util.Log.w(
                                "CloudSyncManager",
                                "Invoice $invNo references missing party $rawPartyId; setting party_id to null while preserving customerName=$custName on invoice"
                            )
                            null
                        }
                    } else null

                    list.add(
                        InvoiceEntity(
                            id = obj.getString("id"),
                            businessId = bId,
                            invoiceNo = obj.getString("invoice_no"),
                            date = parseTimestamp(obj, "date"),
                            partyId = safePartyId,
                            customerName = obj.optString("customer_name", ""),
                            customerPhone = obj.optString("customer_phone", ""),
                            customerAddress = obj.optString("customer_address", ""),
                            customerGstin = obj.optString("customer_gstin", ""),
                            type = obj.optString("type", "NON_GST_BILL"),
                            gstMode = obj.optString("gst_mode", "EXEMPT"),
                            subtotal = obj.optLong("subtotal", 0L),
                            discount = obj.optLong("discount", 0L),
                            taxableAmount = obj.optLong("taxable_amount", 0L),
                            cgst = obj.optLong("cgst", 0L),
                            sgst = obj.optLong("sgst", 0L),
                            igst = obj.optLong("igst", 0L),
                            total = obj.optLong("total", 0L),
                            paidAmount = obj.optLong("paid_amount", 0L),
                            paymentStatus = obj.optString("payment_status", "PAID"),
                            paymentMode = obj.optString("payment_mode", "Cash"),
                            notes = obj.optString("notes", ""),
                            isGst = obj.optBoolean("is_gst", false),
                            createdAt = parseTimestamp(obj, "created_at"),
                            updatedAt = parseTimestamp(obj, "updated_at"),
                            deletedAt = if (obj.isNull("deleted_at")) null else parseTimestamp(obj, "deleted_at")
                        )
                    )
                }
                if (list.isNotEmpty()) db.invoiceDao().insertAllInvoices(list)
            }
            "invoice_items" -> {
                val list = mutableListOf<InvoiceItemEntity>()
                for (i in 0 until records.length()) {
                    val obj = records.getJSONObject(i)
                    val invoiceId = obj.getString("invoice_id")
                    // Check if parent invoice exists locally
                    if (db.invoiceDao().getInvoiceByIdSync(invoiceId) == null) {
                        android.util.Log.w("CloudSyncManager", "InvoiceItem references missing invoice $invoiceId; postponing table for retry")
                        throw IllegalStateException("InvoiceItem references missing invoice $invoiceId; will retry after invoice is restored")
                    }
                    val rawItemId = obj.optString("item_id", "").ifBlank { null }
                    val safeItemId = if (rawItemId != null) {
                        if (db.itemDao().getItemByIdSync(rawItemId) != null) rawItemId else null
                    } else null

                    list.add(
                        InvoiceItemEntity(
                            id = obj.getString("id"),
                            invoiceId = invoiceId,
                            itemId = safeItemId,
                            itemName = obj.getString("item_name"),
                            hsnCode = obj.optString("hsn_code", ""),
                            qty = obj.optDouble("qty", 1.0),
                            unit = obj.optString("unit", "Pcs"),
                            rate = obj.optLong("rate", 0L),
                            discount = obj.optLong("discount", 0L),
                            cgstRate = obj.optDouble("cgst_rate", 0.0),
                            sgstRate = obj.optDouble("sgst_rate", 0.0),
                            igstRate = obj.optDouble("igst_rate", 0.0),
                            amount = obj.optLong("amount", 0L),
                            deletedAt = if (obj.isNull("deleted_at")) null else parseTimestamp(obj, "deleted_at")
                        )
                    )
                }
                if (list.isNotEmpty()) db.invoiceDao().insertInvoiceItems(list)
            }
            "payments" -> {
                val list = mutableListOf<PaymentEntity>()
                for (i in 0 until records.length()) {
                    val obj = records.getJSONObject(i)
                    val bId = obj.optString("business_id", "default_business")
                    ensureBusinessExists(bId)

                    val rawPartyId = obj.optString("party_id", "").ifBlank { null }
                    val safePartyId = if (rawPartyId != null) {
                        if (db.partyDao().getPartyByIdSync(rawPartyId) != null) rawPartyId else {
                            android.util.Log.w("CloudSyncManager", "Payment references missing party $rawPartyId; setting party_id to null")
                            null
                        }
                    } else null

                    val rawInvId = obj.optString("linked_invoice_id", "").ifBlank { null }
                    val safeInvId = if (rawInvId != null) {
                        if (db.invoiceDao().getInvoiceByIdSync(rawInvId) != null) rawInvId else null
                    } else null

                    list.add(
                        PaymentEntity(
                            id = obj.getString("id"),
                            businessId = bId,
                            partyId = safePartyId,
                            date = parseTimestamp(obj, "date"),
                            amount = obj.optLong("amount", 0L),
                            mode = obj.optString("mode", "Cash"),
                            referenceNo = obj.optString("reference_no", ""),
                            notes = obj.optString("notes", ""),
                            linkedInvoiceId = safeInvId,
                            createdAt = parseTimestamp(obj, "created_at"),
                            updatedAt = parseTimestamp(obj, "updated_at"),
                            deletedAt = if (obj.isNull("deleted_at")) null else parseTimestamp(obj, "deleted_at")
                        )
                    )
                }
                if (list.isNotEmpty()) db.paymentDao().insertAllPayments(list)
            }
            "expenses" -> {
                val list = mutableListOf<ExpenseEntity>()
                for (i in 0 until records.length()) {
                    val obj = records.getJSONObject(i)
                    val bId = obj.optString("business_id", "default_business")
                    ensureBusinessExists(bId)

                    list.add(
                        ExpenseEntity(
                            id = obj.getString("id"),
                            businessId = bId,
                            date = parseTimestamp(obj, "date"),
                            category = obj.getString("category"),
                            amount = obj.optLong("amount", 0L),
                            description = obj.optString("description", ""),
                            mode = obj.optString("mode", "Cash"),
                            receiptPath = obj.optString("receipt_path", ""),
                            createdAt = parseTimestamp(obj, "created_at"),
                            updatedAt = parseTimestamp(obj, "updated_at"),
                            deletedAt = if (obj.isNull("deleted_at")) null else parseTimestamp(obj, "deleted_at")
                        )
                    )
                }
                if (list.isNotEmpty()) db.expenseDao().insertAllExpenses(list)
            }
            "khata_entries" -> {
                val list = mutableListOf<KhataEntryEntity>()
                for (i in 0 until records.length()) {
                    val obj = records.getJSONObject(i)
                    val rawBId = obj.optString("business_id", "default_business")
                    val pId = obj.getString("party_id")
                    val existingParty = db.partyDao().getPartyByIdSync(pId)

                    // Reconcile business_id: if parent party belongs to a specific business (e.g. biz_abc_ltd),
                    // resolve to the party's business_id so khata entries are never misfiled under default_business!
                    val resolvedBId = if (existingParty != null && existingParty.businessId.isNotBlank() &&
                        (rawBId == "default_business" || rawBId.isBlank())) {
                        existingParty.businessId
                    } else {
                        rawBId
                    }
                    ensureBusinessExists(resolvedBId)

                    if (existingParty == null) {
                        val recoveryParty = PartyEntity(
                            id = pId,
                            businessId = resolvedBId,
                            name = "Party ($pId) [Pending Recovery]",
                            phone = "",
                            type = "CUSTOMER"
                        )
                        db.partyDao().insertParty(recoveryParty)
                        android.util.Log.w("CloudSyncManager", "Created recovery party for khata_entry partyId=$pId")
                    }

                    val rawEntity = KhataEntryEntity(
                        id = obj.getString("id"),
                        businessId = resolvedBId,
                        partyId = pId,
                        amount = obj.optLong("amount", 0L),
                        type = obj.getString("type"),
                        date = parseTimestamp(obj, "date"),
                        billNumber = obj.optString("bill_number", ""),
                        note = obj.optString("note", ""),
                        createdAt = parseTimestamp(obj, "created_at"),
                        updatedAt = parseTimestamp(obj, "updated_at"),
                        deletedAt = if (obj.isNull("deleted_at")) null else parseTimestamp(obj, "deleted_at")
                    )
                    val aligned = com.hisabpro.app.data.local.KhataAlignment.alignBusinessId(rawEntity, db.partyDao())
                    ensureBusinessExists(aligned.businessId)
                    list.add(aligned)
                }
                if (list.isNotEmpty()) db.khataDao().insertAllEntries(list)
            }
            "journal_entries" -> {
                val list = mutableListOf<JournalEntryEntity>()
                for (i in 0 until records.length()) {
                    val obj = records.getJSONObject(i)
                    val bId = obj.optString("business_id", "default_business")
                    ensureBusinessExists(bId)

                    list.add(
                        JournalEntryEntity(
                            id = obj.getString("id"),
                            businessId = bId,
                            date = parseTimestamp(obj, "date"),
                            voucherNo = obj.optString("voucher_no", ""),
                            narration = obj.optString("narration", ""),
                            createdAt = parseTimestamp(obj, "created_at"),
                            updatedAt = parseTimestamp(obj, "updated_at"),
                            deletedAt = if (obj.isNull("deleted_at")) null else parseTimestamp(obj, "deleted_at")
                        )
                    )
                }
                if (list.isNotEmpty()) db.journalDao().insertAllJournalEntries(list)
            }
            "journal_entry_lines" -> {
                val list = mutableListOf<JournalEntryLineEntity>()
                for (i in 0 until records.length()) {
                    val obj = records.getJSONObject(i)
                    val journalEntryId = obj.getString("journal_entry_id")
                    val accountId = obj.getString("account_id")

                    // Ensure parent account exists
                    if (db.accountDao().getAccountByIdSync(accountId) == null) {
                        val recoveryAccount = AccountEntity(
                            id = accountId,
                            businessId = "default_business",
                            name = "Account ($accountId) [Pending Recovery]",
                            type = "ASSET"
                        )
                        db.accountDao().insertAccount(recoveryAccount)
                    }

                    list.add(
                        JournalEntryLineEntity(
                            id = obj.getString("id"),
                            journalEntryId = journalEntryId,
                            accountId = accountId,
                            accountName = obj.optString("account_name", ""),
                            isDebit = obj.optBoolean("is_debit", true),
                            debit = obj.optLong("debit", 0L),
                            credit = obj.optLong("credit", 0L),
                            createdAt = parseTimestamp(obj, "created_at"),
                            updatedAt = parseTimestamp(obj, "updated_at"),
                            deletedAt = if (obj.isNull("deleted_at")) null else parseTimestamp(obj, "deleted_at")
                        )
                    )
                }
                if (list.isNotEmpty()) db.journalDao().insertJournalLines(list)
            }
        }
    }
}
