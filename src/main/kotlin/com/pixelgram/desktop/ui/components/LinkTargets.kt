package com.pixelgram.desktop.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned

class LinkTargets(val linkFor: (String) -> String?) {
    internal val rects = HashMap<Any, Pair<Rect, String>>()

    fun find(x: Float, y: Float, band: Float, reach: Float): String? = rects.values
        .filter { (rect, _) -> kotlin.math.abs(rect.center.y - y) <= maxOf(band, rect.height / 2) && rect.left - reach <= x }
        .maxWithOrNull(compareBy<Pair<Rect, String>> { it.first.left }.thenBy { -kotlin.math.abs(it.first.center.y - y) })
        ?.second
}

val LocalLinkTargets = compositionLocalOf<LinkTargets?> { null }

@Composable
internal fun Modifier.linkTarget(text: String): Modifier {
    val targets = LocalLinkTargets.current ?: return this
    val link = remember(targets, text) { targets.linkFor(text) } ?: return this
    val key = remember { Any() }
    DisposableEffect(targets, key) { onDispose { targets.rects.remove(key) } }
    return onGloballyPositioned { targets.rects[key] = it.boundsInWindow() to link }
}
