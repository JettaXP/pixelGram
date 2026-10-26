package com.pixelgram.desktop.ui.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.UriHandler
import com.pixelgram.desktop.core.AppLog
import com.pixelgram.desktop.telegram.Telegram
import it.tdlight.jni.TdApi
import java.awt.Desktop
import java.net.URI
import javax.swing.SwingUtilities

object Links : UriHandler {
    var active: (() -> Telegram?)? = null
    var navigate: ((Telegram, Long) -> Unit)? = null
    var pending by mutableStateOf<String?>(null)
    var invite by mutableStateOf<InviteRequest?>(null)
    var confirm by mutableStateOf<String?>(null)

    class InviteRequest(val telegram: Telegram, val link: String, val info: TdApi.ChatInviteLinkInfo)

    private val SafeSchemes = setOf("http", "https", "ton", "mailto", "tel")

    fun safe(url: String): Boolean {
        val scheme = runCatching { URI(url.trim()).scheme }.getOrNull()?.lowercase() ?: return false
        return scheme in SafeSchemes
    }

    fun confirmOpen(url: String) {
        if (isTelegram(url)) open(url) else confirm = url
    }

    private val TelegramHosts = setOf("t.me", "telegram.me", "telegram.dog")

    fun isTelegram(url: String): Boolean {
        val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return false
        if (uri.scheme.equals("tg", ignoreCase = true)) return true
        val host = uri.host?.lowercase()?.removePrefix("www.") ?: return false
        return host in TelegramHosts || host.endsWith(".t.me")
    }

    override fun openUri(uri: String) = open(uri)

    fun open(url: String) {
        if (!isTelegram(url)) return openBrowser(url)
        val telegram = active?.invoke() ?: run {
            pending = url
            return
        }
        handle(telegram, url)
    }

    private fun go(telegram: Telegram, chatId: Long, messageId: Long = 0) = SwingUtilities.invokeLater {
        navigate?.invoke(telegram, chatId)
        if (messageId != 0L) MessageSearch.jump = chatId to messageId
    }

    private fun handle(telegram: Telegram, url: String) {
        val uri = runCatching { URI(url.trim()) }.getOrNull()
        if (uri?.scheme.equals("tg", ignoreCase = true) && uri?.host == "user") {
            val id = uri.query.orEmpty().substringAfter("id=", "").substringBefore('&').toLongOrNull()
            if (id != null) telegram.openPrivateChat(id) { go(telegram, it) }
            return
        }
        telegram.internalLinkType(url) { type ->
            when (type) {
                is TdApi.InternalLinkTypePublicChat ->
                    telegram.openPublicChat(type.chatUsername, { chatId ->
                        go(telegram, chatId)
                        type.draftText?.takeIf { it.isNotEmpty() }?.let { text ->
                            SwingUtilities.invokeLater { ComposerInsert.request = chatId to text }
                        }
                    }, { openBrowser(url) })
                is TdApi.InternalLinkTypeBotStart ->
                    telegram.startBot(type.botUsername, type.startParameter, { go(telegram, it) }, { openBrowser(url) })
                is TdApi.InternalLinkTypeMessage ->
                    telegram.messageLinkInfo(type.url) { chatId, messageId ->
                        if (chatId != 0L) go(telegram, chatId, messageId) else openBrowser(url)
                    }
                is TdApi.InternalLinkTypeChatInvite ->
                    telegram.checkInvite(type.inviteLink) { info ->
                        when {
                            info == null -> openBrowser(url)
                            info.chatId != 0L -> go(telegram, info.chatId)
                            else -> SwingUtilities.invokeLater { invite = InviteRequest(telegram, type.inviteLink, info) }
                        }
                    }
                is TdApi.InternalLinkTypeStickerSet ->
                    telegram.searchStickerSet(type.stickerSetName) { set ->
                        val chatId = MessageSearch.lastChat
                        if (set != null && chatId != null) {
                            SwingUtilities.invokeLater { StickerPack.current = StickerPackRequest(telegram, chatId, set.id) }
                        } else {
                            openBrowser(url)
                        }
                    }
                else -> openBrowser(url)
            }
        }
    }

    fun openBrowser(url: String) {
        if (!safe(url)) {
            AppLog.write("Blocked link with an unsupported scheme")
            return
        }
        Thread {
            runCatching { Desktop.getDesktop().browse(URI(url)) }.recoverCatching {
                ProcessBuilder("xdg-open", url).start()
            }.onFailure { AppLog.write("Open link failed", it) }
        }.apply { isDaemon = true }.start()
    }
}
