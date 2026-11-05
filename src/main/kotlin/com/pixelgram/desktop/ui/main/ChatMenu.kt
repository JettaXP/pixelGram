package com.pixelgram.desktop.ui.main

import com.pixelgram.desktop.ui.components.ConfirmOnEnter
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.IconButton
import com.pixelgram.desktop.ui.icons.outlined.ContentPaste
import com.pixelgram.desktop.ui.icons.outlined.Folder
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.pixelgram.desktop.ui.icons.outlined.Share
import com.pixelgram.desktop.ui.icons.outlined.LinkOff
import com.pixelgram.desktop.ui.icons.outlined.Upload
import com.pixelgram.desktop.ui.icons.outlined.ChatBubbleOutline
import com.pixelgram.desktop.ui.icons.outlined.MarkChatUnread
import com.pixelgram.desktop.ui.icons.outlined.Image
import com.pixelgram.desktop.ui.icons.outlined.CardGiftcard
import com.pixelgram.desktop.ui.icons.outlined.Edit
import com.pixelgram.desktop.ui.icons.outlined.DriveFileRenameOutline
import com.pixelgram.desktop.ui.icons.outlined.PersonAdd
import com.pixelgram.desktop.ui.icons.outlined.Link
import com.pixelgram.desktop.ui.icons.outlined.Key
import com.pixelgram.desktop.ui.icons.outlined.Lock
import com.pixelgram.desktop.telegram.secretChatWith
import com.pixelgram.desktop.telegram.canSetChatTheme
import com.pixelgram.desktop.ui.icons.outlined.Palette
import com.pixelgram.desktop.telegram.startSecretChat
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
import com.pixelgram.desktop.ui.icons.outlined.Numbers
import com.pixelgram.desktop.ui.icons.outlined.Info
import com.pixelgram.desktop.ui.icons.outlined.Phone
import com.pixelgram.desktop.ui.icons.outlined.AlternateEmail
import com.pixelgram.desktop.ui.icons.outlined.FavoriteBorder
import com.pixelgram.desktop.ui.components.InfoRow
import com.pixelgram.desktop.ui.components.UsernameQrButton
import com.pixelgram.desktop.ui.components.InfoGroup
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
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.outlined.Flag
import com.pixelgram.desktop.ui.icons.outlined.Upgrade
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
import com.pixelgram.desktop.ui.icons.outlined.PhoneAndroid
import com.pixelgram.desktop.ui.icons.outlined.Send
import com.pixelgram.desktop.ui.icons.outlined.Visibility
import com.pixelgram.desktop.ui.icons.outlined.Folder
import com.pixelgram.desktop.ui.icons.outlined.KeyboardDoubleArrowUp
import com.pixelgram.desktop.ui.icons.outlined.Notifications
import com.pixelgram.desktop.ui.icons.outlined.NotificationsOff
import com.pixelgram.desktop.ui.icons.outlined.PushPin
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
import com.pixelgram.desktop.telegram.can
import com.pixelgram.desktop.telegram.canManage
import com.pixelgram.desktop.telegram.canManageVideoChats
import com.pixelgram.desktop.telegram.activeVideoChat
import com.pixelgram.desktop.ui.icons.outlined.Sensors
import com.pixelgram.desktop.ui.icons.outlined.Videocam
import com.pixelgram.desktop.ui.icons.outlined.Forum
import com.pixelgram.desktop.ui.icons.outlined.Tune
import com.pixelgram.desktop.ui.icons.outlined.Timer
import androidx.compose.ui.unit.sp
import com.pixelgram.desktop.ui.components.ChatAvatar
import com.pixelgram.desktop.ui.theme.EmojiText

object ChatWindows {
    val open = mutableStateListOf<Pair<Telegram, Long>>()
}

private enum class Confirm { Clear, Delete, Block }

private enum class MenuPage { Main, Folders, Mute }

