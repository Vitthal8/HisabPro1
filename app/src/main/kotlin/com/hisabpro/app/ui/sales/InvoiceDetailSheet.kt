package com.hisabpro.app.ui.sales

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.hisabpro.app.data.model.GstMode
import com.hisabpro.app.data.model.Invoice
import com.hisabpro.app.data.model.InvoiceStatus
import com.hisabpro.app.data.model.InvoiceType
import com.hisabpro.app.data.repository.SettingsRepository
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
import com.hisabpro.app.util.InvoicePdfGenerator
import com.hisabpro.app.util.InvoiceUpiQrSheet
import androidx.compose.material3.rememberModalBottomSheetState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDetailSheet(
    invoice: Invoice,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSharePdf: (Invoice, Boolean) -> Unit,
    onDuplicate: (Invoice) -> Unit,
    onMarkAsPaid: (Invoice) -> Unit,
    onDelete: (Invoice) -> Unit,
    onEditInvoice: (Invoice) -> Unit = {}
) {
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showUpiQrSheet by remember { mutableStateOf(false) }
    val upiSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val settingsRepo = remember { SettingsRepository.getInstance(context) }
    val businessProfile by settingsRepo.profile.collectAsStateWithLifecycle()
    val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.ENGLISH) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 36.dp)
        ) {
            // Header Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Emerald700.copy(alpha = 0.12f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = Emerald700,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = invoice.invoiceNumber,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = invoice.type.label,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            HorizontalDivider(color = Slate200)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Status and Date Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Date: ${dateFormat.format(Date(invoice.dateMillis))}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = when (invoice.gstMode) {
                                GstMode.INTRA_STATE -> "Tax: CGST + SGST"
                                GstMode.INTER_STATE -> "Tax: IGST"
                                GstMode.EXEMPT -> "Non-GST / Exempt"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Emerald800
                        )
                    }

                    Surface(
                        color = when (invoice.paymentStatus) {
                            InvoiceStatus.PAID -> IncomeGreen.copy(alpha = 0.15f)
                            InvoiceStatus.PARTIAL -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                            InvoiceStatus.UNPAID -> ExpenseRed.copy(alpha = 0.15f)
                            InvoiceStatus.CANCELLED -> Color(0xFF94A3B8).copy(alpha = 0.15f)
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = invoice.paymentStatus.label.uppercase(),
                            color = when (invoice.paymentStatus) {
                                InvoiceStatus.PAID -> IncomeGreen
                                InvoiceStatus.PARTIAL -> Color(0xFFD97706)
                                InvoiceStatus.UNPAID -> ExpenseRed
                                InvoiceStatus.CANCELLED -> Color(0xFF64748B)
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                // Customer Info Card
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
                            text = "BILLED TO",
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
                                        text = invoice.customerName.take(1).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Emerald800
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = invoice.customerName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Slate900
                                )
                                if (invoice.customerPhone.isNotBlank()) {
                                    Text(
                                        text = "📞 ${invoice.customerPhone}",
                                        fontSize = 12.sp,
                                        color = Slate600
                                    )
                                }
                            }
                        }

                        if (invoice.customerGstin.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Slate100,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = "GSTIN: ${invoice.customerGstin}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Slate700,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        if (invoice.customerAddress.isNotBlank()) {
                            Text(
                                text = "📍 ${invoice.customerAddress}",
                                fontSize = 12.sp,
                                color = Slate600
                            )
                        }
                    }
                }

                // Line Items Table
                Text(
                    text = "Items & Breakdown (${invoice.items.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Slate900
                )

                invoice.items.forEachIndexed { index, item ->
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
                                            if (item.gstRate > 0) " | GST ${item.gstRate}%" else "",
                                        fontSize = 12.sp,
                                        color = Slate500
                                    )
                                }
                            }
                            Text(
                                text = "₹${String.format(Locale.ENGLISH, "%.2f", item.getTotal(invoice.gstMode))}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Emerald800
                            )
                        }
                    }
                }

                // Summary Card
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
                            text = "PAYMENT SUMMARY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate500,
                            letterSpacing = 0.5.sp
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal (Taxable):", fontSize = 13.sp, color = Slate600)
                            Text("₹${String.format(Locale.ENGLISH, "%.2f", invoice.subtotal)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Slate900)
                        }

                        if (invoice.discountAmount > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Discount:", fontSize = 13.sp, color = ExpenseRed)
                                Text("-₹${String.format(Locale.ENGLISH, "%.2f", invoice.discountAmount)}", fontSize = 13.sp, color = ExpenseRed, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        if (invoice.type == InvoiceType.TAX_INVOICE) {
                            if (invoice.gstMode == GstMode.INTRA_STATE) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("CGST:", fontSize = 13.sp, color = Slate600)
                                    Text("+₹${String.format(Locale.ENGLISH, "%.2f", invoice.cgstTotal)}", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Emerald800)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("SGST:", fontSize = 13.sp, color = Slate600)
                                    Text("+₹${String.format(Locale.ENGLISH, "%.2f", invoice.sgstTotal)}", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Emerald800)
                                }
                            } else if (invoice.gstMode == GstMode.INTER_STATE) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("IGST:", fontSize = 13.sp, color = Slate600)
                                    Text("+₹${String.format(Locale.ENGLISH, "%.2f", invoice.igstTotal)}", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Emerald800)
                                }
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
                                    text = "₹${String.format(Locale.ENGLISH, "%.2f", invoice.grandTotal)}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp,
                                    color = Emerald900
                                )
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Paid Amount:", fontSize = 13.sp, color = Slate600)
                            Text("₹${String.format(Locale.ENGLISH, "%.2f", invoice.paidAmount)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = IncomeGreen)
                        }

                        if (invoice.dueAmount > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Balance Due:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
                                Text("₹${String.format(Locale.ENGLISH, "%.2f", invoice.dueAmount)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
                            }
                        }
                    }
                }

                if (invoice.notes.isNotBlank()) {
                    Text(
                        text = "Notes: ${invoice.notes}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // UPI QR Code Button (PhonePe / GPay / Paytm compatible)
                    if (!invoice.isFullyPaid) {
                        Button(
                            onClick = { showUpiQrSheet = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald800),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_detail_show_upi_qr"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = null,
                                tint = PureWhite,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Show UPI QR (PhonePe / GPay / Paytm)",
                                color = PureWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Row 1: WhatsApp Share & PDF Share
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onSharePdf(invoice, true) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("btn_detail_share_whatsapp"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("WhatsApp", color = PureWhite, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onSharePdf(invoice, false) },
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("btn_detail_share_pdf"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share PDF", color = PureWhite, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Printing Row: Print A4 PDF & Thermal POS Receipt Slip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                InvoicePdfGenerator.printPdf(context, invoice, businessProfile)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("btn_detail_print_a4"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Print, contentDescription = null, tint = Emerald800, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Print A4 Bill", color = Emerald800, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                com.hisabpro.app.util.ThermalSlipGenerator.shareThermalSlip(context, invoice, businessProfile)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("btn_detail_thermal_slip"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Receipt, contentDescription = null, tint = Emerald800, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (businessProfile.isThermalPrinterMode) "POS (BT)" else "POS 58mm",
                                color = Emerald800,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Row 2: Edit, Duplicate & Mark Paid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onEditInvoice(invoice) },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("btn_detail_edit_invoice"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit Bill", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { onDuplicate(invoice) },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("btn_detail_duplicate"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Duplicate", fontSize = 12.sp)
                        }

                        if (!invoice.isFullyPaid) {
                            OutlinedButton(
                                onClick = { onMarkAsPaid(invoice) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("btn_detail_mark_paid"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Mark Paid", fontSize = 12.sp)
                            }
                        }
                    }

                    // Delete button
                    TextButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_detail_delete")
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete Invoice", color = ExpenseRed)
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Invoice?") },
            text = { Text("Are you sure you want to delete invoice ${invoice.invoiceNumber}? Deducted items will be restored back to your stock inventory.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(invoice)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("Delete", color = PureWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showUpiQrSheet) {
        InvoiceUpiQrSheet(
            invoiceNumber = invoice.invoiceNumber,
            amount = if (invoice.dueAmount > 0) invoice.dueAmount else invoice.grandTotal,
            customerName = invoice.customerName,
            merchantName = businessProfile.shopName.ifBlank { "HisabPro Merchant" },
            merchantUpiId = businessProfile.upiId,
            sheetState = upiSheetState,
            onDismiss = { showUpiQrSheet = false },
            onPaymentConfirmed = {
                onMarkAsPaid(invoice)
            }
        )
    }
}
