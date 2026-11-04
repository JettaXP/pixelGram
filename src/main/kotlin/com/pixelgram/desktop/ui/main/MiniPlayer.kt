package com.pixelgram.desktop.ui.main

import androidx.compose.ui.draw.drawWithContent
import com.pixelgram.desktop.ui.components.expressive.IconToggleButton
import com.pixelgram.desktop.ui.icons.outlined.FavoriteBorder
import com.pixelgram.desktop.ui.icons.filled.Favorite
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.ui.unit.DpSize
import androidx.compose.material3.SliderDefaults
import com.pixelgram.desktop.ui.components.expressive.Slider
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.ripple
import androidx.compose.material3.LocalContentColor
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.RepeatOne
import com.pixelgram.desktop.ui.icons.OpenInFull
import com.pixelgram.desktop.ui.icons.automirrored.filled.VolumeOff
import com.pixelgram.desktop.ui.icons.automirrored.filled.VolumeUp
import com.pixelgram.desktop.ui.icons.filled.Close
import com.pixelgram.desktop.ui.icons.rounded.Pause
import com.pixelgram.desktop.ui.icons.rounded.PlayArrow
import com.pixelgram.desktop.ui.icons.filled.Repeat
import com.pixelgram.desktop.ui.icons.filled.RepeatOn
import com.pixelgram.desktop.ui.icons.rounded.SkipNext
import com.pixelgram.desktop.ui.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import com.pixelgram.desktop.ui.components.expressive.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.components.expressive.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AudioPlayer
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.telegram.UiMessage
import it.tdlight.jni.TdApi

enum class RepeatMode { Off, All, One }

class QueueTrack(val key: Long, val audio: TdApi.Audio?, val title: String, val performer: String, val message: UiMessage?)

object AudioQueue {
    var pending by mutableStateOf<UiMessage?>(null)
    var pendingTrack by mutableStateOf<TdApi.Audio?>(null)
    var playlist by mutableStateOf<List<TdApi.Audio>>(emptyList())
    var custom by mutableStateOf<List<UiMessage>?>(null)
    var playlistOwner by mutableStateOf(0L)
    var shuffle by mutableStateOf(false)
    var repeatMode by mutableStateOf(RepeatMode.Off)
        private set

    fun cycleRepeat() {
        repeatMode = RepeatMode.entries[(repeatMode.ordinal + 1) % RepeatMode.entries.size]
        AudioPlayer.repeat = repeatMode == RepeatMode.One
    }

    fun trackTitle(audio: TdApi.Audio): String =
        listOf(audio.performer, audio.title).filter { it.isNotBlank() }.joinToString(" – ").ifBlank { audio.fileName }

    fun move(telegram: Telegram, key: Long, direction: Int) {
        val index = playlist.indexOfFirst { it.audio.id.toLong() == key }
        if (index < 0) return step(telegram, key, direction)
        val next = pick(playlist, index, direction) ?: return
        pendingTrack = next
        telegram.download(next.audio)
    }

    fun file(message: UiMessage): TdApi.File? = when (val content = message.content) {
        is TdApi.MessageAudio -> content.audio.audio
        is TdApi.MessageVoiceNote -> content.voiceNote.voice
        else -> null
    }

    fun title(message: UiMessage): String = when (val content = message.content) {
        is TdApi.MessageAudio -> audioTitle(content.audio)
        else -> "Voice message"
    }

    private fun chatList(telegram: Telegram, key: Long): List<UiMessage> {
        custom?.takeIf { list -> list.any { it.key == key } }?.let { return it }
        val all = telegram.messages(AudioPlayer.chatId)
        val kind = all.firstOrNull { it.key == key }?.content?.javaClass ?: return emptyList()
        return all.filter { it.content?.javaClass == kind && file(it) != null }.reversed()
    }

    fun step(telegram: Telegram, key: Long, direction: Int) {
        val list = chatList(telegram, key)
        val index = list.indexOfFirst { it.key == key }
        if (index < 0) return
        val next = pick(list, index, direction) ?: return
        pending = next
        telegram.download(file(next))
    }

    private fun <T> pick(list: List<T>, index: Int, direction: Int): T? {
        if (shuffle && direction > 0 && list.size > 1) return list.filterIndexed { i, _ -> i != index }.random()
        list.getOrNull(index + direction)?.let { return it }
        return if (repeatMode == RepeatMode.All) list.getOrNull(if (direction > 0) 0 else list.lastIndex) else null
    }

