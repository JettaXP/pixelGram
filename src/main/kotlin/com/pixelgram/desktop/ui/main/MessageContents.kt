package com.pixelgram.desktop.ui.main

import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.pixelgram.desktop.core.Downloads
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.animation.core.animateFloatAsState
import com.pixelgram.desktop.ui.icons.rounded.Close
import androidx.compose.material3.CircularWavyProgressIndicator
import com.pixelgram.desktop.ui.icons.filled.Settings
import com.pixelgram.desktop.core.Backup
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.foundation.border
import com.pixelgram.desktop.ui.icons.filled.ArrowDownward
import com.pixelgram.desktop.core.AudioPlayer
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Checkbox
import com.pixelgram.desktop.ui.icons.filled.CheckCircle
import com.pixelgram.desktop.ui.icons.rounded.Pause
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.automirrored.filled.CallMade
import com.pixelgram.desktop.ui.icons.automirrored.filled.CallMissed
import com.pixelgram.desktop.ui.icons.automirrored.filled.CallMissedOutgoing
import com.pixelgram.desktop.ui.icons.automirrored.filled.CallReceived
import com.pixelgram.desktop.ui.icons.filled.Call
import com.pixelgram.desktop.ui.icons.filled.Extension
import com.pixelgram.desktop.ui.icons.filled.InsertDriveFile
import com.pixelgram.desktop.ui.icons.filled.MusicNote
import com.pixelgram.desktop.ui.icons.filled.Person
import com.pixelgram.desktop.ui.icons.filled.Place
import com.pixelgram.desktop.ui.icons.rounded.PlayArrow
import com.pixelgram.desktop.ui.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import com.pixelgram.desktop.ui.components.expressive.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.components.expressive.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelgram.desktop.core.AppLog
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.telegram.UiMessage
import com.pixelgram.desktop.ui.components.loadImage
import com.pixelgram.desktop.ui.theme.EmojiText
import it.tdlight.jni.TdApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Rect
import org.jetbrains.skia.skottie.Animation
import com.pixelgram.desktop.core.PowerSaving
import java.awt.Desktop
import java.io.File
import java.net.URI
import java.util.zip.GZIPInputStream
import org.jetbrains.skia.Image as SkiaImage

fun isBareContent(content: TdApi.MessageContent?): Boolean =
    content is TdApi.MessageSticker || content is TdApi.MessageAnimatedEmoji || content is TdApi.MessageDice ||
        content is TdApi.MessageVideoNote

fun isGiftContent(content: TdApi.MessageContent?): Boolean =
    content is TdApi.MessageGift || content is TdApi.MessageUpgradedGift ||
        content is TdApi.MessageGiftedStars || content is TdApi.MessageGiftedPremium

fun isMediaContent(content: TdApi.MessageContent?): Boolean =
    content is TdApi.MessageVideo || content is TdApi.MessageAnimation

@Composable
fun StickerView(
    sticker: TdApi.Sticker?,
    telegram: Telegram,
    size: Dp,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    runs: Int = -1,
    onEnd: () -> Unit = {},
) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        if (sticker == null) {
            LoadingIndicator(Modifier.size(size / 3))
            return@Box
        }
        LaunchedEffect(sticker.sticker?.id) { telegram.download(sticker.sticker) }
        val path = telegram.localPath(sticker.sticker)
        when {
            path != null && sticker.format is TdApi.StickerFormatTgs -> LottieView(path, Modifier.fillMaxSize(), animate, runs, onEnd)
            path != null && sticker.format is TdApi.StickerFormatWebp -> FileImage(path, Modifier.fillMaxSize(), ContentScale.Fit)
            path != null && sticker.format is TdApi.StickerFormatWebm -> VideoStickerView(path, Modifier.fillMaxSize(), animate)
            else -> ThumbImage(sticker.thumbnail?.file, null, telegram, Modifier.fillMaxSize(), ContentScale.Fit)
        }
    }
}

