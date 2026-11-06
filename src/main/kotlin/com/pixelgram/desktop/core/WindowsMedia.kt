package com.pixelgram.desktop.core

import androidx.compose.runtime.snapshotFlow
import com.sun.jna.Callback
import com.sun.jna.Function
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.NativeLibrary
import com.sun.jna.Pointer
import com.sun.jna.WString
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.PointerByReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.Executors

interface QueryCallback : Callback {
    fun invoke(self: Pointer, riid: Pointer, out: Pointer): Int
}

interface CountCallback : Callback {
    fun invoke(self: Pointer): Int
}

interface PressCallback : Callback {
    fun invoke(self: Pointer, sender: Pointer, args: Pointer): Int
}

object WindowsMedia {
    val supported: Boolean get() = System.getProperty("os.name").orEmpty().lowercase().contains("windows")

    private val thread = Executors.newSingleThreadExecutor { Thread(it, "pixelgram-smtc").apply { isDaemon = true } }
    private val scope = CoroutineScope(SupervisorJob() + thread.asCoroutineDispatcher())
    private var controls: Pointer? = null
    private var started = false
    private val keep = mutableListOf<Any>()

    fun start(hwnd: Long) {
        if (!supported || started || hwnd == 0L) return
        started = true
        scope.launch {
            runCatching {
                val combase = NativeLibrary.getInstance("combase")
                combase.getFunction("RoInitialize").invokeInt(arrayOf(MultiThreaded))
                val factory = activationFactory(combase, "Windows.Media.SystemMediaTransportControls", InteropIid)
                val out = PointerByReference()
                check(call(factory, InteropGetForWindow, Pointer(hwnd), guid(ControlsIid), out) == 0) { "GetForWindow failed" }
                val smtc = out.value
                controls = smtc
                call(smtc, PutIsEnabled, 1)
                listOf(PutIsPlayEnabled, PutIsPauseEnabled, PutIsNextEnabled, PutIsPreviousEnabled, PutIsStopEnabled).forEach { call(smtc, it, 1) }
                val token = Memory(8)
                call(smtc, AddButtonPressed, handler(), token)
                AppLog.write("Windows media controls ready")
                snapshotFlow { Triple(AudioPlayer.current, AudioPlayer.playing, AudioPlayer.title) to AudioPlayer.audio }
                    .collectLatest { (state, audio) ->
                        val (current, playing, title) = state
                        runCatching { update(combase, smtc, current, playing, title, audio) }
                            .onFailure { AppLog.write("Windows media update failed", it) }
                    }
            }.onFailure { AppLog.write("Windows media controls failed", it) }
        }
    }

    private fun update(combase: NativeLibrary, smtc: Pointer, current: Long?, playing: Boolean, title: String, audio: it.tdlight.jni.TdApi.Audio?) {
        call(smtc, PutPlaybackStatus, if (current == null) StatusClosed else if (playing) StatusPlaying else StatusPaused)
        val updaterOut = PointerByReference()
        if (call(smtc, GetDisplayUpdater, updaterOut) != 0) return
        val updater: Pointer = updaterOut.value ?: return
        call(updater, PutType, TypeMusic)
        val musicOut = PointerByReference()
        val music: Pointer? = if (call(updater, GetMusicProperties, musicOut) == 0) musicOut.value else null
        if (music != null) {
            val name = audio?.title?.ifBlank { null } ?: title
            val artist = audio?.performer.orEmpty()
            withString(combase, name) { call(music, PutTitle, it) }
            withString(combase, artist) { call(music, PutArtist, it) }
            release(music)
        }
        val cover = Mpris.coverPath()
        if (cover != null) runCatching { thumbnail(combase, updater, cover) }
        call(updater, UpdateDisplay)
        release(updater)
    }

    private fun thumbnail(combase: NativeLibrary, updater: Pointer, path: String) {
        val uriFactory = activationFactory(combase, "Windows.Foundation.Uri", UriFactoryIid)
        val uriOut = PointerByReference()
        withString(combase, java.io.File(path).toURI().toString()) { call(uriFactory, CreateUri, it, uriOut) }
        release(uriFactory)
        val uri = uriOut.value ?: return
        val streams = activationFactory(combase, "Windows.Storage.Streams.RandomAccessStreamReference", StreamStaticsIid)
        val refOut = PointerByReference()
        call(streams, CreateFromUri, uri, refOut)
        release(streams)
        release(uri)
        refOut.value?.let { reference ->
            call(updater, PutThumbnail, reference)
            release(reference)
        }
    }

