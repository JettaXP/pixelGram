package com.pixelgram.desktop.telegram

import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import com.pixelgram.desktop.core.AvatarKind
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
    data object Unread : ChatListKey
    data object Mentions : ChatListKey

    fun toTd(): TdApi.ChatList = when (this) {
        Main, Unread, Mentions -> TdApi.ChatListMain()
        Archive -> TdApi.ChatListArchive()
        is Folder -> TdApi.ChatListFolder(id)
    }

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
    var unreadMentions by mutableStateOf(0)
    var markedUnread by mutableStateOf(false)
    var lastReadOutbox by mutableStateOf(0L)
    var isGroup by mutableStateOf(false)
    var privateUserId = 0L
    var isChannel = false
    var supergroupId = 0L
    var basicGroupId = 0L
    var draft by mutableStateOf("")
    var replyMarkupMessageId = 0L
    var keyboard by mutableStateOf<TdApi.ReplyMarkupShowKeyboard?>(null)
    var notifications by mutableStateOf<TdApi.ChatNotificationSettings?>(null)
    var background by mutableStateOf<TdApi.ChatBackground?>(null)
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

data class BotCommandItem(val botId: Long, val command: String, val description: String)

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
    val content: TdApi.MessageContent? = null,
    val entities: List<TdApi.TextEntity> = emptyList(),
    val forwardOrigin: TdApi.MessageOrigin? = null,
    val reactions: List<TdApi.MessageReaction> = emptyList(),
    val keyboard: List<List<TdApi.InlineKeyboardButton>> = emptyList(),
    val viaBot: Long = 0,
    val album: List<UiMessage> = emptyList(),
    val signature: String = "",
    val captionAbove: Boolean = false,
    val views: Int = 0,
    val pinned: Boolean = false,
    val albumId: Long = 0,
    val comments: Int = -1,
)

enum class MessageStatus { Sending, Sent, Read, Failed }

