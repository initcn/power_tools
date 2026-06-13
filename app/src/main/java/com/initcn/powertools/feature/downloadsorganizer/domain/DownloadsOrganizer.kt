package com.initcn.powertools.feature.downloadsorganizer.domain

import android.os.Environment
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadsOrganizer @Inject constructor() {

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

    // Prevents overwriting by generating a unique name
    private fun getUniqueTargetFile(targetFolder: File, originalName: String): File {
        var targetFile = File(targetFolder, originalName)

        // If the name is free, use it immediately
        if (!targetFile.exists()) return targetFile

        // Otherwise, split the name and extension to append a counter
        val nameWithoutExtension = targetFile.nameWithoutExtension
        val extension = targetFile.extension
        val extWithDot = if (extension.isNotEmpty()) ".$extension" else ""

        var counter = 1
        while (targetFile.exists()) {
            targetFile = File(targetFolder, "$nameWithoutExtension ($counter)$extWithDot")
            counter++
        }

        return targetFile
    }

    fun organize(onMoveLog: (String) -> Unit): Int {
        val root = getDownloadsRoot() ?: return 0
        var movedCount = 0

        root.listFiles()?.forEach { file ->
            if (file.isFile && !file.name.startsWith(".")) {
                val extension = file.extension
                val category = DownloadCategory.fromExtension(extension)

                try {
                    // Find or create the target category folder
                    val targetFolder = File(root, category.folderName)
                    if (!targetFolder.exists()) {
                        targetFolder.mkdirs()
                    }

                    if (targetFolder.exists() && targetFolder.isDirectory) {
                        val targetFile = getUniqueTargetFile(targetFolder, file.name)

                        // If file moved successfully
                        if (file.renameTo(targetFile)) {
                            movedCount++
                            // Log the new name in case it had to be changed!
                            onMoveLog("Moved: ${file.name} ➔ ${category.folderName}/${targetFile.name}")
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    onMoveLog("Error: Failed to move ${file.name}")
                }
            }
        }
        return movedCount
    }
}