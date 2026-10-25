package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.filled.Extension
import com.pixelgram.desktop.ui.icons.outlined.FolderOpen
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.theme.EmojiText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class PluginInfo(
    val id: String,
    val name: String,
    val description: String,
    val author: String,
    val version: String,
    val icon: String,
)

private val PluginField = Regex("""^__(\w+)__\s*=\s*(?:"((?:[^"\\]|\\.)*)"|'((?:[^'\\]|\\.)*)')""", RegexOption.MULTILINE)

fun parsePlugin(path: String): PluginInfo {
    val text = runCatching { File(path).readText() }.getOrDefault("")
    val fields = PluginField.findAll(text).associate { match ->
        match.groupValues[1] to (match.groupValues[2].ifEmpty { match.groupValues[3] })
            .replace("\\n", "\n").replace("\\\"", "\"").replace("\\'", "'")
    }
    return PluginInfo(
        id = fields["id"].orEmpty(),
        name = fields["name"].orEmpty().ifBlank { File(path).nameWithoutExtension },
        description = fields["description"].orEmpty(),
        author = fields["author"].orEmpty(),
        version = fields["version"].orEmpty(),
        icon = fields["icon"].orEmpty(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginDialog(path: String, telegram: Telegram, onDismiss: () -> Unit) {
    val info = produceState<PluginInfo?>(null, path) { value = withContext(Dispatchers.IO) { parsePlugin(path) } }.value
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.widthIn(min = 320.dp, max = 420.dp),
        ) {
            Box {
                IconButton(
                    onClick = { File(path).parentFile?.let { openFile(it.absolutePath) } },
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                ) {
                    Icon(Icons.Outlined.FolderOpen, contentDescription = "Show in folder")
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 20.dp),
                ) {
                    PluginIcon(info?.icon.orEmpty(), telegram)
                    EmojiText(info?.name.orEmpty(), style = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center))
                    val line = listOf(info?.version?.takeIf { it.isNotBlank() }?.let { "Version $it" }, info?.author?.takeIf { it.isNotBlank() })
                        .filterNotNull().joinToString(" • ")
                    if (line.isNotEmpty()) {
                        EmojiText(
                            line,
                            style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (!info?.description.isNullOrBlank()) {
                        EmojiText(
                            info!!.description,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        )
                    }
                    FilledTonalButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
private fun PluginIcon(icon: String, telegram: Telegram) {
    val set = icon.substringBeforeLast('/', "")
    val index = icon.substringAfterLast('/').toIntOrNull()
    if (set.isBlank() || index == null) {
        Icon(Icons.Filled.Extension, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(PluginIconSize))
        return
    }
    StickerView(telegram.stickerFromSet(set, index), telegram, PluginIconSize)
}

private val PluginIconSize = 96.dp
