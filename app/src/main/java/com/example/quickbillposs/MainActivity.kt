package com.example.quickbillposs

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.quickbillposs.ui.navigation.Screen
import com.example.quickbillposs.ui.navigation.bottomNavItems
import com.example.quickbillposs.ui.screens.*
import com.example.quickbillposs.ui.theme.*
import com.example.quickbillposs.viewmodel.SalesViewModel

import androidx.lifecycle.lifecycleScope
import com.example.quickbillposs.data.AppDatabase
import com.example.quickbillposs.data.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Safely pre-warm Database & DataStore asynchronously during splash screen
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                AppDatabase.getInstance(applicationContext)
                PreferencesManager(applicationContext)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        setContent {
            QuickBillTheme {
                var showSplash by remember { mutableStateOf(true) }

                if (showSplash) {
                    SplashScreen(onSplashFinished = { showSplash = false })
                } else {
                    QuickBillApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickBillApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val salesViewModel: SalesViewModel = viewModel()
    val shopName by salesViewModel.shopName.collectAsStateWithLifecycle()
    val printerMac by salesViewModel.printerMac.collectAsStateWithLifecycle()
    val printerName by salesViewModel.printerName.collectAsStateWithLifecycle()

    val configuration = LocalConfiguration.current
    val isLandscapeOrTablet = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE || configuration.screenWidthDp >= 600

    if (isLandscapeOrTablet) {
        // ── TABLET / LANDSCAPE LAYOUT (Left Sidebar as in HTML Template) ─────
        Scaffold { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Surface(
                    color = PosBgPanel,
                    modifier = Modifier
                        .width(72.dp)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top Logo / Menu button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 1.dp,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "Menu",
                                        tint = PosTextDark,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(4.dp))

                            // Navigation buttons (.nav-btn)
                            bottomNavItems.forEach { screen ->
                                val isSelected = currentDestination?.hierarchy?.any {
                                    it.route == screen.route
                                } == true

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.surface else PosBgPanel,
                                    shadowElevation = if (isSelected) 1.dp else 0.dp,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .then(
                                            if (isSelected) Modifier.border(1.dp, PosPrimary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                            else Modifier
                                        )
                                        .clickable {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = screen.icon,
                                            contentDescription = screen.label,
                                            tint = if (isSelected) PosPrimary else PosTextMuted,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom Sidebar Footer: LED status, POS Badge, Version
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Online Status LED
                            Surface(
                                shape = CircleShape,
                                color = PosStatusOnline,
                                modifier = Modifier.size(10.dp)
                            ) {}

                            // POS Badge (.pos-badge)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = PosPrimary,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "POS",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        ),
                                        color = PosTextWhite
                                    )
                                }
                            }

                            // Version Tag
                            Text(
                                text = "v1.0.0",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = PosTextMuted
                            )
                        }
                    }
                }

                VerticalDivider(color = PosBorder, thickness = 1.dp)

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    AppNavHost(navController = navController, salesViewModel = salesViewModel)
                }
            }
        }
    } else {
        // ── MOBILE PORTRAIT LAYOUT (Top Bar + Main Area + Bottom Bar) ──────────
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = shopName.ifBlank { "QuickBill POS" },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    actions = {
                        val isPrinterConnected = printerMac.isNotBlank()
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isPrinterConnected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clickable { navController.navigate(Screen.PrinterSettings.route) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Print,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (isPrinterConnected) PosPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (isPrinterConnected) printerName.take(10) else "Printer",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isPrinterConnected) PosPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    bottomNavItems.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == screen.route
                        } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.label
                                )
                            },
                            label = { Text(screen.label) },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AppNavHost(navController = navController, salesViewModel = salesViewModel)
            }
        }
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    salesViewModel: SalesViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.QuickSale.route,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(Screen.QuickSale.route) {
            QuickSaleScreen(
                onNavigateToPrinterSettings = {
                    navController.navigate(Screen.PrinterSettings.route)
                },
                viewModel = salesViewModel
            )
        }
        composable(Screen.History.route) {
            HistoryScreen()
        }
        composable(Screen.PrinterSettings.route) {
            PrinterSettingsScreen(viewModel = salesViewModel)
        }
        composable(Screen.Settings.route) {
            SettingsScreen()
        }
    }
}
