package com.initcn.powertools.core.permissions

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
    ),
    READ_CALL_LOG(
        title = "Call Log Access",
        description = "Required to display recent calls so you can quickly add them to your blocklist or whitelist."
    ),
    POST_NOTIFICATIONS(
        title = "Notifications",
        description = "Required to send alerts and background service statuses."
    )
}