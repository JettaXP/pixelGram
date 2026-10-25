package com.pixelgram.desktop.core

import java.util.concurrent.ConcurrentHashMap

class Language(val code: String, val native: String, val english: String, val available: Boolean, val rtl: Boolean = false)

object I18n {
    var settings: AppSettings? = null

    val languages = listOf(
        Language("en", "English", "English", true),
        Language("ru", "Русский", "Russian", true),
        Language("he", "עברית", "Hebrew", true, rtl = true),
        Language("uk", "Українська", "Ukrainian", false),
        Language("be", "Беларуская", "Belarusian", false),
        Language("ca", "Català", "Catalan", false),
        Language("hr", "Hrvatski", "Croatian", false),
        Language("cs", "Čeština", "Czech", false),
        Language("nl", "Nederlands", "Dutch", false),
        Language("fi", "Suomi", "Finnish", false),
        Language("fr", "Français", "French", false),
        Language("de", "Deutsch", "German", false),
        Language("hu", "Magyar", "Hungarian", false),
        Language("id", "Bahasa Indonesia", "Indonesian", false),
        Language("it", "Italiano", "Italian", false),
        Language("kk", "Қазақша", "Kazakh", false),
        Language("ko", "한국어", "Korean", false),
        Language("ms", "Bahasa Melayu", "Malay", false),
        Language("nb", "Norsk (bokmål)", "Norwegian (Bokmål)", false),
        Language("fa", "فارسی", "Persian", false, rtl = true),
        Language("pl", "Polski", "Polish", false),
        Language("pt-br", "Português (Brasil)", "Portuguese (Brazil)", false),
        Language("ro", "Română", "Romanian", false),
        Language("sr", "Српски", "Serbian", false),
        Language("sk", "Slovenčina", "Slovak", false),
        Language("es", "Español", "Spanish", false),
        Language("sv", "Svenska", "Swedish", false),
        Language("tr", "Türkçe", "Turkish", false),
        Language("uz", "O'zbek", "Uzbek", false),
        Language("ar", "العربية", "Arabic", false, rtl = true),
        Language("ja", "日本語", "Japanese", false),
        Language("zh-hans", "简体中文", "Chinese (Simplified)", false),
        Language("vi", "Tiếng Việt", "Vietnamese", false),
        Language("hi", "हिन्दी", "Hindi", false),
    )

    val code: String get() = settings?.language?.value?.takeIf { lang -> languages.any { it.code == lang && it.available } } ?: "en"

    val rtl: Boolean get() = languages.firstOrNull { it.code == code }?.rtl == true

    private val tables = ConcurrentHashMap<String, Map<String, String>>()

    private fun table(code: String): Map<String, String> = tables.getOrPut(code) {
        val stream = I18n::class.java.getResourceAsStream("/i18n/$code.tsv") ?: return@getOrPut emptyMap()
        stream.bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.filter { it.isNotBlank() && !it.startsWith("#") && '\t' in it }
                .associate { line ->
                    val (key, value) = line.split('\t', limit = 2)
                    key.replace("\\n", "\n") to value.replace("\\n", "\n")
                }
        }
    }

    fun tr(text: String): String {
        val lang = code
        if (lang == "en" || text.isBlank()) return text
        val table = table(lang)
        table[text]?.let { return it }
        if (!text.any(Char::isDigit)) return text
        val numbers = Digits.findAll(text).map { it.value }.toList()
        val pattern = table[Digits.replace(text, "{n}")] ?: return text
        var index = 0
        var last = ""
        return Slot.replace(pattern) { match ->
            val forms = match.groupValues[1]
            if (forms.isEmpty() || forms == "n") {
                last = numbers.getOrElse(index++) { "" }
                last
            } else {
                plural(lang, last.toLongOrNull() ?: 0, forms.split('|'))
            }
        }
    }

    private fun plural(lang: String, n: Long, forms: List<String>): String {
        if (forms.size == 1) return forms[0]
        return when (lang) {
            "ru", "uk", "be" -> {
                val tens = n % 100
                val ones = n % 10
                when {
                    ones == 1L && tens != 11L -> forms[0]
                    ones in 2..4 && tens !in 12..14 -> forms.getOrElse(1) { forms[0] }
                    else -> forms.getOrElse(2) { forms.last() }
                }
            }
            else -> if (n == 1L) forms[0] else forms.getOrElse(1) { forms[0] }
        }
    }

    private val Digits = Regex("\\d+")
    private val Slot = Regex("\\{([^}]*)\\}")
}

fun tr(text: String): String = I18n.tr(text)
