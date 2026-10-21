package com.pixelgram.desktop.telegram

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.pixelgram.desktop.BuildConfig
import com.pixelgram.desktop.core.AppPaths
import com.pixelgram.desktop.core.AppSettings
import it.tdlight.ClientFactory
import it.tdlight.ExceptionHandler
import it.tdlight.Init
import it.tdlight.ResultHandler
import it.tdlight.TelegramClient
import it.tdlight.jni.TdApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

sealed interface AuthState {
    data object Starting : AuthState
    data object NeedApiCredentials : AuthState
    data object WaitPhone : AuthState
    data class WaitQr(val link: String) : AuthState
    data object WaitCode : AuthState
    data class WaitPassword(val hint: String) : AuthState
    data object Ready : AuthState
    data object Closing : AuthState
    data class Unsupported(val name: String) : AuthState
    data class Failed(val reason: String) : AuthState
}

// Chat list key: main list, archive or a chat folder.
sealed interface ChatListKey {
    data object Main : ChatListKey
    data object Archive : ChatListKey
    data class Folder(val id: Int) : ChatListKey

    companion object {
        fun of(list: TdApi.ChatList?): ChatListKey? = when (list) {
            is TdApi.ChatListMain -> Main
            is TdApi.ChatListArchive -> Archive
            is TdApi.ChatListFolder -> Folder(list.chatFolderId)
            else -> null
        }
    }
}

class ChatState(val id: Long) {
    var title by mutableStateOf("")
    var photo by mutableStateOf<TdApi.File?>(null)
    var lastMessage by mutableStateOf<TdApi.Message?>(null)
    var unreadCount by mutableStateOf(0)
    var isGroup by mutableStateOf(false)
    val positions = mutableStateMapOf<ChatListKey, Long>()
}

data class FolderInfo(val id: Int, val title: String)

// Immutable snapshot of a message for the UI; TdApi objects are mutable.
data class UiMessage(
    val id: Long,
    val chatId: Long,
    val senderUserId: Long,
    val senderChatId: Long,
    val isOutgoing: Boolean,
    val date: Int,
    val text: String,
)

