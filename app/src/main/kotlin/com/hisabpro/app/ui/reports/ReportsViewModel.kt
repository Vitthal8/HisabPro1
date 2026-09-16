package com.hisabpro.app.ui.reports

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hisabpro.app.data.model.Invoice
import com.hisabpro.app.data.model.InvoiceType
import com.hisabpro.app.data.model.Item
import com.hisabpro.app.data.model.KhataEntry
import com.hisabpro.app.data.model.KhataEntryType
import com.hisabpro.app.data.model.Party
import com.hisabpro.app.data.model.PartyType
import com.hisabpro.app.data.model.PartyWithBalance
import com.hisabpro.app.data.model.PurchaseBill
import com.hisabpro.app.data.model.Transaction
import com.hisabpro.app.data.model.TransactionType
import com.hisabpro.app.data.repository.InvoiceRepository
import com.hisabpro.app.data.repository.ItemRepository
import com.hisabpro.app.data.repository.PartyRepository
import com.hisabpro.app.data.repository.PurchaseRepository
import com.hisabpro.app.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.max

class ReportsViewModel(application: Application) : AndroidViewModel(application) {

    private val invoiceRepo = InvoiceRepository.getInstance(application.applicationContext)
    private val transactionRepo = TransactionRepository.getInstance(application.applicationContext)
    private val purchaseRepo = PurchaseRepository.getInstance(application.applicationContext)
    private val partyRepo = PartyRepository.getInstance(application.applicationContext)
    private val itemRepo = ItemRepository.getInstance(application.applicationContext)

    private val _selectedPeriod = MutableStateFlow(ReportPeriod.THIS_MONTH)
    private val _selectedDaybookDate = MutableStateFlow(System.currentTimeMillis())

    private data class BusinessEntities(
        val invoices: List<Invoice>,
        val transactions: List<Transaction>,
        val purchases: List<PurchaseBill>,
        val parties: List<Party>,
        val entries: List<KhataEntry>,
        val items: List<Item>
    )

    private val entitiesFlow = combine(
        combine(invoiceRepo.invoices, transactionRepo.transactions, purchaseRepo.purchases) { inv, tx, pur ->
            Triple(inv, tx, pur)
        },
        partyRepo.parties,
        partyRepo.entries,
        itemRepo.items
    ) { (invoices, transactions, purchases), parties, entries, items ->
        BusinessEntities(
            invoices = invoices,
            transactions = transactions,
            purchases = purchases,
            parties = parties,
            entries = entries,
            items = items
        )
    }

