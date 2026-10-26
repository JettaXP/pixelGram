package com.pixelgram.desktop.ui.components

import com.pixelgram.desktop.ui.icons.filled.Check
import com.pixelgram.desktop.ui.components.expressive.popIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
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
    var tip by remember { mutableStateOf<String?>(null) }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(MaterialShapes.Cookie12Sided.toShape())
            .background(if (EasterEgg.israel) IsraelWhite else scheme.primary)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable { tip = OfficialNotice },
    ) {
        BadgeTip(tip) { tip = null }
        Icon(
            logo,
            contentDescription = "Official pixelGram resource",
            tint = if (EasterEgg.israel) IsraelBlue else scheme.onPrimary,
            modifier = Modifier.size(size * LogoScale),
        )
    }
}

enum class UserBadge(val userId: Long, val notice: String) {
    MainDeveloper(1780288121L, "Main developer of pixelGram"),
    Developer(1061520526L, "Developer of pixelGram"),
    Sponsor(1602207467L, "Main sponsor of pixelGram"),
    Tester(7820526891L, "pixelGram tester, hunts bugs in the client"),
    Menorah(8315838128L, "Friend of pixelGram");

    val icon: ImageVector
        get() = when (this) {
            MainDeveloper -> CodeSlash
            Developer -> com.pixelgram.desktop.ui.icons.Icons.Filled.Build
            Sponsor -> com.pixelgram.desktop.ui.icons.Icons.Filled.AttachMoney
            Tester -> BugNet
            Menorah -> MenorahIcon
        }

    companion object {
        fun of(userId: Long): UserBadge? = entries.firstOrNull { it.userId == userId }
    }
}

@Composable
fun UserBadgeView(badge: UserBadge, telegram: com.pixelgram.desktop.telegram.Telegram?, size: Dp = 18.dp, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val custom = com.pixelgram.desktop.core.CustomBadges.of(badge.userId)
    val editable = badge.userId in com.pixelgram.desktop.core.CustomBadges.Owners && telegram?.me?.id == badge.userId
    var tip by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf(false) }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .then(if (custom == null) Modifier.clip(MaterialShapes.Cookie12Sided.toShape()).background(scheme.primary) else Modifier)
            .then(if (editable) Modifier.onSecondaryPress { editing = true } else Modifier)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable { tip = custom?.text?.takeIf { it.isNotBlank() } ?: badge.notice },
    ) {
        BadgeTip(tip) { tip = null }
        if (custom != null) {
            com.pixelgram.desktop.ui.main.BadgeEmoji(custom, size)
        } else {
            Icon(badge.icon, contentDescription = badge.notice, tint = scheme.onPrimary, modifier = Modifier.size(size * LogoScale))
        }
        if (editing && telegram != null) {
            com.pixelgram.desktop.ui.main.BadgeEditor(telegram, badge.userId, custom) { editing = false }
        }
    }
}

private fun Modifier.onSecondaryPress(action: () -> Unit): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                event.changes.first().consume()
                action()
            }
        }
    }
}

@Composable
fun VerifiedBadge(size: Dp = 18.dp, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(MaterialShapes.Cookie12Sided.toShape())
            .background(scheme.primary),
    ) {
        Icon(com.pixelgram.desktop.ui.icons.Icons.Filled.Check, contentDescription = "Verified", tint = scheme.onPrimary, modifier = Modifier.size(size * LogoScale))
    }
}

@Composable
fun ChatBadges(chat: com.pixelgram.desktop.telegram.ChatState, telegram: com.pixelgram.desktop.telegram.Telegram, size: Dp) {
    if (telegram.isOfficial(chat)) OfficialBadge(size)
    if (telegram.isVerified(chat)) VerifiedBadge(size)
    UserBadge.of(chat.privateUserId)?.let { UserBadgeView(it, telegram, size) }
}

private val CodeSlash: ImageVector by lazy {
    ImageVector.Builder("CodeSlash", 24.dp, 24.dp, 960f, 960f)
        .addPath(
            androidx.compose.ui.graphics.vector.addPathNodes(
                "M320 300L140 480L320 660 M640 300L820 480L640 660 M550 220L410 740"
            ),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 84f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        .build()
}

private val MenorahIcon: ImageVector by lazy {
    ImageVector.Builder("Menorah", 24.dp, 24.dp, 960f, 960f)
        .addPath(
            androidx.compose.ui.graphics.vector.addPathNodes(
                "M480 330L480 760 M390 330A90 90 0 0 0 570 330 M300 330A180 180 0 0 0 660 330 M210 330A270 270 0 0 0 750 330 M380 790L580 790"
            ),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 52f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        .addPath(
            androidx.compose.ui.graphics.vector.addPathNodes(
                (0 until 7).joinToString(" ") { i ->
                    val x = 210 + i * 90
                    "M${x - 38} 250L${x + 38} 250L${x + 22} 292L${x - 22} 292Z M$x 150Q${x + 30} 196 $x 228Q${x - 30} 196 $x 150Z"
                }
            ),
            fill = SolidColor(Color.Black),
        )
        .build()
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

@Composable
fun BadgeTip(text: String?, onDone: () -> Unit) {
    if (text == null) return
    val shown = remember(text) { androidx.compose.animation.core.MutableTransitionState(false).apply { targetState = true } }
    LaunchedEffect(text) {
        kotlinx.coroutines.delay(TipMs)
        shown.targetState = false
    }
    LaunchedEffect(shown.currentState, shown.isIdle) {
        if (shown.isIdle && !shown.currentState) onDone()
    }
    androidx.compose.ui.window.Popup(
        popupPositionProvider = CenterInWindow,
        onDismissRequest = { shown.targetState = false },
    ) {
        val motion = MaterialTheme.motionScheme
        androidx.compose.animation.AnimatedVisibility(
            visibleState = shown,
            enter = androidx.compose.animation.fadeIn(motion.fastEffectsSpec()) +
                androidx.compose.animation.scaleIn(motion.defaultSpatialSpec(), initialScale = TipScaleFrom),
            exit = androidx.compose.animation.fadeOut(motion.fastEffectsSpec()) +
                androidx.compose.animation.scaleOut(motion.fastSpatialSpec(), targetScale = TipScaleFrom),
        ) {
            androidx.compose.material3.Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                shadowElevation = 6.dp,
            ) {
                com.pixelgram.desktop.ui.components.Text(
                    text,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }
        }
    }
}

private object CenterInWindow : androidx.compose.ui.window.PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: androidx.compose.ui.unit.IntRect,
        windowSize: androidx.compose.ui.unit.IntSize,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        popupContentSize: androidx.compose.ui.unit.IntSize,
    ) = androidx.compose.ui.unit.IntOffset(
        (windowSize.width - popupContentSize.width) / 2,
        (windowSize.height - popupContentSize.height) / 2,
    )
}

private const val TipScaleFrom = 0.8f
private const val TipMs = 2500L
