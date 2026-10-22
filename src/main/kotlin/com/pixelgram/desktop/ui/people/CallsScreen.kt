package com.pixelgram.desktop.ui.people

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallMade
import androidx.compose.material.icons.automirrored.outlined.CallMissed
import androidx.compose.material.icons.automirrored.outlined.CallReceived
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.components.Avatar
import com.pixelgram.desktop.ui.components.PageScaffold
import com.pixelgram.desktop.ui.main.formatChatDate
import com.pixelgram.desktop.ui.theme.EmojiText
import it.tdlight.jni.TdApi

@Composable
fun CallsScreen(
    telegram: Telegram,
    settings: AppSettings,
    onOpenChat: (Long) -> Unit,
    onBack: () -> Unit,
) {
    var calls by remember { mutableStateOf<List<TdApi.Message>?>(null) }
    LaunchedEffect(Unit) { telegram.recentCalls { calls = it } }

    PageScaffold("Calls", onBack) {
        val loaded = calls
        if (loaded == null) {
            LoadingIndicator()
            return@PageScaffold
        }
        if (loaded.isEmpty()) {
            Text("No recent calls.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            "Making calls is not supported yet, this is your call history.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            modifier = Modifier.fillMaxWidth(),
        ) {
            loaded.forEachIndexed { index, message ->
                val call = message.content as? TdApi.MessageCall ?: return@forEachIndexed
                val chat = telegram.chats[message.chatId]
                val name = chat?.title ?: ""
                val missed = call.discardReason is TdApi.CallDiscardReasonMissed && !message.isOutgoing
                val kind = when {
                    message.isOutgoing -> "Outgoing"
                    missed -> "Missed"
                    else -> "Incoming"
                } + if (call.isVideo) " video call" else " call"
                val duration = if (call.duration > 0) " (%d:%02d)".format(call.duration / 60, call.duration % 60) else ""
                SegmentedListItem(
                    onClick = { onOpenChat(message.chatId) },
                    shapes = ListItemDefaults.segmentedShapes(index, loaded.size),
                    leadingContent = { Avatar(name, chat?.photo, telegram, settings, size = 40.dp) },
                    supportingContent = { Text("$kind$duration · ${formatChatDate(message.date)}") },
                    trailingContent = {
                        Icon(
                            when {
                                message.isOutgoing -> Icons.AutoMirrored.Outlined.CallMade
                                missed -> Icons.AutoMirrored.Outlined.CallMissed
                                else -> Icons.AutoMirrored.Outlined.CallReceived
                            },
                            contentDescription = kind,
                            tint = if (missed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        )
                    },
                ) {
                    EmojiText(name)
                }
            }
        }
    }
}
