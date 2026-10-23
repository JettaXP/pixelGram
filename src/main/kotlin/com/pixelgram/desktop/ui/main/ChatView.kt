package com.pixelgram.desktop.ui.main

import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.expandHorizontally
import androidx.compose.material.icons.outlined.KeyboardHide
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material3.FilledTonalButton
import com.pixelgram.desktop.telegram.BotCommandItem
import com.pixelgram.desktop.ui.components.Avatar
import com.pixelgram.desktop.core.AvatarKind
import androidx.compose.ui.graphics.luminance
import com.materialkolor.hct.Hct
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Visibility
import com.pixelgram.desktop.telegram.contentText
import com.pixelgram.desktop.core.DoubleClickAction
import androidx.compose.ui.unit.IntSize
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.outlined.Forward
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.unit.DpOffset
import com.pixelgram.desktop.ui.components.FormattedText
import com.pixelgram.desktop.ui.components.endsWithQuote
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
import com.pixelgram.desktop.ui.theme.EmojiText
import it.tdlight.jni.TdApi
import java.io.File
import kotlinx.coroutines.launch
import com.pixelgram.desktop.ui.OnEscape
import androidx.compose.material3.toShape
import androidx.compose.material3.MaterialShapes
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.runtime.withFrameNanos
import androidx.compose.animation.scaleOut
import androidx.compose.animation.scaleIn
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.material3.TextButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.AlertDialog
import com.pixelgram.desktop.core.PowerSaving
import com.pixelgram.desktop.ui.components.OfficialBadge
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle

