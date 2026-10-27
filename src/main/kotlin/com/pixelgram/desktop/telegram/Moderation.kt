package com.pixelgram.desktop.telegram

import it.tdlight.jni.TdApi

enum class MemberList { Recent, Administrators, Banned, Restricted }

private const val ModerationPage = 200
private const val EventPage = 100
private const val TopicPage = 100

val TopicColors = intArrayOf(0x6FB9F0, 0xFFD67E, 0xCB86DB, 0x8EEE98, 0xFF93B2, 0xFB6F5F)

private val isDemo get() = System.getProperty("pixelgram.demo") == "true"

private fun <R : TdApi.Object> Telegram.call(function: TdApi.Function<R>, done: (String?) -> Unit) {
    if (isDemo) return done(null)
    send(function) { result -> done((result as? TdApi.Error)?.message?.let(::friendlyError)) }
}

fun Telegram.members(chat: ChatState, list: MemberList, query: String, done: (List<TdApi.ChatMember>) -> Unit) {
    val deliver = { members: List<TdApi.ChatMember> ->
        members.forEach { member -> (member.memberId as? TdApi.MessageSenderUser)?.let { ensureUser(it.userId) } }
        done(members)
    }
    if (isDemo) return deliver(demoMembers(list).filter { query.isBlank() || senderTitle(it.memberId).contains(query, ignoreCase = true) })
    when {
        chat.supergroupId != 0L -> {
            val filter: TdApi.SupergroupMembersFilter = when (list) {
                MemberList.Recent -> if (query.isBlank()) TdApi.SupergroupMembersFilterRecent() else TdApi.SupergroupMembersFilterSearch(query)
                MemberList.Administrators -> TdApi.SupergroupMembersFilterAdministrators()
                MemberList.Banned -> TdApi.SupergroupMembersFilterBanned(query)
                MemberList.Restricted -> TdApi.SupergroupMembersFilterRestricted(query)
            }
            send(TdApi.GetSupergroupMembers(chat.supergroupId, filter, 0, ModerationPage)) { result ->
                deliver((result as? TdApi.ChatMembers)?.members?.toList().orEmpty())
            }
        }
        chat.basicGroupId != 0L -> send(TdApi.GetBasicGroupFullInfo(chat.basicGroupId)) { result ->
            val all = (result as? TdApi.BasicGroupFullInfo)?.members?.toList().orEmpty()
            val picked = when (list) {
                MemberList.Recent -> all
                MemberList.Administrators -> all.filter { it.status is TdApi.ChatMemberStatusCreator || it.status is TdApi.ChatMemberStatusAdministrator }
                MemberList.Banned, MemberList.Restricted -> emptyList()
            }
            deliver(picked.filter { query.isBlank() || senderTitle(it.memberId).contains(query, ignoreCase = true) })
        }
        else -> done(emptyList())
    }
}

fun Telegram.chatMember(chatId: Long, member: TdApi.MessageSender, done: (TdApi.ChatMember?) -> Unit) {
    send(TdApi.GetChatMember(chatId, member)) { result -> done(result as? TdApi.ChatMember) }
}

fun Telegram.setMemberStatus(chat: ChatState, member: TdApi.MessageSender, status: TdApi.ChatMemberStatus, done: (String?) -> Unit) {
    call(TdApi.SetChatMemberStatus(chat.id, member, status)) { error ->
        refreshRoles(chat)
        done(error)
    }
}

fun Telegram.banMember(chat: ChatState, member: TdApi.MessageSender, deleteMessages: Boolean, done: (String?) -> Unit) {
    call(TdApi.BanChatMember(chat.id, member, 0, deleteMessages), done)
}

fun Telegram.removeMember(chat: ChatState, member: TdApi.MessageSender, done: (String?) -> Unit) {
    if (chat.supergroupId == 0L) return setMemberStatus(chat, member, TdApi.ChatMemberStatusLeft(), done)
    banMember(chat, member, deleteMessages = false) { error ->
        if (error != null) done(error) else setMemberStatus(chat, member, TdApi.ChatMemberStatusLeft(), done)
    }
}

