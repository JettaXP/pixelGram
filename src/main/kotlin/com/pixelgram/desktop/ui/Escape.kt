package com.pixelgram.desktop.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

object EscapeStack {
    private val handlers = mutableListOf<() -> Boolean>()

    fun dispatch(): Boolean = handlers.asReversed().any { it() }

    internal fun add(handler: () -> Boolean) = handlers.add(handler)

    internal fun remove(handler: () -> Boolean) = handlers.remove(handler)
}

@Composable
fun OnEscape(handler: () -> Boolean) {
    val current by rememberUpdatedState(handler)
    DisposableEffect(Unit) {
        val entry = { current() }
        EscapeStack.add(entry)
        onDispose { EscapeStack.remove(entry) }
    }
}
