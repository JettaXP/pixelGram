package com.pixelgram.desktop.ui.profile

import com.pixelgram.desktop.ui.components.groupShapes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import com.pixelgram.desktop.ui.components.expressive.AlertDialog
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.components.expressive.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.telegram.contentText
import com.pixelgram.desktop.ui.components.ChatAvatar
import com.pixelgram.desktop.ui.theme.EmojiText

@Composable
fun PersonalChannelCard(
    chatId: Long,
    telegram: Telegram,
    settings: AppSettings,
    onOpen: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(chatId) { telegram.loadChat(chatId) }
    val channel = telegram.chats[chatId] ?: return
    SegmentedListItem(
        onClick = { onOpen(chatId) },
        shapes = groupShapes(0, 1),
        modifier = modifier,
        leadingContent = { ChatAvatar(channel, telegram, settings, size = 40.dp) },
        overlineContent = { Text("Personal channel") },
        supportingContent = channel.lastMessage?.let { last ->
            {
                EmojiText(
                    contentText(last.content).replace('\n', ' '),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
    ) {
        EmojiText(channel.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun PersonalChannelPicker(
    telegram: Telegram,
    settings: AppSettings,
    onPicked: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var ids by remember { mutableStateOf<List<Long>?>(null) }
    LaunchedEffect(Unit) { telegram.suitablePersonalChats { ids = it } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Personal channel") },
        text = {
            val loaded = ids
            when {
                loaded == null -> LoadingIndicator()
                loaded.isEmpty() -> Text(
                    "You have no public channels to show. Create a channel and give it a public link first.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> Column {
                    Text(
                        "The channel and its latest post appear on your profile.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    val channels = loaded.mapNotNull { telegram.chats[it] }
                    LazyColumn(Modifier.heightIn(max = 360.dp).fillMaxWidth()) {
                        itemsIndexed(channels, key = { _, it -> it.id }) { index, channel ->
                            SegmentedListItem(
                                onClick = { onPicked(channel.id) },
                                shapes = groupShapes(index, channels.size),
                                leadingContent = { ChatAvatar(channel, telegram, settings, size = 40.dp) },
                            ) {
                                EmojiText(channel.title)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
