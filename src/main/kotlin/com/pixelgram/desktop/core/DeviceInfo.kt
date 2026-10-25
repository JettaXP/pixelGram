package com.pixelgram.desktop.core

import java.io.File
import java.net.InetAddress
import java.util.concurrent.TimeUnit

object DeviceInfo {
    private const val Fallback = "Desktop"
    private const val ReadTimeoutSeconds = 3L
    private val Junk = setOf("default string", "to be filled by o.e.m.", "system product name", "not applicable", "none", "")

    val model: String by lazy {
        runCatching { board() }.getOrNull()?.takeIf { it.lowercase() !in Junk }
            ?: runCatching { InetAddress.getLocalHost().hostName }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: Fallback
    }

    private fun board(): String? = when {
        AppPaths.isWindows -> {
            val process = ProcessBuilder("reg", "query", """HKLM\HARDWARE\DESCRIPTION\System\BIOS""", "/v", "BaseBoardProduct")
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor(ReadTimeoutSeconds, TimeUnit.SECONDS)
            Regex("""BaseBoardProduct\s+REG_SZ\s+(.+)""").find(output)?.groupValues?.get(1)?.trim()
        }
        AppPaths.isLinux -> listOf("board_name", "product_name")
            .map { File("/sys/devices/virtual/dmi/id/$it") }
            .firstNotNullOfOrNull { file -> file.takeIf { it.canRead() }?.readText()?.trim()?.takeIf { it.lowercase() !in Junk } }
        AppPaths.isMac -> {
            val process = ProcessBuilder("sysctl", "-n", "hw.model").redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor(ReadTimeoutSeconds, TimeUnit.SECONDS)
            output.trim().takeIf { it.isNotEmpty() && it.lowercase() !in Junk }
        }
        else -> null
    }
}
