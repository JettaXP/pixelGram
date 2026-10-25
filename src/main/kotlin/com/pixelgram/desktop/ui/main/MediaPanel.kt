package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.gestures.detectTapGestures
import com.pixelgram.desktop.ui.icons.outlined.NotificationsOff
import com.pixelgram.desktop.ui.icons.outlined.CalendarMonth
import com.pixelgram.desktop.ui.icons.outlined.Alarm
import com.pixelgram.desktop.ui.icons.outlined.Collections
import com.pixelgram.desktop.ui.icons.outlined.Delete
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import com.pixelgram.desktop.ui.icons.outlined.StarOutline
import com.pixelgram.desktop.ui.icons.outlined.Star
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
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.outlined.Schedule
import com.pixelgram.desktop.ui.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import com.pixelgram.desktop.ui.components.Text
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
private const val FavoritesId = -1L

private fun Modifier.onSecondaryClick(action: () -> Unit): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                event.changes.first().consume()
                action()
            }
        }
    }
}

@Composable
fun MediaPanel(
    telegram: Telegram,
    chatId: Long,
    tab: Int,
    onTab: (Int) -> Unit,
    onEmoji: (String) -> Unit,
    onSticker: (TdApi.Sticker, StickerSend) -> Unit,
    onGif: (TdApi.Animation) -> Unit,
    onInline: (Long, String) -> Unit,
    whenOnline: Boolean = false,
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
                1 -> StickersTab(telegram, chatId, whenOnline, onSticker, Modifier.weight(1f))
                else -> GifsTab(telegram, chatId, onGif, onInline, Modifier.weight(1f))
            }
        }
    }
}

class StickerSend(val silent: Boolean = false, val schedule: TdApi.MessageSchedulingState? = null)

private class StickerSection(val id: Long, val title: String, val icon: TdApi.Sticker?, val thumb: TdApi.Thumbnail?)

