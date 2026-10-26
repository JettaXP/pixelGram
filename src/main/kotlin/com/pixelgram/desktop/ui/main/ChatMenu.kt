package com.pixelgram.desktop.ui.main

import com.pixelgram.desktop.ui.icons.outlined.Edit
import com.pixelgram.desktop.ui.icons.outlined.DriveFileRenameOutline
import com.pixelgram.desktop.ui.icons.outlined.PersonAdd
import com.pixelgram.desktop.ui.icons.outlined.Link
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.heightIn
import com.pixelgram.desktop.ui.components.GroupedMenu
import com.pixelgram.desktop.ui.components.MenuEntry
import com.pixelgram.desktop.ui.components.MenuGroups
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateDpAsState
import com.pixelgram.desktop.core.Banners
import com.pixelgram.desktop.ui.profile.ProfileBanner
import androidx.compose.foundation.layout.Row
import androidx.compose.animation.animateColorAsState
import com.pixelgram.desktop.ui.icons.outlined.MoreHoriz
import com.pixelgram.desktop.ui.icons.automirrored.outlined.Chat
import com.pixelgram.desktop.ui.profile.ProfileMusic
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import com.pixelgram.desktop.ui.icons.outlined.Numbers
import com.pixelgram.desktop.ui.icons.outlined.Info
import com.pixelgram.desktop.ui.icons.outlined.Phone
import com.pixelgram.desktop.ui.icons.outlined.AlternateEmail
import com.pixelgram.desktop.ui.components.InfoRow
import com.pixelgram.desktop.ui.components.UsernameQrButton
import com.pixelgram.desktop.ui.components.InfoGroup
import com.pixelgram.desktop.ui.profile.ProfilePhotosDialog
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.pixelgram.desktop.ui.profile.PersonalChannelCard
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
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.automirrored.filled.ArrowBack
import com.pixelgram.desktop.ui.icons.automirrored.filled.KeyboardArrowRight
import com.pixelgram.desktop.ui.icons.automirrored.outlined.OpenInNew
import com.pixelgram.desktop.ui.icons.outlined.AccountCircle
import com.pixelgram.desktop.ui.icons.outlined.Archive
import com.pixelgram.desktop.ui.icons.outlined.Block
import com.pixelgram.desktop.ui.icons.outlined.LockOpen
import com.pixelgram.desktop.ui.icons.outlined.CleaningServices
import com.pixelgram.desktop.ui.icons.outlined.CreateNewFolder
import com.pixelgram.desktop.ui.icons.outlined.Delete
import com.pixelgram.desktop.ui.icons.outlined.Folder
import com.pixelgram.desktop.ui.icons.outlined.KeyboardDoubleArrowUp
import com.pixelgram.desktop.ui.icons.outlined.Notifications
import com.pixelgram.desktop.ui.icons.outlined.NotificationsOff
import com.pixelgram.desktop.ui.icons.outlined.PushPin
import com.pixelgram.desktop.ui.icons.outlined.Search
import com.pixelgram.desktop.ui.icons.outlined.Unarchive
import com.pixelgram.desktop.ui.components.expressive.AlertDialog
import com.pixelgram.desktop.ui.components.expressive.BasicAlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.components.expressive.TextButton
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
import it.tdlight.jni.TdApi
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
    onOpenChat: ((Long) -> Unit)? = null,
    onSearch: (() -> Unit)? = null,
    onToBeginning: (() -> Unit)? = null,
    showProfile: Boolean = true,
) {
    var confirm by remember(chat.id) { mutableStateOf<Confirm?>(null) }
    var profile by remember(chat.id) { mutableStateOf(false) }
    var contactEdit by remember(chat.id) { mutableStateOf(false) }
    var renaming by remember(chat.id) { mutableStateOf(false) }
    LaunchedEffect(chat.id, at != null) {
        if (at != null && chat.privateUserId != 0L) telegram.userFullInfo(chat.privateUserId) {}
    }
    if (at != null) {
        var folders by remember { mutableStateOf(false) }
        Box(Modifier.offset(at.x, at.y)) {
            GroupedMenu(expanded = true, onDismiss = onDismiss) {
                AnimatedContent(
                    targetState = folders,
                    transitionSpec = {
                        val direction = if (targetState) 1 else -1
                        (slideInHorizontally { it / 3 * direction } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it / 3 * direction } + fadeOut())
                    },
                ) { inFolders ->
                    Column {
                        val act = { action: () -> Unit -> { onDismiss(); action() } }
                        if (inFolders) {
                            MenuGroups(listOf(
                                listOf(MenuEntry(Icons.AutoMirrored.Filled.ArrowBack, "Add to folder") { folders = false }),
                                telegram.folders.map { folder -> MenuEntry(Icons.Outlined.Folder, folder.title, onClick = act { telegram.addToFolder(chat, folder.id) }) },
                            ))
                            return@Column
                        }
                        val muted = telegram.isMuted(chat)
                        val pinned = chat.pinned[listKey] == true
                        val archived = ChatListKey.Archive in chat.positions
                        val window = if (onSearch == null) listOf(
                            MenuEntry(Icons.AutoMirrored.Outlined.OpenInNew, "Open in new window", onClick = act {
                                if (ChatWindows.open.none { it.second == chat.id }) ChatWindows.open.add(telegram to chat.id)
                            }),
                        ) else emptyList()
                        val main = buildList {
                            add(MenuEntry(if (muted) Icons.Outlined.Notifications else Icons.Outlined.NotificationsOff, if (muted) "Unmute" else "Mute",
                                onClick = act { telegram.setMuted(chat, !muted) }))
                            if (showProfile) add(MenuEntry(Icons.Outlined.AccountCircle, "View profile", onClick = act { profile = true }))
                            onSearch?.let { add(MenuEntry(Icons.Outlined.Search, "Search", onClick = act(it))) }
                            onToBeginning?.let { add(MenuEntry(Icons.Outlined.KeyboardDoubleArrowUp, "To beginning", onClick = act(it))) }
                        }
                        val organize = buildList {
                            if (onSearch == null) add(MenuEntry(Icons.Outlined.PushPin, if (pinned) "Unpin" else "Pin", onClick = act { telegram.setPinned(chat, listKey, !pinned) }))
                            add(MenuEntry(if (archived) Icons.Outlined.Unarchive else Icons.Outlined.Archive, if (archived) "Unarchive" else "Archive",
                                onClick = act { telegram.setArchived(chat, !archived) }))
                            if (telegram.folders.isNotEmpty()) {
                                add(MenuEntry(Icons.Outlined.CreateNewFolder, "Add to folder", trailing = Icons.AutoMirrored.Filled.KeyboardArrowRight) { folders = true })
                            }
                        }
                        val people = buildList {
                            if (chat.privateUserId != 0L && !telegram.isSavedMessages(chat.id) && !telegram.isBot(chat)) {
                                if (telegram.isContact(chat)) {
                                    add(MenuEntry(Icons.Outlined.Edit, "Edit contact", onClick = act { contactEdit = true }))
                                } else {
                                    add(MenuEntry(Icons.Outlined.PersonAdd, "Add to contacts", onClick = act { contactEdit = true }))
                                }
                                add(MenuEntry(Icons.Outlined.DriveFileRenameOutline, "Rename", onClick = act { renaming = true }))
                            }
                        }
                        val danger = buildList {
                            if (chat.privateUserId != 0L && !telegram.isSavedMessages(chat.id)) {
                                if (telegram.isBlocked(chat)) {
                                    add(MenuEntry(Icons.Outlined.LockOpen, if (telegram.isBot(chat)) "Unblock bot" else "Unblock user", onClick = act { telegram.unblock(chat) }))
                                } else {
                                    add(MenuEntry(Icons.Outlined.Block, if (telegram.isBot(chat)) "Stop and block bot" else "Block user", onClick = act { confirm = Confirm.Block }))
                                }
                            }
                            add(MenuEntry(Icons.Outlined.CleaningServices, "Clear history", onClick = act { confirm = Confirm.Clear }))
                            add(MenuEntry(
                                Icons.Outlined.Delete,
                                if (chat.privateUserId != 0L) "Delete chat" else if (chat.isChannel) "Leave channel" else "Leave group",
                                color = MaterialTheme.colorScheme.error,
                                onClick = act { confirm = Confirm.Delete },
                            ))
                        }
                        MenuGroups(listOf(window, main, people, organize, danger))
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
    if (profile) ChatInfoDialog(chat, telegram, settings, onOpenChat) { profile = false }
    if (contactEdit) ContactDialog(chat, telegram) { contactEdit = false }
    if (renaming) RenameDialog(chat, telegram) { renaming = false }
}

@Composable
private fun ContactDialog(chat: ChatState, telegram: Telegram, onDismiss: () -> Unit) {
    val user = telegram.users[chat.privateUserId]
    val existing = telegram.isContact(chat)
    var first by remember { mutableStateOf(user?.firstName.orEmpty()) }
    var last by remember { mutableStateOf(user?.lastName.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing) "Edit contact" else "Add to contacts") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.OutlinedTextField(first, { first = it }, label = { Text("First name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                androidx.compose.material3.OutlinedTextField(last, { last = it }, label = { Text("Last name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (existing) {
                    TextButton(onClick = {
                        telegram.removeContact(chat.privateUserId)
                        onDismiss()
                    }) { Text("Delete contact", color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = first.isNotBlank(), onClick = {
                telegram.saveContact(chat.privateUserId, first.trim(), last.trim())
                onDismiss()
            }) { Text("Done") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun RenameDialog(chat: ChatState, telegram: Telegram, onDismiss: () -> Unit) {
    val current = com.pixelgram.desktop.core.LocalNames.get(chat.privateUserId)
    val user = telegram.users[chat.privateUserId]
    var first by remember { mutableStateOf(current?.substringBefore(' ') ?: user?.firstName.orEmpty()) }
    var last by remember { mutableStateOf(current?.substringAfter(' ', "") ?: user?.lastName.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Only you will see this name. They stay out of your contacts.", style = MaterialTheme.typography.bodyMedium)
                androidx.compose.material3.OutlinedTextField(first, { first = it }, label = { Text("First name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                androidx.compose.material3.OutlinedTextField(last, { last = it }, label = { Text("Last name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (current != null) {
                    TextButton(onClick = {
                        com.pixelgram.desktop.core.LocalNames.set(chat.privateUserId, null)
                        onDismiss()
                    }) { Text("Restore original name") }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = first.isNotBlank(), onClick = {
                com.pixelgram.desktop.core.LocalNames.set(chat.privateUserId, listOf(first.trim(), last.trim()).filter(String::isNotBlank).joinToString(" "))
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInfoDialog(
    chat: ChatState,
    telegram: Telegram,
    settings: AppSettings,
    onOpenChat: ((Long) -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val user = telegram.users[chat.privateUserId]
    var bio by remember(chat.id) { mutableStateOf("") }
    var bioEntities by remember(chat.id) { mutableStateOf(emptyList<TdApi.TextEntity>()) }
    var mentioned by remember(chat.id) { mutableStateOf<ChatState?>(null) }
    var personalChannel by remember(chat.id) { mutableStateOf(0L) }
    var about by remember(chat.id) { mutableStateOf("") }
    LaunchedEffect(chat.id) {
        if (chat.privateUserId == 0L) telegram.chatDescription(chat) { about = it }
        if (chat.privateUserId != 0L) {
            telegram.userFullInfo(chat.privateUserId) {
                bio = it?.bio?.text.orEmpty()
                bioEntities = it?.bio?.entities?.toList().orEmpty()
                personalChannel = it?.personalChatId ?: 0L
            }
        }
    }
    mentioned?.let { other -> ChatInfoDialog(other, telegram, settings, onOpenChat) { mentioned = null } }
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.widthIn(min = 360.dp, max = 460.dp).heightIn(max = DialogMaxHeight),
        ) {
          val scroll = rememberScrollState()
          Box {
            val bannerId = if (chat.privateUserId != 0L) chat.privateUserId else chat.id
            if (Banners.banner(bannerId) != null) {
                val bannerPx = with(LocalDensity.current) { DialogBannerHeight.toPx() }
                ProfileBanner(
                    bannerId,
                    telegram,
                    DialogBannerHeight,
                    RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.graphicsLayer {
                        val offset = scroll.value.toFloat()
                        translationY = -offset * BannerParallax
                        alpha = (1f - offset / bannerPx).coerceIn(0f, 1f)
                    },
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(scroll).padding(24.dp),
            ) {
                var photos by remember(chat.id) { mutableStateOf(false) }
                Box(Modifier.clip(CircleShape).clickable(enabled = chat.privateUserId != 0L && chat.photo != null) { photos = true }) {
                    ChatAvatar(chat, telegram, settings, size = 96.dp)
                }
                if (photos) ProfilePhotosDialog(telegram, chat.privateUserId, onDismiss = { photos = false })
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EmojiText(telegram.chatTitle(chat), style = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center))
                    com.pixelgram.desktop.ui.components.ChatBadges(chat, telegram, 30.dp)
                }
                val subtitle = telegram.presence(chat)?.label ?: when {
                    chat.isChannel -> "channel"
                    chat.isGroup -> "group"
                    else -> ""
                }
                if (subtitle.isNotEmpty()) Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!telegram.isSavedMessages(chat.id)) {
                    var moreAt by remember(chat.id) { mutableStateOf<DpOffset?>(null) }
                    val muted = telegram.isMuted(chat)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    ) {
                        if (onOpenChat != null) {
                            ProfileAction(Icons.AutoMirrored.Outlined.Chat, if (chat.isChannel) "Open" else "Message", Modifier.weight(1f)) {
                                onDismiss()
                                onOpenChat(chat.id)
                            }
                        }
                        ProfileAction(
                            if (muted) Icons.Outlined.NotificationsOff else Icons.Outlined.Notifications,
                            if (muted) "Unmute" else "Mute",
                            Modifier.weight(1f),
                            checked = muted,
                        ) { telegram.setMuted(chat, !muted) }
                        Box(Modifier.weight(1f)) {
                            ProfileAction(Icons.Outlined.MoreHoriz, "More", Modifier.fillMaxWidth()) { moreAt = DpOffset(0.dp, 0.dp) }
                            ChatActions(chat, telegram, settings, moreAt, onDismiss = { moreAt = null }, onOpenChat = onOpenChat, showProfile = false)
                        }
                    }
                }
                val rows = buildList {
                    if (chat.privateUserId == 0L) {
                        val names = telegram.usernames(chat)
                        names.firstOrNull()?.let { main ->
                            val also = names.drop(1)
                            add(
                                InfoRow(
                                    Icons.Outlined.Link,
                                    if (also.isEmpty()) "Link" else "Link · also " + also.joinToString(", ") { "@$it" },
                                    "t.me/$main",
                                ),
                            )
                        }
                        if (about.isNotBlank()) add(InfoRow(Icons.Outlined.Info, "Description", about))
                    }
                    val extra = user?.usernames?.activeUsernames?.drop(1).orEmpty()
                    user?.usernames?.activeUsernames?.firstOrNull()?.let { username ->
                        add(
                            InfoRow(
                                Icons.Outlined.AlternateEmail,
                                if (extra.isEmpty()) "Username" else "Username · also " + extra.joinToString(", ") { "@$it" },
                                "@$username",
                                trailing = { UsernameQrButton(username, telegram.chatTitle(chat), user.profilePhoto, telegram) },
                            )
                        )
                    }
                    user?.phoneNumber?.takeIf { it.isNotEmpty() }?.let { add(InfoRow(Icons.Outlined.Phone, "Phone", "+$it")) }
                    if (bio.isNotBlank()) {
                        add(
                            InfoRow(
                                Icons.Outlined.Info,
                                "Bio",
                                bio,
                                entities = bioEntities,
                                onMention = { username ->
                                    telegram.openPublicChat(username, { id -> telegram.chats[id]?.let { mentioned = it } }, { Links.notFound = username })
                                },
                            )
                        )
                    }
                    add(InfoRow(Icons.Outlined.Numbers, "ID", (if (chat.privateUserId != 0L) chat.privateUserId else chat.id).toString()))
                }
                if (chat.privateUserId != 0L) ProfileMusic(telegram, settings, chat.privateUserId, own = false, modifier = Modifier.padding(top = 8.dp))
                Box(Modifier.fillMaxWidth().padding(top = 8.dp)) { InfoGroup(rows) }
                ChatMemberRows(chat, telegram, Modifier.fillMaxWidth().padding(top = 8.dp))
                if (personalChannel != 0L) {
                    PersonalChannelCard(
                        personalChannel,
                        telegram,
                        settings,
                        onOpen = { id ->
                            onDismiss()
                            onOpenChat?.invoke(id)
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
                if (!telegram.isSavedMessages(chat.id)) {
                    SharedMediaSection(
                        chat,
                        telegram,
                        settings,
                        onOpenChat = { id ->
                            onDismiss()
                            onOpenChat?.invoke(id)
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Close") }
            }
        }
      }
        }
}

@Composable
private fun ProfileAction(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    checked: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(if (checked) colors.secondaryContainer else colors.surfaceContainerHighest)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val corner by animateDpAsState(
        if (pressed) ActionPressedCorner else ActionCorner,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
    )
    Surface(
        onClick = onClick,
        interactionSource = interaction,
        shape = RoundedCornerShape(corner),
        color = container,
        contentColor = if (checked) colors.onSecondaryContainer else colors.onSurface,
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(vertical = 10.dp),
        ) {
            androidx.compose.animation.AnimatedContent(
                targetState = icon to label,
                transitionSpec = {
                    (androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(initialScale = 0.7f)) togetherWith
                        (androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(targetScale = 0.7f))
                },
                contentAlignment = Alignment.Center,
            ) { (shownIcon, shownLabel) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(shownIcon, contentDescription = null)
                    Text(shownLabel, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

private val MenuWidth = 260.dp
private val ActionCorner = 18.dp
private val ActionPressedCorner = 32.dp

private val DialogBannerHeight = 140.dp
private val DialogMaxHeight = 640.dp
private const val BannerParallax = 0.6f
