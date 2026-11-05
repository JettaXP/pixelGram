package com.pixelgram.desktop.ui.components.expressive

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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

enum class DialogSlot { None, Confirm, Dismiss }

val LocalDialogSlot = staticCompositionLocalOf { DialogSlot.None }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
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
) = androidx.compose.material3.BasicAlertDialog(
    onDismissRequest = onDismissRequest,
    modifier = modifier.popIn(),
    properties = properties,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(DialogCorner),
        color = colors.surfaceContainerHigh,
        modifier = Modifier.widthIn(min = MinWidth, max = MaxWidth),
    ) {
        CompositionLocalProvider(LocalDialogSlot provides DialogSlot.None) {
            Column(Modifier.padding(DialogPadding)) {
                if (icon != null) {
                    Box(Modifier.align(Alignment.CenterHorizontally)) {
                        CompositionLocalProvider(LocalContentColor provides colors.secondary) { icon() }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                if (title != null) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = if (icon != null) Alignment.Center else Alignment.CenterStart) {
                        CompositionLocalProvider(LocalContentColor provides colors.onSurface) {
                            ProvideTextStyle(
                                MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
                                    textAlign = if (icon != null) TextAlign.Center else TextAlign.Start,
                                ),
                            ) { title() }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                if (text != null) {
                    Box(Modifier.weight(1f, fill = false).fillMaxWidth()) {
                        CompositionLocalProvider(LocalContentColor provides colors.onSurfaceVariant) {
                            ProvideTextStyle(MaterialTheme.typography.bodyMedium) { text() }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
                DialogButtonRow {
                    if (dismissButton != null) {
                        CompositionLocalProvider(LocalDialogSlot provides DialogSlot.Dismiss) { dismissButton() }
                    }
                    CompositionLocalProvider(LocalDialogSlot provides DialogSlot.Confirm) { confirmButton() }
                }
            }
        }
    }
}

@Composable
fun DialogActions(dismiss: (@Composable () -> Unit)? = null, confirm: @Composable () -> Unit) {
    DialogButtonRow {
        if (dismiss != null) CompositionLocalProvider(LocalDialogSlot provides DialogSlot.Dismiss) { dismiss() }
        CompositionLocalProvider(LocalDialogSlot provides DialogSlot.Confirm) { confirm() }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DialogButtonRow(content: @Composable () -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGap, Alignment.End),
        verticalArrangement = Arrangement.spacedBy(ButtonGap),
    ) { content() }
}

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
private val MinWidth = 280.dp
private val MaxWidth = 400.dp
private val DialogPadding = 24.dp
private val DialogCorner = 32.dp
private val ButtonGap = 8.dp
val DialogButtonHeight = 40.dp
val DialogButtonPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp)
