package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import com.pixelgram.desktop.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import it.tdlight.jni.TdApi

private const val FallbackScale = 0.8f

@Composable
fun CustomEmojiView(id: Long, fallback: String, modifier: Modifier = Modifier.fillMaxSize(), openPack: Boolean = false) = key(id) {
    val telegram = Links.active?.invoke()
    val sticker = telegram?.customEmoji(id)
    val packId = sticker?.setId?.takeIf { openPack && it != 0L }
    val clickable = if (packId != null && telegram != null) {
        Modifier.pointerHoverIcon(androidx.compose.ui.input.pointer.PointerIcon.Hand).clickable(
            interactionSource = null,
            indication = null,
        ) { StickerPack.current = StickerPackRequest(telegram, ActiveChat.id, packId) }
    } else {
        Modifier
    }
    BoxWithConstraints(modifier.then(clickable), contentAlignment = Alignment.Center) {
        if (telegram == null || sticker == null) {
            val size = with(LocalDensity.current) { (maxHeight * FallbackScale).toSp() }
            Text(fallback, fontSize = size, maxLines = 1)
            return@BoxWithConstraints
        }
        val side = minOf(maxWidth, maxHeight)
        val tint = LocalContentColor.current
        val repaint = (sticker.fullType as? TdApi.StickerFullTypeCustomEmoji)?.needsRepainting == true
        Box(
            if (repaint) {
                Modifier
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    .drawWithContent {
                        drawContent()
                        drawRect(tint, blendMode = BlendMode.SrcIn)
                    }
            } else {
                Modifier
            },
        ) {
            StickerView(sticker, telegram, side, animate = com.pixelgram.desktop.core.PowerSaving.emojiInMessages)
        }
    }
}

object ActiveChat {
    @Volatile var id = 0L
}
