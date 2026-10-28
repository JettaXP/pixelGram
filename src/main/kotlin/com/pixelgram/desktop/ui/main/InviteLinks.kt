package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.core.tr
import com.pixelgram.desktop.telegram.ChatState
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.telegram.createInviteLink
import com.pixelgram.desktop.telegram.deleteRevokedLink
import com.pixelgram.desktop.telegram.editInviteLink
import com.pixelgram.desktop.telegram.inviteLinks
import com.pixelgram.desktop.telegram.revokeInviteLink
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.components.expressive.AlertDialog
import com.pixelgram.desktop.ui.components.expressive.IconButton
import com.pixelgram.desktop.ui.components.expressive.TextButton
import com.pixelgram.desktop.ui.components.groupShapes
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.automirrored.outlined.Forward
import com.pixelgram.desktop.ui.icons.outlined.Add
import com.pixelgram.desktop.ui.icons.outlined.ContentCopy
import com.pixelgram.desktop.ui.icons.outlined.Delete
import com.pixelgram.desktop.ui.icons.outlined.Edit
import com.pixelgram.desktop.ui.icons.outlined.Link
import com.pixelgram.desktop.ui.icons.outlined.LinkOff
import com.pixelgram.desktop.ui.icons.outlined.MoreVert
import com.pixelgram.desktop.ui.settings.PixelSwitch
import com.pixelgram.desktop.ui.theme.EmojiText
import it.tdlight.jni.TdApi
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val Hour = 3_600
private const val Day = 86_400
private val ExpirationChoices = listOf(0, Hour, Day, 7 * Day, 30 * Day)
private val LimitChoices = listOf(0, 1, 10, 50, 100)
private val LinkDate = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")

private fun copyLink(link: String) {
    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(link), null)
    TopNotice.text = tr("Link copied")
}

private fun nowSeconds(): Int = (System.currentTimeMillis() / 1000).toInt()

private fun expiryLabel(seconds: Int): String = when (seconds) {
    0 -> "Never"
    Hour -> "1 hour"
    Day -> "1 day"
    7 * Day -> "1 week"
    30 * Day -> "1 month"
    else -> "{n} days".replace("{n}", (seconds / Day).toString())
}

