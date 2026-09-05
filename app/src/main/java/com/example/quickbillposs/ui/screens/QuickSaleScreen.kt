package com.example.quickbillposs.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quickbillposs.data.model.CartItem
import com.example.quickbillposs.ui.components.CartItemRow
import com.example.quickbillposs.ui.components.NumericKeypad
import com.example.quickbillposs.ui.components.SuggestionChipRow
import com.example.quickbillposs.ui.components.formatAmount
import com.example.quickbillposs.ui.components.formatQuantity
import com.example.quickbillposs.ui.theme.*
import com.example.quickbillposs.viewmodel.SalesViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickSaleScreen(
    onNavigateToPrinterSettings: () -> Unit,
    viewModel: SalesViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )
    } else {
        emptyArray()
    }

    val printPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            viewModel.printReceipt()
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Bluetooth permission is required to print receipts.")
            }
        }
    }

    val input by viewModel.input.collectAsStateWithLifecycle()
    val cart by viewModel.cart.collectAsStateWithLifecycle()
    val total by viewModel.total.collectAsStateWithLifecycle()
    val itemCount by viewModel.itemCount.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val suggestionsEnabled by viewModel.suggestionsEnabled.collectAsStateWithLifecycle()
    val printStatus by viewModel.printStatus.collectAsStateWithLifecycle()
    val checkoutResult by viewModel.checkoutResult.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    var showCheckout by remember { mutableStateOf(false) }
    var showOrdersSheet by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600 || configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Show print status snackbar
    LaunchedEffect(printStatus) {
        printStatus?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearPrintStatus()
        }
    }
    LaunchedEffect(checkoutResult) {
        checkoutResult?.let { result ->
            if (result.success) {
                snackbarHostState.showSnackbar("✓ Sale recorded!")
            } else {
                snackbarHostState.showSnackbar(result.message)
            }
            viewModel.clearCheckoutResult()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = PosBgMain
    ) { innerPadding ->
        if (isWideScreen) {
            // ── 2-COLUMN DUAL PANEL (Tablet / Landscape) ──────────────────────
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Left Workspace: Keypad
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .padding(16.dp)
                ) {
                    TopControlRow(
                        suggestionsEnabled = suggestionsEnabled,
                        onToggleSuggestions = viewModel::toggleSuggestions,
                        onNavigateToPrinter = onNavigateToPrinterSettings
                    )

                    Spacer(Modifier.height(12.dp))

                    AmountDisplay(input = input, modifier = Modifier.fillMaxWidth())

                    Spacer(Modifier.height(12.dp))

                    if (suggestionsEnabled) {
                        SuggestionChipRow(
                            suggestions = suggestions,
                            onSuggestionClick = { product -> viewModel.addItemFromSuggestion(product) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    NumericKeypad(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        isMultiplyActive = input.contains('x') || input.contains('X') || input.contains('*') || input.contains('×'),
                        onDigit = viewModel::onDigit,
                        onBackspace = viewModel::onBackspace,
                        onClear = viewModel::onClear,
                        onMultiply = viewModel::onMultiply,
                        onAddItem = { viewModel.addItemFromInput() }
                    )
                }

                VerticalDivider(color = PosBorder, thickness = 1.dp)

                // Right Panel: Orders Cart Column
                Surface(
                    color = PosBgCartPanel,
                    modifier = Modifier
                        .weight(0.8f)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        CartHeader(itemCount = itemCount, onClearCart = viewModel::clearCart)

                        Spacer(Modifier.height(12.dp))

                        if (cart.isEmpty()) {
                            EmptyCartPlaceholder(modifier = Modifier.weight(1f))
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(cart, key = { it.id }) { item ->
                                    CartItemRow(
                                        item = item,
                                        onRemove = { viewModel.removeItem(item.id) },
                                        onQuantityChange = { qty -> viewModel.updateQuantity(item.id, qty) }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        OrderSummarySection(
                            itemCount = itemCount,
                            total = total,
                            isLoading = isLoading,
                            cartNotEmpty = cart.isNotEmpty(),
                            onPrint = {
                                checkPermissionAndPrint(
                                    context = context,
                                    bluetoothPermissions = bluetoothPermissions,
                                    printPermissionLauncher = printPermissionLauncher,
                                    onPrint = viewModel::printReceipt
                                )
                            },
                            onCheckout = { showCheckout = true }
                        )
                    }
                }
            }
        } else {
            // ── SINGLE COLUMN (Mobile Phone Portrait) ─────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(12.dp)
            ) {
                TopControlRow(
                    suggestionsEnabled = suggestionsEnabled,
                    onToggleSuggestions = viewModel::toggleSuggestions,
                    onNavigateToPrinter = onNavigateToPrinterSettings
                )

                Spacer(Modifier.height(8.dp))

                AmountDisplay(input = input, modifier = Modifier.fillMaxWidth())

                Spacer(Modifier.height(8.dp))

                if (suggestionsEnabled) {
                    SuggestionChipRow(
                        suggestions = suggestions,
                        onSuggestionClick = { product -> viewModel.addItemFromSuggestion(product) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                }

                NumericKeypad(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    isMultiplyActive = input.contains('x') || input.contains('X') || input.contains('*') || input.contains('×'),
                    onDigit = viewModel::onDigit,
                    onBackspace = viewModel::onBackspace,
                    onClear = viewModel::onClear,
                    onMultiply = viewModel::onMultiply,
                    onAddItem = { viewModel.addItemFromInput() }
                )

                // Mobile Bottom Sticky Cart Banner
                if (cart.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PosBgCartPanel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, PosBorder, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showOrdersSheet = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = PosSteelBlue,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            formatQuantity(itemCount),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = PosTextWhite
                                        )
                                    }
                                }
                                Text(
                                    "View Cart",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = PosTextDark
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "₹${formatAmount(total)}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = PosTextDark
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = PosSteelBlue,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { showCheckout = true }
                                ) {
                                    Text(
                                        "Bill →",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = PosTextWhite,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Mobile Orders Bottom Sheet ────────────────────────────────────────────
        if (showOrdersSheet) {
            ModalBottomSheet(
                onDismissRequest = { showOrdersSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = PosBgCartPanel,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .navigationBarsPadding()
                ) {
                    CartHeader(itemCount = itemCount, onClearCart = viewModel::clearCart)

                    Spacer(Modifier.height(12.dp))

                    if (cart.isEmpty()) {
                        EmptyCartPlaceholder(modifier = Modifier.height(200.dp))
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 320.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(cart, key = { it.id }) { item ->
                                CartItemRow(
                                    item = item,
                                    onRemove = { viewModel.removeItem(item.id) },
                                    onQuantityChange = { qty -> viewModel.updateQuantity(item.id, qty) }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    OrderSummarySection(
                        itemCount = itemCount,
                        total = total,
                        isLoading = isLoading,
                        cartNotEmpty = cart.isNotEmpty(),
                        onPrint = {
                            checkPermissionAndPrint(
                                context = context,
                                bluetoothPermissions = bluetoothPermissions,
                                printPermissionLauncher = printPermissionLauncher,
                                onPrint = viewModel::printReceipt
                            )
                        },
                        onCheckout = {
                            showOrdersSheet = false
                            showCheckout = true
                        }
                    )
                }
            }
        }

        // ── Checkout Bottom Sheet ─────────────────────────────────────────────────
        if (showCheckout) {
            CheckoutSheet(
                total = total,
                onDismiss = { showCheckout = false },
                onCheckout = { method, tendered ->
                    viewModel.checkout(method, tendered)
                    showCheckout = false
                }
            )
        }
    }
}

@Composable
private fun TopControlRow(
    suggestionsEnabled: Boolean,
    onToggleSuggestions: () -> Unit,
    onNavigateToPrinter: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Suggestions On / Off Pill
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SuggestionChipBg,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onToggleSuggestions() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = SuggestionChipText,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (suggestionsEnabled) "Suggestions On" else "Suggestions Off",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = SuggestionChipText
                    )
                }
            }

            // Printer Settings Button
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = PosBgMain,
                modifier = Modifier
                    .border(1.dp, PosBorder, RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onNavigateToPrinter() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = null,
                        tint = PosSteelBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Printer Settings",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = PosSteelBlue
                    )
                }
            }
        }
    }
}

@Composable
private fun CartHeader(
    itemCount: Double,
    onClearCart: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Orders",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = PosTextDark
        )

        if (itemCount > 0.0) {
            TextButton(
                onClick = onClearCart,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    "Clear All",
                    color = AccentRed,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun OrderSummarySection(
    itemCount: Double,
    total: Double,
    isLoading: Boolean,
    cartNotEmpty: Boolean,
    onPrint: () -> Unit,
    onCheckout: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Total Qty",
                style = MaterialTheme.typography.bodyMedium,
                color = PosTextMuted
            )
            Text(
                text = formatQuantity(itemCount),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = PosTextDark
            )
        }

        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Grand Total",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = PosTextDark
            )
            Text(
                text = "₹${formatAmount(total)}",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = PosTextDark
            )
        }

        Spacer(Modifier.height(14.dp))

        // Action Buttons: Print (White Outlined Pill) and Bill (Steel Blue Pill)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = PosBgMain,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .border(1.dp, PosBorder, RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(enabled = cartNotEmpty && !isLoading) { onPrint() }
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Print,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (cartNotEmpty) PosTextDark else PosTextMuted
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Print",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                        color = if (cartNotEmpty) PosTextDark else PosTextMuted
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (cartNotEmpty) PosSteelBlue else PosSteelBlue.copy(alpha = 0.5f),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(enabled = cartNotEmpty && !isLoading) { onCheckout() }
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = PosTextWhite,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.AttachMoney,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = PosTextWhite
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (total > 0) "Bill ₹${formatAmount(total)}" else "Bill",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = PosTextWhite
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AmountDisplay(
    input: String,
    modifier: Modifier = Modifier
) {
    val displayText = buildDisplayText(input)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = PosBgKeypadKey,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = "Amount",
                style = MaterialTheme.typography.labelMedium,
                color = PosTextMuted
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = displayText,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                ),
                color = PosTextDark,
                maxLines = 1
            )
        }
    }
}

