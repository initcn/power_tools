package com.initcn.powertools.features.dns

import android.content.Context
import android.provider.Settings
import com.initcn.powertools.model.DnsProvider

object DnsManager {

    private const val PRIVATE_DNS_MODE =
        "private_dns_mode"

    private const val PRIVATE_DNS_SPECIFIER =
        "private_dns_specifier"

    private val knownProviders by lazy {
        DnsProvider.entries
            .filter {
                !it.hostname.isNullOrBlank()
            }
            .associateBy {
                it.hostname
            }
    }

    /**
     * Returns currently configured provider.
     */
    fun getCurrentProvider(
        context: Context
    ): DnsProvider {

        return try {

            val resolver =
                context.contentResolver

            val mode =
                Settings.Global.getString(
                    resolver,
                    PRIVATE_DNS_MODE
                )

            val hostname =
                Settings.Global.getString(
                    resolver,
                    PRIVATE_DNS_SPECIFIER
                )

            when {

                mode == "off" ->
                    DnsProvider.OFF

                mode == "opportunistic" ->
                    DnsProvider.AUTOMATIC

                mode == "hostname" &&
                        !hostname.isNullOrBlank() -> {

                    knownProviders[hostname]
                        ?: DnsProvider.CUSTOM
                }

                else ->
                    DnsProvider.AUTOMATIC
            }

        } catch (_: Exception) {

            DnsProvider.AUTOMATIC
        }
    }

    /**
     * Returns custom hostname if active.
     */
    fun getCustomHostname(
        context: Context
    ): String? {

        return try {

            Settings.Global.getString(
                context.contentResolver,
                PRIVATE_DNS_SPECIFIER
            )

        } catch (_: Exception) {

            null
        }
    }

    /**
     * Apply DNS provider.
     *
     * Requires:
     * adb shell pm grant com.initcn.powertools android.permission.WRITE_SECURE_SETTINGS
     */
    fun apply(
        context: Context,
        provider: DnsProvider,
        customHostname: String? = null
    ): Boolean {

        return try {

            val resolver =
                context.contentResolver

            when (provider) {

                DnsProvider.OFF -> {

                    Settings.Global.putString(
                        resolver,
                        PRIVATE_DNS_MODE,
                        "off"
                    )

                    Settings.Global.putString(
                        resolver,
                        PRIVATE_DNS_SPECIFIER,
                        null
                    )
                }

                DnsProvider.AUTOMATIC -> {

                    Settings.Global.putString(
                        resolver,
                        PRIVATE_DNS_MODE,
                        "opportunistic"
                    )

                    Settings.Global.putString(
                        resolver,
                        PRIVATE_DNS_SPECIFIER,
                        null
                    )
                }

                DnsProvider.CUSTOM -> {

                    val hostname =
                        customHostname
                            ?.trim()
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?: return false

                    Settings.Global.putString(
                        resolver,
                        PRIVATE_DNS_MODE,
                        "hostname"
                    )

                    Settings.Global.putString(
                        resolver,
                        PRIVATE_DNS_SPECIFIER,
                        hostname
                    )
                }

                else -> {

                    Settings.Global.putString(
                        resolver,
                        PRIVATE_DNS_MODE,
                        "hostname"
                    )

                    Settings.Global.putString(
                        resolver,
                        PRIVATE_DNS_SPECIFIER,
                        provider.hostname
                    )
                }
            }

            true

        } catch (_: Exception) {

            false
        }
    }

    /**
     * Disable Private DNS.
     */
    fun disable(
        context: Context
    ): Boolean {

        return apply(
            context = context,
            provider = DnsProvider.OFF
        )
    }

    /**
     * Enable Automatic Private DNS.
     */
    fun automatic(
        context: Context
    ): Boolean {

        return apply(
            context = context,
            provider = DnsProvider.AUTOMATIC
        )
    }
}