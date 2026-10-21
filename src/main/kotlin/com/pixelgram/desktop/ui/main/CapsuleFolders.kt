package com.pixelgram.desktop.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MarkChatUnread
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MarkChatUnread
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.telegram.ChatListKey

// Experimental: folders in one Material 3 Expressive floating toolbar. Only
// the selected folder shows its name, the others shrink to their icon.
@Composable
internal fun CapsuleFolders(
    tabs: List<FolderTab>,
    selected: ChatListKey,
    settings: AppSettings,
    onSelect: (ChatListKey) -> Unit,
) {
    if (tabs.size < 2) return
    val height = settings.capsuleHeight.value.dp
    val enter = MaterialTheme.motionScheme.defaultSpatialSpec<IntSize>()
    val exit = MaterialTheme.motionScheme.fastSpatialSpec<IntSize>()
    Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
        HorizontalFloatingToolbar(
            expanded = true,
            colors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            expandedShadowElevation = 0.dp,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(settings.capsuleSpacing.value.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                tabs.forEach { tab ->
                    val isSelected = tab.key == selected
                    ToggleButton(
                        checked = isSelected,
                        onCheckedChange = { onSelect(tab.key) },
                        shapes = ToggleButtonShapes(CircleShape, CircleShape, CircleShape),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.height(height),
                    ) {
                        Icon(folderIcon(tab.icon, isSelected), contentDescription = tab.title)
                        AnimatedVisibility(
                            visible = isSelected,
                            enter = fadeIn() + expandHorizontally(enter),
                            exit = fadeOut() + shrinkHorizontally(exit),
                        ) {
                            Text(tab.title, maxLines = 1, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

// Telegram folder icon names (chatFolderIcon) mapped to Material icons.
internal fun folderIcon(name: String, filled: Boolean): ImageVector = when (name) {
    "All" -> if (filled) Icons.Filled.Forum else Icons.Outlined.Forum
    "Unread" -> if (filled) Icons.Filled.MarkChatUnread else Icons.Outlined.MarkChatUnread
    "Private" -> if (filled) Icons.Filled.Person else Icons.Outlined.Person
    "Groups" -> if (filled) Icons.Filled.Groups else Icons.Outlined.Groups
    "Channels" -> if (filled) Icons.Filled.Campaign else Icons.Outlined.Campaign
    "Bots" -> if (filled) Icons.Filled.SmartToy else Icons.Outlined.SmartToy
    "Favorite" -> if (filled) Icons.Filled.Star else Icons.Outlined.StarOutline
    "Home" -> if (filled) Icons.Filled.Home else Icons.Outlined.Home
    "Work" -> if (filled) Icons.Filled.Work else Icons.Outlined.Work
    else -> if (filled) Icons.Filled.Folder else Icons.Outlined.Folder
}
