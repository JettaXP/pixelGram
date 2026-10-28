package com.pixelgram.desktop.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.outlined.PushPin
import com.pixelgram.desktop.ui.components.expressive.BasicAlertDialog
import com.pixelgram.desktop.ui.components.expressive.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import com.pixelgram.desktop.ui.components.expressive.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.components.expressive.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AppLog
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.telegram.ChatState
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.telegram.UiMessage
import com.pixelgram.desktop.telegram.contentText
import com.pixelgram.desktop.ui.theme.EmojiText
import com.pixelgram.desktop.ui.theme.schemeFromSeed
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFileSaver
import kotlinx.coroutines.launch
import javax.imageio.ImageIO

const val SelectionLimit = 100

private val SelectionBarHeight = 56.dp
private val ShotPreviewWidth = 560.dp
private val ShotPreviewMaxHeight = 400.dp
private const val PinPreviewChars = 20
private const val PinSegmentsShown = 4

fun pinPreview(message: UiMessage?): String {
    val text = message?.text?.takeIf { it.isNotBlank() } ?: message?.let { contentText(it.content) }.orEmpty()
    val line = text.replace('\n', ' ').trim()
    return if (line.length > PinPreviewChars) line.take(PinPreviewChars) + "…" else line
}

@Composable
fun SelectionBar(count: Int, onForward: () -> Unit, onDelete: () -> Unit, onShot: () -> Unit, onCancel: () -> Unit, canCopy: Boolean = true) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().height(SelectionBarHeight).padding(horizontal = 16.dp),
    ) {
        AnimatedContent(count, transitionSpec = { fadeIn() togetherWith fadeOut() }, modifier = Modifier.weight(1f)) { shown ->
            Text("$shown selected", style = MaterialTheme.typography.titleSmall)
        }
        FilledTonalButton(onClick = onForward, enabled = canCopy) { Text("Forward $count") }
        FilledTonalButton(onClick = onDelete) { Text("Delete $count") }
        FilledTonalButton(onClick = onShot, enabled = canCopy) { Text("Shot $count") }
        TextButton(onClick = onCancel) { Text("Cancel") }
    }
}

@Composable
fun PinnedBar(pins: List<UiMessage>, index: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val current = pins.getOrNull(index) ?: return
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            val segments = pins.size.coerceAtMost(PinSegmentsShown)
            val start = (index - segments / 2).coerceIn(0, pins.size - segments)
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.width(3.dp).fillMaxHeight(),
            ) {
                repeat(segments) { slot ->
                    val active = start + (segments - 1 - slot) == index
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (active) colors.primary else colors.primary.copy(alpha = 0.3f)),
                    )
                }
            }
            Icon(Icons.Outlined.PushPin, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (pins.size > 1) "Pinned message #${pins.size - index}" else "Pinned message",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primary,
                )
                AnimatedContent(current.key, transitionSpec = { fadeIn() togetherWith fadeOut() }) { _ ->
                    EmojiText(
                        (current.text.takeIf { it.isNotBlank() } ?: contentText(current.content)).replace('\n', ' '),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private enum class ShotTheme(val label: String) { Default("Default"), Light("Light"), Dark("Dark") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShotDialog(
    messages: List<UiMessage>,
    chat: ChatState,
    telegram: Telegram,
    settings: AppSettings,
    onDismiss: () -> Unit,
) {
    var theme by remember { mutableStateOf(ShotTheme.Default) }
    var themeMenu by remember { mutableStateOf(false) }
    var background by remember { mutableStateOf(true) }
    var date by remember { mutableStateOf(false) }
    var colorful by remember { mutableStateOf(true) }
    var busy by remember { mutableIntStateOf(0) }
    val layer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()
    val scheme = when (theme) {
        ShotTheme.Default -> MaterialTheme.colorScheme
        ShotTheme.Light -> remember(settings.seedColor.value, settings.colorStyle.value) { schemeFromSeed(settings.seedColor.value, false, settings.colorStyle.value) }
        ShotTheme.Dark -> remember(settings.seedColor.value, settings.colorStyle.value) { schemeFromSeed(settings.seedColor.value, true, settings.colorStyle.value) }
    }
    val render: (suspend (java.awt.image.BufferedImage) -> Unit) -> Unit = { use ->
        busy++
        scope.launch {
            runCatching { use(layer.toImageBitmap().toAwtImage()) }.onFailure { AppLog.write("Shot failed", it) }
            busy--
        }
    }
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(Modifier.padding(vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Shot", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 24.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                ) {
                Box(
                    Modifier
                        .padding(horizontal = 16.dp)
                        .width(ShotPreviewWidth)
                        .heightIn(max = ShotPreviewMaxHeight)
                        .clip(RoundedCornerShape(20.dp))
                        .verticalScroll(rememberScrollState()),
                ) {
                    MaterialTheme(colorScheme = scheme, typography = MaterialTheme.typography, shapes = MaterialTheme.shapes) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .drawWithContent {
                                    layer.record { this@drawWithContent.drawContent() }
                                    drawLayer(layer)
                                }
                                .background(if (background) scheme.surface else Color.Transparent)
                                .padding(12.dp),
                        ) {
                            ShotMessages(messages, chat, telegram, settings, date, colorful)
                        }
                    }
                }
                Text(
                    "Preferences",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                Column(Modifier.width(ShotPreviewWidth + 32.dp)) {
                    val itemColors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    Box {
                        ListItem(
                            headlineContent = { Text("Theme") },
                            trailingContent = { Text(theme.label, color = MaterialTheme.colorScheme.primary) },
                            colors = itemColors,
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { themeMenu = true },
                        )
                        DropdownMenu(expanded = themeMenu, onDismissRequest = { themeMenu = false }, offset = androidx.compose.ui.unit.DpOffset(ShotPreviewWidth - 120.dp, 0.dp)) {
                            ShotTheme.entries.forEach { option ->
                                DropdownMenuItem(text = { Text(option.label) }, onClick = {
                                    theme = option
                                    themeMenu = false
                                })
                            }
                        }
                    }
                    ShotSwitch("Show background", background, itemColors) { background = it }
                    ShotSwitch("Show date", date, itemColors) { date = it }
                    ShotSwitch("Show colorful replies", colorful, itemColors) { colorful = it }
                }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                ) {
                    TextButton(onClick = onDismiss) { Text("Close") }
                    FilledTonalButton(enabled = busy == 0, onClick = { render { Attachments.copyImage(it) } }) { Text("Copy") }
                    Button(enabled = busy == 0, onClick = {
                        render { image ->
                            val target = FileKit.openFileSaver(suggestedName = "pixelgram-shot", extension = "png") ?: return@render
                            ImageIO.write(image, "png", target.file)
                        }
                    }) { Text("Save") }
                }
            }
        }
    }
}

