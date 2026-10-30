package com.pixelgram.desktop.core

import it.tdlight.jni.TdApi

object Tweaks {
    var settings: AppSettings? = null

    val exactNumbers: Boolean get() = settings?.exactNumbers?.value == true

    private val zalgo: Boolean get() = settings?.zalgoFilter?.value == true

    fun name(text: String): String = if (zalgo) clean(text, emptyList()).first else text

    fun clean(text: String, entities: List<TdApi.TextEntity>): Pair<String, List<TdApi.TextEntity>> {
        if (!zalgo || text.none(::isMark)) return text to entities
        val out = StringBuilder(text.length)
        val newIndex = IntArray(text.length + 1)
        var marks = 0
        for (i in text.indices) {
            newIndex[i] = out.length
            val c = text[i]
            marks = if (isMark(c)) marks + 1 else 0
            if (marks <= MaxMarks) out.append(c)
        }
        newIndex[text.length] = out.length
        if (out.length == text.length) return text to entities
        val moved = entities.mapNotNull { entity ->
            val start = newIndex[entity.offset.coerceIn(0, text.length)]
            val end = newIndex[(entity.offset + entity.length).coerceIn(0, text.length)]
            if (end <= start) null else TdApi.TextEntity(start, end - start, entity.type)
        }
        return out.toString() to moved
    }

    private fun isMark(c: Char): Boolean = when (Character.getType(c).toByte()) {
        Character.NON_SPACING_MARK, Character.ENCLOSING_MARK -> c !in VariationSelectors
        else -> false
    }

    private val VariationSelectors = '\uFE00'..'\uFE0F'
    private const val MaxMarks = 2
}
