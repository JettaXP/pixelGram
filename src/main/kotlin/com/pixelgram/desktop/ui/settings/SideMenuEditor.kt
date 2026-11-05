package com.pixelgram.desktop.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.core.SideItem
import com.pixelgram.desktop.core.SideLayout
import com.pixelgram.desktop.core.SideMenuLayout
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.outlined.DragHandle
import com.pixelgram.desktop.ui.main.sideIcon
import kotlin.math.roundToInt

private const val TilesMark = "#tiles"
private const val ListMark = "#list"
private const val HiddenMark = "#hidden"

private fun flatten(layout: SideLayout): List<String> =
    listOf(TilesMark) + layout.tiles.map { it.name } + ListMark + layout.list.map { it.name } + HiddenMark + layout.hidden.map { it.name }

private fun unflatten(entries: List<String>): SideLayout {
    val list = entries.indexOf(ListMark)
    val hidden = entries.indexOf(HiddenMark)
    fun items(range: IntRange) = range.mapNotNull { index -> SideItem.entries.firstOrNull { it.name == entries[index] } }
    return SideLayout(items(1 until list), items(list + 1 until hidden), items(hidden + 1 until entries.size))
}

@Composable
fun SideMenuEditor(settings: AppSettings) {
    var entries by remember { mutableStateOf(flatten(SideMenuLayout.read(settings))) }
    val latest by rememberUpdatedState(entries)
    var dragging by remember { mutableStateOf<String?>(null) }
    var offset by remember { mutableFloatStateOf(0f) }
    val rowPx = with(LocalDensity.current) { (RowHeight + RowGap).toPx() }
    val placement = MaterialTheme.motionScheme.defaultSpatialSpec<androidx.compose.ui.unit.IntOffset>()
    fun commit(next: List<String>) {
        val hiddenAt = next.indexOf(HiddenMark)
        if (next.indexOf(SideItem.Settings.name) > hiddenAt) return
        if (next.first() != TilesMark || next.indexOf(ListMark) > hiddenAt) return
        entries = next
        SideMenuLayout.write(settings, unflatten(next))
    }
    fun toggle(name: String) {
        val list = entries.toMutableList()
        val hiddenAt = list.indexOf(HiddenMark)
        val index = list.indexOf(name)
        list.removeAt(index)
        if (index > hiddenAt) list.add(list.indexOf(HiddenMark), name) else list.add(name)
        commit(list)
    }
    val headers = 3
    val height = RowHeight * (entries.size - headers) + HeaderHeight * headers + RowGap * entries.size
    LazyColumn(
        userScrollEnabled = false,
        verticalArrangement = Arrangement.spacedBy(RowGap),
        modifier = Modifier.fillMaxWidth().height(height),
    ) {
        items(entries, key = { it }) { key ->
            when (key) {
                TilesMark, ListMark, HiddenMark -> Text(
                    when (key) {
                        TilesMark -> "Tiles"
                        ListMark -> "Menu"
                        else -> "Hidden items"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.height(HeaderHeight).padding(start = 4.dp, top = 12.dp).animateItem(fadeInSpec = null, fadeOutSpec = null, placementSpec = placement),
                )
                else -> {
                    val item = SideItem.valueOf(key)
                    val lifted = dragging == key
                    val hidden = entries.indexOf(key) > entries.indexOf(HiddenMark)
                    val tile = entries.indexOf(key) < entries.indexOf(ListMark)
                    val colors = MaterialTheme.colorScheme
                    val container by animateColorAsState(
                        when {
                            lifted -> colors.surfaceContainerHighest
                            tile -> colors.secondaryContainer
                            hidden -> colors.surfaceContainer
                            else -> colors.surfaceContainerHigh
                        },
                        MaterialTheme.motionScheme.defaultEffectsSpec(),
                    )
                    val elevation by animateFloatAsState(if (lifted) 1f else 0f, MaterialTheme.motionScheme.defaultEffectsSpec())
                    val alpha by animateFloatAsState(if (hidden) 0.55f else 1f, MaterialTheme.motionScheme.defaultEffectsSpec())
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .then(if (lifted) Modifier else Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null, placementSpec = placement))
                            .zIndex(if (lifted) 1f else 0f)
                            .graphicsLayer {
                                translationY = if (lifted) offset else 0f
                                scaleX = 1f + elevation * 0.02f
                                scaleY = 1f + elevation * 0.02f
                                shadowElevation = elevation * 10f
                                shape = RoundedCornerShape(if (tile) 20.dp else 16.dp)
                                clip = true
                            }
                            .fillMaxWidth()
                            .height(RowHeight)
                            .background(container)
                            .clickable(enabled = dragging == null && item != SideItem.Settings) { toggle(key) }
                            .padding(start = 16.dp, end = 4.dp),
                    ) {
                        Box(Modifier.graphicsLayer { this.alpha = alpha }) {
                            Icon(sideIcon(item, false), contentDescription = null, tint = colors.primary)
                        }
                        Text(item.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f).graphicsLayer { this.alpha = alpha })
                        if (item == SideItem.Money) {
                            Text("Shown when available", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                        }
                        Icon(
                            Icons.Outlined.DragHandle,
                            contentDescription = "Reorder",
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .padding(10.dp)
                                .pointerInput(key) {
                                    detectDragGestures(
                                        onDragStart = { dragging = key; offset = 0f },
                                        onDragEnd = { dragging = null; offset = 0f },
                                        onDragCancel = { dragging = null; offset = 0f },
                                    ) { change, amount ->
                                        change.consume()
                                        offset += amount.y
                                        val list = latest
                                        val from = list.indexOf(key)
                                        val steps = (offset / rowPx).roundToInt()
                                        if (steps == 0 || from < 0) return@detectDragGestures
                                        val target = (from + steps).coerceIn(1, list.lastIndex)
                                        if (target == from) return@detectDragGestures
                                        val next = list.toMutableList().apply { add(target, removeAt(from)) }
                                        val before = latest
                                        commit(next)
                                        if (latest !== before) offset -= (target - from) * rowPx
                                    }
                                },
                        )
                    }
                }
            }
        }
    }
}

private val RowHeight = 56.dp
private val HeaderHeight = 36.dp
private val RowGap = 2.dp
