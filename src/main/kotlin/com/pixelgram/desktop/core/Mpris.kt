package com.pixelgram.desktop.core

import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.freedesktop.dbus.DBusPath
import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.interfaces.Properties
import org.freedesktop.dbus.messages.DBusSignal
import org.freedesktop.dbus.types.Variant

@DBusInterfaceName("org.mpris.MediaPlayer2")
interface MprisRoot : DBusInterface {
    fun Raise()
    fun Quit()
}

@DBusInterfaceName("org.mpris.MediaPlayer2.Player")
interface MprisPlayer : DBusInterface {
    fun Next()
    fun Previous()
    fun Pause()
    fun PlayPause()
    fun Stop()
    fun Play()
    fun Seek(offset: Long)
    fun SetPosition(trackId: DBusPath, position: Long)
    fun OpenUri(uri: String)

    class Seeked(path: String, val position: Long) : DBusSignal(path, position)
}

object Mpris {
    var onRaise: () -> Unit = {}
    var onQuit: () -> Unit = {}
    var onMove: (Int) -> Unit = {}
    var coverPath: () -> String? = { null }

    private var connection: DBusConnection? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val supported: Boolean get() = System.getProperty("os.name").orEmpty().lowercase().contains("linux")

    fun start() {
        if (!supported || connection != null) return
        runCatching {
            val bus = DBusConnectionBuilder.forSessionBus().build()
            bus.requestBusName(BusName)
            bus.exportObject(ObjectPath, Service)
            connection = bus
            scope.launch {
                snapshotFlow { Triple(AudioPlayer.playing, AudioPlayer.current, AudioPlayer.title) to (AudioPlayer.duration to AudioPlayer.audio) }
                    .collectLatest { changed(PlayerInterface, playerProperties()) }
            }
        }.onFailure { AppLog.write("MPRIS start failed", it) }
    }

    fun stop() {
        runCatching { connection?.close() }
        connection = null
    }

    private fun ui(action: () -> Unit) = javax.swing.SwingUtilities.invokeLater(action)

    private fun micros(seconds: Float) = (seconds * 1_000_000L).toLong()

    private fun trackPath(): DBusPath = DBusPath(AudioPlayer.current?.let { "/app/pixelgram/track/${it.toString().replace('-', '_')}" } ?: NoTrack)

    private fun status() = when {
        AudioPlayer.current == null -> "Stopped"
        AudioPlayer.playing -> "Playing"
        else -> "Paused"
    }

    private fun metadata(): Map<String, Variant<*>> {
        if (AudioPlayer.current == null) return mapOf("mpris:trackid" to Variant(DBusPath(NoTrack)))
        val audio = AudioPlayer.audio
        val title = audio?.title?.ifBlank { null } ?: AudioPlayer.title
        val artist = audio?.performer?.ifBlank { null }
        return buildMap {
            put("mpris:trackid", Variant(trackPath()))
            put("xesam:title", Variant(title))
            if (artist != null) put("xesam:artist", Variant(arrayOf(artist), "as"))
            if (AudioPlayer.duration > 0f) put("mpris:length", Variant(micros(AudioPlayer.duration)))
            coverPath()?.let { put("mpris:artUrl", Variant(java.io.File(it).toURI().toString())) }
        }
    }

    private fun rootProperties(): Map<String, Variant<*>> = mapOf(
        "CanQuit" to Variant(true),
        "CanRaise" to Variant(true),
        "HasTrackList" to Variant(false),
        "Identity" to Variant("pixelGram"),
        "DesktopEntry" to Variant("pixelgram"),
        "SupportedUriSchemes" to Variant(arrayOf<String>(), "as"),
        "SupportedMimeTypes" to Variant(arrayOf<String>(), "as"),
    )

