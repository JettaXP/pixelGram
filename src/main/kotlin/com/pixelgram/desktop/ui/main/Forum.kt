package com.pixelgram.desktop.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelgram.desktop.telegram.ChatState
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.telegram.can
import com.pixelgram.desktop.telegram.contentText
import com.pixelgram.desktop.telegram.createForumTopic
import com.pixelgram.desktop.telegram.forumTopics
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.components.expressive.AlertDialog
import com.pixelgram.desktop.ui.components.expressive.IconButton
import com.pixelgram.desktop.ui.components.expressive.TextButton
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.outlined.Add
import com.pixelgram.desktop.ui.icons.automirrored.filled.ArrowBack
import com.pixelgram.desktop.ui.icons.outlined.Lock
import com.pixelgram.desktop.ui.theme.EmojiText
import it.tdlight.jni.TdApi
import kotlinx.coroutines.delay

private const val TopicNameLimit = 128
private const val TopicRefreshMs = 400L
private val SidebarWidth = 260.dp

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
fun ForumSidebar(chat: ChatState, telegram: Telegram, modifier: Modifier = Modifier, onClose: (() -> Unit)? = null) {
    var topics by remember(chat.id) { mutableStateOf<List<TdApi.ForumTopic>?>(null) }
    var reload by remember(chat.id) { mutableIntStateOf(0) }
    var creating by remember(chat.id) { mutableStateOf(false) }
    val version = telegram.topicsVersion(chat.id)
    LaunchedEffect(chat.id, reload, version) {
        if (topics != null) delay(TopicRefreshMs)
        telegram.forumTopics(chat.id) { list ->
            topics = list
            val current = telegram.selectedTopic(chat.id)
            val chosen = list.firstOrNull { it.info.forumTopicId == current } ?: list.firstOrNull()
            chosen?.let { telegram.selectTopic(chat.id, it.info.forumTopicId, it.info.name) }
        }
    }
    val selected = telegram.selectedTopic(chat.id)
    val canCreate = telegram.can(chat) { it.canManageTopics } || chat.permissions?.canCreateTopics == true
    Column(modifier.width(SidebarWidth).padding(start = 12.dp, top = 12.dp, bottom = 12.dp)) {
        if (onClose != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            ) {
                IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                Column(Modifier.weight(1f).padding(start = 4.dp)) {
                    EmojiText(telegram.chatTitle(chat), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    telegram.presence(chat)?.label?.let { label ->
                        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, bottom = 8.dp).height(40.dp),
        ) {
            Text("Topics", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (canCreate) {
                IconButton(onClick = { creating = true }) { Icon(Icons.Outlined.Add, contentDescription = "New topic") }
            }
        }
        val shown = topics
        if (shown == null) {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { LoadingIndicator() }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxSize()) {
                items(shown, key = { it.info.forumTopicId }) { topic ->
                    TopicRow(topic, topic.info.forumTopicId == selected, telegram) { telegram.selectTopic(chat.id, topic.info.forumTopicId, topic.info.name) }
                }
            }
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
private fun TopicRow(topic: TdApi.ForumTopic, selected: Boolean, telegram: Telegram, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val background by animateColorAsState(if (selected) colors.secondaryContainer else Color.Transparent)
    val preview = topic.lastMessage?.let { last ->
        val text = contentText(last.content)
        val sender = last.senderId?.let { telegram.senderTitle(it).substringBefore(' ') }.orEmpty()
        if (sender.isNotBlank() && !last.isOutgoing) "$sender: $text" else text
    }.orEmpty()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        TopicIcon(topic.info, 36.dp)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                EmojiText(
                    topic.info.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) colors.onSecondaryContainer else colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (topic.info.isClosed) Icon(Icons.Outlined.Lock, contentDescription = "Closed", modifier = Modifier.size(14.dp))
            }
            if (preview.isNotBlank()) {
                EmojiText(
                    preview,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (topic.unreadCount > 0) Badge { Text(topic.unreadCount.toString()) }
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
