package com.pixelgram.desktop.core

import it.tdlight.jni.TdApi

object LocalPremiumEmoji {
    private val Link = Regex("^tg://emoji\\?id=(\\d+)$")

    fun isEmoji(text: String): Boolean {
        if (text.isBlank() || text.length > MaxEmojiLength) return false
        if (text.contains('⃣')) return true
        return text.codePoints().allMatch { point ->
            !Character.isLetterOrDigit(point) && !Character.isWhitespace(point) && (point > 0x2000 || point == 0x00A9 || point == 0x00AE)
        }
    }

    fun incoming(text: String, entities: Array<TdApi.TextEntity>): Array<TdApi.TextEntity> {
        if (entities.none { (it.type as? TdApi.TextEntityTypeTextUrl)?.url?.startsWith(Prefix) == true }) return entities
        return entities.map { entity ->
            val url = (entity.type as? TdApi.TextEntityTypeTextUrl)?.url ?: return@map entity
            val id = Link.find(url)?.groupValues?.get(1)?.toLongOrNull() ?: return@map entity
            val shown = text.substring(entity.offset.coerceIn(0, text.length), (entity.offset + entity.length).coerceIn(0, text.length))
            if (isEmoji(shown)) TdApi.TextEntity(entity.offset, entity.length, TdApi.TextEntityTypeCustomEmoji(id)) else entity
        }.toTypedArray()
    }

    fun outgoing(entities: Array<TdApi.TextEntity>, native: Boolean): Array<TdApi.TextEntity> {
        if (native || entities.none { it.type is TdApi.TextEntityTypeCustomEmoji }) return entities
        return entities.map { entity ->
            val type = entity.type as? TdApi.TextEntityTypeCustomEmoji ?: return@map entity
            TdApi.TextEntity(entity.offset, entity.length, TdApi.TextEntityTypeTextUrl("$Prefix${type.customEmojiId}"))
        }.toTypedArray()
    }

    private const val Prefix = "tg://emoji?id="
    private const val MaxEmojiLength = 16
}
