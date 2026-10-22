package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.ArrowDownward
import com.pixelgram.desktop.core.AudioPlayer
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Checkbox
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallMissedOutgoing
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
fun StickerView(sticker: TdApi.Sticker?, telegram: Telegram, size: Dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        if (sticker == null) {
            LoadingIndicator(Modifier.size(size / 3))
            return@Box
        }
        LaunchedEffect(sticker.sticker?.id) { telegram.download(sticker.sticker) }
        val path = telegram.localPath(sticker.sticker)
        when {
            path != null && sticker.format is TdApi.StickerFormatTgs -> LottieView(path, Modifier.fillMaxSize())
            path != null && sticker.format is TdApi.StickerFormatWebp -> FileImage(path, Modifier.fillMaxSize(), ContentScale.Fit)
            else -> ThumbImage(sticker.thumbnail?.file, null, telegram, Modifier.fillMaxSize(), ContentScale.Fit)
        }
    }
}

@Composable
fun LottieView(path: String, modifier: Modifier) {
    val animation by produceState<Animation?>(null, path) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val json = GZIPInputStream(File(path).inputStream()).use { it.readBytes().decodeToString() }
                Animation.makeFromString(json)
            }.onFailure { AppLog.write("Sticker animation failed", it) }.getOrNull()
        }
    }
    val time by produceState(0f, animation) {
        val start = withFrameNanos { it }
        while (true) withFrameNanos { value = (it - start) / 1_000_000_000f }
    }
    Canvas(modifier) {
        val current = animation ?: return@Canvas
        val duration = current.duration.takeIf { it > 0f } ?: 1f
        current.seekFrameTime(time % duration)
        drawIntoCanvas { canvas ->
            current.render(canvas.nativeCanvas, Rect.makeWH(size.width, size.height))
        }
    }
}

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

fun openFile(path: String) {
    Thread {
        runCatching { Desktop.getDesktop().open(File(path)) }.recoverCatching {
            ProcessBuilder("xdg-open", path).start()
        }.onFailure { AppLog.write("Open file failed", it) }
    }.apply { isDaemon = true }.start()
}

fun openUrl(url: String) {
    Thread {
        runCatching { Desktop.getDesktop().browse(URI(url)) }.recoverCatching {
            ProcessBuilder("xdg-open", url).start()
        }.onFailure { AppLog.write("Open link failed", it) }
    }.apply { isDaemon = true }.start()
}

