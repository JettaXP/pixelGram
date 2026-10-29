package com.pixelgram.desktop.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.core.tr
import com.pixelgram.desktop.telegram.ChatState
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.telegram.answerJoinRequest
import com.pixelgram.desktop.telegram.canManage
import com.pixelgram.desktop.telegram.joinRequests
import com.pixelgram.desktop.ui.components.Avatar
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.components.expressive.AlertDialog
import com.pixelgram.desktop.ui.components.expressive.IconButton
import com.pixelgram.desktop.ui.components.expressive.TextButton
import com.pixelgram.desktop.ui.components.groupShapes
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.outlined.Check
import com.pixelgram.desktop.ui.icons.outlined.Close
import com.pixelgram.desktop.ui.icons.outlined.PersonAdd
import com.pixelgram.desktop.ui.theme.EmojiText
import it.tdlight.jni.TdApi

@Composable
fun JoinRequestsBar(chat: ChatState, telegram: Telegram, settings: AppSettings, modifier: Modifier = Modifier) {
    var open by remember(chat.id) { mutableStateOf(false) }
    AnimatedVisibility(
        visible = chat.joinRequests > 0 && telegram.canManage(chat),
        enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeIn(),
        exit = shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeOut(),
    ) {
        Surface(
            onClick = { open = true },
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.tertiaryContainer,
            modifier = modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Icon(Icons.Outlined.PersonAdd, contentDescription = null)
                Text("${chat.joinRequests} ${tr(if (chat.joinRequests == 1) "join request" else "join requests")}", style = MaterialTheme.typography.titleSmall)
            }
        }
    }
    if (open) JoinRequestsDialog(chat, telegram, settings) { open = false }
}

@Composable
fun JoinRequestsDialog(chat: ChatState, telegram: Telegram, settings: AppSettings, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Join requests") },
        text = { Column(Modifier.heightIn(max = 480.dp)) { JoinRequestsPage(chat, telegram, settings) } },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
internal fun JoinRequestsPage(chat: ChatState, telegram: Telegram, settings: AppSettings?) {
    if (settings == null) return
    var requests by remember(chat.id) { mutableStateOf<List<TdApi.ChatJoinRequest>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(chat.id) { telegram.joinRequests(chat.id) { requests = it } }
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        val list = requests
        when {
            list == null -> LoadingIndicator(Modifier.padding(16.dp))
            list.isEmpty() -> Text("No pending requests", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> list.forEachIndexed { index, request ->
                val user = telegram.users[request.userId]
                val name = user?.let { listOf(it.firstName, it.lastName).filter(String::isNotBlank).joinToString(" ") } ?: tr("User")
                SegmentedListItem(
                    onClick = {},
                    shapes = groupShapes(index, list.size),
                    leadingContent = { Avatar(name, user?.profilePhoto?.small, telegram, settings, 40.dp) },
                    supportingContent = request.bio.takeIf { it.isNotBlank() }?.let { bio -> { EmojiText(bio, maxLines = 2) } },
                    trailingContent = {
                        Row {
                            IconButton(onClick = {
                                telegram.answerJoinRequest(chat.id, request.userId, false) { failure ->
                                    error = failure
                                    if (failure == null) requests = requests?.filter { it.userId != request.userId }
                                }
                            }) { Icon(Icons.Outlined.Close, contentDescription = "Dismiss") }
                            IconButton(onClick = {
                                telegram.answerJoinRequest(chat.id, request.userId, true) { failure ->
                                    error = failure
                                    if (failure == null) requests = requests?.filter { it.userId != request.userId }
                                }
                            }) { Icon(Icons.Outlined.Check, contentDescription = "Add", tint = MaterialTheme.colorScheme.primary) }
                        }
                    },
                ) { EmojiText(name) }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