    val uiState: StateFlow<ReportsUiState> = combine(
        entitiesFlow,
        _selectedPeriod,
        _selectedDaybookDate
    ) { entities, period, daybookDate ->
        buildReportsUiState(
            invoices = entities.invoices,
            transactions = entities.transactions,
            purchases = entities.purchases,
            parties = entities.parties,
            entries = entities.entries,
            items = entities.items,
            period = period,
            daybookDate = daybookDate
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportsUiState()
    )

    fun setPeriod(period: ReportPeriod) {
        _selectedPeriod.value = period
    }

    fun setDaybookDate(dateMillis: Long) {
        _selectedDaybookDate.value = dateMillis
    }

    private fun buildReportsUiState(
        invoices: List<Invoice>,
        transactions: List<Transaction>,
        purchases: List<PurchaseBill>,
        parties: List<Party>,
        entries: List<KhataEntry>,
        items: List<Item>,
        period: ReportPeriod,
        daybookDate: Long
    ): ReportsUiState {
        val (startTime, endTime) = calculatePeriodBounds(period)

        // Filter invoices, transactions & purchases in chosen period
        val periodInvoices = invoices.filter {
            it.dateMillis in startTime..endTime && it.type != InvoiceType.PROFORMA
        }
        val periodTransactions = transactions.filter {
            it.dateMillis in startTime..endTime
        }
        val periodPurchases = purchases.filter {
            it.dateMillis in startTime..endTime
        }

        // Calculate parties with balance
        val partiesWithBalances = parties.map { party ->
            val partyEntries = entries.filter { it.partyId == party.id }
            var totalGave = 0.0
            var totalGot = 0.0
            var lastDate: Long? = null

            for (entry in partyEntries) {
                if (entry.type == KhataEntryType.YOU_GAVE) {
                    totalGave += entry.amount
                } else {
                    totalGot += entry.amount
                }
                if (lastDate == null || entry.dateMillis > lastDate) {
                    lastDate = entry.dateMillis
                }
            }
            PartyWithBalance(
                party = party,
                totalGave = totalGave,
                totalGot = totalGot,
                netBalance = totalGave - totalGot,
                lastEntryDateMillis = lastDate
            )
        }

        val gstr1 = calculateGstr1(periodInvoices)
        val gstr3b = calculateGstr3b(periodInvoices, periodPurchases)
        val profitLoss = calculateProfitLoss(periodInvoices, periodTransactions, periodPurchases, items)
        val daybook = calculateDaybook(invoices, transactions, daybookDate)
        val partyAging = calculatePartyAging(partiesWithBalances)
        val stockValuation = calculateStockValuation(items)

        return ReportsUiState(
            selectedPeriod = period,
            gstr1 = gstr1,
            gstr3b = gstr3b,
            profitLoss = profitLoss,
            daybook = daybook,
            partyAging = partyAging,
            stockValuation = stockValuation,
            selectedDaybookDateMillis = daybookDate
        )
    }

    private fun calculateGstr1(invoices: List<Invoice>): Gstr1Summary {
        var totalTaxable = 0.0
        var totalCgst = 0.0
        var totalSgst = 0.0
        var totalIgst = 0.0
        var totalTax = 0.0
        var totalValue = 0.0

        var b2bCount = 0
        var b2bTaxable = 0.0
        var b2bTax = 0.0

        var b2cCount = 0
        var b2cTaxable = 0.0
        var b2cTax = 0.0

        val slabMap = mutableMapOf<Double, MutableList<Pair<Double, Double>>>() // rate -> list of (taxable, tax)
        val hsnMap = mutableMapOf<String, HsnAccumulator>()

        for (inv in invoices) {
            val invTaxable = inv.subtotal
            val invTax = inv.totalTax
            val invTotal = inv.grandTotal

            totalTaxable += invTaxable
            totalCgst += inv.cgstTotal
            totalSgst += inv.sgstTotal
            totalIgst += inv.igstTotal
            totalTax += invTax
            totalValue += invTotal

            val isB2b = inv.customerGstin.isNotBlank()
            if (isB2b) {
                b2bCount++
                b2bTaxable += invTaxable
                b2bTax += invTax
            } else {
                b2cCount++
                b2cTaxable += invTaxable
                b2cTax += invTax
            }

            // Items breakdown
            for (line in inv.items) {
                val lineTax = line.getTaxAmount(inv.gstMode)
                val rate = line.gstRate
                val list = slabMap.getOrPut(rate) { mutableListOf() }
                list.add(Pair(line.taxableAmount, lineTax))

                // HSN summary
                val hsn = if (line.hsnCode.isNotBlank()) line.hsnCode else "NA"
                val acc = hsnMap.getOrPut(hsn) {
                    HsnAccumulator(
                        hsnCode = hsn,
                        description = line.description,
                        unit = line.unit,
                        gstRate = line.gstRate
                    )
                }
                acc.totalQty += line.quantity
                acc.totalTaxable += line.taxableAmount
                acc.totalTax += lineTax
            }
        }

        // Standard slabs: 0%, 5%, 12%, 18%, 28%
        val standardSlabs = listOf(0.0, 5.0, 12.0, 18.0, 28.0)
        val slabSummaries = standardSlabs.map { rate ->
            val entries = slabMap[rate] ?: emptyList()
            val taxable = entries.sumOf { it.first }
            val tax = entries.sumOf { it.second }
            TaxSlabSummary(
                gstRate = rate,
                taxableAmount = taxable,
                cgstAmount = tax / 2.0,
                sgstAmount = tax / 2.0,
                igstAmount = 0.0,
                totalTax = tax,
                itemCount = entries.size
            )
        }

        val hsnSummaries = hsnMap.values.map {
            HsnSummaryItem(
                hsnCode = it.hsnCode,
                description = it.description,
                totalQuantity = it.totalQty,
                unit = it.unit,
                taxableAmount = it.totalTaxable,
                gstRate = it.gstRate,
                totalTax = it.totalTax
            )
        }.sortedByDescending { it.taxableAmount }

        return Gstr1Summary(
            totalTaxableSupplies = totalTaxable,
            totalCgst = totalCgst,
            totalSgst = totalSgst,
            totalIgst = totalIgst,
            totalTax = totalTax,
            totalInvoiceValue = totalValue,
            b2bCount = b2bCount,
            b2bTaxable = b2bTaxable,
            b2bTax = b2bTax,
            b2cCount = b2cCount,
            b2cTaxable = b2cTaxable,
            b2cTax = b2cTax,
            slabSummaries = slabSummaries,
            hsnSummaries = hsnSummaries,
            eligibleInvoices = invoices
        )
    }

    private fun calculateGstr3b(
        invoices: List<Invoice>,
        purchases: List<PurchaseBill>
    ): Gstr3bSummary {
        val outwardTaxable = invoices.sumOf { it.subtotal }
        val outwardCgst = invoices.sumOf { it.cgstTotal }
        val outwardSgst = invoices.sumOf { it.sgstTotal }
        val outwardIgst = invoices.sumOf { it.igstTotal }
        val totalOutputTax = invoices.sumOf { it.totalTax }

        val eligiblePurchases = purchases.filter { it.itcEligible }
        val inwardTaxable = eligiblePurchases.sumOf { it.subtotal }
        val itcCgst = eligiblePurchases.sumOf { it.cgstTotal }
        val itcSgst = eligiblePurchases.sumOf { it.sgstTotal }
        val itcIgst = eligiblePurchases.sumOf { it.igstTotal }
        val totalItc = eligiblePurchases.sumOf { it.totalTax }

        val netCgst = max(0.0, outwardCgst - itcCgst)
        val netSgst = max(0.0, outwardSgst - itcSgst)
        val netIgst = max(0.0, outwardIgst - itcIgst)
        val totalNet = max(0.0, totalOutputTax - totalItc)

        return Gstr3bSummary(
            outwardTaxable = outwardTaxable,
            outwardCgst = outwardCgst,
            outwardSgst = outwardSgst,
            outwardIgst = outwardIgst,
            totalOutputTax = totalOutputTax,
            inwardTaxable = inwardTaxable,
            itcCgst = itcCgst,
            itcSgst = itcSgst,
            itcIgst = itcIgst,
            totalInputTaxCredit = totalItc,
            netCgstPayable = netCgst,
            netSgstPayable = netSgst,
            netIgstPayable = netIgst,
            totalNetGstPayable = totalNet
        )
    }

    private fun calculateProfitLoss(
        invoices: List<Invoice>,
        transactions: List<Transaction>,
        purchases: List<PurchaseBill>,
        items: List<Item>
    ): ProfitLossSummary {
        val salesRevenue = invoices.sumOf { it.subtotal } // Net turnover before GST
        val totalPurchases = purchases.sumOf { it.grandTotal }

        // Calculate COGS
        val itemsMap = items.associateBy { it.name.trim().lowercase() }
        var totalCogs = 0.0

        for (inv in invoices) {
            for (line in inv.items) {
                val matched = itemsMap[line.description.trim().lowercase()]
                if (matched != null && matched.purchasePrice > 0) {
                    totalCogs += matched.purchasePrice * line.quantity
                } else {
                    // Estimate 65% purchase cost if not matched directly
                    totalCogs += line.taxableAmount * 0.65
                }
            }
        }

        val grossProfit = max(0.0, salesRevenue - totalCogs)
        val grossMarginPercent = if (salesRevenue > 0) (grossProfit / salesRevenue) * 100.0 else 0.0

        // Operating Expenses from Cashbook
        val expenseTx = transactions.filter { it.type == TransactionType.EXPENSE }
        val totalExpenses = expenseTx.sumOf { it.amount }

        val expenseByCategory = expenseTx
            .groupBy { it.category.label }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }

        val netProfit = grossProfit - totalExpenses
        val netMarginPercent = if (salesRevenue > 0) (netProfit / salesRevenue) * 100.0 else 0.0

        // Cash flow: Cash collected from invoices + Income transactions vs Expense transactions + Purchase cash paid
        val invoiceCollections = invoices.sumOf { it.paidAmount }
        val otherIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val cashInflow = invoiceCollections + otherIncome
        val purchaseCashPaid = purchases.sumOf { it.paidAmount }
        val cashOutflow = totalExpenses + purchaseCashPaid
        val netCashflow = cashInflow - cashOutflow

        return ProfitLossSummary(
            salesRevenue = salesRevenue,
            costOfGoodsSold = totalCogs,
            grossProfit = grossProfit,
            grossMarginPercent = grossMarginPercent,
            totalPurchases = totalPurchases,
            totalExpenses = totalExpenses,
            netProfit = netProfit,
            netMarginPercent = netMarginPercent,
            expenseByCategory = expenseByCategory,
            cashInflow = cashInflow,
            cashOutflow = cashOutflow,
            netCashflow = netCashflow
        )
    }

