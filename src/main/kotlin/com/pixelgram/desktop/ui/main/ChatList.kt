package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.telegram.ChatListKey
import com.pixelgram.desktop.telegram.ChatState
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.telegram.contentText
import com.pixelgram.desktop.ui.components.ChatAvatar
import com.pixelgram.desktop.ui.toShape
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ChatList(
    listKey: ChatListKey,
    title: String,
    chats: List<ChatState>,
    selectedChatId: Long?,
    telegram: Telegram,
    settings: AppSettings,
    onOpen: (Long) -> Unit,
    archivedCount: Int,
    onOpenArchive: () -> Unit,
    onMenu: (() -> Unit)?,
    onBack: (() -> Unit)?,
) {
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
                    Icon(Icons.Filled.Menu, contentDescription = "Menu")
                }
            }
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f).padding(start = 4.dp),
            )
        }
        // Material 3 Expressive segmented groups: archive, pinned chats, the rest.
        val (pinned, rest) = chats.partition { it.pinned[listKey] == true }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            if (archivedCount > 0) {
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
                ChatRow(chat, index, pinned.size, chat.id == selectedChatId, telegram, settings) {
                    onOpen(chat.id)
                }
            }
            if (pinned.isNotEmpty() && rest.isNotEmpty()) {
                item(key = "pinned-gap") { Spacer(Modifier.height(GroupGap)) }
            }
            itemsIndexed(rest, key = { _, chat -> chat.id }) { index, chat ->
                ChatRow(chat, index, rest.size, chat.id == selectedChatId, telegram, settings) {
                    onOpen(chat.id)
                }
            }
        }
    }
}

private val GroupGap = 8.dp

@Composable
private fun ChatRow(
    chat: ChatState,
    index: Int,
    count: Int,
    selected: Boolean,
    telegram: Telegram,
    settings: AppSettings,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val last = chat.lastMessage
    SegmentedListItem(
        selected = selected,
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(index, count),
        leadingContent = {
            ChatAvatar(chat, telegram, settings, size = 48.dp)
        },
        supportingContent = {
            Text(
                text = last?.let { contentText(it.content).replace('\n', ' ') } ?: "",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = last?.let { formatChatDate(it.date) } ?: "",
                    style = MaterialTheme.typography.labelSmall,
                )
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
        Text(telegram.chatTitle(chat), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
private val dateFormat = DateTimeFormatter.ofPattern("dd.MM.yy")

fun formatTime(unixSeconds: Int): String =
    Instant.ofEpochSecond(unixSeconds.toLong()).atZone(ZoneId.systemDefault()).format(timeFormat)

fun formatChatDate(unixSeconds: Int): String {
    val time = Instant.ofEpochSecond(unixSeconds.toLong()).atZone(ZoneId.systemDefault())
    return if (time.toLocalDate() == LocalDate.now()) time.format(timeFormat) else time.format(dateFormat)
}
