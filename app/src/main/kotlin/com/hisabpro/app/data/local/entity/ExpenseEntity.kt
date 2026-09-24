package com.hisabpro.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    indices = [
        Index(value = ["business_id"]),
        Index(value = ["date"]),
        Index(value = ["category"])
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
data class ExpenseEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "business_id")
    val businessId: String = "default_business",
    val date: Long,
    val category: String,
    val amount: Long, // In paise
    val description: String = "",
    val mode: String = "Cash", // Cash, UPI, Bank, Cheque
    @ColumnInfo(name = "receipt_path")
    val receiptPath: String = "",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
