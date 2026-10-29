package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.telegram.ReportStep
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.telegram.UiMessage
import com.pixelgram.desktop.telegram.report
import com.pixelgram.desktop.telegram.translateMessage
import com.pixelgram.desktop.telegram.translationLanguage
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.components.expressive.AlertDialog
import com.pixelgram.desktop.ui.components.expressive.TextButton
import com.pixelgram.desktop.ui.components.groupShapes
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.outlined.Translate
import com.pixelgram.desktop.ui.theme.EmojiText
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@Composable
fun TranslateDialog(telegram: Telegram, message: UiMessage, appLanguage: String, onDismiss: () -> Unit) {
    var text by remember(message.key) { mutableStateOf<String?>(null) }
    var error by remember(message.key) { mutableStateOf<String?>(null) }
    LaunchedEffect(message.key) {
        telegram.translateMessage(message, translationLanguage(appLanguage)) { result, failure ->
            text = result
            error = failure ?: if (result == null) "Translation failed" else null
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { androidx.compose.material3.Icon(Icons.Outlined.Translate, contentDescription = null) },
        title = { Text("Translation") },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                when {
                    error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
                    text == null -> LoadingIndicator(Modifier.padding(16.dp))
                    else -> SelectionContainer { EmojiText(text!!, style = MaterialTheme.typography.bodyLarge) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                text?.let { Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(it), null) }
                onDismiss()
            }, enabled = text != null) { Text("Copy") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
fun ReportDialog(telegram: Telegram, chatId: Long, messageIds: LongArray, onDismiss: () -> Unit) {
    var step by remember { mutableStateOf<ReportStep?>(null) }
    var comment by remember { mutableStateOf("") }
    val run = { optionId: ByteArray, text: String ->
        step = null
        telegram.report(chatId, optionId, messageIds, text) { step = it }
    }
    LaunchedEffect(chatId) { run(ByteArray(0), "") }
    val current = step
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (current is ReportStep.Options) current.title else "Report") },
        text = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                when (current) {
                    null -> LoadingIndicator(Modifier.padding(16.dp))
                    is ReportStep.Options -> current.options.forEachIndexed { index, (id, label) ->
                        SegmentedListItem(onClick = { run(id, "") }, shapes = groupShapes(index, current.options.size)) { EmojiText(label) }
                    }
                    is ReportStep.Text -> OutlinedTextField(
                        comment,
                        { comment = it },
                        label = { Text(if (current.optional) "Comment (optional)" else "Describe the problem") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ReportStep.NeedsMessages -> Text("Select the messages you want to report, then choose Report again.")
                    ReportStep.Done -> Text("Thank you! Your report will be reviewed by Telegram moderators.")
                    is ReportStep.Failed -> Text(current.message, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            if (current is ReportStep.Text) {
                TextButton(onClick = { run(current.optionId, comment.trim()) }, enabled = current.optional || comment.isNotBlank()) { Text("Send") }
            } else {
                TextButton(onClick = onDismiss) { Text(if (current == ReportStep.Done) "Done" else "Close") }
            }
        },
        dismissButton = if (current is ReportStep.Text) ({ TextButton(onClick = onDismiss) { Text("Cancel") } }) else null,
    )
}
