package com.hisabpro.app.data.model

import java.util.UUID

enum class StockReason(val label: String, val isAddition: Boolean) {
    OPENING_STOCK("Opening Stock", true),
    PURCHASE_IN("Purchase / Stock In", true),
    RETURN_IN("Customer Return", true),
    SALE_OUT("Sale / Bill Out", false),
    DAMAGE_LOSS("Damaged / Expired", false),
    MANUAL_ADJUSTMENT("Manual Count Adjustment", true);

    companion object {
        fun fromString(value: String): StockReason {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: MANUAL_ADJUSTMENT
        }
    }
}

data class StockHistoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val itemId: String,
    val changeQty: Double,
    val previousStock: Double,
    val newStock: Double,
    val reason: StockReason,
    val note: String = "",
    val timestampMillis: Long = System.currentTimeMillis()
)

data class Item(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val itemCode: String = "", // SKU / Barcode
    val category: String = "General",
    val unit: String = "Pcs", // Pcs, Kg, Box, Ltr, Mtr, Packet, Bag
    val salePrice: Double = 0.0,
    val purchasePrice: Double = 0.0,
    val gstRate: Double = 18.0, // 0, 5, 12, 18, 28
    val hsnCode: String = "",
    val currentStock: Double = 0.0,
    val minStockAlert: Double = 5.0, // Alert threshold
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    val isLowStock: Boolean
        get() = currentStock <= minStockAlert

    val isOutOfStock: Boolean
        get() = currentStock <= 0.0

    val profitMarginAmount: Double
        get() = salePrice - purchasePrice

    val profitMarginPercent: Double
        get() = if (purchasePrice > 0.0) {
            ((salePrice - purchasePrice) / purchasePrice) * 100.0
        } else 0.0

    val stockValuePurchase: Double
        get() = currentStock * purchasePrice

    val stockValueSale: Double
        get() = currentStock * salePrice
}
