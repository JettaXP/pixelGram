package com.pixelgram.desktop.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.automirrored.outlined.Forward
import com.pixelgram.desktop.ui.icons.filled.Close
import com.pixelgram.desktop.ui.icons.outlined.Delete
import com.pixelgram.desktop.ui.icons.outlined.PersonOff
import com.pixelgram.desktop.ui.icons.outlined.Person
import com.pixelgram.desktop.ui.icons.outlined.SubtitlesOff
import com.pixelgram.desktop.ui.icons.outlined.Subtitles
import com.pixelgram.desktop.ui.icons.outlined.SwapHoriz
import com.pixelgram.desktop.ui.components.expressive.AlertDialog
import androidx.compose.material3.Icon
import com.pixelgram.desktop.ui.components.expressive.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.pixelgram.desktop.ui.components.expressive.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.telegram.UiMessage
import com.pixelgram.desktop.telegram.contentText
import com.pixelgram.desktop.ui.theme.EmojiText
import it.tdlight.jni.TdApi

data class ForwardDraft(
    val fromChatId: Long,
    val messages: List<UiMessage>,
    val hideSender: Boolean = false,
    val hideCaption: Boolean = false,
) {
    val hasCaption: Boolean get() = messages.any { it.text.isNotBlank() && (it.photo != null || it.content !is TdApi.MessageText?) }
}

object PendingForward {
    val drafts = mutableStateMapOf<Long, ForwardDraft>()

    fun start(target: Long, fromChatId: Long, messages: List<UiMessage>) {
        if (messages.isNotEmpty()) drafts[target] = ForwardDraft(fromChatId, messages.sortedBy { it.id })
    }

    fun send(telegram: Telegram, chatId: Long) {
        val draft = drafts.remove(chatId) ?: return
        telegram.forwardMessages(
            chatId,
            draft.fromChatId,
            draft.messages.map { it.id }.toLongArray(),
            sendCopy = draft.hideSender,
            removeCaption = draft.hideSender && draft.hideCaption,
        )
    }
}

private fun senderOf(telegram: Telegram, message: UiMessage): String =
    message.forwardOrigin?.let(telegram::forwardName)?.takeIf { it.isNotBlank() }
        ?: telegram.senderName(message).takeIf { it.isNotBlank() && !message.isOutgoing }
        ?: if (message.isOutgoing) "You" else "Unknown"

private fun previewOf(message: UiMessage, hideCaption: Boolean): String {
    val media = message.photo != null || message.content !is TdApi.MessageText?
    if (!media) return message.text
    val label = if (message.photo != null) "Photo" else contentText(message.content).substringBefore('\n')
    return if (hideCaption || message.text.isBlank()) label else message.text
}

@Composable
fun ForwardBar(chatId: Long, telegram: Telegram, settings: AppSettings, onOpenChat: (Long) -> Unit) {
    var shown by remember { mutableStateOf<ForwardDraft?>(null) }
    val draft = PendingForward.drafts[chatId]
    if (draft != null) shown = draft
    var editing by remember { mutableStateOf(false) }
    AnimatedVisibility(
        visible = draft != null,
        enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeIn(),
        exit = shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeOut(),
    ) {
        val current = shown ?: return@AnimatedVisibility
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 12.dp, top = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable { editing = true }
                .padding(start = 12.dp, top = 4.dp, bottom = 4.dp),
        ) {
            Icon(Icons.AutoMirrored.Outlined.Forward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                val names = current.messages.map { senderOf(telegram, it) }.distinct()
                EmojiText(
                    when {
                        current.hideSender -> if (current.messages.size == 1) "Forward message without sender" else "Forward ${current.messages.size} messages without sender"
                        else -> names.joinToString(", ")
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                EmojiText(
                    if (current.messages.size == 1) previewOf(current.messages.first(), current.hideCaption) else "${current.messages.size} forwarded messages",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = { PendingForward.drafts.remove(chatId) }) {
                Icon(Icons.Filled.Close, contentDescription = "Cancel forwarding")
            }
        }
    }
    if (editing && draft != null) {
        ForwardOptionsDialog(chatId, draft, telegram, settings, onOpenChat) { editing = false }
    }
}

@Composable
private fun ForwardOptionsDialog(
    chatId: Long,
    draft: ForwardDraft,
    telegram: Telegram,
    settings: AppSettings,
    onOpenChat: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var hideSender by remember { mutableStateOf(draft.hideSender) }
    var hideCaption by remember { mutableStateOf(draft.hideCaption) }
    var picking by remember { mutableStateOf(false) }
    val save = {
        PendingForward.drafts[chatId] = draft.copy(hideSender = hideSender, hideCaption = hideSender && hideCaption)
        onDismiss()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (draft.messages.size == 1) "Forward Message" else "Forward ${draft.messages.size} Messages") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.align(Alignment.End).widthIn(max = 320.dp).padding(bottom = 8.dp),
                ) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        AnimatedVisibility(!hideSender) {
                            EmojiText(
                                "Forwarded from " + draft.messages.map { senderOf(telegram, it) }.distinct().joinToString(", "),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        draft.messages.take(PreviewLimit).forEach { message ->
                            EmojiText(
                                previewOf(message, hideSender && hideCaption).ifBlank { "Message" },
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = PreviewLines,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (draft.messages.size > PreviewLimit) {
                            Text("+${draft.messages.size - PreviewLimit} more", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                OptionRow(
                    if (hideSender) Icons.Outlined.Person else Icons.Outlined.PersonOff,
                    if (hideSender) "Show Sender Name" else "Hide Sender Name",
                ) {
                    hideSender = !hideSender
                    if (!hideSender) hideCaption = false
                }
                if (draft.hasCaption) {
                    OptionRow(
                        if (hideCaption && hideSender) Icons.Outlined.Subtitles else Icons.Outlined.SubtitlesOff,
                        if (hideCaption && hideSender) "Show Caption" else "Hide Caption",
                    ) {
                        hideCaption = !(hideCaption && hideSender)
                        if (hideCaption) hideSender = true
                    }
                }
                OptionRow(Icons.Outlined.SwapHoriz, "Change Recipient") { picking = true }
                OptionRow(Icons.Outlined.Delete, "Do Not Forward", danger = true) {
                    PendingForward.drafts.remove(chatId)
                    onDismiss()
                }
                Text(
                    "You can remove the sender's name so that this message will look like it was sent by you.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = { TextButton(onClick = save) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
    if (picking) {
        ForwardDialog(telegram, settings, onDismiss = { picking = false }) { target ->
            picking = false
            PendingForward.drafts.remove(chatId)
            PendingForward.drafts[target] = draft.copy(hideSender = hideSender, hideCaption = hideSender && hideCaption)
            onDismiss()
            if (target != chatId) onOpenChat(target)
        }
    }
}

@Composable
private fun OptionRow(icon: ImageVector, label: String, danger: Boolean = false, onClick: () -> Unit) {
    val color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Text(label, color = color, style = MaterialTheme.typography.bodyLarge)
    }
}

private const val PreviewLimit = 3
private const val PreviewLines = 6
