package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelgram.desktop.core.AppLog
import com.pixelgram.desktop.telegram.StickerSetNameState
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.components.expressive.BasicAlertDialog
import com.pixelgram.desktop.ui.components.expressive.Button
import com.pixelgram.desktop.ui.components.expressive.IconButton
import com.pixelgram.desktop.ui.components.expressive.TextButton
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.filled.Close
import com.pixelgram.desktop.ui.icons.outlined.Add
import com.pixelgram.desktop.ui.icons.outlined.Image
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import it.tdlight.jni.TdApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Surface as SkiaSurface
import java.io.File
import org.jetbrains.skia.Image as SkiaImage

class StickerCreateRequest(val telegram: Telegram, val chatId: Long, val addTo: TdApi.StickerSet? = null)

object StickerCreate {
    var current by mutableStateOf<StickerCreateRequest?>(null)

    private val dir: File get() = File(System.getProperty("java.io.tmpdir"), "pixelgram-stickers").apply { mkdirs() }

    suspend fun pick(): List<File> = runCatching {
        FileKit.openFilePicker(
            type = FileKitType.File(StickerImageExtensions),
            mode = FileKitMode.Multiple(maxItems = StickerPickMax),
            dialogSettings = FileKitDialogSettings(title = com.pixelgram.desktop.core.tr("Choose images")),
        ).orEmpty().map { it.file }.filter { it.isFile }
    }.onFailure { AppLog.write("Sticker picker failed", it) }.getOrDefault(emptyList())

    fun prepare(source: File): File? = runCatching {
        SkiaImage.makeFromEncoded(source.readBytes()).use { image ->
            val scale = StickerSide.toFloat() / maxOf(image.width, image.height)
            val width = (image.width * scale).toInt().coerceIn(1, StickerSide)
            val height = (image.height * scale).toInt().coerceIn(1, StickerSide)
            SkiaSurface.makeRasterN32Premul(width, height).use { surface ->
                surface.canvas.drawImageRect(
                    image,
                    Rect.makeWH(image.width.toFloat(), image.height.toFloat()),
                    Rect.makeWH(width.toFloat(), height.toFloat()),
                    SamplingMode.CATMULL_ROM,
                    null,
                    true,
                )
                surface.makeImageSnapshot().use { scaled ->
                    var quality = StickerQuality
                    var bytes: ByteArray
                    do {
                        bytes = scaled.encodeToData(EncodedImageFormat.WEBP, quality)?.bytes ?: return@runCatching null
                        quality -= 10
                    } while (bytes.size > StickerMaxBytes && quality > 30)
                    File(dir, "${System.nanoTime()}.webp").apply { writeBytes(bytes) }
                }
            }
        }
    }.onFailure { AppLog.write("Sticker image failed", it) }.getOrNull()

    fun preview(file: File): ImageBitmap? = runCatching {
        SkiaImage.makeFromEncoded(file.readBytes()).toComposeImageBitmap()
    }.getOrNull()
}

