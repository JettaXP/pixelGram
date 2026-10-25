package com.pixelgram.desktop

import com.pixelgram.desktop.core.AppLock
import com.pixelgram.desktop.ui.LockScreen
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import com.pixelgram.desktop.ui.EasterEgg
import com.pixelgram.desktop.ui.EasterEggOverlay
import com.pixelgram.desktop.ui.main.BotAnswer
import com.pixelgram.desktop.ui.main.TopNotice
import com.pixelgram.desktop.ui.main.AudioQueueEffects
import com.pixelgram.desktop.core.Updater
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.DisposableEffect
import com.pixelgram.desktop.core.UrlScheme
import com.pixelgram.desktop.ui.main.Links
import com.pixelgram.desktop.core.InstanceState
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.Key
import com.pixelgram.desktop.ui.EscapeStack
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.pixelgram.desktop.ui.main.VideoPlayerWindow
import com.pixelgram.desktop.ui.main.VideoPlayer
import androidx.compose.ui.ExperimentalComposeUiApi
import javax.swing.JOptionPane
import com.pixelgram.desktop.ui.CrashHandler
import com.pixelgram.desktop.core.SingleInstance
import androidx.compose.ui.window.LocalWindowExceptionHandlerFactory
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.runtime.CompositionLocalProvider
import com.pixelgram.desktop.ui.main.ChatView
import com.pixelgram.desktop.ui.main.ChatWindows
import kotlinx.coroutines.delay
import androidx.compose.runtime.mutableIntStateOf
import io.github.vinceglb.filekit.FileKit
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.pixelgram.desktop.core.AppPaths
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.core.MacSystem
import com.pixelgram.desktop.core.NotificationStyle
import com.pixelgram.desktop.core.Sounds
import com.pixelgram.desktop.core.SystemNotifications
import com.pixelgram.desktop.telegram.Accounts
import com.pixelgram.desktop.telegram.AuthState
import com.pixelgram.desktop.telegram.Notifications
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.telegram.loadDemo
import com.pixelgram.desktop.ui.AppIcons
import com.pixelgram.desktop.ui.PixelTray
import com.pixelgram.desktop.ui.ResizeHandles
import com.pixelgram.desktop.ui.Screen
import com.pixelgram.desktop.ui.TitleBar
import com.pixelgram.desktop.ui.rememberWindowChrome
import com.pixelgram.desktop.ui.auth.AuthScreen
import com.pixelgram.desktop.ui.main.Attachments
import com.pixelgram.desktop.ui.main.PhotoViewer
import com.pixelgram.desktop.ui.main.PhotoViewerWindow
import com.pixelgram.desktop.ui.main.MainScreen
import com.pixelgram.desktop.ui.notify.ToastData
import com.pixelgram.desktop.ui.notify.ToastStack
import com.pixelgram.desktop.ui.notify.ToastWindow
import com.pixelgram.desktop.ui.notify.testToast
import com.pixelgram.desktop.ui.notify.toastFor
import com.pixelgram.desktop.ui.people.CallsScreen
import com.pixelgram.desktop.ui.people.ContactsScreen
import com.pixelgram.desktop.ui.profile.ProfileScreen
import com.pixelgram.desktop.ui.settings.SettingsScreen
import com.pixelgram.desktop.ui.theme.Emoji3D
import com.pixelgram.desktop.ui.theme.EmojiFonts
import com.pixelgram.desktop.ui.theme.PixelTheme
import com.pixelgram.desktop.ui.theme.appColorScheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.SystemTray
import kotlinx.coroutines.MainScope

