package com.initcn.powertools.feature.dns.presentation

import com.initcn.powertools.feature.dns.domain.CustomDnsProvider
import com.initcn.powertools.feature.dns.domain.DnsProvider

sealed interface DnsEvent {
    data class SelectProvider(val provider: DnsProvider) : DnsEvent
    data class UpdateCustomHostname(val hostname: String) : DnsEvent
    data class SelectSavedProvider(val provider: CustomDnsProvider) : DnsEvent
    data class DeleteSavedProvider(val providerId: Long) : DnsEvent

    data object ClearStatusMessage : DnsEvent
    data object ApplyDns : DnsEvent
}