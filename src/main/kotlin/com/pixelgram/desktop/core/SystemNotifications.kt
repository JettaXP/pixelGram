package com.pixelgram.desktop.core

import androidx.compose.material3.ColorScheme
import com.pixelgram.desktop.ui.AppIcons
import java.awt.EventQueue
import java.io.File

object SystemNotifications {
    private val notifySend: Boolean by lazy { onPath("notify-send") }
    private val linuxAvailable: Boolean by lazy { AppPaths.isLinux && (notifySend || onPath("gdbus")) }
    val available: Boolean
        get() = when {
            AppPaths.isMac -> MacNotifications.available
            AppPaths.isWindows -> trayIcon != null
            else -> linuxAvailable
        }

    fun enabled(settings: AppSettings): Boolean =
        available && (settings.notificationStyle.value == NotificationStyle.System || !AppPaths.translucentWindows) &&
            (!AppPaths.isWindows || settings.windowsToasts.value)

    @Volatile
    var trayIcon: java.awt.TrayIcon? = null
        set(value) {
            field?.removeActionListener(clickListener)
            field = value
            value?.addActionListener(clickListener)
        }

    @Volatile
    private var lastClick: (() -> Unit)? = null
    private val clickListener = java.awt.event.ActionListener { lastClick?.let { EventQueue.invokeLater(it) } }

    private fun onPath(name: String) = System.getenv("PATH").orEmpty().split(File.pathSeparator)
        .any { File(it, name).canExecute() }

    @Volatile
    private var appIcon: String? = null

    fun prepare() {
        if (AppPaths.isMac) MacNotifications.prepare()
    }

    fun updateIcon(scheme: ColorScheme) {
        if (AppPaths.isMac) return
        val file = File(AppPaths.configDir, "pixelgram-icon.png")
        AppIcons.writeTo(file, scheme)
        appIcon = file.absolutePath.takeIf { file.isFile }
    }

    fun show(title: String, body: String, icon: String?, onClick: () -> Unit) {
        if (AppPaths.isMac) {
            MacNotifications.show(title, body, onClick)
            return
        }
        if (AppPaths.isWindows) {
            val icon = trayIcon ?: return
            lastClick = onClick
            runCatching { icon.displayMessage(title, body, java.awt.TrayIcon.MessageType.NONE) }
                .onFailure { AppLog.write("Windows notification failed", it) }
            return
        }
        Thread {
            runCatching {
                if (!notifySend) {
                    viaDbus(title, body, icon ?: appIcon ?: "pixelgram")
                    return@runCatching
                }
                val base = listOf("notify-send", "--app-name=pixelGram", "--icon=${icon ?: appIcon ?: "pixelgram"}")
                val text = listOf(title, escape(body))
                val clicked = run(base + listOf("--action=default=Open", "--wait") + text)
                    ?: run(base + text).let { null }
                if (clicked == "default") EventQueue.invokeLater(onClick)
            }.onFailure { AppLog.write("System notification failed", it) }
        }.apply { isDaemon = true }.start()
    }

    private fun viaDbus(title: String, body: String, icon: String) {
        run(
            listOf(
                "gdbus", "call", "--session",
                "--dest", "org.freedesktop.Notifications",
                "--object-path", "/org/freedesktop/Notifications",
                "--method", "org.freedesktop.Notifications.Notify",
                variant("pixelGram"), "uint32 0", variant(icon), variant(title), variant(escape(body)),
                "@as []", "@a{sv} {}", "int32 -1",
            ),
        ) ?: error("gdbus could not reach the notification daemon")
    }

    private fun variant(text: String) = "'" + text.replace("\\", "\\\\").replace("'", "\\'") + "'"

    private fun run(command: List<String>): String? {
        val process = ProcessBuilder(command).redirectErrorStream(false).start()
        val output = process.inputStream.bufferedReader().readText().trim()
        return if (process.waitFor() == 0) output else null
    }

    private fun escape(text: String) = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
