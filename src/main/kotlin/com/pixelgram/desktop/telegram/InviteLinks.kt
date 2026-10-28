package com.pixelgram.desktop.telegram

import it.tdlight.jni.TdApi

private const val LinkPage = 100

private val isDemo get() = System.getProperty("pixelgram.demo") == "true"

private val demoLinks = mutableListOf(
    TdApi.ChatInviteLink("https://t.me/+pixelPrimaryLink", "", 1, 1_700_000_000, 0, 0, null, 0, 42, 3, 0, false, true, false),
    TdApi.ChatInviteLink("https://t.me/+pixelFriends", "Friends", 1, 1_710_000_000, 0, 0, null, 50, 12, 1, 0, false, false, false),
    TdApi.ChatInviteLink("https://t.me/+pixelApproval", "Requests", 1, 1_720_000_000, 0, 0, null, 0, 4, 0, 2, true, false, false),
    TdApi.ChatInviteLink("https://t.me/+pixelOld", "Old link", 1, 1_690_000_000, 0, 0, null, 0, 7, 2, 0, false, false, true),
)

fun Telegram.inviteLinks(chat: ChatState, revoked: Boolean, done: (List<TdApi.ChatInviteLink>) -> Unit) {
    if (isDemo) return done(demoLinks.filter { it.isRevoked == revoked })
    val me = me?.id ?: return done(emptyList())
    send(TdApi.GetChatInviteLinks(chat.id, me, revoked, 0, "", LinkPage)) { result ->
        done((result as? TdApi.ChatInviteLinks)?.inviteLinks?.filterNotNull().orEmpty())
    }
}

fun Telegram.createInviteLink(chat: ChatState, name: String, expiration: Int, limit: Int, requests: Boolean, done: (String?) -> Unit) {
    if (isDemo) {
        demoLinks.add(TdApi.ChatInviteLink("https://t.me/+pixelNew${demoLinks.size}", name, 1, (System.currentTimeMillis() / 1000).toInt(), 0, expiration, null, limit, 0, 0, 0, requests, false, false))
        return done(null)
    }
    send(TdApi.CreateChatInviteLink(chat.id, name, expiration, if (requests) 0 else limit, requests)) { result ->
        done((result as? TdApi.Error)?.message?.let(::friendlyError))
    }
}

fun Telegram.editInviteLink(chat: ChatState, link: TdApi.ChatInviteLink, name: String, expiration: Int, limit: Int, requests: Boolean, done: (String?) -> Unit) {
    if (isDemo) {
        demoLinks.indexOfFirst { it.inviteLink == link.inviteLink }.takeIf { it >= 0 }?.let { index ->
            demoLinks[index] = TdApi.ChatInviteLink(link.inviteLink, name, link.creatorUserId, link.date, 0, expiration, null, limit, link.memberCount, link.expiredMemberCount, 0, requests, link.isPrimary, false)
        }
        return done(null)
    }
    send(TdApi.EditChatInviteLink(chat.id, link.inviteLink, name, expiration, if (requests) 0 else limit, requests)) { result ->
        done((result as? TdApi.Error)?.message?.let(::friendlyError))
    }
}

fun Telegram.revokeInviteLink(chat: ChatState, link: TdApi.ChatInviteLink, done: (String?) -> Unit) {
    if (isDemo) {
        demoLinks.indexOfFirst { it.inviteLink == link.inviteLink }.takeIf { it >= 0 }?.let { index ->
            demoLinks[index] = demoLinks[index].apply { isRevoked = true }
        }
        if (link.isPrimary) demoLinks.add(0, TdApi.ChatInviteLink("https://t.me/+pixelPrimary${demoLinks.size}", "", 1, (System.currentTimeMillis() / 1000).toInt(), 0, 0, null, 0, 0, 0, 0, false, true, false))
        return done(null)
    }
    val report = { result: TdApi.Object -> done((result as? TdApi.Error)?.message?.let(::friendlyError)) }
    if (link.isPrimary) send(TdApi.ReplacePrimaryChatInviteLink(chat.id), report) else send(TdApi.RevokeChatInviteLink(chat.id, link.inviteLink), report)
}

fun Telegram.deleteRevokedLink(chat: ChatState, link: TdApi.ChatInviteLink, done: (String?) -> Unit) {
    if (isDemo) {
        demoLinks.removeAll { it.inviteLink == link.inviteLink }
        return done(null)
    }
    send(TdApi.DeleteRevokedChatInviteLink(chat.id, link.inviteLink)) { result ->
        done((result as? TdApi.Error)?.message?.let(::friendlyError))
    }
}

fun Telegram.canInvite(chat: ChatState): Boolean =
    isOwner(chat) || myRights(chat)?.canInviteUsers == true
