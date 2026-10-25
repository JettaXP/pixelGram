package com.pixelgram.desktop.core

import com.sun.jna.Pointer
import java.awt.Desktop
import java.awt.EventQueue

/** Hooks the app into the standard macOS application menu and dock. */
object MacSystem {
    var quit: (() -> Unit)? = null
    var openSettings: (() -> Unit)? = null
    var openLink: ((String) -> Unit)? = null
    var openWindow: (() -> Unit)? = null

    fun install() {
        if (!AppPaths.isMac) return
        EventQueue.invokeLater {
            val desktop = runCatching { Desktop.getDesktop() }.getOrNull() ?: return@invokeLater
            runCatching { desktop.setAboutHandler { showAboutPanel() } }
                .onFailure { AppLog.write("macOS About handler unavailable", it) }
            runCatching {
                desktop.setPreferencesHandler {
                    val action = openSettings
                    if (action != null) EventQueue.invokeLater(action)
                }
            }.onFailure { AppLog.write("macOS Preferences handler unavailable", it) }
            runCatching {
                desktop.setQuitHandler { _, response ->
                    val action = quit
                    if (action == null) {
                        response.cancelQuit()
                    } else {
                        runCatching { action() }.onFailure { AppLog.write("Quit handler failed", it) }
                        response.performQuit()
                    }
                }
            }.onFailure { AppLog.write("macOS Quit handler unavailable", it) }
            runCatching {
                desktop.setOpenURIHandler { event ->
                    val link = event.uri.toString()
                    val action = openLink
                    if (action != null) {
                        EventQueue.invokeLater { action(link) }
                    } else {
                        openWindow?.let { show -> EventQueue.invokeLater(show) }
                    }
                }
            }.onFailure { AppLog.write("macOS URL handler unavailable", it) }
        }
    }

    fun showAboutPanel() {
        if (!AppPaths.isMac) return
        runCatching {
            val application = MacObjC.send(MacObjC.cls("NSApplication"), "sharedApplication")
            MacObjC.sendVoid(application, "orderFrontStandardAboutPanel:", Pointer.NULL)
        }.onFailure { AppLog.write("About panel failed", it) }
    }

}
