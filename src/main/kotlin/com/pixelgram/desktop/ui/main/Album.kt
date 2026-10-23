package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.telegram.UiMessage
import it.tdlight.jni.TdApi

private val AlbumGap = 2.dp
private val AlbumRowMax = 260.dp
private val AlbumRowMin = 90.dp

fun groupAlbums(messages: List<UiMessage>): List<UiMessage> {
    val result = ArrayList<UiMessage>(messages.size)
    var i = 0
    while (i < messages.size) {
        val message = messages[i]
        if (message.albumId == 0L) {
            result += message
            i++
            continue
        }
        var end = i
        while (end + 1 < messages.size && messages[end + 1].albumId == message.albumId) end++
        val members = messages.subList(i, end + 1)
        if (members.size == 1) {
            result += message
        } else {
            val head = members.firstOrNull { it.text.isNotEmpty() } ?: members.last()
            result += head.copy(album = members.sortedBy { it.id })
        }
        i = end + 1
    }
    return result
}

private fun rowSizes(count: Int): List<Int> = when (count) {
    1 -> listOf(1)
    2 -> listOf(2)
    3 -> listOf(1, 2)
    4 -> listOf(2, 2)
    5 -> listOf(2, 3)
    6 -> listOf(3, 3)
    7 -> listOf(2, 2, 3)
    8 -> listOf(2, 3, 3)
    9 -> listOf(3, 3, 3)
    else -> listOf(2, 2, 3, 3).let { base -> base + List((count - 10).coerceAtLeast(0) / 3 + if ((count - 10) % 3 > 0) 1 else 0) { 3 } }
}

private class Cell(
    val thumb: TdApi.File?,
    val mini: TdApi.Minithumbnail?,
    val width: Int,
    val height: Int,
    val video: TdApi.Video?,
)

private fun cellOf(message: UiMessage): Cell = when (val content = message.content) {
    is TdApi.MessageVideo -> Cell(
        content.video.thumbnail?.file,
        content.video.minithumbnail,
        content.video.width,
        content.video.height,
        content.video,
    )
    is TdApi.MessagePhoto -> Cell(message.photo, content.photo.minithumbnail, message.photoWidth, message.photoHeight, null)
    else -> Cell(message.photo, null, message.photoWidth, message.photoHeight, null)
}

private fun Cell.ratio(): Float = if (width > 0 && height > 0) (width.toFloat() / height).coerceIn(0.5f, 2.5f) else 1f

@Composable
fun AlbumGrid(
    album: List<UiMessage>,
    telegram: Telegram,
    width: Dp,
    corner: Dp,
    overlay: (@Composable () -> Unit)?,
) {
    val cells = album.map { it to cellOf(it) }
    var start = 0
    Box {
        Column(Modifier.clip(RoundedCornerShape(corner)), verticalArrangement = Arrangement.spacedBy(AlbumGap)) {
            rowSizes(cells.size).forEach { size ->
                val row = cells.subList(start, (start + size).coerceAtMost(cells.size))
                start += size
                if (row.isEmpty()) return@forEach
                val ratios = row.map { it.second.ratio() }
                val inner = width - AlbumGap * (row.size - 1)
                val height = (inner / ratios.sum()).coerceIn(AlbumRowMin, AlbumRowMax)
                Row(horizontalArrangement = Arrangement.spacedBy(AlbumGap)) {
                    row.forEachIndexed { index, (message, cell) ->
                        val cellWidth = inner * (ratios[index] / ratios.sum())
                        AlbumCell(message, cell, telegram, Modifier.width(cellWidth).height(height))
                    }
                }
            }
        }
        if (overlay != null) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.45f),
                contentColor = Color.White,
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
            ) {
                Box(Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) { overlay() }
            }
        }
    }
}

@Composable
private fun AlbumCell(message: UiMessage, cell: Cell, telegram: Telegram, modifier: Modifier) {
    val video = cell.video
    val open: () -> Unit = if (video != null) {
        { VideoPlayer.open(telegram, video, message.text) }
    } else {
        {
            val photo = message.photo
            if (photo != null) PhotoViewer.current = ViewedPhoto(telegram, photo, message.fullPhoto ?: photo, message.text, message.chatId, message.id)
        }
    }
    Box(
        modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest).clickable(onClick = open),
        contentAlignment = Alignment.Center,
    ) {
        ThumbImage(cell.thumb, cell.mini, telegram, Modifier.fillMaxSize())
        if (video != null) {
            PlayBadge(false)
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.45f),
                contentColor = Color.White,
                modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
            ) {
                androidx.compose.material3.Text(
                    formatDuration(video.duration),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                )
            }
        }
    }
}
