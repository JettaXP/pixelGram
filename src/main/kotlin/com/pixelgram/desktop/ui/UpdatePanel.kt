package com.pixelgram.desktop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.BuildConfig
import com.pixelgram.desktop.core.UpdateState
import com.pixelgram.desktop.core.Updater
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@Composable
fun UpdatePanel(onBeforeExit: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val build = if (Updater.enabled) "${BuildConfig.VERSION} (${BuildConfig.COMMIT.take(7)})" else "${BuildConfig.VERSION} (dev build)"
        Text("Installed: $build", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        when (val state = Updater.state) {
            UpdateState.Idle -> Unit
            UpdateState.Checking -> Text("Checking…")
            UpdateState.UpToDate -> Text("You have the latest nightly.")
            UpdateState.Unavailable -> Text("Updates are unavailable right now.")
            is UpdateState.Available -> Text("New nightly ${state.commit} is out: ${state.asset.name}")
            is UpdateState.Downloading -> {
                Text("Downloading ${state.asset.name}… ${(Updater.progress * 100).toInt()}%")
                LinearWavyProgressIndicator(progress = { Updater.progress }, modifier = Modifier.fillMaxWidth())
            }
            is UpdateState.Ready -> {
                Text("Downloaded ${state.file.name}.")
                state.manual?.let { step ->
                    Text(step, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            is UpdateState.Failed -> Text(state.message, color = MaterialTheme.colorScheme.error)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when (val state = Updater.state) {
                is UpdateState.Available -> Button(onClick = Updater::download) { Text("Download") }
                is UpdateState.Ready -> {
                    if (state.manual == null) {
                        Button(onClick = { Updater.install(onBeforeExit) }) { Text("Update") }
                    } else {
                        Button(onClick = { Updater.openFolder(state.file.parentFile) }) { Text("Show file") }
                        if (state.manual.startsWith("sudo")) {
                            OutlinedButton(onClick = {
                                Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(state.manual), null)
                            }) { Text("Copy command") }
                        }
                    }
                }
                is UpdateState.Downloading, UpdateState.Checking -> Unit
                else -> OutlinedButton(onClick = Updater::check, enabled = Updater.enabled) { Text("Check for updates") }
            }
        }
    }
}

@Composable
fun UpdateDialog(onBeforeExit: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.SystemUpdate, contentDescription = null) },
        title = { Text("Update (beta)") },
        text = { UpdatePanel(onBeforeExit) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        shape = RoundedCornerShape(28.dp),
    )
}
