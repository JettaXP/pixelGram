package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.telegram.setGiftResalePrice
import com.pixelgram.desktop.telegram.takeOffGift
import com.pixelgram.desktop.telegram.transferGift
import com.pixelgram.desktop.telegram.wearGift
import com.pixelgram.desktop.telegram.wornGiftId
import com.pixelgram.desktop.ui.components.GroupedMenu
import com.pixelgram.desktop.ui.components.MenuEntry
import com.pixelgram.desktop.ui.components.MenuGroups
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.components.expressive.AlertDialog
import com.pixelgram.desktop.ui.components.expressive.IconButton
import com.pixelgram.desktop.ui.components.expressive.TextButton
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.filled.MoreVert
import com.pixelgram.desktop.ui.icons.outlined.Checkroom
import com.pixelgram.desktop.ui.icons.outlined.MoneyOff
import com.pixelgram.desktop.ui.icons.outlined.Sell
import com.pixelgram.desktop.ui.icons.outlined.Send
import com.pixelgram.desktop.ui.icons.outlined.Share
import com.pixelgram.desktop.ui.icons.outlined.Visibility
import com.pixelgram.desktop.ui.theme.LocalAppSettings
import it.tdlight.jni.TdApi

private enum class NftAction { Transfer, Share }

private fun nowSeconds() = (System.currentTimeMillis() / 1000).toInt()

@Composable
internal fun NftMenu(gift: TdApi.UpgradedGift, received: TdApi.ReceivedGift?, telegram: Telegram, onChanged: () -> Unit, onClosed: () -> Unit, modifier: Modifier = Modifier) {
    val settings = LocalAppSettings.current
    var open by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf<NftAction?>(null) }
    var target by remember { mutableStateOf<Long?>(null) }
    var pricing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var worn by remember(gift.id) { mutableStateOf(telegram.wornGiftId() == gift.id) }
    var saved by remember(received?.receivedGiftId) { mutableStateOf(received?.isSaved == true) }
    var listed by remember(gift.id) { mutableStateOf(gift.resaleParameters?.starCount?.takeIf { it > 0 }) }
    val entries = buildList {
        if (received != null) {
            add(MenuEntry(Icons.Outlined.Checkroom, if (worn) "Take off" else "Wear") {
                open = false
                if (worn) telegram.takeOffGift { failure -> if (failure == null) worn = false else error = failure }
                else telegram.wearGift(gift) { failure -> if (failure == null) worn = true else error = failure }
            })
            if (received.canBeTransferred && settings != null) {
                add(MenuEntry(Icons.Outlined.Send, "Transfer", enabled = received.nextTransferDate <= nowSeconds()) {
                    open = false
                    picking = NftAction.Transfer
                })
            }
            add(MenuEntry(Icons.Outlined.Sell, if (listed == null) "Sell" else "Change price", enabled = received.nextResaleDate <= nowSeconds()) {
                open = false
                pricing = true
            })
            if (listed != null) {
                add(MenuEntry(Icons.Outlined.MoneyOff, "Remove from sale") {
                    open = false
                    telegram.setGiftResalePrice(received, null) { failure -> if (failure == null) listed = null else error = failure }
                })
            }
            add(MenuEntry(Icons.Outlined.Visibility, if (saved) "Hide from profile" else "Show on profile") {
                open = false
                telegram.setGiftSaved(received.receivedGiftId, !saved) { failure ->
                    if (failure == null) {
                        saved = !saved
                        received.isSaved = saved
                    } else {
                        error = failure
                    }
                }
            })
        }
        if (settings != null) {
            add(MenuEntry(Icons.Outlined.Share, "Share") {
                open = false
                picking = NftAction.Share
            })
        }
    }
    if (entries.isEmpty()) return
    Box(modifier) {
        IconButton(onClick = { open = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "More") }
        GroupedMenu(expanded = open, onDismiss = { open = false }) { MenuGroups(listOf(entries)) }
    }
    val action = picking
    if (action != null && settings != null) {
        ForwardDialog(telegram, settings, onDismiss = { picking = null }) { chatId ->
            picking = null
            when (action) {
                NftAction.Share -> telegram.sendText(chatId, nftLink(gift))
                NftAction.Transfer -> target = chatId
            }
        }
    }
    val to = target
    if (to != null && received != null) {
        val chat = telegram.chats[to]
        val name = chat?.let { telegram.chatTitle(it) }.orEmpty()
        AlertDialog(
            onDismissRequest = { target = null },
            title = { Text("Transfer gift?") },
            text = {
                Text(
                    "${gift.title} #${gift.number} → $name" +
                        if (received.transferStarCount > 0) "\n" + stars(received.transferStarCount) else "",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    target = null
                    val owner: TdApi.MessageSender = if (chat != null && chat.privateUserId != 0L) TdApi.MessageSenderUser(chat.privateUserId) else TdApi.MessageSenderChat(to)
                    telegram.transferGift(received, owner) { failure ->
                        if (failure == null) {
                            onChanged()
                            onClosed()
                        } else {
                            error = failure
                        }
                    }
                }) { Text("Transfer") }
            },
            dismissButton = { TextButton(onClick = { target = null }) { Text("Cancel") } },
        )
    }
    if (pricing && received != null) {
        NameDialog("Sell gift", "Price in Stars", onDismiss = { pricing = false }) { value, done ->
            val price = value.trim().toLongOrNull()?.takeIf { it > 0 }
            if (price == null) {
                done("Enter a number of Stars")
            } else {
                telegram.setGiftResalePrice(received, price) { failure ->
                    if (failure == null) listed = price
                    done(failure)
                }
            }
        }
    }
    error?.let { message ->
        AlertDialog(
            onDismissRequest = { error = null },
            title = { Text("Gift") },
            text = { Text(message, color = MaterialTheme.colorScheme.error) },
            confirmButton = { TextButton(onClick = { error = null }) { Text("OK") } },
        )
    }
}
