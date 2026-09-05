package com.example.quickbillposs.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "quickbill_settings")

class PreferencesManager(private val context: Context) {

    companion object {
        private val SHOP_NAME = stringPreferencesKey("shop_name")
        private val SHOP_ADDRESS = stringPreferencesKey("shop_address")
        private val SHOP_PHONE = stringPreferencesKey("shop_phone")
        private val UPI_ID = stringPreferencesKey("upi_id")
        private val TAX_PERCENT = intPreferencesKey("tax_percent")
        private val PRINTER_MAC = stringPreferencesKey("printer_mac")
        private val PRINTER_NAME = stringPreferencesKey("printer_name")
        private val SUGGESTIONS_ENABLED = stringPreferencesKey("suggestions_enabled")
        private val DEFAULT_PAYMENT = stringPreferencesKey("default_payment")
        private val RECEIPT_FOOTER = stringPreferencesKey("receipt_footer")

        const val DEFAULT_SHOP_NAME = "QuickBill POS"
    }

    val shopName: Flow<String> = context.dataStore.data
        .map { it[SHOP_NAME] ?: DEFAULT_SHOP_NAME }

    val shopAddress: Flow<String> = context.dataStore.data
        .map { it[SHOP_ADDRESS] ?: "" }

    val shopPhone: Flow<String> = context.dataStore.data
        .map { it[SHOP_PHONE] ?: "" }

    val upiId: Flow<String> = context.dataStore.data
        .map { it[UPI_ID] ?: "" }

    val taxPercent: Flow<Int> = context.dataStore.data
        .map { it[TAX_PERCENT] ?: 0 }

    val printerMac: Flow<String> = context.dataStore.data
        .map { it[PRINTER_MAC] ?: "" }

    val printerName: Flow<String> = context.dataStore.data
        .map { it[PRINTER_NAME] ?: "No printer selected" }

    val suggestionsEnabled: Flow<Boolean> = context.dataStore.data
        .map { (it[SUGGESTIONS_ENABLED] ?: "true") == "true" }

    val defaultPayment: Flow<String> = context.dataStore.data
        .map { it[DEFAULT_PAYMENT] ?: "CASH" }

    val receiptFooter: Flow<String> = context.dataStore.data
        .map { it[RECEIPT_FOOTER] ?: "Thank you! Visit again." }

    suspend fun setShopName(value: String) =
        context.dataStore.edit { it[SHOP_NAME] = value }

    suspend fun setShopAddress(value: String) =
        context.dataStore.edit { it[SHOP_ADDRESS] = value }

    suspend fun setShopPhone(value: String) =
        context.dataStore.edit { it[SHOP_PHONE] = value }

    suspend fun setUpiId(value: String) =
        context.dataStore.edit { it[UPI_ID] = value }

    suspend fun setTaxPercent(value: Int) =
        context.dataStore.edit { it[TAX_PERCENT] = value }

    suspend fun setPrinter(mac: String, name: String) {
        context.dataStore.edit {
            it[PRINTER_MAC] = mac
            it[PRINTER_NAME] = name
        }
    }

    suspend fun setSuggestionsEnabled(enabled: Boolean) =
        context.dataStore.edit { it[SUGGESTIONS_ENABLED] = enabled.toString() }

    suspend fun setDefaultPayment(value: String) =
        context.dataStore.edit { it[DEFAULT_PAYMENT] = value }

    suspend fun setReceiptFooter(value: String) =
        context.dataStore.edit { it[RECEIPT_FOOTER] = value }
}
