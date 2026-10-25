package com.pixelgram.desktop.ui.main

import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.outlined.Settings
import com.pixelgram.desktop.ui.components.expressive.AlertDialog
import androidx.compose.material3.Icon
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.components.expressive.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.core.Backup
import java.io.File

object BackupViewer {
    var path by mutableStateOf<String?>(null)
}

@Composable
fun BackupImportDialog(file: File, settings: AppSettings, onDone: (Boolean?) -> Unit) {
    AlertDialog(
        onDismissRequest = { onDone(null) },
        icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
        title = { Text("Import settings?") },
        text = { Text("pixelGram Preferences from ${file.name} will replace your current ones. Accounts stay as they are.") },
        confirmButton = { TextButton(onClick = { onDone(Backup.import(settings, file)) }) { Text("Import") } },
        dismissButton = { TextButton(onClick = { onDone(null) }) { Text("Cancel") } },
    )
}
