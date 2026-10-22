package com.pixelgram.desktop.telegram

import com.pixelgram.desktop.core.AppLog
import com.pixelgram.desktop.core.AppPaths
import java.io.File

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
        System.setProperty("it.tdlight.native.workdir", AppPaths.nativesDir.absolutePath)
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
