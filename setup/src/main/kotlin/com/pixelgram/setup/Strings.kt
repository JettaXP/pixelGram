package com.pixelgram.setup

import java.util.Locale

internal object Strings {
    private val language = System.getProperty("pixelgram.setup.lang") ?: Locale.getDefault().language
    val rtl = language == "he" || language == "iw"

    private val ru = mapOf(
        "Telegram client in Material 3 Expressive" to "Telegram-клиент в стиле Material 3 Expressive",
        "Install to" to "Папка установки",
        "Change" to "Изменить",
        "Open pixelGram when done" to "Запустить pixelGram",
        "Install" to "Установить",
        "Cancel" to "Отмена",
        "Choose a folder" to "Выберите папку",
        "New folder" to "Новая папка",
        "Folder name" to "Имя папки",
        "Create" to "Создать",
        "System dialog" to "Системный диалог",
        "Select" to "Выбрать",
        "This folder is empty" to "В этой папке нет подпапок",
        "Home" to "Домашняя",
        "Apps" to "Приложения",
        "No write access to this folder. Choose a folder inside your user folder." to "Нет доступа на запись в эту папку. Выберите папку внутри папки пользователя.",
        "Couldn't create the folder" to "Не удалось создать папку",
        "Installing pixelGram…" to "Устанавливаем pixelGram…",
        "Preparing" to "Подготовка",
        "Removing the previous version" to "Удаляем прошлую версию",
        "Copying new files" to "Копируем файлы",
        "Creating shortcuts" to "Создаём ярлыки",
        "Finishing" to "Завершаем",
        "pixelGram is installed" to "pixelGram установлен",
        "Open pixelGram" to "Открыть pixelGram",
        "Close" to "Закрыть",
        "Installation was cancelled" to "Установка отменена",
        "Installation failed" to "Установка не удалась",
        "Show log" to "Показать лог",
        "Try again" to "Повторить",
        "The installer package is missing." to "Не найден установочный пакет.",
        "Close pixelGram before installing." to "Закройте pixelGram перед установкой.",
        "Done" to "Готово",
    )

    private val he = mapOf(
        "Telegram client in Material 3 Expressive" to "לקוח טלגרם בסגנון Material 3 Expressive",
        "Install to" to "תיקיית התקנה",
        "Change" to "שנה",
        "Open pixelGram when done" to "הפעל את pixelGram",
        "Install" to "התקן",
        "Cancel" to "ביטול",
        "Choose a folder" to "בחר תיקייה",
        "New folder" to "תיקייה חדשה",
        "Folder name" to "שם התיקייה",
        "Create" to "צור",
        "System dialog" to "חלון מערכת",
        "Select" to "בחר",
        "This folder is empty" to "אין תיקיות משנה",
        "Home" to "בית",
        "Apps" to "אפליקציות",
        "No write access to this folder. Choose a folder inside your user folder." to "אין הרשאת כתיבה לתיקייה הזו. בחר תיקייה בתוך תיקיית המשתמש.",
        "Couldn't create the folder" to "לא ניתן ליצור את התיקייה",
        "Installing pixelGram…" to "מתקין את pixelGram…",
        "Preparing" to "מכין",
        "Removing the previous version" to "מסיר את הגרסה הקודמת",
        "Copying new files" to "מעתיק קבצים",
        "Creating shortcuts" to "יוצר קיצורי דרך",
        "Finishing" to "מסיים",
        "pixelGram is installed" to "pixelGram הותקן",
        "Open pixelGram" to "פתח את pixelGram",
        "Close" to "סגור",
        "Installation was cancelled" to "ההתקנה בוטלה",
        "Installation failed" to "ההתקנה נכשלה",
        "Show log" to "הצג יומן",
        "Try again" to "נסה שוב",
        "The installer package is missing." to "חבילת ההתקנה חסרה.",
        "Close pixelGram before installing." to "סגור את pixelGram לפני ההתקנה.",
        "Done" to "סיום",
    )

    fun tr(text: String): String = when (language) {
        "ru" -> ru[text]
        "he", "iw" -> he[text]
        else -> null
    } ?: text
}
