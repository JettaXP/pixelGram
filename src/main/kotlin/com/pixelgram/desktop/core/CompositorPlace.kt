package com.pixelgram.desktop.core

import java.io.File

object CompositorPlace {
    fun place(title: String, x: Int, y: Int, width: Int, height: Int) {
        if (!AppPaths.isLinux || System.getenv("WAYLAND_DISPLAY").isNullOrBlank()) return
        Thread {
            runCatching {
                Thread.sleep(120)
                when {
                    !System.getenv("NIRI_SOCKET").isNullOrBlank() -> niri(title, x, y)
                    !System.getenv("HYPRLAND_INSTANCE_SIGNATURE").isNullOrBlank() -> {
                        val rule = "title:^(${Regex.escape(title)})$"
                        run(listOf("hyprctl", "--batch", "dispatch setfloating $rule ; dispatch resizewindowpixel exact $width $height,$rule ; dispatch movewindowpixel exact $x $y,$rule ; dispatch pin $rule"))
                    }
                    !System.getenv("SWAYSOCK").isNullOrBlank() -> run(listOf("swaymsg", "[title=\"^$title$\"] floating enable, resize set $width $height, move absolute position $x $y, sticky enable"))
                    else -> null
                }
            }.onFailure { AppLog.write("Compositor place failed", it) }
        }.apply { isDaemon = true }.start()
    }

    private fun niri(title: String, x: Int, y: Int) {
        val json = run(listOf("niri", "msg", "--json", "windows")) ?: return
        val window = Regex("\\{[^{}]*\"id\"\\s*:\\s*(\\d+)[^{}]*\\}").findAll(json).map { it.value }
            .firstOrNull { Regex("\"title\"\\s*:\\s*\"${Regex.escape(title)}\"").containsMatchIn(it) } ?: return
        val id = Regex("\"id\"\\s*:\\s*(\\d+)").find(window)?.groupValues?.get(1) ?: return
        if (!Regex("\"is_floating\"\\s*:\\s*true").containsMatchIn(window)) run(listOf("niri", "msg", "action", "move-window-to-floating", "--id", id))
        run(listOf("niri", "msg", "action", "move-floating-window", "--id", id, "-x", x.toString(), "-y", y.toString()))
    }

    private fun onPath(name: String) = System.getenv("PATH").orEmpty().split(File.pathSeparator).any { File(it, name).canExecute() }

    private fun run(command: List<String>): String? {
        if (!onPath(command.first())) return null
        val process = ProcessBuilder(command).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText().trim()
        return if (process.waitFor() == 0) output else null
    }
}
