package com.pixelgram.desktop.core

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.pixelgram.desktop.ui.AvatarShape
import java.util.Properties

enum class ThemeMode { System, Light, Dark }

enum class EmojiStyle { Noto, Noto3D, Apple }

enum class ToastSide { Left, Right, Top, Bottom }

enum class NotificationStyle { System, PixelGram }

enum class DoubleClickAction { Nothing, Reply, Reaction }

enum class AvatarKind(val title: String) {
    Private("Private chats"),
    Bot("Bots"),
    Group("Groups"),
    Forum("Forums"),
    Channel("Channels"),
}

class Setting<T>(
    private val owner: AppSettings,
    val key: String,
    initial: T,
    val default: T = initial,
) {
    private val state: MutableState<T> = mutableStateOf(initial)

    fun reset() {
        value = default
    }

    var value: T
        get() = state.value
        set(value) {
            if (state.value != value) {
                state.value = value
                owner.save()
            }
        }

    @Suppress("UNCHECKED_CAST")
    fun load(raw: String) {
        val parsed: Any? = when (val current = value) {
            is Boolean -> raw.toBooleanStrictOrNull()
            is Int -> raw.toIntOrNull()
            is String -> raw
            is Enum<*> -> current.declaringJavaClass.enumConstants.firstOrNull { it.name == raw }
            else -> null
        }
        if (parsed != null) value = parsed as T
    }
}

class AppSettings private constructor(private val props: Properties) {
    private fun string(key: String, default: String) =
        Setting(this, key, props.getProperty(key) ?: default, default)

    private fun int(key: String, default: Int) =
        Setting(this, key, props.getProperty(key)?.toIntOrNull() ?: default, default)

    private fun bool(key: String, default: Boolean) =
        Setting(this, key, props.getProperty(key)?.toBooleanStrictOrNull() ?: default, default)

    private inline fun <reified E : Enum<E>> enum(key: String, default: E) =
        Setting(this, key, props.getProperty(key)?.let { name ->
            enumValues<E>().firstOrNull { it.name == name }
        } ?: default, default)

    val themeMode = enum("themeMode", ThemeMode.System)
    val seedColor = int("seedColor", 0xFF6750A4.toInt())
    val colorStyle = enum("colorStyle", com.pixelgram.desktop.ui.theme.ColorStyle.TonalSpot)
    val matugen = bool("matugen", false)
    val emojiStyle = enum("emojiStyle", EmojiStyle.Noto)
    val notificationSound = bool("notificationSound", true)
    val notificationsMuted = bool("notificationsMuted", false)
    val notificationPopups = bool("notificationPopups", true)
    val notificationSide = enum("notificationSide", ToastSide.Right)
    val notificationStyle = enum("notificationStyle", NotificationStyle.System)

    val materialFolders = bool("materialFolders", true)
    val hideAllChats = bool("hideAllChats", false)
    val capsuleFolders = bool("capsuleFolders", false)
    val folderOrder = string("folderOrder", "")
    val folderIcons = string("folderIcons", "")
    val unreadFolder = bool("unreadFolder", false)
    val unreadFolderName = string("unreadFolderName", "Unread")
    val unreadFolderIcon = string("unreadFolderIcon", "Unread")
    val mentionsFolder = bool("mentionsFolder", false)
    val mentionsFolderName = string("mentionsFolderName", "Mentions")
    val mentionsFolderIcon = string("mentionsFolderIcon", "Mention")
    val chatListWidth = int("chatListWidth", 360)
    val customTitleBar = bool("customTitleBar", !AppPaths.isLinux)
    val messageSeconds = bool("messageSeconds", false)
    val hidePhone = bool("hidePhone", false)
    val checkUpdates = bool("checkUpdates", true)
    val doubleClickOwn = enum("doubleClickOwn", DoubleClickAction.Reaction)
    val doubleClickOther = enum("doubleClickOther", DoubleClickAction.Reaction)
    val doubleClickEmoji = string("doubleClickEmoji", "❤")
    val capsuleSpacing = int("capsuleSpacing", 4)
    val capsuleHeight = int("capsuleContainerHeight", 64)

