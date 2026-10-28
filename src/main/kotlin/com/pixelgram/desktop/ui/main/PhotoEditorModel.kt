package com.pixelgram.desktop.ui.main

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asSkiaBitmap
import com.pixelgram.desktop.core.AppLog
import com.pixelgram.desktop.core.AppPaths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.javacv.FFmpegFrameRecorder
import org.bytedeco.javacv.Frame
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.FilterMipmap
import org.jetbrains.skia.FilterMode
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.MipmapMode
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.PaintStrokeCap
import org.jetbrains.skia.PaintStrokeJoin
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.Point
import org.jetbrains.skia.RRect
import org.jetbrains.skia.Surface
import org.jetbrains.skia.paragraph.FontCollection
import org.jetbrains.skia.paragraph.Paragraph
import org.jetbrains.skia.paragraph.ParagraphBuilder
import org.jetbrains.skia.paragraph.ParagraphStyle
import org.jetbrains.skia.paragraph.Shadow
import org.jetbrains.skia.paragraph.TextStyle
import org.jetbrains.skia.skottie.Animation
import java.io.File
import java.nio.ByteBuffer
import java.util.zip.GZIPInputStream
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import org.jetbrains.skia.Rect as SkRect

internal enum class DrawTool { Pen, Arrow, Marker, Blur, Eraser }

internal enum class ShapeKind(val label: String, val aspect: Float) {
    Circle("Circle", 1f),
    Rectangle("Rectangle", 1.4f),
    Star("Star", 1f),
    Bubble("Bubble", 1.3f),
    Arrow("Arrow", 2.4f),
}

internal enum class CropRatio(val label: String, val value: Float?) {
    Original("Original", null),
    Square("Square", 1f),
    Landscape("3:2", 3f / 2f),
    Wide("16:9", 16f / 9f),
    Portrait("3:4", 3f / 4f),
    Tall("9:16", 9f / 16f),
    Free("Free", null),
}

internal enum class StickerKind { Still, Lottie, Video }

internal class StickerSource(val path: String, val kind: StickerKind, val aspect: Float)

internal class EditStroke(val tool: DrawTool, val color: Int, val width: Float, val points: List<Offset>)

internal enum class PlacedKind { Sticker, Text, Shape }

internal data class Placed(
    val id: Long,
    val kind: PlacedKind,
    val center: Offset,
    val base: Float,
    val scale: Float = 1f,
    val angle: Float = 0f,
    val color: Int = 0,
    val text: String = "",
    val shape: ShapeKind = ShapeKind.Circle,
    val filled: Boolean = false,
    val sticker: StickerSource? = null,
)

internal data class EditState(
    val width: Int,
    val height: Int,
    val crop: Rect,
    val quarter: Int = 0,
    val mirrored: Boolean = false,
    val ratio: CropRatio = CropRatio.Free,
    val strokes: List<EditStroke> = emptyList(),
    val items: List<Placed> = emptyList(),
) {
    val orientedWidth: Int get() = if (quarter % 2 == 0) width else height
    val orientedHeight: Int get() = if (quarter % 2 == 0) height else width
    val bounds: Rect get() = Rect(0f, 0f, orientedWidth.toFloat(), orientedHeight.toFloat())
    val edited: Boolean get() = quarter != 0 || mirrored || crop != bounds || strokes.isNotEmpty() || items.isNotEmpty()
    val animated: Boolean get() = items.any { it.sticker != null && it.sticker.kind != StickerKind.Still }

    fun rotated(): EditState {
        val w = orientedWidth.toFloat()
        fun map(p: Offset) = Offset(p.y, w - p.x)
        val turned = copy(
            quarter = (quarter + if (mirrored) 3 else 1) % 4,
            crop = Rect(crop.top, w - crop.right, crop.bottom, w - crop.left),
            strokes = strokes.map { stroke -> EditStroke(stroke.tool, stroke.color, stroke.width, stroke.points.map(::map)) },
            items = items.map { it.copy(center = map(it.center), angle = it.angle - 90f) },
        )
        return if (ratio.value != null) turned.withRatio(ratio) else turned
    }

    fun flipped(): EditState {
        val w = orientedWidth.toFloat()
        fun map(p: Offset) = Offset(w - p.x, p.y)
        return copy(
            mirrored = !mirrored,
            crop = Rect(w - crop.right, crop.top, w - crop.left, crop.bottom),
            strokes = strokes.map { stroke -> EditStroke(stroke.tool, stroke.color, stroke.width, stroke.points.map(::map)) },
            items = items.map { it.copy(center = map(it.center), angle = -it.angle) },
        )
    }

    fun ratioValue(choice: CropRatio = ratio): Float? = when (choice) {
        CropRatio.Original -> orientedWidth.toFloat() / orientedHeight
        else -> choice.value
    }

    fun withRatio(choice: CropRatio): EditState {
        val value = ratioValue(choice) ?: return copy(ratio = choice)
        val w = minOf(orientedWidth.toFloat(), orientedHeight * value)
        val h = w / value
        val cx = crop.center.x.coerceIn(w / 2, orientedWidth - w / 2)
        val cy = crop.center.y.coerceIn(h / 2, orientedHeight - h / 2)
        return copy(ratio = choice, crop = Rect(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2))
    }
}

