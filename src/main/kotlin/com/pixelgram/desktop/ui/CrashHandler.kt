package com.pixelgram.desktop.ui

import androidx.compose.ui.window.WindowExceptionHandler
import androidx.compose.ui.window.WindowExceptionHandlerFactory
import com.pixelgram.desktop.core.AppLog
import javax.swing.JOptionPane
import kotlin.system.exitProcess

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
object CrashHandler : WindowExceptionHandlerFactory {
    override fun exceptionHandler(window: java.awt.Window) = WindowExceptionHandler { error ->
        AppLog.write("Window error", error)
        if (error.isClipboardBusy()) return@WindowExceptionHandler
        runCatching {
            JOptionPane.showMessageDialog(
                window,
                "pixelGram ran into an error and has to close.\n${error.message ?: error.javaClass.simpleName}",
                "pixelGram",
                JOptionPane.ERROR_MESSAGE,
            )
        }
        exitProcess(1)
    }

    private fun Throwable.isClipboardBusy(): Boolean =
        generateSequence(this) { it.cause }.any { it.message?.contains("clipboard", ignoreCase = true) == true }
}
