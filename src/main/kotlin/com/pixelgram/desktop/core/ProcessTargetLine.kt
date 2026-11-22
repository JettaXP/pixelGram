package com.pixelgram.desktop.core

import java.io.File
import java.io.InputStream
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.Control
import javax.sound.sampled.DataLine
import javax.sound.sampled.LineListener
import javax.sound.sampled.TargetDataLine

object LinuxCapture {
    private fun onPath(name: String) = System.getenv("PATH").orEmpty().split(File.pathSeparator).any { File(it, name).canExecute() }

    val available: Boolean by lazy { AppPaths.isLinux && (onPath("parecord") || onPath("pw-record")) }

    data class Source(val name: String, val description: String)

    fun sources(): List<Source> {
        if (!AppPaths.isLinux || !onPath("pactl")) return emptyList()
        return runCatching {
            val process = ProcessBuilder("pactl", "list", "sources").redirectErrorStream(true).start()
            val text = process.inputStream.bufferedReader().readText()
            process.waitFor()
            text.split(Regex("\n(?=Source #)")).mapNotNull { block ->
                val name = Regex("\\n\\s*Name: (.+)").find(block)?.groupValues?.get(1)?.trim() ?: return@mapNotNull null
                if (name.endsWith(".monitor")) return@mapNotNull null
                val description = Regex("\\n\\s*Description: (.+)").find(block)?.groupValues?.get(1)?.trim() ?: name
                Source(name, description)
            }
        }.getOrDefault(emptyList())
    }

    fun command(format: AudioFormat, device: String?): List<String> {
        val rate = format.sampleRate.toInt().toString()
        val channels = format.channels.toString()
        return if (onPath("parecord")) {
            buildList {
                addAll(listOf("parecord", "--raw", "--format=s16le", "--rate=$rate", "--channels=$channels", "--latency-msec=40"))
                if (!device.isNullOrBlank()) add("--device=$device")
            }
        } else {
            buildList {
                addAll(listOf("pw-record", "--rate", rate, "--channels", channels, "--format", "s16"))
                if (!device.isNullOrBlank()) addAll(listOf("--target", device))
                add("-")
            }
        }
    }
}

class ProcessTargetLine(private val wanted: AudioFormat, private val device: String?) : TargetDataLine {
    private var process: Process? = null
    private var stream: InputStream? = null
    private var format: AudioFormat = wanted
    private var running = false
    private var frames = 0L

    override fun open(format: AudioFormat, bufferSize: Int) = open(format)

    override fun open(format: AudioFormat) {
        this.format = format
        val started = ProcessBuilder(LinuxCapture.command(format, device)).redirectError(ProcessBuilder.Redirect.DISCARD).start()
        Thread.sleep(60)
        if (!started.isAlive && started.exitValue() != 0) throw javax.sound.sampled.LineUnavailableException("Recorder exited")
        process = started
        stream = started.inputStream
    }

    override fun open() = open(wanted)

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val input = stream ?: return -1
        val frame = format.frameSize.coerceAtLeast(1)
        val wantedLength = len - len % frame
        var total = 0
        while (total < wantedLength) {
            val read = input.read(b, off + total, wantedLength - total)
            if (read < 0) break
            total += read
        }
        frames += total / frame
        return total
    }

    override fun close() {
        running = false
        process?.destroy()
        process = null
        runCatching { stream?.close() }
        stream = null
    }

    override fun start() { running = true }
    override fun stop() { running = false }
    override fun isRunning() = running
    override fun isActive() = running
    override fun isOpen() = process != null
    override fun drain() {}
    override fun flush() {}
    override fun getFormat() = format
    override fun getBufferSize() = (format.sampleRate * format.frameSize).toInt()
    override fun available() = runCatching { stream?.available() ?: 0 }.getOrDefault(0)
    override fun getFramePosition() = frames.toInt()
    override fun getLongFramePosition() = frames
    override fun getMicrosecondPosition() = (frames * 1_000_000L / format.sampleRate.toLong().coerceAtLeast(1))
    override fun getLevel() = -1f
    override fun getLineInfo() = DataLine.Info(TargetDataLine::class.java, format)
    override fun getControls(): Array<Control> = emptyArray()
    override fun isControlSupported(control: Control.Type) = false
    override fun getControl(control: Control.Type): Control = throw IllegalArgumentException("Unsupported control")
    override fun addLineListener(listener: LineListener) {}
    override fun removeLineListener(listener: LineListener) {}
}