private val MuteChoices = listOf(
    "For 1 hour" to 3600,
    "For 8 hours" to 8 * 3600,
    "For 1 day" to 24 * 3600,
    "For 7 days" to 7 * 24 * 3600,
    "Forever" to 0,
)

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
    var managing by remember(chat.id) { mutableStateOf(false) }
    var autoDeleting by remember(chat.id) { mutableStateOf(false) }
    var keyShown by remember(chat.id) { mutableStateOf(false) }
    var theming by remember(chat.id) { mutableStateOf(false) }
    var boosting by remember(chat.id) { mutableStateOf(false) }
    var reportingChat by remember(chat.id) { mutableStateOf(false) }
    var startingVideoChat by remember(chat.id) { mutableStateOf(false) }
    var secretError by remember(chat.id) { mutableStateOf<String?>(null) }
    var exporting by remember(chat.id) { mutableStateOf(false) }
    var gifting by remember(chat.id) { mutableStateOf(false) }
    LaunchedEffect(chat.id, at != null) {
        if (at != null && chat.privateUserId != 0L) telegram.userFullInfo(chat.privateUserId) {}
    }
    if (autoDeleting) {
        AutoDeleteDialog(
            title = "Auto-delete messages",
            description = "Automatically delete new messages sent in this chat after a certain period of time.",
            current = chat.autoDelete,
            choices = if (telegram.isSecret(chat)) SecretAutoDeleteChoices else AutoDeleteChoices,
            onDismiss = { autoDeleting = false },
        ) { seconds, done -> telegram.setAutoDelete(chat, seconds, done) }
    }
    var lastAt by remember { mutableStateOf(at) }
    if (at != null) lastAt = at
    val shownAt = lastAt
    if (shownAt != null) {
        var page by remember(shownAt) { mutableStateOf(MenuPage.Main) }
        Box(Modifier.offset(shownAt.x, shownAt.y)) {
            GroupedMenu(expanded = at != null, onDismiss = onDismiss) {
                AnimatedContent(
                    targetState = page,
                    transitionSpec = {
                        val direction = if (targetState != MenuPage.Main) 1 else -1
                        (slideInHorizontally { it / 3 * direction } + fadeIn(com.pixelgram.desktop.ui.components.Motion.effects())) togetherWith
                            (slideOutHorizontally { -it / 3 * direction } + fadeOut(com.pixelgram.desktop.ui.components.Motion.fastEffects()))
                    },
                ) { current ->
                    Column {
                        val act = { action: () -> Unit -> { onDismiss(); action() } }
                        if (current == MenuPage.Folders) {
                            MenuGroups(listOf(
                                listOf(MenuEntry(Icons.AutoMirrored.Filled.ArrowBack, "Add to folder") { page = MenuPage.Main }),
                                telegram.folders.map { folder -> MenuEntry(Icons.Outlined.Folder, folder.title, onClick = act { telegram.addToFolder(chat, folder.id) }) },
                            ))
                            return@Column
                        }
                        if (current == MenuPage.Mute) {
                            MenuGroups(listOf(
                                listOf(MenuEntry(Icons.AutoMirrored.Filled.ArrowBack, "Mute notifications") { page = MenuPage.Main }),
                                MuteChoices.map { (label, seconds) -> MenuEntry(Icons.Outlined.NotificationsOff, label, onClick = act { telegram.setMutedFor(chat, seconds) }) },
                            ))
                            return@Column
                        }
                        val inChat = onSearch != null
                        val inList = !inChat && showProfile
                        val inProfile = !inChat && !showProfile
                        val muted = telegram.isMuted(chat)
                        val pinned = chat.pinned[listKey] == true
                        val archived = ChatListKey.Archive in chat.positions
                        val saved = telegram.isSavedMessages(chat.id)
                        val person = chat.privateUserId != 0L && !saved
                        val bot = telegram.isBot(chat)
                        val leaveLabel = if (chat.privateUserId != 0L) "Delete chat" else if (chat.isChannel) "Leave channel" else "Leave group"
                        val muteEntry = MenuEntry(
                            if (muted) Icons.Outlined.Notifications else Icons.Outlined.NotificationsOff,
                            if (muted) "Unmute" else "Mute notifications",
                            trailing = if (muted) null else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        ) { if (muted) act { telegram.setMuted(chat, false) }() else page = MenuPage.Mute }
                        val clearEntry = MenuEntry(Icons.Outlined.CleaningServices, "Clear history", onClick = act { confirm = Confirm.Clear })
                        val deleteEntry = MenuEntry(Icons.Outlined.Delete, leaveLabel, color = MaterialTheme.colorScheme.error, onClick = act { confirm = Confirm.Delete })
                        val sharing = if (!saved && !chat.isChannel && (person || telegram.canManage(chat))) {
                            MenuEntry(if (chat.protectedContent) Icons.Outlined.Share else Icons.Outlined.LinkOff, if (chat.protectedContent) "Enable Sharing" else "Disable Sharing",
                                onClick = act { telegram.setProtectedContent(chat, !chat.protectedContent) })
                        } else null
                        val export = MenuEntry(Icons.Outlined.Upload, "Export chat history", onClick = act { exporting = true })
                        val groups: List<List<MenuEntry>> = when {
                            inList -> listOf(
                                listOf(MenuEntry(Icons.AutoMirrored.Outlined.OpenInNew, "Open in new window", onClick = act {
                                    if (ChatWindows.open.none { it.second == chat.id }) ChatWindows.open.add(telegram to chat.id)
                                })),
                                buildList {
                                    add(MenuEntry(if (archived) Icons.Outlined.Unarchive else Icons.Outlined.Archive, if (archived) "Unarchive" else "Archive",
                                        onClick = act { telegram.setArchived(chat, !archived) }))
                                    add(MenuEntry(Icons.Outlined.PushPin, if (pinned) "Unpin" else "Pin", onClick = act { telegram.setPinned(chat, listKey, !pinned) }))
                                    add(muteEntry)
                                    val unread = chat.unreadCount > 0 || chat.markedUnread
                                    add(MenuEntry(if (unread) Icons.Outlined.ChatBubbleOutline else Icons.Outlined.MarkChatUnread, if (unread) "Mark as read" else "Mark as unread",
                                        onClick = act { if (unread) telegram.markChatRead(chat) else telegram.markChatUnread(chat) }))
                                    if (telegram.folders.isNotEmpty()) {
                                        add(MenuEntry(Icons.Outlined.CreateNewFolder, "Add to folder", trailing = Icons.AutoMirrored.Filled.KeyboardArrowRight) { page = MenuPage.Folders })
                                    }
                                    add(clearEntry)
                                    add(deleteEntry)
                                },
                            )
                            inChat -> listOf(
                                listOf(muteEntry),
                                buildList {
                                    add(MenuEntry(Icons.Outlined.AccountCircle, "View profile", onClick = act { profile = true }))
                                    if (chat.privateUserId == 0L && telegram.canManage(chat)) {
                                        add(MenuEntry(Icons.Outlined.Tune, if (chat.isChannel) "Manage channel" else "Manage group", onClick = act { managing = true }))
                                    }
                                    if (telegram.canManageVideoChats(chat) && telegram.activeVideoChat(chat) == null) {
                                        add(MenuEntry(if (chat.isChannel) Icons.Outlined.Sensors else Icons.Outlined.Videocam, if (chat.isChannel) "Start live stream" else "Start video chat",
                                            onClick = act { startingVideoChat = true }))
                                    }
                                    if (telegram.canSetChatTheme(chat)) add(MenuEntry(Icons.Outlined.Image, "Set Wallpaper", onClick = act { theming = true }))
                                    sharing?.let(::add)
                                    if (chat.supergroupId != 0L) add(MenuEntry(Icons.Outlined.Upgrade, if (chat.isChannel) "Boost channel" else "Boost group", onClick = act { boosting = true }))
                                    if (chat.unreadMentions > 0) add(MenuEntry(Icons.Outlined.AlternateEmail, "Read all mentions", onClick = act { telegram.readAllMentions(chat) }))
                                    if (chat.unreadReactions > 0) add(MenuEntry(Icons.Outlined.FavoriteBorder, "Read all reactions", onClick = act { telegram.readAllReactions(chat) }))
                                    onToBeginning?.let { add(MenuEntry(Icons.Outlined.KeyboardDoubleArrowUp, "Go to beginning", onClick = act(it))) }
                                    add(export)
                                    if (!saved && !telegram.isOfficial(chat) && chat.privateUserId == 0L) add(MenuEntry(Icons.Outlined.Flag, "Report", onClick = act { reportingChat = true }))
                                    add(clearEntry)
                                    add(deleteEntry)
                                },
                            )
                            else -> listOf(
                                buildList { if (telegram.canSetAutoDelete(chat)) add(MenuEntry(Icons.Outlined.Timer, "Auto-Delete", onClick = act { autoDeleting = true })) },
                                buildList {
                                    if (person && !bot) {
                                        if (telegram.isContact(chat)) {
                                            add(MenuEntry(Icons.Outlined.Edit, "Edit contact", onClick = act { contactEdit = true }))
                                        } else {
                                            add(MenuEntry(Icons.Outlined.PersonAdd, "Add to contacts", onClick = act { contactEdit = true }))
                                        }
                                    }
                                    if (!saved && !(person && !bot && telegram.isContact(chat))) {
                                        add(MenuEntry(Icons.Outlined.DriveFileRenameOutline, "Rename", onClick = act { renaming = true }))
                                    }
                                    if (person && !bot) add(MenuEntry(Icons.Outlined.CardGiftcard, "Send a Gift", onClick = act { gifting = true }))
                                    add(export)
                                    sharing?.let(::add)
                                    if (telegram.folders.isNotEmpty()) {
                                        add(MenuEntry(Icons.Outlined.CreateNewFolder, "Add to folder", trailing = Icons.AutoMirrored.Filled.KeyboardArrowRight) { page = MenuPage.Folders })
                                    }
                                    if (person && !bot) {
                                        if (telegram.isSecret(chat)) {
                                            add(MenuEntry(Icons.Outlined.Key, "Encryption key", onClick = act { keyShown = true }))
                                        } else if (onOpenChat != null) {
                                            add(MenuEntry(Icons.Outlined.Lock, "Start secret chat", onClick = act {
                                                val existing = telegram.secretChatWith(chat.privateUserId)
                                                if (existing != null) onOpenChat(existing.id)
                                                else telegram.startSecretChat(chat.privateUserId) { chatId, error ->
                                                    if (chatId != null) onOpenChat(chatId) else secretError = error
                                                }
                                            }))
                                        }
                                    }
                                    if (person) {
                                        if (telegram.isBlocked(chat)) {
                                            add(MenuEntry(Icons.Outlined.LockOpen, if (bot) "Unblock bot" else "Unblock user", onClick = act { telegram.unblock(chat) }))
                                        } else {
                                            add(MenuEntry(Icons.Outlined.Block, if (bot) "Stop and block bot" else "Block user", onClick = act { confirm = Confirm.Block }))
                                        }
                                    }
                                    if (!saved && !telegram.isOfficial(chat)) add(MenuEntry(Icons.Outlined.Flag, "Report", onClick = act { reportingChat = true }))
                                    if (person && !bot && telegram.isContact(chat)) {
                                        add(MenuEntry(Icons.Outlined.Delete, "Delete contact", color = MaterialTheme.colorScheme.error, onClick = act { telegram.removeContact(chat.privateUserId) }))
                                    } else if (chat.privateUserId == 0L) {
                                        add(deleteEntry)
                                    }
                                },
                            )
                        }
                        MenuGroups(groups)
                    }
                }
            }
        }
    }
    confirm?.let { which ->
        val title = telegram.chatTitle(chat)
        val apply = {
            when (which) {
                Confirm.Clear -> telegram.clearHistory(chat)
                Confirm.Delete -> telegram.deleteChat(chat)
                Confirm.Block -> telegram.block(chat)
            }
            confirm = null
        }
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
                ConfirmOnEnter(apply) {
                    Text(
                        when (which) {
                            Confirm.Clear -> "All messages in this chat will be deleted for you."
                            Confirm.Delete -> "The chat will be removed from your chat list."
                            Confirm.Block -> "They won't be able to message you."
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = apply) { Text(if (which == Confirm.Clear) "Clear" else if (which == Confirm.Block) "Block" else "Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel") } },
        )
    }
    if (profile) ChatInfoDialog(chat, telegram, settings, onOpenChat) { profile = false }
    if (contactEdit) ContactDialog(chat, telegram) { contactEdit = false }
    if (renaming) RenameDialog(chat, telegram) { renaming = false }
    if (managing) ManageChatDialog(chat, telegram, settings = settings, onDismiss = { managing = false })
    if (keyShown) EncryptionKeyDialog(chat, telegram) { keyShown = false }
    if (theming) ChatThemeDialog(chat, telegram, settings) { theming = false }
    if (boosting) BoostDialog(chat, telegram) { boosting = false }
    if (reportingChat) ReportDialog(telegram, chat.id, LongArray(0)) { reportingChat = false }
    if (startingVideoChat) StartVideoChatDialog(chat, telegram) { startingVideoChat = false }
    if (exporting) ExportChatDialog(chat, telegram) { exporting = false }
    if (gifting) SendGiftDialog(chat, telegram, settings) { gifting = false }
    secretError?.let { error ->
        AlertDialog(
            onDismissRequest = { secretError = null },
            title = { Text("Start secret chat") },
            text = { Text(error) },
            confirmButton = { TextButton(onClick = { secretError = null }) { Text("OK") } },
        )
    }
}

