package com.pixelgram.desktop.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.areAnyPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.automirrored.filled.ArrowBack
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class BackSwipeState {
    val pull = Animatable(0f)
    var job: Job? = null
    var follow: Job? = null
    var total = 0f
    var vertical = 0f
    var at by androidx.compose.runtime.mutableStateOf(androidx.compose.ui.geometry.Offset.Zero)
}

object BackZones {
    var list: androidx.compose.ui.geometry.Rect? = null
    var onList: (() -> Unit)? = null
}

@Composable
fun rememberBackSwipe(): BackSwipeState = remember { BackSwipeState() }

@Composable
fun Modifier.backSwipe(state: BackSwipeState, enabled: () -> Boolean, onBack: () -> Unit): Modifier {
    val scope = rememberCoroutineScope()
    val threshold = with(LocalDensity.current) { BackThreshold.toPx() }
    val step = with(LocalDensity.current) { ScrollStep.toPx() }
    return pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                if (event.type != PointerEventType.Scroll) continue
                val change = event.changes.firstOrNull() ?: continue
                if (change.isConsumed || event.buttons.areAnyPressed) continue
                if (state.total == 0f && state.vertical == 0f) state.at = change.position
                state.total += -change.scrollDelta.x * step
                state.vertical += kotlin.math.abs(change.scrollDelta.y) * step
                if (state.total > 0f && state.total > state.vertical && enabled()) {
                    val target = (state.total / threshold).coerceIn(0f, MaxPull)
                    state.follow?.cancel()
                    state.follow = scope.launch { state.pull.animateTo(target, androidx.compose.animation.core.spring(dampingRatio = 0.9f, stiffness = 900f)) }
                }
                state.job?.cancel()
                state.job = scope.launch {
                    delay(SettleMs)
                    val reached = state.total / threshold >= 1f && state.total > state.vertical
                    val where = state.at
                    state.total = 0f
                    state.vertical = 0f
                    state.follow?.cancel()
                    if (reached) {
                        val list = BackZones.list
                        if (list != null && list.contains(where)) BackZones.onList?.invoke() else onBack()
                    }
                    state.pull.animateTo(0f, androidx.compose.animation.core.spring(dampingRatio = 0.85f, stiffness = 500f))
                }
            }
        }
    }
}

@Composable
fun BoxScope.BackSwipeIndicator(state: BackSwipeState) {
    val value = state.pull.value
    if (value <= 0f) return
    val colors = MaterialTheme.colorScheme
    val reached = value >= 1f
    val shift = with(LocalDensity.current) { (IndicatorTravel * value.coerceAtMost(1f)).roundToPx() }
    val list = BackZones.list
    val inList = list != null && list.contains(state.at)
    val left = if (inList) list!!.left else list?.right ?: 0f
    val half = with(LocalDensity.current) { (IndicatorSize / 2).roundToPx() }
    Box(
        Modifier
            .align(Alignment.TopStart)
            .offset { IntOffset(left.toInt() + shift - IndicatorTravel.roundToPx(), state.at.y.toInt() - half) }
            .size(IndicatorSize)
            .graphicsLayer {
                val scale = 0.6f + value.coerceAtMost(1f) * 0.4f
                scaleX = scale
                scaleY = scale
                alpha = (value * 2).coerceAtMost(1f)
            }
            .background(if (reached) colors.primary else colors.secondaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = if (reached) colors.onPrimary else colors.onSecondaryContainer,
            modifier = Modifier.size(24.dp),
        )
    }
}

private val BackThreshold = 120.dp
private val ScrollStep = 64.dp
private val IndicatorSize = 52.dp
private val IndicatorTravel = 72.dp
private const val SettleMs = 180L
private const val MaxPull = 1.3f

object BackSwipeHolder {
    @Composable
    fun Modifier.backSwipeOf(state: BackSwipeState): Modifier =
        backSwipe(state, enabled = { !com.pixelgram.desktop.ui.main.MessageSearch.open }) { EscapeStack.dispatch() }
}
