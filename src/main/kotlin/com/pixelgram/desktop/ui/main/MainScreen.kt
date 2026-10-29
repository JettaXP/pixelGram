package com.pixelgram.desktop.ui.main

import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.toShape
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import com.pixelgram.desktop.ui.icons.filled.Edit
import com.pixelgram.desktop.ui.icons.filled.Close
import com.pixelgram.desktop.ui.components.groupShapes
import com.pixelgram.desktop.ui.OnEscape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import com.pixelgram.desktop.ui.components.PhoneText
import com.pixelgram.desktop.telegram.UnreadCount
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.BadgedBox
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.automirrored.filled.MenuOpen
import com.pixelgram.desktop.ui.icons.filled.Check
import com.pixelgram.desktop.ui.icons.filled.KeyboardArrowDown
import com.pixelgram.desktop.ui.icons.filled.Menu
import com.pixelgram.desktop.ui.icons.outlined.AccountCircle
import com.pixelgram.desktop.ui.icons.outlined.Bookmark
import com.pixelgram.desktop.ui.icons.outlined.Call
import com.pixelgram.desktop.ui.icons.outlined.Campaign
import com.pixelgram.desktop.ui.icons.outlined.Contacts
import com.pixelgram.desktop.ui.icons.outlined.DarkMode
import com.pixelgram.desktop.ui.icons.outlined.LightMode
import com.pixelgram.desktop.ui.icons.outlined.Groups
import com.pixelgram.desktop.ui.icons.outlined.PersonAdd
import com.pixelgram.desktop.ui.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import com.pixelgram.desktop.ui.components.expressive.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationItemIconPosition
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Surface
import com.pixelgram.desktop.ui.components.Text
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelgram.desktop.core.AppPaths
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
import com.pixelgram.desktop.ui.theme.EmojiText
import com.pixelgram.desktop.ui.theme.isDarkTheme
import java.awt.Cursor
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

internal data class FolderTab(
    val key: ChatListKey,
    val title: String,
    val icon: String = "",
    val unread: UnreadCount? = null,
)

