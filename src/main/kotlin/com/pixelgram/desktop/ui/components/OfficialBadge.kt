package com.pixelgram.desktop.ui.components

import com.pixelgram.desktop.ui.icons.filled.AttachMoney
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
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

enum class UserBadge(val userId: Long, val notice: String) {
    Developer(1061520526L, "Developer of pixelGram"),
    Sponsor(1602207467L, "Main sponsor of pixelGram"),
    Tester(7820526891L, "pixelGram tester, hunts bugs in the client");

    val icon: ImageVector
        get() = when (this) {
            Developer -> com.pixelgram.desktop.ui.icons.Icons.Filled.Build
            Sponsor -> com.pixelgram.desktop.ui.icons.Icons.Filled.AttachMoney
            Tester -> BugNet
        }

    companion object {
        fun of(userId: Long): UserBadge? = entries.firstOrNull { it.userId == userId }
    }
}

@Composable
fun UserBadgeView(badge: UserBadge, size: Dp = 18.dp, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(MaterialShapes.Cookie12Sided.toShape())
            .background(scheme.primary)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable { TopNotice.text = badge.notice },
    ) {
        Icon(badge.icon, contentDescription = badge.notice, tint = scheme.onPrimary, modifier = Modifier.size(size * LogoScale))
    }
}

@Composable
fun ChatBadges(chat: com.pixelgram.desktop.telegram.ChatState, telegram: com.pixelgram.desktop.telegram.Telegram, size: Dp) {
    if (telegram.isOfficial(chat)) OfficialBadge(size)
    UserBadge.of(chat.privateUserId)?.let { UserBadgeView(it, size) }
}

private val BugNet: ImageVector by lazy {
    ImageVector.Builder("BugNet", 24.dp, 24.dp, 960f, 960f)
        .addPath(
            androidx.compose.ui.graphics.vector.addPathNodes(
                "M590 120a250 250 0 1 0 0.1 0Z M590 130L590 610 M350 370L830 370 M410 550L140 840"
            ),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 96f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        .build()
}
