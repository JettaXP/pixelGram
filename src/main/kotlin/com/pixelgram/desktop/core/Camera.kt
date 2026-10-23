package com.pixelgram.desktop.core

import org.bytedeco.ffmpeg.avdevice.AVDeviceInfoList
import org.bytedeco.ffmpeg.global.avdevice
import org.bytedeco.ffmpeg.global.avformat
import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.ffmpeg.avutil.AVDictionary
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Frame
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import java.io.File
import java.nio.ByteBuffer

class CameraSession private constructor(private val grabber: FFmpegFrameGrabber) {
    @Volatile var latest: Image? = null
        private set
    @Volatile private var running = true
    private var pixels = ByteArray(0)

    fun run(onFrame: (Image) -> Unit) {
        try {
            grabber.start()
            while (running) {
                val frame = grabber.grabImage() ?: continue
                toImage(frame)?.let { image ->
                    latest = image
                    onFrame(image)
                }
            }
        } finally {
            runCatching { grabber.stop() }
            runCatching { grabber.release() }
        }
    }

    fun stop() {
        running = false
    }

    fun capture(): File? {
        val image = latest ?: return null
        val data = image.encodeToData(EncodedImageFormat.JPEG, JpegQuality) ?: return null
        val dir = File(AppPaths.dataDir, "camera").apply { mkdirs() }
        return File(dir, "photo-${System.currentTimeMillis()}.jpg").apply { writeBytes(data.bytes) }
    }

    private fun toImage(frame: Frame): Image? {
        val buffer = frame.image?.firstOrNull() as? ByteBuffer ?: return null
        val stride = frame.imageStride
        val size = stride * frame.imageHeight
        if (pixels.size != size) pixels = ByteArray(size)
        buffer.duplicate().apply { rewind() }.get(pixels, 0, minOf(size, buffer.capacity()))
        val info = ImageInfo(frame.imageWidth, frame.imageHeight, ColorType.BGRA_8888, ColorAlphaType.OPAQUE)
        return Image.makeRaster(info, pixels, stride)
    }

    companion object {
        private const val JpegQuality = 92

        fun devices(): List<String> = runCatching {
            avdevice.avdevice_register_all()
            when {
                AppPaths.isWindows -> windowsDevices()
                AppPaths.isMac -> listOf("0")
                else -> File("/dev").listFiles { file -> file.name.startsWith("video") }
                    ?.map { it.absolutePath }?.sorted().orEmpty()
            }
        }.getOrDefault(emptyList())

        fun open(device: String = ""): CameraSession? = runCatching {
            avdevice.avdevice_register_all()
            val chosen = device.takeIf { it.isNotEmpty() && it in devices() } ?: devices().firstOrNull() ?: return null
            val grabber = when {
                AppPaths.isWindows -> FFmpegFrameGrabber("video=$chosen").apply { format = "dshow" }
                AppPaths.isMac -> FFmpegFrameGrabber(chosen).apply { format = "avfoundation" }
                else -> FFmpegFrameGrabber(chosen).apply { format = "video4linux2" }
            }
            grabber.pixelFormat = avutil.AV_PIX_FMT_BGRA
            CameraSession(grabber)
        }.onFailure { AppLog.write("Camera unavailable", it) }.getOrNull()

        private fun windowsDevices(): List<String> {
            val format = avformat.av_find_input_format("dshow") ?: return emptyList()
            val holder = PointerPointer<AVDeviceInfoList>(1L)
            return try {
                if (avdevice.avdevice_list_input_sources(format, null as BytePointer?, null as AVDictionary?, holder) <= 0) return emptyList()
                val list = AVDeviceInfoList(holder.get(0))
                (0 until list.nb_devices())
                    .map { list.devices(it) }
                    .filter { device -> device.device_name()?.string?.isNotBlank() == true }
                    .mapNotNull { it.device_description()?.string }
                    .distinct()
            } finally {
                avdevice.avdevice_free_list_devices(holder)
            }
        }
    }
}
