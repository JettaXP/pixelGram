package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.pixelgram.desktop.core.tr
import com.pixelgram.desktop.telegram.ChatState
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.telegram.UiMessage
import com.pixelgram.desktop.ui.components.AvatarImage
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.filled.DoneAll
import com.pixelgram.desktop.ui.icons.outlined.FavoriteBorder
import com.pixelgram.desktop.ui.theme.EmojiText
import it.tdlight.jni.TdApi
import kotlinx.coroutines.delay

private class SeenEntry(val sender: TdApi.MessageSender, val reaction: TdApi.ReactionType?, val date: Int)

@Composable
fun SeenFooter(message: UiMessage, telegram: Telegram, chat: ChatState?) {
    if (chat == null || message.service || message.id <= 0L || telegram.isSavedMessages(chat.id)) return
    if (chat.privateUserId != 0L) {
        if (message.isOutgoing && !telegram.isBot(chat)) ReadDate(message, telegram)
        return
    }
    if (chat.isGroup && !chat.isChannel) GroupSeen(message, telegram)
}

@Composable
private fun FooterCard(visible: Boolean, content: @Composable () -> Unit) {
    val motion = MaterialTheme.motionScheme
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.expandVertically(motion.fastSpatialSpec()) + androidx.compose.animation.fadeIn(motion.fastEffectsSpec()),
        exit = androidx.compose.animation.ExitTransition.None,
    ) {
        Surface(
            shape = com.pixelgram.desktop.ui.components.groupShapes(0, 1).shape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
            content = content,
        )
    }
}

@Composable
private fun ReadDate(message: UiMessage, telegram: Telegram) {
    var read by remember(message.chatId, message.id) { mutableStateOf<Int?>(null) }
    LaunchedEffect(message.chatId, message.id) {
        telegram.send(TdApi.GetMessageReadDate(message.chatId, message.id)) { result ->
            read = (result as? TdApi.MessageReadDateRead)?.readDate
        }
    }
    FooterCard(read != null) {
    val date = read ?: return@FooterCard
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = RowHeight).padding(horizontal = 12.dp),
    ) {
        Icon(Icons.Filled.DoneAll, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(seenDate(date), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
    }
    }
}

