package com.hisabpro.app

import com.hisabpro.app.data.model.GstMode
import com.hisabpro.app.data.model.InvoiceItem
import com.hisabpro.app.domain.accounting.AccountingEngine
import com.hisabpro.app.util.IndianAccountingFormat
import com.hisabpro.app.util.toPaise
import com.hisabpro.app.util.toRupees
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

/**
 * Unit Test Suite verifying Precision Monetary Accounting and Format Rules in HisabPro:
 * 1. 0.1 + 0.2 accumulation across 1,000 lines without floating-point drift.
 * 2. Per-line GST and invoice total round-off rules.
 * 3. Dr/Cr running balance precision in Cash Book.
 * 4. Indian Numbering System formatting for Lakh (₹1,00,000) and Crore (₹1,00,00,000).
 */
class MonetaryPrecisionAndAccountingTest {

    // 1. 0.1 + 0.2 STYLE ACCUMULATION ACROSS 1000 LINES
    @Test
    fun testFloatingPointDriftEliminationAcross1000Lines() {
        // Standard double floating point: 0.1 + 0.2 = 0.30000000000000004
        var doubleSum = 0.0
        for (i in 1..1000) {
            doubleSum += (0.10 + 0.20)
        }
        // Double sum demonstrates floating-point drift (e.g. 300.00000000000006)
        assertFalse(doubleSum == 300.0)

        // HisabPro Long paise / BigDecimal accumulation:
        var paiseSum = 0L
        val line10Paise = 0.10.toPaise() // 10 paise
        val line20Paise = 0.20.toPaise() // 20 paise

        for (i in 1..1000) {
            paiseSum += (line10Paise + line20Paise)
        }

        assertEquals(30000L, paiseSum) // 30,000 paise
        assertEquals(300.00, paiseSum.toRupees(), 0.000001) // Exactly ₹300.00 with zero drift!
        assertEquals("₹300", IndianAccountingFormat.formatIndianCurrency(paiseSum))
    }

    // 2. INVOICE TOTAL ROUND-OFF BOUNDARY RULE
    @Test
    fun testInvoiceTotalRoundOffBoundaryRule() {
        // Item: 3 units @ ₹33.33 each with 18% GST
        // Taxable = 3 * 33.33 = ₹99.99 (9999 paise)
        // Tax = 99.99 * 18% = ₹17.9982 -> rounded HALF_UP at boundary to ₹18.00 (1800 paise)
        // Grand Total = 99.99 + 18.00 = ₹117.99
        val item = InvoiceItem(
            id = "it_1",
            description = "Precision Item",
            quantity = 3.0,
            unitPrice = 33.33,
            gstRate = 18.0
        )

        val result = AccountingEngine.calculateGstInvoice(
            items = listOf(item),
            discountAmount = 0.0,
            paidAmount = 0.0,
            gstMode = GstMode.INTER_STATE
        )

        assertEquals(99.99, result.subtotal, 0.001)
        assertEquals(18.00, result.totalTax, 0.001)
        assertEquals(18.00, result.igstTotal, 0.001)
        assertEquals(117.99, result.grandTotal, 0.001)

        // Verify exact paise values
        assertEquals(9999L, result.subtotalPaise)
        assertEquals(1800L, result.totalTaxPaise)
        assertEquals(11799L, result.grandTotalPaise)
    }

    // 3. Dr/Cr RUNNING BALANCE ACCUMULATION
    @Test
    fun testDrCrRunningBalanceAccumulation() {
        // Simulate running balance calculation:
        // Opening: ₹5,000.00 Dr (500000 paise)
        // Receipt 1: +₹2,500.50 (Running: ₹7,500.50 Dr)
        // Payment 1: -₹1,200.25 (Running: ₹6,300.25 Dr)
        // Payment 2: -₹7,000.00 (Running: -₹699.75 i.e. ₹699.75 Cr)
        var runningPaise = 500000L

        runningPaise += 250050L
        assertEquals(750050L, runningPaise)
        assertEquals("Dr", IndianAccountingFormat.getDrCrIndicator(runningPaise, isCustomer = true))

        runningPaise -= 120025L
        assertEquals(630025L, runningPaise)
        assertEquals("Dr", IndianAccountingFormat.getDrCrIndicator(runningPaise, isCustomer = true))

        runningPaise -= 700000L
        assertEquals(-69975L, runningPaise)
        assertEquals("Cr", IndianAccountingFormat.getDrCrIndicator(runningPaise, isCustomer = true))

        val label = IndianAccountingFormat.getDrCrBalanceLabel(runningPaise, isCustomer = true)
        assertTrue(label.contains("Cr"))
        assertTrue(label.contains("₹699.75"))
    }

    // 4. LAKH AND CRORE FORMATTING (₹1,00,000 and ₹1,00,00,000)
    @Test
    fun testIndianNumberingLakhAndCroreFormatting() {
        // 1 Lakh Rupees = 100,000.00 = 10000000L paise
        val oneLakhPaise = 10000000L
        val formattedLakh = IndianAccountingFormat.formatIndianCurrency(oneLakhPaise)
        assertEquals("₹1,00,000", formattedLakh)

        // 1 Crore Rupees = 10,00,00,000.00 = 1000000000L paise
        val oneCrorePaise = 1000000000L
        val formattedCrore = IndianAccountingFormat.formatIndianCurrency(oneCrorePaise)
        assertEquals("₹1,00,00,000", formattedCrore)

        // Negative 1 Crore
        val negativeCrorePaise = -1000000000L
        val formattedNegativeCrore = IndianAccountingFormat.formatIndianCurrency(negativeCrorePaise)
        assertEquals("-₹1,00,00,000", formattedNegativeCrore)

        // 50 Lakhs 25 Thousand 4 hundred 30 Rupees & 50 Paise
        val complexPaise = 502543050L
        val formattedComplex = IndianAccountingFormat.formatIndianCurrency(complexPaise)
        assertEquals("₹50,25,430.50", formattedComplex)
    }
}
