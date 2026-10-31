package com.pixelgram.desktop.ui.webapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import com.pixelgram.desktop.core.AppSettings
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.telegram.closeWebApp
import com.pixelgram.desktop.telegram.sendWebAppData
import com.pixelgram.desktop.ui.components.GroupedMenu
import com.pixelgram.desktop.ui.components.MenuEntry
import com.pixelgram.desktop.ui.components.MenuGroups
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.components.expressive.AlertDialog
import com.pixelgram.desktop.ui.components.expressive.Button
import com.pixelgram.desktop.ui.components.expressive.IconButton
import com.pixelgram.desktop.ui.components.expressive.TextButton
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.automirrored.filled.ArrowBack
import com.pixelgram.desktop.ui.icons.filled.Close
import com.pixelgram.desktop.ui.icons.filled.MoreVert
import com.pixelgram.desktop.ui.icons.outlined.Refresh
import com.pixelgram.desktop.ui.icons.outlined.Settings
import com.pixelgram.desktop.ui.main.Links
import com.pixelgram.desktop.ui.ResizeHandles
import com.pixelgram.desktop.ui.TitleBar
import com.pixelgram.desktop.ui.rememberWindowChrome
import com.pixelgram.desktop.ui.theme.PixelTheme
import com.pixelgram.desktop.ui.theme.appColorScheme
import it.tdlight.jni.TdApi
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import javax.swing.SwingUtilities

class MiniAppSession(
    val telegram: Telegram,
    val botUserId: Long,
    val title: String,
    val url: String,
    val chatId: Long,
    val launchId: Long = 0,
    val buttonText: String? = null,
)

object MiniApps {
    val open = mutableStateListOf<MiniAppSession>()

    @Volatile
    var theme: TdApi.ThemeParameters? = null

    @Volatile
    var android = false

    fun launch(session: MiniAppSession) = SwingUtilities.invokeLater { open.add(session) }
}

private data class MainButton(val text: String, val color: Color?, val textColor: Color?, val active: Boolean, val progress: Boolean)

private data class Popup(val title: String, val message: String, val buttons: List<Pair<String, String>>)

private const val AndroidUserAgent = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
private const val ViewportHeight = 640

private fun hex(color: Color) = "#%06x".format(color.toArgb() and 0xFFFFFF)

private fun rgb(color: Color) = color.toArgb() and 0xFFFFFF

private fun parseColor(value: Any?): Color? = (value as? String)?.removePrefix("#")?.takeIf { it.length == 6 }?.toIntOrNull(16)?.let { Color(0xFF000000.toInt() or it) }

fun themeParameters(colors: ColorScheme) = TdApi.ThemeParameters(
    rgb(colors.surface),
    rgb(colors.surfaceContainer),
    rgb(colors.surfaceContainer),
    rgb(colors.surfaceContainer),
    rgb(colors.surfaceContainerLow),
    rgb(colors.outlineVariant),
    rgb(colors.onSurface),
    rgb(colors.primary),
    rgb(colors.primary),
    rgb(colors.onSurfaceVariant),
    rgb(colors.error),
    rgb(colors.onSurfaceVariant),
    rgb(colors.primary),
    rgb(colors.primary),
    rgb(colors.onPrimary),
)

private fun themeJson(colors: ColorScheme): Map<String, Any?> = mapOf(
    "bg_color" to hex(colors.surface),
    "secondary_bg_color" to hex(colors.surfaceContainer),
    "header_bg_color" to hex(colors.surfaceContainer),
    "bottom_bar_bg_color" to hex(colors.surfaceContainer),
    "section_bg_color" to hex(colors.surfaceContainerLow),
    "section_separator_color" to hex(colors.outlineVariant),
    "text_color" to hex(colors.onSurface),
    "accent_text_color" to hex(colors.primary),
    "section_header_text_color" to hex(colors.primary),
    "subtitle_text_color" to hex(colors.onSurfaceVariant),
    "destructive_text_color" to hex(colors.error),
    "hint_color" to hex(colors.onSurfaceVariant),
    "link_color" to hex(colors.primary),
    "button_color" to hex(colors.primary),
    "button_text_color" to hex(colors.onPrimary),
)

