package com.pixelgram.desktop.ui.premium

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.components.expressive.BasicAlertDialog
import com.pixelgram.desktop.ui.components.expressive.Button
import com.pixelgram.desktop.ui.components.expressive.TextButton
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.filled.AddCircle
import com.pixelgram.desktop.ui.icons.filled.CheckCircle
import com.pixelgram.desktop.ui.icons.filled.Star
import com.pixelgram.desktop.ui.icons.outlined.AddReaction
import com.pixelgram.desktop.ui.icons.outlined.Block
import com.pixelgram.desktop.ui.icons.outlined.Celebration
import com.pixelgram.desktop.ui.icons.outlined.Circle
import com.pixelgram.desktop.ui.icons.outlined.Diamond
import com.pixelgram.desktop.ui.icons.outlined.Download
import com.pixelgram.desktop.ui.icons.outlined.EmojiEmotions
import com.pixelgram.desktop.ui.icons.outlined.Face
import com.pixelgram.desktop.ui.icons.outlined.Mic
import com.pixelgram.desktop.ui.icons.outlined.Payments
import com.pixelgram.desktop.ui.icons.outlined.Storage
import com.pixelgram.desktop.ui.icons.outlined.Translate
import com.pixelgram.desktop.ui.icons.outlined.Upgrade
import com.pixelgram.desktop.ui.icons.outlined.Work
import com.pixelgram.desktop.ui.icons.outlined.WorkspacePremium
import com.pixelgram.desktop.ui.main.Links
import it.tdlight.jni.TdApi

val PremiumGradient = listOf(Color(0xFF6B93FF), Color(0xFF976FFF), Color(0xFFE46ACE))
val StarGold = listOf(Color(0xFFFFC93C), Color(0xFFFF9F1C))

fun price(amount: Long, currency: String): String = runCatching {
    val cur = java.util.Currency.getInstance(currency)
    java.text.NumberFormat.getCurrencyInstance().apply { this.currency = cur }.format(amount / Math.pow(10.0, cur.defaultFractionDigits.toDouble()))
}.getOrDefault("$amount $currency")

fun stars(amount: TdApi.StarAmount?): String {
    if (amount == null) return "0"
    val whole = "%,d".format(amount.starCount).replace(',', ' ')
    return if (amount.nanostarCount == 0) whole else "$whole.${amount.nanostarCount / 100_000_000}"
}

private class Feature(val icon: ImageVector, val title: String, val text: String)

private val Features = listOf(
    Feature(Icons.Outlined.Celebration, "Stories", "Unlimited posting, priority order, stealth mode, permanent view history and more."),
    Feature(Icons.Outlined.Storage, "Unlimited cloud storage", "4 GB per document, unlimited storage for your chats and media."),
    Feature(Icons.Outlined.Upgrade, "Doubled limits", "Up to 1000 channels, 30 folders, 10 pins, 20 public links, 4 accounts and more."),
    Feature(Icons.Outlined.Work, "Telegram Business", "Location, opening hours, quick replies and other business features."),
    Feature(Icons.Outlined.Download, "Faster downloads", "No more limits on the speed with which media and documents are downloaded."),
    Feature(Icons.Outlined.Mic, "Voice-to-text", "Read the transcript of any incoming voice message."),
    Feature(Icons.Outlined.Translate, "Real-time translation", "Translate whole chats in real time with one click."),
    Feature(Icons.Outlined.Block, "No ads", "No more ads in public channels."),
    Feature(Icons.Outlined.AddReaction, "Infinite reactions", "React with thousands of emoji, several per message."),
    Feature(Icons.Outlined.EmojiEmotions, "Animated emoji", "Use hundreds of animated emoji from different packs in messages."),
    Feature(Icons.Outlined.Face, "Emoji status", "Choose any emoji to show next to your name."),
    Feature(Icons.Outlined.WorkspacePremium, "Premium badge", "A special badge next to your name showing that you support Telegram."),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GradientMark(icon: ImageVector, colors: List<Color>, size: Dp) {
    Box(
        Modifier.size(size).clip(MaterialShapes.Cookie12Sided.toShape()).background(Brush.linearGradient(colors)),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.56f)) }
}

