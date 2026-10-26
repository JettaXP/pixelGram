package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.clickable
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

private class EmojiGroup(val name: String, val emoji: List<String>)

private val EmojiGroups: List<EmojiGroup> by lazy {
    val text = EmojiGroup::class.java.getResourceAsStream("/emoji/emoji.txt")
        ?.bufferedReader(Charsets.UTF_8)?.readLines().orEmpty()
    text.filter { !it.startsWith("#") && it.isNotBlank() }
        .chunked(2)
        .filter { it.size == 2 && it[0].startsWith("@") }
        .map { (name, emoji) -> EmojiGroup(name.removePrefix("@"), emoji.split(' ').filter(String::isNotEmpty)) }
}

private fun groupIcon(name: String): ImageVector = when (name) {
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

@Composable
fun EmojiPanel(onPick: (String) -> Unit, onSearch: () -> Unit, modifier: Modifier = Modifier) {
    val groups = EmojiGroups
    val grid = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val starts = remember(groups) {
        groups.runningFold(0) { index, group -> index + 1 + group.emoji.size }.dropLast(1)
    }
    val current by remember {
        derivedStateOf { starts.indexOfLast { it <= grid.firstVisibleItemIndex }.coerceAtLeast(0) }
    }
    Box(modifier) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onSearch, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.Search, contentDescription = "Search emoji", modifier = Modifier.size(20.dp))
                }
                groups.forEachIndexed { index, group ->
                    IconToggleButton(
                        checked = index == current,
                        onCheckedChange = { scope.launch { grid.animateScrollToItem(starts[index]) } },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(groupIcon(group.name), contentDescription = group.name, modifier = Modifier.size(20.dp))
                    }
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(EmojiCell),
                state = grid,
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                groups.forEach { group ->
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            group.name,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 4.dp),
                        )
                    }
                    items(group.emoji) { emoji ->
                        Box(
                            Modifier
                                .size(EmojiCell)
                                .clip(CircleShape)
                                .clickable { onPick(emoji) },
                            contentAlignment = Alignment.Center,
                        ) {
                            EmojiText(emoji, fontSize = 26.sp)
                        }
                    }
                }
            }
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

private fun localEmojiSearch(query: String): List<String> {
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
