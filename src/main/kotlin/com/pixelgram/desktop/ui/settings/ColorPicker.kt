package com.pixelgram.desktop.ui.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.materialkolor.hct.Hct
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.filled.Check
import com.pixelgram.desktop.ui.theme.ColorStyle
import com.pixelgram.desktop.ui.theme.isDarkTheme
import com.pixelgram.desktop.ui.theme.schemeFromSeed

@Composable
fun ColorStyleRow(settings: AppSettings) {
    val dark = isDarkTheme(settings)
    val seed = settings.seedColor.value
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
    ) {
        ColorStyle.entries.forEach { style ->
            val scheme = remember(seed, dark, style) { schemeFromSeed(seed, dark, style) }
            val selected = settings.colorStyle.value == style
            val corner by animateDpAsState(if (selected) SelectedCorner else StyleSize / 2, MaterialTheme.motionScheme.defaultSpatialSpec())
            val ring by animateDpAsState(if (selected) 3.dp else 0.dp, MaterialTheme.motionScheme.fastSpatialSpec())
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(StyleSize + 28.dp)) {
                Box(
                    Modifier
                        .size(StyleSize)
                        .then(if (ring > 0.dp) Modifier.border(ring, MaterialTheme.colorScheme.primary, RoundedCornerShape(corner + 4.dp)) else Modifier)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(corner))
                        .clickable(enabled = !settings.israelLogo.value) { settings.colorStyle.value = style },
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val half = Size(size.width / 2, size.height / 2)
                        drawRect(scheme.primaryContainer, Offset.Zero, Size(size.width, half.height))
                        drawRect(scheme.secondary, Offset(0f, half.height), half)
                        drawRect(scheme.tertiary, Offset(half.width, half.height), half)
                    }
                    if (selected) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.Center).size(26.dp)) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(4.dp))
                        }
                    }
                }
                Text(
                    style.title,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
fun HueSlider(settings: AppSettings) {
    val hue = Hct.fromInt(settings.seedColor.value).hue.toFloat()
    val track = remember { Brush.horizontalGradient((0..12).map { Color(Hct.from(it * 30.0, TrackChroma, TrackTone).toInt()) }) }
    val enabled = !settings.israelLogo.value
    BoxWithConstraints(Modifier.fillMaxWidth().height(SliderHeight)) {
        val width = constraints.maxWidth.toFloat()
        val pick = { x: Float ->
            if (enabled) {
                val picked = (x / width).coerceIn(0f, 1f) * 360f
                settings.seedColor.value = Hct.from(picked.toDouble(), SeedChroma, SeedTone).toInt()
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp)
                .clip(CircleShape)
                .background(track)
                .pointerInput(width) {
                    detectTapGestures { pick(it.x) }
                }
                .pointerInput(width) {
                    detectDragGestures { change, _ -> pick(change.position.x) }
                },
        )
        val density = LocalDensity.current
        val x = with(density) { (hue / 360f * width).toDp() }
        Box(
            Modifier
                .offset(x = x - ThumbWidth / 2)
                .width(ThumbWidth)
                .fillMaxSize()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface),
        )
    }
}

private val StyleSize = 64.dp
private val SelectedCorner = 18.dp
private val SliderHeight = 56.dp
private val ThumbWidth = 6.dp
private const val TrackChroma = 60.0
private const val TrackTone = 70.0
private const val SeedChroma = 48.0
private const val SeedTone = 45.0
