package com.pixelgram.desktop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.pixelgram.desktop.core.AppLog
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.core.ApplyUpdate
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.main.EmptyShapes
import com.pixelgram.desktop.ui.theme.PixelTheme
import com.pixelgram.desktop.ui.theme.appColorScheme
import kotlin.concurrent.thread
import kotlin.system.exitProcess

fun runUpdateWindow(job: ApplyUpdate.Job) {
    var done by mutableStateOf(false)
    thread(name = "pixelgram-apply-update") {
        ApplyUpdate.run(job)
        done = true
        Thread.sleep(ForcedExitMs)
        exitProcess(0)
    }
    runCatching {
        application(exitProcessOnExit = false) {
            val settings = remember { AppSettings.load(readOnly = true) }
            LaunchedEffect(done) { if (done) exitApplication() }
            val scheme = appColorScheme(settings)
            val icon = remember(scheme.primaryContainer, scheme.onPrimaryContainer) { AppIcons.painter(scheme) }
            Window(
                onCloseRequest = {},
                title = "pixelGram",
                icon = icon,
                undecorated = true,
                transparent = true,
                resizable = false,
                state = rememberWindowState(position = WindowPosition(Alignment.Center), size = DpSize(WindowWidth, WindowHeight)),
            ) {
                PixelTheme(settings) {
                    WindowDraggableArea { UpdatingCard(spin = settings.interfaceAnimations.value) }
                }
            }
        }
    }.onFailure { AppLog.write("Update window failed", it) }
    while (!done) Thread.sleep(PollMs)
    exitProcess(0)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UpdatingCard(spin: Boolean) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(contentAlignment = Alignment.Center) {
            EmptyShapes(spin = spin, scale = ShapeScale)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.padding(24.dp),
            ) {
                ContainedLoadingIndicator(
                    Modifier.size(IndicatorSize),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    indicatorColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    Text(
                        "pixelGram is updating…",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}

private val WindowWidth = 400.dp
private val WindowHeight = 320.dp
private val IndicatorSize = 104.dp
private const val ShapeScale = 0.6f
private const val PollMs = 100L
private const val ForcedExitMs = 5_000L
