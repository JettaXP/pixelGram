package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.telegram.ChatState
import com.pixelgram.desktop.telegram.MemberRank
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.telegram.can
import com.pixelgram.desktop.telegram.setMemberStatus
import com.pixelgram.desktop.ui.components.AvatarImage
import com.pixelgram.desktop.ui.components.GroupedMenu
import com.pixelgram.desktop.ui.components.MenuEntry
import com.pixelgram.desktop.ui.components.MenuGroups
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.components.groupShapes
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.outlined.AccountCircle
import com.pixelgram.desktop.ui.icons.outlined.AdminPanelSettings
import com.pixelgram.desktop.ui.icons.outlined.Block
import com.pixelgram.desktop.ui.icons.outlined.Group
import com.pixelgram.desktop.ui.icons.outlined.Lock
import com.pixelgram.desktop.ui.icons.outlined.PersonOff
import com.pixelgram.desktop.ui.theme.EmojiText
import it.tdlight.jni.TdApi

private class MemberRow(val title: String, val icon: ImageVector, val count: Int?, val admins: Boolean)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ChatMemberRows(
    chat: ChatState,
    telegram: Telegram,
    settings: AppSettings,
    onOpenChat: ((Long) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val group = !chat.isChannel && (chat.basicGroupId != 0L || chat.supergroupId != 0L)
    val adminChannel = chat.isChannel && telegram.isAdmin(chat)
    if (!group && !adminChannel) return
    var admins by remember(chat.id) { mutableIntStateOf(0) }
    LaunchedEffect(chat.id, adminChannel) { if (adminChannel) telegram.administratorCount(chat) { admins = it } }
    val rows = if (group) {
        listOf(MemberRow("Members", Icons.Outlined.Group, telegram.membersCount(chat), admins = false))
    } else {
        listOf(
            MemberRow("Subscribers", Icons.Outlined.Group, telegram.membersCount(chat), admins = false),
            MemberRow("Administrators", Icons.Outlined.AdminPanelSettings, admins, admins = true),
        )
    }
    var open by remember(chat.id) { mutableStateOf<MemberRow?>(null) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        rows.forEachIndexed { index, row ->
            SegmentedListItem(
                onClick = { open = row },
                shapes = groupShapes(index, rows.size),
                leadingContent = { Icon(row.icon, contentDescription = null) },
                trailingContent = { row.count?.let { Text("%,d".format(it), color = MaterialTheme.colorScheme.onSurfaceVariant) } },
            ) { Text(row.title) }
        }
    }
    open?.let { row -> MembersDialog(chat, telegram, settings, onOpenChat, row.title, row.admins) { open = null } }
}

private class ManageRequest(val member: TdApi.ChatMember, val start: ManageStart)

@Composable
private fun MembersDialog(
    chat: ChatState,
    telegram: Telegram,
    settings: AppSettings,
    onOpenChat: ((Long) -> Unit)?,
    title: String,
    admins: Boolean,
    onDismiss: () -> Unit,
) {
    var members by remember(chat.id, admins) { mutableStateOf<List<TdApi.ChatMember>?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var profile by remember { mutableStateOf<ChatState?>(null) }
    var managing by remember { mutableStateOf<ManageRequest?>(null) }
    LaunchedEffect(chat.id, admins, reload) { telegram.chatMembers(chat, admins) { members = it } }
    val openProfile = { sender: TdApi.MessageSender ->
        when (sender) {
            is TdApi.MessageSenderUser -> telegram.openPrivateChat(sender.userId) { id -> telegram.chats[id]?.let { profile = it } }
            is TdApi.MessageSenderChat -> telegram.chats[sender.chatId]?.let { profile = it }
        }
    }
    GridDialog(title, onDismiss = onDismiss) {
        val list = members
        when {
            list == null -> LoadingIndicator()
            list.isEmpty() -> Text("Telegram doesn't share this list", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                list.forEachIndexed { index, member ->
                    MemberItem(
                        chat,
                        member,
                        telegram,
                        index,
                        list.size,
                        onProfile = { openProfile(member.memberId) },
                        onManage = { start -> managing = ManageRequest(member, start) },
                        onChanged = { reload++ },
                    )
                }
            }
        }
    }
    profile?.let { shown -> ChatInfoDialog(shown, telegram, settings, onOpenChat) { profile = null } }
    managing?.let { request ->
        ManageChatDialog(chat, telegram, member = request.member, start = request.start) {
            managing = null
            reload++
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MemberItem(
    chat: ChatState,
    member: TdApi.ChatMember,
    telegram: Telegram,
    index: Int,
    count: Int,
    onProfile: () -> Unit,
    onManage: (ManageStart) -> Unit,
    onChanged: () -> Unit,
) {
    val name = telegram.senderTitle(member.memberId)
    val status = member.status
    val owner = status is TdApi.ChatMemberStatusCreator
    val admin = status is TdApi.ChatMemberStatusAdministrator
    val role = when {
        owner -> member.tag.orEmpty().ifBlank { com.pixelgram.desktop.core.tr("owner") }
        admin -> member.tag.orEmpty().ifBlank { com.pixelgram.desktop.core.tr("admin") }
        else -> member.tag.orEmpty().takeIf { it.isNotBlank() }
    }
    val userId = (member.memberId as? TdApi.MessageSenderUser)?.userId ?: 0L
    val self = userId != 0L && userId == telegram.me?.id
    val promote = !owner && !self && userId != 0L && telegram.can(chat) { it.canPromoteMembers } &&
        (!admin || (status as TdApi.ChatMemberStatusAdministrator).canBeEdited)
    val restrict = !owner && !admin && !self && telegram.can(chat) { it.canRestrictMembers }
    var menuAt by remember { mutableStateOf<DpOffset?>(null) }
    val density = LocalDensity.current
    Box {
        SegmentedListItem(
            onClick = onProfile,
            shapes = groupShapes(index, count),
            leadingContent = { AvatarImage(name, telegram.senderPhoto(member.memberId), telegram, CircleShape, 36.dp) },
            trailingContent = role?.let { { MemberTagPill(it, if (owner) MemberRank.Owner else if (admin) MemberRank.Admin else MemberRank.Member) } },
            modifier = Modifier.fillMaxWidth().pointerInput(member.memberId) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                            val change = event.changes.first()
                            change.consume()
                            menuAt = with(density) { DpOffset(change.position.x.toDp(), change.position.y.toDp()) }
                        }
                    }
                }
            },
        ) { EmojiText(name, maxLines = 1) }
        menuAt?.let { at ->
            val close = { menuAt = null }
            GroupedMenu(expanded = true, onDismiss = close, offset = at) {
                val danger = MaterialTheme.colorScheme.error
                MenuGroups(
                    listOf(
                        listOf(MenuEntry(Icons.Outlined.AccountCircle, "View profile") { close(); onProfile() }),
                        buildList {
                            if (promote) {
                                add(MenuEntry(Icons.Outlined.AdminPanelSettings, if (admin) "Edit admin rights" else "Promote to admin") {
                                    close()
                                    onManage(ManageStart.Rights)
                                })
                            }
                            if (promote && admin) {
                                add(MenuEntry(Icons.Outlined.PersonOff, "Dismiss admin", color = danger) {
                                    close()
                                    telegram.setMemberStatus(chat, member.memberId, TdApi.ChatMemberStatusMember()) { error ->
                                        if (error != null) TopNotice.text = error else onChanged()
                                    }
                                })
                            }
                            if (restrict && !chat.isChannel) {
                                add(MenuEntry(Icons.Outlined.Lock, if (status is TdApi.ChatMemberStatusRestricted) "Edit restrictions" else "Restrict") {
                                    close()
                                    onManage(ManageStart.Restrict)
                                })
                            }
                            if (restrict) {
                                add(MenuEntry(Icons.Outlined.Block, "Ban", color = danger) {
                                    close()
                                    onManage(ManageStart.Ban)
                                })
                            }
                        },
                    ),
                )
            }
        }
    }
}
