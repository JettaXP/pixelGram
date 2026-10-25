package com.pixelgram.desktop.ui.main

import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.roundToInt
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.materialkolor.quantize.QuantizerCelebi
import com.materialkolor.score.Score
import com.pixelgram.desktop.core.AppLog
import com.pixelgram.desktop.core.AppPaths
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.telegram.ChatState
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.components.loadImage
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import it.tdlight.jni.TdApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Surface
import java.io.File
import org.jetbrains.skia.Image as SkiaImage

object Wallpapers {
    private val dir: File get() = File(AppPaths.dataDir, "wallpapers")

    suspend fun pick(): File? = runCatching {
        FileKit.openFilePicker(
            type = FileKitType.Image,
            dialogSettings = FileKitDialogSettings(title = "Choose a wallpaper"),
        )?.file
    }.onFailure { AppLog.write("Wallpaper picker failed", it) }.getOrNull()

    fun recent(): List<File> = dir.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() }.orEmpty()

    suspend fun install(settings: AppSettings, source: File) {
        val target = withContext(Dispatchers.IO) {
            dir.mkdirs()
            File(dir, "wallpaper-${System.currentTimeMillis()}.${source.extension.ifBlank { "jpg" }}").also { source.copyTo(it) }
                .also { recent().drop(MaxRecent).forEach(File::delete) }
        }
        use(settings, target)
    }

    suspend fun use(settings: AppSettings, file: File) {
        file.setLastModified(System.currentTimeMillis())
        settings.wallpaper.value = file.absolutePath
        if (settings.wallpaperColors.value) applyColors(settings)
    }

    fun remove(settings: AppSettings) {
        settings.wallpaper.value = ""
    }

    suspend fun applyColors(settings: AppSettings) {
        val path = settings.wallpaper.value.takeIf { it.isNotBlank() } ?: return
        seedFrom(path)?.let { settings.seedColor.value = it }
    }

    suspend fun seedFrom(path: String): Int? = withContext(Dispatchers.Default) {
        runCatching {
            val image = SkiaImage.makeFromEncoded(File(path).readBytes())
            val scale = minOf(1.0, kotlin.math.sqrt(SampleArea.toDouble() / (image.width.toDouble() * image.height)))
            val width = (image.width * scale).roundToInt().coerceAtLeast(1)
            val height = (image.height * scale).roundToInt().coerceAtLeast(1)
            val surface = Surface.makeRasterN32Premul(width, height)
            surface.canvas.drawImageRect(
                image,
                Rect.makeWH(image.width.toFloat(), image.height.toFloat()),
                Rect.makeWH(width.toFloat(), height.toFloat()),
                SamplingMode.DEFAULT,
                null,
                true,
            )
            val bitmap = Bitmap()
            bitmap.allocPixels(ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL))
            surface.readPixels(bitmap, 0, 0)
            val bytes = bitmap.readPixels() ?: return@runCatching null
            val pixels = IntArray(width * height) { i ->
                val b = bytes[i * 4].toInt() and 0xFF
                val g = bytes[i * 4 + 1].toInt() and 0xFF
                val r = bytes[i * 4 + 2].toInt() and 0xFF
                (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
            image.close()
            surface.close()
            bitmap.close()
            Score.score(QuantizerCelebi.quantize(pixels, MaxColors)).firstOrNull()
        }.onFailure { AppLog.write("Wallpaper colors failed", it) }.getOrNull()
    }

    private const val SampleArea = 112 * 112
    private const val MaxRecent = 8
    private const val MaxColors = 128
}

@Composable
fun ChatWallpaper(chat: ChatState, telegram: Telegram, settings: AppSettings, dark: Boolean, modifier: Modifier = Modifier) {
    val own = chat.background?.background
    val dim = (settings.wallpaperDim.value / 100f).coerceIn(0f, 0.9f)
    Box(modifier.fillMaxSize().graphicsLayer()) {
        when {
            own != null -> TelegramBackground(own, telegram, dark, chat.background?.darkThemeDimming ?: 0)
            settings.wallpaper.value.isNotBlank() -> {
                val image by produceState<ImageBitmap?>(null, settings.wallpaper.value) {
                    value = loadImage(settings.wallpaper.value, maxSize = WallpaperMaxSize)
                }
                image?.let {
                    Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (dark) dim else dim / 2)))
                }
            }
        }
    }
}

@Composable
private fun TelegramBackground(background: TdApi.Background, telegram: Telegram, dark: Boolean, darkDimming: Int) {
    when (val type = background.type) {
        is TdApi.BackgroundTypeFill -> Box(Modifier.fillMaxSize().background(fillBrush(type.fill)))
        is TdApi.BackgroundTypePattern -> Box(Modifier.fillMaxSize().background(fillBrush(type.fill)).alpha(if (dark) 0.6f else 1f))
        is TdApi.BackgroundTypeWallpaper -> {
            val file = background.document?.document
            LaunchedEffect(file?.id) { telegram.download(file) }
            val path = telegram.localPath(file)
            val image by produceState<ImageBitmap?>(null, path) { value = path?.let { loadImage(it, maxSize = WallpaperMaxSize) } }
            image?.let {
                Image(
                    it,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().then(if (type.isBlurred) Modifier.blur(12.dp) else Modifier),
                )
            }
        }
        else -> Unit
    }
    if (dark && darkDimming > 0) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = darkDimming / 100f)))
}

private fun fillBrush(fill: TdApi.BackgroundFill): Brush = when (fill) {
    is TdApi.BackgroundFillSolid -> Brush.linearGradient(listOf(rgb(fill.color), rgb(fill.color)))
    is TdApi.BackgroundFillGradient -> Brush.verticalGradient(listOf(rgb(fill.topColor), rgb(fill.bottomColor)))
    is TdApi.BackgroundFillFreeformGradient -> Brush.linearGradient(fill.colors.map(::rgb))
    else -> Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
}

private fun rgb(color: Int) = Color(0xFF000000.toInt() or color)

private const val WallpaperMaxSize = 2048
