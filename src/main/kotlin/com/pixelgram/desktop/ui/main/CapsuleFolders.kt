package com.pixelgram.desktop.ui.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.filled.*
import com.pixelgram.desktop.ui.icons.outlined.*
import com.pixelgram.desktop.ui.icons.automirrored.filled.MenuBook
import com.pixelgram.desktop.ui.icons.automirrored.filled.TrendingUp
import com.pixelgram.desktop.ui.icons.automirrored.outlined.MenuBook
import com.pixelgram.desktop.ui.icons.automirrored.outlined.TrendingUp
import com.pixelgram.desktop.ui.icons.filled.Campaign
import com.pixelgram.desktop.ui.icons.filled.Folder
import com.pixelgram.desktop.ui.icons.filled.Forum
import com.pixelgram.desktop.ui.icons.filled.Groups
import com.pixelgram.desktop.ui.icons.filled.Home
import com.pixelgram.desktop.ui.icons.filled.MarkChatUnread
import com.pixelgram.desktop.ui.icons.filled.Person
import com.pixelgram.desktop.ui.icons.filled.SmartToy
import com.pixelgram.desktop.ui.icons.filled.Star
import com.pixelgram.desktop.ui.icons.filled.Work
import com.pixelgram.desktop.ui.icons.outlined.Campaign
import com.pixelgram.desktop.ui.icons.outlined.Folder
import com.pixelgram.desktop.ui.icons.outlined.Forum
import com.pixelgram.desktop.ui.icons.outlined.Groups
import com.pixelgram.desktop.ui.icons.outlined.Home
import com.pixelgram.desktop.ui.icons.outlined.MarkChatUnread
import com.pixelgram.desktop.ui.icons.outlined.Person
import com.pixelgram.desktop.ui.icons.outlined.SmartToy
import com.pixelgram.desktop.ui.icons.outlined.StarOutline
import com.pixelgram.desktop.ui.icons.outlined.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.pixelgram.desktop.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.telegram.ChatListKey
import com.pixelgram.desktop.ui.theme.EmojiText
import kotlin.math.roundToInt

