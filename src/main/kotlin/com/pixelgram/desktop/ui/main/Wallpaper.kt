package com.pixelgram.desktop.ui.main

import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.pixelgram.desktop.telegram.chatBackground
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
            type = FileKitType.File(ImageExtensions + VideoExtensions),
            dialogSettings = FileKitDialogSettings(title = "Choose a wallpaper"),
        )?.file
    }.onFailure { AppLog.write("Wallpaper picker failed", it) }.getOrNull()

    fun recent(): List<File> = dir.listFiles()
        ?.filter { it.isFile && !it.name.endsWith(PosterSuffix) && !it.name.endsWith(".part") }
        ?.sortedByDescending { it.lastModified() }
        .orEmpty()

    fun isVideo(path: String): Boolean = path.substringAfterLast('.', "").lowercase() in VideoExtensions

    fun poster(path: String): String = if (isVideo(path)) "$path$PosterSuffix" else path

    private fun ensurePoster(path: String): String? {
        if (!isVideo(path)) return path
        val target = File(poster(path))
        if (target.isFile) return target.absolutePath
        return runCatching {
            val grabber = org.bytedeco.javacv.FFmpegFrameGrabber(path)
            grabber.start()
            val frame = grabber.grabImage()
            val image = frame?.let { org.bytedeco.javacv.Java2DFrameConverter().convert(it) }
            grabber.stop()
            grabber.release()
            image?.let { javax.imageio.ImageIO.write(it, "png", target) }
            target.takeIf { it.isFile }?.absolutePath
        }.onFailure { AppLog.write("Wallpaper poster failed", it) }.getOrNull()
    }

    suspend fun install(settings: AppSettings, source: File) {
        val target = withContext(Dispatchers.IO) {
            dir.mkdirs()
            File(dir, "wallpaper-${System.currentTimeMillis()}.${source.extension.ifBlank { "jpg" }.lowercase()}").also { source.copyTo(it) }
                .also { ensurePoster(it.absolutePath) }
                .also { recent().drop(MaxRecent).forEach { old -> File(poster(old.absolutePath)).delete(); old.delete() } }
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

    suspend fun seedFrom(source: String): Int? = withContext(Dispatchers.Default) {
        runCatching {
            val path = withContext(Dispatchers.IO) { ensurePoster(source) } ?: return@runCatching null
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
    private const val PosterSuffix = ".poster.png"
    private val ImageExtensions = listOf("png", "jpg", "jpeg", "webp", "bmp")
    private val VideoExtensions = listOf("mp4", "webm", "mov", "mkv", "m4v", "gif")
    private const val MaxRecent = 8
    private const val MaxColors = 128
}

@Composable
fun ChatWallpaper(chat: ChatState, telegram: Telegram, settings: AppSettings, dark: Boolean, modifier: Modifier = Modifier) {
    val chatBackground = telegram.chatBackground(chat, dark)
    val own = chatBackground?.background
    val dim = (settings.wallpaperDim.value / 100f).coerceIn(0f, 0.9f)
    Box(modifier.fillMaxSize().graphicsLayer()) {
        when {
            own != null -> TelegramBackground(own, telegram, dark, chatBackground.darkThemeDimming)
            settings.wallpaper.value.isNotBlank() -> {
                val path = settings.wallpaper.value
                val source by produceState<ImageBitmap?>(null, path) {
                    value = loadImage(Wallpapers.poster(path), maxSize = WallpaperMaxSize)
                }
                androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
                    val width = constraints.maxWidth
                    val height = constraints.maxHeight
                    val fitted by produceState<ImageBitmap?>(null, source, width, height) {
                        val original = source ?: return@produceState
                        kotlinx.coroutines.delay(ResizeSettleMs)
                        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { cropTo(original, width, height) }
                    }
                    val shown = fitted?.takeIf { it.width == width && it.height == height }
                    when {
                        shown != null -> Image(shown, contentDescription = null, contentScale = ContentScale.None, modifier = Modifier.fillMaxSize())
                        source != null -> Image(source!!, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    }
                }
                if (Wallpapers.isVideo(path)) InlineVideo(path, Modifier.fillMaxSize(), maxWidth = LiveWallpaperWidth)
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (dark) dim else dim / 2)))
            }
        }
    }
}