    private fun playerProperties(): Map<String, Variant<*>> {
        val active = AudioPlayer.current != null
        return mapOf(
            "PlaybackStatus" to Variant(status()),
            "LoopStatus" to Variant(if (AudioPlayer.repeat) "Track" else "None"),
            "Rate" to Variant(AudioPlayer.speed.toDouble()),
            "Shuffle" to Variant(false),
            "Metadata" to Variant(metadata(), "a{sv}"),
            "Volume" to Variant(AudioPlayer.volume.toDouble()),
            "Position" to Variant(micros(AudioPlayer.position)),
            "MinimumRate" to Variant(0.5),
            "MaximumRate" to Variant(2.0),
            "CanGoNext" to Variant(active),
            "CanGoPrevious" to Variant(active),
            "CanPlay" to Variant(active),
            "CanPause" to Variant(active),
            "CanSeek" to Variant(active && AudioPlayer.duration > 0f),
            "CanControl" to Variant(true),
        )
    }

    private fun changed(iface: String, values: Map<String, Variant<*>>) {
        val bus = connection ?: return
        runCatching {
            bus.sendMessage(Properties.PropertiesChanged(ObjectPath, iface, values.filterKeys { it != "Position" }, emptyList()))
        }
    }

    private fun seekTo(seconds: Float) {
        if (AudioPlayer.duration <= 0f) return
        AudioPlayer.seekCurrent((seconds / AudioPlayer.duration).coerceIn(0f, 1f))
        runCatching { connection?.sendMessage(MprisPlayer.Seeked(ObjectPath, micros(seconds))) }
    }

    private object Service : MprisRoot, MprisPlayer, Properties {
        override fun getObjectPath(): String = ObjectPath
        override fun Raise() = ui { onRaise() }
        override fun Quit() = ui { onQuit() }
        override fun Next() = ui { onMove(1) }
        override fun Previous() = ui { onMove(-1) }
        override fun Pause() = ui { if (AudioPlayer.playing) AudioPlayer.pauseOrResume() }
        override fun Play() = ui { if (!AudioPlayer.playing && AudioPlayer.current != null) AudioPlayer.pauseOrResume() }
        override fun PlayPause() = ui { if (AudioPlayer.current != null) AudioPlayer.pauseOrResume() }
        override fun Stop() = ui { AudioPlayer.stop() }
        override fun Seek(offset: Long) = ui { seekTo(AudioPlayer.position + offset / 1_000_000f) }
        override fun SetPosition(trackId: DBusPath, position: Long) = ui { if (trackId.path == trackPath().path) seekTo(position / 1_000_000f) }
        override fun OpenUri(uri: String) = Unit

        @Suppress("UNCHECKED_CAST")
        override fun <A : Any?> Get(interfaceName: String, propertyName: String): A =
            all(interfaceName)[propertyName]?.value as A

        override fun GetAll(interfaceName: String): Map<String, Variant<*>> = all(interfaceName)

        override fun <A : Any?> Set(interfaceName: String, propertyName: String, value: A) {
            val raw = (value as? Variant<*>)?.value ?: value
            when (propertyName) {
                "Volume" -> (raw as? Double)?.let { volume -> ui { AudioPlayer.volume = volume.toFloat().coerceIn(0f, 1f) } }
                "Rate" -> (raw as? Double)?.let { rate -> ui { AudioPlayer.speed = rate.toFloat().coerceIn(0.5f, 2f) } }
                "LoopStatus" -> (raw as? String)?.let { loop -> ui { AudioPlayer.repeat = loop == "Track" } }
            }
        }

        private fun all(interfaceName: String) = when (interfaceName) {
            RootInterface -> rootProperties()
            PlayerInterface -> playerProperties()
            else -> emptyMap()
        }
    }

    private const val BusName = "org.mpris.MediaPlayer2.pixelgram"
    private const val ObjectPath = "/org/mpris/MediaPlayer2"
    private const val RootInterface = "org.mpris.MediaPlayer2"
    private const val PlayerInterface = "org.mpris.MediaPlayer2.Player"
    private const val NoTrack = "/org/mpris/MediaPlayer2/TrackList/NoTrack"
}
