package com.pixelgram.desktop.ui.main

import androidx.compose.ui.input.pointer.isSecondaryPressed
import com.pixelgram.desktop.ui.icons.automirrored.outlined.Forward
import com.pixelgram.desktop.ui.icons.automirrored.outlined.OpenInNew
import com.pixelgram.desktop.ui.icons.outlined.Delete
import com.pixelgram.desktop.ui.icons.outlined.ChatBubbleOutline
import com.pixelgram.desktop.ui.icons.outlined.FolderOpen
import com.pixelgram.desktop.ui.icons.outlined.ContentCopy
import com.pixelgram.desktop.ui.components.MenuGroups
import com.pixelgram.desktop.ui.components.MenuEntry
import com.pixelgram.desktop.ui.components.GroupedMenu
import androidx.compose.ui.input.pointer.pointerInput
import com.pixelgram.desktop.ui.components.itemMotion
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.automirrored.rounded.ArrowBack
import com.pixelgram.desktop.ui.icons.rounded.Close
import com.pixelgram.desktop.ui.icons.rounded.Description
import com.pixelgram.desktop.ui.icons.rounded.Link
import com.pixelgram.desktop.ui.icons.rounded.PlayArrow
import com.pixelgram.desktop.ui.components.expressive.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import com.pixelgram.desktop.ui.components.expressive.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.pixelgram.desktop.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import com.pixelgram.desktop.ui.components.groupShapes
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.telegram.UiMessage
import com.pixelgram.desktop.telegram.contentText
import com.pixelgram.desktop.ui.components.Avatar
import com.pixelgram.desktop.ui.components.ChatAvatar
import com.pixelgram.desktop.ui.theme.EmojiText
import it.tdlight.jni.TdApi
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

enum class MediaTab(val title: String, val grid: Boolean) {
    Photos("Photos", true),
    Videos("Videos", true),
    Files("Files", false),
    Links("Links", false),
    Music("Music", false),
    Voice("Voice", false),
    Gifs("GIFs", true);

    fun filter(): TdApi.SearchMessagesFilter = when (this) {
        Photos -> TdApi.SearchMessagesFilterPhoto()
        Videos -> TdApi.SearchMessagesFilterVideo()
        Files -> TdApi.SearchMessagesFilterDocument()
        Links -> TdApi.SearchMessagesFilterUrl()
        Music -> TdApi.SearchMessagesFilterAudio()
        Voice -> TdApi.SearchMessagesFilterVoiceNote()
        Gifs -> TdApi.SearchMessagesFilterAnimation()
    }
}

