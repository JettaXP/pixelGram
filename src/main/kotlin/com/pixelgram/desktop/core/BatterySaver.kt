package com.pixelgram.desktop.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeUnit

object BatterySaver {
    var active by mutableStateOf(false)
        private set
    private var started = false
    private val os = System.getProperty("os.name").orEmpty().lowercase()

    val saving: Boolean get() = active && PowerSaving.settings?.saveOnLowBattery?.value != false

    fun start() {
        if (started) return
        started = true
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            while (true) {
                val now = runCatching { detect() }.getOrDefault(false)
                if (now != active) launch(Dispatchers.Main) { active = now }
                delay(PollMs)
            }
        }
    }

    private fun detect(): Boolean = when {
        os.contains("windows") -> windows()
        os.contains("mac") -> run("pmset", "-g")?.lineSequence()?.any { it.trim().replace(Regex("\\s+"), " ") == "lowpowermode 1" } == true
        else -> linux()
    }

    private fun windows(): Boolean {
        val status = com.sun.jna.Memory(12)
        val ok = com.sun.jna.NativeLibrary.getInstance("kernel32").getFunction("GetSystemPowerStatus").invokeInt(arrayOf(status)) != 0
        return ok && status.getByte(3).toInt() == 1
    }

    private fun linux(): Boolean {
        run("powerprofilesctl", "get")?.trim()?.let { return it == "power-saver" }
        val profile = File("/sys/firmware/acpi/platform_profile").takeIf { it.canRead() }?.readText()?.trim()
        return profile == "low-power"
    }

    private fun run(vararg command: String): String? = runCatching {
        val process = ProcessBuilder(*command).redirectErrorStream(true).start()
        if (!process.waitFor(3, TimeUnit.SECONDS)) {
            process.destroy()
            return null
        }
        process.inputStream.bufferedReader().readText().takeIf { process.exitValue() == 0 }
    }.getOrNull()

    private const val PollMs = 20_000L
}
