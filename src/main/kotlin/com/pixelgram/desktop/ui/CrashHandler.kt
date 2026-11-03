package com.pixelgram.desktop.ui

import androidx.compose.ui.window.WindowExceptionHandler
import androidx.compose.ui.window.WindowExceptionHandlerFactory
import com.pixelgram.desktop.core.AppLog
import javax.swing.JOptionPane
import kotlin.system.exitProcess

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
object CrashHandler : WindowExceptionHandlerFactory {
    override fun exceptionHandler(window: java.awt.Window) = WindowExceptionHandler { error ->
        if (error.isClipboardBusy()) {
            AppLog.write("Window error", error)
            return@WindowExceptionHandler
        }
        AppLog.crash("pixelGram crashed", error)
        runCatching {
            JOptionPane.showMessageDialog(
                window,
                "pixelGram ran into an error and has to close.\n${error.rootSummary()}\n\nLog: ${AppLog.file.parentFile?.absolutePath.orEmpty()}",
                "pixelGram",
                JOptionPane.ERROR_MESSAGE,
            )
        }
        exitProcess(1)
    }

    private fun Throwable.rootSummary(): String {
        val root = generateSequence(this) { it.cause }.last()
        val where = root.stackTrace.firstOrNull { it.className.startsWith("com.pixelgram") } ?: root.stackTrace.firstOrNull()
        return listOfNotNull(
            "${root.javaClass.simpleName}: ${root.message.orEmpty()}".take(SummaryLimit),
            where?.let { "at ${it.className.substringAfterLast('.')}.${it.methodName}(${it.fileName}:${it.lineNumber})" },
        ).joinToString("\n")
    }

    private const val SummaryLimit = 300

    private fun Throwable.isClipboardBusy(): Boolean =
        generateSequence(this) { it.cause }.any { it.message?.contains("clipboard", ignoreCase = true) == true }
}
