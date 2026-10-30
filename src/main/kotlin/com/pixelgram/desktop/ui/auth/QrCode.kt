package com.pixelgram.desktop.ui.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asSkiaPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.geometry.Rect
import kotlinx.coroutines.launch
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.runtime.getValue
import androidx.compose.foundation.Canvas
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.luminance
import io.nayuki.qrcodegen.QrCode as Qr
import kotlin.math.pow

private enum class PieceKind { Dot, Horizontal, Vertical, Blob }

private class Piece(val x: Int, val y: Int, val length: Int, val kind: PieceKind, val hash: Int)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun QrCode(text: String, modifier: Modifier = Modifier) {
    var shown by remember { mutableStateOf(text) }
    val clock = remember { Animatable(0f) }
    val backdrop = remember { Animatable(0f) }
    LaunchedEffect(text) {
        launch { backdrop.animateTo(1f, tween(BackdropMs, easing = LinearEasing)) }
        if (shown != text) {
            clock.animateTo(0f, tween((clock.value / RevealMs * HideMs).toInt(), easing = LinearEasing))
            shown = text
        }
        clock.animateTo(RevealMs, tween((RevealMs - clock.value).toInt(), easing = LinearEasing))
    }
    val qr = remember(shown) { Qr.encodeText(shown, Qr.Ecc.HIGH) }
    val pieces = remember(qr) { pieces(qr) }
    val scheme = MaterialTheme.colorScheme
    val darkTheme = scheme.surface.luminance() < 0.5f
    val background = if (darkTheme) scheme.inverseSurface else scheme.secondaryContainer
    val main = if (darkTheme) scheme.inversePrimary else scheme.primary
    val neutral = if (darkTheme) scheme.inverseOnSurface.copy(alpha = 0.85f) else scheme.onSurfaceVariant
    val accent = if (darkTheme) scheme.tertiaryContainer else scheme.tertiary
    val backShape = MaterialShapes.Cookie4Sided.toShape()
    val eyePolygons = remember(shown) {
        val pool = listOf(MaterialShapes.Cookie7Sided, MaterialShapes.Cookie9Sided, MaterialShapes.Cookie12Sided)
        List(3) { pool.random() }
    }
    val eyeShapes = eyePolygons.map { it.toShape() }
    val blobShapes = listOf(
        MaterialShapes.Circle.toShape(),
        MaterialShapes.Cookie7Sided.toShape(),
        MaterialShapes.Sunny.toShape(),
        MaterialShapes.Clover4Leaf.toShape(),
        MaterialShapes.Cookie6Sided.toShape(),
        MaterialShapes.Cookie9Sided.toShape(),
        MaterialShapes.Cookie4Sided.toShape(),
    )
    val edgeCache = remember(qr) { HashMap<Size, List<Pair<Int, Int>>>() }
    val time by produceState(0f) {
        val start = withFrameMillis { it }
        while (true) withFrameMillis { value = (it - start) / 1000f }
    }
    Canvas(
        modifier.graphicsLayer {
            scaleX = BackdropStartScale + (1f - BackdropStartScale) * backdrop.value
            scaleY = scaleX
        },
    ) {
        val fade = backdrop.value
        val area = size.minDimension * QrArea
        val cell = area / qr.size
        val origin = Offset((size.width - area) / 2, (size.height - area) / 2)
        val now = clock.value
        val middle = qr.size / 2f
        fun grow(start: Float, duration: Float) = decelerate(((now - start) / duration).coerceIn(0f, 1f))
        fun wave(cx: Float, cy: Float, hash: Int): Float {
            val distance = (kotlin.math.hypot(cx - middle, cy - middle) / (middle * Sqrt2)).pow(WaveCurve)
            val jitter = (hash % JitterSteps) / JitterSteps.toFloat()
            return LeadMs + (distance + (jitter - 0.5f) * Jitter).coerceAtLeast(0f) * SpreadMs
        }
        val backPath = (backShape.createOutline(size, layoutDirection, this) as? Outline.Generic)?.path ?: Path()
        val edgeDots = edgeCache.getOrPut(size) { edgeCells(backPath, origin, cell, qr.size) }
        drawIntoCanvas { it.saveLayer(Rect(Offset.Zero, size), Paint().apply { alpha = fade }) }
        drawShape(backShape, Offset.Zero, size, background)
        clipPath(backPath) { translate(origin.x, origin.y) {
            listOf(0 to 0, qr.size - 7 to 0, 0 to qr.size - 7).forEachIndexed { index, (ex, ey) ->
                val center = Offset((ex + 3.5f) * cell, (ey + 3.5f) * cell)
                val ring = grow(RingMs, RingGrowMs)
                if (ring > 0f) scale(ring, pivot = center) {
                    drawCircle(main, radius = cell * 3f, center = center, style = Stroke(width = cell))
                }
                val pupil = grow(PupilMs, PupilGrowMs)
                if (pupil > 0f) scale(pupil, pivot = center) {
                    val eye = cell * 3.3f
                    rotate(EyeSpin[index] * time % 360f, pivot = center) {
                        drawShape(eyeShapes[index], center - Offset(eye / 2, eye / 2), Size(eye, eye), main)
                    }
                }
            }
            val faint = neutral.copy(alpha = 0.08f)
            for (y in 0 until qr.size) {
                for (x in 0 until qr.size) {
                    if (qr.getModule(x, y)) continue
                    val dot = grow(wave(x + 0.5f, y + 0.5f, cellHash(x, y)) - FaintAheadMs, FaintGrowMs)
                    if (dot <= 0f) continue
                    val side = cell * 0.6f * dot
                    drawRoundRect(
                        faint,
                        topLeft = Offset(x * cell + (cell - side) / 2, y * cell + (cell - side) / 2),
                        size = Size(side, side),
                        cornerRadius = CornerRadius(cell * 0.18f * dot),
                    )
                }
            }
            val thickness = cell * 0.74f
            pieces.forEach { piece ->
                val accented = piece.kind == PieceKind.Dot && piece.hash % 9 == 0
                val color = when {
                    accented -> accent
                    piece.hash % 2 == 0 -> main
                    else -> neutral
                }
                val left = piece.x * cell
                val top = piece.y * cell
                val span = when (piece.kind) {
                    PieceKind.Blob -> Offset(1f, 1f)
                    PieceKind.Horizontal -> Offset(piece.length / 2f, 0.5f)
                    PieceKind.Vertical -> Offset(0.5f, piece.length / 2f)
                    PieceKind.Dot -> Offset(0.5f, 0.5f)
                }
                val growth = grow(wave(piece.x + span.x, piece.y + span.y, piece.hash), PieceGrowMs)
                if (growth <= 0f) return@forEach
                scale(growth, pivot = Offset(left + span.x * cell, top + span.y * cell)) { when (piece.kind) {
                    PieceKind.Blob -> {
                        val blob = cell * 1.8f
                        val center = Offset(left + cell, top + cell)
                        val shape = blobShapes[(piece.hash / 2) % blobShapes.size]
                        drawShape(shape, center - Offset(blob / 2, blob / 2), Size(blob, blob), color)
                    }
                    PieceKind.Dot -> if (accented) {
                        drawRoundRect(
                            color,
                            topLeft = Offset(left + cell * 0.12f, top + cell * 0.12f),
                            size = Size(cell * 0.76f, cell * 0.76f),
                            cornerRadius = CornerRadius(cell * 0.2f),
                        )
                    } else {
                        drawCircle(color, radius = cell * 0.4f, center = Offset(left + cell / 2, top + cell / 2))
                    }
                    PieceKind.Horizontal -> drawRoundRect(
                        color,
                        topLeft = Offset(left + (cell - thickness) / 2, top + (cell - thickness) / 2),
                        size = Size(piece.length * cell - (cell - thickness), thickness),
                        cornerRadius = CornerRadius(thickness / 2),
                    )
                    PieceKind.Vertical -> drawRoundRect(
                        color,
                        topLeft = Offset(left + (cell - thickness) / 2, top + (cell - thickness) / 2),
                        size = Size(thickness, piece.length * cell - (cell - thickness)),
                        cornerRadius = CornerRadius(thickness / 2),
                    )
                } }
            }
        } }
        drawIntoCanvas { it.restore() }
        val edgeColor = lerp(background, main, EdgeTint).copy(alpha = (fade * EdgeAppear).coerceAtMost(1f))
        translate(origin.x, origin.y) {
            edgeDots.forEach { (x, y) ->
                drawRoundRect(
                    edgeColor,
                    topLeft = Offset(x * cell + cell * 0.2f, y * cell + cell * 0.2f),
                    size = Size(cell * 0.6f, cell * 0.6f),
                    cornerRadius = CornerRadius(cell * 0.18f),
                )
            }
        }
    }
}

