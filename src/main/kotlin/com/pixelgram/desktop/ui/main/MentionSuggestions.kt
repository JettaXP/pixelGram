package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.components.Avatar
import com.pixelgram.desktop.ui.theme.EmojiText
import it.tdlight.jni.TdApi

val MentionQuery = Regex("""(?:^|\s)@(\w*)$""")

@Composable
fun MentionSuggestions(
    ids: List<Long>,
    selected: Int,
    telegram: Telegram,
    settings: AppSettings,
    onPick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 3.dp,
        modifier = modifier,
    ) {
        LazyColumn(Modifier.heightIn(max = 280.dp).padding(vertical = 6.dp)) {
            itemsIndexed(ids, key = { _, id -> id }) { index, id ->
                val user = telegram.users[id]
                val name = user?.let { listOf(it.firstName, it.lastName).filter(String::isNotBlank).joinToString(" ") } ?: ""
                val username = user?.usernames?.activeUsernames?.firstOrNull()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (index == selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                        .clickable { onPick(id) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Avatar(name, user?.profilePhoto?.small, telegram, settings, size = 36.dp)
                    Column(Modifier.weight(1f)) {
                        EmojiText(name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (username != null) {
                            Text(
                                "@$username" + if (user.type is TdApi.UserTypeBot) " • bot" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
