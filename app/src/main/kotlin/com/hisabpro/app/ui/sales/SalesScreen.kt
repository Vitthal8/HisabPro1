package com.hisabpro.app.ui.sales

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
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
import androidx.compose.runtime.collectAsState
import com.hisabpro.app.ui.theme.Emerald700
import com.hisabpro.app.ui.theme.Emerald800
import com.hisabpro.app.ui.theme.Emerald900
import com.hisabpro.app.ui.theme.ExpenseRed
import com.hisabpro.app.ui.theme.IncomeGreen
import com.hisabpro.app.ui.theme.PureWhite
import com.hisabpro.app.ui.theme.Slate100
import com.hisabpro.app.ui.theme.Slate200
import com.hisabpro.app.ui.theme.Slate700
import com.hisabpro.app.ui.theme.Slate800
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
    var showProfileSheet by remember { mutableStateOf(false) }
    var showCreateSheet by remember { mutableStateOf(false) }
    var invoiceToEdit by remember { mutableStateOf<Invoice?>(null) }
    var initialCreateType by remember { mutableStateOf(InvoiceType.TAX_INVOICE) }
    var showSearchBar by remember { mutableStateOf(false) }

    val createSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val profileSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = PureWhite,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (selectedBillingTab == 0) "Sales & Invoicing" else "Inward Purchases",
                                    color = PureWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                if (businessProfile.shopName.isNotBlank()) {
                                    Text(
                                        text = businessProfile.shopName,
                                        color = PureWhite.copy(alpha = 0.8f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showProfileSheet = true },
                            modifier = Modifier.testTag("btn_store_profile_settings")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = "Store Profile & Settings",
                                tint = PureWhite
                            )
                        }
                        if (selectedBillingTab == 0) {
                            IconButton(
                                onClick = { showSearchBar = !showSearchBar },
                                modifier = Modifier.testTag("btn_sales_search_toggle")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search Invoices",
                                    tint = PureWhite
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Emerald800
                    )
                )

                // Segmented Tabs: Sales (Outward) vs Purchases (Inward)
                TabRow(
                    selectedTabIndex = selectedBillingTab,
                    containerColor = Emerald800,
                    contentColor = PureWhite,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedBillingTab]),
                            color = PureWhite
                        )
                    }
                ) {
                    Tab(
                        selected = selectedBillingTab == 0,
                        onClick = { selectedBillingTab = 0 },
                        text = {
                            Text(
                                text = "Sales (Outward)",
                                fontWeight = if (selectedBillingTab == 0) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("tab_billing_sales")
                    )
                    Tab(
                        selected = selectedBillingTab == 1,
                        onClick = { selectedBillingTab = 1 },
                        text = {
                            Text(
                                text = "Purchases (Inward)",
                                fontWeight = if (selectedBillingTab == 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.LocalShipping,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("tab_billing_purchases")
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedBillingTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = {
                        initialCreateType = InvoiceType.TAX_INVOICE
                        showCreateSheet = true
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("New Invoice", fontWeight = FontWeight.Bold) },
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
            ) {
            // Search Bar (Animated)
            AnimatedVisibility(visible = showSearchBar) {
                Surface(
                    color = Emerald800,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search by Inv #, Customer, or Item...", color = Slate700) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Emerald700)
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = Slate700)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .background(PureWhite, RoundedCornerShape(12.dp))
                            .testTag("sales_search_input")
                    )
                }
            }

            // Financial Summary Banner
            SalesSummaryBanner(
                totalSales = uiState.totalSalesVolume,
                totalTax = uiState.totalTaxCollected,
                totalDue = uiState.totalPendingDue
            )

            // Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.typeFilter == null && uiState.statusFilter == null,
                    onClick = {
                        viewModel.setTypeFilter(null)
                        viewModel.setStatusFilter(null)
                    },
                    label = { Text("All (${uiState.invoices.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Emerald700,
                        selectedLabelColor = PureWhite
                    )
                )

                FilterChip(
                    selected = uiState.typeFilter == InvoiceType.TAX_INVOICE,
                    onClick = {
                        viewModel.setTypeFilter(if (uiState.typeFilter == InvoiceType.TAX_INVOICE) null else InvoiceType.TAX_INVOICE)
                        viewModel.setStatusFilter(null)
                    },
                    label = { Text("GST Invoices") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Emerald700,
                        selectedLabelColor = PureWhite
                    )
                )

                FilterChip(
                    selected = uiState.typeFilter == InvoiceType.NON_GST_BILL,
                    onClick = {
                        viewModel.setTypeFilter(if (uiState.typeFilter == InvoiceType.NON_GST_BILL) null else InvoiceType.NON_GST_BILL)
                        viewModel.setStatusFilter(null)
                    },
                    label = { Text("Simple Bills") },
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
                    label = { Text("Quotations") },
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
                    label = { Text("Unpaid / Due") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ExpenseRed,
                        selectedLabelColor = PureWhite
                    )
                )
            }

            // Quick Counter Sale Shortcut Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = Emerald700.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clickable {
                        initialCreateType = InvoiceType.NON_GST_BILL
                        showCreateSheet = true
                    }
                    .testTag("card_quick_counter_sale")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
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
                            Text("⚡ Quick Cash Counter Sale", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Emerald800)
                            Text("Fast 5-second cash bill, no party info needed", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Text("TAP HERE >", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Emerald700)
                }
            }

            // Invoices List
            if (uiState.filteredInvoices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = Slate700.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "No invoices found",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Slate700
                        )
                        Text(
                            text = "Create a new GST invoice, quick bill, or quotation.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
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
                            }
                        )
                    }
                }
            }
        }
    }
    }

    // Business Profile & Settings Sheet
    if (showProfileSheet) {
        BusinessProfileSheet(
            profile = businessProfile,
            sheetState = profileSheetState,
            onDismiss = { showProfileSheet = false },
            onSaveProfile = { updated ->
                settingsRepo.updateProfile(updated)
                showProfileSheet = false
                Toast.makeText(context, "Business profile updated!", Toast.LENGTH_SHORT).show()
            }
        )
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
            sheetState = createSheetState,
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

                    // Automatically deduct stock for any items sold
                    toSave.items.forEach { lineItem ->
                        itemViewModel?.deductStockForInvoiceItem(
                            itemNameOrId = lineItem.description,
                            quantity = lineItem.quantity,
                            invoiceNumber = saved.invoiceNumber
                        )
                    }

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
private fun SalesSummaryBanner(
    totalSales: Double,
    totalTax: Double,
    totalDue: Double
) {
    Surface(
        color = Emerald900,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1.2f)) {
                Text(
                    text = "TOTAL SALES",
                    color = PureWhite.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "₹${String.format(Locale.ENGLISH, "%,.0f", totalSales)}",
                    color = PureWhite,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "GST TAX",
                    color = PureWhite.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "₹${String.format(Locale.ENGLISH, "%,.0f", totalTax)}",
                    color = PureWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "PENDING DUE",
                    color = PureWhite.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "₹${String.format(Locale.ENGLISH, "%,.0f", totalDue)}",
                    color = if (totalDue > 0) Color(0xFFFFB4AB) else PureWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
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
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH) }

    Card(
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("invoice_card_${invoice.invoiceNumber}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: Invoice No + Date + Type Badge + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = invoice.invoiceNumber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Slate800
                    )
                    Surface(
                        color = when (invoice.type) {
                            InvoiceType.TAX_INVOICE -> Emerald700.copy(alpha = 0.12f)
                            InvoiceType.NON_GST_BILL -> Slate200
                            InvoiceType.PROFORMA -> Color(0xFFE0E7FF)
                        },
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = when (invoice.type) {
                                InvoiceType.TAX_INVOICE -> "GST"
                                InvoiceType.NON_GST_BILL -> "BILL"
                                InvoiceType.PROFORMA -> "QUOT"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (invoice.type) {
                                InvoiceType.TAX_INVOICE -> Emerald800
                                InvoiceType.NON_GST_BILL -> Slate700
                                InvoiceType.PROFORMA -> Color(0xFF3730A3)
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = dateFormat.format(Date(invoice.dateMillis)),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        color = when (invoice.paymentStatus) {
                            InvoiceStatus.PAID -> IncomeGreen.copy(alpha = 0.15f)
                            InvoiceStatus.PARTIAL -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                            InvoiceStatus.UNPAID -> ExpenseRed.copy(alpha = 0.15f)
                        },
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = invoice.paymentStatus.label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (invoice.paymentStatus) {
                                InvoiceStatus.PAID -> IncomeGreen
                                InvoiceStatus.PARTIAL -> Color(0xFFD97706)
                                InvoiceStatus.UNPAID -> ExpenseRed
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Customer Name & Items Preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = invoice.customerName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val itemsPreview = invoice.items.joinToString(", ") { it.description }
                    Text(
                        text = if (itemsPreview.isNotBlank()) "${invoice.items.size} items: $itemsPreview" else "No items",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${String.format(Locale.ENGLISH, "%.2f", invoice.grandTotal)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = Emerald800
                    )
                    if (invoice.dueAmount > 0) {
                        Text(
                            text = "Due: ₹${String.format(Locale.ENGLISH, "%.2f", invoice.dueAmount)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed
                        )
                    }
                }
            }

            HorizontalDivider(color = Slate100, modifier = Modifier.padding(top = 4.dp))

            // Quick Actions: WhatsApp, PDF, Duplicate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        color = Color(0xFF25D366).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.clickable(onClick = onShareWhatsApp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "WhatsApp", tint = Color(0xFF1E7E34), modifier = Modifier.size(13.dp))
                            Text("WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E7E34))
                        }
                    }

                    Surface(
                        color = Emerald700.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.clickable(onClick = onSharePdf)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = "PDF", tint = Emerald800, modifier = Modifier.size(13.dp))
                            Text("PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Emerald800)
                        }
                    }

                    Surface(
                        color = Color(0xFF0F766E).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.clickable(onClick = onShareThermal)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Print, contentDescription = "Thermal Slip", tint = Color(0xFF0F766E), modifier = Modifier.size(13.dp))
                            Text("POS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F766E))
                        }
                    }
                }

                Surface(
                    color = Slate100,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.clickable(onClick = onDuplicate)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = Slate700, modifier = Modifier.size(13.dp))
                        Text("Repeat / Clone", fontSize = 11.sp, color = Slate700)
                    }
                }
            }
        }
    }
}
