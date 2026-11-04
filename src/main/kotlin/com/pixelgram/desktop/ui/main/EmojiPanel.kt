package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.clickable
import com.pixelgram.desktop.ui.icons.outlined.Schedule
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.outlined.DirectionsCar
import com.pixelgram.desktop.ui.icons.outlined.EmojiEmotions
import com.pixelgram.desktop.ui.icons.outlined.EmojiFlags
import com.pixelgram.desktop.ui.icons.outlined.EmojiNature
import com.pixelgram.desktop.ui.icons.outlined.EmojiObjects
import com.pixelgram.desktop.ui.icons.outlined.EmojiPeople
import com.pixelgram.desktop.ui.icons.outlined.EmojiSymbols
import com.pixelgram.desktop.ui.icons.outlined.Fastfood
import com.pixelgram.desktop.ui.icons.outlined.SportsBasketball
import androidx.compose.material3.Icon
import com.pixelgram.desktop.ui.components.expressive.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.pixelgram.desktop.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelgram.desktop.ui.theme.EmojiText
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import com.pixelgram.desktop.ui.components.expressive.IconButton
import com.pixelgram.desktop.ui.icons.outlined.Search
import com.pixelgram.desktop.ui.icons.outlined.Close
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.theme.EmojiVisualTransformation

internal class EmojiGroup(val name: String, val emoji: List<String>)

internal val EmojiGroups: List<EmojiGroup> by lazy {
    val text = EmojiGroup::class.java.getResourceAsStream("/emoji/emoji.txt")
        ?.bufferedReader(Charsets.UTF_8)?.readLines().orEmpty()
    text.filter { !it.startsWith("#") && it.isNotBlank() }
        .chunked(2)
        .filter { it.size == 2 && it[0].startsWith("@") }
        .map { (name, emoji) -> EmojiGroup(name.removePrefix("@"), emoji.split(' ').filter(String::isNotEmpty)) }
}

internal fun groupIcon(name: String): ImageVector = when (name) {
    "Smileys & Emotion" -> Icons.Outlined.EmojiEmotions
    "People & Body" -> Icons.Outlined.EmojiPeople
    "Animals & Nature" -> Icons.Outlined.EmojiNature
    "Food & Drink" -> Icons.Outlined.Fastfood
    "Travel & Places" -> Icons.Outlined.DirectionsCar
    "Activities" -> Icons.Outlined.SportsBasketball
    "Objects" -> Icons.Outlined.EmojiObjects
    "Symbols" -> Icons.Outlined.EmojiSymbols
    else -> Icons.Outlined.EmojiFlags
}

private sealed interface PanelEmoji {
    val emoji: String
    class Plain(override val emoji: String) : PanelEmoji
    class Custom(override val emoji: String, val id: Long) : PanelEmoji
}

private object RecentEmoji {
    fun all(): List<PanelEmoji> = com.pixelgram.desktop.ui.theme.EmojiFonts.settings?.recentEmoji?.value.orEmpty()
        .split(Record).filter { it.isNotBlank() }.map { entry ->
            val parts = entry.split(Field, limit = 2)
            val id = parts.getOrNull(0)?.toLongOrNull()
            if (parts.size == 2 && id != null) PanelEmoji.Custom(parts[1], id) else PanelEmoji.Plain(entry)
        }

    fun add(item: PanelEmoji) {
        val settings = com.pixelgram.desktop.ui.theme.EmojiFonts.settings ?: return
        fun key(it: PanelEmoji) = if (it is PanelEmoji.Custom) "${it.id}$Field${it.emoji}" else it.emoji
        val next = (listOf(item) + all()).distinctBy(::key).take(RecentLimit)
        settings.recentEmoji.value = next.joinToString(Record, transform = ::key)
    }

    private const val Record = "\u001E"
    private const val Field = "\u001F"
    private const val RecentLimit = 32
}

