package com.initcn.powertools.feature.dns.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.initcn.powertools.R
import com.initcn.powertools.core.permissions.RequiredPermission
import com.initcn.powertools.core.theme.Dimens
import com.initcn.powertools.core.ui.components.FeaturePermissionGuard
import com.initcn.powertools.core.ui.components.PowerToolScaffold
import com.initcn.powertools.core.ui.components.StatusMessage
import com.initcn.powertools.feature.dns.domain.DnsProvider

@Composable
fun DnsRoute(
    onNavigateBack: () -> Unit,
    viewModel: DnsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    FeaturePermissionGuard(
        requiredPermissions = listOf(RequiredPermission.WRITE_SECURE_SETTINGS),
        onNavigateBack = onNavigateBack
    ) {
        DnsScreen(
            state = state,
            onEvent = viewModel::onEvent
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnsScreen(
    state: DnsUiState,
    onEvent: (DnsEvent) -> Unit
) {
    PowerToolScaffold(
        title = stringResource(R.string.dns_switcher)
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Dimens.ScreenPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimens.MD)
        ) {

            Text(
                text = stringResource(R.string.dns_description),
                style = MaterialTheme.typography.bodyLarge
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column {
                    DnsProvider.entries.forEach { provider ->
                        ListItem(
                            headlineContent = { Text(text = provider.title) },
                            supportingContent = {
                                when {
                                    provider.hostname != null -> Text(text = provider.hostname)
                                    provider == DnsProvider.OFF -> Text(text = stringResource(R.string.disable_private_dns))
                                    provider == DnsProvider.AUTOMATIC -> Text(text = stringResource(R.string.automatic_dns_mode))
                                }
                            },
                            leadingContent = {
                                RadioButton(
                                    selected = state.selectedProvider == provider,
                                    onClick = {
                                        onEvent(DnsEvent.ClearStatusMessage)
                                        onEvent(DnsEvent.SelectProvider(provider))
                                    }
                                )
                            },
                            modifier = Modifier.clickable {
                                onEvent(DnsEvent.ClearStatusMessage)
                                onEvent(DnsEvent.SelectProvider(provider))
                            }
                        )
                    }
                }
            }

            if (state.selectedProvider == DnsProvider.CUSTOM) {
                OutlinedTextField(
                    value = state.customHostname,
                    onValueChange = { onEvent(DnsEvent.UpdateCustomHostname(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.custom_dns_hostname)) },
                    placeholder = { Text(stringResource(R.string.custom_dns_placeholder)) },
                    singleLine = true
                )

                if (state.savedProviders.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.saved_dns_providers),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            state.savedProviders.forEach { provider ->
                                ListItem(
                                    headlineContent = { Text(provider.name) },
                                    supportingContent = { Text(provider.hostname) },
                                    trailingContent = {
                                        IconButton(
                                            onClick = { onEvent(DnsEvent.DeleteSavedProvider(provider.id)) }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = stringResource(R.string.delete)
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onEvent(DnsEvent.ClearStatusMessage)
                                            onEvent(DnsEvent.SelectSavedProvider(provider))
                                        }
                                )
                            }
                        }
                    }
                }
            }

            // Unpack the UiText back to a String securely in the composable
            StatusMessage(message = state.statusMessage?.asString())

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onEvent(DnsEvent.ApplyDns) }
            ) {
                Text(text = stringResource(R.string.apply_dns))
            }
        }
    }
}