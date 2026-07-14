package com.example.dianzicheng.data.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
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

    enum class ConnectionState { IDLE, SCANNING, CONNECTING, CONNECTED, MEASURING }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val scanRecord = result.scanRecord
            val serviceUuids = scanRecord?.serviceUuids
            val deviceName = device.name ?: scanRecord?.deviceName
            
            Log.d("BleScaleClient", "Scanning... Found: $deviceName [${device.address}] UUIDs: $serviceUuids")

            // Broaden search: Any device with our service UUID or AFU-WL name
            val isAfuName = deviceName?.startsWith("AFU-WL", ignoreCase = true) == true
            val hasService = serviceUuids?.any { it.uuid == SERVICE_UUID } == true

            if (isAfuName || hasService) {
                Log.d("BleScaleClient", "MATCH FOUND! Connecting to: $deviceName")
                stopScan()
                connect(device)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("BleScaleClient", "Scan failed with error: $errorCode")
            _connectionState.value = ConnectionState.IDLE
        }
    }

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    fun startScan() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            val reason = if (bluetoothAdapter == null) "No BT Adapter" else if (!bluetoothAdapter.isEnabled) "BT Disabled" else "Scanner Null"
            Log.e("BleScaleClient", "Bluetooth scanner not available: $reason")
            return
        }
        
        Log.d("BleScaleClient", "Starting scan for service: $SERVICE_UUID")
        _connectionState.value = ConnectionState.SCANNING
        
        // Remove filters temporarily to see all devices in logs if connection fails
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        scanner.startScan(null, settings, scanCallback)
    }

    fun stopScan() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner ?: return
        scanner.stopScan(scanCallback)
        if (_connectionState.value == ConnectionState.SCANNING) {
            _connectionState.value = ConnectionState.IDLE
        }
    }

    private fun connect(device: BluetoothDevice) {
        _connectionState.value = ConnectionState.CONNECTING
        Log.d("BleScaleClient", "Connecting to GATT...")
        bluetoothGatt = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            device.connectGatt(context, false, gattCallback)
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _connectionState.value = ConnectionState.CONNECTED
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                _connectionState.value = ConnectionState.IDLE
                bluetoothGatt = null
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Log.d("BleScaleClient", "Services discovered. Status: $status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(SERVICE_UUID)
                if (service == null) {
                    Log.e("BleScaleClient", "Target service NOT FOUND on device!")
                    // Log all available services to debug
                    gatt.services.forEach { s ->
                        Log.d("BleScaleClient", "Available Service: ${s.uuid}")
                    }
                }
                
                service?.characteristics?.forEach { characteristic ->
                    val props = characteristic.properties
                    if (props and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0 || 
                        props and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) {
                        
                        Log.d("BleScaleClient", "Subscribing to characteristic: ${characteristic.uuid}")
                        gatt.setCharacteristicNotification(characteristic, true)
                        
                        val descriptor = characteristic.getDescriptor(
                            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
                        )
                        if (descriptor != null) {
                            descriptor.value = if (props and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) {
                                BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                            } else {
                                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            }
                            gatt.writeDescriptor(descriptor)
                        }
                    }
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val data = characteristic.value
            Log.d("BleScaleClient", "Data received from ${characteristic.uuid}: ${data.joinToString(",") { "%02X".format(it) }}")
            AFUPacketParser.parseWeight(data)?.let {
                _weight.value = it.weightKg
                _isStable.value = it.isStable
                if (it.isStable) {
                   _connectionState.value = ConnectionState.MEASURING
                }
            }
            AFUPacketParser.parseImpedance(data)?.let {
                _impedance.value = it
            }
        }
    }
}