@Composable
fun EmojiPanel(
    onPick: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    telegram: com.pixelgram.desktop.telegram.Telegram? = null,
    onCustom: ((String, Long) -> Unit)? = null,
) {
    val groups = EmojiGroups
    val grid = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    if (telegram != null && onCustom != null) LaunchedEffect(telegram) { telegram.loadEmojiPacks() }
    val packs = if (telegram != null && onCustom != null) telegram.emojiPacks.toList() else emptyList()
    val recent = RecentEmoji.all().filter { it is PanelEmoji.Plain || onCustom != null }
    val recentCount = if (recent.isEmpty()) 0 else 1 + recent.size
    val starts = remember(groups, recentCount) {
        groups.runningFold(recentCount) { index, group -> index + 1 + group.emoji.size }.dropLast(1)
    }
    val packStarts = remember(starts, packs) {
        packs.runningFold(starts.last() + 1 + groups.last().emoji.size) { index, pack -> index + 1 + pack.emoji.size }.dropLast(1)
    }
    val current by remember(starts) {
        derivedStateOf { starts.indexOfLast { it <= grid.firstVisibleItemIndex }.coerceAtLeast(0) }
    }
    val currentPack by remember(packStarts) {
        derivedStateOf { packStarts.indexOfLast { it <= grid.firstVisibleItemIndex } }
    }
    val pick: (PanelEmoji) -> Unit = { item ->
        RecentEmoji.add(item)
        when (item) {
            is PanelEmoji.Custom -> onCustom?.invoke(item.emoji, item.id)
            is PanelEmoji.Plain -> onPick(item.emoji)
        }
    }
    Box(modifier) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onSearch, modifier = Modifier.size(TabButton)) {
                    Icon(Icons.Outlined.Search, contentDescription = "Search emoji", modifier = Modifier.size(20.dp))
                }
                if (recentCount > 0) {
                    IconToggleButton(
                        checked = grid.firstVisibleItemIndex < recentCount,
                        onCheckedChange = { scope.launch { grid.animateScrollToItem(0) } },
                        modifier = Modifier.size(TabButton),
                    ) {
                        Icon(Icons.Outlined.Schedule, contentDescription = "Recent", modifier = Modifier.size(20.dp))
                    }
                }
                groups.forEachIndexed { index, group ->
                    IconToggleButton(
                        checked = index == current && grid.firstVisibleItemIndex >= recentCount && currentPack < 0,
                        onCheckedChange = { scope.launch { grid.animateScrollToItem(starts[index]) } },
                        modifier = Modifier.size(TabButton),
                    ) {
                        Icon(groupIcon(group.name), contentDescription = group.name, modifier = Modifier.size(20.dp))
                    }
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(EmojiCell),
                state = grid,
                modifier = Modifier.padding(horizontal = 8.dp).weight(1f),
            ) {
                if (recent.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) { PanelHeader("Recent") }
                    items(recent) { item -> EmojiCellView(item, pick) }
                }
                groups.forEach { group ->
                    item(span = { GridItemSpan(maxLineSpan) }) { PanelHeader(group.name) }
                    items(group.emoji) { emoji -> EmojiCellView(PanelEmoji.Plain(emoji), pick) }
                }
                packs.forEach { pack ->
                    item(span = { GridItemSpan(maxLineSpan) }) { PanelHeader(pack.title, translate = false) }
                    items(pack.emoji) { (emoji, id) -> EmojiCellView(PanelEmoji.Custom(emoji, id), pick) }
                }
            }
            if (packs.isNotEmpty()) {
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(packs.size) { index ->
                        val pack = packs[index]
                        val (emoji, id) = pack.emoji.first()
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (index == currentPack) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                                .clickable { scope.launch { grid.animateScrollToItem(packStarts[index]) } }
                                .padding(6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CustomEmojiView(id, emoji)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PanelHeader(title: String, translate: Boolean = true) {
    Text(
        if (translate) com.pixelgram.desktop.core.tr(title) else title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun EmojiCellView(item: PanelEmoji, onPick: (PanelEmoji) -> Unit) {
    Box(
        Modifier
            .size(EmojiCell)
            .clip(CircleShape)
            .clickable { onPick(item) },
        contentAlignment = Alignment.Center,
    ) {
        when (item) {
            is PanelEmoji.Custom -> CustomEmojiView(item.id, item.emoji, Modifier.size(30.dp))
            is PanelEmoji.Plain -> EmojiText(item.emoji, fontSize = 26.sp)
        }
    }
}

private val EmojiNames: List<Pair<String, String>> by lazy {
    EmojiGroups.flatMap { it.emoji }.map { emoji ->
        emoji to emoji.codePoints().toArray()
            .filter { it != 0xFE0F && it != 0x200D }
            .mapNotNull { Character.getName(it) }
            .joinToString(" ")
            .lowercase()
    }
}

internal fun localEmojiSearch(query: String): List<String> {
    val words = query.lowercase().split(' ').filter(String::isNotBlank)
    if (words.isEmpty()) return emptyList()
    return EmojiNames.filter { (_, name) -> words.all { word -> name.split(' ').any { it.startsWith(word) } } }.map { it.first }
}

@Composable
fun EmojiSearch(telegram: Telegram, query: String, onQuery: (String?) -> Unit, onPick: (String) -> Unit, modifier: Modifier = Modifier) {
    var found by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(query) {
        val local = localEmojiSearch(query)
        found = local
        if (query.isBlank()) return@LaunchedEffect
        delay(PanelSearchDelayMs)
        telegram.searchEmojis(query.trim()) { remote -> found = (remote + local).distinctBy { it.replace("\uFE0F", "") } }
    }
    Column(modifier) {
        PanelSearchField(query, "Search emoji", onQuery)
        if (query.isNotBlank() && found.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nothing found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyVerticalGrid(columns = GridCells.Adaptive(EmojiCell), modifier = Modifier.padding(horizontal = 8.dp)) {
                items(found) { emoji ->
                    Box(
                        Modifier.size(EmojiCell).clip(CircleShape).clickable { onPick(emoji) },
                        contentAlignment = Alignment.Center,
                    ) {
                        EmojiText(emoji, fontSize = 26.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PanelSearchField(query: String, placeholder: String, onQuery: (String?) -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    TextField(
        value = query,
        onValueChange = { onQuery(it) },
        visualTransformation = EmojiVisualTransformation,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = { onQuery(null) }) { Icon(Icons.Outlined.Close, contentDescription = "Close search") }
        },
        singleLine = true,
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = Modifier.fillMaxWidth().padding(8.dp).height(52.dp).focusRequester(focus).onPreviewKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                onQuery(null)
                true
            } else {
                false
            }
        },
    )
}

internal const val PanelSearchDelayMs = 250L

private val EmojiCell = 40.dp
private val TabButton = 32.dp
