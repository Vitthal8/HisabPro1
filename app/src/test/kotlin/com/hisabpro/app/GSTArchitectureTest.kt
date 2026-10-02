package com.hisabpro.app

import com.hisabpro.app.data.model.GstMode
import com.hisabpro.app.data.model.InvoiceItem
import com.hisabpro.app.data.model.InvoiceType
import com.hisabpro.app.domain.accounting.AccountingEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class GSTArchitectureTest {

    // 1. Non-GST invoice
    @Test
    fun testNonGstInvoice() {
        val items = listOf(
            InvoiceItem(id = "1", description = "Cotton Shirt", quantity = 2.0, unitPrice = 500.0, gstRate = 0.0),
            InvoiceItem(id = "2", description = "Jeans", quantity = 1.0, unitPrice = 1200.0, gstRate = 0.0)
        )
        // Subtotal = (2 * 500) + (1 * 1200) = 2200
        val result = AccountingEngine.calculateNonGstInvoice(items, discountAmount = 200.0, paidAmount = 1000.0)

        assertEquals(2200.0, result.subtotal, 0.001)
        assertEquals(0.0, result.totalTax, 0.001)
        assertEquals(0.0, result.cgstTotal, 0.001)
        assertEquals(0.0, result.sgstTotal, 0.001)
        assertEquals(0.0, result.igstTotal, 0.001)
        assertEquals(200.0, result.discountAmount, 0.001)
        assertEquals(2000.0, result.grandTotal, 0.001)
        assertEquals(1000.0, result.dueAmount, 0.001)
    }

    // 2. CGST + SGST
    @Test
    fun testCgstPlusSgst() {
        val items = listOf(
            InvoiceItem(id = "1", description = "Office Ergonomic Chair", quantity = 1.0, unitPrice = 10000.0, gstRate = 18.0)
        )
        // Taxable = 10000, Tax = 1800 (CGST 9% = 900, SGST 9% = 900)
        val result = AccountingEngine.calculateGstInvoice(items, discountAmount = 0.0, paidAmount = 11800.0, gstMode = GstMode.INTRA_STATE)

        assertEquals(10000.0, result.subtotal, 0.001)
        assertEquals(1800.0, result.totalTax, 0.001)
        assertEquals(900.0, result.cgstTotal, 0.001)
        assertEquals(900.0, result.sgstTotal, 0.001)
        assertEquals(0.0, result.igstTotal, 0.001)
        assertEquals(11800.0, result.grandTotal, 0.001)
        assertEquals(0.0, result.dueAmount, 0.001)
    }

    // 3. IGST
    @Test
    fun testIgst() {
        val items = listOf(
            InvoiceItem(id = "1", description = "Industrial Machinery", quantity = 1.0, unitPrice = 50000.0, gstRate = 28.0)
        )
        // Taxable = 50000, Tax = 14000 (IGST = 14000, CGST = 0, SGST = 0)
        val result = AccountingEngine.calculateGstInvoice(items, discountAmount = 1000.0, paidAmount = 0.0, gstMode = GstMode.INTER_STATE)

        assertEquals(50000.0, result.subtotal, 0.001)
        assertEquals(14000.0, result.totalTax, 0.001)
        assertEquals(0.0, result.cgstTotal, 0.001)
        assertEquals(0.0, result.sgstTotal, 0.001)
        assertEquals(14000.0, result.igstTotal, 0.001)
        assertEquals(63000.0, result.grandTotal, 0.001)
        assertEquals(63000.0, result.dueAmount, 0.001)
    }

    // 4. Discount
    @Test
    fun testDiscount() {
        val items = listOf(
            InvoiceItem(id = "1", description = "Wireless Mouse", quantity = 4.0, unitPrice = 800.0, gstRate = 18.0)
        )
        // Subtotal = 3200, Tax (18%) = 576, Total before disc = 3776
        // Discount = 276 -> Grand Total = 3500
        val result = AccountingEngine.calculateGstInvoice(items, discountAmount = 276.0, paidAmount = 3500.0, gstMode = GstMode.INTRA_STATE)

        assertEquals(3200.0, result.subtotal, 0.001)
        assertEquals(576.0, result.totalTax, 0.001)
        assertEquals(276.0, result.discountAmount, 0.001)
        assertEquals(3500.0, result.grandTotal, 0.001)
        assertEquals(0.0, result.dueAmount, 0.001)
    }

    // 5. Multiple invoice items
    @Test
    fun testMultipleInvoiceItems() {
        val items = listOf(
            InvoiceItem(id = "1", description = "Rice (0% GST)", quantity = 10.0, unitPrice = 100.0, gstRate = 0.0),
            InvoiceItem(id = "2", description = "Edible Oil (5% GST)", quantity = 2.0, unitPrice = 200.0, gstRate = 5.0),
            InvoiceItem(id = "3", description = "Butter (12% GST)", quantity = 1.0, unitPrice = 500.0, gstRate = 12.0),
            InvoiceItem(id = "4", description = "LED Bulb (18% GST)", quantity = 2.0, unitPrice = 150.0, gstRate = 18.0),
            InvoiceItem(id = "5", description = "Chocolate (28% GST)", quantity = 5.0, unitPrice = 100.0, gstRate = 28.0)
        )

        val result = AccountingEngine.calculateGstInvoice(items, discountAmount = 50.0, paidAmount = 0.0, gstMode = GstMode.INTRA_STATE)

        // Subtotals:
        // Item 1: 10 * 100 = 1000 (Tax = 0)
        // Item 2: 2 * 200 = 400 (Tax = 20)
        // Item 3: 1 * 500 = 500 (Tax = 60)
        // Item 4: 2 * 150 = 300 (Tax = 54)
        // Item 5: 5 * 100 = 500 (Tax = 140)
        // Subtotal = 2700, Total Tax = 274
        assertEquals(2700.0, result.subtotal, 0.001)
        assertEquals(274.0, result.totalTax, 0.001)
        assertEquals(137.0, result.cgstTotal, 0.001)
        assertEquals(137.0, result.sgstTotal, 0.001)
        assertEquals(2924.0, result.grandTotal, 0.001)
    }

    // 6. Rounding
    @Test
    fun testRounding() {
        // Taxable = 399.99
        // Tax = 399.99 * 0.18 = 71.9982 -> rounded to 72.00
        val line = AccountingEngine.calculateGstItem(3.0, 133.33, 18.0, GstMode.INTRA_STATE, isGstBusinessEnabled = true)

        assertEquals(399.99, line.taxableAmount, 0.001)
        assertEquals(72.00, line.taxAmount, 0.001)
        assertEquals(36.00, line.cgst, 0.001)
        assertEquals(36.00, line.sgst, 0.001)
        assertEquals(471.99, line.totalAmount, 0.001)
    }

    // 7. Zero GST
    @Test
    fun testZeroGst() {
        val line = AccountingEngine.calculateGstItem(5.0, 250.0, 0.0, GstMode.INTRA_STATE, isGstBusinessEnabled = true)

        assertEquals(1250.0, line.taxableAmount, 0.001)
        assertEquals(0.0, line.taxAmount, 0.001)
        assertEquals(0.0, line.cgst, 0.001)
        assertEquals(0.0, line.sgst, 0.001)
        assertEquals(1250.0, line.totalAmount, 0.001)
    }

    // 8. GST disabled
    @Test
    fun testGstDisabled() {
        val items = listOf(
            InvoiceItem(id = "1", description = "High-End Laptop", quantity = 1.0, unitPrice = 60000.0, gstRate = 18.0)
        )

        // Even though item has 18% GST rate, when isGstBusinessEnabled = false, master engine must execute Non-GST logic
        val result = AccountingEngine.calculateInvoiceTotals(
            items = items,
            discountAmount = 2000.0,
            paidAmount = 58000.0,
            gstMode = GstMode.INTRA_STATE,
            isGstBusinessEnabled = false,
            invoiceType = InvoiceType.TAX_INVOICE
        )

        assertEquals(60000.0, result.subtotal, 0.001)
        assertEquals(0.0, result.totalTax, 0.001)
        assertEquals(0.0, result.cgstTotal, 0.001)
        assertEquals(0.0, result.sgstTotal, 0.001)
        assertEquals(0.0, result.igstTotal, 0.001)
        assertEquals(58000.0, result.grandTotal, 0.001)
        assertEquals(0.0, result.dueAmount, 0.001)
    }

    // 9. Thermal slip Non-GST formatting
    @Test
    fun testThermalSlipFormatForNonGstBusinessOmitsGst() {
        val nonGstProfile = com.hisabpro.app.data.model.BusinessProfile(
            shopName = "Ramesh General Store",
            isGstRegistered = false,
            gstin = ""
        )
        val invoice = com.hisabpro.app.data.model.Invoice(
            id = "inv_001",
            businessId = "biz_ramesh",
            invoiceNumber = "BILL-001",
            type = InvoiceType.NON_GST_BILL,
            gstMode = GstMode.EXEMPT,
            items = listOf(
                InvoiceItem(id = "i1", description = "Sugar 5kg", quantity = 1.0, unitPrice = 200.0, gstRate = 5.0)
            )
        )

        val thermalText = com.hisabpro.app.util.ThermalSlipGenerator.formatThermalSlipText(invoice, nonGstProfile)

        org.junit.Assert.assertFalse("Non-GST thermal slip must not contain GSTIN", thermalText.contains("GSTIN:"))
        org.junit.Assert.assertFalse("Non-GST thermal slip must not contain line GST %", thermalText.contains("GST 5%"))
        org.junit.Assert.assertFalse("Non-GST thermal slip must not contain Total GST", thermalText.contains("Total GST:"))
        org.junit.Assert.assertTrue("Non-GST thermal slip must contain RETAIL BILL title", thermalText.contains("RETAIL BILL / CASH SLIP"))
    }

    // 10. Historical Invoice Preservation on Business GST Toggle
    @Test
    fun testBusinessSwitchFromNonGstToGstPreservesHistoricalInvoices() {
        val oldNonGstProfile = com.hisabpro.app.data.model.BusinessProfile(
            shopName = "Shree Kirana",
            isGstRegistered = false
        )
        val oldInvoice = com.hisabpro.app.data.model.Invoice(
            id = "inv_old_1",
            businessId = "biz_shree",
            invoiceNumber = "BILL-2024-001",
            type = InvoiceType.NON_GST_BILL,
            gstMode = GstMode.EXEMPT,
            items = listOf(InvoiceItem(description = "Oil", quantity = 1.0, unitPrice = 150.0, gstRate = 12.0))
        )

        // Historical invoice created during non-GST phase
        assertEquals(0.0, oldInvoice.totalTax, 0.001)

        // Business switches to GST
        val newGstProfile = oldNonGstProfile.copy(
            isGstRegistered = true,
            gstin = "27AAAAA1234A1Z5"
        )
        org.junit.Assert.assertTrue(newGstProfile.isGstRegistered)

        // Historical invoice remains Non-GST Bill with 0 tax
        val sanitizedOldInvoice = com.hisabpro.app.domain.accounting.GstPolicy.sanitizeInvoiceForStorage(oldInvoice, oldNonGstProfile)
        assertEquals(InvoiceType.NON_GST_BILL, sanitizedOldInvoice.type)
        assertEquals(GstMode.EXEMPT, sanitizedOldInvoice.gstMode)
        assertEquals(0.0, sanitizedOldInvoice.totalTax, 0.001)
    }
}
