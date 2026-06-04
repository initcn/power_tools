package com.initcn.powertools.permissions

/**
 * Represents the current permission status
 * required by PowerTools.
 */
sealed interface PermissionState {

    /**
     * All required permissions granted.
     */
    data object Granted : PermissionState

    /**
     * One or more permissions are missing.
     */
    data class Missing(
        val missingPermissions: List<RequiredPermission>
    ) : PermissionState
}

/**
 * Permissions/features that PowerTools
 * may require.
 */
enum class RequiredPermission(
    val title: String,
    val description: String,
    val adbCommand: String? = null
) {

    WRITE_SECURE_SETTINGS(
        title = "Write Secure Settings",
        description = "Required for DNS switching and advanced system controls.",
        adbCommand = "adb shell pm grant com.initcn.powertools android.permission.WRITE_SECURE_SETTINGS"
    ),
    WRITE_SYSTEM_SETTINGS(
        title = "Modify System Settings",
        description = "Required for certain device-level controls."
    ),
    ALL_FILES_ACCESS(
        title = "All Files Access",
        description = "Required for Downloads Organizer to manage files in the Downloads folder."
    )
}