package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.telegram.StoryAudience
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.telegram.postStory
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.components.expressive.AlertDialog
import com.pixelgram.desktop.ui.components.expressive.OutlinedButton
import com.pixelgram.desktop.ui.components.expressive.TextButton
import com.pixelgram.desktop.ui.components.loadImage
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.outlined.AddAPhoto
import com.pixelgram.desktop.ui.icons.outlined.Movie
import com.pixelgram.desktop.ui.settings.ConnectedChoice
import com.pixelgram.desktop.ui.settings.PixelSwitch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val VideoExtensions = setOf("mp4", "mov", "m4v", "webm", "mkv")
private val StoryHours = listOf(6, 12, 24, 48)
private const val DefaultStoryHours = 24

@Composable
fun StoryComposerDialog(telegram: Telegram, chatId: Long, own: Boolean, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var file by remember { mutableStateOf<File?>(null) }
    var preview by remember { mutableStateOf<ImageBitmap?>(null) }
    var caption by remember { mutableStateOf("") }
    var audience by remember { mutableStateOf(StoryAudience.Everyone) }
    var hours by remember { mutableStateOf(DefaultStoryHours) }
    var keep by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val picked = file
    val video = picked?.extension?.lowercase() in VideoExtensions
    LaunchedEffect(picked) {
        preview = null
        val chosen = picked ?: return@LaunchedEffect
        preview = withContext(Dispatchers.IO) {
            val path = if (video) Attachments.videoInfo(chosen).poster?.absolutePath else chosen.absolutePath
            path?.let { loadImage(it, StoryPreviewSide) }
        }
    }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("New story") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    onClick = { scope.launch { Attachments.pick().firstOrNull()?.let { file = it } } },
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth().height(StoryPreviewHeight),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val image = preview
                        if (image != null) {
                            Image(image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large))
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(if (video) Icons.Outlined.Movie else Icons.Outlined.AddAPhoto, contentDescription = null)
                                Text(picked?.name ?: "Choose a photo or video", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                if (picked != null) {
                    OutlinedButton(onClick = { scope.launch { Attachments.pick().firstOrNull()?.let { file = it } } }) { Text("Choose another file") }
                }
                OutlinedTextField(caption, { caption = it }, label = { Text("Caption") }, modifier = Modifier.fillMaxWidth())
                if (own) {
                    Text("Who can see it", style = MaterialTheme.typography.titleSmall)
                    ConnectedChoice(StoryAudience.entries.map { it to it.title }, audience, { audience = it })
                }
                Text("Disappears after", style = MaterialTheme.typography.titleSmall)
                ConnectedChoice(StoryHours.map { it to "$it h" }, hours, { hours = it })
                androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (own) "Keep on my profile" else "Keep on the channel page", modifier = Modifier.weight(1f))
                    PixelSwitch(checked = keep, onCheckedChange = { keep = it })
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val chosen = picked ?: return@TextButton
                    busy = true
                    error = null
                    scope.launch {
                        val duration = if (video) withContext(Dispatchers.IO) { Attachments.videoInfo(chosen).duration.toDouble() } else 0.0
                        telegram.postStory(chatId, chosen, video, duration, caption.trim(), audience, hours, keep) { failure ->
                            busy = false
                            if (failure == null) onDismiss() else error = failure
                        }
                    }
                },
                enabled = picked != null && !busy,
            ) { Text("Post") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") } },
    )
}

private const val StoryPreviewSide = 720
private val StoryPreviewHeight = 220.dp
