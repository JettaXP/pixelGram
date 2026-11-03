package com.pixelgram.desktop.core

import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer

object WindowsFrame {
    @Suppress("FunctionName")
    private interface User32 : Library {
        fun GetWindowLongPtrW(hwnd: Pointer, index: Int): Long
        fun SetWindowLongPtrW(hwnd: Pointer, index: Int, value: Long): Long
        fun SetWindowLongPtrW(hwnd: Pointer, index: Int, value: WndProc): Long
        fun CallWindowProcW(previous: Long, hwnd: Pointer, message: Int, wParam: Long, lParam: Long): Long
        fun SetWindowPos(hwnd: Pointer, after: Pointer?, x: Int, y: Int, cx: Int, cy: Int, flags: Int): Boolean
    }

    fun interface WndProc : Callback {
        fun callback(hwnd: Pointer, message: Int, wParam: Long, lParam: Long): Long
    }

    private const val GwlStyle = -16
    private const val GwlpWndProc = -4
    private const val WsCaption = 0x00C00000L
    private const val WsSysMenu = 0x00080000L
    private const val WsMinimizeBox = 0x00020000L
    private const val WmNcCalcSize = 0x0083
    private const val WmNcActivate = 0x0086
    private const val SwpFrameChanged = 0x0020
    private const val SwpKeep = 0x0001 or 0x0002 or 0x0004 or 0x0010

    private val user32: User32? by lazy {
        if (!AppPaths.isWindows) return@lazy null
        runCatching {
            System.setProperty("jna.tmpdir", AppPaths.nativesDir.absolutePath)
            Native.load("user32", User32::class.java)
        }.onFailure { AppLog.write("user32 is not available", it) }.getOrNull()
    }

    private val procs = HashMap<Long, WndProc>()

    fun nativeAnimations(windowHandle: Long): Boolean {
        if (windowHandle == 0L) return false
        if (procs.containsKey(windowHandle)) return true
        val user32 = user32 ?: return false
        return runCatching {
            val hwnd = Pointer(windowHandle)
            var previous = 0L
            val proc = WndProc { h, message, wParam, lParam ->
                when {
                    message == WmNcCalcSize && wParam != 0L -> 0L
                    message == WmNcActivate -> user32.CallWindowProcW(previous, h, message, wParam, -1L)
                    else -> user32.CallWindowProcW(previous, h, message, wParam, lParam)
                }
            }
            previous = user32.SetWindowLongPtrW(hwnd, GwlpWndProc, proc)
            if (previous == 0L) return@runCatching false
            procs[windowHandle] = proc
            val style = user32.GetWindowLongPtrW(hwnd, GwlStyle)
            user32.SetWindowLongPtrW(hwnd, GwlStyle, style or WsCaption or WsSysMenu or WsMinimizeBox)
            user32.SetWindowPos(hwnd, null, 0, 0, 0, 0, SwpFrameChanged or SwpKeep)
            true
        }.onFailure { AppLog.write("Native window animations failed", it) }.getOrDefault(false)
    }
}
