package com.pixelgram.desktop.ui.webapp

import com.pixelgram.desktop.core.AppLog
import javafx.application.Platform
import javafx.concurrent.Worker
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.paint.Color
import javafx.scene.web.WebEngine
import javafx.scene.web.WebView
import netscape.javascript.JSObject
import javax.swing.SwingUtilities

class MiniAppBrowser(private val onEvent: (String, Map<String, Any?>) -> Unit) {
    val panel: JFXPanel = JFXPanel().also { Platform.setImplicitExit(false) }
    private var engine: WebEngine? = null
    private val bridge = Bridge()

    inner class Bridge {
        fun postEvent(type: String, data: String?) {
            @Suppress("UNCHECKED_CAST")
            val payload = data?.takeIf { it.isNotBlank() }?.let { MiniJson.parse(it) as? Map<String, Any?> }.orEmpty()
            SwingUtilities.invokeLater { onEvent(type, payload) }
        }
    }

    fun load(url: String, userAgent: String?, background: Int) = Platform.runLater {
        val view = WebView()
        val engine = view.engine
        userAgent?.let { engine.userAgent = it }
        engine.documentProperty().addListener { _, _, document -> if (document != null) inject(engine) }
        engine.loadWorker.stateProperty().addListener { _, _, state -> if (state == Worker.State.SUCCEEDED) inject(engine) }
        engine.loadWorker.exceptionProperty().addListener { _, _, error -> if (error != null) AppLog.write("Mini app failed to load", error) }
        engine.locationProperty().addListener { _, old, location ->
            if (location != null && TelegramLink.containsMatchIn(location)) {
                SwingUtilities.invokeLater { onEvent("pixelgram_tg_link", mapOf("url" to location)) }
                Platform.runLater { if (engine.history.currentIndex > 0) engine.history.go(-1) else if (old != null) engine.load(old) }
            }
        }
        view.pageFill = Color.rgb(background shr 16 and 0xFF, background shr 8 and 0xFF, background and 0xFF)
        panel.scene = Scene(view)
        engine.load(url)
        this.engine = engine
    }

    private fun inject(engine: WebEngine) {
        runCatching {
            (engine.executeScript("window") as JSObject).setMember("pixelgramBridge", bridge)
            engine.executeScript(Shim)
        }.onFailure { AppLog.write("Mini app bridge failed", it) }
    }

    fun send(type: String, data: Map<String, Any?> = emptyMap()) = Platform.runLater {
        runCatching {
            engine?.executeScript("window.Telegram && window.Telegram.WebView && window.Telegram.WebView.receiveEvent(${MiniJson.quote(type)}, ${MiniJson.write(data)});")
        }.onFailure { AppLog.write("Mini app event failed", it) }
    }

    fun reload() = Platform.runLater { engine?.reload() }

    fun copyText(all: Boolean, done: (Boolean) -> Unit) = Platform.runLater {
        val text = runCatching {
            engine?.executeScript(if (all) "document.body ? document.body.innerText : ''" else "window.getSelection ? String(window.getSelection()) : ''") as? String
        }.getOrNull().orEmpty()
        SwingUtilities.invokeLater {
            if (text.isNotBlank()) copyToClipboard(text)
            done(text.isNotBlank())
        }
    }

    fun dispose() = Platform.runLater {
        engine?.load("about:blank")
        engine = null
        panel.scene = null
    }

    private fun copyToClipboard(text: String) {
        runCatching { java.awt.Toolkit.getDefaultToolkit().systemClipboard.setContents(java.awt.datatransfer.StringSelection(text), null) }
            .onFailure { AppLog.write("Mini app copy failed", it) }
    }

    private companion object {
        val TelegramLink = Regex("^(https?://)?(www\\.)?(t\\.me|telegram\\.me|telegram\\.dog)/|^tg:", RegexOption.IGNORE_CASE)
        const val Shim = "window.TelegramWebviewProxy = { postEvent: function (type, data) { window.pixelgramBridge.postEvent(String(type), data === undefined || data === null ? '' : String(data)); } };" +
            "if (!window.__pgLinks) { window.__pgLinks = true;" +
            "var pgTg = function (u) { return /^(https?:\\/\\/)?(www\\.)?(t\\.me|telegram\\.me|telegram\\.dog)\\//i.test(u) || /^tg:/i.test(u); };" +
            "document.addEventListener('click', function (e) { var a = e.target && e.target.closest ? e.target.closest('a') : null; if (!a || !a.href) return;" +
            " if (pgTg(a.href)) { e.preventDefault(); e.stopPropagation(); window.pixelgramBridge.postEvent('pixelgram_tg_link', JSON.stringify({ url: a.href })); }" +
            " else if (a.target === '_blank') { e.preventDefault(); window.pixelgramBridge.postEvent('web_app_open_link', JSON.stringify({ url: a.href })); } }, true);" +
            "var pgOpen = window.open; window.open = function (u) { var s = String(u || '');" +
            " if (pgTg(s)) { window.pixelgramBridge.postEvent('pixelgram_tg_link', JSON.stringify({ url: s })); return null; }" +
            " if (s) { window.pixelgramBridge.postEvent('web_app_open_link', JSON.stringify({ url: s })); return null; }" +
            " return pgOpen ? pgOpen.apply(window, arguments) : null; }; }" +
            "if (!window.__pgCopy) { window.__pgCopy = true;" +
            "var pgStyle = document.createElement('style'); pgStyle.textContent = '*:not(input):not(textarea){-webkit-user-select:text !important;user-select:text !important;}';" +
            "(document.head || document.documentElement).appendChild(pgStyle);" +
            "document.addEventListener('keydown', function (e) { if ((e.ctrlKey || e.metaKey) && (e.key === 'c' || e.key === 'C')) { var t = String(window.getSelection ? window.getSelection() : '');" +
            " if (t) window.pixelgramBridge.postEvent('pixelgram_copy', JSON.stringify({ text: t })); } }, true); }"
    }
}
