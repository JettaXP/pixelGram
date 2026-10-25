package com.pixelgram.desktop.ui.components

import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
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
    val offset = if (leading != null) 1 else 0
    val total = shown.size + offset
    if (leading != null) {
        DropdownMenuGroup(shapes = MenuDefaults.groupShape(0, total), modifier = Modifier.width(width), content = leading)
    }
    shown.forEachIndexed { groupIndex, entries ->
        if (groupIndex + offset > 0) Spacer(Modifier.height(GroupGap))
        DropdownMenuGroup(
            shapes = MenuDefaults.groupShape(groupIndex + offset, total),
            modifier = Modifier.width(width),
            containerColor = Color.Transparent,
            shadowElevation = 0.dp,
            tonalElevation = 0.dp,
            contentPadding = PaddingValues(0.dp),
        ) {
            entries.forEachIndexed { index, entry ->
                if (index > 0) Spacer(Modifier.height(ItemGap))
                val tint = if (entry.color == Color.Unspecified) MaterialTheme.colorScheme.onSurfaceVariant else entry.color
                Surface(
                    shape = groupShapes(index, entries.size).shape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = ItemShadow,
                ) {
                    DropdownMenuItem(
                        onClick = entry.onClick,
                        text = { Text(entry.label, color = entry.color) },
                        shape = groupShapes(index, entries.size).shape,
                        leadingIcon = entry.icon?.let { icon -> { Icon(icon, contentDescription = null, tint = tint) } },
                        trailingIcon = entry.trailing?.let { icon -> { Icon(icon, contentDescription = null, tint = tint) } },
                    )
                }
            }
        }
    }
}

private val ItemGap = 2.dp
private val GroupGap = 10.dp
private val ItemShadow = 2.dp
private val MenuWidth = 260.dp
