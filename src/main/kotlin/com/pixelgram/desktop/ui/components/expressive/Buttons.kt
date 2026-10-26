package com.pixelgram.desktop.ui.components.expressive

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Shape

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape? = null,
    shapes: ButtonShapes? = null,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) = androidx.compose.material3.Button(
    onClick = onClick,
    shapes = shapes ?: shape?.let { ButtonDefaults.shapes(it, ButtonDefaults.pressedShape) } ?: ButtonDefaults.shapes(),
    modifier = modifier,
    enabled = enabled,
    colors = colors,
    elevation = elevation,
    border = border,
    contentPadding = contentPadding,
    interactionSource = interactionSource,
    content = content,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FilledTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape? = null,
    shapes: ButtonShapes? = null,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    elevation: ButtonElevation? = ButtonDefaults.filledTonalButtonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) = androidx.compose.material3.FilledTonalButton(
    onClick = onClick,
    shapes = shapes ?: shape?.let { ButtonDefaults.shapes(it, ButtonDefaults.pressedShape) } ?: ButtonDefaults.shapes(),
    modifier = modifier,
    enabled = enabled,
    colors = colors,
    elevation = elevation,
    border = border,
    contentPadding = contentPadding,
    interactionSource = interactionSource,
    content = content,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape? = null,
    shapes: ButtonShapes? = null,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) = androidx.compose.material3.OutlinedButton(
    onClick = onClick,
    shapes = shapes ?: shape?.let { ButtonDefaults.shapes(it, ButtonDefaults.pressedShape) } ?: ButtonDefaults.shapes(),
    modifier = modifier,
    enabled = enabled,
    colors = colors,
    elevation = elevation,
    border = border,
    contentPadding = contentPadding,
    interactionSource = interactionSource,
    content = content,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape? = null,
    shapes: ButtonShapes? = null,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) = androidx.compose.material3.TextButton(
    onClick = onClick,
    shapes = shapes ?: shape?.let { ButtonDefaults.shapes(it, ButtonDefaults.pressedShape) } ?: ButtonDefaults.shapes(),
    modifier = modifier,
    enabled = enabled,
    colors = colors,
    elevation = elevation,
    border = border,
    contentPadding = contentPadding,
    interactionSource = interactionSource,
    content = content,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape? = null,
    shapes: IconButtonShapes? = null,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) = androidx.compose.material3.IconButton(
    onClick = onClick,
    shapes = shapes ?: shape?.let { IconButtonDefaults.shapes(it, IconButtonDefaults.mediumPressedShape) } ?: IconButtonDefaults.shapes(),
    modifier = modifier.focusProperties { canFocus = false },
    enabled = enabled,
    colors = colors,
    interactionSource = interactionSource,
    content = content,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FilledIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape? = null,
    shapes: IconButtonShapes? = null,
    colors: IconButtonColors = IconButtonDefaults.filledIconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) = androidx.compose.material3.FilledIconButton(
    onClick = onClick,
    shapes = shapes ?: shape?.let { IconButtonDefaults.shapes(it, IconButtonDefaults.mediumPressedShape) } ?: IconButtonDefaults.shapes(),
    modifier = modifier.focusProperties { canFocus = false },
    enabled = enabled,
    colors = colors,
    interactionSource = interactionSource,
    content = content,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FilledTonalIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape? = null,
    shapes: IconButtonShapes? = null,
    colors: IconButtonColors = IconButtonDefaults.filledTonalIconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) = androidx.compose.material3.FilledTonalIconButton(
    onClick = onClick,
    shapes = shapes ?: shape?.let { IconButtonDefaults.shapes(it, IconButtonDefaults.mediumPressedShape) } ?: IconButtonDefaults.shapes(),
    modifier = modifier.focusProperties { canFocus = false },
    enabled = enabled,
    colors = colors,
    interactionSource = interactionSource,
    content = content,
)

@Composable
fun IconToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) = androidx.compose.material3.IconToggleButton(
    checked = checked,
    onCheckedChange = onCheckedChange,
    modifier = modifier.focusProperties { canFocus = false },
    enabled = enabled,
    content = content,
)
