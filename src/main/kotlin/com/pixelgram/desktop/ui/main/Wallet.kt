package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.tr
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.components.expressive.BasicAlertDialog
import com.pixelgram.desktop.ui.components.expressive.Button
import com.pixelgram.desktop.ui.components.expressive.FilledTonalButton
import com.pixelgram.desktop.ui.components.expressive.IconButton
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.filled.ArrowDownward
import com.pixelgram.desktop.ui.icons.filled.Close
import com.pixelgram.desktop.ui.icons.outlined.ContentCopy
import com.pixelgram.desktop.ui.icons.outlined.Diamond
import com.pixelgram.desktop.ui.icons.outlined.KeyboardArrowDown
import androidx.compose.ui.draw.rotate
import com.pixelgram.desktop.ui.icons.outlined.Send
import com.pixelgram.desktop.ui.theme.EmojiText
import it.tdlight.jni.TdApi

object WalletWindow {
    var open by mutableStateOf<Telegram?>(null)
}

fun gramText(nano: Long, signed: Boolean = false): String {
    val sign = if (signed && nano > 0) "+" else if (nano < 0) "−" else ""
    val abs = kotlin.math.abs(nano)
    val whole = abs / NanoPerGram
    val fraction = (abs % NanoPerGram).toString().padStart(9, '0').trimEnd('0')
    return sign + whole + if (fraction.isEmpty()) "" else ",$fraction"
}

fun shortAddress(address: String): String =
    if (address.length > 12) address.take(6) + "…" + address.takeLast(6) else address

@Composable
fun WalletHost() {
    val telegram = WalletWindow.open ?: return
    WalletDialog(telegram) { WalletWindow.open = null }
}

@Composable
private fun WalletDialog(telegram: Telegram, onDismiss: () -> Unit) {
    val state = telegram.tonWallet
    var tab by remember { mutableIntStateOf(0) }
    var receiving by remember { mutableStateOf(false) }
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.width(DialogWidth)) {
            Column(Modifier.heightIn(max = DialogHeight)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 8.dp, top = 12.dp)) {
                    Text("Money", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Close") }
                }
                if (state == null || state.address.isBlank()) {
                    Text(
                        if (state?.isBeingCreated == true) "Setting up your wallet…" else "Wallet is not available for this account.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                    )
                    return@Column
                }
                WalletCard(state, telegram, Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) { receiving = true }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Button(onClick = { receiving = true }, modifier = Modifier.weight(1f).height(48.dp)) {
                        Icon(Icons.Filled.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Add Funds")
                    }
                    FilledTonalButton(onClick = {}, enabled = false, modifier = Modifier.weight(1f).height(48.dp)) {
                        Icon(Icons.Outlined.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Send")
                    }
                }
                SecondaryTabRow(selectedTabIndex = tab, containerColor = Color.Transparent, modifier = Modifier.padding(top = 12.dp)) {
                    listOf("Transactions", "Collectibles").forEachIndexed { index, title ->
                        Tab(selected = tab == index, onClick = { tab = index }, text = { Text(title) })
                    }
                }
                Box(Modifier.weight(1f, fill = false)) {
                    if (tab == 0) WalletTransactions(telegram) else WalletCollectibles(telegram)
                }
            }
        }
    }
    if (receiving && state != null) ReceiveDialog(state.address) { receiving = false }
}

@Composable
private fun WalletCard(state: TdApi.TonWalletState, telegram: Telegram, modifier: Modifier, onQr: () -> Unit) {
    val me = telegram.me
    val name = me?.let { listOf(it.firstName, it.lastName).filter(String::isNotBlank).joinToString(" ") }.orEmpty()
    GramCard(state.address, state.gramAmount, name, modifier, onQr)
}