internal fun Placed.intrinsic(shaper: TextShaper): Size = when (kind) {
    PlacedKind.Sticker -> Size(base, base / (sticker?.aspect ?: 1f))
    PlacedKind.Shape -> Size(base, base / shape.aspect)
    PlacedKind.Text -> shaper.paragraph(text, base, color).let { Size(it.maxIntrinsicWidth + base * TextPad, it.height) }
}

internal fun Placed.toLocal(point: Offset): Offset {
    val dx = point.x - center.x
    val dy = point.y - center.y
    val radians = -angle * PI.toFloat() / 180f
    return Offset((dx * cos(radians) - dy * sin(radians)) / scale, (dx * sin(radians) + dy * cos(radians)) / scale)
}

internal fun Placed.toImage(local: Offset): Offset {
    val radians = angle * PI.toFloat() / 180f
    val x = local.x * scale
    val y = local.y * scale
    return Offset(center.x + x * cos(radians) - y * sin(radians), center.y + x * sin(radians) + y * cos(radians))
}

internal class TextShaper : AutoCloseable {
    private val fonts = FontCollection().apply { setDefaultFontManager(FontMgr.default) }
    private val cache = LinkedHashMap<String, Paragraph>()

    fun paragraph(text: String, size: Float, color: Int): Paragraph {
        val key = "$size|$color|$text"
        cache[key]?.let { return it }
        val style = TextStyle()
            .setFontSize(size)
            .setColor(color)
            .setFontStyle(FontStyle.BOLD)
            .addShadow(Shadow(ShadowColor, Point(0f, size * ShadowOffset), (size * ShadowBlur).toDouble()))
        val paragraphStyle = ParagraphStyle().apply {
            textStyle = style
            alignment = org.jetbrains.skia.paragraph.Alignment.CENTER
        }
        val built = ParagraphBuilder(paragraphStyle, fonts).pushStyle(style).addText(text.ifEmpty { " " }).build()
        built.layout(Float.POSITIVE_INFINITY)
        built.layout(kotlin.math.ceil(built.maxIntrinsicWidth) + 1f)
        if (cache.size > ShaperCacheSize) cache.remove(cache.keys.first())?.close()
        cache[key] = built
        return built
    }

    override fun close() {
        cache.values.forEach { it.close() }
        cache.clear()
    }
}

internal interface StickerArt : AutoCloseable {
    val durationMs: Long
    fun draw(canvas: Canvas, rect: SkRect, timeMs: Long)
}

private class StillArt(private val image: Image) : StickerArt {
    override val durationMs = 0L
    override fun draw(canvas: Canvas, rect: SkRect, timeMs: Long) {
        canvas.drawImageRect(image, SkRect.makeWH(image.width.toFloat(), image.height.toFloat()), rect, Smooth, null, true)
    }
    override fun close() = image.close()
}

