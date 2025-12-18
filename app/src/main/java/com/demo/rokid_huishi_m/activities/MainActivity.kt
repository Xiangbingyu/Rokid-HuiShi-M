package com.demo.rokid_huishi_m.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import com.demo.rokid_huishi_m.activities.bluetoothConnection.BluetoothConnectionActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // 直接跳转到蓝牙连接页面，将其作为程序入口
        val intent = Intent(this, BluetoothConnectionActivity::class.java)
        startActivity(intent)
        finish()
    }
}