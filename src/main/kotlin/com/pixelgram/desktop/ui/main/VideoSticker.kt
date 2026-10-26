package com.pixelgram.desktop.ui.main

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.pixelgram.desktop.core.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import java.nio.ByteBuffer
import kotlin.math.roundToInt

class StickerClip(val frames: List<ImageBitmap>, val times: LongArray, val durationMs: Long, val bytes: Long)

object VideoStickers {
    private const val MaxCacheBytes = 160L * 1024 * 1024
    private const val MaxSide = 512
    private const val FallbackFrameMs = 33L
    private val cache = LinkedHashMap<String, StickerClip>(16, 0.75f, true)
    private val skia = HashMap<String, List<Image>>()
    private var total = 0L
    private val gate = Mutex()

    suspend fun clip(path: String, side: Int): StickerClip? {
        val target = side.coerceIn(1, MaxSide)
        val key = "$path@$target"
        synchronized(this) { cache[key] }?.let { return it }
        return gate.withLock {
            synchronized(this) { cache[key] }?.let { return@withLock it }
            val decoded = withContext(Dispatchers.IO) { decode(path, target) } ?: return@withLock null
            synchronized(this) {
                cache[key] = decoded.first
                skia[key] = decoded.second
                total += decoded.first.bytes
                val iterator = cache.entries.iterator()
                while (total > MaxCacheBytes && iterator.hasNext()) {
                    val eldest = iterator.next()
                    if (eldest.key == key) continue
                    total -= eldest.value.bytes
                    iterator.remove()
                    skia.remove(eldest.key)?.let { old -> java.awt.EventQueue.invokeLater { old.forEach(Image::close) } }
                }
            }
            decoded.first
        }
    }

    private fun decode(path: String, side: Int): Pair<StickerClip, List<Image>>? =
        runCatching { decodeWith(path, side, "libvpx-vp9") }.getOrNull()
            ?: runCatching { decodeWith(path, side, null) }.onFailure { AppLog.write("Video sticker failed", it) }.getOrNull()

    private fun decodeWith(path: String, side: Int, codec: String?): Pair<StickerClip, List<Image>>? {
        val grabber = FFmpegFrameGrabber(path).apply {
            pixelFormat = avutil.AV_PIX_FMT_BGRA
            codec?.let { videoCodecName = it }
        }
        val images = ArrayList<Image>()
        val times = ArrayList<Long>()
        var bytes = 0L
        try {
            grabber.start()
            val width = grabber.imageWidth.coerceAtLeast(1)
            val height = grabber.imageHeight.coerceAtLeast(1)
            val scale = side.toFloat() / maxOf(width, height)
            if (scale < 1f) {
                grabber.stop()
                grabber.imageWidth = (width * scale).roundToInt().coerceAtLeast(1)
                grabber.imageHeight = (height * scale).roundToInt().coerceAtLeast(1)
                grabber.start()
            }
            var pixels = ByteArray(0)
            while (true) {
                val frame = grabber.grabImage() ?: break
                val buffer = frame.image?.firstOrNull() as? ByteBuffer ?: continue
                val stride = frame.imageStride
                val size = stride * frame.imageHeight
                if (pixels.size != size) pixels = ByteArray(size)
                buffer.duplicate().apply { rewind() }.get(pixels, 0, minOf(size, buffer.capacity()))
                val info = ImageInfo(frame.imageWidth, frame.imageHeight, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL)
                images += Image.makeRaster(info, pixels, stride)
                times += frame.timestamp / 1000
                bytes += size
            }
        } finally {
            runCatching { grabber.stop() }
            runCatching { grabber.release() }
        }
        if (images.isEmpty()) return null
        val start = times.first()
        val offsets = LongArray(times.size) { times[it] - start }
        val duration = (offsets.last() + FallbackFrameMs).coerceAtLeast(FallbackFrameMs)
        return StickerClip(images.map { it.toComposeImageBitmap() }, offsets, duration, bytes) to images
    }
}

@Composable
fun VideoStickerView(path: String, modifier: Modifier, play: Boolean = true) {
    BoxWithConstraints(modifier) {
        val side = with(LocalDensity.current) { minOf(maxWidth, maxHeight).roundToPx() }
        val clip by produceState<StickerClip?>(null, path, side) { value = VideoStickers.clip(path, side) }
        val focused = androidx.compose.ui.platform.LocalWindowInfo.current.isWindowFocused
        val index by produceState(0, clip, play, focused) {
            val current = clip ?: return@produceState
            if (!play || !focused) return@produceState
            val start = System.nanoTime() - current.times.getOrElse(value) { 0L } * 1_000_000
            while (true) {
                val at = (System.nanoTime() - start) / 1_000_000 % current.durationMs
                val next = current.times.indexOfLast { it <= at }.coerceAtLeast(0)
                if (next != value) value = next
                val nextAt = current.times.getOrNull(next + 1) ?: current.durationMs
                kotlinx.coroutines.delay((nextAt - at).coerceAtLeast(1L))
            }
        }
        Canvas(Modifier.matchParentSize().graphicsLayer()) {
            val current = clip ?: return@Canvas
            val frame = current.frames[index]
            val fit = minOf(size.width / frame.width, size.height / frame.height)
            val w = (frame.width * fit).roundToInt()
            val h = (frame.height * fit).roundToInt()
            drawImage(
                frame,
                dstOffset = IntOffset(((size.width - w) / 2).roundToInt(), ((size.height - h) / 2).roundToInt()),
                dstSize = IntSize(w, h),
                filterQuality = FilterQuality.Medium,
            )
        }
    }
}
