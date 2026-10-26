package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.telegram.UiMessage
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.theme.EmojiText
import it.tdlight.jni.TdApi

@Composable
fun ChecklistContent(message: UiMessage, list: TdApi.Checklist, telegram: Telegram) {
    val done = list.tasks.count { it.completionDate != 0 }
    Column(Modifier.widthIn(min = 240.dp, max = 360.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        EmojiText(list.title.text, style = MaterialTheme.typography.titleSmall)
        Text(
            "$done of ${list.tasks.size} completed",
            style = MaterialTheme.typography.bodySmall,
            color = LocalContentColorAlpha(),
        )
        list.tasks.forEach { task ->
            val checked = task.completionDate != 0
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = list.canMarkTasksAsDone) { telegram.toggleChecklistTask(message, task.id, !checked) },
            ) {
                Checkbox(checked = checked, onCheckedChange = null, enabled = list.canMarkTasksAsDone, modifier = Modifier.padding(8.dp))
                Column(Modifier.weight(1f)) {
                    EmojiText(
                        task.text.text,
                        style = MaterialTheme.typography.bodyMedium.copy(textDecoration = if (checked) TextDecoration.LineThrough else null),
                    )
                    if (checked && task.completedBy != null && !message.isOutgoing) {
                        Text(telegram.senderTitle(task.completedBy), style = MaterialTheme.typography.labelSmall, color = LocalContentColorAlpha())
                    }
                }
            }
        }
    }
}

@Composable
fun RichMessageContent(message: UiMessage, rich: TdApi.RichMessage, telegram: Telegram) {
    var full by remember(message.id) { mutableStateOf<TdApi.RichMessage?>(null) }
    var expanded by remember(message.id) { mutableStateOf(false) }
    LaunchedEffect(expanded) {
        if (expanded && !rich.isFull && full == null) telegram.fullRichMessage(message) { full = it }
    }
    val shown = if (expanded) full ?: rich else rich
    Column(Modifier.widthIn(min = 260.dp, max = 460.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        shown.blocks.forEach { PageBlockView(it, telegram) }
        if (!rich.isFull && !expanded) {
            TextButton(onClick = { expanded = true }) { Text("Read the full article") }
        }
    }
}

@Composable
private fun PageBlockView(block: TdApi.PageBlock, telegram: Telegram) {
    val accent = MaterialTheme.colorScheme.primary
    val type = MaterialTheme.typography
    when (block) {
        is TdApi.PageBlockTitle -> RichLine(block.title, type.headlineSmall)
        is TdApi.PageBlockSubtitle -> RichLine(block.subtitle, type.titleMedium, LocalContentColorAlpha())
        is TdApi.PageBlockKicker -> RichLine(block.kicker, type.labelLarge, accent)
        is TdApi.PageBlockHeader -> RichLine(block.header, type.titleLarge)
        is TdApi.PageBlockSubheader -> RichLine(block.subheader, type.titleMedium)
        is TdApi.PageBlockSectionHeading -> RichLine(block.text, if (block.size <= 1) type.titleLarge else type.titleMedium)
        is TdApi.PageBlockParagraph -> RichLine(block.text, type.bodyMedium)
        is TdApi.PageBlockThinking -> RichLine(block.text, type.bodyMedium.copy(fontStyle = FontStyle.Italic), LocalContentColorAlpha())
        is TdApi.PageBlockFooter -> RichLine(block.footer, type.bodySmall, LocalContentColorAlpha())
        is TdApi.PageBlockAuthorDate -> RichLine(block.author, type.labelMedium, LocalContentColorAlpha())
        is TdApi.PageBlockPreformatted -> Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest).padding(12.dp),
        ) { RichLine(block.text, type.bodyMedium.copy(fontFamily = FontFamily.Monospace)) }
        is TdApi.PageBlockMathematicalExpression -> Text(block.expression, style = type.bodyMedium.copy(fontFamily = FontFamily.Monospace))
        is TdApi.PageBlockDivider -> HorizontalDivider(Modifier.padding(vertical = 4.dp))
        is TdApi.PageBlockList -> Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            block.items.forEach { item ->
                Row {
                    Text(
                        when {
                            item.hasCheckbox -> if (item.isChecked) "☑" else "☐"
                            item.label.isNotBlank() -> item.label
                            else -> "•"
                        },
                        style = type.bodyMedium,
                        modifier = Modifier.width(ListMarker),
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) { item.blocks.forEach { PageBlockView(it, telegram) } }
                }
            }
        }
        is TdApi.PageBlockBlockQuote -> Quote(accent) {
            block.blocks.forEach { PageBlockView(it, telegram) }
            Credit(block.credit)
        }
        is TdApi.PageBlockPullQuote -> Quote(accent) {
            RichLine(block.text, type.bodyLarge.copy(fontStyle = FontStyle.Italic))
            Credit(block.credit)
        }
        is TdApi.PageBlockExpandableBlockQuote -> Quote(accent) {
            RichLine(block.text, type.bodyMedium)
            Credit(block.credit)
        }
        is TdApi.PageBlockDetails -> {
            var open by remember(block) { mutableStateOf(block.isOpen) }
            Column {
                Row(Modifier.clip(RoundedCornerShape(8.dp)).clickable { open = !open }.padding(vertical = 4.dp)) {
                    Text(if (open) "▾ " else "▸ ", style = type.titleSmall)
                    RichLine(block.header, type.titleSmall)
                }
                if (open) Column(Modifier.padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { block.blocks.forEach { PageBlockView(it, telegram) } }
            }
        }
        is TdApi.PageBlockTable -> Column(
            Modifier.clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            block.cells.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { cell ->
                        Box(Modifier.weight(1f)) {
                            RichLine(cell.text, if (cell.isHeader) type.labelLarge else type.bodyMedium)
                        }
                    }
                }
            }
        }
        is TdApi.PageBlockPhoto -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val size = block.photo?.sizes?.maxByOrNull { it.width }
            ThumbImage(size?.photo, block.photo?.minithumbnail, telegram, Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).size(width = PhotoWidth, height = PhotoHeight))
            block.caption?.let { caption -> RichLine(caption.text, type.bodySmall, LocalContentColorAlpha()) }
        }
        else -> Unit
    }
}

