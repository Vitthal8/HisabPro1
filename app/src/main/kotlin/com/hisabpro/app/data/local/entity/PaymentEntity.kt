package com.hisabpro.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payments",
    indices = [
        Index(value = ["business_id"]),
        Index(value = ["party_id"]),
        Index(value = ["date"]),
        Index(value = ["linked_invoice_id"])
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
        ),
        ForeignKey(
            entity = InvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["linked_invoice_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class PaymentEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "business_id")
    val businessId: String = "default_business",
    @ColumnInfo(name = "party_id")
    val partyId: String? = null,
    val date: Long,
    val amount: Long, // In paise
    val mode: String = "Cash", // Cash, UPI, Bank, Cheque
    @ColumnInfo(name = "reference_no")
    val referenceNo: String = "",
    val notes: String = "",
    @ColumnInfo(name = "linked_invoice_id")
    val linkedInvoiceId: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long? = null,
    @ColumnInfo(name = "synced_at")
    val syncedAt: Long? = null
)
