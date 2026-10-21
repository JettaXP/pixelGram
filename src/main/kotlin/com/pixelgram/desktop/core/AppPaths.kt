package com.pixelgram.desktop.core

import java.io.File

object AppPaths {
    private val os = System.getProperty("os.name").lowercase()

    val isWindows = os.contains("win")
    val isMac = os.contains("mac")
    val isLinux = !isWindows && !isMac

    private val home = File(System.getProperty("user.home"))

    // Root of the installed app (folder with pixelGram.exe, or with bin/ on
    // Linux); null when running from sources.
    private val appDir: File? by lazy {
        val launcher = System.getProperty("jpackage.app-path")?.let { File(it).absoluteFile }
        when {
            launcher == null -> null
            isWindows -> launcher.parentFile
            else -> launcher.parentFile?.parentFile
        }
    }

    // Portable mode: a "pixelgram-data" folder next to the app keeps all data.
    val portableDir: File? by lazy {
        appDir?.let { File(it, "pixelgram-data") }?.takeIf { it.isDirectory }
    }

    val isPortable: Boolean get() = portableDir != null

    // Linux: ~/.local/share/pixelgram, Windows: %APPDATA%\pixelgram,
    // macOS: ~/Library/Application Support/pixelgram, or the portable folder.
    val dataDir: File by lazy {
        portableDir?.let { return@lazy it }
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

    // TDLib's native library is unpacked here instead of the system temp folder.
    val nativesDir: File get() = File(dataDir, "natives").apply { mkdirs() }
}
