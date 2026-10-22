package com.pixelgram.desktop.ui.main

import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
) {
    Box(Modifier.fillMaxSize()) {
        ChatListBody(
            listKey, title, chatsFor, order, vertical, compact, selectedChatId, telegram, settings,
            onOpen, archivedCount, onOpenArchive, onMenu, onBack, menuOpen, onSearch,
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
) {
    if (compact) {
        CompactChatList(
            listKey, chatsFor, selectedChatId, telegram, settings,
            onOpen, archivedCount, onOpenArchive, onMenu, onBack, menuOpen,
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
            if (onSearch != null) {
                IconButton(onClick = onSearch) { Icon(Icons.Filled.Search, contentDescription = "Search messages") }
            }
        }
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
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                if (archivedCount > 0 && key == ChatListKey.Main) {
                    item(key = "archive") {
                        SegmentedListItem(
                            onClick = onOpenArchive,
                            shapes = ListItemDefaults.segmentedShapes(0, 1),
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
                    ChatRow(chat, index, pinned.size, chat.id == selectedChatId, telegram, settings, key) {
                        onOpen(chat.id)
                    }
                }
                if (pinned.isNotEmpty() && rest.isNotEmpty()) {
                    item(key = "pinned-gap") { Spacer(Modifier.height(GroupGap)) }
                }
                itemsIndexed(rest, key = { _, chat -> chat.id }) { index, chat ->
                    ChatRow(chat, index, rest.size, chat.id == selectedChatId, telegram, settings, key) {
                        onOpen(chat.id)
                    }
                }
            }
        }
    }
}

private val GroupGap = 8.dp

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
            if (archivedCount > 0 && listKey == ChatListKey.Main) {
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
                    Modifier
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
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val last = chat.lastMessage
    var menuAt by remember { mutableStateOf<DpOffset?>(null) }
    val density = LocalDensity.current
    Box(
        Modifier.pointerInput(chat.id) {
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
            shapes = ListItemDefaults.segmentedShapes(index, count),
            leadingContent = {
                OnlineAvatar(chat, telegram, settings, 48.dp)
            },
            supportingContent = {
                EmojiText(
                    last?.let { contentText(it.content).replace('\n', ' ') } ?: "",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
            EmojiText(telegram.chatTitle(chat), maxLines = 1, overflow = TextOverflow.Ellipsis)
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
