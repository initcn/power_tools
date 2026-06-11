package com.initcn.powertools.feature.vault.provider

import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import com.initcn.powertools.R
import com.initcn.powertools.feature.vault.data.VaultDatabase
import com.initcn.powertools.feature.vault.data.VaultFileEntity
import com.initcn.powertools.feature.vault.data.VaultStorageManager
import com.initcn.powertools.feature.vault.domain.VaultCryptoEngine
import com.initcn.powertools.feature.vault.domain.VaultNameEncryptor
import com.initcn.powertools.feature.vault.domain.VaultSessionManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.FileOutputStream
import java.util.UUID

class VaultDocumentsProvider : DocumentsProvider() {

    private val providerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface VaultProviderEntryPoint {
        fun vaultDatabase(): VaultDatabase
        fun vaultStorageManager(): VaultStorageManager
    }

    private fun getDao(): com.initcn.powertools.feature.vault.data.VaultDao {
        val ctx = context ?: throw IllegalStateException("Provider context is null")
        return EntryPointAccessors.fromApplication(
            ctx.applicationContext, VaultProviderEntryPoint::class.java
        ).vaultDatabase().vaultDao()
    }

    private fun getStorage(): VaultStorageManager {
        val ctx = context ?: throw IllegalStateException("Provider context is null")
        return EntryPointAccessors.fromApplication(
            ctx.applicationContext, VaultProviderEntryPoint::class.java
        ).vaultStorageManager()
    }

    override fun onCreate(): Boolean = true

    override fun queryRoots(projection: Array<out String>?): Cursor {
        val resolvedProjection = projection ?: arrayOf(
            DocumentsContract.Root.COLUMN_ROOT_ID,
            DocumentsContract.Root.COLUMN_DOCUMENT_ID,
            DocumentsContract.Root.COLUMN_TITLE,
            DocumentsContract.Root.COLUMN_FLAGS,
            DocumentsContract.Root.COLUMN_ICON,
            DocumentsContract.Root.COLUMN_SUMMARY
        )
        val result = MatrixCursor(resolvedProjection)

        if (VaultSessionManager.isUnlocked()) {
            val row = result.newRow()
            for (column in resolvedProjection) {
                when (column) {
                    DocumentsContract.Root.COLUMN_ROOT_ID -> row.add("vault_root")
                    DocumentsContract.Root.COLUMN_DOCUMENT_ID -> row.add("root")
                    DocumentsContract.Root.COLUMN_TITLE -> row.add("PowerTools Vault")
                    DocumentsContract.Root.COLUMN_FLAGS -> row.add(
                        DocumentsContract.Root.FLAG_LOCAL_ONLY or
                                DocumentsContract.Root.FLAG_SUPPORTS_CREATE or
                                DocumentsContract.Root.FLAG_SUPPORTS_IS_CHILD
                    )
                    DocumentsContract.Root.COLUMN_ICON -> row.add(R.mipmap.ic_launcher)
                    DocumentsContract.Root.COLUMN_SUMMARY -> row.add("Secure encrypted storage")
                    else -> row.add(null)
                }
            }
        }
        return result
    }

    override fun queryDocument(documentId: String?, projection: Array<out String>?): Cursor {
        val resolvedProjection = projection ?: arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_FLAGS
        )
        val result = MatrixCursor(resolvedProjection)
        if (documentId == null || !VaultSessionManager.isUnlocked()) return result

