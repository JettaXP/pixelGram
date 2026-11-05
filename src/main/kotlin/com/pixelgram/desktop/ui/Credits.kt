package com.pixelgram.desktop.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.wrapContentHeight
import com.pixelgram.desktop.ui.components.Text
import kotlinx.coroutines.launch

object Credits {
    var open by mutableStateOf(System.getProperty("pixelgram.demo.credits") == "true")
}

private val Sections = listOf(
    "Developers" to listOf("homewatcha", "jettaxp", "Claude", "MrFaDev"),
    "Sponsors" to listOf("oodze"),
    "Testers" to listOf("homewatcha", "xtrvnn"),
    "Standing on the shoulders of" to listOf(
        "telegramdesktop/tdesktop",
        "tdlight-team/tdlight-java",
        "exteraSquad/exteraGram",
        "AyuGram/AyuGramDesktop",
        "MetrolistGroup/Metrolist",
        "LineageOS/android_frameworks_base",
        "MG-Devs-Team/Mategram",
        "YouRooni/plugins",
        "end-4/dots-hyprland",
        "kovidgoyal/kitty",
        "pytgcalls/ntgcalls",
        "hypfvieh/dbus-java",
        "google/material-design-icons",
        "Remix-Design/RemixIcon",
        "480 Design / Solar Icon Set",
    ),
)

@Composable
fun CreditsOverlay() {
    if (!Credits.open) return
    val scope = rememberCoroutineScope()
    val dim = remember { Animatable(0f) }
    val roll = remember { Animatable(0f) }
    var viewport by remember { mutableIntStateOf(0) }
    var content by remember { mutableIntStateOf(0) }
    var closing by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    fun close() {
        if (closing) return
        closing = true
        scope.launch {
            dim.animateTo(0f, tween(CloseMs))
            Credits.open = false
        }
    }
    LaunchedEffect(Unit) {
        focus.requestFocus()
        dim.animateTo(1f, tween(FadeMs))
    }
    LaunchedEffect(viewport, content) {
        if (viewport == 0 || content == 0) return@LaunchedEffect
        roll.snapTo(0f)
        roll.animateTo(1f, tween(((viewport + content) * MsPerPx).toInt(), easing = LinearEasing))
        close()
    }
    androidx.compose.ui.window.Popup(
        popupPositionProvider = WholeWindow,
        properties = androidx.compose.ui.window.PopupProperties(focusable = true),
        onDismissRequest = { close() },
    ) {
    val window = androidx.compose.ui.platform.LocalWindowInfo.current.containerSize
    val density = androidx.compose.ui.platform.LocalDensity.current
    Box(
        Modifier
            .then(with(density) { Modifier.size(window.width.toDp(), window.height.toDp()) })
            .onSizeChanged { viewport = it.height }
            .graphicsLayer { alpha = dim.value }
            .background(Color.Black.copy(alpha = ScrimAlpha))
            .focusRequester(focus)
            .focusable()
            .onPreviewKeyEvent { close(); true }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Press) close()
                        event.changes.forEach { it.consume() }
                    }
                }
            },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(Alignment.Top, unbounded = true)
                .onSizeChanged { content = it.height }
                .graphicsLayer { translationY = viewport - roll.value * (viewport + content) },
        ) {
            Text("pixelGram", style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
            Spacer(Modifier.height(48.dp))
            Sections.forEach { (title, names) ->
                Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.6f), textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                names.forEach { name ->
                    Text(name, style = MaterialTheme.typography.headlineSmall, color = Color.White, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(56.dp))
            }
            Text("Thank you for using pixelGram", style = MaterialTheme.typography.titleLarge, color = Color.White, textAlign = TextAlign.Center)
            Spacer(Modifier.height(96.dp))
            Text("pixelGram © ${java.time.Year.now().value}", style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.8f))
            Text("Free software under the GNU General Public License v3", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.55f), textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Text("Made by JettaX", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
            Text("with Kotlin, Compose Multiplatform and TDLib", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.55f), textAlign = TextAlign.Center)
        }
    }
    }
}

private object WholeWindow : androidx.compose.ui.window.PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: androidx.compose.ui.unit.IntRect,
        windowSize: androidx.compose.ui.unit.IntSize,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        popupContentSize: androidx.compose.ui.unit.IntSize,
    ) = androidx.compose.ui.unit.IntOffset.Zero
}

private const val FadeMs = 700
private const val CloseMs = 220
private const val ScrimAlpha = 1f
private const val MsPerPx = 22f
