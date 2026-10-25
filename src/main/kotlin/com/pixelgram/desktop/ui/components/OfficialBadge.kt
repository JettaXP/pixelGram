package com.pixelgram.desktop.ui.components

import com.pixelgram.desktop.ui.icons.filled.Build
import com.pixelgram.desktop.ui.EasterEgg
import com.pixelgram.desktop.ui.IsraelBlue
import com.pixelgram.desktop.ui.IsraelWhite
import com.pixelgram.desktop.ui.main.TopNotice
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.loadSvgPainter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val OfficialNotice = "This is an official pixelGram resource"
private const val DeveloperNotice = "This is a pixelGram developer"
private const val LogoScale = 0.66f
private const val LogoResource = "/pixelgram-logo.svg"

@Composable
fun rememberPixelgramLogo(): Painter? {
    val density = LocalDensity.current
    return remember(density) {
        OfficialBadgeHolder::class.java.getResourceAsStream(LogoResource)?.use { loadSvgPainter(it, density) }
    }
}

private object OfficialBadgeHolder


@Composable
fun OfficialBadge(size: Dp = 18.dp, modifier: Modifier = Modifier) {
    val logo = rememberPixelgramLogo() ?: return
    val scheme = MaterialTheme.colorScheme
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(MaterialShapes.Cookie12Sided.toShape())
            .background(if (EasterEgg.israel) IsraelWhite else scheme.primary)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable { TopNotice.text = OfficialNotice },
    ) {
        Icon(
            logo,
            contentDescription = "Official pixelGram resource",
            tint = if (EasterEgg.israel) IsraelBlue else scheme.onPrimary,
            modifier = Modifier.size(size * LogoScale),
        )
    }
}

@Composable
fun DeveloperBadge(size: Dp = 18.dp, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(MaterialShapes.Cookie12Sided.toShape())
            .background(scheme.tertiary)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable { TopNotice.text = DeveloperNotice },
    ) {
        Icon(
            com.pixelgram.desktop.ui.icons.Icons.Filled.Build,
            contentDescription = "pixelGram developer",
            tint = scheme.onTertiary,
            modifier = Modifier.size(size * LogoScale),
        )
    }
}

@Composable
fun ChatBadges(chat: com.pixelgram.desktop.telegram.ChatState, telegram: com.pixelgram.desktop.telegram.Telegram, size: Dp) {
    if (telegram.isOfficial(chat)) OfficialBadge(size)
    if (telegram.isDeveloper(chat)) DeveloperBadge(size)
}
