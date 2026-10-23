package com.pixelgram.desktop.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import kotlinx.coroutines.delay

private const val PillShowMs = 2_500L
private const val LogoScale = 0.58f
private const val LogoResource = "/pixelgram-logo.svg"

@Composable
fun rememberPixelgramLogo(): Painter? {
    val density = LocalDensity.current
    return remember(density) {
        OfficialBadgeHolder::class.java.getResourceAsStream(LogoResource)?.use { loadSvgPainter(it, density) }
    }
}

private object OfficialBadgeHolder

private object WindowCenter : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ) = IntOffset((windowSize.width - popupContentSize.width) / 2, (windowSize.height - popupContentSize.height) / 2)
}

@Composable
fun OfficialBadge(size: Dp = 18.dp, modifier: Modifier = Modifier) {
    val logo = rememberPixelgramLogo() ?: return
    val pill = remember { MutableTransitionState(false) }
    LaunchedEffect(pill.targetState) {
        if (pill.targetState) {
            delay(PillShowMs)
            pill.targetState = false
        }
    }
    val scheme = MaterialTheme.colorScheme
    Box(modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size)
                .clip(MaterialShapes.Cookie12Sided.toShape())
                .background(scheme.primary)
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable { pill.targetState = !pill.targetState },
        ) {
            Icon(
                logo,
                contentDescription = "Official pixelGram resource",
                tint = scheme.onPrimary,
                modifier = Modifier.size(size * LogoScale),
            )
        }
        if (pill.currentState || pill.targetState) {
            Popup(popupPositionProvider = WindowCenter, onDismissRequest = { pill.targetState = false }) {
                AnimatedVisibility(
                    visibleState = pill,
                    enter = fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()) + scaleIn(MaterialTheme.motionScheme.fastSpatialSpec()),
                    exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()) + scaleOut(MaterialTheme.motionScheme.fastSpatialSpec()),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shadowElevation = 3.dp,
                    ) {
                        Text(
                            "This is an official pixelGram resource",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
}
