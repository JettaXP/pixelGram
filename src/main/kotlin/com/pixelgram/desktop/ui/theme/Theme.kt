package com.pixelgram.desktop.ui.theme

import com.pixelgram.desktop.ui.main.Links
import com.pixelgram.desktop.core.PowerSaving
import com.pixelgram.desktop.core.InstantMotion
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.dynamiccolor.DynamicColor
import com.materialkolor.dynamiccolor.DynamicScheme
import com.materialkolor.dynamiccolor.MaterialDynamicColors
import com.materialkolor.hct.Hct
import com.materialkolor.scheme.SchemeTonalSpot
import com.pixelgram.desktop.core.AppPaths
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.core.ThemeMode

val SeedColors = listOf(
    0xFF6750A4.toInt(),
    0xFF0061A4.toInt(),
    0xFF006A6A.toInt(),
    0xFF386A20.toInt(),
    0xFF8B5000.toInt(),
    0xFFB3261E.toInt(),
    0xFF984061.toInt(),
    0xFF5B5F71.toInt(),
)

@Composable
fun isDarkTheme(settings: AppSettings): Boolean = if (settings.israelLogo.value) false else when (settings.themeMode.value) {
    ThemeMode.System -> isSystemInDarkTheme()
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}

@Composable
fun appColorScheme(settings: AppSettings): ColorScheme {
    if (settings.israelLogo.value) return remember { israelScheme() }
    val dark = isDarkTheme(settings)
    val seed = settings.seedColor.value
    val seedScheme = remember(seed, dark) { schemeFromSeed(seed, dark) }
    val matugenScheme = if (AppPaths.isLinux && settings.matugen.value) {
        rememberMatugenScheme()
    } else {
        null
    }
    return matugenScheme ?: seedScheme
}

@Composable
fun PixelTheme(settings: AppSettings, content: @Composable () -> Unit) {
    PowerSaving.settings = settings
    com.pixelgram.desktop.core.AudioDevices.settings = settings
    val corners = com.pixelgram.desktop.ui.components.cornerSpec(settings)
    MaterialExpressiveTheme(
        colorScheme = appColorScheme(settings),
        motionScheme = if (settings.interfaceAnimations.value) MotionScheme.expressive() else InstantMotion,
        typography = PixelTypography,
        shapes = com.pixelgram.desktop.ui.components.cornerShapes(corners),
    ) {
        CompositionLocalProvider(
            LocalUriHandler provides Links,
            com.pixelgram.desktop.ui.components.LocalCorners provides corners,
            content = content,
        )
    }
}

private val IsraelBlue = Color(0xFF0038B8)
private val IsraelPaper = Color(0xFFF7F9FF)

fun israelScheme(): ColorScheme = schemeFromSeed(IsraelBlue.toArgb(), false).copy(
    primary = IsraelBlue,
    onPrimary = Color.White,
    surfaceTint = IsraelBlue,
    background = Color.White,
    surface = Color.White,
    surfaceBright = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = IsraelPaper,
)

