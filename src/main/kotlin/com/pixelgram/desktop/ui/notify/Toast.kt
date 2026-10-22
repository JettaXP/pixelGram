package com.pixelgram.desktop.ui.notify

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.core.ToastSide
import com.pixelgram.desktop.telegram.ChatState
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.components.ChatAvatar
import com.pixelgram.desktop.ui.theme.EmojiText
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

// Material 3 notification card with a wavy timer, ported from the
// Alert Notifier tutorial; colors come from the app's color scheme.

data class ToastColors(val bg: Color, val fg: Color, val accent: Color, val onAccent: Color, val band: Color)

@Composable
fun toastColors(): ToastColors {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.surface.luminance() < 0.5f
    val bg = scheme.primaryContainer
    // The band under the wave: the container about 30% darker.
    val band = if (dark) lerp(bg, Color.Black, 0.3f) else lerp(bg, scheme.primary, 0.25f)
    return ToastColors(bg, scheme.onPrimaryContainer, scheme.primary, scheme.onPrimary, band)
}

const val ToastDurationMs = 6000

// Window around the card: room for the shadow and for sliding in.
val ToastWindowWidth = 400.dp
val ToastWindowHeight = 150.dp

private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
private val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
private val PopEasing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

private fun enterFor(side: ToastSide): EnterTransition {
    val spec = tween<IntOffset>(550, easing = EmphasizedDecelerate)
    return fadeIn(tween(150)) + when (side) {
        ToastSide.Left -> slideInHorizontally(spec) { -it - 32 }
        ToastSide.Right -> slideInHorizontally(spec) { it + 32 }
        ToastSide.Top -> slideInVertically(spec) { -it - 32 }
        ToastSide.Bottom -> slideInVertically(spec) { it + 32 }
    }
}

private fun exitFor(side: ToastSide): ExitTransition {
    val spec = tween<IntOffset>(220, easing = EmphasizedAccelerate)
    return fadeOut(tween(180)) + when (side) {
        ToastSide.Left -> slideOutHorizontally(spec) { -it - 32 }
        ToastSide.Right -> slideOutHorizontally(spec) { it + 32 }
        ToastSide.Top -> slideOutVertically(spec) { -it - 32 }
        ToastSide.Bottom -> slideOutVertically(spec) { it + 32 }
    }
}

// One notification: slides in, waits for the timer, slides out, then onGone.
@Composable
fun Toast(
    toast: ToastData,
    settings: AppSettings,
    onOpen: () -> Unit,
    onGone: () -> Unit,
) {
    val side = settings.notificationSide.value
    val visible = remember(toast.id) { MutableTransitionState(false).apply { targetState = true } }
    LaunchedEffect(toast.id) {
        delay(ToastDurationMs.toLong())
        visible.targetState = false
    }
    LaunchedEffect(visible.isIdle, visible.currentState) {
        if (visible.isIdle && !visible.currentState) onGone()
    }
    Box(Modifier.fillMaxSize().padding(16.dp)) {
        AnimatedVisibility(visible, enter = enterFor(side), exit = exitFor(side)) {
            ToastCard(toast, settings) {
                visible.targetState = false
                onOpen()
            }
        }
    }
}

data class ToastData(
    val id: Long,
    val telegram: Telegram,
    val chat: ChatState,
    val title: String,
    val sender: String?,
    val text: String,
)

@Composable
private fun ToastCard(toast: ToastData, settings: AppSettings, onClick: () -> Unit) {
    val colors = toastColors()
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = colors.bg,
        contentColor = colors.fg,
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxSize(),
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 18.dp, end = 22.dp, top = 14.dp, bottom = WaveZone + 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // The tutorial's icon circle, here the chat avatar with the same "pop".
                val pop = remember(toast.id) { Animatable(0.6f) }
                LaunchedEffect(toast.id) {
                    delay(120)
                    pop.animateTo(1f, tween(600, easing = PopEasing))
                }
                Box(Modifier.size(52.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value }) {
                    // Avatars without a photo use the accent circle, as the tutorial's icon does.
                    val scheme = MaterialTheme.colorScheme
                    MaterialTheme(
                        colorScheme = scheme.copy(primaryContainer = colors.accent, onPrimaryContainer = colors.onAccent),
                    ) {
                        ChatAvatar(toast.chat, toast.telegram, settings, size = 52.dp)
                    }
                }
                Column(Modifier.weight(1f)) {
                    EmojiText(
                        toast.title,
                        fontSize = 17.sp,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.fg,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    toast.sender?.let {
                        EmojiText(
                            it,
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.accent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    EmojiText(
                        toast.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.fg.copy(alpha = 0.88f),
                        maxLines = if (toast.sender == null) 2 else 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            WaveTimer(colors, toast.id, Modifier.align(Alignment.BottomCenter))
        }
    }
}

private val WaveZone = 30.dp

// MD3 wavy progress: the active wave shrinks over the toast's lifetime, a
// faint track and a stop dot follow it, and a wavy band fills the bottom;
// all share one phase so they move together.
@Composable
private fun WaveTimer(colors: ToastColors, key: Long, modifier: Modifier = Modifier) {
    val left = remember(key) { Animatable(1f) }
    LaunchedEffect(key) { left.animateTo(0f, tween(ToastDurationMs, easing = LinearEasing)) }
    val phase by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween((WaveLength / WaveSpeed * 1000).toInt(), easing = LinearEasing)),
    )
    Canvas(modifier.fillMaxWidth().height(WaveZone)) {
        val amp = 3.2.dp.toPx()
        val len = WaveLength.dp.toPx()
        val y1 = 10.dp.toPx()
        val y2 = 16.dp.toPx()
        val edge = 12.dp.toPx()
        val gap = 8.dp.toPx()
        val step = 2.dp.toPx()
        val shift = -phase * len

        fun wy(x: Float, y: Float) = y + amp * sin((x + shift) / len * 2 * PI.toFloat())
        fun wave(x0: Float, x1: Float, y: Float) = Path().apply {
            if (x1 - x0 < 1f) return@apply
            moveTo(x0, wy(x0, y))
            var x = x0 + step
            while (x < x1) {
                lineTo(x, wy(x, y))
                x += step
            }
            lineTo(x1, wy(x1, y))
        }

        drawPath(
            wave(0f, size.width, y2).apply {
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            },
            colors.band,
        )
        val stroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val stopX = size.width - edge
        val end = edge + (stopX - gap - edge) * left.value
        drawPath(wave(if (left.value > 0f) end + gap else edge, stopX - gap, y1), colors.accent.copy(alpha = 0.3f), style = stroke)
        if (left.value > 0f) drawPath(wave(edge, end, y1), colors.accent, style = stroke)
        drawCircle(colors.accent, radius = 2.5.dp.toPx(), center = Offset(stopX, y1))
    }
}

private const val WaveLength = 26f
private const val WaveSpeed = 24f
