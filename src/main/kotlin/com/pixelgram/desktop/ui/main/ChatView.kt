package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.telegram.ChatState
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.telegram.UiMessage
import com.pixelgram.desktop.ui.components.ChatAvatar

@Composable
fun ChatView(chat: ChatState, telegram: Telegram, settings: AppSettings) {
    DisposableEffect(chat.id) {
        telegram.openChat(chat.id)
        onDispose { telegram.closeChat(chat.id) }
    }
    val messages = telegram.messages(chat.id)
    val listState = rememberLazyListState()
    val nearTop by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 5
        }
    }
    LaunchedEffect(nearTop, messages.size) {
        if (nearTop) telegram.loadMoreMessages(chat.id)
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ChatAvatar(chat, telegram, settings, size = 40.dp)
            Text(telegram.chatTitle(chat), style = MaterialTheme.typography.titleMedium)
        }
        LazyColumn(
            state = listState,
            reverseLayout = true,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(messages, key = { it.id }) { message ->
                MessageBubble(
                    message = message,
                    senderName = if (chat.isGroup && !message.isOutgoing) {
                        telegram.senderName(message)
                    } else {
                        null
                    },
                    settings = settings,
                )
            }
        }
        Composer(onSend = { telegram.sendText(chat.id, it) })
    }
}

fun bubbleShape(radius: Int, tailRadius: Int, outgoing: Boolean): Shape {
    val r = radius.dp
    val tail = tailRadius.dp
    return if (outgoing) {
        RoundedCornerShape(topStart = r, topEnd = r, bottomEnd = tail, bottomStart = r)
    } else {
        RoundedCornerShape(topStart = r, topEnd = r, bottomEnd = r, bottomStart = tail)
    }
}

@Composable
fun MessageBubble(message: UiMessage, senderName: String?, settings: AppSettings) {
    val colors = MaterialTheme.colorScheme
    val outgoing = message.isOutgoing
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (outgoing) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Surface(
            shape = bubbleShape(
                settings.bubbleRadius.value,
                settings.bubbleTailRadius.value,
                outgoing,
            ),
            color = if (outgoing) colors.primaryContainer else colors.surfaceContainerHigh,
            contentColor = if (outgoing) colors.onPrimaryContainer else colors.onSurface,
            modifier = Modifier.widthIn(max = 520.dp),
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                if (!senderName.isNullOrEmpty()) {
                    Text(
                        senderName,
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.primary,
                    )
                }
                SelectionContainer {
                    Text(message.text, style = MaterialTheme.typography.bodyLarge)
                }
                Text(
                    formatTime(message.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = (if (outgoing) colors.onPrimaryContainer else colors.onSurfaceVariant)
                        .copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}

@Composable
private fun Composer(onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val send = {
        if (text.isNotBlank()) {
            onSend(text.trim())
            text = ""
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("Message") },
            maxLines = 6,
            shape = RoundedCornerShape(28.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            modifier = Modifier
                .weight(1f)
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown &&
                        event.key == Key.Enter &&
                        !event.isShiftPressed
                    ) {
                        send()
                        true
                    } else {
                        false
                    }
                },
        )
        FilledIconButton(onClick = send, enabled = text.isNotBlank()) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
        }
    }
}
