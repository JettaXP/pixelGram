package com.pixelgram.desktop.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.tr
import com.pixelgram.desktop.telegram.ChatState
import com.pixelgram.desktop.telegram.contentText
import com.pixelgram.desktop.telegram.MemberList
import com.pixelgram.desktop.telegram.MemberRank
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.telegram.banMember
import com.pixelgram.desktop.telegram.can
import com.pixelgram.desktop.telegram.eventLog
import com.pixelgram.desktop.telegram.isOwner
import com.pixelgram.desktop.telegram.members
import com.pixelgram.desktop.telegram.removeMember
import com.pixelgram.desktop.telegram.setChatDescription
import com.pixelgram.desktop.telegram.setChatPermissions
import com.pixelgram.desktop.telegram.setChatTitle
import com.pixelgram.desktop.telegram.setForum
import com.pixelgram.desktop.telegram.setMemberStatus
import com.pixelgram.desktop.telegram.setMemberTag
import com.pixelgram.desktop.telegram.setSlowModeDelay
import com.pixelgram.desktop.telegram.slowModeDelay
import com.pixelgram.desktop.telegram.unbanMember
import com.pixelgram.desktop.ui.components.AvatarImage
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.components.expressive.BasicAlertDialog
import com.pixelgram.desktop.ui.components.expressive.Button
import com.pixelgram.desktop.ui.components.expressive.IconButton
import com.pixelgram.desktop.ui.components.expressive.TextButton
import com.pixelgram.desktop.ui.components.groupShapes
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.automirrored.filled.ArrowBack
import com.pixelgram.desktop.ui.icons.outlined.AdminPanelSettings
import com.pixelgram.desktop.ui.icons.outlined.Block
import com.pixelgram.desktop.ui.icons.outlined.Close
import com.pixelgram.desktop.ui.icons.outlined.Edit
import com.pixelgram.desktop.ui.icons.outlined.Forum
import com.pixelgram.desktop.ui.icons.outlined.Group
import com.pixelgram.desktop.ui.icons.outlined.History
import com.pixelgram.desktop.ui.icons.outlined.Info
import com.pixelgram.desktop.ui.icons.outlined.PanTool
import com.pixelgram.desktop.ui.icons.outlined.PersonAdd
import com.pixelgram.desktop.ui.icons.outlined.PersonOff
import com.pixelgram.desktop.ui.icons.outlined.Lock
import com.pixelgram.desktop.ui.icons.outlined.Delete
import com.pixelgram.desktop.ui.icons.outlined.TextFields
import com.pixelgram.desktop.ui.settings.PixelSwitch
import com.pixelgram.desktop.telegram.canInvite
import com.pixelgram.desktop.ui.icons.outlined.Link
import com.pixelgram.desktop.ui.theme.EmojiText
import it.tdlight.jni.TdApi
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.reflect.KMutableProperty1

enum class ManageStart { Member, Rights, Restrict, Ban }

private sealed interface ManagePage {
    data object Home : ManagePage
    data object Info : ManagePage
    data object Permissions : ManagePage
    data class Members(val list: MemberList) : ManagePage
    data object Log : ManagePage
    data object Links : ManagePage
    data class Member(val member: TdApi.ChatMember, val askBan: Boolean = false) : ManagePage
    data class Rights(val member: TdApi.ChatMember) : ManagePage
    data class Restrict(val member: TdApi.ChatMember) : ManagePage
}

private val PermissionFlags: List<Pair<String, KMutableProperty1<TdApi.ChatPermissions, Boolean>>> = listOf(
    "Send text messages" to TdApi.ChatPermissions::canSendBasicMessages,
    "Send photos" to TdApi.ChatPermissions::canSendPhotos,
    "Send videos" to TdApi.ChatPermissions::canSendVideos,
    "Send stickers and GIFs" to TdApi.ChatPermissions::canSendOtherMessages,
    "Send music" to TdApi.ChatPermissions::canSendAudios,
    "Send files" to TdApi.ChatPermissions::canSendDocuments,
    "Send voice messages" to TdApi.ChatPermissions::canSendVoiceNotes,
    "Send video messages" to TdApi.ChatPermissions::canSendVideoNotes,
    "Embed links" to TdApi.ChatPermissions::canAddLinkPreviews,
    "Send polls" to TdApi.ChatPermissions::canSendPolls,
    "Add members" to TdApi.ChatPermissions::canInviteUsers,
    "Pin messages" to TdApi.ChatPermissions::canPinMessages,
    "Change chat info" to TdApi.ChatPermissions::canChangeInfo,
    "Create topics" to TdApi.ChatPermissions::canCreateTopics,
    "Edit own tag" to TdApi.ChatPermissions::canEditTag,
)

