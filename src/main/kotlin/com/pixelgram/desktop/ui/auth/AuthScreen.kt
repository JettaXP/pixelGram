package com.pixelgram.desktop.ui.auth

import androidx.compose.ui.unit.IntOffset
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.scaleOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.foundation.layout.Spacer
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
                    modifier = Modifier.animateContentSize(MaterialTheme.motionScheme.defaultSpatialSpec()).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Logo()
                    Text("pixelGram", style = MaterialTheme.typography.headlineMedium)
                    var editingNumber by remember { mutableStateOf(false) }
                    val state = telegram.authState
                    LaunchedEffect(state) {
                        if (state !is AuthState.WaitCode && state !is AuthState.WaitQr) editingNumber = false
                    }
                    val step = if (editingNumber && (state is AuthState.WaitCode || state is AuthState.WaitQr)) "phone-edit" else stepName(state)
                    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
                    val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
                    AnimatedContent(
                        targetState = step,
                        transitionSpec = {
                            (slideInHorizontally(spatial) { it / 5 } + fadeIn(effects)) togetherWith
                                (slideOutHorizontally(spatial) { -it / 5 } + fadeOut(effects)) using
                                SizeTransform(clip = false)
                        },
                        contentAlignment = Alignment.TopCenter,
                    ) { _ ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            when {
                                editingNumber && (state is AuthState.WaitCode || state is AuthState.WaitQr) -> PhoneStep(
                                    telegram,
                                    onQr = {
                                        editingNumber = false
                                        if (state !is AuthState.WaitQr) telegram.requestQr()
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
                    val back: (() -> Unit)? = when {
                        editingNumber -> { { editingNumber = false } }
                        state is AuthState.WaitQr -> { { editingNumber = true } }
                        else -> onCancel
                    }
                    var lastBack by remember { mutableStateOf<(() -> Unit)?>(null) }
                    if (back != null) lastBack = back
                    AnimatedVisibility(
                        visible = back != null,
                        enter = scaleIn(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeIn(),
                        exit = scaleOut() + fadeOut(),
                    ) {
                        FilledTonalButton(onClick = { lastBack?.invoke() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize),
                            )
                            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                            Text("Back")
                        }
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
private fun PasswordStep(hint: String, telegram: Telegram) {
    Hint(
        if (hint.isNotEmpty()) "Two-step verification is on. Hint: $hint" else
            "Two-step verification is on. Enter your password."
    )
    ShapePasswordBar(
        busy = telegram.authBusy,
        error = telegram.authError,
        onSubmit = telegram::sendPassword,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun QrStep(link: String) {
    Hint("Open Telegram on your phone → Settings → Devices → Link Desktop Device and scan this code.")
    QrCode(link, Modifier.size(220.dp))
}

private fun stepName(state: AuthState): String = when (state) {
    is AuthState.WaitQr -> "qr"
    is AuthState.WaitCode -> "code"
    is AuthState.WaitPassword -> "password"
    is AuthState.Unsupported -> "unsupported"
    is AuthState.Failed -> "failed"
    else -> state.toString()
}
