package com.hisabpro.app.data.backup

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
import java.io.File

/**
 * Backup schema and metadata models for HisabPro.
 */
object BackupConstants {
    const val CURRENT_BACKUP_VERSION = 1
    const val APP_NAME = "HisabPro"
    const val APP_VERSION = "1.0.0"
    const val FILE_EXTENSION = "hisabpro"
    const val MIME_TYPE = "application/json"
}

data class BusinessInfoSummary(
    val shopName: String = "",
    val ownerName: String = "",
    val phone: String = "",
    val email: String = "",
    val gstin: String = "",
    val isGstRegistered: Boolean = false,
    val city: String = "",
    val state: String = ""
)

data class BackupCounts(
    val businesses: Int = 0,
    val parties: Int = 0,
    val items: Int = 0,
    val invoices: Int = 0,
    val invoiceItems: Int = 0,
    val payments: Int = 0,
    val expenses: Int = 0,
    val accounts: Int = 0,
    val journalEntries: Int = 0,
    val journalLines: Int = 0,
    val khataEntries: Int = 0
) {
    val totalRecords: Int
        get() = businesses + parties + items + invoices + invoiceItems + payments + expenses + accounts + journalEntries + journalLines + khataEntries
}

data class BackupSettings(
    val profile: BusinessProfile = BusinessProfile()
)

data class BackupData(
    val businesses: List<BusinessEntity> = emptyList(),
    val parties: List<PartyEntity> = emptyList(),
    val items: List<ItemEntity> = emptyList(),
    val invoices: List<InvoiceEntity> = emptyList(),
    val invoiceItems: List<InvoiceItemEntity> = emptyList(),
    val payments: List<PaymentEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val journalEntries: List<JournalEntryEntity> = emptyList(),
    val journalLines: List<JournalEntryLineEntity> = emptyList(),
    val khataEntries: List<KhataEntryEntity> = emptyList()
)

data class BackupPayload(
    val backupVersion: Int = BackupConstants.CURRENT_BACKUP_VERSION,
    val appName: String = BackupConstants.APP_NAME,
    val appVersion: String = BackupConstants.APP_VERSION,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val createdAtFormatted: String = "",
    val checksum: String = "",
    val businessInfo: BusinessInfoSummary = BusinessInfoSummary(),
    val counts: BackupCounts = BackupCounts(),
    val settings: BackupSettings = BackupSettings(),
    val data: BackupData = BackupData()
)

data class BackupSummary(
    val fileName: String = "",
    val backupVersion: Int = 1,
    val createdAtMillis: Long = 0L,
    val createdAtFormatted: String = "",
    val businessName: String = "",
    val ownerName: String = "",
    val phone: String = "",
    val isGst: Boolean = false,
    val counts: BackupCounts = BackupCounts(),
    val fileSizeBytes: Long = 0L,
    val isCompatible: Boolean = true
)

sealed class BackupValidationResult {
    data class Valid(
        val summary: BackupSummary,
        val payload: BackupPayload,
        val rawJson: String
    ) : BackupValidationResult()

    data class IncompatibleVersion(
        val foundVersion: Int,
        val supportedVersion: Int,
        val message: String
    ) : BackupValidationResult()

    data class Corrupted(val reason: String) : BackupValidationResult()
    data class InvalidFormat(val reason: String) : BackupValidationResult()
}

sealed class BackupRestoreResult {
    data class Success(
        val message: String,
        val restoredCounts: BackupCounts,
        val safetyBackupPath: String? = null
    ) : BackupRestoreResult()

    data class Failure(
        val message: String,
        val cause: Throwable? = null
    ) : BackupRestoreResult()
}

data class LocalBackupFile(
    val file: File,
    val fileName: String,
    val fileSizeBytes: Long,
    val lastModifiedMillis: Long,
    val summary: BackupSummary? = null
)
