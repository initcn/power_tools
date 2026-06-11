package com.initcn.powertools.feature.doze

import android.content.Context
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DozeManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    companion object {
        private const val SCREEN_OFF_TIMEOUT = Settings.System.SCREEN_OFF_TIMEOUT
    }

    data class TimeoutOption(
        val label: String,
        val value: Int
    )

    val supportedTimeouts = listOf(
        TimeoutOption("15 sec", 15_000),
        TimeoutOption("30 sec", 30_000),
        TimeoutOption("1 min", 60_000),
        TimeoutOption("2 min", 120_000),
        TimeoutOption("5 min", 300_000),
        TimeoutOption("10 min", 600_000),
        TimeoutOption("15 min", 900_000),
        TimeoutOption("30 min", 1_800_000),
        TimeoutOption("1 hr", 3_600_000)
    )

    // Eagerly initialized mapping, ready instantly
    private val timeoutLookup = supportedTimeouts.associateBy { it.label }

    fun getCurrentTimeout(): Int {
        return try {
            Settings.System.getInt(context.contentResolver, SCREEN_OFF_TIMEOUT)
        } catch (_: Exception) {
            60_000
        }
    }

    fun getCurrentLabel(): String? {
        val timeout = getCurrentTimeout()
        return supportedTimeouts.firstOrNull { it.value == timeout }?.label
    }

    fun getCurrentOption(): TimeoutOption? {
        val timeout = getCurrentTimeout()
        return supportedTimeouts.firstOrNull { it.value == timeout }
    }

    fun applyTimeout(label: String): Boolean {
        val option = timeoutLookup[label] ?: return false
        return applyTimeout(option.value)
    }

    fun applyTimeout(timeoutMs: Int): Boolean {
        return try {
            Settings.System.putInt(context.contentResolver, SCREEN_OFF_TIMEOUT, timeoutMs)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun backupCurrentTimeout(): Int {
        return getCurrentTimeout()
    }

    fun restoreTimeout(timeoutMs: Int): Boolean {
        return applyTimeout(timeoutMs)
    }
}