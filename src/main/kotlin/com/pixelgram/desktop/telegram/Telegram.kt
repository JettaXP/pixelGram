package com.pixelgram.desktop.telegram

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.pixelgram.desktop.BuildConfig
import com.pixelgram.desktop.core.AppLog
import com.pixelgram.desktop.core.AppPaths
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.core.Sounds
import com.pixelgram.desktop.core.describe
import it.tdlight.ClientFactory
import it.tdlight.ExceptionHandler
import it.tdlight.Init
import it.tdlight.ResultHandler
import it.tdlight.TelegramClient
import it.tdlight.jni.TdApi
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class CodeInfo(
    val phoneNumber: String,
    val type: TdApi.AuthenticationCodeType?,
    val nextType: TdApi.AuthenticationCodeType?,
    val timeoutSeconds: Int,
    val receivedAt: Long = System.currentTimeMillis(),
) {
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
    var lastReadOutbox by mutableStateOf(0L)
    var isGroup by mutableStateOf(false)
    var privateUserId = 0L
    var isChannel = false
    var notifications: TdApi.ChatNotificationSettings? = null
    val positions = mutableStateMapOf<ChatListKey, Long>()
    val pinned = mutableStateMapOf<ChatListKey, Boolean>()
}

const val SavedMessagesTitle = "Saved Messages"

enum class ConnectionStatus(val label: String?) {
    WaitingForNetwork("Waiting for network…"),
    ConnectingToProxy("Connecting to proxy…"),
    Connecting("Connecting…"),
    Updating("Updating…"),
    Ready(null),
}

data class Presence(val label: String, val online: Boolean)

data class UnreadCount(val chats: Int, val unmuted: Int)

data class FolderInfo(val id: Int, val title: String, val icon: String)

data class UiMessage(
    val id: Long,
    val chatId: Long,
    val senderUserId: Long,
    val senderChatId: Long,
    val isOutgoing: Boolean,
    val date: Int,
    val text: String,
    val key: Long = id,
    val fresh: Boolean = false,
    val photo: TdApi.File? = null,
    val photoWidth: Int = 0,
    val photoHeight: Int = 0,
    val fullPhoto: TdApi.File? = null,
    val sending: Boolean = false,
    val failed: Boolean = false,
    val edited: Boolean = false,
    val service: Boolean = false,
    val editable: Boolean = true,
    val replyToId: Long = 0,
)

enum class MessageStatus { Sending, Sent, Read, Failed }

private const val AUTH_TIMEOUT_MS = 30_000L
private const val ListsInterval = 100L
private const val ClockTickMs = 20_000L
private const val AlbumSize = 10

private object Tdlib {
    private var factory: ClientFactory? = null
    private var error: Throwable? = null

    fun factory(): Result<ClientFactory> {
        error?.let { return Result.failure(it) }
        factory?.let { return Result.success(it) }
        return runCatching {
            NativeLibraries.preload()
            Init.init()
            ClientFactory.create()
        }.onSuccess { factory = it }.onFailure { error = it }
    }
}

