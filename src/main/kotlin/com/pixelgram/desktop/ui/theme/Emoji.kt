package com.pixelgram.desktop.ui.theme

import com.pixelgram.desktop.ui.main.CustomEmojiView
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.LocalTextStyle
import com.pixelgram.desktop.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import com.pixelgram.desktop.core.AppLog
import com.pixelgram.desktop.core.AppPaths
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.core.EmojiStyle
import com.pixelgram.desktop.ui.components.loadImage
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

internal val FlatEmoji: FontFamily by lazy {
    FontFamily(listOfNotNull(fontBytes("NotoColorEmoji")?.let { Font("NotoColorEmoji", it) }))
}

class ImageEmoji(
    private val folder: String,
    private val label: String,
    private val names: (IntArray) -> List<String>,
    private val url: (String) -> String,
) {
    private val dir: File get() = File(AppPaths.dataDir, "emoji/$folder")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gate = Semaphore(MaxDownloads)
    private val pending = ConcurrentHashMap<String, Deferred<File?>>()
    private val missing: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val onDisk = ConcurrentHashMap<String, File>()

    private fun candidates(emoji: String) = names(emoji.codePoints().toArray())

    fun cached(emoji: String): File? = onDisk[emoji] ?: candidates(emoji).firstNotNullOfOrNull { name ->
        File(dir, name).takeIf { it.isFile }
    }?.also { onDisk[emoji] = it }

    suspend fun file(emoji: String): File? {
        cached(emoji)?.let { return it }
        val first = candidates(emoji).firstOrNull() ?: return null
        return pending.getOrPut(first) {
            scope.async {
                gate.withPermit { candidates(emoji).firstNotNullOfOrNull { name -> if (name in missing) null else download(name) } }
                    .also { pending.remove(first) }
            }
        }.await()?.also { onDisk[emoji] = it }
    }

    private fun download(name: String): File? = try {
        val connection = URI(url(name)).toURL().openConnection() as HttpURLConnection
        if (connection.responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
            missing += name
            null
        } else {
            dir.mkdirs()
            val target = File(dir, name)
            val part = File(dir, "$name.part")
            connection.inputStream.use { input -> part.outputStream().use { input.copyTo(it) } }
            if (part.renameTo(target)) target else null
        }
    } catch (e: Exception) {
        AppLog.write("$label emoji $name failed to download", e)
        null
    }

    private companion object {
        const val MaxDownloads = 6
    }
}

val Emoji3D = ImageEmoji(
    folder = "3d",
    label = "Noto 3D",
    names = { points -> listOf("emoji_u" + points.filter { it != VariationSelector }.joinToString("_") { "%04x".format(it) } + ".png") },
    url = { name -> "https://raw.githubusercontent.com/googlefonts/noto-emoji/main/3D/png/128/$name" },
)

val AppleEmoji = ImageEmoji(
    folder = "apple-$AppleEmojiVersion",
    label = "Apple",
    names = { points ->
        val exact = points.joinToString("-") { "%x".format(it) }
        val bare = points.filter { it != VariationSelector }.joinToString("-") { "%x".format(it) }
        val qualified = if (points.size == 1) "$bare-fe0f" else exact
        listOf(exact, qualified, bare).distinct().map { "$it.png" }
    },
    url = { name -> "https://cdn.jsdelivr.net/npm/emoji-datasource-apple@$AppleEmojiVersion/img/apple/64/$name" },
)

private const val AppleEmojiVersion = "16.0.0"

fun removeOldEmojiFont() {
    File(AppPaths.dataDir, "emoji/Noto-3D.ttf").delete()
}

object EmojiFonts {
    var settings: AppSettings? = null

    val use3D: Boolean get() = settings?.emojiStyle?.value.let { it == EmojiStyle.Noto3D || it == EmojiStyle.Apple }

    val images: ImageEmoji get() = if (settings?.emojiStyle?.value == EmojiStyle.Apple) AppleEmoji else Emoji3D

    val flat: FontFamily get() = FlatEmoji
}

private const val Zwj = 0x200D
private const val VariationSelector = 0xFE0F
private const val Keycap = 0x20E3

private fun isRegional(cp: Int) = cp in 0x1F1E6..0x1F1FF
private fun isTag(cp: Int) = cp in 0xE0020..0xE007F

private fun isNewEmoji(cp: Int) = cp in 0x1F000..0x1FAFF && !Character.isDefined(cp)

fun emojiRanges(text: String): List<IntRange> {
    val ranges = mutableListOf<IntRange>()
    var i = 0
    while (i < text.length) {
        val cp = text.codePointAt(i)
        val after = i + Character.charCount(cp)
        val next = if (after < text.length) text.codePointAt(after) else -1
        val starts = Character.isEmojiPresentation(cp) ||
            isNewEmoji(cp) ||
            isRegional(cp) ||
            (Character.isExtendedPictographic(cp) && (next == VariationSelector || next == Zwj)) ||
            ((cp in '0'.code..'9'.code || cp == '#'.code || cp == '*'.code) &&
                (next == VariationSelector || next == Keycap))
        if (!starts) {
            i = after
            continue
        }
        val start = i
        i = after
        if (isRegional(cp) && i < text.length && isRegional(text.codePointAt(i))) i += 2
        while (i < text.length) {
            val c = text.codePointAt(i)
            when {
                c == VariationSelector || c == Keycap || Character.isEmojiModifier(c) || isTag(c) ->
                    i += Character.charCount(c)
                c == Zwj && i + 1 < text.length -> {
                    i += 1
                    i += Character.charCount(text.codePointAt(i))
                }
                else -> break
            }
        }
        ranges += start until i
    }
    return ranges
}

