package com.pixelgram.desktop.calls

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.pixelgram.desktop.core.AppLog
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.core.Sounds
import com.pixelgram.desktop.telegram.Notifications
import com.pixelgram.desktop.telegram.Telegram
import com.sun.jna.Memory
import com.sun.jna.Pointer
import com.sun.jna.ptr.ByteByReference
import it.tdlight.jni.TdApi
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import java.util.concurrent.Executors

enum class CallPhase { Preparing, Ringing, Incoming, Exchanging, Connecting, Active, Ended }

class CallDevice(val name: String, val metadata: String)

class ActiveCall(val telegram: Telegram, val userId: Long, val outgoing: Boolean, video: Boolean) {
    var callId by mutableIntStateOf(0)
    var phase by mutableStateOf(if (outgoing) CallPhase.Preparing else CallPhase.Incoming)
    var startedAt by mutableLongStateOf(0L)
    var muted by mutableStateOf(false)
    var camera by mutableStateOf(video)
    var screen by mutableStateOf<CallDevice?>(null)
    var remoteFrame by mutableStateOf<ImageBitmap?>(null)
    var localFrame by mutableStateOf<ImageBitmap?>(null)
    var remoteVideo by mutableStateOf(false)
    var remoteMuted by mutableStateOf(false)
    var emojis by mutableStateOf<List<String>>(emptyList())
    var status by mutableStateOf<String?>(null)
    val isVideo: Boolean = video

    internal var handle: Pointer? = null
    internal var library: NtgLibrary? = null
    internal val callbacks = ArrayList<Any>()
    internal var lastRemote = 0L
    internal var lastLocal = 0L
}

object Calls {
    private const val SampleRate = 48_000
    private const val Channels: Byte = 2
    private const val CameraWidth: Short = 1280
    private const val CameraHeight: Short = 720
    private const val ScreenWidth: Short = 1920
    private const val ScreenHeight: Short = 1080
    private const val Fps: Byte = 30
    private const val FrameEveryMs = 33L
    private const val MaxFrameWidth = 960
    private const val EndedHoldMs = 2500L
    private const val RingEveryMs = 2000L
    private const val Ok = 0
    private const val DemoRingMs = 2500L

    var current by mutableStateOf<ActiveCall?>(null)
        private set
    var settings: AppSettings? = null

    private val native = Executors.newSingleThreadExecutor { Thread(it, "calls").apply { isDaemon = true } }

    private fun main(block: () -> Unit) = javax.swing.SwingUtilities.invokeLater(block)

    private val demo: Boolean get() = System.getProperty("pixelgram.demo") == "true"

    fun canCall(telegram: Telegram, userId: Long): Boolean =
        demo || (CallLibrary.supported && userId != telegram.me?.id && telegram.users[userId]?.type is TdApi.UserTypeRegular)

    fun start(telegram: Telegram, userId: Long, video: Boolean) {
        if (current != null) return
        val call = ActiveCall(telegram, userId, outgoing = true, video = video)
        current = call
        if (demo) {
            call.phase = CallPhase.Ringing
            Thread {
                Thread.sleep(DemoRingMs)
                main {
                    call.emojis = listOf("🐱", "🚀", "🍕", "🎧")
                    call.phase = CallPhase.Active
                    call.startedAt = System.currentTimeMillis()
                }
            }.apply { isDaemon = true }.start()
            return
        }
        native.execute {
            val lib = CallLibrary.load() ?: return@execute main { fail(call, "Calls aren't available on this device") }
            call.library = lib
            val protocol = protocol(lib) ?: return@execute main { fail(call, "Calls aren't available on this device") }
            main {
                telegram.send(TdApi.CreateCall(userId, protocol, video)) { result ->
                    when (result) {
                        is TdApi.CallId -> call.callId = result.id
                        is TdApi.Error -> fail(call, result.message)
                    }
                }
            }
        }
    }

