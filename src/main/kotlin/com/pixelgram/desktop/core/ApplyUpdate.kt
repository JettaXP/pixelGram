package com.pixelgram.desktop.core

import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile

object ApplyUpdate {
    const val Flag = "--apply-update"

    class Job(
        val pid: Long,
        val archive: File,
        val target: File,
        val exe: File,
        val marker: File,
        val dataDir: File,
    )

    fun parse(args: Array<String>): Job? {
        if (args.firstOrNull() != Flag || args.size != JobArgs + 1) return null
        val pid = args[1].toLongOrNull() ?: return null
        val files = args.drop(2).map { File(it).absoluteFile }
        return Job(pid, files[0], files[1], files[2], files[3], files[4])
    }

    fun arguments(job: Job): List<String> = listOf(
        Flag,
        job.pid.toString(),
        job.archive.absolutePath,
        job.target.absolutePath,
        job.exe.absolutePath,
        job.marker.absolutePath,
        job.dataDir.absolutePath,
    )

    fun readyFile(archive: File): File = File(archive.parentFile, "updater.ready")

    fun signalReady(job: Job) {
        runCatching { readyFile(job.archive).writeText(ProcessHandle.current().pid().toString()) }
    }

    fun run(job: Job) {
        runCatching { apply(job) }.onFailure { error ->
            AppLog.write("Update failed", error)
            runCatching {
                File(job.dataDir, "update-error.log").appendText((error.message ?: error.javaClass.simpleName).replace('\n', ' ') + "\n")
            }
        }
        runCatching { launch(listOf(job.exe.absolutePath), job.exe.parentFile) }
            .onFailure { AppLog.write("Relaunch after the update failed", it) }
    }

    fun launch(command: List<String>, dir: File?): Process {
        val builder = ProcessBuilder(command)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
        dir?.let(builder::directory)
        val env = builder.environment()
        env.remove("_JPACKAGE_LAUNCHER")
        val own = AppPaths.appDir?.absolutePath
        env["LD_LIBRARY_PATH"]?.let { value ->
            val kept = value.split(File.pathSeparatorChar).filter { it.isNotEmpty() && (own == null || !it.startsWith(own)) }
            if (kept.isEmpty()) env.remove("LD_LIBRARY_PATH") else env["LD_LIBRARY_PATH"] = kept.joinToString(File.pathSeparator)
        }
        return builder.start()
    }

    private fun apply(job: Job) {
        ProcessHandle.of(job.pid).ifPresent { process ->
            runCatching { process.onExit().get(ExitWaitSeconds, TimeUnit.SECONDS) }
        }
        Thread.sleep(SettleMs)
        val temp = File(job.archive.parentFile, "extract")
        deleteTree(temp)
        try {
            extract(job.archive, temp)
            job.archive.delete()
            val source = File(temp, "pixelGram").takeIf { it.isDirectory } ?: temp
            moveOver(source, job.target)
            job.marker.writeText("updated")
        } finally {
            deleteTree(temp)
        }
    }

    private fun extract(archive: File, dir: File) {
        dir.mkdirs()
        if (archive.name.endsWith(".zip", ignoreCase = true)) {
            val root = dir.canonicalFile.toPath()
            ZipFile(archive).use { zip ->
                for (entry in zip.entries()) {
                    val out = root.resolve(entry.name.replace('\\', '/')).normalize()
                    require(out.startsWith(root)) { "Unsafe path in the update: ${entry.name}" }
                    if (entry.isDirectory) {
                        Files.createDirectories(out)
                    } else {
                        Files.createDirectories(out.parent)
                        zip.getInputStream(entry).use { Files.copy(it, out, StandardCopyOption.REPLACE_EXISTING) }
                    }
                }
            }
        } else {
            val process = ProcessBuilder("tar", "-xzf", archive.absolutePath, "-C", dir.absolutePath)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText().trim()
            val code = process.waitFor()
            if (code != 0) error("tar exit $code ${output.lineSequence().firstOrNull().orEmpty()}".trim())
        }
    }

    private fun moveOver(source: File, target: File) {
        val from = source.toPath()
        val to = target.toPath()
        val keepData = File(target, PortableData).isDirectory
        val paths = Files.walk(from).use { it.toList() }
        var failed = 0
        var firstError: Throwable? = null
        for (path in paths) {
            if (path == from) continue
            val relative = from.relativize(path)
            if (relative.getName(0).toString() == PortableData && !keepData) continue
            val destination = to.resolve(relative.toString())
            val result = retry {
                if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                    Files.createDirectories(destination)
                } else {
                    Files.move(path, destination, StandardCopyOption.REPLACE_EXISTING)
                }
            }
            result.exceptionOrNull()?.let {
                failed++
                if (firstError == null) firstError = it
                AppLog.write("Update could not replace $relative", it)
            }
        }
        if (failed > 0) error("$failed files could not be replaced: ${firstError?.message ?: firstError?.javaClass?.simpleName}")
    }

    private fun <T> retry(block: () -> T): Result<T> {
        var last: Result<T> = runCatching(block)
        var attempt = 1
        while (last.isFailure && attempt < RetryAttempts) {
            Thread.sleep(RetryDelayMs)
            last = runCatching(block)
            attempt++
        }
        return last
    }

    fun copyApp(from: File, to: File, skip: Set<File>) {
        val root = from.toPath()
        val out = to.toPath()
        Files.createDirectories(out)
        val skipped = skip.map { it.absoluteFile.toPath().normalize() }.toSet()
        val top = from.listFiles().orEmpty().filter { it.absoluteFile.toPath().normalize() !in skipped && it.name != PortableData }
        for (child in top) {
            val paths = Files.walk(child.toPath()).use { it.toList() }
            for (path in paths) {
                val destination = out.resolve(root.relativize(path).toString())
                if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                    Files.createDirectories(destination)
                } else {
                    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES, LinkOption.NOFOLLOW_LINKS)
                }
            }
        }
    }

    fun deleteTree(dir: File): Boolean {
        val root = dir.toPath()
        if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) return true
        val paths = runCatching { Files.walk(root).use { it.sorted(Comparator.reverseOrder()).toList() } }.getOrDefault(listOf(root))
        paths.forEach { runCatching { Files.deleteIfExists(it) } }
        return !Files.exists(root, LinkOption.NOFOLLOW_LINKS)
    }

    fun isInside(path: Path, dir: File): Boolean = runCatching {
        path.toRealPath().startsWith(dir.toPath().toRealPath())
    }.getOrDefault(false)

    private const val JobArgs = 6
    private const val PortableData = "pixelgram-data"
    private const val ExitWaitSeconds = 60L
    private const val SettleMs = 500L
    private const val RetryAttempts = 20
    private const val RetryDelayMs = 500L
}
