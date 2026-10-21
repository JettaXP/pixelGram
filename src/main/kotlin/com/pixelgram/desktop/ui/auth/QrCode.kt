package com.pixelgram.desktop.ui.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import io.nayuki.qrcodegen.QrCode as Qr

@Composable
fun QrCode(text: String, modifier: Modifier = Modifier) {
    val qr = remember(text) { Qr.encodeText(text, Qr.Ecc.MEDIUM) }
    val colors = MaterialTheme.colorScheme
    Canvas(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(colors.surfaceContainerHighest)
            .padding(16.dp),
    ) {
        val cell = size.minDimension / qr.size
        val radius = CornerRadius(cell * 0.35f)
        for (y in 0 until qr.size) {
            for (x in 0 until qr.size) {
                if (qr.getModule(x, y)) {
                    drawRoundRect(
                        color = colors.onSurface,
                        topLeft = Offset(x * cell, y * cell),
                        size = Size(cell, cell),
                        cornerRadius = radius,
                    )
                }
            }
        }
    }
}
