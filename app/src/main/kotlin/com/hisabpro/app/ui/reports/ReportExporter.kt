package com.hisabpro.app.ui.reports

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.hisabpro.app.data.model.Item
import com.hisabpro.app.data.model.PurchaseBill
import com.hisabpro.app.data.model.StockHistoryEntry
import com.hisabpro.app.data.repository.SettingsRepository
import com.hisabpro.app.ui.HisabViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportExporter {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
    private val timeFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH)

    fun shareGstr1Report(
        context: Context,
        gstr: Gstr1Summary,
        period: ReportPeriod,
        businessName: String = SettingsRepository.getInstance(context).profile.value.shopName.ifBlank { "HisabPro Store" },
        gstin: String = SettingsRepository.getInstance(context).profile.value.gstin
    ) {
        val totalTaxableStr = HisabViewModel.formatAmount(gstr.totalTaxableSupplies)
        val totalTaxStr = HisabViewModel.formatAmount(gstr.totalTax)
        val cgstStr = HisabViewModel.formatAmount(gstr.totalCgst)
        val sgstStr = HisabViewModel.formatAmount(gstr.totalSgst)
        val igstStr = HisabViewModel.formatAmount(gstr.totalIgst)
        val grandTotalStr = HisabViewModel.formatAmount(gstr.totalInvoiceValue)

        val sb = StringBuilder()
        sb.append("📋 *GSTR-1 TAX FILING SUMMARY*\n")
        sb.append("🏢 Business: $businessName\n")
        if (gstin.isNotBlank()) sb.append("🆔 GSTIN: $gstin\n")
        sb.append("📅 Period: ${period.label}\n")
        sb.append("🕒 Generated: ${timeFormat.format(Date())}\n\n")

        sb.append("📊 *SALES & TAX TOTALS*\n")
        sb.append("• Total Taxable Turnover: ₹$totalTaxableStr\n")
        sb.append("• CGST Output Tax: ₹$cgstStr\n")
        sb.append("• SGST Output Tax: ₹$sgstStr\n")
        sb.append("• IGST Output Tax: ₹$igstStr\n")
        sb.append("• *Total Tax Collected: ₹$totalTaxStr*\n")
        sb.append("• *Total Invoice Value: ₹$grandTotalStr*\n\n")

        sb.append("👥 *B2B vs B2C INVOICES*\n")
        sb.append("• B2B (With Party GSTIN): ${gstr.b2bCount} bills (₹${HisabViewModel.formatAmount(gstr.b2bTaxable)} Taxable)\n")
        sb.append("• B2C (Consumer Retail): ${gstr.b2cCount} bills (₹${HisabViewModel.formatAmount(gstr.b2cTaxable)} Taxable)\n\n")

        sb.append("🏷️ *GST SLAB BREAKDOWN*\n")
        gstr.slabSummaries.filter { it.taxableAmount > 0.0 }.forEach { slab ->
            sb.append("• ${slab.gstRate.toInt()}% Slab: Taxable ₹${HisabViewModel.formatAmount(slab.taxableAmount)} | Tax ₹${HisabViewModel.formatAmount(slab.totalTax)}\n")
        }

        sb.append("\n✅ Generated via HisabPro Business Suite")

        val message = sb.toString()
        shareTextMessage(context, message, "GSTR-1 Tax Summary - $businessName")
    }

    fun shareProfitLossReport(
        context: Context,
        pl: ProfitLossSummary,
        period: ReportPeriod,
        businessName: String = SettingsRepository.getInstance(context).profile.value.shopName.ifBlank { "HisabPro Store" }
    ) {
        val salesStr = HisabViewModel.formatAmount(pl.salesRevenue)
        val cogsStr = HisabViewModel.formatAmount(pl.costOfGoodsSold)
        val grossProfitStr = HisabViewModel.formatAmount(pl.grossProfit)
        val expensesStr = HisabViewModel.formatAmount(pl.totalExpenses)
        val netProfitStr = HisabViewModel.formatAmount(pl.netProfit)

        val sb = StringBuilder()
        sb.append("📈 *PROFIT & LOSS (P&L) STATEMENT*\n")
        sb.append("🏢 Business: $businessName\n")
        sb.append("📅 Period: ${period.label}\n")
        sb.append("🕒 Generated: ${timeFormat.format(Date())}\n\n")

        sb.append("💰 *REVENUE & COST*\n")
        sb.append("• Gross Sales Revenue: ₹$salesStr\n")
        sb.append("• Cost of Goods Sold (COGS): (-) ₹$cogsStr\n")
        sb.append("• *Gross Profit: ₹$grossProfitStr* (${String.format(Locale.ENGLISH, "%.1f", pl.grossMarginPercent)}% Margin)\n\n")

        sb.append("🧾 *OPERATING EXPENSES*\n")
        pl.expenseByCategory.take(6).forEach { (cat, amt) ->
            sb.append("• $cat: ₹${HisabViewModel.formatAmount(amt)}\n")
        }
        sb.append("• *Total Operating Expenses: ₹$expensesStr*\n\n")

        val netStatus = if (pl.netProfit >= 0) "✅ NET PROFIT" else "⚠️ NET LOSS"
        sb.append("🎯 *$netStatus: ₹$netProfitStr*\n")
        sb.append("• Net Profit Margin: ${String.format(Locale.ENGLISH, "%.1f", pl.netMarginPercent)}%\n")
        sb.append("• Net Cashflow Inflow: ₹${HisabViewModel.formatAmount(pl.netCashflow)}\n\n")

        sb.append("Generated via HisabPro Business Suite")

        val message = sb.toString()
        shareTextMessage(context, message, "P&L Statement - $businessName")
    }

    fun shareDaybookReport(
        context: Context,
        daybook: DaybookSummary,
        businessName: String = SettingsRepository.getInstance(context).profile.value.shopName.ifBlank { "HisabPro Store" }
    ) {
        val dateStr = dateFormat.format(Date(daybook.dateMillis))
        val salesStr = HisabViewModel.formatAmount(daybook.daySalesTotal)
        val purchasesStr = HisabViewModel.formatAmount(daybook.dayPurchasesTotal)
        val cashInStr = HisabViewModel.formatAmount(daybook.dayCashIn)
        val cashOutStr = HisabViewModel.formatAmount(daybook.dayCashOut)
        val netCashStr = HisabViewModel.formatAmount(daybook.netCashMovement)
        val bankInStr = HisabViewModel.formatAmount(daybook.dayBankIn)
        val bankOutStr = HisabViewModel.formatAmount(daybook.dayBankOut)
        val netBankStr = HisabViewModel.formatAmount(daybook.netBankMovement)

        val sb = StringBuilder()
        sb.append("📖 *DAILY DAY BOOK (ROZNAMCHA)*\n")
        sb.append("🏢 Business: $businessName\n")
        sb.append("📅 Date: $dateStr\n\n")

        sb.append("📊 *DAILY TURNOVER SUMMARY*\n")
        sb.append("• Sales: ₹$salesStr (${daybook.daySalesCount} bills)\n")
        sb.append("• Purchases: ₹$purchasesStr (${daybook.dayPurchasesCount} bills)\n\n")

        sb.append("💵 *CASH BOOK (ROKAD)*\n")
        sb.append("• Cash Inflow: ₹$cashInStr\n")
        sb.append("• Cash Outflow: ₹$cashOutStr\n")
        sb.append("• *Net Cash Movement: ₹$netCashStr*\n\n")

        sb.append("🏦 *BANK & UPI BOOK*\n")
        sb.append("• Bank/UPI Inflow: ₹$bankInStr\n")
        sb.append("• Bank/UPI Outflow: ₹$bankOutStr\n")
        sb.append("• *Net Bank Movement: ₹$netBankStr*\n\n")

        if (daybook.vouchers.isNotEmpty()) {
            sb.append("📑 *DAILY JOURNAL VOUCHERS (${daybook.vouchers.size})*\n")
            daybook.vouchers.take(12).forEach { v ->
                sb.append("• [${v.voucherType.label}] #${v.voucherNumber}: ₹${HisabViewModel.formatAmount(v.amount)}\n")
                sb.append("  Dr: ${v.debitAccount} | Cr: ${v.creditAccount}\n")
                sb.append("  ${v.narration}\n")
            }
            sb.append("\n")
        }

        sb.append("Generated via HisabPro Business Suite")

        val message = sb.toString()
        shareTextMessage(context, message, "Daybook ($dateStr) - $businessName")
    }

    fun sharePartyAgingReport(
        context: Context,
        aging: PartyAgingSummary,
        businessName: String = SettingsRepository.getInstance(context).profile.value.shopName.ifBlank { "HisabPro Store" }
    ) {
        val receivableStr = HisabViewModel.formatAmount(aging.totalReceivable)
        val payableStr = HisabViewModel.formatAmount(aging.totalPayable)

        val sb = StringBuilder()
        sb.append("👥 *PARTY OUTSTANDING & AGING REPORT*\n")
        sb.append("🏢 Business: $businessName\n")
        sb.append("🕒 As of: ${timeFormat.format(Date())}\n\n")

        sb.append("📊 *OUTSTANDING BALANCES*\n")
        sb.append("• Total Customer Receivables (You'll Get): ₹$receivableStr\n")
        sb.append("• Total Supplier Payables (You'll Pay): ₹$payableStr\n\n")

        sb.append("⏳ *RECEIVABLES AGING BUCKETS*\n")
        sb.append("• 0 - 15 Days (Current): ₹${HisabViewModel.formatAmount(aging.bucket0to15)}\n")
        sb.append("• 16 - 30 Days (Due Soon): ₹${HisabViewModel.formatAmount(aging.bucket16to30)}\n")
        sb.append("• 31 - 60 Days (Overdue): ₹${HisabViewModel.formatAmount(aging.bucket31to60)}\n")
        sb.append("• 60+ Days (Critical): ₹${HisabViewModel.formatAmount(aging.bucket60Plus)}\n\n")

        if (aging.debtorList.isNotEmpty()) {
            sb.append("🚨 *TOP OUTSTANDING CUSTOMERS*\n")
            aging.debtorList.take(8).forEach { debtor ->
                sb.append("• ${debtor.partyName}: ₹${HisabViewModel.formatAmount(debtor.balanceDue)} (${debtor.bucket.badge})\n")
            }
            sb.append("\n")
        }

        sb.append("Generated via HisabPro Business Suite")

        val message = sb.toString()
        shareTextMessage(context, message, "Party Aging Dues - $businessName")
    }

    fun sharePurchasesRegisterReport(
        context: Context,
        purchasesRegister: PurchasesRegisterSummary,
        period: ReportPeriod,
        businessName: String = SettingsRepository.getInstance(context).profile.value.shopName.ifBlank { "HisabPro Store" }
    ) {
        val totalValStr = HisabViewModel.formatAmount(purchasesRegister.totalPurchasesValue)
        val taxableStr = HisabViewModel.formatAmount(purchasesRegister.totalTaxableAmount)
        val taxStr = HisabViewModel.formatAmount(purchasesRegister.totalTaxAmount)
        val itcStr = HisabViewModel.formatAmount(purchasesRegister.itcAvailableTax)
        val paidStr = HisabViewModel.formatAmount(purchasesRegister.totalPaidAmount)
        val dueStr = HisabViewModel.formatAmount(purchasesRegister.totalDueAmount)

        val sb = StringBuilder()
        sb.append("📦 *INWARD PURCHASES REGISTER & ITC REPORT*\n")
        sb.append("🏢 Business: $businessName\n")
        sb.append("📅 Period: ${period.label}\n")
        sb.append("🕒 Generated: ${timeFormat.format(Date())}\n\n")

        sb.append("📊 *PURCHASES SUMMARY*\n")
        sb.append("• Total Bills Inward: ${purchasesRegister.totalBillsCount}\n")
        sb.append("• Total Taxable Value: ₹$taxableStr\n")
        sb.append("• Total Tax Amount: ₹$taxStr\n")
        sb.append("• *Input Tax Credit (ITC) Eligible: ₹$itcStr*\n")
        sb.append("• *Total Purchase Value: ₹$totalValStr*\n\n")

        sb.append("💳 *PAYMENTS & PAYABLES*\n")
        sb.append("• Total Amount Paid: ₹$paidStr\n")
        sb.append("• Outstanding Supplier Dues: ₹$dueStr\n\n")

        if (purchasesRegister.purchases.isNotEmpty()) {
            sb.append("📑 *RECENT INWARD BILLS*\n")
            purchasesRegister.purchases.take(6).forEach { bill ->
                sb.append("• #${bill.purchaseNumber} - ${bill.supplierName}: ₹${HisabViewModel.formatAmount(bill.grandTotal)} (${bill.paymentStatus.label})\n")
            }
            sb.append("\n")
        }

        sb.append("Generated via HisabPro Business Suite")

        val message = sb.toString()
        shareTextMessage(context, message, "Purchases Register - $businessName")
    }

    fun shareDailySalesReport(
        context: Context,
        report: DailySalesReport,
        isGst: Boolean,
        businessName: String = SettingsRepository.getInstance(context).profile.value.shopName.ifBlank { "HisabPro Store" }
    ) {
        val dateStr = com.hisabpro.app.util.IndianAccountingFormat.formatIndianDate(report.dateMillis)
        val totalSalesStr = com.hisabpro.app.util.IndianAccountingFormat.formatIndianCurrency(report.totalSales)
        val cashSalesStr = com.hisabpro.app.util.IndianAccountingFormat.formatIndianCurrency(report.cashSales)
        val upiSalesStr = com.hisabpro.app.util.IndianAccountingFormat.formatIndianCurrency(report.upiSales)
        val creditSalesStr = com.hisabpro.app.util.IndianAccountingFormat.formatIndianCurrency(report.creditSales)

        val sb = StringBuilder()
        sb.append("📊 *DAILY SALES REPORT (दैनिक विक्री)*\n")
        sb.append("🏢 Business: $businessName\n")
        sb.append("📅 Date: $dateStr\n")
        sb.append("🕒 Generated: ${timeFormat.format(Date())}\n\n")

        sb.append("💰 *TOTAL SALES: $totalSalesStr*\n")
        sb.append("• Total Bills Created: ${report.invoiceCount}\n")
        sb.append("• Cash Sales: $cashSalesStr\n")
        sb.append("• UPI / Online Sales: $upiSalesStr\n")
        sb.append("• Credit (Udhar) Sales: $creditSalesStr\n")

        if (isGst && report.totalTax > 0) {
            val taxStr = com.hisabpro.app.util.IndianAccountingFormat.formatIndianCurrency(report.totalTax)
            val taxableStr = com.hisabpro.app.util.IndianAccountingFormat.formatIndianCurrency(report.taxableAmount)
            sb.append("• Taxable Turnover: $taxableStr\n")
            sb.append("• GST Tax Collected: $taxStr\n")
        }

        if (report.invoices.isNotEmpty()) {
            sb.append("\n🧾 *INVOICES SUMMARY*\n")
            report.invoices.take(8).forEach { inv ->
                sb.append("• #${inv.invoiceNumber} - ${inv.customerName}: ${com.hisabpro.app.util.IndianAccountingFormat.formatIndianCurrency(inv.grandTotal)} (${inv.paymentStatus.label})\n")
            }
            if (report.invoices.size > 8) {
                sb.append("• ... and ${report.invoices.size - 8} more bills\n")
            }
        }

        sb.append("\n✅ Generated via HisabPro Accounting Suite")
        shareTextMessage(context, sb.toString(), "Daily Sales Report ($dateStr) - $businessName")
    }

    fun shareSalesDateRangeReport(
        context: Context,
        report: SalesDateRangeReport,
        isGst: Boolean,
        businessName: String = SettingsRepository.getInstance(context).profile.value.shopName.ifBlank { "HisabPro Store" }
    ) {
        val totalSalesStr = com.hisabpro.app.util.IndianAccountingFormat.formatIndianCurrency(report.totalSales)
        val paidStr = com.hisabpro.app.util.IndianAccountingFormat.formatIndianCurrency(report.paidAmount)
        val unpaidStr = com.hisabpro.app.util.IndianAccountingFormat.formatIndianCurrency(report.unpaidAmount)

        val sb = StringBuilder()
        sb.append("📈 *SALES REGISTER REPORT (विक्री अहवाल)*\n")
        sb.append("🏢 Business: $businessName\n")
        sb.append("📅 Period: ${report.period.label}\n")
        sb.append("🕒 Generated: ${timeFormat.format(Date())}\n\n")

        sb.append("💰 *TURNOVER & COLLECTIONS*\n")
        sb.append("• Gross Sales Turnover: $totalSalesStr\n")
        sb.append("• Total Invoices Billed: ${report.invoiceCount}\n")
        sb.append("• Amount Collected: $paidStr\n")
        sb.append("• Outstanding / Credit: $unpaidStr\n")

        if (isGst && report.totalTax > 0) {
            val taxStr = com.hisabpro.app.util.IndianAccountingFormat.formatIndianCurrency(report.totalTax)
            sb.append("• Total GST Collected: $taxStr\n")
        }

        sb.append("\n✅ Generated via HisabPro Accounting Suite")
        shareTextMessage(context, sb.toString(), "Sales Report (${report.period.label}) - $businessName")
    }

    fun shareOutstandingReport(
        context: Context,
        report: OutstandingReport,
        businessName: String = SettingsRepository.getInstance(context).profile.value.shopName.ifBlank { "HisabPro Store" }
    ) {
        val recStr = com.hisabpro.app.util.IndianAccountingFormat.formatIndianCurrency(report.totalReceivable)
        val payStr = com.hisabpro.app.util.IndianAccountingFormat.formatIndianCurrency(report.totalPayable)

        val sb = StringBuilder()
        sb.append("⚠️ *OUTSTANDING DUES SUMMARY (उधारी अहवाल)*\n")
        sb.append("🏢 Business: $businessName\n")
        sb.append("🕒 As of: ${timeFormat.format(Date())}\n\n")

        sb.append("📥 *CUSTOMER OUTSTANDING (You'll Get / येणे): $recStr*\n")
        sb.append("• Total Debtors: ${report.customerCount}\n")
        sb.append("• 0-15 Days (Current): ${com.hisabpro.app.util.IndianAccountingFormat.formatIndianCurrency(report.aging0to15)}\n")
        sb.append("• 16-30 Days (Due Soon): ${com.hisabpro.app.util.IndianAccountingFormat.formatIndianCurrency(report.aging16to30)}\n")
        sb.append("• 31-60 Days (Overdue): ${com.hisabpro.app.util.IndianAccountingFormat.formatIndianCurrency(report.aging31to60)}\n")
        sb.append("• 60+ Days (Critical): ${com.hisabpro.app.util.IndianAccountingFormat.formatIndianCurrency(report.aging60Plus)}\n\n")

        sb.append("📤 *SUPPLIER OUTSTANDING (You'll Pay / देणे): $payStr*\n")
        sb.append("• Total Creditors: ${report.supplierCount}\n\n")

        if (report.customerList.isNotEmpty()) {
            sb.append("👥 *TOP CUSTOMER DUES*\n")
            report.customerList.take(6).forEach { cust ->
                sb.append("• ${cust.party.name}: ${com.hisabpro.app.util.IndianAccountingFormat.formatIndianCurrency(cust.netBalance)}\n")
            }
            sb.append("\n")
        }

        sb.append("✅ Generated via HisabPro Accounting Suite")
        shareTextMessage(context, sb.toString(), "Outstanding Dues Report - $businessName")
    }

    fun sharePartyLedgerReport(
        context: Context,
        report: PartyLedgerReport,
        businessName: String = SettingsRepository.getInstance(context).profile.value.shopName.ifBlank { "HisabPro Store" }
    ) {
        val partyName = report.party?.name ?: "Party"
        val partyPhone = report.party?.phone ?: ""
        val isCustomer = report.partyType == com.hisabpro.app.data.model.PartyType.CUSTOMER

        val sb = StringBuilder()
        val title = if (isCustomer) "CUSTOMER LEDGER (ग्राहक खाते)" else "SUPPLIER LEDGER (व्यापारी खाते)"
        sb.append("📖 *$title*\n")
        sb.append("🏢 Business: $businessName\n")
        sb.append("👤 Party: $partyName ($partyPhone)\n")
        sb.append("🕒 Generated: ${timeFormat.format(Date())}\n\n")

        sb.append("📊 *ACCOUNT SUMMARY*\n")
        sb.append("• Opening Balance: ${com.hisabpro.app.util.IndianAccountingFormat.formatIndianCurrency(report.openingBalance)} ${report.openingBalanceType}\n")
        sb.append("• Total Debit (Dr): ${com.hisabpro.app.util.IndianAccountingFormat.formatIndianCurrency(report.totalDebit)}\n")
        sb.append("• Total Credit (Cr): ${com.hisabpro.app.util.IndianAccountingFormat.formatIndianCurrency(report.totalCredit)}\n")
        sb.append("• *Closing Balance: ${com.hisabpro.app.util.IndianAccountingFormat.formatIndianCurrency(report.closingBalance)} ${report.closingBalanceType}*\n\n")

        if (report.entries.isNotEmpty()) {
            sb.append("📑 *TRANSACTION STATEMENT*\n")
            report.entries.take(10).forEach { entry ->
                val date = com.hisabpro.app.util.IndianAccountingFormat.formatIndianDate(entry.dateMillis)
                val amt = if (entry.debitAmount > 0) "Dr ${com.hisabpro.app.util.IndianAccountingFormat.formatIndianCurrency(entry.debitAmount)}" else "Cr ${com.hisabpro.app.util.IndianAccountingFormat.formatIndianCurrency(entry.creditAmount)}"
                sb.append("• $date: ${entry.narration.ifBlank { entry.voucherType }} - $amt (Bal: ${com.hisabpro.app.util.IndianAccountingFormat.formatIndianCurrency(entry.runningBalance)} ${entry.balanceType})\n")
            }
            if (report.entries.size > 10) {
                sb.append("• ... and ${report.entries.size - 10} more transactions\n")
            }
            sb.append("\n")
        }

        sb.append("✅ Generated via HisabPro Accounting Suite")
        shareTextMessage(context, sb.toString(), "$partyName Ledger Statement - $businessName")
    }

    fun shareExpenseReport(
        context: Context,
        report: ExpenseReportSummary,
        businessName: String = SettingsRepository.getInstance(context).profile.value.shopName.ifBlank { "HisabPro Store" }
    ) {
        val totalExpStr = com.hisabpro.app.util.IndianAccountingFormat.formatIndianCurrency(report.totalExpenses)

        val sb = StringBuilder()
        sb.append("🧾 *BUSINESS EXPENSE REPORT (खर्च अहवाल)*\n")
        sb.append("🏢 Business: $businessName\n")
        sb.append("📅 Period: ${report.period.label}\n")
        sb.append("🕒 Generated: ${timeFormat.format(Date())}\n\n")

        sb.append("💰 *TOTAL EXPENSES: $totalExpStr*\n")
        sb.append("• Total Expense Vouchers: ${report.expenseCount}\n\n")

        if (report.categoryBreakdown.isNotEmpty()) {
            sb.append("📂 *CATEGORY BREAKDOWN*\n")
            report.categoryBreakdown.forEach { (cat, amt) ->
                val pct = if (report.totalExpenses > 0) (amt / report.totalExpenses) * 100 else 0.0
                sb.append("• $cat: ${com.hisabpro.app.util.IndianAccountingFormat.formatIndianCurrency(amt)} (${String.format(Locale.ENGLISH, "%.1f", pct)}%)\n")
            }
            sb.append("\n")
        }

        if (report.modeBreakdown.isNotEmpty()) {
            sb.append("💳 *PAYMENT MODE BREAKDOWN*\n")
            report.modeBreakdown.forEach { (mode, amt) ->
                sb.append("• $mode: ${com.hisabpro.app.util.IndianAccountingFormat.formatIndianCurrency(amt)}\n")
            }
            sb.append("\n")
        }

        sb.append("✅ Generated via HisabPro Accounting Suite")
        shareTextMessage(context, sb.toString(), "Expense Report (${report.period.label}) - $businessName")
    }

    fun shareCashBookReport(
        context: Context,
        report: CashBookReportSummary,
        businessName: String = SettingsRepository.getInstance(context).profile.value.shopName.ifBlank { "HisabPro Store" }
    ) {
        val bookTitle = if (report.isBankMode) "BANK & UPI BOOK (बँक वही)" else "CASH BOOK (रोकड वही)"
        val openStr = com.hisabpro.app.util.IndianAccountingFormat.formatIndianCurrency(report.openingBalance)
        val inStr = com.hisabpro.app.util.IndianAccountingFormat.formatIndianCurrency(report.totalInflow)
        val outStr = com.hisabpro.app.util.IndianAccountingFormat.formatIndianCurrency(report.totalOutflow)
        val closeStr = com.hisabpro.app.util.IndianAccountingFormat.formatIndianCurrency(report.closingBalance)

        val sb = StringBuilder()
        sb.append("💵 *$bookTitle*\n")
        sb.append("🏢 Business: $businessName\n")
        sb.append("📅 Period: ${report.period.label}\n")
        sb.append("🕒 Generated: ${timeFormat.format(Date())}\n\n")

        sb.append("📊 *BALANCE & MOVEMENT*\n")
        sb.append("• Opening Balance: $openStr\n")
        sb.append("• Total Inflow (Dr / Receipts): (+) $inStr\n")
        sb.append("• Total Outflow (Cr / Payments): (-) $outStr\n")
        sb.append("• *Closing Balance in Hand: $closeStr*\n\n")

        if (report.entries.isNotEmpty()) {
            sb.append("📑 *RECENT CASH/BANK TRANSACTIONS*\n")
            report.entries.take(10).forEach { entry ->
                val date = com.hisabpro.app.util.IndianAccountingFormat.formatIndianDate(entry.dateMillis)
                val flow = if (entry.inAmount > 0) "(+) ${com.hisabpro.app.util.IndianAccountingFormat.formatIndianCurrency(entry.inAmount)}" else "(-) ${com.hisabpro.app.util.IndianAccountingFormat.formatIndianCurrency(entry.outAmount)}"
                sb.append("• $date: ${entry.particulars} - $flow (Bal: ${com.hisabpro.app.util.IndianAccountingFormat.formatIndianCurrency(entry.runningBalance)})\n")
            }
            if (report.entries.size > 10) {
                sb.append("• ... and ${report.entries.size - 10} more vouchers\n")
            }
            sb.append("\n")
        }

        sb.append("✅ Generated via HisabPro Accounting Suite")
        shareTextMessage(context, sb.toString(), "$bookTitle (${report.period.label}) - $businessName")
    }

    fun exportGstr1Csv(
        context: Context,
        gstr: Gstr1Summary,
        period: ReportPeriod,
        businessName: String = SettingsRepository.getInstance(context).profile.value.shopName.ifBlank { "HisabPro Store" },
        gstin: String = SettingsRepository.getInstance(context).profile.value.gstin
    ): Uri? {
        return try {
            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) reportsDir.mkdirs()

            val file = File(reportsDir, "GSTR1_${period.name}_${System.currentTimeMillis()}.csv")
            file.bufferedWriter().use { out ->
                out.write("GSTR-1 SALES SUMMARY REPORT\n")
                out.write("Business Name,\"$businessName\"\n")
                if (gstin.isNotBlank()) out.write("GSTIN,\"$gstin\"\n")
                out.write("Period,${period.label}\n")
                out.write("Generated At,${timeFormat.format(Date())}\n\n")

                out.write("OVERVIEW\n")
                out.write("Metric,Amount (INR)\n")
                out.write("Total Taxable Turnover,${gstr.totalTaxableSupplies}\n")
                out.write("CGST Amount,${gstr.totalCgst}\n")
                out.write("SGST Amount,${gstr.totalSgst}\n")
                out.write("IGST Amount,${gstr.totalIgst}\n")
                out.write("Total Tax Collected,${gstr.totalTax}\n")
                out.write("Total Invoice Value,${gstr.totalInvoiceValue}\n\n")

                out.write("TAX SLAB SUMMARY\n")
                out.write("Rate (%),Taxable Value,CGST,SGST,IGST,Total Tax\n")
                gstr.slabSummaries.forEach { s ->
                    out.write("${s.gstRate}%,${s.taxableAmount},${s.cgstAmount},${s.sgstAmount},${s.igstAmount},${s.totalTax}\n")
                }
                out.write("\n")

                out.write("HSN CODE SUMMARY\n")
                out.write("HSN Code,Description,Qty,Unit,Taxable Value,GST Rate,Tax Amount\n")
                gstr.hsnSummaries.forEach { h ->
                    out.write("\"${h.hsnCode}\",\"${h.description}\",${h.totalQuantity},${h.unit},${h.taxableAmount},${h.gstRate}%,${h.totalTax}\n")
                }
                out.write("\n")

                out.write("INVOICE WISE DETAILS\n")
                out.write("Invoice No,Date,Customer,GSTIN,Type,Taxable,Tax,Total,Status\n")
                gstr.eligibleInvoices.forEach { inv ->
                    out.write("\"${inv.invoiceNumber}\",\"${dateFormat.format(Date(inv.dateMillis))}\",\"${inv.customerName}\",\"${inv.customerGstin}\",\"${inv.type.label}\",${inv.subtotal},${inv.totalTax},${inv.grandTotal},\"${inv.paymentStatus.label}\"\n")
                }
            }

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportInventoryStockCsv(
        context: Context,
        items: List<Item>,
        businessName: String = SettingsRepository.getInstance(context).profile.value.shopName.ifBlank { "HisabPro Store" }
    ): Uri? {
        return try {
            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) reportsDir.mkdirs()

            val file = File(reportsDir, "Inventory_Stock_${System.currentTimeMillis()}.csv")
            file.bufferedWriter().use { out ->
                out.write("INVENTORY STOCK VALUATION & AUDIT REPORT\n")
                out.write("Business Name,\"$businessName\"\n")
                out.write("Generated At,${timeFormat.format(Date())}\n\n")

                out.write("Item Name,SKU,Category,Unit,Current Stock,Min Stock Alert,Stock Status,Sale Price,Cost Price,Profit Margin (INR),Cost Valuation (INR),Retail Valuation (INR),GST Rate,HSN Code\n")
                items.forEach { item ->
                    val status = when {
                        item.isOutOfStock -> "OUT OF STOCK"
                        item.isLowStock -> "LOW STOCK"
                        else -> "IN STOCK"
                    }
                    val margin = item.salePrice - item.purchasePrice
                    val costVal = item.currentStock * item.purchasePrice
                    val saleVal = item.currentStock * item.salePrice
                    out.write("\"${item.name}\",\"${item.itemCode}\",\"${item.category}\",\"${item.unit}\",${item.currentStock},${item.minStockAlert},\"$status\",${item.salePrice},${item.purchasePrice},$margin,$costVal,$saleVal,${item.gstRate}%,\"${item.hsnCode}\"\n")
                }
                out.write("\n")
                val totalCostVal = items.sumOf { it.currentStock * it.purchasePrice }
                val totalRetailVal = items.sumOf { it.currentStock * it.salePrice }
                out.write("TOTALS,,,,,Total Items: ${items.size},,,,,$totalCostVal,$totalRetailVal,,\n")
            }

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportItemStockMovementCsv(
        context: Context,
        item: Item,
        history: List<StockHistoryEntry>,
        businessName: String = SettingsRepository.getInstance(context).profile.value.shopName.ifBlank { "HisabPro Store" }
    ): Uri? {
        return try {
            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) reportsDir.mkdirs()

            val sanitizedName = item.name.replace(Regex("[^a-zA-Z0-9_]"), "_")
            val file = File(reportsDir, "Stock_Movement_${sanitizedName}_${System.currentTimeMillis()}.csv")
            file.bufferedWriter().use { out ->
                out.write("PRODUCT STOCK MOVEMENT AUDIT TRAIL\n")
                out.write("Business Name,\"$businessName\"\n")
                out.write("Product Name,\"${item.name}\"\n")
                if (item.itemCode.isNotBlank()) out.write("SKU,\"${item.itemCode}\"\n")
                if (item.hsnCode.isNotBlank()) out.write("HSN Code,\"${item.hsnCode}\"\n")
                out.write("Category,\"${item.category}\"\n")
                out.write("Current Stock,${item.currentStock} ${item.unit}\n")
                out.write("Sale Price,${item.salePrice}\n")
                out.write("Cost Price,${item.purchasePrice}\n")
                out.write("Generated At,${timeFormat.format(Date())}\n\n")

                out.write("Date & Time,Movement Reason,Change Qty,Unit,Balance After,Note / Reference\n")
                history.forEach { entry ->
                    val changeStr = if (entry.changeQty > 0) "+${entry.changeQty}" else "${entry.changeQty}"
                    out.write("\"${timeFormat.format(Date(entry.timestampMillis))}\",\"${entry.reason.label}\",$changeStr,\"${item.unit}\",${entry.newStock},\"${entry.note.replace("\"", "\"\"")}\"\n")
                }
            }

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportProfitLossCsv(
        context: Context,
        pl: ProfitLossSummary,
        period: ReportPeriod,
        businessName: String = SettingsRepository.getInstance(context).profile.value.shopName.ifBlank { "HisabPro Store" }
    ): Uri? {
        return try {
            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) reportsDir.mkdirs()

            val file = File(reportsDir, "ProfitLoss_${period.name}_${System.currentTimeMillis()}.csv")
            file.bufferedWriter().use { out ->
                out.write("PROFIT & LOSS (P&L) STATEMENT\n")
                out.write("Business Name,\"$businessName\"\n")
                out.write("Period,${period.label}\n")
                out.write("Generated At,${timeFormat.format(Date())}\n\n")

                out.write("REVENUE & EXPENSES BREAKDOWN\n")
                out.write("Particulars,Amount (INR)\n")
                out.write("Gross Sales Revenue,${pl.salesRevenue}\n")
                out.write("Cost of Goods Sold (COGS),${pl.costOfGoodsSold}\n")
                out.write("Gross Profit,${pl.grossProfit}\n")
                out.write("Gross Margin (%),${String.format(Locale.ENGLISH, "%.2f", pl.grossMarginPercent)}\n\n")

                out.write("OPERATING EXPENSES\n")
                out.write("Category,Amount (INR)\n")
                pl.expenseByCategory.forEach { (cat, amt) ->
                    out.write("\"$cat\",$amt\n")
                }
                out.write("Total Operating Expenses,${pl.totalExpenses}\n\n")

                out.write("NET RESULTS\n")
                out.write("Net Profit / (Loss),${pl.netProfit}\n")
                out.write("Net Margin (%),${String.format(Locale.ENGLISH, "%.2f", pl.netMarginPercent)}\n")
                out.write("Cash Inflow,${pl.cashInflow}\n")
                out.write("Cash Outflow,${pl.cashOutflow}\n")
                out.write("Net Cash Movement,${pl.netCashflow}\n")
            }

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportDaybookCsv(
        context: Context,
        daybook: DaybookSummary,
        businessName: String = SettingsRepository.getInstance(context).profile.value.shopName.ifBlank { "HisabPro Store" }
    ): Uri? {
        return try {
            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) reportsDir.mkdirs()

            val dateStr = SimpleDateFormat("yyyyMMdd", Locale.ENGLISH).format(Date(daybook.dateMillis))
            val file = File(reportsDir, "Daybook_Roznamcha_${dateStr}_${System.currentTimeMillis()}.csv")
            file.bufferedWriter().use { out ->
                out.write("DAILY DAY BOOK (ROZNAMCHA) REGISTER\n")
                out.write("Business Name,\"$businessName\"\n")
                out.write("Date,${dateFormat.format(Date(daybook.dateMillis))}\n")
                out.write("Generated At,${timeFormat.format(Date())}\n\n")

                out.write("EXECUTIVE SUMMARY\n")
                out.write("Particulars,Amount (INR)\n")
                out.write("Total Invoiced Sales,${daybook.daySalesTotal}\n")
                out.write("Total Inward Purchases,${daybook.dayPurchasesTotal}\n")
                out.write("Cash Inflow (Rokad In),${daybook.dayCashIn}\n")
                out.write("Cash Outflow (Rokad Out),${daybook.dayCashOut}\n")
                out.write("Net Cash Movement,${daybook.netCashMovement}\n")
                out.write("Bank & UPI Inflow,${daybook.dayBankIn}\n")
                out.write("Bank & UPI Outflow,${daybook.dayBankOut}\n")
                out.write("Net Bank Movement,${daybook.netBankMovement}\n")
                out.write("Net Total Liquidity Movement,${daybook.netDayMovement}\n\n")

                out.write("DAILY JOURNAL VOUCHERS (ROZNAMCHA)\n")
                out.write("Voucher No,Voucher Type,Debit (Dr) Account,Credit (Cr) Account,Narration,Payment Mode,Amount (INR)\n")
                daybook.vouchers.forEach { v ->
                    out.write("\"${v.voucherNumber}\",\"${v.voucherType.label}\",\"${v.debitAccount}\",\"${v.creditAccount}\",\"${v.narration.replace("\"", "\"\"")}\",\"${v.paymentMode}\",${v.amount}\n")
                }
                out.write("\n")

                out.write("DAY INVOICES\n")
                out.write("Invoice No,Customer,Subtotal,Tax,Grand Total,Status\n")
                daybook.dayInvoices.forEach { inv ->
                    out.write("\"${inv.invoiceNumber}\",\"${inv.customerName}\",${inv.subtotal},${inv.totalTax},${inv.grandTotal},\"${inv.paymentStatus.label}\"\n")
                }
                out.write("\n")

                out.write("DAY PURCHASES\n")
                out.write("Bill No,Supplier,Subtotal,Tax,Grand Total,Status\n")
                daybook.dayPurchases.forEach { pur ->
                    out.write("\"${pur.purchaseNumber}\",\"${pur.supplierName}\",${pur.subtotal},${pur.totalTax},${pur.grandTotal},\"${pur.paymentStatus.label}\"\n")
                }
            }

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportPartyAgingCsv(
        context: Context,
        aging: PartyAgingSummary,
        businessName: String = SettingsRepository.getInstance(context).profile.value.shopName.ifBlank { "HisabPro Store" }
    ): Uri? {
        return try {
            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) reportsDir.mkdirs()

            val file = File(reportsDir, "PartyAging_${System.currentTimeMillis()}.csv")
            file.bufferedWriter().use { out ->
                out.write("PARTY OUTSTANDING & AGING REPORT\n")
                out.write("Business Name,\"$businessName\"\n")
                out.write("Generated At,${timeFormat.format(Date())}\n\n")

                out.write("OUTSTANDING TOTALS\n")
                out.write("Total Receivables (Customers),${aging.totalReceivable}\n")
                out.write("Total Payables (Suppliers),${aging.totalPayable}\n")
                out.write("0-15 Days (Current),${aging.bucket0to15}\n")
                out.write("16-30 Days (Due Soon),${aging.bucket16to30}\n")
                out.write("31-60 Days (Overdue),${aging.bucket31to60}\n")
                out.write("60+ Days (Critical),${aging.bucket60Plus}\n\n")

                out.write("DEBTOR DETAILS\n")
                out.write("Party Name,Phone,Balance Due (INR),Days Overdue,Aging Bucket\n")
                aging.debtorList.forEach { d ->
                    out.write("\"${d.partyName}\",\"${d.phone}\",${d.balanceDue},${d.daysOverdue},\"${d.bucket.label}\"\n")
                }
            }

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportGstr3bCsv(
        context: Context,
        gstr3b: Gstr3bSummary,
        period: ReportPeriod,
        businessName: String = SettingsRepository.getInstance(context).profile.value.shopName.ifBlank { "HisabPro Store" },
        gstin: String = SettingsRepository.getInstance(context).profile.value.gstin
    ): Uri? {
        return try {
            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) reportsDir.mkdirs()

            val file = File(reportsDir, "GSTR3B_${period.name}_${System.currentTimeMillis()}.csv")
            file.bufferedWriter().use { out ->
                out.write("GSTR-3B MONTHLY TAX OFFSET & ITC RETURN\n")
                out.write("Business Name,\"$businessName\"\n")
                if (gstin.isNotBlank()) out.write("GSTIN,\"$gstin\"\n")
                out.write("Period,${period.label}\n")
                out.write("Generated At,${timeFormat.format(Date())}\n\n")

                out.write("3.1 OUTWARD TAXABLE SUPPLIES (SALES OUTPUT TAX)\n")
                out.write("Taxable Turnover,CGST,SGST,IGST,Total Output Tax\n")
                out.write("${gstr3b.outwardTaxable},${gstr3b.outwardCgst},${gstr3b.outwardSgst},${gstr3b.outwardIgst},${gstr3b.totalOutputTax}\n\n")

                out.write("4. ELIGIBLE INPUT TAX CREDIT (PURCHASES ITC)\n")
                out.write("Inward Purchases,CGST ITC,SGST ITC,IGST ITC,Total ITC Available\n")
                out.write("${gstr3b.inwardTaxable},${gstr3b.itcCgst},${gstr3b.itcSgst},${gstr3b.itcIgst},${gstr3b.totalInputTaxCredit}\n\n")

                out.write("5. NET TAX PAYABLE IN CASH (OUTPUT TAX MINUS ITC)\n")
                out.write("Net CGST,Net SGST,Net IGST,Total Net Tax Payable\n")
                out.write("${gstr3b.netCgstPayable},${gstr3b.netSgstPayable},${gstr3b.netIgstPayable},${gstr3b.totalNetGstPayable}\n")
            }

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportPurchasesRegisterCsv(
        context: Context,
        purchases: List<PurchaseBill>,
        businessName: String = SettingsRepository.getInstance(context).profile.value.shopName.ifBlank { "HisabPro Store" }
    ): Uri? {
        return try {
            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) reportsDir.mkdirs()

            val file = File(reportsDir, "Purchases_Register_${System.currentTimeMillis()}.csv")
            file.bufferedWriter().use { out ->
                out.write("PURCHASES & INWARD REGISTER REPORT\n")
                out.write("Business Name,\"$businessName\"\n")
                out.write("Generated At,${timeFormat.format(Date())}\n")
                out.write("Total Bills,${purchases.size}\n")
                out.write("Total Purchases Value,${purchases.sumOf { it.grandTotal }}\n")
                out.write("Total Tax / ITC Available,${purchases.filter { it.itcEligible }.sumOf { it.totalTax }}\n")
                out.write("Total Outstanding Payables,${purchases.sumOf { it.dueAmount }}\n\n")

                out.write("Purchase No,Date,Supplier Name,Supplier GSTIN,Vendor Bill No,Subtotal,Tax (ITC),Grand Total,Paid Amount,Balance Due,Status,ITC Eligible,Notes\n")
                purchases.forEach { bill ->
                    out.write("\"${bill.purchaseNumber}\",\"${dateFormat.format(Date(bill.dateMillis))}\",\"${bill.supplierName.replace("\"", "\"\"")}\",\"${bill.supplierGstin}\",\"${bill.vendorBillNumber}\",${bill.subtotal},${bill.totalTax},${bill.grandTotal},${bill.paidAmount},${bill.dueAmount},\"${bill.paymentStatus.label}\",\"${if (bill.itcEligible) "YES" else "NO"}\",\"${bill.notes.replace("\"", "\"\"")}\"\n")
                }
            }

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportSinglePurchaseBillCsv(
        context: Context,
        bill: PurchaseBill,
        businessName: String = SettingsRepository.getInstance(context).profile.value.shopName.ifBlank { "HisabPro Store" }
    ): Uri? {
        return try {
            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) reportsDir.mkdirs()

            val sanitizedBillNo = bill.purchaseNumber.replace(Regex("[^a-zA-Z0-9_]"), "_")
            val file = File(reportsDir, "Purchase_${sanitizedBillNo}_${System.currentTimeMillis()}.csv")
            file.bufferedWriter().use { out ->
                out.write("PURCHASE BILL / INWARD VOUCHER\n")
                out.write("Business Name,\"$businessName\"\n")
                out.write("Purchase Bill No,\"${bill.purchaseNumber}\"\n")
                if (bill.vendorBillNumber.isNotBlank()) out.write("Vendor Bill Ref,\"${bill.vendorBillNumber}\"\n")
                out.write("Supplier Name,\"${bill.supplierName}\"\n")
                if (bill.supplierGstin.isNotBlank()) out.write("Supplier GSTIN,\"${bill.supplierGstin}\"\n")
                out.write("Date,${dateFormat.format(Date(bill.dateMillis))}\n")
                out.write("Status,\"${bill.paymentStatus.label}\"\n")
                out.write("ITC Eligible,\"${if (bill.itcEligible) "YES" else "NO"}\"\n")
                out.write("Generated At,${timeFormat.format(Date())}\n\n")

                out.write("S.No,Item Description,HSN Code,Quantity,Unit,Unit Price,Taxable Amount,GST Rate (%),GST Tax (INR),Total (INR)\n")
                bill.items.forEachIndexed { index, item ->
                    val tax = item.getTaxAmount(bill.gstMode)
                    val total = item.getTotal(bill.gstMode)
                    out.write("${index + 1},\"${item.description.replace("\"", "\"\"")}\",\"${item.hsnCode}\",${item.quantity},\"${item.unit}\",${item.unitPrice},${item.taxableAmount},${item.gstRate},$tax,$total\n")
                }
                out.write("\nSUMMARY\n")
                out.write("Subtotal,${bill.subtotal}\n")
                out.write("Total Tax (ITC),${bill.totalTax}\n")
                out.write("Grand Total,${bill.grandTotal}\n")
                out.write("Paid Amount,${bill.paidAmount}\n")
                out.write("Balance Due,${bill.dueAmount}\n")
                out.write("Payment Mode,\"${bill.paymentMode}\"\n")
                if (bill.notes.isNotBlank()) out.write("Notes,\"${bill.notes.replace("\"", "\"\"")}\"\n")
            }

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareTrialBalanceReport(
        context: Context,
        tb: TrialBalanceSummary,
        period: ReportPeriod,
        businessName: String = SettingsRepository.getInstance(context).profile.value.shopName.ifBlank { "HisabPro Store" }
    ) {
        val totalDrStr = HisabViewModel.formatAmount(tb.totalDebit)
        val totalCrStr = HisabViewModel.formatAmount(tb.totalCredit)
        val diffStr = HisabViewModel.formatAmount(tb.difference)

        val sb = StringBuilder()
        sb.append("⚖️ *TRIAL BALANCE (कच्चा ताळेबंद / तलपट)*\n")
        sb.append("🏢 Business: $businessName\n")
        sb.append("📅 As of: ${dateFormat.format(Date(tb.asOfDateMillis))} (${period.label})\n")
        sb.append("🕒 Generated: ${timeFormat.format(Date())}\n\n")

        val status = if (tb.isBalanced) "✅ BALANCED (Dr == Cr)" else "⚠️ UNBALANCED (Diff: ₹$diffStr)"
        sb.append("Status: $status\n")
        sb.append("• Total Debits (Dr): ₹$totalDrStr\n")
        sb.append("• Total Credits (Cr): ₹$totalCrStr\n\n")

        sb.append("📁 *LEDGER ACCOUNTS:*\n")
        tb.accounts.forEach { acc ->
            val amountStr = if (acc.debitAmount > 0.0) {
                "Dr ₹${HisabViewModel.formatAmount(acc.debitAmount)}"
            } else {
                "Cr ₹${HisabViewModel.formatAmount(acc.creditAmount)}"
            }
            sb.append("• [${acc.accountCategory}] ${acc.accountName}: $amountStr\n")
        }

        sb.append("\n✅ Generated via HisabPro Business Suite")
        shareTextMessage(context, sb.toString(), "Trial Balance - $businessName")
    }

    fun exportTrialBalanceCsv(
        context: Context,
        tb: TrialBalanceSummary,
        period: ReportPeriod,
        businessName: String = SettingsRepository.getInstance(context).profile.value.shopName.ifBlank { "HisabPro Store" }
    ): Uri? {
        return try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(exportDir, "Trial_Balance_${System.currentTimeMillis()}.csv")

            file.bufferedWriter().use { out ->
                out.write("TRIAL BALANCE (KACHHA TALEBAND / TALPAT)\n")
                out.write("Business,\"${businessName.replace("\"", "\"\"")}\"\n")
                out.write("As of Date,\"${dateFormat.format(Date(tb.asOfDateMillis))}\"\n")
                out.write("Period,\"${period.label}\"\n")
                out.write("Status,\"${if (tb.isBalanced) "BALANCED" else "UNBALANCED"}\"\n")
                out.write("\n")

                out.write("Code,Account Name,Category,Debit (Dr INR),Credit (Cr INR),Notes\n")
                tb.accounts.forEach { acc ->
                    val dr = if (acc.debitAmount > 0) String.format(Locale.ENGLISH, "%.2f", acc.debitAmount) else "0.00"
                    val cr = if (acc.creditAmount > 0) String.format(Locale.ENGLISH, "%.2f", acc.creditAmount) else "0.00"
                    out.write("\"${acc.accountCode}\",\"${acc.accountName.replace("\"", "\"\"")}\",\"${acc.accountCategory}\",$dr,$cr,\"${acc.note.replace("\"", "\"\"")}\"\n")
                }

                out.write("\n")
                out.write("TOTAL,,,${String.format(Locale.ENGLISH, "%.2f", tb.totalDebit)},${String.format(Locale.ENGLISH, "%.2f", tb.totalCredit)},\n")
                out.write("DIFFERENCE,,,,${String.format(Locale.ENGLISH, "%.2f", tb.difference)},\n")
            }

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareCsvFile(context: Context, uri: Uri, title: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "Share $title via")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(chooser)
        } catch (_: Exception) {}
    }

    private fun shareTextMessage(context: Context, message: String, subject: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val chooser = Intent.createChooser(intent, "Share via")
        chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        try {
            context.startActivity(chooser)
        } catch (_: Exception) {}
    }
}
