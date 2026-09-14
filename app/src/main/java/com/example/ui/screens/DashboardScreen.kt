package com.example.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import com.example.ui.AccountingViewModel
import com.example.ui.components.InvoiceBillPreviewDialog
import com.example.ui.components.MetricStatCard
import com.example.ui.components.QuickActionChip
import com.example.ui.theme.CreditGreen
import com.example.ui.theme.CreditGreenLight
import com.example.ui.theme.DebitRed
import com.example.ui.theme.DebitRedLight
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.Saffron
import com.example.util.IndianAccountingUtils
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun DashboardScreen(
    viewModel: AccountingViewModel,
    onNavigateToAddSale: () -> Unit,
    onNavigateToReceivePayment: () -> Unit,
    onNavigateToAddParty: () -> Unit,
    onNavigateToAddExpense: () -> Unit,
    onNavigateToSetup: () -> Unit
) {
    val business by viewModel.business.collectAsState()
    val parties by viewModel.parties.collectAsState()
    val todayInvoices by viewModel.todayInvoices.collectAsState()
    val allInvoices by viewModel.invoices.collectAsState()
    val currentLang by viewModel.currentLanguage.collectAsState()
    val scope = rememberCoroutineScope()

    // Preview state
    var previewInvoice by remember { mutableStateOf<Invoice?>(null) }
    var previewItems by remember { mutableStateOf<List<InvoiceItem>>(emptyList()) }

    // Computations
    val todaySalesAmount = todayInvoices.sumOf { it.total }
    val totalReceivables = parties.filter { it.currentBalance > 0 }.sumOf { it.currentBalance }
    val totalPayables = parties.filter { it.currentBalance < 0 }.sumOf { abs(it.currentBalance) }

    // Cash/bank balance estimate (seeded 60,000 + sales paid - expenses)
    val totalCashReceived = allInvoices.sumOf { it.paidAmount }
    val estimatedLiquid = 60000.0 + totalCashReceived

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Business Header & GST Mode Banner
        item {
            Card(
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = DeepNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = business?.name?.ifBlank { "HisabPro Business" } ?: "HisabPro Business",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "FY ${IndianAccountingUtils.getCurrentFinancialYear()} • ${business?.state ?: "Maharashtra"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }

                        // GST Badge
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (business?.gstEnabled == true) Saffron else Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.clickable { onNavigateToSetup() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (business?.gstEnabled == true) Color.White else Color(0xFF81C784))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (business?.gstEnabled == true) "GST Enabled" else "Non-GST Mode",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Non-GST / GST Clarification Banner
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = if (business?.gstEnabled == true) Saffron else Color(0xFF81C784),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (business?.gstEnabled == true)
                                        "GST Tax Invoicing Mode"
                                    else
                                        "Non-GST Mode Active",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (business?.gstEnabled == true)
                                        "Invoices include CGST/SGST and HSN columns."
                                    else
                                        "Simple bills with Item, Qty, Rate, Amount. No tax complexity.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Stats Cards Grid
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricStatCard(
                        title = viewModel.getString("todays_sales"),
                        amount = IndianAccountingUtils.formatCurrency(todaySalesAmount),
                        subtitle = "${todayInvoices.size} bills today",
                        icon = Icons.Default.TrendingUp,
                        iconColor = DeepNavy,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stat_today_sales")
                    )

                    MetricStatCard(
                        title = viewModel.getString("cash_bank_balance"),
                        amount = IndianAccountingUtils.formatCurrency(estimatedLiquid),
                        subtitle = "Cash & Bank",
                        icon = Icons.Default.AccountBalance,
                        iconColor = Saffron,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stat_cash_balance")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricStatCard(
                        title = viewModel.getString("you_will_get"),
                        amount = IndianAccountingUtils.formatCurrency(totalReceivables),
                        subtitle = "Customer Receivables",
                        icon = Icons.Default.ArrowDownward,
                        iconColor = DebitRed,
                        cardBgColor = DebitRedLight.copy(alpha = 0.5f),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stat_receivables")
                    )

                    MetricStatCard(
                        title = viewModel.getString("you_will_give"),
                        amount = IndianAccountingUtils.formatCurrency(totalPayables),
                        subtitle = "Supplier Payables",
                        icon = Icons.Default.ArrowUpward,
                        iconColor = CreditGreen,
                        cardBgColor = CreditGreenLight.copy(alpha = 0.5f),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stat_payables")
                    )
                }
            }
        }

        // Quick Actions Row
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = viewModel.getString("quick_actions"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        QuickActionChip(
                            label = viewModel.getString("add_sale"),
                            icon = Icons.Default.Receipt,
                            color = Saffron,
                            onClick = onNavigateToAddSale,
                            testTag = "action_add_sale"
                        )
                    }
                    item {
                        QuickActionChip(
                            label = viewModel.getString("receive_payment"),
                            icon = Icons.Default.Payments,
                            color = CreditGreen,
                            onClick = onNavigateToReceivePayment,
                            testTag = "action_receive_payment"
                        )
                    }
                    item {
                        QuickActionChip(
                            label = viewModel.getString("add_party"),
                            icon = Icons.Default.PersonAdd,
                            color = DeepNavy,
                            onClick = onNavigateToAddParty,
                            testTag = "action_add_party"
                        )
                    }
                    item {
                        QuickActionChip(
                            label = viewModel.getString("add_expense"),
                            icon = Icons.Default.ShoppingBag,
                            color = Color(0xFF6A1B9A),
                            onClick = onNavigateToAddExpense,
                            testTag = "action_add_expense"
                        )
                    }
                }
            }
        }

        // Recent Invoices Section
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = viewModel.getString("recent_invoices"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${allInvoices.size} total",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        if (allInvoices.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = viewModel.getString("no_invoices"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            items(allInvoices.take(8)) { invoice ->
                val balanceDue = invoice.total - invoice.paidAmount
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 5.dp)
                        .testTag("invoice_card_${invoice.id}")
                        .clickable {
                            scope.launch {
                                previewItems = viewModel.getInvoiceItems(invoice.id)
                                previewInvoice = invoice
                            }
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (invoice.isGst) DeepNavy.copy(alpha = 0.1f) else Saffron.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = null,
                                tint = if (invoice.isGst) DeepNavy else Saffron,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = invoice.partyName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${invoice.invoiceNo} • ${IndianAccountingUtils.formatDate(invoice.date)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            if (invoice.isGst) {
                                Text(
                                    text = "GST Tax Bill",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DeepNavy,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = IndianAccountingUtils.formatCurrency(invoice.total),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = DeepNavy
                            )
                            if (balanceDue > 0) {
                                Text(
                                    text = "Due: ${IndianAccountingUtils.formatCurrency(balanceDue)} Dr",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DebitRed,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else {
                                Text(
                                    text = "Paid (${invoice.paymentMode.name})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CreditGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Bill Preview Dialog
    previewInvoice?.let { inv ->
        InvoiceBillPreviewDialog(
            invoice = inv,
            items = previewItems,
            business = business,
            onDismiss = { previewInvoice = null }
        )
    }
}
