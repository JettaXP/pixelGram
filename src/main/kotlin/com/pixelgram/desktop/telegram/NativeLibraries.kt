package com.pixelgram.desktop.telegram

import com.pixelgram.desktop.core.AppLog
import com.pixelgram.desktop.core.AppPaths
import java.io.File

// TDLib on Windows needs the Microsoft Visual C++ runtime. The DLLs ship
// with the app (and with the bundled Java runtime); loading them first lets
// Windows resolve TDLib's imports without a system-wide installation.
object NativeLibraries {
    private val windowsRuntime = listOf(
        "vcruntime140.dll",
        "vcruntime140_1.dll",
        "msvcp140.dll",
        "msvcp140_1.dll",
        "msvcp140_2.dll",
        "concrt140.dll",
    )

    fun preload() {
        if (!AppPaths.isWindows) return
        val folders = listOfNotNull(
            System.getProperty("compose.application.resources.dir")?.let(::File),
            System.getProperty("java.home")?.let { File(it, "bin") },
        )
        for (name in windowsRuntime) {
            val file = folders.map { File(it, name) }.firstOrNull { it.isFile } ?: continue
            runCatching { System.load(file.absolutePath) }
                .onFailure { AppLog.write("Could not preload ${file.absolutePath}", it) }
        }
    }
}
