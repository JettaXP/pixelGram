package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.components.expressive.BasicAlertDialog
import com.pixelgram.desktop.ui.components.expressive.Button
import com.pixelgram.desktop.ui.components.expressive.TextButton
import kotlin.math.roundToInt

private class Hsv(val hue: Float, val saturation: Float, val value: Float) {
    val color: Color get() = Color.hsv(hue, saturation, value)
    val lightness: Float get() = value * (1 - saturation / 2)
    val hslSaturation: Float get() = lightness.let { l -> if (l <= 0f || l >= 1f) 0f else (value - l) / minOf(l, 1 - l) }

    companion object {
        fun of(color: Color): Hsv {
            val r = color.red
            val g = color.green
            val b = color.blue
            val max = maxOf(r, g, b)
            val min = minOf(r, g, b)
            val delta = max - min
            val hue = when {
                delta == 0f -> 0f
                max == r -> 60f * (((g - b) / delta) % 6f)
                max == g -> 60f * ((b - r) / delta + 2f)
                else -> 60f * ((r - g) / delta + 4f)
            }.let { if (it < 0f) it + 360f else it }
            return Hsv(hue, if (max == 0f) 0f else delta / max, max)
        }

        fun ofHsl(hue: Float, saturation: Float, lightness: Float): Hsv {
            val value = lightness + saturation * minOf(lightness, 1 - lightness)
            return Hsv(hue, if (value == 0f) 0f else 2 * (1 - lightness / value), value)
        }
    }
}

private val Presets = listOf(
    0xFFFFFFFF, 0xFF000000, 0xFFF44336, 0xFFFF9800, 0xFFFFEB3B, 0xFF4CAF50, 0xFF00BCD4, 0xFF2196F3, 0xFF9C27B0, 0xFFE91E63,
).map { Color(it) }

@Composable
internal fun ColorPickerDialog(initial: Color, onDismiss: () -> Unit, onPick: (Color) -> Unit) {
    var hsv by remember { mutableStateOf(Hsv.of(initial)) }
    val color = hsv.color
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.width(DialogWidth),
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Color", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                    Box(Modifier.size(width = 28.dp, height = 28.dp).clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp)).background(initial))
                    Box(Modifier.size(width = 28.dp, height = 28.dp).clip(RoundedCornerShape(topEnd = 14.dp, bottomEnd = 14.dp)).background(color))
                }
                SaturationValueSquare(hsv) { hsv = it }
                GradientSlider(
                    value = hsv.hue / 360f,
                    brush = Brush.horizontalGradient((0..6).map { Color.hsv(it * 60f % 360f, 1f, 1f) }),
                    thumb = Color.hsv(hsv.hue, 1f, 1f),
                ) { hsv = Hsv(it * 360f, hsv.saturation, hsv.value) }
                GradientSlider(
                    value = hsv.lightness,
                    brush = Brush.horizontalGradient(listOf(Color.Black, Color.hsl(hsv.hue, hsv.hslSaturation.coerceIn(0f, 1f), 0.5f), Color.White)),
                    thumb = color,
                ) { hsv = Hsv.ofHsl(hsv.hue, hsv.hslSaturation.coerceIn(0f, 1f), it.coerceIn(0f, 1f)) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField("H", hsv.hue.roundToInt(), 360, Modifier.weight(1f)) { hsv = Hsv(it.toFloat() % 360f, hsv.saturation, hsv.value) }
                    NumberField("S", (hsv.hslSaturation * 100).roundToInt(), 100, Modifier.weight(1f)) {
                        hsv = Hsv.ofHsl(hsv.hue, it / 100f, hsv.lightness)
                    }
                    NumberField("L", (hsv.lightness * 100).roundToInt(), 100, Modifier.weight(1f)) {
                        hsv = Hsv.ofHsl(hsv.hue, hsv.hslSaturation.coerceIn(0f, 1f), it / 100f)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val argb = color.toArgb()
                    val red = (argb shr 16) and 0xFF
                    val green = (argb shr 8) and 0xFF
                    val blue = argb and 0xFF
                    NumberField("R", red, 255, Modifier.weight(1f)) { hsv = Hsv.of(Color(it, green, blue)) }
                    NumberField("G", green, 255, Modifier.weight(1f)) { hsv = Hsv.of(Color(red, it, blue)) }
                    NumberField("B", blue, 255, Modifier.weight(1f)) { hsv = Hsv.of(Color(red, green, it)) }
                }
                val hex = "%06X".format(color.toArgb() and 0xFFFFFF)
                CompactField("#", hex, Modifier.fillMaxWidth()) { typed ->
                    typed.removePrefix("#").takeIf { it.length == 6 }?.toIntOrNull(16)?.let { hsv = Hsv.of(Color(0xFF000000.toInt() or it)) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    Presets.forEach { preset ->
                        Box(
                            Modifier
                                .weight(1f)
                                .height(24.dp)
                                .clip(CircleShape)
                                .background(preset)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                .pointerInput(preset) {
                                    awaitEachGesture {
                                        awaitFirstDown()
                                        hsv = Hsv.of(preset)
                                    }
                                },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss, modifier = Modifier.focusProperties { canFocus = false }) { Text("Cancel") }
                    Button(onClick = { onPick(color) }, modifier = Modifier.focusProperties { canFocus = false }) { Text("Done") }
                }
            }
        }
    }
}

