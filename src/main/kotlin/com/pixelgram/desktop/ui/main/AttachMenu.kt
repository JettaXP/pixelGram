package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.pixelgram.desktop.telegram.ChatState
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.components.expressive.IconButton
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.filled.AddCircle
import com.pixelgram.desktop.ui.icons.filled.CheckCircle
import com.pixelgram.desktop.ui.icons.filled.Checklist
import com.pixelgram.desktop.ui.icons.filled.Close
import com.pixelgram.desktop.ui.icons.filled.Group
import com.pixelgram.desktop.ui.icons.filled.Public
import com.pixelgram.desktop.ui.icons.filled.Shuffle
import com.pixelgram.desktop.ui.icons.filled.Sync
import com.pixelgram.desktop.ui.icons.filled.Timer
import com.pixelgram.desktop.ui.icons.filled.Visibility
import com.pixelgram.desktop.ui.icons.outlined.Article
import com.pixelgram.desktop.ui.icons.outlined.AttachFile
import com.pixelgram.desktop.ui.icons.outlined.BarChart
import com.pixelgram.desktop.ui.icons.outlined.Description
import com.pixelgram.desktop.ui.icons.outlined.Image
import com.pixelgram.desktop.ui.icons.outlined.LibraryMusic
import com.pixelgram.desktop.ui.icons.outlined.Place
import com.pixelgram.desktop.ui.icons.outlined.TaskAlt
import com.pixelgram.desktop.ui.theme.EmojiText
import com.pixelgram.desktop.ui.theme.EmojiVisualTransformation
import it.tdlight.jni.TdApi
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class AttachKind { Media, Document, Poll, Checklist, Article, Location, Music }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AttachButton(
    chat: ChatState?,
    telegram: Telegram,
    replyToId: Long,
    onMedia: (List<File>) -> Unit,
    onDocuments: (List<File>) -> Unit,
    onSent: () -> Unit,
) {
    var shown by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var open by remember { mutableStateOf<AttachKind?>(null) }
    val scope = rememberCoroutineScope()
    val close = {
        expanded = false
        scope.launch {
            delay(MenuCloseMs)
            shown = false
        }
    }
    LaunchedEffect(shown) { if (shown) expanded = true }
    val locked = com.pixelgram.desktop.core.AppLock.locked
    LaunchedEffect(locked) {
        if (locked) {
            expanded = false
            shown = false
        }
    }
    var dismissedAt by remember { mutableStateOf(0L) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val colors = MaterialTheme.colorScheme
    Box {
        val progress by androidx.compose.animation.core.animateFloatAsState(if (expanded) 1f else 0f, MaterialTheme.motionScheme.fastSpatialSpec())
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .focusProperties { canFocus = false }
                .size(MenuButtonSize)
                .clip(RoundedCornerShape(androidx.compose.ui.unit.lerp(MenuButtonSize / 2, MenuButtonCorner, progress)))
                .background(androidx.compose.ui.graphics.lerp(Color.Transparent, colors.primaryContainer, progress))
                .toggleable(value = expanded, role = androidx.compose.ui.semantics.Role.Button) { checked ->
                    when {
                        !checked -> close()
                        System.currentTimeMillis() - dismissedAt > ReopenGuardMs -> shown = true
                    }
                },
        ) {
            Icon(
                if (progress > 0.5f) Icons.Filled.Close else Icons.Outlined.AttachFile,
                contentDescription = if (expanded) "Close" else "Attach",
                tint = androidx.compose.ui.graphics.lerp(colors.onSurfaceVariant, colors.onPrimaryContainer, progress),
                modifier = Modifier.graphicsLayer { rotationZ = progress * IconTurn },
            )
        }
        if (shown && !locked) {
            Popup(
                alignment = Alignment.BottomStart,
                offset = with(density) { IntOffset(-MenuInset.roundToPx(), -(MenuButtonSize + MenuGap).roundToPx()) },
                onDismissRequest = {
                    dismissedAt = System.currentTimeMillis()
                    close()
                },
                properties = PopupProperties(focusable = true),
            ) {
                FloatingActionButtonMenu(
                    expanded = expanded,
                    horizontalAlignment = Alignment.Start,
                    button = {},
                ) {
                    attachKinds(chat, telegram).forEach { (kind, icon, label) ->
                        FloatingActionButtonMenuItem(
                            onClick = {
                                close()
                                when (kind) {
                                    AttachKind.Media -> scope.launch { onMedia(Attachments.pick()) }
                                    AttachKind.Document -> scope.launch { onDocuments(Attachments.pick()) }
                                    else -> open = kind
                                }
                            },
                            icon = { Icon(icon, contentDescription = null) },
                            text = { Text(label) },
                        )
                    }
                }
            }
        }
    }
    val chatId = chat?.id ?: return
    val done = {
        open = null
        onSent()
    }
    when (open) {
        AttachKind.Poll -> PollDialog(telegram, chat, replyToId, onDismiss = { open = null }, onSent = done)
        AttachKind.Checklist -> ChecklistDialog(telegram, chatId, replyToId, onDismiss = { open = null }, onSent = done)
        AttachKind.Article -> ArticleEditor(telegram, chatId, replyToId, onDismiss = { open = null }, onSent = done)
        AttachKind.Location -> LocationDialog(telegram, chatId, replyToId, onDismiss = { open = null }, onSent = done)
        AttachKind.Music -> MusicDialog(telegram, chatId, onDismiss = { open = null }, onSent = done)
        else -> Unit
    }
}

