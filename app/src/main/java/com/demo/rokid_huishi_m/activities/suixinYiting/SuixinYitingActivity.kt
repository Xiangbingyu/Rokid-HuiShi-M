package com.demo.rokid_huishi_m.activities.suixinYiting

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demo.rokid_huishi_m.R

private const val TAG = "SuixinYitingActivity"

data class MusicItem(
    val id: Int,
    val name: String,
    val resourceId: Int,
    val resourceKey: String
)

private data class PreferenceSettings(
    val mbti: String = "infp",
    val mood: String = "低落",
    val preference: String = "安静，流行，民谣"
)

private data class PlayerUiState(
    val currentMusic: MusicItem? = null,
    val currentMusicIndex: Int = -1,
    val isPlaying: Boolean = false,
    val isFromRandomPlay: Boolean = false
)

class MusicPlayerManager {
    private var mediaPlayer: MediaPlayer? = null
    private var currentResourceId: Int? = null

    fun play(context: Context, resourceId: Int, onCompletion: () -> Unit) {
        if (currentResourceId != resourceId || mediaPlayer == null) {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(context, resourceId)
            currentResourceId = resourceId
        }
        mediaPlayer?.setOnCompletionListener { onCompletion() }
        mediaPlayer?.start()
    }

    fun pause() {
        mediaPlayer?.pause()
    }

    fun resume() {
        mediaPlayer?.start()
    }

    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
        currentResourceId = null
    }
}

