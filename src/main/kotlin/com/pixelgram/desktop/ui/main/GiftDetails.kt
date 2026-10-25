package com.pixelgram.desktop.ui.main

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

fun stars(count: Long) = if (count == 1L) "1 Star" else "$count Stars"

@Composable
fun GiftDetailsDialog(message: UiMessage, telegram: Telegram, onDismiss: () -> Unit) {
    val content = message.content
    val date = Instant.ofEpochSecond(message.date.toLong()).atZone(ZoneId.systemDefault()).format(DateFormat)
    val rows = buildList {
        when (content) {
            is TdApi.MessageGift -> {
                content.senderId?.let { add(InfoRow(Icons.Outlined.AccountCircle, "From", telegram.senderTitle(it))) }
                content.receiverId?.let { add(InfoRow(Icons.Outlined.AccountCircle, "To", telegram.senderTitle(it))) }
                add(InfoRow(Icons.Outlined.CalendarMonth, "Date", date))
                add(InfoRow(Icons.Outlined.Star, "Price", stars(content.gift.starCount)))
                if (content.sellStarCount > 0) add(InfoRow(Icons.Outlined.Payments, "Can be converted to", stars(content.sellStarCount)))
                if (content.gift.upgradeStarCount > 0) add(InfoRow(Icons.Outlined.Upgrade, "Upgrade", stars(content.gift.upgradeStarCount)))
                val status = when {
                    content.wasRefunded -> "Refunded"
                    content.wasUpgraded -> "Upgraded to a collectible"
                    content.wasConverted -> "Converted to Stars"
                    content.isSaved -> "Shown on the profile"
                    else -> "Not shown on the profile"
                }
                add(InfoRow(Icons.Outlined.Info, "Status", status))
                if (content.isPrivate) add(InfoRow(Icons.Outlined.Info, "Sender", "Hidden from others"))
            }
            is TdApi.MessageUpgradedGift -> {
                val gift = content.gift
                content.senderId?.let { add(InfoRow(Icons.Outlined.AccountCircle, "From", telegram.senderTitle(it))) }
                gift.ownerId?.let { add(InfoRow(Icons.Outlined.AccountCircle, "Owner", telegram.senderTitle(it))) }
                    ?: gift.ownerName.takeIf { it.isNotBlank() }?.let { add(InfoRow(Icons.Outlined.AccountCircle, "Owner", it)) }
                add(InfoRow(Icons.Outlined.Numbers, "Number", "#${gift.number} of ${gift.totalUpgradedCount}"))
                gift.model?.let { add(InfoRow(Icons.Outlined.Palette, "Model", it.name)) }
                gift.symbol?.let { add(InfoRow(Icons.Outlined.Palette, "Symbol", it.name)) }
                gift.backdrop?.let { add(InfoRow(Icons.Outlined.Palette, "Backdrop", it.name)) }
                if (gift.valueAmount > 0 && !gift.valueCurrency.isNullOrBlank()) add(InfoRow(Icons.Outlined.Payments, "Value", formatPrice(gift.valueAmount, gift.valueCurrency)))
                add(InfoRow(Icons.Outlined.CalendarMonth, "Date", date))
            }
            is TdApi.MessageGiftedStars -> {
                add(InfoRow(Icons.Outlined.AccountCircle, "From", telegram.userTitle(content.gifterUserId)))
                add(InfoRow(Icons.Outlined.AccountCircle, "To", telegram.userTitle(content.receiverUserId)))
                add(InfoRow(Icons.Outlined.Star, "Stars", content.starCount.toString()))
                paidPrice(content.currency, content.amount, content.cryptocurrency, content.cryptocurrencyAmount)?.let { add(InfoRow(Icons.Outlined.Payments, "Paid", it)) }
                add(InfoRow(Icons.Outlined.CalendarMonth, "Date", date))
                content.transactionId?.takeIf { it.isNotBlank() }?.let { add(InfoRow(Icons.Outlined.Numbers, "Transaction", it)) }
            }
            is TdApi.MessageGiftedPremium -> {
                add(InfoRow(Icons.Outlined.AccountCircle, "From", telegram.userTitle(content.gifterUserId)))
                add(InfoRow(Icons.Outlined.AccountCircle, "To", telegram.userTitle(content.receiverUserId)))
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