@Composable
private fun ShotSwitch(label: String, checked: Boolean, colors: androidx.compose.material3.ListItemColors, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange) },
        colors = colors,
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { onChange(!checked) },
    )
}

@Composable
private fun ShotMessages(
    messages: List<UiMessage>,
    chat: ChatState,
    telegram: Telegram,
    settings: AppSettings,
    date: Boolean,
    colorful: Boolean,
) {
    messages.forEachIndexed { index, message ->
        if (date && (index == 0 || !sameDay(messages[index - 1].date, message.date))) DateDivider(message.date)
        if (message.service) {
            MessageBubble(message = message, senderName = null, settings = settings)
            return@forEachIndexed
        }
        val groupChat = chat.isGroup && !chat.isChannel && !message.isOutgoing
        val firstInRun = !sameSender(messages.getOrNull(index - 1), message)
        val lastInRun = !sameSender(messages.getOrNull(index + 1), message)
        val quote: (@Composable () -> Unit)? = if (message.replyToId != 0L) {
            {
                val replied = telegram.replyPreview(chat.id, message.replyToId)
                val senderId = replied?.let { it.senderUserId.takeIf { id -> id != 0L } ?: it.senderChatId }
                ReplyQuote(
                    name = when {
                        replied == null -> "Message"
                        replied.isOutgoing -> telegram.senderName(replied).ifBlank { "You" }
                        else -> telegram.senderName(replied)
                    },
                    text = replied?.text?.takeIf { it.isNotEmpty() } ?: replied?.let { contentText(it.content) }?.ifBlank { null } ?: "…",
                    onClick = {},
                    replied = replied,
                    telegram = telegram,
                    accent = if (colorful && senderId != null) senderColor(senderId) else null,
                )
            }
        } else {
            null
        }
        Row(verticalAlignment = Alignment.Bottom) {
            if (groupChat) {
                Box(Modifier.width(SenderAvatarSlot)) {
                    if (lastInRun) SenderAvatar(message, telegram, settings) {}
                }
            }
            MessageBubble(
                telegram = telegram,
                quote = quote,
                message = message,
                modifier = Modifier.weight(1f),
                senderName = if (groupChat && firstInRun) telegram.senderName(message) else null,
                settings = settings,
                status = if (message.isOutgoing && !chat.isChannel) telegram.status(chat, message) else null,
                onReact = {},
                onButton = {},
            )
        }
    }
}
