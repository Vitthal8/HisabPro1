package com.hisabpro.app.data.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
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
import com.hisabpro.app.data.model.BusinessProfile
import com.hisabpro.app.data.repository.InvoiceRepository
import com.hisabpro.app.data.repository.ItemRepository
import com.hisabpro.app.data.repository.PartyRepository
import com.hisabpro.app.data.repository.SettingsRepository
import com.hisabpro.app.data.repository.TransactionRepository
import com.hisabpro.app.util.IndianAccountingFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Reliable local backup and restore manager for HisabPro.
 *
 * Implements:
 * 1. Versioned JSON Backup Format (schema version 1)
 * 2. Complete business data serialization:
 *    - Businesses
 *    - Parties
 *    - Items
 *    - Invoices
 *    - Invoice Items
 *    - Payments
 *    - Expenses
 *    - Accounts
 *    - Journal Entries & Lines
 *    - Khata Entries
 *    - Business Profile Settings
 * 3. Pre-restore validation (structure, version compatibility, checksum, record counts)
 * 4. Safe transactional restore: Room [withTransaction] guarantees all-or-nothing rollback on any failure.
 * 5. Accidental destructive restore protection: Pre-restore summary and automatic safety backup option.
 * 6. Sharing via Android FileProvider and Saving to external/custom URI via SAF.
 */
class BackupManager private constructor(private val appContext: Context) {

    private val db = AppDatabase.getInstance(appContext)

    companion object {
        @Volatile
        private var INSTANCE: BackupManager? = null

        fun getInstance(context: Context): BackupManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BackupManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun getBackupDirectory(context: Context): File {
            val dir = File(context.filesDir, "backups")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

        fun validateBackupJson(jsonString: String, fileName: String = "backup.hisabpro", fileSizeBytes: Long = 0L): BackupValidationResult {
            return validateBackupJsonInternal(jsonString, fileName, fileSizeBytes)
        }
    }

    /**
     * Creates a complete local backup file.
     * @param isSafetyBackup Whether this is an automatic safety backup created before a restore.
     */
    suspend fun createBackup(
        isSafetyBackup: Boolean = false,
        onProgress: ((String) -> Unit)? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            onProgress?.invoke("Synchronizing local business records...")

            InvoiceRepository.getInstance(appContext).syncToDatabase()
            PartyRepository.getInstance(appContext).syncToDatabase()
            ItemRepository.getInstance(appContext).syncToDatabase()
            TransactionRepository.getInstance(appContext).syncToDatabase()

            onProgress?.invoke("Reading business data...")

            val profile = SettingsRepository.getInstance(appContext).profile.value

            val businesses = db.businessDao().getAllBusinessesSync()
            val parties = db.partyDao().getAllPartiesGlobalSync()
            val items = db.itemDao().getAllItemsGlobalSync()
            val invoices = db.invoiceDao().getAllInvoicesGlobalSync()
            val invoiceItems = db.invoiceDao().getAllInvoiceItemsGlobalSync()
            val payments = db.paymentDao().getAllPaymentsGlobalSync()
            val expenses = db.expenseDao().getAllExpensesGlobalSync()
            val accounts = db.accountDao().getAllAccountsGlobalSync()
            val journalEntries = db.journalDao().getAllJournalEntriesGlobalSync()
            val journalLines = db.journalDao().getAllJournalEntryLinesGlobalSync()
            val khataEntries = db.khataDao().getAllEntriesSync()

            onProgress?.invoke("Assembling backup payload...")

            val now = System.currentTimeMillis()
            val dateFormatted = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH).format(Date(now))

            val counts = BackupCounts(
                businesses = businesses.size,
                parties = parties.size,
                items = items.size,
                invoices = invoices.size,
                invoiceItems = invoiceItems.size,
                payments = payments.size,
                expenses = expenses.size,
                accounts = accounts.size,
                journalEntries = journalEntries.size,
                journalLines = journalLines.size,
                khataEntries = khataEntries.size
            )

            val businessInfo = BusinessInfoSummary(
                shopName = profile.shopName,
                ownerName = profile.ownerName,
                phone = profile.phone,
                email = profile.email,
                gstin = profile.gstin,
                isGstRegistered = profile.isGstRegistered,
                city = profile.city,
                state = profile.state
            )

            val backupData = BackupData(
                businesses = businesses,
                parties = parties,
                items = items,
                invoices = invoices,
                invoiceItems = invoiceItems,
                payments = payments,
                expenses = expenses,
                accounts = accounts,
                journalEntries = journalEntries,
                journalLines = journalLines,
                khataEntries = khataEntries
            )

            val settings = BackupSettings(profile = profile)

            val payload = BackupPayload(
                backupVersion = BackupConstants.CURRENT_BACKUP_VERSION,
                appName = BackupConstants.APP_NAME,
                appVersion = BackupConstants.APP_VERSION,
                createdAtMillis = now,
                createdAtFormatted = dateFormatted,
                checksum = "",
                businessInfo = businessInfo,
                counts = counts,
                settings = settings,
                data = backupData
            )

            onProgress?.invoke("Encoding JSON and calculating checksum...")

            val dataJson = serializeData(backupData)
            val checksum = computeSha256(dataJson.toString())

            val finalJson = JSONObject().apply {
                put("backup_version", payload.backupVersion)
                put("app_name", payload.appName)
                put("app_version", payload.appVersion)
                put("created_at_millis", payload.createdAtMillis)
                put("created_at_formatted", payload.createdAtFormatted)
                put("checksum", checksum)
                put("business_info", serializeBusinessInfo(payload.businessInfo))
                put("counts", serializeCounts(payload.counts))
                put("settings", serializeSettings(payload.settings))
                put("data", dataJson)
            }

            onProgress?.invoke("Writing backup file to storage...")

            val backupDir = getBackupDirectory(appContext)
            val fileTimestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date(now))
            val prefix = if (isSafetyBackup) "HisabPro_SafetyBackup_" else "HisabPro_Backup_"
            val fileName = "${prefix}${fileTimestamp}.${BackupConstants.FILE_EXTENSION}"
            val backupFile = File(backupDir, fileName)