        runBlocking {
            if (documentId == "root") {
                val row = result.newRow()
                for (column in resolvedProjection) {
                    when (column) {
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID -> row.add("root")
                        DocumentsContract.Document.COLUMN_MIME_TYPE -> row.add(DocumentsContract.Document.MIME_TYPE_DIR)
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME -> row.add("PowerTools Vault")
                        DocumentsContract.Document.COLUMN_FLAGS -> row.add(
                            DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE or
                                    DocumentsContract.Document.FLAG_SUPPORTS_WRITE
                        )
                        DocumentsContract.Document.COLUMN_SIZE -> row.add(0)
                        else -> row.add(null)
                    }
                }
            } else {
                val entity = getDao().getFileById(documentId)
                if (entity != null) includeEntityRow(result, resolvedProjection, entity)
            }
        }
        return result
    }

    override fun createDocument(
        parentDocumentId: String?,
        mimeType: String?,
        displayName: String?
    ): String? {

        val ctx = context ?: return null
        if (!VaultSessionManager.isUnlocked()) return null

        val id = UUID.randomUUID().toString()

        val root = getStorage().getVaultRoot()
            ?: return null

        root.createFile(
            "application/octet-stream",
            "$id.enc"
        ) ?: return null

        runBlocking {
            getDao().insertFile(
                VaultFileEntity(
                    id = id,
                    encryptedName = VaultNameEncryptor.encryptName(
                        ctx,
                        displayName ?: "New File"
                    ),
                    isDirectory = false,
                    parentPath = "/",
                    fileSize = 0L,
                    mimeType = mimeType ?: "application/octet-stream"
                )
            )
        }

        return id
    }
    override fun queryChildDocuments(parentDocumentId: String?, projection: Array<out String>?, sortOrder: String?): Cursor {
        val resolvedProjection = projection ?: arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_FLAGS
        )
        val result = MatrixCursor(resolvedProjection)
        val ctx = context ?: return result
        if (parentDocumentId == null || !VaultSessionManager.isUnlocked()) return result

        runBlocking {
            val path = if (parentDocumentId == "root") "/" else {
                val parent = getDao().getFileById(parentDocumentId)
                val name = parent?.let { VaultNameEncryptor.decryptName(ctx, it.encryptedName) } ?: ""
                "${parent?.parentPath}/$name".replace("//", "/")
            }
            getDao().getFilesByParentPath(path).forEach { includeEntityRow(result, resolvedProjection, it) }
        }
        return result
    }

    override fun openDocument(documentId: String?, mode: String, signal: CancellationSignal?): ParcelFileDescriptor? {
        if (documentId == null || !VaultSessionManager.isUnlocked()) return null
        val root = getStorage().getVaultRoot() ?: return null
        val targetFile = root.findFile("$documentId.enc") ?: return null

        return if (mode.contains("w")) handleWriteOperation(documentId, targetFile)
        else handleReadOperation(targetFile)
    }

    private fun handleReadOperation(targetFile: androidx.documentfile.provider.DocumentFile): ParcelFileDescriptor? {
        val ctx = context ?: return null
        val cacheFile = java.io.File(ctx.cacheDir, "vault_temp_${UUID.randomUUID()}.tmp")
        return try {
            runBlocking(Dispatchers.IO) {
                ctx.contentResolver.openInputStream(targetFile.uri)?.use { fileIn ->
                    VaultCryptoEngine.decryptStream(fileIn).use { cryptoStream ->
                        FileOutputStream(cacheFile).use { cacheOut -> cryptoStream.copyTo(cacheOut) }
                    }
                }
            }
            ParcelFileDescriptor.open(cacheFile, ParcelFileDescriptor.MODE_READ_ONLY)
        } catch (_: Exception) { null }
    }

    private fun handleWriteOperation(documentId: String, targetFile: androidx.documentfile.provider.DocumentFile): ParcelFileDescriptor {
        val pipe = ParcelFileDescriptor.createReliablePipe()
        val writeSide = pipe[1]
        val readSide = pipe[0]
        val ctx = context ?: return writeSide

        providerScope.launch(Dispatchers.IO) {
            val tempFile = java.io.File(ctx.cacheDir, "write_tx_${documentId}.tmp")
            try {
                val entity = getDao().getFileById(documentId) ?: throw Exception("Entity not found")
                val clearName = VaultNameEncryptor.decryptName(ctx, entity.encryptedName)

                ParcelFileDescriptor.AutoCloseInputStream(readSide).use { pipeIn ->
                    FileOutputStream(tempFile).use { fos ->
                        VaultCryptoEngine.encryptStream(fos, clearName, entity.mimeType).use { cryptoOut ->
                            pipeIn.copyTo(cryptoOut)
                            cryptoOut.flush()
                        }
                    }
                }

                ctx.contentResolver.openOutputStream(targetFile.uri, "w")?.use { vaultOut ->
                    java.io.FileInputStream(tempFile).use { tempIn ->
                        tempIn.copyTo(vaultOut)
                        vaultOut.flush()
                    }
                }
                getDao().insertFile(entity.copy(fileSize = targetFile.length()))
            } catch (e: Exception) {
                writeSide.closeWithError("Write failed: ${e.message}")
            } finally {
                writeSide.close()
                tempFile.delete()
            }
        }
        return writeSide
    }

    override fun deleteDocument(documentId: String?) {
        if (documentId.isNullOrEmpty() || !VaultSessionManager.isUnlocked()) return

        runBlocking {
            val entity = getDao().getFileById(documentId)
            if (entity != null) {
                getDao().deleteFile(entity)
                getStorage().getVaultRoot()?.findFile("$documentId.enc")?.delete()
            }
        }
    }

    private fun includeEntityRow(cursor: MatrixCursor, projection: Array<out String>, entity: VaultFileEntity) {
        val ctx = context ?: return
        val name = try { VaultNameEncryptor.decryptName(ctx, entity.encryptedName) } catch (_: Exception) { "File" }
        val row = cursor.newRow()
        for (column in projection) {
            when (column) {
                DocumentsContract.Document.COLUMN_DOCUMENT_ID -> row.add(entity.id)
                DocumentsContract.Document.COLUMN_MIME_TYPE -> row.add(entity.mimeType)
                DocumentsContract.Document.COLUMN_DISPLAY_NAME -> row.add(name)
                DocumentsContract.Document.COLUMN_SIZE -> row.add(entity.fileSize)
                DocumentsContract.Document.COLUMN_FLAGS -> row.add(
                    DocumentsContract.Document.FLAG_SUPPORTS_DELETE or
                            DocumentsContract.Document.FLAG_SUPPORTS_WRITE
                )
                else -> row.add(null)
            }
        }
    }

    override fun isChildDocument(parentDocumentId: String?, documentId: String?): Boolean = true
}