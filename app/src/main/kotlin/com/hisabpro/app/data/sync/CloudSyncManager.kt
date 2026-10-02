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
import com.hisabpro.app.data.model.BusinessProfile
import com.hisabpro.app.data.repository.BusinessManager
import com.hisabpro.app.data.repository.InvoiceRepository
import com.hisabpro.app.data.repository.ItemRepository
import com.hisabpro.app.data.repository.PartyRepository
import com.hisabpro.app.data.repository.PurchaseRepository
import com.hisabpro.app.data.repository.SettingsRepository
import com.hisabpro.app.data.repository.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val syncMutex = Mutex()

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
                        val lastUserId = authManager.getLastActiveUserId()
                        val isUserSwitch = (lastUserId == null) || (lastUserId != session.userId) || authManager.isDifferentUser(session)

                        if (isUserSwitch) {
                            android.util.Log.i("SYNC_USER_SWITCH", "User switch detected for ${session.userId} (${session.email}) [lastUserId=$lastUserId]. Resetting local workspace.")
                            handleUserSwitch(session)
                        } else {
                            authManager.setLastActiveUserId(session.userId)
                            authManager.setLastActiveUserEmail(session.email)
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
        val entry = SyncQueueEntity(
            entityType = entityType,
            entityId = entityId,
            operation = operation,
            payloadJson = payloadJson,
            createdAt = System.currentTimeMillis()
        )
        val insertedId = db.syncQueueDao().insert(entry)

        val tableName = when (entityType) {
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
            else -> entityType
        }

        val idempotencyKey = "idem_${entityType}_${entityId}_${System.currentTimeMillis()}"
        val wrappedPayload = "{\"idempotency_key\":\"$idempotencyKey\",\"data\":$payloadJson}"

        val outboxEvent = com.hisabpro.app.data.local.entity.SyncOutbox(
            eventId = java.util.UUID.randomUUID().toString(),
            entityId = entityId,
            tableName = tableName,
            operationType = if (operation == "UPSERT") "INSERT" else operation,
            payload = wrappedPayload,
            createdAt = System.currentTimeMillis()
        )
        db.syncOutboxDao().insert(outboxEvent)

        val bizId = try {
            JSONObject(payloadJson).optString("business_id", JSONObject(payloadJson).optString("businessId", "default_business"))
        } catch (_: Exception) { "default_business" }
        android.util.Log.i("SYNC_QUEUE_DEBUG", "QUEUE_ENQUEUE table=$entityType recordId=$entityId businessId=$bizId operation=$operation queueId=$insertedId")
    }

    suspend fun triggerSync(isManual: Boolean = true): Result<String> = withContext(Dispatchers.IO) {
        performSync(isManual)
    }

    private suspend fun performSync(isManual: Boolean): Result<String> = withContext(Dispatchers.IO) {
        if (syncMutex.isLocked) {
            android.util.Log.i("SYNC_QUEUE_DEBUG", "SYNC_SKIPPED performSync requested while another sync is running")
        }
        syncMutex.withLock {
            performSyncInternal(isManual)
        }
    }

    private suspend fun performSyncInternal(isManual: Boolean): Result<String> = withContext(Dispatchers.IO) {
        android.util.Log.i("SYNC_QUEUE_DEBUG", "SYNC_START isManual=$isManual")

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
            // Step 0: Manual sync unblocks any failed or queued backoff items
            if (isManual) {
                val resetCount = db.syncQueueDao().resetAllForManualSync()
                if (resetCount > 0) {
                    android.util.Log.i("SYNC_QUEUE_DEBUG", "QUEUE_MANUAL_RESET_FAILED_ITEMS count=$resetCount")
                }
            }

            // Step 0b: Recover stale IN_PROGRESS records (> 30 seconds old)
            val cutoffTime = System.currentTimeMillis() - 30_000L
            val recoveredCount = db.syncQueueDao().resetStaleInProgress(cutoffTime)
            if (recoveredCount > 0) {
                android.util.Log.i("SYNC_QUEUE_DEBUG", "QUEUE_RECOVER_STALE_IN_PROGRESS count=$recoveredCount")
            }

            val initialPendingCount = db.syncQueueDao().getPendingCountSync()
            android.util.Log.i("SYNC_QUEUE_DEBUG", "QUEUE_PENDING_COUNT count=$initialPendingCount")

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

            android.util.Log.i("SYNC_QUEUE_DEBUG", "SYNC_END status=$statusMsg")

            if (pullResult.failureCount > 0) {
                Result.failure(Exception(errStr))
            } else {
                Result.success("Sync completed successfully")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("SYNC_QUEUE_DEBUG", "SYNC_END failed error=${e.localizedMessage}")
            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                lastError = e.localizedMessage ?: "Sync error",
                statusMessage = "Sync failed"
            )
            Result.failure(e)
        }
    }

    private suspend fun executePush(session: UserSession): Int {
        android.util.Log.i("SYNC_QUEUE_DEBUG", "QUEUE_PUSH_START")
        val now = System.currentTimeMillis()
        val pending = db.syncQueueDao().getPendingItems(currentTime = now, limit = 200)
        android.util.Log.i("SYNC_QUEUE_DEBUG", "QUEUE_FOUND count=${pending.size}")
        if (pending.isEmpty()) {
            return 0
        }

        val idsToMark = pending.map { it.id }
        db.syncQueueDao().markInProgress(idsToMark, currentTime = now)

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

            android.util.Log.i("SYNC_QUEUE_DEBUG", "QUEUE_PROCESS_START queueId=${item.id} table=$table recordId=${item.entityId} status=${item.status}")
            android.util.Log.i("SYNC_QUEUE_DEBUG", "QUEUE_UPLOAD_START table=$table recordId=${item.entityId}")

            try {
                if (item.operation == "DELETE") {
                    val res = apiClient.softDeleteBatch(table, session.accessToken, listOf(item.entityId))
                    if (res.isSuccess) {
                        android.util.Log.i("SYNC_QUEUE_DEBUG", "QUEUE_UPLOAD_RESPONSE table=$table recordId=${item.entityId} httpStatus=200")
                        android.util.Log.i("SYNC_QUEUE_DEBUG", "QUEUE_UPLOAD_SUCCESS table=$table recordId=${item.entityId}")
                        syncedIds.add(item.id)
                    } else {
                        val ex = res.exceptionOrNull()
                        throw ex ?: Exception("Soft delete failed")
                    }
                } else {
                    val rawObj = JSONObject(item.payloadJson)
                    rawObj.put("user_id", session.userId)
                    val sanitizedObj = sanitizePayloadForTable(table, rawObj)
                    val array = JSONArray().put(sanitizedObj)
                    val res = apiClient.upsertBatch(table, session.accessToken, array, onConflict = "id")
                    if (res.isSuccess) {
                        android.util.Log.i("SYNC_QUEUE_DEBUG", "QUEUE_UPLOAD_RESPONSE table=$table recordId=${item.entityId} httpStatus=200")
                        android.util.Log.i("SYNC_QUEUE_DEBUG", "QUEUE_UPLOAD_SUCCESS table=$table recordId=${item.entityId}")
                        syncedIds.add(item.id)
                    } else {
                        val ex = res.exceptionOrNull()
                        throw ex ?: Exception("Upsert batch failed")
                    }
                }
            } catch (e: Exception) {
                val retry = item.retryCount + 1
                val backoff = (Math.pow(2.0, retry.toDouble()).toLong() * 1000L).coerceAtMost(3600000L)
                val nextRetry = System.currentTimeMillis() + backoff
                db.syncQueueDao().updateStatus(
                    id = item.id,
                    status = "FAILED",
                    error = e.localizedMessage,
                    retryCount = retry,
                    nextRetryAt = nextRetry
                )
                android.util.Log.e("SYNC_QUEUE_DEBUG", "QUEUE_UPLOAD_FAILED table=$table recordId=${item.entityId} error=${e.localizedMessage}")
            }
        }

        if (syncedIds.isNotEmpty()) {
            db.syncQueueDao().deleteByIds(syncedIds)
            for (item in sorted) {
                if (item.id in syncedIds) {
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
                    db.syncOutboxDao().deleteByEntity(table, item.entityId)
                }
            }
            for (syncedId in syncedIds) {
                android.util.Log.i("SYNC_QUEUE_DEBUG", "QUEUE_DELETE_SUCCESS queueId=$syncedId")
            }
        }
        val remainingCount = db.syncQueueDao().getPendingCountSync()
        if (remainingCount == 0) {
            db.syncOutboxDao().clearAll()
        }
        android.util.Log.i("SYNC_QUEUE_DEBUG", "QUEUE_PENDING_COUNT count=$remainingCount")
        return syncedIds.size
    }

    private fun sanitizePayloadForTable(table: String, jsonObj: JSONObject): JSONObject {
        val allowedColumns = when (table) {
            "invoice_items" -> setOf(
                "id", "user_id", "invoice_id", "item_id", "item_name",
                "hsn_code", "qty", "unit", "rate", "discount",
                "cgst_rate", "sgst_rate", "igst_rate", "amount",
                "deleted_at", "synced_at"
            )
            "invoices" -> setOf(
                "id", "user_id", "business_id", "invoice_no", "date",
                "party_id", "customer_name", "customer_phone", "customer_address",
                "customer_gstin", "type", "gst_mode", "subtotal", "discount",
                "taxable_amount", "cgst", "sgst", "igst", "total", "paid_amount",
                "payment_status", "payment_mode", "notes", "is_gst",
                "created_at", "updated_at", "deleted_at", "synced_at"
            )
            "parties" -> setOf(
                "id", "user_id", "business_id", "name", "phone", "email",
                "address", "gstin", "type", "tag", "opening_balance",
                "created_at", "updated_at", "deleted_at", "synced_at"
            )
            "items" -> setOf(
                "id", "user_id", "business_id", "name", "item_code", "unit",
                "hsn_code", "purchase_price", "sell_price", "gst_rate",
                "category", "stock_qty", "low_stock_threshold",
                "created_at", "updated_at", "deleted_at", "synced_at"
            )
            "payments" -> setOf(
                "id", "user_id", "business_id", "party_id", "date", "amount",
                "mode", "reference_no", "notes", "linked_invoice_id",
                "created_at", "updated_at", "deleted_at", "synced_at"
            )
            "expenses" -> setOf(
                "id", "user_id", "business_id", "date", "category", "amount",
                "description", "mode", "receipt_path",
                "created_at", "updated_at", "deleted_at", "synced_at"
            )
            "businesses" -> setOf(
                "id", "user_id", "name", "owner_name", "address", "phone",
                "email", "gstin", "pan", "logo_path", "gst_enabled",
                "financial_year_start", "upi_id", "bank_name", "account_number",
                "ifsc_code", "terms_and_conditions",
                "created_at", "updated_at", "deleted_at", "synced_at"
            )
            "accounts" -> setOf(
                "id", "user_id", "business_id", "name", "type",
                "opening_balance", "account_number", "ifsc_code",
                "created_at", "updated_at", "deleted_at", "synced_at"
            )
            "journal_entries" -> setOf(
                "id", "user_id", "business_id", "date", "voucher_no",
                "narration", "created_at", "updated_at", "deleted_at", "synced_at"
            )
            "journal_entry_lines" -> setOf(
                "id", "user_id", "journal_entry_id", "account_id", "account_name",
                "is_debit", "debit", "credit",
                "created_at", "updated_at", "deleted_at", "synced_at"
            )
            "khata_entries" -> setOf(
                "id", "user_id", "business_id", "party_id", "amount", "type",
                "date", "bill_number", "note",
                "created_at", "updated_at", "deleted_at", "synced_at"
            )
            else -> null
        }

        if (allowedColumns == null) return jsonObj

        val sanitized = JSONObject()
        val keys = jsonObj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (allowedColumns.contains(key)) {
                sanitized.put(key, jsonObj.get(key))
            }
        }
        return sanitized
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

            android.util.Log.i("CloudSyncManager", "SYNC_PULL_START table=$table")
            val result = apiClient.fetchDelta(table, session.accessToken, lastPulled, userId = session.userId)
            if (result.isSuccess) {
                val records = result.getOrThrow()
                successCount++
                tableStatuses[table] = "200 (${records.length()} records)"
                android.util.Log.i("CloudSyncManager", "SYNC_PULL_RESULT table=$table count=${records.length()}")
                if (records.length() > 0) {
                    db.withTransaction {
                        applyRemoteRecords(table, records, session)
                    }
                    totalPulled += records.length()
                }

                val newPulledAt = System.currentTimeMillis()
                db.syncMetadataDao().insertOrUpdate(
                    SyncMetadataEntity(
                        tableName = table,
                        lastPulledAt = newPulledAt,
                        lastSyncStatus = "SUCCESS"
                    )
                )
                android.util.Log.i("CloudSyncManager", "SYNC_METADATA table=$table lastPulledAt=$newPulledAt status=SUCCESS")
            } else {
                failureCount++
                val ex = result.exceptionOrNull()
                val errMsg = ex?.localizedMessage ?: "Unknown error"
                tableStatuses[table] = "ERROR: $errMsg"
                android.util.Log.e("CloudSyncManager", "SYNC_PULL_ERROR table=$table error=$errMsg")

                db.syncMetadataDao().insertOrUpdate(
                    SyncMetadataEntity(
                        tableName = table,
                        lastPulledAt = lastPulled,
                        lastSyncStatus = "FAILED",
                        errorMessage = errMsg
                    )
                )
                android.util.Log.i("CloudSyncManager", "SYNC_METADATA table=$table lastPulledAt=$lastPulled status=FAILED")
            }
        }

        return PullResultSummary(totalPulled, successCount, failureCount, tableStatuses)
    }

    private suspend fun ensureParentBusinessExists(bizId: String, session: UserSession) {
        if (bizId.isBlank()) return
        val existing = db.businessDao().getBusinessSync(bizId)
        if (existing == null) {
            val userPrefix = session.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "My"
            db.businessDao().insertOrUpdate(
                BusinessEntity(
                    id = bizId,
                    name = if (bizId == "default_business") "$userPrefix Business" else "Business ($bizId)",
                    email = session.email ?: "",
                    financialYearStart = "01-04"
                )
            )
            BusinessManager.getInstance(appContext).reloadFromRoomAndCloud(replaceLocal = false)
        }
    }

    private suspend fun applyRemoteRecords(table: String, records: JSONArray, session: UserSession) {
        when (table) {
            "businesses" -> {
                val list = mutableListOf<BusinessEntity>()
                val businessProfiles = mutableListOf<BusinessProfile>()
                for (i in 0 until records.length()) {
                    val obj = records.getJSONObject(i)
                    val recordUserId = obj.optString("user_id", "")
                    if (recordUserId.isNotBlank() && recordUserId != session.userId) {
                        android.util.Log.w("CloudSyncManager", "Skipping business ${obj.optString("id")} belonging to user $recordUserId (current user: ${session.userId})")
                        continue
                    }
                    if (recordUserId.isBlank()) {
                        val recordEmail = obj.optString("email", "")
                        if (session.email.isNullOrBlank() || !recordEmail.equals(session.email, ignoreCase = true)) {
                            android.util.Log.w("CloudSyncManager", "Skipping unassigned business ${obj.optString("id")} for user ${session.userId} (${session.email})")
                            continue
                        }
                    }

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
                        BusinessManager.getInstance(appContext).restoreBusinessesFromCloud(businessProfiles, replaceLocal = true)
                    }
                }
                android.util.Log.i("CloudSyncManager", "ROOM_INSERT table=businesses count=${list.size}")
            }
            "parties" -> {
                val list = mutableListOf<PartyEntity>()
                val deletedIds = mutableListOf<String>()
                for (i in 0 until records.length()) {
                    val obj = records.getJSONObject(i)
                    val recordUserId = obj.optString("user_id", "")
                    if (recordUserId.isNotBlank() && recordUserId != session.userId) continue

                    val rawBiz = obj.optString("business_id", obj.optString("businessId", "default_business")).ifBlank { "default_business" }
                    ensureParentBusinessExists(rawBiz, session)
                    val delAt = if (obj.isNull("deleted_at")) null else obj.optLong("deleted_at")
                    val entity = PartyEntity(
                        id = obj.getString("id"),
                        businessId = rawBiz,
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
                        deletedAt = delAt
                    )
                    if (delAt != null && delAt > 0L) {
                        deletedIds.add(entity.id)
                    } else {
                        list.add(entity)
                    }
                }
                if (list.isNotEmpty()) db.partyDao().insertAllParties(list)
                for (id in deletedIds) {
                    db.partyDao().deletePartyLegacy(id)
                    db.khataDao().deleteEntriesForPartyLegacy(id)
                }
                android.util.Log.i("CloudSyncManager", "ROOM_INSERT table=parties count=${list.size} deletedCount=${deletedIds.size}")
            }
            "items" -> {
                val list = mutableListOf<ItemEntity>()
                val deletedIds = mutableListOf<String>()
                for (i in 0 until records.length()) {
                    val obj = records.getJSONObject(i)
                    val recordUserId = obj.optString("user_id", "")
                    if (recordUserId.isNotBlank() && recordUserId != session.userId) continue

                    val rawBiz = obj.optString("business_id", obj.optString("businessId", "default_business")).ifBlank { "default_business" }
                    ensureParentBusinessExists(rawBiz, session)
                    val delAt = if (obj.isNull("deleted_at")) null else obj.optLong("deleted_at")
                    val entity = ItemEntity(
                        id = obj.getString("id"),
                        businessId = rawBiz,
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
                        deletedAt = delAt
                    )
                    if (delAt != null && delAt > 0L) {
                        deletedIds.add(entity.id)
                    } else {
                        list.add(entity)
                    }
                }
                if (list.isNotEmpty()) db.itemDao().insertAllItems(list)
                for (id in deletedIds) {
                    db.itemDao().deleteItemLegacy(id)
                }
                android.util.Log.i("CloudSyncManager", "ROOM_INSERT table=items count=${list.size} deletedCount=${deletedIds.size}")
            }
            "invoices" -> {
                val list = mutableListOf<InvoiceEntity>()
                val deletedIds = mutableListOf<String>()
                for (i in 0 until records.length()) {
                    val obj = records.getJSONObject(i)
                    val recordUserId = obj.optString("user_id", "")
                    if (recordUserId.isNotBlank() && recordUserId != session.userId) continue

                    val rawBiz = obj.optString("business_id", obj.optString("businessId", "default_business")).ifBlank { "default_business" }
                    ensureParentBusinessExists(rawBiz, session)
                    val rawPartyId = obj.optString("party_id", "").ifBlank { null }
                    val safePartyId = if (rawPartyId != null && db.partyDao().getPartyByIdSync(rawPartyId) != null) rawPartyId else null
                    val delAt = if (obj.isNull("deleted_at")) null else obj.optLong("deleted_at")
                    val entity = InvoiceEntity(
                        id = obj.getString("id"),
                        businessId = rawBiz,
                        invoiceNo = obj.getString("invoice_no"),
                        date = obj.optLong("date", System.currentTimeMillis()),
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
                        createdAt = obj.optLong("created_at", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updated_at", System.currentTimeMillis()),
                        deletedAt = delAt
                    )
                    if (delAt != null && delAt > 0L) {
                        deletedIds.add(entity.id)
                    } else {
                        list.add(entity)
                    }
                }
                if (list.isNotEmpty()) db.invoiceDao().insertAllInvoices(list)
                for (id in deletedIds) {
                    db.invoiceDao().deleteInvoiceLegacy(id)
                    db.invoiceDao().deleteItemsForInvoice(id)
                }
                android.util.Log.i("CloudSyncManager", "ROOM_INSERT table=invoices count=${list.size} deletedCount=${deletedIds.size}")
            }
            "invoice_items" -> {
                val list = mutableListOf<InvoiceItemEntity>()
                val deletedIds = mutableListOf<String>()
                for (i in 0 until records.length()) {
                    val obj = records.getJSONObject(i)
                    val recordUserId = obj.optString("user_id", "")
                    if (recordUserId.isNotBlank() && recordUserId != session.userId) continue

                    val invId = obj.getString("invoice_id")
                    val delAt = if (obj.isNull("deleted_at")) null else obj.optLong("deleted_at")
                    if (delAt != null && delAt > 0L) {
                        deletedIds.add(obj.getString("id"))
                    } else if (db.invoiceDao().getInvoiceByIdSync(invId) != null) {
                        val rawItemId = obj.optString("item_id", "").ifBlank { null }
                        val safeItemId = if (rawItemId != null && db.itemDao().getItemByIdSync(rawItemId) != null) rawItemId else null
                        list.add(
                            InvoiceItemEntity(
                                id = obj.getString("id"),
                                invoiceId = invId,
                                itemId = safeItemId,
                                itemName = obj.optString("item_name", obj.optString("description", "")),
                                hsnCode = obj.optString("hsn_code", ""),
                                qty = obj.optDouble("qty", obj.optDouble("quantity", 1.0)),
                                unit = obj.optString("unit", "Pcs"),
                                rate = obj.optLong("rate", 0L),
                                discount = obj.optLong("discount", 0L),
                                cgstRate = obj.optDouble("cgst_rate", 0.0),
                                sgstRate = obj.optDouble("sgst_rate", 0.0),
                                igstRate = obj.optDouble("igst_rate", 0.0),
                                amount = obj.optLong("amount", 0L),
                                deletedAt = delAt,
                                syncedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
                if (list.isNotEmpty()) db.invoiceDao().insertInvoiceItems(list)
                android.util.Log.i("CloudSyncManager", "ROOM_INSERT table=invoice_items count=${list.size} deletedCount=${deletedIds.size}")
            }
            "payments" -> {
                val list = mutableListOf<PaymentEntity>()
                val deletedIds = mutableListOf<String>()
                for (i in 0 until records.length()) {
                    val obj = records.getJSONObject(i)
                    val recordUserId = obj.optString("user_id", "")
                    if (recordUserId.isNotBlank() && recordUserId != session.userId) continue

                    val delAt = if (obj.isNull("deleted_at")) null else obj.optLong("deleted_at")
                    val entity = PaymentEntity(
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
                        deletedAt = delAt
                    )
                    if (delAt != null && delAt > 0L) {
                        deletedIds.add(entity.id)
                    } else {
                        list.add(entity)
                    }
                }
                if (list.isNotEmpty()) db.paymentDao().insertAllPayments(list)
                for (id in deletedIds) {
                    db.paymentDao().deletePaymentLegacy(id)
                }
                android.util.Log.i("CloudSyncManager", "ROOM_INSERT table=payments count=${list.size} deletedCount=${deletedIds.size}")
            }
            "expenses" -> {
                val list = mutableListOf<ExpenseEntity>()
                val deletedIds = mutableListOf<String>()
                for (i in 0 until records.length()) {
                    val obj = records.getJSONObject(i)
                    val recordUserId = obj.optString("user_id", "")
                    if (recordUserId.isNotBlank() && recordUserId != session.userId) continue

                    val delAt = if (obj.isNull("deleted_at")) null else obj.optLong("deleted_at")
                    val entity = ExpenseEntity(
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
                        deletedAt = delAt
                    )
                    if (delAt != null && delAt > 0L) {
                        deletedIds.add(entity.id)
                    } else {
                        list.add(entity)
                    }
                }
                if (list.isNotEmpty()) db.expenseDao().insertAllExpenses(list)
                for (id in deletedIds) {
                    db.expenseDao().deleteExpenseLegacy(id)
                }
                android.util.Log.i("CloudSyncManager", "ROOM_INSERT table=expenses count=${list.size} deletedCount=${deletedIds.size}")
            }
            "khata_entries" -> {
                val list = mutableListOf<KhataEntryEntity>()
                val deletedIds = mutableListOf<String>()
                for (i in 0 until records.length()) {
                    val obj = records.getJSONObject(i)
                    val recordUserId = obj.optString("user_id", "")
                    if (recordUserId.isNotBlank() && recordUserId != session.userId) continue

                    val delAt = if (obj.isNull("deleted_at")) null else obj.optLong("deleted_at")
                    val entity = KhataEntryEntity(
                        id = obj.getString("id"),
                        partyId = obj.getString("party_id"),
                        amount = obj.optLong("amount", 0L),
                        type = obj.getString("type"),
                        date = obj.optLong("date", System.currentTimeMillis()),
                        billNumber = obj.optString("bill_number", ""),
                        note = obj.optString("note", ""),
                        createdAt = obj.optLong("created_at", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updated_at", System.currentTimeMillis()),
                        deletedAt = delAt
                    )
                    if (delAt != null && delAt > 0L) {
                        deletedIds.add(entity.id)
                    } else {
                        list.add(entity)
                    }
                }
                if (list.isNotEmpty()) db.khataDao().insertAllEntries(list)
                for (id in deletedIds) {
                    db.khataDao().deleteEntryLegacy(id)
                }
                android.util.Log.i("CloudSyncManager", "ROOM_INSERT table=khata_entries count=${list.size} deletedCount=${deletedIds.size}")
            }
            else -> {
                android.util.Log.i("CloudSyncManager", "ROOM_INSERT table=$table count=${records.length()}")
            }
        }
    }

    suspend fun clearAllLocalUserData() = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            android.util.Log.i("USER_SWITCH", "Clearing all local user data from device")
            db.withTransaction {
                db.journalDao().deleteAllJournalLines()
                db.journalDao().deleteAllJournalEntries()
                db.invoiceDao().deleteAllInvoiceItems()
                db.invoiceDao().deleteAllInvoices()
                db.paymentDao().deleteAllPayments()
                db.expenseDao().deleteAllExpenses()
                db.khataDao().deleteAllEntries()
                db.itemDao().deleteAllItems()
                db.accountDao().deleteAllAccounts()
                db.partyDao().deleteAllParties()
                db.businessDao().deleteAllBusinesses()
                db.syncQueueDao().clearAll()
                db.syncOutboxDao().clearAll()
                db.syncMetadataDao().clearAll()
            }
            wipeAllRepositoryCaches()
            BusinessManager.getInstance(appContext).resetForNewUser(null)
            SettingsRepository.getInstance(appContext).resetProfile(null)
            InvoiceRepository.getInstance(appContext).clearLocalData()
            PartyRepository.getInstance(appContext).clearLocalData()
            ItemRepository.getInstance(appContext).clearLocalData()
            TransactionRepository.getInstance(appContext).clearLocalData()
            PurchaseRepository.getInstance(appContext).clearLocalData()
            authManager.setLastActiveUserId(null)
            authManager.setLastActiveUserEmail(null)
            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                lastSyncTimeMillis = 0L,
                statusMessage = "Local data cleared",
                lastError = null,
                totalSyncedRecords = 0
            )
        }
    }

    suspend fun handleUserSwitch(newSession: UserSession) = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            android.util.Log.i("USER_SWITCH", "User switch detected! Resetting device for new user: ${newSession.userId} (${newSession.email})")

            // 1. Wipe all local database tables
            db.withTransaction {
                db.journalDao().deleteAllJournalLines()
                db.journalDao().deleteAllJournalEntries()
                db.invoiceDao().deleteAllInvoiceItems()
                db.invoiceDao().deleteAllInvoices()
                db.paymentDao().deleteAllPayments()
                db.expenseDao().deleteAllExpenses()
                db.khataDao().deleteAllEntries()
                db.itemDao().deleteAllItems()
                db.accountDao().deleteAllAccounts()
                db.partyDao().deleteAllParties()
                db.businessDao().deleteAllBusinesses()
                db.syncQueueDao().clearAll()
                db.syncOutboxDao().clearAll()
                db.syncMetadataDao().clearAll()
            }

            // 2. Wipe repository caches and SharedPreferences
            wipeAllRepositoryCaches()

            // 3. Clear in-memory repositories
            InvoiceRepository.getInstance(appContext).clearLocalData()
            PartyRepository.getInstance(appContext).clearLocalData()
            ItemRepository.getInstance(appContext).clearLocalData()
            TransactionRepository.getInstance(appContext).clearLocalData()
            PurchaseRepository.getInstance(appContext).clearLocalData()

            // 4. Update tracked active user
            authManager.setLastActiveUserId(newSession.userId)
            authManager.setLastActiveUserEmail(newSession.email)

            // 5. Check if user had a registered business name from signup
            val pendingBizName = authManager.getPendingRegistrationBusinessName(newSession.email)

            // 6. Reset BusinessManager and SettingsRepository
            val initialProfile = if (!pendingBizName.isNullOrBlank()) {
                BusinessProfile(
                    shopName = pendingBizName,
                    ownerName = newSession.email?.substringBefore("@") ?: "Owner",
                    email = newSession.email ?: "",
                    phone = newSession.phone ?: "",
                    hasCompletedOnboarding = true
                )
            } else {
                null
            }

            BusinessManager.getInstance(appContext).resetForNewUser(initialProfile)
            SettingsRepository.getInstance(appContext).resetProfile(initialProfile)

            // 7. Perform a fresh initial pull from Supabase for this new user
            _syncState.value = _syncState.value.copy(
                isSyncing = true,
                statusMessage = "Loading user data from cloud...",
                lastError = null
            )

            val pullResult = executePull(newSession)

            // 8. If remote had NO businesses, and we had an initial profile from registration, add it and push it
            val localBusinesses = db.businessDao().getAllBusinessesSync()
            if (localBusinesses.isEmpty()) {
                if (initialProfile != null) {
                    BusinessManager.getInstance(appContext).addBusiness(initialProfile)
                    authManager.clearPendingRegistrationBusinessName(newSession.email)
                }
            } else {
                BusinessManager.getInstance(appContext).reloadFromRoom()
            }

            // 9. Reload repositories again after pulling remote records
            InvoiceRepository.getInstance(appContext).reloadFromDatabase()
            PartyRepository.getInstance(appContext).reloadFromDatabase()
            ItemRepository.getInstance(appContext).reloadFromDatabase()
            TransactionRepository.getInstance(appContext).reloadFromDatabase()
            PurchaseRepository.getInstance(appContext).reloadFromDatabase()

            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                lastSyncTimeMillis = System.currentTimeMillis(),
                statusMessage = if (pullResult.failureCount == 0) "User data loaded" else "Partially loaded (${pullResult.failureCount} errors)",
                totalSyncedRecords = pullResult.totalPulled
            )
        }
    }

    private fun wipeAllRepositoryCaches() {
        val knownPrefs = listOf(
            "hisab_pro_multi_business_v1",
            "hisab_pro_settings_v1",
            "hisab_pro_invoices_default_business",
            "hisab_pro_parties_default_business",
            "hisab_pro_items_default_business",
            "hisab_pro_transactions_default_business",
            "hisab_pro_purchases_default_business"
        )
        for (prefName in knownPrefs) {
            appContext.getSharedPreferences(prefName, Context.MODE_PRIVATE).edit().clear().apply()
        }

        try {
            val prefsDir = java.io.File(appContext.applicationInfo.dataDir, "shared_prefs")
            if (prefsDir.exists() && prefsDir.isDirectory) {
                prefsDir.listFiles()?.forEach { file ->
                    val name = file.name.removeSuffix(".xml")
                    if (name.startsWith("hisab_pro_invoices_") ||
                        name.startsWith("hisab_pro_parties_") ||
                        name.startsWith("hisab_pro_items_") ||
                        name.startsWith("hisab_pro_transactions_") ||
                        name.startsWith("hisab_pro_purchases_")
                    ) {
                        appContext.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().apply()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
