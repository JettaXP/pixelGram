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

    private fun themed(scheme: ColorScheme): BufferedImage? =
        if (EasterEgg.israel) image(IsraelWhite, IsraelBlue) else image(scheme.primaryContainer, scheme.onPrimaryContainer)

    fun painter(scheme: ColorScheme): Painter? =
        themed(scheme)?.let { BitmapPainter(it.toComposeImageBitmap()) }

    fun writeTo(file: File, scheme: ColorScheme) {
        val image = themed(scheme) ?: return
        runCatching { ImageIO.write(image, "png", file) }
    }

    private val sizeCache = HashMap<BufferedImage, List<BufferedImage>>()

    @Synchronized
    fun monochromeSizes(): List<BufferedImage> {
        val image = image(Color.White, Color.Black) ?: return emptyList()
        return sizeCache.getOrPut(image) { SystemSizes.map { scaled(image, it) } }
    }

    @Synchronized
    fun sizes(scheme: ColorScheme): List<BufferedImage> {
        val image = themed(scheme) ?: return emptyList()
        return sizeCache.getOrPut(image) { SystemSizes.map { scaled(image, it) } }
    }

    fun installForSystem(scheme: ColorScheme) {
        if (AppPaths.isWindows) installForWindows(scheme)
        if (!AppPaths.isLinux) return
        val image = themed(scheme) ?: return
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

    private fun installForWindows(scheme: ColorScheme) {
        val exe = System.getProperty("jpackage.app-path")?.let { File(it).absoluteFile } ?: return
        val image = themed(scheme) ?: return
        val key = if (EasterEgg.israel) "israel" else "%08x-%08x".format(scheme.primaryContainer.toArgb(), scheme.onPrimaryContainer.toArgb())
        val dir = File(AppPaths.dataDir, "icons").apply { mkdirs() }
        val ico = File(dir, "pixelgram-$key.ico")
        runCatching {
            if (!ico.exists()) writeIco(image, ico)
            dir.listFiles()?.filter { it.extension == "ico" && it != ico }?.forEach(File::delete)
            val script = WindowsShortcutScript
                .replace("%EXE%", exe.path.replace("'", "''"))
                .replace("%ICO%", ico.path.replace("'", "''"))
            val encoded = java.util.Base64.getEncoder().encodeToString(script.toByteArray(Charsets.UTF_16LE))
            ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-EncodedCommand", encoded)
                .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start()
                .waitFor()
        }.onFailure { com.pixelgram.desktop.core.AppLog.write("Windows icon update failed", it) }
    }

    private fun writeIco(image: BufferedImage, file: File) {
        val images = IcoSizes.map { size ->
            java.io.ByteArrayOutputStream().also { ImageIO.write(scaled(image, size), "png", it) }.toByteArray() to size
        }
        val out = java.io.ByteArrayOutputStream()
        fun short(value: Int) { out.write(value and 0xFF); out.write(value shr 8 and 0xFF) }
        fun int(value: Int) { short(value and 0xFFFF); short(value ushr 16) }
        short(0)
        short(1)
        short(images.size)
        var offset = 6 + 16 * images.size
        images.forEach { (bytes, size) ->
            out.write(if (size >= 256) 0 else size)
            out.write(if (size >= 256) 0 else size)
            out.write(0)
            out.write(0)
            short(1)
            short(32)
            int(bytes.size)
            int(offset)
            offset += bytes.size
        }
        images.forEach { (bytes, _) -> out.write(bytes) }
        file.writeBytes(out.toByteArray())
    }

    private fun scaled(image: BufferedImage, size: Int): BufferedImage {
        var current = image
        while (current.width / 2 >= size) current = draw(current, current.width / 2)
        return if (current.width == size) current else draw(current, size)
    }

    private fun draw(image: BufferedImage, size: Int): BufferedImage {
        val result = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val graphics = result.createGraphics()
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        graphics.drawImage(image, 0, 0, size, size, null)
        graphics.dispose()
        return result
    }

    private val SystemSizes = listOf(16, 24, 32, 48, 64, 128, 256, 512)
    private val IcoSizes = listOf(16, 20, 24, 32, 40, 48, 64, 256)
    private val WindowsShortcutScript = """
        ${'$'}shell = New-Object -ComObject WScript.Shell
        ${'$'}exe = '%EXE%'
        ${'$'}ico = '%ICO%'
        ${'$'}roots = @(
            "${'$'}env:APPDATA\Microsoft\Internet Explorer\Quick Launch\User Pinned\TaskBar",
            "${'$'}env:APPDATA\Microsoft\Internet Explorer\Quick Launch\User Pinned\StartMenu",
            "${'$'}env:APPDATA\Microsoft\Windows\Start Menu\Programs",
            [Environment]::GetFolderPath('Desktop')
        )
        foreach (${'$'}root in ${'$'}roots) {
            if (-not (Test-Path ${'$'}root)) { continue }
            Get-ChildItem -Path ${'$'}root -Filter *.lnk -Recurse -ErrorAction SilentlyContinue | ForEach-Object {
                try {
                    ${'$'}link = ${'$'}shell.CreateShortcut(${'$'}_.FullName)
                    if (${'$'}link.TargetPath -ieq ${'$'}exe) {
                        ${'$'}link.IconLocation = "${'$'}ico,0"
                        ${'$'}link.Save()
                    }
                } catch {}
            }
        }
        Start-Process -FilePath "${'$'}env:SystemRoot\System32\ie4uinit.exe" -ArgumentList '-show' -WindowStyle Hidden -ErrorAction SilentlyContinue
    """.trimIndent()
    private const val MaskBackground = 74f
}

@Composable
fun themedAppIcon(): Painter? {
    val scheme = MaterialTheme.colorScheme
    val israel = EasterEgg.israel
    return remember(scheme.primaryContainer, scheme.onPrimaryContainer, israel) { AppIcons.painter(scheme) }
}
