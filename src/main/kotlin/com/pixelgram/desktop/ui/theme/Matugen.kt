package com.pixelgram.desktop.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.pixelgram.desktop.core.AppLog
import com.pixelgram.desktop.core.AppPaths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

data class MatugenPalette(
    val light: ColorScheme?,
    val dark: ColorScheme?,
    val sourceColor: Color? = null,
) {
    fun scheme(isDark: Boolean): ColorScheme? = if (isDark) (dark ?: light) else (light ?: dark)
}

object Matugen {
    private const val PollIntervalMs = 1000L

    val templateFile: File get() = File(AppPaths.configDir, "matugen-template.json")
    val outputFile: File get() = File(AppPaths.configDir, "matugen.json")
    val matugenConfigFile: File get() = File(File(System.getProperty("user.home")), ".config/matugen/config.toml")
    val matugenAppsFile: File get() = File(File(System.getProperty("user.home")), ".config/matugen/apps.toml")

    val isAvailable: Boolean by lazy {
        if (!AppPaths.isLinux) return@lazy false
        val paths = System.getenv("PATH")?.split(File.pathSeparatorChar).orEmpty()
        paths.any { dir -> File(dir, "matugen").canExecute() }
    }

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
                templateFile.parentFile?.mkdirs()
                templateFile.writeBytes(content)
            }
        }.onFailure { AppLog.write("Failed to export matugen template", it) }
    }

    private val blockRegex = Regex("\"(light|dark)\"\\s*:\\s*\\{([^}]+)\\}")
    private val entryRegex = Regex("\"(\\w+)\"\\s*:\\s*\"#([0-9a-fA-F]{6})\"")
    private val sourceColorRegex = Regex("\"sourceColor\"\\s*:\\s*\"#([0-9a-fA-F]{6})\"")

    fun readPalette(): MatugenPalette? {
        val text = runCatching { outputFile.readText() }.getOrNull() ?: return null
        if (text.isBlank()) return null

        val sourceColor = sourceColorRegex.find(text)?.let {
            val hex = it.groupValues[1]
            Color(0xFF000000 or hex.toLong(16))
        }

        val blocks = blockRegex.findAll(text).associate { match ->
            val sectionName = match.groupValues[1]
            val sectionContent = match.groupValues[2]
            val roles = entryRegex.findAll(sectionContent).associate { entryMatch ->
                val (name, hex) = entryMatch.destructured
                name to Color(0xFF000000 or hex.toLong(16))
            }
            sectionName to roles
        }

        if (blocks.isNotEmpty()) {
            val lightRoles = blocks["light"]
            val darkRoles = blocks["dark"]
            val lightScheme = lightRoles?.let { colorSchemeOf(it, dark = false) }
            val darkScheme = darkRoles?.let { colorSchemeOf(it, dark = true) }
            if (lightScheme != null || darkScheme != null) {
                return MatugenPalette(light = lightScheme, dark = darkScheme, sourceColor = sourceColor)
            }
        }

        // Fallback for flat JSON (legacy format)
        val flatRoles = entryRegex.findAll(text).associate { match ->
            val (name, hex) = match.destructured
            name to Color(0xFF000000 or hex.toLong(16))
        }
        val background = flatRoles["background"] ?: return null
        val isDark = background.luminance() < 0.5f
        val scheme = colorSchemeOf(flatRoles, dark = isDark)
        return MatugenPalette(
            light = if (!isDark) scheme else null,
            dark = if (isDark) scheme else null,
            sourceColor = sourceColor,
        )
    }

    fun read(dark: Boolean = true): ColorScheme? = readPalette()?.scheme(dark)

    suspend fun poll(onChange: (MatugenPalette?) -> Unit) {
        exportTemplate()
        var lastModified = -1L
        while (true) {
            val modified = withContext(Dispatchers.IO) {
                if (outputFile.exists()) outputFile.lastModified() else 0L
            }
            if (modified != lastModified) {
                lastModified = modified
                val palette = withContext(Dispatchers.IO) { readPalette() }
                onChange(palette)
            }
            delay(PollIntervalMs)
        }
    }

    /**
     * Finds the active desktop wallpaper on Linux by querying all known
     * desktop environments (GNOME, KDE Plasma, XFCE, MATE, Cinnamon, LXQt),
     * Wayland compositors (Hyprland/hyprpaper, Sway, Noctalia/Niri),
     * and standalone background tools (swww, feh, nitrogen).
     */
    fun detectWallpaper(): File? {
        val home = System.getProperty("user.home") ?: return null

        // 1. Noctalia (Niri / Wayland quickshell desktop shell)
        runCatching {
            val logFile = File(home, ".cache/noctalia/noctalia.log")
            if (logFile.isFile) {
                val lines = logFile.readLines().takeLast(150).reversed()
                val pathRegex = Regex("""\[wallpaper\]\s+creating.*path=([^\s,]+)""")
                for (line in lines) {
                    val match = pathRegex.find(line)
                    if (match != null) {
                        val file = File(match.groupValues[1].trim())
                        if (file.isFile) return file
                    }
                }
            }
            val settingsFile = File(home, ".config/noctalia/settings.json")
            if (settingsFile.isFile) {
                val text = settingsFile.readText()
                val favRegex = Regex(""""path"\s*:\s*"([^"]+)"""")
                for (match in favRegex.findAll(text)) {
                    val file = File(match.groupValues[1])
                    if (file.isFile) return file
                }
            }
        }

        // 2. swww (Wayland animated wallpaper daemon)
        runCatching {
            val p = ProcessBuilder("swww", "query").start()
            if (p.waitFor(1, TimeUnit.SECONDS) && p.exitValue() == 0) {
                val out = p.inputStream.bufferedReader().readText()
                val match = Regex("""image:\s*([^\n\r]+)""").find(out)
                if (match != null) {
                    val file = File(match.groupValues[1].trim())
                    if (file.isFile) return file
                }
            }
        }

        // 3. hyprpaper (Hyprland official wallpaper utility)
        runCatching {
            val activeProc = ProcessBuilder("hyprctl", "hyprpaper", "listactive").start()
            if (activeProc.waitFor(1, TimeUnit.SECONDS) && activeProc.exitValue() == 0) {
                val lines = activeProc.inputStream.bufferedReader().readLines()
                for (line in lines.reversed()) {
                    val path = line.substringAfter('=', "").trim()
                    if (path.isNotBlank()) {
                        val file = File(path)
                        if (file.isFile) return file
                    }
                }
            }
            val p = ProcessBuilder("hyprctl", "hyprpaper", "listloaded").start()
            if (p.waitFor(1, TimeUnit.SECONDS) && p.exitValue() == 0) {
                val lines = p.inputStream.bufferedReader().readLines()
                for (line in lines.reversed()) {
                    val trimmed = line.trim()
                    if (trimmed.isNotBlank()) {
                        val file = File(trimmed)
                        if (file.isFile) return file
                    }
                }
            }
            val cfg = File(home, ".config/hypr/hyprpaper.conf")
            if (cfg.isFile) {
                val match = Regex("""wallpaper\s*=\s*[^,]+,\s*([^\n\r]+)""").find(cfg.readText())
                if (match != null) {
                    val file = File(match.groupValues[1].trim())
                    if (file.isFile) return file
                }
            }
        }

        // 4. waypaper (~/.config/waypaper/config.ini)
        runCatching {
            val wpCfg = File(home, ".config/waypaper/config.ini")
            if (wpCfg.isFile) {
                val match = Regex("""wallpaper\s*=\s*([^\n\r]+)""").find(wpCfg.readText())
                if (match != null) {
                    val file = File(match.groupValues[1].trim())
                    if (file.isFile) return file
                }
            }
        }

        // 4. GNOME / Cosmic / Cinnamon (via gsettings / dconf)
        runCatching {
            for (schema in listOf("org.gnome.desktop.background", "org.cinnamon.desktop.background")) {
                for (key in listOf("picture-uri-dark", "picture-uri")) {
                    val p = ProcessBuilder("gsettings", "get", schema, key).start()
                    if (p.waitFor(1, TimeUnit.SECONDS) && p.exitValue() == 0) {
                        val out = p.inputStream.bufferedReader().readText().trim().trim('\'', '"')
                        val path = out.removePrefix("file://")
                        val file = File(path)
                        if (file.isFile) return file
                    }
                }
            }
        }

        // 5. KDE Plasma 5 & 6
        runCatching {
            val cfg = File(home, ".config/plasma-org.kde.plasma.desktop-appletsrc")
            if (cfg.isFile) {
                val text = cfg.readText()
                val match = Regex("""Image=(?:file://)?([^\n\r]+)""").find(text)
                if (match != null) {
                    val path = match.groupValues[1].trim()
                    val candidate = File(path)
                    if (candidate.isFile) return candidate
                    if (candidate.isDirectory) {
                        // Plasma packages wallpapers as directories with contents/images/...
                        candidate.walkTopDown().firstOrNull { it.isFile && it.extension.lowercase() in listOf("jpg", "png", "webp") }?.let {
                            return it
                        }
                    }
                }
            }
        }

        // 6. XFCE (xfconf-query)
        runCatching {
            val p = ProcessBuilder("xfconf-query", "-c", "xfce4-desktop", "-p", "/backdrop/screen0/monitor0/workspace0/last-image").start()
            if (p.waitFor(1, TimeUnit.SECONDS) && p.exitValue() == 0) {
                val path = p.inputStream.bufferedReader().readText().trim()
                val file = File(path)
                if (file.isFile) return file
            }
        }

        // 7. MATE
        runCatching {
            val p = ProcessBuilder("dconf", "read", "/org/mate/desktop/background/picture-filename").start()
            if (p.waitFor(1, TimeUnit.SECONDS) && p.exitValue() == 0) {
                val path = p.inputStream.bufferedReader().readText().trim().trim('\'', '"')
                val file = File(path)
                if (file.isFile) return file
            }
        }

        // 8. feh (~/.fehbg)
        runCatching {
            val fehbg = File(home, ".fehbg")
            if (fehbg.isFile) {
                val match = Regex("""feh\s+.*['"]([^'"]+)['"]""").find(fehbg.readText())
                if (match != null) {
                    val file = File(match.groupValues[1])
                    if (file.isFile) return file
                }
            }
        }

        // 9. Nitrogen
        runCatching {
            val ncfg = File(home, ".config/nitrogen/bg-saved.cfg")
            if (ncfg.isFile) {
                val match = Regex("""file=([^\n\r]+)""").find(ncfg.readText())
                if (match != null) {
                    val file = File(match.groupValues[1].trim())
                    if (file.isFile) return file
                }
            }
        }

        // 10. Scan running process commandlines for swaybg, wbg, mpvpaper
        runCatching {
            val procDir = File("/proc")
            if (procDir.isDirectory) {
                for (dir in procDir.listFiles { f -> f.isDirectory && f.name.all(Char::isDigit) }.orEmpty()) {
                    val cmdlineFile = File(dir, "cmdline")
                    if (cmdlineFile.isFile) {
                        val cmd = cmdlineFile.readBytes().toString(Charsets.UTF_8).replace('\u0000', ' ')
                        if (cmd.contains("swaybg") || cmd.contains("wbg") || cmd.contains("mpvpaper")) {
                            val parts = cmd.split(' ')
                            for (part in parts) {
                                if (part.endsWith(".jpg") || part.endsWith(".png") || part.endsWith(".jpeg") || part.endsWith(".webp")) {
                                    val candidate = File(part)
                                    if (candidate.isFile) return candidate
                                }
                            }
                        }
                    }
                }
            }
        }

        return null
    }

    fun runMatugen(image: File? = null): Result<File> = runCatching {
        exportTemplate()
        val targetImage = image ?: detectWallpaper()
            ?: error("No wallpaper image detected. Please select an image file.")

        if (!targetImage.isFile) {
            error("Wallpaper file not found: ${targetImage.absolutePath}")
        }

        val tempConfig = File(AppPaths.configDir, "matugen-run.toml")
        tempConfig.parentFile?.mkdirs()
        tempConfig.writeText(
            """
            |[config]
            |
            |[templates.pixelgram]
            |input_path = '${templateFile.absolutePath}'
            |output_path = '${outputFile.absolutePath}'
            """.trimMargin()
        )

        try {
            val process = ProcessBuilder(
                "matugen",
                "-c", tempConfig.absolutePath,
                "--source-color-index", "0",
                "image", targetImage.absolutePath,
            ).redirectErrorStream(true).start()

            val finished = process.waitFor(10, TimeUnit.SECONDS)
            val output = process.inputStream.bufferedReader().use { it.readText() }
            if (!finished) {
                process.destroyForcibly()
                error("matugen timed out")
            }
            if (process.exitValue() != 0) {
                error("matugen exited with code ${process.exitValue()}: $output")
            }
            AppLog.write("Matugen generated colors successfully from ${targetImage.name}")
            targetImage
        } finally {
            tempConfig.delete()
        }
    }

    fun installToMatugenConfig(): Result<File> = runCatching {
        exportTemplate()
        val target = when {
            matugenAppsFile.isFile -> matugenAppsFile
            matugenConfigFile.isFile -> matugenConfigFile
            else -> matugenConfigFile.apply { parentFile?.mkdirs() }
        }

        val text = if (target.isFile) target.readText() else "[config]\n\n"
        if (!text.contains("[templates.pixelgram]")) {
            val separator = if (text.endsWith("\n\n")) "" else if (text.endsWith("\n")) "\n" else "\n\n"
            target.writeText(text + separator + configSnippet + "\n")
            AppLog.write("Added pixelgram template to ${target.absolutePath}")
        }
        target
    }
}

@Composable
fun rememberMatugenScheme(dark: Boolean): ColorScheme? {
    val palette by produceState<MatugenPalette?>(initialValue = null) {
        Matugen.poll { value = it }
    }
    return palette?.scheme(dark)
}

@Composable
fun rememberMatugenScheme(): ColorScheme? = rememberMatugenScheme(dark = true)
