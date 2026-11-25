package com.pixelgram.desktop.ui.profile

import com.pixelgram.desktop.ui.components.streamerBlur
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.outlined.AddAPhoto
import com.pixelgram.desktop.ui.icons.outlined.Add
import com.pixelgram.desktop.ui.icons.outlined.Close
import com.pixelgram.desktop.ui.icons.outlined.Sticker
import com.pixelgram.desktop.ui.icons.outlined.AlternateEmail
import com.pixelgram.desktop.ui.icons.outlined.Cake
import com.pixelgram.desktop.ui.icons.outlined.Info
import com.pixelgram.desktop.ui.icons.outlined.Numbers
import com.pixelgram.desktop.ui.icons.outlined.Person
import com.pixelgram.desktop.ui.icons.outlined.Phone
import com.pixelgram.desktop.ui.icons.outlined.PhotoLibrary
import com.pixelgram.desktop.ui.components.expressive.AlertDialog
import com.pixelgram.desktop.ui.components.expressive.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.pixelgram.desktop.ui.components.expressive.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.IconButton
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.components.expressive.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.pixelgram.desktop.ui.toShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.core.tr
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.telegram.fullName
import com.pixelgram.desktop.ui.components.Avatar
import com.pixelgram.desktop.ui.components.InfoGroup
import com.pixelgram.desktop.ui.components.InfoRow
import com.pixelgram.desktop.ui.components.UsernameQrButton
import com.pixelgram.desktop.ui.components.PageScaffold
import com.pixelgram.desktop.ui.main.Attachments
import com.pixelgram.desktop.ui.theme.EmojiText
import com.pixelgram.desktop.ui.theme.EmojiVisualTransformation
import it.tdlight.jni.TdApi
import java.io.File
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

private enum class ProfileField { Name, Bio, Birthday, StartPage }

private const val BioLimit = 70
private val BannerHeight = 200.dp
private val AvatarOverlap = 60.dp

@Composable
fun ProfileScreen(
    telegram: Telegram,
    settings: AppSettings,
    onOpenChat: (Long) -> Unit,
    onBack: () -> Unit,
) {
    PageScaffold("My Profile", onBack) { ProfileContent(telegram, settings, onOpenChat) }
}

