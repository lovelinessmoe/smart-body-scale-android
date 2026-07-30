package com.example.dianzicheng.data.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*

@SuppressLint("MissingPermission")
class BleScaleClient(private val context: Context) {
    private val bluetoothAdapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

    private var bluetoothGatt: BluetoothGatt? = null
    private val SERVICE_UUID = UUID.fromString("0000FFB0-0000-1000-8000-00805F9B34FB")

    private val _connectionState = MutableStateFlow(ConnectionState.IDLE)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _weight = MutableStateFlow(0.0)
    val weight: StateFlow<Double> = _weight

    private val _isStable = MutableStateFlow(false)
    val isStable: StateFlow<Boolean> = _isStable

    private val _impedance = MutableStateFlow(0.0)
    val impedance: StateFlow<Double> = _impedance

    private val _discoveredDevice = MutableStateFlow<Pair<String, String>?>(null)
    val discoveredDevice: StateFlow<Pair<String, String>?> = _discoveredDevice

    enum class ConnectionState { IDLE, SCANNING, CONNECTING, CONNECTED, MEASURING }

    var lastPairedMac: String? = null
    var onMacDiscovered: ((String) -> Unit)? = null

    private fun containsAscii(bytes: ByteArray?, keyword: String): Boolean {
        if (bytes == null || bytes.size < keyword.length) return false
        val kwUpper = keyword.uppercase(Locale.ROOT).toByteArray(Charsets.UTF_8)
        val kwLower = keyword.lowercase(Locale.ROOT).toByteArray(Charsets.UTF_8)
        for (i in 0..bytes.size - kwUpper.size) {
            var matchUpper = true
            var matchLower = true
            for (j in kwUpper.indices) {
                val b = bytes[i + j]
                if (b != kwUpper[j]) matchUpper = false
                if (b != kwLower[j]) matchLower = false
            }
            if (matchUpper || matchLower) return true
        }
        return false
    }

    private fun parseNameFromBytes(bytes: ByteArray?): String? {
        if (bytes == null || bytes.isEmpty()) return null
        var i = 0
        while (i < bytes.size) {
            val length = bytes[i].toInt() and 0xFF
            if (length == 0) break
            if (i + length >= bytes.size) break
            val type = bytes[i + 1].toInt() and 0xFF
            if ((type == 0x08 || type == 0x09) && length > 1) {
                return try {
                    String(bytes, i + 2, length - 1, Charsets.UTF_8).trim()
                } catch (e: Exception) {
                    null
                }
            }
            i += length + 1
        }
        return null
    }

    private fun isScaleAdvertisement(result: ScanResult): Boolean {
        val device = result.device
        val scanRecord = result.scanRecord
        val serviceUuids = scanRecord?.serviceUuids
        val rawBytes = scanRecord?.bytes
        val deviceName = device.name ?: scanRecord?.deviceName ?: parseNameFromBytes(rawBytes)

        Log.d("BleScaleClient", "Scanning... Found: '$deviceName' [${device.address}] UUIDs: $serviceUuids")

        val nameMatched = deviceName?.let { name ->
            name.contains("AFU", ignoreCase = true) ||
            name.contains("TZ", ignoreCase = true) ||
            name.contains("WL", ignoreCase = true) ||
            name.contains("A1", ignoreCase = true) ||
            name.contains("Scale", ignoreCase = true)
        } == true

        val rawAsciiMatched = containsAscii(rawBytes, "AFU") ||
                             containsAscii(rawBytes, "TZ") ||
                             containsAscii(rawBytes, "WL")

        val hasService = serviceUuids?.any { it.uuid == SERVICE_UUID } == true
        val isMatchedMac = !lastPairedMac.isNullOrEmpty() && device.address.equals(lastPairedMac, ignoreCase = true)

        var hasAcHeader = false
        if (rawBytes != null && rawBytes.size >= 6) {
            for (i in 0 until rawBytes.size - 5) {
                if ((rawBytes[i].toInt() and 0xFF) == 0xAC) {
                    hasAcHeader = true
                    break
                }
            }
        }

        return nameMatched || rawAsciiMatched || hasService || isMatchedMac || hasAcHeader
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            if (isScaleAdvertisement(result)) {
                val name = device.name ?: result.scanRecord?.deviceName ?: parseNameFromBytes(result.scanRecord?.bytes) ?: "体脂秤 (AFU-WL-TZ-A1)"
                Log.d("BleScaleClient", "MATCH FOUND! Found: $name [${device.address}]")
                _discoveredDevice.value = Pair(name, device.address)
                stopScan()
                onMacDiscovered?.invoke(device.address)
                connect(device)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("BleScaleClient", "Scan failed with error: $errorCode")
            _connectionState.value = ConnectionState.IDLE
            handler.postDelayed({
                if (_connectionState.value == ConnectionState.IDLE) {
                    startScan()
                }
            }, 800)
        }
    }

