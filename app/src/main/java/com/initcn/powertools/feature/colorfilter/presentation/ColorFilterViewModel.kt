package com.initcn.powertools.feature.colorfilter.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.initcn.powertools.feature.colorfilter.domain.ColorFilterManager
import com.initcn.powertools.feature.colorfilter.domain.ColorMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
// FIXED IMPORTS: Added 'coroutines' to the flow package paths
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class ColorFilterUiState(
    val currentMode: ColorMode = ColorMode.OFF,
    val isPermissionGranted: Boolean = true,
    val errorMessage: String? = null
)

sealed interface ColorFilterEvent {
    data class ChangeMode(val mode: ColorMode) : ColorFilterEvent
    data object ClearError : ColorFilterEvent
    data object RefreshStatus : ColorFilterEvent
}

@HiltViewModel
class ColorFilterViewModel @Inject constructor(
    private val filterManager: ColorFilterManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ColorFilterUiState())
    val uiState: StateFlow<ColorFilterUiState> = _uiState.asStateFlow()

    init {
        loadCurrentSystemState()
    }

    fun onEvent(event: ColorFilterEvent) {
        when (event) {
            is ColorFilterEvent.ChangeMode -> applyColorMode(event.mode)
            is ColorFilterEvent.ClearError -> _uiState.update { it.copy(errorMessage = null) }
            is ColorFilterEvent.RefreshStatus -> loadCurrentSystemState()
        }
    }

    private fun loadCurrentSystemState() {
        viewModelScope.launch(Dispatchers.IO) {
            val mode = filterManager.getCurrentMode()
            _uiState.update { it.copy(currentMode = mode, isPermissionGranted = true) }
        }
    }

    private fun applyColorMode(mode: ColorMode) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = filterManager.applyMode(mode)
            if (success) {
                _uiState.update { it.copy(currentMode = mode) }
            } else {
                _uiState.update {
                    it.copy(
                        isPermissionGranted = false,
                        errorMessage = "Secure settings write privilege denied. Please ensure ADB permission is configured."
                    )
                }
            }
        }
    }
}