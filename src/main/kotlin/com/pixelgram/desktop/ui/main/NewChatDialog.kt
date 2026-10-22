package com.pixelgram.desktop.ui.main

import com.pixelgram.desktop.ui.theme.EmojiVisualTransformation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.telegram.Telegram

@Composable
fun NewChatDialog(
    telegram: Telegram,
    channel: Boolean,
    onCreated: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val create = {
        creating = true
        error = null
        telegram.createChat(title, description, channel) { chatId, message ->
            creating = false
            if (chatId != null) onCreated(chatId) else error = message ?: "Could not create the chat."
        }
    }
    AlertDialog(
        onDismissRequest = { if (!creating) onDismiss() },
        title = { Text(if (channel) "New Channel" else "New Group") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    visualTransformation = EmojiVisualTransformation,
                    onValueChange = { title = it.take(128) },
                    label = { Text(if (channel) "Channel name" else "Group name") },
                    singleLine = true,
                    enabled = !creating,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    visualTransformation = EmojiVisualTransformation,
                    onValueChange = { description = it.take(255) },
                    label = { Text("Description (optional)") },
                    enabled = !creating,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!channel) {
                    Text(
                        "You can add members from the group after it is created.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = create, enabled = title.isNotBlank() && !creating) {
                if (creating) {
                    LoadingIndicator(Modifier.size(24.dp), color = LocalContentColor.current)
                } else {
                    Text("Create")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !creating) { Text("Cancel") }
        },
    )
}
