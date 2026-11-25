package com.pixelgram.desktop.ui.notify

import kotlinx.coroutines.delay
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.core.ToastSide
import com.pixelgram.desktop.telegram.ChatState
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.telegram.contentText
import com.pixelgram.desktop.telegram.toUi
import com.pixelgram.desktop.ui.theme.PixelTheme
import it.tdlight.jni.TdApi
import java.awt.GraphicsEnvironment
import java.awt.Toolkit

fun toastFor(telegram: Telegram, message: TdApi.Message): ToastData? {
    val chat = telegram.chats[message.chatId] ?: return null
    val sender = if (chat.isGroup && !chat.isChannel) telegram.senderName(message.toUi()) else null
    return ToastData(
        id = System.nanoTime(),
        telegram = telegram,
        chat = chat,
        title = telegram.chatTitle(chat),
        sender = sender?.takeIf { it.isNotBlank() },
        text = contentText(message.content).replace('\n', ' '),
    )
}

fun testToast(telegram: Telegram) = ToastData(
    id = System.nanoTime(),
    telegram = telegram,
    chat = ChatState(0).apply { title = "pixelGram" },
    title = "pixelGram",
    sender = "Notifications",
    text = "This is how new messages will look 🎉",
    appIcon = true,
)

fun notificationScreens(): List<Pair<String, String>> =
    GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices.mapIndexed { index, device ->
        val bounds = device.defaultConfiguration.bounds
        val mode = device.displayMode
        device.iDstring to "Display ${index + 1} (${mode?.width ?: bounds.width}×${mode?.height ?: bounds.height})"
    }

private fun positionFor(side: ToastSide, screen: String, height: Int, w: Int = ToastWindowWidth.value.toInt()): WindowPosition {
    val environment = GraphicsEnvironment.getLocalGraphicsEnvironment()
    val device = environment.screenDevices.firstOrNull { it.iDstring == screen } ?: environment.defaultScreenDevice
    val config = device.defaultConfiguration
    val bounds = config.bounds
    val insets = Toolkit.getDefaultToolkit().getScreenInsets(config)
    val left = bounds.x + insets.left
    val top = bounds.y + insets.top
    val width = bounds.width - insets.left - insets.right
    val area = bounds.height - insets.top - insets.bottom
    val x = when (side.column) {
        0 -> left
        2 -> left + width - w
        else -> left + (width - w) / 2
    }
    val y = if (side.top) top else top + area - height
    return WindowPosition(x.dp, y.dp)
}

@Composable
fun ToastWindow(stack: ToastStack, settings: AppSettings, onOpen: (ToastData) -> Unit) {
    val side = settings.notificationSide.value
    val screen = settings.notificationScreen.value
    val count = settings.notificationCount.value.coerceIn(1, MaxCount)
    stack.max = count
    val height = toastWindowHeight(count)
    var warm by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(WarmUpMs)
        warm = false
    }
    val showing = stack.entries.isNotEmpty()
    val opaque = !com.pixelgram.desktop.core.AppPaths.translucentWindows
    val visibleCount = stack.shown.size.coerceAtLeast(1)
    val windowWidth = if (opaque) ToastCardWidth else ToastWindowWidth
    val windowHeight = if (opaque) ToastCardHeight * visibleCount + (if (stack.shown.size >= stack.max && stack.max > 1) 48.dp else 0.dp) else height
    val state = rememberDialogState(position = OffScreen, size = DpSize(windowWidth, windowHeight))
    LaunchedEffect(showing, side, screen, windowHeight) {
        state.size = DpSize(windowWidth, windowHeight)
        val target = if (showing) positionFor(side, screen, windowHeight.value.toInt(), windowWidth.value.toInt()) else OffScreen
        state.position = target
        if (showing && opaque) com.pixelgram.desktop.core.CompositorPlace.place(ToastTitle, target.x.value.toInt(), target.y.value.toInt(), windowWidth.value.toInt(), windowHeight.value.toInt())
    }
    DialogWindow(
        onCloseRequest = stack::dismissAll,
        visible = showing || (warm && !opaque),
        state = state,
        title = ToastTitle,
        undecorated = true,
        transparent = com.pixelgram.desktop.core.AppPaths.translucentWindows,
        resizable = false,
        focusable = false,
        alwaysOnTop = true,
    ) {
        PixelTheme(settings) {
            ToastStackContent(stack, settings, onOpen, opaque = opaque)
        }
    }
}

const val ToastTitle = "pixelGram notification"
private val OffScreen = WindowPosition((-10000).dp, (-10000).dp)
private const val WarmUpMs = 1500L
const val MaxCount = 5
