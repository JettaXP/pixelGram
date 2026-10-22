package com.pixelgram.desktop.core

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.pixelgram.desktop.ui.AvatarShape
import java.util.Properties

enum class ThemeMode { System, Light, Dark }

enum class EmojiStyle { Noto, Noto3D }

enum class ToastSide { Left, Right, Top, Bottom }

enum class NotificationStyle { System, PixelGram }

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
    val chatListWidth = int("chatListWidth", 360)
    val customTitleBar = bool("customTitleBar", !AppPaths.isLinux)
    val capsuleSpacing = int("capsuleSpacing", 4)
    val capsuleHeight = int("capsuleContainerHeight", 64)

    val avatarShape = enum("avatarShape", AvatarShape.Circle)
    val avatarCorner = int("avatarCorner", 50)

    val bubbleRadius = int("bubbleRadius", 18)
    val bubbleTailRadius = int("bubbleTailRadius", 4)

    val apiId = int("apiId", 0)
    val apiHash = string("apiHash", "")

    val accounts = string("accounts", "0")
    val activeAccount = int("activeAccount", 0)

    private val all: List<Setting<*>> = listOf(
        themeMode, seedColor, matugen, emojiStyle, notificationSound, notificationsMuted,
        notificationPopups, notificationSide, notificationStyle,
        materialFolders, hideAllChats,
        capsuleFolders, capsuleSpacing, capsuleHeight, chatListWidth, customTitleBar,
        avatarShape, avatarCorner,
        bubbleRadius, bubbleTailRadius,
        apiId, apiHash,
        accounts, activeAccount,
    )

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
