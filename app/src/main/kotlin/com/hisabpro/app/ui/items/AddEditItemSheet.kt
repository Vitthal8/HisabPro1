package com.hisabpro.app.ui.items

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisabpro.app.data.model.Item
import com.hisabpro.app.ui.theme.Amber700
import com.hisabpro.app.ui.theme.Emerald700
import com.hisabpro.app.ui.theme.PureWhite
import java.text.NumberFormat
import java.util.Locale

private val COMMON_UNITS = listOf("Pcs", "Kg", "Gram", "Bag", "Box", "Packet", "Ltr", "Meter", "Dozen", "Set")
private val COMMON_CATEGORIES = listOf("General", "Groceries", "Electronics", "Electricals", "Dairy & FMCG", "Apparel", "Hardware", "Stationery", "Medicines")
private val GST_RATES = listOf(0.0, 5.0, 12.0, 18.0, 28.0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditItemSheet(
    sheetState: SheetState,
    itemToEdit: Item? = null,
    onDismiss: () -> Unit,
    onSave: (Item) -> Unit
) {
    var name by remember { mutableStateOf(itemToEdit?.name ?: "") }
    var itemCode by remember { mutableStateOf(itemToEdit?.itemCode ?: "") }
    var category by remember { mutableStateOf(itemToEdit?.category ?: "General") }
    var unit by remember { mutableStateOf(itemToEdit?.unit ?: "Pcs") }
    var salePriceText by remember { mutableStateOf(if (itemToEdit != null && itemToEdit.salePrice > 0) itemToEdit.salePrice.toString() else "") }
    var purchasePriceText by remember { mutableStateOf(if (itemToEdit != null && itemToEdit.purchasePrice > 0) itemToEdit.purchasePrice.toString() else "") }
    var hsnCode by remember { mutableStateOf(itemToEdit?.hsnCode ?: "") }
    var gstRate by remember { mutableDoubleStateOf(itemToEdit?.gstRate ?: 18.0) }
    var openingStockText by remember { mutableStateOf(if (itemToEdit != null) itemToEdit.currentStock.toString() else "0") }
    var minStockAlertText by remember { mutableStateOf(if (itemToEdit != null) itemToEdit.minStockAlert.toString() else "5") }

    var nameError by remember { mutableStateOf(false) }
    var unitMenuExpanded by remember { mutableStateOf(false) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }

    val salePrice = salePriceText.toDoubleOrNull() ?: 0.0
    val purchasePrice = purchasePriceText.toDoubleOrNull() ?: 0.0
    val profit = salePrice - purchasePrice
    val profitPercent = if (purchasePrice > 0.0) (profit / purchasePrice) * 100.0 else 0.0

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.imePadding(),
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Emerald700.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = Emerald700,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (itemToEdit == null) "Add New Item" else "Edit Item Details",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Item catalog, pricing & stock tracking",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Item Name
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (nameError && it.isNotBlank()) nameError = false
                },
                label = { Text("Item / Product Name *") },
                placeholder = { Text("e.g. Daawat Basmati Rice 5kg") },
                isError = nameError,
                supportingText = if (nameError) { { Text("Item name is required") } } else null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("item_name_input"),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // SKU / Item Code & HSN Code in a Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = itemCode,
                    onValueChange = { itemCode = it },
                    label = { Text("Item Code / Barcode") },
                    placeholder = { Text("e.g. SKU-101") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("item_code_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = hsnCode,
                    onValueChange = { hsnCode = it },
                    label = { Text("HSN / SAC Code") },
                    placeholder = { Text("e.g. 1006") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("item_hsn_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Category & Unit Selectors
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Category
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = {
                            IconButton(onClick = { categoryMenuExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Category")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { categoryMenuExpanded = true }
                    )
                    DropdownMenu(
                        expanded = categoryMenuExpanded,
                        onDismissRequest = { categoryMenuExpanded = false }
                    ) {
                        COMMON_CATEGORIES.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    categoryMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Unit
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unit") },
                        trailingIcon = {
                            IconButton(onClick = { unitMenuExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Unit")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { unitMenuExpanded = true }
                    )
                    DropdownMenu(
                        expanded = unitMenuExpanded,
                        onDismissRequest = { unitMenuExpanded = false }
                    ) {
                        COMMON_UNITS.forEach { u ->
                            DropdownMenuItem(
                                text = { Text(u) },
                                onClick = {
                                    unit = u
                                    unitMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // PRICING SECTION
            Text(
                text = "PRICING & PROFIT MARGIN",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = salePriceText,
                    onValueChange = { salePriceText = it },
                    label = { Text("Sale Price (₹) *") },
                    placeholder = { Text("0.00") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("item_sale_price_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )

                OutlinedTextField(
                    value = purchasePriceText,
                    onValueChange = { purchasePriceText = it },
                    label = { Text("Purchase / Cost (₹)") },
                    placeholder = { Text("0.00") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("item_purchase_price_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }

            // Margin Card
            if (salePrice > 0 && purchasePrice > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (profit >= 0) Emerald700.copy(alpha = 0.1f) else Color(0xFFFFEBEE)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = if (profit >= 0) Emerald700 else Color.Red,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Gross Margin:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "${currencyFormat.format(profit)} (${String.format(Locale.US, "%.1f", profitPercent)}%)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (profit >= 0) Emerald700 else Color.Red
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // GST Rate Chips
            Text(
                text = "GST TAX SLAB",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GST_RATES.forEach { rate ->
                    FilterChip(
                        selected = gstRate == rate,
                        onClick = { gstRate = rate },
                        label = { Text("${rate.toInt()}% GST") },
                        leadingIcon = if (gstRate == rate) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Emerald700.copy(alpha = 0.15f),
                            selectedLabelColor = Emerald700
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // STOCK QUANTITY & ALERT THRESHOLD
            Text(
                text = "STOCK TRACKING",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = openingStockText,
                    onValueChange = { openingStockText = it },
                    label = { Text(if (itemToEdit == null) "Opening Stock Qty" else "Current Stock Qty") },
                    placeholder = { Text("0") },
                    trailingIcon = { Text(unit, modifier = Modifier.padding(end = 8.dp), fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("item_stock_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )

                OutlinedTextField(
                    value = minStockAlertText,
                    onValueChange = { minStockAlertText = it },
                    label = { Text("Low Stock Alert At") },
                    placeholder = { Text("5") },
                    trailingIcon = { Text(unit, modifier = Modifier.padding(end = 8.dp), fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("item_alert_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        if (name.isBlank()) {
                            nameError = true
                            return@Button
                        }
                        val stockVal = openingStockText.toDoubleOrNull() ?: 0.0
                        val alertVal = minStockAlertText.toDoubleOrNull() ?: 5.0

                        val finalItem = if (itemToEdit == null) {
                            Item(
                                name = name.trim(),
                                itemCode = itemCode.trim(),
                                category = category.trim(),
                                unit = unit.trim(),
                                salePrice = salePrice,
                                purchasePrice = purchasePrice,
                                gstRate = gstRate,
                                hsnCode = hsnCode.trim(),
                                currentStock = stockVal,
                                minStockAlert = alertVal
                            )
                        } else {
                            itemToEdit.copy(
                                name = name.trim(),
                                itemCode = itemCode.trim(),
                                category = category.trim(),
                                unit = unit.trim(),
                                salePrice = salePrice,
                                purchasePrice = purchasePrice,
                                gstRate = gstRate,
                                hsnCode = hsnCode.trim(),
                                currentStock = stockVal,
                                minStockAlert = alertVal
                            )
                        }
                        onSave(finalItem)
                    },
                    modifier = Modifier
                        .weight(1.5f)
                        .testTag("save_item_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald700)
                ) {
                    Text(
                        text = if (itemToEdit == null) "Save Item" else "Update Item",
                        color = PureWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