    fun tracks(telegram: Telegram): List<QueueTrack> {
        val key = AudioPlayer.current ?: return emptyList()
        if (playlist.any { it.audio.id.toLong() == key }) {
            return playlist.map { QueueTrack(it.audio.id.toLong(), it, it.title.ifBlank { it.fileName }, it.performer, null) }
        }
        return chatList(telegram, key).map { message ->
            val audio = (message.content as? TdApi.MessageAudio)?.audio
            QueueTrack(message.key, audio, audio?.let { it.title.ifBlank { it.fileName } } ?: title(message), audio?.performer.orEmpty(), message)
        }
    }

    fun reorder(telegram: Telegram, from: Int, to: Int) {
        val key = AudioPlayer.current ?: return
        if (playlist.any { it.audio.id.toLong() == key }) {
            playlist = playlist.toMutableList().apply { add(to, removeAt(from)) }
            return
        }
        custom = chatList(telegram, key).toMutableList().apply { add(to, removeAt(from)) }
    }

    fun playAt(telegram: Telegram, track: QueueTrack) {
        val message = track.message
        if (message != null) {
            pending = message
            telegram.download(file(message))
        } else if (track.audio != null) {
            pendingTrack = track.audio
            telegram.download(track.audio.audio)
        }
    }
}

@Composable
fun AudioQueueEffects(telegram: Telegram) {
    val pending = AudioQueue.pending
    val path = pending?.let { telegram.streamPath(AudioQueue.file(it)) }
    LaunchedEffect(pending?.key, path) {
        if (pending != null && path != null) {
            AudioQueue.pending = null
            val file = AudioQueue.file(pending)
            AudioPlayer.play(pending.key, path, AudioQueue.title(pending), pending.chatId, (pending.content as? TdApi.MessageAudio)?.audio) { telegram.isDownloaded(file) }
        }
    }
    val track = AudioQueue.pendingTrack
    val trackPath = track?.let { telegram.streamPath(it.audio) }
    LaunchedEffect(track?.audio?.id, trackPath) {
        if (track != null && trackPath != null) {
            AudioQueue.pendingTrack = null
            AudioPlayer.play(track.audio.id.toLong(), trackPath, AudioQueue.trackTitle(track), 0L, track) { telegram.isDownloaded(track.audio) }
        }
    }
    DisposableEffect(telegram) {
        AudioPlayer.onFinished = { key -> AudioQueue.move(telegram, key, 1) }
        onDispose { AudioPlayer.onFinished = null }
    }
}

