package com.pixelgram.desktop.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AppSettings

class CornerSpec(val lists: Dp, val cards: Dp, val dialogs: Dp, val menus: Dp) {
    val inner: Dp get() = (lists / 5).coerceAtMost(InnerMax)
}

val LocalCorners = compositionLocalOf { CornerSpec(20.dp, 24.dp, 28.dp, 16.dp) }

fun cornerSpec(settings: AppSettings) = CornerSpec(
    settings.cornerLists.value.dp,
    settings.cornerCards.value.dp,
    settings.cornerDialogs.value.dp,
    settings.cornerMenus.value.dp,
)

fun cornerShapes(spec: CornerSpec) = Shapes(
    extraSmall = RoundedCornerShape(spec.menus),
    small = RoundedCornerShape(spec.menus / 2),
    medium = RoundedCornerShape(spec.cards * 2 / 3),
    large = RoundedCornerShape(spec.cards),
    extraLarge = RoundedCornerShape(spec.dialogs),
)

@Composable
fun groupShapes(index: Int, count: Int): ListItemShapes {
    val spec = LocalCorners.current
    val top = if (index == 0) spec.lists else spec.inner
    val bottom = if (index == count - 1) spec.lists else spec.inner
    val shape = RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
    val round = RoundedCornerShape(spec.lists)
    return ListItemShapes(
        shape = shape,
        selectedShape = round,
        pressedShape = round,
        focusedShape = round,
        hoveredShape = RoundedCornerShape(
            topStart = top + HoverGrow, topEnd = top + HoverGrow,
            bottomStart = bottom + HoverGrow, bottomEnd = bottom + HoverGrow,
        ),
        draggedShape = round,
    )
}

private val InnerMax = 6.dp
private val HoverGrow = 2.dp
