package com.pixelgram.desktop.core

import java.util.concurrent.TimeUnit

object FocusMode {
    private const val CacheMs = 10_000L
    private const val TimeoutSeconds = 2L

    val supported: Boolean by lazy { AppPaths.isLinux && read() != null }

    @Volatile
    private var cached: Boolean = false
    @Volatile
    private var checkedAt = 0L

    fun active(): Boolean {
        val now = System.currentTimeMillis()
        if (now - checkedAt > CacheMs) {
            checkedAt = now
            cached = read() == true
        }
        return cached
    }

    private fun read(): Boolean? = runCatching {
        val process = ProcessBuilder("gsettings", "get", "org.gnome.desktop.notifications", "show-banners")
            .redirectErrorStream(true)
            .start()
        if (!process.waitFor(TimeoutSeconds, TimeUnit.SECONDS)) {
            process.destroy()
            return null
        }
        when (process.inputStream.bufferedReader().readText().trim()) {
            "false" -> true
            "true" -> false
            else -> null
        }
    }.getOrNull()
}