    private fun calculateDaybook(
        allInvoices: List<Invoice>,
        allTransactions: List<Transaction>,
        dateMillis: Long
    ): DaybookSummary {
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

        val dayInvoices = allInvoices.filter {
            it.dateMillis in startOfDay..endOfDay && it.type != InvoiceType.PROFORMA
        }
        val daySalesTotal = dayInvoices.sumOf { it.grandTotal }

        val dayTransactions = allTransactions.filter {
            it.dateMillis in startOfDay..endOfDay
        }

        val dayCashIn = dayInvoices.sumOf { it.paidAmount } +
            dayTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val dayCashOut = dayTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

        return DaybookSummary(
            dateMillis = dateMillis,
            daySalesTotal = daySalesTotal,
            daySalesCount = dayInvoices.size,
            dayInvoices = dayInvoices,
            dayCashIn = dayCashIn,
            dayCashOut = dayCashOut,
            dayTransactions = dayTransactions,
            netDayMovement = dayCashIn - dayCashOut
        )
    }

    private fun calculatePartyAging(parties: List<PartyWithBalance>): PartyAgingSummary {
        var totalReceivable = 0.0
        var totalPayable = 0.0

        var bucket0to15 = 0.0
        var bucket16to30 = 0.0
        var bucket31to60 = 0.0
        var bucket60Plus = 0.0

        val debtors = mutableListOf<PartyAgingItem>()
        val now = System.currentTimeMillis()

        for (p in parties) {
            val net = p.netBalance
            if (net > 0.01) {
                // Customer owes money
                totalReceivable += net

                // Estimate age from last entry or creation
                val lastActivity = p.lastEntryDateMillis ?: p.party.createdAt
                val daysOld = max(1, ((now - lastActivity) / (1000L * 60 * 60 * 24)).toInt())

                val bucket = when {
                    daysOld <= 15 -> {
                        bucket0to15 += net
                        AgingBucket.DAYS_0_15
                    }
                    daysOld <= 30 -> {
                        bucket16to30 += net
                        AgingBucket.DAYS_16_30
                    }
                    daysOld <= 60 -> {
                        bucket31to60 += net
                        AgingBucket.DAYS_31_60
                    }
                    else -> {
                        bucket60Plus += net
                        AgingBucket.DAYS_60_PLUS
                    }
                }

                debtors.add(
                    PartyAgingItem(
                        partyId = p.party.id,
                        partyName = p.party.name,
                        phone = p.party.phone,
                        balanceDue = net,
                        daysOverdue = daysOld,
                        bucket = bucket
                    )
                )
            } else if (net < -0.01) {
                // We owe supplier
                totalPayable += abs(net)
            }
        }

        debtors.sortByDescending { it.balanceDue }

        return PartyAgingSummary(
            totalReceivable = totalReceivable,
            totalPayable = totalPayable,
            bucket0to15 = bucket0to15,
            bucket16to30 = bucket16to30,
            bucket31to60 = bucket31to60,
            bucket60Plus = bucket60Plus,
            debtorList = debtors
        )
    }

