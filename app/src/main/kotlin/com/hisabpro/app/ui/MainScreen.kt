package com.hisabpro.app.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hisabpro.app.R
import com.hisabpro.app.data.repository.SettingsRepository
import com.hisabpro.app.ui.dashboard.DashboardScreen
import com.hisabpro.app.ui.items.ItemViewModel
import com.hisabpro.app.ui.items.ItemsScreen
import com.hisabpro.app.ui.more.MoreScreen
import com.hisabpro.app.ui.onboarding.BusinessSetupScreen
import com.hisabpro.app.ui.party.PartiesListScreen
import com.hisabpro.app.ui.party.PartyViewModel
import com.hisabpro.app.ui.purchases.PurchaseViewModel
import com.hisabpro.app.ui.reports.ReportsScreen
import com.hisabpro.app.ui.reports.ReportsViewModel
import com.hisabpro.app.ui.sales.InvoiceViewModel
import com.hisabpro.app.ui.sales.SalesScreen
import com.hisabpro.app.ui.theme.DeepNavyBlue
import com.hisabpro.app.ui.theme.SaffronOrange
import java.util.Locale

@Composable
fun MainScreen(
    partyViewModel: PartyViewModel,
    hisabViewModel: HisabViewModel,
    invoiceViewModel: InvoiceViewModel,
    itemViewModel: ItemViewModel,
    reportsViewModel: ReportsViewModel,
    purchaseViewModel: PurchaseViewModel? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settingsRepo = remember { SettingsRepository.getInstance(context) }
    val businessProfile by settingsRepo.profile.collectAsStateWithLifecycle()

    val currentLang = businessProfile.appLanguage
    val targetLocale = remember(currentLang) {
        when (currentLang) {
            "hi" -> Locale("hi")
            "mr" -> Locale("mr")
            else -> Locale("en")
        }
    }
    val currentConfig = LocalConfiguration.current
    val updatedConfig = remember(currentConfig, targetLocale) {
        Configuration(currentConfig).apply {
            setLocale(targetLocale)
        }
    }
    val localizedContext = remember(context, targetLocale) {
        val config = Configuration(context.resources.configuration)
        config.setLocale(targetLocale)
        context.createConfigurationContext(config)
    }

    CompositionLocalProvider(
        LocalConfiguration provides updatedConfig,
        LocalContext provides localizedContext
    ) {
        MainScreenContent(
            businessProfile = businessProfile,
            partyViewModel = partyViewModel,
            hisabViewModel = hisabViewModel,
            invoiceViewModel = invoiceViewModel,
            itemViewModel = itemViewModel,
            reportsViewModel = reportsViewModel,
            purchaseViewModel = purchaseViewModel,
            onSaveProfile = { updated -> settingsRepo.saveProfile(updated) },
            modifier = modifier
        )
    }
}

