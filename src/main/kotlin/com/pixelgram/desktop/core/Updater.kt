package com.pixelgram.desktop.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pixelgram.desktop.BuildConfig
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLDecoder
import kotlin.system.exitProcess

data class UpdateAsset(val name: String, val url: String, val size: Long)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data object Unavailable : UpdateState
    data class Available(val commit: String, val asset: UpdateAsset, val version: String?) : UpdateState
    data class Downloading(val asset: UpdateAsset, val version: String?) : UpdateState
    data class Ready(val file: File, val manual: String?) : UpdateState
    data class Failed(val message: String) : UpdateState
}

object Updater {
    private const val Repository = "JettaXP/pixelGram"
    private const val Channel = "nightly"
    private const val ApiUrl = "https://api.github.com/repos/$Repository/releases/tags/$Channel"
    private const val TimeoutMs = 15_000

    var state by mutableStateOf<UpdateState>(UpdateState.Idle)
        private set
    var progress by mutableFloatStateOf(0f)
        private set

    val enabled: Boolean get() = BuildConfig.COMMIT != "dev"

    var shutdown: () -> Unit = {}

    fun check() {
        if (!enabled || state is UpdateState.Checking || state is UpdateState.Downloading) return
        state = UpdateState.Checking
        Thread {
            state = runCatching { fetch() }.getOrElse {
                AppLog.write("Update check failed", it)
                UpdateState.Unavailable
            }
        }.apply { isDaemon = true }.start()
    }

