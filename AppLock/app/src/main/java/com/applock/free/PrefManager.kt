package com.applock.free

import android.content.Context
import java.security.MessageDigest

class PrefManager(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Securely retrieve the stored HASH
    var pin: String
        get() = prefs.getString(KEY_PIN, "") ?: ""
        set(value) {
            // ALWAYS hash the PIN before storing it
            prefs.edit()
                .putString(KEY_PIN, sha256(value))
                .putInt(KEY_PIN_LENGTH, value.length)
                .apply()
        }

    val pinLength: Int
        get() = prefs.getInt(KEY_PIN_LENGTH, 4)

    // Fail-Closed Validation Logic
    fun checkPin(input: String): Boolean {
        val storedPin = pin
        
        // 1. Guard: If input is blank, deny access.
        if (input.isBlank()) return false
        
        // 2. Guard: If no PIN is set, deny access. 
        // This prevents the "default-unlock" behavior if the app is uninitialized.
        if (storedPin.isEmpty()) return false
        
        // 3. Validation: Compare hashes.
        return sha256(input) == storedPin
    }

    // Hash implementation - Must be standard SHA-256
    private fun sha256(input: String): String {
        return try {
            val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    fun hasPin(): Boolean = pin.isNotEmpty()
    
    fun clearPin() {
        prefs.edit().remove(KEY_PIN).remove(KEY_PIN_LENGTH).apply()
    }

    companion object {
        private const val PREFS_NAME = "applock_prefs"
        private const val KEY_PIN = "pin"
        private const val KEY_PIN_LENGTH = "pin_length"
    }
}
