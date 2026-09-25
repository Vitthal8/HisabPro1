package com.hisabpro.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hisabpro.app.data.model.Category
import com.hisabpro.app.data.model.PaymentMode
import com.hisabpro.app.data.model.Transaction
import com.hisabpro.app.ui.components.AddTransactionDialog
import com.hisabpro.app.ui.components.AnalyticsView
import com.hisabpro.app.ui.components.TransactionCard
import com.hisabpro.app.ui.theme.Emerald700
import com.hisabpro.app.ui.theme.Emerald800
import com.hisabpro.app.ui.theme.Emerald900
import com.hisabpro.app.ui.theme.ExpenseRed
import com.hisabpro.app.ui.theme.IncomeGreen
import com.hisabpro.app.ui.theme.PureWhite
import com.hisabpro.app.util.ThermalSlipGenerator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HisabApp(
    viewModel: HisabViewModel,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddSheet by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var showAnalytics by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    BackHandler(enabled = onBack != null) {
        onBack?.invoke()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Emerald700,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "₹",
                                    color = PureWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        }
                        Text(
                            text = "Cashbook",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSearchBar = !showSearchBar },
                        modifier = Modifier.testTag("toggle_search_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search transactions"
                        )
                    }
                    IconButton(
                        onClick = { showAnalytics = !showAnalytics },
                        modifier = Modifier.testTag("toggle_analytics_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = "Toggle expense analytics",
                            tint = if (showAnalytics) Emerald700 else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box {
                        IconButton(
                            onClick = { showExportMenu = true },
                            modifier = Modifier.testTag("export_cashbook_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Export Cashbook",
                                tint = Emerald700
                            )
                        }
                        DropdownMenu(
                            expanded = showExportMenu,
                            onDismissRequest = { showExportMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export PDF Cashbook") },
                                onClick = {
                                    showExportMenu = false
                                    viewModel.shareCashbookPdf(context)
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.PictureAsPdf,
                                        contentDescription = null,
                                        tint = ExpenseRed
                                    )
                                },
                                modifier = Modifier.testTag("menu_export_pdf")
                            )
                            DropdownMenuItem(
                                text = { Text("Export CSV Spreadsheet") },
                                onClick = {
                                    showExportMenu = false
                                    viewModel.exportCashbookCsv(context)
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.TableChart,
                                        contentDescription = null,
                                        tint = Emerald700
                                    )
                                },
                                modifier = Modifier.testTag("menu_export_csv")
                            )
                            DropdownMenuItem(
                                text = { Text("Daily Cash Closing (58mm POS)") },
                                onClick = {
                                    showExportMenu = false
                                    ThermalSlipGenerator.shareDailyCashbookThermalSlip(
                                        context = context,
                                        transactions = uiState.filteredTransactions
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Print,
                                        contentDescription = null,
                                        tint = Emerald700
                                    )
                                },
                                modifier = Modifier.testTag("menu_export_thermal_closing")
                            )
                        }
                    }
                    IconButton(
                        onClick = { viewModel.resetToDemo() },
                        modifier = Modifier.testTag("reset_demo_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset demo ledger data"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingTransaction = null
                    scope.launch {
                        showAddSheet = true
                    }
                },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Hisab entry") },
                text = { Text("Add Entry", fontWeight = FontWeight.Bold) },
                containerColor = Emerald700,
                contentColor = PureWhite,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_add_transaction")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Search Bar (if expanded)
            if (showSearchBar) {
                item {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search by description, note, or category...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_text_input")
                    )
                }
            }

            // Hero Balance Card with Cash & Bank Split
            item {
                HeroBalanceCard(
                    balance = uiState.totalBalance,
                    income = uiState.totalIncome,
                    expense = uiState.totalExpense,
                    cashBalance = uiState.totalCashBalance,
                    bankBalance = uiState.totalBankBalance
                )
            }

            // Analytics Section (Collapsible)
            if (showAnalytics && uiState.categoryBreakdown.isNotEmpty()) {
                item {
                    AnalyticsView(categoryStats = uiState.categoryBreakdown)
                }
            }

            // Filter Chips (Date Range, Transaction Type, Payment Mode, Category)
            item {
                FilterSection(
                    selectedFilterType = uiState.filterType,
                    onFilterTypeSelected = { viewModel.setFilterType(it) },
                    selectedDateFilter = uiState.dateFilterType,
                    onDateFilterSelected = { viewModel.setDateFilterType(it) },
                    selectedPaymentMode = uiState.selectedPaymentMode,
                    onPaymentModeSelected = { viewModel.setSelectedPaymentMode(it) },
                    selectedCategory = uiState.selectedCategory,
                    onCategorySelected = { viewModel.setSelectedCategory(it) }
                )
            }

            // Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transactions (${uiState.filteredTransactions.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val isAnyFilterActive = uiState.filterType != FilterType.ALL ||
                            uiState.dateFilterType != DateFilterType.ALL_TIME ||
                            uiState.selectedCategory != null ||
                            uiState.selectedPaymentMode != null

                    if (isAnyFilterActive) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "Filtered",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Transactions list or Empty state
            if (uiState.filteredTransactions.isEmpty()) {
                item {
                    EmptyTransactionsPlaceholder(
                        isFiltered = uiState.searchQuery.isNotEmpty() ||
                                uiState.filterType != FilterType.ALL ||
                                uiState.dateFilterType != DateFilterType.ALL_TIME ||
                                uiState.selectedPaymentMode != null ||
                                uiState.selectedCategory != null
                    )
                }
            } else {
                items(
                    items = uiState.filteredTransactions,
                    key = { it.id }
                ) { transaction ->
                    TransactionCard(
                        transaction = transaction,
                        onDelete = { viewModel.deleteTransaction(it) },
                        onEdit = { tx ->
                            editingTransaction = tx
                            scope.launch {
                                showAddSheet = true
                            }
                        }
                    )
                }
            }
        }
    }

    if (showAddSheet) {
        AddTransactionDialog(
            sheetState = sheetState,
            initialTransaction = editingTransaction,
            onDismiss = {
                showAddSheet = false
                editingTransaction = null
            },
            onSave = { title, amount, type, category, dateMillis, paymentMode, note ->
                val currentEditing = editingTransaction
                if (currentEditing != null) {
                    viewModel.updateTransaction(
                        currentEditing.copy(
                            title = title,
                            amount = amount,
                            type = type,
                            category = category,
                            dateMillis = dateMillis,
                            paymentMode = paymentMode,
                            note = note
                        )
                    )
                } else {
                    viewModel.addTransaction(
                        title = title,
                        amount = amount,
                        type = type,
                        category = category,
                        dateMillis = dateMillis,
                        paymentMode = paymentMode,
                        note = note
                    )
                }
                editingTransaction = null
            }
        )
    }
}

