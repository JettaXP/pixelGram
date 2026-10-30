package com.pixelgram.desktop.ui.main

import kotlin.math.roundToInt
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.pixelgram.desktop.ui.components.expressive.Slider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.automirrored.rounded.VolumeOff
import com.pixelgram.desktop.ui.icons.automirrored.rounded.VolumeUp
import com.pixelgram.desktop.ui.icons.rounded.Close
import com.pixelgram.desktop.ui.icons.rounded.Pause
import com.pixelgram.desktop.ui.icons.rounded.PlayArrow
import com.pixelgram.desktop.ui.icons.rounded.Replay
import com.pixelgram.desktop.ui.components.expressive.FilledIconButton
import com.pixelgram.desktop.ui.components.expressive.FilledTonalIconButton
import androidx.compose.material3.Icon
import com.pixelgram.desktop.ui.components.expressive.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.pixelgram.desktop.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.pixelgram.desktop.core.AppLog
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.core.AudioPlayer
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.theme.EmojiText
import com.pixelgram.desktop.ui.theme.PixelTheme
import it.tdlight.jni.TdApi
import kotlinx.coroutines.delay
import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Frame
import org.bytedeco.javacv.FrameGrabber
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import java.awt.GraphicsEnvironment
import java.nio.ByteBuffer
import java.nio.ShortBuffer
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine
import org.jetbrains.skia.Data
import org.jetbrains.skia.impl.BufferUtil

class VideoRequest(
    val telegram: Telegram,
    val file: TdApi.File,
    val thumb: TdApi.File?,
    val mini: TdApi.Minithumbnail?,
    val duration: Int,
    val caption: String,
    val loop: Boolean,
)

object VideoPlayer {
    var current by mutableStateOf<VideoRequest?>(null)

    fun open(telegram: Telegram, video: TdApi.Video, caption: String) {
        open(VideoRequest(telegram, video.video, video.thumbnail?.file, video.minithumbnail, video.duration, caption, false))
    }

    fun open(telegram: Telegram, animation: TdApi.Animation, caption: String) {
        open(VideoRequest(telegram, animation.animation, animation.thumbnail?.file, animation.minithumbnail, animation.duration, caption, true))
    }

    private fun open(request: VideoRequest) {
        AppLog.trace("play video")
        if (AudioPlayer.playing) AudioPlayer.pauseOrResume()
        request.telegram.download(request.file)
        current = request
    }
}

private const val SeekStepMs = 5_000L
internal const val ScrimAlpha = 0.72f
internal val MediaInsetX = 72.dp
internal val MediaInsetY = 56.dp

internal fun viewerBounds(): java.awt.Rectangle =
    com.pixelgram.desktop.telegram.Notifications.window?.takeIf { it.isShowing }?.bounds
        ?: GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration.bounds
private const val ControlsHideMs = 2_500L
private const val LateFrameUs = 120_000L
private const val AudioBufferSeconds = 0.4
private const val MaxDecodeWidth = 1920
private const val MaxFrameGapNs = 100_000_000L
private const val AudioDrainPollMs = 10L
private const val AudioStartWaitNs = 1_500_000_000L
private const val AudioPollMs = 5L
private const val ResyncUs = 250_000L
private const val DriftShare = 8
private const val OutputRate = 48_000
private const val OutputChannels = 2
private val OutputRates = setOf(44_100, 48_000)
private const val SpareFrames = 3
private const val RetiredFrames = 8
private val VolumeSliderWidth = 110.dp

internal class VideoEngine(private val path: String, private val loop: Boolean, private val sound: Boolean = true, private val maxWidth: Int = MaxDecodeWidth) {
    var frame by mutableStateOf<org.jetbrains.skia.Image?>(null)
    var position by mutableLongStateOf(0L)
    var duration by mutableLongStateOf(0L)
    var playing by mutableStateOf(true)
    var ended by mutableStateOf(false)
    var failed by mutableStateOf(false)
    var muted by mutableStateOf(false)
    var volume by mutableFloatStateOf(1f)

