package com.pixelgram.desktop.telegram

import it.tdlight.jni.TdApi
import kotlinx.coroutines.launch

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
                isOutgoing = index % 4 == 1
                content = TdApi.MessageText().apply { text = TdApi.FormattedText(texts[index], emptyArray()) }
            }
            unreadCount = if (index % 3 == 0) index + 1 else 0
            lastReadOutbox = if (index % 8 == 1) 1 else 0
            positions[ChatListKey.Main] = 1000L - index
            pinned[ChatListKey.Main] = index < 2
            if (index % 2 == 0) positions[ChatListKey.Folder(1)] = 1000L - index
            if (index % 3 == 0) positions[ChatListKey.Folder(2)] = 1000L - index
        }
    }
    chats[100L]?.lastReadOutbox = 2
    unread[ChatListKey.Main] = UnreadCount(5, 3)
    unread[ChatListKey.Folder(1)] = UnreadCount(2, 0)
    unread[ChatListKey.Folder(2)] = UnreadCount(12, 4)
    chatListsChanged()
    if (System.getProperty("pixelgram.demo.heavy") == "true") loadHeavyDemo(now)
    System.getProperty("pixelgram.demo.photo")?.let { path ->
        val file = TdApi.File().apply {
            id = 999_999
            local = TdApi.LocalFile().apply {
                this.path = path
                isDownloadingCompleted = true
            }
        }
        messages(100L).add(UiMessage(4, 100, 1, 0, true, now - 30, "Screenshot of the new build 🎉", photo = file, photoWidth = 1280, photoHeight = 800))
    }
    messages(100L).addAll(listOf(
        UiMessage(3, 100, 2, 0, false, now - 60, "Did the new emoji arrive? 😀🥳🫠👍"),
        UiMessage(2, 100, 1, 0, true, now - 120, "Yes! 🎉🔥❤️ and flags 🇷🇺🇺🇦", edited = true),
        UiMessage(1, 100, 2, 0, false, now - 180, "Hi there 👋"),
        UiMessage(0, 100, 2, 0, false, now - 240, "Created «pixelGram 🎨»", service = true),
    ))
    messages(100L).add(0, UiMessage(5, 100, 2, 0, false, now - 10, "Looks great 🔥", replyToId = 2))
}

private fun Telegram.loadHeavyDemo(now: Int) {
    listOf("Groups" to "Groups", "Bots" to "Bots", "Home" to "Home", "Plugins" to "Custom", "Unread" to "Unread")
        .forEachIndexed { index, (title, icon) -> folders.add(FolderInfo(10 + index, title, icon)) }
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
