package com.demo.rokid_huishi_m.activities.user

import android.content.Context
import android.content.SharedPreferences

object LoginManager {
    private const val PREF_NAME = "login_preferences"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_EXPIRES_IN = "expires_in"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_ROLE = "user_role"
    private const val KEY_DEVICE_ID = "device_id"
    private const val KEY_LAST_LOGIN_AT = "last_login_at"
    
    private var sharedPreferences: SharedPreferences? = null
    
    private var accessToken: String = ""
    private var refreshToken: String = ""
    private var expiresIn: Long = 0L
    private var userId: String = ""
    private var userName: String = ""
    private var userRole: String = ""
    private var deviceId: String = ""
    private var lastLoginAt: Long = 0L

    fun init(context: Context) {
        if (sharedPreferences != null) return
        sharedPreferences = context.applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        loadFromPreferences()
    }

    private fun prefs(): SharedPreferences? = sharedPreferences

    private fun loadFromPreferences() {
        prefs()?.let {
            accessToken = it.getString(KEY_ACCESS_TOKEN, "") ?: ""
            refreshToken = it.getString(KEY_REFRESH_TOKEN, "") ?: ""
            expiresIn = it.getLong(KEY_EXPIRES_IN, 0L)
            userId = it.getString(KEY_USER_ID, "") ?: ""
            userName = it.getString(KEY_USER_NAME, "") ?: ""
            userRole = it.getString(KEY_USER_ROLE, "") ?: ""
            deviceId = it.getString(KEY_DEVICE_ID, "") ?: ""
            lastLoginAt = it.getLong(KEY_LAST_LOGIN_AT, 0L)
        }
    }

    fun setAccessToken(token: String) {
        accessToken = token
        prefs()?.edit()?.putString(KEY_ACCESS_TOKEN, token)?.apply()
    }

    fun getAccessToken(): String = accessToken

    fun setRefreshToken(token: String) {
        refreshToken = token
        prefs()?.edit()?.putString(KEY_REFRESH_TOKEN, token)?.apply()
    }

    fun getRefreshToken(): String = refreshToken

    fun setExpiresIn(time: Int) {
        expiresIn = System.currentTimeMillis() + (time * 1000L)
        prefs()?.edit()?.putLong(KEY_EXPIRES_IN, expiresIn)?.apply()
    }

    fun isAccessTokenExpired(): Boolean = System.currentTimeMillis() > expiresIn

    fun setUserId(id: String) {
        userId = id
        prefs()?.edit()?.putString(KEY_USER_ID, id)?.apply()
    }

    fun getUserId(): String = userId

    fun setUserName(name: String) {
        userName = name
        prefs()?.edit()?.putString(KEY_USER_NAME, name)?.apply()
    }

    fun getUserName(): String = userName

    fun setUserRole(role: String) {
        userRole = role
        prefs()?.edit()?.putString(KEY_USER_ROLE, role)?.apply()
    }

    fun getUserRole(): String = userRole

    fun getDeviceId(): String = deviceId

    fun getLastLoginAt(): Long = lastLoginAt

    fun isLoggedIn(): Boolean = accessToken.isNotEmpty() && !isAccessTokenExpired()

    fun saveSession(
        userId: String,
        accessToken: String,
        refreshToken: String,
        expiresInSeconds: Int,
        userName: String,
        userRole: String,
        deviceId: String = ""
    ) {
        val expireAt = System.currentTimeMillis() + (expiresInSeconds * 1000L)
        val loginAt = System.currentTimeMillis()
        this.userId = userId
        this.accessToken = accessToken
        this.refreshToken = refreshToken
        this.expiresIn = expireAt
        this.userName = userName
        this.userRole = userRole
        this.deviceId = deviceId
        this.lastLoginAt = loginAt
        prefs()?.edit()?.apply {
            putString(KEY_USER_ID, userId)
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putLong(KEY_EXPIRES_IN, expireAt)
            putString(KEY_USER_NAME, userName)
            putString(KEY_USER_ROLE, userRole)
            putString(KEY_DEVICE_ID, deviceId)
            putLong(KEY_LAST_LOGIN_AT, loginAt)
            apply()
        }
    }

    fun clearLoginInfo() {
        accessToken = ""
        refreshToken = ""
        expiresIn = 0L
        userId = ""
        userName = ""
        userRole = ""
        deviceId = ""
        lastLoginAt = 0L
        prefs()?.edit()?.clear()?.apply()
    }
}
