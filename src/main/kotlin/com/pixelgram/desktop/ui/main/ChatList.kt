package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.telegram.ChatState
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.telegram.contentText
import com.pixelgram.desktop.ui.components.Avatar
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ChatList(
    title: String,
    chats: List<ChatState>,
    selectedChatId: Long?,
    telegram: Telegram,
    settings: AppSettings,
    onOpen: (Long) -> Unit,
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
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(chats, key = { it.id }) { chat ->
                ChatRow(
                    chat = chat,
                    selected = chat.id == selectedChatId,
                    telegram = telegram,
                    settings = settings,
                    onClick = { onOpen(chat.id) },
                )
            }
        }
    }
}

@Composable
private fun ChatRow(
    chat: ChatState,
    selected: Boolean,
    telegram: Telegram,
    settings: AppSettings,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val last = chat.lastMessage
    ListItem(
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        colors = ListItemDefaults.colors(
            containerColor = if (selected) colors.secondaryContainer else Color.Transparent,
        ),
        leadingContent = {
            Avatar(chat.title, chat.photo, telegram, settings, size = 48.dp)
        },
        headlineContent = {
            Text(chat.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(
                text = last?.let { contentText(it.content).replace('\n', ' ') } ?: "",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = colors.onSurfaceVariant,
            )
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = last?.let { formatChatDate(it.date) } ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
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
    )
}

private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
private val dateFormat = DateTimeFormatter.ofPattern("dd.MM.yy")

fun formatTime(unixSeconds: Int): String =
    Instant.ofEpochSecond(unixSeconds.toLong()).atZone(ZoneId.systemDefault()).format(timeFormat)

fun formatChatDate(unixSeconds: Int): String {
    val time = Instant.ofEpochSecond(unixSeconds.toLong()).atZone(ZoneId.systemDefault())
    return if (time.toLocalDate() == LocalDate.now()) time.format(timeFormat) else time.format(dateFormat)
}
