package com.pixelgram.desktop.ui.main

import java.awt.dnd.DropTargetDropEvent
import java.awt.dnd.DropTargetDragEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.outlined.Image
import com.pixelgram.desktop.ui.icons.outlined.InsertDriveFile
import com.pixelgram.desktop.ui.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.io.File
import java.net.URI

private enum class DropZone(val title: String, val subtitle: String, val icon: ImageVector) {
    Files("Drop files here", "to send them as documents", Icons.Outlined.InsertDriveFile),
    Images("Drop images here", "to send them without compression", Icons.Outlined.Image),
    Photos("Drop photos here", "to send them in a quick way", Icons.Outlined.PhotoLibrary),
}

@OptIn(ExperimentalComposeUiApi::class)
private fun DragAndDropEvent.at(density: Float): Offset? {
    val point = when (val native = nativeEvent) {
        is DropTargetDragEvent -> native.location
        is DropTargetDropEvent -> native.location
        else -> null
    } ?: return null
    return Offset(point.x * density, point.y * density)
}

@OptIn(ExperimentalComposeUiApi::class)
private fun DragAndDropEvent.files(): List<File>? = runCatching {
    (dragData() as? DragData.FilesList)?.readFiles()?.mapNotNull { uri ->
        runCatching { File(URI(uri)) }.getOrNull()?.takeIf { it.isFile }
    }
}.getOrNull()

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun FileDropArea(
    onDrop: (files: List<File>, asDocuments: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var dragging by remember { mutableStateOf(false) }
    var images by remember { mutableStateOf(false) }
    var hovered by remember { mutableStateOf<DropZone?>(null) }
    val bounds = remember { mutableStateMapOf<DropZone, Rect>() }
    val density = LocalDensity.current.density
    val scale = remember { floatArrayOf(1f) }
    scale[0] = density
    val zones = if (images) listOf(DropZone.Images, DropZone.Photos) else listOf(DropZone.Files)
    val target = remember {
        object : DragAndDropTarget {
            override fun onStarted(event: DragAndDropEvent) {
                images = event.files()?.let { it.isNotEmpty() && it.all(Attachments::isPhoto) } ?: false
                dragging = true
            }

            override fun onEntered(event: DragAndDropEvent) {
                dragging = true
            }

            override fun onMoved(event: DragAndDropEvent) {
                val at = event.at(scale[0]) ?: return
                hovered = bounds.entries.firstOrNull { it.value.contains(at) }?.key
            }

            override fun onExited(event: DragAndDropEvent) {
                dragging = false
                hovered = null
            }

            override fun onEnded(event: DragAndDropEvent) {
                dragging = false
                hovered = null
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                val at = event.at(scale[0])
                val zone = at?.let { point -> bounds.entries.firstOrNull { it.value.contains(point) }?.key } ?: hovered ?: DropZone.Files
                val files = event.files().orEmpty()
                dragging = false
                hovered = null
                if (files.isEmpty()) return false
                onDrop(files, zone != DropZone.Photos)
                return true
            }
        }
    }
    Box(
        modifier
            .fillMaxSize()
            .dragAndDropTarget(shouldStartDragAndDrop = { it.dragData() is DragData.FilesList }, target = target),
    ) {
        content()
        AnimatedVisibility(
            visible = dragging,
            enter = fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()),
            exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
        ) {
            Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f), modifier = Modifier.fillMaxSize()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    zones.forEach { zone ->
                        DropZoneCard(
                            zone,
                            hovered == zone,
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .onGloballyPositioned { bounds[zone] = it.boundsInRoot() },
                        )
                    }
                }
            }
        }
    }
    if (!dragging) bounds.keys.retainAll(zones.toSet())
}

@Composable
private fun DropZoneCard(zone: DropZone, active: Boolean, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(
        if (active) colors.primaryContainer else colors.surfaceContainerHigh,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
    )
    val outline by animateColorAsState(
        if (active) colors.primary else colors.outlineVariant,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
    )
    val scale by animateFloatAsState(if (active) 1.12f else 1f, MaterialTheme.motionScheme.fastSpatialSpec())
    val spin by animateFloatAsState(if (active) 90f else 0f, MaterialTheme.motionScheme.defaultSpatialSpec())
    val iconShape = MaterialShapes.Cookie9Sided.toShape()
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = container,
        border = BorderStroke(2.dp, outline),
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            modifier = Modifier.fillMaxSize().padding(24.dp),
        ) {
            AnimatedVisibility(true, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                Box(
                    Modifier
                        .size(88.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            rotationZ = spin
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        shape = iconShape,
                        color = if (active) colors.primary else colors.secondaryContainer,
                        modifier = Modifier.size(88.dp),
                    ) {}
                    Icon(
                        zone.icon,
                        contentDescription = null,
                        tint = if (active) colors.onPrimary else colors.onSecondaryContainer,
                        modifier = Modifier.size(40.dp).graphicsLayer { rotationZ = -spin },
                    )
                }
            }
            Text(
                zone.title,
                style = MaterialTheme.typography.headlineSmall,
                color = if (active) colors.onPrimaryContainer else colors.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                zone.subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = if (active) colors.onPrimaryContainer else colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
