package com.pixelgram.desktop.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.ui.theme.LocalAppSettings
import kotlinx.coroutines.delay

private class ThumbGeometry(val top: Float, val height: Float, val travel: Float, val range: Float)

private fun LazyListState.thumb(minHeight: Float): ThumbGeometry? {
    val info = layoutInfo
    val visible = info.visibleItemsInfo
    if (visible.isEmpty()) return null
    val viewport = (info.viewportEndOffset - info.viewportStartOffset).toFloat()
    if (viewport <= 0f) return null
    val average = (visible.sumOf { it.size } + info.mainAxisItemSpacing * (visible.size - 1)).toFloat() / visible.size + info.mainAxisItemSpacing
    val total = average * info.totalItemsCount + info.beforeContentPadding + info.afterContentPadding
    val range = total - viewport
    if (range <= 1f) return null
    val offset = (firstVisibleItemIndex * average + firstVisibleItemScrollOffset).coerceIn(0f, range)
    val height = (viewport * viewport / total).coerceIn(minHeight, viewport)
    val travel = viewport - height
    return ThumbGeometry(info.viewportStartOffset.coerceAtLeast(0) + travel * offset / range, height, travel, range)
}

@Composable
fun Modifier.listScrollbar(state: LazyListState, edge: Float = 4f): Modifier {
    val settings = LocalAppSettings.current
    if (settings?.scrollbars?.value == false) return this
    val alpha = remember { Animatable(0f) }
    var hovered by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    val active = state.isScrollInProgress || hovered || dragging
    LaunchedEffect(active) {
        if (active) alpha.animateTo(1f, tween(FadeInMs))
        else {
            delay(HideDelayMs)
            alpha.animateTo(0f, tween(FadeOutMs))
        }
    }
    val width by animateDpAsState(if (hovered || dragging) WideThumb else Thumb)
    val colors = MaterialTheme.colorScheme
    val idle = colors.onSurfaceVariant.copy(alpha = IdleAlpha)
    val held = colors.primary
    return this
        .pointerInput(state) {
            val zone = HitZone.toPx()
            val minHeight = MinThumb.toPx()
            awaitPointerEventScope {
                var grab = -1f
                var startTop = 0f
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull() ?: continue
                    val inZone = change.position.x >= size.width - zone
                    when (event.type) {
                        PointerEventType.Move -> {
                            hovered = inZone || dragging
                            if (dragging) {
                                val geometry = state.thumb(minHeight)
                                if (geometry != null && geometry.travel > 0f) {
                                    val targetTop = startTop + (change.position.y - grab)
                                    val wanted = (targetTop - geometry.top) * geometry.range / geometry.travel
                                    state.dispatchRawDelta(wanted)
                                }
                                change.consume()
                            }
                        }
                        PointerEventType.Press -> if (inZone) {
                            val geometry = state.thumb(minHeight)
                            if (geometry != null) {
                                val y = change.position.y
                                if (y < geometry.top || y > geometry.top + geometry.height) {
                                    val jump = (y - geometry.height / 2 - geometry.top) * geometry.range / geometry.travel.coerceAtLeast(1f)
                                    state.dispatchRawDelta(jump)
                                }
                                grab = y
                                startTop = state.thumb(minHeight)?.top ?: geometry.top
                                dragging = true
                                change.consume()
                            }
                        }
                        PointerEventType.Release -> if (dragging) {
                            dragging = false
                            hovered = inZone
                            change.consume()
                        }
                        PointerEventType.Exit -> if (!dragging) hovered = false
                    }
                }
            }
        }
        .drawWithContent {
            drawContent()
            val a = alpha.value
            if (a <= 0f) return@drawWithContent
            val geometry = state.thumb(MinThumb.toPx()) ?: return@drawWithContent
            val w = width.toPx()
            val x = size.width - w - edge.dp.toPx()
            drawRoundRect(
                color = lerp(idle, held, if (dragging) 1f else 0f).copy(alpha = (if (dragging) 1f else IdleAlpha) * a),
                topLeft = Offset(x, geometry.top),
                size = Size(w, geometry.height),
                cornerRadius = CornerRadius(w / 2),
            )
        }
}

private val Thumb = 4.dp
private val WideThumb = 8.dp
private val MinThumb = 36.dp
private val HitZone = 14.dp
private const val IdleAlpha = 0.6f
private const val FadeInMs = 120
private const val FadeOutMs = 300
private const val HideDelayMs = 900L