@Composable
private fun MainScreenContent(
    businessProfile: com.hisabpro.app.data.model.BusinessProfile,
    partyViewModel: PartyViewModel,
    hisabViewModel: HisabViewModel,
    invoiceViewModel: InvoiceViewModel,
    itemViewModel: ItemViewModel,
    reportsViewModel: ReportsViewModel,
    purchaseViewModel: PurchaseViewModel?,
    onSaveProfile: (com.hisabpro.app.data.model.BusinessProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val salesUiState by invoiceViewModel.uiState.collectAsStateWithLifecycle()
    val partyUiState by partyViewModel.uiState.collectAsStateWithLifecycle()
    val items by itemViewModel.rawItems.collectAsStateWithLifecycle()
    val hisabUiState by hisabViewModel.uiState.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showBusinessSetup by rememberSaveable { mutableStateOf(!businessProfile.hasCompletedOnboarding) }
    var activeSubScreen by rememberSaveable { mutableStateOf<String?>(null) } // "items", "cashbook"

    if (showBusinessSetup) {
        BusinessSetupScreen(
            currentProfile = businessProfile,
            isInitialOnboarding = !businessProfile.hasCompletedOnboarding,
            onSaveProfile = { updated ->
                onSaveProfile(updated)
                showBusinessSetup = false
            },
            onDismiss = if (businessProfile.hasCompletedOnboarding) {
                { showBusinessSetup = false }
            } else null
        )
        return
    }

    // Sub-screens opened from "More"
    if (activeSubScreen == "items") {
        Box(modifier = Modifier.fillMaxSize()) {
            ItemsScreen(
                viewModel = itemViewModel,
                onBack = { activeSubScreen = null }
            )
        }
        return
    }

    if (activeSubScreen == "cashbook") {
        Box(modifier = Modifier.fillMaxSize()) {
            HisabApp(
                viewModel = hisabViewModel,
                onBack = { activeSubScreen = null }
            )
        }
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                tonalElevation = 6.dp
            ) {
                // Tab 0: Home / Dashboard
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = stringResource(R.string.nav_home)
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.nav_home),
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = SaffronOrange.copy(alpha = 0.15f),
                        selectedIconColor = SaffronOrange,
                        selectedTextColor = SaffronOrange
                    ),
                    modifier = Modifier.testTag("nav_tab_home")
                )

                // Tab 1: Sales & Invoicing
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = stringResource(R.string.nav_sales)
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.nav_sales),
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = SaffronOrange.copy(alpha = 0.15f),
                        selectedIconColor = SaffronOrange,
                        selectedTextColor = SaffronOrange
                    ),
                    modifier = Modifier.testTag("nav_tab_sales")
                )

                // Tab 2: Parties & Khata Ledger
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = stringResource(R.string.nav_parties)
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.nav_parties),
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = SaffronOrange.copy(alpha = 0.15f),
                        selectedIconColor = SaffronOrange,
                        selectedTextColor = SaffronOrange
                    ),
                    modifier = Modifier.testTag("nav_tab_parties")
                )

                // Tab 3: Reports (Day Book, Sales Summary, Outstanding)
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = stringResource(R.string.nav_reports)
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.nav_reports),
                            fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = SaffronOrange.copy(alpha = 0.15f),
                        selectedIconColor = SaffronOrange,
                        selectedTextColor = SaffronOrange
                    ),
                    modifier = Modifier.testTag("nav_tab_reports")
                )

                // Tab 4: More (Settings, Masters, Printer, Pricing)
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = stringResource(R.string.nav_more)
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.nav_more),
                            fontWeight = if (selectedTab == 4) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = DeepNavyBlue.copy(alpha = 0.15f),
                        selectedIconColor = DeepNavyBlue,
                        selectedTextColor = DeepNavyBlue
                    ),
                    modifier = Modifier.testTag("nav_tab_more")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> DashboardScreen(
                    profile = businessProfile,
                    invoices = salesUiState.invoices,
                    totalReceivablesDr = partyUiState.totalReceivable,
                    totalPayablesCr = partyUiState.totalPayable,
                    cashInHand = hisabUiState.totalCashBalance,
                    bankBalance = hisabUiState.totalBankBalance,
                    items = items,
                    onNewSaleClick = { selectedTab = 1 },
                    onRecordPaymentClick = { selectedTab = 2 },
                    onAddExpenseClick = { activeSubScreen = "cashbook" },
                    onAddPartyClick = { selectedTab = 2 },
                    onDaybookClick = { selectedTab = 3 },
                    onCashbookClick = { activeSubScreen = "cashbook" },
                    onViewAllSalesClick = { selectedTab = 1 },
                    onViewAllPartiesClick = { selectedTab = 2 },
                    onInvoiceClick = { selectedTab = 1 },
                    onItemClick = { activeSubScreen = "items" },
                    onSetupBusinessClick = { showBusinessSetup = true }
                )
                1 -> SalesScreen(
                    viewModel = invoiceViewModel,
                    itemViewModel = itemViewModel,
                    purchaseViewModel = purchaseViewModel
                )
                2 -> PartiesListScreen(viewModel = partyViewModel)
                3 -> ReportsScreen(viewModel = reportsViewModel)
                4 -> MoreScreen(
                    profile = businessProfile,
                    onOpenBusinessSetup = { showBusinessSetup = true },
                    onOpenItems = { activeSubScreen = "items" },
                    onOpenCashbook = { activeSubScreen = "cashbook" },
                    onUpdateProfile = { updated -> onSaveProfile(updated) }
                )
            }
        }
    }
}