private class RightFlag(val label: String, val property: KMutableProperty1<TdApi.ChatAdministratorRights, Boolean>, val group: Boolean, val channel: Boolean)

private val RightFlags = listOf(
    RightFlag("Change chat info", TdApi.ChatAdministratorRights::canChangeInfo, group = true, channel = true),
    RightFlag("Post messages", TdApi.ChatAdministratorRights::canPostMessages, group = false, channel = true),
    RightFlag("Edit messages of others", TdApi.ChatAdministratorRights::canEditMessages, group = false, channel = true),
    RightFlag("Delete messages", TdApi.ChatAdministratorRights::canDeleteMessages, group = true, channel = true),
    RightFlag("Ban users", TdApi.ChatAdministratorRights::canRestrictMembers, group = true, channel = false),
    RightFlag("Invite users via link", TdApi.ChatAdministratorRights::canInviteUsers, group = true, channel = true),
    RightFlag("Pin messages", TdApi.ChatAdministratorRights::canPinMessages, group = true, channel = false),
    RightFlag("Manage topics", TdApi.ChatAdministratorRights::canManageTopics, group = true, channel = false),
    RightFlag("Manage video chats", TdApi.ChatAdministratorRights::canManageVideoChats, group = true, channel = true),
    RightFlag("Post stories", TdApi.ChatAdministratorRights::canPostStories, group = true, channel = true),
    RightFlag("Edit stories of others", TdApi.ChatAdministratorRights::canEditStories, group = true, channel = true),
    RightFlag("Delete stories of others", TdApi.ChatAdministratorRights::canDeleteStories, group = true, channel = true),
    RightFlag("Manage direct messages", TdApi.ChatAdministratorRights::canManageDirectMessages, group = false, channel = true),
    RightFlag("Edit member tags", TdApi.ChatAdministratorRights::canManageTags, group = true, channel = false),
    RightFlag("Add new admins", TdApi.ChatAdministratorRights::canPromoteMembers, group = true, channel = true),
    RightFlag("Remain anonymous", TdApi.ChatAdministratorRights::isAnonymous, group = true, channel = false),
)

private val AllRights = RightFlags.map { it.property } + TdApi.ChatAdministratorRights::canManageChat

private fun TdApi.ChatPermissions.copied(): TdApi.ChatPermissions =
    TdApi.ChatPermissions().also { copy -> PermissionFlags.forEach { (_, flag) -> flag.set(copy, flag.get(this)) } }

private fun TdApi.ChatAdministratorRights.copied(): TdApi.ChatAdministratorRights =
    TdApi.ChatAdministratorRights().also { copy -> AllRights.forEach { flag -> flag.set(copy, flag.get(this)) } }

private fun defaultRights(channel: Boolean) = TdApi.ChatAdministratorRights().apply {
    canManageChat = true
    canChangeInfo = true
    canDeleteMessages = true
    canInviteUsers = true
    canManageVideoChats = true
    if (channel) {
        canPostMessages = true
        canEditMessages = true
    } else {
        canRestrictMembers = true
        canPinMessages = true
        canManageTopics = true
        canManageTags = true
    }
}

private val SlowModes = listOf(0 to "Off", 10 to "10s", 30 to "30s", 60 to "1m", 300 to "5m", 900 to "15m", 3600 to "1h")

