package com.hisabpro.app.ui.reports

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
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
        businessName: String = "HisabPro Store"
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
        businessName: String = "HisabPro Store"
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
        businessName: String = "HisabPro Store"
    ) {
        val dateStr = dateFormat.format(Date(daybook.dateMillis))
        val salesStr = HisabViewModel.formatAmount(daybook.daySalesTotal)
        val cashInStr = HisabViewModel.formatAmount(daybook.dayCashIn)
        val cashOutStr = HisabViewModel.formatAmount(daybook.dayCashOut)
        val netMovementStr = HisabViewModel.formatAmount(daybook.netDayMovement)

        val sb = StringBuilder()
        sb.append("📖 *DAILY DAYBOOK REGISTER*\n")
        sb.append("🏢 Business: $businessName\n")
        sb.append("📅 Date: $dateStr\n\n")

        sb.append("💵 *DAILY CASH & SALES SUMMARY*\n")
        sb.append("• Total Invoiced Sales: ₹$salesStr (${daybook.daySalesCount} bills)\n")
        sb.append("• Total Cash Received: ₹$cashInStr\n")
        sb.append("• Total Cash Paid Out: ₹$cashOutStr\n")
        sb.append("• *Net Cashflow Today: ₹$netMovementStr*\n\n")

        if (daybook.dayInvoices.isNotEmpty()) {
            sb.append("📑 *INVOICES ISSUED TODAY*\n")
            daybook.dayInvoices.take(8).forEach { inv ->
                sb.append("• #${inv.invoiceNumber} - ${inv.customerName}: ₹${HisabViewModel.formatAmount(inv.grandTotal)} (${inv.paymentStatus.label})\n")
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
        businessName: String = "HisabPro Store"
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

    fun exportGstr1Csv(
        context: Context,
        gstr: Gstr1Summary,
        period: ReportPeriod
    ): Uri? {
        return try {
            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) reportsDir.mkdirs()

            val file = File(reportsDir, "GSTR1_${period.name}_${System.currentTimeMillis()}.csv")
            file.bufferedWriter().use { out ->
                out.write("GSTR-1 SALES SUMMARY REPORT\n")
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
