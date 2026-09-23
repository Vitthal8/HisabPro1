package com.hisabpro.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "journal_entries",
    indices = [
        Index(value = ["businessId"]),
        Index(value = ["dateMillis"])
    ]
)
data class JournalEntryEntity(
    @PrimaryKey val id: String,
    val businessId: String = "default_business",
    val dateMillis: Long,
    val voucherNo: String = "",
    val narration: String = ""
)

@Entity(
    tableName = "journal_entry_lines",
    indices = [
        Index(value = ["journalEntryId"]),
        Index(value = ["accountId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = JournalEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["journalEntryId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class JournalEntryLineEntity(
    @PrimaryKey val id: String,
    val journalEntryId: String,
    val accountId: String,
    val accountName: String,
    val isDebit: Boolean,
    val amount: Double
)