class Telegram(
    val account: Int,
    private val settings: AppSettings,
    private val scope: CoroutineScope,
    private val onLoggedOut: (Telegram) -> Boolean = { false },
) {
    var authState by mutableStateOf<AuthState>(AuthState.Starting)
        internal set
    var authError by mutableStateOf<String?>(null)
    var authBusy by mutableStateOf(false)
        private set
    private var authRequest = 0

    val chats = mutableStateMapOf<Long, ChatState>()
    val users = mutableStateMapOf<Long, TdApi.User>()
    val unread = mutableStateMapOf<ChatListKey, UnreadCount>()
    val statuses = mutableStateMapOf<Long, TdApi.UserStatus>()
    var clockMinute by mutableStateOf(System.currentTimeMillis() / 60_000)
        private set

    init {
        scope.launch(Dispatchers.Main) {
            while (true) {
                delay(ClockTickMs)
                clockMinute = System.currentTimeMillis() / 60_000
            }
        }
    }
    var connection by mutableStateOf(ConnectionStatus.Ready)
        internal set
    val folders = mutableStateListOf<FolderInfo>()
    private val filePaths = mutableStateMapOf<Int, String>()
    private val messages = mutableMapOf<Long, SnapshotStateList<UiMessage>>()
    private val requestedFiles = mutableSetOf<Int>()

    private var factory: ClientFactory? = null
    private var client: TelegramClient? = null
    private var myUserId = 0L

    var me by mutableStateOf<TdApi.User?>(null)
        internal set

    fun start() {
        Tdlib.factory()
            .onSuccess {
                factory = it
                createClient()
            }
            .onFailure { e ->
                AppLog.write("TDLib native library failed to load", e)
                authState = AuthState.Failed(
                    "TDLib native library failed to load: ${e.describe()}\n\nLog: ${AppLog.file.absolutePath}"
                )
            }
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
    private var loggingOut = false

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
                if (result.message != "Another authorization query has started"
                    && result.message != "Request aborted"
                ) {
                    authError = friendlyError(result.message)
                }
            }
        }
    }

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
            databaseDirectory = File(AppPaths.accountDir(account), "database").absolutePath
            filesDirectory = File(AppPaths.accountDir(account), "files").absolutePath
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

    fun status(chat: ChatState, id: Long, sending: Boolean, failed: Boolean): MessageStatus = when {
        failed -> MessageStatus.Failed
        sending -> MessageStatus.Sending
        id <= chat.lastReadOutbox || isSavedMessages(chat.id) -> MessageStatus.Read
        else -> MessageStatus.Sent
    }

    fun status(chat: ChatState, message: UiMessage) = status(chat, message.id, message.sending, message.failed)

    fun status(chat: ChatState, message: TdApi.Message) = status(
        chat,
        message.id,
        message.sendingState is TdApi.MessageSendingStatePending,
        message.sendingState is TdApi.MessageSendingStateFailed,
    )

    fun chatTitle(chat: ChatState): String =
        if (isSavedMessages(chat.id)) SavedMessagesTitle else chat.title

    fun openSavedMessages(onChat: (Long) -> Unit) {
        if (myUserId != 0L) openPrivateChat(myUserId, onChat)
    }

    fun openPublicChat(username: String, onChat: (Long) -> Unit, onFail: () -> Unit) {
        send(TdApi.SearchPublicChat().apply { this.username = username }) { result ->
            if (result is TdApi.Chat) onChat(result.id) else onFail()
        }
    }

    fun openPrivateChat(userId: Long, onChat: (Long) -> Unit) {
        send(TdApi.CreatePrivateChat().apply {
            this.userId = userId
            force = false
        }) { result ->
            if (result is TdApi.Chat) onChat(result.id)
        }
    }

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

    fun isOnline(chat: ChatState): Boolean {
        val userId = chat.privateUserId
        if (userId == 0L || isSavedMessages(chat.id) || users[userId]?.type is TdApi.UserTypeBot) return false
        return statuses[userId] is TdApi.UserStatusOnline
    }

    fun presence(chat: ChatState): Presence? {
        val userId = chat.privateUserId
        if (userId == 0L || isSavedMessages(chat.id)) return null
        if (users[userId]?.type is TdApi.UserTypeBot) return Presence("bot", false)
        clockMinute
        return when (val status = statuses[userId]) {
            is TdApi.UserStatusOnline -> Presence("online", true)
            is TdApi.UserStatusOffline -> Presence(lastSeen(status.wasOnline), false)
            is TdApi.UserStatusRecently -> Presence("last seen recently", false)
            is TdApi.UserStatusLastWeek -> Presence("last seen within a week", false)
            is TdApi.UserStatusLastMonth -> Presence("last seen within a month", false)
            else -> Presence("last seen a long time ago", false)
        }
    }

    private fun lastSeen(wasOnline: Int): String {
        val minutes = (System.currentTimeMillis() / 1000 - wasOnline) / 60
        val zone = java.time.ZoneId.systemDefault()
        val then = java.time.Instant.ofEpochSecond(wasOnline.toLong()).atZone(zone)
        val time = then.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
        val today = java.time.LocalDate.now(zone)
        return when {
            minutes < 1 -> "last seen just now"
            minutes < 60 -> "last seen $minutes min ago"
            then.toLocalDate() == today -> "last seen today at $time"
            then.toLocalDate() == today.minusDays(1) -> "last seen yesterday at $time"
            else -> "last seen " + then.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yy")) + " at $time"
        }
    }

    fun userName(userId: Long): String = users[userId]?.fullName() ?: ""

    fun myFullInfo(onResult: (TdApi.UserFullInfo?) -> Unit) {
        if (myUserId == 0L) return onResult(null)
        userFullInfo(myUserId, onResult)
    }

    fun userFullInfo(userId: Long, onResult: (TdApi.UserFullInfo?) -> Unit) {
        send(TdApi.GetUserFullInfo().apply { this.userId = userId }) { result ->
            onResult(result as? TdApi.UserFullInfo)
        }
    }

    fun loadChat(chatId: Long) {
        if (chatId != 0L && chatId !in chats) send(TdApi.GetChat().apply { this.chatId = chatId })
    }

    fun suitablePersonalChats(onResult: (List<Long>) -> Unit) {
        send(TdApi.GetSuitablePersonalChats()) { result ->
            onResult((result as? TdApi.Chats)?.chatIds?.toList().orEmpty())
        }
    }

    fun setPersonalChat(chatId: Long, onResult: (error: String?) -> Unit) {
        send(TdApi.SetPersonalChat().apply { this.chatId = chatId }) { result ->
            onResult((result as? TdApi.Error)?.let { friendlyError(it.message) })
        }
    }

    fun storageStatistics(onResult: (TdApi.StorageStatisticsFast?) -> Unit) {
        send(TdApi.GetStorageStatisticsFast()) { result ->
            onResult(result as? TdApi.StorageStatisticsFast)
        }
    }

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
            is TdApi.UpdateConnectionState -> connection = when (update.state) {
                is TdApi.ConnectionStateWaitingForNetwork -> ConnectionStatus.WaitingForNetwork
                is TdApi.ConnectionStateConnectingToProxy -> ConnectionStatus.ConnectingToProxy
                is TdApi.ConnectionStateConnecting -> ConnectionStatus.Connecting
                is TdApi.ConnectionStateUpdating -> ConnectionStatus.Updating
                else -> ConnectionStatus.Ready
            }
            is TdApi.UpdateUnreadChatCount -> ChatListKey.of(update.chatList)?.let { key ->
                unread[key] = UnreadCount(update.unreadCount, update.unreadUnmutedCount)
            }
            is TdApi.UpdateChatReadOutbox -> chats[update.chatId]?.lastReadOutbox = update.lastReadOutboxMessageId
            is TdApi.UpdateMessageSendFailed -> messages[update.message.chatId]?.let { list ->
                val index = list.indexOfFirst { it.id == update.oldMessageId }
                if (index >= 0) list[index] = update.message.toUi().copy(key = list[index].key)
            }
            is TdApi.UpdateUserStatus -> statuses[update.userId] = update.status
            is TdApi.UpdateUser -> {
                users[update.user.id] = update.user
                update.user.status?.let { statuses[update.user.id] = it }
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
                    folders.add(FolderInfo(info.id, info.name?.text?.text ?: "", info.icon?.name ?: ""))
                }
            }
            is TdApi.UpdateNewMessage -> {
                messages[update.message.chatId]?.let { list ->
                    if (list.none { it.id == update.message.id }) {
                        list.add(0, update.message.toUi().copy(fresh = true))
                    }
                }
                if (shouldNotify(update.message)) {
                    Sounds.newMessage()
                    Notifications.onMessage?.invoke(this, update.message)
                }
            }
            is TdApi.UpdateChatNotificationSettings ->
                chats[update.chatId]?.notifications = update.notificationSettings
            is TdApi.UpdateScopeNotificationSettings ->
                scopeNotifications[update.scope.javaClass] = update.notificationSettings
            is TdApi.UpdateMessageSendSucceeded -> messages[update.message.chatId]?.let { list ->
                val index = list.indexOfFirst { it.id == update.oldMessageId }
                if (index >= 0) list[index] = update.message.toUi().copy(key = list[index].key, fresh = true)
            }
            is TdApi.UpdateMessageEdited -> messages[update.chatId]?.let { list ->
                val index = list.indexOfFirst { it.id == update.messageId }
                if (index >= 0) list[index] = list[index].copy(edited = update.editDate > 0)
            }
            is TdApi.UpdateMessageContent -> messages[update.chatId]?.let { list ->
                val index = list.indexOfFirst { it.id == update.messageId }
                if (index >= 0) {
                    val content = update.newContent
                    val text = if (content is TdApi.MessagePhoto) content.caption?.text.orEmpty() else contentText(content)
                    list[index] = list[index].copy(text = text)
                }
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
            is TdApi.AuthorizationStateLoggingOut -> {
                loggingOut = true
                AuthState.Closing
            }
            is TdApi.AuthorizationStateClosing -> AuthState.Closing
            is TdApi.AuthorizationStateClosed -> {
                chats.clear()
                chatListsChanged()
                messages.clear()
                users.clear()
                folders.clear()
                requestedFiles.clear()
                me = null
                myUserId = 0L
                val removed = loggingOut && onLoggedOut(this)
                loggingOut = false
                if (!exiting && !removed) createClient()
                AuthState.Starting
            }
            else -> AuthState.Unsupported(state.javaClass.simpleName)
        }
    }

    private fun addChat(chat: TdApi.Chat) {
        val state = ChatState(chat.id).apply {
            title = chat.title
            photo = chat.photo?.small
            lastMessage = chat.lastMessage
            unreadCount = chat.unreadCount
            lastReadOutbox = chat.lastReadOutboxMessageId
            isGroup = chat.type is TdApi.ChatTypeBasicGroup || chat.type is TdApi.ChatTypeSupergroup
            privateUserId = (chat.type as? TdApi.ChatTypePrivate)?.userId ?: 0L
            isChannel = (chat.type as? TdApi.ChatTypeSupergroup)?.isChannel == true
            notifications = chat.notificationSettings
        }
        chat.positions?.forEach { applyPosition(state, it) }
        chats[chat.id] = state
        chatListsChanged()
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
        chatListsChanged()
    }

    private val listCache = HashMap<ChatListKey, List<ChatState>>()
    private var listsVersion by mutableIntStateOf(0)
    private var listsRefresh: Job? = null

    fun chatList(key: ChatListKey): List<ChatState> {
        listsVersion
        return listCache.getOrPut(key) {
            Snapshot.withoutReadObservation {
                chats.values
                    .filter { key in it.positions }
                    .sortedByDescending { it.positions[key] }
            }
        }
    }

    internal fun chatListsChanged() {
        if (listsRefresh?.isActive == true) return
        listsRefresh = scope.launch(Dispatchers.Main) {
            delay(ListsInterval)
            listCache.clear()
            listsVersion++
        }
    }

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

    fun messages(chatId: Long): SnapshotStateList<UiMessage> =
        messages.getOrPut(chatId) { mutableStateListOf() }

    private val scopeNotifications = mutableMapOf<Class<*>, TdApi.ScopeNotificationSettings>()
    private var openChatId = 0L

    var windowFocused = true

    private fun shouldNotify(message: TdApi.Message): Boolean {
        if (message.isOutgoing || !settings.notificationSound.value || settings.notificationsMuted.value) return false
        if (message.chatId == openChatId && windowFocused) return false
        val chat = chats[message.chatId] ?: return false
        val own = chat.notifications
        val muteFor = if (own == null || own.useDefaultMuteFor) {
            val scope = when {
                chat.privateUserId != 0L -> TdApi.NotificationSettingsScopePrivateChats::class.java
                chat.isChannel -> TdApi.NotificationSettingsScopeChannelChats::class.java
                else -> TdApi.NotificationSettingsScopeGroupChats::class.java
            }
            scopeNotifications[scope]?.muteFor ?: 0
        } else {
            own.muteFor
        }
        return muteFor == 0
    }

    fun openChat(chatId: Long) {
        openChatId = chatId
        send(TdApi.OpenChat().apply { this.chatId = chatId })
        val list = messages(chatId)
        if (list.isEmpty()) loadMoreMessages(chatId)
    }

    fun closeChat(chatId: Long) {
        if (openChatId == chatId) openChatId = 0L
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

    fun sendText(chatId: Long, text: String, replyToId: Long = 0) {
        if (text.isBlank()) return
        send(TdApi.SendMessage().apply {
            this.chatId = chatId
            replyTo = replyInput(replyToId)
            inputMessageContent = TdApi.InputMessageText().apply {
                this.text = TdApi.FormattedText(text, arrayOf())
            }
        })
    }

    fun editMessage(message: UiMessage, text: String) {
        val formatted = TdApi.FormattedText(text, arrayOf())
        if (message.photo != null) {
            send(TdApi.EditMessageCaption().apply {
                chatId = message.chatId
                messageId = message.id
                caption = formatted
            })
        } else {
            if (text.isBlank()) return
            send(TdApi.EditMessageText().apply {
                chatId = message.chatId
                messageId = message.id
                inputMessageContent = TdApi.InputMessageText().apply { this.text = formatted }
            })
        }
    }

    private fun replyInput(id: Long): TdApi.InputMessageReplyTo? =
        if (id == 0L) null else TdApi.InputMessageReplyToMessage().apply { messageId = id }

    fun deleteMessages(chatId: Long, ids: LongArray, revoke: Boolean) {
        send(TdApi.DeleteMessages(chatId, ids, revoke))
    }

    private val replyCache = mutableStateMapOf<Pair<Long, Long>, UiMessage>()
    private val replyRequests = HashSet<Pair<Long, Long>>()

    fun replyPreview(chatId: Long, id: Long): UiMessage? {
        messages[chatId]?.firstOrNull { it.id == id }?.let { return it }
        val key = chatId to id
        replyCache[key]?.let { return it }
        if (replyRequests.add(key)) {
            send(TdApi.GetMessage(chatId, id)) { result ->
                if (result is TdApi.Message) replyCache[key] = result.toUi()
            }
        }
        return null
    }

    fun sendFiles(chatId: Long, files: List<File>, asPhotos: Boolean, caption: String, replyToId: Long = 0) {
        files.chunked(AlbumSize).forEachIndexed { chunkIndex, chunk ->
            val contents = chunk.mapIndexed { index, file ->
                val text = TdApi.FormattedText(
                    if (chunkIndex == 0 && index == 0) caption.trim() else "",
                    arrayOf(),
                )
                val input = TdApi.InputFileLocal(file.absolutePath)
                if (asPhotos) {
                    val (width, height) = imageSize(file)
                    TdApi.InputMessagePhoto().apply {
                        photo = TdApi.InputPhoto().apply {
                            photo = input
                            addedStickerFileIds = IntArray(0)
                            this.width = width
                            this.height = height
                        }
                        this.caption = text
                    }
                } else {
                    TdApi.InputMessageDocument().apply {
                        document = TdApi.InputDocument().apply { document = input }
                        this.caption = text
                    }
                }
            }
            if (contents.size == 1) {
                send(TdApi.SendMessage().apply {
                    this.chatId = chatId
                    if (chunkIndex == 0) replyTo = replyInput(replyToId)
                    inputMessageContent = contents.single()
                })
            } else {
                send(TdApi.SendMessageAlbum().apply {
                    this.chatId = chatId
                    if (chunkIndex == 0) replyTo = replyInput(replyToId)
                    inputMessageContents = contents.toTypedArray()
                })
            }
        }
    }

    fun senderName(message: UiMessage): String = when {
        message.senderUserId != 0L -> userName(message.senderUserId)
        else -> chats[message.senderChatId]?.title ?: ""
    }

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
    val photo = (content as? TdApi.MessagePhoto)?.let { chatPhotoSize(it.photo) }
    return UiMessage(
        id = id,
        chatId = chatId,
        senderUserId = (sender as? TdApi.MessageSenderUser)?.userId ?: 0L,
        senderChatId = (sender as? TdApi.MessageSenderChat)?.chatId ?: 0L,
        isOutgoing = isOutgoing,
        date = date,
        text = if (photo != null) (content as TdApi.MessagePhoto).caption?.text.orEmpty() else contentText(content),
        photo = photo?.photo,
        photoWidth = photo?.width ?: 0,
        photoHeight = photo?.height ?: 0,
        fullPhoto = (content as? TdApi.MessagePhoto)?.photo?.sizes?.maxByOrNull { it.width }?.photo,
        sending = sendingState is TdApi.MessageSendingStatePending,
        failed = sendingState is TdApi.MessageSendingStateFailed,
        edited = editDate > 0,
        service = isService(content),
        editable = isOutgoing && (content is TdApi.MessageText || content is TdApi.MessagePhoto),
        replyToId = (replyTo as? TdApi.MessageReplyToMessage)?.takeIf { it.chatId == 0L || it.chatId == chatId }?.messageId ?: 0L,
    )
}

