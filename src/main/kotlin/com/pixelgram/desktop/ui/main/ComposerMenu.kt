package com.pixelgram.desktop.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

@Composable
fun ComposerMenu(
    at: DpOffset,
    hasSelection: Boolean,
    onDismiss: () -> Unit,
    onCut: () -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onDelete: () -> Unit,
    onSelectAll: () -> Unit,
    onFormat: (FormatType) -> Unit,
    onClear: () -> Unit,
) {
    var formatting by remember { mutableStateOf(false) }
    Box(Modifier.offset(at.x, at.y)) {
        DropdownMenu(
            expanded = true,
            onDismissRequest = onDismiss,
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            AnimatedContent(
                targetState = formatting,
                transitionSpec = {
                    val direction = if (targetState) 1 else -1
                    (slideInHorizontally { it / 3 * direction } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it / 3 * direction } + fadeOut())
                },
            ) { inFormatting ->
                Column(Modifier.width(MenuWidth)) {
                    if (!inFormatting) {
                        Item("Cut", "Ctrl+X", hasSelection) { onCut(); onDismiss() }
                        Item("Copy", "Ctrl+C", hasSelection) { onCopy(); onDismiss() }
                        Item("Paste", "Ctrl+V", true) { onPaste(); onDismiss() }
                        Item("Delete", null, hasSelection) { onDelete(); onDismiss() }
                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        DropdownMenuItem(
                            text = { Text("Formatting") },
                            trailingIcon = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                            enabled = hasSelection,
                            onClick = { formatting = true },
                        )
                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        Item("Select All", "Ctrl+A", true) { onSelectAll(); onDismiss() }
                    } else {
                        DropdownMenuItem(
                            text = { Text("Formatting", style = MaterialTheme.typography.titleSmall) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") },
                            onClick = { formatting = false },
                        )
                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        FormatType.entries.filter { it != FormatType.Link && it != FormatType.Date }.forEach { type ->
                            Item(type.label, type.shortcut, true) { onFormat(type); onDismiss() }
                        }
                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        Item(FormatType.Link.label, FormatType.Link.shortcut, true) { onFormat(FormatType.Link); onDismiss() }
                        Item(FormatType.Date.label, FormatType.Date.shortcut, true) { onFormat(FormatType.Date); onDismiss() }
                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        Item("Clear formatting", "Ctrl+Shift+N", true) { onClear(); onDismiss() }
                    }
                }
            }
        }
    }
}

@Composable
private fun Item(label: String, shortcut: String?, enabled: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        trailingIcon = shortcut?.let {
            { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        },
        enabled = enabled,
        onClick = onClick,
    )
}

@Composable
fun LinkDialog(onDismiss: () -> Unit, onApply: (String) -> Unit) {
    var url by remember { mutableStateOf("https://") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create link") },
        text = {
            OutlinedTextField(value = url, onValueChange = { url = it }, singleLine = true, label = { Text("URL") })
        },
        confirmButton = {
            TextButton(onClick = { onApply(url.trim()) }, enabled = url.trim().length > "https://".length) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateDialog(onDismiss: () -> Unit, onApply: (Int) -> Unit) {
    val state = rememberDatePickerState()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { state.selectedDateMillis?.let { onApply((it / 1000 + NoonSeconds).toInt()) } },
                enabled = state.selectedDateMillis != null,
            ) { Text("Insert") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(state)
    }
}

private val MenuWidth = 240.dp
private const val NoonSeconds = 12 * 3600L
