package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Business
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import com.example.data.model.InvoiceType
import com.example.ui.theme.CreditGreen
import com.example.ui.theme.CreditGreenLight
import com.example.ui.theme.DebitRed
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.Saffron
import com.example.util.IndianAccountingUtils

@Composable
fun InvoiceBillPreviewDialog(
    invoice: Invoice,
    items: List<InvoiceItem>,
    business: Business?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val bizName = business?.name?.ifBlank { "Mali General Stores" } ?: "Mali General Stores"
    val bizPhone = business?.phone?.ifBlank { "9876543210" } ?: "9876543210"
    val bizAddress = business?.address?.ifBlank { "Shop No. 4, Shivaji Chowk, Pune" } ?: "Pune, Maharashtra"
    val upiId = business?.upiId?.ifBlank { "mali.kirana@okhdfcbank" } ?: "mali.kirana@okhdfcbank"
    val isGstBill = invoice.isGst && business?.gstEnabled == true

    var isThermalMode by remember { mutableStateOf(false) } // false: Standard A4, true: 58mm POS Thermal

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Top Action Bar with Thermal / Standard Switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = !isThermalMode,
                            onClick = { isThermalMode = false },
                            label = { Text("Standard A4", fontSize = 12.sp) }
                        )
                        FilterChip(
                            selected = isThermalMode,
                            onClick = { isThermalMode = true },
                            label = { Text("Thermal POS (58mm)", fontSize = 12.sp) }
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_preview_button")) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Printable Content Container
                if (isThermalMode) {
                    ThermalReceiptView(
                        bizName = bizName,
                        bizPhone = bizPhone,
                        bizAddress = bizAddress,
                        upiId = upiId,
                        invoice = invoice,
                        items = items,
                        isGstBill = isGstBill,
                        gstin = business?.gstin ?: ""
                    )
                } else {
                    StandardInvoiceView(
                        bizName = bizName,
                        bizPhone = bizPhone,
                        bizAddress = bizAddress,
                        upiId = upiId,
                        invoice = invoice,
                        items = items,
                        isGstBill = isGstBill,
                        gstin = business?.gstin ?: ""
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // UPI Pay & QR Trigger Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeepNavy.copy(alpha = 0.06f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Saffron.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.QrCode, contentDescription = null, tint = Saffron, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Bharat UPI Pay", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DeepNavy)
                                Text(upiId, fontSize = 11.sp, color = Color.Gray)
                            }
                        }

                        Button(
                            onClick = {
                                val amount = if (invoice.total - invoice.paidAmount > 0) invoice.total - invoice.paidAmount else invoice.total
                                val upiUri = Uri.parse("upi://pay?pa=$upiId&pn=${Uri.encode(bizName)}&am=$amount&cu=INR&tn=Bill_${invoice.invoiceNo}")
                                val upiIntent = Intent(Intent.ACTION_VIEW, upiUri)
                                try {
                                    context.startActivity(upiIntent)
                                } catch (e: Exception) {
                                    val chooser = Intent.createChooser(upiIntent, "Pay via UPI App")
                                    context.startActivity(chooser)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Saffron),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("pay_via_upi_button")
                        ) {
                            Text("Pay via UPI", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Actions: WhatsApp Share & Done
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val shareBody = buildString {
                                appendLine("🧾 *${bizName.uppercase()}*")
                                appendLine("Invoice: ${invoice.invoiceNo}")
                                appendLine("Date: ${IndianAccountingUtils.formatDate(invoice.date)}")
                                appendLine("Customer: ${invoice.partyName}")
                                appendLine("-------------------------")
                                items.forEach { itm ->
                                    appendLine("${itm.itemName} x ${itm.qty} = ${IndianAccountingUtils.formatCurrency(itm.qty * itm.rate)}")
                                }
                                appendLine("-------------------------")
                                appendLine("*Total: ${IndianAccountingUtils.formatCurrency(invoice.total)}*")
                                appendLine("Paid: ${IndianAccountingUtils.formatCurrency(invoice.paidAmount)}")
                                val due = invoice.total - invoice.paidAmount
                                if (due > 0) {
                                    appendLine("*Balance Due: ${IndianAccountingUtils.formatCurrency(due)}*")
                                    appendLine("Pay via UPI: $upiId")
                                }
                                appendLine("Thank you for your business! Visit again.")
                            }
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareBody)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Bill via"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CreditGreen),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_whatsapp_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("WhatsApp Share")
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(0.6f)
                            .testTag("close_dialog_button")
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
fun StandardInvoiceView(
    bizName: String,
    bizPhone: String,
    bizAddress: String,
    upiId: String,
    invoice: Invoice,
    items: List<InvoiceItem>,
    isGstBill: Boolean,
    gstin: String
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Column {
            // Header
            Text(
                text = bizName.uppercase(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = bizAddress,
                fontSize = 11.sp,
                color = Color.DarkGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Phone: $bizPhone",
                fontSize = 11.sp,
                color = Color.DarkGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            if (isGstBill && gstin.isNotBlank()) {
                Text(
                    text = "GSTIN: $gstin",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DeepNavy,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 1.dp, color = Color.Black)

            // Invoice Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (invoice.type == InvoiceType.PURCHASE) "Supplier: ${invoice.partyName}" else "Bill To: ${invoice.partyName}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "Mode: ${invoice.paymentMode.name}",
                        fontSize = 11.sp,
                        color = Color.DarkGray
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "No: ${invoice.invoiceNo}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "Date: ${IndianAccountingUtils.formatDate(invoice.date)}",
                        fontSize = 11.sp,
                        color = Color.DarkGray
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.8.dp, color = Color.LightGray)

            // Table Headers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F5F5))
                    .padding(vertical = 4.dp, horizontal = 2.dp)
            ) {
                Text(text = "Item", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.weight(2f))
                Text(text = "Qty", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black, textAlign = TextAlign.Center, modifier = Modifier.weight(0.8f))
                Text(text = "Rate", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                if (isGstBill) {
                    Text(text = "GST", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black, textAlign = TextAlign.End, modifier = Modifier.weight(0.8f))
                }
                Text(text = "Amount", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black, textAlign = TextAlign.End, modifier = Modifier.weight(1.2f))
            }

            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)

            // Item Rows
            items.forEach { item ->
                val lineAmount = item.qty * item.rate - item.discount
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = item.itemName, fontSize = 11.sp, color = Color.Black, modifier = Modifier.weight(2f))
                    Text(
                        text = "${if (item.qty % 1.0 == 0.0) item.qty.toInt() else item.qty} ${item.unit}",
                        fontSize = 11.sp,
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(0.8f)
                    )
                    Text(text = IndianAccountingUtils.formatCurrency(item.rate), fontSize = 11.sp, color = Color.Black, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                    if (isGstBill) {
                        Text(text = "${(item.cgstRate + item.sgstRate).toInt()}%", fontSize = 11.sp, color = Color.Black, textAlign = TextAlign.End, modifier = Modifier.weight(0.8f))
                    }
                    Text(text = IndianAccountingUtils.formatCurrency(lineAmount), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Black, textAlign = TextAlign.End, modifier = Modifier.weight(1.2f))
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), thickness = 0.8.dp, color = Color.Black)

            // Totals
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(0.6f)) {
                    Text("Subtotal:", fontSize = 11.sp, color = Color.DarkGray)
                    Text(IndianAccountingUtils.formatCurrency(invoice.subtotal), fontSize = 11.sp, color = Color.Black)
                }
                if (isGstBill && (invoice.cgst + invoice.sgst > 0)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(0.6f)) {
                        Text("CGST:", fontSize = 11.sp, color = Color.DarkGray)
                        Text(IndianAccountingUtils.formatCurrency(invoice.cgst), fontSize = 11.sp, color = Color.Black)
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(0.6f)) {
                        Text("SGST:", fontSize = 11.sp, color = Color.DarkGray)
                        Text(IndianAccountingUtils.formatCurrency(invoice.sgst), fontSize = 11.sp, color = Color.Black)
                    }
                }
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(0.6f)) {
                    Text("Total:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text(IndianAccountingUtils.formatCurrency(invoice.total), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DeepNavy)
                }
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(0.6f)) {
                    Text("Paid:", fontSize = 11.sp, color = CreditGreen)
                    Text(IndianAccountingUtils.formatCurrency(invoice.paidAmount), fontSize = 11.sp, color = CreditGreen)
                }
                val balanceDue = invoice.total - invoice.paidAmount
                if (balanceDue > 0) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(0.6f)) {
                        Text("Balance Due:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DebitRed)
                        Text(IndianAccountingUtils.formatCurrency(balanceDue), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DebitRed)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Simulated Bharat QR Code Matrix
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Scan to Pay with Any UPI App", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                    Text("UPI ID: $upiId", fontSize = 9.sp, color = Color.Gray)
                }
                QrCodePlaceholder(sizeDp = 50)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Thank you for your business! Goods once sold will not be taken back.",
                fontSize = 9.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ThermalReceiptView(
    bizName: String,
    bizPhone: String,
    bizAddress: String,
    upiId: String,
    invoice: Invoice,
    items: List<InvoiceItem>,
    isGstBill: Boolean,
    gstin: String
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Color(0xFFFAF9F6), // Thermal paper slight warm tint
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(4.dp))
            .padding(10.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = bizName.uppercase(),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.Black
            )
            Text(text = bizAddress, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Black)
            Text(text = "Tel: $bizPhone", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Black)
            if (isGstBill && gstin.isNotBlank()) {
                Text(text = "GSTIN: $gstin", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Black)
            }

            Text("--------------------------------", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Black)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Bill: ${invoice.invoiceNo}", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Black)
                Text(IndianAccountingUtils.formatDate(invoice.date), fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Black)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Cust: ${invoice.partyName}", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Black)
                Text("Mode: ${invoice.paymentMode.name}", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Black)
            }
            Text("--------------------------------", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Black)

            // Monospace Compact Items
            items.forEach { itm ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = "${itm.itemName} (${itm.qty}${itm.unit})",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color.Black,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = IndianAccountingUtils.formatCurrency(itm.qty * itm.rate),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }

            Text("--------------------------------", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Black)

            // Totals
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("TOTAL AMOUNT:", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                Text(IndianAccountingUtils.formatCurrency(invoice.total), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("PAID AMOUNT:", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Black)
                Text(IndianAccountingUtils.formatCurrency(invoice.paidAmount), fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Black)
            }
            val balanceDue = invoice.total - invoice.paidAmount
            if (balanceDue > 0) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("BALANCE DUE:", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DebitRed)
                    Text(IndianAccountingUtils.formatCurrency(balanceDue), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DebitRed)
                }
            }

            Text("--------------------------------", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Black)

            Spacer(modifier = Modifier.height(4.dp))
            QrCodePlaceholder(sizeDp = 46)
            Spacer(modifier = Modifier.height(4.dp))
            Text("UPI: $upiId", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color.Black)
            Text("*** THANK YOU - VISIT AGAIN ***", fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }
}

