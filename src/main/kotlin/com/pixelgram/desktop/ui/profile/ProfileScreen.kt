package com.pixelgram.desktop.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.telegram.fullName
import com.pixelgram.desktop.ui.components.Avatar
import com.pixelgram.desktop.ui.components.InfoGroup
import com.pixelgram.desktop.ui.components.InfoRow
import com.pixelgram.desktop.ui.components.PageScaffold
import com.pixelgram.desktop.ui.theme.EmojiText
import it.tdlight.jni.TdApi

@Composable
fun ProfileScreen(
    telegram: Telegram,
    settings: AppSettings,
    onOpenChat: (Long) -> Unit,
    onBack: () -> Unit,
) {
    val me = telegram.me
    var fullInfo by remember { mutableStateOf<TdApi.UserFullInfo?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    var picking by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(me?.id, refresh) { telegram.myFullInfo { fullInfo = it } }
    val setChannel = { chatId: Long ->
        picking = false
        telegram.setPersonalChat(chatId) {
            error = it
            refresh++
        }
    }

    PageScaffold("My Profile", onBack) {
        if (me == null) {
            Text("Not signed in")
            return@PageScaffold
        }
        val name = me.fullName()
        Avatar(name, me.profilePhoto?.small, telegram, settings, size = 160.dp, full = me.profilePhoto?.big)
        EmojiText(name, style = MaterialTheme.typography.headlineMediumEmphasized)
        val rows = buildList {
            me.phoneNumber?.takeIf { it.isNotEmpty() }?.let {
                add(InfoRow(Icons.Outlined.Phone, "Phone", "+$it"))
            }
            me.usernames?.activeUsernames?.firstOrNull()?.let {
                add(InfoRow(Icons.Outlined.AlternateEmail, "Username", "@$it"))
            }
            fullInfo?.bio?.text?.takeIf { it.isNotBlank() }?.let {
                add(InfoRow(Icons.Outlined.Info, "Bio", it))
            }
            add(InfoRow(Icons.Outlined.Numbers, "ID", me.id.toString()))
        }
        InfoGroup(rows)

        val channelId = fullInfo?.personalChatId ?: 0L
        if (channelId != 0L) {
            PersonalChannelCard(channelId, telegram, settings, onOpenChat, Modifier.fillMaxWidth())
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { picking = true }) {
                Text(if (channelId != 0L) "Change personal channel" else "Add personal channel")
            }
            if (channelId != 0L) {
                TextButton(onClick = { setChannel(0L) }) { Text("Remove") }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
    if (picking) {
        PersonalChannelPicker(telegram, settings, onPicked = setChannel, onDismiss = { picking = false })
    }
}