    private fun handler(): Pointer {
        val queryInterface = object : QueryCallback {
            override fun invoke(self: Pointer, riid: Pointer, out: Pointer): Int {
                val asked = readGuid(riid)
                return if (asked == UnknownIid || asked == AgileIid || asked == HandlerIid) {
                    out.setPointer(0, self)
                    0
                } else {
                    out.setPointer(0, Pointer.NULL)
                    NoInterface
                }
            }
        }
        val addRef = object : CountCallback {
            override fun invoke(self: Pointer): Int = 1
        }
        val releaseRef = object : CountCallback {
            override fun invoke(self: Pointer): Int = 1
        }
        val invoke = object : PressCallback {
            override fun invoke(self: Pointer, sender: Pointer, args: Pointer): Int {
                runCatching {
                    val button = IntByReference()
                    call(args, GetButton, button)
                    javax.swing.SwingUtilities.invokeLater {
                        when (button.value) {
                            ButtonPlay -> if (!AudioPlayer.playing && AudioPlayer.current != null) AudioPlayer.pauseOrResume()
                            ButtonPause -> if (AudioPlayer.playing) AudioPlayer.pauseOrResume()
                            ButtonStop -> AudioPlayer.stop()
                            ButtonNext -> Mpris.onMove(1)
                            ButtonPrevious -> Mpris.onMove(-1)
                        }
                    }
                }
                return 0
            }
        }
        keep.addAll(listOf(queryInterface, addRef, releaseRef, invoke))
        val table = Memory(Native.POINTER_SIZE * 4L)
        listOf(queryInterface, addRef, releaseRef, invoke).forEachIndexed { index, callback ->
            table.setPointer(Native.POINTER_SIZE * index.toLong(), com.sun.jna.CallbackReference.getFunctionPointer(callback))
        }
        val obj = Memory(Native.POINTER_SIZE.toLong())
        obj.setPointer(0, table)
        keep.addAll(listOf(table, obj))
        return obj
    }

    private fun activationFactory(combase: NativeLibrary, name: String, iid: String): Pointer {
        val out = PointerByReference()
        withString(combase, name) { hstring ->
            check(combase.getFunction("RoGetActivationFactory").invokeInt(arrayOf(hstring, guid(iid), out)) == 0) { "No factory for $name" }
        }
        return out.value
    }

    private inline fun withString(combase: NativeLibrary, value: String, use: (Pointer) -> Unit) {
        val out = PointerByReference()
        combase.getFunction("WindowsCreateString").invokeInt(arrayOf(WString(value), value.length, out))
        try {
            use(out.value ?: Pointer.NULL)
        } finally {
            out.value?.let { combase.getFunction("WindowsDeleteString").invokeInt(arrayOf(it)) }
        }
    }

    private fun call(target: Pointer, slot: Int, vararg args: Any?): Int {
        val table = target.getPointer(0)
        val function = Function.getFunction(table.getPointer(slot.toLong() * Native.POINTER_SIZE))
        return function.invokeInt(arrayOf(target, *args))
    }

    private fun release(target: Pointer) {
        runCatching { call(target, 2) }
    }

    private fun guid(text: String): Memory {
        val uuid = UUID.fromString(text)
        val memory = Memory(16)
        val most = uuid.mostSignificantBits
        memory.setInt(0, (most ushr 32).toInt())
        memory.setShort(4, (most ushr 16).toShort())
        memory.setShort(6, most.toShort())
        val least = uuid.leastSignificantBits
        for (i in 0 until 8) memory.setByte(8L + i, (least ushr (56 - i * 8)).toByte())
        return memory
    }

    private fun readGuid(pointer: Pointer): String {
        val data1 = pointer.getInt(0).toLong() and 0xFFFFFFFFL
        val data2 = pointer.getShort(4).toInt() and 0xFFFF
        val data3 = pointer.getShort(6).toInt() and 0xFFFF
        val rest = (0 until 8).joinToString("") { "%02x".format(pointer.getByte(8L + it)) }
        return "%08x-%04x-%04x-%s-%s".format(data1, data2, data3, rest.take(4), rest.drop(4))
    }

    private const val MultiThreaded = 1
    private const val NoInterface = 0x80004002.toInt()
    private const val InteropIid = "ddb0472d-c911-4a1f-86d9-dc3d71a95f5a"
    private const val ControlsIid = "99fa3ff4-1742-42a6-902e-087d41f965ec"
    private const val HandlerIid = "0557e996-7b23-5bae-aa81-ea0d671143a4"
    private const val UnknownIid = "00000000-0000-0000-c000-000000000046"
    private const val AgileIid = "94ea2b94-e9cc-49e0-c0ff-ee64ca8f5b90"
    private const val UriFactoryIid = "44a9796f-723e-4fdf-a218-033e75b0c084"
    private const val StreamStaticsIid = "857309dc-3fbf-4e7d-986f-ef3b1a07a964"
    private const val InteropGetForWindow = 6
    private const val PutPlaybackStatus = 7
    private const val GetDisplayUpdater = 8
    private const val PutIsEnabled = 11
    private const val PutIsPlayEnabled = 13
    private const val PutIsStopEnabled = 15
    private const val PutIsPauseEnabled = 17
    private const val PutIsPreviousEnabled = 25
    private const val PutIsNextEnabled = 27
    private const val AddButtonPressed = 32
    private const val PutType = 7
    private const val PutThumbnail = 11
    private const val GetMusicProperties = 12
    private const val UpdateDisplay = 17
    private const val PutTitle = 7
    private const val PutArtist = 11
    private const val GetButton = 6
    private const val CreateUri = 6
    private const val CreateFromUri = 7
    private const val TypeMusic = 1
    private const val StatusClosed = 0
    private const val StatusPlaying = 3
    private const val StatusPaused = 4
    private const val ButtonPlay = 0
    private const val ButtonPause = 1
    private const val ButtonStop = 2
    private const val ButtonNext = 6
    private const val ButtonPrevious = 7
}