fun qualifyEmoji(text: String): String {
    if (text.indexOf(Zwj.toChar()) < 0) return text
    val out = StringBuilder(text.length + 2)
    var i = 0
    while (i < text.length) {
        val cp = text.codePointAt(i)
        val after = i + Character.charCount(cp)
        out.appendCodePoint(cp)
        val next = if (after < text.length) text.codePointAt(after) else -1
        if (next == Zwj && Character.isExtendedPictographic(cp) && !Character.isEmojiPresentation(cp)) {
            out.appendCodePoint(VariationSelector)
        }
        i = after
    }
    return out.toString()
}

fun emojiText(text: String): AnnotatedString {
    val ranges = emojiRanges(text)
    if (ranges.isEmpty()) return AnnotatedString(text)
    val style = SpanStyle(fontFamily = FlatEmoji)
    return buildAnnotatedString {
        append(text)
        ranges.forEach { addStyle(style, it.first, it.last + 1) }
    }
}

@Composable
fun EmojiText(
    raw: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    onTextLayout: (TextLayoutResult) -> Unit = {},
) {
    val text = remember(raw) { qualifyEmoji(raw) }
    val ranges = remember(text) { emojiRanges(text) }
    if (!EmojiFonts.use3D || ranges.isEmpty()) {
        Text(
            emojiText(text),
            modifier = modifier,
            color = color,
            fontSize = fontSize,
            style = style,
            maxLines = maxLines,
            overflow = overflow,
            onTextLayout = onTextLayout,
        )
        return
    }
    val annotated = remember(text) {
        buildAnnotatedString {
            var last = 0
            ranges.forEach { range ->
                append(text.substring(last, range.first))
                val emoji = text.substring(range.first, range.last + 1)
                appendInlineContent(emoji, emoji)
                last = range.last + 1
            }
            append(text.substring(last))
        }
    }
    val inline = remember(text) {
        ranges.map { text.substring(it.first, it.last + 1) }.distinct().associateWith { emoji ->
            InlineTextContent(Placeholder(EmojiSize, EmojiSize, PlaceholderVerticalAlign.TextCenter)) {
                Emoji3DImage(emoji)
            }
        }
    }
    Text(
        annotated,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        style = style,
        maxLines = maxLines,
        overflow = overflow,
        inlineContent = inline,
        onTextLayout = onTextLayout,
    )
}

fun withEmoji(text: AnnotatedString): Pair<AnnotatedString, Map<String, InlineTextContent>> {
    val plain = text.text
    val custom = text.getStringAnnotations(CustomEmojiTag, 0, plain.length)
    val ranges = emojiRanges(plain).filter { range -> custom.none { range.first < it.end && range.last + 1 > it.start } }
    if (ranges.isEmpty() && custom.isEmpty()) return text to emptyMap()
    val builder = AnnotatedString.Builder(text)
    val inline = mutableMapOf<String, InlineTextContent>()
    val slots = mutableListOf<Triple<Int, Int, String>>()
    custom.forEach { annotation ->
        val key = CustomEmojiPrefix + annotation.item
        slots += Triple(annotation.start, annotation.end, key)
        if (key !in inline) {
            val fallback = plain.substring(annotation.start, annotation.end)
            inline[key] = InlineTextContent(Placeholder(EmojiSize, EmojiSize, PlaceholderVerticalAlign.TextCenter)) {
                CustomEmojiView(annotation.item.toLong(), fallback)
            }
        }
    }
    if (EmojiFonts.use3D) {
        ranges.forEach { range ->
            val emoji = plain.substring(range.first, range.last + 1)
            slots += Triple(range.first, range.last + 1, emoji)
            if (emoji !in inline) {
                inline[emoji] = InlineTextContent(Placeholder(EmojiSize, EmojiSize, PlaceholderVerticalAlign.TextCenter)) {
                    Emoji3DImage(emoji)
                }
            }
        }
    } else {
        val style = SpanStyle(fontFamily = FlatEmoji)
        ranges.forEach { builder.addStyle(style, it.first, it.last + 1) }
    }
    slots.sortedBy { it.first }.forEach { (start, end, key) -> builder.addStringAnnotation(InlineContentTag, key, start, end) }
    return builder.toAnnotatedString() to inline
}

const val CustomEmojiTag = "pixelgram.customEmoji"

const val CustomEmojiPrefix = "ce:"

const val InlineContentTag = "androidx.compose.foundation.text.inlineContent"

private val EmojiSize = 1.2.em

@Composable
private fun Emoji3DImage(emoji: String) {
    val source = EmojiFonts.images
    key(emoji, source) { SourceEmojiImage(emoji, source) }
}

@Composable
private fun SourceEmojiImage(emoji: String, source: ImageEmoji) {
    val file by produceState(source.cached(emoji), emoji, source) {
        if (value == null) value = source.file(emoji)
    }
    val image by produceState<ImageBitmap?>(null, file) {
        file?.let { loadImage(it.absolutePath) }?.let { value = it }
    }
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val bitmap = image
        if (bitmap != null) {
            Image(bitmap, contentDescription = emoji, modifier = Modifier.fillMaxSize(), filterQuality = FilterQuality.High)
        } else {
            val size = with(LocalDensity.current) { (maxHeight * 0.8f).toSp() }
            Text(emoji, fontFamily = FlatEmoji, fontSize = size)
        }
    }
}

object EmojiVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString) = TransformedText(emojiText(text.text), OffsetMapping.Identity)
}
