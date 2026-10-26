package com.pixelgram.desktop.calls

import com.sun.jna.Callback
import com.sun.jna.IntegerType
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.ptr.ByteByReference
import com.sun.jna.ptr.PointerByReference

class SizeT(value: Long = 0) : IntegerType(Native.SIZE_T_SIZE, value, true) {
    constructor() : this(0)

    override fun toByte(): Byte = toInt().toByte()

    override fun toShort(): Short = toInt().toShort()

    @Deprecated("Number.toChar")
    override fun toChar(): Char = toInt().toChar()
}

object NtgMode {
    const val Capture = 0
    const val Playback = 1
}

object NtgSource {
    const val Device = 4
    const val Desktop = 5
    const val External = 6
}

object NtgDevice {
    const val Microphone = 0
    const val Speaker = 1
    const val Camera = 2
    const val Screen = 3
}

object NtgState {
    const val Connecting = 0
    const val Connected = 1
    const val Failed = 2
    const val Timeout = 3
    const val Closed = 4
}

@Structure.FieldOrder("media_source", "sample_rate", "channel_count", "input", "keep_open")
open class NtgAudio : Structure() {
    @JvmField var media_source: Int = 0
    @JvmField var sample_rate: Int = 0
    @JvmField var channel_count: Byte = 0
    @JvmField var input: String? = null
    @JvmField var keep_open: Byte = 0

    class ByReference : NtgAudio(), Structure.ByReference
}

@Structure.FieldOrder("media_source", "width", "height", "fps", "input", "keep_open")
open class NtgVideo : Structure() {
    @JvmField var media_source: Int = 0
    @JvmField var width: Short = 0
    @JvmField var height: Short = 0
    @JvmField var fps: Byte = 0
    @JvmField var input: String? = null
    @JvmField var keep_open: Byte = 0

    class ByReference : NtgVideo(), Structure.ByReference
}

@Structure.FieldOrder("microphone", "speaker", "camera", "screen")
open class NtgMedia : Structure() {
    @JvmField var microphone: NtgAudio.ByReference? = null
    @JvmField var speaker: NtgAudio.ByReference? = null
    @JvmField var camera: NtgVideo.ByReference? = null
    @JvmField var screen: NtgVideo.ByReference? = null

    class ByValue : NtgMedia(), Structure.ByValue
}

@Structure.FieldOrder("name", "metadata")
open class NtgDeviceInfo(pointer: Pointer? = null) : Structure(pointer) {
    @JvmField var name: Pointer? = null
    @JvmField var metadata: Pointer? = null
}

@Structure.FieldOrder("microphone", "microphone_len", "speaker", "speaker_len", "camera", "camera_len", "screen", "screen_len")
class NtgDevices : Structure() {
    @JvmField var microphone: Pointer? = null
    @JvmField var microphone_len: SizeT = SizeT()
    @JvmField var speaker: Pointer? = null
    @JvmField var speaker_len: SizeT = SizeT()
    @JvmField var camera: Pointer? = null
    @JvmField var camera_len: SizeT = SizeT()
    @JvmField var screen: Pointer? = null
    @JvmField var screen_len: SizeT = SizeT()
}

@Structure.FieldOrder("min_layer", "max_layer", "udp_p2p", "udp_reflector", "library_versions", "library_versions_len")
class NtgProtocol : Structure() {
    @JvmField var min_layer: Int = 0
    @JvmField var max_layer: Int = 0
    @JvmField var udp_p2p: Byte = 0
    @JvmField var udp_reflector: Byte = 0
    @JvmField var library_versions: Pointer? = null
    @JvmField var library_versions_len: SizeT = SizeT()
}

@Structure.FieldOrder("id", "ipv4", "ipv6", "port", "username", "password", "turn", "stun", "tcp", "peer_tag", "peer_tag_len")
open class NtgServer(pointer: Pointer? = null) : Structure(pointer) {
    @JvmField var id: Long = 0
    @JvmField var ipv4: String? = null
    @JvmField var ipv6: String? = null
    @JvmField var port: Short = 0
    @JvmField var username: String? = null
    @JvmField var password: String? = null
    @JvmField var turn: Byte = 0
    @JvmField var stun: Byte = 0
    @JvmField var tcp: Byte = 0
    @JvmField var peer_tag: Pointer? = null
    @JvmField var peer_tag_len: SizeT = SizeT()
}

@Structure.FieldOrder("state", "kind")
open class NtgConnection : Structure() {
    @JvmField var state: Int = 0
    @JvmField var kind: Int = 0

    class ByValue : NtgConnection(), Structure.ByValue
}

@Structure.FieldOrder("absolute_capture_timestamp_ms", "rotation", "width", "height")
open class NtgFrameData : Structure() {
    @JvmField var absolute_capture_timestamp_ms: Long = 0
    @JvmField var rotation: Int = 0
    @JvmField var width: Short = 0
    @JvmField var height: Short = 0
}

