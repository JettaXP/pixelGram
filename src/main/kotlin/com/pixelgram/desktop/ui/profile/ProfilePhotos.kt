package com.pixelgram.desktop.ui.profile

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.main.ThumbImage
import it.tdlight.jni.TdApi

private val GallerySize = 420.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilePhotosDialog(telegram: Telegram, userId: Long, onChanged: () -> Unit, onDismiss: () -> Unit) {
    var photos by remember { mutableStateOf<List<TdApi.ChatPhoto>?>(null) }
    var index by remember { mutableIntStateOf(0) }
    var reload by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(userId, reload) { telegram.profilePhotos(userId) { photos = it } }
    val done = { failure: String? ->
        error = failure
        if (failure == null) {
            index = 0
            reload++
            onChanged()
        }
    }
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val list = photos
                Box(
                    Modifier.size(GallerySize).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        list == null -> LoadingIndicator()
                        list.isEmpty() -> Text("No photos yet")
                        else -> {
                            AnimatedContent(index.coerceIn(0, list.lastIndex), transitionSpec = { fadeIn() togetherWith fadeOut() }) { shown ->
                                val photo = list[shown]
                                ThumbImage(
                                    photo.sizes.maxByOrNull { it.width }?.photo,
                                    photo.minithumbnail,
                                    telegram,
                                    Modifier.size(GallerySize),
                                    ContentScale.Fit,
                                )
                            }
                            if (index > 0) {
                                FilledTonalIconButton(
                                    onClick = { index-- },
                                    modifier = Modifier.align(Alignment.CenterStart).padding(8.dp),
                                ) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous") }
                            }
                            if (index < list.lastIndex) {
                                FilledTonalIconButton(
                                    onClick = { index++ },
                                    modifier = Modifier.align(Alignment.CenterEnd).padding(8.dp),
                                ) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next") }
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.width(GallerySize)) {
                    Text(
                        list?.takeIf { it.isNotEmpty() }?.let { "${index + 1} of ${it.size}" }.orEmpty(),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f),
                    )
                    val current = list?.getOrNull(index)
                    if (current != null && index > 0) {
                        TextButton(onClick = { telegram.setPreviousProfilePhoto(current.id, done) }) {
                            Icon(Icons.Outlined.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Set as main", modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                    if (current != null) {
                        TextButton(onClick = { telegram.deleteProfilePhoto(current.id, done) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                            Text("Delete", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Close") }
            }
        }
    }
}
