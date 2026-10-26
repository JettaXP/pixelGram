package com.pixelgram.desktop.ui.main

import androidx.compose.runtime.key
import com.pixelgram.desktop.ui.components.Avatar
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.components.HoverTarget
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import com.pixelgram.desktop.telegram.ReactionEffects
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.filled.KeyboardArrowDown
import com.pixelgram.desktop.ui.icons.filled.KeyboardArrowUp
import com.pixelgram.desktop.ui.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Icon
import com.pixelgram.desktop.ui.components.expressive.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.pixelgram.desktop.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelgram.desktop.ui.theme.EmojiText
import it.tdlight.jni.TdApi

val DefaultReactions = listOf("❤", "👍", "🙏", "❤‍🔥", "😭", "🤔", "👎")

@Composable
fun ReactionPicker(available: List<String>, onPick: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val all = available.ifEmpty { DefaultReactions }
    Column(Modifier.width(PickerWidth).animateContentSize(MaterialTheme.motionScheme.defaultSpatialSpec())) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp)) {
            all.take(QuickCount).forEach { emoji -> ReactionCell(emoji, onPick) }
            IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(CellSize)) {
                Icon(if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, contentDescription = "More reactions")
            }
        }
        if (expanded) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(GridColumns),
                modifier = Modifier.height(GridHeight).padding(horizontal = 6.dp),
            ) {
                items(all.drop(QuickCount)) { emoji -> ReactionCell(emoji, onPick) }
            }
        }
    }
}

