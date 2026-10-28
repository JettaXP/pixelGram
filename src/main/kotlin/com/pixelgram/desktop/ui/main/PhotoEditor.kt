package com.pixelgram.desktop.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.pixelgram.desktop.core.AppLog
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.OnEscape
import com.pixelgram.desktop.ui.components.GroupedMenu
import com.pixelgram.desktop.ui.components.MenuEntry
import com.pixelgram.desktop.ui.components.MenuGroups
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.components.expressive.AlertDialog
import com.pixelgram.desktop.ui.components.expressive.Button
import com.pixelgram.desktop.ui.components.expressive.FilledTonalButton
import com.pixelgram.desktop.ui.components.expressive.IconButton
import com.pixelgram.desktop.ui.components.expressive.Slider
import com.pixelgram.desktop.ui.components.expressive.TextButton
import com.pixelgram.desktop.ui.components.expressive.popIn
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.filled.Check
import com.pixelgram.desktop.ui.icons.outlined.ArrowForward
import com.pixelgram.desktop.ui.icons.outlined.ArrowOutward
import com.pixelgram.desktop.ui.icons.outlined.AspectRatio
import com.pixelgram.desktop.ui.icons.outlined.BlurOn
import com.pixelgram.desktop.ui.icons.outlined.Brush
import com.pixelgram.desktop.ui.icons.outlined.ChatBubble
import com.pixelgram.desktop.ui.icons.outlined.Circle
import com.pixelgram.desktop.ui.icons.outlined.EmojiEmotions
import com.pixelgram.desktop.ui.icons.outlined.Flip
import com.pixelgram.desktop.ui.icons.outlined.FormatColorFill
import com.pixelgram.desktop.ui.icons.outlined.InkEraser
import com.pixelgram.desktop.ui.icons.outlined.InkHighlighter
import com.pixelgram.desktop.ui.icons.outlined.InkPen
import com.pixelgram.desktop.ui.icons.outlined.LineWeight
import com.pixelgram.desktop.ui.icons.outlined.Rectangle
import com.pixelgram.desktop.ui.icons.outlined.Redo
import com.pixelgram.desktop.ui.icons.outlined.Rotate90DegreesCcw
import com.pixelgram.desktop.ui.icons.outlined.Shapes
import com.pixelgram.desktop.ui.icons.outlined.Star
import com.pixelgram.desktop.ui.icons.outlined.TextFields
import com.pixelgram.desktop.ui.icons.outlined.Undo
import it.tdlight.jni.TdApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import org.jetbrains.skia.Rect as SkRect

class EditRequest(val file: File, val telegram: Telegram, val chatId: Long, val onDone: (File) -> Unit)

class ModalHost {
    var editing by mutableStateOf<EditRequest?>(null)
}

val LocalModalHost = staticCompositionLocalOf<ModalHost?> { null }

@Composable
fun ModalBlurHost(content: @Composable () -> Unit) {
    val host = remember { ModalHost() }
    val open = host.editing != null
    val radius by animateDpAsState(if (open) BackdropBlur else 0.dp, MaterialTheme.motionScheme.defaultEffectsSpec())
    CompositionLocalProvider(LocalModalHost provides host) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().then(if (radius > 0.dp) Modifier.blur(radius) else Modifier)) { content() }
            AnimatedVisibility(
                visible = open,
                enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
                exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
            ) {
                var shown by remember { mutableStateOf(host.editing) }
                host.editing?.let { shown = it }
                shown?.let { request ->
                    key(request) {
                        PhotoEditor(request, onClose = { if (host.editing === request) host.editing = null })
                    }
                }
            }
        }
    }
}

private enum class EditorMode { Crop, Draw }

private class EditHistory {
    var state by mutableStateOf<EditState?>(null)
    val undo = mutableStateListOf<EditState>()
    val redo = mutableStateListOf<EditState>()
    private var gesture: EditState? = null

    fun reset(initial: EditState) {
        state = initial
        undo.clear()
        redo.clear()
    }

    fun commit(next: EditState) {
        val current = state ?: return
        if (next == current) return
        undo.add(current)
        redo.clear()
        state = next
    }

    fun begin() {
        gesture = state
    }

    fun live(next: EditState) {
        state = next
    }

