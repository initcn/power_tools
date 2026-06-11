package com.initcn.powertools.feature.vault.domain

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object VaultNameEncryptor {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val NAME_KEY_ALIAS = "PowerToolsNameWrapperKey"
    private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"

    private const val PREFS_NAME = "vault_name_secure_prefs"
    private const val KEY_ENCRYPTED_NAME_KEY = "encrypted_name_key"
    private const val KEY_GCM_IV = "name_key_iv"

    private const val TAG_BIT_LENGTH = 128
    private const val FORMAT_VERSION_1: Byte = 0x01

    private fun getOrCreateHardwareWrapperKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        keyStore.getKey(NAME_KEY_ALIAS, null)?.let { return it as SecretKey }

        val keyGenerator =
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            NAME_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun getNameKey(context: Context): SecretKeySpec {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedKeyBase64 = prefs.getString(KEY_ENCRYPTED_NAME_KEY, null)
        val ivBase64 = prefs.getString(KEY_GCM_IV, null)

        if (encryptedKeyBase64 != null && ivBase64 != null) {
            val encryptedBytes = Base64.decode(encryptedKeyBase64, Base64.DEFAULT)
            val iv = Base64.decode(ivBase64, Base64.DEFAULT)

            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE, getOrCreateHardwareWrapperKey(),
                GCMParameterSpec(TAG_BIT_LENGTH, iv)
            )
            val decryptedRawKey = cipher.doFinal(encryptedBytes)
            return SecretKeySpec(decryptedRawKey, "AES")
        }

        val rawKeyMaterial = ByteArray(32)
        SecureRandom().nextBytes(rawKeyMaterial)

        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateHardwareWrapperKey())
        val encryptedKeyBytes = cipher.doFinal(rawKeyMaterial)

        prefs.edit().apply {
            putString(
                KEY_ENCRYPTED_NAME_KEY,
                Base64.encodeToString(encryptedKeyBytes, Base64.DEFAULT)
            )
            putString(KEY_GCM_IV, Base64.encodeToString(cipher.iv, Base64.DEFAULT))
            apply()
        }

        return SecretKeySpec(rawKeyMaterial, "AES")
    }

    fun encryptName(context: Context, originalName: String): String {
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv) // Secure random nonce

        cipher.init(Cipher.ENCRYPT_MODE, getNameKey(context), GCMParameterSpec(TAG_BIT_LENGTH, iv))

        val ciphertext = cipher.doFinal(originalName.toByteArray(Charsets.UTF_8))

        // Payload Structure: [Version Byte (1)] + [IV (12)] + [Ciphertext (Variable)]
        val combined = ByteArray(1 + iv.size + ciphertext.size)
        combined[0] = FORMAT_VERSION_1
        System.arraycopy(iv, 0, combined, 1, iv.size)
        System.arraycopy(ciphertext, 0, combined, 1 + iv.size, ciphertext.size)

        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decryptName(context: Context, encryptedName: String): String {
        val decoded = Base64.decode(encryptedName, Base64.NO_WRAP)

        // Strict format enforcement. Fails fast if it sees a legacy or corrupted payload.
        require(decoded.isNotEmpty() && decoded[0] == FORMAT_VERSION_1 && decoded.size >= 13) {
            "Invalid or legacy filename ciphertext format."
        }

        val iv = decoded.copyOfRange(1, 13)
        val ciphertext = decoded.copyOfRange(13, decoded.size)

        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getNameKey(context), GCMParameterSpec(TAG_BIT_LENGTH, iv))

        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }
}