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
import com.hisabpro.app.util.toPaise
import com.hisabpro.app.util.toRupees

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
) {
    val receiptAmountPaise: Long get() = receiptAmount.toPaise()
    val paymentAmountPaise: Long get() = paymentAmount.toPaise()
    val runningBalancePaise: Long get() = runningBalance.toPaise()
}

data class CashBookCalculationResult(
    val isBankMode: Boolean,
    val openingBalance: Double,
    val totalReceipts: Double,
    val totalPayments: Double,
    val netMovement: Double,
    val closingBalance: Double,
    val rows: List<CashBookRow>
) {
    val openingBalancePaise: Long get() = openingBalance.toPaise()
    val totalReceiptsPaise: Long get() = totalReceipts.toPaise()
    val totalPaymentsPaise: Long get() = totalPayments.toPaise()
    val netMovementPaise: Long get() = netMovement.toPaise()
    val closingBalancePaise: Long get() = closingBalance.toPaise()
}

/**
 * Domain-layer Cash Book & Bank Book calculation engine.
 *
 * Requirements:
 * - Columns: Date, Particulars, Receipt, Payment, Balance
 * - Separate cash transactions from bank transactions
 * - De-duplicate transactions (prevent counting invoice paid amount and payment record twice)
 * - Calculate running balance with Long paise / BigDecimal accuracy without floating-point accumulation drift.
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

        val processedInvoiceIds = mutableSetOf<String>()
        val processedPurchaseIds = mutableSetOf<String>()

        // 1. Invoices (Sales with immediate payment)
        for (inv in invoices) {
            if (inv.type == InvoiceType.PROFORMA) continue
            val paidPaise = inv.paidAmount.toPaise()
            if (paidPaise > 0L) {
                val isBank = isPaymentBankOrUpi(inv.paymentMode, inv.notes)
                if (isBank == isBankMode) {
                    val rowDate = inv.dateMillis
                    val amtRupees = paidPaise.toRupees()
                    rawRows.add(
                        CashBookRow(
                            id = "inv_${inv.id}",
                            dateMillis = rowDate,
                            particulars = "Sale: ${inv.customerName}",
                            voucherNo = inv.invoiceNumber,
                            voucherType = "Sale",
                            receiptAmount = amtRupees,
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
            val paidPaise = pur.paidAmount.toPaise()
            if (paidPaise > 0L) {
                val isBank = isPaymentBankOrUpi(pur.paymentMode, pur.notes)
                if (isBank == isBankMode) {
                    val rowDate = pur.dateMillis
                    val amtRupees = paidPaise.toRupees()
                    rawRows.add(
                        CashBookRow(
                            id = "pur_${pur.id}",
                            dateMillis = rowDate,
                            particulars = "Purchase: ${pur.supplierName}",
                            voucherNo = pur.purchaseNumber,
                            voucherType = "Purchase",
                            receiptAmount = 0.0,
                            paymentAmount = amtRupees,
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

            val titleLower = tx.title.trim().lowercase()
            val noteLower = tx.note.trim().lowercase()
            val isDuplicateInvoice = processedInvoiceIds.any { id ->
                titleLower.contains(id) || noteLower.contains(id)
            }
            val isDuplicatePurchase = processedPurchaseIds.any { id ->
                titleLower.contains(id) || noteLower.contains(id)
            }
            if (isDuplicateInvoice || isDuplicatePurchase) continue

            val amtRupees = tx.amount.toPaise().toRupees()

            if (tx.type == TransactionType.INCOME) {
                rawRows.add(
                    CashBookRow(
                        id = "tx_${tx.id}",
                        dateMillis = tx.dateMillis,
                        particulars = "${tx.category.label}: ${tx.title}",
                        voucherNo = "REC-${tx.id.take(4)}",
                        voucherType = "Receipt",
                        receiptAmount = amtRupees,
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
                        paymentAmount = amtRupees,
                        runningBalance = 0.0,
                        paymentMode = tx.paymentMode.label,
                        referenceInfo = tx.note
                    )
                )
            }
        }

        // 4. Khata Entries (Independent collections / payments from parties)
        for (e in khataEntries) {
            if (e.billNumber.isNotBlank()) {
                val billLower = e.billNumber.trim().lowercase()
                if (processedInvoiceIds.contains(billLower) || processedPurchaseIds.contains(billLower)) {
                    continue
                }
            }

            val party = partyMap[e.partyId]
            val partyName = party?.name ?: "Party"

            val isBank = isPaymentBankOrUpi(e.note, e.billNumber)
            if (isBank != isBankMode) continue

            val amtRupees = e.amount.toPaise().toRupees()

            if (e.type == KhataEntryType.YOU_GOT) {
                rawRows.add(
                    CashBookRow(
                        id = "khata_${e.id}",
                        dateMillis = e.dateMillis,
                        particulars = "Received from $partyName",
                        voucherNo = e.billNumber.ifBlank { "REC-${e.id.take(4)}" },
                        voucherType = "Receipt",
                        receiptAmount = amtRupees,
                        paymentAmount = 0.0,
                        runningBalance = 0.0,
                        paymentMode = if (isBankMode) "Bank / UPI" else "Cash",
                        referenceInfo = e.note
                    )
                )
            } else {
                rawRows.add(
                    CashBookRow(
                        id = "khata_${e.id}",
                        dateMillis = e.dateMillis,
                        particulars = "Paid to $partyName",
                        voucherNo = e.billNumber.ifBlank { "PAY-${e.id.take(4)}" },
                        voucherType = "Payment",
                        receiptAmount = 0.0,
                        paymentAmount = amtRupees,
                        runningBalance = 0.0,
                        paymentMode = if (isBankMode) "Bank / UPI" else "Cash",
                        referenceInfo = e.note
                    )
                )
            }
        }

        // Calculate Opening Balance in Long paise
        val priorRows = rawRows.filter { it.dateMillis < startDateMillis }
        val openingReceiptsPaise = priorRows.sumOf { it.receiptAmount.toPaise() }
        val openingPaymentsPaise = priorRows.sumOf { it.paymentAmount.toPaise() }
        val openingBalancePaise = openingReceiptsPaise - openingPaymentsPaise

        val periodRows = rawRows.filter { it.dateMillis in startDateMillis..endDateMillis }
            .sortedBy { it.dateMillis }

        var runningPaise = openingBalancePaise
        var periodTotalReceiptsPaise = 0L
        var periodTotalPaymentsPaise = 0L

        val finalizedRows = mutableListOf<CashBookRow>()

        for (row in periodRows) {
            val rPaise = row.receiptAmount.toPaise()
            val pPaise = row.paymentAmount.toPaise()

            periodTotalReceiptsPaise += rPaise
            periodTotalPaymentsPaise += pPaise
            runningPaise += (rPaise - pPaise)

            finalizedRows.add(
                row.copy(runningBalance = runningPaise.toRupees())
            )
        }

        val netMovementPaise = periodTotalReceiptsPaise - periodTotalPaymentsPaise
        val closingBalancePaise = openingBalancePaise + netMovementPaise

        return CashBookCalculationResult(
            isBankMode = isBankMode,
            openingBalance = openingBalancePaise.toRupees(),
            totalReceipts = periodTotalReceiptsPaise.toRupees(),
            totalPayments = periodTotalPaymentsPaise.toRupees(),
            netMovement = netMovementPaise.toRupees(),
            closingBalance = closingBalancePaise.toRupees(),
            rows = finalizedRows
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
                s.contains("card")
    }
}