@Composable
fun PremiumFeatureRows() {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Features.forEachIndexed { index, feature ->
            val shape = when (index) {
                0 -> RoundedCornerShape(20.dp, 20.dp, 4.dp, 4.dp)
                Features.lastIndex -> RoundedCornerShape(4.dp, 4.dp, 20.dp, 20.dp)
                else -> RoundedCornerShape(4.dp)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth().clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(14.dp),
            ) {
                val tone = PremiumGradient[index % PremiumGradient.size]
                Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(tone.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                    Icon(feature.icon, contentDescription = null, tint = tone, modifier = Modifier.size(20.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(feature.title, style = MaterialTheme.typography.titleSmall)
                    Text(feature.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun planName(months: Int): String = when {
    months == 12 -> com.pixelgram.desktop.core.tr("Annual")
    months == 1 -> com.pixelgram.desktop.core.tr("Monthly")
    months % 12 == 0 -> com.pixelgram.desktop.core.tr("{n} years").replace("{n}", (months / 12).toString())
    else -> com.pixelgram.desktop.core.tr("{n} months").replace("{n}", months.toString())
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PremiumSection(telegram: Telegram) {
    var state by remember { mutableStateOf<TdApi.PremiumState?>(null) }
    LaunchedEffect(Unit) { telegram.send(TdApi.GetPremiumState()) { state = it as? TdApi.PremiumState ?: TdApi.PremiumState(TdApi.FormattedText("", emptyArray()), emptyArray(), emptyArray(), emptyArray()) } }
    val premium = telegram.me?.isPremium == true
    val options = state?.paymentOptions.orEmpty().map { it.paymentOption }.sortedByDescending { it.monthCount }
    var picked by remember(options.size) { mutableIntStateOf(0) }
    var paying by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Brush.verticalGradient(listOf(PremiumGradient[1].copy(alpha = 0.35f), Color.Transparent))).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GradientMark(Icons.Filled.Star, PremiumGradient, 96.dp)
            Spacer(Modifier.height(16.dp))
            Text("Telegram Premium", style = MaterialTheme.typography.headlineMediumEmphasized)
            Text(
                if (premium) "You are subscribed to Telegram Premium. Thank you for supporting Telegram!" else "Go beyond the limits and unlock dozens of exclusive features by subscribing to Telegram Premium.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        if (state == null) {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { LoadingIndicator() }
        } else if (!premium && options.isNotEmpty()) {
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Column(Modifier.padding(vertical = 6.dp)) {
                    options.forEachIndexed { index, option ->
                        val perMonth = option.amount / option.monthCount.coerceAtLeast(1)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth().clickable { picked = index }.padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            Icon(if (picked == index) Icons.Filled.CheckCircle else Icons.Outlined.Circle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(planName(option.monthCount), style = MaterialTheme.typography.titleMedium)
                                    if (option.discountPercentage > 0) {
                                        Surface(shape = RoundedCornerShape(6.dp), color = PremiumGradient[1]) {
                                            Text("-${option.discountPercentage}%", color = Color.White, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp))
                                        }
                                    }
                                }
                                if (option.monthCount > 1) Text(price(option.amount, option.currency), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(price(perMonth, option.currency) + com.pixelgram.desktop.core.tr("/month"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            val option = options.getOrNull(picked)
            if (option != null) {
                Button(onClick = { paying = true }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text(
                        com.pixelgram.desktop.core.tr(if (option.monthCount >= 12) "Subscribe for {price} per year" else "Subscribe for {price} per month").replace("{price}", price(option.amount, option.currency)),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
        PremiumFeatureRows()
    }
    if (paying) {
        val option = options.getOrNull(picked)
        val username = telegram.me?.usernames?.activeUsernames?.firstOrNull()
        PayMethodDialog(
            telegram = telegram,
            title = "Telegram Premium",
            link = option?.paymentLink,
            invoice = null,
            fragment = "https://fragment.com/premium/gift" + (username?.let { "?recipient=$it&months=${option?.monthCount ?: 12}" } ?: ""),
            onDismiss = { paying = false },
        )
    }
}

@Composable
private fun PayMethodDialog(telegram: Telegram, title: String, link: TdApi.InternalLinkType?, invoice: TdApi.InputInvoice?, fragment: String, onDismiss: () -> Unit) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.width(420.dp)) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(title, style = MaterialTheme.typography.headlineSmall)
                Text("Choose how to pay", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                PayRow(Icons.Outlined.Payments, "Card", "Telegram's official payment form") {
                    val target = invoice ?: (link as? TdApi.InternalLinkTypeInvoice)?.let { TdApi.InputInvoiceName(it.invoiceName) }
                    if (target != null) {
                        PaymentRequest.current = telegram to target
                    } else if (link != null) {
                        telegram.send(TdApi.GetInternalLink(link, true)) { result -> (result as? TdApi.HttpUrl)?.url?.let(Links::openBrowser) }
                    }
                    onDismiss()
                }
                PayRow(Icons.Outlined.Diamond, "TON on Fragment", "Pay with TON through fragment.com") {
                    Links.openBrowser(fragment)
                    onDismiss()
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = onDismiss) { Text("Cancel") } }
            }
        }
    }
}

@Composable
private fun PayRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest).clickable(onClick = onClick).padding(14.dp),
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StarsSection(telegram: Telegram) {
    val meId = telegram.me?.id ?: 0L
    var tab by remember { mutableIntStateOf(0) }
    var data by remember { mutableStateOf<TdApi.StarTransactions?>(null) }
    var topUp by remember { mutableStateOf(false) }
    LaunchedEffect(tab, PaymentRequest.completed, meId) {
        data = null
        val direction = when (tab) {
            1 -> TdApi.TransactionDirectionIncoming()
            2 -> TdApi.TransactionDirectionOutgoing()
            else -> null
        }
        telegram.send(TdApi.GetStarTransactions(TdApi.MessageSenderUser(meId), "", direction, "", 50)) { data = it as? TdApi.StarTransactions ?: TdApi.StarTransactions(TdApi.StarAmount(0, 0), emptyArray(), "") }
    }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Brush.verticalGradient(listOf(StarGold[0].copy(alpha = 0.3f), Color.Transparent))).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GradientMark(Icons.Filled.Star, StarGold, 96.dp)
            Spacer(Modifier.height(16.dp))
            Text("Telegram Stars", style = MaterialTheme.typography.headlineMediumEmphasized)
            Text("Buy Stars to unlock content and services in bots and mini apps on Telegram.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = StarGold[0], modifier = Modifier.size(32.dp))
                Text(stars(data?.starAmount), style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold))
            }
            Text("your balance", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { topUp = true }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Filled.AddCircle, contentDescription = null)
                Text("Top up", modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.titleMedium)
            }
        }
        SecondaryTabRow(selectedTabIndex = tab, containerColor = Color.Transparent) {
            listOf("All", "Incoming", "Outgoing").forEachIndexed { index, label ->
                Tab(selected = tab == index, onClick = { tab = index }, text = { Text(label) })
            }
        }
        val loaded = data
        when {
            loaded == null -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { LoadingIndicator() }
            loaded.transactions.isEmpty() -> Text("No transactions yet", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(24.dp), textAlign = TextAlign.Center)
            else -> Column { loaded.transactions.forEach { TransactionRow(it, telegram) } }
        }
    }
    if (topUp) TopUpDialog(telegram) { topUp = false }
}

@Composable
private fun TransactionRow(tx: TdApi.StarTransaction, telegram: Telegram) {
    val incoming = tx.starAmount.starCount > 0 || (tx.starAmount.starCount == 0L && tx.starAmount.nanostarCount > 0)
    val (title, subtitle) = describe(tx, telegram)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Box(Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = StarGold[1])
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Text(java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT).format(java.util.Date(tx.date * 1000L)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text((if (incoming) "+" else "") + stars(tx.starAmount), style = MaterialTheme.typography.titleMedium, color = if (incoming) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error)
        Icon(Icons.Filled.Star, contentDescription = null, tint = StarGold[0], modifier = Modifier.size(18.dp))
    }
}

private fun describe(tx: TdApi.StarTransaction, telegram: Telegram): Pair<String, String?> {
    fun user(id: Long) = telegram.userName(id).takeIf { it.isNotBlank() }
    fun t(s: String) = com.pixelgram.desktop.core.tr(s)
    return when (val type = tx.type) {
        is TdApi.StarTransactionTypeFragmentDeposit -> "Fragment" to null
        is TdApi.StarTransactionTypePremiumBotDeposit -> "Premium Bot" to null
        is TdApi.StarTransactionTypeAppStoreDeposit, is TdApi.StarTransactionTypeGooglePlayDeposit -> t("Stars purchase") to null
        is TdApi.StarTransactionTypeUserDeposit -> t("Gift") to user(type.userId)
        is TdApi.StarTransactionTypeGiftPurchase -> t("Gift sent") to null
        is TdApi.StarTransactionTypeGiftSale -> t("Converted gift") to user(type.userId)
        is TdApi.StarTransactionTypeBotInvoicePurchase -> t("Bot purchase") to user(type.userId)
        is TdApi.StarTransactionTypeBotPaidMediaPurchase -> t("Media purchase") to user(type.userId)
        is TdApi.StarTransactionTypeChannelPaidMediaPurchase -> t("Media purchase") to null
        is TdApi.StarTransactionTypeChannelPaidReactionSend -> t("Star reaction") to null
        is TdApi.StarTransactionTypeBotSubscriptionPurchase -> t("Subscription") to user(type.userId)
        else -> t(if (tx.starAmount.starCount >= 0) "Incoming" else "Outgoing") to null
    }
}

@Composable
private fun TopUpDialog(telegram: Telegram, onDismiss: () -> Unit) {
    var options by remember { mutableStateOf<List<TdApi.StarPaymentOption>?>(null) }
    var chosen by remember { mutableStateOf<TdApi.StarPaymentOption?>(null) }
    LaunchedEffect(Unit) { telegram.send(TdApi.GetStarPaymentOptions()) { options = (it as? TdApi.StarPaymentOptions)?.options?.filter { o -> !o.isAdditional }.orEmpty() } }
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.width(420.dp)) {
            Column(Modifier.padding(24.dp).heightIn(max = 560.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Top up", style = MaterialTheme.typography.headlineSmall)
                val list = options
                if (list == null) Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { LoadingIndicator() }
                list?.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest).clickable { chosen = option }.padding(14.dp),
                    ) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = StarGold[0])
                        Text(com.pixelgram.desktop.core.tr("{n} Stars").replace("{n}", option.starCount.toString()), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text(price(option.amount, option.currency), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = onDismiss) { Text("Close") } }
            }
        }
    }
    chosen?.let { option ->
        val username = telegram.me?.usernames?.activeUsernames?.firstOrNull()
        PayMethodDialog(
            telegram = telegram,
            title = com.pixelgram.desktop.core.tr("{n} Stars").replace("{n}", option.starCount.toString()),
            link = null,
            invoice = TdApi.InputInvoiceTelegram(TdApi.TelegramPaymentPurposeStars(option.currency, option.amount, option.starCount, 0)),
            fragment = "https://fragment.com/stars/buy" + (username?.let { "?recipient=$it&quantity=${option.starCount}" } ?: ""),
            onDismiss = {
                chosen = null
                onDismiss()
            },
        )
    }
}

