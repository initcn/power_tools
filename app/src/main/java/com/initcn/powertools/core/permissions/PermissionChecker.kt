package com.initcn.powertools.core.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat

object PermissionChecker {

    fun hasWriteSecureSettings(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_SECURE_SETTINGS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun canWriteSystemSettings(context: Context): Boolean {
        return Settings.System.canWrite(context)
    }

    fun hasAllFilesAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    fun hasCallLogAccess(context: Context): Boolean {
        // FIXED: Using ContextCompat instead of checkCallingOrSelfPermission to prevent false positives
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasContactsAccess(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasPostNotifications(context: Context): Boolean {
        // NEW: Check for Android 13+ notification permissions
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Automatically granted on Android 12 and below
        }
    }


    fun getMissingPermissions(context: Context): List<RequiredPermission> {
        val missing = mutableListOf<RequiredPermission>()
        if (!canWriteSystemSettings(context)) missing += RequiredPermission.WRITE_SYSTEM_SETTINGS
        if (!hasAllFilesAccess()) missing += RequiredPermission.ALL_FILES_ACCESS
        return missing
    }

    fun getOptionalPermissions(context: Context): List<RequiredPermission> {
        val optional = mutableListOf<RequiredPermission>()
        if (!hasWriteSecureSettings(context)) optional += RequiredPermission.WRITE_SECURE_SETTINGS
        if (!hasCallLogAccess(context)) optional += RequiredPermission.READ_CALL_LOG
        if (!hasPostNotifications(context)) optional += RequiredPermission.POST_NOTIFICATIONS // NEW: Added to list
        return optional
    }
}