package com.pixelgram.desktop.core

import java.io.ByteArrayInputStream
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine
import javax.sound.sampled.FloatControl
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
                AudioSystem.getAudioInputStream(ByteArrayInputStream(data)).use { source ->
                    val base = source.format
                    val format = AudioFormat(AudioFormat.Encoding.PCM_SIGNED, base.sampleRate, PcmBits, base.channels, base.channels * PcmBits / 8, base.sampleRate, false)
                    val pcm = if (base.matches(format)) source else AudioSystem.getAudioInputStream(format, source)
                    val line = openLine(format)
                    try {
                        if (volume < MaxVolume && line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                            val gain = line.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                            val db = if (volume <= 0) gain.minimum else (20 * log10(volume / MaxVolume.toFloat()))
                            gain.value = db.coerceIn(gain.minimum, gain.maximum)
                        }
                        line.start()
                        val buffer = ByteArray(BufferBytes)
                        while (true) {
                            val read = pcm.read(buffer)
                            if (read <= 0) break
                            line.write(buffer, 0, read)
                        }
                        line.drain()
                    } finally {
                        line.close()
                    }
                }
            }.onFailure { AppLog.write("Notification sound failed", it) }
        }.apply { isDaemon = true }.start()
    }

    private fun openLine(format: AudioFormat): SourceDataLine {
        runCatching { return AudioDevices.sourceLine(format).apply { open(format) } }
        return AudioSystem.getSourceDataLine(format).apply { open(format) }
    }

    private const val PcmBits = 16
    private const val BufferBytes = 8192
}
