package com.pixelgram.desktop.ui.main

import com.pixelgram.desktop.ui.components.ConfirmOnEnter
import com.pixelgram.desktop.ui.icons.outlined.AlternateEmail
import com.pixelgram.desktop.ui.icons.outlined.FavoriteBorder

import com.pixelgram.desktop.ui.theme.outgoingAccent

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.rememberUpdatedState
import com.pixelgram.desktop.ui.icons.automirrored.filled.ArrowBack
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.expandHorizontally
import com.pixelgram.desktop.ui.icons.outlined.KeyboardHide
import com.pixelgram.desktop.ui.icons.outlined.Keyboard
import com.pixelgram.desktop.ui.components.drawQuote
import com.pixelgram.desktop.ui.components.expressive.FilledTonalButton
import com.pixelgram.desktop.telegram.BotCommandItem
import com.pixelgram.desktop.ui.components.Avatar
import com.pixelgram.desktop.core.AvatarKind
import com.pixelgram.desktop.ui.toShape
import androidx.compose.ui.graphics.luminance
import com.materialkolor.hct.Hct
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import com.pixelgram.desktop.ui.icons.outlined.PushPin
import com.pixelgram.desktop.ui.icons.outlined.Visibility
import com.pixelgram.desktop.telegram.contentText
import com.pixelgram.desktop.core.DoubleClickAction
import androidx.compose.ui.unit.IntSize
import com.pixelgram.desktop.ui.icons.filled.MoreVert
import com.pixelgram.desktop.ui.icons.automirrored.outlined.Forward
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.unit.DpOffset
import com.pixelgram.desktop.ui.components.FormattedText
import com.pixelgram.desktop.ui.components.endsWithQuote
import com.pixelgram.desktop.ui.icons.filled.Search
import com.pixelgram.desktop.ui.icons.filled.Videocam
import com.pixelgram.desktop.ui.icons.filled.Call
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
import com.pixelgram.desktop.ui.icons.automirrored.outlined.Reply
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.pixelgram.desktop.ui.icons.filled.Check
import com.pixelgram.desktop.ui.icons.filled.Close
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.expandVertically
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.layout.Layout
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import com.pixelgram.desktop.ui.icons.outlined.Edit
import com.pixelgram.desktop.telegram.MessageStatus
import com.pixelgram.desktop.ui.icons.outlined.ErrorOutline
import com.pixelgram.desktop.ui.icons.outlined.Schedule
import com.pixelgram.desktop.ui.icons.outlined.NotificationsOff
import com.pixelgram.desktop.ui.icons.outlined.Alarm
import com.pixelgram.desktop.ui.icons.outlined.CalendarMonth
import com.pixelgram.desktop.ui.icons.filled.DoneAll
import com.pixelgram.desktop.ui.icons.filled.Done
import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.automirrored.filled.Send
import com.pixelgram.desktop.ui.icons.outlined.AttachFile
import com.pixelgram.desktop.ui.icons.outlined.EmojiEmotions
import com.pixelgram.desktop.ui.components.expressive.FilledIconButton
import androidx.compose.material3.Icon
import com.pixelgram.desktop.ui.components.expressive.IconButton
import com.pixelgram.desktop.ui.components.expressive.IconToggleButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.pixelgram.desktop.ui.components.Text
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
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
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
import com.pixelgram.desktop.telegram.fullName
import com.pixelgram.desktop.telegram.UiMessage
import com.pixelgram.desktop.telegram.SenderTag
import com.pixelgram.desktop.telegram.SecretStatus
import com.pixelgram.desktop.telegram.openKeyboardWebApp
import com.pixelgram.desktop.telegram.secretStatus
import com.pixelgram.desktop.ui.components.ChatAvatar
import com.pixelgram.desktop.ui.components.loadImage
import com.pixelgram.desktop.ui.theme.EmojiText
import it.tdlight.jni.TdApi
import androidx.compose.material3.LocalContentColor
import java.io.File
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.abs
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
import com.pixelgram.desktop.ui.components.expressive.TextButton
import androidx.compose.material3.Checkbox
import com.pixelgram.desktop.ui.components.expressive.AlertDialog
import com.pixelgram.desktop.core.PowerSaving
import com.pixelgram.desktop.ui.components.OfficialBadge
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.positionInWindow
import com.pixelgram.desktop.ui.icons.filled.CheckCircle
import com.pixelgram.desktop.ui.icons.outlined.Circle

@Composable
fun ChatView(
    chat: ChatState,
    telegram: Telegram,
    settings: AppSettings,
    onOpenChat: (Long) -> Unit,
    onBack: (() -> Unit)? = null,
    forumSidebar: Boolean = true,
) {
    ChatThemeScope(chat, telegram, settings) { ChatViewContent(chat, telegram, settings, onOpenChat, onBack, forumSidebar) }
}

