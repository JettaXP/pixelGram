package com.pixelgram.desktop.ui.main

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import com.pixelgram.desktop.ui.theme.emojiText
import it.tdlight.jni.TdApi

enum class FormatType(val label: String, val shortcut: String) {
    Bold("Bold", "Ctrl+B"),
    Italic("Italic", "Ctrl+I"),
    Underline("Underline", "Ctrl+U"),
    Strikethrough("Strikethrough", "Ctrl+Shift+X"),
    Quote("Quote", "Ctrl+Shift+."),
    Monospace("Monospace", "Ctrl+Shift+M"),
    Spoiler("Spoiler", "Ctrl+Shift+P"),
    Link("Create link", "Ctrl+K"),
    Date("Date", "Ctrl+Shift+D"),
    Mention("Mention", ""),
}

data class FormatSpan(
    val start: Int,
    val end: Int,
    val type: FormatType,
    val url: String = "",
    val time: Int = 0,
    val userId: Long = 0,
)

fun shiftSpans(spans: List<FormatSpan>, old: String, new: String): List<FormatSpan> {
    if (old == new || spans.isEmpty()) return spans
    var prefix = 0
    val limit = minOf(old.length, new.length)
    while (prefix < limit && old[prefix] == new[prefix]) prefix++
    var suffix = 0
    while (suffix < limit - prefix && old[old.length - 1 - suffix] == new[new.length - 1 - suffix]) suffix++
    val oldEnd = old.length - suffix
    val inserted = new.length - suffix - prefix
    val delta = new.length - old.length
    fun move(position: Int, isEnd: Boolean): Int = when {
        position < prefix || (isEnd && position == prefix) -> position
        position >= oldEnd -> position + delta
        else -> prefix + if (isEnd) inserted else 0
    }
    return spans.map { it.copy(start = move(it.start, false), end = move(it.end, true)) }.filter { it.end > it.start }
}

private fun cut(spans: List<FormatSpan>, from: Int, to: Int, match: (FormatSpan) -> Boolean): List<FormatSpan> =
    spans.flatMap { span ->
        if (!match(span) || span.end <= from || span.start >= to) listOf(span)
        else listOfNotNull(
            span.copy(end = from).takeIf { it.end > it.start },
            span.copy(start = to).takeIf { it.end > it.start },
        )
    }

fun toggleFormat(spans: List<FormatSpan>, selection: TextRange, type: FormatType, url: String = "", time: Int = 0): List<FormatSpan> {
    val from = selection.min
    val to = selection.max
    if (from >= to) return spans
    val covered = (from until to).all { index -> spans.any { it.type == type && index >= it.start && index < it.end } }
    if (covered && type != FormatType.Link && type != FormatType.Date && type != FormatType.Mention) return cut(spans, from, to) { it.type == type }
    val rest = cut(spans, from, to) { it.type == type }
    val added = FormatSpan(from, to, type, url, time)
    if (type == FormatType.Link || type == FormatType.Date || type == FormatType.Mention) return rest + added
    val same = (rest.filter { it.type == type && it.end >= from && it.start <= to } + added)
    val merged = FormatSpan(same.minOf { it.start }, same.maxOf { it.end }, type)
    return rest.filterNot { it.type == type && it.end >= from && it.start <= to } + merged
}

fun clearFormat(spans: List<FormatSpan>, selection: TextRange): List<FormatSpan> =
    if (selection.collapsed) spans else cut(spans, selection.min, selection.max) { true }

