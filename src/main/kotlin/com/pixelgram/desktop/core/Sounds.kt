package com.pixelgram.desktop.core

import java.io.ByteArrayInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.LineEvent

object Sounds {
    private const val MinInterval = 1_000L

    private val message: ByteArray? by lazy {
        Sounds::class.java.getResourceAsStream("/sounds/message.wav")?.use { it.readBytes() }
    }

    @Volatile
    private var lastPlayed = 0L

    fun newMessage() {
        val now = System.currentTimeMillis()
        if (now - lastPlayed < MinInterval) return
        lastPlayed = now
        play(message ?: return)
    }

    fun warmUp() {
        Thread {
            runCatching {
                message
                AudioSystem.getClip().close()
            }
        }.apply { isDaemon = true }.start()
    }

    private fun play(data: ByteArray) {
        Thread {
            runCatching {
                val clip = AudioSystem.getClip()
                AudioSystem.getAudioInputStream(ByteArrayInputStream(data)).use { clip.open(it) }
                clip.addLineListener { if (it.type == LineEvent.Type.STOP) clip.close() }
                clip.start()
            }.onFailure { AppLog.write("Notification sound failed", it) }
        }.apply { isDaemon = true }.start()
    }
}