private class DraftSticker(val file: File, val image: ImageBitmap?, emoji: String) {
    var emoji by mutableStateOf(emoji)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StickerCreateDialog(request: StickerCreateRequest, onDismiss: () -> Unit) {
    val telegram = request.telegram
    val adding = request.addTo
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var nameEdited by remember { mutableStateOf(false) }
    var nameState by remember { mutableStateOf(StickerSetNameState.Invalid) }
    val drafts = remember { mutableStateListOf<DraftSticker>() }
    var loading by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(title) {
        if (adding != null || nameEdited) return@LaunchedEffect
        if (title.isBlank()) {
            name = ""
            return@LaunchedEffect
        }
        delay(NameDelayMs)
        telegram.suggestedStickerSetName(title.trim()) { suggested -> if (!nameEdited && suggested != null) name = suggested }
    }
    LaunchedEffect(name) {
        if (adding != null) return@LaunchedEffect
        if (name.isBlank()) {
            nameState = StickerSetNameState.Invalid
            return@LaunchedEffect
        }
        nameState = StickerSetNameState.Checking
        delay(NameDelayMs)
        telegram.checkStickerSetName(name) { nameState = it }
    }

    val addImages: () -> Unit = {
        scope.launch {
            val files = StickerCreate.pick()
            if (files.isEmpty()) return@launch
            loading = true
            val ready = withContext(Dispatchers.IO) {
                files.mapNotNull { source -> StickerCreate.prepare(source)?.let { DraftSticker(it, StickerCreate.preview(it), DefaultEmoji) } }
            }
            drafts.addAll(ready.take(StickerSetMax - drafts.size))
            if (ready.size < files.size) error = com.pixelgram.desktop.core.tr("Some images could not be opened")
            loading = false
        }
    }

    val canSubmit = !busy && drafts.isNotEmpty() && drafts.all { it.emoji.isNotBlank() } &&
        (adding != null || (title.isNotBlank() && nameState == StickerSetNameState.Ok))

    val submit: () -> Unit = submit@{
        if (!canSubmit) return@submit
        busy = true
        error = null
        val stickers = drafts.map { TdApi.NewSticker(TdApi.InputFileLocal(it.file.absolutePath), TdApi.StickerFormatWebp(), it.emoji.trim(), null, emptyArray()) }
        if (adding != null) {
            var left = stickers.size
            var failure: String? = null
            stickers.forEach { sticker ->
                telegram.addStickerToSet(adding.name, sticker) { message ->
                    if (message != null) failure = message
                    left--
                    if (left == 0) {
                        busy = false
                        if (failure != null) {
                            error = failure
                        } else {
                            onDismiss()
                            StickerPack.current = StickerPackRequest(telegram, request.chatId, adding.id)
                        }
                    }
                }
            }
        } else {
            telegram.createStickerSet(title.trim(), name.trim(), stickers) { set, message ->
                busy = false
                if (set == null) {
                    error = message ?: com.pixelgram.desktop.core.tr("Couldn't create the sticker set")
                } else {
                    telegram.setStickerSetInstalled(set.id, true) {}
                    onDismiss()
                    StickerPack.current = StickerPackRequest(telegram, request.chatId, set.id)
                }
            }
        }
    }

    BasicAlertDialog(onDismissRequest = { if (!busy) onDismiss() }) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.width(DialogWidth),
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    if (adding != null) "Add stickers" else "New sticker set",
                    style = MaterialTheme.typography.headlineSmall,
                )
                if (adding == null) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it.take(TitleMax) },
                        label = { Text("Title") },
                        singleLine = true,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { value ->
                            nameEdited = true
                            name = value.filter { it.isLetterOrDigit() && it.code < 128 || it == '_' }.take(NameMax)
                        },
                        label = { Text("Link") },
                        prefix = { Text("t.me/addstickers/", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        singleLine = true,
                        enabled = !busy,
                        isError = name.isNotEmpty() && (nameState == StickerSetNameState.Occupied || nameState == StickerSetNameState.Invalid),
                        supportingText = {
                            Text(
                                when {
                                    name.isEmpty() -> "Latin letters, digits and underscores"
                                    nameState == StickerSetNameState.Checking -> "Checking…"
                                    nameState == StickerSetNameState.Ok -> "Link is available"
                                    nameState == StickerSetNameState.Occupied -> "This link is already taken"
                                    else -> "This link is not valid"
                                },
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Text(adding.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (drafts.isEmpty()) "Images are resized to 512 px. Each sticker needs an emoji." else "${drafts.size} / $StickerSetMax",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = addImages, enabled = !busy && !loading && drafts.size < StickerSetMax) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Add images", modifier = Modifier.padding(start = 8.dp))
                    }
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = GridMin, max = GridMax)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        loading && drafts.isEmpty() -> LoadingIndicator()
                        drafts.isEmpty() -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
                            Text("PNG, JPG or WEBP", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        else -> LazyVerticalGrid(
                            columns = GridCells.Adaptive(DraftCell),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                        ) {
                            itemsIndexed(drafts, key = { _, draft -> draft.file.path }) { _, draft ->
                                DraftCellView(draft, enabled = !busy) { drafts.remove(draft) }
                            }
                        }
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") }
                    Button(onClick = submit, enabled = canSubmit) {
                        if (busy) LoadingIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                        else Text(if (adding != null) "Add" else "Create")
                    }
                }
            }
        }
    }
}

@Composable
private fun DraftCellView(draft: DraftSticker, enabled: Boolean, onRemove: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(DraftCell)) {
            draft.image?.let { Image(it, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(6.dp)) }
            if (enabled) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.align(Alignment.TopEnd).size(26.dp),
                ) {
                    IconButton(onClick = onRemove, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
            BasicTextField(
                value = draft.emoji,
                onValueChange = { draft.emoji = it.take(EmojiMax) },
                enabled = enabled,
                singleLine = true,
                textStyle = TextStyle(fontSize = 16.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSecondaryContainer),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.width(DraftCell - 16.dp).height(30.dp).padding(horizontal = 8.dp, vertical = 5.dp),
            )
        }
    }
}

private val StickerImageExtensions = listOf("png", "jpg", "jpeg", "webp", "bmp")
private const val StickerSide = 512
private const val StickerQuality = 90
private const val StickerMaxBytes = 500 * 1024
private const val StickerPickMax = 120
private const val StickerSetMax = 120
private const val DefaultEmoji = "🙂"
private const val NameDelayMs = 400L
private const val TitleMax = 64
private const val NameMax = 64
private const val EmojiMax = 20
private val DialogWidth = 520.dp
private val DraftCell = 96.dp
private val GridMin = 140.dp
private val GridMax = 340.dp
