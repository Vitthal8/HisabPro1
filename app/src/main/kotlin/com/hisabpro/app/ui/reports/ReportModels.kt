package com.hisabpro.app.ui.reports

import com.hisabpro.app.data.model.Invoice
import com.hisabpro.app.data.model.Party
import com.hisabpro.app.data.model.PartyType
import com.hisabpro.app.data.model.PartyWithBalance
import com.hisabpro.app.data.model.PurchaseBill
import com.hisabpro.app.data.model.Transaction

enum class ReportPeriod(val label: String) {
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    THIS_QUARTER("This Quarter"),
    FINANCIAL_YEAR("FY 2025-26"),
    ALL_TIME("All Time")
}

enum class AgingBucket(val label: String, val badge: String) {
    DAYS_0_15("0 - 15 Days", "Current"),
    DAYS_16_30("16 - 30 Days", "Due Soon"),
    DAYS_31_60("31 - 60 Days", "Overdue"),
    DAYS_60_PLUS("60+ Days", "Critical")
}

data class TaxSlabSummary(
    val gstRate: Double,
    val taxableAmount: Double = 0.0,
    val cgstAmount: Double = 0.0,
    val sgstAmount: Double = 0.0,
    val igstAmount: Double = 0.0,
    val totalTax: Double = 0.0,
    val itemCount: Int = 0
)

data class HsnSummaryItem(
    val hsnCode: String,
    val description: String,
    val totalQuantity: Double,
    val unit: String,
    val taxableAmount: Double,
    val gstRate: Double,
    val totalTax: Double
)

data class Gstr1Summary(
    val totalTaxableSupplies: Double = 0.0,
    val totalCgst: Double = 0.0,
    val totalSgst: Double = 0.0,
    val totalIgst: Double = 0.0,
    val totalTax: Double = 0.0,
    val totalInvoiceValue: Double = 0.0,
    val b2bCount: Int = 0,
    val b2bTaxable: Double = 0.0,
    val b2bTax: Double = 0.0,
    val b2cCount: Int = 0,
    val b2cTaxable: Double = 0.0,
    val b2cTax: Double = 0.0,
    val slabSummaries: List<TaxSlabSummary> = emptyList(),
    val hsnSummaries: List<HsnSummaryItem> = emptyList(),
    val eligibleInvoices: List<Invoice> = emptyList()
)

data class ProfitLossSummary(
    val salesRevenue: Double = 0.0,
    val costOfGoodsSold: Double = 0.0,
    val grossProfit: Double = 0.0,
    val grossMarginPercent: Double = 0.0,
    val totalPurchases: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netProfit: Double = 0.0,
    val netMarginPercent: Double = 0.0,
    val expenseByCategory: List<Pair<String, Double>> = emptyList(),
    val cashInflow: Double = 0.0,
    val cashOutflow: Double = 0.0,
    val netCashflow: Double = 0.0
)

data class Gstr3bSummary(
    val outwardTaxable: Double = 0.0,
    val outwardCgst: Double = 0.0,
    val outwardSgst: Double = 0.0,
    val outwardIgst: Double = 0.0,
    val totalOutputTax: Double = 0.0,
    val inwardTaxable: Double = 0.0,
    val itcCgst: Double = 0.0,
    val itcSgst: Double = 0.0,
    val itcIgst: Double = 0.0,
    val totalInputTaxCredit: Double = 0.0,
    val netCgstPayable: Double = 0.0,
    val netSgstPayable: Double = 0.0,
    val netIgstPayable: Double = 0.0,
    val totalNetGstPayable: Double = 0.0
)

enum class VoucherType(val label: String, val badgeColor: Long) {
    SALE("Sale", 0xFF16A34A),
    PURCHASE("Purchase", 0xFFDC2626),
    RECEIPT("Receipt", 0xFF2563EB),
    PAYMENT("Payment", 0xFFEA580C),
    EXPENSE("Expense", 0xFF9333EA),
    JOURNAL("Journal", 0xFF0891B2),
    CREDIT_NOTE("Credit Note", 0xFF0D9488),
    DEBIT_NOTE("Debit Note", 0xFFC026D3)
}

data class DaybookVoucherEntry(
    val id: String,
    val dateMillis: Long,
    val voucherNumber: String,
    val voucherType: VoucherType,
    val narration: String,
    val debitAccount: String,
    val creditAccount: String,
    val amount: Double,
    val debitAmount: Double = amount,
    val creditAmount: Double = amount,
    val paymentMode: String = "CASH",
    val partyName: String = ""
)

