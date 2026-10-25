package com.pixelgram.desktop.ui.settings

import com.pixelgram.desktop.ui.icons.outlined.Movie
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.outlined.Animation
import com.pixelgram.desktop.ui.icons.outlined.ChatBubbleOutline
import com.pixelgram.desktop.ui.icons.outlined.EmojiEmotions
import com.pixelgram.desktop.ui.icons.outlined.KeyboardArrowDown
import com.pixelgram.desktop.ui.icons.outlined.StickyNote2
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.core.Setting

private class PowerOption(val title: String, val setting: Setting<Boolean>)

@Composable
fun BatterySection(settings: AppSettings) {
    Text(
        "Turn off animations to save battery and make pixelGram lighter on slow computers.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 8.dp)) {
        PowerGroup(
            Icons.Outlined.StickyNote2,
            "Animated Stickers",
            listOf(
                PowerOption("Autoplay in panel", settings.stickersInPanel),
                PowerOption("Autoplay in chat", settings.stickersInChat),
            ),
            RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
        )
        PowerGroup(
            Icons.Outlined.EmojiEmotions,
            "Animated Emoji",
            listOf(PowerOption("Autoplay in messages", settings.emojiInMessages)),
            RoundedCornerShape(4.dp),
        )
        PowerGroup(
            Icons.Outlined.Movie,
            "Autoplay Media",
            listOf(
                PowerOption("GIFs", settings.autoplayGifs),
                PowerOption("Videos", settings.autoplayVideos),
            ),
            RoundedCornerShape(4.dp),
        )
        PowerGroup(
            Icons.Outlined.ChatBubbleOutline,
            "Animations in Chats",
            listOf(
                PowerOption("Animated spoiler effect", settings.spoilerEffect),
                PowerOption("Message animations", settings.messageAnimations),
            ),
            RoundedCornerShape(4.dp),
        )
        PowerGroup(
            Icons.Outlined.Animation,
            "Interface animations",
            listOf(PowerOption("Interface animations", settings.interfaceAnimations)),
            RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
        )
    }
}

@Composable
private fun PowerGroup(icon: ImageVector, title: String, options: List<PowerOption>, shape: RoundedCornerShape) {
    var expanded by remember { mutableStateOf(false) }
    val on = options.count { it.setting.value }
    val single = options.size == 1
    val toggleAll = { value: Boolean -> options.forEach { it.setting.value = value } }
    Surface(shape = shape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { if (single) toggleAll(on == 0) else expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (!single) {
                    Text("$on/${options.size}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    val turn by animateFloatAsState(if (expanded) 180f else 0f, MaterialTheme.motionScheme.fastSpatialSpec())
                    Icon(
                        Icons.Outlined.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        modifier = Modifier.size(20.dp).rotate(turn),
                    )
                }
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                    PixelSwitch(checked = on > 0, onCheckedChange = { toggleAll(it) })
                }
            }
            AnimatedVisibility(
                visible = expanded && !single,
                enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeIn(),
                exit = shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeOut(),
            ) {
                Column(Modifier.padding(start = 28.dp, end = 12.dp, bottom = 8.dp)) {
                    options.forEach { option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { option.setting.value = !option.setting.value }
                                .padding(vertical = 2.dp),
                        ) {
                            Checkbox(checked = option.setting.value, onCheckedChange = { option.setting.value = it })
                            Text(option.title, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}
