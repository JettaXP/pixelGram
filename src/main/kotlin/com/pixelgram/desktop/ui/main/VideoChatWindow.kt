package com.pixelgram.desktop.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import androidx.graphics.shapes.Morph
import com.pixelgram.desktop.calls.ActiveVideoChat
import com.pixelgram.desktop.calls.CallLibrary
import com.pixelgram.desktop.calls.VideoChatPhase
import com.pixelgram.desktop.calls.VideoChats
import com.pixelgram.desktop.calls.senderKey
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.components.AvatarImage
import com.pixelgram.desktop.ui.components.ChatAvatar
import com.pixelgram.desktop.ui.components.ConfirmOnEnter
import com.pixelgram.desktop.ui.components.GroupedMenu
import com.pixelgram.desktop.ui.components.MenuEntry
import com.pixelgram.desktop.ui.components.MenuGroups
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.components.expressive.AlertDialog
import com.pixelgram.desktop.ui.components.expressive.IconButton
import com.pixelgram.desktop.ui.components.expressive.Slider
import com.pixelgram.desktop.ui.components.expressive.TextButton
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.filled.CallEnd
import com.pixelgram.desktop.ui.icons.filled.Mic
import com.pixelgram.desktop.ui.icons.filled.MicOff
import com.pixelgram.desktop.ui.icons.filled.MoreVert
import com.pixelgram.desktop.ui.icons.filled.PanTool
import com.pixelgram.desktop.ui.icons.filled.Sensors
import com.pixelgram.desktop.ui.icons.filled.Videocam
import com.pixelgram.desktop.ui.icons.filled.VideocamOff
import com.pixelgram.desktop.ui.icons.filled.VolumeUp
import com.pixelgram.desktop.ui.icons.outlined.CallEnd
import com.pixelgram.desktop.ui.icons.outlined.Key
import com.pixelgram.desktop.ui.icons.outlined.Mic
import com.pixelgram.desktop.ui.icons.outlined.MicOff
import com.pixelgram.desktop.ui.icons.outlined.PanTool
import com.pixelgram.desktop.ui.theme.EmojiText
import com.pixelgram.desktop.ui.theme.PixelTheme
import it.tdlight.jni.TdApi
import kotlinx.coroutines.delay

private val VideoChatSize = DpSize(960.dp, 680.dp)
private val TileAvatar = 88.dp
private val RowAvatar = 40.dp
private val MainControl = 76.dp
private val SideControl = 56.dp
private val PanelWidth = 300.dp
private val WideLayout = 760.dp
private const val FullVolume = 10_000
private const val MaxVolume = 20_000
private const val TickMs = 1000L
private const val SpinMs = 9_000

@Composable
fun VideoChatWindow(settings: AppSettings, icon: Painter?) {
    val call = VideoChats.current ?: return
    key(call) {
        val telegram = call.telegram
        val chat = telegram.chats[call.chatId]
        val title = call.call?.title?.takeIf { it.isNotBlank() } ?: chat?.let { telegram.chatTitle(it) } ?: "Video chat"
        val state = rememberWindowState(size = VideoChatSize, position = WindowPosition(Alignment.Center))
        Window(onCloseRequest = VideoChats::leave, title = title, icon = icon, state = state) {
            LaunchedEffect(VideoChats.raise) { if (VideoChats.raise > 0) window.toFront() }
            PixelTheme(settings) {
                VideoChatContent(call, title, settings)
            }
        }
    }
}

private class VideoTile(val key: String, val frame: ImageBitmap, val participant: TdApi.GroupCallParticipant?, val mirror: Boolean, val label: String, val screen: Boolean = false)