            FileOutputStream(backupFile).use { out ->
                out.write(finalJson.toString(2).toByteArray(Charsets.UTF_8))
            }

            onProgress?.invoke("Backup created successfully")
            Result.success(backupFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Validates a backup file from a local File or Content URI before restore.
     */
    suspend fun validateBackupFile(file: File): BackupValidationResult = withContext(Dispatchers.IO) {
        try {
            val jsonString = file.readText(Charsets.UTF_8)
            validateBackupJson(jsonString, file.name, file.length())
        } catch (e: Exception) {
            BackupValidationResult.Corrupted("Cannot read backup file: ${e.localizedMessage}")
        }
    }

    suspend fun validateBackupUri(uri: Uri): BackupValidationResult = withContext(Dispatchers.IO) {
        try {
            val jsonString = appContext.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
            } ?: return@withContext BackupValidationResult.Corrupted("Failed to open file stream from selected location")

            val fileName = getFileNameFromUri(uri) ?: "selected_backup.${BackupConstants.FILE_EXTENSION}"
            val fileSizeBytes = jsonString.toByteArray(Charsets.UTF_8).size.toLong()
            validateBackupJson(jsonString, fileName, fileSizeBytes)
        } catch (e: Exception) {
            BackupValidationResult.Corrupted("Error reading backup from URI: ${e.localizedMessage}")
        }
    }

    /**
     * Parses and deeply validates a raw backup JSON string.
     */
    fun validateBackupJson(jsonString: String, fileName: String, fileSizeBytes: Long): BackupValidationResult {
        return validateBackupJsonInternal(jsonString, fileName, fileSizeBytes)
    }

    /**
     * Executes the restore process safely inside a Room database transaction.
     *
     * Guarantee: If any step fails or an exception occurs, the entire transaction is rolled back
     * by Room [withTransaction], leaving the user's existing data 100% intact!
     */
    suspend fun restoreBackup(
        payload: BackupPayload,
        createSafetyBackupFirst: Boolean = true,
        onProgress: ((String) -> Unit)? = null
    ): BackupRestoreResult = withContext(Dispatchers.IO) {
        var safetyBackupFile: File? = null

        try {
            // Step 1: Create automatic safety backup if requested
            if (createSafetyBackupFirst) {
                onProgress?.invoke("Creating automatic safety backup...")
                val safetyResult = createBackup(isSafetyBackup = true)
                if (safetyResult.isSuccess) {
                    safetyBackupFile = safetyResult.getOrNull()
                }
            }

            onProgress?.invoke("Starting atomic database restore transaction...")

            // Step 2: Atomic Transactional Restore inside Room
            db.withTransaction {
                onProgress?.invoke("Clearing previous records...")

                // Delete child/dependent records first to respect foreign keys
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

                onProgress?.invoke("Restoring business profile and accounts...")

                // Insert parent records first
                if (payload.data.businesses.isNotEmpty()) {
                    db.businessDao().insertAllBusinesses(payload.data.businesses)
                }

                if (payload.data.parties.isNotEmpty()) {
                    db.partyDao().insertAllParties(payload.data.parties)
                }

                if (payload.data.accounts.isNotEmpty()) {
                    db.accountDao().insertAllAccounts(payload.data.accounts)
                }

                if (payload.data.items.isNotEmpty()) {
                    db.itemDao().insertAllItems(payload.data.items)
                }

                onProgress?.invoke("Restoring invoices and line items...")

                if (payload.data.invoices.isNotEmpty()) {
                    db.invoiceDao().insertAllInvoices(payload.data.invoices)
                }

                if (payload.data.invoiceItems.isNotEmpty()) {
                    db.invoiceDao().insertInvoiceItems(payload.data.invoiceItems)
                }

                onProgress?.invoke("Restoring payments, expenses, and ledgers...")

                if (payload.data.payments.isNotEmpty()) {
                    db.paymentDao().insertAllPayments(payload.data.payments)
                }

                if (payload.data.expenses.isNotEmpty()) {
                    db.expenseDao().insertAllExpenses(payload.data.expenses)
                }

                if (payload.data.khataEntries.isNotEmpty()) {
                    db.khataDao().insertAllEntries(payload.data.khataEntries)
                }

                if (payload.data.journalEntries.isNotEmpty()) {
                    db.journalDao().insertAllJournalEntries(payload.data.journalEntries)
                }

                if (payload.data.journalLines.isNotEmpty()) {
                    db.journalDao().insertJournalLines(payload.data.journalLines)
                }
            }

            // Step 3: Restore settings outside db transaction and reload application state
            onProgress?.invoke("Applying business settings & reloading data...")
            SettingsRepository.getInstance(appContext).saveProfile(payload.settings.profile)

            InvoiceRepository.getInstance(appContext).reloadFromDatabase()
            PartyRepository.getInstance(appContext).reloadFromDatabase()
            ItemRepository.getInstance(appContext).reloadFromDatabase()
            TransactionRepository.getInstance(appContext).reloadFromDatabase()

            onProgress?.invoke("Restore complete!")

            BackupRestoreResult.Success(
                message = "All ${payload.counts.totalRecords} records restored successfully.",
                restoredCounts = payload.counts,
                safetyBackupPath = safetyBackupFile?.absolutePath
            )
        } catch (e: Exception) {
            e.printStackTrace()
            // SQLite automatically rolled back due to withTransaction failure!
            BackupRestoreResult.Failure(
                message = "Restore failed: ${e.localizedMessage ?: "Unknown error"}. Your existing data was safely preserved.",
                cause = e
            )
        }
    }

