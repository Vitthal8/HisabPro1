package com.hisabpro.app.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisabpro.app.data.model.InvoiceStatus
import com.hisabpro.app.data.model.PurchaseBill
import com.hisabpro.app.ui.HisabViewModel
import com.hisabpro.app.ui.theme.Emerald700
import com.hisabpro.app.ui.theme.Emerald800
import com.hisabpro.app.ui.theme.Emerald900
import com.hisabpro.app.ui.theme.ExpenseRed
import com.hisabpro.app.ui.theme.IncomeGreen
import com.hisabpro.app.ui.theme.PureWhite
import com.hisabpro.app.ui.theme.Slate100
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchasesRegisterReportSheet(
    sheetState: SheetState,
    register: PurchasesRegisterSummary,
    period: ReportPeriod,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PureWhite,
        modifier = Modifier.testTag("sheet_purchases_register_report")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Emerald900)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(PureWhite.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Purchases Register & ITC",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = PureWhite
                        )
                        Text(
                            text = "Period: ${period.label} • ${register.totalBillsCount} Inward Bills",
                            fontSize = 12.sp,
                            color = PureWhite.copy(alpha = 0.8f)
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("btn_close_purchases_register_report")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = PureWhite
                    )
                }
            }

            // Body
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))

                    // Summary KPI Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Total Purchases", fontSize = 11.sp, color = Color(0xFF166534))
                                Text(
                                    text = "₹${HisabViewModel.formatAmount(register.totalPurchasesValue)}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF15803D)
                                )
                                Text(
                                    text = "${register.totalBillsCount} bills recorded",
                                    fontSize = 10.sp,
                                    color = Color(0xFF166534).copy(alpha = 0.8f)
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Input Tax Credit", fontSize = 11.sp, color = Color(0xFF1E40AF))
                                Text(
                                    text = "₹${HisabViewModel.formatAmount(register.itcAvailableTax)}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF1D4ED8)
                                )
                                Text(
                                    text = "Claimable in GSTR-3B",
                                    fontSize = 10.sp,
                                    color = Color(0xFF1E40AF).copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                item {
                    // Outstanding Payables status card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Supplier Dues (Payables)", fontSize = 11.sp, color = Color(0xFF991B1B))
                                Text(
                                    text = "₹${HisabViewModel.formatAmount(register.totalDueAmount)}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Total Paid", fontSize = 11.sp, color = Color(0xFF64748B))
                                Text(
                                    text = "₹${HisabViewModel.formatAmount(register.totalPaidAmount)}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = IncomeGreen
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Inward Bills (${register.purchases.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Emerald900
                    )
                }

                if (register.purchases.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingBag,
                                    contentDescription = null,
                                    tint = Emerald700.copy(alpha = 0.3f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No inward purchases in this period", color = Color(0xFF64748B))
                            }
                        }
                    }
                } else {
                    items(register.purchases, key = { it.id }) { bill ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = PureWhite),
                            shape = RoundedCornerShape(10.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = bill.supplierName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "${bill.purchaseNumber} • ${dateFormat.format(Date(bill.dateMillis))}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "₹${HisabViewModel.formatAmount(bill.grandTotal)}",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            color = Emerald900
                                        )
                                        Text(
                                            text = bill.paymentStatus.label,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (bill.paymentStatus) {
                                                InvoiceStatus.PAID -> IncomeGreen
                                                InvoiceStatus.PARTIAL -> Color(0xFFD97706)
                                                InvoiceStatus.UNPAID -> ExpenseRed
                                                InvoiceStatus.CANCELLED -> Color(0xFF64748B)
                                            }
                                        )
                                    }
                                }

                                if (bill.itcEligible && bill.totalTax > 0) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    HorizontalDivider(color = Slate100)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "ITC Claim: ₹${HisabViewModel.formatAmount(bill.totalTax)}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF1D4ED8),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (bill.dueAmount > 0) {
                                            Text(
                                                text = "Due: ₹${HisabViewModel.formatAmount(bill.dueAmount)}",
                                                fontSize = 11.sp,
                                                color = ExpenseRed,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // Bottom Actions: Share Report & Export CSV
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                color = PureWhite
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val uri = ReportExporter.exportPurchasesRegisterCsv(context, register.purchases)
                            if (uri != null) {
                                ReportExporter.shareCsvFile(context, uri, "Purchases Register (${period.label})")
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_export_purchases_register_csv"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export CSV", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            ReportExporter.sharePurchasesRegisterReport(context, register, period)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_share_purchases_register_report"),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald800),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Summary", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
