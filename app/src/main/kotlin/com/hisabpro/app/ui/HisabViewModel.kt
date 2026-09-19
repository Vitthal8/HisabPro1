package com.hisabpro.app.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hisabpro.app.data.model.Category
import com.hisabpro.app.data.model.PaymentMode
import com.hisabpro.app.data.model.Transaction
import com.hisabpro.app.data.model.TransactionType
import com.hisabpro.app.data.repository.TransactionRepository
import com.hisabpro.app.util.CashbookPdfGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

enum class FilterType {
    ALL,
    INCOME_ONLY,
    EXPENSE_ONLY
}

enum class DateFilterType(val label: String) {
    ALL_TIME("All Time"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month")
}

data class CategoryStat(
    val category: Category,
    val totalAmount: Double,
    val percentage: Float,
    val count: Int
)

data class HisabUiState(
    val allTransactions: List<Transaction> = emptyList(),
    val filteredTransactions: List<Transaction> = emptyList(),
    val totalBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val totalCashBalance: Double = 0.0,
    val totalBankBalance: Double = 0.0,
    val filterType: FilterType = FilterType.ALL,
    val dateFilterType: DateFilterType = DateFilterType.ALL_TIME,
    val selectedCategory: Category? = null,
    val selectedPaymentMode: PaymentMode? = null,
    val searchQuery: String = "",
    val categoryBreakdown: List<CategoryStat> = emptyList()
)

class HisabViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TransactionRepository.getInstance(application.applicationContext)

    private val _filterType = MutableStateFlow(FilterType.ALL)
    private val _dateFilterType = MutableStateFlow(DateFilterType.ALL_TIME)
    private val _selectedCategory = MutableStateFlow<Category?>(null)
    private val _selectedPaymentMode = MutableStateFlow<PaymentMode?>(null)
    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<HisabUiState> = combine(
        repository.transactions,
        _filterType,
        _dateFilterType,
        _selectedCategory,
        _selectedPaymentMode,
        _searchQuery
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val transactions = args[0] as List<Transaction>
        val filterType = args[1] as FilterType
        val dateFilter = args[2] as DateFilterType
        val selectedCategory = args[3] as? Category
        val selectedMode = args[4] as? PaymentMode
        val query = args[5] as String

        var totalIncome = 0.0
        var totalExpense = 0.0
        var cashInflow = 0.0
        var cashOutflow = 0.0
        var bankInflow = 0.0
        var bankOutflow = 0.0

        for (tx in transactions) {
            if (tx.type == TransactionType.INCOME) {
                totalIncome += tx.amount
                if (tx.paymentMode == PaymentMode.CASH) cashInflow += tx.amount else bankInflow += tx.amount
            } else {
                totalExpense += tx.amount
                if (tx.paymentMode == PaymentMode.CASH) cashOutflow += tx.amount else bankOutflow += tx.amount
            }
        }

        val totalBalance = totalIncome - totalExpense
        val totalCashBalance = cashInflow - cashOutflow
        val totalBankBalance = bankInflow - bankOutflow

        // Date calculation bounds
        val now = Calendar.getInstance()
        val startOfToday = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val startOfWeek = (now.clone() as Calendar).apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val startOfMonth = (now.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val filtered = transactions.filter { tx ->
            val matchesType = when (filterType) {
                FilterType.ALL -> true
                FilterType.INCOME_ONLY -> tx.type == TransactionType.INCOME
                FilterType.EXPENSE_ONLY -> tx.type == TransactionType.EXPENSE
            }

            val matchesDate = when (dateFilter) {
                DateFilterType.ALL_TIME -> true
                DateFilterType.TODAY -> tx.dateMillis >= startOfToday
                DateFilterType.THIS_WEEK -> tx.dateMillis >= startOfWeek
                DateFilterType.THIS_MONTH -> tx.dateMillis >= startOfMonth
            }

            val matchesCategory = selectedCategory == null || tx.category == selectedCategory
            val matchesMode = selectedMode == null || tx.paymentMode == selectedMode

            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                tx.title.contains(query, ignoreCase = true) ||
                        tx.note.contains(query, ignoreCase = true) ||
                        tx.category.label.contains(query, ignoreCase = true) ||
                        tx.paymentMode.label.contains(query, ignoreCase = true)
            }

            matchesType && matchesDate && matchesCategory && matchesMode && matchesQuery
        }

        // Category breakdown for expenses
        val expenseTransactions = filtered.filter { it.type == TransactionType.EXPENSE }
        val totalExpensesForBreakdown = expenseTransactions.sumOf { it.amount }
        val categoryStats = Category.entries.mapNotNull { cat ->
            val catTxs = expenseTransactions.filter { it.category == cat }
            if (catTxs.isEmpty()) null
            else {
                val catTotal = catTxs.sumOf { it.amount }
                val pct = if (totalExpensesForBreakdown > 0) {
                    ((catTotal / totalExpensesForBreakdown) * 100).toFloat()
                } else 0f
                CategoryStat(
                    category = cat,
                    totalAmount = catTotal,
                    percentage = pct,
                    count = catTxs.size
                )
            }
        }.sortedByDescending { it.totalAmount }

        HisabUiState(
            allTransactions = transactions,
            filteredTransactions = filtered,
            totalBalance = totalBalance,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            totalCashBalance = totalCashBalance,
            totalBankBalance = totalBankBalance,
            filterType = filterType,
            dateFilterType = dateFilter,
            selectedCategory = selectedCategory,
            selectedPaymentMode = selectedMode,
            searchQuery = query,
            categoryBreakdown = categoryStats
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HisabUiState()
    )

    fun setFilterType(type: FilterType) {
        _filterType.value = type
    }

    fun setDateFilterType(type: DateFilterType) {
        _dateFilterType.value = type
    }

    fun setSelectedCategory(category: Category?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }

    fun setSelectedPaymentMode(mode: PaymentMode?) {
        _selectedPaymentMode.value = if (_selectedPaymentMode.value == mode) null else mode
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addTransaction(
        title: String,
        amount: Double,
        type: TransactionType,
        category: Category,
        dateMillis: Long,
        paymentMode: PaymentMode,
        note: String
    ) {
        repository.addTransaction(
            title = title,
            amount = amount,
            type = type,
            category = category,
            dateMillis = dateMillis,
            paymentMode = paymentMode,
            note = note
        )
    }

    fun updateTransaction(transaction: Transaction) {
        repository.updateTransaction(transaction)
    }

    fun deleteTransaction(id: String) {
        repository.deleteTransaction(id)
    }

    fun resetToDemo() {
        repository.resetToDemo()
    }

    fun shareCashbookPdf(context: Context, targetWhatsApp: Boolean = false) {
        val state = uiState.value
        CashbookPdfGenerator.sharePdf(
            context = context,
            transactions = state.filteredTransactions,
            dateRangeLabel = state.dateFilterType.label,
            targetWhatsApp = targetWhatsApp
        )
    }

    fun exportCashbookCsv(context: Context) {
        val state = uiState.value
        CashbookPdfGenerator.exportCsv(
            context = context,
            transactions = state.filteredTransactions,
            dateRangeLabel = state.dateFilterType.label
        )
    }

    companion object {
        fun formatAmount(amount: Double): String {
            val formatter = NumberFormat.getNumberInstance(Locale.US)
            formatter.minimumFractionDigits = 2
            formatter.maximumFractionDigits = 2
            return formatter.format(amount)
        }
    }
}
