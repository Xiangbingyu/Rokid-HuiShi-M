package com.demo.rokid_huishi_m.activities.suixinYiting

import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demo.rokid_huishi_m.R

// 音乐数据模型
class MusicItem(val id: Int, val name: String, val resourceId: Int)

// 音乐播放器管理类
class MusicPlayerManager {
    private var mediaPlayer: MediaPlayer? = null
    private var currentResourceId: Int? = null
    private var isPlaying = false

    fun play(context: android.content.Context, resourceId: Int, onCompletion: () -> Unit) {
        if (currentResourceId != resourceId || mediaPlayer == null) {
            // 释放旧的播放器
            mediaPlayer?.release()
            // 创建新的播放器
            mediaPlayer = MediaPlayer.create(context, resourceId)
            mediaPlayer?.setOnCompletionListener {
                isPlaying = false
                onCompletion()
            }
            currentResourceId = resourceId
        }
        mediaPlayer?.start()
        isPlaying = true
    }

    fun pause() {
        mediaPlayer?.pause()
        isPlaying = false
    }

    fun resume() {
        mediaPlayer?.start()
        isPlaying = true
    }

    fun isPlaying(): Boolean {
        return isPlaying
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
        currentResourceId = null
        isPlaying = false
    }
}

class SuixinYitingActivity : ComponentActivity() {
    private lateinit var musicPlayerManager: MusicPlayerManager
    private lateinit var musicRandomPlayer: MusicRandomPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        musicPlayerManager = MusicPlayerManager()
        musicRandomPlayer = MusicRandomPlayer()

        // 动态获取raw文件夹中的所有音频资源
        val musicList = buildMusicList()

        setContent {
            MusicPlayerScreen(this, musicList, musicPlayerManager, musicRandomPlayer)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        musicPlayerManager.release()
    }

