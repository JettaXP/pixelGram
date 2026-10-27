package com.pixelgram.desktop.ui.main

import com.pixelgram.desktop.ui.components.itemMotion
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.automirrored.filled.ArrowBack
import com.pixelgram.desktop.ui.icons.automirrored.filled.KeyboardArrowRight
import com.pixelgram.desktop.ui.icons.automirrored.filled.Send
import com.pixelgram.desktop.ui.icons.outlined.ChatBubbleOutline
import com.pixelgram.desktop.ui.components.expressive.Button
import com.pixelgram.desktop.ui.components.expressive.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import com.pixelgram.desktop.ui.components.expressive.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.pixelgram.desktop.ui.components.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.telegram.UiMessage
import com.pixelgram.desktop.ui.OnEscape
import com.pixelgram.desktop.ui.theme.EmojiVisualTransformation

@Composable
fun CommentsBar(count: Int, onClick: () -> Unit) {
    Column {
        HorizontalDivider(Modifier.padding(horizontal = 12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(
                when (count) {
                    0 -> "Leave a comment"
                    1 -> "1 comment"
                    else -> "$count comments"
                },
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun CommentsView(post: UiMessage, telegram: Telegram, settings: AppSettings, onBack: () -> Unit) {
    var error by remember(post.key) { mutableStateOf<String?>(null) }
    DisposableEffect(post.key) {
        telegram.openThread(post.chatId, post.id) { error = it }
        onDispose { telegram.closeThread() }
    }
    OnEscape {
        onBack()
        true
    }
    val thread = telegram.activeThread
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                Column {
                    Text("Discussion", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Comments",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                when {
                    error != null -> Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                    thread == null -> LoadingIndicator()
                    else -> LazyColumn(
                        reverseLayout = true,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Bottom),
                    ) {
                        if (telegram.threadMessages.isEmpty()) {
                            item(key = "empty") {
                                Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                                    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                                        Text(
                                            "No comments here yet…",
                                            style = MaterialTheme.typography.labelLarge,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                        )
                                    }
                                }
                            }
                        }
                        items(telegram.threadMessages, key = { it.key }) { message ->
                            MessageBubble(
                                modifier = itemMotion(),
                                message = message,
                                senderName = if (message.isOutgoing) null else telegram.senderName(message),
                                settings = settings,
                                telegram = telegram,
                                onReact = { emoji -> telegram.toggleReaction(message, emoji) },
                            )
                        }
                        telegram.threadRoot?.let { root ->
                            item(key = "root") {
                                Box(Modifier.padding(bottom = 8.dp)) {
                                    MessageBubble(message = root, senderName = telegram.senderName(root), settings = settings, telegram = telegram)
                                }
                            }
                        }
                    }
                }
            }
            if (thread != null) {
                if (telegram.mustJoin(thread.first)) {
                    var joining by remember { mutableStateOf(false) }
                    Button(
                        onClick = {
                            joining = true
                            telegram.joinChat(thread.first) { failure ->
                                joining = false
                                error = failure
                            }
                        },
                        enabled = !joining,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    ) { Text("Join group") }
                } else {
                    CommentComposer { telegram.sendComment(it) }
                }
            }
        }
    }
}

@Composable
internal fun CommentComposer(placeholder: String = "Comment", onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val send = {
        if (text.isNotBlank()) {
            onSend(text.trim())
            text = ""
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        TextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text(placeholder) },
            visualTransformation = EmojiVisualTransformation,
            shape = MaterialTheme.shapes.extraLarge,
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            maxLines = 6,
            modifier = Modifier
                .weight(1f)
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Enter && !event.isShiftPressed) {
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
