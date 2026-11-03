package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AvatarKind
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.theme.EmojiText
import com.pixelgram.desktop.ui.toShape
import it.tdlight.jni.TdApi

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PollVotersDialog(chatId: Long, messageId: Long, poll: TdApi.Poll, telegram: Telegram, onDismiss: () -> Unit) {
    val voters = remember(poll.id) { mutableStateMapOf<Int, List<TdApi.PollVoter>>() }
    LaunchedEffect(poll.id) {
        poll.options.forEachIndexed { index, option ->
            if (option.voterCount == 0) {
                voters[index] = emptyList()
                return@forEachIndexed
            }
            telegram.send(TdApi.GetPollVoters(chatId, messageId, index, 0, VotersPage)) { result ->
                voters[index] = (result as? TdApi.PollVoters)?.voters?.filterNotNull().orEmpty()
            }
        }
    }
    val settings = com.pixelgram.desktop.ui.theme.LocalAppSettings.current
    val shape = settings?.avatarShapeFor(AvatarKind.Private)?.toShape(settings.avatarCorner.value) ?: androidx.compose.foundation.shape.CircleShape
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Poll results")
                EmojiText(poll.question.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
                Text(
                    if (poll.totalVoterCount == 1) "1 vote" else "${poll.totalVoterCount} votes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            LazyColumn(Modifier.heightIn(max = ListHeight), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                poll.options.forEachIndexed { index, option ->
                    item(key = "head$index") {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            EmojiText(
                                "${option.text.text} — ${option.votePercentage}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                if (option.voterCount == 1) "1 vote" else "${option.voterCount} votes",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    val list = voters[index]
                    if (list == null) {
                        item(key = "load$index") { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { LoadingIndicator(Modifier.size(32.dp)) } }
                    } else {
                        items(list, key = { voter -> "v$index-${voter.voterId.hashCode()}-${voter.date}" }) { voter ->
                            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                                ) {
                                    com.pixelgram.desktop.ui.components.AvatarImage(
                                        telegram.senderTitle(voter.voterId),
                                        telegram.senderPhoto(voter.voterId),
                                        telegram,
                                        shape,
                                        36.dp,
                                    )
                                    EmojiText(telegram.senderTitle(voter.voterId), maxLines = 1, modifier = Modifier.weight(1f))
                                    Text(formatTime(voter.date), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

private const val VotersPage = 50
private val ListHeight = 420.dp
