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
import kotlin.math.pow
import com.pixelgram.desktop.core.CursorStyle
import androidx.compose.runtime.State
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
    trailJump: Long = 0L,
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
            if (liquid) CustomCaret(value.selection, layout, focused, settings, colors.primary, Modifier.matchParentSize(), trailJump)
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
private fun rememberBlink(visible: Boolean, moved: Long, settings: AppSettings): State<Float> {
    val blink = settings.cursorBlink.value
    val cycle = settings.blinkCycle.value.coerceAtLeast(MinCycle)
    val softness = settings.blinkSmoothness.value / 100f
    val windowFocused = androidx.compose.ui.platform.LocalWindowInfo.current.isWindowFocused
    return produceState(1f, visible, moved, blink, cycle, softness, windowFocused) {
        value = 1f
        if (!visible || !blink || !windowFocused) return@produceState
        delay(BlinkRestMs)
        val half = cycle / 2L
        val fade = (half * softness).toLong().coerceIn(0L, half)
        val hold = half - fade
        suspend fun ease(from: Float, to: Float) {
            if (fade <= 0L) {
                value = to
                return
            }
            val start = System.nanoTime()
            while (true) {
                val progress = ((System.nanoTime() - start) / 1_000_000f / fade).coerceIn(0f, 1f)
                val curve = 0.5f - 0.5f * cos(PI * progress).toFloat()
                value = from + (to - from) * curve
                if (progress >= 1f) break
                delay(FadeFrameMs)
            }
        }
        while (true) {
            delay(hold)
            ease(1f, 0f)
            delay(hold)
            ease(0f, 1f)
        }
    }
}

@Composable
private fun CustomCaret(
    selection: TextRange,
    layout: TextLayoutResult?,
    focused: Boolean,
    settings: AppSettings,
    color: Color,
    modifier: Modifier,
    trailJump: Long = 0L,
) {
    if (settings.cursorStyle.value == CursorStyle.Kitty) TrailCaret(selection, layout, focused, settings, color, modifier, trailJump)
    else LiquidCaret(selection, layout, focused, settings, color, modifier)
}

@Composable
private fun TrailCaret(
    selection: TextRange,
    layout: TextLayoutResult?,
    focused: Boolean,
    settings: AppSettings,
    color: Color,
    modifier: Modifier,
    trailJump: Long = 0L,
) {
    val visible = focused && selection.collapsed && layout != null
    val rect = layout?.let { it.getCursorRect(selection.end.coerceIn(0, it.layoutInput.text.length)) }
    val trail = remember { CursorTrail() }
    if (trailJump != trail.jump) {
        trail.jump = trailJump
        trail.forceNext = true
    }
    var moved by remember { mutableLongStateOf(0L) }
    val latest by rememberUpdatedState(rect)
    val width = with(LocalDensity.current) { settings.cursorWidth.value.coerceAtLeast(1).dp.toPx() }
    val fast = settings.trailFast.value / 1000f
    val slow = maxOf(settings.trailSlow.value / 1000f, fast)
    val threshold = settings.trailThreshold.value
    LaunchedEffect(rect) {
        if (rect != null) moved = System.nanoTime()
    }
    LaunchedEffect(width, fast, slow, threshold) {
        while (true) {
            snapshotFlow { latest }.filterNotNull().first { trail.aim(it, width, threshold) }
            var last = withFrameNanos { it }
            while (true) {
                val done = withFrameNanos { now ->
                    latest?.let { trail.aim(it, width, threshold) }
                    val dt = ((now - last) / 1_000_000_000f).coerceAtMost(MaxFrameSeconds)
                    last = now
                    trail.step(dt, fast, slow)
                }
                if (done) break
            }
        }
    }
    val alpha by rememberBlink(visible, moved, settings)
    Canvas(modifier.graphicsLayer()) {
        val target = rect ?: return@Canvas
        if (!visible) return@Canvas
        trail.tick
        if (trail.active) {
            val path = Path().apply {
                moveTo(trail.x[0], trail.y[0])
                for (i in 1 until 4) lineTo(trail.x[i], trail.y[i])
                close()
            }
            drawPath(path, color)
        }
        if (alpha > 0f) {
            drawRect(color.copy(alpha = alpha), topLeft = Offset(target.left, target.top), size = Size(width, target.height))
        }
    }
}

private class CursorTrail {
    val x = FloatArray(4)
    val y = FloatArray(4)
    var active by mutableStateOf(false)
    private var ready = false
    private var edges = FloatArray(4)
    var tick by mutableLongStateOf(0L)
        private set
    var jump = 0L
    var forceNext = false

    fun aim(rect: androidx.compose.ui.geometry.Rect, width: Float, threshold: Int): Boolean {
        val next = floatArrayOf(rect.left, rect.top, rect.left + width, rect.bottom)
        if (ready && next.contentEquals(edges)) return active
        val cell = rect.height * CellAspect
        val jumpX = kotlin.math.abs(next[0] - edges[0]) / cell
        val jumpY = kotlin.math.abs(next[1] - edges[1]) / rect.height.coerceAtLeast(1f)
        val small = jumpX <= threshold && jumpY < 1f && threshold > 0 && !(forceNext && ready)
        forceNext = false
        edges = next
        if (!ready || (!active && small)) {
            snap()
            ready = true
            return false
        }
        active = true
        return true
    }

    private fun snap() {
        for (i in 0 until 4) {
            x[i] = edges[CornerX[i]]
            y[i] = edges[CornerY[i]]
        }
        active = false
        tick++
    }

    fun step(dt: Float, fast: Float, slow: Float): Boolean {
        val centerX = (edges[0] + edges[2]) / 2
        val centerY = (edges[1] + edges[3]) / 2
        val half = sqrt((edges[2] - edges[0]).let { it * it } + (edges[3] - edges[1]).let { it * it }) / 2
        val dx = FloatArray(4)
        val dy = FloatArray(4)
        val dot = FloatArray(4)
        for (i in 0 until 4) {
            dx[i] = edges[CornerX[i]] - x[i]
            dy[i] = edges[CornerY[i]] - y[i]
            val length = sqrt(dx[i] * dx[i] + dy[i] * dy[i])
            dot[i] = if (length < 1e-3f || half <= 0f) 0f
            else (dx[i] * (edges[CornerX[i]] - centerX) + dy[i] * (edges[CornerY[i]] - centerY)) / half / length
        }
        val low = dot.min()
        val high = dot.max()
        var settled = true
        for (i in 0 until 4) {
            if (dx[i] == 0f && dy[i] == 0f) continue
            val decay = if (high == low) slow else slow + (fast - slow) * (dot[i] - low) / (high - low)
            val part = 1f - 2f.pow(-10f * dt / decay)
            x[i] += dx[i] * part
            y[i] += dy[i] * part
            if (kotlin.math.abs(edges[CornerX[i]] - x[i]) > RestGap || kotlin.math.abs(edges[CornerY[i]] - y[i]) > RestGap) settled = false
        }
        if (settled) snap() else tick++
        return settled
    }
}

private val CornerX = intArrayOf(2, 2, 0, 0)
private val CornerY = intArrayOf(1, 3, 3, 1)
private const val CellAspect = 0.5f
private const val RestGap = 0.5f

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
    val alpha by rememberBlink(visible, moved, settings)
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
private const val FadeFrameMs = 33L
private const val StretchUnit = 2
private const val BarSquash = 0.6f
private const val BarSwell = 0.5f
private const val TrailBody = 0.35f
