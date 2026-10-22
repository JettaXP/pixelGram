package com.pixelgram.desktop.ui.main

import kotlinx.coroutines.launch
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.components.loadImage
import com.pixelgram.desktop.ui.theme.EmojiText
import com.pixelgram.desktop.ui.theme.PixelTheme
import it.tdlight.jni.TdApi
import java.awt.GraphicsEnvironment

class ViewedPhoto(
    val telegram: Telegram,
    val preview: TdApi.File,
    val full: TdApi.File?,
    val caption: String,
)

object PhotoViewer {
    var current by mutableStateOf<ViewedPhoto?>(null)
}

@Composable
fun PhotoViewerWindow(photo: ViewedPhoto, settings: AppSettings) {
    val shown = remember(photo) { MutableTransitionState(false).apply { targetState = true } }
    val close = { shown.targetState = false }
    LaunchedEffect(shown.isIdle, shown.currentState) {
        if (shown.isIdle && !shown.currentState && PhotoViewer.current === photo) PhotoViewer.current = null
    }
    val screen = remember { GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration.bounds }
    Window(
        onCloseRequest = close,
        state = rememberWindowState(
            position = WindowPosition(screen.x.dp, screen.y.dp),
            size = DpSize(screen.width.dp, screen.height.dp),
        ),
        title = "pixelGram photo",
        undecorated = true,
        transparent = true,
        resizable = false,
        alwaysOnTop = true,
        onKeyEvent = { event ->
            if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                close()
                true
            } else {
                false
            }
        },
    ) {
        PixelTheme(settings) {
            AnimatedVisibility(shown, enter = fadeIn(), exit = fadeOut()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.9f))
                        .pointerInput(Unit) { detectTapGestures(onTap = { close() }) },
                    contentAlignment = Alignment.Center,
                ) {
                    ZoomablePhoto(photo, Modifier.align(Alignment.Center).animateEnterExit(
                        enter = scaleIn(initialScale = 0.92f),
                        exit = scaleOut(targetScale = 0.92f),
                    ))
                    FilledTonalIconButton(
                        onClick = close,
                        modifier = Modifier.align(Alignment.TopEnd).padding(24.dp),
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                    if (photo.caption.isNotBlank()) {
                        EmojiText(
                            photo.caption,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp).widthIn(max = 720.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoomablePhoto(photo: ViewedPhoto, modifier: Modifier) {
    val telegram = photo.telegram
    LaunchedEffect(photo.full?.id) { telegram.download(photo.full) }
    val previewPath = telegram.localPath(photo.preview)
    val fullPath = telegram.localPath(photo.full)
    val preview by produceState<ImageBitmap?>(null, previewPath) {
        previewPath?.let { loadImage(it, maxSize = 1024) }?.let { value = it }
    }
    val full by produceState<ImageBitmap?>(null, fullPath) {
        fullPath?.let { loadImage(it, maxSize = FullSize) }?.let { value = it }
    }
    var menuAt by remember { mutableStateOf<DpOffset?>(null) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var zoom by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val shownZoom by animateFloatAsState(zoom)
    Box(
        modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 72.dp)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                            val change = event.changes.first()
                            change.consume()
                            menuAt = with(density) { DpOffset(change.position.x.toDp(), change.position.y.toDp()) }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = full ?: preview
        if (bitmap != null) {
            Image(
                bitmap,
                contentDescription = "Photo",
                contentScale = ContentScale.Fit,
                filterQuality = FilterQuality.High,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = shownZoom
                        scaleY = shownZoom
                        translationX = offset.x
                        translationY = offset.y
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = {
                            zoom = if (zoom > 1f) 1f else 2.5f
                            offset = Offset.Zero
                        })
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            if (zoom > 1f) offset += drag
                        }
                    }
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.type != PointerEventType.Scroll) continue
                                val delta = event.changes.first().scrollDelta.y
                                zoom = (zoom * if (delta < 0) 1.15f else 1 / 1.15f).coerceIn(1f, MaxZoom)
                                if (zoom == 1f) offset = Offset.Zero
                            }
                        }
                    },
            )
        }
        if (full == null) LoadingIndicator(Modifier.align(Alignment.Center))
        menuAt?.let { at ->
            val path = fullPath ?: previewPath
            Box(Modifier.align(Alignment.TopStart).offset(at.x, at.y)) {
                DropdownMenu(
                    expanded = true,
                    onDismissRequest = { menuAt = null },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    DropdownMenuItem(
                        text = { Text("Copy photo") },
                        leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                        enabled = path != null,
                        onClick = {
                            menuAt = null
                            path?.let(Attachments::copyImage)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Save as…") },
                        leadingIcon = { Icon(Icons.Outlined.Download, contentDescription = null) },
                        enabled = path != null,
                        onClick = {
                            menuAt = null
                            path?.let { scope.launch { Attachments.saveImage(it) } }
                        },
                    )
                }
            }
        }
    }
}

private const val FullSize = 4096
private const val MaxZoom = 6f