@Composable
fun ChatView(
    chat: ChatState,
    telegram: Telegram,
    settings: AppSettings,
    onOpenChat: (Long) -> Unit,
    onBack: (() -> Unit)? = null,
) {
    DisposableEffect(chat.id) {
        telegram.openChat(chat.id)
        onDispose { telegram.closeChat(chat.id) }
    }
    val messages = telegram.messages(chat.id)
    val shown by remember(chat.id) { derivedStateOf { groupAlbums(messages.toList()) } }
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
    val nearBottom by remember { derivedStateOf { listState.firstVisibleItemIndex < NewerThreshold } }
    val detached = telegram.detached[chat.id] == true
    LaunchedEffect(nearBottom, detached, messages.size) {
        if (nearBottom && detached) telegram.loadNewer(chat.id)
    }
    val newest = messages.firstOrNull()?.key
    val scope = rememberCoroutineScope()
    val seenNewest = remember(chat.id) { arrayOf(newest) }
    if (newest != seenNewest[0]) {
        seenNewest[0] = newest
        val own = messages.firstOrNull()?.let { it.isOutgoing && !it.service } == true
        if (own && listState.firstVisibleItemIndex > 0) {
            scope.launch {
                if (listState.firstVisibleItemIndex > JumpCloserItems) listState.scrollToItem(JumpLandingItem)
                listState.animateScrollToItem(0)
            }
        } else if (own || listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < StickToBottomPx) {
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
    var forwarding by remember(chat.id) { mutableStateOf<UiMessage?>(null) }
    var profileChat by remember(chat.id) { mutableStateOf<ChatState?>(null) }
    val openOrigin = { origin: TdApi.MessageOrigin ->
        when (origin) {
            is TdApi.MessageOriginUser -> telegram.openPrivateChat(origin.senderUserId) { id -> profileChat = telegram.chats[id] }
            is TdApi.MessageOriginChannel -> onOpenChat(origin.chatId)
            is TdApi.MessageOriginChat -> onOpenChat(origin.senderChatId)
            else -> Unit
        }
    }
    var botAnswer by remember(chat.id) { mutableStateOf<Pair<String, Boolean>?>(null) }
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
    var pinning by remember(chat.id) { mutableStateOf<UiMessage?>(null) }
    var commentsFor by remember(chat.id) { mutableStateOf<UiMessage?>(null) }
    val selected = remember(chat.id) { mutableStateListOf<Long>() }
    val selecting = selected.isNotEmpty()
    var deletingSelected by remember(chat.id) { mutableStateOf(false) }
    var forwardingSelected by remember(chat.id) { mutableStateOf(false) }
    var shot by remember(chat.id) { mutableStateOf(false) }
    val selectedMessages = { shown.filter { it.key in selected } }
    val selectedIds = { selectedMessages().flatMap { message -> message.album.takeIf { it.isNotEmpty() }?.map { it.id } ?: listOf(message.id) }.toLongArray() }
    val itemBounds = remember(chat.id) { HashMap<Long, androidx.compose.ui.geometry.Rect>() }
    var listOrigin by remember(chat.id) { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    if (selecting) OnEscape {
        selected.clear()
        true
    }
    LaunchedEffect(chat.id) { telegram.loadPinned(chat.id) }
    var pinIndex by remember(chat.id) { mutableStateOf(0) }
    val canReact = !telegram.isSavedMessages(chat.id) || telegram.me?.isPremium == true
    val actions = remember(chat.id, canReact) {
        MessageActions(
            onReply = { editing = null; replyTo = it },
            onEdit = { replyTo = null; editing = it },
            onDelete = { deleting = it },
            onCopyPhoto = { message -> telegram.photoPath(message)?.let(Attachments::copyImage) },
            onForward = { forwarding = it },
            onReact = { message, emoji -> telegram.toggleReaction(message, emoji) },
            loadReactions = { message, done -> telegram.availableReactions(message, done) },
            canReact = canReact,
            onPin = { message -> if (message.pinned) telegram.togglePin(message) else pinning = message },
            onSelect = { message -> if (selected.size < SelectionLimit && message.key !in selected) selected.add(message.key) },
            onDoubleClick = { message ->
                val action = if (message.isOutgoing) settings.doubleClickOwn.value else settings.doubleClickOther.value
                when (action) {
                    DoubleClickAction.Reply -> {
                        editing = null
                        replyTo = message
                    }
                    DoubleClickAction.Reaction -> if (canReact) telegram.toggleReaction(message, settings.doubleClickEmoji.value)
                    DoubleClickAction.Nothing -> Unit
                }
            },
            onQuoteClick = { id ->
                val index = shown.indexOfFirst { it.id == id || it.album.any { member -> member.id == id } }
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
            val index = shown.indexOfFirst { it.id == jumpId || it.album.any { member -> member.id == jumpId } }
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
    forwarding?.let { message ->
        ForwardDialog(telegram, settings, onDismiss = { forwarding = null }) { target ->
            telegram.forwardMessages(target, message.chatId, longArrayOf(message.id))
            forwarding = null
            onOpenChat(target)
        }
    }
    pinning?.let { message ->
        val saved = telegram.isSavedMessages(chat.id)
        val private = chat.privateUserId != 0L && !saved
        var also by remember(message.key) { mutableStateOf(true) }
        AlertDialog(
            onDismissRequest = { pinning = null },
            icon = { Icon(Icons.Outlined.PushPin, contentDescription = null) },
            title = { Text("Pin message") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Would you like to pin this message?")
                    if (!saved) {
                        val label = when {
                            private -> "Also pin for ${telegram.chatTitle(chat)}"
                            chat.isChannel -> "Notify all subscribers"
                            else -> "Notify all members"
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { also = !also },
                        ) {
                            Checkbox(checked = also, onCheckedChange = { also = it })
                            EmojiText(label, modifier = Modifier.padding(end = 12.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (private) telegram.togglePin(message, notify = true, onlyForSelf = !also) else telegram.togglePin(message, notify = also)
                    pinning = null
                }) { Text("Pin") }
            },
            dismissButton = { TextButton(onClick = { pinning = null }) { Text("Cancel") } },
        )
    }
    if (forwardingSelected) {
        ForwardDialog(telegram, settings, onDismiss = { forwardingSelected = false }) { target ->
            telegram.forwardMessages(target, chat.id, selectedIds())
            forwardingSelected = false
            selected.clear()
            onOpenChat(target)
        }
    }
    if (deletingSelected) {
        val private = chat.privateUserId != 0L && !telegram.isSavedMessages(chat.id)
        DeleteMessageDialog(
            canRevoke = private || (selectedMessages().all { it.isOutgoing } && !telegram.isSavedMessages(chat.id)),
            revokeLabel = if (private) "Also delete for ${telegram.chatTitle(chat)}" else "Delete for everyone",
            onDismiss = { deletingSelected = false },
            onConfirm = { revoke ->
                telegram.deleteMessages(chat.id, selectedIds(), revoke)
                deletingSelected = false
                selected.clear()
            },
        )
    }
    if (shot) {
        ShotDialog(selectedMessages().reversed(), chat, telegram, settings, onDismiss = { shot = false })
    }
    PluginViewer.path?.let { path -> PluginDialog(path, telegram) { PluginViewer.path = null } }
    BackupViewer.path?.let { path -> BackupImportDialog(java.io.File(path), settings) { BackupViewer.path = null } }
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

    FileDropArea(onDrop = { files, asDocuments ->
        attach(files)
        asFiles = asDocuments || files.any { !Attachments.isPhoto(it) }
    }) {
    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize()) {
        MiniPlayer(telegram)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AnimatedVisibility(
                visible = onBack != null,
                enter = expandHorizontally(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeIn(),
                exit = shrinkHorizontally(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeOut(),
            ) {
                IconButton(onClick = { onBack?.invoke() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable { if (!telegram.isSavedMessages(chat.id)) profileChat = chat }
                    .padding(end = 8.dp),
            ) {
            ChatAvatar(chat, telegram, settings, size = 40.dp)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EmojiText(telegram.chatTitle(chat), style = MaterialTheme.typography.titleMedium)
                    if (telegram.isOfficial(chat)) OfficialBadge(22.dp)
                }
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
            }
            IconButton(onClick = { MessageSearch.start(chat.id) }) {
                Icon(Icons.Filled.Search, contentDescription = "Search in chat")
            }
            Box {
                var headerMenu by remember(chat.id) { mutableStateOf<DpOffset?>(null) }
                IconButton(onClick = { headerMenu = DpOffset(0.dp, 0.dp) }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Chat menu")
                }
                ChatActions(
                    chat,
                    telegram,
                    settings,
                    headerMenu,
                    onDismiss = { headerMenu = null },
                    onOpenChat = onOpenChat,
                    onSearch = { MessageSearch.start(chat.id) },
                    onToBeginning = {
                        telegram.jumpToStart(chat.id) {
                            scope.launch {
                                withFrameNanos { }
                                if (shown.isNotEmpty()) listState.scrollToItem(shown.lastIndex)
                            }
                        }
                    },
                )
            }
        }
        val pins = telegram.pinnedMessages[chat.id].orEmpty()
        AnimatedVisibility(
            visible = pins.isNotEmpty(),
            enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeIn(),
            exit = shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeOut(),
        ) {
            val index = pinIndex.coerceIn(0, (pins.size - 1).coerceAtLeast(0))
            PinnedBar(
                pins,
                index,
                onClick = {
                    pins.getOrNull(index)?.let { MessageSearch.jump = chat.id to it.id }
                    pinIndex = if (pins.isEmpty()) 0 else (index + 1) % pins.size
                },
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            )
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .onGloballyPositioned { listOrigin = it.positionInWindow() }
                .pointerInput(selecting) {
                    if (!selecting) return@pointerInput
                    val keyAt = { position: androidx.compose.ui.geometry.Offset ->
                        val point = position + listOrigin
                        shown.firstOrNull { message -> !message.service && itemBounds[message.key]?.let { point.y >= it.top && point.y < it.bottom } == true }?.key
                    }
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        if (!currentEvent.buttons.isPrimaryPressed) return@awaitEachGesture
                        val start = keyAt(down.position) ?: return@awaitEachGesture
                        down.consume()
                        val base = selected.toList()
                        val wasSelected = start in base
                        var moved = false
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) {
                                change.consume()
                                break
                            }
                            change.consume()
                            if (change.position.y < SelectEdgePx) listState.dispatchRawDelta(SelectEdgePx - change.position.y)
                            if (change.position.y > size.height - SelectEdgePx) listState.dispatchRawDelta(size.height - SelectEdgePx - change.position.y)
                            val current = keyAt(change.position) ?: continue
                            if (current == start && !moved) continue
                            moved = true
                            val from = shown.indexOfFirst { it.key == start }
                            val to = shown.indexOfFirst { it.key == current }
                            if (from < 0 || to < 0) continue
                            val range = shown.subList(minOf(from, to), maxOf(from, to) + 1).filter { !it.service }.map { it.key }
                            selected.clear()
                            selected.addAll((base + range).distinct().take(SelectionLimit))
                        }
                        if (!moved) {
                            if (wasSelected) {
                                selected.remove(start)
                            } else if (selected.size < SelectionLimit) {
                                selected.add(start)
                            }
                        }
                    }
                },
        ) {
            LazyColumn(
                state = listState,
                reverseLayout = true,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Bottom),
            ) {
                itemsIndexed(shown, key = { _, message -> message.key }) { index, message ->
                    val newDay = index == shown.lastIndex || !sameDay(message.date, shown[index + 1].date)
                    val rise = remember { Animatable(if (PowerSaving.messageAnimations && message.fresh && slidIn.add(message.key)) 1f else 0f) }
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
                                text = replied?.text?.takeIf { it.isNotEmpty() } ?: if (replied?.photo != null) "Photo" else replied?.let { contentText(it.content) }?.ifBlank { null } ?: "…",
                                onClick = { actions.onQuoteClick(message.replyToId) },
                                replied = replied,
                                telegram = telegram,
                            )
                        }
                    } else {
                        null
                    }
                    Column(
                        if (PowerSaving.messageAnimations) {
                            Modifier.animateItem(fadeInSpec = fade, placementSpec = if (listState.isScrollInProgress) null else placement, fadeOutSpec = fade)
                        } else {
                            Modifier
                        },
                    ) {
                        if (newDay) DateDivider(message.date)
                        if (message.service) {
                            val pin = message.content as? TdApi.MessagePinMessage
                            if (pin != null) {
                                val target = telegram.replyPreview(chat.id, pin.messageId)
                                val who = if (message.isOutgoing) "You" else telegram.senderName(message)
                                val what = target?.let { "\"${pinPreview(it)}\"" } ?: "a message"
                                MessageBubble(
                                    message = message.copy(text = "$who pinned $what"),
                                    senderName = null,
                                    settings = settings,
                                    modifier = itemModifier
                                        .pointerHoverIcon(PointerIcon.Hand)
                                        .clickable { actions.onQuoteClick(pin.messageId) },
                                )
                            } else {
                                MessageBubble(message = message, senderName = null, settings = settings, modifier = itemModifier)
                            }
                        } else {
                            val isSelected = message.key in selected
                            val selectTint = MaterialTheme.colorScheme.primary
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .onGloballyPositioned { itemBounds[message.key] = it.boundsInWindow() }
                                    .clip(RoundedCornerShape(16.dp))
                                    .drawBehind { if (isSelected) drawRect(selectTint.copy(alpha = SelectedAlpha)) },
                            ) {
                            AnimatedVisibility(
                                visible = selecting,
                                enter = expandHorizontally(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeIn(),
                                exit = shrinkHorizontally(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeOut(),
                            ) {
                                Icon(
                                    if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                                    contentDescription = null,
                                    tint = if (isSelected) selectTint else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(horizontal = 8.dp).size(22.dp),
                                )
                            }
                            Box(Modifier.weight(1f)) {
                            val groupChat = chat.isGroup && !chat.isChannel && !message.isOutgoing
                            val sentAsChat = chat.isGroup && !chat.isChannel && message.isOutgoing &&
                                message.senderChatId != 0L && message.senderChatId != chat.id ||
                                chat.isChannel && message.senderUserId != 0L
                            val firstInRun = !sameSender(shown.getOrNull(index + 1), message)
                            val lastInRun = !sameSender(shown.getOrNull(index - 1), message)
                            MessageGestures(message, actions, itemModifier) {
                              Row(verticalAlignment = Alignment.Bottom) {
                                if (groupChat) {
                                    Box(Modifier.width(SenderAvatarSlot)) {
                                        if (lastInRun) {
                                            SenderAvatar(message, telegram, settings) {
                                                if (message.senderUserId != 0L) {
                                                    telegram.openPrivateChat(message.senderUserId) { id -> profileChat = telegram.chats[id] }
                                                } else {
                                                    onOpenChat(message.senderChatId)
                                                }
                                            }
                                        }
                                    }
                                }
                                MessageBubble(
                                    telegram = telegram,
                                    quote = quote,
                                    message = message,
                                    modifier = Modifier.weight(1f),
                                    senderName = if ((groupChat || sentAsChat) && firstInRun) telegram.senderName(message) else null,
                                    settings = settings,
                                    status = if (message.isOutgoing && !chat.isChannel) telegram.status(chat, message) else null,
                                    onReact = { emoji -> telegram.toggleReaction(message, emoji) },
                                    onButton = { button ->
                                        telegram.pressButton(message, button) { text, alert -> botAnswer = text to alert }
                                    },
                                    onOrigin = openOrigin,
                                    onVia = { bot -> ComposerInsert.request = chat.id to "$bot " },
                                    onComments = if (chat.isChannel) {
                                        { commentsFor = message }
                                    } else {
                                        null
                                    },
                                )
                              }
                            }
                            }
                            }
                        }
                    }
                }
            }
            profileChat?.let { ChatInfoDialog(it, telegram, settings, onOpenChat) { profileChat = null } }
            StickerPack.current?.takeIf { it.chatId == chat.id && it.telegram == telegram }?.let { request ->
                StickerPackDialog(request, settings) { StickerPack.current = null }
            }
            BotAnswer(botAnswer, onDismiss = { botAnswer = null }, modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp))
            FloatingDate(
                listState = listState,
                dateAt = { index -> shown.getOrNull(index)?.date },
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
            )
            ScrollDownButton(
                listState,
                Modifier.align(Alignment.BottomEnd).padding(16.dp),
                forceVisible = detached,
                onLatest = if (detached) {
                    { telegram.returnToLatest(chat.id) { scope.launch { listState.scrollToItem(0) } } }
                } else {
                    null
                },
            )
            if (chat.privateUserId != 0L && telegram.isBot(chat)) {
                var welcome by remember(chat.id) { mutableStateOf(false) }
                val empty = shown.isEmpty()
                LaunchedEffect(chat.id, empty) {
                    welcome = false
                    if (!empty) return@LaunchedEffect
                    delay(WelcomeDelayMs)
                    welcome = true
                }
                if (welcome) {
                    BotWelcome(
                        chat.privateUserId,
                        telegram,
                        onStart = { telegram.sendText(chat.id, "/start") },
                        modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    )
                }
            }
        }
        val panelSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
        val panelFade = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
        val panelSize = MaterialTheme.motionScheme.defaultSpatialSpec<IntSize>()
        AnimatedVisibility(
            visible = attachments.isNotEmpty(),
            enter = expandVertically(panelSize, expandFrom = Alignment.Top) + fadeIn(panelFade),
            exit = shrinkVertically(panelSize, shrinkTowards = Alignment.Top) + fadeOut(panelFade),
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
        if (selecting) {
            SelectionBar(
                count = selected.size,
                onForward = { forwardingSelected = true },
                onDelete = { deletingSelected = true },
                onShot = { shot = true },
                onCancel = { selected.clear() },
            )
        } else Composer(
            chatId = chat.id,
            inGroup = chat.isGroup,
            telegram = telegram,
            settings = settings,
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
            onSend = { text, entities ->
                val edited = editing
                val replyId = replyTo?.id ?: 0L
                replyTo = null
                if (edited != null && edited.photo != null) {
                    val file = attachments.firstOrNull()
                    when {
                        file == null -> Unit
                        file == editPhoto -> telegram.editMessage(edited, text, entities)
                        else -> telegram.editMessageMedia(edited, file, text, !asFiles && Attachments.isPhoto(file), entities)
                    }
                    editing = null
                } else if (edited != null) {
                    telegram.editMessage(edited, text, entities)
                    editing = null
                } else if (attachments.isNotEmpty()) {
                    val photos = !asFiles && attachments.all(Attachments::isPhoto)
                    telegram.sendFiles(chat.id, attachments.toList(), photos, text, replyId, entities)
                    attachments.clear()
                    asFiles = false
                } else {
                    telegram.sendText(chat.id, text, replyId, entities)
                }
                if (listState.firstVisibleItemIndex > 0) {
                    scope.launch { listState.animateScrollToItem(0) }
                }
            },
        )
    }
    AnimatedVisibility(
        visible = commentsFor != null,
        enter = slideInHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec()) { it / 3 } + fadeIn(),
        exit = slideOutHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec()) { it / 3 } + fadeOut(),
    ) {
        var shownPost by remember { mutableStateOf(commentsFor) }
        commentsFor?.let { shownPost = it }
        shownPost?.let { post -> CommentsView(post, telegram, settings) { commentsFor = null } }
    }
    }
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
    onReact: ((String) -> Unit)? = null,
    onButton: ((TdApi.InlineKeyboardButton) -> Unit)? = null,
    onOrigin: ((TdApi.MessageOrigin) -> Unit)? = null,
    onVia: ((String) -> Unit)? = null,
    onComments: (() -> Unit)? = null,
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
        BareMessage(message, telegram, settings, status, seconds, quote, modifier, onReact)
        return
    }
    val colors = MaterialTheme.colorScheme
    val outgoing = message.isOutgoing
    val grouped = message.album.takeIf { it.size > 1 && telegram != null }
    val album = grouped?.takeIf { members -> members.all { it.content is TdApi.MessagePhoto || isMediaContent(it.content) } }
    val files = grouped?.takeIf { album == null }
    val photo = if (grouped != null) null else message.photo
    val media = grouped == null && telegram != null && isMediaContent(content)
    val block = grouped == null && telegram != null && isRich(content) && !media
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
            val inset = photo != null || media || album != null
            Column(Modifier.padding(if (inset) PhotoInset else 0.dp).width(IntrinsicSize.Max)) {
                val via = if (message.viaBot != 0L && telegram != null) telegram.botName(message.viaBot) else ""
                val named = !senderName.isNullOrEmpty() || via.isNotBlank()
                if (named) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(
                            start = if (inset) 8.dp else 12.dp,
                            end = 12.dp,
                            top = if (inset) 4.dp else 7.dp,
                            bottom = if (inset || quote != null) 4.dp else 0.dp,
                        ),
                    ) {
                        if (!senderName.isNullOrEmpty()) {
                            EmojiText(
                                senderName,
                                style = MaterialTheme.typography.labelLarge,
                                color = senderColor(message.senderUserId.takeIf { it != 0L } ?: message.senderChatId),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                        }
                        if (via.isNotBlank()) {
                            Text(
                                "via $via",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.primary,
                                maxLines = 1,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .clickable { if (via.startsWith("@")) onVia?.invoke(via) },
                            )
                        }
                    }
                }
                if (quote != null) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .widthIn(min = QuoteMinWidth)
                            .padding(start = 8.dp, end = 8.dp, top = if (named) 0.dp else 8.dp, bottom = if (photo != null || media || album != null) 6.dp else 0.dp),
                    ) { quote() }
                }
                val mediaBlock = @Composable {
                    if (album != null && telegram != null) {
                        AlbumGrid(
                            album,
                            telegram,
                            PhotoMaxWidth,
                            (settings.bubbleRadius.value.dp - PhotoInset).coerceAtLeast(4.dp),
                            overlay = if (hasText) {
                                null
                            } else {
                                { MessageMeta(message, status, settings.messageSeconds.value, Color.White, Color.White) }
                            },
                        )
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
                }
                if (!(message.captionAbove && hasText)) mediaBlock()
                if (hasText || (photo == null && album == null)) {
                    Column(
                        Modifier
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                            .then(if (photo != null || media || album != null) Modifier.width(PhotoMaxWidth - 24.dp) else Modifier),
                    ) {
                        val origin = message.forwardOrigin
                        if (origin != null && telegram != null) {
                            val reachable = onOrigin != null && origin !is TdApi.MessageOriginHiddenUser
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = if (reachable) {
                                    Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .pointerHoverIcon(PointerIcon.Hand)
                                        .clickable { onOrigin?.invoke(origin) }
                                } else {
                                    Modifier
                                },
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Outlined.Forward,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(14.dp),
                                )
                                EmojiText(
                                    "Forwarded from " + telegram.forwardName(origin).ifBlank { "unknown" },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        if (block && telegram != null) {
                            BlockContent(message, telegram, onPlugin = { PluginViewer.path = it })
                            Spacer(Modifier.height(4.dp))
                        }
                        if (files != null && telegram != null) {
                            files.forEach { member ->
                                BlockContent(member, telegram, onPlugin = { PluginViewer.path = it })
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                        val linkPreview = linkPreviewOf(message.content)
                        val reacted = message.reactions.isNotEmpty() && onReact != null
                        val meta = @Composable { MessageMeta(message, status, settings.messageSeconds.value, metaColor, readColor) }
                        if (linkPreview != null && telegram != null) {
                            TextWithMeta(text = message.text, entities = message.entities, meta = { Box(Modifier) })
                            LinkPreviewCard(linkPreview, telegram, outgoing, PhotoMaxWidth - 24.dp)
                            if (!reacted) Box(Modifier.align(Alignment.End).padding(top = 4.dp)) { meta() }
                        } else if (reacted) {
                            TextWithMeta(text = message.text, entities = message.entities, meta = { Box(Modifier) })
                        } else {
                            TextWithMeta(text = message.text, entities = message.entities, meta = meta)
                        }
                        if (reacted && onReact != null) {
                            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
                                Box(Modifier.weight(1f)) {
                                    ReactionChips(
                                        message.reactions,
                                        outgoing,
                                        onReact,
                                        telegram,
                                        settings,
                                        avatars = telegram?.chats?.get(message.chatId)?.isChannel == false,
                                    )
                                }
                                Box(Modifier.padding(start = 8.dp, bottom = 2.dp)) { meta() }
                            }
                        }
                    }
                } else if (message.reactions.isNotEmpty() && onReact != null) {
                    Box(Modifier.padding(horizontal = 6.dp).padding(bottom = 4.dp)) {
                        ReactionChips(
                            message.reactions,
                            outgoing,
                            onReact,
                            telegram,
                            settings,
                            avatars = telegram?.chats?.get(message.chatId)?.isChannel == false,
                        )
                    }
                }
                if (message.captionAbove && hasText) mediaBlock()
                if (message.keyboard.isNotEmpty() && onButton != null) {
                    InlineKeyboard(
                        message.keyboard,
                        onButton,
                        Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp).widthIn(min = KeyboardMinWidth),
                    )
                }
                if (message.comments >= 0 && onComments != null) CommentsBar(message.comments, onComments)
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
    settings: AppSettings,
    status: MessageStatus?,
    seconds: Boolean,
    quote: (@Composable () -> Unit)?,
    modifier: Modifier,
    onReact: ((String) -> Unit)?,
) {
    Box(modifier.fillMaxWidth(), contentAlignment = if (message.isOutgoing) Alignment.CenterEnd else Alignment.CenterStart) {
        Column(
            horizontalAlignment = if (message.isOutgoing) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (quote != null) Box(Modifier.widthIn(min = QuoteMinWidth, max = 280.dp)) { quote() }
            BareContent(message, telegram)
            if (message.reactions.isNotEmpty() && onReact != null) {
                ReactionChips(
                    message.reactions,
                    message.isOutgoing,
                    onReact,
                    telegram,
                    settings,
                    avatars = telegram.chats[message.chatId]?.isChannel == false,
                )
            }
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
        if (message.signature.isNotBlank()) {
            Text(message.signature, style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1)
        }
        if (message.views > 0) {
            Icon(Icons.Outlined.Visibility, contentDescription = "Views", tint = color, modifier = Modifier.size(14.dp))
            Text(formatViews(message.views), style = MaterialTheme.typography.labelSmall, color = color)
        }
        if (message.pinned) {
            Icon(Icons.Outlined.PushPin, contentDescription = "Pinned", tint = color, modifier = Modifier.size(13.dp))
        }
        if (message.edited) {
            Icon(Icons.Outlined.Edit, contentDescription = "Edited", tint = color, modifier = Modifier.size(13.dp))
        }
        Text(formatMessageTime(message.date, seconds), style = MaterialTheme.typography.labelSmall, color = color)
        if (status != null) {
            MessageStatusIcon(status, color = if (status == MessageStatus.Read) readColor else color, size = 16.dp)
        }
    }
}

private fun formatViews(count: Int): String = when {
    count >= 1_000_000 -> "%.1fM".format(java.util.Locale.US, count / 1_000_000f).replace(".0M", "M")
    count >= 1_000 -> "%.1fK".format(java.util.Locale.US, count / 1_000f).replace(".0K", "K")
    else -> count.toString()
}

@Composable
private fun TextWithMeta(text: String, entities: List<TdApi.TextEntity>, meta: @Composable () -> Unit) {
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val quoteAtEnd = remember(text, entities) { endsWithQuote(text, entities) }
    Layout(
        modifier = Modifier.fillMaxWidth(),
        content = {
            if (text.isNotEmpty()) {
                SelectionContainer {
                    FormattedText(text, entities, style = MaterialTheme.typography.bodyLarge, onTextLayout = { textLayout = it })
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
                val sameLine = text.isEmpty() || (!quoteAtEnd && lastLineRight + gap + metaPlaceable.width <= constraints.maxWidth)
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
private const val NewerThreshold = 5
private const val PanelScaleFrom = 0.9f
private val SendSlot = 40.dp
object VoiceButton {
    var preferred by mutableStateOf(true)
}
private const val SelectedAlpha = 0.14f
private const val SelectEdgePx = 48f
private const val MentionDelayMs = 150L
private val KeyboardMinWidth = 260.dp
private const val JumpPages = 40
private const val JumpPageDelayMs = 300L

object ComposerInsert {
    var request by mutableStateOf<Pair<Long, String>?>(null)
}

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
        } else if (!message.sending) {
            TransferRing(telegram.progress(file), MaterialTheme.colorScheme.primary, null, 48.dp)
        }
        if (message.sending) {
            Box(Modifier.size(56.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
                TransferRing(
                    telegram.progress(message.fullPhoto) ?: telegram.progress(file),
                    Color.White,
                    { telegram.cancelSending(message) },
                    48.dp,
                )
            }
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
internal val SenderAvatarSlot = 42.dp
private val SenderAvatarSize = 34.dp

internal fun sameSender(other: UiMessage?, message: UiMessage): Boolean =
    other != null && !other.service && other.isOutgoing == message.isOutgoing &&
        other.senderUserId == message.senderUserId && other.senderChatId == message.senderChatId &&
        sameDay(other.date, message.date)

@Composable
fun senderColor(id: Long): Color {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return remember(id, dark) {
        val hue = ((id % 360 + 360) % 360 * 37 % 360).toDouble()
        Color(Hct.from(hue, 50.0, if (dark) 78.0 else 42.0).toInt())
    }
}

@Composable
internal fun SenderAvatar(message: UiMessage, telegram: Telegram, settings: AppSettings, onClick: () -> Unit) {
    val photo = if (message.senderUserId != 0L) {
        telegram.users[message.senderUserId]?.profilePhoto?.small
    } else {
        telegram.chats[message.senderChatId]?.photo
    }
    Avatar(
        telegram.senderName(message),
        photo,
        telegram,
        settings,
        SenderAvatarSize,
        Modifier.pointerHoverIcon(PointerIcon.Hand).clickable(onClick = onClick),
        kind = AvatarKind.Private,
    )
}
private const val DraftSaveDelayMs = 1_500L
private const val InlineDelayMs = 400L
private const val GifTab = 2
private const val CommandLimit = 30
private const val JumpCloserItems = 20
private const val JumpLandingItem = 6
private const val WelcomeDelayMs = 700L

@Composable
private fun Composer(
    chatId: Long,
    inGroup: Boolean,
    telegram: Telegram,
    settings: AppSettings,
    hasAttachments: Boolean,
    editing: UiMessage?,
    replyTo: UiMessage?,
    onCancelReply: () -> Unit,
    onEditLast: () -> Boolean,
    onCancelEdit: () -> Unit,
    onAttach: (List<File>) -> Unit,
    onSend: (String, Array<TdApi.TextEntity>) -> Unit,
) {
    var text by remember(chatId) {
        val draft = telegram.chats[chatId]?.draft.orEmpty()
        mutableStateOf(TextFieldValue(draft, TextRange(draft.length)))
    }
    var spans by remember { mutableStateOf(emptyList<FormatSpan>()) }
    LaunchedEffect(chatId, text.text) {
        delay(DraftSaveDelayMs)
        if (editing == null) telegram.saveDraft(chatId, text.text)
    }
    val draftHolder = remember(chatId) { arrayOf("") }
    if (editing == null) draftHolder[0] = text.text
    DisposableEffect(chatId) {
        val holder = draftHolder
        onDispose { telegram.saveDraft(chatId, holder[0]) }
    }
    var menuAt by remember { mutableStateOf<DpOffset?>(null) }
    var linkFor by remember { mutableStateOf<TextRange?>(null) }
    var dateFor by remember { mutableStateOf<TextRange?>(null) }
    val setText = { value: TextFieldValue ->
        spans = shiftSpans(spans, text.text, value.text)
        text = value
    }
    val mentionQuery = if (text.selection.collapsed) {
        MentionQuery.find(text.text.substring(0, text.selection.start.coerceAtMost(text.text.length)))?.groupValues?.get(1)
    } else {
        null
    }
    var suggestions by remember { mutableStateOf(emptyList<Long>()) }
    var suggestionIndex by remember { mutableStateOf(0) }
    LaunchedEffect(mentionQuery, chatId) {
        if (mentionQuery == null) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        delay(MentionDelayMs)
        telegram.mentionSuggestions(chatId, inGroup, mentionQuery) {
            suggestions = it
            suggestionIndex = 0
        }
    }
    val inlineMatch = InlineQuery.matchEntire(text.text)
    val inlineName = inlineMatch?.groupValues?.get(1)
    val inlineText = inlineMatch?.groupValues?.get(2)
    var inlineBot by remember(chatId) { mutableStateOf<TdApi.User?>(null) }
    var inlineResults by remember(chatId) { mutableStateOf<TdApi.InlineQueryResults?>(null) }
    var inlineResolving by remember(chatId) { mutableStateOf(false) }
    LaunchedEffect(inlineName, chatId) {
        if (inlineBot?.usernames?.activeUsernames?.any { it.equals(inlineName, true) } == true) return@LaunchedEffect
        inlineBot = null
        inlineResolving = false
        val name = inlineName ?: return@LaunchedEffect
        inlineResolving = true
        delay(MentionDelayMs)
        telegram.inlineBot(name) {
            inlineBot = it
            inlineResolving = false
        }
    }
    LaunchedEffect(inlineBot?.id, inlineText, chatId) {
        inlineResults = null
        val bot = inlineBot ?: return@LaunchedEffect
        val query = inlineText ?: return@LaunchedEffect
        delay(InlineDelayMs)
        telegram.inlineResults(bot.id, chatId, query) { inlineResults = it }
    }
    val showInline = inlineMatch != null && inlineBot != null
    val inlineLoading = inlineMatch != null && (inlineResolving || inlineBot != null && inlineResults == null)
    val pickInline = { queryId: Long, resultId: String ->
        telegram.sendInlineResult(chatId, queryId, resultId, replyTo?.id ?: 0)
        text = TextFieldValue("")
        spans = emptyList()
        onCancelReply()
    }
    val composerChat = telegram.chats[chatId]
    var commands by remember(chatId) { mutableStateOf(emptyList<BotCommandItem>()) }
    LaunchedEffect(chatId) {
        val target = telegram.chats[chatId] ?: return@LaunchedEffect
        telegram.chatCommands(target) { commands = it }
        telegram.loadKeyboard(target)
    }
    val commandQuery = if (text.selection.collapsed) CommandQuery.matchEntire(text.text)?.groupValues?.get(1) else null
    val commandMatches = commandQuery?.let { query -> commands.filter { it.command.startsWith(query, ignoreCase = true) }.take(CommandLimit) }.orEmpty()
    val showCommands = commandMatches.isNotEmpty()
    val pickCommand = { item: BotCommandItem ->
        val manyBots = commands.map { it.botId }.distinct().size > 1
        val suffix = if (manyBots) telegram.users[item.botId]?.usernames?.activeUsernames?.firstOrNull()?.let { "@$it" }.orEmpty() else ""
        telegram.sendText(chatId, "/${item.command}$suffix", replyTo?.id ?: 0)
        text = TextFieldValue("")
        spans = emptyList()
        onCancelReply()
    }
    val keyboard = composerChat?.keyboard
    var keyboardOpen by remember(chatId, keyboard) { mutableStateOf(true) }
    val showSuggestions = mentionQuery != null && suggestions.isNotEmpty()
    val pickMention = { userId: Long ->
        val query = mentionQuery.orEmpty()
        val user = telegram.users[userId]
        val username = user?.usernames?.activeUsernames?.firstOrNull()
        val start = text.selection.start - query.length - 1
        val insert = if (username != null) "@$username " else (user?.firstName.orEmpty() + " ")
        val updated = text.text.replaceRange(start, text.selection.start, insert)
        spans = shiftSpans(spans, text.text, updated)
        if (username == null) {
            spans = toggleFormat(spans, TextRange(start, start + insert.length - 1), FormatType.Mention).map {
                if (it.type == FormatType.Mention && it.start == start) it.copy(userId = userId) else it
            }
        }
        text = TextFieldValue(updated, TextRange(start + insert.length))
        suggestions = emptyList()
    }
    val applyFormat = { type: FormatType ->
        when {
            text.selection.collapsed -> Unit
            type == FormatType.Link -> linkFor = text.selection
            type == FormatType.Date -> dateFor = text.selection
            else -> spans = toggleFormat(spans, text.selection, type)
        }
    }
    var emojiOpen by remember { mutableStateOf(false) }
    var mediaTab by remember { mutableStateOf(0) }
    val focus = remember { FocusRequester() }
    val pickScope = rememberCoroutineScope()
    LaunchedEffect(chatId) { runCatching { focus.requestFocus() } }
    LaunchedEffect(editing?.key) {
        if (editing != null) {
            text = TextFieldValue(editing.text, TextRange(editing.text.length))
            spans = spansFrom(editing.entities)
            focus.requestFocus()
        }
    }
    LaunchedEffect(ComposerInsert.request) {
        val (target, value) = ComposerInsert.request ?: return@LaunchedEffect
        if (target != chatId) return@LaunchedEffect
        ComposerInsert.request = null
        setText(TextFieldValue(value, TextRange(value.length)))
        runCatching { focus.requestFocus() }
    }
    val canSend = if (editing?.photo != null) hasAttachments else text.text.isNotBlank() || hasAttachments
    val send = {
        if (canSend) {
            val raw = text.text
            val lead = raw.length - raw.trimStart().length
            val trimmed = raw.trim()
            onSend(trimmed, toEntities(spans, lead, trimmed.length))
            text = TextFieldValue("")
            spans = emptyList()
        }
    }
    LaunchedEffect(replyTo?.key) {
        if (replyTo != null) focus.requestFocus()
    }
    val cancelEdit = {
        onCancelEdit()
        text = TextFieldValue("")
        spans = emptyList()
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
            ReplyThumb(banner?.second, telegram, 40.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    if (isEdit) "Editing" else "Reply",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                EmojiText(
                    banner?.second?.text?.takeIf { it.isNotEmpty() } ?: banner?.second?.let { if (it.photo != null) "Photo" else contentText(it.content) }.orEmpty(),
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
        setText(TextFieldValue(updated, TextRange(start + emoji.length)))
        focus.requestFocus()
    }
    AnimatedVisibility(
        visible = showSuggestions,
        enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeIn(),
        exit = shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeOut(),
    ) {
        MentionSuggestions(
            suggestions,
            suggestionIndex,
            telegram,
            settings,
            onPick = pickMention,
            modifier = Modifier.fillMaxWidth().padding(start = 64.dp, end = 120.dp, top = 8.dp),
        )
    }
    AnimatedVisibility(
        visible = showCommands,
        enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeIn(),
        exit = shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeOut(),
    ) {
        CommandSuggestions(
            commandMatches,
            -1,
            telegram,
            settings,
            onPick = pickCommand,
            modifier = Modifier.fillMaxWidth().padding(start = 64.dp, end = 120.dp, top = 8.dp),
        )
    }
    AnimatedVisibility(
        visible = showInline,
        enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeIn(),
        exit = shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeOut(),
    ) {
        InlineResultsPanel(
            inlineResults,
            telegram,
            onPick = pickInline,
            modifier = Modifier.fillMaxWidth().padding(start = 64.dp, end = 120.dp, top = 8.dp),
        )
    }
    linkFor?.let { range ->
        LinkDialog(onDismiss = { linkFor = null }) { url ->
            spans = toggleFormat(spans, range, FormatType.Link, url = url)
            linkFor = null
        }
    }
    dateFor?.let { range ->
        DateDialog(onDismiss = { dateFor = null }) { time ->
            spans = toggleFormat(spans, range, FormatType.Date, time = time)
            dateFor = null
        }
    }
    val voice = remember(chatId) { VoiceRecording() }
    val voiceScope = rememberCoroutineScope()
    val releaseVoice: (Boolean) -> Unit = { cancel ->
        if (cancel) {
            voice.cancel()
        } else {
            voiceScope.launch {
                voice.finish()?.let { note ->
                    telegram.sendVoice(chatId, note, replyTo?.id ?: 0)
                    onCancelReply()
                }
            }
        }
    }
    Box(Modifier.fillMaxWidth()) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (commands.isNotEmpty()) {
            val openCommands = {
                setText(TextFieldValue("/", TextRange(1)))
                runCatching { focus.requestFocus() }
            }
            if (composerChat?.privateUserId != 0L) {
                FilledTonalButton(
                    onClick = { openCommands() },
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    modifier = Modifier.height(36.dp),
                ) { Text("Menu") }
            } else {
                IconButton(onClick = { openCommands() }) {
                    Text("/", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        IconButton(onClick = { pickScope.launch { onAttach(Attachments.pick()) } }) {
            Icon(Icons.Outlined.AttachFile, contentDescription = "Attach files")
        }
        val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
        val selected = { text.text.substring(text.selection.min, text.selection.max) }
        val replaceSelection = { value: String ->
            val start = text.selection.min
            setText(TextFieldValue(text.text.replaceRange(start, text.selection.max, value), TextRange(start + value.length)))
        }
        val density = LocalDensity.current
        Box(
            Modifier
                .weight(1f)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                                val change = event.changes.first()
                                change.consume()
                                menuAt = with(density) { DpOffset(change.position.x.toDp(), change.position.y.toDp()) }
                            }
                        }
                    }
                },
        ) {
            menuAt?.let { at ->
                ComposerMenu(
                    at = at,
                    hasSelection = !text.selection.collapsed,
                    onDismiss = { menuAt = null },
                    onCut = {
                        clipboard.setContents(java.awt.datatransfer.StringSelection(selected()), null)
                        replaceSelection("")
                    },
                    onCopy = { clipboard.setContents(java.awt.datatransfer.StringSelection(selected()), null) },
                    onPaste = {
                        runCatching { clipboard.getData(java.awt.datatransfer.DataFlavor.stringFlavor) as String }.getOrNull()
                            ?.let(replaceSelection)
                    },
                    onDelete = { replaceSelection("") },
                    onSelectAll = { text = text.copy(selection = TextRange(0, text.text.length)) },
                    onFormat = applyFormat,
                    onClear = { spans = clearFormat(spans, text.selection) },
                )
            }
            TextField(
                value = text,
                onValueChange = setText,
                placeholder = { Text(if (hasAttachments) "Add a caption" else "Message") },
                visualTransformation = FormatTransformation(spans, MaterialTheme.colorScheme.primary),
                maxLines = 6,
                shape = RoundedCornerShape(28.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus)
                    .onPreviewKeyEvent { event ->
                        val shortcut = if (event.type == KeyEventType.KeyDown && (event.isCtrlPressed || event.isMetaPressed)) {
                            when {
                                event.isShiftPressed && event.key == Key.X -> FormatType.Strikethrough
                                event.isShiftPressed && event.key == Key.Period -> FormatType.Quote
                                event.isShiftPressed && event.key == Key.M -> FormatType.Monospace
                                event.isShiftPressed && event.key == Key.P -> FormatType.Spoiler
                                event.isShiftPressed && event.key == Key.D -> FormatType.Date
                                !event.isShiftPressed && event.key == Key.B -> FormatType.Bold
                                !event.isShiftPressed && event.key == Key.I -> FormatType.Italic
                                !event.isShiftPressed && event.key == Key.U -> FormatType.Underline
                                !event.isShiftPressed && event.key == Key.K -> FormatType.Link
                                else -> null
                            }
                        } else {
                            null
                        }
                        when {
                            showSuggestions && event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown -> {
                                suggestionIndex = (suggestionIndex + 1) % suggestions.size
                                true
                            }
                            showSuggestions && event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp -> {
                                suggestionIndex = (suggestionIndex - 1 + suggestions.size) % suggestions.size
                                true
                            }
                            showSuggestions && event.type == KeyEventType.KeyDown && (event.key == Key.Enter || event.key == Key.Tab) -> {
                                pickMention(suggestions[suggestionIndex.coerceIn(0, suggestions.lastIndex)])
                                true
                            }
                            shortcut != null -> {
                                applyFormat(shortcut)
                                true
                            }
                            event.type == KeyEventType.KeyDown && (event.isCtrlPressed || event.isMetaPressed) &&
                                event.isShiftPressed && event.key == Key.N -> {
                                spans = clearFormat(spans, text.selection)
                                true
                            }
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
        }
        if (keyboard != null) {
            IconToggleButton(checked = keyboardOpen, onCheckedChange = { keyboardOpen = it }) {
                Icon(if (keyboardOpen) Icons.Outlined.KeyboardHide else Icons.Outlined.Keyboard, contentDescription = "Bot keyboard")
            }
        }
        Box {
            IconToggleButton(checked = emojiOpen, onCheckedChange = { emojiOpen = it }) {
                Icon(Icons.Outlined.EmojiEmotions, contentDescription = "Emoji")
            }
            val panelVisible = remember { MutableTransitionState(false) }
            panelVisible.targetState = emojiOpen
            if (panelVisible.currentState || panelVisible.targetState) {
                Popup(
                    alignment = Alignment.BottomEnd,
                    offset = IntOffset(0, -with(LocalDensity.current) { 56.dp.roundToPx() }),
                    onDismissRequest = { emojiOpen = false },
                    properties = PopupProperties(focusable = mediaTab == GifTab),
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visibleState = panelVisible,
                        enter = fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()) +
                            scaleIn(MaterialTheme.motionScheme.defaultSpatialSpec(), initialScale = PanelScaleFrom, transformOrigin = TransformOrigin(1f, 1f)),
                        exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()) +
                            scaleOut(MaterialTheme.motionScheme.fastSpatialSpec(), targetScale = PanelScaleFrom, transformOrigin = TransformOrigin(1f, 1f)),
                    ) {
                    MediaPanel(
                        telegram = telegram,
                        chatId = chatId,
                        tab = mediaTab,
                        onTab = { mediaTab = it },
                        onEmoji = insert,
                        whenOnline = telegram.chats[chatId]?.let { it.privateUserId != 0L && !telegram.isSavedMessages(chatId) && !telegram.isBot(it) } == true,
                        onSticker = { sticker, how ->
                            telegram.sendSticker(chatId, sticker, replyTo?.id ?: 0, how.silent, how.schedule)
                            onCancelReply()
                            emojiOpen = false
                        },
                        onGif = { animation ->
                            telegram.sendAnimation(chatId, animation, replyTo?.id ?: 0)
                            onCancelReply()
                            emojiOpen = false
                        },
                        onInline = { queryId, resultId ->
                            telegram.sendInlineResult(chatId, queryId, resultId, replyTo?.id ?: 0)
                            onCancelReply()
                            emojiOpen = false
                        },
                    )
                    }
                }
            }
        }
        Crossfade(inlineLoading) { loading ->
            if (loading) {
                Box(Modifier.size(SendSlot), contentAlignment = Alignment.Center) {
                    LoadingIndicator(Modifier.size(SendSlot))
                }
            } else {
                val showMic = voice.active || (VoiceButton.preferred && !canSend && editing == null)
                val swapFade = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
                val swapSlide = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
                Box(
                    Modifier.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed && !voice.active) {
                                    event.changes.forEach { it.consume() }
                                    VoiceButton.preferred = !VoiceButton.preferred
                                }
                            }
                        }
                    },
                ) {
                    AnimatedContent(
                        targetState = showMic,
                        transitionSpec = {
                            val direction = if (targetState) 1 else -1
                            ((slideInHorizontally(swapSlide) { it * direction } + fadeIn(swapFade)) togetherWith
                                (slideOutHorizontally(swapSlide) { -it * direction } + fadeOut(swapFade))) using SizeTransform(clip = false)
                        },
                    ) { mic ->
                        if (mic) {
                            MicButton(voice, onRelease = releaseVoice)
                        } else {
                            FilledIconButton(onClick = send, enabled = canSend, shape = MaterialShapes.Cookie12Sided.toShape()) {
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
                }
            }
        }
    }
    AnimatedVisibility(
        visible = voice.active,
        enter = fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()) +
            expandHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec(), expandFrom = Alignment.End),
        exit = if (voice.cancelled) {
            slideOutHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec()) { -it / 3 } + fadeOut(MaterialTheme.motionScheme.fastEffectsSpec())
        } else {
            fadeOut(MaterialTheme.motionScheme.fastEffectsSpec())
        },
        modifier = Modifier.matchParentSize().padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 12.dp + SendSlot + 8.dp),
    ) {
        RecordingBar(voice, Modifier.fillMaxSize())
    }
    }
    AnimatedVisibility(
        visible = keyboard != null && keyboardOpen,
        enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeIn(),
        exit = shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeOut(),
    ) {
        keyboard?.let { shown ->
            ReplyKeyboardPanel(shown, onPress = { button ->
                telegram.sendText(chatId, button.text, replyTo?.id ?: 0)
                onCancelReply()
                if (shown.oneTime) keyboardOpen = false
            }, modifier = Modifier.padding(bottom = 8.dp))
        }
    }
}
