package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.draw.clip
import com.pixelgram.desktop.core.ThemeMode
import com.pixelgram.desktop.telegram.SavedMessagesTitle
import com.pixelgram.desktop.ui.settings.PixelSwitch
import com.pixelgram.desktop.ui.theme.isDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Bookmark
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
import com.pixelgram.desktop.telegram.fullName

internal data class FolderTab(val key: ChatListKey, val title: String, val icon: String = "")

@Composable
fun MainScreen(
    telegram: Telegram,
    settings: AppSettings,
    openChatId: Long?,
    onOpenChat: (Long) -> Unit,
    onNavigate: (Screen) -> Unit,
) {
    val tabs = buildList {
        if (!settings.hideAllChats.value || telegram.folders.isEmpty()) {
            add(FolderTab(ChatListKey.Main, "All chats", "All"))
        }
        telegram.folders.forEach { add(FolderTab(ChatListKey.Folder(it.id), it.title, it.icon)) }
    }
    var selectedKey by remember { mutableStateOf<ChatListKey>(ChatListKey.Main) }
    if (selectedKey != ChatListKey.Archive && tabs.none { it.key == selectedKey } && tabs.isNotEmpty()) {
        selectedKey = tabs.first().key
    }
    var newChat by remember { mutableStateOf<Boolean?>(null) }
    val railState = rememberWideNavigationRailState()
    val scope = rememberCoroutineScope()
    val menuOpen = railState.targetValue == WideNavigationRailValue.Expanded
    val toggleMenu: () -> Unit = { scope.launch { railState.toggle() } }
    val closeMenuAnd: (() -> Unit) -> Unit = { action ->
        scope.launch { railState.collapse() }
        action()
    }

    LaunchedEffect(selectedKey) { telegram.loadChats(selectedKey) }
    LaunchedEffect(Unit) { telegram.loadChats(ChatListKey.Archive) }

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
                        closeMenuAnd { telegram.openSavedMessages(onOpenChat) }
                    },
                    onNewChat = { channel -> closeMenuAnd { newChat = channel } },
                )
            }
            Column(
                Modifier
                    .width(360.dp)
                    .fillMaxHeight()
                    .padding(vertical = 8.dp),
            ) {
                if (!settings.materialFolders.value && selectedKey != ChatListKey.Archive) {
                    if (settings.capsuleFolders.value) {
                        CapsuleFolders(tabs, selectedKey, settings, onSelect = { selectedKey = it })
                    } else {
                        FoldersTabs(tabs, selectedKey, onSelect = { selectedKey = it })
                    }
                }
                ChatList(
                    listKey = selectedKey,
                    title = if (selectedKey == ChatListKey.Archive) "Archived chats" else "pixelGram",
                    chats = telegram.chatList(selectedKey),
                    selectedChatId = openChatId,
                    telegram = telegram,
                    settings = settings,
                    onOpen = onOpenChat,
                    archivedCount = if (selectedKey == ChatListKey.Main) {
                        telegram.chatList(ChatListKey.Archive).size
                    } else {
                        0
                    },
                    onOpenArchive = { selectedKey = ChatListKey.Archive },
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
                val chat = openChatId?.let { telegram.chats[it] }
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
    newChat?.let { channel ->
        NewChatDialog(
            telegram = telegram,
            channel = channel,
            onCreated = {
                newChat = null
                onOpenChat(it)
            },
            onDismiss = { newChat = null },
        )
    }
}

// Material 3 Expressive navigation rail: folders when collapsed, the app
// menu when expanded.
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
    onNewChat: (channel: Boolean) -> Unit,
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
            MainMenu(telegram, settings, onNavigate, onOpenSaved, onNewChat)
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
                                else -> folderIcon(tab.icon, isSelected)
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

// Telegram's main menu: accounts, then the app actions.
@Composable
private fun MainMenu(
    telegram: Telegram,
    settings: AppSettings,
    onNavigate: (Screen) -> Unit,
    onOpenSaved: () -> Unit,
    onNewChat: (channel: Boolean) -> Unit,
) {
    var accountsOpen by remember { mutableStateOf(false) }
    Column(Modifier.verticalScroll(rememberScrollState())) {
        AccountHeader(telegram, settings, accountsOpen) { accountsOpen = !accountsOpen }
        if (accountsOpen) {
            telegram.me?.let { me ->
                WideNavigationRailItem(
                    selected = true,
                    onClick = {},
                    icon = { Avatar(me.fullName(), me.profilePhoto?.small, telegram, settings, size = 24.dp) },
                    label = { Text(me.fullName(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    railExpanded = true,
                )
            }
            WideNavigationRailItem(
                selected = false,
                onClick = {},
                enabled = false,
                icon = { Icon(Icons.Outlined.PersonAdd, contentDescription = null) },
                label = { Text("Add Account (soon)") },
                railExpanded = true,
            )
            HorizontalDivider(Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        }
        val items = listOf(
            Triple(Icons.Outlined.AccountCircle, "My Profile") { onNavigate(Screen.Profile) },
            Triple(Icons.Outlined.Groups, "New Group") { onNewChat(false) },
            Triple(Icons.Outlined.Campaign, "New Channel") { onNewChat(true) },
            Triple(Icons.Outlined.Contacts, "Contacts") { onNavigate(Screen.Contacts) },
            Triple(Icons.Outlined.Call, "Calls") { onNavigate(Screen.Calls) },
            Triple(Icons.Outlined.Bookmark, SavedMessagesTitle, onOpenSaved),
            Triple(Icons.Outlined.Settings, "Settings") { onNavigate(Screen.Settings()) },
        )
        items.forEach { (icon, label, action) ->
            WideNavigationRailItem(
                selected = false,
                onClick = action,
                icon = { Icon(icon, contentDescription = null) },
                label = { Text(label) },
                railExpanded = true,
            )
        }
        val dark = isDarkTheme(settings)
        val toggleNight = { settings.themeMode.value = if (dark) ThemeMode.Light else ThemeMode.Dark }
        WideNavigationRailItem(
            selected = false,
            onClick = toggleNight,
            icon = { Icon(Icons.Outlined.DarkMode, contentDescription = null) },
            label = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Night Mode", modifier = Modifier.padding(end = 16.dp))
                    PixelSwitch(checked = dark, onCheckedChange = { toggleNight() })
                }
            },
            railExpanded = true,
        )
    }
}

@Composable
private fun AccountHeader(
    telegram: Telegram,
    settings: AppSettings,
    accountsOpen: Boolean,
    onToggle: () -> Unit,
) {
    val me = telegram.me ?: return
    val name = me.fullName()
    Row(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onToggle)
            .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Avatar(name, me.profilePhoto?.small, telegram, settings, size = 48.dp)
        Column(Modifier.width(150.dp)) {
            Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            me.phoneNumber?.takeIf { it.isNotEmpty() }?.let {
                Text(
                    "+$it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Icon(
            if (accountsOpen) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = "Accounts",
        )
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