class SuixinYitingActivity : ComponentActivity() {
    private val musicPlayerManager = MusicPlayerManager()
    private val musicRandomPlayer = MusicRandomPlayer()
    private val musicList by lazy { buildMusicList() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MusicPlayerScreen(
                musicList = musicList,
                musicPlayerManager = musicPlayerManager,
                musicRandomPlayer = musicRandomPlayer
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        musicPlayerManager.release()
    }

    private fun buildMusicList(): List<MusicItem> {
        return R.raw::class.java.fields
            .filter { it.type == Int::class.java && it.name != "rokid" }
            .mapNotNull { field ->
                runCatching {
                    val resourceName = field.name
                    MusicItem(
                        id = field.getInt(null),
                        name = RESOURCE_NAME_TO_DISPLAY_NAME[resourceName]
                            ?: resourceName.removeSuffix("_l").replace("_", " "),
                        resourceId = field.getInt(null),
                        resourceKey = resourceName
                    )
                }.onFailure { throwable ->
                    Log.e(TAG, "构建音乐列表失败 - ${throwable.message}", throwable)
                }.getOrNull()
            }
    }

    companion object {
        private val RESOURCE_NAME_TO_DISPLAY_NAME = mapOf(
            "aslove_my_girl_l" to "Aslove - My Girl",
            "athletics_i_l" to "Athletics - I",
            "athletics_ii_l" to "Athletics - II",
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
            "song_dongye_anheqiao_l" to "宋冬野 - 安和桥",
            "song_dongye_banma_l" to "宋冬野 - 斑马，斑马",
            "man_shuke_musik_i_liao_weishan_my_heart_will_go_on_l" to "满舒克  MuSik I  廖伟珊 - My Heart Will Go On",
            "zhao_lei_wojide_l" to "赵雷 - 我记得",
            "ma_di_song_dongye_yao_shisan_nanshannan_l" to "马頔  宋冬野  尧十三 - 南山南"
        )
    }
}

@Composable
private fun MusicPlayerScreen(
    musicList: List<MusicItem>,
    musicPlayerManager: MusicPlayerManager,
    musicRandomPlayer: MusicRandomPlayer
) {
    val context = LocalContext.current
    val palette = suixinPalette()
    var playerState by remember { mutableStateOf(PlayerUiState()) }
    var preferences by remember { mutableStateOf(PreferenceSettings()) }
    var editingPreferences by remember { mutableStateOf(preferences) }
    var showEditDialog by remember { mutableStateOf(false) }

    fun playMusic(musicItem: MusicItem, fromRandomPlay: Boolean) {
        val targetIndex = musicList.indexOfFirst { it.resourceId == musicItem.resourceId }
        playerState = playerState.copy(
            currentMusic = musicItem,
            currentMusicIndex = targetIndex,
            isFromRandomPlay = fromRandomPlay,
            isPlaying = true
        )
        musicPlayerManager.play(context, musicItem.resourceId) {
            playerState = playerState.copy(isPlaying = false)
            if (!playerState.isFromRandomPlay && musicList.isNotEmpty()) {
                val nextIndex = if (playerState.currentMusicIndex < 0) {
                    0
                } else {
                    (playerState.currentMusicIndex + 1) % musicList.size
                }
                playMusic(musicList[nextIndex], fromRandomPlay = false)
            }
        }
    }

    fun playRandomMusic() {
        val analyzePreference = MusicRandomPlayer.AnalyzePreference(
            mbti = preferences.mbti,
            mood = preferences.mood,
            preference = preferences.preference
        )
        musicRandomPlayer.analyzeImageForMusic(analyzePreference) { musicUrl ->
            val resourceName = musicUrl?.substringBeforeLast(".") ?: return@analyzeImageForMusic
            val musicToPlay = musicList.firstOrNull { it.resourceKey == resourceName }
            if (musicToPlay != null) {
                playMusic(musicToPlay, fromRandomPlay = true)
            } else {
                Log.d(TAG, "未找到匹配的音乐资源: $resourceName")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        HeaderBar(
            title = "随心一听",
            palette = palette
        )
        PreferenceCard(
            preferences = preferences,
            palette = palette,
            onEditClick = {
                editingPreferences = preferences
                showEditDialog = true
            }
        )

        if (showEditDialog) {
            PreferenceEditDialog(
                editingPreferences = editingPreferences,
                palette = palette,
                onPreferencesChange = { editingPreferences = it },
                onSave = {
                    preferences = editingPreferences
                    showEditDialog = false
                },
                onDismiss = { showEditDialog = false }
            )
        }

        Button(
            onClick = ::playRandomMusic,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(56.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = palette.primary,
                contentColor = Color.White
            )
        ) {
            Text(
                text = "🎲 随心播放",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(musicList) { musicItem ->
                MusicItemView(
                    musicItem = musicItem,
                    palette = palette,
                    onClick = { playMusic(musicItem, fromRandomPlay = false) }
                )
            }
        }

        playerState.currentMusic?.let {
            MusicPlayerBar(
                musicItem = it,
                isPlaying = playerState.isPlaying,
                palette = palette,
                onPlayPauseClick = {
                    if (musicPlayerManager.isPlaying()) {
                        musicPlayerManager.pause()
                        playerState = playerState.copy(isPlaying = false)
                    } else {
                        musicPlayerManager.resume()
                        playerState = playerState.copy(isPlaying = true)
                    }
                }
            )
        }
    }
}

@Composable
private fun PreferenceCard(
    preferences: PreferenceSettings,
    palette: SuixinPalette,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .height(132.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, palette.border),
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "个人偏好设置",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textMain,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "MBTI: ${preferences.mbti}",
                            fontSize = 13.sp,
                            color = palette.textMain
                        )
                        Text(
                            text = "心情: ${preferences.mood}",
                            fontSize = 13.sp,
                            color = palette.textMain
                        )
                        Text(
                            text = "偏好: ${preferences.preference}",
                            fontSize = 13.sp,
                            color = palette.textMain
                        )
                    }
                    IconButton(onClick = onEditClick) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "编辑偏好设置",
                            tint = palette.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreferenceEditDialog(
    editingPreferences: PreferenceSettings,
    palette: SuixinPalette,
    onPreferencesChange: (PreferenceSettings) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "编辑个人偏好") },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "MBTI:", fontSize = 14.sp, modifier = Modifier.width(60.dp))
                    TextField(
                        value = editingPreferences.mbti,
                        onValueChange = {
                            onPreferencesChange(editingPreferences.copy(mbti = it))
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(text = "请输入MBTI类型") }
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "心情:", fontSize = 14.sp, modifier = Modifier.width(60.dp))
                    TextField(
                        value = editingPreferences.mood,
                        onValueChange = {
                            onPreferencesChange(editingPreferences.copy(mood = it))
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(text = "请输入当前心情") }
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "偏好:", fontSize = 14.sp, modifier = Modifier.width(60.dp))
                    TextField(
                        value = editingPreferences.preference,
                        onValueChange = {
                            onPreferencesChange(editingPreferences.copy(preference = it))
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(text = "请输入音乐偏好") }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text(text = "保存", color = palette.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "取消", color = palette.textMuted)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MusicItemView(
    musicItem: MusicItem,
    palette: SuixinPalette,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, palette.border),
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color(0xFFEDE9FE), CircleShape)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_background),
                    contentDescription = "音乐封面",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Text(
                text = musicItem.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = palette.textMain,
                modifier = Modifier.weight(1f)
                    .padding(start = 16.dp)
            )

            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "播放",
                tint = palette.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun MusicPlayerBar(
    musicItem: MusicItem,
    isPlaying: Boolean,
    palette: SuixinPalette,
    onPlayPauseClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, palette.border),
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color(0xFFEDE9FE), CircleShape)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_background),
                        contentDescription = "音乐封面",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Text(
                    text = musicItem.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textMain,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            IconButton(onClick = onPlayPauseClick) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    tint = palette.primary,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}

private data class SuixinPalette(
    val primary: Color,
    val background: Color,
    val surface: Color,
    val textMain: Color,
    val textMuted: Color,
    val border: Color
)

private fun suixinPalette() = SuixinPalette(
    primary = Color(0xFF6A5ACD),
    background = Color(0xFFF9FAFB),
    surface = Color(0xFFFFFFFF),
    textMain = Color(0xFF111827),
    textMuted = Color(0xFF6B7280),
    border = Color(0xFFE5E7EB)
)

@Composable
private fun HeaderBar(
    title: String,
    palette: SuixinPalette
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
