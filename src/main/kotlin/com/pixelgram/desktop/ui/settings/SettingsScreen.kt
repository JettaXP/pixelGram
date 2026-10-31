package com.pixelgram.desktop.ui.settings

import kotlin.math.roundToInt
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.focusRequester
import com.pixelgram.desktop.ui.icons.outlined.Edit
import com.pixelgram.desktop.ui.icons.outlined.ContentCopy
import com.pixelgram.desktop.ui.icons.outlined.PushPin
import com.pixelgram.desktop.ui.icons.outlined.Delete
import com.pixelgram.desktop.ui.icons.automirrored.outlined.Reply
import com.pixelgram.desktop.ui.icons.outlined.Settings
import com.pixelgram.desktop.core.PythonRunner
import com.pixelgram.desktop.ui.icons.outlined.DataObject
import com.pixelgram.desktop.ui.icons.outlined.Computer
import com.pixelgram.desktop.ui.icons.outlined.MoreHoriz
import com.pixelgram.desktop.ui.icons.outlined.FormatListBulleted
import com.pixelgram.desktop.ui.icons.outlined.Keyboard
import com.pixelgram.desktop.ui.icons.outlined.ArrowForward
import com.pixelgram.desktop.ui.icons.outlined.Language
import com.pixelgram.desktop.ui.icons.automirrored.outlined.Chat
import com.pixelgram.desktop.ui.icons.outlined.Person
import com.pixelgram.desktop.ui.icons.outlined.PersonAdd
import com.pixelgram.desktop.ui.icons.outlined.RestartAlt
import io.github.vinceglb.filekit.dialogs.openFilePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import com.pixelgram.desktop.ui.components.expressive.BasicAlertDialog
import com.pixelgram.desktop.ui.components.groupShapes
import com.pixelgram.desktop.ui.UpdatePanel
import com.pixelgram.desktop.ui.theme.EmojiVisualTransformation
import com.pixelgram.desktop.ui.OnEscape
import kotlinx.coroutines.launch
import java.io.File
import com.pixelgram.desktop.ui.main.BackupImportDialog
import com.pixelgram.desktop.ui.main.Attachments
import com.pixelgram.desktop.ui.main.ForwardDialog
import com.pixelgram.desktop.core.Backup
import com.pixelgram.desktop.ui.icons.outlined.Download
import com.pixelgram.desktop.ui.icons.outlined.Upload
import com.pixelgram.desktop.ui.icons.outlined.Code
import com.pixelgram.desktop.ui.icons.outlined.Info
import androidx.compose.material3.FilterChip
import com.pixelgram.desktop.core.AvatarKind
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pixelgram.desktop.core.DoubleClickAction
import com.pixelgram.desktop.core.SwipeAction
import com.pixelgram.desktop.ui.main.icon
import com.pixelgram.desktop.ui.icons.outlined.SwipeLeft
import com.pixelgram.desktop.ui.icons.outlined.SwipeRight
import com.pixelgram.desktop.ui.components.PhoneText
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.outlined.VpnKey
import com.pixelgram.desktop.ui.icons.automirrored.filled.ArrowBack
import com.pixelgram.desktop.ui.icons.automirrored.filled.KeyboardArrowRight
import com.pixelgram.desktop.ui.icons.automirrored.outlined.Logout
import com.pixelgram.desktop.ui.icons.automirrored.outlined.OpenInNew
import com.pixelgram.desktop.ui.icons.filled.Check
import com.pixelgram.desktop.ui.icons.filled.Close
import com.pixelgram.desktop.ui.icons.filled.MoreVert
import com.pixelgram.desktop.ui.icons.outlined.AccountCircle
import com.pixelgram.desktop.ui.icons.outlined.AlternateEmail
import com.pixelgram.desktop.ui.icons.outlined.AutoAwesome
import com.pixelgram.desktop.ui.icons.outlined.BatterySaver
import com.pixelgram.desktop.ui.icons.outlined.Campaign
import com.pixelgram.desktop.ui.icons.outlined.ChatBubbleOutline
import com.pixelgram.desktop.ui.icons.outlined.Devices
import com.pixelgram.desktop.ui.icons.outlined.Face
import com.pixelgram.desktop.ui.icons.outlined.RoundedCorner
import com.pixelgram.desktop.ui.icons.outlined.TextFields
import com.pixelgram.desktop.ui.icons.outlined.Folder
import com.pixelgram.desktop.ui.icons.outlined.Headphones
import com.pixelgram.desktop.ui.icons.outlined.Lock
import com.pixelgram.desktop.ui.icons.outlined.Notifications
import com.pixelgram.desktop.ui.icons.outlined.Numbers
import com.pixelgram.desktop.ui.icons.outlined.Palette
import com.pixelgram.desktop.ui.icons.outlined.Phone
import com.pixelgram.desktop.ui.icons.outlined.QrCode
import com.pixelgram.desktop.ui.icons.outlined.Search
import com.pixelgram.desktop.ui.icons.outlined.Translate
import com.pixelgram.desktop.ui.icons.outlined.Tune
import com.pixelgram.desktop.ui.components.expressive.AlertDialog
import com.pixelgram.desktop.ui.components.expressive.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import com.pixelgram.desktop.ui.components.expressive.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import com.pixelgram.desktop.ui.components.expressive.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import com.pixelgram.desktop.ui.components.expressive.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.components.expressive.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelgram.desktop.BuildConfig
import com.pixelgram.desktop.core.AppPaths
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.core.EmojiStyle
import com.pixelgram.desktop.core.NotificationStyle
import com.pixelgram.desktop.core.Sounds
import com.pixelgram.desktop.core.SystemNotifications
import com.pixelgram.desktop.core.ThemeMode
import com.pixelgram.desktop.core.ToastSide
import com.pixelgram.desktop.telegram.ChatListKey
import com.pixelgram.desktop.telegram.Notifications
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.telegram.UiMessage
import com.pixelgram.desktop.telegram.fullName
import com.pixelgram.desktop.ui.AvatarShape
import com.pixelgram.desktop.ui.auth.QrCode
import com.pixelgram.desktop.ui.EasterEgg
import com.pixelgram.desktop.ui.IsraelBlue
import com.pixelgram.desktop.ui.IsraelWhite
import com.pixelgram.desktop.ui.components.UsernameQrDialog
import com.pixelgram.desktop.ui.components.Avatar
import com.pixelgram.desktop.ui.components.AvatarImage
import com.pixelgram.desktop.ui.components.InfoGroup
import com.pixelgram.desktop.ui.components.InfoRow
import com.pixelgram.desktop.ui.components.ShapePreview
import com.pixelgram.desktop.ui.main.MessageBubble
import com.pixelgram.desktop.ui.main.formatChatDate
import com.pixelgram.desktop.ui.storage.MemoryContent
import com.pixelgram.desktop.ui.theme.EmojiText
import com.pixelgram.desktop.ui.theme.Matugen
import com.pixelgram.desktop.ui.theme.SeedColors
import com.pixelgram.desktop.ui.themedAppIcon
import com.pixelgram.desktop.ui.toShape
import it.tdlight.jni.TdApi
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

private enum class Section(val title: String, val icon: ImageVector, val pixel: Boolean = false, val parent: Section? = null) {
    Profile("My Profile", Icons.Outlined.Person),
    Notifications("Notifications and Sounds", Icons.Outlined.Notifications),
    Privacy("Privacy and Security", Icons.Outlined.Lock),
    ChatSettings("Chat Settings", Icons.Outlined.ChatBubbleOutline),
    Folders("Folders", Icons.Outlined.Folder),
    Advanced("Advanced", Icons.Outlined.Tune),
    Proxy("Proxy", Icons.Outlined.VpnKey),
    SpeakersCamera("Speakers and Camera", Icons.Outlined.Headphones),
    Battery("Battery and Animations", Icons.Outlined.BatterySaver),
    Language("Language", Icons.Outlined.Translate),
    General("General", Icons.Outlined.Settings, pixel = true),
    Appearance("Appearance", Icons.Outlined.Palette, pixel = true),
    Chats("Chats", Icons.AutoMirrored.Outlined.Chat, pixel = true),
    Window("Window and system", Icons.Outlined.Computer, pixel = true),
    Experimental("Experimental", Icons.Outlined.DataObject, pixel = true),
    Other("Other", Icons.Outlined.MoreHoriz, pixel = true),
    Fonts("Fonts", Icons.Outlined.TextFields, pixel = true, parent = Appearance),
    Avatars("Avatars", Icons.Outlined.Face, pixel = true, parent = Appearance),
    Bubbles("Message bubbles", Icons.Outlined.ChatBubbleOutline, pixel = true, parent = Appearance),
    Corners("Corners", Icons.Outlined.RoundedCorner, pixel = true, parent = Appearance),
    FolderStyle("Chat folders", Icons.Outlined.Folder, pixel = true, parent = Appearance),
    Navigation("Side menu and settings", Icons.Outlined.FormatListBulleted, pixel = true, parent = Appearance),
    Actions("Message actions", Icons.Outlined.SwipeRight, pixel = true, parent = Chats),
    Cursor("Cursor", Icons.Outlined.Keyboard, pixel = true, parent = Chats),
    About("About", Icons.Outlined.Info, pixel = true, parent = Other),
}