    fun onUpdate(telegram: Telegram, update: TdApi.Call) {
        val call = current
        if (call == null || (call.telegram == telegram && call.callId == 0 && call.outgoing && update.isOutgoing && update.userId == call.userId)) {
            if (call == null) {
                if (update.isOutgoing || update.state !is TdApi.CallStatePending) return
                if (settings?.acceptCalls?.value == false) return
                val incoming = ActiveCall(telegram, update.userId, outgoing = false, video = update.isVideo)
                incoming.callId = update.id
                current = incoming
                ring(incoming)
                Notifications.flash()
                return apply(incoming, update.state)
            }
            call.callId = update.id
        }
        val active = current ?: return
        if (active.telegram != telegram || active.callId != update.id) {
            if (!update.isOutgoing && update.state is TdApi.CallStatePending) {
                telegram.send(TdApi.DiscardCall(update.id, false, "", 0, update.isVideo, 0))
            }
            return
        }
        apply(active, update.state)
    }

    private fun apply(call: ActiveCall, state: TdApi.CallState) {
        when (state) {
            is TdApi.CallStatePending -> if (call.outgoing) call.phase = CallPhase.Ringing
            is TdApi.CallStateExchangingKeys -> call.phase = CallPhase.Exchanging
            is TdApi.CallStateReady -> {
                call.emojis = state.emojis.toList()
                if (call.handle == null) {
                    call.phase = CallPhase.Connecting
                    native.execute { connect(call, state) }
                }
            }
            is TdApi.CallStateHangingUp -> call.phase = CallPhase.Ended
            is TdApi.CallStateDiscarded -> finish(call, reason(state.reason))
            is TdApi.CallStateError -> finish(call, state.error.message)
        }
    }

    private fun reason(reason: TdApi.CallDiscardReason?): String = when (reason) {
        is TdApi.CallDiscardReasonDeclined -> "Call declined"
        is TdApi.CallDiscardReasonMissed -> "No answer"
        is TdApi.CallDiscardReasonDisconnected -> "Disconnected"
        else -> "Call ended"
    }

    fun onSignaling(telegram: Telegram, callId: Int, data: ByteArray) {
        val call = current ?: return
        if (call.telegram != telegram || call.callId != callId) return
        native.execute {
            val handle = call.handle ?: return@execute
            call.library?.ntg_send_signaling_data(handle, call.userId, data, SizeT(data.size.toLong()))
        }
    }

    fun accept() {
        val call = current ?: return
        if (call.phase != CallPhase.Incoming) return
        call.phase = CallPhase.Preparing
        native.execute {
            val lib = CallLibrary.load() ?: return@execute main { decline() }
            call.library = lib
            val protocol = protocol(lib) ?: return@execute main { decline() }
            main {
                call.telegram.send(TdApi.AcceptCall(call.callId, protocol)) { result ->
                    if (result is TdApi.Error) fail(call, result.message)
                }
            }
        }
    }

    fun decline() = hangUp()

    fun hangUp() {
        val call = current ?: return
        if (call.callId != 0) {
            val duration = if (call.startedAt > 0) ((System.currentTimeMillis() - call.startedAt) / 1000).toInt() else 0
            call.telegram.send(TdApi.DiscardCall(call.callId, false, "", duration, call.camera || call.isVideo, 0))
        }
        finish(call, "Call ended")
    }

    fun toggleMute() {
        val call = current ?: return
        val mute = !call.muted
        call.muted = mute
        native.execute {
            val handle = call.handle ?: return@execute
            val lib = call.library ?: return@execute
            val out = ByteByReference()
            if (mute) lib.ntg_mute(handle, call.userId, out) else lib.ntg_unmute(handle, call.userId, out)
        }
    }

    fun toggleCamera() {
        val call = current ?: return
        call.camera = !call.camera
        if (call.camera) call.screen = null
        if (!call.camera) call.localFrame = null
        native.execute { capture(call) }
    }

    fun share(device: CallDevice?) {
        val call = current ?: return
        call.screen = device
        if (device != null) call.camera = false
        if (device == null) call.localFrame = null
        native.execute { capture(call) }
    }

    fun screens(): List<CallDevice> = if (demo) listOf(CallDevice("Display 1 (2560×1440)", ""), CallDevice("Display 2 (1680×1050)", "")) else devices()?.screens.orEmpty()

