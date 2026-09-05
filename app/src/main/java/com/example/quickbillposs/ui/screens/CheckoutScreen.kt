package com.example.quickbillposs.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.quickbillposs.ui.components.formatAmount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutSheet(
    total: Double,
    onDismiss: () -> Unit,
    onCheckout: (method: String, amountTendered: Double) -> Unit
) {
    var selectedMethod by remember { mutableStateOf("CASH") }
    var cashTendered by remember { mutableStateOf("") }
    val change by remember(cashTendered, total) {
        derivedStateOf {
            val tendered = cashTendered.toDoubleOrNull() ?: 0.0
            if (tendered >= total) tendered - total else 0.0
        }
    }
    val isValidCash by remember(cashTendered, total, selectedMethod) {
        derivedStateOf {
            if (selectedMethod != "CASH") true
            else {
                val t = cashTendered.toDoubleOrNull() ?: 0.0
                t >= total
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .navigationBarsPadding()
        ) {
            // Title
            Text(
                text = "Checkout",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(4.dp))

            // Total display
            Text(
                text = "Grand Total: ₹${formatAmount(total)}",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(20.dp))

            // Payment method selection
            Text(
                text = "Payment Method",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PaymentMethodButton(
                    label = "Cash",
                    icon = Icons.Default.Money,
                    selected = selectedMethod == "CASH",
                    modifier = Modifier.weight(1f),
                    onClick = { selectedMethod = "CASH" }
                )
                PaymentMethodButton(
                    label = "UPI",
                    icon = Icons.Default.QrCode,
                    selected = selectedMethod == "UPI",
                    modifier = Modifier.weight(1f),
                    onClick = { selectedMethod = "UPI" }
                )
                PaymentMethodButton(
                    label = "Card",
                    icon = Icons.Default.CreditCard,
                    selected = selectedMethod == "CARD",
                    modifier = Modifier.weight(1f),
                    onClick = { selectedMethod = "CARD" }
                )
            }

            // Cash tendered (only for CASH)
            AnimatedVisibility(
                visible = selectedMethod == "CASH",
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = cashTendered,
                        onValueChange = { cashTendered = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Amount Tendered (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        prefix = { Text("₹") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = cashTendered.isNotEmpty() &&
                                (cashTendered.toDoubleOrNull() ?: 0.0) < total
                    )

                    // Quick amount buttons
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            ceilToNearest(total, 10.0),
                            ceilToNearest(total, 50.0),
                            ceilToNearest(total, 100.0)
                        ).distinct().forEach { quickAmount ->
                            SuggestionChip(
                                onClick = { cashTendered = quickAmount.toLong().toString() },
                                label = { Text("₹${quickAmount.toLong()}") }
                            )
                        }
                    }

                    // Change display
                    if (cashTendered.isNotEmpty() && (cashTendered.toDoubleOrNull() ?: 0.0) >= total) {
                        Spacer(Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Change to return:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    "₹${formatAmount(change)}",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Confirm button
            Button(
                onClick = {
                    val tendered = cashTendered.toDoubleOrNull() ?: total
                    onCheckout(selectedMethod, tendered)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = isValidCash,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Confirm Payment ₹${formatAmount(total)}",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(Modifier.height(8.dp))

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun PaymentMethodButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val containerColor = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurfaceVariant

    Button(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

private fun ceilToNearest(value: Double, nearest: Double): Double {
    return Math.ceil(value / nearest) * nearest
}
