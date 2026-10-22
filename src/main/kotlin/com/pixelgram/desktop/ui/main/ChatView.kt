package com.pixelgram.desktop.ui.main

import com.pixelgram.desktop.telegram.MessageStatus
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Done
import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LoadingIndicator
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.telegram.ChatState
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.telegram.UiMessage
import com.pixelgram.desktop.ui.components.ChatAvatar
import com.pixelgram.desktop.ui.components.loadImage
import com.pixelgram.desktop.ui.profile.PersonalChannelCard
import com.pixelgram.desktop.ui.theme.EmojiText
import com.pixelgram.desktop.ui.theme.EmojiVisualTransformation
import it.tdlight.jni.TdApi
import java.io.File
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
    val newest = messages.firstOrNull()?.key
    val scope = rememberCoroutineScope()
    LaunchedEffect(newest) {
        if (newest != null && listState.firstVisibleItemIndex <= 1) listState.animateScrollToItem(0)
    }
    val attachments = remember(chat.id) { mutableStateListOf<File>() }
    var asFiles by remember(chat.id) { mutableStateOf(false) }
    val attach = { files: List<File> ->
        files.filter { it !in attachments }.let(attachments::addAll)
        if (files.any { !Attachments.isPhoto(it) }) asFiles = true
    }
    val placement = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
    val fade = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val slide = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
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
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Bottom),
        ) {
            items(messages, key = { it.key }) { message ->
                val rise = remember { Animatable(if (message.fresh && slidIn.add(message.key)) 1f else 0f) }
                LaunchedEffect(Unit) { rise.animateTo(0f, slide) }
                MessageBubble(
                    telegram = telegram,
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
                    status = if (message.isOutgoing && !chat.isChannel) telegram.status(chat, message) else null,
                )
            }
        }
        val panelSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
        val panelFade = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
        AnimatedVisibility(
            visible = attachments.isNotEmpty(),
            enter = slideInVertically(panelSpec) { it } + fadeIn(panelFade),
            exit = slideOutVertically(panelSpec) { it } + fadeOut(panelFade),
        ) {
            AttachmentsPanel(
                files = attachments.toList(),
                asFiles = asFiles,
                onAsFiles = { asFiles = it },
                onRemove = { attachments.remove(it) },
                onAdd = { attach(Attachments.pick()) },
                onClose = { attachments.clear() },
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp),
            )
        }
        Composer(
            hasAttachments = attachments.isNotEmpty(),
            onAttach = attach,
            onSend = { text ->
                if (attachments.isNotEmpty()) {
                    val photos = !asFiles && attachments.all(Attachments::isPhoto)
                    telegram.sendFiles(chat.id, attachments.toList(), photos, text)
                    attachments.clear()
                    asFiles = false
                } else {
                    telegram.sendText(chat.id, text)
                }
                scope.launch { listState.animateScrollToItem(0) }
            },
        )
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
    telegram: Telegram? = null,
    status: MessageStatus? = null,
) {
    val colors = MaterialTheme.colorScheme
    val outgoing = message.isOutgoing
    val photo = message.photo
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
            Column(Modifier.padding(if (photo != null) PhotoInset else 0.dp)) {
                if (photo != null && telegram != null) {
                    MessagePhoto(message, photo, telegram, (settings.bubbleRadius.value.dp - PhotoInset).coerceAtLeast(4.dp))
                }
                Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    if (!senderName.isNullOrEmpty()) {
                        Text(
                            senderName,
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.primary,
                        )
                    }
                    if (message.text.isNotEmpty()) {
                        SelectionContainer {
                            EmojiText(message.text, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    val metaColor = (if (outgoing) colors.onPrimaryContainer else colors.onSurfaceVariant)
                        .copy(alpha = 0.7f)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text(
                            formatTime(message.date),
                            style = MaterialTheme.typography.labelSmall,
                            color = metaColor,
                        )
                        if (status != null) {
                            MessageStatusIcon(
                                status,
                                color = if (status == MessageStatus.Read) colors.primary else metaColor,
                                size = 16.dp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageStatusIcon(status: MessageStatus, color: Color, size: Dp, modifier: Modifier = Modifier) {
    val icon = when (status) {
        MessageStatus.Sending -> Icons.Outlined.Schedule
        MessageStatus.Sent -> Icons.Filled.Done
        MessageStatus.Read -> Icons.Filled.DoneAll
        MessageStatus.Failed -> Icons.Outlined.ErrorOutline
    }
    Crossfade(status, modifier = modifier) {
        Icon(
            icon,
            contentDescription = status.name,
            tint = if (it == MessageStatus.Failed) MaterialTheme.colorScheme.error else color,
            modifier = Modifier.size(size),
        )
    }
}

private val PhotoInset = 4.dp
private val PhotoMaxWidth = 360.dp

@Composable
private fun MessagePhoto(message: UiMessage, file: TdApi.File, telegram: Telegram, corner: Dp) {
    LaunchedEffect(file.id) { telegram.download(file) }
    val path = telegram.localPath(file)
    val image by produceState<ImageBitmap?>(null, path) {
        path?.let { loadImage(it, maxSize = PhotoPixels) }?.let { value = it }
    }
    val ratio = if (message.photoWidth > 0 && message.photoHeight > 0) {
        message.photoWidth.toFloat() / message.photoHeight
    } else {
        4f / 3f
    }
    val width = PhotoMaxWidth
    val height = (width / ratio).coerceIn(120.dp, 420.dp)
    Box(
        Modifier
            .size(width, height)
            .clip(RoundedCornerShape(corner))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable { PhotoViewer.current = ViewedPhoto(telegram, file, message.fullPhoto ?: file, message.text) },
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = image
        if (bitmap != null) {
            Image(bitmap, contentDescription = "Photo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            LoadingIndicator()
        }
    }
}

private const val PhotoPixels = 1024

@Composable
private fun Composer(
    hasAttachments: Boolean,
    onAttach: (List<File>) -> Unit,
    onSend: (String) -> Unit,
) {
    var text by remember { mutableStateOf(TextFieldValue("")) }
    var emojiOpen by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val canSend = text.text.isNotBlank() || hasAttachments
    val send = {
        if (canSend) {
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
        IconButton(onClick = { onAttach(Attachments.pick()) }) {
            Icon(Icons.Outlined.AttachFile, contentDescription = "Attach files")
        }
        TextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text(if (hasAttachments) "Add a caption" else "Message") },
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
                    when {
                        event.type != KeyEventType.KeyDown -> false
                        event.key == Key.Enter && !event.isShiftPressed -> {
                            send()
                            true
                        }
                        event.key == Key.V && (event.isCtrlPressed || event.isMetaPressed) -> {
                            val pasted = Attachments.fromClipboard()
                            if (pasted.isNotEmpty()) onAttach(pasted)
                            pasted.isNotEmpty()
                        }
                        else -> false
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
        FilledIconButton(onClick = send, enabled = canSend) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
        }
    }
}
