package com.hisabpro.app.ui.reports

import com.hisabpro.app.data.model.Invoice
import com.hisabpro.app.data.model.Transaction

enum class ReportPeriod(val label: String) {
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    THIS_QUARTER("This Quarter"),
    FINANCIAL_YEAR("FY 2024-25"),
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

data class DaybookSummary(
    val dateMillis: Long = System.currentTimeMillis(),
    val daySalesTotal: Double = 0.0,
    val daySalesCount: Int = 0,
    val dayInvoices: List<Invoice> = emptyList(),
    val dayCashIn: Double = 0.0,
    val dayCashOut: Double = 0.0,
    val dayTransactions: List<Transaction> = emptyList(),
    val netDayMovement: Double = 0.0
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

data class ReportsUiState(
    val selectedPeriod: ReportPeriod = ReportPeriod.THIS_MONTH,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val gstr1: Gstr1Summary = Gstr1Summary(),
    val gstr3b: Gstr3bSummary = Gstr3bSummary(),
    val profitLoss: ProfitLossSummary = ProfitLossSummary(),
    val daybook: DaybookSummary = DaybookSummary(),
    val partyAging: PartyAgingSummary = PartyAgingSummary(),
    val stockValuation: StockValuationSummary = StockValuationSummary(),
    val selectedDaybookDateMillis: Long = System.currentTimeMillis()
)
