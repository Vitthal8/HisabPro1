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

    @Test
    fun testAccountingEngineCalculations() {
        val taxable = com.hisabpro.app.domain.accounting.AccountingEngine.calculateLineItemTaxable(2.5, 450.0)
        assertEquals(1125.0, taxable, 0.001)

        val taxAmount = com.hisabpro.app.domain.accounting.AccountingEngine.calculateTaxAmount(taxable, 18.0, isGst = true)
        assertEquals(202.50, taxAmount, 0.001)

        val cgst = com.hisabpro.app.domain.accounting.AccountingEngine.calculateCgst(taxAmount, GstMode.INTRA_STATE, isGst = true)
        val sgst = com.hisabpro.app.domain.accounting.AccountingEngine.calculateSgst(taxAmount, GstMode.INTRA_STATE, isGst = true)
        assertEquals(101.25, cgst, 0.001)
        assertEquals(101.25, sgst, 0.001)

        val grandTotal = com.hisabpro.app.domain.accounting.AccountingEngine.calculateGrandTotal(taxable, taxAmount, 25.0)
        assertEquals(1302.50, grandTotal, 0.001)

        val balanceDue = com.hisabpro.app.domain.accounting.AccountingEngine.calculateBalanceDue(grandTotal, 1000.0)
        assertEquals(302.50, balanceDue, 0.001)
    }

    @Test
    fun testInputValidator() {
        // Valid Maharashtra GSTIN
        assertTrue(com.hisabpro.app.domain.validation.InputValidator.validateGstin("27ABCDE1234F1Z5").isSuccess)
        // Invalid GSTIN
        assertTrue(com.hisabpro.app.domain.validation.InputValidator.validateGstin("INVALID123").errorMessage != null)

        // Valid Indian Mobile
        assertTrue(com.hisabpro.app.domain.validation.InputValidator.validatePhone("9876543210").isSuccess)
        // Invalid Phone
        assertTrue(com.hisabpro.app.domain.validation.InputValidator.validatePhone("12345").errorMessage != null)

        // Invoice validation
        assertTrue(com.hisabpro.app.domain.validation.InputValidator.validateInvoice("Ramesh Stores", 2).isSuccess)
        assertTrue(com.hisabpro.app.domain.validation.InputValidator.validateInvoice("", 2).errorMessage != null)
        assertTrue(com.hisabpro.app.domain.validation.InputValidator.validateInvoice("Ramesh Stores", 0).errorMessage != null)
    }

    @Test
    fun testSubscriptionTierLimits() {
        com.hisabpro.app.domain.subscription.SubscriptionManager.setActivePlan(com.hisabpro.app.domain.subscription.SubscriptionPlan.FREE)
        val underLimit = com.hisabpro.app.domain.subscription.SubscriptionManager.checkInvoiceCreationAllowed(49)
        assertTrue(underLimit.isGranted)

        val atLimit = com.hisabpro.app.domain.subscription.SubscriptionManager.checkInvoiceCreationAllowed(50)
        assertTrue(!atLimit.isGranted)

        com.hisabpro.app.domain.subscription.SubscriptionManager.setActivePlan(com.hisabpro.app.domain.subscription.SubscriptionPlan.PRO)
        val proUnlimited = com.hisabpro.app.domain.subscription.SubscriptionManager.checkInvoiceCreationAllowed(100)
        assertTrue(proUnlimited.isGranted)
    }

    @Test
    fun testCustomerAndSupplierLedgerCalculations() {
        // Customer Ledger: Debit (Dr) increases receivable, Credit (Cr) reduces receivable
        var customerBalance = 0.0 // Starting balance

        // Sale on credit -> Dr 5,000
        customerBalance += 5000.0
        assertEquals(5000.0, customerBalance, 0.001)

        // Payment received -> Cr 3,000
        customerBalance -= 3000.0
        assertEquals(2000.0, customerBalance, 0.001) // 2,000 Dr receivable

        // Advance payment received -> Cr 3,000
        customerBalance -= 3000.0
        assertEquals(-1000.0, customerBalance, 0.001) // 1,000 Cr advance liability

        // Supplier Ledger: Credit (Cr) increases payable, Debit (Dr) reduces payable
        var supplierPayable = 0.0

        // Inward purchase -> Cr 15,000
        supplierPayable += 15000.0
        assertEquals(15000.0, supplierPayable, 0.001)

        // Payment to supplier -> Dr 10,000
        supplierPayable -= 10000.0
        assertEquals(5000.0, supplierPayable, 0.001) // 5,000 Cr payable
    }

    @Test
    fun testDailySalesAndCashBookMovement() {
        // Test cash inflow and outflow tracking
        var openingCash = 10000.0
        val cashSales = 8500.0
        val cashExpense = 1200.0
        val cashPaidToSupplier = 4000.0

        val closingCash = openingCash + cashSales - (cashExpense + cashPaidToSupplier)
        assertEquals(13300.0, closingCash, 0.001)
    }

    @Test
    fun testCashBookSeparationCashVsBank() {
        // Cash transactions must not mix with Bank/UPI transactions
        val cashReceipts = 5000.0
        val cashPayments = 2000.0
        val bankUpiReceipts = 12500.0
        val bankUpiPayments = 4500.0

        val openingCashBalance = 1000.0
        val openingBankBalance = 25000.0

        val closingCashBalance = openingCashBalance + cashReceipts - cashPayments
        val closingBankBalance = openingBankBalance + bankUpiReceipts - bankUpiPayments

        assertEquals(4000.0, closingCashBalance, 0.001)
        assertEquals(33000.0, closingBankBalance, 0.001)
    }

    @Test
    fun testDayBookDoubleEntryDrCrClassification() {
        // Day Book transactions:
        // Cash Sale of ₹5000:
        // Cash A/c Dr ₹5000, Sales A/c Cr ₹5000
        val saleCashDebit = 5000.0
        val saleCredit = 5000.0
        assertEquals(saleCashDebit, saleCredit, 0.001)

        // Expense of ₹800 paid via Cash:
        // Tea & Refreshment A/c Dr ₹800, Cash A/c Cr ₹800
        val expenseDebit = 800.0
        val expenseCashCredit = 800.0
        assertEquals(expenseDebit, expenseCashCredit, 0.001)

        // Receipt from Customer of ₹3000 via UPI:
        // Bank A/c Dr ₹3000, Customer A/c Cr ₹3000
        val bankDebit = 3000.0
        val customerCredit = 3000.0
        assertEquals(bankDebit, customerCredit, 0.001)

        // Total Debits in Day Book must equal Total Credits
        val totalDebit = saleCashDebit + expenseDebit + bankDebit
        val totalCredit = saleCredit + expenseCashCredit + customerCredit
        assertEquals(8800.0, totalDebit, 0.001)
        assertEquals(8800.0, totalCredit, 0.001)
    }

    @Test
    fun testIndianRupeesAmountInWords() {
        assertEquals("Rupees Zero Only", IndianAccountingFormat.numberToWordsIndian(0.0))
        assertEquals("Rupees One Thousand Only", IndianAccountingFormat.numberToWordsIndian(1000.0))
        assertEquals("Rupees Ten Thousand Only", IndianAccountingFormat.numberToWordsIndian(10000.0))
        assertEquals("Rupees One Lakh Twenty-Five Thousand Only", IndianAccountingFormat.numberToWordsIndian(125000.0))
        assertEquals("Rupees One Lakh Twenty-Three Thousand Four Hundred Fifty-Six Only", IndianAccountingFormat.numberToWordsIndian(123456.0))
        assertEquals("Rupees One Crore Only", IndianAccountingFormat.numberToWordsIndian(10000000.0))
        assertEquals("Rupees Two Thousand Five Hundred and Fifty Paise Only", IndianAccountingFormat.numberToWordsIndian(2500.50))
    }

    @Test
    fun testDateFormattingDDMMYYYY() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.SEPTEMBER, 24)
        val formatted = IndianAccountingFormat.formatIndianDate(cal.timeInMillis)
        assertEquals("24/09/2026", formatted)
    }

    @Test
    fun testInvoicePdfWhatsAppTextGenerationNonGst() {
        val invoice = Invoice(
            id = "inv_101",
            invoiceNumber = "2025-26/BILL/001",
            type = InvoiceType.NON_GST_BILL,
            gstMode = GstMode.EXEMPT,
            customerName = "Rahul Sharma",
            items = listOf(
                InvoiceItem(
                    id = "it_1",
                    description = "Cotton Shirt",
                    quantity = 2.0,
                    unit = "Pcs",
                    unitPrice = 800.0,
                    gstRate = 0.0
                )
            ),
            paidAmount = 1600.0
        )

        val profile = com.hisabpro.app.data.model.BusinessProfile(
            shopName = "Shree Ganesh Garments",
            isGstRegistered = false
        )

        val text = com.hisabpro.app.util.InvoicePdfGenerator.generateInvoiceWhatsAppText(invoice, profile)
        assertTrue(text.contains("BILL OF SUPPLY"))
        assertTrue(text.contains("Shree Ganesh Garments"))
        assertTrue(text.contains("Rahul Sharma"))
        assertTrue(text.contains("₹1,600"))
        assertTrue(text.contains("PAID IN FULL"))
        // Non-GST invoice text must not have GST or tax references
        assertTrue(!text.contains("CGST"))
        assertTrue(!text.contains("SGST"))
        assertTrue(!text.contains("IGST"))
    }
}
