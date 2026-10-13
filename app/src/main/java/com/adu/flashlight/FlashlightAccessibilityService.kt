package com.adu.flashlight

import android.accessibilityservice.AccessibilityService
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

class FlashlightAccessibilityService : AccessibilityService() {
    private var lastVolumeUpPress = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        FlashlightController.initialize(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_VOLUME_UP) return false

        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            val now = android.os.SystemClock.elapsedRealtime()
            if (now - lastVolumeUpPress in 1..DOUBLE_PRESS_WINDOW_MS) {
                lastVolumeUpPress = 0L
                if (!FlashlightController.toggle()) {
                    Toast.makeText(
                        this,
                        FlashlightController.error.value ?: "Could not switch on the flashlight.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                lastVolumeUpPress = now
            }
        }
        return true
    }

    companion object {
        private const val DOUBLE_PRESS_WINDOW_MS = 450L
    }
}
