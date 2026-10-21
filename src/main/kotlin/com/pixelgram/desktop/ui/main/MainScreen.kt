package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.IconButton
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailState
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.rememberCoroutineScope
import com.pixelgram.desktop.ui.Screen
import com.pixelgram.desktop.ui.components.Avatar
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.telegram.ChatListKey
import com.pixelgram.desktop.telegram.Telegram

private data class FolderTab(val key: ChatListKey, val title: String)

@Composable
fun MainScreen(
    telegram: Telegram,
    settings: AppSettings,
    onNavigate: (Screen) -> Unit,
) {
    val tabs = buildList {
        if (!settings.hideAllChats.value || telegram.folders.isEmpty()) {
            add(FolderTab(ChatListKey.Main, "All chats"))
        }
        telegram.folders.forEach { add(FolderTab(ChatListKey.Folder(it.id), it.title)) }
    }
    var selectedKey by remember { mutableStateOf<ChatListKey>(ChatListKey.Main) }
    if (selectedKey != ChatListKey.Archive && tabs.none { it.key == selectedKey } && tabs.isNotEmpty()) {
        selectedKey = tabs.first().key
    }
    var openChatId by remember { mutableStateOf<Long?>(null) }
    val railState = rememberWideNavigationRailState()
    val scope = rememberCoroutineScope()
    val menuOpen = railState.targetValue == WideNavigationRailValue.Expanded
    val toggleMenu: () -> Unit = { scope.launch { railState.toggle() } }
    val closeMenuAnd: (() -> Unit) -> Unit = { action ->
        scope.launch { railState.collapse() }
        action()
    }

    LaunchedEffect(selectedKey) { telegram.loadChats(selectedKey) }

    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(Modifier.fillMaxSize()) {
            // The rail is the folders bar; the menu button expands it into the app menu.
            if (settings.materialFolders.value || menuOpen) {
                PixelRail(
                    state = railState,
                    expanded = menuOpen,
                    telegram = telegram,
                    settings = settings,
                    tabs = tabs,
                    selected = selectedKey,
                    onToggleMenu = toggleMenu,
                    onSelect = { key -> closeMenuAnd { selectedKey = key } },
                    onNavigate = { screen -> closeMenuAnd { onNavigate(screen) } },
                    onOpenSaved = {
                        closeMenuAnd { telegram.openSavedMessages { openChatId = it } }
                    },
                )
            }
            Column(
                Modifier
                    .width(360.dp)
                    .fillMaxHeight()
                    .padding(vertical = 8.dp),
            ) {
                if (!settings.materialFolders.value && selectedKey != ChatListKey.Archive) {
                    FoldersTabs(tabs, selectedKey, onSelect = { selectedKey = it })
                }
                ChatList(
                    title = if (selectedKey == ChatListKey.Archive) "Archived chats" else "pixelGram",
                    chats = telegram.chatList(selectedKey),
                    selectedChatId = openChatId,
                    telegram = telegram,
                    settings = settings,
                    onOpen = { openChatId = it },
                    onMenu = if (settings.materialFolders.value) null else toggleMenu,
                    onBack = if (selectedKey == ChatListKey.Archive) {
                        { selectedKey = tabs.firstOrNull()?.key ?: ChatListKey.Main }
                    } else {
                        null
                    },
                )
            }
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(top = 8.dp, end = 8.dp, bottom = 8.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
            ) {
                val chatId = openChatId
                val chat = chatId?.let { telegram.chats[it] }
                if (chat != null) {
                    ChatView(chat, telegram, settings)
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "Select a chat to start messaging",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

// Material 3 Expressive navigation rail: folders when collapsed, the app
// menu (profile, saved, archive, preferences, settings, memory) when expanded.
@Composable
private fun PixelRail(
    state: WideNavigationRailState,
    expanded: Boolean,
    telegram: Telegram,
    settings: AppSettings,
    tabs: List<FolderTab>,
    selected: ChatListKey,
    onToggleMenu: () -> Unit,
    onSelect: (ChatListKey) -> Unit,
    onNavigate: (Screen) -> Unit,
    onOpenSaved: () -> Unit,
) {
    WideNavigationRail(
        state = state,
        colors = WideNavigationRailDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        header = {
            IconButton(onClick = onToggleMenu, modifier = Modifier.padding(start = 24.dp)) {
                Icon(
                    if (expanded) Icons.AutoMirrored.Filled.MenuOpen else Icons.Filled.Menu,
                    contentDescription = "Menu",
                )
            }
        },
    ) {
        if (expanded) {
            MenuHeader(telegram, settings)
            val items = listOf(
                Triple(Icons.Outlined.AccountCircle, "My Profile") { onNavigate(Screen.Profile) },
                Triple(Icons.Outlined.Bookmark, "Saved Messages", onOpenSaved),
                Triple(Icons.Outlined.Archive, "Archived chats") { onSelect(ChatListKey.Archive) },
                Triple(Icons.Outlined.AutoAwesome, "pixelGram Preferences") {
                    onNavigate(Screen.Settings(preferences = true))
                },
                Triple(Icons.Outlined.Settings, "Settings") { onNavigate(Screen.Settings()) },
                Triple(Icons.Outlined.Storage, "Memory") { onNavigate(Screen.Memory) },
            )
            items.forEach { (icon, label, action) ->
                WideNavigationRailItem(
                    selected = icon == Icons.Outlined.Archive && selected == ChatListKey.Archive,
                    onClick = action,
                    icon = { Icon(icon, contentDescription = null) },
                    label = { Text(label) },
                    railExpanded = true,
                )
            }
        } else {
            tabs.forEach { tab ->
                val isSelected = tab.key == selected
                val isAll = tab.key == ChatListKey.Main
                WideNavigationRailItem(
                    selected = isSelected,
                    onClick = { onSelect(tab.key) },
                    icon = {
                        Icon(
                            imageVector = when {
                                isAll && isSelected -> Icons.Filled.Forum
                                isAll -> Icons.Outlined.Forum
                                isSelected -> Icons.Filled.Folder
                                else -> Icons.Outlined.Folder
                            },
                            contentDescription = tab.title,
                        )
                    },
                    label = { Text(tab.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    railExpanded = false,
                )
            }
        }
    }
}

@Composable
private fun MenuHeader(telegram: Telegram, settings: AppSettings) {
    val me = telegram.me ?: return
    val name = listOf(me.firstName, me.lastName).filter { !it.isNullOrBlank() }.joinToString(" ")
    Row(
        modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Avatar(name, me.profilePhoto?.small, telegram, settings, size = 48.dp)
        Column {
            Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            me.phoneNumber?.takeIf { it.isNotEmpty() }?.let {
                Text(
                    "+$it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FoldersTabs(
    tabs: List<FolderTab>,
    selected: ChatListKey,
    onSelect: (ChatListKey) -> Unit,
) {
    if (tabs.size < 2) return
    val index = tabs.indexOfFirst { it.key == selected }.coerceAtLeast(0)
    PrimaryScrollableTabRow(
        selectedTabIndex = index,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        edgePadding = 8.dp,
    ) {
        tabs.forEachIndexed { i, tab ->
            Tab(
                selected = i == index,
                onClick = { onSelect(tab.key) },
                text = { Text(tab.title, maxLines = 1) },
            )
        }
    }
}
