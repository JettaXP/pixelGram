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
        var lastModified = -1L
        while (true) {
            val modified = withContext(Dispatchers.IO) { outputFile.lastModified() }
            if (modified != lastModified) {
                lastModified = modified
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
