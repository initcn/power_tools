package com.initcn.powertools.ui.screens.dns

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.initcn.powertools.R
import com.initcn.powertools.data.AppPreferences
import com.initcn.powertools.features.dns.DnsManager
import com.initcn.powertools.model.CustomDnsProvider
import com.initcn.powertools.model.DnsProvider
import com.initcn.powertools.permissions.PermissionChecker
import com.initcn.powertools.ui.components.PowerToolScaffold
import com.initcn.powertools.ui.components.StatusMessage
import com.initcn.powertools.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnsScreen() {

    val context = LocalContext.current

    val hasWriteSecureSettings =
        PermissionChecker.hasWriteSecureSettings(
            context
        )

    var selectedProvider by rememberSaveable {
        mutableStateOf(
            DnsProvider.AUTOMATIC
        )
    }

    var customHostname by rememberSaveable {
        mutableStateOf("")
    }

    var statusMessage by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var savedProviders by remember {
        mutableStateOf(
            emptyList<CustomDnsProvider>()
        )
    }

    val dnsApplyFailed =
        stringResource(
            R.string.dns_apply_failed
        )

    LaunchedEffect(Unit) {

        selectedProvider =
            DnsManager.getCurrentProvider(
                context
            )

        if (
            selectedProvider ==
            DnsProvider.CUSTOM
        ) {

            customHostname =
                DnsManager.getCustomHostname(
                    context
                ) ?: ""
        }

        savedProviders =
            AppPreferences
                .getCustomDnsProviders(
                    context
                )
    }

    PowerToolScaffold(
        title = stringResource(
            R.string.dns_switcher
        )
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Dimens.ScreenPadding)
                .verticalScroll(
                    rememberScrollState()
                ),
            verticalArrangement =
                Arrangement.spacedBy(
                    Dimens.MD
                )
        ) {

            Text(
                text = stringResource(
                    R.string.dns_description
                ),
                style =
                    MaterialTheme
                        .typography
                        .bodyLarge
            )

            if (!hasWriteSecureSettings) {

                Card(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier =
                            Modifier.padding(
                                Dimens.MD
                            ),
                        verticalArrangement =
                            Arrangement.spacedBy(
                                Dimens.SM
                            )
                    ) {

                        Text(
                            text = stringResource(
                                R.string.adb_permission_required
                            ),
                            style =
                                MaterialTheme
                                    .typography
                                    .titleMedium
                        )

                        Text(
                            text = stringResource(
                                R.string.dns_permission_description
                            ),
                            style =
                                MaterialTheme
                                    .typography
                                    .bodyMedium
                        )

                        Text(
                            text = stringResource(
                                R.string.adb_grant_command
                            ),
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall
                        )
                    }
                }
            }

            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Column {

                    DnsProvider.entries
                        .forEach { provider ->

                            ListItem(
                                headlineContent = {

                                    Text(
                                        text =
                                            provider.title
                                    )
                                },

                                supportingContent = {

                                    when {

                                        provider.hostname != null -> {

                                            Text(
                                                text =
                                                    provider.hostname
                                            )
                                        }

                                        provider ==
                                                DnsProvider.OFF -> {

                                            Text(
                                                text =
                                                    stringResource(
                                                        R.string.disable_private_dns
                                                    )
                                            )
                                        }

                                        provider ==
                                                DnsProvider.AUTOMATIC -> {

                                            Text(
                                                text =
                                                    stringResource(
                                                        R.string.automatic_dns_mode
                                                    )
                                            )
                                        }
                                    }
                                },

                                leadingContent = {

                                    RadioButton(
                                        selected =
                                            selectedProvider ==
                                                    provider,

                                        onClick = {

                                            selectedProvider =
                                                provider
                                        }
                                    )
                                }
                            )
                        }
                }
            }

            if (
                selectedProvider ==
                DnsProvider.CUSTOM
            ) {

                OutlinedTextField(
                    value = customHostname,

                    onValueChange = {
                        customHostname = it
                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    label = {

                        Text(
                            stringResource(
                                R.string.custom_dns_hostname
                            )
                        )
                    },

                    placeholder = {

                        Text(
                            stringResource(
                                R.string.custom_dns_placeholder
                            )
                        )
                    },

                    singleLine = true
                )

                if (
                    savedProviders.isNotEmpty()
                ) {

                    Text(
                        text = stringResource(
                            R.string.saved_dns_providers
                        ),
                        style =
                            MaterialTheme
                                .typography
                                .titleMedium
                    )

                    Card(
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Column {

                            savedProviders
                                .forEach { provider ->

                                    ListItem(
                                        headlineContent = {

                                            Text(
                                                provider.name
                                            )
                                        },

                                        supportingContent = {

                                            Text(
                                                provider.hostname
                                            )
                                        },

                                        trailingContent = {

                                            IconButton(
                                                onClick = {

                                                    AppPreferences
                                                        .removeCustomDnsProvider(
                                                            context,
                                                            provider.id
                                                        )

                                                    savedProviders =
                                                        AppPreferences
                                                            .getCustomDnsProviders(
                                                                context
                                                            )
                                                }
                                            ) {

                                                Icon(
                                                    imageVector =
                                                        Icons.Default.Close,
                                                    contentDescription =
                                                        stringResource(
                                                            R.string.delete
                                                        )
                                                )
                                            }
                                        },

                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .clickable {

                                                    customHostname =
                                                        provider.hostname

                                                    selectedProvider =
                                                        DnsProvider.CUSTOM
                                                }
                                    )
                                }
                        }
                    }
                }
            }

            StatusMessage(
                message = statusMessage
            )

            Button(
                modifier =
                    Modifier.fillMaxWidth(),

                enabled =
                    hasWriteSecureSettings,

                onClick = {

                    val hostname =
                        customHostname
                            .trim()

                    val success =
                        DnsManager.apply(
                            context =
                                context,

                            provider =
                                selectedProvider,

                            customHostname =
                                hostname.takeIf {
                                    it.isNotBlank()
                                }
                        )

                    statusMessage =
                        if (success) {

                            if (
                                selectedProvider ==
                                DnsProvider.CUSTOM &&
                                hostname.isNotBlank()
                            ) {

                                AppPreferences
                                    .addCustomDnsProviderIfMissing(
                                        context,
                                        CustomDnsProvider(
                                            name = hostname,
                                            hostname = hostname
                                        )
                                    )

                                savedProviders =
                                    AppPreferences
                                        .getCustomDnsProviders(
                                            context
                                        )
                            }

                            context.getString(
                                R.string.dns_applied,
                                selectedProvider.title
                            )

                        } else {

                            dnsApplyFailed
                        }
                }
            ) {

                Text(
                    text = stringResource(
                        R.string.apply_dns
                    )
                )
            }
        }
    }
}