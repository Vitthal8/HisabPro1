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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisabpro.app.data.model.InvoiceStatus
import com.hisabpro.app.ui.theme.Emerald800
import com.hisabpro.app.ui.theme.Emerald900
import com.hisabpro.app.ui.theme.PureWhite
import com.hisabpro.app.ui.theme.SaffronOrange
import com.hisabpro.app.ui.theme.Slate200
import com.hisabpro.app.ui.theme.Slate500
import com.hisabpro.app.ui.theme.Slate600
import com.hisabpro.app.ui.theme.Slate800
import com.hisabpro.app.util.IndianAccountingFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailySalesReportSheet(
    sheetState: SheetState,
    report: DailySalesReport,
    isGst: Boolean,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
    val dateDisplay = IndianAccountingFormat.formatIndianDate(report.dateMillis)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PureWhite,
        modifier = Modifier.testTag("sheet_daily_sales_report")
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
                        color = SaffronOrange.copy(alpha = 0.12f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PointOfSale,
                                contentDescription = null,
                                tint = SaffronOrange,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Daily Sales Report",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )
                        Text(
                            text = "आजची दैनिक विक्री नोंद",
                            fontSize = 12.sp,
                            color = Slate500
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("btn_close_daily_sales")
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Slate500)
                }
            }

            // Date Navigator Bar
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onPreviousDay,
                        modifier = Modifier.testTag("btn_prev_day")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Day",
                            tint = Emerald800
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = SaffronOrange,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = dateDisplay,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Slate800
                        )
                    }

                    IconButton(
                        onClick = onNextDay,
                        modifier = Modifier.testTag("btn_next_day")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Day",
                            tint = Emerald800
                        )
                    }
                }
            }

            // Overview Total Sales Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFEDD5))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "TOTAL DAILY SALES TURNOVER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SaffronOrange,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = IndianAccountingFormat.formatIndianCurrency(report.totalSales),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF9A3412)
                    )
                    Text(
                        text = "${report.invoiceCount} Bills generated on this day",
                        fontSize = 12.sp,
                        color = Slate600
                    )

                    HorizontalDivider(color = Color(0xFFFED7AA))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Cash Sales", fontSize = 11.sp, color = Slate500)
                            Text(
                                text = IndianAccountingFormat.formatIndianCurrency(report.cashSales),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF16A34A)
                            )
                        }
                        Column {
                            Text("UPI / Online", fontSize = 11.sp, color = Slate500)
                            Text(
                                text = IndianAccountingFormat.formatIndianCurrency(report.upiSales),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2563EB)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Credit (Udhar)", fontSize = 11.sp, color = Slate500)
                            Text(
                                text = IndianAccountingFormat.formatIndianCurrency(report.creditSales),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }

                    if (isGst && report.totalTax > 0) {
                        HorizontalDivider(color = Color(0xFFFED7AA))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Taxable: ${IndianAccountingFormat.formatIndianCurrency(report.taxableAmount)}", fontSize = 11.sp, color = Slate600)
                            Text("GST Tax: ${IndianAccountingFormat.formatIndianCurrency(report.totalTax)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFD97706))
                        }
                    }
                }
            }

            // Invoices List Header
            Text(
                text = "Day's Invoices (${report.invoiceCount})",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Emerald900
            )

            if (report.invoices.isEmpty()) {
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
                            text = "No Sales Recorded For This Day",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Slate800
                        )
                        Text(
                            text = "No invoices were billed on $dateDisplay. Tap '+ Add Sale' on the dashboard to bill a sale.",
                            fontSize = 12.sp,
                            color = Slate500,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                report.invoices.forEach { inv ->
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
                                        text = "#${inv.invoiceNumber}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Emerald900
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = timeFormat.format(Date(inv.dateMillis)),
                                        fontSize = 11.sp,
                                        color = Slate500
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = inv.customerName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Slate800
                                )
                                Text(
                                    text = "${inv.items.size} items",
                                    fontSize = 11.sp,
                                    color = Slate500
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = IndianAccountingFormat.formatIndianCurrency(inv.grandTotal),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = Slate800
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                val isPaid = inv.paymentStatus == InvoiceStatus.PAID
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isPaid) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                                ) {
                                    Text(
                                        text = inv.paymentStatus.label,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPaid) Color(0xFF16A34A) else Color(0xFFDC2626),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
                    ReportExporter.shareDailySalesReport(context, report, isGst)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_share_daily_sales"),
                colors = ButtonDefaults.buttonColors(containerColor = SaffronOrange),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Share Daily Sales Summary (WhatsApp)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
