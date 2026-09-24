package com.hisabpro.app.ui.dashboard

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisabpro.app.data.model.BusinessProfile
import com.hisabpro.app.data.model.Invoice
import com.hisabpro.app.data.model.InvoiceStatus
import com.hisabpro.app.data.model.InvoiceType
import com.hisabpro.app.data.model.Item
import com.hisabpro.app.data.model.Party
import com.hisabpro.app.data.model.PaymentMode
import com.hisabpro.app.data.model.Transaction
import com.hisabpro.app.data.model.TransactionType
import com.hisabpro.app.ui.theme.Amber700
import com.hisabpro.app.ui.theme.DeepNavyBlue
import com.hisabpro.app.ui.theme.DeepNavyLight
import com.hisabpro.app.ui.theme.Emerald700
import com.hisabpro.app.ui.theme.ExpenseRed
import com.hisabpro.app.ui.theme.ExpenseRedContainer
import com.hisabpro.app.ui.theme.IncomeGreen
import com.hisabpro.app.ui.theme.IncomeGreenContainer
import com.hisabpro.app.ui.theme.PureWhite
import com.hisabpro.app.ui.theme.SaffronDark
import com.hisabpro.app.ui.theme.SaffronLight
import com.hisabpro.app.ui.theme.SaffronOrange
import com.hisabpro.app.ui.theme.Slate200
import com.hisabpro.app.ui.theme.Slate700
import com.hisabpro.app.ui.theme.Slate800
import com.hisabpro.app.util.IndianAccountingFormat
import java.util.Calendar