@Composable
private fun GramCard(address: String, nano: Long, owner: String, modifier: Modifier, onQr: (() -> Unit)?) {
    val colors = MaterialTheme.colorScheme
    val tiltX = remember { androidx.compose.animation.core.Animatable(0f) }
    val tiltY = remember { androidx.compose.animation.core.Animatable(0f) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val spring = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    Box(
        modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        scope.launch { tiltX.animateTo(0f, spring) }
                        scope.launch { tiltY.animateTo(0f, spring) }
                    },
                    onDragCancel = {
                        scope.launch { tiltX.animateTo(0f, spring) }
                        scope.launch { tiltY.animateTo(0f, spring) }
                    },
                ) { change, amount ->
                    change.consume()
                    scope.launch { tiltY.snapTo((tiltY.value + amount.x * TiltPerPx).coerceIn(-MaxTilt, MaxTilt)) }
                    scope.launch { tiltX.snapTo((tiltX.value - amount.y * TiltPerPx).coerceIn(-MaxTilt, MaxTilt)) }
                }
            }
            .graphicsLayer {
                rotationX = tiltX.value
                rotationY = tiltY.value
                cameraDistance = 12f * density
            }
            .aspectRatio(CardRatio)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(colors.primary, colors.tertiary))),
    ) {
        Text(
            address.chunked(4).joinToString(" "),
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = colors.onPrimary.copy(alpha = 0.55f),
            maxLines = 3,
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).width(110.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.align(Alignment.CenterStart).fillMaxWidth().padding(horizontal = 20.dp),
        ) {
            val amount = gramText(nano)
            val big = amount.length <= BigAmountLength
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.Diamond, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(if (big) 28.dp else 22.dp))
                Text(
                    amount,
                    style = (if (big) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineMedium).copy(fontWeight = FontWeight.Medium),
                    color = colors.onPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Text("GRAM", style = if (big) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium, color = colors.onPrimary.copy(alpha = 0.7f), maxLines = 1)
            }
            if (onQr != null) {
                Surface(
                    onClick = onQr,
                    shape = RoundedCornerShape(16.dp),
                    color = colors.onPrimary.copy(alpha = 0.2f),
                    modifier = Modifier.size(56.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) { Text("QR", style = MaterialTheme.typography.titleMedium, color = colors.onPrimary) }
                }
            }
        }
        if (owner.isNotBlank()) {
            EmojiText(
                owner.uppercase(),
                style = MaterialTheme.typography.titleSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                color = colors.onPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart).padding(20.dp),
            )
        }
    }
}

@Composable
private fun WalletTransactions(telegram: Telegram) {
    val items = remember { mutableStateListOf<TdApi.TonWalletTransaction>() }
    var next by remember { mutableStateOf<String?>("") }
    var loading by remember { mutableStateOf(false) }
    fun more() {
        val offset = next ?: return
        if (loading) return
        loading = true
        telegram.tonTransactions(offset) { result ->
            loading = false
            items.addAll(result?.transactions.orEmpty())
            next = result?.nextOffset?.takeIf { it.isNotEmpty() && result.transactions.isNotEmpty() }
        }
    }
    LaunchedEffect(Unit) { more() }
    if (items.isEmpty()) {
        Text(
            if (loading) "Looking for transactions" else "No transactions yet",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(32.dp),
        )
        return
    }
    LazyColumn(Modifier.fillMaxWidth().heightIn(max = ListHeight), contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp)) {
        items(items, key = { it.id }) { transaction ->
            TransactionRow(transaction, telegram)
            if (transaction === items.last()) LaunchedEffect(transaction.id) { more() }
        }
    }
}

