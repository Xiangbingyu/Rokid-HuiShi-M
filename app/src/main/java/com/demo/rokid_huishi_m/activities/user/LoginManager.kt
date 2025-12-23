package com.demo.rokid_huishi_m.activities.user

import android.content.Context
import android.content.SharedPreferences

// 登录管理类，用于管理全局access_token
object LoginManager {
    private const val PREF_NAME = "login_preferences"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_EXPIRES_IN = "expires_in"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_ROLE = "user_role"
    
    private var sharedPreferences: SharedPreferences? = null
    
    private var accessToken: String = ""
    private var refreshToken: String = ""
    private var expiresIn: Long = 0L
    private var userId: String = ""
    private var userName: String = ""
    private var userRole: String = ""
    
    // 初始化方法，需要在Application或MainActivity中调用
    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        // 从SharedPreferences加载数据到内存
        loadFromPreferences()
    }
    
    // 从SharedPreferences加载数据
    private fun loadFromPreferences() {
        sharedPreferences?.let {
            accessToken = it.getString(KEY_ACCESS_TOKEN, "") ?: ""
            refreshToken = it.getString(KEY_REFRESH_TOKEN, "") ?: ""
            expiresIn = it.getLong(KEY_EXPIRES_IN, 0L)
            userId = it.getString(KEY_USER_ID, "") ?: ""
            userName = it.getString(KEY_USER_NAME, "") ?: ""
            userRole = it.getString(KEY_USER_ROLE, "") ?: ""
        }
    }
    
    // 设置access_token
    fun setAccessToken(token: String) {
        accessToken = token
        sharedPreferences?.edit()?.putString(KEY_ACCESS_TOKEN, token)?.apply()
    }
    
    // 获取access_token
    fun getAccessToken(): String {
        return accessToken
    }
    
    // 设置refresh_token
    fun setRefreshToken(token: String) {
        refreshToken = token
        sharedPreferences?.edit()?.putString(KEY_REFRESH_TOKEN, token)?.apply()
    }
    
    // 获取refresh_token
    fun getRefreshToken(): String {
        return refreshToken
    }
    
    // 设置过期时间
    fun setExpiresIn(time: Int) {
        expiresIn = System.currentTimeMillis() + (time * 1000L)
        sharedPreferences?.edit()?.putLong(KEY_EXPIRES_IN, expiresIn)?.apply()
    }
    
    // 检查access_token是否过期
    fun isAccessTokenExpired(): Boolean {
        return System.currentTimeMillis() > expiresIn
    }
    
    // 设置用户ID
    fun setUserId(id: String) {
        userId = id
        sharedPreferences?.edit()?.putString(KEY_USER_ID, id)?.apply()
    }
    
    // 获取用户ID
    fun getUserId(): String {
        return userId
    }
    
    // 设置用户名
    fun setUserName(name: String) {
        userName = name
        sharedPreferences?.edit()?.putString(KEY_USER_NAME, name)?.apply()
    }
    
    // 获取用户名
    fun getUserName(): String {
        return userName
    }
    
    // 设置用户角色
    fun setUserRole(role: String) {
        userRole = role
        sharedPreferences?.edit()?.putString(KEY_USER_ROLE, role)?.apply()
    }
    
    // 获取用户角色
    fun getUserRole(): String {
        return userRole
    }
    
    // 检查是否已登录
    fun isLoggedIn(): Boolean {
        return accessToken.isNotEmpty() && !isAccessTokenExpired()
    }
    
    // 清除登录信息
    fun clearLoginInfo() {
        // 清除内存数据
        accessToken = ""
        refreshToken = ""
        expiresIn = 0L
        userId = ""
        userName = ""
        userRole = ""
        
        // 清除SharedPreferences数据
        sharedPreferences?.edit()?.clear()?.apply()
    }
}
