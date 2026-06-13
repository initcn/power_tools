package com.initcn.powertools.feature.vault.data

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.initcn.powertools.core.permissions.StorageAccessManager
import com.initcn.powertools.feature.vault.domain.VaultCryptoEngine
import com.initcn.powertools.feature.vault.domain.VaultNameEncryptor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultStorageManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vaultDatabase: VaultDatabase
) {

    /**
     * Retrieves the root folder selected via SAF.
     */
    fun getVaultRoot(): DocumentFile? {
        val uri = StorageAccessManager.getPersistedUri(context, StorageAccessManager.KEY_DOCUMENTS_URI)
            ?: return null
        return DocumentFile.fromTreeUri(context, uri)
    }

    suspend fun importFile(
        sourceInputStream: InputStream,
        fileName: String,
        mimeType: String,
        virtualParentPath: String
    ): Boolean {
        return try {
            val dao = vaultDatabase.vaultDao()
            val secureDbName = VaultNameEncryptor.encryptName(context, fileName)
            val fileId = UUID.randomUUID().toString()

            val initialEntity = VaultFileEntity(
                id = fileId,
                encryptedName = secureDbName,
                isDirectory = false,
                parentPath = virtualParentPath,
                fileSize = 0L,
                mimeType = mimeType
            )
            dao.insertFile(initialEntity)

            val root = getVaultRoot()
                ?: throw IllegalStateException("Vault folder access not granted.")

            // Create physical file via SAF
            val targetFile = root.createFile("application/octet-stream", fileId)
                ?: throw IllegalStateException("Failed to create file.")

            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(targetFile.uri)?.use { fileOutputStream ->
                    VaultCryptoEngine.encryptStream(fileOutputStream, fileName, mimeType)
                        .use { encryptedStream ->
                            sourceInputStream.copyTo(encryptedStream)
                        }
                } ?: throw IllegalStateException("Could not open SAF stream.")
            }

            val updatedEntity = initialEntity.copy(fileSize = targetFile.length())
            dao.insertFile(updatedEntity)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun exportFile(
        fileId: String,
        destinationOutputStream: OutputStream
    ): Boolean {
        return try {
            val root = getVaultRoot() ?: return false
            val targetFile = root.findFile("$fileId.enc") ?: return false

            withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(targetFile.uri)?.use { fileInputStream ->
                    VaultCryptoEngine.decryptStream(fileInputStream).use { decryptedStream ->
                        decryptedStream.copyTo(destinationOutputStream)
                    }
                } ?: return@withContext false
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}