@Composable
private fun TransactionRow(transaction: TdApi.TonWalletTransaction, telegram: Telegram) {
    val colors = MaterialTheme.colorScheme
    val type = transaction.type
    val amount = when (type) {
        is TdApi.TonWalletTransactionTypeTransfer -> type.amount
        is TdApi.TonWalletTransactionTypeOnRampDeposit -> type.amount
        else -> 0L
    }
    val incoming = when (type) {
        is TdApi.TonWalletTransactionTypeNftTransfer -> !type.isOutgoing
        else -> amount >= 0
    }
    val peer = when {
        transaction.peerUserId != 0L -> telegram.userName(transaction.peerUserId)
        transaction.peerDomain.isNotBlank() -> transaction.peerDomain
        type is TdApi.TonWalletTransactionTypeOnRampDeposit -> type.providerName
        else -> shortAddress(transaction.peerAddress)
    }
    val note = when (type) {
        is TdApi.TonWalletTransactionTypeTransfer -> type.comment.takeIf { !type.isCommentEncrypted }
        is TdApi.TonWalletTransactionTypeNftTransfer -> type.nft.name.ifBlank { tr("Collectible") }
        is TdApi.TonWalletTransactionTypeKeyChange -> tr("Key changed")
        else -> null
    }
    val failed = transaction.state is TdApi.TonWalletTransactionStateFailed
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(if (incoming) colors.primaryContainer else colors.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (incoming) Icons.Filled.ArrowDownward else Icons.Outlined.Send,
                contentDescription = null,
                tint = if (incoming) colors.onPrimaryContainer else colors.onSecondaryContainer,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            EmojiText(peer, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!note.isNullOrBlank()) EmojiText(note, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                java.time.Instant.ofEpochSecond(transaction.date.toLong()).atZone(java.time.ZoneId.systemDefault())
                    .format(java.time.format.DateTimeFormatter.ofPattern("d MMM, HH:mm", java.util.Locale.forLanguageTag(com.pixelgram.desktop.core.I18n.code))),
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
            )
        }
        Text(
            when {
                failed -> tr("Failed")
                type is TdApi.TonWalletTransactionTypeNftTransfer -> if (incoming) "+1" else "−1"
                else -> gramText(amount, signed = true)
            },
            style = MaterialTheme.typography.titleSmall,
            color = when {
                failed -> colors.error
                incoming -> colors.primary
                else -> colors.onSurface
            },
        )
    }
}

@Composable
private fun WalletCollectibles(telegram: Telegram) {
    val items = remember { mutableStateListOf<TdApi.TonNft>() }
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        telegram.tonNfts("") { result ->
            items.addAll(result?.nfts.orEmpty())
            loaded = true
        }
    }
    if (items.isEmpty()) {
        Text(
            if (loaded) "No collectibles yet" else "Loading…",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(32.dp),
        )
        return
    }
    LazyVerticalGrid(GridCells.Adaptive(110.dp), Modifier.fillMaxWidth().heightIn(max = ListHeight), contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
        items(items, key = { it.address }) { nft ->
            Column(Modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
                    val image = (nft.thumbnail ?: nft.image)?.photo
                    if (nft.sticker != null) StickerView(nft.sticker, telegram, 100.dp)
                    else ThumbImage(image, null, telegram, Modifier.fillMaxWidth().aspectRatio(1f))
                }
                EmojiText(nft.name, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun ReceiveDialog(address: String, onDismiss: () -> Unit) {
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    com.pixelgram.desktop.ui.components.expressive.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Funds") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Text("Share your address or QR code to receive GRAM.", textAlign = TextAlign.Center)
                com.pixelgram.desktop.ui.auth.QrCode("ton://transfer/$address", Modifier.size(200.dp))
                Text(address, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), textAlign = TextAlign.Center)
            }
        },
        confirmButton = {
            com.pixelgram.desktop.ui.components.expressive.TextButton(onClick = {
                clipboard.setText(androidx.compose.ui.text.AnnotatedString(address))
                copied = true
            }) { Text(if (copied) "Copied" else "Copy address") }
        },
        dismissButton = { com.pixelgram.desktop.ui.components.expressive.TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

class UserWallet(val address: String, val nano: Long)

@Composable
fun ProfileBalance(userId: Long, telegram: Telegram, modifier: Modifier = Modifier) {
    val own = userId == telegram.me?.id
    var other by remember(userId) { mutableStateOf<UserWallet?>(null) }
    var open by remember(userId) { mutableStateOf(false) }
    LaunchedEffect(userId, own) {
        if (own) return@LaunchedEffect
        telegram.send(TdApi.GetUserTonWalletAddresses(longArrayOf(userId))) { result ->
            val address = (result as? TdApi.UserTonWalletAddresses)?.addresses?.firstOrNull { it.userId == userId }?.walletAddress
                ?: (result as? TdApi.UserTonWalletAddresses)?.addresses?.firstOrNull()?.walletAddress
                ?: return@send
            telegram.send(TdApi.SendTonCenterApiRequest("/api/v2/getAddressBalance", TdApi.TonCenterApiRequestTypeGet("address=$address"))) { reply ->
                val json = (reply as? TdApi.Text)?.text ?: return@send
                val nano = Regex(""""result"\s*:\s*"?(\d+)""").find(json)?.groupValues?.get(1)?.toLongOrNull() ?: return@send
                other = UserWallet(address, nano)
            }
        }
    }
    val wallet = if (own) telegram.tonWallet?.takeIf { it.address.isNotBlank() }?.let { UserWallet(it.address, it.gramAmount) } else other
    if (wallet == null) return
    Surface(
        onClick = { open = true },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        ) {
            Text("Balance:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Icon(Icons.Outlined.Diamond, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Text(gramText(wallet.nano), style = MaterialTheme.typography.labelLarge)
            Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp).rotate(-90f))
        }
    }
    if (open) BalanceCardDialog(userId, wallet, own, telegram) { open = false }
}

