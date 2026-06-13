package com.initcn.powertools.feature.downloadsorganizer.presentation

import com.initcn.powertools.feature.downloadsorganizer.domain.DownloadsOrganizer

data class DownloadsUiState(
    val statusMessage: String? = null,
    val liveFiles: List<DownloadsOrganizer.MoveOperation> = emptyList(),
    val moveLogs: List<String> = emptyList(),
    val isOrganizing: Boolean = false,
    val hasScanned: Boolean = false
)