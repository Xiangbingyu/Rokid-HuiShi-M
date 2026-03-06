package com.demo.rokid_huishi_m.activities.bluetoothConnection

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.os.ParcelUuid
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rokid.cxr.client.extend.CxrApi
import com.rokid.cxr.client.extend.callbacks.BluetoothStatusCallback
import com.rokid.cxr.client.utils.ValueUtil
import com.demo.rokid_huishi_m.dataBeans.CONSTANT
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class DeviceItem(
    val device: BluetoothDevice?,
    val name: String,
    val macAddress: String,
    val rssi: Int
)

class BluetoothIniViewModel : ViewModel() {

    private val tag = "BluetoothIniViewModel"
    private val recordPrefName = "record"
    private val recordNameKey = "record_name"
    private val recordUuidKey = "record_uuid"
    private val recordMacAddressKey = "record_mac_address"

    private val _recordState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val recordState: StateFlow<Boolean> = _recordState.asStateFlow()

    private val _isScanning: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isScanningState: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _devicesList: MutableStateFlow<List<DeviceItem>> = MutableStateFlow(emptyList())
    val devicesList: StateFlow<List<DeviceItem>> = _devicesList.asStateFlow()

    private val _recordName: MutableStateFlow<String?> = MutableStateFlow(null)
    val recordName: StateFlow<String?> = _recordName.asStateFlow()

    private val _recordUUID: MutableStateFlow<String?> = MutableStateFlow(null)
    val recordUUID: StateFlow<String?> = _recordUUID.asStateFlow()

    private val _recordMacAddress: MutableStateFlow<String?> = MutableStateFlow(null)
    val recordMacAddress: StateFlow<String?> = _recordMacAddress.asStateFlow()

    private val _connecting: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val connecting: StateFlow<Boolean> = _connecting.asStateFlow()

    private val _connected: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _connectRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val connectRequests: SharedFlow<Unit> = _connectRequests.asSharedFlow()

    private val connectionState = object : BluetoothStatusCallback {
        override fun onConnectionInfo(
            uuid: String?,
            macAddress: String?,
            p2: String?,
            glassesType: Int
        ) {
            val isActuallyConnected = CxrApi.getInstance().isBluetoothConnected
            _connected.value = isActuallyConnected
            _connecting.value = false

            val glassesTypeLabel = if (glassesType == 1) "Display glasses" else "Non-display glasses"
            Log.d(tag, "onConnectionInfo: uuid=$uuid, macAddress=$macAddress, p2=$p2, p3=$glassesTypeLabel")

            uuid?.let { _recordUUID.value = it }
            macAddress?.let { _recordMacAddress.value = it }

            if (!isActuallyConnected && !_recordUUID.value.isNullOrBlank() && !_recordMacAddress.value.isNullOrBlank()) {
                viewModelScope.launch { _connectRequests.emit(Unit) }
            }
        }

        override fun onConnected() {
            Log.d(tag, "Bluetooth device connected successfully")
            _devicesList.value = emptyList()
            _connected.value = true
            _connecting.value = false
        }

        override fun onDisconnected() {
            Log.d(tag, "Bluetooth device disconnected")
            _connecting.value = false
            _connected.value = false
        }

        override fun onFailed(p0: ValueUtil.CxrBluetoothErrorCode?) {
            Log.e(tag, "Bluetooth connection failed with error: $p0")
            _connecting.value = false
            _connected.value = false
        }

    }

    private var connectionCheckJob: Job? = null

    init {
        startConnectionCheck()
    }

    private fun startConnectionCheck() {
        if (connectionCheckJob != null) return
        connectionCheckJob = viewModelScope.launch {
            while (isActive) {
                refreshConnectionStatus()
                delay(1000)
            }
        }
    }

    private fun stopConnectionCheck() {
        connectionCheckJob?.cancel()
        connectionCheckJob = null
    }
    
    override fun onCleared() {
        super.onCleared()
        stopConnectionCheck()
    }

