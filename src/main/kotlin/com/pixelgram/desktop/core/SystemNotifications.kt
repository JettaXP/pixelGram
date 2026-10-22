package com.pixelgram.desktop.core

import androidx.compose.material3.ColorScheme
import com.pixelgram.desktop.ui.AppIcons
import java.awt.EventQueue
import java.io.File

object SystemNotifications {
    val available: Boolean by lazy { AppPaths.isLinux && onPath("notify-send") }

    private fun onPath(name: String) = System.getenv("PATH").orEmpty().split(File.pathSeparator)
        .any { File(it, name).canExecute() }

    @Volatile
    private var appIcon: String? = null

    fun updateIcon(scheme: ColorScheme) {
        val file = File(AppPaths.configDir, "pixelgram-icon.png")
        AppIcons.writeTo(file, scheme)
        appIcon = file.absolutePath.takeIf { file.isFile }
    }

    fun show(title: String, body: String, icon: String?, onClick: () -> Unit) {
        Thread {
            runCatching {
                val base = listOf("notify-send", "--app-name=pixelGram", "--icon=${icon ?: appIcon ?: "pixelgram"}")
                val text = listOf(title, escape(body))
                val clicked = run(base + listOf("--action=default=Open", "--wait") + text)
                    ?: run(base + text).let { null }
                if (clicked == "default") EventQueue.invokeLater(onClick)
            }.onFailure { AppLog.write("System notification failed", it) }
        }.apply { isDaemon = true }.start()
    }

    private fun run(command: List<String>): String? {
        val process = ProcessBuilder(command).redirectErrorStream(false).start()
        val output = process.inputStream.bufferedReader().readText().trim()
        return if (process.waitFor() == 0) output else null
    }

    private fun escape(text: String) = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
