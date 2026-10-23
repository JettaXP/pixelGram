package com.pixelgram.desktop.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import com.pixelgram.desktop.ui.components.loadImage
import com.pixelgram.desktop.ui.theme.Emoji3D
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.main.StickerView
import com.pixelgram.desktop.ui.theme.EmojiText
import it.tdlight.jni.TdApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val Flag = "🇮🇱"
private const val Message = "תהילה לנתניהו"
private const val TapsNeeded = 5
private const val TapWindowMs = 500L
private const val FlagHoldMs = 2_400L
private const val NoticeHoldMs = 3_200L
private const val SwayDegrees = 8f
private val FlagSize = 220.dp

object EasterEgg {
    var shown by mutableIntStateOf(0)
        private set
    var sticker by mutableStateOf<TdApi.Sticker?>(null)
        private set
    var telegram: Telegram? = null
        private set
    private val prefetch = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var taps = 0
    private var lastTap = 0L

    fun tap(telegram: Telegram) {
        val now = System.currentTimeMillis()
        taps = if (now - lastTap <= TapWindowMs) taps + 1 else 1
        lastTap = now
        if (taps == 1) prefetch.launch { Emoji3D.file(Flag) }
        if (taps < TapsNeeded) return
        taps = 0
        this.telegram = telegram
        telegram.animatedEmoji(Flag) { found -> sticker = found }
        shown++
    }
}

@Composable
fun BoxScope.EasterEggOverlay() {
    var flagVisible by remember { mutableStateOf(false) }
    var noticeVisible by remember { mutableStateOf(false) }
    val sway = remember { Animatable(0f) }
    LaunchedEffect(EasterEgg.shown) {
        if (EasterEgg.shown == 0) return@LaunchedEffect
        flagVisible = true
        noticeVisible = true
        launch {
            repeat(3) {
                sway.animateTo(SwayDegrees, tween(300))
                sway.animateTo(-SwayDegrees, tween(300))
            }
            sway.animateTo(0f, spring(Spring.DampingRatioMediumBouncy))
        }
        launch {
            delay(NoticeHoldMs)
            noticeVisible = false
        }
        delay(FlagHoldMs)
        flagVisible = false
    }
    AnimatedVisibility(
        visible = flagVisible,
        enter = scaleIn(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), initialScale = 0.2f) + fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()),
        exit = fadeOut(MaterialTheme.motionScheme.slowEffectsSpec()) + scaleOut(MaterialTheme.motionScheme.slowSpatialSpec(), targetScale = 1.3f),
        modifier = Modifier.align(Alignment.Center),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.graphicsLayer {
                rotationZ = sway.value
                rotationY = sway.value * 2
                cameraDistance = 12f * density
            },
        ) {
            val image by produceState<ImageBitmap?>(null) {
                value = Emoji3D.file(Flag)?.let { loadImage(it.absolutePath) }
            }
            val sticker = EasterEgg.sticker
            val telegram = EasterEgg.telegram
            val flag = image
            if (flag != null) {
                Image(flag, contentDescription = Flag, modifier = Modifier.size(FlagSize))
            } else if (sticker != null && telegram != null) {
                StickerView(sticker, telegram, FlagSize)
            } else {
                EmojiText(Flag, fontSize = 160.sp)
            }
        }
    }
    AnimatedVisibility(
        visible = noticeVisible,
        enter = slideInVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) { it * 2 } + fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
        exit = slideOutVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) { it * 2 } + fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 6.dp,
        ) {
            Text(
                Message,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            )
        }
    }
}
