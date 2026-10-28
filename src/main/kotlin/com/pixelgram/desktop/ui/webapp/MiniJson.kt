package com.pixelgram.desktop.ui.webapp

object MiniJson {
    fun parse(text: String): Any? = runCatching { Reader(text).run { value().also { space() } } }.getOrNull()

    fun quote(text: String): String = buildString {
        append('"')
        text.forEach { char ->
            when {
                char == '"' -> append("\\\"")
                char == '\\' -> append("\\\\")
                char == '\n' -> append("\\n")
                char == '\r' -> append("\\r")
                char == '\t' -> append("\\t")
                char < ' ' || char == ' ' || char == ' ' -> append("\\u%04x".format(char.code))
                else -> append(char)
            }
        }
        append('"')
    }

    fun write(value: Any?): String = when (value) {
        null -> "null"
        is String -> quote(value)
        is Boolean, is Int, is Long -> value.toString()
        is Number -> value.toDouble().let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() }
        is Map<*, *> -> value.entries.joinToString(",", "{", "}") { quote(it.key.toString()) + ":" + write(it.value) }
        is List<*> -> value.joinToString(",", "[", "]") { write(it) }
        else -> quote(value.toString())
    }

    private class Reader(val text: String) {
        var at = 0

        fun space() {
            while (at < text.length && text[at].isWhitespace()) at++
        }

        fun value(): Any? {
            space()
            return when (text[at]) {
                '{' -> obj()
                '[' -> list()
                '"' -> string()
                't' -> word("true", true)
                'f' -> word("false", false)
                'n' -> word("null", null)
                else -> number()
            }
        }

        fun word(word: String, result: Any?): Any? {
            require(text.startsWith(word, at))
            at += word.length
            return result
        }

        fun number(): Double {
            val start = at
            while (at < text.length && (text[at].isDigit() || text[at] in "+-.eE")) at++
            return text.substring(start, at).toDouble()
        }

        fun string(): String {
            at++
            val out = StringBuilder()
            while (text[at] != '"') {
                val char = text[at++]
                if (char != '\\') {
                    out.append(char)
                    continue
                }
                when (val escaped = text[at++]) {
                    'n' -> out.append('\n')
                    'r' -> out.append('\r')
                    't' -> out.append('\t')
                    'b' -> out.append('\b')
                    'f' -> out.append('\u000c')
                    'u' -> {
                        out.append(text.substring(at, at + 4).toInt(16).toChar())
                        at += 4
                    }
                    else -> out.append(escaped)
                }
            }
            at++
            return out.toString()
        }

        fun obj(): Map<String, Any?> {
            at++
            val map = LinkedHashMap<String, Any?>()
            space()
            if (text[at] == '}') return map.also { at++ }
            while (true) {
                space()
                val key = string()
                space()
                require(text[at++] == ':')
                map[key] = value()
                space()
                if (text[at++] == '}') return map
            }
        }

        fun list(): List<Any?> {
            at++
            val items = ArrayList<Any?>()
            space()
            if (text[at] == ']') return items.also { at++ }
            while (true) {
                items.add(value())
                space()
                if (text[at++] == ']') return items
            }
        }
    }
}
