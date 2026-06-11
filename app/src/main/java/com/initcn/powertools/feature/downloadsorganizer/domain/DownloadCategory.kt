package com.initcn.powertools.feature.downloadsorganizer.domain

enum class DownloadCategory(
    val displayName: String,
    val folderName: String,
    val description: String,
    val extensions: Set<String>
) {

    PHOTOS(
        displayName = "Photos",
        folderName = "Photos",
        description = "jpg, jpeg, png, webp, heic, gif",
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
        displayName = "Videos",
        folderName = "Videos",
        description = "mp4, mkv, mov, avi, webm, 3gp",
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
        displayName = "Audio",
        folderName = "Audio",
        description = "mp3, wav, flac, aac, m4a, ogg",
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
        displayName = "Applications",
        folderName = "Applications",
        description = "apk, apks, xapk",
        extensions = setOf(
            "apk",
            "apks",
            "xapk"
        )
    ),

    PDF(
        displayName = "PDF Documents",
        folderName = "PDF",
        description = "pdf",
        extensions = setOf(
            "pdf"
        )
    ),

    DOCUMENTS(
        displayName = "Documents",
        folderName = "Documents",
        description = "doc, docx, odt, txt, rtf",
        extensions = setOf(
            "doc",
            "docx",
            "odt",
            "txt",
            "rtf"
        )
    ),

    SPREADSHEETS(
        displayName = "Spreadsheets",
        folderName = "Spreadsheets",
        description = "xls, xlsx, csv",
        extensions = setOf(
            "xls",
            "xlsx",
            "csv"
        )
    ),

    PRESENTATIONS(
        displayName = "Presentations",
        folderName = "Presentations",
        description = "ppt, pptx",
        extensions = setOf(
            "ppt",
            "pptx"
        )
    ),

    ARCHIVES(
        displayName = "Archives",
        folderName = "Archives",
        description = "zip, rar, 7z, tar, gz",
        extensions = setOf(
            "zip",
            "rar",
            "7z",
            "tar",
            "gz"
        )
    ),

    OTHERS(
        displayName = "Others",
        folderName = "Others",
        description = "All remaining file types",
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