    private val lock = Object()
    @Volatile private var running = true
    @Volatile private var seekTo = -1L
    @Volatile private var audioSeekTo = -1L
    @Volatile private var line: SourceDataLine? = null
    @Volatile private var audioDone = true
    @Volatile private var audioBaseUs = 0L
    @Volatile private var lineMarkUs = 0L
    private var audioThread: Thread? = null
    private var shown: PooledFrame? = null
    private val spare = java.util.concurrent.ConcurrentLinkedQueue<Data>()
    private val retired = ArrayList<PooledFrame>()
    private var released = false

    private class PooledFrame(val image: org.jetbrains.skia.Image, val data: Data)

    private val thread = Thread(::run, "pixelgram-video").apply { isDaemon = true }

    fun start() = thread.start()

    fun toggle() {
        if (ended) {
            seek(0)
            ended = false
        }
        playing = !playing
        synchronized(lock) { lock.notifyAll() }
    }

    fun seek(ms: Long) {
        val target = ms.coerceIn(0, duration.coerceAtLeast(0))
        seekTo = target
        audioSeekTo = target
        ended = false
        synchronized(lock) { lock.notifyAll() }
    }

    fun close() {
        running = false
        synchronized(lock) { lock.notifyAll() }
    }

    private fun audioClockUs(): Long? {
        val output = line ?: return null
        if (audioDone || audioSeekTo >= 0) return null
        return audioBaseUs + (output.microsecondPosition - lineMarkUs)
    }

    private fun awaitAudio(): Long? {
        val deadline = System.nanoTime() + AudioStartWaitNs
        var first: Long? = null
        while (running && playing && seekTo < 0 && !audioDone && System.nanoTime() < deadline) {
            val heard = audioClockUs()
            if (heard != null && first == null) first = heard
            if (heard != null && heard != first) return heard
            Thread.sleep(AudioPollMs)
        }
        return null
    }

    private fun run() {
        val grabber = FFmpegFrameGrabber(path).apply {
            pixelFormat = avutil.AV_PIX_FMT_BGRA
            setVideoOption("threads", "0")
        }
        try {
            grabber.start()
            if (grabber.imageWidth > maxWidth) {
                val height = (grabber.imageHeight.toLong() * maxWidth / grabber.imageWidth).toInt() and 1.inv()
                grabber.stop()
                grabber.imageWidth = maxWidth
                grabber.imageHeight = height
                grabber.start()
            }
            duration = grabber.lengthInTime / 1000
            var startAudio = grabber.audioChannels > 0 && !loop && sound
            if (startAudio) {
                audioDone = false
                audioThread = Thread(::runAudio, "pixelgram-video-audio").apply {
                    isDaemon = true
                    priority = Thread.MAX_PRIORITY
                    start()
                }
            }
            var base = System.nanoTime()
            var baseUs = 0L
            var heard = -1L
            var lastShown = 0L
            while (running) {
                if (!playing && seekTo < 0) {
                    synchronized(lock) { while (running && !playing && seekTo < 0) lock.wait() }
                    if (!running) break
                    base = System.nanoTime()
                    baseUs = position * 1000
                    heard = -1
                }
                val target = seekTo
                if (target >= 0) {
                    seekTo = -1
                    grabber.setTimestamp(target * 1000, true)
                    base = System.nanoTime()
                    baseUs = target * 1000
                    heard = -1
                    position = target
                    if (!playing) {
                        grabber.grabImage()?.let { show(it) }
                        continue
                    }
                }
                if (startAudio && frame != null) {
                    startAudio = false
                    val started = awaitAudio()
                    base = System.nanoTime()
                    baseUs = started ?: (position * 1000)
                    heard = started ?: -1
                }
                val next = grabber.grabImage()
                if (next == null) {
                    if (loop) {
                        grabber.setTimestamp(0)
                        base = System.nanoTime()
                        baseUs = 0
                        continue
                    }
                    while (running && playing && seekTo < 0 && !audioDone) Thread.sleep(AudioDrainPollMs)
                    if (seekTo >= 0 || !running) continue
                    ended = true
                    playing = false
                    continue
                }
                val audible = audioClockUs()
                if (audible != null) {
                    if (heard >= 0 && audible != heard) {
                        val drift = audible - (baseUs + (System.nanoTime() - base) / 1000)
                        if (kotlin.math.abs(drift) > ResyncUs) {
                            base = System.nanoTime()
                            baseUs = audible
                        } else {
                            baseUs += drift / DriftShare
                        }
                    }
                    heard = audible
                }
                val wait = next.timestamp - (baseUs + (System.nanoTime() - base) / 1000)
                if (wait > 0) Thread.sleep(wait / 1000, ((wait % 1000) * 1000).toInt())
                val now = System.nanoTime()
                if (wait > -LateFrameUs || frame == null || now - lastShown > MaxFrameGapNs) {
                    show(next)
                    lastShown = now
                }
                position = next.timestamp / 1000
            }
        } catch (error: Throwable) {
            AppLog.write("Video playback failed", error)
            failed = true
        } finally {
            synchronized(lock) { lock.notifyAll() }
            runCatching { grabber.stop() }
            runCatching { grabber.release() }
            java.awt.EventQueue.invokeLater(::releaseFrames)
        }
    }