@Composable
private fun ContactDialog(chat: ChatState, telegram: Telegram, onDismiss: () -> Unit) {
    val userId = chat.privateUserId
    val user = telegram.users[userId]
    val existing = telegram.isContact(chat)
    if (!existing && user != null) {
        LaunchedEffect(userId) {
            com.pixelgram.desktop.core.OriginalNames.set(userId, listOf(user.firstName, user.lastName).joinToString("\u0000"))
        }
    }
    val original = com.pixelgram.desktop.core.OriginalNames.get(userId)?.split("\u0000")
    var first by remember { mutableStateOf(user?.firstName.orEmpty()) }
    var last by remember { mutableStateOf(user?.lastName.orEmpty()) }
    var status by remember { mutableStateOf<String?>(null) }
    var personal by remember { mutableStateOf(false) }
    LaunchedEffect(userId) { telegram.userFullInfo(userId) { info -> personal = info?.personalPhoto != null } }
    val scope = rememberCoroutineScope()
    fun withImage(fromClipboard: Boolean, use: (String) -> Unit) {
        scope.launch {
            val file = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                (if (fromClipboard) Attachments.fromClipboard() else Attachments.pick()).firstOrNull { it.extension.lowercase() in PhotoExtensions }
            }
            if (file == null) status = if (fromClipboard) "No image in the clipboard" else null else use(file.absolutePath)
        }
    }
    val report: (String?) -> Unit = { error -> status = error }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing) "Edit contact" else "Add to contacts") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ChatAvatar(
                        chat,
                        telegram,
                        settings = com.pixelgram.desktop.ui.theme.LocalAppSettings.current ?: return@Column,
                        size = 88.dp,
                        onClick = if (chat.photo != null) {
                            { com.pixelgram.desktop.ui.profile.openProfilePhotos(telegram, userId, telegram.chatTitle(chat)) }
                        } else {
                            null
                        },
                    )
                    val note = when {
                        com.pixelgram.desktop.core.LocalPhotos.get(userId) != null -> "Local photo, only on this device"
                        else -> null
                    }
                    note?.let { Text(com.pixelgram.desktop.core.tr(it), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) }
                }
                androidx.compose.material3.OutlinedTextField(first, { first = it }, label = { Text("First name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                androidx.compose.material3.OutlinedTextField(last, { last = it }, label = { Text("Last name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                val originalFirst = original?.getOrNull(0).orEmpty()
                val originalLast = original?.getOrNull(1).orEmpty()
                if (original != null && (first != originalFirst || last != originalLast)) {
                    TextButton(onClick = {
                        first = originalFirst
                        last = originalLast
                    }) { Text("Use original name") }
                }
                Text("Photo", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp, top = 8.dp))
                val rows = if (existing) 3 else 2
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                LocalPhotoRow(userId, 0, rows)
                if (existing) {
                    PhotoSourceRow(
                        1,
                        rows,
                        Icons.Outlined.Visibility,
                        "Set photo for me",
                        "Only you will see it, on all your devices",
                        onFile = { withImage(false) { path -> telegram.setPersonalPhoto(userId, path) { report(it); if (it == null) personal = true } } },
                        onPaste = { withImage(true) { path -> telegram.setPersonalPhoto(userId, path) { report(it); if (it == null) personal = true } } },
                        onRemove = if (personal) {
                            { telegram.setPersonalPhoto(userId, null) { report(it); if (it == null) personal = false } }
                        } else {
                            null
                        },
                    )
                }
                PhotoSourceRow(
                    rows - 1,
                    rows,
                    Icons.Outlined.Send,
                    "Suggest photo",
                    com.pixelgram.desktop.core.tr("{name} can set it as their photo").replace("{name}", telegram.chatTitle(chat)),
                    onFile = { withImage(false) { path -> telegram.suggestPhoto(userId, path, report) } },
                    onPaste = { withImage(true) { path -> telegram.suggestPhoto(userId, path, report) } },
                    onRemove = null,
                )
                }
                status?.let { Text(com.pixelgram.desktop.core.tr(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
                if (existing) {
                    androidx.compose.material3.HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    com.pixelgram.desktop.ui.components.expressive.FilledTonalButton(
                        onClick = {
                            telegram.removeContact(userId)
                            onDismiss()
                        },
                        colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Delete contact")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = first.isNotBlank(), onClick = {
                telegram.saveContact(userId, first.trim(), last.trim())
                onDismiss()
            }) { Text("Done") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun PhotoSourceRow(
    index: Int,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onFile: () -> Unit,
    onPaste: () -> Unit,
    onRemove: (() -> Unit)?,
) {
    val colors = MaterialTheme.colorScheme
    androidx.compose.material3.Surface(
        shape = com.pixelgram.desktop.ui.components.groupShapes(index, count).shape,
        color = colors.surfaceContainerHighest,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        ) {
            Box(
                Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(colors.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, contentDescription = null, tint = colors.onSecondaryContainer, modifier = Modifier.size(20.dp)) }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                com.pixelgram.desktop.ui.components.expressive.FilledTonalIconButton(onClick = onFile) {
                    Icon(Icons.Outlined.Folder, contentDescription = "Choose file", modifier = Modifier.size(20.dp))
                }
                com.pixelgram.desktop.ui.components.expressive.FilledTonalIconButton(onClick = onPaste) {
                    Icon(Icons.Outlined.ContentPaste, contentDescription = "Paste from clipboard", modifier = Modifier.size(20.dp))
                }
                if (onRemove != null) {
                    com.pixelgram.desktop.ui.components.expressive.FilledTonalIconButton(
                        onClick = onRemove,
                        colors = androidx.compose.material3.IconButtonDefaults.filledTonalIconButtonColors(colors.errorContainer, colors.onErrorContainer),
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Remove photo", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalPhotoRow(key: Long, index: Int = 0, count: Int = 1) {
    val scope = rememberCoroutineScope()
    var missing by remember { mutableStateOf(false) }
    val local = com.pixelgram.desktop.core.LocalPhotos.get(key)
    fun take(fromClipboard: Boolean) {
        scope.launch {
            val file = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                (if (fromClipboard) Attachments.fromClipboard() else Attachments.pick()).firstOrNull { it.extension.lowercase() in PhotoExtensions }
            }
            missing = file == null && fromClipboard
            if (file != null) com.pixelgram.desktop.core.LocalPhotos.set(key, keepLocalPhoto(key, file.absolutePath))
        }
    }
    PhotoSourceRow(
        index,
        count,
        Icons.Outlined.PhoneAndroid,
        "Local photo",
        "Only on this device",
        onFile = { take(false) },
        onPaste = { take(true) },
        onRemove = if (local != null) {
            { com.pixelgram.desktop.core.LocalPhotos.set(key, null) }
        } else {
            null
        },
    )
    if (missing) Text(com.pixelgram.desktop.core.tr("No image in the clipboard"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
}

private fun keepLocalPhoto(userId: Long, path: String): String? = runCatching {
    val source = java.io.File(path)
    val dir = java.io.File(com.pixelgram.desktop.core.AppPaths.dataDir, "local-photos").apply { mkdirs() }
    dir.listFiles { file -> file.name.startsWith("$userId.") }?.forEach { it.delete() }
    val target = java.io.File(dir, "$userId.${System.currentTimeMillis()}.${source.extension.lowercase()}")
    source.copyTo(target, overwrite = true)
    target.absolutePath
}.getOrNull()

private val PhotoExtensions = setOf("jpg", "jpeg", "png", "webp", "bmp", "gif")

@Composable
private fun RenameDialog(chat: ChatState, telegram: Telegram, onDismiss: () -> Unit) {
    val person = chat.privateUserId != 0L && !telegram.isBot(chat)
    val key = if (chat.privateUserId != 0L) chat.privateUserId else chat.id
    val current = com.pixelgram.desktop.core.LocalNames.get(key)
    val user = telegram.users[chat.privateUserId]
    val originalFirst = if (person) user?.firstName.orEmpty() else if (chat.privateUserId != 0L) user?.firstName.orEmpty() else chat.title
    val originalLast = if (person) user?.lastName.orEmpty() else ""
    var first by remember { mutableStateOf(if (person) current?.substringBefore(' ') ?: originalFirst else current ?: originalFirst) }
    var last by remember { mutableStateOf(if (person) current?.substringAfter(' ', "") ?: originalLast else "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Only you will see this name.", style = MaterialTheme.typography.bodyMedium)
                androidx.compose.material3.OutlinedTextField(first, { first = it }, label = { Text(if (person) "First name" else "Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (person) {
                    androidx.compose.material3.OutlinedTextField(last, { last = it }, label = { Text("Last name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                TextButton(
                    enabled = first != originalFirst || last != originalLast,
                    onClick = {
                        first = originalFirst
                        last = originalLast
                    },
                ) { Text("Use original name") }
                LocalPhotoRow(key)
            }
        },
        confirmButton = {
            TextButton(enabled = first.isNotBlank(), onClick = {
                val name = listOf(first.trim(), last.trim()).filter(String::isNotBlank).joinToString(" ")
                val original = listOf(originalFirst.trim(), originalLast.trim()).filter(String::isNotBlank).joinToString(" ")
                com.pixelgram.desktop.core.LocalNames.set(key, name.takeIf { it != original })
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
    var managing by remember(chat.id) { mutableStateOf(false) }
    if (managing) ManageChatDialog(chat, telegram, settings = settings, onDismiss = { managing = false })
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.widthIn(min = 360.dp, max = 460.dp).heightIn(max = DialogMaxHeight),
        ) {
          val scroll = rememberScrollState()
          com.pixelgram.desktop.ui.BackSwipeBox(onBack = onDismiss) {
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
                ChatAvatar(
                    chat,
                    telegram,
                    settings,
                    size = 96.dp,
                    onClick = when {
                        chat.privateUserId != 0L && chat.photo != null -> {
                            { com.pixelgram.desktop.ui.profile.openProfilePhotos(telegram, chat.privateUserId, telegram.chatTitle(chat)) }
                        }
                        chat.bigPhoto != null -> {
                            { chat.bigPhoto?.let { big -> PhotoViewer.current = ViewedPhoto(telegram, chat.photo ?: big, big, "", subtitle = telegram.chatTitle(chat)) } }
                        }
                        else -> null
                    },
                )
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
                if (chat.privateUserId != 0L) {
                    val note = when {
                        com.pixelgram.desktop.core.LocalPhotos.get(chat.privateUserId) != null -> "Local photo, only on this device"
                        else -> null
                    }
                    note?.let { Text(com.pixelgram.desktop.core.tr(it), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) }
                }
                telegram.communityOf(chat)?.let { community ->
                    var openCommunity by remember(chat.id) { mutableStateOf(false) }
                    val count = telegram.communityInfo[community.id]?.chats?.size
                    LaunchedEffect(community.id) { telegram.communityChats(community.id) {} }
                    androidx.compose.material3.Surface(
                        onClick = { openCommunity = true },
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(12.dp),
                        ) {
                            com.pixelgram.desktop.ui.components.AvatarImage(community.name, community.photo?.small, telegram, RoundedCornerShape(12.dp), 44.dp)
                            Column {
                                EmojiText(community.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    com.pixelgram.desktop.core.tr("Community") + (count?.let { " · $it ${com.pixelgram.desktop.core.tr("chats")}" } ?: ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    if (openCommunity) {
                        CommunityDialog(community, telegram, settings, onOpen = { id ->
                            openCommunity = false
                            Links.navigate?.invoke(telegram, id)
                        }) { openCommunity = false }
                    }
                }
                if (!telegram.isSavedMessages(chat.id)) {
                    var moreAt by remember(chat.id) { mutableStateOf<DpOffset?>(null) }
                    val muted = telegram.isMuted(chat)
                    var discussion by remember(chat.id) { mutableStateOf(0L) }
                    if (chat.isChannel) LaunchedEffect(chat.id) { telegram.linkedChat(chat) { discussion = it } }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    ) {
                        if (onOpenChat != null && !chat.isChannel) {
                            ProfileAction(Icons.AutoMirrored.Outlined.Chat, "Message", Modifier.weight(1f)) {
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
                        if (onOpenChat != null && discussion != 0L) {
                            ProfileAction(Icons.Outlined.Forum, "Discuss", Modifier.weight(1f)) {
                                onDismiss()
                                onOpenChat(discussion)
                            }
                        }
                        if (chat.privateUserId == 0L && telegram.canManage(chat)) {
                            ProfileAction(Icons.Outlined.Tune, "Manage", Modifier.weight(1f)) { managing = true }
                        }
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
                        if (about.isNotBlank()) {
                            add(
                                InfoRow(
                                    Icons.Outlined.Info,
                                    "Description",
                                    about,
                                    onMention = { username ->
                                        telegram.openPublicChat(username, { id -> telegram.chats[id]?.let { mentioned = it } }, { Links.notFound = username })
                                    },
                                ),
                            )
                        }
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
                ChatMemberRows(chat, telegram, settings, onOpenChat, Modifier.fillMaxWidth().padding(top = 8.dp))
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
                com.pixelgram.desktop.ui.components.expressive.OutlinedButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Close") }
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
    val container by animateColorAsState(if (checked) colors.secondaryContainer else colors.surfaceContainerHighest, animationSpec = com.pixelgram.desktop.ui.components.Motion.effects())
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
                    (androidx.compose.animation.fadeIn(com.pixelgram.desktop.ui.components.Motion.effects()) + androidx.compose.animation.scaleIn(initialScale = 0.7f)) togetherWith
                        (androidx.compose.animation.fadeOut(com.pixelgram.desktop.ui.components.Motion.fastEffects()) + androidx.compose.animation.scaleOut(targetScale = 0.7f))
                },
                contentAlignment = Alignment.Center,
            ) { (shownIcon, shownLabel) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(shownIcon, contentDescription = null)
                    val style = MaterialTheme.typography.labelMedium
                    androidx.compose.foundation.text.BasicText(
                        com.pixelgram.desktop.core.tr(shownLabel),
                        style = style.copy(color = androidx.compose.material3.LocalContentColor.current),
                        maxLines = 1,
                        autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(minFontSize = ActionMinLabel, maxFontSize = style.fontSize),
                        modifier = Modifier.padding(horizontal = 6.dp),
                    )
                }
            }
        }
    }
}

private val ActionMinLabel = 9.sp
private val MenuWidth = 260.dp
private val ActionCorner = 18.dp
private val ActionPressedCorner = 32.dp

private val DialogBannerHeight = 140.dp
private val DialogMaxHeight = 640.dp
private const val BannerParallax = 0.6f
