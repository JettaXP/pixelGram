package com.pixelgram.desktop.core

import androidx.compose.ui.awt.ComposeWindow
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import java.awt.Window
import javax.swing.SwingUtilities
import javax.swing.Timer

object ScreenProtection {
    @Suppress("FunctionName")
    private interface User32 : Library {
        fun SetWindowDisplayAffinity(hwnd: Pointer, affinity: Int): Boolean
    }

    private const val AffinityNone = 0
    private const val AffinityMonitor = 1
    private const val AffinityExclude = 0x11
    private const val SharingNone = 0L
    private const val SharingReadOnly = 1L
    private const val RefreshMs = 1000

    private var holders = 0
    private val protectedHandles = mutableSetOf<Long>()
    private val timer = Timer(RefreshMs) { apply(true) }

    private val user32: User32? by lazy {
        if (!AppPaths.isWindows) return@lazy null
        runCatching {
            System.setProperty("jna.tmpdir", AppPaths.nativesDir.absolutePath)
            Native.load("user32", User32::class.java)
        }.onFailure { AppLog.write("user32 is not available", it) }.getOrNull()
    }

    fun acquire() = SwingUtilities.invokeLater {
        holders++
        if (holders == 1) {
            apply(true)
            timer.start()
        }
    }

    fun release() = SwingUtilities.invokeLater {
        if (holders == 0) return@invokeLater
        holders--
        if (holders == 0) {
            timer.stop()
            apply(false)
        }
    }

    private fun apply(on: Boolean) {
        runCatching {
            when {
                AppPaths.isWindows -> windows(on)
                AppPaths.isMac -> mac(on)
            }
        }.onFailure { AppLog.write("Screen protection failed", it) }
    }

    private fun windows(on: Boolean) {
        val user32 = user32 ?: return
        val handles = Window.getWindows().filter { it.isDisplayable }.mapNotNull { (it as? ComposeWindow)?.windowHandle?.takeIf { handle -> handle != 0L } }
        if (on) {
            handles.filter { it !in protectedHandles }.forEach { handle ->
                val pointer = Pointer(handle)
                if (!user32.SetWindowDisplayAffinity(pointer, AffinityExclude)) user32.SetWindowDisplayAffinity(pointer, AffinityMonitor)
                protectedHandles.add(handle)
            }
        } else {
            protectedHandles.filter { it in handles }.forEach { user32.SetWindowDisplayAffinity(Pointer(it), AffinityNone) }
            protectedHandles.clear()
        }
    }

    private fun mac(on: Boolean) {
        val app = MacObjC.send(MacObjC.cls("NSApplication"), "sharedApplication")
        val windows = MacObjC.send(app, "windows")
        val count = Pointer.nativeValue(MacObjC.send(windows, "count"))
        for (index in 0 until count) {
            val window = MacObjC.send(windows, "objectAtIndex:", Pointer(index))
            MacObjC.sendVoid(window, "setSharingType:", Pointer(if (on) SharingNone else SharingReadOnly))
        }
    }
}
