package com.pixelgram.desktop.ui.components

import androidx.compose.material3.toShape
import androidx.compose.material3.MaterialShapes
import com.pixelgram.desktop.core.AvatarKind
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.filled.Bookmark
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.pixelgram.desktop.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.telegram.ChatState
import com.pixelgram.desktop.telegram.SavedMessagesTitle
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.AvatarShape
import com.pixelgram.desktop.ui.toShape
import it.tdlight.jni.TdApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image as SkiaImage
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Surface
import java.io.File

private object ImageCache {
    private const val MaxBytes = 128L * 1024 * 1024
    private var bytes = 0L
    private val cache = object : LinkedHashMap<String, ImageBitmap>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>): Boolean {
            if (bytes <= MaxBytes) return false
            bytes -= eldest.value.sizeInBytes()
            return true
        }
    }

    private fun ImageBitmap.sizeInBytes() = width.toLong() * height * 4

    @Synchronized
    fun get(key: String): ImageBitmap? = cache[key]

    @Synchronized
    fun put(key: String, image: ImageBitmap) {
        cache.put(key, image)?.let { bytes -= it.sizeInBytes() }
        bytes += image.sizeInBytes()
    }
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
private val ImageDecoding = Dispatchers.IO.limitedParallelism(DecodeThreads)

private const val DecodeThreads = 4

fun cachedImage(path: String?, maxSize: Int = 0): ImageBitmap? = path?.let { ImageCache.get("$it@$maxSize") }

suspend fun loadImage(path: String): ImageBitmap? = loadImage(path, maxSize = 0)

suspend fun loadImage(path: String, maxSize: Int): ImageBitmap? {
    val key = "$path@$maxSize"
    ImageCache.get(key)?.let { return it }
    return withContext(ImageDecoding) {
        runCatching {
            SkiaImage.makeFromEncoded(File(path).readBytes()).use { image ->
                val longer = maxOf(image.width, image.height)
                if (maxSize <= 0 || longer <= maxSize) {
                    image.toComposeImageBitmap()
                } else {
                    val scale = maxSize.toFloat() / longer
                    val width = (image.width * scale).toInt().coerceAtLeast(1)
                    val height = (image.height * scale).toInt().coerceAtLeast(1)
                    Surface.makeRasterN32Premul(width, height).use { surface ->
                        surface.canvas.drawImageRect(
                            image,
                            Rect.makeWH(image.width.toFloat(), image.height.toFloat()),
                            Rect.makeWH(width.toFloat(), height.toFloat()),
                            SamplingMode.CATMULL_ROM,
                            null,
                            true,
                        )
                        surface.makeImageSnapshot().use { it.toComposeImageBitmap() }
                    }
                }
            }
        }.getOrNull()
    }?.also { ImageCache.put(key, it) }
}

@Composable
fun Avatar(
    title: String,
    photo: TdApi.File?,
    telegram: Telegram,
    settings: AppSettings,
    size: Dp,
    modifier: Modifier = Modifier,
    full: TdApi.File? = null,
    kind: AvatarKind? = null,
) {
    val shape = settings.avatarShapeFor(kind).toShape(settings.avatarCorner.value)
    AvatarImage(title, photo, telegram, shape, size, modifier, full)
}

@Composable
fun ChatAvatar(
    chat: ChatState,
    telegram: Telegram,
    settings: AppSettings,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    if (telegram.isOfficial(chat)) {
        AvatarImage(chat.title, chat.photo, telegram, MaterialShapes.Cookie12Sided.toShape(), size, modifier)
        return
    }
    if (!telegram.isSavedMessages(chat.id)) {
        Avatar(chat.title, chat.photo, telegram, settings, size, modifier, kind = telegram.avatarKind(chat))
        return
    }
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(size)
            .clip(settings.avatarShape.value.toShape(settings.avatarCorner.value))
            .background(colors.primary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.Bookmark,
            contentDescription = SavedMessagesTitle,
            tint = colors.onPrimary,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

@Composable
fun AvatarImage(
    title: String,
    photo: TdApi.File?,
    telegram: Telegram,
    shape: Shape,
    size: Dp,
    modifier: Modifier = Modifier,
    full: TdApi.File? = null,
) {
    LaunchedEffect(photo?.id) { telegram.download(photo) }
    LaunchedEffect(full?.id) { telegram.download(full) }
    val path = telegram.localPath(full) ?: telegram.localPath(photo)
    val image by produceState(cachedImage(path), path) {
        value = cachedImage(path)
        if (value == null) value = path?.let { loadImage(it) }
    }
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(colors.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = image
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
            )
        } else {
            Text(
                text = initials(title),
                color = colors.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold,
                fontSize = (size.value * 0.38f).sp,
            )
        }
    }
}

@Composable
fun ShapePreview(shape: AvatarShape, cornerPercent: Int, size: Dp, selected: Boolean) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .size(size)
            .clip(shape.toShape(cornerPercent))
            .background(if (selected) colors.primary else colors.surfaceContainerHighest),
    )
}

fun initials(title: String): String = title
    .split(' ')
    .mapNotNull { word -> word.codePoints().filter(Character::isLetterOrDigit).findFirst().orElse(-1).takeIf { it >= 0 } }
    .take(2)
    .joinToString("") { String(Character.toChars(it)).uppercase() }
    .ifEmpty { "?" }