    private class DeviceLists(val microphones: List<CallDevice>, val speakers: List<CallDevice>, val cameras: List<CallDevice>, val screens: List<CallDevice>)

    private fun devices(): DeviceLists? {
        val lib = CallLibrary.load() ?: return null
        val devices = NtgDevices()
        if (lib.ntg_get_media_devices(devices) != Ok) return null
        fun list(pointer: Pointer?, size: SizeT): List<CallDevice> {
            if (pointer == null || size.toInt() == 0) return emptyList()
            val first = NtgDeviceInfo(pointer)
            return (first.toArray(size.toInt()) as Array<*>).mapNotNull { item ->
                val info = item as NtgDeviceInfo
                info.read()
                val name = info.name?.getString(0, "UTF-8") ?: return@mapNotNull null
                CallDevice(name, info.metadata?.getString(0, "UTF-8").orEmpty())
            }
        }
        return try {
            DeviceLists(
                list(devices.microphone, devices.microphone_len),
                list(devices.speaker, devices.speaker_len),
                list(devices.camera, devices.camera_len),
                list(devices.screen, devices.screen_len),
            )
        } finally {
            lib.ntg_media_devices_free(devices)
        }
    }

    private fun pick(list: List<CallDevice>, preferred: String?): CallDevice? =
        list.firstOrNull { !preferred.isNullOrBlank() && (it.name == preferred || it.name.contains(preferred) || preferred.contains(it.name)) }
            ?: list.firstOrNull()

    private fun protocol(lib: NtgLibrary): TdApi.CallProtocol? {
        val raw = NtgProtocol()
        if (lib.ntg_get_protocol(raw) != Ok) return null
        return try {
            val versions = raw.library_versions?.getStringArray(0, raw.library_versions_len.toInt()).orEmpty()
            TdApi.CallProtocol(raw.udp_p2p.toInt() != 0, raw.udp_reflector.toInt() != 0, raw.min_layer, raw.max_layer, versions)
        } finally {
            lib.ntg_protocol_free(raw)
        }
    }

    private fun connect(call: ActiveCall, state: TdApi.CallStateReady) {
        val lib = call.library ?: CallLibrary.load() ?: return main { fail(call, "Calls aren't available on this device") }
        call.library = lib
        val handle = lib.ntg_instance_create()
        call.handle = handle
        val signaling = NtgSignalingCallback { _, _, data, length, _ ->
            val bytes = data?.getByteArray(0, length.toInt()) ?: return@NtgSignalingCallback
            main { call.telegram.send(TdApi.SendCallSignalingData(call.callId, bytes)) }
        }
        val connection = NtgConnectionCallback { _, _, info, _ ->
            main {
                when (info.state) {
                    NtgState.Connected -> if (call.phase != CallPhase.Ended) {
                        call.phase = CallPhase.Active
                        if (call.startedAt == 0L) call.startedAt = System.currentTimeMillis()
                    }
                    NtgState.Failed, NtgState.Timeout -> fail(call, "Couldn't connect")
                }
            }
        }
        val frames = NtgFramesCallback { _, _, mode, device, pointer, count, _ ->
            if (pointer == null || count.toInt() == 0 || device == NtgDevice.Microphone || device == NtgDevice.Speaker) return@NtgFramesCallback
            val now = System.currentTimeMillis()
            val remote = mode == NtgMode.Playback
            if (now - (if (remote) call.lastRemote else call.lastLocal) < FrameEveryMs) return@NtgFramesCallback
            val frame = NtgFrame(pointer)
            frame.read()
            val bitmap = i420(frame) ?: return@NtgFramesCallback
            if (remote) call.lastRemote = now else call.lastLocal = now
            main {
                if (remote) {
                    call.remoteFrame = bitmap
                    call.remoteVideo = true
                } else {
                    call.localFrame = bitmap
                }
            }
        }
        val remoteSource = NtgRemoteSourceCallback { _, _, source, _ ->
            main {
                when (source.device) {
                    NtgDevice.Microphone -> call.remoteMuted = source.state != 0
                    NtgDevice.Camera, NtgDevice.Screen -> if (source.state != 0) {
                        call.remoteVideo = false
                        call.remoteFrame = null
                    }
                }
            }
        }
        call.callbacks.addAll(listOf(signaling, connection, frames, remoteSource))
        lib.ntg_on_signaling_data_callback(handle, signaling, null)
        lib.ntg_on_connection_change_callback(handle, connection, null)
        lib.ntg_on_frames_callback(handle, frames, null)
        lib.ntg_on_remote_source_change_callback(handle, remoteSource, null)
        if (lib.ntg_create_p2p_call(handle, call.userId) != Ok) return main { fail(call, lib.ntg_last_error() ?: "Couldn't start the call") }
        capture(call)
        playback(call)
        val key = state.encryptionKey
        if (lib.ntg_skip_exchange(handle, call.userId, key, SizeT(key.size.toLong()), call.outgoing) != Ok) {
            return main { fail(call, lib.ntg_last_error() ?: "Couldn't start the call") }
        }
        val servers = servers(state.servers)
        val versions = state.protocol.libraryVersions
        val result = lib.ntg_connect_p2p(
            handle,
            call.userId,
            servers?.firstOrNull(),
            SizeT((servers?.size ?: 0).toLong()),
            versions,
            SizeT(versions.size.toLong()),
            state.allowP2p,
            state.customParameters.ifBlank { "{}" },
        )
        if (result != Ok) main { fail(call, lib.ntg_last_error() ?: "Couldn't connect") }
    }