@Composable
fun MiniPlayer(telegram: Telegram) {
    LaunchedEffect(telegram, telegram.me?.id) { telegram.loadMyProfileAudios() }
    AnimatedVisibility(
        visible = (AudioPlayer.current != null || AudioQueue.pending != null) && !(FullPlayer.open && AudioPlayer.current != null),
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        val hover = remember { MutableInteractionSource() }
        val hovered by hover.collectIsHoveredAsState()
        val key = AudioPlayer.current
        val bar = MaterialTheme.colorScheme.surfaceContainer
        Surface(
            color = bar,
            modifier = Modifier
                .fillMaxWidth()
                .hoverable(hover)
                .drawWithContent {
                    drawContent()
                    val radius = FilletRadius.toPx()
                    val top = size.height
                    listOf(0f to radius, size.width to size.width - radius).forEach { (edge, center) ->
                        val corner = androidx.compose.ui.graphics.Path().apply {
                            addRect(androidx.compose.ui.geometry.Rect(minOf(edge, center), top, maxOf(edge, center), top + radius))
                        }
                        val round = androidx.compose.ui.graphics.Path().apply {
                            addOval(androidx.compose.ui.geometry.Rect(androidx.compose.ui.geometry.Offset(center, top + radius), radius))
                        }
                        drawPath(androidx.compose.ui.graphics.Path.combine(androidx.compose.ui.graphics.PathOperation.Difference, corner, round), bar)
                    }
                },
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp).height(BarHeight)) {
                    MorphControls(
                        playing = AudioPlayer.playing,
                        onPrevious = { key?.let { AudioQueue.move(telegram, it, -1) } },
                        onPlay = AudioPlayer::pauseOrResume,
                        onNext = { key?.let { AudioQueue.move(telegram, it, 1) } },
                    )
                    Text(
                        AudioPlayer.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp).clip(RoundedCornerShape(8.dp)).clickable { FullPlayer.open = !FullPlayer.open },
                    )
                    AnimatedVisibility(
                        visible = hovered,
                        enter = expandHorizontally(expandFrom = Alignment.End) + fadeIn(),
                        exit = shrinkHorizontally(shrinkTowards = Alignment.End) + fadeOut(),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                formatDuration(AudioPlayer.position.toInt()),
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 8.dp),
                            )
                            IconButton(onClick = { AudioPlayer.muted = !AudioPlayer.muted }) {
                                Icon(
                                    if (AudioPlayer.muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Sound",
                                )
                            }
                            Slider(
                                value = if (AudioPlayer.muted) 0f else AudioPlayer.volume,
                                onValueChange = {
                                    AudioPlayer.volume = it
                                    AudioPlayer.muted = it == 0f
                                },
                                modifier = Modifier.width(MiniVolumeWidth).padding(end = 4.dp),
                            )
                            IconButton(onClick = AudioQueue::cycleRepeat) {
                                Icon(
                                    when (AudioQueue.repeatMode) {
                                        RepeatMode.Off -> Icons.Filled.Repeat
                                        RepeatMode.All -> Icons.Filled.RepeatOn
                                        RepeatMode.One -> Icons.Rounded.RepeatOne
                                    },
                                    contentDescription = "Repeat",
                                    tint = if (AudioQueue.repeatMode != RepeatMode.Off) MaterialTheme.colorScheme.primary else androidx.compose.material3.LocalContentColor.current,
                                )
                            }
                            TextButton(onClick = AudioPlayer::nextSpeed) {
                                Text(speedLabel(AudioPlayer.speed), style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                    AudioPlayer.audio?.let { track ->
                        val saved = telegram.isInProfile(track)
                        val scale by animateFloatAsState(if (saved) 1f else 0.9f, spring(dampingRatio = 0.4f, stiffness = 600f))
                        IconToggleButton(checked = saved, onCheckedChange = { telegram.toggleProfileAudio(track) }) {
                            Icon(
                                if (saved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (saved) "Remove from profile" else "Add to profile",
                                tint = if (saved) MaterialTheme.colorScheme.primary else androidx.compose.material3.LocalContentColor.current,
                                modifier = Modifier.graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                },
                            )
                        }
                    }
                    IconButton(onClick = { FullPlayer.open = !FullPlayer.open }) {
                        val turn by animateFloatAsState(if (FullPlayer.open) 180f else 0f)
                        Icon(Icons.Rounded.OpenInFull, contentDescription = "Full player", modifier = Modifier.graphicsLayer { rotationZ = turn })
                    }
                    IconButton(onClick = {
                        AudioQueue.pending = null
                        FullPlayer.open = false
                        AudioPlayer.stop()
                    }) {
                        Icon(Icons.Filled.Close, contentDescription = "Close player")
                    }
                }
                WavySeekBar(
                    fraction = if (AudioPlayer.duration > 0f) AudioPlayer.position / AudioPlayer.duration else 0f,
                    onSeek = AudioPlayer::seekCurrent,
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                    playing = AudioPlayer.playing,
                    duration = AudioPlayer.duration,
                )
            }
        }
    }
}

private fun speedLabel(speed: Float) = when (speed) {
    1f -> "1X"
    1.5f -> "1.5X"
    else -> "2X"
}

private val SeekHeight = 8.dp
private val BarHeight = 56.dp

private enum class Control { Previous, Play, Next }

