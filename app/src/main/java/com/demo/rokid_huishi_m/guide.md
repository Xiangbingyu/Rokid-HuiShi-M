# SDK 导入
本章节以使用Kotlin DSL（build.gradle.kts）为例

## 配置Maven仓库
CXR-M SDK 采用Maven 在线管理SDK Package。

Maven 仓库地址: ("https://maven.rokid.com/repository/maven-public/")

找到settings.gradle.kts，并在dependencyResolutionManagement节点的repositories 中添加Maven仓库。

```kotlin
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\.android.*")
                includeGroupByRegex("com\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://maven.rokid.com/repository/maven-public/") }
        google()
        mavenCentral()
    }
}
```

## 依赖导入
CXR-M SDK Package ("com.rokid.cxr:client-m:1.0.1-20250812.080117-2")。

在build.gradle.kts 的dependencies 节点中添加依赖。注意：SDK 需要设置minSdk≥28。

```kotlin
//...Other Settings
android {
    //Other settings
    defaultConfig {
        //other settings
        minSdk = 28
    }
    //Other settings
}
dependencies {
    //....Others
    
    implementation("com.rokid.cxr:client-m:1.0.1-20250812.080117-2")
}
```

其他依赖项（如果和项目中已有版本冲突，请优先选用SDK 中对应的版本）：

```kotlin
implementation ("com.squareup.retrofit2:retrofit:2.9.0")
implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
implementation ("com.squareup.okhttp3:okhttp:4.9.3")
implementation ("org.jetbrains.kotlin:kotlin-stdlib:2.1.0")
implementation ("com.squareup.okio:okio:2.8.0")
implementation ("com.google.code.gson:gson:2.10.1")
implementation ("com.squareup.okhttp3:logging-interceptor:4.9.1")
```

## 权限申请

### 1. 声明权限
CXR-M SDK 需要申请网络、Wi-Fi、Bluetooth（蓝牙权限需要同步申请FINE_LOCATION 权限）等权限，在AndroidManifest.xml 中申请以下是最小权限集：

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.BLUETOOTH" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
    <uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
    <uses-permission android:name="android.permission.INTERNET" />

    <application>
        <!--Other Settings-->
    </application>

</manifest>
```

### 2. 动态申请权限
在CXR-M SDK 使用前，请先进行必要权限动态申请。注意在权限不足的情况下，SDK 将不可用。以下是一个简单示例：

```kotlin
class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "MainActivity"
        // Request Code
        const val REQUEST_CODE_PERMISSIONS = 100
        // Required Permissions
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }.toTypedArray()
    }
    // Permission
    private val permissionGrantedResult = MutableLiveData<Boolean?>()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Other Code
        // Request Permissions
        permissionGrantedResult.postValue(null)
        requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        
        // Observe Permission Result
        permissionGrantedResult.observe(this) {
            if (it == true) {
                // Permission All Granted
            } else {
                // Some Permission Denied or Not Started
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS.hashCode()) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            permissionGrantedResult.postValue(allGranted)
        }
    }
}
```

# 设备连接
在阅读本章节前，请注意已明确《SDK 导入》章节内容

## Bluetooth 连接

### 1. 查找蓝牙设备
通过Android 标准Bluetooth 接口进行设备查找。

扫描过程中可以使用UUID：`00009100-0000-1000-8000-00805f9b34fb`，来过滤Rokid 的设备。

以下是一个简单的示例：

```kotlin
package com.rokid.cxrandroiddocsample.helpers

//imports

/**
 * Bluetooth Helper
 * @author rokid
 * @date 2025/04/27
 * @param context Activity Register Context
 * @param initStatus Init Status
 * @param deviceFound Device Found
 */
