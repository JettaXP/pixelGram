package com.pixelgram.desktop.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pixelgram.desktop.core.AudioPlayer

enum class SideContent { Media, Profile }

object SidePanel {
    var content by mutableStateOf<SideContent?>(null)
    var media by mutableStateOf<(@Composable () -> Unit)?>(null)
    var shown by mutableStateOf(false)

    val playerUp: Boolean get() = FullPlayer.open && AudioPlayer.current != null

    fun showing(target: SideContent): Boolean = content == target && !playerUp

    fun toggle(target: SideContent) {
        if (playerUp) {
            FullPlayer.open = false
            content = target
        } else {
            content = if (content == target) null else target
        }
    }
}
