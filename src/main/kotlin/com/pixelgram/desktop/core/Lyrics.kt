package com.pixelgram.desktop.core

import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class LyricWord(val time: Float, val text: String)

class LyricLine(val time: Float, val text: String, val words: List<LyricWord> = emptyList())

sealed interface LyricsResult {
    data object Loading : LyricsResult
    data object NotFound : LyricsResult
    data object Instrumental : LyricsResult
    class Plain(val lines: List<String>) : LyricsResult
    class Synced(val lines: List<LyricLine>) : LyricsResult
}

object Lyrics {
    private val cache = java.util.concurrent.ConcurrentHashMap<String, LyricsResult>()
    private val client by lazy { HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NORMAL).build() }

    fun cached(artist: String, title: String): LyricsResult? = cache[key(artist, title)]

    fun load(artist: String, title: String, duration: Int): LyricsResult {
        val key = key(artist, title)
        cache[key]?.let { return it }
        if (title.isBlank()) return LyricsResult.NotFound
        val base = "https://lrclib.net/api/get?artist_name=${encode(artist)}&track_name=${encode(title)}"
        val direct = listOfNotNull(
            if (duration > 0) request("$base&duration=$duration") else null,
            request(base),
        ).map(::parse).firstOrNull { it.useful() }
        val result = direct ?: search(artist, title) ?: LyricsResult.NotFound
        cache[key] = result
        return result
    }

    private fun search(artist: String, title: String): LyricsResult? {
        val queries = listOfNotNull(
            "track_name=${encode(title)}" + if (artist.isNotBlank()) "&artist_name=${encode(artist)}" else "",
            "q=${encode("$artist $title".trim())}",
        )
        val wanted = normalize(title)
        val candidates = queries.asSequence()
            .mapNotNull { request("https://lrclib.net/api/search?$it") }
            .flatMap { objects(it) }
            .filter { item -> field(item, "trackName")?.let { normalize(it) == wanted || normalize(it).contains(wanted) } == true }
            .map(::parse)
            .filter { it.useful() }
            .toList()
        return candidates.firstOrNull { it is LyricsResult.Synced }
            ?: candidates.firstOrNull { it is LyricsResult.Instrumental }
            ?: candidates.firstOrNull()
    }

    private fun LyricsResult.useful(): Boolean = when (this) {
        LyricsResult.Instrumental -> true
        is LyricsResult.Synced -> lines.count { it.text.isNotBlank() } >= MinLines
        is LyricsResult.Plain -> lines.count { it.isNotBlank() } >= MinLines
        else -> false
    }

    private fun objects(body: String): List<String> {
        val out = mutableListOf<String>()
        var depth = 0
        var start = -1
        var quoted = false
        var escaped = false
        body.forEachIndexed { i, c ->
            when {
                escaped -> escaped = false
                c == '\\' && quoted -> escaped = true
                c == '"' -> quoted = !quoted
                quoted -> Unit
                c == '{' -> {
                    if (depth == 0) start = i
                    depth++
                }
                c == '}' -> {
                    depth--
                    if (depth == 0 && start >= 0) out += body.substring(start, i + 1)
                }
            }
        }
        return out
    }

    private fun request(url: String): String? = runCatching {
        val request = HttpRequest.newBuilder(URI(url))
            .timeout(Duration.ofSeconds(12))
            .header("User-Agent", "pixelGram/${com.pixelgram.desktop.BuildConfig.VERSION} (https://github.com/JettaXP/pixelGram)")
            .GET()
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        response.body().takeIf { response.statusCode() == 200 && it.isNotBlank() && it != "[]" }
    }.getOrNull()

    private fun parse(body: String): LyricsResult {
        val instrumental = Regex(""""instrumental"\s*:\s*true""").containsMatchIn(body)
        val synced = field(body, "syncedLyrics")
        val plain = field(body, "plainLyrics")
        return when {
            !synced.isNullOrBlank() -> parseSynced(synced).takeIf { it.isNotEmpty() }?.let { LyricsResult.Synced(it) }
                ?: LyricsResult.Plain(synced.lines())
            !plain.isNullOrBlank() -> LyricsResult.Plain(plain.lines())
            instrumental -> LyricsResult.Instrumental
            else -> LyricsResult.NotFound
        }
    }

    private fun parseSynced(text: String): List<LyricLine> {
        val tag = Regex("""\[(\d+):(\d+(?:\.\d+)?)]""")
        return text.lines().flatMap { line ->
            val stamps = tag.findAll(line).toList()
            if (stamps.isEmpty()) return@flatMap emptyList()
            val body = line.substring(stamps.last().range.last + 1).trim()
            val wordTag = Regex("""<(\d+):(\d+(?:\.\d+)?)>""")
            val words = mutableListOf<LyricWord>()
            val marks = wordTag.findAll(body).toList()
            marks.forEachIndexed { i, mark ->
                val end = marks.getOrNull(i + 1)?.range?.first ?: body.length
                val piece = body.substring(mark.range.last + 1, end)
                if (piece.isNotEmpty()) words += LyricWord(mark.groupValues[1].toInt() * 60 + mark.groupValues[2].toFloat(), piece)
            }
            val text = body.replace(wordTag, "").replace(Regex("\\s+"), " ").trim()
            stamps.map { stamp -> LyricLine(stamp.groupValues[1].toInt() * 60 + stamp.groupValues[2].toFloat(), text, words) }
        }.sortedBy { it.time }
    }

    private fun field(body: String, name: String): String? {
        val start = Regex(""""$name"\s*:\s*""").find(body) ?: return null
        var i = start.range.last + 1
        if (i >= body.length || body[i] != '"') return null
        i++
        val out = StringBuilder()
        while (i < body.length) {
            val c = body[i]
            when {
                c == '"' -> return out.toString()
                c == '\\' && i + 1 < body.length -> {
                    when (val next = body[i + 1]) {
                        'n' -> out.append('\n')
                        'r' -> Unit
                        't' -> out.append('\t')
                        'u' -> if (i + 5 < body.length) {
                            body.substring(i + 2, i + 6).toIntOrNull(16)?.let { out.append(it.toChar()) }
                            i += 4
                        }
                        else -> out.append(next)
                    }
                    i += 2
                    continue
                }
                else -> out.append(c)
            }
            i++
        }
        return null
    }

    private fun normalize(value: String) = value.lowercase().filter { it.isLetterOrDigit() }

    private fun key(artist: String, title: String) = "${artist.lowercase()}\u0000${title.lowercase()}"

    private fun encode(value: String) = URLEncoder.encode(value, Charsets.UTF_8)
}

private const val MinLines = 3
