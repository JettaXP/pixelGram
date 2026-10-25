package com.pixelgram.desktop.ui.main

import com.pixelgram.desktop.ui.icons.outlined.FolderOpen
import androidx.compose.foundation.layout.ColumnScope
import com.pixelgram.desktop.ui.components.GroupedMenu
import com.pixelgram.desktop.ui.components.MenuEntry
import com.pixelgram.desktop.ui.components.MenuGroups
import com.pixelgram.desktop.ui.icons.outlined.PushPin
import com.pixelgram.desktop.ui.icons.outlined.Link
import com.pixelgram.desktop.ui.icons.outlined.DataObject
import com.pixelgram.desktop.ui.icons.outlined.CheckCircle
import androidx.compose.ui.graphics.vector.ImageVector
import com.pixelgram.desktop.ui.components.HoverTarget
import it.tdlight.jni.TdApi
import com.pixelgram.desktop.telegram.Telegram
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.geometry.Offset
import com.pixelgram.desktop.ui.icons.automirrored.outlined.Forward
import com.pixelgram.desktop.ui.icons.outlined.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.automirrored.outlined.Reply
import com.pixelgram.desktop.ui.icons.outlined.ContentCopy
import com.pixelgram.desktop.ui.icons.outlined.Delete
import com.pixelgram.desktop.ui.icons.outlined.Edit
import com.pixelgram.desktop.ui.components.expressive.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.components.expressive.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.telegram.UiMessage
import com.pixelgram.desktop.ui.theme.EmojiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import kotlin.math.abs
import kotlin.math.roundToInt

private const val DoubleClickMs = 350L

class MessageActions(
    val onReply: (UiMessage) -> Unit,
    val onEdit: (UiMessage) -> Unit,
    val onDelete: (UiMessage) -> Unit,
    val onQuoteClick: (Long) -> Unit,
    val onCopyPhoto: (UiMessage) -> Unit,
    val onForward: (UiMessage) -> Unit,
    val onReact: (UiMessage, String) -> Unit,
    val loadReactions: (UiMessage, (List<String>) -> Unit) -> Unit,
    val onDoubleClick: (UiMessage) -> Unit = {},
    val canReact: Boolean = true,
    val onPin: ((UiMessage) -> Unit)? = null,
    val onSelect: ((UiMessage) -> Unit)? = null,
    val filePath: (UiMessage) -> String? = { null },
)

private class MenuTarget(val at: DpOffset, val link: String?, val button: TdApi.InlineKeyboardButton?)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MessageGestures(
    message: UiMessage,
    actions: MessageActions,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val shift = remember { Animatable(0f) }
    val threshold = with(LocalDensity.current) { SwipeThreshold.toPx() }
    var fired by remember { mutableStateOf(false) }
    var settle by remember { mutableStateOf<Job?>(null) }
    var menuAt by remember { mutableStateOf<MenuTarget?>(null) }
    var lastPress by remember { mutableStateOf(0L to Offset.Zero) }
    val density = LocalDensity.current
    val slop = LocalViewConfiguration.current.touchSlop
    val move = { delta: Float ->
        val next = (shift.value + delta).coerceIn(-threshold * 1.4f, 0f)
        scope.launch { shift.snapTo(next) }
        if (!fired && -next >= threshold) {
            fired = true
            actions.onReply(message)
        }
    }
    val release = {
        fired = false
        scope.launch { shift.animateTo(0f) }
    }
    Box(
        modifier
            .fillMaxWidth()
            .pointerInput(message.key) {
                detectHorizontalDragGestures(
                    onDragEnd = { release() },
                    onDragCancel = { release() },
                ) { change, drag ->
                    change.consume()
                    move(drag)
                }
            }
            .pointerInput(message.key) {
                awaitPointerEventScope {
                    var lastMove = 0L
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull() ?: continue
                        if (event.type == PointerEventType.Move || event.type == PointerEventType.Enter) lastMove = change.uptimeMillis
                        if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                            change.consume()
                            menuAt = MenuTarget(
                                with(density) { DpOffset(change.position.x.toDp(), change.position.y.toDp()) },
                                HoverTarget.link,
                                HoverTarget.button?.takeIf { HoverTarget.buttonSeenAt >= lastMove },
                            )
                        } else if (event.type == PointerEventType.Press && event.buttons.isPrimaryPressed) {
                            val (time, at) = lastPress
                            val now = change.uptimeMillis
                            if (now - time < DoubleClickMs && (change.position - at).getDistance() < slop) {
                                lastPress = 0L to Offset.Zero
                                actions.onDoubleClick(message)
                            } else {
                                lastPress = now to change.position
                            }
                        } else if (event.type == PointerEventType.Scroll) {
                            val delta = change.scrollDelta
                            if (abs(delta.x) > abs(delta.y) * HorizontalDominance && abs(delta.x) >= MinHorizontalScroll) {
                                change.consume()
                                move(-delta.x * TouchpadFactor)
                                settle?.cancel()
                                settle = scope.launch {
                                    delay(SettleMs)
                                    release()
                                }
                            }
                        }
                    }
                }
            },
    ) {
        val progress = (-shift.value / threshold).coerceIn(0f, 1f)
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
                .size(40.dp)
                .graphicsLayer {
                    alpha = (progress * 2).coerceAtMost(1f)
                    scaleX = 0.6f + progress * 0.4f
                    scaleY = 0.6f + progress * 0.4f
                },
            contentAlignment = Alignment.Center,
        ) {
            CircularWavyProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
            )
            Icon(
                Icons.AutoMirrored.Outlined.Reply,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
        Box(Modifier.offset { IntOffset(shift.value.roundToInt(), 0) }) {
            content()
        }
        menuAt?.let { target ->
            Box(Modifier.offset(target.at.x, target.at.y)) {
                MessageMenu(message, actions, target.link, target.button, onDismiss = { menuAt = null })
            }
        }
    }
}

