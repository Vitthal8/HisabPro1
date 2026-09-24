package com.hisabpro.app.domain.accounting

import com.hisabpro.app.data.model.Invoice
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

data class CashBookRow(
    val id: String,
    val dateMillis: Long,
    val particulars: String,
    val voucherNo: String,
    val voucherType: String,
    val receiptAmount: Double, // Receipt / Inflow / जमा
    val paymentAmount: Double, // Payment / Outflow / नावे
    val runningBalance: Double,
    val paymentMode: String,
    val referenceInfo: String = ""
)

data class CashBookCalculationResult(
    val isBankMode: Boolean,
    val openingBalance: Double,
    val totalReceipts: Double,
    val totalPayments: Double,
    val netMovement: Double,
    val closingBalance: Double,
    val rows: List<CashBookRow>
)

/**
 * Domain-layer Cash Book & Bank Book calculation engine.
 *
 * Requirements:
 * - Columns: Date, Particulars, Receipt, Payment, Balance
 * - Separate cash transactions from bank transactions
 * - De-duplicate transactions (prevent counting invoice paid amount and payment record twice)
 * - Calculate balances with double-precision accuracy
 */
object CashBookCalculator {

    fun calculate(
        invoices: List<Invoice>,
        purchases: List<PurchaseBill>,
        transactions: List<Transaction>,
        khataEntries: List<KhataEntry>,
        parties: List<Party>,
        isBankMode: Boolean,
        startDateMillis: Long,
        endDateMillis: Long
    ): CashBookCalculationResult {
        val partyMap = parties.associateBy { it.id }
        val rawRows = mutableListOf<CashBookRow>()

        // Keep track of invoice numbers and purchase numbers already accounted for
        val processedInvoiceIds = mutableSetOf<String>()
        val processedPurchaseIds = mutableSetOf<String>()

        // 1. Invoices (Sales with immediate payment)
        for (inv in invoices) {
            if (inv.type == InvoiceType.PROFORMA) continue
            if (inv.paidAmount > 0.009) {
                val isBank = isPaymentBankOrUpi(inv.notes, inv.notes)
                if (isBank == isBankMode) {
                    val rowDate = inv.dateMillis
                    rawRows.add(
                        CashBookRow(
                            id = "inv_${inv.id}",
                            dateMillis = rowDate,
                            particulars = "Sale: ${inv.customerName}",
                            voucherNo = inv.invoiceNumber,
                            voucherType = "Sale",
                            receiptAmount = AccountingEngine.roundToTwoDecimals(inv.paidAmount),
                            paymentAmount = 0.0,
                            runningBalance = 0.0,
                            paymentMode = if (isBankMode) "Bank / UPI" else "Cash",
                            referenceInfo = "Invoice #${inv.invoiceNumber}"
                        )
                    )
                    processedInvoiceIds.add(inv.id)
                    processedInvoiceIds.add(inv.invoiceNumber.trim().lowercase())
                }
            }
        }

        // 2. Purchases (Purchase bills with immediate payment)
        for (pur in purchases) {
            if (pur.paidAmount > 0.009) {
                val isBank = isPaymentBankOrUpi(pur.paymentMode, pur.notes)
                if (isBank == isBankMode) {
                    val rowDate = pur.dateMillis
                    rawRows.add(
                        CashBookRow(
                            id = "pur_${pur.id}",
                            dateMillis = rowDate,
                            particulars = "Purchase: ${pur.supplierName}",
                            voucherNo = pur.purchaseNumber,
                            voucherType = "Purchase",
                            receiptAmount = 0.0,
                            paymentAmount = AccountingEngine.roundToTwoDecimals(pur.paidAmount),
                            runningBalance = 0.0,
                            paymentMode = pur.paymentMode,
                            referenceInfo = "Purchase Bill #${pur.purchaseNumber}"
                        )
                    )
                    processedPurchaseIds.add(pur.id)
                    processedPurchaseIds.add(pur.purchaseNumber.trim().lowercase())
                }
            }
        }

        // 3. Transactions (General Income / Expense)
        for (tx in transactions) {
            val isBank = tx.paymentMode != PaymentMode.CASH
            if (isBank != isBankMode) continue

            // De-duplicate: check if transaction was auto-created from an already processed invoice or purchase
            val titleLower = tx.title.trim().lowercase()
            val noteLower = tx.note.trim().lowercase()
            val isDuplicateInvoice = processedInvoiceIds.any { id ->
                titleLower.contains(id) || noteLower.contains(id)
            }
            val isDuplicatePurchase = processedPurchaseIds.any { id ->
                titleLower.contains(id) || noteLower.contains(id)
            }
            if (isDuplicateInvoice || isDuplicatePurchase) continue

            if (tx.type == TransactionType.INCOME) {
                rawRows.add(
                    CashBookRow(
                        id = "tx_${tx.id}",
                        dateMillis = tx.dateMillis,
                        particulars = "${tx.category.label}: ${tx.title}",
                        voucherNo = "REC-${tx.id.take(4)}",
                        voucherType = "Receipt",
                        receiptAmount = AccountingEngine.roundToTwoDecimals(tx.amount),
                        paymentAmount = 0.0,
                        runningBalance = 0.0,
                        paymentMode = tx.paymentMode.label,
                        referenceInfo = tx.note
                    )
                )
            } else {
                rawRows.add(
                    CashBookRow(
                        id = "tx_${tx.id}",
                        dateMillis = tx.dateMillis,
                        particulars = "${tx.category.label}: ${tx.title}",
                        voucherNo = "EXP-${tx.id.take(4)}",
                        voucherType = "Expense",
                        receiptAmount = 0.0,
                        paymentAmount = AccountingEngine.roundToTwoDecimals(tx.amount),
                        runningBalance = 0.0,
                        paymentMode = tx.paymentMode.label,
                        referenceInfo = tx.note
                    )
                )
            }
        }

        // 4. Khata Entries (Independent collections / payments from parties)
        for (e in khataEntries) {
            // De-duplicate: if billNumber matches an invoice already counted on same day, skip
            if (e.billNumber.isNotBlank()) {
                val billLower = e.billNumber.trim().lowercase()
                if (processedInvoiceIds.contains(billLower) || processedPurchaseIds.contains(billLower)) {
                    continue
                }
            }

            val party = partyMap[e.partyId]
            val partyName = party?.name ?: "Party"

            // Infer payment mode from note if present
            val isBank = isPaymentBankOrUpi(e.note, e.billNumber)
            if (isBank != isBankMode) continue

            if (e.type == KhataEntryType.YOU_GOT) {
                // Receipt from party (Money collected) -> Receipt / Inflow
                rawRows.add(
                    CashBookRow(
                        id = "khata_${e.id}",
                        dateMillis = e.dateMillis,
                        particulars = "Received from $partyName",
                        voucherNo = e.billNumber.ifBlank { "REC-${e.id.take(4)}" },
                        voucherType = "Receipt",
                        receiptAmount = AccountingEngine.roundToTwoDecimals(e.amount),
                        paymentAmount = 0.0,
                        runningBalance = 0.0,
                        paymentMode = if (isBankMode) "Bank / UPI" else "Cash",
                        referenceInfo = e.note
                    )
                )
            } else {
                // Payment to party (e.g. Supplier paid in cash) -> Payment / Outflow
                rawRows.add(
                    CashBookRow(
                        id = "khata_${e.id}",
                        dateMillis = e.dateMillis,
                        particulars = "Paid to $partyName",
                        voucherNo = e.billNumber.ifBlank { "PAY-${e.id.take(4)}" },
                        voucherType = "Payment",
                        receiptAmount = 0.0,
                        paymentAmount = AccountingEngine.roundToTwoDecimals(e.amount),
                        runningBalance = 0.0,
                        paymentMode = if (isBankMode) "Bank / UPI" else "Cash",
                        referenceInfo = e.note
                    )
                )
            }
        }

        // Calculate Opening Balance from all transactions occurring BEFORE startDateMillis
        val priorRows = rawRows.filter { it.dateMillis < startDateMillis }
        val openingReceipts = priorRows.sumOf { it.receiptAmount }
        val openingPayments = priorRows.sumOf { it.paymentAmount }
        val openingBalance = AccountingEngine.roundToTwoDecimals(openingReceipts - openingPayments)

        // Filter rows in current period
        val periodRows = rawRows.filter { it.dateMillis in startDateMillis..endDateMillis }
            .sortedBy { it.dateMillis } // Chronological ascending for running balance

        var running = BigDecimal(openingBalance.toString())
        var periodTotalReceipts = BigDecimal.ZERO
        var periodTotalPayments = BigDecimal.ZERO

        val finalizedRows = mutableListOf<CashBookRow>()

        for (row in periodRows) {
            val r = BigDecimal(row.receiptAmount.toString())
            val p = BigDecimal(row.paymentAmount.toString())

            periodTotalReceipts = periodTotalReceipts.add(r)
            periodTotalPayments = periodTotalPayments.add(p)
            running = running.add(r).subtract(p)

            finalizedRows.add(
                row.copy(runningBalance = running.setScale(2, RoundingMode.HALF_UP).toDouble())
            )
        }

        val totalReceipts = periodTotalReceipts.setScale(2, RoundingMode.HALF_UP).toDouble()
        val totalPayments = periodTotalPayments.setScale(2, RoundingMode.HALF_UP).toDouble()
        val netMovement = AccountingEngine.roundToTwoDecimals(totalReceipts - totalPayments)
        val closingBalance = running.setScale(2, RoundingMode.HALF_UP).toDouble()

        return CashBookCalculationResult(
            isBankMode = isBankMode,
            openingBalance = openingBalance,
            totalReceipts = totalReceipts,
            totalPayments = totalPayments,
            netMovement = netMovement,
            closingBalance = closingBalance,
            rows = finalizedRows.reversed() // Return newest at top for viewing convenience
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
