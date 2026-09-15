package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Expense
import com.example.data.model.Invoice
import com.example.data.model.InvoiceType
import com.example.data.model.Party
import com.example.data.model.Payment
import com.example.ui.AccountingViewModel
import com.example.ui.components.BalanceBadge
import com.example.ui.theme.CreditGreen
import com.example.ui.theme.CreditGreenLight
import com.example.ui.theme.DebitRed
import com.example.ui.theme.DebitRedLight
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.Saffron
import com.example.util.IndianAccountingUtils
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: AccountingViewModel) {
    var selectedReportTab by remember { mutableIntStateOf(0) } // 0: Daily Sales, 1: Outstanding, 2: Day Book (Roznamcha)

    val allInvoices by viewModel.invoices.collectAsState()
    val todayInvoices by viewModel.todayInvoices.collectAsState()
    val allParties by viewModel.parties.collectAsState()
    val allPayments by viewModel.payments.collectAsState()
    val allExpenses by viewModel.expenses.collectAsState()

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("reports_screen")
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedReportTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = DeepNavy,
                edgePadding = 12.dp
            ) {
                Tab(
                    selected = selectedReportTab == 0,
                    onClick = { selectedReportTab = 0 },
                    text = { Text("Daily Sales", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    modifier = Modifier.testTag("tab_report_sales")
                )
                Tab(
                    selected = selectedReportTab == 1,
                    onClick = { selectedReportTab = 1 },
                    text = { Text("Outstanding", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    modifier = Modifier.testTag("tab_report_outstanding")
                )
                Tab(
                    selected = selectedReportTab == 2,
                    onClick = { selectedReportTab = 2 },
                    text = { Text("Day Book (रोजनामचा)", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    modifier = Modifier.testTag("tab_report_daybook")
                )
                Tab(
                    selected = selectedReportTab == 3,
                    onClick = { selectedReportTab = 3 },
                    text = { Text("Profit & Loss (नफ़ा-तोटा)", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    modifier = Modifier.testTag("tab_report_pnl")
                )
            }

            when (selectedReportTab) {
                0 -> DailySalesReportView(todayInvoices, allInvoices)
                1 -> OutstandingReportView(allParties)
                2 -> DayBookReportView(allInvoices, allPayments)
                3 -> ProfitAndLossReportView(allInvoices, allExpenses, allPayments)
            }
        }
    }
}

@Composable
fun DailySalesReportView(todayInvoices: List<Invoice>, allInvoices: List<Invoice>) {
    val todayTotal = todayInvoices.sumOf { it.total }
    val todayPaid = todayInvoices.sumOf { it.paidAmount }
    val todayDue = (todayTotal - todayPaid).coerceAtLeast(0.0)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepNavy),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Today's Sales Report",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = IndianAccountingUtils.formatDate(),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = IndianAccountingUtils.formatCurrency(todayTotal),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Received Inflow", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                            Text(IndianAccountingUtils.formatCurrency(todayPaid), color = Color(0xFF81C784), fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Credit Given (Dr)", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                            Text(IndianAccountingUtils.formatCurrency(todayDue), color = Saffron, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Today's Bill Details (${todayInvoices.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (todayInvoices.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No bills generated today yet.", color = Color.Gray)
                }
            }
        } else {
            items(todayInvoices) { inv ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(inv.partyName, fontWeight = FontWeight.Bold)
                            Text("#${inv.invoiceNo} • ${inv.paymentMode.name}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(IndianAccountingUtils.formatCurrency(inv.total), fontWeight = FontWeight.Bold, color = DeepNavy)
                            val due = inv.total - inv.paidAmount
                            if (due > 0) {
                                Text("Due: ${IndianAccountingUtils.formatCurrency(due)} Dr", color = DebitRed, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            } else {
                                Text("Paid", color = CreditGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OutstandingReportView(allParties: List<Party>) {
    val debtors = allParties.filter { it.currentBalance > 0 }
    val creditors = allParties.filter { it.currentBalance < 0 }

    val totalReceivable = debtors.sumOf { it.currentBalance }
    val totalPayable = creditors.sumOf { abs(it.currentBalance) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            // High Level Receivables vs Payables Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Net Outstanding Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = DebitRedLight,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("You Will Get (Dr)", fontSize = 11.sp, color = DebitRed, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    IndianAccountingUtils.formatCurrency(totalReceivable),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DebitRed
                                )
                                Text("${debtors.size} customers", fontSize = 10.sp, color = Color.Gray)
                            }
                        }

                        Surface(
                            color = CreditGreenLight,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("You Will Give (Cr)", fontSize = 11.sp, color = CreditGreen, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    IndianAccountingUtils.formatCurrency(totalPayable),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CreditGreen
                                )
                                Text("${creditors.size} suppliers", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Customers Who Owe You Money (Receivables - Dr)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = DebitRed
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (debtors.isEmpty()) {
            item {
                Text("All customer balances are fully settled! ✓", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
            }
        } else {
            items(debtors) { party ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(party.name, fontWeight = FontWeight.SemiBold)
                        if (party.phone.isNotBlank()) {
                            Text(party.phone, fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                    Text(
                        "${IndianAccountingUtils.formatCurrency(party.currentBalance)} Dr",
                        fontWeight = FontWeight.Bold,
                        color = DebitRed
                    )
                }
                HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Suppliers You Owe Money (Payables - Cr)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = CreditGreen
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (creditors.isEmpty()) {
            item {
                Text("No supplier payables pending.", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
            }
        } else {
            items(creditors) { party ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(party.name, fontWeight = FontWeight.SemiBold)
                        if (party.phone.isNotBlank()) {
                            Text(party.phone, fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                    Text(
                        "${IndianAccountingUtils.formatCurrency(abs(party.currentBalance))} Cr",
                        fontWeight = FontWeight.Bold,
                        color = CreditGreen
                    )
                }
                HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
            }
        }
    }
}

data class DayBookEntry(
    val date: Long,
    val description: String,
    val partyName: String,
    val mode: String,
    val debitAmount: Double,  // Dr
    val creditAmount: Double  // Cr
)

@Composable
fun DayBookReportView(invoices: List<Invoice>, payments: List<Payment>) {
    val entries = mutableListOf<DayBookEntry>()

    invoices.forEach { inv ->
        entries.add(
            DayBookEntry(
                date = inv.date,
                description = "Sales #${inv.invoiceNo}",
                partyName = inv.partyName,
                mode = inv.paymentMode.name,
                debitAmount = inv.total,
                creditAmount = 0.0
            )
        )
    }

    payments.forEach { pay ->
        entries.add(
            DayBookEntry(
                date = pay.date,
                description = "Payment Received",
                partyName = pay.partyName,
                mode = pay.mode.name,
                debitAmount = 0.0,
                creditAmount = pay.amount
            )
        )
    }

    val sortedEntries = entries.sortedByDescending { it.date }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Day Book (Roznamcha / दैनिक रोजनामचा)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Chronological double-entry daily journal recording all sales, collections, and transactions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Table Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEEEEEE), RoundedCornerShape(6.dp))
                    .padding(vertical = 6.dp, horizontal = 8.dp)
            ) {
                Text("Date / Particulars", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                Text("Debit (Dr)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DebitRed, modifier = Modifier.weight(1f))
                Text("Credit (Cr)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CreditGreen, modifier = Modifier.weight(1f))
            }
        }

        items(sortedEntries) { entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(2f)) {
                    Text(entry.description, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${IndianAccountingUtils.formatDate(entry.date)} • ${entry.partyName} (${entry.mode})",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Text(
                    text = if (entry.debitAmount > 0) IndianAccountingUtils.formatCurrency(entry.debitAmount) else "-",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (entry.debitAmount > 0) DebitRed else Color.Gray,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (entry.creditAmount > 0) IndianAccountingUtils.formatCurrency(entry.creditAmount) else "-",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (entry.creditAmount > 0) CreditGreen else Color.Gray,
                    modifier = Modifier.weight(1f)
                )
            }
            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.4f))
        }
    }
}

@Composable
fun ProfitAndLossReportView(
    allInvoices: List<Invoice>,
    allExpenses: List<Expense>,
    allPayments: List<Payment>
) {
    val salesInvoices = allInvoices.filter { it.type == InvoiceType.SALE }
    val purchaseInvoices = allInvoices.filter { it.type == InvoiceType.PURCHASE }

    val totalSalesRevenue = salesInvoices.sumOf { it.total }
    val totalPurchasesCost = purchaseInvoices.sumOf { it.total }
    val grossProfit = totalSalesRevenue - totalPurchasesCost

    val totalExpenses = allExpenses.sumOf { it.amount }
    val netProfit = grossProfit - totalExpenses
    val netMarginPct = if (totalSalesRevenue > 0) (netProfit / totalSalesRevenue) * 100 else 0.0

    val fyString = IndianAccountingUtils.getCurrentFinancialYear()

    val expensesByCategory = allExpenses.groupBy { it.category }
        .mapValues { it.value.sumOf { exp -> exp.amount } }
        .toList()
        .sortedByDescending { it.second }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Net Profit Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (netProfit >= 0) DeepNavy else Color(0xFF4A1515)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Financial Year (FY $fyString)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (netProfit >= 0) CreditGreen else DebitRed
                        ) {
                            Text(
                                text = if (netProfit >= 0) "PROFITABLE" else "NET LOSS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (netProfit >= 0) "Net Profit (निव्वळ नफा)" else "Net Loss (निव्वळ तोटा)",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        text = IndianAccountingUtils.formatCurrency(kotlin.math.abs(netProfit)),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (netProfit >= 0) Color(0xFF69F0AE) else Color(0xFFFF8A80)
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Net Margin: ${String.format(java.util.Locale.US, "%.1f", netMarginPct)}% of total sales",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Detailed Trading & P&L Statement
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Trading & P&L Statement (नफा-तोटा हिशोब)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = DeepNavy
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    PnlRowItem(
                        label = "Gross Sales (बिक्री महसूल)",
                        count = "${salesInvoices.size} bills",
                        amount = IndianAccountingUtils.formatCurrency(totalSalesRevenue),
                        amountColor = CreditGreen
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.4f))

                    PnlRowItem(
                        label = "Purchases (माल खरेदी)",
                        count = "${purchaseInvoices.size} bills",
                        amount = "-${IndianAccountingUtils.formatCurrency(totalPurchasesCost)}",
                        amountColor = Saffron
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.4f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Gross Profit (एकूण नफा)", fontWeight = FontWeight.Bold)
                        Text(
                            text = IndianAccountingUtils.formatCurrency(grossProfit),
                            fontWeight = FontWeight.Bold,
                            color = if (grossProfit >= 0) CreditGreen else DebitRed
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.4f))

                    PnlRowItem(
                        label = "Operating Expenses (दुकान खर्च)",
                        count = "${allExpenses.size} items",
                        amount = "-${IndianAccountingUtils.formatCurrency(totalExpenses)}",
                        amountColor = DebitRed
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 1.dp, color = Color.Gray.copy(alpha = 0.4f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Net P&L (निव्वळ नफा/तोटा)", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                            Text("FY April 1 - March 31", fontSize = 11.sp, color = Color.Gray)
                        }
                        Text(
                            text = IndianAccountingUtils.formatCurrency(netProfit),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = if (netProfit >= 0) CreditGreen else DebitRed
                        )
                    }
                }
            }
        }

        // Expense Categories Breakdown
        if (expensesByCategory.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Expense Categories (खर्च वर्गवारी)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        expensesByCategory.forEach { (cat, amt) ->
                            val progress = if (totalExpenses > 0) (amt / totalExpenses).toFloat() else 0f
                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(cat, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text(
                                        IndianAccountingUtils.formatCurrency(amt),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DebitRed
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = Saffron,
                                    trackColor = Color(0xFFEEEEEE)
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
private fun PnlRowItem(
    label: String,
    count: String,
    amount: String,
    amountColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(count, style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontSize = 11.sp)
        }
        Text(
            text = amount,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = amountColor
        )
    }
}