private const val AUTH_TIMEOUT_MS = 30_000L
private const val PinnedLimit = 100
private const val JumpPageSize = 60
private const val ChatSearchLimit = 20
private const val PreviewPage = 40
private const val StickerSearchLimit = 100
private val StreamingExtensions = setOf("mp4", "m4v", "mov")
private const val ProfileGiftsLimit = 100
private const val StoryPage = 100
private const val MinPublicSearch = 3
private const val MembersLimit = 200
private const val ThumbnailSide = 320
private const val StoryLimit = 1000
private const val JumpMinMessages = 20
private const val JumpAttempts = 3
private const val SavedTopicsPage = 100
private const val CommonGroupsLimit = 100
private const val ThreadPageSize = 100
private const val CustomEmojiBatchMs = 40L
private const val CustomEmojiChunk = 200
private const val OfficialChannel = "pixelgramclient"
private const val DemoOfficialChat = 100L
private const val ListsInterval = 100L
private const val ClockTickMs = 20_000L
private const val SearchLimit = 100
private const val ReactionRow = 8
private const val MentionLimit = 30
private const val MuteForever = 366 * 24 * 3600
private const val AlbumSize = 10
private const val HistoryPageSize = 100
private const val ProfilePhotosLimit = 100
private const val ProfileAudiosLimit = 50
private const val DownloadWaitSteps = 600
private const val DownloadWaitMs = 200L
private const val MentionResolveMin = 4
private const val HistoryPagesMax = 300
private const val DrainBudgetNs = 8_000_000L

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
    var authRetryAt by mutableStateOf(0L)
    var authBusy by mutableStateOf(false)
        private set
    private var authRequest = 0

    val chats = mutableStateMapOf<Long, ChatState>()
    val users = KeyedState<Long, TdApi.User>()
    val unread = mutableStateMapOf<ChatListKey, UnreadCount>()
    val unreadMessages = mutableStateMapOf<ChatListKey, UnreadCount>()
    val statuses = KeyedState<Long, TdApi.UserStatus>()
    private val chatActions = mutableStateMapOf<Long, List<Pair<TdApi.MessageSender, TdApi.ChatAction>>>()
    val emojiClicks = KeyedState<Pair<Long, Long>, TdApi.Sticker>()
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
    private val filePaths = KeyedState<Int, String>()
    private val transfers = KeyedState<Int, Float>()

    fun progress(file: TdApi.File?): Float? = file?.let { transfers[it.id] }

    fun cancelDownload(file: TdApi.File?) {
        file ?: return
        send(TdApi.CancelDownloadFile(file.id, false))
        transfers.remove(file.id)
        requestedFiles.remove(file.id)
    }

    fun cancelSending(message: UiMessage) = deleteMessages(message.chatId, longArrayOf(message.id), true)
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
        flushDrafts()
        send(TdApi.Close())
    }

    private val inbox = java.util.concurrent.ConcurrentLinkedQueue<() -> Unit>()
    private val draining = java.util.concurrent.atomic.AtomicBoolean(false)

    private fun onMain(block: () -> Unit) {
        inbox.add(block)
        if (draining.compareAndSet(false, true)) scheduleDrain()
    }

    private fun scheduleDrain() {
        scope.launch(Dispatchers.Main) { drain() }
    }

    private fun drain() {
        val started = System.nanoTime()
        while (System.nanoTime() - started < DrainBudgetNs) {
            val block = inbox.poll() ?: break
            runCatching(block).onFailure { AppLog.write("TDLib result handling failed", it) }
        }
        if (inbox.isNotEmpty()) return scheduleDrain()
        draining.set(false)
        if (inbox.isNotEmpty() && draining.compareAndSet(false, true)) scheduleDrain()
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
                    floodSeconds(result.message)?.let { authRetryAt = System.currentTimeMillis() + it * 1000L }
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
            deviceModel = settings.deviceName.value.ifBlank { com.pixelgram.desktop.core.DeviceInfo.model }
            systemVersion = "${System.getProperty("os.name")} ${System.getProperty("os.version")}"
            applicationVersion = BuildConfig.VERSION
        }
        send(parameters) { result ->
            if (result is TdApi.Error) {
                AppLog.write("TDLib parameters error ${result.code}: ${result.message}")
                authError = friendlyError(result.message)
            }
        }
    }

    var findDuplicate: (phone: String?, userId: Long) -> Telegram? = { _, _ -> null }
    var onDuplicate: (existing: Telegram) -> Unit = {}

    fun sendPhone(phone: String) {
        val digits = phone.filter { it.isDigit() }
        findDuplicate(digits, 0L)?.let { existing ->
            onDuplicate(existing)
            return
        }
        sendAuth(TdApi.SetAuthenticationPhoneNumber().apply {
            phoneNumber = phone.filter { it.isDigit() || it == '+' }
        })
    }

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

    fun chatTitle(chat: ChatState): String = when {
        isSavedMessages(chat.id) -> SavedMessagesTitle
        chat.privateUserId != 0L -> com.pixelgram.desktop.core.LocalNames.get(chat.privateUserId) ?: chat.title
        else -> chat.title
    }

    fun openSavedMessages(onChat: (Long) -> Unit) {
        if (myUserId != 0L) openPrivateChat(myUserId, onChat)
    }

    fun openPublicChat(username: String, onChat: (Long) -> Unit, onFail: () -> Unit) {
        send(TdApi.SearchPublicChat().apply { this.username = username }) { result ->
            if (result is TdApi.Chat) onChat(result.id) else onFail()
        }
    }

    val botStarts = mutableStateMapOf<Long, String>()

    fun startBot(username: String, parameter: String, onChat: (Long) -> Unit, onFail: () -> Unit) {
        send(TdApi.SearchPublicChat().apply { this.username = username }) { result ->
            val chat = result as? TdApi.Chat ?: return@send onFail()
            if (chat.type !is TdApi.ChatTypePrivate) return@send onFail()
            botStarts[chat.id] = parameter
            onChat(chat.id)
        }
    }

    fun needsStart(chat: ChatState): Boolean =
        isBot(chat) && !isBlocked(chat) && (botStarts.containsKey(chat.id) || (chat.lastMessage == null && messages(chat.id).isEmpty()))

    fun sendStart(chat: ChatState) {
        val parameter = botStarts.remove(chat.id).orEmpty()
        if (parameter.isNotEmpty()) {
            send(TdApi.SendBotStartMessage(chat.privateUserId, chat.id, parameter)) { sent ->
                if (sent is TdApi.Error) AppLog.write("Bot start failed ${sent.code}: ${sent.message}")
            }
        } else {
            sendText(chat.id, "/start")
        }
    }

    fun sendBanner(path: String?, done: (String?) -> Unit) {
        if (System.getProperty("pixelgram.demo") == "true") return done(null)
        send(TdApi.SearchPublicChat().apply { username = com.pixelgram.desktop.core.Banners.Bot }) { chat ->
            if (chat !is TdApi.Chat) return@send done("Couldn't reach @${com.pixelgram.desktop.core.Banners.Bot}")
            val content: TdApi.InputMessageContent = if (path != null) {
                TdApi.InputMessageDocument().apply {
                    document = TdApi.InputDocument(TdApi.InputFileLocal(path), null, true)
                    caption = TdApi.FormattedText("/banner", emptyArray())
                }
            } else {
                TdApi.InputMessageText().apply { text = TdApi.FormattedText("/remove", emptyArray()) }
            }
            send(TdApi.SendMessage().apply {
                chatId = chat.id
                inputMessageContent = content
            }) { result -> done((result as? TdApi.Error)?.message?.let(::friendlyError)) }
        }
    }

    fun sendBadge(badge: com.pixelgram.desktop.core.CustomBadge?, done: (String?) -> Unit) {
        if (System.getProperty("pixelgram.demo") == "true") return done(null)
        send(TdApi.SearchPublicChat().apply { username = com.pixelgram.desktop.core.Banners.Bot }) { chat ->
            if (chat !is TdApi.Chat) return@send done("Couldn't reach @${com.pixelgram.desktop.core.Banners.Bot}")
            send(TdApi.SendMessage().apply {
                chatId = chat.id
                inputMessageContent = TdApi.InputMessageText().apply { text = TdApi.FormattedText(badge?.command() ?: "/badge", emptyArray()) }
            }) { result -> done((result as? TdApi.Error)?.message?.let(::friendlyError)) }
        }
    }

    fun internalLinkType(url: String, onResult: (TdApi.InternalLinkType?) -> Unit) {
        send(TdApi.GetInternalLinkType(url)) { result -> onResult(result as? TdApi.InternalLinkType) }
    }

    fun messageLinkInfo(url: String, onResult: (Long, Long) -> Unit) {
        send(TdApi.GetMessageLinkInfo(url)) { result ->
            val info = result as? TdApi.MessageLinkInfo
            onResult(info?.chatId ?: 0L, info?.message?.id ?: 0L)
        }
    }

    fun checkInvite(link: String, done: (TdApi.ChatInviteLinkInfo?) -> Unit) {
        send(TdApi.CheckChatInviteLink(link)) { result -> done(result as? TdApi.ChatInviteLinkInfo) }
    }

    fun joinByInvite(link: String, onChat: (Long) -> Unit) {
        send(TdApi.JoinChatByInviteLink(link)) { joined -> onChat((joined as? TdApi.Chat)?.id ?: 0L) }
    }

    fun searchStickerSet(name: String, onResult: (TdApi.StickerSet?) -> Unit) {
        send(TdApi.SearchStickerSet(name, false)) { result -> onResult(result as? TdApi.StickerSet) }
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

    var contactIds by mutableStateOf<List<Long>?>(null)
        private set

    fun contacts(onResult: (List<Long>) -> Unit) {
        contactIds?.let(onResult)
        send(TdApi.GetContacts()) { result ->
            val ids = (result as? TdApi.Users)?.userIds?.toList().orEmpty()
            contactIds = ids
            onResult(ids)
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

    fun privacyRules(setting: TdApi.UserPrivacySetting, onResult: (TdApi.UserPrivacySettingRules?) -> Unit) {
        send(TdApi.GetUserPrivacySettingRules(setting)) { result -> onResult(result as? TdApi.UserPrivacySettingRules) }
    }

    fun setPrivacyRules(setting: TdApi.UserPrivacySetting, rules: TdApi.UserPrivacySettingRules, done: (String?) -> Unit) {
        send(TdApi.SetUserPrivacySettingRules(setting, rules)) { result -> done((result as? TdApi.Error)?.message?.let(::friendlyError)) }
    }

    fun newChatPrivacy(onResult: (TdApi.NewChatPrivacySettings?) -> Unit) {
        send(TdApi.GetNewChatPrivacySettings()) { result -> onResult(result as? TdApi.NewChatPrivacySettings) }
    }

    fun setNewChatPrivacy(value: TdApi.NewChatPrivacySettings, done: (String?) -> Unit) {
        send(TdApi.SetNewChatPrivacySettings(value)) { result -> done((result as? TdApi.Error)?.message?.let(::friendlyError)) }
    }

    fun terminateSession(id: Long, done: (String?) -> Unit) {
        send(TdApi.TerminateSession(id)) { result -> done((result as? TdApi.Error)?.message?.let(::friendlyError)) }
    }

    fun terminateOtherSessions(done: (String?) -> Unit) {
        send(TdApi.TerminateAllOtherSessions()) { result -> done((result as? TdApi.Error)?.message?.let(::friendlyError)) }
    }

    fun activeSessions(onResult: (List<TdApi.Session>) -> Unit) {
        if (System.getProperty("pixelgram.demo") == "true") return onResult(demoSessions())
        send(TdApi.GetActiveSessions()) { result ->
            onResult((result as? TdApi.Sessions)?.sessions?.toList().orEmpty())
        }
    }

    fun isOnline(chat: ChatState): Boolean {
        val userId = chat.privateUserId
        if (userId == 0L || isSavedMessages(chat.id) || users[userId]?.type is TdApi.UserTypeBot) return false
        return statuses[userId] is TdApi.UserStatusOnline
    }

    private val supergroupMembers = KeyedState<Long, Int>()
    private val basicGroupMembers = KeyedState<Long, Int>()
    private val memberRequests = mutableSetOf<Long>()

    private fun memberCount(chat: ChatState): Int? = when {
        chat.basicGroupId != 0L -> basicGroupMembers[chat.basicGroupId]
        chat.supergroupId != 0L -> supergroupMembers[chat.supergroupId] ?: run {
            if (memberRequests.add(chat.supergroupId)) send(TdApi.GetSupergroupFullInfo(chat.supergroupId))
            null
        }
        else -> null
    }

    private fun membersLabel(chat: ChatState): String? {
        val count = memberCount(chat) ?: return null
        val noun = if (chat.isChannel) "subscriber" else "member"
        return "%,d $noun${if (count == 1) "" else "s"}".format(java.util.Locale.US, count).replace(',', ' ')
    }

    fun activity(chat: ChatState): Presence? {
        val (sender, action) = chatActions[chat.id]?.lastOrNull() ?: return null
        val label = actionLabel(action) ?: return null
        if (chat.privateUserId != 0L) return Presence(label, true)
        return Presence(senderTitle(sender).substringBefore(' ') + ": " + label, true)
    }

    private fun actionLabel(action: TdApi.ChatAction): String? {
        val tr = com.pixelgram.desktop.core.I18n::tr
        return when (action) {
            is TdApi.ChatActionTyping -> tr("typing…")
            is TdApi.ChatActionRecordingVoiceNote -> tr("recording a voice message")
            is TdApi.ChatActionUploadingVoiceNote -> tr("sending a voice message")
            is TdApi.ChatActionRecordingVideoNote -> tr("recording a video message")
            is TdApi.ChatActionUploadingVideoNote -> tr("sending a video message")
            is TdApi.ChatActionRecordingVideo -> tr("recording a video")
            is TdApi.ChatActionUploadingVideo -> tr("sending a video")
            is TdApi.ChatActionUploadingPhoto -> tr("sending a photo")
            is TdApi.ChatActionUploadingDocument -> tr("sending a file")
            is TdApi.ChatActionChoosingSticker -> tr("choosing a sticker")
            is TdApi.ChatActionChoosingLocation -> tr("choosing a location")
            is TdApi.ChatActionChoosingContact -> tr("choosing a contact")
            is TdApi.ChatActionStartPlayingGame -> tr("playing a game")
            is TdApi.ChatActionWatchingAnimations -> tr("watching") + " " + action.emoji
            else -> null
        }
    }

    private fun sameSender(a: TdApi.MessageSender, b: TdApi.MessageSender) = when {
        a is TdApi.MessageSenderUser && b is TdApi.MessageSenderUser -> a.userId == b.userId
        a is TdApi.MessageSenderChat && b is TdApi.MessageSenderChat -> a.chatId == b.chatId
        else -> false
    }

    fun presence(chat: ChatState): Presence? {
        val userId = chat.privateUserId
        if (isSavedMessages(chat.id)) return null
        if (userId == 0L) return membersLabel(chat)?.let { Presence(it, false) }
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

    private val userRequests = mutableSetOf<Long>()

    fun ensureUser(userId: Long) {
        if (userId == 0L || users[userId] != null || !userRequests.add(userId)) return
        send(TdApi.GetUser(userId)) { result -> if (result is TdApi.User) users[userId] = result }
    }

    fun userName(userId: Long): String {
        ensureUser(userId)
        com.pixelgram.desktop.core.LocalNames.get(userId)?.let { return it }
        return users[userId]?.fullName() ?: ""
    }

    fun myFullInfo(onResult: (TdApi.UserFullInfo?) -> Unit) {
        if (myUserId == 0L) return onResult(null)
        userFullInfo(myUserId, onResult)
    }

    private fun okOrError(done: (String?) -> Unit): (TdApi.Object) -> Unit = { result ->
        done((result as? TdApi.Error)?.message?.let(::friendlyError))
    }

    fun setName(first: String, last: String, done: (String?) -> Unit) =
        send(TdApi.SetName(first.trim(), last.trim()), okOrError(done))

    fun setUsername(username: String, done: (String?) -> Unit) =
        send(TdApi.SetUsername(username.trim().removePrefix("@")), okOrError(done))

    fun setBio(bio: String, done: (String?) -> Unit) = send(TdApi.SetBio(bio.trim()), okOrError(done))

    fun setBirthdate(day: Int, month: Int, year: Int, done: (String?) -> Unit) {
        val date = if (day == 0 || month == 0) null else TdApi.Birthdate(day, month, year)
        send(TdApi.SetBirthdate(date), okOrError(done))
    }

    fun setProfilePhoto(path: String, done: (String?) -> Unit) =
        send(TdApi.SetProfilePhoto(TdApi.InputChatPhotoStatic(TdApi.InputFileLocal(path)), false), okOrError(done))

    fun moveProfileAudio(fileId: Int, afterFileId: Int) {
        send(TdApi.SetProfileAudioPosition(fileId, afterFileId)) { loadMyProfileAudios() }
    }

    val isPremium: Boolean get() = me?.isPremium == true

    fun sendContent(chatId: Long, content: TdApi.InputMessageContent, replyToId: Long = 0, done: (String?) -> Unit = {}) {
        send(TdApi.SendMessage().apply {
            this.chatId = chatId
            replyTo = replyInput(replyToId)
            inputMessageContent = content
        }) { result -> done((result as? TdApi.Error)?.message) }
    }

    fun toggleChecklistTask(message: UiMessage, taskId: Int, done: Boolean) {
        send(TdApi.MarkChecklistTasksAsDone(message.chatId, message.id, if (done) intArrayOf(taskId) else IntArray(0), if (done) IntArray(0) else intArrayOf(taskId)))
    }

    fun composeArticle(prompt: String, done: (TdApi.RichMessage?, String?) -> Unit) {
        send(TdApi.CreateRichMessageWithAi(prompt, com.pixelgram.desktop.core.I18n.code, false)) { result ->
            done(result as? TdApi.RichMessage, (result as? TdApi.Error)?.message)
        }
    }

    fun fullRichMessage(message: UiMessage, done: (TdApi.RichMessage?) -> Unit) {
        send(TdApi.GetFullRichMessage(message.chatId, message.id)) { result -> done(result as? TdApi.RichMessage) }
    }

    fun sendAudio(chatId: Long, audio: TdApi.Audio) {
        send(TdApi.SendMessage().apply {
            this.chatId = chatId
            inputMessageContent = TdApi.InputMessageAudio().apply {
                this.audio = TdApi.InputAudio().apply {
                    this.audio = remoteInput(audio.audio)
                    duration = audio.duration
                    title = audio.title
                    performer = audio.performer
                }
            }
        })
    }

    suspend fun downloaded(file: TdApi.File): String? {
        download(file)
        repeat(DownloadWaitSteps) {
            localPath(file)?.let { return it }
            kotlinx.coroutines.delay(DownloadWaitMs)
        }
        return localPath(file)
    }

    fun setProfileVideo(path: String, done: (String?) -> Unit) =
        send(TdApi.SetProfilePhoto(TdApi.InputChatPhotoAnimation(TdApi.InputFileLocal(path), 0.0), false), okOrError(done))

    fun setPreviousProfilePhoto(photoId: Long, done: (String?) -> Unit) =
        send(TdApi.SetProfilePhoto(TdApi.InputChatPhotoPrevious(photoId), false), okOrError(done))

    fun deleteProfilePhoto(photoId: Long, done: (String?) -> Unit) =
        send(TdApi.DeleteProfilePhoto(photoId), okOrError(done))

    fun profilePhotos(userId: Long, onResult: (List<TdApi.ChatPhoto>) -> Unit) {
        send(TdApi.GetUserProfilePhotos(userId, 0, ProfilePhotosLimit)) { result ->
            onResult((result as? TdApi.ChatPhotos)?.photos?.toList().orEmpty())
        }
    }

    fun userFullInfo(userId: Long, onResult: (TdApi.UserFullInfo?) -> Unit) {
        send(TdApi.GetUserFullInfo().apply { this.userId = userId }) { result ->
            (result as? TdApi.UserFullInfo)?.let { blockedUsers[userId] = it.blockList != null }
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
            is TdApi.UpdateChatReplyMarkup -> chats[update.chatId]?.let { chat ->
                chat.replyMarkupMessageId = update.replyMarkupMessage?.id ?: 0L
                chat.keyboard = update.replyMarkupMessage?.replyMarkup as? TdApi.ReplyMarkupShowKeyboard
            }
            is TdApi.UpdateChatDraftMessage -> chats[update.chatId]?.let { chat ->
                val date = update.draftMessage?.date
                val echo = date != null && date <= (localDraftDates[update.chatId] ?: 0)
                if (!echo) chat.draft = draftText(update.draftMessage)
                update.positions?.forEach { applyPosition(chat, it) }
            }
            is TdApi.UpdateChatPosition -> chats[update.chatId]?.let { applyPosition(it, update.position) }
            is TdApi.UpdateChatBackground -> chats[update.chatId]?.background = update.background
            is TdApi.UpdateSavedMessagesTopic -> savedTopics[update.topic.id] = update.topic
            is TdApi.UpdateChatBlockList -> chats[update.chatId]?.privateUserId?.takeIf { it != 0L }?.let { userId ->
                blockedUsers[userId] = update.blockList != null
            }
            is TdApi.UpdateChatReadInbox -> chats[update.chatId]?.let { chat ->
                chat.unreadCount = update.unreadCount
                chatListsChanged()
            }
            is TdApi.UpdateChatUnreadMentionCount -> chats[update.chatId]?.let { chat ->
                chat.unreadMentions = update.unreadMentionCount
                chatListsChanged()
            }
            is TdApi.UpdateMessageMentionRead -> chats[update.chatId]?.let { chat ->
                chat.unreadMentions = update.unreadMentionCount
                chatListsChanged()
            }
            is TdApi.UpdateChatIsMarkedAsUnread -> chats[update.chatId]?.let { chat ->
                chat.markedUnread = update.isMarkedAsUnread
                chatListsChanged()
            }
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
            is TdApi.UpdateUnreadMessageCount -> ChatListKey.of(update.chatList)?.let { key ->
                unreadMessages[key] = UnreadCount(update.unreadCount, update.unreadUnmutedCount)
            }
            is TdApi.UpdateReactionNotificationSettings -> reactionNotifications = update.notificationSettings
            is TdApi.UpdateChatReadOutbox -> chats[update.chatId]?.lastReadOutbox = update.lastReadOutboxMessageId
            is TdApi.UpdateMessageSendFailed -> messages[update.message.chatId]?.let { list ->
                val index = list.indexOfFirst { it.id == update.oldMessageId }
                if (index >= 0) list[index] = update.message.toUi().copy(key = list[index].key)
            }
            is TdApi.UpdateUserStatus -> statuses[update.userId] = update.status
            is TdApi.UpdateChatAction -> {
                val others = chatActions[update.chatId].orEmpty().filterNot { sameSender(it.first, update.senderId) }
                chatActions[update.chatId] = if (update.action is TdApi.ChatActionCancel) others else others + (update.senderId to update.action)
            }
            is TdApi.UpdateAnimatedEmojiMessageClicked -> emojiClicks[update.chatId to update.messageId] = update.sticker
            is TdApi.UpdateUser -> {
                users[update.user.id] = update.user
                update.user.status?.let { statuses[update.user.id] = it }
                if (update.user.id == myUserId) me = update.user
            }
            is TdApi.UpdateFile -> {
                val file = update.file
                val local = file.local
                if (local != null && local.isDownloadingCompleted) {
                    filePaths[file.id] = local.path
                }
                val total = maxOf(file.size, file.expectedSize).toFloat()
                when {
                    total > 0 && local?.isDownloadingActive == true -> transfers[file.id] = local.downloadedSize / total
                    total > 0 && file.remote?.isUploadingActive == true -> transfers[file.id] = file.remote.uploadedSize / total
                    else -> transfers.remove(file.id)
                }
            }
            is TdApi.UpdateChatFolders -> {
                folders.clear()
                update.chatFolders?.forEach { info ->
                    folders.add(FolderInfo(info.id, info.name?.text?.text ?: "", info.icon?.name ?: ""))
                }
            }
            is TdApi.UpdateNewMessage -> {
                activeThread?.let { (threadChat, threadId) ->
                    val topic = update.message.topicId as? TdApi.MessageTopicThread
                    if (update.message.chatId == threadChat && topic?.messageThreadId == threadId && threadMessages.none { it.id == update.message.id }) {
                        threadMessages.add(0, update.message.toUi().copy(fresh = true))
                    }
                }
                messages[update.message.chatId]?.takeIf { detached[update.message.chatId] != true }?.let { list ->
                    if (list.none { it.id == update.message.id }) {
                        list.add(0, update.message.toUi().copy(fresh = true))
                    }
                }
                if (update.message.chatId == openChatId && windowFocused && !update.message.isOutgoing) {
                    markRead(update.message.chatId, listOf(update.message.id))
                }
                if (shouldNotify(update.message)) {
                    Notifications.onMessage?.invoke(this, update.message)
                }
            }
            is TdApi.UpdateCall -> com.pixelgram.desktop.calls.Calls.onUpdate(this, update.call)
            is TdApi.UpdateNewCallSignalingData -> com.pixelgram.desktop.calls.Calls.onSignaling(this, update.callId, update.data)
            is TdApi.UpdateChatNotificationSettings -> {
                muteOverrides.remove(update.chatId)
                chats[update.chatId]?.notifications = update.notificationSettings
            }
            is TdApi.UpdateScopeNotificationSettings ->
                scopeNotifications[update.scope.javaClass] = update.notificationSettings
            is TdApi.UpdateMessageSendSucceeded -> messages[update.message.chatId]?.let { list ->
                val index = list.indexOfFirst { it.id == update.oldMessageId }
                if (index >= 0) list[index] = update.message.toUi().copy(key = list[index].key, fresh = true)
            }
            is TdApi.UpdatePoll -> messages.values.forEach { list ->
                list.forEachIndexed { index, message ->
                    val old = message.content as? TdApi.MessagePoll ?: return@forEachIndexed
                    if (old.poll.id != update.poll.id) return@forEachIndexed
                    list[index] = message.copy(content = TdApi.MessagePoll().apply {
                        poll = update.poll
                        description = old.description
                        media = old.media
                        canAddOption = old.canAddOption
                    })
                }
            }
            is TdApi.UpdateMessageEdited -> messages[update.chatId]?.let { list ->
                val index = list.indexOfFirst { it.id == update.messageId }
                if (index >= 0) list[index] = list[index].copy(edited = update.editDate > 0, keyboard = inlineRows(update.replyMarkup))
            }
            is TdApi.UpdateMessageInteractionInfo -> messages[update.chatId]?.let { list ->
                val index = list.indexOfFirst { it.id == update.messageId }
                if (index >= 0) {
                    list[index] = list[index].copy(
                        reactions = update.interactionInfo?.reactions?.reactions?.toList().orEmpty(),
                        views = update.interactionInfo?.viewCount ?: list[index].views,
                        comments = update.interactionInfo?.replyInfo?.replyCount ?: list[index].comments,
                    )
                }
            }
            is TdApi.UpdateSupergroup -> {
                forums[update.supergroup.id] = update.supergroup.isForum
                supergroupInfo[update.supergroup.id] = update.supergroup
                officialSupergroups[update.supergroup.id] = update.supergroup.usernames?.activeUsernames
                    ?.any { it.equals(OfficialChannel, ignoreCase = true) } == true
                if (update.supergroup.memberCount > 0) supergroupMembers[update.supergroup.id] = update.supergroup.memberCount
            }
            is TdApi.UpdateBasicGroup -> basicGroupMembers[update.basicGroup.id] = update.basicGroup.memberCount
            is TdApi.UpdateSupergroupFullInfo ->
                if (update.supergroupFullInfo.memberCount > 0) supergroupMembers[update.supergroupId] = update.supergroupFullInfo.memberCount
            is TdApi.UpdateMessageIsPinned -> {
                messages[update.chatId]?.let { list ->
                    val index = list.indexOfFirst { it.id == update.messageId }
                    if (index >= 0) list[index] = list[index].copy(pinned = update.isPinned)
                }
                if (pinnedMessages.containsKey(update.chatId)) loadPinned(update.chatId)
            }
            is TdApi.UpdateMessageContent -> messages[update.chatId]?.let { list ->
                val index = list.indexOfFirst { it.id == update.messageId }
                if (index >= 0) {
                    val content = update.newContent
                    list[index] = list[index].copy(text = messageText(content), content = content, entities = messageEntities(content))
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
        AppLog.write(
            "Auth state: " + state.javaClass.simpleName.removePrefix("AuthorizationState") +
                ((state as? TdApi.AuthorizationStateWaitCode)?.codeInfo?.let { info ->
                    ", code ${info.type?.javaClass?.simpleName?.removePrefix("AuthenticationCodeType")}, next ${info.nextType?.javaClass?.simpleName?.removePrefix("AuthenticationCodeType")}, timeout ${info.timeout}"
                } ?: "")
        )
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
                        val existing = findDuplicate(null, result.id)
                        if (existing != null) {
                            onDuplicate(existing)
                            logOut()
                            return@send
                        }
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
            unreadMentions = chat.unreadMentionCount
            markedUnread = chat.isMarkedAsUnread
            lastReadOutbox = chat.lastReadOutboxMessageId
            isGroup = chat.type is TdApi.ChatTypeBasicGroup || chat.type is TdApi.ChatTypeSupergroup
            privateUserId = (chat.type as? TdApi.ChatTypePrivate)?.userId ?: 0L
            isChannel = (chat.type as? TdApi.ChatTypeSupergroup)?.isChannel == true
            supergroupId = (chat.type as? TdApi.ChatTypeSupergroup)?.supergroupId ?: 0L
            basicGroupId = (chat.type as? TdApi.ChatTypeBasicGroup)?.basicGroupId ?: 0L
            draft = draftText(chat.draftMessage)
            replyMarkupMessageId = chat.replyMarkupMessageId
            notifications = chat.notificationSettings
            background = chat.background
        }
        if (state.privateUserId != 0L) blockedUsers[state.privateUserId] = chat.blockList != null
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
                val source = if (key == ChatListKey.Unread || key == ChatListKey.Mentions) ChatListKey.Main else key
                val ordered = ArrayList<Pair<ChatState, Long>>()
                for (chat in chats.values) {
                    val order = chat.positions[source] ?: continue
                    val keep = when (key) {
                        ChatListKey.Unread -> chat.unreadCount > 0 || chat.markedUnread
                        ChatListKey.Mentions -> chat.unreadMentions > 0
                        else -> true
                    }
                    if (keep) ordered += chat to order
                }
                ordered.sortByDescending { it.second }
                ordered.map { it.first }
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
            ChatListKey.Main, ChatListKey.Unread, ChatListKey.Mentions -> TdApi.ChatListMain()
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

    private val scopeNotifications = mutableStateMapOf<Class<*>, TdApi.ScopeNotificationSettings>()
    var reactionNotifications by mutableStateOf<TdApi.ReactionNotificationSettings?>(null)
        private set
    private var openChatId = 0L

    var windowFocused = true
        set(value) {
            if (value && !field) readOpenChat()
            field = value
        }

    private fun readOpenChat() {
        val chatId = openChatId.takeIf { it != 0L } ?: return
        val ids = messages(chatId).take(50).filter { !it.isOutgoing }.map { it.id }
        if (ids.isNotEmpty()) markRead(chatId, ids)
    }

    private fun shouldNotify(message: TdApi.Message): Boolean {
        if (message.isOutgoing || settings.notificationsMuted.value) return false
        if (message.chatId == openChatId && windowFocused) return false
        val chat = chats[message.chatId] ?: return false
        if (message.content is TdApi.MessageContactRegistered && contactJoinedMuted) return false
        if (message.content is TdApi.MessagePinMessage && scopeSettings(scopeOf(chat))?.disablePinnedMessageNotifications == true) return false
        return !isMuted(chat)
    }

    fun scopeOf(chat: ChatState): TdApi.NotificationSettingsScope = when {
        chat.privateUserId != 0L -> TdApi.NotificationSettingsScopePrivateChats()
        chat.isChannel -> TdApi.NotificationSettingsScopeChannelChats()
        else -> TdApi.NotificationSettingsScopeGroupChats()
    }

    fun scopeSettings(scope: TdApi.NotificationSettingsScope): TdApi.ScopeNotificationSettings? = scopeNotifications[scope.javaClass]

    fun setScopeMuted(scope: TdApi.NotificationSettingsScope, muted: Boolean) =
        updateScope(scope) { muteFor = if (muted) MuteForever else 0 }

    fun setPinnedNotifications(enabled: Boolean) = listOf(
        TdApi.NotificationSettingsScopePrivateChats(),
        TdApi.NotificationSettingsScopeGroupChats(),
        TdApi.NotificationSettingsScopeChannelChats(),
    ).forEach { scope -> updateScope(scope) { disablePinnedMessageNotifications = !enabled } }

    private fun updateScope(scope: TdApi.NotificationSettingsScope, change: TdApi.ScopeNotificationSettings.() -> Unit) {
        val current = scopeNotifications[scope.javaClass] ?: TdApi.ScopeNotificationSettings().apply {
            showPreview = true
            useDefaultMuteStories = true
            showStoryPoster = true
        }
        val next = TdApi.ScopeNotificationSettings(
            current.muteFor, current.soundId, current.showPreview, current.useDefaultMuteStories, current.muteStories,
            current.storySoundId, current.showStoryPoster, current.disablePinnedMessageNotifications, current.disableMentionNotifications,
        ).apply(change)
        scopeNotifications[scope.javaClass] = next
        send(TdApi.SetScopeNotificationSettings(scope, next)) { result ->
            if (result is TdApi.Error) AppLog.write("Scope notifications failed ${result.code}: ${result.message}")
        }
    }

    fun notificationExceptions(scope: TdApi.NotificationSettingsScope, onResult: (Int) -> Unit) {
        send(TdApi.GetChatNotificationSettingsExceptions(scope, false)) { result -> onResult((result as? TdApi.Chats)?.totalCount ?: 0) }
    }

    var contactJoinedMuted by mutableStateOf(false)
        private set

    fun loadContactJoined() {
        send(TdApi.GetOption("disable_contact_registered_notifications")) { result ->
            contactJoinedMuted = (result as? TdApi.OptionValueBoolean)?.value == true
        }
    }

    fun setContactJoined(enabled: Boolean) {
        contactJoinedMuted = !enabled
        send(TdApi.SetOption("disable_contact_registered_notifications", TdApi.OptionValueBoolean(!enabled)))
    }

    fun setReactionNotifications(enabled: Boolean) {
        val current = reactionNotifications ?: TdApi.ReactionNotificationSettings().apply {
            storyReactionSource = TdApi.ReactionNotificationSourceContacts()
            showPreview = true
        }
        val source = if (enabled) TdApi.ReactionNotificationSourceAll() else TdApi.ReactionNotificationSourceNone()
        val next = TdApi.ReactionNotificationSettings(source, current.storyReactionSource, source, current.soundId, current.showPreview)
        reactionNotifications = next
        send(TdApi.SetReactionNotificationSettings(next))
    }

    private val muteOverrides = mutableStateMapOf<Long, Boolean>()

    fun isMuted(chat: ChatState): Boolean {
        muteOverrides[chat.id]?.let { return it }
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
        return muteFor != 0
    }

    fun setMuted(chat: ChatState, muted: Boolean) {
        val settings = (chat.notifications ?: TdApi.ChatNotificationSettings().apply {
            useDefaultSound = true
            useDefaultShowPreview = true
            useDefaultMuteStories = true
            useDefaultStorySound = true
            useDefaultShowStoryPoster = true
            useDefaultDisablePinnedMessageNotifications = true
            useDefaultDisableMentionNotifications = true
        }).apply {
            useDefaultMuteFor = false
            muteFor = if (muted) MuteForever else 0
        }
        muteOverrides[chat.id] = muted
        send(TdApi.SetChatNotificationSettings(chat.id, settings)) { result ->
            if (result is TdApi.Error) muteOverrides.remove(chat.id)
        }
    }

    fun setPinned(chat: ChatState, key: ChatListKey, pinned: Boolean) {
        send(TdApi.ToggleChatIsPinned(key.toTd(), chat.id, pinned))
    }

    fun setArchived(chat: ChatState, archived: Boolean) {
        send(TdApi.AddChatToList(chat.id, if (archived) TdApi.ChatListArchive() else TdApi.ChatListMain()))
    }

    fun isBot(chat: ChatState): Boolean = users[chat.privateUserId]?.type is TdApi.UserTypeBot

    private val forums = KeyedState<Long, Boolean>()
    private val supergroupInfo = KeyedState<Long, TdApi.Supergroup>()

    fun canPost(chat: ChatState): Boolean {
        if (!chat.isChannel) return true
        return when (val status = supergroupInfo[chat.supergroupId]?.status) {
            is TdApi.ChatMemberStatusCreator -> true
            is TdApi.ChatMemberStatusAdministrator -> status.rights?.canPostMessages == true
            else -> false
        }
    }
    fun isAdmin(chat: ChatState): Boolean = when (supergroupInfo[chat.supergroupId]?.status) {
        is TdApi.ChatMemberStatusCreator, is TdApi.ChatMemberStatusAdministrator -> true
        else -> false
    }

    fun membersCount(chat: ChatState): Int? = memberCount(chat)

    fun administratorCount(chat: ChatState, onResult: (Int) -> Unit) {
        if (chat.supergroupId == 0L) return onResult(0)
        send(TdApi.GetSupergroupFullInfo(chat.supergroupId)) { result -> onResult((result as? TdApi.SupergroupFullInfo)?.administratorCount ?: 0) }
    }

    fun chatMembers(chat: ChatState, admins: Boolean, onResult: (List<TdApi.ChatMember>) -> Unit) {
        val deliver = { list: List<TdApi.ChatMember> ->
            list.forEach { member -> (member.memberId as? TdApi.MessageSenderUser)?.let { ensureUser(it.userId) } }
            onResult(list)
        }
        when {
            chat.basicGroupId != 0L -> send(TdApi.GetBasicGroupFullInfo(chat.basicGroupId)) { result ->
                val members = (result as? TdApi.BasicGroupFullInfo)?.members?.toList().orEmpty()
                deliver(if (admins) members.filter { it.status is TdApi.ChatMemberStatusCreator || it.status is TdApi.ChatMemberStatusAdministrator } else members)
            }
            chat.supergroupId != 0L -> {
                val filter = if (admins) TdApi.SupergroupMembersFilterAdministrators() else TdApi.SupergroupMembersFilterRecent()
                send(TdApi.GetSupergroupMembers(chat.supergroupId, filter, 0, MembersLimit)) { result ->
                    deliver((result as? TdApi.ChatMembers)?.members?.toList().orEmpty())
                }
            }
            else -> onResult(emptyList())
        }
    }

    private val officialSupergroups = KeyedState<Long, Boolean>()

    fun isOfficial(chat: ChatState): Boolean =
        chat.supergroupId != 0L && officialSupergroups[chat.supergroupId] == true ||
            System.getProperty("pixelgram.demo") == "true" && chat.id == DemoOfficialChat

    fun isVerified(chat: ChatState): Boolean = when {
        chat.privateUserId != 0L -> users[chat.privateUserId]?.verificationStatus?.isVerified == true
        chat.supergroupId != 0L -> supergroupInfo[chat.supergroupId]?.verificationStatus?.isVerified == true
        else -> false
    }

    fun usernames(chat: ChatState): List<String> = when {
        chat.privateUserId != 0L -> users[chat.privateUserId]?.usernames?.activeUsernames?.toList()
        chat.supergroupId != 0L -> supergroupInfo[chat.supergroupId]?.usernames?.activeUsernames?.toList()
        else -> null
    }.orEmpty()

    fun chatDescription(chat: ChatState, onResult: (String) -> Unit) {
        when {
            chat.supergroupId != 0L -> send(TdApi.GetSupergroupFullInfo(chat.supergroupId)) { result ->
                onResult((result as? TdApi.SupergroupFullInfo)?.description.orEmpty())
            }
            chat.basicGroupId != 0L -> send(TdApi.GetBasicGroupFullInfo(chat.basicGroupId)) { result ->
                onResult((result as? TdApi.BasicGroupFullInfo)?.description.orEmpty())
            }
            else -> onResult("")
        }
    }

    fun avatarKind(chat: ChatState): AvatarKind = when {
        chat.privateUserId != 0L -> if (isBot(chat)) AvatarKind.Bot else AvatarKind.Private
        chat.isChannel -> AvatarKind.Channel
        forums[chat.supergroupId] == true -> AvatarKind.Forum
        chat.isGroup -> AvatarKind.Group
        else -> AvatarKind.Private
    }

    private val blockedUsers = KeyedState<Long, Boolean>()

    fun isBlocked(chat: ChatState): Boolean = chat.privateUserId != 0L && blockedUsers[chat.privateUserId] == true

    fun block(chat: ChatState) = setBlocked(chat, true)

    fun unblock(chat: ChatState) = setBlocked(chat, false)

    private fun setBlocked(chat: ChatState, blocked: Boolean) {
        val userId = chat.privateUserId
        if (userId == 0L) return
        val before = blockedUsers[userId]
        blockedUsers[userId] = blocked
        send(TdApi.SetMessageSenderBlockList(TdApi.MessageSenderUser(userId), if (blocked) TdApi.BlockListMain() else null)) { result ->
            if (result is TdApi.Error) {
                if (before == null) blockedUsers.remove(userId) else blockedUsers[userId] = before
            }
        }
    }

    fun clearHistory(chat: ChatState) {
        send(TdApi.DeleteChatHistory(chat.id, false, false))
        messages[chat.id]?.clear()
    }

    fun deleteChat(chat: ChatState) {
        if (chat.privateUserId != 0L) send(TdApi.DeleteChatHistory(chat.id, true, false)) else send(TdApi.LeaveChat(chat.id))
    }

    fun animatedEmoji(emoji: String, done: (TdApi.Sticker?) -> Unit) {
        send(TdApi.GetAnimatedEmoji(emoji)) { result -> done((result as? TdApi.AnimatedEmoji)?.sticker) }
    }

    fun isContact(chat: ChatState): Boolean = chat.privateUserId != 0L && users[chat.privateUserId]?.isContact == true

    fun saveContact(userId: Long, firstName: String, lastName: String, onDone: (Boolean) -> Unit = {}) {
        val phone = users[userId]?.phoneNumber.orEmpty()
        send(TdApi.AddContact(userId, TdApi.ImportedContact(phone, firstName, lastName, null), false)) { result -> onDone(result is TdApi.Ok) }
    }

    fun removeContact(userId: Long) {
        send(TdApi.RemoveContacts(longArrayOf(userId)))
    }

    fun senderPhoto(sender: TdApi.MessageSender?): TdApi.File? = when (sender) {
        is TdApi.MessageSenderUser -> users[sender.userId]?.profilePhoto?.small
        is TdApi.MessageSenderChat -> chats[sender.chatId]?.photo
        else -> null
    }

    fun receivedGift(id: String, onResult: (TdApi.ReceivedGift?) -> Unit) {
        send(TdApi.GetReceivedGift(id)) { onResult(it as? TdApi.ReceivedGift) }
    }

    fun profileGifts(owner: TdApi.MessageSender, onResult: (Int, List<TdApi.ReceivedGift>) -> Unit) {
        if (System.getProperty("pixelgram.demo") == "true") return demoGifts().let { onResult(it.size, it) }
        send(TdApi.GetReceivedGifts().apply {
            businessConnectionId = ""
            ownerId = owner
            excludeUnsaved = owner !is TdApi.MessageSenderUser || (owner as TdApi.MessageSenderUser).userId != myUserId
            offset = ""
            limit = ProfileGiftsLimit
        }) { result ->
            val gifts = result as? TdApi.ReceivedGifts
            gifts?.gifts?.forEach { gift -> (gift.senderId as? TdApi.MessageSenderUser)?.let { ensureUser(it.userId) } }
            onResult(gifts?.totalCount ?: 0, gifts?.gifts?.toList().orEmpty())
        }
    }

    fun upgradedGift(name: String, onResult: (TdApi.UpgradedGift?) -> Unit) {
        if (System.getProperty("pixelgram.demo") == "true") return onResult(demoUpgradedGift())
        send(TdApi.GetUpgradedGift(name)) { result ->
            val gift = result as? TdApi.UpgradedGift
            (gift?.ownerId as? TdApi.MessageSenderUser)?.let { ensureUser(it.userId) }
            onResult(gift)
        }
    }

    fun giftCollections(owner: TdApi.MessageSender, onResult: (List<TdApi.GiftCollection>) -> Unit) {
        if (System.getProperty("pixelgram.demo") == "true") return onResult(demoGiftCollections())
        send(TdApi.GetGiftCollections(owner)) { result -> onResult((result as? TdApi.GiftCollections)?.collections?.toList().orEmpty()) }
    }

    fun collectionGifts(owner: TdApi.MessageSender, collectionId: Int, onResult: (List<TdApi.ReceivedGift>) -> Unit) {
        if (System.getProperty("pixelgram.demo") == "true") return onResult(demoGifts(collectionId))
        send(TdApi.GetReceivedGifts().apply {
            businessConnectionId = ""
            ownerId = owner
            this.collectionId = collectionId
            offset = ""
            limit = ProfileGiftsLimit
        }) { result ->
            val gifts = (result as? TdApi.ReceivedGifts)?.gifts?.toList().orEmpty()
            gifts.forEach { gift -> (gift.senderId as? TdApi.MessageSenderUser)?.let { ensureUser(it.userId) } }
            onResult(gifts)
        }
    }

    fun createGiftCollection(owner: TdApi.MessageSender, name: String, done: (String?) -> Unit) {
        send(TdApi.CreateGiftCollection(owner, name, emptyArray())) { result -> done((result as? TdApi.Error)?.message?.let(::friendlyError)) }
    }

    fun postedStories(chatId: Long, onResult: (List<TdApi.Story>) -> Unit) =
        if (System.getProperty("pixelgram.demo") == "true") onResult(demoStories(chatId)) else pagedStories({ from -> TdApi.GetChatPostedToChatPageStories(chatId, from, StoryPage) }, { _, list -> onResult(list) })

    fun archivedStories(chatId: Long, onResult: (Int, List<TdApi.Story>) -> Unit) =
        if (System.getProperty("pixelgram.demo") == "true") demoStories(chatId).let { onResult(218, it) } else pagedStories({ from -> TdApi.GetChatArchivedStories(chatId, from, StoryPage) }, onResult)

    private fun pagedStories(
        request: (Int) -> TdApi.Function<TdApi.Stories>,
        onResult: (Int, List<TdApi.Story>) -> Unit,
        from: Int = 0,
        acc: List<TdApi.Story> = emptyList(),
    ) {
        send(request(from)) { result ->
            val page = result as? TdApi.Stories
            val stories = acc + page?.stories.orEmpty().filter { story -> acc.none { it.id == story.id } }
            val last = page?.stories?.lastOrNull()?.id ?: 0
            if (page == null || page.stories.size < StoryPage || last == 0 || stories.size >= StoryLimit) {
                onResult(maxOf(page?.totalCount ?: 0, stories.size), stories)
            } else {
                pagedStories(request, onResult, last, stories)
            }
        }
    }

    fun storyAlbums(chatId: Long, onResult: (List<TdApi.StoryAlbum>) -> Unit) {
        if (System.getProperty("pixelgram.demo") == "true") return onResult(demoStoryAlbums())
        send(TdApi.GetChatStoryAlbums(chatId)) { result -> onResult((result as? TdApi.StoryAlbums)?.albums?.toList().orEmpty()) }
    }

    fun albumStories(chatId: Long, albumId: Int, onResult: (List<TdApi.Story>) -> Unit) {
        if (System.getProperty("pixelgram.demo") == "true") return onResult(demoStories(chatId, albumId))
        send(TdApi.GetStoryAlbumStories(chatId, albumId, 0, StoryLimit)) { result -> onResult((result as? TdApi.Stories)?.stories?.toList().orEmpty()) }
    }

    fun createStoryAlbum(chatId: Long, name: String, done: (String?) -> Unit) {
        send(TdApi.CreateStoryAlbum(chatId, name, IntArray(0))) { result -> done((result as? TdApi.Error)?.message?.let(::friendlyError)) }
    }

    fun openStory(story: TdApi.Story) = send(TdApi.OpenStory(story.posterChatId, story.id)) {}

    fun closeStory(story: TdApi.Story) = send(TdApi.CloseStory(story.posterChatId, story.id)) {}

    fun saveGif(message: UiMessage) {
        val animation = (message.content as? TdApi.MessageAnimation)?.animation?.animation ?: return
        send(TdApi.AddSavedAnimation(TdApi.InputFileId(animation.id))) { result ->
            com.pixelgram.desktop.ui.main.TopNotice.text = com.pixelgram.desktop.core.tr(if (result is TdApi.Ok) "Saved to GIFs" else "Couldn't save the GIF")
        }
    }

    private val searchLanguages: Array<String> get() = listOf(com.pixelgram.desktop.core.I18n.code, "en").distinct().toTypedArray()

    fun searchEmojis(text: String, done: (List<String>) -> Unit) {
        send(TdApi.SearchEmojis(text, searchLanguages)) { result ->
            done((result as? TdApi.EmojiKeywords)?.emojiKeywords?.map { it.emoji }?.distinct().orEmpty())
        }
    }

    fun searchStickers(query: String, chatId: Long, done: (List<TdApi.Sticker>) -> Unit) {
        val emoji = com.pixelgram.desktop.ui.theme.emojiRanges(query).sumOf { it.last - it.first + 1 } == query.length
        send(TdApi.GetStickers(TdApi.StickerTypeRegular(), query, StickerSearchLimit, chatId)) { result ->
            val installed = (result as? TdApi.Stickers)?.stickers?.toList().orEmpty()
            val search = if (emoji) TdApi.SearchStickers(TdApi.StickerTypeRegular(), query, "", searchLanguages, 0, StickerSearchLimit)
            else TdApi.SearchStickers(TdApi.StickerTypeRegular(), "", query, searchLanguages, 0, StickerSearchLimit)
            send(search) { more ->
                val extra = (more as? TdApi.Stickers)?.stickers.orEmpty().filter { found -> installed.none { it.sticker.id == found.sticker.id } }
                done(installed + extra)
            }
        }
    }

    private val emojiReactions = java.util.concurrent.ConcurrentHashMap<String, TdApi.EmojiReaction>()

    fun emojiReaction(emoji: String, done: (TdApi.EmojiReaction?) -> Unit) {
        emojiReactions[emoji]?.let { return done(it) }
        send(TdApi.GetEmojiReaction(emoji)) { result ->
            val reaction = result as? TdApi.EmojiReaction
            if (reaction != null) emojiReactions[emoji] = reaction
            scope.launch(Dispatchers.Main) { done(reaction) }
        }
    }

    fun clickAnimatedEmoji(chatId: Long, messageId: Long, done: (TdApi.Sticker?) -> Unit) {
        send(TdApi.ClickAnimatedEmojiMessage(chatId, messageId)) { result -> done(result as? TdApi.Sticker) }
    }

    fun loadFolder(folderId: Int, done: (TdApi.ChatFolder?) -> Unit) {
        send(TdApi.GetChatFolder(folderId)) { result -> done(result as? TdApi.ChatFolder) }
    }

    fun saveFolder(folderId: Int?, folder: TdApi.ChatFolder, done: (String?) -> Unit) {
        val request = if (folderId == null) TdApi.CreateChatFolder(folder) else TdApi.EditChatFolder(folderId, folder)
        send(request) { result -> done((result as? TdApi.Error)?.message) }
    }

    fun deleteFolder(folderId: Int) {
        send(TdApi.DeleteChatFolder(folderId, longArrayOf()))
    }

    fun reorderFolders(ids: List<Int>) {
        val byId = folders.associateBy { it.id }
        val reordered = ids.mapNotNull { byId[it] }
        if (reordered.size == folders.size) {
            folders.clear()
            folders.addAll(reordered)
        }
        send(TdApi.ReorderChatFolders(ids.toIntArray(), 0))
    }

    fun addToFolder(chat: ChatState, folderId: Int) {
        send(TdApi.GetChatFolder(folderId)) { result ->
            val folder = result as? TdApi.ChatFolder ?: return@send
            if (chat.id in folder.includedChatIds) return@send
            folder.includedChatIds = folder.includedChatIds + chat.id
            folder.excludedChatIds = folder.excludedChatIds.filter { it != chat.id }.toLongArray()
            send(TdApi.EditChatFolder(folderId, folder))
        }
    }

    fun openChat(chatId: Long) {
        openChatId = chatId
        send(TdApi.OpenChat().apply { this.chatId = chatId })
        val list = messages(chatId)
        if (list.isEmpty()) loadMoreMessages(chatId) else readOpenChat()
    }

    fun closeChat(chatId: Long) {
        if (openChatId == chatId) openChatId = 0L
        if (detached.remove(chatId) == true) resetHistory(chatId)
        send(TdApi.CloseChat().apply { this.chatId = chatId })
    }

    private val historyGeneration = HashMap<Long, Int>()

    private fun generation(chatId: Long) = historyGeneration[chatId] ?: 0

    private fun resetHistory(chatId: Long): Int {
        val next = generation(chatId) + 1
        historyGeneration[chatId] = next
        messages(chatId).clear()
        return next
    }

    fun loadMoreMessages(chatId: Long, pageSize: Int = 50, onLoaded: (Int) -> Unit = {}) {
        val list = messages(chatId)
        val fromId = list.lastOrNull()?.id ?: 0L
        val requested = generation(chatId)
        send(TdApi.GetChatHistory().apply {
            this.chatId = chatId
            fromMessageId = fromId
            offset = 0
            limit = pageSize
            onlyLocal = false
        }) { result ->
            if (result !is TdApi.Messages || requested != generation(chatId)) {
                onLoaded(0)
                return@send
            }
            val oldest = list.lastOrNull()?.id ?: Long.MAX_VALUE
            val loaded = result.messages?.filterNotNull().orEmpty()
                .filter { it.id < oldest }
                .sortedByDescending { it.id }
                .map { it.toUi() }
            list.addAll(loaded)
            if (loaded.isNotEmpty()) {
                markRead(chatId, loaded.map { it.id })
            }
            onLoaded(loaded.size)
        }
    }

    var activeThread by mutableStateOf<Pair<Long, Long>?>(null)
        private set
    val threadMessages = mutableStateListOf<UiMessage>()
    var threadRoot by mutableStateOf<UiMessage?>(null)
        private set

    fun openThread(channelId: Long, messageId: Long, onFail: (String) -> Unit) {
        threadMessages.clear()
        threadRoot = null
        activeThread = null
        send(TdApi.GetMessageThread(channelId, messageId)) { result ->
            if (result !is TdApi.MessageThreadInfo) return@send onFail((result as? TdApi.Error)?.message?.let(::friendlyError) ?: "Comments are unavailable")
            activeThread = result.chatId to result.messageThreadId
            threadRoot = result.messages?.lastOrNull()?.toUi()
            send(TdApi.GetChat(result.chatId))
            send(TdApi.GetMessageThreadHistory(channelId, messageId, 0, 0, ThreadPageSize)) { history ->
                val loaded = (history as? TdApi.Messages)?.messages?.filterNotNull().orEmpty()
                    .filter { it.id != threadRoot?.id }
                    .map { it.toUi() }
                threadMessages.addAll(loaded.filter { message -> threadMessages.none { it.id == message.id } })
            }
        }
    }

    fun closeThread() {
        activeThread = null
        threadRoot = null
        threadMessages.clear()
    }

    val savedTopics = mutableStateMapOf<Long, TdApi.SavedMessagesTopic>()

    fun loadSavedTopics() {
        send(TdApi.LoadSavedMessagesTopics(SavedTopicsPage))
    }

    fun savedTopicHistory(topicId: Long, fromMessageId: Long, done: (List<UiMessage>) -> Unit) {
        send(TdApi.GetSavedMessagesTopicHistory(topicId, fromMessageId, 0, HistoryPageSize)) { result ->
            done((result as? TdApi.Messages)?.messages?.filterNotNull().orEmpty().map { it.toUi() })
        }
    }

    fun searchMedia(
        chatId: Long,
        filter: TdApi.SearchMessagesFilter,
        fromMessageId: Long,
        savedTopicId: Long = 0L,
        done: (List<UiMessage>, Long) -> Unit,
    ) {
        send(TdApi.SearchChatMessages().apply {
            this.chatId = chatId
            topicId = if (savedTopicId != 0L) TdApi.MessageTopicSavedMessages(savedTopicId) else null
            query = ""
            this.fromMessageId = fromMessageId
            offset = 0
            limit = HistoryPageSize
            this.filter = filter
        }) { result ->
            val found = result as? TdApi.FoundChatMessages
            done(found?.messages?.filterNotNull().orEmpty().map { it.toUi() }, found?.nextFromMessageId ?: 0L)
        }
    }

    fun groupsInCommon(userId: Long, done: (List<Long>) -> Unit) {
        send(TdApi.GetGroupsInCommon(userId, 0, CommonGroupsLimit)) { result ->
            val ids = (result as? TdApi.Chats)?.chatIds?.toList().orEmpty()
            ids.forEach(::loadChat)
            done(ids)
        }
    }

    fun isLeft(chat: ChatState): Boolean {
        val status = supergroupInfo[chat.supergroupId]?.status ?: return false
        return status is TdApi.ChatMemberStatusLeft
    }

    fun mustJoin(chatId: Long): Boolean {
        val chat = chats[chatId] ?: return false
        val group = supergroupInfo[chat.supergroupId] ?: return false
        val member = group.status !is TdApi.ChatMemberStatusLeft && group.status !is TdApi.ChatMemberStatusBanned
        return group.joinToSendMessages && !member
    }

    fun joinChat(chatId: Long, done: (String?) -> Unit) {
        send(TdApi.JoinChat(chatId)) { result -> done((result as? TdApi.Error)?.message?.let(::friendlyError)) }
    }

    fun sendComment(text: String) {
        val (chatId, threadId) = activeThread ?: return
        if (text.isBlank()) return
        send(TdApi.SendMessage().apply {
            this.chatId = chatId
            topicId = TdApi.MessageTopicThread(threadId)
            inputMessageContent = TdApi.InputMessageText().apply { this.text = TdApi.FormattedText(text, emptyArray()) }
        }) { result -> if (result is TdApi.Error) AppLog.write("Comment failed ${result.code}: ${result.message}") }
    }

    val detached = mutableStateMapOf<Long, Boolean>()

    fun jumpToStart(chatId: Long, done: () -> Unit) = loadAround(chatId, 1, -(JumpPageSize - 1), done)

    fun jumpToMessage(chatId: Long, messageId: Long, done: () -> Unit) = loadAround(chatId, messageId, -(JumpPageSize / 2), done)

    private fun loadAround(chatId: Long, fromId: Long, offset: Int, done: () -> Unit) {
        val requested = generation(chatId)
        val collected = LinkedHashMap<Long, TdApi.Message>()
        fun finish() {
            if (collected.isEmpty() || requested != generation(chatId)) return done()
            if (fromId > 1 && collected[fromId] == null) return done()
            resetHistory(chatId)
            messages(chatId).addAll(collected.values.sortedByDescending { it.id }.map { it.toUi() })
            detached[chatId] = true
            done()
        }
        fun fetch(from: Long, shift: Int, attempt: Int) {
            send(TdApi.GetChatHistory().apply {
                this.chatId = chatId
                fromMessageId = from
                this.offset = shift
                limit = JumpPageSize
                onlyLocal = false
            }) { result ->
                val loaded = (result as? TdApi.Messages)?.messages?.filterNotNull().orEmpty()
                loaded.forEach { collected[it.id] = it }
                val enough = collected.size >= JumpMinMessages || loaded.isEmpty() || attempt >= JumpAttempts
                if (enough || requested != generation(chatId)) {
                    finish()
                } else {
                    val oldest = collected.keys.min()
                    val newest = collected.keys.max()
                    if (fromId <= 1) fetch(newest, -(JumpPageSize - 1), attempt + 1) else fetch(oldest, 0, attempt + 1)
                }
            }
        }
        fetch(fromId, offset, 1)
    }

    private val loadingNewer = HashSet<Long>()

    fun loadNewer(chatId: Long) {
        if (detached[chatId] != true || !loadingNewer.add(chatId)) return
        val list = messages(chatId)
        val newest = list.firstOrNull()?.id ?: return run { loadingNewer.remove(chatId) }
        val requested = generation(chatId)
        send(TdApi.GetChatHistory().apply {
            this.chatId = chatId
            fromMessageId = newest
            offset = -JumpPageSize
            limit = JumpPageSize + 1
            onlyLocal = false
        }) { result ->
            loadingNewer.remove(chatId)
            if (requested != generation(chatId)) return@send
            val top = list.firstOrNull()?.id ?: newest
            val newer = (result as? TdApi.Messages)?.messages?.filterNotNull().orEmpty()
                .filter { it.id > top }
                .sortedByDescending { it.id }
            if (newer.isEmpty()) {
                detached[chatId] = false
                return@send
            }
            list.addAll(0, newer.map { it.toUi() })
        }
    }

    fun returnToLatest(chatId: Long, done: () -> Unit) {
        resetHistory(chatId)
        detached[chatId] = false
        loadMoreMessages(chatId) { done() }
    }

    suspend fun loadWholeHistory(chatId: Long) {
        repeat(HistoryPagesMax) {
            val loaded = suspendCancellableCoroutine { continuation ->
                loadMoreMessages(chatId, HistoryPageSize) { count -> if (continuation.isActive) continuation.resume(count) }
            }
            if (loaded == 0) return
        }
    }

    fun previewHistory(chatId: Long, fromId: Long, onResult: (List<UiMessage>) -> Unit) {
        val local = if (fromId == 0L) messages(chatId).toList() else emptyList()
        if (local.isNotEmpty()) onResult(local)
        if (client == null) return
        send(TdApi.GetChatHistory().apply {
            this.chatId = chatId
            fromMessageId = fromId
            offset = 0
            limit = PreviewPage
            onlyLocal = false
        }) { result ->
            val loaded = (result as? TdApi.Messages)?.messages?.filterNotNull().orEmpty()
            loaded.forEach { message -> (message.senderId as? TdApi.MessageSenderUser)?.let { ensureUser(it.userId) } }
            if (loaded.isNotEmpty() || local.isEmpty()) onResult(loaded.sortedByDescending { it.id }.map { it.toUi() })
        }
    }

    fun markChatRead(chat: ChatState) {
        val last = chat.lastMessage?.id ?: return
        markRead(chat.id, listOf(last))
        if (chat.markedUnread) send(TdApi.ToggleChatIsMarkedAsUnread(chat.id, false))
    }

    private fun markRead(chatId: Long, ids: List<Long>) {
        send(TdApi.ViewMessages().apply {
            this.chatId = chatId
            messageIds = ids.toLongArray()
            forceRead = true
        })
    }

    fun sendText(chatId: Long, text: String, replyToId: Long = 0, entities: Array<TdApi.TextEntity> = emptyArray()) {
        if (text.isBlank()) return
        send(TdApi.SendMessage().apply {
            this.chatId = chatId
            replyTo = replyInput(replyToId)
            inputMessageContent = TdApi.InputMessageText().apply {
                this.text = TdApi.FormattedText(text, entities)
            }
        })
    }

    fun editMessageMedia(message: UiMessage, file: File, caption: String, asPhoto: Boolean, entities: Array<TdApi.TextEntity> = emptyArray()) {
        val text = TdApi.FormattedText(caption, entities)
        val input = TdApi.InputFileLocal(file.absolutePath)
        val content = if (asPhoto) {
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
        send(TdApi.EditMessageMedia().apply {
            chatId = message.chatId
            messageId = message.id
            inputMessageContent = content
        })
    }

    private val stickerSets = KeyedState<String, TdApi.StickerSet>()
    private val stickerSetRequests = HashSet<String>()

    fun stickerFromSet(name: String, index: Int): TdApi.Sticker? {
        stickerSets[name]?.let { return it.stickers.getOrNull(index) }
        if (stickerSetRequests.add(name)) {
            send(TdApi.SearchStickerSet(name, false)) { result ->
                if (result is TdApi.StickerSet) stickerSets[name] = result
            }
        }
        return null
    }

    private val localDraftDates = mutableMapOf<Long, Int>()
    private val pendingDrafts = java.util.concurrent.ConcurrentHashMap<Long, String>()

    fun noteDraft(chatId: Long, text: String) {
        if (chats[chatId]?.draft == text) pendingDrafts.remove(chatId) else pendingDrafts[chatId] = text
    }

    fun flushDrafts() {
        pendingDrafts.keys.toList().forEach { chatId -> pendingDrafts.remove(chatId)?.let { saveDraft(chatId, it) } }
    }

    fun saveDraft(chatId: Long, text: String) {
        pendingDrafts.remove(chatId)
        val chat = chats[chatId] ?: return
        if (chat.draft == text) return
        chat.draft = text
        val now = (System.currentTimeMillis() / 1000).toInt()
        localDraftDates[chatId] = now
        val draft = text.takeIf { it.isNotBlank() }?.let {
            TdApi.DraftMessage().apply {
                date = now
                content = TdApi.DraftMessageContentText().apply { this.text = TdApi.FormattedText(it, emptyArray()) }
            }
        }
        send(TdApi.SetChatDraftMessage(chatId, null, draft))
    }

    fun loadKeyboard(chat: ChatState) {
        val id = chat.replyMarkupMessageId
        if (id == 0L || chat.keyboard != null) return
        send(TdApi.GetMessage(chat.id, id)) { result ->
            chat.keyboard = (result as? TdApi.Message)?.replyMarkup as? TdApi.ReplyMarkupShowKeyboard
        }
    }

    fun botInfo(userId: Long, onResult: (TdApi.BotInfo?) -> Unit) = userFullInfo(userId) { onResult(it?.botInfo) }

    fun chatCommands(chat: ChatState, onResult: (List<BotCommandItem>) -> Unit) {
        fun flatten(groups: Array<TdApi.BotCommands>?) = groups.orEmpty().flatMap { bot ->
            ensureUser(bot.botUserId)
            bot.commands.orEmpty().map { BotCommandItem(bot.botUserId, it.command, it.description) }
        }
        when {
            chat.privateUserId != 0L -> botInfo(chat.privateUserId) { info ->
                onResult(info?.commands.orEmpty().map { BotCommandItem(chat.privateUserId, it.command, it.description) })
            }
            chat.supergroupId != 0L -> send(TdApi.GetSupergroupFullInfo(chat.supergroupId)) { result ->
                onResult(flatten((result as? TdApi.SupergroupFullInfo)?.botCommands))
            }
            chat.basicGroupId != 0L -> send(TdApi.GetBasicGroupFullInfo(chat.basicGroupId)) { result ->
                onResult(flatten((result as? TdApi.BasicGroupFullInfo)?.botCommands))
            }
            else -> onResult(emptyList())
        }
    }

    val myProfileAudios = mutableStateListOf<Int>()

    fun profileAudios(userId: Long, onResult: (List<TdApi.Audio>) -> Unit) {
        if (System.getProperty("pixelgram.demo") == "true") return onResult(demoProfileAudios(userId))
        send(TdApi.GetUserProfileAudios(userId, 0, ProfileAudiosLimit)) { result ->
            onResult((result as? TdApi.Audios)?.audios?.toList().orEmpty())
        }
    }

    fun loadMyProfileAudios() {
        val userId = myUserId.takeIf { it != 0L } ?: me?.id ?: return
        profileAudios(userId) { list ->
            myProfileAudios.clear()
            myProfileAudios.addAll(list.map { it.audio.id })
        }
    }

    fun isInProfile(audio: TdApi.Audio?): Boolean = audio != null && audio.audio.id in myProfileAudios

    fun toggleProfileAudio(audio: TdApi.Audio, done: (String?) -> Unit = {}) {
        val request: TdApi.Function<TdApi.Ok> = if (isInProfile(audio)) {
            TdApi.RemoveProfileAudio(audio.audio.id)
        } else {
            TdApi.AddProfileAudio(TdApi.InputAudio().apply {
                this.audio = remoteInput(audio.audio)
                duration = audio.duration
                title = audio.title
                performer = audio.performer
            })
        }
        send(request) { result ->
            done((result as? TdApi.Error)?.message?.let(::friendlyError))
            loadMyProfileAudios()
        }
    }

    val pinnedMessages = mutableStateMapOf<Long, List<UiMessage>>()

    private val customEmojiStickers = KeyedState<Long, TdApi.Sticker>()
    private val customEmojiQueue = LinkedHashSet<Long>()
    private val customEmojiRequested = HashSet<Long>()

    fun customEmoji(id: Long): TdApi.Sticker? {
        customEmojiStickers[id]?.let { return it }
        if (customEmojiRequested.add(id)) {
            val first = customEmojiQueue.isEmpty()
            customEmojiQueue.add(id)
            if (first) scope.launch(Dispatchers.Main) {
                delay(CustomEmojiBatchMs)
                val ids = customEmojiQueue.toLongArray()
                customEmojiQueue.clear()
                ids.toList().chunked(CustomEmojiChunk).forEach { chunk ->
                    send(TdApi.GetCustomEmojiStickers(chunk.toLongArray())) { result ->
                        if (result is TdApi.Stickers) {
                            result.stickers.forEach { sticker ->
                                (sticker.fullType as? TdApi.StickerFullTypeCustomEmoji)?.let { customEmojiStickers[it.customEmojiId] = sticker }
                            }
                        } else {
                            customEmojiRequested.removeAll(chunk.toSet())
                        }
                    }
                }
            }
        }
        return null
    }

    fun loadPinned(chatId: Long) {
        if (System.getProperty("pixelgram.demo") == "true") {
            pinnedMessages[chatId] = messages(chatId).filter { it.pinned }
            return
        }
        send(TdApi.SearchChatMessages().apply {
            this.chatId = chatId
            query = ""
            limit = PinnedLimit
            filter = TdApi.SearchMessagesFilterPinned()
        }) { result ->
            if (result is TdApi.FoundChatMessages) pinnedMessages[chatId] = result.messages.map { it.toUi() }
        }
    }

    fun togglePin(message: UiMessage, notify: Boolean = true, onlyForSelf: Boolean = false) {
        if (System.getProperty("pixelgram.demo") == "true") {
            val list = messages(message.chatId)
            val index = list.indexOfFirst { it.id == message.id }
            if (index >= 0) list[index] = list[index].copy(pinned = !message.pinned)
            return loadPinned(message.chatId)
        }
        val request: TdApi.Function<TdApi.Ok> = if (message.pinned) {
            TdApi.UnpinChatMessage(message.chatId, message.id)
        } else {
            TdApi.PinChatMessage(message.chatId, message.id, !notify, onlyForSelf)
        }
        send(request) { result ->
            if (result is TdApi.Error) AppLog.write("Pin failed ${result.code}: ${result.message}")
        }
    }

    fun stickerSet(setId: Long, onResult: (TdApi.StickerSet?) -> Unit) {
        send(TdApi.GetStickerSet(setId)) { result -> onResult(result as? TdApi.StickerSet) }
    }

    fun setStickerSetInstalled(setId: Long, installed: Boolean, done: () -> Unit) {
        send(TdApi.ChangeStickerSet(setId, installed, false)) { done() }
    }

    fun userTitle(userId: Long): String = if (userId == 0L) "Anonymous" else senderTitle(TdApi.MessageSenderUser(userId))

    fun senderTitle(sender: TdApi.MessageSender?): String = when (sender) {
        is TdApi.MessageSenderUser -> if (sender.userId == myUserId) "You" else userName(sender.userId)
        is TdApi.MessageSenderChat -> chats[sender.chatId]?.title ?: ""
        else -> ""
    }

    fun photoPath(message: UiMessage): String? = localPath(message.fullPhoto) ?: localPath(message.photo)

    fun searchChats(query: String, onResult: (List<Long>) -> Unit) {
        val local = chats.values
            .filter { chatTitle(it).contains(query, ignoreCase = true) }
            .sortedByDescending { it.lastMessage?.date ?: 0 }
            .map { it.id }
            .take(ChatSearchLimit)
        onResult(local)
        send(TdApi.SearchChats(query, null, ChatSearchLimit)) { result ->
            val found = (result as? TdApi.Chats)?.chatIds?.toList().orEmpty()
            val mine = (local + found).distinct().filter { chats[it] != null }.take(ChatSearchLimit)
            onResult(mine)
            val username = query.removePrefix("@").trim()
            if (username.length < MinPublicSearch) return@send
            send(TdApi.SearchPublicChats().apply { this.query = username }) { public ->
                val global = (public as? TdApi.Chats)?.chatIds?.toList().orEmpty()
                onResult((mine + global).distinct().filter { chats[it] != null })
            }
        }
    }

    fun searchMessages(query: String, chatId: Long?, onResult: (Int, List<TdApi.Message>) -> Unit) {
        val function: TdApi.Function<*> = if (chatId != null) {
            TdApi.SearchChatMessages().apply {
                this.chatId = chatId
                this.query = query
                limit = SearchLimit
            }
        } else {
            TdApi.SearchMessages().apply {
                this.query = query
                offset = ""
                limit = SearchLimit
            }
        }
        send(function) { result ->
            when (result) {
                is TdApi.FoundChatMessages -> onResult(result.totalCount, result.messages.toList())
                is TdApi.FoundMessages -> onResult(result.totalCount, result.messages.toList())
                else -> onResult(0, emptyList())
            }
        }
    }

    fun forwardMessages(toChatId: Long, fromChatId: Long, ids: LongArray, sendCopy: Boolean = false, removeCaption: Boolean = false) {
        send(TdApi.ForwardMessages().apply {
            chatId = toChatId
            this.fromChatId = fromChatId
            messageIds = ids
            this.sendCopy = sendCopy
            this.removeCaption = removeCaption
        })
    }

    fun forwardName(origin: TdApi.MessageOrigin): String = when (origin) {
        is TdApi.MessageOriginUser -> userName(origin.senderUserId)
        is TdApi.MessageOriginChat -> chats[origin.senderChatId]?.title.orEmpty()
        is TdApi.MessageOriginHiddenUser -> origin.senderName
        is TdApi.MessageOriginChannel -> chats[origin.chatId]?.title.orEmpty()
        else -> ""
    }

    private fun withUsers(ids: List<Long>, done: (List<Long>) -> Unit) {
        val missing = ids.filter { users[it] == null }
        if (missing.isEmpty()) return done(ids)
        var left = missing.size
        missing.forEach { id ->
            send(TdApi.GetUser(id)) { result ->
                if (result is TdApi.User) users[id] = result
                left--
                if (left == 0) done(ids)
            }
        }
    }

    private fun TdApi.User.matches(query: String): Boolean =
        query.isEmpty() || firstName.contains(query, true) || lastName.contains(query, true) ||
            usernames?.activeUsernames?.any { it.contains(query, true) } == true

    fun mentionSuggestions(chatId: Long, inGroup: Boolean, query: String, onResult: (List<Long>) -> Unit) {
        val found = LinkedHashSet<Long>()
        val resolve = query.length >= MentionResolveMin
        var pending = 1 + (if (inGroup) 1 else 0) + (if (resolve) 1 else 0)
        val done = {
            pending--
            if (pending == 0) onResult(found.filter { it != myUserId }.take(MentionLimit))
        }
        if (inGroup) {
            send(TdApi.SearchChatMembers(chatId, query, MentionLimit, null)) { result ->
                val ids = (result as? TdApi.ChatMembers)?.members?.mapNotNull { (it.memberId as? TdApi.MessageSenderUser)?.userId }.orEmpty()
                withUsers(ids) {
                    found.addAll(it)
                    done()
                }
            }
        }
        send(TdApi.GetRecentInlineBots()) { result ->
            val ids = (result as? TdApi.Users)?.userIds?.toList().orEmpty()
            withUsers(ids) { loaded ->
                found.addAll(loaded.filter { users[it]?.matches(query) == true })
                done()
            }
        }
        if (resolve) {
            send(TdApi.SearchPublicChat(query)) { result ->
                val userId = ((result as? TdApi.Chat)?.type as? TdApi.ChatTypePrivate)?.userId
                if (userId == null) done() else withUsers(listOf(userId)) {
                    found.add(userId)
                    done()
                }
            }
        }
    }

    fun installedStickerSets(onResult: (List<TdApi.StickerSetInfo>) -> Unit) {
        send(TdApi.GetInstalledStickerSets(TdApi.StickerTypeRegular())) { result ->
            onResult((result as? TdApi.StickerSets)?.sets?.toList().orEmpty())
        }
    }

    fun customEmojiSets(onResult: (List<TdApi.StickerSetInfo>) -> Unit) {
        send(TdApi.GetInstalledStickerSets(TdApi.StickerTypeCustomEmoji())) { result ->
            onResult((result as? TdApi.StickerSets)?.sets?.toList().orEmpty())
        }
    }

    fun searchCustomEmoji(query: String, done: (List<TdApi.Sticker>) -> Unit) {
        val emoji = com.pixelgram.desktop.ui.theme.emojiRanges(query).sumOf { it.last - it.first + 1 } == query.length
        send(TdApi.GetStickers(TdApi.StickerTypeCustomEmoji(), query, StickerSearchLimit, 0)) { result ->
            val installed = (result as? TdApi.Stickers)?.stickers?.toList().orEmpty()
            val search = if (emoji) TdApi.SearchStickers(TdApi.StickerTypeCustomEmoji(), query, "", searchLanguages, 0, StickerSearchLimit)
            else TdApi.SearchStickers(TdApi.StickerTypeCustomEmoji(), "", query, searchLanguages, 0, StickerSearchLimit)
            send(search) { more ->
                val extra = (more as? TdApi.Stickers)?.stickers.orEmpty().filter { found -> installed.none { it.sticker.id == found.sticker.id } }
                done(installed + extra)
            }
        }
    }

    fun recentStickers(onResult: (List<TdApi.Sticker>) -> Unit) {
        send(TdApi.GetRecentStickers(false)) { result -> onResult((result as? TdApi.Stickers)?.stickers?.toList().orEmpty()) }
    }

    fun favoriteStickers(onResult: (List<TdApi.Sticker>) -> Unit) {
        send(TdApi.GetFavoriteStickers()) { result -> onResult((result as? TdApi.Stickers)?.stickers?.toList().orEmpty()) }
    }

    fun setStickerFavorite(sticker: TdApi.Sticker, favorite: Boolean, done: () -> Unit) {
        val input = remoteInput(sticker.sticker)
        send(if (favorite) TdApi.AddFavoriteSticker(input) else TdApi.RemoveFavoriteSticker(input)) { done() }
    }

    fun savedAnimations(onResult: (List<TdApi.Animation>) -> Unit) {
        send(TdApi.GetSavedAnimations()) { result -> onResult((result as? TdApi.Animations)?.animations?.toList().orEmpty()) }
    }

    private fun remoteInput(file: TdApi.File): TdApi.InputFile =
        file.remote?.id?.takeIf { it.isNotEmpty() }?.let { TdApi.InputFileRemote(it) } ?: TdApi.InputFileId(file.id)

    fun removeRecentSticker(sticker: TdApi.Sticker, done: () -> Unit) {
        send(TdApi.RemoveRecentSticker(false, remoteInput(sticker.sticker))) { done() }
    }

    fun sendSticker(chatId: Long, sticker: TdApi.Sticker, replyToId: Long = 0, silent: Boolean = false, schedule: TdApi.MessageSchedulingState? = null) {
        send(TdApi.SendMessage().apply {
            this.chatId = chatId
            replyTo = replyInput(replyToId)
            options = TdApi.MessageSendOptions().apply {
                disableNotification = silent
                schedulingState = schedule
            }
            inputMessageContent = TdApi.InputMessageSticker().apply {
                this.sticker = TdApi.InputSticker().apply {
                    this.sticker = remoteInput(sticker.sticker)
                    width = sticker.width
                    height = sticker.height
                }
                emoji = sticker.emoji
            }
        })
    }

    fun sendVoice(chatId: Long, voice: com.pixelgram.desktop.core.VoiceNote, replyToId: Long = 0) {
        send(TdApi.SendMessage().apply {
            this.chatId = chatId
            replyTo = replyInput(replyToId)
            inputMessageContent = TdApi.InputMessageVoiceNote().apply {
                voiceNote = TdApi.InputVoiceNote(TdApi.InputFileLocal(voice.file.absolutePath), voice.duration, voice.waveform)
                caption = TdApi.FormattedText("", arrayOf())
            }
        })
    }

    fun sendVideoNote(chatId: Long, note: com.pixelgram.desktop.core.VideoNote, replyToId: Long = 0) {
        send(TdApi.SendMessage().apply {
            this.chatId = chatId
            replyTo = replyInput(replyToId)
            inputMessageContent = TdApi.InputMessageVideoNote().apply {
                videoNote = TdApi.InputVideoNote().apply {
                    videoNote = TdApi.InputFileLocal(note.file.absolutePath)
                    thumbnail = note.thumbnail?.let { TdApi.InputThumbnail(TdApi.InputFileLocal(it.absolutePath), ThumbnailSide, ThumbnailSide) }
                    duration = note.duration
                    length = note.length
                }
            }
        }) { result -> if (result is TdApi.Error) AppLog.write("Video note failed ${result.code}: ${result.message}") }
    }

    fun sendAnimation(chatId: Long, animation: TdApi.Animation, replyToId: Long = 0) {
        send(TdApi.SendMessage().apply {
            this.chatId = chatId
            replyTo = replyInput(replyToId)
            inputMessageContent = TdApi.InputMessageAnimation().apply {
                this.animation = TdApi.InputAnimation().apply {
                    this.animation = remoteInput(animation.animation)
                    addedStickerFileIds = IntArray(0)
                    duration = animation.duration
                    width = animation.width
                    height = animation.height
                }
            }
        })
    }

    fun inlineBot(username: String, onResult: (TdApi.User?) -> Unit) {
        send(TdApi.SearchPublicChat(username)) { result ->
            val userId = ((result as? TdApi.Chat)?.type as? TdApi.ChatTypePrivate)?.userId
            if (userId == null) onResult(null) else withUsers(listOf(userId)) {
                onResult(users[userId]?.takeIf { (it.type as? TdApi.UserTypeBot)?.isInline == true })
            }
        }
    }

    fun inlineResults(botId: Long, chatId: Long, query: String, onResult: (TdApi.InlineQueryResults?) -> Unit) {
        send(TdApi.GetInlineQueryResults(botId, chatId, null, query, "")) { result ->
            onResult(result as? TdApi.InlineQueryResults ?: TdApi.InlineQueryResults().apply { results = emptyArray() })
        }
    }

    fun sendInlineResult(chatId: Long, queryId: Long, resultId: String, replyToId: Long = 0) {
        send(TdApi.SendInlineQueryResultMessage(chatId, null, replyInput(replyToId), null, queryId, resultId, false)) { result ->
            if (result is TdApi.Error) AppLog.write("Inline send failed ${result.code}: ${result.message}")
        }
    }

    fun toggleReaction(message: UiMessage, emoji: String) {
        val clean = emoji.replace("\uFE0F", "")
        val type = TdApi.ReactionTypeEmoji(clean)
        val list = messages(message.chatId)
        val current = list.firstOrNull { it.id == message.id } ?: message
        val before = current.reactions
        val chosen = before.any { it.emoji() == clean && it.isChosen }
        val after = before.mapNotNull { reaction ->
            when {
                reaction.emoji() == clean && chosen -> reaction.withCount(-1, false)
                reaction.isChosen && !chosen -> reaction.withCount(-1, false)
                reaction.emoji() == clean -> reaction.withCount(1, true)
                else -> reaction
            }
        }.let { updated ->
            if (chosen || updated.any { it.emoji() == clean }) updated
            else updated + TdApi.MessageReaction().apply {
                this.type = type
                totalCount = 1
                isChosen = true
                recentSenderIds = arrayOf(TdApi.MessageSenderUser(myUserId))
            }
        }
        val replace = { reactions: List<TdApi.MessageReaction> ->
            val index = list.indexOfFirst { it.id == message.id }
            if (index >= 0) list[index] = list[index].copy(reactions = reactions)
        }
        replace(after)
        val big = com.pixelgram.desktop.core.PowerSaving.settings?.bigReactions?.value == true
        if (!chosen && com.pixelgram.desktop.core.PowerSaving.stickersInChat) ReactionEffects.play(message.chatId to message.id, clean, big)
        send(
            if (chosen) TdApi.RemoveMessageReaction(message.chatId, message.id, type)
            else TdApi.AddMessageReaction(message.chatId, message.id, type, big, true)
        ) { result ->
            if (result is TdApi.Error) {
                AppLog.write("Reaction $clean failed ${result.code}: ${result.message}")
                scope.launch(Dispatchers.Main) { replace(before) }
            }
        }
    }

    private fun TdApi.MessageReaction.emoji() = (type as? TdApi.ReactionTypeEmoji)?.emoji?.replace("\uFE0F", "")

    private fun TdApi.MessageReaction.withCount(delta: Int, chosen: Boolean): TdApi.MessageReaction? {
        val count = totalCount + delta
        if (count <= 0) return null
        val source = this
        return TdApi.MessageReaction().apply {
            type = source.type
            totalCount = count
            isChosen = chosen
            usedSenderId = source.usedSenderId
            recentSenderIds = source.recentSenderIds
        }
    }

    fun availableReactions(message: UiMessage, onResult: (List<String>) -> Unit) {
        send(TdApi.GetMessageAvailableReactions(message.chatId, message.id, ReactionRow)) { result ->
            if (result is TdApi.AvailableReactions) {
                onResult(
                    (result.topReactions + result.recentReactions + result.popularReactions)
                        .mapNotNull { (it.type as? TdApi.ReactionTypeEmoji)?.emoji }
                        .distinct()
                )
            }
        }
    }

    fun pressButton(message: UiMessage, button: TdApi.InlineKeyboardButton, onAnswer: (String, Boolean) -> Unit) {
        when (val type = button.type) {
            is TdApi.InlineKeyboardButtonTypeCallback ->
                send(TdApi.GetCallbackQueryAnswer(message.chatId, message.id, TdApi.CallbackQueryPayloadData(type.data))) { result ->
                    when (result) {
                        is TdApi.CallbackQueryAnswer -> {
                            if (result.text.isNotBlank()) onAnswer(result.text, result.showAlert)
                            if (result.url.isNotBlank()) com.pixelgram.desktop.ui.main.openUrl(result.url)
                        }
                        is TdApi.Error -> if (result.code != 502) onAnswer(result.message, false)
                    }
                }
            is TdApi.InlineKeyboardButtonTypeUrl -> com.pixelgram.desktop.ui.main.Links.confirmOpen(type.url)
            is TdApi.InlineKeyboardButtonTypeLoginUrl -> com.pixelgram.desktop.ui.main.Links.confirmOpen(type.url)
            is TdApi.InlineKeyboardButtonTypeCopyText -> {
                java.awt.Toolkit.getDefaultToolkit().systemClipboard.setContents(java.awt.datatransfer.StringSelection(type.text), null)
                onAnswer("Copied", false)
            }
            is TdApi.InlineKeyboardButtonTypeUser -> openPrivateChat(type.userId) { }
            else -> onAnswer("This button isn't supported yet", false)
        }
    }

    fun botName(userId: Long): String {
        ensureUser(userId)
        return users[userId]?.usernames?.activeUsernames?.firstOrNull()?.let { "@$it" } ?: userName(userId)
    }

    fun votePoll(chatId: Long, messageId: Long, options: IntArray) {
        send(TdApi.SetPollAnswer().apply {
            this.chatId = chatId
            this.messageId = messageId
            optionIds = options
        })
    }

    fun editMessage(message: UiMessage, text: String, entities: Array<TdApi.TextEntity> = emptyArray()) {
        val formatted = TdApi.FormattedText(text, entities)
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

    private val replyCache = KeyedState<Pair<Long, Long>, UiMessage>()
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

    fun sendFiles(
        chatId: Long,
        files: List<File>,
        asPhotos: Boolean,
        caption: String,
        replyToId: Long = 0,
        entities: Array<TdApi.TextEntity> = emptyArray(),
    ) {
        if (System.getProperty("pixelgram.demo") == "true") return demoSendFiles(chatId, files, caption)
        scope.launch(Dispatchers.IO) {
            files.chunked(AlbumSize).forEachIndexed { chunkIndex, chunk ->
                val contents = chunk.mapIndexed { index, file ->
                    val first = chunkIndex == 0 && index == 0
                    val text = TdApi.FormattedText(if (first) caption else "", if (first) entities else arrayOf())
                    val input = TdApi.InputFileLocal(file.absolutePath)
                    if (asPhotos && com.pixelgram.desktop.ui.main.Attachments.isVideo(file)) {
                        val info = com.pixelgram.desktop.ui.main.Attachments.videoInfo(file)
                        TdApi.InputMessageVideo().apply {
                            video = TdApi.InputVideo().apply {
                                video = input
                                thumbnail = info.poster?.let { poster ->
                                    val (w, h) = imageSize(poster)
                                    TdApi.InputThumbnail(TdApi.InputFileLocal(poster.absolutePath), w, h)
                                }
                                addedStickerFileIds = IntArray(0)
                                duration = info.duration
                                width = info.width
                                height = info.height
                                supportsStreaming = file.extension.lowercase() in StreamingExtensions
                            }
                            this.caption = text
                        }
                    } else if (asPhotos) {
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
        text = messageText(content),
        photo = photo?.photo,
        photoWidth = photo?.width ?: 0,
        photoHeight = photo?.height ?: 0,
        fullPhoto = (content as? TdApi.MessagePhoto)?.photo?.sizes?.maxByOrNull { it.width }?.photo,
        sending = sendingState is TdApi.MessageSendingStatePending,
        failed = sendingState is TdApi.MessageSendingStateFailed,
        edited = editDate > 0,
        service = isService(content),
        editable = isOutgoing && (content is TdApi.MessageText || content is TdApi.MessagePhoto),
        content = content,
        entities = messageEntities(content),
        forwardOrigin = forwardInfo?.origin,
        reactions = interactionInfo?.reactions?.reactions?.toList().orEmpty(),
        keyboard = inlineRows(replyMarkup),
        viaBot = viaBotUserId,
        views = interactionInfo?.viewCount ?: 0,
        comments = interactionInfo?.replyInfo?.replyCount ?: -1,
        pinned = isPinned,
        albumId = mediaAlbumId,
        signature = authorSignature.orEmpty(),
        captionAbove = when (val media = content) {
            is TdApi.MessagePhoto -> media.showCaptionAboveMedia
            is TdApi.MessageVideo -> media.showCaptionAboveMedia
            is TdApi.MessageAnimation -> media.showCaptionAboveMedia
            else -> false
        },
        replyToId = (replyTo as? TdApi.MessageReplyToMessage)?.takeIf { it.chatId == 0L || it.chatId == chatId }?.messageId ?: 0L,
    )
}

fun draftText(draft: TdApi.DraftMessage?): String =
    (draft?.content as? TdApi.DraftMessageContentText)?.text?.text.orEmpty()

fun isRich(content: TdApi.MessageContent?): Boolean = when (content) {
    is TdApi.MessageDocument, is TdApi.MessageAudio, is TdApi.MessageVoiceNote, is TdApi.MessageCall,
    is TdApi.MessageSticker, is TdApi.MessageGift, is TdApi.MessageUpgradedGift, is TdApi.MessageGiftedStars,
    is TdApi.MessageGiftedPremium, is TdApi.MessageAnimation, is TdApi.MessageVideo, is TdApi.MessageVideoNote,
    is TdApi.MessagePoll, is TdApi.MessageContact, is TdApi.MessageLocation, is TdApi.MessageAnimatedEmoji,
    is TdApi.MessageDice -> true
    else -> false
}

fun inlineRows(markup: TdApi.ReplyMarkup?): List<List<TdApi.InlineKeyboardButton>> =
    (markup as? TdApi.ReplyMarkupInlineKeyboard)?.rows?.map { it.toList() }.orEmpty()

fun messageEntities(content: TdApi.MessageContent?): List<TdApi.TextEntity> {
    val formatted = when (content) {
        is TdApi.MessageText -> content.text
        is TdApi.MessagePhoto -> content.caption
        is TdApi.MessageDocument -> content.caption
        is TdApi.MessageAudio -> content.caption
        is TdApi.MessageVoiceNote -> content.caption
        is TdApi.MessageAnimation -> content.caption
        is TdApi.MessageVideo -> content.caption
        else -> null
    }
    return formatted?.entities?.toList().orEmpty()
}

fun messageText(content: TdApi.MessageContent?): String = when (content) {
    is TdApi.MessagePhoto -> content.caption?.text.orEmpty()
    is TdApi.MessageDocument -> content.caption?.text.orEmpty()
    is TdApi.MessageAudio -> content.caption?.text.orEmpty()
    is TdApi.MessageVoiceNote -> content.caption?.text.orEmpty()
    is TdApi.MessageAnimation -> content.caption?.text.orEmpty()
    is TdApi.MessageVideo -> content.caption?.text.orEmpty()
    is TdApi.MessageGift -> content.text?.text.orEmpty()
    is TdApi.MessageUpgradedGift -> content.text?.text.orEmpty()
    is TdApi.MessageGiftedPremium -> content.text?.text.orEmpty()
    else -> if (isRich(content)) "" else contentText(content)
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

private fun mediaPreview(emoji: String, label: String, caption: String?): String =
    if (caption.isNullOrBlank()) "$emoji $label" else "$emoji $caption"

fun contentText(content: TdApi.MessageContent?): String = when (content) {
    is TdApi.MessageText -> content.text?.text ?: ""
    is TdApi.MessagePhoto -> mediaPreview("🖼", "Photo", content.caption?.text)
    is TdApi.MessageVideo -> mediaPreview("🎬", "Video", content.caption?.text)
    is TdApi.MessageDocument -> listOf("📄 ${content.document?.fileName ?: "File"}", content.caption?.text).filterNotBlank()
    is TdApi.MessageAudio -> "🎵 ${content.audio?.title ?: "Audio"}"
    is TdApi.MessageVoiceNote -> "🎤 Voice message"
    is TdApi.MessageVideoNote -> "⏺ Video message"
    is TdApi.MessageSticker -> "${content.sticker?.emoji ?: ""} Sticker".trim()
    is TdApi.MessageAnimation -> mediaPreview("🎞", "GIF", content.caption?.text)
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
    is TdApi.MessageGift -> "🎁 Gift"
    is TdApi.MessageUpgradedGift -> "🎁 ${content.gift.title}"
    is TdApi.MessageGiftedStars -> "⭐ ${content.starCount} Stars"
    is TdApi.MessageGiftedPremium -> "⭐ Telegram Premium"
    is TdApi.MessageAnimatedEmoji -> content.emoji
    is TdApi.MessageDice -> content.emoji
    null -> ""
    else -> content.javaClass.simpleName.removePrefix("Message")
        .replace(Regex("(?<=[a-z])(?=[A-Z])"), " ").lowercase().replaceFirstChar { it.uppercase() }
}

private fun List<String?>.filterNotBlank(): String =
    filterNot { it.isNullOrBlank() }.joinToString("\n")

private fun floodSeconds(message: String?): Long? =
    Regex("""(?:retry after |FLOOD_WAIT_)(\d+)""").find(message.orEmpty())?.groupValues?.get(1)?.toLongOrNull()

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
