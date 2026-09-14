package com.example.ui.components

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Invoice
import com.example.data.model.Party
import com.example.data.model.Payment
import com.example.ui.AccountingViewModel
import com.example.ui.theme.CreditGreen
import com.example.ui.theme.DebitRed
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.Saffron
import com.example.util.IndianAccountingUtils

data class LedgerEntry(
    val date: Long,
    val description: String,
    val reference: String,
    val debitAmount: Double,  // Dr (Sale / Bill amount due)
    val creditAmount: Double, // Cr (Payment received)
    val isInvoice: Boolean
)

@Composable
fun PartyLedgerDialog(
    party: Party,
    viewModel: AccountingViewModel,
    onDismiss: () -> Unit,
    onReceivePaymentClick: () -> Unit
) {
    val invoices by viewModel.getPartyInvoices(party.id).collectAsState(initial = emptyList())
    val payments by viewModel.getPartyPayments(party.id).collectAsState(initial = emptyList())

    // Combine invoices and payments in chronological order
    val entries = mutableListOf<LedgerEntry>()

    invoices.forEach { inv ->
        val due = inv.total - inv.paidAmount
        entries.add(
            LedgerEntry(
                date = inv.date,
                description = "Sale Bill #${inv.invoiceNo}",
                reference = if (inv.paidAmount > 0) "Paid: ${IndianAccountingUtils.formatCurrency(inv.paidAmount)}" else "Unpaid",
                debitAmount = inv.total,
                creditAmount = 0.0,
                isInvoice = true
            )
        )
    }

    payments.forEach { pay ->
        entries.add(
            LedgerEntry(
                date = pay.date,
                description = "Payment (${pay.mode.name})",
                reference = pay.referenceNo.ifBlank { pay.notes },
                debitAmount = 0.0,
                creditAmount = pay.amount,
                isInvoice = false
            )
        )
    }

    val sortedEntries = entries.sortedByDescending { it.date }

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
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = party.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = DeepNavy
                        )
                        if (party.phone.isNotBlank()) {
                            Text(
                                text = "📱 ${party.phone}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_ledger_button")) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Summary Balance Bar
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
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
                        Column {
                            Text(
                                text = "Current Ledger Balance",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Text(
                                text = IndianAccountingUtils.formatBalanceDrCr(party.currentBalance),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (party.currentBalance > 0) DebitRed else CreditGreen
                            )
                        }
                        if (party.currentBalance > 0) {
                            Button(
                                onClick = {
                                    onDismiss()
                                    onReceivePaymentClick()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CreditGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("ledger_receive_payment_button")
                            ) {
                                Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Receive", fontSize = 13.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Transaction History (Roznamcha Ledger)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (sortedEntries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No transactions recorded yet for this party.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                    ) {
                        items(sortedEntries) { item ->
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (item.isInvoice) Saffron.copy(alpha = 0.15f) else CreditGreen.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (item.isInvoice) Icons.Default.Receipt else Icons.Default.Payments,
                                            contentDescription = null,
                                            tint = if (item.isInvoice) Saffron else CreditGreen,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${IndianAccountingUtils.formatDate(item.date)} • ${item.reference}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        if (item.debitAmount > 0) {
                                            Text(
                                                text = "+${IndianAccountingUtils.formatCurrency(item.debitAmount)} Dr",
                                                color = DebitRed,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                        if (item.creditAmount > 0) {
                                            Text(
                                                text = "-${IndianAccountingUtils.formatCurrency(item.creditAmount)} Cr",
                                                color = CreditGreen,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            }
                        }
                    }
                }
            }
        }
    }
}
