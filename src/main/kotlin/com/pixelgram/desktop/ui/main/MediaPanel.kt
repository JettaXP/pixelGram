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
import androidx.compose.ui.input.pointer.pointerHoverIcon
import com.pixelgram.desktop.ui.components.expressive.IconButton
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
import com.pixelgram.desktop.ui.icons.outlined.Add
import com.pixelgram.desktop.ui.icons.outlined.Schedule
import com.pixelgram.desktop.ui.icons.outlined.Search
import androidx.compose.material3.Icon
import com.pixelgram.desktop.ui.components.expressive.IconToggleButton
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
import com.pixelgram.desktop.ui.icons.outlined.TextFields
import androidx.compose.foundation.layout.Row
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
private const val GifPanelSearchDelayMs = 400L
private const val SetCovers = 5
private val SetCoverSize = 44.dp
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
    onCustomEmoji: ((String, Long) -> Unit)? = null,
    onSendEmoji: ((String, Long?) -> Unit)? = null,
    onSticker: (TdApi.Sticker, StickerSend) -> Unit,
    onGif: (TdApi.Animation, StickerSend, String) -> Unit,
    onInline: (Long, String) -> Unit,
    search: String? = null,
    onSearch: (String?) -> Unit = {},
    whenOnline: Boolean = false,
    docked: Boolean = false,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = if (docked) 0.dp else 6.dp,
        modifier = if (docked) Modifier.fillMaxSize() else Modifier.size(width = PanelWidth, height = PanelHeight),
    ) {
        Column {
            PrimaryTabRow(selectedTabIndex = tab, containerColor = Color.Transparent) {
                listOf("Emoji", "Stickers", "GIFs").forEachIndexed { index, title ->
                    Tab(selected = tab == index, onClick = { onTab(index) }, text = { Text(title) })
                }
            }
            when (tab) {
                0 -> if (search != null) {
                    EmojiSearch(telegram, search, onSearch, onEmoji, Modifier.weight(1f))
                } else {
                    EmojiPanel(onPick = onEmoji, onSearch = { onSearch("") }, modifier = Modifier.weight(1f), telegram = telegram, onCustom = onCustomEmoji, onSend = onSendEmoji)
                }
                1 -> if (search != null) {
                    StickerSearch(telegram, chatId, search, onSearch, onSticker, Modifier.weight(1f))
                } else {
                    StickersTab(telegram, chatId, whenOnline, onSticker, { onSearch("") }, Modifier.weight(1f))
                }
                else -> GifsTab(telegram, chatId, whenOnline, onGif, onInline, Modifier.weight(1f))
            }
        }
    }
}

class StickerSend(val silent: Boolean = false, val schedule: TdApi.MessageSchedulingState? = null)

@Composable
fun StickerPicker(telegram: Telegram, chatId: Long, onPick: (TdApi.Sticker) -> Unit) {
    var search by remember { mutableStateOf<String?>(null) }
    val pick = { sticker: TdApi.Sticker, _: StickerSend -> onPick(sticker) }
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 6.dp,
        modifier = Modifier.size(width = PanelWidth, height = PanelHeight),
    ) {
        val query = search
        if (query != null) {
            StickerSearch(telegram, chatId, query, { search = it }, pick, Modifier.fillMaxSize().padding(top = 8.dp))
        } else {
            StickersTab(telegram, chatId, false, pick, { search = "" }, Modifier.fillMaxSize().padding(top = 4.dp), menu = false)
        }
    }
}

private class StickerSection(val id: Long, val title: String, val icon: TdApi.Sticker?, val thumb: TdApi.Thumbnail?)

@Composable
private fun StickersTab(
    telegram: Telegram,
    chatId: Long,
    whenOnline: Boolean,
    onSticker: (TdApi.Sticker, StickerSend) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier,
    menu: Boolean = true,
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
                            if (section.id == 0L || section.id == FavoritesId) com.pixelgram.desktop.core.tr(section.title) else section.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(start = 4.dp, top = 8.dp)
                                .then(
                                    if (section.id != 0L && section.id != FavoritesId) {
                                        Modifier
                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                            .pointerHoverIcon(androidx.compose.ui.input.pointer.PointerIcon.Hand)
                                            .clickable { StickerPack.current = StickerPackRequest(telegram, chatId, section.id) }
                                    } else {
                                        Modifier
                                    },
                                )
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                        )
                    }
                    items(stickers, key = { "${section.id}-${it.sticker.id}" }) { sticker ->
                        Box(
                            Modifier
                                .size(StickerCell)
                                .clip(RoundedCornerShape(12.dp))
                                .onSecondaryClick { if (menu) menuFor = sticker }
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
            item(key = "search") {
                IconButton(onClick = onSearch, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Outlined.Search, contentDescription = "Search stickers")
                }
            }
            if (menu) {
                item(key = "create") {
                    IconButton(onClick = { StickerCreate.current = StickerCreateRequest(telegram, chatId) }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Outlined.Add, contentDescription = "New sticker set")
                    }
                }
            }
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
    StickerHoldPreview(preview, telegram) { preview = null }
    }
    scheduling?.let { sticker ->
        ScheduleDialog(onDismiss = { scheduling = null }) { date ->
            scheduling = null
            onSticker(sticker, StickerSend(schedule = TdApi.MessageSchedulingStateSendAtDate(date, 0)))
        }
    }
}


