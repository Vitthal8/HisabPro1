package com.hisabpro.app.ui.items

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hisabpro.app.data.model.Item
import com.hisabpro.app.data.model.StockHistoryEntry
import com.hisabpro.app.data.model.StockReason
import com.hisabpro.app.data.repository.ItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class ItemViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ItemRepository(application)
    val rawItems: StateFlow<List<Item>> = repository.items

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _onlyLowStock = MutableStateFlow(false)
    val onlyLowStock: StateFlow<Boolean> = _onlyLowStock.asStateFlow()

    val filteredItems: StateFlow<List<Item>> = combine(
        rawItems,
        _searchQuery,
        _selectedCategory,
        _onlyLowStock
    ) { items, query, category, lowStockOnly ->
        items.filter { item ->
            val matchesQuery = query.isBlank() ||
                item.name.contains(query, ignoreCase = true) ||
                item.itemCode.contains(query, ignoreCase = true) ||
                item.hsnCode.contains(query, ignoreCase = true) ||
                item.category.contains(query, ignoreCase = true)

            val matchesCategory = category == "All" || item.category.equals(category, ignoreCase = true)
            val matchesLowStock = !lowStockOnly || item.isLowStock

            matchesQuery && matchesCategory && matchesLowStock
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<String>> = combine(rawItems) { (items) ->
        val cats = items.map { it.category.trim() }.filter { it.isNotBlank() }.distinct().sorted()
        listOf("All") + cats
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("All"))

    val totalItemsCount: StateFlow<Int> = combine(rawItems) { (items) ->
        items.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val lowStockCount: StateFlow<Int> = combine(rawItems) { (items) ->
        items.count { it.isLowStock }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val outOfStockCount: StateFlow<Int> = combine(rawItems) { (items) ->
        items.count { it.isOutOfStock }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalStockPurchaseValue: StateFlow<Double> = combine(rawItems) { (items) ->
        items.sumOf { it.stockValuePurchase }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalStockSaleValue: StateFlow<Double> = combine(rawItems) { (items) ->
        items.sumOf { it.stockValueSale }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
    }

    fun toggleLowStockFilter() {
        _onlyLowStock.value = !_onlyLowStock.value
    }

    fun setLowStockFilter(enabled: Boolean) {
        _onlyLowStock.value = enabled
    }

    fun addItem(item: Item) {
        viewModelScope.launch {
            repository.addItem(item)
        }
    }

    fun updateItem(item: Item) {
        viewModelScope.launch {
            repository.updateItem(item)
        }
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            repository.deleteItem(itemId)
        }
    }

    fun adjustStock(itemId: String, changeQty: Double, reason: StockReason, note: String) {
        viewModelScope.launch {
            repository.adjustStock(itemId, changeQty, reason, note)
        }
    }

    fun getStockHistory(itemId: String): List<StockHistoryEntry> {
        return repository.getHistoryForItem(itemId)
    }

    fun deductStockForInvoiceItem(itemNameOrId: String, quantity: Double, invoiceNumber: String) {
        repository.deductStockForInvoiceItem(itemNameOrId, quantity, invoiceNumber)
    }

    fun shareStockSummary(context: Context) {
        val items = rawItems.value
        val indiaLocale = Locale("en", "IN")
        val currencyFormat = NumberFormat.getCurrencyInstance(indiaLocale)
        val sb = StringBuilder()
        sb.append("📦 *HisabPro - Inventory & Stock Status*\n")
        sb.append("Total Items: ${items.size} | Low Stock: ${items.count { it.isLowStock }}\n")
        sb.append("Total Stock Value: ${currencyFormat.format(items.sumOf { it.stockValueSale })}\n\n")

        items.forEachIndexed { index, item ->
            val statusEmoji = when {
                item.isOutOfStock -> "🔴 OUT OF STOCK"
                item.isLowStock -> "⚠️ LOW STOCK"
                else -> "✅ IN STOCK"
            }
            sb.append("${index + 1}. *${item.name}* ($statusEmoji)\n")
            sb.append("   • Stock: ${item.currentStock.toInt()} ${item.unit} (Min: ${item.minStockAlert.toInt()})\n")
            sb.append("   • Sale: ${currencyFormat.format(item.salePrice)} | Cost: ${currencyFormat.format(item.purchasePrice)}\n")
            if (item.hsnCode.isNotBlank()) {
                sb.append("   • HSN: ${item.hsnCode} | GST: ${item.gstRate.toInt()}%\n")
            }
            sb.append("\n")
        }
        sb.append("Generated by HisabPro Business Suite")

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, sb.toString())
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share Inventory Report"))
    }
}