@Composable
private fun VideoChatContent(call: ActiveVideoChat, title: String, settings: AppSettings) {
    val colors = MaterialTheme.colorScheme
    var ending by remember { mutableStateOf(false) }
    var streamKey by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(colors.surfaceContainerLowest)) {
        Column(Modifier.fillMaxSize()) {
            Header(call, title, settings, onEnd = { ending = true }, onStreamKey = { streamKey = true })
            Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp)) {
                if (call.stream || call.call?.isRtmpStream == true) StreamStage(call) else Stage(call)
                PhaseOverlay(call, Modifier.align(Alignment.Center))
            }
            Controls(call, Modifier.align(Alignment.CenterHorizontally).padding(vertical = 20.dp))
        }
    }
    if (ending) {
        val stream = call.call?.isRtmpStream == true || call.stream
        val confirm = {
            ending = false
            VideoChats.endForAll()
        }
        AlertDialog(
            onDismissRequest = { ending = false },
            title = { Text(if (stream) "End live stream?" else "End video chat?") },
            text = {
                ConfirmOnEnter(confirm) {
                    Text(if (stream) "The live stream will end for all viewers." else "The video chat will end for all participants.")
                }
            },
            confirmButton = { TextButton(onClick = confirm) { Text("End", color = colors.error) } },
            dismissButton = { TextButton(onClick = { ending = false }) { Text("Cancel") } },
        )
    }
    if (streamKey) {
        call.telegram.chats[call.chatId]?.let { chat -> RtmpDialog(chat, call.telegram) { streamKey = false } }
    }
}

@Composable
private fun Header(call: ActiveVideoChat, title: String, settings: AppSettings, onEnd: () -> Unit, onStreamKey: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val telegram = call.telegram
    val stream = call.stream || call.call?.isRtmpStream == true
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        telegram.chats[call.chatId]?.let { chat -> ChatAvatar(chat, telegram, settings, size = 44.dp) }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EmojiText(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (stream && call.phase != VideoChatPhase.Ended) LiveChip()
            }
            Text(subtitle(call), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1)
        }
        if (call.call?.canBeManaged == true) {
            var menu by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Video chat menu")
                }
                GroupedMenu(expanded = menu, onDismiss = { menu = false }) {
                    MenuGroups(listOf(
                        if (call.call?.isRtmpStream == true) listOf(MenuEntry(Icons.Outlined.Key, "Stream key") {
                            menu = false
                            onStreamKey()
                        }) else emptyList(),
                        listOf(MenuEntry(Icons.Outlined.CallEnd, if (stream) "End live stream" else "End video chat", color = colors.error) {
                            menu = false
                            onEnd()
                        }),
                    ))
                }
            }
        }
    }
}

@Composable
private fun subtitle(call: ActiveVideoChat): String {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(call.phase) {
        while (call.phase == VideoChatPhase.Active) {
            now = System.currentTimeMillis()
            delay(TickMs)
        }
    }
    val stream = call.stream || call.call?.isRtmpStream == true
    val count = maxOf(call.call?.participantCount ?: 0, call.participants.size)
    val people = when {
        stream -> com.pixelgram.desktop.core.tr(if (count == 1) "1 watching" else "$count watching")
        count == 1 -> com.pixelgram.desktop.core.tr("1 participant")
        else -> com.pixelgram.desktop.core.tr("$count participants")
    }
    if (call.phase != VideoChatPhase.Active || call.startedAt == 0L) return people
    val seconds = ((now - call.startedAt).coerceAtLeast(0) / 1000).toInt()
    val clock = if (seconds >= 3600) "%d:%02d:%02d".format(seconds / 3600, seconds / 60 % 60, seconds % 60) else "%d:%02d".format(seconds / 60, seconds % 60)
    return "$people · $clock"
}

@Composable
private fun LiveChip() {
    val colors = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(8.dp), color = colors.error) {
        Text(
            "LIVE",
            style = MaterialTheme.typography.labelMedium,
            color = colors.onError,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun PhaseOverlay(call: ActiveVideoChat, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val text = when (call.phase) {
        VideoChatPhase.Joining -> "Joining…"
        VideoChatPhase.Connecting -> "Connecting…"
        VideoChatPhase.Ended -> call.status ?: "Video chat ended"
        VideoChatPhase.Active -> null
    }
    AnimatedVisibility(text != null || CallLibrary.downloading, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        Surface(shape = RoundedCornerShape(24.dp), color = colors.surfaceContainerHigh, shadowElevation = 4.dp) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp).width(260.dp)) {
                Text(text ?: "Downloading call components…", style = MaterialTheme.typography.titleMedium)
                if (CallLibrary.downloading) {
                    LinearProgressIndicator(progress = { CallLibrary.progress }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
                }
            }
        }
    }
}