@Composable
fun LottieView(path: String, modifier: Modifier, play: Boolean = true, runs: Int = -1, onEnd: () -> Unit = {}) {
    val animation by produceState<Animation?>(null, path) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val json = GZIPInputStream(File(path).inputStream()).use { it.readBytes().decodeToString() }
                Animation.makeFromString(json)
            }.onFailure { AppLog.write("Sticker animation failed", it) }.getOrNull()
        }
    }
    val ended by rememberUpdatedState(onEnd)
    val frame by produceState(0, animation, play, runs) {
        val current = animation ?: return@produceState
        if (!play || runs == 0) {
            value = 0
            return@produceState
        }
        val fps = current.fPS.takeIf { it > 0f } ?: DefaultLottieFps
        val total = (current.duration * fps).toInt().coerceAtLeast(1)
        val start = withFrameNanos { it }
        while (true) {
            var done = false
            withFrameNanos { now ->
                val passed = ((now - start) / 1_000_000_000.0 * fps).toLong()
                if (runs > 0 && passed >= total) {
                    done = true
                    value = 0
                } else {
                    val next = (passed % total).toInt()
                    if (next != value) value = next
                }
            }
            if (done) {
                ended()
                break
            }
        }
    }
    Canvas(modifier.graphicsLayer()) {
        val current = animation ?: return@Canvas
        current.seekFrame(frame.toFloat())
        drawIntoCanvas { canvas ->
            current.render(canvas.nativeCanvas, Rect.makeWH(size.width, size.height))
        }
    }
}

private const val DefaultLottieFps = 60f
private const val AutoplayVideoBytes = 20L * 1024 * 1024

@Composable
private fun AnimatedEmojiView(message: UiMessage, content: TdApi.MessageAnimatedEmoji, telegram: Telegram) {
    val sticker = content.animatedEmoji?.sticker
    val size = StickerSize / 1.4f
    if (sticker == null) {
        Text(content.emoji, fontSize = 64.sp)
        return
    }
    val animate = PowerSaving.stickersInChat
    val key = message.chatId to message.id
    var runs by remember(key) { mutableStateOf(if (animate && AnimatedEmojiPlays.first(key)) 1 else 0) }
    var playing by remember(key) { mutableStateOf(runs > 0) }
    var effect by remember(key) { mutableStateOf<TdApi.Sticker?>(null) }
    var effectRuns by remember(key) { mutableStateOf(0) }
    val remote = telegram.emojiClicks[key]
    LaunchedEffect(remote) {
        if (remote == null || !animate) return@LaunchedEffect
        telegram.emojiClicks.remove(key)
        if (!playing) {
            playing = true
            runs++
        }
        if (effect == null) {
            effect = remote
            effectRuns++
        }
    }
    Box {
        StickerView(
            sticker,
            telegram,
            size,
            Modifier.clickable(interactionSource = null, indication = null) {
                if (!animate) return@clickable
                if (!playing) {
                    playing = true
                    runs++
                }
                if (effect == null) {
                    telegram.clickAnimatedEmoji(message.chatId, message.id) { result ->
                        if (result != null && effect == null) {
                            effect = result
                            effectRuns++
                        }
                    }
                }
            },
            animate = animate,
            runs = runs,
            onEnd = { playing = false },
        )
        effect?.let { shown ->
            val shift = with(LocalDensity.current) { (size * EffectShift).roundToPx() }
            androidx.compose.ui.window.Popup(
                alignment = Alignment.Center,
                offset = IntOffset(if (message.isOutgoing) -shift else shift, 0),
            ) {
                StickerView(
                    shown,
                    telegram,
                    size * EffectScale,
                    Modifier.graphicsLayer { if (!message.isOutgoing) scaleX = -1f },
                    runs = effectRuns,
                    onEnd = { effect = null },
                )
            }
        }
    }
}

private object AnimatedEmojiPlays {
    private val played = object : LinkedHashMap<Pair<Long, Long>, Boolean>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Pair<Long, Long>, Boolean>) = size > MaxRemembered
    }

    fun first(key: Pair<Long, Long>): Boolean = played.put(key, true) == null

    private const val MaxRemembered = 4000
}

private const val EffectScale = 3f
private const val EffectShift = 1f

@Composable
fun FileImage(path: String, modifier: Modifier, scale: ContentScale) {
    val image by produceState<ImageBitmap?>(null, path) { value = loadImage(path, maxSize = 640) }
    image?.let { Image(it, contentDescription = null, contentScale = scale, filterQuality = FilterQuality.High, modifier = modifier) }
}

@Composable
fun ThumbImage(
    file: TdApi.File?,
    mini: TdApi.Minithumbnail?,
    telegram: Telegram,
    modifier: Modifier,
    scale: ContentScale = ContentScale.Crop,
) {
    LaunchedEffect(file?.id) { telegram.download(file) }
    val path = telegram.localPath(file)
    val miniImage = remember(mini) {
        mini?.data?.let { runCatching { SkiaImage.makeFromEncoded(it).toComposeImageBitmap() }.getOrNull() }
    }
    Box(modifier) {
        if (path != null) {
            FileImage(path, Modifier.fillMaxSize(), scale)
        } else if (miniImage != null) {
            Image(miniImage, contentDescription = null, contentScale = scale, modifier = Modifier.fillMaxSize().blur(8.dp))
        }
    }
}

