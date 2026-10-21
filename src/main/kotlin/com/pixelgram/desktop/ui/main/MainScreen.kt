package com.pixelgram.desktop.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationItemIconPosition
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailState
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.core.ThemeMode
import com.pixelgram.desktop.telegram.Accounts
import com.pixelgram.desktop.telegram.ChatListKey
import com.pixelgram.desktop.telegram.SavedMessagesTitle
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.telegram.fullName
import com.pixelgram.desktop.ui.Screen
import com.pixelgram.desktop.ui.components.Avatar
import com.pixelgram.desktop.ui.settings.PixelSwitch
import com.pixelgram.desktop.ui.theme.emojiText
import com.pixelgram.desktop.ui.theme.isDarkTheme
import java.awt.Cursor
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

internal data class FolderTab(val key: ChatListKey, val title: String, val icon: String = "")

@Composable
fun MainScreen(
    accounts: Accounts,
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
    var selectedKey by rememberSaveable { mutableStateOf<ChatListKey>(ChatListKey.Main) }
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

    // The experimental capsule only exists on top.
    val foldersLeft = settings.materialFolders.value && !settings.capsuleFolders.value
    val railSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntSize>()
    val fade = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val density = LocalDensity.current
    // Width while dragging the divider; snaps to the compact list when narrow.
    var dragWidth by remember { mutableStateOf(settings.chatListWidth.value.toFloat()) }

    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val maxListWidth = (maxWidth - MinChatWidth - RailWidth).value.coerceAtLeast(CompactWidth.value)
            val compact = dragWidth < CompactThreshold.value
            val listWidth = if (compact) CompactWidth else dragWidth.coerceAtMost(maxListWidth).dp
            Row(Modifier.fillMaxSize()) {
                // The rail is the folders bar; the menu button expands it into the app menu.
                AnimatedVisibility(
                    visible = foldersLeft || menuOpen,
                    enter = expandHorizontally(railSpec) + fadeIn(fade),
                    exit = shrinkHorizontally(railSpec) + fadeOut(fade),
                ) {
                    PixelRail(
                        accounts = accounts,
                        state = railState,
                        expanded = menuOpen,
                        showFolders = foldersLeft,
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
                        .width(listWidth)
                        .fillMaxHeight()
                        .padding(vertical = 8.dp),
                ) {
                    if (!foldersLeft && !compact && selectedKey != ChatListKey.Archive) {
                        if (settings.capsuleFolders.value) {
                            CapsuleFolders(tabs, selectedKey, settings, onSelect = { selectedKey = it })
                        } else {
                            TopFolders(tabs, selectedKey, onSelect = { selectedKey = it })
                        }
                    }
                    ChatList(
                        listKey = selectedKey,
                        title = if (selectedKey == ChatListKey.Archive) "Archived chats" else "pixelGram",
                        chatsFor = telegram::chatList,
                        order = { key -> if (key == ChatListKey.Archive) -1 else tabs.indexOfFirst { it.key == key } },
                        vertical = foldersLeft,
                        compact = compact,
                        selectedChatId = openChatId,
                        telegram = telegram,
                        settings = settings,
                        onOpen = onOpenChat,
                        archivedCount = telegram.chatList(ChatListKey.Archive).size,
                        onOpenArchive = { selectedKey = ChatListKey.Archive },
                        onMenu = if (foldersLeft) null else toggleMenu,
                        onBack = if (selectedKey == ChatListKey.Archive) {
                            { selectedKey = tabs.firstOrNull()?.key ?: ChatListKey.Main }
                        } else {
                            null
                        },
                    )
                }
                // Drag to resize the chat list; too narrow leaves only avatars.
                Box(
                    Modifier
                        .width(DividerWidth)
                        .fillMaxHeight()
                        .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
                        .draggable(
                            orientation = Orientation.Horizontal,
                            state = rememberDraggableState { delta ->
                                // Accumulates freely, so dragging out of the compact list works.
                                dragWidth = (dragWidth + with(density) { delta.toDp() }.value)
                                    .coerceIn(CompactWidth.value, maxListWidth)
                            },
                            onDragStopped = {
                                if (dragWidth < CompactThreshold.value) dragWidth = CompactWidth.value
                                settings.chatListWidth.value = dragWidth.roundToInt()
                            },
                        ),
                )
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
                        ChatView(chat, telegram, settings, onOpenChat)
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
    accounts: Accounts,
    state: WideNavigationRailState,
    expanded: Boolean,
    showFolders: Boolean,
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
        val fade = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
        AnimatedContent(
            targetState = expanded,
            transitionSpec = { fadeIn(fade) togetherWith fadeOut(fade) },
        ) { showMenu ->
            when {
                showMenu -> MainMenu(accounts, telegram, settings, onNavigate, onOpenSaved, onNewChat)
                showFolders -> Column {
                    tabs.forEach { tab ->
                        val isSelected = tab.key == selected
                        WideNavigationRailItem(
                            selected = isSelected,
                            onClick = { onSelect(tab.key) },
                            icon = { Icon(folderIcon(tab.icon, isSelected), contentDescription = tab.title) },
                            label = { Text(emojiText(tab.title), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            railExpanded = false,
                        )
                    }
                }
            }
        }
    }
}

// Telegram's main menu: accounts, then the app actions.
@Composable
private fun MainMenu(
    accounts: Accounts,
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
            accounts.list.forEach { account ->
                val me = account.me
                val name = me?.fullName() ?: "Signing in…"
                val unread = account.chatList(ChatListKey.Main).sumOf { it.unreadCount }
                WideNavigationRailItem(
                    selected = account == accounts.active,
                    onClick = { accounts.switchTo(account) },
                    icon = { Avatar(name, me?.profilePhoto?.small, account, settings, size = 24.dp) },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(emojiText(name), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (unread > 0 && account != accounts.active) {
                                Badge(Modifier.padding(start = 8.dp)) { Text(unread.toString()) }
                            }
                        }
                    },
                    railExpanded = true,
                )
            }
            WideNavigationRailItem(
                selected = false,
                onClick = accounts::add,
                enabled = accounts.canAdd,
                icon = { Icon(Icons.Outlined.PersonAdd, contentDescription = null) },
                label = { Text("Add Account") },
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
            Text(emojiText(name), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
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

// Material 3 Expressive navigation items laid out horizontally, the same
// style as the rail.
@Composable
private fun TopFolders(
    tabs: List<FolderTab>,
    selected: ChatListKey,
    onSelect: (ChatListKey) -> Unit,
) {
    if (tabs.size < 2) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        tabs.forEach { tab ->
            val isSelected = tab.key == selected
            ShortNavigationBarItem(
                selected = isSelected,
                onClick = { onSelect(tab.key) },
                icon = { Icon(folderIcon(tab.icon, isSelected), contentDescription = null) },
                label = { Text(emojiText(tab.title), maxLines = 1) },
                iconPosition = NavigationItemIconPosition.Start,
            )
        }
    }
}

private val MinChatWidth = 380.dp
private val RailWidth = 96.dp
private val CompactWidth = 88.dp
private val CompactThreshold = 220.dp
private val DividerWidth = 8.dp