private fun edgeCells(shape: Path, origin: Offset, cell: Float, count: Int): List<Pair<Int, Int>> {
    val outline = shape.asSkiaPath()
    val margin = kotlin.math.ceil(origin.x / cell).toInt() + 1
    val reach = cell * EdgeBand
    val result = ArrayList<Pair<Int, Int>>()
    for (y in -margin until count + margin) {
        for (x in -margin until count + margin) {
            if (x in 0 until count && y in 0 until count) continue
            if (cellHash(x + margin, y + margin) % EdgeDotEvery != 0) continue
            val cx = origin.x + (x + 0.5f) * cell
            val cy = origin.y + (y + 0.5f) * cell
            if (!outline.contains(cx, cy)) continue
            val nearEdge = (0 until EdgeProbes).any {
                val angle = it * 2 * Math.PI / EdgeProbes
                !outline.contains(cx + reach * kotlin.math.cos(angle).toFloat(), cy + reach * kotlin.math.sin(angle).toFloat())
            }
            if (nearEdge) result += x to y
        }
    }
    return result
}

private fun DrawScope.drawShape(shape: Shape, topLeft: Offset, size: Size, color: Color) {
    when (val outline = shape.createOutline(size, layoutDirection, this)) {
        is Outline.Generic -> translate(topLeft.x, topLeft.y) { drawPath(outline.path, color) }
        is Outline.Rounded -> translate(topLeft.x, topLeft.y) {
            drawRoundRect(color, size = size, cornerRadius = outline.roundRect.topLeftCornerRadius)
        }
        is Outline.Rectangle -> drawRect(color, topLeft, size)
    }
}