@Composable
private fun SaturationValueSquare(hsv: Hsv, onChange: (Hsv) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth().height(SquareHeight).clip(RoundedCornerShape(16.dp))) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val pick = { at: Offset ->
            onChange(Hsv(hsv.hue, (at.x / width).coerceIn(0f, 1f), 1f - (at.y / height).coerceIn(0f, 1f)))
        }
        Canvas(
            Modifier.matchParentSize().pointerInput(hsv.hue, width, height) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    pick(down.position)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        pick(change.position)
                        change.consume()
                    }
                }
            },
        ) {
            drawRect(Brush.horizontalGradient(listOf(Color.White, Color.hsv(hsv.hue, 1f, 1f))))
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        }
        val density = LocalDensity.current
        val half = with(density) { (ThumbSize / 2).roundToPx() }
        Box(
            Modifier
                .offset { IntOffset((hsv.saturation * width).roundToInt() - half, ((1 - hsv.value) * height).roundToInt() - half) }
                .size(ThumbSize)
                .shadow(2.dp, CircleShape)
                .border(3.dp, Color.White, CircleShape)
                .background(hsv.color, CircleShape),
        )
    }
}

@Composable
private fun GradientSlider(value: Float, brush: Brush, thumb: Color, onChange: (Float) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth().height(ThumbSize), contentAlignment = Alignment.CenterStart) {
        val width = constraints.maxWidth.toFloat()
        val density = LocalDensity.current
        val half = with(density) { (ThumbSize / 2).toPx() }
        val pick = { x: Float -> onChange(((x - half) / (width - half * 2)).coerceIn(0f, 1f)) }
        Canvas(
            Modifier.matchParentSize().pointerInput(width) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    pick(down.position.x)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        pick(change.position.x)
                        change.consume()
                    }
                }
            },
        ) {
            val track = TrackHeight.toPx()
            drawRoundRect(
                brush,
                topLeft = Offset(0f, (size.height - track) / 2),
                size = androidx.compose.ui.geometry.Size(size.width, track),
                cornerRadius = CornerRadius(track / 2),
            )
        }
        Box(
            Modifier
                .offset { IntOffset((half + value * (width - half * 2) - half).roundToInt(), 0) }
                .size(ThumbSize)
                .shadow(2.dp, CircleShape)
                .border(3.dp, Color.White, CircleShape)
                .background(thumb, CircleShape),
        )
    }
}

@Composable
private fun NumberField(label: String, value: Int, max: Int, modifier: Modifier, onValue: (Int) -> Unit) {
    CompactField(label, value.toString(), modifier) { typed -> typed.toIntOrNull()?.takeIf { it in 0..max }?.let(onValue) }
}

@Composable
private fun CompactField(label: String, value: String, modifier: Modifier, onValue: (String) -> Unit) {
    var text by remember(value) { mutableStateOf(value) }
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest, modifier = modifier.height(FieldHeight)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = text,
                onValueChange = {
                    text = it
                    onValue(it.trim())
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface, fontFamily = FontFamily.Monospace),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions.Default,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private val DialogWidth = 380.dp
private val SquareHeight = 180.dp
private val ThumbSize = 24.dp
private val TrackHeight = 14.dp
private val FieldHeight = 44.dp
