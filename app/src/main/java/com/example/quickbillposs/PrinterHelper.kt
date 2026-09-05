package com.example.quickbillposs

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.quickbillposs.data.model.CartItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

sealed class PrintResult {
    object Success : PrintResult()
    data class Error(val message: String) : PrintResult()
    object NoPrinter : PrintResult()
}

class PrinterHelper(private val context: Context) {

    companion object {
        // Known BLE Thermal Printer Service UUIDs
        private val PRINTER_SERVICE_UUIDS = listOf(
            UUID.fromString("000018f0-0000-1000-8000-00805f9b34fb"),
            UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"),
            UUID.fromString("49535343-fe7d-4ae5-8fa9-9fafd205e455")
        )

        // Known BLE Write Characteristic UUIDs
        private val WRITE_CHARACTERISTIC_UUIDS = listOf(
            UUID.fromString("00002af1-0000-1000-8000-00805f9b34fb"),
            UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb"),
            UUID.fromString("49535343-8841-43f4-a8d4-ecbe34729bb3")
        )

        // Classic Bluetooth SPP UUID
        private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        // Fast chunking & timing parameters
        private const val BLE_CHUNK_SIZE = 64
        private const val BLE_INTER_CHUNK_DELAY_MS = 5L
        private const val SOCKET_CHUNK_SIZE = 256
        private const val SOCKET_INTER_CHUNK_DELAY_MS = 2L
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        manager?.adapter
    }

    /** Returns true if required Bluetooth runtime permission is granted */
    fun hasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    /** Returns list of paired Bluetooth devices */
    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        if (!hasPermission()) return emptyList()
        return try {
            bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    /** Fast millisecond print via Classic Bluetooth SPP socket first, then BLE GATT fallback */
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
        savedMac: String = "",
        paperWidth: Int = 58
    ): PrintResult = withContext(Dispatchers.IO) {
        if (!hasPermission()) {
            return@withContext PrintResult.Error("Bluetooth permission not granted.")
        }
        if (!isBluetoothEnabled()) {
            return@withContext PrintResult.Error("Bluetooth is turned off.")
        }

        val device = if (savedMac.isNotBlank()) {
            getPairedDevices().find { it.address.equals(savedMac, ignoreCase = true) }
        } else {
            getPairedDevices().firstOrNull()
        }

        if (device == null) {
            return@withContext PrintResult.NoPrinter
        }

        val data = buildReceiptBytes(
            shopName = shopName,
            shopAddress = shopAddress,
            shopPhone = shopPhone,
            items = items,
            total = total,
            paymentMethod = paymentMethod,
            amountTendered = amountTendered,
            taxPercent = taxPercent,
            footerText = footerText,
            paperWidth = paperWidth
        )

        // 1. Try Classic Bluetooth SPP RFCOMM socket FIRST (Instant ~100ms response for 95%+ printers)
        val classicResult = printViaClassicSocket(device, data)
        if (classicResult is PrintResult.Success) {
            return@withContext classicResult
        }

        // 2. Fallback to BLE GATT if Classic socket fails
        return@withContext printViaBle(device, data)
    }

    /** Generates receipt bytes using EscPosEncoder */
    fun buildReceiptBytes(
        shopName: String,
        shopAddress: String,
        shopPhone: String,
        items: List<CartItem>,
        total: Double,
        paymentMethod: String,
        amountTendered: Double,
        taxPercent: Int,
        footerText: String,
        paperWidth: Int
    ): ByteArray {
        val encoder = EscPosEncoder(paperWidth)
        val dateStr = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date())
        val cleanShopName = shopName.ifBlank { "QUICKBILL POS" }.trim().uppercase()

        // 1. Header Shop Info (Centered)
        encoder.align("center")
            .bold(true)
            .size(1, 1)
            .text(cleanShopName)
            .newline()
            .bold(false)

        if (shopAddress.isNotBlank()) encoder.align("center").text(shopAddress).newline()
        if (shopPhone.isNotBlank()) encoder.align("center").text("Ph: $shopPhone").newline()

