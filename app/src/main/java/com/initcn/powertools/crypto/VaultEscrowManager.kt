package com.initcn.powertools.crypto

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object VaultEscrowManager {

    private const val ESCROW_FILE_NAME = "master.key.blob"
    private const val KDF_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val AES_GCM = "AES/GCM/NoPadding"

    // HARDENING: Modernized iteration count for offline brute-force resistance
    private const val ITERATION_COUNT = 600_000
    private const val KEY_LENGTH_BITS = 256
    private const val TAG_BIT_LENGTH = 128
    private const val BLOB_VERSION = 1

    fun saveMasterKeyToEscrow(vaultDir: File, pin: String) {
        val masterKey = VaultCryptoEngine.getMasterKey()
        val rawMasterKeyBytes = masterKey.encoded
            ?: throw IllegalStateException("Master key is bound to hardware and cannot be exported.")

        val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val derivedKeyBytes = deriveKeyFromPin(pin, salt)
        val derivedKey = SecretKeySpec(derivedKeyBytes, "AES")

        val cipher = Cipher.getInstance(AES_GCM)
        val iv = ByteArray(12).apply { SecureRandom().nextBytes(this) }
        cipher.init(Cipher.ENCRYPT_MODE, derivedKey, GCMParameterSpec(TAG_BIT_LENGTH, iv))

        // Authenticate the unencrypted metadata (Version + Salt + IV)
        val aadBuffer = ByteBuffer.allocate(4 + salt.size + iv.size)
        aadBuffer.putInt(BLOB_VERSION).put(salt).put(iv)
        cipher.updateAAD(aadBuffer.array())

        val encryptedMasterKey = cipher.doFinal(rawMasterKeyBytes)

        val escrowFile = File(vaultDir, ESCROW_FILE_NAME)
        DataOutputStream(FileOutputStream(escrowFile)).use { out ->
            out.writeInt(BLOB_VERSION)
            out.writeInt(salt.size)
            out.write(salt)
            out.writeInt(iv.size)
            out.write(iv)
            out.writeInt(encryptedMasterKey.size)
            out.write(encryptedMasterKey)
        }

        // SECURE MEMORY CLEANUP: Erase sensitive plaintexts from RAM immediately
        Arrays.fill(rawMasterKeyBytes, 0.toByte())
        Arrays.fill(derivedKeyBytes, 0.toByte())
    }

    fun restoreMasterKeyFromEscrow(vaultDir: File, pin: String): SecretKey? {
        val escrowFile = File(vaultDir, ESCROW_FILE_NAME)
        if (!escrowFile.exists()) return null

        return try {
            var version: Int
            var salt: ByteArray
            var iv: ByteArray
            var encryptedMasterKey: ByteArray

            DataInputStream(FileInputStream(escrowFile)).use { input ->
                version = input.readInt()
                if (version != BLOB_VERSION) throw SecurityException("Unsupported escrow blob version")

                val saltSize = input.readInt()
                if (saltSize != 16) throw SecurityException("Invalid salt size")
                salt = ByteArray(saltSize).also { input.readFully(it) }

                val ivSize = input.readInt()
                if (ivSize != 12) throw SecurityException("Invalid IV size")
                iv = ByteArray(ivSize).also { input.readFully(it) }

                val encryptedKeySize = input.readInt()
                if (encryptedKeySize !in 1..1024) throw SecurityException("Invalid payload size")
                encryptedMasterKey = ByteArray(encryptedKeySize).also { input.readFully(it) }
            }

            val derivedKeyBytes = deriveKeyFromPin(pin, salt)
            val derivedKey = SecretKeySpec(derivedKeyBytes, "AES")

            val cipher = Cipher.getInstance(AES_GCM)
            cipher.init(Cipher.DECRYPT_MODE, derivedKey, GCMParameterSpec(TAG_BIT_LENGTH, iv))

            // Verify the metadata hasn't been tampered with
            val aadBuffer = ByteBuffer.allocate(4 + salt.size + iv.size)
            aadBuffer.putInt(version).put(salt).put(iv)
            cipher.updateAAD(aadBuffer.array())

            val rawMasterKeyBytes = cipher.doFinal(encryptedMasterKey)
            val restoredMasterKey = SecretKeySpec(rawMasterKeyBytes, "AES")

            // SECURE MEMORY CLEANUP
            Arrays.fill(derivedKeyBytes, 0.toByte())
            Arrays.fill(rawMasterKeyBytes, 0.toByte())

            restoredMasterKey
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun deriveKeyFromPin(pin: String, salt: ByteArray): ByteArray {
        val pinChars = pin.toCharArray()
        return try {
            val factory = SecretKeyFactory.getInstance(KDF_ALGORITHM)
            val spec = PBEKeySpec(pinChars, salt, ITERATION_COUNT, KEY_LENGTH_BITS)
            factory.generateSecret(spec).encoded
        } finally {
            // SECURE MEMORY CLEANUP: Erase the intermediate char array
            Arrays.fill(pinChars, '\u0000')
        }
    }
}