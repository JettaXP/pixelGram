package com.pixelgram.desktop.telegram

import it.tdlight.jni.TdApi

private val isDemo get() = System.getProperty("pixelgram.demo") == "true"

private fun failure(result: TdApi.Object): String? = (result as? TdApi.Error)?.message?.let(::friendlyError)

fun translationLanguage(appLanguage: String): String = when (appLanguage) {
    "ru", "he", "en" -> appLanguage
    else -> "en"
}

fun Telegram.translateMessage(message: UiMessage, language: String, done: (String?, String?) -> Unit) {
    if (isDemo) return done("[$language] ${message.text}", null)
    send(TdApi.TranslateMessageText(message.chatId, message.id, language, "")) { result ->
        done((result as? TdApi.FormattedText)?.text, failure(result))
    }
}

sealed interface ReportStep {
    data class Options(val title: String, val options: List<Pair<ByteArray, String>>) : ReportStep
    data class Text(val optionId: ByteArray, val optional: Boolean) : ReportStep
    data object NeedsMessages : ReportStep
    data object Done : ReportStep
    data class Failed(val message: String) : ReportStep
}

fun Telegram.report(chatId: Long, optionId: ByteArray, messageIds: LongArray, text: String, done: (ReportStep) -> Unit) {
    if (isDemo) {
        return done(
            when {
                optionId.isEmpty() -> ReportStep.Options(
                    "Report",
                    listOf("Spam", "Violence", "Child abuse", "Illegal goods", "Personal details", "Other").map { it.toByteArray() to it },
                )
                String(optionId) == "Other" && text.isEmpty() -> ReportStep.Text(optionId, optional = false)
                else -> ReportStep.Done
            },
        )
    }
    send(TdApi.ReportChat(chatId, optionId, messageIds, text)) { result ->
        done(
            when (result) {
                is TdApi.ReportChatResultOptionRequired -> ReportStep.Options(result.title, result.options.map { it.id to it.text })
                is TdApi.ReportChatResultTextRequired -> ReportStep.Text(result.optionId, result.isOptional)
                is TdApi.ReportChatResultMessagesRequired -> ReportStep.NeedsMessages
                is TdApi.ReportChatResultOk -> ReportStep.Done
                else -> ReportStep.Failed(failure(result) ?: "Report failed")
            },
        )
    }
}

fun Telegram.recognizeSpeech(message: UiMessage, done: (String?) -> Unit) {
    if (isDemo) {
        val list = messages(message.chatId)
        val index = list.indexOfFirst { it.key == message.key }
        val content = list.getOrNull(index)?.content as? TdApi.MessageVoiceNote ?: return done(null)
        val voice = content.voiceNote
        val updated = TdApi.VoiceNote(voice.duration, voice.waveform, voice.mimeType, TdApi.SpeechRecognitionResultText("This is how a voice message looks as text."), voice.voice)
        list[index] = list[index].copy(content = TdApi.MessageVoiceNote(updated, content.caption, content.isListened))
        return done(null)
    }
    send(TdApi.RecognizeSpeech(message.chatId, message.id)) { done(failure(it)) }
}

fun speechText(result: TdApi.SpeechRecognitionResult?): Pair<String, Boolean>? = when (result) {
    is TdApi.SpeechRecognitionResultText -> result.text to false
    is TdApi.SpeechRecognitionResultPending -> result.partialText.ifEmpty { "…" } to true
    is TdApi.SpeechRecognitionResultError -> (result.error?.message?.let(::friendlyError) ?: "Couldn't recognize speech") to false
    else -> null
}

private var demoPassword: TdApi.PasswordState = TdApi.PasswordState().apply { passwordHint = "" }

fun Telegram.passwordState(done: (TdApi.PasswordState?) -> Unit) {
    if (isDemo) return done(demoPassword)
    send(TdApi.GetPasswordState()) { done(it as? TdApi.PasswordState) }
}

fun Telegram.setCloudPassword(old: String, new: String, hint: String, email: String, done: (TdApi.PasswordState?, String?) -> Unit) {
    if (isDemo) {
        demoPassword = TdApi.PasswordState().apply {
            hasPassword = new.isNotEmpty()
            passwordHint = hint
            hasRecoveryEmailAddress = email.isNotBlank()
        }
        return done(demoPassword, null)
    }
    send(TdApi.SetPassword(old, new, hint, email.isNotBlank(), email.trim())) { result -> done(result as? TdApi.PasswordState, failure(result)) }
}

fun Telegram.confirmRecoveryEmail(code: String, done: (TdApi.PasswordState?, String?) -> Unit) {
    if (isDemo) return done(demoPassword, null)
    send(TdApi.CheckRecoveryEmailAddressCode(code.trim())) { result -> done(result as? TdApi.PasswordState, failure(result)) }
}

fun Telegram.stopLiveLocation(message: UiMessage) {
    if (isDemo) return
    send(TdApi.EditMessageLiveLocation(message.chatId, message.id, null, null))
}
