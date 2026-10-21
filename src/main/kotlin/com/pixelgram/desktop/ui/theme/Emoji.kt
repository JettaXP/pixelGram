package com.pixelgram.desktop.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.platform.Font
import com.pixelgram.desktop.core.AppLog
import com.pixelgram.desktop.core.AppPaths
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.core.EmojiStyle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI
import java.security.MessageDigest

// Noto Color Emoji, the latest flat set (SIL Open Font License, see
// fonts/NotoColorEmoji-OFL.txt).
private val FlatEmoji: FontFamily by lazy {
    FontFamily(listOfNotNull(fontBytes("NotoColorEmoji")?.let { Font("NotoColorEmoji", it) }))
}

sealed interface Emoji3DState {
    data object Missing : Emoji3DState
    data class Downloading(val progress: Float) : Emoji3DState
    data object Ready : Emoji3DState
    data class Failed(val reason: String) : Emoji3DState
}

// Google's Noto 3D emoji (Android 17, Pixel 11). The font is downloaded on
// demand from Google's repository, it is too big to ship with the app.
object Emoji3D {
    private const val Url =
        "https://media.githubusercontent.com/media/googlefonts/noto-emoji/main/3D/fonts/Noto-3D-watch.ttf"
    private const val Sha256 = "d5934469c144a951d26be9ae7173a702a6231418e18bfb395fbda3683891838c"
    const val SizeMegabytes = 36

    private val file: File get() = File(AppPaths.dataDir, "emoji/Noto-3D.ttf")
    private val partFile: File get() = File(AppPaths.dataDir, "emoji/Noto-3D.ttf.part")

    var state by mutableStateOf(if (file.isFile) Emoji3DState.Ready else Emoji3DState.Missing)
        private set

    private var job: Job? = null

    val family: FontFamily? by lazy {
        runCatching { FontFamily(Font(file)) }
            .onFailure { AppLog.write("Noto 3D font failed to load", it) }
            .getOrNull()
    }

    fun download(scope: CoroutineScope) {
        if (state is Emoji3DState.Downloading || state == Emoji3DState.Ready) return
        state = Emoji3DState.Downloading(0f)
        job = scope.launch {
            state = try {
                withContext(Dispatchers.IO) { fetch() }
                Emoji3DState.Ready
            } catch (e: CancellationException) {
                partFile.delete()
                Emoji3DState.Missing
            } catch (e: Exception) {
                AppLog.write("Noto 3D download failed", e)
                partFile.delete()
                Emoji3DState.Failed(e.message ?: e.javaClass.simpleName)
            }
        }
    }

    fun cancel() {
        job?.cancel()
    }

    private suspend fun fetch() {
        val part = partFile
        file.parentFile.mkdirs()
        val connection = URI(Url).toURL().openConnection()
        val total = connection.contentLengthLong.takeIf { it > 0 } ?: (SizeMegabytes * 1_000_000L)
        val digest = MessageDigest.getInstance("SHA-256")
        connection.getInputStream().use { input ->
            part.outputStream().use { output ->
                val buffer = ByteArray(64 * 1024)
                var done = 0L
                var shown = 0f
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    digest.update(buffer, 0, read)
                    done += read
                    val progress = (done.toFloat() / total).coerceIn(0f, 1f)
                    if (progress - shown >= 0.005f) {
                        shown = progress
                        withContext(Dispatchers.Main) { state = Emoji3DState.Downloading(progress) }
                    }
                }
            }
        }
        val hash = digest.digest().joinToString("") { "%02x".format(it) }
        check(hash == Sha256) { "The downloaded file is damaged, try again" }
        check(part.renameTo(file)) { "Could not save the font" }
    }
}

// Current emoji font: Noto 3D once downloaded and chosen, otherwise flat Noto.
object EmojiFonts {
    var settings: AppSettings? = null

    val family: FontFamily
        get() {
            val use3D = settings?.emojiStyle?.value == EmojiStyle.Noto3D && Emoji3D.state == Emoji3DState.Ready
            return (if (use3D) Emoji3D.family else null) ?: FlatEmoji
        }
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
    val style = SpanStyle(fontFamily = EmojiFonts.family)
    return buildAnnotatedString {
        append(text)
        ranges.forEach { addStyle(style, it.first, it.last + 1) }
    }
}

// Same for text being typed.
object EmojiVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString) = TransformedText(emojiText(text.text), OffsetMapping.Identity)
}
