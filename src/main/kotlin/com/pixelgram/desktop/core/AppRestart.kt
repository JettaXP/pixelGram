package com.pixelgram.desktop.core

import java.io.File
import kotlin.system.exitProcess

object AppRestart {
    private fun launcher(): File? =
        (System.getenv("APPIMAGE") ?: System.getProperty("jpackage.app-path"))?.let(::File)?.takeIf { it.isFile }

    val available: Boolean get() = launcher() != null

    fun restart() {
        val exe = launcher() ?: return
        val pid = ProcessHandle.current().pid().toString()
        val command = if (AppPaths.isWindows) {
            val path = exe.absolutePath.replace("'", "''")
            listOf(
                "powershell", "-NoProfile", "-WindowStyle", "Hidden", "-Command",
                "Wait-Process -Id $pid -ErrorAction SilentlyContinue; Start-Process -FilePath '$path'",
            )
        } else {
            listOf("nohup", "sh", "-c", "while kill -0 \"\$1\" 2>/dev/null; do sleep 0.2; done; exec \"\$2\"", "sh", pid, exe.absolutePath)
        }
        runCatching { ApplyUpdate.launch(command, null) }
            .onSuccess { exitProcess(0) }
            .onFailure { AppLog.write("Restart failed", it) }
    }
}
