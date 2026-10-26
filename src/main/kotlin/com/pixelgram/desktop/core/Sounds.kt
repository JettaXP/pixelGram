package com.pixelgram.desktop.core

import java.io.ByteArrayInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.FloatControl
import javax.sound.sampled.LineEvent
import kotlin.math.log10

object Sounds {
    const val MaxVolume = 100
    private const val MinInterval = 1_000L

    private val message: ByteArray? by lazy {
        Sounds::class.java.getResourceAsStream("/sounds/message.wav")?.use { it.readBytes() }
    }

    @Volatile
    private var lastPlayed = 0L

    fun newMessage(volume: Int = MaxVolume) {
        val now = System.currentTimeMillis()
        if (now - lastPlayed < MinInterval) return
        lastPlayed = now
        play(message ?: return, volume)
    }

    fun warmUp() {
        Thread {
            runCatching {
                message
                AudioSystem.getClip().close()
            }
        }.apply { isDaemon = true }.start()
    }

    private fun play(data: ByteArray, volume: Int) {
        Thread {
            runCatching {
                val clip = AudioDevices.clip()
                AudioSystem.getAudioInputStream(ByteArrayInputStream(data)).use { clip.open(it) }
                if (volume < MaxVolume && clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    val gain = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                    val db = if (volume <= 0) gain.minimum else (20 * log10(volume / MaxVolume.toFloat()))
                    gain.value = db.coerceIn(gain.minimum, gain.maximum)
                }
                clip.addLineListener { if (it.type == LineEvent.Type.STOP) clip.close() }
                clip.start()
            }.onFailure { AppLog.write("Notification sound failed", it) }
        }.apply { isDaemon = true }.start()
    }
}
