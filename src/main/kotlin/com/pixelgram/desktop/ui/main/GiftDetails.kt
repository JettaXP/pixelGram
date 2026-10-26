package com.pixelgram.desktop.ui.main

import com.pixelgram.desktop.ui.icons.outlined.CardGiftcard
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.pixelgram.desktop.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.telegram.UiMessage
import com.pixelgram.desktop.ui.components.InfoGroup
import com.pixelgram.desktop.ui.components.InfoRow
import com.pixelgram.desktop.ui.components.expressive.BasicAlertDialog
import com.pixelgram.desktop.ui.components.expressive.TextButton
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.outlined.AccountCircle
import com.pixelgram.desktop.ui.icons.outlined.CalendarMonth
import com.pixelgram.desktop.ui.icons.outlined.Info
import com.pixelgram.desktop.ui.icons.outlined.Numbers
import com.pixelgram.desktop.ui.icons.outlined.Palette
import com.pixelgram.desktop.ui.icons.outlined.Payments
import com.pixelgram.desktop.ui.icons.outlined.Star
import com.pixelgram.desktop.ui.icons.outlined.Upgrade
import com.pixelgram.desktop.ui.theme.EmojiText
import it.tdlight.jni.TdApi
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun formatPrice(amount: Long, currency: String): String = runCatching {
    val code = java.util.Currency.getInstance(currency)
    val format = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.US).apply {
        this.currency = code
        minimumFractionDigits = code.defaultFractionDigits
        maximumFractionDigits = code.defaultFractionDigits
    }
    format.format(amount / Math.pow(10.0, code.defaultFractionDigits.toDouble()))
}.getOrElse { "$amount $currency" }

private fun formatCrypto(amount: Long, currency: String): String =
    "%s %s".format(java.math.BigDecimal.valueOf(amount, CryptoDecimals).stripTrailingZeros().toPlainString(), currency)

fun paidPrice(currency: String?, amount: Long, crypto: String?, cryptoAmount: Long): String? = when {
    !crypto.isNullOrBlank() && cryptoAmount > 0 -> formatCrypto(cryptoAmount, crypto)
    !currency.isNullOrBlank() && amount > 0 -> formatPrice(amount, currency)
    else -> null
}

private const val GiftDateSlack = 5

fun stars(count: Long) = if (count == 1L) "1 Star" else "$count Stars"

