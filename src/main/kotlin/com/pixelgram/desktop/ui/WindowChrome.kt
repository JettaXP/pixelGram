package com.pixelgram.desktop.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.FilterNone
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import java.awt.Cursor
import java.awt.MouseInfo
import java.awt.Rectangle

// pixelGram's own window title bar: drag to move, double click to maximize,
// Material 3 buttons instead of the system ones.
@Composable
fun FrameWindowScope.TitleBar(state: WindowState, onClose: () -> Unit) {
    val maximized = state.placement == WindowPlacement.Maximized
    val toggleMaximized = {
        state.placement = if (maximized) WindowPlacement.Floating else WindowPlacement.Maximized
    }
    val lastClick = remember { longArrayOf(0L) }
    Row(
        modifier = Modifier.fillMaxWidth().height(TitleBarHeight).background(MaterialTheme.colorScheme.surfaceContainer),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WindowDraggableArea(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .pointerInput(maximized) {
                    awaitPointerEventScope {
                        while (true) {
                            if (awaitPointerEvent().type != PointerEventType.Release) continue
                            val now = System.currentTimeMillis()
                            if (now - lastClick[0] < DoubleClickMs) toggleMaximized()
                            lastClick[0] = now
                        }
                    }
                },
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(start = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                themedAppIcon()?.let { Image(it, contentDescription = null, modifier = Modifier.size(20.dp)) }
                Text("pixelGram", style = MaterialTheme.typography.titleSmall)
            }
        }
        TitleButton(Icons.Filled.Remove, "Minimize") { state.isMinimized = true }
        TitleButton(if (maximized) Icons.Filled.FilterNone else Icons.Filled.CropSquare, "Maximize", onClick = toggleMaximized)
        TitleButton(Icons.Filled.Close, "Close", danger = true, onClick = onClose)
        Spacer(Modifier.width(6.dp))
    }
}

val TitleBarHeight = 40.dp
private const val DoubleClickMs = 400L

@Composable
private fun TitleButton(icon: ImageVector, label: String, danger: Boolean = false, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(
        when {
            hovered && danger -> colors.error
            hovered -> colors.surfaceContainerHighest
            else -> Color.Transparent
        },
    )
    val content by animateColorAsState(if (hovered && danger) colors.onError else colors.onSurfaceVariant)
    Box(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .size(36.dp)
            .clip(CircleShape)
            .background(container)
            .hoverable(interaction)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Release) onClick()
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = content, modifier = Modifier.size(18.dp))
    }
}

// Invisible handles on the edges and corners: a window without the system
// frame can't be resized otherwise.
@Composable
fun FrameWindowScope.ResizeHandles(state: WindowState) {
    if (state.placement != WindowPlacement.Floating) return
    Box(Modifier.fillMaxSize()) {
        val window = window
        Handle(window, Alignment.TopCenter, Modifier.fillMaxWidth().height(Edge), Cursor.N_RESIZE_CURSOR, top = true)
        Handle(window, Alignment.BottomCenter, Modifier.fillMaxWidth().height(Edge), Cursor.S_RESIZE_CURSOR, bottom = true)
        Handle(window, Alignment.CenterStart, Modifier.fillMaxHeight().width(Edge), Cursor.W_RESIZE_CURSOR, left = true)
        Handle(window, Alignment.CenterEnd, Modifier.fillMaxHeight().width(Edge), Cursor.E_RESIZE_CURSOR, right = true)
        Handle(window, Alignment.TopStart, Modifier.size(Corner), Cursor.NW_RESIZE_CURSOR, top = true, left = true)
        Handle(window, Alignment.TopEnd, Modifier.size(Corner), Cursor.NE_RESIZE_CURSOR, top = true, right = true)
        Handle(window, Alignment.BottomStart, Modifier.size(Corner), Cursor.SW_RESIZE_CURSOR, bottom = true, left = true)
        Handle(window, Alignment.BottomEnd, Modifier.size(Corner), Cursor.SE_RESIZE_CURSOR, bottom = true, right = true)
    }
}

private val Edge = 5.dp
private val Corner = 12.dp

private class Drag(var bounds: Rectangle = Rectangle(), var mouse: java.awt.Point = java.awt.Point())

@Composable
private fun BoxScope.Handle(
    window: java.awt.Window,
    alignment: Alignment,
    modifier: Modifier,
    cursor: Int,
    top: Boolean = false,
    bottom: Boolean = false,
    left: Boolean = false,
    right: Boolean = false,
) {
    // Screen coordinates, so the handle moving with the window doesn't skew the drag.
    val start = remember { Drag() }
    Box(
        Modifier
            .align(alignment)
            .then(modifier)
            .pointerHoverIcon(PointerIcon(Cursor(cursor)))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val mouse = MouseInfo.getPointerInfo()?.location ?: continue
                        when (event.type) {
                            PointerEventType.Press -> {
                                start.bounds = window.bounds
                                start.mouse = mouse
                            }
                            PointerEventType.Move -> if (event.buttons.isPrimaryPressed) {
                                val from = start.bounds
                                val origin = start.mouse
                                val dx = mouse.x - origin.x
                                val dy = mouse.y - origin.y
                                val min = window.minimumSize
                                var x = from.x
                                var y = from.y
                                var width = from.width
                                var height = from.height
                                if (right) width = (from.width + dx).coerceAtLeast(min.width)
                                if (bottom) height = (from.height + dy).coerceAtLeast(min.height)
                                if (left) {
                                    width = (from.width - dx).coerceAtLeast(min.width)
                                    x = from.x + from.width - width
                                }
                                if (top) {
                                    height = (from.height - dy).coerceAtLeast(min.height)
                                    y = from.y + from.height - height
                                }
                                window.setBounds(x, y, width, height)
                            }
                        }
                    }
                }
            },
    )
}

