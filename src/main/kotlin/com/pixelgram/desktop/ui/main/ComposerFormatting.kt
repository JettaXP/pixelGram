package com.pixelgram.desktop.ui.main

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.withStyle
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
    Code("Code block", "Ctrl+Shift+C"),
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
        is TdApi.TextEntityTypeCode -> FormatSpan(entity.offset, end, FormatType.Monospace)
        is TdApi.TextEntityTypePre -> FormatSpan(entity.offset, end, FormatType.Code)
        is TdApi.TextEntityTypePreCode -> FormatSpan(entity.offset, end, FormatType.Code, url = type.language)
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
        FormatType.Code -> if (span.url.isBlank()) TdApi.TextEntityTypePre() else TdApi.TextEntityTypePreCode(span.url)
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

private val CodeFence = Regex("```(?:([A-Za-z0-9_+#.-]+)[ \\t]*\\n)?\\n?([\\s\\S]*?)\\n?```")

fun autoCodeBlock(text: String, spans: List<FormatSpan>, cursor: Int): Triple<String, List<FormatSpan>, Int>? {
    val match = CodeFence.find(text) ?: return null
    val code = match.groupValues[2]
    if (code.isBlank()) return null
    val language = match.groupValues[1]
    val start = match.range.first
    val end = match.range.last + 1
    val updated = text.substring(0, start) + code + text.substring(end)
    val shifted = shiftSpans(spans, text, updated).filterNot { it.end > start && it.start < start + code.length && it.type == FormatType.Code }
    val newCursor = when {
        cursor <= start -> cursor
        cursor >= end -> cursor - (end - start - code.length)
        else -> start + code.length
    }
    return Triple(updated, shifted + FormatSpan(start, start + code.length, FormatType.Code, url = language), newCursor)
}

class FormatTransformation(
    private val spans: List<FormatSpan>,
    private val accent: Color,
    private val code: com.pixelgram.desktop.ui.components.CodeColors? = null,
    private val ghost: String? = null,
    private val ghostColor: Color = Color.Gray,
    private val settled: IntRange? = null,
    private val settledColor: Color = Color.Unspecified,
) : VisualTransformation {
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
                FormatType.Monospace, FormatType.Code -> SpanStyle(fontFamily = FontFamily.Monospace, background = accent.copy(alpha = 0.12f))
                FormatType.Spoiler -> SpanStyle(background = accent.copy(alpha = 0.3f))
                FormatType.Link -> SpanStyle(color = accent, textDecoration = TextDecoration.Underline)
                FormatType.Date, FormatType.Mention -> SpanStyle(color = accent)
            }
            builder.addStyle(style, start, end)
            if (span.type == FormatType.Code && code != null) {
                com.pixelgram.desktop.ui.components.highlightSpans(builder, text.text.substring(start, end), start, span.url, code)
            }
        }
        settled?.let { range ->
            val start = range.first.coerceIn(0, length)
            val end = (range.last + 1).coerceIn(start, length)
            if (start < end) builder.addStyle(SpanStyle(color = settledColor), start, end)
        }
        if (ghost.isNullOrEmpty()) return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
        builder.withStyle(SpanStyle(color = ghostColor)) { append(ghost) }
        return TransformedText(
            builder.toAnnotatedString(),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int) = offset
                override fun transformedToOriginal(offset: Int) = offset.coerceAtMost(length)
            },
        )
    }

    override fun equals(other: Any?) = other is FormatTransformation && other.spans == spans && other.accent == accent && other.code == code && other.ghost == ghost && other.ghostColor == ghostColor &&
        other.settled == settled && other.settledColor == settledColor
    override fun hashCode() = (spans.hashCode() * 31 + accent.hashCode()) * 31 + ghost.hashCode()
}
