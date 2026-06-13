package com.initcn.powertools.feature.vault.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.initcn.powertools.R
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.initcn.powertools.core.theme.Dimens
import com.initcn.powertools.core.ui.components.PowerAlertDialog
import com.initcn.powertools.feature.vault.presentation.VaultEvent
import com.initcn.powertools.feature.vault.presentation.VaultUiState

@Composable
fun VaultSettingsTab(
    state: VaultUiState,
    onBiometricEnable: () -> Unit,
    onEvent: (VaultEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Security",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = Dimens.MD, top = Dimens.MD, bottom = Dimens.XS)
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.biometric_unlock)) },
            supportingContent = { Text(stringResource(R.string.biometric_unlock_desc)) },
            trailingContent = {
                Switch(
                    checked = state.isBiometricEnabled,
                    onCheckedChange = {
                        if (it) onBiometricEnable()
                        else onEvent(VaultEvent.DisableBiometrics)
                    }
                )
            }
        )
        HorizontalDivider(thickness = 0.5.dp)

        ListItem(
            headlineContent = { Text(stringResource(R.string.change_password)) },
            supportingContent = { Text(stringResource(R.string.change_password_desc)) },
            modifier = Modifier.clickable { onEvent(VaultEvent.ToggleKeyRotation(true)) }
        )
        HorizontalDivider(thickness = 0.5.dp)

        ListItem(
            headlineContent = { Text(stringResource(R.string.delete_vault), color = MaterialTheme.colorScheme.error) },
            supportingContent = { Text(stringResource(R.string.delete_vault_desc)) },
            trailingContent = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            modifier = Modifier.clickable { onEvent(VaultEvent.ToggleDeleteDialog(true)) }
        )

        // Show generic success/error messages below the settings
        state.errorMessage?.let {
            if (!state.isRotatingKey) { // Don't show it here if the dialog is open
                Spacer(modifier = Modifier.height(Dimens.MD))
                Text(
                    text = it,
                    color = if (it.contains("success", true) || it.contains("updated", true)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = Dimens.MD)
                )
            }
        }
    }

    // THE NEW KEY ROTATION POWER ALERT DIALOG
    if (state.isRotatingKey) {
        PowerAlertDialog(
            title = "Rotate Master Password",
            icon = Icons.Default.Lock,
            confirmText = if (state.isKeyRotationProcessing) "Processing..." else "Confirm",
            dismissText = "Cancel",
            onDismiss = {
                if (!state.isKeyRotationProcessing) {
                    onEvent(VaultEvent.ToggleKeyRotation(false))
                    onEvent(VaultEvent.ClearError)
                }
            },
            onConfirm = {
                if (state.newPinInput.length >= 4) onEvent(VaultEvent.RotateMasterKey)
                else onEvent(VaultEvent.SetError("Password must be at least 4 digits."))
            },
            content = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SM)
                ) {
                    Text(
                        "This will generate a new cryptographic Master Key and update all file headers. The actual file contents will not need to be re-encrypted.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = state.newPinInput,
                        onValueChange = { onEvent(VaultEvent.UpdateNewPinInput(it)) },
                        label = { Text("Enter New Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isKeyRotationProcessing // Disable typing while processing
                    )

                    // Show Processing Indicator inside the dialog
                    if (state.isKeyRotationProcessing) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(Dimens.IconMD))
                            Spacer(modifier = Modifier.width(Dimens.SM))
                            Text("Re-encrypting headers...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    // Show Errors inside the dialog
                    state.errorMessage?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        )
    }
}