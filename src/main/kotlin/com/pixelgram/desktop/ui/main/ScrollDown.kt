package com.pixelgram.desktop.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.lazy.LazyListState
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch

private const val ShowAfterItems = 1
private const val SnapCloserItems = 20
private const val SnapToItem = 6

@Composable
fun ScrollDownButton(listState: LazyListState, modifier: Modifier = Modifier, forceVisible: Boolean = false, onLatest: (() -> Unit)? = null) {
    val away by remember(listState) { derivedStateOf { listState.firstVisibleItemIndex >= ShowAfterItems } }
    var dismissed by remember { mutableStateOf(false) }
    LaunchedEffect(away) { if (!away) dismissed = false }
    val visible = away && !dismissed
    val scope = rememberCoroutineScope()
    AnimatedVisibility(
        visible = visible || forceVisible,
        enter = scaleIn(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()),
        exit = scaleOut(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
        modifier = modifier,
    ) {
        SmallFloatingActionButton(
            onClick = {
                dismissed = true
                if (onLatest != null) return@SmallFloatingActionButton onLatest()
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

@Composable
fun UnreadJumpButton(count: Int, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onJump: () -> Unit, onReadAll: () -> Unit) {
    AnimatedVisibility(
        visible = count > 0,
        enter = scaleIn(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()),
        exit = scaleOut(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
    ) {
        androidx.compose.material3.BadgedBox(
            badge = { androidx.compose.material3.Badge { androidx.compose.material3.Text(if (count > MaxBadge) "$MaxBadge+" else count.toString()) } },
        ) {
            SmallFloatingActionButton(
                onClick = onJump,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                elevation = FloatingActionButtonDefaults.loweredElevation(),
                modifier = Modifier.pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) onReadAll()
                        }
                    }
                },
            ) {
                Icon(icon, contentDescription = label)
            }
        }
    }
}

private const val MaxBadge = 99
