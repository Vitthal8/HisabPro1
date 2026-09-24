package com.hisabpro.app.ui.reports

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hisabpro.app.data.model.Category
import com.hisabpro.app.data.model.Invoice
import com.hisabpro.app.data.model.InvoiceStatus
import com.hisabpro.app.data.model.InvoiceType
import com.hisabpro.app.data.model.Item
import com.hisabpro.app.data.model.KhataEntry
import com.hisabpro.app.data.model.KhataEntryType
import com.hisabpro.app.data.model.Party
import com.hisabpro.app.data.model.PartyType
import com.hisabpro.app.data.model.PartyWithBalance
import com.hisabpro.app.data.model.PaymentMode
import com.hisabpro.app.data.model.PurchaseBill
import com.hisabpro.app.data.model.Transaction
import com.hisabpro.app.data.model.TransactionType
import com.hisabpro.app.data.repository.InvoiceRepository
import com.hisabpro.app.data.repository.ItemRepository
import com.hisabpro.app.data.repository.PartyRepository
import com.hisabpro.app.data.repository.PurchaseRepository
import com.hisabpro.app.data.repository.SettingsRepository
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
    private val settingsRepo = SettingsRepository.getInstance(application.applicationContext)

    private val _selectedPeriod = MutableStateFlow(ReportPeriod.THIS_MONTH)
    private val _selectedDaybookDate = MutableStateFlow(System.currentTimeMillis())
    private val _selectedDailySalesDate = MutableStateFlow(System.currentTimeMillis())
    private val _selectedReportsTab = MutableStateFlow(0) // 0 = Simple Business Reports, 1 = Advanced & Tax
    private val _selectedCustomerPartyId = MutableStateFlow<String?>(null)
    private val _selectedSupplierPartyId = MutableStateFlow<String?>(null)
    private val _isCashBookBankMode = MutableStateFlow(false)

    private data class BusinessEntities(
        val invoices: List<Invoice>,
        val transactions: List<Transaction>,
        val purchases: List<PurchaseBill>,
        val parties: List<Party>,
        val entries: List<KhataEntry>,
        val items: List<Item>
    )

    private data class ReportFilterParams(
        val period: ReportPeriod,
        val tab: Int,
        val dailySalesDate: Long,
        val daybookDate: Long,
        val customerPartyId: String?,
        val supplierPartyId: String?,
        val isBankMode: Boolean
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

    private val filtersFlow = combine(
        combine(_selectedPeriod, _selectedReportsTab, _selectedDailySalesDate) { p, tab, dDate ->
            Triple(p, tab, dDate)
        },
        combine(_selectedDaybookDate, _selectedCustomerPartyId, _selectedSupplierPartyId) { dbDate, custId, suppId ->
            Triple(dbDate, custId, suppId)
        },
        _isCashBookBankMode
    ) { (p, tab, dDate), (dbDate, custId, suppId), isBank ->
        ReportFilterParams(
            period = p,
            tab = tab,
            dailySalesDate = dDate,
            daybookDate = dbDate,
            customerPartyId = custId,
            supplierPartyId = suppId,
            isBankMode = isBank
        )
    }

    val uiState: StateFlow<ReportsUiState> = combine(
        entitiesFlow,
        filtersFlow,
        settingsRepo.profile
    ) { entities, filters, profile ->
        buildReportsUiState(
            entities = entities,
            filters = filters,
            isGstRegistered = profile.isGstRegistered
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportsUiState()
    )

    fun setPeriod(period: ReportPeriod) {
        _selectedPeriod.value = period
    }

    fun setReportsTab(tabIndex: Int) {
        _selectedReportsTab.value = tabIndex
    }

    fun setDailySalesDate(dateMillis: Long) {
        _selectedDailySalesDate.value = dateMillis
    }

    fun shiftDailySalesDate(days: Int) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = _selectedDailySalesDate.value
            add(Calendar.DAY_OF_YEAR, days)
        }
        _selectedDailySalesDate.value = cal.timeInMillis
    }

    fun setSelectedCustomerPartyId(partyId: String?) {
        _selectedCustomerPartyId.value = partyId
    }

    fun setSelectedSupplierPartyId(partyId: String?) {
        _selectedSupplierPartyId.value = partyId
    }

    fun setCashBookBankMode(isBank: Boolean) {
        _isCashBookBankMode.value = isBank
    }

    fun setDaybookDate(dateMillis: Long) {
        _selectedDaybookDate.value = dateMillis
    }

    private fun buildReportsUiState(
        entities: BusinessEntities,
        filters: ReportFilterParams,
        isGstRegistered: Boolean
    ): ReportsUiState {
        val invoices = entities.invoices
        val transactions = entities.transactions
        val purchases = entities.purchases
        val parties = entities.parties
        val entries = entities.entries
        val items = entities.items

        val (startTime, endTime) = calculatePeriodBounds(filters.period)

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

        val allCustomers = parties.filter { it.type == PartyType.CUSTOMER || it.type == PartyType.BOTH }
        val allSuppliers = parties.filter { it.type == PartyType.SUPPLIER || it.type == PartyType.BOTH }

        // Core 8 Priority Reports
        val dailySales = calculateDailySalesReport(invoices, filters.dailySalesDate)
        val salesDateRange = calculateSalesDateRangeReport(periodInvoices, filters.period, startTime, endTime)
        val outstanding = calculateOutstandingReport(partiesWithBalances)
        val customerLedger = calculateCustomerLedger(
            targetPartyId = filters.customerPartyId ?: allCustomers.firstOrNull()?.id,
            parties = parties,
            entries = entries,
            invoices = invoices,
            startTime = startTime,
            endTime = endTime
        )
        val supplierLedger = calculateSupplierLedger(
            targetPartyId = filters.supplierPartyId ?: allSuppliers.firstOrNull()?.id,
            parties = parties,
            entries = entries,
            purchases = purchases,
            startTime = startTime,
            endTime = endTime
        )
        val expenseReport = calculateExpenseReport(periodTransactions, filters.period, startTime, endTime)
        val cashBookReport = calculateCashBookReport(
            invoices = invoices,
            purchases = purchases,
            transactions = transactions,
            entries = entries,
            parties = parties,
            isBankMode = filters.isBankMode,
            period = filters.period,
            startTime = startTime,
            endTime = endTime
        )
        val daybook = calculateDaybook(
            allInvoices = invoices,
            allPurchases = purchases,
            allTransactions = transactions,
            allKhataEntries = entries,
            parties = parties,
            dateMillis = filters.daybookDate
        )

        // Advanced Reports
        val gstr1 = calculateGstr1(periodInvoices)
        val gstr3b = calculateGstr3b(periodInvoices, periodPurchases)
        val profitLoss = calculateProfitLoss(periodInvoices, periodTransactions, periodPurchases, items)
        val partyAging = calculatePartyAging(partiesWithBalances)
        val stockValuation = calculateStockValuation(items)
        val purchasesRegister = calculatePurchasesRegister(periodPurchases)
        val trialBalance = calculateTrialBalance(invoices, purchases, transactions, partiesWithBalances, items)

        return ReportsUiState(
            isGstRegistered = isGstRegistered,
            selectedPeriod = filters.period,
            selectedReportsTab = filters.tab,
            dailySales = dailySales,
            salesDateRange = salesDateRange,
            outstanding = outstanding,
            customerLedger = customerLedger,
            supplierLedger = supplierLedger,
            expenseReport = expenseReport,
            cashBookReport = cashBookReport,
            daybook = daybook,
            allCustomers = allCustomers,
            allSuppliers = allSuppliers,
            selectedCustomerPartyId = filters.customerPartyId ?: allCustomers.firstOrNull()?.id,
            selectedSupplierPartyId = filters.supplierPartyId ?: allSuppliers.firstOrNull()?.id,
            selectedDailySalesDateMillis = filters.dailySalesDate,
            gstr1 = gstr1,
            gstr3b = gstr3b,
            profitLoss = profitLoss,
            partyAging = partyAging,
            stockValuation = stockValuation,
            purchasesRegister = purchasesRegister,
            trialBalance = trialBalance,
            selectedDaybookDateMillis = filters.daybookDate
        )
    }

    // 1. Daily Sales
    private fun calculateDailySalesReport(allInvoices: List<Invoice>, dateMillis: Long): DailySalesReport {
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
        val totalSales = dayInvoices.sumOf { it.grandTotal }
        val taxable = dayInvoices.sumOf { it.subtotal }
        val totalTax = dayInvoices.sumOf { it.totalTax }

        var cashSales = 0.0
        var upiSales = 0.0
        var creditSales = 0.0

        for (inv in dayInvoices) {
            when (inv.paymentStatus) {
                InvoiceStatus.PAID -> {
                    if (inv.notes.contains("UPI", ignoreCase = true) || inv.notes.contains("Online", ignoreCase = true) || inv.notes.contains("GPay", ignoreCase = true) || inv.notes.contains("PhonePe", ignoreCase = true)) {
                        upiSales += inv.grandTotal
                    } else {
                        cashSales += inv.grandTotal
                    }
                }
                InvoiceStatus.PARTIAL -> {
                    cashSales += inv.paidAmount
                    creditSales += inv.dueAmount
                }
                InvoiceStatus.UNPAID -> {
                    creditSales += inv.grandTotal
                }
            }
        }

        return DailySalesReport(
            dateMillis = dateMillis,
            totalSales = totalSales,
            invoiceCount = dayInvoices.size,
            cashSales = cashSales,
            upiSales = upiSales,
            creditSales = creditSales,
            totalTax = totalTax,
            taxableAmount = taxable,
            invoices = dayInvoices.sortedByDescending { it.dateMillis }
        )
    }

    // 2. Sales by Date Range
    private fun calculateSalesDateRangeReport(
        periodInvoices: List<Invoice>,
        period: ReportPeriod,
        startTime: Long,
        endTime: Long
    ): SalesDateRangeReport {
        val totalSales = periodInvoices.sumOf { it.grandTotal }
        val paidAmount = periodInvoices.sumOf { it.paidAmount }
        val unpaidAmount = periodInvoices.sumOf { it.dueAmount }
        val totalTax = periodInvoices.sumOf { it.totalTax }

        var cashCollected = 0.0
        var upiCollected = 0.0

        for (inv in periodInvoices) {
            if (inv.paidAmount > 0) {
                if (inv.notes.contains("UPI", ignoreCase = true) || inv.notes.contains("Online", ignoreCase = true)) {
                    upiCollected += inv.paidAmount
                } else {
                    cashCollected += inv.paidAmount
                }
            }
        }

        return SalesDateRangeReport(
            period = period,
            startDateMillis = startTime,
            endDateMillis = endTime,
            totalSales = totalSales,
            invoiceCount = periodInvoices.size,
            paidAmount = paidAmount,
            unpaidAmount = unpaidAmount,
            totalTax = totalTax,
            cashCollected = cashCollected,
            upiCollected = upiCollected,
            invoices = periodInvoices.sortedByDescending { it.dateMillis }
        )
    }

    // 3. Outstanding Report
    private fun calculateOutstandingReport(partiesWithBalances: List<PartyWithBalance>): OutstandingReport {
        val customerList = partiesWithBalances.filter {
            (it.party.type == PartyType.CUSTOMER || it.party.type == PartyType.BOTH) && it.netBalance > 0.009
        }.sortedByDescending { it.netBalance }

        val supplierList = partiesWithBalances.filter {
            (it.party.type == PartyType.SUPPLIER || it.party.type == PartyType.BOTH) && it.netBalance < -0.009
        }.sortedByDescending { abs(it.netBalance) }

        val totalReceivable = customerList.sumOf { it.netBalance }
        val totalPayable = supplierList.sumOf { abs(it.netBalance) }
        val netBalance = totalReceivable - totalPayable

        val now = System.currentTimeMillis()
        var b0to15 = 0.0
        var b16to30 = 0.0
        var b31to60 = 0.0
        var b60Plus = 0.0

        for (cust in customerList) {
            val lastDate = cust.lastEntryDateMillis ?: cust.party.createdAt
            val days = ((now - lastDate) / (1000L * 60 * 60 * 24)).toInt().coerceAtLeast(0)
            when {
                days <= 15 -> b0to15 += cust.netBalance
                days <= 30 -> b16to30 += cust.netBalance
                days <= 60 -> b31to60 += cust.netBalance
                else -> b60Plus += cust.netBalance
            }
        }

        return OutstandingReport(
            totalReceivable = totalReceivable,
            totalPayable = totalPayable,
            netBalance = netBalance,
            customerCount = customerList.size,
            supplierCount = supplierList.size,
            customerList = customerList,
            supplierList = supplierList,
            aging0to15 = b0to15,
            aging16to30 = b16to30,
            aging31to60 = b31to60,
            aging60Plus = b60Plus
        )
    }

    // 4. Customer Ledger
    private fun calculateCustomerLedger(
        targetPartyId: String?,
        parties: List<Party>,
        entries: List<KhataEntry>,
        invoices: List<Invoice>,
        startTime: Long,
        endTime: Long
    ): PartyLedgerReport {
        val party = parties.find { it.id == targetPartyId } ?: return PartyLedgerReport()

        // Prior entries before startTime form the opening balance
        val priorEntries = entries.filter { it.partyId == party.id && it.dateMillis < startTime }
        var priorGave = 0.0
        var priorGot = 0.0
        for (e in priorEntries) {
            if (e.type == KhataEntryType.YOU_GAVE) priorGave += e.amount else priorGot += e.amount
        }
        val openingBalance = priorGave - priorGot
        val openingType = if (openingBalance >= 0) "Dr" else "Cr"

        // Current period entries
        val periodEntries = entries.filter { it.partyId == party.id && it.dateMillis in startTime..endTime }

        var running = openingBalance
        var totalDr = 0.0
        var totalCr = 0.0

        val ledgerItems = mutableListOf<LedgerEntryItem>()

        // Sort chronologically ascending
        val sortedEntries = periodEntries.sortedBy { it.dateMillis }

        for (e in sortedEntries) {
            val isDr = e.type == KhataEntryType.YOU_GAVE
            val dr = if (isDr) e.amount else 0.0
            val cr = if (!isDr) e.amount else 0.0

            totalDr += dr
            totalCr += cr
            running += (dr - cr)

            val balType = if (running >= 0) "Dr" else "Cr"
            ledgerItems.add(
                LedgerEntryItem(
                    id = e.id,
                    dateMillis = e.dateMillis,
                    voucherNo = e.billNumber.ifBlank { "REC" },
                    voucherType = if (isDr) "Sale / Debit" else "Receipt (जमा)",
                    narration = e.note.ifBlank { if (isDr) "Goods sold on credit" else "Payment received" },
                    debitAmount = dr,
                    creditAmount = cr,
                    runningBalance = abs(running),
                    balanceType = balType
                )
            )
        }

        val closingBal = abs(running)
        val closingType = if (running >= 0) "Dr" else "Cr"

        return PartyLedgerReport(
            party = party,
            partyType = PartyType.CUSTOMER,
            openingBalance = abs(openingBalance),
            openingBalanceType = openingType,
            entries = ledgerItems.reversed(), // Show newest first in table
            totalDebit = totalDr,
            totalCredit = totalCr,
            closingBalance = closingBal,
            closingBalanceType = closingType
        )
    }

    // 5. Supplier Ledger
    private fun calculateSupplierLedger(
        targetPartyId: String?,
        parties: List<Party>,
        entries: List<KhataEntry>,
        purchases: List<PurchaseBill>,
        startTime: Long,
        endTime: Long
    ): PartyLedgerReport {
        val party = parties.find { it.id == targetPartyId } ?: return PartyLedgerReport(partyType = PartyType.SUPPLIER)

        // Prior entries before startTime form the opening balance
        val priorEntries = entries.filter { it.partyId == party.id && it.dateMillis < startTime }
        var priorPaid = 0.0   // You gave money to supplier -> Dr
        var priorInward = 0.0 // You got goods from supplier -> Cr
        for (e in priorEntries) {
            if (e.type == KhataEntryType.YOU_GAVE) priorPaid += e.amount else priorInward += e.amount
        }
        // For supplier, Cr (goods received) increases payable, Dr (payments) reduces payable
        val openingPayable = priorInward - priorPaid
        val openingType = if (openingPayable >= 0) "Cr" else "Dr"

        val periodEntries = entries.filter { it.partyId == party.id && it.dateMillis in startTime..endTime }

        var runningPayable = openingPayable
        var totalDr = 0.0
        var totalCr = 0.0

        val ledgerItems = mutableListOf<LedgerEntryItem>()

        val sortedEntries = periodEntries.sortedBy { it.dateMillis }

        for (e in sortedEntries) {
            // YOU_GAVE = Payment to supplier (Dr)
            // YOU_GOT = Inward purchase from supplier (Cr)
            val isPayment = e.type == KhataEntryType.YOU_GAVE
            val dr = if (isPayment) e.amount else 0.0
            val cr = if (!isPayment) e.amount else 0.0

            totalDr += dr
            totalCr += cr
            runningPayable += (cr - dr)

            val balType = if (runningPayable >= 0) "Cr" else "Dr"
            ledgerItems.add(
                LedgerEntryItem(
                    id = e.id,
                    dateMillis = e.dateMillis,
                    voucherNo = e.billNumber.ifBlank { "PUR" },
                    voucherType = if (isPayment) "Payment (नावे)" else "Purchase (खरेदी)",
                    narration = e.note.ifBlank { if (isPayment) "Payment to supplier" else "Stock inward bill" },
                    debitAmount = dr,
                    creditAmount = cr,
                    runningBalance = abs(runningPayable),
                    balanceType = balType
                )
            )
        }

        val closingBal = abs(runningPayable)
        val closingType = if (runningPayable >= 0) "Cr" else "Dr"

        return PartyLedgerReport(
            party = party,
            partyType = PartyType.SUPPLIER,
            openingBalance = abs(openingPayable),
            openingBalanceType = openingType,
            entries = ledgerItems.reversed(),
            totalDebit = totalDr,
            totalCredit = totalCr,
            closingBalance = closingBal,
            closingBalanceType = closingType
        )
    }

    // 6. Expense Report
    private fun calculateExpenseReport(
        transactions: List<Transaction>,
        period: ReportPeriod,
        startTime: Long,
        endTime: Long
    ): ExpenseReportSummary {
        val expenseTx = transactions.filter {
            it.type == TransactionType.EXPENSE && it.dateMillis in startTime..endTime
        }
        val totalExpenses = expenseTx.sumOf { it.amount }
        val categoryBreakdown = expenseTx
            .groupBy { it.category.label }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }

        val modeBreakdown = expenseTx
            .groupBy { it.paymentMode.label }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }

        return ExpenseReportSummary(
            period = period,
            startDateMillis = startTime,
            endDateMillis = endTime,
            totalExpenses = totalExpenses,
            expenseCount = expenseTx.size,
            categoryBreakdown = categoryBreakdown,
            modeBreakdown = modeBreakdown,
            expenses = expenseTx.sortedByDescending { it.dateMillis }
        )
    }

    // 7. Cash Book & Bank Book
    private fun calculateCashBookReport(
        invoices: List<Invoice>,
        purchases: List<PurchaseBill>,
        transactions: List<Transaction>,
        entries: List<KhataEntry>,
        parties: List<Party>,
        isBankMode: Boolean,
        period: ReportPeriod,
        startTime: Long,
        endTime: Long
    ): CashBookReportSummary {
        val domainResult = com.hisabpro.app.domain.accounting.CashBookCalculator.calculate(
            invoices = invoices,
            purchases = purchases,
            transactions = transactions,
            khataEntries = entries,
            parties = parties,
            isBankMode = isBankMode,
            startDateMillis = startTime,
            endDateMillis = endTime
        )

        return CashBookReportSummary(
            period = period,
            isBankMode = isBankMode,
            openingBalance = domainResult.openingBalance,
            totalInflow = domainResult.totalReceipts,
            totalOutflow = domainResult.totalPayments,
            closingBalance = domainResult.closingBalance,
            entries = domainResult.rows.map { row ->
                val vType = VoucherType.entries.find { it.name.equals(row.voucherType, ignoreCase = true) }
                    ?: if (row.receiptAmount > 0) VoucherType.RECEIPT else VoucherType.PAYMENT
                CashBookEntryItem(
                    id = row.id,
                    dateMillis = row.dateMillis,
                    voucherNo = row.voucherNo,
                    particulars = row.particulars,
                    voucherType = vType,
                    inAmount = row.receiptAmount,
                    outAmount = row.paymentAmount,
                    runningBalance = row.runningBalance,
                    mode = row.paymentMode
                )
            }
        )
    }

    // 8. Day Book
    private fun calculateDaybook(
        allInvoices: List<Invoice>,
        allPurchases: List<PurchaseBill>,
        allTransactions: List<Transaction>,
        allKhataEntries: List<KhataEntry>,
        parties: List<Party>,
        dateMillis: Long
    ): DaybookSummary {
        val domainResult = com.hisabpro.app.domain.accounting.DayBookCalculator.calculate(
            allInvoices = allInvoices,
            allPurchases = allPurchases,
            allTransactions = allTransactions,
            allKhataEntries = allKhataEntries,
            parties = parties,
            dateMillis = dateMillis
        )

        return DaybookSummary(
            dateMillis = dateMillis,
            daySalesTotal = domainResult.salesTotal,
            daySalesCount = domainResult.vouchers.count { it.voucherType == com.hisabpro.app.domain.accounting.DayBookVoucherType.SALE },
            dayPurchasesTotal = domainResult.purchasesTotal,
            dayPurchasesCount = domainResult.vouchers.count { it.voucherType == com.hisabpro.app.domain.accounting.DayBookVoucherType.PURCHASE },
            dayInvoices = allInvoices,
            dayPurchases = allPurchases,
            dayCashIn = domainResult.cashIn,
            dayCashOut = domainResult.cashOut,
            dayBankIn = domainResult.bankIn,
            dayBankOut = domainResult.bankOut,
            dayTransactions = allTransactions,
            vouchers = domainResult.vouchers.map { v ->
                val type = VoucherType.entries.find { it.name == v.voucherType.name } ?: VoucherType.JOURNAL
                DaybookVoucherEntry(
                    id = v.id,
                    dateMillis = v.dateMillis,
                    voucherNumber = v.voucherNumber,
                    voucherType = type,
                    narration = v.narration,
                    debitAccount = v.debitAccount,
                    creditAccount = v.creditAccount,
                    amount = v.amount,
                    debitAmount = v.debitAmount,
                    creditAmount = v.creditAmount,
                    paymentMode = v.paymentMode,
                    partyName = v.partyName
                )
            },
            netDayMovement = domainResult.netDayMovement,
            netCashMovement = domainResult.cashIn - domainResult.cashOut,
            netBankMovement = domainResult.bankIn - domainResult.bankOut,
            totalDr = domainResult.totalDr,
            totalCr = domainResult.totalCr,
            isBalanced = domainResult.isBalanced,
            receiptsTotal = domainResult.receiptsTotal,
            paymentsTotal = domainResult.paymentsTotal,
            expensesTotal = domainResult.expensesTotal,
            journalsTotal = domainResult.journalsTotal,
            creditNotesTotal = domainResult.creditNotesTotal,
            debitNotesTotal = domainResult.debitNotesTotal
        )
    }

    private fun calculatePurchasesRegister(purchases: List<PurchaseBill>): PurchasesRegisterSummary {
        val totalCount = purchases.size
        val totalPurchasesValue = purchases.sumOf { it.grandTotal }
        val totalTaxableAmount = purchases.sumOf { it.subtotal }
        val totalTaxAmount = purchases.sumOf { it.totalTax }
        val itcAvailableTax = purchases.filter { it.itcEligible }.sumOf { it.totalTax }
        val totalPaidAmount = purchases.sumOf { it.paidAmount }
        val totalDueAmount = purchases.sumOf { it.dueAmount }

        return PurchasesRegisterSummary(
            totalBillsCount = totalCount,
            totalPurchasesValue = totalPurchasesValue,
            totalTaxableAmount = totalTaxableAmount,
            totalTaxAmount = totalTaxAmount,
            itcAvailableTax = itcAvailableTax,
            totalPaidAmount = totalPaidAmount,
            totalDueAmount = totalDueAmount,
            purchases = purchases
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

        val slabMap = mutableMapOf<Double, MutableList<Pair<Double, Double>>>()
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

            for (line in inv.items) {
                val lineTax = line.getTaxAmount(inv.gstMode)
                val rate = line.gstRate
                val list = slabMap.getOrPut(rate) { mutableListOf() }
                list.add(Pair(line.taxableAmount, lineTax))

                val hsn = if (line.hsnCode.isNotBlank()) line.hsnCode else "NA"
                val acc = hsnMap.getOrPut(hsn) {
                    HsnAccumulator(
                        hsnCode = hsn,
                        description = line.description,
                        unit = line.unit,
                        gstRate = rate
                    )
                }
                acc.totalQty += line.quantity
                acc.totalTaxable += line.taxableAmount
                acc.totalTax += lineTax
            }
        }

        val slabSummaries = listOf(0.0, 5.0, 12.0, 18.0, 28.0).map { rate ->
            val entries = slabMap[rate] ?: emptyList()
            TaxSlabSummary(
                gstRate = rate,
                taxableAmount = entries.sumOf { it.first },
                cgstAmount = entries.sumOf { it.second / 2.0 },
                sgstAmount = entries.sumOf { it.second / 2.0 },
                igstAmount = 0.0,
                totalTax = entries.sumOf { it.second },
                itemCount = entries.size
            )
        }

        val hsnSummaries = hsnMap.values.map { acc ->
            HsnSummaryItem(
                hsnCode = acc.hsnCode,
                description = acc.description,
                totalQuantity = acc.totalQty,
                unit = acc.unit,
                taxableAmount = acc.totalTaxable,
                gstRate = acc.gstRate,
                totalTax = acc.totalTax
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

    private fun calculateGstr3b(invoices: List<Invoice>, purchases: List<PurchaseBill>): Gstr3bSummary {
        val outwardTaxable = invoices.sumOf { it.subtotal }
        val outwardCgst = invoices.sumOf { it.cgstTotal }
        val outwardSgst = invoices.sumOf { it.sgstTotal }
        val outwardIgst = invoices.sumOf { it.igstTotal }
        val totalOutputTax = outwardCgst + outwardSgst + outwardIgst

        val eligiblePurchases = purchases.filter { it.itcEligible }
        val inwardTaxable = eligiblePurchases.sumOf { it.subtotal }
        val itcCgst = eligiblePurchases.sumOf { it.items.sumOf { item -> item.getCgst(it.gstMode) } }
        val itcSgst = eligiblePurchases.sumOf { it.items.sumOf { item -> item.getSgst(it.gstMode) } }
        val itcIgst = eligiblePurchases.sumOf { it.items.sumOf { item -> item.getIgst(it.gstMode) } }
        val totalItc = itcCgst + itcSgst + itcIgst

        val netCgst = max(0.0, outwardCgst - itcCgst)
        val netSgst = max(0.0, outwardSgst - itcSgst)
        val netIgst = max(0.0, outwardIgst - itcIgst)
        val totalNet = netCgst + netSgst + netIgst

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
        val salesRevenue = invoices.sumOf { it.subtotal }
        val totalPurchases = purchases.sumOf { it.grandTotal }

        val itemsMap = items.associateBy { it.name.trim().lowercase() }
        var totalCogs = 0.0

        for (inv in invoices) {
            for (line in inv.items) {
                val matched = itemsMap[line.description.trim().lowercase()]
                if (matched != null && matched.purchasePrice > 0) {
                    totalCogs += matched.purchasePrice * line.quantity
                } else {
                    totalCogs += line.taxableAmount * 0.65
                }
            }
        }

        val grossProfit = max(0.0, salesRevenue - totalCogs)
        val grossMarginPercent = if (salesRevenue > 0) (grossProfit / salesRevenue) * 100.0 else 0.0

        val expenseTx = transactions.filter { it.type == TransactionType.EXPENSE }
        val totalExpenses = expenseTx.sumOf { it.amount }

        val expenseByCategory = expenseTx
            .groupBy { it.category.label }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }

        val netProfit = grossProfit - totalExpenses
        val netMarginPercent = if (salesRevenue > 0) (netProfit / salesRevenue) * 100.0 else 0.0

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

    private fun calculatePartyAging(parties: List<PartyWithBalance>): PartyAgingSummary {
        val now = System.currentTimeMillis()
        var totalReceivable = 0.0
        var totalPayable = 0.0
        var b0to15 = 0.0
        var b16to30 = 0.0
        var b31to60 = 0.0
        var b60Plus = 0.0

        val debtors = mutableListOf<PartyAgingItem>()

        for (p in parties) {
            val net = p.netBalance
            if (net > 0.01) {
                totalReceivable += net
                val lastDate = p.lastEntryDateMillis ?: p.party.createdAt
                val days = ((now - lastDate) / (1000L * 60 * 60 * 24)).toInt().coerceAtLeast(0)
                val bucket = when {
                    days <= 15 -> {
                        b0to15 += net
                        AgingBucket.DAYS_0_15
                    }
                    days <= 30 -> {
                        b16to30 += net
                        AgingBucket.DAYS_16_30
                    }
                    days <= 60 -> {
                        b31to60 += net
                        AgingBucket.DAYS_31_60
                    }
                    else -> {
                        b60Plus += net
                        AgingBucket.DAYS_60_PLUS
                    }
                }
                debtors.add(
                    PartyAgingItem(
                        partyId = p.party.id,
                        partyName = p.party.name,
                        phone = p.party.phone,
                        balanceDue = net,
                        daysOverdue = days,
                        bucket = bucket
                    )
                )
            } else if (net < -0.01) {
                totalPayable += abs(net)
            }
        }

        debtors.sortByDescending { it.balanceDue }

        return PartyAgingSummary(
            totalReceivable = totalReceivable,
            totalPayable = totalPayable,
            bucket0to15 = b0to15,
            bucket16to30 = b16to30,
            bucket31to60 = b31to60,
            bucket60Plus = b60Plus,
            debtorList = debtors
        )
    }

    private fun calculateStockValuation(items: List<Item>): StockValuationSummary {
        var totalUnits = 0.0
        var totalSelling = 0.0
        var totalCost = 0.0
        var lowCount = 0
        var zeroCount = 0

        for (item in items) {
            val stock = max(0.0, item.currentStock)
            totalUnits += stock
            totalSelling += stock * item.salePrice
            totalCost += stock * item.purchasePrice

            if (stock <= 0.0) {
                zeroCount++
            } else if (stock <= item.minStockAlert) {
                lowCount++
            }
        }

        val potentialProfit = max(0.0, totalSelling - totalCost)

        return StockValuationSummary(
            totalItemsCount = items.size,
            totalStockUnits = totalUnits,
            totalSellingValue = totalSelling,
            totalCostValue = totalCost,
            potentialProfit = potentialProfit,
            lowStockCount = lowCount,
            outOfStockCount = zeroCount
        )
    }

    private fun calculateTrialBalance(
        invoices: List<Invoice>,
        purchases: List<PurchaseBill>,
        transactions: List<Transaction>,
        partiesWithBalances: List<PartyWithBalance>,
        items: List<Item>
    ): TrialBalanceSummary {
        val accounts = mutableListOf<TrialBalanceAccount>()

        var totalCashIn = 0.0
        var totalCashOut = 0.0
        var totalBankIn = 0.0
        var totalBankOut = 0.0

        for (inv in invoices) {
            if (inv.paidAmount > 0) {
                val isBank = inv.notes.contains("UPI", ignoreCase = true) || inv.notes.contains("Online", ignoreCase = true)
                if (isBank) totalBankIn += inv.paidAmount else totalCashIn += inv.paidAmount
            }
        }
        for (pur in purchases) {
            if (pur.paidAmount > 0) {
                val isBank = pur.paymentMode.contains("Bank", ignoreCase = true) || pur.paymentMode.contains("UPI", ignoreCase = true)
                if (isBank) totalBankOut += pur.paidAmount else totalCashOut += pur.paidAmount
            }
        }
        for (tx in transactions) {
            val isBank = tx.paymentMode != PaymentMode.CASH
            if (tx.type == TransactionType.INCOME) {
                if (isBank) totalBankIn += tx.amount else totalCashIn += tx.amount
            } else {
                if (isBank) totalBankOut += tx.amount else totalCashOut += tx.amount
            }
        }

        val cashInHand = totalCashIn - totalCashOut
        if (cashInHand >= 0) {
            accounts.add(TrialBalanceAccount("1010", "Cash in Hand (रोकड)", "Asset", debitAmount = cashInHand, creditAmount = 0.0, note = "Physical Cash Vault"))
        } else {
            accounts.add(TrialBalanceAccount("1010", "Cash Overdraft", "Liability", debitAmount = 0.0, creditAmount = abs(cashInHand), note = "Cash Shortage"))
        }

        val bankBalance = totalBankIn - totalBankOut
        if (bankBalance >= 0) {
            accounts.add(TrialBalanceAccount("1020", "Bank & UPI Accounts", "Asset", debitAmount = bankBalance, creditAmount = 0.0, note = "Current/Savings/UPI Balance"))
        } else {
            accounts.add(TrialBalanceAccount("1020", "Bank Overdraft (OD)", "Liability", debitAmount = 0.0, creditAmount = abs(bankBalance), note = "Overdraft Facility"))
        }

        val customersReceivable = partiesWithBalances
            .filter { it.party.type == PartyType.CUSTOMER && it.netBalance > 0.01 }
            .sumOf { it.netBalance }
        if (customersReceivable > 0.0) {
            accounts.add(TrialBalanceAccount("1030", "Sundry Debtors (Receivables)", "Asset", debitAmount = customersReceivable, creditAmount = 0.0, note = "Customer Outstanding Ledger"))
        }

        val stockValuation = items.sumOf { it.currentStock * it.purchasePrice }
        if (stockValuation > 0.0) {
            accounts.add(TrialBalanceAccount("1040", "Closing Stock in Hand", "Asset", debitAmount = stockValuation, creditAmount = 0.0, note = "Valuation At Cost Price"))
        }

        val itcAvailable = purchases.filter { it.itcEligible }.sumOf { it.totalTax }
        if (itcAvailable > 0.0) {
            accounts.add(TrialBalanceAccount("1050", "Input Tax Credit (ITC - GST)", "Asset", debitAmount = itcAvailable, creditAmount = 0.0, note = "Eligible GST On Inward Supplies"))
        }

        val suppliersPayable = partiesWithBalances
            .filter { it.party.type == PartyType.SUPPLIER && it.netBalance < -0.01 }
            .sumOf { abs(it.netBalance) }
        if (suppliersPayable > 0.0) {
            accounts.add(TrialBalanceAccount("2010", "Sundry Creditors (Payables)", "Liability", debitAmount = 0.0, creditAmount = suppliersPayable, note = "Supplier Outstanding Khata"))
        }

        val outputGst = invoices.filter { it.type == InvoiceType.TAX_INVOICE }.sumOf { it.totalTax }
        if (outputGst > 0.0) {
            accounts.add(TrialBalanceAccount("2030", "GST Output Tax Payable", "Liability", debitAmount = 0.0, creditAmount = outputGst, note = "GST Collected on Sales"))
        }

        val totalSalesRevenue = invoices.filter { it.type != InvoiceType.PROFORMA }.sumOf { it.subtotal }
        if (totalSalesRevenue > 0.0) {
            accounts.add(TrialBalanceAccount("3010", "Sales Revenue Account", "Income", debitAmount = 0.0, creditAmount = totalSalesRevenue, note = "Net Taxable Turnover"))
        }

        val totalPurchasesCost = purchases.sumOf { it.subtotal }
        if (totalPurchasesCost > 0.0) {
            accounts.add(TrialBalanceAccount("4010", "Purchases Account", "Expense", debitAmount = totalPurchasesCost, creditAmount = 0.0, note = "Inward Goods Cost"))
        }

        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
        val expensesByCategory = expenses.groupBy { it.category.label }
        expensesByCategory.forEach { (catLabel, txs) ->
            val sum = txs.sumOf { it.amount }
            if (sum > 0.0) {
                accounts.add(TrialBalanceAccount("4020", "Expense: $catLabel", "Expense", debitAmount = sum, creditAmount = 0.0, note = "Business Expense"))
            }
        }

        val totalDebitsWithoutEquity = accounts.sumOf { it.debitAmount }
        val totalCreditsWithoutEquity = accounts.sumOf { it.creditAmount }
        val balancingEquity = totalDebitsWithoutEquity - totalCreditsWithoutEquity
        if (balancingEquity > 0.001) {
            accounts.add(TrialBalanceAccount("5010", "Owner's Equity & Retained Earnings", "Liability", debitAmount = 0.0, creditAmount = balancingEquity, note = "Capital / Retained Surplus"))
        } else if (balancingEquity < -0.001) {
            accounts.add(TrialBalanceAccount("5010", "Owner's Drawings / Net Deficit", "Asset", debitAmount = abs(balancingEquity), creditAmount = 0.0, note = "Drawings / Net Deficit"))
        }

        val totalDebit = accounts.sumOf { it.debitAmount }
        val totalCredit = accounts.sumOf { it.creditAmount }
        val diff = abs(totalDebit - totalCredit)

        return TrialBalanceSummary(
            asOfDateMillis = System.currentTimeMillis(),
            totalDebit = totalDebit,
            totalCredit = totalCredit,
            isBalanced = diff < 0.01,
            difference = diff,
            accounts = accounts,
            assetTotal = accounts.filter { it.accountCategory == "Asset" }.sumOf { it.debitAmount },
            liabilityTotal = accounts.filter { it.accountCategory == "Liability" }.sumOf { it.creditAmount },
            incomeTotal = accounts.filter { it.accountCategory == "Income" }.sumOf { it.creditAmount },
            expenseTotal = accounts.filter { it.accountCategory == "Expense" }.sumOf { it.debitAmount }
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
                val month = cal.get(Calendar.MONTH)
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
                val fyStartYear = if (month >= Calendar.APRIL) year else year - 1
                cal.set(fyStartYear, Calendar.APRIL, 1, 0, 0, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(fyStartYear + 1, Calendar.MARCH, 31, 23, 59, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
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