fun replyThumb(message: UiMessage): Pair<TdApi.File?, TdApi.Minithumbnail?>? {
    val thumb = when (val content = message.content) {
        is TdApi.MessageVideo -> content.video.thumbnail?.file to content.video.minithumbnail
        is TdApi.MessageAnimation -> content.animation.thumbnail?.file to content.animation.minithumbnail
        is TdApi.MessageVideoNote -> content.videoNote.thumbnail?.file to content.videoNote.minithumbnail
        is TdApi.MessageDocument -> content.document.thumbnail?.file to content.document.minithumbnail
        is TdApi.MessageSticker -> content.sticker.thumbnail?.file to null
        is TdApi.MessagePhoto -> message.photo to content.photo.minithumbnail
        else -> message.photo to null
    }
    return thumb.takeIf { it.first != null || it.second != null }
}

@Composable
fun ReplyThumb(message: UiMessage?, telegram: Telegram?, size: androidx.compose.ui.unit.Dp) {
    val thumb = message?.let(::replyThumb) ?: return
    if (telegram == null) return
    ThumbImage(thumb.first, thumb.second, telegram, Modifier.size(size).clip(RoundedCornerShape(6.dp)))
}

fun openFile(path: String) {
    Thread {
        runCatching { Desktop.getDesktop().open(File(path)) }.recoverCatching {
            ProcessBuilder("xdg-open", path).start()
        }.onFailure { AppLog.write("Open file failed", it) }
    }.apply { isDaemon = true }.start()
}

fun showInFolder(path: String) {
    Thread {
        val file = File(path)
        val os = System.getProperty("os.name").lowercase()
        runCatching {
            when {
                os.contains("win") -> ProcessBuilder("explorer.exe", "/select,", file.absolutePath).start()
                os.contains("mac") -> ProcessBuilder("open", "-R", file.absolutePath).start()
                else -> {
                    val shown = runCatching {
                        ProcessBuilder(
                            "gdbus", "call", "--session",
                            "--dest", "org.freedesktop.FileManager1",
                            "--object-path", "/org/freedesktop/FileManager1",
                            "--method", "org.freedesktop.FileManager1.ShowItems",
                            "['${file.toURI()}']", "",
                        ).start().waitFor() == 0
                    }.getOrDefault(false)
                    if (!shown) ProcessBuilder("xdg-open", file.parentFile.absolutePath).start()
                }
            }
        }.onFailure { AppLog.write("Show in folder failed", it) }
    }.apply { isDaemon = true }.start()
}

fun openUrl(url: String) {
    Links.open(url)
}

@Composable
fun rememberOpener(file: TdApi.File?, telegram: Telegram, onReady: (String) -> Unit): Pair<Boolean, () -> Unit> {
    var waiting by remember(file?.id) { mutableStateOf(false) }
    val path = telegram.localPath(file)
    LaunchedEffect(path, waiting) {
        if (waiting && path != null) {
            waiting = false
            onReady(path)
        }
    }
    return (waiting && path == null) to {
        if (path != null) onReady(path) else {
            waiting = true
            telegram.download(file)
        }
    }
}

fun formatSize(bytes: Long): String = when {
    bytes >= 1L shl 30 -> "%.1f GB".format(bytes / (1L shl 30).toDouble())
    bytes >= 1L shl 20 -> "%.1f MB".format(bytes / (1L shl 20).toDouble())
    bytes >= 1L shl 10 -> "%.1f KB".format(bytes / (1L shl 10).toDouble())
    else -> "$bytes B"
}

fun formatDuration(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FileRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    busy: Boolean,
    preview: (@Composable (Modifier) -> Unit)? = null,
    progress: Float? = null,
    onCancel: (() -> Unit)? = null,
    dragPath: String? = null,
    onClick: () -> Unit,
) {
    val transferring = busy || progress != null
    val scheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .then(if (dragPath != null) Modifier.fileDragSource(dragPath) else Modifier)
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
    ) {
        if (preview != null) {
            Box(
                Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)).background(scheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                preview(Modifier.matchParentSize())
                if (transferring) TransferRing(progress, scheme.primary, onCancel, 40.dp)
            }
        } else Box(Modifier.size(46.dp).clip(CircleShape).background(scheme.primary), contentAlignment = Alignment.Center) {
            if (transferring) {
                TransferRing(progress, scheme.onPrimary, onCancel, 40.dp)
            } else {
                Icon(icon, contentDescription = null, tint = scheme.onPrimary)
            }
        }
        Column(Modifier.widthIn(max = 280.dp)) {
            EmojiText(
                title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LocalContentColorAlpha())
        }
    }
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
fun Modifier.fileDragSource(path: String): Modifier = dragAndDropSource { _ ->
    androidx.compose.ui.draganddrop.DragAndDropTransferData(
        androidx.compose.ui.draganddrop.DragAndDropTransferable(FileTransfer(File(path))),
        supportedActions = listOf(androidx.compose.ui.draganddrop.DragAndDropTransferAction.Copy),
    )
}

