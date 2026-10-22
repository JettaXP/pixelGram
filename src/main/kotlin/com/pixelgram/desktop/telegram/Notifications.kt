package com.pixelgram.desktop.telegram

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import it.tdlight.jni.TdApi

object Notifications {
    var onMessage: ((Telegram, TdApi.Message) -> Unit)? = null
    var showTest: (() -> Unit)? = null

    var openRequest by mutableStateOf<Pair<Int, Long>?>(null)
}
