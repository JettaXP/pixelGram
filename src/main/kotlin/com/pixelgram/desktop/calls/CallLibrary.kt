package com.pixelgram.desktop.calls

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pixelgram.desktop.core.AppLog
import com.pixelgram.desktop.core.AppPaths
import com.sun.jna.Native
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.security.MessageDigest
import java.util.zip.ZipInputStream

object CallLibrary {
    private const val Version = "3.0.5"
    private const val Base = "https://github.com/pytgcalls/ntgcalls/releases/download/v$Version"
    private const val TimeoutMs = 30_000
    private const val BufferSize = 64 * 1024

    private class Build(val asset: String, val sha256: String, val entry: String)

    private val build: Build? by lazy {
        val arch = System.getProperty("os.arch").lowercase()
        val arm = arch == "aarch64" || arch == "arm64"
        when {
            AppPaths.isWindows && !arm -> Build(
                "windows-x86_64",
                "880e4a0c0123b7d92a60649097f5846c84f85830ac6e608020387aeb26e71d07",
                "lib/Release/ntgcalls.dll",
            )
            AppPaths.isMac && arm -> Build(
                "macos-arm64",
                "a0e0cbb56a0161f7aba49e03da5bdb7454fbe30f229f4cd7403107ba91d17e5d",
                "lib/libntgcalls.dylib",
            )
            AppPaths.isLinux && !arm -> Build(
                "linux-x86_64",
                "a6430d968db2b5c273784ede0b8c32f12a55ba89151d3d217c9396397ea1beb3",
                "lib/libntgcalls.so",
            )
            else -> null
        }
    }

    val supported: Boolean get() = build != null

    var progress by mutableFloatStateOf(0f)
        private set
    var downloading by mutableStateOf(false)
        private set

    private var loaded: NtgLibrary? = null
    private val dir get() = File(AppPaths.nativesDir, "ntgcalls-$Version")

    private val file: File? get() = build?.let { File(dir, it.entry.substringAfterLast('/')) }

    val installed: Boolean get() = file?.isFile == true

    @Synchronized
    fun load(): NtgLibrary? {
        loaded?.let { return it }
        val target = file ?: return null
        if (!target.isFile) download() ?: return null
        return runCatching { Native.load(target.absolutePath, NtgLibrary::class.java) }
            .onFailure { AppLog.write("Call library failed to load", it) }
            .getOrNull()
            ?.also { loaded = it }
    }

    private fun download(): File? {
        val info = build ?: return null
        val target = file ?: return null
        downloading = true
        progress = 0f
        return try {
            dir.mkdirs()
            val zip = File(dir, "download.zip")
            val connection = URI("$Base/ntgcalls.${info.asset}-shared_libs.zip").toURL().openConnection() as HttpURLConnection
            connection.connectTimeout = TimeoutMs
            connection.readTimeout = TimeoutMs
            connection.instanceFollowRedirects = true
            if (connection.responseCode != HttpURLConnection.HTTP_OK) error("HTTP ${connection.responseCode}")
            val total = connection.contentLengthLong
            val digest = MessageDigest.getInstance("SHA-256")
            connection.inputStream.use { input ->
                zip.outputStream().use { output ->
                    val buffer = ByteArray(BufferSize)
                    var done = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        done += read
                        if (total > 0) progress = done.toFloat() / total
                    }
                }
            }
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            if (hash != info.sha256) {
                zip.delete()
                error("Checksum mismatch")
            }
            val part = File(dir, target.name + ".part")
            ZipInputStream(zip.inputStream().buffered()).use { stream ->
                while (true) {
                    val entry = stream.nextEntry ?: break
                    if (entry.name == info.entry) {
                        part.outputStream().use { stream.copyTo(it) }
                        break
                    }
                }
            }
            zip.delete()
            if (!part.isFile || !part.renameTo(target)) error("Library not found in the archive")
            target
        } catch (error: Exception) {
            AppLog.write("Call library download failed", error)
            null
        } finally {
            downloading = false
        }
    }
}
