package com.pixelgram.desktop.telegram

import it.tdlight.jni.TdApi

const val DemoVideoChatId = 501
const val DemoLiveStreamId = 502
private const val DemoNewCallId = 503
private const val DemoVideoChat = 104L
private const val DemoChannel = 106L

private val isDemo get() = System.getProperty("pixelgram.demo") == "true"

fun Telegram.canManageVideoChats(chat: ChatState): Boolean {
    if (!chat.isGroup || chat.privateUserId != 0L) return false
    if (isDemo) return true
    return when (val status = myStatus(chat)) {
        is TdApi.ChatMemberStatusCreator -> true
        is TdApi.ChatMemberStatusAdministrator -> status.rights?.canManageVideoChats == true
        else -> false
    }
}

fun Telegram.activeVideoChat(chat: ChatState): TdApi.GroupCall? {
    val id = chat.videoChat?.groupCallId ?: 0
    if (id == 0) return null
    return groupCall(id)?.takeIf { it.isActive }
}

fun Telegram.startVideoChat(chat: ChatState, rtmp: Boolean, done: (error: String?, groupCallId: Int) -> Unit) {
    if (isDemo) {
        groupCalls[DemoNewCallId] = demoGroupCall(DemoNewCallId, participants = 0, stream = rtmp)
        chat.videoChat = TdApi.VideoChat(DemoNewCallId, false, null)
        return done(null, DemoNewCallId)
    }
    send(TdApi.CreateVideoChat(chat.id, "", 0, rtmp)) { result ->
        when (result) {
            is TdApi.GroupCallId -> {
                send(TdApi.GetGroupCall(result.id)) { call -> if (call is TdApi.GroupCall) groupCalls[result.id] = call }
                done(null, result.id)
            }
            is TdApi.Error -> done(friendlyError(result.message), 0)
            else -> done(null, 0)
        }
    }
}

fun Telegram.rtmpUrl(chat: ChatState, onResult: (TdApi.RtmpUrl?) -> Unit) {
    if (isDemo) return onResult(TdApi.RtmpUrl("rtmps://dc4-1.rtmp.t.me/s/", "1234567890:AbCdEfGhIjKlMnOp"))
    send(TdApi.GetVideoChatRtmpUrl(chat.id)) { result -> onResult(result as? TdApi.RtmpUrl) }
}

internal fun Telegram.endDemoVideoChat(groupCallId: Int) {
    groupCalls[groupCallId]?.let { groupCalls[groupCallId] = demoGroupCall(groupCallId, 0, it.isRtmpStream).apply { isActive = false } }
    chats.values.filter { it.videoChat?.groupCallId == groupCallId }.forEach { it.videoChat = TdApi.VideoChat(0, false, null) }
}

private fun demoGroupCall(id: Int, participants: Int, stream: Boolean, speakers: List<Long> = emptyList()): TdApi.GroupCall = TdApi.GroupCall().apply {
    this.id = id
    title = if (stream) "Evening news" else ""
    inviteLink = ""
    isActive = true
    isVideoChat = true
    isRtmpStream = stream
    canBeManaged = true
    participantCount = participants
    loadedAllParticipants = true
    recentSpeakers = speakers.map { TdApi.GroupCallRecentSpeaker(TdApi.MessageSenderUser(it), it == speakers.first()) }.toTypedArray()
    canEnableVideo = true
    duration = 0
}

internal fun Telegram.loadVideoChatDemo() {
    users[4] = TdApi.User().apply { id = 4; firstName = "Carol"; lastName = "" }
    users[5] = TdApi.User().apply { id = 5; firstName = "Dave"; lastName = "" }
    users[6] = TdApi.User().apply { id = 6; firstName = "Eve"; lastName = "" }
    groupCalls[DemoVideoChatId] = demoGroupCall(DemoVideoChatId, participants = 6, stream = false, speakers = listOf(2L, 3L, 5L))
    groupCalls[DemoLiveStreamId] = demoGroupCall(DemoLiveStreamId, participants = 1284, stream = true)
    chats[DemoVideoChat]?.videoChat = TdApi.VideoChat(DemoVideoChatId, true, null)
    chats[DemoChannel]?.apply {
        isChannel = true
        isGroup = true
        videoChat = TdApi.VideoChat(DemoLiveStreamId, true, null)
    }
}
