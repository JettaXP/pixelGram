package com.pixelgram.desktop

import androidx.compose.animation.AnimatedContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.telegram.AuthState
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.auth.AuthScreen
import com.pixelgram.desktop.ui.main.MainScreen
import com.pixelgram.desktop.ui.settings.SettingsScreen
import com.pixelgram.desktop.ui.theme.PixelTheme
import kotlinx.coroutines.MainScope

fun main() = application {
    val settings = remember { AppSettings.load() }
    val telegram = remember { Telegram(settings, MainScope()) }
    LaunchedEffect(Unit) { telegram.start() }

    Window(
        onCloseRequest = {
            telegram.close()
            exitApplication()
        },
        title = "pixelGram",
        state = rememberWindowState(size = DpSize(1180.dp, 760.dp)),
    ) {
        window.minimumSize = java.awt.Dimension(800, 520)
        PixelTheme(settings) {
            App(telegram, settings)
        }
    }
}

@Composable
private fun App(telegram: Telegram, settings: AppSettings) {
    var showSettings by remember { mutableStateOf(false) }
    val ready = telegram.authState == AuthState.Ready
    AnimatedContent(targetState = ready to showSettings) { (isReady, inSettings) ->
        when {
            !isReady -> AuthScreen(telegram)
            inSettings -> SettingsScreen(telegram, settings, onBack = { showSettings = false })
            else -> MainScreen(telegram, settings, onOpenSettings = { showSettings = true })
        }
    }
}
