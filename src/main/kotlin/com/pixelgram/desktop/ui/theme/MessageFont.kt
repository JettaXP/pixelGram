package com.pixelgram.desktop.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.platform.SystemFont
import androidx.compose.ui.unit.sp
import com.pixelgram.desktop.core.AppSettings
import org.jetbrains.skia.FontMgr
import java.io.File

class MessageFont(val family: FontFamily?, val size: Int)

val LocalMessageFont = compositionLocalOf { MessageFont(null, DefaultMessageSize) }

object MessageFonts {
    private val cache = HashMap<String, FontFamily?>()

    val systemFamilies: List<String> by lazy {
        val manager = FontMgr.default
        (0 until manager.familiesCount).map { manager.getFamilyName(it) }
            .filter { it.isNotBlank() && !it.startsWith(".") }
            .distinct()
            .sortedBy { it.lowercase() }
    }

    @OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
    fun family(value: String): FontFamily? = cache.getOrPut(value) {
        when {
            value.isBlank() -> null
            value.startsWith(FilePrefix) -> runCatching {
                val file = File(value.removePrefix(FilePrefix))
                FontFamily(Font(file, FontWeight.Normal))
            }.getOrNull()
            else -> FontFamily(SystemFont(value, FontWeight.Normal), SystemFont(value, FontWeight.Bold))
        }
    }

    fun label(value: String): String = when {
        value.isBlank() -> "Google Sans"
        value.startsWith(FilePrefix) -> File(value.removePrefix(FilePrefix)).nameWithoutExtension
        else -> value
    }

    const val FilePrefix = "file:"
}

fun messageFont(settings: AppSettings) = MessageFont(MessageFonts.family(settings.messageFont.value), settings.messageFontSize.value)

@Composable
fun messageTextStyle(): TextStyle {
    val font = LocalMessageFont.current
    val base = MaterialTheme.typography.bodyLarge
    return base.copy(
        fontFamily = font.family ?: base.fontFamily,
        fontSize = font.size.sp,
        lineHeight = (font.size + LineGap).sp,
    )
}

const val DefaultMessageSize = 16
private const val LineGap = 6
