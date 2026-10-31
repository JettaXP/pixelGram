package com.pixelgram.desktop.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
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
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.filled.WindowClose
import com.pixelgram.desktop.ui.icons.filled.WindowRestore
import com.pixelgram.desktop.ui.icons.filled.WindowMaximize
import com.pixelgram.desktop.ui.icons.filled.WindowMinimize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.pixelgram.desktop.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
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
import com.pixelgram.desktop.core.AppPaths
import com.pixelgram.desktop.core.WindowsCorners
import java.awt.Cursor
import java.awt.Frame
import java.awt.MouseInfo
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.Window
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WindowChrome(private val window: Window, private val scope: CoroutineScope) {
    var maximized by mutableStateOf(false)
        private set

    private var shaped = false

    fun updateCorners() {
        val compose = window as? ComposeWindow ?: return
        if (shaped || !WindowsCorners.apply(compose.windowHandle, round = !maximized)) shapeCorners(compose)
    }

    private fun shapeCorners(frame: ComposeWindow) {
        if (AppPaths.isMac || !frame.isUndecorated) return
        val device = frame.graphicsConfiguration?.device ?: return
        if (!device.isWindowTranslucencySupported(java.awt.GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSPARENT)) return
        shaped = true
        val round = !maximized && frame.extendedState and Frame.MAXIMIZED_BOTH == 0
        val radius = ShapeRadius
        runCatching {
            frame.shape = if (round) java.awt.geom.RoundRectangle2D.Double(0.0, 0.0, frame.width.toDouble(), frame.height.toDouble(), radius, radius) else null
        }.onFailure { com.pixelgram.desktop.core.AppLog.write("Window shape failed", it) }
    }
    private var restoreBounds: Rectangle? = null
    private var nativeRestore: Rectangle? = null
    private var job: Job? = null

    private val handle: Long get() = (window as? ComposeWindow)?.windowHandle ?: 0L

    private fun workArea(): Rectangle {
        val config = window.graphicsConfiguration
        val insets = Toolkit.getDefaultToolkit().getScreenInsets(config)
        val screen = config.bounds
        return Rectangle(
            screen.x + insets.left,
            screen.y + insets.top,
            screen.width - insets.left - insets.right,
            screen.height - insets.top - insets.bottom,
        )
    }

    fun toggleMaximized() {
        val current = WindowsCorners.windowRect(handle)
        val work = WindowsCorners.workArea(handle)
        if (current != null && work != null) {
            if (maximized) {
                maximized = false
                nativeRestore?.let { animateNative(current, it) }
            } else {
                nativeRestore = current
                maximized = true
                animateNative(current, work)
            }
        } else if (maximized) {
            maximized = false
            restoreBounds?.let { animateTo(it) }
        } else {
            restoreBounds = window.bounds
            maximized = true
            animateTo(workArea())
        }
        updateCorners()
    }

    private fun animateNative(from: Rectangle, target: Rectangle) {
        swap { WindowsCorners.setRect(handle, target) }
    }

    private fun animateTo(target: Rectangle) {
        swap { window.bounds = target }
    }

    private fun swap(resize: () -> Unit) {
        job?.cancel()
        job = scope.launch {
            fade(window.opacity, SwapOpacity, SwapOutMs)
            resize()
            delay(SwapSettleMs)
            updateCorners()
            fade(window.opacity, 1f, SwapInMs)
        }
    }

    fun minimize() {
        job?.cancel()
        job = scope.launch {
            fade(1f, 0f)
            (window as? Frame)?.extendedState = Frame.ICONIFIED
        }
    }

    fun restored() {
        job?.cancel()
        job = scope.launch {
            fade(window.opacity, 1f)
            updateCorners()
        }
    }

    private suspend fun fade(from: Float, to: Float, duration: Int = FadeMs) {
        runCatching {
            animate(from, to, animationSpec = tween(duration, easing = FastOutSlowInEasing)) { value, _ ->
                window.opacity = value.coerceIn(0f, 1f)
            }
        }
    }
}

private const val ShapeRadius = 16.0
private const val FadeMs = 160
private const val SwapOutMs = 70
private const val SwapInMs = 140
private const val SwapSettleMs = 40L
private const val SwapOpacity = 0.15f

@Composable
fun FrameWindowScope.rememberWindowChrome(): WindowChrome {
    val scope = rememberCoroutineScope()
    val chrome = remember { WindowChrome(window, scope) }
    LaunchedEffect(window) { chrome.updateCorners() }
    DisposableEffect(window) {
        val listener = object : WindowAdapter() {
            override fun windowDeiconified(e: WindowEvent) = chrome.restored()
            override fun windowOpened(e: WindowEvent) = chrome.updateCorners()
            override fun windowActivated(e: WindowEvent) = chrome.updateCorners()
        }
        val resize = object : java.awt.event.ComponentAdapter() {
            override fun componentResized(e: java.awt.event.ComponentEvent) = chrome.updateCorners()
        }
        window.addWindowListener(listener)
        window.addComponentListener(resize)
        onDispose {
            window.removeWindowListener(listener)
            window.removeComponentListener(resize)
        }
    }
    return chrome
}

@Composable
fun FrameWindowScope.MacTitleBar() {
    Row(
        modifier = Modifier.fillMaxWidth().height(MacTitleBarHeight).background(MaterialTheme.colorScheme.surfaceContainer),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WindowDraggableArea(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxSize().padding(start = 80.dp, end = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                themedAppIcon()?.let { Image(it, contentDescription = null, modifier = Modifier.size(20.dp)) }
                Text(
                    com.pixelgram.desktop.core.WindowTitle.text(),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
fun FrameWindowScope.TitleBar(chrome: WindowChrome, onClose: () -> Unit, title: String? = null) {
    val lastClick = remember { longArrayOf(0L) }
    Row(
        modifier = Modifier.fillMaxWidth().height(TitleBarHeight).background(MaterialTheme.colorScheme.surfaceContainer),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WindowDraggableArea(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            if (awaitPointerEvent().type != PointerEventType.Release) continue
                            val now = System.currentTimeMillis()
                            if (now - lastClick[0] < DoubleClickMs) chrome.toggleMaximized()
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
                Text(
                    title ?: com.pixelgram.desktop.core.WindowTitle.text(),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        TitleButton(Icons.Filled.WindowMinimize, "Minimize", onClick = chrome::minimize)
        TitleButton(
            if (chrome.maximized) Icons.Filled.WindowRestore else Icons.Filled.WindowMaximize,
            if (chrome.maximized) "Restore" else "Maximize",
            onClick = chrome::toggleMaximized,
        )
        TitleButton(Icons.Filled.WindowClose, "Close", danger = true, onClick = onClose)
        Spacer(Modifier.width(6.dp))
    }
}

val TitleBarHeight = 40.dp
private val MacTitleBarHeight = 28.dp
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

@Composable
fun FrameWindowScope.ResizeHandles(chrome: WindowChrome) {
    if (chrome.maximized) return
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

