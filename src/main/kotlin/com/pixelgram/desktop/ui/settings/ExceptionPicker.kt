package com.pixelgram.desktop.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.telegram.ChatListKey
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.components.ChatAvatar
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.outlined.Close
import com.pixelgram.desktop.ui.theme.EmojiText

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ExceptionPicker(
    title: String,
    telegram: Telegram,
    settings: AppSettings,
    initial: List<Long>,
    onDismiss: () -> Unit,
    onDone: (List<Long>) -> Unit,
) {
    var picked by remember { mutableStateOf(initial) }
    var query by remember { mutableStateOf("") }
    var armed by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val candidates = telegram.chatList(ChatListKey.Main)
        .filter { chat -> !telegram.isSavedMessages(chat.id) && !chat.isChannel && (chat.privateUserId == 0L || !telegram.isBot(chat)) }
        .filter { chat -> query.isBlank() || telegram.chatTitle(chat).contains(query.trim(), ignoreCase = true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        itemVerticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(8.dp).animateContentSize(MaterialTheme.motionScheme.defaultSpatialSpec()),
                    ) {
                        picked.forEachIndexed { index, id ->
                            val chat = telegram.chats[id] ?: return@forEachIndexed
                            androidx.compose.runtime.key(id) {
                            val appear = remember { androidx.compose.animation.core.MutableTransitionState(id in initial).apply { targetState = true } }
                            androidx.compose.animation.AnimatedVisibility(
                                visibleState = appear,
                                enter = androidx.compose.animation.scaleIn(MaterialTheme.motionScheme.fastSpatialSpec(), initialScale = 0.6f) +
                                    androidx.compose.animation.fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()),
                            ) {
                            Pill(
                                name = telegram.chatTitle(chat),
                                selected = armed && index == picked.lastIndex,
                                avatar = { ChatAvatar(chat, telegram, settings, size = PillAvatar) },
                                onRemove = {
                                    picked = picked - id
                                    armed = false
                                },
                            )
                            }
                            }
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = {
                                query = it
                                armed = false
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .widthIn(min = 120.dp)
                                .padding(horizontal = 6.dp, vertical = 6.dp)
                                .focusRequester(focus)
                                .onPreviewKeyEvent { event ->
                                    if (event.type != KeyEventType.KeyDown || event.key != Key.Backspace || query.isNotEmpty() || picked.isEmpty()) return@onPreviewKeyEvent false
                                    if (armed) {
                                        picked = picked.dropLast(1)
                                        armed = false
                                    } else {
                                        armed = true
                                    }
                                    true
                                },
                            decorationBox = { field ->
                                Box {
                                    if (query.isEmpty()) Text("Search", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    field()
                                }
                            },
                        )
                    }
                }
                LazyColumn(Modifier.heightIn(max = ListHeight)) {
                    items(candidates, key = { it.id }) { chat ->
                        val checked = chat.id in picked
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    picked = if (checked) picked - chat.id else picked + chat.id
                                    armed = false
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                        ) {
                            ChatAvatar(chat, telegram, settings, size = 40.dp)
                            EmojiText(telegram.chatTitle(chat), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Checkbox(checked = checked, onCheckedChange = null)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onDone(picked) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun Pill(name: String, selected: Boolean, avatar: @Composable () -> Unit, onRemove: () -> Unit) {
    val hover = remember { MutableInteractionSource() }
    val hovered by hover.collectIsHoveredAsState()
    val colors = MaterialTheme.colorScheme
    val container by androidx.compose.animation.animateColorAsState(if (selected) colors.primary else colors.secondaryContainer, animationSpec = com.pixelgram.desktop.ui.components.Motion.effects())
    val content by androidx.compose.animation.animateColorAsState(if (selected) colors.onPrimary else colors.onSecondaryContainer, animationSpec = com.pixelgram.desktop.ui.components.Motion.effects())
    Surface(
        shape = CircleShape,
        color = container,
        contentColor = content,
        modifier = Modifier.hoverable(hover),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp)) {
            Box(
                Modifier.size(PillAvatar).clip(CircleShape).clickable(onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                if (hovered || selected) {
                    Box(Modifier.size(PillAvatar).background(if (selected) colors.onPrimary.copy(alpha = 0.2f) else colors.primary), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Close, contentDescription = "Remove", tint = if (selected) colors.onPrimary else colors.onPrimary, modifier = Modifier.size(18.dp))
                    }
                } else {
                    avatar()
                }
            }
            EmojiText(name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 8.dp).widthIn(max = 160.dp))
        }
    }
}

private val PillAvatar = 32.dp
private val ListHeight = 320.dp
