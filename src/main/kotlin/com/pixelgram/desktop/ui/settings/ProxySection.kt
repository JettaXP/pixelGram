package com.pixelgram.desktop.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.core.ProxyEntry
import com.pixelgram.desktop.core.ProxyKind
import com.pixelgram.desktop.core.tr
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.components.ConfirmOnEnter
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.components.expressive.AlertDialog
import com.pixelgram.desktop.ui.components.expressive.IconButton
import com.pixelgram.desktop.ui.components.expressive.TextButton
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.outlined.Add
import com.pixelgram.desktop.ui.icons.outlined.ContentCopy
import com.pixelgram.desktop.ui.icons.outlined.Delete
import com.pixelgram.desktop.ui.icons.outlined.Edit
import com.pixelgram.desktop.ui.icons.outlined.VpnKey
import com.pixelgram.desktop.ui.main.groupShape
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

private const val PingUnknown = -1L
private const val PingFailed = -2L
private const val MillisInSecond = 1000

@Composable
fun ProxySection(telegram: Telegram, settings: AppSettings) {
    val proxies = remember(settings.proxies.value) { ProxyEntry.decode(settings.proxies.value) }
    val selected = settings.proxySelected.value.takeIf { it in proxies.indices }
    val active = settings.proxyOn.value && selected != null
    var editing by remember { mutableStateOf<Int?>(null) }
    fun save(list: List<ProxyEntry>) {
        settings.proxies.value = ProxyEntry.encode(list)
    }

    Description("Connect to Telegram through a SOCKS5, HTTP or MTProto proxy. The proxy is used by all accounts.")
    Header("Connection")
    SwitchRow(
        index = 0,
        count = 1,
        title = "Use proxy",
        subtitle = when {
            proxies.isEmpty() -> tr("Add a proxy first")
            active -> proxies[selected!!].address
            else -> tr("Off")
        },
        checked = active,
        onChange = { on ->
            if (on && proxies.isEmpty()) {
                editing = -1
            } else {
                if (on && selected == null) settings.proxySelected.value = 0
                settings.proxyOn.value = on
            }
        },
    )

    Header("Proxies")
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        proxies.forEachIndexed { index, proxy ->
            ProxyRow(
                telegram,
                proxy,
                chosen = index == selected,
                index = index,
                count = proxies.size + 1,
                onSelect = {
                    settings.proxySelected.value = index
                    settings.proxyOn.value = true
                },
                onEdit = { editing = index },
                onDelete = {
                    val rest = proxies.filterIndexed { at, _ -> at != index }
                    val current = selected
                    when {
                        current == null -> Unit
                        current == index -> {
                            settings.proxyOn.value = false
                            settings.proxySelected.value = 0
                        }
                        current > index -> settings.proxySelected.value = current - 1
                    }
                    save(rest)
                },
            )
        }
        Surface(
            onClick = { editing = -1 },
            shape = groupShape(proxies.size, proxies.size + 1),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Add proxy", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    editing?.let { index ->
        ProxyDialog(
            initial = proxies.getOrNull(index),
            onDismiss = { editing = null },
        ) { proxy ->
            val list = if (index in proxies.indices) proxies.toMutableList().apply { set(index, proxy) } else proxies + proxy
            save(list)
            settings.proxySelected.value = if (index in proxies.indices) index else list.lastIndex
            settings.proxyOn.value = true
            editing = null
        }
    }
}

@Composable
private fun ProxyRow(
    telegram: Telegram,
    proxy: ProxyEntry,
    chosen: Boolean,
    index: Int,
    count: Int,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var ping by remember(proxy) { mutableStateOf(PingUnknown) }
    LaunchedEffect(proxy) {
        telegram.pingProxy(proxy) { seconds -> ping = seconds?.let { (it * MillisInSecond).toLong() } ?: PingFailed }
    }
    Surface(
        onClick = onSelect,
        shape = groupShape(index, count),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        ) {
            RadioButton(selected = chosen, onClick = onSelect)
            Column(Modifier.weight(1f)) {
                Text(proxy.address, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                val status = when (ping) {
                    PingUnknown -> tr("Checking…")
                    PingFailed -> tr("Unavailable")
                    else -> "$ping ${tr("ms")}"
                }
                Text(
                    "${proxy.kind.title} · $status",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (ping == PingFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(proxy.link()), null) }) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy link")
            }
            IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, contentDescription = "Edit") }
            IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, contentDescription = "Delete") }
        }
    }
}

@Composable
fun ProxyDialog(initial: ProxyEntry?, onDismiss: () -> Unit, onSave: (ProxyEntry) -> Unit) {
    var kind by remember { mutableStateOf(initial?.kind ?: ProxyKind.Socks5) }
    var link by remember { mutableStateOf("") }
    var server by remember { mutableStateOf(initial?.server.orEmpty()) }
    var port by remember { mutableStateOf(initial?.port?.toString().orEmpty()) }
    var username by remember { mutableStateOf(initial?.username.orEmpty()) }
    var password by remember { mutableStateOf(initial?.password.orEmpty()) }
    var secret by remember { mutableStateOf(initial?.secret.orEmpty()) }
    val proxy = ProxyEntry(
        kind,
        server.trim(),
        port.trim().toIntOrNull() ?: 0,
        if (kind == ProxyKind.MtProto) "" else username,
        if (kind == ProxyKind.MtProto) "" else password,
        if (kind == ProxyKind.MtProto) secret.trim() else "",
    )
    val confirm = { if (proxy.valid()) onSave(proxy) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.VpnKey, contentDescription = null) },
        title = { Text(if (initial == null) "Add proxy" else "Edit proxy") },
        text = {
            ConfirmOnEnter(confirm) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        link,
                        { text ->
                            link = text
                            ProxyEntry.parseLink(text)?.let { parsed ->
                                kind = parsed.kind
                                server = parsed.server
                                port = parsed.port.toString()
                                username = parsed.username
                                password = parsed.password
                                secret = parsed.secret
                            }
                        },
                        label = { Text("Proxy link") },
                        placeholder = { Text("t.me/proxy?… or t.me/socks?…") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ConnectedChoice(ProxyKind.entries.map { it to it.title }, kind, { kind = it })
                    OutlinedTextField(server, { server = it }, label = { Text("Server") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        port,
                        { text -> port = text.filter(Char::isDigit).take(PortDigits) },
                        label = { Text("Port") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (kind == ProxyKind.MtProto) {
                        OutlinedTextField(secret, { secret = it }, label = { Text("Secret") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    } else {
                        OutlinedTextField(username, { username = it }, label = { Text("Username (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(
                            password,
                            { password = it },
                            label = { Text("Password (optional)") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = confirm, enabled = proxy.valid()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private const val PortDigits = 5
