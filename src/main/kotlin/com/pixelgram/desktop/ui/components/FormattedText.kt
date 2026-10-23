package com.pixelgram.desktop.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.geometry.Rect
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelgram.desktop.ui.theme.withEmoji
import com.pixelgram.desktop.ui.theme.CustomEmojiTag
import it.tdlight.jni.TdApi
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

object HoverTarget {
    var link: String? = null
    var button: TdApi.InlineKeyboardButton? = null
}

data class FormatColors(val accent: Color, val code: Color, val quote: Color)

fun formattedString(text: String, entities: List<TdApi.TextEntity>, colors: FormatColors): AnnotatedString {
    val length = text.length
    val chars = text.toCharArray()
    entities.filter { it.type is TdApi.TextEntityTypeBlockQuote || it.type is TdApi.TextEntityTypeExpandableBlockQuote }.forEach {
        val before = it.offset - 1
        val after = it.offset + it.length
        if (before in 0 until length && chars[before] == '\n') chars[before] = ParagraphJoint
        if (after in 0 until length && chars[after] == '\n') chars[after] = ParagraphJoint
    }
    val builder = AnnotatedString.Builder(String(chars))
    entities.forEach { entity ->
        val start = entity.offset.coerceIn(0, length)
        val end = (entity.offset + entity.length).coerceIn(start, length)
        if (start >= end) return@forEach
        when (val type = entity.type) {
            is TdApi.TextEntityTypeBold -> builder.addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
            is TdApi.TextEntityTypeItalic -> builder.addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
            is TdApi.TextEntityTypeUnderline -> builder.addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
            is TdApi.TextEntityTypeCode, is TdApi.TextEntityTypePre, is TdApi.TextEntityTypePreCode ->
                builder.addStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = colors.code), start, end)
            is TdApi.TextEntityTypeBlockQuote, is TdApi.TextEntityTypeExpandableBlockQuote ->
                builder.addStyle(ParagraphStyle(textIndent = TextIndent(QuoteIndent, QuoteIndent)), start, end)
            is TdApi.TextEntityTypeTextUrl -> builder.addLink(link(type.url, colors), start, end)
            is TdApi.TextEntityTypeUrl -> builder.addLink(link(text.substring(start, end).let { if ("://" in it) it else "https://$it" }, colors), start, end)
            is TdApi.TextEntityTypeEmailAddress -> builder.addLink(link("mailto:" + text.substring(start, end), colors), start, end)
            is TdApi.TextEntityTypePhoneNumber -> builder.addLink(link("tel:" + text.substring(start, end), colors), start, end)
            is TdApi.TextEntityTypeMention ->
                builder.addLink(link("tg://resolve?domain=" + text.substring(start, end).removePrefix("@"), colors, underline = false), start, end)
            is TdApi.TextEntityTypeMentionName ->
                builder.addLink(link("tg://user?id=${type.userId}", colors, underline = false), start, end)
            is TdApi.TextEntityTypeCustomEmoji ->
                builder.addStringAnnotation(CustomEmojiTag, type.customEmojiId.toString(), start, end)
            is TdApi.TextEntityTypeHashtag,
            is TdApi.TextEntityTypeCashtag, is TdApi.TextEntityTypeBotCommand, is TdApi.TextEntityTypeDateTime ->
                builder.addStyle(SpanStyle(color = colors.accent), start, end)
            else -> Unit
        }
    }
    return builder.toAnnotatedString()
}

private fun link(url: String, colors: FormatColors, underline: Boolean = true) = LinkAnnotation.Url(
    url,
    TextLinkStyles(SpanStyle(color = colors.accent, textDecoration = if (underline) TextDecoration.Underline else null)),
)

fun formatDateEntity(unixTime: Int): String =
    Instant.ofEpochSecond(unixTime.toLong()).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", Locale.ENGLISH))

private fun ranges(entities: List<TdApi.TextEntity>, length: Int, match: (TdApi.TextEntityType) -> Boolean) =
    entities.filter { match(it.type) }.map { it.offset.coerceIn(0, length) until (it.offset + it.length).coerceIn(0, length) }
        .filter { !it.isEmpty() }