fun isService(content: TdApi.MessageContent?): Boolean = when (content) {
    is TdApi.MessageSupergroupChatCreate, is TdApi.MessageBasicGroupChatCreate, is TdApi.MessageChatChangeTitle,
    is TdApi.MessageChatChangePhoto, is TdApi.MessageChatDeletePhoto, is TdApi.MessageChatAddMembers,
    is TdApi.MessageChatJoinByLink, is TdApi.MessageChatJoinByRequest, is TdApi.MessageChatDeleteMember,
    is TdApi.MessagePinMessage, is TdApi.MessageScreenshotTaken, is TdApi.MessageContactRegistered,
    is TdApi.MessageChatUpgradeTo, is TdApi.MessageChatUpgradeFrom, is TdApi.MessageCustomServiceAction,
    is TdApi.MessageChatSetMessageAutoDeleteTime, is TdApi.MessageForumTopicCreated,
    is TdApi.MessageVideoChatStarted, is TdApi.MessageVideoChatEnded -> true
    else -> false
}

private fun chatPhotoSize(photo: TdApi.Photo?): TdApi.PhotoSize? {
    val sizes = photo?.sizes.orEmpty().sortedBy { it.width }
    return sizes.firstOrNull { it.width >= ChatPhotoMinWidth } ?: sizes.lastOrNull()
}

