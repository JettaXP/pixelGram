package com.pixelgram.desktop.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch

private const val ShowAfterItems = 3
private const val SnapCloserItems = 20
private const val SnapToItem = 6

@Composable
fun ScrollDownButton(listState: LazyListState, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(listState) {
        var last = 0L
        snapshotFlow { listState.firstVisibleItemIndex.toLong() * Int.MAX_VALUE + listState.firstVisibleItemScrollOffset }
            .collect { position ->
                visible = when {
                    listState.firstVisibleItemIndex < ShowAfterItems -> false
                    position < last -> true
                    position > last -> false
                    else -> visible
                }
                last = position
            }
    }
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()),
        exit = scaleOut(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
        modifier = modifier,
    ) {
        SmallFloatingActionButton(
            onClick = {
                visible = false
                scope.launch {
                    if (listState.firstVisibleItemIndex > SnapCloserItems) listState.scrollToItem(SnapToItem)
                    listState.animateScrollToItem(0)
                }
            },
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            elevation = FloatingActionButtonDefaults.loweredElevation(),
        ) {
            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Scroll to the newest message")
        }
    }
}
