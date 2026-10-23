package com.pixelgram.desktop.ui.components

import com.pixelgram.desktop.ui.OnEscape
import java.awt.datatransfer.StringSelection
import java.awt.Toolkit
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Box
import com.pixelgram.desktop.ui.theme.EmojiText
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun PageScaffold(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    OnEscape {
        onBack()
        true
    }
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                EmojiText(title, style = MaterialTheme.typography.titleLarge)
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier.widthIn(max = 640.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    content = content,
                )
            }
        }
    }
}

data class InfoRow(
    val icon: ImageVector,
    val title: String,
    val value: String,
    val phone: Boolean = false,
    val onEdit: (() -> Unit)? = null,
)

@Composable
fun InfoGroup(rows: List<InfoRow>) {
    Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        rows.forEachIndexed { index, row ->
            var menuAt by remember { mutableStateOf<DpOffset?>(null) }
            val density = LocalDensity.current
            val copy = { Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(row.value), null) }
            Box(
                Modifier.pointerInput(row) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                                val change = event.changes.first()
                                change.consume()
                                menuAt = with(density) { DpOffset(change.position.x.toDp(), change.position.y.toDp()) }
                            }
                        }
                    }
                },
            ) {
                SegmentedListItem(
                    onClick = { row.onEdit?.invoke() ?: copy() },
                    shapes = ListItemDefaults.segmentedShapes(index, rows.size),
                    leadingContent = { Icon(row.icon, contentDescription = null) },
                    supportingContent = { Text(row.title) },
                    trailingContent = row.onEdit?.let { { Icon(Icons.Outlined.Edit, contentDescription = "Edit") } },
                ) {
                    if (row.phone) PhoneText(row.value) else EmojiText(row.value)
                }
                menuAt?.let { at ->
                    Box(Modifier.offset(at.x, at.y)) {
                        DropdownMenu(
                            expanded = true,
                            onDismissRequest = { menuAt = null },
                            shape = RoundedCornerShape(16.dp),
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ) {
                            DropdownMenuItem(
                                text = { Text("Copy") },
                                leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                                onClick = {
                                    menuAt = null
                                    copy()
                                },
                            )
                            row.onEdit?.let { edit ->
                                DropdownMenuItem(
                                    text = { Text("Edit") },
                                    leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                                    onClick = {
                                        menuAt = null
                                        edit()
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