private fun attachKinds(chat: ChatState?, telegram: Telegram): List<Triple<AttachKind, ImageVector, String>> = buildList {
    add(Triple(AttachKind.Media, Icons.Outlined.Image, "Photo or video"))
    add(Triple(AttachKind.Document, Icons.Outlined.Description, "Document"))
    if (chat != null && chat.isGroup) add(Triple(AttachKind.Poll, Icons.Outlined.BarChart, "Poll"))
    if (chat != null && !chat.isChannel && !telegram.isSavedMessages(chat.id)) add(Triple(AttachKind.Checklist, Icons.Outlined.TaskAlt, "Checklist"))
    add(Triple(AttachKind.Article, Icons.Outlined.Article, "Article"))
    add(Triple(AttachKind.Location, Icons.Outlined.Place, "Location"))
    add(Triple(AttachKind.Music, Icons.Outlined.LibraryMusic, "Music"))
}

@Composable
fun PremiumNeededDialog(feature: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Telegram Premium needed") },
        text = { Text("$feature are available with Telegram Premium.") },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
    )
}

@Composable
private fun FormDialog(
    title: String,
    confirm: String,
    enabled: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                Modifier.width(DialogWidth).heightIn(max = DialogMaxHeight).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                content()
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        confirmButton = { TextButton(enabled = enabled, onClick = onConfirm) { Text(confirm) } },
    )
}