@Composable
fun tagColor(rank: MemberRank): Color {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return when (rank) {
        MemberRank.Owner -> if (dark) Color(0xFFD0A6FF) else Color(0xFF7B3FC4)
        MemberRank.Admin -> if (dark) Color(0xFF7DD99A) else Color(0xFF1E7F3E)
        MemberRank.Member -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@Composable
fun MemberTagPill(text: String, rank: MemberRank, modifier: Modifier = Modifier) {
    val color = tagColor(rank)
    EmojiText(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = TagPillAlpha))
            .padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

private const val TagPillAlpha = 0.16f

private fun memberRank(member: TdApi.ChatMember): MemberRank = when (member.status) {
    is TdApi.ChatMemberStatusCreator -> MemberRank.Owner
    is TdApi.ChatMemberStatusAdministrator -> MemberRank.Admin
    else -> MemberRank.Member
}

private fun memberLabel(member: TdApi.ChatMember): String? = when (val status = member.status) {
    is TdApi.ChatMemberStatusCreator -> member.tag.orEmpty().ifBlank { tr("owner") }
    is TdApi.ChatMemberStatusAdministrator -> member.tag.orEmpty().ifBlank { tr("admin") }
    is TdApi.ChatMemberStatusBanned -> tr("banned")
    is TdApi.ChatMemberStatusRestricted -> member.tag.orEmpty().ifBlank { tr(if (status.isMember) "restricted" else "restricted, left") }
    else -> member.tag.orEmpty().takeIf { it.isNotBlank() }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ManageChatDialog(
    chat: ChatState,
    telegram: Telegram,
    member: TdApi.ChatMember? = null,
    start: ManageStart = ManageStart.Member,
    settings: com.pixelgram.desktop.core.AppSettings? = null,
    onDismiss: () -> Unit,
) {
    val stack = remember(chat.id) {
        mutableStateListOf<ManagePage>().apply {
            if (member == null) {
                add(ManagePage.Home)
            } else {
                add(ManagePage.Member(member, askBan = start == ManageStart.Ban))
                if (start == ManageStart.Rights) add(ManagePage.Rights(member))
                if (start == ManageStart.Restrict) add(ManagePage.Restrict(member))
            }
        }
    }
    val page = stack.last()
    val go: (ManagePage) -> Unit = { next -> stack.add(next) }
    val back: () -> Unit = { if (stack.size > 1) stack.removeAt(stack.lastIndex) else onDismiss() }
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
    BasicAlertDialog(onDismissRequest = back) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.widthIn(min = 420.dp, max = 520.dp).heightIn(max = ManageMaxHeight),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = back) {
                        Icon(if (stack.size > 1) Icons.AutoMirrored.Filled.ArrowBack else Icons.Outlined.Close, contentDescription = "Back")
                    }
                    Text(pageTitle(page, chat, telegram), style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                AnimatedContent(
                    targetState = page,
                    transitionSpec = {
                        val forward = stack.size > 1 && targetState == stack.last() && initialState != targetState
                        val direction = if (forward) 1 else -1
                        (slideInHorizontally(spatial) { it / 5 * direction } + fadeIn()) togetherWith
                            (slideOutHorizontally(spatial) { -it / 5 * direction } + fadeOut())
                    },
                ) { shown ->
                    when (shown) {
                        ManagePage.Home -> HomePage(chat, telegram, settings, go)
                        ManagePage.Info -> InfoPage(chat, telegram, back)
                        ManagePage.Permissions -> PermissionsPage(chat, telegram, back)
                        is ManagePage.Members -> MembersPage(chat, telegram, shown.list) { go(ManagePage.Member(it)) }
                        ManagePage.Log -> LogPage(chat, telegram)
                        ManagePage.Links -> settings?.let { InviteLinksPage(chat, telegram, it) }
                        is ManagePage.Member -> MemberPage(chat, telegram, shown.member, go, back, shown.askBan)
                        is ManagePage.Rights -> RightsPage(chat, telegram, shown.member, back)
                        is ManagePage.Restrict -> RestrictPage(chat, telegram, shown.member, back)
                    }
                }
            }
        }
    }
}

private fun pageTitle(page: ManagePage, chat: ChatState, telegram: Telegram): String = when (page) {
    ManagePage.Home -> if (chat.isChannel) "Manage channel" else "Manage group"
    ManagePage.Info -> "Name and description"
    ManagePage.Permissions -> "Permissions"
    is ManagePage.Members -> when (page.list) {
        MemberList.Recent -> if (chat.isChannel) "Subscribers" else "Members"
        MemberList.Administrators -> "Administrators"
        MemberList.Banned -> "Removed users"
        MemberList.Restricted -> "Exceptions"
    }
    ManagePage.Log -> "Recent actions"
    ManagePage.Links -> "Invite links"
    is ManagePage.Member -> telegram.senderTitle(page.member.memberId)
    is ManagePage.Rights -> "Admin rights"
    is ManagePage.Restrict -> "Restrictions"
}

