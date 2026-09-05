package com.example.quickbillposs.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    object QuickSale : Screen("quick_sale", "Sale", Icons.Default.PointOfSale)
    object History : Screen("history", "History", Icons.Default.History)
    object PrinterSettings : Screen("printer_settings", "Printer", Icons.Default.Print)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

val bottomNavItems = listOf(
    Screen.QuickSale,
    Screen.History,
    Screen.PrinterSettings,
    Screen.Settings
)
