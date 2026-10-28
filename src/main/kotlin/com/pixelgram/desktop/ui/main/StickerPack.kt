package com.pixelgram.desktop.ui.main

import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.automirrored.outlined.Forward
import com.pixelgram.desktop.ui.icons.filled.Close
import com.pixelgram.desktop.ui.icons.filled.MoreVert
import com.pixelgram.desktop.ui.icons.outlined.ContentCopy
import com.pixelgram.desktop.ui.components.expressive.BasicAlertDialog
import com.pixelgram.desktop.ui.components.expressive.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import com.pixelgram.desktop.ui.components.expressive.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.pixelgram.desktop.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.theme.EmojiText
import it.tdlight.jni.TdApi
import java.awt.Toolkit
import java.io.ByteArrayOutputStream
import io.github.vinceglb.filekit.dialogs.openFileSaver
import java.io.File
import java.util.zip.GZIPInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import com.pixelgram.desktop.ui.icons.outlined.NotificationsOff
import com.pixelgram.desktop.ui.icons.outlined.CalendarMonth
import com.pixelgram.desktop.ui.icons.outlined.Star
import com.pixelgram.desktop.ui.icons.outlined.StarOutline
import com.pixelgram.desktop.ui.icons.outlined.Download
import com.pixelgram.desktop.ui.icons.outlined.Delete
import java.awt.datatransfer.StringSelection

class StickerPackRequest(val telegram: Telegram, val chatId: Long, val setId: Long)

object StickerPack {
    var current by mutableStateOf<StickerPackRequest?>(null)
}

