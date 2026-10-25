package com.pixelgram.desktop.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.outlined.Add
import com.pixelgram.desktop.ui.icons.outlined.Close
import com.pixelgram.desktop.ui.icons.outlined.CreateNewFolder
import com.pixelgram.desktop.ui.icons.outlined.Delete
import com.pixelgram.desktop.ui.icons.outlined.DragIndicator
import com.pixelgram.desktop.ui.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.core.Setting
import com.pixelgram.desktop.telegram.ChatListKey
import com.pixelgram.desktop.telegram.ChatState
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.components.ChatAvatar
import com.pixelgram.desktop.ui.main.FolderIcons
import com.pixelgram.desktop.ui.main.FolderLayout
import com.pixelgram.desktop.ui.main.folderIcon
import com.pixelgram.desktop.ui.theme.EmojiText
import it.tdlight.jni.TdApi
import kotlin.math.roundToInt

private val RowGap = 2.dp
private val PickerHeight = 360.dp
private const val LiftScale = 1.02f
private const val PickerLimit = 300

private sealed interface FolderEdit {
    data class Server(val id: Int?) : FolderEdit
    data class Special(val key: ChatListKey) : FolderEdit
}

@Composable
fun FoldersSection(telegram: Telegram, settings: AppSettings) {
    var editing by remember { mutableStateOf<FolderEdit?>(null) }
    var deleting by remember { mutableStateOf<Int?>(null) }
    val order = FolderLayout.order(telegram, settings)
    val overrides = FolderLayout.iconOverrides(settings)
    val byId = telegram.folders.associateBy { it.id }
    var dragged by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var rowHeight by remember { mutableIntStateOf(1) }
    val gap = with(LocalDensity.current) { RowGap.toPx() }

    Description("Drag folders to change their order. Folders sync with your other Telegram apps; Unread and Mentions live in pixelGram.")
    Header("My folders")
    Column(verticalArrangement = Arrangement.spacedBy(RowGap), modifier = Modifier.animateContentSize()) {
        order.forEachIndexed { index, key ->
            val moving = index == dragged
            val lift by animateFloatAsState(if (moving) LiftScale else 1f, MaterialTheme.motionScheme.fastSpatialSpec())
            val (title, icon, subtitle) = when (key) {
                ChatListKey.Main -> Triple("All chats", "All", if (settings.hideAllChats.value) "Hidden" else "All your chats")
                ChatListKey.Unread -> Triple(settings.unreadFolderName.value, settings.unreadFolderIcon.value, "Chats with unread messages")
                ChatListKey.Mentions -> Triple(settings.mentionsFolderName.value, settings.mentionsFolderIcon.value, "Chats where you were mentioned")
                is ChatListKey.Folder -> byId[key.id]?.let { Triple(it.title, overrides[it.id] ?: it.icon, "Folder") } ?: return@forEachIndexed
                else -> return@forEachIndexed
            }
            val handle = Modifier.pointerInput(key, order.size) {
                detectDragGestures(
                    onDragStart = {
                        dragged = index
                        dragOffset = 0f
                    },
                    onDragEnd = {
                        val target = (dragged + (dragOffset / (rowHeight + gap)).roundToInt()).coerceIn(0, order.lastIndex)
                        if (target != dragged && dragged >= 0) {
                            val reordered = order.toMutableList().apply { add(target, removeAt(dragged)) }
                            FolderLayout.saveOrder(settings, reordered)
                            telegram.reorderFolders(reordered.filterIsInstance<ChatListKey.Folder>().map { it.id })
                        }
                        dragged = -1
                        dragOffset = 0f
                    },
                    onDragCancel = {
                        dragged = -1
                        dragOffset = 0f
                    },
                ) { change, drag ->
                    change.consume()
                    dragOffset += drag.y
                }
            }
            val shape = when {
                order.size == 1 -> RoundedCornerShape(20.dp)
                index == 0 -> RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                index == order.lastIndex -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
                else -> RoundedCornerShape(4.dp)
            }
            val container by animateColorAsState(
                if (moving) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainer,
                MaterialTheme.motionScheme.fastEffectsSpec(),
            )
            Surface(
                shape = shape,
                color = container,
                shadowElevation = if (moving) 6.dp else 0.dp,
                modifier = Modifier
                    .onSizeChanged { rowHeight = it.height }
                    .zIndex(if (moving) 1f else 0f)
                    .offset { IntOffset(0, if (moving) dragOffset.roundToInt() else 0) }
                    .graphicsLayer {
                        scaleX = lift
                        scaleY = lift
                    },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                ) {
                    Icon(
                        Icons.Outlined.DragIndicator,
                        contentDescription = "Drag to reorder",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = handle.padding(4.dp),
                    )
                    Icon(folderIcon(icon, true), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) {
                        EmojiText(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    when (key) {
                        ChatListKey.Main -> PixelSwitch(checked = !settings.hideAllChats.value, onCheckedChange = { settings.hideAllChats.value = !it })
                        ChatListKey.Unread, ChatListKey.Mentions -> {
                            IconButton(onClick = { editing = FolderEdit.Special(key) }) { Icon(Icons.Outlined.Edit, contentDescription = "Edit") }
                            val enabled = if (key == ChatListKey.Unread) settings.unreadFolder else settings.mentionsFolder
                            PixelSwitch(checked = enabled.value, onCheckedChange = { enabled.value = it })
                        }
                        is ChatListKey.Folder -> {
                            IconButton(onClick = { editing = FolderEdit.Server(key.id) }) { Icon(Icons.Outlined.Edit, contentDescription = "Edit") }
                            IconButton(onClick = { deleting = key.id }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }
    FilledTonalButton(onClick = { editing = FolderEdit.Server(null) }, modifier = Modifier.padding(top = 12.dp)) {
        Icon(Icons.Outlined.CreateNewFolder, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
        Text("Create new folder")
    }

    when (val edit = editing) {
        is FolderEdit.Server -> FolderEditor(telegram, settings, edit.id) { editing = null }
        is FolderEdit.Special -> SpecialFolderEditor(
            name = if (edit.key == ChatListKey.Unread) settings.unreadFolderName else settings.mentionsFolderName,
            icon = if (edit.key == ChatListKey.Unread) settings.unreadFolderIcon else settings.mentionsFolderIcon,
        ) { editing = null }
        null -> {}
    }
    deleting?.let { id ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete folder") },
            text = { Text("Delete “${byId[id]?.title.orEmpty()}”? Chats stay where they are.") },
            confirmButton = {
                TextButton(onClick = {
                    telegram.deleteFolder(id)
                    FolderLayout.setIconOverride(settings, id, null)
                    deleting = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SpecialFolderEditor(name: Setting<String>, icon: Setting<String>, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(name.value) }
    var chosen by remember { mutableStateOf(icon.value) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit folder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(title, { title = it.take(NameLimit) }, label = { Text("Folder name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                IconPicker(chosen) { chosen = it }
            }
        },
        confirmButton = {
            TextButton(enabled = title.isNotBlank(), onClick = {
                name.value = title.trim()
                icon.value = chosen
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private const val NameLimit = 12

private class TypeOption(val title: String, val get: (TdApi.ChatFolder) -> Boolean, val set: (TdApi.ChatFolder, Boolean) -> Unit)

private val IncludeTypes = listOf(
    TypeOption("Contacts", { it.includeContacts }, { f, v -> f.includeContacts = v }),
    TypeOption("Non-contacts", { it.includeNonContacts }, { f, v -> f.includeNonContacts = v }),
    TypeOption("Groups", { it.includeGroups }, { f, v -> f.includeGroups = v }),
    TypeOption("Channels", { it.includeChannels }, { f, v -> f.includeChannels = v }),
    TypeOption("Bots", { it.includeBots }, { f, v -> f.includeBots = v }),
)

private val ExcludeTypes = listOf(
    TypeOption("Muted", { it.excludeMuted }, { f, v -> f.excludeMuted = v }),
    TypeOption("Read", { it.excludeRead }, { f, v -> f.excludeRead = v }),
    TypeOption("Archived", { it.excludeArchived }, { f, v -> f.excludeArchived = v }),
)

@Composable
private fun FolderEditor(telegram: Telegram, settings: AppSettings, folderId: Int?, onDismiss: () -> Unit) {
    var folder by remember { mutableStateOf<TdApi.ChatFolder?>(null) }
    var version by remember { mutableIntStateOf(0) }
    var title by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("Custom") }
    val included = remember { mutableStateListOf<Long>() }
    var picking by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    LaunchedEffect(folderId) {
        if (folderId == null) {
            folder = TdApi.ChatFolder().apply {
                name = TdApi.ChatFolderName(TdApi.FormattedText("", arrayOf()), false)
                pinnedChatIds = longArrayOf()
                includedChatIds = longArrayOf()
                excludedChatIds = longArrayOf()
            }
        } else {
            telegram.loadFolder(folderId) { loaded ->
                loaded ?: return@loadFolder
                title = loaded.name?.text?.text.orEmpty()
                icon = FolderLayout.iconOverrides(settings)[folderId] ?: loaded.icon?.name?.takeIf { it.isNotEmpty() } ?: "Custom"
                included.clear()
                included.addAll(loaded.includedChatIds.toList())
                folder = loaded
            }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (folderId == null) "New folder" else "Edit folder") },
        text = {
            val current = folder
            if (current == null) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { androidx.compose.material3.LoadingIndicator() }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState()).animateContentSize(),
                ) {
                    OutlinedTextField(title, { title = it.take(NameLimit) }, label = { Text("Folder name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text("Icon", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    IconPicker(icon) { icon = it }
                    Text("Included chat types", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    key(version) {
                        IncludeTypes.forEach { option ->
                            CheckRow(option.title, option.get(current)) {
                                option.set(current, it)
                                version++
                            }
                        }
                        Text("Exclude", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        ExcludeTypes.forEach { option ->
                            CheckRow(option.title, option.get(current)) {
                                option.set(current, it)
                                version++
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Included chats", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                        TextButton(onClick = { picking = true }) {
                            Icon(Icons.Outlined.Add, contentDescription = null)
                            Text("Add chats")
                        }
                    }
                    included.toList().forEach { id ->
                        val chat = telegram.chats[id] ?: return@forEach
                        ChatLine(chat, telegram, settings) {
                            IconButton(onClick = { included.remove(id) }) { Icon(Icons.Outlined.Close, contentDescription = "Remove") }
                        }
                    }
                    AnimatedVisibility(
                        visible = error != null,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = folder != null && title.isNotBlank() && !saving,
                onClick = {
                    val current = folder ?: return@TextButton
                    current.name = TdApi.ChatFolderName(TdApi.FormattedText(title.trim(), arrayOf()), false)
                    current.icon = TdApi.ChatFolderIcon(if (icon in FolderLayout.ClientIcons) "Custom" else icon)
                    current.includedChatIds = included.toLongArray()
                    current.pinnedChatIds = current.pinnedChatIds.filter { it in included }.toLongArray()
                    current.excludedChatIds = current.excludedChatIds.filter { it !in included }.toLongArray()
                    saving = true
                    telegram.saveFolder(folderId, current) { failure ->
                        saving = false
                        if (failure == null) {
                            if (folderId != null) FolderLayout.setIconOverride(settings, folderId, icon.takeIf { it in FolderLayout.ClientIcons })
                            onDismiss()
                        } else {
                            error = if (failure.contains("FILTER_INCLUDE_EMPTY")) "Add at least one chat or chat type" else failure
                        }
                    }
                },
            ) { Text(if (folderId == null) "Create" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
    if (picking) {
        ChatPicker(telegram, settings, included.toSet(), onDismiss = { picking = false }) { picked ->
            included.clear()
            included.addAll(picked)
            picking = false
        }
    }
}

@Composable
private fun CheckRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onChange(!checked) },
    ) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(title)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IconPicker(selected: String, onPick: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FolderIcons.keys.forEach { name ->
            val chosen = name == selected
            val background by animateColorAsState(
                if (chosen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                MaterialTheme.motionScheme.fastEffectsSpec(),
            )
            val corner by animateFloatAsState(if (chosen) 30f else 50f, MaterialTheme.motionScheme.fastSpatialSpec())
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(corner.roundToInt()))
                    .background(background)
                    .clickable { onPick(name) },
            ) {
                Icon(
                    folderIcon(name, chosen),
                    contentDescription = name,
                    tint = if (chosen) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun ChatLine(chat: ChatState, telegram: Telegram, settings: AppSettings, trailing: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        ChatAvatar(chat, telegram, settings, 36.dp)
        EmojiText(telegram.chatTitle(chat), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
private fun ChatPicker(
    telegram: Telegram,
    settings: AppSettings,
    initial: Set<Long>,
    onDismiss: () -> Unit,
    onDone: (List<Long>) -> Unit,
) {
    val picked = remember { mutableStateListOf<Long>().apply { addAll(initial) } }
    var query by remember { mutableStateOf("") }
    val chats = (telegram.chatList(ChatListKey.Main) + telegram.chatList(ChatListKey.Archive))
        .filter { query.isBlank() || telegram.chatTitle(it).contains(query.trim(), ignoreCase = true) }
        .take(PickerLimit)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add chats") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(query, { query = it }, label = { Text("Search") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                LazyColumn(Modifier.heightIn(max = PickerHeight)) {
                    items(chats, key = { it.id }) { chat ->
                        val checked = chat.id in picked
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .animateItem()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { if (checked) picked.remove(chat.id) else picked.add(chat.id) }
                                .padding(vertical = 4.dp),
                        ) {
                            Checkbox(checked = checked, onCheckedChange = { if (it) picked.add(chat.id) else picked.remove(chat.id) })
                            ChatLine(chat, telegram, settings) {}
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onDone(picked.toList()) }) { Text("Done") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