private class LottieArt(private val animation: Animation) : StickerArt {
    override val durationMs = (animation.duration * 1000).toLong().coerceAtLeast(1)
    override fun draw(canvas: Canvas, rect: SkRect, timeMs: Long) {
        synchronized(animation) {
            animation.seekFrameTime((timeMs % durationMs) / 1000f)
            animation.render(canvas, rect)
        }
    }
    override fun close() = animation.close()
}

private class VideoArt(private val frames: List<Image>, private val times: LongArray, override val durationMs: Long) : StickerArt {
    override fun draw(canvas: Canvas, rect: SkRect, timeMs: Long) {
        val at = timeMs % durationMs
        var index = times.indexOfLast { it <= at }
        if (index < 0) index = 0
        val image = frames[index]
        canvas.drawImageRect(image, SkRect.makeWH(image.width.toFloat(), image.height.toFloat()), rect, Smooth, null, true)
    }
    override fun close() = frames.forEach(Image::close)
}

internal suspend fun loadStickerArt(source: StickerSource): StickerArt? = runCatching {
    when (source.kind) {
        StickerKind.Still -> withContext(Dispatchers.IO) { StillArt(Image.makeFromEncoded(File(source.path).readBytes())) }
        StickerKind.Lottie -> withContext(Dispatchers.IO) {
            val json = GZIPInputStream(File(source.path).inputStream()).use { it.readBytes().decodeToString() }
            LottieArt(Animation.makeFromString(json))
        }
        StickerKind.Video -> {
            val clip = VideoStickers.clip(source.path, StickerArtSide) ?: return null
            val frames = clip.frames.map { Image.makeFromBitmap(it.asSkiaBitmap().apply { setImmutable() }) }
            VideoArt(frames, clip.times, clip.durationMs.coerceAtLeast(1))
        }
    }
}.onFailure { AppLog.write("Sticker art failed", it) }.getOrNull()

internal class EditImages(val base: Image, val blurred: Image?)

internal fun blurredCopy(image: Image): Image =
    Surface.makeRasterN32Premul(image.width, image.height).use { surface ->
        val sigma = maxOf(image.width, image.height) * BlurStrength
        val paint = Paint().apply { imageFilter = ImageFilter.makeBlur(sigma, sigma, FilterTileMode.CLAMP, null, null) }
        surface.canvas.drawImage(image, 0f, 0f, paint)
        surface.makeImageSnapshot()
    }

internal fun uprightSize(bytes: ByteArray): Pair<Int, Int> =
    org.jetbrains.skia.Codec.makeFromData(org.jetbrains.skia.Data.makeFromBytes(bytes)).use { codec ->
        if (codec.encodedOrigin.ordinal in 5..8) codec.height to codec.width else codec.width to codec.height
    }

internal fun decodeUpright(bytes: ByteArray, maxSide: Int = 0): Image {
    val orientation = runCatching {
        org.jetbrains.skia.Codec.makeFromData(org.jetbrains.skia.Data.makeFromBytes(bytes)).use { it.encodedOrigin.ordinal }
    }.getOrDefault(1)
    val decoded = Image.makeFromEncoded(bytes)
    val swap = orientation in 5..8
    val fullW = if (swap) decoded.height else decoded.width
    val fullH = if (swap) decoded.width else decoded.height
    val scale = if (maxSide > 0 && maxOf(fullW, fullH) > maxSide) maxSide.toFloat() / maxOf(fullW, fullH) else 1f
    if (orientation <= 1 && scale == 1f) return decoded
    val w = (fullW * scale).roundToInt().coerceAtLeast(1)
    val h = (fullH * scale).roundToInt().coerceAtLeast(1)
    return decoded.use { source ->
        Surface.makeRasterN32Premul(w, h).use { surface ->
            val canvas = surface.canvas
            canvas.scale(w.toFloat() / fullW, h.toFloat() / fullH)
            val sw = source.width.toFloat()
            val sh = source.height.toFloat()
            when (orientation) {
                2 -> canvas.translate(sw, 0f).scale(-1f, 1f)
                3 -> canvas.translate(sw, sh).rotate(180f)
                4 -> canvas.translate(0f, sh).scale(1f, -1f)
                5 -> canvas.rotate(90f).scale(1f, -1f)
                6 -> canvas.translate(sh, 0f).rotate(90f)
                7 -> canvas.translate(sh, sw).rotate(-90f).scale(1f, -1f)
                8 -> canvas.translate(0f, sw).rotate(-90f)
            }
            canvas.drawImageRect(source, SkRect.makeWH(sw, sh), SkRect.makeWH(sw, sh), Smooth, null, true)
            surface.makeImageSnapshot()
        }
    }
}

