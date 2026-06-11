package com.initcn.powertools.feature.callblocker.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.initcn.powertools.core.theme.Dimens
import com.initcn.powertools.feature.callblocker.presentation.CallBlockerEvent
import com.initcn.powertools.feature.callblocker.presentation.CallBlockerUiState

@Composable
fun CallBlockerSettingsTab(
    state: CallBlockerUiState,
    hasCallLogPermission: Boolean,
    hasContactsPermission: Boolean,
    onRequestCallLogPermission: () -> Unit,
    onRequestContactsPermission: () -> Unit,
    onEvent: (CallBlockerEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // --- 1. PERMISSIONS SECTION ---
        Text(
            text = "App Permissions",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = Dimens.MD, top = Dimens.MD, bottom = Dimens.XS)
        )

        ListItem(
            headlineContent = { Text("Call Log Access") },
            supportingContent = { Text("Required to view recent calls history.") },
            trailingContent = {
                if (hasCallLogPermission) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Granted", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                } else {
                    TextButton(onClick = onRequestCallLogPermission, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Text("Grant")
                    }
                }
            }
        )
        HorizontalDivider(thickness = 0.5.dp)

        ListItem(
            headlineContent = { Text("Contacts Access") },
            supportingContent = { Text("Required to identify and block unsaved numbers.") },
            trailingContent = {
                if (hasContactsPermission) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Granted", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                } else {
                    TextButton(onClick = onRequestContactsPermission, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Text("Grant")
                    }
                }
            }
        )
        HorizontalDivider(thickness = 0.5.dp)
        Spacer(modifier = Modifier.height(Dimens.SM))

        // --- 2. FILTER TRIGGERS SECTION ---
        Text(
            text = "Block Triggers",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = Dimens.MD, top = Dimens.MD, bottom = Dimens.XS)
        )

        ListItem(
            headlineContent = { Text("Block Hidden Numbers") },
            supportingContent = { Text("Automatically intercept private or restricted caller IDs.") },
            trailingContent = { Switch(checked = state.blockHiddenNumbers, onCheckedChange = { onEvent(CallBlockerEvent.ToggleBlockHidden(it)) }) }
        )
        HorizontalDivider(thickness = 0.5.dp)

        ListItem(
            headlineContent = { Text("Block Unsaved Contacts") },
            supportingContent = {
                Column {
                    Text("Only allow numbers saved in your phone book.")
                    if (!hasContactsPermission) {
                        Text("Requires Contacts Permission. Tap to grant above.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = Dimens.XXS))
                    }
                }
            },
            trailingContent = {
                Switch(
                    checked = state.blockUnsavedContacts && hasContactsPermission,
                    enabled = hasContactsPermission,
                    onCheckedChange = { onEvent(CallBlockerEvent.ToggleBlockUnsaved(it)) }
                )
            }
        )
        HorizontalDivider(thickness = 0.5.dp)
        Spacer(modifier = Modifier.height(Dimens.SM))

        // --- 3. INTERCEPTION METHOD SECTION (Restored!) ---
        Text(
            text = "Interception Behavior",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = Dimens.MD, top = Dimens.MD, bottom = Dimens.XS)
        )

        ListItem(
            headlineContent = { Text("Disallow Call") },
            supportingContent = { Text("Terminate the call immediately.") },
            trailingContent = { Switch(checked = state.disallowCall, onCheckedChange = { onEvent(CallBlockerEvent.ToggleDisallow(it)) }) }
        )
        HorizontalDivider(thickness = 0.5.dp)

        ListItem(
            headlineContent = { Text("Reject Call") },
            supportingContent = { Text("Send the blocked caller directly to voicemail.") },
            trailingContent = {
                Switch(
                    checked = state.rejectCall && state.disallowCall,
                    enabled = state.disallowCall, // Only works if disallow is enabled
                    onCheckedChange = { onEvent(CallBlockerEvent.ToggleReject(it)) }
                )
            }
        )
        HorizontalDivider(thickness = 0.5.dp)

        ListItem(
            headlineContent = { Text("Skip System Notification") },
            supportingContent = { Text("Prevent Android from showing a 'Missed Call' alert.") },
            trailingContent = {
                Switch(
                    checked = state.skipNotif && state.disallowCall,
                    enabled = state.disallowCall,
                    onCheckedChange = { onEvent(CallBlockerEvent.ToggleSkipNotif(it)) }
                )
            }
        )
        HorizontalDivider(thickness = 0.5.dp)

        ListItem(
            headlineContent = { Text("Silence Call Only") },
            supportingContent = { Text("Let the call ring in the background without sound/vibration instead of dropping it.") },
            trailingContent = {
                Switch(
                    checked = state.silenceCall && !state.disallowCall,
                    enabled = !state.disallowCall, // Silence and Disallow are mutually exclusive in Telecom API
                    onCheckedChange = { onEvent(CallBlockerEvent.ToggleSilence(it)) }
                )
            }
        )
        HorizontalDivider(thickness = 0.5.dp)
        Spacer(modifier = Modifier.height(Dimens.SM))

        // --- 4. DATA MANAGEMENT SECTION ---
        Text(
            text = "Data Management",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = Dimens.MD, top = Dimens.MD, bottom = Dimens.XS)
        )

        ListItem(
            headlineContent = { Text("Export Rules") },
            supportingContent = { Text("Save your blocklist and whitelist to a file.") },
            modifier = Modifier.clickable { onEvent(CallBlockerEvent.ExportRules) }
        )
        HorizontalDivider(thickness = 0.5.dp)

        ListItem(
            headlineContent = { Text("Import Rules") },
            supportingContent = { Text("Load rules from a previously exported file.") },
            modifier = Modifier.clickable { onEvent(CallBlockerEvent.ImportRules(android.net.Uri.EMPTY)) }
        )
        HorizontalDivider(thickness = 0.5.dp)

        ListItem(
            headlineContent = { Text("Clear All Rules", color = MaterialTheme.colorScheme.error) },
            supportingContent = { Text("Permanently delete all blocklist and whitelist entries.") },
            modifier = Modifier.clickable { onEvent(CallBlockerEvent.ClearAllRules) },
            trailingContent = { Icon(Icons.Default.Warning, contentDescription = "Warning", tint = MaterialTheme.colorScheme.error) }
        )

        Spacer(modifier = Modifier.height(Dimens.XXL))
    }
}