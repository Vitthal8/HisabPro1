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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hisabpro.app.data.repository.SettingsRepository
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisabpro.app.ui.HisabViewModel
import com.hisabpro.app.ui.theme.Emerald700
import com.hisabpro.app.ui.theme.Emerald800
import com.hisabpro.app.ui.theme.Emerald900
import com.hisabpro.app.ui.theme.DeepNavyBlue
import com.hisabpro.app.ui.theme.PureWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Gstr1ReportSheet(
    sheetState: SheetState,
    gstr: Gstr1Summary,
    period: ReportPeriod,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val settingsRepo = remember { SettingsRepository.getInstance(context) }
    val businessProfile by settingsRepo.profile.collectAsStateWithLifecycle()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PureWhite,
        modifier = Modifier.testTag("sheet_gstr1_report")
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
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Emerald800.copy(alpha = 0.12f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = Emerald800,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "GSTR-1 Tax Summary",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Emerald900
                            )
                            Text(
                                text = "${businessProfile.shopName.ifBlank { "HisabPro Store" }} • ${period.label}",
                                fontSize = 12.sp,
                                color = Color(0xFF667085)
                            )
                        }
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

            if (!businessProfile.isGstRegistered) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "GST Reporting Inactive",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFFB78103)
                        )
                        Text(
                            text = "This business operates in Non-GST mode (gst_enabled = false). All sales are Non-GST / Exempt. No GST returns are required.",
                            fontSize = 12.sp,
                            color = Color(0xFF5D4037)
                        )
                    }
                }
            }

            // Quick Actions: Share with CA, Export CSV & Govt Portal JSON
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        ReportExporter.shareGstr1Report(
                            context = context,
                            gstr = gstr,
                            period = period,
                            businessName = businessProfile.shopName.ifBlank { "HisabPro Store" },
                            gstin = businessProfile.gstin
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("btn_share_gstr_ca"),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald800),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        val uri = ReportExporter.exportGstr1Csv(
                            context = context,
                            gstr = gstr,
                            period = period,
                            businessName = businessProfile.shopName.ifBlank { "HisabPro Store" },
                            gstin = businessProfile.gstin
                        )
                        if (uri != null) {
                            ReportExporter.shareCsvFile(context, uri, "GSTR-1 Report CSV")
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("btn_export_gstr_csv"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Emerald800
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("CSV", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Emerald800)
                }

                Button(
                    onClick = {
                        val uri = ReportExporter.exportGstr1GovtJson(
                            context = context,
                            gstr = gstr,
                            period = period,
                            businessName = businessProfile.shopName.ifBlank { "HisabPro Store" },
                            gstin = businessProfile.gstin
                        )
                        if (uri != null) {
                            ReportExporter.shareJsonFile(context, uri, "Govt GSTR-1 Portal JSON")
                        }
                    },
                    modifier = Modifier
                        .weight(1.3f)
                        .height(44.dp)
                        .testTag("btn_export_gstr_json"),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepNavyBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = PureWhite
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Portal JSON", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                }
            }

            // Overview Total Cards
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Emerald800.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "TOTAL TURNOVER & TAX COLLECTED",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Emerald800,
                        letterSpacing = 0.5.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Taxable Supplies", fontSize = 12.sp, color = Color(0xFF667085))
                            Text(
                                text = "₹${HisabViewModel.formatAmount(gstr.totalTaxableSupplies)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Emerald900
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total GST Tax", fontSize = 12.sp, color = Color(0xFF667085))
                            Text(
                                text = "₹${HisabViewModel.formatAmount(gstr.totalTax)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFD97706)
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFFE4E7EC), thickness = 1.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TaxComponentColumn(label = "CGST", amount = gstr.totalCgst)
                        TaxComponentColumn(label = "SGST", amount = gstr.totalSgst)
                        TaxComponentColumn(label = "IGST", amount = gstr.totalIgst)
                        TaxComponentColumn(label = "Total Value", amount = gstr.totalInvoiceValue, isHighlight = true)
                    }
                }
            }

            // B2B vs B2C Split
            Text(
                text = "B2B vs B2C Invoice Split",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Emerald900
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("B2B Invoices", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1E293B))
                        Text("With Registered GSTIN", fontSize = 10.sp, color = Color(0xFF64748B))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${gstr.b2bCount} Bills", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Emerald800)
                        Text("Taxable: ₹${HisabViewModel.formatAmount(gstr.b2bTaxable)}", fontSize = 11.sp, color = Color(0xFF475569))
                        Text("Tax: ₹${HisabViewModel.formatAmount(gstr.b2bTax)}", fontSize = 11.sp, color = Color(0xFFD97706), fontWeight = FontWeight.SemiBold)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("B2C Invoices", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1E293B))
                        Text("Retail Consumers", fontSize = 10.sp, color = Color(0xFF64748B))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${gstr.b2cCount} Bills", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Emerald800)
                        Text("Taxable: ₹${HisabViewModel.formatAmount(gstr.b2cTaxable)}", fontSize = 11.sp, color = Color(0xFF475569))
                        Text("Tax: ₹${HisabViewModel.formatAmount(gstr.b2cTax)}", fontSize = 11.sp, color = Color(0xFFD97706), fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // GST Rate Wise Slabs
            Text(
                text = "GST Slab Breakdown",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Emerald900
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Slab", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF667085), modifier = Modifier.weight(1f))
                        Text("Taxable (₹)", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF667085), modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
                        Text("CGST (₹)", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF667085), modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                        Text("SGST (₹)", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF667085), modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                        Text("Tax (₹)", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF667085), modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
                    }

                    HorizontalDivider(color = Color(0xFFE4E7EC))

                    gstr.slabSummaries.forEach { slab ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${slab.gstRate.toInt()}%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Emerald800,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = HisabViewModel.formatAmount(slab.taxableAmount),
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1.5f),
                                textAlign = TextAlign.End
                            )
                            Text(
                                text = HisabViewModel.formatAmount(slab.cgstAmount),
                                fontSize = 11.sp,
                                color = Color(0xFF475569),
                                modifier = Modifier.weight(1.2f),
                                textAlign = TextAlign.End
                            )
                            Text(
                                text = HisabViewModel.formatAmount(slab.sgstAmount),
                                fontSize = 11.sp,
                                color = Color(0xFF475569),
                                modifier = Modifier.weight(1.2f),
                                textAlign = TextAlign.End
                            )
                            Text(
                                text = HisabViewModel.formatAmount(slab.totalTax),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = Color(0xFFD97706),
                                modifier = Modifier.weight(1.5f),
                                textAlign = TextAlign.End
                            )
                        }
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                    }
                }
            }

            // HSN Summary
            if (gstr.hsnSummaries.isNotEmpty()) {
                Text(
                    text = "HSN / SAC Code Summary (${gstr.hsnSummaries.size})",
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
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        gstr.hsnSummaries.take(6).forEach { hsn ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${hsn.hsnCode} • ${hsn.description}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = "Qty: ${hsn.totalQuantity.toInt()} ${hsn.unit} • ${hsn.gstRate.toInt()}% GST",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "₹${HisabViewModel.formatAmount(hsn.taxableAmount)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "Tax: ₹${HisabViewModel.formatAmount(hsn.totalTax)}",
                                        fontSize = 10.sp,
                                        color = Color(0xFFD97706)
                                    )
                                }
                            }
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TaxComponentColumn(
    label: String,
    amount: Double,
    isHighlight: Boolean = false
) {
    Column {
        Text(label, fontSize = 11.sp, color = Color(0xFF667085))
        Text(
            text = "₹${HisabViewModel.formatAmount(amount)}",
            fontSize = 12.sp,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Medium,
            color = if (isHighlight) Emerald900 else Color(0xFF334155)
        )
    }
}