    private val bleScannerCallback: ScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: android.bluetooth.le.ScanResult) {
            super.onScanResult(callbackType, result)
            val device = result.device
            val name = device.name ?: "Unknown"
            val macAddress = device.address
            val rssi = result.rssi
            Log.d(tag, "Found BLE device: name=$name, address=$macAddress, rssi=$rssi")
            addDevice(device, rssi)
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(tag, "BLE scan failed with error code: $errorCode")
        }
    }

    @SuppressLint("MissingPermission")
    fun toggleScan(bleScanner: BluetoothLeScanner?) {
        if (_isScanning.value) {
            stopScan(bleScanner)
            return
        }
        Log.d(tag, "Starting BLE scan with service UUID: ${CONSTANT.SERVICE_UUID}")
        clearDevices()
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid.fromString(CONSTANT.SERVICE_UUID))
            .build()
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        bleScanner?.startScan(mutableListOf(filter), scanSettings, bleScannerCallback)
        _isScanning.value = true
    }

    @SuppressLint("MissingPermission")
    fun stopScan(bleScanner: BluetoothLeScanner?) {
        if (!_isScanning.value) return
        Log.d(tag, "Stopping BLE scan")
        bleScanner?.stopScan(bleScannerCallback)
        _isScanning.value = false
    }

    @SuppressLint("MissingPermission")
    fun handleScan(bleScanner: BluetoothLeScanner?) = toggleScan(bleScanner)

    fun checkRecordState(context: Context) {
        val sharedPreferences =
            context.applicationContext.getSharedPreferences(recordPrefName, Context.MODE_PRIVATE)
        val recordName = sharedPreferences.getString(recordNameKey, null)
        val recordUUID = sharedPreferences.getString(recordUuidKey, null)
        val recordMacAddress = sharedPreferences.getString(recordMacAddressKey, null)
        recordName?.let { name ->
            recordUUID?.let { uuid ->
                recordMacAddress?.let { mac ->
                    this._recordName.value = name
                    this._recordUUID.value = uuid
                    this._recordMacAddress.value = mac
                    _recordState.value = true
                    return
                }
            }
        }
        _recordState.value = false
    }

    fun record(context: Context) {
        val sharedPreferences =
            context.applicationContext.getSharedPreferences(recordPrefName, Context.MODE_PRIVATE)
        sharedPreferences.edit {
            putString(recordNameKey, _recordName.value)
            putString(recordUuidKey, _recordUUID.value)
            putString(recordMacAddressKey, _recordMacAddress.value)
        }
        _recordState.value = true
    }


    /**
     * Add bluetooth device to the found list
     * @param device The BluetoothDevice instance
     * @param rssi The RSSI value
     */
    @SuppressLint("MissingPermission")
    fun addDevice(device: BluetoothDevice, rssi: Int) {
        val existingDevice = _devicesList.value.find { it.device == device }
        if (existingDevice != null) {
            updateRssi(device, rssi)
        } else {
            val newDevice = DeviceItem(device, device.name ?: "Unknown", device.address, rssi)
            _devicesList.value += newDevice
        }
    }

    /**
     * Update the RSSI value for a device if it exists in the list
     * @param device The BluetoothDevice instance
     * @param rssi The new RSSI value
     */
    fun updateRssi(device: BluetoothDevice, rssi: Int) {
        _devicesList.value = _devicesList.value.map {
            if (it.device == device) {
                it.copy(rssi = rssi)
            } else {
                it
            }
        }
    }

    /**
     * Clear the device list
     */
    fun clearDevices() {
        _devicesList.value = emptyList()
    }

    /**
     * Connect to Glasses's socket, the last step of the connection process
     */
    fun connectBTSocket(context: Context) {
        val uuid = _recordUUID.value
        val mac = _recordMacAddress.value
        if (uuid.isNullOrBlank() || mac.isNullOrBlank()) {
            _connecting.value = false
            return
        }
        _connecting.value = true
        try {
            CxrApi.getInstance().connectBluetooth(
                context.applicationContext,
                uuid,
                mac,
                connectionState,
                readRawFile(context.applicationContext),
                CONSTANT.CLIENT_SECRET.replace("-", "")
            )
        } catch (e: Exception) {
            Log.e(tag, "connectBluetooth error: ${e.message}", e)
            _connecting.value = false
        }

    }


    /**
     * Init Bluetooth connection after a device in fount device list is clicked
     * @param deviceItem The selected device item
     */
    fun deviceClicked(context: Context, deviceItem: DeviceItem) {
        _recordName.value = deviceItem.name
        _connecting.value = true
        CxrApi.getInstance().initBluetooth(context.applicationContext, deviceItem.device, connectionState)
    }

    /**
     * Read the SN authentication file
     */
    @Throws(Exception::class)
    fun readRawFile(context: Context): ByteArray {
        try {
            val inputStream =
                context.resources.openRawResource(CONSTANT.getSNResource())
            val bytes = inputStream.readBytes()
            return bytes
        } catch (e: Exception) {
            Log.e(tag, "Error reading raw file: ${e.message}", e)
            throw Exception("Error reading raw file")
        }
    }

    /**
     * Disconnect from the Bluetooth socket
     */
    fun disconnect() {
        _connecting.value = false
        _connected.value = false
        CxrApi.getInstance().deinitBluetooth()
    }
    
    fun toUseGlasses(context: Context) {
        context.startActivity(
            Intent(
                context,
                com.demo.rokid_huishi_m.activities.AppNavigationActivity::class.java
            )
        )
    }
    
    fun refreshConnectionStatus() {
        val isConnected = CxrApi.getInstance().isBluetoothConnected
        _connected.value = isConnected
        if (isConnected) _connecting.value = false
    }
}
