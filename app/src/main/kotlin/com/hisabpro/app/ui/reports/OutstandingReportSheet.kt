package com.hisabpro.app.ui.reports

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisabpro.app.data.model.PartyWithBalance
import com.hisabpro.app.ui.theme.Emerald800
import com.hisabpro.app.ui.theme.Emerald900
import com.hisabpro.app.ui.theme.PureWhite
import com.hisabpro.app.ui.theme.SaffronOrange
import com.hisabpro.app.ui.theme.Slate100
import com.hisabpro.app.ui.theme.Slate200
import com.hisabpro.app.ui.theme.Slate500
import com.hisabpro.app.ui.theme.Slate600
import com.hisabpro.app.ui.theme.Slate800
import com.hisabpro.app.util.IndianAccountingFormat
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutstandingReportSheet(
    sheetState: SheetState,
    report: OutstandingReport,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0 = Customer Receivables, 1 = Supplier Payables
    var searchQuery by remember { mutableStateOf("") }

    val rawList = if (selectedTab == 0) report.customerList else report.supplierList
    val displayList = rawList.filter {
        searchQuery.isBlank() ||
                it.party.name.contains(searchQuery, ignoreCase = true) ||
                it.party.phone.contains(searchQuery)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PureWhite,
        modifier = Modifier.testTag("sheet_outstanding_report")
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
                        color = Color(0xFFEA580C).copy(alpha = 0.12f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.HourglassBottom,
                                contentDescription = null,
                                tint = Color(0xFFEA580C),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Outstanding & Dues Report",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )
                        Text(
                            text = "उधारी व येणे-देणे अहवाल",
                            fontSize = 12.sp,
                            color = Slate500
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("btn_close_outstanding")
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Slate500)
                }
            }

            // Dual Summary Card: Customers vs Suppliers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Receivable Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("You'll Receive (येणे)", fontSize = 11.sp, color = Color(0xFF15803D), fontWeight = FontWeight.Bold)
                        Text(
                            text = IndianAccountingFormat.formatIndianCurrency(report.totalReceivable),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF166534)
                        )
                        Text("${report.customerCount} Customers", fontSize = 10.sp, color = Slate500)
                    }
                }

                // Payable Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("You'll Pay (देणे)", fontSize = 11.sp, color = Color(0xFFB91C1C), fontWeight = FontWeight.Bold)
                        Text(
                            text = IndianAccountingFormat.formatIndianCurrency(report.totalPayable),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF991B1B)
                        )
                        Text("${report.supplierCount} Suppliers", fontSize = 10.sp, color = Slate500)
                    }
                }
            }

            // Aging Matrix for Receivables
            if (selectedTab == 0 && report.totalReceivable > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFEDD5))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("AGING BREAKDOWN (कधीपासून उधारी आहे?)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SaffronOrange)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("0-15 Days", fontSize = 10.sp, color = Slate500)
                                Text(IndianAccountingFormat.formatIndianCurrency(report.aging0to15), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                            }
                            Column {
                                Text("16-30 Days", fontSize = 10.sp, color = Slate500)
                                Text(IndianAccountingFormat.formatIndianCurrency(report.aging16to30), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                            }
                            Column {
                                Text("31-60 Days", fontSize = 10.sp, color = Slate500)
                                Text(IndianAccountingFormat.formatIndianCurrency(report.aging31to60), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEA580C))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("60+ Days", fontSize = 10.sp, color = Slate500)
                                Text(IndianAccountingFormat.formatIndianCurrency(report.aging60Plus), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                            }
                        }
                    }
                }
            }

            // Tab Selector: Customer vs Supplier
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Slate100,
                modifier = Modifier.fillMaxWidth(),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = if (selectedTab == 0) Emerald800 else Color(0xFFDC2626)
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "Customer Receivables (${report.customerList.size})",
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0) Emerald800 else Slate600
                        )
                    },
                    modifier = Modifier.testTag("tab_customer_receivables")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "Supplier Payables (${report.supplierList.size})",
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) Color(0xFFDC2626) else Slate600
                        )
                    },
                    modifier = Modifier.testTag("tab_supplier_payables")
                )
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_search_outstanding"),
                placeholder = { Text("Search by name or phone...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate500) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Emerald800,
                    unfocusedBorderColor = Slate200,
                    focusedContainerColor = PureWhite,
                    unfocusedContainerColor = PureWhite
                )
            )

            // Parties List
            if (displayList.isEmpty()) {
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
                        Text("🎉", fontSize = 28.sp)
                        Text(
                            text = if (selectedTab == 0) "No Pending Customer Dues" else "No Supplier Dues",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Slate800
                        )
                        Text(
                            text = if (selectedTab == 0) "All customer accounts are settled! There is no money left to receive." else "All supplier accounts are clear! There is no money left to pay.",
                            fontSize = 12.sp,
                            color = Slate500,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                displayList.forEach { pwb ->
                    val isReceivable = selectedTab == 0
                    val due = abs(pwb.netBalance)
                    val formattedDue = IndianAccountingFormat.formatIndianCurrency(due)

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
                                Text(
                                    text = pwb.party.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Slate800
                                )
                                if (pwb.party.phone.isNotBlank()) {
                                    Text(
                                        text = pwb.party.phone,
                                        fontSize = 12.sp,
                                        color = Slate500
                                    )
                                }
                                if (pwb.lastEntryDateMillis != null) {
                                    Text(
                                        text = "Last: ${IndianAccountingFormat.formatIndianDate(pwb.lastEntryDateMillis)}",
                                        fontSize = 11.sp,
                                        color = Slate500
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = formattedDue,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = if (isReceivable) Color(0xFF16A34A) else Color(0xFFDC2626)
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                if (isReceivable && pwb.party.phone.isNotBlank()) {
                                    // Direct WhatsApp reminder button
                                    OutlinedButton(
                                        onClick = {
                                            val message = "Namaskar ${pwb.party.name}, gentle reminder from our store regarding pending payment of $formattedDue. Please pay via UPI or cash. Dhanyawad!"
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                data = Uri.parse("https://api.whatsapp.com/send?phone=91${pwb.party.phone.takeLast(10)}&text=${Uri.encode(message)}")
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            try {
                                                context.startActivity(intent)
                                            } catch (_: Exception) {}
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFF16A34A))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Remind", fontSize = 10.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Share Summary Button
            Button(
                onClick = {
                    ReportExporter.shareOutstandingReport(context, report)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_share_outstanding"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Share Outstanding Summary (WhatsApp)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