private const val ChatPhotoMinWidth = 640

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
    is TdApi.MessageSupergroupChatCreate -> "Created «${content.title}»"
    is TdApi.MessageBasicGroupChatCreate -> "Created the group «${content.title}»"
    is TdApi.MessageChatChangeTitle -> "Changed the name to «${content.title}»"
    is TdApi.MessageChatChangePhoto -> "Updated the photo"
    is TdApi.MessageChatDeletePhoto -> "Removed the photo"
    is TdApi.MessageChatAddMembers -> "Added new members"
    is TdApi.MessageChatJoinByLink -> "Joined via invite link"
    is TdApi.MessageChatJoinByRequest -> "Joined the chat"
    is TdApi.MessageChatDeleteMember -> "Left the chat"
    is TdApi.MessagePinMessage -> "📌 Pinned a message"
    is TdApi.MessageScreenshotTaken -> "Took a screenshot"
    is TdApi.MessageContactRegistered -> "Joined Telegram"
    is TdApi.MessageChatUpgradeTo, is TdApi.MessageChatUpgradeFrom -> "The group was upgraded to a supergroup"
    is TdApi.MessageCustomServiceAction -> content.text
    is TdApi.MessageChatSetMessageAutoDeleteTime ->
        if (content.messageAutoDeleteTime == 0) "Disabled auto-delete" else "Enabled auto-delete"
    is TdApi.MessageForumTopicCreated -> "Created the topic «${content.name}»"
    is TdApi.MessageVideoChatStarted -> "Started a video chat"
    is TdApi.MessageVideoChatEnded -> "Video chat ended"
    is TdApi.MessageCall -> if (content.isVideo) "📹 Video call" else "📞 Call"
    null -> ""
    else -> content.javaClass.simpleName.removePrefix("Message")
        .replace(Regex("(?<=[a-z])(?=[A-Z])"), " ").lowercase().replaceFirstChar { it.uppercase() }
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

fun imageSize(file: File): Pair<Int, Int> = runCatching {
    javax.imageio.ImageIO.createImageInputStream(file).use { stream ->
        val reader = javax.imageio.ImageIO.getImageReaders(stream).next()
        try {
            reader.input = stream
            reader.getWidth(0) to reader.getHeight(0)
        } finally {
            reader.dispose()
        }
    }
}.getOrDefault(0 to 0)