@Composable
internal fun MorphControls(
    playing: Boolean,
    onPrevious: () -> Unit,
    onPlay: () -> Unit,
    onNext: () -> Unit,
    scale: Float = ControlScale,
    gap: androidx.compose.ui.unit.Dp = ControlGap,
    iconSize: androidx.compose.ui.unit.Dp = 22.dp,
    playIcon: androidx.compose.ui.unit.Dp = 26.dp,
    wide: Boolean = false,
    onHold: ((Int) -> Unit)? = null,
) {
    val sources = remember { Control.entries.associateWith { MutableInteractionSource() } }
    val pressed = Control.entries.firstOrNull { sources.getValue(it).collectIsPressedAsState().value }
    var held by remember { mutableStateOf(0) }
    val holding = when (pressed) {
        Control.Previous -> -1
        Control.Next -> 1
        else -> 0
    }
    LaunchedEffect(holding) {
        if (onHold == null) return@LaunchedEffect
        if (holding != 0) {
            kotlinx.coroutines.delay(HoldDelayMs)
            held = holding
            onHold(holding)
        } else if (held != 0) {
            onHold(0)
        }
    }
    val release: (() -> Unit) -> () -> Unit = { action ->
        {
            if (held != 0) {
                held = 0
                onHold?.invoke(0)
            } else {
                action()
            }
        }
    }
    val sideWidth = { me: Control ->
        when (pressed) {
            me -> 76f
            Control.Play -> 58f
            else -> 64f
        }
    }
    val playWidth = when (pressed) {
        Control.Play -> 100f
        Control.Previous, Control.Next -> 80f
        else -> 88f
    } + if (wide) 44f else 0f
    val scheme = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) {
        MorphButton(
            width = sideWidth(Control.Previous),
            height = 64f,
            radius = if (pressed == Control.Previous) 18f else 32f,
            colors = scheme.secondaryContainer to scheme.onSecondaryContainer,
            source = sources.getValue(Control.Previous),
            onClick = release(onPrevious),
            scale = scale,
        ) { Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(iconSize)) }
        MorphButton(
            width = playWidth,
            height = 88f,
            radius = when {
                pressed == Control.Play -> 20f
                playing -> 28f
                else -> 44f
            },
            colors = scheme.primary to scheme.onPrimary,
            source = sources.getValue(Control.Play),
            onClick = onPlay,
            scale = scale,
        ) { PlayPauseMorph(playing, playIcon) }
        MorphButton(
            width = sideWidth(Control.Next),
            height = 64f,
            radius = if (pressed == Control.Next) 18f else 32f,
            colors = scheme.secondaryContainer to scheme.onSecondaryContainer,
            source = sources.getValue(Control.Next),
            onClick = release(onNext),
            scale = scale,
        ) { Icon(Icons.Rounded.SkipNext, contentDescription = "Next", modifier = Modifier.size(iconSize)) }
    }
}

@Composable
private fun MorphButton(
    width: Float,
    height: Float,
    radius: Float,
    colors: Pair<Color, Color>,
    source: MutableInteractionSource,
    onClick: () -> Unit,
    scale: Float = ControlScale,
    content: @Composable () -> Unit,
) {
    val shownWidth by animateFloatAsState(width, spring(dampingRatio = 1f, stiffness = 700f))
    val shownRadius by animateFloatAsState(radius, spring(dampingRatio = 0.55f, stiffness = 520f))
    Box(
        Modifier
            .size((shownWidth * scale).dp, (height * scale).dp)
            .clip(RoundedCornerShape((shownRadius * scale).coerceAtLeast(0f).dp))
            .background(colors.first)
            .clickable(interactionSource = source, indication = ripple(), onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.second) { content() }
    }
}

@Composable
private fun PlayPauseIcon(playing: Boolean) = PlayPauseMorph(playing, 26.dp)

@Composable
fun PlayPauseMorph(playing: Boolean, size: androidx.compose.ui.unit.Dp, tint: Color = LocalContentColor.current) {
    val alt by animateFloatAsState(if (playing) 1f else 0f, spring(dampingRatio = 0.7f, stiffness = 480f))
    Box(contentAlignment = Alignment.Center) {
        Icon(
            Icons.Rounded.PlayArrow,
            contentDescription = if (playing) null else "Play",
            tint = tint,
            modifier = Modifier.size(size).graphicsLayer {
                alpha = (1f - alt * 1.6f).coerceIn(0f, 1f)
                rotationZ = MorphTurn * alt
                scaleX = 1f - 0.3f * alt
                scaleY = 1f - 0.3f * alt
            },
        )
        Icon(
            Icons.Rounded.Pause,
            contentDescription = if (playing) "Pause" else null,
            tint = tint,
            modifier = Modifier.size(size).graphicsLayer {
                alpha = ((alt - 0.35f) * 1.6f).coerceIn(0f, 1f)
                rotationZ = -MorphTurn * (1f - alt)
                scaleX = 0.7f + 0.3f * alt
                scaleY = 0.7f + 0.3f * alt
            },
        )
    }
}

