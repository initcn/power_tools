package com.initcn.powertools.feature.vault.domain

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.initcn.powertools.feature.vault.provider.VaultHeaderManager
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.AEADBadTagException

@RunWith(AndroidJUnit4::class)
class VaultSecurityAuditTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val random = SecureRandom()
    private val androidKeystore = "AndroidKeyStore"
    private val vaultKeyAlias = "PowerToolsVaultMasterKey" // From VaultCryptoEngine

    @Before
    fun setUp() {
        clearAllState()
    }

    @After
    fun tearDown() {
        clearAllState()
    }

    private fun clearAllState() {
        VaultCryptoEngine.deleteMasterKey()
        VaultSessionManager.factoryReset(context)
    }

    private fun randomBytes(size: Int): ByteArray = ByteArray(size).also { random.nextBytes(it) }

    // --- 1. KEYSTORE & MEMORY LEAK TESTS ---

    @Test
    fun audit_deleteMasterKey_completelyRemovesFromHardware() {
        // Arrange: Generate the key
        VaultCryptoEngine.getMasterKey()
        val keyStore = KeyStore.getInstance(androidKeystore).apply { load(null) }
        assertTrue("Key should exist in hardware", keyStore.containsAlias(vaultKeyAlias))
        assertTrue("Engine should report key exists", VaultCryptoEngine.hasMasterKey())

        // Act: Delete it
        VaultCryptoEngine.deleteMasterKey()

        // Assert: Ensure it is wiped from hardware and memory
        assertFalse("Key leaked: Still exists in hardware Keystore", keyStore.containsAlias(vaultKeyAlias))
        assertFalse("Key leaked: Engine still reports key exists", VaultCryptoEngine.hasMasterKey())
    }

    @Test
    fun audit_sessionLock_clearsUIState() {
        // Arrange: Simulate an unlocked vault
        VaultSessionManager.setupVault(context, "1234")
        assertTrue("Vault should be unlocked", VaultSessionManager.isUnlocked())

        // Act: Lock it
        VaultSessionManager.lock()

        // Assert: The session must be completely locked
        assertFalse("State leak: Vault is still unlocked after lock()", VaultSessionManager.isUnlocked())
    }

    // --- 2. CIPHERTEXT DATA LEAK TESTS ---

    @Test
    fun audit_ciphertext_doesNotLeakPlaintext() {
        val plainTextString = "TOP_SECRET_FINANCIAL_DATA_DO_NOT_LEAK"
        val plainBytes = plainTextString.toByteArray(Charsets.UTF_8)

        val encryptedStream = ByteArrayOutputStream()
        VaultCryptoEngine.encryptStream(encryptedStream, "finance.pdf", "application/pdf").use {
            it.write(plainBytes)
        }
        val cipherBytes = encryptedStream.toByteArray()
        val cipherString = String(cipherBytes, Charsets.UTF_8)

        // Assert: The exact plaintext byte sequence must NOT exist in the ciphertext
        assertFalse(
            "Data Leak: Plaintext was found unencrypted inside the ciphertext output!",
            cipherString.contains(plainTextString)
        )
    }

    @Test
    fun audit_nameEncryptor_doesNotLeakOriginalName() {
        val originalName = "TaxReturns2024.pdf"

        // Act
        val encryptedName = VaultNameEncryptor.encryptName(context, originalName)

        // Assert: The encrypted name should look nothing like the original
        assertFalse("Name Leak: Encrypted string contains original file name", encryptedName.contains("TaxReturns"))
        assertFalse("Name Leak: Encrypted string contains extension", encryptedName.contains(".pdf"))

        // Ensure it decrypts correctly
        val decryptedName = VaultNameEncryptor.decryptName(context, encryptedName)
        assertEquals("Name decryption failed", originalName, decryptedName)
    }

    // --- 3. TAMPERING & INTEGRITY (AAD) TESTS ---

    @Test
    fun audit_envelopeAAD_tamperingCausesInstantFailure() {
        // Testing AES-GCM's strict authentication
        val plainBytes = randomBytes(256)
        val encryptedStream = ByteArrayOutputStream()

        // We write the header manually here to easily manipulate the raw envelope bytes
        VaultHeaderManager.writeHeaderToStream(encryptedStream, "test.bin", "app/bin")
        VaultCryptoEngine.encryptStream(encryptedStream, "test.bin", "app/bin").use { it.write(plainBytes) }

        val cipherBytes = encryptedStream.toByteArray()

        // Act: Tamper with the Version Byte or IV size (first few bytes of the envelope)
        // The envelope starts right after the JSON header.
        // We will flip a single bit in the ciphertext array to simulate disk corruption or a malicious edit.
        cipherBytes[cipherBytes.size - 50] = (cipherBytes[cipherBytes.size - 50].toInt() xor 0xFF).toByte()

        // Assert
        try {
            VaultCryptoEngine.decryptStream(ByteArrayInputStream(cipherBytes), true).readBytes()
            fail("Security Vulnerability: AES-GCM accepted tampered ciphertext/AAD without throwing AEADBadTagException!")
        } catch (e: SecurityException) {
            // Your engine wraps or throws SecurityException for envelope validation
            assertTrue(true)
        } catch (e: AEADBadTagException) {
            // Native GCM rejection
            assertTrue(true)
        } catch (e: Exception) {
            // Any other crypto exception is also an acceptable rejection
            assertTrue(true)
        }
    }

    // --- 4. ESCROW / PBKDF2 TESTS ---

    @Test
    fun audit_escrowRestore_failsWithIncorrectPin() {
        val originalPin = "8492"
        val hackerPin = "8493" // Off by one

        // Arrange: Create a master key and back it up
        val masterKey = VaultCryptoEngine.getMasterKey()
        val escrowStream = ByteArrayOutputStream()
        VaultEscrowManager.saveToStream(masterKey, originalPin, escrowStream)
        val escrowBlob = escrowStream.toByteArray()

        // Act & Assert: Attempt to restore with the WRONG pin
        val restoredWrong = VaultEscrowManager.restoreFromStream(ByteArrayInputStream(escrowBlob), hackerPin)
        assertNull("Security Bypass: Escrow successfully decrypted with the wrong PIN!", restoredWrong)

        // Act & Assert: Attempt to restore with the CORRECT pin
        val restoredRight = VaultEscrowManager.restoreFromStream(ByteArrayInputStream(escrowBlob), originalPin)
        assertNotNull("System Failure: Could not restore Escrow with the correct PIN.", restoredRight)

        // Verify key integrity
        assertArrayEquals("Restored key does not match original hardware key", masterKey.encoded, restoredRight?.encoded)
    }
}