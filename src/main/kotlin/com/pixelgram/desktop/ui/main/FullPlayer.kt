package com.pixelgram.desktop.ui.main

import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.SizeTransform
import androidx.compose.ui.unit.IntOffset
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
import androidx.compose.foundation.gestures.animateScrollBy
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
import com.pixelgram.desktop.ui.components.listScrollbar
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
import androidx.compose.ui.layout.layout
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
import com.pixelgram.desktop.telegram.fullName
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.components.expressive.IconButton
import com.pixelgram.desktop.ui.components.expressive.IconToggleButton
import com.pixelgram.desktop.ui.icons.FormatQuote
import com.pixelgram.desktop.ui.icons.Icons
import kotlinx.coroutines.launch
import com.pixelgram.desktop.ui.icons.automirrored.outlined.Forward
import com.pixelgram.desktop.ui.icons.outlined.Info
import com.pixelgram.desktop.ui.icons.outlined.Delete
import com.pixelgram.desktop.ui.icons.outlined.Download
import com.pixelgram.desktop.ui.icons.outlined.Share
import com.pixelgram.desktop.ui.icons.filled.MoreVert
import com.pixelgram.desktop.ui.icons.QueueMusic
import com.pixelgram.desktop.ui.icons.RepeatOne
import com.pixelgram.desktop.ui.icons.filled.Favorite
import com.pixelgram.desktop.ui.icons.filled.KeyboardArrowDown
import com.pixelgram.desktop.ui.icons.TrackNote
import com.pixelgram.desktop.ui.icons.filled.Repeat
import com.pixelgram.desktop.ui.icons.filled.Shuffle
import com.pixelgram.desktop.ui.icons.outlined.DragHandle
import com.pixelgram.desktop.ui.icons.outlined.FavoriteBorder
import it.tdlight.jni.TdApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.drawText
import androidx.compose.ui.graphics.drawscope.translate
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
    val coverPath = com.pixelgram.desktop.core.CoverArt.path(audio, large = true) ?: telegram.localPath(cover)
    val base = MaterialTheme.colorScheme
    val palette by produceState<List<androidx.compose.ui.graphics.Color>?>(null, coverPath) {
        value = coverPath?.let { withContext(Dispatchers.IO) { coverPalette(it) } }
    }
    val dark = base.surface.luminance() < 0.5f
    val style = com.pixelgram.desktop.ui.theme.LocalAppSettings.current?.colorStyle?.value ?: com.pixelgram.desktop.ui.theme.ColorStyle.TonalSpot
    val seed = palette?.getOrNull(3) ?: palette?.getOrNull(1)
    val themed = remember(seed, dark, style) { seed?.let { com.pixelgram.desktop.ui.theme.schemeFromSeed(it.toArgb(), dark, style) } } ?: base
    MaterialTheme(colorScheme = animatedScheme(themed)) {
    val tracks = AudioQueue.tracks(telegram)
    val index = tracks.indexOfFirst { it.key == AudioPlayer.current }
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = colors.surfaceContainerLow,
        modifier = modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                    if (event.type == androidx.compose.ui.input.pointer.PointerEventType.Press) PlayerKeys.armed = true
                }
            }
        },
    ) {
        Box(Modifier.fillMaxSize()) {
        AmbientBackdrop(palette?.take(3), Modifier.matchParentSize())
        androidx.compose.animation.SharedTransitionLayout(Modifier.fillMaxSize()) {
        val trackSpatial = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
        val trackScale = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
        val trackFade = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
        val trackFadeOut = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { FullPlayer.open = false }) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Collapse player", modifier = Modifier.rotate(-90f))
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        when {
                            AudioQueue.playlist.any { it.audio.id.toLong() == AudioPlayer.current } ->
                                telegram.chats[AudioQueue.playlistOwner]?.let(telegram::chatTitle)
                                    ?: telegram.users[AudioQueue.playlistOwner]?.fullName()
                                    ?: com.pixelgram.desktop.core.tr("Profile music")
                            else -> telegram.chats[AudioPlayer.chatId]?.let(telegram::chatTitle) ?: com.pixelgram.desktop.core.tr("Music")
                        },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (index >= 0 && tracks.size > 1) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${com.pixelgram.desktop.core.tr("Track")} ", style = MaterialTheme.typography.labelMedium, color = colors.primary)
                            AnimatedContent(
                                targetState = index + 1,
                                transitionSpec = {
                                    val up = if (targetState > initialState) 1 else -1
                                    (slideInVertically(trackSpatial) { it * up } + fadeIn(trackFade)) togetherWith
                                        (slideOutVertically(trackSpatial) { -it * up } + fadeOut(trackFadeOut))
                                },
                            ) { number ->
                                Text("$number", style = MaterialTheme.typography.labelMedium, color = colors.primary)
                            }
                            Text(" ${com.pixelgram.desktop.core.tr("of")} ${tracks.size}", style = MaterialTheme.typography.labelMedium, color = colors.primary)
                        }
                    }
                }
                PlayerMenu(telegram)
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
                    PlayerView.Cover -> AnimatedContent(
                        targetState = TrackSlot(audio, index),
                        contentKey = { it.audio?.audio?.id },
                        transitionSpec = {
                            val dir = if (targetState.index >= initialState.index) 1 else -1
                            (slideInHorizontally(trackSpatial) { it / 3 * dir } + fadeIn(trackFade) + scaleIn(trackScale, initialScale = 0.9f)) togetherWith
                                (slideOutHorizontally(trackSpatial) { -it / 3 * dir } + fadeOut(trackFadeOut) + scaleOut(trackScale, targetScale = 0.9f))
                        },
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.sharedBounds(rememberSharedContentState(CoverKey), this@AnimatedContent, boundsTransform = { _, _ -> spring(dampingRatio = CoverSpringDamping, stiffness = CoverSpringStiffness) }, resizeMode = androidx.compose.animation.SharedTransitionScope.ResizeMode.RemeasureToBounds),
                    ) { slot -> BigCover(slot.audio, telegram) }
                    PlayerView.Lyrics -> LyricsView(audio)
                    PlayerView.Queue -> QueueView(telegram, tracks, index)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                val coverSlot by androidx.compose.animation.core.animateDpAsState(
                    if (FullPlayer.view != PlayerView.Cover) 70.dp else 0.dp,
                    spring(dampingRatio = CoverSpringDamping, stiffness = CoverSpringStiffness),
                )
                Box(Modifier.width(coverSlot.coerceAtLeast(0.dp)).height(56.dp)) {
                androidx.compose.animation.AnimatedVisibility(
                    FullPlayer.view != PlayerView.Cover,
                    enter = fadeIn(com.pixelgram.desktop.ui.components.Motion.effects()),
                    exit = fadeOut(com.pixelgram.desktop.ui.components.Motion.fastEffects()),
                ) {
                    SmallCover(
                        audio,
                        telegram,
                        56.dp,
                        Modifier.sharedBounds(rememberSharedContentState(CoverKey), this@AnimatedVisibility, boundsTransform = { _, _ -> spring(dampingRatio = CoverSpringDamping, stiffness = CoverSpringStiffness) }, resizeMode = androidx.compose.animation.SharedTransitionScope.ResizeMode.RemeasureToBounds),
                    )
                }
                }
                val shownTitle = audio?.title?.ifBlank { null } ?: AudioPlayer.title
                AnimatedContent(
                    targetState = TrackText(shownTitle, audio?.performer?.takeIf { it.isNotBlank() }, index),
                    contentKey = { it.title to it.performer },
                    transitionSpec = {
                        val dir = if (targetState.index >= initialState.index) 1 else -1
                        androidx.compose.animation.ContentTransform(
                            slideInHorizontally(trackSpatial) { it / 4 * dir } + fadeIn(trackFade),
                            slideOutHorizontally(trackSpatial) { -it / 4 * dir } + fadeOut(trackFadeOut),
                            sizeTransform = SizeTransform(clip = false),
                        )
                    },
                    modifier = Modifier.weight(1f),
                ) { text ->
                    Column {
                        Text(
                            text.title,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        text.performer?.let { performer ->
                            Text(performer, style = MaterialTheme.typography.titleSmall, color = colors.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
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
                duration = AudioPlayer.duration,
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
                IconButton(onClick = { AudioQueue.shuffle = !AudioQueue.shuffle }) {
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
                IconButton(onClick = { AudioQueue.cycleRepeat() }) {
                    AnimatedContent(AudioQueue.repeatMode, transitionSpec = { scaleIn(com.pixelgram.desktop.ui.components.Motion.spatial()) + fadeIn(com.pixelgram.desktop.ui.components.Motion.effects()) togetherWith scaleOut(com.pixelgram.desktop.ui.components.Motion.fastSpatial()) + fadeOut(com.pixelgram.desktop.ui.components.Motion.fastEffects()) }) { mode ->
                        Icon(
                            when (mode) {
                                RepeatMode.Off -> Icons.Filled.Repeat
                                RepeatMode.All -> Icons.Filled.Repeat
                                RepeatMode.One -> Icons.Rounded.RepeatOne
                            },
                            contentDescription = "Repeat",
                            tint = if (mode == RepeatMode.Off) colors.onSurfaceVariant else colors.primary,
                        )
                    }
                }
            }
            val groupColors = androidx.compose.material3.ToggleButtonDefaults.toggleButtonColors(
                containerColor = colors.secondaryContainer.copy(alpha = GroupIdleAlpha),
                contentColor = colors.onSecondaryContainer,
            )
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
                            colors = groupColors,
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
                            colors = groupColors,
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
        ScrubPill(Modifier.align(Alignment.TopCenter).padding(top = 64.dp))
        }
    }
    }
}

@Composable
private fun ScrubPill(modifier: Modifier) {
    val direction = AudioPlayer.scrubbing
    var shown by remember { mutableIntStateOf(1) }
    if (direction != 0) shown = direction
    androidx.compose.animation.AnimatedVisibility(
        visible = direction != 0,
        enter = scaleIn(spring(dampingRatio = 0.6f, stiffness = 500f), initialScale = 0.6f) + fadeIn(com.pixelgram.desktop.ui.components.Motion.effects()),
        exit = scaleOut(targetScale = 0.8f) + fadeOut(com.pixelgram.desktop.ui.components.Motion.fastEffects()),
        modifier = modifier,
    ) {
        val colors = MaterialTheme.colorScheme
        Surface(shape = CircleShape, color = colors.primary, contentColor = colors.onPrimary, shadowElevation = 6.dp) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                val backward = shown < 0
                if (backward) Chevrons(backward = true, color = colors.onPrimary)
                Text(if (backward) "3x" else "4x", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                if (!backward) Chevrons(backward = false, color = colors.onPrimary)
            }
        }
    }
}

