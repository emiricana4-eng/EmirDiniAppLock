package com.applock.free

import android.content.Intent
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.KeyEvent
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.applock.free.databinding.ActivityLockBinding

class LockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLockBinding
    private lateinit var prefManager: PrefManager

    private var packageToUnlock = ""
    private var enteredPin = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefManager = PrefManager(this)
        packageToUnlock = intent.getStringExtra(EXTRA_PACKAGE) ?: ""

        // FIX: Fail-Closed Security
        // If there is no PIN, we DO NOT unlock. We stop the app.
        if (!prefManager.hasPin()) {
            Toast.makeText(this, "Security Error: No PIN set. Access Denied.", Toast.LENGTH_LONG).show()
            finish() 
            return
        }

        showAppName()
        setupNumpad()
        updateDots()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val newPackage = intent?.getStringExtra(EXTRA_PACKAGE)
        if (!newPackage.isNullOrEmpty() && newPackage != packageToUnlock) {
            packageToUnlock = newPackage
            showAppName()
            clearPin()
        }
    }

    private fun showAppName() {
        val label = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(packageToUnlock, 0)
            ).toString()
        } catch (e: Exception) { "App" }
        binding.tvAppName.text = "Unlock $label"
    }

    private fun setupNumpad() {
        val digitButtons = listOf(
            binding.btn0, binding.btn1, binding.btn2, binding.btn3, binding.btn4,
            binding.btn5, binding.btn6, binding.btn7, binding.btn8, binding.btn9
        )
        digitButtons.forEachIndexed { index, btn ->
            btn.setOnClickListener { appendDigit(index.toString()) }
        }
        binding.btnBackspace.setOnClickListener { removeLastDigit() }
        binding.btnClear.setOnClickListener { clearPin() }
    }

    private fun appendDigit(digit: String) {
        if (enteredPin.length >= MAX_PIN_LENGTH) return
        entered
