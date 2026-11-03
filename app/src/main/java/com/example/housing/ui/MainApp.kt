package com.example.housing.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.housing.ui.screens.AccountsScreen
import com.example.housing.ui.screens.AddEditPurchaseScreen
import com.example.housing.ui.screens.DashboardScreen
import com.example.housing.ui.screens.PurchaseDetailsScreen
import com.example.housing.ui.screens.PurchasesScreen
import com.example.housing.ui.screens.SettingsScreen

sealed class Screen {
    data object Dashboard : Screen()
    data object Purchases : Screen()
    data object Accounts : Screen()
    data object Settings : Screen()
    data class PurchaseDetails(val purchaseId: Long) : Screen()
    data class AddEditPurchase(val purchaseIdToEdit: Long?) : Screen()
}

@Composable
fun MainApp(
    viewModel: HousingViewModel = viewModel()
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }

    // Enforce RTL layout throughout the entire application
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val showBottomNav = currentScreen is Screen.Dashboard ||
                currentScreen is Screen.Purchases ||
                currentScreen is Screen.Accounts ||
                currentScreen is Screen.Settings

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (showBottomNav) {
                    NavigationBar(modifier = Modifier.testTag("app_bottom_nav")) {
                        NavigationBarItem(
                            selected = currentScreen is Screen.Dashboard,
                            onClick = { currentScreen = Screen.Dashboard },
                            icon = { Icon(Icons.Default.Dashboard, contentDescription = "الرئيسية") },
                            label = { Text("الرئيسية") },
                            modifier = Modifier.testTag("nav_tab_dashboard")
                        )
                        NavigationBarItem(
                            selected = currentScreen is Screen.Purchases,
                            onClick = { currentScreen = Screen.Purchases },
                            icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "المشتريات") },
                            label = { Text("المشتريات") },
                            modifier = Modifier.testTag("nav_tab_purchases")
                        )
                        NavigationBarItem(
                            selected = currentScreen is Screen.Accounts,
                            onClick = { currentScreen = Screen.Accounts },
                            icon = { Icon(Icons.Default.AccountBalance, contentDescription = "الحسابات") },
                            label = { Text("الحسابات") },
                            modifier = Modifier.testTag("nav_tab_accounts")
                        )
                        NavigationBarItem(
                            selected = currentScreen is Screen.Settings,
                            onClick = { currentScreen = Screen.Settings },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "الإعدادات") },
                            label = { Text("الإعدادات") },
                            modifier = Modifier.testTag("nav_tab_settings")
                        )
                    }
                }
            }
        ) { innerPadding ->
            when (val screen = currentScreen) {
                is Screen.Dashboard -> {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToAddPurchase = { currentScreen = Screen.AddEditPurchase(null) },
                        onNavigateToPurchaseDetails = { id -> currentScreen = Screen.PurchaseDetails(id) },
                        onNavigateToPurchasesList = { currentScreen = Screen.Purchases },
                        onNavigateToAccounts = { currentScreen = Screen.Accounts }
                    )
                }

                is Screen.Purchases -> {
                    PurchasesScreen(
                        viewModel = viewModel,
                        onNavigateToAddPurchase = { currentScreen = Screen.AddEditPurchase(null) },
                        onNavigateToPurchaseDetails = { id -> currentScreen = Screen.PurchaseDetails(id) }
                    )
                }

                is Screen.Accounts -> {
                    AccountsScreen(
                        viewModel = viewModel,
                        onNavigateToPurchaseDetails = { id -> currentScreen = Screen.PurchaseDetails(id) }
                    )
                }

                is Screen.Settings -> {
                    SettingsScreen(viewModel = viewModel)
                }

                is Screen.PurchaseDetails -> {
                    BackHandler {
                        currentScreen = Screen.Purchases
                    }
                    PurchaseDetailsScreen(
                        purchaseId = screen.purchaseId,
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = Screen.Purchases },
                        onNavigateToEdit = { editId -> currentScreen = Screen.AddEditPurchase(editId) }
                    )
                }

                is Screen.AddEditPurchase -> {
                    BackHandler {
                        currentScreen = if (screen.purchaseIdToEdit != null) {
                            Screen.PurchaseDetails(screen.purchaseIdToEdit)
                        } else {
                            Screen.Purchases
                        }
                    }
                    AddEditPurchaseScreen(
                        viewModel = viewModel,
                        purchaseIdToEdit = screen.purchaseIdToEdit,
                        onNavigateBack = {
                            currentScreen = if (screen.purchaseIdToEdit != null) {
                                Screen.PurchaseDetails(screen.purchaseIdToEdit)
                            } else {
                                Screen.Purchases
                            }
                        },
                        onPurchaseSaved = { savedId ->
                            currentScreen = Screen.PurchaseDetails(savedId)
                        }
                    )
                }
            }
        }
    }
}