@Composable
fun QrCodePlaceholder(sizeDp: Int) {
    Canvas(modifier = Modifier.size(sizeDp.dp)) {
        val w = size.width
        val h = size.height
        // Draw white background
        drawRect(Color.White, size = Size(w, h))
        // Draw outer border
        drawRect(Color.Black, size = Size(w, h), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))

        // Draw 3 QR alignment squares
        val cornerSize = w * 0.28f
        // Top-Left
        drawRect(Color.Black, topLeft = Offset(2f, 2f), size = Size(cornerSize, cornerSize))
        drawRect(Color.White, topLeft = Offset(6f, 6f), size = Size(cornerSize - 8f, cornerSize - 8f))
        drawRect(Color.Black, topLeft = Offset(10f, 10f), size = Size(cornerSize - 16f, cornerSize - 16f))

        // Top-Right
        drawRect(Color.Black, topLeft = Offset(w - cornerSize - 2f, 2f), size = Size(cornerSize, cornerSize))
        drawRect(Color.White, topLeft = Offset(w - cornerSize + 2f, 6f), size = Size(cornerSize - 8f, cornerSize - 8f))
        drawRect(Color.Black, topLeft = Offset(w - cornerSize + 6f, 10f), size = Size(cornerSize - 16f, cornerSize - 16f))

        // Bottom-Left
        drawRect(Color.Black, topLeft = Offset(2f, h - cornerSize - 2f), size = Size(cornerSize, cornerSize))
        drawRect(Color.White, topLeft = Offset(6f, h - cornerSize + 2f), size = Size(cornerSize - 8f, cornerSize - 8f))
        drawRect(Color.Black, topLeft = Offset(10f, h - cornerSize + 6f), size = Size(cornerSize - 16f, cornerSize - 16f))

        // Center simulated dots
        val dot = w * 0.08f
        drawRect(Color.Black, topLeft = Offset(w * 0.46f, h * 0.46f), size = Size(dot, dot))
        drawRect(Color.Black, topLeft = Offset(w * 0.35f, h * 0.65f), size = Size(dot, dot))
        drawRect(Color.Black, topLeft = Offset(w * 0.65f, h * 0.35f), size = Size(dot, dot))
        drawRect(Color.Black, topLeft = Offset(w * 0.7f, h * 0.7f), size = Size(dot, dot))
    }
}
