package com.initcn.powertools.feature.flipaction.data

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit
import com.initcn.powertools.feature.flipaction.domain.FlipActionManager

@Singleton
class FlipActionPrefs @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("flip_action_prefs", Context.MODE_PRIVATE)

    // --- ARCHITECTURE UPGRADE: IN-MEMORY CACHING ---
    // By holding these in variables, the SensorManager can read them
    // instantly without triggering synchronized map lookups in SharedPreferences.

    private var _isFeatureEnabledCache = prefs.getBoolean("enabled", false)
    private var _selectedModeCache = FlipActionManager.FlipMode.valueOf(
        prefs.getString("selected_mode", FlipActionManager.FlipMode.SILENCE.name) ?: FlipActionManager.FlipMode.SILENCE.name
    )
    private var _didAppMakeChangeCache = prefs.getBoolean("did_make_change", false)

    // Flow for the UI to observe
    private val _featureEnabled = MutableStateFlow(_isFeatureEnabledCache)

    // --- READ/WRITE METHODS ---

    fun isFeatureEnabled(): Boolean = _isFeatureEnabledCache

    fun setFeatureEnabled(enabled: Boolean) {
        _isFeatureEnabledCache = enabled // Update cache instantly
        prefs.edit { putBoolean("enabled", enabled) } // Async disk write
        _featureEnabled.value = enabled
    }

    fun getSelectedMode(): FlipActionManager.FlipMode = _selectedModeCache

    fun setSelectedMode(mode: FlipActionManager.FlipMode) {
        _selectedModeCache = mode
        prefs.edit { putString("selected_mode", mode.name) }
    }

    // --- STATE PERSISTENCE ---

    fun didAppMakeChange(): Boolean = _didAppMakeChangeCache

    fun setDidAppMakeChange(madeChange: Boolean) {
        _didAppMakeChangeCache = madeChange
        prefs.edit { putBoolean("did_make_change", madeChange) }
    }

    // These are only called once during the flip action, so caching is optional,
    // but they remain fast and efficient.
    fun savePreviousRingerMode(mode: Int) {
        prefs.edit { putInt("prev_ringer", mode) }
    }

    fun getPreviousRingerMode(): Int = prefs.getInt("prev_ringer", -1)

    fun savePreviousDndState(filter: Int) {
        prefs.edit { putInt("prev_dnd", filter) }
    }

    fun getPreviousDndState(): Int = prefs.getInt("prev_dnd", -1)
}