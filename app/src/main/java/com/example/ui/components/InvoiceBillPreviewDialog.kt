package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.ui.theme.CreditGreen
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
    val isGstBill = invoice.isGst && business?.gstEnabled == true

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isGstBill) "TAX INVOICE" else "ESTIMATE / CASH BILL",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isGstBill) DeepNavy else Saffron
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_preview_button")) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Printable Receipt Canvas
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        // Shop Header
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
                        if (isGstBill && !business?.gstin.isNullOrBlank()) {
                            Text(
                                text = "GSTIN: ${business?.gstin}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DeepNavy,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            thickness = 1.dp,
                            color = Color.Black
                        )

                        // Invoice & Customer Info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Bill To: ${invoice.partyName}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Text(
                                    text = "Pay Mode: ${invoice.paymentMode.name}",
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

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            thickness = 0.8.dp,
                            color = Color.LightGray
                        )

                        // Items Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF5F5F5))
                                .padding(vertical = 4.dp, horizontal = 2.dp)
                        ) {
                            Text(
                                text = "Item",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.weight(2f)
                            )
                            Text(
                                text = "Qty",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(0.8f)
                            )
                            Text(
                                text = "Rate",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f)
                            )
                            if (isGstBill) {
                                Text(
                                    text = "GST",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(0.8f)
                                )
                            }
                            Text(
                                text = "Amount",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1.2f)
                            )
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
                                Text(
                                    text = item.itemName,
                                    fontSize = 11.sp,
                                    color = Color.Black,
                                    modifier = Modifier.weight(2f)
                                )
                                Text(
                                    text = "${if (item.qty % 1.0 == 0.0) item.qty.toInt() else item.qty} ${item.unit}",
                                    fontSize = 11.sp,
                                    color = Color.Black,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(0.8f)
                                )
                                Text(
                                    text = IndianAccountingUtils.formatCurrency(item.rate),
                                    fontSize = 11.sp,
                                    color = Color.Black,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isGstBill) {
                                    Text(
                                        text = "${(item.cgstRate + item.sgstRate).toInt()}%",
                                        fontSize = 11.sp,
                                        color = Color.Black,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.weight(0.8f)
                                    )
                                }
                                Text(
                                    text = IndianAccountingUtils.formatCurrency(lineAmount),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.Black,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1.2f)
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            thickness = 0.8.dp,
                            color = Color.Black
                        )

                        // Totals Summary
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.End
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(0.6f)
                            ) {
                                Text("Subtotal:", fontSize = 11.sp, color = Color.DarkGray)
                                Text(IndianAccountingUtils.formatCurrency(invoice.subtotal), fontSize = 11.sp, color = Color.Black)
                            }
                            if (isGstBill && (invoice.cgst + invoice.sgst > 0)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth(0.6f)
                                ) {
                                    Text("CGST:", fontSize = 11.sp, color = Color.DarkGray)
                                    Text(IndianAccountingUtils.formatCurrency(invoice.cgst), fontSize = 11.sp, color = Color.Black)
                                }
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth(0.6f)
                                ) {
                                    Text("SGST:", fontSize = 11.sp, color = Color.DarkGray)
                                    Text(IndianAccountingUtils.formatCurrency(invoice.sgst), fontSize = 11.sp, color = Color.Black)
                                }
                            }
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(0.6f)
                            ) {
                                Text("Total:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                Text(IndianAccountingUtils.formatCurrency(invoice.total), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DeepNavy)
                            }
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(0.6f)
                            ) {
                                Text("Paid:", fontSize = 11.sp, color = CreditGreen)
                                Text(IndianAccountingUtils.formatCurrency(invoice.paidAmount), fontSize = 11.sp, color = CreditGreen)
                            }
                            val balanceDue = invoice.total - invoice.paidAmount
                            if (balanceDue > 0) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth(0.6f)
                                ) {
                                    Text("Balance Due:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DebitRed)
                                    Text(IndianAccountingUtils.formatCurrency(balanceDue), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DebitRed)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Footer declaration
                        Text(
                            text = "Thank you for your business! Goods once sold will not be taken back.",
                            fontSize = 9.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "Authorised Signatory",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.DarkGray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                                }
                                appendLine("Thank you for shopping with us!")
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
                        Icon(Icons.Default.Share, contentDescription = null)
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
