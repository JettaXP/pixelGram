package com.pixelgram.desktop.core

import androidx.compose.runtime.mutableStateMapOf
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

object Banners {
    const val Bot = "pixelGramCI_bot"
    private const val Base = "https://raw.githubusercontent.com/JettaXP/pixelGram/main/Banners"
    private const val RefreshMs = 60 * 60 * 1000L
    private const val TimeoutMs = 15_000
    private const val Width = 1500
    private const val Height = 500
    private const val MaxBytes = 500 * 1024
    private const val StartQuality = 0.9f
    private const val MinQuality = 0.5f
    private const val QualityStep = 0.1f

    private val dir: File by lazy { File(AppPaths.dataDir, "banners").apply { mkdirs() } }
    private val indexFile get() = File(dir, "index.json")
    private val etagFile get() = File(dir, "index.etag")
    private val index = mutableStateMapOf<Long, String>()
    private val ready = mutableStateMapOf<Long, File>()
    private val own = mutableStateMapOf<Long, File?>()
    private val loading = HashSet<Long>()
    private var lastRefresh = 0L
    private var started = false

    fun banner(userId: Long): File? {
        if (!started) start()
        if (own.containsKey(userId)) return own[userId]
        val hash = index[userId] ?: return null
        val file = File(dir, "$userId-$hash.jpg")
        ready[userId]?.takeIf { it == file }?.let { return it }
        if (file.isFile) {
            ready[userId] = file
            return file
        }
        if (loading.add(userId)) Thread {
            runCatching { download("$Base/$userId.jpg?h=$hash", file) }
                .onSuccess {
                    dir.listFiles { f -> f.name.startsWith("$userId-") && f != file }?.forEach { it.delete() }
                    ready[userId] = file
                }
                .onFailure { AppLog.write("Banner $userId failed", it) }
            loading.remove(userId)
        }.apply { isDaemon = true }.start()
        return null
    }

    private fun start() {
        started = true
        if (indexFile.isFile) parse(indexFile.readText())
        refresh()
    }

    fun refresh() {
        val now = System.currentTimeMillis()
        if (now - lastRefresh < RefreshMs) return
        lastRefresh = now
        Thread {
            runCatching {
                val connection = URI("$Base/index.json").toURL().openConnection() as HttpURLConnection
                connection.connectTimeout = TimeoutMs
                connection.readTimeout = TimeoutMs
                etagFile.takeIf { it.isFile && indexFile.isFile }?.readText()?.let { connection.setRequestProperty("If-None-Match", it) }
                when (connection.responseCode) {
                    HttpURLConnection.HTTP_OK -> {
                        val text = connection.inputStream.bufferedReader().use { it.readText() }
                        indexFile.writeText(text)
                        connection.getHeaderField("ETag")?.let(etagFile::writeText)
                        parse(text)
                    }
                    HttpURLConnection.HTTP_NOT_MODIFIED -> Unit
                    else -> AppLog.write("Banner index: HTTP ${connection.responseCode}")
                }
            }.onFailure { AppLog.write("Banner index failed", it) }
        }.apply { isDaemon = true }.start()
    }

    private fun parse(text: String) {
        val entries = Regex(""""(\d+)"\s*:\s*"([0-9a-f]+)"""").findAll(text)
            .associate { it.groupValues[1].toLong() to it.groupValues[2] }
        index.keys.retainAll(entries.keys)
        index.putAll(entries)
    }

    private fun download(url: String, target: File) {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = TimeoutMs
        connection.readTimeout = TimeoutMs
        if (connection.responseCode != HttpURLConnection.HTTP_OK) error("HTTP ${connection.responseCode}")
        val part = File(target.path + ".part")
        connection.inputStream.use { input -> part.outputStream().use { input.copyTo(it) } }
        if (!part.renameTo(target)) error("Couldn't save ${target.name}")
    }

    fun prepare(source: File): File? = runCatching {
        val image = ImageIO.read(source) ?: return null
        val sourceRatio = image.width.toDouble() / image.height
        val ratio = Width.toDouble() / Height
        val cropWidth = if (sourceRatio > ratio) (image.height * ratio).toInt() else image.width
        val cropHeight = if (sourceRatio > ratio) image.height else (image.width / ratio).toInt()
        val x = (image.width - cropWidth) / 2
        val y = (image.height - cropHeight) / 2
        val out = BufferedImage(Width, Height, BufferedImage.TYPE_INT_RGB)
        out.createGraphics().apply {
            setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
            setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            drawImage(image, 0, 0, Width, Height, x, y, x + cropWidth, y + cropHeight, null)
            dispose()
        }
        var quality = StartQuality
        var bytes = jpeg(out, quality)
        while (bytes.size > MaxBytes && quality > MinQuality) {
            quality -= QualityStep
            bytes = jpeg(out, quality)
        }
        File(dir, "upload-${System.currentTimeMillis()}.jpg").apply { writeBytes(bytes) }
    }.onFailure { AppLog.write("Banner prepare failed", it) }.getOrNull()

    private fun jpeg(image: BufferedImage, quality: Float): ByteArray {
        val writer = ImageIO.getImageWritersByFormatName("jpeg").next()
        val buffer = ByteArrayOutputStream()
        ImageIO.createImageOutputStream(buffer).use { stream ->
            writer.output = stream
            val params = writer.defaultWriteParam.apply {
                compressionMode = ImageWriteParam.MODE_EXPLICIT
                compressionQuality = quality
            }
            writer.write(null, IIOImage(image, null, null), params)
            writer.dispose()
        }
        return buffer.toByteArray()
    }

    fun setOwn(userId: Long, file: File?) {
        own[userId] = file
    }
}
