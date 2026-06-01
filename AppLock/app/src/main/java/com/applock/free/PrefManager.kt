package com.applock.free

import android.content.Context
import java.security.MessageDigest

class PrefManager(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var pin: String
        get() = prefs.getString(KEY_PIN, "") ?: ""
        set(value) {
            // Store the HASH, not the raw PIN
            prefs.edit()
                .putString(KEY_PIN, sha256(value))
                .putInt(KEY_PIN_LENGTH, value.length)
                .apply()
        }

    val pinLength: Int
        get() = prefs.getInt(KEY_PIN_LENGTH, 4)

    // Secure validation: Fail if empty, fail if no PIN set
    fun checkPin(input: String): Boolean {
        val storedPin = pin
        if (input.isBlank() || storedPin.isEmpty()) return false
        return sha256(input) == storedPin
    }

    // SHA-256 Hashing Implementation
    private fun sha256(input: String): String {
        return try {
            val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    // ... (Keep the rest of your existing functions like isEnabled, lockedApps, etc.)
    
    companion object {
        private const val PREFS_NAME = "applock_prefs"
        private const val KEY_PIN = "pin"
        private const val KEY_PIN_LENGTH = "pin_length"
        // ... (Keep your other keys)
    }
}
