package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.telegram.ChatState
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.components.Text
import it.tdlight.jni.TdApi
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatCalendar(chat: ChatState, telegram: Telegram, initialDate: Int, onDismiss: () -> Unit) {
    var selecting by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<Pair<Long, Long>?>(null) }
    val past = remember {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= System.currentTimeMillis()
        }
    }
    val initial = remember(initialDate) {
        val day = Instant.ofEpochSecond(initialDate.toLong()).atZone(ZoneId.systemDefault()).toLocalDate()
        day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }
    val single = rememberDatePickerState(initialSelectedDateMillis = initial, selectableDates = past)
    val range = rememberDateRangePickerState(initialSelectedStartDateMillis = initial, selectableDates = past)
    val canClear = telegram.canDeleteByDate(chat)
    confirm?.let { (from, to) ->
        var revoke by remember { mutableStateOf(false) }
        val days = ((to - from) / DayMillis + 1).toInt()
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("Clear history") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (days == 1) "Delete all messages from this day?" else "${com.pixelgram.desktop.core.tr("Delete all messages from the selected days?")} ($days)")
                    if (chat.privateUserId != 0L && !telegram.isSavedMessages(chat.id)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { revoke = !revoke }.padding(end = 12.dp),
                        ) {
                            Checkbox(checked = revoke, onCheckedChange = { revoke = it })
                            Text("${com.pixelgram.desktop.core.tr("Also delete for")} ${telegram.chatTitle(chat)}")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    telegram.deleteByDate(chat.id, localStart(from), localStart(to) + DaySeconds - 1, revoke)
                    confirm = null
                    onDismiss()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel") } },
        )
    }
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (canClear) {
                    TextButton(onClick = {
                        if (!selecting) {
                            selecting = true
                        } else {
                            val start = range.selectedStartDateMillis
                            if (start != null) confirm = start to (range.selectedEndDateMillis ?: start)
                        }
                    }) { Text(if (selecting) "Clear history" else "Select days", color = if (selecting) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) }
                }
                if (!selecting) {
                    TextButton(onClick = {
                        val picked = single.selectedDateMillis ?: return@TextButton onDismiss()
                        telegram.messageByDate(chat.id, localStart(picked) + DaySeconds - 1) { id ->
                            if (id != null) MessageSearch.jump = chat.id to id
                        }
                        onDismiss()
                    }) { Text("Jump to date") }
                } else {
                    TextButton(onClick = { selecting = false }) { Text("Cancel") }
                }
            }
        },
    ) {
        if (selecting) {
            DateRangePicker(state = range, title = null, headline = null, showModeToggle = false, modifier = Modifier.height(RangeHeight))
        } else {
            DatePicker(state = single, title = null, showModeToggle = false)
        }
    }
}

private fun localStart(utcMillis: Long): Int {
    val day = Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()
    return day.atStartOfDay(ZoneId.systemDefault()).toEpochSecond().toInt()
}

private const val DayMillis = 86_400_000L
private const val DaySeconds = 86_400
private val RangeHeight = 460.dp