    val avatarShape = enum("avatarShape", AvatarShape.Circle)
    val avatarCorner = int("avatarCorner", 50)
    val avatarKindShapes = AvatarKind.entries.associateWith { string("avatarShape${it.name}", "") }

    fun kindShape(kind: AvatarKind): AvatarShape? =
        avatarKindShapes[kind]?.value?.let { name -> AvatarShape.entries.firstOrNull { it.name == name } }

    fun avatarShapeFor(kind: AvatarKind?): AvatarShape = kind?.let(::kindShape) ?: avatarShape.value

    val deviceName = string("deviceName", "")
    val israelLogo = bool("israelLogo", false)
    val outputDevice = string("outputDevice", "")
    val inputDevice = string("inputDevice", "")
    val cameraDevice = string("cameraDevice", "")
    val callsSameDevices = bool("callsSameDevices", true)
    val acceptCalls = bool("acceptCalls", true)
    val passcodeHash = string("passcodeHash", "")
    val passcodeSalt = string("passcodeSalt", "")
    val autoLockMinutes = int("autoLockMinutes", 0)

    val stickersInPanel = bool("stickersInPanel", true)
    val stickersInChat = bool("stickersInChat", true)
    val emojiInMessages = bool("emojiInMessages", true)
    val spoilerEffect = bool("spoilerEffect", true)
    val messageAnimations = bool("messageAnimations", true)
    val interfaceAnimations = bool("interfaceAnimations", true)
    val language = string("language", "en")
    val bigReactions = bool("bigReactions", false)
    val localNames = string("localNames", "")
    val liquidCursor = bool("liquidCursor", true)
    val cursorStretch = int("cursorStretch", 40)
    val handleStretch = int("handleStretch", 60)
    val sidePull = int("sidePull", 45)
    val smoothCursor = bool("smoothCursor", true)
    val cursorSpeed = int("cursorSpeed", 25)
    val cursorWidth = int("cursorWidth", 2)
    val cursorBlink = bool("cursorBlink", true)
    val blinkCycle = int("blinkCycle", 1000)
    val blinkSmoothness = int("blinkSmoothness", 100)
    val titleChatName = bool("titleChatName", true)
    val titleAccount = bool("titleAccount", true)
    val titleUnread = bool("titleUnread", true)
    val trayIcon = bool("trayIcon", true)
    val trayMonochrome = bool("trayMonochrome", false)
    val autostart = bool("autostart", false)
    val launchMinimized = bool("launchMinimized", false)
    val restoreWindow = bool("restoreWindow", true)
    val emptyShapes = bool("emptyShapes", true)
    val windowBounds = string("windowBounds", "")
    val autoplayGifs = bool("autoplayGifs", true)
    val autoplayVideos = bool("autoplayVideos", true)

    val bubbleRadius = int("bubbleRadius", 18)
    val bubbleTailRadius = int("bubbleTailRadius", 4)

    val messageFont = string("messageFont", "")
    val messageFontSize = int("messageFontSize", 16)
    val fontEverywhere = bool("fontEverywhere", true)
    val wallpaper = string("wallpaper", "")
    val wallpaperDim = int("wallpaperDim", 20)
    val wallpaperColors = bool("wallpaperColors", true)

    val cornerLists = int("cornerLists", 20)
    val cornerCards = int("cornerCards", 24)
    val cornerDialogs = int("cornerDialogs", 28)
    val cornerMenus = int("cornerMenus", 16)

    val apiId = int("apiId", 0)
    val apiHash = string("apiHash", "")

    val accounts = string("accounts", "0")
    val activeAccount = int("activeAccount", 0)

