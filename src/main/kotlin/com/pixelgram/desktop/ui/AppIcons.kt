package com.pixelgram.desktop.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image as SkiaImage

// App icon variants: lime for light themes, green for dark ones.
object AppIcons {
    val light: Painter? by lazy { load("icon-light.png") }
    val dark: Painter? by lazy { load("icon-dark.png") }

    private fun load(name: String): Painter? = AppIcons::class.java.getResourceAsStream("/$name")
        ?.use { BitmapPainter(SkiaImage.makeFromEncoded(it.readBytes()).toComposeImageBitmap()) }

    fun forTheme(dark: Boolean): Painter? = if (dark) this.dark else light
}

// Icon matching the current color scheme.
@Composable
fun themedAppIcon(): Painter? = AppIcons.forTheme(MaterialTheme.colorScheme.surface.luminance() < 0.5f)
