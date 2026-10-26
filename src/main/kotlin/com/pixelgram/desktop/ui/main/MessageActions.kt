package com.pixelgram.desktop.ui.main

import com.pixelgram.desktop.ui.icons.outlined.Gif
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
import androidx.compose.ui.input.pointer.areAnyPressed
import kotlin.math.ln
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
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
    val onSaveGif: ((UiMessage) -> Unit)? = null,
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
    val step = with(LocalDensity.current) { ScrollUnit.toPx() * SwipeSlow }
    var settle by remember { mutableStateOf<Job?>(null) }
    var menuAt by remember { mutableStateOf<MenuTarget?>(null) }
    var menuShown by remember { mutableStateOf(false) }
    var lastPress by remember { mutableStateOf(0L to Offset.Zero) }
    val density = LocalDensity.current
    val slop = LocalViewConfiguration.current.touchSlop
    val reach = remember { Animatable(0f) }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val swipe = remember { ReplySwipe() }
    val show = { ratio: Float ->
        val clamped = ratio.coerceAtLeast(0f)
        val over = (clamped - 1f).coerceAtLeast(0f) * threshold
        val pixels = minOf(clamped, 1f) * threshold + with(density) { dampedOverswipe(over.toDp().value).dp.toPx() }
        scope.launch { shift.snapTo(-pixels) }
        if (!swipe.reached && clamped >= 1f) {
            swipe.reached = true
            scope.launch {
                reach.snapTo(0f)
                reach.animateTo(1f, tween(ReachBounceMs))
            }
        } else if (swipe.reached && clamped < ResetReachedOn) {
            swipe.reached = false
        }
    }
    val finish = {
        val ratio = swipe.ratio(threshold).coerceIn(0f, MaxSwipeRatio)
        if (swipe.horizontal && ratio >= 1f) actions.onReply(message)
        swipe.reset()
        scope.launch { shift.animateTo(0f, tween((minOf(1f, ratio) * SwipeEndMs).roundToInt())) }
    }
    Box(
        modifier
            .fillMaxWidth()
            .pointerInput(message.key) {
                awaitPointerEventScope {
                    var lastMove = 0L
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull() ?: continue
                        if (event.type == PointerEventType.Move || event.type == PointerEventType.Enter) lastMove = change.uptimeMillis
                        if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                            change.consume()
                            menuShown = true
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
                        } else if (event.type == PointerEventType.Move && swipe.horizontal) {
                            if (abs(change.position.y - swipe.startY) > slop) {
                                settle?.cancel()
                                finish()
                            }
                        } else if (event.type == PointerEventType.Scroll) {
                            val delta = change.scrollDelta
                            if (event.buttons.areAnyPressed) continue
                            if (!swipe.active) {
                                swipe.active = true
                                swipe.startY = change.position.y
                            }
                            swipe.add(delta.x * step * (if (rtl) -1 else 1), delta.y * step)
                            if (swipe.horizontal) {
                                change.consume()
                                show(swipe.ratio(threshold))
                            }
                            settle?.cancel()
                            settle = scope.launch {
                                delay(SettleMs)
                                finish()
                            }
                        }
                    }
                }
            },
    ) {
        val progress = (-shift.value / threshold).coerceIn(0f, 1f)
        Box(
            Modifier
                .align(if (rtl) Alignment.CenterStart else Alignment.CenterEnd)
                .padding(horizontal = 8.dp)
                .size(40.dp)
                .graphicsLayer {
                    val bounce = reach.value.let { value -> if (value > BouncePart) BouncePart * 2 - value else value }.coerceIn(0f, 1f)
                    alpha = (progress * 2).coerceAtMost(1f)
                    scaleX = 0.6f + progress * 0.4f + bounce * BounceScale
                    scaleY = 0.6f + progress * 0.4f + bounce * BounceScale
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
        Box(Modifier.offset { IntOffset((if (rtl) -shift.value else shift.value).roundToInt(), 0) }) {
            content()
        }
        menuAt?.let { target ->
            Box(Modifier.offset(target.at.x, target.at.y)) {
                MessageMenu(message, actions, target.link, target.button, menuShown, onDismiss = { menuShown = false })
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
    expanded: Boolean,
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
        (message.content as? TdApi.MessageAnimatedEmoji)?.emoji?.takeIf { message.text.isEmpty() }?.let { emoji ->
            add(MenuEntry(Icons.Outlined.ContentCopy, "Copy emoji", onClick = act { copy(emoji) }))
        }
        actions.filePath(message)?.let { path ->
            add(MenuEntry(Icons.Outlined.FolderOpen, "Show in folder", onClick = act { showInFolder(path) }))
            add(MenuEntry(Icons.Outlined.Link, "Copy file path", onClick = act { copy(path) }))
        }
        if (message.photo != null) add(MenuEntry(Icons.Outlined.Image, "Copy photo", onClick = act { actions.onCopyPhoto(message) }))
        val saveGif = actions.onSaveGif
        if (saveGif != null && message.content is TdApi.MessageAnimation) {
            add(MenuEntry(Icons.Outlined.Gif, "Save to GIFs", onClick = act { saveGif(message) }))
        }
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
    GroupedMenu(expanded = expanded, onDismiss = onDismiss) {
        val reactions: (@Composable ColumnScope.() -> Unit)? = if (!message.service && actions.canReact) {
            {
                var available by remember(message.key) { mutableStateOf(emptyList<String>()) }
                androidx.compose.runtime.LaunchedEffect(message.key) { actions.loadReactions(message) { available = it } }
                val scope = androidx.compose.runtime.rememberCoroutineScope()
                ReactionPicker(available) { emoji ->
                    scope.launch {
                        onDismiss()
                        actions.onReact(message, emoji)
                    }
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
private class ReplySwipe {
    var active = false
    var startY = 0f
    var reached = false
    private var orientation = 0
    private var x = 0f
    private var y = 0f
    val horizontal: Boolean get() = orientation == Horizontal

    fun add(dx: Float, dy: Float) {
        if (orientation == Vertical) return
        x += dx
        y += dy
        if (orientation == 0) {
            val diff = abs(x) - abs(y)
            if (diff > OrientationThreshold) orientation = Horizontal
            else if (diff < -OrientationThreshold) orientation = Vertical
        }
    }

    fun ratio(threshold: Float) = x / threshold

    fun reset() {
        active = false
        reached = false
        orientation = 0
        x = 0f
        y = 0f
    }
}

private fun dampedOverswipe(dp: Float): Float = if (dp <= 0f) 0f else OverswipeLogA * ln(1f + dp / OverswipeLogB)

private const val Horizontal = 1
private const val Vertical = 2
private val SwipeThreshold = 50.dp
private val ScrollUnit = 64.dp
private const val SwipeSlow = 0.2f
private const val SettleMs = 180L
private const val OrientationThreshold = 1f
private const val MaxSwipeRatio = 1.5f
private const val ResetReachedOn = 0.95f
private const val SwipeEndMs = 200
private const val ReachBounceMs = 500
private const val BouncePart = 0.25f
private const val BounceScale = 0.6f
private const val OverswipeLogA = 16f
private const val OverswipeLogB = 10f