@Composable
private fun MessageMenu(
    message: UiMessage,
    actions: MessageActions,
    link: String?,
    button: TdApi.InlineKeyboardButton?,
    onDismiss: () -> Unit,
) {
    val act = { action: () -> Unit -> { onDismiss(); action() } }
    val copy = { value: String -> Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(value), null) }
    val buttonUrl = (button?.type as? TdApi.InlineKeyboardButtonTypeUrl)?.url
        ?: (button?.type as? TdApi.InlineKeyboardButtonTypeLoginUrl)?.url
    val copyLink = link ?: buttonUrl
    val reply = buildList {
        add(MenuEntry(Icons.AutoMirrored.Outlined.Reply, "Reply", onClick = act { actions.onReply(message) }))
        add(MenuEntry(Icons.AutoMirrored.Outlined.Forward, "Forward", onClick = act { actions.onForward(message) }))
        if (message.isOutgoing && message.editable) add(MenuEntry(Icons.Outlined.Edit, "Edit", onClick = act { actions.onEdit(message) }))
        actions.onPin?.takeIf { !message.service }?.let { pin ->
            add(MenuEntry(Icons.Outlined.PushPin, if (message.pinned) "Unpin" else "Pin", onClick = act { pin(message) }))
        }
    }
    val copies = buildList {
        if (message.text.isNotEmpty()) add(MenuEntry(Icons.Outlined.ContentCopy, "Copy text", onClick = act { copy(message.text) }))
        actions.filePath(message)?.let { path ->
            add(MenuEntry(Icons.Outlined.FolderOpen, "Show in folder", onClick = act { showInFolder(path) }))
            add(MenuEntry(Icons.Outlined.Link, "Copy file path", onClick = act { copy(path) }))
        }
        if (message.photo != null) add(MenuEntry(Icons.Outlined.Image, "Copy photo", onClick = act { actions.onCopyPhoto(message) }))
        if (copyLink != null) add(MenuEntry(Icons.Outlined.Link, "Copy Link", onClick = act { copy(copyLink) }))
        (button?.type as? TdApi.InlineKeyboardButtonTypeCallback)?.let { callback ->
            add(MenuEntry(Icons.Outlined.DataObject, "Copy Callback Data", onClick = act { copy(String(callback.data, Charsets.UTF_8)) }))
        }
    }
    val manage = buildList {
        actions.onSelect?.takeIf { !message.service }?.let { select ->
            add(MenuEntry(Icons.Outlined.CheckCircle, "Select", onClick = act { select(message) }))
        }
        add(MenuEntry(Icons.Outlined.Delete, "Delete", color = MaterialTheme.colorScheme.error, onClick = act { actions.onDelete(message) }))
    }
    GroupedMenu(expanded = true, onDismiss = onDismiss) {
        val reactions: (@Composable ColumnScope.() -> Unit)? = if (!message.service && actions.canReact) {
            {
                var available by remember(message.key) { mutableStateOf(emptyList<String>()) }
                androidx.compose.runtime.LaunchedEffect(message.key) { actions.loadReactions(message) { available = it } }
                ReactionPicker(available) { emoji ->
                    onDismiss()
                    actions.onReact(message, emoji)
                }
            }
        } else null
        MenuGroups(listOf(reply, copies, manage), leading = reactions)
    }
}

@Composable
fun DeleteMessageDialog(
    canRevoke: Boolean,
    revokeLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (revoke: Boolean) -> Unit,
) {
    var revoke by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
        title = { Text("Delete message?") },
        text = {
            if (canRevoke) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { revoke = !revoke },
                ) {
                    Checkbox(checked = revoke, onCheckedChange = { revoke = it })
                    EmojiText(revokeLabel, modifier = Modifier.padding(end = 12.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(canRevoke && revoke) }) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun ReplyQuote(
    name: String,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    replied: UiMessage? = null,
    telegram: Telegram? = null,
    accent: androidx.compose.ui.graphics.Color? = null,
) {
    val colors = MaterialTheme.colorScheme
    val tint = accent ?: colors.primary
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = tint.copy(alpha = 0.12f),
        modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min),
    ) {
        Row {
            Box(Modifier.width(3.dp).fillMaxHeight().background(tint))
            if (telegram != null && replied?.let(::replyThumb) != null) {
                Box(Modifier.padding(start = 6.dp).align(Alignment.CenterVertically)) {
                    ReplyThumb(replied, telegram, ReplyThumbSize)
                }
            }
            Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                EmojiText(
                    name,
                    style = MaterialTheme.typography.labelLarge,
                    color = tint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                EmojiText(
                    text.replace('\n', ' '),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private val ReplyThumbSize = 32.dp
private val SwipeThreshold = 64.dp
private const val TouchpadFactor = 1.6f
private const val SettleMs = 140L
private const val HorizontalDominance = 2f
private const val MinHorizontalScroll = 0.5f
