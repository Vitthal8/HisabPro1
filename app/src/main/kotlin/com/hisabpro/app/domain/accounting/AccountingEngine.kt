package com.hisabpro.app.domain.accounting

import com.hisabpro.app.data.model.GstMode
import com.hisabpro.app.data.model.InvoiceItem
import com.hisabpro.app.data.model.InvoiceType
import com.hisabpro.app.util.toPaise
import java.math.BigDecimal
import java.math.RoundingMode

data class LineItemCalculation(
    val taxableAmount: Double,
    val taxAmount: Double,
    val cgst: Double,
    val sgst: Double,
    val igst: Double,
    val totalAmount: Double
) {
    val taxableAmountPaise: Long get() = taxableAmount.toPaise()
    val taxAmountPaise: Long get() = taxAmount.toPaise()
    val cgstPaise: Long get() = cgst.toPaise()
    val sgstPaise: Long get() = sgst.toPaise()
    val igstPaise: Long get() = igst.toPaise()
    val totalAmountPaise: Long get() = totalAmount.toPaise()
}

data class InvoiceCalculationResult(
    val subtotal: Double,
    val totalTax: Double,
    val cgstTotal: Double,
    val sgstTotal: Double,
    val igstTotal: Double,
    val discountAmount: Double,
    val grandTotal: Double,
    val dueAmount: Double
) {
    val subtotalPaise: Long get() = subtotal.toPaise()
    val totalTaxPaise: Long get() = totalTax.toPaise()
    val cgstTotalPaise: Long get() = cgstTotal.toPaise()
    val sgstTotalPaise: Long get() = sgstTotal.toPaise()
    val igstTotalPaise: Long get() = igstTotal.toPaise()
    val discountAmountPaise: Long get() = discountAmount.toPaise()
    val grandTotalPaise: Long get() = grandTotal.toPaise()
    val dueAmountPaise: Long get() = dueAmount.toPaise()
}

/**
 * HisabPro Accounting Engine
 * Centralized, double-entry and precision monetary calculation rules.
 * Supports both GST and Non-GST architecture explicitly.
 *
 * ROUNDING BOUNDARY RULES:
 * Rounding is performed ONLY at defined boundaries:
 * 1. Per-line GST tax calculation.
 * 2. Invoice grand total & balance due calculation.
 * All intermediate operations use BigDecimal scale 2 HALF_UP arithmetic to avoid floating-point drift.
 */
object AccountingEngine {

    fun roundToTwoDecimals(amount: Double): Double {
        return BigDecimal.valueOf(amount)
            .setScale(2, RoundingMode.HALF_UP)
            .toDouble()
    }