class Telegram(
    private val settings: AppSettings,
    private val scope: CoroutineScope,
) {
    var authState by mutableStateOf<AuthState>(AuthState.Starting)
        private set
    var authError by mutableStateOf<String?>(null)

    val chats = mutableStateMapOf<Long, ChatState>()
    val users = mutableStateMapOf<Long, TdApi.User>()
    val folders = mutableStateListOf<FolderInfo>()
    private val filePaths = mutableStateMapOf<Int, String>()
    private val messages = mutableMapOf<Long, SnapshotStateList<UiMessage>>()
    private val requestedFiles = mutableSetOf<Int>()

    private var factory: ClientFactory? = null
    private var client: TelegramClient? = null
    private var myUserId = 0L

    fun start() {
        try {
            Init.init()
        } catch (e: Throwable) {
            authState = AuthState.Failed("TDLib native library failed to load: ${e.message}")
            return
        }
        factory = ClientFactory.create()
        createClient()
    }

    private fun createClient() {
        val client = factory?.createClient() ?: return
        this.client = client
        client.initialize(
            ResultHandler<TdApi.Update> { update -> onMain { handleUpdate(update) } },
            ExceptionHandler { it.printStackTrace() },
            ExceptionHandler { it.printStackTrace() },
        )
    }

    private var exiting = false

    fun close() {
        exiting = true
        send(TdApi.Close())
    }

    private fun onMain(block: () -> Unit) {
        scope.launch(Dispatchers.Main) { block() }
    }

    fun <R : TdApi.Object> send(
        function: TdApi.Function<R>,
        onResult: (TdApi.Object) -> Unit = {},
    ) {
        val client = client ?: return
        client.send(
            function,
            ResultHandler<R> { result -> onMain { onResult(result) } },
            ExceptionHandler { it.printStackTrace() },
        )
    }

    private fun sendAuth(function: TdApi.Function<TdApi.Ok>) {
        authError = null
        send(function) { result ->
            if (result is TdApi.Error) {
                authError = result.message
            }
        }
    }

    // Authorization.

    private val apiId: Int
        get() = if (BuildConfig.API_ID != 0) BuildConfig.API_ID else settings.apiId.value

    private val apiHash: String
        get() = BuildConfig.API_HASH.ifEmpty { settings.apiHash.value }

    fun setApiCredentials(id: Int, hash: String) {
        settings.apiId.value = id
        settings.apiHash.value = hash.trim()
        sendTdlibParameters()
    }

    private fun sendTdlibParameters() {
        if (apiId == 0 || apiHash.isEmpty()) {
            authState = AuthState.NeedApiCredentials
            return
        }
        val parameters = TdApi.SetTdlibParameters().apply {
            useTestDc = false
            databaseDirectory = AppPaths.tdlibDatabase.absolutePath
            filesDirectory = AppPaths.tdlibFiles.absolutePath
            databaseEncryptionKey = ByteArray(0)
            useFileDatabase = true
            useChatInfoDatabase = true
            useMessageDatabase = true
            useSecretChats = false
            apiId = this@Telegram.apiId
            apiHash = this@Telegram.apiHash
            systemLanguageCode = System.getProperty("user.language") ?: "en"
            deviceModel = "Desktop"
            systemVersion = "${System.getProperty("os.name")} ${System.getProperty("os.version")}"
            applicationVersion = "pixelGram ${BuildConfig.VERSION}"
        }
        sendAuth(parameters)
    }

    fun sendPhone(phone: String) = sendAuth(TdApi.SetAuthenticationPhoneNumber().apply {
        phoneNumber = phone.filter { it.isDigit() || it == '+' }
    })

    fun requestQr() = sendAuth(TdApi.RequestQrCodeAuthentication().apply {
        otherUserIds = LongArray(0)
    })

    fun sendCode(code: String) = sendAuth(TdApi.CheckAuthenticationCode().apply {
        this.code = code.trim()
    })

    fun sendPassword(password: String) = sendAuth(TdApi.CheckAuthenticationPassword().apply {
        this.password = password
    })

    fun logOut() = send(TdApi.LogOut())

    // Updates.

    private fun handleUpdate(update: TdApi.Object) {
        when (update) {
            is TdApi.UpdateAuthorizationState -> handleAuthState(update.authorizationState)
            is TdApi.UpdateNewChat -> addChat(update.chat)
            is TdApi.UpdateChatTitle -> chats[update.chatId]?.title = update.title
            is TdApi.UpdateChatPhoto -> chats[update.chatId]?.photo = update.photo?.small
            is TdApi.UpdateChatLastMessage -> chats[update.chatId]?.let { chat ->
                chat.lastMessage = update.lastMessage
                update.positions?.forEach { applyPosition(chat, it) }
            }
            is TdApi.UpdateChatPosition -> chats[update.chatId]?.let { applyPosition(it, update.position) }
            is TdApi.UpdateChatReadInbox -> chats[update.chatId]?.unreadCount = update.unreadCount
            is TdApi.UpdateUser -> users[update.user.id] = update.user
            is TdApi.UpdateFile -> {
                val local = update.file.local
                if (local != null && local.isDownloadingCompleted) {
                    filePaths[update.file.id] = local.path
                }
            }
            is TdApi.UpdateChatFolders -> {
                folders.clear()
                update.chatFolders?.forEach { info ->
                    folders.add(FolderInfo(info.id, info.name?.text?.text ?: ""))
                }
            }
            is TdApi.UpdateNewMessage -> messages[update.message.chatId]?.let { list ->
                if (list.none { it.id == update.message.id }) {
                    list.add(0, update.message.toUi())
                }
            }
            is TdApi.UpdateMessageSendSucceeded -> messages[update.message.chatId]?.let { list ->
                val index = list.indexOfFirst { it.id == update.oldMessageId }
                if (index >= 0) list[index] = update.message.toUi()
            }
            is TdApi.UpdateMessageContent -> messages[update.chatId]?.let { list ->
                val index = list.indexOfFirst { it.id == update.messageId }
                if (index >= 0) list[index] = list[index].copy(text = contentText(update.newContent))
            }
            is TdApi.UpdateDeleteMessages -> if (update.isPermanent) {
                messages[update.chatId]?.removeAll { it.id in update.messageIds }
            }
            else -> Unit
        }
    }

    private fun handleAuthState(state: TdApi.AuthorizationState) {
        authState = when (state) {
            is TdApi.AuthorizationStateWaitTdlibParameters -> {
                sendTdlibParameters()
                return
            }
            is TdApi.AuthorizationStateWaitPhoneNumber -> AuthState.WaitPhone
            is TdApi.AuthorizationStateWaitOtherDeviceConfirmation -> AuthState.WaitQr(state.link)
            is TdApi.AuthorizationStateWaitCode -> AuthState.WaitCode
            is TdApi.AuthorizationStateWaitPassword -> AuthState.WaitPassword(state.passwordHint ?: "")
            is TdApi.AuthorizationStateReady -> {
                send(TdApi.GetMe()) { result ->
                    if (result is TdApi.User) myUserId = result.id
                }
                loadChats(ChatListKey.Main)
                AuthState.Ready
            }
            is TdApi.AuthorizationStateLoggingOut,
            is TdApi.AuthorizationStateClosing -> AuthState.Closing
            is TdApi.AuthorizationStateClosed -> {
                chats.clear()
                messages.clear()
                users.clear()
                folders.clear()
                requestedFiles.clear()
                // WHY: after logging out TDLib closes the instance, a new one
                // is needed to sign in again.
                if (!exiting) createClient()
                AuthState.Starting
            }
            else -> AuthState.Unsupported(state.javaClass.simpleName)
        }
    }

    // Chats.

    private fun addChat(chat: TdApi.Chat) {
        val state = ChatState(chat.id).apply {
            title = chat.title
            photo = chat.photo?.small
            lastMessage = chat.lastMessage
            unreadCount = chat.unreadCount
            isGroup = chat.type is TdApi.ChatTypeBasicGroup || chat.type is TdApi.ChatTypeSupergroup
        }
        chat.positions?.forEach { applyPosition(state, it) }
        chats[chat.id] = state
    }

    private fun applyPosition(chat: ChatState, position: TdApi.ChatPosition) {
        val key = ChatListKey.of(position.list) ?: return
        if (position.order == 0L) {
            chat.positions.remove(key)
        } else {
            chat.positions[key] = position.order
        }
    }

    fun chatList(key: ChatListKey): List<ChatState> = chats.values
        .filter { key in it.positions }
        .sortedByDescending { it.positions[key] }

    fun loadChats(key: ChatListKey) {
        val list: TdApi.ChatList = when (key) {
            ChatListKey.Main -> TdApi.ChatListMain()
            ChatListKey.Archive -> TdApi.ChatListArchive()
            is ChatListKey.Folder -> TdApi.ChatListFolder().apply { chatFolderId = key.id }
        }
        send(TdApi.LoadChats().apply {
            chatList = list
            limit = 100
        })
    }

    // Messages.

    fun messages(chatId: Long): SnapshotStateList<UiMessage> =
        messages.getOrPut(chatId) { mutableStateListOf() }

    fun openChat(chatId: Long) {
        send(TdApi.OpenChat().apply { this.chatId = chatId })
        val list = messages(chatId)
        if (list.isEmpty()) loadMoreMessages(chatId)
    }

    fun closeChat(chatId: Long) {
        send(TdApi.CloseChat().apply { this.chatId = chatId })
    }

    fun loadMoreMessages(chatId: Long) {
        val list = messages(chatId)
        val fromId = list.lastOrNull()?.id ?: 0L
        send(TdApi.GetChatHistory().apply {
            this.chatId = chatId
            fromMessageId = fromId
            offset = 0
            limit = 50
            onlyLocal = false
        }) { result ->
            if (result !is TdApi.Messages) return@send
            val loaded = result.messages?.filterNotNull().orEmpty()
                .filter { message -> list.none { it.id == message.id } }
                .map { it.toUi() }
            list.addAll(loaded)
            if (loaded.isNotEmpty()) {
                markRead(chatId, loaded.map { it.id })
            }
        }
    }

    private fun markRead(chatId: Long, ids: List<Long>) {
        send(TdApi.ViewMessages().apply {
            this.chatId = chatId
            messageIds = ids.toLongArray()
            forceRead = true
        })
    }

    fun sendText(chatId: Long, text: String) {
        if (text.isBlank()) return
        send(TdApi.SendMessage().apply {
            this.chatId = chatId
            inputMessageContent = TdApi.InputMessageText().apply {
                this.text = TdApi.FormattedText(text, arrayOf())
            }
        })
    }

    fun senderName(message: UiMessage): String = when {
        message.senderUserId != 0L -> users[message.senderUserId]?.let { user ->
            listOf(user.firstName, user.lastName).filter { !it.isNullOrBlank() }.joinToString(" ")
        } ?: ""
        else -> chats[message.senderChatId]?.title ?: ""
    }

    // Files.

    fun localPath(file: TdApi.File?): String? {
        file ?: return null
        filePaths[file.id]?.let { return it }
        val local = file.local
        return if (local != null && local.isDownloadingCompleted) local.path else null
    }

    fun download(file: TdApi.File?) {
        file ?: return
        if (localPath(file) != null || !requestedFiles.add(file.id)) return
        send(TdApi.DownloadFile().apply {
            fileId = file.id
            priority = 1
            synchronous = false
        })
    }
}

