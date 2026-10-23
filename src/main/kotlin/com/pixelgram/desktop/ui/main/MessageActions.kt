package com.pixelgram.desktop.ui.main

import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.ui.graphics.vector.ImageVector
import com.pixelgram.desktop.ui.components.HoverTarget
import it.tdlight.jni.TdApi
import com.pixelgram.desktop.telegram.Telegram
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.automirrored.outlined.Forward
import androidx.compose.material.icons.outlined.Image
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull() ?: continue
                        if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                            change.consume()
                            menuAt = MenuTarget(
                                with(density) { DpOffset(change.position.x.toDp(), change.position.y.toDp()) },
                                HoverTarget.link,
                                HoverTarget.button,
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
    val item = @Composable { label: String, icon: ImageVector, action: () -> Unit ->
        DropdownMenuItem(
            text = { Text(label) },
            leadingIcon = { Icon(icon, contentDescription = null) },
            onClick = {
                onDismiss()
                action()
            },
        )
    }
    val copy = { value: String -> Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(value), null) }
    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        if (!message.service && actions.canReact) {
            var available by remember(message.key) { mutableStateOf(emptyList<String>()) }
            androidx.compose.runtime.LaunchedEffect(message.key) { actions.loadReactions(message) { available = it } }
            ReactionPicker(available) { emoji ->
                onDismiss()
                actions.onReact(message, emoji)
            }
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
        }
        item("Reply", Icons.AutoMirrored.Outlined.Reply) { actions.onReply(message) }
        actions.onPin?.takeIf { !message.service }?.let { pin ->
            item(if (message.pinned) "Unpin" else "Pin", Icons.Outlined.PushPin) { pin(message) }
        }
        if (message.isOutgoing && message.editable) {
            item("Edit", Icons.Outlined.Edit) { actions.onEdit(message) }
        }
        val buttonUrl = (button?.type as? TdApi.InlineKeyboardButtonTypeUrl)?.url
            ?: (button?.type as? TdApi.InlineKeyboardButtonTypeLoginUrl)?.url
        val copyLink = link ?: buttonUrl
        if (copyLink != null) {
            item("Copy Link", Icons.Outlined.Link) { copy(copyLink) }
        }
        (button?.type as? TdApi.InlineKeyboardButtonTypeCallback)?.let { callback ->
            item("Copy Callback Data", Icons.Outlined.DataObject) { copy(String(callback.data, Charsets.UTF_8)) }
        }
        if (message.photo != null) {
            item("Copy photo", Icons.Outlined.Image) { actions.onCopyPhoto(message) }
        }
        if (message.text.isNotEmpty()) {
            item("Copy text", Icons.Outlined.ContentCopy) { copy(message.text) }
        }
        item("Forward", Icons.AutoMirrored.Outlined.Forward) { actions.onForward(message) }
        DropdownMenuItem(
            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            onClick = {
                onDismiss()
                actions.onDelete(message)
            },
        )
        actions.onSelect?.takeIf { !message.service }?.let { select ->
            item("Select", Icons.Outlined.CheckCircle) { select(message) }
        }
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
