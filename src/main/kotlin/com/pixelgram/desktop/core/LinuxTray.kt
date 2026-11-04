package com.pixelgram.desktop.core

import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

object LinuxTray {
    private interface Gtk : Library {
        fun gtk_init_check(argc: Pointer?, argv: Pointer?): Boolean
        fun gtk_menu_new(): Pointer
        fun gtk_menu_item_new_with_label(label: String): Pointer
        fun gtk_separator_menu_item_new(): Pointer
        fun gtk_menu_shell_append(menu: Pointer, item: Pointer)
        fun gtk_widget_show_all(widget: Pointer)
        fun gtk_main()
    }

    private interface GObject : Library {
        fun g_signal_connect_data(instance: Pointer, signal: String, handler: Callback, data: Pointer?, destroy: Pointer?, flags: Int): Long
    }

    private interface Indicator : Library {
        fun app_indicator_new(id: String, icon: String, category: Int): Pointer
        fun app_indicator_set_status(indicator: Pointer, status: Int)
        fun app_indicator_set_menu(indicator: Pointer, menu: Pointer)
        fun app_indicator_set_icon_full(indicator: Pointer, icon: String, description: String)
        fun app_indicator_set_title(indicator: Pointer, title: String)
    }

    fun interface Activate : Callback {
        fun invoke(item: Pointer?, data: Pointer?)
    }

    private val keep = mutableListOf<Any>()
    private var indicator: Pointer? = null
    private var iconFile: File? = null

    @Volatile
    var running = false
        private set

    val needed: Boolean
        get() = AppPaths.isLinux && (System.getenv("WAYLAND_DISPLAY") != null || !java.awt.SystemTray.isSupported())

    fun start(icon: BufferedImage, openLabel: String, quitLabel: String, onOpen: () -> Unit, onQuit: () -> Unit): Boolean {
        if (running) return true
        val indicatorLib = loadIndicator() ?: return false
        val gtk = runCatching { Native.load("libgtk-3.so.0", Gtk::class.java) }.getOrNull() ?: return false
        val gobject = runCatching { Native.load("libgobject-2.0.so.0", GObject::class.java) }.getOrNull() ?: return false
        val file = writeIcon(icon) ?: return false
        val ready = java.util.concurrent.CountDownLatch(1)
        var ok = false
        Thread({
            runCatching {
                if (!gtk.gtk_init_check(null, null)) return@runCatching
                val menu = gtk.gtk_menu_new()
                val open = gtk.gtk_menu_item_new_with_label(openLabel)
                val quit = gtk.gtk_menu_item_new_with_label(quitLabel)
                val openHandler = Activate { _, _ -> java.awt.EventQueue.invokeLater(onOpen) }
                val quitHandler = Activate { _, _ -> java.awt.EventQueue.invokeLater(onQuit) }
                keep += openHandler
                keep += quitHandler
                gobject.g_signal_connect_data(open, "activate", openHandler, null, null, 0)
                gobject.g_signal_connect_data(quit, "activate", quitHandler, null, null, 0)
                gtk.gtk_menu_shell_append(menu, open)
                gtk.gtk_menu_shell_append(menu, gtk.gtk_separator_menu_item_new())
                gtk.gtk_menu_shell_append(menu, quit)
                gtk.gtk_widget_show_all(menu)
                val created = indicatorLib.app_indicator_new("pixelgram", file.absolutePath, 0)
                indicatorLib.app_indicator_set_icon_full(created, file.absolutePath, "pixelGram")
                indicatorLib.app_indicator_set_title(created, "pixelGram")
                indicatorLib.app_indicator_set_menu(created, menu)
                indicatorLib.app_indicator_set_status(created, 1)
                indicator = created
                keep += indicatorLib
                ok = true
                running = true
                ready.countDown()
                gtk.gtk_main()
            }.onFailure { AppLog.write("Linux tray failed", it) }
            running = false
            ready.countDown()
        }, "pixelgram-tray").apply { isDaemon = true }.start()
        ready.await(3, java.util.concurrent.TimeUnit.SECONDS)
        return ok
    }

    fun hide() {
        val current = indicator ?: return
        runCatching { loadIndicator()?.app_indicator_set_status(current, 0) }
    }

    fun show() {
        val current = indicator ?: return
        runCatching { loadIndicator()?.app_indicator_set_status(current, 1) }
    }

    private var indicatorLib: Indicator? = null

    private fun loadIndicator(): Indicator? {
        indicatorLib?.let { return it }
        val names = listOf("libayatana-appindicator3.so.1", "libappindicator3.so.1", "ayatana-appindicator3", "appindicator3")
        return names.firstNotNullOfOrNull { name -> runCatching { Native.load(name, Indicator::class.java) }.getOrNull() }
            ?.also { indicatorLib = it }
    }

    private fun writeIcon(icon: BufferedImage): File? = runCatching {
        val dir = File(System.getProperty("java.io.tmpdir"), "pixelgram-tray").apply { mkdirs() }
        File(dir, "pixelgram-${ProcessHandle.current().pid()}.png").also { file ->
            ImageIO.write(icon, "png", file)
            file.deleteOnExit()
            iconFile = file
        }
    }.getOrNull()
}
