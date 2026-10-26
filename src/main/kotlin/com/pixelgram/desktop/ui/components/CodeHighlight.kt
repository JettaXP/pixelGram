package com.pixelgram.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.ui.components.expressive.IconButton
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.outlined.Check
import com.pixelgram.desktop.ui.icons.outlined.ContentCopy
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CodeColors(
    val keyword: Color,
    val string: Color,
    val number: Color,
    val comment: Color,
    val type: Color,
    val function: Color,
    val annotation: Color,
)

@Composable
fun codeColors(): CodeColors {
    val scheme = MaterialTheme.colorScheme
    return CodeColors(
        keyword = scheme.primary,
        string = scheme.tertiary,
        number = scheme.error,
        comment = scheme.onSurfaceVariant.copy(alpha = CommentAlpha),
        type = scheme.secondary,
        function = scheme.secondary,
        annotation = scheme.error,
    )
}

private class Grammar(
    val keywords: Set<String>,
    val lineComment: List<String>,
    val blockComment: Pair<String, String>?,
    val quotes: String,
    val annotations: Char? = null,
    val tags: Boolean = false,
)

private fun words(text: String) = text.split(' ').filter { it.isNotBlank() }.toSet()

private val CLike = "if else for while do switch case default break continue return goto try catch finally throw new delete this true false null void static const extern struct enum union typedef sizeof"

private val Grammars: Map<String, Grammar> = buildMap {
    val kotlin = Grammar(words("fun val var class object interface data sealed enum annotation companion if else when for while do return break continue try catch finally throw is in as null true false this super import package private public protected internal override open abstract final suspend inline reified lateinit by get set init constructor typealias vararg operator infix tailrec const"), listOf("//"), "/*" to "*/", "\"'", '@')
    put("kotlin", kotlin); put("kt", kotlin); put("kts", kotlin)
    val java = Grammar(words("$CLike class interface extends implements package import public private protected abstract final synchronized volatile transient native instanceof int long short byte char float double boolean var record yield sealed permits"), listOf("//"), "/*" to "*/", "\"'", '@')
    put("java", java)
    val js = Grammar(words("$CLike function var let const class extends import export from as async await yield typeof instanceof in of undefined interface type implements enum readonly public private protected declare namespace keyof"), listOf("//"), "/*" to "*/", "\"'`")
    listOf("js", "javascript", "ts", "typescript", "jsx", "tsx", "mjs").forEach { put(it, js) }
    val python = Grammar(words("def class if elif else for while return import from as with try except finally raise pass break continue lambda yield global nonlocal assert del in is not and or None True False async await match case self print"), listOf("#"), null, "\"'", '@')
    put("python", python); put("py", python)
    val c = Grammar(words("$CLike int long short char float double unsigned signed auto register volatile inline bool class public private protected virtual override template typename namespace using nullptr friend operator include define ifndef endif pragma"), listOf("//"), "/*" to "*/", "\"'", '#')
    listOf("c", "cpp", "c++", "h", "hpp", "cc").forEach { put(it, c) }
    val csharp = Grammar(words("$CLike class interface namespace using public private protected internal abstract sealed override virtual readonly async await var int long string bool double float decimal object base is as in out ref get set record"), listOf("//"), "/*" to "*/", "\"'", '[')
    put("cs", csharp); put("csharp", csharp); put("c#", csharp)
    val go = Grammar(words("func package import var const type struct interface map chan go defer select range if else for switch case default break continue return fallthrough nil true false string int bool error"), listOf("//"), "/*" to "*/", "\"'`")
    put("go", go); put("golang", go)
    val rust = Grammar(words("fn let mut const static struct enum impl trait pub use mod crate self super match if else for while loop return break continue as in ref move async await dyn where unsafe true false Some None Ok Err"), listOf("//"), "/*" to "*/", "\"'", '#')
    put("rust", rust); put("rs", rust)
    val swift = Grammar(words("func let var class struct enum protocol extension import if else guard for while repeat return break continue switch case default in is as try catch throw throws nil true false self Self public private internal fileprivate open static override init deinit"), listOf("//"), "/*" to "*/", "\"", '@')
    put("swift", swift)
    val dart = Grammar(words("$CLike class extends implements with import library part async await var final late required dynamic void int double String bool"), listOf("//"), "/*" to "*/", "\"'", '@')
    put("dart", dart)
    val php = Grammar(words("$CLike function class public private protected echo print require include namespace use extends implements array foreach as fn match"), listOf("//", "#"), "/*" to "*/", "\"'")
    put("php", php)
    val ruby = Grammar(words("def class module if elsif else unless end do while until for in return yield begin rescue ensure raise nil true false self require puts attr_accessor"), listOf("#"), null, "\"'")
    put("ruby", ruby); put("rb", ruby)
    val shell = Grammar(words("if then else elif fi for while do done case esac in function return export local echo exit sudo cd source"), listOf("#"), null, "\"'")
    listOf("bash", "sh", "shell", "zsh", "fish", "console").forEach { put(it, shell) }
    val sql = Grammar(words("select from where insert into values update set delete create table drop alter index join left right inner outer on group by order having limit and or not null as distinct union primary key foreign references SELECT FROM WHERE INSERT INTO VALUES UPDATE SET DELETE CREATE TABLE JOIN ON GROUP BY ORDER LIMIT AND OR NOT NULL AS"), listOf("--"), "/*" to "*/", "'\"")
    put("sql", sql)
    val lua = Grammar(words("function local if then else elseif end for while do repeat until return break in and or not nil true false"), listOf("--"), "--[[" to "]]", "\"'")
    put("lua", lua)
    val json = Grammar(words("true false null"), emptyList(), null, "\"")
    put("json", json)
    val yaml = Grammar(words("true false null yes no"), listOf("#"), null, "\"'")
    put("yaml", yaml); put("yml", yaml)
    val css = Grammar(words("important"), emptyList(), "/*" to "*/", "\"'", '@')
    put("css", css); put("scss", css)
    val markup = Grammar(emptySet(), emptyList(), "<!--" to "-->", "\"'", tags = true)
    listOf("html", "xml", "svg", "vue").forEach { put(it, markup) }
}

