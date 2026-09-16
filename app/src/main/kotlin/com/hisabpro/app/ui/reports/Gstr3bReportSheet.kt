package com.hisabpro.app.ui.reports

import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
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
import com.hisabpro.app.ui.theme.Emerald700
import com.hisabpro.app.ui.theme.Emerald800
import com.hisabpro.app.ui.theme.ExpenseRed
import com.hisabpro.app.ui.theme.IncomeGreen
import com.hisabpro.app.ui.theme.PureWhite
import com.hisabpro.app.ui.theme.Slate100
import com.hisabpro.app.ui.theme.Slate200
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Gstr3bReportSheet(
    summary: Gstr3bSummary,
    periodLabel: String,
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                            .clip(CircleShape)
                            .background(Emerald700.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = Emerald700,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "GSTR-3B Monthly Return",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Output Tax vs. Input Tax Credit (ITC) • $periodLabel",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            HorizontalDivider(color = Slate200)

            // Net Tax Payable Highlight Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Emerald800.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "NET GST PAYABLE TO GOVT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Emerald800
                        )
                        Surface(
                            color = if (summary.totalNetGstPayable <= 0.01) IncomeGreen.copy(alpha = 0.15f) else ExpenseRed.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (summary.totalNetGstPayable <= 0.01) "ITC Surplus" else "Tax Due",
                                color = if (summary.totalNetGstPayable <= 0.01) IncomeGreen else ExpenseRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = "₹${String.format(Locale.ENGLISH, "%,.2f", summary.totalNetGstPayable)}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Emerald800
                    )

                    Text(
                        text = "Output Tax (₹${String.format(Locale.ENGLISH, "%,.0f", summary.totalOutputTax)}) minus Inward ITC (₹${String.format(Locale.ENGLISH, "%,.0f", summary.totalInputTaxCredit)})",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Section 1: Table 3.1 Outward Supplies (Sales)
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate100),
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
                        text = "TABLE 3.1: OUTWARD TAXABLE SUPPLIES (SALES)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Taxable Turnover:", fontSize = 13.sp)
                        Text("₹${String.format(Locale.ENGLISH, "%,.2f", summary.outwardTaxable)}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("CGST Output:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${String.format(Locale.ENGLISH, "%,.2f", summary.outwardCgst)}", fontSize = 13.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("SGST Output:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${String.format(Locale.ENGLISH, "%,.2f", summary.outwardSgst)}", fontSize = 13.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("IGST Output:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${String.format(Locale.ENGLISH, "%,.2f", summary.outwardIgst)}", fontSize = 13.sp)
                    }
                    HorizontalDivider(color = Slate200)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Output Tax Liability:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("₹${String.format(Locale.ENGLISH, "%,.2f", summary.totalOutputTax)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Emerald800)
                    }
                }
            }

            // Section 2: Table 4 Eligible Input Tax Credit (Purchases)
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate100),
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
                        text = "TABLE 4: ELIGIBLE INPUT TAX CREDIT (ITC - PURCHASES)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E40AF)
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Inward Taxable Purchases:", fontSize = 13.sp)
                        Text("₹${String.format(Locale.ENGLISH, "%,.2f", summary.inwardTaxable)}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("CGST ITC:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${String.format(Locale.ENGLISH, "%,.2f", summary.itcCgst)}", fontSize = 13.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("SGST ITC:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${String.format(Locale.ENGLISH, "%,.2f", summary.itcSgst)}", fontSize = 13.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("IGST ITC:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${String.format(Locale.ENGLISH, "%,.2f", summary.itcIgst)}", fontSize = 13.sp)
                    }
                    HorizontalDivider(color = Slate200)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Eligible ITC Claimed:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF))
                        Text("₹${String.format(Locale.ENGLISH, "%,.2f", summary.totalInputTaxCredit)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF))
                    }
                }
            }

            // Section 3: Tax Paid / Net Payable Split
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate100),
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
                        text = "TABLE 6.1: PAYMENT OF TAX (NET CASH PAYMENT)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Net Central Tax (CGST):", fontSize = 13.sp)
                        Text("₹${String.format(Locale.ENGLISH, "%,.2f", summary.netCgstPayable)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Net State Tax (SGST):", fontSize = 13.sp)
                        Text("₹${String.format(Locale.ENGLISH, "%,.2f", summary.netSgstPayable)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Net Integrated Tax (IGST):", fontSize = 13.sp)
                        Text("₹${String.format(Locale.ENGLISH, "%,.2f", summary.netIgstPayable)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    HorizontalDivider(color = Slate200)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Net Cash Tax Payable:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("₹${String.format(Locale.ENGLISH, "%,.2f", summary.totalNetGstPayable)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Emerald800)
                    }
                }
            }

            // Share with CA Button
            Button(
                onClick = {
                    shareGstr3bToWhatsApp(context, summary, periodLabel)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_share_gstr3b_ca"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = PureWhite, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share GSTR-3B Summary with CA", color = PureWhite, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

private fun shareGstr3bToWhatsApp(context: Context, s: Gstr3bSummary, period: String) {
    val text = buildString {
        appendLine("🏛️ *GSTR-3B MONTHLY FILING SUMMARY*")
        appendLine("Period: $period")
        appendLine("Generated by HisabPro")
        appendLine("--------------------------------")
        appendLine("*1. OUTWARD TAXABLE SUPPLIES (SALES):*")
        appendLine("• Taxable Turnover: ₹${String.format(Locale.ENGLISH, "%,.2f", s.outwardTaxable)}")
        appendLine("• CGST: ₹${String.format(Locale.ENGLISH, "%,.2f", s.outwardCgst)}")
        appendLine("• SGST: ₹${String.format(Locale.ENGLISH, "%,.2f", s.outwardSgst)}")
        appendLine("• IGST: ₹${String.format(Locale.ENGLISH, "%,.2f", s.outwardIgst)}")
        appendLine("• *Total Output Tax: ₹${String.format(Locale.ENGLISH, "%,.2f", s.totalOutputTax)}*")
        appendLine("--------------------------------")
        appendLine("*2. ELIGIBLE INPUT TAX CREDIT (ITC):*")
        appendLine("• Inward Purchases: ₹${String.format(Locale.ENGLISH, "%,.2f", s.inwardTaxable)}")
        appendLine("• CGST ITC: ₹${String.format(Locale.ENGLISH, "%,.2f", s.itcCgst)}")
        appendLine("• SGST ITC: ₹${String.format(Locale.ENGLISH, "%,.2f", s.itcSgst)}")
        appendLine("• IGST ITC: ₹${String.format(Locale.ENGLISH, "%,.2f", s.itcIgst)}")
        appendLine("• *Total ITC Claimed: ₹${String.format(Locale.ENGLISH, "%,.2f", s.totalInputTaxCredit)}*")
        appendLine("--------------------------------")
        appendLine("*3. NET TAX PAYABLE TO GOVERNMENT (CASH):*")
        appendLine("• Net CGST: ₹${String.format(Locale.ENGLISH, "%,.2f", s.netCgstPayable)}")
        appendLine("• Net SGST: ₹${String.format(Locale.ENGLISH, "%,.2f", s.netSgstPayable)}")
        appendLine("• Net IGST: ₹${String.format(Locale.ENGLISH, "%,.2f", s.netIgstPayable)}")
        appendLine("👉 *TOTAL CASH PAYABLE: ₹${String.format(Locale.ENGLISH, "%,.2f", s.totalNetGstPayable)}*")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share GSTR-3B Summary"))
}