private val Section.subsections: List<Section> get() = Section.entries.filter { it.parent == this }

private val QrAvatarSize = 80.dp
private const val DisabledAlpha = 0.38f

private const val PixelPreferencesTitle = "pixelGram Preferences"
private const val HighlightHoldMs = 2_500L
private val SettingsMenuWidth = 220.dp

private fun searchSettings(raw: String): List<Pair<Section, String?>> {
    val query = raw.trim()
    if (query.isEmpty()) return emptyList()
    fun matches(text: String) = text.contains(query, ignoreCase = true) || com.pixelgram.desktop.core.tr(text).contains(query, ignoreCase = true)
    val sections = Section.entries.filter { matches(it.title) }.map { it to null as String? }
    val items = SettingsIndex.mapNotNull { (name, title) ->
        val target = Section.entries.firstOrNull { it.name == name } ?: return@mapNotNull null
        if (matches(title)) target to title else null
    }
    return (sections + items).distinct().take(SearchLimit)
}

private const val SearchLimit = 40

@Composable
private fun SearchResultRow(index: Int, count: Int, target: Section, title: String?, onClick: () -> Unit) {
    val path = buildList {
        add(com.pixelgram.desktop.core.tr("Settings"))
        if (target.pixel) add(com.pixelgram.desktop.core.tr(PixelPreferencesTitle))
        target.parent?.let { add(com.pixelgram.desktop.core.tr(it.title)) }
        if (title != null) add(com.pixelgram.desktop.core.tr(target.title))
    }.joinToString(" › ")
    androidx.compose.material3.SegmentedListItem(
        onClick = onClick,
        shapes = com.pixelgram.desktop.ui.components.groupShapes(index, count),
        leadingContent = { Icon(target.icon, contentDescription = null) },
        supportingContent = {
            androidx.compose.material3.Text(path, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        },
    ) {
        Text(title ?: target.title, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    }
}
private const val ChannelUsername = "pixelgramclient"
private const val ChannelUrl = "https://t.me/$ChannelUsername"
private const val GitHubUrl = "https://github.com/JettaXP/pixelGram"
private const val WebsiteHost = "pixelgram.jettax.xyz"

@Composable
fun SettingsScreen(
    telegram: Telegram,
    settings: AppSettings,
    startInPreferences: Boolean = false,
    onOpenProfile: () -> Unit,
    onOpenChat: (Long) -> Unit,
    onBack: () -> Unit,
    onAddAccount: (() -> Unit)? = null,
) {
    var section by rememberSaveable {
        val initial = System.getProperty("pixelgram.section")
        mutableStateOf(
            Section.entries.firstOrNull { it.name == initial }
                ?: if (startInPreferences) Section.General else Section.Profile
        )
    }
    var inPreferences by rememberSaveable { mutableStateOf(section.pixel) }
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    var highlight by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(highlight) {
        if (highlight != null) {
            kotlinx.coroutines.delay(HighlightHoldMs)
            highlight = null
        }
    }
    val openResult = { target: Section, title: String? ->
        inPreferences = target.pixel
        section = target
        highlight = title
        searching = false
        query = ""
    }
    OnEscape {
        val parent = section.parent
        if (searching) {
            searching = false
            query = ""
        } else if (parent != null) {
            section = parent
        } else if (inPreferences) {
            inPreferences = false
            section = Section.Profile
        } else {
            onBack()
        }
        true
    }
    val materialYou = settings.settingsStyle.value == com.pixelgram.desktop.core.SettingsStyle.MaterialYou
    Surface(color = if (materialYou) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxSize()) {
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
                            section = Section.Profile
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    val titleFx = effects<Float>()
                    AnimatedContent(
                        targetState = inPreferences,
                        transitionSpec = { fadeIn(titleFx) togetherWith fadeOut(titleFx) },
                        modifier = Modifier.weight(1f),
                    ) { preferences ->
                        Text(
                            if (preferences) PixelPreferencesTitle else "Settings",
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                    AnimatedVisibility(
                        visible = !inPreferences,
                        enter = fadeIn(effects()) + scaleIn(MaterialTheme.motionScheme.fastSpatialSpec()),
                        exit = fadeOut(effects()) + scaleOut(MaterialTheme.motionScheme.fastSpatialSpec()),
                    ) {
                      Row {
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
                            com.pixelgram.desktop.ui.components.GroupedMenu(expanded = menuOpen, onDismiss = { menuOpen = false }) {
                                com.pixelgram.desktop.ui.components.MenuGroups(
                                    listOf(
                                        listOfNotNull(
                                            onAddAccount?.let { add ->
                                                com.pixelgram.desktop.ui.components.MenuEntry(Icons.Outlined.PersonAdd, "Add Account") {
                                                    menuOpen = false
                                                    add()
                                                }
                                            },
                                            com.pixelgram.desktop.ui.components.MenuEntry(Icons.Outlined.Edit, "Edit profile") {
                                                menuOpen = false
                                                section = Section.Profile
                                            },
                                        ),
                                        listOf(
                                            com.pixelgram.desktop.ui.components.MenuEntry(
                                                Icons.AutoMirrored.Outlined.Logout,
                                                "Log out",
                                                MaterialTheme.colorScheme.error,
                                            ) {
                                                menuOpen = false
                                                telegram.logOut()
                                            },
                                        ),
                                    ),
                                    width = SettingsMenuWidth,
                                )
                            }
                        }
                      }
                    }
                }
                Spacer(Modifier.height(8.dp))
                val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
                val paneFx = effects<Float>()
                AnimatedContent(
                    targetState = inPreferences,
                    transitionSpec = {
                        val direction = if (targetState) 1 else -1
                        (slideInHorizontally(spatial) { it / 4 * direction } + fadeIn(paneFx)) togetherWith
                            (slideOutHorizontally(spatial) { -it / 4 * direction } + fadeOut(paneFx))
                    },
                ) { preferencesPane ->
                  Column {
                if (preferencesPane) {
                    PreferencesHeader(telegram)
                    Header("Categories")
                    val items = Section.entries.filter { it.pixel && it.parent == null }
                    SegmentedGroup {
                        items.forEachIndexed { index, item ->
                            NavItem(index, items.size, item.title, item.icon, item == section || item == section.parent, settings.settingsStyle.value == com.pixelgram.desktop.core.SettingsStyle.MaterialYou) {
                                section = item
                            }
                        }
                    }
                } else {
                    val searchFocus = remember { androidx.compose.ui.focus.FocusRequester() }
                    LaunchedEffect(searching) {
                        if (searching) {
                            withFrameNanos { }
                            runCatching { searchFocus.requestFocus() }
                        }
                    }
                    Reveal(searching) {
                        OutlinedTextField(
                            value = query,
                            visualTransformation = EmojiVisualTransformation,
                            onValueChange = { query = it },
                            label = { Text("Search settings") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).focusRequester(searchFocus),
                        )
                    }
                    val items = if (query.isBlank()) Section.entries.filter { !it.pixel && it != Section.Profile } else emptyList()
                    val results = remember(query, com.pixelgram.desktop.core.I18n.code) { searchSettings(query) }
                    if (query.isNotBlank()) {
                        if (results.isEmpty()) {
                            Text("No results", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(8.dp))
                        }
                        SegmentedGroup {
                            results.forEachIndexed { index, (target, title) ->
                                SearchResultRow(index, results.size, target, title) { openResult(target, title) }
                            }
                        }
                    }
                    Reveal(query.isBlank()) {
                        Column {
                            ProfileCard(telegram, settings, selected = section == Section.Profile) { section = Section.Profile }
                            Spacer(Modifier.height(12.dp))
                            PreferencesEntry {
                                inPreferences = true
                                section = Section.General
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                        modifier = Modifier.animateContentSize(MaterialTheme.motionScheme.defaultSpatialSpec()),
                    ) {
                        items.forEachIndexed { index, item ->
                            key(item) {
                                NavItem(index, items.size, item.title, item.icon, item == section, settings.settingsStyle.value == com.pixelgram.desktop.core.SettingsStyle.MaterialYou) {
                                    section = item
                                }
                            }
                        }
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
                color = if (materialYou) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surface,
            ) {
                val sectionSpatial = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
                val sectionIn = effects<Float>()
                val sectionOut = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
                AnimatedContent(
                    targetState = section,
                    transitionSpec = {
                        val direction = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                        (slideInVertically(sectionSpatial) { it / 12 * direction } + fadeIn(sectionIn)) togetherWith
                            fadeOut(sectionOut)
                    },
                ) { shown ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp)
                        .animateContentSize(MaterialTheme.motionScheme.defaultSpatialSpec()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (shown.pixel) {
                        Text(
                            listOfNotNull(PixelPreferencesTitle, shown.parent?.title).joinToString(" › ") { com.pixelgram.desktop.core.tr(it) },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        shown.parent?.let { parent ->
                            IconButton(onClick = { section = parent }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                        Text(shown.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                        if (shown == Section.Profile) AccountMenuButton(telegram, onAddAccount)
                    }
                    Spacer(Modifier.height(8.dp))
                    androidx.compose.runtime.CompositionLocalProvider(com.pixelgram.desktop.ui.components.LocalHighlightText provides highlight) {
                    when (shown) {
                        Section.General -> GeneralSection(settings)
                        Section.Appearance -> AppearanceSection(settings) { section = it }
                        Section.Chats -> ChatsSection(settings) { section = it }
                        Section.Window -> WindowSection(settings)
                        Section.Experimental -> ExperimentalSection(settings)
                        Section.Other -> OtherSection(telegram, settings) { section = it }
                        Section.Fonts -> FontsSection(settings)
                        Section.FolderStyle -> FolderStyleSection(settings)
                        Section.Navigation -> NavigationSection(settings)
                        Section.Actions -> ActionsSection(settings)
                        Section.Avatars -> AvatarsSection(settings)
                        Section.Bubbles -> BubblesSection(settings)
                        Section.Corners -> CornersSection(settings)
                        Section.Cursor -> CursorSection(settings)
                        Section.About -> AboutSection(telegram, onOpenChat)
                        Section.Privacy -> PrivacySection(telegram, settings)
                        Section.ChatSettings -> ChatSettingsSection(settings)
                        Section.Folders -> FoldersSection(telegram, settings)
                        Section.Advanced -> AdvancedSection(telegram, settings)
                        Section.Proxy -> ProxySection(telegram, settings)
                        Section.Language -> LanguageSection(settings)
                        Section.Profile -> com.pixelgram.desktop.ui.profile.ProfileContent(telegram, settings, onOpenChat)
                        Section.Notifications -> NotificationsSection(telegram, settings)
                        Section.Battery -> BatterySection(settings)
                        Section.SpeakersCamera -> DevicesSection(settings)
                    }
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun ProfileCard(telegram: Telegram, settings: AppSettings, selected: Boolean, onClick: () -> Unit) {
    val me = telegram.me ?: return
    val username = me.usernames?.activeUsernames?.firstOrNull()
    var showQr by remember { mutableStateOf(false) }
    val container by androidx.compose.animation.animateColorAsState(
        if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
    )
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        color = container,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Avatar(me.fullName(), me.profilePhoto?.small, telegram, settings, size = 64.dp)
            Column(Modifier.weight(1f)) {
                EmojiText(me.fullName(), style = MaterialTheme.typography.titleLargeEmphasized, maxLines = 1)
                me.phoneNumber?.takeIf { it.isNotEmpty() }?.let {
                    PhoneText(it, style = MaterialTheme.typography.bodyMedium)
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
        UsernameQrDialog(username, me.fullName(), me.profilePhoto, telegram) { showQr = false }
    }
}

@Composable
private fun PreferencesHeader(telegram: Telegram) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    ) {
        themedAppIcon()?.let { Image(it, contentDescription = null, modifier = Modifier.size(96.dp)) }
        Text("pixelGram Desktop", style = MaterialTheme.typography.titleLargeEmphasized)
        Surface(
            onClick = { EasterEgg.tap(telegram) },
            shape = CircleShape,
            color = if (EasterEgg.israel) IsraelWhite else MaterialTheme.colorScheme.secondaryContainer,
            contentColor = if (EasterEgg.israel) IsraelBlue else MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(top = 6.dp),
        ) {
            Text(
                listOf("v${BuildConfig.VERSION}", BuildConfig.CODENAME).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
internal fun SegmentedGroup(content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        content()
    }
}

@Composable
private fun AccountMenuButton(telegram: Telegram, onAddAccount: (() -> Unit)?) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "More")
        }
        com.pixelgram.desktop.ui.components.GroupedMenu(expanded = open, onDismiss = { open = false }) {
            com.pixelgram.desktop.ui.components.MenuGroups(
                listOf(
                    listOfNotNull(
                        onAddAccount?.let { add ->
                            com.pixelgram.desktop.ui.components.MenuEntry(Icons.Outlined.PersonAdd, "Add Account") {
                                open = false
                                add()
                            }
                        },
                    ),
                    listOf(
                        com.pixelgram.desktop.ui.components.MenuEntry(Icons.AutoMirrored.Outlined.Logout, "Log out", MaterialTheme.colorScheme.error) {
                            open = false
                            telegram.logOut()
                        },
                    ),
                ),
                width = SettingsMenuWidth,
            )
        }
    }
}

@Composable
private fun NavItem(
    index: Int,
    count: Int,
    title: String,
    icon: ImageVector,
    selected: Boolean,
    flat: Boolean,
    onClick: () -> Unit,
) {
    if (!flat) {
        SegmentedListItem(
            selected = selected,
            onClick = onClick,
            shapes = groupShapes(index, count),
            leadingContent = { Icon(icon, contentDescription = null) },
        ) {
            Text(title)
        }
        return
    }
    val colors = MaterialTheme.colorScheme
    val background by androidx.compose.animation.animateColorAsState(
        if (selected) colors.primary else Color.Transparent,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
    )
    val content = if (selected) colors.onPrimary else colors.onSurface
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(FlatNavHeight)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) colors.onPrimary else colors.onSurfaceVariant)
        Text(title, style = MaterialTheme.typography.titleMedium, color = content, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    }
}

private val FlatNavHeight = 56.dp

@Composable
private fun SettingsStyleChoice(settings: AppSettings) {
    ConnectedChoice(
        listOf(com.pixelgram.desktop.core.SettingsStyle.Expressive to "Expressive", com.pixelgram.desktop.core.SettingsStyle.MaterialYou to "Material You"),
        settings.settingsStyle.value,
        { settings.settingsStyle.value = it },
        translate = false,
    )
}

@Composable
fun PixelSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    interactionSource: MutableInteractionSource? = null,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        interactionSource = interactionSource,
        thumbContent = {
            Icon(
                imageVector = if (checked) Icons.Filled.Check else Icons.Filled.Close,
                contentDescription = null,
                modifier = Modifier.size(SwitchDefaults.IconSize),
            )
        },
    )
}

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
internal fun Header(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}

@Composable
internal fun <T> effects() = MaterialTheme.motionScheme.defaultEffectsSpec<T>()

@Composable
internal fun Reveal(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeIn(effects()),
        exit = shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeOut(effects()),
    ) {
        content()
    }
}

@Composable
internal fun AnimatedDescription(text: String) {
    val fx = effects<Float>()
    val size = MaterialTheme.motionScheme.defaultSpatialSpec<IntSize>()
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            fadeIn(fx) togetherWith fadeOut(fx) using SizeTransform(sizeAnimationSpec = { _, _ -> size })
        },
    ) {
        Description(it)
    }
}

@Composable
internal fun Description(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
internal fun SwitchRow(
    index: Int,
    count: Int,
    title: String,
    subtitle: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    SegmentedListItem(
        checked = checked,
        onCheckedChange = onChange,
        shapes = groupShapes(index, count),
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = {
            PixelSwitch(checked = checked, onCheckedChange = null, interactionSource = interaction)
        },
        interactionSource = interaction,
    ) {
        Text(title)
    }
}

@Composable
private fun GeneralSection(settings: AppSettings) {
    Header("Messages")
    SegmentedGroup {
        SwitchRow(
            index = 0,
            count = 2,
            title = "Seconds in message time",
            subtitle = "Show 18:52:33 instead of 18:52 under messages.",
            checked = settings.messageSeconds.value,
            onChange = { settings.messageSeconds.value = it },
        )
        SwitchRow(
            index = 1,
            count = 2,
            title = "Disable number rounding",
            subtitle = "Show 1,234 views instead of 1.2K.",
            checked = settings.exactNumbers.value,
            onChange = { settings.exactNumbers.value = it },
        )
    }
    Header("Text")
    SwitchRow(
        index = 0,
        count = 1,
        title = "Filter Zalgo",
        subtitle = "Removes stacked Zalgo symbols from names and messages.",
        checked = settings.zalgoFilter.value,
        onChange = { settings.zalgoFilter.value = it },
    )
    Header("Privacy")
    SwitchRow(
        index = 0,
        count = 1,
        title = "Hide my phone number",
        subtitle = "Cover your number with Material shapes in your profile and settings. Click it to peek.",
        checked = settings.hidePhone.value,
        onChange = { settings.hidePhone.value = it },
    )
    Header("Mini apps")
    SwitchRow(0, 1, "Spoof client as Android", "Mini apps see pixelGram as Telegram for Android. Some apps unlock mobile-only features.", settings.miniAppsAndroid.value) {
        settings.miniAppsAndroid.value = it
    }
}

@Composable
private fun AppearanceSection(settings: AppSettings, onOpen: (Section) -> Unit) {
    Header("Icons")
    ConnectedChoice(
        listOf(
            com.pixelgram.desktop.core.IconPack.Material to "Material",
            com.pixelgram.desktop.core.IconPack.Solar to "Solar",
            com.pixelgram.desktop.core.IconPack.Telegram to "Telegram",
        ),
        settings.iconPack.value,
        { settings.iconPack.value = it },
    )
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(vertical = 4.dp)) {
        listOf(
            Icons.Outlined.Search, Icons.Outlined.Settings, Icons.AutoMirrored.Outlined.Reply, Icons.Outlined.ContentCopy,
            Icons.Outlined.PushPin, Icons.Outlined.Delete, Icons.Outlined.Notifications, Icons.Outlined.Lock,
        ).forEach { Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
    AnimatedDescription(
        when (settings.iconPack.value) {
            com.pixelgram.desktop.core.IconPack.Material -> "Material Symbols by Google."
            com.pixelgram.desktop.core.IconPack.Solar -> "Solar Icon Set by 480 Design."
            com.pixelgram.desktop.core.IconPack.Telegram -> "Icons from Telegram Desktop."
        },
    )
    Header("Emoji")
    EmojiStyleChoice(settings)
    Header("Chat list title")
    Description("The text above the chat list. Your username updates when you change it.")
    ConnectedChoice(
        listOf(
            com.pixelgram.desktop.core.TitleText.App to "pixelGram",
            com.pixelgram.desktop.core.TitleText.Username to "Username",
            com.pixelgram.desktop.core.TitleText.Custom to "Custom text",
        ),
        settings.titleText.value,
        { settings.titleText.value = it },
    )
    Reveal(settings.titleText.value == com.pixelgram.desktop.core.TitleText.Custom) {
        OutlinedTextField(
            value = settings.titleCustom.value,
            visualTransformation = EmojiVisualTransformation,
            onValueChange = { settings.titleCustom.value = it.replace("\n", "").take(TitleLimit) },
            label = { Text("Title text") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    SubsectionLinks(Section.Appearance, onOpen)
}

@Composable
private fun ColorsSection(settings: AppSettings) {
    Header("Your colors")
    Description("Pick a style. Expressive, Vibrant and the others are the bright Material 3 Expressive schemes from Google's Material color utilities.")
    ColorStyleRow(settings)

    Header("Select a color")
    Description("Slide to pick any hue, or use one of the presets below.")
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) { HueSlider(settings) }
        ResetButton(settings.seedColor)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 8.dp)) {
        SeedColors.forEach { seed ->
            val selected = settings.seedColor.value == seed
            val ring by animateDpAsState(if (selected) 3.dp else 0.dp, MaterialTheme.motionScheme.fastSpatialSpec())
            val scale by animateFloatAsState(if (selected) 1f else 0.88f, MaterialTheme.motionScheme.fastSpatialSpec())
            Box(
                Modifier
                    .size(40.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(CircleShape)
                    .background(Color(seed))
                    .then(
                        if (ring > 0.dp) {
                            Modifier.border(ring, MaterialTheme.colorScheme.onSurface, CircleShape)
                        } else {
                            Modifier
                        }
                    )
                    .graphicsLayer { alpha = if (settings.israelLogo.value) DisabledAlpha else 1f }
                    .clickable(enabled = !settings.israelLogo.value) { settings.seedColor.value = seed },
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
                if (!settings.israelLogo.value) {
                    settings.matugen.value = it
                    if (it) Matugen.exportTemplate()
                }
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
}

@Composable
private fun NavigationSection(settings: AppSettings) {
    Header("Side menu")
    Description("Over the chats keeps the window still and dims it. Push aside moves the chats to make room for the menu.")
    ConnectedChoice(
        listOf(com.pixelgram.desktop.core.SideMenuStyle.Overlay to "Over the chats", com.pixelgram.desktop.core.SideMenuStyle.Push to "Push aside"),
        settings.sideMenuStyle.value,
        { settings.sideMenuStyle.value = it },
    )

    Header("Settings style")
    Description("Expressive groups the sections into segmented cards. Material You shows a flat list with a highlighted pill, like Android settings.")
    SettingsStyleChoice(settings)
}

@Composable
private fun FolderStyleSection(settings: AppSettings) {
    Header("Folder title")
    ConnectedChoice(
        listOf(
            com.pixelgram.desktop.core.FolderTitle.NamesAndIcons to "Names with icons",
            com.pixelgram.desktop.core.FolderTitle.Names to "Names only",
            com.pixelgram.desktop.core.FolderTitle.Icons to "Icons only",
        ),
        settings.folderTitle.value,
        { settings.folderTitle.value = it },
    )
    Reveal(settings.folderTitle.value == com.pixelgram.desktop.core.FolderTitle.Icons) {
        Description("The title above the chat list shows the name of the open folder.")
    }
    Header("Chat folders")
    Text("Position", style = MaterialTheme.typography.bodyLarge)
    val capsule = settings.capsuleFolders.value
    ConnectedChoice(
        options = listOf(true to "Left", false to "Top"),
        selected = settings.materialFolders.value && !capsule,
        onSelect = { settings.materialFolders.value = it },
        enabled = { !(it && capsule) },
    )
    AnimatedDescription(
        if (capsule) "The capsule is always on top." else
            "Left is a Material 3 navigation rail, top is a bar above the chat list."
    )
    Text("Style on top", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
    ConnectedChoice(
        options = listOf(false to "Tabs", true to "Capsule"),
        selected = settings.capsuleFolders.value,
        onSelect = {
            settings.capsuleFolders.value = it
            if (it) settings.materialFolders.value = false
        },
    )

    Header("Capsules")
    Text("Spacing", style = MaterialTheme.typography.bodyLarge)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Slider(
            value = settings.capsuleSpacing.value.toFloat(),
            onValueChange = { settings.capsuleSpacing.value = it.toInt() },
            valueRange = 0f..16f,
            steps = 15,
            modifier = Modifier.weight(1f),
        )
        Text("${settings.capsuleSpacing.value} dp", modifier = Modifier.width(64.dp).padding(start = 12.dp))
        ResetButton(settings.capsuleSpacing)
    }
    Text("Height", style = MaterialTheme.typography.bodyLarge)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Slider(
            value = settings.capsuleHeight.value.toFloat(),
            onValueChange = { settings.capsuleHeight.value = it.toInt() },
            valueRange = 56f..80f,
            steps = 23,
            modifier = Modifier.weight(1f),
        )
        Text("${settings.capsuleHeight.value} dp", modifier = Modifier.width(64.dp).padding(start = 12.dp))
        ResetButton(settings.capsuleHeight)
    }
}

@Composable
private fun SubsectionLinks(parent: Section, onOpen: (Section) -> Unit) {
    val items = parent.subsections
    Header("More")
    SegmentedGroup {
        items.forEachIndexed { index, item ->
            SegmentedListItem(
                onClick = { onOpen(item) },
                shapes = groupShapes(index, items.size),
                leadingContent = { Icon(item.icon, contentDescription = null) },
                trailingContent = { Icon(Icons.Outlined.ArrowForward, contentDescription = null) },
            ) {
                Text(item.title)
            }
        }
    }
}

@Composable
private fun EmojiStyleChoice(settings: AppSettings) {
    ConnectedChoice(
        options = listOf(EmojiStyle.Noto to "Noto", EmojiStyle.Noto3D to "Noto 3D", EmojiStyle.Apple to "Apple"),
        selected = settings.emojiStyle.value,
        onSelect = { settings.emojiStyle.value = it },
    )
    EmojiText("😀 😍 🥳 🫠 🤔 👍 🎉 ❤️ 🔥 🐱 🍕 🚀", fontSize = 30.sp, modifier = Modifier.padding(vertical = 4.dp))
    AnimatedDescription(
        when (settings.emojiStyle.value) {
            EmojiStyle.Noto -> "Google's latest emoji set."
            EmojiStyle.Noto3D -> "Google's new 3D emoji style from Android 17."
            EmojiStyle.Apple -> "Apple's emoji set from iOS (Emoji 16)."
        },
    )
}

@Composable
private fun ThemeModeButtons(settings: AppSettings) = ConnectedChoice(
    options = ThemeMode.entries.map { it to it.name },
    selected = if (settings.israelLogo.value) ThemeMode.Light else settings.themeMode.value,
    onSelect = { if (!settings.israelLogo.value) settings.themeMode.value = it },
)

@Composable
internal fun <T> ConnectedChoice(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    enabled: (T) -> Boolean = { true },
    translate: Boolean = true,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
        options.forEachIndexed { index, (value, label) ->
            ToggleButton(
                checked = selected == value,
                onCheckedChange = { onSelect(value) },
                enabled = enabled(value),
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
            ) {
                if (translate) Text(label) else androidx.compose.material3.Text(label)
            }
        }
    }
}

@Composable
private fun AvatarsSection(settings: AppSettings) {
    var kind by remember { mutableStateOf<AvatarKind?>(null) }
    val current = settings.avatarShapeFor(kind)
    val corner = settings.avatarCorner.value
    val pick = { shape: AvatarShape ->
        val target = kind
        if (target == null) settings.avatarShape.value = shape else settings.avatarKindShapes.getValue(target).value = shape.name
    }

    Header("Chat type")
    Description("Pick a shape for all chats, or a separate one for each kind of chat.")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = kind == null, onClick = { kind = null }, label = { Text("All chats") })
        AvatarKind.entries.forEach { entry ->
            FilterChip(
                selected = kind == entry,
                onClick = { kind = entry },
                label = { Text(entry.title) },
                leadingIcon = settings.kindShape(entry)?.let { shape ->
                    { ShapePreview(shape, corner, 16.dp, selected = true) }
                },
            )
        }
    }
    Spacer(Modifier.height(8.dp))

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        ShapePreview(current, corner, 96.dp, selected = true)
        Column {
            Text(current.title, style = MaterialTheme.typography.titleMedium)
            val override = kind?.let(settings::kindShape)
            Description(
                when {
                    kind == null -> "Official Material 3 Expressive shapes."
                    override == null -> "Same as all chats."
                    else -> "Only for ${kind?.title?.lowercase()}."
                }
            )
            Reveal(override != null) {
                TextButton(onClick = { kind?.let { settings.avatarKindShapes.getValue(it).value = "" } }) {
                    Text("Use the shape for all chats")
                }
            }
        }
    }

    Header("Shape")
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AvatarShape.entries.forEach { shape ->
            val selected = shape == current
            val tile by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                MaterialTheme.motionScheme.defaultEffectsSpec(),
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(88.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(tile)
                    .clickable { pick(shape) }
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
        ResetButton(settings.avatarCorner)
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
        ResetButton(settings.bubbleRadius)
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
        ResetButton(settings.bubbleTailRadius)
    }
}

@Composable
private fun CornersSection(settings: AppSettings) {
    Description("Pick how round each kind of element is, everywhere in pixelGram.")
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        listOf("A list row", "Another one").forEachIndexed { index, title ->
            Surface(
                shape = groupShapes(index, 2).shape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Icon(if (index == 0) Icons.Outlined.RoundedCorner else Icons.Outlined.Palette, contentDescription = null)
                    Text(title)
                }
            }
        }
    }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        Text("A card", modifier = Modifier.padding(20.dp), style = MaterialTheme.typography.titleMedium)
    }
    CornerSlider("Lists", "Grouped rows in settings, profiles and lists.", settings.cornerLists, 0..32)
    CornerSlider("Cards", "Panels, cards and previews.", settings.cornerCards, 0..40)
    CornerSlider("Dialogs and bars", "Dialogs, the message bar, pop-ups and sheets.", settings.cornerDialogs, 0..48)
    CornerSlider("Menus and fields", "Context menus and text fields.", settings.cornerMenus, 0..28)
    TextButton(
        onClick = {
            settings.cornerLists.value = 20
            settings.cornerCards.value = 24
            settings.cornerDialogs.value = 28
            settings.cornerMenus.value = 16
        },
        modifier = Modifier.padding(top = 8.dp),
    ) { Text("Reset to default") }
}

@Composable
private fun ResetButton(setting: com.pixelgram.desktop.core.Setting<Int>) {
    IconButton(onClick = { setting.reset() }, enabled = setting.value != setting.default) {
        Icon(Icons.Outlined.RestartAlt, contentDescription = "Reset to default")
    }
}

@Composable
private fun CornerSlider(title: String, description: String, setting: com.pixelgram.desktop.core.Setting<Int>, range: IntRange) {
    Header(title)
    Description(description)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Slider(
            value = setting.value.toFloat(),
            onValueChange = { setting.value = it.toInt() },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            modifier = Modifier.weight(1f),
        )
        Text("${setting.value} dp", modifier = Modifier.width(64.dp).padding(start = 12.dp))
        ResetButton(setting)
    }
}

@Composable
private fun CursorSection(settings: AppSettings) {
    CursorSettings(settings)
}

@Composable
private fun ChatSettingsSection(settings: AppSettings) {
    WallpaperSettings(settings)
    Header("Theme")
    ThemeModeButtons(settings)
    ColorsSection(settings)
}

@Composable
private fun FontsSection(settings: AppSettings) {
    MessageFontSettings(settings)
}

@Composable
private fun ChatsSection(settings: AppSettings, onOpen: (Section) -> Unit) {
    Header("Reactions")
    SwitchRow(
        0,
        1,
        "Always big reactions",
        "Every reaction you set plays the big effect, like a long press.",
        settings.bigReactions.value,
    ) { settings.bigReactions.value = it }
    Header("Empty chat area")
    SwitchRow(
        0,
        1,
        "Material shapes",
        "Shapes behind the hint when no chat is open.",
        settings.emptyShapes.value,
    ) { settings.emptyShapes.value = it }
    Header("Typing")
    SwitchRow(
        0,
        1,
        "Show typing as a message bubble",
        "When off, only the chat header says that someone is typing.",
        settings.typingBubble.value,
    ) { settings.typingBubble.value = it }
    Header("Input")
    SegmentedGroup {
        SwitchRow(
            0,
            2,
            "Capitalize sentences",
            "The first letter of each sentence becomes a capital letter as you type.",
            settings.autoCapitalize.value,
        ) { settings.autoCapitalize.value = it }
        SwitchRow(
            1,
            2,
            "Calculator in the message field",
            "Type an example ending with =, like 5+5=, to see the answer in gray. Press → to insert it.",
            settings.inlineMath.value,
        ) { settings.inlineMath.value = it }
    }
    SubsectionLinks(Section.Chats, onOpen)
}

@Composable
private fun ActionsSection(settings: AppSettings) {
    Header("Double click on a message")
    val options = listOf(
        DoubleClickAction.Reaction to "Reaction",
        DoubleClickAction.Reply to "Reply",
        DoubleClickAction.Nothing to "Nothing",
    )
    val ownOptions = options.take(2) + (DoubleClickAction.Edit to "Edit") + options.drop(2)
    Text("Your messages", style = MaterialTheme.typography.bodyLarge)
    ConnectedChoice(ownOptions, settings.doubleClickOwn.value, { settings.doubleClickOwn.value = it })
    Text("Other people's messages", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
    ConnectedChoice(options, settings.doubleClickOther.value, { settings.doubleClickOther.value = it })
    Header("Swipe actions")
    Description("Hold the right mouse button on a message and drag it sideways, or swipe sideways on a touchpad. A right click without dragging still opens the menu.")
    val swipeOptions = listOf(
        SwipeAction.Reply to "Reply",
        SwipeAction.React to "Reaction",
        SwipeAction.Pin to "Pin",
        SwipeAction.Forward to "Forward",
        SwipeAction.Copy to "Copy",
        SwipeAction.Select to "Select",
        SwipeAction.Off to "Off",
    )
    SegmentedGroup {
        SwipeActionRow(Icons.Outlined.SwipeLeft, "Swipe left", settings.swipeLeft, swipeOptions, 0, 2)
        SwipeActionRow(Icons.Outlined.SwipeRight, "Swipe right", settings.swipeRight, swipeOptions, 1, 2)
    }
    val quickReaction = DoubleClickAction.Reaction in listOf(settings.doubleClickOwn.value, settings.doubleClickOther.value) ||
        SwipeAction.React in listOf(settings.swipeLeft.value, settings.swipeRight.value)
    Reveal(quickReaction) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Reaction", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
            DoubleClickEmoji(settings)
        }
    }
}

@Composable
private fun SwipeActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    setting: com.pixelgram.desktop.core.Setting<SwipeAction>,
    options: List<Pair<SwipeAction, String>>,
    index: Int,
    count: Int,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        SegmentedListItem(
            onClick = { open = true },
            shapes = groupShapes(index, count),
            leadingContent = { Icon(icon, contentDescription = null) },
            trailingContent = {
                val enter = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
                val exit = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
                AnimatedContent(
                    targetState = setting.value,
                    transitionSpec = { fadeIn(enter) togetherWith fadeOut(exit) },
                ) { value ->
                    Text(
                        options.firstOrNull { it.first == value }?.second.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            },
        ) { Text(title) }
        Box(Modifier.align(Alignment.TopEnd)) {
            com.pixelgram.desktop.ui.components.GroupedMenu(expanded = open, onDismiss = { open = false }) {
                com.pixelgram.desktop.ui.components.MenuGroups(
                    listOf(
                        options.map { (value, label) ->
                            com.pixelgram.desktop.ui.components.MenuEntry(
                                value.icon,
                                label,
                                trailing = if (value == setting.value) Icons.Filled.Check else null,
                            ) {
                                setting.value = value
                                open = false
                            }
                        },
                    ),
                    width = SettingsMenuWidth,
                )
            }
        }
    }
}

@Composable
private fun WallpaperSettings(settings: AppSettings) {
    val scope = rememberCoroutineScope()
    Header("Wallpaper")
    Description("Shown behind every chat, except chats that have their own background in Telegram.")
    val path = settings.wallpaper.value
    val recent = remember(path) { com.pixelgram.desktop.ui.main.Wallpapers.recent() }
    if (recent.isEmpty()) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier.fillMaxWidth().height(WallpaperPreviewHeight),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("No wallpaper", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        val carousel = androidx.compose.material3.carousel.rememberCarouselState { recent.size }
        androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel(
            state = carousel,
            preferredItemWidth = WallpaperItemWidth,
            itemSpacing = 8.dp,
            modifier = Modifier.fillMaxWidth().height(WallpaperPreviewHeight),
        ) { index ->
            val file = recent[index]
            val selected = file.absolutePath == path
            Box(
                Modifier
                    .fillMaxSize()
                    .maskClip(MaterialTheme.shapes.extraLarge)
                    .clickable { scope.launch { com.pixelgram.desktop.ui.main.Wallpapers.use(settings, file) } },
                contentAlignment = Alignment.Center,
            ) {
                com.pixelgram.desktop.ui.main.FileImage(com.pixelgram.desktop.ui.main.Wallpapers.poster(file.absolutePath), Modifier.fillMaxSize(), androidx.compose.ui.layout.ContentScale.Crop)
                if (com.pixelgram.desktop.ui.main.Wallpapers.isVideo(file.absolutePath)) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.45f),
                        contentColor = Color.White,
                        modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
                    ) {
                        Text("Live", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                }
                androidx.compose.animation.AnimatedVisibility(
                    selected,
                    enter = androidx.compose.animation.scaleIn() + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.scaleOut() + androidx.compose.animation.fadeOut(),
                ) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(56.dp)) {
                        Icon(Icons.Filled.Check, contentDescription = "Current", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(14.dp))
                    }
                }
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
        Button(onClick = {
            scope.launch { com.pixelgram.desktop.ui.main.Wallpapers.pick()?.let { com.pixelgram.desktop.ui.main.Wallpapers.install(settings, it) } }
        }) { Text("Add image or video") }
        if (path.isNotBlank()) {
            OutlinedButton(onClick = { com.pixelgram.desktop.ui.main.Wallpapers.remove(settings) }) { Text("No wallpaper") }
        }
    }
    if (path.isNotBlank()) {
        Text("Dimming in dark theme", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Slider(
                value = settings.wallpaperDim.value.toFloat(),
                onValueChange = { settings.wallpaperDim.value = it.toInt() },
                valueRange = 0f..80f,
                modifier = Modifier.weight(1f),
            )
            Text("${settings.wallpaperDim.value}%", modifier = Modifier.width(64.dp).padding(start = 12.dp))
            ResetButton(settings.wallpaperDim)
        }
    }
    SwitchRow(
        index = 0,
        count = 1,
        title = "Colors from wallpaper",
        subtitle = "Pick the accent color from the wallpaper with Google's Material color utilities, the same way Android does.",
        checked = settings.wallpaperColors.value,
        onChange = {
            settings.wallpaperColors.value = it
            if (it) scope.launch { com.pixelgram.desktop.ui.main.Wallpapers.applyColors(settings) }
        },
    )
}

private val WallpaperPreviewHeight = 180.dp
private val WallpaperItemWidth = 260.dp

@Composable
private fun MessageFontSettings(settings: AppSettings) {
    val scope = rememberCoroutineScope()
    var choosing by remember { mutableStateOf(false) }
    Header("Message font")
    Description("The font and size of message text. Pick an installed font or load a .ttf or .otf file.")
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            MessageBubble(UiMessage(1, 0, 0, 0, false, 1_700_000_000, "The quick brown fox jumps over the lazy dog"), senderName = null, settings = settings)
            MessageBubble(UiMessage(2, 0, 0, 0, true, 1_700_000_060, "Съешь же ещё этих мягких французских булок 🥐"), senderName = null, settings = settings)
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
        Button(onClick = { choosing = true }) { Text(com.pixelgram.desktop.ui.theme.MessageFonts.label(settings.messageFont.value)) }
        OutlinedButton(onClick = {
            scope.launch {
                val file = runCatching {
                    io.github.vinceglb.filekit.FileKit.openFilePicker(
                        type = io.github.vinceglb.filekit.dialogs.FileKitType.File(listOf("ttf", "otf")),
                    )?.file
                }.getOrNull() ?: return@launch
                val dir = java.io.File(com.pixelgram.desktop.core.AppPaths.dataDir, "fonts/${file.nameWithoutExtension}").apply { mkdirs() }
                runCatching {
                    com.pixelgram.desktop.ui.theme.MessageFonts.siblings(file).forEach { it.copyTo(dir.resolve(it.name), overwrite = true) }
                }.onSuccess {
                    settings.messageFont.value = com.pixelgram.desktop.ui.theme.MessageFonts.FilePrefix + dir.resolve(file.name).absolutePath
                }
            }
        }) { Text("From file") }
        if (settings.messageFont.value.isNotBlank()) {
            TextButton(onClick = { settings.messageFont.value = "" }) { Text("Default") }
        }
    }
    SwitchRow(
        index = 0,
        count = 1,
        title = "Use this font everywhere",
        subtitle = "Menus, titles, settings and the chat list use it too, not only messages.",
        checked = settings.fontEverywhere.value,
        onChange = { settings.fontEverywhere.value = it },
    )
    Text("Size", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Slider(
            value = settings.messageFontSize.value.toFloat(),
            onValueChange = { settings.messageFontSize.value = it.toInt() },
            valueRange = 12f..24f,
            modifier = Modifier.weight(1f),
        )
        Text("${settings.messageFontSize.value} sp", modifier = Modifier.width(64.dp).padding(start = 12.dp))
        ResetButton(settings.messageFontSize)
    }
    if (choosing) FontPickerDialog(settings) { choosing = false }
}

@Composable
private fun CursorSettings(settings: AppSettings) {
    Header("Cursor")
    Description("The text cursor in the message field.")
    val custom = settings.liquidCursor.value
    val kitty = settings.cursorStyle.value == com.pixelgram.desktop.core.CursorStyle.Kitty
    SwitchRow(0, 1, "Animated cursor", "pixelGram draws its own animated cursor.", custom) { settings.liquidCursor.value = it }
    Reveal(custom) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Cursor style", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
            ConnectedChoice(
                listOf(com.pixelgram.desktop.core.CursorStyle.Liquid to "Liquid", com.pixelgram.desktop.core.CursorStyle.Kitty to "Kitty trail"),
                settings.cursorStyle.value,
                { settings.cursorStyle.value = it },
            )
            Description(if (kitty) "A trail follows the cursor on big jumps, the same way the kitty terminal does it." else "The cursor stretches like a drop when it moves.")
        }
    }
    Reveal(custom && !kitty) {
        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
            SwitchRow(0, 2, "Smooth cursor", "The cursor glides to its new place instead of jumping.", settings.smoothCursor.value) { settings.smoothCursor.value = it }
            SwitchRow(1, 2, "Cursor blinking", null, settings.cursorBlink.value) { settings.cursorBlink.value = it }
        }
    }
    Reveal(custom && kitty) {
        SwitchRow(0, 1, "Cursor blinking", null, settings.cursorBlink.value) { settings.cursorBlink.value = it }
    }
    Reveal(custom && !kitty) {
        Column {
            CursorSlider("Cursor stretch", settings.cursorStretch, 0..100, "")
            CursorSlider("Handle stretch", settings.handleStretch, 0..100, "%")
            CursorSlider("Side pull", settings.sidePull, 0..100, "%")
            CursorSlider("Cursor speed", settings.cursorSpeed, 1..100, "")
        }
    }
    Reveal(custom && kitty) {
        Column {
            CursorSlider("Trail front", settings.trailFast, 20..500, " ms", step = 10)
            CursorSlider("Trail end", settings.trailSlow, 50..1000, " ms", step = 10)
            CursorSlider("Trail start distance", settings.trailThreshold, 0..10, "")
        }
    }
    CursorSlider("Cursor width", settings.cursorWidth, 1..6, " dp")
    CursorSlider("Blink cycle", settings.blinkCycle, 300..2000, " ms", step = 100)
    CursorSlider("Blink smoothness", settings.blinkSmoothness, 0..100, "%")
}

