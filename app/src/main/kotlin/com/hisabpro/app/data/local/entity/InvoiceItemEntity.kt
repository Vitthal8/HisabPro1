package com.hisabpro.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "invoice_items",
    indices = [
        Index(value = ["invoiceId"]),
        Index(value = ["itemId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = InvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class InvoiceItemEntity(
    @PrimaryKey val id: String,
    val invoiceId: String,
    val itemId: String? = null,
    val itemName: String,
    val hsnCode: String = "",
    val qty: Double = 1.0,
    val unit: String = "Pcs",
    val rate: Double = 0.0,
    val discount: Double = 0.0,
    val cgstRate: Double = 0.0,
    val sgstRate: Double = 0.0,
    val amount: Double = 0.0
)