private val Fallback = Grammar(words("$CLike fun function def class import return var val let"), listOf("//", "#"), "/*" to "*/", "\"'`")

fun languageLabel(language: String): String = when (language.lowercase()) {
    "" -> "code"
    "kt", "kts" -> "kotlin"
    "py" -> "python"
    "js" -> "javascript"
    "ts" -> "typescript"
    "rb" -> "ruby"
    "rs" -> "rust"
    "sh" -> "bash"
    "cs" -> "c#"
    else -> language.lowercase()
}

fun highlightCode(code: String, language: String, colors: CodeColors): AnnotatedString {
    val grammar = Grammars[language.lowercase()] ?: Fallback
    return buildAnnotatedString {
        append(code)
        highlightInto(this, code, 0, grammar, colors)
    }
}

fun highlightSpans(builder: AnnotatedString.Builder, code: String, offset: Int, language: String, colors: CodeColors) {
    highlightInto(builder, code, offset, Grammars[language.lowercase()] ?: Fallback, colors)
}

private fun highlightInto(builder: AnnotatedString.Builder, code: String, offset: Int, grammar: Grammar, colors: CodeColors) {
    fun mark(style: SpanStyle, start: Int, end: Int) = builder.addStyle(style, offset + start, offset + end)
    var i = 0
    val n = code.length
    while (i < n) {
        val ch = code[i]
        val block = grammar.blockComment
        val line = grammar.lineComment.firstOrNull { code.startsWith(it, i) }
        when {
            block != null && code.startsWith(block.first, i) -> {
                val close = code.indexOf(block.second, i + block.first.length).let { if (it < 0) n else it + block.second.length }
                mark(SpanStyle(color = colors.comment, fontStyle = FontStyle.Italic), i, close)
                i = close
            }
            line != null -> {
                val close = code.indexOf('\n', i).let { if (it < 0) n else it }
                mark(SpanStyle(color = colors.comment, fontStyle = FontStyle.Italic), i, close)
                i = close
            }
            ch in grammar.quotes -> {
                var j = i + 1
                while (j < n && code[j] != ch && (code[j] != '\n' || ch == '`')) {
                    if (code[j] == '\\') j++
                    j++
                }
                val close = (j + 1).coerceAtMost(n)
                mark(SpanStyle(color = colors.string), i, close)
                i = close
            }
            grammar.tags && ch == '<' -> {
                var j = i + 1
                if (j < n && code[j] == '/') j++
                val start = j
                while (j < n && (code[j].isLetterOrDigit() || code[j] == '-' || code[j] == ':')) j++
                if (j > start) mark(SpanStyle(color = colors.keyword), start, j)
                i = j
            }
            grammar.annotations != null && ch == grammar.annotations && i + 1 < n && code[i + 1].isLetter() -> {
                var j = i + 1
                while (j < n && (code[j].isLetterOrDigit() || code[j] == '_' || code[j] == '.')) j++
                mark(SpanStyle(color = colors.annotation), i, j)
                i = j
            }
            ch.isDigit() && (i == 0 || !code[i - 1].isLetterOrDigit() && code[i - 1] != '_') -> {
                var j = i
                while (j < n && (code[j].isLetterOrDigit() || code[j] == '.' || code[j] == '_')) j++
                mark(SpanStyle(color = colors.number), i, j)
                i = j
            }
            ch.isLetter() || ch == '_' || ch == '$' -> {
                var j = i
                while (j < n && (code[j].isLetterOrDigit() || code[j] == '_' || code[j] == '$')) j++
                val word = code.substring(i, j)
                var k = j
                while (k < n && code[k] == ' ') k++
                when {
                    word in grammar.keywords -> mark(SpanStyle(color = colors.keyword), i, j)
                    k < n && code[k] == '(' -> mark(SpanStyle(color = colors.function), i, j)
                    word[0].isUpperCase() && word.length > 1 && !grammar.tags -> mark(SpanStyle(color = colors.type), i, j)
                }
                i = j
            }
            else -> i++
        }
    }
}

