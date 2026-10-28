package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.telegram.ChatState
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.telegram.resetChatWallpaper
import com.pixelgram.desktop.telegram.setChatTheme
import com.pixelgram.desktop.telegram.setChatWallpaper
import com.pixelgram.desktop.telegram.themeName
import com.pixelgram.desktop.telegram.themeSettings
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.components.expressive.AlertDialog
import com.pixelgram.desktop.ui.components.expressive.TextButton
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.outlined.Block
import com.pixelgram.desktop.ui.theme.EmojiText
import com.pixelgram.desktop.ui.theme.isDarkTheme
import com.pixelgram.desktop.ui.theme.schemeFromSeed
import it.tdlight.jni.TdApi
import kotlinx.coroutines.launch

private val CardWidth = 76.dp
private val CardHeight = 100.dp
private val CardShape = RoundedCornerShape(16.dp)
private val BubbleShape = RoundedCornerShape(8.dp)

@Composable
fun ChatThemeScope(chat: ChatState, telegram: Telegram, settings: AppSettings, content: @Composable () -> Unit) {
    val dark = isDarkTheme(settings)
    val accent = telegram.themeSettings(chat, dark)?.accentColor
    if (accent == null) return content()
    val style = settings.colorStyle.value
    val scheme = remember(accent, dark, style) { schemeFromSeed(rgb(accent).toArgb(), dark, style) }
    MaterialExpressiveTheme(
        colorScheme = scheme,
        motionScheme = MaterialTheme.motionScheme,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = content,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatThemeDialog(chat: ChatState, telegram: Telegram, settings: AppSettings, onDismiss: () -> Unit) {
    val dark = isDarkTheme(settings)
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    val current = telegram.themeName(chat)
    val report = { failure: String? -> error = failure }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chat theme") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeCard(null, null, current == null) { telegram.setChatTheme(chat, null, report) }
                    telegram.emojiThemes.forEach { theme ->
                        val themeSettings = if (dark) theme.darkSettings else theme.lightSettings
                        ThemeCard(theme.name, themeSettings, current == theme.name) { telegram.setChatTheme(chat, theme.name, report) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        scope.launch {
                            Wallpapers.pick()?.let { file -> telegram.setChatWallpaper(chat, file, blurred = false, dimming = 0, done = report) }
                        }
                    }) { Text("Choose wallpaper") }
                    if (chat.background != null) {
                        TextButton(onClick = { telegram.resetChatWallpaper(chat, report) }) { Text("Reset wallpaper") }
                    }
                }
                Text(
                    "The theme and wallpaper are shown to both you and your contact.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun ThemeCard(emoji: String?, theme: TdApi.ThemeSettings?, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val fill = (theme?.background?.type as? TdApi.BackgroundTypeFill)?.fill ?: (theme?.background?.type as? TdApi.BackgroundTypePattern)?.fill
    val backdrop = fill?.let(::fillBrush) ?: Brush.linearGradient(listOf(colors.surfaceContainerHighest, colors.surfaceContainerHighest))
    val outgoing = theme?.outgoingMessageFill?.let(::fillBrush) ?: Brush.linearGradient(listOf(colors.primary, colors.primary))
    val incoming = if (theme?.background?.isDark == true) Color(0xFF2B2B30) else Color.White
    Box(
        Modifier
            .size(CardWidth, CardHeight)
            .clip(CardShape)
            .background(backdrop)
            .border(if (selected) 3.dp else 1.dp, if (selected) colors.primary else colors.outlineVariant, CardShape)
            .clickable(onClick = onClick),
    ) {
        if (emoji == null) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.Block, contentDescription = null, tint = colors.onSurfaceVariant)
                Text("No theme", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
        } else {
            Column(Modifier.padding(8.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.align(Alignment.End).width(36.dp).height(12.dp).clip(BubbleShape).background(outgoing))
                Box(Modifier.width(30.dp).height(12.dp).clip(BubbleShape).background(incoming))
                Spacer(Modifier.height(6.dp))
                EmojiText(emoji, style = MaterialTheme.typography.titleLarge, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}
