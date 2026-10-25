package com.pixelgram.desktop.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font

internal fun fontBytes(name: String): ByteArray? =
    Typography::class.java.getResourceAsStream("/fonts/$name.ttf")?.use { it.readBytes() }

val GoogleSans: FontFamily by lazy {
    FontFamily(
        listOf(
            FontWeight.Normal to "Regular",
            FontWeight.Medium to "Medium",
            FontWeight.SemiBold to "SemiBold",
            FontWeight.Bold to "Bold",
        ).mapNotNull { (weight, name) ->
            fontBytes("GoogleSans-$name")?.let { Font("GoogleSans-$name", it, weight) }
        }
    )
}

val PixelTypography: Typography by lazy {
    val base = Typography()
    Typography(
        displayLarge = base.displayLarge.copy(fontFamily = GoogleSans, fontWeight = FontWeight.SemiBold),
        displayMedium = base.displayMedium.copy(fontFamily = GoogleSans, fontWeight = FontWeight.SemiBold),
        displaySmall = base.displaySmall.copy(fontFamily = GoogleSans, fontWeight = FontWeight.SemiBold),
        headlineLarge = base.headlineLarge.copy(fontFamily = GoogleSans, fontWeight = FontWeight.Bold),
        headlineMedium = base.headlineMedium.copy(fontFamily = GoogleSans, fontWeight = FontWeight.Bold),
        headlineSmall = base.headlineSmall.copy(fontFamily = GoogleSans, fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontFamily = GoogleSans, fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(fontFamily = GoogleSans, fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.copy(fontFamily = GoogleSans, fontWeight = FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.copy(fontFamily = GoogleSans),
        bodyMedium = base.bodyMedium.copy(fontFamily = GoogleSans),
        bodySmall = base.bodySmall.copy(fontFamily = GoogleSans),
        labelLarge = base.labelLarge.copy(fontFamily = GoogleSans, fontWeight = FontWeight.SemiBold),
        labelMedium = base.labelMedium.copy(fontFamily = GoogleSans, fontWeight = FontWeight.SemiBold),
        labelSmall = base.labelSmall.copy(fontFamily = GoogleSans, fontWeight = FontWeight.SemiBold),
        displayLargeEmphasized = base.displayLargeEmphasized.copy(fontFamily = GoogleSans, fontWeight = FontWeight.Bold),
        displayMediumEmphasized = base.displayMediumEmphasized.copy(fontFamily = GoogleSans, fontWeight = FontWeight.Bold),
        displaySmallEmphasized = base.displaySmallEmphasized.copy(fontFamily = GoogleSans, fontWeight = FontWeight.Bold),
        headlineLargeEmphasized = base.headlineLargeEmphasized.copy(fontFamily = GoogleSans, fontWeight = FontWeight.Bold),
        headlineMediumEmphasized = base.headlineMediumEmphasized.copy(fontFamily = GoogleSans, fontWeight = FontWeight.Bold),
        headlineSmallEmphasized = base.headlineSmallEmphasized.copy(fontFamily = GoogleSans, fontWeight = FontWeight.Bold),
        titleLargeEmphasized = base.titleLargeEmphasized.copy(fontFamily = GoogleSans, fontWeight = FontWeight.Bold),
        titleMediumEmphasized = base.titleMediumEmphasized.copy(fontFamily = GoogleSans, fontWeight = FontWeight.Bold),
        titleSmallEmphasized = base.titleSmallEmphasized.copy(fontFamily = GoogleSans, fontWeight = FontWeight.Bold),
        bodyLargeEmphasized = base.bodyLargeEmphasized.copy(fontFamily = GoogleSans, fontWeight = FontWeight.Bold),
        bodyMediumEmphasized = base.bodyMediumEmphasized.copy(fontFamily = GoogleSans, fontWeight = FontWeight.Bold),
        bodySmallEmphasized = base.bodySmallEmphasized.copy(fontFamily = GoogleSans, fontWeight = FontWeight.Bold),
        labelLargeEmphasized = base.labelLargeEmphasized.copy(fontFamily = GoogleSans, fontWeight = FontWeight.Bold),
        labelMediumEmphasized = base.labelMediumEmphasized.copy(fontFamily = GoogleSans, fontWeight = FontWeight.Bold),
        labelSmallEmphasized = base.labelSmallEmphasized.copy(fontFamily = GoogleSans, fontWeight = FontWeight.Bold),
    )
}
