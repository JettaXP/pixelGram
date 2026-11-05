package com.pixelgram.desktop.ui.components

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkOut
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MotionScheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
object Motion {
    var scheme: MotionScheme = MotionScheme.expressive()

    fun <T> spatial(): FiniteAnimationSpec<T> = scheme.defaultSpatialSpec()
    fun <T> fastSpatial(): FiniteAnimationSpec<T> = scheme.fastSpatialSpec()
    fun <T> effects(): FiniteAnimationSpec<T> = scheme.defaultEffectsSpec()
    fun <T> fastEffects(): FiniteAnimationSpec<T> = scheme.fastEffectsSpec()

    fun appear(): EnterTransition = fadeIn(effects()) + expandIn(spatial())
    fun disappear(): ExitTransition = fadeOut(fastEffects()) + shrinkOut(fastSpatial())

    fun content(): ContentTransform =
        ContentTransform(
            fadeIn(effects()) + scaleIn(spatial(), initialScale = SwapScale),
            fadeOut(fastEffects()),
            sizeTransform = SizeTransform(sizeAnimationSpec = { _, _ -> spatial() }),
        )

    private const val SwapScale = 0.92f
}
