package com.pixelgram.desktop.ui.main

import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.telegram.ChatListKey
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.telegram.UnreadCount

object FolderLayout {
    const val MainToken = "main"
    const val UnreadToken = "unread"
    const val MentionsToken = "mentions"
    private const val FolderPrefix = "f"
    val ClientIcons = setOf("Flag", "Mention")

    fun token(key: ChatListKey): String = when (key) {
        ChatListKey.Unread -> UnreadToken
        ChatListKey.Mentions -> MentionsToken
        is ChatListKey.Folder -> FolderPrefix + key.id
        else -> MainToken
    }

    fun key(token: String): ChatListKey? = when {
        token == MainToken -> ChatListKey.Main
        token == UnreadToken -> ChatListKey.Unread
        token == MentionsToken -> ChatListKey.Mentions
        token.startsWith(FolderPrefix) -> token.drop(1).toIntOrNull()?.let { ChatListKey.Folder(it) }
        else -> null
    }

    fun order(telegram: Telegram, settings: AppSettings): List<ChatListKey> {
        val all = buildList {
            add(ChatListKey.Main)
            telegram.folders.forEach { add(ChatListKey.Folder(it.id)) }
            add(ChatListKey.Unread)
            add(ChatListKey.Mentions)
        }
        val saved = settings.folderOrder.value.split(',').mapNotNull { key(it.trim()) }.filter { it in all }.distinct()
        val missing = all.filter { it !in saved }
        val result = saved.toMutableList()
        missing.forEach { key ->
            val before = all.indexOf(key)
            val anchor = all.take(before).lastOrNull { it in result }
            result.add(if (anchor == null) 0 else result.indexOf(anchor) + 1, key)
        }
        return result
    }

    fun saveOrder(settings: AppSettings, keys: List<ChatListKey>) {
        settings.folderOrder.value = keys.joinToString(",") { token(it) }
    }

    fun iconOverrides(settings: AppSettings): Map<Int, String> =
        settings.folderIcons.value.split(',').mapNotNull { entry ->
            val (id, icon) = entry.split('=').takeIf { it.size == 2 } ?: return@mapNotNull null
            id.toIntOrNull()?.let { it to icon }
        }.toMap()

    fun setIconOverride(settings: AppSettings, folderId: Int, icon: String?) {
        val map = iconOverrides(settings).toMutableMap()
        if (icon == null) map.remove(folderId) else map[folderId] = icon
        settings.folderIcons.value = map.entries.joinToString(",") { "${it.key}=${it.value}" }
    }

    internal fun tabs(telegram: Telegram, settings: AppSettings): List<FolderTab> {
        val overrides = iconOverrides(settings)
        val byId = telegram.folders.associateBy { it.id }
        return order(telegram, settings).mapNotNull { key ->
            when (key) {
                ChatListKey.Main -> if (!settings.hideAllChats.value || telegram.folders.isEmpty()) {
                    FolderTab(key, com.pixelgram.desktop.core.tr("All chats"), "All", telegram.unread[ChatListKey.Main])
                } else {
                    null
                }
                ChatListKey.Unread -> if (settings.unreadFolder.value) {
                    FolderTab(key, com.pixelgram.desktop.core.tr(settings.unreadFolderName.value), settings.unreadFolderIcon.value, count(telegram, key))
                } else {
                    null
                }
                ChatListKey.Mentions -> if (settings.mentionsFolder.value) {
                    FolderTab(key, com.pixelgram.desktop.core.tr(settings.mentionsFolderName.value), settings.mentionsFolderIcon.value, count(telegram, key))
                } else {
                    null
                }
                is ChatListKey.Folder -> byId[key.id]?.let { info ->
                    FolderTab(key, info.title, overrides[info.id] ?: info.icon, telegram.unread[key])
                }
                else -> null
            }
        }
    }

    private fun count(telegram: Telegram, key: ChatListKey): UnreadCount {
        val chats = telegram.chatList(key)
        return UnreadCount(chats.size, chats.count { !telegram.isMuted(it) })
    }
}
