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

class BackSwipeState {
    val progress = Animatable(0f)
    val active = Animatable(0f)
    val commit = Animatable(0f)
    var dx = 0f
    var dy = 0f
    var locked: Boolean? = null
    var reached = false
    var at by mutableStateOf(Offset.Zero)
    var job: Job? = null
    var follow: Job? = null
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
    return pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                if (event.type != PointerEventType.Scroll) continue
                val change = event.changes.firstOrNull() ?: continue
                if (change.isConsumed || event.buttons.areAnyPressed) continue
                if (state.locked == null && state.dx == 0f && state.dy == 0f) state.at = change.position
                state.dx += -change.scrollDelta.x * step
                state.dy += change.scrollDelta.y * step
                if (state.locked == null && abs(state.dx) + abs(state.dy) >= lock) {
                    state.locked = abs(state.dx) > abs(state.dy) * LockRatio && state.dx > 0f && enabled()
                }
                if (state.locked == true) {
                    change.consume()
                    val raw = (state.dx / threshold).coerceAtLeast(0f)
                    val shown = if (raw <= 1f) raw else 1f + (raw - 1f) * OverDamping
                    if (!state.reached && raw >= 1f) state.reached = true
                    if (state.reached && raw < Deactivate) state.reached = false
                    state.follow?.cancel()
                    state.follow = scope.launch {
                        launch { state.progress.animateTo(shown.coerceAtMost(MaxPull), spring(dampingRatio = 1f, stiffness = FollowStiffness)) }
                        state.active.animateTo(if (state.reached) 1f else 0f, spring(dampingRatio = 0.55f, stiffness = 700f))
                    }
                }
                state.job?.cancel()
                state.job = scope.launch {
                    delay(SettleMs)
                    val fire = state.locked == true && state.reached
                    val where = state.at
                    state.dx = 0f
                    state.dy = 0f
                    state.locked = null
                    state.reached = false
                    state.follow?.cancel()
                    if (fire) {
                        val list = BackZones.list
                        if (list != null && list.contains(where)) BackZones.onList?.invoke() else onBack()
                        state.commit.animateTo(1f, spring(dampingRatio = 1f, stiffness = 500f))
                        state.progress.snapTo(0f)
                        state.active.snapTo(0f)
                        state.commit.snapTo(0f)
                    } else {
                        launch { state.active.animateTo(0f, spring(dampingRatio = 1f, stiffness = 600f)) }
                        state.progress.animateTo(0f, spring(dampingRatio = 1f, stiffness = 500f))
                    }
                }
            }
        }
    }
}

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

@Composable
fun BoxScope.BackSwipeIndicator(state: BackSwipeState, followList: Boolean = true) {
    val p = state.progress.value
    if (p <= 0f && state.commit.value <= 0f) return
    val colors = MaterialTheme.colorScheme
    val background = colors.secondaryContainer
    val arrow = colors.onSecondaryContainer
    val list = BackZones.list.takeIf { followList }
    val inList = list != null && list.contains(state.at)
    Canvas(Modifier.fillMaxSize()) {
        val pre = p.coerceAtMost(1f)
        val over = (p - 1f).coerceAtLeast(0f) / (MaxPull - 1f)
        val a = state.active.value
        val c = state.commit.value
        val width = lerp(lerp(0f, PreWidth.toPx(), pre), ActiveWidth.toPx(), a) + (StretchWidth.toPx() - ActiveWidth.toPx()) * over * a
        val height = lerp(lerp(EntryHeight.toPx(), PreHeight.toPx(), pre), ActiveHeight.toPx(), a)
        val edge = lerp(lerp(EntryCorner.toPx(), PreEdgeCorner.toPx(), pre), ActiveCorner.toPx(), a)
        val far = lerp(lerp(EntryCorner.toPx(), PreFarCorner.toPx(), pre), ActiveCorner.toPx(), a)
        val margin = lerp(EntryMargin.toPx(), ActiveMargin.toPx(), a) + (StretchMargin.toPx() - ActiveMargin.toPx()) * over * a
        val arrowLength = lerp(lerp(EntryArrowLength.toPx(), PreArrowLength.toPx(), pre), ActiveArrowLength.toPx(), a)
        val arrowHeight = lerp(lerp(EntryArrowHeight.toPx(), PreArrowHeight.toPx(), pre), ActiveArrowHeight.toPx(), a)
        val arrowAlpha = (pre * 3f).coerceIn(0f, 1f) * (1f - c)
        val scale = lerp(lerp(EntryScale, 1f, a), CommitScale, c)
        val left = if (inList) list!!.left else list?.right ?: 0f
        val centerY = if (inList) list!!.center.y else size.height / 2f
        translate(left + margin, centerY) {
            scale(scale, scale, pivot = Offset(0f, 0f)) {
                val path = Path().apply {
                    addRoundRect(
                        RoundRect(
                            Rect(0f, -height / 2f, width, height / 2f),
                            topLeft = CornerRadius(edge),
                            bottomLeft = CornerRadius(edge),
                            topRight = CornerRadius(far),
                            bottomRight = CornerRadius(far),
                        ),
                    )
                }
                drawPath(path, background, alpha = 1f - c)
                val offset = (width - arrowLength) / 2f
                val chevron = Path().apply {
                    moveTo(offset + arrowLength, -arrowHeight)
                    lineTo(offset, 0f)
                    lineTo(offset + arrowLength, arrowHeight)
                }
                drawPath(chevron, arrow, alpha = arrowAlpha, style = Stroke(width = ArrowThickness.toPx(), cap = StrokeCap.Square))
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
private const val OverDamping = 0.35f
private const val SettleMs = 140L
private const val MaxPull = 1.4f
private const val FollowStiffness = 1400f
private const val EntryScale = 0.98f
private const val CommitScale = 0.85f
private val ArrowThickness = 4.dp
private val EntryHeight = 48.dp
private val EntryCorner = 6.dp
private val EntryMargin = 4.dp
private val EntryArrowLength = 8.6.dp
private val EntryArrowHeight = 5.dp
private val PreWidth = 51.dp
private val PreHeight = 46.dp
private val PreEdgeCorner = 16.dp
private val PreFarCorner = 20.dp
private val PreArrowLength = 8.dp
private val PreArrowHeight = 5.6.dp
private val ActiveWidth = 48.dp
private val ActiveHeight = 48.dp
private val ActiveCorner = 24.dp
private val ActiveMargin = 14.dp
private val ActiveArrowLength = 6.4.dp
private val ActiveArrowHeight = 7.2.dp
private val StretchWidth = 60.dp
private val StretchMargin = 18.dp

object BackSwipeHolder {
    @Composable
    fun Modifier.backSwipeOf(state: BackSwipeState): Modifier =
        backSwipe(state, enabled = { !com.pixelgram.desktop.ui.main.MessageSearch.open }) { EscapeStack.dispatch() }
}
