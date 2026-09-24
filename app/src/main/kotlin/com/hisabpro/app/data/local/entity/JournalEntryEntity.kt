package com.hisabpro.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "journal_entries",
    indices = [
        Index(value = ["business_id"]),
        Index(value = ["date"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["business_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class JournalEntryEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "business_id")
    val businessId: String = "default_business",
    val date: Long,
    @ColumnInfo(name = "voucher_no")
    val voucherNo: String = "",
    val narration: String = "",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "journal_entry_lines",
    indices = [
        Index(value = ["journal_entry_id"]),
        Index(value = ["account_id"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = JournalEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["journal_entry_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class JournalEntryLineEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "journal_entry_id")
    val journalEntryId: String,
    @ColumnInfo(name = "account_id")
    val accountId: String,
    @ColumnInfo(name = "account_name")
    val accountName: String = "",
    @ColumnInfo(name = "is_debit")
    val isDebit: Boolean = true,
    val debit: Long = 0L,  // In paise
    val credit: Long = 0L, // In paise
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
