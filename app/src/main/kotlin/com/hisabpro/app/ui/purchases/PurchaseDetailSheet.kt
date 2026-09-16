package com.hisabpro.app.ui.purchases

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.hisabpro.app.data.model.InvoiceStatus
import com.hisabpro.app.data.model.PurchaseBill
import com.hisabpro.app.ui.theme.Emerald700
import com.hisabpro.app.ui.theme.Emerald800
import com.hisabpro.app.ui.theme.ExpenseRed
import com.hisabpro.app.ui.theme.IncomeGreen
import com.hisabpro.app.ui.theme.PureWhite
import com.hisabpro.app.ui.theme.Slate100
import com.hisabpro.app.ui.theme.Slate200
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseDetailSheet(
    bill: PurchaseBill,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onMarkAsPaid: (PurchaseBill) -> Unit,
    onDelete: (String) -> Unit
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Emerald700.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null,
                            tint = Emerald700,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = bill.purchaseNumber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        if (bill.vendorBillNumber.isNotBlank()) {
                            Text(
                                text = "Vendor Bill: ${bill.vendorBillNumber}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            HorizontalDivider(color = Slate200)

            // Status Badge & Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Date: ${dateFormat.format(Date(bill.dateMillis))}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    color = when (bill.paymentStatus) {
                        InvoiceStatus.PAID -> IncomeGreen.copy(alpha = 0.15f)
                        InvoiceStatus.PARTIAL -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                        InvoiceStatus.UNPAID -> ExpenseRed.copy(alpha = 0.15f)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = bill.paymentStatus.label,
                        color = when (bill.paymentStatus) {
                            InvoiceStatus.PAID -> IncomeGreen
                            InvoiceStatus.PARTIAL -> Color(0xFFD97706)
                            InvoiceStatus.UNPAID -> ExpenseRed
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Supplier Information
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate100),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "PURCHASED FROM",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = bill.supplierName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    if (bill.supplierPhone.isNotBlank()) {
                        Text(
                            text = "Phone: ${bill.supplierPhone}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (bill.supplierGstin.isNotBlank()) {
                        Text(
                            text = "GSTIN: ${bill.supplierGstin}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (bill.supplierAddress.isNotBlank()) {
                        Text(
                            text = "Address: ${bill.supplierAddress}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Items breakdown
            Text(
                text = "Items & Inward Quantities",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            bill.items.forEachIndexed { index, item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
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
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "${item.quantity} ${item.unit} @ ₹${String.format(Locale.ENGLISH, "%.2f", item.unitPrice)} | GST: ${item.gstRate.toInt()}%",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "₹${String.format(Locale.ENGLISH, "%.2f", item.getTotal(bill.gstMode))}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Emerald800
                        )
                    }
                }
            }

            // ITC & GST Status
            if (bill.itcEligible) {
                Surface(
                    color = Emerald700.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Emerald700,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Input Tax Credit (ITC) Eligible: ₹${String.format(Locale.ENGLISH, "%.2f", bill.totalTax)}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Emerald800
                        )
                    }
                }
            }

            // Summary Breakdown
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate100),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal:", fontSize = 13.sp)
                        Text("₹${String.format(Locale.ENGLISH, "%.2f", bill.subtotal)}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    if (bill.cgstTotal > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("CGST:", fontSize = 13.sp)
                            Text("₹${String.format(Locale.ENGLISH, "%.2f", bill.cgstTotal)}", fontSize = 13.sp)
                        }
                    }
                    if (bill.sgstTotal > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("SGST:", fontSize = 13.sp)
                            Text("₹${String.format(Locale.ENGLISH, "%.2f", bill.sgstTotal)}", fontSize = 13.sp)
                        }
                    }
                    if (bill.igstTotal > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("IGST:", fontSize = 13.sp)
                            Text("₹${String.format(Locale.ENGLISH, "%.2f", bill.igstTotal)}", fontSize = 13.sp)
                        }
                    }
                    if (bill.discountAmount > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Discount:", fontSize = 13.sp)
                            Text("-₹${String.format(Locale.ENGLISH, "%.2f", bill.discountAmount)}", fontSize = 13.sp, color = IncomeGreen)
                        }
                    }

                    HorizontalDivider(color = Slate200)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Grand Total:", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "₹${String.format(Locale.ENGLISH, "%.2f", bill.grandTotal)}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Emerald800
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Paid Amount (${bill.paymentMode}):", fontSize = 13.sp)
                        Text("₹${String.format(Locale.ENGLISH, "%.2f", bill.paidAmount)}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }

                    if (bill.dueAmount > 0.01) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Payable Due:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
                            Text(
                                "₹${String.format(Locale.ENGLISH, "%.2f", bill.dueAmount)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed
                            )
                        }
                    }
                }
            }

            if (bill.notes.isNotBlank()) {
                Text(
                    text = "Notes: ${bill.notes}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Actions: Mark Paid & WhatsApp Share
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { sharePurchaseVoucher(context, bill) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("btn_share_purchase_voucher"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Voucher", color = PureWhite, fontWeight = FontWeight.Bold)
                    }

                    if (!bill.isFullyPaid) {
                        Button(
                            onClick = {
                                onMarkAsPaid(bill)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("btn_mark_purchase_paid"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mark Paid", color = PureWhite, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                TextButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_delete_purchase_bill")
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete Purchase Bill", color = ExpenseRed)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Purchase Bill?") },
            text = { Text("Are you sure you want to delete purchase bill ${bill.purchaseNumber}? This will remove it from purchase registers and reverse the inward stock added to inventory.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDelete(bill.id)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun sharePurchaseVoucher(context: Context, bill: PurchaseBill) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
    val text = buildString {
        appendLine("📦 *PURCHASE INWARD VOUCHER*")
        appendLine("Bill No: ${bill.purchaseNumber}")
        if (bill.vendorBillNumber.isNotBlank()) {
            appendLine("Vendor Bill: ${bill.vendorBillNumber}")
        }
        appendLine("Supplier: ${bill.supplierName}")
        if (bill.supplierGstin.isNotBlank()) {
            appendLine("GSTIN: ${bill.supplierGstin}")
        }
        appendLine("Date: ${dateFormat.format(Date(bill.dateMillis))}")
        appendLine("--------------------------------")
        appendLine("*ITEMS RECEIVED:*")
        bill.items.forEachIndexed { i, itm ->
            appendLine("${i + 1}. ${itm.description} - ${itm.quantity} ${itm.unit} @ ₹${itm.unitPrice} + ${itm.gstRate.toInt()}% GST")
        }
        appendLine("--------------------------------")
        appendLine("Subtotal: ₹${String.format(Locale.ENGLISH, "%.2f", bill.subtotal)}")
        appendLine("Total Tax (ITC): ₹${String.format(Locale.ENGLISH, "%.2f", bill.totalTax)}")
        appendLine("*Grand Total: ₹${String.format(Locale.ENGLISH, "%.2f", bill.grandTotal)}*")
        appendLine("Paid: ₹${String.format(Locale.ENGLISH, "%.2f", bill.paidAmount)} (${bill.paymentMode})")
        if (bill.dueAmount > 0.01) {
            appendLine("⚠️ *Balance Payable: ₹${String.format(Locale.ENGLISH, "%.2f", bill.dueAmount)}*")
        } else {
            appendLine("✅ *Status: Fully Paid*")
        }
        appendLine("\nGenerated via HisabPro")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share Purchase Voucher"))
}
