package com.pixelgram.desktop.ui.components

import com.pixelgram.desktop.ui.icons.rounded.Add
import com.pixelgram.desktop.ui.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialShapes
import com.pixelgram.desktop.ui.components.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.ui.theme.EmojiFonts
import kotlinx.coroutines.delay

private val PhoneShapes = listOf(
    MaterialShapes.Clover4Leaf,
    MaterialShapes.Arrow,
    MaterialShapes.Pill,
    MaterialShapes.SoftBurst,
    MaterialShapes.Diamond,
    MaterialShapes.ClamShell,
    MaterialShapes.Pentagon,
)

@Composable
fun PhoneText(
    phone: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
) {
    val hidden = EmojiFonts.settings?.hidePhone?.value == true
    var peek by remember { mutableStateOf(false) }
    LaunchedEffect(peek) {
        if (peek) {
            delay(PeekMs)
            peek = false
        }
    }
    val text = if (phone.startsWith("+")) phone else "+$phone"
    val tint = if (color != Color.Unspecified) color else style.color.takeIf { it != Color.Unspecified } ?: LocalContentColor.current
    androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
    AnimatedContent(
        targetState = hidden && !peek,
        transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.9f)) togetherWith fadeOut() },
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .then(if (hidden) Modifier.clickable { peek = !peek } else Modifier),
    ) { covered ->
        if (!covered) {
            Text(text, style = style, color = tint)
        } else {
            val full = with(LocalDensity.current) { (style.fontSize.takeIf { it.value > 0 } ?: LocalTextStyle.current.fontSize).toDp() }
            val digits = text.count(Char::isDigit).coerceAtLeast(1)
            BoxWithConstraints {
                val fit = if (maxWidth.value.isFinite()) maxWidth / ((digits + 1) * (1f + ShapeGap)) else full
                val size = minOf(full, fit)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(size * ShapeGap),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(full * 1.4f),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, tint = tint, modifier = Modifier.size(size * 1.1f))
                    repeat(digits) { index ->
                        Box(Modifier.size(size).background(tint, PhoneShapes[index % PhoneShapes.size].toShape()))
                    }
                }
            }
        }
    }
    }
}

private const val PeekMs = 4000L
private const val ShapeGap = 0.15f
