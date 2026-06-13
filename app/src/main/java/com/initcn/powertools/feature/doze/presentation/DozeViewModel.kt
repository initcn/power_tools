package com.initcn.powertools.feature.doze.presentation

import androidx.lifecycle.ViewModel
import com.initcn.powertools.R
import com.initcn.powertools.core.utils.UiText
import com.initcn.powertools.feature.doze.domain.DozeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class DozeViewModel @Inject constructor(
    private val dozeManager: DozeManager
) : ViewModel() {

    val supportedTimeouts = dozeManager.supportedTimeouts

    private val _uiState = MutableStateFlow(
        DozeUiState(
            selectedLabel = dozeManager.getCurrentLabel() ?: "1 min"
        )
    )
    val uiState: StateFlow<DozeUiState> = _uiState.asStateFlow()

    fun onEvent(event: DozeEvent) {
        when (event) {
            is DozeEvent.SelectLabel -> {
                _uiState.update { it.copy(selectedLabel = event.label, statusMessage = null) }
            }
            is DozeEvent.SetStatusMessage -> {
                _uiState.update { it.copy(statusMessage = event.message) }
            }
            is DozeEvent.ApplyTimeout -> {
                val success = dozeManager.applyTimeout(_uiState.value.selectedLabel)
                _uiState.update {
                    it.copy(
                        statusMessage = if (success) {
                            UiText.StringResource(R.string.timeout_success)
                        } else {
                            UiText.StringResource(R.string.timeout_failed)
                        }
                    )
                }
            }
        }
    }
}