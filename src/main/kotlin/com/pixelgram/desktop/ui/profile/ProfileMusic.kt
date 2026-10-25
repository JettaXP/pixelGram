package com.pixelgram.desktop.ui.profile

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.automirrored.outlined.Forward
import com.pixelgram.desktop.ui.icons.filled.Favorite
import com.pixelgram.desktop.ui.icons.outlined.Delete
import com.pixelgram.desktop.ui.icons.outlined.Download
import com.pixelgram.desktop.ui.icons.outlined.DragHandle
import com.pixelgram.desktop.ui.icons.outlined.ExpandLess
import com.pixelgram.desktop.ui.icons.outlined.ExpandMore
import com.pixelgram.desktop.ui.icons.outlined.FavoriteBorder
import com.pixelgram.desktop.ui.icons.outlined.MusicNote
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import com.pixelgram.desktop.ui.components.expressive.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.components.expressive.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.main.Attachments
import com.pixelgram.desktop.ui.main.AudioQueue
import com.pixelgram.desktop.ui.main.ForwardDialog
import com.pixelgram.desktop.ui.main.PlayButton
import com.pixelgram.desktop.ui.main.ThumbImage
import com.pixelgram.desktop.ui.main.formatDuration
import com.pixelgram.desktop.ui.theme.EmojiText
import it.tdlight.jni.TdApi
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val CollapsedTracks = 1
private val TrackGap = 8.dp

@Composable
fun ProfileMusic(telegram: Telegram, settings: AppSettings, userId: Long, own: Boolean, modifier: Modifier = Modifier) {
    val mine = own || userId == telegram.me?.id
    var tracks by remember(userId) { mutableStateOf<List<TdApi.Audio>>(emptyList()) }
    var expanded by remember(userId) { mutableStateOf(false) }
    var forwarding by remember { mutableStateOf<TdApi.Audio?>(null) }
    val changes = if (mine) telegram.myProfileAudios.toList() else emptyList()
    LaunchedEffect(userId, changes) { telegram.profileAudios(userId) { tracks = it } }
    LaunchedEffect(telegram) { telegram.loadMyProfileAudios() }
    SideEffect { if (tracks.isNotEmpty()) AudioQueue.playlist = tracks }
    if (tracks.isEmpty()) return
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth().animateContentSize(MaterialTheme.motionScheme.defaultSpatialSpec()),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(TrackGap)) {
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
            val reorder = mine && expanded && shown.size > 1
            var dragged by remember { mutableIntStateOf(-1) }
            var dragOffset by remember { mutableFloatStateOf(0f) }
            var rowHeight by remember { mutableIntStateOf(1) }
            val gap = with(LocalDensity.current) { TrackGap.toPx() }
            shown.forEachIndexed { index, track ->
                val moving = index == dragged
                TrackRow(
                    telegram,
                    track,
                    featured = index == 0,
                    own = mine,
                    onForward = { forwarding = track },
                    handle = if (reorder) {
                        Modifier.pointerInput(track.audio.id, shown.size) {
                            detectDragGestures(
                                onDragStart = {
                                    dragged = index
                                    dragOffset = 0f
                                },
                                onDragEnd = {
                                    val step = rowHeight + gap
                                    val target = (dragged + (dragOffset / step).roundToInt()).coerceIn(0, shown.lastIndex)
                                    if (target != dragged) {
                                        val reordered = shown.toMutableList().apply { add(target, removeAt(dragged)) }
                                        tracks = reordered
                                        val after = reordered.getOrNull(target - 1)?.audio?.id ?: 0
                                        telegram.moveProfileAudio(track.audio.id, after)
                                    }
                                    dragged = -1
                                    dragOffset = 0f
                                },
                                onDragCancel = {
                                    dragged = -1
                                    dragOffset = 0f
                                },
                            ) { change, drag ->
                                change.consume()
                                dragOffset += drag.y
                            }
                        }
                    } else {
                        null
                    },
                    modifier = Modifier
                        .onSizeChanged { rowHeight = it.height }
                        .zIndex(if (moving) 1f else 0f)
                        .offset { IntOffset(0, if (moving) dragOffset.roundToInt() else 0) }
                        .graphicsLayer {
                            val lift = if (moving) 1.02f else 1f
                            scaleX = lift
                            scaleY = lift
                        },
                )
            }
        }
    }
    forwarding?.let { track ->
        ForwardDialog(telegram, settings, onDismiss = { forwarding = null }) { chatId ->
            forwarding = null
            telegram.sendAudio(chatId, track)
        }
    }
}

@Composable
private fun TrackRow(
    telegram: Telegram,
    track: TdApi.Audio,
    featured: Boolean,
    own: Boolean,
    onForward: () -> Unit,
    handle: Modifier?,
    modifier: Modifier = Modifier,
) {
    var menuAt by remember { mutableStateOf<DpOffset?>(null) }
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    Box(
        modifier.pointerInput(track.audio.id) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                        val change = event.changes.first()
                        change.consume()
                        menuAt = with(density) { DpOffset(change.position.x.toDp(), change.position.y.toDp()) }
                    }
                }
            }
        },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (featured) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(8.dp),
        ) {
            val coverSize = if (featured) 56.dp else 44.dp
            Box(Modifier.size(coverSize).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                val cover = track.albumCoverThumbnail
                if (cover != null) {
                    ThumbImage(cover.file, track.albumCoverMinithumbnail, telegram, Modifier.size(coverSize))
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
            val saved = telegram.isInProfile(track)
            IconButton(onClick = { telegram.toggleProfileAudio(track) }) {
                Icon(
                    if (saved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (saved) "Remove from profile" else "Add to profile",
                    tint = if (saved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            PlayButton(track.audio.id.toLong(), 0L, AudioQueue.trackTitle(track), track.audio, telegram, track)
            if (handle != null) {
                Icon(
                    Icons.Outlined.DragHandle,
                    contentDescription = "Reorder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = handle.pointerHoverIcon(PointerIcon.Hand).padding(horizontal = 4.dp),
                )
            }
        }
        menuAt?.let { at ->
            Box(Modifier.offset(at.x, at.y)) {
                DropdownMenu(expanded = true, onDismissRequest = { menuAt = null }, shape = RoundedCornerShape(16.dp)) {
                    DropdownMenuItem(
                        text = { Text("Save as…") },
                        leadingIcon = { Icon(Icons.Outlined.Download, contentDescription = null) },
                        onClick = {
                            menuAt = null
                            scope.launch {
                                telegram.downloaded(track.audio)?.let { path -> Attachments.saveFile(path, track.fileName.ifBlank { "${track.title}.mp3" }) }
                            }
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Forward") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Forward, contentDescription = null) },
                        onClick = {
                            menuAt = null
                            onForward()
                        },
                    )
                    if (own) {
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuAt = null
                                if (telegram.isInProfile(track)) telegram.toggleProfileAudio(track)
                            },
                        )
                    }
                }
            }
        }
    }
}
