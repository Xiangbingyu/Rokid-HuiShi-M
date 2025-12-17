package com.demo.rokid_huishi_m.activities

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Divider
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.demo.rokid_huishi_m.R
import com.demo.rokid_huishi_m.activities.bluetoothConnection.BluetoothIniViewModel
import com.demo.rokid_huishi_m.activities.bluetoothConnection.DeviceItem
import com.demo.rokid_huishi_m.dataBeans.CONSTANT

class BluetoothInitActivity : ComponentActivity() {

    private val viewModel: BluetoothIniViewModel by viewModels()
    lateinit var btManager: BluetoothManager
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            // Permissions granted, can proceed with Bluetooth operations
        } else {
            Toast.makeText(
                this,
                "Bluetooth permissions are required to connect to devices",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BluetoothInitScreen(viewModel = viewModel, reconnect = {
                viewModel.connectBTSocket(this)
            }, scan = {
                if (checkBluetoothPermissions()) {
                    viewModel.handleScan(btManager.adapter.bluetoothLeScanner)
                } else {
                    requestBluetoothPermissions()
                }
            }, onItemClicked = { deviceItem ->
                viewModel.handleScan(btManager.adapter.bluetoothLeScanner)
                viewModel.deviceClicked(this, deviceItem)
            }, onToast = {
                Toast.makeText(
                    this@BluetoothInitActivity,
                    "Connecting...",
                    Toast.LENGTH_SHORT
                ).show()
            }, clear = {
                viewModel.clearDevices()
            }, doAfterConnected = {
                viewModel.record(this)
            }, disconnect = {
                viewModel.disconnect()
            }, toUseGlasses = {
                viewModel.toUseGlasses(this)
            })
        }
        btManager = getSystemService(BluetoothManager::class.java)
        viewModel.toConnect.observe(this) {
            if (it) {
                viewModel.connectBTSocket(this)
            }
        }
        viewModel.checkRecordState(this)
        viewModel.checkConnection()
        requestBluetoothPermissions()
    }
    
    private fun requestBluetoothPermissions() {
        requestPermissionLauncher.launch(CONSTANT.BLUETOOTH_PERMISSIONS)
    }
    
    private fun checkBluetoothPermissions(): Boolean {
        return CONSTANT.BLUETOOTH_PERMISSIONS.all {
            ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}

//Jetpack Compose

@SuppressLint("MissingPermission")
@Composable
fun BluetoothInitScreen(
    viewModel: BluetoothIniViewModel = viewModel(),
    reconnect: () -> Unit,
    scan: () -> Unit,
    onItemClicked: (DeviceItem?) -> Unit,
    onToast: () -> Unit,
    clear: () -> Unit,
    doAfterConnected: () -> Unit,
    disconnect: () -> Unit,
    toUseGlasses: () -> Unit
) {
    val recordState = viewModel.recordState.collectAsState()
    val scanning = viewModel.isScanningState.collectAsState()
    val devices = viewModel.devicesList.collectAsState()

    val recordName = viewModel.recordName.collectAsState()
    val recordMacAddress = viewModel.recordMacAddress.collectAsState()
    val recordUuid = viewModel.recordUUID.collectAsState()
    val connecting = viewModel.connecting.collectAsState()
    val connected = viewModel.connected.collectAsState()
    if (connected.value) {
        doAfterConnected()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "", modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
            )
            Text(
                text = if (recordState.value) {
                    "Device Recorded"
                } else {
                    "No Device Recorded"
                },
                modifier = Modifier
            )
            if (recordState.value) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)

                ) {
                    Text(
                        text = "Device Name:",
                        fontSize = 12.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .fillMaxWidth(0.3f)
                            .padding(end = 8.dp)
                    )
                    Text(
                        text = recordName.value ?: "",
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)

                ) {
                    Text(
                        text = "MAC Address:",
                        fontSize = 12.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .fillMaxWidth(0.3f)
                            .padding(end = 8.dp)
                    )
                    Text(
                        text = recordMacAddress.value ?: "",
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)

                ) {
                    Text(
                        text = "UUID:",
                        fontSize = 12.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .fillMaxWidth(0.3f)
                            .padding(end = 8.dp)
                    )
                    Text(
                        text = recordUuid.value ?: "",
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                }
                if (!connected.value) {
                    Button(onClick = reconnect, modifier = Modifier.fillMaxWidth(0.8f)) {
                        Text(text = "Reconnect")
                    }
                }

            }
            if (!connected.value) {
                Row(modifier = Modifier.fillMaxWidth(0.8f)) {
                    Button(
                        onClick = scan, modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp, end = 4.dp)
                            .offset(y = 12.dp)
                    ) {
                        Text(
                            text = if (!scanning.value) {
                                "Scan"
                            } else {
                                "Stop Scan"
                            }
                        )
                    }
                    if (!scanning.value && devices.value.isNotEmpty()) {
                        Button(
                            onClick = clear,
                            modifier = Modifier
                                .offset(y = 12.dp)
                                .padding(start = 4.dp, end = 4.dp)
                        ) {
                            Text(text = "Clear")
                        }
                    }
                }
            }

            // 扫描结果列表
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // 占据剩余可用空间
                    .padding(16.dp)
            ) {
                items(devices.value) { deviceItem ->
                    BluetoothDeviceItem(
                        item = deviceItem,
                        onClick = {
                            if (!connecting.value) {
                                onItemClicked(deviceItem)
                            } else {
                                onToast()
                            }
                        }
                    )
                    // 添加分隔线
                    Divider(color = Color.Gray, thickness = 0.5.dp)
                }
            }

            if (connected.value) {
                Button(onClick = disconnect, modifier = Modifier.fillMaxWidth(0.7f)) {
                    Text(text = "Disconnect")
                }
                Button(onClick = toUseGlasses, modifier = Modifier.fillMaxWidth(0.7f)) {
                    Text(text = "To Use Glasses")
                }
            }

        }
    }
}

@Composable
fun BluetoothDeviceItem(item: DeviceItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.name, fontSize = 16.sp, color = Color.Black)
            Text(text = item.macAddress, fontSize = 12.sp, color = Color.Gray)
        }
        Text(text = "${item.rssi} dBm", fontSize = 14.sp, color = Color.Blue)
    }
}