package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.rememberDialogState
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.components.expressive.IconButton
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.automirrored.filled.Send
import com.pixelgram.desktop.ui.icons.outlined.AutoAwesome
import com.pixelgram.desktop.ui.icons.outlined.AttachFile
import com.pixelgram.desktop.ui.icons.outlined.Close
import com.pixelgram.desktop.ui.icons.outlined.Delete
import com.pixelgram.desktop.ui.icons.outlined.EmojiEmotions
import com.pixelgram.desktop.ui.icons.outlined.FormatListBulleted
import com.pixelgram.desktop.ui.icons.outlined.Functions
import com.pixelgram.desktop.ui.icons.outlined.Redo
import com.pixelgram.desktop.ui.icons.outlined.TableChart
import com.pixelgram.desktop.ui.icons.outlined.TextFields
import com.pixelgram.desktop.ui.icons.outlined.Undo
import com.pixelgram.desktop.ui.theme.EmojiVisualTransformation
import it.tdlight.jni.TdApi
import java.io.File
import kotlinx.coroutines.launch

private enum class BlockType(val label: String) {
    Title("Title"),
    Heading("Heading"),
    Subheading("Subheading"),
    Paragraph("Text"),
    Bullet("Bulleted list"),
    Numbered("Numbered list"),
    Quote("Quote"),
    Pullquote("Pullquote"),
    Code("Code"),
    Footer("Footer"),
    Math("Formula"),
    Table("Table"),
    Divider("Divider"),
    Photo("Photo"),
}

private val TextTypes = listOf(BlockType.Heading, BlockType.Paragraph, BlockType.Quote, BlockType.Pullquote, BlockType.Code, BlockType.Footer)

private class ArticleBlock(type: BlockType, text: String = "", val file: File? = null) {
    var type by mutableStateOf(type)
    var value by mutableStateOf(TextFieldValue(text))
    val focus = FocusRequester()
}

private class Snapshot(val blocks: List<Triple<BlockType, String, File?>>)

