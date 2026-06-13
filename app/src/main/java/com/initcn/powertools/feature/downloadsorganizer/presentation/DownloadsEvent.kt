package com.initcn.powertools.feature.downloadsorganizer.presentation

import com.initcn.powertools.core.utils.UiText

sealed interface DownloadsEvent {
    data object PreviewChanges : DownloadsEvent
    data object OrganizeDownloads : DownloadsEvent
    data class SetStatusMessage(val message: UiText?) : DownloadsEvent
}