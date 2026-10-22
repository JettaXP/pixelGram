package com.pixelgram.desktop.ui.main

import androidx.compose.material.icons.filled.Search
import com.pixelgram.desktop.telegram.isRich
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.animation.togetherWith
import androidx.compose.animation.AnimatedContent
import kotlinx.coroutines.delay
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.expandVertically
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.layout.Layout
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.Edit
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
    val seenNewest = remember(chat.id) { arrayOf(newest) }
    if (newest != seenNewest[0]) {
        seenNewest[0] = newest
        if (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < StickToBottomPx) {
            listState.requestScrollToItem(0)
        }
    }
    val attachments = remember(chat.id) { mutableStateListOf<File>() }
    var asFiles by remember(chat.id) { mutableStateOf(false) }
    var editPhoto by remember(chat.id) { mutableStateOf<File?>(null) }
    var editingPhoto by remember(chat.id) { mutableStateOf(false) }
    val attach = { files: List<File> ->
        if (editingPhoto) {
            files.firstOrNull()?.let {
                attachments.clear()
                attachments.add(it)
            }
        } else {
            files.filter { it !in attachments }.let(attachments::addAll)
        }
        if (files.any { !Attachments.isPhoto(it) }) asFiles = true
    }
    val placement = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
    val fade = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val slide = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val slidIn = remember(chat.id) { mutableSetOf<Long>() }
    var editing by remember(chat.id) { mutableStateOf<UiMessage?>(null) }
    var replyTo by remember(chat.id) { mutableStateOf<UiMessage?>(null) }
    var deleting by remember(chat.id) { mutableStateOf<UiMessage?>(null) }
    LaunchedEffect(editing?.key) {
        val message = editing
        if (message?.photo != null) {
            editingPhoto = true
            asFiles = false
            attachments.clear()
            editPhoto = telegram.photoPath(message)?.let(::File)?.also { attachments.add(it) }
        } else if (editingPhoto) {
            editingPhoto = false
            editPhoto = null
            attachments.clear()
        }
    }
    var highlight by remember(chat.id) { mutableStateOf<Pair<Long, Long>?>(null) }
    val actions = remember(chat.id) {
        MessageActions(
            onReply = { editing = null; replyTo = it },
            onEdit = { replyTo = null; editing = it },
            onDelete = { deleting = it },
            onCopyPhoto = { message -> telegram.photoPath(message)?.let(Attachments::copyImage) },
            onQuoteClick = { id ->
                val index = messages.indexOfFirst { it.id == id }
                if (index >= 0) {
                    scope.launch { listState.animateScrollToItem(index) }
                    highlight = id to System.nanoTime()
                }
            },
        )
    }
    LaunchedEffect(chat.id) { MessageSearch.lastChat = chat.id }
    LaunchedEffect(MessageSearch.jump, chat.id) {
        val (jumpChat, jumpId) = MessageSearch.jump ?: return@LaunchedEffect
        if (jumpChat != chat.id) return@LaunchedEffect
        repeat(JumpPages) {
            val index = messages.indexOfFirst { it.id == jumpId }
            if (index >= 0) {
                listState.animateScrollToItem(index)
                highlight = jumpId to System.nanoTime()
                MessageSearch.jump = null
                return@LaunchedEffect
            }
            telegram.loadMoreMessages(chat.id)
            delay(JumpPageDelayMs)
        }
        MessageSearch.jump = null
    }
    PluginViewer.path?.let { path -> PluginDialog(path, telegram) { PluginViewer.path = null } }
    deleting?.let { message ->
        val private = chat.privateUserId != 0L && !telegram.isSavedMessages(chat.id)
        DeleteMessageDialog(
            canRevoke = private || (message.isOutgoing && !telegram.isSavedMessages(chat.id)),
            revokeLabel = if (private) "Also delete for ${telegram.chatTitle(chat)}" else "Delete for everyone",
            onDismiss = { deleting = null },
            onConfirm = { revoke ->
                telegram.deleteMessages(chat.id, longArrayOf(message.id), revoke)
                if (editing?.id == message.id) editing = null
                if (replyTo?.id == message.id) replyTo = null
                deleting = null
            },
        )
    }

    AudioQueueEffects(telegram)
    Column(Modifier.fillMaxSize()) {
        MiniPlayer(telegram)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ChatAvatar(chat, telegram, settings, size = 40.dp)
            Column(Modifier.weight(1f)) {
                EmojiText(telegram.chatTitle(chat), style = MaterialTheme.typography.titleMedium)
                val status = telegram.presence(chat)
                AnimatedContent(status, transitionSpec = { fadeIn() togetherWith fadeOut() }) { text ->
                    if (text != null) {
                        Text(
                            text.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (text.online) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            IconButton(onClick = { MessageSearch.start(chat.id) }) {
                Icon(Icons.Filled.Search, contentDescription = "Search in chat")
            }
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
        Box(Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(
                state = listState,
                reverseLayout = true,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Bottom),
            ) {
                itemsIndexed(messages, key = { _, message -> message.key }) { index, message ->
                    val newDay = index == messages.lastIndex || !sameDay(message.date, messages[index + 1].date)
                    val rise = remember { Animatable(if (message.fresh && slidIn.add(message.key)) 1f else 0f) }
                    LaunchedEffect(Unit) { rise.animateTo(0f, slide) }
                    val flash = remember { Animatable(0f) }
                    LaunchedEffect(highlight) {
                        if (highlight?.first == message.id) {
                            flash.snapTo(1f)
                            delay(HighlightHoldMs)
                            flash.animateTo(0f, tween(HighlightFadeMs))
                        }
                    }
                    val flashColor = MaterialTheme.colorScheme.primary
                    val itemModifier = Modifier
                        .graphicsLayer { translationY = rise.value * size.height }
                        .drawBehind {
                            if (flash.value > 0f) {
                                drawRoundRect(
                                    flashColor.copy(alpha = HighlightAlpha * flash.value),
                                    cornerRadius = CornerRadius(HighlightCorner.toPx()),
                                )
                            }
                        }
                    val quote: (@Composable () -> Unit)? = if (message.replyToId != 0L) {
                        {
                            val replied = telegram.replyPreview(chat.id, message.replyToId)
                            ReplyQuote(
                                name = when {
                                    replied == null -> "Message"
                                    replied.isOutgoing -> "You"
                                    else -> telegram.senderName(replied)
                                },
                                text = replied?.text?.takeIf { it.isNotEmpty() } ?: if (replied?.photo != null) "Photo" else "…",
                                onClick = { actions.onQuoteClick(message.replyToId) },
                            )
                        }
                    } else {
                        null
                    }
                    Column(Modifier.animateItem(fadeInSpec = fade, placementSpec = placement, fadeOutSpec = fade)) {
                        if (newDay) DateDivider(message.date)
                        if (message.service) {
                            MessageBubble(message = message, senderName = null, settings = settings, modifier = itemModifier)
                        } else {
                            MessageGestures(message, actions, itemModifier) {
                                MessageBubble(
                                    telegram = telegram,
                                    quote = quote,
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
                    }
                }
            }
            FloatingDate(
                listState = listState,
                dateAt = { index -> messages.getOrNull(index)?.date },
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
            )
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
                onAdd = { scope.launch { attach(Attachments.pick()) } },
                onClose = { attachments.clear() },
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp),
            )
        }
        Composer(
            chatId = chat.id,
            hasAttachments = attachments.isNotEmpty(),
            editing = editing,
            replyTo = replyTo,
            onCancelReply = { replyTo = null },
            onEditLast = {
                val now = System.currentTimeMillis() / 1000
                val last = messages.firstOrNull {
                    it.isOutgoing && it.editable && !it.service && !it.sending && !it.failed
                }?.takeIf { now - it.date < EditWindowSeconds || telegram.isSavedMessages(chat.id) }
                editing = last
                last != null
            },
            onCancelEdit = { editing = null },
            onAttach = attach,
            onSend = { text ->
                val edited = editing
                val replyId = replyTo?.id ?: 0L
                replyTo = null
                if (edited != null && edited.photo != null) {
                    val file = attachments.firstOrNull()
                    when {
                        file == null -> Unit
                        file == editPhoto -> telegram.editMessage(edited, text)
                        else -> telegram.editMessageMedia(edited, file, text, !asFiles && Attachments.isPhoto(file))
                    }
                    editing = null
                } else if (edited != null) {
                    telegram.editMessage(edited, text)
                    editing = null
                } else if (attachments.isNotEmpty()) {
                    val photos = !asFiles && attachments.all(Attachments::isPhoto)
                    telegram.sendFiles(chat.id, attachments.toList(), photos, text, replyId)
                    attachments.clear()
                    asFiles = false
                } else {
                    telegram.sendText(chat.id, text, replyId)
                }
                if (listState.firstVisibleItemIndex > 0) {
                    scope.launch { listState.animateScrollToItem(0) }
                }
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
    quote: (@Composable () -> Unit)? = null,
) {
    val seconds = settings.messageSeconds.value
    if (message.service) {
        ServiceMessage(
            listOfNotNull(senderName?.takeIf { it.isNotBlank() }, message.text).joinToString(" · "),
            message.date,
            seconds,
            modifier,
        )
        return
    }
    val content = message.content
    if (telegram != null && isGiftContent(content)) {
        Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            giftServiceText(message, telegram)?.let { ServiceMessage(it, message.date, seconds, Modifier) }
            GiftCard(message, telegram, Modifier.padding(vertical = 4.dp))
        }
        return
    }
    if (telegram != null && isBareContent(content)) {
        BareMessage(message, telegram, status, seconds, quote, modifier)
        return
    }
    val colors = MaterialTheme.colorScheme
    val outgoing = message.isOutgoing
    val photo = message.photo
    val media = telegram != null && isMediaContent(content)
    val block = telegram != null && isRich(content) && !media
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
            val metaColor = (if (outgoing) colors.onPrimaryContainer else colors.onSurfaceVariant).copy(alpha = 0.7f)
            val readColor = if (outgoing) colors.primary else metaColor
            val hasText = message.text.isNotEmpty()
            Column(Modifier.padding(if (photo != null || media) PhotoInset else 0.dp).width(IntrinsicSize.Max)) {
                if (quote != null) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .widthIn(min = QuoteMinWidth)
                            .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = if (photo != null || media) 6.dp else 0.dp),
                    ) { quote() }
                }
                if (photo != null && telegram != null) {
                    MessagePhoto(
                        message,
                        photo,
                        telegram,
                        (settings.bubbleRadius.value.dp - PhotoInset).coerceAtLeast(4.dp),
                        overlay = if (hasText) {
                            null
                        } else {
                            { MessageMeta(message, status, settings.messageSeconds.value, Color.White, Color.White) }
                        },
                    )
                }
                if (media && telegram != null) {
                    MediaContent(message, telegram, PhotoMaxWidth, (settings.bubbleRadius.value.dp - PhotoInset).coerceAtLeast(4.dp))
                }
                if (hasText || photo == null) {
                    Column(
                        Modifier
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                            .then(if (photo != null || media) Modifier.width(PhotoMaxWidth - 24.dp) else Modifier),
                    ) {
                        if (!senderName.isNullOrEmpty()) {
                            Text(
                                senderName,
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.primary,
                            )
                        }
                        if (block && telegram != null) {
                            BlockContent(message, telegram, onPlugin = { PluginViewer.path = it })
                            Spacer(Modifier.height(4.dp))
                        }
                        TextWithMeta(
                            text = message.text,
                            meta = { MessageMeta(message, status, settings.messageSeconds.value, metaColor, readColor) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceMessage(text: String, date: Int, seconds: Boolean, modifier: Modifier) {
    Box(modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
            ) {
                EmojiText(text, style = MaterialTheme.typography.labelLarge)
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                    Text(
                        formatMessageTime(date, seconds),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun BareMessage(
    message: UiMessage,
    telegram: Telegram,
    status: MessageStatus?,
    seconds: Boolean,
    quote: (@Composable () -> Unit)?,
    modifier: Modifier,
) {
    Box(modifier.fillMaxWidth(), contentAlignment = if (message.isOutgoing) Alignment.CenterEnd else Alignment.CenterStart) {
        Column(
            horizontalAlignment = if (message.isOutgoing) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (quote != null) Box(Modifier.widthIn(min = QuoteMinWidth, max = 280.dp)) { quote() }
            BareContent(message, telegram)
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                Box(Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) {
                    MessageMeta(message, status, seconds, MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun MessageMeta(
    message: UiMessage,
    status: MessageStatus?,
    seconds: Boolean,
    color: Color,
    readColor: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        if (message.edited) {
            Icon(Icons.Outlined.Edit, contentDescription = "Edited", tint = color, modifier = Modifier.size(13.dp))
        }
        Text(formatMessageTime(message.date, seconds), style = MaterialTheme.typography.labelSmall, color = color)
        if (status != null) {
            MessageStatusIcon(status, color = if (status == MessageStatus.Read) readColor else color, size = 16.dp)
        }
    }
}

@Composable
private fun TextWithMeta(text: String, meta: @Composable () -> Unit) {
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    Layout(
        modifier = Modifier.fillMaxWidth(),
        content = {
            if (text.isNotEmpty()) {
                SelectionContainer {
                    EmojiText(text, style = MaterialTheme.typography.bodyLarge, onTextLayout = { textLayout = it })
                }
            } else {
                Box {}
            }
            meta()
        },
        measurePolicy = object : MeasurePolicy {
            override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
                val loose = constraints.copy(minWidth = 0)
                val textPlaceable = measurables[0].measure(loose)
                val metaPlaceable = measurables[1].measure(loose)
                val gap = MetaGap.roundToPx()
                val lines = textLayout?.takeIf { text.isNotEmpty() }
                val lastLineRight = lines?.let { it.getLineRight(it.lineCount - 1).toInt() } ?: 0
                val sameLine = text.isEmpty() || lastLineRight + gap + metaPlaceable.width <= constraints.maxWidth
                val width: Int
                val height: Int
                val metaX: Int
                val metaY: Int
                if (sameLine) {
                    width = maxOf(textPlaceable.width, lastLineRight + gap + metaPlaceable.width)
                        .coerceAtLeast(constraints.minWidth)
                    height = maxOf(textPlaceable.height, metaPlaceable.height)
                    metaX = width - metaPlaceable.width
                    metaY = height - metaPlaceable.height
                } else {
                    width = maxOf(textPlaceable.width, metaPlaceable.width).coerceAtLeast(constraints.minWidth)
                    height = textPlaceable.height + metaPlaceable.height
                    metaX = width - metaPlaceable.width
                    metaY = textPlaceable.height
                }
                return layout(width, height) {
                    textPlaceable.place(0, 0)
                    metaPlaceable.place(metaX, metaY)
                }
            }

            override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int) =
                measurables[0].maxIntrinsicWidth(height) + MetaGap.roundToPx() + measurables[1].maxIntrinsicWidth(height)

            override fun IntrinsicMeasureScope.minIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int) =
                maxOf(measurables[0].minIntrinsicWidth(height), measurables[1].minIntrinsicWidth(height))

            override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int) =
                measurables[0].maxIntrinsicHeight(width) + measurables[1].maxIntrinsicHeight(width)

            override fun IntrinsicMeasureScope.minIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int) =
                measurables[0].minIntrinsicHeight(width) + measurables[1].minIntrinsicHeight(width)
        },
    )
}

private val MetaGap = 10.dp
private const val JumpPages = 40
private const val JumpPageDelayMs = 300L

object PluginViewer {
    var path by mutableStateOf<String?>(null)
}
private const val StickToBottomPx = 48
private val HighlightCorner = 12.dp
private const val HighlightAlpha = 0.22f
private const val HighlightHoldMs = 500L
private const val HighlightFadeMs = 1200
private val QuoteMinWidth = 160.dp
private const val EditWindowSeconds = 48 * 3600

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
private fun MessagePhoto(
    message: UiMessage,
    file: TdApi.File,
    telegram: Telegram,
    corner: Dp,
    overlay: (@Composable () -> Unit)?,
) {
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
        if (overlay != null) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.45f),
                contentColor = Color.White,
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
            ) {
                Box(Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) { overlay() }
            }
        }
    }
}

private const val PhotoPixels = 1024

@Composable
private fun Composer(
    chatId: Long,
    hasAttachments: Boolean,
    editing: UiMessage?,
    replyTo: UiMessage?,
    onCancelReply: () -> Unit,
    onEditLast: () -> Boolean,
    onCancelEdit: () -> Unit,
    onAttach: (List<File>) -> Unit,
    onSend: (String) -> Unit,
) {
    var text by remember { mutableStateOf(TextFieldValue("")) }
    var emojiOpen by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val pickScope = rememberCoroutineScope()
    LaunchedEffect(chatId) { runCatching { focus.requestFocus() } }
    LaunchedEffect(editing?.key) {
        if (editing != null) {
            text = TextFieldValue(editing.text, TextRange(editing.text.length))
            focus.requestFocus()
        }
    }
    val canSend = if (editing?.photo != null) hasAttachments else text.text.isNotBlank() || hasAttachments
    val send = {
        if (canSend) {
            onSend(text.text.trim())
            text = TextFieldValue("")
        }
    }
    LaunchedEffect(replyTo?.key) {
        if (replyTo != null) focus.requestFocus()
    }
    val cancelEdit = {
        onCancelEdit()
        text = TextFieldValue("")
    }
    val shown = remember { MutableTransitionState(false) }
    shown.targetState = editing != null || replyTo != null
    var banner by remember { mutableStateOf<Pair<Boolean, UiMessage>?>(null) }
    if (editing != null) banner = true to editing else if (replyTo != null) banner = false to replyTo
    AnimatedVisibility(
        visibleState = shown,
        enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeIn(),
        exit = shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeOut(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val isEdit = banner?.first == true
            Icon(
                if (isEdit) Icons.Outlined.Edit else Icons.AutoMirrored.Outlined.Reply,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    if (isEdit) "Editing" else "Reply",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                EmojiText(
                    banner?.second?.text?.takeIf { it.isNotEmpty() } ?: "Photo",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = { if (editing != null) cancelEdit() else onCancelReply() }) {
                Icon(Icons.Filled.Close, contentDescription = "Cancel")
            }
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
        IconButton(onClick = { pickScope.launch { onAttach(Attachments.pick()) } }) {
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
                        event.key == Key.DirectionUp && text.text.isEmpty() && !hasAttachments && editing == null ->
                            onEditLast()
                        event.key == Key.Escape && editing != null -> {
                            cancelEdit()
                            true
                        }
                        event.key == Key.Escape && replyTo != null -> {
                            onCancelReply()
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
            Crossfade(editing != null) { isEditing ->
                if (isEditing) {
                    Icon(Icons.Filled.Check, contentDescription = "Save")
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }
        }
    }
}