private fun linkSummary(link: TdApi.ChatInviteLink): String = buildList {
    add(tr("${link.memberCount} joined"))
    if (link.pendingJoinRequestCount > 0) add(tr("${link.pendingJoinRequestCount} requests"))
    if (link.memberLimit > 0) add(tr("${(link.memberLimit - link.memberCount).coerceAtLeast(0)} left"))
    if (link.expirationDate > 0) {
        val date = Instant.ofEpochSecond(link.expirationDate.toLong()).atZone(ZoneId.systemDefault()).format(LinkDate)
        add(if (link.expirationDate < nowSeconds()) tr("expired") else tr("until {date}").replace("{date}", date))
    }
}.joinToString(" · ")

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun InviteLinksPage(chat: ChatState, telegram: Telegram, settings: AppSettings) {
    var reload by remember { mutableIntStateOf(0) }
    var active by remember { mutableStateOf<List<TdApi.ChatInviteLink>?>(null) }
    var revoked by remember { mutableStateOf<List<TdApi.ChatInviteLink>>(emptyList()) }
    var editing by remember { mutableStateOf<TdApi.ChatInviteLink?>(null) }
    var creating by remember { mutableStateOf(false) }
    var revoking by remember { mutableStateOf<TdApi.ChatInviteLink?>(null) }
    var sharing by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val refresh = { reload++ }
    LaunchedEffect(reload) {
        telegram.inviteLinks(chat, revoked = false) { active = it }
        telegram.inviteLinks(chat, revoked = true) { revoked = it }
    }
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val links = active
        if (links == null) {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { LoadingIndicator() }
            return@Column
        }
        val primary = links.firstOrNull { it.isPrimary }
        val others = links.filter { !it.isPrimary }
        primary?.let { link ->
            Text("Primary link", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            SegmentedListItem(
                onClick = { copyLink(link.inviteLink) },
                shapes = groupShapes(0, 1),
                leadingContent = { Icon(Icons.Outlined.Link, contentDescription = null) },
                supportingContent = { Text(linkSummary(link)) },
                trailingContent = {
                    Row {
                        IconButton(onClick = { copyLink(link.inviteLink) }) { Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy link") }
                        IconButton(onClick = { sharing = link.inviteLink }) { Icon(Icons.AutoMirrored.Outlined.Forward, contentDescription = "Share link") }
                        IconButton(onClick = { revoking = link }) { Icon(Icons.Outlined.LinkOff, contentDescription = "Revoke link") }
                    }
                },
            ) { Text(link.inviteLink.removePrefix("https://"), maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
        Text("Additional links", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val count = others.size + 1
            SegmentedListItem(
                onClick = { creating = true },
                shapes = groupShapes(0, count),
                leadingContent = { Icon(Icons.Outlined.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            ) { Text("Create a new link", color = MaterialTheme.colorScheme.primary) }
            others.forEachIndexed { index, link ->
                LinkRow(link, index + 1, count, onEdit = { editing = link }, onRevoke = { revoking = link }, onShare = { sharing = link.inviteLink })
            }
        }
        Text(
            "You can create additional invite links that have a limited time or number of uses, or that need admin approval.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (revoked.isNotEmpty()) {
            Text("Revoked links", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                revoked.forEachIndexed { index, link ->
                    SegmentedListItem(
                        onClick = {},
                        shapes = groupShapes(index, revoked.size),
                        leadingContent = { Icon(Icons.Outlined.LinkOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        supportingContent = { Text(linkSummary(link)) },
                        trailingContent = {
                            IconButton(onClick = {
                                telegram.deleteRevokedLink(chat, link) { failure -> if (failure == null) refresh() else error = failure }
                            }) { Icon(Icons.Outlined.Delete, contentDescription = "Delete link") }
                        },
                    ) { EmojiText(link.name.ifBlank { link.inviteLink.removePrefix("https://") }, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
    if (creating || editing != null) {
        val link = editing
        LinkEditor(link, onDismiss = {
            creating = false
            editing = null
        }) { name, expiration, limit, requests, done ->
            val finish = { failure: String? ->
                done(failure)
                if (failure == null) {
                    creating = false
                    editing = null
                    refresh()
                }
            }
            if (link == null) telegram.createInviteLink(chat, name, expiration, limit, requests, finish)
            else telegram.editInviteLink(chat, link, name, expiration, limit, requests, finish)
        }
    }
    revoking?.let { link ->
        AlertDialog(
            onDismissRequest = { revoking = null },
            title = { Text("Revoke link") },
            text = { Text(if (link.isPrimary) "The current primary link will stop working and a new one will be created." else "This link will stop working. Anyone who has it won't be able to join.") },
            confirmButton = {
                TextButton(onClick = {
                    revoking = null
                    telegram.revokeInviteLink(chat, link) { failure -> if (failure == null) refresh() else error = failure }
                }) { Text("Revoke", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { revoking = null }) { Text("Cancel") } },
        )
    }
    sharing?.let { link ->
        ForwardDialog(telegram, settings, onDismiss = { sharing = null }) { target ->
            sharing = null
            telegram.sendText(target, link)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LinkRow(link: TdApi.ChatInviteLink, index: Int, count: Int, onEdit: () -> Unit, onRevoke: () -> Unit, onShare: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    SegmentedListItem(
        onClick = { copyLink(link.inviteLink) },
        shapes = groupShapes(index, count),
        leadingContent = { Icon(Icons.Outlined.Link, contentDescription = null) },
        supportingContent = { Text(linkSummary(link)) },
        trailingContent = {
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, contentDescription = "More") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, shape = RoundedCornerShape(16.dp)) {
                    DropdownMenuItem(text = { Text("Copy link") }, leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) }, onClick = { menu = false; copyLink(link.inviteLink) })
                    DropdownMenuItem(text = { Text("Share link") }, leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Forward, null) }, onClick = { menu = false; onShare() })
                    DropdownMenuItem(text = { Text("Edit link") }, leadingIcon = { Icon(Icons.Outlined.Edit, null) }, onClick = { menu = false; onEdit() })
                    DropdownMenuItem(
                        text = { Text("Revoke link", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Outlined.LinkOff, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { menu = false; onRevoke() },
                    )
                }
            }
        },
    ) { EmojiText(link.name.ifBlank { link.inviteLink.removePrefix("https://") }, maxLines = 1, overflow = TextOverflow.Ellipsis) }
}

@Composable
private fun LinkEditor(
    link: TdApi.ChatInviteLink?,
    onDismiss: () -> Unit,
    onSave: (String, Int, Int, Boolean, (String?) -> Unit) -> Unit,
) {
    val now = remember { nowSeconds() }
    var name by remember { mutableStateOf(link?.name.orEmpty()) }
    var expiry by remember { mutableStateOf(link?.expirationDate?.takeIf { it > now }?.let { ExpirationChoices.minByOrNull { choice -> kotlin.math.abs(choice - (it - now)) } } ?: 0) }
    var limit by remember { mutableStateOf(link?.memberLimit ?: 0) }
    var requests by remember { mutableStateOf(link?.createsJoinRequest == true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (link == null) "New link" else "Edit link") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(LinkNameLimit) },
                    label = { Text("Link name (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { requests = !requests }.padding(vertical = 4.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Request admin approval")
                        Text("New members will need to be approved by an admin.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    PixelSwitch(checked = requests, onCheckedChange = { requests = it })
                }
                Text("Limit by time period", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                ExpirationChoices.forEach { option ->
                    Choice(expiryLabel(option), expiry == option) { expiry = option }
                }
                if (!requests) {
                    Text("Limit by number of users", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    LimitChoices.forEach { option ->
                        Choice(if (option == 0) "No limit" else option.toString(), limit == option) { limit = option }
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = {
                busy = true
                val expiration = if (expiry == 0) 0 else nowSeconds() + expiry
                onSave(name.trim(), expiration, if (requests) 0 else limit, requests) { failure ->
                    busy = false
                    error = failure
                }
            }) { Text(if (link == null) "Create" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick),
    ) {
        RadioButton(selected = selected, onClick = onClick, modifier = Modifier.size(40.dp))
        Text(label)
    }
}

private const val LinkNameLimit = 32
