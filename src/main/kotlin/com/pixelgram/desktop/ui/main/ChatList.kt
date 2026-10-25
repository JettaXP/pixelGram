package com.pixelgram.desktop.ui.main

import com.pixelgram.desktop.ui.components.itemMotion
import com.pixelgram.desktop.ui.components.groupShapes
import com.pixelgram.desktop.ui.components.expressive.FilledTonalIconButton
import com.pixelgram.desktop.ui.icons.outlined.SystemUpdate
import com.pixelgram.desktop.ui.UpdateDialog
import com.pixelgram.desktop.core.UpdateState
import com.pixelgram.desktop.core.Updater
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import com.pixelgram.desktop.ui.icons.filled.Search
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.fillMaxSize
import com.pixelgram.desktop.telegram.MessageStatus
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.pixelgram.desktop.ui.icons.outlined.Lock
import com.pixelgram.desktop.ui.icons.outlined.LockOpen
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.automirrored.filled.ArrowBack
import com.pixelgram.desktop.ui.icons.automirrored.filled.MenuOpen
import com.pixelgram.desktop.ui.icons.filled.Menu
import com.pixelgram.desktop.ui.icons.outlined.Archive
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import com.pixelgram.desktop.ui.components.expressive.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.telegram.ChatListKey
import com.pixelgram.desktop.telegram.ChatState
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.telegram.contentText
import com.pixelgram.desktop.ui.components.ChatAvatar
import com.pixelgram.desktop.ui.theme.EmojiText
import com.pixelgram.desktop.ui.toShape
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ChatList(
    listKey: ChatListKey,
    title: String,
    chatsFor: (ChatListKey) -> List<ChatState>,
    order: (ChatListKey) -> Int,
    vertical: Boolean,
    compact: Boolean,
    selectedChatId: Long?,
    telegram: Telegram,
    settings: AppSettings,
    onOpen: (Long) -> Unit,
    archivedCount: Int,
    onOpenArchive: () -> Unit,
    onMenu: (() -> Unit)?,
    onBack: (() -> Unit)?,
    menuOpen: Boolean = false,
    onSearch: (() -> Unit)? = null,
    belowHeader: @Composable () -> Unit = {},
    archiveOn: ChatListKey = ChatListKey.Main,
) {
    Box(Modifier.fillMaxSize()) {
        ChatListBody(
            listKey, title, chatsFor, order, vertical, compact, selectedChatId, telegram, settings,
            onOpen, archivedCount, onOpenArchive, onMenu, onBack, menuOpen, onSearch, belowHeader, archiveOn,
        )
        ConnectionPill(
            status = telegram.connection,
            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
        )
    }
}

