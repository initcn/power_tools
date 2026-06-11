package com.initcn.powertools.data

import android.content.Context
import androidx.core.content.edit // FIXED: Added KTX shorthand extension import
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.initcn.powertools.model.CustomDnsProvider

object AppPreferences {

    private const val PREFS_NAME = "powertools_preferences"
    private const val KEY_CUSTOM_DNS_PROVIDERS = "custom_dns_providers"
    private const val KEY_LAST_DNS_PROVIDER = "last_dns_provider"
    private const val KEY_LAST_TIMEOUT = "last_timeout"

    private val gson = Gson()

    private fun prefs(context: Context) = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    // ----------------------------------------------------
    // Custom DNS Providers
    // ----------------------------------------------------

    fun getCustomDnsProviders(context: Context): List<CustomDnsProvider> {
        val json = prefs(context).getString(KEY_CUSTOM_DNS_PROVIDERS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<CustomDnsProvider>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveCustomDnsProviders(context: Context, providers: List<CustomDnsProvider>) {
        // FIXED: Utilized KTX SharedPreferences lambda context block
        prefs(context).edit {
            putString(KEY_CUSTOM_DNS_PROVIDERS, gson.toJson(providers))
        }
    }

    fun addCustomDnsProviderIfMissing(context: Context, provider: CustomDnsProvider): Boolean {
        val providers = getCustomDnsProviders(context)
        val exists = providers.any {
            it.hostname.equals(provider.hostname, ignoreCase = true)
        }

        if (exists) {
            return false
        }

        saveCustomDnsProviders(context, providers + provider)
        return true
    }

    fun removeCustomDnsProvider(context: Context, providerId: Long) {
        val updated = getCustomDnsProviders(context).filterNot {
            it.id == providerId
        }
        saveCustomDnsProviders(context, updated)
    }

    fun updateCustomDnsProvider(context: Context, provider: CustomDnsProvider) {
        val updated = getCustomDnsProviders(context).map {
            if (it.id == provider.id) provider else it
        }
        saveCustomDnsProviders(context, updated)
    }

    fun clearCustomDnsProviders(context: Context) {
        prefs(context).edit {
            remove(KEY_CUSTOM_DNS_PROVIDERS)
        }
    }

    // ----------------------------------------------------
    // Last DNS Provider
    // ----------------------------------------------------

    fun setLastDnsProvider(context: Context, provider: String) {
        prefs(context).edit {
            putString(KEY_LAST_DNS_PROVIDER, provider)
        }
    }

    fun getLastDnsProvider(context: Context): String? {
        return prefs(context).getString(KEY_LAST_DNS_PROVIDER, null)
    }

    // ----------------------------------------------------
    // Last Timeout
    // ----------------------------------------------------

    fun setLastTimeout(context: Context, timeoutLabel: String) {
        prefs(context).edit {
            putString(KEY_LAST_TIMEOUT, timeoutLabel)
        }
    }

    fun getLastTimeout(context: Context): String? {
        return prefs(context).getString(KEY_LAST_TIMEOUT, null)
    }
}