package com.hisabpro.app.ui.purchases

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hisabpro.app.data.model.InvoiceStatus
import com.hisabpro.app.data.model.PurchaseBill
import androidx.compose.foundation.BorderStroke
import com.hisabpro.app.ui.theme.Emerald50
import com.hisabpro.app.ui.theme.Emerald700
import com.hisabpro.app.ui.theme.Emerald800
import com.hisabpro.app.ui.theme.Emerald900
import com.hisabpro.app.ui.theme.ExpenseRed
import com.hisabpro.app.ui.theme.IncomeGreen
import com.hisabpro.app.ui.theme.PureWhite
import com.hisabpro.app.ui.theme.Slate50
import com.hisabpro.app.ui.theme.Slate100
import com.hisabpro.app.ui.theme.Slate200
import com.hisabpro.app.ui.theme.Slate400
import com.hisabpro.app.ui.theme.Slate500
import com.hisabpro.app.ui.theme.Slate600
import com.hisabpro.app.ui.theme.Slate700
import com.hisabpro.app.ui.theme.Slate800
import com.hisabpro.app.ui.theme.Slate900
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchasesScreen(
    viewModel: PurchaseViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()
    val inventoryItems by viewModel.inventoryItems.collectAsStateWithLifecycle()

    var showCreateSheet by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }

    val createSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateSheet = true },
                containerColor = Emerald700,
                contentColor = PureWhite,
                icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
                text = { Text("New Purchase Bill", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("fab_new_purchase")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar (Toggleable)
            AnimatedVisibility(visible = showSearchBar) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search supplier, bill no, or item...") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Emerald700)
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Emerald700,
                            unfocusedBorderColor = Slate200
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("input_search_purchases")
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Summary Metrics
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Header info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Emerald50,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.LocalShipping,
                                            contentDescription = null,
                                            tint = Emerald700,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Inward Stock & Purchases",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Slate900
                                    )
                                    Text(
                                        text = "${uiState.totalBillCount} recorded bills",
                                        fontSize = 12.sp,
                                        color = Slate500
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { viewModel.exportPurchasesCsv(context) },
                                    modifier = Modifier.testTag("btn_export_purchases_register")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FileDownload,
                                        contentDescription = "Export Purchases Register",
                                        tint = Emerald700
                                    )
                                }
                                IconButton(onClick = { showSearchBar = !showSearchBar }) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search Purchases",
                                        tint = Emerald700
                                    )
                                }
                            }
                        }

                        // Cards Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Total Purchases
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, Slate200),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "PURCHASES",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate500,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "₹${String.format(Locale.ENGLISH, "%.0f", uiState.totalPurchasesAmount)}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Slate900
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${uiState.totalBillCount} Bills",
                                        fontSize = 11.sp,
                                        color = Slate500
                                    )
                                }
                            }

                            // ITC Claimable
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "ITC CREDIT",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1D4ED8),
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "₹${String.format(Locale.ENGLISH, "%.0f", uiState.totalItcAmount)}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF1E40AF)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Tax Offset",
                                        fontSize = 11.sp,
                                        color = Color(0xFF3B82F6)
                                    )
                                }
                            }

                            // Supplier Payables
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                                border = BorderStroke(1.dp, Color(0xFFFECACA)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "PAYABLES",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ExpenseRed,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "₹${String.format(Locale.ENGLISH, "%.0f", uiState.totalSupplierPayables)}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ExpenseRed
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Due to Vendors",
                                        fontSize = 11.sp,
                                        color = ExpenseRed.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Filter Chips
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PurchaseFilter.entries.forEach { filter ->
                            val isSelected = uiState.selectedFilter == filter
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setFilter(filter) },
                                label = {
                                    Text(
                                        text = filter.label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Emerald700,
                                    selectedLabelColor = PureWhite,
                                    containerColor = Slate50,
                                    labelColor = Slate700
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = if (isSelected) Emerald700 else Slate200,
                                    selectedBorderColor = Emerald700,
                                    enabled = true,
                                    selected = isSelected
                                ),
                                modifier = Modifier.testTag("filter_purchase_${filter.name.lowercase()}")
                            )
                        }
                    }
                }

                // Purchases List
                if (uiState.filteredPurchases.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, Slate200),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Emerald50,
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.ShoppingBag,
                                            contentDescription = null,
                                            tint = Emerald700,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "No purchase bills found",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Slate900
                                )
                                Text(
                                    text = "Record inward supplier bills to auto-add stock and claim input tax credit.",
                                    fontSize = 13.sp,
                                    color = Slate500,
                                    modifier = Modifier.padding(top = 4.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(uiState.filteredPurchases, key = { it.id }) { bill ->
                        PurchaseBillCard(
                            bill = bill,
                            onClick = { viewModel.selectPurchase(bill) },
                            onShare = {
                                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
                                val text = buildString {
                                    appendLine("📦 *PURCHASE INWARD VOUCHER*")
                                    appendLine("Bill No: ${bill.purchaseNumber}")
                                    if (bill.vendorBillNumber.isNotBlank()) appendLine("Vendor Ref: ${bill.vendorBillNumber}")
                                    appendLine("Supplier: ${bill.supplierName}")
                                    appendLine("Date: ${dateFormat.format(Date(bill.dateMillis))}")
                                    appendLine("Grand Total: ₹${String.format(Locale.ENGLISH, "%.2f", bill.grandTotal)}")
                                    appendLine("Status: ${bill.paymentStatus.label}")
                                    appendLine("\nShared via HisabPro")
                                }
                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_TEXT, text)
                                }
                                context.startActivity(android.content.Intent.createChooser(intent, "Share Purchase Summary"))
                            }
                        )
                    }
                }
            }
        }
    }

    // Create Purchase Sheet
    if (showCreateSheet) {
        CreatePurchaseSheet(
            sheetState = createSheetState,
            suppliers = suppliers,
            inventoryItems = inventoryItems,
            nextPurchaseNumber = viewModel.generateNextNumber(),
            onDismiss = {
                scope.launch { createSheetState.hide() }
                showCreateSheet = false
            },
            onSavePurchase = { newBill ->
                viewModel.savePurchaseBill(newBill)
            }
        )
    }

    // Detail Purchase Sheet
    uiState.selectedPurchase?.let { bill ->
        PurchaseDetailSheet(
            bill = bill,
            sheetState = detailSheetState,
            onDismiss = {
                scope.launch { detailSheetState.hide() }
                viewModel.selectPurchase(null)
            },
            onMarkAsPaid = { updatedBill ->
                viewModel.markAsPaid(updatedBill)
            },
            onDelete = { billId ->
                viewModel.deletePurchase(billId)
            },
            onExportCsv = { billToExport ->
                viewModel.exportSingleBillCsv(context, billToExport)
            }
        )
    }
}

