package com.pixelgram.desktop.core

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.WString
import java.io.File

@Structure.FieldOrder(
    "cbSize", "fMask", "hwnd", "lpVerb", "lpFile", "lpParameters", "lpDirectory", "nShow",
    "hInstApp", "lpIDList", "lpClass", "hkeyClass", "dwHotKey", "hIconOrMonitor", "hProcess",
)
class ShellExecuteInfo : Structure() {
    @JvmField var cbSize: Int = 0
    @JvmField var fMask: Int = 0
    @JvmField var hwnd: Pointer? = null
    @JvmField var lpVerb: WString? = null
    @JvmField var lpFile: WString? = null
    @JvmField var lpParameters: WString? = null
    @JvmField var lpDirectory: WString? = null
    @JvmField var nShow: Int = 0
    @JvmField var hInstApp: Pointer? = null
    @JvmField var lpIDList: Pointer? = null
    @JvmField var lpClass: WString? = null
    @JvmField var hkeyClass: Pointer? = null
    @JvmField var dwHotKey: Int = 0
    @JvmField var hIconOrMonitor: Pointer? = null
    @JvmField var hProcess: Pointer? = null
}

object SystemShare {
    private val windows = System.getProperty("os.name").orEmpty().lowercase().contains("windows")

    fun share(file: File, reveal: (String) -> Unit) {
        if (windows && shareOnWindows(file)) return
        reveal(file.absolutePath)
    }

    private fun shareOnWindows(file: File): Boolean = runCatching {
        val shell = com.sun.jna.NativeLibrary.getInstance("shell32", com.sun.jna.win32.W32APIOptions.UNICODE_OPTIONS)
        val info = ShellExecuteInfo().apply {
            fMask = InvokeIdList or NoUi
            lpVerb = WString(ShareVerb)
            lpFile = WString(file.absolutePath)
            nShow = ShowNormal
        }
        info.cbSize = info.size()
        info.write()
        val ok = shell.getFunction("ShellExecuteExW").invokeInt(arrayOf(info.pointer)) != 0
        if (!ok) AppLog.write("System share failed: ${Native.getLastError()}")
        ok
    }.onFailure { AppLog.write("System share failed", it) }.getOrDefault(false)

    private const val ShareVerb = "Windows.ModernShare"
    private const val InvokeIdList = 0x0000000C
    private const val NoUi = 0x00000400
    private const val ShowNormal = 1
}
