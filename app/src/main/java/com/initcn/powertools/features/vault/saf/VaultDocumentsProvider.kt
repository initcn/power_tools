package com.initcn.powertools.features.vault.saf

import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import com.initcn.powertools.R
import com.initcn.powertools.crypto.VaultCryptoEngine
import com.initcn.powertools.crypto.VaultNameEncryptor
import com.initcn.powertools.data.vault.VaultDatabase
import com.initcn.powertools.data.vault.VaultFileEntity
import com.initcn.powertools.data.vault.VaultStorageManager
import kotlinx.coroutines.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class VaultDocumentsProvider : DocumentsProvider() {

    companion object {
        private const val ROOT_ID = "root"
        private const val AUTHORITY = "com.initcn.powertools.vault.documents"

        private val DEFAULT_ROOT_PROJECTION = arrayOf(
            DocumentsContract.Root.COLUMN_ROOT_ID,
            DocumentsContract.Root.COLUMN_DOCUMENT_ID,
            DocumentsContract.Root.COLUMN_TITLE,
            DocumentsContract.Root.COLUMN_ICON,
            DocumentsContract.Root.COLUMN_MIME_TYPES,
            DocumentsContract.Root.COLUMN_FLAGS
        )

        private val DEFAULT_DOCUMENT_PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_FLAGS,
            DocumentsContract.Document.COLUMN_SIZE
        )
    }

    private val providerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate(): Boolean = true

    private fun resolveRootProjection(projection: Array<out String>?): Array<out String> =
        projection ?: DEFAULT_ROOT_PROJECTION

    private fun resolveDocumentProjection(projection: Array<out String>?): Array<out String> =
        projection ?: DEFAULT_DOCUMENT_PROJECTION

    override fun queryRoots(projection: Array<out String>?): Cursor {
        val cols = resolveRootProjection(projection)
        val result = MatrixCursor(cols)
        val row = result.newRow()

        for (col in cols) {
            when (col) {
                DocumentsContract.Root.COLUMN_ROOT_ID -> row.add(ROOT_ID)
                DocumentsContract.Root.COLUMN_DOCUMENT_ID -> row.add(ROOT_ID)
                DocumentsContract.Root.COLUMN_TITLE -> row.add("Secure Vault")
                DocumentsContract.Root.COLUMN_ICON -> row.add(R.mipmap.ic_launcher)
                DocumentsContract.Root.COLUMN_MIME_TYPES -> row.add("${DocumentsContract.Document.MIME_TYPE_DIR},*/*")
                DocumentsContract.Root.COLUMN_FLAGS -> row.add(
                    DocumentsContract.Root.FLAG_LOCAL_ONLY or
                            DocumentsContract.Root.FLAG_SUPPORTS_CREATE or
                            DocumentsContract.Root.FLAG_SUPPORTS_IS_CHILD or
                            DocumentsContract.Root.FLAG_SUPPORTS_SEARCH
                )
                else -> row.add(null)
            }
        }
        return result
    }

    override fun queryDocument(documentId: String?, projection: Array<out String>?): Cursor {
        val cols = resolveDocumentProjection(projection)
        val result = MatrixCursor(cols)

        if (documentId == null || !VaultSessionManager.isUnlocked()) return result

        if (documentId == ROOT_ID) {
            val row = result.newRow()
            for (col in cols) {
                when (col) {
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID -> row.add(ROOT_ID)
                    DocumentsContract.Document.COLUMN_MIME_TYPE -> row.add(DocumentsContract.Document.MIME_TYPE_DIR)
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME -> row.add("/")
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED -> row.add(System.currentTimeMillis())
                    DocumentsContract.Document.COLUMN_FLAGS -> row.add(DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE)
                    DocumentsContract.Document.COLUMN_SIZE -> row.add(0L)
                    else -> row.add(null)
                }
            }
        } else {
            // FIX: documentId is now a UUID String
            runBlocking {
                VaultDatabase.getDatabase(context!!).vaultDao().getFileById(documentId)?.let { entity ->
                    includeEntityRow(result, cols, entity)
                }
            }
        }
        return result
    }

    override fun queryChildDocuments(
        parentDocumentId: String?,
        projection: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val cols = resolveDocumentProjection(projection)
        val result = MatrixCursor(cols)

        if (parentDocumentId == null || !VaultSessionManager.isUnlocked()) return result

        val targetPath = if (parentDocumentId == ROOT_ID) "/" else {
            runBlocking {
                // FIX: parentDocumentId is a UUID String
                val parent = VaultDatabase.getDatabase(context!!).vaultDao().getFileById(parentDocumentId)
                if (parent != null) {
                    val decryptedParentName = VaultNameEncryptor.decryptName(context!!, parent.encryptedName)
                    "${parent.parentPath}/$decryptedParentName".replace("//", "/")
                } else "/"
            }
        }

        context?.let { ctx ->
            val notifyUri = DocumentsContract.buildChildDocumentsUri(AUTHORITY, parentDocumentId)
            result.setNotificationUri(ctx.contentResolver, notifyUri)
        }

        runBlocking {
            val children = VaultDatabase.getDatabase(context!!).vaultDao().getFilesByParentPath(targetPath)
            children.forEach { entity -> includeEntityRow(result, cols, entity) }
        }
        return result
    }

    override fun createDocument(
        parentDocumentId: String?,
        mimeType: String?,
        displayName: String?
    ): String? {
        if (parentDocumentId == null || mimeType == null || displayName == null) return null
        if (!VaultSessionManager.isUnlocked()) throw SecurityException("Vault locked.")

        return runBlocking {
            val dao = VaultDatabase.getDatabase(context!!).vaultDao()
            val secureDbName = VaultNameEncryptor.encryptName(context!!, displayName)

            val parentPath = if (parentDocumentId == ROOT_ID) "/" else {
                val parentEntity = dao.getFileById(parentDocumentId)
                if (parentEntity != null) {
                    val decryptedParentName = VaultNameEncryptor.decryptName(context!!, parentEntity.encryptedName)
                    "${parentEntity.parentPath}/$decryptedParentName".replace("//", "/")
                } else "/"
            }

            // FIX: Create new UUID for the file
            val fileId = java.util.UUID.randomUUID().toString()
            val entity = VaultFileEntity(
                id = fileId,
                encryptedName = secureDbName,
                isDirectory = (mimeType == DocumentsContract.Document.MIME_TYPE_DIR),
                parentPath = parentPath,
                fileSize = 0L,
                mimeType = mimeType
            )
            dao.insertFile(entity)

            if (mimeType != DocumentsContract.Document.MIME_TYPE_DIR) {
                val targetFile = File(VaultStorageManager.getPublicVaultDirectory(), "$fileId.enc")
                if (!targetFile.exists()) {
                    targetFile.createNewFile()
                }
            }

            fileId
        }
    }

    override fun openDocument(
        documentId: String?,
        mode: String?,
        signal: CancellationSignal?
    ): ParcelFileDescriptor? {
        if (documentId == null || mode == null) return null
        if (!VaultSessionManager.isUnlocked()) throw SecurityException("Vault locked.")

        // FIX: No need to parse to Long, use UUID String
        val targetFile = File(VaultStorageManager.getPublicVaultDirectory(), "$documentId.enc")

        val isWrite = mode.contains("w") || mode.contains("a")

        if (isWrite) {
            val pipe = ParcelFileDescriptor.createReliablePipe()
            val readFd = pipe[0]
            val writeFd = pipe[1]

            val entity = runBlocking { VaultDatabase.getDatabase(context!!).vaultDao().getFileById(documentId) }
            val clearName = entity?.let { VaultNameEncryptor.decryptName(context!!, it.encryptedName) } ?: "file_$documentId"
            val mimeType = entity?.mimeType ?: "application/octet-stream"

            val job = providerScope.launch(Dispatchers.IO) {
                var success = false
                try {
                    ParcelFileDescriptor.AutoCloseInputStream(readFd).use { pipeIn ->
                        FileOutputStream(targetFile, false).use { fileOut ->
                            VaultCryptoEngine.encryptStream(fileOut, clearName, mimeType).use { cryptoStream ->
                                val buffer = ByteArray(64 * 1024)
                                var bytesRead: Int
                                while (pipeIn.read(buffer).also { bytesRead = it } != -1) {
                                    ensureActive()
                                    cryptoStream.write(buffer, 0, bytesRead)
                                }
                                cryptoStream.flush()
                            }
                        }
                    }
                    success = true
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    try {
                        if (success) {
                            writeFd.close()
                        } else {
                            writeFd.closeWithError("Crypto stream write transaction aborted programmatically.")
                        }
                    } catch (_: Exception) {}
                }
            }

            signal?.setOnCancelListener { job.cancel() }
            return writeFd
        } else {
            if (!targetFile.exists() || targetFile.length() == 0L) return null

            val cacheFile = File(context!!.cacheDir, "vault_pfd_$documentId.tmp")
            try {
                runBlocking(Dispatchers.IO) {
                    FileInputStream(targetFile).use { fileIn ->
                        VaultCryptoEngine.decryptStream(fileIn).use { cryptoStream ->
                            FileOutputStream(cacheFile).use { cacheOut ->
                                val buffer = ByteArray(64 * 1024)
                                var bytesRead: Int
                                while (cryptoStream.read(buffer).also { bytesRead = it } != -1) {
                                    ensureActive()
                                    cacheOut.write(buffer, 0, bytesRead)
                                }
                                cacheOut.flush()
                            }
                        }
                    }
                }

                val pfd = ParcelFileDescriptor.open(cacheFile, ParcelFileDescriptor.MODE_READ_ONLY)
                cacheFile.delete()
                return pfd
            } catch (e: Exception) {
                e.printStackTrace()
                if (cacheFile.exists()) cacheFile.delete()
                return null
            }
        }
    }

    override fun isChildDocument(parentDocumentId: String?, documentId: String?): Boolean {
        if (parentDocumentId == null || documentId == null) return false
        if (parentDocumentId == ROOT_ID) return true

        return runBlocking {
            val dao = VaultDatabase.getDatabase(context!!).vaultDao()
            val child = dao.getFileById(documentId)
            val parent = dao.getFileById(parentDocumentId)

            if (child != null && parent != null) {
                val decryptedParentName = VaultNameEncryptor.decryptName(context!!, parent.encryptedName)
                val expectedParentPath = "${parent.parentPath}/$decryptedParentName".replace("//", "/")
                child.parentPath == expectedParentPath
            } else {
                false
            }
        }
    }

    override fun deleteDocument(documentId: String?) {
        if (documentId == null) return
        if (!VaultSessionManager.isUnlocked()) throw SecurityException("Vault locked.")

        runBlocking {
            val dao = VaultDatabase.getDatabase(context!!).vaultDao()
            dao.getFileById(documentId)?.let { entity ->
                dao.deleteFile(entity)
                File(VaultStorageManager.getPublicVaultDirectory(), "$documentId.enc").delete()
            }
        }
    }

    private fun includeEntityRow(cursor: MatrixCursor, cols: Array<out String>, entity: VaultFileEntity) {
        val flags = if (entity.isDirectory) {
            DocumentsContract.Document.FLAG_SUPPORTS_DELETE or DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE
        } else {
            DocumentsContract.Document.FLAG_SUPPORTS_DELETE or DocumentsContract.Document.FLAG_SUPPORTS_WRITE
        }

        val clearDisplayName = try {
            VaultNameEncryptor.decryptName(context!!, entity.encryptedName)
        } catch (_: Exception) {
            "Encrypted_File_${entity.id}"
        }

        val row = cursor.newRow()
        for (col in cols) {
            when (col) {
                DocumentsContract.Document.COLUMN_DOCUMENT_ID -> row.add(entity.id)
                DocumentsContract.Document.COLUMN_MIME_TYPE -> row.add(if (entity.isDirectory) DocumentsContract.Document.MIME_TYPE_DIR else entity.mimeType)
                DocumentsContract.Document.COLUMN_DISPLAY_NAME -> row.add(clearDisplayName)
                DocumentsContract.Document.COLUMN_LAST_MODIFIED -> row.add(entity.lastModified)
                DocumentsContract.Document.COLUMN_SIZE -> row.add(entity.fileSize)
                DocumentsContract.Document.COLUMN_FLAGS -> row.add(flags)
                else -> row.add(null)
            }
        }
    }
}