@Composable
fun ProfileContent(
    telegram: Telegram,
    settings: AppSettings,
    onOpenChat: (Long) -> Unit,
) {
    val me = telegram.me
    var fullInfo by remember { mutableStateOf<TdApi.UserFullInfo?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    var picking by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ProfileField?>(null) }
    var gallery by remember { mutableStateOf(false) }
    var usernamesOpen by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(me?.id, refresh) { telegram.myFullInfo { fullInfo = it } }
    val setChannel = { chatId: Long ->
        picking = false
        telegram.setPersonalChat(chatId) {
            error = it
            refresh++
        }
    }
    val pickPhoto: (File) -> Unit = { file ->
        val done: (String?) -> Unit = { failure ->
            error = failure
            refresh++
        }
        if (isVideoFile(file)) telegram.setProfileVideo(file.absolutePath, done) else telegram.setProfilePhoto(file.absolutePath, done)
    }

    androidx.compose.foundation.layout.Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (me == null) {
            Text("Not signed in")
            return@Column
        }
        val name = me.fullName()
        Box(Modifier.fillMaxWidth().height(BannerHeight + AvatarOverlap)) {
            ProfileBanner(
                me.id,
                telegram,
                BannerHeight,
                MaterialTheme.shapes.extraLarge.copy(bottomStart = androidx.compose.foundation.shape.CornerSize(0), bottomEnd = androidx.compose.foundation.shape.CornerSize(0)),
                MaterialTheme.colorScheme.surfaceContainer,
                editable = true,
            )
            AvatarEditor(160.dp, onPicked = pickPhoto, modifier = Modifier.align(Alignment.BottomCenter)) {
                Box(
                    Modifier
                        .clip(settings.avatarShapeFor(null).toShape(settings.avatarCorner.value))
                        .clickable(enabled = me.profilePhoto != null) { gallery = true },
                ) {
                    Avatar(name, me.profilePhoto?.small, telegram, settings, size = 160.dp, full = me.profilePhoto?.big)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EmojiText(name, style = MaterialTheme.typography.headlineMediumEmphasized, modifier = Modifier.streamerBlur())
            com.pixelgram.desktop.ui.components.UserBadge.of(me.id)?.let { com.pixelgram.desktop.ui.components.UserBadgeView(it, telegram, 30.dp) }
                ?: com.pixelgram.desktop.ui.components.SupporterBadge(me.id, name, telegram, 30.dp, label = true)
        }
        com.pixelgram.desktop.ui.main.ProfileBalance(me.id, telegram)
        val activeUsernames = me.usernames?.activeUsernames?.toList().orEmpty()
        val rows = buildList {
            add(InfoRow(Icons.Outlined.Person, "Name", name, onEdit = { editing = ProfileField.Name }))
            me.phoneNumber?.takeIf { it.isNotEmpty() }?.let {
                add(InfoRow(Icons.Outlined.Phone, "Phone", "+$it", phone = true))
            }
            add(
                InfoRow(
                    Icons.Outlined.AlternateEmail,
                    "Username",
                    activeUsernames.joinToString(", ") { "@$it" }.ifEmpty { "Not set" },
                    onEdit = { usernamesOpen = true },
                    trailing = activeUsernames.firstOrNull()?.let { username ->
                        { UsernameQrButton(username, name, me.profilePhoto, telegram) }
                    },
                )
            )
            add(
                InfoRow(
                    Icons.Outlined.Info,
                    "Bio",
                    fullInfo?.bio?.text?.takeIf { it.isNotBlank() } ?: "Not set",
                    onEdit = { editing = ProfileField.Bio },
                    entities = fullInfo?.bio?.takeIf { it.text.isNotBlank() }?.entities?.toList().orEmpty(),
                    onMention = { username -> telegram.openPublicChat(username, { id -> onOpenChat(id) }, { com.pixelgram.desktop.ui.main.Links.notFound = username }) },
                )
            )
            add(
                InfoRow(
                    Icons.Outlined.Cake,
                    "Birthday",
                    fullInfo?.birthdate?.let(::formatBirthdate) ?: "Not set",
                    onEdit = { editing = ProfileField.Birthday },
                )
            )
            add(InfoRow(Icons.Outlined.Numbers, "ID", me.id.toString()))
        }
        ProfileMusic(telegram, settings, me.id, own = true)
        InfoGroup(rows)
        androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(androidx.compose.material3.ListItemDefaults.SegmentedGap)) {
            val owner = remember(me.id) { TdApi.MessageSenderUser(me.id) }
            com.pixelgram.desktop.ui.main.ProfileGifts(owner, telegram, Modifier.fillMaxWidth(), own = true, index = 0, count = 2)
            com.pixelgram.desktop.ui.main.StoryArchiveRow(me.id, telegram, index = 1, count = 2, modifier = Modifier.fillMaxWidth())
        }
        com.pixelgram.desktop.ui.main.ProfileStories(me.id, telegram, own = true, modifier = Modifier.fillMaxWidth())

        val channelId = fullInfo?.personalChatId ?: 0L
        if (channelId != 0L) {
            PersonalChannelCard(channelId, telegram, settings, onOpenChat, Modifier.fillMaxWidth())
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { picking = true }) {
                Text(if (channelId != 0L) "Change personal channel" else "Add personal channel")
            }
            if (channelId != 0L) {
                TextButton(onClick = { setChannel(0L) }) { Text("Remove") }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
    if (picking) {
        PersonalChannelPicker(telegram, settings, onPicked = setChannel, onDismiss = { picking = false })
    }
    if (gallery && me != null) {
        ProfilePhotosDialog(telegram, me.id, onDismiss = { gallery = false }, editable = true, onChanged = { refresh++ })
    }
    if (usernamesOpen && me != null) {
        UsernamesDialog(
            telegram = telegram,
            usernames = me.usernames,
            onDismiss = { usernamesOpen = false },
            onChanged = { refresh++ },
        )
    }
    val dismiss = { editing = null }
    val saved: () -> Unit = {
        editing = null
        refresh++
    }
    when (editing) {
        ProfileField.Name -> EditDialog(
            title = "Name",
            initial = listOf(me?.firstName.orEmpty(), me?.lastName.orEmpty()),
            labels = listOf("First name", "Last name"),
            stacked = true,
            onDismiss = dismiss,
            onSaved = saved,
        ) { values, done -> telegram.setName(values[0], values[1], done) }
        ProfileField.Bio -> EditDialog(
            title = "Bio",
            initial = listOf(fullInfo?.bio?.text.orEmpty()),
            labels = listOf("Bio"),
            maxLength = BioLimit,
            singleLine = false,
            onDismiss = dismiss,
            onSaved = saved,
        ) { values, done -> telegram.setBio(values[0], done) }
        ProfileField.StartPage -> com.pixelgram.desktop.ui.main.StartPageDialog(telegram, fullInfo?.businessInfo?.startPage, onDismiss = dismiss, onSaved = saved)
        ProfileField.Birthday -> EditDialog(
            title = "Birthday",
            initial = listOf(
                fullInfo?.birthdate?.day?.takeIf { it > 0 }?.toString().orEmpty(),
                fullInfo?.birthdate?.month?.takeIf { it > 0 }?.toString().orEmpty(),
                fullInfo?.birthdate?.year?.takeIf { it > 0 }?.toString().orEmpty(),
            ),
            labels = listOf("Day", "Month", "Year"),
            numbers = true,
            ranges = listOf(1..31, 1..12, MinBirthYear..java.time.Year.now().value),
            supporting = "Year is optional. Clear the day to remove the birthday.",
            valid = ::birthdateValid,
            onDismiss = dismiss,
            onSaved = saved,
        ) { values, done ->
            telegram.setBirthdate(values[0].toIntOrNull() ?: 0, values[1].toIntOrNull() ?: 0, values[2].toIntOrNull() ?: 0, done)
        }
        null -> Unit
    }
}

private fun fits(value: String, range: IntRange): Boolean {
    val number = value.toIntOrNull() ?: return true
    val digits = range.last.toString().length
    return number <= range.last && (value.length < digits || number >= range.first)
}

private const val MinBirthYear = 1877

private fun formatBirthdate(date: TdApi.Birthdate): String {
    val month = Month.of(date.month.coerceIn(1, 12)).getDisplayName(TextStyle.FULL, Locale.forLanguageTag(com.pixelgram.desktop.core.I18n.code))
    if (date.year <= 0) return "${date.day} $month"
    val age = runCatching {
        java.time.Period.between(java.time.LocalDate.of(date.year, date.month, date.day), java.time.LocalDate.now()).years
    }.getOrNull()
    return "${date.day} $month ${date.year}" + (age?.takeIf { it >= 0 }?.let { " ($it)" } ?: "")
}

private fun birthdateValid(values: List<String>): Boolean {
    val day = values[0].toIntOrNull() ?: return values.all { it.isBlank() } || values[0].isBlank()
    val month = values[1].toIntOrNull() ?: return false
    val year = values[2].toIntOrNull()
    val date = runCatching { java.time.LocalDate.of(year ?: LeapYear, month, day) }.getOrNull() ?: return false
    return year == null || !date.isAfter(java.time.LocalDate.now())
}

private const val LeapYear = 2000

@Composable
private fun UsernamesDialog(
    telegram: Telegram,
    usernames: TdApi.Usernames?,
    onDismiss: () -> Unit,
    onChanged: () -> Unit,
) {
    val active = usernames?.activeUsernames?.toList().orEmpty()
    val disabled = usernames?.disabledUsernames?.toList().orEmpty()
    var mainUsername by remember(active) { mutableStateOf(active.firstOrNull().orEmpty()) }
    var editable by remember(usernames?.editableUsername) { mutableStateOf(usernames?.editableUsername.orEmpty()) }
    var busy by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val chooseMain: (String) -> Unit = { username ->
        if (username != mainUsername) {
            mainUsername = username
            busy = username
            error = null
            telegram.reorderUsernames(listOf(username) + active.filter { it != username }) { failure ->
                busy = null
                error = failure
                if (failure == null) onChanged() else mainUsername = active.firstOrNull().orEmpty()
            }
        }
    }
    val toggle: (String, Boolean) -> Unit = { username, on ->
        busy = username
        error = null
        telegram.toggleUsername(username, on) { failure ->
            busy = null
            error = failure
            if (failure == null) onChanged()
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Usernames")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(tr("Active"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                if (active.isEmpty()) {
                    Text(tr("No active usernames"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                active.forEach { username ->
                    UsernameToggleRow(
                        username = username,
                        active = true,
                        enabled = busy == null && !saving,
                        main = username == mainUsername,
                        onMain = { chooseMain(username) },
                        onToggle = { on -> toggle(username, on) },
                    )
                }
                if (active.size > 1) {
                    Text(
                        tr("Tap the circle to choose the main username."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (disabled.isNotEmpty()) {
                    Text(tr("Inactive"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                    disabled.forEach { username ->
                        UsernameToggleRow(username, false, busy == null && !saving) { on -> toggle(username, on) }
                    }
                }
                OutlinedTextField(
                    value = editable,
                    onValueChange = { editable = it.trim().removePrefix("@").take(32) },
                    label = { Text(tr("Editable username")) },
                    singleLine = true,
                    enabled = busy == null && !saving,
                    supportingText = { Text(tr("a-z, 0-9 and underscores, at least 5 characters. Leave empty to remove it.")) },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                )
                Text(
                    tr("Other active usernames are switched on and off here. New collectible usernames are acquired in Fragment."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = busy == null && !saving && editable != usernames?.editableUsername.orEmpty(),
                onClick = {
                    saving = true
                    error = null
                    telegram.setUsername(editable) { failure ->
                        saving = false
                        error = failure
                        if (failure == null) onChanged()
                    }
                },
            ) { Text(if (saving) tr("Saving…") else tr("Save")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Close")) } },
    )
}

@Composable
private fun UsernameToggleRow(
    username: String,
    active: Boolean,
    enabled: Boolean,
    main: Boolean = false,
    onMain: (() -> Unit)? = null,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp))
            .background(if (active) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer)
            .padding(start = if (onMain != null) 4.dp else 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onMain != null) {
            RadioButton(selected = main, onClick = onMain, enabled = enabled)
        }
        Text(
            "@$username",
            style = MaterialTheme.typography.bodyLarge,
            color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { onToggle(!active) }, enabled = enabled, modifier = Modifier.size(40.dp)) {
            Icon(
                if (active) Icons.Outlined.Close else Icons.Outlined.Add,
                contentDescription = tr(if (active) "Deactivate" else "Activate"),
                tint = if (active) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun EditDialog(
    title: String,
    initial: List<String>,
    labels: List<String>,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    supporting: String? = null,
    maxLength: Int = Int.MAX_VALUE,
    singleLine: Boolean = true,
    numbers: Boolean = false,
    ranges: List<IntRange> = emptyList(),
    valid: (List<String>) -> Boolean = { true },
    stacked: Boolean = false,
    onSave: (List<String>, (String?) -> Unit) -> Unit,
) {
    var values by remember { mutableStateOf(initial) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val fields = @Composable { fieldModifier: (Int) -> Modifier ->
                    labels.forEachIndexed { index, label ->
                        OutlinedTextField(
                            value = values[index],
                            onValueChange = { value ->
                                val range = ranges.getOrNull(index)
                                val clean = if (numbers) value.filter(Char::isDigit).take(range?.last?.toString()?.length ?: 4) else value.take(maxLength)
                                if (range == null || fits(clean, range)) values = values.toMutableList().also { it[index] = clean }
                            },
                            label = { Text(label) },
                            singleLine = singleLine,
                            visualTransformation = EmojiVisualTransformation,
                            keyboardOptions = if (numbers) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
                            supportingText = if (maxLength != Int.MAX_VALUE) {
                                { Text("${values[index].length} / $maxLength") }
                            } else {
                                null
                            },
                            modifier = fieldModifier(index),
                        )
                    }
                }
                if (stacked || labels.size == 1) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { fields { Modifier.fillMaxWidth() } }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { fields { Modifier.weight(1f) } }
                }
                supporting?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy && valid(values),
                onClick = {
                    busy = true
                    onSave(values) { failure ->
                        busy = false
                        if (failure == null) onSaved() else error = failure
                    }
                },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
