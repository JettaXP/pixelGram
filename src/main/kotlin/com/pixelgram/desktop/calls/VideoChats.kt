package com.pixelgram.desktop.calls

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.pixelgram.desktop.core.AppLog
import com.pixelgram.desktop.telegram.ChatState
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.telegram.friendlyError
import com.sun.jna.Memory
import com.sun.jna.Pointer
import com.sun.jna.ptr.ByteByReference
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.PointerByReference
import it.tdlight.jni.TdApi
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

enum class VideoChatPhase { Joining, Connecting, Active, Ended }

fun senderKey(sender: TdApi.MessageSender?): Long = when (sender) {
    is TdApi.MessageSenderUser -> sender.userId
    is TdApi.MessageSenderChat -> sender.chatId
    else -> 0L
}

class ActiveVideoChat(val telegram: Telegram, val chatId: Long, val groupCallId: Int) {
    var phase by mutableStateOf(VideoChatPhase.Joining)
    var muted by mutableStateOf(true)
    var camera by mutableStateOf(false)
    var screen by mutableStateOf<CallDevice?>(null)
    var status by mutableStateOf<String?>(null)
    var startedAt by mutableLongStateOf(0L)
    var stream by mutableStateOf(false)
    var cameraFrame by mutableStateOf<ImageBitmap?>(null)
    var screenFrame by mutableStateOf<ImageBitmap?>(null)
    var streamFrame by mutableStateOf<ImageBitmap?>(null)
    val participants = mutableStateMapOf<Long, TdApi.GroupCallParticipant>()
    val frames = mutableStateMapOf<String, ImageBitmap>()

    val call: TdApi.GroupCall? get() = telegram.groupCalls[groupCallId]
    val me: TdApi.GroupCallParticipant? get() = participants.values.firstOrNull { it.isCurrentUser }
    val handRaised: Boolean get() = me?.isHandRaised == true
    val canSpeak: Boolean get() = me?.let { !it.isMutedForAllUsers || it.canUnmuteSelf } ?: true

    @Volatile internal var handle: Pointer? = null
    @Volatile internal var library: NtgLibrary? = null
    @Volatile internal var connected = false
    @Volatile internal var presenting = false
    internal val callbacks = ArrayList<Any>()
    internal val endpoints = HashMap<String, Long>()
    internal val ssrcs = ConcurrentHashMap<Long, String>()
    internal val lastFrame = ConcurrentHashMap<String, Long>()
    internal val scales = ConcurrentHashMap<Int, Int>()
    private val retired = HashMap<String, ArrayDeque<Image>>()

    internal fun retire(key: String, image: Image) {
        val queue = retired.getOrPut(key) { ArrayDeque() }
        queue.addLast(image)
        while (queue.size > KeptFrames) queue.removeFirst().close()
    }

    internal fun releaseFrames() {
        frames.clear()
        cameraFrame = null
        screenFrame = null
        streamFrame = null
        retired.values.forEach { queue -> queue.forEach { it.close() } }
        retired.clear()
    }

    private companion object {
        const val KeptFrames = 6
    }
}

object VideoChats {
    private const val Ok = 0
    private const val FrameEveryMs = 33L
    private const val EndedHoldMs = 2500L
    private const val ReleaseDelayMs = 1000L
    private const val ParticipantsPage = 100
    private const val StreamKey = "stream"
    private const val CameraKey = "camera"
    private const val ScreenKey = "screen"
    private const val UnsignedMask = 0xFFFFFFFFL
    private val SourceId = Regex("\"ssrc\"\\s*:\\s*(\\d+)")

    var current by mutableStateOf<ActiveVideoChat?>(null)
        private set
    var raise by mutableIntStateOf(0)
        private set

    private val native = Executors.newSingleThreadExecutor { Thread(it, "video-chat").apply { isDaemon = true } }

    private fun main(block: () -> Unit) = javax.swing.SwingUtilities.invokeLater(block)

