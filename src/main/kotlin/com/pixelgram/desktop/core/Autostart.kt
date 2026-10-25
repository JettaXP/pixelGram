package com.pixelgram.desktop.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File

object Autostart {
    var launchedAtLogin = false

    val launcher: File? get() = System.getenv("APPIMAGE")?.let(::File)?.takeIf { it.isFile }
        ?: System.getProperty("jpackage.app-path")?.let(::File)?.takeIf { it.isFile }

    val supported: Boolean get() = launcher != null

    fun apply(enabled: Boolean) {
        val exe = launcher ?: return
        runCatching {
            when {
                AppPaths.isWindows -> windows(exe, enabled)
                AppPaths.isMac -> mac(exe, enabled)
                else -> linux(exe, enabled)
            }
        }.onFailure { AppLog.write("Autostart update failed", it) }
    }

    private fun windows(exe: File, enabled: Boolean) {
        val command = if (enabled) {
            listOf("reg", "add", RunKey, "/v", Name, "/t", "REG_SZ", "/d", "\"${exe.absolutePath}\" $Flag", "/f")
        } else {
            listOf("reg", "delete", RunKey, "/v", Name, "/f")
        }
        ProcessBuilder(command).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start().waitFor()
    }

    private fun linux(exe: File, enabled: Boolean) {
        val home = System.getenv("XDG_CONFIG_HOME")?.takeIf { it.isNotBlank() } ?: (System.getProperty("user.home") + "/.config")
        val entry = File(home, "autostart/com.pixelgram.desktop.desktop")
        if (!enabled) {
            entry.delete()
            return
        }
        entry.parentFile.mkdirs()
        entry.writeText(
            """
            [Desktop Entry]
            Type=Application
            Name=pixelGram
            Exec="${exe.absolutePath}" $Flag
            Icon=com.pixelgram.desktop
            X-GNOME-Autostart-enabled=true
            """.trimIndent() + "\n",
        )
    }

    private fun mac(exe: File, enabled: Boolean) {
        val agent = File(System.getProperty("user.home"), "Library/LaunchAgents/com.pixelgram.desktop.autostart.plist")
        if (!enabled) {
            agent.delete()
            return
        }
        agent.parentFile.mkdirs()
        agent.writeText(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
            <plist version="1.0">
            <dict>
                <key>Label</key>
                <string>com.pixelgram.desktop.autostart</string>
                <key>ProgramArguments</key>
                <array>
                    <string>${exe.absolutePath}</string>
                    <string>$Flag</string>
                </array>
                <key>RunAtLoad</key>
                <true/>
            </dict>
            </plist>
            """.trimIndent() + "\n",
        )
    }

    const val Flag = "--autostart"
    private const val Name = "pixelGram"
    private const val RunKey = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run"
}

object WindowTitle {
    var settings: AppSettings? = null
    var chat by mutableStateOf<String?>(null)
    var account by mutableStateOf<String?>(null)
    var unread by mutableIntStateOf(0)

    fun text(): String = settings?.let(::text) ?: "pixelGram"

    fun text(settings: AppSettings): String {
        val base = if (settings.titleUnread.value && unread > 0) "pixelGram ($unread)" else "pixelGram"
        val parts = listOfNotNull(
            chat?.takeIf { settings.titleChatName.value && it.isNotBlank() },
            base,
            account?.takeIf { settings.titleAccount.value && it.isNotBlank() },
        )
        return parts.joinToString(" – ")
    }
}
