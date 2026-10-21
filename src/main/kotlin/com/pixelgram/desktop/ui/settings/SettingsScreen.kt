package com.pixelgram.desktop.ui.settings

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
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.pixelgram.desktop.ui.AvatarShape
import com.pixelgram.desktop.ui.components.ShapePreview
import com.pixelgram.desktop.ui.main.MessageBubble
import com.pixelgram.desktop.ui.theme.Matugen
import com.pixelgram.desktop.ui.theme.SeedColors
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.URI

private enum class Section(val title: String, val icon: ImageVector) {
    Appearance("Appearance", Icons.Outlined.Palette),
    Avatars("Avatars", Icons.Outlined.Face),
    Bubbles("Message bubbles", Icons.Outlined.ChatBubbleOutline),
    Account("Account", Icons.Outlined.AccountCircle),
    About("About", Icons.Outlined.Info),
}

@Composable
fun SettingsScreen(telegram: Telegram, settings: AppSettings, onBack: () -> Unit) {
    var section by remember {
        val initial = System.getProperty("pixelgram.section")
        mutableStateOf(Section.entries.firstOrNull { it.name == initial } ?: Section.Appearance)
    }
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxSize()) {
        Row {
            Column(
                Modifier
                    .width(280.dp)
                    .fillMaxHeight()
                    .padding(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text("Settings", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.height(8.dp))
                Section.entries.forEach { item ->
                    val selected = item == section
                    ListItem(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.extraLarge)
                            .clickable { section = item },
                        colors = ListItemDefaults.colors(
                            containerColor = if (selected) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                Color.Transparent
                            },
                        ),
                        leadingContent = { Icon(item.icon, contentDescription = null) },
                        headlineContent = { Text(item.title) },
                    )
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
                    Text(section.title, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                    when (section) {
                        Section.Appearance -> AppearanceSection(settings)
                        Section.Avatars -> AvatarsSection(settings)
                        Section.Bubbles -> BubblesSection(settings)
                        Section.Account -> AccountSection(telegram)
                        Section.About -> AboutSection()
                    }
                }
            }
        }
    }
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
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .clickable { onChange(!checked) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange) },
    )
}

@Composable
private fun AppearanceSection(settings: AppSettings) {
    Header("Theme")
    SingleChoiceSegmentedButtonRow {
        ThemeMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = settings.themeMode.value == mode,
                onClick = { settings.themeMode.value = mode },
                shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
            ) {
                Text(mode.name)
            }
        }
    }

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
    SwitchRow(
        title = "Material 3 navigation rail",
        subtitle = "Show folders in a side rail. When off, folders are tabs above the chat list.",
        checked = settings.materialFolders.value,
        onChange = { settings.materialFolders.value = it },
    )
    SwitchRow(
        title = "Hide \"All chats\"",
        subtitle = "Only works when you have at least one folder.",
        checked = settings.hideAllChats.value,
        onChange = { settings.hideAllChats.value = it },
    )
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
private fun AccountSection(telegram: Telegram) {
    Description("Log out of Telegram on this computer.")
    Button(onClick = telegram::logOut) { Text("Log out") }
}

@Composable
private fun AboutSection() {
    Text("pixelGram ${BuildConfig.VERSION}", style = MaterialTheme.typography.titleMedium)
    Description("Telegram client built with Kotlin, Compose Multiplatform and Material 3 Expressive, powered by TDLib.")
    Header("Links")
    OutlinedButton(onClick = { openUrl("https://t.me/pixelgramclient") }) {
        Text("Channel @pixelgramclient")
    }
    Header("License")
    Description("GNU General Public License v3.0")
}

private fun openUrl(url: String) {
    runCatching {
        if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(URI(url))
    }
}
