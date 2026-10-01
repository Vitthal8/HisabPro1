package com.hisabpro.app.data.model

enum class InvoiceType(val label: String, val prefix: String) {
    TAX_INVOICE("GST Tax Invoice", "INV"),
    NON_GST_BILL("Simple Bill (Non-GST)", "BILL"),
    PROFORMA("Quotation / Proforma", "QUOT");

    companion object {
        fun fromString(value: String): InvoiceType {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: TAX_INVOICE
        }
    }
}

enum class GstMode(val label: String) {
    INTRA_STATE("Intra-State (CGST + SGST)"),
    INTER_STATE("Inter-State (IGST)"),
    EXEMPT("None / Exempt");

    companion object {
        fun fromString(value: String): GstMode {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: INTRA_STATE
        }
    }
}

enum class InvoiceStatus(val label: String) {
    PAID("Paid"),
    PARTIAL("Partial"),
    UNPAID("Unpaid / Credit"),
    CANCELLED("Cancelled");

    companion object {
        fun fromString(value: String): InvoiceStatus {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: PAID
        }
    }
}

data class InvoiceItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val description: String,
    val hsnCode: String = "",
    val quantity: Double = 1.0,
    val unit: String = "Pcs",
    val unitPrice: Double = 0.0,
    val gstRate: Double = 18.0, // 0, 5, 12, 18, 28
    val discount: Double = 0.0
) {
    val taxableAmount: Double
        get() = kotlin.math.max(0.0, (quantity * unitPrice) - discount)

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

data class Invoice(
    val id: String,
    val businessId: String = "default_business",
    val invoiceNumber: String,
    val type: InvoiceType = InvoiceType.TAX_INVOICE,
    val gstMode: GstMode = GstMode.INTRA_STATE,
    val customerId: String? = null,
    val customerName: String = "Cash Customer",
    val customerPhone: String = "",
    val customerAddress: String = "",
    val customerGstin: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val items: List<InvoiceItem> = emptyList(),
    val discountAmount: Double = 0.0,
    val notes: String = "",
    val paymentStatus: InvoiceStatus = InvoiceStatus.PAID,
    val paidAmount: Double = 0.0,
    val paymentMode: String = "Cash",
    val createdAt: Long = System.currentTimeMillis(),
    val fallbackSubtotal: Double? = null,
    val fallbackTaxableAmount: Double? = null,
    val fallbackCgst: Double? = null,
    val fallbackSgst: Double? = null,
    val fallbackIgst: Double? = null,
    val fallbackTotal: Double? = null
) {
    val subtotal: Double
        get() = if (items.isNotEmpty()) {
            items.sumOf { it.taxableAmount }
        } else {
            fallbackSubtotal ?: 0.0
        }

    val totalTax: Double
        get() = if (type == InvoiceType.NON_GST_BILL || gstMode == GstMode.EXEMPT) {
            0.0
        } else if (items.isNotEmpty()) {
            items.sumOf { it.getTaxAmount(gstMode) }
        } else {
            (fallbackCgst ?: 0.0) + (fallbackSgst ?: 0.0) + (fallbackIgst ?: 0.0)
        }

    val cgstTotal: Double
        get() = if (type == InvoiceType.NON_GST_BILL || gstMode != GstMode.INTRA_STATE) {
            0.0
        } else if (items.isNotEmpty()) {
            items.sumOf { it.getCgst(gstMode) }
        } else {
            fallbackCgst ?: 0.0
        }

    val sgstTotal: Double
        get() = if (type == InvoiceType.NON_GST_BILL || gstMode != GstMode.INTRA_STATE) {
            0.0
        } else if (items.isNotEmpty()) {
            items.sumOf { it.getSgst(gstMode) }
        } else {
            fallbackSgst ?: 0.0
        }

    val igstTotal: Double
        get() = if (type == InvoiceType.NON_GST_BILL || gstMode != GstMode.INTER_STATE) {
            0.0
        } else if (items.isNotEmpty()) {
            items.sumOf { it.getIgst(gstMode) }
        } else {
            fallbackIgst ?: 0.0
        }

    val grandTotal: Double
        get() = if (items.isNotEmpty()) {
            kotlin.math.max(0.0, subtotal + totalTax - discountAmount)
        } else if (fallbackTotal != null) {
            fallbackTotal
        } else {
            kotlin.math.max(0.0, subtotal + totalTax - discountAmount)
        }

    val dueAmount: Double
        get() = kotlin.math.max(0.0, grandTotal - paidAmount)

    val isFullyPaid: Boolean
        get() = dueAmount < 0.01

    val isQuickSale: Boolean
        get() = customerId == null && customerName.equals("Cash Customer", ignoreCase = true)

    val isGstInvoice: Boolean
        get() = type == InvoiceType.TAX_INVOICE && gstMode != GstMode.EXEMPT
}
