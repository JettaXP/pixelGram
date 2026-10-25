package com.pixelgram.desktop.core

import io.github.jaredmdobson.concentus.OpusApplication
import io.github.jaredmdobson.concentus.OpusEncoder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.TargetDataLine
import kotlin.math.log10
import kotlin.math.sqrt
import kotlin.random.Random

class VoiceNote(val file: File, val duration: Int, val waveform: ByteArray)

class VoiceRecorder private constructor(private val file: File, private val onFail: (String) -> Unit) {
    @Volatile var level = 0f
        private set
    @Volatile var elapsedMs = 0L
        private set
    @Volatile private var running = true
    @Volatile private var discard = false
    private val peaks = ArrayList<Float>()
    private var result: VoiceNote? = null
    private val worker = Thread(::record, "voice-recorder").apply { isDaemon = true }

    fun stop(): VoiceNote? {
        running = false
        worker.join(StopWaitMs)
        return result
    }

    fun cancel() {
        discard = true
        running = false
    }

    private fun record() {
        if (System.getProperty("pixelgram.demo") == "true" && openLine() == null) return recordDemo()
        val line = openLine() ?: run {
            onFail("No microphone found")
            return
        }
        try {
            line.start()
            encode(line.format.sampleRate.toInt()) { buffer, offset, length -> line.read(buffer, offset, length) }
        } catch (error: Exception) {
            AppLog.write("Voice recording failed", error)
            onFail("Recording failed")
            discard = true
        } finally {
            runCatching { line.stop() }
            runCatching { line.close() }
        }
        if (discard || elapsedMs < MinDurationMs) {
            file.delete()
        } else {
            result = VoiceNote(file, ((elapsedMs + 500) / 1000).toInt().coerceAtLeast(1), waveform())
        }
    }

    private fun encode(rate: Int, read: (ByteArray, Int, Int) -> Int) {
        val frame = rate / FramesPerSecond
        val granuleStep = (OpusRate / FramesPerSecond).toLong()
        val bytes = ByteArray(frame * 2)
        val samples = ShortArray(frame)
        val packet = ByteArray(MaxPacket)
        var frames = 0L
        val encoder = OpusEncoder(rate, 1, OpusApplication.OPUS_APPLICATION_VOIP).apply { bitrate = Bitrate }
        val preSkip = encoder.lookahead * (OpusRate / rate)
        file.outputStream().buffered().use { out ->
            val ogg = OggWriter(out)
            ogg.page(listOf(opusHead(preSkip)), 0, OggWriter.FirstPage)
            ogg.page(listOf(opusTags()), 0, 0)
            val pending = ArrayList<ByteArray>()
            while (running) {
                var read = 0
                while (read < bytes.size && running) {
                    val got = read(bytes, read, bytes.size - read)
                    if (got < 0) break
                    read += got
                }
                if (read < bytes.size) break
                var sum = 0.0
                for (i in 0 until frame) {
                    val value = ((bytes[i * 2 + 1].toInt() shl 8) or (bytes[i * 2].toInt() and 0xFF)).toShort()
                    samples[i] = value
                    sum += value.toDouble() * value
                }
                val rms = sqrt(sum / frame) / Short.MAX_VALUE
                val loudness = if (rms <= 0) 0f else ((20 * log10(rms) + SilenceDb) / SilenceDb).toFloat().coerceIn(0f, 1f)
                level = loudness
                peaks += loudness
                val size = encoder.encode(samples, 0, frame, packet, 0, packet.size)
                pending += packet.copyOf(size)
                frames++
                elapsedMs = frames * 1000 / FramesPerSecond
                if (pending.size >= PacketsPerPage) {
                    ogg.page(pending, preSkip + frames * granuleStep, 0)
                    pending.clear()
                }
            }
            ogg.page(pending, preSkip + frames * granuleStep, OggWriter.LastPage)
        }
    }

    private fun recordDemo() {
        var t = 0.0
        encode(DemoRate) { buffer, offset, length ->
            Thread.sleep(1000L / FramesPerSecond)
            for (i in 0 until length / 2) {
                val loud = 0.5 + 0.5 * kotlin.math.sin(t / DemoRate * 3)
                val value = (kotlin.math.sin(t * 2 * Math.PI * 220 / DemoRate) * loud * Short.MAX_VALUE * 0.6).toInt()
                buffer[offset + i * 2] = value.toByte()
                buffer[offset + i * 2 + 1] = (value shr 8).toByte()
                t++
            }
            length
        }
        result = VoiceNote(file, ((elapsedMs + 500) / 1000).toInt().coerceAtLeast(1), waveform()).takeIf { !discard && elapsedMs >= MinDurationMs }
    }

