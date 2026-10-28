package com.pixelgram.desktop.ui.main

import io.github.vinceglb.filekit.dialogs.openFileSaver
import java.awt.KeyboardFocusManager
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.FileKitDialogParent
import io.github.vinceglb.filekit.FileKit
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.filled.Close
import com.pixelgram.desktop.ui.icons.filled.PlayArrow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import com.pixelgram.desktop.ui.icons.outlined.Add
import com.pixelgram.desktop.ui.icons.outlined.Description
import androidx.compose.material3.ButtonGroupDefaults
import com.pixelgram.desktop.ui.components.expressive.FilledTonalIconButton
import androidx.compose.material3.Icon
import com.pixelgram.desktop.ui.components.expressive.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import com.pixelgram.desktop.ui.components.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.blur
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import com.pixelgram.desktop.ui.components.GroupedMenu
import com.pixelgram.desktop.ui.components.MenuEntry
import com.pixelgram.desktop.ui.components.MenuGroups
import com.pixelgram.desktop.ui.icons.filled.Check
import com.pixelgram.desktop.ui.icons.filled.MoreVert
import com.pixelgram.desktop.ui.icons.outlined.AutoAwesome
import com.pixelgram.desktop.ui.icons.outlined.CalendarMonth
import com.pixelgram.desktop.ui.icons.outlined.Hd
import com.pixelgram.desktop.ui.icons.outlined.NotificationsOff
import it.tdlight.jni.TdApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AppLog
import com.pixelgram.desktop.core.AppPaths
import com.pixelgram.desktop.telegram.imageSize
import com.pixelgram.desktop.ui.components.loadImage
import com.pixelgram.desktop.ui.storage.formatSize
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Image
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

object Attachments {
    private val PhotoExtensions = setOf("jpg", "jpeg", "png", "webp", "bmp")

    private const val MaxPhotoBytes = 10L * 1024 * 1024

    private val tempDir: File get() = AppPaths.tempDir

    fun isPhoto(file: File) = file.extension.lowercase() in PhotoExtensions && file.length() <= MaxPhotoBytes

    private val VideoExtensions = setOf("mp4", "m4v", "mov", "webm", "mkv")

    fun isVideo(file: File) = file.extension.lowercase() in VideoExtensions

    fun isAnimation(file: File) = file.extension.lowercase() == "gif"

    fun isMedia(file: File) = isPhoto(file) || isVideo(file) || isAnimation(file)

    var highQuality by mutableStateOf(false)

    fun standardQuality(file: File): File = runCatching {
        val bytes = file.readBytes()
        val (width, height) = uprightSize(bytes)
        if (maxOf(width, height) <= StandardSide) return file
        val encoded = decodeUpright(bytes, StandardSide).use { image ->
            org.jetbrains.skia.Surface.makeRasterN32Premul(image.width, image.height).use { surface ->
                surface.canvas.clear(0xFFFFFFFF.toInt())
                surface.canvas.drawImage(image, 0f, 0f)
                surface.makeImageSnapshot().use { it.encodeToData(org.jetbrains.skia.EncodedImageFormat.JPEG, StandardJpegQuality) }
            }
        } ?: return file
        tempDir.mkdirs()
        File(tempDir, "photo-${file.nameWithoutExtension.hashCode()}-${System.nanoTime()}.jpg").also { it.writeBytes(encoded.bytes) }
    }.onFailure { AppLog.write("Photo resize failed", it) }.getOrDefault(file)

    private const val StandardSide = 1280
    private const val StandardJpegQuality = 87

    class VideoInfo(val poster: File?, val width: Int, val height: Int, val duration: Int)

    private val videoInfos = java.util.concurrent.ConcurrentHashMap<String, VideoInfo>()

