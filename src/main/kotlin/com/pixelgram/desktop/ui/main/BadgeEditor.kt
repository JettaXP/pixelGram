package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.pixelgram.desktop.core.AppLog
import com.pixelgram.desktop.core.CustomBadge
import com.pixelgram.desktop.core.CustomBadges
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.components.expressive.IconButton
import com.pixelgram.desktop.ui.components.expressive.IconToggleButton
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.outlined.Check
import com.pixelgram.desktop.ui.icons.outlined.RestartAlt
import com.pixelgram.desktop.ui.icons.outlined.Search
import com.pixelgram.desktop.ui.theme.EmojiText
import com.pixelgram.desktop.ui.theme.EmojiVisualTransformation
import it.tdlight.jni.TdApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private sealed interface BadgeCell {
    val emoji: String
    val customId: Long

    class Plain(override val emoji: String) : BadgeCell {
        override val customId = 0L
    }

    class Custom(val sticker: TdApi.Sticker) : BadgeCell {
        override val emoji: String = sticker.emoji.ifBlank { "⭐" }
        override val customId = (sticker.fullType as? TdApi.StickerFullTypeCustomEmoji)?.customEmojiId ?: 0L
    }
}

private class BadgeSection(val key: String, val title: String, val icon: @Composable () -> Unit, val cells: List<BadgeCell>)

private val EditorWidth = 380.dp
private val EditorHeight = 480.dp
private val BadgeCellSize = 40.dp
private val TabSize = 36.dp
private val CustomSize = 30.dp
private val PreviewSize = 32.dp

@Composable
fun BadgeEditor(telegram: Telegram, userId: Long, current: CustomBadge?, onDismiss: () -> Unit) {
    var picked by remember { mutableStateOf(current) }
    var text by remember { mutableStateOf(current?.text.orEmpty()) }
    var query by remember { mutableStateOf("") }
    var found by remember { mutableStateOf<List<BadgeCell>>(emptyList()) }
    var sets by remember { mutableStateOf<List<TdApi.StickerSetInfo>>(emptyList()) }
    val loaded = remember { mutableStateMapOf<Long, List<TdApi.Sticker>>() }
    val textFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        textFocus.requestFocus()
        telegram.customEmojiSets { list ->
            sets = list
            list.forEach { info -> telegram.stickerSet(info.id) { set -> set?.let { loaded[info.id] = it.stickers.toList() } } }
        }
    }
    LaunchedEffect(query) {
        val local = localEmojiSearch(query).map { BadgeCell.Plain(it) }
        found = local
        if (query.isBlank()) return@LaunchedEffect
        delay(PanelSearchDelayMs)
        telegram.searchCustomEmoji(query.trim()) { stickers ->
            found = stickers.map { BadgeCell.Custom(it) } + local
        }
    }
    val sections = buildList {
        sets.forEach { info ->
            val stickers = loaded[info.id].orEmpty()
            add(BadgeSection("set${info.id}", info.title, {
                stickers.firstOrNull()?.let { StickerView(it, telegram, CustomSize, animate = false) }
            }, stickers.map { BadgeCell.Custom(it) }))
        }
        EmojiGroups.forEach { group ->
            add(BadgeSection(group.name, group.name, {
                Icon(groupIcon(group.name), contentDescription = group.name, modifier = Modifier.size(20.dp))
            }, group.emoji.map { BadgeCell.Plain(it) }))
        }
    }
    val save = {
        picked?.let { badge ->
            val result = badge.copy(text = CustomBadges.clean(text))
            CustomBadges.setOwn(userId, result)
            telegram.sendBadge(result) { error -> error?.let { AppLog.write("Badge upload failed: $it") } }
        }
        onDismiss()
    }
    val reset = {
        CustomBadges.setOwn(userId, null)
        telegram.sendBadge(null) { error -> error?.let { AppLog.write("Badge reset failed: $it") } }
        onDismiss()
    }
    Popup(
        popupPositionProvider = BelowAnchor,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
        onPreviewKeyEvent = { event ->
            if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                onDismiss()
                true
            } else {
                false
            }
        },
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 8.dp,
            modifier = Modifier.width(EditorWidth).height(EditorHeight),
        ) {
            Column {
                val grid = rememberLazyGridState()
                val scope = rememberCoroutineScope()
                val starts = sections.runningFold(0) { index, section -> index + 1 + section.cells.size }.dropLast(1)
                val currentTab by remember(starts) { derivedStateOf { starts.indexOfLast { it <= grid.firstVisibleItemIndex }.coerceAtLeast(0) } }
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    itemsIndexed(sections, key = { _, section -> section.key }) { index, section ->
                        IconToggleButton(
                            checked = query.isBlank() && index == currentTab,
                            onCheckedChange = {
                                query = ""
                                scope.launch { grid.animateScrollToItem(starts[index]) }
                            },
                            modifier = Modifier.size(TabSize),
                        ) { section.icon() }
                    }
                }
                CompactSearch(query) { query = it }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    if (query.isNotBlank()) {
                        if (found.isEmpty()) {
                            Text(
                                "Nothing found",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.Center),
                            )
                        } else {
                            LazyVerticalGrid(GridCells.Adaptive(BadgeCellSize), Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                                items(found) { cell -> BadgeCellView(cell, telegram, picked) { picked = it } }
                            }
                        }
                    } else {
                        LazyVerticalGrid(GridCells.Adaptive(BadgeCellSize), Modifier.fillMaxSize().padding(horizontal = 8.dp), state = grid) {
                            sections.forEach { section ->
                                item(span = { GridItemSpan(maxLineSpan) }, key = "title-${section.key}") {
                                    EmojiText(
                                        section.title,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 4.dp),
                                    )
                                }
                                items(section.cells) { cell -> BadgeCellView(cell, telegram, picked) { picked = it } }
                            }
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                ) {
                    Box(Modifier.size(PreviewSize), contentAlignment = Alignment.Center) {
                        picked?.let { BadgeEmoji(it, PreviewSize) }
                    }
                    TextField(
                        value = text,
                        onValueChange = { text = it.replace('\n', ' ').take(CustomBadges.MaxText) },
                        visualTransformation = EmojiVisualTransformation,
                        placeholder = { Text("Badge text") },
                        singleLine = true,
                        shape = CircleShape,
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier.weight(1f).height(52.dp).focusRequester(textFocus).onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && (event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                                save()
                                true
                            } else {
                                false
                            }
                        },
                    )
                    IconButton(onClick = reset) { Icon(Icons.Outlined.RestartAlt, contentDescription = "Reset badge") }
                    IconButton(onClick = save, enabled = picked != null) { Icon(Icons.Outlined.Check, contentDescription = "Save badge") }
                }
            }
        }
    }
}

