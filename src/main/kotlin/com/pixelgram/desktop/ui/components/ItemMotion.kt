package com.pixelgram.desktop.ui.components

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

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