fun schemeFromSeed(seed: Int, dark: Boolean): ColorScheme {
    val scheme = SchemeTonalSpot(
        Hct.fromInt(seed),
        dark,
        0.0,
        ColorSpec.SpecVersion.SPEC_2021,
        DynamicScheme.Platform.PHONE,
    )
    val colors = MaterialDynamicColors()
    fun DynamicColor.color() = Color(getArgb(scheme))

    val roles = mapOf(
        "primary" to colors.primary.color(),
        "onPrimary" to colors.onPrimary.color(),
        "primaryContainer" to colors.primaryContainer.color(),
        "onPrimaryContainer" to colors.onPrimaryContainer.color(),
        "inversePrimary" to colors.inversePrimary.color(),
        "secondary" to colors.secondary.color(),
        "onSecondary" to colors.onSecondary.color(),
        "secondaryContainer" to colors.secondaryContainer.color(),
        "onSecondaryContainer" to colors.onSecondaryContainer.color(),
        "tertiary" to colors.tertiary.color(),
        "onTertiary" to colors.onTertiary.color(),
        "tertiaryContainer" to colors.tertiaryContainer.color(),
        "onTertiaryContainer" to colors.onTertiaryContainer.color(),
        "background" to colors.background.color(),
        "onBackground" to colors.onBackground.color(),
        "surface" to colors.surface.color(),
        "onSurface" to colors.onSurface.color(),
        "surfaceVariant" to colors.surfaceVariant.color(),
        "onSurfaceVariant" to colors.onSurfaceVariant.color(),
        "surfaceTint" to colors.surfaceTint.color(),
        "inverseSurface" to colors.inverseSurface.color(),
        "inverseOnSurface" to colors.inverseOnSurface.color(),
        "error" to colors.error.color(),
        "onError" to colors.onError.color(),
        "errorContainer" to colors.errorContainer.color(),
        "onErrorContainer" to colors.onErrorContainer.color(),
        "outline" to colors.outline.color(),
        "outlineVariant" to colors.outlineVariant.color(),
        "scrim" to colors.scrim.color(),
        "surfaceBright" to colors.surfaceBright.color(),
        "surfaceDim" to colors.surfaceDim.color(),
        "surfaceContainer" to colors.surfaceContainer.color(),
        "surfaceContainerHigh" to colors.surfaceContainerHigh.color(),
        "surfaceContainerHighest" to colors.surfaceContainerHighest.color(),
        "surfaceContainerLow" to colors.surfaceContainerLow.color(),
        "surfaceContainerLowest" to colors.surfaceContainerLowest.color(),
    )
    return colorSchemeOf(roles, dark)
}

fun colorSchemeOf(roles: Map<String, Color>, dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = roles["primary"] ?: base.primary,
        onPrimary = roles["onPrimary"] ?: base.onPrimary,
        primaryContainer = roles["primaryContainer"] ?: base.primaryContainer,
        onPrimaryContainer = roles["onPrimaryContainer"] ?: base.onPrimaryContainer,
        inversePrimary = roles["inversePrimary"] ?: base.inversePrimary,
        secondary = roles["secondary"] ?: base.secondary,
        onSecondary = roles["onSecondary"] ?: base.onSecondary,
        secondaryContainer = roles["secondaryContainer"] ?: base.secondaryContainer,
        onSecondaryContainer = roles["onSecondaryContainer"] ?: base.onSecondaryContainer,
        tertiary = roles["tertiary"] ?: base.tertiary,
        onTertiary = roles["onTertiary"] ?: base.onTertiary,
        tertiaryContainer = roles["tertiaryContainer"] ?: base.tertiaryContainer,
        onTertiaryContainer = roles["onTertiaryContainer"] ?: base.onTertiaryContainer,
        background = roles["background"] ?: base.background,
        onBackground = roles["onBackground"] ?: base.onBackground,
        surface = roles["surface"] ?: base.surface,
        onSurface = roles["onSurface"] ?: base.onSurface,
        surfaceVariant = roles["surfaceVariant"] ?: base.surfaceVariant,
        onSurfaceVariant = roles["onSurfaceVariant"] ?: base.onSurfaceVariant,
        surfaceTint = roles["surfaceTint"] ?: base.surfaceTint,
        inverseSurface = roles["inverseSurface"] ?: base.inverseSurface,
        inverseOnSurface = roles["inverseOnSurface"] ?: base.inverseOnSurface,
        error = roles["error"] ?: base.error,
        onError = roles["onError"] ?: base.onError,
        errorContainer = roles["errorContainer"] ?: base.errorContainer,
        onErrorContainer = roles["onErrorContainer"] ?: base.onErrorContainer,
        outline = roles["outline"] ?: base.outline,
        outlineVariant = roles["outlineVariant"] ?: base.outlineVariant,
        scrim = roles["scrim"] ?: base.scrim,
        surfaceBright = roles["surfaceBright"] ?: base.surfaceBright,
        surfaceContainer = roles["surfaceContainer"] ?: base.surfaceContainer,
        surfaceContainerHigh = roles["surfaceContainerHigh"] ?: base.surfaceContainerHigh,
        surfaceContainerHighest = roles["surfaceContainerHighest"] ?: base.surfaceContainerHighest,
        surfaceContainerLow = roles["surfaceContainerLow"] ?: base.surfaceContainerLow,
        surfaceContainerLowest = roles["surfaceContainerLowest"] ?: base.surfaceContainerLowest,
        surfaceDim = roles["surfaceDim"] ?: base.surfaceDim,
    )
}
