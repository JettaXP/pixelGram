package com.pixelgram.desktop.ui.notify

import kotlinx.coroutines.delay
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
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

private fun positionFor(side: ToastSide): WindowPosition {
    val config = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration
    val bounds = config.bounds
    val insets = Toolkit.getDefaultToolkit().getScreenInsets(config)
    val left = bounds.x + insets.left
    val top = bounds.y + insets.top
    val width = bounds.width - insets.left - insets.right
    val height = bounds.height - insets.top - insets.bottom
    val w = ToastWindowWidth.value.toInt()
    val h = ToastWindowHeight.value.toInt()
    val (x, y) = when (side) {
        ToastSide.Left -> left to top + height - h
        ToastSide.Right -> left + width - w to top + height - h
        ToastSide.Top -> left + (width - w) / 2 to top
        ToastSide.Bottom -> left + (width - w) / 2 to top + height - h
    }
    return WindowPosition(x.dp, y.dp)
}

@Composable
fun ToastWindow(stack: ToastStack, settings: AppSettings, onOpen: (ToastData) -> Unit) {
    val side = settings.notificationSide.value
    var warm by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(WarmUpMs)
        warm = false
    }
    val showing = stack.entries.isNotEmpty()
    key(side) {
        val state = rememberDialogState(
            position = OffScreen,
            size = DpSize(ToastWindowWidth, ToastWindowHeight),
        )
        LaunchedEffect(showing) {
            state.position = if (showing) positionFor(side) else OffScreen
        }
        DialogWindow(
            onCloseRequest = stack::dismissAll,
            visible = warm || showing,
            state = state,
            title = "pixelGram",
            undecorated = true,
            transparent = true,
            resizable = false,
            focusable = false,
            alwaysOnTop = true,
        ) {
            PixelTheme(settings) {
                ToastStackContent(stack, settings, onOpen)
            }
        }
    }
}

private val OffScreen = WindowPosition((-10000).dp, (-10000).dp)
private const val WarmUpMs = 1500L
