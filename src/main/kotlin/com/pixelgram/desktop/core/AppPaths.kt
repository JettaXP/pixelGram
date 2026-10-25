package com.pixelgram.desktop.core

import java.io.File

object AppPaths {
    private val os = System.getProperty("os.name").lowercase()

    val isWindows = os.contains("win")
    val isMac = os.contains("mac")
    val isLinux = !isWindows && !isMac

    private val home = File(System.getProperty("user.home"))

    val appDir: File? by lazy {
        val launcher = System.getProperty("jpackage.app-path")?.let { File(it).absoluteFile }
        when {
            launcher == null -> null
            isWindows -> launcher.parentFile
            isMac -> generateSequence(launcher.parentFile) { it.parentFile }
                .firstOrNull { it.name.endsWith(".app") }
                ?: launcher.parentFile?.parentFile
            else -> launcher.parentFile?.parentFile
        }
    }

    /** True when the JVM runs from a macOS application bundle (needed by native APIs). */
    val isMacBundle: Boolean get() = isMac && appDir?.name?.endsWith(".app") == true

    val portableDir: File? by lazy {
        if (isMac) return@lazy null
        appDir?.let { File(it, "pixelgram-data") }?.takeIf { it.isDirectory }
    }

    val isPortable: Boolean get() = portableDir != null

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

    val configDir: File by lazy {
        if (isMac) return@lazy File(dataDir, "config").apply { mkdirs() }
        val base = System.getenv("XDG_CONFIG_HOME")?.takeIf { it.isNotBlank() }?.let(::File)
            ?: File(home, ".config")
        File(base, "pixelgram").apply { mkdirs() }
    }

    fun accountDir(account: Int): File =
        if (account == 0) File(dataDir, "tdlib") else File(dataDir, "accounts/$account")
    val settingsFile: File get() = File(dataDir, "settings.properties")

    val nativesDir: File get() = File(dataDir, "natives").apply { mkdirs() }
}
