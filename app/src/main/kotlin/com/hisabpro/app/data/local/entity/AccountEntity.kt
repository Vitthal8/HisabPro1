package com.hisabpro.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "accounts",
    indices = [
        Index(value = ["business_id"]),
        Index(value = ["type"]),
        Index(value = ["business_id", "name"], unique = true)
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
data class AccountEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "business_id")
    val businessId: String = "default_business",
    val name: String,
    val type: String, // CASH, BANK, ASSET, LIABILITY, EQUITY
    @ColumnInfo(name = "opening_balance")
    val openingBalance: Long = 0L, // In paise
    @ColumnInfo(name = "account_number")
    val accountNumber: String = "",
    @ColumnInfo(name = "ifsc_code")
    val ifscCode: String = "",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
