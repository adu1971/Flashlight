package com.adu.flashlight

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityButtonController
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

class FlashlightAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var pendingVolumeUpPress: Long? = null
    private val singlePress = Runnable { finishSinglePress() }
    // Consumed accessibility key events do not reliably receive system repeats.
    private val repeatVolumeUp = object : Runnable {
        override fun run() {
            finishSinglePress()
            raiseVolume()
            handler.postDelayed(this, VOLUME_REPEAT_INTERVAL_MS)
        }
    }
    private val accessibilityButtonCallback =
        object : AccessibilityButtonController.AccessibilityButtonCallback() {
            override fun onClicked(controller: AccessibilityButtonController) {
                toggleFlashlight()
            }
        }

    private fun toggleFlashlight() {
        if (!FlashlightController.toggle()) {
            Toast.makeText(
                this,
                FlashlightController.error.value ?: "Could not switch on the flashlight.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun raiseVolume() {
        getSystemService(AudioManager::class.java).adjustSuggestedStreamVolume(
            AudioManager.ADJUST_RAISE,
            AudioManager.USE_DEFAULT_STREAM_TYPE,
            AudioManager.FLAG_SHOW_UI or AudioManager.FLAG_PLAY_SOUND
        )
    }

    private fun finishSinglePress() {
        if (pendingVolumeUpPress == null) return
        handler.removeCallbacks(singlePress)
        pendingVolumeUpPress = null
        raiseVolume()
    }

    private fun clearPendingPress() {
        handler.removeCallbacks(singlePress)
        pendingVolumeUpPress = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        FlashlightController.initialize(applicationContext)
        accessibilityButtonController.registerAccessibilityButtonCallback(
            accessibilityButtonCallback,
            handler
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() {
        handler.removeCallbacks(repeatVolumeUp)
        clearPendingPress()
    }

    override fun onDestroy() {
        handler.removeCallbacks(repeatVolumeUp)
        accessibilityButtonController.unregisterAccessibilityButtonCallback(accessibilityButtonCallback)
        clearPendingPress()
        super.onDestroy()
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_VOLUME_UP) {
            // Preserve the ordering when Volume Down follows a single Volume Up.
            if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN &&
                event.action == KeyEvent.ACTION_DOWN) {
                finishSinglePress()
            }
            return false
        }

        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            // Start the window when the service receives the press. Key events
            // may already be old by the time accessibility dispatches them.
            val now = SystemClock.elapsedRealtime()
            val previousPress = pendingVolumeUpPress
            handler.removeCallbacks(repeatVolumeUp)
            if (previousPress != null && now - previousPress in 0..DOUBLE_PRESS_WINDOW_MS) {
                clearPendingPress()
                toggleFlashlight()
            } else {
                finishSinglePress()
                pendingVolumeUpPress = now
                handler.postDelayed(singlePress, DOUBLE_PRESS_WINDOW_MS)
                handler.postDelayed(repeatVolumeUp, ViewConfiguration.getLongPressTimeout().toLong())
            }
        } else if (event.action == KeyEvent.ACTION_UP) {
            handler.removeCallbacks(repeatVolumeUp)
        }
        // Consume both down and up; single presses are handled through AudioManager.
        return true
    }

    companion object {
        private const val DOUBLE_PRESS_WINDOW_MS = 350L
        private const val VOLUME_REPEAT_INTERVAL_MS = 100L
    }
}
