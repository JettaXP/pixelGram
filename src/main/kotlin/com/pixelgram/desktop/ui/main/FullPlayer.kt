package com.pixelgram.desktop.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimationRepeat
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.pixelgram.desktop.core.AudioPlayer
import com.pixelgram.desktop.core.LyricLine
import com.pixelgram.desktop.core.Lyrics
import com.pixelgram.desktop.core.LyricsResult
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.components.expressive.IconButton
import com.pixelgram.desktop.ui.components.expressive.IconToggleButton
import com.pixelgram.desktop.ui.icons.FormatQuote
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.QueueMusic
import com.pixelgram.desktop.ui.icons.RepeatOne
import com.pixelgram.desktop.ui.icons.filled.Favorite
import com.pixelgram.desktop.ui.icons.filled.KeyboardArrowDown
import com.pixelgram.desktop.ui.icons.filled.MusicNote
import com.pixelgram.desktop.ui.icons.filled.Repeat
import com.pixelgram.desktop.ui.icons.filled.RepeatOn
import com.pixelgram.desktop.ui.icons.filled.Shuffle
import com.pixelgram.desktop.ui.icons.outlined.DragHandle
import com.pixelgram.desktop.ui.icons.outlined.FavoriteBorder
import it.tdlight.jni.TdApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

enum class PlayerView { Cover, Lyrics, Queue }

object FullPlayer {
    var open by mutableStateOf(false)
    var view by mutableStateOf(PlayerView.Cover)
}