@Composable
private fun CursorSlider(title: String, setting: com.pixelgram.desktop.core.Setting<Int>, range: IntRange, unit: String, step: Int = 1) {
    Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Slider(
            value = setting.value.toFloat(),
            onValueChange = { setting.value = (it / step).roundToInt() * step },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            modifier = Modifier.weight(1f),
        )
        Text("${setting.value}$unit", modifier = Modifier.width(72.dp).padding(start = 12.dp))
        ResetButton(setting)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FontPickerDialog(settings: AppSettings, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val families = com.pixelgram.desktop.ui.theme.MessageFonts.systemFamilies
    val shown = remember(query) { listOf("") + families.filter { it.contains(query, ignoreCase = true) } }
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.width(420.dp).height(560.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Message font", style = MaterialTheme.typography.headlineSmall)
                OutlinedTextField(query, { query = it }, placeholder = { Text("Search fonts") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                androidx.compose.foundation.lazy.LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    items(shown.size) { index ->
                        val value = shown[index]
                        val selected = settings.messageFont.value == value
                        SegmentedListItem(
                            onClick = {
                                settings.messageFont.value = value
                                onDismiss()
                            },
                            shapes = groupShapes(index, shown.size),
                            trailingContent = if (selected) {
                                { Icon(Icons.Filled.Check, contentDescription = null) }
                            } else null,
                        ) {
                            Text(
                                com.pixelgram.desktop.ui.theme.MessageFonts.label(value),
                                fontFamily = com.pixelgram.desktop.ui.theme.MessageFonts.family(value) ?: MaterialTheme.typography.bodyLarge.fontFamily,
                            )
                        }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Close") }
            }
        }
    }
}

@Composable
private fun DoubleClickEmoji(settings: AppSettings) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        QuickReactions.forEach { emoji ->
            val selected = settings.doubleClickEmoji.value == emoji
            val tile by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                MaterialTheme.motionScheme.defaultEffectsSpec(),
            )
            val corner by animateDpAsState(if (selected) 14.dp else 24.dp, MaterialTheme.motionScheme.fastSpatialSpec())
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(corner))
                    .background(tile)
                    .clickable { settings.doubleClickEmoji.value = emoji },
            ) {
                EmojiText(emoji, fontSize = 24.sp)
            }
        }
    }
}