@Composable
private fun rememberOpener(file: TdApi.File?, telegram: Telegram, onReady: (String) -> Unit): Pair<Boolean, () -> Unit> {
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
private fun FileRow(icon: ImageVector, title: String, subtitle: String, busy: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 2.dp),
    ) {
        Box(Modifier.size(46.dp).clip(CircleShape).background(scheme.primary), contentAlignment = Alignment.Center) {
            if (busy) {
                LoadingIndicator(Modifier.size(30.dp), color = scheme.onPrimary)
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

@Composable
private fun LocalContentColorAlpha(): Color = androidx.compose.material3.LocalContentColor.current.copy(alpha = 0.7f)

@Composable
fun BlockContent(message: UiMessage, telegram: Telegram, onPlugin: (String) -> Unit) {
    when (val content = message.content) {
        is TdApi.MessageDocument -> {
            val document = content.document
            val name = document.fileName.ifBlank { "File" }
            val plugin = name.endsWith(".plugin", ignoreCase = true)
            val (busy, open) = rememberOpener(document.document, telegram) { path ->
                if (plugin) onPlugin(path) else openFile(path)
            }
            FileRow(
                if (plugin) Icons.Filled.Extension else Icons.Filled.InsertDriveFile,
                name,
                formatSize(document.document?.let { maxOf(it.size, it.expectedSize) } ?: 0),
                busy,
                open,
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
private fun PlayButton(key: Long, chatId: Long, title: String, file: TdApi.File?, telegram: Telegram) {
    val scheme = MaterialTheme.colorScheme
    val (busy, start) = rememberOpener(file, telegram) { path -> AudioPlayer.toggle(key, path, title, chatId) }
    val active = AudioPlayer.current == key
    val downloaded = telegram.localPath(file) != null
    Box(Modifier.size(PlayerButton)) {
        Box(
            Modifier.fillMaxSize().clip(CircleShape).background(scheme.primary).clickable(onClick = start),
            contentAlignment = Alignment.Center,
        ) {
            when {
                busy || (active && AudioPlayer.loading) -> LoadingIndicator(Modifier.size(32.dp), color = scheme.onPrimary)
                active && AudioPlayer.playing -> Icon(Icons.Filled.Pause, contentDescription = "Pause", tint = scheme.onPrimary, modifier = Modifier.size(30.dp))
                else -> Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = scheme.onPrimary, modifier = Modifier.size(30.dp))
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
        PlayButton(key, message.chatId, title, audio.audio, telegram)
        Column(Modifier.width(WaveWidth)) {
            EmojiText(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (active) {
                Canvas(Modifier.fillMaxWidth().height(14.dp).seekable(key, audio.audio, telegram)) {
                    val y = size.height / 2
                    val stroke = 4.dp.toPx()
                    val played = size.width * AudioPlayer.progress(key)
                    drawLine(color.copy(alpha = 0.25f), Offset(0f, y), Offset(size.width, y), stroke, StrokeCap.Round)
                    drawLine(color, Offset(0f, y), Offset(played.coerceAtLeast(stroke / 2), y), stroke, StrokeCap.Round)
                    drawCircle(color, radius = stroke * 1.2f, center = Offset(played, y))
                }
                Text(
                    formatDuration(AudioPlayer.position.toInt()) + " / " + formatDuration(audio.duration),
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
                    LinearProgressIndicator(
                        progress = { option.votePercentage / 100f },
                        color = scheme.primary,
                        trackColor = scheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
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
    val (busy, open) = rememberOpener(file, telegram, ::openFile)
    Box(
        Modifier
            .size(width, (width / ratio).coerceIn(120.dp, 420.dp))
            .clip(RoundedCornerShape(corner))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(onClick = open),
        contentAlignment = Alignment.Center,
    ) {
        ThumbImage(thumb, mini, telegram, Modifier.fillMaxSize())
        PlayBadge(busy)
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
private fun PlayBadge(busy: Boolean) {
    Box(Modifier.size(52.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
        if (busy) LoadingIndicator(Modifier.size(34.dp), color = Color.White) else
            Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(30.dp))
    }
}

@Composable
fun BareContent(message: UiMessage, telegram: Telegram) {
    when (val content = message.content) {
        is TdApi.MessageSticker -> StickerView(content.sticker, telegram, StickerSize)
        is TdApi.MessageAnimatedEmoji -> {
            val sticker = content.animatedEmoji?.sticker
            if (sticker != null) StickerView(sticker, telegram, StickerSize / 1.4f) else
                Text(content.emoji, fontSize = 64.sp)
        }
        is TdApi.MessageDice -> {
            val sticker = (content.finalState as? TdApi.DiceStickersRegular)?.sticker
            if (sticker != null) StickerView(sticker, telegram, StickerSize / 1.4f) else
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
            subtitle = "${content.gift.starCount} Stars"
        }
        is TdApi.MessageUpgradedGift -> {
            sticker = content.gift.model?.sticker
            title = "${content.gift.title} #${content.gift.number}"
            subtitle = "Collectible gift"
        }
        is TdApi.MessageGiftedStars -> {
            sticker = content.sticker
            title = "${content.starCount} Stars"
            subtitle = "Gift"
        }
        is TdApi.MessageGiftedPremium -> {
            sticker = content.sticker
            title = "Telegram Premium"
            subtitle = if (content.monthCount > 0) "for ${content.monthCount} months" else "for ${content.dayCount} days"
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
            FilledTonalButton(onClick = {}) { Text("View") }
        }
    }
}

fun giftServiceText(message: UiMessage, telegram: Telegram): String? = when (val content = message.content) {
    is TdApi.MessageGift ->
        if (message.isOutgoing) "You sent a gift for ${content.gift.starCount} Stars"
        else "${telegram.senderTitle(content.senderId)} sent you a gift for ${content.gift.starCount} Stars"
    is TdApi.MessageUpgradedGift -> "Collectible gift"
    is TdApi.MessageGiftedStars -> if (message.isOutgoing) "You gifted ${content.starCount} Stars" else "You received ${content.starCount} Stars"
    is TdApi.MessageGiftedPremium -> if (message.isOutgoing) "You gifted Telegram Premium" else "You received Telegram Premium"
    else -> null
}

val StickerSize = 180.dp
private val VideoNoteSize = 220.dp
private val GiftWidth = 240.dp
