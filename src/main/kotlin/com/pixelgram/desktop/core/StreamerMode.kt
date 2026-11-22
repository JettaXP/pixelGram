package com.pixelgram.desktop.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.CompletionStage

object StreamerMode {
    var settings: AppSettings? = null
    var obsRunning by mutableStateOf(false)
        private set
    var obsLive by mutableStateOf(false)
        private set
    private var suppressed = false
    private var autoEnabled = false
    private var lastTrigger = false

    val active: Boolean get() = settings?.streamerMode?.value == true

    val hideNames: Boolean get() = active && settings?.streamerHideNames?.value == true
    val hidePhones: Boolean get() = active && settings?.streamerHidePhones?.value == true
    val hidePreviews: Boolean get() = active && settings?.streamerHidePreviews?.value == true
    val compactList: Boolean get() = active && settings?.streamerCompactList?.value == true
    val avatars: StreamerAvatars get() = if (active) settings?.streamerAvatars?.value ?: StreamerAvatars.Keep else StreamerAvatars.Keep

    fun toggle(on: Boolean) {
        val s = settings ?: return
        s.streamerMode.value = on
        autoEnabled = false
        if (!on && trigger()) suppressed = true
    }

    private fun trigger(): Boolean {
        val s = settings ?: return false
        return (s.streamerWithObs.value && obsRunning) || (s.streamerWithObsOutput.value && obsLive)
    }

    fun start(settings: AppSettings) {
        this.settings = settings
        if (settings.streamerOnStartup.value) settings.streamerMode.value = true else if (settings.streamerMode.value) autoEnabled = true
        Thread({ loop() }, "streamer-watch").apply { isDaemon = true }.start()
    }

    private fun loop() {
        var socket: ObsSocket? = null
        while (true) {
            runCatching {
                val s = settings ?: return@runCatching
                val running = obsProcessRunning()
                obsRunning = running
                if (running && s.streamerWithObsOutput.value) {
                    if (socket == null || socket?.closed == true) socket = ObsSocket(s.obsPort.value, s.obsPassword.value) { live -> obsLive = live }.also { it.connect() }
                } else {
                    socket?.close()
                    socket = null
                    obsLive = false
                }
                val now = trigger()
                if (!now) suppressed = false
                if (now && !lastTrigger && !suppressed && !s.streamerMode.value) {
                    s.streamerMode.value = true
                    autoEnabled = true
                }
                if (!now && lastTrigger && autoEnabled) {
                    s.streamerMode.value = false
                    autoEnabled = false
                }
                lastTrigger = now
            }
            Thread.sleep(PollMs)
        }
    }

    private fun obsProcessRunning(): Boolean = ProcessHandle.allProcesses().anyMatch { handle ->
        val command = handle.info().command().orElse("").substringAfterLast('/').substringAfterLast('\\').lowercase()
        command == "obs" || command == "obs64.exe" || command == "obs32.exe" || command == "obs.exe" || command == "obs-studio" || command.startsWith("obs-studio")
    }

    private const val PollMs = 3000L
}

private class ObsSocket(private val port: Int, private val password: String, private val onLive: (Boolean) -> Unit) : WebSocket.Listener {
    @Volatile var closed = false
        private set
    private var socket: WebSocket? = null
    private val buffer = StringBuilder()
    private var recording = false
    private var streaming = false

    fun connect() {
        runCatching {
            HttpClient.newHttpClient().newWebSocketBuilder().buildAsync(URI("ws://127.0.0.1:$port"), this)
                .whenComplete { ws, error -> if (error != null) closed = true else socket = ws }
        }.onFailure { closed = true }
    }

    fun close() {
        closed = true
        runCatching { socket?.sendClose(WebSocket.NORMAL_CLOSURE, "") }
    }

    override fun onText(webSocket: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*>? {
        buffer.append(data)
        if (last) {
            handle(webSocket, buffer.toString())
            buffer.setLength(0)
        }
        webSocket.request(1)
        return null
    }

    override fun onClose(webSocket: WebSocket, statusCode: Int, reason: String): CompletionStage<*>? {
        closed = true
        onLive(false)
        return null
    }

    override fun onError(webSocket: WebSocket, error: Throwable) {
        closed = true
        onLive(false)
    }

    private fun handle(ws: WebSocket, text: String) {
        val op = Regex("\"op\"\\s*:\\s*(\\d+)").find(text)?.groupValues?.get(1)?.toIntOrNull() ?: return
        when (op) {
            0 -> {
                val challenge = Regex("\"challenge\"\\s*:\\s*\"([^\"]+)\"").find(text)?.groupValues?.get(1)
                val salt = Regex("\"salt\"\\s*:\\s*\"([^\"]+)\"").find(text)?.groupValues?.get(1)
                val auth = if (challenge != null && salt != null) {
                    val secret = b64(sha(password + salt))
                    ",\"authentication\":\"${b64(sha(secret + challenge))}\""
                } else {
                    ""
                }
                ws.sendText("{\"op\":1,\"d\":{\"rpcVersion\":1,\"eventSubscriptions\":64$auth}}", true)
            }
            2 -> {
                ws.sendText("{\"op\":6,\"d\":{\"requestType\":\"GetRecordStatus\",\"requestId\":\"rec\"}}", true)
                ws.sendText("{\"op\":6,\"d\":{\"requestType\":\"GetStreamStatus\",\"requestId\":\"str\"}}", true)
            }
            5, 7 -> {
                val active = Regex("\"outputActive\"\\s*:\\s*(true|false)").find(text)?.groupValues?.get(1) == "true"
                when {
                    text.contains("RecordStateChanged") || text.contains("\"rec\"") -> recording = active
                    text.contains("StreamStateChanged") || text.contains("\"str\"") -> streaming = active
                    else -> return
                }
                onLive(recording || streaming)
            }
        }
    }

    private fun sha(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())

    private fun b64(bytes: ByteArray) = Base64.getEncoder().encodeToString(bytes)
}