    private val demo: Boolean get() = System.getProperty("pixelgram.demo") == "true"

    fun isJoined(telegram: Telegram, groupCallId: Int): Boolean =
        current?.let { it.telegram == telegram && it.groupCallId == groupCallId && it.phase != VideoChatPhase.Ended } == true

    fun join(telegram: Telegram, chat: ChatState, groupCallId: Int = chat.videoChat?.groupCallId ?: 0): String? {
        val id = groupCallId
        if (id == 0) return null
        current?.let { active ->
            if (active.telegram == telegram && active.groupCallId == id && active.phase != VideoChatPhase.Ended) {
                raise++
                return null
            }
            return "Leave the current video chat first"
        }
        if (Calls.current != null) return "Finish the current call first"
        if (!demo && !CallLibrary.supported) return "Calls aren't available on this device"
        AppLog.trace("join video chat")
        val call = ActiveVideoChat(telegram, chat.id, id)
        current = call
        if (demo) {
            VideoChatDemo.start(call)
            return null
        }
        telegram.send(TdApi.GetGroupCall(id)) { result ->
            if (result is TdApi.GroupCall) telegram.groupCalls[id] = result
        }
        native.execute { connect(call) }
        return null
    }

    fun onGroupCall(telegram: Telegram, groupCall: TdApi.GroupCall) {
        val call = current ?: return
        if (call.telegram != telegram || call.groupCallId != groupCall.id || call.phase == VideoChatPhase.Ended) return
        when {
            !groupCall.isActive -> finish(call, ended(call), EndedHoldMs)
            groupCall.needRejoin && call.connected -> rejoin(call)
        }
    }

    fun onParticipant(telegram: Telegram, groupCallId: Int, participant: TdApi.GroupCallParticipant) {
        val call = current ?: return
        if (call.telegram != telegram || call.groupCallId != groupCallId || call.phase == VideoChatPhase.Ended) return
        val key = senderKey(participant.participantId)
        if (participant.order.isNullOrEmpty()) call.participants.remove(key) else call.participants[key] = participant
        if (participant.isCurrentUser) {
            val mutedNow = participant.isMutedForAllUsers || participant.isMutedForCurrentUser
            if (mutedNow && !call.muted) {
                call.muted = true
                native.execute { call.handle?.let { call.library?.ntg_mute(it, call.chatId, ByteByReference()) } }
            }
        }
        syncVideos(call)
    }

    fun loadMore() {
        val call = current ?: return
        if (demo || call.call?.loadedAllParticipants == true) return
        call.telegram.send(TdApi.LoadGroupCallParticipants(call.groupCallId, ParticipantsPage))
    }

    private fun mySender(call: ActiveVideoChat): TdApi.MessageSender =
        call.me?.participantId ?: TdApi.MessageSenderUser(call.telegram.me?.id ?: 0L)

    fun toggleMute() {
        val call = current ?: return
        if (call.muted && !call.canSpeak) return
        val mute = !call.muted
        call.muted = mute
        if (demo) return VideoChatDemo.updateMe(call)
        call.telegram.send(TdApi.ToggleGroupCallParticipantIsMuted(call.groupCallId, mySender(call), mute))
        native.execute {
            val handle = call.handle ?: return@execute
            val lib = call.library ?: return@execute
            if (mute) lib.ntg_mute(handle, call.chatId, ByteByReference()) else lib.ntg_unmute(handle, call.chatId, ByteByReference())
        }
    }

    fun toggleHand() {
        val call = current ?: return
        val raised = !call.handRaised
        if (demo) return VideoChatDemo.raiseHand(call, raised)
        call.telegram.send(TdApi.ToggleGroupCallParticipantIsHandRaised(call.groupCallId, mySender(call), raised))
    }

    fun toggleCamera() {
        val call = current ?: return
        call.camera = !call.camera
        if (!call.camera) call.cameraFrame = null
        if (demo) return VideoChatDemo.updateMe(call)
        call.telegram.send(TdApi.ToggleGroupCallIsMyVideoEnabled(call.groupCallId, call.camera))
        native.execute { capture(call) }
    }

