package com.pixelgram.setup

import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.WString
import java.io.File

sealed interface InstallResult {
    data object Done : InstallResult
    data object Cancelled : InstallResult
    data class Failed(val message: String, val log: File?) : InstallResult
}

interface InstallListener {
    fun progress(fraction: Float)
    fun action(text: String)
}

interface Installer {
    fun install(msi: File, target: File, listener: InstallListener, cancelled: () -> Boolean): InstallResult
}

object DemoInstaller : Installer {
    private val steps = listOf("Preparing", "Removing the previous version", "Copying new files", "Creating shortcuts", "Finishing")

    override fun install(msi: File, target: File, listener: InstallListener, cancelled: () -> Boolean): InstallResult {
        val total = DemoSteps
        for (step in 0..total) {
            if (cancelled()) return InstallResult.Cancelled
            listener.action(steps[(step * steps.size / (total + 1)).coerceAtMost(steps.lastIndex)])
            listener.progress(step.toFloat() / total)
            Thread.sleep(DemoStepMs)
        }
        return InstallResult.Done
    }

    private const val DemoSteps = 120
    private const val DemoStepMs = 40L
}

private interface MsiLibrary : Library {
    fun MsiSetInternalUI(level: Int, window: Pointer?): Int
    fun MsiSetExternalUIW(handler: MsiUiHandler, filter: Int, context: Pointer?): Pointer?
    fun MsiEnableLogW(mode: Int, path: WString, attributes: Int): Int
    fun MsiInstallProductW(packagePath: WString, commandLine: WString): Int
}

private fun interface MsiUiHandler : Callback {
    fun invoke(context: Pointer?, type: Int, message: Pointer?): Int
}

object WindowsInstaller : Installer {
    private const val UiLevelNone = 2
    private const val LogAll = 0x3FFF
    private const val LogAppend = 1
    private const val Success = 0
    private const val RebootRequired = 3010
    private const val UserCancelled = 1602
    private const val IdOk = 1
    private const val IdCancel = 2
    private const val TypeMask = 0xFF000000.toInt()
    private const val MessageFatal = 0x00000000
    private const val MessageError = 0x01000000
    private const val MessageActionStart = 0x08000000
    private const val MessageActionData = 0x09000000
    private const val MessageProgress = 0x0A000000
    private const val FilterFatal = 1
    private const val FilterError = 1 shl 1
    private const val FilterActionStart = 1 shl 8
    private const val FilterActionData = 1 shl 9
    private const val FilterProgress = 1 shl 10
    private const val PreparationShare = 0.12f

    override fun install(msi: File, target: File, listener: InstallListener, cancelled: () -> Boolean): InstallResult {
        val lib = runCatching { Native.load("msi", MsiLibrary::class.java) }.getOrElse {
            return InstallResult.Failed("Windows Installer is not available: ${it.message}", null)
        }
        val log = File(System.getProperty("java.io.tmpdir"), "pixelGram-setup.log").apply { delete() }
        val progress = MsiProgress(listener)
        var lastError: String? = null
        val handler = MsiUiHandler { _, type, message ->
            val text = message?.getWideString(0).orEmpty()
            when (type and TypeMask) {
                MessageProgress -> progress.onProgress(text)
                MessageActionData -> progress.onActionData()
                MessageActionStart -> actionTitle(text)?.let(listener::action)
                MessageError, MessageFatal -> lastError = text.ifBlank { lastError }
            }
            if (cancelled()) IdCancel else if (type and TypeMask == MessageError) 0 else IdOk
        }
        lib.MsiSetInternalUI(UiLevelNone, null)
        lib.MsiSetExternalUIW(handler, FilterFatal or FilterError or FilterActionStart or FilterActionData or FilterProgress, null)
        lib.MsiEnableLogW(LogAll, WString(log.absolutePath), LogAppend)
        val arguments = "INSTALLDIR=\"${target.absolutePath}\" REBOOT=ReallySuppress MSIFASTINSTALL=1"
        val code = lib.MsiInstallProductW(WString(msi.absolutePath), WString(arguments))
        java.lang.ref.Reference.reachabilityFence(handler)
        return when (code) {
            Success, RebootRequired -> {
                listener.progress(1f)
                InstallResult.Done
            }
            UserCancelled -> InstallResult.Cancelled
            else -> InstallResult.Failed(lastError ?: "Windows Installer error $code", log)
        }
    }

    private fun actionTitle(text: String): String? {
        val parts = text.substringAfter(": ", "").split(". ")
        return parts.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }
    }

    private class MsiProgress(private val listener: InstallListener) {
        private var total = 0
        private var position = 0
        private var forward = true
        private var executing = false
        private var perTick = 0
        private var useActionData = false
        private var shown = 0f

        fun onProgress(text: String) {
            val fields = Regex("""(\d+):\s*(-?\d+)""").findAll(text).associate { it.groupValues[1].toInt() to it.groupValues[2].toInt() }
            when (fields[1]) {
                0 -> {
                    total = fields[2] ?: 0
                    forward = (fields[3] ?: 0) == 0
                    position = if (forward) 0 else total
                    executing = (fields[4] ?: 0) == 1
                }
                1 -> {
                    useActionData = (fields[3] ?: 0) != 0
                    perTick = if (useActionData) fields[2] ?: 0 else 0
                }
                2 -> if (total != 0) position += if (forward) fields[2] ?: 0 else -(fields[2] ?: 0)
                3 -> total += fields[2] ?: 0
            }
            report()
        }

        fun onActionData() {
            if (!useActionData || perTick == 0) return
            position += if (forward) perTick else -perTick
            report()
        }

        private fun report() {
            if (total <= 0) return
            val local = (position.toFloat() / total).coerceIn(0f, 1f)
            val overall = if (executing) PreparationShare + local * (1f - PreparationShare) else local * PreparationShare
            if (overall > shown) {
                shown = overall
                listener.progress(shown)
            }
        }
    }
}