@Composable
private fun ReactionCell(emoji: String, onPick: (String) -> Unit) {
    Box(
        Modifier.size(CellSize).clip(CircleShape).clickable { onPick(emoji) },
        contentAlignment = Alignment.Center,
    ) {
        EmojiText(emoji, fontSize = 22.sp)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReactionChips(
    owner: Pair<Long, Long>,
    reactions: List<TdApi.MessageReaction>,
    outgoing: Boolean,
    onToggle: (String) -> Unit,
    telegram: Telegram? = null,
    settings: AppSettings? = null,
    avatars: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val known = remember(owner) { ReactionMemory.known(owner) }
    LaunchedEffect(owner, reactions) { ReactionMemory.store(owner, reactions) }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(top = 4.dp),
    ) {
        reactions.forEach { reaction ->
            val emoji = (reaction.type as? TdApi.ReactionTypeEmoji)?.emoji ?: return@forEach
            key(emoji) {
            val chosen = reaction.isChosen
            val clean = emoji.replace("\uFE0F", "")
            val fresh = remember { known != null && clean !in known }
            val pop = remember { Animatable(if (fresh) 0f else 1f) }
            LaunchedEffect(Unit) {
                if (fresh) pop.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.5f, stiffness = 400f))
            }
            var shown by remember { mutableStateOf(reaction.totalCount to chosen) }
            LaunchedEffect(reaction.totalCount, chosen) {
                val now = reaction.totalCount to chosen
                if (now == shown) return@LaunchedEffect
                shown = now
                try {
                    pop.snapTo(0.85f)
                    pop.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.45f, stiffness = 500f))
                } finally {
                    withContext(NonCancellable) { pop.snapTo(1f) }
                }
            }
            Surface(
                onClick = { onToggle(emoji) },
                shape = CircleShape,
                color = when {
                    chosen -> scheme.primary
                    outgoing -> scheme.onPrimaryContainer.copy(alpha = 0.12f)
                    else -> scheme.secondaryContainer
                },
                contentColor = if (chosen) scheme.onPrimary else if (outgoing) scheme.onPrimaryContainer else scheme.onSecondaryContainer,
                modifier = Modifier.graphicsLayer {
                    scaleX = pop.value
                    scaleY = pop.value
                },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(start = 8.dp, end = 10.dp, top = 3.dp, bottom = 3.dp).animateContentSize(),
                ) {
                    val shot = if (telegram != null) ReactionEffects.pending[owner]?.takeIf { it.emoji == clean } else null
                    var covered by remember { mutableStateOf(false) }
                    Box(contentAlignment = Alignment.Center) {
                        EmojiText(emoji, fontSize = 15.sp, modifier = Modifier.graphicsLayer { alpha = if (covered) 0f else 1f })
                        if (shot != null && telegram != null) key(shot) { ReactionBurst(owner, shot, telegram) { covered = it } }
                    }
                    val senders = reaction.recentSenderIds.orEmpty()
                    if (avatars && telegram != null && settings != null && reaction.totalCount <= MaxReactionAvatars && senders.size == reaction.totalCount) {
                        Row(horizontalArrangement = Arrangement.spacedBy(-ReactionAvatarOverlap)) {
                            senders.forEach { sender ->
                                val photo = when (sender) {
                                    is TdApi.MessageSenderUser -> telegram.users[sender.userId]?.profilePhoto?.small
                                    is TdApi.MessageSenderChat -> telegram.chats[sender.chatId]?.photo
                                    else -> null
                                }
                                Avatar(telegram.senderTitle(sender), photo, telegram, settings, ReactionAvatarSize)
                            }
                        }
                    } else {
                        Text(reaction.totalCount.toString(), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun ReactionBurst(owner: Pair<Long, Long>, shot: ReactionEffects.Shot, telegram: Telegram, onCover: (Boolean) -> Unit) {
    var reaction by remember(shot) { mutableStateOf<TdApi.EmojiReaction?>(null) }
    LaunchedEffect(shot) {
        telegram.emojiReaction(shot.emoji) { result ->
            if (result == null) ReactionEffects.done(owner, shot) else reaction = result
        }
        delay(BurstWaitMs)
        if (reaction == null) ReactionEffects.done(owner, shot)
    }
    val current = reaction ?: return
    val center = current.activateAnimation ?: current.centerAnimation
    val effect = current.effectAnimation ?: current.aroundAnimation
    val parts = listOfNotNull(center, effect)
    val ready = parts.all { telegram.localPath(it.sticker) != null }
    LaunchedEffect(current) {
        parts.forEach { telegram.download(it.sticker) }
        delay(BurstWaitMs)
        if (parts.any { telegram.localPath(it.sticker) == null }) ReactionEffects.done(owner, shot)
    }
    if (parts.isEmpty()) {
        LaunchedEffect(Unit) { ReactionEffects.done(owner, shot) }
        return
    }
    if (!ready) return
    val effectSize = if (shot.big) BigEffectSize else EffectSize
    val centerSize = effectSize * CenterShare
    var centerDone by remember(shot) { mutableStateOf(center == null) }
    var effectDone by remember(shot) { mutableStateOf(effect == null) }
    var shrunk by remember(shot) { mutableStateOf(center == null) }
    val shrink = remember(shot) { Animatable(1f) }
    DisposableEffect(shot) {
        onCover(center != null)
        onDispose { onCover(false) }
    }
    LaunchedEffect(centerDone) {
        if (!centerDone || shrunk) return@LaunchedEffect
        shrink.animateTo(ChipEmojiSize / centerSize, androidx.compose.animation.core.tween(ShrinkMs))
        shrunk = true
        onCover(false)
    }
    LaunchedEffect(shrunk, effectDone) {
        if (shrunk && effectDone) ReactionEffects.done(owner, shot)
    }
    androidx.compose.ui.window.Popup(alignment = Alignment.Center) {
        Box(Modifier.size(effectSize), contentAlignment = Alignment.Center) {
            if (effect != null && !effectDone) {
                StickerView(effect, telegram, effectSize, runs = 1, onEnd = { effectDone = true })
            }
            if (center != null && !shrunk) {
                StickerView(
                    center,
                    telegram,
                    centerSize,
                    Modifier.graphicsLayer {
                        scaleX = shrink.value
                        scaleY = shrink.value
                    },
                    runs = if (centerDone) 0 else 1,
                    onEnd = { centerDone = true },
                )
            }
        }
    }
}

private val EffectSize = 120.dp
private val BigEffectSize = 220.dp
private val ChipEmojiSize = 20.dp
private const val CenterShare = 0.5f
private const val ShrinkMs = 260
private const val BurstWaitMs = 4000L

private const val MaxReactionAvatars = 3
private val ReactionAvatarSize = 20.dp
private val ReactionAvatarOverlap = 6.dp

@Composable
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
fun InlineKeyboard(rows: List<List<TdApi.InlineKeyboardButton>>, onPress: (TdApi.InlineKeyboardButton) -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(ButtonGap), modifier = modifier) {
        rows.forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(ButtonGap), modifier = Modifier.fillMaxWidth()) {
                row.forEachIndexed { index, button ->
                    val last = rowIndex == rows.lastIndex
                    val big = 16.dp
                    val small = 6.dp
                    Surface(
                        onClick = { onPress(button) },
                        shape = RoundedCornerShape(
                            topStart = small,
                            topEnd = small,
                            bottomStart = if (last && index == 0) big else small,
                            bottomEnd = if (last && index == row.lastIndex) big else small,
                        ),
                        color = when (button.style) {
                            is TdApi.ButtonStylePrimary -> scheme.primary
                            is TdApi.ButtonStyleSuccess -> scheme.tertiaryContainer
                            is TdApi.ButtonStyleDanger -> scheme.errorContainer
                            else -> scheme.secondaryContainer
                        },
                        contentColor = when (button.style) {
                            is TdApi.ButtonStylePrimary -> scheme.onPrimary
                            is TdApi.ButtonStyleSuccess -> scheme.onTertiaryContainer
                            is TdApi.ButtonStyleDanger -> scheme.onErrorContainer
                            else -> scheme.onSecondaryContainer
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(ButtonHeight)
                            .onPointerEvent(PointerEventType.Enter) {
                                HoverTarget.button = button
                                HoverTarget.buttonSeenAt = it.changes.firstOrNull()?.uptimeMillis ?: 0L
                            }
                            .onPointerEvent(PointerEventType.Move) {
                                HoverTarget.button = button
                                HoverTarget.buttonSeenAt = it.changes.firstOrNull()?.uptimeMillis ?: 0L
                            }
                            .onPointerEvent(PointerEventType.Exit) { if (HoverTarget.button === button) HoverTarget.button = null },
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (button.iconCustomEmojiId != 0L) {
                                    CustomEmojiView(button.iconCustomEmojiId, "", Modifier.size(ButtonIcon))
                                }
                                if (button.text.isNotBlank()) {
                                    EmojiText(
                                        button.text,
                                        style = MaterialTheme.typography.labelLarge.copy(textAlign = TextAlign.Center),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            if (button.type is TdApi.InlineKeyboardButtonTypeUrl || button.type is TdApi.InlineKeyboardButtonTypeLoginUrl) {
                                Icon(
                                    Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp).align(Alignment.TopEnd).padding(top = 2.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val QuickCount = 7
private const val GridColumns = 8
private val CellSize = 40.dp
private val PickerWidth = 340.dp
private val GridHeight = 200.dp
private val ButtonGap = 4.dp
private val ButtonHeight = 42.dp
private val ButtonIcon = 22.dp

object TopNotice {
    var text by mutableStateOf<String?>(null)
}

@Composable
fun BotAnswer(answer: Pair<String, Boolean>?, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val (text, alert) = answer ?: (null to false)
    if (text != null && alert) {
        com.pixelgram.desktop.ui.components.expressive.AlertDialog(
            onDismissRequest = onDismiss,
            text = { EmojiText(text) },
            confirmButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("OK") } },
        )
        return
    }
    LaunchedEffect(answer) {
        if (text != null) {
            kotlinx.coroutines.delay(AnswerMs)
            onDismiss()
        }
    }
    var shown by remember { mutableStateOf("") }
    if (text != null) shown = text
    androidx.compose.animation.AnimatedVisibility(
        visible = text != null,
        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically { -it },
        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically { -it },
        modifier = modifier,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            shadowElevation = 4.dp,
        ) {
            EmojiText(shown, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp))
        }
    }
}

private const val AnswerMs = 2500L

object ReactionMemory {
    private val seen = object : LinkedHashMap<Pair<Long, Long>, Set<String>>(256, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Pair<Long, Long>, Set<String>>) = size > MaxMessages
    }

    fun note(message: com.pixelgram.desktop.telegram.UiMessage) {
        val key = message.chatId to message.id
        if (seen[key] == null) seen[key] = emojis(message.reactions)
    }

    fun known(owner: Pair<Long, Long>): Set<String>? = seen[owner]

    fun forget(owner: Pair<Long, Long>) {
        if (seen[owner]?.isNotEmpty() == true) seen[owner] = emptySet()
    }

    fun store(owner: Pair<Long, Long>, reactions: List<TdApi.MessageReaction>) {
        seen[owner] = emojis(reactions)
    }

    private fun emojis(reactions: List<TdApi.MessageReaction>) =
        reactions.mapNotNullTo(HashSet()) { (it.type as? TdApi.ReactionTypeEmoji)?.emoji?.replace("\uFE0F", "") }

    private const val MaxMessages = 4000
}