private fun pieces(qr: Qr): List<Piece> {
    val n = qr.size
    val taken = Array(n) { BooleanArray(n) }
    fun inEye(x: Int, y: Int) = (x < 8 && y < 8) || (x >= n - 8 && y < 8) || (x < 8 && y >= n - 8)
    fun free(x: Int, y: Int) = x in 0 until n && y in 0 until n && qr.getModule(x, y) && !taken[y][x] && !inEye(x, y)
    val result = ArrayList<Piece>()
    for (y in 0 until n) {
        for (x in 0 until n) {
            if (!free(x, y)) continue
            val hash = ((x * 73856093) xor (y * 19349663)).and(0x7fffffff) / 7
            if (hash % 4 == 0 && free(x + 1, y) && free(x, y + 1) && free(x + 1, y + 1)) {
                taken[y][x] = true
                taken[y][x + 1] = true
                taken[y + 1][x] = true
                taken[y + 1][x + 1] = true
                result += Piece(x, y, 2, PieceKind.Blob, hash)
                continue
            }
            var right = 1
            while (right < MaxRun && free(x + right, y)) right++
            var down = 1
            while (down < MaxRun && free(x, y + down)) down++
            when {
                down >= 2 && down >= right -> {
                    repeat(down) { taken[y + it][x] = true }
                    result += Piece(x, y, down, PieceKind.Vertical, hash)
                }
                right >= 2 -> {
                    repeat(right) { taken[y][x + it] = true }
                    result += Piece(x, y, right, PieceKind.Horizontal, hash)
                }
                else -> {
                    taken[y][x] = true
                    result += Piece(x, y, 1, PieceKind.Dot, hash)
                }
            }
        }
    }
    return result
}

private fun decelerate(t: Float): Float {
    val x = 1f - t
    return 1f - x * x * x
}

private fun cellHash(x: Int, y: Int) = ((x * 73856093) xor (y * 19349663)).and(0x7fffffff) / 7

private const val QrArea = 0.66f
private const val LeadMs = 110f
private const val SpreadMs = 1000f
private const val PieceGrowMs = 450f
private const val FaintAheadMs = 80f
private const val FaintGrowMs = 300f
private const val RingMs = 690f
private const val RingGrowMs = 380f
private const val PupilMs = 1120f
private const val PupilGrowMs = 700f
private const val RevealMs = PupilMs + PupilGrowMs
private const val HideMs = 300f
private const val Jitter = 0.25f
private const val WaveCurve = 1f
private const val EdgeDotEvery = 4
private const val EdgeBand = 1.6f
private const val EdgeProbes = 8
private const val EdgeTint = 0.12f
private const val EdgeAppear = 2.5f
private const val JitterSteps = 997
private const val BackdropMs = 350
private const val BackdropStartScale = 0.8f
private const val Sqrt2 = 1.4142135f
private const val MaxRun = 4
private val EyeSpin = floatArrayOf(72f, 84f, 66f)