private fun sortedParticipants(call: ActiveVideoChat): List<TdApi.GroupCallParticipant> =
    call.participants.values.sortedWith(compareByDescending<TdApi.GroupCallParticipant> { it.order.orEmpty().length }.thenByDescending { it.order.orEmpty() })

private fun name(telegram: Telegram, participant: TdApi.GroupCallParticipant): String {
    if (participant.isCurrentUser) return com.pixelgram.desktop.core.tr("You")
    return telegram.senderTitle(participant.participantId).ifBlank { "…" }
}

private fun avatarTitle(telegram: Telegram, participant: TdApi.GroupCallParticipant): String = when (val sender = participant.participantId) {
    is TdApi.MessageSenderUser -> {
        val user = if (participant.isCurrentUser) telegram.me else telegram.users[sender.userId]
        listOfNotNull(user?.firstName, user?.lastName).joinToString(" ").ifBlank { name(telegram, participant) }
    }
    else -> name(telegram, participant)
}

private fun photo(telegram: Telegram, participant: TdApi.GroupCallParticipant): TdApi.File? = when (val sender = participant.participantId) {
    is TdApi.MessageSenderUser -> (if (participant.isCurrentUser) telegram.me else telegram.users[sender.userId])?.profilePhoto?.small
    is TdApi.MessageSenderChat -> telegram.chats[sender.chatId]?.photo
    else -> null
}

private fun statusText(participant: TdApi.GroupCallParticipant): String = when {
    participant.isHandRaised -> "wants to speak"
    participant.isSpeaking -> "speaking"
    participant.isMutedForCurrentUser -> "muted for you"
    participant.isMutedForAllUsers && !participant.canUnmuteSelf -> "muted"
    else -> "listening"
}

@Composable
private fun Stage(call: ActiveVideoChat) {
    val participants by remember(call) { derivedStateOf { sortedParticipants(call) } }
    val tiles = videoTiles(call, participants)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= WideLayout
        when {
            tiles.isEmpty() -> ParticipantGrid(call, participants)
            wide -> Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize()) {
                VideoGrid(call, tiles, Modifier.weight(1f).fillMaxHeight())
                ParticipantList(call, participants, Modifier.width(PanelWidth).fillMaxHeight())
            }
            else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                VideoGrid(call, tiles, Modifier.weight(1f).fillMaxWidth())
                ParticipantList(call, participants, Modifier.fillMaxWidth().height(200.dp))
            }
        }
    }
}

@Composable
private fun videoTiles(call: ActiveVideoChat, participants: List<TdApi.GroupCallParticipant>): List<VideoTile> {
    val telegram = call.telegram
    val byKey = participants.associateBy { senderKey(it.participantId) }
    val me = participants.firstOrNull { it.isCurrentUser }
    return buildList {
        call.cameraFrame?.takeIf { call.camera }?.let { add(VideoTile("camera", it, me, mirror = true, label = com.pixelgram.desktop.core.tr("You"))) }
        call.screenFrame?.takeIf { call.screen != null }?.let { add(VideoTile("screen", it, me, mirror = false, label = com.pixelgram.desktop.core.tr("Your screen"), screen = true)) }
        participants.forEach { participant ->
            val owner = byKey[senderKey(participant.participantId)] ?: participant
            participant.videoInfo?.let { info ->
                call.frames[info.endpointId]?.let { add(VideoTile(info.endpointId, it, owner, mirror = false, label = name(telegram, owner))) }
            }
            participant.screenSharingVideoInfo?.let { info ->
                call.frames[info.endpointId]?.let { add(VideoTile(info.endpointId, it, owner, mirror = false, label = name(telegram, owner), screen = true)) }
            }
        }
    }
}

@Composable
private fun VideoGrid(call: ActiveVideoChat, tiles: List<VideoTile>, modifier: Modifier) {
    BoxWithConstraints(modifier) {
        val columns = when {
            tiles.size <= 1 -> 1
            tiles.size <= 4 -> 2
            else -> 3
        }
        val rows = (tiles.size + columns - 1) / columns
        val gap = 12.dp
        val tileHeight = ((maxHeight - gap * (rows - 1)) / rows).coerceAtLeast(120.dp)
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalArrangement = Arrangement.spacedBy(gap),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(tiles, key = { it.key }) { tile -> VideoTileView(call, tile, Modifier.fillMaxWidth().height(tileHeight)) }
        }
    }
}

