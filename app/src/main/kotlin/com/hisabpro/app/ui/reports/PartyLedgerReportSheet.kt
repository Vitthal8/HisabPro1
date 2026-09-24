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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
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
import com.hisabpro.app.data.model.Party
import com.hisabpro.app.data.model.PartyType
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
fun PartyLedgerReportSheet(
    sheetState: SheetState,
    report: PartyLedgerReport,
    parties: List<Party>,
    selectedPeriod: ReportPeriod,
    isSupplierMode: Boolean,
    onSelectParty: (String) -> Unit,
    onSelectPeriod: (ReportPeriod) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val title = if (isSupplierMode) "Supplier Ledger (व्यापारी खातेवही)" else "Customer Ledger (ग्राहक खातेवही)"
    val themeColor = if (isSupplierMode) Color(0xFF0D9488) else Emerald800

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PureWhite,
        modifier = Modifier.testTag("sheet_party_ledger_report")
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
                                imageVector = Icons.Default.MenuBook,
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
                            text = "Double-Entry Dr/Cr Statement (${selectedPeriod.label})",
                            fontSize = 12.sp,
                            color = Slate500
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("btn_close_ledger")
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Slate500)
                }
            }

            // Party Selector Chips
            if (parties.isNotEmpty()) {
                Text(
                    text = if (isSupplierMode) "Select Supplier:" else "Select Customer:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate600
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    parties.forEach { p ->
                        val isSelected = report.party?.id == p.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectParty(p.id) },
                            label = { Text(p.name, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = themeColor,
                                selectedLabelColor = PureWhite,
                                containerColor = Slate100,
                                labelColor = Slate800
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.testTag("chip_party_${p.id}")
                        )
                    }
                }
            }

            // Period Selector Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReportPeriod.entries.forEach { p ->
                    val isSelected = selectedPeriod == p
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
                        modifier = Modifier.testTag("chip_ledger_period_${p.name}")
                    )
                }
            }

            // Party Overview Card
            val partyName = report.party?.name ?: "No Party Selected"
            val partyPhone = report.party?.phone ?: ""

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = partyName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate800
                            )
                            if (partyPhone.isNotBlank()) {
                                Text(text = partyPhone, fontSize = 12.sp, color = Slate500)
                            }
                        }

                        // Closing Balance Badge
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (report.closingBalanceType == "Dr") Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                        ) {
                            Text(
                                text = "Closing: ${IndianAccountingFormat.formatIndianCurrency(report.closingBalance)} ${report.closingBalanceType}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (report.closingBalanceType == "Dr") Color(0xFF16A34A) else Color(0xFFDC2626),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = Slate200)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Opening Balance", fontSize = 10.sp, color = Slate500)
                            Text(
                                text = "${IndianAccountingFormat.formatIndianCurrency(report.openingBalance)} ${report.openingBalanceType}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate800
                            )
                        }
                        Column {
                            Text("Total Debit (Dr)", fontSize = 10.sp, color = Slate500)
                            Text(
                                text = IndianAccountingFormat.formatIndianCurrency(report.totalDebit),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF16A34A)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Credit (Cr)", fontSize = 10.sp, color = Slate500)
                            Text(
                                text = IndianAccountingFormat.formatIndianCurrency(report.totalCredit),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2563EB)
                            )
                        }
                    }
                }
            }

            // Ledger Entries Header
            Text(
                text = "Ledger Transactions (${report.entries.size})",
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
                            text = "No Transactions Found",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Slate800
                        )
                        Text(
                            text = "There are no bills, payments or entries for $partyName in this period.",
                            fontSize = 12.sp,
                            color = Slate500,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                // Table-like cards
                report.entries.forEach { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = IndianAccountingFormat.formatIndianDate(item.dateMillis),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate800
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Slate100
                                    ) {
                                        Text(
                                            text = item.voucherType,
                                            fontSize = 10.sp,
                                            color = Slate600,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "Bal: ${IndianAccountingFormat.formatIndianCurrency(item.runningBalance)} ${item.balanceType}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate800
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.narration,
                                    fontSize = 12.sp,
                                    color = Slate500,
                                    modifier = Modifier.weight(1f)
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (item.debitAmount > 0) {
                                        Text(
                                            text = "Dr ${IndianAccountingFormat.formatIndianCurrency(item.debitAmount)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFF16A34A)
                                        )
                                    }
                                    if (item.creditAmount > 0) {
                                        Text(
                                            text = "Cr ${IndianAccountingFormat.formatIndianCurrency(item.creditAmount)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFF2563EB)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Share Statement Button
            Button(
                onClick = {
                    ReportExporter.sharePartyLedgerReport(context, report)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_share_party_ledger"),
                colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Share Ledger Statement (WhatsApp)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
