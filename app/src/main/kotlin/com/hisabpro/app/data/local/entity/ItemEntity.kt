package com.hisabpro.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "items",
    indices = [
        Index(value = ["businessId"]),
        Index(value = ["category"])
    ]
)
data class ItemEntity(
    @PrimaryKey val id: String,
    val businessId: String = "default_business",
    val name: String,
    val itemCode: String = "",
    val unit: String = "Pcs",
    val hsnCode: String = "",
    val purchasePrice: Double = 0.0,
    val sellPrice: Double = 0.0,
    val gstRate: Double = 0.0,
    val category: String = "General",
    val stockQty: Double = 0.0,
    val minStockAlert: Double = 5.0,
    val createdAt: Long = System.currentTimeMillis()
)