private fun buildDisplayText(input: String): String {
    if (input.isBlank()) return "₹0"
    val clean = input.trim().replace('X', 'x').replace('*', 'x').replace('×', 'x')
    return if (clean.contains('x')) {
        val parts = clean.split('x')
        val price = parts[0].toDoubleOrNull() ?: 0.0
        val qtyStr = parts.getOrNull(1) ?: ""
        val qty = qtyStr.toDoubleOrNull() ?: 1.0
        if (qtyStr.isBlank()) {
            "₹${formatAmount(price)} ×"
        } else {
            "₹${formatAmount(price)} × ${formatQuantity(qty)} = ₹${formatAmount(price * qty)}"
        }
    } else {
        "₹$input"
    }
}

@Composable
private fun EmptyCartPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 32.dp, horizontal = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = PosTextMuted
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Your cart is empty",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = PosTextDark,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Enter an amount and tap Add Item",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = PosTextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun checkPermissionAndPrint(
    context: Context,
    bluetoothPermissions: Array<String>,
    printPermissionLauncher: ActivityResultLauncher<Array<String>>,
    onPrint: () -> Unit
) {
    val hasPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        bluetoothPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    } else {
        true
    }

    if (hasPerm) {
        onPrint()
    } else {
        printPermissionLauncher.launch(bluetoothPermissions)
    }
}
