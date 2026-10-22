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
import com.pixelgram.desktop.core.AppPaths
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

object AppIcons {
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

    fun writeTo(file: File, scheme: ColorScheme) {
        val image = image(scheme.primaryContainer, scheme.onPrimaryContainer) ?: return
        runCatching { ImageIO.write(image, "png", file) }
    }

    fun sizes(scheme: ColorScheme): List<BufferedImage> {
        val image = image(scheme.primaryContainer, scheme.onPrimaryContainer) ?: return emptyList()
        return SystemSizes.map { scaled(image, it) }
    }

    fun installForSystem(scheme: ColorScheme) {
        if (!AppPaths.isLinux) return
        val image = image(scheme.primaryContainer, scheme.onPrimaryContainer) ?: return
        val dataHome = System.getenv("XDG_DATA_HOME")?.takeIf { it.isNotBlank() }
            ?: (System.getProperty("user.home") + "/.local/share")
        val hicolor = File(dataHome, "icons/hicolor")
        runCatching {
            SystemSizes.forEach { size ->
                val dir = File(hicolor, "${size}x$size/apps").apply { mkdirs() }
                ImageIO.write(scaled(image, size), "png", File(dir, "com.pixelgram.desktop.png"))
            }
            hicolor.setLastModified(System.currentTimeMillis())
            ProcessBuilder("gtk-update-icon-cache", "-f", "-t", hicolor.path)
                .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start()
        }
    }

    private fun scaled(image: BufferedImage, size: Int): BufferedImage {
        val result = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val graphics = result.createGraphics()
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        graphics.drawImage(image.getScaledInstance(size, size, java.awt.Image.SCALE_SMOOTH), 0, 0, null)
        graphics.dispose()
        return result
    }

    private val SystemSizes = listOf(16, 24, 32, 48, 64, 128, 256, 512)
    private const val MaskBackground = 74f
}

@Composable
fun themedAppIcon(): Painter? {
    val scheme = MaterialTheme.colorScheme
    return remember(scheme.primaryContainer, scheme.onPrimaryContainer) { AppIcons.painter(scheme) }
}
