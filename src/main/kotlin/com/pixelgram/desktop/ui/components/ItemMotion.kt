package com.pixelgram.desktop.ui.components

import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorizedFiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

class ScrollAwarePlacement(
    private val scrolling: () -> Boolean,
    private val spec: FiniteAnimationSpec<IntOffset>,
) : FiniteAnimationSpec<IntOffset> {
    override fun <V : AnimationVector> vectorize(converter: TwoWayConverter<IntOffset, V>): VectorizedFiniteAnimationSpec<V> =
        if (scrolling()) snap<IntOffset>().vectorize(converter) else spec.vectorize(converter)
}

@Composable
fun rememberScrollAwarePlacement(scrolling: () -> Boolean): ScrollAwarePlacement {
    val spec = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
    val current = androidx.compose.runtime.rememberUpdatedState(scrolling)
    return androidx.compose.runtime.remember(spec) { ScrollAwarePlacement({ current.value() }, spec) }
}

@Composable
fun LazyItemScope.itemMotion(placement: ScrollAwarePlacement): Modifier = Modifier.animateItem(
    fadeInSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
    placementSpec = placement,
    fadeOutSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
)

@Composable
fun LazyItemScope.itemMotion(placement: Boolean = true): Modifier = Modifier.animateItem(
    fadeInSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
    placementSpec = if (placement) MaterialTheme.motionScheme.defaultSpatialSpec() else null,
    fadeOutSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
)

@Composable
fun LazyGridItemScope.itemMotion(): Modifier = Modifier.animateItem(
    fadeInSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
    placementSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
    fadeOutSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
)