@Composable
private fun BadgeCellView(cell: BadgeCell, telegram: Telegram, picked: CustomBadge?, onPick: (CustomBadge) -> Unit) {
    val selected = picked != null && picked.emoji == cell.emoji && picked.customEmojiId == cell.customId
    Box(
        Modifier
            .size(BadgeCellSize)
            .clip(CircleShape)
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .clickable { onPick(CustomBadge(cell.emoji, cell.customId, picked?.text.orEmpty())) },
        contentAlignment = Alignment.Center,
    ) {
        when (cell) {
            is BadgeCell.Custom -> StickerView(cell.sticker, telegram, CustomSize, animate = false)
            is BadgeCell.Plain -> EmojiText(cell.emoji, fontSize = 26.sp)
        }
    }
}

@Composable
fun BadgeEmoji(badge: CustomBadge, size: androidx.compose.ui.unit.Dp) {
    if (badge.customEmojiId != 0L) {
        CustomEmojiView(badge.customEmojiId, badge.emoji, Modifier.size(size))
    } else {
        val fontSize = with(androidx.compose.ui.platform.LocalDensity.current) { (size * EmojiScale).toSp() }
        EmojiText(badge.emoji, fontSize = fontSize)
    }
}

private const val EmojiScale = 0.8f

@Composable
private fun CompactSearch(query: String, onQuery: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQuery,
        visualTransformation = EmojiVisualTransformation,
        placeholder = { Text("Search") },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        singleLine = true,
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).height(52.dp),
    )
}

private object BelowAnchor : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val x = (anchorBounds.center.x - popupContentSize.width / 2).coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
        val below = anchorBounds.bottom + 8
        val y = if (below + popupContentSize.height <= windowSize.height) below else (anchorBounds.top - 8 - popupContentSize.height).coerceAtLeast(0)
        return IntOffset(x, y)
    }
}