    fun share(device: CallDevice?) {
        val call = current ?: return
        call.screen = device
        if (device == null) call.screenFrame = null
        if (demo) return VideoChatDemo.updateMe(call)
        native.execute {
            capture(call)
            if (device != null && !call.presenting) present(call) else if (device == null && call.presenting) stopPresenting(call)
        }
    }

    fun screens(): List<CallDevice> = Calls.screens()

    fun setMuted(participant: TdApi.GroupCallParticipant, muted: Boolean) {
        val call = current ?: return
        if (demo) return VideoChatDemo.edit(call, participant) { isMutedForAllUsers = muted; canUnmuteSelf = !muted; isSpeaking = false }
        call.telegram.send(TdApi.ToggleGroupCallParticipantIsMuted(call.groupCallId, participant.participantId, muted))
    }

    fun setVolume(participant: TdApi.GroupCallParticipant, level: Int) {
        val call = current ?: return
        if (demo) return VideoChatDemo.edit(call, participant) { volumeLevel = level }
        call.telegram.send(TdApi.SetGroupCallParticipantVolumeLevel(call.groupCallId, participant.participantId, level))
    }

    fun lowerHand(participant: TdApi.GroupCallParticipant) {
        val call = current ?: return
        if (demo) return VideoChatDemo.edit(call, participant) { isHandRaised = false }
        call.telegram.send(TdApi.ToggleGroupCallParticipantIsHandRaised(call.groupCallId, participant.participantId, false))
    }

    fun leave() {
        val call = current ?: return
        if (!demo && call.phase != VideoChatPhase.Ended) call.telegram.send(TdApi.LeaveGroupCall(call.groupCallId))
        finish(call, "You left the video chat", 0L)
    }

    fun endForAll() {
        val call = current ?: return
        if (demo) {
            VideoChatDemo.end(call)
        } else {
            call.telegram.send(TdApi.EndGroupCall(call.groupCallId))
        }
        finish(call, ended(call), EndedHoldMs)
    }

    private fun connect(call: ActiveVideoChat) {
        val lib = CallLibrary.load() ?: return main { fail(call, "Calls aren't available on this device") }
        call.library = lib
        val handle = lib.ntg_instance_create()
        call.handle = handle
        register(call, lib, handle)
        val params = takeString(lib) { lib.ntg_create_call(handle, call.chatId, it) }
            ?: return main { fail(call, lib.ntg_last_error() ?: "Couldn't join the video chat") }
        capture(call)
        if (lib.ntg_set_stream_sources(handle, call.chatId, NtgMode.Playback, Calls.playbackMedia()) != Ok) {
            AppLog.write("Video chat playback failed: ${lib.ntg_last_error()}")
        }
        val source = sourceId(params)
        main {
            if (current != call || call.phase == VideoChatPhase.Ended) return@main
            val join = TdApi.GroupCallJoinParameters(source, params, call.muted, call.camera)
            call.telegram.send(TdApi.JoinVideoChat(call.groupCallId, null, join, "")) { result ->
                when (result) {
                    is TdApi.Text -> native.execute { joined(call, result.text) }
                    is TdApi.Error -> fail(call, friendlyError(result.message))
                }
            }
        }
    }

    private fun joined(call: ActiveVideoChat, response: String) {
        val handle = call.handle ?: return
        val lib = call.library ?: return
        if (lib.ntg_connect(handle, call.chatId, response, false) != Ok) {
            return main { fail(call, lib.ntg_last_error() ?: "Couldn't connect") }
        }
        val mode = IntByReference()
        val stream = lib.ntg_get_connection_mode(handle, call.chatId, mode) == Ok &&
            (mode.value == NtgConnectionMode.Stream || mode.value == NtgConnectionMode.Rtmp)
        call.connected = true
        main {
            if (current != call || call.phase == VideoChatPhase.Ended) return@main
            call.stream = stream
            if (stream || call.phase == VideoChatPhase.Joining) call.phase = if (stream) VideoChatPhase.Active else VideoChatPhase.Connecting
            if (stream && call.startedAt == 0L) call.startedAt = System.currentTimeMillis()
            call.telegram.send(TdApi.LoadGroupCallParticipants(call.groupCallId, ParticipantsPage))
            syncVideos(call)
        }
    }

