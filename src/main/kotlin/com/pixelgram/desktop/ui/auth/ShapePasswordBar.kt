package com.pixelgram.desktop.ui.auth

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ShapePasswordBar(
    busy: Boolean,
    error: String?,
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var value by remember { mutableStateOf(TextFieldValue("")) }
    var focused by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val appear = remember { Animatable(0f) }
    val shake = remember { Animatable(0f) }
    val density = LocalDensity.current
    val spatial = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    LaunchedEffect(Unit) {
        runCatching { focus.requestFocus() }
        appear.animateTo(1f, spatial)
    }
    LaunchedEffect(error) {
        if (error == null) return@LaunchedEffect
        value = TextFieldValue("")
        runCatching { focus.requestFocus() }
        val step = with(density) { ShakeStep.toPx() }
        for (k in ShakePattern) shake.animateTo(k * step, tween(ShakeFrameMs))
        shake.animateTo(0f, tween(ShakeFrameMs))
    }
    val submit = { if (value.text.isNotEmpty() && !busy) onSubmit(value.text) }
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = CircleShape,
        color = scheme.surfaceContainer,
        shadowElevation = 3.dp,
        modifier = modifier.graphicsLayer {
            alpha = appear.value
            scaleX = 0.9f + appear.value * 0.1f
            scaleY = 0.9f + appear.value * 0.1f
        },
    ) {
        Row(Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .weight(1f)
                    .height(BarHeight)
                    .offset { IntOffset(shake.value.roundToInt(), 0) }
                    .clip(CircleShape)
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.text.isEmpty()) {
                    Text(
                        if (error != null) "Incorrect password" else "Enter password",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (error != null) scheme.error else scheme.onSurfaceVariant,
                    )
                }
                PasswordChars(
                    length = value.text.length,
                    selection = value.selection,
                    cursorVisible = focused,
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                )
                CompositionLocalProvider(
                    LocalTextSelectionColors provides TextSelectionColors(Color.Transparent, Color.Transparent),
                ) {
                    BasicTextField(
                        value = value,
                        onValueChange = { value = it },
                        enabled = !busy,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.Transparent),
                        cursorBrush = SolidColor(Color.Transparent),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focus)
                            .onFocusChanged { focused = it.isFocused }
                            .onPreviewKeyEvent { event ->
                                when {
                                    event.type != KeyEventType.KeyDown -> false
                                    event.key == Key.Enter || event.key == Key.NumPadEnter -> {
                                        submit()
                                        true
                                    }
                                    event.key == Key.Escape -> {
                                        value = TextFieldValue("", TextRange.Zero)
                                        true
                                    }
                                    else -> false
                                }
                            },
                    )
                }
            }
            FilledIconButton(
                onClick = submit,
                enabled = !busy,
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = scheme.primary),
                modifier = Modifier.size(BarHeight),
            ) {
                if (busy) {
                    LoadingIndicator(Modifier.size(28.dp), color = scheme.onPrimary)
                } else {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Sign in")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PasswordChars(length: Int, selection: TextRange, cursorVisible: Boolean, modifier: Modifier) {
    val ids = remember { mutableStateListOf<Int>() }
    var nextId by remember { mutableIntStateOf(0) }
    while (ids.size < length) ids.add(nextId++)
    while (ids.size > length) ids.removeAt(ids.lastIndex)
    val scroll = rememberScrollState()
    LaunchedEffect(length, scroll.maxValue) { scroll.animateScrollTo(scroll.maxValue) }
    val scheme = MaterialTheme.colorScheme
    val shapes = remember {
        listOf(
            MaterialShapes.Clover4Leaf,
            MaterialShapes.Arrow,
            MaterialShapes.Pill,
            MaterialShapes.SoftBurst,
            MaterialShapes.Diamond,
            MaterialShapes.ClamShell,
            MaterialShapes.Pentagon,
        )
    }
    val cursorX by animateDpAsState(CharSize * selection.end, MaterialTheme.motionScheme.fastSpatialSpec())
    Box(modifier.horizontalScroll(scroll), contentAlignment = Alignment.CenterStart) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ids.forEachIndexed { index, id ->
                androidx.compose.runtime.key(id) {
                    val selected = index >= selection.min && index < selection.max
                    Box(
                        Modifier
                            .size(CharSize)
                            .background(if (selected) scheme.secondaryContainer else Color.Transparent),
                        contentAlignment = Alignment.Center,
                    ) {
                        PasswordShape(
                            shape = shapes[index % shapes.size].toShape(),
                            color = if (selected) scheme.onSecondaryContainer else null,
                        )
                    }
                }
            }
        }
        if (cursorVisible) {
            Box(
                Modifier
                    .offset(x = cursorX)
                    .width(2.dp)
                    .height(CharSize)
                    .background(scheme.primary),
            )
        }
    }
}

@Composable
private fun PasswordShape(shape: androidx.compose.ui.graphics.Shape, color: Color?) {
    val scheme = MaterialTheme.colorScheme
    val grow = remember { Animatable(0f) }
    val tint = remember { Animatable(scheme.primary) }
    val spatial = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    LaunchedEffect(Unit) { grow.animateTo(1f, spatial) }
    LaunchedEffect(Unit) { tint.animateTo(scheme.onSurface, tween(ColorFadeMs)) }
    Box(
        Modifier
            .size(ShapeSize)
            .graphicsLayer {
                alpha = (grow.value * 4f).coerceAtMost(1f)
                scaleX = 0.5f + grow.value / 2
                scaleY = 0.5f + grow.value / 2
            }
            .background(color ?: tint.value, shape),
    )
}

private val BarHeight = 48.dp
private val CharSize = 20.dp
private val ShapeSize = 18.dp
private val ShakeStep = 10.dp
private val ShakePattern = floatArrayOf(-1f, 1f, -0.8f, 0.8f, -0.4f, 0.4f)
private const val ShakeFrameMs = 50
private const val ColorFadeMs = 1000