@Composable
fun ArticleEditor(telegram: Telegram, chatId: Long, replyToId: Long, onDismiss: () -> Unit, onSent: () -> Unit) {
    val blocks = remember { mutableStateListOf(ArticleBlock(BlockType.Title), ArticleBlock(BlockType.Paragraph)) }
    var current by remember { mutableIntStateOf(0) }
    val undo = remember { mutableStateListOf<Snapshot>() }
    val redo = remember { mutableStateListOf<Snapshot>() }
    var typeMenu by remember { mutableStateOf(false) }
    var linking by remember { mutableStateOf(false) }
    var emoji by remember { mutableStateOf(false) }
    var ai by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(toast) {
        if (toast != null) {
            kotlinx.coroutines.delay(ToastMs)
            toast = null
        }
    }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snapshot = { Snapshot(blocks.map { Triple(it.type, it.value.text, it.file) }) }
    val restore = { state: Snapshot ->
        blocks.clear()
        blocks.addAll(state.blocks.map { (type, text, file) -> ArticleBlock(type, text, file) })
        current = current.coerceIn(0, (blocks.size - 1).coerceAtLeast(0))
    }
    val remember = {
        undo.add(snapshot())
        if (undo.size > HistoryLimit) undo.removeAt(0)
        redo.clear()
    }
    val focused = blocks.getOrNull(current)
    val insert = { block: ArticleBlock ->
        remember()
        val at = (current + 1).coerceAtMost(blocks.size)
        blocks.add(at, block)
        current = at
    }
    val wrap = { before: String, after: String ->
        focused?.let { block ->
            remember()
            val value = block.value
            val start = value.selection.min
            val end = value.selection.max
            val text = value.text.substring(0, start) + before + value.text.substring(start, end) + after + value.text.substring(end)
            block.value = TextFieldValue(text, TextRange(end + before.length + after.length))
        }
    }
    val dialogState = rememberDialogState(size = DpSize(EditorWidth, EditorHeight))
    DialogWindow(onCloseRequest = onDismiss, state = dialogState, title = "New article") {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp)) {
                        Box(Modifier.weight(1f))
                        IconButton(enabled = undo.isNotEmpty(), onClick = {
                            redo.add(snapshot())
                            restore(undo.removeAt(undo.lastIndex))
                        }) { Icon(Icons.Outlined.Undo, contentDescription = "Undo") }
                        IconButton(enabled = redo.isNotEmpty(), onClick = {
                            undo.add(snapshot())
                            restore(redo.removeAt(redo.lastIndex))
                        }) { Icon(Icons.Outlined.Redo, contentDescription = "Redo") }
                    }
                    LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        itemsIndexed(blocks, key = { _, block -> block.hashCode() }) { index, block ->
                            BlockEditor(
                                block,
                                number = if (block.type == BlockType.Numbered) blocks.take(index + 1).takeLastWhile { it.type == BlockType.Numbered }.size else 0,
                                onFocus = { current = index },
                                onChange = { value ->
                                    if (value.text != block.value.text && (undo.isEmpty() || value.text.length % WordStep == 0)) remember()
                                    block.value = value
                                },
                                onEnter = {
                                    val keep = block.type in listOf(BlockType.Bullet, BlockType.Numbered)
                                    if (keep && block.value.text.isEmpty()) {
                                        block.type = BlockType.Paragraph
                                    } else {
                                        remember()
                                        val next = ArticleBlock(if (keep) block.type else BlockType.Paragraph)
                                        blocks.add(index + 1, next)
                                        current = index + 1
                                    }
                                },
                                onBackspaceEmpty = {
                                    if (blocks.size > 1 && index > 0) {
                                        remember()
                                        blocks.removeAt(index)
                                        current = (index - 1).coerceAtLeast(0)
                                    }
                                },
                                onRemove = {
                                    remember()
                                    blocks.removeAt(index)
                                    if (blocks.isEmpty()) blocks.add(ArticleBlock(BlockType.Paragraph))
                                    current = (index - 1).coerceAtLeast(0)
                                },
                                onShortcut = { key ->
                                    when (key) {
                                        Key.B -> wrap("**", "**")
                                        Key.I -> wrap("_", "_")
                                        Key.U -> wrap("__", "__")
                                        Key.K -> linking = true
                                        else -> Unit
                                    }
                                },
                            )
                        }
                    }
                    LaunchedEffect(current, blocks.size) { runCatching { blocks.getOrNull(current)?.focus?.requestFocus() } }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp)) {
                        IconButton(onClick = { ai = true }) { Icon(Icons.Outlined.AutoAwesome, contentDescription = "Write with AI") }
                        Box(Modifier.weight(1f))
                        Box {
                            IconButton(onClick = { emoji = !emoji }) { Icon(Icons.Outlined.EmojiEmotions, contentDescription = "Emoji") }
                            if (emoji) {
                                Popup(alignment = Alignment.BottomStart, offset = androidx.compose.ui.unit.IntOffset(0, -60), onDismissRequest = { emoji = false }, properties = PopupProperties(focusable = true)) {
                                    Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh, shadowElevation = 6.dp, modifier = Modifier.size(EmojiPanelWidth, EmojiPanelHeight)) {
                                        EmojiPanel(onPick = { picked ->
                                            focused?.let { block ->
                                                val value = block.value
                                                val text = value.text.substring(0, value.selection.min) + picked + value.text.substring(value.selection.max)
                                                block.value = TextFieldValue(text, TextRange(value.selection.min + picked.length))
                                            }
                                        }, onSearch = {})
                                    }
                                }
                            }
                        }
                        Box {
                            IconButton(onClick = { typeMenu = true }) { Icon(Icons.Outlined.TextFields, contentDescription = "Text style") }
                            DropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }, shape = RoundedCornerShape(16.dp)) {
                                TextTypes.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type.label) },
                                        leadingIcon = { Text(typeGlyph(type), style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(24.dp)) },
                                        onClick = {
                                            typeMenu = false
                                            focused?.let {
                                                remember()
                                                it.type = type
                                            }
                                        },
                                    )
                                }
                            }
                        }
                        IconButton(onClick = {
                            focused?.let {
                                remember()
                                it.type = if (it.type == BlockType.Bullet) BlockType.Numbered else if (it.type == BlockType.Numbered) BlockType.Paragraph else BlockType.Bullet
                            }
                        }) { Icon(Icons.Outlined.FormatListBulleted, contentDescription = "List") }
                        IconButton(onClick = { insert(ArticleBlock(BlockType.Table, "Column 1 | Column 2\n | ")) }) { Icon(Icons.Outlined.TableChart, contentDescription = "Table") }
                        IconButton(onClick = { insert(ArticleBlock(BlockType.Math, "E = mc^2")) }) { Icon(Icons.Outlined.Functions, contentDescription = "Formula") }
                        IconButton(onClick = {
                            scope.launch { Attachments.pick().firstOrNull(Attachments::isPhoto)?.let { insert(ArticleBlock(BlockType.Photo, "", it)) } }
                        }) { Icon(Icons.Outlined.AttachFile, contentDescription = "Photo") }
                        Box(Modifier.weight(1f))
                        FilledIconButton(
                            enabled = !busy && blocks.any { it.value.text.isNotBlank() || it.file != null },
                            modifier = Modifier.size(SendSize),
                            onClick = {
                                busy = true
                                val content = TdApi.InputMessageRichMessage(
                                    TdApi.InputRichMessage(TdApi.RichMessageSourceBlocks(buildBlocks(blocks)), false, true),
                                    true,
                                )
                                telegram.sendContent(chatId, content, replyToId) { failure ->
                                    busy = false
                                    if (failure == null) onSent() else toast = failure
                                }
                            },
                        ) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send") }
                    }
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = toast != null,
                    enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(initialScale = ToastScale),
                    exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(targetScale = ToastScale),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = ToastBottom),
                ) {
                    var shown by remember { mutableStateOf("") }
                    toast?.let { shown = it }
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.inverseSurface, contentColor = MaterialTheme.colorScheme.inverseOnSurface, shadowElevation = 4.dp) {
                        Text(shown, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
                    }
                }
            }
            if (linking) LinkDialog(onDismiss = { linking = false }) { url ->
                linking = false
                wrap("[", "]($url)")
            }
            if (ai) AiDialog(telegram, onDismiss = { ai = false }, onError = { failure ->
                ai = false
                toast = failure
            }) { result ->
                ai = false
                remember()
                blocks.clear()
                blocks.addAll(fromRich(result))
                if (blocks.isEmpty()) blocks.add(ArticleBlock(BlockType.Paragraph))
            }
        }
    }
}

