package com.pixelgram.desktop.ui.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.telegram.ChatState
import com.pixelgram.desktop.telegram.Telegram
import it.tdlight.jni.TdApi

class ProfileLook(val colors: List<Color>?, val pattern: Long)

@Composable
fun profileLook(chat: ChatState, telegram: Telegram): ProfileLook {
    val user = telegram.users[chat.privateUserId]
    val raw = if (user != null) user.profileAccentColorId to user.profileBackgroundCustomEmojiId else -1 to 0L
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val accent = telegram.profileAccents[raw.first]
    val values = accent?.let { if (dark) it.darkThemeColors else it.lightThemeColors }?.backgroundColors
    val colors = values?.map { Color(0xFF000000.toInt() or it) }?.takeIf { it.isNotEmpty() }
    return ProfileLook(colors, raw.second)
}

@Composable
fun ProfileBackdrop(look: ProfileLook, avatarCenter: Dp, avatarSize: Dp, height: Dp, modifier: Modifier = Modifier) {
    if (look.colors == null && look.pattern == 0L) return
    val surface = MaterialTheme.colorScheme.surfaceContainerHigh
    val density = LocalDensity.current
    BoxWithConstraints(modifier.fillMaxWidth().height(height)) {
        val width = maxWidth
        look.colors?.let { colors ->
            Box(
                Modifier.matchParentSize().background(
                    Brush.radialGradient(
                        listOf(colors.first(), colors.last()),
                        center = with(density) { Offset((width / 2).toPx(), avatarCenter.toPx()) },
                        radius = with(density) { (width * RadiusShare).toPx() },
                    ),
                ),
            )
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(FadeStart to Color.Transparent, 1f to surface)))
        }
        if (look.pattern != 0L) {
            val tint = if (look.colors != null) Color.White else MaterialTheme.colorScheme.primary
            PatternRings.forEach { ring ->
                ring.angles.forEach { angle ->
                    val rad = Math.toRadians(angle.toDouble())
                    val r = avatarSize / 2 + ring.distance
                    Box(
                        Modifier
                            .align(Alignment.TopCenter)
                            .offset(x = r * kotlin.math.cos(rad).toFloat(), y = avatarCenter + r * kotlin.math.sin(rad).toFloat() - ring.size / 2)
                            .size(ring.size)
                            .graphicsLayer {
                                alpha = ring.alpha
                                compositingStrategy = CompositingStrategy.Offscreen
                            }
                            .drawWithContent {
                                drawContent()
                                drawRect(tint, blendMode = BlendMode.SrcIn)
                            },
                    ) { CustomEmojiView(look.pattern, "") }
                }
            }
        }
    }
}

@Composable
fun PinnedGiftsOrbit(gifts: List<TdApi.UpgradedGift>, telegram: Telegram, avatarSize: Dp, modifier: Modifier = Modifier) {
    if (gifts.isEmpty()) return
    val box = avatarSize + OrbitPad * 2
    BoxWithConstraints(modifier.fillMaxWidth().height(box)) {
        val cx = maxWidth / 2
        val half = avatarSize / 2
        val spots = listOf(
            (cx - half - 30.dp) to 10.dp,
            (cx + half + 30.dp) to 10.dp,
            (cx - half - 52.dp) to (box / 2),
            (cx + half + 52.dp) to (box / 2),
            (cx - half - 30.dp) to (box - 10.dp),
            (cx + half + 30.dp) to (box - 10.dp),
        )
        gifts.take(spots.size).forEachIndexed { i, gift ->
            val (x, y) = spots[i]
            val glow = gift.backdrop?.colors?.let { Color(0xFF000000.toInt() or it.centerColor) } ?: MaterialTheme.colorScheme.primary
            val enter = remember(gift.name) { Animatable(0f) }
            LaunchedEffect(gift.name) {
                kotlinx.coroutines.delay(i * StaggerMs)
                enter.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 300f))
            }
            Box(
                Modifier
                    .offset(x = x - OrbitSize / 2, y = y - OrbitSize / 2)
                    .size(OrbitSize)
                    .graphicsLayer {
                        val v = enter.value
                        alpha = v.coerceIn(0f, 1f)
                        val scale = 0.5f + 0.5f * v
                        scaleX = scale
                        scaleY = scale
                    }
                    .background(Brush.radialGradient(listOf(glow, glow.copy(alpha = 0f))), CircleShape)
                    .clip(CircleShape)
                    .clickable { Links.open("https://t.me/nft/${gift.name}") },
                contentAlignment = Alignment.Center,
            ) {
                StickerView(gift.model?.sticker, telegram, 26.dp, animate = false)
            }
        }
    }
}

fun Telegram.pinnedUpgradedGifts(userId: Long, onResult: (List<TdApi.UpgradedGift>) -> Unit) {
    profileGifts(TdApi.MessageSenderUser(userId)) { _, list ->
        onResult(list.filter { it.isPinned }.mapNotNull { (it.gift as? TdApi.SentGiftUpgraded)?.gift }.take(6))
    }
}

private class PatternRing(val distance: Dp, val size: Dp, val alpha: Float, val angles: List<Float>)

private val PatternRings = listOf(
    PatternRing(30.dp, 20.dp, 0.5f, listOf(0f, 60f, 120f, 180f, 240f, 300f)),
    PatternRing(64.dp, 16.dp, 0.32f, listOf(30f, 75f, 105f, 150f, 210f, 255f, 285f, 330f)),
    PatternRing(100.dp, 13.dp, 0.18f, listOf(0f, 40f, 140f, 180f, 220f, 320f)),
)

private const val RadiusShare = 1.1f
private const val FadeStart = 0.6f
private const val StaggerMs = 60L
private val OrbitSize = 44.dp
private val OrbitPad = 24.dp
