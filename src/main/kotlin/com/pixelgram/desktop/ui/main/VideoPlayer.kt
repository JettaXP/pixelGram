package com.pixelgram.desktop.ui.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pixelgram.desktop.telegram.Telegram
import it.tdlight.jni.TdApi

class VideoRequest(val telegram: Telegram, val file: TdApi.File, val thumb: TdApi.File?, val mini: TdApi.Minithumbnail?, val width: Int, val height: Int, val duration: Int, val caption: String)

object VideoPlayer {
    var current by mutableStateOf<VideoRequest?>(null)

    fun open(telegram: Telegram, video: TdApi.Video, caption: String) {
        telegram.download(video.video)
        current = VideoRequest(telegram, video.video, video.thumbnail?.file, video.minithumbnail, video.width, video.height, video.duration, caption)
    }
}