    private fun servers(list: Array<TdApi.CallServer>): Array<NtgServer>? {
        if (list.isEmpty()) return null
        @Suppress("UNCHECKED_CAST")
        val array = NtgServer().toArray(list.size) as Array<NtgServer>
        list.forEachIndexed { index, server ->
            val target = array[index]
            target.id = server.id
            target.ipv4 = server.ipAddress.takeIf { it.isNotBlank() }
            target.ipv6 = server.ipv6Address.takeIf { it.isNotBlank() }
            target.port = server.port.toShort()
            when (val type = server.type) {
                is TdApi.CallServerTypeTelegramReflector -> {
                    target.tcp = if (type.isTcp) 1 else 0
                    if (type.peerTag.isNotEmpty()) {
                        target.peer_tag = Memory(type.peerTag.size.toLong()).apply { write(0, type.peerTag, 0, type.peerTag.size) }
                        target.peer_tag_len = SizeT(type.peerTag.size.toLong())
                    }
                }
                is TdApi.CallServerTypeWebrtc -> {
                    target.username = type.username
                    target.password = type.password
                    target.turn = if (type.supportsTurn) 1 else 0
                    target.stun = if (type.supportsStun) 1 else 0
                }
            }
            target.write()
        }
        return array
    }

    private fun capture(call: ActiveCall) {
        val handle = call.handle ?: return
        val lib = call.library ?: return
        val lists = devices()
        val media = NtgMedia.ByValue()
        pick(lists?.microphones.orEmpty(), settings?.inputDevice?.value)?.let { device ->
            media.microphone = NtgAudio.ByReference().apply {
                media_source = NtgSource.Device
                sample_rate = SampleRate
                channel_count = Channels
                input = device.metadata
            }
        }
        val screen = call.screen
        if (screen != null) {
            media.screen = NtgVideo.ByReference().apply {
                media_source = NtgSource.Desktop
                width = ScreenWidth
                height = ScreenHeight
                fps = Fps
                input = screen.metadata
            }
        } else if (call.camera) {
            pick(lists?.cameras.orEmpty(), settings?.cameraDevice?.value)?.let { device ->
                media.camera = NtgVideo.ByReference().apply {
                    media_source = NtgSource.Device
                    width = CameraWidth
                    height = CameraHeight
                    fps = Fps
                    input = device.metadata
                }
            } ?: main {
                call.camera = false
                call.status = "No camera found"
            }
        }
        if (lib.ntg_set_stream_sources(handle, call.userId, NtgMode.Capture, media) != Ok) {
            AppLog.write("Call capture failed: ${lib.ntg_last_error()}")
        }
        if (call.muted) lib.ntg_mute(handle, call.userId, ByteByReference())
    }

