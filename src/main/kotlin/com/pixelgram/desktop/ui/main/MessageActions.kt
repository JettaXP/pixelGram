package com.pixelgram.desktop.ui.main

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

class MessageActions(
    val onReply: (UiMessage) -> Unit,
    val onEdit: (UiMessage) -> Unit,
    val onDelete: (UiMessage) -> Unit,
    val onQuoteClick: (Long) -> Unit,
    val onCopyPhoto: (UiMessage) -> Unit,
    val onForward: (UiMessage) -> Unit,
)

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
    var menuAt by remember { mutableStateOf<DpOffset?>(null) }
    val density = LocalDensity.current
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
                            menuAt = with(density) { DpOffset(change.position.x.toDp(), change.position.y.toDp()) }
                        } else if (event.type == PointerEventType.Scroll) {
                            val delta = change.scrollDelta
                            if (abs(delta.x) > abs(delta.y) && delta.x != 0f) {
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
        menuAt?.let { at ->
            Box(Modifier.offset(at.x, at.y)) {
                MessageMenu(message, actions, onDismiss = { menuAt = null })
            }
        }
    }
}

@Composable
private fun MessageMenu(message: UiMessage, actions: MessageActions, onDismiss: () -> Unit) {
    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        DropdownMenuItem(
            text = { Text("Reply") },
            leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Reply, contentDescription = null) },
            onClick = {
                onDismiss()
                actions.onReply(message)
            },
        )
        DropdownMenuItem(
            text = { Text("Forward") },
            leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Forward, contentDescription = null) },
            onClick = {
                onDismiss()
                actions.onForward(message)
            },
        )
        if (message.isOutgoing && message.editable) {
            DropdownMenuItem(
                text = { Text("Edit") },
                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                onClick = {
                    onDismiss()
                    actions.onEdit(message)
                },
            )
        }
        if (message.photo != null) {
            DropdownMenuItem(
                text = { Text("Copy photo") },
                leadingIcon = { Icon(Icons.Outlined.Image, contentDescription = null) },
                onClick = {
                    onDismiss()
                    actions.onCopyPhoto(message)
                },
            )
        }
        if (message.text.isNotEmpty()) {
            DropdownMenuItem(
                text = { Text("Copy text") },
                leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                onClick = {
                    onDismiss()
                    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(message.text), null)
                },
            )
        }
        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        DropdownMenuItem(
            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            onClick = {
                onDismiss()
                actions.onDelete(message)
            },
        )
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
fun ReplyQuote(name: String, text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = colors.primary.copy(alpha = 0.12f),
        modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min),
    ) {
        Row {
            Box(Modifier.width(3.dp).fillMaxHeight().background(colors.primary))
            Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                EmojiText(
                    name,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primary,
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

private val SwipeThreshold = 64.dp
private const val TouchpadFactor = 1.6f
private const val SettleMs = 140L
