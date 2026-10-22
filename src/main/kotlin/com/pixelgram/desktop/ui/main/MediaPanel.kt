package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.theme.EmojiText
import com.pixelgram.desktop.ui.theme.EmojiVisualTransformation
import it.tdlight.jni.TdApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val PanelWidth = 380.dp
private val PanelHeight = 440.dp
private val StickerCell = 68.dp
private val GifCell = 112.dp
private const val GifSearchDelayMs = 400L
private const val GifBot = "gif"

@Composable
fun MediaPanel(
    telegram: Telegram,
    chatId: Long,
    tab: Int,
    onTab: (Int) -> Unit,
    onEmoji: (String) -> Unit,
    onSticker: (TdApi.Sticker) -> Unit,
    onGif: (TdApi.Animation) -> Unit,
    onInline: (Long, String) -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 6.dp,
        modifier = Modifier.size(width = PanelWidth, height = PanelHeight),
    ) {
        Column {
            PrimaryTabRow(selectedTabIndex = tab, containerColor = Color.Transparent) {
                listOf("Emoji", "Stickers", "GIFs").forEachIndexed { index, title ->
                    Tab(selected = tab == index, onClick = { onTab(index) }, text = { Text(title) })
                }
            }
            when (tab) {
                0 -> EmojiPanel(onPick = onEmoji, modifier = Modifier.weight(1f))
                1 -> StickersTab(telegram, onSticker, Modifier.weight(1f))
                else -> GifsTab(telegram, chatId, onGif, onInline, Modifier.weight(1f))
            }
        }
    }
}

private class StickerSection(val id: Long, val title: String, val icon: TdApi.Sticker?, val thumb: TdApi.Thumbnail?)

@Composable
private fun StickersTab(telegram: Telegram, onSticker: (TdApi.Sticker) -> Unit, modifier: Modifier) {
    var recent by remember { mutableStateOf<List<TdApi.Sticker>?>(null) }
    var sets by remember { mutableStateOf<List<TdApi.StickerSetInfo>>(emptyList()) }
    val loaded = remember { mutableStateMapOf<Long, List<TdApi.Sticker>>() }
    LaunchedEffect(Unit) {
        telegram.recentStickers { recent = it }
        telegram.installedStickerSets { list ->
            sets = list
            list.forEach { info -> telegram.stickerSet(info.id) { set -> set?.let { loaded[info.id] = it.stickers.toList() } } }
        }
    }
    val sections = buildList {
        if (!recent.isNullOrEmpty()) add(StickerSection(0L, "Recently used", null, null) to recent.orEmpty())
        sets.forEach { info -> add(StickerSection(info.id, info.title, info.covers?.firstOrNull(), info.thumbnail) to loaded[info.id].orEmpty()) }
    }
    val starts = sections.runningFold(0) { index, (_, stickers) -> index + 1 + stickers.size }.dropLast(1)
    val grid = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val current by remember(starts) { derivedStateOf { starts.indexOfLast { it <= grid.firstVisibleItemIndex }.coerceAtLeast(0) } }
    Column(modifier) {
        if (recent == null && sets.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { LoadingIndicator() }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(StickerCell),
                state = grid,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            ) {
                sections.forEach { (section, stickers) ->
                    item(span = { GridItemSpan(maxLineSpan) }, key = "title${section.id}") {
                        EmojiText(
                            section.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 4.dp),
                        )
                    }
                    items(stickers, key = { "${section.id}-${it.sticker.id}" }) { sticker ->
                        Box(
                            Modifier.size(StickerCell).clip(RoundedCornerShape(12.dp)).clickable { onSticker(sticker) }.padding(4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            StickerThumb(sticker, telegram)
                        }
                    }
                }
            }
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            itemsIndexed(sections, key = { _, (section, _) -> section.id }) { index, (section, _) ->
                IconToggleButton(
                    checked = index == current,
                    onCheckedChange = { scope.launch { grid.animateScrollToItem(starts[index]) } },
                    modifier = Modifier.size(40.dp),
                ) {
                    when {
                        section.id == 0L -> Icon(Icons.Outlined.Schedule, contentDescription = section.title)
                        section.icon != null -> Box(Modifier.size(28.dp)) { StickerThumb(section.icon, telegram) }
                        else -> ThumbImage(section.thumb?.file, null, telegram, Modifier.size(28.dp), ContentScale.Fit)
                    }
                }
            }
        }
    }
}

@Composable
private fun StickerThumb(sticker: TdApi.Sticker, telegram: Telegram) {
    if (sticker.format is TdApi.StickerFormatWebp) {
        StickerView(sticker, telegram, StickerCell)
    } else {
        ThumbImage(sticker.thumbnail?.file, null, telegram, Modifier.fillMaxSize(), ContentScale.Fit)
    }
}

@Composable
private fun GifsTab(
    telegram: Telegram,
    chatId: Long,
    onGif: (TdApi.Animation) -> Unit,
    onInline: (Long, String) -> Unit,
    modifier: Modifier,
) {
    var query by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf<List<TdApi.Animation>?>(null) }
    var found by remember { mutableStateOf<TdApi.InlineQueryResults?>(null) }
    var bot by remember { mutableStateOf<TdApi.User?>(null) }
    LaunchedEffect(Unit) {
        telegram.savedAnimations { saved = it }
        telegram.inlineBot(GifBot) { bot = it }
    }
    LaunchedEffect(query, bot?.id) {
        found = null
        val gifBot = bot ?: return@LaunchedEffect
        if (query.isBlank()) return@LaunchedEffect
        delay(GifSearchDelayMs)
        telegram.inlineResults(gifBot.id, chatId, query.trim()) { found = it }
    }
    Column(modifier) {
        TextField(
            value = query,
            onValueChange = { query = it },
            visualTransformation = EmojiVisualTransformation,
            placeholder = { Text("Search GIFs") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            singleLine = true,
            shape = CircleShape,
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth().padding(8.dp).height(52.dp),
        )
        val searching = query.isNotBlank()
        val results = found
        when {
            !searching && saved == null -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { LoadingIndicator() }
            searching && results == null -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { LoadingIndicator() }
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(GifCell),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            ) {
                if (searching && results != null) {
                    items(results.results.filterIsInstance<TdApi.InlineQueryResultAnimation>(), key = { it.id }) { result ->
                        GifCellView(result.animation, telegram) { onInline(results.inlineQueryId, result.id) }
                    }
                } else {
                    items(saved.orEmpty(), key = { it.animation.id }) { animation ->
                        GifCellView(animation, telegram) { onGif(animation) }
                    }
                }
            }
        }
    }
}

@Composable
private fun GifCellView(animation: TdApi.Animation, telegram: Telegram, onClick: () -> Unit) {
    ThumbImage(
        animation.thumbnail?.file,
        animation.minithumbnail,
        telegram,
        Modifier
            .size(GifCell)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(onClick = onClick),
    )
}
