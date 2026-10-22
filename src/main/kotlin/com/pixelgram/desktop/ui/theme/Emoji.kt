package com.pixelgram.desktop.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
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

// Noto Color Emoji, the latest flat set (SIL Open Font License, see
// fonts/NotoColorEmoji-OFL.txt).
internal val FlatEmoji: FontFamily by lazy {
    FontFamily(listOfNotNull(fontBytes("NotoColorEmoji")?.let { Font("NotoColorEmoji", it) }))
}

// Google's Noto 3D emoji (Android 17, Pixel 11). Each emoji is a picture
// fetched from Google's repository the first time it is shown and cached on
// disk; drawn as an image it stays sharp and centred on every system, which
// the 3D font's bitmaps are not on Windows.
object Emoji3D {
    private const val BaseUrl = "https://raw.githubusercontent.com/googlefonts/noto-emoji/main/3D/png/128/"
    private const val MaxDownloads = 6

    private val dir: File get() = File(AppPaths.dataDir, "emoji/3d")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gate = Semaphore(MaxDownloads)
    private val pending = ConcurrentHashMap<String, Deferred<File?>>()
    private val missing: MutableSet<String> = ConcurrentHashMap.newKeySet()

    // Google's file names: code points in hex, without the FE0F selector.
    private fun fileName(emoji: String): String = "emoji_u" + emoji.codePoints()
        .filter { it != VariationSelector }
        .toArray()
        .joinToString("_") { "%04x".format(it) } + ".png"

    fun cached(emoji: String): File? = File(dir, fileName(emoji)).takeIf { it.isFile }

    suspend fun file(emoji: String): File? {
        cached(emoji)?.let { return it }
        val name = fileName(emoji)
        if (name in missing) return null
        return pending.getOrPut(name) {
            scope.async {
                gate.withPermit { download(name) }.also { pending.remove(name) }
            }
        }.await()
    }

    private fun download(name: String): File? = try {
        val connection = URI(BaseUrl + name).toURL().openConnection() as HttpURLConnection
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
        AppLog.write("Noto 3D emoji $name failed to download", e)
        null
    }

    // The 3D font from earlier versions is no longer used.
    fun removeOldFont() {
        File(AppPaths.dataDir, "emoji/Noto-3D.ttf").delete()
    }
}

// The chosen emoji style; read in composition so the UI follows changes.
object EmojiFonts {
    var settings: AppSettings? = null

    val use3D: Boolean get() = settings?.emojiStyle?.value == EmojiStyle.Noto3D

    val flat: FontFamily get() = FlatEmoji
}

private const val Zwj = 0x200D
private const val VariationSelector = 0xFE0F
private const val Keycap = 0x20E3

private fun isRegional(cp: Int) = cp in 0x1F1E6..0x1F1FF
private fun isTag(cp: Int) = cp in 0xE0020..0xE007F

// The JDK knows emoji up to Unicode 15; newer ones are still undefined there.
private fun isNewEmoji(cp: Int) = cp in 0x1F000..0x1FAFF && !Character.isDefined(cp)

// Character ranges of emoji sequences: flags, keycaps, skin tones, ZWJ families.
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
            (Character.isExtendedPictographic(cp) && next == VariationSelector) ||
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

// WHY: Skia takes missing glyphs from the system fonts first, which have no
// flags on Windows and lack new emoji elsewhere, so emoji get Noto explicitly.
fun emojiText(text: String): AnnotatedString {
    val ranges = emojiRanges(text)
    if (ranges.isEmpty()) return AnnotatedString(text)
    val style = SpanStyle(fontFamily = FlatEmoji)
    return buildAnnotatedString {
        append(text)
        ranges.forEach { addStyle(style, it.first, it.last + 1) }
    }
}

// Text with emoji in the chosen style: flat Noto through the font, Noto 3D
// as pictures placed in the line.
@Composable
fun EmojiText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
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
    )
}

private val EmojiSize = 1.2.em

// The picture once it is on disk; until then the flat emoji stands in.
@Composable
private fun Emoji3DImage(emoji: String) {
    val file by produceState(Emoji3D.cached(emoji), emoji) {
        if (value == null) value = Emoji3D.file(emoji)
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

// Text being typed always shows flat emoji: an input field cannot hold pictures.
object EmojiVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString) = TransformedText(emojiText(text.text), OffsetMapping.Identity)
}
