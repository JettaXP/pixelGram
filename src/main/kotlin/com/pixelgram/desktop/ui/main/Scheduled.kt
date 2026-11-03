package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.telegram.ChatState
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.telegram.UiMessage
import com.pixelgram.desktop.telegram.contentText
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.outlined.CalendarMonth
import com.pixelgram.desktop.ui.icons.outlined.ContentCopy
import com.pixelgram.desktop.ui.icons.outlined.Delete
import com.pixelgram.desktop.ui.icons.outlined.Edit
import com.pixelgram.desktop.ui.icons.outlined.Send
import com.pixelgram.desktop.ui.theme.EmojiText
import it.tdlight.jni.TdApi
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ScheduledDialog(chat: ChatState, telegram: Telegram, onDismiss: () -> Unit) {
    var list by remember { mutableStateOf<List<UiMessage>?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    LaunchedEffect(reload) { telegram.scheduledMessages(chat.id) { list = it.sortedBy { message -> if (message.scheduledAt < 0) Int.MAX_VALUE else message.scheduledAt } } }
    val refresh = { reload++ }
    var menuFor by remember { mutableStateOf<UiMessage?>(null) }
    var editing by remember { mutableStateOf<UiMessage?>(null) }
    var rescheduling by remember { mutableStateOf<UiMessage?>(null) }
    rescheduling?.let { message ->
        ScheduleDialog(onDismiss = { rescheduling = null }) { date ->
            telegram.reschedule(chat.id, message.id, TdApi.MessageSchedulingStateSendAtDate(date, 0)) { refresh() }
            rescheduling = null
        }
    }
    editing?.let { message ->
        var draft by remember(message.key) { mutableStateOf(message.text) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Edit") },
            text = { OutlinedTextField(draft, { draft = it }, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                TextButton(enabled = draft.isNotBlank(), onClick = {
                    telegram.editScheduledText(chat.id, message.id, draft) { refresh() }
                    editing = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } },
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (telegram.isSavedMessages(chat.id)) "Reminders" else "Scheduled messages") },
        text = {
            val items = list
            when {
                items == null -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { LoadingIndicator() }
                items.isEmpty() -> Text("No scheduled messages")
                else -> LazyColumn(Modifier.heightIn(max = ListHeight), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(items, key = { it.key }) { message ->
                        Box {
                            Surface(
                                onClick = { menuFor = message },
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            ) {
                                Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                                    Text(
                                        if (message.scheduledAt < 0) "When online" else scheduleLabel(message.scheduledAt),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    EmojiText(
                                        message.text.ifBlank { contentText(message.content) },
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            if (menuFor === message) {
                                DropdownMenu(expanded = true, onDismissRequest = { menuFor = null }, shape = RoundedCornerShape(16.dp)) {
                                    val item = @Composable { label: String, icon: ImageVector, action: () -> Unit ->
                                        DropdownMenuItem(
                                            text = { Text(label) },
                                            leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                            onClick = {
                                                menuFor = null
                                                action()
                                            },
                                        )
                                    }
                                    if (message.content is TdApi.MessageText) item("Edit", Icons.Outlined.Edit) { editing = message }
                                    if (message.text.isNotBlank()) {
                                        item("Copy Text", Icons.Outlined.ContentCopy) {
                                            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(message.text), null)
                                        }
                                    }
                                    item("Send Now", Icons.Outlined.Send) { telegram.reschedule(chat.id, message.id, null) { refresh() } }
                                    if (message.scheduledAt >= 0) item("Reschedule", Icons.Outlined.CalendarMonth) { rescheduling = message }
                                    item("Delete", Icons.Outlined.Delete) { telegram.deleteScheduled(chat.id, longArrayOf(message.id)) { refresh() } }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

private fun scheduleLabel(date: Int): String =
    DateTimeFormatter.ofPattern("d MMMM, HH:mm", Locale.forLanguageTag(com.pixelgram.desktop.core.I18n.code))
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochSecond(date.toLong()))

private val ListHeight = 420.dp
