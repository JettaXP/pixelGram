package com.pixelgram.desktop.telegram

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.pixelgram.desktop.BuildConfig
import com.pixelgram.desktop.core.AppPaths
import com.pixelgram.desktop.core.AppLog
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.core.describe
import it.tdlight.ClientFactory
import it.tdlight.ExceptionHandler
import it.tdlight.Init
import it.tdlight.ResultHandler
import it.tdlight.TelegramClient
import it.tdlight.jni.TdApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

data class CodeInfo(
    val phoneNumber: String,
    val type: TdApi.AuthenticationCodeType?,
    val nextType: TdApi.AuthenticationCodeType?,
    val timeoutSeconds: Int,
    val receivedAt: Long = System.currentTimeMillis(),
) {
    // Expected code length, 0 when unknown.
    val length: Int
        get() = when (val t = type) {
            is TdApi.AuthenticationCodeTypeTelegramMessage -> t.length
            is TdApi.AuthenticationCodeTypeSms -> t.length
            is TdApi.AuthenticationCodeTypeCall -> t.length
            is TdApi.AuthenticationCodeTypeMissedCall -> t.length
            is TdApi.AuthenticationCodeTypeFragment -> t.length
            else -> 0
        }
}

data class Country(
    val code: String,
    val name: String,
    val flag: String,
    val callingCode: String,
)

sealed interface AuthState {
    data object Starting : AuthState
    data object NeedApiCredentials : AuthState
    data object WaitPhone : AuthState
    data class WaitQr(val link: String) : AuthState
    data class WaitCode(val info: CodeInfo) : AuthState
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
    val pinned = mutableStateMapOf<ChatListKey, Boolean>()
}

const val SavedMessagesTitle = "Saved Messages"

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

private const val AUTH_TIMEOUT_MS = 30_000L

