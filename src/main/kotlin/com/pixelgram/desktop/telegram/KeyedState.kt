package com.pixelgram.desktop.telegram

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class KeyedState<K, V : Any> {
    private val slots = HashMap<K, MutableState<V?>>()
    private var count by mutableIntStateOf(0)

    private fun slot(key: K): MutableState<V?> = synchronized(slots) { slots.getOrPut(key) { mutableStateOf(null) } }

    operator fun get(key: K): V? = slot(key).value

    operator fun set(key: K, value: V) {
        val state = slot(key)
        if (state.value == null) count++
        state.value = value
    }

    fun remove(key: K): V? {
        val state = synchronized(slots) { slots[key] } ?: return null
        val old = state.value ?: return null
        state.value = null
        count--
        return old
    }

    operator fun contains(key: K): Boolean = get(key) != null

    fun containsKey(key: K): Boolean = contains(key)

    val size: Int get() = count

    fun clear() {
        val all = synchronized(slots) { slots.values.toList() }
        all.forEach { it.value = null }
        count = 0
    }
}
