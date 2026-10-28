package com.pixelgram.desktop.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.takeOrElse
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
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
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
    val enabled: Boolean = true,
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
    val scope = rememberCoroutineScope()
    Column(Modifier.width(width).padding(MenuShadow), verticalArrangement = Arrangement.spacedBy(GroupGap)) {
        if (leading != null) {
            Surface(
                shape = groupShapes(0, 1).shape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = MenuShadow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(content = leading)
            }
        }
        shown.forEach { entries ->
            Column(verticalArrangement = Arrangement.spacedBy(ItemGap)) {
                entries.forEachIndexed { index, entry ->
                    val base = if (entry.color == Color.Unspecified) MaterialTheme.colorScheme.onSurfaceVariant else entry.color
                    val tint = if (entry.enabled) base else base.copy(alpha = DisabledAlpha)
                    val shape = groupShapes(index, entries.size).shape
                    Surface(
                        onClick = { scope.launch { entry.onClick() } },
                        enabled = entry.enabled,
                        shape = shape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shadowElevation = MenuShadow,
                        modifier = Modifier.fillMaxWidth().height(ItemHeight).focusProperties { canFocus = false },
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(ItemPadding),
                            modifier = Modifier.padding(horizontal = ItemPadding),
                        ) {
                            entry.icon?.let { icon -> Icon(icon, contentDescription = null, tint = tint) }
                            Text(
                                entry.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (entry.enabled) entry.color.takeOrElse { MaterialTheme.colorScheme.onSurface } else MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledAlpha),
                                maxLines = 1,
                                modifier = Modifier.weight(1f),
                            )
                            entry.trailing?.let { icon -> Icon(icon, contentDescription = null, tint = tint) }
                        }
                    }
                }
            }
        }
    }
}

private val GroupGap = 6.dp
private val ItemGap = 2.dp
private val ItemHeight = 40.dp
private val ItemPadding = 12.dp
private val MenuShadow = 2.dp
private val MenuWidth = 240.dp

private const val DisabledAlpha = 0.38f
