package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.tr
import com.pixelgram.desktop.telegram.ChatState
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.telegram.secretChat
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.components.expressive.AlertDialog
import com.pixelgram.desktop.ui.components.expressive.TextButton
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.filled.Lock

private const val IdenticonCells = 8
private const val IdenticonBytes = 16
private const val HexChars = 8
private const val HexPerLine = 3
private val IdenticonSize = 200.dp
private val IdenticonColors = listOf(Color(0xFFFFFFFF), Color(0xFFD5E6F3), Color(0xFF2D5775), Color(0xFF2F99C9))

@Composable
fun SecretLock(chat: ChatState, telegram: Telegram, size: Dp) {
    if (!telegram.isSecret(chat)) return
    Icon(Icons.Filled.Lock, contentDescription = "Secret chat", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(size))
}

@Composable
fun EncryptionKeyDialog(chat: ChatState, telegram: Telegram, onDismiss: () -> Unit) {
    val key = telegram.secretChat(chat)?.keyHash ?: ByteArray(0)
    val name = telegram.chatTitle(chat)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Encryption key") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (key.size >= IdenticonBytes) Identicon(key)
                Text(
                    key.joinToString("") { "%02x".format(it) }.chunked(HexChars).chunked(HexPerLine).joinToString("\n") { it.joinToString(" ") },
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    tr("If this image and text look the same on {name}'s device, your chat is 100% secure.").replace("{name}", name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun Identicon(key: ByteArray) {
    Canvas(Modifier.size(IdenticonSize).clip(RoundedCornerShape(12.dp))) {
        val cell = size.width / IdenticonCells
        var bit = 0
        for (row in 0 until IdenticonCells) {
            for (column in 0 until IdenticonCells) {
                val byte = key[bit / 8].toInt() and 0xFF
                val index = (byte shr (bit % 8)) and 0x3
                bit += 2
                drawRect(IdenticonColors[index], Offset(column * cell, row * cell), Size(cell, cell))
            }
        }
    }
}
