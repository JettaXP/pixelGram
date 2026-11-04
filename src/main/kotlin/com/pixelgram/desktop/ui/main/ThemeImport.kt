package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.core.ThemeMode
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.theme.schemeFromSeed
import it.tdlight.jni.TdApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile

class ThemeRequest(val telegram: Telegram, val file: TdApi.File?, val name: String)

class ParsedTheme(val accent: Int, val background: Int, val dark: Boolean, val swatches: List<Int>)

object ThemeImport {
    var request by mutableStateOf<ThemeRequest?>(null)

    private val Extensions = listOf(".tdesktop-theme", ".tdesktop-palette", ".attheme")

    fun isTheme(name: String): Boolean = Extensions.any { name.endsWith(it, ignoreCase = true) }

    fun pick(documents: Array<TdApi.Document>?): TdApi.Document? =
        documents?.sortedBy { if (it.fileName.endsWith(".attheme", true)) 1 else 0 }?.firstOrNull { isTheme(it.fileName) }

    fun parse(file: File, name: String): ParsedTheme? = runCatching {
        if (name.endsWith(".attheme", true)) android(file.readText()) else desktop(desktopText(file))
    }.getOrNull()

    private fun desktopText(file: File): String {
        val zip = file.inputStream().use { input -> ByteArray(2).also { input.read(it) } }.let { it[0] == 'P'.code.toByte() && it[1] == 'K'.code.toByte() }
        if (!zip) return file.readText()
        return ZipFile(file).use { archive ->
            val entry = archive.getEntry("colors.tdesktop-theme") ?: archive.getEntry("colors.tdesktop-palette") ?: return ""
            archive.getInputStream(entry).use { String(it.readBytes()) }
        }
    }

    private fun desktop(text: String): ParsedTheme? {
        val raw = HashMap<String, String>()
        text.lineSequence().forEach { line ->
            val clean = line.substringBefore("//").trim().removeSuffix(";").trim()
            val key = clean.substringBefore(':', "").trim()
            val value = clean.substringAfter(':', "").trim()
            if (key.isNotEmpty() && value.isNotEmpty()) raw[key] = value
        }
        fun color(key: String, depth: Int = 0): Int? {
            val value = raw[key] ?: return null
            if (!value.startsWith("#")) return if (depth < 16) color(value, depth + 1) else null
            return hex(value.removePrefix("#"))
        }
        val background = color("windowBg") ?: color("dialogsBg") ?: return null
        val accent = color("windowBgActive") ?: color("activeButtonBg") ?: color("msgOutBg") ?: return null
        val swatches = listOf("windowBg", "windowBgActive", "msgInBg", "msgOutBg", "dialogsBgActive", "windowFg").mapNotNull { color(it) }.distinct()
        return ParsedTheme(accent, background, Color(background).luminance() < 0.4f, swatches)
    }

    private fun android(text: String): ParsedTheme? {
        val raw = HashMap<String, Int>()
        for (line in text.lineSequence()) {
            if (line.startsWith("WPS")) break
            val key = line.substringBefore('=', "").trim()
            val value = line.substringAfter('=', "").trim()
            val parsed = if (value.startsWith("#")) hex(value.removePrefix("#")) else value.toLongOrNull()?.toInt()
            if (key.isNotEmpty() && parsed != null) raw[key] = parsed
        }
        val background = raw["windowBackgroundWhite"] ?: raw["chat_wallpaper"] ?: return null
        val accent = raw["chats_actionBackground"] ?: raw["actionBarDefault"] ?: raw["chat_outBubble"] ?: return null
        val swatches = listOf("windowBackgroundWhite", "actionBarDefault", "chat_inBubble", "chat_outBubble", "chats_actionBackground", "windowBackgroundWhiteBlackText")
            .mapNotNull { raw[it] }.distinct()
        return ParsedTheme(accent, background, Color(background).luminance() < 0.4f, swatches)
    }

    private fun hex(value: String): Int? = when (value.length) {
        6 -> value.toLongOrNull(16)?.let { (0xFF000000 or it).toInt() }
        8 -> value.toLongOrNull(16)?.let { (((it and 0xFF) shl 24) or (it ushr 8)).toInt() }
        else -> null
    }
}

@Composable
fun ThemeImportDialog(request: ThemeRequest, settings: AppSettings, onDismiss: () -> Unit) {
    val telegram = request.telegram
    LaunchedEffect(request) { telegram.download(request.file) }
    val path = telegram.localPath(request.file)
    val parsed by produceState<ParsedTheme?>(null, path) {
        value = path?.let { withContext(Dispatchers.IO) { ThemeImport.parse(File(it), request.name) } }
    }
    val failed = path != null && parsed == null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(request.name.substringBeforeLast('.').ifBlank { "Theme" }) },
        text = {
            Box(Modifier.fillMaxWidth().widthIn(min = 320.dp), contentAlignment = Alignment.Center) {
                val theme = parsed
                when {
                    theme != null -> ThemePreview(theme, settings)
                    failed -> Text("This theme file could not be read")
                    else -> LoadingIndicator(Modifier.padding(24.dp))
                }
            }
        },
        confirmButton = {
            val theme = parsed
            TextButton(enabled = theme != null, onClick = {
                if (theme != null) {
                    settings.seedColor.value = theme.accent
                    settings.themeMode.value = if (theme.dark) ThemeMode.Dark else ThemeMode.Light
                    settings.matugen.value = false
                }
                onDismiss()
            }) { Text("Apply this theme") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ThemePreview(theme: ParsedTheme, settings: AppSettings) {
    val scheme = schemeFromSeed(theme.accent, theme.dark, settings.colorStyle.value)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MaterialTheme(colorScheme = scheme) {
            val colors = MaterialTheme.colorScheme
            Surface(shape = RoundedCornerShape(24.dp), color = colors.surface, contentColor = colors.onSurface) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(36.dp).background(colors.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                            Text("P", color = colors.onPrimaryContainer, style = MaterialTheme.typography.titleMedium)
                        }
                        Column {
                            Text("pixelGram", style = MaterialTheme.typography.titleSmall)
                            Text("online", style = MaterialTheme.typography.bodySmall, color = colors.primary)
                        }
                    }
                    PreviewBubble("Have you seen the new theme?", false)
                    PreviewBubble("Looks great!", true)
                    PreviewBubble("Apply it in one click", false)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(shape = CircleShape, color = colors.surfaceContainerHighest, modifier = Modifier.weight(1f).height(40.dp)) {
                            Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.padding(horizontal = 16.dp)) {
                                Text("Message", color = colors.onSurfaceVariant)
                            }
                        }
                        Box(Modifier.size(40.dp).background(colors.primary, CircleShape))
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            theme.swatches.forEach { argb ->
                Box(Modifier.size(22.dp).background(Color(argb), CircleShape))
            }
            Box(Modifier.width(4.dp))
            Text(
                "Colors are adapted to Material You",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PreviewBubble(text: String, outgoing: Boolean) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.fillMaxWidth(), contentAlignment = if (outgoing) Alignment.CenterEnd else Alignment.CenterStart) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (outgoing) colors.primary else colors.surfaceContainerHighest,
            contentColor = if (outgoing) colors.onPrimary else colors.onSurface,
        ) {
            Text(text, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
        }
    }
}
