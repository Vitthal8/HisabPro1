package com.hisabpro.app

import com.hisabpro.app.data.model.GstMode
import com.hisabpro.app.data.model.InvoiceItem
import com.hisabpro.app.data.model.InvoiceType
import com.hisabpro.app.domain.accounting.AccountingEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class GstTaxEngineTest {

    @Test
    fun testNonGstBillIgnoresAllGstRates() {
        val items = listOf(
            InvoiceItem(id = "1", description = "Wheat Flour 10kg", quantity = 3.0, unitPrice = 380.0, gstRate = 5.0),
            InvoiceItem(id = "2", description = "Cooking Oil 1L", quantity = 2.0, unitPrice = 160.0, gstRate = 12.0)
        )

        val result = AccountingEngine.calculateInvoiceTotals(
            items = items,
            discountAmount = 60.0,
            paidAmount = 1400.0,
            gstMode = GstMode.EXEMPT,
            isGstBusinessEnabled = false,
            invoiceType = InvoiceType.NON_GST_BILL
        )

        // Subtotal = (3 * 380) + (2 * 160) = 1140 + 320 = 1460
        assertEquals(1460.0, result.subtotal, 0.001)
        assertEquals(0.0, result.totalTax, 0.001)
        assertEquals(0.0, result.cgstTotal, 0.001)
        assertEquals(0.0, result.sgstTotal, 0.001)
        assertEquals(0.0, result.igstTotal, 0.001)
        // Grand total = 1460 - 60 = 1400
        assertEquals(1400.0, result.grandTotal, 0.001)
        assertEquals(0.0, result.dueAmount, 0.001)
    }

    @Test
    fun testCgstAndSgstSplitPrecision() {
        val line = AccountingEngine.calculateGstItem(
            quantity = 1.0,
            unitPrice = 1000.0,
            gstRate = 18.0,
            gstMode = GstMode.INTRA_STATE,
            isGstBusinessEnabled = true
        )

        assertEquals(1000.0, line.taxableAmount, 0.001)
        assertEquals(180.0, line.taxAmount, 0.001)
        assertEquals(90.0, line.cgst, 0.001)
        assertEquals(90.0, line.sgst, 0.001)
        assertEquals(0.0, line.igst, 0.001)
        assertEquals(1180.0, line.totalAmount, 0.001)
    }

    @Test
    fun testIgstInterStateFullTax() {
        val line = AccountingEngine.calculateGstItem(
            quantity = 1.0,
            unitPrice = 1000.0,
            gstRate = 18.0,
            gstMode = GstMode.INTER_STATE,
            isGstBusinessEnabled = true
        )

        assertEquals(1000.0, line.taxableAmount, 0.001)
        assertEquals(180.0, line.taxAmount, 0.001)
        assertEquals(0.0, line.cgst, 0.001)
        assertEquals(0.0, line.sgst, 0.001)
        assertEquals(180.0, line.igst, 0.001)
        assertEquals(1180.0, line.totalAmount, 0.001)
    }

    @Test
    fun testMixedGstRateSlabsInSingleInvoice() {
        val items = listOf(
            InvoiceItem(id = "1", description = "Rice (0%)", quantity = 10.0, unitPrice = 60.0, gstRate = 0.0),       // 600
            InvoiceItem(id = "2", description = "Tea (5%)", quantity = 2.0, unitPrice = 200.0, gstRate = 5.0),         // 400 + 20 tax = 420
            InvoiceItem(id = "3", description = "Butter (12%)", quantity = 1.0, unitPrice = 500.0, gstRate = 12.0),     // 500 + 60 tax = 560
            InvoiceItem(id = "4", description = "Hardware (18%)", quantity = 2.0, unitPrice = 1000.0, gstRate = 18.0),  // 2000 + 360 tax = 2360
            InvoiceItem(id = "5", description = "AC Unit (28%)", quantity = 1.0, unitPrice = 30000.0, gstRate = 28.0)   // 30000 + 8400 tax = 38400
        )

        val result = AccountingEngine.calculateGstInvoice(
            items = items,
            discountAmount = 500.0,
            paidAmount = 30000.0,
            gstMode = GstMode.INTRA_STATE
        )

        // Subtotal = 600 + 400 + 500 + 2000 + 30000 = 33500
        assertEquals(33500.0, result.subtotal, 0.01)
        // Total Tax = 0 + 20 + 60 + 360 + 8400 = 8840
        assertEquals(8840.0, result.totalTax, 0.01)
        assertEquals(4420.0, result.cgstTotal, 0.01)
        assertEquals(4420.0, result.sgstTotal, 0.01)
        // Grand Total = 33500 + 8840 - 500 = 41840
        assertEquals(41840.0, result.grandTotal, 0.01)
        assertEquals(11840.0, result.dueAmount, 0.01)
    }

    @Test
    fun testRoundingHalfUpPrecision() {
        val line = AccountingEngine.calculateGstItem(
            quantity = 3.0,
            unitPrice = 33.33, // Taxable = 99.99
            gstRate = 18.0,    // 99.99 * 0.18 = 17.9982 -> 18.00
            gstMode = GstMode.INTRA_STATE,
            isGstBusinessEnabled = true
        )

        assertEquals(99.99, line.taxableAmount, 0.001)
        assertEquals(18.00, line.taxAmount, 0.001)
        assertEquals(9.00, line.cgst, 0.001)
        assertEquals(9.00, line.sgst, 0.001)
        assertEquals(117.99, line.totalAmount, 0.001)
    }
}
