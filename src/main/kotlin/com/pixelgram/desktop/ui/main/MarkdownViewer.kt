package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.pixelgram.desktop.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.ui.components.expressive.BasicAlertDialog
import com.pixelgram.desktop.ui.components.expressive.IconButton
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.outlined.FolderOpen
import com.pixelgram.desktop.ui.icons.rounded.Close
import com.pixelgram.desktop.ui.theme.messageTextStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.parser.MarkdownParser
import java.io.File
import com.pixelgram.desktop.core.FileSafety

object MarkdownViewer {
    var path by mutableStateOf<String?>(null)

    fun accepts(name: String) = name.endsWith(".md", ignoreCase = true) || name.endsWith(".markdown", ignoreCase = true)
}

fun openDocument(path: String) {
    when {
        MarkdownViewer.accepts(path) -> MarkdownViewer.path = path
        FileSafety.dangerous(path) -> FileSafety.pending = path
        else -> openFile(path)
    }
}

private class MdDocument(val source: String, val blocks: List<ASTNode>)

@Composable
fun MarkdownDialog(path: String, onDismiss: () -> Unit) {
    val document by produceState<MdDocument?>(null, path) {
        value = withContext(Dispatchers.Default) {
            runCatching {
                val text = File(path).readText()
                val root = MarkdownParser(GFMFlavourDescriptor()).buildMarkdownTreeFromString(text)
                MdDocument(text, root.children.filter { it.type != MarkdownTokenTypes.EOL && it.type != MarkdownTokenTypes.WHITE_SPACE })
            }.getOrNull()
        }
    }
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.widthIn(min = 480.dp, max = 860.dp).fillMaxHeight(0.88f),
        ) {
            Column(Modifier.padding(start = 24.dp, end = 12.dp, top = 12.dp, bottom = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        File(path).name,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { showInFolder(path) }) { Icon(Icons.Outlined.FolderOpen, contentDescription = "Show in folder") }
                    IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, contentDescription = "Close") }
                }
                val doc = document
                if (doc == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Can't open this file", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    SelectionContainer {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize().padding(end = 12.dp)) {
                            items(doc.blocks) { node -> MdBlock(node, doc.source) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MdBlock(node: ASTNode, src: String) {
    val type = node.type
    val typography = MaterialTheme.typography
    val colors = MaterialTheme.colorScheme
    val body = messageTextStyle()
    when (type) {
        MarkdownElementTypes.ATX_1, MarkdownElementTypes.SETEXT_1 -> Header(node, src, typography.headlineMedium)
        MarkdownElementTypes.ATX_2, MarkdownElementTypes.SETEXT_2 -> Header(node, src, typography.headlineSmall)
        MarkdownElementTypes.ATX_3 -> Header(node, src, typography.titleLarge)
        MarkdownElementTypes.ATX_4, MarkdownElementTypes.ATX_5, MarkdownElementTypes.ATX_6 -> Header(node, src, typography.titleMedium)
        MarkdownElementTypes.PARAGRAPH -> Text(inline(node, src), style = body)
        MarkdownElementTypes.UNORDERED_LIST, MarkdownElementTypes.ORDERED_LIST -> MdList(node, src, type == MarkdownElementTypes.ORDERED_LIST)
        MarkdownElementTypes.CODE_FENCE, MarkdownElementTypes.CODE_BLOCK -> Surface(
            shape = MaterialTheme.shapes.medium,
            color = colors.surfaceContainerHighest,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                codeText(node, src),
                style = body.copy(fontFamily = FontFamily.Monospace, fontSize = typography.bodyMedium.fontSize),
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(14.dp),
            )
        }
        MarkdownElementTypes.BLOCK_QUOTE -> Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(colors.primary, RoundedCornerShape(2.dp)))
            Column(Modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                node.children.filter { it.type != MarkdownTokenTypes.BLOCK_QUOTE && it.type != MarkdownTokenTypes.EOL && it.type != MarkdownTokenTypes.WHITE_SPACE }
                    .forEach { MdBlock(it, src) }
            }
        }
        GFMElementTypes.TABLE -> MdTable(node, src)
        MarkdownTokenTypes.HORIZONTAL_RULE -> HorizontalDivider(Modifier.padding(vertical = 6.dp))
        else -> {
            val text = node.getTextInNode(src).toString().trim()
            if (text.isNotEmpty()) Text(text, style = body)
        }
    }
}

