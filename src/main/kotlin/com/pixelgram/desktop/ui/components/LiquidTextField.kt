package com.pixelgram.desktop.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
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
import kotlinx.coroutines.launch

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
                visualTransformation = visualTransformation,
                onTextLayout = { layout = it },
                modifier = modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(requester)
                    .onFocusChanged { focused = it.isFocused },
            )
            if (liquid) LiquidCaret(value.selection, layout, focused, settings, colors.primary, Modifier.matchParentSize())
        }
    }
}

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
    val head = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val tail = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val height = remember { Animatable(0f) }
    var ready by remember { mutableStateOf(false) }
    var moved by remember { mutableLongStateOf(0L) }
    val smooth = settings.smoothCursor.value
    val stiffness = MinStiffness + settings.cursorSpeed.value * StiffnessStep
    val tailStiffness = stiffness * (1f - settings.handleStretch.value / 100f * TailLag)
    LaunchedEffect(rect?.left, rect?.top, rect?.height, smooth, stiffness) {
        val target = rect ?: return@LaunchedEffect
        moved = System.nanoTime()
        val point = Offset(target.left, target.top)
        if (!ready || !smooth) {
            head.snapTo(point)
            tail.snapTo(point)
            height.snapTo(target.height)
            ready = true
            return@LaunchedEffect
        }
        launch { head.animateTo(point, spring(dampingRatio = HeadDamping, stiffness = stiffness)) }
        launch { tail.animateTo(point, spring(dampingRatio = TailDamping, stiffness = tailStiffness)) }
        height.animateTo(target.height, spring(dampingRatio = HeadDamping, stiffness = stiffness))
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
        if (!visible || !ready || alpha <= 0f) return@Canvas
        val w = width.dp.toPx()
        val h = height.value
        val tint = color.copy(alpha = alpha)
        val front = head.value
        var back = tail.value
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
            drawLine(
                tint,
                start = Offset(back.x, back.y + h / 2),
                end = Offset(front.x, front.y + h / 2),
                strokeWidth = (w + (barHeight - w) * (1f - pull) * TrailBody).coerceAtLeast(w),
                cap = StrokeCap.Round,
            )
        }
        drawRoundRect(
            tint,
            topLeft = Offset(front.x - barWidth / 2, front.y + (h - barHeight) / 2),
            size = Size(barWidth, barHeight),
            cornerRadius = CornerRadius(barWidth / 2),
        )
    }
}

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
