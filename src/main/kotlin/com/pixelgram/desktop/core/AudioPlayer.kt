package com.pixelgram.desktop.core

import it.tdlight.jni.TdApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.jaredmdobson.concentus.OpusDecoder
import javazoom.jl.decoder.Bitstream
import javazoom.jl.decoder.Decoder
import javazoom.jl.decoder.SampleBuffer
import java.awt.EventQueue
import java.nio.ShortBuffer
import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.FrameGrabber
import java.io.ByteArrayOutputStream
import java.io.File
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem

object AudioPlayer {
    var current by mutableStateOf<Long?>(null)
        private set
    var playing by mutableStateOf(false)
        private set
    var loading by mutableStateOf(false)
        private set
    var position by mutableFloatStateOf(0f)
        private set
    var duration by mutableFloatStateOf(0f)
        private set
    var title by mutableStateOf("")
        private set
    var chatId by mutableStateOf(0L)
        private set
    var audio by mutableStateOf<TdApi.Audio?>(null)
        private set
    var repeat by mutableStateOf(false)
    var muted by mutableStateOf(false)
    var speed by mutableFloatStateOf(1f)
    var onFinished: ((Long) -> Unit)? = null
    private var path: String? = null

    @Volatile private var paused = false
    @Volatile private var seekTo: Float? = null
    @Volatile private var generation = 0

    fun toggle(key: Long, path: String, title: String, chatId: Long, audio: TdApi.Audio? = null) {
        if (key == current && !loading) {
            paused = playing
            playing = !playing
            return
        }
        play(key, path, title, chatId, audio)
    }

    fun play(key: Long, path: String, title: String, chatId: Long, audio: TdApi.Audio? = null) {
        this.title = title
        this.chatId = chatId
        this.audio = audio
        start(key, path, 0f)
    }

    fun pauseOrResume() {
        if (current == null || loading) return
        paused = playing
        playing = !playing
    }

    fun seekCurrent(fraction: Float) {
        val key = current ?: return
        seek(key, path ?: return, fraction)
    }

    fun stop() {
        generation++
        current = null
        playing = false
        loading = false
        position = 0f
    }

    fun nextSpeed() {
        speed = when (speed) {
            1f -> 1.5f
            1.5f -> 2f
            else -> 1f
        }
    }

    fun seek(key: Long, path: String, fraction: Float) {
        if (key == current) {
            seekTo = fraction.coerceIn(0f, 1f) * duration
            if (!playing) {
                paused = false
                playing = true
            }
        } else {
            start(key, path, fraction)
        }
    }

    fun progress(key: Long): Float = if (key == current && duration > 0f) (position / duration).coerceIn(0f, 1f) else 0f

    private fun start(key: Long, path: String, fraction: Float) {
        this.path = path
        val run = ++generation
        current = key
        loading = true
        playing = true
        paused = false
        position = 0f
        seekTo = null
        Thread {
            runCatching { play(run, path, fraction) }.onFailure {
                AppLog.write("Audio playback failed", it)
                ui(run) {
                    playing = false
                    loading = false
                }
            }
        }.apply { isDaemon = true }.start()
    }

    private fun ui(run: Int, block: () -> Unit) = EventQueue.invokeLater { if (run == generation) block() }

    private fun play(run: Int, path: String, fraction: Float) {
        val pcm = decode(File(path))
        val frames = pcm.samples.size / pcm.channels
        val total = frames.toFloat() / pcm.rate
        ui(run) {
            duration = total
            loading = false
        }
        val format = AudioFormat(pcm.rate.toFloat(), 16, pcm.channels, true, false)
        val frameBytes = 2 * pcm.channels
        val line = runCatching {
            AudioDevices.sourceLine(format).apply {
                open(format, pcm.rate / 5 * frameBytes)
                start()
            }
        }.onFailure { AppLog.write("No audio output, playing silently", it) }.getOrNull()
        val sink = line?.let { Sink.Device(it) } ?: Sink.Silent(pcm.rate * frameBytes)
        var index = (fraction * frames).toInt()
        val chunk = pcm.rate / 25
        val bytes = ByteArray(chunk * frameBytes)
        try {
            while (run == generation) {
                seekTo?.let {
                    index = (it * pcm.rate).toInt().coerceIn(0, frames)
                    seekTo = null
                    sink.flush()
                }
                if (paused) {
                    Thread.sleep(25)
                    continue
                }
                if (index >= frames) {
                    if (repeat) {
                        index = 0
                        continue
                    }
                    sink.drain()
                    val finished = current
                    ui(run) {
                        playing = false
                        position = 0f
                        current = null
                        finished?.let { onFinished?.invoke(it) }
                    }
                    break
                }
                val step = speed
                val silent = muted
                var o = 0
                var source = index.toFloat()
                var written = 0
                while (written < chunk && source < frames) {
                    val frame = source.toInt()
                    for (c in 0 until pcm.channels) {
                        val sample = if (silent) 0 else pcm.samples[frame * pcm.channels + c].toInt()
                        bytes[o++] = sample.toByte()
                        bytes[o++] = (sample shr 8).toByte()
                    }
                    source += step
                    written++
                }
                sink.write(bytes, written * frameBytes)
                index = source.toInt()
                val queued = sink.queued() / frameBytes
                val now = (index - queued * step).coerceAtLeast(0f) / pcm.rate
                ui(run) { position = now }
            }
        } finally {
            sink.close()
        }
    }