    fun videoInfo(file: File): VideoInfo = videoInfos.getOrPut(file.absolutePath + ":" + file.lastModified()) {
        runCatching {
            tempDir.mkdirs()
            val grabber = org.bytedeco.javacv.FFmpegFrameGrabber(file.absolutePath)
            grabber.start()
            val frame = grabber.grabImage()
            val image = frame?.let { org.bytedeco.javacv.Java2DFrameConverter().convert(it) }
            val width = grabber.imageWidth
            val height = grabber.imageHeight
            val duration = (grabber.lengthInTime / 1_000_000L).toInt()
            grabber.stop()
            grabber.release()
            val poster = image?.let { picture ->
                val scale = minOf(1.0, PosterMax.toDouble() / maxOf(picture.width, picture.height))
                val scaled = BufferedImage((picture.width * scale).toInt().coerceAtLeast(1), (picture.height * scale).toInt().coerceAtLeast(1), BufferedImage.TYPE_INT_RGB)
                scaled.createGraphics().apply {
                    setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                    drawImage(picture, 0, 0, scaled.width, scaled.height, null)
                    dispose()
                }
                File(tempDir, "poster-${file.nameWithoutExtension.hashCode()}-${System.nanoTime()}.jpg").also { ImageIO.write(scaled, "jpg", it) }
            }
            VideoInfo(poster, width, height, duration)
        }.onFailure { AppLog.write("Video preview failed", it) }.getOrDefault(VideoInfo(null, 0, 0, 0))
    }

    private const val PosterMax = 320

    suspend fun pick(): List<File> {
        val parent = KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow
        return runCatching {
            FileKit.openFilePicker(
                mode = FileKitMode.Multiple(),
                dialogSettings = FileKitDialogSettings(
                    title = "Send files",
                    parent = parent?.let { FileKitDialogParent.awt(it) },
                ),
            ).orEmpty().map { it.file }.filter { it.isFile }
        }.getOrElse {
            AppLog.write("System file picker failed", it)
            awtPick(parent as? Frame)
        }
    }

    private fun awtPick(parent: Frame?): List<File> {
        val dialog = FileDialog(parent, "Send files", FileDialog.LOAD).apply { isMultipleMode = true }
        dialog.isVisible = true
        return dialog.files.orEmpty().filter { it.isFile }
    }

    fun copyImage(image: java.awt.Image) {
        copier.execute {
            runCatching {
                Toolkit.getDefaultToolkit().systemClipboard.setContents(ImageSelection(image), null)
            }.onFailure { AppLog.write("Copy image failed", it) }
        }
    }

    fun copyImage(path: String) {
        copier.execute {
            runCatching {
                val image = ImageIO.read(File(path)) ?: return@runCatching
                Toolkit.getDefaultToolkit().systemClipboard.setContents(ImageSelection(image), null)
            }.onFailure { AppLog.write("Copy image failed", it) }
        }
    }

    private val copier = java.util.concurrent.Executors.newSingleThreadExecutor { task ->
        Thread(task, "image-copy").apply { isDaemon = true }
    }

    suspend fun saveFile(path: String, name: String) {
        val source = File(path)
        runCatching {
            val target = FileKit.openFileSaver(
                suggestedName = name.substringBeforeLast('.').ifBlank { source.nameWithoutExtension },
                extension = name.substringAfterLast('.', source.extension).ifBlank { source.extension },
            ) ?: return
            source.copyTo(target.file, overwrite = true)
        }.onFailure { AppLog.write("Save file failed", it) }
    }

    suspend fun saveImage(path: String) {
        val source = File(path)
        runCatching {
            val target = FileKit.openFileSaver(
                suggestedName = source.nameWithoutExtension.ifBlank { "photo" },
                extension = source.extension.ifBlank { "jpg" },
            ) ?: return
            source.copyTo(target.file, overwrite = true)
        }.onFailure { AppLog.write("Save image failed", it) }
    }

    private class ImageSelection(private val image: java.awt.Image) : java.awt.datatransfer.Transferable {
        override fun getTransferDataFlavors() = arrayOf(DataFlavor.imageFlavor)
        override fun isDataFlavorSupported(flavor: DataFlavor) = flavor == DataFlavor.imageFlavor
        override fun getTransferData(flavor: DataFlavor): Any {
            if (flavor != DataFlavor.imageFlavor) throw java.awt.datatransfer.UnsupportedFlavorException(flavor)
            return image
        }
    }

    fun fromClipboard(): List<File> {
        repeat(ClipboardAttempts) { attempt ->
            val result = runCatching { readClipboard() }
            result.getOrNull()?.let { return it }
            if (attempt == ClipboardAttempts - 1) AppLog.write("Paste failed", result.exceptionOrNull())
            Thread.sleep(ClipboardRetryMs)
        }
        return emptyList()
    }

