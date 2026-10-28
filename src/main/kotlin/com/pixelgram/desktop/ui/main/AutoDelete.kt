package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.components.expressive.AlertDialog
import com.pixelgram.desktop.ui.components.expressive.TextButton

private const val Day = 86_400
private val ChoicesMaxHeight = 420.dp

val AutoDeleteChoices = listOf(0, Day, 2 * Day, 3 * Day, 7 * Day, 14 * Day, 31 * Day, 92 * Day, 183 * Day, 365 * Day)

val SecretAutoDeleteChoices = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 30, 60, 3600, Day, 7 * Day)

fun autoDeleteLabel(seconds: Int): String = when {
    seconds <= 0 -> "Off"
    seconds < 60 -> "{n} sec".replace("{n}", seconds.toString())
    seconds < 3600 -> "{n} min".replace("{n}", (seconds / 60).toString())
    seconds < Day -> "{n} h".replace("{n}", (seconds / 3600).toString())
    seconds == 7 * Day -> "1 week"
    seconds == 14 * Day -> "2 weeks"
    seconds == 31 * Day -> "1 month"
    seconds == 92 * Day -> "3 months"
    seconds == 183 * Day -> "6 months"
    seconds == 365 * Day -> "1 year"
    else -> "{n} days".replace("{n}", (seconds / Day).toString())
}

@Composable
fun AutoDeleteDialog(
    title: String,
    description: String,
    current: Int,
    choices: List<Int> = AutoDeleteChoices,
    onDismiss: () -> Unit,
    onSave: (Int, (String?) -> Unit) -> Unit,
) {
    var picked by remember { mutableStateOf(if (current in choices) current else choices.firstOrNull { it >= current } ?: current) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.heightIn(max = ChoicesMaxHeight).verticalScroll(rememberScrollState())) {
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
                choices.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { picked = option },
                    ) {
                        RadioButton(selected = picked == option, onClick = { picked = option })
                        Text(autoDeleteLabel(option))
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = {
                busy = true
                onSave(picked) { failure ->
                    busy = false
                    if (failure == null) onDismiss() else error = failure
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
