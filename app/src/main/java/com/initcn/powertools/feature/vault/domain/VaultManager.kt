package com.initcn.powertools.feature.vault.domain

import android.content.Context
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.initcn.powertools.feature.vault.data.VaultDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultManager @Inject constructor(
    private val scanner: VaultScanner
) {

    private val authority = "com.initcn.powertools.vault.documents"
    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked = _isVaultUnlocked.asStateFlow()

    suspend fun initializeVault(
        context: Context,
        vaultRoot: DocumentFile,
        dao: VaultDao,
        recoveryPassword: String? = null
    ): Boolean {
        return try {
            if (!VaultCryptoEngine.hasMasterKey()) {
                val password = recoveryPassword ?: return false
                val escrowFile = vaultRoot.findFile("master.key.blob") ?: return false

                val restoredKey = context.contentResolver.openInputStream(escrowFile.uri)?.use {
                    VaultEscrowManager.restoreFromStream(it, password)
                } ?: return false

                VaultCryptoEngine.importMasterKey(restoredKey)
            }

            scanner.rebuildCatalogFromPublicStorage(vaultRoot, dao)
            _isVaultUnlocked.value = true

            context.contentResolver.notifyChange(
                DocumentsContract.buildRootsUri(authority), null
            )
            true
        } catch (e: Exception) {
            false
        } finally {
            VaultCryptoEngine.clearMemoryKey()
        }
    }

    fun createEscrowBackup(context: Context, vaultRoot: DocumentFile, recoveryPassword: String) {
        try {
            val masterKey = VaultCryptoEngine.getMasterKey()
            val escrowFile = vaultRoot.findFile("master.key.blob")
                ?: vaultRoot.createFile("application/octet-stream", "master.key.blob")
                ?: return

            context.contentResolver.openOutputStream(escrowFile.uri, "wt")?.use { outputStream ->
                VaultEscrowManager.saveToStream(masterKey, recoveryPassword, outputStream)
            }
        } finally {
            VaultCryptoEngine.clearMemoryKey()
        }
    }

    suspend fun rotateMasterVaultKey(context: Context, vaultRoot: DocumentFile, newPin: String): Boolean {
        return try {
            val oldMasterKey = VaultCryptoEngine.getMasterKey()
            val newMasterKey = VaultCryptoEngine.generateStandbyMasterKey()

            val escrowFile = vaultRoot.findFile("master.key.blob")
                ?: vaultRoot.createFile("application/octet-stream", "master.key.blob")
                ?: throw IllegalStateException("Could not create escrow file")

            context.contentResolver.openOutputStream(escrowFile.uri, "wt")?.use { out ->
                VaultEscrowManager.saveToStream(newMasterKey, newPin, out)
            }

            vaultRoot.listFiles().forEach { file ->
                if (file.name?.endsWith(".enc") == true) {
                    try {
                        val originalName = file.name!!
                        val tempFile = vaultRoot.createFile("application/octet-stream", "temp_$originalName")
                            ?: throw IllegalStateException("Could not create temp file")

                        val success = context.contentResolver.openInputStream(file.uri)?.use { input ->
                            context.contentResolver.openOutputStream(tempFile.uri)?.use { output ->
                                try {
                                    VaultCryptoEngine.rotateEnvelopeStream(input, output, oldMasterKey, newMasterKey)
                                    true
                                } catch (e: Exception) {
                                    false
                                }
                            }
                        } ?: false

                        if (success) {
                            file.delete()
                            tempFile.renameTo(originalName)
                        } else {
                            tempFile.delete()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            VaultCryptoEngine.importMasterKey(newMasterKey)
            VaultSessionManager.setupVault(context, newPin)
            VaultSessionManager.disableBiometrics(context)

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteEntireVault(vaultRoot: DocumentFile, dao: VaultDao): Boolean {
        return try {
            vaultRoot.listFiles().forEach { it.delete() }
            dao.deleteAll()
            VaultCryptoEngine.deleteMasterKey()
            _isVaultUnlocked.value = false
            true
        } catch (e: Exception) {
            false
        }
    }

    fun lockVault(context: Context) {
        _isVaultUnlocked.value = false
        VaultCryptoEngine.clearMemoryKey()

        context.contentResolver.notifyChange(
            DocumentsContract.buildRootsUri(authority), null
        )
    }
}