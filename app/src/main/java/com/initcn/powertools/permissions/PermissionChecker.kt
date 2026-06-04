package com.initcn.powertools.permissions

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.Manifest

object PermissionChecker {

    /**
     * DNS feature.
     *
     * Optional permission granted via ADB:
     *
     * adb shell pm grant
     * com.initcn.powertools
     * android.permission.WRITE_SECURE_SETTINGS
     */
    fun hasWriteSecureSettings(
        context: Context
    ): Boolean {

        return context.checkCallingOrSelfPermission(
            Manifest.permission.WRITE_SECURE_SETTINGS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Screen timeout feature.
     */
    fun canWriteSystemSettings(
        context: Context
    ): Boolean {

        return Settings.System.canWrite(
            context
        )
    }

    /**
     * Downloads organizer feature.
     */
    fun hasAllFilesAccess(): Boolean {

        return if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.R
        ) {

            Environment.isExternalStorageManager()

        } else {

            true
        }
    }

    /**
     * Required permissions only.
     *
     * WRITE_SECURE_SETTINGS is intentionally
     * excluded because DNS support is optional
     * and can be granted later via ADB.
     */
    fun getMissingPermissions(
        context: Context
    ): List<RequiredPermission> {

        val missing =
            mutableListOf<RequiredPermission>()

        if (
            !canWriteSystemSettings(
                context
            )
        ) {

            missing +=
                RequiredPermission.WRITE_SYSTEM_SETTINGS
        }

        if (
            !hasAllFilesAccess()
        ) {

            missing +=
                RequiredPermission.ALL_FILES_ACCESS
        }

        return missing
    }

    /**
     * Optional permissions.
     */
    fun getOptionalPermissions(
        context: Context
    ): List<RequiredPermission> {

        val optional =
            mutableListOf<RequiredPermission>()

        if (
            !hasWriteSecureSettings(
                context
            )
        ) {

            optional +=
                RequiredPermission.WRITE_SECURE_SETTINGS
        }

        return optional
    }
}