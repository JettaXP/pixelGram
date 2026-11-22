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
import com.pixelgram.desktop.core.StreamerMode

@Composable
fun Modifier.streamerBlur(clickable: Boolean = true): Modifier {
    if (!StreamerMode.hideNames) return this
    var revealed by remember { mutableStateOf(false) }
    val radius by animateDpAsState(if (revealed) 0.dp else 8.dp, Motion.fastSpatial())
    val blurred = this.blur(radius, BlurredEdgeTreatment.Unbounded)
    return if (clickable) blurred.pointerInput(Unit) { detectTapGestures { revealed = !revealed } } else blurred
}
