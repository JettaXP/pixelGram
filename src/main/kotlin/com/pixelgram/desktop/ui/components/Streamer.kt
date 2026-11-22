package com.pixelgram.desktop.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.layout.padding
import com.pixelgram.desktop.core.StreamerMode

@Composable
fun Modifier.streamerBlur(clickable: Boolean = true, enabled: Boolean = StreamerMode.hideNames): Modifier {
    if (!enabled) return this
    var revealed by remember { mutableStateOf(false) }
    val radius by animateDpAsState(if (revealed) 0.dp else 8.dp, Motion.fastSpatial())
    val blurred = this
        .layout { measurable, constraints ->
            val pad = BlurRoom.roundToPx()
            val inner = constraints.copy(
                maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth + pad * 2 else constraints.maxWidth,
                maxHeight = if (constraints.hasBoundedHeight) constraints.maxHeight + pad * 2 else constraints.maxHeight,
            )
            val placeable = measurable.measure(inner)
            layout((placeable.width - pad * 2).coerceAtLeast(0), (placeable.height - pad * 2).coerceAtLeast(0)) { placeable.place(-pad, -pad) }
        }
        .blur(radius, BlurredEdgeTreatment.Unbounded)
        .padding(BlurRoom)
    return if (clickable) blurred.pointerInput(Unit) { detectTapGestures { revealed = !revealed } } else blurred
}

private val BlurRoom = 8.dp
