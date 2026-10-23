package com.pixelgram.desktop.ui.profile

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.main.PlayButton
import com.pixelgram.desktop.ui.main.ThumbImage
import com.pixelgram.desktop.ui.main.formatDuration
import com.pixelgram.desktop.ui.theme.EmojiText
import it.tdlight.jni.TdApi

private const val CollapsedTracks = 1

@Composable
fun ProfileMusic(telegram: Telegram, userId: Long, own: Boolean, modifier: Modifier = Modifier) {
    var tracks by remember(userId) { mutableStateOf<List<TdApi.Audio>>(emptyList()) }
    var expanded by remember(userId) { mutableStateOf(false) }
    val changes = if (own) telegram.myProfileAudios.toList() else emptyList()
    LaunchedEffect(userId, changes) { telegram.profileAudios(userId) { tracks = it } }
    if (tracks.isEmpty()) return
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth().animateContentSize(MaterialTheme.motionScheme.defaultSpatialSpec()),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Music", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f).padding(start = 8.dp))
                if (tracks.size > CollapsedTracks) {
                    TextButton(onClick = { expanded = !expanded }) {
                        Text(if (expanded) "Hide" else "Earlier · ${tracks.size - CollapsedTracks}")
                        Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null)
                    }
                }
            }
            val shown = if (expanded) tracks else tracks.take(CollapsedTracks)
            shown.forEachIndexed { index, track ->
                TrackRow(telegram, track, featured = index == 0, own = own)
            }
        }
    }
}

@Composable
private fun TrackRow(telegram: Telegram, track: TdApi.Audio, featured: Boolean, own: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (featured) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(8.dp),
    ) {
        Box(Modifier.size(if (featured) 56.dp else 44.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            val cover = track.albumCoverThumbnail
            if (cover != null) {
                ThumbImage(cover.file, track.albumCoverMinithumbnail, telegram, Modifier.size(if (featured) 56.dp else 44.dp))
            } else {
                Icon(Icons.Outlined.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Column(Modifier.weight(1f)) {
            EmojiText(
                track.title.ifBlank { track.fileName },
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            EmojiText(
                listOf(track.performer, formatDuration(track.duration)).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (own) {
            IconButton(onClick = { telegram.toggleProfileAudio(track) }) {
                Icon(Icons.Filled.Favorite, contentDescription = "Remove from profile", tint = MaterialTheme.colorScheme.primary)
            }
        }
        PlayButton(
            track.audio.id.toLong(),
            0L,
            listOf(track.performer, track.title).filter { it.isNotBlank() }.joinToString(" – ").ifBlank { track.fileName },
            track.audio,
            telegram,
            track,
        )
    }
}
