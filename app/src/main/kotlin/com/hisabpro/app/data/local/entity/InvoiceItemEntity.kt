package com.hisabpro.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "invoice_items",
    indices = [
        Index(value = ["invoice_id"]),
        Index(value = ["item_id"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = InvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoice_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["item_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class InvoiceItemEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "invoice_id")
    val invoiceId: String,
    @ColumnInfo(name = "item_id")
    val itemId: String? = null,
    @ColumnInfo(name = "item_name")
    val itemName: String,
    @ColumnInfo(name = "hsn_code")
    val hsnCode: String = "",
    val qty: Double = 1.0,
    val unit: String = "Pcs",
    val rate: Long = 0L,         // In paise
    val discount: Long = 0L,     // In paise
    @ColumnInfo(name = "cgst_rate")
    val cgstRate: Double = 0.0,
    @ColumnInfo(name = "sgst_rate")
    val sgstRate: Double = 0.0,
    @ColumnInfo(name = "igst_rate")
    val igstRate: Double = 0.0,
    val amount: Long = 0L,       // In paise
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long? = null,
    @ColumnInfo(name = "synced_at")
    val syncedAt: Long? = null
)
