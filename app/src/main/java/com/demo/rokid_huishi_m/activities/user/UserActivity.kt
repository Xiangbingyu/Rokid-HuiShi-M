package com.demo.rokid_huishi_m.activities.user

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.launch

private const val TAG = "UserActivity"
private const val BASE_URL = "http://10.252.22.148:8000"
private const val LOGIN_URL = "https://face-arec.hdu.edu.cn/auth/login"
private const val FIXED_DEVICE_ID = "test-device-001"

class UserActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LoginManager.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            UserScreen()
        }
    }
}

private data class UserUiState(
    val isLoggedIn: Boolean,
    val userName: String,
    val userRole: String,
    val accessToken: String
)

private data class LoginFormState(
    val userId: String = "",
    val password: String = ""
)

private data class LoginResult(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int,
    val role: String,
    val name: String
)

private class UserApi(
    private val client: OkHttpClient
) {
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    fun login(
        userId: String,
        password: String,
        deviceId: String,
        onSuccess: (LoginResult) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val payload = JSONObject().apply {
            put("user_id", userId)
            put("password", password)
            put("device_id", deviceId)
        }
        Log.d(TAG, "发送的登录请求数据: $payload")
        val requestBody = payload.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(LOGIN_URL)
            .post(requestBody)
            .header("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "登录请求失败 - ${e.message}", e)
                onFailure(e.message ?: "登录请求失败")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string().orEmpty()
                Log.d(TAG, "登录响应: $responseData")
                if (!response.isSuccessful) {
                    val message = "登录失败，状态码: ${response.code}"
                    Log.e(TAG, message)
                    onFailure(message)
                    return
                }
                runCatching {
                    val json = JSONObject(responseData)
                    LoginResult(
                        accessToken = json.optString("access_token", ""),
                        refreshToken = json.optString("refresh_token", ""),
                        expiresIn = json.optInt("expires_in"),
                        role = json.optString("role", ""),
                        name = json.optString("name", "")
                    )
                }.onSuccess { result ->
                    if (result.accessToken.isBlank()) {
                        onFailure("登录失败，access_token为空")
                    } else {
                        onSuccess(result)
                    }
                }.onFailure {
                    Log.e(TAG, "JSON解析失败 - ${it.message}", it)
                    onFailure("JSON解析失败")
                }
            }
        })
    }

    fun setDeviceOnline(accessToken: String, userId: String, deviceId: String) {
        val payload = JSONObject().apply {
            put("device_id", deviceId)
            put("wearer_user_id", userId)
            put("location", "测试位置")
        }
        Log.d(TAG, "发送设备上线请求数据: $payload")
        postJson(
            endpoint = "/system/device-online",
            payload = payload,
            accessToken = accessToken,
            requestLabel = "设备上线请求",
            successLabel = "设备上线"
        )
    }

    fun setDeviceOffline(accessToken: String, deviceId: String) {
        val payload = JSONObject().apply {
            put("device_id", deviceId)
        }
        Log.d(TAG, "发送设备下线请求数据: $payload")
        postJson(
            endpoint = "/system/device-offline",
            payload = payload,
            accessToken = accessToken,
            requestLabel = "设备下线请求",
            successLabel = "设备下线"
        )
    }

    private fun postJson(
        endpoint: String,
        payload: JSONObject,
        accessToken: String?,
        requestLabel: String,
        successLabel: String,
        onSuccess: ((String) -> Unit)? = null,
        onFailure: ((String) -> Unit)? = null
    ) {
        val requestBody = payload.toString().toRequestBody(mediaType)
        val requestBuilder = Request.Builder()
            .url("$BASE_URL$endpoint")
            .post(requestBody)
            .header("Content-Type", "application/json")

        if (!accessToken.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $accessToken")
        }

        client.newCall(requestBuilder.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "${requestLabel}失败 - ${e.message}", e)
                onFailure?.invoke(e.message ?: "${requestLabel}失败")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string().orEmpty()
                Log.d(TAG, "${successLabel}响应: $responseData")
                if (response.isSuccessful) {
                    Log.d(TAG, "${successLabel}成功")
                    onSuccess?.invoke(responseData)
                } else {
                    val message = "${successLabel}失败，状态码: ${response.code}"
                    Log.e(TAG, message)
                    onFailure?.invoke(message)
                }
            }
        })
    }
}

private fun createHttpClient(): OkHttpClient {
    return OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
}