    fun end() {
        val start = gesture ?: return
        gesture = null
        if (state != start) {
            undo.add(start)
            redo.clear()
        }
    }

    fun stepBack() {
        val previous = undo.removeLastOrNull() ?: return
        state?.let(redo::add)
        state = previous
    }

    fun stepForward() {
        val next = redo.removeLastOrNull() ?: return
        state?.let(undo::add)
        state = next
    }
}

private class ViewMap(val origin: Offset, val k: Float, val offset: Offset) {
    fun toView(point: Offset) = Offset(offset.x + (point.x - origin.x) * k, offset.y + (point.y - origin.y) * k)
    fun toImage(point: Offset) = Offset(origin.x + (point.x - offset.x) / k, origin.y + (point.y - offset.y) / k)
}

private sealed interface CropGrip {
    data class Corner(val index: Int) : CropGrip
    data class Edge(val side: Int) : CropGrip
    data object Move : CropGrip
}

private class TextTarget(val id: Long?, val text: String)

private val ToolIcons = mapOf(
    DrawTool.Pen to Icons.Outlined.InkPen,
    DrawTool.Arrow to Icons.Outlined.ArrowOutward,
    DrawTool.Marker to Icons.Outlined.InkHighlighter,
    DrawTool.Blur to Icons.Outlined.BlurOn,
    DrawTool.Eraser to Icons.Outlined.InkEraser,
)

private val ToolNames = mapOf(
    DrawTool.Pen to "Pen",
    DrawTool.Arrow to "Arrow",
    DrawTool.Marker to "Marker",
    DrawTool.Blur to "Blur",
    DrawTool.Eraser to "Eraser",
)

private val ShapeIcons = mapOf(
    ShapeKind.Circle to Icons.Outlined.Circle,
    ShapeKind.Rectangle to Icons.Outlined.Rectangle,
    ShapeKind.Star to Icons.Outlined.Star,
    ShapeKind.Bubble to Icons.Outlined.ChatBubble,
    ShapeKind.Arrow to Icons.Outlined.ArrowForward,
)