private const val MorphTurn = 45f

private const val ControlScale = 0.5f
private const val HoldDelayMs = 400L
private val ControlGap = 4.dp

private const val WaveSettleMs = 420
private val MiniVolumeWidth = 96.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WavySeekBar(fraction: Float, onSeek: (Float) -> Unit, modifier: Modifier = Modifier, playing: Boolean = true, duration: Float = 0f) {
    var drag by remember { mutableStateOf<Float?>(null) }
    val amplitude = remember { androidx.compose.animation.core.Animatable(if (playing) 1f else 0f) }
    var phase by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    val active = playing && drag == null
    LaunchedEffect(active) {
        amplitude.animateTo(if (active) 1f else 0f, androidx.compose.animation.core.tween(WaveSettleMs, easing = androidx.compose.animation.core.FastOutSlowInEasing))
    }
    LaunchedEffect(active) {
        var last = 0L
        while (active || amplitude.value > 0.001f) {
            androidx.compose.runtime.withFrameNanos { now ->
                if (last != 0L) phase += (now - last) / 1_000_000_000f * WaveCyclesPerSecond * amplitude.value
                last = now
            }
        }
    }
    val colors = MaterialTheme.colorScheme
    val shown = (drag ?: fraction).coerceIn(0f, 1f)
    Box(modifier.height(28.dp)) {
        androidx.compose.foundation.Canvas(
            Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitPointerEvent()
                            val first = down.changes.firstOrNull { it.pressed } ?: continue
                            var at = (first.position.x / size.width).coerceIn(0f, 1f)
                            drag = at
                            first.consume()
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                at = (change.position.x / size.width).coerceIn(0f, 1f)
                                drag = at
                                change.consume()
                                if (!change.pressed) break
                            }
                            onSeek(at)
                            drag = null
                        }
                    }
                },
        ) {
            val mid = size.height / 2f
            val stroke = 4.dp.toPx()
            val gap = 5.dp.toPx()
            val x = size.width * shown
            val amp = 3.dp.toPx() * amplitude.value
            val wavelength = 30.dp.toPx()
            val end = (x - gap).coerceAtLeast(0f)
            if (end > stroke / 2) {
                val path = androidx.compose.ui.graphics.Path()
                var px = stroke / 2
                path.moveTo(px, mid + amp * kotlin.math.sin((px / wavelength - phase) * 2f * kotlin.math.PI.toFloat()))
                while (px < end) {
                    px = (px + 1f).coerceAtMost(end)
                    path.lineTo(px, mid + amp * kotlin.math.sin((px / wavelength - phase) * 2f * kotlin.math.PI.toFloat()))
                }
                drawPath(path, colors.primary, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = StrokeCap.Round))
            }
            val rest = x + gap
            if (rest < size.width - stroke / 2) {
                drawLine(colors.secondaryContainer, Offset(rest, mid), Offset(size.width - stroke / 2, mid), stroke, StrokeCap.Round)
            }
            drawCircle(colors.primary, stroke / 2, Offset(size.width - stroke / 2, mid))
            val thumb = if (drag != null) 6.dp.toPx() else 4.dp.toPx()
            drawRoundRect(
                colors.primary,
                topLeft = Offset(x - thumb / 2, mid - 11.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(thumb, 22.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(thumb / 2),
            )
        }
        val label = drag
        if (label != null && duration > 0f) {
            androidx.compose.ui.layout.Layout(
                content = {
                    Surface(shape = CircleShape, color = colors.inverseSurface, contentColor = colors.inverseOnSurface, shadowElevation = 4.dp) {
                        Text(formatDuration((label * duration).toInt()), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                },
                modifier = Modifier.matchParentSize().zIndex(1f),
            ) { measurables, constraints ->
                val bubble = measurables.first().measure(constraints.copy(minWidth = 0, minHeight = 0))
                layout(constraints.maxWidth, constraints.maxHeight) {
                    val x = (constraints.maxWidth * label - bubble.width / 2f).toInt().coerceIn(0, (constraints.maxWidth - bubble.width).coerceAtLeast(0))
                    bubble.place(x, -bubble.height - 6.dp.roundToPx())
                }
            }
        }
    }
}

private val FilletRadius = 24.dp
private const val WaveCyclesPerSecond = 0.45f
