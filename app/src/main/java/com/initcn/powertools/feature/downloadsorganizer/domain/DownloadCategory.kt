package com.initcn.powertools.feature.downloadsorganizer.domain

enum class DownloadCategory(
    val folderName: String,
    val extensions: Set<String>
) {

    PHOTOS(
        folderName = "Photos",
        extensions = setOf(
            "jpg",
            "jpeg",
            "png",
            "webp",
            "heic",
            "gif"
        )
    ),

    VIDEOS(
        folderName = "Videos",
        extensions = setOf(
            "mp4",
            "mkv",
            "mov",
            "avi",
            "webm",
            "3gp"
        )
    ),

    AUDIO(
        folderName = "Audio",
        extensions = setOf(
            "mp3",
            "wav",
            "flac",
            "aac",
            "m4a",
            "ogg"
        )
    ),

    APPLICATIONS(
        folderName = "Applications",
        extensions = setOf(
            "apk",
            "apks",
            "xapk"
        )
    ),

    PDF(
        folderName = "PDF",
        extensions = setOf(
            "pdf"
        )
    ),

    DOCUMENTS(
        folderName = "Documents",
        extensions = setOf(
            "doc",
            "docx",
            "odt",
            "txt",
            "rtf"
        )
    ),

    SPREADSHEETS(
        folderName = "Spreadsheets",
        extensions = setOf(
            "xls",
            "xlsx",
            "csv"
        )
    ),

    PRESENTATIONS(
        folderName = "Presentations",
        extensions = setOf(
            "ppt",
            "pptx"
        )
    ),

    ARCHIVES(
        folderName = "Archives",
        extensions = setOf(
            "zip",
            "rar",
            "7z",
            "tar",
            "gz"
        )
    ),

    OTHERS(
        folderName = "Others",
        extensions = emptySet()
    );

    companion object {

        fun fromExtension(
            extension: String?
        ): DownloadCategory {

            if (extension.isNullOrBlank()) {
                return OTHERS
            }

            val normalized = extension
                .trim()
                .lowercase()

            return entries.firstOrNull { category ->

                category != OTHERS &&
                        normalized in category.extensions

            } ?: OTHERS
        }
    }
}