@Composable
private fun VideoTileView(call: ActiveVideoChat, tile: VideoTile, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val speaking = tile.participant?.isSpeaking == true && !tile.screen
    val border by animateColorAsState(if (speaking) colors.primary else Color.Transparent)
    var menu by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier
            .clip(shape)
            .background(Color.Black)
            .border(3.dp, border, shape)
            .participantClicks(tile.participant) { menu = true },
    ) {
        Image(
            tile.frame,
            contentDescription = tile.label,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().graphicsLayer { if (tile.mirror) scaleX = -1f },
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.Black.copy(alpha = 0.55f),
            modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                tile.participant?.takeIf { !tile.screen }?.let { participant ->
                    Icon(
                        if (participant.isMutedForAllUsers || participant.isMutedForCurrentUser) Icons.Filled.MicOff else Icons.Filled.Mic,
                        contentDescription = null,
                        tint = if (speaking) colors.primaryContainer else Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
                EmojiText(tile.label, style = MaterialTheme.typography.labelLarge, color = Color.White, maxLines = 1)
            }
        }
        tile.participant?.let { participant -> ParticipantMenu(call, participant, menu) { menu = false } }
    }
}

@Composable
private fun ParticipantGrid(call: ActiveVideoChat, participants: List<TdApi.GroupCallParticipant>) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(168.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(participants, key = { senderKey(it.participantId) }) { participant -> ParticipantTile(call, participant) }
    }
}

@Composable
private fun ParticipantTile(call: ActiveVideoChat, participant: TdApi.GroupCallParticipant) {
    val colors = MaterialTheme.colorScheme
    var menu by remember { mutableStateOf(false) }
    val container by animateColorAsState(if (participant.isSpeaking) colors.surfaceContainerHigh else colors.surfaceContainer)
    Box {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = container,
            modifier = Modifier.fillMaxWidth().aspectRatio(0.92f).participantClicks(participant) { menu = true },
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize().padding(12.dp),
            ) {
                SpeakerAvatar(call.telegram, participant, TileAvatar)
                Spacer(Modifier.height(12.dp))
                EmojiText(name(call.telegram, participant), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                StatusLine(participant)
            }
        }
        ParticipantMenu(call, participant, menu) { menu = false }
    }
}

@Composable
private fun StatusLine(participant: TdApi.GroupCallParticipant) {
    val colors = MaterialTheme.colorScheme
    val highlight = participant.isSpeaking || participant.isHandRaised
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        val icon = when {
            participant.isHandRaised -> Icons.Filled.PanTool
            participant.isMutedForAllUsers || participant.isMutedForCurrentUser -> Icons.Filled.MicOff
            else -> Icons.Filled.Mic
        }
        Icon(icon, contentDescription = null, tint = if (highlight) colors.primary else colors.onSurfaceVariant, modifier = Modifier.size(14.dp))
        Text(statusText(participant), style = MaterialTheme.typography.bodySmall, color = if (highlight) colors.primary else colors.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
private fun SpeakerAvatar(telegram: Telegram, participant: TdApi.GroupCallParticipant, size: Dp) {
    val colors = MaterialTheme.colorScheme
    val speaking = participant.isSpeaking
    val progress by animateFloatAsState(if (speaking) 1f else 0f, MaterialTheme.motionScheme.defaultSpatialSpec())
    val spin = rememberInfiniteTransition()
    val turn by spin.animateFloat(0f, 360f, infiniteRepeatable(tween(SpinMs, easing = LinearEasing)))
    val pulse by spin.animateFloat(1f, 1.06f, infiniteRepeatable(tween(700), RepeatMode.Reverse))
    val morph = remember { Morph(MaterialShapes.Circle, MaterialShapes.Cookie12Sided) }
    val outline = remember(progress) { MorphShape(morph, progress) }
    val halo by animateColorAsState(if (speaking) colors.primary else colors.surfaceContainerHighest)
    val ring = size * 0.14f
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size + ring)) {
        Box(
            Modifier
                .size(size + ring)
                .graphicsLayer {
                    val scale = 1f + (pulse - 1f) * progress
                    scaleX = scale
                    scaleY = scale
                    rotationZ = turn * progress
                }
                .clip(outline)
                .background(halo),
        )
        AvatarImage(avatarTitle(telegram, participant), photo(telegram, participant), telegram, CircleShape, size - ring * 0.2f)
        if (participant.isHandRaised) {
            Badge(Icons.Filled.PanTool, colors.tertiaryContainer, colors.onTertiaryContainer, size, Modifier.align(Alignment.BottomEnd))
        } else if (participant.isMutedForAllUsers && !participant.canUnmuteSelf) {
            Badge(Icons.Filled.MicOff, colors.errorContainer, colors.onErrorContainer, size, Modifier.align(Alignment.BottomEnd))
        }
    }
}

