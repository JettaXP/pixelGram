package com.pixelgram.desktop.ui.components.expressive

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    androidx.compose.material3.Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        interactionSource = interaction,
        thumb = { SliderThumb(interaction, enabled = enabled) },
    )
}

@Composable
fun SliderThumb(interaction: MutableInteractionSource, height: Dp = ThumbHeight, enabled: Boolean = true) {
    val pressed by interaction.collectIsPressedAsState()
    val dragged by interaction.collectIsDraggedAsState()
    val held = pressed || dragged
    val width by animateDpAsState(
        if (held) ThumbPressedWidth else ThumbWidth,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
    )
    val tall by animateDpAsState(
        if (held) height + ThumbGrow else height,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
    )
    val colors = MaterialTheme.colorScheme
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(ThumbWidth, height)
            .hoverable(interaction),
    ) {
        Box(
            Modifier
                .requiredSize(width, tall)
                .background(if (enabled) colors.primary else colors.onSurface.copy(alpha = DisabledAlpha), CircleShape),
        )
    }
}

private val ThumbWidth = 4.dp
private val ThumbPressedWidth = 2.dp
private val ThumbHeight = 44.dp
private val ThumbGrow = 4.dp
private const val DisabledAlpha = 0.38f
