package com.pixelgram.desktop.core

import it.tdlight.jni.TdApi
import java.io.File
import java.util.concurrent.TimeUnit

object PythonRunner {
    class Result(val output: String, val failed: Boolean)

    private val Command = Regex("""^\.py\s+"(.*)"$""", RegexOption.DOT_MATCHES_ALL)

    val packagesDir: File get() = File(AppPaths.dataDir, "python-packages")

    fun code(text: String): String? = Command.matchEntire(text.trim())?.groupValues?.get(1)?.trim('\n')?.takeIf { it.isNotBlank() }

    private val candidates: List<List<String>>
        get() = if (AppPaths.isWindows) listOf(listOf("py", "-3"), listOf("python"), listOf("python3")) else listOf(listOf("python3"), listOf("python"))

    @Volatile private var found: List<String>? = null

    fun interpreter(): List<String>? {
        found?.let { return it }
        val candidate = candidates.firstOrNull { command ->
            runCatching { exec(command + "--version", VersionTimeoutMs).let { !it.failed && it.output.trim().startsWith("Python 3") } }.getOrDefault(false)
        }
        found = candidate
        return candidate
    }

    fun version(): String? = interpreter()?.let { exec(it + "--version", VersionTimeoutMs).output.trim().takeIf(String::isNotBlank) }

    fun run(code: String): Result {
        val python = interpreter() ?: return Result("Python is not installed.", true)
        val dir = File(System.getProperty("java.io.tmpdir"), "pixelgram-python").apply { mkdirs() }
        val script = File.createTempFile("message", ".py", dir).apply { writeText(code) }
        val runner = File(dir, "runner.py").apply { writeText(Runner) }
        packagesDir.mkdirs()
        return try {
            exec(python + listOf("-X", "utf8", runner.absolutePath, packagesDir.absolutePath, script.absolutePath), RunTimeoutMs)
        } finally {
            script.delete()
        }
    }

    fun install(name: String): Result {
        val python = interpreter() ?: return Result("Python is not installed.", true)
        packagesDir.mkdirs()
        return exec(python + listOf("-X", "utf8", "-m", "pip", "install", "--disable-pip-version-check", "--upgrade", "--target", packagesDir.absolutePath, name), InstallTimeoutMs)
    }

    fun message(code: String, result: Result): Pair<String, Array<TdApi.TextEntity>> {
        val room = MessageLimit - code.length - 1
        val output = result.output.trimEnd().ifEmpty { "(no output)" }.let { if (room > 0 && it.length > room) it.take(room - 1) + "…" else it }
        val body = code.take(MessageLimit - output.length - 1)
        val text = "$output\n$body"
        return text to arrayOf(
            TdApi.TextEntity(0, output.length, TdApi.TextEntityTypeBlockQuote()),
            TdApi.TextEntity(output.length + 1, body.length, TdApi.TextEntityTypeBlockQuote()),
        )
    }

    private fun exec(command: List<String>, timeoutMs: Long): Result {
        val process = ProcessBuilder(command).redirectErrorStream(true).apply { environment()["PYTHONIOENCODING"] = "utf-8" }.start()
        val output = StringBuilder()
        val reader = Thread {
            runCatching {
                process.inputStream.bufferedReader(Charsets.UTF_8).use { input ->
                    val buffer = CharArray(BufferSize)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        synchronized(output) { if (output.length < OutputLimit) output.appendRange(buffer, 0, read) }
                    }
                }
            }
        }.apply { isDaemon = true; start() }
        val finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
        if (!finished) process.destroyForcibly()
        reader.join(ReaderJoinMs)
        val text = synchronized(output) { output.toString() }
        return if (finished) Result(text, process.exitValue() != 0) else Result(text + "\nStopped after ${timeoutMs / 1000} s.", true)
    }

    private val Runner = """
        import sys, traceback
        sys.path.insert(0, sys.argv[1])
        with open(sys.argv[2], encoding="utf-8") as file:
            source = file.read()
        sys.argv = ["message.py"]
        try:
            exec(compile(source, "message.py", "exec"), {"__name__": "__main__"})
        except SystemExit:
            raise
        except BaseException as error:
            traceback.print_exception(type(error), error, error.__traceback__.tb_next)
            sys.exit(1)
    """.trimIndent()

    private const val RunTimeoutMs = 15_000L
    private const val InstallTimeoutMs = 180_000L
    private const val VersionTimeoutMs = 5_000L
    private const val ReaderJoinMs = 1_000L
    private const val OutputLimit = 16_384
    private const val BufferSize = 4_096
    private const val MessageLimit = 4_096
}
