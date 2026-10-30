package com.pixelgram.setup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelgram.setup.Strings.tr
import java.io.File

private const val UpTurn = 180f

private class Place(val title: String, val icon: ImageVector, val dir: File)

private fun places(): List<Place> {
    val home = File(System.getProperty("user.home"))
    val apps = System.getenv("LOCALAPPDATA")?.let(::File)?.takeIf { it.isDirectory }
    return buildList {
        add(Place(tr("Home"), SetupIcons.Home, home))
        apps?.let { add(Place(tr("Apps"), SetupIcons.FolderOpen, it)) }
        File.listRoots().orEmpty().filter { it.exists() }.forEach { root -> add(Place(root.path.trimEnd('\\', '/').ifEmpty { "/" }, SetupIcons.Storage, root)) }
    }
}

private fun folders(dir: File): List<File> = dir.listFiles { file -> file.isDirectory && !file.isHidden && !file.name.startsWith('.') && !file.name.startsWith('$') }
    .orEmpty()
    .sortedBy { it.name.lowercase() }

@Composable
fun FolderPicker(start: File, onPicked: (File) -> Unit, onBack: () -> Unit) {
    var current by remember { mutableStateOf(generateSequence(start) { it.parentFile }.firstOrNull { it.isDirectory } ?: File(System.getProperty("user.home"))) }
    var creating by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val children = remember(current) { folders(current) }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.focusProperties { canFocus = false }) { Icon(SetupIcons.ArrowBack, contentDescription = tr("Cancel")) }
            Text(tr("Choose a folder"), style = MaterialTheme.typography.titleLarge)
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            remember { places() }.forEach { place ->
                FilterChip(
                    selected = current.absolutePath == place.dir.absolutePath,
                    onClick = { current = place.dir },
                    label = { Text(place.title) },
                    leadingIcon = { Icon(place.icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                )
            }
        }
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp)) {
                IconButton(onClick = { current.parentFile?.let { current = it } }, enabled = current.parentFile != null) {
                    Icon(SetupIcons.ArrowDown, contentDescription = "Up", modifier = Modifier.size(22.dp).rotate(UpTurn))
                }
                Text(current.absolutePath, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.StartEllipsis)
            }
        }
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (children.isEmpty()) {
                Box(contentAlignment = Alignment.Center) { Text(tr("This folder is empty"), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                LazyColumn(Modifier.padding(6.dp)) {
                    items(children, key = { it.absolutePath }) { folder ->
                        Surface(onClick = { current = folder }, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            ) {
                                Icon(SetupIcons.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(folder.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
        AnimatedVisibility(creating, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it.filter { ch -> ch !in "\\/:*?\"<>|" } }, label = { Text(tr("Folder name")) }, singleLine = true, modifier = Modifier.weight(1f))
                FilledTonalButton(onClick = {
                    val folder = File(current, name.trim())
                    if (name.isNotBlank() && (folder.isDirectory || folder.mkdirs())) {
                        current = folder
                        creating = false
                        name = ""
                        error = null
                    } else {
                        error = tr("Couldn't create the folder")
                    }
                }, enabled = name.isNotBlank()) { Text(tr("Create")) }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { creating = !creating }) {
                Icon(SetupIcons.CreateNewFolder, contentDescription = null, modifier = Modifier.padding(end = 8.dp).size(18.dp))
                Text(tr("New folder"))
            }
            TextButton(onClick = { systemFolder(current)?.let { current = it } }) { Text(tr("System dialog")) }
            Box(Modifier.weight(1f))
            Button(onClick = {
                if (writable(current)) onPicked(current) else error = tr("No write access to this folder. Choose a folder inside your user folder.")
            }) { Text(tr("Select")) }
        }
    }
}

fun writable(dir: File): Boolean {
    val existing = generateSequence(dir) { it.parentFile }.firstOrNull { it.isDirectory } ?: return false
    return runCatching {
        val probe = File.createTempFile("pixelgram", ".probe", existing)
        probe.delete()
    }.getOrDefault(false)
}

private fun systemFolder(start: File): File? {
    val chooser = javax.swing.JFileChooser(start).apply {
        fileSelectionMode = javax.swing.JFileChooser.DIRECTORIES_ONLY
        dialogTitle = tr("Choose a folder")
    }
    return if (chooser.showOpenDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
}
