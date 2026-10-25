package com.pixelgram.desktop.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.core.I18n
import com.pixelgram.desktop.ui.components.BadgeTip
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.components.groupShapes
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.filled.Search

@Composable
fun LanguageSection(settings: AppSettings) {
    var query by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf<String?>(null) }
    val selected = I18n.code
    val shown = I18n.languages.filter {
        query.isBlank() || it.native.contains(query, ignoreCase = true) || it.english.contains(query, ignoreCase = true)
    }
    Box {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        TextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text("Search") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            shape = MaterialTheme.shapes.extraLarge,
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
        shown.forEachIndexed { index, language ->
            Surface(
                shape = groupShapes(index, shown.size).shape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .clickable {
                            if (language.available) settings.language.value = language.code else notice = NoTranslation
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    RadioButton(selected = language.code == selected, onClick = null, enabled = language.available)
                    Column {
                        androidx.compose.material3.Text(
                            language.native,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (language.available) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            language.english,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
    BadgeTip(notice) { notice = null }
    }
}

private const val NoTranslation = "There is no translation to this language yet"