fun Telegram.unbanMember(chat: ChatState, member: TdApi.MessageSender, done: (String?) -> Unit) =
    setMemberStatus(chat, member, TdApi.ChatMemberStatusLeft(), done)

fun Telegram.setMemberTag(chat: ChatState, userId: Long, tag: String, done: (String?) -> Unit) {
    call(TdApi.SetChatMemberTag(chat.id, userId, tag.trim())) { error ->
        refreshRoles(chat)
        done(error)
    }
}

fun Telegram.setChatPermissions(chatId: Long, permissions: TdApi.ChatPermissions, done: (String?) -> Unit) {
    call(TdApi.SetChatPermissions(chatId, permissions)) { error ->
        if (error == null && isDemo) chats[chatId]?.permissions = permissions
        done(error)
    }
}

fun Telegram.slowModeDelay(chat: ChatState, done: (Int) -> Unit) {
    if (chat.supergroupId == 0L || isDemo) return done(0)
    send(TdApi.GetSupergroupFullInfo(chat.supergroupId)) { result -> done((result as? TdApi.SupergroupFullInfo)?.slowModeDelay ?: 0) }
}

fun Telegram.setSlowModeDelay(chatId: Long, seconds: Int, done: (String?) -> Unit) {
    call(TdApi.SetChatSlowModeDelay(chatId, seconds), done)
}

fun Telegram.setChatTitle(chatId: Long, title: String, done: (String?) -> Unit) {
    call(TdApi.SetChatTitle(chatId, title.trim()), done)
}

fun Telegram.setChatDescription(chatId: Long, description: String, done: (String?) -> Unit) {
    call(TdApi.SetChatDescription(chatId, description.trim()), done)
}

fun Telegram.eventLog(chatId: Long, fromEventId: Long, done: (List<TdApi.ChatEvent>) -> Unit) {
    if (isDemo) return done(if (fromEventId == 0L) demoEvents() else emptyList())
    send(TdApi.GetChatEventLog(chatId, "", fromEventId, EventPage, null, LongArray(0))) { result ->
        val events = (result as? TdApi.ChatEvents)?.events?.toList().orEmpty()
        events.forEach { event -> (event.memberId as? TdApi.MessageSenderUser)?.let { ensureUser(it.userId) } }
        done(events)
    }
}

fun Telegram.setForum(chat: ChatState, enabled: Boolean, done: (String?) -> Unit) {
    if (chat.supergroupId == 0L) return done("Topics need a supergroup")
    call(TdApi.ToggleSupergroupIsForum(chat.supergroupId, enabled, enabled), done)
}

fun Telegram.forumTopics(chatId: Long, done: (List<TdApi.ForumTopic>) -> Unit) {
    if (isDemo) return done(demoTopics(chatId))
    send(TdApi.GetForumTopics(chatId, "", 0, 0, 0, TopicPage)) { result ->
        done((result as? TdApi.ForumTopics)?.topics?.toList().orEmpty())
    }
}

fun Telegram.createForumTopic(chatId: Long, name: String, done: (String?) -> Unit) {
    val title = name.trim()
    if (isDemo) demoCreateTopic(chatId, title)
    call(TdApi.CreateForumTopic().apply {
        this.chatId = chatId
        this.name = title
        icon = TdApi.ForumTopicIcon(TopicColors[Math.floorMod(title.hashCode(), TopicColors.size)], 0)
    }, done)
}

fun Telegram.canManage(chat: ChatState): Boolean = when (myStatus(chat)) {
    is TdApi.ChatMemberStatusCreator -> true
    is TdApi.ChatMemberStatusAdministrator -> true
    else -> false
}

fun Telegram.myRights(chat: ChatState): TdApi.ChatAdministratorRights? = when (val status = myStatus(chat)) {
    is TdApi.ChatMemberStatusCreator -> null
    is TdApi.ChatMemberStatusAdministrator -> status.rights
    else -> null
}

fun Telegram.isOwner(chat: ChatState): Boolean = myStatus(chat) is TdApi.ChatMemberStatusCreator

fun Telegram.can(chat: ChatState, right: (TdApi.ChatAdministratorRights) -> Boolean): Boolean =
    isOwner(chat) || myRights(chat)?.let(right) == true
