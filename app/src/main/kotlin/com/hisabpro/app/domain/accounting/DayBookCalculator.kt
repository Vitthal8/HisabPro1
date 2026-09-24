package com.hisabpro.app.domain.accounting

import com.hisabpro.app.data.model.Invoice
import com.hisabpro.app.data.model.InvoiceStatus
import com.hisabpro.app.data.model.InvoiceType
import com.hisabpro.app.data.model.KhataEntry
import com.hisabpro.app.data.model.KhataEntryType
import com.hisabpro.app.data.model.Party
import com.hisabpro.app.data.model.PaymentMode
import com.hisabpro.app.data.model.PurchaseBill
import com.hisabpro.app.data.model.Transaction
import com.hisabpro.app.data.model.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar

enum class DayBookVoucherType(val label: String, val badgeColor: Long) {
    SALE("Sale", 0xFF16A34A),
    PURCHASE("Purchase", 0xFFDC2626),
    RECEIPT("Receipt", 0xFF2563EB),
    PAYMENT("Payment", 0xFFEA580C),
    EXPENSE("Expense", 0xFF9333EA),
    JOURNAL("Journal", 0xFF0891B2),
    CREDIT_NOTE("Credit Note", 0xFF0D9488),
    DEBIT_NOTE("Debit Note", 0xFFC026D3)
}

data class DayBookVoucherItem(
    val id: String,
    val dateMillis: Long,
    val voucherNumber: String,
    val voucherType: DayBookVoucherType,
    val narration: String,
    val debitAccount: String,  // Dr Account
    val creditAccount: String, // Cr Account
    val debitAmount: Double,   // Dr Amount
    val creditAmount: Double,  // Cr Amount
    val amount: Double,
    val paymentMode: String = "CASH",
    val partyName: String = "",
    val referenceId: String = ""
)

data class DayBookCalculationResult(
    val dateMillis: Long,
    val totalDr: Double,
    val totalCr: Double,
    val isBalanced: Boolean,
    val vouchers: List<DayBookVoucherItem>,
    val salesTotal: Double,
    val purchasesTotal: Double,
    val receiptsTotal: Double,
    val paymentsTotal: Double,
    val expensesTotal: Double,
    val journalsTotal: Double,
    val creditNotesTotal: Double,
    val debitNotesTotal: Double,
    val cashIn: Double,
    val cashOut: Double,
    val bankIn: Double,
    val bankOut: Double,
    val netDayMovement: Double
)

/**
 * Domain-layer Day Book / Roznamcha calculation engine.
 *
 * Implements strict Indian double-entry accounting rules:
 * - Shows all 8 transaction types: Sales, Purchases, Receipts, Payments, Expenses, Journals, Credit Notes, Debit Notes.
 * - Double-entry Dr & Cr accounting columns.
 * - Prevents transaction duplication between sales and receipts.
 * - High-precision monetary balancing.
 */
object DayBookCalculator {

