package com.pixelgram.desktop.ui.settings

import com.pixelgram.desktop.ui.components.groupShapes
import com.pixelgram.desktop.ui.EasterEgg
import com.pixelgram.desktop.ui.IsraelBlue
import com.pixelgram.desktop.ui.IsraelWhite
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.automirrored.filled.ArrowBack
import com.pixelgram.desktop.ui.icons.outlined.Add
import com.pixelgram.desktop.ui.icons.outlined.Close
import com.pixelgram.desktop.ui.icons.outlined.Computer
import com.pixelgram.desktop.ui.icons.outlined.Devices
import com.pixelgram.desktop.ui.icons.outlined.Info
import com.pixelgram.desktop.ui.icons.outlined.Language
import com.pixelgram.desktop.ui.icons.outlined.LaptopMac
import com.pixelgram.desktop.ui.icons.outlined.Lock
import com.pixelgram.desktop.ui.icons.outlined.PanTool
import com.pixelgram.desktop.ui.icons.outlined.PhoneAndroid
import com.pixelgram.desktop.ui.icons.outlined.PhoneIphone
import com.pixelgram.desktop.ui.icons.outlined.Place
import com.pixelgram.desktop.ui.icons.outlined.SportsEsports
import com.pixelgram.desktop.ui.icons.outlined.Star
import com.pixelgram.desktop.ui.icons.outlined.TabletMac
import com.pixelgram.desktop.ui.components.expressive.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import com.pixelgram.desktop.ui.components.expressive.FilledTonalButton
import androidx.compose.material3.Icon
import com.pixelgram.desktop.ui.components.expressive.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import com.pixelgram.desktop.ui.components.expressive.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AppLock
import com.pixelgram.desktop.ui.OnEscape
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.components.rememberPixelgramLogo
import com.pixelgram.desktop.ui.main.ForwardDialog
import com.pixelgram.desktop.ui.theme.EmojiText
import it.tdlight.jni.TdApi
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class PrivacyPage { Main, Sessions }

private enum class Audience(val label: String) { Everybody("Everybody"), Contacts("My contacts"), Nobody("Nobody") }

private class PrivacyItem(val title: String, val setting: TdApi.UserPrivacySetting, val premium: Boolean = false)

private val PrivacyItems = listOf(
    PrivacyItem("Phone number", TdApi.UserPrivacySettingShowPhoneNumber()),
    PrivacyItem("Last seen & online", TdApi.UserPrivacySettingShowStatus()),
    PrivacyItem("Profile photos", TdApi.UserPrivacySettingShowProfilePhoto()),
    PrivacyItem("Forwarded messages", TdApi.UserPrivacySettingShowLinkInForwardedMessages()),
    PrivacyItem("Calls", TdApi.UserPrivacySettingAllowCalls()),
    PrivacyItem("Voice messages", TdApi.UserPrivacySettingAllowPrivateVoiceAndVideoNoteMessages(), premium = true),
)

private val PrivacyItemsAfterMessages = listOf(
    PrivacyItem("Birthday", TdApi.UserPrivacySettingShowBirthdate()),
    PrivacyItem("Gifts", TdApi.UserPrivacySettingAutosaveGifts()),
    PrivacyItem("Bio", TdApi.UserPrivacySettingShowBio()),
    PrivacyItem("Saved Music", TdApi.UserPrivacySettingShowProfileAudio()),
    PrivacyItem("Invites", TdApi.UserPrivacySettingAllowChatInvites()),
)

private val AutoLockOptions = listOf(0 to "Disabled", 1 to "1 min", 5 to "5 min", 60 to "1 hour", 300 to "5 hours")
private val SessionDateFormat = DateTimeFormatter.ofPattern("MMMM d 'at' HH:mm", Locale.ENGLISH)

