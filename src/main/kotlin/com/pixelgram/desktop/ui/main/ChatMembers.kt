package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.telegram.ChatState
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.components.AvatarImage
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.components.groupShapes
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.outlined.AdminPanelSettings
import com.pixelgram.desktop.ui.icons.outlined.Group
import com.pixelgram.desktop.ui.theme.EmojiText
import it.tdlight.jni.TdApi

private class MemberRow(val title: String, val icon: ImageVector, val count: Int?, val admins: Boolean)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ChatMemberRows(chat: ChatState, telegram: Telegram, modifier: Modifier = Modifier) {
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
    open?.let { row -> MembersDialog(chat, telegram, row.title, row.admins) { open = null } }
}

@Composable
private fun MembersDialog(chat: ChatState, telegram: Telegram, title: String, admins: Boolean, onDismiss: () -> Unit) {
    var members by remember(chat.id, admins) { mutableStateOf<List<TdApi.ChatMember>?>(null) }
    LaunchedEffect(chat.id, admins) { telegram.chatMembers(chat, admins) { members = it } }
    GridDialog(title, onDismiss = onDismiss) {
        val list = members
        when {
            list == null -> LoadingIndicator()
            list.isEmpty() -> Text("Telegram doesn't share this list", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                list.forEachIndexed { index, member -> MemberItem(member, telegram, index, list.size) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MemberItem(member: TdApi.ChatMember, telegram: Telegram, index: Int, count: Int) {
    val name = telegram.senderTitle(member.memberId)
    val role = when (member.status) {
        is TdApi.ChatMemberStatusCreator -> member.tag.orEmpty().ifBlank { "Owner" }
        is TdApi.ChatMemberStatusAdministrator -> member.tag.orEmpty().ifBlank { "Admin" }
        else -> null
    }
    SegmentedListItem(
        onClick = {},
        shapes = groupShapes(index, count),
        leadingContent = { AvatarImage(name, telegram.senderPhoto(member.memberId), telegram, CircleShape, 36.dp) },
        trailingContent = role?.let { { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) } },
        modifier = Modifier.fillMaxWidth(),
    ) { EmojiText(name, maxLines = 1) }
}
