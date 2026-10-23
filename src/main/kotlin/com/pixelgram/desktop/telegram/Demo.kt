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
    chats[104L]?.isGroup = true
    fun key(text: String) = TdApi.KeyboardButton().apply { this.text = text; type = TdApi.KeyboardButtonTypeText() }
    chats[102L]?.keyboard = TdApi.ReplyMarkupShowKeyboard().apply {
        rows = arrayOf(
            arrayOf(key("Edit birthday date"), key("Disable notifications")),
            arrayOf(key("How many days until my birthday?")),
            arrayOf(key("How many days since my birthday?")),
        )
    }
    users[2] = TdApi.User().apply { id = 2; firstName = "Alice"; lastName = "" }
    users[3] = TdApi.User().apply { id = 3; firstName = "Bob"; lastName = "" }
    messages(104L).addAll(listOf(
        UiMessage(206, 104, 1, 0, true, now - 60, "Pizza then 🍕"),
        UiMessage(205, 104, 3, 0, false, now - 120, "I'm in, but after 7"),
        UiMessage(204, 104, 2, 0, false, now - 180, "Dinner at 7?"),
        UiMessage(203, 104, 2, 0, false, now - 200, "Who is cooking today?"),
        UiMessage(202, 104, 2, 0, false, now - 220, "Hi everyone 👋"),
    ))
    System.getProperty("pixelgram.demo.plugin")?.let { path -> loadTypesDemo(path, now) }
    chats[101L]?.privateUserId = 2
    statuses[2] = TdApi.UserStatusOnline(0)
    chats[102L]?.privateUserId = 3
    statuses[3] = TdApi.UserStatusOffline(now - 300)
    if (System.getProperty("pixelgram.demo.offline") == "true") connection = ConnectionStatus.Connecting
    if (System.getProperty("pixelgram.demo.qr") == "true") {
        kotlinx.coroutines.MainScope().launch {
            kotlinx.coroutines.delay(4000)
            authState = AuthState.WaitQr("tg://login?token=pixelgram-demo")
        }
    }
    System.getProperty("pixelgram.demo.auth")?.let { step ->
        kotlinx.coroutines.MainScope().launch {
            kotlinx.coroutines.delay(4000)
            authState = if (step == "qr") AuthState.WaitQr("tg://login?token=pixelgram-demo") else AuthState.WaitPhone
        }
    }
    if (System.getProperty("pixelgram.demo.password") == "true") {
        kotlinx.coroutines.MainScope().launch {
            kotlinx.coroutines.delay(4000)
            authState = AuthState.WaitPassword("pixel")
        }
    }
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
    messages(100L).addAll((1..16).map { k ->
        UiMessage(-k.toLong(), 100, if (k % 2 == 0) 1 else 2, 0, k % 2 == 0, now - 86_400 - k * 3_000 - (if (k > 8) 86_400 * 3 else 0), "Older message $k")
    })
    messages(100L).add(0, UiMessage(5, 100, 2, 0, false, now - 10, "Looks great 🔥", replyToId = 2, reactions = listOf(
        TdApi.MessageReaction().apply { type = TdApi.ReactionTypeEmoji("❤️"); totalCount = 2; isChosen = true },
        TdApi.MessageReaction().apply { type = TdApi.ReactionTypeEmoji("🔥"); totalCount = 1 },
    )))
    run {
        fun button(text: String) = TdApi.InlineKeyboardButton().apply { this.text = text; type = TdApi.InlineKeyboardButtonTypeCallback(text.toByteArray()) }
        val rows = (0 until 3).map { r -> (0 until 4).map { c -> button(if ((r + c) % 3 == 0) "⚫" else " ") } } + listOf(listOf(button("🏳️ Сдаться")))
        messages(100L).add(0, UiMessage(7, 100, 2, 0, false, now - 3, "Шашки\nХодит: Alice (⚪ белые)", keyboard = rows, viaBot = 2))
    }
    run {
        val text = "Bold, italic, underline, struck out, mono and a secret spoiler text right here. Link to pixelGram.\nAnd a quote line"
        fun at(part: String, type: TdApi.TextEntityType) = TdApi.TextEntity(text.indexOf(part), part.length, type)
        val entities = listOf(
            at("Bold", TdApi.TextEntityTypeBold()),
            at("italic", TdApi.TextEntityTypeItalic()),
            at("underline", TdApi.TextEntityTypeUnderline()),
            at("struck out", TdApi.TextEntityTypeStrikethrough()),
            at("mono", TdApi.TextEntityTypeCode()),
            at("secret spoiler text right", TdApi.TextEntityTypeSpoiler()),
            at("pixelGram", TdApi.TextEntityTypeTextUrl("https://t.me/pixelgramclient")),
            at("And a quote line", TdApi.TextEntityTypeBlockQuote()),
        )
        messages(100L).add(0, UiMessage(6, 100, 2, 0, false, now - 5, text, entities = entities))
    }
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

