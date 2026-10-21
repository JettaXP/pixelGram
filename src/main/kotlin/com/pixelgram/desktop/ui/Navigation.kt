package com.pixelgram.desktop.ui

// Top-level screens of the app window.
sealed interface Screen {
    data object Chats : Screen
    data class Settings(val preferences: Boolean = false) : Screen
    data object Profile : Screen
    data object Memory : Screen
}
