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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.initcn.powertools.R
import com.initcn.powertools.core.permissions.PermissionChecker
import com.initcn.powertools.core.theme.Dimens
import com.initcn.powertools.core.ui.components.PowerToolScaffold
import com.initcn.powertools.core.ui.components.StatusMessage
import com.initcn.powertools.feature.dns.domain.DnsProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnsScreen(
    viewModel: DnsViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val hasWriteSecureSettings = remember {
        PermissionChecker.hasWriteSecureSettings(context)
    }

    // Single source of truth
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val dnsApplyFailed = stringResource(R.string.dns_apply_failed)
    val dnsAppliedTemplate = stringResource(R.string.dns_applied)

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

            if (!hasWriteSecureSettings) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(Dimens.MD),
                        verticalArrangement = Arrangement.spacedBy(Dimens.SM)
                    ) {
                        Text(
                            text = stringResource(R.string.adb_permission_required),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.dns_permission_description),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = stringResource(R.string.adb_grant_command),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

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
                                        viewModel.onEvent(DnsEvent.ClearStatusMessage)
                                        viewModel.onEvent(DnsEvent.SelectProvider(provider))
                                    }
                                )
                            },
                            modifier = Modifier.clickable {
                                viewModel.onEvent(DnsEvent.ClearStatusMessage)
                                viewModel.onEvent(DnsEvent.SelectProvider(provider))
                            }
                        )
                    }
                }
            }

            if (state.selectedProvider == DnsProvider.CUSTOM) {
                OutlinedTextField(
                    value = state.customHostname,
                    onValueChange = { viewModel.onEvent(DnsEvent.UpdateCustomHostname(it)) },
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
                                            onClick = { viewModel.onEvent(DnsEvent.DeleteSavedProvider(provider.id)) }
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
                                            viewModel.onEvent(DnsEvent.ClearStatusMessage)
                                            viewModel.onEvent(DnsEvent.SelectSavedProvider(provider))
                                        }
                                )
                            }
                        }
                    }
                }
            }

            StatusMessage(message = state.statusMessage)

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = hasWriteSecureSettings,
                onClick = {
                    viewModel.onEvent(DnsEvent.ApplyDns(
                        successMessageTemplate = dnsAppliedTemplate,
                        failureMessage = dnsApplyFailed
                    ))
                }
            ) {
                Text(text = stringResource(R.string.apply_dns))
            }
        }
    }
}