package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.pixelgram.desktop.ui.components.expressive.AlertDialog
import com.pixelgram.desktop.ui.components.expressive.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import com.pixelgram.desktop.ui.components.expressive.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.components.expressive.IconButton
import com.pixelgram.desktop.ui.components.groupShapes
import com.pixelgram.desktop.ui.components.loadImage
import com.pixelgram.desktop.ui.icons.Icons
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.pixelgram.desktop.ui.icons.outlined.Visibility
import com.pixelgram.desktop.ui.icons.outlined.FavoriteBorder
import com.pixelgram.desktop.ui.icons.filled.Favorite
import com.pixelgram.desktop.ui.icons.automirrored.filled.Send
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onPreviewKeyEvent
import com.pixelgram.desktop.ui.icons.filled.PlayArrow
import com.pixelgram.desktop.ui.icons.outlined.Add
import com.pixelgram.desktop.ui.icons.outlined.Close
import com.pixelgram.desktop.ui.icons.outlined.History
import com.pixelgram.desktop.ui.theme.EmojiText
import it.tdlight.jni.TdApi
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val StoryColumns = 4
private const val StoryRatio = 9f / 16f
private const val PhotoStoryMs = 5000L
private const val StoryTickMs = 50L
private const val StoryImageSize = 1280
private val StoryGap = 4.dp
private val StoryCorner = 12.dp
private val ViewerMaxWidth = 460.dp
private val DateFormat = DateTimeFormatter.ofPattern("d MMM, HH:mm")

private fun storyThumb(story: TdApi.Story): Pair<TdApi.File?, TdApi.Minithumbnail?> = when (val content = story.content) {
    is TdApi.StoryContentPhoto -> content.photo.sizes.minByOrNull { kotlin.math.abs(it.width - 320) }?.photo to content.photo.minithumbnail
    is TdApi.StoryContentVideo -> content.video.thumbnail?.file to content.video.minithumbnail
    else -> null to null
}

private fun storyDuration(story: TdApi.Story): Double? = (story.content as? TdApi.StoryContentVideo)?.video?.duration

private fun clock(seconds: Double): String {
    val total = seconds.toInt()
    return "%02d:%02d".format(total / 60, total % 60)
}

@Composable
fun ProfileStories(chatId: Long, telegram: Telegram, own: Boolean, modifier: Modifier = Modifier) {
    var albums by remember(chatId) { mutableStateOf<List<TdApi.StoryAlbum>>(emptyList()) }
    var selected by remember(chatId) { mutableIntStateOf(0) }
    var stories by remember(chatId) { mutableStateOf<List<TdApi.Story>?>(null) }
    var adding by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }
    LaunchedEffect(chatId, reload) { telegram.storyAlbums(chatId) { albums = it } }
    LaunchedEffect(chatId, selected, albums) {
        val album = albums.getOrNull(selected - 1)
        if (album == null) telegram.postedStories(chatId) { stories = it } else telegram.albumStories(chatId, album.id) { stories = it }
    }
    if (!own && albums.isEmpty() && stories.isNullOrEmpty()) return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AlbumTabs(listOf("All Stories") + albums.map { it.name }, selected, onSelect = { selected = it }, addLabel = if (own) "Add Album" else null, onAdd = { adding = true })
        StoryGrid(stories.orEmpty(), telegram)
        if (stories?.isEmpty() == true) {
            Text(
                "No stories here yet",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(8.dp),
            )
        }
    }
    if (adding) {
        NameDialog("New album", "Album name", onDismiss = { adding = false }) { name, done ->
            telegram.createStoryAlbum(chatId, name) { error ->
                if (error == null) reload++
                done(error)
            }
        }
    }
}

