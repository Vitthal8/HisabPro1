package com.hisabpro.app.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisabpro.app.ui.theme.Emerald800
import com.hisabpro.app.ui.theme.Emerald900
import com.hisabpro.app.ui.theme.PureWhite
import com.hisabpro.app.ui.theme.Slate100
import com.hisabpro.app.ui.theme.Slate200
import com.hisabpro.app.ui.theme.Slate500
import com.hisabpro.app.ui.theme.Slate600
import com.hisabpro.app.ui.theme.Slate800
import com.hisabpro.app.util.IndianAccountingFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashBookReportSheet(
    sheetState: SheetState,
    report: CashBookReportSummary,
    onToggleBankMode: (Boolean) -> Unit,
    onSelectPeriod: (ReportPeriod) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val title = if (report.isBankMode) "Bank & UPI Book (बँक वही)" else "Cash Book (रोकड वही)"
    val themeColor = if (report.isBankMode) Color(0xFF2563EB) else Emerald800

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PureWhite,
        modifier = Modifier.testTag("sheet_cash_book_report")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = themeColor.copy(alpha = 0.12f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (report.isBankMode) Icons.Default.AccountBalance else Icons.Default.Payments,
                                contentDescription = null,
                                tint = themeColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )
                        Text(
                            text = "Daily Inflow & Outflow Movement (${report.period.label})",
                            fontSize = 12.sp,
                            color = Slate500
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("btn_close_cash_book")
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Slate500)
                }
            }

            // Cash vs Bank Tab Selector
            TabRow(
                selectedTabIndex = if (report.isBankMode) 1 else 0,
                containerColor = Slate100,
                modifier = Modifier.fillMaxWidth(),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[if (report.isBankMode) 1 else 0]),
                        color = themeColor
                    )
                }
            ) {
                Tab(
                    selected = !report.isBankMode,
                    onClick = { onToggleBankMode(false) },
                    text = {
                        Text(
                            text = "Cash Book (रोकड)",
                            fontSize = 12.sp,
                            fontWeight = if (!report.isBankMode) FontWeight.Bold else FontWeight.Normal,
                            color = if (!report.isBankMode) Emerald800 else Slate600
                        )
                    },
                    modifier = Modifier.testTag("tab_cash_book")
                )
                Tab(
                    selected = report.isBankMode,
                    onClick = { onToggleBankMode(true) },
                    text = {
                        Text(
                            text = "Bank & UPI Book (बँक)",
                            fontSize = 12.sp,
                            fontWeight = if (report.isBankMode) FontWeight.Bold else FontWeight.Normal,
                            color = if (report.isBankMode) Color(0xFF2563EB) else Slate600
                        )
                    },
                    modifier = Modifier.testTag("tab_bank_book")
                )
            }

            // Period Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReportPeriod.entries.forEach { p ->
                    val isSelected = report.period == p
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectPeriod(p) },
                        label = { Text(p.label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = themeColor,
                            selectedLabelColor = PureWhite,
                            containerColor = Slate100,
                            labelColor = Slate800
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("chip_cash_period_${p.name}")
                    )
                }
            }

            // Balance Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = if (report.isBankMode) Color(0xFFEFF6FF) else Color(0xFFF0FDF4)),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (report.isBankMode) Color(0xFFBFDBFE) else Color(0xFFBBF7D0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (report.isBankMode) "NET BANK & UPI INFLOW (${report.period.label})" else "NET CASH IN HAND MOVEMENT (${report.period.label})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColor,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = IndianAccountingFormat.formatIndianCurrency(report.closingBalance),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (report.closingBalance >= 0) (if (report.isBankMode) Color(0xFF1D4ED8) else Emerald900) else Color(0xFFDC2626)
                    )

                    HorizontalDivider(color = if (report.isBankMode) Color(0xFFDBEAFE) else Color(0xFFDCFCE7))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Inflow (Receipts)", fontSize = 11.sp, color = Slate500)
                            Text(
                                text = "(+) ${IndianAccountingFormat.formatIndianCurrency(report.totalInflow)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF16A34A)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Outflow (Payments)", fontSize = 11.sp, color = Slate500)
                            Text(
                                text = "(-) ${IndianAccountingFormat.formatIndianCurrency(report.totalOutflow)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }
                }
            }

            // Entries List Header
            Text(
                text = "Cash Movement Vouchers (${report.entries.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Emerald900
            )

            if (report.entries.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = Slate500,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "No Movement Recorded",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Slate800
                        )
                        Text(
                            text = "No transactions found in ${report.period.label} for ${if (report.isBankMode) "Bank/UPI" else "Cash"}.",
                            fontSize = 12.sp,
                            color = Slate500
                        )
                    }
                }
            } else {
                report.entries.forEach { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = IndianAccountingFormat.formatIndianDate(entry.dateMillis),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate800
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Slate100
                                    ) {
                                        Text(
                                            text = entry.mode,
                                            fontSize = 10.sp,
                                            color = Slate600,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = entry.particulars,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Slate800
                                )
                                Text(
                                    text = "Bal: ${IndianAccountingFormat.formatIndianCurrency(entry.runningBalance)}",
                                    fontSize = 11.sp,
                                    color = Slate500
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                if (entry.inAmount > 0) {
                                    Text(
                                        text = "(+) ${IndianAccountingFormat.formatIndianCurrency(entry.inAmount)}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF16A34A)
                                    )
                                } else {
                                    Text(
                                        text = "(-) ${IndianAccountingFormat.formatIndianCurrency(entry.outAmount)}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = Color(0xFFDC2626)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Share Button
            Button(
                onClick = {
                    ReportExporter.shareCashBookReport(context, report)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_share_cash_book"),
                colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Share $title (WhatsApp)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