private class FileTransfer(private val file: File) : java.awt.datatransfer.Transferable {
    override fun getTransferDataFlavors() = arrayOf(java.awt.datatransfer.DataFlavor.javaFileListFlavor)
    override fun isDataFlavorSupported(flavor: java.awt.datatransfer.DataFlavor) = flavor == java.awt.datatransfer.DataFlavor.javaFileListFlavor
    override fun getTransferData(flavor: java.awt.datatransfer.DataFlavor): Any = listOf(file)
}

private val PreviewExtensions = listOf(".png", ".jpg", ".jpeg", ".webp")

@Composable
private fun LocalContentColorAlpha(): Color = androidx.compose.material3.LocalContentColor.current.copy(alpha = 0.7f)

@Composable
fun BlockContent(message: UiMessage, telegram: Telegram, onPlugin: (String) -> Unit) {
    when (val content = message.content) {
        is TdApi.MessageDocument -> {
            val document = content.document
            val name = com.pixelgram.desktop.core.FileSafety.clean(document.fileName).ifBlank { "File" }
            val plugin = name.endsWith(".plugin", ignoreCase = true)
            val backup = Backup.isBackup(name)
            val scope = rememberCoroutineScope()
            val downloaded = telegram.localPath(document.document)
            var saved by remember(downloaded) { mutableStateOf(downloaded?.let { Downloads.exported(it, name) }) }
            LaunchedEffect(downloaded) {
                if (downloaded != null && saved == null && !message.sending) saved = Downloads.export(downloaded, name)
            }
            val (busy, open) = rememberOpener(document.document, telegram) { path ->
                when {
                    plugin -> onPlugin(path)
                    backup -> BackupViewer.path = path
                    else -> scope.launch { openDocument(Downloads.export(path, name)) }
                }
            }
            val image = PreviewExtensions.any { name.endsWith(it, ignoreCase = true) }
            val local = telegram.localPath(document.document)?.takeIf { image }
            val thumb = document.thumbnail?.file
            val mini = document.minithumbnail
            val preview: (@Composable (Modifier) -> Unit)? = when {
                plugin || backup -> null
                local != null -> { modifier -> FileImage(local, modifier, ContentScale.Crop) }
                thumb != null || mini != null -> { modifier -> ThumbImage(thumb, mini, telegram, modifier) }
                else -> null
            }
            val total = document.document?.let { maxOf(it.size, it.expectedSize) } ?: 0
            val progress = telegram.progress(document.document)
            FileRow(
                when {
                    plugin -> Icons.Filled.Extension
                    backup -> Icons.Filled.Settings
                    else -> Icons.Filled.InsertDriveFile
                },
                name,
                if (progress != null) "${formatSize((total * progress).toLong())} / ${formatSize(total)}" else formatSize(total),
                busy || message.sending,
                preview,
                progress,
                onCancel = {
                    if (message.sending) telegram.cancelSending(message) else telegram.cancelDownload(document.document)
                },
                dragPath = saved ?: downloaded,
                onClick = open,
            )
        }
        is TdApi.MessageAudio -> AudioContent(message, content.audio, telegram)
        is TdApi.MessageVoiceNote -> VoiceContent(message, content.voiceNote, telegram)
        is TdApi.MessageCall -> CallContent(content, message.isOutgoing, message.date)
        is TdApi.MessagePoll -> PollContent(message, content.poll, telegram)
        is TdApi.MessageContact -> {
            val contact = content.contact
            FileRow(
                Icons.Filled.Person,
                listOf(contact.firstName, contact.lastName).filter { it.isNotBlank() }.joinToString(" "),
                contact.phoneNumber.let { if (it.startsWith("+")) it else "+$it" },
                false,
            ) {}
        }
        is TdApi.MessageLocation -> {
            val location = content.location
            FileRow(
                Icons.Filled.Place,
                "Location",
                "%.5f, %.5f".format(location.latitude, location.longitude),
                false,
            ) {
                openUrl("https://www.openstreetmap.org/?mlat=${location.latitude}&mlon=${location.longitude}#map=16/${location.latitude}/${location.longitude}")
            }
        }
        else -> Unit
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlayButton(key: Long, chatId: Long, title: String, file: TdApi.File?, telegram: Telegram, audio: TdApi.Audio? = null) {
    val scheme = MaterialTheme.colorScheme
    val (busy, start) = rememberOpener(file, telegram) { path -> AudioPlayer.toggle(key, path, title, chatId, audio) }
    val active = AudioPlayer.current == key
    val downloaded = telegram.localPath(file) != null
    Box(Modifier.size(PlayerButton)) {
        Box(
            Modifier.fillMaxSize().clip(CircleShape).background(scheme.primary).clickable(onClick = start),
            contentAlignment = Alignment.Center,
        ) {
            when {
                busy -> TransferRing(telegram.progress(file), scheme.onPrimary, { telegram.cancelDownload(file) }, 40.dp)
                active && AudioPlayer.loading -> LoadingIndicator(Modifier.size(32.dp), color = scheme.onPrimary)
                active && AudioPlayer.playing -> Icon(Icons.Rounded.Pause, contentDescription = "Pause", tint = scheme.onPrimary, modifier = Modifier.size(30.dp))
                else -> Icon(Icons.Rounded.PlayArrow, contentDescription = "Play", tint = scheme.onPrimary, modifier = Modifier.size(30.dp))
            }
        }
        if (!downloaded && !busy) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(scheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.ArrowDownward, contentDescription = null, tint = scheme.onPrimaryContainer, modifier = Modifier.size(14.dp))
            }
        }
    }
}

