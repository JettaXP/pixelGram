package com.pixelgram.desktop.ui.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Morph
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.pixelgram.desktop.calls.ActiveCall
import com.pixelgram.desktop.calls.CallDevice
import com.pixelgram.desktop.calls.CallLibrary
import com.pixelgram.desktop.calls.CallPhase
import com.pixelgram.desktop.calls.Calls
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.ui.components.AvatarImage
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.components.expressive.FilledIconButton
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.filled.Call
import com.pixelgram.desktop.ui.icons.filled.CallEnd
import com.pixelgram.desktop.ui.icons.filled.Mic
import com.pixelgram.desktop.ui.icons.filled.MicOff
import com.pixelgram.desktop.ui.icons.filled.ScreenShare
import com.pixelgram.desktop.ui.icons.filled.StopScreenShare
import com.pixelgram.desktop.ui.icons.filled.Videocam
import com.pixelgram.desktop.ui.icons.filled.VideocamOff
import com.pixelgram.desktop.ui.theme.EmojiText
import com.pixelgram.desktop.ui.theme.PixelTheme
import kotlinx.coroutines.delay

private val WindowSize = DpSize(420.dp, 640.dp)
private val AvatarSize = 160.dp
private val ControlSize = 64.dp
private val EndSize = 72.dp
private val PreviewWidth = 120.dp
private const val PreviewRatio = 3f / 4f
private const val TickMs = 1000L

@Composable
fun CallWindow(settings: AppSettings, icon: androidx.compose.ui.graphics.painter.Painter?) {
    val call = Calls.current ?: return
    val telegram = call.telegram
    val user = telegram.users[call.userId]
    val name = listOfNotNull(user?.firstName, user?.lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Call" }
    val state = rememberWindowState(size = WindowSize, position = WindowPosition(Alignment.Center))
    Window(
        onCloseRequest = Calls::hangUp,
        title = name,
        icon = icon,
        state = state,
        alwaysOnTop = call.phase == CallPhase.Incoming,
    ) {
        PixelTheme(settings) {
            CallContent(call, name)
        }
    }
}

@Composable
private fun CallContent(call: ActiveCall, name: String) {
    val colors = MaterialTheme.colorScheme
    val telegram = call.telegram
    val user = telegram.users[call.userId]
    Box(Modifier.fillMaxSize().background(colors.surfaceContainerLowest)) {
        call.remoteFrame?.takeIf { call.remoteVideo }?.let { frame ->
            Image(frame, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 56.dp),
        ) {
            if (!call.remoteVideo) {
                val pulse = rememberInfiniteTransition()
                val ringing = call.phase == CallPhase.Ringing || call.phase == CallPhase.Incoming
                val scale by pulse.animateFloat(1f, if (ringing) 1.08f else 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse))
                val turn by pulse.animateFloat(0f, if (ringing) 360f else 0f, infiniteRepeatable(tween(12_000)))
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .size(AvatarSize + 28.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                rotationZ = turn
                            }
                            .background(colors.primaryContainer, MaterialShapes.Cookie12Sided.toShape()),
                    )
                    AvatarImage(name, user?.profilePhoto?.small, telegram, MaterialShapes.Cookie9Sided.toShape(), AvatarSize, full = user?.profilePhoto?.big)
                }
            }
            EmojiText(name, style = MaterialTheme.typography.headlineSmall, color = if (call.remoteVideo) Color.White else colors.onSurface)
            StatusLine(call)
            if (call.emojis.isNotEmpty() && call.phase == CallPhase.Active) {
                EmojiText(call.emojis.joinToString(" "), style = MaterialTheme.typography.titleLarge)
            }
            if (CallLibrary.downloading) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(240.dp)) {
                    Text("Downloading call components…", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    LinearProgressIndicator(progress = { CallLibrary.progress }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                }
            }
        }
        call.localFrame?.let { frame ->
            Image(
                frame,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .width(PreviewWidth)
                    .graphicsLayer {
                        scaleX = if (call.screen == null) -1f else 1f
                    }
                    .clip(RoundedCornerShape(16.dp))
            )
        }
        Controls(call, Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp))
    }
}

