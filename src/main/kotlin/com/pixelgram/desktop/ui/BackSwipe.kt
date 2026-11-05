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
                    state.locked = abs(state.dx) > abs(state.dy) * LockRatio && state.dx > 0f && enabled() && settings?.backSwipe?.value == true
                }
                if (state.locked == null && abs(change.scrollDelta.x) > abs(change.scrollDelta.y)) change.consume()
                if (state.locked == true) {
                    change.consume()
                    val raw = (state.dx / threshold).coerceAtLeast(0f)
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

private class Dims(
    val width: Float, val height: Float, val edge: Float, val far: Float,
    val length: Float, val arrowHeight: Float, val arrowAlpha: Float,
    val margin: Float, val scale: Float, val alpha: Float,
)

private class Springs(
    val width: Pair<Float, Float>, val height: Pair<Float, Float>, val edge: Pair<Float, Float>, val far: Pair<Float, Float>,
    val arrow: Pair<Float, Float>, val margin: Pair<Float, Float>, val scale: Pair<Float, Float>,
)

private val PreSprings = Springs(650f to 1f, 1500f to 0.45f, 250f to 0.5f, 300f to 1f, 100f to 0.6f, 6000f to 1f, 120f to 0.8f)
private val ActiveSprings = Springs(850f to 0.75f, 10000f to 1f, 2600f to 0.855f, 1200f to 0.30f, 1500f to 0.29f, 1000f to 0.8f, 325f to 0.55f)
private val EndSprings = Springs(10000f to 1f, 10000f to 1f, 10000f to 1f, 10000f to 1f, 1500f to 0.29f, 1000f to 0.8f, 5700f to 1f)
private val CancelSprings = Springs(450f to 1f, 1500f to 0.45f, 150f to 0.5f, 300f to 0.5f, 600f to 0.4f, 800f to 0.76f, 120f to 0.8f)

@Composable
private fun springy(target: Float, spec: Pair<Float, Float>, velocity: Float = 0f, delayMs: Long = 0L): Float {
    val anim = remember { Animatable(target) }
    androidx.compose.runtime.LaunchedEffect(target, spec, velocity) {
        if (delayMs > 0) delay(delayMs)
        anim.animateTo(target, spring(dampingRatio = spec.second, stiffness = spec.first), initialVelocity = if (velocity != 0f) velocity else anim.velocity)
    }
    return anim.value
}

@Composable
fun BoxScope.BackSwipeIndicator(state: BackSwipeState, followList: Boolean = true) {
    val density = LocalDensity.current
    val dp = density.density
    val phase = state.phase
    val over = ((state.raw - 1f) / StretchRange).coerceIn(0f, 1f)
    val pre = state.raw.coerceIn(0f, 1f)
    val (target, springs) = when {
        phase == BackPhase.Idle -> Dims(0f, 48f, 6f, 6f, 8.6f, 5f, 0f, 4f, 0.98f, 0f) to CancelSprings
        phase == BackPhase.Committed -> Dims(lerp(48f, 60f, over), 48f, 24f, 24f, lerp(6.4f, 5.6f, over), lerp(7.2f, 8f, over), 0f, lerp(14f, 18f, over), CommitScale, 0f) to EndSprings
        phase == BackPhase.Cancelled -> Dims(0f, 48f, 6f, 6f, 8.6f, 5f, 0f, 4f, 0.98f, 0f) to CancelSprings
        state.reached -> Dims(lerp(48f, 60f, over), 48f, 24f, 24f, lerp(6.4f, 5.6f, over), lerp(7.2f, 8f, over), 1f, lerp(14f, 18f, over), 1f, 1f) to ActiveSprings
        else -> Dims(lerp(0f, 51f, pre), lerp(48f, 46f, pre), lerp(6f, 16f, pre), lerp(6f, 20f, pre), lerp(8.6f, 8f, pre), lerp(5f, 5.6f, pre), (pre * 4f).coerceAtMost(1f), 4f, 0.98f, 1f) to PreSprings
    }
    val width = springy(target.width, springs.width)
    val height = springy(target.height, springs.height)
    val edge = springy(target.edge, springs.edge)
    val far = springy(target.far, springs.far)
    val length = springy(target.length, springs.arrow)
    val arrowHeight = springy(target.arrowHeight, springs.arrow)
    val arrowAlpha = springy(target.arrowAlpha, if (state.reached) 180f to 0.9f else 2000f to 0.6f, delayMs = if (phase == BackPhase.Committed) CommitArrowDelayMs else 0L)
    val margin = springy(target.margin, springs.margin)
    val scale = springy(target.scale, if (phase == BackPhase.Committed) 5700f to 1f else springs.scale, velocity = if (phase == BackPhase.Committed) CommitPopVelocity else 0f)
    val alpha = springy(target.alpha, when (phase) {
        BackPhase.Committed -> 1400f to 1f
        BackPhase.Cancelled -> 450f to 1f
        else -> 10000f to 1f
    })
    if (phase == BackPhase.Idle && width < 0.5f) return
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

private val BackThreshold = 120.dp
private val ScrollStep = 64.dp
private val LockDistance = 10.dp
private const val LockRatio = 1.2f
private const val Deactivate = 0.8f
private const val StretchRange = 1.5f
private const val SettleMs = 140L
private const val FinishMs = 450L
private const val CommitScale = 0.86f
private const val CommitPopVelocity = 3f
private const val CommitArrowDelayMs = 80L
private val ArrowThickness = 4.dp

object BackSwipeHolder {
    @Composable
    fun Modifier.backSwipeOf(state: BackSwipeState): Modifier =
        backSwipe(state, enabled = { !com.pixelgram.desktop.ui.main.MessageSearch.open }) { EscapeStack.dispatch() }
}
