package com.pixelgram.desktop.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

internal fun symbol(name: String, path: String, mirror: Boolean = false): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, Viewport, Viewport, autoMirror = mirror)
        .addPath(addPathNodes(path), fill = SolidColor(Color.Black))
        .build()

private const val Viewport = 960f

object Icons {
    object Filled
    object Outlined
    object Rounded
    val Default = Filled

    object AutoMirrored {
        object Filled
        object Outlined
        object Rounded
    }
}
