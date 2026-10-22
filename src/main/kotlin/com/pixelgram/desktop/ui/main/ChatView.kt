package com.pixelgram.desktop.ui.main

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.telegram.ChatState
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.telegram.UiMessage
import com.pixelgram.desktop.ui.components.ChatAvatar
import com.pixelgram.desktop.ui.profile.PersonalChannelCard
import com.pixelgram.desktop.ui.theme.EmojiText
import com.pixelgram.desktop.ui.theme.EmojiVisualTransformation
import kotlinx.coroutines.launch

@Composable
fun ChatView(chat: ChatState, telegram: Telegram, settings: AppSettings, onOpenChat: (Long) -> Unit) {
    DisposableEffect(chat.id) {
        telegram.openChat(chat.id)
        onDispose { telegram.closeChat(chat.id) }
    }
    val messages = telegram.messages(chat.id)
    val listState = rememberLazyListState()
    val nearTop by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 5
        }
    }
    LaunchedEffect(nearTop, messages.size) {
        if (nearTop) telegram.loadMoreMessages(chat.id)
    }
    // Follow the newest message while you are at the bottom, and always after sending.
    val newest = messages.firstOrNull()?.key
    val scope = rememberCoroutineScope()
    LaunchedEffect(newest) {
        if (newest != null && listState.firstVisibleItemIndex <= 1) listState.animateScrollToItem(0)
    }
    val placement = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
    val fade = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val slide = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    // New messages slide in once; scrolling back to them later does not replay it.
    val slidIn = remember(chat.id) { mutableSetOf<Long>() }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ChatAvatar(chat, telegram, settings, size = 40.dp)
            EmojiText(telegram.chatTitle(chat), style = MaterialTheme.typography.titleMedium)
        }
        var personalChannel by remember(chat.id) { mutableLongStateOf(0L) }
        LaunchedEffect(chat.id) {
            if (chat.privateUserId != 0L && !telegram.isSavedMessages(chat.id)) {
                telegram.userFullInfo(chat.privateUserId) { personalChannel = it?.personalChatId ?: 0L }
            }
        }
        if (personalChannel != 0L) {
            PersonalChannelCard(
                personalChannel,
                telegram,
                settings,
                onOpenChat,
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
        }
        LazyColumn(
            state = listState,
            reverseLayout = true,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            // Short chats sit at the bottom, next to the composer.
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Bottom),
        ) {
            items(messages, key = { it.key }) { message ->
                val rise = remember { Animatable(if (message.fresh && slidIn.add(message.key)) 1f else 0f) }
                LaunchedEffect(Unit) { rise.animateTo(0f, slide) }
                MessageBubble(
                    modifier = Modifier
                        .animateItem(fadeInSpec = fade, placementSpec = placement, fadeOutSpec = fade)
                        .graphicsLayer { translationY = rise.value * size.height },
                    message = message,
                    senderName = if (chat.isGroup && !message.isOutgoing) {
                        telegram.senderName(message)
                    } else {
                        null
                    },
                    settings = settings,
                )
            }
        }
        Composer(onSend = {
            telegram.sendText(chat.id, it)
            scope.launch { listState.animateScrollToItem(0) }
        })
    }
}

fun bubbleShape(radius: Int, tailRadius: Int, outgoing: Boolean): Shape {
    val r = radius.dp
    val tail = tailRadius.dp
    return if (outgoing) {
        RoundedCornerShape(topStart = r, topEnd = r, bottomEnd = tail, bottomStart = r)
    } else {
        RoundedCornerShape(topStart = r, topEnd = r, bottomEnd = r, bottomStart = tail)
    }
}

@Composable
fun MessageBubble(
    message: UiMessage,
    senderName: String?,
    settings: AppSettings,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val outgoing = message.isOutgoing
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = if (outgoing) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Surface(
            shape = bubbleShape(
                settings.bubbleRadius.value,
                settings.bubbleTailRadius.value,
                outgoing,
            ),
            color = if (outgoing) colors.primaryContainer else colors.surfaceContainerHigh,
            contentColor = if (outgoing) colors.onPrimaryContainer else colors.onSurface,
            modifier = Modifier.widthIn(max = 520.dp),
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                if (!senderName.isNullOrEmpty()) {
                    Text(
                        senderName,
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.primary,
                    )
                }
                SelectionContainer {
                    EmojiText(message.text, style = MaterialTheme.typography.bodyLarge)
                }
                Text(
                    formatTime(message.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = (if (outgoing) colors.onPrimaryContainer else colors.onSurfaceVariant)
                        .copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}

@Composable
private fun Composer(onSend: (String) -> Unit) {
    var text by remember { mutableStateOf(TextFieldValue("")) }
    var emojiOpen by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val send = {
        if (text.text.isNotBlank()) {
            onSend(text.text.trim())
            text = TextFieldValue("")
        }
    }
    val insert: (String) -> Unit = { emoji ->
        val start = text.selection.min
        val updated = text.text.replaceRange(start, text.selection.max, emoji)
        text = TextFieldValue(updated, TextRange(start + emoji.length))
        focus.requestFocus()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("Message") },
            visualTransformation = EmojiVisualTransformation,
            maxLines = 6,
            shape = RoundedCornerShape(28.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focus)
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown &&
                        event.key == Key.Enter &&
                        !event.isShiftPressed
                    ) {
                        send()
                        true
                    } else {
                        false
                    }
                },
        )
        Box {
            IconToggleButton(checked = emojiOpen, onCheckedChange = { emojiOpen = it }) {
                Icon(Icons.Outlined.EmojiEmotions, contentDescription = "Emoji")
            }
            if (emojiOpen) {
                Popup(
                    alignment = Alignment.BottomEnd,
                    offset = IntOffset(0, -with(LocalDensity.current) { 56.dp.roundToPx() }),
                    onDismissRequest = { emojiOpen = false },
                    properties = PopupProperties(focusable = false),
                ) {
                    EmojiPanel(onPick = insert)
                }
            }
        }
        FilledIconButton(onClick = send, enabled = text.text.isNotBlank()) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
        }
    }
}