@Composable
fun UserScreen() {
    val palette = userPalette()
    val deviceId = remember { FIXED_DEVICE_ID }
    val api = remember { UserApi(createHttpClient()) }
    val uiScope = rememberCoroutineScope()

    var userState by remember {
        mutableStateOf(
            UserUiState(
                isLoggedIn = LoginManager.isLoggedIn(),
                userName = LoginManager.getUserName(),
                userRole = LoginManager.getUserRole(),
                accessToken = LoginManager.getAccessToken()
            )
        )
    }
    var showLoginDialog by remember { mutableStateOf(false) }
    var formState by remember { mutableStateOf(LoginFormState()) }

    fun resetLoginState() {
        userState = UserUiState(
            isLoggedIn = false,
            userName = "",
            userRole = "",
            accessToken = ""
        )
    }

    fun handleLogout() {
        if (userState.accessToken.isNotBlank()) {
            api.setDeviceOffline(userState.accessToken, deviceId)
        }
        LoginManager.clearLoginInfo()
        resetLoginState()
    }

    fun handleLoginRequest() {
        api.login(
            userId = formState.userId,
            password = formState.password,
            deviceId = deviceId,
            onSuccess = { result ->
                uiScope.launch {
                    LoginManager.saveSession(
                        userId = formState.userId,
                        accessToken = result.accessToken,
                        refreshToken = result.refreshToken,
                        expiresInSeconds = result.expiresIn,
                        userName = result.name,
                        userRole = result.role,
                        deviceId = deviceId
                    )
                    userState = UserUiState(
                        isLoggedIn = true,
                        userName = result.name,
                        userRole = result.role,
                        accessToken = result.accessToken
                    )
                    showLoginDialog = false
                    api.setDeviceOnline(result.accessToken, formState.userId, deviceId)
                }
            },
            onFailure = { message ->
                Log.e(TAG, message)
            }
        )
    }

    LaunchedEffect(userState.isLoggedIn) {
        if (userState.isLoggedIn && LoginManager.isAccessTokenExpired()) {
            handleLogout()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        HeaderBar(
            title = "我的",
            palette = palette
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, palette.border),
                elevation = CardDefaults.cardElevation(0.dp),
                colors = CardDefaults.cardColors(containerColor = palette.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (userState.isLoggedIn) {
                        Text(
                            text = "欢迎, ${userState.userName}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.textMain
                        )
                        Text(
                            text = "角色: ${userState.userRole}",
                            fontSize = 14.sp,
                            color = palette.textMuted
                        )
                        Text(
                            text = "已登录",
                            fontSize = 14.sp,
                            color = palette.success
                        )
                    } else {
                        Text(
                            text = "未登录",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.textMain
                        )
                        Text(
                            text = "请登录以继续使用完整功能",
                            fontSize = 14.sp,
                            color = palette.textMuted
                        )
                    }
                }
            }
            Button(
                onClick = {
                    if (userState.isLoggedIn) {
                        handleLogout()
                    } else {
                        showLoginDialog = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.primary,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = if (userState.isLoggedIn) "退出登录" else "点击登录",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
    
    if (showLoginDialog) {
        AlertDialog(
            onDismissRequest = { showLoginDialog = false },
            title = { Text(text = "登录") },
            text = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "学号:", fontSize = 14.sp, modifier = Modifier.width(60.dp))
                        TextField(
                            value = formState.userId,
                            onValueChange = { newValue ->
                                formState = formState.copy(userId = newValue)
                            },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text(text = "请输入学号") }
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "密码:", fontSize = 14.sp, modifier = Modifier.width(60.dp))
                        TextField(
                            value = formState.password,
                            onValueChange = { newValue ->
                                formState = formState.copy(password = newValue)
                            },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text(text = "请输入密码") }
                        )
                    }
                }
            },
            confirmButton = {
                Text(
                    text = "登录",
                    color = palette.primary,
                    modifier = Modifier
                        .padding(8.dp)
                        .clickable(onClick = ::handleLoginRequest)
                )
            },
            dismissButton = {
                Text(
                    text = "取消",
                    color = palette.textMuted,
                    modifier = Modifier
                        .padding(8.dp)
                        .clickable { showLoginDialog = false }
                )
            }
        )
    }
}

private data class UserPalette(
    val primary: Color,
    val background: Color,
    val surface: Color,
    val textMain: Color,
    val textMuted: Color,
    val border: Color,
    val success: Color
)

private fun userPalette() = UserPalette(
    primary = Color(0xFF6A5ACD),
    background = Color(0xFFF9FAFB),
    surface = Color(0xFFFFFFFF),
    textMain = Color(0xFF111827),
    textMuted = Color(0xFF6B7280),
    border = Color(0xFFE5E7EB),
    success = Color(0xFF22C55E)
)

@Composable
private fun HeaderBar(
    title: String,
    palette: UserPalette
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