    private const val ClipboardAttempts = 5
    private const val ClipboardRetryMs = 40L

    private fun readClipboard(): List<File> {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        return when {
            clipboard.isDataFlavorAvailable(DataFlavor.javaFileListFlavor) ->
                (clipboard.getData(DataFlavor.javaFileListFlavor) as List<*>).filterIsInstance<File>().filter { it.isFile }
            clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor) -> {
                val image = clipboard.getData(DataFlavor.imageFlavor) as Image
                listOfNotNull(savePasted(image))
            }
            else -> emptyList()
        }
    }

    private fun savePasted(image: Image): File? {
        val width = image.getWidth(null)
        val height = image.getHeight(null)
        if (width <= 0 || height <= 0) return null
        val buffered = image as? BufferedImage ?: BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).also {
            it.createGraphics().apply { drawImage(image, 0, 0, null) }.dispose()
        }
        tempDir.mkdirs()
        val file = File(tempDir, "pasted-${System.currentTimeMillis()}.png")
        ImageIO.write(buffered, "png", file)
        return file
    }

    fun cleanTemp() {
        val dayAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000
        tempDir.listFiles()?.filter { it.lastModified() < dayAgo }?.forEach { it.delete() }
    }
}

class AttachmentSend(val silent: Boolean = false, val schedule: TdApi.MessageSchedulingState? = null)

