package com.pixelgram.desktop.core

import java.io.File

object UrlScheme {
    private const val Scheme = "tg"
    private const val DesktopFile = "com.pixelgram.desktop-links.desktop"

    fun register() {
        Thread {
            runCatching {
                when {
                    AppPaths.isWindows -> registerWindows()
                    AppPaths.isLinux -> registerLinux()
                }
            }.onFailure { AppLog.write("tg:// registration failed", it) }
        }.apply { isDaemon = true }.start()
    }

    private fun registerWindows() {
        val exe = System.getProperty("jpackage.app-path")?.takeIf { File(it).isFile } ?: return
        val key = "HKCU\\Software\\Classes\\$Scheme"
        reg(key, "/ve", "/d", "URL:Telegram Link")
        reg(key, "/v", "URL Protocol", "/d", "")
        reg("$key\\DefaultIcon", "/ve", "/d", "\"$exe\",0")
        reg("$key\\shell\\open\\command", "/ve", "/d", "\"$exe\" \"%1\"")
    }

    private fun reg(key: String, vararg args: String) {
        ProcessBuilder(listOf("reg", "add", key) + args + "/f")
            .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start().waitFor()
    }

    private fun registerLinux() {
        val launcher = System.getenv("APPIMAGE")?.takeIf { it.isNotBlank() }
            ?: System.getProperty("jpackage.app-path")?.takeIf { File(it).isFile }
            ?: return
        val dataHome = System.getenv("XDG_DATA_HOME")?.takeIf { it.isNotBlank() }
            ?: (System.getProperty("user.home") + "/.local/share")
        val dir = File(dataHome, "applications").apply { mkdirs() }
        val entry = """
            |[Desktop Entry]
            |Type=Application
            |Name=pixelGram
            |Exec="$launcher" %u
            |Icon=com.pixelgram.desktop
            |NoDisplay=true
            |MimeType=x-scheme-handler/$Scheme;
            |""".trimMargin()
        val file = File(dir, DesktopFile)
        if (file.isFile && file.readText() == entry) return
        file.writeText(entry)
        run("xdg-mime", "default", DesktopFile, "x-scheme-handler/$Scheme")
        run("update-desktop-database", dir.path)
    }

    private fun run(vararg command: String) {
        runCatching {
            ProcessBuilder(*command).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start().waitFor()
        }
    }
}
