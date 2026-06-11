package com.initcn.powertools.core.utils

object AppInfo {

    const val APP_NAME = "PowerTools"

    const val PACKAGE_NAME = "com.initcn.powertools"

    const val VERSION_NAME = "1.0.0"

    const val VERSION_CODE = 1

    /**
     * ADB command required for advanced features.
     */
    val WRITE_SECURE_SETTINGS_COMMAND: String
        get() = "adb shell pm grant $PACKAGE_NAME android.permission.WRITE_SECURE_SETTINGS"

    /**
     * GitHub URL (future)
     */
    const val SOURCE_URL = ""

    /**
     * Support URL (future)
     */
    const val SUPPORT_URL = ""

    /**
     * Features available in MVP.
     */
    val FEATURES = listOf(
        "Screen Doze",
        "DNS Switcher",
        "Downloads Organizer"
    )
}