    private fun rejoin(call: ActiveVideoChat) {
        call.connected = false
        call.presenting = false
        call.endpoints.clear()
        call.frames.clear()
        call.phase = VideoChatPhase.Joining
        native.execute {
            val handle = call.handle
            val lib = call.library
            if (handle != null && lib != null) {
                runCatching { lib.ntg_stop(handle, call.chatId) }
                runCatching { lib.ntg_instance_destroy(handle) }
            }
            call.handle = null
            call.ssrcs.clear()
            call.callbacks.clear()
            connect(call)
            if (call.screen != null) present(call)
        }
    }

    private fun register(call: ActiveVideoChat, lib: NtgLibrary, handle: Pointer) {
        val connection = NtgConnectionCallback { _, _, info, _ ->
            main {
                if (current != call || call.phase == VideoChatPhase.Ended) return@main
                val presentation = info.kind == 1
                when (info.state) {
                    NtgState.Connected -> if (!presentation && call.phase != VideoChatPhase.Active) {
                        call.phase = VideoChatPhase.Active
                        if (call.startedAt == 0L) call.startedAt = System.currentTimeMillis()
                    }
                    NtgState.Failed, NtgState.Timeout -> if (presentation) {
                        call.screen = null
                        call.screenFrame = null
                        call.presenting = false
                        call.status = "Screen sharing stopped"
                    } else {
                        fail(call, "Couldn't connect")
                    }
                }
            }
        }
        val frames = NtgFramesCallback { _, _, mode, device, pointer, count, _ ->
            if (pointer == null || count.toInt() == 0 || device == NtgDevice.Microphone || device == NtgDevice.Speaker) return@NtgFramesCallback
            val remote = mode == NtgMode.Playback
            NtgFrame(pointer).toArray(count.toInt()).forEach { item ->
                val frame = item as NtgFrame
                frame.read()
                val key = when {
                    !remote -> if (device == NtgDevice.Screen) ScreenKey else CameraKey
                    else -> call.ssrcs[frame.ssrc and UnsignedMask] ?: StreamKey
                }
                val now = System.currentTimeMillis()
                if (now - (call.lastFrame[key] ?: 0L) < FrameEveryMs) return@forEach
                val image = Calls.i420(frame) ?: return@forEach
                val bitmap = image.toComposeImageBitmap()
                call.lastFrame[key] = now
                main { show(call, key, image, bitmap) }
            }
        }
        val remoteSource = NtgRemoteSourceCallback { _, _, source, _ ->
            if (source.device != NtgDevice.Camera && source.device != NtgDevice.Screen) return@NtgRemoteSourceCallback
            if (source.state == 0) return@NtgRemoteSourceCallback
            val endpoint = call.ssrcs[source.ssrc.toLong() and UnsignedMask] ?: return@NtgRemoteSourceCallback
            main { call.frames.remove(endpoint) }
        }
        val timestamp = NtgBroadcastTimestampCallback { _, _, _ ->
            main { requestTimestamp(call) }
        }
        val part = NtgBroadcastPartCallback { _, _, request, _ ->
            val segment = request.segment_id
            val partId = request.part_id
            val time = request.timestamp
            val qualityUpdate = request.quality_update.toInt() != 0
            val channel = request.channel_id
            val quality = request.quality
            main { requestPart(call, segment, partId, time, qualityUpdate, channel, quality) }
        }
        val participants = NtgParticipantsCallback { _, _, request, _ ->
            val size = request.ssrcs_len.toInt()
            val sources = request.ssrcs?.takeIf { size > 0 }?.getIntArray(0, size) ?: return@NtgParticipantsCallback
            main { mapSources(call, sources) }
        }
        call.callbacks.addAll(listOf(connection, frames, remoteSource, timestamp, part, participants))
        lib.ntg_on_connection_change_callback(handle, connection, null)
        lib.ntg_on_frames_callback(handle, frames, null)
        lib.ntg_on_remote_source_change_callback(handle, remoteSource, null)
        lib.ntg_on_request_broadcast_timestamp_callback(handle, timestamp, null)
        lib.ntg_on_request_broadcast_part_callback(handle, part, null)
        lib.ntg_on_request_participants_callback(handle, participants, null)
    }

