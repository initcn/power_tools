package com.initcn.powertools.feature.dns.presentation

import androidx.lifecycle.ViewModel
import com.initcn.powertools.core.AppPreferences
import com.initcn.powertools.feature.dns.domain.CustomDnsProvider
import com.initcn.powertools.feature.dns.domain.DnsManager
import com.initcn.powertools.feature.dns.domain.DnsProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class DnsViewModel @Inject constructor(
    private val dnsManager: DnsManager,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(DnsUiState())
    val uiState: StateFlow<DnsUiState> = _uiState.asStateFlow()

    init {
        loadInitialState()
    }

    private fun loadInitialState() {
        val currentProvider = dnsManager.getCurrentProvider()
        val customHostname = if (currentProvider == DnsProvider.CUSTOM) {
            dnsManager.getCustomHostname() ?: ""
        } else ""

        _uiState.update { state ->
            state.copy(
                selectedProvider = currentProvider,
                customHostname = customHostname,
                savedProviders = appPreferences.getCustomDnsProviders()
            )
        }
    }

    fun onEvent(event: DnsEvent) {
        when (event) {
            is DnsEvent.SelectProvider -> _uiState.update { it.copy(selectedProvider = event.provider) }
            is DnsEvent.UpdateCustomHostname -> _uiState.update { it.copy(customHostname = event.hostname) }
            is DnsEvent.SelectSavedProvider -> {
                _uiState.update {
                    it.copy(
                        customHostname = event.provider.hostname,
                        selectedProvider = DnsProvider.CUSTOM
                    )
                }
            }
            is DnsEvent.DeleteSavedProvider -> {
                appPreferences.removeCustomDnsProvider(event.providerId)
                _uiState.update { it.copy(savedProviders = appPreferences.getCustomDnsProviders()) }
            }
            is DnsEvent.ClearStatusMessage -> _uiState.update { it.copy(statusMessage = null) }
            is DnsEvent.ApplyDns -> applyDns(event.successMessageTemplate, event.failureMessage)
        }
    }

    private fun applyDns(successMessageTemplate: String, failureMessage: String) {
        val hostname = _uiState.value.customHostname.trim()
        val provider = _uiState.value.selectedProvider

        val success = dnsManager.apply(
            provider = provider,
            customHostname = hostname.takeIf { it.isNotBlank() }
        )

        if (success) {
            // Save custom provider if it's new
            if (provider == DnsProvider.CUSTOM && hostname.isNotBlank()) {
                appPreferences.addCustomDnsProviderIfMissing(
                    CustomDnsProvider(name = hostname, hostname = hostname)
                )
                _uiState.update { it.copy(savedProviders = appPreferences.getCustomDnsProviders()) }
            }
            _uiState.update { it.copy(statusMessage = successMessageTemplate.format(provider.title)) }
        } else {
            _uiState.update { it.copy(statusMessage = failureMessage) }
        }
    }
}