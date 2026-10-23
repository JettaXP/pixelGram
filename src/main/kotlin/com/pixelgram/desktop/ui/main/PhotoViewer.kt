package com.pixelgram.desktop.ui.main

import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.unit.IntOffset
import com.pixelgram.desktop.telegram.UiMessage
import kotlinx.coroutines.delay
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
    val chatId: Long = 0,
    val messageId: Long = 0,
)

private fun UiMessage.toViewed(telegram: Telegram): ViewedPhoto? =
    photo?.let { ViewedPhoto(telegram, it, fullPhoto ?: it, text, chatId, id) }

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
    val telegram = photo.telegram
    val photos = remember(photo) {
        if (photo.chatId == 0L) emptyList() else telegram.messages(photo.chatId).filter { it.photo != null }.reversed()
    }
    var index by remember(photo) { mutableIntStateOf(photos.indexOfFirst { it.id == photo.messageId && it.chatId == photo.chatId }) }
    var direction by remember { mutableIntStateOf(1) }
    val current = photos.getOrNull(index)?.toViewed(telegram) ?: photo
    var controls by remember { mutableStateOf(true) }
    var moves by remember { mutableIntStateOf(0) }
    var lastWheel by remember { mutableLongStateOf(0L) }
    LaunchedEffect(moves, index) {
        controls = true
        delay(IdleHideMs)
        controls = false
    }
    val go: (Int) -> Unit = { step ->
        val target = index + step
        if (index >= 0 && target in photos.indices) {
            direction = step
            index = target
        }
    }
    Window(
        onCloseRequest = close,
        state = rememberWindowState(
            position = WindowPosition(screen.x.dp, screen.y.dp),
            size = DpSize(screen.width.dp, screen.height.dp),
        ),
        title = "pixelGram photo",
        undecorated = true,
        transparent = false,
        resizable = false,
        alwaysOnTop = true,
        onKeyEvent = { event ->
            when {
                event.type != KeyEventType.KeyDown -> false
                event.key == Key.Escape -> {
                    close()
                    true
                }
                event.key == Key.DirectionLeft -> {
                    go(-1)
                    true
                }
                event.key == Key.DirectionRight -> {
                    go(1)
                    true
                }
                else -> false
            }
        },
    ) {
        PixelTheme(settings) {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
            AnimatedVisibility(shown, enter = fadeIn(), exit = fadeOut()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.9f))
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    if (event.type == PointerEventType.Move || event.type == PointerEventType.Enter) {
                                        controls = true
                                        moves++
                                    }
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.type != PointerEventType.Scroll) continue
                                    val change = event.changes.first()
                                    if (change.isConsumed) continue
                                    val now = System.currentTimeMillis()
                                    if (now - lastWheel < WheelStepMs) continue
                                    lastWheel = now
                                    go(if (change.scrollDelta.y + change.scrollDelta.x > 0) 1 else -1)
                                }
                            }
                        }
                        .pointerInput(Unit) { detectTapGestures(onTap = { close() }) },
                    contentAlignment = Alignment.Center,
                ) {
                    val slide = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
                    val fade = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
                    AnimatedContent(
                        targetState = current,
                        contentKey = { it.messageId to it.preview.id },
                        transitionSpec = {
                            (slideInHorizontally(slide) { it / SlideDivisor * direction } + fadeIn(fade)) togetherWith
                                (slideOutHorizontally(slide) { -it / SlideDivisor * direction } + fadeOut(fade))
                        },
                        modifier = Modifier.fillMaxSize().animateEnterExit(
                            enter = scaleIn(initialScale = 0.92f),
                            exit = scaleOut(targetScale = 0.92f),
                        ),
                        contentAlignment = Alignment.Center,
                    ) { viewed ->
                        ZoomablePhoto(viewed, Modifier.fillMaxSize())
                    }
                    FilledTonalIconButton(
                        onClick = close,
                        modifier = Modifier.align(Alignment.TopEnd).padding(24.dp),
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                    val chrome by animateFloatAsState(
                        if (controls) 1f else 0f,
                        tween(if (controls) ShowMs else HideFadeMs),
                    )
                    NavArrow(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous photo", index > 0, chrome, Modifier.align(Alignment.CenterStart)) { go(-1) }
                    NavArrow(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next photo", index in 0 until photos.lastIndex, chrome, Modifier.align(Alignment.CenterEnd)) { go(1) }
                    AnimatedContent(
                        targetState = current.caption,
                        transitionSpec = { fadeIn(fade) togetherWith fadeOut(fade) },
                        modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp).graphicsLayer { alpha = chrome },
                    ) { caption ->
                        if (caption.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = CaptionColor.copy(alpha = CaptionAlpha),
                                contentColor = Color.White,
                                modifier = Modifier.widthIn(max = 720.dp),
                            ) {
                                EmojiText(
                                    caption,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier
                                        .heightIn(max = CaptionMaxHeight)
                                        .verticalScroll(rememberScrollState())
                                        .padding(horizontal = 20.dp, vertical = 14.dp),
                                )
                            }
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun NavArrow(icon: ImageVector, label: String, visible: Boolean, chrome: Float, modifier: Modifier, onClick: () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.8f),
        exit = fadeOut() + scaleOut(targetScale = 0.8f),
        modifier = modifier.padding(horizontal = 24.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .graphicsLayer { alpha = chrome }
                .size(ArrowSize)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = ArrowAlpha))
                .clickable(enabled = chrome > 0f, onClick = onClick),
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(32.dp))
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
                                if (!event.keyboardModifiers.isCtrlPressed && zoom <= 1f) continue
                                val change = event.changes.first()
                                change.consume()
                                val delta = change.scrollDelta.y
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
private const val IdleHideMs = 2_000L
private const val ShowMs = 120
private const val HideFadeMs = 400
private const val WheelStepMs = 250L
private const val SlideDivisor = 5
private const val CaptionAlpha = 0.72f
private val CaptionColor = Color(0xFF2B2B2E)
private const val ArrowAlpha = 0.16f
private val ArrowSize = 56.dp
private val CaptionMaxHeight = 220.dp
private const val MaxZoom = 6f
