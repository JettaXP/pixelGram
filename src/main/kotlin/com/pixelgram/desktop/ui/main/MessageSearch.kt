package com.pixelgram.desktop.ui.main

import com.pixelgram.desktop.ui.components.groupShapes
import com.pixelgram.desktop.ui.theme.EmojiVisualTransformation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.automirrored.filled.ArrowBack
import com.pixelgram.desktop.ui.icons.filled.Close
import com.pixelgram.desktop.ui.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import com.pixelgram.desktop.ui.components.expressive.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import com.pixelgram.desktop.ui.components.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.telegram.contentText
import com.pixelgram.desktop.ui.components.ChatAvatar
import com.pixelgram.desktop.ui.theme.EmojiText
import it.tdlight.jni.TdApi
import kotlinx.coroutines.delay

object MessageSearch {
    var open by mutableStateOf(false)
    var scopeChatId by mutableStateOf<Long?>(null)
    var jump by mutableStateOf<Pair<Long, Long>?>(null)
    var lastChat by mutableStateOf<Long?>(null)

    fun start(chatId: Long?) {
        scopeChatId = chatId
        if (chatId != null) lastChat = chatId
        open = true
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MessageSearchPanel(telegram: Telegram, settings: AppSettings, onOpen: (Long) -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<TdApi.Message>>(emptyList()) }
    var foundChats by remember { mutableStateOf<List<Long>>(emptyList()) }
    var total by remember { mutableStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    val scope = MessageSearch.scopeChatId
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    LaunchedEffect(query, scope) {
        if (query.isBlank()) {
            results = emptyList()
            foundChats = emptyList()
            total = 0
            busy = false
            return@LaunchedEffect
        }
        busy = true
        if (scope == null) telegram.searchChats(query.trim()) { foundChats = it } else foundChats = emptyList()
        delay(SearchDelayMs)
        telegram.searchMessages(query.trim(), scope) { count, found ->
            total = count
            results = found
            busy = false
        }
    }
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, end = 8.dp, top = 4.dp)) {
            IconButton(onClick = { MessageSearch.open = false }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close search")
            }
            TextField(
                value = query,
                visualTransformation = EmojiVisualTransformation,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text("Search messages") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = if (query.isNotEmpty()) {
                    { IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, contentDescription = "Clear") } }
                } else {
                    null
                },
                shape = MaterialTheme.shapes.extraLarge,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                modifier = Modifier.weight(1f).focusRequester(focus),
            )
        }
        Text(
            "Search messages in",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 20.dp, top = 12.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            FilterChip(
                selected = scope == null,
                onClick = { MessageSearch.scopeChatId = null },
                label = { Text("All chats") },
            )
            val scoped = scope ?: MessageSearch.lastChat
            scoped?.let { id -> telegram.chats[id] }?.let { chat ->
                FilterChip(
                    selected = scope != null,
                    onClick = { MessageSearch.scopeChatId = chat.id },
                    label = { EmojiText(telegram.chatTitle(chat), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = { ChatAvatar(chat, telegram, settings, size = 20.dp) },
                )
            }
        }
        if (query.isNotBlank()) {
            Text(
                when {
                    busy -> "Searching…"
                    total == 1 -> "1 message found"
                    else -> "$total messages found"
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, top = 6.dp, bottom = 6.dp),
            )
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (busy && results.isEmpty()) LoadingIndicator(Modifier.align(Alignment.TopCenter).padding(top = 24.dp))
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                if (foundChats.isNotEmpty()) {
                    item(key = "chats-header") { SearchSection("Chats") }
                    itemsIndexed(foundChats, key = { _, id -> "chat:$id" }) { index, id ->
                        val chat = telegram.chats[id] ?: return@itemsIndexed
                        SegmentedListItem(
                            onClick = { onOpen(id) },
                            shapes = groupShapes(index, foundChats.size),
                            leadingContent = { ChatAvatar(chat, telegram, settings, size = 44.dp) },
                        ) {
                            EmojiText(telegram.chatTitle(chat), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    if (results.isNotEmpty()) item(key = "messages-header") { SearchSection("Messages") }
                }
                itemsIndexed(results, key = { _, m -> "${m.chatId}:${m.id}" }) { index, message ->
                    val chat = telegram.chats[message.chatId] ?: return@itemsIndexed
                    val ui = message.toUiOrNull()
                    SegmentedListItem(
                        onClick = {
                            MessageSearch.jump = message.chatId to message.id
                            onOpen(message.chatId)
                        },
                        shapes = groupShapes(index, results.size),
                        leadingContent = { ChatAvatar(chat, telegram, settings, size = 44.dp) },
                        supportingContent = {
                            EmojiText(
                                snippet(contentText(message.content), query),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        trailingContent = { Text(formatChatDate(message.date), style = MaterialTheme.typography.labelSmall) },
                    ) {
                        EmojiText(
                            if (scope != null && ui != null) telegram.senderName(ui).ifBlank { telegram.chatTitle(chat) } else telegram.chatTitle(chat),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchSection(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 6.dp),
    )
}

private fun TdApi.Message.toUiOrNull() = com.pixelgram.desktop.telegram.UiMessage(
    id, chatId,
    (senderId as? TdApi.MessageSenderUser)?.userId ?: 0L,
    (senderId as? TdApi.MessageSenderChat)?.chatId ?: 0L,
    isOutgoing, date, "",
)

private fun snippet(text: String, query: String): String {
    val flat = text.replace('\n', ' ')
    val at = flat.indexOf(query.trim(), ignoreCase = true)
    if (at <= SnippetLead) return flat
    return "…" + flat.substring(at - SnippetLead)
}

private const val SearchDelayMs = 350L
private const val SnippetLead = 12
