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

    fun dispose() = Platform.runLater {
        engine?.load("about:blank")
        engine = null
        panel.scene = null
    }

    private companion object {
        const val Shim = "window.TelegramWebviewProxy = { postEvent: function (type, data) { window.pixelgramBridge.postEvent(String(type), data === undefined || data === null ? '' : String(data)); } };"
    }
}