@Composable
private fun Header(node: ASTNode, src: String, style: TextStyle) {
    val content = node.children.firstOrNull { it.type == MarkdownTokenTypes.ATX_CONTENT || it.type == MarkdownTokenTypes.SETEXT_CONTENT }
    Text(content?.let { inline(it, src) } ?: AnnotatedString(node.getTextInNode(src).toString().trimStart('#', ' ')), style = style, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun MdList(node: ASTNode, src: String, ordered: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        var number = 1
        node.children.filter { it.type == MarkdownElementTypes.LIST_ITEM }.forEach { item ->
            val marker = item.children.firstOrNull { it.type == MarkdownTokenTypes.LIST_BULLET || it.type == MarkdownTokenTypes.LIST_NUMBER }
            val checkbox = item.children.firstOrNull { it.type == GFMTokenTypes.CHECK_BOX }?.getTextInNode(src)?.toString()
            val label = when {
                checkbox != null -> if (checkbox.contains('x', ignoreCase = true)) "☑" else "☐"
                ordered -> marker?.getTextInNode(src)?.toString()?.trim() ?: "${number}."
                else -> "•"
            }
            number++
            Row {
                Text(label, style = messageTextStyle(), color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(28.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    item.children.filter {
                        it.type != MarkdownTokenTypes.LIST_BULLET && it.type != MarkdownTokenTypes.LIST_NUMBER &&
                            it.type != GFMTokenTypes.CHECK_BOX && it.type != MarkdownTokenTypes.EOL && it.type != MarkdownTokenTypes.WHITE_SPACE
                    }.forEach { MdBlock(it, src) }
                }
            }
        }
    }
}

@Composable
private fun MdTable(node: ASTNode, src: String) {
    val rows = node.children.filter { it.type == GFMElementTypes.HEADER || it.type == GFMElementTypes.ROW }
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.horizontalScroll(rememberScrollState())) {
            rows.forEachIndexed { index, row ->
                val header = row.type == GFMElementTypes.HEADER
                Row(Modifier.background(if (header) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)) {
                    row.children.filter { it.type == GFMTokenTypes.CELL }.forEach { cell ->
                        Text(
                            inline(cell, src),
                            style = messageTextStyle().copy(fontWeight = if (header) FontWeight.Bold else null),
                            modifier = Modifier.width(CellWidth).padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
                if (index < rows.lastIndex) HorizontalDivider()
            }
        }
    }
}

private fun codeText(node: ASTNode, src: String): String =
    node.children.filter { it.type == MarkdownTokenTypes.CODE_FENCE_CONTENT || it.type == MarkdownTokenTypes.CODE_LINE || it.type == MarkdownTokenTypes.EOL }
        .joinToString("") { if (it.type == MarkdownTokenTypes.EOL) "\n" else it.getTextInNode(src).toString() }
        .trim('\n')

@Composable
private fun inline(node: ASTNode, src: String): AnnotatedString {
    val colors = MaterialTheme.colorScheme
    val link = TextLinkStyles(SpanStyle(color = colors.primary, textDecoration = TextDecoration.Underline))
    val code = SpanStyle(fontFamily = FontFamily.Monospace, background = colors.surfaceContainerHighest)
    return buildAnnotatedString {
        fun walk(n: ASTNode) {
            when (n.type) {
                MarkdownElementTypes.STRONG -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { n.children.forEach(::walk) }
                MarkdownElementTypes.EMPH -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { n.children.forEach(::walk) }
                GFMElementTypes.STRIKETHROUGH -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { n.children.forEach(::walk) }
                MarkdownElementTypes.CODE_SPAN -> withStyle(code) {
                    append(n.getTextInNode(src).toString().trim('`'))
                }
                MarkdownElementTypes.INLINE_LINK -> {
                    val label = n.children.firstOrNull { it.type == MarkdownElementTypes.LINK_TEXT }
                    val destination = n.children.firstOrNull { it.type == MarkdownElementTypes.LINK_DESTINATION }?.getTextInNode(src)?.toString().orEmpty()
                    withLink(LinkAnnotation.Url(destination, link)) {
                        label?.children?.filter { it.type != MarkdownTokenTypes.LBRACKET && it.type != MarkdownTokenTypes.RBRACKET }?.forEach(::walk)
                            ?: append(destination)
                    }
                }
                MarkdownElementTypes.AUTOLINK, GFMTokenTypes.GFM_AUTOLINK -> {
                    val url = n.getTextInNode(src).toString().trim('<', '>')
                    withLink(LinkAnnotation.Url(url, link)) { append(url) }
                }
                MarkdownTokenTypes.EMPH, MarkdownTokenTypes.BACKTICK, MarkdownTokenTypes.BLOCK_QUOTE -> Unit
                MarkdownTokenTypes.EOL -> append(' ')
                else -> if (n.children.isEmpty()) append(n.getTextInNode(src).toString()) else n.children.forEach(::walk)
            }
        }
        node.children.forEach(::walk)
    }.trimmed()
}

private fun AnnotatedString.trimmed(): AnnotatedString {
    val start = text.indexOfFirst { !it.isWhitespace() }.takeIf { it >= 0 } ?: return AnnotatedString("")
    val end = text.indexOfLast { !it.isWhitespace() } + 1
    return subSequence(start, end)
}

private val CellWidth = 200.dp
