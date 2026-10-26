package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.core.FileSafety
import com.pixelgram.desktop.ui.components.Avatar
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.theme.EmojiText
import it.tdlight.jni.TdApi
import javax.swing.SwingUtilities

@Composable
fun LinkDialogs(settings: AppSettings, onOpenChat: (Long) -> Unit) {
    Links.invite?.let { request -> InviteDialog(request, settings, onOpenChat) { Links.invite = null } }
    Links.confirm?.let { url -> OpenLinkDialog(url) { Links.confirm = null } }
    Links.nft?.let { (telegram, name) -> NftDialog(telegram, name) { Links.nft = null } }
    FileSafety.pending?.let { path -> DangerousFileDialog(path) { FileSafety.pending = null } }
}

@Composable
private fun DangerousFileDialog(path: String, onDismiss: () -> Unit) {
    val name = java.io.File(path).name
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Open this file?") },
        text = {
            Text(
                "$name has the .${FileSafety.extension(name)} extension. It can run programs on your computer. Open it only if you trust the sender.",
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                openFile(path)
            }) { Text("Open anyway") }
        },
    )
}

@Composable
private fun InviteDialog(request: Links.InviteRequest, settings: AppSettings, onOpenChat: (Long) -> Unit, onDismiss: () -> Unit) {
    val info = request.info
    var joining by remember(request) { mutableStateOf(false) }
    val channel = (info.type as? TdApi.InviteLinkChatTypeChannel) != null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Avatar(info.title, info.photo?.small, request.telegram, settings, InviteAvatar)
                EmojiText(info.title, style = MaterialTheme.typography.titleLarge)
                Text(
                    "%,d ${if (channel) "subscribers" else "members"}".format(info.memberCount).replace(',', ' '),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (info.description.isNotBlank()) {
                    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                        EmojiText(
                            info.description,
                            style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                        )
                    }
                }
                if (info.createsJoinRequest) {
                    Text("An admin has to approve your request.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        confirmButton = {
            TextButton(enabled = !joining, onClick = {
                joining = true
                request.telegram.joinByInvite(request.link) { chatId ->
                    SwingUtilities.invokeLater {
                        onDismiss()
                        if (chatId != 0L) onOpenChat(chatId)
                    }
                }
            }) {
                Text(
                    when {
                        info.createsJoinRequest -> "Request to join"
                        channel -> "Join channel"
                        else -> "Join group"
                    },
                )
            }
        },
    )
}

@Composable
private fun OpenLinkDialog(url: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Open this link?") },
        text = { Text(url, color = MaterialTheme.colorScheme.primary) },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                Links.open(url)
            }) { Text("Open") }
        },
    )
}

private val InviteAvatar = 80.dp
