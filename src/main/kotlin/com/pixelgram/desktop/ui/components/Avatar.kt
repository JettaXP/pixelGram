package com.pixelgram.desktop.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.AvatarShape
import com.pixelgram.desktop.ui.toShape
import it.tdlight.jni.TdApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image as SkiaImage
import java.io.File

private object ImageCache {
    private const val MaxEntries = 300
    private val cache = object : LinkedHashMap<String, ImageBitmap>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>) =
            size > MaxEntries
    }

    @Synchronized
    fun get(path: String): ImageBitmap? = cache[path]

    @Synchronized
    fun put(path: String, image: ImageBitmap) {
        cache[path] = image
    }
}

suspend fun loadImage(path: String): ImageBitmap? {
    ImageCache.get(path)?.let { return it }
    return withContext(Dispatchers.IO) {
        runCatching {
            SkiaImage.makeFromEncoded(File(path).readBytes()).toComposeImageBitmap()
        }.getOrNull()
    }?.also { ImageCache.put(path, it) }
}

@Composable
fun Avatar(
    title: String,
    photo: TdApi.File?,
    telegram: Telegram,
    settings: AppSettings,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val shape = settings.avatarShape.value.toShape(settings.avatarCorner.value)
    AvatarImage(title, photo, telegram, shape, size, modifier)
}

@Composable
fun AvatarImage(
    title: String,
    photo: TdApi.File?,
    telegram: Telegram,
    shape: Shape,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(photo?.id) { telegram.download(photo) }
    val path = telegram.localPath(photo)
    val image by produceState<ImageBitmap?>(null, path) {
        value = path?.let { loadImage(it) }
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

// Shape preview without a real photo, used in settings.
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
    .filter { it.isNotBlank() }
    .take(2)
    .joinToString("") { it.first().uppercase() }
    .ifEmpty { "?" }