@Composable
fun GiftDetailsDialog(message: UiMessage, telegram: Telegram, onDismiss: () -> Unit) {
    val content = message.content
    val date = Instant.ofEpochSecond(message.date.toLong()).atZone(ZoneId.systemDefault()).format(DateFormat)
    var saved by remember(message.key) { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(message.key) {
        val gift = content as? TdApi.MessageGift ?: return@LaunchedEffect
        val receiver = gift.receiverId
        val toMe = (receiver as? TdApi.MessageSenderUser)?.userId == telegram.me?.id
        if (toMe && !gift.receivedGiftId.isNullOrBlank()) {
            telegram.receivedGift(gift.receivedGiftId) { found -> found?.let { saved = it.isSaved } }
        } else if (receiver != null) {
            telegram.profileGifts(receiver) { _, gifts ->
                saved = gifts.any { item ->
                    (item.gift as? TdApi.SentGiftRegular)?.gift?.id == gift.gift.id && kotlin.math.abs(item.date - message.date) <= GiftDateSlack
                }
            }
        }
    }
    fun person(sender: TdApi.MessageSender, title: String, label: String) = InfoRow(
        Icons.Outlined.AccountCircle,
        label,
        title,
        leading = { com.pixelgram.desktop.ui.components.AvatarImage(title, telegram.senderPhoto(sender), telegram, androidx.compose.foundation.shape.CircleShape, 32.dp) },
    )
    val rows = buildList {
        when (content) {
            is TdApi.MessageGift -> {
                content.senderId?.let { add(person(it, telegram.senderTitle(it), "From")) }
                content.receiverId?.let { add(person(it, telegram.senderTitle(it), "To")) }
                add(InfoRow(Icons.Outlined.CalendarMonth, "Date", date))
                add(InfoRow(Icons.Outlined.Star, "Price", stars(content.gift.starCount)))
                if (content.sellStarCount > 0) add(InfoRow(Icons.Outlined.Payments, "Can be converted to", stars(content.sellStarCount)))
                if (content.gift.upgradeStarCount > 0) add(InfoRow(Icons.Outlined.Upgrade, "Upgrade", stars(content.gift.upgradeStarCount)))
                val status = when {
                    content.wasRefunded -> "Refunded"
                    content.wasUpgraded -> "Upgraded to a collectible"
                    content.wasConverted -> "Converted to Stars"
                    saved ?: content.isSaved -> "Shown on the profile"
                    else -> "Not shown on the profile"
                }
                add(InfoRow(Icons.Outlined.Info, "Status", status))
                if (content.isPrivate) add(InfoRow(Icons.Outlined.Info, "Sender", "Hidden from others"))
            }
            is TdApi.MessageUpgradedGift -> {
                val gift = content.gift
                content.senderId?.let { add(person(it, telegram.senderTitle(it), "From")) }
                gift.ownerId?.let { add(person(it, telegram.senderTitle(it), "Owner")) }
                    ?: gift.ownerName.takeIf { it.isNotBlank() }?.let { add(InfoRow(Icons.Outlined.AccountCircle, "Owner", it)) }
                add(InfoRow(Icons.Outlined.Numbers, "Number", "#${gift.number} of ${gift.totalUpgradedCount}"))
                gift.model?.let { add(InfoRow(Icons.Outlined.Palette, "Model", it.name)) }
                gift.symbol?.let { add(InfoRow(Icons.Outlined.Palette, "Symbol", it.name)) }
                gift.backdrop?.let { add(InfoRow(Icons.Outlined.Palette, "Backdrop", it.name)) }
                if (gift.valueAmount > 0 && !gift.valueCurrency.isNullOrBlank()) add(InfoRow(Icons.Outlined.Payments, "Value", formatPrice(gift.valueAmount, gift.valueCurrency)))
                add(InfoRow(Icons.Outlined.CalendarMonth, "Date", date))
            }
            is TdApi.MessageGiftedStars -> {
                add(person(TdApi.MessageSenderUser(content.gifterUserId), telegram.userTitle(content.gifterUserId), "From"))
                add(person(TdApi.MessageSenderUser(content.receiverUserId), telegram.userTitle(content.receiverUserId), "To"))
                add(InfoRow(Icons.Outlined.Star, "Stars", content.starCount.toString()))
                paidPrice(content.currency, content.amount, content.cryptocurrency, content.cryptocurrencyAmount)?.let { add(InfoRow(Icons.Outlined.Payments, "Paid", it)) }
                add(InfoRow(Icons.Outlined.CalendarMonth, "Date", date))
                content.transactionId?.takeIf { it.isNotBlank() }?.let { add(InfoRow(Icons.Outlined.Numbers, "Transaction", it)) }
            }
            is TdApi.MessageGiftedPremium -> {
                add(person(TdApi.MessageSenderUser(content.gifterUserId), telegram.userTitle(content.gifterUserId), "From"))
                add(person(TdApi.MessageSenderUser(content.receiverUserId), telegram.userTitle(content.receiverUserId), "To"))
                add(InfoRow(Icons.Outlined.Star, "Duration", if (content.monthCount > 0) "${content.monthCount} months" else "${content.dayCount} days"))
                paidPrice(content.currency, content.amount, content.cryptocurrency, content.cryptocurrencyAmount)?.let { add(InfoRow(Icons.Outlined.Payments, "Paid", it)) }
                add(InfoRow(Icons.Outlined.CalendarMonth, "Date", date))
            }
            else -> Unit
        }
    }
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.widthIn(min = 340.dp, max = 440.dp)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
            ) {
                val sticker = when (content) {
                    is TdApi.MessageGift -> content.gift.sticker
                    is TdApi.MessageUpgradedGift -> content.gift.model?.sticker
                    is TdApi.MessageGiftedStars -> content.sticker
                    is TdApi.MessageGiftedPremium -> content.sticker
                    else -> null
                }
                StickerView(sticker, telegram, 140.dp)
                EmojiText(giftTitle(message) ?: "Gift", style = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center))
                (content as? TdApi.MessageGift)?.text?.text?.takeIf { it.isNotBlank() }?.let {
                    EmojiText(it, style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center))
                }
                Column(Modifier.fillMaxWidth().padding(top = 8.dp)) { InfoGroup(rows) }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Close") }
            }
        }
    }
}