        encoder.separator('-')
        encoder.align("center").bold(true).text("TAX INVOICE").newline().bold(false)
        encoder.separator('-')

        // 2. Bill Meta Info (Left Aligned)
        encoder.align("left")
        encoder.tableRow("Date: $dateStr", "Pay: ${paymentMethod.uppercase()}")
        encoder.separator('-')

        // 3. Item Table Header & Rows (Left & Right Aligned)
        encoder.tableRow("ITEM", "AMOUNT")
        encoder.separator('-')

        for (item in items) {
            val label = item.label.ifBlank { "Item" }.trim()
            val lineTotal = "Rs.${formatAmount(item.lineTotal)}"

            if (item.quantity > 1) {
                encoder.align("left").text(label.take(encoder.maxChars)).newline()
                val qtyPriceStr = "  ${item.quantity} x Rs.${formatAmount(item.amount)}"
                encoder.tableRow(qtyPriceStr, lineTotal)
            } else {
                encoder.tableRow(label, lineTotal)
            }
        }

        encoder.separator('-')

        // 4. Summary & Tax
        val totalQty = items.sumOf { it.quantity }
        val taxAmount = if (taxPercent > 0) total * taxPercent / (100 + taxPercent) else 0.0
        val subTotal = total - taxAmount

        if (taxPercent > 0) {
            encoder.tableRow("Subtotal", "Rs.${formatAmount(subTotal)}")
            encoder.tableRow("GST ($taxPercent%)", "Rs.${formatAmount(taxAmount)}")
            encoder.separator('-')
        }

        encoder.tableRow("Total Items: ${items.size}", "Total Qty: $totalQty")
        encoder.separator('-')

        // 5. Grand Total (Bold)
        encoder.bold(true)
        encoder.tableRow("GRAND TOTAL", "Rs.${formatAmount(total)}")
        encoder.bold(false)
        encoder.separator('-')

        // 6. Cash Payment Breakdown
        if (paymentMethod.equals("CASH", ignoreCase = true) && amountTendered > 0) {
            encoder.tableRow("Amount Paid", "Rs.${formatAmount(amountTendered)}")
            val change = amountTendered - total
            if (change >= 0) {
                encoder.tableRow("Change Return", "Rs.${formatAmount(change)}")
            }
            encoder.separator('-')
        }

        // 7. Footer Message & Software Watermark (Centered)
        encoder.align("center")
        if (footerText.isNotBlank()) {
            encoder.text(footerText).newline()
        } else {
            encoder.text("Thank you! Visit again.").newline()
        }
        encoder.text("Powered by QuickBill POS").newline()

        // 8. Paper Feed & Cut
        encoder.feedLines(3)
        encoder.cut()

