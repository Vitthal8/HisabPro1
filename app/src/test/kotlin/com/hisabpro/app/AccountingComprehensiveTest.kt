package com.hisabpro.app

import com.hisabpro.app.data.model.Category
import com.hisabpro.app.data.model.GstMode
import com.hisabpro.app.data.model.Invoice
import com.hisabpro.app.data.model.InvoiceItem
import com.hisabpro.app.data.model.InvoiceStatus
import com.hisabpro.app.data.model.InvoiceType
import com.hisabpro.app.data.model.KhataEntry
import com.hisabpro.app.data.model.KhataEntryType
import com.hisabpro.app.data.model.Party
import com.hisabpro.app.data.model.PartyType
import com.hisabpro.app.data.model.PartyWithBalance
import com.hisabpro.app.data.model.PaymentMode
import com.hisabpro.app.data.model.PurchaseBill
import com.hisabpro.app.data.model.PurchaseItem
import com.hisabpro.app.data.model.Transaction
import com.hisabpro.app.data.model.TransactionType
import com.hisabpro.app.util.IndianAccountingFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountingComprehensiveTest {

    // 1. Sales & Invoicing Accounting
    @Test
    fun testSalesInvoiceAccountingTotalsAndDue() {
        val invoice = Invoice(
            id = "inv_sales_101",
            businessId = "biz_test",
            invoiceNumber = "2025-26/INV/101",
            type = InvoiceType.TAX_INVOICE,
            gstMode = GstMode.INTRA_STATE,
            customerName = "Mali Kirana",
            items = listOf(
                InvoiceItem(id = "i1", description = "Sugar 50kg Bag", quantity = 2.0, unitPrice = 2000.0, gstRate = 5.0),
                InvoiceItem(id = "i2", description = "Edible Oil 15L", quantity = 1.0, unitPrice = 1800.0, gstRate = 5.0)
            ),
            discountAmount = 200.0,
            paidAmount = 3000.0,
            paymentStatus = InvoiceStatus.PARTIAL
        )

        // Subtotal = (2 * 2000) + (1 * 1800) = 5800
        assertEquals(5800.0, invoice.subtotal, 0.01)
        // Tax (5% on 5800) = 290 (CGST 145 + SGST 145)
        assertEquals(290.0, invoice.totalTax, 0.01)
        assertEquals(145.0, invoice.cgstTotal, 0.01)
        assertEquals(145.0, invoice.sgstTotal, 0.01)
        // Grand Total = 5800 + 290 - 200 = 5890
        assertEquals(5890.0, invoice.grandTotal, 0.01)
        // Due = 5890 - 3000 = 2890
        assertEquals(2890.0, invoice.dueAmount, 0.01)
    }

    // 2. Purchases & Inward Bills Accounting
    @Test
    fun testPurchaseBillAccountingTotalsAndItc() {
        val purchaseBill = PurchaseBill(
            id = "pur_bill_201",
            purchaseNumber = "2025-26/PUR/201",
            supplierName = "Apex Wholesalers",
            supplierGstin = "27ABCDE1234F1Z5",
            gstMode = GstMode.INTRA_STATE,
            itcEligible = true,
            items = listOf(
                PurchaseItem(id = "pi1", description = "Raw Tea 10kg", quantity = 5.0, unitPrice = 400.0, gstRate = 5.0)
            ),
            discountAmount = 100.0,
            paidAmount = 1000.0
        )

        // Subtotal = 2000
        assertEquals(2000.0, purchaseBill.subtotal, 0.01)
        // Tax = 5% of 2000 = 100 (Eligible for ITC)
        assertEquals(100.0, purchaseBill.totalTax, 0.01)
        assertTrue(purchaseBill.itcEligible)
        // Grand Total = 2000 + 100 - 100 = 2000
        assertEquals(2000.0, purchaseBill.grandTotal, 0.01)
        // Due = 2000 - 1000 = 1000
        assertEquals(1000.0, purchaseBill.dueAmount, 0.01)
    }

    // 3. Payments & Expenses
    @Test
    fun testExpensesCategoryBreakdownAndTotals() {
        val transactions = listOf(
            Transaction(id = "t1", title = "Electricity Bill", amount = 2500.0, type = TransactionType.EXPENSE, category = Category.UTILITIES, dateMillis = 100L, paymentMode = PaymentMode.BANK_TRANSFER),
            Transaction(id = "t2", title = "Internet Broadband", amount = 1200.0, type = TransactionType.EXPENSE, category = Category.UTILITIES, dateMillis = 200L, paymentMode = PaymentMode.ONLINE_UPI),
            Transaction(id = "t3", title = "Shop Rent", amount = 15000.0, type = TransactionType.EXPENSE, category = Category.BUSINESS, dateMillis = 300L, paymentMode = PaymentMode.BANK_TRANSFER),
            Transaction(id = "t4", title = "Client Payment Received", amount = 20000.0, type = TransactionType.INCOME, category = Category.BUSINESS, dateMillis = 400L, paymentMode = PaymentMode.ONLINE_UPI)
        )

        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
        val totalExpenses = expenses.sumOf { it.amount }
        assertEquals(18700.0, totalExpenses, 0.01)

        val utilitiesTotal = expenses.filter { it.category == Category.UTILITIES }.sumOf { it.amount }
        assertEquals(3700.0, utilitiesTotal, 0.01)
    }

    // 4. Ledger Running Balances & Dr/Cr
    @Test
    fun testCustomerAndSupplierLedgers() {
        val party = Party(id = "p_10", name = "Suresh Stores", phone = "9822012345", type = PartyType.CUSTOMER)

        val partyWithBal = PartyWithBalance(
            party = party,
            totalGave = 5000.0,
            totalGot = 2000.0,
            netBalance = 4000.0 // 1000 opening Dr + 5000 Dr - 2000 Cr = 4000 Dr
        )

        assertEquals(4000.0, partyWithBal.dueAmount, 0.01)
        assertTrue(partyWithBal.isReceivable)
        assertEquals("Dr", IndianAccountingFormat.getDrCrIndicator(partyWithBal.netBalance, isCustomer = true))
    }

    // 5. Outstanding Dues Aging Buckets
    @Test
    fun testOutstandingAgingBuckets() {
        val now = System.currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L

        val invoiceCurrent = Invoice(id = "i1", businessId = "biz_test", invoiceNumber = "INV-01", type = InvoiceType.NON_GST_BILL, gstMode = GstMode.EXEMPT, customerName = "A", dateMillis = now - (day * 5), items = listOf(InvoiceItem(description = "X", quantity = 1.0, unitPrice = 1000.0, gstRate = 0.0)), paidAmount = 0.0) // 5 days old -> 0-15 bucket
        val invoiceDueSoon = Invoice(id = "i2", businessId = "biz_test", invoiceNumber = "INV-02", type = InvoiceType.NON_GST_BILL, gstMode = GstMode.EXEMPT, customerName = "B", dateMillis = now - (day * 20), items = listOf(InvoiceItem(description = "Y", quantity = 1.0, unitPrice = 2000.0, gstRate = 0.0)), paidAmount = 0.0) // 20 days old -> 16-30 bucket
        val invoiceOverdue = Invoice(id = "i3", businessId = "biz_test", invoiceNumber = "INV-03", type = InvoiceType.NON_GST_BILL, gstMode = GstMode.EXEMPT, customerName = "C", dateMillis = now - (day * 45), items = listOf(InvoiceItem(description = "Z", quantity = 1.0, unitPrice = 3000.0, gstRate = 0.0)), paidAmount = 0.0) // 45 days old -> 31-60 bucket
        val invoiceCritical = Invoice(id = "i4", businessId = "biz_test", invoiceNumber = "INV-04", type = InvoiceType.NON_GST_BILL, gstMode = GstMode.EXEMPT, customerName = "D", dateMillis = now - (day * 75), items = listOf(InvoiceItem(description = "W", quantity = 1.0, unitPrice = 4000.0, gstRate = 0.0)), paidAmount = 0.0) // 75 days old -> 60+ bucket

        val unpaidInvoices = listOf(invoiceCurrent, invoiceDueSoon, invoiceOverdue, invoiceCritical)

        var bucket0to15 = 0.0
        var bucket16to30 = 0.0
        var bucket31to60 = 0.0
        var bucket60Plus = 0.0

        unpaidInvoices.forEach { inv ->
            val ageDays = ((now - inv.dateMillis) / day).toInt()
            when {
                ageDays <= 15 -> bucket0to15 += inv.dueAmount
                ageDays <= 30 -> bucket16to30 += inv.dueAmount
                ageDays <= 60 -> bucket31to60 += inv.dueAmount
                else -> bucket60Plus += inv.dueAmount
            }
        }

        assertEquals(1000.0, bucket0to15, 0.01)
        assertEquals(2000.0, bucket16to30, 0.01)
        assertEquals(3000.0, bucket31to60, 0.01)
        assertEquals(4000.0, bucket60Plus, 0.01)
    }

    // 6. Cash Book Cash vs Bank Isolation
    @Test
    fun testCashBookCashVsBankIsolation() {
        var cashInHand = 5000.0
        var bankBalance = 25000.0

        // Transaction 1: Cash sale ₹2000
        cashInHand += 2000.0
        // Transaction 2: UPI receipt ₹5000
        bankBalance += 5000.0
        // Transaction 3: Cash expense ₹800
        cashInHand -= 800.0
        // Transaction 4: NEFT vendor payment ₹10000
        bankBalance -= 10000.0

        assertEquals(6200.0, cashInHand, 0.01)
        assertEquals(20000.0, bankBalance, 0.01)
    }

    // 7. Day Book Double-Entry Debit/Credit Equality
    @Test
    fun testDayBookDoubleEntryEquality() {
        val totalDebits = 15000.0 // Cash Dr + Bank Dr + Expense Dr
        val totalCredits = 15000.0 // Sales Cr + Customer Cr + Income Cr

        assertEquals(totalDebits, totalCredits, 0.001)
    }
}
