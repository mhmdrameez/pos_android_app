package com.example.quickbillposs.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.quickbillposs.PrinterHelper
import com.example.quickbillposs.PrintResult
import com.example.quickbillposs.SuggestionEngine
import com.example.quickbillposs.data.AppDatabase
import com.example.quickbillposs.data.PreferencesManager
import com.example.quickbillposs.data.model.CartItem
import com.example.quickbillposs.data.model.Product
import com.example.quickbillposs.data.model.Sale
import com.example.quickbillposs.data.model.SaleItem
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CheckoutResult(
    val success: Boolean,
    val saleId: Long = -1,
    val message: String = ""
)

@OptIn(FlowPreview::class)
class SalesViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val prefs = PreferencesManager(application)
    private val suggestionEngine = SuggestionEngine(db.productDao())
    private val printerHelper = PrinterHelper(application)

    // ── Keypad Input ──────────────────────────────────────────────────────────
    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    // ── Cart ──────────────────────────────────────────────────────────────────
    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    val total: StateFlow<Double> = _cart
        .map { items -> items.sumOf { it.lineTotal } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val itemCount: StateFlow<Int> = _cart
        .map { items -> items.sumOf { it.quantity } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ── Suggestions ───────────────────────────────────────────────────────────
    private val _suggestions = MutableStateFlow<List<Product>>(emptyList())
    val suggestions: StateFlow<List<Product>> = _suggestions.asStateFlow()

    val suggestionsEnabled: StateFlow<Boolean> = prefs.suggestionsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // ── Print / Checkout Status ───────────────────────────────────────────────
    private val _printStatus = MutableStateFlow<String?>(null)
    val printStatus: StateFlow<String?> = _printStatus.asStateFlow()

    private val _checkoutResult = MutableStateFlow<CheckoutResult?>(null)
    val checkoutResult: StateFlow<CheckoutResult?> = _checkoutResult.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Settings flows
    val shopName = prefs.shopName.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "QuickBill POS")
    val shopAddress = prefs.shopAddress.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")
    val shopPhone = prefs.shopPhone.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")
    val taxPercent = prefs.taxPercent.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)
    val printerMac = prefs.printerMac.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")
    val receiptFooter = prefs.receiptFooter.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "Thank you! Visit again.")

    init {
        // Auto-refresh suggestions whenever input changes (debounced 300ms)
        viewModelScope.launch {
            _input
                .debounce(300)
                .collect { inputValue ->
                    if (suggestionsEnabled.value) {
                        refreshSuggestions(inputValue)
                    }
                }
        }
        // Load initial suggestions
        viewModelScope.launch {
            refreshSuggestions("")
        }
    }

    // ── Keypad Actions ────────────────────────────────────────────────────────

    fun onDigit(digit: String) {
        val current = _input.value
        // Prevent leading zeros (except "0." and "0")
        if (current == "0" && digit != "." && digit != "00") {
            _input.value = digit
        } else {
            // Prevent multiple dots
            if (digit == "." && current.contains(".")) return
            // Prevent double-zero at start
            if (digit == "00" && current.isEmpty()) {
                _input.value = "0"
                return
            }
            _input.value = current + digit
        }
    }

    fun onBackspace() {
        val current = _input.value
        if (current.isNotEmpty()) {
            _input.value = current.dropLast(1)
        }
    }

    fun onClear() {
        _input.value = ""
    }

    fun onMultiply() {
        val current = _input.value
        // Toggle: if already has 'x', remove it; otherwise append 'x'
        if (!current.contains('x') && !current.contains('*') && current.isNotEmpty()) {
            _input.value = "${current}x"
        }
    }

    // ── Cart Actions ──────────────────────────────────────────────────────────

    fun addItemFromInput(label: String = "") {
        val inputStr = _input.value
        if (inputStr.isBlank()) return

        val (amount, qty) = parseInputToAmountQty(inputStr)
        if (amount <= 0) return

        val item = CartItem(
            label = label,
            amount = amount,
            quantity = qty
        )
        _cart.value = _cart.value + item

        // Learn from this sale item
        viewModelScope.launch {
            suggestionEngine.learnFromSale(amount, label)
        }

        _input.value = ""
        refreshSuggestionsNow("")
    }

    fun addItemFromSuggestion(product: Product) {
        val item = CartItem(
            label = product.name,
            amount = product.price,
            quantity = 1
        )
        _cart.value = _cart.value + item

        viewModelScope.launch {
            suggestionEngine.recordUsage(product.id)
        }

        _input.value = ""
        refreshSuggestionsNow("")
    }

    fun removeItem(itemId: Long) {
        _cart.value = _cart.value.filter { it.id != itemId }
    }

    fun updateQuantity(itemId: Long, newQty: Int) {
        if (newQty <= 0) {
            removeItem(itemId)
            return
        }
        _cart.value = _cart.value.map {
            if (it.id == itemId) it.copy(quantity = newQty) else it
        }
    }

    fun clearCart() {
        _cart.value = emptyList()
        _input.value = ""
    }

    // ── Checkout ──────────────────────────────────────────────────────────────

    fun checkout(
        paymentMethod: String,
        amountTendered: Double = 0.0,
        printAfterCheckout: Boolean = true
    ) {
        if (_cart.value.isEmpty()) return
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val items = _cart.value
                val grandTotal = items.sumOf { it.lineTotal }

                val sale = Sale(
                    total = grandTotal,
                    paymentMethod = paymentMethod,
                    itemCount = items.sumOf { it.quantity },
                    amountTendered = amountTendered,
                    changeGiven = if (paymentMethod == "CASH") amountTendered - grandTotal else 0.0
                )

                val saleItems = items.map { cartItem ->
                    SaleItem(
                        saleId = 0, // will be set in DAO transaction
                        amount = cartItem.amount,
                        quantity = cartItem.quantity,
                        label = cartItem.label,
                        lineTotal = cartItem.lineTotal
                    )
                }

                val saleId = db.saleDao().insertSaleWithItems(sale, saleItems)

                if (printAfterCheckout) {
                    printReceipt(items, grandTotal, paymentMethod, amountTendered)
                }

                clearCart()
                _checkoutResult.value = CheckoutResult(success = true, saleId = saleId)

            } catch (e: Exception) {
                _checkoutResult.value = CheckoutResult(
                    success = false,
                    message = "Checkout failed: ${e.message}"
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearCheckoutResult() {
        _checkoutResult.value = null
    }

    // ── Printing ──────────────────────────────────────────────────────────────

    fun printReceipt(
        items: List<CartItem> = _cart.value,
        total: Double = this.total.value,
        paymentMethod: String = "CASH",
        amountTendered: Double = 0.0
    ) {
        viewModelScope.launch {
            _printStatus.value = "Printing..."
            val result = printerHelper.printReceipt(
                shopName = shopName.value,
                shopAddress = shopAddress.value,
                shopPhone = shopPhone.value,
                items = items,
                total = total,
                paymentMethod = paymentMethod,
                amountTendered = amountTendered,
                taxPercent = taxPercent.value,
                footerText = receiptFooter.value,
                savedMac = printerMac.value
            )
            _printStatus.value = when (result) {
                is PrintResult.Success -> "✓ Printed successfully"
                is PrintResult.NoPrinter -> "No Bluetooth printer found. Pair a printer in Settings."
                is PrintResult.Error -> "⚠ ${result.message}"
            }
        }
    }

    fun clearPrintStatus() {
        _printStatus.value = null
    }

    fun getPairedDevices() = printerHelper.getPairedDevices()
    fun isBluetoothEnabled() = printerHelper.isBluetoothEnabled()

    suspend fun savePrinter(mac: String, name: String) {
        prefs.setPrinter(mac, name)
    }

    // ── Suggestions ───────────────────────────────────────────────────────────

    private fun refreshSuggestionsNow(input: String) {
        viewModelScope.launch {
            refreshSuggestions(input)
        }
    }

    private suspend fun refreshSuggestions(input: String) {
        _suggestions.value = suggestionEngine.getSuggestions(input)
    }

    fun toggleSuggestions() {
        viewModelScope.launch {
            prefs.setSuggestionsEnabled(!suggestionsEnabled.value)
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun parseInputToAmountQty(input: String): Pair<Double, Int> {
        val multiplyRegex = Regex("""^(\d+\.?\d*)\s*[xX*×]\s*(\d+\.?\d*)$""")
        val match = multiplyRegex.matchEntire(input.trim())
        return if (match != null) {
            val amount = match.groupValues[1].toDoubleOrNull() ?: 0.0
            val qty = match.groupValues[2].toDoubleOrNull()?.toInt() ?: 1
            Pair(amount, qty)
        } else {
            val amount = input.trim().toDoubleOrNull() ?: 0.0
            Pair(amount, 1)
        }
    }
}