@Composable
private fun Section(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun SettingRow(icon: ImageVector, tint: Color, title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onChange(!checked) }.padding(vertical = 6.dp),
    ) {
        Box(Modifier.size(SettingIcon).clip(RoundedCornerShape(8.dp)).background(tint), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        com.pixelgram.desktop.ui.settings.PixelSwitch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun PhotoChip(file: File?, onPick: (File?) -> Unit) {
    val scope = rememberCoroutineScope()
    IconButton(onClick = {
        if (file != null) onPick(null) else scope.launch { Attachments.pick().firstOrNull(Attachments::isPhoto)?.let(onPick) }
    }) {
        Icon(if (file != null) Icons.Filled.Close else Icons.Outlined.AttachFile, contentDescription = if (file != null) "Remove photo" else "Attach photo")
    }
}

private fun pollPhoto(file: File): TdApi.InputPollMediaPhoto {
    val (width, height) = com.pixelgram.desktop.telegram.imageSize(file)
    return TdApi.InputPollMediaPhoto(TdApi.InputPhoto(TdApi.InputFileLocal(file.absolutePath), null, null, IntArray(0), width, height))
}

private class PollOptionDraft(text: String = "") {
    var text by mutableStateOf(text)
    var photo by mutableStateOf<File?>(null)
    var correct by mutableStateOf(false)
}

private val DurationChoices = listOf(3600 to "1 hour", 3 * 3600 to "3 hours", 86400 to "1 day", 3 * 86400 to "3 days", 7 * 86400 to "1 week")

@Composable
private fun PollDialog(telegram: Telegram, chat: ChatState, replyToId: Long, onDismiss: () -> Unit, onSent: () -> Unit) {
    val channel = chat.isChannel
    var question by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var descriptionPhoto by remember { mutableStateOf<File?>(null) }
    val options = remember { mutableStateListOf(PollOptionDraft()) }
    var showVoters by remember { mutableStateOf(!channel) }
    var multiple by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    var revoting by remember { mutableStateOf(true) }
    var shuffle by remember { mutableStateOf(false) }
    var quiz by remember { mutableStateOf(false) }
    var explanation by remember { mutableStateOf("") }
    var membersOnly by remember { mutableStateOf(false) }
    var countries by remember { mutableStateOf<List<String>>(emptyList()) }
    var limitCountry by remember { mutableStateOf(false) }
    var pickingCountries by remember { mutableStateOf(false) }
    var limitDuration by remember { mutableStateOf(false) }
    var duration by remember { mutableStateOf(DurationChoices[2].first) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val filled = options.filter { it.text.isNotBlank() }
    if (options.last().text.isNotBlank() && options.size < MaxPollOptions) options.add(PollOptionDraft())
    val valid = question.isNotBlank() && filled.size >= 2 && (!quiz || filled.any { it.correct }) && (!limitCountry || countries.isNotEmpty())
    FormDialog("New poll", "Create", valid && !busy, error, onDismiss, onConfirm = {
        busy = true
        val content = TdApi.InputMessagePoll().apply {
            this.question = TdApi.FormattedText(question.trim(), emptyArray())
            this.options = filled.map { option ->
                TdApi.InputPollOption(TdApi.FormattedText(option.text.trim(), emptyArray()), option.photo?.let(::pollPhoto))
            }.toTypedArray()
            this.description = TdApi.FormattedText(description.trim(), emptyArray())
            media = descriptionPhoto?.let(::pollPhoto)
            isAnonymous = channel || !showVoters
            allowsMultipleAnswers = multiple
            allowsRevoting = revoting && !quiz
            this.membersOnly = channel && membersOnly
            countryCodes = if (channel && limitCountry) countries.toTypedArray() else emptyArray()
            shuffleOptions = shuffle
            type = if (quiz) {
                TdApi.InputPollTypeQuiz(
                    filled.indices.filter { filled[it].correct }.toIntArray(),
                    TdApi.FormattedText(explanation.trim(), emptyArray()),
                    null,
                )
            } else {
                TdApi.InputPollTypeRegular(!channel && adding)
            }
            closeDate = if (limitDuration) (System.currentTimeMillis() / 1000).toInt() + duration else 0
        }
        telegram.sendContent(chat.id, content, replyToId) { failure ->
            busy = false
            if (failure == null) onSent() else error = failure
        }
    }) {
        Section("Question")
        OutlinedTextField(question, { question = it }, placeholder = { Text("Ask a question") }, visualTransformation = EmojiVisualTransformation, modifier = Modifier.fillMaxWidth())
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(description, { description = it }, placeholder = { Text("Add Description (optional)") }, visualTransformation = EmojiVisualTransformation, modifier = Modifier.weight(1f))
            PhotoChip(descriptionPhoto) { descriptionPhoto = it }
        }
        descriptionPhoto?.let { Text(it.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
        Section("Poll options")
        options.forEachIndexed { index, option ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (quiz && option.text.isNotBlank()) {
                    if (multiple) {
                        Checkbox(option.correct, { option.correct = it })
                    } else {
                        RadioButton(option.correct, { options.forEach { other -> other.correct = other === option } })
                    }
                }
                OutlinedTextField(
                    option.text,
                    { option.text = it },
                    placeholder = { Text("Add an option…") },
                    visualTransformation = EmojiVisualTransformation,
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                PhotoChip(option.photo) { option.photo = it }
                if (index < options.lastIndex) {
                    IconButton(onClick = { options.removeAt(index) }) { Icon(Icons.Filled.Close, contentDescription = "Remove option") }
                }
            }
        }
        val left = MaxPollOptions - filled.size
        Text(
            if (left > 0) "You can add $left more options." else "You have added the maximum number of options.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Section("Settings")
        if (!channel) SettingRow(Icons.Filled.Visibility, Blue, "Show Who Voted", "Display voter name on each option.", showVoters) { showVoters = it }
        SettingRow(Icons.Filled.Checklist, Orange, "Allow Multiple Answers", "Voters can select more than one option.", multiple) { multiple = it }
        if (!channel && !quiz) SettingRow(Icons.Filled.AddCircle, Blue, "Allow Adding Options", "Participants can suggest new options.", adding) { adding = it }
        if (!quiz) SettingRow(Icons.Filled.Sync, Purple, "Allow Revoting", "Voters can change their vote.", revoting) { revoting = it }
        SettingRow(Icons.Filled.Shuffle, Orange, "Shuffle Options", "Answers appear in random order for each voter.", shuffle) { shuffle = it }
        SettingRow(Icons.Filled.CheckCircle, Green, "Set Correct Answer", "Mark one or more options as the right answer.", quiz) { quiz = it }
        if (quiz) OutlinedTextField(explanation, { explanation = it }, placeholder = { Text("Explanation (optional)") }, modifier = Modifier.fillMaxWidth())
        if (channel) {
            SettingRow(Icons.Filled.Group, Indigo, "Restrict to Subscribers", "Only subscribers who joined 24+ hours ago can vote.", membersOnly) { membersOnly = it }
            SettingRow(Icons.Filled.Public, Blue, "Limit by Country", "Only users from selected countries can vote.", limitCountry) {
                limitCountry = it
                if (it && countries.isEmpty()) pickingCountries = true
            }
            if (limitCountry) {
                TextButton(onClick = { pickingCountries = true }) {
                    Text(if (countries.isEmpty()) "Choose countries" else countries.joinToString(", ") { code -> countryName(code) })
                }
            }
        }
        SettingRow(Icons.Filled.Timer, Red, "Limit Duration", "Automatically close the poll at a set time.", limitDuration) { limitDuration = it }
        if (limitDuration) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                DurationChoices.forEach { (seconds, label) ->
                    androidx.compose.material3.FilterChip(selected = duration == seconds, onClick = { duration = seconds }, label = { Text(label) })
                }
            }
        }
    }
    if (pickingCountries) {
        CountryPicker(countries, onDismiss = { pickingCountries = false }) {
            countries = it
            pickingCountries = false
            if (it.isEmpty()) limitCountry = false
        }
    }
}

private fun countryName(code: String): String = java.util.Locale.of("", code).getDisplayCountry(java.util.Locale.ENGLISH)

@Composable
private fun CountryPicker(initial: List<String>, onDismiss: () -> Unit, onDone: (List<String>) -> Unit) {
    val chosen = remember { mutableStateListOf<String>().apply { addAll(initial) } }
    var query by remember { mutableStateOf("") }
    val all = remember { java.util.Locale.getISOCountries().sortedBy(::countryName) }
    val shown = all.filter { query.isBlank() || countryName(it).contains(query, ignoreCase = true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Countries") },
        text = {
            Column(Modifier.width(DialogWidth)) {
                OutlinedTextField(query, { query = it }, placeholder = { Text("Search") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(shown) { code ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { if (code in chosen) chosen.remove(code) else chosen.add(code) },
                        ) {
                            Checkbox(code in chosen, null, modifier = Modifier.padding(8.dp))
                            Text(countryName(code))
                        }
                    }
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        confirmButton = { TextButton(onClick = { onDone(chosen.toList()) }) { Text("Done") } },
    )
}

@Composable
private fun ChecklistDialog(telegram: Telegram, chatId: Long, replyToId: Long, onDismiss: () -> Unit, onSent: () -> Unit) {
    var title by remember { mutableStateOf("") }
    val tasks = remember { mutableStateListOf("") }
    var othersAdd by remember { mutableStateOf(true) }
    var othersMark by remember { mutableStateOf(true) }
    var premium by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    if (tasks.last().isNotBlank() && tasks.size < MaxChecklistTasks) tasks.add("")
    val filled = tasks.filter { it.isNotBlank() }
    FormDialog("New Checklist", "Create", title.isNotBlank() && filled.isNotEmpty() && !busy, error, onDismiss, onConfirm = {
        if (!telegram.isPremium) {
            premium = true
            return@FormDialog
        }
        busy = true
        val list = TdApi.InputChecklist(
            TdApi.FormattedText(title.trim(), emptyArray()),
            filled.mapIndexed { index, text -> TdApi.InputChecklistTask(index + 1, TdApi.FormattedText(text.trim(), emptyArray())) }.toTypedArray(),
            othersAdd,
            othersMark,
        )
        telegram.sendContent(chatId, TdApi.InputMessageChecklist(list), replyToId) { failure ->
            busy = false
            if (failure == null) onSent() else error = failure
        }
    }) {
        OutlinedTextField(title, { title = it }, placeholder = { Text("Title") }, visualTransformation = EmojiVisualTransformation, modifier = Modifier.fillMaxWidth())
        Section("Checklist")
        tasks.forEachIndexed { index, task ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    task,
                    { tasks[index] = it },
                    placeholder = { Text("Add a task…") },
                    visualTransformation = EmojiVisualTransformation,
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                if (index < tasks.lastIndex) {
                    IconButton(onClick = { tasks.removeAt(index) }) { Icon(Icons.Filled.Close, contentDescription = "Remove task") }
                }
            }
        }
        Text("You can add ${MaxChecklistTasks - filled.size} more tasks.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Section("Settings")
        CheckRow("Allow Others to Add Tasks", othersAdd) { othersAdd = it }
        CheckRow("Allow Others to Mark As Done", othersMark) { othersMark = it }
    }
    if (premium) PremiumNeededDialog("Checklists") { premium = false }
}

@Composable
private fun CheckRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onChange(!checked) },
    ) {
        Checkbox(checked, null, modifier = Modifier.padding(8.dp))
        Text(title)
    }
}

private val Coordinates = Regex("""(-?\d{1,3}(?:\.\d+)?)\s*[,; ]\s*(-?\d{1,3}(?:\.\d+)?)""")

@Composable
private fun LocationDialog(telegram: Telegram, chatId: Long, replyToId: Long, onDismiss: () -> Unit, onSent: () -> Unit) {
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val match = Coordinates.find(input)
    val latitude = match?.groupValues?.get(1)?.toDoubleOrNull()?.takeIf { it in -90.0..90.0 }
    val longitude = match?.groupValues?.get(2)?.toDoubleOrNull()?.takeIf { it in -180.0..180.0 }
    FormDialog("Send location", "Send", latitude != null && longitude != null, error, onDismiss, onConfirm = {
        val content = TdApi.InputMessageLocation().apply { location = TdApi.Location(latitude ?: 0.0, longitude ?: 0.0, 0.0) }
        telegram.sendContent(chatId, content, replyToId) { failure -> if (failure == null) onSent() else error = failure }
    }) {
        Text("Paste coordinates or a map link, for example 55.7539, 37.6208.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(input, { input = it }, placeholder = { Text("Latitude, longitude") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        if (latitude != null && longitude != null) {
            TextButton(onClick = { Links.confirmOpen("https://www.openstreetmap.org/?mlat=$latitude&mlon=$longitude#map=16/$latitude/$longitude") }) {
                Text("Check on the map")
            }
        }
    }
}

@Composable
private fun MusicDialog(telegram: Telegram, chatId: Long, onDismiss: () -> Unit, onSent: () -> Unit) {
    var audios by remember { mutableStateOf<List<TdApi.Audio>?>(null) }
    LaunchedEffect(Unit) {
        val me = telegram.me?.id ?: return@LaunchedEffect run { audios = emptyList() }
        telegram.profileAudios(me) { audios = it }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Music from your profile") },
        text = {
            val list = audios
            Column(Modifier.width(DialogWidth)) {
                when {
                    list == null -> androidx.compose.material3.LoadingIndicator(Modifier.align(Alignment.CenterHorizontally))
                    list.isEmpty() -> Text("Add music to your profile to send it from here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else -> LazyColumn(Modifier.heightIn(max = 400.dp)) {
                        items(list) { audio ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable {
                                    telegram.sendAudio(chatId, audio)
                                    onSent()
                                }.padding(8.dp),
                            ) {
                                Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.LibraryMusic, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                                Column(Modifier.weight(1f)) {
                                    EmojiText(audio.title.ifBlank { audio.fileName }, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                                    if (audio.performer.isNotBlank()) Text(audio.performer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

private const val MaxPollOptions = 12
private const val MaxChecklistTasks = 30
private const val MenuCloseMs = 260L
private val MenuButtonSize = 48.dp
private val MenuButtonCorner = 16.dp
private val MenuGap = 4.dp
private val MenuInset = 16.dp
private const val IconTurn = 90f
private const val ReopenGuardMs = 250L
private val DialogWidth = 420.dp
private val DialogMaxHeight = 560.dp
private val SettingIcon = 30.dp
private val Blue = Color(0xFF4A90E2)
private val Orange = Color(0xFFF5A623)
private val Purple = Color(0xFFA06CD5)
private val Green = Color(0xFF4CAF50)
private val Indigo = Color(0xFF5C6BC0)
private val Red = Color(0xFFE57373)