@Composable
fun DashboardScreen(
    profile: BusinessProfile,
    invoices: List<Invoice>,
    totalReceivablesDr: Double,
    totalPayablesCr: Double,
    cashInHand: Double,
    bankBalance: Double,
    items: List<Item>,
    onNewSaleClick: () -> Unit,
    onRecordPaymentClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onAddPartyClick: () -> Unit,
    onAddPurchaseClick: (() -> Unit)? = null,
    onDaybookClick: () -> Unit,
    onCashbookClick: () -> Unit,
    onViewAllSalesClick: () -> Unit,
    onViewAllPartiesClick: () -> Unit,
    onInvoiceClick: (Invoice) -> Unit,
    onItemClick: (Item) -> Unit,
    onSetupBusinessClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = profile.appLanguage

    // Calculations for Today's date
    val todayMidnight = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val todayInvoices = remember(invoices, todayMidnight) {
        invoices.filter { it.dateMillis >= todayMidnight }
    }

    val todaySalesAmount = remember(todayInvoices) {
        todayInvoices.sumOf { it.grandTotal }
    }

    // Low stock items
    val lowStockItems = remember(items) {
        items.filter { it.isLowStock || it.isOutOfStock }
    }

    // Recent 5 Invoices
    val recentInvoices = remember(invoices) {
        invoices.sortedByDescending { it.dateMillis }.take(5)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Top Header Banner with Indian SMB Styling
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(DeepNavyBlue, Color(0xFF0E1343))
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = profile.shopName,
                                    color = PureWhite,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (profile.isGstRegistered) Emerald700 else SaffronOrange
                                ) {
                                    Text(
                                        text = if (profile.isGstRegistered) "GST: ${profile.gstin.take(2)}..." else "Non-GST Shop",
                                        color = PureWhite,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = "• FY ${IndianAccountingFormat.getFinancialYear()}",
                                    color = PureWhite.copy(alpha = 0.8f),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = onSetupBusinessClick,
                            modifier = Modifier.testTag("dashboard_settings_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Business Setup & GST Settings",
                                tint = PureWhite
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Today's Sales Hero Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite)
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
                                    text = IndianAccountingFormat.t("today_sales", lang).uppercase(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Slate700,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = IndianAccountingFormat.formatIndianCurrency(todaySalesAmount),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DeepNavyBlue
                                )
                                Text(
                                    text = "${todayInvoices.size} bills created today",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate700
                                )
                            }

                            Button(
                                onClick = onNewSaleClick,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SaffronOrange),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                modifier = Modifier.testTag("hero_new_sale_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FlashOn,
                                    contentDescription = null,
                                    tint = PureWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "+ Sale",
                                    fontWeight = FontWeight.Bold,
                                    color = PureWhite
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick Actions Row (Minimum 48dp Touch Targets for busy shop owners)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Quick Actions",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate700
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        QuickActionButton(
                            icon = Icons.Default.ReceiptLong,
                            label = "Add Sale",
                            color = SaffronOrange,
                            onClick = onNewSaleClick,
                            tag = "quick_action_sale"
                        )
                        QuickActionButton(
                            icon = Icons.Default.ArrowDownward,
                            label = "Payment In",
                            color = IncomeGreen,
                            onClick = onRecordPaymentClick,
                            tag = "quick_action_payment_in"
                        )
                        QuickActionButton(
                            icon = Icons.Default.PersonAdd,
                            label = "Add Party",
                            color = DeepNavyBlue,
                            onClick = onAddPartyClick,
                            tag = "quick_action_party"
                        )
                        QuickActionButton(
                            icon = Icons.Default.ArrowUpward,
                            label = "Add Expense",
                            color = ExpenseRed,
                            onClick = onAddExpenseClick,
                            tag = "quick_action_expense"
                        )
                        QuickActionButton(
                            icon = Icons.Default.Inventory2,
                            label = "Add Purchase",
                            color = Emerald700,
                            onClick = { onAddPurchaseClick?.invoke() ?: onDaybookClick() },
                            tag = "quick_action_purchase"
                        )
                    }
                }
            }
        }

        // Indian Accounting: Outstanding Receivables (Dr) & Payables (Cr)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // To Collect (Receivables Dr)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onViewAllPartiesClick() },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = IncomeGreenContainer.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "To Collect (Dr)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = IncomeGreen
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = IncomeGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = IndianAccountingFormat.formatIndianCurrency(totalReceivablesDr),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = IncomeGreen
                        )
                        Text(
                            text = "Customer dues",
                            fontSize = 11.sp,
                            color = Slate700
                        )
                    }
                }

                // To Pay (Payables Cr)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onViewAllPartiesClick() },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = ExpenseRedContainer.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "To Pay (Cr)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = ExpenseRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = IndianAccountingFormat.formatIndianCurrency(totalPayablesCr),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ExpenseRed
                        )
                        Text(
                            text = "Supplier payables",
                            fontSize = 11.sp,
                            color = Slate700
                        )
                    }
                }
            }
        }

        // Cash Book vs Bank Book Balances
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clickable { onCashbookClick() },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Emerald700.copy(alpha = 0.12f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = Emerald700,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text("Cash in Hand", fontSize = 11.sp, color = Slate700)
                            Text(
                                text = IndianAccountingFormat.formatIndianCurrency(cashInHand),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (cashInHand >= 0) Slate800 else ExpenseRed
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .height(30.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = DeepNavyBlue.copy(alpha = 0.12f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PointOfSale,
                                    contentDescription = null,
                                    tint = DeepNavyBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text("Bank / Online", fontSize = 11.sp, color = Slate700)
                            Text(
                                text = IndianAccountingFormat.formatIndianCurrency(bankBalance),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (bankBalance >= 0) Slate800 else ExpenseRed
                            )
                        }
                    }
                }
            }
        }

        // Low Stock Reorder Banner (if any)
        if (lowStockItems.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Amber700.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Amber700,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = "${lowStockItems.size} Item(s) Need Reordering",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Amber700
                                )
                                Text(
                                    text = lowStockItems.take(2).joinToString { it.name },
                                    fontSize = 11.sp,
                                    color = Slate700
                                )
                            }
                        }

                        Text(
                            text = "View >",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Amber700,
                            modifier = Modifier.clickable { onItemClick(lowStockItems.first()) }
                        )
                    }
                }
            }
        }

        // Recent Invoices Section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Bills & Invoices",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "View All (${invoices.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = Emerald700,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onViewAllSalesClick() }
                )
            }
        }

        if (recentInvoices.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No invoices created yet",
                            fontWeight = FontWeight.SemiBold,
                            color = Slate700
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onNewSaleClick,
                            colors = ButtonDefaults.buttonColors(containerColor = SaffronOrange)
                        ) {
                            Text("+ Create First Invoice", color = PureWhite)
                        }
                    }
                }
            }
        } else {
            items(recentInvoices, key = { it.id }) { invoice ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { onInvoiceClick(invoice) }
                        .testTag("dashboard_invoice_${invoice.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (invoice.type == InvoiceType.NON_GST_BILL) SaffronLight else DeepNavyLight,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.ReceiptLong,
                                        contentDescription = null,
                                        tint = if (invoice.type == InvoiceType.NON_GST_BILL) SaffronOrange else DeepNavyBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = invoice.customerName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${invoice.invoiceNumber} • ${IndianAccountingFormat.formatIndianDate(invoice.dateMillis)}",
                                    fontSize = 11.sp,
                                    color = Slate700
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = IndianAccountingFormat.formatIndianCurrency(invoice.grandTotal),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = Slate800
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = when (invoice.paymentStatus) {
                                    InvoiceStatus.PAID -> IncomeGreenContainer
                                    InvoiceStatus.PARTIAL -> Amber700.copy(alpha = 0.15f)
                                    InvoiceStatus.UNPAID -> ExpenseRedContainer
                                }
                            ) {
                                Text(
                                    text = invoice.paymentStatus.label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (invoice.paymentStatus) {
                                        InvoiceStatus.PAID -> IncomeGreen
                                        InvoiceStatus.PARTIAL -> Amber700
                                        InvoiceStatus.UNPAID -> ExpenseRed
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    tag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .testTag(tag)
            .padding(4.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.12f),
            modifier = Modifier.size(48.dp) // Minimum 48dp touch target
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Slate800
        )
    }
}