@Composable
private fun ChatListBody(
    listKey: ChatListKey,
    title: String,
    chatsFor: (ChatListKey) -> List<ChatState>,
    order: (ChatListKey) -> Int,
    vertical: Boolean,
    compact: Boolean,
    selectedChatId: Long?,
    telegram: Telegram,
    settings: AppSettings,
    onOpen: (Long) -> Unit,
    archivedCount: Int,
    onOpenArchive: () -> Unit,
    onMenu: (() -> Unit)?,
    onBack: (() -> Unit)?,
    menuOpen: Boolean = false,
    onSearch: (() -> Unit)? = null,
    belowHeader: @Composable () -> Unit = {},
    archiveOn: ChatListKey = ChatListKey.Main,
) {
    if (compact) {
        CompactChatList(
            listKey, chatsFor, selectedChatId, telegram, settings,
            onOpen, archivedCount, onOpenArchive, onMenu, onBack, menuOpen, archiveOn,
        )
        return
    }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when {
                onBack != null -> IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                onMenu != null -> IconButton(onClick = onMenu) {
                    Icon(menuIcon(menuOpen), contentDescription = "Menu")
                }
            }
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f).padding(start = 4.dp),
            )
            if (Updater.state is UpdateState.Available || Updater.state is UpdateState.Ready || Updater.state is UpdateState.Downloading) {
                var updateOpen by remember { mutableStateOf(false) }
                FilledTonalIconButton(onClick = { updateOpen = true }) {
                    Icon(Icons.Outlined.SystemUpdate, contentDescription = "Update available")
                }
                if (updateOpen) UpdateDialog(onBeforeExit = {}, onDismiss = { updateOpen = false })
            }
            if (onSearch != null) {
                if (com.pixelgram.desktop.core.AppLock.enabled) LockButton()
                IconButton(onClick = onSearch) { Icon(Icons.Filled.Search, contentDescription = "Search messages") }
            }
        }
        belowHeader()
        val forward = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
        val fade = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
        AnimatedContent(
            targetState = listKey,
            transitionSpec = {
                val direction = if (order(targetState) >= order(initialState)) 1 else -1
                if (vertical) {
                    (slideInVertically(forward) { it / 6 * direction } + fadeIn(fade)) togetherWith
                        (slideOutVertically(forward) { -it / 6 * direction } + fadeOut(fade))
                } else {
                    (slideInHorizontally(forward) { it / 4 * direction } + fadeIn(fade)) togetherWith
                        (slideOutHorizontally(forward) { -it / 4 * direction } + fadeOut(fade))
                }
            },
        ) { key ->
            val (pinned, rest) = chatsFor(key).partition { it.pinned[key] == true }
            val listState = rememberLazyListState()
            val topChat = rest.firstOrNull()?.id
            LaunchedEffect(topChat) {
                withFrameNanos { }
                if (listState.firstVisibleItemIndex in 1..FollowTopItems) listState.animateScrollToItem(0)
            }
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                if (archivedCount > 0 && key == archiveOn) {
                    item(key = "archive") {
                        SegmentedListItem(
                            onClick = onOpenArchive,
                            shapes = ListItemDefaults.shapes(shape = MaterialTheme.shapes.large),
                            leadingContent = {
                                Box(
                                    Modifier
                                        .size(48.dp)
                                        .clip(settings.avatarShape.value.toShape(settings.avatarCorner.value))
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Outlined.Archive,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                }
                            },
                            supportingContent = { Text("$archivedCount chats") },
                        ) {
                            Text("Archived chats")
                        }
                    }
                    item(key = "archive-gap") { Spacer(Modifier.height(GroupGap)) }
                }
                itemsIndexed(pinned, key = { _, chat -> chat.id }) { index, chat ->
                    ChatRow(chat, index, pinned.size, chat.id == selectedChatId, telegram, settings, key, itemMotion()) {
                        onOpen(chat.id)
                    }
                }
                if (pinned.isNotEmpty() && rest.isNotEmpty()) {
                    item(key = "pinned-gap") { Spacer(Modifier.height(GroupGap)) }
                }
                itemsIndexed(rest, key = { _, chat -> chat.id }) { index, chat ->
                    ChatRow(chat, index, rest.size, chat.id == selectedChatId, telegram, settings, key, itemMotion()) {
                        onOpen(chat.id)
                    }
                }
            }
        }
    }
}

private val GroupGap = 8.dp
private const val FollowTopItems = 4

@Composable
private fun OnlineAvatar(chat: ChatState, telegram: Telegram, settings: AppSettings, size: Dp) {
    val online = telegram.isOnline(chat)
    val dot by animateFloatAsState(if (online) 1f else 0f, MaterialTheme.motionScheme.fastSpatialSpec())
    Box {
        ChatAvatar(chat, telegram, settings, size = size)
        if (dot > 0f) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(OnlineDot)
                    .graphicsLayer {
                        scaleX = dot
                        scaleY = dot
                    }
                    .border(OnlineRing, MaterialTheme.colorScheme.surfaceContainerLow, CircleShape)
                    .padding(OnlineRing)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
            )
        }
    }
}

private val OnlineDot = 15.dp
private val OnlineRing = 2.5.dp

private fun menuIcon(open: Boolean) = if (open) Icons.AutoMirrored.Filled.MenuOpen else Icons.Filled.Menu

@Composable
private fun CompactChatList(
    listKey: ChatListKey,
    chatsFor: (ChatListKey) -> List<ChatState>,
    selectedChatId: Long?,
    telegram: Telegram,
    settings: AppSettings,
    onOpen: (Long) -> Unit,
    archivedCount: Int,
    onOpenArchive: () -> Unit,
    onMenu: (() -> Unit)?,
    onBack: (() -> Unit)?,
    menuOpen: Boolean,
    archiveOn: ChatListKey,
) {
    val colors = MaterialTheme.colorScheme
    val shape = settings.avatarShape.value.toShape(settings.avatarCorner.value)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        when {
            onBack != null -> IconButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            onMenu != null -> IconButton(onClick = onMenu, modifier = Modifier.padding(top = 8.dp)) {
                Icon(menuIcon(menuOpen), contentDescription = "Menu")
            }
        }
        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (archivedCount > 0 && listKey == archiveOn) {
                item(key = "archive") {
                    Box(
                        Modifier
                            .size(CompactItemSize)
                            .clip(MaterialTheme.shapes.large)
                            .clickable(onClick = onOpenArchive),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier.size(48.dp).clip(shape).background(colors.secondaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Outlined.Archive, contentDescription = "Archived chats", tint = colors.onSecondaryContainer)
                        }
                    }
                }
            }
            items(chatsFor(listKey), key = { it.id }) { chat ->
                Box(
                    itemMotion()
                        .size(CompactItemSize)
                        .clip(MaterialTheme.shapes.large)
                        .background(if (chat.id == selectedChatId) colors.secondaryContainer else Color.Transparent)
                        .clickable { onOpen(chat.id) },
                    contentAlignment = Alignment.Center,
                ) {
                    OnlineAvatar(chat, telegram, settings, 48.dp)
                    if (chat.unreadCount > 0) {
                        Badge(
                            modifier = Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 4.dp),
                            containerColor = colors.primary,
                            contentColor = colors.onPrimary,
                        ) {
                            Text(if (chat.unreadCount > 99) "99+" else chat.unreadCount.toString())
                        }
                    }
                }
            }
        }
    }
}

