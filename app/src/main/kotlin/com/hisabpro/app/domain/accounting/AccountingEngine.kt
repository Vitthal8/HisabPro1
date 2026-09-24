package com.hisabpro.app.domain.accounting

import com.hisabpro.app.data.model.GstMode
import com.hisabpro.app.data.model.InvoiceItem
import com.hisabpro.app.data.model.InvoiceType
import java.math.BigDecimal
import java.math.RoundingMode

data class LineItemCalculation(
    val taxableAmount: Double,
    val taxAmount: Double,
    val cgst: Double,
    val sgst: Double,
    val igst: Double,
    val totalAmount: Double
)

data class InvoiceCalculationResult(
    val subtotal: Double,
    val totalTax: Double,
    val cgstTotal: Double,
    val sgstTotal: Double,
    val igstTotal: Double,
    val discountAmount: Double,
    val grandTotal: Double,
    val dueAmount: Double
)

/**
 * HisabPro Accounting Engine
 * Centralized, double-entry and precision monetary calculation rules.
 * Supports both GST and Non-GST architecture explicitly.
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
        val subtotal = roundToTwoDecimals(items.sumOf { calculateNonGstItem(it.quantity, it.unitPrice).totalAmount })
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
        val subtotal = roundToTwoDecimals(lineCalcs.sumOf { it.taxableAmount })
        val totalTax = roundToTwoDecimals(lineCalcs.sumOf { it.taxAmount })
        val cgstTotal = roundToTwoDecimals(lineCalcs.sumOf { it.cgst })
        val sgstTotal = roundToTwoDecimals(lineCalcs.sumOf { it.sgst })
        val igstTotal = roundToTwoDecimals(lineCalcs.sumOf { it.igst })

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
        val isEffectiveGst = isGstBusinessEnabled &&
                invoiceType == InvoiceType.TAX_INVOICE &&
                gstMode != GstMode.EXEMPT

        return if (isEffectiveGst) {
            calculateGstInvoice(items, discountAmount, paidAmount, gstMode)
        } else {
            calculateNonGstInvoice(items, discountAmount, paidAmount)
        }
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
