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
import androidx.compose.material.icons.filled.FileDownload
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
import androidx.compose.foundation.BorderStroke
import com.hisabpro.app.ui.theme.Emerald50
import com.hisabpro.app.ui.theme.Emerald700
import com.hisabpro.app.ui.theme.Emerald800
import com.hisabpro.app.ui.theme.Emerald900
import com.hisabpro.app.ui.theme.ExpenseRed
import com.hisabpro.app.ui.theme.IncomeGreen
import com.hisabpro.app.ui.theme.PureWhite
import com.hisabpro.app.ui.theme.Slate50
import com.hisabpro.app.ui.theme.Slate100
import com.hisabpro.app.ui.theme.Slate200
import com.hisabpro.app.ui.theme.Slate400
import com.hisabpro.app.ui.theme.Slate500
import com.hisabpro.app.ui.theme.Slate600
import com.hisabpro.app.ui.theme.Slate700
import com.hisabpro.app.ui.theme.Slate800
import com.hisabpro.app.ui.theme.Slate900
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
    onDelete: (String) -> Unit,
    onExportCsv: (PurchaseBill) -> Unit = {}
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Slate200),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "PURCHASED FROM",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate500,
                        letterSpacing = 0.5.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Emerald50,
                            border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.3f)),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = bill.supplierName.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Emerald800
                                )
                            }
                        }
                        Column {
                            Text(
                                text = bill.supplierName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Slate900
                            )
                            if (bill.supplierPhone.isNotBlank()) {
                                Text(
                                    text = "📞 ${bill.supplierPhone}",
                                    fontSize = 12.sp,
                                    color = Slate600
                                )
                            }
                        }
                    }

                    if (bill.supplierGstin.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Slate100,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = "GSTIN: ${bill.supplierGstin}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Slate700,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    if (bill.supplierAddress.isNotBlank()) {
                        Text(
                            text = "📍 ${bill.supplierAddress}",
                            fontSize = 12.sp,
                            color = Slate600
                        )
                    }
                }
            }

            // Items breakdown
            Text(
                text = "Items & Inward Quantities (${bill.items.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Slate900
            )

            bill.items.forEachIndexed { index, item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Slate200),
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
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Slate100,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${index + 1}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate600
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = item.description,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = Slate900
                                )
                                Text(
                                    text = "${item.quantity} ${item.unit} @ ₹${String.format(Locale.ENGLISH, "%.2f", item.unitPrice)}" +
                                        if (item.gstRate > 0) " | GST ${item.gstRate.toInt()}%" else "",
                                    fontSize = 12.sp,
                                    color = Slate500
                                )
                            }
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
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    shape = RoundedCornerShape(12.dp),
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
                            tint = Color(0xFF1D4ED8),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Input Tax Credit (ITC) Claimable",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF1E40AF)
                            )
                            Text(
                                text = "Eligible GST Credit: ₹${String.format(Locale.ENGLISH, "%.2f", bill.totalTax)}",
                                fontSize = 12.sp,
                                color = Color(0xFF2563EB)
                            )
                        }
                    }
                }
            }

            // Summary Breakdown
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Slate200),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "PURCHASE SUMMARY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate500,
                        letterSpacing = 0.5.sp
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal:", fontSize = 13.sp, color = Slate600)
                        Text("₹${String.format(Locale.ENGLISH, "%.2f", bill.subtotal)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Slate900)
                    }
                    if (bill.cgstTotal > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("CGST:", fontSize = 13.sp, color = Slate600)
                            Text("+₹${String.format(Locale.ENGLISH, "%.2f", bill.cgstTotal)}", fontSize = 13.sp, color = Emerald800, fontWeight = FontWeight.Medium)
                        }
                    }
                    if (bill.sgstTotal > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("SGST:", fontSize = 13.sp, color = Slate600)
                            Text("+₹${String.format(Locale.ENGLISH, "%.2f", bill.sgstTotal)}", fontSize = 13.sp, color = Emerald800, fontWeight = FontWeight.Medium)
                        }
                    }
                    if (bill.igstTotal > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("IGST:", fontSize = 13.sp, color = Slate600)
                            Text("+₹${String.format(Locale.ENGLISH, "%.2f", bill.igstTotal)}", fontSize = 13.sp, color = Emerald800, fontWeight = FontWeight.Medium)
                        }
                    }
                    if (bill.discountAmount > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Discount:", fontSize = 13.sp, color = IncomeGreen)
                            Text("-₹${String.format(Locale.ENGLISH, "%.2f", bill.discountAmount)}", fontSize = 13.sp, color = IncomeGreen, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Grand Total Box
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Emerald50,
                        border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("GRAND TOTAL", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Emerald900)
                            Text(
                                "₹${String.format(Locale.ENGLISH, "%.2f", bill.grandTotal)}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Emerald900
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Paid Amount (${bill.paymentMode}):", fontSize = 13.sp, color = Slate600)
                        Text("₹${String.format(Locale.ENGLISH, "%.2f", bill.paidAmount)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = IncomeGreen)
                    }

                    if (bill.dueAmount > 0.01) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Payable Due:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
                            Text(
                                "₹${String.format(Locale.ENGLISH, "%.2f", bill.dueAmount)}",
                                fontSize = 14.sp,
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { sharePurchaseVoucher(context, bill) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_share_purchase_voucher"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = PureWhite, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share Voucher", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    OutlinedButton(
                        onClick = { onExportCsv(bill) },
                        border = BorderStroke(1.dp, Emerald700),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_export_purchase_csv"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, tint = Emerald700, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export CSV", color = Emerald700, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                if (!bill.isFullyPaid) {
                    Button(
                        onClick = {
                            onMarkAsPaid(bill)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_mark_purchase_paid"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = PureWhite, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mark as Fully Paid", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                TextButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("btn_delete_purchase_bill")
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete Inward Bill", color = ExpenseRed, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
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
