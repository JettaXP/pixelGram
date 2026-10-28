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
    System.getProperty("pixelgram.demo.banner")?.let { path ->
        com.pixelgram.desktop.core.Banners.setOwn(1, java.io.File(path))
        com.pixelgram.desktop.core.Banners.setOwn(2, java.io.File(path))
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
    if (System.getProperty("pixelgram.demo.manage") == "true") loadManageDemo(now)
    System.getProperty("pixelgram.demo.plugin")?.let { path -> loadTypesDemo(path, now) }
    chats[101L]?.privateUserId = 2
    statuses[2] = TdApi.UserStatusOnline(0)
    chats[102L]?.privateUserId = 3
    statuses[3] = TdApi.UserStatusOffline(now - 300)
    if (System.getProperty("pixelgram.demo.offline") == "true") connection = ConnectionStatus.Connecting
    if (System.getProperty("pixelgram.demo.protected") == "true") chats[106L]?.protectedContent = true
    if (System.getProperty("pixelgram.demo.secret") == "true") loadSecretDemo(now)
    loadThemesDemo()
    System.getProperty("pixelgram.demo.miniapp")?.let { path ->
        kotlinx.coroutines.MainScope().launch {
            kotlinx.coroutines.delay(DemoMiniAppDelayMs)
            com.pixelgram.desktop.ui.webapp.MiniApps.launch(com.pixelgram.desktop.ui.webapp.MiniAppSession(this@loadDemo, 2, "Demo App", java.io.File(path).toURI().toString() + "#tgWebAppPlatform=tdesktop", 101))
        }
    }
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
    System.getProperty("pixelgram.demo.typing")?.toLongOrNull()?.let { chatId ->
        demoAction(chatId, TdApi.MessageSenderUser(2), TdApi.ChatActionTyping())
    }
    System.getProperty("pixelgram.demo.photo")?.let { path ->
        val file = TdApi.File().apply {
            id = 999_999
            local = TdApi.LocalFile().apply {
                this.path = path
                isDownloadingCompleted = true
            }
        }
        messages(100L).add(UiMessage(4, 100, 1, 0, true, now - 30, "Screenshot of the new build 🎉", photo = file, photoWidth = 1280, photoHeight = 800))
        chats[106L]?.apply {
            isChannel = true
            isGroup = true
        }
        fun reaction(emoji: String, count: Int) = TdApi.MessageReaction().apply { type = TdApi.ReactionTypeEmoji(emoji); totalCount = count }
        messages(106L).add(UiMessage(502, 106, 0, 106, false, now - 900, "ИнтеллиНьюз - Израиль и Ближний Восток. Все о безопасности: цена нефти марки Brent достигла отметки в 115 долларов"))
        messages(106L).add(0, UiMessage(
            501, 106, 0, 106, false, now - 600, replyToId = 502, text = "Цена нефти марки Brent достигла отметки в 117 долларов за баррель.\n\n#news",
            photo = file, photoWidth = 800, photoHeight = 1000, fullPhoto = file,
            reactions = listOf(reaction("😱", 25), reaction("💔", 2), reaction("🤷", 2), reaction("🤬", 1), reaction("🐳", 1)),
            signature = "Alex Dromi", views = 2500, comments = 4,
        ))
        System.getProperty("pixelgram.demo.photo2")?.let { second ->
            val other = TdApi.File().apply {
                id = 999_998
                local = TdApi.LocalFile().apply {
                    this.path = second
                    isDownloadingCompleted = true
                }
            }
            messages(100L).add(UiMessage(4_007, 100, 2, 0, false, now - 45, "The banner from the profile, with a much longer caption so it wraps across a couple of lines in the viewer 🖼", photo = other, photoWidth = 1280, photoHeight = 400))
        }
    }
    messages(100L).addAll(listOf(
        UiMessage(3, 100, 2, 0, false, now - 60, "Did the new emoji arrive? 😀🥳🫠👍"),
        UiMessage(60, 100, 1, 0, true, now - 100, "", service = true, content = TdApi.MessagePinMessage(2)),
        UiMessage(2, 100, 1, 0, true, now - 120, "Yes! 🎉🔥❤️ and flags 🇷🇺🇺🇦", edited = true, pinned = true),
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
        val text = "Ходит: 🍇 Jetta (⚪ белые)\n⚫ 12 — ⚪ 12"
        val entities = listOf(
            TdApi.TextEntity(text.indexOf("⚪"), 1, TdApi.TextEntityTypeCustomEmoji(11)),
            TdApi.TextEntity(text.indexOf("⚫"), 1, TdApi.TextEntityTypeCustomEmoji(12)),
            TdApi.TextEntity(text.lastIndexOf("⚪"), 1, TdApi.TextEntityTypeCustomEmoji(11)),
        )
        messages(100L).add(0, UiMessage(9, 100, 2, 0, false, now - 2, text, entities = entities, viaBot = 2))
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
    run {
        val quote = "3g0th2002 - Topic - что ты хочешь_.m4a"
        val text = "$quote\nчто это"
        val entities = listOf(TdApi.TextEntity(0, quote.length, TdApi.TextEntityTypeBold()), TdApi.TextEntity(0, quote.length, TdApi.TextEntityTypeBlockQuote()))
        messages(100L).add(0, UiMessage(12, 100, 2, 0, false, now - 4, text, entities = entities))
    }
    run {
        val code = "import random\n\n# pick a cat\ndef cat(names):\n    return random.choice(names) + \" says hi\"\n\nprint(cat([\"Tom\", \"Luna\"]), 42)"
        val text = "Here is some Python:\n$code"
        val entities = listOf(TdApi.TextEntity(text.indexOf(code), code.length, TdApi.TextEntityTypePreCode("python")))
        messages(100L).add(0, UiMessage(11, 100, 2, 0, false, now - 1, text, entities = entities))
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


private class DemoTrack(val title: String, val performer: String, val duration: Int, val cover: String)

private val DemoTracks = mapOf(
    1L to listOf(
        DemoTrack("Midnight Shapes", "Expressive Hours", 214, "c1"),
        DemoTrack("Tonal Palette", "Material Waves", 187, "c2"),
        DemoTrack("Squircle", "The Corners", 242, "c3"),
        DemoTrack("Cookie 9", "Shape Morph", 199, "c4"),
    ),
    2L to listOf(
        DemoTrack("Ocean Blue", "Alice & the Pixels", 231, "c2"),
        DemoTrack("Sunny Side", "Alice & the Pixels", 176, "c3"),
    ),
)

fun Telegram.demoProfileAudios(userId: Long): List<TdApi.Audio> {
    val covers = System.getProperty("pixelgram.demo.covers")
    val audioPath = System.getProperty("pixelgram.demo.audio")
    return DemoTracks[userId].orEmpty().mapIndexed { index, track ->
        val coverPath = covers?.let { "$it/${track.cover}.jpg" }
        TdApi.Audio().apply {
            title = track.title
            performer = track.performer
            duration = track.duration
            fileName = "${track.title}.mp3"
            mimeType = "audio/mpeg"
            albumCoverThumbnail = TdApi.Thumbnail(TdApi.ThumbnailFormatJpeg(), 320, 320, demoFile(910_000 + userId.toInt() * 10 + index, coverPath, 20_000))
            audio = demoFile(920_000 + userId.toInt() * 10 + index, audioPath, 3_000_000)
        }
    }
}

fun demoSessions(): List<TdApi.Session> {
    val now = (System.currentTimeMillis() / 1000).toInt()
    fun session(id: Long, current: Boolean, app: String, version: String, device: String, platform: String, system: String, type: TdApi.SessionDeviceType, official: Boolean, ago: Int) =
        TdApi.Session().apply {
            this.id = id
            isCurrent = current
            applicationName = app
            applicationVersion = version
            deviceModel = device
            this.platform = platform
            systemVersion = system
            deviceType = type
            isOfficialApplication = official
            logInDate = now - 86_400
            lastActiveDate = now - ago
            location = "Dnipro, Ukraine"
        }
    return listOf(
        session(1, true, "pixelGram", "0.17.0", "PRIME H510M-K", "Windows", "11", TdApi.SessionDeviceTypeWindows(), false, 0),
        session(2, false, "Telegram Android", "12.1.0", "Google Pixel 7a", "Android", "16", TdApi.SessionDeviceTypeAndroid(), true, 600),
        session(3, false, "pixelGram", "pixelGram 0.12.0", "Desktop", "Linux", "6.18", TdApi.SessionDeviceTypeLinux(), false, 7_200),
        session(4, false, "Telegram Desktop", "6.2.4 x64", "M5506NA", "Windows", "11 x64", TdApi.SessionDeviceTypeWindows(), true, 90_000),
    )
}

private val demoStoryDir: String? get() = System.getProperty("pixelgram.demo.stories")

fun demoStories(chatId: Long, album: Int = 0): List<TdApi.Story> {
    val dir = demoStoryDir ?: return emptyList()
    val now = (System.currentTimeMillis() / 1000).toInt()
    return (1..8).filter { album == 0 || it % album == 0 }.map { index ->
        val file = demoFile(950_000 + index, "$dir/story$index.jpg", 120_000)
        TdApi.Story().apply {
            id = index
            posterChatId = chatId
            posterId = TdApi.MessageSenderUser(chatId)
            date = now - index * 86_400
            isPostedToChatPage = true
            content = if (index % 3 == 1) {
                TdApi.StoryContentVideo(
                    TdApi.StoryVideo().apply {
                        duration = 15.0 + index * 7
                        width = 720
                        height = 1280
                        thumbnail = TdApi.Thumbnail(TdApi.ThumbnailFormatJpeg(), 720, 1280, file)
                        video = demoFile(951_000 + index, null, 3_000_000)
                    },
                    null,
                )
            } else {
                TdApi.StoryContentPhoto(TdApi.Photo(false, null, arrayOf(TdApi.PhotoSize("x", file, 720, 1280, IntArray(0)))))
            }
            caption = TdApi.FormattedText(if (index == 2) "Material 3 Expressive everywhere" else "", emptyArray())
            albumIds = IntArray(0)
            canGetInteractions = chatId == 1L
            canBeReplied = chatId != 1L
            interactionInfo = TdApi.StoryInteractionInfo(12 + index, 0, 3, LongArray(0))
        }
    }
}

fun demoStoryAlbums(): List<TdApi.StoryAlbum> =
    if (demoStoryDir == null) emptyList() else listOf(TdApi.StoryAlbum(2, "banner 3x1", null, null), TdApi.StoryAlbum(3, "#photos", null, null))

fun demoGifts(collection: Int = 0): List<TdApi.ReceivedGift> {
    val dir = demoStoryDir ?: return emptyList()
    val animated = System.getProperty("pixelgram.demo.giftTgs")
    fun sticker(index: Int) = TdApi.Sticker().apply {
        id = 960_000L + index
        width = 256
        height = 256
        emoji = "🎁"
        format = if (animated != null && index > 2) TdApi.StickerFormatTgs() else TdApi.StickerFormatWebp()
        fullType = TdApi.StickerFullTypeRegular()
        sticker = demoFile(960_000 + index, if (animated != null && index > 2) animated else "$dir/gift$index.png", 40_000)
    }
    return (0 until 14).filter { collection == 0 || it % 3 == 0 }.map { i ->
        val index = i % 6 + 1
        TdApi.ReceivedGift().apply {
            receivedGiftId = "demo$i"
            senderId = TdApi.MessageSenderUser(if (i % 2 == 0) 2 else 3)
            text = TdApi.FormattedText(if (i == 0) "Happy birthday 🎉" else "", emptyArray())
            date = (System.currentTimeMillis() / 1000).toInt() - i * 86_400
            isSaved = i % 5 != 3
            sellStarCount = 13
            gift = if (i == 4) {
                TdApi.SentGiftUpgraded(TdApi.UpgradedGift().apply {
                    title = "Toy Bear"
                    name = "ToyBear-92639"
                    number = 92_639
                    totalUpgradedCount = 120_481
                    maxUpgradedCount = 150_000
                    ownerName = "Jetta"
                    model = TdApi.UpgradedGiftModel().apply { name = "Toy"; sticker = sticker(1); rarity = TdApi.UpgradedGiftAttributeRarityPerMille(15) }
                    symbol = TdApi.UpgradedGiftSymbol().apply { name = "Gem"; sticker = sticker(2); rarity = TdApi.UpgradedGiftAttributeRarityPerMille(4) }
                    backdrop = TdApi.UpgradedGiftBackdrop().apply {
                        name = "Desert"
                        colors = TdApi.UpgradedGiftBackdropColors(0xB08C6A, 0x6E4E36, 0x4E3624, 0xFFFFFF)
                        rarity = TdApi.UpgradedGiftAttributeRarityPerMille(20)
                    }
                })
            } else {
                TdApi.SentGiftRegular(TdApi.Gift().apply { sticker = sticker(index); starCount = 15; upgradeStarCount = 25 })
            }
            collectionIds = IntArray(0)
        }
    }
}

fun demoGiftCollections(): List<TdApi.GiftCollection> =
    if (demoStoryDir == null) emptyList() else listOf(TdApi.GiftCollection(1, "🧸 мишка фредди", null, 5))

fun demoUpgradedGift(): TdApi.UpgradedGift? =
    (demoGifts().firstOrNull { it.gift is TdApi.SentGiftUpgraded }?.gift as? TdApi.SentGiftUpgraded)?.gift

private const val DemoSupergroup = 5104L

private fun demoPermissions(all: Boolean) = TdApi.ChatPermissions().apply {
    canSendBasicMessages = true
    canSendPhotos = true
    canSendVideos = true
    canSendOtherMessages = true
    canSendAudios = all
    canSendDocuments = all
    canSendVoiceNotes = true
    canSendVideoNotes = true
    canAddLinkPreviews = true
    canSendPolls = all
    canInviteUsers = true
    canPinMessages = false
    canChangeInfo = false
    canCreateTopics = true
    canEditTag = true
}

private fun demoRights() = TdApi.ChatAdministratorRights().apply {
    canManageChat = true
    canChangeInfo = true
    canDeleteMessages = true
    canInviteUsers = true
    canRestrictMembers = true
    canPinMessages = true
    canManageTopics = true
    canPromoteMembers = true
    canManageVideoChats = true
    canManageTags = true
}

private fun Telegram.loadManageDemo(now: Int) {
    me?.let { users[it.id] = it }
    users[4] =TdApi.User().apply { id = 4; firstName = "Charlie"; lastName = "" }
    users[5] = TdApi.User().apply { id = 5; firstName = "Dana"; lastName = "" }
    users[6] = TdApi.User().apply { id = 6; firstName = "Spam"; lastName = "Bot" }
    chats[104L]?.apply {
        supergroupId = DemoSupergroup
        permissions = demoPermissions(all = false)
    }
    demoUpdate(TdApi.UpdateSupergroup(TdApi.Supergroup().apply {
        id = DemoSupergroup
        isForum = true
        hasForumTabs = true
        memberCount = 6
        status = TdApi.ChatMemberStatusAdministrator(false, demoRights())
    }))
    messages(104L).add(0, UiMessage(207, 104, 4, 0, false, now - 30, "Can I bring my cat? 🐈", senderTag = "cat lover"))
}

fun demoMembers(list: MemberList): List<TdApi.ChatMember> {
    val now = (System.currentTimeMillis() / 1000).toInt()
    fun member(userId: Long, tag: String, status: TdApi.ChatMemberStatus, days: Int) = TdApi.ChatMember().apply {
        memberId = TdApi.MessageSenderUser(userId)
        this.tag = tag
        joinedChatDate = now - days * 86_400
        this.status = status
    }
    val owner = member(2, "mom", TdApi.ChatMemberStatusCreator(false, true), 400)
    val me = member(1, "", TdApi.ChatMemberStatusAdministrator(false, demoRights()), 380)
    val chef = member(3, "chef", TdApi.ChatMemberStatusAdministrator(true, demoRights().apply { canPromoteMembers = false }), 300)
    val charlie = member(4, "cat lover", TdApi.ChatMemberStatusMember(), 40)
    val dana = member(5, "", TdApi.ChatMemberStatusRestricted(true, 0, demoPermissions(all = false).apply { canSendPhotos = false; canSendVideos = false }), 12)
    val spam = member(6, "", TdApi.ChatMemberStatusBanned(0), 2)
    return when (list) {
        MemberList.Recent -> listOf(owner, me, chef, charlie, dana)
        MemberList.Administrators -> listOf(owner, me, chef)
        MemberList.Banned -> listOf(spam)
        MemberList.Restricted -> listOf(dana)
    }
}

fun demoEvents(): List<TdApi.ChatEvent> {
    val now = (System.currentTimeMillis() / 1000).toInt()
    fun event(id: Long, userId: Long, ago: Int, action: TdApi.ChatEventAction) = TdApi.ChatEvent().apply {
        this.id = id
        date = now - ago
        memberId = TdApi.MessageSenderUser(userId)
        this.action = action
    }
    return listOf(
        event(6, 1, 300, TdApi.ChatEventMemberRestricted(TdApi.MessageSenderUser(6), TdApi.ChatMemberStatusMember(), TdApi.ChatMemberStatusBanned(0))),
        event(5, 1, 900, TdApi.ChatEventMemberTagChanged(4, "", "cat lover")),
        event(4, 2, 3_600, TdApi.ChatEventMemberPromoted(3, TdApi.ChatMemberStatusMember(), TdApi.ChatMemberStatusAdministrator(true, demoRights()))),
        event(3, 2, 7_200, TdApi.ChatEventPermissionsChanged(demoPermissions(all = true), demoPermissions(all = false))),
        event(2, 5, 86_400, TdApi.ChatEventMemberJoined()),
        event(1, 2, 172_800, TdApi.ChatEventTitleChanged("Family", "Family 👨‍👩‍👧")),
    )
}

private val demoCreatedTopics = mutableListOf<String>()

fun demoTopics(chatId: Long): List<TdApi.ForumTopic> {
    val names = listOf("General", "Dinner 🍕", "Trips ✈️", "Photos", "Birthdays 🎂") + demoCreatedTopics
    return names.mapIndexed { index, name ->
        TdApi.ForumTopic().apply {
            info = TdApi.ForumTopicInfo().apply {
                this.chatId = chatId
                forumTopicId = index + 1
                this.name = name
                icon = TdApi.ForumTopicIcon(TopicColors[index % TopicColors.size], 0)
                isGeneral = index == 0
            }
            unreadCount = if (index == 1) 3 else 0
            val previews = listOf("Welcome to the topic 👋", "Pizza for me 🍕", "Tickets are booked ✈️", "Look at this sunset 📷", "Cake is ordered 🎂")
            lastMessage = TdApi.Message().apply {
                id = (index + 1) * 1_000L + 3
                this.chatId = chatId
                senderId = TdApi.MessageSenderUser(if (index % 2 == 0) 2 else 4)
                content = TdApi.MessageText().apply { text = TdApi.FormattedText(previews.getOrElse(index) { "New topic" }, emptyArray()) }
            }
        }
    }
}

fun demoCreateTopic(chatId: Long, name: String) {
    if (name.isNotBlank()) demoCreatedTopics.add(name)
}

fun demoTopicHistory(chatId: Long, topicId: Int): List<UiMessage> {
    val now = (System.currentTimeMillis() / 1000).toInt()
    val base = topicId * 1_000L
    return when (topicId) {
        2 -> listOf(
            UiMessage(base + 3, chatId, 4, 0, false, now - 40, "Pizza for me 🍕", senderTag = "cat lover"),
            UiMessage(base + 2, chatId, 3, 0, false, now - 400, "I'll cook pasta tonight"),
            UiMessage(base + 1, chatId, 2, 0, false, now - 900, "What's for dinner?"),
        )
        else -> listOf(UiMessage(base + 1, chatId, 2, 0, false, now - 3_600, "Welcome to the topic 👋"))
    }
}

fun demoComments(chatId: Long): List<UiMessage> {
    val now = (System.currentTimeMillis() / 1000).toInt()
    return listOf(
        UiMessage(9_004, chatId, 3, 0, false, now - 60, "Prices will drop again by summer"),
        UiMessage(9_003, chatId, 2, 0, false, now - 120, "Source?"),
        UiMessage(9_002, chatId, 2, 0, false, now - 150, "That's a lot 😱"),
        UiMessage(9_001, chatId, 3, 0, false, now - 300, "First 🔥"),
    )
}

fun demoStoryViewers(): List<TdApi.StoryInteraction> {
    val now = (System.currentTimeMillis() / 1000).toInt()
    return listOf(2L to "❤", 3L to null, 4L to "🔥", 5L to null).mapIndexed { index, (userId, emoji) ->
        TdApi.StoryInteraction().apply {
            actorId = TdApi.MessageSenderUser(userId)
            interactionDate = now - (index + 1) * 900
            type = TdApi.StoryInteractionTypeView(emoji?.let { TdApi.ReactionTypeEmoji(it) })
        }
    }
}

private const val DemoMiniAppDelayMs = 12_000L
