package com.example.mykeyboardapp

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import android.os.VibrationEffect
import android.view.inputmethod.InputConnection

/**
 * KeyboardManager class to handle keyboard state and events.
 * Based on the design pattern from Florisboard.
 */
class KeyboardManager(private val context: Context) {
    
    companion object {
        const val KEYCODE_SHIFT = -1
        const val KEYCODE_MODE_CHANGE = -102
        const val KEYCODE_EMOJI = -101
        const val KEYCODE_SETTINGS = -103
        const val KEYCODE_DRAFT = -120
    }
    
    var isShifted = false
        private set
    
    var isSymbolMode = false
        private set
    
    private val mainHandler = Handler(Looper.getMainLooper())
    private var deleteHandler: Handler? = null
    private var deleteRunnable: Runnable? = null
    private var isDeleteKeyHeld = false
    
    /**
     * Toggle shift state and return the new state
     */
    fun toggleShift(): Boolean {
        isShifted = !isShifted
        return isShifted
    }
    
    /**
     * Toggle symbol mode and return the new state
     */
    fun toggleSymbolMode(): Boolean {
        isSymbolMode = !isSymbolMode
        if (isSymbolMode) {
            // When entering symbol mode, turn off shift
            isShifted = false
        }
        return isSymbolMode
    }
    
    /**
     * Process key press with haptic feedback
     */
    fun processKeyPress(keyCode: Int, ic: InputConnection) {
        // Provide haptic feedback
        provideHapticFeedback()
        
        when (keyCode) {
            -5 -> { // Delete key
                ic.deleteSurroundingText(1, 0)
            }
            10 -> { // Enter key
                ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER))
            }
            else -> {
                if (keyCode >= 0) { // Regular character
                    var code = keyCode.toChar()
                    if (isShifted && !isSymbolMode) {
                        code = code.uppercaseChar()
                    }
                    ic.commitText(code.toString(), 1)
                }
            }
        }
    }
    
    /**
     * Handle delete key press with continuous deletion
     */
    fun handleDeleteKeyPress(ic: InputConnection) {
        isDeleteKeyHeld = true
        
        deleteHandler = Handler(Looper.getMainLooper())
        deleteRunnable = object : Runnable {
            override fun run() {
                if (isDeleteKeyHeld) {
                    ic.deleteSurroundingText(1, 0)
                    deleteHandler?.postDelayed(this, 100) // delete every 100ms
                }
            }
        }
        deleteHandler?.post(deleteRunnable!!)
    }
    
    /**
     * Handle delete key release
     */
    fun handleDeleteKeyRelease() {
        isDeleteKeyHeld = false
        deleteHandler?.removeCallbacks(deleteRunnable!!)
    }
    
    /**
     * Provide haptic feedback on key press
     */
    private fun provideHapticFeedback() {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(30)
        }
    }
}
