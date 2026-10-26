package com.pixelgram.desktop.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.core.FocusMode
import com.pixelgram.desktop.core.NotificationStyle
import com.pixelgram.desktop.core.Sounds
import com.pixelgram.desktop.core.SystemNotifications
import com.pixelgram.desktop.core.ToastSide
import com.pixelgram.desktop.telegram.Notifications
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.components.expressive.Button
import com.pixelgram.desktop.ui.components.expressive.IconButton
import com.pixelgram.desktop.ui.components.expressive.OutlinedButton
import com.pixelgram.desktop.ui.components.expressive.Slider
import com.pixelgram.desktop.ui.components.groupShapes
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.filled.Check
import com.pixelgram.desktop.ui.icons.filled.PlayArrow
import com.pixelgram.desktop.ui.notify.MaxCount
import com.pixelgram.desktop.ui.notify.notificationScreens
import it.tdlight.jni.TdApi

private val Scopes: List<Pair<String, TdApi.NotificationSettingsScope>> = listOf(
    "Private chats" to TdApi.NotificationSettingsScopePrivateChats(),
    "Groups" to TdApi.NotificationSettingsScopeGroupChats(),
    "Channels" to TdApi.NotificationSettingsScopeChannelChats(),
)

@Composable
internal fun NotificationsSection(telegram: Telegram, settings: AppSettings) {
    Reveal(settings.notificationsMuted.value) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Description("Notifications are disabled from the tray menu.")
            Button(onClick = { settings.notificationsMuted.value = false }, modifier = Modifier.padding(bottom = 8.dp)) {
                Text("Enable notifications")
            }
        }
    }

    Header("Show notifications from")
    SwitchRow(0, 1, "All accounts", null, settings.notifyAllAccounts.value) { settings.notifyAllAccounts.value = it }
    Description("Turn this off if you want to receive notifications only from the account you are currently using.")

    Header("Global settings")
    SwitchRow(0, 3, "Desktop notifications", null, settings.notificationPopups.value) { settings.notificationPopups.value = it }
    SwitchRow(1, 3, "Flash the taskbar icon", null, settings.flashTaskbar.value) { settings.flashTaskbar.value = it }
    SwitchRow(2, 3, "Allow sound", null, settings.notificationSound.value) { settings.notificationSound.value = it }
    Reveal(settings.notificationSound.value) {
        Column {
            Header("Volume")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = settings.notificationVolume.value.toFloat(),
                    onValueChange = { settings.notificationVolume.value = it.toInt() },
                    onValueChangeFinished = { Sounds.newMessage(settings.notificationVolume.value) },
                    valueRange = 0f..Sounds.MaxVolume.toFloat(),
                    modifier = Modifier.weight(1f),
                )
                Text("${settings.notificationVolume.value}%", modifier = Modifier.width(56.dp).padding(start = 12.dp))
                IconButton(onClick = { Sounds.newMessage(settings.notificationVolume.value) }) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Play sound")
                }
            }
        }
    }
    PreviewCard(settings)

    Header("Notifications for chats")
    val exceptions = remember { mutableStateMapOf<Int, Int>() }
    LaunchedEffect(telegram) {
        Scopes.forEachIndexed { index, (_, scope) -> telegram.notificationExceptions(scope) { exceptions[index] = it } }
        telegram.loadContactJoined()
    }
    Scopes.forEachIndexed { index, (title, scope) ->
        val on = (telegram.scopeSettings(scope)?.muteFor ?: 0) == 0
        val count = exceptions[index] ?: 0
        val state = if (on) "On" else "Off"
        SwitchRow(
            index,
            Scopes.size + 1,
            title,
            if (count > 0) "$state, $count ${if (count == 1) "exception" else "exceptions"}" else state,
            on,
        ) { telegram.setScopeMuted(scope, !it) }
    }
    val reactions = telegram.reactionNotifications?.messageReactionSource !is TdApi.ReactionNotificationSourceNone
    SwitchRow(Scopes.size, Scopes.size + 1, "Reactions", "Messages, Poll votes", reactions) { telegram.setReactionNotifications(it) }

    Header("Events")
    SwitchRow(0, 2, "Contact joined Telegram", null, !telegram.contactJoinedMuted) { telegram.setContactJoined(it) }
    val pinned = Scopes.all { (_, scope) -> telegram.scopeSettings(scope)?.disablePinnedMessageNotifications != true }
    SwitchRow(1, 2, "Pinned messages", null, pinned) { telegram.setPinnedNotifications(it) }

    Header("Calls")
    SwitchRow(0, 1, "Accept calls on this device", null, settings.acceptCalls.value) { settings.acceptCalls.value = it }

    Header("Badge counter")
    SwitchRow(0, 3, "Include muted chats in unread count", null, settings.badgeIncludeMuted.value) { settings.badgeIncludeMuted.value = it }
    SwitchRow(1, 3, "Include muted chats in folder counters", null, settings.folderIncludeMuted.value) { settings.folderIncludeMuted.value = it }
    SwitchRow(2, 3, "Count unread messages", null, settings.badgeCountMessages.value) { settings.badgeCountMessages.value = it }

    val systemRows = buildList {
        if (SystemNotifications.available) add("system")
        if (FocusMode.supported) add("focus")
    }
    if (systemRows.isNotEmpty()) {
        Header("System integration")
        systemRows.forEachIndexed { index, row ->
            when (row) {
                "system" -> SwitchRow(
                    index,
                    systemRows.size,
                    "Use system notifications",
                    "Shown by your desktop's notification daemon instead of pixelGram's own Material 3 cards.",
                    settings.notificationStyle.value == NotificationStyle.System,
                ) { settings.notificationStyle.value = if (it) NotificationStyle.System else NotificationStyle.PixelGram }
                else -> SwitchRow(
                    index,
                    systemRows.size,
                    "Respect system Focus mode",
                    "No pop-ups or sounds while Do Not Disturb is on.",
                    settings.respectFocus.value,
                ) { settings.respectFocus.value = it }
            }
        }
    }

    val ownCards = !(SystemNotifications.available && settings.notificationStyle.value == NotificationStyle.System)
    Reveal(ownCards) {
        Column {
            val screens = remember { notificationScreens() }
            if (screens.size > 1) {
                Header("Display for notifications")
                val options = listOf("" to "Default") + screens
                options.forEachIndexed { index, (id, label) ->
                    SegmentedListItem(
                        selected = settings.notificationScreen.value == id,
                        onClick = { settings.notificationScreen.value = id },
                        shapes = groupShapes(index, options.size),
                        leadingContent = { RadioButton(selected = settings.notificationScreen.value == id, onClick = null) },
                    ) { Text(label) }
                }
            }
            Header("Location on the screen")
            ScreenPicker(settings.notificationSide.value) { settings.notificationSide.value = it }
            Header("Notifications count")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = settings.notificationCount.value.toFloat(),
                    onValueChange = { settings.notificationCount.value = it.toInt() },
                    valueRange = 1f..MaxCount.toFloat(),
                    steps = MaxCount - 2,
                    modifier = Modifier.weight(1f),
                )
                Text(settings.notificationCount.value.toString(), modifier = Modifier.width(40.dp).padding(start = 12.dp))
            }
        }
    }
    OutlinedButton(
        onClick = {
            if (!ownCards) {
                SystemNotifications.show("pixelGram", "This is how new messages will look 🎉", null) {}
            } else {
                Notifications.showTest?.invoke()
            }
        },
        modifier = Modifier.padding(top = 12.dp),
    ) {
        Text("Show test notification")
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PreviewCard(settings: AppSettings) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
            Surface(shape = MaterialTheme.shapes.medium, color = colors.primaryContainer, modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(12.dp)) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(colors.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(if (settings.notifyShowName.value) "D" else "p", color = colors.onPrimary, style = MaterialTheme.typography.titleMedium)
                    }
                    Column {
                        Text(
                            if (settings.notifyShowName.value) "Dino Rex" else "pixelGram",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.onPrimaryContainer,
                        )
                        Text(
                            if (settings.notifyShowText.value) "It's morning in Tokyo 😎" else "New message",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onPrimaryContainer,
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                PreviewChip("Name", settings.notifyShowName.value) { settings.notifyShowName.value = it }
                PreviewChip("Text", settings.notifyShowText.value) { settings.notifyShowText.value = it }
            }
        }
    }
}