    private fun show(call: ActiveVideoChat, key: String, image: Image, bitmap: ImageBitmap) {
        if (current != call || call.phase == VideoChatPhase.Ended) {
            image.close()
            return
        }
        when (key) {
            CameraKey -> if (call.camera) call.cameraFrame = bitmap
            ScreenKey -> if (call.screen != null) call.screenFrame = bitmap
            StreamKey -> call.streamFrame = bitmap
            else -> if (key in call.endpoints) call.frames[key] = bitmap
        }
        call.retire(key, image)
    }

    private fun requestTimestamp(call: ActiveVideoChat) {
        call.telegram.send(TdApi.GetGroupCallStreams(call.groupCallId)) { result ->
            val streams = (result as? TdApi.GroupCallStreams)?.streams.orEmpty()
            streams.forEach { call.scales[it.channelId] = it.scale }
            val time = streams.maxOfOrNull { it.timeOffset } ?: 0L
            native.execute {
                val handle = call.handle ?: return@execute
                call.library?.ntg_send_broadcast_timestamp(handle, call.chatId, time)
            }
        }
    }

    private fun requestPart(call: ActiveVideoChat, segment: Long, part: Int, time: Long, qualityUpdate: Boolean, channel: Int, quality: Int) {
        val videoQuality: TdApi.GroupCallVideoQuality? = when (quality) {
            NtgQuality.Thumbnail -> TdApi.GroupCallVideoQualityThumbnail()
            NtgQuality.Medium -> TdApi.GroupCallVideoQualityMedium()
            NtgQuality.Full -> TdApi.GroupCallVideoQualityFull()
            else -> null
        }
        val scale = call.scales[channel] ?: 0
        call.telegram.send(TdApi.GetGroupCallStreamSegment(call.groupCallId, time, scale, if (videoQuality == null) 0 else channel, videoQuality)) { result ->
            val data = (result as? TdApi.Data)?.data
            val status = when {
                data != null -> NtgPartStatus.Success
                (result as? TdApi.Error)?.message?.contains("TIME_TOO_BIG") == true -> NtgPartStatus.NotReady
                else -> NtgPartStatus.ResyncNeeded
            }
            native.execute {
                val handle = call.handle ?: return@execute
                call.library?.ntg_send_broadcast_part(handle, call.chatId, segment, part, status, qualityUpdate, data, SizeT((data?.size ?: 0).toLong()))
            }
        }
    }

    private fun mapSources(call: ActiveVideoChat, sources: IntArray) {
        val known = sources.toList().mapNotNull { ssrc ->
            call.participants.values.firstOrNull { it.audioSourceId == ssrc || it.screenSharingAudioSourceId == ssrc }
                ?.let { senderKey(it.participantId) to ssrc }
        }
        if (known.isEmpty()) return
        native.execute {
            val handle = call.handle ?: return@execute
            val lib = call.library ?: return@execute
            @Suppress("UNCHECKED_CAST")
            val array = NtgSsrcMapping().toArray(known.size) as Array<NtgSsrcMapping>
            known.forEachIndexed { index, (user, ssrc) ->
                array[index].user_id = user
                array[index].ssrc = ssrc
                array[index].write()
            }
            lib.ntg_update_audio_ssrc_mappings(handle, call.chatId, array[0], SizeT(known.size.toLong()))
        }
    }

