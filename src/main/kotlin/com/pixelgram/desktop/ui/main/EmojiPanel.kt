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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.EmojiFlags
import androidx.compose.material.icons.outlined.EmojiNature
import androidx.compose.material.icons.outlined.EmojiObjects
import androidx.compose.material.icons.outlined.EmojiPeople
import androidx.compose.material.icons.outlined.EmojiSymbols
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.SportsBasketball
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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

private class EmojiGroup(val name: String, val emoji: List<String>)

// Built from Unicode's emoji-test.txt, see resources/emoji/emoji.txt.
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
fun EmojiPanel(onPick: (String) -> Unit, modifier: Modifier = Modifier) {
    val groups = EmojiGroups
    val grid = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    // Grid index of each group's header, for the category buttons.
    val starts = remember(groups) {
        groups.runningFold(0) { index, group -> index + 1 + group.emoji.size }.dropLast(1)
    }
    val current by remember {
        derivedStateOf { starts.indexOfLast { it <= grid.firstVisibleItemIndex }.coerceAtLeast(0) }
    }
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 6.dp,
        modifier = modifier.size(width = 360.dp, height = 400.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
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

private val EmojiCell = 40.dp
