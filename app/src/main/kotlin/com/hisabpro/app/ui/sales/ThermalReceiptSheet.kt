package com.hisabpro.app.ui.sales

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisabpro.app.data.model.BusinessProfile
import com.hisabpro.app.data.model.Invoice
import com.hisabpro.app.ui.reports.ReportExporter
import com.hisabpro.app.ui.theme.DeepNavyBlue
import com.hisabpro.app.ui.theme.Emerald800
import com.hisabpro.app.ui.theme.PureWhite
import com.hisabpro.app.ui.theme.SaffronOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThermalReceiptSheet(
    sheetState: SheetState,
    invoice: Invoice,
    businessProfile: BusinessProfile,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var selectedWidthTab by remember { mutableIntStateOf(0) } // 0: 58mm (32 char), 1: 80mm (48 char)

    val widthChars = if (selectedWidthTab == 0) 32 else 48
    val thermalText = remember(invoice, businessProfile, widthChars) {
        ReportExporter.generateThermalReceiptText(invoice, businessProfile, widthChars)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("sheet_thermal_receipt")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SaffronOrange.copy(alpha = 0.15f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = null,
                                tint = SaffronOrange,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "POS Thermal Receipt",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Bluetooth & USB Mini Printer Format",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Width Selector: 58mm vs 80mm
            TabRow(
                selectedTabIndex = selectedWidthTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth().border(0.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
            ) {
                Tab(
                    selected = selectedWidthTab == 0,
                    onClick = { selectedWidthTab = 0 },
                    text = { Text("58mm Standard (2 Inch)", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedWidthTab == 1,
                    onClick = { selectedWidthTab = 1 },
                    text = { Text("80mm Wide (3 Inch)", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Thermal Paper Preview Box
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)), // Off-white thermal paper
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = thermalText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = Color(0xFF1E293B),
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(thermalText))
                        Toast.makeText(context, "Thermal text copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Text", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, thermalText)
                            putExtra(android.content.Intent.EXTRA_SUBJECT, "Thermal Bill #${invoice.invoiceNumber}")
                        }
                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Receipt via"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronOrange),
                    modifier = Modifier.weight(1f).height(46.dp).testTag("btn_share_thermal_bill"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share / Print", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
