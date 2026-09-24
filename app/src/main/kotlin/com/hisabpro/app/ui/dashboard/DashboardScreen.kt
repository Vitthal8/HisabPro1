package com.hisabpro.app.ui.dashboard

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisabpro.app.data.model.BusinessProfile
import com.hisabpro.app.data.model.Invoice
import com.hisabpro.app.data.model.InvoiceStatus
import com.hisabpro.app.data.model.InvoiceType
import com.hisabpro.app.data.model.Item
import com.hisabpro.app.data.model.Party
import com.hisabpro.app.data.model.PartyTag
import com.hisabpro.app.data.model.PartyType
import com.hisabpro.app.data.model.PaymentMode
import com.hisabpro.app.data.model.PurchaseBill
import com.hisabpro.app.data.model.Transaction
import com.hisabpro.app.data.model.TransactionType
import com.hisabpro.app.ui.components.AddTransactionDialog
import com.hisabpro.app.ui.party.AddPartyDialog
import com.hisabpro.app.ui.payments.PaymentDirection
import com.hisabpro.app.ui.payments.PaymentRecordData
import com.hisabpro.app.ui.payments.RecordPaymentSheet
import com.hisabpro.app.ui.purchases.CreatePurchaseSheet
import com.hisabpro.app.ui.sales.CreateInvoiceSheet
import com.hisabpro.app.ui.sales.SaveAction
import com.hisabpro.app.ui.theme.Amber700
import com.hisabpro.app.ui.theme.DeepNavyBlue
import com.hisabpro.app.ui.theme.DeepNavyLight
import com.hisabpro.app.ui.theme.Emerald700
import com.hisabpro.app.ui.theme.ExpenseRed
import com.hisabpro.app.ui.theme.ExpenseRedContainer
import com.hisabpro.app.ui.theme.IncomeGreen
import com.hisabpro.app.ui.theme.IncomeGreenContainer
import com.hisabpro.app.ui.theme.PureWhite
import com.hisabpro.app.ui.theme.SaffronDark
import com.hisabpro.app.ui.theme.SaffronLight
import com.hisabpro.app.ui.theme.SaffronOrange
import com.hisabpro.app.ui.theme.Slate100
import com.hisabpro.app.ui.theme.Slate200
import com.hisabpro.app.ui.theme.Slate50
import com.hisabpro.app.ui.theme.Slate500
import com.hisabpro.app.ui.theme.Slate700
import com.hisabpro.app.ui.theme.Slate800
import com.hisabpro.app.ui.theme.Slate900
import com.hisabpro.app.util.IndianAccountingFormat
import java.util.Calendar
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    profile: BusinessProfile,
    invoices: List<Invoice>,
    totalReceivablesDr: Double,
    totalPayablesCr: Double,
    cashInHand: Double,
    bankBalance: Double,
    items: List<Item>,
    transactions: List<Transaction> = emptyList(),
    parties: List<Party> = emptyList(),
    onNewSaleClick: () -> Unit,
    onRecordPaymentClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onAddPartyClick: () -> Unit,
    onAddPurchaseClick: () -> Unit,
    onDaybookClick: () -> Unit,
    onCashbookClick: () -> Unit,
    onViewAllSalesClick: () -> Unit,
    onViewAllPartiesClick: () -> Unit,
    onInvoiceClick: (Invoice) -> Unit,
    onItemClick: (Item) -> Unit,
    onSetupBusinessClick: () -> Unit,
    onSaveInvoice: ((Invoice, SaveAction) -> Unit)? = null,
    onSavePayment: ((PaymentRecordData) -> Unit)? = null,
    onSaveParty: ((name: String, phone: String, address: String, gstin: String, type: PartyType, tag: PartyTag) -> Unit)? = null,
    onSaveExpense: ((Transaction) -> Unit)? = null,
    onSavePurchase: ((PurchaseBill) -> Unit)? = null,
    nextPurchaseNumber: String = "PUR-001",
    modifier: Modifier = Modifier
) {
    // -------------------------------------------------------------
    // DATE & TIMESTAMPS
    // -------------------------------------------------------------
    val todayBounds = remember {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val end = cal.timeInMillis
        Pair(start, end)
    }
    val todayStart = todayBounds.first
    val todayEnd = todayBounds.second

    // -------------------------------------------------------------
    // TODAY'S REAL DATABASE FIGURES (Strictly computed from DB)
    // -------------------------------------------------------------
    // 1. Today's Sales
    val todayInvoices = remember(invoices, todayStart, todayEnd) {
        invoices.filter { it.dateMillis in todayStart until todayEnd && it.type != InvoiceType.PROFORMA }
    }
    val todaySalesAmount = remember(todayInvoices) {
        todayInvoices.sumOf { it.grandTotal }
    }
    val todaySalesCount = todayInvoices.size

    // 2. Today's Collections (Payments Received today)
    val todayIncomeTransactions = remember(transactions, todayStart, todayEnd) {
        transactions.filter { it.type == TransactionType.INCOME && it.dateMillis in todayStart until todayEnd }
    }
    val todayCollectionsAmount = remember(todayIncomeTransactions, todayInvoices) {
        val txCollections = todayIncomeTransactions.sumOf { it.amount }
        val invoiceCollections = todayInvoices.sumOf { it.paidAmount }
        maxOf(txCollections, invoiceCollections)
    }
    val todayCollectionsCount = remember(todayIncomeTransactions, todayInvoices) {
        maxOf(todayIncomeTransactions.size, todayInvoices.count { it.paidAmount > 0 })
    }

    // 3. Today's Expenses
    val todayExpenseTransactions = remember(transactions, todayStart, todayEnd) {
        transactions.filter { it.type == TransactionType.EXPENSE && it.dateMillis in todayStart until todayEnd }
    }
    val todayExpensesAmount = remember(todayExpenseTransactions) {
        todayExpenseTransactions.sumOf { it.amount }
    }
    val todayExpensesCount = todayExpenseTransactions.size

    // 4. Outstanding
    val netOutstanding = totalReceivablesDr - totalPayablesCr

    // -------------------------------------------------------------
    // PENDING INVOICES
    // -------------------------------------------------------------
    val pendingInvoices = remember(invoices) {
        invoices.filter {
            (it.paymentStatus == InvoiceStatus.UNPAID || it.paymentStatus == InvoiceStatus.PARTIAL) &&
                    it.type != InvoiceType.PROFORMA
        }.sortedByDescending { it.dateMillis }
    }
    val totalPendingDue = remember(pendingInvoices) {
        pendingInvoices.sumOf { it.dueAmount }
    }

    // -------------------------------------------------------------
    // LOW STOCK ITEMS
    // -------------------------------------------------------------
    val lowStockItems = remember(items) {
        items.filter { it.isLowStock || it.isOutOfStock }
    }

    // -------------------------------------------------------------
    // RECENT TRANSACTIONS
    // -------------------------------------------------------------
    val recentTransactions = remember(transactions, invoices) {
        if (transactions.isNotEmpty()) {
            transactions.sortedByDescending { it.dateMillis }.take(6)
        } else {
            // Fallback to recent invoices converted for display
            invoices.sortedByDescending { it.dateMillis }.take(6).map { inv ->
                Transaction(
                    id = inv.id,
                    title = "Sale - ${inv.customerName}",
                    amount = inv.grandTotal,
                    type = TransactionType.INCOME,
                    category = com.hisabpro.app.data.model.Category.OTHER,
                    dateMillis = inv.dateMillis,
                    paymentMode = PaymentMode.CASH,
                    note = inv.invoiceNumber
                )
            }
        }
    }

    // -------------------------------------------------------------
    // BOTTOM SHEET CONTROLS FOR ONE-TAP QUICK ACTIONS
    // -------------------------------------------------------------
    var showAddSaleSheet by remember { mutableStateOf(false) }
    var showReceivePaymentSheet by remember { mutableStateOf(false) }
    var showAddCustomerSheet by remember { mutableStateOf(false) }
    var showAddExpenseSheet by remember { mutableStateOf(false) }
    var showAddPurchaseSheet by remember { mutableStateOf(false) }
    var quickPayPartyId by remember { mutableStateOf<String?>(null) }

    val saleSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val paymentSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val partySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val expenseSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val purchaseSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // =========================================================
        // 1. BUSINESS HEADER & FY
        // =========================================================
        item {
            BusinessHeaderCard(
                profile = profile,
                onSettingsClick = onSetupBusinessClick
            )
        }

        // =========================================================
        // 2. TODAY'S 4 CORE FIGURES
        // =========================================================
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today's Overview",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Slate900
                    )
                    Text(
                        text = IndianAccountingFormat.formatIndianDate(System.currentTimeMillis()),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = Slate500
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2x2 Grid of Today's Numbers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Today's Sales
                    TodayMetricCard(
                        title = "Today's Sales",
                        amountText = IndianAccountingFormat.formatIndianCurrency(todaySalesAmount),
                        subtext = if (todaySalesCount > 0) "$todaySalesCount bills generated" else "0 bills today",
                        icon = Icons.Default.ReceiptLong,
                        accentColor = SaffronOrange,
                        containerColor = SaffronLight,
                        testTag = "metric_today_sales",
                        modifier = Modifier.weight(1f)
                    )

                    // Today's Collections
                    TodayMetricCard(
                        title = "Today's Collections",
                        amountText = IndianAccountingFormat.formatIndianCurrency(todayCollectionsAmount),
                        subtext = if (todayCollectionsCount > 0) "$todayCollectionsCount payments in" else "No collections yet",
                        icon = Icons.Default.Payments,
                        accentColor = IncomeGreen,
                        containerColor = IncomeGreenContainer,
                        testTag = "metric_today_collections",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Today's Expenses
                    TodayMetricCard(
                        title = "Today's Expenses",
                        amountText = IndianAccountingFormat.formatIndianCurrency(todayExpensesAmount),
                        subtext = if (todayExpensesCount > 0) "$todayExpensesCount vouchers" else "₹0 spent today",
                        icon = Icons.Default.MoneyOff,
                        accentColor = ExpenseRed,
                        containerColor = ExpenseRedContainer,
                        testTag = "metric_today_expenses",
                        modifier = Modifier.weight(1f)
                    )

                    // Outstanding (Receivables & Payables)
                    OutstandingMetricCard(
                        receivablesDr = totalReceivablesDr,
                        payablesCr = totalPayablesCr,
                        netOutstanding = netOutstanding,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // =========================================================
        // 3. QUICK ACTIONS (Add Sale, Receive Payment, Add Customer, Add Expense, Add Purchase)
        // =========================================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Slate200)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Quick Actions",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Slate800
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Add Sale
                        QuickActionItem(
                            icon = Icons.Default.ReceiptLong,
                            label = "Add Sale",
                            accentColor = SaffronOrange,
                            testTag = "btn_quick_add_sale",
                            onClick = {
                                if (onSaveInvoice != null) showAddSaleSheet = true
                                else onNewSaleClick()
                            }
                        )

                        // 2. Receive Payment
                        QuickActionItem(
                            icon = Icons.Default.Payments,
                            label = "Receive Payment",
                            accentColor = IncomeGreen,
                            testTag = "btn_quick_receive_payment",
                            onClick = {
                                if (onSavePayment != null) showReceivePaymentSheet = true
                                else onRecordPaymentClick()
                            }
                        )

                        // 3. Add Customer
                        QuickActionItem(
                            icon = Icons.Default.PersonAdd,
                            label = "Add Customer",
                            accentColor = DeepNavyBlue,
                            testTag = "btn_quick_add_customer",
                            onClick = {
                                if (onSaveParty != null) showAddCustomerSheet = true
                                else onAddPartyClick()
                            }
                        )

                        // 4. Add Expense
                        QuickActionItem(
                            icon = Icons.Default.MoneyOff,
                            label = "Add Expense",
                            accentColor = ExpenseRed,
                            testTag = "btn_quick_add_expense",
                            onClick = {
                                if (onSaveExpense != null) showAddExpenseSheet = true
                                else onAddExpenseClick()
                            }
                        )

                        // 5. Add Purchase
                        QuickActionItem(
                            icon = Icons.Default.ShoppingBag,
                            label = "Add Purchase",
                            accentColor = Emerald700,
                            testTag = "btn_quick_add_purchase",
                            onClick = {
                                if (onSavePurchase != null) showAddPurchaseSheet = true
                                else onAddPurchaseClick()
                            }
                        )
                    }
                }
            }
        }

        // =========================================================
        // 4. CASH & BANK / UPI BALANCES
        // =========================================================
        item {
            CashAndBankCard(
                cashInHand = cashInHand,
                bankBalance = bankBalance,
                onCashbookClick = onCashbookClick
            )
        }

        // =========================================================
        // 5. PENDING INVOICES SECTION
        // =========================================================
        item {
            PendingInvoicesSection(
                pendingInvoices = pendingInvoices,
                totalPendingDue = totalPendingDue,
                onInvoiceClick = onInvoiceClick,
                onViewAllSalesClick = onViewAllSalesClick,
                onReceivePaymentForInvoice = { invoice ->
                    quickPayPartyId = invoice.customerId
                    if (onSavePayment != null) {
                        showReceivePaymentSheet = true
                    } else {
                        onRecordPaymentClick()
                    }
                }
            )
        }

        // =========================================================
        // 6. LOW-STOCK ITEMS ALERT
        // =========================================================
        item {
            LowStockSection(
                lowStockItems = lowStockItems,
                totalItemsCount = items.size,
                onItemClick = onItemClick,
                onReorderClick = {
                    if (onSavePurchase != null) showAddPurchaseSheet = true
                    else onAddPurchaseClick()
                }
            )
        }

        // =========================================================
        // 7. RECENT TRANSACTIONS FEED
        // =========================================================
        item {
            RecentTransactionsSection(
                transactions = recentTransactions,
                onDaybookClick = onDaybookClick
            )
        }
    }

    // =============================================================
    // MODAL BOTTOM SHEETS FOR QUICK ACTIONS
    // =============================================================
    // 1. Add Sale Sheet
    if (showAddSaleSheet && onSaveInvoice != null) {
        CreateInvoiceSheet(
            sheetState = saleSheetState,
            parties = parties,
            availableItems = items,
            initialInvoiceType = if (profile.isGstRegistered) InvoiceType.TAX_INVOICE else InvoiceType.NON_GST_BILL,
            invoiceToEdit = null,
            onDismiss = { showAddSaleSheet = false },
            onSaveInvoice = { inv, action ->
                onSaveInvoice(inv, action)
                showAddSaleSheet = false
            }
        )
    }

    // 2. Receive Payment Sheet
    if (showReceivePaymentSheet && onSavePayment != null) {
        RecordPaymentSheet(
            parties = parties,
            initialDirection = PaymentDirection.RECEIPT_IN,
            initialPartyId = quickPayPartyId,
            merchantUpiId = profile.upiId,
            merchantName = profile.shopName.ifBlank { "HisabPro Merchant" },
            sheetState = paymentSheetState,
            onDismiss = {
                showReceivePaymentSheet = false
                quickPayPartyId = null
            },
            onSavePayment = { record ->
                onSavePayment(record)
                showReceivePaymentSheet = false
                quickPayPartyId = null
            }
        )
    }

    // 3. Add Customer Sheet
    if (showAddCustomerSheet && onSaveParty != null) {
        AddPartyDialog(
            sheetState = partySheetState,
            onDismiss = { showAddCustomerSheet = false },
            onSave = { name, phone, address, gstin, type, tag ->
                onSaveParty(name, phone, address, gstin, type, tag)
                showAddCustomerSheet = false
            }
        )
    }

    // 4. Add Expense Sheet
    if (showAddExpenseSheet && onSaveExpense != null) {
        AddTransactionDialog(
            sheetState = expenseSheetState,
            initialTransaction = null,
            onDismiss = { showAddExpenseSheet = false },
            onSave = { title, amount, type, category, dateMillis, paymentMode, note ->
                onSaveExpense(
                    Transaction(
                        id = UUID.randomUUID().toString(),
                        title = title,
                        amount = amount,
                        type = type,
                        category = category,
                        dateMillis = dateMillis,
                        paymentMode = paymentMode,
                        note = note
                    )
                )
                showAddExpenseSheet = false
            }
        )
    }

    // 5. Add Purchase Sheet
    if (showAddPurchaseSheet && onSavePurchase != null) {
        CreatePurchaseSheet(
            sheetState = purchaseSheetState,
            suppliers = parties,
            inventoryItems = items,
            nextPurchaseNumber = nextPurchaseNumber,
            onDismiss = { showAddPurchaseSheet = false },
            onSavePurchase = { bill ->
                onSavePurchase(bill)
                showAddPurchaseSheet = false
            }
        )
    }
}