class Telegram(
    private val settings: AppSettings,
    private val scope: CoroutineScope,
) {
    var authState by mutableStateOf<AuthState>(AuthState.Starting)
        private set
    var authError by mutableStateOf<String?>(null)
    var authBusy by mutableStateOf(false)
        private set
    private var authRequest = 0

    val chats = mutableStateMapOf<Long, ChatState>()
    val users = mutableStateMapOf<Long, TdApi.User>()
    val folders = mutableStateListOf<FolderInfo>()
    private val filePaths = mutableStateMapOf<Int, String>()
    private val messages = mutableMapOf<Long, SnapshotStateList<UiMessage>>()
    private val requestedFiles = mutableSetOf<Int>()

    private var factory: ClientFactory? = null
    private var client: TelegramClient? = null
    private var myUserId = 0L

    var me by mutableStateOf<TdApi.User?>(null)
        private set

    fun start() {
        try {
            NativeLibraries.preload()
            Init.init()
        } catch (e: Throwable) {
            AppLog.write("TDLib native library failed to load", e)
            authState = AuthState.Failed(
                "TDLib native library failed to load: ${e.describe()}\n\nLog: ${AppLog.file.absolutePath}"
            )
            return
        }
        factory = ClientFactory.create()
        createClient()
    }

    private fun createClient() {
        val client = factory?.createClient() ?: return
        this.client = client
        client.execute(TdApi.SetLogVerbosityLevel(2))
        client.execute(TdApi.SetLogStream(TdApi.LogStreamFile(
            File(AppPaths.dataDir, "tdlib.log").absolutePath,
            10L * 1024 * 1024,
            false,
        )))
        client.initialize(
            ResultHandler<TdApi.Update> { update -> onMain { handleUpdate(update) } },
            ExceptionHandler { AppLog.write("TDLib update error", it) },
            ExceptionHandler { AppLog.write("TDLib error", it) },
        )
        loadCountries()
    }

    // Countries with calling codes, straight from Telegram.
    var countries by mutableStateOf<List<Country>>(emptyList())
        private set

    private fun loadCountries() {
        send(TdApi.GetCountries()) { result ->
            if (result !is TdApi.Countries) return@send
            countries = result.countries.orEmpty()
                .filter { !it.isHidden }
                .flatMap { info ->
                    info.callingCodes.orEmpty().map { code ->
                        Country(info.countryCode, info.name, info.flagEmoji ?: "", code)
                    }
                }
                .sortedBy { it.name }
        }
    }

    // Formatted national number, expected digits are '-'; null when unknown.
    fun phoneInfo(fullNumber: String): TdApi.PhoneNumberInfo? {
        val client = client ?: return null
        val result = client.execute(TdApi.GetPhoneNumberInfoSync(
            System.getProperty("user.language") ?: "en",
            fullNumber.filter(Char::isDigit),
        ))
        return result as? TdApi.PhoneNumberInfo
    }

    fun resendCode() = sendAuth(TdApi.ResendAuthenticationCode().apply {
        reason = TdApi.ResendCodeReasonUserRequest()
    })

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
        if (authBusy) return
        authError = null
        authBusy = true
        val request = ++authRequest
        scope.launch(Dispatchers.Main) {
            delay(AUTH_TIMEOUT_MS)
            if (authBusy && authRequest == request) {
                authBusy = false
                authError = "Telegram is taking too long to respond. Try again."
            }
        }
        send(function) { result ->
            if (authRequest == request) authBusy = false
            if (result is TdApi.Error) {
                AppLog.write("Auth error ${result.code}: ${result.message}")
                // WHY: both mean an earlier request is still finishing or was
                // superseded by a state change, the state update follows anyway.
                if (result.message != "Another authorization query has started"
                    && result.message != "Request aborted"
                ) {
                    authError = friendlyError(result.message)
                }
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
        send(parameters) { result ->
            if (result is TdApi.Error) {
                AppLog.write("TDLib parameters error ${result.code}: ${result.message}")
                authError = friendlyError(result.message)
            }
        }
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

    fun isSavedMessages(chatId: Long): Boolean = me?.id == chatId

    fun chatTitle(chat: ChatState): String =
        if (isSavedMessages(chat.id)) SavedMessagesTitle else chat.title

    fun openSavedMessages(onChat: (Long) -> Unit) {
        if (myUserId != 0L) openPrivateChat(myUserId, onChat)
    }

    fun openPrivateChat(userId: Long, onChat: (Long) -> Unit) {
        send(TdApi.CreatePrivateChat().apply {
            this.userId = userId
            force = false
        }) { result ->
            if (result is TdApi.Chat) onChat(result.id)
        }
    }

    // Groups are created as supergroups: they need no initial members.
    fun createChat(
        title: String,
        description: String,
        channel: Boolean,
        onResult: (chatId: Long?, error: String?) -> Unit,
    ) {
        send(TdApi.CreateNewSupergroupChat().apply {
            this.title = title.trim()
            this.description = description.trim()
            isChannel = channel
            isForum = false
            forImport = false
        }) { result ->
            when (result) {
                is TdApi.Chat -> onResult(result.id, null)
                is TdApi.Error -> onResult(null, friendlyError(result.message))
                else -> onResult(null, null)
            }
        }
    }

    fun contacts(onResult: (List<Long>) -> Unit) {
        send(TdApi.GetContacts()) { result ->
            onResult((result as? TdApi.Users)?.userIds?.toList().orEmpty())
        }
    }

    fun recentCalls(onResult: (List<TdApi.Message>) -> Unit) {
        send(TdApi.SearchCallMessages().apply {
            offset = ""
            limit = 100
            onlyMissed = false
        }) { result ->
            onResult((result as? TdApi.FoundMessages)?.messages?.toList().orEmpty())
        }
    }

    fun activeSessions(onResult: (List<TdApi.Session>) -> Unit) {
        send(TdApi.GetActiveSessions()) { result ->
            onResult((result as? TdApi.Sessions)?.sessions?.toList().orEmpty())
        }
    }

    fun userName(userId: Long): String = users[userId]?.fullName() ?: ""

    fun myFullInfo(onResult: (TdApi.UserFullInfo?) -> Unit) {
        if (myUserId == 0L) return onResult(null)
        send(TdApi.GetUserFullInfo().apply { userId = myUserId }) { result ->
            onResult(result as? TdApi.UserFullInfo)
        }
    }

    fun storageStatistics(onResult: (TdApi.StorageStatisticsFast?) -> Unit) {
        send(TdApi.GetStorageStatisticsFast()) { result ->
            onResult(result as? TdApi.StorageStatisticsFast)
        }
    }

    // Deletes all cached media and files; chats and messages stay.
    fun clearCache(onDone: () -> Unit) {
        send(TdApi.OptimizeStorage().apply {
            size = 0
            ttl = 0
            count = 0
            immunityDelay = 0
            returnDeletedFileStatistics = false
            chatLimit = 0
        }) { onDone() }
    }

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
            is TdApi.UpdateUser -> {
                users[update.user.id] = update.user
                if (update.user.id == myUserId) me = update.user
            }
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
        authBusy = false
        authRequest++
        authState = when (state) {
            is TdApi.AuthorizationStateWaitTdlibParameters -> {
                sendTdlibParameters()
                return
            }
            is TdApi.AuthorizationStateWaitPhoneNumber -> AuthState.WaitPhone
            is TdApi.AuthorizationStateWaitOtherDeviceConfirmation -> AuthState.WaitQr(state.link)
            is TdApi.AuthorizationStateWaitCode -> AuthState.WaitCode(CodeInfo(
                phoneNumber = state.codeInfo?.phoneNumber ?: "",
                type = state.codeInfo?.type,
                nextType = state.codeInfo?.nextType,
                timeoutSeconds = state.codeInfo?.timeout ?: 0,
            ))
            is TdApi.AuthorizationStateWaitPassword -> AuthState.WaitPassword(state.passwordHint ?: "")
            is TdApi.AuthorizationStateReady -> {
                send(TdApi.GetMe()) { result ->
                    if (result is TdApi.User) {
                        myUserId = result.id
                        me = result
                    }
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
            chat.pinned.remove(key)
        } else {
            chat.positions[key] = position.order
            chat.pinned[key] = position.isPinned
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
        message.senderUserId != 0L -> userName(message.senderUserId)
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
        }) { result ->
            when (result) {
                is TdApi.File -> result.local?.takeIf { it.isDownloadingCompleted }?.let {
                    filePaths[result.id] = it.path
                }
                is TdApi.Error -> {
                    AppLog.write("Download ${file.id} failed ${result.code}: ${result.message}")
                    requestedFiles.remove(file.id)
                }
            }
        }
    }
}

fun TdApi.User.fullName(): String =
    listOf(firstName, lastName).filter { !it.isNullOrBlank() }.joinToString(" ")

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

private fun friendlyError(message: String?): String {
    val text = message.orEmpty()
    return when {
        text.contains("PHONE_NUMBER_INVALID") -> "Invalid phone number. Check the country code and number."
        text.contains("PHONE_NUMBER_BANNED") -> "This phone number is banned in Telegram."
        text.contains("PHONE_CODE_INVALID") -> "Wrong code. Try again."
        text.contains("PHONE_CODE_EXPIRED") -> "The code has expired. Request a new one."
        text.contains("PHONE_CODE_EMPTY") -> "Enter the code."
        text.contains("PASSWORD_HASH_INVALID") -> "Wrong password."
        text.contains("FLOOD_WAIT") || text.contains("Too Many Requests") ->
            "Too many attempts. Please wait a bit and try again."
        text.contains("API_ID_INVALID") || text.contains("API_ID_PUBLISHED_FLOOD") ->
            "The API ID / hash are not valid. Check them on my.telegram.org."
        else -> text.ifEmpty { "Something went wrong." }
    }
}
