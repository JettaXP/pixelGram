package com.pixelgram.desktop.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font

// Google Sans (SIL Open Font License, see fonts/GoogleSans-OFL.txt).
val GoogleSans: FontFamily by lazy {
    FontFamily(
        listOf(
            FontWeight.Normal to "Regular",
            FontWeight.Medium to "Medium",
            FontWeight.SemiBold to "SemiBold",
            FontWeight.Bold to "Bold",
        ).mapNotNull { (weight, name) ->
            val data = Typography::class.java.getResourceAsStream("/fonts/GoogleSans-$name.ttf")
                ?.use { it.readBytes() } ?: return@mapNotNull null
            Font("GoogleSans-$name", data, weight)
        }
    )
}

// Material 3 Expressive type scale with Google Sans.
val PixelTypography: Typography by lazy {
    val base = Typography()
    Typography(
        displayLarge = base.displayLarge.copy(fontFamily = GoogleSans),
        displayMedium = base.displayMedium.copy(fontFamily = GoogleSans),
        displaySmall = base.displaySmall.copy(fontFamily = GoogleSans),
        headlineLarge = base.headlineLarge.copy(fontFamily = GoogleSans),
        headlineMedium = base.headlineMedium.copy(fontFamily = GoogleSans),
        headlineSmall = base.headlineSmall.copy(fontFamily = GoogleSans),
        titleLarge = base.titleLarge.copy(fontFamily = GoogleSans),
        titleMedium = base.titleMedium.copy(fontFamily = GoogleSans),
        titleSmall = base.titleSmall.copy(fontFamily = GoogleSans),
        bodyLarge = base.bodyLarge.copy(fontFamily = GoogleSans),
        bodyMedium = base.bodyMedium.copy(fontFamily = GoogleSans),
        bodySmall = base.bodySmall.copy(fontFamily = GoogleSans),
        labelLarge = base.labelLarge.copy(fontFamily = GoogleSans),
        labelMedium = base.labelMedium.copy(fontFamily = GoogleSans),
        labelSmall = base.labelSmall.copy(fontFamily = GoogleSans),
        displayLargeEmphasized = base.displayLargeEmphasized.copy(fontFamily = GoogleSans),
        displayMediumEmphasized = base.displayMediumEmphasized.copy(fontFamily = GoogleSans),
        displaySmallEmphasized = base.displaySmallEmphasized.copy(fontFamily = GoogleSans),
        headlineLargeEmphasized = base.headlineLargeEmphasized.copy(fontFamily = GoogleSans),
        headlineMediumEmphasized = base.headlineMediumEmphasized.copy(fontFamily = GoogleSans),
        headlineSmallEmphasized = base.headlineSmallEmphasized.copy(fontFamily = GoogleSans),
        titleLargeEmphasized = base.titleLargeEmphasized.copy(fontFamily = GoogleSans),
        titleMediumEmphasized = base.titleMediumEmphasized.copy(fontFamily = GoogleSans),
        titleSmallEmphasized = base.titleSmallEmphasized.copy(fontFamily = GoogleSans),
        bodyLargeEmphasized = base.bodyLargeEmphasized.copy(fontFamily = GoogleSans),
        bodyMediumEmphasized = base.bodyMediumEmphasized.copy(fontFamily = GoogleSans),
        bodySmallEmphasized = base.bodySmallEmphasized.copy(fontFamily = GoogleSans),
        labelLargeEmphasized = base.labelLargeEmphasized.copy(fontFamily = GoogleSans),
        labelMediumEmphasized = base.labelMediumEmphasized.copy(fontFamily = GoogleSans),
        labelSmallEmphasized = base.labelSmallEmphasized.copy(fontFamily = GoogleSans),
    )
}
