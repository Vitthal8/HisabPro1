package com.hisabpro.app.data.model

import java.util.UUID

enum class StockReason(val label: String, val isAddition: Boolean) {
    OPENING_STOCK("Opening Stock", true),
    PURCHASE_IN("Purchase / Inward Stock", true),
    RETURN_IN("Customer Return", true),
    SALES_RETURN("Sales Return / Credit Note", true),
    SALE_OUT("Sale / Bill Out", false),
    PURCHASE_RETURN("Purchase Return / Debit Note", false),
    DAMAGE_LOSS("Damaged / Expired / Loss", false),
    MANUAL_ADJUSTMENT("Physical Audit / Adjustment", true);

    companion object {
        fun fromString(value: String): StockReason {
            if (value.equals("RETURN_IN", ignoreCase = true)) return SALES_RETURN
            return entries.find { it.name.equals(value, ignoreCase = true) }
                ?: MANUAL_ADJUSTMENT
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
    val timestampMillis: Long = System.currentTimeMillis(),
    val sourceTransactionId: String? = null,
    val sourceTransactionType: String? = null, // "INVOICE", "INVOICE_EDIT", "INVOICE_CANCEL", "PURCHASE", "PURCHASE_CANCEL", "SALES_RETURN", "PURCHASE_RETURN", "OPENING_STOCK", "STOCK_ADJUSTMENT"
    val sourceRefNumber: String? = null // e.g. "2025-26/INV/001", "PB-1002", "ADJ-2026-001"
) {
    val displaySourceRef: String
        get() {
            if (!sourceRefNumber.isNullOrBlank()) return sourceRefNumber
            if (note.contains("Invoice #")) {
                val num = note.substringAfter("Invoice #").substringBefore(" ").substringBefore(",")
                if (num.isNotBlank()) return "INV #$num"
            }
            if (note.contains("Purchase")) {
                val num = note.substringAfter("Purchase:").substringAfter("Purchase Bill").trim().take(15)
                if (num.isNotBlank()) return "PB: $num"
            }
            return reason.label
        }
}

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