@Composable
fun CodeBlock(code: String, language: String, style: TextStyle, modifier: Modifier = Modifier) {
    val colors = codeColors()
    val scheme = MaterialTheme.colorScheme
    val highlighted = remember(code, language, colors.keyword, colors.string) { highlightCode(code, language, colors) }
    var copied by remember(code) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val stripe = scheme.primary
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CodeCorner))
            .background(scheme.surfaceContainerHighest.copy(alpha = CodeBackgroundAlpha))
            .drawBehind { drawRect(stripe, size = androidx.compose.ui.geometry.Size(Stripe.toPx(), size.height)) }
            .padding(start = Stripe),
    ) {
        Column(Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().background(scheme.primary.copy(alpha = HeaderAlpha)).padding(start = 10.dp),
            ) {
                androidx.compose.material3.Text(
                    languageLabel(language),
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.primary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = {
                        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(code), null)
                        copied = true
                        scope.launch {
                            delay(CopiedMs)
                            copied = false
                        }
                    },
                    modifier = Modifier.size(CopyButton),
                ) {
                    Icon(if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy, contentDescription = "Copy code", tint = scheme.primary, modifier = Modifier.size(16.dp))
                }
            }
            SelectionContainer {
                androidx.compose.material3.Text(
                    highlighted,
                    style = style.copy(fontFamily = FontFamily.Monospace, fontSize = style.fontSize * CodeScale),
                    softWrap = false,
                    modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 8.dp),
                )
            }
        }
    }
}

private const val CommentAlpha = 0.7f
private const val CodeBackgroundAlpha = 0.6f
private const val HeaderAlpha = 0.12f
private const val CodeScale = 0.92f
private const val CopiedMs = 1500L
private val CodeCorner = 10.dp
private val CopyButton = 32.dp
private val Stripe = 3.dp