private val CompactItemSize = 64.dp

@Composable
private fun ChatRow(
    chat: ChatState,
    index: Int,
    count: Int,
    selected: Boolean,
    telegram: Telegram,
    settings: AppSettings,
    listKey: ChatListKey,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val last = chat.lastMessage
    var menuAt by remember { mutableStateOf<DpOffset?>(null) }
    val density = LocalDensity.current
    Box(
        modifier.pointerInput(chat.id) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                        val change = event.changes.first()
                        change.consume()
                        menuAt = with(density) { DpOffset(change.position.x.toDp(), change.position.y.toDp()) }
                    }
                }
            }
        },
    ) {
        ChatActions(chat, telegram, settings, menuAt, onDismiss = { menuAt = null }, listKey = listKey)
        SegmentedListItem(
            selected = selected,
            onClick = onClick,
            shapes = groupShapes(index, count),
            leadingContent = {
                OnlineAvatar(chat, telegram, settings, 48.dp)
            },
            supportingContent = {
                val draft = chat.draft.takeIf { it.isNotBlank() && !selected }
                if (draft != null) {
                    Row {
                        Text("Draft: ", color = colors.error, maxLines = 1)
                        EmojiText(draft.replace('\n', ' '), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                } else {
                    EmojiText(
                        last?.let { contentText(it.content).replace('\n', ' ') } ?: "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
            trailingContent = {
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        if (last != null && last.isOutgoing && !chat.isChannel) {
                            val status = telegram.status(chat, last)
                            MessageStatusIcon(
                                status,
                                color = if (status == MessageStatus.Read) colors.primary else colors.onSurfaceVariant,
                                size = 16.dp,
                            )
                        }
                        Text(
                            text = last?.let { formatChatDate(it.date) } ?: "",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    if (chat.unreadCount > 0) {
                        Badge(
                            modifier = Modifier.padding(top = 6.dp),
                            containerColor = colors.primary,
                            contentColor = colors.onPrimary,
                        ) {
                            Text(if (chat.unreadCount > 999) "999+" else chat.unreadCount.toString())
                        }
                    }
                }
            },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                EmojiText(telegram.chatTitle(chat), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                com.pixelgram.desktop.ui.components.ChatBadges(chat, telegram, 18.dp)
            }
        }
    }
}

private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
private val dateFormat = DateTimeFormatter.ofPattern("dd.MM.yy")

fun formatMessageTime(unixSeconds: Int, seconds: Boolean): String =
    Instant.ofEpochSecond(unixSeconds.toLong()).atZone(ZoneId.systemDefault())
        .format(if (seconds) messageTimeFormat else timeFormat)

private val messageTimeFormat = DateTimeFormatter.ofPattern("HH:mm:ss")

fun formatTime(unixSeconds: Int): String =
    Instant.ofEpochSecond(unixSeconds.toLong()).atZone(ZoneId.systemDefault()).format(timeFormat)

fun formatChatDate(unixSeconds: Int): String {
    val time = Instant.ofEpochSecond(unixSeconds.toLong()).atZone(ZoneId.systemDefault())
    return if (time.toLocalDate() == LocalDate.now()) time.format(timeFormat) else time.format(dateFormat)
}

@Composable
private fun LockButton() {
    var closing by remember { mutableStateOf(false) }
    LaunchedEffect(closing) {
        if (closing) {
            kotlinx.coroutines.delay(LockAnimationMs)
            com.pixelgram.desktop.core.AppLock.lock()
            closing = false
        }
    }
    IconButton(onClick = { closing = true }) {
        androidx.compose.animation.AnimatedContent(
            targetState = closing,
            transitionSpec = {
                (androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(initialScale = 0.6f)) togetherWith
                    (androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(targetScale = 1.3f))
            },
        ) { locked ->
            Icon(
                if (locked) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                contentDescription = "Lock pixelGram",
            )
        }
    }
}

private const val LockAnimationMs = 260L
