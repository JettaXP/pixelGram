package com.pixelgram.desktop.telegram

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.Color
import it.tdlight.jni.TdApi

object AccentColors {
    private val builtIn = listOf(
        0xFFCC5049 to 0xFFFF8E86,
        0xFFD67722 to 0xFFFFAE5C,
        0xFF955CDB to 0xFFC8A5FF,
        0xFF40A920 to 0xFF8AD86C,
        0xFF309EBA to 0xFF6FD3EC,
        0xFF368AD1 to 0xFF7EBDF5,
        0xFFC7508B to 0xFFFF8CC4,
    )
    private val custom = mutableStateMapOf<Int, TdApi.AccentColor>()

    fun update(colors: Array<TdApi.AccentColor>) {
        colors.forEach { custom[it.id] = it }
    }

    fun colors(id: Int, dark: Boolean): List<Color> {
        if (id in builtIn.indices) return listOf(Color(if (dark) builtIn[id].second else builtIn[id].first))
        val accent = custom[id] ?: return listOf(Color(if (dark) builtIn[0].second else builtIn[0].first))
        val values = if (dark) accent.darkThemeColors else accent.lightThemeColors
        if (values.isEmpty()) return colors(accent.builtInAccentColorId, dark)
        return values.map { Color(0xFF000000.toInt() or it) }
    }
}
