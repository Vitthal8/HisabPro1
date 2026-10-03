package com.hisabpro.app.ui.sales

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisabpro.app.R
import com.hisabpro.app.data.model.GstMode
import com.hisabpro.app.data.model.InvoiceItem
import com.hisabpro.app.data.model.Item
import com.hisabpro.app.ui.theme.Emerald800
import com.hisabpro.app.ui.theme.ExpenseRed
import com.hisabpro.app.ui.theme.Slate100
import com.hisabpro.app.ui.theme.Slate200
import com.hisabpro.app.ui.theme.Slate50
import com.hisabpro.app.ui.theme.Slate500
import java.util.Locale

/**
 * Line Items Display Section for Invoice Creation.
 */
@Composable
fun InvoiceLineItemsSection(
    items: List<InvoiceItem>,
    gstMode: GstMode,
    availableItems: List<Item>,
    onRemoveItem: (Int) -> Unit,
    onOpenItemPicker: () -> Unit,
    onOpenBarcodeScanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Invoice Items (${items.size})",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )

            if (availableItems.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onOpenItemPicker,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Emerald800
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pick", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Emerald800)
                    }

                    OutlinedButton(
                        onClick = onOpenBarcodeScanner,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Emerald800
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Barcode", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Emerald800)
                    }
                }
            }
        }

        if (items.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate50),
                border = BorderStroke(1.dp, Slate200),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No items added yet. Pick from inventory or use '+ Add Item' below.",
                        fontSize = 12.sp,
                        color = Slate500,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items.forEachIndexed { index, item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Slate100),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${index + 1}. ${item.description}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${item.quantity} ${item.unit} @ ₹${item.unitPrice} | GST: ${item.gstRate}%",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Total: ₹${String.format(Locale.ENGLISH, "%.2f", item.getTotal(gstMode))}",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = Emerald800
                            )
                        }
                        IconButton(
                            onClick = { onRemoveItem(index) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove item",
                                tint = ExpenseRed
                            )
                        }
                    }
                }
            }
        }
    }
}
