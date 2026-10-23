package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.core.AvatarKind
import com.pixelgram.desktop.telegram.BotCommandItem
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.components.Avatar
import com.pixelgram.desktop.ui.theme.EmojiText
import it.tdlight.jni.TdApi

val CommandQuery = Regex("""^/(\w*)$""")

private val KeyboardMaxHeight = 260.dp
private val WelcomeMaxWidth = 380.dp

@Composable
fun CommandSuggestions(
    commands: List<BotCommandItem>,
    selected: Int,
    telegram: Telegram,
    settings: AppSettings,
    onPick: (BotCommandItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 3.dp,
        modifier = modifier,
    ) {
        LazyColumn(Modifier.heightIn(max = 280.dp).padding(vertical = 6.dp)) {
            itemsIndexed(commands, key = { _, item -> "${item.botId}/${item.command}" }) { index, item ->
                val bot = telegram.users[item.botId]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (index == selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                        .clickable { onPick(item) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Avatar(telegram.userName(item.botId), bot?.profilePhoto?.small, telegram, settings, 32.dp, kind = AvatarKind.Bot)
                    Text("/${item.command}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    EmojiText(
                        item.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun ReplyKeyboardPanel(keyboard: TdApi.ReplyMarkupShowKeyboard, onPress: (TdApi.KeyboardButton) -> Unit, modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = KeyboardMaxHeight)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        keyboard.rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { button ->
                    FilledTonalButton(
                        onClick = { onPress(button) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        EmojiText(button.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
fun BotWelcome(botId: Long, telegram: Telegram, onStart: () -> Unit, modifier: Modifier = Modifier) {
    var info by remember(botId) { mutableStateOf<TdApi.BotInfo?>(null) }
    LaunchedEffect(botId) { telegram.botInfo(botId) { info = it } }
    val loaded = info ?: return
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.widthIn(max = WelcomeMaxWidth),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            val photo = loaded.photo?.sizes?.maxByOrNull { it.width }
            val cover = photo?.photo ?: loaded.animation?.thumbnail?.file
            if (cover != null) {
                ThumbImage(
                    cover,
                    loaded.photo?.minithumbnail ?: loaded.animation?.minithumbnail,
                    telegram,
                    Modifier.fillMaxWidth().heightIn(max = 220.dp).clip(RoundedCornerShape(20.dp)),
                    ContentScale.Crop,
                )
            }
            Text("What can this bot do?", style = MaterialTheme.typography.titleMedium)
            EmojiText(
                loaded.description.ifBlank { loaded.shortDescription },
                style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
            )
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text("Start") }
        }
    }
}