internal fun Canvas.orient(state: EditState) {
    if (state.mirrored) {
        translate(state.orientedWidth.toFloat(), 0f)
        scale(-1f, 1f)
    }
    val w = state.width.toFloat()
    val h = state.height.toFloat()
    when (state.quarter) {
        1 -> translate(0f, w).rotate(-90f)
        2 -> translate(w, h).rotate(180f)
        3 -> translate(h, 0f).rotate(90f)
    }
}

internal fun strokePath(points: List<Offset>): Path {
    val builder = PathBuilder()
    val first = points.firstOrNull() ?: return builder.detach()
    builder.moveTo(first.x, first.y)
    if (points.size == 1) {
        builder.lineTo(first.x + DotNudge, first.y)
    } else if (points.size == 2) {
        builder.lineTo(points[1].x, points[1].y)
    } else {
        for (index in 1 until points.size - 1) {
            val current = points[index]
            val next = points[index + 1]
            builder.quadTo(current.x, current.y, (current.x + next.x) / 2, (current.y + next.y) / 2)
        }
        val last = points.last()
        builder.lineTo(last.x, last.y)
    }
    return builder.detach()
}

private fun arrowHead(points: List<Offset>, width: Float): Path? {
    val end = points.lastOrNull() ?: return null
    val reach = width * ArrowReach
    val from = points.asReversed().firstOrNull { (it - end).getDistance() >= reach } ?: points.first()
    if ((from - end).getDistance() < 1f) return null
    val direction = atan2(end.y - from.y, end.x - from.x)
    val length = width * ArrowHeadScale
    val builder = PathBuilder()
    for (side in listOf(-1, 1)) {
        val wing = direction + PI.toFloat() + side * ArrowWing
        builder.moveTo(end.x, end.y)
        builder.lineTo(end.x + cos(wing) * length, end.y + sin(wing) * length)
    }
    return builder.detach()
}

private fun strokePaint(stroke: EditStroke): Paint = Paint().apply {
    isAntiAlias = true
    mode = PaintMode.STROKE
    strokeCap = PaintStrokeCap.ROUND
    strokeJoin = PaintStrokeJoin.ROUND
    strokeWidth = stroke.width
    color = stroke.color
    when (stroke.tool) {
        DrawTool.Marker -> {
            strokeWidth = stroke.width * MarkerWidth
            alpha = (MarkerAlpha * 255).roundToInt()
        }
        DrawTool.Eraser -> blendMode = BlendMode.CLEAR
        DrawTool.Blur -> {
            color = 0xFFFFFFFF.toInt()
            strokeWidth = stroke.width * BlurWidth
        }
        else -> Unit
    }
}

internal fun drawEdit(
    canvas: Canvas,
    state: EditState,
    images: EditImages,
    shaper: TextShaper,
    art: (StickerSource) -> StickerArt?,
    timeMs: Long,
    live: EditStroke? = null,
) {
    val full = SkRect.makeWH(state.width.toFloat(), state.height.toFloat())
    val bounds = SkRect.makeWH(state.orientedWidth.toFloat(), state.orientedHeight.toFloat())
    canvas.save()
    canvas.orient(state)
    canvas.drawImageRect(images.base, SkRect.makeWH(images.base.width.toFloat(), images.base.height.toFloat()), full, Smooth, null, true)
    canvas.restore()
    val strokes = if (live != null) state.strokes + live else state.strokes
    val blurs = strokes.filter { it.tool == DrawTool.Blur }
    val blurred = images.blurred
    if (blurs.isNotEmpty() && blurred != null) {
        canvas.saveLayer(bounds, null)
        blurs.forEach { stroke -> strokePath(stroke.points).use { canvas.drawPath(it, strokePaint(stroke)) } }
        canvas.save()
        canvas.orient(state)
        canvas.drawImageRect(
            blurred,
            SkRect.makeWH(blurred.width.toFloat(), blurred.height.toFloat()),
            full,
            Smooth,
            Paint().apply { blendMode = BlendMode.SRC_IN },
            true,
        )
        canvas.restore()
        canvas.restore()
    }
    val drawn = strokes.filter { it.tool != DrawTool.Blur }
    if (drawn.isNotEmpty()) {
        canvas.saveLayer(bounds, null)
        drawn.forEach { stroke ->
            val paint = strokePaint(stroke)
            strokePath(stroke.points).use { canvas.drawPath(it, paint) }
            if (stroke.tool == DrawTool.Arrow) arrowHead(stroke.points, stroke.width)?.use { canvas.drawPath(it, paint) }
        }
        canvas.restore()
    }
    state.items.forEach { item -> drawPlaced(canvas, item, shaper, art, timeMs) }
}

