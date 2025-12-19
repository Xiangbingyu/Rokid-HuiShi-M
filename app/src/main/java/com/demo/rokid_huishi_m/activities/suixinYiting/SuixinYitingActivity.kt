package com.demo.rokid_huishi_m.activities.suixinYiting

import android.media.MediaPlayer
import android.os.Bundle
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.foundation.clickable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
                        // 点击随机播放按钮
                        val randomMusic = musicRandomPlayer.getRandomMusic(musicList)
                        if (randomMusic != null) {
                            // 更新当前播放的音乐
                            currentMusic = randomMusic
                            // 使用同一个播放器实例播放
                            musicPlayerManager.play(context, randomMusic.resourceId) {
                                isPlaying = false
                            }
                            isPlaying = true
                        }
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
