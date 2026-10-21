package com.pixelgram.desktop.ui.profile

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.components.Avatar
import com.pixelgram.desktop.ui.components.InfoGroup
import com.pixelgram.desktop.ui.components.InfoRow
import com.pixelgram.desktop.ui.components.PageScaffold
import it.tdlight.jni.TdApi

@Composable
fun ProfileScreen(telegram: Telegram, settings: AppSettings, onBack: () -> Unit) {
    val me = telegram.me
    var fullInfo by remember { mutableStateOf<TdApi.UserFullInfo?>(null) }
    LaunchedEffect(me?.id) { telegram.myFullInfo { fullInfo = it } }

    PageScaffold("My Profile", onBack) {
        if (me == null) {
            Text("Not signed in")
            return@PageScaffold
        }
        val name = listOf(me.firstName, me.lastName).filter { !it.isNullOrBlank() }.joinToString(" ")
        Avatar(name, me.profilePhoto?.big ?: me.profilePhoto?.small, telegram, settings, size = 160.dp)
        Text(name, style = MaterialTheme.typography.headlineMediumEmphasized)
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
    }
}
