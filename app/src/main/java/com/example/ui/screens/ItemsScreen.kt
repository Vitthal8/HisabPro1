package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Item
import com.example.ui.AccountingViewModel
import com.example.ui.theme.CreditGreen
import com.example.ui.theme.CreditGreenLight
import com.example.ui.theme.DebitRed
import com.example.ui.theme.DebitRedLight
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.Saffron
import com.example.util.IndianAccountingUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemsScreen(
    viewModel: AccountingViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val items by viewModel.items.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, LOW_STOCK, GROCERY, GRAINS, BEVERAGES
    var showAddItemDialog by remember { mutableStateOf(false) }
    var adjustingItem by remember { mutableStateOf<Item?>(null) }

    // Computations
    val totalValuation = items.sumOf { it.stockQty * it.purchasePrice }
    val lowStockCount = items.count { it.stockQty <= 5 }

    val filteredItems = items.filter { item ->
        val matchesSearch = item.name.contains(searchQuery, ignoreCase = true) ||
                item.category.contains(searchQuery, ignoreCase = true) ||
                item.hsnCode.contains(searchQuery, ignoreCase = true)

        val matchesFilter = when (selectedFilter) {
            "LOW_STOCK" -> item.stockQty <= 5
            "ALL" -> true
            else -> item.category.equals(selectedFilter, ignoreCase = true)
        }
        matchesSearch && matchesFilter
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Items & Stock (स्टॉक इन्व्हेंटरी)", fontWeight = FontWeight.Bold, color = Color.White)
                        Text("${items.size} Products Registered", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepNavy)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddItemDialog = true },
                containerColor = Saffron,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_item")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Item")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("items_screen")
        ) {
            // Summary Header Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("TOTAL STOCK VALUE", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(
                            text = IndianAccountingUtils.formatCurrency(totalValuation),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = DeepNavy
                        )
                    }

                    if (lowStockCount > 0) {
                        Surface(
                            color = DebitRedLight,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.clickable { selectedFilter = "LOW_STOCK" }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = DebitRed, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$lowStockCount Low Stock",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DebitRed
                                )
                            }
                        }
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("item_search_input"),
                placeholder = { Text("Search item, category, HSN...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("All (${items.size})") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "LOW_STOCK",
                        onClick = { selectedFilter = "LOW_STOCK" },
                        label = { Text("Low Stock (<=5)") }
                    )
                }
                val categories = items.map { it.category }.distinct().filter { it.isNotBlank() }
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedFilter == cat,
                        onClick = { selectedFilter = cat },
                        label = { Text(cat) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Items List
            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(54.dp), tint = Color.LightGray)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No items found", fontWeight = FontWeight.Bold, color = Color.Gray)
                        Text("Tap + button below to add your first product", fontSize = 13.sp, color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 84.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        ItemCard(
                            item = item,
                            onAdjustStock = { adjustingItem = item }
                        )
                    }
                }
            }
        }
    }

    // Add Item Dialog
    if (showAddItemDialog) {
        AddItemDialog(
            onDismiss = { showAddItemDialog = false },
            onSave = { name, unit, hsn, pPrice, sPrice, gst, category, stock ->
                viewModel.addItem(
                    name = name,
                    unit = unit,
                    hsnCode = hsn,
                    purchasePrice = pPrice,
                    sellPrice = sPrice,
                    gstRate = gst,
                    category = category,
                    stockQty = stock
                ) {
                    Toast.makeText(context, "$name added to inventory", Toast.LENGTH_SHORT).show()
                    showAddItemDialog = false
                }
            }
        )
    }

    // Adjust Stock Dialog
    adjustingItem?.let { item ->
        AdjustStockDialog(
            item = item,
            onDismiss = { adjustingItem = null },
            onConfirm = { delta, reason ->
                viewModel.updateStock(item.id, delta) {
                    val msg = if (delta >= 0) "+$delta ${item.unit} added ($reason)" else "$delta ${item.unit} removed ($reason)"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    adjustingItem = null
                }
            }
        )
    }
}