@Composable
private fun ChatViewContent(
    chat: ChatState,
    telegram: Telegram,
    settings: AppSettings,
    onOpenChat: (Long) -> Unit,
    onBack: (() -> Unit)?,
    forumSidebar: Boolean,
) {
    DisposableEffect(chat.id) {
        telegram.openChat(chat.id)
        onDispose { telegram.closeChat(chat.id) }
    }
    val guarded = chat.protectedContent
    DisposableEffect(chat.id, guarded) {
        if (guarded) com.pixelgram.desktop.core.ScreenProtection.acquire()
        onDispose { if (guarded) com.pixelgram.desktop.core.ScreenProtection.release() }
    }
    val messages = telegram.messages(chat.id)
    val shown by remember(chat.id) { derivedStateOf { groupAlbums(messages.toList()) } }
    androidx.compose.runtime.SideEffect { ActiveChat.id = chat.id }
    val groupStarts by remember(chat.id) { derivedStateOf { messageGroupStarts(shown) } }
    val listState = rememberLazyListState()
    var jumping by remember(chat.id) { mutableStateOf(false) }
    var calendarAt by remember(chat.id) { mutableStateOf<Int?>(null) }
    val nearTop by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - HistoryPrefetchItems
        }
    }
    LaunchedEffect(nearTop, messages.size, jumping) {
        if (nearTop && !jumping) telegram.loadMoreMessages(chat.id)
    }
    val nearBottom by remember { derivedStateOf { listState.firstVisibleItemIndex < NewerThreshold } }
    val detached = telegram.detached[chat.id] == true
    LaunchedEffect(nearBottom, detached, messages.size, jumping) {
        if (nearBottom && detached && !jumping) telegram.loadNewer(chat.id)
    }
    val newest = messages.firstOrNull()?.key
    val newestId = messages.firstOrNull()?.id ?: 0L
    val scope = rememberCoroutineScope()
    val seenNewest = remember(chat.id) { arrayOf(newest) }
    val seenNewestId = remember(chat.id) { longArrayOf(newestId) }
    if (newest != seenNewest[0]) {
        seenNewest[0] = newest
        val newer = newestId >= seenNewestId[0] && !jumping
        seenNewestId[0] = newestId
        val own = newer && messages.firstOrNull()?.let { it.isOutgoing && !it.service } == true
        if (own && listState.firstVisibleItemIndex > 0) {
            scope.launch {
                if (listState.firstVisibleItemIndex > JumpCloserItems) listState.scrollToItem(JumpLandingItem)
                listState.animateScrollToItem(0)
            }
        } else if (own || newer && listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < StickToBottomPx) {
            listState.requestScrollToItem(0)
        }
    }
    val attachments = remember(chat.id) { mutableStateListOf<File>() }
    var asFiles by remember(chat.id) { mutableStateOf(false) }
    var editPhoto by remember(chat.id) { mutableStateOf<File?>(null) }
    var editingPhoto by remember(chat.id) { mutableStateOf(false) }
    var spoiler by remember(chat.id) { mutableStateOf(false) }
    var sendWith by remember(chat.id) { mutableStateOf<AttachmentSend?>(null) }
    var sendRequested by remember(chat.id) { mutableStateOf(false) }
    val modal = LocalModalHost.current
    val editAttachment = { file: File ->
        modal?.editing = EditRequest(file, telegram, chat.id) { result ->
            val index = attachments.indexOf(file)
            if (index >= 0) attachments[index] = result
        }
    }
    val attach = { files: List<File> ->
        if (editingPhoto) {
            files.firstOrNull()?.let {
                attachments.clear()
                attachments.add(it)
            }
        } else {
            files.filter { it !in attachments }.let(attachments::addAll)
        }
        if (files.any { !Attachments.isMedia(it) }) asFiles = true
    }
    LaunchedEffect(chat.id) {
        com.pixelgram.desktop.telegram.demoAttachment()?.let { file ->
            attach(listOf(file))
            if (com.pixelgram.desktop.telegram.demoOpensEditor) editAttachment(file)
        }
    }
    val placement = androidx.compose.animation.core.spring<IntOffset>(dampingRatio = 1f, stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow)
    val fade = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val slide = androidx.compose.animation.core.spring<Float>(dampingRatio = 1f, stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow)
    val slidIn = remember(chat.id) { mutableSetOf<Long>() }
    var editing by remember(chat.id) { mutableStateOf<UiMessage?>(null) }
    var replyTo by remember(chat.id) { mutableStateOf<UiMessage?>(null) }
    var deleting by remember(chat.id) { mutableStateOf<UiMessage?>(null) }
    var forwarding by remember(chat.id) { mutableStateOf<UiMessage?>(null) }
    var profileChat by remember(chat.id) { mutableStateOf<ChatState?>(null) }
    var savedHub by remember(chat.id) { mutableStateOf(false) }
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
    val returnStack = remember(chat.id) { androidx.compose.runtime.mutableStateListOf<Long>() }
    var pinning by remember(chat.id) { mutableStateOf<UiMessage?>(null) }
    var commentsFor by remember(chat.id) { mutableStateOf<UiMessage?>(null) }
    val selected = remember(chat.id) { mutableStateListOf<Long>() }
    val selecting = selected.isNotEmpty()
    val selectingState = rememberUpdatedState(selecting)
    val selectScope = rememberCoroutineScope()
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
    LaunchedEffect(chat.id) {
        androidx.compose.runtime.snapshotFlow {
            val pins = telegram.pinnedMessages[chat.id].orEmpty()
            val visible = shown.getOrNull(listState.firstVisibleItemIndex)?.id
            if (pins.isEmpty() || visible == null) null else pins.indexOfFirst { it.id <= visible }.let { if (it < 0) pins.lastIndex else it }
        }.collect { index -> if (index != null) pinIndex = index }
    }
    var translating by remember(chat.id) { mutableStateOf<UiMessage?>(null) }
    var reporting by remember(chat.id) { mutableStateOf<LongArray?>(null) }
    translating?.let { message -> TranslateDialog(telegram, message, settings.language.value) { translating = null } }
    reporting?.let { ids -> ReportDialog(telegram, chat.id, ids) { reporting = null } }
    val canReact = !telegram.isSavedMessages(chat.id) || telegram.me?.isPremium == true
    val actions = remember(chat.id, canReact, chat.protectedContent) {
        MessageActions(
            onReply = { editing = null; replyTo = it },
            onEdit = { replyTo = null; editing = it },
            onDelete = { deleting = it },
            onCopyPhoto = { message -> telegram.photoPath(message)?.let(Attachments::copyImage) },
            filePath = { message ->
                val file = when (val content = message.content) {
                    is TdApi.MessageDocument -> content.document.document
                    is TdApi.MessageAudio -> content.audio.audio
                    is TdApi.MessageVideo -> content.video.video
                    is TdApi.MessageAnimation -> content.animation.animation
                    is TdApi.MessageVoiceNote -> content.voiceNote.voice
                    else -> message.fullPhoto ?: message.photo
                }
                val path = telegram.localPath(file)
                val name = (message.content as? TdApi.MessageDocument)?.document?.fileName
                if (path != null && name != null) com.pixelgram.desktop.core.Downloads.exported(path, name) ?: path else path
            },
            onForward = { forwarding = it },
            onSaveGif = { message -> telegram.saveGif(message) },
            canEdit = { message, done -> telegram.canEdit(message, done) },
            properties = { message, done -> telegram.messageProperties(message, done) },
            protectedContent = chat.protectedContent,
            onTranslate = { translating = it },
            onResend = { message -> telegram.resend(message) },
            onCancelSending = { message -> telegram.cancelSending(message) },
            onReport = { message -> reporting = message.album.takeIf { it.isNotEmpty() }?.map { it.id }?.toLongArray() ?: longArrayOf(message.id) },
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
                    DoubleClickAction.Edit -> if (message.isOutgoing && message.editable) {
                        telegram.canEdit(message) { allowed ->
                            if (allowed) {
                                replyTo = null
                                editing = message
                            }
                        }
                    }
                    DoubleClickAction.Nothing -> Unit
                }
            },
            onQuoteClick = { id ->
                listState.layoutInfo.visibleItemsInfo.firstOrNull()?.let { item -> shown.getOrNull(item.index)?.id }?.let { from -> if (from != id) returnStack.add(from) }
                MessageSearch.jump = chat.id to id
            },
        )
    }
    LaunchedEffect(chat.id) { MessageSearch.lastChat = chat.id }
    LaunchedEffect(MessageSearch.jump, chat.id) {
        val (jumpChat, jumpId) = MessageSearch.jump ?: return@LaunchedEffect
        if (jumpChat != chat.id) return@LaunchedEffect
        MessageSearch.jump = null
        fun indexOf() = shown.indexOfFirst { it.id == jumpId || it.album.any { member -> member.id == jumpId } }
        try {
            if (indexOf() < 0) {
                jumping = true
                suspendCancellableCoroutine { continuation ->
                    telegram.jumpToMessage(chat.id, jumpId) { if (continuation.isActive) continuation.resume(Unit) }
                }
                withFrameNanos { }
            }
            val index = indexOf()
            if (index < 0) return@LaunchedEffect
            revealMessage(listState, index)
            highlight = jumpId to System.nanoTime()
        } finally {
            jumping = false
        }
    }
    forwarding?.let { message ->
        ForwardDialog(telegram, settings, onDismiss = { forwarding = null }) { target ->
            PendingForward.start(target, message.chatId, listOf(message))
            forwarding = null
            onOpenChat(target)
        }
    }
    pinning?.let { message ->
        val saved = telegram.isSavedMessages(chat.id)
        val private = chat.privateUserId != 0L && !saved
        var also by remember(message.key) { mutableStateOf(true) }
        val pin = {
            if (private) telegram.togglePin(message, notify = true, onlyForSelf = !also) else telegram.togglePin(message, notify = also)
            pinning = null
        }
        AlertDialog(
            onDismissRequest = { pinning = null },
            icon = { Icon(Icons.Outlined.PushPin, contentDescription = null) },
            title = { Text("Pin message") },
            text = {
                ConfirmOnEnter(pin) { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                } }
            },
            confirmButton = { TextButton(onClick = pin) { Text("Pin") } },
            dismissButton = { TextButton(onClick = { pinning = null }) { Text("Cancel") } },
        )
    }
    if (forwardingSelected) {
        ForwardDialog(telegram, settings, onDismiss = { forwardingSelected = false }) { target ->
            PendingForward.start(target, chat.id, selectedMessages())
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
    ThemeImport.request?.let { request -> ThemeImportDialog(request, settings) { ThemeImport.request = null } }
    MarkdownViewer.path?.let { path -> MarkdownDialog(path) { MarkdownViewer.path = null } }
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

    val dropped = DroppedFiles.pending
    LaunchedEffect(chat.id, dropped) {
        if (dropped == null || dropped.chatId != chat.id) return@LaunchedEffect
        DroppedFiles.pending = null
        attach(dropped.files)
        asFiles = dropped.files.any { !Attachments.isMedia(it) }
    }
    FileDropArea(onDrop = { files, asDocuments ->
        attach(files)
        asFiles = asDocuments || files.any { !Attachments.isMedia(it) }
    }) {
    androidx.compose.runtime.CompositionLocalProvider(LocalProtectedContent provides chat.protectedContent) {
    Box(Modifier.fillMaxSize()) {
    Row(Modifier.fillMaxSize()) {
    if (forumSidebar && telegram.isForum(chat)) ForumSidebar(chat, telegram, Modifier.fillMaxHeight())
    Column(Modifier.weight(1f).fillMaxHeight()) {
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
                    .clickable { if (telegram.isSavedMessages(chat.id)) savedHub = true else profileChat = chat }
                    .padding(end = 8.dp),
            ) {
            ChatAvatar(chat, telegram, settings, size = 40.dp)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SecretLock(chat, telegram, 18.dp)
                    EmojiText(
                        telegram.chatTitle(chat),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    com.pixelgram.desktop.ui.components.ChatBadges(chat, telegram, 22.dp)
                }
                val topicName = if (telegram.isForum(chat)) telegram.selectedTopicName(chat.id) else null
                val status = telegram.activity(chat) ?: telegram.presence(chat)?.let { presence ->
                    if (topicName != null) presence.copy(label = topicName + " · " + presence.label) else presence
                }
                AnimatedContent(status, transitionSpec = { fadeIn() togetherWith fadeOut() }) { text ->
                    if (text != null) {
                        EmojiText(
                            text.label,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (text.online) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            }
            if (chat.privateUserId != 0L && com.pixelgram.desktop.calls.Calls.canCall(telegram, chat.privateUserId)) {
                IconButton(onClick = { com.pixelgram.desktop.calls.Calls.start(telegram, chat.privateUserId, video = false) }) {
                    Icon(Icons.Filled.Call, contentDescription = "Call")
                }
                IconButton(onClick = { com.pixelgram.desktop.calls.Calls.start(telegram, chat.privateUserId, video = true) }) {
                    Icon(Icons.Filled.Videocam, contentDescription = "Video call")
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
                        jumping = true
                        telegram.jumpToStart(chat.id) {
                            scope.launch {
                                try {
                                    withFrameNanos { }
                                    if (shown.isNotEmpty()) listState.scrollToItem(shown.lastIndex)
                                    withFrameNanos { }
                                } finally {
                                    jumping = false
                                }
                            }
                        }
                    },
                )
            }
        }
        VideoChatBar(chat, telegram, Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp))
        JoinRequestsBar(chat, telegram, settings, Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp))
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
                    pins.getOrNull(index)?.let { actions.onQuoteClick(it.id) }
                    pinIndex = if (pins.isEmpty()) 0 else (index + 1) % pins.size
                },
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            )
        }
        val lastPress = remember(chat.id) { longArrayOf(0L) }
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .onGloballyPositioned { listOrigin = it.positionInWindow() }
                .pointerInput(chat.id) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
                        if (!currentEvent.buttons.isPrimaryPressed) return@awaitEachGesture
                        val pressedAt = down.uptimeMillis
                        lastPress[0] = pressedAt
                        var moved = false
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Final)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) moved = true
                            if (!change.pressed) break
                        }
                        if (moved || selectingState.value) return@awaitEachGesture
                        scope.launch {
                            delay(FocusAfterClickMs)
                            if (lastPress[0] == pressedAt) ComposerFocus.request = chat.id to System.nanoTime()
                        }
                    }
                }
                .pointerInput(Unit) {
                    val keyAt = { position: androidx.compose.ui.geometry.Offset ->
                        val point = position + listOrigin
                        shown.firstOrNull { message -> !message.service && itemBounds[message.key]?.let { point.y >= it.top && point.y < it.bottom } == true }?.key
                    }
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        if (!currentEvent.buttons.isPrimaryPressed) return@awaitEachGesture
                        val start = keyAt(down.position) ?: return@awaitEachGesture
                        val wasSelecting = selectingState.value
                        if (wasSelecting) down.consume()
                        val base = selected.toList()
                        val wasSelected = start in base
                        var moved = false
                        var pointer = down.position
                        var edgeJob: kotlinx.coroutines.Job? = null
                        val verticalPull = {
                            val delta = pointer - down.position
                            kotlin.math.abs(delta.y) >= SelectPullDp.toPx() && kotlin.math.abs(delta.y) > kotlin.math.abs(delta.x) * SelectPullRatio
                        }
                        val extend = extend@{
                            val current = keyAt(pointer) ?: return@extend
                            if (current == start && !moved && !verticalPull()) return@extend
                            moved = true
                            val from = shown.indexOfFirst { it.key == start }
                            val to = shown.indexOfFirst { it.key == current }
                            if (from < 0 || to < 0) return@extend
                            val range = shown.subList(minOf(from, to), maxOf(from, to) + 1).filter { !it.service }.map { it.key }
                            selected.clear()
                            selected.addAll((base + range).distinct().take(SelectionLimit))
                        }
                        try {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val change = event.changes.firstOrNull() ?: break
                                if (!change.pressed) {
                                    if (wasSelecting || moved) change.consume()
                                    break
                                }
                                pointer = change.position
                                if (!moved && (pointer - down.position).getDistance() < viewConfiguration.touchSlop) continue
                                if (!wasSelecting && !moved) {
                                    val current = keyAt(pointer)
                                    if (current == null || current == start && !verticalPull()) continue
                                }
                                change.consume()
                                extend()
                                if (edgeJob == null && moved) {
                                    edgeJob = selectScope.launch {
                                        while (true) {
                                            withFrameNanos { }
                                            val y = pointer.y
                                            val speed = when {
                                                y < SelectEdgePx -> (SelectEdgePx - y) / SelectEdgePx
                                                y > size.height - SelectEdgePx -> -(y - (size.height - SelectEdgePx)) / SelectEdgePx
                                                else -> 0f
                                            }.coerceIn(-1f, 1f)
                                            if (speed != 0f) {
                                                listState.dispatchRawDelta(speed * SelectScrollPxPerFrame)
                                                extend()
                                            }
                                        }
                                    }
                                }
                            }
                        } finally {
                            edgeJob?.cancel()
                        }
                        if (wasSelecting && !moved) {
                            if (wasSelected) {
                                selected.remove(start)
                            } else if (selected.size < SelectionLimit) {
                                selected.add(start)
                            }
                        }
                    }
                },
        ) {
            ChatWallpaper(chat, telegram, settings, com.pixelgram.desktop.ui.theme.isDarkTheme(settings))
            var listWidth by remember { mutableStateOf(0) }
            val wide = when (settings.messageSides.value) {
                com.pixelgram.desktop.core.MessageSides.OneSide -> true
                com.pixelgram.desktop.core.MessageSides.BothSides -> false
                com.pixelgram.desktop.core.MessageSides.Auto -> with(androidx.compose.ui.platform.LocalDensity.current) { listWidth.toDp() } >= WideChatWidth
            }
            androidx.compose.runtime.CompositionLocalProvider(LocalWideChat provides wide) {
            LazyColumn(
                state = listState,
                reverseLayout = true,
                modifier = Modifier.fillMaxSize().onSizeChanged { listWidth = it.width }.then(with(com.pixelgram.desktop.ui.components.SmoothWheelScope) { Modifier.glide(listState, reverse = true) }),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(GroupInnerGap, Alignment.Bottom),
            ) {
                if (shown.isEmpty()) item(key = TypingKey) { TypingBubble(chat, telegram) }
                itemsIndexed(shown, key = { _, message -> message.key }, contentType = { _, message -> messageContentType(message) }) { index, message ->
                    val newDay = index == shown.lastIndex || !sameDay(message.date, shown[index + 1].date)
                    val firstInRun = groupStarts.getOrElse(index) { true }
                    val typingJoins = index == 0 && (chat.isGroup || wide) && typingSameSender(chat, telegram, message, settings)
                    val lastInRun = !typingJoins && (index == 0 || groupStarts.getOrElse(index - 1) { true })
                    val rise = remember { Animatable(if (PowerSaving.messageAnimations && message.fresh && slidIn.add(message.key)) 1f else 0f) }
                    if (rise.value != 0f || rise.isRunning) LaunchedEffect(Unit) { rise.animateTo(0f, slide) }
                    val flash = remember { Animatable(0f) }
                    val flashed = highlight?.takeIf { it.first == message.id }
                    if (flashed != null) {
                        LaunchedEffect(flashed) {
                            flash.snapTo(0f)
                            flash.animateTo(1f, tween(PulseInMs, easing = androidx.compose.animation.core.FastOutSlowInEasing))
                            kotlinx.coroutines.delay(PulseHoldMs)
                            flash.animateTo(0f, tween(PulseOutMs, easing = androidx.compose.animation.core.LinearOutSlowInEasing))
                        }
                    }
                    val itemModifier = Modifier
                        .padding(top = if (firstInRun && !newDay) GroupOuterGap else 0.dp)
                        .graphicsLayer { translationY = rise.value * size.height }
                    val quote: (@Composable () -> Unit)? = if (message.replyToId != 0L) {
                        {
                            val replied = telegram.replyPreview(chat.id, message.replyToId)
                            val gone = replied == null && telegram.replyDeleted(chat.id, message.replyToId)
                            ReplyQuote(
                                name = when {
                                    gone -> "Deleted message"
                                    replied == null -> "Message"
                                    replied.isOutgoing -> telegram.senderName(replied).ifBlank { "You" }
                                    else -> telegram.senderName(replied)
                                },
                                text = if (gone) "" else replied?.text?.takeIf { it.isNotEmpty() } ?: if (replied?.photo != null) "Photo" else replied?.let { contentText(it.content) }?.ifBlank { null } ?: "…",
                                onClick = { actions.onQuoteClick(message.replyToId) },
                                replied = replied,
                                telegram = telegram,
                            )
                        }
                    } else {
                        null
                    }
                    androidx.compose.runtime.CompositionLocalProvider(LocalBubblePulse provides { flash.value }, LocalBubbleGroup provides BubbleGroup(top = firstInRun || newDay, bottom = lastInRun || (index > 0 && !sameDay(message.date, shown[index - 1].date)))) {
                    Column(
                        if (PowerSaving.messageAnimations) {
                            Modifier.animateItem(fadeInSpec = if (message.fresh || jumping) null else fade, placementSpec = if (listState.isScrollInProgress || jumping) null else placement, fadeOutSpec = if (jumping) null else fade)
                        } else {
                            Modifier
                        },
                    ) {
                        if (newDay) DateDivider(message.date) { calendarAt = message.date }
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
                            val flashTint = MaterialTheme.colorScheme.secondaryContainer
                            val highlight by animateFloatAsState(if (isSelected) SelectedAlpha else 0f, MaterialTheme.motionScheme.defaultEffectsSpec())
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .onGloballyPositioned { itemBounds[message.key] = it.boundsInWindow() }
                                    .clip(RoundedCornerShape(24.dp))
                                    .drawBehind {
                                        if (highlight > 0f) drawRect(selectTint.copy(alpha = highlight))
                                        val glow = flash.value
                                        if (glow > 0f) drawRect(flashTint.copy(alpha = HighlightAlpha * glow))
                                    },
                            ) {
                            AnimatedVisibility(
                                visible = selecting,
                                enter = expandHorizontally(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeIn(),
                                exit = shrinkHorizontally(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeOut(),
                            ) {
                                val pop by animateFloatAsState(if (isSelected) 1f else 0.85f, MaterialTheme.motionScheme.fastSpatialSpec())
                                Crossfade(isSelected, animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()) { checked ->
                                    Icon(
                                        if (checked) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                                        contentDescription = null,
                                        tint = if (checked) selectTint else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.padding(horizontal = 8.dp).size(22.dp).graphicsLayer {
                                            scaleX = pop
                                            scaleY = pop
                                        },
                                    )
                                }
                            }
                            Box(Modifier.weight(1f)) {
                            val groupChat = chat.isGroup && !chat.isChannel && !message.isOutgoing
                            val sentAsChat = chat.isGroup && !chat.isChannel && message.isOutgoing &&
                                message.senderChatId != 0L && message.senderChatId != chat.id ||
                                chat.isChannel && message.senderUserId != 0L
                            MessageGestures(message, actions, itemModifier) {
                              Row(verticalAlignment = Alignment.Bottom) {
                                if (groupChat || (wide && !chat.isChannel)) {
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
                                    message = if (chat.isChannel && message.isOutgoing) message.copy(isOutgoing = false) else message,
                                    modifier = Modifier.weight(1f),
                                    senderName = if ((groupChat || sentAsChat) && firstInRun) telegram.senderName(message) else null,
                                    senderTag = if (groupChat && firstInRun) telegram.senderTag(chat, message) else null,
                                    settings = settings,
                                    status = if (message.isOutgoing && !chat.isChannel) telegram.status(chat, message) else null,
                                    onReact = { emoji -> telegram.toggleReaction(message, emoji) },
                                    onButton = { button ->
                                        telegram.pressButton(message, button) { text, alert -> botAnswer = text to alert }
                                        ComposerFocus.request = chat.id to System.nanoTime()
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
                        if (index == 0) TypingBubble(chat, telegram)
                    }
                    }
                }
            }
            }
            profileChat?.let { ChatInfoDialog(it, telegram, settings, onOpenChat) { profileChat = null } }
            if (savedHub) SavedMessagesDialog(chat, telegram, settings, onOpenChat) { savedHub = false }
            StickerPack.current?.takeIf { it.chatId == chat.id && it.telegram == telegram }?.let { request ->
                StickerPackDialog(request, settings) { StickerPack.current = null }
            }
            BotAnswer(botAnswer, onDismiss = { botAnswer = null }, modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp))
            FloatingDate(
                listState = listState,
                dateAt = { index -> shown.getOrNull(index)?.date },
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
                onClick = { date -> calendarAt = date },
            )
            calendarAt?.let { date -> ChatCalendar(chat, telegram, date) { calendarAt = null } }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            ) {
                LaunchedEffect(chat.id, listState) {
                    androidx.compose.runtime.snapshotFlow { listState.layoutInfo.visibleItemsInfo.mapNotNull { (it.key as? Long)?.let { key -> shown.firstOrNull { m -> m.key == key }?.id } } }
                        .collect { ids ->
                            kotlinx.coroutines.delay(SeenDelayMs)
                            telegram.sawMessages(chat.id, ids)
                        }
                }
                val live = telegram.liveReactions[chat.id]?.size ?: 0
                UnreadJumpButton(
                    maxOf(chat.unreadReactions, live),
                    com.pixelgram.desktop.ui.icons.Icons.Outlined.FavoriteBorder,
                    "Next unread reaction",
                    onJump = {
                        val local = telegram.takeLiveReaction(chat.id)
                        if (local != null) actions.onQuoteClick(local) else telegram.nextUnread(chat.id, mentions = false) { id -> actions.onQuoteClick(id) }
                    },
                    onReadAll = { telegram.readAllReactions(chat) },
                )
                UnreadJumpButton(
                    chat.unreadMentions,
                    com.pixelgram.desktop.ui.icons.Icons.Outlined.AlternateEmail,
                    "Next unread mention",
                    onJump = { telegram.nextUnread(chat.id, mentions = true) { id -> actions.onQuoteClick(id) } },
                    onReadAll = { telegram.readAllMentions(chat) },
                )
                ScrollDownButton(
                    listState,
                    forceVisible = detached || returnStack.isNotEmpty(),
                    onLatest = when {
                        returnStack.isNotEmpty() -> {
                            { MessageSearch.jump = chat.id to returnStack.removeAt(returnStack.lastIndex) }
                        }
                        detached -> {
                            { telegram.returnToLatest(chat.id) { scope.launch { listState.scrollToItem(0) } } }
                        }
                        else -> null
                    },
                )
            }
            if (chat.privateUserId != 0L && !telegram.isBot(chat) && !telegram.isSavedMessages(chat.id)) {
                var greet by remember(chat.id) { mutableStateOf(false) }
                val noMessages = shown.isEmpty()
                LaunchedEffect(chat.id, noMessages) {
                    greet = false
                    if (!noMessages) return@LaunchedEffect
                    delay(WelcomeDelayMs)
                    greet = true
                }
                if (greet) EmptyChatGreeting(chat, telegram, Modifier.align(Alignment.Center).padding(16.dp))
            }
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
                onEdit = if (modal != null) editAttachment else null,
                spoiler = spoiler,
                onSpoiler = { spoiler = it },
                onSendWith = if (editingPhoto) null else { how ->
                    sendWith = how
                    sendRequested = true
                },
            )
        }
        val barMode = when {
            selecting -> BarMode.Select
            telegram.secretStatus(chat) == SecretStatus.Pending -> BarMode.SecretPending
            telegram.secretStatus(chat) == SecretStatus.Closed -> BarMode.SecretClosed
            telegram.isBlocked(chat) -> BarMode.Blocked
            telegram.needsStart(chat) -> BarMode.Start
            telegram.isLeft(chat) -> BarMode.Join
            !telegram.canPost(chat) -> BarMode.Mute
            telegram.writeRestriction(chat) != null -> BarMode.Restricted
            else -> BarMode.Compose
        }
        val barFadeIn = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
        val barSlide = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
        val barFadeOut = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
        AnimatedContent(
            targetState = barMode,
            transitionSpec = {
                (fadeIn(barFadeIn) + slideInVertically(barSlide) { it / 2 }) togetherWith
                    fadeOut(barFadeOut) using SizeTransform(clip = false)
            },
        ) { mode ->
        Column {
        if (mode == BarMode.Select) {
            SelectionBar(
                count = selected.size,
                canCopy = !chat.protectedContent,
                onForward = { forwardingSelected = true },
                onDelete = { deletingSelected = true },
                onShot = { shot = true },
                onCancel = { selected.clear() },
            )
        } else if (mode == BarMode.SecretPending) {
            ComposerBar("Waiting for the contact to get online…", enabled = false) {}
        } else if (mode == BarMode.SecretClosed) {
            ComposerBar("Secret chat cancelled", enabled = false) {}
        } else if (mode == BarMode.Blocked) {
            ComposerBar("UNBLOCK", accent = true) { telegram.unblock(chat) }
        } else if (mode == BarMode.Start) {
            ComposerBar("START", accent = true, detail = telegram.botStarts[chat.id]?.takeIf { it.isNotEmpty() }) { telegram.sendStart(chat) }
        } else if (mode == BarMode.Join) {
            var joining by remember(chat.id) { mutableStateOf(false) }
            ComposerBar(if (chat.isChannel) "JOIN CHANNEL" else "JOIN GROUP", accent = true, enabled = !joining) {
                joining = true
                telegram.joinChat(chat.id) { joining = false }
            }
        } else if (mode == BarMode.Restricted) {
            val (personal, until) = telegram.writeRestriction(chat) ?: (false to 0)
            val text = when {
                !personal -> com.pixelgram.desktop.core.tr("Writing messages isn't allowed in this group.")
                until > 0 -> com.pixelgram.desktop.core.tr("The admins of this group have restricted your ability to send messages until") + " " +
                    java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", java.util.Locale.forLanguageTag(com.pixelgram.desktop.core.I18n.code))
                        .withZone(java.time.ZoneId.systemDefault()).format(java.time.Instant.ofEpochSecond(until.toLong())) + "."
                else -> com.pixelgram.desktop.core.tr("The admins of this group have restricted your ability to send messages.")
            }
            ComposerBar(text, enabled = false) {}
        } else if (mode == BarMode.Mute) {
            val muted = telegram.isMuted(chat)
            ComposerBar(if (muted) "UNMUTE" else "MUTE") { telegram.setMuted(chat, !muted) }
        } else Composer(
            chatId = chat.id,
            inGroup = chat.isGroup,
            telegram = telegram,
            settings = settings,
            hasAttachments = attachments.isNotEmpty(),
            sendRequested = sendRequested,
            onSendRequestHandled = { sendRequested = false },
            editing = editing,
            replyTo = replyTo,
            onCancelReply = { replyTo = null },
            onEditLast = {
                val now = System.currentTimeMillis() / 1000
                val last = messages.firstOrNull {
                    it.isOutgoing && it.editable && !it.service && !it.sending && !it.failed
                }?.takeIf { now - it.date < EditWindowSeconds || telegram.isSavedMessages(chat.id) }
                editing = last
                last?.let { candidate ->
                    telegram.canEdit(candidate) { allowed -> if (!allowed && editing?.key == candidate.key) editing = null }
                }
                last != null
            },
            onCancelEdit = { editing = null },
            onAttach = attach,
            onAttachDocuments = { files ->
                attach(files)
                if (files.isNotEmpty()) asFiles = true
            },
            onOpenChat = onOpenChat,
            onDeleteEditing = { editing?.let { deleting = it } },
            onScrollBottom = { telegram.returnToLatest(chat.id) { scope.launch { listState.scrollToItem(0) } } },
            onSendWith = { how ->
                sendWith = how
                sendRequested = true
            },
            onSend = { text, entities ->
                val edited = editing
                val replyId = replyTo?.id ?: 0L
                replyTo = null
                if (edited != null && edited.photo != null) {
                    val file = attachments.firstOrNull()
                    when {
                        file == null -> Unit
                        file == editPhoto -> telegram.editMessage(edited, text, entities)
                        else -> telegram.editMessageMedia(edited, file, text, !asFiles && (Attachments.isPhoto(file) || Attachments.isAnimation(file)), entities)
                    }
                    editing = null
                } else if (edited != null) {
                    telegram.editMessage(edited, text, entities)
                    editing = null
                } else if (attachments.isNotEmpty()) {
                    val photos = !asFiles && attachments.all(Attachments::isMedia)
                    val how = sendWith ?: AttachmentSend()
                    telegram.sendFiles(
                        chat.id,
                        attachments.toList(),
                        photos,
                        text,
                        replyId,
                        entities,
                        silent = how.silent,
                        schedule = how.schedule,
                        spoiler = photos && spoiler,
                        highQuality = Attachments.highQuality,
                    )
                    attachments.clear()
                    asFiles = false
                    spoiler = false
                } else if (text.isNotBlank()) {
                    val code = if (com.pixelgram.desktop.core.Tweaks.settings?.pythonMessages?.value == true) com.pixelgram.desktop.core.PythonRunner.code(text) else null
                    if (code != null) {
                        val chatId = chat.id
                        Thread {
                            val (output, quotes) = com.pixelgram.desktop.core.PythonRunner.message(code, com.pixelgram.desktop.core.PythonRunner.run(code))
                            telegram.sendText(chatId, output, replyId, quotes)
                        }.apply { isDaemon = true }.start()
                    } else {
                        val how = sendWith ?: AttachmentSend()
                        telegram.sendText(chat.id, text, replyId, entities, how.silent, how.schedule)
                    }
                }
                sendWith = null
                if (edited == null) PendingForward.send(telegram, chat.id)
                if (listState.firstVisibleItemIndex > 0) {
                    scope.launch { listState.animateScrollToItem(0) }
                }
            },
        )
        }
        }
    }
    }
    AnimatedVisibility(
        visible = commentsFor != null,
        enter = slideInHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec()) { it / 3 } + fadeIn(),
        exit = slideOutHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec()) { it / 3 } + fadeOut(),
    ) {
        var shownPost by remember { mutableStateOf(commentsFor) }
        commentsFor?.let { shownPost = it }
        shownPost?.let { post -> CommentsView(post, telegram, settings, onOpenChat) { commentsFor = null } }
    }
    }
    }
    }
}

