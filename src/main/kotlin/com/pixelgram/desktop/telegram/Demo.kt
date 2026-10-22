package com.pixelgram.desktop.telegram

import it.tdlight.jni.TdApi
import kotlinx.coroutines.launch

// Development only: -Dpixelgram.demo=true fills the UI with sample chats so
// layouts can be checked without signing in.
fun Telegram.loadDemo() {
    me = TdApi.User().apply {
        id = 1
        firstName = "Jetta"
        lastName = ""
        phoneNumber = "79990000000"
        usernames = TdApi.Usernames(arrayOf("pixelgramclient"), emptyArray<String>(), "pixelgramclient", emptyArray<String>(), "")
    }
    folders.addAll(listOf(
        FolderInfo(1, "Personal", "Private"),
        FolderInfo(2, "Work 💼", "Work"),
        FolderInfo(3, "Channels", "Channels"),
    ))
    val now = (System.currentTimeMillis() / 1000).toInt()
    val titles = listOf(
        "pixelGram 🎨", "Alice", "Bob 🚀", "Material Design", "Family 👨‍👩‍👧", "Kotlin", "News 🇺🇦",
        "Compose", "Weekend 🥳", "Music 🎧", "Photos", "Gaming 🎮",
    )
    val texts = listOf(
        "New build is out 🎉", "See you tomorrow 😀", "Rocket launch at 9 🚀🔥", "Expressive shapes 💜",
        "Dinner at 7? 🍕", "Coroutines are neat", "Breaking news 🇷🇺🇺🇦", "Recomposition 🤔",
        "Party time 🥳🎶", "Listen to this 🎧", "📷 Photo", "GG 🎮👍",
    )
    titles.forEachIndexed { index, title ->
        val chatId = 100L + index
        chats[chatId] = ChatState(chatId).apply {
            this.title = title
            lastMessage = TdApi.Message().apply {
                id = 1
                this.chatId = chatId
                date = now - index * 3600
                content = TdApi.MessageText().apply { text = TdApi.FormattedText(texts[index], emptyArray()) }
            }
            unreadCount = if (index % 3 == 0) index + 1 else 0
            positions[ChatListKey.Main] = 1000L - index
            pinned[ChatListKey.Main] = index < 2
            if (index % 2 == 0) positions[ChatListKey.Folder(1)] = 1000L - index
            if (index % 3 == 0) positions[ChatListKey.Folder(2)] = 1000L - index
        }
    }
    chatListsChanged()
    if (System.getProperty("pixelgram.demo.heavy") == "true") loadHeavyDemo(now)
    messages(100L).addAll(listOf(
        UiMessage(3, 100, 2, 0, false, now - 60, "Did the new emoji arrive? 😀🥳🫠👍"),
        UiMessage(2, 100, 1, 0, true, now - 120, "Yes! 🎉🔥❤️ and flags 🇷🇺🇺🇦"),
        UiMessage(1, 100, 2, 0, false, now - 180, "Hi there 👋"),
    ))
}

// Hundreds of chats and a stream of updates, like the first minute after
// signing in to a real account.
private fun Telegram.loadHeavyDemo(now: Int) {
    val emoji = listOf("🚀", "🎉", "😀", "🔥", "❤️", "🇺🇦", "👍", "🥳", "🤔", "🐱")
    for (index in 0 until 600) {
        val chatId = 1000L + index
        chats[chatId] = ChatState(chatId).apply {
            title = "Chat $index ${emoji[index % emoji.size]}"
            lastMessage = TdApi.Message().apply {
                id = 1
                this.chatId = chatId
                date = now - index * 60
                content = TdApi.MessageText().apply {
                    text = TdApi.FormattedText("Message $index ${emoji[(index + 3) % emoji.size]}", emptyArray())
                }
            }
            positions[ChatListKey.Main] = 500L - index
            if (index % 2 == 0) positions[ChatListKey.Archive] = 500L - index
        }
    }
    chatListsChanged()
    kotlinx.coroutines.MainScope().launch {
        var tick = 0
        while (true) {
            kotlinx.coroutines.delay(20)
            val chat = chats[1000L + (tick * 7) % 600] ?: continue
            chat.unreadCount = tick % 50
            chat.positions[ChatListKey.Main] = 1000L + tick
            chatListsChanged()
            tick++
        }
    }
}