    private val all: List<Setting<*>> = listOf(
        themeMode, seedColor, colorStyle, matugen, emojiStyle, notificationSound, notificationsMuted,
        notificationPopups, notificationSide, notificationStyle,
        materialFolders, hideAllChats, folderOrder, folderIcons,
        unreadFolder, unreadFolderName, unreadFolderIcon, mentionsFolder, mentionsFolderName, mentionsFolderIcon,
        capsuleFolders, capsuleSpacing, capsuleHeight, chatListWidth, customTitleBar, messageSeconds, hidePhone, checkUpdates,
        doubleClickOwn, doubleClickOther, doubleClickEmoji,
        avatarShape, avatarCorner,
        bubbleRadius, bubbleTailRadius, cornerLists, cornerCards, cornerDialogs, cornerMenus,
        wallpaper, wallpaperDim, wallpaperColors, messageFont, messageFontSize, fontEverywhere,
        deviceName, israelLogo, outputDevice, inputDevice, cameraDevice, callsSameDevices, acceptCalls, passcodeHash, passcodeSalt, autoLockMinutes,
        stickersInPanel, stickersInChat, emojiInMessages, spoilerEffect, messageAnimations, interfaceAnimations, autoplayGifs, autoplayVideos, titleChatName, titleAccount, titleUnread, trayIcon, trayMonochrome, restoreWindow, windowBounds, emptyShapes, autostart, launchMinimized, language, localNames, bigReactions, liquidCursor, cursorStretch, handleStretch, sidePull, smoothCursor, cursorSpeed, cursorWidth, cursorBlink, blinkCycle, blinkSmoothness,
        apiId, apiHash,
        accounts, activeAccount,
    ) + avatarKindShapes.values

    private val localOnly = setOf(apiId, apiHash, accounts, activeAccount, outputDevice, inputDevice, cameraDevice, windowBounds, autostart, launchMinimized)

    fun resetAll() {
        val keep = localOnly + setOf(passcodeHash, passcodeSalt, autoLockMinutes, deviceName)
        all.filter { it !in keep }.forEach { it.reset() }
    }

    fun snapshot(): Properties = Properties().apply {
        for (setting in all) {
            if (setting in localOnly) continue
            val value = setting.value
            setProperty(setting.key, if (value is Enum<*>) value.name else value.toString())
        }
    }

    fun restore(props: Properties): Boolean {
        val shared = all.filter { it !in localOnly && props.getProperty(it.key) != null }
        shared.forEach { it.load(props.getProperty(it.key)) }
        return shared.isNotEmpty()
    }

    fun save() {
        val out = Properties()
        for (setting in all) {
            val value = setting.value
            out.setProperty(setting.key, if (value is Enum<*>) value.name else value.toString())
        }
        Writer.schedule(out)
    }

    private object Writer {
        private val executor = java.util.concurrent.Executors.newSingleThreadScheduledExecutor { task ->
            Thread(task, "pixelgram-settings").apply { isDaemon = true }
        }
        private var pending: Properties? = null
        private var job: java.util.concurrent.ScheduledFuture<*>? = null

        init {
            Runtime.getRuntime().addShutdownHook(Thread { flush() })
        }

        @Synchronized
        fun schedule(props: Properties) {
            pending = props
            job?.cancel(false)
            job = executor.schedule({ flush() }, SaveDelayMs, java.util.concurrent.TimeUnit.MILLISECONDS)
        }

        @Synchronized
        fun flush() {
            val props = pending ?: return
            pending = null
            runCatching {
                AppPaths.settingsFile.outputStream().use { props.store(it, "pixelGram settings") }
            }
        }
    }

    companion object {
        fun load(): AppSettings {
            val props = Properties()
            runCatching {
                val file = AppPaths.settingsFile
                if (file.exists()) file.inputStream().use(props::load)
            }
            return AppSettings(props)
        }
    }
}

private const val SaveDelayMs = 250L
