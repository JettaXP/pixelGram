package com.pixelgram.desktop.ui.notify

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
)

// Where the pop-up window goes on the main screen, inside the work area
// (without the taskbar): left and right use the bottom corners.
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

// A frameless, transparent window above everything that never takes focus;
// as a dialog it gets no taskbar button.
@Composable
fun ToastWindow(toast: ToastData, settings: AppSettings, onOpen: () -> Unit, onGone: () -> Unit) {
    val side = settings.notificationSide.value
    key(side) {
        DialogWindow(
            onCloseRequest = onGone,
            state = rememberDialogState(
                position = positionFor(side),
                size = DpSize(ToastWindowWidth, ToastWindowHeight),
            ),
            title = "pixelGram",
            undecorated = true,
            transparent = true,
            resizable = false,
            focusable = false,
            alwaysOnTop = true,
        ) {
            PixelTheme(settings) {
                key(toast.id) { Toast(toast, settings, onOpen, onGone) }
            }
        }
    }
}
