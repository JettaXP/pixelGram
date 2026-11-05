package com.pixelgram.desktop.core

enum class SideItem(val title: String, val group: Int) {
    Theme("Night mode", 0),
    Saved("Saved", 0),
    Profile("My Profile", 1),
    Contacts("Contacts", 1),
    Calls("Calls", 1),
    Money("Money", 1),
    NewGroup("New Group", 2),
    NewChannel("New Channel", 2),
    Settings("Settings", 3),
    PixelSettings("pixelGram Preferences", 3),
}

data class SideLayout(val tiles: List<SideItem>, val list: List<SideItem>, val hidden: List<SideItem>)

object SideMenuLayout {
    val default = SideLayout(
        listOf(SideItem.Theme, SideItem.Saved),
        listOf(SideItem.Profile, SideItem.Contacts, SideItem.Calls, SideItem.Money, SideItem.NewGroup, SideItem.NewChannel, SideItem.Settings),
        listOf(SideItem.PixelSettings),
    )
    private val hiddenByDefault = setOf(SideItem.PixelSettings)

    fun read(settings: AppSettings): SideLayout {
        val raw = settings.sideMenuLayout.value
        if (raw.isBlank()) return default
        val parts = raw.split('|')
        fun names(index: Int) = parts.getOrNull(index).orEmpty().split(',').mapNotNull { name -> SideItem.entries.firstOrNull { it.name == name } }
        val tiles = names(0).distinct()
        val list = names(1).filter { it !in tiles }.distinct()
        val hidden = names(2).filter { it !in tiles && it !in list }.distinct()
        val missing = SideItem.entries.filter { it !in tiles && it !in list && it !in hidden }
        return SideLayout(tiles, list + missing.filter { it !in hiddenByDefault }, hidden.filter { it != SideItem.Settings } + missing.filter { it in hiddenByDefault })
    }

    fun write(settings: AppSettings, layout: SideLayout) {
        settings.sideMenuLayout.value = listOf(layout.tiles, layout.list, layout.hidden).joinToString("|") { part -> part.joinToString(",") { it.name } }
    }
}
