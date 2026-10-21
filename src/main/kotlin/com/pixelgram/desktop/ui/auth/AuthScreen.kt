package com.pixelgram.desktop.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.telegram.AuthState
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.themedAppIcon

@Composable
fun AuthScreen(telegram: Telegram, onCancel: (() -> Unit)? = null) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.width(420.dp),
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Logo()
                    Text("pixelGram", style = MaterialTheme.typography.headlineMedium)
                    var editingNumber by remember { mutableStateOf(false) }
                    val state = telegram.authState
                    LaunchedEffect(state) {
                        if (state !is AuthState.WaitCode) editingNumber = false
                    }
                    when {
                        editingNumber && state is AuthState.WaitCode -> PhoneStep(
                            telegram,
                            onQr = {
                                editingNumber = false
                                telegram.requestQr()
                            },
                        )
                        else -> when (state) {
                        AuthState.Starting, AuthState.Closing -> LoadingIndicator()
                        AuthState.NeedApiCredentials -> ApiCredentialsStep(telegram)
                        AuthState.WaitPhone -> PhoneStep(telegram, onQr = telegram::requestQr)
                        is AuthState.WaitQr -> QrStep(state.link)
                        is AuthState.WaitCode -> CodeStep(state.info, telegram) { editingNumber = true }
                        is AuthState.WaitPassword -> PasswordStep(state.hint, telegram)
                        is AuthState.Unsupported -> Hint(
                            "This sign-in step (${state.name}) is not supported yet. " +
                                "Finish it in another Telegram app."
                        )
                        is AuthState.Failed -> Hint(state.reason)
                        AuthState.Ready -> Unit
                        }
                    }
                    telegram.authError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                    }
                    if (onCancel != null) {
                        TextButton(onClick = onCancel) { Text("Cancel adding account") }
                    }
                }
            }
        }
    }
}

@Composable
private fun Logo() {
    val icon = themedAppIcon() ?: return
    Image(icon, contentDescription = "pixelGram", modifier = Modifier.size(96.dp))
}

// The button turns into a loading indicator while a sign-in request is pending.
@Composable
internal fun BusyButton(
    text: String,
    busy: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !busy,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (busy) {
            LoadingIndicator(
                modifier = Modifier.size(24.dp),
                color = LocalContentColor.current,
            )
        } else {
            Text(text)
        }
    }
}

@Composable
internal fun Hint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun SingleField(
    busy: Boolean,
    label: String,
    hint: String,
    action: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    password: Boolean = false,
    secondary: (@Composable () -> Unit)? = null,
    onSubmit: (String) -> Unit,
) {
    var value by remember { mutableStateOf("") }
    Hint(hint)
    OutlinedTextField(
        value = value,
        onValueChange = { value = it },
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (password) PasswordVisualTransformation() else
            androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { if (value.isNotBlank() && !busy) onSubmit(value) }),
        enabled = !busy,
        modifier = Modifier.fillMaxWidth(),
    )
    BusyButton(action, busy, enabled = value.isNotBlank()) { onSubmit(value) }
    secondary?.invoke()
}

@Composable
private fun ApiCredentialsStep(telegram: Telegram) {
    var id by remember { mutableStateOf("") }
    var hash by remember { mutableStateOf("") }
    Hint("Enter your API ID and API hash from my.telegram.org. They are stored only on this computer.")
    OutlinedTextField(
        value = id,
        onValueChange = { id = it.filter(Char::isDigit) },
        label = { Text("API ID") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = hash,
        onValueChange = { hash = it.trim() },
        label = { Text("API hash") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    BusyButton(
        "Continue",
        busy = telegram.authBusy,
        enabled = id.toIntOrNull() != null && hash.length >= 16,
    ) { telegram.setApiCredentials(id.toInt(), hash) }
}

@Composable
private fun PasswordStep(hint: String, telegram: Telegram) = SingleField(
    busy = telegram.authBusy,
    label = "Password",
    hint = if (hint.isNotEmpty()) "Two-step verification is on. Hint: $hint" else
        "Two-step verification is on. Enter your password.",
    action = "Sign in",
    password = true,
    onSubmit = telegram::sendPassword,
)

@Composable
private fun QrStep(link: String) {
    Hint("Open Telegram on your phone → Settings → Devices → Link Desktop Device and scan this code.")
    QrCode(link, Modifier.size(220.dp))
}