private fun typeGlyph(type: BlockType): String = when (type) {
    BlockType.Heading -> "H"
    BlockType.Paragraph -> "Aa"
    BlockType.Quote -> "“"
    BlockType.Pullquote -> "”"
    BlockType.Code -> "</>"
    BlockType.Footer -> "≡"
    else -> ""
}

@Composable
private fun BlockEditor(
    block: ArticleBlock,
    number: Int,
    onFocus: () -> Unit,
    onChange: (TextFieldValue) -> Unit,
    onEnter: () -> Unit,
    onBackspaceEmpty: () -> Unit,
    onRemove: () -> Unit,
    onShortcut: (Key) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    val style: TextStyle = when (block.type) {
        BlockType.Title -> type.headlineSmall
        BlockType.Heading -> type.titleLarge
        BlockType.Subheading -> type.titleMedium
        BlockType.Quote -> type.bodyLarge.copy(fontStyle = FontStyle.Italic)
        BlockType.Pullquote -> type.titleLarge.copy(fontStyle = FontStyle.Italic)
        BlockType.Footer -> type.bodySmall.copy(color = colors.onSurfaceVariant)
        BlockType.Code, BlockType.Math, BlockType.Table -> type.bodyMedium.copy(fontFamily = FontFamily.Monospace)
        else -> type.bodyLarge
    }.copy(color = colors.onSurface)
    when (block.type) {
        BlockType.Divider -> Row(verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(Modifier.weight(1f))
            IconButton(onClick = onRemove) { Icon(Icons.Outlined.Close, contentDescription = "Remove") }
        }
        BlockType.Photo -> Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FileImage(block.file?.absolutePath.orEmpty(), Modifier.weight(1f).size(height = PhotoPreview, width = PhotoPreview).clip(RoundedCornerShape(12.dp)), androidx.compose.ui.layout.ContentScale.Crop)
                IconButton(onClick = onRemove) { Icon(Icons.Outlined.Close, contentDescription = "Remove") }
            }
            Field(block, type.bodySmall.copy(color = colors.onSurfaceVariant), "Caption", onFocus, onChange, onEnter, onBackspaceEmpty, onShortcut)
        }
        else -> Row(verticalAlignment = Alignment.Top) {
            val marker = when (block.type) {
                BlockType.Bullet -> "•"
                BlockType.Numbered -> "$number."
                BlockType.Math -> "Σ"
                else -> null
            }
            marker?.let { Text(it, style = style, modifier = Modifier.width(Marker)) }
            if (block.type == BlockType.Quote || block.type == BlockType.Pullquote) Box(Modifier.width(3.dp).size(width = 3.dp, height = QuoteHeight).background(colors.primary, RoundedCornerShape(2.dp)))
            val hint = when (block.type) {
                BlockType.Title -> "Title"
                BlockType.Footer -> "Footer"
                BlockType.Table -> "Cells separated by |, one row per line"
                BlockType.Math -> "LaTeX formula"
                else -> "Write something…"
            }
            Box(
                Modifier.weight(1f).then(
                    if (block.type == BlockType.Code || block.type == BlockType.Table) Modifier.clip(RoundedCornerShape(10.dp)).background(colors.surfaceContainerHighest).padding(10.dp)
                    else Modifier.padding(start = if (block.type == BlockType.Quote || block.type == BlockType.Pullquote) 10.dp else 0.dp),
                ),
            ) {
                Field(block, style, hint, onFocus, onChange, onEnter, onBackspaceEmpty, onShortcut)
            }
        }
    }
}

