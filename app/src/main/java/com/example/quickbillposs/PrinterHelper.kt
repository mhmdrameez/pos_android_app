package com.example.quickbillposs

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.dantsu.escposprinter.exceptions.EscPosBarcodeException
import com.dantsu.escposprinter.exceptions.EscPosConnectionException
import com.dantsu.escposprinter.exceptions.EscPosEncodingException
import com.dantsu.escposprinter.exceptions.EscPosParserException
import com.example.quickbillposs.data.model.CartItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class PrintResult {
    object Success : PrintResult()
    data class Error(val message: String) : PrintResult()
    object NoPrinter : PrintResult()
}

class PrinterHelper(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        manager?.adapter
    }

    /** Returns list of paired Bluetooth devices */
    fun getPairedDevices(): List<BluetoothDevice> {
        return try {
            bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    /** Prints a receipt to the saved/first paired Bluetooth printer */
    suspend fun printReceipt(
        shopName: String,
        shopAddress: String,
        shopPhone: String,
        items: List<CartItem>,
        total: Double,
        paymentMethod: String,
        amountTendered: Double = 0.0,
        taxPercent: Int = 0,
        footerText: String = "Thank you! Visit again.",
        savedMac: String = ""
    ): PrintResult = withContext(Dispatchers.IO) {
        try {
            val connection = if (savedMac.isNotBlank()) {
                // Connect to saved printer by MAC
                val device = getPairedDevices().find { it.address == savedMac }
                if (device != null) {
                    com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection(device)
                } else {
                    BluetoothPrintersConnections.selectFirstPaired()
                }
            } else {
                BluetoothPrintersConnections.selectFirstPaired()
            }

            if (connection == null) return@withContext PrintResult.NoPrinter

            val printer = EscPosPrinter(connection, 203, 58f, 32)
            val receiptText = buildReceiptText(
                shopName = shopName,
                shopAddress = shopAddress,
                shopPhone = shopPhone,
                items = items,
                total = total,
                paymentMethod = paymentMethod,
                amountTendered = amountTendered,
                taxPercent = taxPercent,
                footerText = footerText
            )

            printer.printFormattedTextAndCut(receiptText)
            PrintResult.Success

        } catch (e: EscPosConnectionException) {
            PrintResult.Error("Printer connection failed: ${e.message}")
        } catch (e: EscPosParserException) {
            PrintResult.Error("Receipt format error: ${e.message}")
        } catch (e: EscPosEncodingException) {
            PrintResult.Error("Encoding error: ${e.message}")
        } catch (e: EscPosBarcodeException) {
            PrintResult.Error("Barcode error: ${e.message}")
        } catch (e: SecurityException) {
            PrintResult.Error("Bluetooth permission denied. Please grant Bluetooth permissions.")
        } catch (e: Exception) {
            PrintResult.Error("Print failed: ${e.message}")
        }
    }

    private fun buildReceiptText(
        shopName: String,
        shopAddress: String,
        shopPhone: String,
        items: List<CartItem>,
        total: Double,
        paymentMethod: String,
        amountTendered: Double,
        taxPercent: Int,
        footerText: String
    ): String {
        val dateStr = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date())
        val separator = "[L]--------------------------------\n"

        val sb = StringBuilder()

        // Header
        sb.append("[C]<b>${shopName.uppercase()}</b>\n")
        if (shopAddress.isNotBlank()) sb.append("[C]$shopAddress\n")
        if (shopPhone.isNotBlank()) sb.append("[C]Ph: $shopPhone\n")
        sb.append("[C]--------------------------------\n")
        sb.append("[C]RECEIPT\n")
        sb.append("[L]Date: [R]$dateStr\n")
        sb.append(separator)

        // Header row
        sb.append("[L]<b>Item</b>[R]<b>Amount</b>\n")
        sb.append(separator)

        // Items
        for (item in items) {
            val label = item.label.ifBlank { "Item" }.take(18)
            val lineTotal = "Rs.${formatAmount(item.lineTotal)}"
            if (item.quantity > 1) {
                sb.append("[L]$label\n")
                sb.append("[L]  ${formatAmount(item.amount)} x ${item.quantity}[R]$lineTotal\n")
            } else {
                sb.append("[L]$label[R]$lineTotal\n")
            }
        }

        sb.append(separator)

        // Tax
        val taxAmount = if (taxPercent > 0) total * taxPercent / (100 + taxPercent) else 0.0
        val subTotal = total - taxAmount
        if (taxPercent > 0) {
            sb.append("[L]Subtotal[R]Rs.${formatAmount(subTotal)}\n")
            sb.append("[L]GST ($taxPercent%)[R]Rs.${formatAmount(taxAmount)}\n")
            sb.append("[C]--------------------------------\n")
        }

        // Total
        sb.append("[L]<b>TOTAL[R]Rs.${formatAmount(total)}</b>\n")
        sb.append("[L]Payment[R]${paymentMethod}\n")

        // Change
        if (paymentMethod == "CASH" && amountTendered > 0) {
            sb.append("[L]Tendered[R]Rs.${formatAmount(amountTendered)}\n")
            sb.append("[L]Change[R]Rs.${formatAmount(amountTendered - total)}\n")
        }

        sb.append(separator)

        // Footer
        sb.append("[C]$footerText\n")
        sb.append("[L]\n")
        sb.append("[L]\n")

        return sb.toString()
    }

    private fun formatAmount(amount: Double): String {
        return if (amount % 1.0 == 0.0) {
            amount.toLong().toString()
        } else {
            String.format("%.2f", amount)
        }
    }

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true
}
