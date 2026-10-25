package com.pixelgram.desktop.core

import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object Downloads {
    val dir: File by lazy { File(systemDownloads(), "pixelGram") }

    private val exported = ConcurrentHashMap<String, File>()

    fun exported(path: String, name: String): String? {
        exported[path]?.takeIf { it.isFile }?.let { return it.absolutePath }
        val source = File(path)
        val candidate = File(dir, safeName(name, source))
        return candidate.takeIf { it.isFile && it.length() == source.length() }?.also { exported[path] = it }?.absolutePath
    }

    suspend fun export(path: String, name: String): String = withContext(Dispatchers.IO) {
        exported(path, name)?.let { return@withContext it }
        runCatching {
            val source = File(path)
            dir.mkdirs()
            val target = freeName(safeName(name, source))
            val part = File(dir, target.name + ".part")
            source.copyTo(part, overwrite = true)
            if (!part.renameTo(target)) error("Rename failed")
            exported[path] = target
            target.absolutePath
        }.onFailure { AppLog.write("Export to downloads failed", it) }.getOrDefault(path)
    }

    private fun safeName(name: String, source: File): String =
        name.ifBlank { source.name }.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().ifBlank { source.name }

    private fun freeName(name: String): File {
        val first = File(dir, name)
        if (!first.exists()) return first
        val base = name.substringBeforeLast('.', name)
        val ext = name.substringAfterLast('.', "").let { if (it.isEmpty() || it == name) "" else ".$it" }
        var index = 1
        while (true) {
            val next = File(dir, "$base ($index)$ext")
            if (!next.exists()) return next
            index++
        }
    }

    private fun systemDownloads(): File {
        val home = File(System.getProperty("user.home"))
        if (AppPaths.isLinux) {
            runCatching {
                val process = ProcessBuilder("xdg-user-dir", "DOWNLOAD").redirectErrorStream(true).start()
                val out = process.inputStream.bufferedReader().readText().trim()
                if (process.waitFor() == 0 && out.isNotBlank() && out != home.path) return File(out)
            }
        }
        return File(home, "Downloads")
    }
}
