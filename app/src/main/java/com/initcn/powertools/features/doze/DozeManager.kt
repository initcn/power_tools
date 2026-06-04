package com.initcn.powertools.features.doze

import android.content.Context
import android.provider.Settings

object DozeManager {

    private const val SCREEN_OFF_TIMEOUT =
        Settings.System.SCREEN_OFF_TIMEOUT

    data class TimeoutOption(
        val label: String,
        val value: Int
    )

    /**
     * Supported timeout values.
     */
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

    private val timeoutLookup by lazy {
        supportedTimeouts.associateBy { it.label }
    }

    /**
     * Returns current timeout in milliseconds.
     */
    fun getCurrentTimeout(
        context: Context
    ): Int {

        return try {

            Settings.System.getInt(
                context.contentResolver,
                SCREEN_OFF_TIMEOUT
            )

        } catch (_: Exception) {

            60_000
        }
    }

    /**
     * Returns matching label if known.
     */
    fun getCurrentLabel(
        context: Context
    ): String? {

        val timeout =
            getCurrentTimeout(context)

        return supportedTimeouts
            .firstOrNull {
                it.value == timeout
            }
            ?.label
    }

    /**
     * Returns matching option if known.
     */
    fun getCurrentOption(
        context: Context
    ): TimeoutOption? {

        val timeout =
            getCurrentTimeout(context)

        return supportedTimeouts
            .firstOrNull {
                it.value == timeout
            }
    }

    /**
     * Apply timeout using display label.
     */
    fun applyTimeout(
        context: Context,
        label: String
    ): Boolean {

        val option =
            timeoutLookup[label]
                ?: return false

        return applyTimeout(
            context,
            option.value
        )
    }

    /**
     * Apply timeout using milliseconds.
     */
    fun applyTimeout(
        context: Context,
        timeoutMs: Int
    ): Boolean {

        return try {

            Settings.System.putInt(
                context.contentResolver,
                SCREEN_OFF_TIMEOUT,
                timeoutMs
            )

        } catch (_: Exception) {

            false
        }
    }

    /**
     * Save current timeout before changes.
     */
    fun backupCurrentTimeout(
        context: Context
    ): Int {

        return getCurrentTimeout(
            context
        )
    }

    /**
     * Restore previous timeout.
     */
    fun restoreTimeout(
        context: Context,
        timeoutMs: Int
    ): Boolean {

        return applyTimeout(
            context,
            timeoutMs
        )
    }
}