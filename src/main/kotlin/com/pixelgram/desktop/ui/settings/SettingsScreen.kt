package com.pixelgram.desktop.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BatterySaver
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.BuildConfig
import com.pixelgram.desktop.core.AppPaths
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.core.ThemeMode
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.telegram.UiMessage
import com.pixelgram.desktop.telegram.fullName
import com.pixelgram.desktop.ui.AvatarShape
import com.pixelgram.desktop.ui.auth.QrCode
import com.pixelgram.desktop.ui.components.Avatar
import com.pixelgram.desktop.ui.components.InfoGroup
import com.pixelgram.desktop.ui.components.InfoRow
import com.pixelgram.desktop.ui.components.ShapePreview
import com.pixelgram.desktop.ui.main.MessageBubble
import com.pixelgram.desktop.ui.main.formatChatDate
import com.pixelgram.desktop.ui.storage.MemoryContent
import com.pixelgram.desktop.ui.theme.Matugen
import com.pixelgram.desktop.ui.theme.SeedColors
import com.pixelgram.desktop.ui.themedAppIcon
import it.tdlight.jni.TdApi
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.URI

// Telegram settings on the main list; pixelGram's own settings live in
// the "pixelGram Preferences" sub-list, like AyuGram Preferences.
private enum class Section(val title: String, val icon: ImageVector, val pixel: Boolean = false) {
    MyAccount("My Account", Icons.Outlined.AccountCircle),
    Notifications("Notifications and Sounds", Icons.Outlined.Notifications),
    Privacy("Privacy and Security", Icons.Outlined.Lock),
    ChatSettings("Chat Settings", Icons.Outlined.ChatBubbleOutline),
    Folders("Folders", Icons.Outlined.Folder),
    Advanced("Advanced", Icons.Outlined.Tune),
    SpeakersCamera("Speakers and Camera", Icons.Outlined.Headphones),
    Battery("Battery and Animations", Icons.Outlined.BatterySaver),
    Language("Language", Icons.Outlined.Translate),
    Appearance("Appearance", Icons.Outlined.Palette, pixel = true),
    Avatars("Avatars", Icons.Outlined.Face, pixel = true),
    Bubbles("Message bubbles", Icons.Outlined.ChatBubbleOutline, pixel = true),
}

private const val PixelPreferencesTitle = "pixelGram Preferences"
private const val ChannelUrl = "https://t.me/pixelgramclient"

@Composable
fun SettingsScreen(
    telegram: Telegram,
    settings: AppSettings,
    startInPreferences: Boolean = false,
    onOpenProfile: () -> Unit,
    onBack: () -> Unit,
) {
    var section by remember {
        val initial = System.getProperty("pixelgram.section")
        mutableStateOf(
            Section.entries.firstOrNull { it.name == initial }
                ?: if (startInPreferences) Section.Appearance else Section.MyAccount
        )
    }
    var inPreferences by remember { mutableStateOf(section.pixel) }
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxSize()) {
        Row {
            Column(
                Modifier
                    .width(320.dp)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        if (inPreferences) {
                            inPreferences = false
                            section = Section.MyAccount
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        if (inPreferences) PixelPreferencesTitle else "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    if (!inPreferences) {
                        IconButton(onClick = {
                            searching = !searching
                            query = ""
                        }) {
                            Icon(Icons.Outlined.Search, contentDescription = "Search")
                        }
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "More")
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text("Log out") },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null) },
                                    onClick = {
                                        menuOpen = false
                                        telegram.logOut()
                                    },
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (inPreferences) {
                    PreferencesHeader()
                    Header("Categories")
                    val items = Section.entries.filter { it.pixel }
                    SegmentedGroup {
                        items.forEachIndexed { index, item ->
                            NavItem(index, items.size, item.title, item.icon, item == section) {
                                section = item
                            }
                        }
                    }
                    Header("Links")
                    SegmentedListItem(
                        onClick = { openUrl(ChannelUrl) },
                        shapes = ListItemDefaults.segmentedShapes(0, 1),
                        leadingContent = { Icon(Icons.Outlined.Campaign, contentDescription = null) },
                        supportingContent = { Text("@pixelgramclient") },
                        trailingContent = { Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null) },
                    ) {
                        Text("Channel")
                    }
                } else {
                    if (searching) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            label = { Text("Search settings") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        )
                    }
                    val items = Section.entries.filter {
                        !it.pixel && (query.isBlank() || it.title.contains(query.trim(), ignoreCase = true))
                    }
                    if (query.isBlank()) {
                        ProfileCard(telegram, settings, onOpenProfile)
                        Spacer(Modifier.height(12.dp))
                        PreferencesEntry {
                            inPreferences = true
                            section = Section.Appearance
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    SegmentedGroup {
                        items.forEachIndexed { index, item ->
                            NavItem(index, items.size, item.title, item.icon, item == section) {
                                section = item
                            }
                        }
                    }
                }
            }
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(top = 8.dp, end = 8.dp, bottom = 8.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (section.pixel) {
                        Text(
                            PixelPreferencesTitle,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(section.title, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                    when (section) {
                        Section.Appearance -> AppearanceSection(settings)
                        Section.Avatars -> AvatarsSection(settings)
                        Section.Bubbles -> BubblesSection(settings)
                        Section.MyAccount -> AccountSection(telegram, onOpenProfile)
                        Section.Privacy -> PrivacySection(telegram)
                        Section.ChatSettings -> ChatSettingsSection(settings) {
                            inPreferences = true
                            section = Section.Bubbles
                        }
                        Section.Folders -> FoldersSection(telegram)
                        Section.Advanced -> AdvancedSection(telegram)
                        Section.Language -> Description("pixelGram is in English for now. More languages are coming.")
                        Section.Notifications,
                        Section.SpeakersCamera,
                        Section.Battery -> Description("Not available in pixelGram yet.")
                    }
                }
            }
        }
    }
}

