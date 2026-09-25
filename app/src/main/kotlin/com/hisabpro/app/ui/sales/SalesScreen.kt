package com.hisabpro.app.ui.sales

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.hisabpro.app.R
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hisabpro.app.data.model.Invoice
import com.hisabpro.app.data.model.InvoiceStatus
import com.hisabpro.app.data.model.InvoiceType
import com.hisabpro.app.data.repository.SettingsRepository
import com.hisabpro.app.ui.items.ItemViewModel
import com.hisabpro.app.ui.purchases.PurchaseViewModel
import com.hisabpro.app.ui.purchases.PurchasesScreen
import com.hisabpro.app.ui.settings.BusinessProfileSheet
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
fun SalesScreen(
    viewModel: InvoiceViewModel,
    itemViewModel: ItemViewModel? = null,
    purchaseViewModel: PurchaseViewModel? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val parties by viewModel.parties.collectAsStateWithLifecycle(initialValue = emptyList())
    val inventoryItems = itemViewModel?.rawItems?.collectAsState()?.value ?: emptyList()

    val settingsRepo = remember { SettingsRepository.getInstance(context) }
    val businessProfile by settingsRepo.profile.collectAsStateWithLifecycle()

    var selectedBillingTab by rememberSaveable { mutableIntStateOf(0) } // 0: Sales, 1: Purchases
    var showCreateSheet by remember { mutableStateOf(false) }
    var invoiceToEdit by remember { mutableStateOf<Invoice?>(null) }
    var initialCreateType by remember {
        mutableStateOf(if (businessProfile.isGstRegistered) InvoiceType.TAX_INVOICE else InvoiceType.NON_GST_BILL)
    }
    var showSearchBar by remember { mutableStateOf(false) }

    val createSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Emerald700.copy(alpha = 0.12f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (selectedBillingTab == 0) Icons.Default.ReceiptLong else Icons.Default.LocalShipping,
                                        contentDescription = null,
                                        tint = Emerald700,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (selectedBillingTab == 0) stringResource(R.string.sales_invoices) else stringResource(R.string.purchases),
                                    color = Slate900,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                                if (businessProfile.shopName.isNotBlank()) {
                                    Text(
                                        text = businessProfile.shopName,
                                        color = Slate600,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showSearchBar = !showSearchBar },
                            modifier = Modifier.testTag("btn_sales_search_toggle")
                        ) {
                            Icon(
                                imageVector = if (showSearchBar) Icons.Default.Clear else Icons.Default.Search,
                                contentDescription = "Search Invoices",
                                tint = if (showSearchBar) Emerald700 else Slate700
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Modern Segmented Pill Tabs: Sales (Outward) vs Purchases (Inward)
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Slate100)
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Tab 0: Sales (Outward)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (selectedBillingTab == 0) PureWhite else Color.Transparent,
                            shadowElevation = if (selectedBillingTab == 0) 2.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { selectedBillingTab = 0 }
                                .testTag("tab_billing_sales")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = if (selectedBillingTab == 0) Emerald700 else Slate500,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Sales (Outward)",
                                    fontWeight = if (selectedBillingTab == 0) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = if (selectedBillingTab == 0) Emerald800 else Slate600
                                )
                            }
                        }

                        // Tab 1: Purchases (Inward)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (selectedBillingTab == 1) PureWhite else Color.Transparent,
                            shadowElevation = if (selectedBillingTab == 1) 2.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { selectedBillingTab = 1 }
                                .testTag("tab_billing_purchases")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalShipping,
                                    contentDescription = null,
                                    tint = if (selectedBillingTab == 1) Emerald700 else Slate500,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Purchases (Inward)",
                                    fontWeight = if (selectedBillingTab == 1) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = if (selectedBillingTab == 1) Emerald800 else Slate600
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = Slate200.copy(alpha = 0.6f))
            }
        },
        floatingActionButton = {
            if (selectedBillingTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = {
                        initialCreateType = if (businessProfile.isGstRegistered) InvoiceType.TAX_INVOICE else InvoiceType.NON_GST_BILL
                        showCreateSheet = true
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = {
                        Text(
                            text = if (businessProfile.isGstRegistered) "New Invoice" else "New Bill",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    containerColor = Emerald700,
                    contentColor = PureWhite,
                    modifier = Modifier.testTag("fab_create_invoice")
                )
            }
        }
    ) { innerPadding ->
        if (selectedBillingTab == 1) {
            val pVm = purchaseViewModel ?: androidx.lifecycle.viewmodel.compose.viewModel()
            PurchasesScreen(
                viewModel = pVm,
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Slate50)
            ) {
                // Search Bar (Animated)
                AnimatedVisibility(visible = showSearchBar) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Search by Inv #, Customer, or Item...", color = Slate400) },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Emerald700)
                            },
                            trailingIcon = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = Slate500)
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Slate900,
                                unfocusedTextColor = Slate900,
                                focusedBorderColor = Emerald700,
                                unfocusedBorderColor = Slate200
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .testTag("sales_search_input")
                        )
                    }
                }

                // Financial Summary Card
                SalesSummaryCard(
                    totalSales = uiState.totalSalesVolume,
                    totalTax = uiState.totalTaxCollected,
                    totalDue = uiState.totalPendingDue,
                    invoiceCount = uiState.invoices.size,
                    isGstRegistered = businessProfile.isGstRegistered,
                    onDueClick = {
                        viewModel.setStatusFilter(if (uiState.statusFilter == InvoiceStatus.UNPAID) null else InvoiceStatus.UNPAID)
                        viewModel.setTypeFilter(null)
                    }
                )

                // Quick Sales Action Cards (Fast Cash Sale & Full Invoice)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Fast Cash Sale Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Emerald50),
                        border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                initialCreateType = InvoiceType.NON_GST_BILL
                                showCreateSheet = true
                            }
                            .testTag("card_quick_counter_sale")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Emerald700),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FlashOn,
                                    contentDescription = null,
                                    tint = PureWhite,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "⚡ Quick Cash Sale",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Emerald900
                                )
                                Text(
                                    text = "Fast 5-sec bill",
                                    fontSize = 11.sp,
                                    color = Emerald800
                                )
                            }
                        }
                    }

                    // Detailed Bill / GST Invoice Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        border = BorderStroke(1.dp, Slate200),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                initialCreateType = if (businessProfile.isGstRegistered) InvoiceType.TAX_INVOICE else InvoiceType.NON_GST_BILL
                                showCreateSheet = true
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Slate100),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = Slate700,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (businessProfile.isGstRegistered) "+ GST Invoice" else "+ Sales Bill",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Slate900
                                )
                                Text(
                                    text = "Itemized & Party",
                                    fontSize = 11.sp,
                                    color = Slate600
                                )
                            }
                        }
                    }
                }

                // Filter Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.typeFilter == null && uiState.statusFilter == null,
                        onClick = {
                            viewModel.setTypeFilter(null)
                            viewModel.setStatusFilter(null)
                        },
                        label = { Text("All (${uiState.invoices.size})", fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Emerald700,
                            selectedLabelColor = PureWhite
                        )
                    )

                    if (businessProfile.isGstRegistered) {
                        FilterChip(
                            selected = uiState.typeFilter == InvoiceType.TAX_INVOICE,
                            onClick = {
                                viewModel.setTypeFilter(if (uiState.typeFilter == InvoiceType.TAX_INVOICE) null else InvoiceType.TAX_INVOICE)
                                viewModel.setStatusFilter(null)
                            },
                            label = { Text("GST Invoices", fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Emerald700,
                                selectedLabelColor = PureWhite
                            )
                        )
                    }

                    FilterChip(
                        selected = uiState.typeFilter == InvoiceType.NON_GST_BILL,
                        onClick = {
                            viewModel.setTypeFilter(if (uiState.typeFilter == InvoiceType.NON_GST_BILL) null else InvoiceType.NON_GST_BILL)
                            viewModel.setStatusFilter(null)
                        },
                        label = { Text(if (businessProfile.isGstRegistered) "Simple Bills" else "Sales Bills", fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Emerald700,
                            selectedLabelColor = PureWhite
                        )
                    )

                    FilterChip(
                        selected = uiState.typeFilter == InvoiceType.PROFORMA,
                        onClick = {
                            viewModel.setTypeFilter(if (uiState.typeFilter == InvoiceType.PROFORMA) null else InvoiceType.PROFORMA)
                            viewModel.setStatusFilter(null)
                        },
                        label = { Text("Quotations", fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Emerald700,
                            selectedLabelColor = PureWhite
                        )
                    )

                    FilterChip(
                        selected = uiState.statusFilter == InvoiceStatus.UNPAID,
                        onClick = {
                            viewModel.setStatusFilter(if (uiState.statusFilter == InvoiceStatus.UNPAID) null else InvoiceStatus.UNPAID)
                            viewModel.setTypeFilter(null)
                        },
                        label = { Text("Unpaid / Due", fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ExpenseRed,
                            selectedLabelColor = PureWhite
                        )
                    )
                }

                // Invoices List
                if (uiState.filteredInvoices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Emerald50,
                                modifier = Modifier.size(80.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.ReceiptLong,
                                        contentDescription = null,
                                        tint = Emerald700,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (uiState.searchQuery.isNotBlank()) "No matching invoices found" else "No invoices recorded yet",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = Slate800
                            )
                            Text(
                                text = if (uiState.searchQuery.isNotBlank()) "Try searching with a different term"
                                else "Create your first cash bill or GST tax invoice to get started.",
                                fontSize = 13.sp,
                                color = Slate600
                            )
                            Button(
                                onClick = {
                                    initialCreateType = if (businessProfile.isGstRegistered) InvoiceType.TAX_INVOICE else InvoiceType.NON_GST_BILL
                                    showCreateSheet = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Create First Bill", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = uiState.filteredInvoices,
                            key = { it.id }
                        ) { invoice ->
                            InvoiceCard(
                                invoice = invoice,
                                onClick = { viewModel.selectInvoice(invoice) },
                                onShareWhatsApp = { viewModel.shareWhatsAppSummary(context, invoice) },
                                onSharePdf = { viewModel.sharePdf(context, invoice, false) },
                                onShareThermal = {
                                    com.hisabpro.app.util.ThermalSlipGenerator.shareThermalSlip(context, invoice, businessProfile)
                                },
                                onDuplicate = {
                                    val dup = viewModel.duplicateInvoice(invoice.id)
                                    Toast.makeText(context, "Duplicated as ${dup?.invoiceNumber}", Toast.LENGTH_SHORT).show()
                                },
                                onEdit = {
                                    invoiceToEdit = invoice
                                    showCreateSheet = true
                                },
                                onMarkAsPaid = {
                                    viewModel.markAsPaid(invoice)
                                    Toast.makeText(context, "Marked as Paid!", Toast.LENGTH_SHORT).show()
                                },
                                onDelete = {
                                    viewModel.deleteInvoice(invoice.id)
                                    Toast.makeText(context, "Invoice deleted", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Detail Sheet
    if (uiState.selectedInvoice != null) {
        InvoiceDetailSheet(
            invoice = uiState.selectedInvoice!!,
            sheetState = detailSheetState,
            onDismiss = { viewModel.selectInvoice(null) },
            onSharePdf = { inv, isWhatsApp ->
                viewModel.sharePdf(context, inv, isWhatsApp)
            },
            onDuplicate = { inv ->
                val dup = viewModel.duplicateInvoice(inv.id)
                Toast.makeText(context, "Duplicated as ${dup?.invoiceNumber}", Toast.LENGTH_SHORT).show()
            },
            onMarkAsPaid = { inv ->
                viewModel.markAsPaid(inv)
                Toast.makeText(context, "Marked as Paid!", Toast.LENGTH_SHORT).show()
            },
            onDelete = { inv ->
                viewModel.deleteInvoice(inv.id)
                Toast.makeText(context, "Invoice deleted", Toast.LENGTH_SHORT).show()
            },
            onEditInvoice = { inv ->
                viewModel.selectInvoice(null)
                invoiceToEdit = inv
                showCreateSheet = true
            }
        )
    }

    // Create / Edit Sheet
    if (showCreateSheet) {
        CreateInvoiceSheet(
            parties = parties,
            availableItems = inventoryItems,
            initialInvoiceType = initialCreateType,
            invoiceToEdit = invoiceToEdit,
            onDismiss = {
                showCreateSheet = false
                invoiceToEdit = null
            },
            onSaveInvoice = { invoice, action ->
                if (invoiceToEdit != null) {
                    viewModel.updateInvoice(invoice)
                    Toast.makeText(context, "Invoice ${invoice.invoiceNumber} updated!", Toast.LENGTH_SHORT).show()
                } else {
                    val nextNum = viewModel.getNextInvoiceNumber(invoice.type)
                    val toSave = invoice.copy(invoiceNumber = nextNum)
                    val saved = viewModel.createInvoice(toSave)

                    when (action) {
                        SaveAction.SAVE_ONLY -> {
                            Toast.makeText(context, "Invoice ${saved.invoiceNumber} saved!", Toast.LENGTH_SHORT).show()
                        }
                        SaveAction.SAVE_AND_WHATSAPP -> {
                            viewModel.shareWhatsAppSummary(context, saved)
                        }
                        SaveAction.SAVE_AND_PDF -> {
                            viewModel.sharePdf(context, saved, false)
                        }
                    }
                }
                showCreateSheet = false
                invoiceToEdit = null
            }
        )
    }
}

@Composable
private fun SalesSummaryCard(
    totalSales: Double,
    totalTax: Double,
    totalDue: Double,
    invoiceCount: Int,
    isGstRegistered: Boolean = true,
    onDueClick: () -> Unit = {}
) {
    val totalReceived = kotlin.math.max(0.0, totalSales - totalDue)
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Slate200.copy(alpha = 0.7f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Total Sales Hero + Count
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
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = Emerald700,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "TOTAL SALES REVENUE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate500,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "₹${String.format(Locale.ENGLISH, "%,.0f", totalSales)}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = Slate900
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate100
                ) {
                    Text(
                        text = "$invoiceCount Invoices",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate700,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = Slate100)

            // Bottom 2 or 3 Metric Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Received
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(IncomeGreen.copy(alpha = 0.08f))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(IncomeGreen)
                            )
                            Text(
                                text = "RECEIVED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = IncomeGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "₹${String.format(Locale.ENGLISH, "%,.0f", totalReceived)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = IncomeGreen
                        )
                    }
                }

                // Pending Due
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (totalDue > 0) ExpenseRed.copy(alpha = 0.08f) else Slate100)
                        .clickable(enabled = totalDue > 0, onClick = onDueClick)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (totalDue > 0) ExpenseRed else Slate400)
                            )
                            Text(
                                text = "PENDING DUE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (totalDue > 0) ExpenseRed else Slate600
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "₹${String.format(Locale.ENGLISH, "%,.0f", totalDue)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (totalDue > 0) ExpenseRed else Slate700
                        )
                    }
                }

                if (isGstRegistered) {
                    // GST Tax Collected
                    Box(
                        modifier = Modifier
                            .weight(0.9f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Emerald700.copy(alpha = 0.08f))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Column {
                            Text(
                                text = "GST TAX",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Emerald800
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "₹${String.format(Locale.ENGLISH, "%,.0f", totalTax)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Emerald800
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InvoiceCard(
    invoice: Invoice,
    onClick: () -> Unit,
    onShareWhatsApp: () -> Unit,
    onSharePdf: () -> Unit,
    onShareThermal: () -> Unit = {},
    onDuplicate: () -> Unit,
    onEdit: () -> Unit = {},
    onMarkAsPaid: () -> Unit = {},
    onDelete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH) }
    var showMenu by remember { mutableStateOf(false) }

    val statusColor = when (invoice.paymentStatus) {
        InvoiceStatus.PAID -> IncomeGreen
        InvoiceStatus.PARTIAL -> Color(0xFFD97706)
        InvoiceStatus.UNPAID -> ExpenseRed
        InvoiceStatus.CANCELLED -> Slate500
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        border = BorderStroke(1.dp, Slate200.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("invoice_card_${invoice.invoiceNumber}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Customer Info + Payment Status Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Avatar Circle with initial
                    val initial = invoice.customerName.firstOrNull()?.uppercase() ?: "C"
                    Surface(
                        shape = CircleShape,
                        color = when (invoice.type) {
                            InvoiceType.TAX_INVOICE -> Emerald700.copy(alpha = 0.12f)
                            InvoiceType.NON_GST_BILL -> Slate100
                            InvoiceType.PROFORMA -> Color(0xFFEEF2FF)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = initial,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = when (invoice.type) {
                                    InvoiceType.TAX_INVOICE -> Emerald700
                                    InvoiceType.NON_GST_BILL -> Slate700
                                    InvoiceType.PROFORMA -> Color(0xFF4338CA)
                                }
                            )
                        }
                    }

                    Column {
                        Text(
                            text = invoice.customerName.ifBlank { "Cash Customer" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Slate900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = invoice.invoiceNumber,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = Emerald800
                            )
                            Text("•", fontSize = 10.sp, color = Slate400)
                            Text(
                                text = dateFormat.format(Date(invoice.dateMillis)),
                                fontSize = 11.sp,
                                color = Slate500
                            )
                        }
                    }
                }

                // Payment Status Badge
                Surface(
                    color = statusColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Text(
                            text = invoice.paymentStatus.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }
            }

            // Middle Section: Line items summary and Type Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Line Items Summary
                val itemsPreview = invoice.items.joinToString(", ") { it.description }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = null,
                        tint = Slate400,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = if (itemsPreview.isNotBlank()) "${invoice.items.size} items (${itemsPreview})" else "No items",
                        fontSize = 12.sp,
                        color = Slate600,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Invoice Type Pill
                Surface(
                    color = when (invoice.type) {
                        InvoiceType.TAX_INVOICE -> Emerald50
                        InvoiceType.NON_GST_BILL -> Slate100
                        InvoiceType.PROFORMA -> Color(0xFFEEF2FF)
                    },
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = when (invoice.type) {
                            InvoiceType.TAX_INVOICE -> "GST INVOICE"
                            InvoiceType.NON_GST_BILL -> "CASH BILL"
                            InvoiceType.PROFORMA -> "QUOTATION"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (invoice.type) {
                            InvoiceType.TAX_INVOICE -> Emerald700
                            InvoiceType.NON_GST_BILL -> Slate700
                            InvoiceType.PROFORMA -> Color(0xFF4338CA)
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Grand Total and Pending Due Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Slate50)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "GRAND TOTAL",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate500
                    )
                    Text(
                        text = "₹${String.format(Locale.ENGLISH, "%.2f", invoice.grandTotal)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = Slate900
                    )
                }

                if (invoice.dueAmount > 0) {
                    Surface(
                        color = ExpenseRed.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = ExpenseRed,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Due: ₹${String.format(Locale.ENGLISH, "%.2f", invoice.dueAmount)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Slate100)

            // Action Buttons: WhatsApp, PDF, POS Thermal, More (Duplicate, Edit, Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // WhatsApp Action
                    Surface(
                        color = Color(0xFF25D366).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable(onClick = onShareWhatsApp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "WhatsApp",
                                tint = Color(0xFF15803D),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "WhatsApp",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D)
                            )
                        }
                    }

                    // PDF Action
                    Surface(
                        color = Emerald700.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable(onClick = onSharePdf)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "PDF",
                                tint = Emerald800,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "PDF",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Emerald800
                            )
                        }
                    }

                    // POS Slip Action
                    Surface(
                        color = Slate100,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable(onClick = onShareThermal)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = "Thermal POS",
                                tint = Slate700,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "POS",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate700
                            )
                        }
                    }
                }

                // More Menu Button (Duplicate, Edit, Mark as Paid, Delete)
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More Options",
                            tint = Slate600
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Repeat / Duplicate") },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Emerald700) },
                            onClick = {
                                showMenu = false
                                onDuplicate()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit Invoice") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Slate700) },
                            onClick = {
                                showMenu = false
                                onEdit()
                            }
                        )
                        if (invoice.paymentStatus != InvoiceStatus.PAID) {
                            DropdownMenuItem(
                                text = { Text("Mark as Full Paid") },
                                leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = IncomeGreen) },
                                onClick = {
                                    showMenu = false
                                    onMarkAsPaid()
                                }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Delete", color = ExpenseRed) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ExpenseRed) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}