private val PackCell = 72.dp
private val PackGridMax = 420.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StickerPackDialog(request: StickerPackRequest, settings: AppSettings, onDismiss: () -> Unit) {
    val telegram = request.telegram
    var set by remember(request.setId) { mutableStateOf<TdApi.StickerSet?>(null) }
    var busy by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var sharing by remember { mutableStateOf(false) }
    var menuFor by remember { mutableStateOf<TdApi.Sticker?>(null) }
    var scheduling by remember { mutableStateOf<TdApi.Sticker?>(null) }
    var favorites by remember { mutableStateOf(emptyList<TdApi.Sticker>()) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    LaunchedEffect(request.setId) { telegram.stickerSet(request.setId) { set = it } }
    LaunchedEffect(Unit) { telegram.favoriteStickers { favorites = it } }
    val loaded = set
    val link = loaded?.let { "https://t.me/${if (it.stickerType is TdApi.StickerTypeCustomEmoji) "addemoji" else "addstickers"}/${it.name}" }
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.width(440.dp),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EmojiText(
                        loaded?.title ?: "Stickers",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                    )
                    Box {
                        IconButton(onClick = { menu = true }, enabled = link != null) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text("Share stickers") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Forward, contentDescription = null) },
                                onClick = {
                                    menu = false
                                    sharing = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Copy link") },
                                leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                                onClick = {
                                    menu = false
                                    link?.let { Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(it), null) }
                                },
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Close") }
                }
                if (loaded == null) {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { LoadingIndicator() }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(PackCell),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(max = PackGridMax),
                    ) {
                        val sendable = loaded.stickerType is TdApi.StickerTypeRegular && request.chatId != 0L
                        items(loaded.stickers.toList(), key = { it.sticker.id }) { sticker ->
                            Box(
                                Modifier
                                    .size(PackCell)
                                    .clip(RoundedCornerShape(12.dp))
                                    .onSecondaryPress { menuFor = sticker }
                                    .then(
                                        if (sendable) {
                                            Modifier.clickable {
                                                telegram.sendSticker(request.chatId, sticker)
                                                onDismiss()
                                            }
                                        } else {
                                            Modifier
                                        },
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                StickerView(sticker, telegram, PackCell, animate = com.pixelgram.desktop.core.PowerSaving.stickersInPanel)
                                if (menuFor === sticker) {
                                    val favorite = favorites.any { it.sticker.id == sticker.sticker.id }
                                    val item = @Composable { label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, danger: Boolean, action: () -> Unit ->
                                        val tint = if (danger) MaterialTheme.colorScheme.error else androidx.compose.ui.graphics.Color.Unspecified
                                        DropdownMenuItem(
                                            text = { Text(label, color = tint) },
                                            leadingIcon = { Icon(icon, contentDescription = null, tint = if (danger) tint else androidx.compose.material3.LocalContentColor.current) },
                                            onClick = {
                                                menuFor = null
                                                action()
                                            },
                                        )
                                    }
                                    DropdownMenu(expanded = true, onDismissRequest = { menuFor = null }, shape = RoundedCornerShape(16.dp)) {
                                        if (sendable) {
                                            item("Send without sound", Icons.Outlined.NotificationsOff, false) {
                                                telegram.sendSticker(request.chatId, sticker, silent = true)
                                                onDismiss()
                                            }
                                            item("Schedule message", Icons.Outlined.CalendarMonth, false) { scheduling = sticker }
                                        }
                                        if (loaded.stickerType is TdApi.StickerTypeRegular) {
                                            item(if (favorite) "Remove from Favorites" else "Add to Favorites", if (favorite) Icons.Outlined.StarOutline else Icons.Outlined.Star, false) {
                                                telegram.setStickerFavorite(sticker, !favorite) { telegram.favoriteStickers { favorites = it } }
                                            }
                                        }
                                        item("Save as image", Icons.Outlined.Download, false) {
                                            scope.launch { StickerImage.save(telegram, sticker) }
                                        }
                                        if (loaded.isOwned) {
                                            item("Delete sticker", Icons.Outlined.Delete, true) {
                                                telegram.removeStickerFromSet(sticker) { telegram.stickerSet(loaded.id) { set = it } }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    val installed = loaded.isInstalled && !loaded.isArchived
                    val noun = if (loaded.stickerType is TdApi.StickerTypeCustomEmoji) "emoji" else "stickers"
                    Button(
                        onClick = {
                            busy = true
                            telegram.setStickerSetInstalled(loaded.id, !installed) {
                                busy = false
                                telegram.stickerSet(loaded.id) { set = it }
                            }
                        },
                        enabled = !busy,
                        colors = if (installed) ButtonDefaults.filledTonalButtonColors() else ButtonDefaults.buttonColors(),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) {
                        Text(if (installed) "Remove $noun" else "Add $noun")
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = if (installed) 0f else 0.2f),
                            modifier = Modifier.padding(start = 8.dp),
                        ) {
                            Text(
                                loaded.stickers.size.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }
        }
    }
    scheduling?.let { sticker ->
        ScheduleDialog(onDismiss = { scheduling = null }) { date ->
            scheduling = null
            telegram.sendSticker(request.chatId, sticker, schedule = TdApi.MessageSchedulingStateSendAtDate(date, 0))
            onDismiss()
        }
    }
    if (sharing && link != null) {
        ForwardDialog(telegram, settings, onDismiss = { sharing = false }) { target ->
            sharing = false
            telegram.sendText(target, link)
        }
    }
}

fun Modifier.openPack(telegram: Telegram, chatId: Long, sticker: TdApi.Sticker?): Modifier =
    if (sticker == null || sticker.setId == 0L) this else clickable {
        StickerPack.current = StickerPackRequest(telegram, chatId, sticker.setId)
    }

private fun Modifier.onSecondaryPress(action: () -> Unit): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                event.changes.first().consume()
                action()
            }
        }
    }
}

object StickerImage {
    suspend fun save(telegram: Telegram, sticker: TdApi.Sticker) {
        val path = telegram.downloaded(sticker.sticker) ?: return
        val png = withContext(Dispatchers.IO) {
            runCatching {
                when (sticker.format) {
                    is TdApi.StickerFormatTgs -> lottie(path)
                    is TdApi.StickerFormatWebm -> video(path)
                    else -> still(path)
                }
            }.onFailure { com.pixelgram.desktop.core.AppLog.write("Sticker image failed", it) }.getOrNull()
        } ?: return
        runCatching {
            val target = io.github.vinceglb.filekit.FileKit.openFileSaver(suggestedName = "sticker-${sticker.sticker.id}", extension = "png") ?: return
            withContext(Dispatchers.IO) { target.file.writeBytes(png) }
        }.onFailure { com.pixelgram.desktop.core.AppLog.write("Save sticker failed", it) }
    }

    private fun still(path: String): ByteArray? =
        org.jetbrains.skia.Image.makeFromEncoded(File(path).readBytes()).use { image -> image.encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)?.bytes }

    private fun lottie(path: String): ByteArray? {
        val json = GZIPInputStream(File(path).inputStream()).use { it.readBytes().decodeToString() }
        return org.jetbrains.skia.skottie.Animation.makeFromString(json).use { animation ->
            org.jetbrains.skia.Surface.makeRasterN32Premul(ImageSide, ImageSide).use { surface ->
                animation.seekFrame(0f)
                animation.render(surface.canvas, org.jetbrains.skia.Rect.makeWH(ImageSide.toFloat(), ImageSide.toFloat()))
                surface.makeImageSnapshot().use { it.encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)?.bytes }
            }
        }
    }

    private fun video(path: String): ByteArray? {
        val grabber = org.bytedeco.javacv.FFmpegFrameGrabber(path).apply {
            videoCodecName = "libvpx-vp9"
            pixelFormat = org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_BGRA
        }
        return try {
            runCatching { grabber.start() }.getOrElse {
                grabber.release()
                return fallbackVideo(path)
            }
            val frame = grabber.grabImage() ?: return null
            val image = org.bytedeco.javacv.Java2DFrameConverter().convert(frame) ?: return null
            ByteArrayOutputStream().use { out ->
                javax.imageio.ImageIO.write(image, "png", out)
                out.toByteArray()
            }
        } finally {
            runCatching { grabber.stop() }
            runCatching { grabber.release() }
        }
    }

    private fun fallbackVideo(path: String): ByteArray? {
        val grabber = org.bytedeco.javacv.FFmpegFrameGrabber(path)
        return try {
            grabber.start()
            val image = grabber.grabImage()?.let { org.bytedeco.javacv.Java2DFrameConverter().convert(it) } ?: return null
            ByteArrayOutputStream().use { out ->
                javax.imageio.ImageIO.write(image, "png", out)
                out.toByteArray()
            }
        } finally {
            runCatching { grabber.stop() }
            runCatching { grabber.release() }
        }
    }

    private const val ImageSide = 512
}