    private fun calculateStockValuation(items: List<Item>): StockValuationSummary {
        val totalCount = items.size
        val totalUnits = items.sumOf { it.currentStock }
        val totalSelling = items.sumOf { it.currentStock * it.salePrice }
        val totalCost = items.sumOf { it.currentStock * it.purchasePrice }
        val profit = max(0.0, totalSelling - totalCost)
        val lowStock = items.count { it.isLowStock }
        val outOfStock = items.count { it.isOutOfStock }

        return StockValuationSummary(
            totalItemsCount = totalCount,
            totalStockUnits = totalUnits,
            totalSellingValue = totalSelling,
            totalCostValue = totalCost,
            potentialProfit = profit,
            lowStockCount = lowStock,
            outOfStockCount = outOfStock
        )
    }

    private fun calculatePeriodBounds(period: ReportPeriod): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        val now = System.currentTimeMillis()

        return when (period) {
            ReportPeriod.TODAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                val end = start + 86400000L - 1
                Pair(start, end)
            }
            ReportPeriod.THIS_WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                Pair(cal.timeInMillis, now)
            }
            ReportPeriod.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.add(Calendar.MONTH, 1)
                val end = cal.timeInMillis - 1
                Pair(start, end)
            }
            ReportPeriod.LAST_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.add(Calendar.MONTH, -1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.add(Calendar.MONTH, 1)
                val end = cal.timeInMillis - 1
                Pair(start, end)
            }
            ReportPeriod.THIS_QUARTER -> {
                val month = cal.get(Calendar.MONTH) // 0-11
                val quarterStartMonth = (month / 3) * 3
                cal.set(Calendar.MONTH, quarterStartMonth)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.add(Calendar.MONTH, 3)
                val end = cal.timeInMillis - 1
                Pair(start, end)
            }
            ReportPeriod.FINANCIAL_YEAR -> {
                val year = cal.get(Calendar.YEAR)
                val month = cal.get(Calendar.MONTH)
                // Indian FY starts on April 1st (month index 3)
                val fyStartYear = if (month >= Calendar.APRIL) year else year - 1
                cal.set(Calendar.YEAR, fyStartYear)
                cal.set(Calendar.MONTH, Calendar.APRIL)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.add(Calendar.YEAR, 1)
                val end = cal.timeInMillis - 1
                Pair(start, end)
            }
            ReportPeriod.ALL_TIME -> {
                Pair(0L, Long.MAX_VALUE)
            }
        }
    }

    private class HsnAccumulator(
        val hsnCode: String,
        val description: String,
        val unit: String,
        val gstRate: Double,
        var totalQty: Double = 0.0,
        var totalTaxable: Double = 0.0,
        var totalTax: Double = 0.0
    )
}
