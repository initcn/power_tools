package com.initcn.powertools.feature.vault.domain

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.initcn.powertools.feature.vault.data.VaultDao
import com.initcn.powertools.feature.vault.data.VaultFileEntity
import com.initcn.powertools.feature.vault.provider.VaultHeaderManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun rebuildCatalogFromPublicStorage(vaultRoot: DocumentFile, dao: VaultDao) {
        dao.deleteAll()

        // SAF listFiles() returns an Array<DocumentFile>
        // Filter only for .enc files
        val files = vaultRoot.listFiles().filter { it.name?.endsWith(".enc") == true }

        files.forEach { file ->
            try {
                // The file ID is the filename without the ".enc" extension
                val fileId = file.name?.substringBeforeLast(".") ?: return@forEach

                // Open stream via ContentResolver for SAF access
                context.contentResolver.openInputStream(file.uri)?.use { fileInputStream ->
                    // Disable automated header skip to allow the scanner to read it manually
                    VaultCryptoEngine.decryptStream(fileInputStream, consumeHeader = false)
                        .use { decryptedStream ->
                            val metadata = VaultHeaderManager.readHeaderFromStream(decryptedStream)

                            if (metadata != null) {
                                val secureName = VaultNameEncryptor.encryptName(context, metadata.originalName)

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
            } catch (e: Exception) {
                // Log and continue so one corrupt file doesn't crash the whole catalog rebuild
                e.printStackTrace()
            }
        }
    }
}