@Composable
private fun Field(
    block: ArticleBlock,
    style: TextStyle,
    hint: String,
    onFocus: () -> Unit,
    onChange: (TextFieldValue) -> Unit,
    onEnter: () -> Unit,
    onBackspaceEmpty: () -> Unit,
    onShortcut: (Key) -> Unit,
) {
    val multiline = block.type == BlockType.Code || block.type == BlockType.Table
    Box {
        if (block.value.text.isEmpty()) Text(hint, style = style.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = HintAlpha)))
        BasicTextField(
            value = block.value,
            onValueChange = onChange,
            textStyle = style,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            visualTransformation = EmojiVisualTransformation,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(block.focus)
                .onFocusChanged { if (it.isFocused) onFocus() }
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when {
                        event.isCtrlPressed && event.key in listOf(Key.B, Key.I, Key.U, Key.K) -> {
                            onShortcut(event.key)
                            true
                        }
                        event.key == Key.Enter && !event.isShiftPressed && !multiline -> {
                            onEnter()
                            true
                        }
                        event.key == Key.Backspace && block.value.text.isEmpty() -> {
                            onBackspaceEmpty()
                            true
                        }
                        else -> false
                    }
                },
        )
    }
}

@Composable
private fun AiDialog(telegram: Telegram, onDismiss: () -> Unit, onError: (String) -> Unit, onDone: (TdApi.RichMessage) -> Unit) {
    var prompt by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Write with AI") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(prompt, { prompt = it }, placeholder = { Text("What should the article be about?") }, modifier = Modifier.fillMaxWidth())
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        confirmButton = {
            TextButton(enabled = prompt.isNotBlank() && !busy, onClick = {
                busy = true
                telegram.composeArticle(prompt.trim()) { result, failure ->
                    busy = false
                    if (result != null) onDone(result) else onError(failure ?: "Error")
                }
            }) { Text("Create") }
        },
    )
}

private fun fromRich(rich: TdApi.RichMessage): List<ArticleBlock> = rich.blocks.mapNotNull { block ->
    when (block) {
        is TdApi.PageBlockTitle -> ArticleBlock(BlockType.Title, richPlain(block.title))
        is TdApi.PageBlockHeader -> ArticleBlock(BlockType.Heading, richPlain(block.header))
        is TdApi.PageBlockSectionHeading -> ArticleBlock(if (block.size <= 2) BlockType.Heading else BlockType.Subheading, richPlain(block.text))
        is TdApi.PageBlockSubheader -> ArticleBlock(BlockType.Subheading, richPlain(block.subheader))
        is TdApi.PageBlockParagraph -> ArticleBlock(BlockType.Paragraph, richPlain(block.text))
        is TdApi.PageBlockPreformatted -> ArticleBlock(BlockType.Code, richPlain(block.text))
        is TdApi.PageBlockPullQuote -> ArticleBlock(BlockType.Pullquote, richPlain(block.text))
        is TdApi.PageBlockFooter -> ArticleBlock(BlockType.Footer, richPlain(block.footer))
        is TdApi.PageBlockDivider -> ArticleBlock(BlockType.Divider)
        is TdApi.PageBlockMathematicalExpression -> ArticleBlock(BlockType.Math, block.expression)
        else -> null
    }
}