object PaymentRequest {
    var current by mutableStateOf<Pair<Telegram, TdApi.InputInvoice>?>(null)
    var completed by mutableIntStateOf(0)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PaymentOverlay() {
    val (telegram, invoice) = PaymentRequest.current ?: return
    var form by remember(invoice) { mutableStateOf<TdApi.PaymentForm?>(null) }
    var error by remember(invoice) { mutableStateOf<String?>(null) }
    var balance by remember { mutableStateOf<TdApi.StarAmount?>(null) }
    var busy by remember(invoice) { mutableStateOf(false) }
    LaunchedEffect(invoice) {
        telegram.send(TdApi.GetPaymentForm(invoice, null)) { result ->
            when (result) {
                is TdApi.PaymentForm -> form = result
                is TdApi.Error -> error = result.message
            }
        }
        telegram.me?.id?.let { me -> telegram.send(TdApi.GetStarTransactions(TdApi.MessageSenderUser(me), "", null, "", 1)) { balance = (it as? TdApi.StarTransactions)?.starAmount } }
    }
    val close = { PaymentRequest.current = null }
    BasicAlertDialog(onDismissRequest = close) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.width(400.dp)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val loaded = form
                when {
                    error != null -> Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    loaded == null -> LoadingIndicator(Modifier.padding(24.dp))
                    else -> {
                        val type = loaded.type
                        val starsForm = type is TdApi.PaymentFormTypeStars
                        GradientMark(if (starsForm) Icons.Filled.Star else Icons.Outlined.Payments, if (starsForm) StarGold else PremiumGradient, 64.dp)
                        Text(loaded.productInfo.title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                        if (loaded.productInfo.description.text.isNotBlank()) Text(loaded.productInfo.description.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        if (loaded.sellerBotUserId != 0L) Text(com.pixelgram.desktop.core.tr("Sold by {name}").replace("{name}", telegram.userName(loaded.sellerBotUserId)), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        when (type) {
                            is TdApi.PaymentFormTypeStars -> {
                                Text(com.pixelgram.desktop.core.tr("Your balance: ⭐ {n}").replace("{n}", stars(balance)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val enough = (balance?.starCount ?: 0) >= type.starCount
                                Button(
                                    onClick = {
                                        busy = true
                                        telegram.send(TdApi.SendPaymentForm(invoice, loaded.id, "", "", null, 0)) { result ->
                                            busy = false
                                            if (result is TdApi.PaymentResult) {
                                                PaymentRequest.completed++
                                                close()
                                            } else {
                                                error = (result as? TdApi.Error)?.message ?: "Payment failed"
                                            }
                                        }
                                    },
                                    enabled = !busy && enough,
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                ) {
                                    if (busy) LoadingIndicator(Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary)
                                    else Text(com.pixelgram.desktop.core.tr("Pay ⭐ {n}").replace("{n}", type.starCount.toString()), style = MaterialTheme.typography.titleMedium)
                                }
                                if (!enough) Text("Not enough Stars. Top up your balance in Settings → Telegram Stars.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                            }
                            is TdApi.PaymentFormTypeRegular -> {
                                Text(price(type.invoice.priceParts.sumOf { it.amount }, type.invoice.currency), style = MaterialTheme.typography.headlineSmall)
                                Button(
                                    onClick = {
                                        val url = (type.paymentProvider as? TdApi.PaymentProviderOther)?.url
                                        if (url != null) {
                                            Links.openBrowser(url)
                                            close()
                                        } else {
                                            val fallback: TdApi.Function<*>? = when (invoice) {
                                                is TdApi.InputInvoiceName -> TdApi.GetInternalLink(TdApi.InternalLinkTypeInvoice(invoice.name), true)
                                                is TdApi.InputInvoiceMessage -> TdApi.GetMessageLink().apply { chatId = invoice.chatId; messageId = invoice.messageId }
                                                else -> null
                                            }
                                            if (fallback == null) error = "This payment can't be completed in pixelGram yet."
                                            else telegram.send(fallback) { result ->
                                                val link = (result as? TdApi.HttpUrl)?.url ?: (result as? TdApi.MessageLink)?.link
                                                if (link != null) {
                                                    Links.openBrowser(link)
                                                    close()
                                                } else error = "This payment can't be completed in pixelGram yet."
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                ) { Text("Continue to payment", style = MaterialTheme.typography.titleMedium) }
                                Text("The payment page opens in your browser.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            else -> Text("This payment can't be completed in pixelGram yet.", textAlign = TextAlign.Center)
                        }
                    }
                }
                TextButton(onClick = close) { Text("Close") }
            }
        }
    }
}

object EmojiStatusInfo {
    var request by mutableStateOf<Triple<Telegram, Long, Long>?>(null)
    var openPremium by mutableStateOf(false)
}

@Composable
fun EmojiStatusOverlay() {
    val (telegram, userId, emojiId) = EmojiStatusInfo.request ?: return
    var pack by remember(emojiId) { mutableStateOf<String?>(null) }
    LaunchedEffect(emojiId) {
        telegram.send(TdApi.GetCustomEmojiStickers(longArrayOf(emojiId))) { result ->
            val setId = (result as? TdApi.Stickers)?.stickers?.firstOrNull()?.setId ?: 0L
            if (setId != 0L) telegram.send(TdApi.GetStickerSet(setId)) { pack = (it as? TdApi.StickerSet)?.title }
        }
    }
    val close = { EmojiStatusInfo.request = null }
    val name = telegram.userName(userId)
    BasicAlertDialog(onDismissRequest = close) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.width(460.dp)) {
            Column(Modifier.padding(24.dp).heightIn(max = 680.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(110.dp)) { com.pixelgram.desktop.ui.main.CustomEmojiView(emojiId, "⭐", Modifier.size(110.dp)) }
                Text(
                    if (pack != null) com.pixelgram.desktop.core.tr("{name} set this emoji from «{pack}» as their current status.").replace("{name}", name).replace("{pack}", pack.orEmpty())
                    else com.pixelgram.desktop.core.tr("{name} set this emoji as their current status.").replace("{name}", name),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                if (telegram.me?.isPremium != true) {
                    Text("Emoji statuses are a premium feature. Other features included in Telegram Premium:", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    PremiumFeatureRows()
                    Button(
                        onClick = {
                            close()
                            EmojiStatusInfo.openPremium = true
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) { Text("Learn about Telegram Premium", style = MaterialTheme.typography.titleMedium) }
                }
                TextButton(onClick = close) { Text("Close") }
            }
        }
    }
}

@Composable
fun PremiumOverlay() {
    if (!EmojiStatusInfo.openPremium) return
    val telegram = com.pixelgram.desktop.ui.main.Links.active?.invoke() ?: run {
        EmojiStatusInfo.openPremium = false
        return
    }
    BasicAlertDialog(onDismissRequest = { EmojiStatusInfo.openPremium = false }) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surface, modifier = Modifier.width(520.dp)) {
            Column(Modifier.padding(20.dp).heightIn(max = 720.dp).verticalScroll(rememberScrollState())) {
                PremiumSection(telegram)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = { EmojiStatusInfo.openPremium = false }) { Text("Close") } }
            }
        }
    }
}

@Composable
fun EmojiStatusBadge(userId: Long, telegram: Telegram, size: Dp) {
    val status = telegram.users[userId]?.emojiStatus?.type as? TdApi.EmojiStatusTypeCustomEmoji ?: return
    Box(
        Modifier
            .size(size)
            .clickable(interactionSource = null, indication = null) { EmojiStatusInfo.request = Triple(telegram, userId, status.customEmojiId) },
    ) { com.pixelgram.desktop.ui.main.CustomEmojiView(status.customEmojiId, "⭐", Modifier.size(size)) }
}