@Composable
private fun StickersTab(
    telegram: Telegram,
    chatId: Long,
    whenOnline: Boolean,
    onSticker: (TdApi.Sticker, StickerSend) -> Unit,
    modifier: Modifier,
) {
    var preview by remember { mutableStateOf<TdApi.Sticker?>(null) }
    var scheduling by remember { mutableStateOf<TdApi.Sticker?>(null) }
    var recent by remember { mutableStateOf<List<TdApi.Sticker>?>(null) }
    val reloadRecent = { telegram.recentStickers { recent = it } }
    var favorites by remember { mutableStateOf<List<TdApi.Sticker>>(emptyList()) }
    var menuFor by remember { mutableStateOf<TdApi.Sticker?>(null) }
    val reloadFavorites = { telegram.favoriteStickers { favorites = it } }
    var sets by remember { mutableStateOf<List<TdApi.StickerSetInfo>>(emptyList()) }
    val loaded = remember { mutableStateMapOf<Long, List<TdApi.Sticker>>() }
    LaunchedEffect(Unit) {
        reloadRecent()
        reloadFavorites()
        telegram.installedStickerSets { list ->
            sets = list
            list.forEach { info -> telegram.stickerSet(info.id) { set -> set?.let { loaded[info.id] = it.stickers.toList() } } }
        }
    }
    val sections = buildList {
        if (favorites.isNotEmpty()) add(StickerSection(FavoritesId, "Favorites", null, null) to favorites)
        if (!recent.isNullOrEmpty()) add(StickerSection(0L, "Recently used", null, null) to recent.orEmpty())
        sets.forEach { info -> add(StickerSection(info.id, info.title, info.covers?.firstOrNull(), info.thumbnail) to loaded[info.id].orEmpty()) }
    }
    val starts = sections.runningFold(0) { index, (_, stickers) -> index + 1 + stickers.size }.dropLast(1)
    val grid = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val current by remember(starts) { derivedStateOf { starts.indexOfLast { it <= grid.firstVisibleItemIndex }.coerceAtLeast(0) } }
    Box(modifier) {
    Column(Modifier.fillMaxSize()) {
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
                            Modifier
                                .size(StickerCell)
                                .clip(RoundedCornerShape(12.dp))
                                .onSecondaryClick { menuFor = sticker }
                                .pointerInput(sticker.sticker.id) {
                                    detectTapGestures(
                                        onTap = { onSticker(sticker, StickerSend()) },
                                        onLongPress = { preview = sticker },
                                        onPress = {
                                            tryAwaitRelease()
                                            preview = null
                                        },
                                    )
                                }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            StickerThumb(sticker, telegram)
                            if (menuFor === sticker) {
                                val favorite = favorites.any { it.sticker.id == sticker.sticker.id }
                                val item = @Composable { label: String, icon: ImageVector, action: () -> Unit ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        leadingIcon = { Icon(icon, contentDescription = null) },
                                        onClick = {
                                            menuFor = null
                                            action()
                                        },
                                    )
                                }
                                DropdownMenu(expanded = true, onDismissRequest = { menuFor = null }, shape = RoundedCornerShape(16.dp)) {
                                    item("Send without sound", Icons.Outlined.NotificationsOff) { onSticker(sticker, StickerSend(silent = true)) }
                                    item("Schedule message", Icons.Outlined.CalendarMonth) { scheduling = sticker }
                                    if (whenOnline) {
                                        item("Send when online", Icons.Outlined.Alarm) {
                                            onSticker(sticker, StickerSend(schedule = TdApi.MessageSchedulingStateSendWhenOnline()))
                                        }
                                    }
                                    item(if (favorite) "Remove from Favorites" else "Add to Favorites", if (favorite) Icons.Outlined.StarOutline else Icons.Outlined.Star) {
                                        telegram.setStickerFavorite(sticker, !favorite) { reloadFavorites() }
                                    }
                                    if (sticker.setId != 0L) {
                                        item("View Sticker Set", Icons.Outlined.Collections) {
                                            StickerPack.current = StickerPackRequest(telegram, chatId, sticker.setId)
                                        }
                                    }
                                    if (section.id == 0L) {
                                        item("Remove from Recent", Icons.Outlined.Delete) { telegram.removeRecentSticker(sticker) { reloadRecent() } }
                                    }
                                }
                            }
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
                        section.id == FavoritesId -> Icon(Icons.Outlined.Star, contentDescription = section.title)
                        section.icon != null -> Box(Modifier.size(28.dp)) { StickerThumb(section.icon, telegram) }
                        else -> ThumbImage(section.thumb?.file, null, telegram, Modifier.size(28.dp), ContentScale.Fit)
                    }
                }
            }
        }
    }
    val previewState = remember { androidx.compose.animation.core.MutableTransitionState(false) }
    previewState.targetState = preview != null
    if (previewState.currentState || previewState.targetState) {
        androidx.compose.ui.window.Popup(
            popupPositionProvider = FullWindow,
            properties = androidx.compose.ui.window.PopupProperties(focusable = false),
        ) {
            val window = androidx.compose.ui.platform.LocalWindowInfo.current.containerSize
            val density = androidx.compose.ui.platform.LocalDensity.current
            androidx.compose.animation.AnimatedVisibility(
                visibleState = previewState,
                enter = androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.fadeOut(),
                modifier = with(density) { Modifier.size(window.width.toDp(), window.height.toDp()) },
            ) {
                var shown by remember { mutableStateOf(preview) }
                preview?.let { shown = it }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = PreviewScrim))
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.type == androidx.compose.ui.input.pointer.PointerEventType.Release) preview = null
                                }
                            }
                        },
                ) {
                    shown?.let { sticker ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.animateEnterExit(
                                enter = androidx.compose.animation.scaleIn(MaterialTheme.motionScheme.defaultSpatialSpec(), initialScale = 0.6f),
                                exit = androidx.compose.animation.scaleOut(MaterialTheme.motionScheme.fastSpatialSpec(), targetScale = 0.6f),
                            ),
                        ) {
                            EmojiText(sticker.emoji, style = MaterialTheme.typography.displaySmall)
                            StickerView(sticker, telegram, PreviewSize)
                        }
                    }
                }
            }
        }
    }
    }
    scheduling?.let { sticker ->
        ScheduleDialog(onDismiss = { scheduling = null }) { date ->
            scheduling = null
            onSticker(sticker, StickerSend(schedule = TdApi.MessageSchedulingStateSendAtDate(date, 0)))
        }
    }
}

private object FullWindow : androidx.compose.ui.window.PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: androidx.compose.ui.unit.IntRect,
        windowSize: androidx.compose.ui.unit.IntSize,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        popupContentSize: androidx.compose.ui.unit.IntSize,
    ) = androidx.compose.ui.unit.IntOffset.Zero
}

@Composable
private fun StickerThumb(sticker: TdApi.Sticker, telegram: Telegram) {
    StickerView(sticker, telegram, StickerCell, animate = com.pixelgram.desktop.core.PowerSaving.stickersInPanel)
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

private val PreviewSize = 320.dp
private const val PreviewScrim = 0.45f

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleDialog(onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    var day by remember { mutableStateOf<Long?>(null) }
    val dateState = androidx.compose.material3.rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    val now = java.time.LocalTime.now().plusMinutes(ScheduleLeadMinutes)
    val timeState = androidx.compose.material3.rememberTimePickerState(initialHour = now.hour, initialMinute = now.minute, is24Hour = true)
    val pickedDay = day
    if (pickedDay == null) {
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = { day = dateState.selectedDateMillis },
                    enabled = dateState.selectedDateMillis != null,
                ) { Text("Next") }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") } },
        ) { androidx.compose.material3.DatePicker(dateState) }
    } else {
        com.pixelgram.desktop.ui.components.expressive.AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Send at") },
            text = { androidx.compose.material3.TimePicker(timeState) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    val date = java.time.Instant.ofEpochMilli(pickedDay).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                    val at = date.atTime(timeState.hour, timeState.minute).atZone(java.time.ZoneId.systemDefault()).toEpochSecond()
                    onPick(at.toInt())
                }) { Text("Schedule") }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") } },
        )
    }
}

private const val ScheduleLeadMinutes = 10L
