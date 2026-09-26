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

        try {
            // Step 1: Ensure repositories flush uncommitted state to database
            InvoiceRepository.getInstance(appContext).syncToDatabase()
            PartyRepository.getInstance(appContext).syncToDatabase()
            ItemRepository.getInstance(appContext).syncToDatabase()
            TransactionRepository.getInstance(appContext).syncToDatabase()

            // Step 2: Push pending local changes to Supabase (Topological Order)
            _syncState.value = _syncState.value.copy(statusMessage = "Uploading changes to Cloud...")
            val pushResult = executePush(session)

            // Step 3: Pull remote updates from Supabase (Delta Sync)
            _syncState.value = _syncState.value.copy(statusMessage = "Downloading updates from Cloud...")
            val pullResult = executePull(session)

            // Step 4: Refresh active in-memory repositories
            InvoiceRepository.getInstance(appContext).reloadFromDatabase()
            PartyRepository.getInstance(appContext).reloadFromDatabase()
            ItemRepository.getInstance(appContext).reloadFromDatabase()
            TransactionRepository.getInstance(appContext).reloadFromDatabase()

            val now = System.currentTimeMillis()
            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                lastSyncTimeMillis = now,
                statusMessage = "Cloud Sync Complete",
                lastError = null
            )

            Result.success("Sync completed successfully")
        } catch (e: Exception) {
            e.printStackTrace()
            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                lastError = e.localizedMessage ?: "Sync error",
                statusMessage = "Sync failed"
            )
            Result.failure(e)
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

    private suspend fun executePull(session: UserSession): Int {
        val tables = listOf(
            "businesses", "parties", "accounts", "items", "invoices",
            "invoice_items", "payments", "expenses", "khata_entries",
            "journal_entries", "journal_entry_lines"
        )

        var totalPulled = 0

        for (table in tables) {
            val metadata = db.syncMetadataDao().getMetadata(table)
            val lastPulled = metadata?.lastPulledAt ?: 0L

            val result = apiClient.fetchDelta(table, session.accessToken, lastPulled)
            if (result.isSuccess) {
                val records = result.getOrThrow()
                if (records.length() > 0) {
                    db.withTransaction {
                        applyRemoteRecords(table, records)
                    }
                    totalPulled += records.length()
                }

                db.syncMetadataDao().insertOrUpdate(
                    SyncMetadataEntity(
                        tableName = table,
                        lastPulledAt = System.currentTimeMillis(),
                        lastSyncStatus = "SUCCESS"
                    )
                )
            }
        }

        return totalPulled
    }

    private suspend fun applyRemoteRecords(table: String, records: JSONArray) {
        when (table) {
            "businesses" -> {
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
                        createdAt = obj.optLong("created_at", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updated_at", System.currentTimeMillis())
                    )
                    val local = db.businessDao().getBusinessSync(remote.id)
                    val resolved = ConflictResolver.resolveBusiness(local, remote)
                    db.businessDao().insertOrUpdate(resolved)
                }
            }
            "parties" -> {
                val list = mutableListOf<PartyEntity>()
                for (i in 0 until records.length()) {
                    val obj = records.getJSONObject(i)
                    list.add(
                        PartyEntity(
                            id = obj.getString("id"),
                            businessId = obj.optString("business_id", "default_business"),
                            name = obj.getString("name"),
                            phone = obj.optString("phone", ""),
                            email = obj.optString("email", ""),
                            address = obj.optString("address", ""),
                            gstin = obj.optString("gstin", ""),
                            type = obj.optString("type", "CUSTOMER"),
                            tag = obj.optString("tag", "REGULAR"),
                            openingBalance = obj.optLong("opening_balance", 0L),
                            createdAt = obj.optLong("created_at", System.currentTimeMillis()),
                            updatedAt = obj.optLong("updated_at", System.currentTimeMillis()),
                            deletedAt = if (obj.isNull("deleted_at")) null else obj.optLong("deleted_at")
                        )
                    )
                }
                if (list.isNotEmpty()) db.partyDao().insertAllParties(list)
            }
            "items" -> {
                val list = mutableListOf<ItemEntity>()
                for (i in 0 until records.length()) {
                    val obj = records.getJSONObject(i)
                    list.add(
                        ItemEntity(
                            id = obj.getString("id"),
                            businessId = obj.optString("business_id", "default_business"),
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
                            createdAt = obj.optLong("created_at", System.currentTimeMillis()),
                            updatedAt = obj.optLong("updated_at", System.currentTimeMillis()),
                            deletedAt = if (obj.isNull("deleted_at")) null else obj.optLong("deleted_at")
                        )
                    )
                }
                if (list.isNotEmpty()) db.itemDao().insertAllItems(list)
            }
            "invoices" -> {
                val list = mutableListOf<InvoiceEntity>()
                for (i in 0 until records.length()) {
                    val obj = records.getJSONObject(i)
                    list.add(
                        InvoiceEntity(
                            id = obj.getString("id"),
                            businessId = obj.optString("business_id", "default_business"),
                            invoiceNo = obj.getString("invoice_no"),
                            date = obj.optLong("date", System.currentTimeMillis()),
                            partyId = obj.optString("party_id", "").ifBlank { null },
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
                            createdAt = obj.optLong("created_at", System.currentTimeMillis()),
                            updatedAt = obj.optLong("updated_at", System.currentTimeMillis()),
                            deletedAt = if (obj.isNull("deleted_at")) null else obj.optLong("deleted_at")
                        )
                    )
                }
                if (list.isNotEmpty()) db.invoiceDao().insertAllInvoices(list)
            }
            "payments" -> {
                val list = mutableListOf<PaymentEntity>()
                for (i in 0 until records.length()) {
                    val obj = records.getJSONObject(i)
                    list.add(
                        PaymentEntity(
                            id = obj.getString("id"),
                            businessId = obj.optString("business_id", "default_business"),
                            partyId = obj.optString("party_id", "").ifBlank { null },
                            date = obj.optLong("date", System.currentTimeMillis()),
                            amount = obj.optLong("amount", 0L),
                            mode = obj.optString("mode", "Cash"),
                            referenceNo = obj.optString("reference_no", ""),
                            notes = obj.optString("notes", ""),
                            linkedInvoiceId = obj.optString("linked_invoice_id", "").ifBlank { null },
                            createdAt = obj.optLong("created_at", System.currentTimeMillis()),
                            updatedAt = obj.optLong("updated_at", System.currentTimeMillis()),
                            deletedAt = if (obj.isNull("deleted_at")) null else obj.optLong("deleted_at")
                        )
                    )
                }
                if (list.isNotEmpty()) db.paymentDao().insertAllPayments(list)
            }
            "expenses" -> {
                val list = mutableListOf<ExpenseEntity>()
                for (i in 0 until records.length()) {
                    val obj = records.getJSONObject(i)
                    list.add(
                        ExpenseEntity(
                            id = obj.getString("id"),
                            businessId = obj.optString("business_id", "default_business"),
                            date = obj.optLong("date", System.currentTimeMillis()),
                            category = obj.getString("category"),
                            amount = obj.optLong("amount", 0L),
                            description = obj.optString("description", ""),
                            mode = obj.optString("mode", "Cash"),
                            receiptPath = obj.optString("receipt_path", ""),
                            createdAt = obj.optLong("created_at", System.currentTimeMillis()),
                            updatedAt = obj.optLong("updated_at", System.currentTimeMillis()),
                            deletedAt = if (obj.isNull("deleted_at")) null else obj.optLong("deleted_at")
                        )
                    )
                }
                if (list.isNotEmpty()) db.expenseDao().insertAllExpenses(list)
            }
            "khata_entries" -> {
                val list = mutableListOf<KhataEntryEntity>()
                for (i in 0 until records.length()) {
                    val obj = records.getJSONObject(i)
                    list.add(
                        KhataEntryEntity(
                            id = obj.getString("id"),
                            partyId = obj.getString("party_id"),
                            amount = obj.optLong("amount", 0L),
                            type = obj.getString("type"),
                            date = obj.optLong("date", System.currentTimeMillis()),
                            billNumber = obj.optString("bill_number", ""),
                            note = obj.optString("note", ""),
                            createdAt = obj.optLong("created_at", System.currentTimeMillis()),
                            updatedAt = obj.optLong("updated_at", System.currentTimeMillis()),
                            deletedAt = if (obj.isNull("deleted_at")) null else obj.optLong("deleted_at")
                        )
                    )
                }
                if (list.isNotEmpty()) db.khataDao().insertAllEntries(list)
            }
        }
    }
}
