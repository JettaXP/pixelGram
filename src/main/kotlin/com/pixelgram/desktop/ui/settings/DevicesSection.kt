package com.pixelgram.desktop.ui.settings

import com.pixelgram.desktop.ui.components.groupShapes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.outlined.Check
import com.pixelgram.desktop.ui.icons.outlined.Headphones
import com.pixelgram.desktop.ui.icons.outlined.Mic
import com.pixelgram.desktop.ui.icons.outlined.Settings
import com.pixelgram.desktop.ui.icons.outlined.Videocam
import com.pixelgram.desktop.ui.icons.outlined.VideocamOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AppPaths
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.core.AudioDevices
import com.pixelgram.desktop.core.CameraSession
import com.pixelgram.desktop.core.MicMeter
import com.pixelgram.desktop.core.Setting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val DefaultDevice = "Default"
private const val MeterTicks = 36
private const val MeterSmoothing = 0.35f
private val CameraShape = RoundedCornerShape(20.dp)
private val PreviewMaxWidth = 520.dp

@Composable
fun DevicesSection(settings: AppSettings) {
    val outputs by produceState(emptyList<String>()) { value = withContext(Dispatchers.IO) { AudioDevices.outputs() } }
    val inputs by produceState(emptyList<String>()) { value = withContext(Dispatchers.IO) { AudioDevices.inputs() } }
    val cameras by produceState(emptyList<String>()) { value = withContext(Dispatchers.IO) { CameraSession.devices() } }

    Header("Speakers and headphones")
    SegmentedGroup {
        DeviceRow(Icons.Outlined.Headphones, "Output device", settings.outputDevice, outputs, 0, 1)
    }
    Header("Microphone")
    SegmentedGroup {
        DeviceRow(Icons.Outlined.Mic, "Input device", settings.inputDevice, inputs, 0, 2)
        Surface(shape = groupShapes(1, 2).shape, color = MaterialTheme.colorScheme.surfaceContainer) {
            MicLevel(settings.inputDevice.value, Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp))
        }
    }
    Header("Calls and video chats")
    SegmentedGroup {
        ToggleRow("Use the same devices for calls", settings.callsSameDevices, 0, 1)
    }
    Header("Camera")
    SegmentedGroup {
        DeviceRow(Icons.Outlined.Videocam, "Input device", settings.cameraDevice, cameras, 0, 1)
    }
    CameraPreview(settings.cameraDevice.value, Modifier.padding(top = 8.dp).widthIn(max = PreviewMaxWidth).fillMaxWidth())
    Header("Other settings")
    SegmentedGroup {
        ToggleRow("Accept calls on this device", settings.acceptCalls, 0, 2)
        SegmentedListItem(
            onClick = ::openSoundSettings,
            shapes = groupShapes(1, 2),
            leadingContent = { Icon(Icons.Outlined.Settings, contentDescription = null) },
        ) { Text("Open system sound preferences") }
    }
}

@Composable
private fun DeviceRow(icon: ImageVector, title: String, setting: Setting<String>, devices: List<String>, index: Int, count: Int) {
    var open by remember { mutableStateOf(false) }
    Box {
        SegmentedListItem(
            onClick = { open = true },
            shapes = groupShapes(index, count),
            leadingContent = { Icon(icon, contentDescription = null) },
            trailingContent = {
                val enter = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
                val exit = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
                AnimatedContent(
                    targetState = setting.value.ifEmpty { DefaultDevice },
                    transitionSpec = { fadeIn(enter) togetherWith fadeOut(exit) },
                ) { name ->
                    Text(
                        name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 220.dp),
                    )
                }
            },
        ) { Text(title) }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            (listOf("") + devices).forEach { device ->
                DropdownMenuItem(
                    text = { Text(device.ifEmpty { DefaultDevice }) },
                    trailingIcon = if (device == setting.value) {
                        { Icon(Icons.Outlined.Check, contentDescription = null) }
                    } else {
                        null
                    },
                    onClick = {
                        setting.value = device
                        open = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(title: String, setting: Setting<Boolean>, index: Int, count: Int) {
    SegmentedListItem(
        onClick = { setting.value = !setting.value },
        shapes = groupShapes(index, count),
        trailingContent = { PixelSwitch(checked = setting.value, onCheckedChange = { setting.value = it }) },
    ) { Text(title) }
}

@Composable
private fun MicLevel(device: String, modifier: Modifier) {
    var level by remember { mutableFloatStateOf(0f) }
    val meter = remember(device) { MicMeter.start() }
    DisposableEffect(meter) { onDispose { meter.stop() } }
    LaunchedEffect(meter) {
        while (true) {
            withFrameNanos { }
            level += (meter.level - level) * MeterSmoothing
        }
    }
    val lit = MaterialTheme.colorScheme.primary
    val dim = MaterialTheme.colorScheme.outlineVariant
    Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = modifier.height(24.dp)) {
        repeat(MeterTicks) { index ->
            val on = index < (level * MeterTicks).toInt()
            val color by animateColorAsState(if (on) lit else dim, MaterialTheme.motionScheme.fastEffectsSpec())
            val grow by animateFloatAsState(if (on) 1f else 0.7f, MaterialTheme.motionScheme.fastSpatialSpec())
            Box(Modifier.width(3.dp).fillMaxHeight(grow).clip(RoundedCornerShape(50)).background(color))
        }
    }
}

@Composable
private fun CameraPreview(device: String, modifier: Modifier) {
    var frame by remember(device) { mutableStateOf<ImageBitmap?>(null) }
    var failed by remember(device) { mutableStateOf(false) }
    DisposableEffect(device) {
        var session: CameraSession? = null
        val worker = Thread {
            session = CameraSession.open(device)
            val opened = session
            if (opened == null) {
                failed = true
            } else {
                runCatching { opened.run { image -> frame = image.toComposeImageBitmap() } }.onFailure { failed = true }
            }
        }.apply { isDaemon = true }
        worker.start()
        onDispose { session?.stop() }
    }
    Box(modifier.aspectRatio(16f / 9f).clip(CameraShape).background(Color.Black), contentAlignment = Alignment.Center) {
        val current = frame
        val enter = MaterialTheme.motionScheme.slowEffectsSpec<Float>()
        val exit = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
        AnimatedContent(
            targetState = when {
                current != null -> 2
                failed -> 1
                else -> 0
            },
            transitionSpec = { fadeIn(enter) togetherWith fadeOut(exit) },
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) { state ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when (state) {
                    2 -> frame?.let { Image(it, contentDescription = "Camera preview", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                    1 -> Icon(Icons.Outlined.VideocamOff, contentDescription = "No camera", tint = Color.White, modifier = Modifier.size(48.dp))
                    else -> LoadingIndicator()
                }
            }
        }
    }
}

private fun openSoundSettings() {
    val commands = when {
        AppPaths.isWindows -> listOf(listOf("cmd", "/c", "start", "ms-settings:sound"))
        AppPaths.isMac -> listOf(listOf("open", "x-apple.systempreferences:com.apple.preference.sound"))
        else -> listOf(listOf("pavucontrol"), listOf("gnome-control-center", "sound"), listOf("systemsettings", "kcm_pulseaudio"))
    }
    for (command in commands) {
        if (runCatching { ProcessBuilder(command).start() }.isSuccess) return
    }
}