private val QuickReactions = listOf("❤", "❤‍🔥", "👍", "🔥", "😁", "🥰", "👏", "🎉", "🤩", "😢", "🤔", "🤯", "😱", "🙏", "👌", "🕊", "💯", "⚡", "🏆", "💔", "👀")

private data class SystemRow(
    val title: String,
    val checked: Boolean,
    val visible: Boolean,
    val subtitle: String? = null,
    val onChange: (Boolean) -> Unit,
)

@Composable
private fun WindowSection(settings: AppSettings) {
    Header("Rendering")
    Description("Applies after restart. Auto uses DirectX on Windows and switches to OpenGL if pixelGram closed unexpectedly twice in a row.")
    ConnectedChoice(
        listOf("Auto" to "Auto", "DirectX" to "DirectX", "OpenGL" to "OpenGL", "Software" to "Software"),
        settings.renderApi.value,
        { settings.renderApi.value = it },
        enabled = { it != "DirectX" || com.pixelgram.desktop.core.AppPaths.isWindows },
    )
    Header("Window title bar")
    val titleRows = buildList {
        add(Triple("Show chat name", settings.titleChatName.value) { value: Boolean -> settings.titleChatName.value = value })
        add(Triple("Show active account", settings.titleAccount.value) { value: Boolean -> settings.titleAccount.value = value })
        add(Triple("Total unread count", settings.titleUnread.value) { value: Boolean -> settings.titleUnread.value = value })
        if (!AppPaths.isMac) {
            add(Triple("Use system window frame", !settings.customTitleBar.value) { value: Boolean -> settings.customTitleBar.value = !value })
        }
    }
    titleRows.forEachIndexed { index, (title, checked, change) ->
        SwitchRow(index, titleRows.size, title, if (title == "Use system window frame") "Applies after restarting pixelGram." else null, checked, change)
    }
    Header("System integration")
    val autostart = com.pixelgram.desktop.core.Autostart.supported
    val systemRows = buildList {
        add(SystemRow("Show tray icon", settings.trayIcon.value, true) { settings.trayIcon.value = it })
        add(SystemRow("Use monochrome icon", settings.trayMonochrome.value, settings.trayIcon.value) { settings.trayMonochrome.value = it })
        add(SystemRow("Restore window size and position", settings.restoreWindow.value, true) { settings.restoreWindow.value = it })
        if (autostart) {
            add(SystemRow("Launch pixelGram when system starts", settings.autostart.value, true) { value ->
                settings.autostart.value = value
                Thread { com.pixelgram.desktop.core.Autostart.apply(value) }.start()
            })
            add(
                SystemRow(
                    "Launch minimized",
                    settings.launchMinimized.value,
                    settings.autostart.value,
                    "pixelGram starts in the tray: notifications work, the window opens from the tray icon.",
                ) { settings.launchMinimized.value = it },
            )
        }
    }
    val shown = systemRows.filter { it.visible }
    systemRows.forEach { row ->
        val index = shown.indexOf(row)
        val last = remember { mutableStateOf(index to shown.size) }
        if (index >= 0) last.value = index to shown.size
        Reveal(row.visible) {
            SwitchRow(last.value.first, last.value.second, row.title, row.subtitle, row.checked, row.onChange)
        }
    }
}