@Composable
private fun PhotoEditor(request: EditRequest, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val history = remember { EditHistory() }
    var images by remember { mutableStateOf<EditImages?>(null) }
    var failed by remember { mutableStateOf(false) }
    val shaper = remember { TextShaper() }
    val arts = remember { mutableStateMapOf<String, StickerArt?>() }
    LaunchedEffect(request) {
        val loaded = withContext(Dispatchers.IO) {
            runCatching {
                val bytes = request.file.readBytes()
                val size = uprightSize(bytes)
                val base = decodeUpright(bytes, PreviewSide)
                size to EditImages(base, blurredCopy(base))
            }.onFailure { AppLog.write("Photo editor load failed", it) }.getOrNull()
        }
        if (loaded == null) {
            failed = true
        } else {
            val (width, height) = loaded.first
            history.reset(EditState(width, height, Rect(0f, 0f, width.toFloat(), height.toFloat())))
            images = loaded.second
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            val dropped = images
            val held = arts.values.toList()
            java.awt.EventQueue.invokeLater {
                dropped?.base?.close()
                dropped?.blurred?.close()
                held.forEach { it?.close() }
                shaper.close()
            }
        }
    }
    val state = history.state
    LaunchedEffect(state?.items) {
        state?.items?.mapNotNull { it.sticker }?.filter { it.path !in arts }?.distinctBy { it.path }?.forEach { source ->
            arts[source.path] = null
            arts[source.path] = loadStickerArt(source)
        }
    }
    var mode by remember { mutableStateOf(EditorMode.Crop) }
    var tool by remember { mutableStateOf(DrawTool.Pen) }
    var color by remember { mutableStateOf(Color(DefaultColor)) }
    var brush by remember { mutableFloatStateOf(DefaultBrush) }
    var filled by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Long?>(null) }
    var ratioMenu by remember { mutableStateOf(false) }
    var shapesMenu by remember { mutableStateOf(false) }
    var stickers by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf(false) }
    var textTarget by remember { mutableStateOf<TextTarget?>(null) }
    var exporting by remember { mutableStateOf(false) }
    var nextId by remember { mutableLongStateOf(1L) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val selectedItem = state?.items?.firstOrNull { it.id == selected }
    val place = { item: Placed ->
        history.state?.let { current -> history.commit(current.copy(items = current.items + item)) }
        selected = item.id
        mode = EditorMode.Draw
    }
    val newCenter = { current: EditState ->
        val shift = (current.items.size % CascadeSteps) * minOf(current.crop.width, current.crop.height) * CascadeShift
        current.crop.center + Offset(shift, shift)
    }
    val addShape = { kind: ShapeKind ->
        history.state?.let { current ->
            place(Placed(nextId++, PlacedKind.Shape, newCenter(current), minOf(current.crop.width, current.crop.height) * ShapeShare, color = color.toArgb(), shape = kind, filled = filled))
        }
    }
    val addSticker = { sticker: TdApi.Sticker ->
        stickers = false
        scope.launch {
            val path = request.telegram.downloaded(sticker.sticker) ?: return@launch
            val kind = when (sticker.format) {
                is TdApi.StickerFormatTgs -> StickerKind.Lottie
                is TdApi.StickerFormatWebm -> StickerKind.Video
                else -> StickerKind.Still
            }
            val aspect = if (sticker.width > 0 && sticker.height > 0) sticker.width.toFloat() / sticker.height else 1f
            history.state?.let { current ->
                place(Placed(nextId++, PlacedKind.Sticker, newCenter(current), minOf(current.crop.width, current.crop.height) * StickerShare, sticker = StickerSource(path, kind, aspect)))
            }
        }
        Unit
    }
    val applyColor = { picked: Color ->
        color = picked
        val current = history.state
        val target = selected
        if (current != null && target != null) {
            history.commit(current.copy(items = current.items.map { if (it.id == target && it.kind != PlacedKind.Sticker) it.copy(color = picked.toArgb()) else it }))
        }
    }
    val deleteSelected = {
        val current = history.state
        val target = selected
        if (current != null && target != null) history.commit(current.copy(items = current.items.filterNot { it.id == target }))
        selected = null
    }
    val finish = {
        val current = history.state
        when {
            current == null || exporting -> Unit
            !current.edited -> onClose()
            else -> {
                exporting = true
                scope.launch {
                    val output = PhotoExport.render(request.file, current)
                    exporting = false
                    if (output != null) {
                        request.onDone(output)
                        onClose()
                    }
                }
            }
        }
        Unit
    }
    OnEscape {
        when {
            stickers -> stickers = false
            selected != null -> selected = null
            else -> onClose()
        }
        true
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = EditorScrim))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent(PointerEventPass.Final).changes.forEach { it.consume() }
                }
            }
            .focusRequester(focus)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val command = event.isCtrlPressed || event.isMetaPressed
                when {
                    command && event.key == Key.Z && event.isShiftPressed -> history.stepForward()
                    command && event.key == Key.Z -> history.stepBack()
                    command && event.key == Key.Y -> history.stepForward()
                    (event.key == Key.Delete || event.key == Key.Backspace) && selected != null -> deleteSelected()
                    else -> return@onPreviewKeyEvent false
                }
                true
            },
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainer,
            shadowElevation = 8.dp,
            modifier = Modifier
                .padding(EditorMargin)
                .widthIn(max = EditorMaxWidth)
                .fillMaxSize()
                .popIn(),
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().height(BarHeight).padding(horizontal = 16.dp),
                ) {
                    if (mode == EditorMode.Crop) {
                        IconButton(onClick = { history.state?.let { history.commit(it.rotated()) } }, enabled = state != null) {
                            Icon(Icons.Outlined.Rotate90DegreesCcw, contentDescription = "Rotate")
                        }
                        IconButton(onClick = { history.state?.let { history.commit(it.flipped()) } }, enabled = state != null) {
                            Icon(Icons.Outlined.Flip, contentDescription = "Mirror")
                        }
                        Box {
                            FilledTonalButton(onClick = { ratioMenu = true }, enabled = state != null) {
                                Icon(Icons.Outlined.AspectRatio, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(state?.ratio?.label ?: CropRatio.Free.label)
                            }
                            GroupedMenu(expanded = ratioMenu, onDismiss = { ratioMenu = false }) {
                                MenuGroups(
                                    listOf(
                                        CropRatio.entries.map { choice ->
                                            MenuEntry(
                                                icon = null,
                                                label = choice.label,
                                                trailing = if (state?.ratio == choice) Icons.Filled.Check else null,
                                            ) {
                                                ratioMenu = false
                                                history.state?.let { history.commit(it.withRatio(choice)) }
                                            }
                                        },
                                    ),
                                    width = RatioMenuWidth,
                                )
                            }
                        }
                    } else {
                        ConnectedToggles(
                            DrawTool.entries.map { entry ->
                                Toggle(ToolIcons.getValue(entry), ToolNames.getValue(entry), tool == entry) { tool = entry }
                            },
                        )
                        Spacer(Modifier.width(4.dp))
                        Box(
                            Modifier
                                .size(SwatchSize)
                                .clip(CircleShape)
                                .background(color)
                                .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .pointerInput(Unit) {
                                    awaitEachGesture {
                                        awaitFirstDown()
                                        picking = true
                                    }
                                },
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Outlined.LineWeight, contentDescription = "Brush size", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Slider(value = brush, onValueChange = { brush = it }, valueRange = MinBrush..MaxBrush, modifier = Modifier.width(SliderWidth))
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { history.stepBack() }, enabled = history.undo.isNotEmpty()) {
                        Icon(Icons.Outlined.Undo, contentDescription = "Undo")
                    }
                    IconButton(onClick = { history.stepForward() }, enabled = history.redo.isNotEmpty()) {
                        Icon(Icons.Outlined.Redo, contentDescription = "Redo")
                    }
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(CanvasCorner))
                        .background(Color(CanvasBackdrop)),
                ) {
                    val loadedImages = images
                    when {
                        failed -> Text("Couldn't open this photo", color = Color.White.copy(alpha = 0.7f))
                        state == null || loadedImages == null -> LoadingIndicator()
                        else -> EditorCanvas(
                            history = history,
                            images = loadedImages,
                            shaper = shaper,
                            arts = arts,
                            mode = mode,
                            tool = tool,
                            color = color,
                            brush = brush,
                            selected = selectedItem,
                            onSelect = { selected = it },
                            onEditText = { item -> textTarget = TextTarget(item.id, item.text) },
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().height(BottomHeight).padding(horizontal = 16.dp),
                ) {
                    TextButton(onClick = onClose) { Text("Cancel") }
                    Spacer(Modifier.weight(1f))
                    Box {
                        ConnectedToggles(
                            listOf(
                                Toggle(Icons.Outlined.Brush, "Draw", mode == EditorMode.Draw) {
                                    mode = if (mode == EditorMode.Draw) EditorMode.Crop else EditorMode.Draw
                                    selected = null
                                },
                                Toggle(Icons.Outlined.EmojiEmotions, "Sticker", stickers) { stickers = !stickers },
                                Toggle(Icons.Outlined.TextFields, "Text", textTarget != null) { textTarget = TextTarget(null, "") },
                                Toggle(Icons.Outlined.Shapes, "Shapes", shapesMenu) { shapesMenu = true },
                            ),
                            large = true,
                        )
                        if (stickers) {
                            Popup(
                                alignment = Alignment.BottomCenter,
                                offset = with(LocalDensity.current) { IntOffset(0, -(BottomHeight - 8.dp).roundToPx()) },
                                onDismissRequest = { stickers = false },
                                properties = PopupProperties(focusable = true),
                            ) {
                                StickerPicker(request.telegram, request.chatId, onPick = addSticker)
                            }
                        }
                        Box(Modifier.align(Alignment.TopEnd)) {
                            GroupedMenu(expanded = shapesMenu, onDismiss = { shapesMenu = false }) {
                                MenuGroups(
                                    listOf(
                                        ShapeKind.entries.map { kind ->
                                            MenuEntry(ShapeIcons.getValue(kind), kind.label) {
                                                shapesMenu = false
                                                addShape(kind)
                                            }
                                        },
                                        listOf(
                                            MenuEntry(Icons.Outlined.FormatColorFill, "Filled", trailing = if (filled) Icons.Filled.Check else null) {
                                                filled = !filled
                                                val current = history.state
                                                val target = selectedItem
                                                if (current != null && target?.kind == PlacedKind.Shape) {
                                                    history.commit(current.copy(items = current.items.map { if (it.id == target.id) it.copy(filled = filled) else it }))
                                                }
                                            },
                                        ),
                                    ),
                                    width = ShapesMenuWidth,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Button(onClick = finish, enabled = state != null && !exporting) {
                        if (exporting) {
                            LoadingIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("Done")
                        }
                    }
                }
            }
        }
    }
    if (picking) {
        ColorPickerDialog(initial = color, onDismiss = { picking = false }) {
            picking = false
            applyColor(it)
        }
    }
    textTarget?.let { target ->
        var value by remember(target) { mutableStateOf(target.text) }
        val textFocus = remember(target) { FocusRequester() }
        LaunchedEffect(target) { runCatching { textFocus.requestFocus() } }
        val confirm = {
            textTarget = null
            val current = history.state
            if (current != null) {
                val trimmed = value.trim()
                when {
                    target.id == null && trimmed.isNotEmpty() -> place(
                        Placed(nextId++, PlacedKind.Text, newCenter(current), minOf(current.crop.width, current.crop.height) * TextShare, color = color.toArgb(), text = trimmed),
                    )
                    target.id != null && trimmed.isEmpty() -> history.commit(current.copy(items = current.items.filterNot { it.id == target.id }))
                    target.id != null -> history.commit(current.copy(items = current.items.map { if (it.id == target.id) it.copy(text = trimmed) else it }))
                }
            }
        }
        AlertDialog(
            onDismissRequest = { textTarget = null },
            title = { Text(if (target.id == null) "Add text" else "Edit text") },
            text = {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    placeholder = { Text("Text") },
                    modifier = Modifier.fillMaxWidth().focusRequester(textFocus),
                )
            },
            confirmButton = { Button(onClick = confirm) { Text("Done") } },
            dismissButton = { TextButton(onClick = { textTarget = null }) { Text("Cancel") } },
        )
    }
}

private class Toggle(val icon: ImageVector, val label: String, val checked: Boolean, val onClick: () -> Unit)

@Composable
private fun ConnectedToggles(toggles: List<Toggle>, large: Boolean = false) {
    Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
        toggles.forEachIndexed { index, toggle ->
            val shapes: ToggleButtonShapes = when (index) {
                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                toggles.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
            }
            ToggleButton(
                checked = toggle.checked,
                onCheckedChange = { toggle.onClick() },
                shapes = shapes,
                colors = androidx.compose.material3.ToggleButtonDefaults.toggleButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = if (large) 20.dp else 14.dp),
                modifier = Modifier.height(if (large) LargeToggle else SmallToggle),
            ) {
                Icon(toggle.icon, contentDescription = toggle.label, modifier = Modifier.size(if (large) 24.dp else 20.dp))
            }
        }
    }
}

