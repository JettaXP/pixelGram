package com.pixelgram.desktop.ui.components

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.addOutline
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.exp
import androidx.compose.runtime.withFrameNanos

private class WheelGlide {
    var remaining = 0f
    var job: Job? = null
}

fun Modifier.smoothWheel(state: ScrollableState, reverse: Boolean = false): Modifier = composed {
    val scope = rememberCoroutineScope()
    val step = with(LocalDensity.current) { WheelStep.toPx() }
    val glide = remember(state) { WheelGlide() }
    pointerInput(state, reverse) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.type != PointerEventType.Scroll) continue
                val change = event.changes.firstOrNull() ?: continue
                if (change.isConsumed) continue
                val delta = change.scrollDelta
                if (delta.y == 0f || abs(delta.x) > abs(delta.y)) continue
                change.consume()
                val sign = if (reverse) -1f else 1f
                val notch = abs(delta.y - kotlin.math.round(delta.y)) < NotchTolerance && abs(delta.y) >= 1f
                if (!notch) {
                    glide.job?.cancel()
                    glide.remaining = 0f
                    state.dispatchRawDelta(delta.y * step * TouchpadFactor * sign)
                    continue
                }
                glide.remaining += delta.y * step * sign
                if (glide.job?.isActive == true) continue
                glide.job = scope.launch {
                    var last = withFrameNanos { it }
                    while (abs(glide.remaining) >= MinPiece) {
                        val now = withFrameNanos { it }
                        val elapsed = ((now - last) / 1_000_000f).coerceIn(0f, MaxFrameMs)
                        last = now
                        val piece = glide.remaining * (1f - exp(-elapsed / GlideTauMs))
                        if (piece == 0f) continue
                        glide.remaining -= piece
                        val used = state.dispatchRawDelta(piece)
                        if (abs(used) < abs(piece) * 0.5f) glide.remaining = 0f
                    }
                    if (glide.remaining != 0f) state.dispatchRawDelta(glide.remaining)
                    glide.remaining = 0f
                }
            }
        }
    }
}

object SmoothWheelScope {
    fun Modifier.glide(state: ScrollableState, reverse: Boolean = false): Modifier = smoothWheel(state, reverse)
}

private val WheelStep = 72.dp
private const val GlideTauMs = 55f
private const val MaxFrameMs = 50f
private const val MinPiece = 0.5f
private const val NotchTolerance = 0.001f
private const val TouchpadFactor = 1f

fun Modifier.smoothClip(shape: androidx.compose.ui.graphics.Shape): Modifier = this
    .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val outside = androidx.compose.ui.graphics.Path().apply {
            addRect(androidx.compose.ui.geometry.Rect(-1f, -1f, size.width + 1f, size.height + 1f))
            addOutline(shape.createOutline(size, layoutDirection, this@drawWithContent))
            fillType = androidx.compose.ui.graphics.PathFillType.EvenOdd
        }
        drawPath(outside, androidx.compose.ui.graphics.Color.Black, blendMode = androidx.compose.ui.graphics.BlendMode.DstOut)
    }
