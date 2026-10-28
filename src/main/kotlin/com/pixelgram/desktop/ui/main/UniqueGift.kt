package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.components.InfoGroup
import com.pixelgram.desktop.ui.components.InfoRow
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.components.expressive.BasicAlertDialog
import com.pixelgram.desktop.ui.components.expressive.FilledTonalButton
import com.pixelgram.desktop.ui.components.expressive.TextButton
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.outlined.AccountCircle
import com.pixelgram.desktop.ui.icons.outlined.Numbers
import com.pixelgram.desktop.ui.icons.outlined.Palette
import com.pixelgram.desktop.ui.icons.outlined.Payments
import com.pixelgram.desktop.ui.theme.EmojiText
import it.tdlight.jni.TdApi
import kotlin.math.cos
import kotlin.math.sin

private const val PatternRings = 2
private const val PatternPerRing = 8
private const val PatternAlpha = 0.35f
private const val ModelScale = 0.62f
private const val SymbolScale = 0.11f
private const val PatternImageSize = 96

fun giftColor(rgb: Int) = Color(0xFF000000.toInt() or rgb)

fun rarity(rarity: TdApi.UpgradedGiftAttributeRarity?): String? = when (rarity) {
    is TdApi.UpgradedGiftAttributeRarityPerMille -> "%s%%".format(java.math.BigDecimal.valueOf(rarity.perMille.toLong(), 1).stripTrailingZeros().toPlainString())
    is TdApi.UpgradedGiftAttributeRarityUncommon -> "Uncommon"
    is TdApi.UpgradedGiftAttributeRarityRare -> "Rare"
    is TdApi.UpgradedGiftAttributeRarityEpic -> "Epic"
    is TdApi.UpgradedGiftAttributeRarityLegendary -> "Legendary"
    else -> null
}

fun nftLink(gift: TdApi.UpgradedGift) = "https://t.me/nft/${gift.name}"

@Composable
fun UniqueGiftArt(
    gift: TdApi.UpgradedGift,
    telegram: Telegram,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    animate: Boolean = true,
    loopWhile: (() -> Boolean)? = null,
    onEnd: () -> Unit = {},
    raster: Boolean = false,
) {
    val colors = gift.backdrop?.colors
    BoxWithConstraints(
        modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(
                if (colors != null) {
                    Brush.radialGradient(listOf(giftColor(colors.centerColor), giftColor(colors.edgeColor)))
                } else {
                    androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.surfaceContainerHighest)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        val side = minOf(maxWidth, maxHeight)
        val symbol = gift.symbol?.sticker
        val tint = colors?.let { giftColor(it.symbolColor) } ?: MaterialTheme.colorScheme.onSurfaceVariant
        val thumb = symbol?.thumbnail?.file
        if (thumb != null) {
            LaunchedEffect(thumb.id) { telegram.download(thumb) }
            val path = telegram.localPath(thumb)
            val image by androidx.compose.runtime.produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, path) {
                value = path?.let { com.pixelgram.desktop.ui.components.loadImage(it, maxSize = PatternImageSize) }
            }
            val filter = remember(tint) { androidx.compose.ui.graphics.ColorFilter.tint(tint, BlendMode.SrcIn) }
            androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
                val picture = image ?: return@Canvas
                val box = minOf(size.width, size.height)
                val mark = (box * SymbolScale).toInt().coerceAtLeast(1)
                repeat(PatternRings) { ring ->
                    val radius = box * (0.3f + ring * 0.14f)
                    repeat(PatternPerRing) { index ->
                        val angle = (index + ring * 0.5) * 2 * Math.PI / PatternPerRing
                        val x = size.width / 2 + radius * cos(angle).toFloat() - mark / 2f
                        val y = size.height / 2 + radius * sin(angle).toFloat() - mark / 2f
                        drawImage(
                            picture,
                            dstOffset = androidx.compose.ui.unit.IntOffset(x.toInt(), y.toInt()),
                            dstSize = androidx.compose.ui.unit.IntSize(mark, mark),
                            alpha = PatternAlpha,
                            colorFilter = filter,
                        )
                    }
                }
            }
        } else if (symbol != null) {
            val size = side * SymbolScale
            repeat(PatternRings) { ring ->
                val radius = side * (0.3f + ring * 0.14f)
                repeat(PatternPerRing) { index ->
                    val angle = (index + ring * 0.5) * 2 * Math.PI / PatternPerRing
                    Box(
                        Modifier
                            .offset(radius * cos(angle).toFloat(), radius * sin(angle).toFloat())
                            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen, alpha = PatternAlpha)
                            .drawWithContent {
                                drawContent()
                                drawRect(tint, blendMode = BlendMode.SrcIn)
                            },
                    ) {
                        StickerView(symbol, telegram, size, animate = false)
                    }
                }
            }
        }
        StickerView(gift.model?.sticker, telegram, side * ModelScale, animate = animate, onEnd = onEnd, loopWhile = loopWhile, raster = raster)
    }
}

