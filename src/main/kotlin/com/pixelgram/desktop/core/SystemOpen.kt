package com.pixelgram.desktop.core

import java.awt.Desktop
import java.io.File
import java.net.URI

object SystemOpen {
    private val os = System.getProperty("os.name").lowercase()
    private val windows = os.contains("win")
    private val mac = os.contains("mac")

    fun file(file: File) {
        val first = runCatching { Desktop.getDesktop().open(file) }.exceptionOrNull() ?: return
        val fallback = when {
            windows -> listOf("rundll32.exe", "shell32.dll,OpenAs_RunDLL", file.absolutePath)
            mac -> listOf("open", file.absolutePath)
            else -> listOf("xdg-open", file.absolutePath)
        }
        runCatching { ProcessBuilder(fallback).start() }.onFailure { it.addSuppressed(first); throw it }
    }

    fun link(url: String) {
        val first = runCatching { Desktop.getDesktop().browse(URI(url)) }.exceptionOrNull() ?: return
        val fallback = when {
            windows -> listOf("rundll32.exe", "url.dll,FileProtocolHandler", url)
            mac -> listOf("open", url)
            else -> listOf("xdg-open", url)
        }
        runCatching { ProcessBuilder(fallback).start() }.onFailure { it.addSuppressed(first); throw it }
    }
}
