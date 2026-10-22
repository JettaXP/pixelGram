package com.pixelgram.desktop.core

import java.io.File
import java.io.RandomAccessFile
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.channels.FileLock
import javax.swing.SwingUtilities

enum class InstanceState { Primary, Forwarded, Stuck }

object SingleInstance {
    var onShow: (() -> Unit)? = null
    var onOpen: ((String) -> Unit)? = null
    private var lock: FileLock? = null

    fun acquire(link: String? = null): InstanceState {
        val dir = AppPaths.dataDir.apply { mkdirs() }
        val portFile = File(dir, "instance.port")
        lock = runCatching { RandomAccessFile(File(dir, "instance.lock"), "rw").channel.tryLock() }.getOrNull()
        if (lock == null) return if (showOther(portFile, link)) InstanceState.Forwarded else InstanceState.Stuck
        runCatching {
            val server = ServerSocket(0, 4, InetAddress.getLoopbackAddress())
            portFile.writeText(server.localPort.toString())
            Thread {
                while (true) {
                    runCatching { server.accept().use { it.getInputStream().bufferedReader().readLine().orEmpty() } }
                        .onSuccess { line ->
                            SwingUtilities.invokeLater {
                                if (line.isNotBlank() && line != ShowCommand) onOpen?.invoke(line)
                                onShow?.invoke()
                            }
                        }
                }
            }.apply { isDaemon = true }.start()
        }.onFailure { AppLog.write("Instance server failed", it) }
        return InstanceState.Primary
    }

    private fun showOther(portFile: File, link: String?): Boolean = runCatching {
        Socket(InetAddress.getLoopbackAddress(), portFile.readText().trim().toInt()).use {
            it.soTimeout = 2000
            it.getOutputStream().write(((link ?: ShowCommand) + "\n").toByteArray())
        }
    }.isSuccess

    private const val ShowCommand = "show"
}
