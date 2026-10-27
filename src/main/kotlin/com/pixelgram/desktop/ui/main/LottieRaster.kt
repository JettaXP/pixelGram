package com.pixelgram.desktop.ui.main

import androidx.compose.runtime.mutableIntStateOf
import com.pixelgram.desktop.core.AppLog
import org.jetbrains.skia.Image
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import org.jetbrains.skia.skottie.Animation
import java.io.File
import java.util.concurrent.Executors
import java.util.zip.GZIPInputStream

internal class LottieFrames(private val path: String, val width: Int, val height: Int) {
    private var animation: Animation? = null
    private var frames: Array<Image?> = emptyArray()
    private var pending = BooleanArray(0)
    private var failed = false
    @Volatile var closed = false
        private set
    @Volatile var lastUsed = System.nanoTime()
        private set
    @Volatile var bytes = 0L
        private set
    val ready = mutableIntStateOf(0)

    fun frame(index: Int): Image? {
        lastUsed = System.nanoTime()
        synchronized(this) {
            if (closed || failed) return null
            if (frames.isEmpty()) {
                if (!pending.contains(true) && animation == null) {
                    pending = BooleanArray(1) { true }
                    LottieRaster.run { load(index) }
                }
                return null
            }
            val count = frames.size
            val wanted = Math.floorMod(index, count)
            for (ahead in 0 until LottieRaster.PrefetchFrames) schedule((wanted + ahead) % count)
            frames[wanted]?.let { return it }
            for (back in 1 until count) frames[Math.floorMod(wanted - back, count)]?.let { return it }
            return null
        }
    }

    private fun schedule(index: Int) {
        if (frames[index] != null || pending[index]) return
        pending[index] = true
        LottieRaster.run { render(index) }
    }

    private fun load(first: Int) {
        val parsed = runCatching {
            val json = GZIPInputStream(File(path).inputStream()).use { it.readBytes().decodeToString() }
            Animation.makeFromString(json)
        }.onFailure { AppLog.write("Sticker frames failed", it) }.getOrNull()
        synchronized(this) {
            if (parsed == null || closed) {
                failed = parsed == null
                parsed?.close()
                return
            }
            animation = parsed
            val fps = parsed.fPS.takeIf { it > 0f } ?: LottieRaster.DefaultFps
            val count = (parsed.duration * fps).toInt().coerceAtLeast(1)
            frames = arrayOfNulls(count)
            pending = BooleanArray(count)
            schedule(Math.floorMod(first, count))
        }
    }

    private fun render(index: Int) {
        val current = synchronized(this) { animation?.takeIf { !closed } } ?: return
        val image = runCatching {
            Surface.makeRasterN32Premul(width, height).use { surface ->
                synchronized(current) {
                    current.seekFrame(index.toFloat())
                    current.render(surface.canvas, Rect.makeWH(width.toFloat(), height.toFloat()))
                }
                surface.makeImageSnapshot()
            }
        }.getOrNull() ?: return
        val kept = synchronized(this) {
            if (closed || index >= frames.size) {
                false
            } else {
                frames[index] = image
                pending[index] = false
                bytes += width.toLong() * height * 4
                true
            }
        }
        if (!kept) {
            image.close()
            return
        }
        java.awt.EventQueue.invokeLater { ready.intValue++ }
        LottieRaster.trim()
    }

    fun close() {
        val dropped = synchronized(this) {
            closed = true
            val list = frames.filterNotNull()
            frames = emptyArray()
            pending = BooleanArray(0)
            bytes = 0
            val parsed = animation
            animation = null
            list to parsed
        }
        java.awt.EventQueue.invokeLater {
            dropped.first.forEach { it.close() }
            dropped.second?.let { parsed -> synchronized(parsed) { parsed.close() } }
        }
    }
}

internal object LottieRaster {
    const val DefaultFps = 60f
    const val PrefetchFrames = 6
    private const val BudgetBytes = 160L * 1024 * 1024
    private const val KeepAliveNs = 2_000_000_000L

    private val workers = Executors.newFixedThreadPool(maxOf(2, Runtime.getRuntime().availableProcessors() / 2)) { task ->
        Thread(task, "pixelgram-lottie").apply {
            isDaemon = true
            priority = Thread.NORM_PRIORITY - 1
        }
    }
    private val players = LinkedHashMap<String, LottieFrames>()

    fun run(task: () -> Unit) {
        workers.execute(task)
    }

    @Synchronized
    fun frames(path: String, width: Int, height: Int): LottieFrames {
        val key = "$path@${width}x$height"
        players[key]?.let { if (!it.closed) return it }
        return LottieFrames(path, width, height).also { players[key] = it }
    }

    @Synchronized
    fun trim() {
        var total = players.values.sumOf { it.bytes }
        if (total <= BudgetBytes) return
        val now = System.nanoTime()
        players.entries
            .filter { now - it.value.lastUsed > KeepAliveNs }
            .sortedBy { it.value.lastUsed }
            .forEach { (key, frames) ->
                if (total <= BudgetBytes) return
                total -= frames.bytes
                frames.close()
                players.remove(key)
            }
    }
}
