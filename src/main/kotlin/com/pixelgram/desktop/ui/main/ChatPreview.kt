package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.telegram.ChatState
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.telegram.UiMessage
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.components.expressive.FilledTonalButton
import com.pixelgram.desktop.ui.components.expressive.TextButton
import com.pixelgram.desktop.ui.theme.EmojiText

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ChatPreview(chat: ChatState, telegram: Telegram, settings: AppSettings, onOpen: () -> Unit, onDismiss: () -> Unit) {
    var messages by remember(chat.id) { mutableStateOf<List<UiMessage>?>(null) }
    var loading by remember(chat.id) { mutableStateOf(false) }
    var done by remember(chat.id) { mutableStateOf(false) }
    val list = rememberLazyListState()
    LaunchedEffect(chat.id) { telegram.previewHistory(chat.id, 0) { messages = it } }
    val nearTop by remember { derivedStateOf { list.layoutInfo.visibleItemsInfo.lastOrNull()?.index?.let { it >= (messages?.size ?: 0) - PreloadItems } == true } }
    LaunchedEffect(nearTop, messages?.size) {
        val current = messages ?: return@LaunchedEffect
        if (!nearTop || loading || done || current.isEmpty()) return@LaunchedEffect
        loading = true
        telegram.previewHistory(chat.id, current.last().id) { older ->
            val fresh = older.filter { o -> current.none { it.id == o.id } }
            if (fresh.isEmpty()) done = true
            messages = current + fresh
            loading = false
        }
    }
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.width(PreviewWidth).height(PreviewHeight)) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                ) {
                    com.pixelgram.desktop.ui.components.ChatAvatar(chat, telegram, settings, 44.dp)
                    Column(Modifier.weight(1f)) {
                        EmojiText(telegram.chatTitle(chat), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        telegram.presence(chat)?.label?.let {
                            Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp),
                ) {
                    val shown = messages
                    if (shown == null) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() }
                    } else {
                        LazyColumn(
                            state = list,
                            reverseLayout = true,
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(shown, key = { it.id }) { message ->
                                MessageBubble(
                                    message = message,
                                    senderName = if (chat.isGroup && !message.isOutgoing) telegram.senderName(message) else null,
                                    settings = settings,
                                    telegram = telegram,
                                )
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.align(Alignment.End).padding(12.dp)) {
                    if (chat.unreadCount > 0 || chat.markedUnread) {
                        TextButton(onClick = {
                            telegram.markChatRead(chat)
                            onDismiss()
                        }) { Text("Mark as read") }
                    }
                    FilledTonalButton(onClick = {
                        onDismiss()
                        onOpen()
                    }) { Text("Open") }
                }
            }
        }
    }
}

private const val PreloadItems = 6
private val PreviewWidth = 460.dp
private val PreviewHeight = 600.dp
