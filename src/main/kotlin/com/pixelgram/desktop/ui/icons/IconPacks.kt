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

    private class Glyph(val viewport: Float, val paths: List<Pair<String, Boolean>>)

    private val tables = ConcurrentHashMap<IconPack, Map<String, Glyph>>()
    private val built = ConcurrentHashMap<String, ImageVector>()

    fun icon(key: String, fallback: ImageVector): ImageVector {
        val pack = settings?.iconPack?.value ?: IconPack.Material
        if (pack == IconPack.Material) return fallback
        val id = "${pack.name}:$key"
        built[id]?.let { return it }
        val glyph = table(pack)[key] ?: return fallback
        return build(key, glyph, fallback.autoMirror).also { built[id] = it }
    }

    private fun table(pack: IconPack): Map<String, Glyph> = tables.getOrPut(pack) {
        val stream = IconPacks::class.java.getResourceAsStream("/iconpacks/${pack.name.lowercase()}.tsv") ?: return@getOrPut emptyMap()
        stream.bufferedReader().useLines { lines ->
            lines.filter { it.isNotBlank() }.associate { line ->
                val parts = line.split('\t')
                parts[0] to Glyph(parts[1].toFloat(), parts.drop(2).map { it.substring(2) to it.startsWith("e:") })
            }
        }
    }

    private fun build(key: String, glyph: Glyph, mirror: Boolean): ImageVector {
        val builder = ImageVector.Builder(key, 24.dp, 24.dp, glyph.viewport, glyph.viewport, autoMirror = mirror)
        glyph.paths.forEach { (data, evenOdd) ->
            builder.addPath(
                addPathNodes(data),
                pathFillType = if (evenOdd) PathFillType.EvenOdd else PathFillType.NonZero,
                fill = SolidColor(Color.Black),
            )
        }
        return builder.build()
    }
}
