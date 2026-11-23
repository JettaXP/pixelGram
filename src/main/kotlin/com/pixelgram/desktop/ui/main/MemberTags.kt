package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.tr
import com.pixelgram.desktop.telegram.ChatState
import com.pixelgram.desktop.telegram.MemberRank
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.telegram.setMemberTag
import com.pixelgram.desktop.telegram.can
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.components.expressive.AlertDialog
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.outlined.Sell

class TagInfo(val chat: ChatState, val name: String, val tag: String, val rank: MemberRank)

object TagDialogs {
    var info by mutableStateOf<TagInfo?>(null)
    var edit by mutableStateOf<ChatState?>(null)
}

fun Telegram.canEditOwnTag(chat: ChatState): Boolean =
    !chat.isChannel && (can(chat) { it.canManageTags } || chat.permissions?.canEditTag == true)

@Composable
fun TagDialogHost(telegram: Telegram) {
    TagDialogs.info?.let { info -> TagInfoDialog(info, telegram) }
    TagDialogs.edit?.let { chat -> EditOwnTagDialog(chat, telegram) }
}

@Composable
private fun TagInfoDialog(info: TagInfo, telegram: Telegram) {
    val color = tagColor(info.rank)
    val close = { TagDialogs.info = null }
    val title = when (info.rank) {
        MemberRank.Owner -> "Owner Tags"
        MemberRank.Admin -> "Admin Tags"
        MemberRank.Member -> "Member Tags"
    }
    val kind = when (info.rank) {
        MemberRank.Owner -> tr("purple")
        MemberRank.Admin -> tr("green")
        MemberRank.Member -> tr("gray")
    }
    val role = when (info.rank) {
        MemberRank.Owner -> tr("owner tag")
        MemberRank.Admin -> tr("admin tag")
        MemberRank.Member -> tr("member tag")
    }
    val editable = telegram.canEditOwnTag(info.chat)
    AlertDialog(
        onDismissRequest = close,
        icon = {
            Box(Modifier.size(72.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Sell, contentDescription = null, tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(36.dp))
            }
        },
        title = { Text(title) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                androidx.compose.material3.Text(
                    buildAnnotatedString {
                        append(tr("This %s tag").format(kind))
                        append(" ")
                        withStyle(SpanStyle(color = color, background = if (info.rank == MemberRank.Member) Color.Transparent else color.copy(alpha = TagFill))) { append(" ${info.tag} ") }
                        append(" ")
                        append(tr("is %1\$s's %2\$s in %3\$s.").format(info.name, role, telegram.chatTitle(info.chat)))
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TagPreviewCard("Member Tag", MemberRank.Member, Modifier.weight(1f))
                    TagPreviewCard(if (info.rank == MemberRank.Owner) "Owner Tag" else "Admin Tag", if (info.rank == MemberRank.Owner) MemberRank.Owner else MemberRank.Admin, Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            if (editable) {
                androidx.compose.material3.Button(onClick = {
                    TagDialogs.info = null
                    TagDialogs.edit = info.chat
                }, modifier = Modifier.fillMaxWidth()) { Text("Edit My Tag") }
            } else {
                TextButton(onClick = close) { Text("OK") }
            }
        },
    )
}

@Composable
private fun TagPreviewCard(label: String, rank: MemberRank, modifier: Modifier) {
    val line = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f)
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(12.dp),
    ) {
        MemberTagPill(tr(label), rank)
        Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(line))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).height(6.dp).clip(CircleShape).background(line))
            Spacer(Modifier.size(24.dp, 0.dp))
            Text("12:00", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EditOwnTagDialog(chat: ChatState, telegram: Telegram) {
    val me = telegram.me ?: return
    val current = telegram.ownTag(chat)
    var value by remember(chat.id) { mutableStateOf(current?.text.orEmpty()) }
    var error by remember(chat.id) { mutableStateOf<String?>(null) }
    var busy by remember(chat.id) { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(chat.id) { telegram.ownMemberTag(chat.id) { tag -> if (value.isBlank()) value = tag } }
    val rank = current?.rank ?: MemberRank.Member
    val close = { TagDialogs.edit = null }
    AlertDialog(
        onDismissRequest = close,
        title = { Text("Edit tag") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        com.pixelgram.desktop.ui.theme.EmojiText(
                            telegram.userName(me.id),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (value.isNotBlank()) MemberTagPill(value.trim(), rank)
                    }
                    val line = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f)
                    Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(line))
                    Box(Modifier.fillMaxWidth(0.7f).height(6.dp).clip(CircleShape).background(line))
                }
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it.take(MaxTagLength) },
                    singleLine = true,
                    isError = error != null,
                    shape = RoundedCornerShape(16.dp),
                    supportingText = {
                        Text(error ?: "Share your role, title or how you're known in this group. Your tag is visible to all members.")
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        dismissButton = { TextButton(onClick = close) { Text("Cancel") } },
        confirmButton = {
            TextButton(enabled = !busy, onClick = {
                busy = true
                telegram.setMemberTag(chat, me.id, value) { failure ->
                    busy = false
                    if (failure == null) close() else error = failure
                }
            }) { Text("Save") }
        },
    )
}

private const val TagFill = 0.16f
private const val MaxTagLength = 16