data class DaybookSummary(
    val dateMillis: Long = System.currentTimeMillis(),
    val daySalesTotal: Double = 0.0,
    val daySalesCount: Int = 0,
    val dayPurchasesTotal: Double = 0.0,
    val dayPurchasesCount: Int = 0,
    val dayInvoices: List<Invoice> = emptyList(),
    val dayPurchases: List<PurchaseBill> = emptyList(),
    val dayCashIn: Double = 0.0,
    val dayCashOut: Double = 0.0,
    val dayBankIn: Double = 0.0,
    val dayBankOut: Double = 0.0,
    val dayTransactions: List<Transaction> = emptyList(),
    val vouchers: List<DaybookVoucherEntry> = emptyList(),
    val netDayMovement: Double = 0.0,
    val netCashMovement: Double = 0.0,
    val netBankMovement: Double = 0.0,
    val totalDr: Double = 0.0,
    val totalCr: Double = 0.0,
    val isBalanced: Boolean = true,
    val receiptsTotal: Double = 0.0,
    val paymentsTotal: Double = 0.0,
    val expensesTotal: Double = 0.0,
    val journalsTotal: Double = 0.0,
    val creditNotesTotal: Double = 0.0,
    val debitNotesTotal: Double = 0.0
)

data class PartyAgingItem(
    val partyId: String,
    val partyName: String,
    val phone: String,
    val balanceDue: Double,
    val daysOverdue: Int,
    val bucket: AgingBucket
)

data class PartyAgingSummary(
    val totalReceivable: Double = 0.0,
    val totalPayable: Double = 0.0,
    val bucket0to15: Double = 0.0,
    val bucket16to30: Double = 0.0,
    val bucket31to60: Double = 0.0,
    val bucket60Plus: Double = 0.0,
    val debtorList: List<PartyAgingItem> = emptyList()
)

data class StockValuationSummary(
    val totalItemsCount: Int = 0,
    val totalStockUnits: Double = 0.0,
    val totalSellingValue: Double = 0.0,
    val totalCostValue: Double = 0.0,
    val potentialProfit: Double = 0.0,
    val lowStockCount: Int = 0,
    val outOfStockCount: Int = 0
)

data class PurchasesRegisterSummary(
    val totalBillsCount: Int = 0,
    val totalPurchasesValue: Double = 0.0,
    val totalTaxableAmount: Double = 0.0,
    val totalTaxAmount: Double = 0.0,
    val itcAvailableTax: Double = 0.0,
    val totalPaidAmount: Double = 0.0,
    val totalDueAmount: Double = 0.0,
    val purchases: List<PurchaseBill> = emptyList()
)

data class TrialBalanceAccount(
    val accountCode: String,
    val accountName: String,
    val accountCategory: String, // "Asset", "Liability", "Income", "Expense", "Equity"
    val debitAmount: Double = 0.0,
    val creditAmount: Double = 0.0,
    val note: String = ""
)

data class TrialBalanceSummary(
    val asOfDateMillis: Long = System.currentTimeMillis(),
    val totalDebit: Double = 0.0,
    val totalCredit: Double = 0.0,
    val isBalanced: Boolean = true,
    val difference: Double = 0.0,
    val accounts: List<TrialBalanceAccount> = emptyList(),
    val assetTotal: Double = 0.0,
    val liabilityTotal: Double = 0.0,
    val incomeTotal: Double = 0.0,
    val expenseTotal: Double = 0.0
)

// ==================== CORE 8 PRIORITY REPORTS DATA MODELS ====================

// 1. Daily Sales
data class DailySalesReport(
    val dateMillis: Long = System.currentTimeMillis(),
    val totalSales: Double = 0.0,
    val invoiceCount: Int = 0,
    val cashSales: Double = 0.0,
    val upiSales: Double = 0.0,
    val creditSales: Double = 0.0,
    val totalTax: Double = 0.0,
    val taxableAmount: Double = 0.0,
    val invoices: List<Invoice> = emptyList()
)

// 2. Sales by Date Range
data class SalesDateRangeReport(
    val period: ReportPeriod = ReportPeriod.THIS_MONTH,
    val startDateMillis: Long = 0L,
    val endDateMillis: Long = 0L,
    val totalSales: Double = 0.0,
    val invoiceCount: Int = 0,
    val paidAmount: Double = 0.0,
    val unpaidAmount: Double = 0.0,
    val totalTax: Double = 0.0,
    val cashCollected: Double = 0.0,
    val upiCollected: Double = 0.0,
    val invoices: List<Invoice> = emptyList()
)