private fun buildBlocks(blocks: List<ArticleBlock>): Array<TdApi.InputPageBlock> {
    val out = mutableListOf<TdApi.InputPageBlock>()
    var list = mutableListOf<TdApi.InputPageBlockListItem>()
    var numbered = false
    fun flush() {
        if (list.isNotEmpty()) out += TdApi.InputPageBlockList(list.toTypedArray())
        list = mutableListOf()
    }
    blocks.forEach { block ->
        val text = block.value.text.trim()
        if (block.type == BlockType.Bullet || block.type == BlockType.Numbered) {
            val isNumbered = block.type == BlockType.Numbered
            if (list.isNotEmpty() && isNumbered != numbered) flush()
            numbered = isNumbered
            if (text.isNotEmpty()) {
                list += TdApi.InputPageBlockListItem(
                    arrayOf(TdApi.InputPageBlockParagraph(richInline(text))),
                    false,
                    false,
                    if (isNumbered) list.size + 1 else 0,
                    if (isNumbered) "1" else "",
                )
            }
            return@forEach
        }
        flush()
        when (block.type) {
            BlockType.Title -> if (text.isNotEmpty()) out += TdApi.InputPageBlockSectionHeading(richInline(text), 1)
            BlockType.Heading -> if (text.isNotEmpty()) out += TdApi.InputPageBlockSectionHeading(richInline(text), 2)
            BlockType.Subheading -> if (text.isNotEmpty()) out += TdApi.InputPageBlockSectionHeading(richInline(text), 3)
            BlockType.Paragraph -> if (text.isNotEmpty()) out += TdApi.InputPageBlockParagraph(richInline(text))
            BlockType.Quote -> if (text.isNotEmpty()) out += TdApi.InputPageBlockBlockQuote(arrayOf(TdApi.InputPageBlockParagraph(richInline(text))), null)
            BlockType.Pullquote -> if (text.isNotEmpty()) out += TdApi.InputPageBlockPullQuote(richInline(text), null)
            BlockType.Footer -> if (text.isNotEmpty()) out += TdApi.InputPageBlockFooter(richInline(text))
            BlockType.Code -> if (text.isNotEmpty()) out += TdApi.InputPageBlockPreformatted(TdApi.RichTextPlain(block.value.text), "")
            BlockType.Math -> if (text.isNotEmpty()) out += TdApi.InputPageBlockMathematicalExpression(text)
            BlockType.Divider -> out += TdApi.InputPageBlockDivider()
            BlockType.Table -> {
                val rows = text.lines().filter { it.isNotBlank() }.mapIndexed { row, line ->
                    line.split('|').map { cell ->
                        TdApi.PageBlockTableCell(richInline(cell.trim()), row == 0, 1, 1, TdApi.PageBlockHorizontalAlignmentLeft(), TdApi.PageBlockVerticalAlignmentTop())
                    }.toTypedArray()
                }
                if (rows.isNotEmpty()) out += TdApi.InputPageBlockTable(TdApi.RichTextPlain(""), rows.toTypedArray(), true, false, false)
            }
            BlockType.Photo -> block.file?.let { file ->
                val (width, height) = com.pixelgram.desktop.telegram.imageSize(file)
                out += TdApi.InputPageBlockPhoto(
                    TdApi.InputPhoto(TdApi.InputFileLocal(file.absolutePath), null, null, IntArray(0), width, height),
                    TdApi.PageBlockCaption(richInline(text), TdApi.RichTextPlain("")),
                    false,
                )
            }
            else -> Unit
        }
    }
    flush()
    return out.toTypedArray()
}

private val InlinePattern = Regex("""\*\*(.+?)\*\*|__(.+?)__|_(.+?)_|~~(.+?)~~|`(.+?)`|\[(.+?)]\((\S+?)\)""")

private fun richInline(text: String): TdApi.RichText {
    val parts = mutableListOf<TdApi.RichText>()
    var last = 0
    InlinePattern.findAll(text).forEach { match ->
        if (match.range.first > last) parts += TdApi.RichTextPlain(text.substring(last, match.range.first))
        val g = match.groupValues
        parts += when {
            g[1].isNotEmpty() -> TdApi.RichTextBold(richInline(g[1]))
            g[2].isNotEmpty() -> TdApi.RichTextUnderline(richInline(g[2]))
            g[3].isNotEmpty() -> TdApi.RichTextItalic(richInline(g[3]))
            g[4].isNotEmpty() -> TdApi.RichTextStrikethrough(richInline(g[4]))
            g[5].isNotEmpty() -> TdApi.RichTextFixed(TdApi.RichTextPlain(g[5]))
            Links.safe(g[7]) -> TdApi.RichTextUrl(richInline(g[6]), g[7], false)
            else -> TdApi.RichTextPlain(match.value)
        }
        last = match.range.last + 1
    }
    if (last < text.length) parts += TdApi.RichTextPlain(text.substring(last))
    return when (parts.size) {
        0 -> TdApi.RichTextPlain("")
        1 -> parts.first()
        else -> TdApi.RichTexts(parts.toTypedArray())
    }
}

private const val HistoryLimit = 100
private const val ToastMs = 3000L
private const val ToastScale = 0.9f
private val ToastBottom = 84.dp
private val SendSize = 52.dp
private const val WordStep = 8
private const val HintAlpha = 0.6f
private val EditorWidth = 520.dp
private val EditorHeight = 680.dp
private val EmojiPanelWidth = 360.dp
private val EmojiPanelHeight = 380.dp
private val PhotoPreview = 200.dp
private val Marker = 24.dp
private val QuoteHeight = 24.dp
