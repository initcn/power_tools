package com.initcn.powertools.feature.downloadsorganizer.presentation

sealed interface DownloadsEvent {
    data class PreviewChanges(val noFilesMsg: String, val filesReadyTemplate: String) : DownloadsEvent
    data class OrganizeDownloads(val filesMovedTemplate: String) : DownloadsEvent
    data class SetStatusMessage(val message: String?) : DownloadsEvent
}