// 3. Outstanding Report
data class OutstandingReport(
    val totalReceivable: Double = 0.0, // Amount to receive from customers (येणे)
    val totalPayable: Double = 0.0,    // Amount to pay to suppliers (देणे)
    val netBalance: Double = 0.0,
    val customerCount: Int = 0,
    val supplierCount: Int = 0,
    val customerList: List<PartyWithBalance> = emptyList(),
    val supplierList: List<PartyWithBalance> = emptyList(),
    val aging0to15: Double = 0.0,
    val aging16to30: Double = 0.0,
    val aging31to60: Double = 0.0,
    val aging60Plus: Double = 0.0
)

// 4 & 5. Party Ledger (Customer & Supplier Ledger)
data class LedgerEntryItem(
    val id: String,
    val dateMillis: Long,
    val voucherNo: String,
    val voucherType: String, // "Sale", "Payment", "Purchase", "Return", "Opening"
    val narration: String,
    val debitAmount: Double = 0.0,   // Dr
    val creditAmount: Double = 0.0,  // Cr
    val runningBalance: Double = 0.0,
    val balanceType: String = "Dr"   // "Dr" or "Cr"
)

data class PartyLedgerReport(
    val party: Party? = null,
    val partyType: PartyType = PartyType.CUSTOMER,
    val openingBalance: Double = 0.0,
    val openingBalanceType: String = "Dr",
    val entries: List<LedgerEntryItem> = emptyList(),
    val totalDebit: Double = 0.0,
    val totalCredit: Double = 0.0,
    val closingBalance: Double = 0.0,
    val closingBalanceType: String = "Dr"
)

// 6. Expense Report
data class ExpenseReportSummary(
    val period: ReportPeriod = ReportPeriod.THIS_MONTH,
    val startDateMillis: Long = 0L,
    val endDateMillis: Long = 0L,
    val totalExpenses: Double = 0.0,
    val expenseCount: Int = 0,
    val categoryBreakdown: List<Pair<String, Double>> = emptyList(),
    val modeBreakdown: List<Pair<String, Double>> = emptyList(),
    val expenses: List<Transaction> = emptyList()
)

// 7. Cash Book & Bank Book
data class CashBookEntryItem(
    val id: String,
    val dateMillis: Long,
    val voucherNo: String,
    val particulars: String,
    val voucherType: VoucherType,
    val inAmount: Double = 0.0,      // Debit / Receipt
    val outAmount: Double = 0.0,     // Credit / Payment
    val runningBalance: Double = 0.0,
    val mode: String = "Cash"
)

data class CashBookReportSummary(
    val period: ReportPeriod = ReportPeriod.THIS_MONTH,
    val isBankMode: Boolean = false, // false = Cash Book, true = Bank & UPI Book
    val openingBalance: Double = 0.0,
    val totalInflow: Double = 0.0,   // Total Receipts (Dr)
    val totalOutflow: Double = 0.0,  // Total Payments (Cr)
    val closingBalance: Double = 0.0,
    val entries: List<CashBookEntryItem> = emptyList()
)

data class ReportsUiState(
    val isGstRegistered: Boolean = false,
    val selectedPeriod: ReportPeriod = ReportPeriod.THIS_MONTH,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val selectedReportsTab: Int = 0, // 0 = Core Business Reports, 1 = Advanced & Tax Reports

    // Core 8 Priority Reports
    val dailySales: DailySalesReport = DailySalesReport(),
    val salesDateRange: SalesDateRangeReport = SalesDateRangeReport(),
    val outstanding: OutstandingReport = OutstandingReport(),
    val customerLedger: PartyLedgerReport = PartyLedgerReport(),
    val supplierLedger: PartyLedgerReport = PartyLedgerReport(),
    val expenseReport: ExpenseReportSummary = ExpenseReportSummary(),
    val cashBookReport: CashBookReportSummary = CashBookReportSummary(),
    val daybook: DaybookSummary = DaybookSummary(),

    // Party selection for ledgers
    val allCustomers: List<Party> = emptyList(),
    val allSuppliers: List<Party> = emptyList(),
    val selectedCustomerPartyId: String? = null,
    val selectedSupplierPartyId: String? = null,
    val selectedDailySalesDateMillis: Long = System.currentTimeMillis(),

    // Advanced & Tax Reports
    val gstr1: Gstr1Summary = Gstr1Summary(),
    val gstr3b: Gstr3bSummary = Gstr3bSummary(),
    val profitLoss: ProfitLossSummary = ProfitLossSummary(),
    val partyAging: PartyAgingSummary = PartyAgingSummary(),
    val stockValuation: StockValuationSummary = StockValuationSummary(),
    val purchasesRegister: PurchasesRegisterSummary = PurchasesRegisterSummary(),
    val trialBalance: TrialBalanceSummary = TrialBalanceSummary(),
    val selectedDaybookDateMillis: Long = System.currentTimeMillis()
)