@Composable
private fun Chevrons(backward: Boolean, color: androidx.compose.ui.graphics.Color) {
    val flow = rememberInfiniteTransition()
    val phase by flow.animateFloat(0f, 1f, infiniteRepeatable(tween(ChevronMs, easing = LinearEasing), AnimationRepeat.Restart))
    androidx.compose.foundation.Canvas(Modifier.size(width = 26.dp, height = 18.dp)) {
        val stroke = 2.6.dp.toPx()
        val h = size.height
        val step = size.width / 2.6f
        for (k in 0..1) {
            val order = if (backward) 1 - k else k
            val wave = ((phase - order * 0.35f) % 1f + 1f) % 1f
            val alpha = 0.3f + 0.7f * (1f - kotlin.math.abs(wave * 2f - 1f))
            val x = step * 0.4f + k * step
            val tip = if (backward) x else x + step * 0.8f
            val tail = if (backward) x + step * 0.8f else x
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(tail, h * 0.15f)
                lineTo(tip, h / 2f)
                lineTo(tail, h * 0.85f)
            }
            drawPath(
                path,
                color.copy(alpha = alpha),
                style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round),
            )
        }
    }
}

private const val ChevronMs = 700

@Composable
private fun AmbientBackdrop(cover: List<androidx.compose.ui.graphics.Color>?, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val palette = cover ?: listOf(scheme.primary, scheme.tertiary, scheme.secondary)
    val blobs = palette.map { androidx.compose.animation.animateColorAsState(it, tween(1200)).value }
    var phase by remember { mutableFloatStateOf(0f) }
    val playing = AudioPlayer.playing
    LaunchedEffect(playing) {
        var last = 0L
        while (playing) {
            androidx.compose.runtime.withFrameNanos { now ->
                if (last != 0L) phase = (phase + (now - last) / 1_000_000_000f / (DriftMs / 1000f) * 2f * kotlin.math.PI.toFloat()) % (2f * kotlin.math.PI.toFloat())
                last = now
            }
        }
    }
    val dark = scheme.surface.luminance() < 0.5f
    Box(modifier) {
        androidx.compose.foundation.Canvas(Modifier.matchParentSize().blur(80.dp, androidx.compose.ui.draw.BlurredEdgeTreatment.Unbounded)) {
            val w = size.width
            val h = size.height
            BlobSpots.forEachIndexed { i, spot ->
                val turn = phase * spot.speed + i * 2.1f
                val center = androidx.compose.ui.geometry.Offset(
                    w * (spot.x + 0.16f * kotlin.math.sin(turn)),
                    h * (spot.y + 0.1f * kotlin.math.cos(turn + spot.y * 3f)),
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
    val image = com.pixelgram.desktop.ui.components.loadImage(path, PaletteSize) ?: return null
    val width = image.width
    val height = image.height
    if (width == 0 || height == 0) return null
    val pixels = IntArray(width * height)
    image.readPixels(pixels)
    val bands = listOf(0 until height / 3, height / 3 until height * 2 / 3, height * 2 / 3 until height)
    val all = vividColor(pixels, 0 until height, width) ?: return null
    return bands.map { rows -> vividColor(pixels, rows, width) ?: all } + all
}

private fun vividColor(pixels: IntArray, rows: IntRange, width: Int): androidx.compose.ui.graphics.Color? {
    val weight = FloatArray(HueBuckets)
    val r = FloatArray(HueBuckets)
    val g = FloatArray(HueBuckets)
    val b = FloatArray(HueBuckets)
    var sumR = 0L
    var sumG = 0L
    var sumB = 0L
    var count = 0
    val hsb = FloatArray(3)
    for (y in rows) for (x in 0 until width) {
        val p = pixels[y * width + x]
        val pr = (p shr 16) and 0xFF
        val pg = (p shr 8) and 0xFF
        val pb = p and 0xFF
        sumR += pr
        sumG += pg
        sumB += pb
        count++
        java.awt.Color.RGBtoHSB(pr, pg, pb, hsb)
        val w = hsb[1] * hsb[1] * hsb[2]
        if (w < MinVivid) continue
        val bucket = (hsb[0] * HueBuckets).toInt().coerceIn(0, HueBuckets - 1)
        weight[bucket] += w
        r[bucket] += pr * w
        g[bucket] += pg * w
        b[bucket] += pb * w
    }
    if (count == 0) return null
    val best = weight.indices.maxByOrNull { weight[it] } ?: return null
    val (cr, cg, cb) = if (weight[best] > count * MinShare) {
        Triple(r[best] / weight[best], g[best] / weight[best], b[best] / weight[best])
    } else {
        Triple(sumR.toFloat() / count, sumG.toFloat() / count, sumB.toFloat() / count)
    }
    java.awt.Color.RGBtoHSB(cr.toInt(), cg.toInt(), cb.toInt(), hsb)
    val vivid = java.awt.Color.HSBtoRGB(hsb[0], (hsb[1] * 1.2f).coerceIn(0.35f, 1f), hsb[2].coerceIn(0.5f, 0.95f))
    return androidx.compose.ui.graphics.Color(vivid or (0xFF shl 24))
}

private const val HueBuckets = 24
private const val MinVivid = 0.04f
private const val MinShare = 0.002f
private const val PaletteSize = 96

private fun coverFile(audio: TdApi.Audio?): Pair<TdApi.File?, TdApi.Minithumbnail?> =
    (audio?.albumCoverThumbnail?.file ?: audio?.externalAlbumCovers?.maxByOrNull { it.width }?.file) to audio?.albumCoverMinithumbnail

@Composable
private fun SmallCover(audio: TdApi.Audio?, telegram: Telegram, size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    val (file, mini) = coverFile(audio)
    val found = com.pixelgram.desktop.core.CoverArt.path(audio)
        val hasImage = found != null || mini != null || (file != null && telegram.localPath(file) != null)
    val shape = if (hasImage) RoundedCornerShape(16.dp) else MaterialShapes.Cookie12Sided.toShape()
    Box(
        modifier.size(size).clip(shape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.TrackNote, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(size / 2.4f))
        if (hasImage) ThumbImage(file, mini, telegram, Modifier.fillMaxSize(), localPath = found)
    }
}

@Composable
private fun BigCover(audio: TdApi.Audio?, telegram: Telegram, modifier: Modifier = Modifier) {
    val (file, mini) = coverFile(audio)
    val breathe by animateFloatAsState(if (AudioPlayer.playing) 1f else 0.9f, spring(dampingRatio = 0.6f, stiffness = 220f))
    val spin = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(AudioPlayer.playing) {
        if (AudioPlayer.playing) {
            while (true) {
                spin.animateTo(spin.value + 360f, tween(SpinMs, easing = LinearEasing))
                spin.snapTo(spin.value % 360f)
            }
        } else {
            val rest = kotlin.math.round(spin.value / CookieStep) * CookieStep
            spin.animateTo(rest, spring(dampingRatio = 0.55f, stiffness = 120f))
            spin.snapTo(spin.value % 360f)
        }
    }
    val angle = spin.value
    Box(Modifier.aspectRatio(1f).then(modifier), contentAlignment = Alignment.Center) {
        LaunchedEffect(file?.id) { telegram.download(file) }
        val found = com.pixelgram.desktop.core.CoverArt.path(audio, large = true)
        val hasImage = found != null || mini != null || (file != null && telegram.localPath(file) != null)
        if (hasImage) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = breathe
                        scaleY = breathe
                    }
                    .clip(RoundedCornerShape(32.dp)),
            ) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.TrackNote, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(96.dp))
                }
                ThumbImage(file, mini, telegram, Modifier.fillMaxSize(), localPath = found)
            }
        } else {
            Box(
                Modifier
                    .fillMaxSize(0.86f)
                    .graphicsLayer {
                        scaleX = breathe
                        scaleY = breathe
                        rotationZ = angle
                    }
                    .clip(MaterialShapes.Cookie12Sided.toShape())
                    .background(MaterialTheme.colorScheme.primaryContainer),
            )
            Icon(Icons.Rounded.TrackNote, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(96.dp))
        }
    }
}

