package com.demo.rokid_huishi_m.activities.suixinYiting

import android.util.Log
import android.util.Size
import com.rokid.cxr.client.extend.CxrApi
import com.rokid.cxr.client.extend.callbacks.PhotoResultCallback
import com.rokid.cxr.client.utils.ValueUtil
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.Base64
import java.util.concurrent.TimeUnit

class MusicRandomPlayer {
    companion object {
        private const val TAG = "MusicRandomPlayer"
        private const val ANALYZE_URL = "http://10.252.98.57:8000/llm/analyze_image/"
        private const val DEFAULT_PHOTO_QUALITY = 50
        private val DEFAULT_PHOTO_SIZE = Size(320, 240)
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val HTTP_CLIENT = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    interface OnAnalyzeCompleteListener {
        fun onAnalyzeComplete(musicUrl: String?)
    }

    data class AnalyzePreference(
        val mbti: String,
        val mood: String,
        val preference: String
    )

    fun analyzeImageForMusic(
        mbti: String,
        mood: String,
        preference: String,
        onAnalyzeCompleteListener: OnAnalyzeCompleteListener
    ) {
        analyzeImageForMusic(
            params = AnalyzePreference(mbti = mbti, mood = mood, preference = preference),
            onComplete = onAnalyzeCompleteListener::onAnalyzeComplete
        )
    }

    fun analyzeImageForMusic(
        params: AnalyzePreference,
        onComplete: (String?) -> Unit
    ) {
        val pictureCallback = PhotoResultCallback { status, imageData ->
            if (status != ValueUtil.CxrStatus.RESPONSE_SUCCEED || imageData == null) {
                Log.d(TAG, "拍照失败，状态码：$status")
                onComplete(null)
                return@PhotoResultCallback
            }
            runCatching {
                Base64.getEncoder().encodeToString(imageData)
            }.onSuccess { base64Data ->
                sendImageToBackend(base64Data = base64Data, params = params, onComplete = onComplete)
            }.onFailure { throwable ->
                Log.e(TAG, "Base64转换失败 - ${throwable.message}", throwable)
                onComplete(null)
            }
        }

        when (CxrApi.getInstance().takeGlassPhotoGlobal(
            DEFAULT_PHOTO_SIZE.width,
            DEFAULT_PHOTO_SIZE.height,
            DEFAULT_PHOTO_QUALITY,
            pictureCallback
        )) {
            ValueUtil.CxrStatus.REQUEST_SUCCEED -> Log.d(TAG, "拍照请求发送成功")
            ValueUtil.CxrStatus.REQUEST_FAILED -> {
                Log.d(TAG, "拍照请求发送失败")
                onComplete(null)
            }
            ValueUtil.CxrStatus.REQUEST_WAITING -> Log.d(TAG, "拍照请求等待中")
            else -> {
                Log.d(TAG, "拍照请求未知状态")
                onComplete(null)
            }
        }
    }

    private fun sendImageToBackend(
        base64Data: String,
        params: AnalyzePreference,
        onComplete: (String?) -> Unit
    ) {
        val requestBody = JSONObject().apply {
            put("base64_data", base64Data)
            put("mbti", params.mbti)
            put("mood", params.mood)
            put("preference", params.preference)
        }.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(ANALYZE_URL)
            .post(requestBody)
            .header("Content-Type", "application/json")
            .build()

        HTTP_CLIENT.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "网络请求失败 - ${e.message}", e)
                onComplete(null)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Log.e(TAG, "后端请求失败，状态码: ${response.code}")
                    onComplete(null)
                    return
                }
                val musicUrl = parseMusicUrl(responseData)
                onComplete(musicUrl)
            }
        })
    }

    private fun parseMusicUrl(responseData: String): String? {
        return runCatching {
            JSONObject(responseData)
                .optJSONObject("result")
                ?.optJSONObject("music_recommendation")
                ?.optString("music_url", null)
                ?.takeIf { it.isNotBlank() }
        }.onFailure { throwable ->
            Log.e(TAG, "JSON解析失败 - ${throwable.message}", throwable)
        }.getOrNull()
    }
}
