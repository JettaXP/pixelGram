package com.pixelgram.desktop.ui.components.expressive

import androidx.compose.animation.core.Animatable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

@Composable
fun Modifier.popIn(): Modifier {
    val scale = remember { Animatable(StartScale) }
    val alpha = remember { Animatable(0f) }
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    LaunchedEffect(Unit) {
        launch { scale.animateTo(1f, spatial) }
        alpha.animateTo(1f, effects)
    }
    return graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
        this.alpha = alpha.value
    }.then(this)
}

@Composable
fun AlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    properties: DialogProperties = DialogProperties(),
) = androidx.compose.material3.AlertDialog(
    onDismissRequest = onDismissRequest,
    confirmButton = confirmButton,
    modifier = modifier.popIn(),
    dismissButton = dismissButton,
    icon = icon,
    title = title,
    text = text,
    properties = properties,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasicAlertDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit,
) = androidx.compose.material3.BasicAlertDialog(
    onDismissRequest = onDismissRequest,
    modifier = modifier.popIn(),
    properties = properties,
    content = content,
)

private const val StartScale = 0.88f