internal fun drawPlaced(canvas: Canvas, item: Placed, shaper: TextShaper, art: (StickerSource) -> StickerArt?, timeMs: Long) {
    val size = item.intrinsic(shaper)
    canvas.save()
    canvas.translate(item.center.x, item.center.y)
    canvas.rotate(item.angle)
    canvas.scale(item.scale, item.scale)
    val rect = SkRect.makeXYWH(-size.width / 2, -size.height / 2, size.width, size.height)
    when (item.kind) {
        PlacedKind.Sticker -> item.sticker?.let(art)?.draw(canvas, rect, timeMs)
        PlacedKind.Text -> shaper.paragraph(item.text, item.base, item.color).paint(canvas, rect.left + item.base * TextPad / 2, rect.top)
        PlacedKind.Shape -> drawShape(canvas, item, rect)
    }
    canvas.restore()
}

private fun drawShape(canvas: Canvas, item: Placed, rect: SkRect) {
    val line = item.base * ShapeLine
    val paint = Paint().apply {
        isAntiAlias = true
        color = item.color
        mode = if (item.filled && item.shape != ShapeKind.Arrow) PaintMode.FILL else PaintMode.STROKE
        strokeWidth = line
        strokeCap = PaintStrokeCap.ROUND
        strokeJoin = PaintStrokeJoin.ROUND
    }
    val inner = if (paint.mode == PaintMode.STROKE) rect.inflate(-line / 2) else rect
    when (item.shape) {
        ShapeKind.Circle -> canvas.drawOval(inner, paint)
        ShapeKind.Rectangle -> canvas.drawRRect(RRect.makeLTRB(inner.left, inner.top, inner.right, inner.bottom, item.base * ShapeCorner), paint)
        ShapeKind.Star -> starPath(inner).use { canvas.drawPath(it, paint) }
        ShapeKind.Bubble -> bubblePath(inner, item.base).use { canvas.drawPath(it, paint) }
        ShapeKind.Arrow -> {
            val y = rect.top + rect.height / 2
            val head = rect.height / 2 - line / 2
            val path = PathBuilder()
                .moveTo(rect.left + line, y)
                .lineTo(rect.right - line, y)
                .moveTo(rect.right - line - head, y - head)
                .lineTo(rect.right - line, y)
                .lineTo(rect.right - line - head, y + head)
                .detach()
            paint.strokeWidth = line * ArrowShapeLine
            path.use { canvas.drawPath(it, paint) }
        }
    }
}

private fun starPath(rect: SkRect): Path {
    val cx = (rect.left + rect.right) / 2
    val cy = (rect.top + rect.bottom) / 2 + rect.height * StarDrop
    val outer = minOf(rect.width, rect.height) / 2
    val inner = outer * StarInner
    val builder = PathBuilder()
    for (index in 0 until StarPoints * 2) {
        val radius = if (index % 2 == 0) outer else inner
        val angle = -PI / 2 + index * PI / StarPoints
        val x = cx + (cos(angle) * radius).toFloat()
        val y = cy + (sin(angle) * radius).toFloat()
        if (index == 0) builder.moveTo(x, y) else builder.lineTo(x, y)
    }
    return builder.closePath().detach()
}

