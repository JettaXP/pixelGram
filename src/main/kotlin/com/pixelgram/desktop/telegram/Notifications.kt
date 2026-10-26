package com.pixelgram.desktop.telegram

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import it.tdlight.jni.TdApi

object Notifications {
    var onMessage: ((Telegram, TdApi.Message) -> Unit)? = null
    var showTest: (() -> Unit)? = null

    var openRequest by mutableStateOf<Pair<Int, Long>?>(null)

    var window: java.awt.Window? = null

    fun flash() {
        val target = window ?: return
        if (target.isFocused) return
        runCatching {
            if (java.awt.Taskbar.isTaskbarSupported()) {
                val taskbar = java.awt.Taskbar.getTaskbar()
                if (taskbar.isSupported(java.awt.Taskbar.Feature.USER_ATTENTION_WINDOW)) taskbar.requestWindowUserAttention(target)
                else if (taskbar.isSupported(java.awt.Taskbar.Feature.USER_ATTENTION)) taskbar.requestUserAttention(true, false)
            }
        }
    }
}