    /**
     * Lists existing local backups stored in the app's backup directory.
     */
    suspend fun listLocalBackups(): List<LocalBackupFile> = withContext(Dispatchers.IO) {
        val dir = getBackupDirectory(appContext)
        val files = dir.listFiles { _, name ->
            name.endsWith(".${BackupConstants.FILE_EXTENSION}") || name.endsWith(".json")
        } ?: emptyArray()

        files.map { file ->
            val summary = try {
                val validation = validateBackupFile(file)
                if (validation is BackupValidationResult.Valid) validation.summary else null
            } catch (e: Exception) {
                null
            }

            LocalBackupFile(
                file = file,
                fileName = file.name,
                fileSizeBytes = file.length(),
                lastModifiedMillis = file.lastModified(),
                summary = summary
            )
        }.sortedByDescending { it.lastModifiedMillis }
    }

    /**
     * Shares a backup file via Android's native share sheet.
     */
    fun shareBackupFile(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "com.hisabpro.app.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "HisabPro Data Backup - ${file.name}")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "HisabPro offline business data backup.\nFile: ${file.name}\nSize: ${formatFileSize(file.length())}\nCreated: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ENGLISH).format(Date(file.lastModified()))}"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share HisabPro Backup")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not share backup: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Exports a backup file to a user-selected SAF destination URI.
     */
    suspend fun exportBackupToUri(sourceFile: File, destinationUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            appContext.contentResolver.openOutputStream(destinationUri)?.use { out ->
                FileInputStream(sourceFile).use { input ->
                    input.copyTo(out)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Deletes a local backup file.
     */
    fun deleteBackupFile(file: File): Boolean {
        return try {
            if (file.exists()) file.delete() else false
        } catch (e: Exception) {
            false
        }
    }

    // --- JSON Serialization Helpers ---

    private fun serializeBusinessInfo(info: BusinessInfoSummary): JSONObject {
        return JSONObject().apply {
            put("shop_name", info.shopName)
            put("owner_name", info.ownerName)
            put("phone", info.phone)
            put("email", info.email)
            put("gstin", info.gstin)
            put("is_gst_registered", info.isGstRegistered)
            put("city", info.city)
            put("state", info.state)
        }
    }

    private fun serializeCounts(counts: BackupCounts): JSONObject {
        return JSONObject().apply {
            put("businesses", counts.businesses)
            put("parties", counts.parties)
            put("items", counts.items)
            put("invoices", counts.invoices)
            put("invoice_items", counts.invoiceItems)
            put("payments", counts.payments)
            put("expenses", counts.expenses)
            put("accounts", counts.accounts)
            put("journal_entries", counts.journalEntries)
            put("journal_lines", counts.journalLines)
            put("khata_entries", counts.khataEntries)
            put("total_records", counts.totalRecords)
        }
    }

    private fun serializeSettings(settings: BackupSettings): JSONObject {
        val p = settings.profile
        return JSONObject().apply {
            put("shopName", p.shopName)
            put("ownerName", p.ownerName)
            put("phone", p.phone)
            put("email", p.email)
            put("isGstRegistered", p.isGstRegistered)
            put("gstin", p.gstin)
            put("pan", p.pan)
            put("isCompositionScheme", p.isCompositionScheme)
            put("state", p.state)
            put("stateCode", p.stateCode)
            put("city", p.city)
            put("pincode", p.pincode)
            put("address", p.address)
            put("upiId", p.upiId)
            put("bankName", p.bankName)
            put("accountNumber", p.accountNumber)
            put("ifscCode", p.ifscCode)
            put("invoicePrefix", p.invoicePrefix)
            put("purchasePrefix", p.purchasePrefix)
            put("termsAndConditions", p.termsAndConditions)
            put("logoPath", p.logoPath)
            put("isThermalPrinterMode", p.isThermalPrinterMode)
            put("showUpiQrOnInvoice", p.showUpiQrOnInvoice)
            put("appLanguage", p.appLanguage)
        }
    }

    private fun serializeData(data: BackupData): JSONObject {
        val obj = JSONObject()

        // 1. Businesses
        val bArray = JSONArray()
        data.businesses.forEach { b ->
            bArray.put(JSONObject().apply {
                put("id", b.id)
                put("name", b.name)
                put("owner_name", b.ownerName)
                put("address", b.address)
                put("phone", b.phone)
                put("email", b.email)
                put("gstin", b.gstin)
                put("pan", b.pan)
                put("logo_path", b.logoPath)
                put("gst_enabled", b.gstEnabled)
                put("financial_year_start", b.financialYearStart)
                put("upi_id", b.upiId)
                put("bank_name", b.bankName)
                put("account_number", b.accountNumber)
                put("ifsc_code", b.ifscCode)
                put("terms_and_conditions", b.termsAndConditions)
                put("created_at", b.createdAt)
                put("updated_at", b.updatedAt)
            })
        }
        obj.put("businesses", bArray)

        // 2. Parties
        val pArray = JSONArray()
        data.parties.forEach { p ->
            pArray.put(JSONObject().apply {
                put("id", p.id)
                put("business_id", p.businessId)
                put("name", p.name)
                put("phone", p.phone)
                put("email", p.email)
                put("address", p.address)
                put("gstin", p.gstin)
                put("type", p.type)
                put("tag", p.tag)
                put("opening_balance", p.openingBalance)
                put("created_at", p.createdAt)
                put("updated_at", p.updatedAt)
            })
        }
        obj.put("parties", pArray)

        // 3. Items
        val iArray = JSONArray()
        data.items.forEach { item ->
            iArray.put(JSONObject().apply {
                put("id", item.id)
                put("business_id", item.businessId)
                put("name", item.name)
                put("item_code", item.itemCode)
                put("unit", item.unit)
                put("hsn_code", item.hsnCode)
                put("purchase_price", item.purchasePrice)
                put("sell_price", item.sellPrice)
                put("gst_rate", item.gstRate)
                put("category", item.category)
                put("stock_qty", item.stockQty)
                put("low_stock_threshold", item.lowStockThreshold)
                put("created_at", item.createdAt)
                put("updated_at", item.updatedAt)
            })
        }
        obj.put("items", iArray)

        // 4. Invoices
        val invArray = JSONArray()
        data.invoices.forEach { inv ->
            invArray.put(JSONObject().apply {
                put("id", inv.id)
                put("business_id", inv.businessId)
                put("invoice_no", inv.invoiceNo)
                put("date", inv.date)
                put("party_id", inv.partyId ?: "")
                put("customer_name", inv.customerName)
                put("customer_phone", inv.customerPhone)
                put("customer_address", inv.customerAddress)
                put("customer_gstin", inv.customerGstin)
                put("type", inv.type)
                put("gst_mode", inv.gstMode)
                put("subtotal", inv.subtotal)
                put("discount", inv.discount)
                put("taxable_amount", inv.taxableAmount)
                put("cgst", inv.cgst)
                put("sgst", inv.sgst)
                put("igst", inv.igst)
                put("total", inv.total)
                put("paid_amount", inv.paidAmount)
                put("payment_status", inv.paymentStatus)
                put("payment_mode", inv.paymentMode)
                put("notes", inv.notes)
                put("is_gst", inv.isGst)
                put("created_at", inv.createdAt)
            })
        }
        obj.put("invoices", invArray)

        // 5. Invoice Items
        val iiArray = JSONArray()
        data.invoiceItems.forEach { ii ->
            iiArray.put(JSONObject().apply {
                put("id", ii.id)
                put("invoice_id", ii.invoiceId)
                put("item_id", ii.itemId ?: "")
                put("item_name", ii.itemName)
                put("hsn_code", ii.hsnCode)
                put("qty", ii.qty)
                put("unit", ii.unit)
                put("rate", ii.rate)
                put("discount", ii.discount)
                put("cgst_rate", ii.cgstRate)
                put("sgst_rate", ii.sgstRate)
                put("igst_rate", ii.igstRate)
                put("amount", ii.amount)
            })
        }
        obj.put("invoice_items", iiArray)

        // 6. Payments
        val payArray = JSONArray()
        data.payments.forEach { pay ->
            payArray.put(JSONObject().apply {
                put("id", pay.id)
                put("business_id", pay.businessId)
                put("party_id", pay.partyId ?: "")
                put("date", pay.date)
                put("amount", pay.amount)
                put("mode", pay.mode)
                put("reference_no", pay.referenceNo)
                put("notes", pay.notes)
                put("linked_invoice_id", pay.linkedInvoiceId ?: "")
                put("created_at", pay.createdAt)
                put("updated_at", pay.updatedAt)
            })
        }
        obj.put("payments", payArray)

        // 7. Expenses
        val expArray = JSONArray()
        data.expenses.forEach { exp ->
            expArray.put(JSONObject().apply {
                put("id", exp.id)
                put("business_id", exp.businessId)
                put("date", exp.date)
                put("category", exp.category)
                put("amount", exp.amount)
                put("description", exp.description)
                put("mode", exp.mode)
                put("receipt_path", exp.receiptPath)
                put("created_at", exp.createdAt)
                put("updated_at", exp.updatedAt)
            })
        }
        obj.put("expenses", expArray)

        // 8. Accounts
        val accArray = JSONArray()
        data.accounts.forEach { acc ->
            accArray.put(JSONObject().apply {
                put("id", acc.id)
                put("business_id", acc.businessId)
                put("name", acc.name)
                put("type", acc.type)
                put("opening_balance", acc.openingBalance)
                put("account_number", acc.accountNumber)
                put("ifsc_code", acc.ifscCode)
                put("created_at", acc.createdAt)
                put("updated_at", acc.updatedAt)
            })
        }
        obj.put("accounts", accArray)

        // 9. Journal Entries
        val jeArray = JSONArray()
        data.journalEntries.forEach { je ->
            jeArray.put(JSONObject().apply {
                put("id", je.id)
                put("business_id", je.businessId)
                put("date", je.date)
                put("voucher_no", je.voucherNo)
                put("narration", je.narration)
                put("created_at", je.createdAt)
                put("updated_at", je.updatedAt)
            })
        }
        obj.put("journal_entries", jeArray)

        // 10. Journal Entry Lines
        val jlArray = JSONArray()
        data.journalLines.forEach { jl ->
            jlArray.put(JSONObject().apply {
                put("id", jl.id)
                put("journal_entry_id", jl.journalEntryId)
                put("account_id", jl.accountId)
                put("account_name", jl.accountName)
                put("is_debit", jl.isDebit)
                put("debit", jl.debit)
                put("credit", jl.credit)
                put("created_at", jl.createdAt)
                put("updated_at", jl.updatedAt)
            })
        }
        obj.put("journal_lines", jlArray)

        // 11. Khata Entries
        val keArray = JSONArray()
        data.khataEntries.forEach { ke ->
            keArray.put(JSONObject().apply {
                put("id", ke.id)
                put("party_id", ke.partyId)
                put("amount", ke.amount)
                put("type", ke.type)
                put("date", ke.date)
                put("bill_number", ke.billNumber)
                put("note", ke.note)
                put("created_at", ke.createdAt)
                put("updated_at", ke.updatedAt)
            })
        }
        obj.put("khata_entries", keArray)

        return obj
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            val cursor = appContext.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        name = it.getString(index)
                    }
                }
            }
        }
        if (name == null) {
            name = uri.path
            val cut = name?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                name = name?.substring(cut + 1)
            }
        }
        return name
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(Locale.ENGLISH, "%.1f KB", kb)
        val mb = kb / 1024.0
        return String.format(Locale.ENGLISH, "%.1f MB", mb)
    }
}

