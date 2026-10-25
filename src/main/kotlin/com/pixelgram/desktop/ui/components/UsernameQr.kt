package com.pixelgram.desktop.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.outlined.QrCode
import com.pixelgram.desktop.ui.components.expressive.AlertDialog
import androidx.compose.material3.Icon
import com.pixelgram.desktop.ui.components.expressive.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.pixelgram.desktop.ui.components.expressive.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.AvatarShape
import com.pixelgram.desktop.ui.auth.QrCode
import com.pixelgram.desktop.ui.toShape
import it.tdlight.jni.TdApi

private val QrAvatarSize = 80.dp
private val QrSize = 260.dp

@Composable
fun UsernameQrButton(username: String, title: String, photo: TdApi.ProfilePhoto?, telegram: Telegram) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }) { Icon(Icons.Outlined.QrCode, contentDescription = "QR code") }
    if (open) UsernameQrDialog(username, title, photo, telegram) { open = false }
}

@Composable
fun UsernameQrDialog(username: String, title: String, photo: TdApi.ProfilePhoto?, telegram: Telegram, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AvatarImage(
                    title,
                    photo?.small,
                    telegram,
                    AvatarShape.Cookie12Sided.toShape(0),
                    size = QrAvatarSize,
                    full = photo?.big,
                )
                QrCode("https://t.me/$username", Modifier.padding(top = 16.dp).size(QrSize))
                Text(
                    "@$username",
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
