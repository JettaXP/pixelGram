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

    var uncleanStreak = 0
        private set

    fun start() {
        runCatching {
            if (file.isFile) {
                previous.delete()
                file.renameTo(previous)
            }
            File(AppPaths.dataDir, "pixelgram.log").takeIf { it.isFile }?.let { legacy -> move(legacy, File(AppPaths.logsDir, "old.log")) }
            File(AppPaths.dataDir, "tdlib.log").takeIf { it.isFile }?.delete()
            val crashed = running.isFile
            val dumped = collectDumps()
            if (dumped) running.delete() else reportUncleanExit()
            val streak = File(AppPaths.logsDir, "unclean-streak")
            uncleanStreak = if (crashed) (runCatching { streak.readText().trim().toInt() }.getOrDefault(0) + 1) else 0
            streak.writeText(uncleanStreak.toString())
            pruneCrashes()
            file.writeText("${LocalDateTime.now()} pixelGram ${BuildConfig.VERSION} ${BuildConfig.CODENAME} (${BuildConfig.COMMIT}) on ${System.getProperty("os.name")} ${System.getProperty("os.version")}, Java ${System.getProperty("java.version")}\n")
        }
        Thread.setDefaultUncaughtExceptionHandler { thread, error -> write("Uncaught error in ${thread.name}", error) }
        runCatching {
            writeMarker()
            Runtime.getRuntime().addShutdownHook(Thread { running.delete() })
        }
    }

    private val running: File get() = File(AppPaths.logsDir, "running.marker")
    private val trail = ArrayDeque<String>()

    @Synchronized
    fun trace(event: String) {
        trail.addLast("${LocalDateTime.now()} $event")
        while (trail.size > TrailLines) trail.removeFirst()
        runCatching { if (running.isFile) writeMarker() }
    }

    private fun writeMarker() = running.writeText((listOf(ProcessHandle.current().pid().toString()) + trail).joinToString("\n"))

    private fun reportUncleanExit() {
        val marker = running.takeIf { it.isFile } ?: return
        val time = Instant.ofEpochMilli(previous.takeIf { it.isFile }?.lastModified() ?: marker.lastModified())
            .atZone(ZoneId.systemDefault()).format(stamp)
        val recent = previous.takeIf { it.isFile }?.readLines()?.takeLast(CrashContextLines)?.joinToString("\n").orEmpty()
        val tdlib = File(AppPaths.logsDir, "tdlib.log").takeIf { it.isFile }?.let(::tail).orEmpty()
        val actions = runCatching { marker.readLines().drop(1).joinToString("\n") }.getOrDefault("")
        File(AppPaths.logsDir, "crash-$time.log").writeText(
            "pixelGram ${BuildConfig.VERSION} ${BuildConfig.CODENAME} (${BuildConfig.COMMIT})\n\n" +
                "The previous session ended unexpectedly, without a Java error. This usually means a native crash or the process being killed.\n\n" +
                "Last actions:\n$actions\n\nRecent log:\n$recent\n\nRecent TDLib log:\n$tdlib\n",
        )
        marker.delete()
    }

    private fun tail(file: File): String = runCatching {
        java.io.RandomAccessFile(file, "r").use { raf ->
            val start = (raf.length() - TailBytes).coerceAtLeast(0)
            raf.seek(start)
            val bytes = ByteArray((raf.length() - start).toInt())
            raf.readFully(bytes)
            bytes.decodeToString()
        }
    }.getOrDefault("")

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

    private fun collectDumps(): Boolean {
        var moved = 0
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
                moved++
            }
        }
        return moved > 0
    }

    private fun isOurs(file: File): Boolean = runCatching {
        file.bufferedReader().use { reader -> generateSequence { reader.readLine() }.take(DumpScanLines).any { "com.pixelgram.desktop" in it || "pixelGram" in it || "pixelgram" in it } }
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
    private const val DumpScanLines = 20_000
    private const val TailBytes = 64L * 1024
    private const val KeptCrashes = 20
    private const val TrailLines = 30
}

fun Throwable.describe(): String = generateSequence(this) { it.cause }
    .mapNotNull { it.message?.takeIf(String::isNotBlank) ?: it.javaClass.simpleName }
    .distinct()
    .joinToString(" → ")
