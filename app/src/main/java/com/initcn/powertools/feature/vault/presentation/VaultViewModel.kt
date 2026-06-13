package com.initcn.powertools.feature.vault.presentation

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.initcn.powertools.core.permissions.StorageAccessManager
import com.initcn.powertools.feature.vault.data.VaultDao
import com.initcn.powertools.feature.vault.data.VaultStorageManager
import com.initcn.powertools.feature.vault.domain.VaultManager
import com.initcn.powertools.feature.vault.domain.VaultSessionManager
import com.initcn.powertools.feature.vault.domain.auth.BiometricKeyManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class VaultViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val vaultDao: VaultDao,
    private val vaultManager: VaultManager,
    private val vaultStorageManager: VaultStorageManager // Injected Storage Manager
) : ViewModel() {

    val vaultRoot: DocumentFile?
        get() = StorageAccessManager.getPersistedUri(context, StorageAccessManager.KEY_DOCUMENTS_URI)
            ?.let { DocumentFile.fromTreeUri(context, it) }

    fun hasEscrowBackup(): Boolean {
        return vaultRoot?.findFile("master.key.blob")?.exists() == true
    }

    // Holds the plaintext PIN in RAM only while the vault is actively unlocked
    private var activeSessionPin: String? = null

    private val _uiState = MutableStateFlow(
        VaultUiState(
            isLoading = true, // Start in loading state
            isVaultSetup = VaultSessionManager.isVaultSetup.value,
            isBiometricEnabled = VaultSessionManager.isBiometricEnabled.value,
            isUnlocked = VaultSessionManager.isVaultUnlocked.value
        )
    )
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            VaultSessionManager.syncState(context)

            combine(
                VaultSessionManager.isVaultSetup,
                VaultSessionManager.isBiometricEnabled,
                VaultSessionManager.isVaultUnlocked,
                vaultDao.getFilesByParentPathFlow("/")
            ) { isSetup, isBio, isUnlocked, files ->
                VaultUiState(
                    isLoading = false, // Sync complete
                    isVaultSetup = isSetup,
                    isBiometricEnabled = isBio,
                    isUnlocked = isUnlocked,
                    databaseFiles = files
                )
            }.collect { newState ->
                withContext(Dispatchers.Main) {
                    _uiState.value = newState
                }
            }
        }
    }

    fun onEvent(event: VaultEvent) {
        when (event) {
            is VaultEvent.UpdatePinInput -> _uiState.update { it.copy(pinInput = event.pin, errorMessage = null) }
            is VaultEvent.UpdatePinConfirmInput -> _uiState.update { it.copy(pinConfirmInput = event.pin, errorMessage = null) }
            is VaultEvent.UpdateNewPinInput -> _uiState.update { it.copy(newPinInput = event.pin, errorMessage = null) }
            is VaultEvent.ToggleKeyRotation -> _uiState.update { it.copy(isRotatingKey = event.show, newPinInput = "") }
            is VaultEvent.ToggleDeleteDialog -> _uiState.update { it.copy(showDeleteDialog = event.show) }
            is VaultEvent.ToggleFileDeleteDialog -> _uiState.update { it.copy(fileToDelete = event.file) }
            is VaultEvent.ClearError -> _uiState.update { it.copy(errorMessage = null) }
            is VaultEvent.SetError -> _uiState.update { it.copy(errorMessage = event.message) }
            is VaultEvent.ImportFile -> importFile(event.uri)
            is VaultEvent.LockVault -> {
                VaultSessionManager.lock()
                vaultManager.lockVault(context) // Passed context to notify OS
                activeSessionPin = null // Wipe it from memory
                _uiState.update { it.copy(pinInput = "", pinConfirmInput = "", isRotatingKey = false) }
            }
            is VaultEvent.SetupVault -> setupVault()
            is VaultEvent.RestoreVault -> restoreVault()
            is VaultEvent.UnlockVault -> unlockVault()
            is VaultEvent.ProcessBiometricUnlock -> processBiometricUnlock()
            is VaultEvent.EnableBiometrics -> enableBiometrics()
            is VaultEvent.DisableBiometrics -> VaultSessionManager.disableBiometrics(context)
            is VaultEvent.RotateMasterKey -> rotateMasterKey()
            is VaultEvent.DeleteEntireVault -> deleteEntireVault()
            is VaultEvent.DeleteFile -> deleteFile()
        }
    }

    private fun importFile(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Extract original filename and mime type from the Android URI
                var fileName = "imported_file"
                var mimeType = "application/octet-stream"

                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) fileName = cursor.getString(nameIndex)
                    }
                }
                mimeType = context.contentResolver.getType(uri) ?: mimeType

                // 2. Stream the file into the vault
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val success = vaultStorageManager.importFile(inputStream, fileName, mimeType, "/")
                    if (!success) {
                        _uiState.update { it.copy(errorMessage = "Failed to encrypt and store file.") }
                    }
                }

                // Note: No need to manually update the UI here.
                // The Room Flow will instantly detect the new file and refresh the screen!

            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to import file: ${e.localizedMessage}") }
            }
        }
    }

    private fun setupVault() {
        val pin = _uiState.value.pinInput
        val root = vaultRoot ?: return
        if (pin.length < 4) {
            _uiState.update { it.copy(errorMessage = "PIN must be at least 4 digits.") }
            return
        }
        VaultSessionManager.setupVault(context, pin)
        activeSessionPin = pin // Save to RAM for session
        vaultManager.createEscrowBackup(context, root, pin)
        _uiState.update { it.copy(pinInput = "", pinConfirmInput = "") }
    }

    private fun restoreVault() {
        val pin = _uiState.value.pinInput
        val root = vaultRoot ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val success = vaultManager.initializeVault(context, root, vaultDao, pin)
            if (success) {
                VaultSessionManager.setupVault(context, pin)
                activeSessionPin = pin // Save to RAM for session
                _uiState.update { it.copy(pinInput = "", pinConfirmInput = "") }
            } else {
                _uiState.update { it.copy(errorMessage = "Invalid PIN.") }
            }
        }
    }

    private fun unlockVault() {
        val pin = _uiState.value.pinInput
        val root = vaultRoot ?: return
        viewModelScope.launch(Dispatchers.IO) {
            if (VaultSessionManager.verifyAndUnlock(context, pin)) {
                val success = vaultManager.initializeVault(context, root, vaultDao, pin)
                if (success) {
                    activeSessionPin = pin // Save to RAM for session
                    _uiState.update { it.copy(pinInput = "", errorMessage = null) }
                } else {
                    VaultSessionManager.lock()
                    activeSessionPin = null
                    _uiState.update { it.copy(errorMessage = "Vault initialization failed.") }
                }
            } else {
                _uiState.update { it.copy(errorMessage = "Incorrect credentials.") }
            }
        }
    }

    private fun processBiometricUnlock() {
        val root = vaultRoot ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val (encryptedPinB64, ivB64) = VaultSessionManager.getBiometricData(context)
                if (encryptedPinB64 != null && ivB64 != null) {
                    val encryptedBytes = Base64.decode(encryptedPinB64, Base64.DEFAULT)
                    val iv = Base64.decode(ivB64, Base64.DEFAULT)
                    val cipher = BiometricKeyManager.getDecryptionCipher(iv)
                    val decryptedPin = String(cipher.doFinal(encryptedBytes), Charsets.UTF_8)

                    val success = vaultManager.initializeVault(context, root, vaultDao, decryptedPin)
                    if (success && VaultSessionManager.verifyAndUnlock(context, decryptedPin)) {
                        activeSessionPin = decryptedPin // Save to RAM for session
                        _uiState.update { it.copy(pinInput = decryptedPin) }
                    } else {
                        _uiState.update { it.copy(errorMessage = "Biometric recovery failed.") }
                    }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(errorMessage = "Biometrics invalidated.") }
                VaultSessionManager.disableBiometrics(context)
            }
        }
    }

    private fun enableBiometrics() {
        try {
            // Pull the PIN securely from our ephemeral RAM variable
            val activePin = activeSessionPin
            if (activePin.isNullOrBlank()) {
                _uiState.update { it.copy(errorMessage = "Active PIN missing from memory. Please lock and unlock the vault to enable biometrics.") }
                return
            }

            val cipher = BiometricKeyManager.getEncryptionCipher()
            val encryptedBytes = cipher.doFinal(activePin.toByteArray(Charsets.UTF_8))

            VaultSessionManager.enableBiometrics(
                context,
                Base64.encodeToString(encryptedBytes, Base64.DEFAULT),
                Base64.encodeToString(cipher.iv, Base64.DEFAULT)
            )

            _uiState.update { it.copy(errorMessage = "Biometrics enabled successfully!") }
        } catch (_: Exception) {
            _uiState.update { it.copy(errorMessage = "Biometric setup failed.") }
        }
    }

    private fun rotateMasterKey() {
        val newPin = _uiState.value.newPinInput
        val root = vaultRoot ?: return

        // 1. Show the processing indicator inside the dialog
        _uiState.update { it.copy(isKeyRotationProcessing = true, errorMessage = null) }

        viewModelScope.launch(Dispatchers.IO) {
            val success = vaultManager.rotateMasterVaultKey(context, root, newPin)
            withContext(Dispatchers.Main) {
                if (success) {
                    activeSessionPin = newPin // Update RAM session
                    // 2. Hide dialog and processing state on success
                    _uiState.update {
                        it.copy(isRotatingKey = false, isKeyRotationProcessing = false, newPinInput = "", pinInput = newPin, errorMessage = "Master Password updated!")
                    }
                } else {
                    // 3. Keep dialog open but stop processing to show the error
                    _uiState.update {
                        it.copy(isKeyRotationProcessing = false, errorMessage = "Failed to rotate key.")
                    }
                }
            }
        }
    }

    private fun deleteEntireVault() {
        val root = vaultRoot ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val success = vaultManager.deleteEntireVault(root, vaultDao)

            // Clear all session preferences and state flows if deletion succeeds
            if (success) {
                VaultSessionManager.factoryReset(context)
                activeSessionPin = null // Wipe from RAM
            }

            withContext(Dispatchers.Main) {
                if (success) {
                    // Reset inputs and hide the dialog
                    _uiState.update {
                        it.copy(
                            pinInput = "",
                            pinConfirmInput = "",
                            showDeleteDialog = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(errorMessage = "Deletion failed.") }
                }
            }
        }
    }

    private fun deleteFile() {
        val file = _uiState.value.fileToDelete ?: return
        val root = vaultRoot ?: return
        viewModelScope.launch(Dispatchers.IO) {
            vaultDao.deleteFile(file)
            root.findFile("${file.id}.enc")?.delete()
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(fileToDelete = null) }
            }
        }
    }
}