@Composable
fun PrivacySection(telegram: Telegram, settings: AppSettings) {
    var page by remember { mutableStateOf(PrivacyPage.Main) }
    var sessions by remember { mutableStateOf<List<TdApi.Session>?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    LaunchedEffect(reload) { telegram.activeSessions { sessions = it } }
    if (page == PrivacyPage.Sessions) OnEscape {
        page = PrivacyPage.Main
        true
    }
    AnimatedContent(
        targetState = page,
        transitionSpec = {
            val direction = if (targetState == PrivacyPage.Sessions) 1 else -1
            (slideInHorizontally { it / 4 * direction } + fadeIn()) togetherWith (slideOutHorizontally { -it / 4 * direction } + fadeOut())
        },
    ) { shown ->
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (shown) {
                PrivacyPage.Main -> PrivacyMain(telegram, settings, sessions?.size) { page = PrivacyPage.Sessions }
                PrivacyPage.Sessions -> SessionsPage(telegram, settings, sessions, onChanged = { reload++ }) { page = PrivacyPage.Main }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ValueRow(index: Int, count: Int, icon: ImageVector?, title: String, value: String?, star: Boolean = false, onClick: () -> Unit) {
    SegmentedListItem(
        onClick = onClick,
        shapes = groupShapes(index, count),
        leadingContent = icon?.let { { Icon(it, contentDescription = null) } },
        trailingContent = {
            if (value == null) LoadingIndicator(Modifier.size(24.dp)) else Text(value, color = MaterialTheme.colorScheme.primary)
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title)
            if (star) Icon(Icons.Outlined.Star, contentDescription = "Premium", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun PrivacyMain(telegram: Telegram, settings: AppSettings, sessionCount: Int?, onSessions: () -> Unit) {
    var passcodeDialog by remember { mutableStateOf(false) }
    val rules = remember { mutableStateMapOf<String, TdApi.UserPrivacySettingRules>() }
    var newChats by remember { mutableStateOf<TdApi.NewChatPrivacySettings?>(null) }
    var editing by remember { mutableStateOf<PrivacyItem?>(null) }
    var editingMessages by remember { mutableStateOf(false) }
    val all = PrivacyItems + PrivacyItemsAfterMessages
    LaunchedEffect(Unit) {
        all.forEach { item -> telegram.privacyRules(item.setting) { result -> result?.let { rules[item.title] = it } } }
        telegram.newChatPrivacy { newChats = it }
    }
    Header("Security")
    SegmentedGroup {
        ValueRow(0, 2, Icons.Outlined.Lock, "Local passcode", if (AppLock.enabled) "On" else "Off") { passcodeDialog = true }
        ValueRow(1, 2, Icons.Outlined.Devices, "Active sessions", sessionCount?.toString(), onClick = onSessions)
    }
    Header("Privacy")
    val count = all.size + 1
    SegmentedGroup {
        PrivacyItems.forEachIndexed { index, item ->
            ValueRow(index, count, null, item.title, rules[item.title]?.let(::summary), star = item.premium) { editing = item }
        }
        ValueRow(PrivacyItems.size, count, null, "Messages", newChats?.let { if (it.allowNewChatsFromUnknownUsers) "Everybody" else "Contacts and Premium" }) {
            editingMessages = true
        }
        PrivacyItemsAfterMessages.forEachIndexed { index, item ->
            ValueRow(PrivacyItems.size + 1 + index, count, null, item.title, rules[item.title]?.let(::summary)) { editing = item }
        }
    }
    editing?.let { item ->
        RulesDialog(item, rules[item.title], telegram, settings, onDismiss = { editing = null }) { saved ->
            rules[item.title] = saved
            editing = null
        }
    }
    if (editingMessages) {
        MessagesDialog(newChats, telegram, onDismiss = { editingMessages = false }) { saved ->
            newChats = saved
            editingMessages = false
        }
    }
    if (passcodeDialog) PasscodeDialog(settings) { passcodeDialog = false }
}

private class ParsedRules(
    val audience: Audience,
    val allowUsers: List<Long>,
    val allowChats: List<Long>,
    val restrictUsers: List<Long>,
    val restrictChats: List<Long>,
    val kept: List<TdApi.UserPrivacySettingRule>,
)

private fun parse(rules: TdApi.UserPrivacySettingRules?): ParsedRules {
    val list = rules?.rules.orEmpty()
    val audience = when {
        list.any { it is TdApi.UserPrivacySettingRuleAllowAll } -> Audience.Everybody
        list.any { it is TdApi.UserPrivacySettingRuleAllowContacts } -> Audience.Contacts
        else -> Audience.Nobody
    }
    return ParsedRules(
        audience,
        list.filterIsInstance<TdApi.UserPrivacySettingRuleAllowUsers>().flatMap { it.userIds.toList() },
        list.filterIsInstance<TdApi.UserPrivacySettingRuleAllowChatMembers>().flatMap { it.chatIds.toList() },
        list.filterIsInstance<TdApi.UserPrivacySettingRuleRestrictUsers>().flatMap { it.userIds.toList() },
        list.filterIsInstance<TdApi.UserPrivacySettingRuleRestrictChatMembers>().flatMap { it.chatIds.toList() },
        list.filter {
            it is TdApi.UserPrivacySettingRuleAllowPremiumUsers || it is TdApi.UserPrivacySettingRuleAllowBots ||
                it is TdApi.UserPrivacySettingRuleRestrictBots
        },
    )
}

private fun summary(rules: TdApi.UserPrivacySettingRules): String {
    val parsed = parse(rules)
    val premium = parsed.kept.any { it is TdApi.UserPrivacySettingRuleAllowPremiumUsers }
    val base = when {
        parsed.audience == Audience.Nobody && premium -> "Premium users"
        parsed.audience == Audience.Contacts && premium -> "Contacts and Premium"
        else -> parsed.audience.label
    }
    val minus = parsed.restrictUsers.size + parsed.restrictChats.size
    val plus = parsed.allowUsers.size + parsed.allowChats.size
    val parts = listOfNotNull(
        if (minus > 0 && parsed.audience != Audience.Nobody) "-$minus" else null,
        if (plus > 0 && parsed.audience != Audience.Everybody) "+$plus" else null,
    )
    return if (parts.isEmpty()) base else "$base (${parts.joinToString(", ")})"
}

private fun build(audience: Audience, allowUsers: List<Long>, allowChats: List<Long>, restrictUsers: List<Long>, restrictChats: List<Long>, kept: List<TdApi.UserPrivacySettingRule>) =
    TdApi.UserPrivacySettingRules(
        buildList {
            if (audience != Audience.Nobody) {
                if (restrictUsers.isNotEmpty()) add(TdApi.UserPrivacySettingRuleRestrictUsers(restrictUsers.toLongArray()))
                if (restrictChats.isNotEmpty()) add(TdApi.UserPrivacySettingRuleRestrictChatMembers(restrictChats.toLongArray()))
            }
            if (audience != Audience.Everybody) {
                if (allowUsers.isNotEmpty()) add(TdApi.UserPrivacySettingRuleAllowUsers(allowUsers.toLongArray()))
                if (allowChats.isNotEmpty()) add(TdApi.UserPrivacySettingRuleAllowChatMembers(allowChats.toLongArray()))
            }
            addAll(kept)
            add(
                when (audience) {
                    Audience.Everybody -> TdApi.UserPrivacySettingRuleAllowAll()
                    Audience.Contacts -> TdApi.UserPrivacySettingRuleAllowContacts()
                    Audience.Nobody -> TdApi.UserPrivacySettingRuleRestrictAll()
                }
            )
        }.toTypedArray()
    )

@Composable
private fun RulesDialog(
    item: PrivacyItem,
    current: TdApi.UserPrivacySettingRules?,
    telegram: Telegram,
    settings: AppSettings,
    onDismiss: () -> Unit,
    onSaved: (TdApi.UserPrivacySettingRules) -> Unit,
) {
    val parsed = remember(current) { parse(current) }
    var audience by remember { mutableStateOf(parsed.audience) }
    var allowUsers by remember { mutableStateOf(parsed.allowUsers) }
    var allowChats by remember { mutableStateOf(parsed.allowChats) }
    var restrictUsers by remember { mutableStateOf(parsed.restrictUsers) }
    var restrictChats by remember { mutableStateOf(parsed.restrictChats) }
    var picking by remember { mutableStateOf<Boolean?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Audience.entries.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { audience = option },
                    ) {
                        RadioButton(selected = audience == option, onClick = { audience = option })
                        Text(option.label)
                    }
                }
                if (audience != Audience.Nobody) {
                    ExceptionList("Never share with", restrictUsers, restrictChats, telegram,
                        onRemoveUser = { id -> restrictUsers = restrictUsers - id },
                        onRemoveChat = { id -> restrictChats = restrictChats - id },
                        onAdd = { picking = false })
                }
                if (audience != Audience.Everybody) {
                    ExceptionList("Always share with", allowUsers, allowChats, telegram,
                        onRemoveUser = { id -> allowUsers = allowUsers - id },
                        onRemoveChat = { id -> allowChats = allowChats - id },
                        onAdd = { picking = true })
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = {
                busy = true
                val rules = build(audience, allowUsers, allowChats, restrictUsers, restrictChats, parsed.kept)
                telegram.setPrivacyRules(item.setting, rules) { failure ->
                    busy = false
                    if (failure == null) onSaved(rules) else error = failure
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
    picking?.let { allow ->
        ForwardDialog(telegram, settings, onDismiss = { picking = null }) { chatId ->
            val chat = telegram.chats[chatId]
            val userId = chat?.privateUserId ?: 0L
            if (allow) {
                if (userId != 0L) {
                    allowUsers = (allowUsers + userId).distinct()
                    restrictUsers = restrictUsers - userId
                } else {
                    allowChats = (allowChats + chatId).distinct()
                }
            } else {
                if (userId != 0L) {
                    restrictUsers = (restrictUsers + userId).distinct()
                    allowUsers = allowUsers - userId
                } else {
                    restrictChats = (restrictChats + chatId).distinct()
                }
            }
            picking = null
        }
    }
}

@Composable
private fun ExceptionList(
    title: String,
    users: List<Long>,
    chats: List<Long>,
    telegram: Telegram,
    onRemoveUser: (Long) -> Unit,
    onRemoveChat: (Long) -> Unit,
    onAdd: () -> Unit,
) {
    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp))
    users.forEach { id -> ExceptionRow(telegram.userName(id), onRemove = { onRemoveUser(id) }) }
    chats.forEach { id -> ExceptionRow(telegram.chats[id]?.let(telegram::chatTitle) ?: "Group", onRemove = { onRemoveChat(id) }) }
    TextButton(onClick = onAdd) {
        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Text("Add users or groups", modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun ExceptionRow(name: String, onRemove: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        EmojiText(name, modifier = Modifier.weight(1f).padding(start = 12.dp), maxLines = 1)
        IconButton(onClick = onRemove) { Icon(Icons.Outlined.Close, contentDescription = "Remove") }
    }
}

@Composable
private fun MessagesDialog(
    current: TdApi.NewChatPrivacySettings?,
    telegram: Telegram,
    onDismiss: () -> Unit,
    onSaved: (TdApi.NewChatPrivacySettings) -> Unit,
) {
    var everybody by remember { mutableStateOf(current?.allowNewChatsFromUnknownUsers ?: true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Messages") },
        text = {
            Column {
                Text("Who can start a chat with you.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                listOf(true to "Everybody", false to "My contacts and Premium users").forEach { (value, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { everybody = value },
                    ) {
                        RadioButton(selected = everybody == value, onClick = { everybody = value })
                        Text(label)
                    }
                }
                if (!everybody) {
                    Text("Only Telegram Premium subscribers can limit this.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = {
                busy = true
                val value = TdApi.NewChatPrivacySettings(everybody, current?.incomingPaidMessageStarCount ?: 0L)
                telegram.setNewChatPrivacy(value) { failure ->
                    busy = false
                    if (failure == null) onSaved(value) else error = failure
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private enum class PasscodeStep { Set, Manage, Change, Off }

@Composable
private fun PasscodeDialog(settings: AppSettings, onDismiss: () -> Unit) {
    var step by remember { mutableStateOf(if (AppLock.enabled) PasscodeStep.Manage else PasscodeStep.Set) }
    var current by remember { mutableStateOf("") }
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val field = @Composable { value: String, label: String, onChange: (String) -> Unit ->
        OutlinedTextField(
            value = value,
            onValueChange = {
                onChange(it.filter(Char::isDigit).take(MaxPasscode))
                error = null
            },
            label = { Text(label) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.width(PasscodeFieldWidth),
        )
    }
    val setNew = {
        when {
            first.length != AppLock.PinLength -> error = "The passcode must be ${AppLock.PinLength} digits"
            first != second -> error = "Passcodes don't match"
            else -> {
                AppLock.setPasscode(first)
                onDismiss()
            }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Local passcode", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        modifier = Modifier.widthIn(max = PasscodeDialogWidth),
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                when (step) {
                    PasscodeStep.Set -> {
                        Text(
                            "A ${AppLock.PinLength}-digit PIN. pixelGram will ask for it when it starts and after you've been away.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                        field(first, "New passcode") { first = it }
                        field(second, "Repeat passcode") { second = it }
                    }
                    PasscodeStep.Change -> {
                        field(current, "Current passcode") { current = it }
                        field(first, "New passcode") { first = it }
                        field(second, "Repeat passcode") { second = it }
                    }
                    PasscodeStep.Off -> field(current, "Current passcode") { current = it }
                    PasscodeStep.Manage -> {
                        Text("Auto-lock if away for", style = MaterialTheme.typography.titleSmall, modifier = Modifier.fillMaxWidth())
                        AutoLockOptions.forEach { (minutes, label) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { settings.autoLockMinutes.value = minutes },
                            ) {
                                RadioButton(selected = settings.autoLockMinutes.value == minutes, onClick = { settings.autoLockMinutes.value = minutes })
                                Text(label)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(onClick = { step = PasscodeStep.Change }) { Text("Change") }
                            FilledTonalButton(onClick = {
                                AppLock.lock()
                                onDismiss()
                            }) { Text("Lock now") }
                        }
                        TextButton(onClick = { step = PasscodeStep.Off }) { Text("Turn passcode off", color = MaterialTheme.colorScheme.error) }
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            when (step) {
                PasscodeStep.Set -> TextButton(onClick = setNew) { Text("Save") }
                PasscodeStep.Change -> TextButton(onClick = {
                    if (!AppLock.check(current)) error = "Wrong passcode" else setNew()
                }) { Text("Save") }
                PasscodeStep.Off -> TextButton(onClick = {
                    if (!AppLock.check(current)) {
                        error = "Wrong passcode"
                    } else {
                        AppLock.setPasscode(null)
                        onDismiss()
                    }
                }) { Text("Turn off", color = MaterialTheme.colorScheme.error) }
                PasscodeStep.Manage -> TextButton(onClick = onDismiss) { Text("Done") }
            }
        },
        dismissButton = if (step != PasscodeStep.Manage) {
            { TextButton(onClick = onDismiss) { Text("Cancel") } }
        } else {
            null
        },
    )
}

private const val MaxPasscode = AppLock.PinLength
private val PasscodeFieldWidth = 200.dp
private val PasscodeDialogWidth = 300.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SessionsPage(
    telegram: Telegram,
    settings: AppSettings,
    sessions: List<TdApi.Session>?,
    onChanged: () -> Unit,
    onBack: () -> Unit,
) {
    var details by remember { mutableStateOf<TdApi.Session?>(null) }
    var terminate by remember { mutableStateOf<TdApi.Session?>(null) }
    var terminateAll by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
        Text("Active sessions", style = MaterialTheme.typography.titleLarge)
    }
    if (sessions == null) {
        LoadingIndicator()
        return
    }
    val currentSession = sessions.firstOrNull { it.isCurrent }
    val others = sessions.filter { !it.isCurrent }.sortedByDescending { it.lastActiveDate }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) { Header("This device") }
        TextButton(onClick = { rename = true }) { Text("Rename") }
    }
    currentSession?.let { session ->
        SegmentedListItem(
            onClick = { details = session },
            shapes = groupShapes(0, 1),
            leadingContent = { SessionIcon(session, 44.dp) },
            supportingContent = { Text(sessionSubtitle(session, current = true)) },
        ) { Text(session.deviceModel.ifBlank { session.platform }) }
    }
    if (others.isNotEmpty()) {
        SegmentedListItem(
            onClick = { terminateAll = true },
            shapes = groupShapes(0, 1),
            leadingContent = { Icon(Icons.Outlined.PanTool, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            supportingContent = { Text("Logs out all devices except for this one.") },
        ) { Text("Terminate all other sessions", color = MaterialTheme.colorScheme.error) }
        Header("Active devices")
        SegmentedGroup {
            others.forEachIndexed { index, session ->
                SegmentedListItem(
                    onClick = { details = session },
                    shapes = groupShapes(index, others.size),
                    leadingContent = { SessionIcon(session, 44.dp) },
                    supportingContent = { Text(sessionSubtitle(session, current = false)) },
                    trailingContent = {
                        IconButton(onClick = { terminate = session }) { Icon(Icons.Outlined.Close, contentDescription = "Terminate") }
                    },
                ) { Text(session.deviceModel.ifBlank { session.platform }) }
            }
        }
    }
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    details?.let { session ->
        SessionDetails(session, onTerminate = if (session.isCurrent) null else {
            {
                details = null
                terminate = session
            }
        }) { details = null }
    }
    terminate?.let { session ->
        AlertDialog(
            onDismissRequest = { terminate = null },
            title = { Text("Terminate session?") },
            text = { Text("${session.deviceModel} will be logged out of your account.") },
            confirmButton = {
                TextButton(onClick = {
                    terminate = null
                    telegram.terminateSession(session.id) { failure ->
                        error = failure
                        onChanged()
                    }
                }) { Text("Terminate", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { terminate = null }) { Text("Cancel") } },
        )
    }
    if (terminateAll) {
        AlertDialog(
            onDismissRequest = { terminateAll = false },
            title = { Text("Terminate all other sessions?") },
            text = { Text("Every device except this one will be logged out.") },
            confirmButton = {
                TextButton(onClick = {
                    terminateAll = false
                    telegram.terminateOtherSessions { failure ->
                        error = failure
                        onChanged()
                    }
                }) { Text("Terminate", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { terminateAll = false }) { Text("Cancel") } },
        )
    }
    if (rename) {
        var name by remember { mutableStateOf(settings.deviceName.value.ifBlank { currentSession?.deviceModel.orEmpty() }) }
        AlertDialog(
            onDismissRequest = { rename = false },
            title = { Text("Device name") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it.take(DeviceNameLimit) }, singleLine = true, label = { Text("Name") })
                    Text("Other devices will see the new name after pixelGram restarts.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    settings.deviceName.value = name.trim()
                    rename = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { rename = false }) { Text("Cancel") } },
        )
    }
}

private const val DeviceNameLimit = 64

private fun isPixelgram(session: TdApi.Session) = session.applicationName.contains("pixelgram", ignoreCase = true)

private fun sessionApp(session: TdApi.Session): String {
    val version = session.applicationVersion.removePrefix(session.applicationName).trim()
    return listOf(session.applicationName, version).filter { it.isNotBlank() }.joinToString(" ")
}

private fun sessionSubtitle(session: TdApi.Session, current: Boolean): String {
    val place = listOf(session.location, if (current) "online" else formatSessionDate(session.lastActiveDate)).filter { it.isNotBlank() }
    return sessionApp(session) + "\n" + place.joinToString(" • ")
}

private fun formatSessionDate(unix: Int): String =
    Instant.ofEpochSecond(unix.toLong()).atZone(ZoneId.systemDefault()).format(SessionDateFormat)

private fun deviceIcon(type: TdApi.SessionDeviceType?): ImageVector = when (type) {
    is TdApi.SessionDeviceTypeAndroid -> Icons.Outlined.PhoneAndroid
    is TdApi.SessionDeviceTypeIphone -> Icons.Outlined.PhoneIphone
    is TdApi.SessionDeviceTypeIpad -> Icons.Outlined.TabletMac
    is TdApi.SessionDeviceTypeMac, is TdApi.SessionDeviceTypeApple -> Icons.Outlined.LaptopMac
    is TdApi.SessionDeviceTypeChrome, is TdApi.SessionDeviceTypeFirefox, is TdApi.SessionDeviceTypeEdge,
    is TdApi.SessionDeviceTypeSafari, is TdApi.SessionDeviceTypeOpera, is TdApi.SessionDeviceTypeVivaldi,
    is TdApi.SessionDeviceTypeBrave -> Icons.Outlined.Language
    is TdApi.SessionDeviceTypeXbox -> Icons.Outlined.SportsEsports
    else -> Icons.Outlined.Computer
}

@Composable
private fun SessionIcon(session: TdApi.Session, size: Dp) {
    val scheme = MaterialTheme.colorScheme
    if (isPixelgram(session)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(size).clip(MaterialShapes.Cookie12Sided.toShape()).background(if (EasterEgg.israel) IsraelWhite else scheme.primary),
        ) {
            rememberPixelgramLogo()?.let { Icon(it, contentDescription = "pixelGram", tint = if (EasterEgg.israel) IsraelBlue else scheme.onPrimary, modifier = Modifier.size(size * 0.58f)) }
        }
    } else {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(size).clip(CircleShape).background(scheme.secondaryContainer),
        ) {
            Icon(deviceIcon(session.deviceType), contentDescription = null, tint = scheme.onSecondaryContainer, modifier = Modifier.size(size * 0.55f))
        }
    }
}

@Composable
private fun SessionDetails(session: TdApi.Session, onTerminate: (() -> Unit)?, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { SessionIcon(session, 72.dp) },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(session.deviceModel.ifBlank { session.platform })
                Text(
                    formatSessionDate(if (session.isCurrent) session.logInDate else session.lastActiveDate),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Info", style = MaterialTheme.typography.titleSmall)
                DetailRow(Icons.Outlined.Devices, sessionApp(session), "Application")
                DetailRow(Icons.Outlined.Info, listOf(session.platform, session.systemVersion).filter { it.isNotBlank() }.joinToString(" "), "System version")
                DetailRow(Icons.Outlined.Info, if (session.isOfficialApplication) "Yes" else "No", "Official app")
                DetailRow(Icons.Outlined.Place, session.location.ifBlank { session.ipAddress }, "Location")
                Text(
                    "This location estimate is based on the IP address and may not always be accurate.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        dismissButton = onTerminate?.let { terminate ->
            { TextButton(onClick = terminate) { Text("Terminate Session", color = MaterialTheme.colorScheme.error) } }
        },
    )
}

@Composable
private fun DetailRow(icon: ImageVector, value: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column {
            Text(value.ifBlank { "Unknown" })
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
