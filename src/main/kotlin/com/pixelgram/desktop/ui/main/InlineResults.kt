package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.outlined.Article
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.theme.EmojiText
import it.tdlight.jni.TdApi
import kotlin.math.abs

val InlineQuery = Regex("""^@(\w{3,})\s(.*)$""", RegexOption.DOT_MATCHES_ALL)

private val InlineCell = 96.dp
private val InlineMaxHeight = 320.dp
private const val PreferredThumb = 320

private class InlineItem(
    val id: String,
    val title: String,
    val description: String,
    val thumb: TdApi.File?,
    val mini: TdApi.Minithumbnail?,
    val media: Boolean,
    val sound: TdApi.File? = null,
    val audio: TdApi.Audio? = null,
)

private fun duration(seconds: Int) = "%d:%02d".format(seconds / 60, seconds % 60)

private fun TdApi.Photo.closest(): TdApi.File? = sizes?.minByOrNull { abs(it.width - PreferredThumb) }?.photo

private fun TdApi.InlineQueryResult.item(): InlineItem? = when (this) {
    is TdApi.InlineQueryResultArticle -> InlineItem(id, title, description, thumbnail?.file, null, false)
    is TdApi.InlineQueryResultPhoto -> InlineItem(id, title, description, photo.closest(), photo.minithumbnail, true)
    is TdApi.InlineQueryResultAnimation -> InlineItem(id, title, "", animation.thumbnail?.file, animation.minithumbnail, true)
    is TdApi.InlineQueryResultSticker -> InlineItem(id, "", "", sticker.thumbnail?.file, null, true)
    is TdApi.InlineQueryResultVideo -> InlineItem(id, title, description, video.thumbnail?.file, video.minithumbnail, false)
    is TdApi.InlineQueryResultDocument -> InlineItem(id, title, description, document.thumbnail?.file, document.minithumbnail, false)
    is TdApi.InlineQueryResultAudio -> InlineItem(
        id,
        audio.title.ifBlank { audio.fileName },
        listOf(audio.performer, duration(audio.duration)).filter { it.isNotBlank() }.joinToString(" · "),
        audio.albumCoverThumbnail?.file,
        null,
        false,
        sound = audio.audio,
        audio = audio,
    )
    is TdApi.InlineQueryResultVoiceNote -> InlineItem(id, title.ifBlank { "Voice message" }, duration(voiceNote.duration), null, null, false, sound = voiceNote.voice)
    is TdApi.InlineQueryResultGame -> InlineItem(id, game.title, game.description, game.photo?.closest(), game.photo?.minithumbnail, false)
    else -> null
}

@Composable
fun InlineResultsPanel(
    results: TdApi.InlineQueryResults?,
    telegram: Telegram,
    onPick: (Long, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 3.dp,
        modifier = modifier,
    ) {
        val items = results?.results.orEmpty().mapNotNull { it.item() }
        when {
            results == null -> Box(Modifier.fillMaxWidth().height(96.dp), contentAlignment = Alignment.Center) { LoadingIndicator() }
            items.isEmpty() -> Text(
                "No results",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(20.dp),
            )
            items.count { it.media } * 2 > items.size -> LazyVerticalGrid(
                columns = GridCells.Adaptive(InlineCell),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.heightIn(max = InlineMaxHeight).padding(8.dp),
            ) {
                items(items, key = { it.id }) { item ->
                    ThumbImage(
                        item.thumb,
                        item.mini,
                        telegram,
                        Modifier
                            .size(InlineCell)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .clickable { onPick(results.inlineQueryId, item.id) },
                    )
                }
            }
            else -> LazyColumn(Modifier.heightIn(max = InlineMaxHeight).padding(vertical = 6.dp)) {
                items(items, key = { it.id }) { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onPick(results.inlineQueryId, item.id) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        if (item.sound != null) {
                            PlayButton(item.id.hashCode().toLong(), 0L, item.title, item.sound, telegram, item.audio)
                        } else Box(
                            Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (item.thumb != null || item.mini != null) {
                                ThumbImage(item.thumb, item.mini, telegram, Modifier.size(48.dp), ContentScale.Crop)
                            } else {
                                Icon(Icons.Outlined.Article, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            EmojiText(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (item.description.isNotBlank()) {
                                EmojiText(
                                    item.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
