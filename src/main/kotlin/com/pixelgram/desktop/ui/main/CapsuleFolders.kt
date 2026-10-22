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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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

// Experimental: folders in one capsule. Only the selected folder shows its
// name, the others shrink to their icon.
@Composable
internal fun CapsuleFolders(
    tabs: List<FolderTab>,
    selected: ChatListKey,
    settings: AppSettings,
    onSelect: (ChatListKey) -> Unit,
) {
    if (tabs.size < 2) return
    // Official M3 Expressive proportions: a 64dp toolbar with a 40dp active
    // indicator; the same inset on every side keeps the pill a scaled-down
    // copy of the capsule.
    val height = settings.capsuleHeight.value.dp - CapsuleInset * 2
    // WHY: a bouncy spring makes the whole bar overshoot its final width;
    // labels and the indicator share one non-bouncy spec so they stay in sync.
    val spec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val colors = MaterialTheme.colorScheme

    val bounds = remember { mutableStateMapOf<ChatListKey, Pair<Float, Float>>() }
    val scroll = rememberScrollState()
    // Keeps the selected folder in view, centred when the row scrolls.
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
            // With many folders the row scrolls; rounded ends match the pill, so
            // folders slide under the curve instead of being cut off flat.
            Box(Modifier.padding(CapsuleInset).clip(CircleShape).horizontalScroll(scroll)) {
                // One indicator slides between folders instead of each button
                // fading its own background.
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
                            Icon(folderIcon(tab.icon, isSelected), contentDescription = tab.title, tint = content)
                            if (progress > 0f) {
                                EmojiText(
                                    tab.title,
                                    color = content,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    modifier = Modifier
                                        .clipToBounds()
                                        .layout { measurable, constraints ->
                                            val placeable = measurable.measure(
                                                constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity)
                                            )
                                            val gap = LabelGap.roundToPx()
                                            val width = ((placeable.width + gap) * progress).roundToInt()
                                            layout(width, placeable.height) { placeable.place(gap, 0) }
                                        }
                                        .graphicsLayer { alpha = progress },
                                )
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