@Composable
internal fun FolderIcon(tab: FolderTab, selected: Boolean, tint: Color = LocalContentColor.current) {
    val unread = tab.unread?.takeIf { it.chats > 0 }
    BadgedBox(
        badge = {
            if (unread != null) {
                Badge(
                    containerColor = if (unread.unmuted > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    contentColor = if (unread.unmuted > 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.surface,
                ) {
                    Text(if (unread.chats > 999) "999+" else unread.chats.toString())
                }
            }
        },
    ) {
        Icon(folderIcon(tab.icon, selected), contentDescription = tab.title, tint = tint)
    }
}

@Composable
fun MainScreen(
    accounts: Accounts,
    telegram: Telegram,
    settings: AppSettings,
    openChatId: Long?,
    onOpenChat: (Long) -> Unit,
    onNavigate: (Screen) -> Unit,
    onCloseChat: () -> Unit = {},
    onOpenChatInside: (Long) -> Unit = onOpenChat,
    onChatBack: (() -> Unit)? = null,
) {
    LinkDialogs(settings, onOpenChat)
    val tabs = FolderLayout.tabs(telegram, settings)
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

    val foldersLeft = settings.materialFolders.value && !settings.capsuleFolders.value && tabs.size > 1
    val railSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntSize>()
    val fade = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val density = LocalDensity.current
    var dragWidth by remember { mutableStateOf(settings.chatListWidth.value.toFloat()) }
    var railWidthPx by remember { androidx.compose.runtime.mutableIntStateOf(0) }

    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val maxListWidth = minOf((maxWidth - MinChatWidth - RailWidth).value, maxWidth.value * MaxListShare).coerceAtLeast(CompactWidth.value)
            val forum = openChatId?.let { telegram.chats[it] }?.takeIf { telegram.isForum(it) }
            val compact = forum != null || dragWidth < CompactThreshold.value
            val listWidth by androidx.compose.animation.core.animateDpAsState(
                if (compact) CompactWidth else dragWidth.coerceAtMost(maxListWidth).dp,
                MaterialTheme.motionScheme.defaultSpatialSpec(),
            )
            val expandedWidth = maxOf(listWidth, maxOf(dragWidth.coerceAtMost(maxListWidth), CompactThreshold.value).dp)
            val push = settings.sideMenuStyle.value == com.pixelgram.desktop.core.SideMenuStyle.Push
            Row(Modifier.fillMaxSize()) {
                when {
                    push -> Spacer(Modifier.width(with(density) { railWidthPx.toDp() }.coerceAtLeast(if (foldersLeft) RailWidth else 0.dp)))
                    foldersLeft -> Spacer(Modifier.width(RailWidth))
                }
                Column(
                    Modifier
                        .width(listWidth)
                        .fillMaxHeight()
                        .clipToBounds()
                        .padding(vertical = if (AppPaths.isMac) 0.dp else 8.dp),
                ) {
                    val topFolders = @Composable {
                        if (!foldersLeft && selectedKey != ChatListKey.Archive) {
                            if (settings.capsuleFolders.value) {
                                CapsuleFolders(tabs, selectedKey, settings, onSelect = { if (it == selectedKey) ChatListScroll.request++ else selectedKey = it })
                            } else {
                                TopFolders(tabs, selectedKey, onSelect = { if (it == selectedKey) ChatListScroll.request++ else selectedKey = it })
                            }
                        }
                    }
                    val searchSlide = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
                    AnimatedContent(
                        targetState = MessageSearch.open && !compact,
                        transitionSpec = {
                            (fadeIn(fade) + slideInVertically(searchSlide) { it / SearchSlideDivisor }) togetherWith
                                (fadeOut(fade) + slideOutVertically(searchSlide) { -it / SearchSlideDivisor })
                        },
                        modifier = Modifier.weight(1f),
                    ) { searching ->
                        if (searching) {
                            Column {
                                topFolders()
                                MessageSearchPanel(telegram, settings, onOpen = onOpenChat)
                            }
                        } else {
                            Box(Modifier.fillMaxSize()) {
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
                                    archivedCount = telegram.chatCount(ChatListKey.Archive),
                                    onOpenArchive = { selectedKey = ChatListKey.Archive },
                                    onMenu = if (foldersLeft) null else toggleMenu,
                                    onSearch = { MessageSearch.start(null) },
                                    menuOpen = menuOpen,
                                    belowHeader = topFolders,
                                    compactWidth = CompactWidth,
                                    expandedWidth = expandedWidth,
                                    archiveOn = if (tabs.any { it.key == ChatListKey.Main }) ChatListKey.Main else tabs.firstOrNull()?.key ?: ChatListKey.Main,
                                    onBack = if (selectedKey == ChatListKey.Archive) {
                                        { selectedKey = tabs.firstOrNull()?.key ?: ChatListKey.Main }
                                    } else {
                                        null
                                    },
                                )
                                Box(Modifier.wrapContentWidth(Alignment.Start, unbounded = true).width(expandedWidth).fillMaxHeight()) {
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = !compact && selectedKey != ChatListKey.Archive,
                                        modifier = Modifier.align(Alignment.BottomEnd),
                                        enter = scaleIn(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeIn(fade),
                                        exit = scaleOut(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeOut(fade),
                                    ) {
                                        NewChatFab(
                                            onNewChat = { channel -> newChat = channel },
                                            onContacts = { onNavigate(Screen.Contacts) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Box(
                    Modifier
                        .width(DividerWidth)
                        .fillMaxHeight()
                        .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
                        .draggable(
                            orientation = Orientation.Horizontal,
                            state = rememberDraggableState { delta ->
                                dragWidth = (dragWidth + with(density) { delta.toDp() }.value)
                                    .coerceIn(CompactWidth.value, maxListWidth)
                            },
                            onDragStopped = {
                                if (dragWidth < CompactThreshold.value) dragWidth = CompactWidth.value
                                settings.chatListWidth.value = dragWidth.roundToInt()
                            },
                        ),
                )
                androidx.compose.animation.AnimatedVisibility(
                    visible = forum != null,
                    enter = fadeIn(fade) + expandHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec(), expandFrom = Alignment.Start),
                    exit = fadeOut(fade) + shrinkHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec(), shrinkTowards = Alignment.Start),
                ) {
                    var shownForum by remember { mutableStateOf(forum) }
                    if (forum != null) shownForum = forum
                    shownForum?.let { chat ->
                        androidx.compose.runtime.key(chat.id) { ForumSidebar(chat, telegram, Modifier.fillMaxHeight().padding(end = 8.dp), onClose = onCloseChat) }
                    }
                }
                OnEscape {
                    when {
                        menuOpen -> scope.launch { railState.collapse() }
                        MessageSearch.open -> MessageSearch.open = false
                        onChatBack != null -> onChatBack()
                        openChatId != null -> onCloseChat()
                        selectedKey == ChatListKey.Archive -> selectedKey = tabs.firstOrNull()?.key ?: ChatListKey.Main
                        else -> return@OnEscape false
                    }
                    true
                }
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(
                            top = if (AppPaths.isMac) 0.dp else 8.dp,
                            end = if (AppPaths.isMac) 0.dp else 8.dp,
                            bottom = if (AppPaths.isMac) 0.dp else 8.dp,
                        ),
                    shape = if (AppPaths.isMac) {
                        RoundedCornerShape(topStart = MacPaneCornerRadius, bottomStart = MacPaneCornerRadius)
                    } else {
                        MaterialTheme.shapes.extraLarge
                    },
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    val chat = openChatId?.let { telegram.chats[it] }
                    AnimatedContent(
                        targetState = chat,
                        transitionSpec = { fadeIn(tween(EmptyFadeMs)) togetherWith fadeOut(tween(EmptyFadeMs)) },
                        contentKey = { it?.id },
                    ) { shown ->
                        if (shown != null) {
                            ChatView(shown, telegram, settings, onOpenChatInside, onChatBack, forumSidebar = false)
                        } else {
                            androidx.compose.foundation.layout.Column(Modifier.fillMaxSize()) {
                            MiniPlayer(telegram)
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().weight(1f)) {
                                androidx.compose.animation.AnimatedVisibility(
                                    settings.emptyShapes.value,
                                    enter = fadeIn(),
                                    exit = fadeOut(),
                                    modifier = Modifier.matchParentSize(),
                                ) { EmptyShapes() }
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ) {
                                    Text(
                                        "Select a chat to start messaging",
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    )
                                }
                            }
                            }
                        }
                    }
                }
            }
            val scrim by animateFloatAsState(if (menuOpen && !push) RailScrimAlpha else 0f, fade)
            if (scrim > 0f) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = scrim }
                        .background(Color.Black)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { scope.launch { railState.collapse() } },
                )
            }
            AnimatedVisibility(
                visible = foldersLeft || menuOpen,
                modifier = Modifier.zIndex(1f).onSizeChanged { railWidthPx = it.width },
                enter = expandHorizontally(railSpec) + fadeIn(fade),
                exit = shrinkHorizontally(railSpec) + fadeOut(fade),
            ) {
                PixelRail(
                    accounts = accounts,
                    state = railState,
                    expanded = menuOpen,
                    showFolders = foldersLeft,
                    showMenuButton = foldersLeft,
                    telegram = telegram,
                    settings = settings,
                    tabs = tabs,
                    selected = selectedKey,
                    onToggleMenu = toggleMenu,
                    onSelect = { key -> closeMenuAnd { if (key == selectedKey) ChatListScroll.request++ else selectedKey = key } },
                    onNavigate = { screen -> closeMenuAnd { onNavigate(screen) } },
                    onOpenSaved = {
                        closeMenuAnd { telegram.openSavedMessages(onOpenChat) }
                    },
                    onNewChat = { channel -> closeMenuAnd { newChat = channel } },
                )
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

@Composable
private fun PixelRail(
    accounts: Accounts,
    state: WideNavigationRailState,
    expanded: Boolean,
    showFolders: Boolean,
    showMenuButton: Boolean,
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
        header = if (showMenuButton) {
            {
                IconButton(onClick = onToggleMenu, modifier = Modifier.padding(start = 24.dp)) {
                    Icon(
                        if (expanded) Icons.AutoMirrored.Filled.MenuOpen else Icons.Filled.Menu,
                        contentDescription = "Menu",
                    )
                }
            }
        } else {
            null
        },
    ) {
        AnimatedContent(
            targetState = expanded,
            transitionSpec = {
                fadeIn(tween(RailFadeInMs, delayMillis = RailFadeOutMs)) togetherWith
                    fadeOut(tween(RailFadeOutMs)) using SizeTransform(clip = true) { _, _ ->
                    tween(RailResizeMs, easing = FastOutSlowInEasing)
                }
            },
            contentAlignment = Alignment.TopStart,
            modifier = Modifier.clipToBounds(),
        ) { showMenu ->
            when {
                showMenu -> MainMenu(accounts, telegram, settings, onNavigate, onOpenSaved, onNewChat)
                showFolders -> Column {
                    tabs.forEach { tab ->
                        val isSelected = tab.key == selected
                        WideNavigationRailItem(
                            selected = isSelected,
                            onClick = { onSelect(tab.key) },
                            icon = { FolderIcon(tab, isSelected) },
                            label = { EmojiText(tab.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            railExpanded = false,
                        )
                    }
                }
            }
        }
    }
}

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
        val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<IntSize>()
        val fade = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
        AnimatedVisibility(
            visible = accountsOpen,
            enter = expandVertically(spatial) + fadeIn(fade),
            exit = shrinkVertically(spatial) + fadeOut(fade),
        ) {
            AccountsList(accounts, settings)
        }
        val dark = isDarkTheme(settings)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.width(AccountsWidth).padding(horizontal = 12.dp).padding(bottom = 12.dp),
        ) {
            QuickTile(if (dark) Icons.Outlined.DarkMode else Icons.Outlined.LightMode, if (dark) "Night" else "Day", dark, Modifier.weight(1f)) {
                settings.themeMode.value = if (dark) ThemeMode.Light else ThemeMode.Dark
            }
            QuickTile(Icons.Outlined.Bookmark, "Saved", false, Modifier.weight(1f), onOpenSaved)
        }
        val groups = listOf(
            listOf(
                Triple(Icons.Outlined.AccountCircle, "My Profile") { onNavigate(Screen.Profile) },
                Triple(Icons.Outlined.Contacts, "Contacts") { onNavigate(Screen.Contacts) },
                Triple(Icons.Outlined.Call, "Calls") { onNavigate(Screen.Calls) },
            ),
            listOf(
                Triple(Icons.Outlined.Groups, "New Group") { onNewChat(false) },
                Triple(Icons.Outlined.Campaign, "New Channel") { onNewChat(true) },
            ),
            listOf(
                Triple(Icons.Outlined.Settings, "Settings") { onNavigate(Screen.Settings()) },
            ),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.width(AccountsWidth).padding(horizontal = 12.dp),
        ) {
            groups.forEach { group ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    group.forEachIndexed { index, (icon, label, action) ->
                        SegmentedListItem(
                            onClick = action,
                            shapes = groupShapes(index, group.size),
                            colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                            leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        ) { Text(label, style = MaterialTheme.typography.titleSmall) }
                    }
                }
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 8.dp),
            ) {
                Text(
                    listOf("pixelGram ${com.pixelgram.desktop.BuildConfig.VERSION}", com.pixelgram.desktop.BuildConfig.CODENAME).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NewChatFab(onNewChat: (Boolean) -> Unit, onContacts: () -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    androidx.compose.material3.FloatingActionButtonMenu(
        expanded = expanded,
        button = {
            androidx.compose.material3.ToggleFloatingActionButton(
                checked = expanded,
                onCheckedChange = { expanded = it },
                modifier = Modifier.focusProperties { canFocus = false },
            ) {
                val progress = checkedProgress
                Icon(
                    if (progress > 0.5f) Icons.Filled.Close else Icons.Filled.Edit,
                    contentDescription = if (expanded) "Close" else "New chat",
                    modifier = Modifier.animateIcon({ progress }),
                )
            }
        },
        modifier = modifier,
    ) {
        listOf(
            Triple(Icons.Outlined.Contacts, "New message") { onContacts() },
            Triple(Icons.Outlined.Groups, "New group") { onNewChat(false) },
            Triple(Icons.Outlined.Campaign, "New channel") { onNewChat(true) },
        ).forEach { (icon, label, action) ->
            FloatingActionButtonMenuItem(
                onClick = {
                    expanded = false
                    action()
                },
                icon = { Icon(icon, contentDescription = null) },
                text = { Text(label) },
            )
        }
    }
}

@Composable
private fun QuickTile(icon: ImageVector, label: String, active: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val corner by animateDpAsState(
        when {
            pressed -> TilePressedCorner
            active -> TileActiveCorner
            else -> TileCorner
        },
        MaterialTheme.motionScheme.fastSpatialSpec(),
    )
    val container by animateColorAsState(if (active) colors.primary else colors.surfaceContainerHighest, MaterialTheme.motionScheme.defaultEffectsSpec())
    val content by animateColorAsState(if (active) colors.onPrimary else colors.onSurface, MaterialTheme.motionScheme.defaultEffectsSpec())
    Surface(
        onClick = onClick,
        interactionSource = interaction,
        shape = RoundedCornerShape(corner),
        color = container,
        contentColor = content,
        modifier = modifier.height(TileHeight),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 14.dp),
        ) {
            Icon(icon, contentDescription = null)
            val style = MaterialTheme.typography.titleSmall
            androidx.compose.foundation.text.BasicText(
                com.pixelgram.desktop.core.tr(label),
                style = style.copy(color = LocalContentColor.current),
                maxLines = 1,
                autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(minFontSize = TileMinLabel, maxFontSize = style.fontSize),
            )
        }
    }
}

private val TileHeight = 64.dp
private val TileMinLabel = 10.sp
private val DrawerBannerHeight = 120.dp
private val TileCorner = 20.dp
private val TileActiveCorner = 32.dp
private val TilePressedCorner = 12.dp

@Composable
private fun AccountsList(accounts: Accounts, settings: AppSettings) {
    val count = accounts.list.size + 1
    Column(
        modifier = Modifier.width(AccountsWidth).padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(AccountsGap),
    ) {
        accounts.list.forEachIndexed { index, account ->
            val me = account.me
            val name = me?.fullName() ?: "Signing in…"
            val active = account == accounts.active
            val unread = account.chatList(ChatListKey.Main).sumOf { it.unreadCount }
            SegmentedListItem(
                selected = active,
                onClick = { accounts.switchTo(account) },
                shapes = groupShapes(index, count),
                leadingContent = { Avatar(name, me?.profilePhoto?.small, account, settings, size = 32.dp) },
                trailingContent = when {
                    active -> {
                        { Icon(Icons.Filled.Check, contentDescription = "Current account") }
                    }
                    unread > 0 -> {
                        { Badge { Text(unread.toString()) } }
                    }
                    else -> null
                },
            ) {
                EmojiText(name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        SegmentedListItem(
            onClick = accounts::add,
            enabled = accounts.canAdd,
            shapes = groupShapes(count - 1, count),
            leadingContent = { Icon(Icons.Outlined.PersonAdd, contentDescription = null) },
        ) {
            Text("Add Account")
        }
    }
}

private val AccountsWidth = 280.dp
private val AccountsGap = 6.dp

@Composable
private fun AccountHeader(
    telegram: Telegram,
    settings: AppSettings,
    accountsOpen: Boolean,
    onToggle: () -> Unit,
) {
    val me = telegram.me ?: return
    val name = me.fullName()
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onToggle,
        shape = MaterialTheme.shapes.large,
        color = colors.primaryContainer,
        contentColor = colors.onPrimaryContainer,
        modifier = Modifier.width(AccountsWidth).padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
    ) {
      Box {
        if (com.pixelgram.desktop.core.Banners.banner(me.id) != null) {
            com.pixelgram.desktop.ui.profile.ProfileBanner(
                me.id,
                telegram,
                DrawerBannerHeight,
                RoundedCornerShape(topStart = com.pixelgram.desktop.ui.components.LocalCorners.current.cards, topEnd = com.pixelgram.desktop.ui.components.LocalCorners.current.cards),
                colors.primaryContainer,
            )
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.clip(CircleShape).background(colors.surface.copy(alpha = 0.35f)).padding(4.dp)) {
                    Avatar(name, me.profilePhoto?.small, telegram, settings, size = 60.dp)
                }
                Spacer(Modifier.weight(1f))
                val turn by animateFloatAsState(
                    if (accountsOpen) 180f else 0f,
                    MaterialTheme.motionScheme.defaultSpatialSpec(),
                )
                Surface(shape = CircleShape, color = colors.surface.copy(alpha = 0.35f)) {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Accounts",
                        modifier = Modifier.padding(6.dp).rotate(turn),
                    )
                }
            }
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EmojiText(
                        name,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    telegram.chats[me.id]?.let { own -> com.pixelgram.desktop.ui.components.ChatBadges(own, telegram, 22.dp) }
                        ?: com.pixelgram.desktop.ui.components.UserBadge.of(me.id)?.let { com.pixelgram.desktop.ui.components.UserBadgeView(it, telegram, 22.dp) }
                }
                me.phoneNumber?.takeIf { it.isNotEmpty() }?.let {
                    PhoneText(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onPrimaryContainer.copy(alpha = 0.75f),
                    )
                }
            }
        }
      }
    }
}

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
                icon = { FolderIcon(tab, isSelected) },
                label = { EmojiText(tab.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                iconPosition = NavigationItemIconPosition.Top,
                modifier = Modifier.width(TopFolderWidth),
            )
        }
    }
}

