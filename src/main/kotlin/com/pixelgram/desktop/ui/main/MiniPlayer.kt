package com.pixelgram.desktop.ui.main

import androidx.compose.material3.IconToggleButton
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
import androidx.compose.material3.Text
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

object AudioQueue {
    var pending by mutableStateOf<UiMessage?>(null)
    var pendingTrack by mutableStateOf<TdApi.Audio?>(null)
    var playlist: List<TdApi.Audio> = emptyList()

    fun trackTitle(audio: TdApi.Audio): String =
        listOf(audio.performer, audio.title).filter { it.isNotBlank() }.joinToString(" – ").ifBlank { audio.fileName }

    fun move(telegram: Telegram, key: Long, direction: Int) {
        val index = playlist.indexOfFirst { it.audio.id.toLong() == key }
        if (index < 0) return step(telegram, key, direction)
        val next = playlist.getOrNull(index + direction) ?: return
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

    fun step(telegram: Telegram, key: Long, direction: Int) {
        val all = telegram.messages(AudioPlayer.chatId)
        val kind = all.firstOrNull { it.key == key }?.content?.javaClass ?: return
        val list = all.filter { it.content?.javaClass == kind && file(it) != null }
        val index = list.indexOfFirst { it.key == key }
        if (index < 0) return
        val next = list.getOrNull(index - direction) ?: return
        pending = next
        telegram.download(file(next))
    }
}

@Composable
fun AudioQueueEffects(telegram: Telegram) {
    val pending = AudioQueue.pending
    val path = pending?.let { telegram.localPath(AudioQueue.file(it)) }
    LaunchedEffect(pending?.key, path) {
        if (pending != null && path != null) {
            AudioQueue.pending = null
            AudioPlayer.play(pending.key, path, AudioQueue.title(pending), pending.chatId, (pending.content as? TdApi.MessageAudio)?.audio)
        }
    }
    val track = AudioQueue.pendingTrack
    val trackPath = track?.let { telegram.localPath(it.audio) }
    LaunchedEffect(track?.audio?.id, trackPath) {
        if (track != null && trackPath != null) {
            AudioQueue.pendingTrack = null
            AudioPlayer.play(track.audio.id.toLong(), trackPath, AudioQueue.trackTitle(track), 0L, track)
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
        visible = AudioPlayer.current != null || AudioQueue.pending != null,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        val hover = remember { MutableInteractionSource() }
        val hovered by hover.collectIsHoveredAsState()
        val key = AudioPlayer.current
        Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth().hoverable(hover)) {
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
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
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
                            IconButton(onClick = { AudioPlayer.repeat = !AudioPlayer.repeat }) {
                                Icon(
                                    if (AudioPlayer.repeat) Icons.Filled.RepeatOn else Icons.Filled.Repeat,
                                    contentDescription = "Repeat",
                                    tint = if (AudioPlayer.repeat) MaterialTheme.colorScheme.primary else androidx.compose.material3.LocalContentColor.current,
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
                    IconButton(onClick = {
                        AudioQueue.pending = null
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
private fun MorphControls(playing: Boolean, onPrevious: () -> Unit, onPlay: () -> Unit, onNext: () -> Unit) {
    val sources = remember { Control.entries.associateWith { MutableInteractionSource() } }
    val pressed = Control.entries.firstOrNull { sources.getValue(it).collectIsPressedAsState().value }
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
    }
    val scheme = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(ControlGap), verticalAlignment = Alignment.CenterVertically) {
        MorphButton(
            width = sideWidth(Control.Previous),
            height = 64f,
            radius = if (pressed == Control.Previous) 18f else 32f,
            colors = scheme.secondaryContainer to scheme.onSecondaryContainer,
            source = sources.getValue(Control.Previous),
            onClick = onPrevious,
        ) { Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(22.dp)) }
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
        ) { PlayPauseIcon(playing) }
        MorphButton(
            width = sideWidth(Control.Next),
            height = 64f,
            radius = if (pressed == Control.Next) 18f else 32f,
            colors = scheme.secondaryContainer to scheme.onSecondaryContainer,
            source = sources.getValue(Control.Next),
            onClick = onNext,
        ) { Icon(Icons.Rounded.SkipNext, contentDescription = "Next", modifier = Modifier.size(22.dp)) }
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
    content: @Composable () -> Unit,
) {
    val shownWidth by animateFloatAsState(width, spring(dampingRatio = 1f, stiffness = 700f))
    val shownRadius by animateFloatAsState(radius, spring(dampingRatio = 0.55f, stiffness = 520f))
    Box(
        Modifier
            .size((shownWidth * ControlScale).dp, (height * ControlScale).dp)
            .clip(RoundedCornerShape((shownRadius * ControlScale).coerceAtLeast(0f).dp))
            .background(colors.first)
            .clickable(interactionSource = source, indication = ripple(), onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.second) { content() }
    }
}

@Composable
private fun PlayPauseIcon(playing: Boolean) {
    val alt by animateFloatAsState(if (playing) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = 520f))
    Box(contentAlignment = Alignment.Center) {
        Icon(
            Icons.Rounded.PlayArrow,
            contentDescription = if (playing) null else "Play",
            modifier = Modifier.size(26.dp).graphicsLayer {
                alpha = (1f - alt).coerceIn(0f, 1f)
                scaleX = 1f - 0.4f * alt
                scaleY = 1f - 0.4f * alt
            },
        )
        Icon(
            Icons.Rounded.Pause,
            contentDescription = if (playing) "Pause" else null,
            modifier = Modifier.size(26.dp).graphicsLayer {
                alpha = alt.coerceIn(0f, 1f)
                scaleX = 0.6f + 0.4f * alt
                scaleY = 0.6f + 0.4f * alt
            },
        )
    }
}

private const val ControlScale = 0.5f
private val ControlGap = 4.dp

private const val WaveSettleStiffness = 16.5f
private val MiniVolumeWidth = 96.dp
private const val WaveRestThreshold = 0.01f

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WavySeekBar(fraction: Float, onSeek: (Float) -> Unit, modifier: Modifier = Modifier, playing: Boolean = true) {
    var dragging by remember { mutableStateOf<Float?>(null) }
    val wave by animateFloatAsState(if (playing) 1f else 0f, spring(dampingRatio = 1f, stiffness = WaveSettleStiffness))
    val interaction = remember { MutableInteractionSource() }
    androidx.compose.material3.Slider(
        value = dragging ?: fraction.coerceIn(0f, 1f),
        onValueChange = { dragging = it },
        onValueChangeFinished = {
            dragging?.let(onSeek)
            dragging = null
        },
        interactionSource = interaction,
        thumb = { com.pixelgram.desktop.ui.components.expressive.SliderThumb(interaction, height = 22.dp) },
        track = { state ->
            LinearWavyProgressIndicator(
                progress = { state.value },
                modifier = Modifier.fillMaxWidth(),
                amplitude = { progress -> WavyProgressIndicatorDefaults.indicatorAmplitude(progress) * wave },
                waveSpeed = if (wave > WaveRestThreshold) WavyProgressIndicatorDefaults.LinearDeterminateWavelength else 0.dp,
            )
        },
        modifier = modifier.height(28.dp),
    )
}
