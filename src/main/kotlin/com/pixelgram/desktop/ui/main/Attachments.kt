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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
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

    private val tempDir: File get() = File(AppPaths.dataDir, "temp")

    fun isPhoto(file: File) = file.extension.lowercase() in PhotoExtensions && file.length() <= MaxPhotoBytes

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

@Composable
fun AttachmentsPanel(
    files: List<File>,
    asFiles: Boolean,
    onAsFiles: (Boolean) -> Unit,
    onRemove: (File) -> Unit,
    onAdd: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canBePhotos = files.all(Attachments::isPhoto)
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 3.dp,
        modifier = modifier.fillMaxWidth().animateContentSize(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val kind = if (asFiles || !canBePhotos) "file" else "photo"
                Text(
                    "${files.size} $kind${if (files.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 8.dp),
                )
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                    ToggleButton(
                        checked = !asFiles && canBePhotos,
                        onCheckedChange = { onAsFiles(false) },
                        enabled = canBePhotos,
                        shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                    ) { Text("Photo") }
                    ToggleButton(
                        checked = asFiles || !canBePhotos,
                        onCheckedChange = { onAsFiles(true) },
                        shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                    ) { Text("File") }
                }
                IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Cancel") }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(files, key = { it.absolutePath }) { file ->
                    AttachmentTile(file, showAsPhoto = !asFiles && canBePhotos, onRemove = { onRemove(file) })
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
        }
    }
}

private val TileHeight = 180.dp
private val TileShape = RoundedCornerShape(20.dp)

@Composable
private fun AttachmentTile(file: File, showAsPhoto: Boolean, onRemove: () -> Unit) {
    val image by produceState<ImageBitmap?>(null, file, showAsPhoto) {
        value = if (showAsPhoto) loadImage(file.absolutePath, maxSize = PreviewSize) else null
    }
    val width = if (showAsPhoto) {
        val (w, h) = remember(file) { imageSize(file) }
        if (w > 0 && h > 0) (TileHeight.value * w / h).coerceIn(110f, 320f).dp else TileHeight
    } else {
        200.dp
    }
    Box(Modifier.size(width = width, height = TileHeight).clip(TileShape)) {
        val bitmap = image
        if (bitmap != null) {
            Image(bitmap, contentDescription = file.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
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