@Composable
fun FormattedText(
    text: String,
    entities: List<TdApi.TextEntity>,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    onTextLayout: (TextLayoutResult) -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    val colors = FormatColors(scheme.primary, scheme.onSurface.copy(alpha = 0.1f), scheme.primary)
    val (annotated, inline) = remember(text, entities, colors) { withEmoji(formattedString(text, entities, colors)) }
    if (entities.isEmpty()) {
        Text(annotated, modifier = modifier, style = style, color = color, inlineContent = inline, onTextLayout = onTextLayout)
        return
    }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var revealed by remember(text, entities) { mutableStateOf(emptySet<Int>()) }
    val spoilers = remember(entities, text) { ranges(entities, text.length) { it is TdApi.TextEntityTypeSpoiler } }
    val hidden = spoilers.filter { it.first !in revealed }
    var fadingStart by remember(text, entities) { mutableStateOf<Int?>(null) }
    val fade = remember { Animatable(0f) }
    LaunchedEffect(fadingStart) {
        if (fadingStart == null) return@LaunchedEffect
        fade.snapTo(0f)
        fade.animateTo(1f, tween(RevealMs, easing = FastOutSlowInEasing))
        fadingStart = null
    }
    val fading = spoilers.firstOrNull { it.first == fadingStart }
    var overSpoiler by remember { mutableStateOf(false) }
    val strikes = remember(entities, text) { ranges(entities, text.length) { it is TdApi.TextEntityTypeStrikethrough } }
    val quotes = remember(entities, text) {
        ranges(entities, text.length) { it is TdApi.TextEntityTypeBlockQuote || it is TdApi.TextEntityTypeExpandableBlockQuote }
    }
    val textColor = if (color != Color.Unspecified) color else style.color.takeIf { it != Color.Unspecified } ?: androidx.compose.material3.LocalContentColor.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val fontPx = with(density) { (style.fontSize.takeIf { it.isSp } ?: 16.sp).toPx() }
    val spoilerPath = remember(layout, hidden) {
        Path().apply { layout?.let { l -> hidden.forEach { addPath(pillPath(l, it.first, it.last + 1, density)) } } }
    }
    val fadingPath = remember(layout, fading) {
        Path().apply { layout?.let { l -> fading?.let { addPath(pillPath(l, it.first, it.last + 1, density)) } } }
    }
    Box(
        modifier
            .pointerInput(hidden, layout) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val current = layout ?: continue
                        val change = event.changes.first()
                        val index = current.getOffsetForPosition(change.position)
                        val hit = hidden.firstOrNull { index in it }
                        val onText = event.type != PointerEventType.Exit && current.getBoundingBox(index.coerceAtMost(annotated.length - 1).coerceAtLeast(0)).let {
                            change.position.y in it.top..it.bottom
                        }
                        HoverTarget.link = if (onText) {
                            (annotated.getLinkAnnotations(index, index + 1).firstOrNull()?.item as? LinkAnnotation.Url)?.url
                        } else {
                            null
                        }
                        overSpoiler = hit != null && event.type != PointerEventType.Exit
                        if (event.type != PointerEventType.Press || hit == null || !event.buttons.isPrimaryPressed) continue
                        change.consume()
                        revealed = revealed + hit.first
                        fadingStart = hit.first
                    }
                }
            }
            .pointerHoverIcon(if (overSpoiler) PointerIcon.Hand else PointerIcon.Text, overrideDescendants = overSpoiler),
    ) {
        Text(
            annotated,
            style = style,
            color = color,
            inlineContent = inline,
            onTextLayout = {
                layout = it
                onTextLayout(it)
            },
            modifier = Modifier
                .drawWithContent {
                    val current = layout
                    if (current != null) {
                        quotes.forEach { range -> drawQuote(current, range, colors.quote) }
                        hidden.forEach { range -> drawPath(pillPath(current, range.first, range.last + 1, this), colors.accent.copy(alpha = 0.3f)) }
                        fading?.let { range ->
                            drawPath(pillPath(current, range.first, range.last + 1, this), colors.accent.copy(alpha = 0.3f * (1f - fade.value)))
                        }
                    }
                    val covered = Path().apply {
                        addPath(spoilerPath)
                        if (fading != null) addPath(fadingPath)
                    }
                    if (hidden.isEmpty() && fading == null) {
                        drawContent()
                    } else {
                        clipPath(covered, ClipOp.Difference) { this@drawWithContent.drawContent() }
                    }
                    if (fading != null) {
                        clipPath(fadingPath) {
                            drawIntoCanvas { canvas ->
                                canvas.saveLayer(Rect(Offset.Zero, size), Paint().apply { alpha = fade.value })
                                this@drawWithContent.drawContent()
                                canvas.restore()
                            }
                        }
                    }
                    if (current != null) strikes.forEach { range -> drawWavyStrike(current, range, textColor, fontPx) }
                },
        )
        if (hidden.isNotEmpty()) {
            BlurredCopy(annotated, inline, style, color, spoilerPath, 1f)
            if (com.pixelgram.desktop.core.PowerSaving.spoilerEffect) SpoilerShimmer(spoilerPath, textColor, Modifier.matchParentSize())
        }
        if (fading != null) BlurredCopy(annotated, inline, style, color, fadingPath, 1f - fade.value)
    }
}