private fun bubblePath(rect: SkRect, base: Float): Path {
    val body = SkRect.makeLTRB(rect.left, rect.top, rect.right, rect.bottom - rect.height * BubbleTail)
    val radius = minOf(body.width, body.height) * BubbleRound
    val tailX = body.left + body.width * BubbleTailAt
    val builder = PathBuilder()
        .moveTo(body.left + radius, body.top)
        .lineTo(body.right - radius, body.top)
        .quadTo(body.right, body.top, body.right, body.top + radius)
        .lineTo(body.right, body.bottom - radius)
        .quadTo(body.right, body.bottom, body.right - radius, body.bottom)
        .lineTo(tailX + base * BubbleTailWidth, body.bottom)
        .lineTo(tailX - base * BubbleTailWidth / 2, rect.bottom)
        .lineTo(tailX, body.bottom)
        .lineTo(body.left + radius, body.bottom)
        .quadTo(body.left, body.bottom, body.left, body.bottom - radius)
        .lineTo(body.left, body.top + radius)
        .quadTo(body.left, body.top, body.left + radius, body.top)
        .closePath()
    return builder.detach()
}

internal object PhotoExport {
    suspend fun render(file: File, state: EditState): File? = withContext(Dispatchers.IO) {
        runCatching {
            val arts = HashMap<String, StickerArt?>()
            state.items.mapNotNull { it.sticker }.distinctBy { it.path }.forEach { arts[it.path] = loadStickerArt(it) }
            try {
                TextShaper().use { shaper ->
                    if (state.animated) gif(file, state, shaper, arts) else still(file, state, shaper, arts)
                }
            } finally {
                arts.values.forEach { it?.close() }
            }
        }.onFailure { AppLog.write("Photo edit export failed", it) }.getOrNull()
    }

    private fun target(extension: String): File {
        AppPaths.tempDir.mkdirs()
        return File(AppPaths.tempDir, "edited-${System.currentTimeMillis()}.$extension")
    }

    private fun still(file: File, state: EditState, shaper: TextShaper, arts: Map<String, StickerArt?>): File {
        val crop = state.crop
        val scale = minOf(1f, MaxStillSide / maxOf(crop.width, crop.height))
        val width = (crop.width * scale).roundToInt().coerceAtLeast(1)
        val height = (crop.height * scale).roundToInt().coerceAtLeast(1)
        val png = file.extension.lowercase() == "png"
        val bytes = decodeUpright(file.readBytes()).use { base ->
            val blurred = if (state.strokes.any { it.tool == DrawTool.Blur }) blurredCopy(base) else null
            EditImages(base, blurred).let { images ->
                Surface.makeRasterN32Premul(width, height).use { surface ->
                    val canvas = surface.canvas
                    if (!png) canvas.clear(0xFFFFFFFF.toInt())
                    canvas.scale(width / crop.width, height / crop.height)
                    canvas.translate(-crop.left, -crop.top)
                    drawEdit(canvas, state, images, shaper, { arts[it.path] }, 0L)
                    blurred?.close()
                    surface.makeImageSnapshot().use { image ->
                        if (png) image.encodeToData(EncodedImageFormat.PNG) else image.encodeToData(EncodedImageFormat.JPEG, JpegQuality)
                    }?.bytes
                }
            }
        } ?: error("encode failed")
        return target(if (png) "png" else "jpg").also { it.writeBytes(bytes) }
    }

