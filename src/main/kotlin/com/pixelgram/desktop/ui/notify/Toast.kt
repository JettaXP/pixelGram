package com.pixelgram.desktop.ui.notify

import com.pixelgram.desktop.ui.themedAppIcon
import androidx.compose.foundation.Image
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.filled.ClearAll
import com.pixelgram.desktop.ui.icons.filled.Close
import com.pixelgram.desktop.ui.components.expressive.FilledTonalButton
import androidx.compose.material3.Icon
import com.pixelgram.desktop.ui.components.expressive.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.pixelgram.desktop.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.core.ToastSide
import com.pixelgram.desktop.telegram.ChatState
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.components.ChatAvatar
import com.pixelgram.desktop.ui.theme.EmojiText
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.delay

data class ToastColors(val bg: Color, val fg: Color, val accent: Color, val onAccent: Color, val band: Color)

@Composable
fun toastColors(): ToastColors {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.surface.luminance() < 0.5f
    val bg = scheme.primaryContainer
    val band = if (dark) lerp(bg, Color.Black, 0.3f) else lerp(bg, scheme.primary, 0.25f)
    return ToastColors(bg, scheme.onPrimaryContainer, scheme.primary, scheme.onPrimary, band)
}

const val ToastDurationMs = 6000

val ToastCardWidth = 368.dp
val ToastCardHeight = 118.dp

val ToastWindowWidth = 400.dp

fun toastWindowHeight(count: Int) = (ToastCardHeight + 8.dp) * count + 76.dp

private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
private val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
private val PopEasing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

private fun enterFor(side: ToastSide): EnterTransition {
    val spec = tween<IntOffset>(550, easing = EmphasizedDecelerate)
    return fadeIn(tween(150)) + when (side.column) {
        0 -> slideInHorizontally(spec) { -it - 32 }
        2 -> slideInHorizontally(spec) { it + 32 }
        else -> if (side.top) slideInVertically(spec) { -it - 32 } else slideInVertically(spec) { it + 32 }
    }
}

private fun exitFor(side: ToastSide): ExitTransition {
    val slide = tween<IntOffset>(ExitMs, easing = FastOutSlowInEasing)
    val fade = tween<Float>(ExitMs, easing = LinearEasing)
    val shrink = tween<IntSize>(ExitMs, delayMillis = 150, easing = FastOutSlowInEasing)
    return fadeOut(fade) + shrinkVertically(shrink) + when (side.column) {
        0 -> slideOutHorizontally(slide) { -it - 32 }
        2 -> slideOutHorizontally(slide) { it + 32 }
        else -> if (side.top) slideOutVertically(slide) { -it - 32 } else slideOutVertically(slide) { it + 32 }
    }
}

private const val ExitMs = 450

class ToastStack {
    class Entry(val data: ToastData) {
        val visible = MutableTransitionState(false).apply { targetState = true }
    }

    val entries = mutableStateListOf<Entry>()

    var max = 3

    val shown: List<Entry> get() = entries.filter { it.visible.targetState }

    fun push(data: ToastData) {
        val showing = shown
        showing.dropLast((max - 1).coerceAtLeast(0)).forEach { it.visible.targetState = false }
        entries.add(Entry(data))
    }

    fun dismiss(entry: Entry) {
        entry.visible.targetState = false
    }

    fun dismissAll() = entries.forEach(::dismiss)

    internal fun removeGone() {
        entries.removeAll { it.visible.isIdle && !it.visible.currentState && !it.visible.targetState }
    }
}


@Composable
fun ToastStackContent(stack: ToastStack, settings: AppSettings, onOpen: (ToastData) -> Unit) {
    val side = settings.notificationSide.value
    val newestFirst = side.top
    val entries = if (newestFirst) stack.entries.reversed() else stack.entries.toList()
    val closeAll: @Composable () -> Unit = {
        AnimatedVisibility(
            visible = stack.shown.size >= stack.max && stack.max > 1,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            FilledTonalButton(onClick = stack::dismissAll) {
                Icon(Icons.Filled.ClearAll, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Close all", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
    Box(
        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = if (newestFirst) Alignment.TopCenter else Alignment.BottomCenter,
    ) {
        Column(horizontalAlignment = when (side.column) {
            0 -> Alignment.Start
            2 -> Alignment.End
            else -> Alignment.CenterHorizontally
        }) {
            if (!newestFirst) closeAll()
            entries.forEach { entry ->
                key(entry.data.id) {
                    LaunchedEffect(Unit) {
                        delay(ToastDurationMs.toLong())
                        stack.dismiss(entry)
                    }
                    LaunchedEffect(entry.visible.isIdle, entry.visible.currentState) {
                        stack.removeGone()
                    }
                    AnimatedVisibility(entry.visible, enter = enterFor(side), exit = exitFor(side)) {
                        Box(Modifier.padding(vertical = 4.dp)) {
                            ToastCard(
                                entry.data,
                                settings,
                                onClick = {
                                    stack.dismiss(entry)
                                    onOpen(entry.data)
                                },
                                onClose = { stack.dismiss(entry) },
                            )
                        }
                    }
                }
            }
            if (newestFirst) closeAll()
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
    val appIcon: Boolean = false,
)

@Composable
private fun ToastCard(toast: ToastData, settings: AppSettings, onClick: () -> Unit, onClose: () -> Unit) {
    val colors = toastColors()
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        color = colors.bg,
        contentColor = colors.fg,
        shadowElevation = 6.dp,
        modifier = Modifier.size(ToastCardWidth, ToastCardHeight),
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 18.dp, end = 40.dp, top = 14.dp, bottom = WaveZone + 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val pop = remember(toast.id) { Animatable(0.6f) }
                LaunchedEffect(toast.id) {
                    delay(120)
                    pop.animateTo(1f, tween(600, easing = PopEasing))
                }
                Box(Modifier.size(52.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value }) {
                    val scheme = MaterialTheme.colorScheme
                    MaterialTheme(
                        colorScheme = scheme.copy(primaryContainer = colors.accent, onPrimaryContainer = colors.onAccent),
                    ) {
                        val icon = if (toast.appIcon) themedAppIcon() else null
                        if (icon != null) {
                            Image(icon, contentDescription = null, modifier = Modifier.size(52.dp))
                        } else {
                            ChatAvatar(toast.chat, toast.telegram, settings, size = 52.dp)
                        }
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
                    if (toast.text.isNotEmpty()) {
                        EmojiText(
                            toast.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.fg.copy(alpha = 0.88f),
                            maxLines = if (toast.sender == null) 2 else 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            WaveTimer(colors, toast.id, Modifier.align(Alignment.BottomCenter))
            IconButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(32.dp),
                colors = IconButtonDefaults.iconButtonColors(contentColor = colors.fg),
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
            }
        }
    }
}

private val WaveZone = 30.dp

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
