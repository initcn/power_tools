package com.initcn.powertools.core

import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.initcn.powertools.feature.dns.domain.CustomDnsProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPreferences @Inject constructor(
    private val prefs: SharedPreferences
) {
    private val gson = Gson()

    // ----------------------------------------------------
    // Custom DNS Providers
    // ----------------------------------------------------

    fun getCustomDnsProviders(): List<CustomDnsProvider> {
        val json = prefs.getString(KEY_CUSTOM_DNS_PROVIDERS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<CustomDnsProvider>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveCustomDnsProviders(providers: List<CustomDnsProvider>) {
        prefs.edit {
            putString(KEY_CUSTOM_DNS_PROVIDERS, gson.toJson(providers))
        }
    }

    fun addCustomDnsProviderIfMissing(provider: CustomDnsProvider): Boolean {
        val providers = getCustomDnsProviders()
        val exists = providers.any {
            it.hostname.equals(provider.hostname, ignoreCase = true)
        }

        if (exists) return false

        saveCustomDnsProviders(providers + provider)
        return true
    }

    fun removeCustomDnsProvider(providerId: Long) {
        val updated = getCustomDnsProviders().filterNot { it.id == providerId }
        saveCustomDnsProviders(updated)
    }

    fun updateCustomDnsProvider(provider: CustomDnsProvider) {
        val updated = getCustomDnsProviders().map {
            if (it.id == provider.id) provider else it
        }
        saveCustomDnsProviders(updated)
    }

    fun clearCustomDnsProviders() {
        prefs.edit { remove(KEY_CUSTOM_DNS_PROVIDERS) }
    }

    // ----------------------------------------------------
    // Last DNS Provider
    // ----------------------------------------------------

    fun setLastDnsProvider(provider: String) {
        prefs.edit { putString(KEY_LAST_DNS_PROVIDER, provider) }
    }

    fun getLastDnsProvider(): String? {
        return prefs.getString(KEY_LAST_DNS_PROVIDER, null)
    }

    // ----------------------------------------------------
    // Last Timeout
    // ----------------------------------------------------

    fun setLastTimeout(timeoutLabel: String) {
        prefs.edit { putString(KEY_LAST_TIMEOUT, timeoutLabel) }
    }

    fun getLastTimeout(): String? {
        return prefs.getString(KEY_LAST_TIMEOUT, null)
    }

    // Move the constants here to keep the linter happy!
    companion object {
        private const val KEY_CUSTOM_DNS_PROVIDERS = "custom_dns_providers"
        private const val KEY_LAST_DNS_PROVIDER = "last_dns_provider"
        private const val KEY_LAST_TIMEOUT = "last_timeout"
    }
}