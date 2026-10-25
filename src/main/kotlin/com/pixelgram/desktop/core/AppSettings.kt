package com.pixelgram.desktop.core

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.pixelgram.desktop.ui.AvatarShape
import java.util.Properties

enum class ThemeMode { System, Light, Dark }

enum class EmojiStyle { Noto, Noto3D }

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
) {
    private val state: MutableState<T> = mutableStateOf(initial)

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
        Setting(this, key, props.getProperty(key) ?: default)

    private fun int(key: String, default: Int) =
        Setting(this, key, props.getProperty(key)?.toIntOrNull() ?: default)

    private fun bool(key: String, default: Boolean) =
        Setting(this, key, props.getProperty(key)?.toBooleanStrictOrNull() ?: default)

    private inline fun <reified E : Enum<E>> enum(key: String, default: E) =
        Setting(this, key, props.getProperty(key)?.let { name ->
            enumValues<E>().firstOrNull { it.name == name }
        } ?: default)

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

    val bubbleRadius = int("bubbleRadius", 18)
    val bubbleTailRadius = int("bubbleTailRadius", 4)

    val wallpaper = string("wallpaper", "")
    val wallpaperDim = int("wallpaperDim", 20)
    val wallpaperColors = bool("wallpaperColors", false)

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
        wallpaper, wallpaperDim, wallpaperColors,
        deviceName, israelLogo, outputDevice, inputDevice, cameraDevice, callsSameDevices, acceptCalls, passcodeHash, passcodeSalt, autoLockMinutes,
        stickersInPanel, stickersInChat, emojiInMessages, spoilerEffect, messageAnimations, interfaceAnimations,
        apiId, apiHash,
        accounts, activeAccount,
    ) + avatarKindShapes.values

    private val localOnly = setOf(apiId, apiHash, accounts, activeAccount, outputDevice, inputDevice, cameraDevice)

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
        runCatching {
            AppPaths.settingsFile.outputStream().use { out.store(it, "pixelGram settings") }
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