@OptIn(ExperimentalComposeUiApi::class)
fun main(args: Array<String>) {
    System.setProperty("sun.awt.noerasebackground", "true")
    if (AppPaths.isMac) {
        System.setProperty("apple.awt.application.name", "pixelGram")
        System.setProperty("apple.laf.useScreenMenuBar", "true")
    }
    val link = args.firstOrNull { Links.isTelegram(it) }
    when (SingleInstance.acquire(link)) {
        InstanceState.Primary -> Unit
        InstanceState.Forwarded -> return
        InstanceState.Stuck -> {
            JOptionPane.showMessageDialog(
            null,
            "pixelGram is already running but does not respond.\nEnd the pixelGram process in the task manager and start it again.",
            "pixelGram",
            JOptionPane.WARNING_MESSAGE,
            )
            return
        }
    }
    link?.let { Links.pending = it }
    SingleInstance.onOpen = { Links.pending = it }
    UrlScheme.register()
    if (AppPaths.isMac) {
        MacSystem.openLink = { url ->
            SingleInstance.onOpen?.invoke(url)
            MacSystem.openWindow?.invoke()
        }
        MacSystem.install()
    }
    application {
        CompositionLocalProvider(LocalWindowExceptionHandlerFactory provides CrashHandler) {
            PixelApp()
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ApplicationScope.PixelApp() {
    val settings = remember {
        com.pixelgram.desktop.ui.theme.removeOldEmojiFont()
        Attachments.cleanTemp()
        runCatching { FileKit.init("com.pixelgram.desktop") }
        Sounds.warmUp()
        AppSettings.load().also { EmojiFonts.settings = it }
    }
    val accounts = remember {
        Accounts(settings, MainScope()).apply { onDuplicate = { TopNotice.text = "This account is already added to pixelGram" } }
    }
    LaunchedEffect(Unit) {
        Updater.shutdown = {
            accounts.closeAll()
            Thread.sleep(ShutdownGraceMs)
        }
        if (settings.checkUpdates.value) Updater.check()
        if (settings.wallpaperColors.value) com.pixelgram.desktop.ui.main.Wallpapers.applyColors(settings)
        if (settings.notificationStyle.value == NotificationStyle.System) SystemNotifications.prepare()
        accounts.start()
        if (System.getProperty("pixelgram.demo") == "true") accounts.active.loadDemo()
    }

    val quit = {
        accounts.closeAll()
        exitApplication()
    }
    var visible by remember { mutableStateOf(true) }
    val scheme = appColorScheme(settings)
    EasterEgg.settings = settings
    val israel = EasterEgg.israel
    val icon = remember(scheme.primaryContainer, scheme.onPrimaryContainer, israel) { AppIcons.painter(scheme) }
    LaunchedEffect(scheme.primaryContainer, scheme.onPrimaryContainer, israel) {
        withContext(Dispatchers.IO) {
            SystemNotifications.updateIcon(scheme)
            AppIcons.installForSystem(scheme)
        }
    }
    val hasTray = remember {
        SystemTray.isSupported() && !(AppPaths.isLinux && System.getenv("WAYLAND_DISPLAY") != null)
    }
    if (hasTray) {
        val trayImages = remember(scheme.primaryContainer, scheme.onPrimaryContainer, israel) { AppIcons.sizes(scheme) }
        PixelTray(trayImages, settings, onOpen = { visible = true }, onQuit = quit)
    }

    val toasts = remember { ToastStack() }
    var raise by remember { mutableIntStateOf(0) }
    DisposableEffect(Unit) {
        MacSystem.quit = quit
        MacSystem.openWindow = {
            visible = true
            raise++
        }
        onDispose {
            MacSystem.quit = null
            MacSystem.openWindow = null
        }
    }
    val openFromNotification = { card: ToastData ->
        visible = true
        raise++
        if (card.chat.id != 0L) {
            accounts.switchTo(card.telegram)
            Notifications.openRequest = card.telegram.account to card.chat.id
        }
    }
    LaunchedEffect(Unit) {
        SingleInstance.onShow = {
            visible = true
            raise++
        }
        Notifications.onMessage = { telegram, message ->
            val card = (if (settings.notificationPopups.value) toastFor(telegram, message) else null)
                ?.let { if (AppLock.locked) it.copy(text = "") else it }
            when {
                card == null -> Unit
                settings.notificationStyle.value == NotificationStyle.System && SystemNotifications.available ->
                    SystemNotifications.show(
                        title = card.sender?.let { "$it · ${card.title}" } ?: card.title,
                        body = card.text.ifEmpty { LockedBody },
                        icon = telegram.localPath(card.chat.photo),
                        onClick = { openFromNotification(card) },
                    )
                else -> toasts.push(card)
            }
        }
        Notifications.showTest = { toasts.push(testToast(accounts.active)) }
    }
    ChatWindows.open.toList().forEach { (owner, chatId) ->
        key(owner.account, chatId) {
            var shownId by remember { mutableStateOf(chatId) }
            val chat = owner.chats[shownId]
            Window(
                onCloseRequest = { ChatWindows.open.remove(owner to chatId) },
                title = chat?.let { owner.chatTitle(it) } ?: "pixelGram",
                icon = icon,
                state = rememberWindowState(size = DpSize(520.dp, 720.dp)),
            ) {
                PixelTheme(settings) {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxSize()) {
                        if (chat != null) key(shownId) { ChatView(chat, owner, settings, onOpenChat = { shownId = it }) }
                    }
                }
            }
        }
    }
    PhotoViewer.current?.let { photo ->
        key(photo) { PhotoViewerWindow(photo, settings) }
    }
    VideoPlayer.current?.let { video ->
        key(video) { VideoPlayerWindow(video, settings) }
    }
    ToastWindow(toasts, settings, onOpen = openFromNotification)

    val close = { if (hasTray) visible = false else quit() }
    val ownTitleBar = remember { settings.customTitleBar.value }
    val windowState = rememberWindowState(size = DpSize(1180.dp, 760.dp))
    Window(
        onCloseRequest = close,
        undecorated = ownTitleBar,
        visible = visible,
        title = "pixelGram",
        icon = icon,
        state = windowState,
    ) {
        window.minimumSize = java.awt.Dimension(800, 520)
        LaunchedEffect(Unit) {
            delay(RenderApiLogDelayMs)
            com.pixelgram.desktop.core.AppLog.write("Render API: ${window.renderApi}")
        }
        LaunchedEffect(scheme.surfaceContainer) {
            val background = java.awt.Color(scheme.surfaceContainer.toArgb())
            window.background = background
            window.contentPane.background = background
        }
        LaunchedEffect(scheme.primaryContainer, scheme.onPrimaryContainer, israel) {
            val images = withContext(Dispatchers.IO) { AppIcons.sizes(scheme) }
            if (images.isNotEmpty()) window.iconImages = images
        }
        LaunchedEffect(visible, raise) {
            if (!visible) return@LaunchedEffect
            if (window.extendedState and java.awt.Frame.ICONIFIED != 0) window.extendedState = java.awt.Frame.NORMAL
            if (window.opacity < 1f) window.opacity = 1f
            window.isAlwaysOnTop = true
            window.toFront()
            window.requestFocus()
            delay(RaiseHoldMs)
            window.isAlwaysOnTop = false
        }
        val focused = LocalWindowInfo.current.isWindowFocused
        LaunchedEffect(focused) { accounts.list.forEach { it.windowFocused = focused } }
        val chrome = rememberWindowChrome()
        val rootFocus = remember { FocusRequester() }
        var focusInside by remember { mutableStateOf(false) }
        val focusScope = rememberCoroutineScope()
        LaunchedEffect(Unit) { rootFocus.requestFocus() }
        PixelTheme(settings) {
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                if (event.type == PointerEventType.Press && !focusInside) rootFocus.requestFocus()
                            }
                        }
                    }
                    .focusRequester(rootFocus)
                    .onFocusChanged {
                        focusInside = it.hasFocus
                        if (!it.hasFocus) focusScope.launch { runCatching { rootFocus.requestFocus() } }
                    }
                    .focusable()
                    .onKeyEvent { event ->
                        event.type == KeyEventType.KeyDown && event.key == Key.Escape && EscapeStack.dispatch()
                    },
            ) {
                Column {
                    if (ownTitleBar) TitleBar(chrome, onClose = close)
                    key(accounts.active) {
                        App(accounts, accounts.active, settings)
                    }
                }
                if (ownTitleBar) ResizeHandles(chrome)
            }
        }
    }
}

