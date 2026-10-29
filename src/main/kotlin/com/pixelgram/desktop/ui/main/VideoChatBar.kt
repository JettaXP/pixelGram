package com.pixelgram.desktop.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.calls.VideoChats
import com.pixelgram.desktop.telegram.ChatState
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.telegram.activeVideoChat
import com.pixelgram.desktop.telegram.rtmpUrl
import com.pixelgram.desktop.telegram.startVideoChat
import com.pixelgram.desktop.ui.components.AvatarImage
import com.pixelgram.desktop.ui.components.ConfirmOnEnter
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.components.expressive.AlertDialog
import com.pixelgram.desktop.ui.components.expressive.Button
import com.pixelgram.desktop.ui.components.expressive.IconButton
import com.pixelgram.desktop.ui.components.expressive.TextButton
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.filled.Groups
import com.pixelgram.desktop.ui.icons.filled.Sensors
import com.pixelgram.desktop.ui.icons.outlined.ContentCopy
import com.pixelgram.desktop.ui.settings.PixelSwitch
import com.pixelgram.desktop.ui.theme.EmojiText
import it.tdlight.jni.TdApi
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

private const val SpinMs = 12_000
private const val ShownSpeakers = 3

@Composable
fun VideoChatBar(chat: ChatState, telegram: Telegram, modifier: Modifier = Modifier) {
    val call = telegram.activeVideoChat(chat)
    var last by remember(chat.id) { mutableStateOf<TdApi.GroupCall?>(null) }
    if (call != null) last = call
    AnimatedVisibility(
        visible = call != null,
        enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeIn(),
        exit = shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeOut(),
        modifier = modifier,
    ) {
        val shown = call ?: last ?: return@AnimatedVisibility
        val colors = MaterialTheme.colorScheme
        val stream = shown.isRtmpStream || chat.isChannel
        val joined = VideoChats.isJoined(telegram, shown.id)
        val join = { VideoChats.join(telegram, chat, shown.id)?.let { TopNotice.text = it } }
        Surface(
            onClick = { join() },
            shape = RoundedCornerShape(20.dp),
            color = colors.primaryContainer,
            modifier = Modifier.fillMaxWidth().focusProperties { canFocus = false },
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
            ) {
                val spin = rememberInfiniteTransition()
                val turn by spin.animateFloat(0f, 360f, infiniteRepeatable(tween(SpinMs, easing = LinearEasing)))
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp)) {
                    Box(Modifier.fillMaxSize().graphicsLayer { rotationZ = turn }.background(colors.primary, MaterialShapes.Cookie9Sided.toShape()))
                    Icon(if (stream) Icons.Filled.Sensors else Icons.Filled.Groups, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(22.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        if (stream) "Live stream" else "Video chat",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onPrimaryContainer,
                    )
                    val count = shown.participantCount
                    Text(
                        when {
                            count == 0 -> "No one is here yet"
                            stream && count == 1 -> "1 watching"
                            stream -> "$count watching"
                            count == 1 -> "1 participant"
                            else -> "$count participants"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onPrimaryContainer.copy(alpha = 0.8f),
                    )
                }
                Speakers(shown, telegram)
                Button(onClick = { join() }) { Text(if (joined) "Open" else if (stream) "Watch" else "Join call") }
            }
        }
    }
}

@Composable
private fun Speakers(call: TdApi.GroupCall, telegram: Telegram) {
    val speakers = call.recentSpeakers.orEmpty().take(ShownSpeakers)
    if (speakers.isEmpty()) return
    val colors = MaterialTheme.colorScheme
    Box(Modifier.size(width = (28 + 18 * (speakers.size - 1)).dp, height = 28.dp)) {
        speakers.forEachIndexed { index, speaker ->
            val sender = speaker.participantId
            val user = (sender as? TdApi.MessageSenderUser)?.let { telegram.users[it.userId] }
            val chat = (sender as? TdApi.MessageSenderChat)?.let { telegram.chats[it.chatId] }
            val title = telegram.senderTitle(sender)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (18 * index).dp)
                    .size(28.dp)
                    .background(if (speaker.isSpeaking) colors.primary else colors.surfaceContainerLowest, CircleShape),
            ) {
                AvatarImage(title, user?.profilePhoto?.small ?: chat?.photo, telegram, CircleShape, 24.dp)
            }
        }
    }
}

@Composable
fun StartVideoChatDialog(chat: ChatState, telegram: Telegram, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var rtmp by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var streamInfo by remember { mutableStateOf(false) }
    if (streamInfo) return RtmpDialog(chat, telegram, onDismiss)
    val start = {
        if (!busy) {
            busy = true
            error = null
            telegram.startVideoChat(chat, rtmp) { failure, id ->
                busy = false
                when {
                    failure != null -> error = failure
                    rtmp -> streamInfo = true
                    else -> {
                        onDismiss()
                        VideoChats.join(telegram, chat, id)?.let { TopNotice.text = it }
                    }
                }
            }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (chat.isChannel) "Start live stream?" else "Start video chat?") },
        text = {
            ConfirmOnEnter(start) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        if (chat.isChannel) "Subscribers will be able to watch and listen to your live stream."
                        else "Members of this group will be able to join the video chat.",
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("Stream with other apps", style = MaterialTheme.typography.titleSmall)
                            Text("Use OBS or another app to stream over RTMP.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                        PixelSwitch(checked = rtmp, onCheckedChange = { rtmp = it })
                    }
                    error?.let { Text(it, color = colors.error, style = MaterialTheme.typography.bodySmall) }
                }
            }
        },
        confirmButton = { TextButton(onClick = start, enabled = !busy) { Text("Start") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun RtmpDialog(chat: ChatState, telegram: Telegram, onDismiss: () -> Unit) {
    var url by remember { mutableStateOf<TdApi.RtmpUrl?>(null) }
    var failed by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(chat.id) {
        telegram.rtmpUrl(chat) { result ->
            url = result
            failed = result == null
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Stream with other apps") },
        text = {
            ConfirmOnEnter(onDismiss) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Paste these into your streaming app. Keep the stream key secret.")
                    val info = url
                    when {
                        info != null -> {
                            CopyRow("Server URL", info.url)
                            CopyRow("Stream key", info.streamKey)
                        }
                        failed -> Text("Couldn't get the stream key", color = MaterialTheme.colorScheme.error)
                        else -> Text("Loading…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
private fun CopyRow(label: String, value: String) {
    val colors = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(16.dp), color = colors.surfaceContainerHighest) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                EmojiText(value, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            }
            IconButton(onClick = {
                Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(value), null)
                TopNotice.text = "Copied"
            }) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy")
            }
        }
    }
}
