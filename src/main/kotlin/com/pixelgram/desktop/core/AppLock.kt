package com.pixelgram.desktop.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.awt.AWTEvent
import java.awt.Toolkit
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object AppLock {
    const val PinLength = 4
    private const val Iterations = 120_000
    private const val KeyBits = 256
    private const val SaltBytes = 16
    private const val MinuteMs = 60_000L

    var locked by mutableStateOf(false)
        private set
    private var settings: AppSettings? = null
    @Volatile private var lastActivity = System.currentTimeMillis()

    val enabled: Boolean get() = settings?.passcodeHash?.value?.isNotEmpty() == true

    fun start(settings: AppSettings) {
        if (this.settings != null) return
        this.settings = settings
        locked = enabled
        Toolkit.getDefaultToolkit().addAWTEventListener(
            { lastActivity = System.currentTimeMillis() },
            AWTEvent.KEY_EVENT_MASK or AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK or AWTEvent.MOUSE_WHEEL_EVENT_MASK,
        )
    }

    fun tick() {
        val minutes = settings?.autoLockMinutes?.value ?: 0
        if (enabled && !locked && minutes > 0 && System.currentTimeMillis() - lastActivity > minutes * MinuteMs) locked = true
    }

    fun lock() {
        if (enabled) locked = true
    }

    fun unlock(passcode: String): Boolean {
        val ok = check(passcode)
        if (ok) {
            locked = false
            lastActivity = System.currentTimeMillis()
        }
        return ok
    }

    fun check(passcode: String): Boolean {
        val current = settings ?: return false
        if (current.passcodeHash.value.isEmpty()) return true
        val salt = runCatching { Base64.getDecoder().decode(current.passcodeSalt.value) }.getOrNull() ?: return false
        val expected = runCatching { Base64.getDecoder().decode(current.passcodeHash.value) }.getOrNull() ?: return false
        return MessageDigest.isEqual(hash(passcode, salt), expected)
    }

    fun setPasscode(passcode: String?) {
        val current = settings ?: return
        if (passcode.isNullOrEmpty()) {
            current.passcodeHash.value = ""
            current.passcodeSalt.value = ""
            locked = false
            return
        }
        val salt = ByteArray(SaltBytes).also(SecureRandom()::nextBytes)
        current.passcodeSalt.value = Base64.getEncoder().encodeToString(salt)
        current.passcodeHash.value = Base64.getEncoder().encodeToString(hash(passcode, salt))
    }

    private fun hash(passcode: String, salt: ByteArray): ByteArray =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(passcode.toCharArray(), salt, Iterations, KeyBits))
            .encoded
}
