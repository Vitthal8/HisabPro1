package com.hisabpro.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hisabpro.app.data.model.Category
import com.hisabpro.app.data.model.PaymentMode
import com.hisabpro.app.data.model.Transaction
import com.hisabpro.app.data.model.TransactionType
import com.hisabpro.app.data.repository.TransactionRepository
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
    val filterType: FilterType = FilterType.ALL,
    val selectedCategory: Category? = null,
    val searchQuery: String = "",
    val categoryBreakdown: List<CategoryStat> = emptyList()
)

class HisabViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TransactionRepository.getInstance(application.applicationContext)

    private val _filterType = MutableStateFlow(FilterType.ALL)
    private val _selectedCategory = MutableStateFlow<Category?>(null)
    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<HisabUiState> = combine(
        repository.transactions,
        _filterType,
        _selectedCategory,
        _searchQuery
    ) { transactions, filterType, selectedCategory, query ->
        var totalIncome = 0.0
        var totalExpense = 0.0

        for (tx in transactions) {
            if (tx.type == TransactionType.INCOME) {
                totalIncome += tx.amount
            } else {
                totalExpense += tx.amount
            }
        }

        val totalBalance = totalIncome - totalExpense

        val filtered = transactions.filter { tx ->
            val matchesType = when (filterType) {
                FilterType.ALL -> true
                FilterType.INCOME_ONLY -> tx.type == TransactionType.INCOME
                FilterType.EXPENSE_ONLY -> tx.type == TransactionType.EXPENSE
            }
            val matchesCategory = selectedCategory == null || tx.category == selectedCategory
            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                tx.title.contains(query, ignoreCase = true) ||
                        tx.note.contains(query, ignoreCase = true) ||
                        tx.category.label.contains(query, ignoreCase = true)
            }
            matchesType && matchesCategory && matchesQuery
        }

        // Category breakdown for expenses
        val expenseTransactions = transactions.filter { it.type == TransactionType.EXPENSE }
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
            filterType = filterType,
            selectedCategory = selectedCategory,
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

    fun setSelectedCategory(category: Category?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
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

    fun deleteTransaction(id: String) {
        repository.deleteTransaction(id)
    }

    fun resetToDemo() {
        repository.resetToDemo()
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
