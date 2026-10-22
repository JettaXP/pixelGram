package com.pixelgram.desktop.ui.main

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
fun ReactionChips(reactions: List<TdApi.MessageReaction>, outgoing: Boolean, onToggle: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(top = 4.dp),
    ) {
        reactions.forEach { reaction ->
            val emoji = (reaction.type as? TdApi.ReactionTypeEmoji)?.emoji ?: return@forEach
            val pop = remember(emoji) { Animatable(0.6f) }
            LaunchedEffect(reaction.totalCount, reaction.isChosen) { pop.snapTo(0.85f); pop.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.45f, stiffness = 500f)) }
            val chosen = reaction.isChosen
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
                    EmojiText(emoji, fontSize = 15.sp)
                    Text(reaction.totalCount.toString(), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
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
                        color = scheme.secondaryContainer,
                        contentColor = scheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f).height(ButtonHeight),
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                            EmojiText(
                                button.text,
                                style = MaterialTheme.typography.labelLarge.copy(textAlign = TextAlign.Center),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
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

@Composable
fun BotAnswer(answer: Pair<String, Boolean>?, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val (text, alert) = answer ?: (null to false)
    if (text != null && alert) {
        androidx.compose.material3.AlertDialog(
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
