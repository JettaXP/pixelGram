package com.pixelgram.desktop.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import com.pixelgram.desktop.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

class MenuEntry(
    val icon: ImageVector?,
    val label: String,
    val color: Color = Color.Unspecified,
    val trailing: ImageVector? = null,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GroupedMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    offset: DpOffset = DpOffset.Zero,
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenuPopup(expanded = expanded, onDismissRequest = onDismiss, offset = offset, content = content)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MenuGroups(
    groups: List<List<MenuEntry>>,
    width: androidx.compose.ui.unit.Dp = MenuWidth,
    leading: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val shown = groups.filter { it.isNotEmpty() }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = MenuShadow,
        modifier = Modifier.width(width),
    ) {
        Column(Modifier.padding(MenuPadding)) {
            if (leading != null) {
                Surface(shape = groupShapes(0, 1).shape, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
                    Column(content = leading)
                }
            }
            shown.forEachIndexed { groupIndex, entries ->
                if (groupIndex > 0 || leading != null) Spacer(Modifier.height(GroupGap))
                entries.forEachIndexed { index, entry ->
                    if (index > 0) Spacer(Modifier.height(ItemGap))
                    val tint = if (entry.color == Color.Unspecified) MaterialTheme.colorScheme.onSurfaceVariant else entry.color
                    val shape = groupShapes(index, entries.size).shape
                    Surface(shape = shape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        DropdownMenuItem(
                            onClick = entry.onClick,
                            text = { Text(entry.label, color = entry.color) },
                            shape = shape,
                            leadingIcon = entry.icon?.let { icon -> { Icon(icon, contentDescription = null, tint = tint) } },
                            trailingIcon = entry.trailing?.let { icon -> { Icon(icon, contentDescription = null, tint = tint) } },
                        )
                    }
                }
            }
        }
    }
}

private val ItemGap = 2.dp
private val GroupGap = 8.dp
private val MenuShadow = 6.dp
private val MenuPadding = 4.dp
private val MenuWidth = 260.dp