@Composable
fun ItemCard(
    item: Item,
    onAdjustStock: () -> Unit
) {
    val isLowStock = item.stockQty <= 5
    val margin = item.sellPrice - item.purchasePrice
    val marginPercent = if (item.purchasePrice > 0) ((margin / item.purchasePrice) * 100).toInt() else 0

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("item_card_${item.id}")
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
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = DeepNavy
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = item.category,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = Color.DarkGray
                            )
                        }
                        if (item.hsnCode.isNotBlank()) {
                            Text(text = "HSN: ${item.hsnCode}", fontSize = 11.sp, color = Color.Gray)
                        }
                        if (item.gstRate > 0) {
                            Text(text = "GST ${item.gstRate.toInt()}%", fontSize = 11.sp, color = Saffron, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Stock Quantity Badge
                Surface(
                    color = if (isLowStock) DebitRedLight else CreditGreenLight,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${if (item.stockQty % 1.0 == 0.0) item.stockQty.toInt() else item.stockQty} ${item.unit}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isLowStock) DebitRed else CreditGreen
                        )
                        Text(
                            text = if (isLowStock) "Low Stock" else "In Stock",
                            fontSize = 9.sp,
                            color = if (isLowStock) DebitRed else CreditGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(10.dp))

            // Pricing & Margins row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text("Sale Price", fontSize = 11.sp, color = Color.Gray)
                        Text(IndianAccountingUtils.formatCurrency(item.sellPrice), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CreditGreen)
                    }

                    Column {
                        Text("Purchase", fontSize = 11.sp, color = Color.Gray)
                        Text(IndianAccountingUtils.formatCurrency(item.purchasePrice), fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Color.DarkGray)
                    }

                    if (margin > 0) {
                        Column {
                            Text("Margin", fontSize = 11.sp, color = Color.Gray)
                            Text("+${marginPercent}%", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Saffron)
                        }
                    }
                }

                OutlinedButton(
                    onClick = onAdjustStock,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("adjust_stock_${item.id}")
                ) {
                    Text("Adjust Stock", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, unit: String, hsn: String, pPrice: Double, sPrice: Double, gst: Double, category: String, stock: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("PCS") }
    var hsnCode by remember { mutableStateOf("") }
    var purchasePriceText by remember { mutableStateOf("") }
    var sellPriceText by remember { mutableStateOf("") }
    var gstRateText by remember { mutableStateOf("0") }
    var category by remember { mutableStateOf("General") }
    var openingStockText by remember { mutableStateOf("10") }

    val commonUnits = listOf("PCS", "KG", "LTR", "PKT", "BOX", "MTR", "BAG", "DOZ")
    var unitDropdownExpanded by remember { mutableStateOf(false) }

    val commonGstRates = listOf("0", "5", "12", "18", "28")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Product / Item", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name *") },
                    placeholder = { Text("e.g. Tata Salt 1kg, Parle-G") },
                    modifier = Modifier.fillMaxWidth().testTag("item_name_input"),
                    singleLine = true
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    ExposedDropdownMenuBox(
                        expanded = unitDropdownExpanded,
                        onExpandedChange = { unitDropdownExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = unit,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Unit") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitDropdownExpanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = unitDropdownExpanded,
                            onDismissRequest = { unitDropdownExpanded = false }
                        ) {
                            commonUnits.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(u) },
                                    onClick = {
                                        unit = u
                                        unitDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sellPriceText,
                        onValueChange = { sellPriceText = it },
                        label = { Text("Sale Price (₹) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f).testTag("item_sell_price_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = purchasePriceText,
                        onValueChange = { purchasePriceText = it },
                        label = { Text("Purchase (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f).testTag("item_purchase_price_input"),
                        singleLine = true
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = openingStockText,
                        onValueChange = { openingStockText = it },
                        label = { Text("Opening Stock") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f).testTag("item_stock_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = hsnCode,
                        onValueChange = { hsnCode = it },
                        label = { Text("HSN Code") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // GST Rate Selection
                Text("GST Tax Rate: ${gstRateText}%", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    commonGstRates.forEach { rate ->
                        FilterChip(
                            selected = gstRateText == rate,
                            onClick = { gstRateText = rate },
                            label = { Text("${rate}%") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    val sPrice = sellPriceText.toDoubleOrNull() ?: 0.0
                    val pPrice = purchasePriceText.toDoubleOrNull() ?: 0.0
                    val stock = openingStockText.toDoubleOrNull() ?: 0.0
                    val gst = gstRateText.toDoubleOrNull() ?: 0.0
                    onSave(name, unit, hsnCode, pPrice, sPrice, gst, category, stock)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Saffron),
                modifier = Modifier.testTag("save_item_button")
            ) {
                Text("Save Product")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AdjustStockDialog(
    item: Item,
    onDismiss: () -> Unit,
    onConfirm: (delta: Double, reason: String) -> Unit
) {
    var deltaText by remember { mutableStateOf("5") }
    var isAdd by remember { mutableStateOf(true) } // true = Inward (+), false = Outward (-)
    var selectedReason by remember { mutableStateOf("Purchase Inward") }

    val reasons = if (isAdd) {
        listOf("Purchase Inward", "Customer Return", "Inventory Recount", "Opening Balance")
    } else {
        listOf("Damaged / Expired", "Wastage", "Theft / Loss", "Inventory Recount")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adjust Stock for ${item.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Current Stock: ${item.stockQty} ${item.unit}", fontWeight = FontWeight.Bold, color = DeepNavy)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { isAdd = true; selectedReason = "Purchase Inward" },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isAdd) CreditGreen else Color.LightGray),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Stock In")
                    }

                    Button(
                        onClick = { isAdd = false; selectedReason = "Damaged / Expired" },
                        colors = ButtonDefaults.buttonColors(containerColor = if (!isAdd) DebitRed else Color.LightGray),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("- Stock Out")
                    }
                }

                OutlinedTextField(
                    value = deltaText,
                    onValueChange = { deltaText = it },
                    label = { Text("Quantity (${item.unit}) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("stock_adjust_qty_input")
                )

                Text("Reason for adjustment:", fontSize = 12.sp, color = Color.Gray)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    reasons.forEach { r ->
                        FilterChip(
                            selected = selectedReason == r,
                            onClick = { selectedReason = r },
                            label = { Text(r) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = deltaText.toDoubleOrNull() ?: 0.0
                    if (qty <= 0) return@Button
                    val finalDelta = if (isAdd) qty else -qty
                    onConfirm(finalDelta, selectedReason)
                },
                colors = ButtonDefaults.buttonColors(containerColor = if (isAdd) CreditGreen else DebitRed),
                modifier = Modifier.testTag("confirm_stock_adjust_button")
            ) {
                Text("Update Stock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
