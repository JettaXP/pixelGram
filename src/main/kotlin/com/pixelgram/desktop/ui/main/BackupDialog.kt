package com.pixelgram.desktop.ui.main

import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.outlined.Settings
import com.pixelgram.desktop.ui.icons.outlined.PhoneAndroid
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
    var path by mutableStateOf<String?>(System.getProperty("pixelgram.demo.backup"))
}

@Composable
fun BackupImportDialog(file: File, settings: AppSettings, onDone: (Boolean?) -> Unit) {
    val mobile = androidx.compose.runtime.remember(file) { Backup.isMobile(file) }
    if (mobile) {
        AlertDialog(
            onDismissRequest = { onDone(null) },
            icon = { Icon(Icons.Outlined.PhoneAndroid, contentDescription = null) },
            title = { Text("Mobile backup") },
            text = { Text("This backup was made in pixelGram for Android. Mobile and desktop settings are different, so it can't be applied here.") },
            confirmButton = { TextButton(onClick = { onDone(null) }) { Text("OK") } },
        )
        return
    }
    AlertDialog(
        onDismissRequest = { onDone(null) },
        icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
        title = { Text("Import settings?") },
        text = { Text(com.pixelgram.desktop.core.tr("pixelGram Preferences from {file} will replace your current ones. Accounts stay as they are.").replace("{file}", file.name)) },
        confirmButton = { TextButton(onClick = { onDone(Backup.import(settings, file)) }) { Text("Import") } },
        dismissButton = { TextButton(onClick = { onDone(null) }) { Text("Cancel") } },
    )
}