private enum class BarMode { Select, SecretPending, SecretClosed, Blocked, Start, Join, Mute, Restricted, Compose }

private fun typingSameSender(chat: ChatState, telegram: Telegram, message: UiMessage, settings: AppSettings): Boolean {
    if (settings.typingBubble.value == false || chat.isChannel || message.isOutgoing) return false
    return when (val typer = telegram.typing(chat.id).firstOrNull()) {
        is TdApi.MessageSenderUser -> typer.userId == message.senderUserId
        is TdApi.MessageSenderChat -> typer.chatId == message.senderChatId
        else -> false
    }
}

@Composable
private fun TypingBubble(chat: ChatState, telegram: Telegram) {
    if (com.pixelgram.desktop.ui.theme.LocalAppSettings.current?.typingBubble?.value == false) return
    val typers = telegram.typing(chat.id)
    var last by remember { mutableStateOf<TdApi.MessageSender?>(null) }
    typers.firstOrNull()?.let { last = it }
    AnimatedVisibility(
        visible = typers.isNotEmpty(),
        enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec(), expandFrom = Alignment.Top) + fadeIn(),
        exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec(), shrinkTowards = Alignment.Top) + fadeOut(),
    ) {
        val colors = MaterialTheme.colorScheme
        val group = !chat.isChannel && (chat.isGroup || LocalWideChat.current)
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) {
            val sender = last
            if (group && sender != null) {
                Box(Modifier.width(SenderAvatarSlot)) {
                    val appSettings = com.pixelgram.desktop.ui.theme.LocalAppSettings.current
                    com.pixelgram.desktop.ui.components.AvatarImage(
                        telegram.senderTitle(sender),
                        telegram.senderPhoto(sender),
                        telegram,
                        appSettings?.avatarShapeFor(AvatarKind.Private)?.toShape(appSettings.avatarCorner.value) ?: CircleShape,
                        SenderAvatarSize,
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 6.dp),
                color = colors.surfaceContainerHigh,
            ) {
                TypingDots(colors.onSurfaceVariant, Modifier.padding(horizontal = 14.dp, vertical = 12.dp))
            }
        }
    }
}