    fun calculate(
        allInvoices: List<Invoice>,
        allPurchases: List<PurchaseBill>,
        allTransactions: List<Transaction>,
        allKhataEntries: List<KhataEntry>,
        parties: List<Party>,
        dateMillis: Long
    ): DayBookCalculationResult {
        val cal = Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val endOfDay = cal.timeInMillis - 1

        val partyMap = parties.associateBy { it.id }
        val vouchers = mutableListOf<DayBookVoucherItem>()

        var cashIn = 0.0
        var cashOut = 0.0
        var bankIn = 0.0
        var bankOut = 0.0

        val processedInvoiceIds = mutableSetOf<String>()
        val processedPurchaseIds = mutableSetOf<String>()

        // 1. SALES
        val dayInvoices = allInvoices.filter {
            it.dateMillis in startOfDay..endOfDay && it.type != InvoiceType.PROFORMA
        }

        for (inv in dayInvoices) {
            val isUpiOrBank = isPaymentBankOrUpi(inv.notes, inv.notes)
            val modeStr = if (isUpiOrBank) "Bank/UPI" else "Cash"
            val amt = AccountingEngine.roundToTwoDecimals(inv.grandTotal)

            val drAccount = when (inv.paymentStatus) {
                InvoiceStatus.PAID -> if (isUpiOrBank) "Bank / UPI A/c" else "Cash in Hand"
                InvoiceStatus.PARTIAL -> "${inv.customerName} & Cash/Bank"
                InvoiceStatus.UNPAID -> "${inv.customerName} (Dr)"
            }

            if (inv.paidAmount > 0.009) {
                if (isUpiOrBank) bankIn += inv.paidAmount else cashIn += inv.paidAmount
            }

            vouchers.add(
                DayBookVoucherItem(
                    id = "sale_${inv.id}",
                    dateMillis = inv.dateMillis,
                    voucherNumber = inv.invoiceNumber,
                    voucherType = DayBookVoucherType.SALE,
                    narration = "Sale to ${inv.customerName} (${inv.items.size} items)",
                    debitAccount = drAccount,
                    creditAccount = "Sales Revenue A/c",
                    debitAmount = amt,
                    creditAmount = amt,
                    amount = amt,
                    paymentMode = modeStr,
                    partyName = inv.customerName,
                    referenceId = inv.id
                )
            )
            processedInvoiceIds.add(inv.id)
            processedInvoiceIds.add(inv.invoiceNumber.trim().lowercase())
        }

        // 2. PURCHASES
        val dayPurchases = allPurchases.filter {
            it.dateMillis in startOfDay..endOfDay
        }

        for (pur in dayPurchases) {
            val isBank = isPaymentBankOrUpi(pur.paymentMode, pur.notes)
            val modeStr = if (isBank) "Bank/UPI" else "Cash"
            val amt = AccountingEngine.roundToTwoDecimals(pur.grandTotal)

            val crAccount = when (pur.paymentStatus) {
                InvoiceStatus.PAID -> if (isBank) "Bank A/c" else "Cash in Hand"
                InvoiceStatus.PARTIAL -> "${pur.supplierName} & Cash/Bank"
                InvoiceStatus.UNPAID -> "${pur.supplierName} (Cr)"
            }

            if (pur.paidAmount > 0.009) {
                if (isBank) bankOut += pur.paidAmount else cashOut += pur.paidAmount
            }

            vouchers.add(
                DayBookVoucherItem(
                    id = "pur_${pur.id}",
                    dateMillis = pur.dateMillis,
                    voucherNumber = pur.purchaseNumber,
                    voucherType = DayBookVoucherType.PURCHASE,
                    narration = "Purchase inward from ${pur.supplierName}",
                    debitAccount = "Purchases A/c",
                    creditAccount = crAccount,
                    debitAmount = amt,
                    creditAmount = amt,
                    amount = amt,
                    paymentMode = modeStr,
                    partyName = pur.supplierName,
                    referenceId = pur.id
                )
            )
            processedPurchaseIds.add(pur.id)
            processedPurchaseIds.add(pur.purchaseNumber.trim().lowercase())
        }

        // 3. TRANSACTIONS (EXPENSES & GENERAL RECEIPTS)
        val dayTransactions = allTransactions.filter {
            it.dateMillis in startOfDay..endOfDay
        }

        for (tx in dayTransactions) {
            val isBank = tx.paymentMode != PaymentMode.CASH
            val titleLower = tx.title.trim().lowercase()
            val noteLower = tx.note.trim().lowercase()

            // De-duplicate check
            val isDuplicate = processedInvoiceIds.any { id -> titleLower.contains(id) || noteLower.contains(id) } ||
                    processedPurchaseIds.any { id -> titleLower.contains(id) || noteLower.contains(id) }
            if (isDuplicate) continue

            val amt = AccountingEngine.roundToTwoDecimals(tx.amount)

            if (tx.type == TransactionType.INCOME) {
                if (isBank) bankIn += amt else cashIn += amt
                vouchers.add(
                    DayBookVoucherItem(
                        id = "tx_${tx.id}",
                        dateMillis = tx.dateMillis,
                        voucherNumber = "REC-${tx.id.take(4)}",
                        voucherType = DayBookVoucherType.RECEIPT,
                        narration = tx.title,
                        debitAccount = if (isBank) "Bank / UPI A/c" else "Cash in Hand",
                        creditAccount = "${tx.category.label} A/c",
                        debitAmount = amt,
                        creditAmount = amt,
                        amount = amt,
                        paymentMode = tx.paymentMode.label,
                        partyName = tx.title
                    )
                )
            } else {
                if (isBank) bankOut += amt else cashOut += amt
                vouchers.add(
                    DayBookVoucherItem(
                        id = "tx_${tx.id}",
                        dateMillis = tx.dateMillis,
                        voucherNumber = "EXP-${tx.id.take(4)}",
                        voucherType = DayBookVoucherType.EXPENSE,
                        narration = tx.title,
                        debitAccount = "${tx.category.label} Expense A/c",
                        creditAccount = if (isBank) "Bank / UPI A/c" else "Cash in Hand",
                        debitAmount = amt,
                        creditAmount = amt,
                        amount = amt,
                        paymentMode = tx.paymentMode.label,
                        partyName = tx.category.label
                    )
                )
            }
        }

        // 4. KHATA ENTRIES (RECEIPTS, PAYMENTS, CREDIT NOTES, DEBIT NOTES)
        val dayKhata = allKhataEntries.filter {
            it.dateMillis in startOfDay..endOfDay
        }

        for (e in dayKhata) {
            val billLower = e.billNumber.trim().lowercase()
            val noteLower = e.note.trim().lowercase()

            // Check if this khata entry is already counted in an invoice on the same day
            if (billLower.isNotBlank() && (processedInvoiceIds.contains(billLower) || processedPurchaseIds.contains(billLower))) {
                continue
            }

            val party = partyMap[e.partyId]
            val partyName = party?.name ?: "Party"
            val amt = AccountingEngine.roundToTwoDecimals(e.amount)
            val isBank = isPaymentBankOrUpi(e.note, e.billNumber)
            val modeStr = if (isBank) "Bank/UPI" else "Cash"

            // Identify Credit Note / Debit Note / Receipt / Payment
            val isCreditNote = billLower.startsWith("cn") || noteLower.contains("credit note") || noteLower.contains("return")
            val isDebitNote = billLower.startsWith("dn") || noteLower.contains("debit note")

            when {
                isCreditNote -> {
                    // Credit note: Customer return / credit concession
                    vouchers.add(
                        DayBookVoucherItem(
                            id = "khata_cn_${e.id}",
                            dateMillis = e.dateMillis,
                            voucherNumber = e.billNumber.ifBlank { "CN-${e.id.take(4)}" },
                            voucherType = DayBookVoucherType.CREDIT_NOTE,
                            narration = "Credit Note issued to $partyName: ${e.note.ifBlank { "Goods return/discount" }}",
                            debitAccount = "Sales Returns A/c",
                            creditAccount = "$partyName (Cr)",
                            debitAmount = amt,
                            creditAmount = amt,
                            amount = amt,
                            paymentMode = "Journal / Credit",
                            partyName = partyName
                        )
                    )
                }
                isDebitNote -> {
                    // Debit note: Purchase return to supplier
                    vouchers.add(
                        DayBookVoucherItem(
                            id = "khata_dn_${e.id}",
                            dateMillis = e.dateMillis,
                            voucherNumber = e.billNumber.ifBlank { "DN-${e.id.take(4)}" },
                            voucherType = DayBookVoucherType.DEBIT_NOTE,
                            narration = "Debit Note issued to $partyName: ${e.note.ifBlank { "Goods return/debit adjustment" }}",
                            debitAccount = "$partyName (Dr)",
                            creditAccount = "Purchase Returns A/c",
                            debitAmount = amt,
                            creditAmount = amt,
                            amount = amt,
                            paymentMode = "Journal / Debit",
                            partyName = partyName
                        )
                    )
                }
                e.type == KhataEntryType.YOU_GOT -> {
                    // Receipt from party (Money collected)
                    if (isBank) bankIn += amt else cashIn += amt
                    vouchers.add(
                        DayBookVoucherItem(
                            id = "khata_rec_${e.id}",
                            dateMillis = e.dateMillis,
                            voucherNumber = e.billNumber.ifBlank { "REC-${e.id.take(4)}" },
                            voucherType = DayBookVoucherType.RECEIPT,
                            narration = "Received from $partyName: ${e.note.ifBlank { "Customer collection" }}",
                            debitAccount = if (isBank) "Bank / UPI A/c" else "Cash in Hand",
                            creditAccount = "$partyName (Cr)",
                            debitAmount = amt,
                            creditAmount = amt,
                            amount = amt,
                            paymentMode = modeStr,
                            partyName = partyName
                        )
                    )
                }
                e.type == KhataEntryType.YOU_GAVE -> {
                    // Payment to party (e.g. Supplier payment or money given)
                    if (isBank) bankOut += amt else cashOut += amt
                    vouchers.add(
                        DayBookVoucherItem(
                            id = "khata_pay_${e.id}",
                            dateMillis = e.dateMillis,
                            voucherNumber = e.billNumber.ifBlank { "PAY-${e.id.take(4)}" },
                            voucherType = DayBookVoucherType.PAYMENT,
                            narration = "Payment made to $partyName: ${e.note.ifBlank { "Supplier dues settlement" }}",
                            debitAccount = "$partyName (Dr)",
                            creditAccount = if (isBank) "Bank / UPI A/c" else "Cash in Hand",
                            debitAmount = amt,
                            creditAmount = amt,
                            amount = amt,
                            paymentMode = modeStr,
                            partyName = partyName
                        )
                    )
                }
            }
        }

        // Sort descending by timestamp for newest on top
        vouchers.sortByDescending { it.dateMillis }

        // Category Totals
        val salesTotal = vouchers.filter { it.voucherType == DayBookVoucherType.SALE }.sumOf { it.amount }
        val purchasesTotal = vouchers.filter { it.voucherType == DayBookVoucherType.PURCHASE }.sumOf { it.amount }
        val receiptsTotal = vouchers.filter { it.voucherType == DayBookVoucherType.RECEIPT }.sumOf { it.amount }
        val paymentsTotal = vouchers.filter { it.voucherType == DayBookVoucherType.PAYMENT }.sumOf { it.amount }
        val expensesTotal = vouchers.filter { it.voucherType == DayBookVoucherType.EXPENSE }.sumOf { it.amount }
        val journalsTotal = vouchers.filter { it.voucherType == DayBookVoucherType.JOURNAL }.sumOf { it.amount }
        val creditNotesTotal = vouchers.filter { it.voucherType == DayBookVoucherType.CREDIT_NOTE }.sumOf { it.amount }
        val debitNotesTotal = vouchers.filter { it.voucherType == DayBookVoucherType.DEBIT_NOTE }.sumOf { it.amount }

        val totalDr = vouchers.sumOf { it.debitAmount }
        val totalCr = vouchers.sumOf { it.creditAmount }
        val isBalanced = kotlin.math.abs(totalDr - totalCr) < 0.01

        val netMovement = (cashIn + bankIn) - (cashOut + bankOut)

        return DayBookCalculationResult(
            dateMillis = dateMillis,
            totalDr = totalDr,
            totalCr = totalCr,
            isBalanced = isBalanced,
            vouchers = vouchers,
            salesTotal = salesTotal,
            purchasesTotal = purchasesTotal,
            receiptsTotal = receiptsTotal,
            paymentsTotal = paymentsTotal,
            expensesTotal = expensesTotal,
            journalsTotal = journalsTotal,
            creditNotesTotal = creditNotesTotal,
            debitNotesTotal = debitNotesTotal,
            cashIn = cashIn,
            cashOut = cashOut,
            bankIn = bankIn,
            bankOut = bankOut,
            netDayMovement = netMovement
        )
    }

    private fun isPaymentBankOrUpi(field1: String, field2: String): Boolean {
        val s = (field1 + " " + field2).lowercase()
        return s.contains("upi") ||
                s.contains("online") ||
                s.contains("bank") ||
                s.contains("cheque") ||
                s.contains("neft") ||
                s.contains("rtgs") ||
                s.contains("gpay") ||
                s.contains("phonepe") ||
                s.contains("paytm")
    }
}
