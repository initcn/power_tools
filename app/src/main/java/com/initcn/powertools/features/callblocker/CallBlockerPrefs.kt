package com.initcn.powertools.features.callblocker

import android.content.Context
import androidx.core.content.edit

object CallBlockerPrefs {
    private const val PREFS_NAME = "call_blocker_prefs"

    // Existing Filters
    private const val KEY_BLOCK_HIDDEN = "block_hidden_numbers"
    private const val KEY_BLOCK_UNSAVED = "block_unsaved_contacts"

    // New Behavior Settings
    private const val KEY_DISALLOW_CALL = "behavior_disallow_call"
    private const val KEY_REJECT_CALL = "behavior_reject_call"
    private const val KEY_SKIP_NOTIF = "behavior_skip_notif"
    private const val KEY_SILENCE_CALL = "behavior_silence_call"

    // --- Filters ---

    fun isBlockHiddenEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_BLOCK_HIDDEN, false)
    }

    fun setBlockHiddenEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_BLOCK_HIDDEN, enabled)
        }
    }

    fun isBlockUnsavedEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_BLOCK_UNSAVED, false)
    }

    fun setBlockUnsavedEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_BLOCK_UNSAVED, enabled)
        }
    }

    // --- Interception Behaviors ---

    fun isDisallowEnabled(context: Context): Boolean {
        // Defaulting to true so active blocking works by default
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DISALLOW_CALL, true)
    }

    fun setDisallowEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_DISALLOW_CALL, enabled)
        }
    }

    fun isRejectEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_REJECT_CALL, true)
    }

    fun setRejectEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_REJECT_CALL, enabled)
        }
    }

    fun isSkipNotifEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SKIP_NOTIF, true)
    }

    fun setSkipNotifEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_SKIP_NOTIF, enabled)
        }
    }

    fun isSilenceEnabled(context: Context): Boolean {
        // Defaulting to false, as it only applies when Disallow is false
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SILENCE_CALL, false)
    }

    fun setSilenceEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_SILENCE_CALL, enabled)
        }
    }
}