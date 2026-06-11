package com.initcn.powertools.feature.dns.presentation

import com.initcn.powertools.feature.dns.domain.CustomDnsProvider
import com.initcn.powertools.feature.dns.domain.DnsProvider

data class DnsUiState(
    val selectedProvider: DnsProvider = DnsProvider.AUTOMATIC,
    val customHostname: String = "",
    val savedProviders: List<CustomDnsProvider> = emptyList(),
    val statusMessage: String? = null
)