@Composable
private fun GroupSeen(message: UiMessage, telegram: Telegram) {
    var viewers by remember(message.chatId, message.id) { mutableStateOf<List<TdApi.MessageViewer>>(emptyList()) }
    var reacted by remember(message.chatId, message.id) { mutableStateOf<List<TdApi.AddedReaction>>(emptyList()) }
    val hasReactions = message.reactions.isNotEmpty()
    LaunchedEffect(message.chatId, message.id) {
        if (message.isOutgoing) {
            telegram.send(TdApi.GetMessageViewers(message.chatId, message.id)) { result ->
                viewers = (result as? TdApi.MessageViewers)?.viewers?.toList().orEmpty()
            }
        }
        if (hasReactions) {
            telegram.send(TdApi.GetMessageAddedReactions(message.chatId, message.id, null, "", ReactionLimit)) { result ->
                reacted = (result as? TdApi.AddedReactions)?.reactions?.toList().orEmpty()
            }
        }
    }
    FooterCard(viewers.isNotEmpty() || reacted.isNotEmpty()) {
    val entries = remember(viewers, reacted) {
        val byUser = reacted.associateBy { (it.senderId as? TdApi.MessageSenderUser)?.userId }
        val fromReactions = reacted.map { SeenEntry(it.senderId, it.type, it.date) }
        val onlySeen = viewers.filter { it.userId !in byUser.keys }.map { SeenEntry(TdApi.MessageSenderUser(it.userId), null, it.viewDate) }
        fromReactions + onlySeen
    }
    val seen = maxOf(viewers.size, entries.size)
    val label = when {
        reacted.isNotEmpty() && viewers.isNotEmpty() -> tr("{a}/{b} Reacted").replace("{a}", reacted.size.toString()).replace("{b}", seen.toString())
        reacted.isNotEmpty() -> tr("{n} Reacted").replace("{n}", reacted.size.toString())
        else -> tr("{n} Seen").replace("{n}", viewers.size.toString())
    }
    var hovered by remember { mutableStateOf(false) }
    var overList by remember { mutableStateOf(false) }
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(hovered, overList) {
        if (hovered || overList) shown = true else {
            delay(CloseDelayMs)
            shown = false
        }
    }
    var width by remember { mutableStateOf(0) }
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = RowHeight)
                .onSizeChanged { width = it.width }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            when (event.type) {
                                PointerEventType.Enter -> hovered = true
                                PointerEventType.Exit -> hovered = false
                            }
                        }
                    }
                }
                .padding(horizontal = 12.dp),
        ) {
            val first = reacted.firstOrNull()?.type
            Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                if (first != null) ReactionMark(first) else Icon(Icons.Filled.DoneAll, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, modifier = Modifier.weight(1f))
            Box(Modifier.width(AvatarStep * (minOf(entries.size, MaxFaces) - 1) + FaceSize)) {
                entries.take(MaxFaces).forEachIndexed { index, entry ->
                    SenderFace(entry.sender, telegram, Modifier.offset(x = AvatarStep * index))
                }
            }
        }
        if (shown) {
            val gap = with(LocalDensity.current) { 6.dp.roundToPx() }
            Popup(alignment = Alignment.TopStart, offset = IntOffset(width + gap, 0)) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .width(ListWidth)
                        .heightIn(max = ListMaxHeight)
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    when (event.type) {
                                        PointerEventType.Enter -> overList = true
                                        PointerEventType.Exit -> overList = false
                                    }
                                }
                            }
                        },
                ) {
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(vertical = 6.dp)) {
                        entries.forEach { entry -> SeenRow(entry, telegram) }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun SeenRow(entry: SeenEntry, telegram: Telegram) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        SenderFace(entry.sender, telegram, Modifier, ListFace)
        Column(Modifier.weight(1f)) {
            EmojiText(telegram.senderTitle(entry.sender), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(
                    if (entry.reaction != null) Icons.Outlined.FavoriteBorder else Icons.Filled.DoneAll,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
                if (entry.date > 0) Text(seenDate(entry.date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        entry.reaction?.let { Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) { ReactionMark(it) } }
    }
}

@Composable
private fun SenderFace(sender: TdApi.MessageSender, telegram: Telegram, modifier: Modifier, size: androidx.compose.ui.unit.Dp = FaceSize) {
    val photo = when (sender) {
        is TdApi.MessageSenderUser -> telegram.users[sender.userId]?.profilePhoto?.small
        is TdApi.MessageSenderChat -> telegram.chats[sender.chatId]?.photo
        else -> null
    }
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = modifier) {
        AvatarImage(telegram.senderTitle(sender), photo, telegram, CircleShape, size, Modifier.padding(1.dp))
    }
}

@Composable
private fun ReactionMark(reaction: TdApi.ReactionType) {
    when (reaction) {
        is TdApi.ReactionTypeEmoji -> EmojiText(reaction.emoji, style = MaterialTheme.typography.titleMedium)
        is TdApi.ReactionTypeCustomEmoji -> CustomEmojiView(reaction.customEmojiId, "❤", Modifier.size(20.dp))
        is TdApi.ReactionTypePaid -> EmojiText("⭐", style = MaterialTheme.typography.titleMedium)
        else -> Icon(Icons.Outlined.FavoriteBorder, contentDescription = null, modifier = Modifier.size(18.dp))
    }
}

private fun seenDate(seconds: Int): String {
    val zone = java.time.ZoneId.systemDefault()
    val moment = java.time.Instant.ofEpochSecond(seconds.toLong()).atZone(zone)
    val today = java.time.LocalDate.now(zone)
    val time = moment.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
    val locale = java.util.Locale.forLanguageTag(com.pixelgram.desktop.core.I18n.code)
    return when (moment.toLocalDate()) {
        today -> tr("today at {time}").replace("{time}", time)
        today.minusDays(1) -> tr("yesterday at {time}").replace("{time}", time)
        else -> tr("{date} at {time}")
            .replace("{date}", moment.format(java.time.format.DateTimeFormatter.ofPattern(if (moment.year == today.year) "d MMM" else "d MMM yyyy", locale)))
            .replace("{time}", time)
    }
}

private val RowHeight = 44.dp
private val FaceSize = 24.dp
private val ListFace = 32.dp
private val AvatarStep = 16.dp
private val ListWidth = 280.dp
private val ListMaxHeight = 360.dp
private const val MaxFaces = 3
private const val ReactionLimit = 100
private const val CloseDelayMs = 250L
