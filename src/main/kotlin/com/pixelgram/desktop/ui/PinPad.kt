package com.pixelgram.desktop.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.automirrored.outlined.Backspace
import com.pixelgram.desktop.ui.icons.automirrored.outlined.KeyboardTab
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.FastOutSlowInEasing
import kotlin.math.roundToInt
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import kotlinx.coroutines.delay

private val KeySize = 72.dp
private val KeyGap = 16.dp
private val DotSize = 16.dp
private val DotGap = 10.dp
private const val RoundPercent = 50
private const val PressedPercent = 28
private const val ShapeHoldMs = 700L
private const val MorphMs = 450
private const val SlotMs = 220
private const val GrowDamping = 0.6f
private const val GrowStiffness = 500f

private val DotShapes = listOf(
    MaterialShapes.Cookie4Sided,
    MaterialShapes.Clover4Leaf,
    MaterialShapes.Sunny,
    MaterialShapes.Pentagon,
    MaterialShapes.Gem,
    MaterialShapes.Cookie6Sided,
    MaterialShapes.Flower,
    MaterialShapes.Heart,
)

const val BackspaceKey = "back"
const val EnterKey = "enter"

@Composable
fun PinDots(count: Int, modifier: Modifier = Modifier) {
    val shapes = remember { mutableStateListOf<Int>() }
    val last = remember { intArrayOf(-1) }
    while (shapes.size > count) shapes.removeAt(shapes.lastIndex)
    while (shapes.size < count) {
        val avoid = setOf(shapes.lastOrNull(), last[0])
        val next = DotShapes.indices.filter { it !in avoid }.random()
        shapes.add(next)
        last[0] = next
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.height(DotSize * 1.6f),
    ) {
        shapes.forEachIndexed { index, shape -> key(index, shape) { PinDot(index, shape) } }
    }
}

@Composable
private fun PinDot(index: Int, shape: Int) {
    val color = MaterialTheme.colorScheme.onSurface
    val morph = remember(shape) { Morph(DotShapes[shape], MaterialShapes.Circle) }
    val progress = remember { Animatable(0f) }
    val grow = remember { Animatable(0f) }
    val slot = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        grow.animateTo(1f, spring(dampingRatio = GrowDamping, stiffness = GrowStiffness))
    }
    LaunchedEffect(Unit) {
        slot.animateTo(1f, tween(SlotMs, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        delay(ShapeHoldMs)
        progress.animateTo(1f, tween(MorphMs))
    }
    val dot = DotSize * 1.4f
    val gap = if (index == 0) 0.dp else DotGap
    Canvas(
        Modifier
            .layout { measurable, constraints ->
                val full = (dot + gap).roundToPx()
                val width = (full * slot.value).roundToInt()
                val placeable = measurable.measure(Constraints.fixed(dot.roundToPx(), dot.roundToPx()))
                layout(width, placeable.height) { placeable.place(width - placeable.width, 0) }
            }
            .graphicsLayer { alpha = grow.value.coerceIn(0f, 1f) },
    ) {
        val path = Path()
        var first = true
        morph.forEachCubic(progress.value) { cubic ->
            if (first) {
                path.moveTo(cubic.anchor0X, cubic.anchor0Y)
                first = false
            }
            path.cubicTo(cubic.control0X, cubic.control0Y, cubic.control1X, cubic.control1Y, cubic.anchor1X, cubic.anchor1Y)
        }
        path.close()
        val side = size.minDimension * grow.value
        val offset = (size.minDimension - side) / 2f
        translateAndScale(offset, side) { drawPath(path, color) }
    }
}

private inline fun androidx.compose.ui.graphics.drawscope.DrawScope.translateAndScale(
    offset: Float,
    side: Float,
    block: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit,
) {
    drawContext.transform.translate(offset, offset)
    scale(side, side, pivot = androidx.compose.ui.geometry.Offset.Zero) { block() }
    drawContext.transform.translate(-offset, -offset)
}

@Composable
fun PinPad(onKey: (String) -> Unit, modifier: Modifier = Modifier) {
    val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf(BackspaceKey, "0", EnterKey))
    Column(verticalArrangement = Arrangement.spacedBy(KeyGap), modifier = modifier) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(KeyGap)) {
                row.forEach { key -> PinKey(key, onKey) }
            }
        }
    }
}

@Composable
private fun PinKey(key: String, onKey: (String) -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val percent by animateIntAsState(
        if (pressed) PressedPercent else RoundPercent,
        spring(dampingRatio = 0.6f, stiffness = 900f),
    )
    val scheme = MaterialTheme.colorScheme
    val action = key == BackspaceKey || key == EnterKey
    val container = if (action) scheme.primaryContainer else scheme.surfaceContainerHighest
    val content = if (action) scheme.onPrimaryContainer else scheme.onSurface
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(KeySize)
            .clip(RoundedCornerShape(percent))
            .background(if (pressed) container.blend(content) else container)
            .clickable(interactionSource = interaction, indication = null) { onKey(key) },
    ) {
        when (key) {
            BackspaceKey -> Icon(Icons.AutoMirrored.Outlined.Backspace, contentDescription = "Delete", tint = content)
            EnterKey -> Icon(Icons.AutoMirrored.Outlined.KeyboardTab, contentDescription = "Unlock", tint = content)
            else -> Text(key, style = MaterialTheme.typography.headlineSmall, color = content)
        }
    }
}

private fun Color.blend(over: Color): Color = Color(
    red = red * 0.88f + over.red * 0.12f,
    green = green * 0.88f + over.green * 0.12f,
    blue = blue * 0.88f + over.blue * 0.12f,
    alpha = alpha,
)
