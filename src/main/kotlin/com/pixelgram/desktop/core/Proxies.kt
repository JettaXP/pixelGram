package com.pixelgram.desktop.core

import it.tdlight.jni.TdApi
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder

enum class ProxyKind(val title: String) { Socks5("SOCKS5"), Http("HTTP"), MtProto("MTProto") }

data class ProxyEntry(
    val kind: ProxyKind,
    val server: String,
    val port: Int,
    val username: String = "",
    val password: String = "",
    val secret: String = "",
) {
    val address: String get() = "$server:$port"

    fun toTd(): TdApi.Proxy = TdApi.Proxy(
        server,
        port,
        when (kind) {
            ProxyKind.Socks5 -> TdApi.ProxyTypeSocks5(username, password)
            ProxyKind.Http -> TdApi.ProxyTypeHttp(username, password, false)
            ProxyKind.MtProto -> TdApi.ProxyTypeMtproto(secret)
        },
    )

    fun link(): String = when (kind) {
        ProxyKind.MtProto -> "https://t.me/proxy?server=${enc(server)}&port=$port&secret=${enc(secret)}"
        else -> buildString {
            append("https://t.me/socks?server=").append(enc(server)).append("&port=").append(port)
            if (username.isNotEmpty()) append("&user=").append(enc(username))
            if (password.isNotEmpty()) append("&pass=").append(enc(password))
        }
    }

    fun valid(): Boolean = server.isNotBlank() && port in 1..MaxPort && (kind != ProxyKind.MtProto || secret.isNotBlank())

    companion object {
        private const val MaxPort = 65_535

        fun from(proxy: TdApi.Proxy): ProxyEntry? = when (val type = proxy.type) {
            is TdApi.ProxyTypeSocks5 -> ProxyEntry(ProxyKind.Socks5, proxy.server, proxy.port, type.username.orEmpty(), type.password.orEmpty())
            is TdApi.ProxyTypeHttp -> ProxyEntry(ProxyKind.Http, proxy.server, proxy.port, type.username.orEmpty(), type.password.orEmpty())
            is TdApi.ProxyTypeMtproto -> ProxyEntry(ProxyKind.MtProto, proxy.server, proxy.port, secret = type.secret.orEmpty())
            else -> null
        }

        fun decode(raw: String): List<ProxyEntry> = raw.lines().mapNotNull { line ->
            val parts = line.split(' ')
            if (parts.size != 6) return@mapNotNull null
            val kind = ProxyKind.entries.firstOrNull { it.name == parts[0] } ?: return@mapNotNull null
            val port = parts[2].toIntOrNull() ?: return@mapNotNull null
            ProxyEntry(kind, dec(parts[1]), port, dec(parts[3]), dec(parts[4]), dec(parts[5])).takeIf { it.valid() }
        }

        fun encode(list: List<ProxyEntry>): String = list.joinToString("\n") { proxy ->
            listOf(proxy.kind.name, enc(proxy.server), proxy.port.toString(), enc(proxy.username), enc(proxy.password), enc(proxy.secret)).joinToString(" ")
        }

        fun parseLink(text: String): ProxyEntry? {
            val uri = runCatching { URI(text.trim()) }.getOrNull() ?: return null
            val scheme = uri.scheme?.lowercase() ?: return null
            val host = uri.host?.lowercase()?.removePrefix("www.")
            val kind = when {
                scheme == "tg" && host == "proxy" -> ProxyKind.MtProto
                scheme == "tg" && host == "socks" -> ProxyKind.Socks5
                (scheme == "https" || scheme == "http") && (host == "t.me" || host == "telegram.me") && uri.path == "/proxy" -> ProxyKind.MtProto
                (scheme == "https" || scheme == "http") && (host == "t.me" || host == "telegram.me") && uri.path == "/socks" -> ProxyKind.Socks5
                else -> return null
            }
            val query = uri.rawQuery.orEmpty().split('&').mapNotNull { pair ->
                val key = pair.substringBefore('=', "").takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                key to dec(pair.substringAfter('=', ""))
            }.toMap()
            val port = query["port"]?.toIntOrNull() ?: return null
            return ProxyEntry(
                kind,
                query["server"].orEmpty(),
                port,
                query["user"].orEmpty(),
                query["pass"].orEmpty(),
                query["secret"].orEmpty(),
            ).takeIf { it.valid() }
        }

        private fun enc(text: String): String = URLEncoder.encode(text, Charsets.UTF_8)

        private fun dec(text: String): String = runCatching { URLDecoder.decode(text, Charsets.UTF_8) }.getOrDefault(text)
    }
}