    private fun playback(call: ActiveCall) {
        val handle = call.handle ?: return
        val lib = call.library ?: return
        val media = NtgMedia.ByValue()
        pick(devices()?.speakers.orEmpty(), settings?.outputDevice?.value)?.let { device ->
            media.speaker = NtgAudio.ByReference().apply {
                media_source = NtgSource.Device
                sample_rate = SampleRate
                channel_count = Channels
                input = device.metadata
            }
        }
        media.camera = NtgVideo.ByReference().apply {
            media_source = NtgSource.External
            width = -1
            height = -1
            fps = Fps
        }
        media.screen = NtgVideo.ByReference().apply {
            media_source = NtgSource.External
            width = -1
            height = -1
            fps = Fps
        }
        if (lib.ntg_set_stream_sources(handle, call.userId, NtgMode.Playback, media) != Ok) {
            AppLog.write("Call playback failed: ${lib.ntg_last_error()}")
        }
    }

    private fun ring(call: ActiveCall) {
        Thread {
            while (current == call && call.phase == CallPhase.Incoming) {
                Sounds.newMessage(settings?.notificationVolume?.value ?: Sounds.MaxVolume)
                Thread.sleep(RingEveryMs)
            }
        }.apply { isDaemon = true }.start()
    }

    private fun fail(call: ActiveCall, message: String) {
        if (call.callId != 0 && call.phase != CallPhase.Ended) {
            call.telegram.send(TdApi.DiscardCall(call.callId, true, "", 0, call.isVideo, 0))
        }
        finish(call, message)
    }

    private fun finish(call: ActiveCall, message: String) {
        if (current != call) return
        call.phase = CallPhase.Ended
        call.status = message
        native.execute {
            val handle = call.handle ?: return@execute
            val lib = call.library ?: return@execute
            runCatching { lib.ntg_stop(handle, call.userId) }
            runCatching { lib.ntg_instance_destroy(handle) }
            call.handle = null
        }
        Thread {
            Thread.sleep(EndedHoldMs)
            main { if (current == call) current = null }
        }.apply { isDaemon = true }.start()
    }

    private fun i420(frame: NtgFrame): ImageBitmap? {
        val width = frame.frame_data.width.toInt() and 0xFFFF
        val height = frame.frame_data.height.toInt() and 0xFFFF
        val data = frame.data ?: return null
        if (width <= 0 || height <= 0) return null
        val chromaWidth = (width + 1) / 2
        val chromaHeight = (height + 1) / 2
        val ySize = width * height
        val uvSize = chromaWidth * chromaHeight
        if (frame.data_len.toLong() < ySize + uvSize * 2L) return null
        val bytes = data.getByteArray(0, ySize + uvSize * 2)
        val step = if (width > MaxFrameWidth) 2 else 1
        val outWidth = width / step
        val outHeight = height / step
        val pixels = ByteArray(outWidth * outHeight * 4)
        var index = 0
        for (row in 0 until outHeight) {
            val y = row * step
            val yRow = y * width
            val uvRow = (y / 2) * chromaWidth
            for (column in 0 until outWidth) {
                val x = column * step
                val luma = (bytes[yRow + x].toInt() and 0xFF) - 16
                val u = (bytes[ySize + uvRow + x / 2].toInt() and 0xFF) - 128
                val v = (bytes[ySize + uvSize + uvRow + x / 2].toInt() and 0xFF) - 128
                val c = 298 * luma
                pixels[index] = ((c + 516 * u + 128) shr 8).coerceIn(0, 255).toByte()
                pixels[index + 1] = ((c - 100 * u - 208 * v + 128) shr 8).coerceIn(0, 255).toByte()
                pixels[index + 2] = ((c + 409 * v + 128) shr 8).coerceIn(0, 255).toByte()
                pixels[index + 3] = 0xFF.toByte()
                index += 4
            }
        }
        val info = ImageInfo(outWidth, outHeight, ColorType.BGRA_8888, ColorAlphaType.OPAQUE)
        return Image.makeRaster(info, pixels, outWidth * 4).toComposeImageBitmap()
    }
}