    private fun syncVideos(call: ActiveVideoChat) {
        if (!call.connected || call.stream || call.phase == VideoChatPhase.Ended) return
        val wanted = HashMap<String, Pair<Long, TdApi.GroupCallParticipantVideoInfo>>()
        call.participants.values.filter { !it.isCurrentUser }.forEach { participant ->
            listOfNotNull(participant.videoInfo, participant.screenSharingVideoInfo).forEach { info ->
                if (!info.endpointId.isNullOrBlank() && !info.isPaused) wanted[info.endpointId] = senderKey(participant.participantId) to info
            }
        }
        (call.endpoints.keys - wanted.keys).forEach { endpoint ->
            call.endpoints.remove(endpoint)
            call.frames.remove(endpoint)
            native.execute {
                val handle = call.handle ?: return@execute
                call.library?.ntg_remove_incoming_video(handle, call.chatId, endpoint, ByteByReference())
                call.ssrcs.entries.removeIf { it.value == endpoint }
            }
        }
        wanted.filterKeys { it !in call.endpoints }.forEach { (endpoint, pair) ->
            val (owner, info) = pair
            call.endpoints[endpoint] = owner
            native.execute { addVideo(call, owner, endpoint, info) }
        }
    }

    private fun addVideo(call: ActiveVideoChat, owner: Long, endpoint: String, info: TdApi.GroupCallParticipantVideoInfo) {
        val handle = call.handle ?: return
        val lib = call.library ?: return
        val groups = info.sourceGroups.orEmpty().filter { it.sourceIds.isNotEmpty() }
        groups.forEach { group -> group.sourceIds.forEach { call.ssrcs[it.toLong() and UnsignedMask] = endpoint } }
        @Suppress("UNCHECKED_CAST")
        val array = if (groups.isEmpty()) null else NtgSsrcGroup().toArray(groups.size) as Array<NtgSsrcGroup>
        groups.forEachIndexed { index, group ->
            val target = array!![index]
            target.semantics = group.semantics
            target.ssrcs = Memory(group.sourceIds.size * 4L).apply { group.sourceIds.forEachIndexed { at, id -> setInt(at * 4L, id) } }
            target.ssrcs_len = SizeT(group.sourceIds.size.toLong())
            target.write()
        }
        val out = IntByReference()
        val added = lib.ntg_add_incoming_video(handle, call.chatId, owner, endpoint, array?.firstOrNull(), SizeT(groups.size.toLong()), out)
        java.lang.ref.Reference.reachabilityFence(array)
        if (added == Ok) {
            call.ssrcs[out.value.toLong() and UnsignedMask] = endpoint
        } else {
            AppLog.write("Video chat incoming video failed: ${lib.ntg_last_error()}")
        }
    }

    private fun present(call: ActiveVideoChat) {
        val handle = call.handle ?: return
        val lib = call.library ?: return
        val params = takeString(lib) { lib.ntg_init_presentation(handle, call.chatId, it) } ?: return main {
            call.screen = null
            call.status = lib.ntg_last_error() ?: "Couldn't share the screen"
        }
        call.presenting = true
        val source = sourceId(params)
        main {
            call.telegram.send(TdApi.StartGroupCallScreenSharing(call.groupCallId, source, params)) { result ->
                when (result) {
                    is TdApi.Text -> native.execute {
                        val current = call.handle ?: return@execute
                        if (lib.ntg_connect(current, call.chatId, result.text, true) != Ok) {
                            main {
                                call.screen = null
                                call.status = lib.ntg_last_error() ?: "Couldn't share the screen"
                            }
                        }
                    }
                    is TdApi.Error -> {
                        call.screen = null
                        call.screenFrame = null
                        call.status = friendlyError(result.message)
                        native.execute { stopPresenting(call, notify = false) }
                    }
                }
            }
        }
    }