@Composable
private fun StatusLine(call: ActiveCall) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(call.phase) {
        while (call.phase == CallPhase.Active) {
            now = System.currentTimeMillis()
            delay(TickMs)
        }
    }
    val text = when (call.phase) {
        CallPhase.Preparing -> if (call.outgoing) "Requesting…" else "Connecting…"
        CallPhase.Ringing -> "Ringing…"
        CallPhase.Incoming -> if (call.isVideo) "Incoming video call" else "Incoming call"
        CallPhase.Exchanging -> "Exchanging encryption keys…"
        CallPhase.Connecting -> "Connecting…"
        CallPhase.Active -> {
            val seconds = ((now - call.startedAt).coerceAtLeast(0) / 1000).toInt()
            val clock = if (seconds >= 3600) "%d:%02d:%02d".format(seconds / 3600, seconds / 60 % 60, seconds % 60) else "%d:%02d".format(seconds / 60, seconds % 60)
            if (call.remoteMuted) "$clock · muted" else clock
        }
        CallPhase.Ended -> call.status ?: "Call ended"
    }
    Text(
        call.status?.takeIf { call.phase != CallPhase.Ended && it.isNotBlank() }?.let { "$text · $it" } ?: text,
        style = MaterialTheme.typography.bodyLarge,
        color = if (call.remoteVideo) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun Controls(call: ActiveCall, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    AnimatedVisibility(call.phase != CallPhase.Ended, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        if (call.phase == CallPhase.Incoming) {
            Row(horizontalArrangement = Arrangement.spacedBy(48.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundAction(Icons.Filled.CallEnd, "Decline", EndSize, colors.error, colors.onError, onClick = Calls::decline)
                RoundAction(Icons.Filled.Call, "Accept", EndSize, colors.primary, colors.onPrimary, onClick = Calls::accept)
            }
            return@AnimatedVisibility
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Toggle(if (call.muted) Icons.Filled.MicOff else Icons.Filled.Mic, if (call.muted) "Unmute" else "Mute", active = !call.muted, onClick = Calls::toggleMute)
            Toggle(if (call.camera) Icons.Filled.Videocam else Icons.Filled.VideocamOff, if (call.camera) "Turn camera off" else "Turn camera on", active = call.camera, onClick = Calls::toggleCamera)
            ScreenButton(call)
            RoundAction(Icons.Filled.CallEnd, "End call", EndSize, colors.error, colors.onError, onClick = Calls::hangUp)
        }
    }
}

@Composable
private fun ScreenButton(call: ActiveCall) {
    var screens by remember { mutableStateOf<List<CallDevice>?>(null) }
    Box {
        Toggle(
            if (call.screen != null) Icons.Filled.StopScreenShare else Icons.Filled.ScreenShare,
            if (call.screen != null) "Stop sharing" else "Share screen",
            active = call.screen != null,
        ) {
            if (call.screen != null) Calls.share(null) else screens = Calls.screens()
        }
        DropdownMenu(expanded = screens != null, onDismissRequest = { screens = null }) {
            val list = screens.orEmpty()
            if (list.isEmpty()) {
                DropdownMenuItem(text = { Text("Nothing to share") }, onClick = { screens = null }, enabled = false)
            }
            list.forEach { device ->
                DropdownMenuItem(
                    text = { EmojiText(device.name, maxLines = 1) },
                    leadingIcon = { Icon(Icons.Filled.ScreenShare, contentDescription = null) },
                    onClick = {
                        screens = null
                        Calls.share(device)
                    },
                )
            }
        }
    }
}

@Composable
private fun Toggle(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(if (active) colors.secondaryContainer else colors.surfaceContainerHighest)
    val content by animateColorAsState(if (active) colors.onSecondaryContainer else colors.onSurfaceVariant)
    RoundAction(icon, label, ControlSize, container, content, active, onClick)
}

@Composable
private fun RoundAction(icon: ImageVector, label: String, size: Dp, container: Color, content: Color, active: Boolean = true, onClick: () -> Unit) {
    val source = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val spatial = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    val shape by animateFloatAsState(if (active) 1f else 0f, MaterialTheme.motionScheme.defaultSpatialSpec())
    val press by animateFloatAsState(if (pressed) 1f else 0f, spatial)
    val morph = remember { Morph(MaterialShapes.Circle, MaterialShapes.Cookie9Sided) }
    val outline = remember(shape) { MorphShape(morph, shape) }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                val scale = 1f - press * PressShrink
                scaleX = scale
                scaleY = scale
                rotationZ = press * PressTurn
            }
            .clip(outline)
            .background(container)
            .focusProperties { canFocus = false }
            .clickable(interactionSource = source, indication = ripple(color = content), role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(size * 0.42f).graphicsLayer { rotationZ = -press * PressTurn },
        )
    }
}

private class MorphShape(private val morph: Morph, private val progress: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        var first = true
        morph.forEachCubic(progress) { cubic ->
            if (first) {
                path.moveTo(cubic.anchor0X * size.width, cubic.anchor0Y * size.height)
                first = false
            }
            path.cubicTo(
                cubic.control0X * size.width, cubic.control0Y * size.height,
                cubic.control1X * size.width, cubic.control1Y * size.height,
                cubic.anchor1X * size.width, cubic.anchor1Y * size.height,
            )
        }
        path.close()
        return Outline.Generic(path)
    }
}

private const val PressShrink = 0.1f
private const val PressTurn = 25f