@Composable
private fun Quote(accent: androidx.compose.ui.graphics.Color, content: @Composable () -> Unit) {
    Row {
        Box(Modifier.width(3.dp).padding(vertical = 2.dp).background(accent, RoundedCornerShape(2.dp)).size(width = 3.dp, height = QuoteBar))
        Column(Modifier.padding(start = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { content() }
    }
}

@Composable
private fun Credit(credit: TdApi.RichText?) {
    if (credit == null || richPlain(credit).isBlank()) return
    RichLine(credit, MaterialTheme.typography.labelMedium, LocalContentColorAlpha())
}

@Composable
private fun RichLine(text: TdApi.RichText?, style: androidx.compose.ui.text.TextStyle, color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified) {
    if (text == null) return
    val accent = MaterialTheme.colorScheme.primary
    val annotated = remember(text, accent) { buildAnnotatedString { appendRich(text, accent) } }
    if (annotated.isEmpty()) return
    androidx.compose.material3.Text(annotated, style = style, color = color)
}

private fun AnnotatedString.Builder.appendRich(text: TdApi.RichText, accent: androidx.compose.ui.graphics.Color) {
    fun styled(style: SpanStyle, inner: TdApi.RichText?) {
        val start = length
        inner?.let { appendRich(it, accent) }
        addStyle(style, start, length)
    }
    when (text) {
        is TdApi.RichTextPlain -> append(text.text)
        is TdApi.RichTexts -> text.texts.forEach { appendRich(it, accent) }
        is TdApi.RichTextBold -> styled(SpanStyle(fontWeight = FontWeight.Bold), text.text)
        is TdApi.RichTextItalic -> styled(SpanStyle(fontStyle = FontStyle.Italic), text.text)
        is TdApi.RichTextUnderline -> styled(SpanStyle(textDecoration = TextDecoration.Underline), text.text)
        is TdApi.RichTextStrikethrough -> styled(SpanStyle(textDecoration = TextDecoration.LineThrough), text.text)
        is TdApi.RichTextFixed -> styled(SpanStyle(fontFamily = FontFamily.Monospace), text.text)
        is TdApi.RichTextMarked -> styled(SpanStyle(background = accent.copy(alpha = MarkAlpha)), text.text)
        is TdApi.RichTextSubscript -> styled(SpanStyle(baselineShift = BaselineShift.Subscript), text.text)
        is TdApi.RichTextSuperscript -> styled(SpanStyle(baselineShift = BaselineShift.Superscript), text.text)
        is TdApi.RichTextUrl -> {
            val start = length
            appendRich(text.text, accent)
            addLink(
                LinkAnnotation.Clickable(text.url, TextLinkStyles(SpanStyle(color = accent, textDecoration = TextDecoration.Underline))) { Links.confirmOpen(text.url) },
                start,
                length,
            )
        }
        is TdApi.RichTextCustomEmoji -> append(text.alternativeText)
        else -> innerRich(text)?.let { appendRich(it, accent) }
    }
}

private fun innerRich(text: TdApi.RichText): TdApi.RichText? =
    runCatching { text.javaClass.getField("text").get(text) as? TdApi.RichText }.getOrNull()

fun richPlain(text: TdApi.RichText): String = when (text) {
    is TdApi.RichTextPlain -> text.text
    is TdApi.RichTexts -> text.texts.joinToString("") { richPlain(it) }
    is TdApi.RichTextCustomEmoji -> text.alternativeText
    else -> innerRich(text)?.let { richPlain(it) }.orEmpty()
}

fun richTitle(rich: TdApi.RichMessage): String = rich.blocks.firstNotNullOfOrNull { block ->
    when (block) {
        is TdApi.PageBlockTitle -> richPlain(block.title)
        is TdApi.PageBlockHeader -> richPlain(block.header)
        is TdApi.PageBlockParagraph -> richPlain(block.text)
        else -> null
    }?.takeIf { it.isNotBlank() }
}.orEmpty()

private val ListMarker = 22.dp
private val PhotoWidth = 400.dp
private val PhotoHeight = 240.dp
private val QuoteBar = 40.dp
private const val MarkAlpha = 0.25f
