package com.pixelgram.desktop.telegram

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import it.tdlight.jni.TdApi

// Connects incoming messages to the pop-ups, and a pop-up click back to the chat.
object Notifications {
    var onMessage: ((Telegram, TdApi.Message) -> Unit)? = null
    var showTest: (() -> Unit)? = null

    // A chat to open, set by a pop-up click: account number to chat id.
    var openRequest by mutableStateOf<Pair<Int, Long>?>(null)
}
