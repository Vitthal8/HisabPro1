package com.hisabpro.app

import com.hisabpro.app.data.model.GstMode
import com.hisabpro.app.data.model.Invoice
import com.hisabpro.app.data.model.InvoiceItem
import com.hisabpro.app.data.model.InvoiceType
import com.hisabpro.app.data.model.PartyType
import com.hisabpro.app.util.IndianAccountingFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class IndianAccountingTest {

    @Test
    fun testIndianNumberCurrencyFormatting() {
        assertEquals("₹1,000", IndianAccountingFormat.formatIndianCurrency(1000.0))
        assertEquals("₹10,000", IndianAccountingFormat.formatIndianCurrency(10000.0))
        assertEquals("₹1,00,000", IndianAccountingFormat.formatIndianCurrency(100000.0))
        assertEquals("₹1,23,456", IndianAccountingFormat.formatIndianCurrency(123456.0))
        assertEquals("₹1,00,00,000", IndianAccountingFormat.formatIndianCurrency(10000000.0))
    }

    @Test
    fun testDynamicFinancialYear() {
        val cal = Calendar.getInstance()

        // 15 May 2025 -> FY 2025-26
        cal.set(2025, Calendar.MAY, 15)
        assertEquals("2025-26", IndianAccountingFormat.getFinancialYear(cal.timeInMillis))

        // 31 March 2026 -> FY 2025-26
        cal.set(2026, Calendar.MARCH, 31)
        assertEquals("2025-26", IndianAccountingFormat.getFinancialYear(cal.timeInMillis))

        // 1 April 2026 -> FY 2026-27
        cal.set(2026, Calendar.APRIL, 1)
        assertEquals("2026-27", IndianAccountingFormat.getFinancialYear(cal.timeInMillis))

        // 15 January 2026 -> FY 2025-26
        cal.set(2026, Calendar.JANUARY, 15)
        assertEquals("2025-26", IndianAccountingFormat.getFinancialYear(cal.timeInMillis))
    }

    @Test
    fun testInvoiceNumberWithFY() {
        val cal = Calendar.getInstance()
        cal.set(2025, Calendar.JULY, 1)
        val invoiceNo = IndianAccountingFormat.formatInvoiceNumberWithFY("INV", 1, cal.timeInMillis)
        assertEquals("2025-26/INV/001", invoiceNo)

        val billNo = IndianAccountingFormat.formatInvoiceNumberWithFY("BILL", 42, cal.timeInMillis)
        assertEquals("2025-26/BILL/042", billNo)
    }

    @Test
    fun testNonGstInvoiceCalculations() {
        val items = listOf(
            InvoiceItem(
                id = "item_1",
                description = "Basmati Rice 25kg",
                quantity = 2.0,
                unit = "Bags",
                unitPrice = 1500.0,
                gstRate = 0.0
            ),
            InvoiceItem(
                id = "item_2",
                description = "Mustard Oil 1L",
                quantity = 5.0,
                unit = "Bottles",
                unitPrice = 160.0,
                gstRate = 5.0 // Even if item has rate, Non-GST invoice ignores tax
            )
        )

        val nonGstInvoice = Invoice(
            id = "inv_nongst_1",
            invoiceNumber = "2025-26/BILL/001",
            type = InvoiceType.NON_GST_BILL,
            gstMode = GstMode.EXEMPT,
            items = items,
            discountAmount = 100.0,
            paidAmount = 2500.0
        )

        // Subtotal = (2 * 1500) + (5 * 160) = 3000 + 800 = 3800
        assertEquals(3800.0, nonGstInvoice.subtotal, 0.01)
        // Non-GST invoice MUST have zero tax
        assertEquals(0.0, nonGstInvoice.totalTax, 0.01)
        assertEquals(0.0, nonGstInvoice.cgstTotal, 0.01)
        assertEquals(0.0, nonGstInvoice.sgstTotal, 0.01)
        assertEquals(0.0, nonGstInvoice.igstTotal, 0.01)
        // Grand Total = 3800 - 100 discount = 3700
        assertEquals(3700.0, nonGstInvoice.grandTotal, 0.01)
        // Due Amount = 3700 - 2500 paid = 1200
        assertEquals(1200.0, nonGstInvoice.dueAmount, 0.01)
    }

    @Test
    fun testGstIntraStateInvoiceCalculations() {
        val items = listOf(
            InvoiceItem(
                id = "item_1",
                description = "LED Monitor",
                quantity = 2.0,
                unit = "Pcs",
                unitPrice = 10000.0,
                gstRate = 18.0
            )
        )

        val gstInvoice = Invoice(
            id = "inv_gst_intra",
            invoiceNumber = "2025-26/INV/001",
            type = InvoiceType.TAX_INVOICE,
            gstMode = GstMode.INTRA_STATE,
            items = items,
            discountAmount = 0.0
        )

        // Subtotal = 2 * 10000 = 20000
        assertEquals(20000.0, gstInvoice.subtotal, 0.01)
        // CGST (9%) = 1800, SGST (9%) = 1800, Total Tax = 3600
        assertEquals(1800.0, gstInvoice.cgstTotal, 0.01)
        assertEquals(1800.0, gstInvoice.sgstTotal, 0.01)
        assertEquals(0.0, gstInvoice.igstTotal, 0.01)
        assertEquals(3600.0, gstInvoice.totalTax, 0.01)
        assertEquals(23600.0, gstInvoice.grandTotal, 0.01)
    }

    @Test
    fun testGstInterStateInvoiceCalculations() {
        val items = listOf(
            InvoiceItem(
                id = "item_1",
                description = "Air Conditioner",
                quantity = 1.0,
                unit = "Pcs",
                unitPrice = 30000.0,
                gstRate = 28.0
            )
        )

        val gstInvoice = Invoice(
            id = "inv_gst_inter",
            invoiceNumber = "2025-26/INV/002",
            type = InvoiceType.TAX_INVOICE,
            gstMode = GstMode.INTER_STATE,
            items = items,
            discountAmount = 1000.0
        )

        // Subtotal = 30000
        assertEquals(30000.0, gstInvoice.subtotal, 0.01)
        // IGST (28%) = 8400, CGST = 0, SGST = 0
        assertEquals(0.0, gstInvoice.cgstTotal, 0.01)
        assertEquals(0.0, gstInvoice.sgstTotal, 0.01)
        assertEquals(8400.0, gstInvoice.igstTotal, 0.01)
        assertEquals(8400.0, gstInvoice.totalTax, 0.01)
        // Grand Total = 30000 + 8400 - 1000 = 37400
        assertEquals(37400.0, gstInvoice.grandTotal, 0.01)
    }

    @Test
    fun testPartyBalanceDrCrIndicators() {
        // Customer who owes ₹5000 -> Dr (Debit)
        val customerDr = IndianAccountingFormat.getDrCrIndicator(5000.0, isCustomer = true)
        assertEquals("Dr", customerDr)

        // Customer with advance payment -₹1000 -> Cr (Credit)
        val customerCr = IndianAccountingFormat.getDrCrIndicator(-1000.0, isCustomer = true)
        assertEquals("Cr", customerCr)

        // Supplier whom we owe ₹8000 -> Cr (Credit)
        val supplierCr = IndianAccountingFormat.getDrCrIndicator(8000.0, isCustomer = false)
        assertEquals("Cr", supplierCr)
    }
}