@Composable
fun AlbumTabs(tabs: List<String>, selected: Int, onSelect: (Int) -> Unit, addLabel: String?, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    PillTabs(tabs, selected, onSelect, modifier) {
        if (addLabel != null) {
            Surface(
                onClick = onAdd,
                shape = CircleShape,
                color = Color.Transparent,
                contentColor = colors.primary,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineVariant),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(start = 10.dp, end = 14.dp, top = 7.dp, bottom = 7.dp),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(addLabel, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun StoryGrid(stories: List<TdApi.Story>, telegram: Telegram) {
    var viewing by remember { mutableStateOf<Int?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(StoryGap)) {
        stories.chunked(StoryColumns).forEachIndexed { row, chunk ->
            Row(horizontalArrangement = Arrangement.spacedBy(StoryGap)) {
                chunk.forEachIndexed { column, story ->
                    StoryCell(story, telegram, Modifier.weight(1f)) { viewing = row * StoryColumns + column }
                }
                repeat(StoryColumns - chunk.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
    viewing?.let { index -> StoryViewer(stories, index, telegram) { viewing = null } }
}

@Composable
private fun StoryCell(story: TdApi.Story, telegram: Telegram, modifier: Modifier, onClick: () -> Unit) {
    val (file, mini) = storyThumb(story)
    Box(
        modifier
            .aspectRatio(StoryRatio)
            .clip(RoundedCornerShape(StoryCorner))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(onClick = onClick),
    ) {
        ThumbImage(file, mini, telegram, Modifier.fillMaxSize())
        storyDuration(story)?.let { seconds ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(start = 4.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Text(clock(seconds), color = Color.White, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StoryArchiveRow(chatId: Long, telegram: Telegram, index: Int, count: Int, modifier: Modifier = Modifier) {
    var total by remember(chatId) { mutableIntStateOf(0) }
    var stories by remember(chatId) { mutableStateOf<List<TdApi.Story>>(emptyList()) }
    var open by remember { mutableStateOf(false) }
    LaunchedEffect(chatId) {
        telegram.archivedStories(chatId) { size, list ->
            total = size
            stories = list
        }
    }
    SegmentedListItem(
        onClick = { open = true },
        shapes = groupShapes(index, count),
        leadingContent = { Icon(Icons.Outlined.History, contentDescription = null) },
        trailingContent = { Text(total.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant) },
        modifier = modifier,
    ) {
        Text("Story Archive")
    }
    if (open) {
        GridDialog("Story Archive", onDismiss = { open = false }) {
            if (stories.isEmpty()) {
                Text("You haven't posted any stories yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                StoryGrid(stories, telegram)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GridDialog(title: String, onDismiss: () -> Unit, header: @Composable () -> Unit = {}, scrollable: Boolean = true, content: @Composable () -> Unit) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.widthIn(min = 400.dp, max = 520.dp),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, contentDescription = "Close") }
                }
                header()
                Column(Modifier.heightIn(max = 560.dp).then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)) { content() }
            }
        }
    }
}

@Composable
fun NameDialog(title: String, label: String, onDismiss: () -> Unit, onSave: (String, (String?) -> Unit) -> Unit) {
    var name by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val save = {
        if (name.isNotBlank() && !busy) {
            busy = true
            onSave(name.trim()) { failure ->
                busy = false
                if (failure == null) onDismiss() else error = failure
            }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.replace('\n', ' ').take(NameLimit) },
                    label = { Text(label) },
                    singleLine = true,
                    modifier = Modifier.focusRequester(focus).onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                            save()
                            true
                        } else {
                            false
                        }
                    },
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(enabled = name.isNotBlank() && !busy, onClick = save) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private const val NameLimit = 12

@Composable
fun StoryViewer(stories: List<TdApi.Story>, start: Int, telegram: Telegram, onDismiss: () -> Unit) {
    var index by remember { mutableIntStateOf(start) }
    val story = stories.getOrNull(index) ?: return onDismiss()
    val next = { if (index < stories.lastIndex) index += 1 else onDismiss() }
    val previous = { if (index > 0) index -= 1 }
    val focus = remember { FocusRequester() }
    DisposableEffect(story.posterChatId, story.id) {
        telegram.openStory(story)
        onDispose { telegram.closeStory(story) }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        LaunchedEffect(Unit) { focus.requestFocus() }
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .pointerInput(Unit) { detectTapGestures { onDismiss() } }
                .focusRequester(focus)
                .focusable()
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    when (event.key) {
                        Key.DirectionRight -> next().let { true }
                        Key.DirectionLeft -> previous().let { true }
                        Key.Escape -> onDismiss().let { true }
                        else -> false
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            BoxWithConstraints(
                Modifier
                    .fillMaxHeight(0.94f)
                    .aspectRatio(StoryRatio)
                    .widthIn(max = ViewerMaxWidth)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black),
            ) {
                var progress by remember(story.posterChatId, story.id) { mutableStateOf(0f) }
                var replying by remember(story.posterChatId, story.id) { mutableStateOf(false) }
                var viewers by remember(story.posterChatId, story.id) { mutableStateOf(false) }
                StoryMedia(story, telegram, paused = replying || viewers, onProgress = { progress = it }, onEnd = next)
                Row(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f).fillMaxHeight().clickable(indication = null, interactionSource = null) { previous() })
                    Box(Modifier.weight(2f).fillMaxHeight().clickable(indication = null, interactionSource = null) { next() })
                }
                Column(Modifier.fillMaxWidth().background(TopShade).padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        stories.indices.forEach { i ->
                            LinearProgressIndicator(
                                progress = { if (i < index) 1f else if (i > index) 0f else progress },
                                color = Color.White,
                                trackColor = Color.White.copy(alpha = 0.3f),
                                drawStopIndicator = {},
                                gapSize = 0.dp,
                                modifier = Modifier.weight(1f).height(3.dp),
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val poster = story.posterId ?: TdApi.MessageSenderChat(story.posterChatId)
                        com.pixelgram.desktop.ui.components.AvatarImage(
                            telegram.senderTitle(poster),
                            telegram.senderPhoto(poster),
                            telegram,
                            CircleShape,
                            32.dp,
                        )
                        Column(Modifier.weight(1f)) {
                            EmojiText(telegram.senderTitle(poster), color = Color.White, style = MaterialTheme.typography.titleSmall)
                            Text(
                                DateFormat.format(Instant.ofEpochSecond(story.date.toLong()).atZone(ZoneId.systemDefault())),
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                        IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, contentDescription = "Close", tint = Color.White) }
                    }
                }
                Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(BottomShade)) {
                    story.caption?.text?.takeIf { it.isNotBlank() }?.let { caption ->
                        EmojiText(
                            caption,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp),
                        )
                    }
                    StoryActions(story, telegram, onViewers = { viewers = true }, onTyping = { replying = it })
                }
                if (viewers) StoryViewersPanel(story, telegram, Modifier.align(Alignment.BottomCenter)) { viewers = false }
            }
        }
    }
}

@Composable
private fun StoryActions(story: TdApi.Story, telegram: Telegram, onViewers: () -> Unit, onTyping: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(12.dp),
    ) {
        if (story.canGetInteractions) {
            val info = story.interactionInfo
            Surface(onClick = onViewers, shape = CircleShape, color = Color.White.copy(alpha = StoryChipAlpha), contentColor = Color.White) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Outlined.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("%,d".format(info?.viewCount ?: 0), style = MaterialTheme.typography.labelLarge)
                    if ((info?.reactionCount ?: 0) > 0) {
                        Icon(Icons.Filled.Favorite, contentDescription = null, tint = LikeColor, modifier = Modifier.size(16.dp))
                        Text("%,d".format(info?.reactionCount ?: 0), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            return@Row
        }
        if (story.canBeReplied) {
            var text by remember(story.posterChatId, story.id) { mutableStateOf("") }
            var sent by remember(story.posterChatId, story.id) { mutableStateOf(false) }
            val send = {
                val message = text.trim()
                if (message.isNotEmpty()) {
                    text = ""
                    telegram.replyToStory(story, message) { error -> if (error == null) sent = true }
                }
            }
            androidx.compose.material3.TextField(
                value = text,
                onValueChange = {
                    text = it
                    sent = false
                },
                placeholder = { Text(if (sent) "Reply sent" else "Reply privately…", color = Color.White.copy(alpha = 0.7f)) },
                singleLine = true,
                shape = CircleShape,
                visualTransformation = com.pixelgram.desktop.ui.theme.EmojiVisualTransformation,
                colors = androidx.compose.material3.TextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = StoryChipAlpha),
                    unfocusedContainerColor = Color.White.copy(alpha = StoryChipAlpha),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                trailingIcon = if (text.isNotBlank()) {
                    { IconButton(onClick = send) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White) } }
                } else {
                    null
                },
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { onTyping(it.isFocused) }
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                            send()
                            true
                        } else {
                            false
                        }
                    },
            )
        } else {
            Spacer(Modifier.weight(1f))
        }
        var liked by remember(story.posterChatId, story.id) { mutableStateOf(story.chosenReactionType != null) }
        IconButton(onClick = {
            val target = !liked
            liked = target
            telegram.setStoryLiked(story, target) { error ->
                if (error != null) liked = !target else story.chosenReactionType = if (target) TdApi.ReactionTypeEmoji(com.pixelgram.desktop.telegram.StoryLike) else null
            }
        }) {
            Icon(
                if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (liked) "Unlike" else "Like",
                tint = if (liked) LikeColor else Color.White,
            )
        }
    }
}

@Composable
private fun StoryViewersPanel(story: TdApi.Story, telegram: Telegram, modifier: Modifier, onClose: () -> Unit) {
    var viewers by remember(story.posterChatId, story.id) { mutableStateOf<List<TdApi.StoryInteraction>?>(null) }
    LaunchedEffect(story.posterChatId, story.id) { telegram.storyViewers(story, "") { list, _ -> viewers = list } }
    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth().fillMaxHeight(ViewersHeight).clickable(indication = null, interactionSource = null) {},
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val info = story.interactionInfo
                Text(
                    "%,d views".format(info?.viewCount ?: 0),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, contentDescription = "Close") }
            }
            val list = viewers
            when {
                list == null -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { androidx.compose.material3.LoadingIndicator() }
                list.isEmpty() -> Text("No views yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> androidx.compose.foundation.lazy.LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    items(list.size) { index ->
                        val viewer = list[index]
                        val name = telegram.senderTitle(viewer.actorId)
                        val reaction = ((viewer.type as? TdApi.StoryInteractionTypeView)?.chosenReactionType as? TdApi.ReactionTypeEmoji)?.emoji
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        ) {
                            com.pixelgram.desktop.ui.components.AvatarImage(name, telegram.senderPhoto(viewer.actorId), telegram, CircleShape, 40.dp)
                            Column(Modifier.weight(1f)) {
                                EmojiText(name, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                                Text(
                                    DateFormat.format(Instant.ofEpochSecond(viewer.interactionDate.toLong()).atZone(ZoneId.systemDefault())),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            reaction?.let { EmojiText(it, style = MaterialTheme.typography.titleLarge) }
                        }
                    }
                }
            }
        }
    }
}

private const val StoryChipAlpha = 0.16f
private const val ViewersHeight = 0.6f
private val LikeColor = Color(0xFFFF4D6D)

private val TopShade = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent))
private val BottomShade = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)))