@Composable
private fun TypingDots(color: Color, modifier: Modifier) {
    val focused = androidx.compose.ui.platform.LocalWindowInfo.current.isWindowFocused
    val phase = if (focused) {
        val transition = rememberInfiniteTransition()
        transition.animateFloat(0f, 1f, infiniteRepeatable(tween(TypingCycleMs, easing = LinearEasing))).value
    } else {
        0f
    }
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically, modifier = modifier.height(14.dp)) {
        repeat(TypingDotCount) { dot ->
            val local = ((phase - dot * TypingDotDelay) % 1f + 1f) % 1f
            val wave = if (local < 0.5f) kotlin.math.sin(local * 2 * Math.PI).toFloat() else 0f
            Box(
                Modifier
                    .size(7.dp)
                    .graphicsLayer {
                        translationY = -wave * TypingLift.toPx()
                        alpha = 0.45f + 0.55f * wave
                    }
                    .background(color, CircleShape),
            )
        }
    }
}

private const val TypingCycleMs = 1100
private const val TypingKey = "typing"
private const val TypingDotCount = 3
private const val TypingDotDelay = 0.16f
private val TypingLift = 4.dp

@Composable
private fun ComposerBar(label: String, accent: Boolean = false, enabled: Boolean = true, detail: String? = null, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = if (accent) colors.primaryContainer else colors.surfaceContainer,
        contentColor = if (accent) colors.onPrimaryContainer else colors.primary,
        modifier = Modifier.fillMaxWidth().padding(12.dp).heightIn(min = SendSlot + 16.dp),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.heightIn(min = SendSlot + 16.dp)) {
            AnimatedContent(
                targetState = label,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
            ) { text ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                    if (detail != null) {
                        Text(
                            detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalContentColor.current.copy(alpha = DetailAlpha),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            }
        }
    }
}