@Composable
internal fun StickerHoldPreview(
    preview: TdApi.Sticker?,
    telegram: Telegram,
    dismissOnPress: Boolean = false,
    footer: (@Composable (TdApi.Sticker) -> Unit)? = null,
    onDismiss: () -> Unit,
) {
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
                enter = androidx.compose.animation.fadeIn(com.pixelgram.desktop.ui.components.Motion.effects()),
                exit = androidx.compose.animation.fadeOut(com.pixelgram.desktop.ui.components.Motion.fastEffects()),
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
                                    val closing = if (dismissOnPress) androidx.compose.ui.input.pointer.PointerEventType.Press else androidx.compose.ui.input.pointer.PointerEventType.Release
                                    if (event.type == closing && event.changes.none { it.isConsumed }) onDismiss()
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
                            footer?.invoke(sticker)
                        }
                    }
                }
            }
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
private fun StickerSearch(
    telegram: Telegram,
    chatId: Long,
    query: String,
    onQuery: (String?) -> Unit,
    onSticker: (TdApi.Sticker, StickerSend) -> Unit,
    modifier: Modifier,
) {
    var found by remember { mutableStateOf<List<TdApi.Sticker>?>(emptyList()) }
    var sets by remember { mutableStateOf<List<TdApi.StickerSetInfo>?>(null) }
    val added = remember { androidx.compose.runtime.mutableStateMapOf<Long, Boolean>() }
    LaunchedEffect(query) {
        if (query.isBlank()) {
            found = emptyList()
            sets = null
            telegram.trendingStickerSets { sets = it }
            return@LaunchedEffect
        }
        found = null
        sets = null
        delay(PanelSearchDelayMs)
        telegram.searchStickerSets(query.trim()) { sets = it }
        telegram.searchStickers(query.trim(), chatId) { found = it }
    }
    Column(modifier) {
        PanelSearchField(query, "Search stickers", onQuery)
        val results = found
        val packs = sets.orEmpty()
        when {
            query.isBlank() && sets == null || results == null -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { LoadingIndicator() }
            query.isNotBlank() && results.isEmpty() && packs.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Nothing found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(StickerCell),
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            ) {
                if (packs.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "sets-title") {
                        SearchSectionTitle(if (query.isBlank()) "Trending sticker sets" else "Sticker sets")
                    }
                    items(packs, key = { "set${it.id}" }, span = { GridItemSpan(maxLineSpan) }) { info ->
                        StickerSetRow(info, added[info.id] ?: info.isInstalled, telegram, chatId) { install ->
                            added[info.id] = install
                            telegram.setStickerSetInstalled(info.id, install) {}
                        }
                    }
                }
                if (results.isNotEmpty()) {
                    if (packs.isNotEmpty()) item(span = { GridItemSpan(maxLineSpan) }, key = "stickers-title") { SearchSectionTitle("Stickers") }
                    items(results.distinctBy { it.sticker.id }, key = { it.sticker.id }) { sticker ->
                        Box(
                            Modifier
                                .size(StickerCell)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSticker(sticker, StickerSend()) }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            StickerThumb(sticker, telegram)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchSectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun StickerSetRow(info: TdApi.StickerSetInfo, installed: Boolean, telegram: Telegram, chatId: Long, onInstall: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { StickerPack.current = StickerPackRequest(telegram, chatId, info.id) }
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Column(Modifier.weight(1f)) {
            EmojiText(info.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text("${info.size} stickers", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 4.dp)) {
                info.covers.orEmpty().take(SetCovers).forEach { cover ->
                    Box(Modifier.size(SetCoverSize)) { StickerView(cover, telegram, SetCoverSize, animate = false) }
                }
            }
        }
        if (installed) {
            com.pixelgram.desktop.ui.components.expressive.OutlinedButton(onClick = { onInstall(false) }) { Text("Remove") }
        } else {
            com.pixelgram.desktop.ui.components.expressive.Button(onClick = { onInstall(true) }) { Text("Add") }
        }
    }
}

@Composable
private fun StickerThumb(sticker: TdApi.Sticker, telegram: Telegram) {
    StickerView(sticker, telegram, StickerCell, animate = com.pixelgram.desktop.core.PowerSaving.stickersInPanel)
}

@Composable
private fun GifsTab(
    telegram: Telegram,
    chatId: Long,
    whenOnline: Boolean,
    onGif: (TdApi.Animation, StickerSend, String) -> Unit,
    onInline: (Long, String) -> Unit,
    modifier: Modifier,
) {
    GifHoldPreview(telegram)
    var query by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf<List<TdApi.Animation>?>(null) }
    var found by remember { mutableStateOf<TdApi.InlineQueryResults?>(null) }
    var bot by remember { mutableStateOf<TdApi.User?>(null) }
    var menuFor by remember { mutableStateOf<TdApi.Animation?>(null) }
    var scheduling by remember { mutableStateOf<TdApi.Animation?>(null) }
    var captioning by remember { mutableStateOf<TdApi.Animation?>(null) }
    val reload = { telegram.savedAnimations { saved = it } }
    LaunchedEffect(Unit) {
        reload()
        telegram.inlineBot(GifBot) { bot = it }
    }
    LaunchedEffect(query, bot?.id) {
        found = null
        val gifBot = bot ?: return@LaunchedEffect
        if (query.isBlank()) return@LaunchedEffect
        delay(GifPanelSearchDelayMs)
        telegram.inlineResults(gifBot.id, chatId, query.trim()) { found = it }
    }
    scheduling?.let { animation ->
        ScheduleDialog(onDismiss = { scheduling = null }) { date ->
            onGif(animation, StickerSend(schedule = TdApi.MessageSchedulingStateSendAtDate(date, 0)), "")
            scheduling = null
        }
    }
    captioning?.let { animation ->
        var caption by remember(animation) { mutableStateOf("") }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { captioning = null },
            title = { Text("Send GIF with caption") },
            text = {
                androidx.compose.material3.OutlinedTextField(caption, { caption = it }, label = { Text("Caption") }, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    onGif(animation, StickerSend(), caption)
                    captioning = null
                }) { Text("Send") }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { captioning = null }) { Text("Cancel") } },
        )
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
            else -> androidx.compose.foundation.layout.BoxWithConstraints(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                val cells: List<Pair<TdApi.Animation, () -> Unit>> = if (searching && results != null) {
                    results.results.filterIsInstance<TdApi.InlineQueryResultAnimation>().distinctBy { it.id }
                        .map { result -> result.animation to { onInline(results.inlineQueryId, result.id) } }
                } else {
                    saved.orEmpty().distinctBy { it.animation.id }.map { animation -> animation to { onGif(animation, StickerSend(), "") } }
                }
                val rows = remember(cells.map { it.first.animation.id }, maxWidth) { justifiedRows(cells.map { it.first }, maxWidth.value, GifRowHeight.value, GifGap.value) }
                androidx.compose.foundation.lazy.LazyColumn(verticalArrangement = Arrangement.spacedBy(GifGap)) {
                    items(rows.size) { rowIndex ->
                        val row = rows[rowIndex]
                        Row(horizontalArrangement = Arrangement.spacedBy(GifGap)) {
                            row.items.forEach { (index, width) ->
                                val (animation, send) = cells[index]
                                Box {
                                    GifCellView(animation, telegram, Modifier.size(width.dp, row.height.dp), onClick = send, onMenu = { menuFor = animation })
                                    if (menuFor === animation) {
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
                                            item("Send without sound", Icons.Outlined.NotificationsOff) { onGif(animation, StickerSend(silent = true), "") }
                                            item("Schedule message", Icons.Outlined.CalendarMonth) { scheduling = animation }
                                            if (whenOnline) {
                                                item("Send when online", Icons.Outlined.Alarm) {
                                                    onGif(animation, StickerSend(schedule = TdApi.MessageSchedulingStateSendWhenOnline()), "")
                                                }
                                            }
                                            item("Send GIF with caption", Icons.Outlined.TextFields) { captioning = animation }
                                            if (!searching) {
                                                item("Delete", Icons.Outlined.Delete) { telegram.removeSavedAnimation(animation) { reload() } }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private class GifRow(val items: List<Pair<Int, Float>>, val height: Float)

private fun justifiedRows(list: List<TdApi.Animation>, width: Float, target: Float, gap: Float): List<GifRow> {
    val rows = ArrayList<GifRow>()
    var start = 0
    while (start < list.size) {
        var sum = 0f
        var end = start
        while (end < list.size) {
            val a = list[end]
            val ratio = if (a.width > 0 && a.height > 0) (a.width.toFloat() / a.height).coerceIn(0.5f, 3f) else 1f
            sum += ratio
            end++
            if (sum * target + gap * (end - start - 1) >= width) break
        }
        val count = end - start
        val free = width - gap * (count - 1)
        val full = sum * target + gap * (count - 1) >= width
        val height = if (full) free / sum else target
        rows += GifRow(
            (start until end).map { i ->
                val a = list[i]
                val ratio = if (a.width > 0 && a.height > 0) (a.width.toFloat() / a.height).coerceIn(0.5f, 3f) else 1f
                i to ratio * height
            },
            height,
        )
        start = end
    }
    return rows
}

private val GifRowHeight = 96.dp
private val GifGap = 4.dp

@Composable
private fun GifCellView(animation: TdApi.Animation, telegram: Telegram, modifier: Modifier, onClick: () -> Unit, onMenu: () -> Unit) {
    ThumbImage(
        animation.thumbnail?.file,
        animation.minithumbnail,
        telegram,
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .onSecondaryPress(onMenu)
            .pointerInput(animation.animation.id) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { GifHold.current = animation },
                    onPress = {
                        tryAwaitRelease()
                        GifHold.current = null
                    },
                )
            },
    )
}

internal object GifHold {
    var current by mutableStateOf<TdApi.Animation?>(null)
}

@Composable
internal fun GifHoldPreview(telegram: Telegram) {
    val held = GifHold.current
    val state = remember { androidx.compose.animation.core.MutableTransitionState(false) }
    state.targetState = held != null
    var shown by remember { mutableStateOf(held) }
    held?.let { shown = it }
    if (!state.currentState && !state.targetState) return
    androidx.compose.ui.window.Popup(popupPositionProvider = FullWindow, properties = androidx.compose.ui.window.PopupProperties(focusable = false)) {
        val window = androidx.compose.ui.platform.LocalWindowInfo.current.containerSize
        val density = androidx.compose.ui.platform.LocalDensity.current
        androidx.compose.animation.AnimatedVisibility(
            visibleState = state,
            enter = androidx.compose.animation.fadeIn(com.pixelgram.desktop.ui.components.Motion.effects()),
            exit = androidx.compose.animation.fadeOut(com.pixelgram.desktop.ui.components.Motion.fastEffects()),
            modifier = with(density) { Modifier.size(window.width.toDp(), window.height.toDp()) },
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = PreviewScrim)),
            ) {
                shown?.let { gif ->
                    LaunchedEffect(gif.animation.id) { telegram.download(gif.animation) }
                    val path = telegram.localPath(gif.animation)
                    val ratio = if (gif.width > 0 && gif.height > 0) gif.width.toFloat() / gif.height else 1f
                    val box = if (ratio >= 1f) Modifier.size(GifPreviewSize, GifPreviewSize / ratio) else Modifier.size(GifPreviewSize * ratio, GifPreviewSize)
                    Box(
                        box
                            .animateEnterExit(
                                enter = androidx.compose.animation.scaleIn(MaterialTheme.motionScheme.defaultSpatialSpec(), initialScale = 0.6f),
                                exit = androidx.compose.animation.scaleOut(MaterialTheme.motionScheme.fastSpatialSpec(), targetScale = 0.6f),
                            )
                            .clip(RoundedCornerShape(20.dp)),
                    ) {
                        ThumbImage(gif.thumbnail?.file, gif.minithumbnail, telegram, Modifier.fillMaxSize())
                        if (path != null) InlineVideo(path, Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}

private val GifPreviewSize = 360.dp

private val PreviewSize = 320.dp
private const val PreviewScrim = 0.45f

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun ScheduleDialog(onDismiss: () -> Unit, onPick: (Int) -> Unit) {
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

private fun Modifier.onSecondaryPress(action: () -> Unit): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
            if (event.type == androidx.compose.ui.input.pointer.PointerEventType.Press && event.buttons.isSecondaryPressed) {
                event.changes.first().consume()
                action()
            }
        }
    }
}
