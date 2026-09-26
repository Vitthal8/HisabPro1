package com.hisabpro.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "parties",
    indices = [
        Index(value = ["business_id"]),
        Index(value = ["phone"])
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
data class PartyEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "business_id")
    val businessId: String = "default_business",
    val name: String,
    val phone: String,
    val email: String = "",
    val address: String = "",
    val gstin: String = "",
    val type: String = "CUSTOMER", // CUSTOMER, SUPPLIER
    val tag: String = "REGULAR",   // REGULAR, OCCASIONAL, BLOCKED
    @ColumnInfo(name = "opening_balance")
    val openingBalance: Long = 0L, // Precise monetary representation in paise
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long? = null,
    @ColumnInfo(name = "synced_at")
    val syncedAt: Long? = null
)
