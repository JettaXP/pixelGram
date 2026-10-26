package com.pixelgram.desktop.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object FileSafety {
    var pending by mutableStateOf<String?>(null)

    private val Executable = setOf(
        "exe", "com", "bat", "cmd", "scr", "pif", "msi", "msp", "msc", "cpl", "dll", "sys", "jar", "js", "jse", "vbs", "vbe", "vb",
        "wsf", "wsh", "ws", "ps1", "psm1", "psd1", "hta", "lnk", "url", "reg", "inf", "scf", "sct", "shb", "shs", "chm", "application",
        "gadget", "appref-ms", "library-ms", "settingcontent-ms", "search-ms", "ade", "adp", "ins", "isp", "mst", "xll", "docm", "xlsm",
        "pptm", "iso", "img", "vhd", "vhdx", "sh", "bash", "zsh", "csh", "run", "bin", "desktop", "appimage", "deb", "rpm", "command",
        "app", "pkg", "dmg", "workflow", "scpt", "py", "pyw", "pl", "rb", "php", "apk",
    )

    private val Invisible = Regex("[\\u200E\\u200F\\u202A-\\u202E\\u2066-\\u2069\\u0000-\\u001F\\u007F]")

    fun extension(name: String): String = clean(name).substringAfterLast('.', "").lowercase()

    fun dangerous(name: String): Boolean = extension(name) in Executable

    fun clean(name: String): String = Invisible.replace(name, "")
}