// Profile summary at the top of Settings, like in Telegram.
@Composable
private fun ProfileCard(telegram: Telegram, settings: AppSettings, onClick: () -> Unit) {
    val me = telegram.me ?: return
    val username = me.usernames?.activeUsernames?.firstOrNull()
    var showQr by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Avatar(me.fullName(), me.profilePhoto?.small, telegram, settings, size = 64.dp)
            Column(Modifier.weight(1f)) {
                Text(me.fullName(), style = MaterialTheme.typography.titleLargeEmphasized, maxLines = 1)
                me.phoneNumber?.takeIf { it.isNotEmpty() }?.let {
                    Text("+$it", style = MaterialTheme.typography.bodyMedium)
                }
                username?.let {
                    Text("@$it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            if (username != null) {
                IconButton(onClick = { showQr = true }) {
                    Icon(Icons.Outlined.QrCode, contentDescription = "QR code")
                }
            }
        }
    }
    if (showQr && username != null) {
        AlertDialog(
            onDismissRequest = { showQr = false },
            title = { Text("@$username") },
            text = { QrCode("https://t.me/$username", Modifier.size(240.dp)) },
            confirmButton = { TextButton(onClick = { showQr = false }) { Text("Close") } },
        )
    }
}

// AyuGram-style header of the preferences list: app icon, name, version.
@Composable
private fun PreferencesHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    ) {
        themedAppIcon()?.let { Image(it, contentDescription = null, modifier = Modifier.size(96.dp)) }
        Text("pixelGram Desktop", style = MaterialTheme.typography.titleLargeEmphasized)
        Text(
            "v${BuildConfig.VERSION}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// Material 3 Expressive segmented list: items separated by a small gap,
// outer corners large, inner corners small.
@Composable
private fun SegmentedGroup(content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        content()
    }
}

@Composable
private fun NavItem(
    index: Int,
    count: Int,
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    SegmentedListItem(
        selected = selected,
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(index, count),
        leadingContent = { Icon(icon, contentDescription = null) },
    ) {
        Text(title)
    }
}

// Official Material 3 switch with check / close icons on the thumb.
@Composable
fun PixelSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        thumbContent = {
            Icon(
                imageVector = if (checked) Icons.Filled.Check else Icons.Filled.Close,
                contentDescription = null,
                modifier = Modifier.size(SwitchDefaults.IconSize),
            )
        },
    )
}

// Highlighted entry that opens the pixelGram Preferences sub-list.
@Composable
private fun PreferencesEntry(onClick: () -> Unit) {
    ListItem(
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable(onClick = onClick),
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            headlineColor = MaterialTheme.colorScheme.onPrimaryContainer,
            supportingColor = MaterialTheme.colorScheme.onPrimaryContainer,
            leadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
            trailingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        leadingContent = { Icon(Icons.Outlined.AutoAwesome, contentDescription = null) },
        headlineContent = { Text(PixelPreferencesTitle) },
        supportingContent = { Text("Appearance, avatars, bubbles") },
        trailingContent = {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        },
    )
}