@Composable
private fun HeroBalanceCard(
    balance: Double,
    income: Double,
    expense: Double,
    cashBalance: Double,
    bankBalance: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_balance_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Emerald800, Emerald900)
                    )
                )
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Total Cashbook Balance",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = PureWhite.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(6.dp))

                val balanceSign = if (balance < 0) "- ₹" else "₹"
                val absBalance = kotlin.math.abs(balance)

                Text(
                    text = "$balanceSign${HisabViewModel.formatAmount(absBalance)}",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 34.sp
                    ),
                    color = PureWhite
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Cash in Hand vs Bank Balance Breakdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cash in Hand
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = PureWhite.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = PureWhite.copy(alpha = 0.9f),
                                modifier = Modifier.size(16.dp)
                            )
                            Column {
                                Text(
                                    text = "Cash in Hand",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PureWhite.copy(alpha = 0.75f)
                                )
                                Text(
                                    text = "₹${HisabViewModel.formatAmount(cashBalance)}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = PureWhite
                                )
                            }
                        }
                    }

                    // Bank / Online Balance
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = PureWhite.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = PureWhite.copy(alpha = 0.9f),
                                modifier = Modifier.size(16.dp)
                            )
                            Column {
                                Text(
                                    text = "Bank / UPI",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PureWhite.copy(alpha = 0.75f)
                                )
                                Text(
                                    text = "₹${HisabViewModel.formatAmount(bankBalance)}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = PureWhite
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Inflow and Outflow stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Income Pill
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = PureWhite.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(IncomeGreen.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = IncomeGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Total Inflow",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PureWhite.copy(alpha = 0.75f)
                                )
                                Text(
                                    text = "₹${HisabViewModel.formatAmount(income)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = PureWhite
                                )
                            }
                        }
                    }

                    // Expense Pill
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = PureWhite.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(ExpenseRed.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = ExpenseRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Total Outflow",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PureWhite.copy(alpha = 0.75f)
                                )
                                Text(
                                    text = "₹${HisabViewModel.formatAmount(expense)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = PureWhite
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterSection(
    selectedFilterType: FilterType,
    onFilterTypeSelected: (FilterType) -> Unit,
    selectedDateFilter: DateFilterType,
    onDateFilterSelected: (DateFilterType) -> Unit,
    selectedPaymentMode: PaymentMode?,
    onPaymentModeSelected: (PaymentMode) -> Unit,
    selectedCategory: Category?,
    onCategorySelected: (Category) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Date Range Quick Filters
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DateFilterType.entries.forEach { dateFilter ->
                val isSelected = selectedDateFilter == dateFilter
                FilterChip(
                    selected = isSelected,
                    onClick = { onDateFilterSelected(dateFilter) },
                    label = { Text(dateFilter.label) },
                    modifier = Modifier.testTag("date_filter_${dateFilter.name.lowercase()}")
                )
            }
        }

        // Type Filters (All, Income, Expense)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilterType == FilterType.ALL,
                onClick = { onFilterTypeSelected(FilterType.ALL) },
                label = { Text("All") },
                modifier = Modifier.testTag("filter_chip_all")
            )
            FilterChip(
                selected = selectedFilterType == FilterType.INCOME_ONLY,
                onClick = { onFilterTypeSelected(FilterType.INCOME_ONLY) },
                label = { Text("Income") },
                modifier = Modifier.testTag("filter_chip_income")
            )
            FilterChip(
                selected = selectedFilterType == FilterType.EXPENSE_ONLY,
                onClick = { onFilterTypeSelected(FilterType.EXPENSE_ONLY) },
                label = { Text("Expense") },
                modifier = Modifier.testTag("filter_chip_expense")
            )
        }

        // Payment Mode Filter Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PaymentMode.entries.forEach { mode ->
                val isSelected = selectedPaymentMode == mode
                FilterChip(
                    selected = isSelected,
                    onClick = { onPaymentModeSelected(mode) },
                    label = { Text(mode.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier.testTag("filter_mode_${mode.name.lowercase()}")
                )
            }
        }

        // Category Filter row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Category.entries.forEach { category ->
                val isSelected = selectedCategory == category
                FilterChip(
                    selected = isSelected,
                    onClick = { onCategorySelected(category) },
                    label = { Text(category.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    }
}

@Composable
private fun EmptyTransactionsPlaceholder(isFiltered: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
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
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isFiltered) "No Matching Entries" else "No Transactions Yet",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isFiltered) {
                    "Try clearing your search query or filters."
                } else {
                    "Tap '+ Add Entry' below to record your first income or expense."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