@Composable
private fun EditorCanvas(
    history: EditHistory,
    images: EditImages,
    shaper: TextShaper,
    arts: Map<String, StickerArt?>,
    mode: EditorMode,
    tool: DrawTool,
    color: Color,
    brush: Float,
    selected: Placed?,
    onSelect: (Long?) -> Unit,
    onEditText: (Placed) -> Unit,
) {
    val state = history.state ?: return
    var liveStroke by remember { mutableStateOf<EditStroke?>(null) }
    val animated = state.animated
    val time by produceState(0L, animated) {
        if (!animated) return@produceState
        val start = withFrameMillis { it }
        while (true) withFrameMillis { value = it - start }
    }
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val pad = with(density) { (if (mode == EditorMode.Crop) CropPadding else DrawPadding).toPx() }
        val region = if (mode == EditorMode.Crop) state.bounds else state.crop
        val viewW = constraints.maxWidth.toFloat()
        val viewH = constraints.maxHeight.toFloat()
        val k = minOf((viewW - pad * 2) / region.width, (viewH - pad * 2) / region.height).coerceAtLeast(MinZoom)
        val map = ViewMap(region.topLeft, k, Offset((viewW - region.width * k) / 2, (viewH - region.height * k) / 2))
        val mapNow by rememberUpdatedState(map)
        val modeNow by rememberUpdatedState(mode)
        val toolNow by rememberUpdatedState(tool)
        val colorNow by rememberUpdatedState(color)
        val brushNow by rememberUpdatedState(brush)
        val selectedNow by rememberUpdatedState(selected)
        val touch = with(density) { TouchRadius.toPx() }
        val handleGap = with(density) { HandleGap.toPx() }
        val minCrop = with(density) { MinCropSide.toPx() }
        val brushPx = with(density) { 1.dp.toPx() }
        var lastTap by remember { mutableStateOf(0L to 0L) }
        Canvas(
            Modifier.fillMaxSize().pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val current = history.state ?: return@awaitEachGesture
                    val view = mapNow
                    if (modeNow == EditorMode.Crop) {
                        val crop = current.crop
                        val corners = listOf(crop.topLeft, crop.topRight, crop.bottomLeft, crop.bottomRight).map(view::toView)
                        val cornerHit = corners.indexOfFirst { distance(it, down.position) <= touch }
                        val cropView = Rect(view.toView(crop.topLeft), view.toView(crop.bottomRight))
                        val free = current.ratio == CropRatio.Free
                        val edgeHit = when {
                            !free -> -1
                            abs(down.position.x - cropView.left) <= touch && down.position.y in cropView.top..cropView.bottom -> 0
                            abs(down.position.y - cropView.top) <= touch && down.position.x in cropView.left..cropView.right -> 1
                            abs(down.position.x - cropView.right) <= touch && down.position.y in cropView.top..cropView.bottom -> 2
                            abs(down.position.y - cropView.bottom) <= touch && down.position.x in cropView.left..cropView.right -> 3
                            else -> -1
                        }
                        val grip = when {
                            cornerHit >= 0 -> CropGrip.Corner(cornerHit)
                            edgeHit >= 0 -> CropGrip.Edge(edgeHit)
                            cropView.contains(down.position) -> CropGrip.Move
                            else -> return@awaitEachGesture
                        }
                        history.begin()
                        val minSide = minCrop / view.k
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            change.consume()
                            val delta = (change.position - down.position) / view.k
                            val latest = history.state ?: break
                            history.live(latest.copy(crop = dragCrop(current, crop, grip, delta, minSide)))
                        }
                        history.end()
                        return@awaitEachGesture
                    }
                    val point = view.toImage(down.position)
                    val chosen = selectedNow?.let { item -> current.items.firstOrNull { it.id == item.id } }
                    if (chosen != null) {
                        val size = chosen.intrinsic(shaper)
                        val gap = handleGap / view.k / chosen.scale
                        val rotateAt = view.toView(chosen.toImage(Offset(size.width / 2 + gap, size.height / 2 + gap)))
                        val removeAt = view.toView(chosen.toImage(Offset(-size.width / 2 - gap, -size.height / 2 - gap)))
                        if (distance(removeAt, down.position) <= touch) {
                            down.consume()
                            history.commit(current.copy(items = current.items.filterNot { it.id == chosen.id }))
                            onSelect(null)
                            return@awaitEachGesture
                        }
                        if (distance(rotateAt, down.position) <= touch) {
                            down.consume()
                            history.begin()
                            val centerView = view.toView(chosen.center)
                            val startDistance = distance(centerView, down.position).coerceAtLeast(1f)
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) break
                                change.consume()
                                val scale = (chosen.scale * distance(centerView, change.position) / startDistance).coerceIn(MinItemScale, MaxItemScale)
                                val angle = chosen.angle + angleBetween(centerView, down.position, change.position)
                                val latest = history.state ?: break
                                history.live(latest.copy(items = latest.items.map { if (it.id == chosen.id) it.copy(scale = scale, angle = angle) else it }))
                            }
                            history.end()
                            return@awaitEachGesture
                        }
                    }
                    val slop = touch / view.k
                    val hit = current.items.lastOrNull { item ->
                        val local = item.toLocal(point)
                        val size = item.intrinsic(shaper)
                        abs(local.x) <= size.width / 2 + slop / item.scale && abs(local.y) <= size.height / 2 + slop / item.scale
                    }
                    if (hit != null) {
                        down.consume()
                        val now = System.currentTimeMillis()
                        val (lastId, lastAt) = lastTap
                        lastTap = hit.id to now
                        if (hit.kind == PlacedKind.Text && lastId == hit.id && now - lastAt < DoubleTapMs) {
                            onEditText(hit)
                            return@awaitEachGesture
                        }
                        onSelect(hit.id)
                        history.begin()
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            change.consume()
                            val delta = (change.position - down.position) / view.k
                            val latest = history.state ?: break
                            history.live(latest.copy(items = latest.items.map { if (it.id == hit.id) it.copy(center = hit.center + delta) else it }))
                        }
                        history.end()
                        return@awaitEachGesture
                    }
                    onSelect(null)
                    val width = brushNow * brushPx / view.k
                    val points = mutableListOf(point)
                    val drawing = toolNow
                    val ink = colorNow.toArgb()
                    liveStroke = EditStroke(drawing, ink, width, points.toList())
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        change.consume()
                        val next = view.toImage(change.position)
                        if (distance(next, points.last()) * view.k >= MinStep) {
                            points += next
                            liveStroke = EditStroke(drawing, ink, width, points.toList())
                        }
                    }
                    liveStroke = null
                    history.state?.let { latest -> history.commit(latest.copy(strokes = latest.strokes + EditStroke(drawing, ink, width, points.toList()))) }
                }
            },
        ) {
            time
            arts.size
            val stroke = liveStroke
            drawIntoCanvas { canvas ->
                val native = canvas.nativeCanvas
                native.save()
                native.translate(map.offset.x, map.offset.y)
                native.scale(map.k, map.k)
                native.translate(-region.left, -region.top)
                if (mode == EditorMode.Draw) native.clipRect(SkRect.makeLTRB(region.left, region.top, region.right, region.bottom))
                drawEdit(native, state, images, shaper, { arts[it.path] }, time, stroke)
                native.restore()
            }
            if (mode == EditorMode.Crop) {
                val full = Rect(map.toView(state.bounds.topLeft), map.toView(state.bounds.bottomRight))
                val cropView = Rect(map.toView(state.crop.topLeft), map.toView(state.crop.bottomRight))
                clipRect(cropView.left, cropView.top, cropView.right, cropView.bottom, ClipOp.Difference) {
                    drawRect(Color.Black.copy(alpha = DimAlpha), full.topLeft, full.size)
                }
                val line = 1.dp.toPx()
                for (third in 1..2) {
                    val x = cropView.left + cropView.width * third / 3
                    val y = cropView.top + cropView.height * third / 3
                    drawLine(Color.White.copy(alpha = GridAlpha), Offset(x, cropView.top), Offset(x, cropView.bottom), line)
                    drawLine(Color.White.copy(alpha = GridAlpha), Offset(cropView.left, y), Offset(cropView.right, y), line)
                }
                drawRect(Color.White, cropView.topLeft, cropView.size, style = Stroke(1.5.dp.toPx()))
                val arm = CornerArm.toPx()
                val thick = CornerThickness.toPx()
                listOf(
                    Triple(cropView.topLeft, 1f, 1f),
                    Triple(cropView.topRight, -1f, 1f),
                    Triple(cropView.bottomLeft, 1f, -1f),
                    Triple(cropView.bottomRight, -1f, -1f),
                ).forEach { (corner, dx, dy) ->
                    val inset = Offset(-dx * thick / 2, -dy * thick / 2)
                    drawLine(Color.White, corner + inset, corner + inset + Offset(dx * arm, 0f), thick)
                    drawLine(Color.White, corner + inset, corner + inset + Offset(0f, dy * arm), thick)
                }
            } else if (selected != null) {
                val item = state.items.firstOrNull { it.id == selected.id }
                if (item != null) {
                    val size = item.intrinsic(shaper)
                    val gap = handleGap / map.k / item.scale
                    val halfW = size.width / 2 + gap
                    val halfH = size.height / 2 + gap
                    val outline = listOf(Offset(-halfW, -halfH), Offset(halfW, -halfH), Offset(halfW, halfH), Offset(-halfW, halfH)).map { map.toView(item.toImage(it)) }
                    val path = Path().apply {
                        moveTo(outline[0].x, outline[0].y)
                        outline.drop(1).forEach { lineTo(it.x, it.y) }
                        close()
                    }
                    val dash = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))
                    drawPath(path, Color.Black.copy(alpha = OutlineShade), style = Stroke(3.dp.toPx()))
                    drawPath(path, Color.White, style = Stroke(1.5.dp.toPx(), pathEffect = dash))
                    val radius = HandleRadius.toPx()
                    drawCircle(Color.White, radius, outline[2])
                    drawCircle(Color.Black.copy(alpha = OutlineShade), radius, outline[2], style = Stroke(1.dp.toPx()))
                    drawCircle(Color(RemoveColor), radius, outline[0])
                    val cross = radius * CrossShare
                    drawLine(Color.White, outline[0] + Offset(-cross, -cross), outline[0] + Offset(cross, cross), 2.dp.toPx())
                    drawLine(Color.White, outline[0] + Offset(cross, -cross), outline[0] + Offset(-cross, cross), 2.dp.toPx())
                }
            }
        }
    }
}

