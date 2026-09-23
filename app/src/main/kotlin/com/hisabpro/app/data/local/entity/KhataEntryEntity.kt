package com.hisabpro.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "khata_entries",
    indices = [
        Index(value = ["partyId"]),
        Index(value = ["dateMillis"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = PartyEntity::class,
            parentColumns = ["id"],
            childColumns = ["partyId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class KhataEntryEntity(
    @PrimaryKey val id: String,
    val partyId: String,
    val amount: Double,
    val type: String, // YOU_GAVE, YOU_GOT
    val dateMillis: Long,
    val billNumber: String = "",
    val note: String = ""
)
