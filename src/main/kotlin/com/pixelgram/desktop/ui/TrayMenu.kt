package com.pixelgram.desktop.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
    menuAt?.let { at ->
        TrayMenuWindow(at, settings, onDismiss = { menuAt = null }, onOpen = onOpen, onQuit = onQuit)
    }
}

@Composable
private fun TrayMenuWindow(
    at: Point,
    settings: AppSettings,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onQuit: () -> Unit,
) {
    val screen = remember(at) {
        GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices
            .map { it.defaultConfiguration.bounds }
            .firstOrNull { it.contains(at) }
            ?: GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration.bounds
    }
    val width = MenuWindow.width.value.toInt()
    val height = MenuWindow.height.value.toInt()
    val above = at.y + height > screen.y + screen.height
    val left = at.x + width > screen.x + screen.width
    val x = if (left) at.x - width else at.x
    val y = if (above) at.y - height else at.y
    val shown = remember { MutableTransitionState(false).apply { targetState = true } }
    Window(
        onCloseRequest = onDismiss,
        state = rememberWindowState(position = WindowPosition(x.dp, y.dp), size = MenuWindow),
        title = "pixelGram",
        undecorated = true,
        transparent = true,
        resizable = false,
        alwaysOnTop = true,
    ) {
        DisposableEffect(Unit) {
            val listener = object : WindowAdapter() {
                override fun windowLostFocus(e: WindowEvent) = onDismiss()
            }
            window.addWindowFocusListener(listener)
            window.toFront()
            window.requestFocus()
            onDispose { window.removeWindowFocusListener(listener) }
        }
        PixelTheme(settings) {
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
                    enter = fadeIn() + scaleIn(
                        initialScale = 0.9f,
                        transformOrigin = TransformOrigin(if (left) 1f else 0f, if (above) 1f else 0f),
                    ),
                    exit = fadeOut() + scaleOut(targetScale = 0.9f),
                ) {
                    TrayMenu(
                        settings,
                        onOpen = {
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
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 4.dp,
        modifier = Modifier.width(MenuWidth),
    ) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                themedAppIcon()?.let { Image(it, contentDescription = null, modifier = Modifier.size(28.dp)) }
                Text("pixelGram", style = MaterialTheme.typography.titleMedium)
            }
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            DropdownMenuItem(
                text = { Text("Open pixelGram") },
                leadingIcon = { Icon(Icons.Outlined.OpenInNew, contentDescription = null) },
                onClick = onOpen,
            )
            DropdownMenuItem(
                text = { Text(if (muted) "Enable notifications" else "Disable notifications") },
                leadingIcon = {
                    Icon(if (muted) Icons.Outlined.Notifications else Icons.Outlined.NotificationsOff, contentDescription = null)
                },
                onClick = { settings.notificationsMuted.value = !muted },
            )
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            DropdownMenuItem(
                text = { Text("Quit pixelGram", color = MaterialTheme.colorScheme.error) },
                leadingIcon = {
                    Icon(Icons.AutoMirrored.Outlined.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                },
                onClick = onQuit,
            )
        }
    }
}

private val MenuWidth = 248.dp
private val MenuWindow = DpSize(MenuWidth + 16.dp, 260.dp)