    private fun stopPresenting(call: ActiveVideoChat, notify: Boolean = true) {
        if (!call.presenting) return
        call.presenting = false
        call.handle?.let { call.library?.ntg_stop_presentation(it, call.chatId) }
        if (notify) main { call.telegram.send(TdApi.EndGroupCallScreenSharing(call.groupCallId)) }
    }

    private fun capture(call: ActiveVideoChat) {
        val handle = call.handle ?: return
        val lib = call.library ?: return
        val media = Calls.captureMedia(call.camera, call.screen) {
            main {
                call.camera = false
                call.status = "No camera found"
                call.telegram.send(TdApi.ToggleGroupCallIsMyVideoEnabled(call.groupCallId, false))
            }
        }
        if (lib.ntg_set_stream_sources(handle, call.chatId, NtgMode.Capture, media) != Ok) {
            AppLog.write("Video chat capture failed: ${lib.ntg_last_error()}")
        }
        if (call.muted) lib.ntg_mute(handle, call.chatId, ByteByReference())
    }

    private fun takeString(lib: NtgLibrary, block: (PointerByReference) -> Int): String? {
        val out = PointerByReference()
        if (block(out) != Ok) return null
        val pointer = out.value ?: return null
        return try {
            pointer.getString(0, "UTF-8")
        } finally {
            lib.ntg_string_free(pointer)
        }
    }

    private fun sourceId(params: String): Int = SourceId.find(params)?.groupValues?.get(1)?.toLongOrNull()?.toInt() ?: 0

    private fun fail(call: ActiveVideoChat, message: String) {
        if (call.phase != VideoChatPhase.Ended && !demo) call.telegram.send(TdApi.LeaveGroupCall(call.groupCallId))
        finish(call, message, EndedHoldMs)
    }

    private fun finish(call: ActiveVideoChat, message: String, holdMs: Long) {
        if (current != call) return
        call.phase = VideoChatPhase.Ended
        call.status = message
        call.connected = false
        native.execute {
            val handle = call.handle ?: return@execute
            val lib = call.library ?: return@execute
            runCatching { lib.ntg_stop(handle, call.chatId) }
            runCatching { lib.ntg_instance_destroy(handle) }
            call.handle = null
        }
        if (holdMs <= 0L) current = null
        Thread {
            Thread.sleep(holdMs)
            main { if (current == call) current = null }
            Thread.sleep(ReleaseDelayMs)
            main { call.releaseFrames() }
        }.apply { isDaemon = true }.start()
    }

    private fun ended(call: ActiveVideoChat): String =
        if (call.stream || call.call?.isRtmpStream == true) "Live stream ended" else "Video chat ended"

    internal fun demoImage(width: Int, height: Int, phase: Float, hue: Float): Image {
        val pixels = ByteArray(width * height * 4)
        val cx = width * (0.5f + 0.3f * kotlin.math.cos(phase))
        val cy = height * (0.5f + 0.25f * kotlin.math.sin(phase * 1.3f))
        val radius = height * 0.22f
        var index = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                val dx = x - cx
                val dy = y - cy
                val inside = dx * dx + dy * dy < radius * radius
                val shade = (x + y).toFloat() / (width + height)
                val color = java.awt.Color.HSBtoRGB(hue + shade * 0.15f, if (inside) 0.25f else 0.55f, if (inside) 0.98f else 0.35f + shade * 0.4f)
                pixels[index] = (color and 0xFF).toByte()
                pixels[index + 1] = (color shr 8 and 0xFF).toByte()
                pixels[index + 2] = (color shr 16 and 0xFF).toByte()
                pixels[index + 3] = 0xFF.toByte()
                index += 4
            }
        }
        return Image.makeRaster(ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.OPAQUE), pixels, width * 4)
    }

    internal fun demoFrame(call: ActiveVideoChat, key: String, image: Image) = main { show(call, key, image, image.toComposeImageBitmap()) }

    internal fun demoEndpoint(call: ActiveVideoChat, endpoint: String, owner: Long) {
        call.endpoints[endpoint] = owner
    }
}
