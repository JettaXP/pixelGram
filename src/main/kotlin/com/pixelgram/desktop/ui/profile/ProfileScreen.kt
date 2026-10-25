package com.pixelgram.desktop.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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

private enum class ProfileField { Name, Username, Bio, Birthday }

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
    val me = telegram.me
    var fullInfo by remember { mutableStateOf<TdApi.UserFullInfo?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    var picking by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ProfileField?>(null) }
    var gallery by remember { mutableStateOf(false) }
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

    PageScaffold("My Profile", onBack) {
        if (me == null) {
            Text("Not signed in")
            return@PageScaffold
        }
        val name = me.fullName()
        Box(Modifier.fillMaxWidth().height(BannerHeight + AvatarOverlap)) {
            ProfileBanner(
                me.id,
                telegram,
                BannerHeight,
                MaterialTheme.shapes.extraLarge,
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
            EmojiText(name, style = MaterialTheme.typography.headlineMediumEmphasized)
            com.pixelgram.desktop.ui.components.UserBadge.of(me.id)?.let { com.pixelgram.desktop.ui.components.UserBadgeView(it, 30.dp) }
        }
        val rows = buildList {
            add(InfoRow(Icons.Outlined.Person, "Name", name, onEdit = { editing = ProfileField.Name }))
            me.phoneNumber?.takeIf { it.isNotEmpty() }?.let {
                add(InfoRow(Icons.Outlined.Phone, "Phone", "+$it", phone = true))
            }
            add(
                InfoRow(
                    Icons.Outlined.AlternateEmail,
                    "Username",
                    me.usernames?.activeUsernames?.firstOrNull()?.let { "@$it" } ?: "Not set",
                    onEdit = { editing = ProfileField.Username },
                    trailing = me.usernames?.activeUsernames?.firstOrNull()?.let { username ->
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
            onDismiss = dismiss,
            onSaved = saved,
        ) { values, done -> telegram.setName(values[0], values[1], done) }
        ProfileField.Username -> EditDialog(
            title = "Username",
            initial = listOf(me?.usernames?.activeUsernames?.firstOrNull().orEmpty()),
            labels = listOf("Username"),
            supporting = "a-z, 0-9 and underscores, at least 5 characters. Leave empty to remove it.",
            onDismiss = dismiss,
            onSaved = saved,
        ) { values, done -> telegram.setUsername(values[0], done) }
        ProfileField.Bio -> EditDialog(
            title = "Bio",
            initial = listOf(fullInfo?.bio?.text.orEmpty()),
            labels = listOf("Bio"),
            maxLength = BioLimit,
            singleLine = false,
            onDismiss = dismiss,
            onSaved = saved,
        ) { values, done -> telegram.setBio(values[0], done) }
        ProfileField.Birthday -> EditDialog(
            title = "Birthday",
            initial = listOf(
                fullInfo?.birthdate?.day?.takeIf { it > 0 }?.toString().orEmpty(),
                fullInfo?.birthdate?.month?.takeIf { it > 0 }?.toString().orEmpty(),
                fullInfo?.birthdate?.year?.takeIf { it > 0 }?.toString().orEmpty(),
            ),
            labels = listOf("Day", "Month", "Year"),
            numbers = true,
            supporting = "Year is optional. Clear the day to remove the birthday.",
            onDismiss = dismiss,
            onSaved = saved,
        ) { values, done ->
            telegram.setBirthdate(values[0].toIntOrNull() ?: 0, values[1].toIntOrNull() ?: 0, values[2].toIntOrNull() ?: 0, done)
        }
        null -> Unit
    }
}

private fun formatBirthdate(date: TdApi.Birthdate): String {
    val month = Month.of(date.month.coerceIn(1, 12)).getDisplayName(TextStyle.FULL, Locale.ENGLISH)
    return if (date.year > 0) "${date.day} $month ${date.year}" else "${date.day} $month"
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    labels.forEachIndexed { index, label ->
                        OutlinedTextField(
                            value = values[index],
                            onValueChange = { value ->
                                val clean = if (numbers) value.filter(Char::isDigit).take(4) else value.take(maxLength)
                                values = values.toMutableList().also { it[index] = clean }
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
                            modifier = if (labels.size > 1) Modifier.weight(1f) else Modifier.width(320.dp),
                        )
                    }
                }
                supporting?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy,
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
