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
import androidx.compose.material.icons.filled.Inventory2
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockValuationSheet(
    sheetState: SheetState,
    stock: StockValuationSummary,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PureWhite,
        modifier = Modifier.testTag("sheet_stock_valuation_report")
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
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = Emerald800,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Stock Valuation & Health",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Emerald900
                        )
                        Text(
                            text = "Capital Locked in Inventory & Margin",
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

            // Valuation Cards
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
                        text = "CAPITAL VALUATION SUMMARY",
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
                            Text("Retail Valuation (Sale)", fontSize = 12.sp, color = Color(0xFF667085))
                            Text(
                                text = "₹${HisabViewModel.formatAmount(stock.totalSellingValue)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Emerald900
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Cost Valuation (Purchase)", fontSize = 12.sp, color = Color(0xFF667085))
                            Text(
                                text = "₹${HisabViewModel.formatAmount(stock.totalCostValue)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFFE4E7EC))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Potential Gross Profit", fontSize = 11.sp, color = Color(0xFF667085))
                            Text(
                                text = "₹${HisabViewModel.formatAmount(stock.potentialProfit)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF16A34A)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Stock Quantity", fontSize = 11.sp, color = Color(0xFF667085))
                            Text(
                                text = "${stock.totalStockUnits.toInt()} Units across ${stock.totalItemsCount} items",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155)
                            )
                        }
                    }
                }
            }

            // Low Stock & Reorder Alerts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Low Stock Alerts", fontSize = 11.sp, color = Color(0xFFD97706), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${stock.lowStockCount} Items",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFD97706)
                        )
                        Text("Need immediate reordering", fontSize = 10.sp, color = Color(0xFF64748B))
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Out of Stock", fontSize = 11.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${stock.outOfStockCount} Items",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFDC2626)
                        )
                        Text("Zero available inventory", fontSize = 10.sp, color = Color(0xFF64748B))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
