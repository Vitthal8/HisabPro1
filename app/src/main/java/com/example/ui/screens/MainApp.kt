package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.AccountingViewModel
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.Saffron
import com.example.util.AppLanguage

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Home", Icons.Default.Home)
    object Sales : Screen("sales", "Sales", Icons.Default.Receipt)
    object Parties : Screen("parties", "Parties", Icons.Default.People)
    object Reports : Screen("reports", "Reports", Icons.Default.Assessment)
    object More : Screen("more", "More", Icons.Default.MoreHoriz)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(viewModel: AccountingViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentLang by viewModel.currentLanguage.collectAsState()

    var showLanguageMenu by remember { mutableStateOf(false) }

    val bottomNavItems = listOf(
        Screen.Dashboard,
        Screen.Sales,
        Screen.Parties,
        Screen.Reports,
        Screen.More
    )

    val isTopLevelDestination = bottomNavItems.any { it.route == currentRoute }

    Scaffold(
        topBar = {
            if (isTopLevelDestination) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Saffron),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "₹",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "HisabPro",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showLanguageMenu = !showLanguageMenu },
                            modifier = Modifier.testTag("language_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = "Language",
                                tint = Color.White
                            )
                        }

                        DropdownMenu(
                            expanded = showLanguageMenu,
                            onDismissRequest = { showLanguageMenu = false }
                        ) {
                            AppLanguage.values().forEach { lang ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = lang.displayName,
                                            fontWeight = if (currentLang == lang.code) FontWeight.Bold else FontWeight.Normal,
                                            color = if (currentLang == lang.code) DeepNavy else Color.Unspecified
                                        )
                                    },
                                    onClick = {
                                        viewModel.setLanguage(lang.code)
                                        showLanguageMenu = false
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepNavy)
                )
            }
        },
        bottomBar = {
            if (isTopLevelDestination) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    bottomNavItems.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(Screen.Dashboard.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title
                                )
                            },
                            label = {
                                Text(
                                    text = when (screen) {
                                        Screen.Dashboard -> viewModel.getString("dashboard")
                                        Screen.Sales -> viewModel.getString("sales")
                                        Screen.Parties -> viewModel.getString("parties")
                                        Screen.Reports -> viewModel.getString("reports")
                                        Screen.More -> "More"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = DeepNavy,
                                selectedTextColor = DeepNavy,
                                indicatorColor = Saffron.copy(alpha = 0.2f),
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray
                            ),
                            modifier = Modifier.testTag("nav_item_${screen.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToAddSale = { navController.navigate("add_sale") },
                    onNavigateToReceivePayment = { navController.navigate("receive_payment") },
                    onNavigateToAddParty = { navController.navigate(Screen.Parties.route) },
                    onNavigateToAddExpense = { navController.navigate(Screen.More.route) },
                    onNavigateToSetup = { navController.navigate("business_setup") },
                    onNavigateToInventory = { navController.navigate("inventory") }
                )
            }

            composable(Screen.Sales.route) {
                SalesScreen(
                    viewModel = viewModel,
                    onNavigateToAddSale = { navController.navigate("add_sale") }
                )
            }

            composable(Screen.Parties.route) {
                PartiesScreen(
                    viewModel = viewModel,
                    onNavigateToReceivePayment = { partyId ->
                        navController.navigate("receive_payment?partyId=$partyId")
                    }
                )
            }

            composable(Screen.Reports.route) {
                ReportsScreen(viewModel = viewModel)
            }

            composable(Screen.More.route) {
                MoreScreen(
                    viewModel = viewModel,
                    onNavigateToBusinessSetup = { navController.navigate("business_setup") },
                    onNavigateToInventory = { navController.navigate("inventory") }
                )
            }

            composable("inventory") {
                ItemsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("add_sale") {
                AddInvoiceScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "receive_payment?partyId={partyId}",
                arguments = listOf(
                    navArgument("partyId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val partyId = backStackEntry.arguments?.getString("partyId")?.toLongOrNull()
                PaymentReceivedScreen(
                    viewModel = viewModel,
                    preselectedPartyId = partyId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("business_setup") {
                BusinessSetupScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
