package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.core.tr
import com.pixelgram.desktop.telegram.ChatState
import com.pixelgram.desktop.telegram.DefaultChatReactions
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.telegram.boostChat
import com.pixelgram.desktop.telegram.boostStatus
import com.pixelgram.desktop.telegram.chatReactions
import com.pixelgram.desktop.telegram.chatStatistics
import com.pixelgram.desktop.telegram.discussionCandidates
import com.pixelgram.desktop.telegram.linkedChat
import com.pixelgram.desktop.telegram.setChatReactions
import com.pixelgram.desktop.telegram.setDiscussionGroup
import com.pixelgram.desktop.ui.components.ChatAvatar
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.components.expressive.AlertDialog
import com.pixelgram.desktop.ui.components.expressive.Button
import com.pixelgram.desktop.ui.components.expressive.TextButton
import com.pixelgram.desktop.ui.components.groupShapes
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.outlined.ContentCopy
import com.pixelgram.desktop.ui.icons.outlined.LinkOff
import com.pixelgram.desktop.ui.theme.EmojiText
import it.tdlight.jni.TdApi
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

private enum class ReactionMode(val title: String) { All("All reactions"), Some("Some reactions"), None("No reactions") }

@Composable
internal fun ReactionsPage(chat: ChatState, telegram: Telegram, back: () -> Unit) {
    var loaded by remember(chat.id) { mutableStateOf(false) }
    var mode by remember(chat.id) { mutableStateOf(ReactionMode.All) }
    val picked = remember(chat.id) { mutableStateListOf<String>() }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(chat.id) {
        telegram.chatReactions(chat.id) { reactions ->
            when (reactions) {
                is TdApi.ChatAvailableReactionsSome -> {
                    val emoji = reactions.reactions.mapNotNull { (it as? TdApi.ReactionTypeEmoji)?.emoji }
                    picked.clear()
                    picked.addAll(emoji)
                    mode = if (emoji.isEmpty()) ReactionMode.None else ReactionMode.Some
                }
                else -> mode = ReactionMode.All
            }
            loaded = true
        }
    }
    if (!loaded) return LoadingIndicator(Modifier.padding(24.dp))
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            ReactionMode.entries.forEachIndexed { index, option ->
                SegmentedListItem(
                    onClick = { mode = option },
                    shapes = groupShapes(index, ReactionMode.entries.size),
                    leadingContent = { RadioButton(selected = mode == option, onClick = { mode = option }) },
                ) { Text(option.title) }
            }
        }
        if (mode == ReactionMode.Some) {
            Text("Choose which reactions people can use.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DefaultChatReactions.forEach { emoji ->
                    val on = emoji in picked
                    FilterChip(
                        selected = on,
                        onClick = { if (on) picked.remove(emoji) else picked.add(emoji) },
                        label = { EmojiText(emoji) },
                    )
                }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = {
                busy = true
                val reactions = when (mode) {
                    ReactionMode.All -> TdApi.ChatAvailableReactionsAll(0)
                    ReactionMode.Some -> TdApi.ChatAvailableReactionsSome(picked.map { TdApi.ReactionTypeEmoji(it) }.toTypedArray(), 0)
                    ReactionMode.None -> TdApi.ChatAvailableReactionsSome(emptyArray(), 0)
                }
                telegram.setChatReactions(chat.id, reactions) { failure ->
                    busy = false
                    error = failure
                    if (failure == null) back()
                }
            },
            enabled = !busy && (mode != ReactionMode.Some || picked.isNotEmpty()),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save") }
    }
}

