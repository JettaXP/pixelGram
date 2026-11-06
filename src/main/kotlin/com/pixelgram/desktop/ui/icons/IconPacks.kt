package com.pixelgram.desktop.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.core.IconPack
import java.util.concurrent.ConcurrentHashMap

object IconPacks {
    var settings: AppSettings? = null

    private class Glyph(val viewport: Float, val top: Float, val paths: List<Pair<String, Boolean>>)

    private val tables = ConcurrentHashMap<String, Map<String, Glyph>>()
    private val built = ConcurrentHashMap<String, ImageVector>()

    fun icon(key: String, fallback: ImageVector): ImageVector {
        val pack = settings?.iconPack?.value ?: IconPack.Material
        val name = when {
            pack != IconPack.Material -> pack.name.lowercase()
            settings?.roundedIcons?.value == true -> RoundedTable
            else -> return fallback
        }
        val id = "$name:$key"
        built[id]?.let { return it }
        val glyphs = table(name)
        val base = key.substringAfterLast('.')
        val glyph = glyphs[key]
            ?: listOf("Outlined", "Filled", "Rounded", "AutoMirrored.Outlined", "AutoMirrored.Filled").firstNotNullOfOrNull { glyphs["$it.$base"] }
            ?: return fallback
        val pad = if (pack == IconPack.Solar && key.substringAfter('.') in MediaKeys) glyph.viewport * MediaPadding else 0f
        return build(key, glyph, fallback.autoMirror, pad).also { built[id] = it }
    }

    private fun table(name: String): Map<String, Glyph> = tables.getOrPut(name) {
        val stream = IconPacks::class.java.getResourceAsStream("/iconpacks/$name.tsv") ?: return@getOrPut emptyMap()
        stream.bufferedReader().useLines { lines ->
            lines.filter { it.isNotBlank() }.associate { line ->
                val parts = line.split('\t')
                val box = parts[1].split(',')
                parts[0] to Glyph(box[0].toFloat(), box.getOrNull(1)?.toFloat() ?: 0f, parts.drop(2).map { it.substring(2) to it.startsWith("e:") })
            }
        }
    }

    private const val RoundedTable = "materialrounded"
    private val MediaKeys = setOf("PlayArrow", "Pause", "SkipNext", "SkipPrevious", "Stop", "FastForward", "FastRewind")
    private const val MediaPadding = 0.14f

    private fun build(key: String, glyph: Glyph, mirror: Boolean, pad: Float = 0f): ImageVector {
        val size = glyph.viewport + pad * 2
        val builder = ImageVector.Builder(key, 24.dp, 24.dp, size, size, autoMirror = mirror)
        val shift = pad > 0f || glyph.top != 0f
        if (shift) builder.addGroup(translationX = pad, translationY = pad - glyph.top)
        glyph.paths.forEach { (data, evenOdd) ->
            builder.addPath(
                addPathNodes(data),
                pathFillType = if (evenOdd) PathFillType.EvenOdd else PathFillType.NonZero,
                fill = SolidColor(Color.Black),
            )
        }
        if (shift) builder.clearGroup()
        return builder.build()
    }
}
