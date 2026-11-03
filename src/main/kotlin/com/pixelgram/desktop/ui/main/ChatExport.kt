package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AppLog
import com.pixelgram.desktop.core.Downloads
import com.pixelgram.desktop.core.SystemOpen
import com.pixelgram.desktop.telegram.ChatState
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.telegram.contentText
import com.pixelgram.desktop.ui.components.Text
import it.tdlight.jni.TdApi
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

private enum class ExportFormat(val label: String, val extension: String) { Html("HTML", "html"), Json("JSON", "json") }

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExportChatDialog(chat: ChatState, telegram: Telegram, onDismiss: () -> Unit) {
    var format by remember { mutableStateOf(ExportFormat.Html) }
    var busy by remember { mutableStateOf(false) }
    var count by remember { mutableIntStateOf(0) }
    var result by remember { mutableStateOf<File?>(null) }
    var failed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Export chat history") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    result != null -> Text("${com.pixelgram.desktop.core.tr("Saved")}: ${result?.absolutePath}")
                    failed -> Text("Export failed")
                    busy -> {
                        LinearWavyProgressIndicator(Modifier.fillMaxWidth())
                        Text("${com.pixelgram.desktop.core.tr("Messages")}: $count")
                    }
                    else -> {
                        Text("Text messages are exported without media files.", style = MaterialTheme.typography.bodyMedium)
                        ExportFormat.entries.forEach { option ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { format = option }.padding(end = 12.dp),
                            ) {
                                RadioButton(selected = format == option, onClick = { format = option })
                                Text(option.label)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            when {
                result != null -> TextButton(onClick = {
                    result?.parentFile?.let(SystemOpen::file)
                    onDismiss()
                }) { Text("Open folder") }
                busy -> Unit
                else -> TextButton(onClick = {
                    busy = true
                    failed = false
                    scope.launch {
                        val messages = collectHistory(telegram, chat.id) { count = it }
                        val file = runCatching {
                            withContext(Dispatchers.IO) { writeExport(telegram, chat, messages.asReversed(), format) }
                        }.onFailure { AppLog.write("Chat export failed", it) }.getOrNull()
                        busy = false
                        if (file != null) result = file else failed = true
                    }
                }) { Text("Export") }
            }
        },
        dismissButton = {
            if (result == null && !busy) TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private suspend fun collectHistory(telegram: Telegram, chatId: Long, onProgress: (Int) -> Unit): List<TdApi.Message> {
    val all = ArrayList<TdApi.Message>()
    var from = 0L
    repeat(ExportPagesMax) {
        val page = suspendCancellableCoroutine { continuation ->
            telegram.send(TdApi.GetChatHistory(chatId, from, 0, ExportPage, false)) { result ->
                if (continuation.isActive) continuation.resume((result as? TdApi.Messages)?.messages?.filterNotNull().orEmpty())
            }
        }
        val fresh = page.filter { message -> all.isEmpty() || message.id < all.last().id }
        if (fresh.isEmpty()) return all
        all.addAll(fresh)
        from = fresh.last().id
        onProgress(all.size)
    }
    return all
}

private fun writeExport(telegram: Telegram, chat: ChatState, messages: List<TdApi.Message>, format: ExportFormat): File {
    val title = telegram.chatTitle(chat)
    val stamp = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm").withZone(ZoneId.systemDefault()).format(Instant.now())
    val safe = title.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().ifBlank { "chat" }
    Downloads.dir.mkdirs()
    val file = File(Downloads.dir, "ChatExport_${safe}_$stamp.${format.extension}")
    val dates = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss").withZone(ZoneId.systemDefault())
    fun text(message: TdApi.Message): String = when (val content = message.content) {
        is TdApi.MessageText -> content.text.text
        else -> contentText(content)
    }
    fun from(message: TdApi.Message) = telegram.senderTitle(message.senderId)
    val body = when (format) {
        ExportFormat.Json -> buildString {
            append("{\n  \"name\": ").append(json(title)).append(",\n  \"id\": ").append(chat.id).append(",\n  \"messages\": [\n")
            messages.forEachIndexed { index, message ->
                append("    {\"id\": ").append(message.id)
                append(", \"date\": ").append(json(dates.format(Instant.ofEpochSecond(message.date.toLong()))))
                append(", \"date_unixtime\": ").append(message.date)
                append(", \"from\": ").append(json(from(message)))
                append(", \"text\": ").append(json(text(message))).append('}')
                append(if (index < messages.lastIndex) ",\n" else "\n")
            }
            append("  ]\n}\n")
        }
        ExportFormat.Html -> buildString {
            append("<!doctype html><html><head><meta charset=\"utf-8\"><title>").append(html(title)).append("</title><style>")
            append("body{font-family:system-ui,sans-serif;background:#141218;color:#e6e0e9;margin:0;padding:24px}")
            append(".m{max-width:720px;margin:0 auto 8px;padding:10px 14px;border-radius:16px;background:#211f26}")
            append(".f{font-weight:600;color:#d0bcff}.d{float:right;font-size:12px;color:#948f99}.t{white-space:pre-wrap;margin-top:4px}")
            append("h1{max-width:720px;margin:0 auto 16px}</style></head><body><h1>").append(html(title)).append("</h1>")
            messages.forEach { message ->
                append("<div class=\"m\"><span class=\"d\">").append(dates.format(Instant.ofEpochSecond(message.date.toLong())))
                append("</span><div class=\"f\">").append(html(from(message))).append("</div><div class=\"t\">")
                append(html(text(message))).append("</div></div>")
            }
            append("</body></html>\n")
        }
    }
    file.writeText(body)
    return file
}

private fun json(value: String): String = buildString {
    append('"')
    value.forEach { c ->
        when {
            c == '"' -> append("\\\"")
            c == '\\' -> append("\\\\")
            c == '\n' -> append("\\n")
            c == '\r' -> append("\\r")
            c == '\t' -> append("\\t")
            c < ' ' -> append("\\u%04x".format(c.code))
            else -> append(c)
        }
    }
    append('"')
}

private fun html(value: String): String = value
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")

private const val ExportPage = 100
private const val ExportPagesMax = 2_000
