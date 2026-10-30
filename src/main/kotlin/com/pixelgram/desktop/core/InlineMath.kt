package com.pixelgram.desktop.core

import java.math.BigDecimal
import java.math.MathContext

object InlineMath {
    private const val Allowed = "0123456789.,+-*/×÷^%() \t"
    private const val Operators = "+-*/×÷^%"
    private const val MaxLength = 200
    private val Precision = MathContext(12)

    fun answer(text: String): String? {
        if (!text.endsWith("=")) return null
        val body = text.dropLast(1)
        var start = body.length
        while (start > 0 && body[start - 1] in Allowed && body.length - start < MaxLength) start--
        val expression = body.substring(start).trim()
        if (expression.none(Char::isDigit) || expression.drop(1).none { it in Operators }) return null
        val value = runCatching { Parser(expression).parse() }.getOrNull() ?: return null
        if (value.isNaN() || value.isInfinite()) return null
        val plain = BigDecimal(value).round(Precision).stripTrailingZeros().toPlainString()
        return if (',' in expression && '.' !in expression) plain.replace('.', ',') else plain
    }

    private class Parser(source: String) {
        private val text = source.replace(" ", "").replace("\t", "").replace(',', '.')
        private var at = 0

        fun parse(): Double {
            val value = sum()
            require(at == text.length)
            return value
        }

        private fun peek(): Char? = text.getOrNull(at)

        private fun sum(): Double {
            var value = product()
            while (true) {
                value = when (peek()) {
                    '+' -> { at++; value + product() }
                    '-' -> { at++; value - product() }
                    else -> return value
                }
            }
        }

        private fun product(): Double {
            var value = power()
            while (true) {
                value = when (peek()) {
                    '*', '×' -> { at++; value * power() }
                    '/', '÷' -> {
                        at++
                        val divisor = power()
                        require(divisor != 0.0)
                        value / divisor
                    }
                    else -> return value
                }
            }
        }

        private fun power(): Double {
            val base = unary()
            if (peek() != '^') return base
            at++
            return Math.pow(base, power())
        }

        private fun unary(): Double = when (peek()) {
            '-' -> { at++; -unary() }
            '+' -> { at++; unary() }
            else -> percent()
        }

        private fun percent(): Double {
            var value = atom()
            while (peek() == '%') {
                at++
                value /= 100
            }
            return value
        }

        private fun atom(): Double {
            if (peek() == '(') {
                at++
                val value = sum()
                require(peek() == ')')
                at++
                return value
            }
            val from = at
            while (peek()?.let { it.isDigit() || it == '.' } == true) at++
            require(at > from)
            return text.substring(from, at).toDouble()
        }
    }
}
