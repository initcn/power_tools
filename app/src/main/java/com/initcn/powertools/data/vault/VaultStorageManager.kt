package com.initcn.powertools.data.vault

import android.content.Context
import android.os.Environment
import com.initcn.powertools.crypto.VaultCryptoEngine
import com.initcn.powertools.crypto.VaultNameEncryptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream

object VaultStorageManager {

    fun getPublicVaultDirectory(): File {
        val publicDocumentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val appFolder = File(publicDocumentsDir, "PowerToolsVault")
        if (!appFolder.exists()) {
            appFolder.mkdirs()
        }
        return appFolder
    }

    suspend fun importFile(
        context: Context,
        sourceInputStream: InputStream,
        fileName: String,
        mimeType: String,
        virtualParentPath: String
    ): Boolean {
        return try {
            val dao = VaultDatabase.getDatabase(context).vaultDao()
            val secureDbName = VaultNameEncryptor.encryptName(context, fileName)

            val fileId = java.util.UUID.randomUUID().toString()

            val initialEntity = VaultFileEntity(
                id = fileId,
                encryptedName = secureDbName,
                isDirectory = false,
                parentPath = virtualParentPath,
                fileSize = 0L,
                mimeType = mimeType
            )
            dao.insertFile(initialEntity)

            val vaultDir = getPublicVaultDirectory()
            val targetFile = File(vaultDir, "$fileId.enc")

            withContext(Dispatchers.IO) {
                java.io.FileOutputStream(targetFile).use { fileOutputStream ->
                    VaultCryptoEngine.encryptStream(fileOutputStream, fileName, mimeType).use { encryptedStream ->
                        sourceInputStream.copyTo(encryptedStream)
                    }
                }
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
        context: Context,
        fileId: String, // FIX: Changed from Long to String to support UUIDs
        destinationOutputStream: OutputStream
    ): Boolean {
        return try {
            val vaultDir = getPublicVaultDirectory()
            val targetFile = File(vaultDir, "$fileId.enc")

            if (!targetFile.exists()) return false

            withContext(Dispatchers.IO) {
                FileInputStream(targetFile).use { fileInputStream ->
                    VaultCryptoEngine.decryptStream(fileInputStream).use { decryptedStream ->
                        decryptedStream.copyTo(destinationOutputStream)
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}