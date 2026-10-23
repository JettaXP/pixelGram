package com.pixelgram.desktop.ui.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.pixelgram.desktop.core.VoiceNote
import com.pixelgram.desktop.core.VoiceRecorder
import com.pixelgram.desktop.ui.OnEscape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

private const val SampleEveryMs = 50L
private const val LevelSmoothing = 0.3f
private const val CollapseMs = 450
private const val MinBar = 0.12f
private const val PressedScale = 1.35f
private const val PulseScale = 0.8f
private const val PulseAlpha = 0.3f
private const val MaxLevels = 400
private val BarWidth = 3.dp
private val BarGap = 2.dp
private val PillWidth = 180.dp
private val CancelDistance = 120.dp
private val CollapseLift = 48.dp

@Stable
class VoiceRecording {
    var recorder by mutableStateOf<VoiceRecorder?>(null)
        private set
    var active by mutableStateOf(false)
        private set
    var collapsing by mutableStateOf(false)
        private set
    var cancelled by mutableStateOf(false)
        private set
    var dragX by mutableFloatStateOf(0f)
    var level by mutableFloatStateOf(0f)
        private set
    var elapsedMs by mutableLongStateOf(0L)
        private set
    val levels = mutableStateListOf<Float>()

    fun start() {
        if (active) return
        levels.clear()
        level = 0f
        elapsedMs = 0
        dragX = 0f
        cancelled = false
        collapsing = false
        active = true
        recorder = VoiceRecorder.start { error ->
            javax.swing.SwingUtilities.invokeLater {
                TopNotice.text = error
                cancel()
            }
        }
    }

    fun cancel() {
        recorder?.cancel()
        recorder = null
        cancelled = true
        active = false
    }

    suspend fun finish(): VoiceNote? = coroutineScope {
        val current = recorder ?: return@coroutineScope null
        collapsing = true
        val note = async(Dispatchers.IO) { current.stop() }
        delay(CollapseMs.toLong())
        val result = note.await()
        recorder = null
        active = false
        collapsing = false
        result
    }

    suspend fun sample() {
        var last = 0L
        while (true) {
            val now = withFrameNanos { it } / 1_000_000
            val current = recorder ?: return
            if (collapsing) return
            level += (current.level - level) * LevelSmoothing
            elapsedMs = current.elapsedMs
            if (now - last >= SampleEveryMs) {
                last = now
                levels += level
                if (levels.size > MaxLevels) levels.removeAt(0)
            }
        }
    }
}

@Composable
fun MicButton(voice: VoiceRecording, onRelease: (cancel: Boolean) -> Unit, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val cancelPx = with(density) { CancelDistance.toPx() }
    val recording = voice.active && !voice.collapsing
    val scale by animateFloatAsState(if (recording) PressedScale else 1f, MaterialTheme.motionScheme.fastSpatialSpec())
    val pulse by animateFloatAsState(if (recording) 1f + voice.level * PulseScale else 1f, MaterialTheme.motionScheme.fastSpatialSpec())
    val scheme = MaterialTheme.colorScheme
    val micShape = MaterialShapes.Cookie4Sided.toShape()
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(40.dp)
            .pointerInput(voice) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    if (!currentEvent.buttons.isPrimaryPressed) return@awaitEachGesture
                    down.consume()
                    voice.start()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        voice.dragX = (change.position.x - down.position.x).coerceAtMost(0f)
                        change.consume()
                        if (!change.pressed) break
                    }
                    if (voice.active) onRelease(voice.dragX < -cancelPx)
                    voice.dragX = 0f
                }
            },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale * pulse
                    scaleY = scale * pulse
                    alpha = if (recording) PulseAlpha else 0f
                }
                .background(scheme.primary, micShape),
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .background(scheme.primary, micShape),
        ) {
            Icon(Icons.Filled.Mic, contentDescription = "Record voice message", tint = scheme.onPrimary, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
fun RecordingBar(voice: VoiceRecording, modifier: Modifier = Modifier) {
    val collapse = remember { Animatable(0f) }
    val spec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    LaunchedEffect(voice.recorder) { voice.sample() }
    LaunchedEffect(voice.collapsing) {
        if (voice.collapsing) collapse.animateTo(1f, tween(CollapseMs)) else collapse.snapTo(0f)
    }
    OnEscape {
        voice.cancel()
        true
    }
    val scheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val c = collapse.value
    val chrome = (1f - c * 2f).coerceIn(0f, 1f)
    val vanish = ((c - 0.65f) / 0.35f).coerceIn(0f, 1f)
    BoxWithConstraints(modifier.background(scheme.surface.copy(alpha = 1f - c))) {
        val width = lerp(maxWidth, PillWidth, c)
        val barColor = lerp(scheme.surfaceContainerHigh, scheme.primaryContainer, c)
        val waveColor = lerp(scheme.primary, scheme.onPrimaryContainer, c)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(width)
                .fillMaxHeight()
                .graphicsLayer {
                    translationY = -with(density) { CollapseLift.toPx() } * c
                    alpha = 1f - vanish
                }
                .clip(RoundedCornerShape(28.dp))
                .background(barColor)
                .padding(horizontal = 16.dp),
        ) {
            if (chrome > 0f) {
                val blink by animateFloatAsState(if ((voice.elapsedMs / 500) % 2 == 0L) 1f else 0.3f, tween(250))
                Box(
                    Modifier
                        .size(10.dp)
                        .graphicsLayer { alpha = blink * chrome }
                        .background(scheme.error, CircleShape),
                )
                Text(
                    formatElapsed(voice.elapsedMs),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.graphicsLayer { alpha = chrome },
                )
            }
            Waveform(voice, waveColor, c, Modifier.weight(1f).height(32.dp))
            if (chrome > 0f) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.graphicsLayer {
                        alpha = chrome * (1f - (-voice.dragX / with(density) { CancelDistance.toPx() }).coerceIn(0f, 0.7f))
                        translationX = voice.dragX / 2
                    },
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, tint = scheme.onSurfaceVariant)
                    Text("Slide to cancel", style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun Waveform(voice: VoiceRecording, color: androidx.compose.ui.graphics.Color, collapse: Float, modifier: Modifier) {
    Canvas(modifier) {
        val bar = BarWidth.toPx()
        val step = bar + BarGap.toPx()
        val levels = voice.levels
        val all = levels.size + 1
        val fits = (size.width / step).toInt().coerceAtLeast(1)
        val shown = if (collapse > 0f) all else minOf(all, fits)
        val spacing = if (collapse > 0f) size.width / shown.coerceAtLeast(1) else step
        val first = all - shown
        for (i in 0 until shown) {
            val index = first + i
            val value = if (index < levels.size) levels[index] else voice.level
            val height = size.height * (MinBar + (1f - MinBar) * value.coerceIn(0f, 1f))
            val x = if (collapse > 0f) i * spacing else size.width - (shown - i) * step
            val w = if (collapse > 0f) (spacing * 0.6f).coerceAtMost(bar) else bar
            drawRoundRect(
                color = color,
                topLeft = Offset(x, (size.height - height) / 2),
                size = Size(w.coerceAtLeast(1f), height),
                cornerRadius = CornerRadius(w / 2),
            )
        }
    }
}

private fun formatElapsed(ms: Long): String {
    val seconds = ms / 1000
    val tenths = (ms % 1000) / 100
    return "%d:%02d,%d".format(seconds / 60, seconds % 60, tenths)
}

