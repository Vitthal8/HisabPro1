package com.hisabpro.app.domain.accounting

import com.hisabpro.app.data.model.GstMode
import com.hisabpro.app.data.model.InvoiceType
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * HisabPro Accounting Engine
 * Centralized, double-entry and precision monetary calculation rules.
 * Uses BigDecimal with RoundingMode.HALF_UP to avoid IEEE 754 floating-point drift.
 */
object AccountingEngine {

    fun roundToTwoDecimals(amount: Double): Double {
        return BigDecimal(amount.toString())
            .setScale(2, RoundingMode.HALF_UP)
            .toDouble()
    }

    /**
     * Calculates line item taxable amount = Quantity * Unit Rate
     */
    fun calculateLineItemTaxable(quantity: Double, unitPrice: Double): Double {
        if (quantity <= 0.0 || unitPrice <= 0.0) return 0.0
        val q = BigDecimal(quantity.toString())
        val p = BigDecimal(unitPrice.toString())
        return q.multiply(p).setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    /**
     * Calculates tax amount for a line item based on GST rate.
     * Returns 0.0 if Non-GST or Exempt.
     */
    fun calculateTaxAmount(taxableAmount: Double, gstRate: Double, isGst: Boolean): Double {
        if (!isGst || gstRate <= 0.0 || taxableAmount <= 0.0) return 0.0
        val base = BigDecimal(taxableAmount.toString())
        val rate = BigDecimal(gstRate.toString())
        return base.multiply(rate)
            .divide(BigDecimal("100"), 2, RoundingMode.HALF_UP)
            .toDouble()
    }

    /**
     * Computes Central GST (CGST) for Intra-State supply (50% of total GST).
     */
    fun calculateCgst(taxAmount: Double, gstMode: GstMode, isGst: Boolean): Double {
        if (!isGst || gstMode != GstMode.INTRA_STATE || taxAmount <= 0.0) return 0.0
        return BigDecimal(taxAmount.toString())
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
     * Calculates invoice grand total = (Subtotal + Tax) - Discount
     */
    fun calculateGrandTotal(
        subtotal: Double,
        totalTax: Double,
        discountAmount: Double
    ): Double {
        val sub = BigDecimal(subtotal.toString())
        val tax = BigDecimal(totalTax.toString())
        val disc = BigDecimal(discountAmount.toString())
        val total = sub.add(tax).subtract(disc).setScale(2, RoundingMode.HALF_UP).toDouble()
        return total.coerceAtLeast(0.0)
    }

    /**
     * Calculates outstanding balance due = Grand Total - Paid Amount
     */
    fun calculateBalanceDue(grandTotal: Double, paidAmount: Double): Double {
        val total = BigDecimal(grandTotal.toString())
        val paid = BigDecimal(paidAmount.toString())
        val due = total.subtract(paid).setScale(2, RoundingMode.HALF_UP).toDouble()
        return due.coerceAtLeast(0.0)
    }

    /**
     * Calculates profit margin amount and percentage at cost price.
     */
    fun calculateProfitMargin(salePrice: Double, purchasePrice: Double): Pair<Double, Double> {
        val s = BigDecimal(salePrice.toString())
        val p = BigDecimal(purchasePrice.toString())
        val diff = s.subtract(p).setScale(2, RoundingMode.HALF_UP).toDouble()
        val percent = if (purchasePrice > 0.0) {
            BigDecimal(diff.toString())
                .multiply(BigDecimal("100"))
                .divide(p, 2, RoundingMode.HALF_UP)
                .toDouble()
        } else {
            0.0
        }
        return Pair(diff, percent)
    }
}
