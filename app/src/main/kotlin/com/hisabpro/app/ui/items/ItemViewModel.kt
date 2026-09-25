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
import com.hisabpro.app.data.repository.SettingsRepository
import com.hisabpro.app.ui.reports.ReportExporter
import com.hisabpro.app.util.ThermalSlipGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

enum class ItemSortOption(val label: String) {
    NAME_ASC("Name (A to Z)"),
    STOCK_LOW_TO_HIGH("Lowest Stock First"),
    STOCK_VALUE_HIGH_TO_LOW("Highest Valuation"),
    PRICE_HIGH_TO_LOW("Price (High to Low)"),
    PRICE_LOW_TO_HIGH("Price (Low to High)")
}

class ItemViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ItemRepository.getInstance(application)
    val rawItems: StateFlow<List<Item>> = repository.items

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _onlyLowStock = MutableStateFlow(false)
    val onlyLowStock: StateFlow<Boolean> = _onlyLowStock.asStateFlow()

    private val _sortOption = MutableStateFlow(ItemSortOption.NAME_ASC)
    val sortOption: StateFlow<ItemSortOption> = _sortOption.asStateFlow()

    val filteredItems: StateFlow<List<Item>> = combine(
        rawItems,
        _searchQuery,
        _selectedCategory,
        _onlyLowStock,
        _sortOption
    ) { items, query, category, lowStockOnly, sortOpt ->
        val filtered = items.filter { item ->
            val matchesQuery = query.isBlank() ||
                item.name.contains(query, ignoreCase = true) ||
                item.itemCode.contains(query, ignoreCase = true) ||
                item.hsnCode.contains(query, ignoreCase = true) ||
                item.category.contains(query, ignoreCase = true)

            val matchesCategory = category == "All" || item.category.equals(category, ignoreCase = true)
            val matchesLowStock = !lowStockOnly || item.isLowStock

            matchesQuery && matchesCategory && matchesLowStock
        }
        when (sortOpt) {
            ItemSortOption.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
            ItemSortOption.STOCK_LOW_TO_HIGH -> filtered.sortedBy { it.currentStock }
            ItemSortOption.STOCK_VALUE_HIGH_TO_LOW -> filtered.sortedByDescending { it.stockValueSale }
            ItemSortOption.PRICE_HIGH_TO_LOW -> filtered.sortedByDescending { it.salePrice }
            ItemSortOption.PRICE_LOW_TO_HIGH -> filtered.sortedBy { it.salePrice }
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

    fun setSortOption(option: ItemSortOption) {
        _sortOption.value = option
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

    fun adjustStock(
        itemId: String,
        changeQty: Double,
        reason: StockReason,
        note: String,
        sourceRefNumber: String? = null,
        sourceTransactionId: String? = null,
        sourceTransactionType: String? = null
    ) {
        viewModelScope.launch {
            repository.adjustStock(
                itemId = itemId,
                changeQty = changeQty,
                reason = reason,
                note = note,
                sourceRefNumber = sourceRefNumber,
                sourceTransactionId = sourceTransactionId,
                sourceTransactionType = sourceTransactionType
            )
        }
    }

    fun quickAdjustStock(itemId: String, delta: Double) {
        val reason = if (delta > 0) StockReason.PURCHASE_IN else StockReason.SALE_OUT
        val note = if (delta > 0) "Quick Count In (+${delta.toInt()})" else "Quick Count Out (${delta.toInt()})"
        adjustStock(
            itemId = itemId,
            changeQty = delta,
            reason = reason,
            note = note,
            sourceRefNumber = "QUICK-ADJ",
            sourceTransactionType = "STOCK_ADJUSTMENT"
        )
    }

    fun getStockHistory(itemId: String): List<StockHistoryEntry> {
        return repository.getHistoryForItem(itemId)
    }

    fun deductStockForInvoiceItem(
        itemNameOrId: String,
        quantity: Double,
        invoiceNumber: String,
        sourceTransactionId: String? = null
    ) {
        repository.deductStockForInvoiceItem(itemNameOrId, quantity, invoiceNumber, sourceTransactionId)
    }

    fun restoreStockForInvoiceItem(
        itemNameOrId: String,
        quantity: Double,
        invoiceNumber: String,
        sourceTransactionId: String? = null
    ) {
        repository.restoreStockForInvoiceItem(itemNameOrId, quantity, invoiceNumber, sourceTransactionId)
    }

    fun exportStockCsv(context: Context) {
        val businessName = SettingsRepository.getInstance(context).profile.value.shopName.ifBlank { "HisabPro Store" }
        val uri = ReportExporter.exportInventoryStockCsv(context, rawItems.value, businessName)
        if (uri != null) {
            ReportExporter.shareCsvFile(context, uri, "Inventory Stock Report - $businessName")
        }
    }

    fun exportItemStockMovementCsv(context: Context, item: Item) {
        val businessName = SettingsRepository.getInstance(context).profile.value.shopName.ifBlank { "HisabPro Store" }
        val history = getStockHistory(item.id)
        val uri = ReportExporter.exportItemStockMovementCsv(context, item, history, businessName)
        if (uri != null) {
            ReportExporter.shareCsvFile(context, uri, "Stock Movement - ${item.name}")
        }
    }

    fun shareSingleItem(context: Context, item: Item) {
        val indiaLocale = Locale("en", "IN")
        val currencyFormat = NumberFormat.getCurrencyInstance(indiaLocale)
        val businessProfile = SettingsRepository.getInstance(context).profile.value
        val shopName = businessProfile.shopName.ifBlank { "HisabPro Store" }

        val sb = StringBuilder()
        sb.append("🏷️ *${item.name}*\n")
        sb.append("--------------------------------\n")
        sb.append("• *Price:* ${currencyFormat.format(item.salePrice)} / ${item.unit}\n")
        if (item.purchasePrice > 0) {
            sb.append("• *Cost Price:* ${currencyFormat.format(item.purchasePrice)}\n")
        }
        sb.append("• *GST Rate:* ${item.gstRate.toInt()}%\n")
        if (item.hsnCode.isNotBlank()) {
            sb.append("• *HSN Code:* ${item.hsnCode}\n")
        }
        if (item.itemCode.isNotBlank()) {
            sb.append("• *SKU/Barcode:* ${item.itemCode}\n")
        }
        sb.append("• *Category:* ${item.category}\n")
        sb.append("• *Current Stock:* ${item.currentStock.toInt()} ${item.unit} ")
        if (item.isOutOfStock) {
            sb.append("(🔴 Out of Stock)\n")
        } else if (item.isLowStock) {
            sb.append("(⚠️ Low Stock)\n")
        } else {
            sb.append("(✅ Available)\n")
        }
        sb.append("--------------------------------\n")
        sb.append("🏪 *$shopName*\n")
        if (businessProfile.phone.isNotBlank()) {
            sb.append("📞 Contact: ${businessProfile.phone}\n")
        }
        if (businessProfile.gstin.isNotBlank()) {
            sb.append("🏛️ GSTIN: ${businessProfile.gstin}\n")
        }

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, sb.toString())
            putExtra(Intent.EXTRA_SUBJECT, "Product Details - ${item.name}")
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share Product Info"))
    }

    fun shareStockSummary(context: Context) {
        val items = rawItems.value
        val indiaLocale = Locale("en", "IN")
        val currencyFormat = NumberFormat.getCurrencyInstance(indiaLocale)
        val businessProfile = SettingsRepository.getInstance(context).profile.value
        val shopName = businessProfile.shopName.ifBlank { "HisabPro Store" }

        val sb = StringBuilder()
        sb.append("📦 *$shopName - Inventory & Stock Status*\n")
        if (businessProfile.gstin.isNotBlank()) {
            sb.append("GSTIN: ${businessProfile.gstin}\n")
        }
        sb.append("Total Items: ${items.size} | Low Stock: ${items.count { it.isLowStock }}\n")
        sb.append("Total Retail Value: ${currencyFormat.format(items.sumOf { it.stockValueSale })}\n")
        sb.append("Total Cost Value: ${currencyFormat.format(items.sumOf { it.stockValuePurchase })}\n\n")

        items.forEachIndexed { index, item ->
            val statusEmoji = when {
                item.isOutOfStock -> "🔴 OUT"
                item.isLowStock -> "⚠️ LOW"
                else -> "✅ IN"
            }
            sb.append("${index + 1}. *${item.name}* ($statusEmoji)\n")
            sb.append("   • Stock: ${item.currentStock.toInt()} ${item.unit} (Alert: ${item.minStockAlert.toInt()})\n")
            sb.append("   • Sale: ${currencyFormat.format(item.salePrice)} | Cost: ${currencyFormat.format(item.purchasePrice)}\n")
            if (item.hsnCode.isNotBlank()) {
                sb.append("   • HSN: ${item.hsnCode} | GST: ${item.gstRate.toInt()}%\n")
            }
            sb.append("\n")
        }
        sb.append("Generated by $shopName via HisabPro")

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, sb.toString())
            putExtra(Intent.EXTRA_SUBJECT, "$shopName Inventory Report")
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share Inventory Report"))
    }

    fun shareStockInventoryThermalSlip(context: Context) {
        val items = filteredItems.value
        val category = selectedCategory.value
        val lowStockOnly = onlyLowStock.value
        ThermalSlipGenerator.shareInventoryStockThermalSlip(context, items, category, lowStockOnly)
    }

    fun shareSingleItemThermalLabel(context: Context, item: Item) {
        ThermalSlipGenerator.shareSingleItemThermalLabel(context, item)
    }
}