@Composable
internal fun DiscussionPage(chat: ChatState, telegram: Telegram, settings: AppSettings?) {
    var linked by remember(chat.id) { mutableStateOf<Long?>(null) }
    var candidates by remember(chat.id) { mutableStateOf<List<Long>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(chat.id) {
        telegram.linkedChat(chat) { linked = it }
        if (chat.isChannel) telegram.discussionCandidates { candidates = it }
    }
    val current = linked ?: return LoadingIndicator(Modifier.padding(24.dp))
    fun link(groupId: Long) {
        busy = true
        val channelId = if (chat.isChannel) chat.id else current
        telegram.setDiscussionGroup(channelId, groupId) { failure ->
            busy = false
            error = failure
            if (failure == null) linked = groupId
        }
    }
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            if (chat.isChannel) "Comments under posts are sent to the discussion group." else "This group is used for comments of the linked channel.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val linkedChat = telegram.chats[current]
        if (current != 0L) {
            SegmentedListItem(
                onClick = {},
                shapes = groupShapes(0, 2),
                leadingContent = { linkedChat?.let { ChatAvatar(it, telegram, settings ?: return@let, 40.dp) } },
                supportingContent = { Text(if (chat.isChannel) "Discussion group" else "Linked channel") },
            ) { EmojiText(linkedChat?.let(telegram::chatTitle) ?: tr("Chat")) }
            SegmentedListItem(
                onClick = { if (!busy) link(0L) },
                shapes = groupShapes(1, 2),
                leadingContent = { Icon(Icons.Outlined.LinkOff, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            ) { Text(if (chat.isChannel) "Unlink group" else "Unlink channel", color = MaterialTheme.colorScheme.error) }
        } else if (chat.isChannel) {
            val list = candidates
            when {
                list == null -> LoadingIndicator(Modifier.padding(12.dp))
                list.isEmpty() -> Text("You have no groups that can be linked. Create a public group first.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Choose a group", style = MaterialTheme.typography.titleSmall)
                    list.forEachIndexed { index, id ->
                        val group = telegram.chats[id]
                        SegmentedListItem(
                            onClick = { if (!busy) link(id) },
                            shapes = groupShapes(index, list.size),
                            leadingContent = { group?.let { ChatAvatar(it, telegram, settings ?: return@let, 40.dp) } },
                        ) { EmojiText(group?.let(telegram::chatTitle) ?: tr("Chat")) }
                    }
                }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
internal fun StatisticsPage(chat: ChatState, telegram: Telegram, dark: Boolean) {
    var stats by remember(chat.id) { mutableStateOf<TdApi.ChatStatistics?>(null) }
    var error by remember(chat.id) { mutableStateOf<String?>(null) }
    LaunchedEffect(chat.id) {
        telegram.chatStatistics(chat.id, dark) { result, failure ->
            stats = result
            error = failure ?: if (result == null) tr("Statistics are not available for this chat") else null
        }
    }
    error?.let { return Text(it, color = MaterialTheme.colorScheme.error) }
    val data = stats ?: return LoadingIndicator(Modifier.padding(24.dp))
    val rows: List<Pair<String, TdApi.StatisticalValue?>> = when (data) {
        is TdApi.ChatStatisticsChannel -> listOf(
            "Subscribers" to data.memberCount,
            "Views per post" to data.meanMessageViewCount,
            "Shares per post" to data.meanMessageShareCount,
            "Reactions per post" to data.meanMessageReactionCount,
            "Views per story" to data.meanStoryViewCount,
            "Shares per story" to data.meanStoryShareCount,
            "Reactions per story" to data.meanStoryReactionCount,
        )
        is TdApi.ChatStatisticsSupergroup -> listOf(
            "Members" to data.memberCount,
            "Messages" to data.messageCount,
            "Viewing members" to data.viewerCount,
            "Posting members" to data.senderCount,
        )
        else -> emptyList()
    }
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        rows.forEachIndexed { index, (title, value) ->
            StatRow(title, value, index, rows.size + if (data is TdApi.ChatStatisticsChannel) 1 else 0)
        }
        if (data is TdApi.ChatStatisticsChannel) {
            SegmentedListItem(
                onClick = {},
                shapes = groupShapes(rows.size, rows.size + 1),
                trailingContent = { Text("%.1f%%".format(data.enabledNotificationsPercentage), fontWeight = FontWeight.SemiBold) },
            ) { Text("Notifications enabled") }
        }
    }
}

@Composable
private fun StatRow(title: String, value: TdApi.StatisticalValue?, index: Int, count: Int) {
    SegmentedListItem(
        onClick = {},
        shapes = groupShapes(index, count),
        supportingContent = value?.takeIf { it.previousValue != 0.0 || it.growthRatePercentage != 0.0 }?.let { stat ->
            {
                val up = stat.growthRatePercentage >= 0
                Text(
                    "%s%.1f%%".format(if (up) "+" else "", stat.growthRatePercentage),
                    color = if (up) GrowthColor else MaterialTheme.colorScheme.error,
                )
            }
        },
        trailingContent = { Text(value?.let { "%,d".format(it.value.toLong()) } ?: "—", style = MaterialTheme.typography.titleMedium) },
    ) { Text(title) }
}

@Composable
internal fun BoostsContent(chat: ChatState, telegram: Telegram, canBoost: Boolean) {
    var status by remember(chat.id) { mutableStateOf<TdApi.ChatBoostStatus?>(null) }
    var error by remember(chat.id) { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var version by remember { mutableStateOf(0) }
    LaunchedEffect(chat.id, version) {
        telegram.boostStatus(chat.id) { result, failure ->
            status = result
            error = failure
        }
    }
    error?.let { return Text(it, color = MaterialTheme.colorScheme.error) }
    val boost = status ?: return LoadingIndicator(Modifier.padding(24.dp))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.secondaryContainer) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${tr("Level")} ${boost.level}", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    val next = boost.nextLevelBoostCount
                    Text(
                        if (next > 0) "${boost.boostCount} / $next" else boost.boostCount.toString(),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                val span = (boost.nextLevelBoostCount - boost.currentLevelBoostCount).coerceAtLeast(1)
                val progress = if (boost.nextLevelBoostCount <= 0) 1f else ((boost.boostCount - boost.currentLevelBoostCount).toFloat() / span).coerceIn(0f, 1f)
                LinearWavyProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                Text(
                    "${tr("Premium subscribers")}: ${"%,d".format(boost.premiumMemberCount)} (${"%.1f".format(boost.premiumMemberPercentage)}%)",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        if (boost.boostUrl.isNotEmpty()) {
            SegmentedListItem(
                onClick = {
                    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(boost.boostUrl), null)
                    note = tr("Link copied")
                },
                shapes = groupShapes(0, 1),
                supportingContent = { Text(boost.boostUrl, maxLines = 1) },
                trailingContent = { Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy link") },
            ) { Text("Boost link") }
        }
        if (canBoost) {
            Button(
                onClick = {
                    busy = true
                    telegram.boostChat(chat.id) { failure ->
                        busy = false
                        note = failure ?: tr("Thanks for the boost!")
                        if (failure == null) version++
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (chat.isChannel) "Boost channel" else "Boost group") }
        }
        note?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
fun BoostDialog(chat: ChatState, telegram: Telegram, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Boosts") },
        text = { Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) { BoostsContent(chat, telegram, canBoost = true) } },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

private val GrowthColor = Color(0xFF2E9E5B)