private fun dragCrop(state: EditState, start: Rect, grip: CropGrip, delta: Offset, minSide: Float): Rect {
    val maxW = state.orientedWidth.toFloat()
    val maxH = state.orientedHeight.toFloat()
    return when (grip) {
        CropGrip.Move -> {
            val dx = delta.x.coerceIn(-start.left, maxW - start.right)
            val dy = delta.y.coerceIn(-start.top, maxH - start.bottom)
            start.translate(dx, dy)
        }
        is CropGrip.Edge -> when (grip.side) {
            0 -> start.copy(left = (start.left + delta.x).coerceIn(0f, start.right - minSide))
            1 -> start.copy(top = (start.top + delta.y).coerceIn(0f, start.bottom - minSide))
            2 -> start.copy(right = (start.right + delta.x).coerceIn(start.left + minSide, maxW))
            else -> start.copy(bottom = (start.bottom + delta.y).coerceIn(start.top + minSide, maxH))
        }
        is CropGrip.Corner -> {
            val dirX = if (grip.index % 2 == 0) -1f else 1f
            val dirY = if (grip.index < 2) -1f else 1f
            val anchor = Offset(if (dirX < 0) start.right else start.left, if (dirY < 0) start.bottom else start.top)
            val moving = Offset(if (dirX < 0) start.left else start.right, if (dirY < 0) start.top else start.bottom) + delta
            val roomW = if (dirX < 0) anchor.x else maxW - anchor.x
            val roomH = if (dirY < 0) anchor.y else maxH - anchor.y
            var width = ((moving.x - anchor.x) * dirX).coerceIn(minSide, roomW)
            var height = ((moving.y - anchor.y) * dirY).coerceIn(minSide, roomH)
            val ratio = state.ratioValue()
            if (ratio != null && state.ratio != CropRatio.Free) {
                width = maxOf(width, height * ratio)
                height = width / ratio
                if (width > roomW) {
                    width = roomW
                    height = width / ratio
                }
                if (height > roomH) {
                    height = roomH
                    width = height * ratio
                }
            }
            val left = if (dirX < 0) anchor.x - width else anchor.x
            val top = if (dirY < 0) anchor.y - height else anchor.y
            Rect(left, top, left + width, top + height)
        }
    }
}

