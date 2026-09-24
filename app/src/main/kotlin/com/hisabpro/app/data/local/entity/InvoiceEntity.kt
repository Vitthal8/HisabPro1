package com.hisabpro.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "invoices",
    indices = [
        Index(value = ["business_id"]),
        Index(value = ["invoice_no"]),
        Index(value = ["party_id"]),
        Index(value = ["date"]),
        Index(value = ["business_id", "invoice_no"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["business_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PartyEntity::class,
            parentColumns = ["id"],
            childColumns = ["party_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class InvoiceEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "business_id")
    val businessId: String = "default_business",
    @ColumnInfo(name = "invoice_no")
    val invoiceNo: String,
    val date: Long,
    @ColumnInfo(name = "party_id")
    val partyId: String? = null,
    @ColumnInfo(name = "customer_name")
    val customerName: String = "",
    @ColumnInfo(name = "customer_phone")
    val customerPhone: String = "",
    @ColumnInfo(name = "customer_address")
    val customerAddress: String = "",
    @ColumnInfo(name = "customer_gstin")
    val customerGstin: String = "",
    val type: String = "NON_GST_BILL", // TAX_INVOICE, NON_GST_BILL, PROFORMA
    @ColumnInfo(name = "gst_mode")
    val gstMode: String = "EXEMPT",     // INTRA_STATE, INTER_STATE, EXEMPT
    val subtotal: Long = 0L,           // In paise
    val discount: Long = 0L,           // In paise
    @ColumnInfo(name = "taxable_amount")
    val taxableAmount: Long = 0L,      // In paise
    val cgst: Long = 0L,               // In paise
    val sgst: Long = 0L,               // In paise
    val igst: Long = 0L,               // In paise
    val total: Long = 0L,              // In paise
    @ColumnInfo(name = "paid_amount")
    val paidAmount: Long = 0L,         // In paise
    @ColumnInfo(name = "payment_status")
    val paymentStatus: String = "PAID", // PAID, UNPAID, PARTIAL
    @ColumnInfo(name = "payment_mode")
    val paymentMode: String = "Cash",
    val notes: String = "",
    @ColumnInfo(name = "is_gst")
    val isGst: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
