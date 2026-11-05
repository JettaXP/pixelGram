package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.telegram.contentText
import com.pixelgram.desktop.ui.components.ChatAvatar
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.components.groupShapes
import com.pixelgram.desktop.ui.theme.EmojiText
import it.tdlight.jni.TdApi

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CommunityDialog(community: TdApi.Community, telegram: Telegram, settings: AppSettings, onOpen: (Long) -> Unit, onDismiss: () -> Unit) {
    var chats by remember(community.id) { mutableStateOf<List<TdApi.CommunityChat>?>(null) }
    LaunchedEffect(community.id) { telegram.communityChats(community.id) { chats = it } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                com.pixelgram.desktop.ui.components.AvatarImage(community.name, community.photo?.small, telegram, CircleShape, CommunityPhoto)
                EmojiText(community.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 10.dp))
                chats?.let { list ->
                    Text(
                        "${list.size} ${com.pixelgram.desktop.core.tr("chats")}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        text = {
            val list = chats
            if (list == null) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { LoadingIndicator() }
                return@AlertDialog
            }
            val loaded = list.filter { telegram.chats[it.chatId] != null }
            val joined = loaded.filter { telegram.isMember(telegram.chats.getValue(it.chatId)) }
            val viewable = loaded.filter { !it.isHidden && it !in joined }
            val hidden = loaded.filter { it.isHidden && it !in joined }
            LazyColumn(Modifier.heightIn(max = ListHeight), verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                listOf("Chats you are in" to joined, "Chats you can view" to viewable, "Hidden chats" to hidden).forEach { (title, group) ->
                    if (group.isEmpty()) return@forEach
                    item(key = title) {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                        )
                    }
                    itemsIndexed(group, key = { _, item -> item.chatId }) { index, item ->
                        val chat = telegram.chats.getValue(item.chatId)
                        SegmentedListItem(
                            onClick = { onOpen(chat.id) },
                            shapes = groupShapes(index, group.size),
                            leadingContent = { ChatAvatar(chat, telegram, settings, size = 44.dp) },
                            supportingContent = {
                                EmojiText(
                                    chat.lastMessage?.let { contentText(it.content).replace('\n', ' ') }.orEmpty(),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                        ) {
                            EmojiText(telegram.chatTitle(chat), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

private val CommunityPhoto = 88.dp
private val ListHeight = 380.dp
