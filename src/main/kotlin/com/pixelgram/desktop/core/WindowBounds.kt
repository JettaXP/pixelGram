package com.pixelgram.desktop.core

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPosition
import java.awt.GraphicsEnvironment
import java.awt.Rectangle

object WindowBounds {
    class Saved(val position: WindowPosition, val size: DpSize)

    fun load(settings: AppSettings): Saved? {
        if (!settings.restoreWindow.value) return null
        val parts = settings.windowBounds.value.split(',').mapNotNull { it.trim().toIntOrNull() }
        if (parts.size != 4) return null
        val (x, y, width, height) = parts
        if (width < MinWidth || height < MinHeight) return null
        val visible = runCatching {
            GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices.any { device ->
                device.defaultConfiguration.bounds.intersects(Rectangle(x + Grip, y, width - Grip * 2, Grip))
            }
        }.getOrDefault(false)
        if (!visible) return null
        return Saved(WindowPosition(x.dp, y.dp), DpSize(width.dp, height.dp))
    }

    fun save(settings: AppSettings, x: Int, y: Int, width: Int, height: Int) {
        if (!settings.restoreWindow.value) return
        val value = "$x,$y,$width,$height"
        if (settings.windowBounds.value != value) settings.windowBounds.value = value
    }

    private const val MinWidth = 800
    private const val MinHeight = 520
    private const val Grip = 40
}