private fun Modifier.seekable(key: Long, file: TdApi.File?, telegram: Telegram): Modifier = pointerInput(key) {
    detectTapGestures { offset ->
        val path = telegram.localPath(file) ?: return@detectTapGestures telegram.download(file)
        AudioPlayer.seek(key, path, offset.x / size.width)
    }
}.pointerInput(key) {
    detectHorizontalDragGestures { change, _ ->
        val path = telegram.localPath(file) ?: return@detectHorizontalDragGestures
        AudioPlayer.seek(key, path, change.position.x / size.width)
    }
}

@Composable
private fun VoiceContent(message: UiMessage, voice: TdApi.VoiceNote, telegram: Telegram) {
    val key = message.key
    val bars = remember(voice.waveform) { waveform(voice.waveform) }
    val color = androidx.compose.material3.LocalContentColor.current
    val active = AudioPlayer.current == key
    val progress = AudioPlayer.progress(key)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PlayButton(key, message.chatId, "Voice message", voice.voice, telegram)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Canvas(Modifier.width(WaveWidth).height(28.dp).seekable(key, voice.voice, telegram)) {
                val step = size.width / bars.size
                val width = step * 0.62f
                bars.forEachIndexed { index, value ->
                    val height = (value * size.height).coerceAtLeast(width)
                    val played = (index + 0.5f) / bars.size <= progress
                    drawRoundRect(
                        if (played) color else color.copy(alpha = 0.35f),
                        topLeft = Offset(index * step, (size.height - height) / 2),
                        size = Size(width, height),
                        cornerRadius = CornerRadius(width / 2),
                    )
                }
            }
            Text(
                if (active) formatDuration(AudioPlayer.position.toInt()) else formatDuration(voice.duration),
                style = MaterialTheme.typography.bodySmall,
                color = LocalContentColorAlpha(),
            )
        }
    }
}

