package com.initcn.powertools.feature.dns.domain

import android.content.Context
import android.provider.Settings
import com.initcn.powertools.core.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DnsManager @Inject constructor(
    @param:ApplicationContext private val context: Context, // FIXED: Added @param:
    private val appPreferences: AppPreferences
) {

    // FIXED: Moved to companion object to satisfy the linter's naming rules
    companion object {
        private const val PRIVATE_DNS_MODE = "private_dns_mode"
        private const val PRIVATE_DNS_SPECIFIER = "private_dns_specifier"
    }

    private val knownProviders = DnsProvider.entries
        .filter { !it.hostname.isNullOrBlank() }
        .associateBy { it.hostname }

    fun getCurrentProvider(): DnsProvider {
        return try {
            val resolver = context.contentResolver
            val mode = Settings.Global.getString(resolver, PRIVATE_DNS_MODE)
            val hostname = Settings.Global.getString(resolver, PRIVATE_DNS_SPECIFIER)

            // FIXED: Introduced 'mode' as the subject of 'when' for cleaner code
            when (mode) {
                "off" -> DnsProvider.OFF
                "opportunistic" -> DnsProvider.AUTOMATIC
                "hostname" -> {
                    if (!hostname.isNullOrBlank()) {
                        knownProviders[hostname] ?: DnsProvider.CUSTOM
                    } else {
                        DnsProvider.AUTOMATIC
                    }
                }

                else -> DnsProvider.AUTOMATIC
            }
        } catch (_: Exception) {
            DnsProvider.AUTOMATIC
        }
    }

    fun getCustomHostname(): String? {
        return try {
            Settings.Global.getString(context.contentResolver, PRIVATE_DNS_SPECIFIER)
        } catch (_: Exception) {
            null
        }
    }

    fun apply(provider: DnsProvider, customHostname: String? = null): Boolean {
        return try {
            val resolver = context.contentResolver

            when (provider) {
                DnsProvider.OFF -> {
                    Settings.Global.putString(resolver, PRIVATE_DNS_MODE, "off")
                    Settings.Global.putString(resolver, PRIVATE_DNS_SPECIFIER, null)
                }

                DnsProvider.AUTOMATIC -> {
                    Settings.Global.putString(resolver, PRIVATE_DNS_MODE, "opportunistic")
                    Settings.Global.putString(resolver, PRIVATE_DNS_SPECIFIER, null)
                }

                DnsProvider.CUSTOM -> {
                    val hostname =
                        customHostname?.trim()?.takeIf { it.isNotBlank() } ?: return false
                    Settings.Global.putString(resolver, PRIVATE_DNS_MODE, "hostname")
                    Settings.Global.putString(resolver, PRIVATE_DNS_SPECIFIER, hostname)
                    appPreferences.setLastDnsProvider(DnsProvider.CUSTOM.name)
                }

                else -> {
                    Settings.Global.putString(resolver, PRIVATE_DNS_MODE, "hostname")
                    Settings.Global.putString(resolver, PRIVATE_DNS_SPECIFIER, provider.hostname)
                    appPreferences.setLastDnsProvider(provider.name)
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun disable(): Boolean = apply(provider = DnsProvider.OFF)

    fun automatic(): Boolean = apply(provider = DnsProvider.AUTOMATIC)
}