@Composable
fun PurchaseBillCard(
    bill: PurchaseBill,
    onClick: () -> Unit,
    onShare: (() -> Unit)? = null
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, yyyy", Locale.ENGLISH) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Slate200),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable(onClick = onClick)
            .testTag("purchase_card_${bill.purchaseNumber}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Emerald50,
                        border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.25f)),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = bill.supplierName.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Emerald800
                            )
                        }
                    }

                    Column {
                        Text(
                            text = bill.supplierName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Slate900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Slate100,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = bill.purchaseNumber,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Emerald800,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            if (bill.vendorBillNumber.isNotBlank()) {
                                Text(
                                    text = "• Ref: ${bill.vendorBillNumber}",
                                    fontSize = 11.sp,
                                    color = Slate500
                                )
                            }
                            Text(
                                text = "• ${dateFormat.format(Date(bill.dateMillis))}",
                                fontSize = 11.sp,
                                color = Slate500
                            )
                        }
                    }
                }

                // Amount & Status Badge
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${String.format(Locale.ENGLISH, "%.2f", bill.grandTotal)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = Slate900
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Surface(
                        color = when (bill.paymentStatus) {
                            InvoiceStatus.PAID -> IncomeGreen.copy(alpha = 0.12f)
                            InvoiceStatus.PARTIAL -> Color(0xFFFEF3C7)
                            InvoiceStatus.UNPAID -> ExpenseRed.copy(alpha = 0.12f)
                        },
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (bill.dueAmount > 0) "Due: ₹${String.format(Locale.ENGLISH, "%.0f", bill.dueAmount)}" else "PAID",
                            color = when (bill.paymentStatus) {
                                InvoiceStatus.PAID -> IncomeGreen
                                InvoiceStatus.PARTIAL -> Color(0xFFD97706)
                                InvoiceStatus.UNPAID -> ExpenseRed
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = Slate100)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Item preview
                val itemSummary = bill.items.joinToString(", ") { it.description }
                Text(
                    text = "${bill.items.size} item(s): $itemSummary",
                    fontSize = 12.sp,
                    color = Slate600,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (bill.itcEligible && bill.totalTax > 0) {
                        Surface(
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(0.5.dp, Color(0xFFBFDBFE)),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "ITC: ₹${String.format(Locale.ENGLISH, "%.0f", bill.totalTax)}",
                                fontSize = 10.sp,
                                color = Color(0xFF1D4ED8),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (onShare != null) {
                        IconButton(
                            onClick = onShare,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("btn_quick_share_${bill.purchaseNumber}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Bill Summary",
                                tint = Emerald700,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
