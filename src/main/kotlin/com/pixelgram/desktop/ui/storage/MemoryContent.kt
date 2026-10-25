package com.pixelgram.desktop.ui.storage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.outlined.Description
import com.pixelgram.desktop.ui.icons.outlined.Language
import com.pixelgram.desktop.ui.icons.outlined.PermMedia
import com.pixelgram.desktop.ui.icons.outlined.Storage
import com.pixelgram.desktop.ui.components.expressive.Button
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AppPaths
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.components.InfoGroup
import com.pixelgram.desktop.ui.components.InfoRow
import it.tdlight.jni.TdApi

@Composable
fun MemoryContent(telegram: Telegram) {
    var stats by remember { mutableStateOf<TdApi.StorageStatisticsFast?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    var clearing by remember { mutableStateOf(false) }
    LaunchedEffect(refresh) { telegram.storageStatistics { stats = it } }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val current = stats
        if (current == null) {
            LoadingIndicator()
            return@Column
        }
        val total = current.filesSize + current.databaseSize + current.languagePackDatabaseSize + current.logSize
        Text(formatSize(total), style = MaterialTheme.typography.displaySmallEmphasized)
        Text(
            "used by pixelGram in ${AppPaths.dataDir.absolutePath}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        InfoGroup(listOf(
            InfoRow(Icons.Outlined.PermMedia, "Media and files (${current.fileCount})", formatSize(current.filesSize)),
            InfoRow(Icons.Outlined.Storage, "Message database", formatSize(current.databaseSize)),
            InfoRow(Icons.Outlined.Language, "Language pack", formatSize(current.languagePackDatabaseSize)),
            InfoRow(Icons.Outlined.Description, "Logs", formatSize(current.logSize)),
        ))
        Button(
            onClick = {
                clearing = true
                telegram.clearCache {
                    clearing = false
                    refresh++
                }
            },
            enabled = !clearing && current.filesSize > 0,
        ) {
            Text(if (clearing) "Clearing…" else "Clear cache (${formatSize(current.filesSize)})")
        }
        Text(
            "Clears downloaded media and files. Chats and messages stay, media downloads again when needed.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

fun formatSize(bytes: Long): String {
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    return if (unit == 0) "$bytes B" else "%.1f %s".format(value, units[unit])
}