private fun giftTitle(message: UiMessage): String? = when (val content = message.content) {
    is TdApi.MessageGift -> "Gift"
    is TdApi.MessageUpgradedGift -> "${content.gift.title} #${content.gift.number}"
    is TdApi.MessageGiftedStars -> stars(content.starCount)
    is TdApi.MessageGiftedPremium -> "Telegram Premium"
    else -> null
}

private val DateFormat = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")
private const val CryptoDecimals = 9

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProfileGifts(owner: TdApi.MessageSender, telegram: Telegram, modifier: Modifier = Modifier) {
    var total by remember(owner) { mutableStateOf(0) }
    var gifts by remember(owner) { mutableStateOf(emptyList<TdApi.ReceivedGift>()) }
    var open by remember(owner) { mutableStateOf(false) }
    LaunchedEffect(owner) {
        telegram.profileGifts(owner) { count, list ->
            total = count
            gifts = list
        }
    }
    if (gifts.isEmpty()) return
    androidx.compose.material3.SegmentedListItem(
        onClick = { open = true },
        shapes = com.pixelgram.desktop.ui.components.groupShapes(0, 1),
        leadingContent = { androidx.compose.material3.Icon(Icons.Outlined.CardGiftcard, contentDescription = null) },
        trailingContent = {
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                gifts.take(GiftPreviews).forEach { gift -> StickerView(giftSticker(gift), telegram, 28.dp, animate = false) }
            }
        },
        modifier = modifier,
    ) {
        Text(if (total == 1) "1 gift" else "$total gifts")
    }
    if (open) GiftsDialog(owner, gifts, telegram) { open = false }
}

private fun giftSticker(gift: TdApi.ReceivedGift): TdApi.Sticker? = when (val sent = gift.gift) {
    is TdApi.SentGiftRegular -> sent.gift.sticker
    is TdApi.SentGiftUpgraded -> sent.gift.model?.sticker
    else -> null
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun GiftsDialog(owner: TdApi.MessageSender, gifts: List<TdApi.ReceivedGift>, telegram: Telegram, onDismiss: () -> Unit) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.widthIn(min = 360.dp, max = 480.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Gifts", style = MaterialTheme.typography.headlineSmall)
                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(GiftColumns),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = GiftGridHeight),
                ) {
                    items(gifts.size) { index ->
                        val gift = gifts[index]
                        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                            Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
                                StickerView(giftSticker(gift), telegram, 88.dp)
                                gift.senderId?.let { sender ->
                                    com.pixelgram.desktop.ui.components.AvatarImage(
                                        telegram.senderTitle(sender),
                                        telegram.senderPhoto(sender),
                                        telegram,
                                        androidx.compose.foundation.shape.CircleShape,
                                        24.dp,
                                        Modifier.align(Alignment.TopStart).padding(6.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                val name = telegram.senderTitle(owner)
                if (name.isNotBlank()) {
                    Text(
                        "These gifts were sent to $name.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Close") }
            }
        }
    }
}

private const val GiftPreviews = 3
private const val GiftColumns = 3
private val GiftGridHeight = 440.dp
