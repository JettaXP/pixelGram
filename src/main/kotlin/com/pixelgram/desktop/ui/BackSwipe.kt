package com.pixelgram.desktop.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.areAnyPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class BackPhase { Idle, Dragging, Committed, Cancelled }

class BackSwipeState {
    var raw by mutableStateOf(0f)
    var phase by mutableStateOf(BackPhase.Idle)
    var reached by mutableStateOf(false)
    var dx = 0f
    var dy = 0f
    var locked: Boolean? = null
    var rangeMin = 0f
    var sticky = false
    var lastEvent = 0L
    var at by mutableStateOf(Offset.Zero)
    var job: Job? = null
}

object BackZones {
    var list: Rect? = null
    var onList: (() -> Unit)? = null
}

@Composable
fun rememberBackSwipe(): BackSwipeState = remember { BackSwipeState() }

@Composable
fun Modifier.backSwipe(state: BackSwipeState, enabled: () -> Boolean, onBack: () -> Unit): Modifier {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val threshold = with(density) { BackThreshold.toPx() }
    val step = with(density) { ScrollStep.toPx() }
    val lock = with(density) { LockDistance.toPx() }
    val settings = com.pixelgram.desktop.ui.theme.LocalAppSettings.current
    return pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.type != PointerEventType.Scroll) continue
                val change = event.changes.firstOrNull() ?: continue
                if (change.isConsumed || event.buttons.areAnyPressed) continue
                val time = change.uptimeMillis
                val quiet = time - state.lastEvent
                state.lastEvent = time
                if (state.sticky) {
                    if (quiet < StickyGapMs) {
                        change.consume()
                        continue
                    }
                    state.sticky = false
                }
                if (state.locked == false) {
                    state.job?.cancel()
                    state.job = scope.launch {
                        delay(SettleMs)
                        state.dx = 0f
                        state.dy = 0f
                        state.locked = null
                    }
                    continue
                }
                if (state.locked == null && state.dx == 0f && state.dy == 0f) state.at = change.position
                state.dx += -change.scrollDelta.x * step
                state.dy += change.scrollDelta.y * step
                if (state.locked == null && abs(state.dx) + abs(state.dy) >= lock) {
                    val horizontal = abs(state.dx) - abs(state.dy) > lock / 2f
                    state.locked = horizontal && state.dx > 0f && enabled() && settings?.backSwipe?.value == true
                    state.rangeMin = 0f
                }
                if (state.locked == null && abs(change.scrollDelta.x) > abs(change.scrollDelta.y)) change.consume()
                if (state.locked == true) {
                    change.consume()
                    val value = state.dx / threshold
                    if (value < state.rangeMin) state.rangeMin = value
                    if (value > state.rangeMin + RangeSpan) state.rangeMin = value - RangeSpan
                    val raw = (value - state.rangeMin).coerceIn(0f, RangeSpan)
                    if (!state.reached && raw >= 1f) state.reached = true
                    if (state.reached && raw < Deactivate) state.reached = false
                    state.raw = raw
                    state.phase = BackPhase.Dragging
                }
                state.job?.cancel()
                state.job = scope.launch {
                    delay(SettleMs)
                    val fire = state.locked == true && state.reached
                    val where = state.at
                    val dragging = state.locked == true
                    state.dx = 0f
                    state.dy = 0f
                    state.locked = null
                    state.reached = false
                    if (!dragging) return@launch
                    state.sticky = true
                    if (fire) {
                        state.phase = BackPhase.Committed
                        val list = BackZones.list
                        if (list != null && list.contains(where)) BackZones.onList?.invoke() else onBack()
                    } else {
                        state.phase = BackPhase.Cancelled
                    }
                    delay(FinishMs)
                    state.phase = BackPhase.Idle
                    state.raw = 0f
                }
            }
        }
    }
}

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t.coerceIn(0f, 1f)

