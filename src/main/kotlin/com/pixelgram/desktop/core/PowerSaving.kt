package com.pixelgram.desktop.core

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.material3.MotionScheme

object PowerSaving {
    var settings: AppSettings? = null

    val stickersInPanel: Boolean get() = settings?.stickersInPanel?.value ?: true
    val stickersInChat: Boolean get() = settings?.stickersInChat?.value ?: true
    val emojiInMessages: Boolean get() = settings?.emojiInMessages?.value ?: true
    val spoilerEffect: Boolean get() = settings?.spoilerEffect?.value ?: true
    val messageAnimations: Boolean get() = settings?.messageAnimations?.value ?: true
    val autoplayGifs: Boolean get() = settings?.autoplayGifs?.value ?: true
    val autoplayVideos: Boolean get() = settings?.autoplayVideos?.value ?: true
}

object InstantMotion : MotionScheme {
    override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> = snap()
    override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> = snap()
    override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> = snap()
    override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> = snap()
    override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> = snap()
    override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> = snap()
}
