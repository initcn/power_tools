package com.initcn.powertools.feature.callblocker.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.initcn.powertools.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.initcn.powertools.core.theme.Dimens
import com.initcn.powertools.feature.callblocker.presentation.CallBlockerEvent
import com.initcn.powertools.feature.callblocker.presentation.CallBlockerUiState

@Composable
fun CallBlockerSettingsTab(
    state: CallBlockerUiState,
    onEvent: (CallBlockerEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // FILTER TRIGGERS SECTION
        // FILTER TRIGGERS SECTION
        Text(
            text = stringResource(R.string.block_triggers),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = Dimens.MD, top = Dimens.MD, bottom = Dimens.XS)
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.block_all_calls)) },
            supportingContent = { Text(stringResource(R.string.block_all_calls_desc)) },
            trailingContent = { Switch(checked = state.blockAllCalls, onCheckedChange = { onEvent(CallBlockerEvent.ToggleBlockAll(it)) }) }
        )
        HorizontalDivider(thickness = 0.5.dp)

        ListItem(
            headlineContent = { Text(stringResource(R.string.block_hidden_numbers)) },
            supportingContent = { Text(stringResource(R.string.block_hidden_numbers_desc)) },
            trailingContent = { Switch(checked = state.blockHiddenNumbers, onCheckedChange = { onEvent(CallBlockerEvent.ToggleBlockHidden(it)) }) }
        )
        HorizontalDivider(thickness = 0.5.dp)

        ListItem(
            headlineContent = { Text(stringResource(R.string.block_unsaved_contacts)) },
            supportingContent = { Text(stringResource(R.string.block_unsaved_contacts_desc)) },
            trailingContent = {
                Switch(
                    checked = state.blockUnsavedContacts,
                    onCheckedChange = { onEvent(CallBlockerEvent.ToggleBlockUnsaved(it)) }
                )
            }
        )
        HorizontalDivider(thickness = 0.5.dp)
        Spacer(modifier = Modifier.height(Dimens.SM))

        // INTERCEPTION METHOD SECTION
        Text(
            text = stringResource(R.string.interception_behavior),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = Dimens.MD, top = Dimens.MD, bottom = Dimens.XS)
        )

        val isOff = !state.disallowCall && !state.silenceCall
        val isSilence = state.silenceCall && !state.disallowCall
        val isDisallow = state.disallowCall

        // Mode Selector Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.MD, vertical = Dimens.SM),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SM)
        ) {
            Button(
                onClick = {
                    onEvent(CallBlockerEvent.ToggleDisallow(false))
                    onEvent(CallBlockerEvent.ToggleSilence(false))
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isOff) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isOff) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) { Text(stringResource(R.string.mode_off)) }

            Button(
                onClick = {
                    onEvent(CallBlockerEvent.ToggleDisallow(false))
                    onEvent(CallBlockerEvent.ToggleSilence(true))
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSilence) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isSilence) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) { Text(stringResource(R.string.mode_silence)) }

            Button(
                onClick = {
                    onEvent(CallBlockerEvent.ToggleSilence(false))
                    onEvent(CallBlockerEvent.ToggleDisallow(true))
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDisallow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isDisallow) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) { Text(stringResource(R.string.mode_disallow)) }
        }
        HorizontalDivider(thickness = 0.5.dp)

        // Conditional "Pop Up" only for Disallow
        AnimatedVisibility(visible = state.disallowCall) {
            Column {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.reject_call)) },
                    supportingContent = { Text(stringResource(R.string.reject_call_desc)) },
                    trailingContent = {
                        Switch(
                            checked = state.rejectCall,
                            onCheckedChange = { onEvent(CallBlockerEvent.ToggleReject(it)) }
                        )
                    }
                )
                HorizontalDivider(thickness = 0.5.dp)
            }
        }
        Spacer(modifier = Modifier.height(Dimens.SM))

        // GLOBAL: Skip Notification (Appears for both Silence/Disallow)
        ListItem(
            headlineContent = { Text(stringResource(R.string.skip_system_notif)) },
            supportingContent = { Text(stringResource(R.string.skip_system_notif_desc)) },
            trailingContent = {
                Switch(
                    checked = state.skipNotif,
                    enabled = !isOff,
                    onCheckedChange = { onEvent(CallBlockerEvent.ToggleSkipNotif(it)) }
                )
            }
        )
        HorizontalDivider(thickness = 0.5.dp)

        // DATA MANAGEMENT SECTION
        Text(
            text = stringResource(R.string.data_management),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = Dimens.MD, top = Dimens.MD, bottom = Dimens.XS)
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.export_rules)) },
            supportingContent = { Text(stringResource(R.string.export_rules_desc)) },
            modifier = Modifier.clickable { onEvent(CallBlockerEvent.ExportRules) }
        )
        HorizontalDivider(thickness = 0.5.dp)

        ListItem(
            headlineContent = { Text(stringResource(R.string.import_rules)) },
            supportingContent = { Text(stringResource(R.string.import_rules_desc)) },
            modifier = Modifier.clickable { onEvent(CallBlockerEvent.ImportRules(android.net.Uri.EMPTY)) }
        )
        HorizontalDivider(thickness = 0.5.dp)

        ListItem(
            headlineContent = { Text(stringResource(R.string.clear_all_rules), color = MaterialTheme.colorScheme.error) },
            supportingContent = { Text(stringResource(R.string.clear_all_rules_desc)) },
            modifier = Modifier.clickable { onEvent(CallBlockerEvent.ClearAllRules) },
            trailingContent = { Icon(Icons.Default.Warning, contentDescription = "Warning", tint = MaterialTheme.colorScheme.error) }
        )

        Spacer(modifier = Modifier.height(Dimens.XXL))
    }
}