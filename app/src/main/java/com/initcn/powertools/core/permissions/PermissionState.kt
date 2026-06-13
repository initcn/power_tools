package com.initcn.powertools.core.permissions

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import androidx.annotation.StringRes
import com.initcn.powertools.R

@SuppressLint("InlinedApi")
enum class RequiredPermission(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val manifestString: String? = null,
    val settingsAction: String? = null,
    val adbCommand: String? = null
) {
    WRITE_SECURE_SETTINGS(
        titleRes = R.string.perm_secure_title,
        descriptionRes = R.string.perm_secure_desc,
        adbCommand = "adb shell pm grant com.initcn.powertools android.permission.WRITE_SECURE_SETTINGS"
    ),
    WRITE_SYSTEM_SETTINGS(
        titleRes = R.string.perm_sys_title,
        descriptionRes = R.string.perm_sys_desc,
        settingsAction = Settings.ACTION_MANAGE_WRITE_SETTINGS
    ),
    ALL_FILES_ACCESS(
        titleRes = R.string.perm_files_title,
        descriptionRes = R.string.perm_files_desc,
        settingsAction = Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
    ),
    READ_CALL_LOG(
        titleRes = R.string.perm_calllog_title,
        descriptionRes = R.string.perm_calllog_desc,
        manifestString = Manifest.permission.READ_CALL_LOG
    ),
    READ_CONTACTS(
        titleRes = R.string.perm_contacts_title,
        descriptionRes = R.string.perm_contacts_desc,
        manifestString = Manifest.permission.READ_CONTACTS
    ),
    POST_NOTIFICATIONS(
        titleRes = R.string.perm_notif_title,
        descriptionRes = R.string.perm_notif_desc,
        manifestString = Manifest.permission.POST_NOTIFICATIONS
    ),
    ACCESS_NOTIFICATION_POLICY(
        titleRes = R.string.perm_dnd_title,
        descriptionRes = R.string.perm_dnd_desc,
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