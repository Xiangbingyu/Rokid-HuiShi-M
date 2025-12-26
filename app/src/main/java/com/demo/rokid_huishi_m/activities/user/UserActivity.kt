package com.demo.rokid_huishi_m.activities.user

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class UserActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 初始化LoginManager，确保从SharedPreferences加载数据
        LoginManager.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            UserScreen()
        }
    }
}

@Composable
fun UserScreen() {
    // 登录状态管理 - 从LoginManager获取持久化的登录状态
    val (isLoggedIn, setIsLoggedIn) = remember { mutableStateOf(LoginManager.isLoggedIn()) }
    val (userName, setUserName) = remember { mutableStateOf(LoginManager.getUserName()) }
    val (userRole, setUserRole) = remember { mutableStateOf(LoginManager.getUserRole()) }
    val (accessToken, setAccessToken) = remember { mutableStateOf(LoginManager.getAccessToken()) }
    
    // 登录窗口状态
    val (showLoginDialog, setShowLoginDialog) = remember { mutableStateOf(false) }
    val (userId, setUserId) = remember { mutableStateOf("") }
    val (password, setPassword) = remember { mutableStateOf("") }
    
    // 从SharedPreferences获取设备名称作为deviceId
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPreferences = context.getSharedPreferences("record", Context.MODE_PRIVATE)
    val deviceId = remember { sharedPreferences.getString("record_name", "AR001") ?: "AR001" }
    
    // 设备下线请求
    fun setDeviceOffline(accessToken: String, deviceId: String) {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        
        val url = "http://10.252.22.148:8000/system/device-offline"
        val mediaType = "application/json; charset=utf-8".toMediaType()
        
        val requestBody = JSONObject()
        requestBody.put("device_id", deviceId)
        val requestBodyString = requestBody.toString().toRequestBody(mediaType)
        
        Log.d("UserActivity", "发送设备下线请求数据: ${requestBody.toString()}")
        
        val request = Request.Builder()
            .url(url)
            .post(requestBodyString)
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $accessToken")
            .build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("UserActivity", "设备下线请求失败 - ${e.message}", e)
            }
            
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body
                if (responseBody != null) {
                    val responseData = responseBody.string()
                    Log.d("UserActivity", "设备下线响应: $responseData")
                    
                    if (response.isSuccessful) {
                        Log.d("UserActivity", "设备下线成功")
                    } else {
                        Log.e("UserActivity", "设备下线失败，状态码: ${response.code}")
                    }
                }
            }
        })
    }
    
    // 检查token是否过期，如果过期则自动下线设备并清除登录信息
    LaunchedEffect(Unit) {
        if (isLoggedIn && LoginManager.isAccessTokenExpired()) {
            setDeviceOffline(accessToken, deviceId)
            LoginManager.clearLoginInfo()
            setIsLoggedIn(false)
            setUserName("")
            setUserRole("")
            setAccessToken("")
        }
    }
    
    // 处理登录按钮点击
    fun handleLoginClick() {
        setShowLoginDialog(true)
    }
    
    // 设备上线请求
    fun setDeviceOnline(accessToken: String, userId: String, deviceId: String) {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        
        val url = "http://10.252.22.148:8000/system/device-online"
        val mediaType = "application/json; charset=utf-8".toMediaType()
        
        // 创建请求体
        val requestBody = JSONObject()
        requestBody.put("device_id", deviceId)
        requestBody.put("wearer_user_id", userId)
        requestBody.put("location", "测试位置")
        val requestBodyString = requestBody.toString().toRequestBody(mediaType)
        
        Log.d("UserActivity", "发送设备上线请求数据: ${requestBody.toString()}")
        
        // 创建请求
        val request = Request.Builder()
            .url(url)
            .post(requestBodyString)
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $accessToken")
            .build()
        
        // 发送异步请求
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("UserActivity", "设备上线请求失败 - ${e.message}", e)
            }
            
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body
                if (responseBody != null) {
                    val responseData = responseBody.string()
                    Log.d("UserActivity", "设备上线响应: $responseData")
                    
                    if (response.isSuccessful) {
                        Log.d("UserActivity", "设备上线成功")
                    } else {
                        Log.e("UserActivity", "设备上线失败，状态码: ${response.code}")
                    }
                }
            }
        })
    }
    
    // 处理登录请求
    fun handleLoginRequest() {
        // 创建OkHttpClient
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        
        val url = "http://10.252.22.148:8000/auth/login"
        val mediaType = "application/json; charset=utf-8".toMediaType()
        
        // 创建请求体
        val requestBody = JSONObject()
        requestBody.put("user_id", userId)
        requestBody.put("password", password)
        requestBody.put("device_id", deviceId)
        val requestBodyString = requestBody.toString().toRequestBody(mediaType)
        
        // 记录发送的请求数据以便调试
        Log.d("UserActivity", "发送的登录请求数据: ${requestBody.toString()}")
        
        // 创建请求
        val request = Request.Builder()
            .url(url)
            .post(requestBodyString)
            .header("Content-Type", "application/json")
            .build()
        
        // 发送异步请求
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("UserActivity", "登录请求失败 - ${e.message}", e)
            }
            
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body
                if (responseBody != null) {
                    val responseData = responseBody.string()
                    Log.d("UserActivity", "登录响应: $responseData")
                    
                    if (response.isSuccessful) {
                        try {
                            val jsonResponse = JSONObject(responseData)
                            val token = jsonResponse.optString("access_token", "")
                            val expiresIn = jsonResponse.optInt("expires_in")
                            val refreshToken = jsonResponse.optString("refresh_token", "")
                            val role = jsonResponse.optString("role", "")
                            val name = jsonResponse.optString("name", "")
                            
                            // 保存登录信息到全局管理器
                            LoginManager.setAccessToken(token)
                            LoginManager.setRefreshToken(refreshToken)
                            LoginManager.setExpiresIn(expiresIn)
                            LoginManager.setUserId(userId)
                            LoginManager.setUserName(name)
                            LoginManager.setUserRole(role)
                            
                            // 更新登录状态
                            setAccessToken(token)
                            setUserRole(role)
                            setUserName(name)
                            setIsLoggedIn(true)
                            setShowLoginDialog(false)
                            
                            Log.d("UserActivity", "登录成功，用户名: $name, 角色: $role")
                            
                            // 调用设备上线API
                            setDeviceOnline(token, userId, deviceId)
                        } catch (e: Exception) {
                            Log.e("UserActivity", "JSON解析失败 - ${e.message}", e)
                        }
                    } else {
                        Log.e("UserActivity", "登录失败，状态码: ${response.code}")
                    }
                }
            }
        })
    }
    
    // 处理退出登录
    fun handleLogout() {
        setDeviceOffline(accessToken, deviceId)
        LoginManager.clearLoginInfo()
        setIsLoggedIn(false)
        setUserName("")
        setUserRole("")
        setAccessToken("")
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "我的",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // 根据登录状态显示不同内容
        if (isLoggedIn) {
            Text(
                text = "欢迎, $userName",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = "角色: $userRole",
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "已登录",
                fontSize = 16.sp,
                color = androidx.compose.ui.graphics.Color.Green,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "退出登录",
                fontSize = 16.sp,
                color = androidx.compose.ui.graphics.Color.Red,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .clickable {
                        handleLogout()
                    }
            )
        } else {
            Text(
                text = "未登录",
                fontSize = 16.sp,
                color = androidx.compose.ui.graphics.Color.Red,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = "点击登录",
                fontSize = 16.sp,
                color = androidx.compose.ui.graphics.Color.Blue,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clickable {
                        handleLoginClick()
                    }
            )
        }
    }
    
    // 登录窗口
    if (showLoginDialog) {
        AlertDialog(
            onDismissRequest = {
                setShowLoginDialog(false)
            },
            title = { Text(text = "登录") },
            text = {
                Column {
                    // 学号输入
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "学号:", fontSize = 14.sp, modifier = Modifier.width(60.dp))
                        TextField(
                            value = userId,
                            onValueChange = { newValue -> setUserId(newValue) },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text(text = "请输入学号") }
                        )
                    }
                    
                    // 密码输入
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "密码:", fontSize = 14.sp, modifier = Modifier.width(60.dp))
                        TextField(
                            value = password,
                            onValueChange = { newValue -> setPassword(newValue) },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text(text = "请输入密码") }
                        )
                    }
                }
            },
            confirmButton = {
                Text(
                    text = "登录",
                    color = androidx.compose.ui.graphics.Color(0xFF4CAF50),
                    modifier = Modifier
                        .padding(8.dp)
                        .clickable {
                            handleLoginRequest()
                        }
                )
            },
            dismissButton = {
                Text(
                    text = "取消",
                    color = androidx.compose.ui.graphics.Color(0xFFF44336),
                    modifier = Modifier
                        .padding(8.dp)
                        .clickable {
                            setShowLoginDialog(false)
                        }
                )
            }
        )
    }
}