@Composable
fun NftDialog(telegram: Telegram, name: String, received: TdApi.ReceivedGift? = null, onChanged: () -> Unit = {}, onDismiss: () -> Unit) {
    var gift by remember(name) { mutableStateOf<TdApi.UpgradedGift?>(null) }
    var failed by remember(name) { mutableStateOf(false) }
    LaunchedEffect(name) {
        telegram.upgradedGift(name) { result ->
            if (result == null) failed = true else gift = result
        }
    }
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.widthIn(min = 340.dp, max = 440.dp)) {
            Box {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
            ) {
                val current = gift
                when {
                    current != null -> NftContent(current, telegram)
                    failed -> Text("This collectible wasn't found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else -> LoadingIndicator()
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.align(Alignment.End)) {
                    if (current != null) {
                        FilledTonalButton(onClick = {
                            clipboard.setText(AnnotatedString(nftLink(current)))
                            copied = true
                        }) { Text(if (copied) "Link copied" else "Copy link") }
                    }
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
            }
            gift?.let { NftMenu(it, received, telegram, onChanged, onDismiss, Modifier.align(Alignment.TopEnd).padding(8.dp)) }
            }
        }
    }
}

@Composable
private fun NftContent(gift: TdApi.UpgradedGift, telegram: Telegram) {
    UniqueGiftArt(gift, telegram, Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge)
    EmojiText(gift.title, style = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center))
    Text(
        "Collectible #%,d".format(gift.number),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    fun attribute(label: String, value: String?, chance: TdApi.UpgradedGiftAttributeRarity?) = value?.let {
        InfoRow(
            Icons.Outlined.Palette,
            label,
            it,
            trailing = rarity(chance)?.let { text -> { RarityPill(text) } },
        )
    }
    val rows = buildList {
        val owner = gift.ownerId
        when {
            owner != null -> {
                val title = telegram.senderTitle(owner)
                add(
                    InfoRow(
                        Icons.Outlined.AccountCircle,
                        "Owner",
                        title,
                        leading = { com.pixelgram.desktop.ui.components.AvatarImage(title, telegram.senderPhoto(owner), telegram, CircleShape, 32.dp) },
                    ),
                )
            }
            gift.ownerName.isNotBlank() -> add(InfoRow(Icons.Outlined.AccountCircle, "Owner", gift.ownerName))
        }
        attribute("Model", gift.model?.name, gift.model?.rarity)?.let(::add)
        attribute("Backdrop", gift.backdrop?.name, gift.backdrop?.rarity)?.let(::add)
        attribute("Symbol", gift.symbol?.name, gift.symbol?.rarity)?.let(::add)
        val issued = if (gift.maxUpgradedCount > 0) "%,d of %,d issued".format(gift.totalUpgradedCount, gift.maxUpgradedCount) else "%,d issued".format(gift.totalUpgradedCount)
        add(InfoRow(Icons.Outlined.Numbers, "Quantity", issued))
        if (gift.valueAmount > 0 && !gift.valueCurrency.isNullOrBlank()) {
            add(InfoRow(Icons.Outlined.Payments, "Value", formatPrice(gift.valueAmount, gift.valueCurrency)))
        }
    }
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) { InfoGroup(rows) }
}

@Composable
private fun RarityPill(text: String) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
        Text(text, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}