@Composable
private fun PreviewChip(label: String, selected: Boolean, onChange: (Boolean) -> Unit) {
    FilterChip(
        selected = selected,
        onClick = { onChange(!selected) },
        label = { Text(label) },
        leadingIcon = if (selected) {
            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
        } else {
            null
        },
    )
}

@Composable
private fun ScreenPicker(selected: ToastSide, onSelect: (ToastSide) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        BoxWithConstraints(
            Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .clip(RoundedCornerShape(20.dp))
                .background(colors.surfaceContainerHighest)
                .padding(10.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surfaceContainerLowest),
        ) {
            ToastSide.entries.forEach { side ->
                val active = side == selected
                val stack = if (active) 3 else 1
                val align = when (side) {
                    ToastSide.TopLeft -> Alignment.TopStart
                    ToastSide.TopCenter -> Alignment.TopCenter
                    ToastSide.TopRight -> Alignment.TopEnd
                    ToastSide.BottomLeft -> Alignment.BottomStart
                    ToastSide.BottomCenter -> Alignment.BottomCenter
                    ToastSide.BottomRight -> Alignment.BottomEnd
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier
                        .align(align)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelect(side) }
                        .padding(4.dp),
                ) {
                    repeat(stack) { MiniToast(active) }
                }
            }
        }
        Box(Modifier.width(56.dp).height(18.dp).background(colors.surfaceContainerHighest))
        Box(Modifier.width(120.dp).height(8.dp).clip(RoundedCornerShape(4.dp)).background(colors.surfaceContainerHighest))
    }
}

@Composable
private fun MiniToast(active: Boolean) {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(if (active) colors.primaryContainer else colors.surfaceContainerHigh)
    val line by animateColorAsState(if (active) colors.onPrimaryContainer.copy(alpha = 0.6f) else colors.outlineVariant)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(container)
            .border(if (active) 1.dp else 0.dp, if (active) colors.primary else container, RoundedCornerShape(6.dp))
            .padding(5.dp),
    ) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(line))
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Box(Modifier.width(40.dp).height(3.dp).clip(CircleShape).background(line))
            Box(Modifier.width(28.dp).height(3.dp).clip(CircleShape).background(line))
        }
    }
}
