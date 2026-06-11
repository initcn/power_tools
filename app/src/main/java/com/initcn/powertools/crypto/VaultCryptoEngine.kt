package com.initcn.powertools.crypto

import android.security.keystore.KeyProperties
import android.security.keystore.KeyProtection
import com.initcn.powertools.features.vault.saf.VaultHeaderManager
import java.io.*
import java.nio.ByteBuffer
import java.security.KeyStore
import java.util.Arrays
import javax.crypto.*
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object VaultCryptoEngine {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val VAULT_KEY_ALIAS = "PowerToolsVaultMasterKey"
    private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val TAG_BIT_LENGTH = 128
    private const val ENVELOPE_VERSION = 1

    private var rawMemoryKey: SecretKey? = null

    // Helper to strictly bind envelope metadata to the Master Key (KEK) cipher
    private fun buildKekAad(version: Int, kekIvSize: Int, dekIvSize: Int, dekIv: ByteArray): ByteArray {
        val aadBuffer = ByteBuffer.allocate(4 + 4 + 4 + dekIv.size)
        aadBuffer.putInt(version)
        aadBuffer.putInt(kekIvSize)
        aadBuffer.putInt(dekIvSize).put(dekIv)
        return aadBuffer.array()
    }

    fun getMasterKey(): SecretKey {
        rawMemoryKey?.let { return it }
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        keyStore.getKey(VAULT_KEY_ALIAS, null)?.let { return it as SecretKey }

        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256)
        val newKey = keyGenerator.generateKey()
        importMasterKey(newKey)
        return newKey
    }

    fun encryptStream(outputStream: OutputStream, fileName: String, mimeType: String): OutputStream {
        val dek = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val masterKey = getMasterKey()

        val dekCipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        dekCipher.init(Cipher.ENCRYPT_MODE, dek)

        val kekCipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        kekCipher.init(Cipher.ENCRYPT_MODE, masterKey)

        // 1. Authenticate envelope metadata using the Master Key's AAD
        val aad = buildKekAad(ENVELOPE_VERSION, kekCipher.iv.size, dekCipher.iv.size, dekCipher.iv)
        kekCipher.updateAAD(aad)

        val encryptedDek = kekCipher.doFinal(dek.encoded)

        // 2. Structure the Envelope
        val envelopeBuffer = ByteBuffer.allocate(4 + 4 + kekCipher.iv.size + 4 + encryptedDek.size + 4 + dekCipher.iv.size)
        envelopeBuffer.putInt(ENVELOPE_VERSION)
        envelopeBuffer.putInt(kekCipher.iv.size).put(kekCipher.iv)
        envelopeBuffer.putInt(encryptedDek.size).put(encryptedDek)
        envelopeBuffer.putInt(dekCipher.iv.size).put(dekCipher.iv)

        // 3. Write the envelope to disk
        outputStream.write(envelopeBuffer.array())

        // 4. Initialize stream and header
        val cipherOut = CipherOutputStream(outputStream, dekCipher)
        VaultHeaderManager.writeHeaderToStream(cipherOut, fileName, mimeType)

        // Secure Memory Cleanup
        dek.encoded?.let { Arrays.fill(it, 0.toByte()) }

        return cipherOut
    }

    fun decryptStream(inputStream: InputStream, consumeHeader: Boolean = true): InputStream {
        val dataIn = DataInputStream(inputStream)

        // Strict bounds checking
        val version = dataIn.readInt()
        if (version != ENVELOPE_VERSION) throw SecurityException("Invalid envelope version: $version")

        val kekIvSize = dataIn.readInt()
        if (kekIvSize !in 1..32) throw SecurityException("Invalid KEK IV size")
        val kekIv = ByteArray(kekIvSize).also { dataIn.readFully(it) }

        val encryptedDekSize = dataIn.readInt()
        if (encryptedDekSize !in 1..1024) throw SecurityException("Invalid DEK size")
        val encryptedDek = ByteArray(encryptedDekSize).also { dataIn.readFully(it) }

        val dekIvSize = dataIn.readInt()
        if (dekIvSize !in 1..32) throw SecurityException("Invalid DEK IV size")
        val dekIv = ByteArray(dekIvSize).also { dataIn.readFully(it) }

        val masterKey = getMasterKey()
        val kekCipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        kekCipher.init(Cipher.DECRYPT_MODE, masterKey, GCMParameterSpec(TAG_BIT_LENGTH, kekIv))

        // Reconstruct AAD and verify envelope integrity
        val aad = buildKekAad(version, kekIvSize, dekIvSize, dekIv)
        kekCipher.updateAAD(aad)

        // Throws AEADBadTagException instantly if envelope was tampered with
        val rawDekBytes = kekCipher.doFinal(encryptedDek)
        val dek = SecretKeySpec(rawDekBytes, "AES")

        val dekCipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        dekCipher.init(Cipher.DECRYPT_MODE, dek, GCMParameterSpec(TAG_BIT_LENGTH, dekIv))

        val cipherIn = CipherInputStream(inputStream, dekCipher)

        if (consumeHeader) {
            VaultHeaderManager.readHeaderFromStream(cipherIn)
        }

        // Secure Memory Cleanup
        Arrays.fill(rawDekBytes, 0.toByte())

        return cipherIn
    }

    fun hasMasterKey(): Boolean {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return keyStore.containsAlias(VAULT_KEY_ALIAS)
    }

    fun importMasterKey(key: SecretKey) {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val protection = KeyProtection.Builder(KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build()
        keyStore.setEntry(VAULT_KEY_ALIAS, KeyStore.SecretKeyEntry(key), protection)
        rawMemoryKey = key
    }

    fun clearMemoryKey() {
        rawMemoryKey = null
    }

    fun deleteMasterKey() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (keyStore.containsAlias(VAULT_KEY_ALIAS)) {
                keyStore.deleteEntry(VAULT_KEY_ALIAS)
            }
            clearMemoryKey()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun generateStandbyMasterKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256)
        return keyGenerator.generateKey()
    }

    fun rotateFileEnvelopeKey(
        targetFile: File,
        oldMasterKey: SecretKey,
        newMasterKey: SecretKey
    ) {
        val tempFile = File(targetFile.parent, "${targetFile.name}.tmp")

        try {
            DataInputStream(FileInputStream(targetFile)).use { dataIn ->
                val version = dataIn.readInt()
                if (version != ENVELOPE_VERSION) throw SecurityException("Invalid version")

                val kekIvSize = dataIn.readInt()
                val oldKekIv = ByteArray(kekIvSize).also { dataIn.readFully(it) }

                val encryptedDekSize = dataIn.readInt()
                val oldEncryptedDek = ByteArray(encryptedDekSize).also { dataIn.readFully(it) }

                val dekIvSize = dataIn.readInt()
                val dekIv = ByteArray(dekIvSize).also { dataIn.readFully(it) }

                val oldKekCipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
                oldKekCipher.init(Cipher.DECRYPT_MODE, oldMasterKey, GCMParameterSpec(TAG_BIT_LENGTH, oldKekIv))

                // Verify the old envelope before unpacking
                val oldAad = buildKekAad(version, kekIvSize, dekIvSize, dekIv)
                oldKekCipher.updateAAD(oldAad)

                val rawDekBytes = oldKekCipher.doFinal(oldEncryptedDek)

                val newKekCipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
                newKekCipher.init(Cipher.ENCRYPT_MODE, newMasterKey)

                // Sign the new envelope
                val newAad = buildKekAad(ENVELOPE_VERSION, newKekCipher.iv.size, dekIvSize, dekIv)
                newKekCipher.updateAAD(newAad)

                val newEncryptedDek = newKekCipher.doFinal(rawDekBytes)

                val envelopeBuffer = ByteBuffer.allocate(4 + 4 + newKekCipher.iv.size + 4 + newEncryptedDek.size + 4 + dekIvSize)
                envelopeBuffer.putInt(ENVELOPE_VERSION)
                envelopeBuffer.putInt(newKekCipher.iv.size).put(newKekCipher.iv)
                envelopeBuffer.putInt(newEncryptedDek.size).put(newEncryptedDek)
                envelopeBuffer.putInt(dekIvSize).put(dekIv)
                val newEnvelopeBytes = envelopeBuffer.array()

                FileOutputStream(tempFile).use { fileOut ->
                    fileOut.write(newEnvelopeBytes)

                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Int
                    while (dataIn.read(buffer).also { bytesRead = it } != -1) {
                        fileOut.write(buffer, 0, bytesRead)
                    }
                }

                Arrays.fill(rawDekBytes, 0.toByte())
            }

            if (tempFile.exists() && tempFile.length() > 0) {
                targetFile.delete()
                tempFile.renameTo(targetFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (tempFile.exists()) tempFile.delete()
            throw e
        }
    }
}