@Composable
internal fun CapsuleFolders(
    tabs: List<FolderTab>,
    selected: ChatListKey,
    settings: AppSettings,
    onSelect: (ChatListKey) -> Unit,
) {
    if (tabs.size < 2) return
    val height = settings.capsuleHeight.value.dp - CapsuleInset * 2
    val spec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val colors = MaterialTheme.colorScheme
    val mode = settings.folderTitle.value

    val bounds = remember { mutableStateMapOf<ChatListKey, Pair<Float, Float>>() }
    val scroll = rememberScrollState()
    LaunchedEffect(selected, bounds[selected], scroll.viewportSize) {
        val (left, width) = bounds[selected] ?: return@LaunchedEffect
        if (scroll.maxValue == 0) return@LaunchedEffect
        val target = (left + width / 2 - scroll.viewportSize / 2f).roundToInt().coerceIn(0, scroll.maxValue)
        scroll.animateScrollTo(target, spec)
    }
    var previous by remember { mutableStateOf(selected) }
    val travel = remember { Animatable(1f) }
    LaunchedEffect(selected) {
        if (travel.value < 1f || previous != selected) {
            travel.snapTo(0f)
            travel.animateTo(1f, spec)
        }
        previous = selected
    }

    Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
        Surface(shape = CircleShape, color = colors.surfaceContainerHighest) {
            Box(Modifier.padding(CapsuleInset).clip(CircleShape).horizontalScroll(scroll)) {
                Box(
                    Modifier
                        .layout { measurable, _ ->
                            val from = bounds[previous]
                            val to = bounds[selected] ?: from
                            val t = travel.value
                            val left = lerp(from?.first ?: to?.first ?: 0f, to?.first ?: 0f, t)
                            val width = lerp(from?.second ?: to?.second ?: 0f, to?.second ?: 0f, t)
                            val placeable = measurable.measure(
                                Constraints.fixed(width.roundToInt(), height.roundToPx())
                            )
                            layout(0, 0) { placeable.place(left.roundToInt(), 0) }
                        }
                        .clip(CircleShape)
                        .background(colors.primary),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(settings.capsuleSpacing.value.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    tabs.forEach { tab ->
                        val isSelected = tab.key == selected
                        val progress by animateFloatAsState(if (isSelected) 1f else 0f, spec)
                        val content = lerp(colors.onSurfaceVariant, colors.onPrimary, progress)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .onPlaced { bounds[tab.key] = it.positionInParent().x to it.size.width.toFloat() }
                                .height(height)
                                .clip(CircleShape)
                                .selectable(selected = isSelected, role = Role.Tab) { onSelect(tab.key) }
                                .padding(horizontal = IndicatorSidePadding),
                        ) {
                            if (mode != com.pixelgram.desktop.core.FolderTitle.Names) FolderIcon(tab, isSelected, tint = content)
                            if (mode != com.pixelgram.desktop.core.FolderTitle.Icons) {
                                Box(Modifier.padding(start = if (mode == com.pixelgram.desktop.core.FolderTitle.Names) 0.dp else LabelGap)) {
                                    if (mode == com.pixelgram.desktop.core.FolderTitle.Names) {
                                        FolderName(tab, tint = content)
                                    } else {
                                        EmojiText(tab.title, color = content, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private val LabelGap = 4.dp
private val CapsuleInset = 12.dp
private val IndicatorSidePadding = 16.dp
internal fun folderIcon(name: String, filled: Boolean): ImageVector {
    val pair = FolderIcons[name] ?: FolderIcons.getValue("Custom")
    return if (filled) pair.first else pair.second
}

internal val FolderIcons: Map<String, Pair<ImageVector, ImageVector>> = linkedMapOf(
    "All" to (Icons.Filled.Forum to Icons.Outlined.Forum),
    "Unread" to (Icons.Filled.MarkChatUnread to Icons.Outlined.MarkChatUnread),
    "Mention" to (Icons.Filled.AlternateEmail to Icons.Outlined.AlternateEmail),
    "Unmuted" to (Icons.Filled.NotificationsActive to Icons.Outlined.NotificationsActive),
    "Private" to (Icons.Filled.Person to Icons.Outlined.Person),
    "Groups" to (Icons.Filled.Groups to Icons.Outlined.Groups),
    "Channels" to (Icons.Filled.Campaign to Icons.Outlined.Campaign),
    "Bots" to (Icons.Filled.SmartToy to Icons.Outlined.SmartToy),
    "Flag" to (Icons.Filled.Flag to Icons.Outlined.Flag),
    "Favorite" to (Icons.Filled.Star to Icons.Outlined.StarOutline),
    "Love" to (Icons.Filled.Favorite to Icons.Outlined.FavoriteBorder),
    "Home" to (Icons.Filled.Home to Icons.Outlined.Home),
    "Work" to (Icons.Filled.Work to Icons.Outlined.Work),
    "Study" to (Icons.Filled.School to Icons.Outlined.School),
    "Book" to (Icons.AutoMirrored.Filled.MenuBook to Icons.AutoMirrored.Outlined.MenuBook),
    "Trade" to (Icons.AutoMirrored.Filled.TrendingUp to Icons.AutoMirrored.Outlined.TrendingUp),
    "Money" to (Icons.Filled.Payments to Icons.Outlined.Payments),
    "Travel" to (Icons.Filled.Luggage to Icons.Outlined.Luggage),
    "Airplane" to (Icons.Filled.Flight to Icons.Outlined.Flight),
    "Game" to (Icons.Filled.SportsEsports to Icons.Outlined.SportsEsports),
    "Sport" to (Icons.Filled.SportsSoccer to Icons.Outlined.SportsSoccer),
    "Party" to (Icons.Filled.Celebration to Icons.Outlined.Celebration),
    "Mask" to (Icons.Filled.TheaterComedy to Icons.Outlined.TheaterComedy),
    "Crown" to (Icons.Filled.WorkspacePremium to Icons.Outlined.WorkspacePremium),
    "Cat" to (Icons.Filled.Pets to Icons.Outlined.Pets),
    "Flower" to (Icons.Filled.LocalFlorist to Icons.Outlined.LocalFlorist),
    "Note" to (Icons.Filled.MusicNote to Icons.Outlined.MusicNote),
    "Palette" to (Icons.Filled.Palette to Icons.Outlined.Palette),
    "Light" to (Icons.Filled.Lightbulb to Icons.Outlined.Lightbulb),
    "Like" to (Icons.Filled.ThumbUp to Icons.Outlined.ThumbUp),
    "Setup" to (Icons.Filled.Settings to Icons.Outlined.Settings),
    "Custom" to (Icons.Filled.Folder to Icons.Outlined.Folder),
)
