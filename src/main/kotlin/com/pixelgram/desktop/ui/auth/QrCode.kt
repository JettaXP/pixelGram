package com.pixelgram.desktop.ui.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import io.nayuki.qrcodegen.QrCode as Qr
import kotlin.math.abs

// `center` is drawn over the middle of the code (an avatar); the high error
// correction level keeps the code readable under it.
@Composable
fun QrCode(text: String, modifier: Modifier = Modifier, center: (@Composable () -> Unit)? = null) {
    val qr = remember(text, center != null) {
        Qr.encodeText(text, if (center != null) Qr.Ecc.HIGH else Qr.Ecc.MEDIUM)
    }
    val colors = MaterialTheme.colorScheme
    Box(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(colors.surfaceContainerHighest)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cell = size.minDimension / qr.size
            val radius = CornerRadius(cell * 0.35f)
            // Modules under the center picture are left out instead of covered.
            val hole = if (center != null) (qr.size * CenterFraction / 2 + 0.5f) else 0f
            val middle = qr.size / 2f
            for (y in 0 until qr.size) {
                for (x in 0 until qr.size) {
                    val inHole = abs(x + 0.5f - middle) < hole && abs(y + 0.5f - middle) < hole
                    if (qr.getModule(x, y) && !inHole) {
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
        if (center != null) {
            Box(Modifier.fillMaxSize(CenterFraction), contentAlignment = Alignment.Center) { center() }
        }
    }
}

private const val CenterFraction = 0.24f
