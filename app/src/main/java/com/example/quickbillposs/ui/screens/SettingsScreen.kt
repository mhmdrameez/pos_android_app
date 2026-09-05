package com.example.quickbillposs.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quickbillposs.data.PreferencesManager
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val shopName by prefs.shopName.collectAsState(initial = "QuickBill POS")
    val shopAddress by prefs.shopAddress.collectAsState(initial = "")
    val shopPhone by prefs.shopPhone.collectAsState(initial = "")
    val upiId by prefs.upiId.collectAsState(initial = "")
    val taxPercent by prefs.taxPercent.collectAsState(initial = 0)
    val receiptFooter by prefs.receiptFooter.collectAsState(initial = "Thank you! Visit again.")

    var nameField by remember(shopName) { mutableStateOf(shopName) }
    var addressField by remember(shopAddress) { mutableStateOf(shopAddress) }
    var phoneField by remember(shopPhone) { mutableStateOf(shopPhone) }
    var upiField by remember(upiId) { mutableStateOf(upiId) }
    var taxField by remember(taxPercent) { mutableStateOf(taxPercent.toString()) }
    var footerField by remember(receiptFooter) { mutableStateOf(receiptFooter) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Settings",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            // Shop Details section
            SettingsSection(title = "Shop Details") {
                SettingsField(
                    label = "Shop Name",
                    value = nameField,
                    onValueChange = { nameField = it },
                    leadingIcon = Icons.Default.Store
                )
                SettingsField(
                    label = "Address",
                    value = addressField,
                    onValueChange = { addressField = it },
                    leadingIcon = Icons.Default.LocationOn,
                    singleLine = false
                )
                SettingsField(
                    label = "Phone Number",
                    value = phoneField,
                    onValueChange = { phoneField = it },
                    leadingIcon = Icons.Default.Phone,
                    keyboardType = KeyboardType.Phone
                )
            }

            // Payment section
            SettingsSection(title = "Payment & Tax") {
                SettingsField(
                    label = "UPI ID",
                    value = upiField,
                    onValueChange = { upiField = it },
                    leadingIcon = Icons.Default.QrCode
                )
                SettingsField(
                    label = "Tax % (0 = no tax)",
                    value = taxField,
                    onValueChange = { taxField = it.filter { c -> c.isDigit() } },
                    leadingIcon = Icons.Default.Percent,
                    keyboardType = KeyboardType.Number
                )
            }

            // Receipt section
            SettingsSection(title = "Receipt") {
                SettingsField(
                    label = "Footer Message",
                    value = footerField,
                    onValueChange = { footerField = it },
                    leadingIcon = Icons.Default.TextFields,
                    singleLine = false
                )
            }

            // Save button
            Button(
                onClick = {
                    scope.launch {
                        prefs.setShopName(nameField.ifBlank { "QuickBill POS" })
                        prefs.setShopAddress(addressField)
                        prefs.setShopPhone(phoneField)
                        prefs.setUpiId(upiField)
                        prefs.setTaxPercent(taxField.toIntOrNull() ?: 0)
                        prefs.setReceiptFooter(footerField.ifBlank { "Thank you! Visit again." })
                        snackbarHostState.showSnackbar("✓ Settings saved!")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Save Settings")
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
    }
}

@Composable
private fun SettingsField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(leadingIcon, contentDescription = null) },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            capitalization = if (keyboardType == KeyboardType.Text)
                KeyboardCapitalization.Sentences else KeyboardCapitalization.None
        ),
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else 3,
        modifier = Modifier.fillMaxWidth()
    )
}