    private fun runAudio() {
        val grabber = FFmpegFrameGrabber(path).apply { sampleMode = FrameGrabber.SampleMode.SHORT }
        var output: SourceDataLine? = null
        try {
            grabber.start()
            val rate = grabber.sampleRate.takeIf { it in OutputRates } ?: OutputRate
            grabber.sampleRate = rate
            grabber.audioChannels = OutputChannels
            output = openLine(rate, OutputChannels) ?: return
            lineMarkUs = output.microsecondPosition
            audioBaseUs = 0
            line = output
            while (running) {
                if (!playing && audioSeekTo < 0) {
                    output.stop()
                    synchronized(lock) { while (running && !playing && audioSeekTo < 0) lock.wait() }
                    if (!running) break
                    if (playing) output.start()
                }
                val target = audioSeekTo
                if (target >= 0) {
                    audioSeekTo = -1
                    grabber.setTimestamp(target * 1000)
                    output.flush()
                    lineMarkUs = output.microsecondPosition
                    audioBaseUs = target * 1000
                    audioDone = false
                    if (playing) output.start()
                    continue
                }
                if (audioDone) {
                    synchronized(lock) { while (running && audioSeekTo < 0) lock.wait() }
                    continue
                }
                val next = grabber.grabSamples()
                if (next == null) {
                    output.drain()
                    audioDone = true
                    continue
                }
                writeAudio(output, next)
            }
        } catch (_: InterruptedException) {
        } catch (error: Throwable) {
            AppLog.write("Video audio failed", error)
        } finally {
            audioDone = true
            line = null
            runCatching { output?.stop() }
            runCatching { output?.close() }
            runCatching { grabber.stop() }
            runCatching { grabber.release() }
        }
    }

    private fun openLine(rate: Int, channels: Int): SourceDataLine? = runCatching {
        val format = AudioFormat(rate.toFloat(), 16, channels, true, false)
        com.pixelgram.desktop.core.AudioDevices.sourceLine(format).apply {
            open(format, (rate * channels * 2 * AudioBufferSeconds).toInt())
            start()
        }
    }.onFailure { AppLog.write("Video audio unavailable", it) }.getOrNull()

    private fun writeAudio(output: SourceDataLine, frame: Frame) {
        val samples = frame.samples.firstOrNull() as? ShortBuffer ?: return
        val count = samples.remaining()
        val bytes = ByteArray(count * 2)
        val start = samples.position()
        val gain = if (muted) 0f else volume
        for (i in 0 until count) {
            val value = (samples.get(start + i) * gain).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            bytes[i * 2] = value.toByte()
            bytes[i * 2 + 1] = (value shr 8).toByte()
        }
        output.write(bytes, 0, bytes.size)
    }

    private fun show(frame: Frame) {
        val buffer = frame.image?.firstOrNull() as? ByteBuffer ?: return
        val stride = frame.imageStride
        val size = stride * frame.imageHeight
        val info = ImageInfo(frame.imageWidth, frame.imageHeight, ColorType.BGRA_8888, ColorAlphaType.OPAQUE)
        var data = spare.poll()
        while (data != null && data.size != size) {
            data.close()
            data = spare.poll()
        }
        val pixels = data ?: Data.makeUninitialized(size)
        val source = buffer.duplicate().apply { rewind() }
        source.limit(minOf(source.capacity(), size))
        BufferUtil.getByteBufferFromPointer(pixels.writableData(), size).put(source)
        val image = org.jetbrains.skia.Image.makeRaster(info, pixels, stride)
        val stale = shown
        shown = PooledFrame(image, pixels)
        this.frame = image
        if (stale != null) java.awt.EventQueue.invokeLater { retire(stale) }
    }

