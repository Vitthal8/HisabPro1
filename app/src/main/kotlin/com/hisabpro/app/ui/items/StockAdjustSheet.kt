package com.hisabpro.app.ui.items

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.hisabpro.app.data.model.StockReason
import com.hisabpro.app.ui.theme.Amber700
import com.hisabpro.app.ui.theme.Emerald700
import com.hisabpro.app.ui.theme.PureWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockAdjustSheet(
    sheetState: SheetState,
    item: Item,
    onDismiss: () -> Unit,
    onConfirm: (changeQty: Double, reason: StockReason, note: String) -> Unit
) {
    var isAddition by remember { mutableStateOf(true) }
    var qtyText by remember { mutableStateOf("") }
    var selectedReason by remember { mutableStateOf(StockReason.PURCHASE_IN) }
    var note by remember { mutableStateOf("") }
    var qtyError by remember { mutableStateOf(false) }

    val qty = qtyText.toDoubleOrNull() ?: 0.0
    val signedChange = if (isAddition) qty else -qty
    val previewStock = (item.currentStock + signedChange).coerceAtLeast(0.0)

    val addReasons = listOf(StockReason.PURCHASE_IN, StockReason.RETURN_IN, StockReason.MANUAL_ADJUSTMENT)
    val removeReasons = listOf(StockReason.SALE_OUT, StockReason.DAMAGE_LOSS, StockReason.MANUAL_ADJUSTMENT)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
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
                            .background(if (isAddition) Emerald700.copy(alpha = 0.12f) else Color(0xFFFFEBEE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = null,
                            tint = if (isAddition) Emerald700 else Color(0xFFC62828),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Adjust Stock",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Current Stock Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CURRENT IN-STOCK",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${item.currentStock.toInt()} ${item.unit}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (item.isOutOfStock) Color(0xFFC62828) else if (item.isLowStock) Amber700 else Emerald700
                        )
                    }

                    if (item.isLowStock) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (item.isOutOfStock) Color(0xFFFFEBEE) else Color(0xFFFFF8E1)
                        ) {
                            Text(
                                text = if (item.isOutOfStock) "Out of Stock" else "Low Stock (Min: ${item.minStockAlert.toInt()})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (item.isOutOfStock) Color(0xFFC62828) else Amber700,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stock IN vs Stock OUT Toggle Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isAddition) Emerald700 else Color.Transparent)
                        .clickable {
                            isAddition = true
                            selectedReason = StockReason.PURCHASE_IN
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = if (isAddition) PureWhite else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Stock IN (+)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isAddition) PureWhite else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (!isAddition) Color(0xFFC62828) else Color.Transparent)
                        .clickable {
                            isAddition = false
                            selectedReason = StockReason.SALE_OUT
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = null,
                            tint = if (!isAddition) PureWhite else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Stock OUT (-)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (!isAddition) PureWhite else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quantity Input
            OutlinedTextField(
                value = qtyText,
                onValueChange = {
                    qtyText = it
                    if (qtyError && it.isNotBlank()) qtyError = false
                },
                label = { Text(if (isAddition) "Quantity to Add *" else "Quantity to Reduce *") },
                placeholder = { Text("0") },
                trailingIcon = { Text(item.unit, modifier = Modifier.padding(end = 12.dp), fontWeight = FontWeight.SemiBold) },
                isError = qtyError,
                supportingText = if (qtyError) { { Text("Please enter a valid quantity > 0") } } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("adjust_stock_qty_input"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )

            // Dynamic Preview
            if (qty > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isAddition) Emerald700.copy(alpha = 0.08f) else Color(0xFFFFEBEE)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Resulting Stock:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${item.currentStock.toInt()} ${if (isAddition) "+" else "-"} ${qty.toInt()} = ${previewStock.toInt()} ${item.unit}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isAddition) Emerald700 else Color(0xFFC62828)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reason Chips
            Text(
                text = "REASON FOR ADJUSTMENT",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            val currentReasons = if (isAddition) addReasons else removeReasons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                currentReasons.forEach { reason ->
                    FilterChip(
                        selected = selectedReason == reason,
                        onClick = { selectedReason = reason },
                        label = { Text(reason.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (isAddition) Emerald700.copy(alpha = 0.15f) else Color(0xFFFFCDD2),
                            selectedLabelColor = if (isAddition) Emerald700 else Color(0xFFB71C1C)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Note / Remark
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note / Reference (Optional)") },
                placeholder = { Text(if (isAddition) "e.g. Supplier Bill #405 or batch arrival" else "e.g. Damaged carton or counter sale") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("adjust_stock_note_input"),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                singleLine = true
            )

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
                        if (qty <= 0) {
                            qtyError = true
                            return@Button
                        }
                        onConfirm(signedChange, selectedReason, note.trim())
                    },
                    modifier = Modifier
                        .weight(1.5f)
                        .testTag("confirm_adjust_stock_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAddition) Emerald700 else Color(0xFFC62828)
                    )
                ) {
                    Text(
                        text = if (isAddition) "Add to Stock" else "Deduct Stock",
                        color = PureWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
