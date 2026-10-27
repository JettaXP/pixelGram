package com.pixelgram.desktop.core

import com.pixelgram.desktop.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object AppLog {
    val file: File get() = File(AppPaths.logsDir, "startup.log")

    private val previous: File get() = File(AppPaths.logsDir, "startup.previous.log")
    private val stamp = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
    private val dump = Regex("""(hs_err_pid|crash-)\d+\.log""")

    fun start() {
        runCatching {
            if (file.isFile) {
                previous.delete()
                file.renameTo(previous)
            }
            File(AppPaths.dataDir, "pixelgram.log").takeIf { it.isFile }?.let { legacy -> move(legacy, File(AppPaths.logsDir, "old.log")) }
            File(AppPaths.dataDir, "tdlib.log").takeIf { it.isFile }?.delete()
            collectDumps()
            pruneCrashes()
            file.writeText("${LocalDateTime.now()} pixelGram ${BuildConfig.VERSION} ${BuildConfig.CODENAME} (${BuildConfig.COMMIT}) on ${System.getProperty("os.name")} ${System.getProperty("os.version")}, Java ${System.getProperty("java.version")}\n")
        }
        Thread.setDefaultUncaughtExceptionHandler { thread, error -> write("Uncaught error in ${thread.name}", error) }
    }

    @Synchronized
    fun write(message: String, error: Throwable? = null) {
        val text = entry(message, error)
        System.err.print(text)
        runCatching { file.appendText(text) }
    }

    @Synchronized
    fun crash(message: String, error: Throwable) {
        val text = entry(message, error)
        System.err.print(text)
        runCatching { file.appendText(text) }
        runCatching {
            val recent = file.takeIf { it.isFile }?.readLines()?.takeLast(CrashContextLines)?.joinToString("\n").orEmpty()
            File(AppPaths.logsDir, "crash-${LocalDateTime.now().format(stamp)}.log")
                .writeText("pixelGram ${BuildConfig.VERSION} ${BuildConfig.CODENAME} (${BuildConfig.COMMIT})\n\n$text\nRecent log:\n$recent\n")
        }
    }

    private fun entry(message: String, error: Throwable?) = buildString {
        append(LocalDateTime.now()).append(' ').append(message).append('\n')
        if (error != null) {
            val trace = StringWriter()
            error.printStackTrace(PrintWriter(trace))
            append(trace)
        }
    }

    private fun collectDumps() {
        val places = listOfNotNull(
            File(System.getProperty("user.dir")),
            AppPaths.appDir,
            AppPaths.appDir?.let { File(it, "app") },
            AppPaths.dataDir,
            AppPaths.logsDir,
            File(System.getProperty("java.io.tmpdir")),
        ).distinct()
        places.forEach { dir ->
            dir.listFiles { candidate -> candidate.isFile && dump.matches(candidate.name) }?.forEach { found ->
                if (!isOurs(found)) return@forEach
                val time = Instant.ofEpochMilli(found.lastModified()).atZone(ZoneId.systemDefault()).format(stamp)
                var target = File(AppPaths.logsDir, "crash-$time.log")
                var index = 2
                while (target.exists()) target = File(AppPaths.logsDir, "crash-$time-${index++}.log")
                move(found, target)
            }
        }
    }

    private fun isOurs(file: File): Boolean = runCatching {
        file.bufferedReader().use { reader -> generateSequence { reader.readLine() }.take(DumpHeaderLines).any { "com.pixelgram.desktop" in it } }
    }.getOrDefault(false)

    private fun pruneCrashes() {
        AppPaths.logsDir.listFiles { candidate -> candidate.isFile && candidate.name.startsWith("crash-") }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(KeptCrashes)
            ?.forEach { it.delete() }
    }

    private fun move(from: File, to: File) {
        if (from.renameTo(to)) return
        runCatching {
            from.copyTo(to, overwrite = true)
            from.delete()
        }
    }

    private const val CrashContextLines = 200
    private const val DumpHeaderLines = 80
    private const val KeptCrashes = 20
}

fun Throwable.describe(): String = generateSequence(this) { it.cause }
    .mapNotNull { it.message?.takeIf(String::isNotBlank) ?: it.javaClass.simpleName }
    .distinct()
    .joinToString(" → ")
