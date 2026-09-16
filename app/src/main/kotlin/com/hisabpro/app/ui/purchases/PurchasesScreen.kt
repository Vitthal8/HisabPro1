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
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hisabpro.app.data.model.InvoiceStatus
import com.hisabpro.app.data.model.PurchaseBill
import com.hisabpro.app.ui.theme.Emerald700
import com.hisabpro.app.ui.theme.Emerald800
import com.hisabpro.app.ui.theme.ExpenseRed
import com.hisabpro.app.ui.theme.IncomeGreen
import com.hisabpro.app.ui.theme.PureWhite
import com.hisabpro.app.ui.theme.Slate100
import com.hisabpro.app.ui.theme.Slate200
import com.hisabpro.app.ui.theme.Slate700
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
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Header info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocalShipping,
                                    contentDescription = null,
                                    tint = Emerald700,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Inward Stock & Purchases",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
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

                        // Cards Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Total Purchases
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Slate100),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "PURCHASES",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "₹${String.format(Locale.ENGLISH, "%.0f", uiState.totalPurchasesAmount)}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Emerald800
                                    )
                                    Text(
                                        text = "${uiState.totalBillCount} Bills",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // ITC Claimable
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "ITC CREDIT",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1D4ED8)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "₹${String.format(Locale.ENGLISH, "%.0f", uiState.totalItcAmount)}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E40AF)
                                    )
                                    Text(
                                        text = "Tax Offset",
                                        fontSize = 10.sp,
                                        color = Color(0xFF3B82F6)
                                    )
                                }
                            }

                            // Supplier Payables
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "PAYABLES",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ExpenseRed
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "₹${String.format(Locale.ENGLISH, "%.0f", uiState.totalSupplierPayables)}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ExpenseRed
                                    )
                                    Text(
                                        text = "To Suppliers",
                                        fontSize = 10.sp,
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
                            FilterChip(
                                selected = uiState.selectedFilter == filter,
                                onClick = { viewModel.setFilter(filter) },
                                label = { Text(filter.label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Emerald700,
                                    selectedLabelColor = PureWhite
                                ),
                                modifier = Modifier.testTag("filter_purchase_${filter.name.lowercase()}")
                            )
                        }
                    }
                }

                // Purchases List
                if (uiState.filteredPurchases.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = null,
                                tint = Emerald700.copy(alpha = 0.4f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No purchase bills found",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Record inward supplier bills to auto-add stock and claim ITC.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                } else {
                    items(uiState.filteredPurchases, key = { it.id }) { bill ->
                        PurchaseBillCard(
                            bill = bill,
                            onClick = { viewModel.selectPurchase(bill) }
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
            }
        )
    }
}

@Composable
fun PurchaseBillCard(
    bill: PurchaseBill,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM", Locale.ENGLISH) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bill.supplierName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = bill.purchaseNumber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Emerald800
                        )
                        if (bill.vendorBillNumber.isNotBlank()) {
                            Text(
                                text = "• Ref: ${bill.vendorBillNumber}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "• ${dateFormat.format(Date(bill.dateMillis))}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Amount
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${String.format(Locale.ENGLISH, "%.2f", bill.grandTotal)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Emerald800
                    )
                    Surface(
                        color = when (bill.paymentStatus) {
                            InvoiceStatus.PAID -> IncomeGreen.copy(alpha = 0.15f)
                            InvoiceStatus.PARTIAL -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                            InvoiceStatus.UNPAID -> ExpenseRed.copy(alpha = 0.15f)
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (bill.dueAmount > 0) "Due ₹${String.format(Locale.ENGLISH, "%.0f", bill.dueAmount)}" else "Paid",
                            color = when (bill.paymentStatus) {
                                InvoiceStatus.PAID -> IncomeGreen
                                InvoiceStatus.PARTIAL -> Color(0xFFD97706)
                                InvoiceStatus.UNPAID -> ExpenseRed
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (bill.itcEligible && bill.totalTax > 0) {
                    Surface(
                        color = Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "ITC: ₹${String.format(Locale.ENGLISH, "%.0f", bill.totalTax)}",
                            fontSize = 10.sp,
                            color = Color(0xFF1D4ED8),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