private val Speeds = listOf(1f, 1.5f, 2f)
private val QueueRow = 64.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class, androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun FullPlayerPanel(telegram: Telegram, modifier: Modifier = Modifier) {
    val audio = AudioPlayer.audio
    val (cover, _) = coverFile(audio)
    LaunchedEffect(cover?.id) { telegram.download(cover) }
    val coverPath = telegram.localPath(cover)
    val base = MaterialTheme.colorScheme
    val palette by produceState<List<androidx.compose.ui.graphics.Color>?>(null, coverPath) {
        value = coverPath?.let { withContext(Dispatchers.IO) { coverPalette(it) } }
    }
    val dark = base.surface.luminance() < 0.5f
    val style = com.pixelgram.desktop.ui.theme.LocalAppSettings.current?.colorStyle?.value ?: com.pixelgram.desktop.ui.theme.ColorStyle.TonalSpot
    val seed = palette?.getOrNull(1)
    val themed = remember(seed, dark, style) { seed?.let { com.pixelgram.desktop.ui.theme.schemeFromSeed(it.toArgb(), dark, style) } } ?: base
    MaterialTheme(colorScheme = themed) {
    val tracks = AudioQueue.tracks(telegram)
    val index = tracks.indexOfFirst { it.key == AudioPlayer.current }
    val colors = MaterialTheme.colorScheme
    Surface(shape = MaterialTheme.shapes.extraLarge, color = colors.surfaceContainerLow, modifier = modifier) {
        Box(Modifier.fillMaxSize()) {
        AmbientBackdrop(palette, Modifier.matchParentSize())
        androidx.compose.animation.SharedTransitionLayout(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { FullPlayer.open = false }) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Collapse player", modifier = Modifier.rotate(-90f))
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (AudioQueue.playlist.any { it.audio.id.toLong() == AudioPlayer.current }) "Profile music"
                        else telegram.chats[AudioPlayer.chatId]?.let(telegram::chatTitle) ?: "Music",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (index >= 0 && tracks.size > 1) {
                        Text(
                            "${com.pixelgram.desktop.core.tr("Track")} ${index + 1} ${com.pixelgram.desktop.core.tr("of")} ${tracks.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.primary,
                        )
                    }
                }
                Spacer(Modifier.width(48.dp))
            }
            val enterFade = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
            val enterScale = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
            val exitFade = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
            AnimatedContent(
                targetState = FullPlayer.view,
                transitionSpec = {
                    (fadeIn(enterFade) + scaleIn(enterScale, initialScale = 0.94f)) togetherWith fadeOut(exitFade)
                },
                modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) { view ->
                when (view) {
                    PlayerView.Cover -> BigCover(
                        audio,
                        telegram,
                        Modifier.sharedBounds(rememberSharedContentState(CoverKey), this@AnimatedContent, resizeMode = androidx.compose.animation.SharedTransitionScope.ResizeMode.RemeasureToBounds),
                    )
                    PlayerView.Lyrics -> LyricsView(audio)
                    PlayerView.Queue -> QueueView(telegram, tracks, index)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                androidx.compose.animation.AnimatedVisibility(FullPlayer.view != PlayerView.Cover, enter = fadeIn(), exit = fadeOut()) {
                    SmallCover(
                        audio,
                        telegram,
                        56.dp,
                        Modifier.sharedBounds(rememberSharedContentState(CoverKey), this@AnimatedVisibility, resizeMode = androidx.compose.animation.SharedTransitionScope.ResizeMode.RemeasureToBounds),
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        audio?.title?.ifBlank { null } ?: AudioPlayer.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    audio?.performer?.takeIf { it.isNotBlank() }?.let { performer ->
                        Text(performer, style = MaterialTheme.typography.titleSmall, color = colors.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (audio != null) {
                    val saved = telegram.isInProfile(audio)
                    val pop by animateFloatAsState(if (saved) 1f else 0.85f, spring(dampingRatio = 0.4f, stiffness = 600f))
                    IconToggleButton(checked = saved, onCheckedChange = { telegram.toggleProfileAudio(audio) }) {
                        Icon(
                            if (saved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (saved) "Remove from profile" else "Add to profile",
                            tint = if (saved) colors.primary else colors.onSurfaceVariant,
                            modifier = Modifier.graphicsLayer {
                                scaleX = pop
                                scaleY = pop
                            },
                        )
                    }
                }
            }
            WavySeekBar(
                fraction = if (AudioPlayer.duration > 0f) AudioPlayer.position / AudioPlayer.duration else 0f,
                onSeek = AudioPlayer::seekCurrent,
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                playing = AudioPlayer.playing,
            )
            Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Text(formatDuration(AudioPlayer.position.toInt()), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                Text(formatDuration(AudioPlayer.duration.toInt()), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            ) {
                IconToggleButton(checked = AudioQueue.shuffle, onCheckedChange = { AudioQueue.shuffle = it }) {
                    Icon(Icons.Filled.Shuffle, contentDescription = "Shuffle", tint = if (AudioQueue.shuffle) colors.primary else colors.onSurfaceVariant)
                }
                MorphControls(
                    playing = AudioPlayer.playing,
                    onPrevious = { AudioPlayer.current?.let { AudioQueue.move(telegram, it, -1) } },
                    onPlay = AudioPlayer::pauseOrResume,
                    onNext = { AudioPlayer.current?.let { AudioQueue.move(telegram, it, 1) } },
                    scale = 0.82f,
                    gap = 8.dp,
                    iconSize = 28.dp,
                    playIcon = 34.dp,
                    wide = true,
                    onHold = AudioPlayer::scrub,
                )
                IconToggleButton(checked = AudioQueue.repeatMode != RepeatMode.Off, onCheckedChange = { AudioQueue.cycleRepeat() }) {
                    AnimatedContent(AudioQueue.repeatMode, transitionSpec = { scaleIn() + fadeIn() togetherWith scaleOut() + fadeOut() }) { mode ->
                        Icon(
                            when (mode) {
                                RepeatMode.Off -> Icons.Filled.Repeat
                                RepeatMode.All -> Icons.Filled.RepeatOn
                                RepeatMode.One -> Icons.Rounded.RepeatOne
                            },
                            contentDescription = "Repeat",
                            tint = if (mode == RepeatMode.Off) colors.onSurfaceVariant else colors.primary,
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                    Speeds.forEachIndexed { position, speed ->
                        ToggleButton(
                            checked = AudioPlayer.speed == speed,
                            onCheckedChange = { AudioPlayer.speed = speed },
                            shapes = when (position) {
                                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                Speeds.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier.height(40.dp),
                        ) {
                            Text(if (speed == 1.5f) "1.5x" else "${speed.toInt()}x", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                    listOf(PlayerView.Lyrics to Icons.Rounded.FormatQuote, PlayerView.Queue to Icons.Rounded.QueueMusic).forEachIndexed { position, (view, icon) ->
                        ToggleButton(
                            checked = FullPlayer.view == view,
                            onCheckedChange = { FullPlayer.view = if (FullPlayer.view == view) PlayerView.Cover else view },
                            shapes = if (position == 0) ButtonGroupDefaults.connectedLeadingButtonShapes() else ButtonGroupDefaults.connectedTrailingButtonShapes(),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            modifier = Modifier.height(40.dp),
                        ) {
                            Icon(icon, contentDescription = if (view == PlayerView.Lyrics) "Lyrics" else "Queue", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
        }
        }
    }
    }
}

@Composable
private fun AmbientBackdrop(cover: List<androidx.compose.ui.graphics.Color>?, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val palette = cover ?: listOf(scheme.primary, scheme.tertiary, scheme.secondary)
    val blobs = palette.map { androidx.compose.animation.animateColorAsState(it, tween(1200)).value }
    val drift = rememberInfiniteTransition()
    val phase by drift.animateFloat(0f, (2 * kotlin.math.PI).toFloat(), infiniteRepeatable(tween(DriftMs, easing = LinearEasing), AnimationRepeat.Restart))
    val dark = scheme.surface.luminance() < 0.5f
    Box(modifier) {
        androidx.compose.foundation.Canvas(Modifier.matchParentSize().blur(80.dp, androidx.compose.ui.draw.BlurredEdgeTreatment.Unbounded)) {
            val w = size.width
            val h = size.height
            BlobSpots.forEachIndexed { i, spot ->
                val turn = phase * spot.speed + i * 2.1f
                val center = androidx.compose.ui.geometry.Offset(
                    w * (spot.x + 0.16f * kotlin.math.sin(turn)),
                    h * (spot.y + 0.1f * kotlin.math.cos(turn * 0.8f)),
                )
                drawCircle(blobs[i % blobs.size].copy(alpha = if (dark) 0.6f else 0.45f), radius = w * spot.radius, center = center)
            }
        }
        Box(Modifier.matchParentSize().background(scheme.surfaceContainerLow.copy(alpha = if (dark) 0.35f else 0.45f)))
    }
}

private class BlobSpot(val x: Float, val y: Float, val radius: Float, val speed: Float)

private val BlobSpots = listOf(
    BlobSpot(0.2f, 0.18f, 0.42f, 1f),
    BlobSpot(0.85f, 0.5f, 0.38f, 2f),
    BlobSpot(0.3f, 0.9f, 0.46f, 1f),
)

private const val DriftMs = 42_000

private suspend fun coverPalette(path: String): List<androidx.compose.ui.graphics.Color>? {
    val image = com.pixelgram.desktop.ui.components.loadImage(path, 48) ?: return null
    val width = image.width
    val height = image.height
    if (width == 0 || height == 0) return null
    val pixels = IntArray(width * height)
    image.readPixels(pixels)
    val bands = listOf(0 until height / 3, height / 3 until height * 2 / 3, height * 2 / 3 until height)
    return bands.map { rows ->
        var r = 0L
        var g = 0L
        var b = 0L
        var n = 0
        for (y in rows) for (x in 0 until width) {
            val p = pixels[y * width + x]
            r += (p shr 16) and 0xFF
            g += (p shr 8) and 0xFF
            b += p and 0xFF
            n++
        }
        if (n == 0) return null
        val hsb = java.awt.Color.RGBtoHSB((r / n).toInt(), (g / n).toInt(), (b / n).toInt(), null)
        val vivid = java.awt.Color.HSBtoRGB(hsb[0], (hsb[1] * 1.4f).coerceIn(0.35f, 1f), hsb[2].coerceIn(0.45f, 0.9f))
        androidx.compose.ui.graphics.Color(vivid or (0xFF shl 24))
    }
}

private fun coverFile(audio: TdApi.Audio?): Pair<TdApi.File?, TdApi.Minithumbnail?> =
    (audio?.albumCoverThumbnail?.file ?: audio?.externalAlbumCovers?.maxByOrNull { it.width }?.file) to audio?.albumCoverMinithumbnail

@Composable
private fun SmallCover(audio: TdApi.Audio?, telegram: Telegram, size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    val (file, mini) = coverFile(audio)
    Box(
        modifier.size(size).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (file != null || mini != null) {
            ThumbImage(file, mini, telegram, Modifier.fillMaxSize())
        } else {
            Icon(Icons.Filled.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(size / 2.4f))
        }
    }
}

@Composable
private fun BigCover(audio: TdApi.Audio?, telegram: Telegram, modifier: Modifier = Modifier) {
    val (file, mini) = coverFile(audio)
    val breathe by animateFloatAsState(if (AudioPlayer.playing) 1f else 0.9f, spring(dampingRatio = 0.6f, stiffness = 220f))
    val spin = rememberInfiniteTransition()
    val angle by spin.animateFloat(0f, 360f, infiniteRepeatable(tween(24_000, easing = LinearEasing), AnimationRepeat.Restart))
    Box(Modifier.aspectRatio(1f).then(modifier), contentAlignment = Alignment.Center) {
        if (file != null || mini != null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = breathe
                        scaleY = breathe
                    }
                    .clip(RoundedCornerShape(32.dp)),
            ) { ThumbImage(file, mini, telegram, Modifier.fillMaxSize()) }
        } else {
            Box(
                Modifier
                    .fillMaxSize(0.86f)
                    .graphicsLayer {
                        scaleX = breathe
                        scaleY = breathe
                        rotationZ = if (AudioPlayer.playing) angle else 0f
                    }
                    .clip(MaterialShapes.Cookie12Sided.toShape())
                    .background(MaterialTheme.colorScheme.primaryContainer),
            )
            Icon(Icons.Filled.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(96.dp))
        }
    }
}

@Composable
private fun LyricsView(audio: TdApi.Audio?) {
    val artist = audio?.performer.orEmpty()
    val title = audio?.title?.ifBlank { null } ?: audio?.fileName?.substringBeforeLast('.').orEmpty()
    val result by produceState(Lyrics.cached(artist, title) ?: LyricsResult.Loading, artist, title) {
        value = Lyrics.cached(artist, title) ?: LyricsResult.Loading
        if (value == LyricsResult.Loading) value = withContext(Dispatchers.IO) { Lyrics.load(artist, title, audio?.duration ?: 0) }
    }
    when (val lyrics = result) {
        LyricsResult.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() }
        LyricsResult.NotFound -> LyricsState(Icons.Rounded.FormatQuote, "No lyrics found", "There are no lyrics for this track on LRCLIB.")
        LyricsResult.Instrumental -> LyricsState(Icons.Filled.MusicNote, "Instrumental", "This track is marked as having no lyrics.")
        is LyricsResult.Plain -> Column(
            Modifier.fillMaxSize().edgeFade().verticalScroll(rememberScrollState()).padding(vertical = 40.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            lyrics.lines.forEach { line -> Text(line, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)) }
        }
        is LyricsResult.Synced -> SyncedLyrics(lyrics.lines)
    }
}

@Composable
private fun LyricsState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)) {
        Box(
            Modifier.size(88.dp).clip(MaterialShapes.Cookie9Sided.toShape()).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(40.dp))
        }
        Text(title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center), color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun SyncedLyrics(lines: List<LyricLine>) {
    val now = AudioPlayer.position
    val current = lines.indexOfLast { it.time <= now + 0.15f }
    val state = rememberLazyListState()
    var pausedUntil by remember { mutableStateOf(0L) }
    val follow = remember {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
                if (source == androidx.compose.ui.input.nestedscroll.NestedScrollSource.UserInput) pausedUntil = System.currentTimeMillis() + FollowPauseMs
                return androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }
    val viewport = state.layoutInfo.viewportSize.height
    LaunchedEffect(current, pausedUntil) {
        val wait = pausedUntil - System.currentTimeMillis()
        if (wait > 0) kotlinx.coroutines.delay(wait)
        if (current >= 0) state.animateScrollToItem(current, -viewport / 3)
    }
    LazyColumn(
        state = state,
        contentPadding = PaddingValues(vertical = 120.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(follow)
            .edgeFade(),
    ) {
        itemsIndexed(lines) { position, line ->
            val distance = if (current < 0) position + 1 else abs(position - current)
            val upcoming = position > current
            val alpha by animateFloatAsState(if (distance == 0) 1f else if (upcoming) 0.5f else 0.38f, tween(400))
            val blur by animateFloatAsState(if (distance == 0 || !upcoming) 0f else (distance * 1.4f).coerceAtMost(5f), tween(500))
            val scale by animateFloatAsState(if (distance == 0) 1f else 0.94f, spring(dampingRatio = 0.8f, stiffness = 300f))
            val style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            val color = MaterialTheme.colorScheme.onSurface
            val text = if (distance == 0) {
                val end = lines.getOrNull(position + 1)?.time ?: (line.time + 5f)
                karaoke(line, now, end, color)
            } else {
                androidx.compose.ui.text.AnnotatedString(line.text.ifBlank { "♪" })
            }
            Text(
                text,
                style = style,
                color = color,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        this.alpha = alpha
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                    }
                    .blur(blur.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        pausedUntil = 0L
                        if (AudioPlayer.duration > 0f) AudioPlayer.seekCurrent(line.time / AudioPlayer.duration)
                    },
            )
        }
    }
}

private fun karaoke(line: LyricLine, now: Float, end: Float, color: androidx.compose.ui.graphics.Color): androidx.compose.ui.text.AnnotatedString {
    val text = line.text.ifBlank { "♪" }
    val sung = if (line.words.isNotEmpty()) {
        var chars = 0f
        var offset = 0
        line.words.forEachIndexed { i, word ->
            val wordEnd = line.words.getOrNull(i + 1)?.time ?: end
            val length = word.text.length
            val part = ((now - word.time) / (wordEnd - word.time).coerceAtLeast(0.05f)).coerceIn(0f, 1f)
            if (now >= word.time) chars = offset + part * length
            offset += length
        }
        chars / offset.coerceAtLeast(1) * text.length
    } else {
        val span = (end - line.time).coerceIn(0.4f, text.length * 0.09f + 0.6f)
        ((now - line.time) / span).coerceIn(0f, 1f) * text.length
    }
    return androidx.compose.ui.text.buildAnnotatedString {
        text.forEachIndexed { i, ch ->
            val lit = (sung - i).coerceIn(0f, 1f)
            withStyle(androidx.compose.ui.text.SpanStyle(color = color.copy(alpha = 0.35f + 0.65f * lit))) { append(ch) }
        }
    }
}

private fun Modifier.edgeFade(): Modifier = this
    .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        drawRect(
            androidx.compose.ui.graphics.Brush.verticalGradient(
                0f to androidx.compose.ui.graphics.Color.Transparent,
                0.12f to androidx.compose.ui.graphics.Color.Black,
                0.78f to androidx.compose.ui.graphics.Color.Black,
                1f to androidx.compose.ui.graphics.Color.Transparent,
            ),
            blendMode = androidx.compose.ui.graphics.BlendMode.DstIn,
        )
    }

private const val FollowPauseMs = 3_000L
private const val CoverKey = "player-cover"

@Composable
private fun QueueView(telegram: Telegram, tracks: List<QueueTrack>, current: Int) {
    val state = rememberLazyListState()
    LaunchedEffect(Unit) { if (current > 0) state.scrollToItem((current - 2).coerceAtLeast(0)) }
    var dragging by remember { mutableIntStateOf(-1) }
    var offset by remember { mutableFloatStateOf(0f) }
    val rowPx = with(androidx.compose.ui.platform.LocalDensity.current) { QueueRow.toPx() }
    LazyColumn(state = state, modifier = Modifier.fillMaxSize()) {
        itemsIndexed(tracks, key = { _, track -> track.key }) { position, track ->
            val lifted = position == dragging
            val playing = position == current
            val elevation by animateFloatAsState(if (lifted) 1f else 0f)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .zIndex(if (lifted) 1f else 0f)
                    .graphicsLayer {
                        translationY = if (lifted) offset else 0f
                        scaleX = 1f + elevation * 0.03f
                        scaleY = 1f + elevation * 0.03f
                    }
                    .fillMaxWidth()
                    .height(QueueRow)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        when {
                            lifted -> MaterialTheme.colorScheme.surfaceContainerHighest
                            playing -> MaterialTheme.colorScheme.secondaryContainer
                            else -> androidx.compose.ui.graphics.Color.Transparent
                        },
                    )
                    .clickable { AudioQueue.playAt(telegram, track) }
                    .padding(horizontal = 8.dp),
            ) {
                SmallCover(track.audio, telegram, 48.dp)
                Column(Modifier.weight(1f)) {
                    Text(track.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (track.performer.isNotBlank()) {
                        Text(track.performer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Icon(
                    Icons.Outlined.DragHandle,
                    contentDescription = "Reorder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(40.dp)
                        .padding(8.dp)
                        .pointerInput(tracks.size) {
                            detectDragGestures(
                                onDragStart = {
                                    dragging = tracks.indexOfFirst { it.key == track.key }
                                    offset = 0f
                                },
                                onDragEnd = { dragging = -1; offset = 0f },
                                onDragCancel = { dragging = -1; offset = 0f },
                            ) { change, amount ->
                                change.consume()
                                offset += amount.y
                                val steps = (offset / rowPx).roundToInt()
                                if (steps != 0 && dragging >= 0) {
                                    val target = (dragging + steps).coerceIn(0, tracks.lastIndex)
                                    if (target != dragging) {
                                        AudioQueue.reorder(telegram, dragging, target)
                                        offset -= (target - dragging) * rowPx
                                        dragging = target
                                    }
                                }
                            }
                        },
                )
            }
        }
    }
}
