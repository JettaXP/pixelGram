package com.pixelgram.desktop.core

import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.LocalDateTime

// Simple app log in the data folder, for problems users can send us.
object AppLog {
    val file: File get() = File(AppPaths.dataDir, "pixelgram.log")

    @Synchronized
    fun write(message: String, error: Throwable? = null) {
        val text = buildString {
            append(LocalDateTime.now()).append(' ').append(message).append('\n')
            if (error != null) {
                val trace = StringWriter()
                error.printStackTrace(PrintWriter(trace))
                append(trace)
            }
        }
        System.err.print(text)
        runCatching { file.appendText(text) }
    }
}

// "Message (cause: ...)" for every cause in the chain.
fun Throwable.describe(): String = generateSequence(this) { it.cause }
    .mapNotNull { it.message?.takeIf(String::isNotBlank) ?: it.javaClass.simpleName }
    .distinct()
    .joinToString(" → ")
