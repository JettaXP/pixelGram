package com.pixelgram.desktop.telegram

import it.tdlight.jni.TdApi

private val isDemo get() = System.getProperty("pixelgram.demo") == "true"

private fun failure(result: TdApi.Object): String? = (result as? TdApi.Error)?.message?.let(::friendlyError)

val DefaultChatReactions = listOf(
    "👍", "👎", "❤", "🔥", "🥰", "👏", "😁", "🤔", "🤯", "😱", "🤬", "😢", "🎉", "🤩", "🤮", "💩",
    "🙏", "👌", "🕊", "🤡", "🥱", "🥴", "😍", "🐳", "❤‍🔥", "🌚", "🌭", "💯", "🤣", "⚡", "🍌", "🏆",
)

private var demoReactions: TdApi.ChatAvailableReactions = TdApi.ChatAvailableReactionsAll(0)
private val demoLinked = HashMap<Long, Long>()

fun Telegram.chatReactions(chatId: Long, done: (TdApi.ChatAvailableReactions?) -> Unit) {
    if (isDemo) return done(demoReactions)
    send(TdApi.GetChat(chatId)) { result -> done((result as? TdApi.Chat)?.availableReactions) }
}

fun Telegram.setChatReactions(chatId: Long, reactions: TdApi.ChatAvailableReactions, done: (String?) -> Unit) {
    if (isDemo) {
        demoReactions = reactions
        return done(null)
    }
    send(TdApi.SetChatAvailableReactions(chatId, reactions)) { done(failure(it)) }
}

fun Telegram.linkedChat(chat: ChatState, done: (Long) -> Unit) {
    if (isDemo) return done(demoLinked[chat.id] ?: 0L)
    if (chat.supergroupId == 0L) return done(0L)
    send(TdApi.GetSupergroupFullInfo(chat.supergroupId)) { result -> done((result as? TdApi.SupergroupFullInfo)?.linkedChatId ?: 0L) }
}

fun Telegram.discussionCandidates(done: (List<Long>) -> Unit) {
    if (isDemo) return done(chats.values.filter { !it.isChannel && it.privateUserId == 0L }.map { it.id })
    send(TdApi.GetSuitableDiscussionChats()) { result ->
        val ids = (result as? TdApi.Chats)?.chatIds?.toList().orEmpty()
        ids.forEach(::loadChat)
        done(ids)
    }
}

fun Telegram.setDiscussionGroup(channelId: Long, groupId: Long, done: (String?) -> Unit) {
    if (isDemo) {
        if (groupId == 0L) demoLinked.remove(channelId) else demoLinked[channelId] = groupId
        return done(null)
    }
    send(TdApi.SetChatDiscussionGroup(channelId, groupId)) { done(failure(it)) }
}

fun Telegram.chatStatistics(chatId: Long, dark: Boolean, done: (TdApi.ChatStatistics?, String?) -> Unit) {
    if (isDemo) {
        return done(
            TdApi.ChatStatisticsChannel().apply {
                memberCount = TdApi.StatisticalValue(12_480.0, 11_900.0, 4.87)
                meanMessageViewCount = TdApi.StatisticalValue(5_210.0, 5_600.0, -6.96)
                meanMessageShareCount = TdApi.StatisticalValue(84.0, 61.0, 37.7)
                meanMessageReactionCount = TdApi.StatisticalValue(312.0, 280.0, 11.4)
                meanStoryViewCount = TdApi.StatisticalValue(2_040.0, 1_990.0, 2.5)
                meanStoryShareCount = TdApi.StatisticalValue(12.0, 15.0, -20.0)
                meanStoryReactionCount = TdApi.StatisticalValue(95.0, 70.0, 35.7)
                enabledNotificationsPercentage = 41.3
            },
            null,
        )
    }
    send(TdApi.GetChatStatistics(chatId, dark)) { result ->
        done(result as? TdApi.ChatStatistics, failure(result))
    }
}

fun Telegram.boostStatus(chatId: Long, done: (TdApi.ChatBoostStatus?, String?) -> Unit) {
    if (isDemo) {
        return done(
            TdApi.ChatBoostStatus().apply {
                boostUrl = "https://t.me/boost/pixelgramclient"
                appliedSlotIds = IntArray(0)
                level = 2
                boostCount = 14
                currentLevelBoostCount = 10
                nextLevelBoostCount = 20
                premiumMemberCount = 870
                premiumMemberPercentage = 6.9
                prepaidGiveaways = emptyArray()
            },
            null,
        )
    }
    send(TdApi.GetChatBoostStatus(chatId)) { result -> done(result as? TdApi.ChatBoostStatus, failure(result)) }
}

