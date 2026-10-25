package com.pixelgram.desktop.ui.main

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
import androidx.compose.ui.graphics.asComposeImageBitmap
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
import java.nio.ByteOrder
import java.nio.ShortBuffer
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine
import org.jetbrains.skia.Bitmap

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
        if (AudioPlayer.playing) AudioPlayer.pauseOrResume()
        request.telegram.download(request.file)
        current = request
    }
}

private const val SeekStepMs = 5_000L
private const val ViewerShare = 0.8f
private const val ControlsHideMs = 2_500L
private const val LateFrameUs = 120_000L
private const val AudioBufferSeconds = 0.4
private const val MaxDecodeWidth = 1920
private const val MaxFrameGapNs = 100_000_000L
private const val KeptFrames = 3
private const val AudioQueueChunks = 256
private const val AudioQueueWaitMs = 20L
private const val AudioDrainPollMs = 10L
private val VolumeSliderWidth = 110.dp

internal class VideoEngine(private val path: String, private val loop: Boolean, private val sound: Boolean = true, private val maxWidth: Int = MaxDecodeWidth) {
    var frame by mutableStateOf<ImageBitmap?>(null)
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
    private var line: SourceDataLine? = null
    private val audio = java.util.concurrent.LinkedBlockingQueue<ByteArray>(AudioQueueChunks)
    private var audioThread: Thread? = null
    private var pixels = ByteArray(0)
    private val retired = ArrayDeque<Bitmap>()

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
        seekTo = ms.coerceIn(0, duration.coerceAtLeast(0))
        ended = false
        synchronized(lock) { lock.notifyAll() }
    }

    fun close() {
        running = false
        synchronized(lock) { lock.notifyAll() }
    }

    private fun run() {
        val grabber = FFmpegFrameGrabber(path).apply {
            pixelFormat = avutil.AV_PIX_FMT_BGRA
            sampleMode = FrameGrabber.SampleMode.SHORT
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
            if (grabber.audioChannels > 0 && !loop && sound) line = openLine(grabber.sampleRate, grabber.audioChannels)
            line?.let { output ->
                audioThread = Thread({ pumpAudio(output) }, "pixelgram-video-audio").apply {
                    isDaemon = true
                    start()
                }
            }
            var base = System.nanoTime()
            var baseUs = 0L
            var lastShown = 0L
            while (running) {
                if (!playing && seekTo < 0) {
                    audio.clear()
                    line?.stop()
                    synchronized(lock) { while (running && !playing && seekTo < 0) lock.wait() }
                    if (!running) break
                    base = System.nanoTime()
                    baseUs = position * 1000
                    if (playing) line?.start()
                }
                val target = seekTo
                if (target >= 0) {
                    seekTo = -1
                    grabber.setTimestamp(target * 1000, true)
                    audio.clear()
                    line?.flush()
                    base = System.nanoTime()
                    baseUs = target * 1000
                    position = target
                    if (!playing) {
                        grabber.grabImage()?.let { show(it) }
                        continue
                    }
                }
                val next = grabber.grab()
                if (next == null) {
                    if (loop) {
                        grabber.setTimestamp(0)
                        base = System.nanoTime()
                        baseUs = 0
                        continue
                    }
                    while (running && audio.isNotEmpty()) Thread.sleep(AudioDrainPollMs)
                    line?.drain()
                    ended = true
                    playing = false
                    continue
                }
                if (next.image != null) {
                    val clock = baseUs + (System.nanoTime() - base) / 1000
                    val wait = next.timestamp - clock
                    if (wait > 0) Thread.sleep(wait / 1000, ((wait % 1000) * 1000).toInt())
                    val now = System.nanoTime()
                    if (wait > -LateFrameUs || frame == null || now - lastShown > MaxFrameGapNs) {
                        show(next)
                        lastShown = now
                    }
                    position = next.timestamp / 1000
                } else if (next.samples != null && line != null) {
                    writeAudio(next)
                }
            }
        } catch (error: Throwable) {
            AppLog.write("Video playback failed", error)
            failed = true
        } finally {
            releaseFrames()
            audioThread?.interrupt()
            audio.clear()
            runCatching { line?.close() }
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

    private fun pumpAudio(output: SourceDataLine) {
        try {
            while (running) {
                val chunk = audio.take()
                output.write(chunk, 0, chunk.size)
            }
        } catch (_: InterruptedException) {
        }
    }

    private fun writeAudio(frame: Frame) {
        if (line == null) return
        val samples = frame.samples.firstOrNull() as? ShortBuffer ?: return
        val count = samples.remaining()
        val bytes = ByteBuffer.allocate(count * 2).order(ByteOrder.LITTLE_ENDIAN)
        val start = samples.position()
        val gain = if (muted) 0f else volume
        for (i in 0 until count) bytes.putShort((samples.get(start + i) * gain).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort())
        audio.offer(bytes.array(), AudioQueueWaitMs, java.util.concurrent.TimeUnit.MILLISECONDS)
    }

    private fun show(frame: Frame) {
        val buffer = frame.image?.firstOrNull() as? ByteBuffer ?: return
        val stride = frame.imageStride
        val size = stride * frame.imageHeight
        if (pixels.size != size) pixels = ByteArray(size)
        buffer.duplicate().apply { rewind() }.get(pixels, 0, minOf(size, buffer.capacity()))
        val info = ImageInfo(frame.imageWidth, frame.imageHeight, ColorType.BGRA_8888, ColorAlphaType.OPAQUE)
        val bitmap = Bitmap()
        bitmap.allocPixels(info, stride)
        bitmap.installPixels(info, pixels, stride)
        bitmap.setImmutable()
        this.frame = bitmap.asComposeImageBitmap()
        retired.addLast(bitmap)
        if (retired.size > KeptFrames) {
            val old = retired.removeFirst()
            java.awt.EventQueue.invokeLater { old.close() }
        }
    }

    private fun releaseFrames() {
        val left = retired.toList()
        retired.clear()
        java.awt.EventQueue.invokeLater { left.dropLast(1).forEach { it.close() } }
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
    val screen = remember { GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration.bounds }
    val windowWidth = (screen.width * ViewerShare).toInt()
    val windowHeight = (screen.height * ViewerShare).toInt()
    Window(
        onCloseRequest = close,
        state = rememberWindowState(
            position = WindowPosition((screen.x + (screen.width - windowWidth) / 2).dp, (screen.y + (screen.height - windowHeight) / 2).dp),
            size = DpSize(windowWidth.dp, windowHeight.dp),
        ),
        title = "pixelGram video",
        undecorated = false,
        transparent = false,
        resizable = true,
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
            Box(Modifier.fillMaxSize().background(Color.Black)) {
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
            .background(Color.Black.copy(alpha = 0.92f))
            .onPointerEvent(PointerEventType.Move) { lastMove = System.currentTimeMillis() }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { engine?.toggle() },
        contentAlignment = Alignment.Center,
    ) {
        val frame = engine?.frame
        if (frame != null) {
            Image(frame, contentDescription = "Video", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        } else {
            ThumbImage(request.thumb, request.mini, request.telegram, Modifier.fillMaxSize(), ContentScale.Fit)
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
fun InlineVideo(path: String, modifier: Modifier = Modifier) {
    val engine = remember(path) { VideoEngine(path, loop = true, sound = false, maxWidth = InlineDecodeWidth).apply { start() } }
    DisposableEffect(engine) { onDispose { engine.close() } }
    engine.frame?.let { frame ->
        Image(frame, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier)
    }
}

private const val InlineDecodeWidth = 640
