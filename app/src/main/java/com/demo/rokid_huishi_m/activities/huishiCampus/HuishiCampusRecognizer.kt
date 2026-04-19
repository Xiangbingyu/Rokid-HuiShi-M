package com.demo.rokid_huishi_m.activities.huishiCampus

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import com.demo.rokid_huishi_m.activities.user.LoginManager
import com.rokid.cxr.client.extend.CxrApi
import com.rokid.cxr.client.extend.callbacks.PhotoResultCallback
import com.rokid.cxr.client.extend.listeners.CustomViewListener
import com.rokid.cxr.client.utils.ValueUtil
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Base64
import java.util.concurrent.TimeUnit

class HuishiCampusRecognizer {
    companion object {
        private const val TAG = "HuishiCampusRecognizer"
        private const val BASE_URL = "https://face-arec.hdu.edu.cn"
        private const val QUERY_ENDPOINT = "/students/query-by-photo"
        private const val DEVICE_ID = "AR-GLASS-004"
        private const val WEARER_USER_ID = "24320313"
        private const val CAPTURE_INTERVAL_MS = 1_800L
        private const val MANUAL_VIEW_DISMISS_DELAY_MS = 5_000L
        private const val PHOTO_QUALITY = 50
        private val PHOTO_SIZE = Size(320, 240)
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val HTTP_CLIENT = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private data class RecognitionResult(
        val studentInfoText: String,
        val confidence: Double?
    )

    private var isRecognizing = false
    private var latestRecognitionMessage = "点击开始识别"
    private var cachedRecognition: RecognitionResult? = null
    private var isRecognitionViewOpened = false
    private var isStatusViewShowing = false
    private var activeRecognitionSessionId = 0
    private var pendingManualDismissSessionId: Int? = null
    private var updateCustomViewMethodResolved = false
    private var updateCustomViewMethodName: String? = null
    private var isAttached = false
    private var onRecognizingChanged: ((Boolean) -> Unit)? = null
    private var onMessage: ((String) -> Unit)? = null
    private var onRecognitionUpdated: ((String) -> Unit)? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val manualDismissRunnable = Runnable {
        val sessionId = pendingManualDismissSessionId ?: return@Runnable
        pendingManualDismissSessionId = null
        if (isSessionActive(sessionId) && !isRecognizing) {
            pause()
        }
    }

    private val captureLoopRunnable = object : Runnable {
        override fun run() {
            if (!isRecognizing) return
            captureAndQueryStudent()
        }
    }

    private val customViewListener = object : CustomViewListener {
        override fun onIconsSent() {
            Log.d(TAG, "onIconsSent")
        }

        override fun onOpened() {
            Log.d(TAG, "onOpened")
            isRecognitionViewOpened = true
        }

        override fun onOpenFailed(p0: Int) {
            Log.e(TAG, "onOpenFailed: $p0")
            isRecognitionViewOpened = false
        }

        override fun onUpdated() {
            Log.d(TAG, "onUpdated")
            isRecognitionViewOpened = true
        }

        override fun onClosed() {
            Log.d(TAG, "onClosed")
            isRecognitionViewOpened = false
        }
    }

    fun bindUi(
        onRecognizingChanged: (Boolean) -> Unit,
        onMessage: (String) -> Unit,
        onRecognitionUpdated: (String) -> Unit
    ) {
        this.onRecognizingChanged = onRecognizingChanged
        this.onMessage = onMessage
        this.onRecognitionUpdated = onRecognitionUpdated
        onRecognizingChanged(isRecognizing)
        onRecognitionUpdated(latestRecognitionMessage)
    }

    fun unbindUi() {
        onRecognizingChanged = null
        onMessage = null
        onRecognitionUpdated = null
    }

    fun attach() {
        if (isAttached) return
        CxrApi.getInstance().setCustomViewListener(customViewListener)
        isAttached = true
    }

    fun start() {
        if (isRecognizing) return
        cancelPendingManualDismiss()
        beginRecognitionSession()
        isRecognizing = true
        dispatchRecognizingChanged(true)
        dispatchRecognitionUpdated("识别已开始，正在抓拍照片...")
        cachedRecognition?.let { showRecognitionOnGlasses(it) } ?: showPendingViewOnGlasses()
        mainHandler.post(captureLoopRunnable)
    }

    fun recognizeOnce() {
        val wasRecognizing = isRecognizing
        if (wasRecognizing) {
            stop(showToast = false)
        }
        cancelPendingManualDismiss()
        val sessionId = beginRecognitionSession()
        dispatchRecognitionUpdated("手动识别中，正在抓拍照片...")
        showPendingViewOnGlasses()
        dispatchMessage(if (wasRecognizing) "已关闭自动识别，开始手动识别" else "开始手动识别")
        captureAndQueryStudent(shouldRepeat = false, sessionId = sessionId)
    }

    fun stop(showToast: Boolean = true) {
        if (!isRecognizing) return
        cancelPendingManualDismiss()
        invalidateRecognitionSession()
        isRecognizing = false
        dispatchRecognizingChanged(false)
        dispatchRecognitionUpdated("识别已停止")
        mainHandler.removeCallbacks(captureLoopRunnable)
        pause()
        if (showToast) dispatchMessage("已停止识别")
    }

    fun pause() {
        cancelPendingManualDismiss()
        CxrApi.getInstance().closeCustomView()
        isRecognitionViewOpened = false
        isStatusViewShowing = false
    }

    fun release() {
        cancelPendingManualDismiss()
        invalidateRecognitionSession()
        stop(showToast = false)
        pause()
        CxrApi.getInstance().setCustomViewListener(null)
        isAttached = false
        unbindUi()
    }

    private fun captureAndQueryStudent() {
        captureAndQueryStudent(shouldRepeat = true, sessionId = activeRecognitionSessionId)
    }

    private fun captureAndQueryStudent(shouldRepeat: Boolean, sessionId: Int) {
        if (!isSessionActive(sessionId)) return
        val accessToken = LoginManager.getAccessToken().trim()
        if (accessToken.isBlank()) {
            if (!isSessionActive(sessionId)) return
            dispatchRecognitionUpdated("未检测到登录令牌，请先登录")
            dispatchMessage("未检测到登录令牌，请先登录")
            finishCaptureCycle(shouldRepeat, sessionId)
            return
        }

        val pictureCallback = PhotoResultCallback { status, imageData ->
            if (!isSessionActive(sessionId)) {
                Log.d(TAG, "忽略过期拍照回调，sessionId=$sessionId")
                return@PhotoResultCallback
            }
            if (status != ValueUtil.CxrStatus.RESPONSE_SUCCEED || imageData == null) {
                Log.e(TAG, "拍照失败，状态码：$status")
                dispatchCaptureFailure("拍照失败", shouldRepeat, sessionId)
                return@PhotoResultCallback
            }
            runCatching {
                Base64.getEncoder().encodeToString(imageData)
            }.onSuccess { base64Data ->
                if (!isSessionActive(sessionId)) {
                    Log.d(TAG, "忽略过期图片编码结果，sessionId=$sessionId")
                    return@onSuccess
                }
                dispatchRecognitionUpdated("拍照成功，正在识别...")
                queryStudentByPhoto(
                    accessToken = accessToken,
                    base64Photo = base64Data,
                    shouldRepeat = shouldRepeat,
                    sessionId = sessionId
                )
            }.onFailure {
                Log.e(TAG, "图片编码失败 - ${it.message}", it)
                dispatchCaptureFailure("图片编码失败", shouldRepeat, sessionId)
            }
        }

        when (
            CxrApi.getInstance().takeGlassPhotoGlobal(
                PHOTO_SIZE.width,
                PHOTO_SIZE.height,
                PHOTO_QUALITY,
                pictureCallback
            )
        ) {
            ValueUtil.CxrStatus.REQUEST_SUCCEED,
            ValueUtil.CxrStatus.REQUEST_WAITING -> {
                Log.d(TAG, "拍照请求发送成功")
            }

            else -> {
                Log.e(TAG, "拍照请求发送失败")
                dispatchCaptureFailure("拍照请求发送失败", shouldRepeat, sessionId)
            }
        }
    }

    private fun queryStudentByPhoto(
        accessToken: String,
        base64Photo: String,
        shouldRepeat: Boolean,
        sessionId: Int
    ) {
        val requestBody = JSONObject().apply {
            put("access_token", accessToken)
            put("wearer_user_id", WEARER_USER_ID)
            put("photo", base64Photo)
            put("device_id", DEVICE_ID)
        }.toString().toRequestBody(JSON_MEDIA_TYPE)

        val request = Request.Builder()
            .url("$BASE_URL$QUERY_ENDPOINT")
            .post(requestBody)
            .header("Content-Type", "application/json; charset=utf-8")
            .header("Authorization", "Bearer $accessToken")
            .build()

        HTTP_CLIENT.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (!isSessionActive(sessionId)) {
                    Log.d(TAG, "忽略过期网络失败回调，sessionId=$sessionId")
                    return
                }
                Log.e(TAG, "查询学生信息失败 - ${e.message}", e)
                dispatchCaptureFailure("网络请求失败", shouldRepeat, sessionId)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!isSessionActive(sessionId)) {
                    Log.d(TAG, "忽略过期网络响应，sessionId=$sessionId")
                    response.close()
                    return
                }
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Log.e(TAG, "查询学生信息失败，状态码: ${response.code}, body: $responseBody")
                    dispatchCaptureFailure("识别失败(${response.code})", shouldRepeat, sessionId)
                    return
                }
                val recognitionResult = parseRecognitionResult(responseBody)
                if (recognitionResult != null) {
                    if (!isSessionActive(sessionId)) {
                        Log.d(TAG, "忽略过期识别结果，sessionId=$sessionId")
                        return
                    }
                    applyRecognitionResult(recognitionResult)
                    if (!shouldRepeat) {
                        scheduleManualDismiss(sessionId)
                    }
                } else {
                    Log.e(TAG, "响应解析失败，保留上次识别结果")
                    dispatchCaptureFailure("返回数据解析失败", shouldRepeat, sessionId)
                    return
                }
                finishCaptureCycle(shouldRepeat, sessionId)
            }
        })
    }

    private fun parseRecognitionResult(responseBody: String): RecognitionResult? {
        return runCatching {
            val root = JSONObject(responseBody)
            val resultObject = root.optJSONObject("result") ?: root

            val studentInfo = resultObject.opt("student_info") ?: root.opt("student_info")
            val studentInfoText = buildStudentInfoText(studentInfo)

            val confidence = when {
                resultObject.has("confidence") -> resultObject.optDouble("confidence")
                root.has("confidence") -> root.optDouble("confidence")
                else -> Double.NaN
            }.takeIf { !it.isNaN() }

            RecognitionResult(
                studentInfoText = studentInfoText,
                confidence = confidence
            )
        }.onFailure {
            Log.e(TAG, "解析查询结果失败 - ${it.message}", it)
        }.getOrNull()
    }

    private fun buildStudentInfoText(studentInfo: Any?): String {
        return when (studentInfo) {
            null -> "未识别到学生信息"
            is JSONObject -> {
                val basicInfo = studentInfo.optJSONObject("basic_info") ?: studentInfo
                val name = basicInfo.optString("name", "")
                val studentId = basicInfo.optString("student_id", "")
                val department = basicInfo.optString("department", "")
                val major = basicInfo.optString("major", "")
                val className = basicInfo.optString("class", "")
                val grade = basicInfo.optString("grade", "")
                if (
                    name.isBlank() &&
                    studentId.isBlank() &&
                    department.isBlank() &&
                    major.isBlank() &&
                    className.isBlank() &&
                    grade.isBlank()
                ) {
                    "未识别到学生基本信息"
                } else {
                    listOf(
                        "姓名: ${name.ifBlank { "未知" }}",
                        "学号: ${studentId.ifBlank { "未知" }}",
                        "学院: ${department.ifBlank { "未知" }}",
                        "专业: ${major.ifBlank { "未知" }}",
                        "班级: ${className.ifBlank { "未知" }}",
                        "年级: ${grade.ifBlank { "未知" }}"
                    ).joinToString("\n")
                }
            }
            is JSONArray -> {
                if (studentInfo.length() == 0) return "未识别到学生信息"
                buildString {
                    for (index in 0 until studentInfo.length()) {
                        val item = studentInfo.opt(index)
                        append("学生${index + 1}：")
                        append(valueToDisplayString(item))
                        if (index != studentInfo.length() - 1) append("\n")
                    }
                }
            }

            else -> studentInfo.toString().ifBlank { "未识别到学生信息" }
        }
    }

    private fun valueToDisplayString(value: Any?): String {
        return when (value) {
            null -> "无"
            is JSONObject -> {
                val keys = value.keys()
                if (!keys.hasNext()) return "无"
                buildString {
                    while (keys.hasNext()) {
                        val key = keys.next()
                        append("$key: ${value.opt(key)}")
                        if (keys.hasNext()) append("\n")
                    }
                }
            }

            is JSONArray -> {
                if (value.length() == 0) return "[]"
                buildString {
                    for (i in 0 until value.length()) {
                        append(value.opt(i))
                        if (i != value.length() - 1) append(", ")
                    }
                }
            }

            else -> value.toString()
        }
    }

    private fun applyRecognitionResult(recognitionResult: RecognitionResult) {
        val isChanged = recognitionResult != cachedRecognition
        cachedRecognition = recognitionResult
        if (!isChanged) {
            Log.d(TAG, "识别结果未变化，执行眼镜端兜底刷新")
        }
        showRecognitionOnGlasses(recognitionResult)
        dispatchRecognitionUpdated(buildRecognitionMessage(recognitionResult))
    }

    private fun buildRecognitionMessage(result: RecognitionResult): String {
        return "识别成功\n${result.studentInfoText}\n置信度: ${
            result.confidence?.let { String.format("%.2f", it) } ?: "未知"
        }"
    }

    private fun showPendingViewOnGlasses() {
        showStatusOnGlasses(
            title = "慧视校园识别中...",
            message = "正在抓拍并识别，请稍候",
            titleColor = "#FFFFFFFF"
        )
    }

    private fun showRecognitionOnGlasses(result: RecognitionResult) {
        if (isRecognitionViewOpened && !isStatusViewShowing && updateRecognitionContentOnGlasses(result)) {
            return
        }
        val root = JSONObject().apply {
            put("type", "LinearLayout")
            put("props", JSONObject().apply {
                put("layout_width", "match_parent")
                put("layout_height", "match_parent")
                put("orientation", "vertical")
                put("gravity", "top|end")
                put("paddingTop", "100dp")
                put("paddingRight", "40dp")
                put("backgroundColor", "#00000000")
            })
            put("children", JSONArray().apply {
                put(
                    JSONObject().apply {
                        put("type", "LinearLayout")
                        put("props", JSONObject().apply {
                            put("layout_width", "260dp")
                            put("layout_height", "wrap_content")
                            put("orientation", "vertical")
                            put("padding", "12dp")
                        })
                        put("children", JSONArray().apply {
                            put(
                                JSONObject().apply {
                                    put("type", "TextView")
                                    put("props", JSONObject().apply {
                                        put("layout_width", "wrap_content")
                                        put("layout_height", "wrap_content")
                                        put("text", "学生信息")
                                        put("textSize", "18sp")
                                        put("textStyle", "bold")
                                        put("textColor", "#FFFFFFFF")
                                    })
                                }
                            )
                            put(
                                JSONObject().apply {
                                    put("type", "TextView")
                                    put("props", JSONObject().apply {
                                        put("id", "confidence_value")
                                        put("layout_width", "wrap_content")
                                        put("layout_height", "wrap_content")
                                        put("layout_marginTop", "6dp")
                                        put("text", buildConfidenceText(result.confidence))
                                        put("textSize", "14sp")
                                        put("textColor", "#FF90CAF9")
                                    })
                                }
                            )
                            put(
                                JSONObject().apply {
                                    put("type", "TextView")
                                    put("props", JSONObject().apply {
                                        put("id", "student_info_value")
                                        put("layout_width", "wrap_content")
                                        put("layout_height", "wrap_content")
                                        put("layout_marginTop", "6dp")
                                        put("text", result.studentInfoText)
                                        put("textSize", "14sp")
                                        put("textColor", "#FFFFFFFF")
                                    })
                                }
                            )
                        })
                    }
                )
            })
        }
        isStatusViewShowing = false
        isRecognitionViewOpened = openCustomView(root.toString())
    }

    private fun showFailureOnGlasses(message: String) {
        showStatusOnGlasses(
            title = "识别失败",
            message = message,
            titleColor = "#FFFF8A80"
        )
    }

    private fun showStatusOnGlasses(
        title: String,
        message: String,
        titleColor: String
    ) {
        val root = JSONObject().apply {
            put("type", "LinearLayout")
            put("props", JSONObject().apply {
                put("layout_width", "match_parent")
                put("layout_height", "match_parent")
                put("orientation", "vertical")
                put("gravity", "center")
                put("paddingLeft", "24dp")
                put("paddingRight", "24dp")
                put("backgroundColor", "#55000000")
            })
            put("children", JSONArray().apply {
                put(
                    JSONObject().apply {
                        put("type", "TextView")
                        put("props", JSONObject().apply {
                            put("layout_width", "wrap_content")
                            put("layout_height", "wrap_content")
                            put("text", title)
                            put("textSize", "22sp")
                            put("textColor", titleColor)
                            put("textStyle", "bold")
                        })
                    }
                )
                put(
                    JSONObject().apply {
                        put("type", "TextView")
                        put("props", JSONObject().apply {
                            put("layout_width", "wrap_content")
                            put("layout_height", "wrap_content")
                            put("layout_marginTop", "12dp")
                            put("text", message)
                            put("textSize", "16sp")
                            put("textColor", "#FFFFFFFF")
                        })
                    }
                )
            })
        }
        isStatusViewShowing = true
        isRecognitionViewOpened = openCustomView(root.toString())
    }

    private fun buildConfidenceText(confidence: Double?): String {
        return "置信度: ${confidence?.let { String.format("%.2f", it) } ?: "未知"}"
    }

    private fun updateRecognitionContentOnGlasses(result: RecognitionResult): Boolean {
        val updatePayload = JSONArray().apply {
            put(
                JSONObject().apply {
                    put("action", "update")
                    put("id", "confidence_value")
                    put("props", JSONObject().apply {
                        put("text", buildConfidenceText(result.confidence))
                    })
                }
            )
            put(
                JSONObject().apply {
                    put("action", "update")
                    put("id", "student_info_value")
                    put("props", JSONObject().apply {
                        put("text", result.studentInfoText)
                    })
                }
            )
        }
        val updateMethodName = resolveUpdateCustomViewMethodName()
        if (updateMethodName == null) {
            Log.d(TAG, "未发现 updateCustomView 接口，退化为整页刷新")
            return false
        }
        return runCatching {
            val method = CxrApi.getInstance().javaClass.getMethod(updateMethodName, String::class.java)
            val resultStatus = method.invoke(CxrApi.getInstance(), updatePayload.toString())
            when (resultStatus) {
                is Int -> {
                    val isSuccess = resultStatus == ValueUtil.CxrStatus.REQUEST_SUCCEED ||
                        resultStatus == ValueUtil.CxrStatus.REQUEST_WAITING
                    if (!isSuccess) {
                        Log.e(TAG, "局部更新发送失败，状态码：$resultStatus")
                    } else {
                        Log.d(TAG, "学生信息区域已局部更新")
                    }
                    isSuccess
                }

                else -> {
                    Log.d(TAG, "学生信息区域已局部更新")
                    true
                }
            }
        }.onFailure {
            Log.e(TAG, "局部更新失败 - ${it.message}", it)
        }.getOrDefault(false)
    }

    private fun resolveUpdateCustomViewMethodName(): String? {
        if (updateCustomViewMethodResolved) return updateCustomViewMethodName
        val method = CxrApi.getInstance().javaClass.methods.firstOrNull {
            it.name == "updateCustomView" &&
                it.parameterTypes.size == 1 &&
                it.parameterTypes[0].isAssignableFrom(String::class.java)
        }
        updateCustomViewMethodName = method?.name
        updateCustomViewMethodResolved = true
        return updateCustomViewMethodName
    }

    private fun openCustomView(customViewJson: String): Boolean {
        return when (CxrApi.getInstance().openCustomView(customViewJson)) {
            ValueUtil.CxrStatus.REQUEST_SUCCEED,
            ValueUtil.CxrStatus.REQUEST_WAITING -> {
                Log.d(TAG, "眼镜端视图发送成功")
                true
            }

            else -> {
                Log.e(TAG, "眼镜端视图发送失败")
                false
            }
        }
    }

    private fun dispatchRecognizingChanged(recognizing: Boolean) {
        onRecognizingChanged?.invoke(recognizing)
    }

    private fun dispatchMessage(message: String) {
        onMessage?.invoke(message)
    }

    private fun dispatchRecognitionUpdated(message: String) {
        latestRecognitionMessage = message
        onRecognitionUpdated?.invoke(message)
    }

    private fun scheduleNextCapture() {
        if (!isRecognizing) return
        mainHandler.removeCallbacks(captureLoopRunnable)
        mainHandler.postDelayed(captureLoopRunnable, CAPTURE_INTERVAL_MS)
    }

    private fun finishCaptureCycle(shouldRepeat: Boolean, sessionId: Int) {
        if (shouldRepeat && isSessionActive(sessionId)) {
            scheduleNextCapture()
        }
    }

    private fun dispatchCaptureFailure(baseMessage: String, shouldRepeat: Boolean, sessionId: Int) {
        if (!isSessionActive(sessionId)) return
        val message = if (shouldRepeat) "$baseMessage，1.8秒后重试" else baseMessage
        showFailureOnGlasses(message)
        dispatchRecognitionUpdated(message)
        if (!shouldRepeat) {
            scheduleManualDismiss(sessionId)
        }
        finishCaptureCycle(shouldRepeat, sessionId)
    }

    private fun beginRecognitionSession(): Int {
        activeRecognitionSessionId += 1
        return activeRecognitionSessionId
    }

    private fun invalidateRecognitionSession() {
        activeRecognitionSessionId += 1
    }

    private fun isSessionActive(sessionId: Int): Boolean {
        return sessionId == activeRecognitionSessionId
    }

    private fun scheduleManualDismiss(sessionId: Int) {
        cancelPendingManualDismiss()
        pendingManualDismissSessionId = sessionId
        mainHandler.postDelayed(manualDismissRunnable, MANUAL_VIEW_DISMISS_DELAY_MS)
    }

    private fun cancelPendingManualDismiss() {
        pendingManualDismissSessionId = null
        mainHandler.removeCallbacks(manualDismissRunnable)
    }
}