    private fun retire(stale: PooledFrame) {
        retired.add(stale)
        val iterator = retired.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val unused = entry.image.refCount == 1 && entry.image !== frame
            if (!unused && retired.size <= RetiredFrames) continue
            iterator.remove()
            entry.image.close()
            if (unused && !released && spare.size < SpareFrames) spare.add(entry.data) else entry.data.close()
        }
    }

    private fun releaseFrames() {
        released = true
        retired.forEach { entry ->
            entry.image.close()
            entry.data.close()
        }
        retired.clear()
        while (true) spare.poll()?.close() ?: break
    }

}

@Composable
fun VideoPlayerWindow(request: VideoRequest, settings: AppSettings) {
    val shown = remember(request) { MutableTransitionState(false).apply { targetState = true } }
    val close = { shown.targetState = false }
    LaunchedEffect(shown.isIdle, shown.currentState) {
        if (shown.isIdle && !shown.currentState && VideoPlayer.current === request) VideoPlayer.current = null
    }
    val path = request.telegram.localPath(request.file)
    val engine = remember(path) { path?.let { VideoEngine(it, request.loop).apply { start() } } }
    DisposableEffect(engine) { onDispose { engine?.close() } }
    val bounds = remember { viewerBounds() }
    Window(
        onCloseRequest = close,
        state = rememberWindowState(
            position = WindowPosition(bounds.x.dp, bounds.y.dp),
            size = DpSize(bounds.width.dp, bounds.height.dp),
        ),
        title = "pixelGram video",
        undecorated = true,
        transparent = true,
        resizable = false,
        alwaysOnTop = true,
        onKeyEvent = { event ->
            if (event.type != KeyEventType.KeyDown) return@Window false
            when (event.key) {
                Key.Escape -> close()
                Key.Spacebar, Key.K -> engine?.toggle()
                Key.DirectionLeft -> engine?.let { it.seek(it.position - SeekStepMs) }
                Key.DirectionRight -> engine?.let { it.seek(it.position + SeekStepMs) }
                Key.M -> engine?.let { it.muted = !it.muted }
                else -> return@Window false
            }
            true
        },
    ) {
        PixelTheme(settings) {
            Box(Modifier.fillMaxSize()) {
            AnimatedVisibility(shown, enter = fadeIn(), exit = fadeOut()) {
                VideoSurface(request, engine, close)
            }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun VideoSurface(request: VideoRequest, engine: VideoEngine?, close: () -> Unit) {
    var lastMove by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var controls by remember { mutableStateOf(true) }
    LaunchedEffect(lastMove, engine?.playing) {
        controls = true
        if (engine?.playing == true) {
            delay(ControlsHideMs)
            controls = false
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = ScrimAlpha))
            .onPointerEvent(PointerEventType.Move) { lastMove = System.currentTimeMillis() }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = close),
        contentAlignment = Alignment.Center,
    ) {
        val ratio by remember(engine) { derivedStateOf { engine?.frame?.let { it.width.toFloat() / it.height.coerceAtLeast(1) } } }
        val aspect = ratio
        val media = Modifier
            .fillMaxSize()
            .padding(horizontal = MediaInsetX, vertical = MediaInsetY)
        if (engine != null && aspect != null) {
            Box(media, contentAlignment = Alignment.Center) {
                VideoFrame(
                    engine,
                    Modifier
                        .aspectRatio(aspect)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { engine.toggle() },
                )
            }
        } else {
            ThumbImage(request.thumb, request.mini, request.telegram, media, ContentScale.Fit)
            if (engine?.failed == true) {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.errorContainer) {
                    Text("Can't play this video", modifier = Modifier.padding(16.dp))
                }
            } else {
                LoadingIndicator(Modifier.size(72.dp), color = Color.White)
            }
        }
        AnimatedVisibility(controls, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.TopEnd)) {
            FilledTonalIconButton(onClick = close, modifier = Modifier.padding(24.dp)) {
                Icon(Icons.Rounded.Close, contentDescription = "Close")
            }
        }
        if (engine != null) {
            AnimatedVisibility(
                controls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
            ) {
                VideoControls(request, engine)
            }
        }
    }
}

