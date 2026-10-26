package com.pixelgram.desktop.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import com.pixelgram.desktop.ui.theme.Emoji3DImage
import com.pixelgram.desktop.ui.theme.EmojiFonts
import com.pixelgram.desktop.ui.theme.HiddenEmojiTransformation
import com.pixelgram.desktop.ui.theme.emojiRanges
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AppSettings
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sqrt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberUpdatedState

@Composable
fun ComposerField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    placeholder: String,
    visualTransformation: VisualTransformation,
    settings: AppSettings,
    shape: Shape,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var focused by remember { mutableStateOf(false) }
    val requester = remember { BringIntoViewRequester() }
    val colors = MaterialTheme.colorScheme
    val style = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface)
    val liquid = settings.liquidCursor.value
    val images = EmojiFonts.use3D
    val transformation = remember(visualTransformation, images) {
        if (images) HiddenEmojiTransformation(visualTransformation) else visualTransformation
    }
    LaunchedEffect(value.selection.end, layout) {
        val current = layout ?: return@LaunchedEffect
        val end = value.selection.end.coerceIn(0, current.layoutInput.text.length)
        requester.bringIntoView(current.getCursorRect(end))
    }
    Surface(shape = shape, color = color, modifier = Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = FieldMinHeight, max = FieldMaxHeight)
                .verticalScroll(scroll)
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            if (value.text.isEmpty()) Text(placeholder, style = style.copy(color = colors.onSurfaceVariant))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = style,
                cursorBrush = SolidColor(if (liquid) Color.Transparent else colors.primary),
                visualTransformation = transformation,
                onTextLayout = { layout = it },
                modifier = modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(requester)
                    .onFocusChanged { focused = it.isFocused },
            )
            if (images) EmojiOverlay(layout)
            if (liquid) LiquidCaret(value.selection, layout, focused, settings, colors.primary, Modifier.matchParentSize())
        }
    }
}

@Composable
private fun EmojiOverlay(layout: TextLayoutResult?) {
    val current = layout ?: return
    val text = current.layoutInput.text.text
    val ranges = remember(text) { emojiRanges(text) }
    val density = LocalDensity.current
    ranges.forEach { range ->
        val box = current.getBoundingBox(range.first)
        val side = minOf(box.width, box.height) * EmojiScale
        with(density) {
            Box(
                Modifier
                    .offset { IntOffset((box.center.x - side / 2).roundToInt(), (box.center.y - side / 2).roundToInt()) }
                    .size(side.toDp()),
            ) {
                key(text.substring(range.first, range.last + 1)) { Emoji3DImage(text.substring(range.first, range.last + 1)) }
            }
        }
    }
}

private const val EmojiScale = 1f

