package com.pixelgram.desktop.ui.people

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
import com.pixelgram.desktop.ui.theme.emojiText

@Composable
fun ContactsScreen(
    telegram: Telegram,
    settings: AppSettings,
    onOpenChat: (Long) -> Unit,
    onBack: () -> Unit,
) {
    var ids by remember { mutableStateOf<List<Long>?>(null) }
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { telegram.contacts { ids = it } }

    PageScaffold("Contacts", onBack) {
        val loaded = ids
        if (loaded == null) {
            LoadingIndicator()
            return@PageScaffold
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        val contacts = loaded
            .mapNotNull { telegram.users[it] }
            .filter { query.isBlank() || it.fullName().contains(query.trim(), ignoreCase = true) }
            .sortedBy { it.fullName().lowercase() }
        if (contacts.isEmpty()) {
            Text(
                if (loaded.isEmpty()) "You have no contacts yet." else "Nothing found.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            modifier = Modifier.fillMaxWidth(),
        ) {
            contacts.forEachIndexed { index, user ->
                SegmentedListItem(
                    onClick = { telegram.openPrivateChat(user.id, onOpenChat) },
                    shapes = ListItemDefaults.segmentedShapes(index, contacts.size),
                    leadingContent = {
                        Avatar(user.fullName(), user.profilePhoto?.small, telegram, settings, size = 40.dp)
                    },
                    supportingContent = user.usernames?.activeUsernames?.firstOrNull()?.let { { Text("@$it") } },
                ) {
                    Text(emojiText(user.fullName()))
                }
            }
        }
    }
}