@Composable
private fun ExperimentalSection(settings: AppSettings) {
    val scope = rememberCoroutineScope()
    var version by remember { mutableStateOf<String?>(null) }
    var checked by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var installing by remember { mutableStateOf(false) }
    var log by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        version = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { runCatching { PythonRunner.version() }.getOrNull() }
        checked = true
    }
    Description("These features are still in testing and may change or break.")
    Header("Python in messages")
    SwitchRow(
        0,
        1,
        "Run Python from messages",
        "Send .py \"code\" and pixelGram runs it on this computer, then sends the output in a quote with the code in a second quote. Only messages you send are run.",
        settings.pythonMessages.value,
    ) { settings.pythonMessages.value = it }
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
        Text(PythonExample, style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, modifier = Modifier.padding(12.dp))
    }
    AnimatedDescription(
        when {
            !checked -> "Looking for Python…"
            version != null -> com.pixelgram.desktop.core.tr("Interpreter") + ": " + version
            AppPaths.isWindows -> "Python 3 was not found. Install it from python.org, then restart pixelGram."
            else -> "Python 3 was not found. Install it with your package manager, then restart pixelGram."
        }
    )
    Header("Packages")
    Description("Install packages from PyPI for your code, for example requests. They are stored in the pixelGram data folder.")
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.filter { c -> c.isLetterOrDigit() || c in "-_.=<>[]," } },
            label = { Text("Package name") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        androidx.compose.material3.FilledTonalButton(
            enabled = name.isNotBlank() && !installing && version != null,
            onClick = {
                installing = true
                log = null
                val target = name.trim()
                scope.launch {
                    val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { PythonRunner.install(target) }
                    log = result.output.trim().lines().takeLast(PipLogLines).joinToString("\n")
                    installing = false
                    if (!result.failed) name = ""
                }
            },
        ) { Text(if (installing) "Installing…" else "Install") }
    }
    log?.let { text ->
        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
            Text(text, style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, modifier = Modifier.padding(12.dp))
        }
    }
}