@Composable
private fun BalanceCardDialog(userId: Long, wallet: UserWallet, own: Boolean, telegram: Telegram, onDismiss: () -> Unit) {
    val name = telegram.userName(userId)
    var how by remember { mutableStateOf(false) }
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.width(DialogWidth)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(24.dp)) {
                val grams = gramText(wallet.nano)
                Text(
                    when {
                        own -> tr("Your balance is public due to blockchain transparency.")
                        wallet.nano > 0 -> tr("{name} owns {amount} Grams on the TON blockchain.").replace("{name}", name).replace("{amount}", grams)
                        else -> tr("{name} doesn't own any Grams yet.").replace("{name}", name)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                GramCard(wallet.address, wallet.nano, if (own) name else name, Modifier, null)
                val mine = telegram.tonWallet?.address?.isNotBlank() == true
                when {
                    own -> Button(onClick = { onDismiss(); WalletWindow.open = telegram }, modifier = Modifier.height(48.dp)) { Text("Open My Wallet") }
                    !mine -> FilledTonalButton(onClick = { how = true }, modifier = Modifier.height(48.dp)) { Text("How does it work?") }
                }
            }
        }
    }
    if (how) HowItWorksDialog { how = false }
}

@Composable
private fun HowItWorksDialog(onDismiss: () -> Unit) {
    com.pixelgram.desktop.ui.components.expressive.AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Diamond, contentDescription = null) },
        title = { Text("How It Works") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(
                    "Send Money Instantly" to "Send Grams in any chat, just like sharing a photo.",
                    "No Fees" to "First transfers each day are free, the rest cost almost nothing.",
                    "Blockchain Verified" to "All transactions are recorded and verifiable on a public ledger.",
                ).forEach { (title, text) ->
                    Column {
                        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        Text(text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = { com.pixelgram.desktop.ui.components.expressive.TextButton(onClick = onDismiss) { Text("OK") } },
    )
}

private const val NanoPerGram = 1_000_000_000L
private const val CardRatio = 1.75f
private const val BigAmountLength = 7
private const val TiltPerPx = 0.25f
private const val MaxTilt = 18f
private val DialogWidth = 420.dp
private val DialogHeight = 760.dp
private val ListHeight = 320.dp