private fun demoFile(id: Int, path: String?, size: Long) = TdApi.File().apply {
    this.id = id
    this.size = size
    expectedSize = size
    local = TdApi.LocalFile().apply {
        this.path = path.orEmpty()
        isDownloadingCompleted = path != null
    }
}

private fun Telegram.loadTypesDemo(pluginPath: String, now: Int) {
    val list = messages(101L)
    fun msg(id: Long, out: Boolean, ago: Int, content: TdApi.MessageContent) =
        UiMessage(id, 101, if (out) 1 else 2, 0, out, now - ago, messageText(content), content = content)
    fun link(url: String, preview: TdApi.LinkPreview) = TdApi.MessageText().apply {
        text = TdApi.FormattedText(url, arrayOf(TdApi.TextEntity(0, url.length, TdApi.TextEntityTypeUrl())))
        linkPreview = preview.apply { this.url = "https://$url"; displayUrl = url }
    }
    val shot = System.getProperty("pixelgram.demo.photo")
    val photo = TdApi.Photo().apply {
        sizes = arrayOf(TdApi.PhotoSize("x", demoFile(900_010, shot, 120_000), 1180, 760, IntArray(0)))
    }
    fun albumPhoto(id: Long, caption: String) = msg(id, true, 10, TdApi.MessagePhoto().apply {
        this.photo = photo
        this.caption = TdApi.FormattedText(caption, emptyArray())
    }).copy(photo = photo.sizes[0].photo, photoWidth = 1180, photoHeight = 760, fullPhoto = photo.sizes[0].photo, albumId = 77)
    val videoPath = System.getProperty("pixelgram.demo.video")
    val videoMessage = msg(38, false, 5, TdApi.MessageVideo().apply {
        video = TdApi.Video().apply {
            duration = 12; width = 1280; height = 720; fileName = "test.mp4"; mimeType = "video/mp4"
            thumbnail = TdApi.Thumbnail(TdApi.ThumbnailFormatJpeg(), 320, 180, demoFile(900_021, videoPath?.replace(".mp4", "_thumb.jpg"), 10_000))
            video = demoFile(900_020, videoPath, 1_000_000)
        }
        caption = TdApi.FormattedText("", emptyArray())
    })
    list.addAll(listOfNotNull(
        videoMessage.takeIf { videoPath != null },
        albumPhoto(37, ""),
        albumPhoto(36, ""),
        albumPhoto(35, ""),
        albumPhoto(34, "Album with four photos"),
        msg(33, false, 20, link("t.me/addtheme/end4hypr", TdApi.LinkPreview().apply {
            siteName = "Color theme"; title = "end-4 theme"; description = TdApi.FormattedText("", emptyArray())
            type = TdApi.LinkPreviewTypeTheme(arrayOf(TdApi.Document().apply {
                fileName = "theme.tdesktop-theme"
                thumbnail = TdApi.Thumbnail(TdApi.ThumbnailFormatPng(), 1180, 760, demoFile(900_010, shot, 120_000))
                document = demoFile(900_011, null, 1000)
            }), null)
        })),
        msg(32, true, 30, link("github.com/JettaXP/pixelGram", TdApi.LinkPreview().apply {
            siteName = "GitHub"; title = "JettaXP/pixelGram"
            description = TdApi.FormattedText("Telegram desktop client with Material 3 Expressive design.", emptyArray())
            type = TdApi.LinkPreviewTypeArticle(photo)
        })),
        msg(31, false, 40, link("t.me/addemoji/GOOGLEturtlestickers", TdApi.LinkPreview().apply {
            siteName = "Telegram"; title = "Google Turtle"
            description = TdApi.FormattedText("Free sticker pack with 120 stickers.", emptyArray())
            type = TdApi.LinkPreviewTypeStickerSet(emptyArray())
        })),
        msg(30, true, 60, TdApi.MessageDocument().apply {
            document = TdApi.Document().apply { fileName = "media_glow.plugin"; document = demoFile(900_001, pluginPath, 1006_284) }
            caption = TdApi.FormattedText("", emptyArray())
        }),
        msg(29, false, 120, TdApi.MessageAudio().apply {
            audio = TdApi.Audio().apply { title = ""; performer = ""; fileName = "2_5345844024081165008.mp3"; duration = 31; audio = demoFile(900_002, System.getProperty("pixelgram.demo.audio"), 751_718) }
            caption = TdApi.FormattedText("", emptyArray())
        }),
        msg(28, false, 180, TdApi.MessageVoiceNote().apply {
            voiceNote = TdApi.VoiceNote().apply { duration = 14; waveform = ByteArray(40) { (it * 37 % 251).toByte() }; voice = demoFile(900_003, System.getProperty("pixelgram.demo.voice"), 40_000) }
            caption = TdApi.FormattedText("", emptyArray())
        }),
        msg(27, true, 240, TdApi.MessageCall().apply { isVideo = false; discardReason = TdApi.CallDiscardReasonMissed(); duration = 0 }),
        msg(26, false, 300, TdApi.MessageCall().apply { isVideo = false; discardReason = TdApi.CallDiscardReasonHungUp(); duration = 101 }),
        msg(25, false, 360, TdApi.MessagePoll().apply {
            poll = TdApi.Poll().apply {
                question = TdApi.FormattedText("Which emoji style is the best?", emptyArray())
                isAnonymous = true
                type = TdApi.PollTypeRegular()
                totalVoterCount = 12
                options = arrayOf(
                    TdApi.PollOption().apply { text = TdApi.FormattedText("Noto 3D", emptyArray()); votePercentage = 58; isChosen = true },
                    TdApi.PollOption().apply { text = TdApi.FormattedText("Noto", emptyArray()); votePercentage = 25 },
                    TdApi.PollOption().apply { text = TdApi.FormattedText("Apple", emptyArray()); votePercentage = 17 },
                    TdApi.PollOption().apply { text = TdApi.FormattedText("Everyone", emptyArray()); votePercentage = 100 },
                )
            }
        }),
        msg(24, true, 420, TdApi.MessageContact().apply { contact = TdApi.Contact("79990001122", "Bob", "", "", 3) }),
        msg(23, false, 480, TdApi.MessageLocation().apply { location = TdApi.Location(55.751244, 37.618423, 0.0) }),
        msg(22, true, 540, TdApi.MessageGift().apply {
            gift = TdApi.Gift().apply { starCount = 15 }
            receiverId = TdApi.MessageSenderUser(2)
            text = TdApi.FormattedText("ЭКСТЕРАГРАМ ✅", emptyArray())
        }),
    ))
}

internal fun Telegram.demoSendFiles(chatId: Long, files: List<java.io.File>, caption: String) {
    val list = messages(chatId)
    val now = (System.currentTimeMillis() / 1000).toInt()
    files.forEachIndexed { index, file ->
        val (width, height) = imageSize(file)
        val local = TdApi.File().apply {
            id = 800_000 + index + now % 1000
            local = TdApi.LocalFile().apply {
                path = file.absolutePath
                isDownloadingCompleted = true
            }
        }
        val pending = UiMessage(
            -now.toLong() - index, chatId, 1, 0, true, now, if (index == 0) caption else "",
            fresh = true, photo = local, photoWidth = width, photoHeight = height, sending = true,
        )
        list.add(0, pending)
        kotlinx.coroutines.MainScope().launch {
            kotlinx.coroutines.delay(1500)
            val at = list.indexOfFirst { it.key == pending.key }
            if (at >= 0) list[at] = pending.copy(id = now.toLong() + index, sending = false, fresh = true)
        }
    }
}