private val BackdropBlur = 18.dp
private val EditorMargin = 28.dp
private val EditorMaxWidth = 1180.dp
private val BarHeight = 64.dp
private val BottomHeight = 76.dp
private val CanvasCorner = 20.dp
private val CropPadding = 28.dp
private val DrawPadding = 12.dp
private val TouchRadius = 18.dp
private val HandleGap = 10.dp
private val HandleRadius = 10.dp
private val MinCropSide = 48.dp
private val CornerArm = 18.dp
private val CornerThickness = 4.dp
private val SwatchSize = 32.dp
private val SliderWidth = 150.dp
private val RatioMenuWidth = 200.dp
private val ShapesMenuWidth = 220.dp
private val SmallToggle = 40.dp
private val LargeToggle = 48.dp
private const val EditorScrim = 0.32f
private const val PreviewSide = 2048
private const val DefaultColor = 0xFFF44336
private const val DefaultBrush = 8f
private const val MinBrush = 2f
private const val MaxBrush = 48f
private const val ShapeShare = 0.35f
private const val StickerShare = 0.4f
private const val TextShare = 0.08f
private const val CascadeSteps = 5
private const val CascadeShift = 0.04f
private const val MinZoom = 0.0001f
private const val MinStep = 1.5f
private const val MinItemScale = 0.1f
private const val MaxItemScale = 12f
private const val DoubleTapMs = 400L
private const val DimAlpha = 0.55f
private const val GridAlpha = 0.45f
private const val OutlineShade = 0.35f
private const val CrossShare = 0.4f
private const val RemoveColor = 0xFFE53935
private const val CanvasBackdrop = 0xFF16161A
