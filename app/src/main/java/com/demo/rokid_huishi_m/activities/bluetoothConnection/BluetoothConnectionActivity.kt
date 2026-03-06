package com.demo.rokid_huishi_m.activities.bluetoothConnection

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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.DevicesOther
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SignalCellularAlt1Bar
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.demo.rokid_huishi_m.dataBeans.CONSTANT
import kotlinx.coroutines.launch

class BluetoothConnectionActivity : ComponentActivity() {
    private val viewModel: BluetoothIniViewModel by viewModels()
    private lateinit var btManager: BluetoothManager
    
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
        btManager = getSystemService(BluetoothManager::class.java)

        setContent {
            val actions = rememberBluetoothActions(
                viewModel = viewModel,
                btManager = btManager,
                checkPermissions = ::checkBluetoothPermissions,
                requestPermissions = ::requestBluetoothPermissions
            )
            BluetoothConnectionScreen(
                viewModel = viewModel,
                actions = actions
            )
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.connectRequests.collect {
                    viewModel.connectBTSocket(this@BluetoothConnectionActivity)
                }
            }
        }
        viewModel.checkRecordState(this)
        requestBluetoothPermissions()
    }
    
    override fun onResume() {
        super.onResume()
        viewModel.refreshConnectionStatus()
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

private data class BluetoothActions(
    val onReconnect: () -> Unit,
    val onScan: () -> Unit,
    val onClear: () -> Unit,
    val onDeviceClick: (DeviceItem) -> Unit,
    val onDisconnect: () -> Unit,
    val onToUseGlasses: () -> Unit
)

@SuppressLint("MissingPermission")
@Composable
private fun rememberBluetoothActions(
    viewModel: BluetoothIniViewModel,
    btManager: BluetoothManager,
    checkPermissions: () -> Boolean,
    requestPermissions: () -> Unit
): BluetoothActions {
    val context = LocalContext.current
    return remember(viewModel, btManager, context) {
        BluetoothActions(
            onReconnect = { viewModel.connectBTSocket(context) },
            onScan = {
                if (checkPermissions()) {
                    viewModel.toggleScan(btManager.adapter.bluetoothLeScanner)
                } else {
                    requestPermissions()
                }
            },
            onClear = viewModel::clearDevices,
            onDeviceClick = { item ->
                viewModel.stopScan(btManager.adapter.bluetoothLeScanner)
                viewModel.deviceClicked(context, item)
            },
            onDisconnect = viewModel::disconnect,
            onToUseGlasses = { viewModel.toUseGlasses(context) }
        )
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun BluetoothConnectionScreen(
    viewModel: BluetoothIniViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    actions: BluetoothActions
) {
    val context = LocalContext.current
    val recordState = viewModel.recordState.collectAsState().value
    val scanning = viewModel.isScanningState.collectAsState().value
    val devices = viewModel.devicesList.collectAsState().value
    val recordName = viewModel.recordName.collectAsState().value
    val recordMacAddress = viewModel.recordMacAddress.collectAsState().value
    val recordUuid = viewModel.recordUUID.collectAsState().value
    val connecting = viewModel.connecting.collectAsState().value
    val connected = viewModel.connected.collectAsState().value

    LaunchedEffect(connected) {
        if (connected) viewModel.record(context)
    }

    if (connected) {
        ConnectedScreen(
            recordName = recordName,
            recordMacAddress = recordMacAddress,
            recordUuid = recordUuid,
            onDisconnect = actions.onDisconnect,
            onToUseGlasses = actions.onToUseGlasses
        )
    } else {
        ConnectionScreen(
            recordState = recordState,
            recordName = recordName,
            recordMacAddress = recordMacAddress,
            recordUuid = recordUuid,
            scanning = scanning,
            devices = devices,
            connecting = connecting,
            onReconnect = actions.onReconnect,
            onScan = actions.onScan,
            onClear = actions.onClear,
            onItemClicked = actions.onDeviceClick
        )
    }
}

@Composable
private fun ConnectionScreen(
    recordState: Boolean,
    recordName: String?,
    recordMacAddress: String?,
    recordUuid: String?,
    scanning: Boolean,
    devices: List<DeviceItem>,
    connecting: Boolean,
    onReconnect: () -> Unit,
    onScan: () -> Unit,
    onClear: () -> Unit,
    onItemClicked: (DeviceItem) -> Unit
) {
    val palette = connectionPalette()
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        HeaderBar(
            title = "Device Connection",
            palette = palette
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                DeviceInfoCard(
                    title = "Device Recorded",
                    recordState = recordState,
                    recordName = recordName,
                    recordMacAddress = recordMacAddress,
                    recordUuid = recordUuid,
                    palette = palette
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (recordState) {
                        PrimaryButton(
                            text = "Reconnect",
                            icon = Icons.Filled.BluetoothConnected,
                            palette = palette,
                            onClick = onReconnect
                        )
                    }
                    PrimaryButton(
                        text = if (scanning) "Stop Scan" else "Scan",
                        icon = if (scanning) Icons.Filled.StopCircle else Icons.Filled.Bluetooth,
                        palette = palette,
                        onClick = onScan
                    )
                }
            }
            item {
                Text(
                    text = "Scanned Devices",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textMuted
                )
            }
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = palette.surface),
                    border = BorderStroke(1.dp, palette.border)
                ) {
                    if (devices.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "No devices found",
                                color = palette.textMuted,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        Column {
                            devices.forEachIndexed { index, item ->
                                BluetoothDeviceItem(
                                    item = item,
                                    palette = palette,
                                    onClick = {
                                        if (!connecting) {
                                            onItemClicked(item)
                                        } else {
                                            Toast.makeText(context, "Connecting...", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                                if (index != devices.lastIndex) {
                                    Divider(color = palette.border, thickness = 1.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectedScreen(
    recordName: String?,
    recordMacAddress: String?,
    recordUuid: String?,
    onDisconnect: () -> Unit,
    onToUseGlasses: () -> Unit
) {
    val palette = connectedPalette()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        HeaderBar(
            title = "Device Recorded",
            palette = palette
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DeviceInfoCard(
                title = "Device Recorded",
                recordState = true,
                recordName = recordName,
                recordMacAddress = recordMacAddress,
                recordUuid = recordUuid,
                palette = palette
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(palette.success, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Connected",
                    color = palette.success,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PrimaryButton(
                text = "Disconnect",
                icon = Icons.Filled.LinkOff,
                palette = palette,
                onClick = onDisconnect
            )
            PrimaryButton(
                text = "To Use Glasses",
                icon = Icons.Filled.Visibility,
                palette = palette,
                onClick = onToUseGlasses
            )
        }
    }
}

@Composable
private fun BluetoothDeviceItem(
    item: DeviceItem,
    palette: BluetoothPalette,
    onClick: () -> Unit
) {
    val displayName = if (item.name.isBlank()) "Unknown Device" else item.name
    val isUnknown = displayName == "Unknown Device"
    val signalStrong = item.rssi >= -70
    val iconBackground = if (isUnknown) Color(0xFFF3F4F6) else Color(0xFFEDE9FE)
    val iconTint = if (isUnknown) palette.textMuted else palette.primary
    val signalColor = if (signalStrong) palette.primary else palette.textMuted
    val signalIcon = if (signalStrong) Icons.Filled.SignalCellularAlt else Icons.Filled.SignalCellularAlt1Bar
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .alpha(if (isUnknown) 0.7f else 1f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconBackground, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isUnknown) Icons.Filled.Devices else Icons.Filled.DevicesOther,
                    contentDescription = null,
                    tint = iconTint
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textMain
                )
                Text(
                    text = item.macAddress,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = palette.textMuted
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = signalIcon,
                contentDescription = null,
                tint = signalColor
            )
            Text(
                text = "${item.rssi} dBm",
                fontSize = 13.sp,
                color = signalColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun HeaderBar(
    title: String,
    palette: BluetoothPalette
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.surface)
            .border(BorderStroke(1.dp, palette.border))
            .statusBarsPadding()
            .padding(top = 12.dp, bottom = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.textMain
        )
    }
}

@Composable
private fun PrimaryButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    palette: BluetoothPalette,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = palette.primary,
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(50)
    ) {
        Icon(imageVector = icon, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun RecordField(
    label: String,
    value: String,
    valueColor: Color,
    palette: BluetoothPalette,
    mono: Boolean,
    smallValue: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = palette.textMuted,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = if (smallValue) 12.sp else 14.sp,
            color = valueColor,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
            fontWeight = if (smallValue) FontWeight.Medium else FontWeight.SemiBold
        )
    }
}

@Composable
private fun DeviceInfoCard(
    title: String,
    recordState: Boolean,
    recordName: String?,
    recordMacAddress: String?,
    recordUuid: String?,
    palette: BluetoothPalette
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        border = BorderStroke(1.dp, palette.border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textMain
            )
            Divider(color = palette.border, thickness = 1.dp)
            if (recordState) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    RecordField(
                        label = "Device Name",
                        value = recordName ?: "",
                        valueColor = palette.textMain,
                        palette = palette,
                        mono = false
                    )
                    RecordField(
                        label = "MAC Address",
                        value = recordMacAddress ?: "",
                        valueColor = palette.textMain,
                        palette = palette,
                        mono = true
                    )
                    RecordField(
                        label = "UUID",
                        value = recordUuid ?: "",
                        valueColor = palette.primary,
                        palette = palette,
                        mono = true,
                        smallValue = true
                    )
                }
            } else {
                Text(
                    text = "No Device Recorded",
                    fontSize = 14.sp,
                    color = palette.textMuted
                )
            }
        }
    }
}

private data class BluetoothPalette(
    val primary: Color,
    val background: Color,
    val surface: Color,
    val textMain: Color,
    val textMuted: Color,
    val border: Color,
    val success: Color
)

private fun connectionPalette() = BluetoothPalette(
    primary = Color(0xFF6A5ACD),
    background = Color(0xFFF9FAFB),
    surface = Color(0xFFFFFFFF),
    textMain = Color(0xFF111827),
    textMuted = Color(0xFF6B7280),
    border = Color(0xFFE5E7EB),
    success = Color(0xFF22C55E)
)

private fun connectedPalette() = BluetoothPalette(
    primary = Color(0xFF725AC1),
    background = Color(0xFFF8F9FA),
    surface = Color(0xFFFFFFFF),
    textMain = Color(0xFF1A1A1A),
    textMuted = Color(0xFF666666),
    border = Color(0xFFE0E0E0),
    success = Color(0xFF22C55E)
)
