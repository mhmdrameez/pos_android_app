package com.example.quickbillposs

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.quickbillposs.ui.theme.PosBgSidebar
import com.example.quickbillposs.ui.theme.PosBorder
import com.example.quickbillposs.ui.theme.PosSteelBlue
import com.example.quickbillposs.ui.theme.QuickBillTheme
import com.example.quickbillposs.viewmodel.SalesViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
        // ── TABLET / LANDSCAPE LAYOUT (Left Navigation Rail) ──────────────────
        Scaffold { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Surface(
                    color = PosBgSidebar,
                    modifier = Modifier
                        .width(64.dp)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 12.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 1.dp,
                                modifier = Modifier
                                    .size(42.dp)
                                    .border(1.dp, PosBorder, RoundedCornerShape(10.dp))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "Menu",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(2.dp))

                            bottomNavItems.forEach { screen ->
                                val isSelected = currentDestination?.hierarchy?.any {
                                    it.route == screen.route
                                } == true

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.surface else PosBgSidebar,
                                    shadowElevation = if (isSelected) 1.dp else 0.dp,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .then(
                                            if (isSelected) Modifier.border(1.dp, PosBorder, RoundedCornerShape(10.dp))
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
                                            tint = if (isSelected) PosSteelBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = PosSteelBlue,
                                modifier = Modifier.size(36.dp, 26.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "POS",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                            Text(
                                text = "v1.0.60",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    tint = if (isPrinterConnected) PosSteelBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (isPrinterConnected) printerName.take(10) else "Printer",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isPrinterConnected) PosSteelBlue else MaterialTheme.colorScheme.onSurfaceVariant
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
