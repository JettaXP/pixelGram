package com.pixelgram.desktop.telegram

import com.pixelgram.desktop.ui.webapp.MiniAppSession
import com.pixelgram.desktop.ui.webapp.MiniApps
import it.tdlight.jni.TdApi

private const val DesktopPlatform = "tdesktop"
private const val AndroidPlatform = "android"

private fun parameters() = TdApi.WebAppOpenParameters(
    MiniApps.theme,
    if (MiniApps.android) AndroidPlatform else DesktopPlatform,
    TdApi.WebAppOpenModeFullSize(),
)

private fun Telegram.botTitle(botUserId: Long): String {
    ensureUser(botUserId)
    return userName(botUserId)
}

private fun Telegram.report(result: TdApi.Object, onError: (String) -> Unit) {
    (result as? TdApi.Error)?.let { onError(friendlyError(it.message)) }
}

fun Telegram.openWebApp(chatId: Long, botUserId: Long, url: String, threadId: Long = 0, onError: (String) -> Unit = {}) {
    val topic = threadId.takeIf { it != 0L }?.let { TdApi.MessageTopicThread(it) }
    send(TdApi.OpenWebApp(chatId, botUserId, url, topic, null, parameters())) { result ->
        if (result is TdApi.WebAppInfo) {
            MiniApps.launch(MiniAppSession(this, botUserId, botTitle(botUserId), result.url.url, chatId, launchId = result.launchId))
        } else {
            report(result, onError)
        }
    }
}

fun Telegram.openKeyboardWebApp(chatId: Long, botUserId: Long, url: String, buttonText: String, onError: (String) -> Unit = {}) {
    send(TdApi.GetWebAppUrl(botUserId, url, parameters())) { result ->
        if (result is TdApi.WebAppUrl) {
            MiniApps.launch(MiniAppSession(this, botUserId, botTitle(botUserId), result.url, chatId, buttonText = buttonText))
        } else {
            report(result, onError)
        }
    }
}

fun Telegram.openMainWebApp(chatId: Long, botUserId: Long, startParameter: String = "", onError: (String) -> Unit = {}) {
    send(TdApi.GetMainWebApp(chatId, botUserId, startParameter, parameters())) { result ->
        if (result is TdApi.MainWebApp) {
            MiniApps.launch(MiniAppSession(this, botUserId, botTitle(botUserId), result.url.url, chatId))
        } else {
            report(result, onError)
        }
    }
}

fun Telegram.openLinkWebApp(chatId: Long, botUserId: Long, shortName: String, startParameter: String, onError: (String) -> Unit = {}) {
    send(TdApi.GetWebAppLinkUrl(chatId, botUserId, shortName, startParameter, true, parameters())) { result ->
        if (result is TdApi.WebAppUrl) {
            MiniApps.launch(MiniAppSession(this, botUserId, botTitle(botUserId), result.url, chatId))
        } else {
            report(result, onError)
        }
    }
}

fun Telegram.closeWebApp(launchId: Long) {
    if (launchId != 0L) send(TdApi.CloseWebApp(launchId))
}

fun Telegram.sendWebAppData(botUserId: Long, buttonText: String, data: String) {
    send(TdApi.SendWebAppData(botUserId, buttonText, data))
}

fun Telegram.hasMainWebApp(botUserId: Long): Boolean =
    users[botUserId]?.type.let { it as? TdApi.UserTypeBot }?.hasMainWebApp == true