private fun platformUrl(url: String, android: Boolean): String =
    if (android) url.replace(Regex("tgWebAppPlatform=[^&]*"), "tgWebAppPlatform=android") else url

@Composable
fun MiniAppHost(settings: AppSettings, icon: Painter?) {
    val colors = appColorScheme(settings)
    SideEffect {
        MiniApps.theme = themeParameters(colors)
        MiniApps.android = settings.miniAppsAndroid.value
    }
    MiniApps.open.toList().forEach { session ->
        key(session) {
            MiniAppWindow(session, settings, icon) {
                MiniApps.open.remove(session)
                session.telegram.closeWebApp(session.launchId)
            }
        }
    }
}

@Composable
private fun MiniAppWindow(session: MiniAppSession, settings: AppSettings, icon: Painter?, onClose: () -> Unit) {
    val ownFrame = remember { settings.customTitleBar.value && !com.pixelgram.desktop.core.AppPaths.isMac }
    Window(onCloseRequest = onClose, title = session.title, icon = icon, undecorated = ownFrame, state = rememberWindowState(size = DpSize(440.dp, 760.dp))) {
        val chrome = rememberWindowChrome()
        PixelTheme(settings) {
            val colors = MaterialTheme.colorScheme
            var mainButton by remember { mutableStateOf<MainButton?>(null) }
            var backButton by remember { mutableStateOf(false) }
            var settingsButton by remember { mutableStateOf(false) }
            var popup by remember { mutableStateOf<Popup?>(null) }
            var loading by remember { mutableStateOf(true) }
            var menu by remember { mutableStateOf(false) }
            var header by remember { mutableStateOf<Color?>(null) }
            val browser = remember {
                lateinit var self: MiniAppBrowser
                self = MiniAppBrowser { type, data ->
                    when (type) {
                        "web_app_ready" -> loading = false
                        "web_app_close" -> onClose()
                        "web_app_expand", "web_app_request_viewport" ->
                            self.send("viewport_changed", mapOf("height" to ViewportHeight, "is_state_stable" to true, "is_expanded" to true))
                        "web_app_request_theme" -> self.send("theme_changed", mapOf("theme_params" to themeJson(colors)))
                        "web_app_setup_main_button" -> mainButton = if (data["is_visible"] == true) {
                            MainButton(
                                data["text"] as? String ?: "",
                                parseColor(data["color"]),
                                parseColor(data["text_color"]),
                                data["is_active"] != false,
                                data["is_progress_visible"] == true,
                            )
                        } else {
                            null
                        }
                        "web_app_setup_back_button" -> backButton = data["is_visible"] == true
                        "web_app_setup_settings_button" -> settingsButton = data["is_visible"] == true
                        "web_app_set_header_color" -> header = parseColor(data["color"])
                        "web_app_open_link" -> (data["url"] as? String)?.let(Links::openBrowser)
                        "web_app_open_tg_link" -> (data["path_full"] as? String)?.let { Links.open("https://t.me$it") }
                        "web_app_data_send" -> session.buttonText?.let { text ->
                            session.telegram.sendWebAppData(session.botUserId, text, data["data"] as? String ?: "")
                            onClose()
                        }
                        "web_app_open_popup" -> popup = Popup(
                            data["title"] as? String ?: session.title,
                            data["message"] as? String ?: "",
                            (data["buttons"] as? List<*>).orEmpty().mapNotNull { item ->
                                val button = item as? Map<*, *> ?: return@mapNotNull null
                                val id = button["id"] as? String ?: ""
                                val text = button["text"] as? String ?: when (button["type"]) {
                                    "ok" -> "OK"
                                    "close" -> "Close"
                                    "cancel" -> "Cancel"
                                    else -> ""
                                }
                                id to text
                            }.ifEmpty { listOf("" to "Close") },
                        )
                        "web_app_read_text_from_clipboard" -> {
                            val text = runCatching { Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor) as? String }.getOrNull()
                            self.send("clipboard_text_received", mapOf("req_id" to data["req_id"], "data" to text))
                        }
                        "web_app_request_phone" -> self.send("phone_requested", mapOf("status" to "cancelled"))
                        "web_app_request_write_access" -> self.send("write_access_requested", mapOf("status" to "cancelled"))
                        "web_app_open_invoice" -> self.send("invoice_closed", mapOf("slug" to data["slug"], "status" to "failed"))
                        "web_app_request_fullscreen" -> self.send("fullscreen_failed", mapOf("error" to "UNSUPPORTED"))
                        "web_app_check_home_screen" -> self.send("home_screen_checked", mapOf("status" to "unsupported"))
                        "web_app_invoke_custom_method" -> session.telegram.send(
                            TdApi.SendWebAppCustomRequest(session.botUserId, data["method"] as? String ?: "", MiniJson.write(data["params"])),
                        ) { result ->
                            val answer = (result as? TdApi.CustomRequestResult)?.result?.let(MiniJson::parse)
                            val error = (result as? TdApi.Error)?.message
                            self.send("custom_method_invoked", mapOf("req_id" to data["req_id"], "result" to answer, "error" to error))
                        }
                    }
                }
                self
            }
            DisposableEffect(browser) {
                val android = settings.miniAppsAndroid.value
                browser.load(platformUrl(session.url, android), if (android) AndroidUserAgent else null, rgb(colors.surface))
                onDispose { browser.dispose() }
            }
            Box(Modifier.fillMaxSize()) {
            Surface(color = colors.surface, modifier = Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize()) {
                    if (ownFrame) TitleBar(chrome, onClose = onClose, title = session.title)
                    Surface(color = header ?: colors.surfaceContainer) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp)) {
                            if (backButton || !ownFrame) {
                                IconButton(onClick = { if (backButton) browser.send("back_button_pressed") else onClose() }) {
                                    Icon(if (backButton) Icons.AutoMirrored.Filled.ArrowBack else Icons.Filled.Close, contentDescription = if (backButton) "Back" else "Close")
                                }
                            } else {
                                androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
                            }
                            Text(
                                session.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            )
                            Box {
                                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "More") }
                                GroupedMenu(expanded = menu, onDismiss = { menu = false }) {
                                    MenuGroups(listOf(buildList {
                                        if (settingsButton) add(MenuEntry(Icons.Outlined.Settings, "Settings") {
                                            menu = false
                                            browser.send("settings_button_pressed")
                                        })
                                        add(MenuEntry(Icons.Outlined.Refresh, "Reload page") {
                                            menu = false
                                            browser.reload()
                                        })
                                    }))
                                }
                            }
                        }
                    }
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        SwingPanel(factory = { browser.panel }, modifier = Modifier.fillMaxSize())
                        if (loading) LoadingIndicator(Modifier.align(Alignment.Center).size(48.dp))
                    }
                    mainButton?.let { button ->
                        Button(
                            onClick = { browser.send("main_button_pressed") },
                            enabled = button.active && !button.progress,
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = button.color ?: colors.primary,
                                contentColor = button.textColor ?: colors.onPrimary,
                            ),
                            modifier = Modifier.fillMaxWidth().padding(12.dp).height(48.dp),
                        ) { Text(button.text, maxLines = 1) }
                    }
                }
            }
            if (ownFrame) ResizeHandles(chrome)
            }
            popup?.let { shown ->
                val close = { id: String ->
                    popup = null
                    browser.send("popup_closed", mapOf("button_id" to id))
                }
                AlertDialog(
                    onDismissRequest = { close("") },
                    title = { Text(shown.title) },
                    text = { Text(shown.message) },
                    confirmButton = {
                        Row {
                            shown.buttons.forEach { (id, text) -> TextButton(onClick = { close(id) }) { Text(text) } }
                        }
                    },
                )
            }
        }
    }
}
