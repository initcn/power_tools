package com.initcn.powertools.feature.vault.domain

import android.content.Context
import androidx.core.content.edit
import com.initcn.powertools.feature.vault.data.VaultDao
import com.initcn.powertools.feature.vault.data.VaultFileEntity
import com.initcn.powertools.feature.vault.provider.VaultHeaderManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileInputStream

object VaultSessionManager {

    private const val VAULT_PREFS = "vault_session_secure_prefs"
    private const val KEY_IS_SETUP = "vault_is_setup_completed"
    private const val KEY_VAULT_PIN = "vault_user_hashed_pin"
    private const val KEY_PIN_SALT = "vault_pin_salt"

    private const val KEY_BIO_ENABLED = "vault_bio_enabled"
    private const val KEY_ENCRYPTED_PIN = "vault_encrypted_pin"
    private const val KEY_BIO_IV = "vault_bio_iv"

    private const val ITERATIONS = 100_000 // High enough to slow down brute force

    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    private val _isVaultSetup = MutableStateFlow(false)
    val isVaultSetup: StateFlow<Boolean> = _isVaultSetup.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(false)
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    // Add this private helper function to hash the PIN
    private fun hashPin(pin: String, salt: ByteArray): String {
        val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = javax.crypto.spec.PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, 256)
        val hashBytes = factory.generateSecret(spec).encoded
        return android.util.Base64.encodeToString(hashBytes, android.util.Base64.NO_WRAP)
    }

    // We use your existing constants to properly sync the memory with the disk on app launch
    // Add this to VaultSessionManager.kt
    fun syncState(context: Context) {
        val prefs = context.getSharedPreferences(VAULT_PREFS, Context.MODE_PRIVATE)
        // CRITICAL: Ensure we update the flow values immediately
        _isVaultSetup.value = prefs.getBoolean(KEY_IS_SETUP, false)
        _isBiometricEnabled.value = prefs.getBoolean(KEY_BIO_ENABLED, false)
        // Do NOT set _isVaultUnlocked.value = true here.
        // It must only be true after successful decryption.
    }

    fun isUnlocked(): Boolean {
        return _isVaultUnlocked.value
    }

    fun setupVault(context: Context, pin: String) {
        val salt = ByteArray(16).apply { java.security.SecureRandom().nextBytes(this) }
        val hashedPin = hashPin(pin, salt)
        val saltBase64 = android.util.Base64.encodeToString(salt, android.util.Base64.NO_WRAP)

        val prefs = context.getSharedPreferences(VAULT_PREFS, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean(KEY_IS_SETUP, true)
            putString(KEY_VAULT_PIN, hashedPin)
            putString(KEY_PIN_SALT, saltBase64)
            apply()
        }
        _isVaultSetup.value = true
        _isVaultUnlocked.value = true
    }

    fun verifyAndUnlock(context: Context, pin: String): Boolean {
        val prefs = context.getSharedPreferences(VAULT_PREFS, Context.MODE_PRIVATE)
        val savedHashedPin = prefs.getString(KEY_VAULT_PIN, null)
        val savedSaltBase64 = prefs.getString(KEY_PIN_SALT, null)

        if (savedHashedPin == null || savedSaltBase64 == null) return false

        val salt = android.util.Base64.decode(savedSaltBase64, android.util.Base64.NO_WRAP)
        val attemptHash = hashPin(pin, salt)

        return if (attemptHash == savedHashedPin) {
            _isVaultUnlocked.value = true
            true
        } else {
            false
        }
    }

    fun enableBiometrics(context: Context, encryptedPin: String, iv: String) {
        val prefs = context.getSharedPreferences(VAULT_PREFS, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean(KEY_BIO_ENABLED, true)
            putString(KEY_ENCRYPTED_PIN, encryptedPin)
            putString(KEY_BIO_IV, iv)
            apply()
        }
        _isBiometricEnabled.value = true
    }

    fun disableBiometrics(context: Context) {
        val prefs = context.getSharedPreferences(VAULT_PREFS, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean(KEY_BIO_ENABLED, false)
            remove(KEY_ENCRYPTED_PIN)
            remove(KEY_BIO_IV)
            apply()
        }
        _isBiometricEnabled.value = false
    }

    fun getBiometricData(context: Context): Pair<String?, String?> {
        val prefs = context.getSharedPreferences(VAULT_PREFS, Context.MODE_PRIVATE)
        return Pair(
            prefs.getString(KEY_ENCRYPTED_PIN, null),
            prefs.getString(KEY_BIO_IV, null)
        )
    }

    fun lock() {
        _isVaultUnlocked.value = false
    }

    fun factoryReset(context: Context) {
        val prefs = context.getSharedPreferences(VAULT_PREFS, Context.MODE_PRIVATE)

        prefs.edit { clear() }

        _isVaultSetup.value = false
        _isBiometricEnabled.value = false

        lock()
    }

    suspend fun rebuildCatalogFromPublicStorage(context: Context, vaultDir: File, dao: VaultDao) {
        dao.deleteAll()

        vaultDir.listFiles { _, name -> name.endsWith(".enc") }?.forEach { file ->
            try {
                // FIX: Keep the ID as a string UUID
                val fileId = file.nameWithoutExtension

                FileInputStream(file).use { fileInputStream ->
                    VaultCryptoEngine.decryptStream(fileInputStream, consumeHeader = false)
                        .use { decryptedStream ->
                            val metadata = VaultHeaderManager.readHeaderFromStream(decryptedStream)

                            if (metadata != null) {
                                val secureName =
                                    VaultNameEncryptor.encryptName(context, metadata.originalName)

                                val entity = VaultFileEntity(
                                    id = fileId,
                                    encryptedName = secureName,
                                    isDirectory = false,
                                    parentPath = "/",
                                    fileSize = file.length(),
                                    mimeType = metadata.mimeType
                                )
                                dao.insertFile(entity)
                            }
                        }
                }
            } catch (_: Exception) {
                // Keep corrupt parsing events isolated cleanly
            }
        }
    }
}