private val PythonExample = ".py \"import requests\nprint(requests.get('https://api.ipify.org').text)\""
private const val PipLogLines = 6
private const val TitleLimit = 32

@Composable
private fun AdvancedSection(telegram: Telegram, settings: AppSettings) {
    Header("Data and storage")
    MemoryContent(telegram)
    Header("Data folder")
    Description(
        (if (AppPaths.isPortable) com.pixelgram.desktop.core.tr("Portable mode") + ": " else "") + AppPaths.dataDir.absolutePath
    )
    OutlinedButton(onClick = {
        Thread { runCatching { com.pixelgram.desktop.core.SystemOpen.file(AppPaths.dataDir) }.onFailure { com.pixelgram.desktop.core.AppLog.write("Open folder failed", it) } }.apply { isDaemon = true }.start()
    }) {
        Text("Open data folder")
    }
}

private fun openUrl(url: String) {
    com.pixelgram.desktop.ui.main.Links.openBrowser(url)
}

@Composable
private fun AboutSection(telegram: Telegram, onOpenChat: (Long) -> Unit) {
    Description("pixelGram ${BuildConfig.VERSION}. " + com.pixelgram.desktop.core.tr("A Telegram client with Material 3 Expressive, built on TDLib."))
    Header("Links")
    SegmentedGroup {
        LinkItem(0, 3, Icons.Outlined.Campaign, "Channel", "@$ChannelUsername") {
            telegram.openPublicChat(ChannelUsername, onOpenChat, onFail = { openUrl(ChannelUrl) })
        }
        LinkItem(1, 3, Icons.Outlined.Code, "GitHub", "JettaXP/pixelGram") { openUrl(GitHubUrl) }
        LinkItem(2, 3, Icons.Outlined.Language, "Website", WebsiteHost) { openUrl("https://$WebsiteHost") }
    }
}