// =================================================================
// SUBCOMPONENTS
// =================================================================

@Composable
private fun BusinessHeaderCard(
    profile: BusinessProfile,
    onSettingsClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DeepNavyBlue,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.shopName.ifBlank { "My Business" },
                        color = PureWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (profile.isGstRegistered) Color(0xFF2E7D32) else Slate700
                        ) {
                            Text(
                                text = if (profile.isGstRegistered) "GST Active: ${profile.gstin}" else "Non-GST Business",
                                color = PureWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = "FY ${IndianAccountingFormat.getFinancialYear()}",
                            color = Color(0xFFE0E0E0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.testTag("btn_dashboard_settings")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Business Settings",
                        tint = PureWhite
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayMetricCard(
    title: String,
    amountText: String,
    subtext: String,
    icon: ImageVector,
    accentColor: Color,
    containerColor: Color,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Slate200)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = Slate700
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(containerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = amountText,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Slate900,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall,
                color = Slate500,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun OutstandingMetricCard(
    receivablesDr: Double,
    payablesCr: Double,
    netOutstanding: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag("metric_today_outstanding"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Slate200)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Outstanding",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = Slate700
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(DeepNavyLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PointOfSale,
                        contentDescription = null,
                        tint = DeepNavyBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = IndianAccountingFormat.formatIndianCurrency(kotlin.math.abs(netOutstanding)),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (netOutstanding >= 0) IncomeGreen else ExpenseRed,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (netOutstanding >= 0) "Dr" else "Cr",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (netOutstanding >= 0) IncomeGreen else ExpenseRed
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Dr: ${IndianAccountingFormat.formatIndianCurrency(receivablesDr)} | Cr: ${IndianAccountingFormat.formatIndianCurrency(payablesCr)}",
                style = MaterialTheme.typography.labelSmall,
                color = Slate500,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun QuickActionItem(
    icon: ImageVector,
    label: String,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(vertical = 4.dp, horizontal = 2.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = accentColor.copy(alpha = 0.12f),
            modifier = Modifier.size(52.dp) // Minimum 48dp touch target
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accentColor,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Slate800,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CashAndBankCard(
    cashInHand: Double,
    bankBalance: Double,
    onCashbookClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onCashbookClick)
            .testTag("card_cash_and_bank"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Slate200)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Account Balances",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Slate900
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Cashbook",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Emerald700
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Open Cashbook",
                        tint = Emerald700,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cash In Hand
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Slate50,
                    border = BorderStroke(1.dp, Slate200),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(IncomeGreenContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PointOfSale,
                                contentDescription = null,
                                tint = IncomeGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Cash in Hand",
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate500
                            )
                            Text(
                                text = IndianAccountingFormat.formatIndianCurrency(cashInHand),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Slate900
                            )
                        }
                    }
                }

                // Bank / UPI Balance
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Slate50,
                    border = BorderStroke(1.dp, Slate200),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(DeepNavyLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = DeepNavyBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Bank & UPI",
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate500
                            )
                            Text(
                                text = IndianAccountingFormat.formatIndianCurrency(bankBalance),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Slate900
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingInvoicesSection(
    pendingInvoices: List<Invoice>,
    totalPendingDue: Double,
    onInvoiceClick: (Invoice) -> Unit,
    onViewAllSalesClick: () -> Unit,
    onReceivePaymentForInvoice: (Invoice) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("section_pending_invoices"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Slate200)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Pending Invoices",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Slate900
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (pendingInvoices.isEmpty()) IncomeGreenContainer else ExpenseRedContainer
                    ) {
                        Text(
                            text = if (pendingInvoices.isEmpty()) "All Settled" else "${pendingInvoices.size} Pending",
                            color = if (pendingInvoices.isEmpty()) IncomeGreen else ExpenseRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                if (pendingInvoices.isNotEmpty()) {
                    Text(
                        text = "Due: ${IndianAccountingFormat.formatIndianCurrency(totalPendingDue)}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = ExpenseRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (pendingInvoices.isEmpty()) {
                // Graceful Empty State
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Slate50, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = IncomeGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "All invoices are settled!",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Slate800
                        )
                        Text(
                            text = "No pending dues from customers right now.",
                            fontSize = 11.sp,
                            color = Slate500
                        )
                    }
                }
            } else {
                // Show up to 3 urgent pending invoices
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    pendingInvoices.take(3).forEach { invoice ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Slate50,
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onInvoiceClick(invoice) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = invoice.customerName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Slate900,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${invoice.invoiceNumber} • ${IndianAccountingFormat.formatIndianDate(invoice.dateMillis)}",
                                        fontSize = 11.sp,
                                        color = Slate500
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${IndianAccountingFormat.formatIndianCurrency(invoice.dueAmount)} Dr",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = ExpenseRed
                                        )
                                        Text(
                                            text = if (invoice.paymentStatus == InvoiceStatus.PARTIAL) "Partial" else "Unpaid",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (invoice.paymentStatus == InvoiceStatus.PARTIAL) Amber700 else ExpenseRed
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { onReceivePaymentForInvoice(invoice) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, IncomeGreen),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text(
                                            text = "Collect",
                                            color = IncomeGreen,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onViewAllSalesClick),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "View All Bills (${pendingInvoices.size} pending)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Emerald700
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = Emerald700,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LowStockSection(
    lowStockItems: List<Item>,
    totalItemsCount: Int,
    onItemClick: (Item) -> Unit,
    onReorderClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("section_low_stock"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (lowStockItems.isNotEmpty()) ExpenseRed.copy(alpha = 0.4f) else Slate200
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Inventory & Stock Alerts",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Slate900
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (lowStockItems.isEmpty()) IncomeGreenContainer else ExpenseRedContainer
                    ) {
                        Text(
                            text = if (lowStockItems.isEmpty()) "Stock Healthy" else "${lowStockItems.size} Alerts",
                            color = if (lowStockItems.isEmpty()) IncomeGreen else ExpenseRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                if (lowStockItems.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onReorderClick,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Emerald700),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(
                            text = "+ Purchase",
                            color = Emerald700,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (lowStockItems.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Slate50, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = IncomeGreen,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = if (totalItemsCount > 0) "All $totalItemsCount items have adequate stock." else "No inventory items added yet.",
                        fontSize = 12.sp,
                        color = Slate700
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    lowStockItems.take(3).forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Slate50,
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onItemClick(item) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (item.isOutOfStock) ExpenseRed else Amber700,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column {
                                        Text(
                                            text = item.name,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = Slate900
                                        )
                                        Text(
                                            text = "Min threshold: ${item.minStockAlert.toInt()} ${item.unit}",
                                            fontSize = 11.sp,
                                            color = Slate500
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (item.isOutOfStock) ExpenseRedContainer else SaffronLight
                                ) {
                                    Text(
                                        text = if (item.isOutOfStock) "Out of Stock" else "${item.currentStock.toInt()} ${item.unit} left",
                                        color = if (item.isOutOfStock) ExpenseRed else SaffronDark,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentTransactionsSection(
    transactions: List<Transaction>,
    onDaybookClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("section_recent_transactions"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Slate200)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = DeepNavyBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Slate900
                    )
                }

                Row(
                    modifier = Modifier.clickable(onClick = onDaybookClick),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Day Book",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = DeepNavyBlue
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Open Day Book",
                        tint = DeepNavyBlue,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (transactions.isEmpty()) {
                // Graceful Empty State
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Slate50, RoundedCornerShape(12.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recent transactions recorded yet.\nUse quick actions above to record sales or expenses.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    transactions.take(5).forEach { tx ->
                        val isIncome = tx.type == TransactionType.INCOME
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Slate50,
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(if (isIncome) IncomeGreenContainer else ExpenseRedContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                            contentDescription = null,
                                            tint = if (isIncome) IncomeGreen else ExpenseRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = tx.title,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = Slate900,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = IndianAccountingFormat.formatIndianDate(tx.dateMillis),
                                                fontSize = 11.sp,
                                                color = Slate500
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Slate200
                                            ) {
                                                Text(
                                                    text = tx.paymentMode.name,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Slate700,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Text(
                                    text = (if (isIncome) "+ " else "- ") + IndianAccountingFormat.formatIndianCurrency(tx.amount),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isIncome) IncomeGreen else ExpenseRed
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