@Composable
private fun LiquidCaret(
    selection: TextRange,
    layout: TextLayoutResult?,
    focused: Boolean,
    settings: AppSettings,
    color: Color,
    modifier: Modifier,
) {
    val visible = focused && selection.collapsed && layout != null
    val rect = layout?.let { it.getCursorRect(selection.end.coerceIn(0, it.layoutInput.text.length)) }
    val smooth = settings.smoothCursor.value
    val stiffness = MinStiffness + settings.cursorSpeed.value * StiffnessStep
    val tailStiffness = stiffness * (1f - settings.handleStretch.value / 100f * TailLag)
    val motion = remember { CaretMotion() }
    var moved by remember { mutableLongStateOf(0L) }
    val target = rect?.let { Offset(it.left, it.top) to it.height }
    val latestTarget by rememberUpdatedState(target)
    LaunchedEffect(target) {
        if (target != null) moved = System.nanoTime()
    }
    LaunchedEffect(smooth, stiffness, tailStiffness) {
        while (true) {
            val goal = snapshotFlow { latestTarget }.filterNotNull().first()
            if (!motion.ready || !smooth) {
                motion.snap(goal.first, goal.second)
                snapshotFlow { latestTarget }.filterNotNull().first { it != goal }
                continue
            }
            var last = withFrameNanos { it }
            while (true) {
                val settled = withFrameNanos { now ->
                    val current = latestTarget ?: return@withFrameNanos true
                    val dt = ((now - last) / 1_000_000_000f).coerceAtMost(MaxFrameSeconds)
                    last = now
                    motion.step(current.first, current.second, dt, stiffness, tailStiffness)
                }
                if (settled) break
            }
        }
    }
    val blink = settings.cursorBlink.value
    val cycle = settings.blinkCycle.value.coerceAtLeast(MinCycle)
    val softness = settings.blinkSmoothness.value / 100f
    val alpha by produceState(1f, visible, moved, blink, cycle, softness) {
        value = 1f
        if (!visible || !blink) return@produceState
        delay(BlinkRestMs)
        if (softness <= 0f) {
            while (true) {
                value = 0f
                delay(cycle / 2L)
                value = 1f
                delay(cycle / 2L)
            }
        }
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                val phase = ((now - start) / 1_000_000L % cycle) / cycle.toFloat()
                val hard = if (phase < 0.5f) 1f else 0f
                val soft = 0.5f + 0.5f * cos(2 * PI * phase).toFloat()
                value = hard + (soft - hard) * softness
            }
        }
    }
    val liquid = settings.liquidCursor.value
    val stretch = settings.cursorStretch.value
    val pull = settings.sidePull.value / 100f
    val width = settings.cursorWidth.value.coerceAtLeast(1)
    Canvas(modifier.graphicsLayer()) {
        if (!visible || !motion.ready || alpha <= 0f) return@Canvas
        val w = width.dp.toPx()
        val h = motion.height
        val tint = color.copy(alpha = alpha)
        val front = motion.head
        var back = motion.tail
        val reach = (stretch * StretchUnit).dp.toPx()
        val dx = front.x - back.x
        val dy = front.y - back.y
        val distance = sqrt(dx * dx + dy * dy)
        if (liquid && distance > reach && distance > 0f) {
            back = Offset(front.x - dx / distance * reach, front.y - dy / distance * reach)
        }
        val span = if (liquid) sqrt((front.x - back.x) * (front.x - back.x) + (front.y - back.y) * (front.y - back.y)) else 0f
        val k = if (reach > 0f) (span / reach).coerceIn(0f, 1f) else 0f
        val barHeight = h * (1f - pull * k * BarSquash)
        val barWidth = w * (1f + k * BarSwell)
        if (liquid && span > 0.5f) {
            val body = (w + (barHeight - w) * (1f - pull) * TrailBody).coerceIn(w, barHeight)
            val ux = (front.x - back.x) / span
            val uy = (front.y - back.y) / span
            val mid = h / 2
            val trail = Path().apply {
                moveTo(back.x - uy * w / 2, back.y + mid + ux * w / 2)
                lineTo(front.x - uy * body / 2, front.y + mid + ux * body / 2)
                lineTo(front.x + uy * body / 2, front.y + mid - ux * body / 2)
                lineTo(back.x + uy * w / 2, back.y + mid - ux * w / 2)
                close()
            }
            drawPath(trail, tint)
            drawCircle(tint, radius = w / 2, center = Offset(back.x, back.y + mid))
        }
        drawRoundRect(
            tint,
            topLeft = Offset(front.x - barWidth / 2, front.y + (h - barHeight) / 2),
            size = Size(barWidth, barHeight),
            cornerRadius = CornerRadius(barWidth / 2),
        )
    }
}

private class CaretMotion {
    var ready by mutableStateOf(false)
    var head by mutableStateOf(Offset.Zero)
    var tail by mutableStateOf(Offset.Zero)
    var height by mutableStateOf(0f)
    private var headSpeed = Offset.Zero
    private var tailSpeed = Offset.Zero
    private var heightSpeed = 0f

    fun snap(point: Offset, size: Float) {
        head = point
        tail = point
        height = size
        headSpeed = Offset.Zero
        tailSpeed = Offset.Zero
        heightSpeed = 0f
        ready = true
    }

    fun step(point: Offset, size: Float, dt: Float, stiffness: Float, tailStiffness: Float): Boolean {
        var left = dt
        var h = head
        var t = tail
        var z = height
        while (left > 0f) {
            val step = minOf(left, SubStep)
            left -= step
            headSpeed += (point - h) * (stiffness * step) - headSpeed * (damping(stiffness, HeadDamping) * step)
            h += headSpeed * step
            tailSpeed += (point - t) * (tailStiffness * step) - tailSpeed * (damping(tailStiffness, TailDamping) * step)
            t += tailSpeed * step
            heightSpeed += (size - z) * stiffness * step - heightSpeed * damping(stiffness, HeadDamping) * step
            z += heightSpeed * step
        }
        val done = (h - point).getDistance() < RestDistance && (t - point).getDistance() < RestDistance &&
            headSpeed.getDistance() < RestSpeed && tailSpeed.getDistance() < RestSpeed && kotlin.math.abs(size - z) < RestDistance
        if (done) snap(point, size) else {
            head = h
            tail = t
            height = z
        }
        return done
    }

    private fun damping(stiffness: Float, ratio: Float) = 2f * ratio * sqrt(stiffness)
}

private const val SubStep = 0.004f
private const val MaxFrameSeconds = 0.2f
private const val RestDistance = 0.3f
private const val RestSpeed = 4f

private val FieldMinHeight = 56.dp
private val FieldMaxHeight = 184.dp
private const val MinStiffness = 200f
private const val StiffnessStep = 40f
private const val TailLag = 0.85f
private const val HeadDamping = 0.8f
private const val TailDamping = 0.9f
private const val MinCycle = 200
private const val BlinkRestMs = 500L
private const val StretchUnit = 2
private const val BarSquash = 0.6f
private const val BarSwell = 0.5f
private const val TrailBody = 0.35f
