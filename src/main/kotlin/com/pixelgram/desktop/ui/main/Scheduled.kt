package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.input.pointer.pointerInput
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
import com.pixelgram.desktop.ui.OnEscape
import androidx.compose.ui.input.pointer.isSecondaryPressed
import com.pixelgram.desktop.ui.icons.automirrored.filled.ArrowBack
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

private sealed interface ScheduledRow {
    val key: String

    class Day(val label: String) : ScheduledRow {
        override val key = "day:$label"
    }

    class Item(val message: UiMessage) : ScheduledRow {
        override val key = "msg:${message.key}"
    }
}

private fun scheduledRows(list: List<UiMessage>): List<ScheduledRow> {
    val zone = ZoneId.systemDefault()
    val dated = list.filter { it.scheduledAt >= 0 }.sortedBy { it.scheduledAt }
    val online = list.filter { it.scheduledAt < 0 }
    val rows = ArrayList<ScheduledRow>()
    dated.groupBy { Instant.ofEpochSecond(it.scheduledAt.toLong()).atZone(zone).toLocalDate() }.forEach { (day, items) ->
        rows += ScheduledRow.Day(dayLabel(day))
        items.forEach { rows += ScheduledRow.Item(it) }
    }
    if (online.isNotEmpty()) {
        rows += ScheduledRow.Day(com.pixelgram.desktop.core.tr("Scheduled until online"))
        online.forEach { rows += ScheduledRow.Item(it) }
    }
    return rows
}

private fun dayLabel(day: java.time.LocalDate): String {
    val today = java.time.LocalDate.now()
    val tr = { key: String -> com.pixelgram.desktop.core.tr(key) }
    return when (day) {
        today -> tr("Scheduled for today")
        today.plusDays(1) -> tr("Scheduled for tomorrow")
        else -> tr("Scheduled for") + " " + DateTimeFormatter.ofPattern(if (day.year == today.year) "d MMMM" else "d MMMM yyyy", Locale.forLanguageTag(com.pixelgram.desktop.core.I18n.code)).format(day)
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ScheduledPane(chat: ChatState, telegram: Telegram, settings: com.pixelgram.desktop.core.AppSettings, modifier: Modifier, onBack: () -> Unit) {
    var list by remember(chat.id) { mutableStateOf<List<UiMessage>?>(null) }
    LaunchedEffect(chat.id, telegram.scheduledVersion) {
        telegram.scheduledMessages(chat.id) { found -> list = found }
    }
    LaunchedEffect(list, chat.hasScheduled) {
        if (list?.isEmpty() == true && !chat.hasScheduled) onBack()
    }
    OnEscape {
        onBack()
        true
    }
    val refresh = { telegram.scheduledMessages(chat.id) { found -> list = found } }
    var editing by remember { mutableStateOf<UiMessage?>(null) }
    var rescheduling by remember { mutableStateOf<UiMessage?>(null) }
    rescheduling?.let { message ->
        ScheduleDialog(onDismiss = { rescheduling = null }, initial = message.scheduledAt.coerceAtLeast(0)) { date ->
            telegram.reschedule(chat.id, message.id, TdApi.MessageSchedulingStateSendAtDate(date, 0)) { refresh() }
            rescheduling = null
        }
    }
    editing?.let { message ->
        var draft by remember(message.key) { mutableStateOf(message.text) }
        com.pixelgram.desktop.ui.components.expressive.AlertDialog(
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
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)) {
            com.pixelgram.desktop.ui.components.expressive.IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                if (telegram.isSavedMessages(chat.id)) "Reminders" else "Scheduled Messages",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        val items = list
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (items == null) {
                LoadingIndicator(Modifier.align(Alignment.Center))
            } else {
                val rows = remember(items) { scheduledRows(items).asReversed() }
                LazyColumn(
                    reverseLayout = true,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(rows, key = { it.key }) { row ->
                        when (row) {
                            is ScheduledRow.Day -> Box(Modifier.fillMaxWidth().padding(vertical = 6.dp).animateItem(), contentAlignment = Alignment.Center) {
                                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                                    Text(row.label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp))
                                }
                            }
                            is ScheduledRow.Item -> ScheduledBubble(
                                row.message,
                                telegram,
                                settings,
                                Modifier.animateItem(),
                                onEdit = { editing = row.message },
                                onSendNow = { telegram.reschedule(chat.id, row.message.id, null) { refresh() } },
                                onReschedule = { rescheduling = row.message },
                                onDelete = { telegram.deleteScheduled(chat.id, longArrayOf(row.message.id)) { refresh() } },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduledBubble(
    message: UiMessage,
    telegram: Telegram,
    settings: com.pixelgram.desktop.core.AppSettings,
    modifier: Modifier,
    onEdit: () -> Unit,
    onSendNow: () -> Unit,
    onReschedule: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Box(
            Modifier.pointerInput(message.key) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                        if (event.type == androidx.compose.ui.input.pointer.PointerEventType.Press && event.buttons.isSecondaryPressed) {
                            event.changes.first().consume()
                            menu = true
                        }
                    }
                }
            },
        ) {
            MessageBubble(
                message = message.copy(date = message.scheduledAt.coerceAtLeast(0), isOutgoing = true),
                senderName = null,
                settings = settings,
                telegram = telegram,
            )
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, shape = RoundedCornerShape(16.dp)) {
                val item = @Composable { label: String, icon: ImageVector, action: () -> Unit ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        onClick = {
                            menu = false
                            action()
                        },
                    )
                }
                item("Send Now", Icons.Outlined.Send, onSendNow)
                if (message.scheduledAt >= 0) item("Reschedule", Icons.Outlined.CalendarMonth, onReschedule)
                if (message.content is TdApi.MessageText) item("Edit", Icons.Outlined.Edit, onEdit)
                if (message.text.isNotBlank()) {
                    item("Copy Text", Icons.Outlined.ContentCopy) {
                        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(message.text), null)
                    }
                }
                item("Delete", Icons.Outlined.Delete, onDelete)
            }
        }
    }
}

