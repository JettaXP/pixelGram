package com.pixelgram.desktop.ui.main

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.pixelgram.desktop.core.AppPaths
import java.io.File
import java.util.Base64

/**
 * Attachments picked for a chat but not sent yet. TDLib only keeps text drafts, so photos and
 * documents are remembered here until the composer is cleared or the message is sent.
 */
object DraftAttachments {
    private val chats = mutableMapOf<Long, SnapshotStateList<File>>()
    private val store: File by lazy { File(AppPaths.dataDir, "draft-attachments.tsv") }
    private var loaded = false

    @Synchronized
    private fun ensureLoaded() {
        if (loaded) return
        loaded = true
        runCatching {
            if (!store.isFile) return@runCatching
            store.readLines().forEach { line ->
                val tab = line.indexOf('\t')
                if (tab <= 0) return@forEach
                val chatId = line.substring(0, tab).toLongOrNull() ?: return@forEach
                val files = line.substring(tab + 1).split(',').mapNotNull { encoded ->
                    val path = runCatching { String(Base64.getDecoder().decode(encoded)) }.getOrNull()
                    path?.let(::File)?.takeIf(File::isFile)
                }
                if (files.isNotEmpty()) chats[chatId] = mutableStateListOf<File>().apply { addAll(files) }
            }
        }
    }

    fun list(chatId: Long): SnapshotStateList<File> {
        ensureLoaded()
        return chats.getOrPut(chatId) { mutableStateListOf() }
    }

    fun save(chatId: Long, files: List<File>) {
        ensureLoaded()
        val current = chats[chatId]
        if (files.isEmpty()) {
            if (current != null && current.isNotEmpty()) {
                current.clear()
                chats.remove(chatId)
                persist()
            }
        } else {
            val target = current ?: mutableStateListOf<File>().also { chats[chatId] = it }
            if (target != files) {
                target.clear()
                target.addAll(files)
            }
            persist()
        }
    }

    fun clear(chatId: Long) {
        ensureLoaded()
        if (chats.remove(chatId) != null) persist()
    }

    private fun persist() {
        runCatching {
            val text = chats.entries
                .filter { it.value.isNotEmpty() }
                .joinToString("\n") { (id, files) ->
                    id.toString() + "\t" + files.joinToString(",") {
                        Base64.getEncoder().encodeToString(it.absolutePath.toByteArray())
                    }
                }
            store.parentFile?.mkdirs()
            store.writeText(text)
        }
    }
}