fun Telegram.boostChat(chatId: Long, done: (String?) -> Unit) {
    if (isDemo) return done(null)
    send(TdApi.GetAvailableChatBoostSlots()) { result ->
        val slots = (result as? TdApi.ChatBoostSlots)?.slots.orEmpty()
        if (slots.isEmpty()) return@send done(failure(result) ?: "Boosting needs Telegram Premium")
        val now = (System.currentTimeMillis() / 1000).toInt()
        if (slots.any { it.currentlyBoostedChatId == chatId }) return@send done("You already boost this chat")
        val slot = slots.firstOrNull { it.currentlyBoostedChatId == 0L }
            ?: slots.filter { it.cooldownUntilDate <= now }.minByOrNull { it.startDate }
            ?: return@send done("All your boosts are on cooldown")
        send(TdApi.BoostChat(chatId, intArrayOf(slot.slotId))) { done(failure(it)) }
    }
}

enum class StoryAudience(val title: String) { Everyone("Everyone"), Contacts("My contacts"), CloseFriends("Close friends") }

fun Telegram.postStory(
    chatId: Long,
    file: java.io.File,
    video: Boolean,
    duration: Double,
    caption: String,
    audience: StoryAudience,
    hours: Int,
    keepOnPage: Boolean,
    done: (String?) -> Unit,
) {
    if (isDemo) return done(null)
    val content = if (video) {
        TdApi.InputStoryContentVideo(TdApi.InputFileLocal(file.absolutePath), IntArray(0), duration, 0.0, false)
    } else {
        TdApi.InputStoryContentPhoto(TdApi.InputFileLocal(file.absolutePath), IntArray(0))
    }
    val privacy = when (audience) {
        StoryAudience.Everyone -> TdApi.StoryPrivacySettingsEveryone(LongArray(0))
        StoryAudience.Contacts -> TdApi.StoryPrivacySettingsContacts(LongArray(0))
        StoryAudience.CloseFriends -> TdApi.StoryPrivacySettingsCloseFriends()
    }
    send(TdApi.CanPostStory(chatId)) { check ->
        val blocked = when (check) {
            is TdApi.CanPostStoryResultPremiumNeeded -> "Posting more stories needs Telegram Premium"
            is TdApi.CanPostStoryResultBoostNeeded -> "The channel needs more boosts to post stories"
            is TdApi.CanPostStoryResultActiveStoryLimitExceeded -> "Too many active stories"
            is TdApi.CanPostStoryResultWeeklyLimitExceeded -> "Weekly story limit reached"
            is TdApi.CanPostStoryResultMonthlyLimitExceeded -> "Monthly story limit reached"
            is TdApi.Error -> failure(check)
            else -> null
        }
        if (blocked != null) return@send done(blocked)
        send(
            TdApi.PostStory(
                chatId,
                content,
                null,
                TdApi.FormattedText(caption, emptyArray()),
                privacy,
                IntArray(0),
                hours * SecondsInHour,
                null,
                keepOnPage,
                false,
            ),
        ) { done(failure(it)) }
    }
}

private const val SecondsInHour = 3600

fun Telegram.joinByRequest(chat: ChatState): Boolean = supergroupOf(chat)?.joinByRequest == true

fun Telegram.setJoinByRequest(chat: ChatState, enabled: Boolean, done: (String?) -> Unit) {
    if (isDemo) {
        supergroupOf(chat)?.joinByRequest = enabled
        return done(null)
    }
    send(TdApi.ToggleSupergroupJoinByRequest(chat.supergroupId, enabled, 0L, false)) { done(failure(it)) }
}

fun Telegram.joinRequests(chatId: Long, done: (List<TdApi.ChatJoinRequest>) -> Unit) {
    if (isDemo) return done(listOf(TdApi.ChatJoinRequest(2L, (System.currentTimeMillis() / 1000).toInt() - 600, "Hi, I'd like to join")))
    send(TdApi.GetChatJoinRequests(chatId, "", "", null, JoinRequestsPage)) { result ->
        val list = (result as? TdApi.ChatJoinRequests)?.requests?.toList().orEmpty()
        list.forEach { ensureUser(it.userId) }
        done(list)
    }
}

fun Telegram.answerJoinRequest(chatId: Long, userId: Long, approve: Boolean, done: (String?) -> Unit) {
    if (isDemo) return done(null)
    send(TdApi.ProcessChatJoinRequest(chatId, userId, approve)) { done(failure(it)) }
}

private const val JoinRequestsPage = 100
