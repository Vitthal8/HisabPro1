package com.hisabpro.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "items",
    indices = [
        Index(value = ["business_id"]),
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
data class ItemEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "business_id")
    val businessId: String = "default_business",
    val name: String,
    @ColumnInfo(name = "item_code")
    val itemCode: String = "",
    val unit: String = "Pcs",
    @ColumnInfo(name = "hsn_code")
    val hsnCode: String = "",
    @ColumnInfo(name = "purchase_price")
    val purchasePrice: Long = 0L, // In paise
    @ColumnInfo(name = "sell_price")
    val sellPrice: Long = 0L,     // In paise
    @ColumnInfo(name = "gst_rate")
    val gstRate: Double = 0.0,
    val category: String = "General",
    @ColumnInfo(name = "stock_qty")
    val stockQty: Double = 0.0,
    @ColumnInfo(name = "low_stock_threshold")
    val lowStockThreshold: Double = 5.0,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long? = null,
    @ColumnInfo(name = "synced_at")
    val syncedAt: Long? = null
)
