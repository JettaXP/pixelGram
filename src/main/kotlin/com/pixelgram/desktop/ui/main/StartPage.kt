package com.pixelgram.desktop.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.telegram.ChatState
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.outlined.Lock
import com.pixelgram.desktop.ui.theme.EmojiText
import it.tdlight.jni.TdApi

private const val TitleLimit = 32
private const val MessageLimit = 70
private val GreetingSticker = 120.dp

@Composable
fun EmptyChatGreeting(chat: ChatState, telegram: Telegram, modifier: Modifier = Modifier) {
    val userId = chat.privateUserId
    var page by remember(userId) { mutableStateOf<TdApi.BusinessStartPage?>(null) }
    var loaded by remember(userId) { mutableStateOf(false) }
    var fallback by remember(userId) { mutableStateOf<TdApi.Sticker?>(null) }
    var how by remember { mutableStateOf(false) }
    LaunchedEffect(userId) {
        telegram.userFullInfo(userId) { info ->
            page = info?.businessInfo?.startPage
            loaded = true
            if (page?.sticker == null) telegram.greetingStickers { list -> fallback = list.randomOrNull() }
        }
    }
    val sticker = page?.sticker ?: fallback
    AnimatedVisibility(
        visible = loaded,
        enter = scaleIn(MaterialTheme.motionScheme.defaultSpatialSpec(), initialScale = 0.85f) + fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
        modifier = modifier,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (page != null) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        EmojiText(
                            "${telegram.chatTitle(chat)} ${com.pixelgram.desktop.core.tr("added the message below for all empty chats.")} ",
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Text(
                            "How?",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clip(CircleShape).clickable { how = true },
                        )
                    }
                }
            }
            GreetingCard(page?.title.orEmpty(), page?.message.orEmpty(), sticker, telegram) { picked ->
                telegram.sendSticker(chat.id, picked)
            }
        }
    }
    if (how) {
        AlertDialog(
            onDismissRequest = { how = false },
            title = { Text("Start page") },
            text = { Text("Telegram Premium users can set a custom title, message and sticker that people see before they start a chat. Set yours in My profile → Start page.") },
            confirmButton = { TextButton(onClick = { how = false }) { Text("OK") } },
        )
    }
}

@Composable
private fun GreetingCard(title: String, message: String, sticker: TdApi.Sticker?, telegram: Telegram, onSticker: ((TdApi.Sticker) -> Unit)?) {
    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.widthIn(max = 260.dp)) {
        Column(
            Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            EmojiText(
                title.ifBlank { com.pixelgram.desktop.core.tr("No messages here yet...") },
                style = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center),
            )
            EmojiText(
                message.ifBlank { com.pixelgram.desktop.core.tr("Send a message or click on the greeting below") },
                style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
            )
            Box(Modifier.padding(top = 8.dp).size(GreetingSticker), contentAlignment = Alignment.Center) {
                if (sticker != null) {
                    StickerView(
                        sticker,
                        telegram,
                        GreetingSticker,
                        Modifier.clip(RoundedCornerShape(16.dp)).clickable(enabled = onSticker != null) { onSticker?.invoke(sticker) },
                    )
                }
            }
        }
    }
}

@Composable
fun StartPageDialog(telegram: Telegram, current: TdApi.BusinessStartPage?, onDismiss: () -> Unit, onSaved: () -> Unit) {
    var title by remember { mutableStateOf(current?.title.orEmpty()) }
    var message by remember { mutableStateOf(current?.message.orEmpty()) }
    var sticker by remember { mutableStateOf(current?.sticker) }
    var preview by remember { mutableStateOf<TdApi.Sticker?>(null) }
    var choosing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var premiumHint by remember { mutableStateOf(false) }
    val premium = telegram.isPremium
    LaunchedEffect(Unit) { telegram.greetingStickers { preview = it.randomOrNull() } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start page") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.widthIn(min = 340.dp)) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    GreetingCard(title, message, sticker ?: preview, telegram, null)
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(TitleLimit) },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it.take(MessageLimit) },
                    label = { Text("Message") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable {
                        if (premium) choosing = true else premiumHint = true
                    },
                ) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Choose sticker", modifier = Modifier.weight(1f))
                        if (!premium) Icon(Icons.Outlined.Lock, null, Modifier.padding(end = 6.dp).size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        val chosen = sticker
                        if (chosen != null) StickerView(chosen, telegram, 28.dp, animate = false)
                        else Text("Random", color = MaterialTheme.colorScheme.primary)
                    }
                }
                Text(
                    if (premiumHint) "Choosing a sticker is available with Telegram Premium." else "You can customize the message people see before they start a chat with you.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (premiumHint) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                telegram.setStartPage(title, message, sticker) { failure ->
                    if (failure == null) onSaved() else error = failure
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
    if (choosing) {
        StickerChooser(telegram, onDismiss = { choosing = false }) { picked ->
            sticker = picked
            choosing = false
        }
    }
}

@Composable
private fun StickerChooser(telegram: Telegram, onDismiss: () -> Unit, onPicked: (TdApi.Sticker?) -> Unit) {
    var stickers by remember { mutableStateOf<List<TdApi.Sticker>>(emptyList()) }
    LaunchedEffect(Unit) {
        telegram.greetingStickers { greeting ->
            telegram.recentStickers { recent -> stickers = (greeting + recent).distinctBy { it.sticker.id } }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose sticker") },
        text = {
            LazyVerticalGrid(GridCells.Adaptive(72.dp), Modifier.heightIn(max = 380.dp).widthIn(min = 320.dp)) {
                item {
                    Box(
                        Modifier.aspectRatio(1f).padding(4.dp).clip(RoundedCornerShape(16.dp)).clickable { onPicked(null) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Random", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                    }
                }
                items(stickers, key = { it.sticker.id }) { item ->
                    Box(
                        Modifier.aspectRatio(1f).padding(4.dp).clip(RoundedCornerShape(16.dp)).clickable { onPicked(item) },
                        contentAlignment = Alignment.Center,
                    ) {
                        StickerView(item, telegram, 64.dp, animate = false)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