    private fun openLine(): TargetDataLine? {
        for (rate in Rates) {
            val format = AudioFormat(rate.toFloat(), 16, 1, true, false)
            val line = runCatching {
                AudioDevices.targetLine(format).apply { open(format, rate / FramesPerSecond * 2 * BufferFrames) }
            }.getOrNull()
            if (line != null) return line
        }
        return null
    }

    private fun waveform(): ByteArray {
        val values = IntArray(WaveformBars)
        if (peaks.isNotEmpty()) {
            val buckets = FloatArray(WaveformBars) { index ->
                val from = index * peaks.size / WaveformBars
                val to = maxOf(from + 1, (index + 1) * peaks.size / WaveformBars)
                (from until minOf(to, peaks.size)).maxOfOrNull { peaks[it] } ?: 0f
            }
            val top = buckets.max().coerceAtLeast(0.01f)
            buckets.forEachIndexed { index, value -> values[index] = (value / top * 31).toInt().coerceIn(0, 31) }
        }
        val packed = ByteArray((WaveformBars * 5 + 7) / 8)
        values.forEachIndexed { index, value ->
            val bit = index * 5
            for (b in 0 until 5) {
                if (value shr b and 1 == 1) {
                    val position = bit + b
                    packed[position / 8] = (packed[position / 8].toInt() or (1 shl (position % 8))).toByte()
                }
            }
        }
        return packed
    }

    private fun opusHead(preSkip: Int): ByteArray = ByteArrayOutputStream().apply {
        write("OpusHead".toByteArray())
        write(1)
        write(1)
        writeLe(preSkip.toLong(), 2)
        writeLe(OpusRate.toLong(), 4)
        writeLe(0, 2)
        write(0)
    }.toByteArray()

    private fun opusTags(): ByteArray = ByteArrayOutputStream().apply {
        val vendor = "pixelGram".toByteArray()
        write("OpusTags".toByteArray())
        writeLe(vendor.size.toLong(), 4)
        write(vendor)
        writeLe(0, 4)
    }.toByteArray()

    companion object {
        private const val OpusRate = 48_000
        private const val DemoRate = 48_000
        private val Rates = listOf(48_000, 16_000)
        private const val FramesPerSecond = 50
        private const val BufferFrames = 4
        private const val Bitrate = 32_000
        private const val MaxPacket = 4_000
        private const val PacketsPerPage = 50
        private const val SilenceDb = 50.0
        const val MinDurationMs = 700L
        private const val StopWaitMs = 3_000L
        private const val WaveformBars = 100

        fun start(onFail: (String) -> Unit): VoiceRecorder {
            val dir = File(AppPaths.dataDir, "voice").apply { mkdirs() }
            return VoiceRecorder(File(dir, "voice-${System.currentTimeMillis()}.ogg"), onFail).apply { worker.start() }
        }
    }
}

private fun OutputStream.writeLe(value: Long, bytes: Int) {
    for (i in 0 until bytes) write((value shr (8 * i)).toInt() and 0xFF)
}

private class OggWriter(private val out: OutputStream) {
    private val serial = Random.nextInt()
    private var sequence = 0

    fun page(packets: List<ByteArray>, granule: Long, flags: Int) {
        val segments = ByteArrayOutputStream()
        packets.forEach { packet ->
            var left = packet.size
            while (left >= 255) {
                segments.write(255)
                left -= 255
            }
            segments.write(left)
        }
        val table = segments.toByteArray()
        val header = ByteArrayOutputStream().apply {
            write("OggS".toByteArray())
            write(0)
            write(flags)
            writeLe(granule, 8)
            writeLe(serial.toLong(), 4)
            writeLe((sequence++).toLong(), 4)
            writeLe(0, 4)
            write(table.size)
            write(table)
        }.toByteArray()
        val body = ByteArrayOutputStream().apply { packets.forEach { write(it) } }.toByteArray()
        var crc = 0
        for (byte in header) crc = update(crc, byte)
        for (byte in body) crc = update(crc, byte)
        for (i in 0 until 4) header[22 + i] = (crc ushr (8 * i)).toByte()
        out.write(header)
        out.write(body)
    }

    private fun update(crc: Int, byte: Byte): Int = (crc shl 8) xor Table[((crc ushr 24) xor (byte.toInt() and 0xFF)) and 0xFF]

    companion object {
        const val FirstPage = 2
        const val LastPage = 4
        private val Table = IntArray(256) { index ->
            var r = index shl 24
            repeat(8) { r = if (r and Int.MIN_VALUE != 0) (r shl 1) xor 0x04C11DB7 else r shl 1 }
            r
        }
    }
}
