package com.hisabpro.app.data.model

data class PurchaseItem(
    val id: String,
    val itemId: String? = null,
    val description: String,
    val hsnCode: String = "",
    val quantity: Double = 1.0,
    val unit: String = "Pcs",
    val unitPrice: Double = 0.0, // Cost / Purchase Price
    val gstRate: Double = 18.0 // 0, 5, 12, 18, 28
) {
    val taxableAmount: Double
        get() = quantity * unitPrice

    fun getTaxAmount(gstMode: GstMode): Double {
        if (gstMode == GstMode.EXEMPT || gstRate <= 0.0) return 0.0
        return (taxableAmount * gstRate) / 100.0
    }

    fun getCgst(gstMode: GstMode): Double {
        if (gstMode != GstMode.INTRA_STATE || gstRate <= 0.0) return 0.0
        return getTaxAmount(gstMode) / 2.0
    }

    fun getSgst(gstMode: GstMode): Double {
        if (gstMode != GstMode.INTRA_STATE || gstRate <= 0.0) return 0.0
        return getTaxAmount(gstMode) / 2.0
    }

    fun getIgst(gstMode: GstMode): Double {
        if (gstMode != GstMode.INTER_STATE || gstRate <= 0.0) return 0.0
        return getTaxAmount(gstMode)
    }

    fun getTotal(gstMode: GstMode): Double {
        return taxableAmount + getTaxAmount(gstMode)
    }
}

data class PurchaseBill(
    val id: String,
    val purchaseNumber: String,
    val vendorBillNumber: String = "",
    val supplierId: String? = null,
    val supplierName: String = "Distributor / Supplier",
    val supplierPhone: String = "",
    val supplierAddress: String = "",
    val supplierGstin: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val dueDateMillis: Long = System.currentTimeMillis() + (15L * 24 * 60 * 60 * 1000),
    val items: List<PurchaseItem> = emptyList(),
    val discountAmount: Double = 0.0,
    val notes: String = "",
    val paymentStatus: InvoiceStatus = InvoiceStatus.PAID,
    val paidAmount: Double = 0.0,
    val paymentMode: String = "Bank Transfer", // Cash, Bank Transfer, UPI, Cheque, Credit
    val itcEligible: Boolean = true, // Input Tax Credit Eligible
    val gstMode: GstMode = GstMode.INTRA_STATE,
    val createdAt: Long = System.currentTimeMillis()
) {
    val subtotal: Double
        get() = items.sumOf { it.taxableAmount }

    val totalTax: Double
        get() = if (gstMode == GstMode.EXEMPT) {
            0.0
        } else {
            items.sumOf { it.getTaxAmount(gstMode) }
        }

    val cgstTotal: Double
        get() = if (gstMode != GstMode.INTRA_STATE) 0.0 else items.sumOf { it.getCgst(gstMode) }

    val sgstTotal: Double
        get() = if (gstMode != GstMode.INTRA_STATE) 0.0 else items.sumOf { it.getSgst(gstMode) }

    val igstTotal: Double
        get() = if (gstMode != GstMode.INTER_STATE) 0.0 else items.sumOf { it.getIgst(gstMode) }

    val grandTotal: Double
        get() = kotlin.math.max(0.0, subtotal + totalTax - discountAmount)

    val dueAmount: Double
        get() = kotlin.math.max(0.0, grandTotal - paidAmount)

    val isFullyPaid: Boolean
        get() = dueAmount < 0.01

    val itcAmount: Double
        get() = if (itcEligible) totalTax else 0.0
}
