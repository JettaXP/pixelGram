package com.pixelgram.desktop.ui.main

import com.pixelgram.desktop.ui.icons.outlined.CardGiftcard
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
                if (content is TdApi.MessageUpgradedGift) {
                    UniqueGiftArt(content.gift, telegram, Modifier.size(180.dp), shape = MaterialTheme.shapes.extraLarge)
                } else {
                    StickerView(sticker, telegram, 140.dp)
                }
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
fun ProfileGifts(owner: TdApi.MessageSender, telegram: Telegram, modifier: Modifier = Modifier, own: Boolean = false, index: Int = 0, count: Int = 1) {
    var total by remember(owner) { mutableStateOf(0) }
    var gifts by remember(owner) { mutableStateOf(emptyList<TdApi.ReceivedGift>()) }
    var open by remember(owner) { mutableStateOf(false) }
    LaunchedEffect(owner) {
        telegram.profileGifts(owner) { size, list ->
            total = size
            gifts = list
        }
    }
    if (gifts.isEmpty() && !own) return
    androidx.compose.material3.SegmentedListItem(
        onClick = { open = true },
        shapes = com.pixelgram.desktop.ui.components.groupShapes(index, count),
        leadingContent = { androidx.compose.material3.Icon(Icons.Outlined.CardGiftcard, contentDescription = null) },
        trailingContent = {
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                    gifts.take(GiftPreviews).forEach { gift -> StickerView(giftSticker(gift), telegram, 28.dp, animate = false) }
                }
                Text(total.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        modifier = modifier,
    ) {
        Text("Gifts")
    }
    if (open) GiftsDialog(owner, gifts, own, telegram) { open = false }
}

private fun giftSticker(gift: TdApi.ReceivedGift): TdApi.Sticker? = when (val sent = gift.gift) {
    is TdApi.SentGiftRegular -> sent.gift.sticker
    is TdApi.SentGiftUpgraded -> sent.gift.model?.sticker
    else -> null
}

@Composable
private fun GiftsDialog(owner: TdApi.MessageSender, all: List<TdApi.ReceivedGift>, own: Boolean, telegram: Telegram, onDismiss: () -> Unit) {
    GridDialog("Gifts", onDismiss = onDismiss) {
        GiftsPane(owner, all, own, telegram, scroll = false)
    }
}

@Composable
fun GiftPreviews(gifts: List<TdApi.ReceivedGift>, telegram: Telegram) {
    androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy((-4).dp)) {
        gifts.take(GiftPreviews).forEach { gift -> StickerView(giftSticker(gift), telegram, 18.dp, animate = false) }
    }
}

@Composable
fun GiftsPane(owner: TdApi.MessageSender, all: List<TdApi.ReceivedGift>, own: Boolean, telegram: Telegram, modifier: Modifier = Modifier, scroll: Boolean = true) {
    var collections by remember(owner) { mutableStateOf<List<TdApi.GiftCollection>>(emptyList()) }
    var selected by remember(owner) { mutableStateOf(0) }
    var shown by remember(owner, all) { mutableStateOf(all) }
    var adding by remember { mutableStateOf(false) }
    var reload by remember { mutableStateOf(0) }
    LaunchedEffect(owner, reload) { telegram.giftCollections(owner) { collections = it } }
    LaunchedEffect(owner, selected, collections, all) {
        val collection = collections.getOrNull(selected - 1)
        if (collection == null) shown = all else telegram.collectionGifts(owner, collection.id) { shown = it }
    }
    Column(
        modifier.then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (own || collections.isNotEmpty()) {
            AlbumTabs(listOf("All Gifts") + collections.map { it.name }, selected, onSelect = { selected = it }, addLabel = if (own) "Add Collection" else null, onAdd = { adding = true })
        }
        if (shown.isEmpty()) {
            Text("No gifts here yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        shown.chunked(GiftColumns).forEach { row ->
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { gift -> GiftCell(gift, telegram, Modifier.weight(1f)) }
                repeat(GiftColumns - row.size) { androidx.compose.foundation.layout.Spacer(Modifier.weight(1f)) }
            }
        }
    }
    if (adding) {
        NameDialog("New collection", "Collection name", onDismiss = { adding = false }) { name, done ->
            telegram.createGiftCollection(owner, name) { error ->
                if (error == null) reload++
                done(error)
            }
        }
    }
}

@Composable
private fun GiftCell(gift: TdApi.ReceivedGift, telegram: Telegram, modifier: Modifier) {
    val upgraded = (gift.gift as? TdApi.SentGiftUpgraded)?.gift
    var open by remember { mutableStateOf(false) }
    val shape = MaterialTheme.shapes.large
    Box(
        modifier
            .aspectRatio(1f)
            .clip(shape)
            .then(if (upgraded != null) Modifier.clickable { open = true } else Modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest)),
        contentAlignment = Alignment.Center,
    ) {
        if (upgraded != null) {
            UniqueGiftArt(upgraded, telegram, Modifier.fillMaxWidth(), shape = shape, animate = false)
        } else {
            StickerView(giftSticker(gift), telegram, 88.dp)
        }
        gift.senderId?.let { sender ->
            com.pixelgram.desktop.ui.components.AvatarImage(
                telegram.senderTitle(sender),
                telegram.senderPhoto(sender),
                telegram,
                androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                24.dp,
                Modifier.align(Alignment.TopStart).padding(6.dp),
            )
        }
        if (upgraded != null) {
            Text(
                "#%,d".format(upgraded.number),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(giftColor(upgraded.backdrop?.colors?.symbolColor ?: 0x333333).copy(alpha = 0.85f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
    if (open && upgraded != null) NftDialog(telegram, upgraded.name) { open = false }
}


private const val GiftPreviews = 3
private const val GiftColumns = 3