    private fun gif(file: File, state: EditState, shaper: TextShaper, arts: Map<String, StickerArt?>): File {
        val crop = state.crop
        val scale = minOf(1f, MaxGifSide / maxOf(crop.width, crop.height))
        val width = ((crop.width * scale).roundToInt() / GifAlign * GifAlign).coerceAtLeast(GifAlign)
        val height = ((crop.height * scale).roundToInt() / 2 * 2).coerceAtLeast(2)
        val duration = arts.values.maxOfOrNull { it?.durationMs ?: 0L }?.coerceIn(MinGifMs, MaxGifMs) ?: MinGifMs
        val frames = (duration * GifFps / 1000).toInt().coerceAtLeast(1)
        val output = target("gif")
        decodeUpright(file.readBytes(), MaxGifSide.toInt() * 2).use { base ->
            val preview = EditImages(base, if (state.strokes.any { it.tool == DrawTool.Blur }) blurredCopy(base) else null)
            Surface.makeRasterN32Premul(width, height).use { surface ->
                val canvas = surface.canvas
                val info = ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL)
                val bitmap = Bitmap().apply { allocPixels(info) }
                fun frame(index: Int): ByteArray {
                    canvas.clear(0xFFFFFFFF.toInt())
                    canvas.save()
                    canvas.scale(width / crop.width, height / crop.height)
                    canvas.translate(-crop.left, -crop.top)
                    drawEdit(canvas, state, preview, shaper, { arts[it.path] }, index * 1000L / GifFps)
                    canvas.restore()
                    surface.makeImageSnapshot().use { it.readPixels(bitmap, 0, 0) }
                    return bitmap.readPixels(info, width * 4, 0, 0) ?: ByteArray(width * height * 4)
                }
                val samples = listOf(0, frames / 3, frames * 2 / 3).distinct().map(::frame)
                val palette = GifPalette.build(samples)
                val recorder = FFmpegFrameRecorder(output, width, height, 0).apply {
                    format = "gif"
                    videoCodec = avcodec.AV_CODEC_ID_GIF
                    pixelFormat = avutil.AV_PIX_FMT_PAL8
                    frameRate = GifFps.toDouble()
                }
                recorder.start()
                try {
                    val buffer = ByteBuffer.allocateDirect(width * height + PaletteBytes)
                    for (index in 0 until frames) {
                        val pixels = frame(index)
                        buffer.clear()
                        palette.indexInto(pixels, width, height, buffer)
                        palette.writeTable(buffer)
                        buffer.rewind()
                        val packed = Frame(width, height, Frame.DEPTH_UBYTE, 1).apply {
                            image[0] = buffer
                            imageStride = width
                        }
                        recorder.record(packed, avutil.AV_PIX_FMT_PAL8)
                    }
                } finally {
                    runCatching { recorder.stop() }
                    runCatching { recorder.release() }
                }
                bitmap.close()
            }
            preview.blurred?.close()
        }
        return output
    }
}

private class GifPalette(private val colors: IntArray) {
    private val lookup = ByteArray(LookupSize)

    init {
        for (key in 0 until LookupSize) {
            val r = ((key shr 10) and 31) * 255 / 31
            val g = ((key shr 5) and 31) * 255 / 31
            val b = (key and 31) * 255 / 31
            var best = 0
            var bestDistance = Int.MAX_VALUE
            for (index in colors.indices) {
                val color = colors[index]
                val dr = ((color shr 16) and 255) - r
                val dg = ((color shr 8) and 255) - g
                val db = (color and 255) - b
                val distance = dr * dr * 3 + dg * dg * 4 + db * db * 2
                if (distance < bestDistance) {
                    bestDistance = distance
                    best = index
                }
            }
            lookup[key] = best.toByte()
        }
    }

    fun indexInto(pixels: ByteArray, width: Int, height: Int, out: ByteBuffer) {
        for (y in 0 until height) {
            for (x in 0 until width) {
                val at = (y * width + x) * 4
                val dither = Bayer[(y and 3) * 4 + (x and 3)] - 8
                val b = ((pixels[at].toInt() and 255) + dither).coerceIn(0, 255)
                val g = ((pixels[at + 1].toInt() and 255) + dither).coerceIn(0, 255)
                val r = ((pixels[at + 2].toInt() and 255) + dither).coerceIn(0, 255)
                out.put(lookup[((r shr 3) shl 10) or ((g shr 3) shl 5) or (b shr 3)])
            }
        }
    }

    fun writeTable(out: ByteBuffer) {
        for (index in 0 until PaletteSize) {
            val color = colors.getOrElse(index) { 0 }
            out.put((color and 255).toByte())
            out.put(((color shr 8) and 255).toByte())
            out.put(((color shr 16) and 255).toByte())
            out.put(255.toByte())
        }
    }

