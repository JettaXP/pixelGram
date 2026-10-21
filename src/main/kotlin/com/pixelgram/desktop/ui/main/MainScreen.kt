package com.pixelgram.desktop.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.telegram.ChatListKey
import com.pixelgram.desktop.telegram.Telegram

private data class FolderTab(val key: ChatListKey, val title: String)

@Composable
fun MainScreen(
    telegram: Telegram,
    settings: AppSettings,
    onOpenSettings: () -> Unit,
) {
    val tabs = buildList {
        if (!settings.hideAllChats.value || telegram.folders.isEmpty()) {
            add(FolderTab(ChatListKey.Main, "All chats"))
        }
        telegram.folders.forEach { add(FolderTab(ChatListKey.Folder(it.id), it.title)) }
    }
    var selectedKey by remember { mutableStateOf<ChatListKey>(ChatListKey.Main) }
    if (tabs.none { it.key == selectedKey } && tabs.isNotEmpty()) {
        selectedKey = tabs.first().key
    }
    var openChatId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(selectedKey) { telegram.loadChats(selectedKey) }

    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(Modifier.fillMaxSize()) {
            if (settings.materialFolders.value) {
                FoldersRail(
                    tabs = tabs,
                    selected = selectedKey,
                    onSelect = { selectedKey = it },
                    onOpenSettings = onOpenSettings,
                )
            }
            Column(
                Modifier
                    .width(360.dp)
                    .fillMaxHeight()
                    .padding(vertical = 8.dp),
            ) {
                if (!settings.materialFolders.value) {
                    FoldersTabs(tabs, selectedKey, onSelect = { selectedKey = it })
                }
                ChatList(
                    chats = telegram.chatList(selectedKey),
                    selectedChatId = openChatId,
                    telegram = telegram,
                    settings = settings,
                    onOpen = { openChatId = it },
                    onOpenSettings = if (settings.materialFolders.value) null else onOpenSettings,
                )
            }
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(top = 8.dp, end = 8.dp, bottom = 8.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
            ) {
                val chatId = openChatId
                val chat = chatId?.let { telegram.chats[it] }
                if (chat != null) {
                    ChatView(chat, telegram, settings)
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "Select a chat to start messaging",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

// Material 3 navigation rail with folders, like the Material 3 guidelines show.
@Composable
private fun FoldersRail(
    tabs: List<FolderTab>,
    selected: ChatListKey,
    onSelect: (ChatListKey) -> Unit,
    onOpenSettings: () -> Unit,
) {
    NavigationRail(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Spacer(Modifier.padding(top = 8.dp))
        tabs.forEach { tab ->
            val isSelected = tab.key == selected
            val isAll = tab.key == ChatListKey.Main
            NavigationRailItem(
                selected = isSelected,
                onClick = { onSelect(tab.key) },
                icon = {
                    Icon(
                        imageVector = when {
                            isAll && isSelected -> Icons.Filled.Forum
                            isAll -> Icons.Outlined.Forum
                            isSelected -> Icons.Filled.Folder
                            else -> Icons.Outlined.Folder
                        },
                        contentDescription = tab.title,
                    )
                },
                label = {
                    Text(tab.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
            )
        }
        Spacer(Modifier.weight(1f))
        NavigationRailItem(
            selected = false,
            onClick = onOpenSettings,
            icon = { Icon(Icons.Outlined.Settings, contentDescription = "Settings") },
            label = { Text("Settings") },
        )
        Spacer(Modifier.padding(bottom = 8.dp))
    }
}

@Composable
private fun FoldersTabs(
    tabs: List<FolderTab>,
    selected: ChatListKey,
    onSelect: (ChatListKey) -> Unit,
) {
    if (tabs.size < 2) return
    val index = tabs.indexOfFirst { it.key == selected }.coerceAtLeast(0)
    PrimaryScrollableTabRow(
        selectedTabIndex = index,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        edgePadding = 8.dp,
    ) {
        tabs.forEachIndexed { i, tab ->
            Tab(
                selected = i == index,
                onClick = { onSelect(tab.key) },
                text = { Text(tab.title, maxLines = 1) },
            )
        }
    }
}
