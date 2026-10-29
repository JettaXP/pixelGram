package com.pixelgram.desktop.ui.components

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

@Composable
fun ConfirmOnEnter(onConfirm: () -> Unit, content: @Composable () -> Unit) {
    val focus = remember { FocusRequester() }
    val confirm = rememberUpdatedState(onConfirm)
    Box(
        Modifier
            .onPreviewKeyEvent { event ->
                val enter = event.key == Key.Enter || event.key == Key.NumPadEnter
                if (!enter) return@onPreviewKeyEvent false
                if (event.type == KeyEventType.KeyDown) confirm.value()
                true
            }
            .focusRequester(focus)
            .focusable(),
    ) { content() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
}
