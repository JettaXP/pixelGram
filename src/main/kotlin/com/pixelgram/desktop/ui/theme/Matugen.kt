package com.pixelgram.desktop.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.pixelgram.desktop.core.AppPaths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

object Matugen {
    private const val PollIntervalMs = 1000L

    val templateFile: File get() = File(AppPaths.configDir, "matugen-template.json")
    val outputFile: File get() = File(AppPaths.configDir, "matugen.json")

    val configSnippet: String
        get() = """
            |[templates.pixelgram]
            |input_path = '${templateFile.absolutePath}'
            |output_path = '${outputFile.absolutePath}'
            """.trimMargin()

    private val configFile: File
        get() = File(System.getenv("XDG_CONFIG_HOME")?.takeIf { it.isNotBlank() } ?: (System.getProperty("user.home") + "/.config"), "matugen/config.toml")

    fun configured(): Boolean = runCatching {
        val text = configFile.readText()
        text.contains("[templates.pixelgram]") || text.contains(outputFile.absolutePath)
    }.getOrDefault(false)

    fun exportTemplate() {
        val content = javaClass.getResourceAsStream("/matugen/pixelgram.json")
            ?.use { it.readBytes() } ?: return
        runCatching {
            if (!templateFile.exists() || !templateFile.readBytes().contentEquals(content)) {
                templateFile.writeBytes(content)
            }
        }
    }

    private val entry = Regex("\"(\\w+)\"\\s*:\\s*\"#([0-9a-fA-F]{6})\"")

    fun read(): ColorScheme? {
        val text = runCatching { outputFile.readText() }.getOrNull() ?: return null
        val roles = entry.findAll(text).associate { match ->
            val (name, hex) = match.destructured
            name to Color(0xFF000000 or hex.toLong(16))
        }
        val background = roles["background"] ?: return null
        return colorSchemeOf(roles, dark = background.luminance() < 0.5f)
    }

    suspend fun poll(onChange: (ColorScheme?) -> Unit) {
        exportTemplate()
        var lastText: String? = null
        var first = true
        while (true) {
            val text = withContext(Dispatchers.IO) { runCatching { outputFile.readText() }.getOrNull() }
            if (first || text != lastText) {
                first = false
                lastText = text
                onChange(withContext(Dispatchers.IO) { read() })
            }
            delay(PollIntervalMs)
        }
    }
}

@Composable
fun rememberMatugenScheme(): ColorScheme? {
    val scheme by produceState<ColorScheme?>(initialValue = null) {
        Matugen.poll { value = it }
    }
    return scheme
}
