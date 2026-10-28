package com.pixelgram.desktop.telegram

import it.tdlight.jni.TdApi
import java.io.File

private val isDemo get() = System.getProperty("pixelgram.demo") == "true"

fun Telegram.themeSettings(chat: ChatState, dark: Boolean): TdApi.ThemeSettings? = when (val theme = chat.theme) {
    is TdApi.ChatThemeEmoji -> emojiThemes.firstOrNull { it.name == theme.name }?.let { if (dark) it.darkSettings else it.lightSettings }
    is TdApi.ChatThemeGift -> theme.giftTheme?.let { if (dark) it.darkSettings else it.lightSettings }
    else -> null
}

fun Telegram.themeName(chat: ChatState): String? = when (val theme = chat.theme) {
    is TdApi.ChatThemeEmoji -> theme.name
    is TdApi.ChatThemeGift -> theme.giftTheme?.gift?.let { "${it.title} #${it.number}" }
    else -> null
}

fun Telegram.chatBackground(chat: ChatState, dark: Boolean): TdApi.ChatBackground? =
    chat.background ?: themeSettings(chat, dark)?.background?.let { TdApi.ChatBackground(it, 0) }

fun Telegram.canSetChatTheme(chat: ChatState): Boolean =
    chat.privateUserId != 0L && !isSavedMessages(chat.id) && !isBot(chat)

fun Telegram.setChatTheme(chat: ChatState, name: String?, done: (String?) -> Unit) {
    if (isDemo) {
        chat.theme = name?.let { TdApi.ChatThemeEmoji(it) }
        return done(null)
    }
    send(TdApi.SetChatTheme(chat.id, name?.let { TdApi.InputChatThemeEmoji(it) })) { result ->
        done((result as? TdApi.Error)?.message?.let(::friendlyError))
    }
}

fun Telegram.setChatWallpaper(chat: ChatState, file: File, blurred: Boolean, dimming: Int, done: (String?) -> Unit) {
    val type = TdApi.BackgroundTypeWallpaper(blurred, false)
    if (isDemo) {
        val document = TdApi.Document().apply {
            fileName = file.name
            mimeType = "image/jpeg"
            document = TdApi.File().apply {
                id = file.path.hashCode()
                local = TdApi.LocalFile().apply { path = file.absolutePath; isDownloadingCompleted = true }
            }
        }
        chat.background = TdApi.ChatBackground(TdApi.Background(1, false, false, file.name, document, type), dimming)
        return done(null)
    }
    send(TdApi.SetChatBackground(chat.id, TdApi.InputBackgroundLocal(TdApi.InputFileLocal(file.absolutePath)), type, dimming, false)) { result ->
        done((result as? TdApi.Error)?.message?.let(::friendlyError))
    }
}

fun Telegram.resetChatWallpaper(chat: ChatState, done: (String?) -> Unit) {
    if (isDemo) {
        chat.background = null
        return done(null)
    }
    send(TdApi.DeleteChatBackground(chat.id, false)) { result ->
        done((result as? TdApi.Error)?.message?.let(::friendlyError))
    }
}

internal fun Telegram.loadThemesDemo() {
    fun settings(accent: Int, colors: IntArray, dark: Boolean) = TdApi.ThemeSettings(
        null,
        accent,
        TdApi.Background(0, false, dark, "", null, TdApi.BackgroundTypeFill(TdApi.BackgroundFillFreeformGradient(colors))),
        TdApi.BackgroundFillSolid(accent),
        false,
        false,
        0,
    )
    fun theme(emoji: String, accent: Int, light: IntArray, dark: IntArray) =
        TdApi.EmojiChatTheme(emoji, settings(accent, light, false), settings(accent, dark, true))
    emojiThemes = listOf(
        theme("🐥", 0xF2A93B, intArrayOf(0xFCE3A0, 0xF6C66B, 0xFDEFC4, 0xF2B860), intArrayOf(0x3B2A12, 0x5A3B14, 0x2A1E0E, 0x4A3214)),
        theme("⛄", 0x3D8BD9, intArrayOf(0xCDE4F7, 0x9CC6EE, 0xE3F0FB, 0xB4D4F2), intArrayOf(0x102436, 0x16324D, 0x0C1A28, 0x1A3A58)),
        theme("💎", 0x2CB5A6, intArrayOf(0xC4EFE6, 0x8CDCCD, 0xDDF6F0, 0xA8E6DA), intArrayOf(0x0E2E2A, 0x14403A, 0x0A221F, 0x18504A)),
        theme("👨‍🏫", 0x8A63D2, intArrayOf(0xE2D6F7, 0xC5AEEE, 0xEEE6FA, 0xD3C0F2), intArrayOf(0x241A38, 0x32244E, 0x1A1328, 0x3C2C5C)),
        theme("🌷", 0xE0569B, intArrayOf(0xF9D2E4, 0xF2A8CB, 0xFCE4EF, 0xF5BCD7), intArrayOf(0x3A1427, 0x521C37, 0x2A0F1C, 0x602242)),
        theme("💜", 0xB04FD8, intArrayOf(0xEED0F8, 0xDCA4F0, 0xF5E2FB, 0xE5BAF4), intArrayOf(0x30123C, 0x441A55, 0x220D2B, 0x521F66)),
    )
    if (System.getProperty("pixelgram.demo.theme") == "true") chats[101L]?.theme = TdApi.ChatThemeEmoji("🌷")
}
