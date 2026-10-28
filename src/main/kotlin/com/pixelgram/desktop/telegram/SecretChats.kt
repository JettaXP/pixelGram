package com.pixelgram.desktop.telegram

import it.tdlight.jni.TdApi

private val isDemo get() = System.getProperty("pixelgram.demo") == "true"

const val DemoSecretChatId = 112L

enum class SecretStatus { Pending, Ready, Closed }

fun Telegram.secretChat(chat: ChatState): TdApi.SecretChat? =
    chat.secretChatId.takeIf { it != 0 }?.let { secretChats[it] }

fun Telegram.secretStatus(chat: ChatState): SecretStatus? {
    if (!isSecret(chat)) return null
    return when (secretChat(chat)?.state) {
        is TdApi.SecretChatStateReady -> SecretStatus.Ready
        is TdApi.SecretChatStateClosed -> SecretStatus.Closed
        else -> SecretStatus.Pending
    }
}

fun Telegram.secretChatWith(userId: Long): ChatState? =
    chats.values.firstOrNull { it.privateUserId == userId && it.secretChatId != 0 && secretStatus(it) != SecretStatus.Closed }

fun Telegram.startSecretChat(userId: Long, done: (Long?, String?) -> Unit) {
    if (isDemo) {
        if (chats[DemoSecretChatId] == null) {
            loadSecretDemo((System.currentTimeMillis() / 1000).toInt())
            chatListsChanged()
        }
        return done(DemoSecretChatId, null)
    }
    send(TdApi.CreateNewSecretChat(userId)) { result ->
        when (result) {
            is TdApi.Chat -> done(result.id, null)
            is TdApi.Error -> done(null, friendlyError(result.message))
            else -> done(null, null)
        }
    }
}

fun Telegram.closeSecretChat(chat: ChatState, done: (String?) -> Unit) {
    if (isDemo) {
        secretChats[chat.secretChatId] = TdApi.SecretChat(chat.secretChatId, chat.privateUserId, TdApi.SecretChatStateClosed(), true, ByteArray(0), 0)
        return done(null)
    }
    send(TdApi.CloseSecretChat(chat.secretChatId)) { result ->
        done((result as? TdApi.Error)?.message?.let(::friendlyError))
    }
}

internal fun Telegram.loadSecretDemo(now: Int) {
    val secretId = 7
    val key = ByteArray(36) { index -> ((index * 73 + 41) xor (index shl 3)).toByte() }
    secretChats[secretId] = TdApi.SecretChat(secretId, 2, TdApi.SecretChatStateReady(), true, key, 144)
    chats[DemoSecretChatId] = ChatState(DemoSecretChatId).apply {
        title = "Alice"
        privateUserId = 2
        secretChatId = secretId
        autoDelete = 60
        lastMessage = TdApi.Message().apply {
            id = 1
            chatId = DemoSecretChatId
            date = now - 90
            content = TdApi.MessageText().apply { text = TdApi.FormattedText("This chat is end-to-end encrypted 🔒", emptyArray()) }
        }
        positions[ChatListKey.Main] = 999L
    }
    messages(DemoSecretChatId).addAll(listOf(
        UiMessage(302, DemoSecretChatId, 2, 0, false, now - 90, "This chat is end-to-end encrypted 🔒"),
        UiMessage(301, DemoSecretChatId, 1, 0, true, now - 120, "Hi, secret Alice"),
    ))
}
