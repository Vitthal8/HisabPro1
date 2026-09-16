package com.hisabpro.app.ui.reports

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisabpro.app.ui.HisabViewModel
import com.hisabpro.app.ui.theme.Emerald700
import com.hisabpro.app.ui.theme.Emerald800
import com.hisabpro.app.ui.theme.Emerald900
import com.hisabpro.app.ui.theme.PureWhite
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfitLossReportSheet(
    sheetState: SheetState,
    profitLoss: ProfitLossSummary,
    period: ReportPeriod,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isProfit = profitLoss.netProfit >= 0
    val netStatusColor = if (isProfit) Color(0xFF16A34A) else Color(0xFFDC2626)
    val netStatusBg = if (isProfit) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PureWhite,
        modifier = Modifier.testTag("sheet_pl_report")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Emerald800.copy(alpha = 0.12f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ShowChart,
                                contentDescription = null,
                                tint = Emerald800,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Profit & Loss (P&L)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Emerald900
                        )
                        Text(
                            text = "Period: ${period.label} • Real-time Business Health",
                            fontSize = 12.sp,
                            color = Color(0xFF667085)
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Close",
                        tint = Color(0xFF667085)
                    )
                }
            }

            // Share Action
            Button(
                onClick = {
                    ReportExporter.shareProfitLossReport(context, profitLoss, period)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("btn_share_pl"),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald800),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share P&L Statement", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            // Net Profit Banner Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = netStatusBg)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isProfit) "NET BUSINESS PROFIT" else "NET BUSINESS LOSS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = netStatusColor,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "₹${HisabViewModel.formatAmount(kotlin.math.abs(profitLoss.netProfit))}",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = netStatusColor
                    )
                    Text(
                        text = "Net Margin: ${String.format(Locale.ENGLISH, "%.1f", profitLoss.netMarginPercent)}% • Based on Invoiced Revenue & Expenses",
                        fontSize = 12.sp,
                        color = netStatusColor.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Financial Breakdown Waterfall
            Text(
                text = "Revenue & Expense Breakdown",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Emerald900
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Revenue
                    PlRow(
                        label = "Gross Sales Revenue",
                        subtext = "Total taxable invoiced turnover",
                        amount = profitLoss.salesRevenue,
                        isPositive = true,
                        isBold = true
                    )

                    // COGS
                    PlRow(
                        label = "Cost of Goods Sold (COGS)",
                        subtext = "Inventory purchase cost of sold items",
                        amount = -profitLoss.costOfGoodsSold,
                        isPositive = false
                    )

                    HorizontalDivider(color = Color(0xFFE4E7EC))

                    // Gross Profit
                    PlRow(
                        label = "Gross Profit",
                        subtext = "${String.format(Locale.ENGLISH, "%.1f", profitLoss.grossMarginPercent)}% Gross Margin",
                        amount = profitLoss.grossProfit,
                        isPositive = profitLoss.grossProfit >= 0,
                        isBold = true
                    )

                    HorizontalDivider(color = Color(0xFFE4E7EC))

                    // Operating Expenses
                    PlRow(
                        label = "Operating Expenses (Cashbook)",
                        subtext = "Rent, staff wages, utilities, tea & bills",
                        amount = -profitLoss.totalExpenses,
                        isPositive = false,
                        isBold = true
                    )

                    // Breakdown of expenses
                    if (profitLoss.expenseByCategory.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, top = 4.dp, bottom = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            profitLoss.expenseByCategory.forEach { (category, amt) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("• $category", fontSize = 12.sp, color = Color(0xFF64748B))
                                    Text("₹${HisabViewModel.formatAmount(amt)}", fontSize = 12.sp, color = Color(0xFF64748B))
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFCBD5E1), thickness = 1.5.dp)

                    // Final Net Profit
                    PlRow(
                        label = "Net Business Profit / (Loss)",
                        subtext = "After all inventory costs & operating expenses",
                        amount = profitLoss.netProfit,
                        isPositive = isProfit,
                        isBold = true,
                        fontSize = 16
                    )
                }
            }

            // Cashflow Status
            Text(
                text = "Cashflow Reality Check",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Emerald900
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Cash Inflow", fontSize = 11.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "₹${HisabViewModel.formatAmount(profitLoss.cashInflow)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16A34A)
                        )
                        Text("Paid bills + Cash in", fontSize = 10.sp, color = Color(0xFF64748B))
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Cash Outflow", fontSize = 11.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "₹${HisabViewModel.formatAmount(profitLoss.cashOutflow)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626)
                        )
                        Text("Expenses paid", fontSize = 10.sp, color = Color(0xFF64748B))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PlRow(
    label: String,
    subtext: String,
    amount: Double,
    isPositive: Boolean,
    isBold: Boolean = false,
    fontSize: Int = 13
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
                fontSize = fontSize.sp,
                color = Color(0xFF1E293B)
            )
            if (subtext.isNotBlank()) {
                Text(
                    text = subtext,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }
        }
        val prefix = if (amount < 0) "(-) ₹" else if (isPositive && amount > 0) "(+) ₹" else "₹"
        val displayAmt = HisabViewModel.formatAmount(kotlin.math.abs(amount))
        Text(
            text = "$prefix$displayAmt",
            fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.SemiBold,
            fontSize = fontSize.sp,
            color = if (isPositive) Color(0xFF16A34A) else Color(0xFFDC2626)
        )
    }
}