@Composable
fun PillTabs(tabs: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    val colors = MaterialTheme.colorScheme
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.horizontalScroll(rememberScrollState()),
    ) {
        tabs.forEachIndexed { index, title ->
            val active = index == selected
            val container by animateColorAsState(if (active) colors.secondaryContainer else colors.secondaryContainer.copy(alpha = 0f))
            val content by animateColorAsState(if (active) colors.onSecondaryContainer else colors.onSurfaceVariant)
            Surface(onClick = { onSelect(index) }, shape = CircleShape, color = container, contentColor = content) {
                EmojiText(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    color = content,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
        trailing()
    }
}

class MediaActions(
    val onGoTo: (UiMessage) -> Unit,
    val onForward: (UiMessage) -> Unit,
)

@Composable
fun rememberMediaActions(telegram: Telegram, settings: AppSettings, onOpenChat: ((Long) -> Unit)?, onClose: () -> Unit): MediaActions {
    var forwarding by remember { mutableStateOf<UiMessage?>(null) }
    forwarding?.let { message ->
        ForwardDialog(telegram, settings, onDismiss = { forwarding = null }) { target ->
            PendingForward.start(target, message.chatId, listOf(message))
            forwarding = null
            onClose()
            onOpenChat?.invoke(target)
        }
    }
    return remember(onOpenChat) {
        MediaActions(
            onGoTo = { message ->
                onClose()
                onOpenChat?.invoke(message.chatId)
                MessageSearch.jump = message.chatId to message.id
            },
            onForward = { forwarding = it },
        )
    }
}

@Composable
fun SharedMediaPane(
    chatId: Long,
    tab: MediaTab,
    telegram: Telegram,
    modifier: Modifier = Modifier,
    savedTopicId: Long = 0L,
    actions: MediaActions? = null,
) {
    val items = remember(chatId, tab, savedTopicId) { mutableStateListOf<UiMessage>() }
    var next by remember(chatId, tab, savedTopicId) { mutableLongStateOf(0L) }
    var done by remember(chatId, tab, savedTopicId) { mutableStateOf(false) }
    var loading by remember(chatId, tab, savedTopicId) { mutableStateOf(false) }
    val loadMore = {
        if (!loading && !done) {
            loading = true
            telegram.searchMedia(chatId, tab.filter(), next, savedTopicId) { found, from ->
                val known = items.mapTo(HashSet()) { it.id }
                items.addAll(found.filter { it.id !in known })
                next = from
                done = from == 0L || found.isEmpty()
                loading = false
            }
        }
    }
    LaunchedEffect(chatId, tab, savedTopicId) { loadMore() }
    val delete = { message: UiMessage ->
        telegram.deleteMessages(message.chatId, longArrayOf(message.id), false)
        items.removeAll { it.id == message.id }
        Unit
    }
    Box(modifier) {
        when {
            items.isEmpty() && loading -> LoadingIndicator(Modifier.align(Alignment.Center))
            items.isEmpty() -> Text(
                "Nothing here yet",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center),
            )
            tab.grid -> MediaGrid(items, tab, telegram, actions, onDelete = delete, onEnd = loadMore)
            else -> MediaList(items, tab, telegram, actions, onDelete = delete, onEnd = loadMore)
        }
    }
}

@Composable
private fun MediaGrid(items: List<UiMessage>, tab: MediaTab, telegram: Telegram, actions: MediaActions?, onDelete: (UiMessage) -> Unit, onEnd: () -> Unit) {
    val state = rememberLazyGridState()
    val nearEnd by remember { derivedStateOf { (state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= state.layoutInfo.totalItemsCount - 6 } }
    LaunchedEffect(nearEnd, items.size) { if (nearEnd) onEnd() }
    val months = remember(items.size) { items.groupBy { monthOf(it.date) } }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(GridCell),
        state = state,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        months.forEach { (month, list) ->
            item(key = "m$month", span = { GridItemSpan(maxLineSpan) }) { MonthHeader(month) }
            items(list, key = { it.id }) { message -> Box(itemMotion()) { MediaCell(message, tab, telegram, actions, onDelete) } }
        }
    }
}

@Composable
private fun MediaCell(message: UiMessage, tab: MediaTab, telegram: Telegram, actions: MediaActions?, onDelete: (UiMessage) -> Unit) {
    val thumb = replyThumb(message)
    val content = message.content
    var menu by remember { mutableStateOf(false) }
    if (menu) MediaMenu(message, tab, telegram, actions, onDelete, link = null, open = null) { menu = false }
    Box(
        Modifier
            .secondaryClick { menu = true }
            .aspectRatio(1f)
            .clip(RoundedCornerShape(CellCorner))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable {
                when {
                    content is TdApi.MessageVideo -> VideoPlayer.open(telegram, content.video, message.text)
                    content is TdApi.MessageAnimation -> VideoPlayer.open(telegram, content.animation, message.text)
                    message.photo != null -> PhotoViewer.current = ViewedPhoto(telegram, message.photo, message.fullPhoto ?: message.photo, message.text)
                }
            },
    ) {
        if (thumb != null) ThumbImage(thumb.first, thumb.second, telegram, Modifier.fillMaxSize())
        if (tab == MediaTab.Videos && content is TdApi.MessageVideo) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f),
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                modifier = Modifier.align(Alignment.BottomStart).padding(6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, end = 8.dp, top = 2.dp, bottom = 2.dp)) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(formatDuration(content.video.duration), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun MediaList(items: List<UiMessage>, tab: MediaTab, telegram: Telegram, actions: MediaActions?, onDelete: (UiMessage) -> Unit, onEnd: () -> Unit) {
    val state = rememberLazyListState()
    val nearEnd by remember { derivedStateOf { (state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= state.layoutInfo.totalItemsCount - 4 } }
    LaunchedEffect(nearEnd, items.size) { if (nearEnd) onEnd() }
    LazyColumn(state = state, verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxSize()) {
        itemsIndexed(items, key = { _, message -> message.id }) { index, message ->
            Box(itemMotion()) { MediaRow(message, tab, telegram, groupShape(index, items.size), actions, onDelete) }
        }
    }
}

@Composable
fun groupShape(index: Int, count: Int): Shape = groupShapes(index, count).shape

@Composable
private fun MediaRow(message: UiMessage, tab: MediaTab, telegram: Telegram, shape: Shape, actions: MediaActions?, onDelete: (UiMessage) -> Unit) {
    val content = message.content
    val colors = MaterialTheme.colorScheme
    val link = remember(message.id) { UrlPattern.find(message.text)?.value }
    val document = (content as? TdApi.MessageDocument)?.document
    val (busy, open) = rememberOpener(document?.document, telegram) { path -> openDocument(path) }
    var menu by remember { mutableStateOf(false) }
    if (menu) MediaMenu(message, tab, telegram, actions, onDelete, link, open = if (document != null) open else null) { menu = false }
    Surface(
        onClick = { menu = true },
        shape = shape,
        color = colors.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            when (content) {
                is TdApi.MessageAudio -> PlayButton(message.id, message.chatId, audioTitle(content.audio), content.audio.audio, telegram, content.audio)
                is TdApi.MessageVoiceNote -> PlayButton(message.id, message.chatId, "Voice message", content.voiceNote.voice, telegram)
                else -> Box(
                    Modifier.size(RowIcon).clip(RoundedCornerShape(14.dp)).background(colors.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    if (busy) {
                        LoadingIndicator(Modifier.size(28.dp), color = colors.onPrimaryContainer)
                    } else {
                        Icon(
                            if (tab == MediaTab.Links) Icons.Rounded.Link else Icons.Rounded.Description,
                            contentDescription = null,
                            tint = colors.onPrimaryContainer,
                        )
                    }
                }
            }
            Column(Modifier.weight(1f)) {
                val title = when (content) {
                    is TdApi.MessageDocument -> content.document.fileName
                    is TdApi.MessageAudio -> audioTitle(content.audio)
                    is TdApi.MessageVoiceNote -> "Voice message · ${formatDuration(content.voiceNote.duration)}"
                    else -> link ?: message.text
                }
                val subtitle = when (content) {
                    is TdApi.MessageDocument -> "${formatSize(content.document.document.size)} · ${dayOf(message.date)}"
                    is TdApi.MessageAudio -> "${formatDuration(content.audio.duration)} · ${dayOf(message.date)}"
                    else -> if (tab == MediaTab.Links) message.text.lineSequence().firstOrNull { it != link }.orEmpty() else dayOf(message.date)
                }
                EmojiText(title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
                if (subtitle.isNotBlank()) {
                    EmojiText(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun MediaMenu(
    message: UiMessage,
    tab: MediaTab,
    telegram: Telegram,
    actions: MediaActions?,
    onDelete: (UiMessage) -> Unit,
    link: String?,
    open: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val act = { action: () -> Unit -> { onDismiss(); action() } }
    val file = when (val content = message.content) {
        is TdApi.MessageDocument -> content.document.document
        is TdApi.MessageAudio -> content.audio.audio
        is TdApi.MessageVoiceNote -> content.voiceNote.voice
        is TdApi.MessageVideo -> content.video.video
        is TdApi.MessageAnimation -> content.animation.animation
        else -> message.fullPhoto ?: message.photo
    }
    val path = telegram.localPath(file)
    val first = buildList {
        if (link != null && tab == MediaTab.Links) {
            add(MenuEntry(Icons.AutoMirrored.Outlined.OpenInNew, "Open link", onClick = act { openUrl(link) }))
            add(MenuEntry(Icons.Outlined.ContentCopy, "Copy link", onClick = act {
                java.awt.Toolkit.getDefaultToolkit().systemClipboard.setContents(java.awt.datatransfer.StringSelection(link), null)
            }))
        }
        if (open != null) add(MenuEntry(Icons.AutoMirrored.Outlined.OpenInNew, "Open", onClick = act(open)))
        if (path != null) add(MenuEntry(Icons.Outlined.FolderOpen, "Show in folder", onClick = act { showInFolder(path) }))
    }
    val second = buildList {
        if (actions != null) {
            add(MenuEntry(Icons.Outlined.ChatBubbleOutline, "Go to message", onClick = act { actions.onGoTo(message) }))
            add(MenuEntry(Icons.AutoMirrored.Outlined.Forward, "Forward", onClick = act { actions.onForward(message) }))
        }
    }
    val third = listOf(MenuEntry(Icons.Outlined.Delete, "Delete", color = MaterialTheme.colorScheme.error, onClick = act { onDelete(message) }))
    GroupedMenu(expanded = true, onDismiss = onDismiss) {
        MenuGroups(listOf(first, second, third))
    }
}

private fun Modifier.secondaryClick(onClick: () -> Unit): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            if (event.type == androidx.compose.ui.input.pointer.PointerEventType.Press && event.buttons.isSecondaryPressed) {
                event.changes.forEach { it.consume() }
                onClick()
            }
        }
    }
}

@Composable
private fun MonthHeader(month: String) {
    Text(
        month,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 6.dp),
    )
}

@Composable
fun CommonGroupsPane(userId: Long, telegram: Telegram, settings: AppSettings, onOpenChat: ((Long) -> Unit)?, modifier: Modifier = Modifier) {
    var groups by remember(userId) { mutableStateOf<List<Long>?>(null) }
    LaunchedEffect(userId) { telegram.groupsInCommon(userId) { groups = it } }
    Box(modifier) {
        val list = groups
        when {
            list == null -> LoadingIndicator(Modifier.align(Alignment.Center))
            list.isEmpty() -> Text("No groups in common", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.Center))
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxSize()) {
                itemsIndexed(list) { index, id ->
                    val chat = telegram.chats[id] ?: return@itemsIndexed
                    Surface(
                        onClick = { onOpenChat?.invoke(id) },
                        shape = groupShape(index, list.size),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            ChatAvatar(chat, telegram, settings, RowIcon)
                            EmojiText(telegram.chatTitle(chat), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SharedMediaSection(chat: com.pixelgram.desktop.telegram.ChatState, telegram: Telegram, settings: AppSettings, onOpenChat: ((Long) -> Unit)?, modifier: Modifier = Modifier) {
    val actions = rememberMediaActions(telegram, settings, onOpenChat, onClose = {})
    val user = chat.privateUserId != 0L && !telegram.isSavedMessages(chat.id) && !telegram.isBot(chat)
    val titles = MediaTab.entries.map { it.title } + if (user) listOf("Groups") else emptyList()
    var selected by remember(chat.id) { mutableIntStateOf(0) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PillTabs(titles, selected, { selected = it })
        TabContent(selected, Modifier.fillMaxWidth().height(SectionHeight)) { index ->
            val tab = MediaTab.entries.getOrNull(index)
            if (tab != null) {
                SharedMediaPane(chat.id, tab, telegram, Modifier.fillMaxSize(), actions = actions)
            } else {
                CommonGroupsPane(chat.privateUserId, telegram, settings, onOpenChat, Modifier.fillMaxSize())
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SavedMessagesDialog(
    chat: com.pixelgram.desktop.telegram.ChatState,
    telegram: Telegram,
    settings: AppSettings,
    onOpenChat: ((Long) -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    LaunchedEffect(Unit) { telegram.loadSavedTopics() }
    val actions = rememberMediaActions(telegram, settings, onOpenChat, onClose = onDismiss)
    var topic by remember { mutableStateOf<TdApi.SavedMessagesTopic?>(null) }
    var selected by remember { mutableIntStateOf(0) }
    val topics = telegram.savedTopics.values.filter { it.order != 0L }.sortedWith(compareByDescending<TdApi.SavedMessagesTopic> { it.isPinned }.thenByDescending { it.order })
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.widthIn(min = 360.dp, max = 480.dp).height(DialogHeight),
        ) {
            AnimatedContent(
                targetState = topic,
                transitionSpec = {
                    val forward = targetState != null
                    (slideInHorizontally { if (forward) it / 4 else -it / 4 } + fadeIn()) togetherWith
                        (slideOutHorizontally { if (forward) -it / 4 else it / 4 } + fadeOut())
                },
            ) { opened ->
                if (opened != null) {
                    SavedTopicView(chat, opened, telegram, settings, onBack = { topic = null })
                    return@AnimatedContent
                }
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).padding(start = 8.dp)) {
                            Text("Saved Messages", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                            Text("${topics.size} chats", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        }
                        IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, contentDescription = "Close") }
                    }
                    PillTabs(listOf("Chats") + MediaTab.entries.map { it.title }, selected, { selected = it })
                    TabContent(selected, Modifier.weight(1f)) { shownTab ->
                        if (shownTab == 0) {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxSize()) {
                                itemsIndexed(topics, key = { _, item -> item.id }) { index, item ->
                                    Box(itemMotion()) { SavedTopicRow(item, telegram, settings, groupShape(index, topics.size)) { topic = item } }
                                }
                            }
                        } else {
                            SharedMediaPane(chat.id, MediaTab.entries[shownTab - 1], telegram, Modifier.fillMaxSize(), actions = actions)
                        }
                    }
                }
            }
        }
    }
}

private fun topicTitle(topic: TdApi.SavedMessagesTopic, telegram: Telegram): String = when (val type = topic.type) {
    is TdApi.SavedMessagesTopicTypeMyNotes -> "My Notes"
    is TdApi.SavedMessagesTopicTypeAuthorHidden -> "Author Hidden"
    is TdApi.SavedMessagesTopicTypeSavedFromChat -> telegram.chats[type.chatId]?.let(telegram::chatTitle) ?: "Chat"
    else -> "Chat"
}

@Composable
private fun TopicAvatar(topic: TdApi.SavedMessagesTopic, telegram: Telegram, settings: AppSettings) {
    val type = topic.type
    val chat = (type as? TdApi.SavedMessagesTopicTypeSavedFromChat)?.let { telegram.chats[it.chatId] }
    LaunchedEffect(type) { (type as? TdApi.SavedMessagesTopicTypeSavedFromChat)?.let { telegram.loadChat(it.chatId) } }
    if (chat != null) ChatAvatar(chat, telegram, settings, RowIcon) else Avatar(topicTitle(topic, telegram), null, telegram, settings, RowIcon)
}

@Composable
private fun SavedTopicRow(topic: TdApi.SavedMessagesTopic, telegram: Telegram, settings: AppSettings, shape: Shape, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(onClick = onClick, shape = shape, color = colors.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            TopicAvatar(topic, telegram, settings)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EmojiText(
                        topicTitle(topic, telegram),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f),
                    )
                    topic.lastMessage?.let { Text(formatChatDate(it.date), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant) }
                }
                val preview = topic.lastMessage?.content?.let(::contentText).orEmpty()
                if (preview.isNotBlank()) {
                    EmojiText(preview, maxLines = 1, overflow = TextOverflow.Ellipsis, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun SavedTopicView(chat: com.pixelgram.desktop.telegram.ChatState, topic: TdApi.SavedMessagesTopic, telegram: Telegram, settings: AppSettings, onBack: () -> Unit) {
    val messages = remember(topic.id) { mutableStateListOf<UiMessage>() }
    var loading by remember(topic.id) { mutableStateOf(false) }
    var done by remember(topic.id) { mutableStateOf(false) }
    val loadMore = {
        if (!loading && !done) {
            loading = true
            telegram.savedTopicHistory(topic.id, messages.lastOrNull()?.id ?: 0L) { page ->
                val known = messages.mapTo(HashSet()) { it.id }
                val fresh = page.filter { it.id !in known }
                messages.addAll(fresh)
                done = fresh.isEmpty()
                loading = false
            }
        }
    }
    val state = rememberLazyListState()
    val nearEnd by remember { derivedStateOf { (state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= state.layoutInfo.totalItemsCount - 5 } }
    LaunchedEffect(nearEnd, messages.size) { if (nearEnd) loadMore() }
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
            TopicAvatar(topic, telegram, settings)
            EmojiText(
                topicTitle(topic, telegram),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
        }
        Box(Modifier.weight(1f)) {
            if (messages.isEmpty() && loading) LoadingIndicator(Modifier.align(Alignment.Center))
            LazyColumn(
                state = state,
                reverseLayout = true,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(message, null, settings, modifier = itemMotion(), telegram = telegram)
                }
            }
        }
    }
}

@Composable
private fun TabContent(selected: Int, modifier: Modifier, content: @Composable (Int) -> Unit) {
    val slide = MaterialTheme.motionScheme.defaultSpatialSpec<androidx.compose.ui.unit.IntOffset>()
    val fade = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    AnimatedContent(
        targetState = selected,
        transitionSpec = {
            val forward = targetState > initialState
            (slideInHorizontally(slide) { if (forward) it / 3 else -it / 3 } + fadeIn(fade)) togetherWith
                (slideOutHorizontally(slide) { if (forward) -it / 3 else it / 3 } + fadeOut(fade))
        },
        modifier = modifier,
    ) { content(it) }
}

private val UrlPattern = Regex("""https?://\S+|t\.me/\S+""")

private val monthFormat = DateTimeFormatter.ofPattern("LLLL yyyy", Locale.ENGLISH)

private fun monthOf(unixSeconds: Int): String {
    val date = Instant.ofEpochSecond(unixSeconds.toLong()).atZone(ZoneId.systemDefault())
    return if (date.year == java.time.Year.now().value) date.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.ENGLISH) else date.format(monthFormat)
}

private fun dayOf(unixSeconds: Int): String = formatChatDate(unixSeconds)

private val GridCell = 96.dp
private val CellCorner = 6.dp
private val RowIcon = 44.dp
private val SectionHeight = 360.dp
private val DialogHeight = 640.dp
