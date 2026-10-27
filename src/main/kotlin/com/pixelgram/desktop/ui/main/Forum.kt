package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.telegram.ChatState
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.telegram.can
import com.pixelgram.desktop.telegram.createForumTopic
import com.pixelgram.desktop.telegram.forumTopics
import com.pixelgram.desktop.ui.OnEscape
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.components.expressive.AlertDialog
import com.pixelgram.desktop.ui.components.expressive.IconButton
import com.pixelgram.desktop.ui.components.expressive.TextButton
import com.pixelgram.desktop.ui.components.itemMotion
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.automirrored.filled.ArrowBack
import com.pixelgram.desktop.ui.icons.outlined.Add
import com.pixelgram.desktop.ui.icons.outlined.Lock
import com.pixelgram.desktop.ui.theme.EmojiText
import it.tdlight.jni.TdApi
import kotlinx.coroutines.launch

private const val TopicNameLimit = 128
private const val TopicPreloadItems = 5
private const val WheelStep = 48f

@Composable
fun TopicIcon(info: TdApi.ForumTopicInfo, size: Dp) {
    val color = if (info.isGeneral) MaterialTheme.colorScheme.surfaceContainerHighest else Color(0xFF000000 or (info.icon?.color?.toLong() ?: 0x6FB9F0L))
    Box(Modifier.size(size).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
        Text(
            if (info.isGeneral) "#" else info.name.trim().take(1).uppercase(),
            color = if (info.isGeneral) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.5f).sp,
            maxLines = 1,
        )
    }
}

@Composable
fun ForumTabs(chat: ChatState, telegram: Telegram, onOpen: (TdApi.ForumTopicInfo) -> Unit, modifier: Modifier = Modifier) {
    var topics by remember(chat.id) { mutableStateOf(emptyList<TdApi.ForumTopic>()) }
    var reload by remember(chat.id) { mutableIntStateOf(0) }
    var creating by remember(chat.id) { mutableStateOf(false) }
    LaunchedEffect(chat.id, reload) { telegram.forumTopics(chat.id) { topics = it } }
    val canCreate = telegram.can(chat) { it.canManageTopics } || chat.permissions?.canCreateTopics == true
    val rowState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    Row(modifier.fillMaxWidth().padding(end = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyRow(
            state = rowState,
            modifier = Modifier.weight(1f).pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type != PointerEventType.Scroll) continue
                        val change = event.changes.firstOrNull() ?: continue
                        if (change.isConsumed || change.scrollDelta.y == 0f) continue
                        change.consume()
                        scope.launch { rowState.scrollBy(change.scrollDelta.y * WheelStep) }
                    }
                }
            },
            contentPadding = PaddingValues(start = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(topics, key = { it.info.forumTopicId }) { topic ->
                AssistChip(
                    onClick = { onOpen(topic.info) },
                    label = { EmojiText(topic.info.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = { TopicIcon(topic.info, 18.dp) },
                    trailingIcon = when {
                        topic.unreadCount > 0 -> {
                            { Badge { Text(topic.unreadCount.toString()) } }
                        }
                        topic.info.isClosed -> {
                            { Icon(Icons.Outlined.Lock, contentDescription = "Closed", modifier = Modifier.size(16.dp)) }
                        }
                        else -> null
                    },
                    shape = RoundedCornerShape(50),
                )
            }
        }
        if (canCreate) {
            AssistChip(
                onClick = { creating = true },
                label = { Text("New topic") },
                leadingIcon = { Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp)) },
                shape = RoundedCornerShape(50),
            )
        }
    }
    if (creating) {
        NewTopicDialog(onDismiss = { creating = false }) { name ->
            telegram.createForumTopic(chat.id, name) { error ->
                if (error != null) TopNotice.text = error else reload++
            }
            creating = false
        }
    }
}

@Composable
private fun NewTopicDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New topic") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(TopicNameLimit) },
                label = { Text("Topic name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().focusRequester(focus).onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Enter && name.isNotBlank()) {
                        onCreate(name)
                        true
                    } else {
                        false
                    }
                },
            )
        },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onCreate(name) }) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun TopicView(chat: ChatState, topic: TdApi.ForumTopicInfo, telegram: Telegram, settings: AppSettings, onBack: () -> Unit) {
    DisposableEffect(chat.id, topic.forumTopicId) {
        telegram.openTopic(chat.id, topic.forumTopicId)
        onDispose { telegram.closeTopic() }
    }
    OnEscape {
        onBack()
        true
    }
    val listState = rememberLazyListState()
    LaunchedEffect(listState, topic.forumTopicId) {
        snapshotFlow { listState.layoutInfo.let { (it.visibleItemsInfo.lastOrNull()?.index ?: 0) to it.totalItemsCount } }
            .collect { (last, total) -> if (total > 0 && last >= total - TopicPreloadItems) telegram.loadTopicHistory() }
    }
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                TopicIcon(topic, 40.dp)
                Column(Modifier.weight(1f)) {
                    EmojiText(topic.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    EmojiText(
                        telegram.chatTitle(chat),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Bottom),
                ) {
                    items(telegram.topicMessages, key = { it.key }) { message ->
                        MessageBubble(
                            modifier = itemMotion(),
                            message = message,
                            senderName = if (message.isOutgoing) null else telegram.senderName(message),
                            senderTag = if (message.isOutgoing) null else telegram.senderTag(chat, message),
                            settings = settings,
                            telegram = telegram,
                            onReact = { emoji -> telegram.toggleReaction(message, emoji) },
                        )
                    }
                    if (telegram.topicLoading) {
                        item(key = "loading") {
                            Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) { LoadingIndicator() }
                        }
                    }
                }
                if (telegram.topicComplete && telegram.topicMessages.isEmpty()) {
                    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        Text(
                            "No messages here yet…",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        )
                    }
                }
            }
            if (topic.isClosed && !telegram.can(chat) { it.canManageTopics }) {
                Text(
                    "This topic is closed",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                )
            } else {
                CommentComposer("Message") { telegram.sendToTopic(it) }
            }
        }
    }
}
