package com.initcn.powertools.feature.downloadsorganizer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.initcn.powertools.feature.downloadsorganizer.domain.DownloadsOrganizer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadsOrganizer: DownloadsOrganizer
) : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadsUiState())
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()

    fun onEvent(event: DownloadsEvent) {
        when (event) {
            is DownloadsEvent.SetStatusMessage -> {
                _uiState.update { it.copy(statusMessage = event.message) }
            }
            is DownloadsEvent.PreviewChanges -> previewChanges(event.noFilesMsg, event.filesReadyTemplate)
            is DownloadsEvent.OrganizeDownloads -> organizeDownloads(event.filesMovedTemplate)
        }
    }

    private fun previewChanges(noFilesMsg: String, filesReadyTemplate: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val operations = downloadsOrganizer.preview()
            val newMessage = if (operations.isEmpty()) {
                noFilesMsg
            } else {
                filesReadyTemplate.format(operations.size)
            }
            _uiState.update {
                it.copy(
                    liveFiles = operations,
                    moveLogs = emptyList(),
                    hasScanned = true, // Unlock the list view
                    statusMessage = newMessage
                )
            }
        }
    }

    private fun organizeDownloads(filesMovedTemplate: String) {
        _uiState.update { it.copy(isOrganizing = true, moveLogs = emptyList()) }
        viewModelScope.launch(Dispatchers.IO) {
            val movedCount = downloadsOrganizer.organize { logMsg ->
                _uiState.update { it.copy(moveLogs = it.moveLogs + logMsg) }
            }

            _uiState.update {
                it.copy(
                    isOrganizing = false,
                    statusMessage = filesMovedTemplate.format(movedCount),
                    liveFiles = downloadsOrganizer.preview() // Refresh the view once done
                )
            }
        }
    }
}