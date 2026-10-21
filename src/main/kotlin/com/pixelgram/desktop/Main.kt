package com.pixelgram.desktop

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.telegram.Accounts
import com.pixelgram.desktop.telegram.AuthState
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.AppIcons
import com.pixelgram.desktop.ui.Screen
import com.pixelgram.desktop.ui.profile.ProfileScreen
import com.pixelgram.desktop.ui.people.CallsScreen
import com.pixelgram.desktop.ui.people.ContactsScreen
import com.pixelgram.desktop.ui.auth.AuthScreen
import com.pixelgram.desktop.ui.main.MainScreen
import com.pixelgram.desktop.ui.settings.SettingsScreen
import com.pixelgram.desktop.ui.theme.PixelTheme
import com.pixelgram.desktop.ui.theme.isDarkTheme
import kotlinx.coroutines.MainScope

fun main() = application {
    val settings = remember { AppSettings.load() }
    val accounts = remember { Accounts(settings, MainScope()) }
    LaunchedEffect(Unit) { accounts.start() }

    Window(
        onCloseRequest = {
            accounts.closeAll()
            exitApplication()
        },
        title = "pixelGram",
        icon = AppIcons.forTheme(isDarkTheme(settings)),
        state = rememberWindowState(size = DpSize(1180.dp, 760.dp)),
    ) {
        window.minimumSize = java.awt.Dimension(800, 520)
        PixelTheme(settings) {
            key(accounts.active) {
                App(accounts, accounts.active, settings)
            }
        }
    }
}

// Development only: -Dpixelgram.screen=settings opens settings without signing in.
private val devScreen: String? = System.getProperty("pixelgram.screen")

@Composable
private fun App(accounts: Accounts, telegram: Telegram, settings: AppSettings) {
    var screen by remember {
        mutableStateOf<Screen>(if (devScreen == "settings") Screen.Settings() else Screen.Chats)
    }
    var openChatId by remember { mutableStateOf<Long?>(null) }
    val openChat = { id: Long ->
        openChatId = id
        screen = Screen.Chats
    }
    val ready = telegram.authState == AuthState.Ready || devScreen != null
    // WHY: without a background the window shows through during the
    // crossfade, which looks like a white flash when opening settings.
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxSize()) {
        val enter = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
        val exit = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
        AnimatedContent(
            targetState = ready to screen,
            transitionSpec = { fadeIn(enter) togetherWith fadeOut(exit) },
        ) { (isReady, current) ->
            val back = { screen = Screen.Chats }
            when {
                !isReady -> AuthScreen(
                    telegram,
                    onCancel = if (accounts.list.size > 1) {
                        { accounts.cancelAdding(telegram) }
                    } else {
                        null
                    },
                )
                current is Screen.Settings -> SettingsScreen(
                    telegram,
                    settings,
                    startInPreferences = current.preferences,
                    onOpenProfile = { screen = Screen.Profile },
                    onBack = back,
                )
                current == Screen.Profile -> ProfileScreen(telegram, settings, openChat, onBack = back)
                current == Screen.Contacts -> ContactsScreen(telegram, settings, openChat, onBack = back)
                current == Screen.Calls -> CallsScreen(telegram, settings, openChat, onBack = back)
                else -> MainScreen(accounts, telegram, settings, openChatId, openChat, onNavigate = { screen = it })
            }
        }
    }
}