@Composable
private fun SpoilerShimmer(path: Path, tint: Color, modifier: Modifier) {
    val transition = rememberInfiniteTransition()
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(ShimmerMs, easing = LinearEasing)),
    )
    Spacer(
        modifier.drawBehind {
            val bounds = path.getBounds()
            if (bounds.isEmpty) return@drawBehind
            val band = maxOf(bounds.width * ShimmerBand, ShimmerMinBand.toPx())
            val x = bounds.left - band + sweep * (bounds.width + band * 2)
            clipPath(path) {
                drawRect(
                    Brush.linearGradient(
                        listOf(Color.Transparent, tint.copy(alpha = ShimmerAlpha), Color.Transparent),
                        start = Offset(x - band, bounds.top),
                        end = Offset(x + band, bounds.bottom),
                    ),
                )
            }
        },
    )
}

@Composable
private fun BlurredCopy(
    annotated: AnnotatedString,
    inline: Map<String, androidx.compose.foundation.text.InlineTextContent>,
    style: TextStyle,
    color: Color,
    path: Path,
    strength: Float,
) {
    val shape = remember(path) {
        object : Shape {
            override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) = Outline.Generic(path)
        }
    }
    Text(
        annotated,
        style = style,
        color = color,
        inlineContent = inline,
        modifier = Modifier
            .graphicsLayer {
                clip = true
                this.shape = shape
                alpha = strength
            }
            .blur(SpoilerBlur * strength),
    )
}