@Composable
private fun StoryMedia(story: TdApi.Story, telegram: Telegram, paused: Boolean, onProgress: (Float) -> Unit, onEnd: () -> Unit) {
    val holding by androidx.compose.runtime.rememberUpdatedState(paused)
    when (val content = story.content) {
        is TdApi.StoryContentPhoto -> {
            val file = content.photo.sizes.maxByOrNull { it.width }?.photo
            LaunchedEffect(file?.id) { telegram.download(file) }
            val path = telegram.localPath(file)
            val image by produceState(com.pixelgram.desktop.ui.components.cachedImage(path, StoryImageSize), path) { value = path?.let { loadImage(it, maxSize = StoryImageSize) } }
            Box(Modifier.fillMaxSize()) {
                ThumbImage(null, content.photo.minithumbnail, telegram, Modifier.fillMaxSize())
                image?.let { Image(it, contentDescription = null, contentScale = ContentScale.Crop, filterQuality = FilterQuality.High, modifier = Modifier.fillMaxSize()) }
            }
            LaunchedEffect(story.id, image != null) {
                if (image == null) return@LaunchedEffect
                var elapsed = 0L
                while (elapsed < PhotoStoryMs) {
                    onProgress(elapsed.toFloat() / PhotoStoryMs)
                    delay(StoryTickMs)
                    if (!holding) elapsed += StoryTickMs
                }
                onEnd()
            }
        }
        is TdApi.StoryContentVideo -> {
            val file = content.video.video
            LaunchedEffect(file?.id) { telegram.download(file) }
            val path = telegram.localPath(file)
            Box(Modifier.fillMaxSize()) {
                ThumbImage(content.video.thumbnail?.file, content.video.minithumbnail, telegram, Modifier.fillMaxSize())
                if (path != null) {
                    val engine = remember(path) { VideoEngine(path, loop = false).apply { start() } }
                    DisposableEffect(engine) { onDispose { engine.close() } }
                    LaunchedEffect(engine, paused) {
                        if (paused == engine.playing && !engine.ended) engine.toggle()
                    }
                    VideoFrame(engine, Modifier.fillMaxSize())
                    LaunchedEffect(engine) {
                        while (!engine.ended && !engine.failed) {
                            if (engine.duration > 0) onProgress(engine.position.toFloat() / engine.duration)
                            delay(StoryTickMs)
                        }
                        onEnd()
                    }
                }
            }
        }
        else -> {
            Text("This story isn't supported yet", color = Color.White, modifier = Modifier.padding(24.dp))
            LaunchedEffect(story.id) {
                delay(PhotoStoryMs)
                onEnd()
            }
        }
    }
}