@Composable
private fun Badge(icon: ImageVector, container: Color, content: Color, size: Dp, modifier: Modifier) {
    val badge = (size * 0.34f).coerceAtLeast(18.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(badge)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest, MaterialShapes.Cookie9Sided.toShape())
            .padding(2.dp)
            .background(container, MaterialShapes.Cookie9Sided.toShape()),
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(badge * 0.55f))
    }
}

@Composable
private fun ParticipantList(call: ActiveVideoChat, participants: List<TdApi.GroupCallParticipant>, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val list = rememberLazyListState()
    val nearEnd by remember { derivedStateOf { list.layoutInfo.visibleItemsInfo.lastOrNull()?.index?.let { it >= list.layoutInfo.totalItemsCount - 5 } == true } }
    LaunchedEffect(nearEnd) { if (nearEnd) VideoChats.loadMore() }
    Surface(shape = RoundedCornerShape(28.dp), color = colors.surfaceContainer, modifier = modifier) {
        LazyColumn(state = list, modifier = Modifier.fillMaxSize().padding(vertical = 8.dp)) {
            items(participants, key = { senderKey(it.participantId) }) { participant ->
                var menu by remember { mutableStateOf(false) }
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .participantClicks(participant) { menu = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        SpeakerAvatar(call.telegram, participant, RowAvatar)
                        Column(Modifier.weight(1f)) {
                            EmojiText(name(call.telegram, participant), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            StatusLine(participant)
                        }
                    }
                    ParticipantMenu(call, participant, menu) { menu = false }
                }
            }
        }
    }
}

private fun Modifier.participantClicks(participant: TdApi.GroupCallParticipant?, open: () -> Unit): Modifier {
    if (participant == null || participant.isCurrentUser) return this
    return this
        .pointerInput(participant.participantId) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                        event.changes.first().consume()
                        open()
                    }
                }
            }
        }
        .focusProperties { canFocus = false }
        .clickable(onClick = open)
}

@Composable
private fun ParticipantMenu(call: ActiveVideoChat, participant: TdApi.GroupCallParticipant, expanded: Boolean, onDismiss: () -> Unit) {
    if (!expanded || participant.isCurrentUser) return
    val manage = call.call?.canBeManaged == true
    val act = { action: () -> Unit -> { onDismiss(); action() } }
    GroupedMenu(expanded = true, onDismiss = onDismiss) {
        val entries = buildList {
            when {
                participant.canBeUnmutedForAllUsers -> add(MenuEntry(Icons.Outlined.Mic, "Allow to speak", onClick = act { VideoChats.setMuted(participant, false) }))
                participant.canBeMutedForAllUsers -> add(MenuEntry(Icons.Outlined.MicOff, "Mute for everyone", onClick = act { VideoChats.setMuted(participant, true) }))
            }
            if (!participant.canBeMutedForAllUsers && !participant.canBeUnmutedForAllUsers) {
                when {
                    participant.canBeUnmutedForCurrentUser -> add(MenuEntry(Icons.Outlined.Mic, "Unmute for me", onClick = act { VideoChats.setMuted(participant, false) }))
                    participant.canBeMutedForCurrentUser -> add(MenuEntry(Icons.Outlined.MicOff, "Mute for me", onClick = act { VideoChats.setMuted(participant, true) }))
                }
            }
            if (participant.isHandRaised && manage) add(MenuEntry(Icons.Outlined.PanTool, "Lower hand", onClick = act { VideoChats.lowerHand(participant) }))
        }
        MenuGroups(listOf(entries), leading = { VolumeSlider(participant) })
    }
}

