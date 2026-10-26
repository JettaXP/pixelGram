package com.pixelgram.desktop.core

import kotlin.math.PI
import kotlin.math.cos

internal class TimeStretch(private val channels: Int, rate: Int) {
    private val hop = (rate * HopSeconds).toInt().coerceAtLeast(MinHop)
    private val window = hop * 2
    private val search = (rate * SearchSeconds).toInt().coerceAtLeast(1)
    private val hann = FloatArray(window) { i -> (0.5 - 0.5 * cos(2 * PI * i / window)).toFloat() }
    private var input = FloatArray(0)
    private var frames = 0
    private var position = 0.0
    private var previous = -1
    private val tail = FloatArray(hop * channels)

    fun reset() {
        frames = 0
        position = 0.0
        previous = -1
        tail.fill(0f)
    }

    fun process(samples: ShortArray, count: Int, speed: Float): ShortArray {
        append(samples, count)
        val out = ShortArray(((count / speed).toInt() + hop * 2) * channels)
        var written = 0
        while (position.toInt() + search + window < frames) {
            val ideal = position.toInt().coerceAtLeast(search)
            val segment = if (previous < 0) ideal else best(ideal, previous + hop)
            for (i in 0 until hop) {
                for (c in 0 until channels) {
                    val value = tail[i * channels + c] + input[(segment + i) * channels + c] * hann[i]
                    tail[i * channels + c] = input[(segment + hop + i) * channels + c] * hann[hop + i]
                    if (written * channels + c < out.size) out[written * channels + c] = value.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
                written++
            }
            previous = segment
            position += hop * speed
        }
        compact()
        return if (written * channels == out.size) out else out.copyOf(written * channels)
    }

    private fun best(ideal: Int, natural: Int): Int {
        var bestOffset = ideal
        var bestScore = Double.NEGATIVE_INFINITY
        val from = (ideal - search).coerceAtLeast(0)
        val to = (ideal + search).coerceAtMost(frames - window)
        var candidate = from
        while (candidate <= to) {
            var score = 0.0
            var i = 0
            while (i < hop) {
                var a = 0f
                var b = 0f
                for (c in 0 until channels) {
                    a += input[(candidate + i) * channels + c]
                    b += input[(natural + i) * channels + c]
                }
                score += a * b
                i += CorrelationStep
            }
            if (score > bestScore) {
                bestScore = score
                bestOffset = candidate
            }
            candidate += SearchStep
        }
        return bestOffset
    }

    private fun append(samples: ShortArray, count: Int) {
        val needed = (frames + count) * channels
        if (input.size < needed) input = input.copyOf(maxOf(needed, input.size * 2))
        for (i in 0 until count * channels) input[frames * channels + i] = samples[i].toFloat()
        frames += count
    }

    private fun compact() {
        val keep = minOf(if (previous < 0) position.toInt() else previous, position.toInt()) - search
        if (keep <= 0) return
        System.arraycopy(input, keep * channels, input, 0, (frames - keep) * channels)
        frames -= keep
        position -= keep
        if (previous >= 0) previous -= keep
    }

    private companion object {
        const val HopSeconds = 0.015
        const val SearchSeconds = 0.008
        const val MinHop = 64
        const val SearchStep = 2
        const val CorrelationStep = 2
    }
}
