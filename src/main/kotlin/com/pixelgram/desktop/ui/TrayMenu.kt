package com.pixelgram.desktop.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.expandVertically
import androidx.compose.runtime.withFrameNanos
import com.pixelgram.desktop.ui.components.MenuEntry
import com.pixelgram.desktop.ui.components.MenuGroups
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.automirrored.outlined.ExitToApp
import com.pixelgram.desktop.ui.icons.outlined.Notifications
import com.pixelgram.desktop.ui.icons.outlined.NotificationsOff
import com.pixelgram.desktop.ui.icons.outlined.OpenInNew
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.pixelgram.desktop.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.ui.theme.PixelTheme
import java.awt.EventQueue
import java.awt.GraphicsEnvironment
import java.awt.MouseInfo
import java.awt.Rectangle
import java.awt.Toolkit
import kotlinx.coroutines.delay
import java.awt.Point
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.image.BufferedImage

@Composable
fun PixelTray(images: List<BufferedImage>, settings: AppSettings, onOpen: () -> Unit, onQuit: () -> Unit) {
    var menuAt by remember { mutableStateOf<Point?>(null) }
    val open by rememberUpdatedState(onOpen)
    val trayIcon = remember {
        TrayIcon(BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), "pixelGram").apply {
            isImageAutoSize = true
            addMouseListener(object : MouseAdapter() {
                override fun mouseReleased(e: MouseEvent) {
                    if (e.button == MouseEvent.BUTTON3 || e.isPopupTrigger) {
                        val at = e.locationOnScreen
                        EventQueue.invokeLater { menuAt = at }
                    } else if (e.button == MouseEvent.BUTTON1) {
                        EventQueue.invokeLater { open() }
                    }
                }
            })
        }
    }
    LaunchedEffect(images) {
        val size = runCatching { SystemTray.getSystemTray().trayIconSize.width }.getOrDefault(16)
        images.firstOrNull { it.width >= size * 2 }?.let { trayIcon.image = it }
            ?: images.lastOrNull()?.let { trayIcon.image = it }
    }
    DisposableEffect(Unit) {
        runCatching { SystemTray.getSystemTray().add(trayIcon) }
        onDispose { runCatching { SystemTray.getSystemTray().remove(trayIcon) } }
    }
    TrayMenuWindow(menuAt, settings, onDismiss = { menuAt = null }, onOpen = onOpen, onQuit = onQuit)
}

@Composable
private fun TrayMenuWindow(
    at: Point?,
    settings: AppSettings,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onQuit: () -> Unit,
) {
    val width = MenuWindow.width.value.toInt()
    val height = MenuWindow.height.value.toInt()
    val state = rememberWindowState(position = WindowPosition(0.dp, 0.dp), size = MenuWindow)
    var place by remember { mutableStateOf<TrayPlace?>(null) }
    val shown = remember { MutableTransitionState(false) }
    var closing by remember { mutableStateOf(false) }
    val close = {
        if (!closing && shown.targetState) {
            closing = true
            shown.targetState = false
        }
    }
    if (at != null) {
        val next = remember(at) { trayMenuPlace(at, width, height) }
        if (place !== next) {
            place = next
            state.position = WindowPosition(next.x.dp, next.y.dp)
        }
    }
    LaunchedEffect(closing, shown.isIdle) {
        if (closing && shown.isIdle) {
            closing = false
            onDismiss()
        }
    }
    val current = place
    val above = current?.above ?: true
    val left = current?.left ?: true
    Window(
        onCloseRequest = close,
        visible = at != null,
        state = state,
        title = "pixelGram",
        undecorated = true,
        transparent = true,
        resizable = false,
        alwaysOnTop = true,
        focusable = true,
    ) {
        DisposableEffect(Unit) {
            val listener = object : WindowAdapter() {
                override fun windowLostFocus(e: WindowEvent) = close()
            }
            window.addWindowFocusListener(listener)
            onDispose { window.removeWindowFocusListener(listener) }
        }
        LaunchedEffect(at) {
            if (at == null) return@LaunchedEffect
            while (!window.isShowing) delay(ShowPollMs)
            window.toFront()
            window.requestFocus()
            withFrameNanos { }
            shown.targetState = true
        }
        PixelTheme(settings) {
            val motion = MaterialTheme.motionScheme
            val origin = TransformOrigin(if (left) 1f else 0f, if (above) 1f else 0f)
            Box(
                Modifier.fillMaxSize().padding(8.dp),
                contentAlignment = when {
                    above && left -> Alignment.BottomEnd
                    above -> Alignment.BottomStart
                    left -> Alignment.TopEnd
                    else -> Alignment.TopStart
                },
            ) {
                AnimatedVisibility(
                    shown,
                    enter = fadeIn(motion.fastEffectsSpec()) +
                        scaleIn(motion.defaultSpatialSpec(), initialScale = 0.6f, transformOrigin = origin) +
                        expandVertically(motion.defaultSpatialSpec(), expandFrom = if (above) Alignment.Bottom else Alignment.Top),
                    exit = fadeOut(motion.fastEffectsSpec()) +
                        scaleOut(motion.fastSpatialSpec(), targetScale = 0.8f, transformOrigin = origin),
                ) {
                    TrayMenu(
                        settings,
                        onOpen = {
                            shown.targetState = false
                            onDismiss()
                            onOpen()
                        },
                        onQuit = onQuit,
                    )
                }
            }
        }
    }
}

