package com.demo.rokid_huishi_m.activities.suixinYiting

import android.content.Context
import kotlin.random.Random

// 随机音乐播放工具类
class MusicRandomPlayer {
    // 随机选择一首音乐
    fun getRandomMusic(musicList: List<MusicItem>): MusicItem? {
        if (musicList.isEmpty()) return null
        val randomIndex = Random.nextInt(musicList.size)
        return musicList[randomIndex]
    }
}
