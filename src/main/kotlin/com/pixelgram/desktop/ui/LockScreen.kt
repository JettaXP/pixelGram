package com.pixelgram.desktop.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AppLock
import com.pixelgram.desktop.ui.components.rememberPixelgramLogo
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val ShakeMs = 400
private const val ShakePx = 14f

@Composable
fun LockScreen() {
    var passcode by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    val shake = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val attempt = { code: String ->
        if (!AppLock.unlock(code)) {
            wrong = true
            passcode = ""
            scope.launch {
                shake.animateTo(0f, keyframes {
                    durationMillis = ShakeMs
                    ShakePx at ShakeMs / 8
                    -ShakePx at ShakeMs * 3 / 8
                    ShakePx / 2 at ShakeMs * 5 / 8
                    -ShakePx / 2 at ShakeMs * 7 / 8
                })
            }
        }
    }
    val press: (String) -> Unit = { key ->
        when (key) {
            BackspaceKey -> passcode = passcode.dropLast(1)
            EnterKey -> if (passcode.length == AppLock.PinLength) attempt(passcode)
            else -> if (passcode.length < AppLock.PinLength) {
                passcode += key
                wrong = false
                if (passcode.length == AppLock.PinLength) attempt(passcode)
            }
        }
    }
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focus)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val digit = event.utf16CodePoint.toChar().takeIf { it.isDigit() }
                    when {
                        digit != null -> press(digit.toString())
                        event.key == Key.Backspace -> press(BackspaceKey)
                        event.key == Key.Enter || event.key == Key.NumPadEnter -> press(EnterKey)
                        else -> return@onPreviewKeyEvent false
                    }
                    true
                },
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(72.dp).clip(MaterialShapes.Cookie12Sided.toShape()).background(MaterialTheme.colorScheme.primary),
                ) {
                    rememberPixelgramLogo()?.let { Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(42.dp)) }
                }
                Text(
                    if (wrong) "Wrong passcode" else "Enter passcode",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (wrong) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
                PinDots(passcode.length, Modifier.offset { IntOffset(shake.value.roundToInt(), 0) })
                PinPad(onKey = press, modifier = Modifier.padding(top = 12.dp))
            }
        }
    }
}