@Composable
private fun Header(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun Description(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SwitchRow(
    index: Int,
    count: Int,
    title: String,
    subtitle: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    SegmentedListItem(
        checked = checked,
        onCheckedChange = onChange,
        shapes = ListItemDefaults.segmentedShapes(index, count),
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = { PixelSwitch(checked = checked, onCheckedChange = null) },
    ) {
        Text(title)
    }
}

@Composable
private fun AppearanceSection(settings: AppSettings) {
    Header("Theme")
    ThemeModeButtons(settings)

    Header("Accent color")
    Description("The whole color scheme is generated from this color with Google's Material color utilities.")
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 8.dp)) {
        SeedColors.forEach { seed ->
            val selected = settings.seedColor.value == seed
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(seed))
                    .then(
                        if (selected) {
                            Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        } else {
                            Modifier
                        }
                    )
                    .clickable { settings.seedColor.value = seed },
            )
        }
    }

    if (AppPaths.isLinux) {
        Header("Material You (matugen)")
        SwitchRow(
            index = 0,
            count = 1,
            title = "Dynamic colors from matugen",
            subtitle = "Colors follow your wallpaper. Overrides the accent color.",
            checked = settings.matugen.value,
            onChange = {
                settings.matugen.value = it
                if (it) Matugen.exportTemplate()
            },
        )
        Description(
            "Add these lines to ~/.config/matugen/config.toml, then run matugen. " +
                "pixelGram applies new colors automatically."
        )
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                Matugen.configSnippet,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(12.dp),
            )
        }
        OutlinedButton(onClick = {
            Matugen.exportTemplate()
            Toolkit.getDefaultToolkit().systemClipboard
                .setContents(StringSelection(Matugen.configSnippet), null)
        }) {
            Text("Copy config")
        }
    }

    Header("Chat folders")
    SegmentedGroup {
    SwitchRow(
        index = 0,
        count = 2,
        title = "Material 3 navigation rail",
        subtitle = "Show folders in a side rail. When off, folders are tabs above the chat list.",
        checked = settings.materialFolders.value,
        onChange = { settings.materialFolders.value = it },
    )
    SwitchRow(
        index = 1,
        count = 2,
        title = "Hide \"All chats\"",
        subtitle = "Only works when you have at least one folder.",
        checked = settings.hideAllChats.value,
        onChange = { settings.hideAllChats.value = it },
    )
    }
}

@Composable
private fun ThemeModeButtons(settings: AppSettings) {
    Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
        val modes = ThemeMode.entries
        modes.forEachIndexed { index, mode ->
            ToggleButton(
                checked = settings.themeMode.value == mode,
                onCheckedChange = { settings.themeMode.value = mode },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    modes.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
            ) {
                Text(mode.name)
            }
        }
    }
}

@Composable
private fun AvatarsSection(settings: AppSettings) {
    val current = settings.avatarShape.value
    val corner = settings.avatarCorner.value

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        ShapePreview(current, corner, 96.dp, selected = true)
        Column {
            Text(current.title, style = MaterialTheme.typography.titleMedium)
            Description("Official Material 3 Expressive shapes.")
        }
    }

    Header("Shape")
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AvatarShape.entries.forEach { shape ->
            val selected = shape == current
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(88.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(
                        if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                    )
                    .clickable { settings.avatarShape.value = shape }
                    .padding(8.dp),
            ) {
                ShapePreview(shape, corner, 48.dp, selected)
                Text(
                    shape.title,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }

    Header("Corner radius")
    Description("Used by the \"Custom corner\" shape: 0% is a square, 50% is a circle.")
    Row(verticalAlignment = Alignment.CenterVertically) {
        Slider(
            value = corner.toFloat(),
            onValueChange = { settings.avatarCorner.value = it.toInt() },
            valueRange = 0f..50f,
            steps = 49,
            enabled = current == AvatarShape.Custom,
            modifier = Modifier.weight(1f),
        )
        Text("$corner%", modifier = Modifier.width(56.dp).padding(start = 12.dp))
    }
}

@Composable
private fun BubblesSection(settings: AppSettings) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            MessageBubble(
                UiMessage(1, 0, 0, 0, false, 1_700_000_000, "Hi! How do the new corners look?"),
                senderName = null,
                settings = settings,
            )
            MessageBubble(
                UiMessage(2, 0, 0, 0, true, 1_700_000_060, "Way better with Material 3 ✨"),
                senderName = null,
                settings = settings,
            )
        }
    }

    Header("Corner radius")
    Row(verticalAlignment = Alignment.CenterVertically) {
        Slider(
            value = settings.bubbleRadius.value.toFloat(),
            onValueChange = { settings.bubbleRadius.value = it.toInt() },
            valueRange = 0f..32f,
            steps = 31,
            modifier = Modifier.weight(1f),
        )
        Text("${settings.bubbleRadius.value} dp", modifier = Modifier.width(64.dp).padding(start = 12.dp))
    }

    Header("Tail corner")
    Description("The corner next to the sender. Set it equal to the radius for fully round bubbles.")
    Row(verticalAlignment = Alignment.CenterVertically) {
        Slider(
            value = settings.bubbleTailRadius.value.toFloat(),
            onValueChange = { settings.bubbleTailRadius.value = it.toInt() },
            valueRange = 0f..32f,
            steps = 31,
            modifier = Modifier.weight(1f),
        )
        Text("${settings.bubbleTailRadius.value} dp", modifier = Modifier.width(64.dp).padding(start = 12.dp))
    }
}