private fun pillPath(layout: TextLayoutResult, start: Int, end: Int, density: Density): Path {
    val path = Path()
    val length = layout.layoutInput.text.length
    if (length == 0 || start >= end) return path
    val from = start.coerceIn(0, length)
    val to = end.coerceIn(0, length)
    with(density) {
        val firstLine = layout.getLineForOffset(from)
        val lastLine = layout.getLineForOffset(maxOf(from, to - 1))
        for (line in firstLine..lastLine) {
            val first = line == firstLine
            val last = line == lastLine
            val charStart = if (first) from else layout.getLineStart(line)
            var charEnd = if (last) to else layout.getLineEnd(line, visibleEnd = true)
            if (charEnd > charStart && layout.layoutInput.text[charEnd - 1] == '\n') charEnd -= 1
            if (charStart >= charEnd && firstLine != lastLine) continue
            val startBox = layout.getBoundingBox(charStart)
            val endBox = layout.getBoundingBox(maxOf(charStart, charEnd - 1))
            val left = if (first) startBox.left else layout.getLineLeft(line)
            val right = if (last) endBox.right else layout.getLineRight(line)
            val lineTop = layout.getLineTop(line)
            val lineBottom = layout.getLineBottom(line)
            val centerY = (lineTop + lineBottom) / 2f
            val half = (lineBottom - lineTop) / 2f - PillInset.toPx()
            val round = minOf(half, PillRadius.toPx())
            val startRadius = if (first) round else 2.dp.toPx()
            val endRadius = if (last) round else 2.dp.toPx()
            val pad = 3.dp.toPx()
            path.addRoundRect(
                RoundRect(
                    left = minOf(left, right) - pad,
                    top = centerY - half,
                    right = maxOf(left, right) + pad,
                    bottom = centerY + half,
                    topLeftCornerRadius = CornerRadius(startRadius),
                    bottomLeftCornerRadius = CornerRadius(startRadius),
                    topRightCornerRadius = CornerRadius(endRadius),
                    bottomRightCornerRadius = CornerRadius(endRadius),
                ),
            )
        }
    }
    return path
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWavyStrike(
    layout: TextLayoutResult,
    range: IntRange,
    color: Color,
    fontSize: Float,
) {
    val length = layout.layoutInput.text.length
    val from = range.first.coerceIn(0, length)
    val to = (range.last + 1).coerceIn(0, length)
    if (from >= to) return
    val amplitude = WaveAmplitude.toPx()
    val wavelength = WaveLength.toPx()
    for (line in layout.getLineForOffset(from)..layout.getLineForOffset(to - 1)) {
        val charStart = maxOf(from, layout.getLineStart(line))
        val charEnd = minOf(to, layout.getLineEnd(line, visibleEnd = true))
        if (charStart >= charEnd) continue
        val left = layout.getBoundingBox(charStart).left
        val right = layout.getBoundingBox(charEnd - 1).right
        val y = layout.getLineBaseline(line) - fontSize * StrikeHeight
        val path = Path().apply {
            moveTo(left, y)
            var x = left
            while (x <= right) {
                lineTo(x, y + amplitude * sin(2 * PI * (x - left) / wavelength).toFloat())
                x += 1f
            }
        }
        drawPath(path, color, style = Stroke(width = WaveStroke.toPx(), cap = StrokeCap.Round))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawQuote(layout: TextLayoutResult, range: IntRange, color: Color) {
    val length = layout.layoutInput.text.length
    val from = range.first.coerceIn(0, length)
    val to = (range.last + 1).coerceIn(0, length)
    if (from >= to) return
    val top = layout.getLineTop(layout.getLineForOffset(from)) + QuoteGap.toPx()
    val bottom = layout.getLineBottom(layout.getLineForOffset(to - 1))
    drawRoundRect(color.copy(alpha = 0.1f), topLeft = Offset(0f, top), size = Size(size.width, bottom - top), cornerRadius = CornerRadius(8.dp.toPx()))
    drawRoundRect(color, topLeft = Offset(0f, top), size = Size(3.dp.toPx(), bottom - top), cornerRadius = CornerRadius(2.dp.toPx()))
}

private val QuoteIndent = 12.sp
private const val ParagraphJoint = '\u200B'
private val PillRadius = 10.dp
private val PillInset = 1.dp
private val QuoteGap = 4.dp
private const val StrikeHeight = 0.3f
private val SpoilerBlur = 8.dp
private const val ShimmerMs = 1_800
private const val ShimmerBand = 0.35f
private const val ShimmerAlpha = 0.38f
private val ShimmerMinBand = 28.dp
private const val RevealMs = 600
private val WaveAmplitude = 1.6.dp
private val WaveLength = 7.dp
private val WaveStroke = 1.6.dp

fun endsWithQuote(text: String, entities: List<TdApi.TextEntity>): Boolean = entities.any {
    (it.type is TdApi.TextEntityTypeBlockQuote || it.type is TdApi.TextEntityTypeExpandableBlockQuote) &&
        it.offset + it.length >= text.trimEnd().length
}