    private sealed interface Sink {
        fun write(bytes: ByteArray, length: Int)
        fun queued(): Int
        fun flush()
        fun drain()
        fun close()

        class Device(private val line: javax.sound.sampled.SourceDataLine) : Sink {
            override fun write(bytes: ByteArray, length: Int) {
                line.write(bytes, 0, length)
            }
            override fun queued() = line.bufferSize - line.available()
            override fun flush() = line.flush()
            override fun drain() = line.drain()
            override fun close() {
                line.stop()
                line.close()
            }
        }

        class Silent(private val bytesPerSecond: Int) : Sink {
            override fun write(bytes: ByteArray, length: Int) = Thread.sleep(length * 1000L / bytesPerSecond)
            override fun queued() = 0
            override fun flush() = Unit
            override fun drain() = Unit
            override fun close() = Unit
        }
    }

    private class Pcm(val samples: ShortArray, val rate: Int, val channels: Int)

    private class Samples {
        var data = ShortArray(1 shl 16)
        var size = 0
        fun add(source: ShortArray, count: Int) {
            if (size + count > data.size) data = data.copyOf(maxOf(data.size * 2, size + count))
            System.arraycopy(source, 0, data, size, count)
            size += count
        }
    }

    private fun decode(file: File): Pcm {
        val bytes = file.readBytes()
        return when {
            bytes.size > 4 && String(bytes, 0, 4, Charsets.US_ASCII) == "OggS" -> runCatching { decodeOpus(bytes) }.getOrElse { decodeFfmpeg(file) }
            isMp3(bytes) -> decodeMp3(file).takeIf { it.samples.isNotEmpty() } ?: decodeFfmpeg(file)
            else -> decodeFfmpeg(file)
        }
    }

    private fun isMp3(bytes: ByteArray): Boolean = bytes.size > 3 && (
        String(bytes, 0, 3, Charsets.US_ASCII) == "ID3" ||
            ((bytes[0].toInt() and 0xFF) == 0xFF && (bytes[1].toInt() and 0xE0) == 0xE0)
        )

    private fun decodeFfmpeg(file: File): Pcm {
        val grabber = FFmpegFrameGrabber(file).apply {
            sampleMode = FrameGrabber.SampleMode.SHORT
            sampleFormat = avutil.AV_SAMPLE_FMT_S16
        }
        val samples = Samples()
        try {
            grabber.start()
            val channels = grabber.audioChannels.coerceAtLeast(1)
            while (true) {
                val frame = grabber.grabSamples() ?: break
                val planes = frame.samples ?: continue
                val first = planes.firstOrNull() as? ShortBuffer ?: continue
                if (planes.size == 1) {
                    val chunk = ShortArray(first.remaining())
                    first.duplicate().get(chunk)
                    samples.add(chunk, chunk.size)
                } else {
                    val count = first.remaining()
                    val chunk = ShortArray(count * planes.size)
                    planes.forEachIndexed { c, plane ->
                        val buffer = (plane as ShortBuffer).duplicate()
                        for (i in 0 until count) chunk[i * planes.size + c] = buffer.get()
                    }
                    samples.add(chunk, chunk.size)
                }
            }
            return Pcm(samples.data.copyOf(samples.size), grabber.sampleRate, channels)
        } finally {
            runCatching { grabber.stop() }
            runCatching { grabber.release() }
        }
    }

    private fun decodeOpus(bytes: ByteArray): Pcm {
        val packets = ArrayList<ByteArray>()
        val packet = ByteArrayOutputStream()
        var page = 0
        while (page + 27 <= bytes.size && bytes[page] == 'O'.code.toByte() && bytes[page + 1] == 'g'.code.toByte()) {
            val segments = bytes[page + 26].toInt() and 0xFF
            var data = page + 27 + segments
            for (i in 0 until segments) {
                val length = bytes[page + 27 + i].toInt() and 0xFF
                packet.write(bytes, data, length)
                data += length
                if (length < 255) {
                    packets += packet.toByteArray()
                    packet.reset()
                }
            }
            page = data
        }
        val head = packets.first()
        val channels = (head[9].toInt() and 0xFF).coerceIn(1, 2)
        val preSkip = (head[10].toInt() and 0xFF) or ((head[11].toInt() and 0xFF) shl 8)
        val decoder = OpusDecoder(OpusRate, channels)
        val out = ShortArray(MaxOpusFrame * channels)
        val samples = Samples()
        for (data in packets.drop(2)) {
            if (data.isEmpty()) continue
            val frames = decoder.decode(data, 0, data.size, out, 0, MaxOpusFrame, false)
            samples.add(out, frames * channels)
        }
        val skip = (preSkip * channels).coerceAtMost(samples.size)
        return Pcm(samples.data.copyOfRange(skip, samples.size), OpusRate, channels)
    }

    private fun decodeMp3(file: File): Pcm {
        val samples = Samples()
        var rate = 44_100
        var channels = 2
        file.inputStream().buffered().use { input ->
            val stream = Bitstream(input)
            val decoder = Decoder()
            while (true) {
                val header = stream.readFrame() ?: break
                val buffer = decoder.decodeFrame(header, stream) as SampleBuffer
                rate = decoder.outputFrequency
                channels = decoder.outputChannels
                samples.add(buffer.buffer, buffer.bufferLength)
                stream.closeFrame()
            }
        }
        return Pcm(samples.data.copyOf(samples.size), rate, channels)
    }

    private const val OpusRate = 48_000
    private const val MaxOpusFrame = 5_760
}
