package com.pixelgram.desktop.core

import androidx.compose.runtime.mutableStateMapOf
import java.io.File
import java.net.HttpURLConnection
import java.net.URI

data class CustomBadge(val emoji: String, val customEmojiId: Long, val text: String) {
    fun command(): String = "/badge $customEmojiId $emoji $text".trimEnd()
}

object CustomBadges {
    val Owners = setOf(1780288121L)
    const val MaxText = 64
    private const val MaxEmoji = 32
    private const val Url = "https://raw.githubusercontent.com/JettaXP/pixelGram/main/Banners/badges.tsv"
    private const val RefreshMs = 60 * 60 * 1000L
    private const val TimeoutMs = 15_000
    private const val Reset = "-"

    private val dir: File by lazy { File(AppPaths.dataDir, "banners").apply { mkdirs() } }
    private val cacheFile get() = File(dir, "badges.tsv")
    private val ownFile get() = File(dir, "own-badges.tsv")
    private val remote = mutableStateMapOf<Long, CustomBadge>()
    private val own = mutableStateMapOf<Long, CustomBadge?>()
    private var lastRefresh = 0L
    private var started = false

    fun of(userId: Long): CustomBadge? {
        if (userId !in Owners) return null
        if (!started) start()
        if (own.containsKey(userId)) return own[userId]
        return remote[userId]
    }

    fun clean(text: String): String = text.replace(Regex("[\\p{Cc}\\p{Cf}&&[^\\u200D\\uFE0F]]"), " ").trim().take(MaxText)

    fun setOwn(userId: Long, badge: CustomBadge?) {
        own[userId] = badge?.copy(text = clean(badge.text))
        runCatching {
            ownFile.writeText(own.entries.joinToString("") { (id, value) -> line(id, value) })
        }.onFailure { AppLog.write("Own badge save failed", it) }
    }

    private fun line(userId: Long, badge: CustomBadge?): String =
        if (badge == null) "$userId\t$Reset\n" else "$userId\t${badge.customEmojiId}\t${badge.emoji}\t${badge.text}\n"

    private fun parse(text: String): Map<Long, CustomBadge?> = text.lines().mapNotNull { row ->
        val parts = row.split('\t')
        val userId = parts.getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
        if (parts.getOrNull(1) == Reset) return@mapNotNull userId to null
        val customId = parts.getOrNull(1)?.toLongOrNull() ?: return@mapNotNull null
        val emoji = parts.getOrNull(2)?.takeIf { it.isNotBlank() && it.length <= MaxEmoji } ?: return@mapNotNull null
        userId to CustomBadge(emoji, customId, clean(parts.getOrNull(3).orEmpty()))
    }.toMap()

    private fun start() {
        started = true
        runCatching {
            if (cacheFile.isFile) apply(cacheFile.readText())
            if (ownFile.isFile) own.putAll(parse(ownFile.readText()))
        }
        refresh()
    }

    private fun apply(text: String) {
        val entries = parse(text).filterValues { it != null }.mapValues { it.value!! }.filterKeys { it in Owners }
        remote.keys.retainAll(entries.keys)
        remote.putAll(entries)
    }

    fun refresh() {
        val now = System.currentTimeMillis()
        if (now - lastRefresh < RefreshMs) return
        lastRefresh = now
        Thread {
            runCatching {
                val connection = URI(Url).toURL().openConnection() as HttpURLConnection
                connection.connectTimeout = TimeoutMs
                connection.readTimeout = TimeoutMs
                when (connection.responseCode) {
                    HttpURLConnection.HTTP_OK -> {
                        val text = connection.inputStream.bufferedReader().use { it.readText() }
                        cacheFile.writeText(text)
                        apply(text)
                    }
                    HttpURLConnection.HTTP_NOT_FOUND -> remote.clear()
                    else -> AppLog.write("Badges: HTTP ${connection.responseCode}")
                }
            }.onFailure { AppLog.write("Badges failed", it) }
        }.apply { isDaemon = true }.start()
    }
}
