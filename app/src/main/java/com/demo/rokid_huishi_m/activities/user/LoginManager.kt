package com.demo.rokid_huishi_m.activities.user

// 登录管理类，用于管理全局access_token
object LoginManager {
    private var accessToken: String = ""
    private var refreshToken: String = ""
    private var expiresIn: Long = 0L
    private var userId: String = ""
    private var userName: String = ""
    private var userRole: String = ""
    
    // 设置access_token
    fun setAccessToken(token: String) {
        accessToken = token
    }
    
    // 获取access_token
    fun getAccessToken(): String {
        return accessToken
    }
    
    // 设置refresh_token
    fun setRefreshToken(token: String) {
        refreshToken = token
    }
    
    // 获取refresh_token
    fun getRefreshToken(): String {
        return refreshToken
    }
    
    // 设置过期时间
    fun setExpiresIn(time: Int) {
        expiresIn = System.currentTimeMillis() + (time * 1000L)
    }
    
    // 检查access_token是否过期
    fun isAccessTokenExpired(): Boolean {
        return System.currentTimeMillis() > expiresIn
    }
    
    // 设置用户ID
    fun setUserId(id: String) {
        userId = id
    }
    
    // 获取用户ID
    fun getUserId(): String {
        return userId
    }
    
    // 设置用户名
    fun setUserName(name: String) {
        userName = name
    }
    
    // 获取用户名
    fun getUserName(): String {
        return userName
    }
    
    // 设置用户角色
    fun setUserRole(role: String) {
        userRole = role
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
        accessToken = ""
        refreshToken = ""
        expiresIn = 0L
        userId = ""
        userName = ""
        userRole = ""
    }
}