    private fun fetch(): UpdateState {
        val connection = URI(ApiUrl).toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = TimeoutMs
        connection.readTimeout = TimeoutMs
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        if (connection.responseCode != HttpURLConnection.HTTP_OK) return UpdateState.Unavailable
        val json = connection.inputStream.bufferedReader().use { it.readText() }
        val commit = Regex(""""target_commitish"\s*:\s*"([^"]+)"""").find(json)?.groupValues?.get(1)
            ?: return UpdateState.Unavailable
        if (commit.equals(BuildConfig.COMMIT, ignoreCase = true)) return UpdateState.UpToDate
        val assets = Regex(""""browser_download_url"\s*:\s*"([^"]+)"""")
            .findAll(json)
            .map { match ->
                val url = match.groupValues[1]
                UpdateAsset(URLDecoder.decode(url.substringAfterLast('/'), Charsets.UTF_8), url, 0L)
            }
            .toList()
        val asset = pickAsset(assets) ?: return UpdateState.Unavailable
        val version = Regex("""pixel[Gg]ram-(\d+\.\d+\.\d+)""").find(asset.name)?.groupValues?.get(1)
            ?: Regex("""What's new in (\d+\.\d+\.\d+)""").find(json)?.groupValues?.get(1)
        return UpdateState.Available(commit.take(7), asset, version)
    }

    private fun pickAsset(assets: List<UpdateAsset>): UpdateAsset? {
        fun find(predicate: (String) -> Boolean) = assets.firstOrNull { predicate(it.name.lowercase()) }
        return when {
            AppPaths.isWindows && AppPaths.isPortable -> find { it.endsWith("portable.zip") }
            AppPaths.isWindows -> find { it.endsWith(".msi") }
            System.getenv("APPIMAGE") != null -> find { it.endsWith(".appimage") }
            isPacmanInstall() -> find { it.endsWith(".pkg.tar.zst") }
            AppPaths.isLinux -> find { it.endsWith(".tar.gz") }
            else -> null
        }
    }

    private fun isPacmanInstall(): Boolean =
        System.getProperty("jpackage.app-path")?.startsWith("/usr/") == true || File("/usr/lib/pixelgram").isDirectory

    fun download() {
        val available = state as? UpdateState.Available ?: return
        state = UpdateState.Downloading(available.asset, available.version)
        progress = 0f
        Thread {
            state = runCatching { save(available.asset) }.getOrElse {
                AppLog.write("Update download failed", it)
                UpdateState.Failed(it.message ?: "Download failed")
            }
        }.apply { isDaemon = true }.start()
    }

    private fun save(asset: UpdateAsset): UpdateState {
        val dir = File(AppPaths.dataDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val target = File(dir, asset.name)
        val part = File(dir, asset.name + ".part")
        val connection = URI(asset.url).toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = TimeoutMs
        connection.instanceFollowRedirects = true
        val total = connection.contentLengthLong.takeIf { it > 0 } ?: asset.size
        connection.inputStream.use { input ->
            part.outputStream().use { output ->
                val buffer = ByteArray(1 shl 16)
                var done = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    done += read
                    if (total > 0) progress = done.toFloat() / total
                }
            }
        }
        if (!part.renameTo(target)) error("Couldn't save ${asset.name}")
        return UpdateState.Ready(target, manualStep(target))
    }

    private fun manualStep(file: File): String? = when {
        file.name.endsWith(".msi") || file.name.endsWith(".AppImage", ignoreCase = true) -> null
        file.name.endsWith(".pkg.tar.zst") -> "sudo pacman -U \"${file.absolutePath}\""
        isArchive(file) && launcher() != null && AppPaths.appDir != null -> null
        else -> "Unpack ${file.name} over the current pixelGram folder."
    }

    private fun isArchive(file: File) = file.name.endsWith(".zip") || file.name.endsWith(".tar.gz")

    private fun launcher(): File? = System.getProperty("jpackage.app-path")?.let(::File)?.takeIf { it.isFile }

    private val marker: File get() = File(AppPaths.dataDir, "just-updated")

    fun consumeUpdated(): Boolean = marker.isFile && marker.delete()

    fun consumeFailure(): String? {
        val log = File(AppPaths.dataDir, "update-error.log").takeIf { it.isFile } ?: return null
        val reason = log.readLines().lastOrNull { it.isNotBlank() }?.trim()
        log.delete()
        return reason ?: "Unknown error"
    }

    private fun applyArchive(file: File) {
        val exe = launcher() ?: error("Unknown launcher")
        val target = AppPaths.appDir ?: error("Unknown install folder")
        val pid = ProcessHandle.current().pid().toString()
        val dir = file.parentFile
        if (AppPaths.isWindows) {
            val script = File(dir, "apply-update.ps1").apply { writeText(WindowsScript) }
            ProcessBuilder(
                "powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-WindowStyle", "Hidden", "-File", script.absolutePath,
                "-ProcessId", pid, "-Archive", file.absolutePath, "-Target", target.absolutePath,
                "-Exe", exe.absolutePath, "-Marker", marker.absolutePath,
            ).start()
        } else {
            val script = File(dir, "apply-update.sh").apply { writeText(LinuxScript) }
            ProcessBuilder("nohup", "sh", script.absolutePath, pid, file.absolutePath, target.absolutePath, exe.absolutePath, marker.absolutePath)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
        }
    }

    fun install(beforeExit: () -> Unit) {
        val ready = state as? UpdateState.Ready ?: return
        val file = ready.file
        runCatching {
            when {
                file.name.endsWith(".msi") -> {
                    marker.writeText("updated")
                    val exe = System.getProperty("jpackage.app-path").orEmpty()
                    val relaunch = if (exe.isNotEmpty()) " && start \"\" \"$exe\"" else ""
                    ProcessBuilder("cmd", "/c", "msiexec /i \"${file.absolutePath}\" /passive$relaunch").start()
                }
                isArchive(file) -> applyArchive(file)
                file.name.endsWith(".AppImage", ignoreCase = true) -> {
                    val current = File(System.getenv("APPIMAGE") ?: error("Not running from an AppImage"))
                    file.setExecutable(true)
                    marker.writeText("updated")
                    if (!file.renameTo(current)) file.copyTo(current, overwrite = true).setExecutable(true)
                    ProcessBuilder(current.absolutePath).start()
                }
                else -> {
                    openFolder(file.parentFile)
                    return
                }
            }
            beforeExit()
            shutdown()
            exitProcess(0)
        }.onFailure {
            AppLog.write("Update install failed", it)
            state = UpdateState.Failed(it.message ?: "Install failed")
        }
    }

    fun openFolder(dir: File) {
        runCatching { java.awt.Desktop.getDesktop().open(dir) }.recoverCatching {
            ProcessBuilder("xdg-open", dir.absolutePath).start()
        }
    }
}

private val WindowsScript = """
param([int]${'$'}ProcessId, [string]${'$'}Archive, [string]${'$'}Target, [string]${'$'}Exe, [string]${'$'}Marker)
Wait-Process -Id ${'$'}ProcessId -Timeout 60 -ErrorAction SilentlyContinue
Start-Sleep -Milliseconds 500
${'$'}temp = Join-Path (Split-Path ${'$'}Archive) 'extract'
Remove-Item -LiteralPath ${'$'}temp -Recurse -Force -ErrorAction SilentlyContinue
try {
    Expand-Archive -LiteralPath ${'$'}Archive -DestinationPath ${'$'}temp -Force
    Remove-Item -LiteralPath ${'$'}Archive -Force -ErrorAction SilentlyContinue
    ${'$'}source = Join-Path ${'$'}temp 'pixelGram'
    if (-not (Test-Path ${'$'}source)) { ${'$'}source = ${'$'}temp }
    robocopy ${'$'}source ${'$'}Target /E /MOVE /R:5 /W:1 /NFL /NDL /NJH /NJS /NP | Out-Null
    if (${'$'}LASTEXITCODE -lt 8) {
        Set-Content -LiteralPath ${'$'}Marker -Value 'updated'
    }
    else { Add-Content -LiteralPath (Join-Path (Split-Path ${'$'}Marker) 'update-error.log') -Value "robocopy exit ${'$'}LASTEXITCODE" }
} catch {
    Add-Content -LiteralPath (Join-Path (Split-Path ${'$'}Marker) 'update-error.log') -Value ${'$'}_.Exception.Message
} finally {
    Remove-Item -LiteralPath ${'$'}temp -Recurse -Force -ErrorAction SilentlyContinue
}
Start-Process -FilePath ${'$'}Exe
""".trimStart()

private val LinuxScript = """
pid="${'$'}1"; archive="${'$'}2"; target="${'$'}3"; exe="${'$'}4"; marker="${'$'}5"
while kill -0 "${'$'}pid" 2>/dev/null; do sleep 0.3; done
temp="${'$'}(dirname "${'$'}archive")/extract"
rm -rf "${'$'}temp"; mkdir -p "${'$'}temp"
if tar -xzf "${'$'}archive" -C "${'$'}temp" && rm -f "${'$'}archive" && cp -a "${'$'}temp/pixelGram/." "${'$'}target/"; then
    echo updated > "${'$'}marker"
else
    echo "update failed" >> "${'$'}(dirname "${'$'}marker")/update-error.log"
fi
rm -rf "${'$'}temp"
"${'$'}exe" > /dev/null 2>&1 &
""".trimStart()