private val devScreen: String? = System.getProperty("pixelgram.screen")
private const val ShutdownGraceMs = 800L
private const val ScreenScaleFrom = 0.94f
private const val ScreenScaleTo = 1.03f

@Composable
private fun App(accounts: Accounts, telegram: Telegram, settings: AppSettings) {
    var screen by remember {
        mutableStateOf<Screen>(if (devScreen == "settings") Screen.Settings() else Screen.Chats)
    }
    val history = remember { mutableStateListOf<Screen>() }
    val navigate = { to: Screen ->
        history.add(screen)
        screen = to
    }
    val back = { screen = history.removeLastOrNull() ?: Screen.Chats }
    var openChatId by remember { mutableStateOf<Long?>(null) }
    AudioQueueEffects(telegram)
    val chatTrail = remember { mutableStateListOf<Long>() }
    val openChat = { id: Long ->
        chatTrail.clear()
        openChatId = id
        history.clear()
        screen = Screen.Chats
    }
    val openChatInside = { id: Long ->
        openChatId?.takeIf { it != id }?.let { chatTrail.add(it) }
        openChatId = id
        history.clear()
        screen = Screen.Chats
    }
    val chatBack = { openChatId = chatTrail.removeLastOrNull() }
    DisposableEffect(telegram) {
        Links.active = { telegram.takeIf { it.authState == AuthState.Ready } }
        Links.navigate = { owner, chatId ->
            if (owner != telegram) accounts.switchTo(owner)
            openChatInside(chatId)
        }
        onDispose {}
    }
    LaunchedEffect(Links.pending, telegram.authState) {
        val link = Links.pending ?: return@LaunchedEffect
        if (telegram.authState != AuthState.Ready) return@LaunchedEffect
        Links.pending = null
        Links.open(link)
    }
    val saved = rememberSaveableStateHolder()
    LaunchedEffect(Notifications.openRequest) {
        val request = Notifications.openRequest ?: return@LaunchedEffect
        if (request.first == telegram.account) {
            Notifications.openRequest = null
            openChat(request.second)
        }
    }
    val ready = telegram.authState == AuthState.Ready || devScreen != null
    DisposableEffect(ready) {
        MacSystem.openSettings = if (ready) {
            { if (screen !is Screen.Settings) navigate(Screen.Settings()) }
        } else {
            null
        }
        onDispose { MacSystem.openSettings = null }
    }
    AppLock.start(settings)
    LaunchedEffect(Unit) {
        while (true) {
            delay(LockCheckMs)
            AppLock.tick()
        }
    }
    LaunchedEffect(ready) {
        if (!ready) return@LaunchedEffect
        val updated = Updater.consumeUpdated()
        val failure = Updater.consumeFailure()
        if (updated || failure != null) {
            delay(UpdatedNoticeDelayMs)
            TopNotice.text = if (updated) "pixelGram Updated!" else "Update failed: $failure"
        }
    }
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxSize()) {
      Box(Modifier.fillMaxSize()) {
        val enter = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
        val exit = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
        val grow = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
        AnimatedContent(
            targetState = ready to screen,
            transitionSpec = {
                (fadeIn(enter) + androidx.compose.animation.scaleIn(grow, initialScale = ScreenScaleFrom)) togetherWith
                    (fadeOut(exit) + androidx.compose.animation.scaleOut(grow, targetScale = ScreenScaleTo))
            },
        ) { (isReady, current) ->
            saved.SaveableStateProvider(if (isReady) current.toString() else "auth") {
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
                        onOpenProfile = { navigate(Screen.Profile) },
                        onOpenChat = openChat,
                        onBack = back,
                    )
                    current == Screen.Profile -> ProfileScreen(telegram, settings, openChat, onBack = back)
                    current == Screen.Contacts -> ContactsScreen(telegram, settings, openChat, onBack = back)
                    current == Screen.Calls -> CallsScreen(telegram, settings, openChat, onBack = back)
                    else -> MainScreen(
                        accounts,
                        telegram,
                        settings,
                        openChatId,
                        openChat,
                        onNavigate = navigate,
                        onCloseChat = {
                            chatTrail.clear()
                            openChatId = null
                        },
                        onOpenChatInside = openChatInside,
                        onChatBack = if (chatTrail.isNotEmpty()) chatBack else null,
                    )
                }
            }
        }
        androidx.compose.animation.AnimatedVisibility(
            visible = AppLock.locked,
            enter = androidx.compose.animation.fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                androidx.compose.animation.scaleIn(MaterialTheme.motionScheme.defaultSpatialSpec(), initialScale = LockScaleFrom),
            exit = androidx.compose.animation.fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                androidx.compose.animation.scaleOut(MaterialTheme.motionScheme.defaultSpatialSpec(), targetScale = LockScaleFrom),
        ) { LockScreen() }
        EasterEggOverlay()
        BotAnswer(
            TopNotice.text?.let { it to false },
            onDismiss = { TopNotice.text = null },
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp),
        )
      }
    }
}

private const val RaiseHoldMs = 150L
private const val RenderApiLogDelayMs = 3_000L
private const val UpdatedNoticeDelayMs = 1500L
private const val LockCheckMs = 10_000L
private const val LockScaleFrom = 1.08f

private const val LockedBody = " "
