package com.pixelgram.desktop.core

import java.io.File

object AppPaths {
    private val os = System.getProperty("os.name").lowercase()

    val isWindows = os.contains("win")
    val isMac = os.contains("mac")
    val isLinux = !isWindows && !isMac

    private val home = File(System.getProperty("user.home"))

    // Linux: ~/.local/share/pixelgram, Windows: %APPDATA%\pixelgram,
    // macOS: ~/Library/Application Support/pixelgram.
    val dataDir: File by lazy {
        val base = when {
            isWindows -> System.getenv("APPDATA")?.let(::File) ?: home
            isMac -> File(home, "Library/Application Support")
            else -> System.getenv("XDG_DATA_HOME")?.takeIf { it.isNotBlank() }?.let(::File)
                ?: File(home, ".local/share")
        }
        File(base, "pixelgram").apply { mkdirs() }
    }

    // Linux: ~/.config/pixelgram, used for the matugen template and output.
    val configDir: File by lazy {
        val base = System.getenv("XDG_CONFIG_HOME")?.takeIf { it.isNotBlank() }?.let(::File)
            ?: File(home, ".config")
        File(base, "pixelgram").apply { mkdirs() }
    }

    val tdlibDatabase: File get() = File(dataDir, "tdlib/database")
    val tdlibFiles: File get() = File(dataDir, "tdlib/files")
    val settingsFile: File get() = File(dataDir, "settings.properties")
}
