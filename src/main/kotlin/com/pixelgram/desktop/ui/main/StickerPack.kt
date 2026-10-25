package com.pixelgram.desktop.ui.main

import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.automirrored.outlined.Forward
import com.pixelgram.desktop.ui.icons.filled.Close
import com.pixelgram.desktop.ui.icons.filled.MoreVert
import com.pixelgram.desktop.ui.icons.outlined.ContentCopy
import com.pixelgram.desktop.ui.components.expressive.BasicAlertDialog
import com.pixelgram.desktop.ui.components.expressive.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import com.pixelgram.desktop.ui.components.expressive.IconButton
import androidx.compose.material3.LoadingIndicator
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.theme.EmojiText
import it.tdlight.jni.TdApi
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

class StickerPackRequest(val telegram: Telegram, val chatId: Long, val setId: Long)

object StickerPack {
    var current by mutableStateOf<StickerPackRequest?>(null)
}

private val PackCell = 72.dp
private val PackGridMax = 420.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StickerPackDialog(request: StickerPackRequest, settings: AppSettings, onDismiss: () -> Unit) {
    val telegram = request.telegram
    var set by remember(request.setId) { mutableStateOf<TdApi.StickerSet?>(null) }
    var busy by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var sharing by remember { mutableStateOf(false) }
    LaunchedEffect(request.setId) { telegram.stickerSet(request.setId) { set = it } }
    val loaded = set
    val link = loaded?.let { "https://t.me/${if (it.stickerType is TdApi.StickerTypeCustomEmoji) "addemoji" else "addstickers"}/${it.name}" }
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.width(440.dp),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EmojiText(
                        loaded?.title ?: "Stickers",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                    )
                    Box {
                        IconButton(onClick = { menu = true }, enabled = link != null) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text("Share stickers") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Forward, contentDescription = null) },
                                onClick = {
                                    menu = false
                                    sharing = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Copy link") },
                                leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                                onClick = {
                                    menu = false
                                    link?.let { Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(it), null) }
                                },
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Close") }
                }
                if (loaded == null) {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { LoadingIndicator() }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(PackCell),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(max = PackGridMax),
                    ) {
                        val sendable = loaded.stickerType is TdApi.StickerTypeRegular && request.chatId != 0L
                        items(loaded.stickers.toList(), key = { it.sticker.id }) { sticker ->
                            Box(
                                Modifier
                                    .size(PackCell)
                                    .clip(RoundedCornerShape(12.dp))
                                    .then(
                                        if (sendable) {
                                            Modifier.clickable {
                                                telegram.sendSticker(request.chatId, sticker)
                                                onDismiss()
                                            }
                                        } else {
                                            Modifier
                                        },
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                StickerView(sticker, telegram, PackCell, animate = com.pixelgram.desktop.core.PowerSaving.stickersInPanel)
                            }
                        }
                    }
                    val installed = loaded.isInstalled && !loaded.isArchived
                    val noun = if (loaded.stickerType is TdApi.StickerTypeCustomEmoji) "emoji" else "stickers"
                    Button(
                        onClick = {
                            busy = true
                            telegram.setStickerSetInstalled(loaded.id, !installed) {
                                busy = false
                                telegram.stickerSet(loaded.id) { set = it }
                            }
                        },
                        enabled = !busy,
                        colors = if (installed) ButtonDefaults.filledTonalButtonColors() else ButtonDefaults.buttonColors(),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) {
                        Text(if (installed) "Remove $noun" else "Add $noun")
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = if (installed) 0f else 0.2f),
                            modifier = Modifier.padding(start = 8.dp),
                        ) {
                            Text(
                                loaded.stickers.size.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }
        }
    }
    if (sharing && link != null) {
        ForwardDialog(telegram, settings, onDismiss = { sharing = false }) { target ->
            sharing = false
            telegram.sendText(target, link)
        }
    }
}

fun Modifier.openPack(telegram: Telegram, chatId: Long, sticker: TdApi.Sticker?): Modifier =
    if (sticker == null || sticker.setId == 0L) this else clickable {
        StickerPack.current = StickerPackRequest(telegram, chatId, sticker.setId)
    }
