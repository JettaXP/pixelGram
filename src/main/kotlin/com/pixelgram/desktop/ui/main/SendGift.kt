package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
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
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.telegram.ChatState
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.outlined.Star
import it.tdlight.jni.TdApi

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SendGiftDialog(chat: ChatState, telegram: Telegram, settings: AppSettings, onDismiss: () -> Unit) {
    var gifts by remember { mutableStateOf<List<TdApi.Gift>?>(null) }
    var chosen by remember { mutableStateOf<TdApi.Gift?>(null) }
    var note by remember { mutableStateOf("") }
    var hidden by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(chat.id) {
        telegram.send(TdApi.GetAvailableGifts()) { result ->
            gifts = (result as? TdApi.AvailableGifts)?.gifts?.mapNotNull { it?.gift }?.filter { !it.isPremium || telegram.me?.isPremium == true }.orEmpty()
        }
    }
    val title = telegram.chatTitle(chat)
    val gift = chosen
    AlertDialog(
        onDismissRequest = { if (!sending) onDismiss() },
        title = { Text(if (gift == null) "Send a Gift" else title) },
        text = {
            when {
                gift != null -> Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    StickerView(gift.sticker, telegram, 120.dp)
                    OutlinedTextField(
                        note,
                        { note = it.take(NoteLimit) },
                        label = { Text("Message") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { hidden = !hidden }.padding(end = 12.dp),
                    ) {
                        Checkbox(checked = hidden, onCheckedChange = { hidden = it })
                        Text("Hide my name and message")
                    }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
                gifts == null -> Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) { LoadingIndicator() }
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(GiftCell),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(GridHeight),
                ) {
                    items(gifts.orEmpty(), key = { it.id }) { item ->
                        Surface(
                            onClick = { chosen = item },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        ) {
                            Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                StickerView(item.sticker, telegram, 64.dp, animate = false)
                                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
                                    Row(Modifier.padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.Star, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Text(" ${item.starCount}", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (gift != null) {
                TextButton(enabled = !sending, onClick = {
                    sending = true
                    error = null
                    val owner = TdApi.MessageSenderUser(chat.privateUserId)
                    telegram.send(TdApi.SendGift(gift.id, owner, TdApi.FormattedText(note.trim(), emptyArray()), hidden, false)) { result ->
                        sending = false
                        if (result is TdApi.Error) error = result.message else onDismiss()
                    }
                }) { Text("${com.pixelgram.desktop.core.tr("Send for")} ${gift.starCount} ⭐") }
            }
        },
        dismissButton = {
            TextButton(onClick = { if (gift != null) chosen = null else onDismiss() }) { Text(if (gift != null) "Back" else "Cancel") }
        },
    )
}

private val GiftCell = 96.dp
private val GridHeight = 360.dp
private const val NoteLimit = 255
