package com.initcn.powertools.feature.vault.presentation

import android.net.Uri
import com.initcn.powertools.core.utils.UiText
import com.initcn.powertools.feature.vault.data.VaultFileEntity

sealed class VaultEvent {
    data class UpdatePinInput(val pin: String) : VaultEvent()
    data class UpdatePinConfirmInput(val pin: String) : VaultEvent()
    data class UpdateNewPinInput(val pin: String) : VaultEvent()
    data class ToggleKeyRotation(val show: Boolean) : VaultEvent()
    data class ToggleDeleteDialog(val show: Boolean) : VaultEvent()
    data class ToggleFileDeleteDialog(val file: VaultFileEntity?) : VaultEvent()
    data class SetError(val message: UiText?) : VaultEvent()
    data class ImportFile(val uri: Uri) : VaultEvent()

    object ClearError : VaultEvent()
    object LockVault : VaultEvent()
    object SetupVault : VaultEvent()
    object RestoreVault : VaultEvent()
    object UnlockVault : VaultEvent()
    object ProcessBiometricUnlock : VaultEvent()
    object EnableBiometrics : VaultEvent()
    object DisableBiometrics : VaultEvent()
    object RotateMasterKey : VaultEvent()
    object DeleteEntireVault : VaultEvent()
    object DeleteFile : VaultEvent()
}