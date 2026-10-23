package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.theme.EmojiText
import it.tdlight.jni.TdApi

private val PreviewThumb = 56.dp
private val PreviewMaxMedia = 320.dp

private class PreviewMedia(
    val file: TdApi.File?,
    val mini: TdApi.Minithumbnail?,
    val width: Int,
    val height: Int,
)

private fun TdApi.Photo.media(): PreviewMedia? {
    val size = sizes?.maxByOrNull { it.width * it.height } ?: return null
    return PreviewMedia(size.photo, minithumbnail, size.width, size.height)
}

private fun TdApi.ChatPhoto.media(): PreviewMedia? {
    val size = sizes?.maxByOrNull { it.width * it.height } ?: return null
    return PreviewMedia(size.photo, minithumbnail, size.width, size.height)
}

private fun TdApi.Thumbnail.media(mini: TdApi.Minithumbnail?) = PreviewMedia(file, mini, width, height)

private fun TdApi.LinkPreviewType.media(): PreviewMedia? = when (this) {
    is TdApi.LinkPreviewTypeArticle -> photo?.media()
    is TdApi.LinkPreviewTypePhoto -> photo?.media()
    is TdApi.LinkPreviewTypeApp -> photo?.media()
    is TdApi.LinkPreviewTypeVideo -> cover?.media() ?: video?.thumbnail?.media(video.minithumbnail)
    is TdApi.LinkPreviewTypeEmbeddedVideoPlayer -> thumbnail?.media()
    is TdApi.LinkPreviewTypeAnimation -> animation?.thumbnail?.media(animation.minithumbnail)
    is TdApi.LinkPreviewTypeDocument -> document?.thumbnail?.media(document.minithumbnail)
    is TdApi.LinkPreviewTypeTheme -> documents?.firstOrNull()?.let { it.thumbnail?.media(it.minithumbnail) }
    is TdApi.LinkPreviewTypeSticker -> sticker?.thumbnail?.media(null)
    is TdApi.LinkPreviewTypeStickerSet -> stickers?.firstOrNull()?.thumbnail?.media(null)
    is TdApi.LinkPreviewTypeUser -> photo?.media()
    is TdApi.LinkPreviewTypeChat -> photo?.media()
    is TdApi.LinkPreviewTypeChannelBoost -> photo?.media()
    is TdApi.LinkPreviewTypeSupergroupBoost -> photo?.media()
    is TdApi.LinkPreviewTypeWebApp -> photo?.media()
    else -> null
}

private fun TdApi.LinkPreviewType.large(preview: TdApi.LinkPreview): Boolean = when (this) {
    is TdApi.LinkPreviewTypeTheme,
    is TdApi.LinkPreviewTypeVideo,
    is TdApi.LinkPreviewTypeEmbeddedVideoPlayer,
    is TdApi.LinkPreviewTypeAnimation -> true
    is TdApi.LinkPreviewTypePhoto -> true
    is TdApi.LinkPreviewTypeStickerSet,
    is TdApi.LinkPreviewTypeSticker,
    is TdApi.LinkPreviewTypeUser,
    is TdApi.LinkPreviewTypeChat,
    is TdApi.LinkPreviewTypeChannelBoost,
    is TdApi.LinkPreviewTypeSupergroupBoost -> false
    else -> preview.showLargeMedia
}

private fun TdApi.LinkPreviewType.action(): String? = when (this) {
    is TdApi.LinkPreviewTypeStickerSet -> "View stickers"
    is TdApi.LinkPreviewTypeSticker -> "View sticker"
    is TdApi.LinkPreviewTypeTheme -> "View theme"
    is TdApi.LinkPreviewTypeBackground -> "View background"
    is TdApi.LinkPreviewTypeUser -> if (isBot) "Open bot" else "Send message"
    is TdApi.LinkPreviewTypeChat -> if (type is TdApi.InviteLinkChatTypeChannel) "View channel" else "View group"
    is TdApi.LinkPreviewTypeChannelBoost,
    is TdApi.LinkPreviewTypeSupergroupBoost -> "Boost"
    is TdApi.LinkPreviewTypeMessage -> "View message"
    is TdApi.LinkPreviewTypeVideoChat,
    is TdApi.LinkPreviewTypeGroupCall -> "Join"
    is TdApi.LinkPreviewTypeWebApp -> "Open app"
    is TdApi.LinkPreviewTypeShareableChatFolder -> "Add folder"
    is TdApi.LinkPreviewTypeStory -> "View story"
    else -> null
}

fun linkPreviewOf(content: TdApi.MessageContent?): TdApi.LinkPreview? =
    (content as? TdApi.MessageText)?.linkPreview

@Composable
fun LinkPreviewCard(preview: TdApi.LinkPreview, telegram: Telegram, outgoing: Boolean, maxWidth: Dp) {
    val colors = MaterialTheme.colorScheme
    val type = preview.type
    val media = type?.media()
    val large = media != null && type.large(preview)
    val description = preview.description?.text.orEmpty()
    val open = { openUrl(preview.url) }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (outgoing) colors.primary.copy(alpha = 0.16f) else colors.surfaceContainerHighest,
        modifier = Modifier.padding(top = 6.dp).fillMaxWidth().widthIn(max = maxWidth),
    ) {
        Column(Modifier.clickable(onClick = open).padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (large && preview.showMediaAboveDescription) LargeMedia(media, telegram)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (preview.siteName.isNotBlank()) {
                        Text(
                            preview.siteName,
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (preview.title.isNotBlank()) {
                        EmojiText(
                            preview.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (description.isNotBlank()) {
                        EmojiText(
                            description,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 6,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (media != null && !large) {
                    ThumbImage(
                        media.file,
                        media.mini,
                        telegram,
                        Modifier.size(PreviewThumb).clip(RoundedCornerShape(10.dp)),
                    )
                }
            }
            if (large && !preview.showMediaAboveDescription) LargeMedia(media, telegram)
            type?.action()?.let { label ->
                FilledTonalButton(onClick = open, modifier = Modifier.fillMaxWidth()) {
                    Text(label)
                }
            }
        }
    }
}

@Composable
private fun LargeMedia(media: PreviewMedia, telegram: Telegram) {
    val ratio = if (media.width > 0 && media.height > 0) {
        (media.width.toFloat() / media.height).coerceIn(0.6f, 2.5f)
    } else {
        16f / 9f
    }
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(max = PreviewMaxMedia)
            .aspectRatio(ratio)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer),
        contentAlignment = Alignment.Center,
    ) {
        ThumbImage(media.file, media.mini, telegram, Modifier.matchParentSize())
    }
}