@Composable
private fun LyricsView(audio: TdApi.Audio?) {
    val named = audio?.fileName?.substringBeforeLast('.').orEmpty()
    val split = named.split(" - ", " – ", limit = 2).takeIf { it.size == 2 }
    val artist = audio?.performer?.ifBlank { null } ?: split?.first()?.trim().orEmpty()
    val title = audio?.title?.ifBlank { null } ?: split?.last()?.trim() ?: named
    val result by produceState(Lyrics.cached(artist, title) ?: LyricsResult.Loading, artist, title) {
        value = Lyrics.cached(artist, title) ?: LyricsResult.Loading
        if (value == LyricsResult.Loading) value = withContext(Dispatchers.IO) { Lyrics.load(artist, title, audio?.duration ?: 0) }
    }
    when (val lyrics = result) {
        LyricsResult.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() }
        LyricsResult.NotFound -> LyricsState(Icons.Rounded.FormatQuote, "No lyrics found", "There are no lyrics for this track on LRCLIB.")
        LyricsResult.Instrumental -> LyricsState(Icons.Rounded.TrackNote, "Instrumental", "This track is marked as having no lyrics.")
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

private sealed interface LyricItem {
    val start: Float
    class Line(val index: Int, override val start: Float) : LyricItem
    class Gap(override val start: Float, val end: Float) : LyricItem
}

private fun lyricItems(lines: List<LyricLine>): List<LyricItem> {
    val out = mutableListOf<LyricItem>()
    val first = lines.firstOrNull { it.text.isNotBlank() }
    if (first != null && first.time > GapSeconds) out += LyricItem.Gap(0f, first.time)
    lines.forEachIndexed { i, line ->
        if (line.text.isNotBlank()) {
            out += LyricItem.Line(i, line.time)
        } else {
            val next = lines.drop(i + 1).firstOrNull { it.text.isNotBlank() }?.time
            if (next != null && next - line.time > GapSeconds && out.lastOrNull() !is LyricItem.Gap) out += LyricItem.Gap(line.time, next)
        }
    }
    return out
}

@Composable
private fun SyncedLyrics(lines: List<LyricLine>) {
    val now = rememberSmoothPosition()
    val items = remember(lines) { lyricItems(lines) }
    val current = items.indexOfLast { it.start <= now + 0.15f }
    val state = rememberLazyListState(initialFirstVisibleItemIndex = (current - 1).coerceAtLeast(0))
    var placed by remember { mutableStateOf(false) }
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
        if (current < 0) return@LaunchedEffect
        val info = state.layoutInfo
        val item = info.visibleItemsInfo.firstOrNull { it.index == current }
        val previous = info.visibleItemsInfo.firstOrNull { it.index == current - 1 }?.size ?: 0
        val desired = (viewport * LyricsTopFade).toInt() + previous + info.mainAxisItemSpacing
        when {
            !placed -> {
                state.scrollToItem(current, -desired + info.beforeContentPadding)
                placed = true
            }
            item != null -> state.animateScrollBy((item.offset - info.viewportStartOffset - desired).toFloat(), tween(LineScrollMs, easing = androidx.compose.animation.core.FastOutSlowInEasing))
            else -> state.animateScrollToItem(current, -desired + info.beforeContentPadding)
        }
    }
    LazyColumn(
        state = state,
        contentPadding = PaddingValues(horizontal = 6.dp + LyricsBleed - LinePad, vertical = 120.dp),
        verticalArrangement = Arrangement.spacedBy(LineSpacing),
        modifier = Modifier
            .fillMaxSize()
            .listScrollbar(state)
            .layout { measurable, constraints ->
                val extra = LyricsBleed.roundToPx()
                val wide = constraints.copy(
                    minWidth = constraints.minWidth + extra * 2,
                    maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth + extra * 2 else constraints.maxWidth,
                )
                val placeable = measurable.measure(wide)
                layout((placeable.width - extra * 2).coerceIn(constraints.minWidth, constraints.maxWidth), placeable.height) { placeable.place(-extra, 0) }
            }
            .nestedScroll(follow)
            .edgeFade(),
    ) {
        itemsIndexed(items) { position, entry ->
            val ease = androidx.compose.animation.core.FastOutSlowInEasing
            if (entry is LyricItem.Gap) {
                GapIndicator(entry, now, Modifier.padding(horizontal = LinePad))
                return@itemsIndexed
            }
            val line = lines[(entry as LyricItem.Line).index]
            val distance = if (current < 0) position + 1 else abs(position - current)
            val upcoming = position > current
            val lineEnd = items.getOrNull(position + 1)?.start ?: (line.time + 5f)
            val done = distance == 0 && now >= lineEnd - LineSettle
            val alpha by animateFloatAsState(if (distance == 0) 1f else if (upcoming) 0.5f else 0.4f, tween(LineFadeMs, easing = ease))
            val blur by animateFloatAsState(if (distance == 0) 0f else (distance * 1.3f).coerceAtMost(5f), tween(LineFadeMs, easing = ease))
            val scale by animateFloatAsState(
                when {
                    done -> 0.97f
                    distance == 0 -> 1.02f
                    else -> 0.94f
                },
                spring(dampingRatio = 0.9f, stiffness = 120f),
            )
            val style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            val color = MaterialTheme.colorScheme.onSurface
            Box(
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        this.alpha = alpha
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                    }
                    .blur(blur.dp, androidx.compose.ui.draw.BlurredEdgeTreatment.Unbounded)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        pausedUntil = 0L
                        if (AudioPlayer.duration > 0f) AudioPlayer.seekCurrent(line.time / AudioPlayer.duration)
                    }
                    .padding(horizontal = LinePad, vertical = LinePad * 0.75f),
            ) {
                if (distance == 0) {
                    KaraokeLine(line.text, sungChars(line, now, lineEnd), style, color, MaterialTheme.colorScheme.primary)
                } else {
                    Text(line.text, style = style, color = color)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GapIndicator(gap: LyricItem.Gap, now: Float, modifier: Modifier) {
    val end = gap.end - GapLead
    val visible = now >= gap.start && now <= end
    val shown by animateFloatAsState(if (visible) 1f else 0f, tween(GapFadeMs))
    val progress = if (end > gap.start) ((now - gap.start) / (end - gap.start)).coerceIn(0f, 1f) else 0f
    val smooth by animateFloatAsState(progress, tween(100, easing = androidx.compose.animation.core.LinearEasing))
    val color = MaterialTheme.colorScheme.primary
    Box(
        modifier
            .fillMaxWidth()
            .height(GapHeight * shown)
            .graphicsLayer { alpha = shown; clip = true },
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.CircularWavyProgressIndicator(
            progress = { smooth },
            modifier = Modifier.size(36.dp),
            color = color,
            trackColor = color.copy(alpha = 0.2f),
        )
    }
}

private fun sungChars(line: LyricLine, now: Float, end: Float): Float {
    val text = line.text
    return if (line.words.isNotEmpty()) {
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
}

@Composable
private fun rememberSmoothPosition(): Float {
    var shown by remember { mutableFloatStateOf(AudioPlayer.position) }
    LaunchedEffect(Unit) {
        var last = -1L
        while (true) {
            androidx.compose.runtime.withFrameNanos { time ->
                val dt = if (last < 0) 0f else (time - last) / 1_000_000_000f
                last = time
                val target = AudioPlayer.position
                shown = if (!AudioPlayer.playing) {
                    target
                } else {
                    val predicted = shown + dt * AudioPlayer.speed
                    if (kotlin.math.abs(target - predicted) > ResyncSeconds) target else predicted + (target - predicted) * ClockPull
                }
            }
        }
    }
    return shown
}

private const val ResyncSeconds = 0.35f
private const val ClockPull = 0.06f

@Composable
private fun KaraokeLine(
    text: String,
    sung: Float,
    style: androidx.compose.ui.text.TextStyle,
    color: androidx.compose.ui.graphics.Color,
    glow: androidx.compose.ui.graphics.Color,
) {
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
        val width = constraints.maxWidth
        val layout = remember(text, style, width) {
            measurer.measure(text, style, constraints = androidx.compose.ui.unit.Constraints(maxWidth = width))
        }
        val letters = remember(text, style) {
            var i = 0
            buildList {
                while (i < text.length) {
                    val size = Character.charCount(text.codePointAt(i))
                    val piece = text.substring(i, i + size)
                    if (!piece.isBlank()) add(i to measurer.measure(piece, style, softWrap = false))
                    i += size
                }
            }
        }
        val height = with(androidx.compose.ui.platform.LocalDensity.current) { layout.size.height.toDp() }
        androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(height)) {
            val glowRadius = GlowRadius.toPx()
            letters.forEach { (index, letter) ->
                val box = layout.getBoundingBox(index)
                val line = layout.getLineForOffset(index)
                val top = layout.getLineBaseline(line) - letter.firstBaseline
                val lit = (sung - index).coerceIn(0f, 1f)
                val nudge = if (lit > 0f && lit < 1f) NudgeStrength * kotlin.math.sin(lit * kotlin.math.PI.toFloat()) * kotlin.math.exp(-3f * lit) else 0f
                val w = letter.size.width.toFloat()
                val h = letter.size.height.toFloat()
                translate(box.left, top) {
                    withTransform({
                        scale(1f + nudge * 0.3f, 1f + nudge, pivot = androidx.compose.ui.geometry.Offset(w / 2f, h))
                    }) {
                        when {
                            lit >= 1f -> drawText(letter, color = color)
                            lit <= 0f -> drawText(letter, color = color.copy(alpha = DimAlpha))
                            else -> {
                                val fade = (lit * 5f).coerceIn(0f, 1f) * ((1f - lit) * 8f).coerceIn(0f, 1f)
                                drawText(
                                    letter,
                                    color = color.copy(alpha = DimAlpha),
                                    shadow = if (fade > 0.01f) androidx.compose.ui.graphics.Shadow(glow.copy(alpha = GlowAlpha * fade), blurRadius = glowRadius * fade) else null,
                                )
                                val filled = w * lit
                                val edge = (w * EdgeShare).coerceAtLeast(1f)
                                val solid = (filled - edge).coerceAtLeast(0f)
                                if (solid > 0f) clipRect(-glowRadius, -h, solid, h * 2f) { drawText(letter, color = color) }
                                for (j in 0 until EdgeSlices) {
                                    val from = solid + j * edge / EdgeSlices
                                    val to = (solid + (j + 1) * edge / EdgeSlices + 0.5f).coerceAtMost(filled)
                                    if (to > from) {
                                        val a = DimAlpha + (1f - DimAlpha) * (1f - (j + 0.5f) / EdgeSlices)
                                        clipRect(from, -h, to, h * 2f) { drawText(letter, color = color.copy(alpha = a)) }
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

private val LyricsBleed = 20.dp
private const val CoverSpringDamping = 0.68f
private const val CoverSpringStiffness = 260f
private val MenuWidth = 280.dp
private val LinePad = 10.dp
private val LineSpacing = 10.dp
private val GapHeight = 56.dp
private val GlowRadius = 12.dp
private const val GapSeconds = 4f
private const val GapLead = 0.65f
private const val GapFadeMs = 200
private const val DimAlpha = 0.35f
private const val GlowAlpha = 0.35f
private const val NudgeStrength = 0.038f
private const val EdgeShare = 0.45f
private const val EdgeSlices = 12
private const val LyricsTopFade = 0.1f
private const val FollowPauseMs = 3_000L
private const val LineScrollMs = 650
private const val LineFadeMs = 700
private const val LineSettle = 0.35f
private const val SpinMs = 24_000
private const val CookieStep = 30f
private const val CoverKey = "player-cover"

@Composable
private fun QueueView(telegram: Telegram, tracks: List<QueueTrack>, current: Int) {
    val state = rememberLazyListState()
    LaunchedEffect(Unit) { if (current > 0) state.scrollToItem((current - 2).coerceAtLeast(0)) }
    var draggingKey by remember { mutableStateOf<Long?>(null) }
    var offset by remember { mutableFloatStateOf(0f) }
    val latest by androidx.compose.runtime.rememberUpdatedState(tracks)
    val rowPx = with(androidx.compose.ui.platform.LocalDensity.current) { QueueRow.toPx() }
    val placement = MaterialTheme.motionScheme.defaultSpatialSpec<androidx.compose.ui.unit.IntOffset>()
    LazyColumn(state = state, modifier = Modifier.fillMaxSize().listScrollbar(state)) {
        itemsIndexed(tracks, key = { _, track -> track.key }) { position, track ->
            val lifted = track.key == draggingKey
            val playing = position == current
            val rowColor by androidx.compose.animation.animateColorAsState(
                when {
                    lifted -> MaterialTheme.colorScheme.surfaceContainerHighest
                    playing -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0f)
                },
                MaterialTheme.motionScheme.defaultEffectsSpec(),
            )
            val elevation by animateFloatAsState(if (lifted) 1f else 0f, animationSpec = com.pixelgram.desktop.ui.components.Motion.effects())
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .then(if (lifted) Modifier else Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null, placementSpec = placement))
                    .zIndex(if (lifted) 1f else 0f)
                    .graphicsLayer {
                        translationY = if (lifted) offset else 0f
                        scaleX = 1f + elevation * 0.03f
                        scaleY = 1f + elevation * 0.03f
                        shadowElevation = elevation * 12f
                        shape = RoundedCornerShape(18.dp)
                        clip = true
                    }
                    .fillMaxWidth()
                    .height(QueueRow)
                    .background(rowColor)
                    .clickable(enabled = draggingKey == null) { AudioQueue.playAt(telegram, track) }
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
                        .pointerInput(track.key) {
                            detectDragGestures(
                                onDragStart = {
                                    draggingKey = track.key
                                    offset = 0f
                                },
                                onDragEnd = { draggingKey = null; offset = 0f },
                                onDragCancel = { draggingKey = null; offset = 0f },
                            ) { change, amount ->
                                change.consume()
                                offset += amount.y
                                val list = latest
                                val from = list.indexOfFirst { it.key == track.key }
                                if (from < 0) return@detectDragGestures
                                val steps = (offset / rowPx).roundToInt()
                                if (steps != 0) {
                                    val target = (from + steps).coerceIn(0, list.lastIndex)
                                    if (target != from) {
                                        AudioQueue.reorder(telegram, from, target)
                                        offset -= (target - from) * rowPx
                                    }
                                }
                            }
                        },
                )
            }
        }
    }
}

@Composable
private fun animatedScheme(target: androidx.compose.material3.ColorScheme): androidx.compose.material3.ColorScheme {
    val spec = tween<androidx.compose.ui.graphics.Color>(SchemeFadeMs)
    @Composable
    fun c(color: androidx.compose.ui.graphics.Color) = androidx.compose.animation.animateColorAsState(color, spec).value
    return target.copy(
        primary = c(target.primary),
        onPrimary = c(target.onPrimary),
        primaryContainer = c(target.primaryContainer),
        onPrimaryContainer = c(target.onPrimaryContainer),
        secondary = c(target.secondary),
        secondaryContainer = c(target.secondaryContainer),
        onSecondaryContainer = c(target.onSecondaryContainer),
        tertiary = c(target.tertiary),
        surface = c(target.surface),
        onSurface = c(target.onSurface),
        onSurfaceVariant = c(target.onSurfaceVariant),
        surfaceContainerLow = c(target.surfaceContainerLow),
        surfaceContainer = c(target.surfaceContainer),
        surfaceContainerHigh = c(target.surfaceContainerHigh),
        surfaceContainerHighest = c(target.surfaceContainerHighest),
        inverseSurface = c(target.inverseSurface),
        inverseOnSurface = c(target.inverseOnSurface),
    )
}

private const val SchemeFadeMs = 900

@Composable
private fun PlayerMenu(telegram: Telegram) {
    val audio = AudioPlayer.audio
    if (audio == null) {
        Spacer(Modifier.width(48.dp))
        return
    }
    val settings = com.pixelgram.desktop.ui.theme.LocalAppSettings.current
    var open by remember { mutableStateOf(false) }
    var details by remember { mutableStateOf(false) }
    var forwarding by remember { mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val path = telegram.localPath(audio.audio)
    val name = audio.fileName.ifBlank { "${audio.performer} - ${audio.title}.mp3" }
    val saved = telegram.isInProfile(audio)
    Box {
        IconButton(onClick = {
            details = false
            open = true
        }) { Icon(Icons.Filled.MoreVert, contentDescription = "More") }
        com.pixelgram.desktop.ui.components.GroupedMenu(expanded = open, onDismiss = { open = false }) {
            AnimatedContent(details, transitionSpec = { (fadeIn(com.pixelgram.desktop.ui.components.Motion.effects()) togetherWith fadeOut(com.pixelgram.desktop.ui.components.Motion.fastEffects())).using(null) }, modifier = Modifier.width(MenuWidth), contentAlignment = Alignment.TopEnd) { showDetails ->
                if (!showDetails) {
                    com.pixelgram.desktop.ui.components.MenuGroups(
                        listOf(
                            listOf(
                                com.pixelgram.desktop.ui.components.MenuEntry(Icons.AutoMirrored.Outlined.Forward, "Forward") {
                                    open = false
                                    forwarding = true
                                },
                                com.pixelgram.desktop.ui.components.MenuEntry(Icons.Outlined.Share, "Share", enabled = path != null) {
                                    open = false
                                    path?.let { com.pixelgram.desktop.core.SystemShare.share(java.io.File(it), ::showInFolder) }
                                },
                                com.pixelgram.desktop.ui.components.MenuEntry(Icons.Outlined.Download, "Save to Music", enabled = path != null) {
                                    open = false
                                    path?.let { source -> scope.launch(Dispatchers.IO) { saveToMusic(java.io.File(source), name) } }
                                },
                            ),
                            listOf(
                                if (saved) {
                                    com.pixelgram.desktop.ui.components.MenuEntry(Icons.Outlined.Delete, "Remove from profile", color = MaterialTheme.colorScheme.error) {
                                        open = false
                                        telegram.toggleProfileAudio(audio)
                                    }
                                } else {
                                    com.pixelgram.desktop.ui.components.MenuEntry(Icons.Filled.Favorite, "Add to profile") {
                                        open = false
                                        telegram.toggleProfileAudio(audio)
                                    }
                                },
                            ),
                            listOf(com.pixelgram.desktop.ui.components.MenuEntry(Icons.Outlined.Info, "Details") { details = true }),
                        ),
                        width = MenuWidth - 4.dp,
                    )
                } else {
                    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.padding(2.dp)) {
                        TrackDetails(audio, path, name) { details = false }
                    }
                }
            }
        }
    }
    if (forwarding && settings != null) {
        ForwardDialog(telegram, settings, onDismiss = { forwarding = false }) { chatId ->
            forwarding = false
            telegram.sendAudio(chatId, audio)
        }
    }
}

@Composable
private fun TrackDetails(audio: TdApi.Audio, path: String?, name: String, onBack: () -> Unit) {
    val size = maxOf(audio.audio.size, audio.audio.expectedSize)
    val seconds = audio.duration.takeIf { it > 0 } ?: AudioPlayer.duration.toInt()
    val rows = listOfNotNull(
        "Format" to (name.substringAfterLast('.', "").ifBlank { audio.mimeType.substringAfter('/') }).uppercase(),
        if (size > 0 && seconds > 0) "Bitrate" to "${size * 8 / seconds / 1000} kbps" else null,
        AudioPlayer.sampleRate.takeIf { it > 0 }?.let { "Sample rate" to "%.1f kHz".format(it / 1000f) },
        if (size > 0) "File size" to formatSize(size) else null,
        "Duration" to formatDuration(seconds),
        "File name" to name,
        path?.let { "File path" to it },
    )
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onBack).padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, modifier = Modifier.rotate(90f))
            Text("Back", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 12.dp))
        }
        rows.forEach { (label, value) ->
            Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                Text(label, style = MaterialTheme.typography.titleSmall)
                Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private fun saveToMusic(source: java.io.File, name: String) {
    runCatching {
        val home = java.io.File(System.getProperty("user.home"))
        val music = listOf("Music", "Музыка").map { java.io.File(home, it) }.firstOrNull { it.isDirectory } ?: java.io.File(home, "Music")
        val target = java.io.File(java.io.File(music, "pixelGram").apply { mkdirs() }, com.pixelgram.desktop.core.FileSafety.clean(name))
        source.copyTo(target, overwrite = true)
    }.onFailure { com.pixelgram.desktop.core.AppLog.write("Save to music failed", it) }
}

object PlayerKeys {
    var armed by mutableStateOf(false)
}

private class TrackSlot(val audio: TdApi.Audio?, val index: Int)

private class TrackText(val title: String, val performer: String?, val index: Int)

private const val GroupIdleAlpha = 0.55f