fun TdApi.Message.toUi(): UiMessage {
    val sender = senderId
    return UiMessage(
        id = id,
        chatId = chatId,
        senderUserId = (sender as? TdApi.MessageSenderUser)?.userId ?: 0L,
        senderChatId = (sender as? TdApi.MessageSenderChat)?.chatId ?: 0L,
        isOutgoing = isOutgoing,
        date = date,
        text = contentText(content),
    )
}

fun contentText(content: TdApi.MessageContent?): String = when (content) {
    is TdApi.MessageText -> content.text?.text ?: ""
    is TdApi.MessagePhoto -> listOf("🖼 Photo", content.caption?.text).filterNotBlank()
    is TdApi.MessageVideo -> listOf("🎬 Video", content.caption?.text).filterNotBlank()
    is TdApi.MessageDocument -> listOf("📄 ${content.document?.fileName ?: "File"}", content.caption?.text).filterNotBlank()
    is TdApi.MessageAudio -> "🎵 ${content.audio?.title ?: "Audio"}"
    is TdApi.MessageVoiceNote -> "🎤 Voice message"
    is TdApi.MessageVideoNote -> "⏺ Video message"
    is TdApi.MessageSticker -> "${content.sticker?.emoji ?: ""} Sticker".trim()
    is TdApi.MessageAnimation -> listOf("GIF", content.caption?.text).filterNotBlank()
    is TdApi.MessageContact -> "👤 Contact"
    is TdApi.MessageLocation -> "📍 Location"
    is TdApi.MessagePoll -> "📊 Poll"
    null -> ""
    else -> content.javaClass.simpleName.removePrefix("Message")
}

private fun List<String?>.filterNotBlank(): String =
    filterNot { it.isNullOrBlank() }.joinToString("\n")
