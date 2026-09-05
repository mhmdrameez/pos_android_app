package com.example.quickbillposs.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    var showLabelDialog by remember { mutableStateOf(false) }

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
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── LEFT PANEL: Keypad + Input + Suggestions ──────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(12.dp)
            ) {
                // Top bar: Suggestions toggle + Printer settings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Suggestions toggle chip
                    FilterChip(
                        selected = suggestionsEnabled,
                        onClick = { viewModel.toggleSuggestions() },
                        label = {
                            Text(
                                text = if (suggestionsEnabled) "Suggestions On" else "Suggestions Off",
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )

                    // Printer settings button
                    OutlinedButton(
                        onClick = onNavigateToPrinterSettings,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.Print,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Printer", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Amount display
                AmountDisplay(
                    input = input,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                // Suggestion chips
                if (suggestionsEnabled) {
                    SuggestionChipRow(
                        suggestions = suggestions,
                        onSuggestionClick = { product ->
                            viewModel.addItemFromSuggestion(product)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                }

                // Numeric keypad
                NumericKeypad(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    onDigit = viewModel::onDigit,
                    onBackspace = viewModel::onBackspace,
                    onClear = viewModel::onClear,
                    onMultiply = viewModel::onMultiply,
                    onAddItem = {
                        if (input.isNotBlank()) {
                            showLabelDialog = true
                        }
                    }
                )
            }

            // ── DIVIDER ───────────────────────────────────────────────────────
            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // ── RIGHT PANEL: Orders / Cart ────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(12.dp)
            ) {
                // Orders header
                Text(
                    text = "Orders",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Cart items
                if (cart.isEmpty()) {
                    EmptyCartPlaceholder(modifier = Modifier.weight(1f))
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
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

                Spacer(Modifier.height(8.dp))

                // Summary
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total Qty",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$itemCount",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Grand Total",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "₹${formatAmount(total)}",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Print + Bill buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val hasPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                bluetoothPermissions.all {
                                    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                                }
                            } else {
                                true
                            }

                            if (hasPerm) {
                                viewModel.printReceipt()
                            } else {
                                printPermissionLauncher.launch(bluetoothPermissions)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = cart.isNotEmpty() && !isLoading
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Print")
                    }

                    Button(
                        onClick = { showCheckout = true },
                        modifier = Modifier.weight(1f),
                        enabled = cart.isNotEmpty() && !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Bill")
                        }
                    }
                }
            }
        }

        // ── Label Dialog (quick item name) ────────────────────────────────────────
        if (showLabelDialog) {
            var labelText by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = {
                    showLabelDialog = false
                    viewModel.addItemFromInput()
                },
                title = { Text("Item label (optional)") },
                text = {
                    OutlinedTextField(
                        value = labelText,
                        onValueChange = { labelText = it },
                        placeholder = { Text("e.g. Milk, Rice, etc.") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showLabelDialog = false
                        viewModel.addItemFromInput(labelText)
                    }) { Text("Add") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showLabelDialog = false
                        viewModel.addItemFromInput()
                    }) { Text("Skip") }
                }
            )
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
private fun AmountDisplay(
    input: String,
    modifier: Modifier = Modifier
) {
    val displayText = buildDisplayText(input)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column {
            Text(
                text = "Amount",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = displayText,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

private fun buildDisplayText(input: String): String {
    if (input.isBlank()) return "₹0"
    return if (input.contains('x')) {
        val parts = input.split('x')
        val price = parts[0].toDoubleOrNull() ?: 0.0
        val qty = parts.getOrNull(1)?.toIntOrNull() ?: 1
        "₹${formatAmount(price)} × $qty = ₹${formatAmount(price * qty)}"
    } else {
        "₹$input"
    }
}

@Composable
private fun EmptyCartPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ShoppingCart,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.outlineVariant
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Your cart is empty",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Enter an amount and tap Add Item",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outlineVariant,
            textAlign = TextAlign.Center
        )
    }
}
