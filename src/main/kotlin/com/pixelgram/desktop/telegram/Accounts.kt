package com.pixelgram.desktop.telegram

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pixelgram.desktop.core.AppSettings
import kotlinx.coroutines.CoroutineScope

class Accounts(private val settings: AppSettings, private val scope: CoroutineScope) {
    val list = mutableStateListOf<Telegram>()

    var active by mutableStateOf(load())
        private set

    private fun load(): Telegram {
        val ids = settings.accounts.value.split(',')
            .mapNotNull { it.trim().toIntOrNull() }
            .distinct()
            .ifEmpty { listOf(0) }
        ids.forEach { list.add(create(it)) }
        return list.firstOrNull { it.account == settings.activeAccount.value } ?: list.first()
    }

    var onDuplicate: () -> Unit = {}

    private fun create(account: Int) = Telegram(account, settings, scope, onLoggedOut = ::onLoggedOut).also { telegram ->
        telegram.findDuplicate = { phone, userId ->
            list.firstOrNull { other ->
                val user = other.me
                other !== telegram && user != null &&
                    (userId != 0L && user.id == userId || !phone.isNullOrEmpty() && user.phoneNumber?.filter(Char::isDigit) == phone)
            }
        }
        telegram.onDuplicate = { existing ->
            switchTo(existing)
            onDuplicate()
        }
    }

    val canAdd: Boolean get() = list.size < MaxAccounts

    fun start() = list.forEach { it.start() }

    fun switchTo(telegram: Telegram) {
        active = telegram
        settings.activeAccount.value = telegram.account
    }

    fun add() {
        if (!canAdd) return
        val telegram = create(list.maxOf { it.account } + 1)
        list.add(telegram)
        save()
        telegram.start()
        switchTo(telegram)
    }

    fun cancelAdding(telegram: Telegram) {
        if (list.size < 2 || telegram.authState == AuthState.Ready) return
        telegram.close()
        remove(telegram)
    }

    fun closeAll() = list.forEach { it.close() }

    private fun onLoggedOut(telegram: Telegram): Boolean {
        if (list.size < 2) return false
        remove(telegram)
        return true
    }

    private fun remove(telegram: Telegram) {
        list.remove(telegram)
        save()
        if (active == telegram) switchTo(list.first())
    }

    private fun save() {
        settings.accounts.value = list.joinToString(",") { it.account.toString() }
    }

    private companion object {
        const val MaxAccounts = 10
    }
}
