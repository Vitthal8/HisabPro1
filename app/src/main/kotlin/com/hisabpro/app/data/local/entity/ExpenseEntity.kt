package com.hisabpro.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    indices = [
        Index(value = ["businessId"]),
        Index(value = ["dateMillis"]),
        Index(value = ["category"])
    ]
)
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val businessId: String = "default_business",
    val dateMillis: Long,
    val category: String,
    val amount: Double,
    val description: String = "",
    val mode: String = "Cash", // Cash, UPI, Bank, Cheque
    val receiptPath: String = ""
)