    /**
     * Calculates line item taxable amount = Quantity * Unit Rate
     */
    fun calculateLineItemTaxable(quantity: Double, unitPrice: Double): Double {
        if (quantity <= 0.0 || unitPrice <= 0.0) return 0.0
        val q = BigDecimal.valueOf(quantity)
        val p = BigDecimal.valueOf(unitPrice)
        return q.multiply(p).setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    /**
     * Calculates tax amount for a line item based on GST rate.
     * Returns 0.0 if Non-GST or Exempt.
     */
    fun calculateTaxAmount(taxableAmount: Double, gstRate: Double, isGst: Boolean): Double {
        if (!isGst || gstRate <= 0.0 || taxableAmount <= 0.0) return 0.0
        val base = BigDecimal.valueOf(taxableAmount)
        val rate = BigDecimal.valueOf(gstRate)
        return base.multiply(rate)
            .divide(BigDecimal("100"), 2, RoundingMode.HALF_UP)
            .toDouble()
    }

    /**
     * Computes Central GST (CGST) for Intra-State supply (50% of total GST).
     */
    fun calculateCgst(taxAmount: Double, gstMode: GstMode, isGst: Boolean): Double {
        if (!isGst || gstMode != GstMode.INTRA_STATE || taxAmount <= 0.0) return 0.0
        return BigDecimal.valueOf(taxAmount)
            .divide(BigDecimal("2"), 2, RoundingMode.HALF_UP)
            .toDouble()
    }

    /**
     * Computes State GST (SGST) for Intra-State supply (50% of total GST).
     */
    fun calculateSgst(taxAmount: Double, gstMode: GstMode, isGst: Boolean): Double {
        return calculateCgst(taxAmount, gstMode, isGst)
    }

    /**
     * Computes Integrated GST (IGST) for Inter-State supply (100% of total GST).
     */
    fun calculateIgst(taxAmount: Double, gstMode: GstMode, isGst: Boolean): Double {
        if (!isGst || gstMode != GstMode.INTER_STATE || taxAmount <= 0.0) return 0.0
        return roundToTwoDecimals(taxAmount)
    }

    /**
     * Reusable Non-GST calculation logic for a single line item.
     * Item Total = Quantity * Unit Rate
     */
    fun calculateNonGstItem(quantity: Double, unitPrice: Double): LineItemCalculation {
        val taxable = calculateLineItemTaxable(quantity, unitPrice)
        return LineItemCalculation(
            taxableAmount = taxable,
            taxAmount = 0.0,
            cgst = 0.0,
            sgst = 0.0,
            igst = 0.0,
            totalAmount = taxable
        )
    }

    /**
     * Reusable GST calculation logic for a single line item.
     */
    fun calculateGstItem(
        quantity: Double,
        unitPrice: Double,
        gstRate: Double,
        gstMode: GstMode,
        isGstBusinessEnabled: Boolean
    ): LineItemCalculation {
        val taxable = calculateLineItemTaxable(quantity, unitPrice)
        if (!isGstBusinessEnabled || gstMode == GstMode.EXEMPT || gstRate <= 0.0) {
            return LineItemCalculation(
                taxableAmount = taxable,
                taxAmount = 0.0,
                cgst = 0.0,
                sgst = 0.0,
                igst = 0.0,
                totalAmount = taxable
            )
        }

        val taxAmount = calculateTaxAmount(taxable, gstRate, isGst = true)
        val cgst = calculateCgst(taxAmount, gstMode, isGst = true)
        val sgst = calculateSgst(taxAmount, gstMode, isGst = true)
        val igst = calculateIgst(taxAmount, gstMode, isGst = true)
        val total = roundToTwoDecimals(taxable + taxAmount)

        return LineItemCalculation(
            taxableAmount = taxable,
            taxAmount = taxAmount,
            cgst = cgst,
            sgst = sgst,
            igst = igst,
            totalAmount = total
        )
    }

    /**
     * Reusable Non-GST calculation logic for an entire invoice.
     * Format: Item | Qty | Rate | Amount
     */
    fun calculateNonGstInvoice(
        items: List<InvoiceItem>,
        discountAmount: Double,
        paidAmount: Double
    ): InvoiceCalculationResult {
        var subtotalBD = BigDecimal.ZERO
        for (item in items) {
            val calc = calculateNonGstItem(item.quantity, item.unitPrice)
            subtotalBD = subtotalBD.add(BigDecimal.valueOf(calc.totalAmount))
        }
        val subtotal = subtotalBD.setScale(2, RoundingMode.HALF_UP).toDouble()
        val grandTotal = calculateGrandTotal(subtotal, 0.0, discountAmount)
        val due = calculateBalanceDue(grandTotal, paidAmount)

        return InvoiceCalculationResult(
            subtotal = subtotal,
            totalTax = 0.0,
            cgstTotal = 0.0,
            sgstTotal = 0.0,
            igstTotal = 0.0,
            discountAmount = roundToTwoDecimals(discountAmount),
            grandTotal = grandTotal,
            dueAmount = due
        )
    }

    /**
     * Reusable GST calculation logic for an entire invoice.
     */
    fun calculateGstInvoice(
        items: List<InvoiceItem>,
        discountAmount: Double,
        paidAmount: Double,
        gstMode: GstMode
    ): InvoiceCalculationResult {
        val lineCalcs = items.map {
            calculateGstItem(it.quantity, it.unitPrice, it.gstRate, gstMode, isGstBusinessEnabled = true)
        }
        var subtotalBD = BigDecimal.ZERO
        var totalTaxBD = BigDecimal.ZERO
        var cgstBD = BigDecimal.ZERO
        var sgstBD = BigDecimal.ZERO
        var igstBD = BigDecimal.ZERO

        for (lc in lineCalcs) {
            subtotalBD = subtotalBD.add(BigDecimal.valueOf(lc.taxableAmount))
            totalTaxBD = totalTaxBD.add(BigDecimal.valueOf(lc.taxAmount))
            cgstBD = cgstBD.add(BigDecimal.valueOf(lc.cgst))
            sgstBD = sgstBD.add(BigDecimal.valueOf(lc.sgst))
            igstBD = igstBD.add(BigDecimal.valueOf(lc.igst))
        }

        val subtotal = subtotalBD.setScale(2, RoundingMode.HALF_UP).toDouble()
        val totalTax = totalTaxBD.setScale(2, RoundingMode.HALF_UP).toDouble()
        val cgstTotal = cgstBD.setScale(2, RoundingMode.HALF_UP).toDouble()
        val sgstTotal = sgstBD.setScale(2, RoundingMode.HALF_UP).toDouble()
        val igstTotal = igstBD.setScale(2, RoundingMode.HALF_UP).toDouble()

        val grandTotal = calculateGrandTotal(subtotal, totalTax, discountAmount)
        val due = calculateBalanceDue(grandTotal, paidAmount)

        return InvoiceCalculationResult(
            subtotal = subtotal,
            totalTax = totalTax,
            cgstTotal = cgstTotal,
            sgstTotal = sgstTotal,
            igstTotal = igstTotal,
            discountAmount = roundToTwoDecimals(discountAmount),
            grandTotal = grandTotal,
            dueAmount = due
        )
    }

    /**
     * Master calculation function that automatically determines whether to execute
     * Non-GST or GST calculation logic based on business setting (isGstBusinessEnabled)
     * and invoice document type.
     */
    fun calculateInvoiceTotals(
        items: List<InvoiceItem>,
        discountAmount: Double,
        paidAmount: Double,
        gstMode: GstMode,
        isGstBusinessEnabled: Boolean,
        invoiceType: InvoiceType = InvoiceType.TAX_INVOICE
    ): InvoiceCalculationResult {
        val isEffectiveGst = GstPolicy.isGstApplicable(
            isGstRegistered = isGstBusinessEnabled,
            invoiceType = invoiceType,
            gstMode = gstMode
        )

        return if (isEffectiveGst) {
            calculateGstInvoice(items, discountAmount, paidAmount, gstMode)
        } else {
            calculateNonGstInvoice(items, discountAmount, paidAmount)
        }
    }

    /**
     * Helper to determine GST mode (Inter-State vs Intra-State) based on supply state rules.
     */
    fun determineGstMode(
        businessState: String? = null,
        businessStateCode: String? = null,
        businessGstin: String? = null,
        customerState: String? = null,
        customerStateCode: String? = null,
        customerGstin: String? = null,
        customerAddress: String? = null
    ): GstMode = com.hisabpro.app.util.IndianAccountingFormat.determineGstMode(
        businessState = businessState,
        businessStateCode = businessStateCode,
        businessGstin = businessGstin,
        customerState = customerState,
        customerStateCode = customerStateCode,
        customerGstin = customerGstin,
        customerAddress = customerAddress
    )

    /**
     * Determines whether supply is inter-state based on state identification.
     */
    fun isInterStateSupply(
        businessState: String? = null,
        businessStateCode: String? = null,
        businessGstin: String? = null,
        customerState: String? = null,
        customerStateCode: String? = null,
        customerGstin: String? = null,
        customerAddress: String? = null
    ): Boolean = com.hisabpro.app.util.IndianAccountingFormat.isInterStateSupply(
        businessState = businessState,
        businessStateCode = businessStateCode,
        businessGstin = businessGstin,
        customerState = customerState,
        customerStateCode = customerStateCode,
        customerGstin = customerGstin,
        customerAddress = customerAddress
    )

    /**
     * Calculates invoice grand total = (Subtotal + Tax) - Discount
     */
    fun calculateGrandTotal(
        subtotal: Double,
        totalTax: Double,
        discountAmount: Double
    ): Double {
        val sub = BigDecimal.valueOf(subtotal)
        val tax = BigDecimal.valueOf(totalTax)
        val disc = BigDecimal.valueOf(discountAmount)
        val total = sub.add(tax).subtract(disc).setScale(2, RoundingMode.HALF_UP).toDouble()
        return total.coerceAtLeast(0.0)
    }

    /**
     * Calculates outstanding balance due = Grand Total - Paid Amount
     */
    fun calculateBalanceDue(grandTotal: Double, paidAmount: Double): Double {
        val total = BigDecimal.valueOf(grandTotal)
        val paid = BigDecimal.valueOf(paidAmount)
        val due = total.subtract(paid).setScale(2, RoundingMode.HALF_UP).toDouble()
        return due.coerceAtLeast(0.0)
    }

    /**
     * Calculates profit margin amount and percentage at cost price.
     */
    fun calculateProfitMargin(salePrice: Double, purchasePrice: Double): Pair<Double, Double> {
        val s = BigDecimal.valueOf(salePrice)
        val p = BigDecimal.valueOf(purchasePrice)
        val diff = s.subtract(p).setScale(2, RoundingMode.HALF_UP).toDouble()
        val percent = if (purchasePrice > 0.0) {
            BigDecimal.valueOf(diff)
                .multiply(BigDecimal("100"))
                .divide(p, 2, RoundingMode.HALF_UP)
                .toDouble()
        } else {
            0.0
        }
        return Pair(diff, percent)
    }
}