@Composable
private fun AudioContent(message: UiMessage, audio: TdApi.Audio, telegram: Telegram) {
    val key = message.key
    val active = AudioPlayer.current == key
    val title = audioTitle(audio)
    val color = androidx.compose.material3.LocalContentColor.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PlayButton(key, message.chatId, title, audio.audio, telegram, audio)
        Column(Modifier.width(WaveWidth)) {
            EmojiText(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (active) {
                Text(
                    "%02d:%02d / %02d:%02d".format(
                        AudioPlayer.position.toInt() / 60, AudioPlayer.position.toInt() % 60,
                        audio.duration / 60, audio.duration % 60,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColorAlpha(),
                )
            } else {
                Text(
                    "%02d:%02d, %s".format(audio.duration / 60, audio.duration % 60, formatSize(audio.audio?.size ?: 0)),
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColorAlpha(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

fun audioTitle(audio: TdApi.Audio): String =
    listOf(audio.performer, audio.title).filter { it.isNotBlank() }.joinToString(" – ")
        .ifBlank { audio.fileName.ifBlank { "Audio" } }

private fun waveform(data: ByteArray?): List<Float> {
    val bits = data ?: return List(Bars) { 0.15f }
    val values = (0 until bits.size * 8 / 5).map { i ->
        val bit = i * 5
        val byte = bit / 8
        val shift = bit % 8
        val raw = ((bits[byte].toInt() and 0xFF) or ((bits.getOrNull(byte + 1)?.toInt() ?: 0) and 0xFF shl 8)) shr shift
        (raw and 31) / 31f
    }
    if (values.isEmpty()) return List(Bars) { 0.15f }
    return List(Bars) { i -> values[i * values.size / Bars] }
}

private const val Bars = 36
private val WaveWidth = 190.dp
private val PlayerButton = 46.dp

@Composable
private fun CallContent(call: TdApi.MessageCall, outgoing: Boolean, date: Int) {
    val missed = call.discardReason is TdApi.CallDiscardReasonMissed
    val declined = call.discardReason is TdApi.CallDiscardReasonDeclined
    val title = when {
        outgoing && (missed || declined) -> "Canceled call"
        missed -> "Missed call"
        declined -> "Declined call"
        outgoing -> "Outgoing call"
        else -> "Incoming call"
    }.let { if (call.isVideo) it.replace("call", "video call") else it }
    val bad = !outgoing && (missed || declined)
    val arrow = when {
        outgoing && (missed || declined) -> Icons.AutoMirrored.Filled.CallMissedOutgoing
        outgoing -> Icons.AutoMirrored.Filled.CallMade
        bad -> Icons.AutoMirrored.Filled.CallMissed
        else -> Icons.AutoMirrored.Filled.CallReceived
    }
    val scheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.widthIn(min = 220.dp)) {
        Column(Modifier.weight(1f, fill = false)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(arrow, contentDescription = null, tint = if (bad) scheme.error else scheme.primary, modifier = Modifier.size(16.dp))
                Text(
                    formatTime(date) + if (call.duration > 0) ", " + callLength(call.duration) else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColorAlpha(),
                )
            }
        }
        Spacer(Modifier.width(24.dp).weight(1f))
        Icon(if (call.isVideo) Icons.Filled.Videocam else Icons.Filled.Call, contentDescription = null, tint = scheme.primary)
    }
}

private fun callLength(seconds: Int): String = when {
    seconds >= 3600 -> "${seconds / 3600} h ${seconds % 3600 / 60} min"
    seconds >= 60 -> "${seconds / 60} min ${seconds % 60} s"
    else -> "$seconds s"
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PollContent(message: UiMessage, poll: TdApi.Poll, telegram: Telegram) {
    val scheme = MaterialTheme.colorScheme
    val voted = poll.options.any { it.isChosen }
    val results = poll.isClosed || voted
    val multiple = poll.allowsMultipleAnswers && poll.type !is TdApi.PollTypeQuiz
    val picked = remember(poll.id, voted) { androidx.compose.runtime.mutableStateListOf<Int>() }
    Column(Modifier.widthIn(min = 260.dp, max = 360.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        EmojiText(poll.question.text, style = MaterialTheme.typography.titleSmall)
        Text(
            when {
                poll.isClosed -> "Final results"
                poll.type is TdApi.PollTypeQuiz -> if (poll.isAnonymous) "Anonymous quiz" else "Quiz"
                poll.isAnonymous -> "Anonymous poll"
                else -> "Public poll"
            },
            style = MaterialTheme.typography.bodySmall,
            color = LocalContentColorAlpha(),
        )
        poll.options.forEachIndexed { index, option ->
            if (results) {
                Column(Modifier.padding(vertical = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${option.votePercentage}%", style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(44.dp))
                        EmojiText(
                            option.text.text,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (option.isChosen) FontWeight.SemiBold else FontWeight.Normal,
                            ),
                            modifier = Modifier.weight(1f),
                        )
                        if (option.isChosen) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                    LinearWavyProgressIndicator(
                        progress = { option.votePercentage / 100f },
                        color = scheme.primary,
                        trackColor = scheme.primary.copy(alpha = 0.2f),
                        waveSpeed = 0.dp,
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    )
                }
            } else {
                val selected = index in picked
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            if (multiple) {
                                if (selected) picked.remove(index) else picked.add(index)
                            } else {
                                telegram.votePoll(message.chatId, message.id, intArrayOf(index))
                            }
                        },
                ) {
                    if (multiple) {
                        Checkbox(checked = selected, onCheckedChange = null, modifier = Modifier.padding(8.dp))
                    } else {
                        RadioButton(selected = false, onClick = null, modifier = Modifier.padding(8.dp))
                    }
                    EmojiText(option.text.text, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (poll.totalVoterCount == 1) "1 vote" else "${poll.totalVoterCount} votes",
                style = MaterialTheme.typography.bodySmall,
                color = LocalContentColorAlpha(),
                modifier = Modifier.weight(1f),
            )
            when {
                !results && multiple -> TextButton(
                    onClick = { telegram.votePoll(message.chatId, message.id, picked.sorted().toIntArray()) },
                    enabled = picked.isNotEmpty(),
                ) { Text("Vote") }
                voted && !poll.isClosed && poll.type !is TdApi.PollTypeQuiz -> TextButton(
                    onClick = { telegram.votePoll(message.chatId, message.id, IntArray(0)) },
                ) { Text("Retract vote") }
            }
        }
    }
}

@Composable
fun MediaContent(message: UiMessage, telegram: Telegram, width: Dp, corner: Dp) {
    val content = message.content
    val (file, thumb, mini, duration, size) = when (content) {
        is TdApi.MessageVideo -> MediaInfo(content.video.video, content.video.thumbnail?.file, content.video.minithumbnail, content.video.duration, content.video.width to content.video.height)
        is TdApi.MessageAnimation -> MediaInfo(content.animation.animation, content.animation.thumbnail?.file, content.animation.minithumbnail, -1, content.animation.width to content.animation.height)
        else -> return
    }
    val ratio = if (size.first > 0 && size.second > 0) size.first.toFloat() / size.second else 16f / 9f
    val gif = content is TdApi.MessageAnimation
    val autoplay = !message.sending && if (gif) PowerSaving.autoplayGifs else PowerSaving.autoplayVideos && ((file?.size ?: 0L) in 1..AutoplayVideoBytes)
    var started by remember(file?.id) { mutableStateOf(false) }
    LaunchedEffect(file?.id, autoplay, started) { if (autoplay || started) telegram.download(file) }
    val path = telegram.localPath(file)
    val inline = path != null && (autoplay || started)
    val open = {
        when (content) {
            is TdApi.MessageVideo -> VideoPlayer.open(telegram, content.video, message.text)
            is TdApi.MessageAnimation -> VideoPlayer.open(telegram, content.animation, message.text)
            else -> Unit
        }
    }
    val busy = started && path == null
    Box(
        Modifier
            .size(width, (width / ratio).coerceIn(120.dp, 520.dp))
            .clip(RoundedCornerShape(corner))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable { if (inline || autoplay) open() else started = true },
        contentAlignment = Alignment.Center,
    ) {
        ThumbImage(thumb, mini, telegram, Modifier.fillMaxSize())
        if (inline && path != null) InlineVideo(path, Modifier.fillMaxSize())
        if (message.sending) {
            Box(Modifier.size(52.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
                TransferRing(telegram.progress(file), Color.White, { telegram.cancelSending(message) }, 44.dp)
            }
        } else if (!inline) {
            PlayBadge(busy)
        }
        Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.45f),
            contentColor = Color.White,
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        ) {
            Text(
                if (duration < 0) "GIF" else formatDuration(duration),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}

private data class MediaInfo(
    val file: TdApi.File?,
    val thumb: TdApi.File?,
    val mini: TdApi.Minithumbnail?,
    val duration: Int,
    val size: Pair<Int, Int>,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TransferRing(progress: Float?, color: Color, onCancel: (() -> Unit)?, size: Dp) {
    Box(
        Modifier.size(size).then(if (onCancel != null) Modifier.clip(CircleShape).clickable(onClick = onCancel) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        val shown by animateFloatAsState((progress ?: 0f).coerceIn(MinRingProgress, 1f))
        CircularWavyProgressIndicator(
            progress = { shown },
            modifier = Modifier.size(size),
            color = color,
            trackColor = color.copy(alpha = 0.25f),
            amplitude = { value -> if (value < RingFlattenFrom) 1f else ((1f - value) / (1f - RingFlattenFrom)).coerceIn(0f, 1f) },
            waveSpeed = 0.dp,
        )
        if (onCancel != null) {
            Icon(Icons.Rounded.Close, contentDescription = "Cancel", tint = color, modifier = Modifier.size(size * 0.42f))
        }
    }
}

private const val RingFlattenFrom = 0.8f
private const val MinRingProgress = 0.02f

@Composable
fun PlayBadge(busy: Boolean) {
    Box(Modifier.size(52.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
        if (busy) LoadingIndicator(Modifier.size(34.dp), color = Color.White) else
            Icon(Icons.Rounded.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(30.dp))
    }
}

@Composable
fun BareContent(message: UiMessage, telegram: Telegram) {
    when (val content = message.content) {
        is TdApi.MessageSticker -> StickerView(
            content.sticker,
            telegram,
            StickerSize,
            Modifier.openPack(telegram, message.chatId, content.sticker),
            animate = PowerSaving.stickersInChat,
        )
        is TdApi.MessageAnimatedEmoji -> AnimatedEmojiView(message, content, telegram)
        is TdApi.MessageDice -> {
            val sticker = (content.finalState as? TdApi.DiceStickersRegular)?.sticker
            if (sticker != null) StickerView(sticker, telegram, StickerSize / 1.4f, animate = PowerSaving.stickersInChat) else
                Text("${content.emoji} ${content.value}", fontSize = 40.sp)
        }
        is TdApi.MessageVideoNote -> {
            val note = content.videoNote
            val (busy, open) = rememberOpener(note.video, telegram, ::openFile)
            Box(
                Modifier.size(VideoNoteSize).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest).clickable(onClick = open),
                contentAlignment = Alignment.Center,
            ) {
                ThumbImage(note.thumbnail?.file, note.minithumbnail, telegram, Modifier.fillMaxSize())
                PlayBadge(busy)
            }
        }
        else -> Unit
    }
}

@Composable
fun GiftCard(message: UiMessage, telegram: Telegram, modifier: Modifier = Modifier) {
    val content = message.content
    var details by remember(message.id) { mutableStateOf(false) }
    if (details) GiftDetailsDialog(message, telegram) { details = false }
    val sticker: TdApi.Sticker?
    val title: String
    val subtitle: String
    when (content) {
        is TdApi.MessageGift -> {
            sticker = content.gift.sticker
            val other = if (message.isOutgoing) content.receiverId else content.senderId
            val name = telegram.senderTitle(other)
            title = when {
                name.isBlank() -> "Gift"
                message.isOutgoing -> "Gift for $name"
                else -> "Gift from $name"
            }
            subtitle = stars(content.gift.starCount)
        }
        is TdApi.MessageUpgradedGift -> {
            sticker = content.gift.model?.sticker
            title = "${content.gift.title} #${content.gift.number}"
            subtitle = "Collectible gift"
        }
        is TdApi.MessageGiftedStars -> {
            sticker = content.sticker
            title = stars(content.starCount)
            subtitle = paidPrice(content.currency, content.amount, content.cryptocurrency, content.cryptocurrencyAmount)?.let { "Gift · $it" } ?: "Gift"
        }
        is TdApi.MessageGiftedPremium -> {
            sticker = content.sticker
            title = "Telegram Premium"
            val length = if (content.monthCount > 0) "for ${content.monthCount} months" else "for ${content.dayCount} days"
            subtitle = paidPrice(content.currency, content.amount, content.cryptocurrency, content.cryptocurrencyAmount)?.let { "$length · $it" } ?: length
        }
        else -> return
    }
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.width(GiftWidth),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            StickerView(sticker, telegram, 120.dp)
            EmojiText(title, style = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LocalContentColorAlpha())
            if (message.text.isNotBlank()) {
                EmojiText(message.text, style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center))
            }
            FilledTonalButton(onClick = { details = true }) { Text("View") }
        }
    }
}

fun giftServiceText(message: UiMessage, telegram: Telegram): String? = when (val content = message.content) {
    is TdApi.MessageGift ->
        if (message.isOutgoing) "You sent a gift for ${stars(content.gift.starCount)}"
        else "${telegram.senderTitle(content.senderId)} sent you a gift for ${stars(content.gift.starCount)}"
    is TdApi.MessageUpgradedGift -> "Collectible gift"
    is TdApi.MessageGiftedStars -> {
        val paid = paidPrice(content.currency, content.amount, content.cryptocurrency, content.cryptocurrencyAmount)?.let { " for $it" }.orEmpty()
        if (message.isOutgoing) "You gifted ${stars(content.starCount)}$paid" else "You received ${stars(content.starCount)}$paid"
    }
    is TdApi.MessageGiftedPremium -> {
        val paid = paidPrice(content.currency, content.amount, content.cryptocurrency, content.cryptocurrencyAmount)?.let { " for $it" }.orEmpty()
        if (message.isOutgoing) "You gifted Telegram Premium$paid" else "You received Telegram Premium$paid"
    }
    else -> null
}

val StickerSize = 180.dp
private val VideoNoteSize = 220.dp
private val GiftWidth = 240.dp
