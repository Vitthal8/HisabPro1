package com.hisabpro.app.ui.items

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisabpro.app.data.model.Item
import com.hisabpro.app.ui.theme.Amber700
import com.hisabpro.app.ui.theme.Emerald50
import com.hisabpro.app.ui.theme.Emerald700
import com.hisabpro.app.ui.theme.Emerald800
import com.hisabpro.app.ui.theme.PureWhite
import com.hisabpro.app.ui.theme.Slate100
import com.hisabpro.app.ui.theme.Slate200
import com.hisabpro.app.ui.theme.Slate500
import com.hisabpro.app.ui.theme.Slate600
import com.hisabpro.app.ui.theme.Slate700
import com.hisabpro.app.ui.theme.Slate900
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemsScreen(
    viewModel: ItemViewModel,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val items by viewModel.filteredItems.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val onlyLowStock by viewModel.onlyLowStock.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()

    val totalItems by viewModel.totalItemsCount.collectAsState()
    val lowStockCount by viewModel.lowStockCount.collectAsState()
    val totalStockSaleVal by viewModel.totalStockSaleValue.collectAsState()
    val totalStockCostVal by viewModel.totalStockPurchaseValue.collectAsState()

    var showAddEditSheet by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<Item?>(null) }

    var showAdjustSheet by remember { mutableStateOf(false) }
    var itemToAdjust by remember { mutableStateOf<Item?>(null) }

    var showDetailSheet by remember { mutableStateOf(false) }
    var selectedItemDetail by remember { mutableStateOf<Item?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }

    val addEditSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val adjustSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = PureWhite
                            )
                        }
                    }
                },
                title = {
                    Column {
                        Text(
                            text = "Items & Stock",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                        Text(
                            text = "Inventory Catalog & Stock Tracking",
                            style = MaterialTheme.typography.bodySmall,
                            color = PureWhite.copy(alpha = 0.85f)
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.testTag("sort_items_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = "Sort Items",
                                tint = PureWhite
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            ItemSortOption.values().forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option.label,
                                            fontWeight = if (sortOption == option) FontWeight.Bold else FontWeight.Normal,
                                            color = if (sortOption == option) Emerald700 else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        viewModel.setSortOption(option)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = { viewModel.shareStockInventoryThermalSlip(context) },
                        modifier = Modifier.testTag("print_thermal_stock_slip_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "Print / Share Thermal Stock Audit Slip",
                            tint = PureWhite
                        )
                    }

                    IconButton(
                        onClick = { viewModel.exportStockCsv(context) },
                        modifier = Modifier.testTag("export_stock_csv_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Export Inventory CSV",
                            tint = PureWhite
                        )
                    }

                    IconButton(
                        onClick = { viewModel.shareStockSummary(context) },
                        modifier = Modifier.testTag("share_stock_summary_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Stock Summary",
                            tint = PureWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Emerald700)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    itemToEdit = null
                    showAddEditSheet = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null, tint = PureWhite) },
                text = { Text("Add Item", color = PureWhite, fontWeight = FontWeight.Bold) },
                containerColor = Emerald700,
                modifier = Modifier.testTag("fab_add_item")
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Metrics Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Slate200),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TOTAL STOCK VALUE (RETAIL)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = currencyFormat.format(totalStockSaleVal),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Emerald700
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "COST VALUE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = currencyFormat.format(totalStockCostVal),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$totalItems Products / SKUs",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Clickable Low Stock badge
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (lowStockCount > 0) Color(0xFFFFF8E1) else Emerald700.copy(alpha = 0.1f),
                            modifier = Modifier.clickable {
                                viewModel.toggleLowStockFilter()
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (lowStockCount > 0) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Amber700,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = if (lowStockCount > 0) "$lowStockCount Low Stock" else "Stock Healthy",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (lowStockCount > 0) Amber700 else Emerald700
                                )
                            }
                        }
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("item_search_field"),
                placeholder = { Text("Search by name, SKU, HSN, or category...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Emerald700,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips (Categories & Low Stock)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Low stock filter chip
                FilterChip(
                    selected = onlyLowStock,
                    onClick = { viewModel.toggleLowStockFilter() },
                    label = { Text("⚠️ Low Stock Alert ($lowStockCount)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFFF8E1),
                        selectedLabelColor = Amber700
                    )
                )

                // Category chips
                categories.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category && !onlyLowStock,
                        onClick = {
                            viewModel.setLowStockFilter(false)
                            viewModel.onCategorySelected(category)
                        },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Emerald700.copy(alpha = 0.15f),
                            selectedLabelColor = Emerald700
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Item List
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty() || onlyLowStock) "No matching items found" else "No items in catalog yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tap '+ Add Item' below to register products",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        ItemCard(
                            item = item,
                            currencyFormat = currencyFormat,
                            onItemClick = {
                                selectedItemDetail = item
                                showDetailSheet = true
                            },
                            onQuickStockIn = {
                                viewModel.quickAdjustStock(item.id, 1.0)
                            },
                            onQuickStockOut = {
                                viewModel.quickAdjustStock(item.id, -1.0)
                            },
                            onAdjustClick = {
                                itemToAdjust = item
                                showAdjustSheet = true
                            },
                            onShareClick = {
                                viewModel.shareSingleItem(context, item)
                            }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Sheet
    if (showAddEditSheet) {
        AddEditItemSheet(
            sheetState = addEditSheetState,
            itemToEdit = itemToEdit,
            onDismiss = {
                showAddEditSheet = false
                itemToEdit = null
            },
            onSave = { savedItem ->
                if (itemToEdit == null) {
                    viewModel.addItem(savedItem)
                } else {
                    viewModel.updateItem(savedItem)
                }
                showAddEditSheet = false
                itemToEdit = null
            }
        )
    }

    // Adjust Stock Sheet
    if (showAdjustSheet && itemToAdjust != null) {
        StockAdjustSheet(
            sheetState = adjustSheetState,
            item = itemToAdjust!!,
            onDismiss = {
                showAdjustSheet = false
                itemToAdjust = null
            },
            onConfirm = { changeQty, reason, note, refNumber ->
                viewModel.adjustStock(
                    itemId = itemToAdjust!!.id,
                    changeQty = changeQty,
                    reason = reason,
                    note = note,
                    sourceRefNumber = refNumber.ifBlank { null }
                )
                showAdjustSheet = false
                itemToAdjust = null
            }
        )
    }

    // Detail Sheet
    if (showDetailSheet && selectedItemDetail != null) {
        val currentItem = items.find { it.id == selectedItemDetail!!.id } ?: selectedItemDetail!!
        ItemDetailSheet(
            sheetState = detailSheetState,
            item = currentItem,
            stockHistory = viewModel.getStockHistory(currentItem.id),
            onDismiss = {
                showDetailSheet = false
                selectedItemDetail = null
            },
            onAdjustStockClick = {
                showDetailSheet = false
                itemToAdjust = currentItem
                showAdjustSheet = true
            },
            onEditClick = {
                showDetailSheet = false
                itemToEdit = currentItem
                showAddEditSheet = true
            },
            onDeleteClick = {
                viewModel.deleteItem(currentItem.id)
                showDetailSheet = false
                selectedItemDetail = null
            },
            onShareClick = {
                viewModel.shareSingleItem(context, currentItem)
            },
            onThermalLabelClick = {
                viewModel.shareSingleItemThermalLabel(context, currentItem)
            },
            onExportHistoryCsv = {
                viewModel.exportItemStockMovementCsv(context, currentItem)
            }
        )
    }
}

@Composable
private fun ItemCard(
    item: Item,
    currencyFormat: NumberFormat,
    onItemClick: () -> Unit,
    onQuickStockIn: () -> Unit,
    onQuickStockOut: () -> Unit,
    onAdjustClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
            .testTag("item_card_${item.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = item.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (item.itemCode.isNotBlank()) {
                            Text(
                                text = "SKU: ${item.itemCode}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (item.hsnCode.isNotBlank()) {
                            Text(
                                text = "• HSN: ${item.hsnCode}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Current Stock Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        item.isOutOfStock -> Color(0xFFFFEBEE)
                        item.isLowStock -> Color(0xFFFFF8E1)
                        else -> Emerald700.copy(alpha = 0.1f)
                    }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "${item.currentStock.toInt()} ${item.unit}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                item.isOutOfStock -> Color(0xFFC62828)
                                item.isLowStock -> Amber700
                                else -> Emerald700
                            }
                        )
                        Text(
                            text = when {
                                item.isOutOfStock -> "Out of stock"
                                item.isLowStock -> "Low Stock"
                                else -> "In Stock"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = when {
                                item.isOutOfStock -> Color(0xFFC62828)
                                item.isLowStock -> Amber700
                                else -> Emerald700
                            }
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column {
                        Text("Sale Price", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(currencyFormat.format(item.salePrice), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }

                    if (item.purchasePrice > 0) {
                        Column {
                            Text("Cost Price", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(currencyFormat.format(item.purchasePrice), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Column {
                            Text("Margin", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "+${currencyFormat.format(item.profitMarginAmount)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (item.profitMarginAmount >= 0) Emerald700 else Color.Red
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick Stock Stepper
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(2.dp)
                    ) {
                        IconButton(
                            onClick = onQuickStockOut,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("quick_stock_minus_${item.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Decrease Stock by 1",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        IconButton(
                            onClick = onQuickStockIn,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("quick_stock_plus_${item.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Increase Stock by 1",
                                tint = Emerald700,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onShareClick,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("share_item_btn_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share item details",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    OutlinedButton(
                        onClick = onAdjustClick,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("adjust_stock_btn_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = Emerald700
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Adjust", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Emerald700)
                    }
                }
            }
        }
    }
}
