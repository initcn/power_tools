package com.initcn.powertools.feature.colorfilter.domain

import android.content.Context
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ColorFilterManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val resolver = context.contentResolver

    companion object {
        private const val DALTONIZER_ENABLED = "accessibility_display_daltonizer_enabled"
        private const val DALTONIZER_MODE = "accessibility_display_daltonizer"
    }

    /**
     * Checks the current system settings to determine which color mode is active.
     */
    fun getCurrentMode(): ColorMode {
        return try {
            val isEnabled = Settings.Secure.getInt(resolver, DALTONIZER_ENABLED, 0) == 1
            if (!isEnabled) {
                ColorMode.OFF
            } else {
                val modeVal = Settings.Secure.getInt(resolver, DALTONIZER_MODE, -1)
                ColorMode.fromValue(modeVal)
            }
        } catch (_: Exception) {
            ColorMode.OFF
        }
    }

    /**
     * Updates the device's Secure Settings with the selected color matrix configuration.
     * Throws SecurityException if WRITE_SECURE_SETTINGS permission hasn't been granted.
     */
    fun applyMode(mode: ColorMode): Boolean {
        return try {
            if (mode == ColorMode.OFF) {
                Settings.Secure.putInt(resolver, DALTONIZER_ENABLED, 0)
            } else {
                // Secure order execution: Set mode value first, then enable the utility toggle
                Settings.Secure.putInt(resolver, DALTONIZER_MODE, mode.value)
                Settings.Secure.putInt(resolver, DALTONIZER_ENABLED, 1)
            }
            true
        } catch (e: SecurityException) {
            false
        }
    }
}