@Structure.FieldOrder("ssrc", "data", "data_len", "frame_data")
open class NtgFrame(pointer: Pointer? = null) : Structure(pointer) {
    @JvmField var ssrc: Long = 0
    @JvmField var data: Pointer? = null
    @JvmField var data_len: SizeT = SizeT()
    @JvmField var frame_data: NtgFrameData = NtgFrameData()
}

@Structure.FieldOrder("ssrc", "state", "device")
open class NtgRemoteSource : Structure() {
    @JvmField var ssrc: Int = 0
    @JvmField var state: Int = 0
    @JvmField var device: Int = 0

    class ByValue : NtgRemoteSource(), Structure.ByValue
}

@Structure.FieldOrder("muted", "video_paused", "video_stopped", "presentation_paused", "presentation_stopped")
open class NtgMediaState : Structure() {
    @JvmField var muted: Byte = 0
    @JvmField var video_paused: Byte = 0
    @JvmField var video_stopped: Byte = 0
    @JvmField var presentation_paused: Byte = 0
    @JvmField var presentation_stopped: Byte = 0

    class ByValue : NtgMediaState(), Structure.ByValue
}

fun interface NtgSignalingCallback : Callback {
    fun invoke(handle: Pointer?, chatId: Long, data: Pointer?, length: SizeT, user: Pointer?)
}

fun interface NtgConnectionCallback : Callback {
    fun invoke(handle: Pointer?, chatId: Long, state: NtgConnection.ByValue, user: Pointer?)
}

fun interface NtgFramesCallback : Callback {
    fun invoke(handle: Pointer?, chatId: Long, mode: Int, device: Int, frames: Pointer?, count: SizeT, user: Pointer?)
}

fun interface NtgRemoteSourceCallback : Callback {
    fun invoke(handle: Pointer?, chatId: Long, source: NtgRemoteSource.ByValue, user: Pointer?)
}

fun interface NtgUpgradeCallback : Callback {
    fun invoke(handle: Pointer?, chatId: Long, state: NtgMediaState.ByValue, user: Pointer?)
}

fun interface NtgEmojisCallback : Callback {
    fun invoke(handle: Pointer?, chatId: Long, emojis: String?, user: Pointer?)
}

@Suppress("FunctionName")
interface NtgLibrary : Library {
    fun ntg_get_version(): String
    fun ntg_last_error(): String?
    fun ntg_instance_create(): Pointer
    fun ntg_instance_destroy(handle: Pointer)
    fun ntg_create_p2p_call(handle: Pointer, userId: Long): Int
    fun ntg_skip_exchange(handle: Pointer, userId: Long, key: ByteArray, length: SizeT, outgoing: Boolean): Int
    fun ntg_connect_p2p(
        handle: Pointer,
        userId: Long,
        servers: NtgServer?,
        serversLength: SizeT,
        versions: Array<String>,
        versionsLength: SizeT,
        p2pAllowed: Boolean,
        customParameters: String,
    ): Int
    fun ntg_set_stream_sources(handle: Pointer, chatId: Long, mode: Int, media: NtgMedia.ByValue): Int
    fun ntg_mute(handle: Pointer, chatId: Long, out: ByteByReference): Int
    fun ntg_unmute(handle: Pointer, chatId: Long, out: ByteByReference): Int
    fun ntg_stop(handle: Pointer, chatId: Long): Int
    fun ntg_send_signaling_data(handle: Pointer, chatId: Long, data: ByteArray, length: SizeT): Int
    fun ntg_get_media_devices(out: NtgDevices): Int
    fun ntg_media_devices_free(value: NtgDevices)
    fun ntg_get_protocol(out: NtgProtocol): Int
    fun ntg_protocol_free(value: NtgProtocol)
    fun ntg_get_emojis_fingerprint(handle: Pointer, chatId: Long, out: PointerByReference): Int
    fun ntg_string_free(value: Pointer)
    fun ntg_on_signaling_data_callback(handle: Pointer, callback: NtgSignalingCallback, user: Pointer?): Int
    fun ntg_on_connection_change_callback(handle: Pointer, callback: NtgConnectionCallback, user: Pointer?): Int
    fun ntg_on_frames_callback(handle: Pointer, callback: NtgFramesCallback, user: Pointer?): Int
    fun ntg_on_remote_source_change_callback(handle: Pointer, callback: NtgRemoteSourceCallback, user: Pointer?): Int
    fun ntg_on_upgrade_callback(handle: Pointer, callback: NtgUpgradeCallback, user: Pointer?): Int
    fun ntg_on_update_emojis_callback(handle: Pointer, callback: NtgEmojisCallback, user: Pointer?): Int
}
