package com.initcn.powertools.feature.downloadsorganizer.domain

import android.content.Context
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadsOrganizer @Inject constructor(
    @ApplicationContext private val context: Context
) {

    data class MoveOperation(
        val sourcePath: String,
        val fileName: String,
        val category: DownloadCategory
    )

    private fun getDownloadsRoot(): File? {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return if (dir.exists() && dir.isDirectory) dir else null
    }

    fun preview(): List<MoveOperation> {
        val root = getDownloadsRoot() ?: return emptyList()
        val operations = mutableListOf<MoveOperation>()

        root.listFiles()?.forEach { file ->
            // Filter out directories and hidden files (starting with '.')
            if (file.isFile && !file.name.startsWith(".")) {
                val extension = file.extension
                val category = DownloadCategory.fromExtension(extension)

                operations += MoveOperation(
                    sourcePath = file.absolutePath,
                    fileName = file.name,
                    category = category
                )
            }
        }

        return operations
    }

    fun organize(): Int {
        val root = getDownloadsRoot() ?: return 0
        var movedCount = 0

        root.listFiles()?.forEach { file ->
            if (file.isFile && !file.name.startsWith(".")) {
                val extension = file.extension
                val category = DownloadCategory.fromExtension(extension)

                try {
                    // 1. Find or create the target category folder
                    val targetFolder = File(root, category.folderName)
                    if (!targetFolder.exists()) {
                        targetFolder.mkdirs()
                    }

                    // 2. Move the file using native Java IO renameTo (lightning fast)
                    if (targetFolder.exists() && targetFolder.isDirectory) {
                        val targetFile = File(targetFolder, file.name)

                        // If file moved successfully
                        if (file.renameTo(targetFile)) {
                            movedCount++
                        }
                    }
                } catch (e: Exception) {
                    // Ignore failures (e.g., file lock, name collision)
                    e.printStackTrace()
                }
            }
        }

        return movedCount
    }

    fun getCategoryCounts(): Map<DownloadCategory, Int> {
        return preview().groupingBy { it.category }.eachCount()
    }

    fun totalFiles(): Int = preview().size

    fun downloadsExists(): Boolean {
        return getDownloadsRoot()?.exists() == true
    }

    fun downloadsPath(): String {
        return getDownloadsRoot()?.absolutePath ?: ""
    }
}