package com.hisabpro.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "invoices",
    indices = [
        Index(value = ["businessId"]),
        Index(value = ["invoiceNo"]),
        Index(value = ["partyId"]),
        Index(value = ["dateMillis"])
    ]
)
data class InvoiceEntity(
    @PrimaryKey val id: String,
    val businessId: String = "default_business",
    val invoiceNo: String,
    val dateMillis: Long,
    val partyId: String? = null,
    val customerName: String = "",
    val customerPhone: String = "",
    val customerAddress: String = "",
    val customerGstin: String = "",
    val type: String = "NON_GST_BILL", // TAX_INVOICE, NON_GST_BILL, PROFORMA
    val gstMode: String = "EXEMPT",     // INTRA_STATE, INTER_STATE, EXEMPT
    val subtotal: Double = 0.0,
    val cgst: Double = 0.0,
    val sgst: Double = 0.0,
    val igst: Double = 0.0,
    val discountAmount: Double = 0.0,
    val total: Double = 0.0,
    val paidAmount: Double = 0.0,
    val paymentStatus: String = "PAID", // PAID, UNPAID, PARTIAL
    val paymentMode: String = "Cash",
    val notes: String = "",
    val isGst: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
