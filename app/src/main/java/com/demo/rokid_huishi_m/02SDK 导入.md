***本章节以使用Kotlin DSL（build.gradle.kts）为例***

# 开发者认证与SN 鉴权文件获取

## 1 注册/登录Rokid 开发者账号

### 1.1 注册

如果没有开发者账号，可以到https://ar.rokid.com Rokid 开放平台注册开发者账号，根据提示一步步的完成注册。

![image-20251114135658273]( https://ota-g.rokidcdn.com/toB/Document/CXR/1.0.4/registerOrLogin.png)

![image-20251114135828942]( https://ota-g.rokidcdn.com/toB/Document/CXR/1.0.4/register.png)

![image-20251114135922583]( https://ota-g.rokidcdn.com/toB/Document/CXR/1.0.4/register_step1.png)

### 1.2 登录开发者账号

![image-20251114140401415]( https://ota-g.rokidcdn.com/toB/Document/CXR/1.0.4/login.png)

### 1.3 实名认证

登录成功后，需要进行实名认证。

![image-20251114140902910]( https://ota-g.rokidcdn.com/toB/Document/CXR/1.0.4/authorize.png)

开发者需要完成专业认证。

![image-20251114141002355]( https://ota-g.rokidcdn.com/toB/Document/CXR/1.0.4/authorize_1.png)

#### 1.3.1 基础认证

填写姓名和证件号即可。

![image-20251114141044897]( https://ota-g.rokidcdn.com/toB/Document/CXR/1.0.4/authorize_2.png)

实名认证后，会显示审核通过。

![image-20251114141152307]( https://ota-g.rokidcdn.com/toB/Document/CXR/1.0.4/authorize_3.png)

#### 1.3.2 开发者认证

开发者认证需要选择认证类别，并上传相应的证件资料。

![image-20251114142202656]( https://ota-g.rokidcdn.com/toB/Document/CXR/1.0.4/developer_1.png)

![image-20251114142453918]( https://ota-g.rokidcdn.com/toB/Document/CXR/1.0.4/authorize_6.png)

## 2 获取凭证

注册好开发者，并完成实名认证后，就可以前往“凭证信息”获取个人/企业用户的凭证。这里选择普通厂商获取密钥即可。

![image-20251114141329178]( https://ota-g.rokidcdn.com/toB/Document/CXR/1.0.4/authorize_4.png)

凭证信息获取结果：

![image-20251114141521092]( https://ota-g.rokidcdn.com/toB/Document/CXR/1.0.4/authorize_5.png)



##  3 获取SN 鉴权文件

开发者认证审核通过，并获取了凭证信息之后，可以前往https://ar.rokid.com 进入控制台。

![image-20251114141719761]( https://ota-g.rokidcdn.com/toB/Document/CXR/1.0.4/console_1.png)

进入控制台之后，选择设备管理，并点击绑定新设备。

### 3.1 添加设备

**Tips：个人用户只能绑定10台同类型设备，在提交之前请认真确认SN 号。**

![image-20251114151516682]( https://ota-g.rokidcdn.com/toB/Document/CXR/1.0.4/SN_1.png)

![image-20251114151605958]( https://ota-g.rokidcdn.com/toB/Document/CXR/1.0.4/SN_3.png)

### 3.2 下载授权文件

绑定好设备之后，就下载鉴权文件了。鉴权文件扩展名为`.lc`。

![image-20251114151640958]( https://ota-g.rokidcdn.com/toB/Document/CXR/1.0.4/SN_2.png)

# 配置Maven仓库

CXR-M SDK 采用Maven 在线管理SDK Package。

Maven 仓库地址:（"https://maven.rokid.com/repository/maven-public/"）

找到settings.gradle.kts，并在`dependencyResolutionManagement`节点的`repositories` 中添加Maven仓库。

![image-20251114132639963](https://ota-g.rokidcdn.com/toB/Document/CXR/1.0.4/gradle_settings.png)

```kotlin
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
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
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.rokid.com/repository/maven-public/") }

        google()
        mavenCentral()
    }
}
```

# 依赖导入

CXR-M SDK Package（"com.rokid.cxr:client-m:1.0.4"）。

在build.gradle.kts 的`dependencies` 节点中添加依赖。***注意：SDK 需要设置minSdk≥28***。

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
    
    implementation("com.rokid.cxr:client-m:1.0.4")
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

![image-20251114132940630](https://ota-g.rokidcdn.com/toB/Document/CXR/1.0.4/Dependencies_settings.png)

# 权限申请

## 1 声明权限

CXR-M SDK 需要申请网络、Wi-Fi、Bluetooth（蓝牙权限需要同步申请FINE_LOCATION 权限） 等权限，在AndroidManifest.xml 中申请以下是最小权限集，注意`android.permission.NEARBY_WIFI_DEVICES`权限是Android 使用Wi-Fi P2P 的基础权限，在使用Wi-Fi 功能时必须申请：

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.BLUETOOTH" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES" />

    <application>
        <!--Other Settings-->
    </application>

</manifest>
```

# 2 动态权限申请

除了声明权限之外，Android 需要在使用权限时进行动态申请。具体的申请过程将在后续的功能使用过程中展示。

