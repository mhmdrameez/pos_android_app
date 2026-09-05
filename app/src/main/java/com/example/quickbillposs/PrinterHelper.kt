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

        // Chunking & timing parameters
        private const val CHUNK_SIZE = 20
        private const val INTER_CHUNK_DELAY_MS = 20L
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

    /** Prints receipt via BLE GATT or Classic Bluetooth SPP */
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

        // Try BLE GATT first
        val bleResult = printViaBle(device, data)
        if (bleResult is PrintResult.Success) {
            return@withContext bleResult
        }

        // Fallback to Classic Bluetooth RFCOMM SPP socket
        return@withContext printViaClassicSocket(device, data)
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

        // Header
        encoder.align("center")
            .bold(true)
            .size(2, 2)
            .text(shopName.ifBlank { "QUICKBILL POS" }.uppercase())
            .newline()
            .bold(false)
            .size(1, 1)

        if (shopAddress.isNotBlank()) encoder.text(shopAddress).newline()
        if (shopPhone.isNotBlank()) encoder.text("Ph: $shopPhone").newline()

        encoder.separator()
        encoder.align("center").bold(true).text("RECEIPT").newline().bold(false)
        encoder.align("left").text("Date: $dateStr").newline()
        encoder.separator()

        // Items Table
        encoder.tableRow("Item", "Amount")
        encoder.separator()

        for (item in items) {
            val label = item.label.ifBlank { "Item" }
            val lineTotal = "Rs.${formatAmount(item.lineTotal)}"
            if (item.quantity > 1) {
                encoder.align("left").text(label).newline()
                encoder.tableRow("  ${formatAmount(item.amount)} x ${item.quantity}", lineTotal)
            } else {
                encoder.tableRow(label, lineTotal)
            }
        }

        encoder.separator()

        // Tax
        val taxAmount = if (taxPercent > 0) total * taxPercent / (100 + taxPercent) else 0.0
        val subTotal = total - taxAmount
        if (taxPercent > 0) {
            encoder.tableRow("Subtotal", "Rs.${formatAmount(subTotal)}")
            encoder.tableRow("GST ($taxPercent%)", "Rs.${formatAmount(taxAmount)}")
            encoder.separator()
        }

        // Quantity and Total
        val totalQty = items.sumOf { it.quantity }
        encoder.tableRow("Total Qty", totalQty.toString())
        encoder.separator()

        encoder.align("center")
            .size(1, 2)
            .bold(true)
            .text("TOTAL: Rs.${formatAmount(total)}")
            .newline()
            .size(1, 1)
            .bold(false)

        encoder.separator()

        // Payment details
        encoder.align("left").text("Payment: $paymentMethod").newline()
        if (paymentMethod == "CASH" && amountTendered > 0) {
            encoder.text("Paid: Rs.${formatAmount(amountTendered)}").newline()
            val change = amountTendered - total
            if (change >= 0) {
                encoder.text("Change: Rs.${formatAmount(change)}").newline()
            }
        }

        encoder.separator()

        // Footer
        if (footerText.isNotBlank()) {
            encoder.align("center").text(footerText).newline()
        }

        encoder.feedLines(2)
        encoder.cut()

        return encoder.encode()
    }

    /** Print via BLE GATT with 20-byte chunking & 20ms delays */
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
                    val chunkSize = minOf(CHUNK_SIZE, data.size - offset)
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
                    delay(INTER_CHUNK_DELAY_MS)
                }
                delay(200) // Buffer drain delay
                onFinish(PrintResult.Success)
            } catch (e: Exception) {
                onFinish(PrintResult.Error("Write error: ${e.message}"))
            }
        }
    }

    /** Fallback: Print via Classic Bluetooth SPP socket */
    @SuppressLint("MissingPermission")
    private suspend fun printViaClassicSocket(device: BluetoothDevice, data: ByteArray): PrintResult =
        withContext(Dispatchers.IO) {
            try {
                val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket.connect()
                val outputStream = socket.outputStream

                var offset = 0
                while (offset < data.size) {
                    val length = minOf(CHUNK_SIZE, data.size - offset)
                    outputStream.write(data, offset, length)
                    outputStream.flush()
                    offset += length
                    delay(INTER_CHUNK_DELAY_MS)
                }

                delay(200)
                socket.close()
                PrintResult.Success
            } catch (e: Exception) {
                PrintResult.Error("Printer connection failed: ${e.message}")
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
