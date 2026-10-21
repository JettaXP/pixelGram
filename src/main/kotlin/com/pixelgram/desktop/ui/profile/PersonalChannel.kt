package com.pixelgram.desktop.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

// A user's personal channel with its latest post, as on Telegram profiles.
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
        shapes = ListItemDefaults.segmentedShapes(0, 1),
        modifier = modifier,
        leadingContent = { ChatAvatar(channel, telegram, settings, size = 40.dp) },
        overlineContent = { Text("Personal channel") },
        supportingContent = channel.lastMessage?.let { last ->
            {
                Text(
                    contentText(last.content).replace('\n', ' '),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
    ) {
        Text(channel.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                                shapes = ListItemDefaults.segmentedShapes(index, channels.size),
                                leadingContent = { ChatAvatar(channel, telegram, settings, size = 40.dp) },
                            ) {
                                Text(channel.title)
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
