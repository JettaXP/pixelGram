package com.pixelgram.desktop.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowUp
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.telegram.ChatListKey
import com.pixelgram.desktop.telegram.ChatState
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.components.ChatAvatar
import com.pixelgram.desktop.ui.theme.EmojiText

object ChatWindows {
    val open = mutableStateListOf<Pair<Telegram, Long>>()
}

private enum class Confirm { Clear, Delete, Block }

@Composable
fun ChatActions(
    chat: ChatState,
    telegram: Telegram,
    settings: AppSettings,
    at: DpOffset?,
    onDismiss: () -> Unit,
    listKey: ChatListKey = ChatListKey.Main,
    onSearch: (() -> Unit)? = null,
    onToBeginning: (() -> Unit)? = null,
) {
    var confirm by remember(chat.id) { mutableStateOf<Confirm?>(null) }
    var profile by remember(chat.id) { mutableStateOf(false) }
    if (at != null) {
        var folders by remember { mutableStateOf(false) }
        Box(Modifier.offset(at.x, at.y)) {
            DropdownMenu(
                expanded = true,
                onDismissRequest = onDismiss,
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                AnimatedContent(
                    targetState = folders,
                    transitionSpec = {
                        val direction = if (targetState) 1 else -1
                        (slideInHorizontally { it / 3 * direction } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it / 3 * direction } + fadeOut())
                    },
                ) { inFolders ->
                    Column(Modifier.width(MenuWidth)) {
                        val act = { action: () -> Unit -> { onDismiss(); action() } }
                        if (inFolders) {
                            Item(Icons.AutoMirrored.Filled.ArrowBack, "Add to folder") { folders = false }
                            HorizontalDivider(Modifier.padding(vertical = 4.dp))
                            telegram.folders.forEach { folder ->
                                Item(Icons.Outlined.Folder, folder.title, onClick = act { telegram.addToFolder(chat, folder.id) })
                            }
                        } else {
                            if (onSearch == null) {
                                Item(Icons.AutoMirrored.Outlined.OpenInNew, "Open in new window", onClick = act {
                                    if (ChatWindows.open.none { it.second == chat.id }) ChatWindows.open.add(telegram to chat.id)
                                })
                                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                            }
                            val muted = telegram.isMuted(chat)
                            Item(if (muted) Icons.Outlined.Notifications else Icons.Outlined.NotificationsOff, if (muted) "Unmute" else "Mute",
                                onClick = act { telegram.setMuted(chat, !muted) })
                            Item(Icons.Outlined.AccountCircle, "View profile", onClick = act { profile = true })
                            onSearch?.let { Item(Icons.Outlined.Search, "Search", onClick = act(it)) }
                            onToBeginning?.let { Item(Icons.Outlined.KeyboardDoubleArrowUp, "To beginning", onClick = act(it)) }
                            if (onSearch == null) {
                                val pinned = chat.pinned[listKey] == true
                                Item(Icons.Outlined.PushPin, if (pinned) "Unpin" else "Pin", onClick = act { telegram.setPinned(chat, listKey, !pinned) })
                            }
                            val archived = ChatListKey.Archive in chat.positions
                            Item(if (archived) Icons.Outlined.Unarchive else Icons.Outlined.Archive, if (archived) "Unarchive" else "Archive",
                                onClick = act { telegram.setArchived(chat, !archived) })
                            Item(Icons.Outlined.DoneAll, "Mark as read", onClick = act { telegram.markRead(chat) })
                            if (telegram.folders.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Add to folder") },
                                    leadingIcon = { Icon(Icons.Outlined.CreateNewFolder, contentDescription = null) },
                                    trailingIcon = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                                    onClick = { folders = true },
                                )
                            }
                            if (chat.privateUserId != 0L && !telegram.isSavedMessages(chat.id)) {
                                Item(Icons.Outlined.Block, if (telegram.isBot(chat)) "Stop and block bot" else "Block user",
                                    onClick = act { confirm = Confirm.Block })
                            }
                            HorizontalDivider(Modifier.padding(vertical = 4.dp))
                            Item(Icons.Outlined.CleaningServices, "Clear history", onClick = act { confirm = Confirm.Clear })
                            Item(
                                Icons.Outlined.Delete,
                                if (chat.privateUserId != 0L) "Delete chat" else if (chat.isChannel) "Leave channel" else "Leave group",
                                color = MaterialTheme.colorScheme.error,
                                onClick = act { confirm = Confirm.Delete },
                            )
                        }
                    }
                }
            }
        }
    }
    confirm?.let { which ->
        val title = telegram.chatTitle(chat)
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = {
                Text(
                    when (which) {
                        Confirm.Clear -> "Clear history?"
                        Confirm.Delete -> if (chat.privateUserId != 0L) "Delete chat?" else "Leave $title?"
                        Confirm.Block -> "Block $title?"
                    }
                )
            },
            text = {
                Text(
                    when (which) {
                        Confirm.Clear -> "All messages in this chat will be deleted for you."
                        Confirm.Delete -> "The chat will be removed from your chat list."
                        Confirm.Block -> "They won't be able to message you."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    when (which) {
                        Confirm.Clear -> telegram.clearHistory(chat)
                        Confirm.Delete -> telegram.deleteChat(chat)
                        Confirm.Block -> telegram.block(chat)
                    }
                    confirm = null
                }) { Text(if (which == Confirm.Clear) "Clear" else if (which == Confirm.Block) "Block" else "Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel") } },
        )
    }
    if (profile) ChatInfoDialog(chat, telegram, settings) { profile = false }
}

@Composable
private fun Item(icon: ImageVector, label: String, color: Color = Color.Unspecified, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, color = color) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurfaceVariant else color) },
        onClick = onClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInfoDialog(chat: ChatState, telegram: Telegram, settings: AppSettings, onDismiss: () -> Unit) {
    val user = telegram.users[chat.privateUserId]
    var bio by remember(chat.id) { mutableStateOf("") }
    LaunchedEffect(chat.id) {
        if (chat.privateUserId != 0L) telegram.userFullInfo(chat.privateUserId) { bio = it?.bio?.text.orEmpty() }
    }
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.widthIn(min = 320.dp, max = 420.dp)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(24.dp),
            ) {
                ChatAvatar(chat, telegram, settings, size = 96.dp)
                EmojiText(telegram.chatTitle(chat), style = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center))
                val subtitle = telegram.presence(chat)?.label ?: when {
                    chat.isChannel -> "channel"
                    chat.isGroup -> "group"
                    else -> ""
                }
                if (subtitle.isNotEmpty()) Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                user?.usernames?.activeUsernames?.firstOrNull()?.let {
                    Text("@$it", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleSmall)
                }
                user?.phoneNumber?.takeIf { it.isNotEmpty() }?.let { Text("+$it", style = MaterialTheme.typography.bodyMedium) }
                if (bio.isNotBlank()) EmojiText(bio, style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Close") }
            }
        }
    }
}

private val MenuWidth = 260.dp
