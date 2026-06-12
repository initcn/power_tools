package com.initcn.powertools.feature.callblocker.data

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallBlockerPrefs @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "call_blocker_prefs"

        // Filters
        private const val KEY_BLOCK_HIDDEN = "block_hidden_numbers"
        private const val KEY_BLOCK_UNSAVED = "block_unsaved_contacts"

        // Behaviors
        private const val KEY_DISALLOW_CALL = "behavior_disallow_call"
        private const val KEY_REJECT_CALL = "behavior_reject_call"
        private const val KEY_SKIP_NOTIF = "behavior_skip_notif"
        private const val KEY_SILENCE_CALL = "behavior_silence_call"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Filters

    fun isBlockHiddenEnabled(): Boolean = prefs.getBoolean(KEY_BLOCK_HIDDEN, false)

    fun setBlockHiddenEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_BLOCK_HIDDEN, enabled) }
    }

    fun isBlockUnsavedEnabled(): Boolean = prefs.getBoolean(KEY_BLOCK_UNSAVED, false)

    fun setBlockUnsavedEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_BLOCK_UNSAVED, enabled) }
    }

    // Interception Behaviors

    fun isDisallowEnabled(): Boolean = prefs.getBoolean(KEY_DISALLOW_CALL, true)

    fun setDisallowEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_DISALLOW_CALL, enabled) }
    }

    fun isRejectEnabled(): Boolean = prefs.getBoolean(KEY_REJECT_CALL, true)

    fun setRejectEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_REJECT_CALL, enabled) }
    }

    fun isSkipNotifEnabled(): Boolean = prefs.getBoolean(KEY_SKIP_NOTIF, true)

    fun setSkipNotifEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_SKIP_NOTIF, enabled) }
    }

    fun isSilenceEnabled(): Boolean = prefs.getBoolean(KEY_SILENCE_CALL, false)

    fun setSilenceEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_SILENCE_CALL, enabled) }
    }
}