@Composable
private fun VideoControls(request: VideoRequest, engine: VideoEngine) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
        modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth(0.7f).clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
        ) {},
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            if (request.caption.isNotBlank()) {
                EmojiText(
                    request.caption,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledIconButton(
                    onClick = engine::toggle,
                    shapes = IconButtonDefaults.shapes(),
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        when {
                            engine.ended -> Icons.Rounded.Replay
                            engine.playing -> Icons.Rounded.Pause
                            else -> Icons.Rounded.PlayArrow
                        },
                        contentDescription = if (engine.playing) "Pause" else "Play",
                    )
                }
                Text(formatDuration((engine.position / 1000).toInt()), style = MaterialTheme.typography.labelLarge)
                val total = engine.duration.takeIf { it > 0 } ?: (request.duration * 1000L)
                WavySeekBar(
                    fraction = if (total > 0) engine.position.toFloat() / total else 0f,
                    onSeek = { engine.seek((it * total).toLong()) },
                    modifier = Modifier.weight(1f),
                    playing = engine.playing,
                )
                Text(formatDuration((total / 1000).toInt()), style = MaterialTheme.typography.labelLarge)
                if (!request.loop) {
                    IconButton(onClick = { engine.muted = !engine.muted }) {
                        Icon(
                            if (engine.muted || engine.volume == 0f) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = "Mute",
                        )
                    }
                    Slider(
                        value = if (engine.muted) 0f else engine.volume,
                        onValueChange = {
                            engine.volume = it
                            engine.muted = false
                        },
                        modifier = Modifier.width(VolumeSliderWidth),
                    )
                }
            }
        }
    }
}

@Composable
fun InlineVideo(path: String, modifier: Modifier = Modifier, maxWidth: Int = InlineDecodeWidth) {
    var settled by remember(path) { mutableStateOf(false) }
    LaunchedEffect(path) {
        kotlinx.coroutines.delay(InlineStartDelayMs)
        settled = true
    }
    if (!settled) return
    val engine = remember(path) { if (InlineSlots.acquire()) VideoEngine(path, loop = true, sound = false, maxWidth = maxWidth).apply { start() } else null }
    DisposableEffect(engine) {
        onDispose {
            if (engine != null) {
                engine.close()
                InlineSlots.release()
            }
        }
    }
    val playing = engine ?: return
    val focused = androidx.compose.ui.platform.LocalWindowInfo.current.isWindowFocused
    LaunchedEffect(playing, focused) { if (playing.playing != focused) playing.toggle() }
    VideoFrame(playing, modifier)
}

private object InlineSlots {
    private var used = 0

    @Synchronized
    fun acquire(): Boolean {
        if (used >= MaxInlineVideos) return false
        used++
        return true
    }

    @Synchronized
    fun release() {
        used = (used - 1).coerceAtLeast(0)
    }
}

private const val InlineStartDelayMs = 350L
private const val MaxInlineVideos = 6

@Composable
internal fun VideoFrame(engine: VideoEngine, modifier: Modifier) {
    androidx.compose.foundation.layout.Spacer(
        modifier.drawBehind {
            val image = engine.frame ?: return@drawBehind
            val scale = maxOf(size.width / image.width, size.height / image.height)
            val sourceWidth = (size.width / scale).coerceIn(1f, image.width.toFloat())
            val sourceHeight = (size.height / scale).coerceIn(1f, image.height.toFloat())
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawImageRect(
                    image,
                    org.jetbrains.skia.Rect.makeXYWH((image.width - sourceWidth) / 2f, (image.height - sourceHeight) / 2f, sourceWidth, sourceHeight),
                    org.jetbrains.skia.Rect.makeWH(size.width, size.height),
                    org.jetbrains.skia.SamplingMode.LINEAR,
                    null,
                    true,
                )
            }
        },
    )
}

private const val InlineDecodeWidth = 640
