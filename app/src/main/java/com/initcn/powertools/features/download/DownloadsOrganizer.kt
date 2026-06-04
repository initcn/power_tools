package com.initcn.powertools.features.download

import android.os.Environment
import com.initcn.powertools.model.DownloadCategory
import java.io.File

object DownloadsOrganizer {

    data class MoveOperation(
        val source: File,
        val destination: File,
        val category: DownloadCategory
    )

    private fun downloadsDirectory(): File {
        return Environment
            .getExternalStorageDirectory()
            .resolve(Environment.DIRECTORY_DOWNLOADS)
    }

    private fun createUniqueDestination(
        file: File
    ): File {

        if (!file.exists()) {
            return file
        }

        val parent =
            file.parentFile ?: return file

        val name =
            file.nameWithoutExtension

        val extension =
            file.extension

        var index = 1

        while (true) {

            val candidate = File(
                parent,
                if (extension.isBlank()) {
                    "${name}_$index"
                } else {
                    "${name}_$index.$extension"
                }
            )

            if (!candidate.exists()) {
                return candidate
            }

            index++
        }
    }

    /**
     * Preview planned file moves.
     */
    fun preview(): List<MoveOperation> {

        val downloadsDir =
            downloadsDirectory()

        if (!downloadsDir.exists()) {
            return emptyList()
        }

        val operations =
            mutableListOf<MoveOperation>()

        downloadsDir.listFiles()
            ?.filter { file ->

                file.isFile &&
                        !file.isHidden

            }
            ?.forEach { file ->

                val category =
                    DownloadCategory.fromExtension(
                        file.extension
                    )

                val destinationFolder = File(
                    downloadsDir,
                    category.folderName
                )

                val destinationFile = File(
                    destinationFolder,
                    file.name
                )

                operations += MoveOperation(
                    source = file,
                    destination = destinationFile,
                    category = category
                )
            }

        return operations
    }

    /**
     * Organize files.
     *
     * Returns number of successful moves.
     */
    fun organize(): Int {

        val operations =
            preview()

        var movedCount = 0

        operations.forEach { operation ->

            try {

                operation.destination.parentFile
                    ?.mkdirs()

                if (
                    operation.source.absolutePath ==
                    operation.destination.absolutePath
                ) {
                    return@forEach
                }

                val finalDestination =
                    createUniqueDestination(
                        operation.destination
                    )

                if (
                    operation.source.renameTo(
                        finalDestination
                    )
                ) {
                    movedCount++
                }

            } catch (_: Exception) {
                // Ignore failures
            }
        }

        return movedCount
    }

    /**
     * Count files by category.
     */
    fun getCategoryCounts():
            Map<DownloadCategory, Int> {

        return preview()
            .groupingBy {
                it.category
            }
            .eachCount()
    }

    /**
     * Total files available.
     */
    fun totalFiles(): Int {

        return preview().size
    }

    /**
     * Check Downloads folder existence.
     */
    fun downloadsExists(): Boolean {

        return downloadsDirectory()
            .exists()
    }

    /**
     * Downloads path.
     */
    fun downloadsPath(): String {

        return downloadsDirectory()
            .absolutePath
    }
}