@Composable
private fun TelegramBackground(background: TdApi.Background, telegram: Telegram, dark: Boolean, darkDimming: Int) {
    when (val type = background.type) {
        is TdApi.BackgroundTypeFill, is TdApi.BackgroundTypePattern -> {
            val file = (type as? TdApi.BackgroundTypePattern)?.let { background.document?.document }
            LaunchedEffect(file?.id) { telegram.download(file) }
            val pattern = telegram.localPath(file)
            val fill = (type as? TdApi.BackgroundTypeFill)?.fill ?: (type as TdApi.BackgroundTypePattern).fill
            Box(Modifier.fillMaxSize().background(fillBrush(fill)))
            androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
                val width = constraints.maxWidth
                val height = constraints.maxHeight
                val rendered by produceState<ImageBitmap?>(null, background.id, type, pattern, width, height) {
                    if (width <= 0 || height <= 0) return@produceState
                    kotlinx.coroutines.delay(ResizeSettleMs)
                    value = withContext(Dispatchers.Default) {
                        runCatching { renderBackground(type, pattern?.let(::File), width, height) }
                            .onFailure { AppLog.write("Background render failed", it) }.getOrNull()
                    }
                }
                rendered?.let { Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
            }
        }
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

internal fun fillBrush(fill: TdApi.BackgroundFill): Brush = when (fill) {
    is TdApi.BackgroundFillSolid -> Brush.linearGradient(listOf(rgb(fill.color), rgb(fill.color)))
    is TdApi.BackgroundFillGradient -> Brush.verticalGradient(listOf(rgb(fill.topColor), rgb(fill.bottomColor)))
    is TdApi.BackgroundFillFreeformGradient -> Brush.linearGradient(fill.colors.map(::rgb))
    else -> Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
}

private val FreeformPoints = listOf(Offset(0.80f, 0.10f), Offset(0.60f, 0.85f), Offset(0.35f, 0.25f), Offset(0.25f, 0.60f))
private const val FreeformRadius = 0.75f
private const val LightPatternAlpha = 0.45f

private fun renderBackground(type: TdApi.BackgroundType, pattern: File?, width: Int, height: Int): ImageBitmap {
    val bitmap = ImageBitmap(width, height)
    val canvas = androidx.compose.ui.graphics.Canvas(bitmap)
    val size = Size(width.toFloat(), height.toFloat())
    val bounds = androidx.compose.ui.geometry.Rect(Offset.Zero, size)
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, size) {
        when (type) {
            is TdApi.BackgroundTypeFill -> drawFill(type.fill)
            is TdApi.BackgroundTypePattern -> {
                val inverted = type.isInverted || type.intensity < 0
                val alpha = kotlin.math.abs(type.intensity) / 100f
                val svg = pattern?.takeIf { it.isFile }?.let(::loadPattern)
                if (inverted) drawRect(Color.Black) else drawFill(type.fill)
                if (svg != null) {
                    svg.use { dom ->
                        canvas.saveLayer(bounds, Paint().apply { this.alpha = if (inverted) alpha else alpha * LightPatternAlpha })
                        drawPattern(dom)
                        if (inverted) {
                            canvas.saveLayer(bounds, Paint().apply { blendMode = BlendMode.SrcIn })
                            drawFill(type.fill)
                            canvas.restore()
                        }
                        canvas.restore()
                    }
                }
            }
            else -> Unit
        }
    }
    return bitmap
}

private fun DrawScope.drawFill(fill: TdApi.BackgroundFill) {
    if (fill !is TdApi.BackgroundFillFreeformGradient || fill.colors.size < 3) {
        drawRect(fillBrush(fill))
        return
    }
    val colors = fill.colors.map(::rgb)
    drawRect(Color(colors.map { it.red }.average().toFloat(), colors.map { it.green }.average().toFloat(), colors.map { it.blue }.average().toFloat()))
    val radius = size.maxDimension * FreeformRadius
    colors.forEachIndexed { index, color ->
        val point = FreeformPoints[index % FreeformPoints.size]
        val center = Offset(point.x * size.width, point.y * size.height)
        drawRect(Brush.radialGradient(listOf(color, color.copy(alpha = 0f)), center, radius))
    }
}

private fun loadPattern(file: File): org.jetbrains.skia.svg.SVGDOM? {
    val raw = file.readBytes()
    val bytes = if (raw.size > 2 && raw[0] == 0x1f.toByte() && raw[1] == 0x8b.toByte()) {
        java.util.zip.GZIPInputStream(raw.inputStream()).use { it.readBytes() }
    } else {
        raw
    }
    return org.jetbrains.skia.Data.makeFromBytes(bytes).use { org.jetbrains.skia.svg.SVGDOM(it) }
}

private fun DrawScope.drawPattern(dom: org.jetbrains.skia.svg.SVGDOM) {
    val root = dom.root ?: return
    val box = root.viewBox
    val patternWidth = box?.width ?: root.width.value
    val patternHeight = box?.height ?: root.height.value
    if (patternWidth <= 0f || patternHeight <= 0f) return
    val scale = maxOf(size.width / patternWidth, size.height / patternHeight)
    val native = drawContext.canvas.nativeCanvas
    native.save()
    native.translate((size.width - patternWidth * scale) / 2f, (size.height - patternHeight * scale) / 2f)
    native.scale(scale, scale)
    dom.setContainerSize(patternWidth, patternHeight)
    dom.render(native)
    native.restore()
}

internal fun rgb(color: Int) = Color(0xFF000000.toInt() or color)

private const val WallpaperMaxSize = 2048

private const val LiveWallpaperWidth = 1920

private const val ResizeSettleMs = 150L

private fun cropTo(image: ImageBitmap, width: Int, height: Int): ImageBitmap? {
    if (width <= 0 || height <= 0) return null
    return org.jetbrains.skia.Image.makeFromBitmap(image.asSkiaBitmap()).use { skia -> cropSkia(skia, width, height) }
}

private fun cropSkia(skia: org.jetbrains.skia.Image, width: Int, height: Int): ImageBitmap {
    val scale = maxOf(width / skia.width.toFloat(), height / skia.height.toFloat())
    val srcWidth = width / scale
    val srcHeight = height / scale
    val left = (skia.width - srcWidth) / 2f
    val top = (skia.height - srcHeight) / 2f
    return org.jetbrains.skia.Surface.makeRasterN32Premul(width, height).use { surface -> drawCrop(surface, skia, left, top, srcWidth, srcHeight, width, height) }
}

private fun drawCrop(surface: org.jetbrains.skia.Surface, skia: org.jetbrains.skia.Image, left: Float, top: Float, srcWidth: Float, srcHeight: Float, width: Int, height: Int): ImageBitmap {
    surface.canvas.drawImageRect(
        skia,
        org.jetbrains.skia.Rect.makeXYWH(left, top, srcWidth, srcHeight),
        org.jetbrains.skia.Rect.makeWH(width.toFloat(), height.toFloat()),
        org.jetbrains.skia.SamplingMode.LINEAR,
        null,
        true,
    )
    return surface.makeImageSnapshot().use { it.toComposeImageBitmap() }
}
