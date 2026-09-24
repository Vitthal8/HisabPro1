package com.hisabpro.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "khata_entries",
    indices = [
        Index(value = ["party_id"]),
        Index(value = ["date"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = PartyEntity::class,
            parentColumns = ["id"],
            childColumns = ["party_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class KhataEntryEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "party_id")
    val partyId: String,
    val amount: Long, // In paise
    val type: String, // YOU_GAVE, YOU_GOT
    val date: Long,
    @ColumnInfo(name = "bill_number")
    val billNumber: String = "",
    val note: String = "",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