class BluetoothHelper(
    val context: AppCompatActivity,
    val initStatus: (INIT_STATUS) -> Unit,
    val deviceFound: () -> Unit
) {
    companion object {
        const val TAG = "Rokid Glasses CXR-M"

        // Request Code
        const val REQUEST_CODE_PERMISSIONS = 100

        // Required Permissions
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }.toTypedArray()

        // Init Status
        enum class INIT_STATUS {
            NotStart,
            INITING,
            INIT_END
        }
    }

    // Scan Results
    val scanResultMap: ConcurrentHashMap<String, BluetoothDevice> = ConcurrentHashMap()

    // Bonded Devices
    val bondedDeviceMap: ConcurrentHashMap<String, BluetoothDevice> = ConcurrentHashMap()

    // Scanner
    private val scanner by lazy {
        adapter?.bluetoothLeScanner ?: run {
            Toast.makeText(context, "Bluetooth is not supported", Toast.LENGTH_SHORT).show()
            showRequestPermissionDialog()
            throw Exception("Bluetooth is not supported!!")
        }
    }

    // Bluetooth Enabled
    @SuppressLint("MissingPermission")
    private val bluetoothEnabled: MutableLiveData<Boolean> = MutableLiveData<Boolean>().apply {
        this.observe(context) {
            if (this.value == true) {
                initStatus.invoke(INIT_STATUS.INIT_END)
                startScan()
            } else {
                showRequestBluetoothEnableDialog()
            }
        }
    }

    // Bluetooth State Listener
    private val requestBluetoothEnable = context.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            adapter = manager?.adapter
        } else {
            showRequestBluetoothEnableDialog()
        }
    }

    // Bluetooth Adapter
    private var adapter: BluetoothAdapter? = null
        set(value) {
            field = value
            value?.let {
                if (!it.isEnabled) {
                    //to Enable it
                    requestBluetoothEnable.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                } else {
                    bluetoothEnabled.postValue(true)
                }
            }
        }

    // Bluetooth Manager
    private var manager: BluetoothManager? = null
        set(value) {
            field = value
            initStatus.invoke(INIT_STATUS.INITING)
            value?.let {
                adapter = it.adapter
            } ?: run {
                Toast.makeText(context, "Bluetooth is not supported", Toast.LENGTH_SHORT).show()
                showRequestPermissionDialog()
            }
        }

    // Permission Result
    val permissionResult: MutableLiveData<Boolean> = MutableLiveData<Boolean>().apply {
        this.observe(context) {
            if (it == true) {
                manager =
                    context.getSystemService(AppCompatActivity.BLUETOOTH_SERVICE) as BluetoothManager
            } else {
                showRequestPermissionDialog()
            }
        }
    }

    // Scan Listener
    val scanListener = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.let { r ->
                r.device.name?.let {
                    scanResultMap[it] = r.device
                    deviceFound.invoke()
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Toast.makeText(
                context,
                "Scan Failed $errorCode",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    // check permissions
    fun checkPermissions() {
        initStatus.invoke(INIT_STATUS.NotStart)
        context.requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        context.registerReceiver(
            bluetoothStateListener,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )
    }

    // Release
    @SuppressLint("MissingPermission")
    fun release() {
        context.unregisterReceiver(bluetoothStateListener)
        stopScan()
        permissionResult.postValue(false)
        bluetoothEnabled.postValue(false)
    }

    // Show Request Permission Dialog
    private fun showRequestPermissionDialog() {
        AlertDialog.Builder(context)
            .setTitle("Permission")
            .setMessage("Please grant the permission")
            .setPositiveButton("OK") { _, _ ->
                context.requestPermissions(
                    REQUIRED_PERMISSIONS,
                    REQUEST_CODE_PERMISSIONS
                )
            }
            .setNegativeButton("Cancel") { _, _ ->
                Toast.makeText(
                    context,
                    "Permission does not granted, FINISH",
                    Toast.LENGTH_SHORT
                ).show()
                context.finish()
            }
            .show()
    }

    // Show Request Bluetooth Enable Dialog
    private fun showRequestBluetoothEnableDialog() {
        AlertDialog.Builder(context)
            .setTitle("Bluetooth")
            .setMessage("Please enable the bluetooth")
            .setPositiveButton("OK") { _, _ ->
                requestBluetoothEnable.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            }
            .setNegativeButton("Cancel") { _, _ ->
                Toast.makeText(
                    context,
                    "Bluetooth does not enabled, FINISH",
                    Toast.LENGTH_SHORT
                ).show()
                context.finish()
            }
            .show()
    }

    // Start Scan
    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startScan() {
        scanResultMap.clear()
        val connectedList = getConnectedDevices()
        for (device in connectedList) {
            device.name?.let {
                if (it.contains("Glasses", false)) {
                    bondedDeviceMap[it] = device
                    deviceFound.invoke()
                }
            }
        }

        adapter?.bondedDevices?.forEach { d ->
            d.name?.let {
                if (it.contains("Glasses", false)) {
                    if (bondedDeviceMap[it] == null) {
                        bondedDeviceMap[it] = d
                    }
                }
                deviceFound.invoke()
            }
        }

        try {
            scanner.startScan(
                listOf<ScanFilter>(
                    ScanFilter.Builder()
                        .setServiceUuid(ParcelUuid.fromString("00009100-0000-1000-8000-00805f9b34fb"))//Rokid Glasses Service
                        .build()
                ), ScanSettings.Builder().build(),
                scanListener
            )
        } catch (e: Exception) {
            Toast.makeText(context, "Scan Failed ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Stop Scan
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopScan() {
        scanner.stopScan(scanListener)
    }

    // Get Connected Devices
    @SuppressLint("MissingPermission")
    private fun getConnectedDevices(): List<BluetoothDevice> {
        return adapter?.bondedDevices?.filter { device ->
            try {
                val isConnected =
                    device::class.java.getMethod("isConnected").invoke(device) as Boolean
                isConnected
            } catch (_: Exception) {
                Toast.makeText(context, "Get Connected Devices Failed", Toast.LENGTH_SHORT).show()
                false
            }
        } ?: emptyList()
    }

    // Bluetooth State Listener
    val bluetoothStateListener = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                when (state) {
                    BluetoothAdapter.STATE_OFF -> {
                        initStatus.invoke(INIT_STATUS.NotStart)
                        bluetoothEnabled.postValue(false)
                    }
                }
            }
        }
    }

}
```

### 2. 初始化蓝牙获取蓝牙信息
设备初始化，通过CXR_M SDK 的CxrApi 类进行控制。

初始化蓝牙模块方法：`fun initBluetooth(context: Context, device: BluetoothDevice, callback: BluetoothStatusCallback)`。

以下是简单的用法示例：

```kotlin
/**
 * Init Bluetooth
 * 
 * @param context   Application Context
 * @param device     Bluetooth Device
 */
fun initDevice(context: Context, device: BluetoothDevice){
    /**
     * Init Bluetooth
     *
     * @param context   Application Context
     * @param device     Bluetooth Device
     * @param callback   Bluetooth Status Callback
     */
    CxrApi.getInstance().initBluetooth(context, device,  object : BluetoothStatusCallback{
        /**
         * Connection Info
         *
         * @param socketUuid   Socket UUID
         * @param macAddress   Classic Bluetooth MAC Address
         * @param rokidAccount Rokid Account
         * @param glassesType  Device Type, 0-no display, 1-have display
         */
        override fun onConnectionInfo(
            socketUuid: String?,
            macAddress: String?,
            rokidAccount: String?,
            glassesType: Int
        ) {
            socketUuid?.let { uuid ->
                macAddress?.let { address->
                    connect(context, uuid, address)
                }?:run {
                    Log.e(TAG, "macAddress is null")
                }
            }?:run{
                Log.e(TAG, "socketUuid is null")
            }
        }

        /**
         * Connected
         */
        override fun onConnected() {
        }

        /**
         * Disconnected
         */
        override fun onDisconnected() {
        }

        /**
         * Failed
         *
         * @param errorCode   Error Code:
         * @see ValueUtil.CxrBluetoothErrorCode
         * @see ValueUtil.CxrBluetoothErrorCode.PARAM_INVALID  Parameter Invalid
         * @see ValueUtil.CxrBluetoothErrorCode.BLE_CONNECT_FAILED BLE Connect Failed
         * @see ValueUtil.CxrBluetoothErrorCode.SOCKET_CONNECT_FAILED Socket Connect Failed
         * @see ValueUtil.CxrBluetoothErrorCode.UNKNOWN Unknown
         */
        override fun onFailed(p0: ValueUtil.CxrBluetoothErrorCode?) {
        }

    })
}
```
其中`BluetoothStatusCallback`是蓝牙信息监听接口。

其中：

- `fun onConnected()`：当蓝牙连接成功时回调
- `fun onDisconnected()`：当蓝牙连接丢失时回调
- `fun onConnectionInfo(p0: String?, p1: String?)`：设备信息更新接口
  - `socketUuid`：设备UUID
  - `macAddress`：设备mac 地址
  - `rokidAccount`：Rokid 账号
  - `glassesType`：眼镜类型

### 3. 连接蓝牙模块
设备连接，通过CXR_M SDK 的CxrApi 类进行控制。

初始化蓝牙模块方法：`fun connectBluetooth(context: Context, socketUuid: String, macAddress: String, callback: BluetoothStatusCallback)`。

以下是简单的用法示例：

```kotlin
/**
 * Connect
 *  
 * @param context   Application Context
 * @param socketUuid   Socket UUID
 * @param macAddress   Classic Bluetooth MAC Address
 */
fun connect(context: Context, socketUuid: String, macAddress: String){
    /**
     * Connect
     */
    CxrApi.getInstance().connectBluetooth(context, socketUuid, macAddress, object : BluetoothStatusCallback{
        /**
         * Connection Info
         *
         * @param socketUuid   Socket UUID
         * @param macAddress   Classic Bluetooth MAC Address
         * @param rokidAccount Rokid Account
         * @param glassesType  Device Type, 0-no display, 1-have display
         */
        override fun onConnectionInfo(
            socketUuid: String?,
            macAddress: String?,
            rokidAccount: String?,
            glassesType: Int
        ) {
            //
        }

        /**
         * Connected
         */
        override fun onConnected() {
            Log.d(TAG, "Connected")
        }

        /**
         * Disconnected
         */
        override fun onDisconnected() {
            Log.d(TAG, "Disconnected")
        }

        /**
         * Failed
         *
         * @param errorCode   Error Code:
         * @see ValueUtil.CxrBluetoothErrorCode
         * @see ValueUtil.CxrBluetoothErrorCode.PARAM_INVALID  Parameter Invalid
         * @see ValueUtil.CxrBluetoothErrorCode.BLE_CONNECT_FAILED BLE Connect Failed
         * @see ValueUtil.CxrBluetoothErrorCode.SOCKET_CONNECT_FAILED Socket Connect Failed
         * @see ValueUtil.CxrBluetoothErrorCode.UNKNOWN Unknown
         */
        override fun onFailed(p0: ValueUtil.CxrBluetoothErrorCode?) {
            Log.e(TAG, "Failed")
        }

    })
}
```
其中`BluetoothStatusCallback`是蓝牙信息监听接口。

其中：

- `fun onConnected()`：当蓝牙连接成功时回调
- `fun onDisconnected()`：当蓝牙连接丢失时回调
- `fun onConnectionInfo(p0: String?, p1: String?)`：设备信息更新接口
  - `socketUuid`：设备UUID
  - `macAddress`：设备mac 地址
  - `rokidAccount`：Rokid 账号
  - `glassesType`：眼镜类型

### 4. 获取蓝牙通信模块连接状态
可以通过`fun isBluetoothConnected():Boolean`方法获取当前蓝牙通信模块的连接状态。

返回值:

- `true`：蓝牙通信模块已连接
- `false`：蓝牙模块未连接

简单示例如下：

```kotlin
/**
 * Get Connection Status
 *
 * @return  Connection Status: true-connected, false-disconnected
 */
fun getConnectionStatus(): Boolean{
    return CxrApi.getInstance().isBluetoothConnected
}
```

### 5. 反初始化蓝牙
可以通过`fun deinitBluetooth()`。

简单示例如下：

```kotlin
/**
 * DeInit Bluetooth
 */
fun deInit(){
    CxrApi.getInstance().deinitBluetooth()
}
```

### 6. 蓝牙重连
可以通过`fun connectBluetooth(context: Context, sockectUuid: String, macAddress: String, callback: BluetoothStatusCallback)`方法进行蓝牙重连。

其中：

- `sockectUuid`: UUID
- `macAddress`: Mac 地址

```kotlin
/**
 * Connect
 *  
 * @param context   Application Context
 * @param socketUuid   Socket UUID
 * @param macAddress   Classic Bluetooth MAC Address
 */
fun connect(context: Context, socketUuid: String, macAddress: String){
    /**
     * Connect
     */
    CxrApi.getInstance().connectBluetooth(context, socketUuid, macAddress, object : BluetoothStatusCallback{
        /**
         * Connection Info
         *
         * @param socketUuid   Socket UUID
         * @param macAddress   Classic Bluetooth MAC Address
         * @param rokidAccount Rokid Account
         * @param glassesType  Device Type, 0-no display, 1-have display
         */
        override fun onConnectionInfo(
            socketUuid: String?,
            macAddress: String?,
            rokidAccount: String?,
            glassesType: Int
        ) {
            //
        }

        /**
         * Connected
         */
        override fun onConnected() {
            Log.d(TAG, "Connected")
        }

        /**
         * Disconnected
         */
        override fun onDisconnected() {
            Log.d(TAG, "Disconnected")
        }

        /**
         * Failed
         *
         * @param errorCode   Error Code:
         * @see ValueUtil.CxrBluetoothErrorCode
         * @see ValueUtil.CxrBluetoothErrorCode.PARAM_INVALID  Parameter Invalid
         * @see ValueUtil.CxrBluetoothErrorCode.BLE_CONNECT_FAILED BLE Connect Failed
         * @see ValueUtil.CxrBluetoothErrorCode.SOCKET_CONNECT_FAILED Socket Connect Failed
         * @see ValueUtil.CxrBluetoothErrorCode.UNKNOWN Unknown
         */
        override fun onFailed(p0: ValueUtil.CxrBluetoothErrorCode?) {
            Log.e(TAG, "Failed")
        }

    })
}
```
