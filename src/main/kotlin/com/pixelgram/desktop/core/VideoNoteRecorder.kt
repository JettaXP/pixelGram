package com.pixelgram.desktop.core

import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.javacv.FFmpegFrameRecorder
import org.bytedeco.javacv.Frame
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import java.io.File
import java.nio.ByteBuffer
import java.nio.ShortBuffer
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.TargetDataLine
import kotlin.math.log10
import kotlin.math.sqrt

class VideoNote(val file: File, val duration: Int, val length: Int, val thumbnail: File?)

class VideoNoteRecorder private constructor(
    private val file: File,
    private val device: String,
    private val onFail: (String) -> Unit,
) {
    @Volatile var level = 0f
        private set
    @Volatile var elapsedMs = 0L
        private set
    @Volatile var preview: Image? = null
        private set
    @Volatile var limitReached = false
        private set
    @Volatile private var running = true
    @Volatile private var discard = false
    private val lock = Object()
    private var recorder: FFmpegFrameRecorder? = null
    private var camera: CameraSession? = null
    private var line: TargetDataLine? = null
    private var rate = AudioRate
    private var startNanos = 0L
    private var lastTimestamp = -1L
    private var result: VideoNote? = null
    private val square = Frame(Side, Side, Frame.DEPTH_UBYTE, Channels)
    private val worker = Thread(::record, "video-note").apply { isDaemon = true }

    fun stop(): VideoNote? {
        running = false
        worker.join(StopWaitMs)
        return result
    }

    fun cancel() {
        discard = true
        running = false
    }

    private fun record() {
        val demo = System.getProperty("pixelgram.demo") == "true"
        camera = CameraSession.open(device)
        if (camera == null && !demo) return fail("No camera found")
        line = openLine()
        rate = line?.format?.sampleRate?.toInt() ?: AudioRate
        val output = FFmpegFrameRecorder(file, Side, Side, 1).apply {
            format = "mp4"
            videoCodecName = "libopenh264"
            pixelFormat = avutil.AV_PIX_FMT_YUV420P
            frameRate = FrameRate.toDouble()
            videoBitrate = VideoBitrate
            gopSize = FrameRate
            audioCodec = avcodec.AV_CODEC_ID_AAC
            sampleRate = rate
            audioBitrate = AudioBitrate
        }
        try {
            output.start()
        } catch (error: Exception) {
            AppLog.write("Video note encoder failed", error)
            return fail("Recording failed")
        }
        recorder = output
        startNanos = System.nanoTime()
        val audio = Thread(::recordAudio, "video-note-audio").apply { isDaemon = true }
        audio.start()
        try {
            val session = camera
            if (session != null) {
                val watcher = Thread {
                    while (running) Thread.sleep(PollMs)
                    session.stop()
                }.apply { isDaemon = true }
                watcher.start()
                session.run(onRaw = ::onCameraFrame) { image -> preview = image }
            } else {
                recordDemoVideo()
            }
        } catch (error: Exception) {
            AppLog.write("Video note recording failed", error)
            discard = true
        } finally {
            running = false
            audio.join(StopWaitMs)
            synchronized(lock) {
                runCatching { output.stop() }
                runCatching { output.release() }
                recorder = null
            }
        }
        if (discard || elapsedMs < MinDurationMs) {
            file.delete()
            return
        }
        result = VideoNote(file, ((elapsedMs + 500) / 1000).toInt().coerceAtLeast(1), Side, thumbnail())
    }

    private fun fail(message: String) {
        running = false
        discard = true
        onFail(message)
    }

    private fun tick() {
        elapsedMs = (System.nanoTime() - startNanos) / 1_000_000
        if (elapsedMs >= MaxDurationMs) {
            limitReached = true
            running = false
        }
    }

    private fun onCameraFrame(frame: Frame) {
        if (!running) return
        val buffer = frame.image?.firstOrNull() as? ByteBuffer ?: return
        val side = minOf(frame.imageWidth, frame.imageHeight)
        val left = (frame.imageWidth - side) / 2
        val top = (frame.imageHeight - side) / 2
        val crop = Frame(side, side, Frame.DEPTH_UBYTE, Channels)
        val target = crop.image[0] as ByteBuffer
        val row = ByteArray(side * Channels)
        for (y in 0 until side) {
            buffer.position((top + y) * frame.imageStride + left * Channels)
            buffer.get(row)
            target.position(y * crop.imageStride)
            target.put(row)
        }
        buffer.rewind()
        target.rewind()
        writeVideo(crop)
    }

    private fun writeVideo(frame: Frame) {
        tick()
        val timestamp = elapsedMs * 1000
        synchronized(lock) {
            val output = recorder ?: return
            if (timestamp > lastTimestamp) {
                output.timestamp = timestamp
                lastTimestamp = timestamp
            }
            output.record(frame, avutil.AV_PIX_FMT_BGRA)
        }
    }

    private fun recordDemoVideo() {
        val pixels = ByteArray(Side * Side * Channels)
        var index = 0
        while (running) {
            val shift = index * 3
            for (y in 0 until Side) {
                for (x in 0 until Side) {
                    val offset = (y * Side + x) * Channels
                    pixels[offset] = ((x + shift) % 256).toByte()
                    pixels[offset + 1] = ((y + shift / 2) % 256).toByte()
                    pixels[offset + 2] = (180 + (index % 60)).toByte()
                    pixels[offset + 3] = 0xFF.toByte()
                }
            }
            val buffer = square.image[0] as ByteBuffer
            buffer.rewind()
            buffer.put(pixels)
            buffer.rewind()
            preview = Image.makeRaster(ImageInfo(Side, Side, ColorType.BGRA_8888, ColorAlphaType.OPAQUE), pixels, Side * Channels)
            writeVideo(square)
            index++
            Thread.sleep(1000L / FrameRate)
        }
    }

    private fun recordAudio() {
        val chunk = rate / ChunksPerSecond
        val bytes = ByteArray(chunk * 2)
        val samples = ShortArray(chunk)
        val input = line
        try {
            input?.start()
            val demo = System.getProperty("pixelgram.demo") == "true"
            var tone = 0.0
            while (running) {
                if (input != null) {
                    var read = 0
                    while (read < bytes.size && running) {
                        val got = input.read(bytes, read, bytes.size - read)
                        if (got < 0) break
                        read += got
                    }
                    if (read < bytes.size) break
                } else {
                    Thread.sleep(1000L / ChunksPerSecond)
                    for (i in 0 until chunk) {
                        val value = if (demo) (kotlin.math.sin(tone * 2 * Math.PI * 220 / rate) * Short.MAX_VALUE * 0.2).toInt() else 0
                        bytes[i * 2] = value.toByte()
                        bytes[i * 2 + 1] = (value shr 8).toByte()
                        tone++
                    }
                }
                var sum = 0.0
                for (i in 0 until chunk) {
                    val value = ((bytes[i * 2 + 1].toInt() shl 8) or (bytes[i * 2].toInt() and 0xFF)).toShort()
                    samples[i] = value
                    sum += value.toDouble() * value
                }
                val rms = sqrt(sum / chunk) / Short.MAX_VALUE
                level = if (rms <= 0) 0f else ((20 * log10(rms) + SilenceDb) / SilenceDb).toFloat().coerceIn(0f, 1f)
                synchronized(lock) {
                    recorder?.recordSamples(rate, 1, ShortBuffer.wrap(samples.copyOf()))
                }
            }
        } catch (error: Exception) {
            AppLog.write("Video note audio failed", error)
        } finally {
            runCatching { input?.stop() }
            runCatching { input?.close() }
        }
    }

    private fun openLine(): TargetDataLine? {
        for (candidate in Rates) {
            val format = AudioFormat(candidate.toFloat(), 16, 1, true, false)
            val opened = runCatching {
                AudioDevices.targetLine(format).apply { open(format, candidate / ChunksPerSecond * 2 * BufferChunks) }
            }.getOrNull()
            if (opened != null) return opened
        }
        return null
    }

    private fun thumbnail(): File? = runCatching {
        val image = preview ?: return null
        val side = minOf(image.width, image.height)
        val surface = Surface.makeRasterN32Premul(ThumbSide, ThumbSide)
        val left = (image.width - side) / 2f
        val top = (image.height - side) / 2f
        surface.canvas.drawImageRect(image, Rect.makeXYWH(left, top, side.toFloat(), side.toFloat()), Rect.makeWH(ThumbSide.toFloat(), ThumbSide.toFloat()))
        val data = surface.makeImageSnapshot().encodeToData(EncodedImageFormat.JPEG, ThumbQuality) ?: return null
        File(file.parentFile, file.nameWithoutExtension + ".jpg").apply { writeBytes(data.bytes) }
    }.getOrNull()

    companion object {
        const val Side = 384
        const val MaxDurationMs = 60_000L
        const val MinDurationMs = 1_000L
        private const val Channels = 4
        private const val FrameRate = 30
        private const val VideoBitrate = 1_000_000
        private const val AudioBitrate = 64_000
        private const val AudioRate = 48_000
        private val Rates = listOf(48_000, 44_100, 16_000)
        private const val ChunksPerSecond = 50
        private const val BufferChunks = 4
        private const val SilenceDb = 50.0
        private const val StopWaitMs = 5_000L
        private const val PollMs = 50L
        private const val ThumbSide = 320
        private const val ThumbQuality = 85

        fun warmUp() {
            Thread {
                runCatching {
                    org.bytedeco.javacpp.Loader.load(avcodec::class.java)
                    org.bytedeco.javacpp.Loader.load(org.bytedeco.ffmpeg.global.avformat::class.java)
                }
            }.apply { isDaemon = true }.start()
        }

        fun start(device: String, onFail: (String) -> Unit): VideoNoteRecorder {
            val dir = File(AppPaths.dataDir, "video-notes").apply { mkdirs() }
            return VideoNoteRecorder(File(dir, "round-${System.currentTimeMillis()}.mp4"), device, onFail).apply { worker.start() }
        }
    }
}