private val TopFolderWidth = 80.dp
private const val RailFadeOutMs = 110
private const val EmptyFadeMs = 180
private const val RailFadeInMs = 180
private const val RailResizeMs = 280
private val MinChatWidth = 380.dp
private val RailWidth = 96.dp
private const val RailScrimAlpha = 0.32f
private val CompactWidth = 88.dp
private val CompactThreshold = 220.dp
private val DividerWidth = 8.dp

private const val SearchSlideDivisor = 12

private const val MaxListShare = 0.45f
private val MacPaneCornerRadius = 28.dp

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun EmptyShapes(spin: Boolean = false, scale: Float = 1f) {
    val scheme = MaterialTheme.colorScheme
    val turn = if (spin) {
        androidx.compose.animation.core.rememberInfiniteTransition().animateFloat(
            0f,
            360f,
            androidx.compose.animation.core.infiniteRepeatable(tween(EmptySpinMs, easing = androidx.compose.animation.core.LinearEasing)),
        )
    } else {
        null
    }
    val shapes = listOf(
        EmptyShape(androidx.compose.material3.MaterialShapes.Cookie12Sided, scheme.primaryContainer, -0.28f, -0.22f, 120.dp, 12f),
        EmptyShape(androidx.compose.material3.MaterialShapes.Flower, scheme.tertiaryContainer, 0.3f, -0.26f, 96.dp, -18f),
        EmptyShape(androidx.compose.material3.MaterialShapes.Pill, scheme.secondaryContainer, 0.26f, 0.24f, 110.dp, 35f),
        EmptyShape(androidx.compose.material3.MaterialShapes.Sunny, scheme.primaryContainer, -0.3f, 0.26f, 84.dp, 0f),
        EmptyShape(androidx.compose.material3.MaterialShapes.Cookie4Sided, scheme.tertiaryContainer, -0.05f, -0.36f, 56.dp, 20f),
        EmptyShape(androidx.compose.material3.MaterialShapes.SoftBurst, scheme.secondaryContainer, 0.04f, 0.36f, 64.dp, -10f),
    )
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
        shapes.forEachIndexed { index, item ->
            Box(
                Modifier
                    .align(Alignment.Center)
                    .offset(maxWidth * item.x, maxHeight * item.y)
                    .size(item.size * scale)
                    .graphicsLayer {
                        rotationZ = item.rotation + (turn?.value ?: 0f) * (index % 3 + 1) * if (index % 2 == 0) 1 else -1
                        alpha = EmptyShapeAlpha
                    }
                    .background(item.color, item.shape.toShape()),
            )
        }
    }
}

private class EmptyShape(
    val shape: androidx.graphics.shapes.RoundedPolygon,
    val color: androidx.compose.ui.graphics.Color,
    val x: Float,
    val y: Float,
    val size: androidx.compose.ui.unit.Dp,
    val rotation: Float,
)

private const val EmptyShapeAlpha = 0.55f
private const val EmptySpinMs = 24_000
