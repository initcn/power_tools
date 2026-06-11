package com.initcn.powertools.features.vault

import android.content.Context
import android.util.Base64
import com.initcn.powertools.crypto.VaultCryptoEngine
import com.initcn.powertools.crypto.VaultEscrowManager
import com.initcn.powertools.data.vault.VaultDao
import com.initcn.powertools.features.vault.saf.VaultSessionManager
import com.initcn.powertools.features.vault.saf.auth.BiometricKeyManager
import com.initcn.powertools.features.vault.scanner.VaultScanner
import java.io.File

object VaultManager {

    // Expose a public getter but keep the setter private to resolve "unused" warnings
    var isVaultUnlocked = false
        private set

    suspend fun initializeVault(
        context: Context,
        vaultDir: File,
        dao: VaultDao,
        recoveryPassword: String? = null
    ): Boolean {
        return try {
            if (!VaultCryptoEngine.hasMasterKey()) {
                if (recoveryPassword == null) return false

                val restoredKey = VaultEscrowManager.restoreMasterKeyFromEscrow(vaultDir, recoveryPassword)
                    ?: return false

                VaultCryptoEngine.importMasterKey(restoredKey)
            }

            VaultScanner.rebuildCatalogFromPublicStorage(context, vaultDir, dao)

            isVaultUnlocked = true
            true
        } finally {
            VaultCryptoEngine.clearMemoryKey()
        }
    }

    suspend fun syncVault(context: Context, vaultDir: File, dao: VaultDao) {
        if (!isVaultUnlocked) throw IllegalStateException("Vault must be unlocked first")
        VaultScanner.rebuildCatalogFromPublicStorage(context, vaultDir, dao)
    }

    fun createEscrowBackup(vaultDir: File, recoveryPassword: String) {
        try {
            VaultEscrowManager.saveMasterKeyToEscrow(vaultDir, recoveryPassword)
        } finally {
            VaultCryptoEngine.clearMemoryKey()
        }
    }

    suspend fun rotateMasterVaultKey(
        context: Context,
        vaultDir: File,
        newPin: String
    ): Boolean {
        if (!isVaultUnlocked) throw IllegalStateException("Vault must be unlocked to rotate keys.")

        return try {
            val oldMasterKey = VaultCryptoEngine.getMasterKey()
            val newMasterKey = VaultCryptoEngine.generateStandbyMasterKey()

            val files = vaultDir.listFiles { _, name -> name.endsWith(".enc") } ?: emptyArray()
            for (file in files) {
                VaultCryptoEngine.rotateFileEnvelopeKey(file, oldMasterKey, newMasterKey)
            }

            VaultCryptoEngine.importMasterKey(newMasterKey)
            VaultEscrowManager.saveMasterKeyToEscrow(vaultDir, newPin)
            VaultSessionManager.setupVault(context, newPin)

            // --- SILENTLY SYNC BIOMETRICS ---
            // If biometrics are enabled, automatically re-encrypt the new PIN with the
            // hardware biometric key so the user isn't locked out of fingerprint scanning.
            if (VaultSessionManager.isBiometricEnabled.value) {
                try {
                    val cipher = BiometricKeyManager.getEncryptionCipher()
                    val encryptedBytes = cipher.doFinal(newPin.toByteArray(Charsets.UTF_8))
                    VaultSessionManager.enableBiometrics(
                        context,
                        Base64.encodeToString(encryptedBytes, Base64.DEFAULT),
                        Base64.encodeToString(cipher.iv, Base64.DEFAULT)
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Failsafe: If biometric re-encryption fails, turn it off so the user isn't permanently locked out
                    VaultSessionManager.disableBiometrics(context)
                }
            }
            // --------------------------------

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            VaultCryptoEngine.clearMemoryKey()
        }
    }

    suspend fun deleteEntireVault(context: Context, vaultDir: File, dao: VaultDao): Boolean {
        return try {
            if (vaultDir.exists() && vaultDir.isDirectory) {
                vaultDir.listFiles()?.forEach { it.delete() }
            }

            dao.deleteAll()
            VaultCryptoEngine.deleteMasterKey()
            VaultSessionManager.factoryReset(context)

            isVaultUnlocked = false
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun lockVault() {
        isVaultUnlocked = false
        VaultCryptoEngine.clearMemoryKey()
    }
}