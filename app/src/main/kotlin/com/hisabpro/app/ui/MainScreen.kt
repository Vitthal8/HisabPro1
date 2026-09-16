package com.hisabpro.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisabpro.app.ui.items.ItemViewModel
import com.hisabpro.app.ui.items.ItemsScreen
import com.hisabpro.app.ui.party.PartiesListScreen
import com.hisabpro.app.ui.party.PartyViewModel
import com.hisabpro.app.ui.purchases.PurchaseViewModel
import com.hisabpro.app.ui.reports.ReportsScreen
import com.hisabpro.app.ui.reports.ReportsViewModel
import com.hisabpro.app.ui.sales.InvoiceViewModel
import com.hisabpro.app.ui.sales.SalesScreen
import com.hisabpro.app.ui.theme.Emerald700
import com.hisabpro.app.ui.theme.PureWhite

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
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                tonalElevation = 6.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = "Sales & Invoicing"
                        )
                    },
                    label = {
                        Text(
                            text = "Sales",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Emerald700.copy(alpha = 0.15f),
                        selectedIconColor = Emerald700,
                        selectedTextColor = Emerald700
                    ),
                    modifier = Modifier.testTag("nav_tab_sales")
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = "Parties and Khata"
                        )
                    },
                    label = {
                        Text(
                            text = "Parties",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Emerald700.copy(alpha = 0.15f),
                        selectedIconColor = Emerald700,
                        selectedTextColor = Emerald700
                    ),
                    modifier = Modifier.testTag("nav_tab_parties")
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = "Items & Stock"
                        )
                    },
                    label = {
                        Text(
                            text = "Items",
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Emerald700.copy(alpha = 0.15f),
                        selectedIconColor = Emerald700,
                        selectedTextColor = Emerald700
                    ),
                    modifier = Modifier.testTag("nav_tab_items")
                )

                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "Cashbook & Expenses"
                        )
                    },
                    label = {
                        Text(
                            text = "Cashbook",
                            fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Emerald700.copy(alpha = 0.15f),
                        selectedIconColor = Emerald700,
                        selectedTextColor = Emerald700
                    ),
                    modifier = Modifier.testTag("nav_tab_cashbook")
                )

                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = "Reports and GST"
                        )
                    },
                    label = {
                        Text(
                            text = "Reports",
                            fontWeight = if (selectedTab == 4) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Emerald700.copy(alpha = 0.15f),
                        selectedIconColor = Emerald700,
                        selectedTextColor = Emerald700
                    ),
                    modifier = Modifier.testTag("nav_tab_reports")
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
                0 -> SalesScreen(
                    viewModel = invoiceViewModel,
                    itemViewModel = itemViewModel,
                    purchaseViewModel = purchaseViewModel
                )
                1 -> PartiesListScreen(viewModel = partyViewModel)
                2 -> ItemsScreen(viewModel = itemViewModel)
                3 -> HisabApp(viewModel = hisabViewModel)
                4 -> ReportsScreen(viewModel = reportsViewModel)
            }
        }
    }
}