private class HomeRow(val icon: ImageVector, val title: String, val detail: String?, val action: () -> Unit)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HomePage(chat: ChatState, telegram: Telegram, settings: com.pixelgram.desktop.core.AppSettings?, go: (ManagePage) -> Unit) {
    val group = !chat.isChannel
    val supergroup = chat.supergroupId != 0L
    val restrict = telegram.can(chat) { it.canRestrictMembers }
    val changeInfo = telegram.can(chat) { it.canChangeInfo }
    val allowed = chat.permissions?.let { perms -> PermissionFlags.count { (_, flag) -> flag.get(perms) } }
    val rows = buildList {
        if (changeInfo) add(HomeRow(Icons.Outlined.Info, "Name and description", null) { go(ManagePage.Info) })
        if (group && restrict) add(HomeRow(Icons.Outlined.Lock, "Permissions", allowed?.let { "$it/${PermissionFlags.size}" }) { go(ManagePage.Permissions) })
        if (settings != null && telegram.canInvite(chat)) add(HomeRow(Icons.Outlined.Link, "Invite links", null) { go(ManagePage.Links) })
        add(HomeRow(Icons.Outlined.AdminPanelSettings, "Administrators", null) { go(ManagePage.Members(MemberList.Administrators)) })
        add(HomeRow(Icons.Outlined.Group, if (group) "Members" else "Subscribers", telegram.membersCount(chat)?.let { "%,d".format(it) }) { go(ManagePage.Members(MemberList.Recent)) })
        if (supergroup && group && restrict) add(HomeRow(Icons.Outlined.PanTool, "Exceptions", null) { go(ManagePage.Members(MemberList.Restricted)) })
        if (supergroup && restrict) add(HomeRow(Icons.Outlined.Block, "Removed users", null) { go(ManagePage.Members(MemberList.Banned)) })
        if (supergroup) add(HomeRow(Icons.Outlined.History, "Recent actions", null) { go(ManagePage.Log) })
    }
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            rows.forEachIndexed { index, row ->
                SegmentedListItem(
                    onClick = row.action,
                    shapes = groupShapes(index, rows.size),
                    leadingContent = { Icon(row.icon, contentDescription = null) },
                    trailingContent = row.detail?.let { { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
                ) { Text(row.title) }
            }
        }
        if (group && supergroup && (telegram.isOwner(chat) || changeInfo)) {
            var busy by remember { mutableStateOf(false) }
            var error by remember { mutableStateOf<String?>(null) }
            val forum = telegram.isForum(chat)
            SwitchItem(
                Icons.Outlined.Forum,
                "Topics",
                "Split the group into topics, each with its own messages.",
                forum,
                index = 0,
                count = 1,
                enabled = !busy,
            ) { enabled ->
                busy = true
                telegram.setForum(chat, enabled) { failure ->
                    busy = false
                    error = failure
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SwitchItem(
    icon: ImageVector?,
    title: String,
    subtitle: String?,
    checked: Boolean,
    index: Int,
    count: Int,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    SegmentedListItem(
        onClick = { if (enabled) onChange(!checked) },
        shapes = groupShapes(index, count),
        leadingContent = icon?.let { { Icon(it, contentDescription = null) } },
        supportingContent = subtitle?.let { { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
        trailingContent = { PixelSwitch(checked = checked, onCheckedChange = if (enabled) onChange else null) },
    ) { Text(title) }
}

@Composable
private fun InfoPage(chat: ChatState, telegram: Telegram, back: () -> Unit) {
    var title by remember(chat.id) { mutableStateOf(chat.title) }
    var description by remember(chat.id) { mutableStateOf("") }
    var original by remember(chat.id) { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(chat.id) {
        telegram.chatDescription(chat) {
            description = it
            original = it
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it.replace('\n', ' ').take(TitleLimit) },
            label = { Text(if (chat.isChannel) "Channel name" else "Group name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it.take(DescriptionLimit) },
            label = { Text("Description") },
            minLines = 3,
            maxLines = 6,
            modifier = Modifier.fillMaxWidth(),
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            enabled = !busy && title.isNotBlank() && (title.trim() != chat.title || description.trim() != original.trim()),
            onClick = {
                busy = true
                val finish = { failure: String? ->
                    busy = false
                    if (failure == null) back() else error = failure
                }
                val saveDescription = {
                    if (description.trim() != original.trim()) telegram.setChatDescription(chat.id, description, finish) else finish(null)
                }
                if (title.trim() != chat.title) {
                    telegram.setChatTitle(chat.id, title) { failure -> if (failure != null) finish(failure) else saveDescription() }
                } else {
                    saveDescription()
                }
            },
            modifier = Modifier.align(Alignment.End),
        ) { Text("Save") }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PermissionsPage(chat: ChatState, telegram: Telegram, back: () -> Unit) {
    val draft = remember(chat.id) { mutableStateOf((chat.permissions ?: TdApi.ChatPermissions()).copied()) }
    var version by remember { mutableStateOf(0) }
    var slow by remember(chat.id) { mutableStateOf(0) }
    var slowOriginal by remember(chat.id) { mutableStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(chat.id) {
        telegram.slowModeDelay(chat) {
            slow = it
            slowOriginal = it
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text("What can members of this group do?", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        PermissionSwitches(draft.value, version, chat) { version++ }
        if (chat.supergroupId != 0L) {
            Text("Slow mode", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text("Members can send one message per this interval.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SlowModes.forEach { (seconds, label) ->
                    FilterChip(selected = slow == seconds, onClick = { slow = seconds }, label = { Text(label) })
                }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            enabled = !busy,
            onClick = {
                busy = true
                telegram.setChatPermissions(chat.id, draft.value) { failure ->
                    if (failure != null || slow == slowOriginal) {
                        busy = false
                        if (failure == null) back() else error = failure
                    } else {
                        telegram.setSlowModeDelay(chat.id, slow) { slowFailure ->
                            busy = false
                            if (slowFailure == null) back() else error = slowFailure
                        }
                    }
                }
            },
            modifier = Modifier.align(Alignment.End),
        ) { Text("Save") }
    }
}

@Composable
private fun PermissionSwitches(permissions: TdApi.ChatPermissions, version: Int, chat: ChatState, onChanged: () -> Unit) {
    val flags = PermissionFlags.filter { (_, flag) -> flag != TdApi.ChatPermissions::canCreateTopics || chat.supergroupId != 0L }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        flags.forEachIndexed { index, (label, flag) ->
            version
            SwitchItem(null, label, null, flag.get(permissions), index, flags.size) { value ->
                flag.set(permissions, value)
                onChanged()
            }
        }
    }
}

@Composable
private fun MembersPage(chat: ChatState, telegram: Telegram, list: MemberList, open: (TdApi.ChatMember) -> Unit) {
    var query by remember(list) { mutableStateOf("") }
    var members by remember(list) { mutableStateOf<List<TdApi.ChatMember>?>(null) }
    LaunchedEffect(chat.id, list, query) {
        kotlinx.coroutines.delay(if (query.isBlank()) 0 else SearchDelayMs)
        telegram.members(chat, list, query) { members = it }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (list != MemberList.Administrators) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it.replace('\n', ' ') },
                label = { Text("Search") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (list == MemberList.Administrators && telegram.can(chat) { it.canPromoteMembers }) {
            Text(
                "To add an administrator, open Members and pick someone.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val shown = members
        when {
            shown == null -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { LoadingIndicator() }
            shown.isEmpty() -> Text("Nobody here", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> androidx.compose.foundation.lazy.LazyColumn(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.heightIn(max = ListMaxHeight),
            ) {
                items(shown.size) { index -> MemberRow(shown[index], telegram, index, shown.size) { open(shown[index]) } }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MemberRow(member: TdApi.ChatMember, telegram: Telegram, index: Int, count: Int, onClick: () -> Unit) {
    val name = telegram.senderTitle(member.memberId)
    val label = memberLabel(member)
    SegmentedListItem(
        onClick = onClick,
        shapes = groupShapes(index, count),
        leadingContent = { AvatarImage(name, telegram.senderPhoto(member.memberId), telegram, CircleShape, 36.dp) },
        trailingContent = label?.let { { MemberTagPill(it, memberRank(member)) } },
        modifier = Modifier.fillMaxWidth(),
    ) { EmojiText(name, maxLines = 1, overflow = TextOverflow.Ellipsis) }
}

private class MemberAction(val icon: ImageVector, val title: String, val danger: Boolean = false, val run: () -> Unit)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MemberPage(chat: ChatState, telegram: Telegram, member: TdApi.ChatMember, go: (ManagePage) -> Unit, back: () -> Unit, askBan: Boolean = false) {
    val name = telegram.senderTitle(member.memberId)
    val userId = (member.memberId as? TdApi.MessageSenderUser)?.userId ?: 0L
    val self = userId != 0L && userId == telegram.me?.id
    val status = member.status
    val owner = status is TdApi.ChatMemberStatusCreator
    val admin = status is TdApi.ChatMemberStatusAdministrator
    val banned = status is TdApi.ChatMemberStatusBanned
    val restricted = status is TdApi.ChatMemberStatusRestricted
    val group = !chat.isChannel
    val promote = !owner && !self && telegram.can(chat) { it.canPromoteMembers } && (!admin || (status as TdApi.ChatMemberStatusAdministrator).canBeEdited)
    val restrict = !owner && !admin && !self && telegram.can(chat) { it.canRestrictMembers }
    val tagEditable = group && userId != 0L && (telegram.can(chat) { it.canManageTags } || (self && chat.permissions?.canEditTag == true)) && !banned
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var confirmBan by remember { mutableStateOf(askBan) }
    val done = { failure: String? ->
        busy = false
        if (failure == null) back() else error = failure
    }
    val actions = buildList {
        if (promote) add(MemberAction(Icons.Outlined.AdminPanelSettings, if (admin) "Edit admin rights" else "Promote to admin") { go(ManagePage.Rights(member)) })
        if (admin && promote) {
            add(MemberAction(Icons.Outlined.PersonOff, "Dismiss admin", danger = true) {
                busy = true
                telegram.setMemberStatus(chat, member.memberId, TdApi.ChatMemberStatusMember(), done)
            })
        }
        if (restrict && group && !banned) add(MemberAction(Icons.Outlined.Lock, if (restricted) "Edit restrictions" else "Restrict") { go(ManagePage.Restrict(member)) })
        if (restrict && restricted) {
            add(MemberAction(Icons.Outlined.PanTool, "Remove restrictions") {
                busy = true
                telegram.setMemberStatus(chat, member.memberId, TdApi.ChatMemberStatusMember(), done)
            })
        }
        if (restrict && banned) {
            add(MemberAction(Icons.Outlined.PersonAdd, "Unban") {
                busy = true
                telegram.unbanMember(chat, member.memberId, done)
            })
        }
        if (restrict && !banned) {
            add(MemberAction(Icons.Outlined.PersonOff, if (chat.isChannel) "Remove from channel" else "Remove from group", danger = true) {
                busy = true
                telegram.removeMember(chat, member.memberId, done)
            })
            add(MemberAction(Icons.Outlined.Block, "Ban", danger = true) { confirmBan = true })
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AvatarImage(name, telegram.senderPhoto(member.memberId), telegram, CircleShape, 56.dp)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                EmojiText(name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                memberLabel(member)?.let { MemberTagPill(it, memberRank(member)) }
                if (member.joinedChatDate > 0) {
                    Text(
                        tr(if (banned) "Banned" else "Since") + " " + formatEventTime(member.joinedChatDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (tagEditable) TagEditor(chat, telegram, userId, member.tag.orEmpty(), rank = memberRank(member))
        if (actions.isEmpty() && !tagEditable) {
            Text("You can't change anything for this member.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            actions.forEachIndexed { index, action ->
                val tint = if (action.danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                SegmentedListItem(
                    onClick = { if (!busy) action.run() },
                    shapes = groupShapes(index, actions.size),
                    leadingContent = { Icon(action.icon, contentDescription = null, tint = tint) },
                ) { Text(action.title, color = tint) }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
    if (confirmBan) {
        var wipe by remember { mutableStateOf(false) }
        com.pixelgram.desktop.ui.components.expressive.AlertDialog(
            onDismissRequest = { confirmBan = false },
            icon = { Icon(Icons.Outlined.Block, contentDescription = null) },
            title = { Text("Ban $name?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("They won't be able to come back through invite links.")
                    SwitchItem(Icons.Outlined.Delete, "Delete all their messages", null, wipe, 0, 1) { wipe = it }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmBan = false
                    busy = true
                    telegram.banMember(chat, member.memberId, wipe, done)
                }) { Text("Ban", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmBan = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun TagEditor(chat: ChatState, telegram: Telegram, userId: Long, current: String, rank: MemberRank) {
    var tag by remember(userId) { mutableStateOf(current) }
    var saved by remember(userId) { mutableStateOf(current) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = tag,
                onValueChange = { value -> tag = value.filterNot { Character.isSurrogate(it) || it == '\n' }.take(TagLimit) },
                label = { Text(if (rank == MemberRank.Member) "Member tag" else "Admin title") },
                leadingIcon = { Icon(Icons.Outlined.TextFields, contentDescription = null) },
                supportingText = { Text("Up to $TagLimit characters, no emoji. Shown next to the name in messages.") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        if (tag.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Preview", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                MemberTagPill(tag.trim(), rank)
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (tag.trim() != saved.trim()) {
            Button(
                enabled = !busy,
                onClick = {
                    busy = true
                    telegram.setMemberTag(chat, userId, tag) { failure ->
                        busy = false
                        error = failure
                        if (failure == null) saved = tag
                    }
                },
            ) {
                Icon(Icons.Outlined.Edit, contentDescription = null)
                Text("Save tag", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun RightsPage(chat: ChatState, telegram: Telegram, member: TdApi.ChatMember, back: () -> Unit) {
    val existing = (member.status as? TdApi.ChatMemberStatusAdministrator)?.rights
    val draft = remember(member) { mutableStateOf(existing?.copied() ?: defaultRights(chat.isChannel)) }
    var version by remember { mutableStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val mine = telegram.isOwner(chat)
    val myRights = (telegram.myStatus(chat) as? TdApi.ChatMemberStatusAdministrator)?.rights
    val flags = RightFlags.filter { if (chat.isChannel) it.channel else it.group }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text("What can this admin do?", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            flags.forEachIndexed { index, flag ->
                version
                val allowed = mine || myRights?.let { flag.property.get(it) } == true || flag.property == TdApi.ChatAdministratorRights::isAnonymous
                SwitchItem(null, flag.label, null, flag.property.get(draft.value), index, flags.size, enabled = allowed) { value ->
                    flag.property.set(draft.value, value)
                    version++
                }
            }
        }
        if (!chat.isChannel) {
            Text("The title is set in the member card after saving.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            enabled = !busy,
            onClick = {
                busy = true
                val rights = draft.value.copied().apply { canManageChat = true }
                val status = TdApi.ChatMemberStatusAdministrator().apply {
                    canBeEdited = true
                    this.rights = rights
                }
                telegram.setMemberStatus(chat, member.memberId, status) { failure ->
                    busy = false
                    if (failure == null) back() else error = failure
                }
            },
            modifier = Modifier.align(Alignment.End),
        ) { Text(if (existing == null) "Promote" else "Save") }
    }
}

@Composable
private fun RestrictPage(chat: ChatState, telegram: Telegram, member: TdApi.ChatMember, back: () -> Unit) {
    val current = (member.status as? TdApi.ChatMemberStatusRestricted)?.permissions ?: chat.permissions ?: TdApi.ChatPermissions()
    val draft = remember(member) { mutableStateOf(current.copied()) }
    var version by remember { mutableStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text("What can ${telegram.senderTitle(member.memberId)} do?", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        PermissionSwitches(draft.value, version, chat) { version++ }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            enabled = !busy,
            onClick = {
                busy = true
                val status = TdApi.ChatMemberStatusRestricted().apply {
                    isMember = true
                    restrictedUntilDate = 0
                    permissions = draft.value
                }
                telegram.setMemberStatus(chat, member.memberId, status) { failure ->
                    busy = false
                    if (failure == null) back() else error = failure
                }
            },
            modifier = Modifier.align(Alignment.End),
        ) { Text("Save") }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LogPage(chat: ChatState, telegram: Telegram) {
    val events = remember(chat.id) { mutableStateListOf<TdApi.ChatEvent>() }
    var loading by remember(chat.id) { mutableStateOf(true) }
    var finished by remember(chat.id) { mutableStateOf(false) }
    val load = {
        loading = true
        telegram.eventLog(chat.id, events.lastOrNull()?.id ?: 0L) { page ->
            loading = false
            if (page.isEmpty()) finished = true
            events.addAll(page.filter { event -> events.none { it.id == event.id } })
        }
    }
    LaunchedEffect(chat.id) { load() }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Admin actions from the last 48 hours.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (events.isEmpty() && !loading) Text("Nothing happened lately", color = MaterialTheme.colorScheme.onSurfaceVariant)
        androidx.compose.foundation.lazy.LazyColumn(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.heightIn(max = ListMaxHeight),
        ) {
            items(events.size) { index ->
                val event = events[index]
                val actor = telegram.senderTitle(event.memberId)
                SegmentedListItem(
                    onClick = {},
                    shapes = groupShapes(index, events.size),
                    leadingContent = { AvatarImage(actor, telegram.senderPhoto(event.memberId), telegram, CircleShape, 32.dp) },
                    supportingContent = { Text(formatEventTime(event.date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                ) { EmojiText(eventText(event, telegram), maxLines = 4, overflow = TextOverflow.Ellipsis) }
            }
            if (!finished && events.isNotEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                        if (loading) LoadingIndicator() else TextButton(onClick = load) { Text("Load more") }
                    }
                }
            }
        }
        if (loading && events.isEmpty()) Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { LoadingIndicator() }
    }
}

private val EventTime = DateTimeFormatter.ofPattern("d MMM, HH:mm")

private fun formatEventTime(date: Int): String =
    Instant.ofEpochSecond(date.toLong()).atZone(ZoneId.systemDefault()).format(EventTime)

private fun statusName(status: TdApi.ChatMemberStatus?): String = when (status) {
    is TdApi.ChatMemberStatusCreator -> "owner"
    is TdApi.ChatMemberStatusAdministrator -> "admin"
    is TdApi.ChatMemberStatusRestricted -> "restricted"
    is TdApi.ChatMemberStatusBanned -> "banned"
    is TdApi.ChatMemberStatusLeft -> "not a member"
    else -> "member"
}

private fun eventText(event: TdApi.ChatEvent, telegram: Telegram): String {
    val actor = telegram.senderTitle(event.memberId)
    val user = { id: Long -> telegram.userTitle(id) }
    return when (val action = event.action) {
        is TdApi.ChatEventMessageEdited -> "$actor edited a message: ${contentText(action.newMessage?.content).take(EventSnippet)}"
        is TdApi.ChatEventMessageDeleted -> "$actor deleted a message: ${contentText(action.message?.content).take(EventSnippet)}"
        is TdApi.ChatEventMessagePinned -> "$actor pinned a message: ${contentText(action.message?.content).take(EventSnippet)}"
        is TdApi.ChatEventMessageUnpinned -> "$actor unpinned a message"
        is TdApi.ChatEventMemberJoined -> "$actor joined"
        is TdApi.ChatEventMemberJoinedByInviteLink -> "$actor joined via an invite link"
        is TdApi.ChatEventMemberJoinedByRequest -> "$actor was accepted to join"
        is TdApi.ChatEventMemberLeft -> "$actor left"
        is TdApi.ChatEventMemberInvited -> "$actor added ${user(action.userId)}"
        is TdApi.ChatEventMemberPromoted -> "$actor changed ${user(action.userId)}: ${statusName(action.oldStatus)} → ${statusName(action.newStatus)}"
        is TdApi.ChatEventMemberRestricted -> "$actor changed ${telegram.senderTitle(action.memberId)}: ${statusName(action.oldStatus)} → ${statusName(action.newStatus)}"
        is TdApi.ChatEventMemberTagChanged -> when {
            action.oldTag.isNullOrBlank() -> "$actor set the tag of ${user(action.userId)} to «${action.newTag}»"
            action.newTag.isNullOrBlank() -> "$actor removed the tag of ${user(action.userId)}"
            else -> "$actor changed the tag of ${user(action.userId)}: «${action.oldTag}» → «${action.newTag}»"
        }
        is TdApi.ChatEventTitleChanged -> "$actor renamed the chat to «${action.newTitle}»"
        is TdApi.ChatEventDescriptionChanged -> "$actor changed the description"
        is TdApi.ChatEventPhotoChanged -> "$actor changed the photo"
        is TdApi.ChatEventPermissionsChanged -> "$actor changed the member permissions"
        is TdApi.ChatEventSlowModeDelayChanged -> "$actor set slow mode to ${action.newSlowModeDelay}s"
        else -> "$actor: " + action.javaClass.simpleName.removePrefix("ChatEvent")
            .replace(Regex("(?<=[a-z])(?=[A-Z])"), " ").lowercase()
    }
}

private const val EventSnippet = 80
private const val TitleLimit = 128
private const val DescriptionLimit = 255
private const val TagLimit = 16
private const val SearchDelayMs = 300L
private val ManageMaxHeight = 680.dp
private val ListMaxHeight = 460.dp
