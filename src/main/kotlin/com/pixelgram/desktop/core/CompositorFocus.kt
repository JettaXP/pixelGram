package com.pixelgram.desktop.core

import java.io.File

object CompositorFocus {
    private val pid = ProcessHandle.current().pid()

    fun focus(title: String) {
        if (!AppPaths.isLinux) return
        Thread {
            runCatching {
                when {
                    !System.getenv("NIRI_SOCKET").isNullOrBlank() -> niri(title)
                    !System.getenv("HYPRLAND_INSTANCE_SIGNATURE").isNullOrBlank() -> run(listOf("hyprctl", "dispatch", "focuswindow", "pid:$pid"))
                    !System.getenv("SWAYSOCK").isNullOrBlank() -> run(listOf("swaymsg", "[pid=$pid] focus"))
                    else -> null
                }
            }.onFailure { AppLog.write("Compositor focus failed", it) }
        }.apply { isDaemon = true }.start()
    }

    private fun niri(title: String) {
        val json = run(listOf("niri", "msg", "--json", "windows")) ?: return
        val windows = Regex("\\{[^{}]*\"id\"\\s*:\\s*(\\d+)[^{}]*\\}").findAll(json).map { it.value }.toList()
        fun field(window: String, name: String) = Regex("\"$name\"\\s*:\\s*(\"(?:[^\"\\\\]|\\\\.)*\"|\\d+|null)").find(window)?.groupValues?.get(1)?.trim('"')
        val target = windows.firstOrNull { field(it, "pid") == pid.toString() }
            ?: windows.firstOrNull { field(it, "title") == title }
            ?: windows.firstOrNull { field(it, "title")?.contains("pixelGram", ignoreCase = true) == true }
            ?: return
        val id = field(target, "id") ?: return
        run(listOf("niri", "msg", "action", "focus-window", "--id", id))
    }

    private fun onPath(name: String) = System.getenv("PATH").orEmpty().split(File.pathSeparator).any { File(it, name).canExecute() }

    private fun run(command: List<String>): String? {
        if (!onPath(command.first())) return null
        val process = ProcessBuilder(command).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText().trim()
        return if (process.waitFor() == 0) output else null
    }
}
