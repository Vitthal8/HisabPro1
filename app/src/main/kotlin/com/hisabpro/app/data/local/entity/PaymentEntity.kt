package com.hisabpro.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payments",
    indices = [
        Index(value = ["businessId"]),
        Index(value = ["partyId"]),
        Index(value = ["dateMillis"])
    ]
)
data class PaymentEntity(
    @PrimaryKey val id: String,
    val businessId: String = "default_business",
    val partyId: String? = null,
    val dateMillis: Long,
    val amount: Double,
    val mode: String = "Cash", // Cash, UPI, Bank, Cheque
    val referenceNo: String = "",
    val notes: String = "",
    val linkedInvoiceId: String? = null
)
