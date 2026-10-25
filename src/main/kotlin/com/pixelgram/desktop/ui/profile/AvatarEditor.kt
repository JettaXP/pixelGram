package com.pixelgram.desktop.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.outlined.AddAPhoto
import com.pixelgram.desktop.ui.icons.outlined.ContentPaste
import com.pixelgram.desktop.ui.icons.outlined.PhotoCamera
import com.pixelgram.desktop.ui.icons.outlined.PhotoLibrary
import com.pixelgram.desktop.ui.components.expressive.BasicAlertDialog
import com.pixelgram.desktop.ui.components.expressive.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.pixelgram.desktop.ui.components.expressive.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.CameraSession
import com.pixelgram.desktop.ui.main.Attachments
import kotlinx.coroutines.launch
import java.io.File

private val VideoExtensions = setOf("mp4", "mov", "webm", "mkv", "gif")
private val CameraWidth = 520.dp

fun isVideoFile(file: File) = file.extension.lowercase() in VideoExtensions

@Composable
fun AvatarEditor(size: Dp, onPicked: (File) -> Unit, modifier: Modifier = Modifier, avatar: @Composable () -> Unit) {
    val hover = remember { MutableInteractionSource() }
    val hovered by hover.collectIsHoveredAsState()
    var menu by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Box(modifier.size(size).hoverable(hover), contentAlignment = Alignment.Center) {
        avatar()
        AnimatedVisibility(
            visible = hovered || menu,
            enter = slideInVertically(MaterialTheme.motionScheme.fastSpatialSpec()) { it } + fadeIn(),
            exit = slideOutVertically(MaterialTheme.motionScheme.fastSpatialSpec()) { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
        ) {
            Box {
                Surface(
                    onClick = { menu = true },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shadowElevation = 3.dp,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Icon(Icons.Outlined.AddAPhoto, contentDescription = "Set new photo", modifier = Modifier.size(20.dp))
                    }
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, shape = RoundedCornerShape(16.dp)) {
                    DropdownMenuItem(
                        text = { Text("Photo or video") },
                        leadingIcon = { Icon(Icons.Outlined.PhotoLibrary, contentDescription = null) },
                        onClick = {
                            menu = false
                            scope.launch { Attachments.pick().firstOrNull()?.let(onPicked) }
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Camera") },
                        leadingIcon = { Icon(Icons.Outlined.PhotoCamera, contentDescription = null) },
                        onClick = {
                            menu = false
                            camera = true
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("From clipboard") },
                        leadingIcon = { Icon(Icons.Outlined.ContentPaste, contentDescription = null) },
                        onClick = {
                            menu = false
                            Attachments.fromClipboard().firstOrNull()?.let(onPicked)
                        },
                    )
                }
            }
        }
    }
    if (camera) {
        CameraDialog(onDismiss = { camera = false }) { file ->
            camera = false
            onPicked(file)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CameraDialog(onDismiss: () -> Unit, onCaptured: (File) -> Unit) {
    var frame by remember { mutableStateOf<ImageBitmap?>(null) }
    var failed by remember { mutableStateOf(false) }
    val session = remember { CameraSession.open(com.pixelgram.desktop.core.AudioDevices.settings?.cameraDevice?.value.orEmpty()) }
    DisposableEffect(session) {
        if (session == null) {
            failed = true
        } else {
            Thread {
                runCatching { session.run { image -> frame = image.toComposeImageBitmap() } }.onFailure { failed = true }
            }.apply { isDaemon = true }.start()
        }
        onDispose { session?.stop() }
    }
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(Modifier.width(CameraWidth).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Camera", style = MaterialTheme.typography.headlineSmall)
                Box(
                    Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center,
                ) {
                    val bitmap = frame
                    when {
                        bitmap != null -> Image(bitmap, contentDescription = "Camera", contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
                        failed -> Text("No camera found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        else -> LoadingIndicator()
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.align(Alignment.End)) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = { session?.capture()?.let(onCaptured) }, enabled = frame != null) {
                        Icon(Icons.Outlined.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Take photo", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}
