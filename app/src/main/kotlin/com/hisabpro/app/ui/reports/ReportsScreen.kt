package com.hisabpro.app.ui.reports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hisabpro.app.ui.theme.DeepNavyBlue
import com.hisabpro.app.ui.theme.Emerald700
import com.hisabpro.app.ui.theme.Emerald800
import com.hisabpro.app.ui.theme.Emerald900
import com.hisabpro.app.ui.theme.PureWhite
import com.hisabpro.app.ui.theme.SaffronOrange
import com.hisabpro.app.ui.theme.Slate100
import com.hisabpro.app.ui.theme.Slate200
import com.hisabpro.app.ui.theme.Slate500
import androidx.compose.ui.res.stringResource
import com.hisabpro.app.R
import com.hisabpro.app.ui.theme.Slate600
import com.hisabpro.app.ui.theme.Slate700
import com.hisabpro.app.ui.theme.Slate800
import com.hisabpro.app.ui.theme.Slate900
import com.hisabpro.app.util.IndianAccountingFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Sheet states for the 8 Core Priority Reports
    var showDailySalesSheet by remember { mutableStateOf(false) }
    var showSalesDateRangeSheet by remember { mutableStateOf(false) }
    var showOutstandingSheet by remember { mutableStateOf(false) }
    var showCustomerLedgerSheet by remember { mutableStateOf(false) }
    var showSupplierLedgerSheet by remember { mutableStateOf(false) }
    var showExpenseSheet by remember { mutableStateOf(false) }
    var showCashBookSheet by remember { mutableStateOf(false) }
    var showDaybookSheet by remember { mutableStateOf(false) }

    // Sheet states for Advanced Reports
    var showGstrSheet by remember { mutableStateOf(false) }
    var showGstr3bSheet by remember { mutableStateOf(false) }
    var showPlSheet by remember { mutableStateOf(false) }
    var showStockSheet by remember { mutableStateOf(false) }
    var showPurchasesRegisterSheet by remember { mutableStateOf(false) }
    var showTrialBalanceSheet by remember { mutableStateOf(false) }

    val dailySalesSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val salesDateRangeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val outstandingSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val customerLedgerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val supplierLedgerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val expenseSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val cashBookSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val daybookSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val gstrSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val gstr3bSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val plSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val stockSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val purchasesRegisterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val trialBalanceSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PureWhite)
            .testTag("screen_reports")
    ) {
        // Top Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Emerald900)
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.reports),
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Text(
                        text = "दैनिक हिशोब, उधारी, खातेवही व कर अहवाल",
                        color = PureWhite.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = PureWhite.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Offline-First",
                            color = PureWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Period Selector Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReportPeriod.entries.forEach { period ->
                val isSelected = uiState.selectedPeriod == period
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setPeriod(period) },
                    label = {
                        Text(
                            text = period.label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Emerald800,
                        selectedLabelColor = PureWhite,
                        containerColor = Color(0xFFF1F5F9),
                        labelColor = Color(0xFF475569)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("chip_period_${period.name}")
                )
            }
        }

        // Segmented Tabs: Core Business Reports vs Advanced Accounting Reports
        TabRow(
            selectedTabIndex = uiState.selectedReportsTab,
            containerColor = Slate100,
            modifier = Modifier.fillMaxWidth(),
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[uiState.selectedReportsTab]),
                    color = if (uiState.selectedReportsTab == 0) Emerald800 else SaffronOrange
                )
            }
        ) {
            Tab(
                selected = uiState.selectedReportsTab == 0,
                onClick = { viewModel.setReportsTab(0) },
                text = {
                    Text(
                        text = "Business Reports (8)",
                        fontSize = 13.sp,
                        fontWeight = if (uiState.selectedReportsTab == 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (uiState.selectedReportsTab == 0) Emerald800 else Slate600
                    )
                },
                modifier = Modifier.testTag("tab_core_business_reports")
            )
            Tab(
                selected = uiState.selectedReportsTab == 1,
                onClick = { viewModel.setReportsTab(1) },
                text = {
                    Text(
                        text = "Advanced & Tax",
                        fontSize = 13.sp,
                        fontWeight = if (uiState.selectedReportsTab == 1) FontWeight.Bold else FontWeight.Normal,
                        color = if (uiState.selectedReportsTab == 1) SaffronOrange else Slate600
                    )
                },
                modifier = Modifier.testTag("tab_advanced_accounting_reports")
            )
        }

        // Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Executive Financial Pulse Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Slate200),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "FINANCIAL PULSE (${uiState.selectedPeriod.label})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Emerald800,
                            letterSpacing = 0.5.sp
                        )

                        val isNetProfitable = uiState.profitLoss.netProfit >= 0
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isNetProfitable) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                        ) {
                            Text(
                                text = if (isNetProfitable) "Profitable" else "Loss",
                                color = if (isNetProfitable) Color(0xFF16A34A) else Color(0xFFDC2626),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Net Business Profit", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(
                                text = IndianAccountingFormat.formatIndianCurrency(uiState.profitLoss.netProfit),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (uiState.profitLoss.netProfit >= 0) Color(0xFF16A34A) else Color(0xFFDC2626)
                            )
                            Text(
                                text = "${String.format(Locale.ENGLISH, "%.1f", uiState.profitLoss.netMarginPercent)}% margin",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Sales Turnover", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(
                                text = IndianAccountingFormat.formatIndianCurrency(uiState.profitLoss.salesRevenue),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Emerald900
                            )
                            if (uiState.isGstRegistered) {
                                Text(
                                    text = "Tax ${IndianAccountingFormat.formatIndianCurrency(uiState.gstr1.totalTax)}",
                                    fontSize = 11.sp,
                                    color = Color(0xFFD97706),
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else {
                                Text(
                                    text = "Non-GST Store",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("You'll Receive (येणे)", fontSize = 10.sp, color = Color(0xFF64748B))
                            Text(
                                text = IndianAccountingFormat.formatIndianCurrency(uiState.outstanding.totalReceivable),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF16A34A)
                            )
                        }

                        Column {
                            Text("You'll Pay (देणे)", fontSize = 10.sp, color = Color(0xFF64748B))
                            Text(
                                text = IndianAccountingFormat.formatIndianCurrency(uiState.outstanding.totalPayable),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Cash in Hand", fontSize = 10.sp, color = Color(0xFF64748B))
                            Text(
                                text = IndianAccountingFormat.formatIndianCurrency(uiState.cashBookReport.closingBalance),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Emerald800
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // TAB 0: Core 8 Priority Reports
            if (uiState.selectedReportsTab == 0) {
                Text(
                    text = "Priority Business Reports",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Emerald900,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 1. Daily Sales Report
                ReportNavigationCard(
                    icon = Icons.Default.PointOfSale,
                    iconColor = SaffronOrange,
                    title = "1. Daily Sales Report (आजची विक्री)",
                    subtitle = "Today's bills, cash, UPI and credit sales breakdown",
                    metricHighlight = IndianAccountingFormat.formatIndianCurrency(uiState.dailySales.totalSales),
                    badge = "${uiState.dailySales.invoiceCount} Bills",
                    badgeColor = SaffronOrange,
                    testTag = "card_report_daily_sales",
                    onClick = { showDailySalesSheet = true }
                )

                // 2. Sales by Date Range Report
                ReportNavigationCard(
                    icon = Icons.Default.DateRange,
                    iconColor = Emerald800,
                    title = "2. Sales by Date Range (विक्री नोंद)",
                    subtitle = "Turnover, collected vs pending credit in ${uiState.selectedPeriod.label}",
                    metricHighlight = IndianAccountingFormat.formatIndianCurrency(uiState.salesDateRange.totalSales),
                    badge = "${uiState.salesDateRange.invoiceCount} Invoices",
                    badgeColor = Emerald800,
                    testTag = "card_report_sales_range",
                    onClick = { showSalesDateRangeSheet = true }
                )

                // 3. Outstanding Report
                ReportNavigationCard(
                    icon = Icons.Default.HourglassBottom,
                    iconColor = Color(0xFFEA580C),
                    title = "3. Outstanding Report (उधारी येणे-देणे)",
                    subtitle = "Receivables (Customers) vs Payables (Suppliers) & Aging",
                    metricHighlight = "${IndianAccountingFormat.formatIndianCurrency(uiState.outstanding.totalReceivable)} To Get",
                    badge = "${uiState.outstanding.customerCount} Debtors",
                    badgeColor = Color(0xFFEA580C),
                    testTag = "card_report_outstanding",
                    onClick = { showOutstandingSheet = true }
                )

                // 4. Customer Ledger
                ReportNavigationCard(
                    icon = Icons.Default.Person,
                    iconColor = Color(0xFF16A34A),
                    title = "4. Customer Ledger (ग्राहक खातेवही)",
                    subtitle = "Debit (Dr), Credit (Cr) running statement & balance",
                    metricHighlight = "${uiState.allCustomers.size} Accounts",
                    badge = "Dr / Cr Khata",
                    badgeColor = Color(0xFF16A34A),
                    testTag = "card_report_customer_ledger",
                    onClick = { showCustomerLedgerSheet = true }
                )

                // 5. Supplier Ledger
                ReportNavigationCard(
                    icon = Icons.Default.Store,
                    iconColor = Color(0xFF0D9488),
                    title = "5. Supplier Ledger (व्यापारी खातेवही)",
                    subtitle = "Inward purchase bills, payments made & running dues",
                    metricHighlight = "${uiState.allSuppliers.size} Vendors",
                    badge = "Vendor Khata",
                    badgeColor = Color(0xFF0D9488),
                    testTag = "card_report_supplier_ledger",
                    onClick = { showSupplierLedgerSheet = true }
                )

                // 6. Expense Report
                ReportNavigationCard(
                    icon = Icons.Default.MoneyOff,
                    iconColor = Color(0xFFDC2626),
                    title = "6. Expense Report (खर्च अहवाल)",
                    subtitle = "Category breakdown, payment modes & spending analysis",
                    metricHighlight = IndianAccountingFormat.formatIndianCurrency(uiState.expenseReport.totalExpenses),
                    badge = "${uiState.expenseReport.expenseCount} Vouchers",
                    badgeColor = Color(0xFFDC2626),
                    testTag = "card_report_expense",
                    onClick = { showExpenseSheet = true }
                )

                // 7. Cash Book
                ReportNavigationCard(
                    icon = Icons.Default.Payments,
                    iconColor = Emerald700,
                    title = "7. Cash Book (रोकड वही)",
                    subtitle = "Daily cash in hand, inflows (Receipts) vs outflows (Payments)",
                    metricHighlight = IndianAccountingFormat.formatIndianCurrency(uiState.cashBookReport.closingBalance),
                    badge = "Cash & Bank",
                    badgeColor = Emerald700,
                    testTag = "card_report_cash_book",
                    onClick = { showCashBookSheet = true }
                )

                // 8. Day Book
                ReportNavigationCard(
                    icon = Icons.Default.ReceiptLong,
                    iconColor = DeepNavyBlue,
                    title = "8. Day Book (रोजनामचा / रोजकिर्द)",
                    subtitle = "Daily primary accounting journal of all transactions",
                    metricHighlight = "${IndianAccountingFormat.formatIndianCurrency(uiState.daybook.daySalesTotal)} Day Sales",
                    badge = "Roznamcha",
                    badgeColor = DeepNavyBlue,
                    testTag = "card_report_daybook",
                    onClick = { showDaybookSheet = true }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Fast WhatsApp share
                Button(
                    onClick = {
                        ReportExporter.shareDailySalesReport(context, uiState.dailySales, uiState.isGstRegistered)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(48.dp)
                        .testTag("btn_share_daily_summary_whatsapp"),
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronOrange),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Share Today's Sales via WhatsApp",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

            } else {
                // TAB 1: Advanced Accounting & Tax Reports
                Text(
                    text = "Advanced Accounting & Tax Reports",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Emerald900,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // GSTR-1 & GSTR-3B Tax Reports
                ReportNavigationCard(
                    icon = Icons.Default.Description,
                    iconColor = Emerald800,
                    title = "GSTR-1 Tax Filing Summary",
                    subtitle = "B2B, B2C, 0-28% GST Slabs & HSN Summary",
                    metricHighlight = "${IndianAccountingFormat.formatIndianCurrency(uiState.gstr1.totalTax)} Tax",
                    badge = if (uiState.isGstRegistered) "CA Ready" else "Non-GST",
                    badgeColor = if (uiState.isGstRegistered) Color(0xFFD97706) else Color(0xFF64748B),
                    testTag = "card_report_gstr1",
                    onClick = { showGstrSheet = true }
                )

                ReportNavigationCard(
                    icon = Icons.Default.Assessment,
                    iconColor = Emerald700,
                    title = "GSTR-3B Tax Offset & ITC Return",
                    subtitle = "Output Tax vs. Input Tax Credit (ITC) = Net Tax",
                    metricHighlight = "${IndianAccountingFormat.formatIndianCurrency(uiState.gstr3b.totalNetGstPayable)} Cash Tax",
                    badge = if (uiState.isGstRegistered) "ITC Claim" else "Non-GST",
                    badgeColor = if (uiState.isGstRegistered) Color(0xFF1D4ED8) else Color(0xFF64748B),
                    testTag = "card_report_gstr3b",
                    onClick = { showGstr3bSheet = true }
                )

                // Profit & Loss
                ReportNavigationCard(
                    icon = Icons.Default.ShowChart,
                    iconColor = Color(0xFF16A34A),
                    title = "Profit & Loss (P&L) Statement",
                    subtitle = "Revenue, COGS, Gross Margin & Operating Expenses",
                    metricHighlight = "${IndianAccountingFormat.formatIndianCurrency(kotlin.math.abs(uiState.profitLoss.netProfit))} Net",
                    badge = if (uiState.profitLoss.netProfit >= 0) "Profit" else "Loss",
                    badgeColor = if (uiState.profitLoss.netProfit >= 0) Color(0xFF16A34A) else Color(0xFFDC2626),
                    testTag = "card_report_pl",
                    onClick = { showPlSheet = true }
                )

                // Trial Balance
                ReportNavigationCard(
                    icon = Icons.Default.AccountBalance,
                    iconColor = DeepNavyBlue,
                    title = "Trial Balance (कच्चा ताळेबंद)",
                    subtitle = "Double-entry Dr/Cr verification, assets, liabilities & equity",
                    metricHighlight = if (uiState.trialBalance.isBalanced) "Balanced (Dr=Cr)" else "Diff: ${IndianAccountingFormat.formatIndianCurrency(uiState.trialBalance.difference)}",
                    badge = if (uiState.trialBalance.isBalanced) "Balanced" else "Mismatch",
                    badgeColor = if (uiState.trialBalance.isBalanced) Emerald700 else Color(0xFFDC2626),
                    testTag = "card_report_trial_balance",
                    onClick = { showTrialBalanceSheet = true }
                )

                // Stock Valuation
                ReportNavigationCard(
                    icon = Icons.Default.Inventory2,
                    iconColor = Color(0xFF7C3AED),
                    title = "Stock Valuation & Inventory Health",
                    subtitle = "Capital locked in inventory, cost vs retail valuation",
                    metricHighlight = IndianAccountingFormat.formatIndianCurrency(uiState.stockValuation.totalSellingValue),
                    badge = if (uiState.stockValuation.lowStockCount > 0) "${uiState.stockValuation.lowStockCount} Low" else "Healthy",
                    badgeColor = if (uiState.stockValuation.lowStockCount > 0) Color(0xFFD97706) else Color(0xFF16A34A),
                    testTag = "card_report_stock_valuation",
                    onClick = { showStockSheet = true }
                )

                // Inward Purchases Register
                ReportNavigationCard(
                    icon = Icons.Default.ShoppingBag,
                    iconColor = Color(0xFF0D9488),
                    title = "Purchases Register & ITC",
                    subtitle = "Inward supplier bills, input tax credit & payables",
                    metricHighlight = IndianAccountingFormat.formatIndianCurrency(uiState.purchasesRegister.totalPurchasesValue),
                    badge = "${uiState.purchasesRegister.totalBillsCount} Inward",
                    badgeColor = Color(0xFF0D9488),
                    testTag = "card_report_purchases_register",
                    onClick = { showPurchasesRegisterSheet = true }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        ReportExporter.shareProfitLossReport(context, uiState.profitLoss, uiState.selectedPeriod)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(48.dp)
                        .testTag("btn_share_pl_whatsapp"),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald800),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Share P&L Statement (WhatsApp)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }

    // ==================== BOTTOM MODAL SHEETS ====================

    // 1. Daily Sales Sheet
    if (showDailySalesSheet) {
        DailySalesReportSheet(
            sheetState = dailySalesSheetState,
            report = uiState.dailySales,
            isGst = uiState.isGstRegistered,
            onPreviousDay = { viewModel.shiftDailySalesDate(-1) },
            onNextDay = { viewModel.shiftDailySalesDate(1) },
            onDismiss = { showDailySalesSheet = false }
        )
    }

    // 2. Sales by Date Range Sheet
    if (showSalesDateRangeSheet) {
        SalesDateRangeReportSheet(
            sheetState = salesDateRangeSheetState,
            report = uiState.salesDateRange,
            isGst = uiState.isGstRegistered,
            onSelectPeriod = { viewModel.setPeriod(it) },
            onDismiss = { showSalesDateRangeSheet = false }
        )
    }

    // 3. Outstanding Report Sheet
    if (showOutstandingSheet) {
        OutstandingReportSheet(
            sheetState = outstandingSheetState,
            report = uiState.outstanding,
            onDismiss = { showOutstandingSheet = false }
        )
    }

    // 4. Customer Ledger Sheet
    if (showCustomerLedgerSheet) {
        PartyLedgerReportSheet(
            sheetState = customerLedgerSheetState,
            report = uiState.customerLedger,
            parties = uiState.allCustomers,
            selectedPeriod = uiState.selectedPeriod,
            isSupplierMode = false,
            onSelectParty = { viewModel.setSelectedCustomerPartyId(it) },
            onSelectPeriod = { viewModel.setPeriod(it) },
            onDismiss = { showCustomerLedgerSheet = false }
        )
    }

    // 5. Supplier Ledger Sheet
    if (showSupplierLedgerSheet) {
        PartyLedgerReportSheet(
            sheetState = supplierLedgerSheetState,
            report = uiState.supplierLedger,
            parties = uiState.allSuppliers,
            selectedPeriod = uiState.selectedPeriod,
            isSupplierMode = true,
            onSelectParty = { viewModel.setSelectedSupplierPartyId(it) },
            onSelectPeriod = { viewModel.setPeriod(it) },
            onDismiss = { showSupplierLedgerSheet = false }
        )
    }

    // 6. Expense Report Sheet
    if (showExpenseSheet) {
        ExpenseReportSheet(
            sheetState = expenseSheetState,
            report = uiState.expenseReport,
            onSelectPeriod = { viewModel.setPeriod(it) },
            onDismiss = { showExpenseSheet = false }
        )
    }

    // 7. Cash Book Sheet
    if (showCashBookSheet) {
        CashBookReportSheet(
            sheetState = cashBookSheetState,
            report = uiState.cashBookReport,
            onToggleBankMode = { viewModel.setCashBookBankMode(it) },
            onSelectPeriod = { viewModel.setPeriod(it) },
            onDismiss = { showCashBookSheet = false }
        )
    }

    // 8. Day Book Sheet
    if (showDaybookSheet) {
        DaybookReportSheet(
            sheetState = daybookSheetState,
            daybook = uiState.daybook,
            onSelectDate = { viewModel.setDaybookDate(it) },
            onDismiss = { showDaybookSheet = false }
        )
    }

    // Advanced sheets
    if (showGstrSheet) {
        Gstr1ReportSheet(
            sheetState = gstrSheetState,
            gstr = uiState.gstr1,
            period = uiState.selectedPeriod,
            onDismiss = { showGstrSheet = false }
        )
    }

    if (showGstr3bSheet) {
        Gstr3bReportSheet(
            summary = uiState.gstr3b,
            periodLabel = uiState.selectedPeriod.label,
            period = uiState.selectedPeriod,
            sheetState = gstr3bSheetState,
            onDismiss = { showGstr3bSheet = false }
        )
    }

    if (showPlSheet) {
        ProfitLossReportSheet(
            sheetState = plSheetState,
            profitLoss = uiState.profitLoss,
            period = uiState.selectedPeriod,
            onDismiss = { showPlSheet = false }
        )
    }

    if (showStockSheet) {
        StockValuationSheet(
            sheetState = stockSheetState,
            stock = uiState.stockValuation,
            onDismiss = { showStockSheet = false }
        )
    }

    if (showPurchasesRegisterSheet) {
        PurchasesRegisterReportSheet(
            sheetState = purchasesRegisterSheetState,
            register = uiState.purchasesRegister,
            period = uiState.selectedPeriod,
            onDismiss = { showPurchasesRegisterSheet = false }
        )
    }

    if (showTrialBalanceSheet) {
        TrialBalanceReportSheet(
            sheetState = trialBalanceSheetState,
            trialBalance = uiState.trialBalance,
            period = uiState.selectedPeriod,
            onDismiss = { showTrialBalanceSheet = false }
        )
    }
}

@Composable
private fun ReportNavigationCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    metricHighlight: String,
    badge: String,
    badgeColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = iconColor.copy(alpha = 0.12f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Slate900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = badge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Slate600,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = metricHighlight,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = Emerald900
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Slate500,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