fun spansFrom(entities: List<TdApi.TextEntity>): List<FormatSpan> = entities.mapNotNull { entity ->
    val end = entity.offset + entity.length
    when (val type = entity.type) {
        is TdApi.TextEntityTypeBold -> FormatSpan(entity.offset, end, FormatType.Bold)
        is TdApi.TextEntityTypeItalic -> FormatSpan(entity.offset, end, FormatType.Italic)
        is TdApi.TextEntityTypeUnderline -> FormatSpan(entity.offset, end, FormatType.Underline)
        is TdApi.TextEntityTypeStrikethrough -> FormatSpan(entity.offset, end, FormatType.Strikethrough)
        is TdApi.TextEntityTypeBlockQuote, is TdApi.TextEntityTypeExpandableBlockQuote -> FormatSpan(entity.offset, end, FormatType.Quote)
        is TdApi.TextEntityTypeCode, is TdApi.TextEntityTypePre, is TdApi.TextEntityTypePreCode -> FormatSpan(entity.offset, end, FormatType.Monospace)
        is TdApi.TextEntityTypeSpoiler -> FormatSpan(entity.offset, end, FormatType.Spoiler)
        is TdApi.TextEntityTypeTextUrl -> FormatSpan(entity.offset, end, FormatType.Link, url = type.url)
        is TdApi.TextEntityTypeDateTime -> FormatSpan(entity.offset, end, FormatType.Date, time = type.unixTime)
        is TdApi.TextEntityTypeMentionName -> FormatSpan(entity.offset, end, FormatType.Mention, userId = type.userId)
        else -> null
    }
}

fun toEntities(spans: List<FormatSpan>, shift: Int, length: Int): Array<TdApi.TextEntity> = spans.mapNotNull { span ->
    val start = (span.start - shift).coerceIn(0, length)
    val end = (span.end - shift).coerceIn(0, length)
    if (end <= start) return@mapNotNull null
    val type: TdApi.TextEntityType = when (span.type) {
        FormatType.Bold -> TdApi.TextEntityTypeBold()
        FormatType.Italic -> TdApi.TextEntityTypeItalic()
        FormatType.Underline -> TdApi.TextEntityTypeUnderline()
        FormatType.Strikethrough -> TdApi.TextEntityTypeStrikethrough()
        FormatType.Quote -> TdApi.TextEntityTypeBlockQuote()
        FormatType.Monospace -> TdApi.TextEntityTypeCode()
        FormatType.Spoiler -> TdApi.TextEntityTypeSpoiler()
        FormatType.Link -> TdApi.TextEntityTypeTextUrl(span.url)
        FormatType.Mention -> TdApi.TextEntityTypeMentionName(span.userId)
        FormatType.Date -> TdApi.TextEntityTypeDateTime(
            span.time,
            TdApi.DateTimeFormattingTypeAbsolute(TdApi.DateTimePartPrecisionShort(), TdApi.DateTimePartPrecisionLong(), false),
        )
    }
    TdApi.TextEntity(start, end - start, type)
}.toTypedArray()

class FormatTransformation(private val spans: List<FormatSpan>, private val accent: Color) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val builder = AnnotatedString.Builder(emojiText(text.text))
        val length = text.length
        spans.forEach { span ->
            val start = span.start.coerceIn(0, length)
            val end = span.end.coerceIn(start, length)
            if (start >= end) return@forEach
            val style = when (span.type) {
                FormatType.Bold -> SpanStyle(fontWeight = FontWeight.Bold)
                FormatType.Italic -> SpanStyle(fontStyle = FontStyle.Italic)
                FormatType.Underline -> SpanStyle(textDecoration = TextDecoration.Underline)
                FormatType.Strikethrough -> SpanStyle(textDecoration = TextDecoration.LineThrough)
                FormatType.Quote -> SpanStyle(fontStyle = FontStyle.Italic, background = accent.copy(alpha = 0.12f))
                FormatType.Monospace -> SpanStyle(fontFamily = FontFamily.Monospace, background = accent.copy(alpha = 0.12f))
                FormatType.Spoiler -> SpanStyle(background = accent.copy(alpha = 0.3f))
                FormatType.Link -> SpanStyle(color = accent, textDecoration = TextDecoration.Underline)
                FormatType.Date, FormatType.Mention -> SpanStyle(color = accent)
            }
            builder.addStyle(style, start, end)
        }
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }

    override fun equals(other: Any?) = other is FormatTransformation && other.spans == spans && other.accent == accent
    override fun hashCode() = spans.hashCode() * 31 + accent.hashCode()
}
