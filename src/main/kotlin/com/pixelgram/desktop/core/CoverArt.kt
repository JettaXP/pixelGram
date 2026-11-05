package com.pixelgram.desktop.core

import androidx.compose.runtime.mutableStateMapOf
import it.tdlight.jni.TdApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

object CoverArt {
    private val found = mutableStateMapOf<String, String>()
    private val requested = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client by lazy { HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NORMAL).build() }
    private val dir by lazy { File(AppPaths.dataDir, "covers").apply { mkdirs() } }

    fun path(audio: TdApi.Audio?): String? {
        audio ?: return null
        if (audio.albumCoverThumbnail != null) return null
        val title = audio.title.trim()
        if (title.isEmpty()) return null
        val artist = audio.performer.trim()
        val key = "$artist|$title".lowercase()
        found[key]?.let { return it }
        if (requested.add(key)) scope.launch { fetch(key, artist, title)?.let { found[key] = it } }
        return null
    }

    private fun fetch(key: String, artist: String, title: String): String? = runCatching {
        val file = File(dir, Integer.toHexString(key.hashCode()) + ".jpg")
        if (file.length() > 0) return file.absolutePath
        val term = URLEncoder.encode("$artist $title".trim(), Charsets.UTF_8)
        val body = get("https://itunes.apple.com/search?term=$term&entity=song&limit=5")?.let { String(it) } ?: return null
        val art = Regex(""""artworkUrl100"\s*:\s*"([^"]+)"""").find(body)?.groupValues?.get(1) ?: return null
        val image = get(art.replace("100x100bb", "600x600bb")) ?: return null
        file.writeBytes(image)
        file.absolutePath
    }.onFailure { requested.remove(key) }.getOrNull()

    private fun get(url: String): ByteArray? {
        val request = HttpRequest.newBuilder(URI(url))
            .timeout(Duration.ofSeconds(12))
            .header("User-Agent", "pixelGram/${com.pixelgram.desktop.BuildConfig.VERSION}")
            .GET()
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofByteArray())
        if (response.statusCode() != 200) {
            if (response.statusCode() >= 500 || response.statusCode() == 429) throw java.io.IOException("HTTP ${response.statusCode()}")
            return null
        }
        return response.body().takeIf { it.isNotEmpty() }
    }
}