    // 动态构建音乐列表
    private fun buildMusicList(): List<MusicItem> {
        val musicItems = mutableListOf<MusicItem>()
        
        // 创建资源名到显示名称的映射表
        val resourceNameToDisplayName = mapOf(
            // 英文歌曲
            "aslove_my_girl_l" to "Aslove - My Girl",
            "daya_hide_away_l" to "Daya - Hide Away",
            "lil_future_jingjicang_l" to "Lil Future - 经济舱",
            "shawn_mendes_treat_you_better_l" to "Shawn Mendes - Treat You Better",
            "taylor_swift_all_you_had_to_do_was_stay_l" to "Taylor Swift - All You Had To Do Was Stay",
            "taylor_swift_blank_space_l" to "Taylor Swift - Blank Space",
            "taylor_swift_how_you_get_the_girl_l" to "Taylor Swift - How You Get The Girl",
            "taylor_swift_love_story_live_l" to "Taylor Swift - Love Story (Live)",
            "taylor_swift_shake_it_off_l" to "Taylor Swift - Shake It Off",
            "taylor_swift_style_l" to "Taylor Swift - Style",
            "taylor_swift_welcome_to_new_york_l" to "Taylor Swift - Welcome To New York",
            
            // 中文歌曲
            "song_dongye_anheqiao_l" to "宋冬野 - 安和桥",
            "song_dongye_banma_l" to "宋冬野 - 斑马，斑马",
            "man_shuke_musik_i_liao_weishan_my_heart_will_go_on_l" to "满舒克  MuSik I  廖伟珊 - My Heart Will Go On",
            "zhao_lei_wojide_l" to "赵雷 - 我记得",
            "ma_di_song_dongye_yao_shisan_nanshannan_l" to "马頔  宋冬野  尧十三 - 南山南"
        )
        
        // 使用反射获取R.raw类中的所有资源ID
        val rawClass = R.raw::class.java
        val fields = rawClass.fields
        
        var id = 1
        for (field in fields) {
            if (field.type == Int::class.java) {
                try {
                    // 获取资源名称
                    val resourceName = field.name
                    // 过滤掉非音频文件，特别是rokid.lc
                    if (resourceName == "rokid") {
                        continue
                    }
                    // 获取资源ID
                    val resourceId = field.getInt(null)
                    // 获取显示名称，如果没有映射则使用默认格式化
                    val displayName = resourceNameToDisplayName[resourceName] ?: 
                        resourceName.replace("_l", "").replace("_", " ")
                    // 创建音乐项
                    musicItems.add(MusicItem(id++, displayName, resourceId))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        
        return musicItems
    }
}

@Composable
fun MusicPlayerScreen(
    context: android.content.Context, 
    musicList: List<MusicItem>, 
    musicPlayerManager: MusicPlayerManager,
    musicRandomPlayer: MusicRandomPlayer
) {
    var currentMusic by remember { mutableStateOf<MusicItem?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    
    // 用户偏好设置状态
    var mbti by remember { mutableStateOf("infp") }
    var mood by remember { mutableStateOf("低落") }
    var preference by remember { mutableStateOf("安静，流行，民谣") }
    
    // 编辑模式状态
    var isEditMode by remember { mutableStateOf(false) }
    var editMbti by remember { mutableStateOf(mbti) }
    var editMood by remember { mutableStateOf(mood) }
    var editPreference by remember { mutableStateOf(preference) }

    Column(modifier = Modifier.fillMaxSize()) {
        // 标题
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(Color(0xFF2196F3))
                .padding(16.dp)
        ) {
            Text(
                text = "随心一听",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // 用户偏好设置展示栏
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(120.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFE3F2FD)
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // 展示模式
                Column(modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)) {
                    Text(
                        text = "个人偏好设置",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(text = "MBTI: $mbti", fontSize = 14.sp)
                            Text(text = "心情: $mood", fontSize = 14.sp)
                            Text(text = "偏好: $preference", fontSize = 14.sp)
                        }
                        IconButton(onClick = {
                            // 进入编辑模式
                            editMbti = mbti
                            editMood = mood
                            editPreference = preference
                            isEditMode = true
                        }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "编辑偏好设置",
                                tint = Color(0xFF2196F3)
                            )
                        }
                    }
                }
            }
        }
        
        // 编辑对话框
        if (isEditMode) {
            AlertDialog(
                onDismissRequest = {
                    // 点击对话框外部关闭
                    isEditMode = false
                },
                title = { Text(text = "编辑个人偏好") },
                text = {
                    Column {
                        // MBTI编辑
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "MBTI:", fontSize = 14.sp, modifier = Modifier.width(60.dp))
                            TextField(
                                value = editMbti,
                                onValueChange = { editMbti = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text(text = "请输入MBTI类型") }
                            )
                        }
                        
                        // 心情编辑
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "心情:", fontSize = 14.sp, modifier = Modifier.width(60.dp))
                            TextField(
                                value = editMood,
                                onValueChange = { editMood = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text(text = "请输入当前心情") }
                            )
                        }
                        
                        // 偏好编辑
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "偏好:", fontSize = 14.sp, modifier = Modifier.width(60.dp))
                            TextField(
                                value = editPreference,
                                onValueChange = { editPreference = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text(text = "请输入音乐偏好") }
                            )
                        }
                    }
                },
                confirmButton = {
                    Text(
                        text = "保存",
                        color = Color(0xFF4CAF50),
                        modifier = Modifier
                            .padding(8.dp)
                            .clickable {
                                // 保存修改
                                mbti = editMbti
                                mood = editMood
                                preference = editPreference
                                isEditMode = false
                            }
                    )
                },
                dismissButton = {
                    Text(
                        text = "取消",
                        color = Color(0xFFF44336),
                        modifier = Modifier
                            .padding(8.dp)
                            .clickable {
                                isEditMode = false
                            }
                    )
                }
            )
        }

        // 随心播放按钮
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp)
                                    .clickable {
                                        // 点击随心播放按钮
                                        musicRandomPlayer.analyzeImageForMusic(mbti, mood, preference, object : MusicRandomPlayer.OnAnalyzeCompleteListener {
                                            override fun onAnalyzeComplete(musicUrl: String?) {
                                                if (musicUrl != null) {
                                                    // 从musicUrl中提取资源名称（去掉.ogg后缀）
                                                    val resourceName = musicUrl.substringBeforeLast(".")
                                                    Log.d("SuixinYitingActivity", "解析到的资源名称: $resourceName")
                                                    
                                                    // 根据资源名称查找对应的音乐
                                                    // 使用反射获取R.raw类中的所有资源ID，查找匹配的资源
                                                    val rawClass = R.raw::class.java
                                                    var resourceId: Int? = null
                                                    
                                                    try {
                                                        // 尝试直接通过资源名称获取资源ID
                                                        val field = rawClass.getField(resourceName)
                                                        resourceId = field.getInt(null)
                                                        Log.d("SuixinYitingActivity", "找到匹配的资源ID: $resourceId")
                                                    } catch (e: Exception) {
                                                        // 如果直接匹配失败，遍历所有资源查找
                                                        for (field in rawClass.fields) {
                                                            if (field.type == Int::class.java && field.name == resourceName) {
                                                                resourceId = field.getInt(null)
                                                                Log.d("SuixinYitingActivity", "遍历找到匹配的资源ID: $resourceId")
                                                                break
                                                            }
                                                        }
                                                    }
                                                    
                                                    if (resourceId != null) {
                                                        // 在音乐列表中查找对应的MusicItem
                                                        val musicToPlay = musicList.find { it.resourceId == resourceId }
                                                        if (musicToPlay != null) {
                                                            // 更新当前播放的音乐
                                                            currentMusic = musicToPlay
                                                            // 使用同一个播放器实例播放
                                                            musicPlayerManager.play(context, musicToPlay.resourceId) {
                                                                isPlaying = false
                                                            }
                                                            isPlaying = true
                                                        } else {
                                                            Log.d("SuixinYitingActivity", "资源ID找到但未在音乐列表中找到对应项")
                                                        }
                                                    } else {
                                                        Log.d("SuixinYitingActivity", "未找到匹配的资源ID")
                                                    }
                                                } else {
                                                    // 分析失败或没有找到匹配的音乐，可以根据需要添加提示
                                                }
                                            }
                                        })
                                    },
                                elevation = CardDefaults.cardElevation(4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF4CAF50)
                                )
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Text(
                                        text = "🎲 随心播放",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            }
                        }

        // 音乐列表
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(musicList) {
                MusicItemView(
                    musicItem = it,
                    onClick = {
                        currentMusic = it
                        musicPlayerManager.play(context, it.resourceId) {
                            isPlaying = false
                        }
                        isPlaying = true
                    }
                )
            }
        }

        // 当前播放栏
        currentMusic?.let {
            MusicPlayerBar(
                musicItem = it,
                isPlaying = isPlaying,
                onPlayPauseClick = {
                    if (musicPlayerManager.isPlaying()) {
                        musicPlayerManager.pause()
                        isPlaying = false
                    } else {
                        musicPlayerManager.resume()
                        isPlaying = true
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicItemView(musicItem: MusicItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .height(80.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 音乐封面
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color(0xFFE3F2FD))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_background),
                    contentDescription = "音乐封面",
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 音乐名称
            Text(
                text = musicItem.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
                    .padding(start = 16.dp)
            )

            // 播放图标
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "播放",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun MusicPlayerBar(musicItem: MusicItem, isPlaying: Boolean, onPlayPauseClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 音乐封面和名称
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color(0xFFE3F2FD))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_background),
                        contentDescription = "音乐封面",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Text(
                    text = musicItem.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            // 播放/暂停按钮
            IconButton(onClick = onPlayPauseClick) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}