    fun connectMac(macAddress: String) {
        if (BluetoothAdapter.checkBluetoothAddress(macAddress)) {
            val device = bluetoothAdapter?.getRemoteDevice(macAddress)
            if (device != null) {
                onMacDiscovered?.invoke(macAddress)
                connect(device)
            }
        }
    }

    private var connectTimeoutRunnable: Runnable? = null
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    fun startScan() {
        _weight.value = 0.0
        _isStable.value = false
        _impedance.value = 0.0

        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            val reason = if (bluetoothAdapter == null) "No BT Adapter" else if (!bluetoothAdapter.isEnabled) "BT Disabled" else "Scanner Null"
            Log.e("BleScaleClient", "Bluetooth scanner not available: $reason")
            _connectionState.value = ConnectionState.IDLE
            return
        }

        // Clean up previous connection and watchdog timer
        connectTimeoutRunnable?.let { handler.removeCallbacks(it) }
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
        } catch (e: Exception) {}
        bluetoothGatt = null

        Log.d("BleScaleClient", "Starting active LE scan (pairedMac: $lastPairedMac)...")
        _connectionState.value = ConnectionState.SCANNING

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .build()

        try {
            scanner.stopScan(scanCallback)
        } catch (e: Exception) {}

        try {
            scanner.startScan(null, settings, scanCallback)
        } catch (e: Exception) {
            Log.e("BleScaleClient", "Failed to start LE scan", e)
            _connectionState.value = ConnectionState.IDLE
        }
    }

    fun stopScan() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner ?: return
        try {
            scanner.stopScan(scanCallback)
        } catch (e: Exception) {
            Log.w("BleScaleClient", "Error stopping scan", e)
        }
        if (_connectionState.value == ConnectionState.SCANNING) {
            _connectionState.value = ConnectionState.IDLE
        }
    }

    private fun connect(device: BluetoothDevice) {
        // ALWAYS stop scanning before initiating GATT connection to prevent radio collision
        stopScan()

        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
        } catch (e: Exception) {
            Log.w("BleScaleClient", "Error closing old GATT instance", e)
        }
        bluetoothGatt = null

        _connectionState.value = ConnectionState.CONNECTING
        Log.d("BleScaleClient", "Connecting to GATT: ${device.address}...")

        // Cancel previous connection watchdog
        connectTimeoutRunnable?.let { handler.removeCallbacks(it) }

        // 5-second watchdog: if GATT doesn't establish, close GATT & fall back to scan
        val timeoutRunnable = Runnable {
            if (_connectionState.value == ConnectionState.CONNECTING) {
                Log.w("BleScaleClient", "GATT connection watchdog timed out after 5s! Retrying LE scan...")
                try {
                    bluetoothGatt?.disconnect()
                    bluetoothGatt?.close()
                } catch (e: Exception) {}
                bluetoothGatt = null
                _connectionState.value = ConnectionState.IDLE
                startScan()
            }
        }
        connectTimeoutRunnable = timeoutRunnable
        handler.postDelayed(timeoutRunnable, 5000)

        bluetoothGatt = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            device.connectGatt(context, false, gattCallback)
        }
    }

    private var pendingSubscribeList = mutableListOf<BluetoothGattCharacteristic>()
    private var pendingSubscribeIndex = 0

    private fun sendHandshake(gatt: BluetoothGatt) {
        for (service in gatt.services) {
            val sUuid = service.uuid.toString().uppercase()
            if (sUuid.startsWith("00001800") || sUuid.startsWith("00001801")) continue
            for (char in service.characteristics) {
                val props = char.properties
                if (props and BluetoothGattCharacteristic.PROPERTY_WRITE != 0 ||
                    props and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) {

                    Log.d("BleScaleClient", "Sending handshake packet to: ${char.uuid}")
                    val handshakeData = byteArrayOf(0xFD.toByte(), 0x37, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x37)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        gatt.writeCharacteristic(char, handshakeData, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                    } else {
                        @Suppress("DEPRECATION")
                        char.value = handshakeData
                        @Suppress("DEPRECATION")
                        gatt.writeCharacteristic(char)
                    }
                }
            }
        }
    }

    private fun subscribeNextCharacteristic(gatt: BluetoothGatt) {
        if (pendingSubscribeIndex >= pendingSubscribeList.size) {
            Log.d("BleScaleClient", "All characteristic notifications subscribed successfully!")
            sendHandshake(gatt)
            _connectionState.value = ConnectionState.CONNECTED
            return
        }
        val characteristic = pendingSubscribeList[pendingSubscribeIndex]
        pendingSubscribeIndex++

        Log.d("BleScaleClient", "Subscribing (${pendingSubscribeIndex}/${pendingSubscribeList.size}): ${characteristic.uuid}")
        gatt.setCharacteristicNotification(characteristic, true)

        val descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
        if (descriptor != null) {
            val props = characteristic.properties
            descriptor.value = if (props and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) {
                BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            } else {
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            }
            val success = gatt.writeDescriptor(descriptor)
            Log.d("BleScaleClient", "writeDescriptor result for ${characteristic.uuid}: $success")
            if (!success) {
                subscribeNextCharacteristic(gatt)
            }
        } else {
            subscribeNextCharacteristic(gatt)
        }
    }

    private fun handleIncomingData(data: ByteArray, charUuid: String) {
        val hexStr = data.joinToString(",") { "%02X".format(it) }
        Log.d("BleScaleClient", "Data received from $charUuid: $hexStr")

        AFUPacketParser.parseWeight(data)?.let {
            _weight.value = it.weightKg
            if (it.weightKg > 0.0) {
                _isStable.value = it.isStable
                if (it.isStable) {
                    _connectionState.value = ConnectionState.MEASURING
                }
            } else {
                _isStable.value = false
            }
        }
        AFUPacketParser.parseImpedance(data)?.let {
            _impedance.value = it
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Log.d("BleScaleClient", "onConnectionStateChange status: $status, newState: $newState")
            connectTimeoutRunnable?.let { handler.removeCallbacks(it) }

            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e("BleScaleClient", "GATT connection failed with status: $status. Retrying via scan...")
                _connectionState.value = ConnectionState.IDLE
                try {
                    gatt.close()
                } catch (e: Exception) {}
                if (bluetoothGatt == gatt) {
                    bluetoothGatt = null
                }
                // Fallback to fresh scan on GATT error
                handler.post { startScan() }
                return
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _connectionState.value = ConnectionState.CONNECTING
                gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                _connectionState.value = ConnectionState.IDLE
                try {
                    gatt.close()
                } catch (e: Exception) {
                    Log.w("BleScaleClient", "Error closing GATT on disconnect", e)
                }
                if (bluetoothGatt == gatt) {
                    bluetoothGatt = null
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Log.d("BleScaleClient", "Services discovered. Status: $status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                pendingSubscribeList.clear()
                pendingSubscribeIndex = 0

                // Search ALL services for Notify/Indicate characteristics
                for (service in gatt.services) {
                    val sUuid = service.uuid.toString().uppercase()
                    if (sUuid.startsWith("00001800") || sUuid.startsWith("00001801")) continue

                    for (char in service.characteristics) {
                        val props = char.properties
                        if (props and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0 ||
                            props and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) {
                            pendingSubscribeList.add(char)
                        }
                    }
                }

                Log.d("BleScaleClient", "Found ${pendingSubscribeList.size} notification characteristics")
                subscribeNextCharacteristic(gatt)
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            Log.d("BleScaleClient", "onDescriptorWrite status: $status for ${descriptor.characteristic?.uuid}")
            subscribeNextCharacteristic(gatt)
        }

        // Android 13+ (API 33+) overload
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handleIncomingData(value, characteristic.uuid.toString())
        }

        // Older Android versions overload
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val data = characteristic.value ?: return
            handleIncomingData(data, characteristic.uuid.toString())
        }
    }
}

