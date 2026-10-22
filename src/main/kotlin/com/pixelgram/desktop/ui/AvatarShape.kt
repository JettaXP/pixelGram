package com.pixelgram.desktop.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape
import androidx.graphics.shapes.RoundedPolygon

enum class AvatarShape(val title: String) {
    Custom("Custom corner"),
    Circle("Circle"),
    Square("Square"),
    Slanted("Slanted"),
    Arch("Arch"),
    Fan("Fan"),
    Arrow("Arrow"),
    SemiCircle("Semicircle"),
    Oval("Oval"),
    Pill("Pill"),
    Triangle("Triangle"),
    Diamond("Diamond"),
    ClamShell("Clamshell"),
    Pentagon("Pentagon"),
    Gem("Gem"),
    Sunny("Sunny"),
    VerySunny("Very sunny"),
    Cookie4Sided("4-sided cookie"),
    Cookie6Sided("6-sided cookie"),
    Cookie7Sided("7-sided cookie"),
    Cookie9Sided("9-sided cookie"),
    Cookie12Sided("12-sided cookie"),
    Ghostish("Ghost-ish"),
    Clover4Leaf("4-leaf clover"),
    Clover8Leaf("8-leaf clover"),
    Burst("Burst"),
    SoftBurst("Soft burst"),
    Boom("Boom"),
    SoftBoom("Soft boom"),
    Flower("Flower"),
    Puffy("Puffy"),
    PuffyDiamond("Puffy diamond"),
    PixelCircle("Pixel circle"),
    PixelTriangle("Pixel triangle"),
    Bun("Bun"),
    Heart("Heart");

    val polygon: RoundedPolygon?
        get() = when (this) {
            Custom -> null
            Circle -> MaterialShapes.Circle
            Square -> MaterialShapes.Square
            Slanted -> MaterialShapes.Slanted
            Arch -> MaterialShapes.Arch
            Fan -> MaterialShapes.Fan
            Arrow -> MaterialShapes.Arrow
            SemiCircle -> MaterialShapes.SemiCircle
            Oval -> MaterialShapes.Oval
            Pill -> MaterialShapes.Pill
            Triangle -> MaterialShapes.Triangle
            Diamond -> MaterialShapes.Diamond
            ClamShell -> MaterialShapes.ClamShell
            Pentagon -> MaterialShapes.Pentagon
            Gem -> MaterialShapes.Gem
            Sunny -> MaterialShapes.Sunny
            VerySunny -> MaterialShapes.VerySunny
            Cookie4Sided -> MaterialShapes.Cookie4Sided
            Cookie6Sided -> MaterialShapes.Cookie6Sided
            Cookie7Sided -> MaterialShapes.Cookie7Sided
            Cookie9Sided -> MaterialShapes.Cookie9Sided
            Cookie12Sided -> MaterialShapes.Cookie12Sided
            Ghostish -> MaterialShapes.Ghostish
            Clover4Leaf -> MaterialShapes.Clover4Leaf
            Clover8Leaf -> MaterialShapes.Clover8Leaf
            Burst -> MaterialShapes.Burst
            SoftBurst -> MaterialShapes.SoftBurst
            Boom -> MaterialShapes.Boom
            SoftBoom -> MaterialShapes.SoftBoom
            Flower -> MaterialShapes.Flower
            Puffy -> MaterialShapes.Puffy
            PuffyDiamond -> MaterialShapes.PuffyDiamond
            PixelCircle -> MaterialShapes.PixelCircle
            PixelTriangle -> MaterialShapes.PixelTriangle
            Bun -> MaterialShapes.Bun
            Heart -> MaterialShapes.Heart
        }
}

@Composable
fun AvatarShape.toShape(cornerPercent: Int): Shape =
    polygon?.toShape() ?: RoundedCornerShape(cornerPercent.coerceIn(0, 50))
