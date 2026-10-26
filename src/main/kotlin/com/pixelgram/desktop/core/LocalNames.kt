package com.pixelgram.desktop.core

object LocalNames {
    var settings: AppSettings? = null

    private fun all(): Map<Long, String> = settings?.localNames?.value.orEmpty()
        .split(Record)
        .mapNotNull { entry ->
            val parts = entry.split(Field, limit = 2)
            val id = parts.getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
            val name = parts.getOrNull(1)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            id to name
        }
        .toMap()

    fun get(userId: Long): String? = all()[userId]

    fun set(userId: Long, name: String?) {
        val next = all().toMutableMap()
        if (name.isNullOrBlank()) next.remove(userId) else next[userId] = name.trim()
        settings?.localNames?.value = next.entries.joinToString(Record) { "${it.key}$Field${it.value}" }
    }

    private const val Record = "\u001E"
    private const val Field = "\u001F"
}
