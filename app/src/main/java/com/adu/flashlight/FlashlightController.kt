package com.adu.flashlight

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object FlashlightController {
    private val mutableIsOn = MutableStateFlow(false)
    private val mutableError = MutableStateFlow<String?>(null)
    val isOn: StateFlow<Boolean> = mutableIsOn.asStateFlow()
    val error: StateFlow<String?> = mutableError.asStateFlow()

    @Volatile
    private var appContext: Context? = null
    @Volatile
    private var cameraManager: CameraManager? = null
    @Volatile
    private var cameraId: String? = null

    @Synchronized
    fun initialize(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        val manager = context.getSystemService(CameraManager::class.java)
        cameraManager = manager
        try {
            cameraId = manager.cameraIdList.firstOrNull { id ->
                manager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (exception: CameraAccessException) {
            mutableError.value = exception.message ?: "Could not access the device camera."
        }
        manager.registerTorchCallback(object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(id: String, enabled: Boolean) {
                if (id == cameraId) mutableIsOn.value = enabled
            }

            override fun onTorchModeUnavailable(id: String) {
                if (id == cameraId) {
                    mutableIsOn.value = false
                    mutableError.value = "The flashlight is currently unavailable."
                }
            }
        }, Handler(Looper.getMainLooper()))
        if (cameraId == null) mutableError.value = "This device does not have a flashlight."
    }

    fun toggle(): Boolean = setEnabled(!mutableIsOn.value)

    @Synchronized
    fun setEnabled(enabled: Boolean): Boolean {
        val context = appContext
        val manager = cameraManager
        val id = cameraId
        if (context == null || manager == null || id == null) {
            mutableError.value = mutableError.value ?: "This device does not have a flashlight."
            return false
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            mutableError.value = "Allow camera access in the app before using the shortcut."
            return false
        }
        return try {
            manager.setTorchMode(id, enabled)
            mutableIsOn.value = enabled
            mutableError.value = null
            true
        } catch (exception: CameraAccessException) {
            mutableError.value = exception.message ?: "Could not change the flashlight state."
            false
        } catch (exception: IllegalArgumentException) {
            mutableError.value = exception.message ?: "Could not change the flashlight state."
            false
        } catch (exception: SecurityException) {
            mutableError.value = exception.message ?: "Could not change the flashlight state."
            false
        }
    }

    fun setError(message: String) {
        mutableError.value = message
    }

    fun clearError() {
        mutableError.value = null
    }
}
