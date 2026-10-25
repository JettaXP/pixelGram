package com.pixelgram.desktop.ui.people

import com.pixelgram.desktop.ui.icons.outlined.HowToReg
import com.pixelgram.desktop.ui.icons.Icons
import androidx.compose.material3.Icon
import com.pixelgram.desktop.ui.components.expressive.IconButton
import com.pixelgram.desktop.ui.components.BadgeTip
import com.pixelgram.desktop.ui.components.itemMotion
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyColumn
import com.pixelgram.desktop.ui.components.groupShapes
import com.pixelgram.desktop.ui.theme.EmojiVisualTransformation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.telegram.fullName
import com.pixelgram.desktop.ui.components.Avatar
import com.pixelgram.desktop.ui.components.PageScaffold
import com.pixelgram.desktop.ui.theme.EmojiText

@Composable
fun ContactsScreen(
    telegram: Telegram,
    settings: AppSettings,
    onOpenChat: (Long) -> Unit,
    onBack: () -> Unit,
) {
    var ids by remember { mutableStateOf(telegram.contactIds) }
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { telegram.contacts { ids = it } }

    PageScaffold("Contacts", onBack, scroll = false) {
        val loaded = ids
        if (loaded == null) {
            LoadingIndicator()
            return@PageScaffold
        }
        OutlinedTextField(
            value = query,
            visualTransformation = EmojiVisualTransformation,
            onValueChange = { query = it },
            label = { Text("Search") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        val sorted = remember(loaded, telegram.users.size) {
            loaded.mapNotNull { telegram.users[it] }.sortedBy { it.fullName().lowercase() }
        }
        val contacts = remember(sorted, query) {
            val needle = query.trim()
            if (needle.isEmpty()) sorted else sorted.filter { it.fullName().contains(needle, ignoreCase = true) }
        }
        if (contacts.isEmpty()) {
            Text(
                if (loaded.isEmpty()) "You have no contacts yet." else "Nothing found.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            itemsIndexed(contacts, key = { _, user -> user.id }) { index, user ->
                SegmentedListItem(
                    onClick = { telegram.openPrivateChat(user.id, onOpenChat) },
                    shapes = groupShapes(index, contacts.size),
                    modifier = itemMotion(),
                    leadingContent = {
                        Avatar(user.fullName(), user.profilePhoto?.small, telegram, settings, size = 40.dp)
                    },
                    supportingContent = user.usernames?.activeUsernames?.firstOrNull()?.let { { Text("@$it") } },
                    trailingContent = if (user.isMutualContact) {
                        { MutualBadge() }
                    } else {
                        null
                    },
                ) {
                    EmojiText(user.fullName())
                }
            }
        }
    }
}

@Composable
private fun MutualBadge() {
    var tip by remember { mutableStateOf<String?>(null) }
    BadgeTip(tip) { tip = null }
    IconButton(onClick = { tip = "Mutual contact: you're in their contacts too" }) {
        Icon(Icons.Outlined.HowToReg, contentDescription = "Mutual contact", tint = MaterialTheme.colorScheme.primary)
    }
}
