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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShowChart
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
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hisabpro.app.ui.HisabViewModel
import com.hisabpro.app.ui.theme.DeepNavyBlue
import com.hisabpro.app.ui.theme.Emerald700
import com.hisabpro.app.ui.theme.Emerald800
import com.hisabpro.app.ui.theme.Emerald900
import com.hisabpro.app.ui.theme.PureWhite
import com.hisabpro.app.ui.theme.Slate100
import com.hisabpro.app.ui.theme.Slate200
import com.hisabpro.app.ui.theme.Slate500
import com.hisabpro.app.ui.theme.Slate600
import com.hisabpro.app.ui.theme.Slate700
import com.hisabpro.app.ui.theme.Slate800
import com.hisabpro.app.ui.theme.Slate900
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showGstrSheet by remember { mutableStateOf(false) }
    var showGstr3bSheet by remember { mutableStateOf(false) }
    var showPlSheet by remember { mutableStateOf(false) }
    var showDaybookSheet by remember { mutableStateOf(false) }
    var showAgingSheet by remember { mutableStateOf(false) }
    var showStockSheet by remember { mutableStateOf(false) }
    var showPurchasesRegisterSheet by remember { mutableStateOf(false) }
    var showTrialBalanceSheet by remember { mutableStateOf(false) }

    val gstrSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val gstr3bSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val plSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val daybookSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val agingSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
                        text = "Business Reports & GST",
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Text(
                        text = "Executive Financial Health & Tax Filing",
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
                            text = "Analytics",
                            color = PureWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            // Period Selector Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
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

            // Executive Financial Pulse Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Slate200),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                text = "₹${HisabViewModel.formatAmount(kotlin.math.abs(uiState.profitLoss.netProfit))}",
                                fontSize = 20.sp,
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
                                text = "₹${HisabViewModel.formatAmount(uiState.profitLoss.salesRevenue)}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Emerald900
                            )
                            if (uiState.isGstRegistered) {
                                Text(
                                    text = "Tax ₹${HisabViewModel.formatAmount(uiState.gstr1.totalTax)}",
                                    fontSize = 11.sp,
                                    color = Color(0xFFD97706),
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else {
                                Text(
                                    text = "Non-GST Business",
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
                            Text("You'll Receive (Dues)", fontSize = 10.sp, color = Color(0xFF64748B))
                            Text(
                                text = "₹${HisabViewModel.formatAmount(uiState.partyAging.totalReceivable)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF16A34A)
                            )
                        }

                        Column {
                            Text("You'll Pay (Suppliers)", fontSize = 10.sp, color = Color(0xFF64748B))
                            Text(
                                text = "₹${HisabViewModel.formatAmount(uiState.partyAging.totalPayable)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Inventory Stock Value", fontSize = 10.sp, color = Color(0xFF64748B))
                            Text(
                                text = "₹${HisabViewModel.formatAmount(uiState.stockValuation.totalSellingValue)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Emerald800
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Reports List Header
            Text(
                text = "Detailed Business Reports",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Emerald900,
                modifier = Modifier.padding(horizontal = 18.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 1. GSTR-1 Tax Filing Report Card (Only for GST Registered Businesses)
            if (uiState.isGstRegistered) {
                ReportNavigationCard(
                    icon = Icons.Default.Description,
                    iconColor = Emerald800,
                    title = "GSTR-1 Tax Filing Summary",
                    subtitle = "B2B, B2C, 0-28% GST Slabs & HSN Summary",
                    metricHighlight = "₹${HisabViewModel.formatAmount(uiState.gstr1.totalTax)} Tax",
                    badge = "CA Ready",
                    badgeColor = Color(0xFFD97706),
                    testTag = "card_report_gstr1",
                    onClick = { showGstrSheet = true }
                )

                // 1b. GSTR-3B Tax Offset & ITC Return Card
                ReportNavigationCard(
                    icon = Icons.Default.Assessment,
                    iconColor = Emerald700,
                    title = "GSTR-3B Tax Offset & ITC Return",
                    subtitle = "Output Tax vs. Input Tax Credit (ITC) = Net Tax",
                    metricHighlight = "₹${HisabViewModel.formatAmount(uiState.gstr3b.totalNetGstPayable)} Cash Tax",
                    badge = "ITC Claim",
                    badgeColor = Color(0xFF1D4ED8),
                    testTag = "card_report_gstr3b",
                    onClick = { showGstr3bSheet = true }
                )
            }

            // 2. Profit & Loss Report Card
            ReportNavigationCard(
                icon = Icons.Default.ShowChart,
                iconColor = Color(0xFF16A34A),
                title = "Profit & Loss (P&L) Statement",
                subtitle = "Revenue, COGS, Gross Margin & Operating Expenses",
                metricHighlight = "₹${HisabViewModel.formatAmount(kotlin.math.abs(uiState.profitLoss.netProfit))} Net",
                badge = if (uiState.profitLoss.netProfit >= 0) "Profit" else "Loss",
                badgeColor = if (uiState.profitLoss.netProfit >= 0) Color(0xFF16A34A) else Color(0xFFDC2626),
                testTag = "card_report_pl",
                onClick = { showPlSheet = true }
            )

            // 3. Daily Daybook Register Card
            ReportNavigationCard(
                icon = Icons.Default.CalendarMonth,
                iconColor = Color(0xFF0284C7),
                title = "Daily Daybook Register",
                subtitle = "Day's Billed Invoices, Cash In & Cash Out flow",
                metricHighlight = "₹${HisabViewModel.formatAmount(uiState.daybook.daySalesTotal)} Today",
                badge = "${uiState.daybook.daySalesCount} Bills",
                badgeColor = Color(0xFF0284C7),
                testTag = "card_report_daybook",
                onClick = { showDaybookSheet = true }
            )

            // 4. Party Aging & Overdue Card
            ReportNavigationCard(
                icon = Icons.Default.HourglassBottom,
                iconColor = Color(0xFFEA580C),
                title = "Party Outstanding & Aging Matrix",
                subtitle = "0-15d, 16-30d, 31-60d & 60+d overdue follow-ups",
                metricHighlight = "₹${HisabViewModel.formatAmount(uiState.partyAging.totalReceivable)} To Get",
                badge = "${uiState.partyAging.debtorList.size} Debtors",
                badgeColor = Color(0xFFEA580C),
                testTag = "card_report_party_aging",
                onClick = { showAgingSheet = true }
            )

            // 5. Stock Valuation & Margin Card
            ReportNavigationCard(
                icon = Icons.Default.Inventory2,
                iconColor = Color(0xFF7C3AED),
                title = "Stock Valuation & Health",
                subtitle = "Capital locked in inventory, cost vs retail margin",
                metricHighlight = "₹${HisabViewModel.formatAmount(uiState.stockValuation.totalSellingValue)}",
                badge = if (uiState.stockValuation.lowStockCount > 0) "${uiState.stockValuation.lowStockCount} Low" else "Healthy",
                badgeColor = if (uiState.stockValuation.lowStockCount > 0) Color(0xFFD97706) else Color(0xFF16A34A),
                testTag = "card_report_stock_valuation",
                onClick = { showStockSheet = true }
            )

            // 6. Inward Purchases Register & ITC Card
            ReportNavigationCard(
                icon = Icons.Default.ShoppingBag,
                iconColor = Color(0xFF0D9488),
                title = "Purchases Register & ITC",
                subtitle = "Inward supplier bills, input tax credit & payables",
                metricHighlight = "₹${HisabViewModel.formatAmount(uiState.purchasesRegister.totalPurchasesValue)} Value",
                badge = "${uiState.purchasesRegister.totalBillsCount} Inward",
                badgeColor = Color(0xFF0D9488),
                testTag = "card_report_purchases_register",
                onClick = { showPurchasesRegisterSheet = true }
            )

            // 7. Trial Balance (कच्चा ताळेबंद / तलपट)
            ReportNavigationCard(
                icon = Icons.Default.AccountBalance,
                iconColor = DeepNavyBlue,
                title = "Trial Balance (कच्चा ताळेबंद)",
                subtitle = "Double-entry Dr/Cr verification, assets, liabilities & equity",
                metricHighlight = if (uiState.trialBalance.isBalanced) "Balanced (Dr=Cr)" else "₹${HisabViewModel.formatAmount(uiState.trialBalance.difference)} Diff",
                badge = if (uiState.trialBalance.isBalanced) "Balanced" else "Mismatch",
                badgeColor = if (uiState.trialBalance.isBalanced) Emerald700 else Color(0xFFDC2626),
                testTag = "card_report_trial_balance",
                onClick = { showTrialBalanceSheet = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Fast Share All-in-One CA Report
            Button(
                onClick = {
                    ReportExporter.shareGstr1Report(context, uiState.gstr1, uiState.selectedPeriod)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(48.dp)
                    .testTag("btn_share_all_reports"),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald800),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Share Tax Report via WhatsApp",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }

    // Modal Sheets
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

    if (showDaybookSheet) {
        DaybookReportSheet(
            sheetState = daybookSheetState,
            daybook = uiState.daybook,
            onSelectDate = { viewModel.setDaybookDate(it) },
            onDismiss = { showDaybookSheet = false }
        )
    }

    if (showAgingSheet) {
        PartyAgingReportSheet(
            sheetState = agingSheetState,
            aging = uiState.partyAging,
            onDismiss = { showAgingSheet = false }
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
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF1E293B)
                    )

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = badgeColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = badge,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = metricHighlight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Emerald800
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