@Composable
fun AttachmentsPanel(
    files: List<File>,
    asFiles: Boolean,
    onAsFiles: (Boolean) -> Unit,
    onRemove: (File) -> Unit,
    onAdd: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onEdit: ((File) -> Unit)? = null,
    spoiler: Boolean = false,
    onSpoiler: (Boolean) -> Unit = {},
    onSendWith: ((AttachmentSend) -> Unit)? = null,
) {
    val canBePhotos = files.all(Attachments::isMedia)
    val asMedia = !asFiles && canBePhotos
    var menu by remember { mutableStateOf(false) }
    var scheduling by remember { mutableStateOf(false) }
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 3.dp,
        modifier = modifier.fillMaxWidth().animateContentSize(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val kind = when {
                    asFiles || !canBePhotos -> "file"
                    files.all(Attachments::isVideo) -> "video"
                    files.all(Attachments::isPhoto) -> "photo"
                    files.all(Attachments::isAnimation) -> "GIF"
                    else -> "media file"
                }
                Text(
                    "${files.size} $kind${if (files.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 8.dp),
                )
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                    ToggleButton(
                        checked = asMedia,
                        onCheckedChange = { onAsFiles(false) },
                        enabled = canBePhotos,
                        shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                    ) { Text("Media") }
                    ToggleButton(
                        checked = !asMedia,
                        onCheckedChange = { onAsFiles(true) },
                        shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                    ) { Text("File") }
                }
                if (onSendWith != null) {
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "More") }
                        GroupedMenu(expanded = menu, onDismiss = { menu = false }) {
                            MenuGroups(
                                listOf(
                                    listOf(
                                        MenuEntry(Icons.Outlined.NotificationsOff, "Send without sound") {
                                            menu = false
                                            onSendWith(AttachmentSend(silent = true))
                                        },
                                        MenuEntry(Icons.Outlined.CalendarMonth, "Schedule message") {
                                            menu = false
                                            scheduling = true
                                        },
                                    ),
                                    buildList {
                                        if (asMedia && files.any(Attachments::isPhoto)) {
                                            add(MenuEntry(Icons.Outlined.Hd, "High Quality", trailing = if (Attachments.highQuality) Icons.Filled.Check else null) {
                                                Attachments.highQuality = !Attachments.highQuality
                                            })
                                        }
                                        if (asMedia) {
                                            add(MenuEntry(Icons.Outlined.AutoAwesome, "Hide with Spoiler", trailing = if (spoiler) Icons.Filled.Check else null) {
                                                onSpoiler(!spoiler)
                                            })
                                        }
                                    },
                                ),
                                width = MenuWidth,
                            )
                        }
                    }
                }
                IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Cancel") }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(files, key = { it.absolutePath }) { file ->
                    val editable = asMedia && onEdit != null && Attachments.isPhoto(file)
                    AttachmentTile(
                        file,
                        showAsPhoto = asMedia,
                        spoiler = asMedia && spoiler,
                        onRemove = { onRemove(file) },
                        onEdit = if (editable) ({ onEdit?.invoke(file) }) else null,
                    )
                }
                item(key = "add") {
                    OutlinedCard(
                        onClick = onAdd,
                        shape = TileShape,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.size(width = 96.dp, height = TileHeight),
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Add, contentDescription = "Add files")
                        }
                    }
                }
            }
            if (asMedia && onEdit != null && files.any(Attachments::isPhoto)) {
                Text(
                    "Left-click on the photo to edit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
    if (scheduling && onSendWith != null) {
        ScheduleDialog(onDismiss = { scheduling = false }) { date ->
            scheduling = false
            onSendWith(AttachmentSend(schedule = TdApi.MessageSchedulingStateSendAtDate(date, 0)))
        }
    }
}

private val MenuWidth = 250.dp
private val TileHeight = 180.dp
private val TileShape = RoundedCornerShape(20.dp)

@Composable
private fun AttachmentTile(file: File, showAsPhoto: Boolean, spoiler: Boolean, onRemove: () -> Unit, onEdit: (() -> Unit)?) {
    val video = showAsPhoto && Attachments.isVideo(file)
    val info by produceState<Attachments.VideoInfo?>(null, file, video) {
        value = if (video) kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { Attachments.videoInfo(file) } else null
    }
    val image by produceState<ImageBitmap?>(null, file, showAsPhoto, info) {
        value = when {
            !showAsPhoto -> null
            video -> info?.poster?.let { loadImage(it.absolutePath, maxSize = PreviewSize) }
            else -> loadImage(file.absolutePath, maxSize = PreviewSize)
        }
    }
    val width = when {
        video -> info?.takeIf { it.width > 0 && it.height > 0 }?.let { (TileHeight.value * it.width / it.height).coerceIn(110f, 320f).dp } ?: TileHeight
        showAsPhoto -> {
            val (w, h) = remember(file) { imageSize(file) }
            if (w > 0 && h > 0) (TileHeight.value * w / h).coerceIn(110f, 320f).dp else TileHeight
        }
        else -> 200.dp
    }
    Box(Modifier.size(width = width, height = TileHeight).clip(TileShape)) {
        val bitmap = image
        if (bitmap != null) {
            Image(
                bitmap,
                contentDescription = file.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (spoiler) Modifier.blur(SpoilerBlur) else Modifier)
                    .then(if (onEdit != null) Modifier.pointerHoverIcon(PointerIcon.Hand).clickable(onClick = onEdit) else Modifier),
            )
            if (Attachments.isAnimation(file)) {
                Surface(shape = CircleShape, color = Color.Black.copy(alpha = VideoBadgeAlpha), contentColor = Color.White, modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)) {
                    Text("GIF", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
            if (video) {
                Surface(shape = CircleShape, color = Color.Black.copy(alpha = VideoBadgeAlpha), contentColor = Color.White, modifier = Modifier.align(Alignment.Center).size(48.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(30.dp)) }
                }
                info?.duration?.takeIf { it > 0 }?.let { seconds ->
                    Surface(shape = CircleShape, color = Color.Black.copy(alpha = VideoBadgeAlpha), contentColor = Color.White, modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)) {
                        Text("%d:%02d".format(seconds / 60, seconds % 60), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                }
            }
        } else if (video) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
                androidx.compose.material3.LoadingIndicator(Modifier.size(40.dp))
            }
        } else {
            Column(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHighest).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Outlined.Description, contentDescription = null, modifier = Modifier.size(40.dp))
                Text(file.name, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    formatSize(file.length()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        FilledTonalIconButton(
            onClick = onRemove,
            modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(32.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(),
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Remove", modifier = Modifier.size(18.dp))
        }
    }
}

private const val PreviewSize = 640
private val SpoilerBlur = 18.dp
private const val VideoBadgeAlpha = 0.55f
