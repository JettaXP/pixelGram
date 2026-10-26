package com.pixelgram.desktop.core

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.ptr.IntByReference
import java.awt.Rectangle

object WindowsCorners {
    @Suppress("FunctionName")
    private interface Dwm : Library {
        fun DwmSetWindowAttribute(hwnd: Pointer, attribute: Int, value: IntByReference, size: Int): Int
    }

    @Suppress("FunctionName")
    private interface User32 : Library {
        fun GetWindowRect(hwnd: Pointer, rect: Rect): Boolean
        fun MonitorFromWindow(hwnd: Pointer, flags: Int): Pointer?
        fun GetMonitorInfoW(monitor: Pointer, info: MonitorInfo): Boolean
        fun SetWindowPos(hwnd: Pointer, after: Pointer?, x: Int, y: Int, cx: Int, cy: Int, flags: Int): Boolean
    }

    @Structure.FieldOrder("left", "top", "right", "bottom")
    class Rect : Structure() {
        @JvmField var left = 0
        @JvmField var top = 0
        @JvmField var right = 0
        @JvmField var bottom = 0

        fun toRectangle() = Rectangle(left, top, right - left, bottom - top)
    }

    @Structure.FieldOrder("cbSize", "rcMonitor", "rcWork", "dwFlags")
    class MonitorInfo : Structure() {
        @JvmField var cbSize = size()
        @JvmField var rcMonitor = Rect()
        @JvmField var rcWork = Rect()
        @JvmField var dwFlags = 0
    }

    private const val CornerPreference = 33
    private const val Round = 2
    private const val Square = 1
    private const val MonitorDefaultToNearest = 2
    private const val NoZOrderNoActivate = 0x0004 or 0x0010

    private val dwm: Dwm? by lazy { load("dwmapi", Dwm::class.java) }
    private val user32: User32? by lazy { load("user32", User32::class.java) }

    private fun <T : Library> load(name: String, type: Class<T>): T? {
        if (!AppPaths.isWindows) return null
        return runCatching {
            System.setProperty("jna.tmpdir", AppPaths.nativesDir.absolutePath)
            Native.load(name, type)
        }.onFailure { AppLog.write("$name is not available", it) }.getOrNull()
    }

    fun apply(windowHandle: Long, round: Boolean): Boolean {
        if (windowHandle == 0L) return false
        val dwm = dwm ?: return false
        return runCatching {
            dwm.DwmSetWindowAttribute(Pointer(windowHandle), CornerPreference, IntByReference(if (round) Round else Square), 4) == 0
        }.getOrDefault(false)
    }

    fun windowRect(windowHandle: Long): Rectangle? {
        if (windowHandle == 0L) return null
        val user32 = user32 ?: return null
        val rect = Rect()
        return rect.takeIf { runCatching { user32.GetWindowRect(Pointer(windowHandle), it) }.getOrDefault(false) }
            ?.toRectangle()
    }

    fun workArea(windowHandle: Long): Rectangle? {
        if (windowHandle == 0L) return null
        val user32 = user32 ?: return null
        return runCatching {
            val monitor = user32.MonitorFromWindow(Pointer(windowHandle), MonitorDefaultToNearest) ?: return null
            val info = MonitorInfo()
            if (user32.GetMonitorInfoW(monitor, info)) info.rcWork.toRectangle() else null
        }.getOrNull()
    }

    fun setRect(windowHandle: Long, rect: Rectangle) {
        if (windowHandle == 0L) return
        val user32 = user32 ?: return
        runCatching {
            user32.SetWindowPos(Pointer(windowHandle), null, rect.x, rect.y, rect.width, rect.height, NoZOrderNoActivate)
        }
    }
}
