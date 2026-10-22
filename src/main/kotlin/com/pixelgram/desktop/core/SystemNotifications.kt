package com.pixelgram.desktop.core

import java.awt.EventQueue
import java.io.File

// Linux: notifications through the desktop's own notification daemon (mako,
// swaync, dunst, GNOME, KDE...) via notify-send from libnotify.
object SystemNotifications {
    val available: Boolean by lazy { AppPaths.isLinux && onPath("notify-send") }

    private fun onPath(name: String) = System.getenv("PATH").orEmpty().split(File.pathSeparator)
        .any { File(it, name).canExecute() }

    // The app icon as a file, for notifications from chats without a photo.
    private val appIcon: String? by lazy {
        runCatching {
            val file = File(AppPaths.configDir, "pixelgram-icon.png")
            if (!file.isFile) {
                SystemNotifications::class.java.getResourceAsStream("/icon-dark.png")
                    ?.use { input -> file.outputStream().use { input.copyTo(it) } }
            }
            file.absolutePath.takeIf { file.isFile }
        }.getOrNull()
    }

    fun show(title: String, body: String, icon: String?, onClick: () -> Unit) {
        Thread {
            runCatching {
                val base = listOf("notify-send", "--app-name=pixelGram", "--icon=${icon ?: appIcon ?: "pixelgram"}")
                val text = listOf(title, escape(body))
                // --action needs libnotify 0.7.10+; without it the notification just can't be clicked.
                val clicked = run(base + listOf("--action=default=Open", "--wait") + text)
                    ?: run(base + text).let { null }
                if (clicked == "default") EventQueue.invokeLater(onClick)
            }.onFailure { AppLog.write("System notification failed", it) }
        }.apply { isDaemon = true }.start()
    }

    // The clicked action's name, or null when the command failed.
    private fun run(command: List<String>): String? {
        val process = ProcessBuilder(command).redirectErrorStream(false).start()
        val output = process.inputStream.bufferedReader().readText().trim()
        return if (process.waitFor() == 0) output else null
    }

    // Notification bodies may be read as markup.
    private fun escape(text: String) = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
