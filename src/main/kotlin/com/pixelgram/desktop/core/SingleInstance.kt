package com.pixelgram.desktop.core

import java.io.File
import java.io.RandomAccessFile
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.channels.FileLock
import javax.swing.SwingUtilities

object SingleInstance {
    var onShow: (() -> Unit)? = null
    private var lock: FileLock? = null

    fun acquire(): Boolean {
        val dir = AppPaths.dataDir.apply { mkdirs() }
        val portFile = File(dir, "instance.port")
        lock = runCatching { RandomAccessFile(File(dir, "instance.lock"), "rw").channel.tryLock() }.getOrNull()
        if (lock == null) return !showOther(portFile)
        runCatching {
            val server = ServerSocket(0, 4, InetAddress.getLoopbackAddress())
            portFile.writeText(server.localPort.toString())
            Thread {
                while (true) {
                    runCatching { server.accept().use { it.getInputStream().read() } }
                        .onSuccess { SwingUtilities.invokeLater { onShow?.invoke() } }
                }
            }.apply { isDaemon = true }.start()
        }.onFailure { AppLog.write("Instance server failed", it) }
        return true
    }

    private fun showOther(portFile: File): Boolean = runCatching {
        Socket(InetAddress.getLoopbackAddress(), portFile.readText().trim().toInt()).use {
            it.soTimeout = 2000
            it.getOutputStream().write(1)
        }
    }.isSuccess
}
