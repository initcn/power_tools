package com.initcn.powertools.core.permissions

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.edit
import androidx.core.net.toUri

object StorageAccessManager {
    private const val PREFS_NAME = "storage_access_prefs"
    const val KEY_DOCUMENTS_URI = "documents_uri"

    fun getPersistedUri(context: Context, key: String): Uri? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uriString = prefs.getString(key, null) ?: return null
        val uri = uriString.toUri()
        val persistedUriPermissions = context.contentResolver.persistedUriPermissions
        val hasPermission = persistedUriPermissions.any { it.uri == uri }

        return if (hasPermission) uri else null
    }

    fun savePersistedUri(context: Context, key: String, uri: Uri) {
        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(uri, takeFlags)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(key, uri.toString()) }
    }
}