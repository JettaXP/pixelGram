package com.pixelgram.desktop.core

import java.io.File
import java.util.Properties

object Backup {
    private const val Extension = "backup"
    private const val CodeLength = 4
    private val CodeChars = ('a'..'z') + ('0'..'9')

    fun isBackup(name: String) = name.endsWith(".$Extension", ignoreCase = true)

    fun export(settings: AppSettings): File? = runCatching {
        val dir = File(AppPaths.dataDir, "backups").apply { mkdirs() }
        val code = (1..CodeLength).map { CodeChars.random() }.joinToString("")
        File(dir, "pixelgram-$code.$Extension").also { file ->
            file.outputStream().use { settings.snapshot().store(it, "pixelGram settings backup") }
        }
    }.onFailure { AppLog.write("Backup export failed", it) }.getOrNull()

    fun import(settings: AppSettings, file: File): Boolean = runCatching {
        val props = Properties()
        file.inputStream().use(props::load)
        settings.restore(props)
    }.onFailure { AppLog.write("Backup import failed", it) }.getOrDefault(false)
}
