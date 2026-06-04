package com.initcn.powertools.permissions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.net.toUri

object AllFilesAccessManager {

    /**
     * Android 11+ Full Files Access
     */
    fun hasAccess(): Boolean {

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
     * Open system page where user grants
     * "Allow access to manage all files".
     */
    fun requestAccess(
        context: Context
    ) {

        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.R
        ) {
            return
        }

        try {

            val intent = Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                "package:${context.packageName}".toUri()
            )

            intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
            )

            context.startActivity(intent)

        } catch (_: Exception) {

            val intent = Intent(
                Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
            )

            intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
            )

            context.startActivity(intent)
        }
    }
}