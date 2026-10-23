package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import it.tdlight.jni.TdApi

private const val FallbackScale = 0.8f

@Composable
fun CustomEmojiView(id: Long, fallback: String, modifier: Modifier = Modifier.fillMaxSize()) {
    val telegram = Links.active?.invoke()
    val sticker = telegram?.customEmoji(id)
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
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
