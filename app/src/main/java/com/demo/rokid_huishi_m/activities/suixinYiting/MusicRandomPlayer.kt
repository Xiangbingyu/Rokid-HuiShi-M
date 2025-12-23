package com.demo.rokid_huishi_m.activities.suixinYiting

import android.util.Size
import android.util.Log
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

// 音乐播放工具类
class MusicRandomPlayer {
    companion object {
        private const val TAG = "MusicRandomPlayer"
    }
    
    // 定义回调接口
    interface OnAnalyzeCompleteListener {
        fun onAnalyzeComplete(musicUrl: String?)
    }

    // 降低图片分辨率和质量以减少Base64编码后的大小
    private val DEFAULT_PHOTO_SIZE = Size(320, 240)
    private val DEFAULT_PHOTO_QUALITY = 50

    // 拍照并发送图片到后端进行分析
    fun analyzeImageForMusic(mbti: String, mood: String, preference: String, onAnalyzeCompleteListener: OnAnalyzeCompleteListener) {
        // 拍照回调
        val pictureCallback = PhotoResultCallback { status, imageData ->
            when (status) {
                ValueUtil.CxrStatus.RESPONSE_SUCCEED -> {
                    // imageData 是 webP 格式的图片数据
                    Log.d(TAG, "拍照成功，图片数据长度：${imageData?.size}")
                    if (imageData != null) {
                        try {
                            val base64Data = Base64.getEncoder().encodeToString(imageData)
                            Log.d(TAG, "Base64转换成功，长度：${base64Data.length}")
                            // 发送图片到后端分析
                            sendImageToBackend(base64Data, mbti, mood, preference, onAnalyzeCompleteListener)
                        } catch (e: Exception) {
                            // Base64转换失败
                            e.printStackTrace()
                            onAnalyzeCompleteListener.onAnalyzeComplete(null)
                        }
                    } else {
                        Log.d(TAG, "拍照成功但图片数据为空")
                        onAnalyzeCompleteListener.onAnalyzeComplete(null)
                    }
                }
                else -> {
                    // 拍照失败
                    Log.d(TAG, "拍照失败，状态码：$status")
                    onAnalyzeCompleteListener.onAnalyzeComplete(null)
                }
            }
        }

        // 调用眼镜摄像头拍照
        when (CxrApi.getInstance().takeGlassPhotoGlobal(
            DEFAULT_PHOTO_SIZE.width,
            DEFAULT_PHOTO_SIZE.height,
            DEFAULT_PHOTO_QUALITY,
            pictureCallback
        )) {
            ValueUtil.CxrStatus.REQUEST_SUCCEED -> {
                // 拍照请求发送成功
                Log.d(TAG, "拍照请求发送成功")
            }
            ValueUtil.CxrStatus.REQUEST_FAILED -> {
                Log.d(TAG, "拍照请求发送失败")
                onAnalyzeCompleteListener.onAnalyzeComplete(null)
            }
            ValueUtil.CxrStatus.REQUEST_WAITING -> {
                Log.d(TAG, "拍照请求等待中")
            }
            else -> {
                Log.d(TAG, "拍照请求未知状态")
                onAnalyzeCompleteListener.onAnalyzeComplete(null)
            }
        }
    }

    // 将图片发送到后端进行分析
    private fun sendImageToBackend(base64Data: String, mbti: String, mood: String, preference: String, onAnalyzeCompleteListener: OnAnalyzeCompleteListener) {
        // 增加连接超时和读取超时时间
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        val url = "http://10.252.12.20:8000/llm/analyze_image/"
        val mediaType = "application/json; charset=utf-8".toMediaType()

        Log.d(TAG, "准备发送网络请求到: $url")

        // 创建请求体
        val requestBody = JSONObject()
        requestBody.put("base64_data", base64Data)
        requestBody.put("mbti", mbti)
        requestBody.put("mood", mood)
        requestBody.put("preference", preference)
        val requestBodyString = requestBody.toString().toRequestBody(mediaType)

        Log.d(TAG, "请求体大小: ${requestBodyString.contentLength()}")

        // 创建请求，确保设置正确的Content-Type头
        val request = Request.Builder()
            .url(url)
            .post(requestBodyString)
            .header("Content-Type", "application/json")
            .build()

        // 发送异步请求
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // 请求失败
                Log.e(TAG, "网络请求失败 - ${e.message}", e)
                onAnalyzeCompleteListener.onAnalyzeComplete(null)
            }

            override fun onResponse(call: Call, response: Response) {
                // 请求成功
                val responseBody = response.body
                if (responseBody != null) {
                    val responseData = responseBody.string()
                    Log.d(TAG, "后端响应状态码: ${response.code}")
                    Log.d(TAG, "后端响应内容: $responseData")

                    if (response.isSuccessful) {
                        try {
                            val jsonResponse = JSONObject(responseData)
                            // 从result.music_recommendation中获取music_url
                            val result = jsonResponse.optJSONObject("result")
                            val musicRecommendation = result?.optJSONObject("music_recommendation")
                            val musicUrl = musicRecommendation?.optString("music_url", null)
                            
                            if (musicUrl != null && musicUrl.isNotEmpty()) {
                                Log.d(TAG, "获取到音乐URL: $musicUrl")
                                onAnalyzeCompleteListener.onAnalyzeComplete(musicUrl)
                            } else {
                                Log.d(TAG, "后端未返回有效的音乐URL")
                                onAnalyzeCompleteListener.onAnalyzeComplete(null)
                            }
                        } catch (e: Exception) {
                            // JSON解析失败
                            Log.e(TAG, "JSON解析失败 - ${e.message}", e)
                            onAnalyzeCompleteListener.onAnalyzeComplete(null)
                        }
                    } else {
                        // 响应失败
                        Log.e(TAG, "后端请求失败，状态码: ${response.code}")
                        onAnalyzeCompleteListener.onAnalyzeComplete(null)
                    }
                } else {
                    Log.e(TAG, "后端响应体为空")
                    onAnalyzeCompleteListener.onAnalyzeComplete(null)
                }
            }
        })
    }
}
