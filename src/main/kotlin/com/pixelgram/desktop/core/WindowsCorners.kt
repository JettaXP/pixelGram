package com.pixelgram.desktop.core

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.IntByReference

object WindowsCorners {
    @Suppress("FunctionName")
    private interface Dwm : Library {
        fun DwmSetWindowAttribute(hwnd: Pointer, attribute: Int, value: IntByReference, size: Int): Int
    }

    private const val CornerPreference = 33
    private const val Round = 2
    private const val Square = 1

    private val dwm: Dwm? by lazy {
        if (!AppPaths.isWindows) return@lazy null
        runCatching {
            System.setProperty("jna.tmpdir", AppPaths.nativesDir.absolutePath)
            Native.load("dwmapi", Dwm::class.java)
        }.onFailure { AppLog.write("dwmapi is not available", it) }.getOrNull()
    }

    fun apply(windowHandle: Long, round: Boolean) {
        if (windowHandle == 0L) return
        val dwm = dwm ?: return
        runCatching {
            dwm.DwmSetWindowAttribute(Pointer(windowHandle), CornerPreference, IntByReference(if (round) Round else Square), 4)
        }
    }
}