@Composable
private fun TrayMenu(settings: AppSettings, onOpen: () -> Unit, onQuit: () -> Unit) {
    val muted = settings.notificationsMuted.value
    MenuGroups(
        groups = listOf(
            listOf(
                MenuEntry(Icons.Outlined.OpenInNew, "Open pixelGram", onClick = onOpen),
                MenuEntry(
                    if (muted) Icons.Outlined.Notifications else Icons.Outlined.NotificationsOff,
                    if (muted) "Enable notifications" else "Disable notifications",
                ) { settings.notificationsMuted.value = !muted },
            ),
            listOf(MenuEntry(Icons.AutoMirrored.Outlined.ExitToApp, "Quit pixelGram", MaterialTheme.colorScheme.error, onClick = onQuit)),
        ),
        width = MenuWidth,
        leading = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                themedAppIcon()?.let { Image(it, contentDescription = null, modifier = Modifier.size(28.dp)) }
                Text("pixelGram", style = MaterialTheme.typography.titleMedium)
            }
        },
    )
}

private val MenuWidth = 248.dp
private val MenuWindow = DpSize(MenuWidth + 16.dp, 300.dp)

private class TrayPlace(val x: Int, val y: Int, val above: Boolean, val left: Boolean)

private fun trayMenuPlace(click: Point, width: Int, height: Int): TrayPlace {
    val configs = GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices.map { it.defaultConfiguration }
    val pointer = runCatching { MouseInfo.getPointerInfo()?.location }.getOrNull()
    val at = listOfNotNull(pointer, click).firstOrNull { point -> configs.any { it.bounds.contains(point) } }
        ?: configs.firstNotNullOfOrNull { config ->
            val scale = config.defaultTransform.scaleX.takeIf { it > 1.0 } ?: return@firstNotNullOfOrNull null
            Point((click.x / scale).toInt(), (click.y / scale).toInt()).takeIf { config.bounds.contains(it) }
        }
        ?: click
    val config = configs.firstOrNull { it.bounds.contains(at) }
        ?: GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration
    val bounds = config.bounds
    val insets = runCatching { Toolkit.getDefaultToolkit().getScreenInsets(config) }.getOrNull()
    val work = Rectangle(
        bounds.x + (insets?.left ?: 0),
        bounds.y + (insets?.top ?: 0),
        bounds.width - (insets?.left ?: 0) - (insets?.right ?: 0),
        bounds.height - (insets?.top ?: 0) - (insets?.bottom ?: 0),
    )
    val above = at.y > bounds.y + bounds.height / 2
    val left = at.x > bounds.x + bounds.width / 2
    val x = (if (left) at.x - width else at.x).coerceIn(work.x, maxOf(work.x, work.x + work.width - width))
    val y = (if (above) at.y - height else at.y).coerceIn(work.y, maxOf(work.y, work.y + work.height - height))
    return TrayPlace(x, y, above, left)
}

private const val ShowPollMs = 16L