@Composable
fun BoxScope.BackSwipeIndicator(state: BackSwipeState, followList: Boolean = true) {
    val density = LocalDensity.current
    val dp = density.density
    val phase = state.phase
    val dragging = phase == BackPhase.Dragging
    val reach by androidx.compose.animation.core.animateFloatAsState(
        if (state.reached && dragging || phase == BackPhase.Committed) 1f else 0f,
        spring(dampingRatio = ReachDamping, stiffness = ReachStiffness),
    )
    val end = remember { Animatable(0f) }
    var frozen by remember { mutableStateOf(0f) }
    androidx.compose.runtime.LaunchedEffect(phase) {
        when (phase) {
            BackPhase.Committed, BackPhase.Cancelled -> {
                frozen = state.raw
                end.snapTo(0f)
                end.animateTo(1f, spring(dampingRatio = 1f, stiffness = if (phase == BackPhase.Committed) EndStiffness else CancelStiffness))
            }
            else -> end.snapTo(0f)
        }
    }
    val raw = if (dragging) state.raw else frozen
    val pre = raw.coerceIn(0f, 1f)
    val over = ((raw - 1f) / StretchRange).coerceIn(0f, 1f)
    val fade = when (phase) {
        BackPhase.Committed, BackPhase.Cancelled -> 1f - end.value
        BackPhase.Idle -> 0f
        else -> 1f
    }
    val shrink = if (phase == BackPhase.Cancelled) 1f - end.value else 1f
    val width = lerp(lerp(0f, 51f, pre), lerp(48f, 60f, over), reach) * shrink
    val height = lerp(lerp(48f, 46f, pre), 48f, reach)
    val edge = lerp(lerp(6f, 16f, pre), 24f, reach)
    val far = lerp(lerp(6f, 20f, pre), 24f, reach)
    val length = lerp(lerp(8.6f, 8f, pre), lerp(6.4f, 5.6f, over), reach)
    val arrowHeight = lerp(lerp(5f, 5.6f, pre), lerp(7.2f, 8f, over), reach)
    val arrowAlpha = lerp((pre * 4f).coerceAtMost(1f), 1f, reach)
    val margin = lerp(4f, lerp(14f, 18f, over), reach)
    val scale = if (phase == BackPhase.Committed) lerp(1f, CommitScale, end.value) else lerp(0.98f, 1f, reach)
    val alpha = fade
    if (phase == BackPhase.Idle || width < 0.5f || alpha <= 0f) return
    val colors = MaterialTheme.colorScheme
    val background = colors.secondaryContainer
    val arrow = colors.onSecondaryContainer
    val list = BackZones.list.takeIf { followList }
    val inList = list != null && list.contains(state.at)
    Canvas(Modifier.fillMaxSize()) {
        val w = width.coerceAtLeast(0f) * dp
        val h = height.coerceAtLeast(0f) * dp
        val left = if (inList) list!!.left else list?.right ?: 0f
        val centerY = if (inList) list!!.center.y else size.height / 2f
        translate(left + margin * dp, centerY) {
            scale(scale, scale, pivot = Offset(if (phase == BackPhase.Committed) w / 2f else w, 0f)) {
                val path = Path().apply {
                    addRoundRect(
                        RoundRect(
                            Rect(0f, -h / 2f, w, h / 2f),
                            topLeft = CornerRadius(edge.coerceAtLeast(0f) * dp),
                            bottomLeft = CornerRadius(edge.coerceAtLeast(0f) * dp),
                            topRight = CornerRadius(far.coerceAtLeast(0f) * dp),
                            bottomRight = CornerRadius(far.coerceAtLeast(0f) * dp),
                        ),
                    )
                }
                drawPath(path, background, alpha = alpha.coerceIn(0f, 1f))
                val dx = length * dp
                val dy = arrowHeight * dp
                val offset = (w - dx) / 2f
                val chevron = Path().apply {
                    moveTo(offset + dx, -dy)
                    lineTo(offset, 0f)
                    lineTo(offset + dx, dy)
                }
                drawPath(chevron, arrow, alpha = (arrowAlpha * alpha).coerceIn(0f, 1f), style = Stroke(width = ArrowThickness.toPx(), cap = StrokeCap.Square))
            }
        }
    }
}

@Composable
fun BackSwipeBox(onBack: () -> Unit, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val state = rememberBackSwipe()
    Box(modifier.backSwipe(state, enabled = { true }, onBack = onBack)) {
        content()
        BackSwipeIndicator(state, followList = false)
    }
}

private val BackThreshold = 80.dp
private val ScrollStep = 64.dp
private val LockDistance = 4.dp
private const val Deactivate = 0.95f
private const val StretchRange = 1.5f
private const val SettleMs = 110L
private const val StickyGapMs = 260L
private const val FinishMs = 450L
private const val CommitScale = 0.86f
private const val RangeSpan = 1f + StretchRange
private const val ReachDamping = 0.55f
private const val ReachStiffness = 700f
private const val EndStiffness = 900f
private const val CancelStiffness = 600f
private val ArrowThickness = 4.dp

object BackSwipeHolder {
    @Composable
    fun Modifier.backSwipeOf(state: BackSwipeState): Modifier =
        backSwipe(state, enabled = { !com.pixelgram.desktop.ui.main.MessageSearch.open }) { EscapeStack.dispatch() }
}
