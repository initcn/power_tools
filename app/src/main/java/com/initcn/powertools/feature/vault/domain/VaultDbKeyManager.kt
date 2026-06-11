package com.initcn.powertools.feature.vault.domain

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object VaultDbKeyManager {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val DB_KEY_ALIAS = "PowerToolsDbEncryptionKey"
    private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val PREFS_NAME = "vault_db_secure_prefs"
    private const val KEY_ENCRYPTED_PASSPHRASE = "encrypted_db_passphrase"
    private const val KEY_GCM_IV = "db_passphrase_iv"

    private fun getOrCreateWrapperKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        keyStore.getKey(DB_KEY_ALIAS, null)?.let { return it as SecretKey }

        val keyGenerator =
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            DB_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun getDatabasePassphrase(context: Context): ByteArray {
        // FIXED: Removed the trailing "" text token causing array indexing errors
        System.loadLibrary("sqlcipher")

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedPassphraseBase64 = prefs.getString(KEY_ENCRYPTED_PASSPHRASE, null)
        val ivBase64 = prefs.getString(KEY_GCM_IV, null)

        if (encryptedPassphraseBase64 != null && ivBase64 != null) {
            val encryptedBytes = Base64.decode(encryptedPassphraseBase64, Base64.DEFAULT)
            val iv = Base64.decode(ivBase64, Base64.DEFAULT)

            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateWrapperKey(), spec)
            return cipher.doFinal(encryptedBytes)
        }

        val rawPassphrase = ByteArray(32)
        SecureRandom().nextBytes(rawPassphrase)

        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateWrapperKey())
        val encryptedPassphraseBytes = cipher.doFinal(rawPassphrase)

        prefs.edit {
            putString(
                KEY_ENCRYPTED_PASSPHRASE,
                Base64.encodeToString(encryptedPassphraseBytes, Base64.DEFAULT)
            )
            putString(KEY_GCM_IV, Base64.encodeToString(cipher.iv, Base64.DEFAULT))
        }

        return rawPassphrase
    }
}