private const val DetailAlpha = 0.7f

class BubbleGroup(val top: Boolean, val bottom: Boolean)

val LocalBubbleGroup = androidx.compose.runtime.staticCompositionLocalOf { BubbleGroup(top = true, bottom = true) }

fun bubbleShape(radius: Int, tailRadius: Int, outgoing: Boolean, group: BubbleGroup = BubbleGroup(top = true, bottom = true)): Shape {
    val r = radius.dp
    val joined = r * JoinedRatio
    val sideTop = if (group.top) r else joined
    val sideBottom = if (group.bottom) minOf(tailRadius.dp, r) else joined
    return if (outgoing) {
        RoundedCornerShape(topStart = r, topEnd = sideTop, bottomEnd = sideBottom, bottomStart = r)
    } else {
        RoundedCornerShape(topStart = sideTop, topEnd = r, bottomEnd = r, bottomStart = sideBottom)
    }
}

private const val JoinedRatio = 6f / 16f

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
    senderTag: SenderTag? = null,
) {
    val seconds = settings.messageSeconds.value
    remember(message.chatId, message.id) { ReactionMemory.note(message) }
    if (message.reactions.isEmpty()) ReactionMemory.forget(message.chatId to message.id)
    if (message.service) {
        val serviceText = listOfNotNull(senderName?.takeIf { it.isNotBlank() }, message.text).joinToString(" · ")
        val newPhoto = (message.content as? TdApi.MessageChatChangePhoto)?.photo
        if (newPhoto != null && telegram != null) {
            Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                ServiceMessage(serviceText, message.date, seconds, Modifier)
                val small = newPhoto.sizes.minByOrNull { it.width }?.photo
                val big = newPhoto.sizes.maxByOrNull { it.width }?.photo
                com.pixelgram.desktop.ui.components.AvatarImage(serviceText, small, telegram, CircleShape, ServicePhotoSize, Modifier.padding(vertical = 4.dp), full = big)
            }
        } else {
            ServiceMessage(serviceText, message.date, seconds, modifier)
        }
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
    val bigEmoji = remember(message.text, message.entities, content) { bigCustomEmoji(message) }
    if (telegram != null && bigEmoji != null) {
        BareMessage(message, telegram, settings, status, seconds, quote, modifier, onReact) { BigEmojiRow(bigEmoji) }
        return
    }
    if (telegram != null && isBareContent(content)) {
        BareMessage(message, telegram, settings, status, seconds, quote, modifier, onReact)
        return
    }
    val colors = MaterialTheme.colorScheme
    val outgoing = message.isOutgoing
    val endSide = outgoing && !LocalWideChat.current
    val grouped = message.album.takeIf { it.size > 1 && telegram != null }
    val album = grouped?.takeIf { members -> members.all { it.content is TdApi.MessagePhoto || isMediaContent(it.content) } }
    val files = grouped?.takeIf { album == null }
    val photo = if (grouped != null) null else message.photo
    val media = grouped == null && telegram != null && isMediaContent(content)
    val block = grouped == null && telegram != null && isRich(content) && !media
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = if (endSide) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        val pulseNow = LocalBubblePulse.current
        Surface(
            shape = bubbleShape(
                settings.bubbleRadius.value,
                settings.bubbleTailRadius.value,
                endSide,
                LocalBubbleGroup.current,
            ),
            color = if (outgoing) colors.primaryContainer else colors.surfaceContainerHigh,
            contentColor = if (outgoing) colors.onPrimaryContainer else colors.onSurface,
            modifier = Modifier.widthIn(max = 520.dp).graphicsLayer {
                val pop = pulseNow()
                scaleX = 1f + pop * HighlightPop
                scaleY = 1f + pop * HighlightPop
            },
        ) {
            val metaColor = (if (outgoing) colors.onPrimaryContainer else colors.onSurfaceVariant).copy(alpha = 0.7f)
            val readColor = if (outgoing) colors.outgoingAccent else metaColor
            val hasText = message.text.isNotEmpty()
            val inset = photo != null || media || album != null
            Column(
                Modifier
                    .padding(if (inset) PhotoInset else 0.dp)
                    .then(if (inset) Modifier.widthIn(max = PhotoMaxWidth) else Modifier)
                    .width(IntrinsicSize.Max),
            ) {
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
                            senderTag?.let { MemberTagPill(it.text, it.rank) }
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
                val origin = message.forwardOrigin
                if (origin != null && telegram != null) {
                    val reachable = onOrigin != null && origin !is TdApi.MessageOriginHiddenUser
                    val originName = telegram.forwardName(origin).ifBlank { "unknown" }
                    val originPhoto = when (origin) {
                        is TdApi.MessageOriginUser -> telegram.users[origin.senderUserId]?.profilePhoto?.small
                        is TdApi.MessageOriginChat -> telegram.chats[origin.senderChatId]?.photo
                        is TdApi.MessageOriginChannel -> telegram.chats[origin.chatId]?.photo
                        else -> null
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .padding(start = if (inset) 8.dp else 12.dp, end = 12.dp, top = if (inset || named) 4.dp else 7.dp, bottom = 4.dp)
                            .then(if (photo != null || media || album != null) Modifier.widthIn(max = (if (album != null) PhotoMaxWidth else mediaFrameOf(message)?.width ?: PhotoMaxWidth) - 20.dp) else Modifier)
                            .then(
                                if (reachable) {
                                    Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .pointerHoverIcon(PointerIcon.Hand)
                                        .clickable { onOrigin?.invoke(origin) }
                                } else {
                                    Modifier
                                },
                            ),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.Forward,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(14.dp),
                        )
                        Text("Forwarded from", style = MaterialTheme.typography.labelMedium, color = colors.primary, maxLines = 1)
                        if (originPhoto != null) Avatar(originName, originPhoto, telegram, settings, ForwardAvatarSize)
                        EmojiText(
                            originName,
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                }
                if (quote != null) {
                    val radius = settings.bubbleRadius.value
                    val extra = ((radius - DefaultBubbleRadius).coerceAtLeast(0) * QuoteGrowth).dp
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .widthIn(min = QuoteMinWidth)
                            .padding(start = 8.dp + extra, end = 8.dp + extra, top = if (named) 0.dp else 8.dp + extra / 2, bottom = if (photo != null || media || album != null) 6.dp else 0.dp),
                    ) {
                        androidx.compose.runtime.CompositionLocalProvider(LocalQuoteCorner provides (radius.dp - 8.dp - extra).coerceAtLeast(10.dp)) { quote() }
                    }
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
                        MediaContent(message, telegram, (settings.bubbleRadius.value.dp - PhotoInset).coerceAtLeast(4.dp))
                    }
                }
                if (!(message.captionAbove && hasText)) mediaBlock()
                if (hasText || (photo == null && album == null)) {
                    Column(
                        Modifier
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                            .then(if (photo != null || media || album != null) Modifier.width((if (album != null) PhotoMaxWidth else mediaFrameOf(message)?.width ?: PhotoMaxWidth) - 24.dp) else Modifier),
                    ) {
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
                                        message.chatId to message.id,
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
                            message.chatId to message.id,
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
                if (message.comments >= 0 && onComments != null) CommentsBar(message.comments, onComments, message.commenters, telegram)
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
    body: @Composable () -> Unit = { BareContent(message, telegram) },
) {
    remember(message.chatId, message.id) { ReactionMemory.note(message) }
    if (message.reactions.isEmpty()) ReactionMemory.forget(message.chatId to message.id)
    val endSide = message.isOutgoing && !LocalWideChat.current
    Box(modifier.fillMaxWidth(), contentAlignment = if (endSide) Alignment.CenterEnd else Alignment.CenterStart) {
        Column(
            horizontalAlignment = if (endSide) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (quote != null) Box(Modifier.widthIn(min = QuoteMinWidth, max = 280.dp)) { quote() }
            body()
            if (message.reactions.isNotEmpty() && onReact != null) {
                ReactionChips(
                    message.chatId to message.id,
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

private fun bigCustomEmoji(message: UiMessage): List<Pair<String, Long>>? {
    if (message.content !is TdApi.MessageText) return null
    val text = message.text
    if (text.isBlank()) return null
    val entities = com.pixelgram.desktop.core.LocalPremiumEmoji.incoming(text, message.entities.toTypedArray())
    val custom = entities.mapNotNull { entity -> (entity.type as? TdApi.TextEntityTypeCustomEmoji)?.let { entity to it.customEmojiId } }.sortedBy { it.first.offset }
    if (custom.isEmpty() || custom.size > BigEmojiMax) return null
    val covered = BooleanArray(text.length)
    custom.forEach { (entity, _) -> for (i in entity.offset until (entity.offset + entity.length).coerceAtMost(text.length)) covered[i] = true }
    if (text.indices.any { !covered[it] && !text[it].isWhitespace() }) return null
    return custom.map { (entity, id) -> text.substring(entity.offset, (entity.offset + entity.length).coerceAtMost(text.length)) to id }
}

@Composable
private fun BigEmojiRow(emoji: List<Pair<String, Long>>) {
    val size = BigEmojiSizes.getOrElse(emoji.size - 1) { BigEmojiSizes.last() }
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.widthIn(max = 420.dp)) {
        emoji.forEach { (fallback, id) -> CustomEmojiView(id, fallback, Modifier.size(size), openPack = true) }
    }
}

private const val BigEmojiMax = 7
private val BigEmojiSizes = listOf(112.dp, 88.dp, 72.dp, 60.dp, 52.dp, 46.dp, 40.dp)

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
    com.pixelgram.desktop.core.Tweaks.exactNumbers -> "%,d".format(java.util.Locale.US, count)
    count >= 1_000_000 -> "%.1fM".format(java.util.Locale.US, count / 1_000_000f).replace(".0M", "M")
    count >= 1_000 -> "%.1fK".format(java.util.Locale.US, count / 1_000f).replace(".0K", "K")
    else -> count.toString()
}

private sealed class TextPart {
    class Plain(val text: String, val entities: List<TdApi.TextEntity>) : TextPart()
    class Code(val code: String, val language: String) : TextPart()
}

private fun splitCode(text: String, entities: List<TdApi.TextEntity>): List<TextPart> {
    val blocks = entities.filter { it.type is TdApi.TextEntityTypePre || it.type is TdApi.TextEntityTypePreCode }.sortedBy { it.offset }
    val parts = mutableListOf<TextPart>()
    var cursor = 0
    fun plain(from: Int, to: Int) {
        var start = from
        var end = to
        while (start < end && text[start] == '\n') start++
        while (end > start && text[end - 1] == '\n') end--
        if (start >= end) return
        val inside = entities.filter { it !in blocks && it.offset < end && it.offset + it.length > start }.map { entity ->
            val a = maxOf(entity.offset, start)
            val b = minOf(entity.offset + entity.length, end)
            TdApi.TextEntity(a - start, b - a, entity.type)
        }
        parts += TextPart.Plain(text.substring(start, end), inside)
    }
    blocks.forEach { block ->
        val start = block.offset.coerceIn(cursor, text.length)
        val end = (block.offset + block.length).coerceIn(start, text.length)
        plain(cursor, start)
        parts += TextPart.Code(text.substring(start, end).trimEnd('\n'), (block.type as? TdApi.TextEntityTypePreCode)?.language.orEmpty())
        cursor = end
    }
    plain(cursor, text.length)
    return parts
}

@Composable
private fun TextWithCode(text: String, entities: List<TdApi.TextEntity>, meta: @Composable () -> Unit) {
    val parts = remember(text, entities) { splitCode(text, entities) }
    val style = com.pixelgram.desktop.ui.theme.messageTextStyle()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        parts.forEachIndexed { index, part ->
            val last = index == parts.lastIndex
            when (part) {
                is TextPart.Plain -> if (last) {
                    TextWithMeta(part.text, part.entities, meta)
                } else {
                    MessageSelection { FormattedText(part.text, part.entities, style = style) }
                }
                is TextPart.Code -> com.pixelgram.desktop.ui.components.CodeBlock(part.code, part.language, style, Modifier.widthIn(min = CodeMinWidth))
            }
        }
        if (parts.lastOrNull() !is TextPart.Plain) Box(Modifier.align(Alignment.End)) { meta() }
    }
}

private val CodeMinWidth = 220.dp

@Composable
private fun TextWithMeta(text: String, entities: List<TdApi.TextEntity>, meta: @Composable () -> Unit) {
    if (entities.any { it.type is TdApi.TextEntityTypePre || it.type is TdApi.TextEntityTypePreCode }) {
        TextWithCode(text, entities, meta)
        return
    }
    val layoutRef = remember { arrayOfNulls<TextLayoutResult>(1) }
    val quoteAtEnd = remember(text, entities) { endsWithQuote(text, entities) }
    val quotes = remember(text, entities) { com.pixelgram.desktop.ui.components.quoteRanges(text, entities) }
    val quoteColor = MaterialTheme.colorScheme.primary
    Layout(
        modifier = Modifier.fillMaxWidth().drawBehind {
            val current = layoutRef[0] ?: return@drawBehind
            quotes.forEach { range -> drawQuote(current, range, quoteColor, size.width) }
        },
        content = {
            if (text.isNotEmpty()) {
                MessageSelection {
                    FormattedText(text, entities, style = com.pixelgram.desktop.ui.theme.messageTextStyle(), onTextLayout = { layoutRef[0] = it }, quoteBackground = false)
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
                val lines = layoutRef[0]?.takeIf { text.isNotEmpty() }
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
private const val SwapScaleFrom = 0.6f
private val SendSlot = 40.dp
private const val SwapMs = 180
private const val SeenDelayMs = 600L
private val SendButtonSlot = 48.dp
private val ServicePhotoSize = 120.dp
object VoiceButton {
    var preferred by mutableStateOf(true)
}
private const val SelectedAlpha = 0.14f
private const val SelectEdgePx = 64f
private const val SelectScrollPxPerFrame = 9f
private const val MentionDelayMs = 150L
private val KeyboardMinWidth = 260.dp

object ComposerInsert {
    var request by mutableStateOf<Pair<Long, String>?>(null)
}

object ComposerFocus {
    var request by mutableStateOf<Pair<Long, Long>?>(null)
}

object ComposerSend {
    var request by mutableStateOf<Pair<Long, Long>?>(null)
}

private const val FocusAfterClickMs = 360L

object PluginViewer {
    var path by mutableStateOf<String?>(null)
}
private const val StickToBottomPx = 48
private const val PulseInMs = 320
private const val PulseHoldMs = 700L
private const val PulseOutMs = 900
private const val HighlightAlpha = 0.85f
private const val HighlightPop = 0.035f
private val PulseOutset = 8.dp
private const val PulseAlpha = 0.75f

val LocalBubblePulse = androidx.compose.runtime.staticCompositionLocalOf<() -> Float> { { 0f } }

private fun Modifier.pulseRing(shape: androidx.compose.ui.graphics.Shape, color: Color, pulse: () -> Float): Modifier = drawBehind {
    val value = pulse()
    if (value <= 0f) return@drawBehind
    val outset = PulseOutset.toPx() * value
    val outline = shape.createOutline(androidx.compose.ui.geometry.Size(size.width + outset * 2, size.height + outset * 2), layoutDirection, this)
    translate(-outset, -outset) {
        drawOutline(outline, color.copy(alpha = PulseAlpha * value), style = androidx.compose.ui.graphics.drawscope.Stroke(width = outset * 1.4f))
    }
}
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
private const val DefaultBubbleRadius = 18
private val SelectPullDp = 18.dp
private const val SelectPullRatio = 1.5f
private const val QuoteGrowth = 0.35f
private val PhotoMaxWidth = MediaMaxWidth

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
    val image by produceState(com.pixelgram.desktop.ui.components.cachedImage(path, PhotoPixels), path) {
        path?.let { loadImage(it, maxSize = PhotoPixels) }?.let { value = it }
    }
    val spoiler = (message.content as? TdApi.MessagePhoto)?.hasSpoiler == true
    var revealed by remember(message.key) { mutableStateOf(false) }
    val hidden = spoiler && !revealed
    val blur by androidx.compose.animation.core.animateDpAsState(if (hidden) SpoilerBlur else 0.dp, tween(SpoilerRevealMs))
    Box(
        Modifier
            .size(mediaFrame(message.photoWidth, message.photoHeight, 4f / 3f))
            .clip(RoundedCornerShape(corner))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable {
                if (hidden) revealed = true
                else PhotoViewer.current = ViewedPhoto(telegram, file, message.fullPhoto ?: file, message.text, message.chatId, message.id)
            },
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = image
        if (bitmap != null) {
            Image(bitmap, contentDescription = "Photo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().blur(blur, androidx.compose.ui.draw.BlurredEdgeTreatment.Rectangle))
            if (blur > 0.5.dp) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = SpoilerScrim * (blur / SpoilerBlur))))
                if (hidden) {
                    Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.45f), contentColor = Color.White) {
                        Text("Spoiler", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
                    }
                }
            }
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
private val SpoilerBlur = 28.dp
private const val SpoilerScrim = 0.25f
private const val SpoilerRevealMs = 350
internal val SenderAvatarSlot = 42.dp
private val SenderAvatarSize = 34.dp

private fun messageGroupStarts(list: List<UiMessage>): BooleanArray {
    val starts = BooleanArray(list.size)
    var count = 0
    for (i in list.indices.reversed()) {
        val older = list.getOrNull(i + 1)
        val joins = sameSender(older, list[i]) && older != null && list[i].date - older.date <= GroupWindowSeconds && count < GroupLimit
        starts[i] = !joins
        count = if (joins) count + 1 else 1
    }
    return starts
}

private const val GroupWindowSeconds = 5 * 60
private const val GroupLimit = 100
private val GroupInnerGap = 2.dp
private val GroupOuterGap = 6.dp

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
    val me = telegram.me?.takeIf { message.isOutgoing && message.senderChatId == 0L }
    val photo = if (message.senderUserId != 0L) {
        telegram.users[message.senderUserId]?.profilePhoto?.small ?: me?.profilePhoto?.small
    } else {
        telegram.chats[message.senderChatId]?.photo
    }
    Avatar(
        telegram.senderName(message).ifBlank { me?.fullName().orEmpty() },
        photo,
        telegram,
        settings,
        SenderAvatarSize,
        kind = AvatarKind.Private,
        onClick = onClick,
    )
}
private const val DraftSaveDelayMs = 1_500L
private const val PanelReopenGuardMs = 300L
private val ForwardAvatarSize = 16.dp
private const val InlineDelayMs = 400L
private const val GifTab = 2
private const val CommandLimit = 30
private const val JumpCloserItems = 20

val LocalWideChat = androidx.compose.runtime.staticCompositionLocalOf { false }
private val WideChatWidth = 700.dp

private suspend fun revealMessage(listState: LazyListState, index: Int) {
    fun fit(): Float? {
        val info = listState.layoutInfo
        val item = info.visibleItemsInfo.firstOrNull { it.index == index } ?: return null
        val start = info.viewportStartOffset + info.beforeContentPadding
        val end = info.viewportEndOffset - info.afterContentPadding
        val viewport = end - start
        val top = item.offset
        val bottom = item.offset + item.size
        if (top >= start && bottom <= end) return 0f
        val shift = if (item.size <= viewport) top - (start + (viewport - item.size) / 2) else top - start
        return if (info.reverseLayout) -shift.toFloat() else shift.toFloat()
    }
    val visible = fit()
    if (visible != null) {
        if (visible != 0f) listState.animateScrollBy(visible, tween(RevealMs, easing = FastOutSlowInEasing))
        return
    }
    listState.scrollToItem(index)
    withFrameNanos { }
    fit()?.takeIf { it != 0f }?.let { listState.scrollBy(it) }
}

private const val RevealMs = 260
private const val JumpLandingItem = 6
private const val WelcomeDelayMs = 700L

private fun emDash(before: String, typed: TextFieldValue): TextFieldValue {
    val end = typed.selection.end
    if (!typed.selection.collapsed || typed.text.length != before.length + 1 || end < 2) return typed
    val replacement = when (typed.text.substring(end - 2, end)) {
        "--" -> "—"
        ">>" -> "»"
        "<<" -> "«"
        else -> return typed
    }
    if (typed.text.substring(0, end - 1) + typed.text.substring(end) != before) return typed
    return TextFieldValue(typed.text.substring(0, end - 2) + replacement + typed.text.substring(end), TextRange(end - 1))
}

@Composable
private fun Composer(
    chatId: Long,
    inGroup: Boolean,
    telegram: Telegram,
    settings: AppSettings,
    hasAttachments: Boolean,
    sendRequested: Boolean,
    onSendRequestHandled: () -> Unit,
    editing: UiMessage?,
    replyTo: UiMessage?,
    onCancelReply: () -> Unit,
    onEditLast: () -> Boolean,
    onCancelEdit: () -> Unit,
    onAttach: (List<File>) -> Unit,
    onAttachDocuments: (List<File>) -> Unit,
    onSend: (String, Array<TdApi.TextEntity>) -> Unit,
    onDeleteEditing: () -> Unit,
    onOpenChat: (Long) -> Unit = {},
    onScrollBottom: () -> Unit = {},
    onSendWith: ((AttachmentSend) -> Unit)? = null,
) {
    var text by remember(chatId) {
        val draft = telegram.chats[chatId]?.draft.orEmpty()
        mutableStateOf(TextFieldValue(draft, TextRange(draft.length)))
    }
    var spans by remember { mutableStateOf(emptyList<FormatSpan>()) }
    val storedDraft = telegram.chats[chatId]?.draft.orEmpty()
    var savedDraft by remember(chatId) { mutableStateOf(storedDraft) }
    LaunchedEffect(chatId, storedDraft) {
        if (storedDraft == savedDraft) return@LaunchedEffect
        savedDraft = storedDraft
        if (editing == null && storedDraft != text.text) {
            text = TextFieldValue(storedDraft, TextRange(storedDraft.length))
            spans = emptyList()
        }
    }
    LaunchedEffect(chatId, text.text) {
        delay(DraftSaveDelayMs)
        if (editing == null) {
            savedDraft = text.text
            telegram.saveDraft(chatId, text.text)
        }
    }
    val draftHolder = remember(chatId) { arrayOf("") }
    if (editing == null && draftHolder[0] != text.text) {
        draftHolder[0] = text.text
        telegram.noteDraft(chatId, text.text)
    }
    DisposableEffect(chatId) {
        val holder = draftHolder
        onDispose { telegram.saveDraft(chatId, holder[0]) }
    }
    var menuAt by remember { mutableStateOf<DpOffset?>(null) }
    var linkFor by remember { mutableStateOf<TextRange?>(null) }
    var dateFor by remember { mutableStateOf<TextRange?>(null) }
    val setText = { typed: TextFieldValue ->
        val value = emDash(text.text, if (settings.autoCapitalize.value) capitalizeTyped(text.text, typed) else typed)
        val shifted = shiftSpans(spans, text.text, value.text)
        val fenced = if (value.text.contains("```")) autoCodeBlock(value.text, shifted, value.selection.end) else null
        if (fenced != null) {
            spans = fenced.second
            text = TextFieldValue(fenced.first, TextRange(fenced.third))
        } else {
            spans = shifted
            text = value
        }
    }
    val mentionQuery = if (text.selection.collapsed) {
        MentionQuery.find(text.text.substring(0, text.selection.start.coerceAtMost(text.text.length)))?.groupValues?.get(1)
    } else {
        null
    }
    var suggestions by remember { mutableStateOf(emptyList<Long>()) }
    var suggestionIndex by remember { mutableStateOf(0) }
    val latestMention by rememberUpdatedState(mentionQuery)
    LaunchedEffect(mentionQuery, chatId) {
        if (mentionQuery == null) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        delay(MentionDelayMs)
        telegram.mentionSuggestions(chatId, inGroup, mentionQuery) {
            if (latestMention != mentionQuery) return@mentionSuggestions
            suggestions = it
            suggestionIndex = 0
        }
    }
    val inlineMatch = InlineQuery.matchEntire(text.text)
    val inlineName = inlineMatch?.groupValues?.get(1)
    val inlineText = inlineMatch?.groupValues?.get(2)
    val latestInlineName by rememberUpdatedState(inlineName)
    val latestInlineText by rememberUpdatedState(inlineText)
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
            if (latestInlineName != name) return@inlineBot
            inlineBot = it
            inlineResolving = false
        }
    }
    LaunchedEffect(inlineBot?.id, inlineText, chatId) {
        inlineResults = null
        val bot = inlineBot ?: return@LaunchedEffect
        val query = inlineText ?: return@LaunchedEffect
        delay(InlineDelayMs)
        telegram.inlineResults(bot.id, chatId, query) { if (latestInlineText == query) inlineResults = it }
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
    var shownCommands by remember(chatId) { mutableStateOf(commandMatches) }
    if (showCommands) shownCommands = commandMatches
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
    var shownSuggestions by remember(chatId) { mutableStateOf(suggestions) }
    if (showSuggestions) shownSuggestions = suggestions
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
    var mediaSearch by remember { mutableStateOf<String?>(null) }
    val focus = remember { FocusRequester() }
    val forwarding = PendingForward.drafts[chatId] != null
    LaunchedEffect(forwarding) { if (forwarding) runCatching { focus.requestFocus() } }
    val pickScope = rememberCoroutineScope()
    LaunchedEffect(chatId) { runCatching { focus.requestFocus() } }
    var draftBeforeEdit by remember(chatId) { mutableStateOf<Pair<TextFieldValue, List<FormatSpan>>?>(null) }
    LaunchedEffect(editing?.key) {
        if (editing != null) {
            if (draftBeforeEdit == null) draftBeforeEdit = text to spans
            text = TextFieldValue(editing.text, TextRange(editing.text.length))
            spans = spansFrom(editing.entities)
            focus.requestFocus()
        } else {
            draftBeforeEdit?.let { (value, saved) ->
                text = value
                spans = saved
            }
            draftBeforeEdit = null
        }
    }
    LaunchedEffect(ComposerFocus.request) {
        if (ComposerFocus.request?.first == chatId) runCatching { focus.requestFocus() }
    }
    LaunchedEffect(ComposerInsert.request) {
        val (target, value) = ComposerInsert.request ?: return@LaunchedEffect
        if (target != chatId) return@LaunchedEffect
        ComposerInsert.request = null
        setText(TextFieldValue(value, TextRange(value.length)))
        runCatching { focus.requestFocus() }
    }
    val hasForward = editing == null && PendingForward.drafts[chatId] != null
    val canSend = if (editing?.photo != null) hasAttachments else text.text.isNotBlank() || hasAttachments || hasForward
    val send = {
        if (editing != null && editing.photo == null && text.text.isBlank() && !hasAttachments) {
            onDeleteEditing()
        } else if (!canSend && text.text.isNotEmpty() && editing == null) {
            text = TextFieldValue("")
            spans = emptyList()
            onScrollBottom()
        } else if (canSend) {
            val raw = text.text
            val lead = raw.length - raw.trimStart().length
            val trimmed = raw.trim()
            onSend(trimmed, toEntities(spans, lead, trimmed.length))
            text = TextFieldValue("")
            spans = emptyList()
        }
    }
    LaunchedEffect(ComposerSend.request) {
        val (target, _) = ComposerSend.request ?: return@LaunchedEffect
        if (target != chatId) return@LaunchedEffect
        ComposerSend.request = null
        runCatching { focus.requestFocus() }
        if (canSend) send()
    }
    LaunchedEffect(sendRequested) {
        if (sendRequested) {
            onSendRequestHandled()
            send()
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
    ForwardBar(chatId, telegram, settings, onOpenChat)
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
    val insertCustom: (String, Long) -> Unit = { emoji, id ->
        val start = text.selection.min
        val updated = text.text.replaceRange(start, text.selection.max, emoji)
        setText(TextFieldValue(updated, TextRange(start + emoji.length)))
        spans = spans + FormatSpan(start, start + emoji.length, FormatType.CustomEmoji, url = id.toString())
        focus.requestFocus()
    }
    AnimatedVisibility(
        visible = showSuggestions,
        enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeIn(),
        exit = shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeOut(),
    ) {
        MentionSuggestions(
            shownSuggestions,
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
            shownCommands,
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
    voice.camera = settings.cameraDevice.value
    var typingNow by remember(chatId) { mutableStateOf(false) }
    val typedSeen = remember(chatId) { arrayOf<String?>(null) }
    LaunchedEffect(chatId, text.text) {
        val before = typedSeen[0]
        typedSeen[0] = text.text
        if (before == null || text.text.isBlank() || editing != null) {
            typingNow = false
            return@LaunchedEffect
        }
        typingNow = true
        delay(TypingIdleMs)
        typingNow = false
    }
    val ownAction: TdApi.ChatAction? = when {
        voice.video != null -> TdApi.ChatActionRecordingVideoNote()
        voice.recorder != null -> TdApi.ChatActionRecordingVoiceNote()
        emojiOpen && mediaTab >= 1 -> TdApi.ChatActionChoosingSticker()
        typingNow -> TdApi.ChatActionTyping()
        else -> null
    }
    val sentAction = remember(chatId) { arrayOf(false) }
    LaunchedEffect(chatId, ownAction?.javaClass) {
        val action = ownAction
        if (action == null) {
            if (sentAction[0]) telegram.sendAction(chatId, TdApi.ChatActionCancel())
            sentAction[0] = false
            return@LaunchedEffect
        }
        while (true) {
            telegram.sendAction(chatId, action)
            sentAction[0] = true
            delay(ActionRepeatMs)
        }
    }
    val voiceScope = rememberCoroutineScope()
    val releaseVoice: (Boolean) -> Unit = { cancel ->
        if (cancel) {
            voice.cancel()
        } else {
            voiceScope.launch {
                when (val recorded = voice.finish()) {
                    is Recorded.Voice -> telegram.sendVoice(chatId, recorded.note, replyTo?.id ?: 0)
                    is Recorded.Round -> telegram.sendVideoNote(chatId, recorded.note, replyTo?.id ?: 0)
                    null -> return@launch
                }
                onCancelReply()
            }
        }
    }
    LaunchedEffect(voice.limitReached) {
        if (voice.limitReached && voice.active && !voice.collapsing) {
            voice.locked = false
            releaseVoice(false)
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
        val botApp = rememberBotApp(composerChat, telegram)
        if (botApp != null) {
            BotAppButton(botApp)
        } else if (commands.isNotEmpty()) {
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
            }
        }
        AttachButton(
            chat = composerChat,
            telegram = telegram,
            replyToId = replyTo?.id ?: 0,
            onMedia = onAttach,
            onDocuments = onAttachDocuments,
            onSent = onCancelReply,
        )
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
            val mathAnswer = remember(text.text, settings.inlineMath.value) {
                if (settings.inlineMath.value) com.pixelgram.desktop.core.InlineMath.answer(text.text) else null
            }?.takeIf { text.selection.collapsed && text.selection.end == text.text.length }
            val ghostFade = remember { Animatable(0f) }
            LaunchedEffect(mathAnswer) {
                if (mathAnswer == null) ghostFade.snapTo(0f) else ghostFade.animateTo(1f, tween(GhostFadeMs))
            }
            var settled by remember { mutableStateOf<IntRange?>(null) }
            val settle = remember { Animatable(1f) }
            LaunchedEffect(settled) {
                if (settled == null) return@LaunchedEffect
                settle.snapTo(0f)
                settle.animateTo(1f, tween(GhostFadeMs))
                settled = null
            }
            var trailJump by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
            val ghostColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = GhostAlpha)
            com.pixelgram.desktop.ui.components.ComposerField(
                value = text,
                onValueChange = setText,
                placeholder = if (hasAttachments) "Add a caption" else "Message",
                visualTransformation = FormatTransformation(
                    spans,
                    MaterialTheme.colorScheme.primary,
                    com.pixelgram.desktop.ui.components.codeColors(),
                    mathAnswer,
                    ghostColor.copy(alpha = GhostAlpha * ghostFade.value),
                    settled,
                    androidx.compose.ui.graphics.lerp(ghostColor, MaterialTheme.colorScheme.onSurface, settle.value),
                ),
                trailJump = trailJump,
                customEmoji = spans.filter { it.type == FormatType.CustomEmoji && it.end > it.start }.mapNotNull { span -> span.url.toLongOrNull()?.let { (span.start until span.end) to it } },
                settings = settings,
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .focusRequester(focus)
                    .onPreviewKeyEvent { event ->
                        val shortcut = if (event.type == KeyEventType.KeyDown && (event.isCtrlPressed || event.isMetaPressed)) {
                            when {
                                event.isShiftPressed && event.key == Key.X -> FormatType.Strikethrough
                                event.isShiftPressed && event.key == Key.Period -> FormatType.Quote
                                event.isShiftPressed && event.key == Key.M -> FormatType.Monospace
                                event.isShiftPressed && event.key == Key.C -> FormatType.Code
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
                            mathAnswer != null && event.key == Key.DirectionRight && !event.isShiftPressed -> {
                                val filled = text.text + mathAnswer
                                settled = text.text.length until filled.length
                                trailJump++
                                setText(TextFieldValue(filled, TextRange(filled.length)))
                                true
                            }
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
        val composerChat = telegram.chats[chatId]
        if (composerChat?.hasScheduled == true && editing == null) {
            var scheduledOpen by remember(chatId) { mutableStateOf(false) }
            IconButton(onClick = { scheduledOpen = true }, modifier = Modifier.focusProperties { canFocus = false }) {
                Box {
                    Icon(Icons.Outlined.Schedule, contentDescription = "Scheduled messages")
                    Box(Modifier.align(Alignment.TopEnd).size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error))
                }
            }
            if (scheduledOpen) ScheduledDialog(composerChat, telegram) { scheduledOpen = false }
        }
        if (botApp == null && commands.isNotEmpty() && composerChat?.privateUserId == 0L) {
            IconButton(onClick = {
                setText(TextFieldValue("/", TextRange(1)))
                runCatching { focus.requestFocus() }
            }) {
                val tint = MaterialTheme.colorScheme.onSurfaceVariant
                Box(
                    Modifier.size(22.dp).border(2.dp, tint, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("/", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = tint)
                }
            }
        }
        Box {
            var dismissedAt by remember { mutableStateOf(0L) }
            IconToggleButton(
                checked = emojiOpen,
                onCheckedChange = { open ->
                    if (!open || System.currentTimeMillis() - dismissedAt > PanelReopenGuardMs) emojiOpen = open
                    focus.requestFocus()
                },
                modifier = Modifier.focusProperties { canFocus = false },
            ) {
                Icon(Icons.Outlined.EmojiEmotions, contentDescription = "Emoji")
            }
            LaunchedEffect(emojiOpen) { if (!emojiOpen) mediaSearch = null }
            LaunchedEffect(com.pixelgram.desktop.core.AppLock.locked) { if (com.pixelgram.desktop.core.AppLock.locked) emojiOpen = false }
            if (emojiOpen) OnEscape {
                emojiOpen = false
                true
            }
            val panelVisible = remember { MutableTransitionState(false) }
            panelVisible.targetState = emojiOpen
            if (panelVisible.currentState || panelVisible.targetState) {
                Popup(
                    alignment = Alignment.BottomEnd,
                    offset = IntOffset(0, -with(LocalDensity.current) { 56.dp.roundToPx() }),
                    onDismissRequest = {
                        dismissedAt = System.currentTimeMillis()
                        emojiOpen = false
                        focus.requestFocus()
                    },
                    properties = PopupProperties(focusable = mediaTab == GifTab || mediaSearch != null),
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
                        onTab = {
                            mediaTab = it
                            mediaSearch = null
                        },
                        search = mediaSearch,
                        onSearch = { query ->
                            mediaSearch = query
                            if (query == null) focus.requestFocus()
                        },
                        onEmoji = insert,
                        onCustomEmoji = insertCustom,
                        onSendEmoji = { emoji, id ->
                            telegram.sendText(chatId, emoji, replyTo?.id ?: 0, id?.let { arrayOf(TdApi.TextEntity(0, emoji.length, TdApi.TextEntityTypeCustomEmoji(it))) } ?: emptyArray())
                            onCancelReply()
                            emojiOpen = false
                        },
                        whenOnline = telegram.chats[chatId]?.let { it.privateUserId != 0L && !telegram.isSavedMessages(chatId) && !telegram.isBot(it) } == true,
                        onSticker = { sticker, how ->
                            telegram.sendSticker(chatId, sticker, replyTo?.id ?: 0, how.silent, how.schedule)
                            onCancelReply()
                            emojiOpen = false
                        },
                        onGif = { animation, how, caption ->
                            telegram.sendAnimation(chatId, animation, replyTo?.id ?: 0, how.silent, how.schedule, caption)
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
                val swapScale = androidx.compose.animation.core.tween<Float>(SwapMs, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                Box(Modifier.size(SendButtonSlot), contentAlignment = Alignment.Center) {
                    AnimatedContent(
                        targetState = showMic,
                        transitionSpec = {
                            ((fadeIn(swapFade) + scaleIn(swapScale, initialScale = SwapScaleFrom)) togetherWith
                                (fadeOut(swapFade) + scaleOut(swapScale, targetScale = SwapScaleFrom))) using SizeTransform(clip = false) { _, _ -> androidx.compose.animation.core.snap() }
                        },
                        contentAlignment = Alignment.Center,
                    ) { mic ->
                        if (mic) {
                            MicButton(voice, onRelease = releaseVoice)
                        } else {
                            var sendMenu by remember { mutableStateOf(false) }
                            var scheduleFor by remember { mutableStateOf(false) }
                            if (scheduleFor && onSendWith != null) {
                                ScheduleDialog(onDismiss = { scheduleFor = false }) { date ->
                                    scheduleFor = false
                                    onSendWith(AttachmentSend(schedule = TdApi.MessageSchedulingStateSendAtDate(date, 0)))
                                }
                            }
                            if (sendMenu && onSendWith != null) {
                                val saved = telegram.isSavedMessages(chatId)
                                val person = composerChat?.let { it.privateUserId != 0L && !saved && !telegram.isBot(it) } == true
                                androidx.compose.material3.DropdownMenu(expanded = true, onDismissRequest = { sendMenu = false }, shape = RoundedCornerShape(16.dp)) {
                                    val item = @Composable { label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, action: () -> Unit ->
                                        androidx.compose.material3.DropdownMenuItem(
                                            text = { Text(label) },
                                            leadingIcon = { Icon(icon, contentDescription = null) },
                                            onClick = {
                                                sendMenu = false
                                                action()
                                            },
                                        )
                                    }
                                    if (!saved) item("Send without sound", Icons.Outlined.NotificationsOff) { onSendWith(AttachmentSend(silent = true)) }
                                    item(if (saved) "Set a reminder" else "Schedule message", Icons.Outlined.CalendarMonth) { scheduleFor = true }
                                    if (person) item("Send when online", Icons.Outlined.Alarm) { onSendWith(AttachmentSend(schedule = TdApi.MessageSchedulingStateSendWhenOnline())) }
                                }
                            }
                            FilledIconButton(
                                onClick = send,
                                enabled = canSend,
                                shape = MaterialShapes.Cookie12Sided.toShape(),
                                modifier = Modifier.pointerInput(canSend) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                                            if (canSend && editing == null && event.type == androidx.compose.ui.input.pointer.PointerEventType.Press && event.buttons.isSecondaryPressed) {
                                                event.changes.first().consume()
                                                sendMenu = true
                                            }
                                        }
                                    }
                                },
                            ) {
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
                val webApp = button.type as? TdApi.KeyboardButtonTypeWebApp
                val bot = telegram.chats[chatId]?.privateUserId ?: 0L
                if (webApp != null && bot != 0L) {
                    telegram.openKeyboardWebApp(chatId, bot, webApp.url, button.text)
                    return@ReplyKeyboardPanel
                }
                telegram.sendText(chatId, button.text, replyTo?.id ?: 0)
                onCancelReply()
                if (shown.oneTime) keyboardOpen = false
            }, modifier = Modifier.padding(bottom = 8.dp))
        }
    }
}

val LocalProtectedContent = androidx.compose.runtime.compositionLocalOf { false }

@Composable
fun MessageSelection(content: @Composable () -> Unit) {
    if (LocalProtectedContent.current) content() else SelectionContainer(content = content)
}

private val SentenceEnds = setOf('.', '!', '?')

private fun capitalizeTyped(old: String, typed: TextFieldValue): TextFieldValue {
    val new = typed.text
    if (!typed.selection.collapsed || new.length <= old.length) return typed
    val start = new.commonPrefixWith(old).length
    val tail = old.length - start
    val suffix = new.takeLast(tail).commonSuffixWith(old.takeLast(tail)).length
    if (start + suffix != old.length) return typed
    val end = new.length - suffix
    val chars = new.toCharArray()
    var changed = false
    for (at in start until end) {
        if (chars[at].isLowerCase() && startsSentence(chars, at)) {
            chars[at] = chars[at].uppercaseChar()
            changed = true
        }
    }
    return if (changed) typed.copy(text = String(chars)) else typed
}

private fun startsSentence(chars: CharArray, at: Int): Boolean {
    var before = at - 1
    if (before < 0 || chars[before] == '\n') return true
    if (chars[before] != ' ') return false
    while (before >= 0 && chars[before] == ' ') before--
    return before < 0 || chars[before] in SentenceEnds || chars[before] == '\n'
}

private fun messageContentType(message: UiMessage): Int = when {
    message.service -> 0
    message.album.isNotEmpty() -> 1
    else -> when (message.content) {
        is TdApi.MessageSticker -> 2
        is TdApi.MessagePhoto -> 3
        is TdApi.MessageVideo, is TdApi.MessageAnimation -> 4
        is TdApi.MessageVideoNote -> 5
        is TdApi.MessageVoiceNote, is TdApi.MessageAudio -> 6
        is TdApi.MessageDocument -> 7
        null, is TdApi.MessageText -> if (message.photo != null) 3 else 8
        else -> 9
    }
}

private const val HistoryPrefetchItems = 30

private const val GhostFadeMs = 220
private const val GhostAlpha = 0.6f

private const val TypingIdleMs = 5_000L
private const val ActionRepeatMs = 4_500L