@Composable
private fun AccountSection(telegram: Telegram, onOpenProfile: () -> Unit) {
    val me = telegram.me
    if (me != null) {
        InfoGroup(buildList {
            me.phoneNumber?.takeIf { it.isNotEmpty() }?.let { add(InfoRow(Icons.Outlined.Phone, "Phone", "+$it")) }
            me.usernames?.activeUsernames?.firstOrNull()?.let {
                add(InfoRow(Icons.Outlined.AlternateEmail, "Username", "@$it"))
            }
            add(InfoRow(Icons.Outlined.Numbers, "ID", me.id.toString()))
        })
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
        OutlinedButton(onClick = onOpenProfile) { Text("Open profile") }
        Button(onClick = telegram::logOut) { Text("Log out") }
    }
}

@Composable
private fun PrivacySection(telegram: Telegram) {
    var sessions by remember { mutableStateOf<List<TdApi.Session>?>(null) }
    LaunchedEffect(Unit) { telegram.activeSessions { sessions = it } }
    Header("Active sessions")
    val loaded = sessions
    if (loaded == null) {
        LoadingIndicator()
        return
    }
    val sorted = loaded.sortedWith(compareByDescending<TdApi.Session> { it.isCurrent }.thenByDescending { it.lastActiveDate })
    SegmentedGroup {
        sorted.forEachIndexed { index, session ->
            SegmentedListItem(
                onClick = {},
                shapes = ListItemDefaults.segmentedShapes(index, sorted.size),
                leadingContent = { Icon(Icons.Outlined.Devices, contentDescription = null) },
                supportingContent = {
                    Text(
                        listOf(
                            "${session.applicationName} ${session.applicationVersion}",
                            session.location,
                            if (session.isCurrent) "this device" else formatChatDate(session.lastActiveDate),
                        ).filter { it.isNotBlank() }.joinToString(" · ")
                    )
                },
            ) {
                Text(session.deviceModel.ifBlank { session.platform })
            }
        }
    }
}

@Composable
private fun ChatSettingsSection(settings: AppSettings, onOpenBubbles: () -> Unit) {
    Header("Theme")
    ThemeModeButtons(settings)
    Header("Message bubbles")
    Description("Bubble corners, avatar shapes and colors are in pixelGram Preferences.")
    OutlinedButton(onClick = onOpenBubbles) { Text("Open $PixelPreferencesTitle") }
}

@Composable
private fun FoldersSection(telegram: Telegram) {
    val folders = telegram.folders
    if (folders.isEmpty()) {
        Description("You have no chat folders.")
    } else {
        SegmentedGroup {
            folders.forEachIndexed { index, folder ->
                SegmentedListItem(
                    onClick = {},
                    shapes = ListItemDefaults.segmentedShapes(index, folders.size),
                    leadingContent = { Icon(Icons.Outlined.Folder, contentDescription = null) },
                ) {
                    Text(folder.title)
                }
            }
        }
    }
    Description("Create and edit folders in another Telegram app for now; changes appear here automatically.")
}

@Composable
private fun AdvancedSection(telegram: Telegram) {
    Header("Data and storage")
    MemoryContent(telegram)
    Header("Data folder")
    Description(
        (if (AppPaths.isPortable) "Portable mode: " else "") + AppPaths.dataDir.absolutePath
    )
    OutlinedButton(onClick = {
        runCatching { Desktop.getDesktop().open(AppPaths.dataDir) }
    }) {
        Text("Open data folder")
    }
}

private fun openUrl(url: String) {
    runCatching {
        if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(URI(url))
    }
}