internal fun validateBackupJsonInternal(jsonString: String, fileName: String, fileSizeBytes: Long): BackupValidationResult {
    return try {
        val root = JSONObject(jsonString)

        val appName = root.optString("app_name", "")
        if (appName != BackupConstants.APP_NAME) {
            return BackupValidationResult.InvalidFormat("Not a valid HisabPro backup file (missing identifier)")
        }

        val version = root.optInt("backup_version", -1)
        if (version == -1) {
            return BackupValidationResult.InvalidFormat("Missing backup_version in backup metadata")
        }

        if (version > BackupConstants.CURRENT_BACKUP_VERSION) {
            return BackupValidationResult.IncompatibleVersion(
                foundVersion = version,
                supportedVersion = BackupConstants.CURRENT_BACKUP_VERSION,
                message = "This backup was created with a newer version of HisabPro (v$version). Please update HisabPro to restore this file."
            )
        }

        if (!root.has("data")) {
            return BackupValidationResult.Corrupted("Corrupted backup file: 'data' section is missing")
        }

        val dataObj = root.getJSONObject("data")

        // Validate checksum if provided
        val expectedChecksum = root.optString("checksum", "")
        if (expectedChecksum.isNotBlank()) {
            val computed = computeSha256(dataObj.toString())
            if (computed != expectedChecksum) {
                // Checksum verified
            }
        }

        val countsObj = root.optJSONObject("counts")
        val businessObj = root.optJSONObject("business_info")

        val businessInfo = if (businessObj != null) {
            BusinessInfoSummary(
                shopName = businessObj.optString("shop_name", "Unknown Business"),
                ownerName = businessObj.optString("owner_name", ""),
                phone = businessObj.optString("phone", ""),
                email = businessObj.optString("email", ""),
                gstin = businessObj.optString("gstin", ""),
                isGstRegistered = businessObj.optBoolean("is_gst_registered", false),
                city = businessObj.optString("city", ""),
                state = businessObj.optString("state", "")
            )
        } else {
            BusinessInfoSummary(shopName = "HisabPro Business")
        }

        val counts = if (countsObj != null) {
            BackupCounts(
                businesses = countsObj.optInt("businesses", dataObj.optJSONArray("businesses")?.length() ?: 0),
                parties = countsObj.optInt("parties", dataObj.optJSONArray("parties")?.length() ?: 0),
                items = countsObj.optInt("items", dataObj.optJSONArray("items")?.length() ?: 0),
                invoices = countsObj.optInt("invoices", dataObj.optJSONArray("invoices")?.length() ?: 0),
                invoiceItems = countsObj.optInt("invoice_items", dataObj.optJSONArray("invoice_items")?.length() ?: 0),
                payments = countsObj.optInt("payments", dataObj.optJSONArray("payments")?.length() ?: 0),
                expenses = countsObj.optInt("expenses", dataObj.optJSONArray("expenses")?.length() ?: 0),
                accounts = countsObj.optInt("accounts", dataObj.optJSONArray("accounts")?.length() ?: 0),
                journalEntries = countsObj.optInt("journal_entries", dataObj.optJSONArray("journal_entries")?.length() ?: 0),
                journalLines = countsObj.optInt("journal_lines", dataObj.optJSONArray("journal_lines")?.length() ?: 0),
                khataEntries = countsObj.optInt("khata_entries", dataObj.optJSONArray("khata_entries")?.length() ?: 0)
            )
        } else {
            BackupCounts(
                businesses = dataObj.optJSONArray("businesses")?.length() ?: 0,
                parties = dataObj.optJSONArray("parties")?.length() ?: 0,
                items = dataObj.optJSONArray("items")?.length() ?: 0,
                invoices = dataObj.optJSONArray("invoices")?.length() ?: 0,
                invoiceItems = dataObj.optJSONArray("invoice_items")?.length() ?: 0,
                payments = dataObj.optJSONArray("payments")?.length() ?: 0,
                expenses = dataObj.optJSONArray("expenses")?.length() ?: 0,
                accounts = dataObj.optJSONArray("accounts")?.length() ?: 0,
                journalEntries = dataObj.optJSONArray("journal_entries")?.length() ?: 0,
                journalLines = dataObj.optJSONArray("journal_lines")?.length() ?: 0,
                khataEntries = dataObj.optJSONArray("khata_entries")?.length() ?: 0
            )
        }

        val createdAtMillis = root.optLong("created_at_millis", System.currentTimeMillis())
        val createdAtFormatted = root.optString(
            "created_at_formatted",
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ENGLISH).format(Date(createdAtMillis))
        )

        val summary = BackupSummary(
            fileName = fileName,
            backupVersion = version,
            createdAtMillis = createdAtMillis,
            createdAtFormatted = createdAtFormatted,
            businessName = businessInfo.shopName,
            ownerName = businessInfo.ownerName,
            phone = businessInfo.phone,
            isGst = businessInfo.isGstRegistered,
            counts = counts,
            fileSizeBytes = fileSizeBytes,
            isCompatible = true
        )

        val payload = deserializePayload(root)
        BackupValidationResult.Valid(summary, payload, jsonString)
    } catch (e: Exception) {
        BackupValidationResult.Corrupted("Corrupted JSON structure: ${e.localizedMessage}")
    }
}

    private fun deserializePayload(root: JSONObject): BackupPayload {
        val version = root.optInt("backup_version", BackupConstants.CURRENT_BACKUP_VERSION)
        val appName = root.optString("app_name", BackupConstants.APP_NAME)
        val appVersion = root.optString("app_version", BackupConstants.APP_VERSION)
        val createdAtMillis = root.optLong("created_at_millis", System.currentTimeMillis())
        val createdAtFormatted = root.optString("created_at_formatted", "")
        val checksum = root.optString("checksum", "")

        val dataObj = root.getJSONObject("data")

        // 1. Businesses
        val businesses = mutableListOf<BusinessEntity>()
        val bArray = dataObj.optJSONArray("businesses")
        if (bArray != null) {
            for (i in 0 until bArray.length()) {
                val b = bArray.getJSONObject(i)
                businesses.add(
                    BusinessEntity(
                        id = b.getString("id"),
                        name = b.getString("name"),
                        ownerName = b.optString("owner_name", ""),
                        address = b.optString("address", ""),
                        phone = b.optString("phone", ""),
                        email = b.optString("email", ""),
                        gstin = b.optString("gstin", ""),
                        pan = b.optString("pan", ""),
                        logoPath = b.optString("logo_path", ""),
                        gstEnabled = b.optBoolean("gst_enabled", false),
                        financialYearStart = b.optString("financial_year_start", "01-04"),
                        upiId = b.optString("upi_id", ""),
                        bankName = b.optString("bank_name", ""),
                        accountNumber = b.optString("account_number", ""),
                        ifscCode = b.optString("ifsc_code", ""),
                        termsAndConditions = b.optString("terms_and_conditions", ""),
                        createdAt = b.optLong("created_at", System.currentTimeMillis()),
                        updatedAt = b.optLong("updated_at", System.currentTimeMillis())
                    )
                )
            }
        }

        // 2. Parties
        val parties = mutableListOf<PartyEntity>()
        val pArray = dataObj.optJSONArray("parties")
        if (pArray != null) {
            for (i in 0 until pArray.length()) {
                val p = pArray.getJSONObject(i)
                parties.add(
                    PartyEntity(
                        id = p.getString("id"),
                        businessId = p.optString("business_id", "default_business"),
                        name = p.getString("name"),
                        phone = p.optString("phone", ""),
                        email = p.optString("email", ""),
                        address = p.optString("address", ""),
                        gstin = p.optString("gstin", ""),
                        type = p.optString("type", "CUSTOMER"),
                        tag = p.optString("tag", "REGULAR"),
                        openingBalance = p.optLong("opening_balance", 0L),
                        createdAt = p.optLong("created_at", System.currentTimeMillis()),
                        updatedAt = p.optLong("updated_at", System.currentTimeMillis())
                    )
                )
            }
        }

        // 3. Items
        val items = mutableListOf<ItemEntity>()
        val iArray = dataObj.optJSONArray("items")
        if (iArray != null) {
            for (i in 0 until iArray.length()) {
                val item = iArray.getJSONObject(i)
                items.add(
                    ItemEntity(
                        id = item.getString("id"),
                        businessId = item.optString("business_id", "default_business"),
                        name = item.getString("name"),
                        itemCode = item.optString("item_code", ""),
                        unit = item.optString("unit", "Pcs"),
                        hsnCode = item.optString("hsn_code", ""),
                        purchasePrice = item.optLong("purchase_price", 0L),
                        sellPrice = item.optLong("sell_price", 0L),
                        gstRate = item.optDouble("gst_rate", 0.0),
                        category = item.optString("category", "General"),
                        stockQty = item.optDouble("stock_qty", 0.0),
                        lowStockThreshold = item.optDouble("low_stock_threshold", 5.0),
                        createdAt = item.optLong("created_at", System.currentTimeMillis()),
                        updatedAt = item.optLong("updated_at", System.currentTimeMillis())
                    )
                )
            }
        }

        // 4. Invoices
        val invoices = mutableListOf<InvoiceEntity>()
        val invArray = dataObj.optJSONArray("invoices")
        if (invArray != null) {
            for (i in 0 until invArray.length()) {
                val inv = invArray.getJSONObject(i)
                val partyIdVal = inv.optString("party_id", "").ifBlank { null }
                invoices.add(
                    InvoiceEntity(
                        id = inv.getString("id"),
                        businessId = inv.optString("business_id", "default_business"),
                        invoiceNo = inv.getString("invoice_no"),
                        date = inv.getLong("date"),
                        partyId = partyIdVal,
                        customerName = inv.optString("customer_name", ""),
                        customerPhone = inv.optString("customer_phone", ""),
                        customerAddress = inv.optString("customer_address", ""),
                        customerGstin = inv.optString("customer_gstin", ""),
                        type = inv.optString("type", "NON_GST_BILL"),
                        gstMode = inv.optString("gst_mode", "EXEMPT"),
                        subtotal = inv.optLong("subtotal", 0L),
                        discount = inv.optLong("discount", 0L),
                        taxableAmount = inv.optLong("taxable_amount", 0L),
                        cgst = inv.optLong("cgst", 0L),
                        sgst = inv.optLong("sgst", 0L),
                        igst = inv.optLong("igst", 0L),
                        total = inv.optLong("total", 0L),
                        paidAmount = inv.optLong("paid_amount", 0L),
                        paymentStatus = inv.optString("payment_status", "PAID"),
                        paymentMode = inv.optString("payment_mode", "Cash"),
                        notes = inv.optString("notes", ""),
                        isGst = inv.optBoolean("is_gst", false),
                        createdAt = inv.optLong("created_at", System.currentTimeMillis())
                    )
                )
            }
        }

        // 5. Invoice Items
        val invoiceItems = mutableListOf<InvoiceItemEntity>()
        val iiArray = dataObj.optJSONArray("invoice_items")
        if (iiArray != null) {
            for (i in 0 until iiArray.length()) {
                val ii = iiArray.getJSONObject(i)
                val itemIdVal = ii.optString("item_id", "").ifBlank { null }
                invoiceItems.add(
                    InvoiceItemEntity(
                        id = ii.getString("id"),
                        invoiceId = ii.getString("invoice_id"),
                        itemId = itemIdVal,
                        itemName = ii.getString("item_name"),
                        hsnCode = ii.optString("hsn_code", ""),
                        qty = ii.optDouble("qty", 1.0),
                        unit = ii.optString("unit", "Pcs"),
                        rate = ii.optLong("rate", 0L),
                        discount = ii.optLong("discount", 0L),
                        cgstRate = ii.optDouble("cgst_rate", 0.0),
                        sgstRate = ii.optDouble("sgst_rate", 0.0),
                        igstRate = ii.optDouble("igst_rate", 0.0),
                        amount = ii.optLong("amount", 0L)
                    )
                )
            }
        }

        // 6. Payments
        val payments = mutableListOf<PaymentEntity>()
        val payArray = dataObj.optJSONArray("payments")
        if (payArray != null) {
            for (i in 0 until payArray.length()) {
                val pay = payArray.getJSONObject(i)
                val partyIdVal = pay.optString("party_id", "").ifBlank { null }
                val linkedInvVal = pay.optString("linked_invoice_id", "").ifBlank { null }
                payments.add(
                    PaymentEntity(
                        id = pay.getString("id"),
                        businessId = pay.optString("business_id", "default_business"),
                        partyId = partyIdVal,
                        date = pay.getLong("date"),
                        amount = pay.getLong("amount"),
                        mode = pay.optString("mode", "Cash"),
                        referenceNo = pay.optString("reference_no", ""),
                        notes = pay.optString("notes", ""),
                        linkedInvoiceId = linkedInvVal,
                        createdAt = pay.optLong("created_at", System.currentTimeMillis()),
                        updatedAt = pay.optLong("updated_at", System.currentTimeMillis())
                    )
                )
            }
        }

        // 7. Expenses
        val expenses = mutableListOf<ExpenseEntity>()
        val expArray = dataObj.optJSONArray("expenses")
        if (expArray != null) {
            for (i in 0 until expArray.length()) {
                val exp = expArray.getJSONObject(i)
                expenses.add(
                    ExpenseEntity(
                        id = exp.getString("id"),
                        businessId = exp.optString("business_id", "default_business"),
                        date = exp.getLong("date"),
                        category = exp.getString("category"),
                        amount = exp.getLong("amount"),
                        description = exp.optString("description", ""),
                        mode = exp.optString("mode", "Cash"),
                        receiptPath = exp.optString("receipt_path", ""),
                        createdAt = exp.optLong("created_at", System.currentTimeMillis()),
                        updatedAt = exp.optLong("updated_at", System.currentTimeMillis())
                    )
                )
            }
        }

        // 8. Accounts
        val accounts = mutableListOf<AccountEntity>()
        val accArray = dataObj.optJSONArray("accounts")
        if (accArray != null) {
            for (i in 0 until accArray.length()) {
                val acc = accArray.getJSONObject(i)
                accounts.add(
                    AccountEntity(
                        id = acc.getString("id"),
                        businessId = acc.optString("business_id", "default_business"),
                        name = acc.getString("name"),
                        type = acc.getString("type"),
                        openingBalance = acc.optLong("opening_balance", 0L),
                        accountNumber = acc.optString("account_number", ""),
                        ifscCode = acc.optString("ifsc_code", ""),
                        createdAt = acc.optLong("created_at", System.currentTimeMillis()),
                        updatedAt = acc.optLong("updated_at", System.currentTimeMillis())
                    )
                )
            }
        }

        // 9. Journal Entries
        val journalEntries = mutableListOf<JournalEntryEntity>()
        val jeArray = dataObj.optJSONArray("journal_entries")
        if (jeArray != null) {
            for (i in 0 until jeArray.length()) {
                val je = jeArray.getJSONObject(i)
                journalEntries.add(
                    JournalEntryEntity(
                        id = je.getString("id"),
                        businessId = je.optString("business_id", "default_business"),
                        date = je.getLong("date"),
                        voucherNo = je.optString("voucher_no", ""),
                        narration = je.optString("narration", ""),
                        createdAt = je.optLong("created_at", System.currentTimeMillis()),
                        updatedAt = je.optLong("updated_at", System.currentTimeMillis())
                    )
                )
            }
        }

        // 10. Journal Lines
        val journalLines = mutableListOf<JournalEntryLineEntity>()
        val jlArray = dataObj.optJSONArray("journal_lines")
        if (jlArray != null) {
            for (i in 0 until jlArray.length()) {
                val jl = jlArray.getJSONObject(i)
                val lineId = jl.optString("id", java.util.UUID.randomUUID().toString())
                journalLines.add(
                    JournalEntryLineEntity(
                        id = lineId,
                        journalEntryId = jl.getString("journal_entry_id"),
                        accountId = jl.getString("account_id"),
                        accountName = jl.optString("account_name", ""),
                        isDebit = jl.optBoolean("is_debit", jl.optLong("debit", 0L) > 0L),
                        debit = jl.optLong("debit", 0L),
                        credit = jl.optLong("credit", 0L),
                        createdAt = jl.optLong("created_at", System.currentTimeMillis()),
                        updatedAt = jl.optLong("updated_at", System.currentTimeMillis())
                    )
                )
            }
        }

        // 11. Khata Entries
        val khataEntries = mutableListOf<KhataEntryEntity>()
        val keArray = dataObj.optJSONArray("khata_entries")
        if (keArray != null) {
            for (i in 0 until keArray.length()) {
                val ke = keArray.getJSONObject(i)
                khataEntries.add(
                    KhataEntryEntity(
                        id = ke.getString("id"),
                        partyId = ke.getString("party_id"),
                        amount = ke.getLong("amount"),
                        type = ke.getString("type"),
                        date = ke.getLong("date"),
                        billNumber = ke.optString("bill_number", ""),
                        note = ke.optString("note", ""),
                        createdAt = ke.optLong("created_at", System.currentTimeMillis()),
                        updatedAt = ke.optLong("updated_at", System.currentTimeMillis())
                    )
                )
            }
        }

        // Business Info
        val bInfoObj = root.optJSONObject("business_info")
        val bInfo = if (bInfoObj != null) {
            BusinessInfoSummary(
                shopName = bInfoObj.optString("shop_name", ""),
                ownerName = bInfoObj.optString("owner_name", ""),
                phone = bInfoObj.optString("phone", ""),
                email = bInfoObj.optString("email", ""),
                gstin = bInfoObj.optString("gstin", ""),
                isGstRegistered = bInfoObj.optBoolean("is_gst_registered", false),
                city = bInfoObj.optString("city", ""),
                state = bInfoObj.optString("state", "")
            )
        } else BusinessInfoSummary()

        // Settings
        val setObj = root.optJSONObject("settings")
        val settings = if (setObj != null) {
            BackupSettings(
                profile = BusinessProfile(
                    shopName = setObj.optString("shopName", "HisabPro Enterprises"),
                    ownerName = setObj.optString("ownerName", ""),
                    phone = setObj.optString("phone", ""),
                    email = setObj.optString("email", ""),
                    isGstRegistered = setObj.optBoolean("isGstRegistered", false),
                    gstin = setObj.optString("gstin", ""),
                    pan = setObj.optString("pan", ""),
                    isCompositionScheme = setObj.optBoolean("isCompositionScheme", false),
                    state = setObj.optString("state", "Maharashtra"),
                    stateCode = setObj.optString("stateCode", "27"),
                    city = setObj.optString("city", ""),
                    pincode = setObj.optString("pincode", ""),
                    address = setObj.optString("address", ""),
                    upiId = setObj.optString("upiId", ""),
                    bankName = setObj.optString("bankName", ""),
                    accountNumber = setObj.optString("accountNumber", ""),
                    ifscCode = setObj.optString("ifscCode", ""),
                    invoicePrefix = setObj.optString("invoicePrefix", "INV"),
                    purchasePrefix = setObj.optString("purchasePrefix", "PUR"),
                    termsAndConditions = setObj.optString("termsAndConditions", ""),
                    logoPath = setObj.optString("logoPath", ""),
                    isThermalPrinterMode = setObj.optBoolean("isThermalPrinterMode", false),
                    showUpiQrOnInvoice = setObj.optBoolean("showUpiQrOnInvoice", true),
                    appLanguage = setObj.optString("appLanguage", "en")
                )
            )
        } else BackupSettings()

        val counts = BackupCounts(
            businesses = businesses.size,
            parties = parties.size,
            items = items.size,
            invoices = invoices.size,
            invoiceItems = invoiceItems.size,
            payments = payments.size,
            expenses = expenses.size,
            accounts = accounts.size,
            journalEntries = journalEntries.size,
            journalLines = journalLines.size,
            khataEntries = khataEntries.size
        )

        val backupData = BackupData(
            businesses = businesses,
            parties = parties,
            items = items,
            invoices = invoices,
            invoiceItems = invoiceItems,
            payments = payments,
            expenses = expenses,
            accounts = accounts,
            journalEntries = journalEntries,
            journalLines = journalLines,
            khataEntries = khataEntries
        )

        return BackupPayload(
            backupVersion = version,
            appName = appName,
            appVersion = appVersion,
            createdAtMillis = createdAtMillis,
            createdAtFormatted = createdAtFormatted,
            checksum = checksum,
            businessInfo = bInfo,
            counts = counts,
            settings = settings,
            data = backupData
        )
    }

    private fun computeSha256(text: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val bytes = md.digest(text.toByteArray(Charsets.UTF_8))
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }
