package com.pixelgram.desktop.core

import androidx.compose.runtime.mutableStateMapOf
import java.io.File
import java.net.HttpURLConnection
import java.net.URI

object Supporters {
    private val Urls = listOf(
        "https://pixelgram.jettax.xyz/api/badges.json",
        "https://raw.githubusercontent.com/JettaXP/pixelGram/main/docs/api/badges.json",
    )
    private const val RefreshMs = 30 * 60 * 1000L
    private const val TimeoutMs = 15_000

    private val dir: File by lazy { File(AppPaths.dataDir, "banners").apply { mkdirs() } }
    private val cacheFile get() = File(dir, "supporters.json")
    private val numbers = mutableStateMapOf<Long, Int>()
    private val custom = mutableStateMapOf<Long, Boolean>()
    private var lastRefresh = 0L
    private var started = false

    fun number(userId: Long): Int? {
        if (!started) start()
        return numbers[userId]
    }

    fun canCustomize(userId: Long): Boolean {
        if (!started) start()
        return custom[userId] == true
    }

    private fun start() {
        started = true
        runCatching { if (cacheFile.isFile) apply(cacheFile.readText()) }
        refresh()
    }

    private fun apply(text: String) {
        val supporters = Regex("\"supporters\"\\s*:\\s*\\{([^}]*)}").find(text)?.groupValues?.get(1).orEmpty()
        val parsed = Regex("\"(\\d+)\"\\s*:\\s*(\\d+)").findAll(supporters).associate { it.groupValues[1].toLong() to it.groupValues[2].toInt() }
        val list = Regex("\"custom\"\\s*:\\s*\\[([^]]*)]").find(text)?.groupValues?.get(1).orEmpty()
        val customIds = Regex("\"?(\\d+)\"?").findAll(list).map { it.groupValues[1].toLong() }.toSet()
        numbers.keys.retainAll(parsed.keys)
        numbers.putAll(parsed)
        custom.keys.retainAll(customIds)
        customIds.forEach { custom[it] = true }
    }

    fun refresh() {
        val now = System.currentTimeMillis()
        if (now - lastRefresh < RefreshMs) return
        lastRefresh = now
        Thread {
            for (url in Urls) {
                val ok = runCatching {
                    val connection = URI(url).toURL().openConnection() as HttpURLConnection
                    connection.connectTimeout = TimeoutMs
                    connection.readTimeout = TimeoutMs
                    connection.setRequestProperty("Cache-Control", "no-cache")
                    if (connection.responseCode != HttpURLConnection.HTTP_OK) return@runCatching false
                    val text = connection.inputStream.bufferedReader().use { it.readText() }
                    if (!text.contains("\"supporters\"")) return@runCatching false
                    cacheFile.writeText(text)
                    apply(text)
                    true
                }.onFailure { AppLog.write("Supporters failed: $url", it) }.getOrDefault(false)
                if (ok) break
            }
        }.apply { isDaemon = true }.start()
    }
}
