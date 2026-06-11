package com.initcn.powertools.features.vault.scanner

import android.content.Context
import com.initcn.powertools.crypto.VaultCryptoEngine
import com.initcn.powertools.crypto.VaultNameEncryptor
import com.initcn.powertools.data.vault.VaultDao
import com.initcn.powertools.data.vault.VaultFileEntity
import com.initcn.powertools.features.vault.saf.VaultHeaderManager
import java.io.File
import java.io.FileInputStream

object VaultScanner {

    suspend fun rebuildCatalogFromPublicStorage(context: Context, vaultDir: File, dao: VaultDao) {
        dao.deleteAll()

        vaultDir.listFiles { _, name -> name.endsWith(".enc") }?.forEach { file ->
            try {
                // FIX: Keep the ID as a string UUID
                val fileId = file.nameWithoutExtension

                FileInputStream(file).use { fileInputStream ->
                    // Disable automated header skip to allow the scanner to read it manually
                    VaultCryptoEngine.decryptStream(fileInputStream, consumeHeader = false).use { decryptedStream ->
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
            } catch (_: Exception) {
                // Keep corrupt parsing events isolated cleanly
            }
        }
    }
}