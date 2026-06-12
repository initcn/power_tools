package com.initcn.powertools.core.permissions

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings

@SuppressLint("InlinedApi")
enum class RequiredPermission(
    val title: String,
    val description: String,
    val manifestString: String? = null,
    val settingsAction: String? = null,
    val adbCommand: String? = null
) {
    WRITE_SECURE_SETTINGS(
        title = "Write Secure Settings",
        description = "Required for DNS switching and advanced system controls. This must be granted via a computer.",
        adbCommand = "adb shell pm grant com.initcn.powertools android.permission.WRITE_SECURE_SETTINGS"
    ),
    WRITE_SYSTEM_SETTINGS(
        title = "Modify System Settings",
        description = "Required to alter the device screen timeout duration.",
        settingsAction = Settings.ACTION_MANAGE_WRITE_SETTINGS
    ),
    ALL_FILES_ACCESS(
        title = "All Files Access",
        description = "Required for Downloads Organizer to manage files in the global Downloads folder.",
        settingsAction = Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
    ),
    READ_CALL_LOG(
        title = "Call Log Access",
        description = "Required to display recent calls so you can quickly add them to your blocklist or whitelist.",
        manifestString = Manifest.permission.READ_CALL_LOG
    ),
    READ_CONTACTS(
        title = "Contacts Access",
        description = "Required to identify and block unsaved numbers.",
        manifestString = Manifest.permission.READ_CONTACTS
    ),
    POST_NOTIFICATIONS(
        title = "Notifications",
        description = "Required to send alerts and background service statuses.",
        manifestString = Manifest.permission.POST_NOTIFICATIONS
    ),
    ACCESS_NOTIFICATION_POLICY(
        title = "Do Not Disturb Access",
        description = "Required to switch the phone into Silence or DND when flipped.",
        settingsAction = Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
    );

    fun checkIsGranted(context: Context): Boolean {
        return when (this) {
            WRITE_SECURE_SETTINGS -> PermissionChecker.hasWriteSecureSettings(context)
            WRITE_SYSTEM_SETTINGS -> PermissionChecker.canWriteSystemSettings(context)
            ALL_FILES_ACCESS -> PermissionChecker.hasAllFilesAccess()
            READ_CALL_LOG -> PermissionChecker.hasCallLogAccess(context)
            READ_CONTACTS -> PermissionChecker.hasContactsAccess(context)
            POST_NOTIFICATIONS -> PermissionChecker.hasPostNotifications(context)
            ACCESS_NOTIFICATION_POLICY -> PermissionChecker.hasNotificationPolicyAccess(context)
        }
    }
}