        return encoder.encode()
    }

    /** Fast Classic Bluetooth SPP socket print (Sub-200ms transfer) */
    @SuppressLint("MissingPermission")
    private suspend fun printViaClassicSocket(device: BluetoothDevice, data: ByteArray): PrintResult =
        withContext(Dispatchers.IO) {
            var socket: BluetoothSocket? = null
            try {
                // Try standard RFCOMM socket first
                socket = try {
                    device.createRfcommSocketToServiceRecord(SPP_UUID)
                } catch (_: Exception) {
                    // Insecure / reflective fallback for older/cheap BT chips
                    val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                    method.invoke(device, 1) as BluetoothSocket
                }

                socket.connect()
                val outputStream = socket.outputStream

                // Fast stream write in 256-byte chunks with minimal 2ms delay
                var offset = 0
                while (offset < data.size) {
                    val length = minOf(SOCKET_CHUNK_SIZE, data.size - offset)
                    outputStream.write(data, offset, length)
                    outputStream.flush()
                    offset += length
                    delay(SOCKET_INTER_CHUNK_DELAY_MS)
                }

                delay(50) // Short buffer drain
                try {
                    socket.close()
                } catch (_: Exception) {}
                PrintResult.Success
            } catch (e: Exception) {
                try {
                    socket?.close()
                } catch (_: Exception) {}
                PrintResult.Error("Classic socket failed: ${e.message}")
            }
        }

    /** Fast BLE GATT fallback print */
    @SuppressLint("MissingPermission")
    private suspend fun printViaBle(device: BluetoothDevice, data: ByteArray): PrintResult =
        suspendCancellableCoroutine { continuation ->
            val isResumed = AtomicBoolean(false)
            var bluetoothGatt: BluetoothGatt? = null

            fun finish(result: PrintResult) {
                if (isResumed.compareAndSet(false, true)) {
                    try {
                        bluetoothGatt?.disconnect()
                        bluetoothGatt?.close()
                    } catch (_: Exception) {}
                    if (continuation.isActive) {
                        continuation.resume(result)
                    }
                }
            }

            val gattCallback = object : BluetoothGattCallback() {
                override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        gatt.discoverServices()
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        if (!isResumed.get()) {
                            finish(PrintResult.Error("BLE GATT disconnected."))
                        }
                    }
                }

                override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        finish(PrintResult.Error("Failed to discover BLE services."))
                        return
                    }

                    var characteristic: BluetoothGattCharacteristic? = null

                    // 1. Try known printer service & write characteristic UUIDs
                    for (serviceUuid in PRINTER_SERVICE_UUIDS) {
                        val service = gatt.getService(serviceUuid) ?: continue
                        for (charUuid in WRITE_CHARACTERISTIC_UUIDS) {
                            val c = service.getCharacteristic(charUuid)
                            if (c != null) {
                                characteristic = c
                                break
                            }
                        }
                        if (characteristic != null) break
                    }

                    // 2. Fallback: search all services for any writable characteristic
                    if (characteristic == null) {
                        for (service in gatt.services) {
                            for (c in service.characteristics) {
                                val props = c.properties
                                if ((props and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0 ||
                                    (props and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0
                                ) {
                                    characteristic = c
                                    break
                                }
                            }
                            if (characteristic != null) break
                        }
                    }

                    if (characteristic == null) {
                        finish(PrintResult.Error("No writable BLE characteristic found."))
                        return
                    }

                    writeBleChunks(gatt, characteristic, data, ::finish)
                }
            }

            continuation.invokeOnCancellation {
                try {
                    bluetoothGatt?.disconnect()
                    bluetoothGatt?.close()
                } catch (_: Exception) {}
            }

            try {
                bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
                } else {
                    device.connectGatt(context, false, gattCallback)
                }
            } catch (e: Exception) {
                finish(PrintResult.Error("BLE connect exception: ${e.message}"))
            }
        }

    @SuppressLint("MissingPermission")
    private fun writeBleChunks(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        data: ByteArray,
        onFinish: (PrintResult) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var offset = 0
                while (offset < data.size) {
                    val chunkSize = minOf(BLE_CHUNK_SIZE, data.size - offset)
                    val chunk = data.copyOfRange(offset, offset + chunkSize)

                    val writeType = if ((characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0) {
                        BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    } else {
                        BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        gatt.writeCharacteristic(characteristic, chunk, writeType)
                    } else {
                        @Suppress("DEPRECATION")
                        characteristic.value = chunk
                        @Suppress("DEPRECATION")
                        characteristic.writeType = writeType
                        @Suppress("DEPRECATION")
                        gatt.writeCharacteristic(characteristic)
                    }

                    offset += chunkSize
                    delay(BLE_INTER_CHUNK_DELAY_MS)
                }
                delay(50) // Short buffer drain delay
                onFinish(PrintResult.Success)
            } catch (e: Exception) {
                onFinish(PrintResult.Error("Write error: ${e.message}"))
            }
        }
    }

    private fun formatAmount(amount: Double): String {
        return if (amount % 1.0 == 0.0) {
            amount.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", amount)
        }
    }
}