@Composable
private fun OtherSection(telegram: Telegram, settings: AppSettings, onOpen: (Section) -> Unit) {
    val scope = rememberCoroutineScope()
    var exporting by remember { mutableStateOf<File?>(null) }
    var importing by remember { mutableStateOf<File?>(null) }
    var status by remember { mutableStateOf<String?>(null) }

    Header("Backup")
    Description("Save pixelGram Preferences to a .backup file and send it to any chat, or load them from one. Accounts are not included.")
    SegmentedGroup {
        LinkItem(0, 2, Icons.Outlined.Upload, "Export settings", "Choose a chat to send the backup to", trailing = false) {
            exporting = Backup.export(settings)
            if (exporting == null) status = "Couldn't create the backup."
        }
        LinkItem(1, 2, Icons.Outlined.Download, "Import settings", "Pick a .backup file", trailing = false) {
            scope.launch {
                val file = Attachments.pick().firstOrNull()
                when {
                    file == null -> Unit
                    Backup.isBackup(file.name) -> importing = file
                    else -> status = "That's not a pixelGram backup."
                }
            }
        }
    }
    Reveal(status != null) {
        Text(status.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }

    Header("Reset")
    var confirmReset by remember { mutableStateOf(false) }
    SegmentedGroup {
        LinkItem(0, 1, Icons.Outlined.RestartAlt, "Reset all settings", "Theme, colors, folders, corners, fonts and everything else in pixelGram", trailing = false) {
            confirmReset = true
        }
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            icon = { Icon(Icons.Outlined.RestartAlt, contentDescription = null) },
            title = { Text("Reset all settings?") },
            text = { Text("Every pixelGram setting goes back to its default. Your accounts, chats and passcode stay.") },
            confirmButton = {
                TextButton(onClick = {
                    settings.resetAll()
                    confirmReset = false
                    status = "All settings were reset."
                }) { Text("Reset", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
        )
    }

    Header("Updates")
    SwitchRow(
        index = 0,
        count = 1,
        title = "Check for updates on start",
        subtitle = "Looks for a newer nightly build on GitHub.",
        checked = settings.checkUpdates.value,
        onChange = { settings.checkUpdates.value = it },
    )
    UpdatePanel(onBeforeExit = {}, modifier = Modifier.padding(top = 8.dp))

    exporting?.let { file ->
        ForwardDialog(telegram, settings, onDismiss = { exporting = null }) { chatId ->
            exporting = null
            telegram.sendFiles(chatId, listOf(file), asPhotos = false, caption = "")
            status = "Sent ${file.name}."
        }
    }
    importing?.let { file ->
        BackupImportDialog(file, settings) { done ->
            importing = null
            if (done != null) status = if (done) "Settings imported." else "Couldn't read ${file.name}."
        }
    }
    SubsectionLinks(Section.Other, onOpen)
}

@Composable
private fun LinkItem(
    index: Int,
    count: Int,
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: Boolean = true,
    onClick: () -> Unit,
) {
    SegmentedListItem(
        onClick = onClick,
        shapes = groupShapes(index, count),
        leadingContent = { Icon(icon, contentDescription = null) },
        supportingContent = { Text(subtitle) },
        trailingContent = if (trailing) {
            { Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null) }
        } else {
            null
        },
    ) {
        Text(title)
    }
}
