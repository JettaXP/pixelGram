package com.pixelgram.desktop.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.tr
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.telegram.confirmRecoveryEmail
import com.pixelgram.desktop.telegram.setCloudPassword
import com.pixelgram.desktop.ui.components.ConfirmOnEnter
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.components.expressive.AlertDialog
import com.pixelgram.desktop.ui.components.expressive.FilledTonalButton
import com.pixelgram.desktop.ui.components.expressive.TextButton
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.outlined.Key
import it.tdlight.jni.TdApi

private enum class CloudStep { Manage, Set, Change, Off, EmailCode, Done }

@Composable
fun CloudPasswordDialog(telegram: Telegram, state: TdApi.PasswordState, onChanged: (TdApi.PasswordState) -> Unit, onDismiss: () -> Unit) {
    var step by remember { mutableStateOf(if (state.hasPassword) CloudStep.Manage else CloudStep.Set) }
    var current by remember { mutableStateOf("") }
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    var hint by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var pattern by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    fun finish(result: TdApi.PasswordState?, failure: String?) {
        busy = false
        if (failure != null || result == null) {
            error = failure ?: tr("Something went wrong")
            return
        }
        onChanged(result)
        val info = result.recoveryEmailAddressCodeInfo
        if (info != null) {
            pattern = info.emailAddressPattern
            step = CloudStep.EmailCode
        } else {
            step = CloudStep.Done
        }
    }
    val confirm: () -> Unit = confirm@{
        if (busy) return@confirm
        error = null
        when (step) {
            CloudStep.Set, CloudStep.Change -> {
                if (first.length < MinPassword) return@confirm run { error = tr("Password is too short") }
                if (first != second) return@confirm run { error = tr("Passwords don't match") }
                if (hint == first) return@confirm run { error = tr("The hint must be different from the password") }
                busy = true
                telegram.setCloudPassword(if (step == CloudStep.Change) current else "", first, hint.trim(), if (step == CloudStep.Set) email else "", ::finish)
            }
            CloudStep.Off -> {
                busy = true
                telegram.setCloudPassword(current, "", "", "", ::finish)
            }
            CloudStep.EmailCode -> {
                busy = true
                telegram.confirmRecoveryEmail(code, ::finish)
            }
            CloudStep.Manage, CloudStep.Done -> onDismiss()
        }
    }
    @Composable
    fun secret(value: String, label: String, onChange: (String) -> Unit) = OutlinedTextField(
        value, onChange, label = { Text(label) }, singleLine = true,
        visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(),
    )
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        icon = { Icon(Icons.Outlined.Key, contentDescription = null) },
        title = { Text("Two-step verification") },
        text = {
            ConfirmOnEnter(confirm) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (step) {
                        CloudStep.Manage -> {
                            Text("Your account is protected by an additional password. You'll need it when you log in on a new device.")
                            if (state.passwordHint.isNotEmpty()) Text("${tr("Hint")}: ${state.passwordHint}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                if (state.hasRecoveryEmailAddress) tr("Recovery email is set") else tr("No recovery email"),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            FilledTonalButton(onClick = { step = CloudStep.Change }) { Text("Change password") }
                            TextButton(onClick = { step = CloudStep.Off }) { Text("Turn password off", color = MaterialTheme.colorScheme.error) }
                        }
                        CloudStep.Set, CloudStep.Change -> {
                            if (step == CloudStep.Set) Text("Set an additional password that will be required when you log in on a new device.")
                            if (step == CloudStep.Change) secret(current, tr("Current password")) { current = it }
                            secret(first, tr("New password")) { first = it }
                            secret(second, tr("Repeat password")) { second = it }
                            OutlinedTextField(hint, { hint = it }, label = { Text("Hint (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            if (step == CloudStep.Set) {
                                OutlinedTextField(email, { email = it }, label = { Text("Recovery email (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            }
                        }
                        CloudStep.Off -> {
                            Text("Enter your current password to turn two-step verification off.")
                            secret(current, tr("Current password")) { current = it }
                        }
                        CloudStep.EmailCode -> {
                            Text("${tr("Enter the code we sent to")} $pattern")
                            OutlinedTextField(code, { code = it.filter(Char::isDigit) }, label = { Text("Code") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        }
                        CloudStep.Done -> Text("Two-step verification settings are saved.")
                    }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = confirm, enabled = !busy) {
                Text(
                    when (step) {
                        CloudStep.Manage, CloudStep.Done -> "Done"
                        CloudStep.Off -> "Turn off"
                        CloudStep.EmailCode -> "Confirm"
                        else -> "Save"
                    },
                )
            }
        },
        dismissButton = if (step == CloudStep.Manage || step == CloudStep.Done) null else ({ TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") } }),
    )
}

private const val MinPassword = 4