    companion object {
        private const val LookupSize = 32 * 32 * 32
        private val Bayer = intArrayOf(0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5)

        fun build(frames: List<ByteArray>): GifPalette {
            val total = frames.sumOf { it.size / 4 }
            val step = maxOf(1, total / MaxSamples)
            val samples = ArrayList<Int>(total / step + 1)
            var counter = 0
            frames.forEach { pixels ->
                var at = 0
                while (at + 3 < pixels.size) {
                    if (counter++ % step == 0) {
                        samples += ((pixels[at + 2].toInt() and 255) shl 16) or ((pixels[at + 1].toInt() and 255) shl 8) or (pixels[at].toInt() and 255)
                    }
                    at += 4
                }
            }
            val boxes = mutableListOf(samples.toIntArray())
            while (boxes.size < PaletteSize) {
                val (index, channel) = boxes.withIndex()
                    .filter { it.value.size > 1 }
                    .map { (at, box) -> Triple(at, widestChannel(box), box) }
                    .maxByOrNull { (_, channel, box) -> channel.second.toLong() * box.size }
                    ?.let { (at, channel, _) -> at to channel.first } ?: break
                val box = boxes.removeAt(index)
                val sorted = box.sortedBy { (it shr (16 - channel * 8)) and 255 }.toIntArray()
                val middle = sorted.size / 2
                boxes += sorted.copyOfRange(0, middle)
                boxes += sorted.copyOfRange(middle, sorted.size)
            }
            return GifPalette(boxes.filter { it.isNotEmpty() }.map(::average).toIntArray())
        }

        private fun widestChannel(box: IntArray): Pair<Int, Int> {
            var best = 0 to -1
            for (channel in 0..2) {
                val shift = 16 - channel * 8
                var low = 255
                var high = 0
                box.forEach {
                    val value = (it shr shift) and 255
                    if (value < low) low = value
                    if (value > high) high = value
                }
                if (high - low > best.second) best = channel to high - low
            }
            return best
        }

        private fun average(box: IntArray): Int {
            var r = 0L
            var g = 0L
            var b = 0L
            box.forEach {
                r += (it shr 16) and 255
                g += (it shr 8) and 255
                b += it and 255
            }
            val n = box.size.toLong()
            return ((r / n).toInt() shl 16) or ((g / n).toInt() shl 8) or (b / n).toInt()
        }
    }
}

internal fun angleBetween(center: Offset, from: Offset, to: Offset): Float {
    val a = atan2(from.y - center.y, from.x - center.x)
    val b = atan2(to.y - center.y, to.x - center.x)
    return ((b - a) * 180f / PI.toFloat())
}

internal fun distance(a: Offset, b: Offset): Float = hypot(a.x - b.x, a.y - b.y)

private val Smooth = FilterMipmap(FilterMode.LINEAR, MipmapMode.LINEAR)
private val ShadowColor = 0x59000000
private const val ShadowOffset = 0.04f
private const val ShadowBlur = 0.08f
private const val TextPad = 0.5f
private const val ShaperCacheSize = 64
private const val StickerArtSide = 512
private const val BlurStrength = 0.02f
private const val DotNudge = 0.01f
private const val ArrowReach = 3f
private const val ArrowHeadScale = 3.2f
private const val ArrowWing = 0.5f
private const val MarkerWidth = 2f
private const val MarkerAlpha = 0.4f
private const val BlurWidth = 3f
private const val ShapeLine = 0.06f
private const val ShapeCorner = 0.08f
private const val ArrowShapeLine = 1.4f
private const val StarPoints = 5
private const val StarInner = 0.45f
private const val StarDrop = 0.04f
private const val BubbleTail = 0.2f
private const val BubbleRound = 0.35f
private const val BubbleTailAt = 0.28f
private const val BubbleTailWidth = 0.12f
private const val MaxStillSide = 4096f
private const val MaxGifSide = 640f
private const val GifAlign = 8
private const val GifFps = 25
private const val MinGifMs = 1000L
private const val MaxGifMs = 6000L
private const val PaletteSize = 256
private const val PaletteBytes = 1024
private const val MaxSamples = 120_000
private const val JpegQuality = 95
