package com.pixelgram.desktop.core

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.Mixer
import javax.sound.sampled.SourceDataLine
import javax.sound.sampled.TargetDataLine

object AudioDevices {
    var settings: AppSettings? = null

    fun outputs(): List<String> = names { mixer -> mixer.sourceLineInfo.any { SourceDataLine::class.java.isAssignableFrom(it.lineClass) } }

    fun inputs(): List<String> = names { mixer -> mixer.targetLineInfo.any { TargetDataLine::class.java.isAssignableFrom(it.lineClass) } }

    fun sourceLine(format: AudioFormat): SourceDataLine =
        mixer(settings?.outputDevice?.value)?.let { info -> runCatching { AudioSystem.getSourceDataLine(format, info) }.getOrNull() }
            ?: AudioSystem.getSourceDataLine(format)

    fun targetLine(format: AudioFormat): TargetDataLine =
        mixer(settings?.inputDevice?.value)?.let { info -> runCatching { AudioSystem.getTargetDataLine(format, info) }.getOrNull() }
            ?: AudioSystem.getTargetDataLine(format)

    fun clip(): Clip =
        mixer(settings?.outputDevice?.value)?.let { info -> runCatching { AudioSystem.getClip(info) }.getOrNull() }
            ?: AudioSystem.getClip()

    private fun mixer(name: String?): Mixer.Info? =
        name?.takeIf { it.isNotEmpty() }?.let { wanted -> AudioSystem.getMixerInfo().firstOrNull { it.name == wanted } }

    private fun names(supports: (Mixer) -> Boolean): List<String> = runCatching {
        AudioSystem.getMixerInfo().filter { info -> runCatching { supports(AudioSystem.getMixer(info)) }.getOrDefault(false) }
            .map { it.name }
            .distinct()
    }.getOrDefault(emptyList())
}

class MicMeter private constructor() {
    @Volatile var level = 0f
        private set
    @Volatile private var running = true

    fun stop() {
        running = false
    }

    private fun run() {
        val format = AudioFormat(SampleRate.toFloat(), 16, 1, true, false)
        val line = runCatching { AudioDevices.targetLine(format).apply { open(format) } }.getOrNull() ?: return
        val bytes = ByteArray(SampleRate / ChunksPerSecond * 2)
        try {
            line.start()
            while (running) {
                val read = line.read(bytes, 0, bytes.size)
                if (read <= 0) continue
                var sum = 0.0
                for (i in 0 until read / 2) {
                    val value = ((bytes[i * 2 + 1].toInt() shl 8) or (bytes[i * 2].toInt() and 0xFF)).toShort().toDouble()
                    sum += value * value
                }
                val rms = kotlin.math.sqrt(sum / (read / 2)) / Short.MAX_VALUE
                level = if (rms <= 0) 0f else ((20 * kotlin.math.log10(rms) + SilenceDb) / SilenceDb).toFloat().coerceIn(0f, 1f)
            }
        } finally {
            runCatching { line.stop() }
            runCatching { line.close() }
        }
    }

    companion object {
        private const val SampleRate = 48_000
        private const val ChunksPerSecond = 25
        private const val SilenceDb = 50.0

        fun start(): MicMeter = MicMeter().also { meter -> Thread(meter::run, "mic-meter").apply { isDaemon = true }.start() }
    }
}
