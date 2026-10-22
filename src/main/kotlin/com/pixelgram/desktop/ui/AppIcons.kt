package com.pixelgram.desktop.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

// The app icon recolored to the current scheme, so a purple theme gets a purple icon.
object AppIcons {
    // icon-dark.png is a dark green square with a white glyph: how white a pixel
    // is says how much of the glyph color it gets, its alpha is kept as is.
    private val mask: BufferedImage? by lazy {
        AppIcons::class.java.getResourceAsStream("/icon-dark.png")?.use { ImageIO.read(it) }
    }
    private val cache = HashMap<Pair<Int, Int>, BufferedImage>()

    @Synchronized
    fun image(background: Color, glyph: Color): BufferedImage? {
        val source = mask ?: return null
        return cache.getOrPut(background.toArgb() to glyph.toArgb()) {
            val bg = background.toArgb()
            val fg = glyph.toArgb()
            val result = BufferedImage(source.width, source.height, BufferedImage.TYPE_INT_ARGB)
            for (y in 0 until source.height) {
                for (x in 0 until source.width) {
                    val pixel = source.getRGB(x, y)
                    val alpha = pixel ushr 24
                    if (alpha == 0) continue
                    val gray = ((pixel shr 16 and 0xFF) + (pixel shr 8 and 0xFF) + (pixel and 0xFF)) / 3f
                    val t = ((gray - MaskBackground) / (255f - MaskBackground)).coerceIn(0f, 1f)
                    fun mix(shift: Int) = ((bg shr shift and 0xFF) * (1 - t) + (fg shr shift and 0xFF) * t).toInt()
                    result.setRGB(x, y, (alpha shl 24) or (mix(16) shl 16) or (mix(8) shl 8) or mix(0))
                }
            }
            result
        }
    }

    fun painter(scheme: ColorScheme): Painter? =
        image(scheme.primaryContainer, scheme.onPrimaryContainer)?.let { BitmapPainter(it.toComposeImageBitmap()) }

    // A PNG of the icon for places that want a file, like system notifications.
    fun writeTo(file: File, scheme: ColorScheme) {
        val image = image(scheme.primaryContainer, scheme.onPrimaryContainer) ?: return
        runCatching { ImageIO.write(image, "png", file) }
    }

    private const val MaskBackground = 74f
}

// Icon matching the current color scheme.
@Composable
fun themedAppIcon(): Painter? {
    val scheme = MaterialTheme.colorScheme
    return remember(scheme.primaryContainer, scheme.onPrimaryContainer) { AppIcons.painter(scheme) }
}
