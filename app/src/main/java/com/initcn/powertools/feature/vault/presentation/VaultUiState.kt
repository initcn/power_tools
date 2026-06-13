package com.initcn.powertools.feature.vault.presentation

import com.initcn.powertools.core.utils.UiText
import com.initcn.powertools.feature.vault.data.VaultFileEntity

data class VaultUiState(
    val isLoading: Boolean = true,
    val isVaultSetup: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val isUnlocked: Boolean = false,
    val isRotatingKey: Boolean = false,
    val isKeyRotationProcessing: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val fileToDelete: VaultFileEntity? = null,
    val pinInput: String = "",
    val pinConfirmInput: String = "",
    val newPinInput: String = "",
    val errorMessage: UiText? = null,
    val databaseFiles: List<VaultFileEntity> = emptyList()
)