@Composable
private fun VolumeSlider(participant: TdApi.GroupCallParticipant) {
    val colors = MaterialTheme.colorScheme
    var level by remember(participant.participantId) { mutableFloatStateOf((participant.volumeLevel.takeIf { it > 0 } ?: FullVolume).toFloat()) }
    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.VolumeUp, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
            Text("Volume", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Text("${(level / 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = colors.primary)
        }
        Slider(
            value = level,
            onValueChange = { level = it },
            valueRange = 100f..MaxVolume.toFloat(),
            onValueChangeFinished = { VideoChats.setVolume(participant, level.toInt()) },
        )
    }
}

@Composable
private fun StreamStage(call: ActiveVideoChat) {
    val colors = MaterialTheme.colorScheme
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp)).background(Color.Black),
    ) {
        val frame = call.streamFrame
        if (frame != null) {
            Image(frame, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        } else if (call.phase == VideoChatPhase.Active) {
            val spin = rememberInfiniteTransition()
            val turn by spin.animateFloat(0f, 360f, infiniteRepeatable(tween(SpinMs, easing = LinearEasing)))
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(96.dp)) {
                    Box(Modifier.fillMaxSize().graphicsLayer { rotationZ = turn }.background(colors.primaryContainer, MaterialShapes.Cookie12Sided.toShape()))
                    Icon(Icons.Filled.Sensors, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(40.dp))
                }
                Text(if (call.stream) "Waiting for the stream…" else "The stream has no video yet", style = MaterialTheme.typography.titleMedium, color = Color.White)
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
        ) {
            LiveChip()
            Surface(shape = RoundedCornerShape(8.dp), color = Color.Black.copy(alpha = 0.55f)) {
                val count = call.call?.participantCount ?: 0
                Text(
                    com.pixelgram.desktop.core.tr(if (count == 1) "1 watching" else "$count watching"),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun Controls(call: ActiveVideoChat, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val stream = call.stream || call.call?.isRtmpStream == true
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top, modifier = modifier) {
        if (!stream) {
            Labeled(if (call.camera) "Stop video" else "Video") {
                CallToggle(if (call.camera) Icons.Filled.Videocam else Icons.Filled.VideocamOff, if (call.camera) "Turn camera off" else "Turn camera on", active = call.camera, onClick = VideoChats::toggleCamera)
            }
            Labeled(if (call.screen != null) "Stop sharing" else "Screen") {
                ScreenShareButton(call.screen != null, VideoChats::screens, VideoChats::share)
            }
            MainMicButton(call)
            Labeled(if (call.handRaised) "Lower hand" else "Raise hand") {
                CallToggle(Icons.Filled.PanTool, if (call.handRaised) "Lower hand" else "Raise hand", active = call.handRaised, onClick = VideoChats::toggleHand)
            }
        }
        Labeled("Leave") {
            RoundAction(Icons.Filled.CallEnd, "Leave", SideControl, colors.error, colors.onError, onClick = VideoChats::leave)
        }
    }
}

@Composable
private fun MainMicButton(call: ActiveVideoChat) {
    val colors = MaterialTheme.colorScheme
    val speak = call.canSpeak
    val live = !call.muted && speak
    val container by animateColorAsState(
        when {
            !speak && call.handRaised -> colors.tertiary
            !speak -> colors.tertiaryContainer
            live -> colors.primary
            else -> colors.secondaryContainer
        }
    )
    val content by animateColorAsState(
        when {
            !speak && call.handRaised -> colors.onTertiary
            !speak -> colors.onTertiaryContainer
            live -> colors.onPrimary
            else -> colors.onSecondaryContainer
        }
    )
    val label = when {
        !speak && call.handRaised -> "Hand raised"
        !speak -> "Raise hand"
        live -> "Mute"
        else -> "Unmute"
    }
    Labeled(label) {
        RoundAction(
            if (!speak) Icons.Filled.PanTool else if (live) Icons.Filled.Mic else Icons.Filled.MicOff,
            label,
            MainControl,
            container,
            content,
            active = live,
            onClick = if (speak) VideoChats::toggleMute else VideoChats::toggleHand,
        )
    }
}

@Composable
private fun Labeled(label: String, content: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.width(96.dp)) {
        Box(Modifier.height(MainControl), contentAlignment = Alignment.Center) { content() }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
